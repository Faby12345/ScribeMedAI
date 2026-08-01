import type { LabelHTMLAttributes } from "react";

import { cn } from "@/lib/class-names";

type LabelProps = LabelHTMLAttributes<HTMLLabelElement> & {
  required?: boolean;
};

export function Label({ className, children, required, ...props }: LabelProps) {
  return (
    <label
      className={cn("text-sm font-medium leading-5 text-foreground", className)}
      {...props}
    >
      {children}
      {required ? (
        <span className="ml-1 text-destructive" aria-hidden="true">
          *
        </span>
      ) : null}
    </label>
  );
}
