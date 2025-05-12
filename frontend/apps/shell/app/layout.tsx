import type { Metadata } from "next";
import "@ocs/shared/globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL ?? "http://localhost:3000"),
  title: { default: "OpenCourse — corporate learning portal", template: "%s · OpenCourse" },
  description: "Onboarding and training courses for your team.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
