#!/usr/bin/env node

import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { readFileSync, writeFileSync } from "node:fs";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const source = resolve(root, "src", "openapi.yml");
const target = resolve(root, "openapi.yml");

function stripDocumentMarker(content) {
  return content.replace(/^---\r?\n/, "");
}

function indentContent(content, indentation) {
  return content
    .split(/\r?\n/)
    .map((line) => (line.length === 0 ? line : `${indentation}${line}`))
    .join("\n");
}

function decodeJsonPointerToken(token) {
  return token.replaceAll("~1", "/").replaceAll("~0", "~");
}

function extractTopLevelNode(content, fragment) {
  if (!fragment) {
    return content;
  }

  const pointer = fragment.replace(/^#/, "");
  const tokens = pointer.split("/").filter(Boolean).map(decodeJsonPointerToken);

  if (tokens.length !== 1) {
    throw new Error(`Only single-level YAML fragments are supported: ${fragment}`);
  }

  const key = tokens[0];
  const lines = content.split(/\r?\n/);
  const keyPattern = new RegExp(`^("?${key.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}"?)\\s*:\\s*$`);
  const start = lines.findIndex((line) => keyPattern.test(line));

  if (start === -1) {
    throw new Error(`Fragment ${fragment} was not found`);
  }

  const nodeLines = [];
  for (let index = start + 1; index < lines.length; index += 1) {
    const line = lines[index];
    if (line.length > 0 && !line.startsWith(" ")) {
      break;
    }
    nodeLines.push(line.startsWith("  ") ? line.slice(2) : line);
  }

  return nodeLines.join("\n").trimEnd();
}

function inlineLocalRefs(filePath) {
  const currentDir = dirname(filePath);
  const content = stripDocumentMarker(readFileSync(filePath, "utf8"));

  return content
    .split(/\r?\n/)
    .flatMap((line) => {
      const refMatch = line.match(/^(\s*)(?:"?\$ref"?)\s*:\s*["'](\.\/[^"']+)["']\s*$/);

      if (!refMatch) {
        return line;
      }

      const [, indentation, refPath] = refMatch;
      const [referencedFilePath, fragment] = refPath.split("#");
      const referencedPath = resolve(currentDir, referencedFilePath);
      const referencedContent = extractTopLevelNode(inlineLocalRefs(referencedPath), fragment);

      return indentContent(referencedContent, indentation).split(/\r?\n/);
    })
    .join("\n");
}

writeFileSync(target, `---\n${inlineLocalRefs(source).trimEnd()}\n`);
