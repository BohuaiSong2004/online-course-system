// Write-path concurrency: N learners enroll in the same course and complete the same chapters at the
// same moment. Checks for errors and that every learner ends with exactly one enrollment and
// consistent progress (unique keys + idempotent writes, no deadlocks surfacing as 5xx).
// Usage: k6 run -e API=http://localhost:8080 -e VUS=200 k6/enroll-burst.js
import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";

const API = __ENV.API || "http://localhost:8080";
const VUS = Number(__ENV.VUS || 200);
const COURSE_SLUG = "effective-code-review";
const json = { headers: { "Content-Type": "application/json" } };

export const options = {
  scenarios: { burst: { executor: "per-vu-iterations", vus: VUS, iterations: 1, maxDuration: "2m" } },
  thresholds: { http_req_failed: ["rate==0"], checks: ["rate==1"] },
  summaryTrendStats: ["avg", "med", "p(95)", "p(99)", "max"],
  setupTimeout: "5m",
};

export function setup() {
  const run = Date.now().toString(36);
  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    const r = http.post(`${API}/api/auth/register`, JSON.stringify({
      email: `load-${run}-${i}@example.com`, password: "password123", displayName: `Load ${i}`,
    }), json);
    tokens.push(r.json("token"));
  }
  const course = http.get(`${API}/api/courses/${COURSE_SLUG}`).json();
  return { tokens, courseId: course.course.id, chapterIds: course.chapters.map((c) => c.id) };
}

export default function (s) {
  const auth = { headers: { Authorization: `Bearer ${s.tokens[exec.vu.idInTest - 1]}`, "Content-Type": "application/json" } };
  // Enroll twice concurrently-ish (idempotency under contention), then complete every chapter.
  const responses = http.batch([
    ["POST", `${API}/api/courses/${s.courseId}/enroll`, null, auth],
    ["POST", `${API}/api/courses/${s.courseId}/enroll`, null, auth],
  ]);
  check(responses[0], { "enroll ok": (r) => r.status === 200 });
  check(responses[1], { "re-enroll ok": (r) => r.status === 200 });
  let last;
  for (const id of s.chapterIds) {
    last = http.post(`${API}/api/chapters/${id}/complete`, null, auth);
    check(last, { "complete ok": (r) => r.status === 200 });
  }
  check(last, { "course completed + certificate": (r) => r.json("courseCompleted") === true && !!r.json("certificateCode") });
  const mine = http.get(`${API}/api/me/enrollments`, auth).json();
  check(mine, { "exactly one enrollment": (m) => m.filter((e) => e.courseId === s.courseId).length === 1 });
}

export function handleSummary(data) {
  const m = data.metrics;
  const line = `learners ${VUS}  requests ${m.http_reqs.values.count}  failed ${(m.http_req_failed.values.rate * 100).toFixed(2)}%  ` +
    `checks ${(m.checks.values.rate * 100).toFixed(1)}%  p95 ${m.http_req_duration.values["p(95)"].toFixed(1)}ms  ` +
    `max ${m.http_req_duration.values.max.toFixed(1)}ms\n`;
  return { [__ENV.OUT || "results/enroll-burst.json"]: JSON.stringify(data, null, 2), stdout: line };
}
