#!/usr/bin/env bash
# Lighthouse (mobile, simulated 4G + 4x CPU slowdown) against a running production build.
# Usage: ./lighthouse.sh [base-url]   (default http://localhost:3000)
set -euo pipefail
BASE="${1:-http://localhost:3000}"
mkdir -p results/lighthouse
for page in "/" "/courses" "/courses/sql-for-analysts" "/login"; do
  name=$(echo "$page" | sed 's#^/$#home#; s#^/##; s#/#_#g')
  npx lighthouse "$BASE$page" --quiet --chrome-flags="--headless=new --no-sandbox" \
    --output=json --output-path="results/lighthouse/$name.json" >/dev/null
  node -e '
    const r = require("./results/lighthouse/'"$name"'.json");
    const s = (k) => Math.round(r.categories[k].score * 100);
    const a = r.audits;
    console.log(`'"$page"'`.padEnd(28), "perf", s("performance"), " a11y", s("accessibility"),
      " best", s("best-practices"), " seo", s("seo"),
      " | LCP", a["largest-contentful-paint"].displayValue, " TBT", a["total-blocking-time"].displayValue,
      " CLS", a["cumulative-layout-shift"].displayValue);'
done
