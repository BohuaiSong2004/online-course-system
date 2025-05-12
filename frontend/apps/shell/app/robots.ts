import type { MetadataRoute } from "next";
import { SITE_URL } from "@ocs/shared/server";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: "*", allow: ["/", "/courses"], disallow: ["/learn", "/studio", "/backend", "/login", "/register"] }],
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
