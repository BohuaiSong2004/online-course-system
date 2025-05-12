#!/usr/bin/env python3
"""Generates backend/src/main/resources/db/data.sql (demo users, courses, chapters).

Usage: python3 scripts/gen_seed.py   (requires `pip install bcrypt`)
All demo accounts use the password: password123
"""
import pathlib
import bcrypt

PASSWORD = "password123"
OUT = pathlib.Path(__file__).resolve().parent.parent / "backend/src/main/resources/db/data.sql"

USERS = [
    (1, "admin@demo.dev", "Ada Admin", "ADMIN"),
    (2, "instructor@demo.dev", "Ivy Instructor", "INSTRUCTOR"),
    (3, "learner@demo.dev", "Leo Learner", "LEARNER"),
    (4, "mentor@demo.dev", "Marco Mentor", "INSTRUCTOR"),
]

COURSES = [
    dict(id=1, slug="java-spring-boot-fundamentals", title="Java & Spring Boot Fundamentals",
         category="Engineering", level="BEGINNER", instructor=2,
         summary="Build and ship a production-style REST API with Spring Boot, from project setup to testing.",
         description="A hands-on onboarding track for new backend engineers. You will build a small REST service, "
                     "wire it to MySQL, and learn the conventions our teams use in code review.",
         chapters=[
             ("Welcome & environment setup", 8, True,
              "## Goals\n\n- Install JDK 17+ and Maven\n- Generate a project with Spring Initializr\n- Run it locally\n\n"
              "```bash\n./mvnw spring-boot:run\n```\n\nOpen `http://localhost:8080/actuator/health` and you should see `UP`."),
             ("Controllers and request mapping", 15, False,
              "## REST controllers\n\nA controller maps HTTP routes to Java methods.\n\n"
              "```java\n@RestController\n@RequestMapping(\"/api/greetings\")\nclass GreetingController {\n"
              "    @GetMapping(\"/{name}\")\n    Map<String, String> hello(@PathVariable String name) {\n"
              "        return Map.of(\"message\", \"Hello, \" + name);\n    }\n}\n```\n\n"
              "**Exercise:** add a `POST` endpoint that validates its body with `@Valid`."),
             ("Persistence with MyBatis", 20, False,
              "## Why MyBatis\n\nMyBatis keeps SQL explicit, which makes query plans easy to reason about.\n\n"
              "1. Define a mapper interface\n2. Write the SQL in XML\n3. Enable `map-underscore-to-camel-case`\n\n"
              "Use `EXPLAIN` on every new query that touches a large table."),
             ("Testing your API", 18, False,
              "## Test pyramid\n\n- Unit tests for services\n- `MockMvc` tests for controllers\n- A few end-to-end checks\n\n"
              "Keep tests deterministic: no shared mutable state between test methods."),
         ]),
    dict(id=2, slug="react-and-nextjs-for-internal-tools", title="React & Next.js for Internal Tools",
         category="Engineering", level="INTERMEDIATE", instructor=4,
         summary="Server-side rendering, routing and data fetching patterns for fast internal dashboards.",
         description="Learn the App Router, server components and when to reach for client components.",
         chapters=[
             ("Server vs client components", 12, True,
              "## Mental model\n\nServer components render on the server and ship **no JavaScript** by default. "
              "Mark a file with `'use client'` only when it needs state, effects or browser APIs."),
             ("Data fetching and caching", 16, False,
              "## fetch() on the server\n\n```ts\nconst res = await fetch(url, { next: { revalidate: 60 } });\n```\n\n"
              "`revalidate` gives you incremental static regeneration for catalog-style pages."),
             ("Styling with Tailwind", 10, False,
              "## Utility-first CSS\n\nCompose small utilities instead of writing bespoke CSS files. "
              "Extract repeated patterns into components, not into `@apply` soup."),
         ]),
    dict(id=3, slug="workplace-security-essentials", title="Workplace Security Essentials",
         category="Compliance", level="BEGINNER", instructor=2,
         summary="Phishing, passwords, device hygiene and incident reporting in 30 minutes.",
         description="Required for all new hires. Covers the security basics every employee is expected to follow.",
         chapters=[
             ("Spotting phishing", 8, True,
              "## Red flags\n\n- Urgency and threats\n- Mismatched sender domains\n- Unexpected attachments\n\n"
              "When in doubt, report the message instead of clicking."),
             ("Passwords and MFA", 7, False,
              "## Rules of thumb\n\n1. Use the company password manager\n2. Never reuse passwords\n3. Turn on MFA everywhere"),
             ("Reporting an incident", 6, False,
              "## What to do\n\nDisconnect from the network if you suspect malware, then contact the security team. "
              "Speed matters more than certainty."),
         ]),
    dict(id=4, slug="sql-for-analysts", title="SQL for Analysts",
         category="Data", level="BEGINNER", instructor=4,
         summary="SELECT to window functions: answer business questions straight from the warehouse.",
         description="Practical SQL with real-world examples drawn from product analytics.",
         chapters=[
             ("SELECT, WHERE, ORDER BY", 10, True,
              "## Your first query\n\n```sql\nSELECT id, title FROM courses WHERE status = 'PUBLISHED' ORDER BY published_at DESC;\n```"),
             ("Joins without tears", 14, False,
              "## INNER vs LEFT JOIN\n\nA `LEFT JOIN` keeps every row from the left table, filling missing matches with `NULL`."),
             ("Indexes and query plans", 15, False,
              "## B-tree indexes\n\nA composite index on `(user_id, created_at)` lets MySQL find one user's rows and read them "
              "already sorted. Check with `EXPLAIN` and look for `Using filesort`."),
             ("Window functions", 16, False,
              "## Running totals\n\n```sql\nSELECT day, SUM(signups) OVER (ORDER BY day) AS cumulative FROM daily_signups;\n```"),
         ]),
    dict(id=5, slug="effective-code-review", title="Effective Code Review",
         category="Engineering", level="INTERMEDIATE", instructor=2,
         summary="Give and receive review feedback that improves code without slowing the team down.",
         description="Conventions for pull requests, review etiquette and what to look for.",
         chapters=[
             ("Writing a reviewable PR", 9, True,
              "## Small and focused\n\nAim for PRs a reviewer can read in under 20 minutes. Describe *why*, not just *what*."),
             ("What reviewers look for", 12, False,
              "## Checklist\n\n- Correctness and edge cases\n- Tests\n- Naming and readability\n- Security (input validation, authz)"),
         ]),
    dict(id=6, slug="communicating-as-a-new-engineer", title="Communicating as a New Engineer",
         category="Professional Skills", level="BEGINNER", instructor=4,
         summary="Stand-ups, design docs and asking good questions during your first 90 days.",
         description="Soft skills that make onboarding faster for you and your team.",
         chapters=[
             ("Asking good questions", 8, True,
              "## The template\n\n1. What I am trying to do\n2. What I tried\n3. What happened vs what I expected"),
             ("Writing a one-page design doc", 14, False,
              "## Sections\n\nContext, goals and non-goals, proposal, alternatives, open questions."),
             ("Running a stand-up update", 6, False,
              "## Yesterday / today / blockers\n\nKeep it under a minute. Take deep dives offline."),
         ]),
]

