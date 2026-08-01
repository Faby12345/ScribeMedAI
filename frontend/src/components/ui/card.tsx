import type { HTMLAttributes } from "react";

import { cn } from "@/lib/class-names";

type SurfaceVariant = "standard" | "elevated" | "muted" | "interactive";
type CardProps = HTMLAttributes<HTMLDivElement> & {
  variant?: SurfaceVariant;
};

const variantClasses: Record<SurfaceVariant, string> = {
  standard: "border-border bg-surface shadow-surface",
  elevated: "border-border bg-surface-elevated shadow-elevated",
  muted: "border-border bg-surface-muted shadow-none",
  interactive:
    "border-border bg-surface shadow-surface transition-colors hover:border-primary/35 hover:bg-surface-muted",
};

export function Card({
  variant = "standard",
  className,
  ...props
}: CardProps) {
  return (
    <div
      className={cn(
        "rounded-[var(--radius-surface)] border text-foreground",
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("space-y-1.5 p-5 pb-3", className)} {...props} />;
}

export function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("p-5 pt-2", className)} {...props} />;
}
