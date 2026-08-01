import type { ButtonHTMLAttributes, ReactNode } from "react";

import { Spinner } from "@/components/ui/spinner";
import { cn } from "@/lib/class-names";

type ButtonVariant =
  | "primary"
  | "secondary"
  | "outline"
  | "ghost"
  | "success"
  | "warning"
  | "destructive";
type ButtonSize = "sm" | "md" | "lg" | "icon";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  loadingText?: string;
  children: ReactNode;
};

const baseClasses =
  "inline-flex shrink-0 items-center justify-center gap-2 rounded-[var(--radius-control)] border font-medium tracking-normal transition-colors " +
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background " +
  "disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-55";

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    "border-primary bg-primary text-primary-foreground shadow-surface hover:bg-primary-hover active:bg-primary-hover",
  secondary:
    "border-secondary bg-secondary text-foreground hover:bg-secondary-hover active:bg-secondary-hover",
  outline:
    "border-border bg-surface text-foreground shadow-surface hover:bg-surface-muted active:bg-secondary",
  ghost:
    "border-transparent bg-transparent text-muted-foreground hover:bg-secondary hover:text-foreground active:bg-secondary-hover",
  success:
    "border-success bg-success text-primary-foreground shadow-surface hover:bg-success-hover active:bg-success-hover",
  warning:
    "border-warning bg-warning text-primary-foreground shadow-surface hover:bg-warning-hover active:bg-warning-hover",
  destructive:
    "border-destructive bg-destructive text-destructive-foreground shadow-surface hover:bg-destructive-hover active:bg-destructive-hover",
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: "h-9 px-3 text-sm",
  md: "h-[var(--control-height)] px-4 text-sm",
  lg: "h-12 px-5 text-base",
  icon: "size-10 p-0",
};

export function Button({
  variant = "primary",
  size = "md",
  isLoading = false,
  loadingText,
  disabled,
  className,
  children,
  ...props
}: ButtonProps) {
  const isDisabled = disabled || isLoading;

  return (
    <button
      className={cn(
        baseClasses,
        variantClasses[variant],
        sizeClasses[size],
        className,
      )}
      disabled={isDisabled}
      aria-busy={isLoading || undefined}
      {...props}
    >
      {isLoading ? <Spinner /> : null}
      {isLoading && loadingText ? loadingText : children}
    </button>
  );
}
