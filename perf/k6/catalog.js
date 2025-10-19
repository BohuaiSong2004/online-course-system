// Read-heavy catalog traffic straight at the API: listing, search and course detail.
// Usage: k6 run -e API=http://localhost:8080 k6/catalog.js
import http from "k6/http";
import { check } from "k6";

const API = __ENV.API || "http://localhost:8080";
const SLUGS = ["java-spring-boot-fundamentals", "react-and-nextjs-for-internal-tools", "workplace-security-essentials",
  "sql-for-analysts", "effective-code-review", "communicating-as-a-new-engineer"];
const QUERIES = ["spring", "sql", "security", "review", "next"];

export const options = {
  scenarios: {
    catalog: { executor: "constant-vus", vus: Number(__ENV.VUS || 50), duration: __ENV.DURATION || "30s" },
  },
  thresholds: { http_req_failed: ["rate<0.01"], http_req_duration: ["p(95)<500"] },
  summaryTrendStats: ["avg", "med", "p(95)", "p(99)", "max"],
};

export default function () {
  const r1 = http.get(`${API}/api/courses?page=1&size=12`, { tags: { name: "list" } });
  const r2 = http.get(`${API}/api/courses/${SLUGS[Math.floor(Math.random() * SLUGS.length)]}`, { tags: { name: "detail" } });
  const r3 = http.get(`${API}/api/courses?q=${QUERIES[Math.floor(Math.random() * QUERIES.length)]}`, { tags: { name: "search" } });
  check(r1, { "list 200": (r) => r.status === 200 });
  check(r2, { "detail 200": (r) => r.status === 200 });
  check(r3, { "search 200": (r) => r.status === 200 });
}

export function handleSummary(data) {
  const out = __ENV.OUT || "results/catalog.json";
  return { [out]: JSON.stringify(data, null, 2), stdout: textSummary(data) };
}

function textSummary(d) {
  const m = d.metrics;
  return `requests ${m.http_reqs.values.count}  rps ${m.http_reqs.values.rate.toFixed(0)}  ` +
    `p50 ${m.http_req_duration.values.med.toFixed(1)}ms  p95 ${m.http_req_duration.values["p(95)"].toFixed(1)}ms  ` +
    `p99 ${m.http_req_duration.values["p(99)"].toFixed(1)}ms  failed ${(m.http_req_failed.values.rate * 100).toFixed(2)}%\n`;
}
