import type { HTMLAttributes } from "react";

import { cn } from "@/lib/class-names";

export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("rounded-[var(--radius-control)] bg-secondary", className)}
      {...props}
    />
  );
}
