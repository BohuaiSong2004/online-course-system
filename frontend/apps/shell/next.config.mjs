import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../..");

/** @type {import('next').NextConfig} */
const nextConfig = {
  transpilePackages: ["@ocs/shared"],
  output: "standalone",
  outputFileTracingRoot: root, // include workspace packages in the standalone bundle
  poweredByHeader: false,
  async rewrites() {
    const backend = process.env.BACKEND_URL ?? "http://localhost:8080";
    const catalog = process.env.CATALOG_URL ?? "http://localhost:3001";
    const learn = process.env.LEARN_URL ?? "http://localhost:3002";
    const studio = process.env.STUDIO_URL ?? "http://localhost:3003";
    const admin = process.env.ADMIN_URL ?? "http://localhost:3004";
    // Multi-zone routing: each path prefix is owned by an independently built and deployed Next.js app.
    return [
      { source: "/backend/:path*", destination: `${backend}/api/:path*` },
      { source: "/courses", destination: `${catalog}/courses` },
      { source: "/courses/:path*", destination: `${catalog}/courses/:path*` },
      { source: "/learn", destination: `${learn}/learn` },
      { source: "/learn/:path*", destination: `${learn}/learn/:path*` },
      { source: "/studio", destination: `${studio}/studio` },
      { source: "/studio/:path*", destination: `${studio}/studio/:path*` },
      { source: "/admin", destination: `${admin}/admin` },
      { source: "/admin/:path*", destination: `${admin}/admin/:path*` },
    ];
  },
};

export default nextConfig;
