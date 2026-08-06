import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/features/auth/api/current-user";
import { DashboardShell } from "@/features/dashboard/components/dashboard-shell";

export default async function DashboardPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return <DashboardShell />;
}
