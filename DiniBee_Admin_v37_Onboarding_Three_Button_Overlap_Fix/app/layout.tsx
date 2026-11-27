import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "DiniBee Admin Panel",
  description: "DiniBee's secure control center for restaurant verification, marketplace monitoring, finance, support, marketing, analytics, and platform administration.",
  icons: { icon: "/dinibee-favicon.png", shortcut: "/dinibee-favicon.png" },
  other: { "codex-preview": "development" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en" suppressHydrationWarning><body>{children}</body></html>;
}
