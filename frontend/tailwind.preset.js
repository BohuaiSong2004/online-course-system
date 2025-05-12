/** Shared design tokens so every zone looks like one product. */
module.exports = {
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eef6ff", 100: "#d9eaff", 200: "#bcdaff", 300: "#8ec2ff", 400: "#599fff",
          500: "#3479fc", 600: "#1d5af1", 700: "#1646de", 800: "#183bb4", 900: "#1a378e",
        },
        ink: { DEFAULT: "#0f172a", soft: "#334155", muted: "#64748b" },
      },
      fontFamily: {
        sans: ["var(--font-sans)", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: { card: "0 1px 2px rgb(15 23 42 / 0.06), 0 4px 16px rgb(15 23 42 / 0.06)" },
    },
  },
  plugins: [require("@tailwindcss/typography")],
};
