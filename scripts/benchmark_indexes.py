#!/usr/bin/env python3
"""Measures the activity-feed query with and without the (user_id, created_at) B-tree index.

It builds a throwaway copy of user_activity (bench_activity) so the real table is untouched,
fills it with synthetic rows, then times the exact query used by ActivityMapper.recentForUser.

Usage:
    pip install pymysql
    python3 scripts/benchmark_indexes.py --rows 1000000 --users 5000
Environment: DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME (defaults match docker-compose).
"""
import argparse
import os
import random
import statistics
import time

import pymysql

QUERY = ("SELECT id, user_id, course_id, chapter_id, action, created_at FROM bench_activity "
         "WHERE user_id = %s ORDER BY created_at DESC LIMIT 20")


def connect():
    return pymysql.connect(host=os.getenv("DB_HOST", "127.0.0.1"), port=int(os.getenv("DB_PORT", "3306")),
                           user=os.getenv("DB_USER", "ocs"), password=os.getenv("DB_PASSWORD", "ocs"),
                           database=os.getenv("DB_NAME", "ocs"), autocommit=True)


def populate(cur, rows, users):
    cur.execute("DROP TABLE IF EXISTS bench_activity")
    cur.execute("CREATE TABLE bench_activity LIKE user_activity")
    cur.execute("ALTER TABLE bench_activity DROP INDEX idx_activity_user_created")
    t0 = time.time()
    # Generate rows server-side (much faster than shipping them over the wire).
    cur.execute("SET SESSION cte_max_recursion_depth = %s", (rows + 1,))
    cur.execute(
        "INSERT INTO bench_activity (user_id, course_id, chapter_id, action, created_at) "
        "WITH RECURSIVE seq (n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < %s) "
        "SELECT 1 + FLOOR(RAND(n) * %s), 1 + FLOOR(RAND(n + 1) * 50), 1 + FLOOR(RAND(n + 2) * 400), "
        "ELT(1 + FLOOR(RAND(n + 3) * 3), 'ENROLL', 'VIEW_CHAPTER', 'COMPLETE_CHAPTER'), "
        "TIMESTAMPADD(SECOND, FLOOR(RAND(n + 4) * 31536000), '2025-01-01') FROM seq",
        (rows, users))
    cur.execute("ANALYZE TABLE bench_activity")
    cur.fetchall()
    print(f"inserted {rows:,} rows for {users:,} users in {time.time() - t0:.1f}s")


def timed(cur, users, n):
    samples = []
    for _ in range(n):
        uid = random.randint(1, users)
        t = time.perf_counter()
        cur.execute(QUERY, (uid,))
        cur.fetchall()
        samples.append((time.perf_counter() - t) * 1000)
    samples.sort()
    return statistics.mean(samples), samples[int(len(samples) * 0.95) - 1]


def explain(cur):
    cur.execute("EXPLAIN " + QUERY, (1,))
    cols = [d[0] for d in cur.description]
    row = dict(zip(cols, cur.fetchone()))
    return f"type={row['type']} key={row['key']} rows={row['rows']} extra={row['Extra']}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--rows", type=int, default=1_000_000)
    ap.add_argument("--users", type=int, default=5_000)
    ap.add_argument("--queries", type=int, default=50)
    args = ap.parse_args()
    random.seed(7)
    with connect() as conn, conn.cursor() as cur:
        populate(cur, args.rows, args.users)

        print("\nWITHOUT index:", explain(cur))
        before = timed(cur, args.users, args.queries)
        print(f"  mean {before[0]:.1f} ms   p95 {before[1]:.1f} ms")

        cur.execute("ALTER TABLE bench_activity ADD INDEX idx_activity_user_created (user_id, created_at)")
        print("\nWITH (user_id, created_at) index:", explain(cur))
        after = timed(cur, args.users, args.queries)
        print(f"  mean {after[0]:.2f} ms   p95 {after[1]:.2f} ms")

        print(f"\nspeed-up: {before[0] / after[0]:.0f}x on mean latency")
        cur.execute("DROP TABLE bench_activity")


if __name__ == "__main__":
    main()
