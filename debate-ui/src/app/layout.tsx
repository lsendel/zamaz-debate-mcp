import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { OrganizationProvider } from '@/hooks/use-organization';
import { NotificationProvider } from '@/contexts/notification-context';
import { WebSocketProvider } from '@/contexts/websocket-context';
import { NotificationBell } from '@/components/notifications/notification-bell';
import { Toaster } from '@/components/ui/toaster';
import { ErrorBoundary } from '@/components/error-boundary';

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
        <ErrorBoundary>
          <OrganizationProvider>
            <NotificationProvider>
              <WebSocketProvider>
                <div className="min-h-screen bg-background">
                  <div className="fixed top-4 right-4 z-50">
                    <NotificationBell />
                  </div>
                  {children}
                  <Toaster />
                </div>
              </WebSocketProvider>
            </NotificationProvider>
          </OrganizationProvider>
        </ErrorBoundary>
      </body>
    </html>
  );
}