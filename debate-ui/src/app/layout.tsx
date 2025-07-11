import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { OrganizationProvider } from '@/hooks/use-organization';

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Debate UI - MCP Debate System",
  description: "AI-powered debate management system",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <OrganizationProvider>
          <div className="min-h-screen bg-background">
            {children}
          </div>
        </OrganizationProvider>
      </body>
    </html>
  );
}