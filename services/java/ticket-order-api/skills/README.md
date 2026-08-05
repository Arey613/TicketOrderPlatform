# Skills

Reusable agent skills for the `ticket-order-api` service live here.

Use this folder for Java API-specific workflows such as:

- adding a new use case
- adding an inbound web adapter
- adding an outbound adapter
- updating CORS or security configuration
- preparing the API Docker image

Every new skill should follow the service architecture and testing conventions in
`../AGENTS.md`, including the controller integration-test approach for
`@SpringBootTest`, `MockMvc`, and test port overrides.
