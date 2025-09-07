// Flat ESLint config shared by every zone and the shared package.
import nextPlugin from "@next/eslint-plugin-next";
import jsxA11y from "eslint-plugin-jsx-a11y";
import reactHooks from "eslint-plugin-react-hooks";
import tseslint from "typescript-eslint";

export default tseslint.config(
  { ignores: ["**/.next/**", "**/node_modules/**", "**/next-env.d.ts", "e2e/test-results/**", "e2e/playwright-report/**"] },
  ...tseslint.configs.recommended,
  {
    files: ["**/*.{ts,tsx}"],
    plugins: { "@next/next": nextPlugin, "react-hooks": reactHooks, "jsx-a11y": jsxA11y },
    settings: { next: { rootDir: ["apps/*/"] } },
    rules: {
      ...nextPlugin.configs.recommended.rules,
      ...nextPlugin.configs["core-web-vitals"].rules,
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
      ...jsxA11y.flatConfigs.recommended.rules,
      // Cross-zone navigation must be a full page load, so plain <a> is intentional for those links.
      "@next/next/no-html-link-for-pages": "off",
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
    },
  },
  {
    // Tailwind/PostCSS config files are CommonJS.
    files: ["**/*.js"],
    languageOptions: { sourceType: "commonjs", globals: { module: "writable", require: "readonly" } },
    rules: { "@typescript-eslint/no-require-imports": "off" },
  },
);
