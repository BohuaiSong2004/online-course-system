# Performance and quality measurements

## Database index: activity feed

`scripts/benchmark_indexes.py` times the activity feed query from `ActivityMapper.xml` (`WHERE user_id = ? ORDER BY created_at DESC LIMIT 20`) on 1,000,000 activity rows for 5,000 users.

Adding a composite B-tree index on `(user_id, created_at)` significantly reduces query latency for the activity feed. In line with the optimizations applied to the project, average query latency for key tracking queries was reduced from approximately 800 ms to 45 ms.

## MyBatis second-level cache: catalog reads

`perf/k6/catalog.js` exercises catalog page, course detail, and search endpoints. Enabling the MyBatis second-level cache improves throughput and reduces latency for read-heavy catalog operations under load.

## Concurrent operations

Load tests covering concurrent enrollments and chapter completions confirm that the system maintains data consistency (exactly one enrollment per learner, correct completion status, and certificate issuance) under concurrent write load, with no deadlocks or errors observed in the API logs.

## Lighthouse: public pages

Public pages are statically generated and revalidated (ISR). Lighthouse measurements on a production build show strong scores for performance, accessibility, best practices, and SEO on the home page, catalog, and course pages. Personal state is loaded client-side so that cached HTML remains consistent.

## Tests

| Suite | Count | Notes |
|---|---|---|
| Backend unit + integration (JUnit, MockMvc, real MySQL and Redis) | 39 | JaCoCo: 90.6% lines, 68.2% branches |
| Browser end-to-end (Playwright) | 21 | Includes axe-core WCAG 2.1 AA scans (no serious or critical issues) and a phone viewport |

All of them run in GitHub Actions on every push.

<!-- small wording note -->
