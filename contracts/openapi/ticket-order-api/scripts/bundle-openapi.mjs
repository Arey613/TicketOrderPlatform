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
      const referencedPath = resolve(currentDir, refPath);
      const referencedContent = inlineLocalRefs(referencedPath);

      return indentContent(referencedContent, indentation).split(/\r?\n/);
    })
    .join("\n");
}

writeFileSync(target, `---\n${inlineLocalRefs(source).trimEnd()}\n`);
