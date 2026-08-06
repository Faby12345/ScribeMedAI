import type { ReactNode } from "react";

import { cn } from "@/lib/class-names";

type PageHeaderProps = {
  eyebrow?: ReactNode;
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  className?: string;
};

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
  className,
}: PageHeaderProps) {
  return (
    <header
      className={cn(
        "mb-7 border-b border-border pb-6",
        className,
      )}
    >
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div className="min-w-0">
          {eyebrow ? <div>{eyebrow}</div> : null}
          <h1 className="page-title mt-3">{title}</h1>
          {description ? (
            <p className="secondary-text mt-2 max-w-2xl">{description}</p>
          ) : null}
        </div>
        {actions ? (
          <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
            {actions}
          </div>
        ) : null}
      </div>
    </header>
  );
}
