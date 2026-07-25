# ticket-order-api-contract

OpenAPI contract module for the Ticket Service API.

## Development

Edit the API contract in:

```text
openapi.yml
```

Keep operation IDs stable because generators use them for method names.

## Test

Validate the contract from the repository root:

```sh
make validate-contracts
```

## Generate

Generate API code for consumer modules:

```sh
make generate
```

Generated files are build artifacts and are not committed.
