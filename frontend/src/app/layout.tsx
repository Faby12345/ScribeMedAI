import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { AppShell } from "@/components/app-shell";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const sidebarPreferenceScript = `
try {
  var value = localStorage.getItem("scribemed.sidebar.expanded");
  document.documentElement.dataset.sidebarExpanded = value === "false" ? "false" : "true";
} catch (error) {
  document.documentElement.dataset.sidebarExpanded = "true";
}
`;

export const metadata: Metadata = {
  title: "ScribeMedAI",
  description: "Spațiu securizat pentru documentație medicală clinică.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ro"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col">
        <script dangerouslySetInnerHTML={{ __html: sidebarPreferenceScript }} />
        <AppShell>{children}</AppShell>
      </body>
    </html>
  );
}
