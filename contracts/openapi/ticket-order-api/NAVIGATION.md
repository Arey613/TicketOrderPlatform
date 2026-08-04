# API Contract Navigation

Fast local guide for contract-only work. Read this module's `AGENTS.md` first.

## Start Here

| Need | Path |
| --- | --- |
| OpenAPI source | `openapi.yml` |
| Module build | `pom.xml` |
| Module docs | `README.md` |

## Search

| Goal | Command |
| --- | --- |
| Files | `rg --files` |
| Paths | `rg -n "^  /" openapi.yml` |
| Operations | `rg -n "operationId:" openapi.yml` |
| Schemas/refs | `rg -n "components:|schemas:|\\$ref:|schema:" openapi.yml` |
| Security | `rg -n "securitySchemes:|security:|bearer|oauth|apiKey" openapi.yml` |

## Commands

Run from the repository root.

| Need | Command |
| --- | --- |
| Validate contract | `make validate-contracts` |
| Generate all consumers | `make generate` |
| Generate Java sources | `make generate-api-contracts` |
| Generate web client | `make generate-web-contracts` |

## Rules Of Thumb

- `openapi.yml` is the API source of truth.
- Keep `operationId` values stable.
- Prefer shared `components.schemas` over duplicated inline schemas.
- Update the contract before generated code or implementation.
