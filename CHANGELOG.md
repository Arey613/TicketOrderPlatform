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
- BUGFIX#2 - add auth schema and role-aware UI correction spec

### Changed

- GLOBAL_CONFIG#4 - add pull request iteration and style guidance
- GLOBAL_CONFIG#5 - organize changelog entries
- TICKET_CONTRACTS#3 - add split OpenAPI contract bundling workflow
- GLOBAL_CONFIG#7 - require latest main for new branches
- BUGFIX#2 - add explicit primary and analytical datasource configuration

## ticket-order-api

### Added

- GLOBAL_CONFIG#1 - add scoped agent guidance and skill folders
- TICKET_SERVICE#1 - add user persistence and postgres wiring
- TICKET_SERVICE#2 - add OpenAPI MapStruct mapping
- TICKET_SERVICE#4 - add session login authentication and spec
- TICKET_SERVICE#5 - add registration authentication flow
- TICKET_SERVICE#6 - add event management specification
- TICKET_SERVICE#6 - add event management API implementation
- TICKET_SERVICE#7 - add backend observability

### Changed

- TICKET_SERVICE#3 - align user persistence mapping with users relation
- TICKET_SERVICE#4 - replace hello smoke endpoint with actuator health
- TICKET_SERVICE#6 - address event management review comments
- TICKET_SERVICE#7 - address observability review comments
- BUGFIX#2 - keep operational persistence on the primary transactional datasource
- TICKET_PORTAL#3 - add customer-owned event-order backend behavior

## ticket-order-api-contract

### Added

- TICKET_CONTRACTS#1 - add ticket service OpenAPI contract
- TICKET_CONTRACTS#4 - add UI-facing event and auth contract fields
- TICKET_CONTRACTS#3 - add event management contract

### Changed

- TICKET_CONTRACTS#2 - align contract with login authentication
- TICKET_SERVICE#5 - add registration authentication flow
- TICKET_SERVICE#6 - address event management review comments
- TICKET_PORTAL#3 - add booked-place ownership hints to event contract

## ticket-order-db-migrations

### Added

- DATABASE_MIGRATIONS#1 - add Java database migration modules
- DATABASE_MIGRATIONS#3 - add event management schema

### Changed

- DATABASE_MIGRATIONS#2 - rename application user migrations
- TICKET_SERVICE#6 - address event management review comments
- BUGFIX#2 - move migration schema selection to Maven profiles
- TICKET_PORTAL#3 - add transactional event-order customer ownership

## ticket-order-web

### Added

- GLOBAL_CONFIG#1 - add scoped agent guidance and skill folders
- TICKET_PORTAL#1 - add OpenAPI client generation
- TICKET_PORTAL#2 - add initial TypeScript Tailwind ticketing UI, startup fixes, and review guidance

### Changed

- BUGFIX#1 - fix Docker image builds and Compose startup
- BUGFIX#2 - hide auth actions after login and add role-aware home actions
- TICKET_PORTAL#3 - connect customer event browsing and booking UI to API
