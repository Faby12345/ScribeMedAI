import type { InputHTMLAttributes, ReactNode } from "react";

import { cn } from "@/lib/class-names";

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  hasError?: boolean;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
};

export function Input({
  className,
  hasError = false,
  leadingIcon,
  trailingIcon,
  disabled,
  ...props
}: InputProps) {
  return (
    <div className="relative">
      {leadingIcon ? (
        <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-muted-foreground">
          {leadingIcon}
        </span>
      ) : null}
      <input
        className={cn(
          "h-11 w-full rounded-md border bg-surface px-3 text-base text-foreground shadow-input transition-colors",
          "placeholder:text-muted-foreground/75",
          "focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 focus:ring-offset-background",
          "disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-muted-foreground",
          leadingIcon ? "pl-10" : undefined,
          trailingIcon ? "pr-12" : undefined,
          hasError
            ? "border-destructive focus:border-destructive focus:ring-destructive"
            : "border-input hover:border-border",
          className,
        )}
        disabled={disabled}
        aria-invalid={hasError || undefined}
        {...props}
      />
      {trailingIcon ? (
        <span className="absolute inset-y-0 right-2 flex items-center">
          {trailingIcon}
        </span>
      ) : null}
    </div>
  );
}
