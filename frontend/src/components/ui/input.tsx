import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";

import { cn } from "@/lib/class-names";

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  hasError?: boolean;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
};

const controlClasses =
  "w-full rounded-[var(--radius-control)] border bg-surface text-base text-foreground shadow-surface transition-colors " +
  "placeholder:text-muted-foreground/70 hover:border-border " +
  "focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 focus:ring-offset-background " +
  "disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-muted-foreground";

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
          controlClasses,
          "h-[var(--control-height)] px-3",
          leadingIcon ? "pl-10" : undefined,
          trailingIcon ? "pr-16" : undefined,
          hasError
            ? "border-destructive focus:border-destructive focus:ring-destructive"
            : "border-input",
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

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  hasError?: boolean;
};

export function Textarea({ className, hasError = false, ...props }: TextareaProps) {
  return (
    <textarea
      className={cn(
        controlClasses,
        "min-h-28 resize-y px-3 py-2.5",
        hasError
          ? "border-destructive focus:border-destructive focus:ring-destructive"
          : "border-input",
        className,
      )}
      aria-invalid={hasError || undefined}
      {...props}
    />
  );
}

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  hasError?: boolean;
};

export function Select({ className, hasError = false, ...props }: SelectProps) {
  return (
    <select
      className={cn(
        controlClasses,
        "h-[var(--control-height)] px-3",
        hasError
          ? "border-destructive focus:border-destructive focus:ring-destructive"
          : "border-input",
        className,
      )}
      aria-invalid={hasError || undefined}
      {...props}
    />
  );
}

type CheckboxProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  description?: string;
};

export function Checkbox({
  label,
  description,
  className,
  ...props
}: CheckboxProps) {
  return (
    <label className="flex gap-3 text-sm leading-6 text-foreground">
      <input
        type="checkbox"
        className={cn(
          "mt-1 size-4 rounded border-input text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
          className,
        )}
        {...props}
      />
      <span>
        <span className="font-medium">{label}</span>
        {description ? (
          <span className="block text-muted-foreground">{description}</span>
        ) : null}
      </span>
    </label>
  );
}
