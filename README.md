# Resilience4j Patterns Lab

Practical lab project built with **Java 17**, **Spring Boot 3.4.5** and **Resilience4j 2.2.0** to explore and implement the main resilience patterns in a professional setup. Uses [JSONPlaceholder](https://jsonplaceholder.typicode.com) as external API to simulate real-world HTTP calls.

---

## Patterns Implemented

### Retry
Automatically retries failed calls caused by transient faults (5xx errors, IOExceptions).
Configured with exponential backoff (500ms → 1000ms → 2000ms) and a clear separation between
retryable exceptions and ignored ones (4xx errors are never retried).
Combined with Circuit Breaker using explicit `aspectOrder` to ensure the CB sees the final
result of the Retry, not each individual attempt.

### Circuit Breaker
Monitors accumulated failures using a count-based sliding window. Once the failure rate exceeds
the threshold, the circuit opens and rejects calls immediately without hitting the external service.
Transitions through three states: **CLOSED** → **OPEN** → **HALF_OPEN**.
Exposes real-time metrics via Spring Actuator (`/actuator/health`, `/actuator/circuitbreakerevents`)
and a custom status endpoint (`/circuit-breakers/{name}/status`).

### Bulkhead
Limits the maximum number of concurrent calls to protect application threads from being exhausted
by slow external services. Implemented on a dedicated endpoint (`/api/posts/{id}/comments`) to
avoid aspect order conflicts when combined with other patterns.
Calls exceeding the limit are rejected immediately with a 503-equivalent fallback re

### TimeLimiter
Cancels async operations that exceed a configured timeout (2s). Requires `CompletableFuture`
as return type — this is the only mechanism that allows an in-progress operation to be
cancelled. Combined with Circuit Breaker on a dedicated async endpoint (`/api/posts/{id}/async`).

---

## Key Concepts

- **Aspect ordering** — combining CB + Retry requires explicit `aspectOrder` configuration,
  otherwise R4j's default order prevents the CB from accumulating failures correctly.
- **Fault Simulator** — dedicated component to trigger controlled failures at runtime via HTTP,
  without modifying business logic or external dependencies.
- **Testing strategy** — `@SpringBootTest` is required for AOP-based annotations to work.
  CB instances are recreated programmatically in `@BeforeEach` to avoid state leaking between tests.
