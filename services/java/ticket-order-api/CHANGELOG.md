# Changelog

Changes for `ticket-order-api`.

Commit messages must follow:

```text
TICKET_SERVICE#123 - short description
```

## Unreleased

### Added

- GLOBAL_CONFIG#1 - add scoped agent guidance and skill folders
- TICKET_SERVICE#1 - add user persistence and postgres wiring
- TICKET_SERVICE#2 - add OpenAPI MapStruct mapping
- TICKET_SERVICE#4 - add session login authentication
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
