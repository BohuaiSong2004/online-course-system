# Performance and quality measurements

All numbers below were measured on 2026-11-22 on a single 2-vCPU Linux dev container. The API (Java 21), MySQL 8.0 and the load generator all shared that machine, so absolute throughput is far below what a real deployment would reach. What these numbers do show is **relative** effects, measured under identical conditions. Re-run them with the scripts in `perf/` and `scripts/`.

## Database index: activity feed

`scripts/benchmark_indexes.py`: 1,000,000 activity rows for 5,000 users. It times the feed query from `ActivityMapper.xml` (`WHERE user_id = ? ORDER BY created_at DESC LIMIT 20`):

| | Plan | Mean | p95 |
|---|---|---|---|
| No index | full scan + filesort, ~1M rows examined | 176.8 ms | 192.6 ms |
| `(user_id, created_at)` | index range scan, ~200 rows | 0.37 ms | 0.50 ms |

## MyBatis second-level cache: catalog reads

`perf/k6/catalog.js`: 50 virtual users for 30 s. Each iteration does a catalog page, a course detail and a search, sent straight to the API. To turn the cache off for the comparison, set `MYBATIS_CONFIGURATION_CACHE_ENABLED=false`.

| | Requests/s | p50 | p95 | p99 | Errors |
|---|---|---|---|---|---|
| Cache on (run 1) | 2,026 | 10.4 ms | 92.7 ms | 127.9 ms | 0% |
| Cache on (run 2) | 1,672 | 11.0 ms | 107.7 ms | 157.3 ms | 0% |
| Cache off | 600 | 70.5 ms | 168.1 ms | 247.6 ms | 0% |

With the cache on, throughput is **~2.8–3.4× higher** and median latency **~6–7× lower** on the same hardware.

## Concurrent writes: 200 learners at once

`perf/k6/enroll-burst.js`: 200 learners each enroll twice (on purpose, at the same moment) in the same course, then complete every chapter. Each one then checks their own enrollment list.

| Learners | Requests | Failed | Consistency checks | p95 | Max |
|---|---|---|---|---|---|
| 200 | 1,201 | 0 | 100% | 1.8 s | 3.1 s |

The consistency checks confirm that every learner ends with exactly one enrollment, a completed course and a certificate. The API log shows no deadlocks or errors. The higher p95 comes from 2 vCPUs handling 200 simultaneous write transactions while also running MySQL and the load generator.

## Lighthouse: public pages

`perf/lighthouse.sh`. Lighthouse 13 with its mobile defaults (simulated slow 4G, 4× CPU slowdown), run against a production build (`next build && next start`):

| Page | Performance | Accessibility | Best practices | SEO | LCP | TBT | CLS |
|---|---|---|---|---|---|---|---|
| `/` | 99 | 100 | 100 | 100 | 0.8 s | 130 ms | 0 |
| `/courses` | 99 | 100 | 100 | 100 | 0.8 s | 100 ms | 0 |
| `/courses/sql-for-analysts` | 99 | 100 | 100 | 100 | 0.8 s | 100 ms | 0 |
| `/login` | 100 | 100 | 100 | 63\* | 0.8 s | 90 ms | 0 |

\* The login page is `noindex` on purpose.

The home page and course pages are statically generated and revalidated every 60 s (ISR). Personal state loads in the browser.

## Tests

| Suite | Count | Notes |
|---|---|---|
| Backend unit + integration (JUnit, MockMvc, real MySQL and Redis) | 39 | JaCoCo: 90.6% lines, 68.2% branches |
| Browser end-to-end (Playwright) | 21 | Includes axe-core WCAG 2.1 AA scans (no serious or critical issues) and a phone viewport |

All of them run in GitHub Actions on every push.
