import type { HTMLAttributes } from "react";

import { cn } from "@/lib/class-names";

type BadgeVariant =
  | "neutral"
  | "info"
  | "processing"
  | "success"
  | "warning"
  | "destructive";

type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  variant?: BadgeVariant;
};

const variantClasses: Record<BadgeVariant, string> = {
  neutral: "border-border bg-secondary text-muted-foreground",
  info: "border-info/25 bg-info-soft text-info",
  processing: "border-primary/25 bg-primary-soft text-primary",
  success: "border-success/25 bg-success-soft text-success",
  warning: "border-warning/30 bg-warning-soft text-warning",
  destructive: "border-destructive/25 bg-destructive-soft text-destructive",
};

export function Badge({
  variant = "neutral",
  className,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-[calc(var(--radius-control)-0.125rem)] border px-2 py-0.5 text-xs font-medium leading-5",
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  );
}
