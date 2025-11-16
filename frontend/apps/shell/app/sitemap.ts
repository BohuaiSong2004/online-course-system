import type { MetadataRoute } from "next";
import { BACKEND_URL, SITE_URL } from "@ocs/shared/server";

// Rendered per request, with the course list kept in the data cache for an hour. A static sitemap would
// be baked at build time — when the API usually isn't running (e.g. docker build) — and list no courses.
export const dynamic = "force-dynamic";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const pages: MetadataRoute.Sitemap = [
    { url: `${SITE_URL}/`, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/courses`, changeFrequency: "daily", priority: 0.9 },
  ];
  try {
    const res = await fetch(`${BACKEND_URL}/api/courses/sitemap`, { next: { revalidate: 3600 } });
    const courses: { slug: string; updatedAt: string }[] = res.ok ? await res.json() : [];
    for (const c of courses) {
      pages.push({ url: `${SITE_URL}/courses/${c.slug}`, lastModified: c.updatedAt, changeFrequency: "weekly", priority: 0.8 });
    }
  } catch {
    /* API down: still serve the static entries */
  }
  return pages;
}
