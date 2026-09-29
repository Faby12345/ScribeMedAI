import type { HTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/class-names";

type AlertVariant = "info" | "error" | "success" | "warning";

type AlertProps = HTMLAttributes<HTMLDivElement> & {
  variant?: AlertVariant;
  title?: string;
  children: ReactNode;
};

const variantClasses: Record<AlertVariant, string> = {
  info: "border-info/25 bg-info-soft",
  error: "border-destructive/30 bg-destructive-soft",
  success: "border-success/30 bg-success-soft",
  warning: "border-warning/35 bg-warning-soft",
};

export function Alert({
  variant = "info",
  title,
  className,
  children,
  ...props
}: AlertProps) {
  return (
    <div
      className={cn(
        "rounded-[var(--radius-control)] border px-4 py-3 text-sm leading-6",
        variantClasses[variant],
        className,
      )}
      role={variant === "error" ? "alert" : "status"}
      {...props}
    >
      {title ? <p className="font-medium text-foreground">{title}</p> : null}
      <div className={cn(title ? "mt-1" : undefined, "text-muted-foreground")}>
        {children}
      </div>
    </div>
  );
}