# One unpublished draft so the studio view has something to edit.
DRAFT = dict(id=7, slug="kubernetes-basics-draft", title="Kubernetes Basics (draft)",
             category="Engineering", level="INTERMEDIATE", instructor=2,
             summary="Pods, deployments and services. Work in progress.",
             description="Draft course - not visible in the public catalog yet.",
             chapters=[("Pods and containers", 12, True, "## Coming soon\n\nOutline only.")])


def q(s):
    if s is None:
        return "NULL"
    return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    pw = bcrypt.hashpw(PASSWORD.encode(), bcrypt.gensalt(rounds=10, prefix=b"2a")).decode()
    lines = ["-- Demo seed data. Generated by scripts/gen_seed.py - do not edit by hand.",
             "-- Every demo account uses the password: password123", ""]
    for uid, email, name, role in USERS:
        lines.append(f"INSERT IGNORE INTO users (id, email, password_hash, display_name, role) VALUES "
                     f"({uid}, {q(email)}, {q(pw)}, {q(name)}, {q(role)});")
    lines.append("")
    chapter_id = 1
    for i, c in enumerate(COURSES + [DRAFT]):
        published = c is not DRAFT
        pub_at = f"DATE_SUB(NOW(3), INTERVAL {(len(COURSES) - i) * 3} DAY)" if published else "NULL"
        lines.append(
            "INSERT IGNORE INTO courses (id, slug, title, summary, description, category, level, instructor_id, status, published_at) "
            f"VALUES ({c['id']}, {q(c['slug'])}, {q(c['title'])}, {q(c['summary'])}, {q(c['description'])}, "
            f"{q(c['category'])}, {q(c['level'])}, {c['instructor']}, {q('PUBLISHED' if published else 'DRAFT')}, {pub_at});")
        for pos, (title, mins, preview, content) in enumerate(c["chapters"], start=1):
            lines.append(
                "INSERT IGNORE INTO chapters (id, course_id, title, content, position, duration_min, is_preview) "
                f"VALUES ({chapter_id}, {c['id']}, {q(title)}, {q(content)}, {pos}, {mins}, {'TRUE' if preview else 'FALSE'});")
            chapter_id += 1
        lines.append("")
    # Leo is enrolled in the first course and has finished chapter 1.
    lines += [
        "INSERT IGNORE INTO enrollments (id, user_id, course_id, status) VALUES (1, 3, 1, 'ACTIVE');",
        "INSERT IGNORE INTO chapter_progress (user_id, chapter_id, course_id) VALUES (3, 1, 1);",
        "",
    ]
    OUT.write_text("\n".join(lines))
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
