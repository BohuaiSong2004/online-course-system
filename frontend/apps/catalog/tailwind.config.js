/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [require("../../tailwind.preset.js")],
  content: ["./app/**/*.{ts,tsx}", "../../packages/shared/src/**/*.{ts,tsx}"],
};
