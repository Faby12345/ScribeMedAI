import { forwardRef, type TextareaHTMLAttributes } from "react";

import { cn } from "@/lib/class-names";

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  hasError?: boolean;
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  function Textarea({ className, hasError = false, ...props }, ref) {
    return (
      <textarea
        ref={ref}
        className={cn(
          "min-h-24 w-full resize-none rounded-[var(--radius-control)] border bg-surface px-3.5 py-3 text-sm leading-6 text-foreground shadow-surface outline-none transition-colors placeholder:text-muted-foreground/80",
          "hover:border-primary/45 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-ring/25",
          "disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-muted-foreground disabled:opacity-70",
          hasError
            ? "border-destructive focus-visible:border-destructive focus-visible:ring-destructive/20"
            : "border-input",
          className,
        )}
        aria-invalid={hasError || undefined}
        {...props}
      />
    );
  },
);
