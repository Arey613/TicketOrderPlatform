# Notification Service Inbox Analysis

Status: analysis only. This document is for discussion and is not an accepted specification. It is not intended to be merged.

## Purpose

Analyze a new `notification-service` written in Node.js (TypeScript) that receives notification requests through a transactional inbox and processes them in batches. The service is also an experiment in comparing language capabilities across new services.

## Background and Goals

- Add services alongside the Java `ticket-order-api` in different languages to compare their capabilities.
- Candidate set discussed: Node.js (notification), Go (payment), Python (AI agent tool), Kotlin (background worker).
- Notification is a good fit for Node.js because its work is I/O bound: calling external providers and waiting for responses.
- The background worker is a good fit for Kotlin because it shares the JVM, Spring, Flyway, and observability setup with the Java API.

## Scope of the First Slice

In scope:

- HTTP-fed transactional inbox with batch insert and deduplication.
- Worker loop that claims pending rows, sends them in batches, and records results.
- Retry with backoff and a terminal dead state.
- Fake email provider behind an outbound port.

Out of scope for the first slice:

- Kafka consumption.
- Real email providers (Nodemailer, SES, and similar).
- Publishing events from the Java API (outbox on the producer side).
- Other services (payment, AI agent tool, background worker).

## Inbox Pattern

Kafka and most queues deliver at-least-once, so the same message can arrive twice. The inbox makes handling idempotent.

1. Receive a batch and insert every message into an `inbox` table in one database transaction, keyed by message ID, using `INSERT ... ON CONFLICT DO NOTHING`. Duplicates are skipped silently.
2. Acknowledge the source (HTTP response now, Kafka offset commit later) only after that transaction commits.
3. A separate worker claims pending rows with `SELECT ... FOR UPDATE SKIP LOCKED LIMIT N`, so several instances can run without contention.
4. The worker sends each notification outside the claim transaction, then records the result in a short second transaction.

Remaining gap: the provider call and the status update cannot be atomic. A crash between them can cause a rare duplicate send unless the provider call is idempotent. This limitation exists in every language.

## Data Model (proposed)

Own schema, `ticket_notifications`, separate from the Java API schemas.

`inbox` table:

| Column | Purpose |
| --- | --- |
| `id` | Message ID, unique, used for deduplication |
| `payload` | Notification content and recipient |
| `status` | `PENDING`, `SENT`, `FAILED`, `DEAD` |
| `attempts` | Number of send attempts |
| `next_attempt_at` | Earliest time of the next attempt, used for backoff |
| `created_at`, `updated_at` | Audit timestamps |

Status flow: `PENDING` to `SENT`; on failure `PENDING` with a later `next_attempt_at` and incremented `attempts`; after the maximum attempts, `DEAD` for manual inspection.

## Batching

- Receiving: `POST /notifications/batch` validates the body with `zod` and inserts all rows with one multi-row statement. The response reports accepted and duplicate counts.
- Sending: the worker processes a claimed batch with bounded concurrency (for example 20 in flight) using a small promise pool. Providers that offer bulk send endpoints can reduce calls further.
- Transactions stay short. Rows are claimed in one transaction and updated in another. No database lock is held while waiting on a provider.

## Node.js Capabilities Relevant to This Design

Strengths:

- Non-blocking I/O handles many in-flight provider calls on one thread.
- Mature SDKs for email, SMS, and push providers.
- Fast startup, small images, low memory per instance.
- Same Node version and tooling as the web app (`v26.5.0`).

Limitations:

- CPU-heavy work blocks the event loop. Keep template rendering light or use `worker_threads`.
- No built-in transaction management, listener containers, or retry and dead-letter handling as in Spring. These are written by hand.
- TypeScript types are erased at runtime, so external input must be validated.
- Layering such as the hexagonal structure is a convention, not enforced.
- JavaScript numbers are floating point; money needs integer cents or a decimal library if it is ever involved.

## Kafka (later step)

Node supports Kafka through KafkaJS (pure JavaScript, `eachBatch`) or Confluent's `@confluentinc/kafka-javascript` (native librdkafka, official support). Mapping to Spring Kafka:

| Spring Kafka | Node |
| --- | --- |
| `@KafkaListener` | `consumer.run({ eachBatch })` |
| `KafkaTemplate.send` | `producer.send` |
| Manual offset commit | Supported, written explicitly |
| Retry and dead-letter handling | Built by hand, for example a dead-letter topic |

Rules when added: commit the offset only after the inbox insert commits, key messages by user or order ID to keep related notifications in one partition, validate every message before inserting, and send invalid messages to a dead-letter topic.

## Email Integration

Email is sent through SMTP (Nodemailer, comparable to `JavaMailSender`) or a provider HTTP API through its SDK (for example SES). For local development, Mailpit provides a fake SMTP server with a web inbox, and LocalStack (already in `docker-compose.yml`) can emulate SES. The first slice uses a fake provider behind an outbound port, so a real provider can be swapped in as an adapter.

## Proposed Module Layout

`services/node/notification-service`, with layers mirroring the hexagonal layout used by the Java API:

- `domain` - framework-free notification and status types
- `application` - use cases (accept batch, process pending)
- `adapters/in/http` - Fastify routes and validation
- `adapters/out/persistence` - Postgres inbox repository
- `adapters/out/email` - provider adapters (fake first)
- `config` - environment configuration

## Implementation Steps (if approved)

1. Create a work branch from the latest `origin/main`.
2. Add a Flyway one-shot Compose container and the `ticket_notifications` schema with the `inbox` table.
3. Scaffold the Node module: TypeScript, Fastify, `pg`, `zod`, `p-limit`, Vitest.
4. Implement the batch endpoint and the worker loop.
5. Add the fake email adapter behind an outbound port.
6. Add unit tests for retry and deduplication, and one integration test against Postgres.
7. Add module `AGENTS.md`, `README.md`, `NAVIGATION.md`, `CHANGELOG.md`, and a health endpoint.
8. Wire the root: Makefile targets, Dockerfile, Compose service, `docs/PROJECT_MAP.md`, root changelog.
9. Validate with the smallest relevant targets first, then `make test` and `docker compose config`.

## Decisions Still Open

- Migration tooling: reuse Flyway in a one-shot container (recommended, matches the existing pattern) or a Node-native tool.
- Whether Kafka belongs in the first slice or a later one.
- Where notification events originate; the Java API would need to publish them, ideally through its own outbox, as a separate ticket.
- Ticket number and module name for the implementation work.
- Approval for dependency installs from npm, with an exact package and version list, before anything is installed.

## Risks

- A second schema and migration stream adds operational surface.
- Hand-written retry and offset logic is a common source of bugs; it needs focused tests.
- Comparing languages is only meaningful if the compared services do comparable work. Services with different jobs compare the jobs, not the languages.
