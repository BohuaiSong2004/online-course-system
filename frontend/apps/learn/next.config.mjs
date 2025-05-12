import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../..");

/** @type {import('next').NextConfig} */
const nextConfig = {
  basePath: "/learn",
  transpilePackages: ["@ocs/shared"],
  output: "standalone",
  outputFileTracingRoot: root, // include workspace packages in the standalone bundle
  poweredByHeader: false,
};

export default nextConfig;
