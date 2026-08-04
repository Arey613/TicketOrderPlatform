# AGENTS.md

Agent guidance for the `ticket-order-api-contract` module.

## Scope

This file applies to `contracts/openapi/ticket-order-api`.

## Development

- Commit messages for this module must follow `TICKET_CONTRACTS#123 - short description`.
- Update this module's `CHANGELOG.md` before every commit that changes this module.
- Also update the root `CHANGELOG.md` under the `ticket-order-api-contract` module section.
- Keep the OpenAPI source in YAML.
- Keep generated API code out of this module.
- Use stable `operationId` values because downstream generators depend on them.
- Contract-first API changes must update `openapi.yml` before generated code or implementations.

## Test

- Validate the contract from the repository root:

```sh
make validate-contracts
```

## Deploy

- This module is not deployed as a runtime service.
- Publish or package contracts only when a release workflow is introduced.

## Next Iterations

- Add schemas under `components.schemas` before duplicating inline response bodies.
- Add security schemes here before implementing authentication flows in services.
- Update generators in service modules whenever contract output requirements change.
