import type { LabelHTMLAttributes } from "react";

import { cn } from "@/lib/class-names";

type LabelProps = LabelHTMLAttributes<HTMLLabelElement>;

export function Label({ className, ...props }: LabelProps) {
  return (
    <label
      className={cn("text-sm font-medium leading-5 text-foreground", className)}
      {...props}
    />
  );
}
