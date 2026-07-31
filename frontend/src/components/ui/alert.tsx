import type { HTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/class-names";

type AlertVariant = "info" | "error" | "success" | "warning";

type AlertProps = HTMLAttributes<HTMLDivElement> & {
  variant?: AlertVariant;
  title?: string;
  children: ReactNode;
};

const variantClasses: Record<AlertVariant, string> = {
  info: "border-primary/25 bg-primary-soft text-foreground",
  error: "border-destructive/30 bg-destructive-soft text-foreground",
  success: "border-success/30 bg-success-soft text-foreground",
  warning: "border-warning/35 bg-warning-soft text-foreground",
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
        "rounded-md border px-4 py-3 text-sm leading-6",
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
