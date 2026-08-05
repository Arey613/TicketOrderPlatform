# Changelog

All notable changes are tracked here using the repository commit message format:

```text
MODULE_NAME#123 - short description
```

Entries are grouped by module, then by change type. Within each change type, entries read from oldest to newest.

## Repository

### Added

- GLOBAL_CONFIG#1 - add scoped agent guidance and skill folders
- GLOBAL_CONFIG#2 - add OpenAPI contract workflow
- GLOBAL_CONFIG#3 - add GitHub Actions CI validation
- GLOBAL_CONFIG#4 - add project navigation guides
- GLOBAL_CONFIG#5 - add Minikube local Kubernetes deployment
- GLOBAL_CONFIG#6 - add formatter validation

### Changed

- GLOBAL_CONFIG#4 - add pull request iteration guidance
- GLOBAL_CONFIG#5 - organize changelog entries

## ticket-order-api

### Added

- GLOBAL_CONFIG#1 - add scoped agent guidance and skill folders
- TICKET_SERVICE#1 - add user persistence and postgres wiring
- TICKET_SERVICE#2 - add OpenAPI MapStruct mapping
- TICKET_SERVICE#4 - add session login authentication
- TICKET_SERVICE#4 - add Lombok annotation configuration

### Changed

- TICKET_SERVICE#3 - align user persistence mapping with users relation
- TICKET_SERVICE#4 - replace hello smoke endpoint with actuator health

## ticket-order-api-contract

### Added

- TICKET_CONTRACTS#1 - add ticket service OpenAPI contract

### Changed

- TICKET_CONTRACTS#2 - align contract with login authentication

## ticket-order-db-migrations

### Added

- DATABASE_MIGRATIONS#1 - add Java database migration modules

### Changed

- DATABASE_MIGRATIONS#2 - rename application user migrations

## ticket-order-web

### Added

- GLOBAL_CONFIG#1 - add scoped agent guidance and skill folders
- TICKET_PORTAL#1 - add OpenAPI client generation
