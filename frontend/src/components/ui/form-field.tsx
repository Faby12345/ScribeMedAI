import type { ReactNode } from "react";

import { Label } from "@/components/ui/label";
import { cn } from "@/lib/class-names";

type FormFieldProps = {
  id: string;
  label: string;
  description?: string;
  error?: string;
  required?: boolean;
  children: ReactNode;
  className?: string;
};

export function FormField({
  id,
  label,
  description,
  error,
  required,
  children,
  className,
}: FormFieldProps) {
  return (
    <div className={cn("space-y-2", className)}>
      <Label htmlFor={id} required={required}>
        {label}
      </Label>
      {children}
      {description ? (
        <FormMessage id={`${id}-description`}>{description}</FormMessage>
      ) : null}
      {error ? (
        <FormMessage id={`${id}-error`} variant="error">
          {error}
        </FormMessage>
      ) : null}
    </div>
  );
}

type FormMessageProps = {
  id?: string;
  variant?: "description" | "error";
  children: ReactNode;
};

export function FormMessage({
  id,
  variant = "description",
  children,
}: FormMessageProps) {
  return (
    <p
      id={id}
      className={cn(
        "text-sm leading-5",
        variant === "error" ? "text-destructive" : "text-muted-foreground",
      )}
    >
      {children}
    </p>
  );
}
