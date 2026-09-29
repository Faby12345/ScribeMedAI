import type { ReactNode } from "react";

import { Card } from "@/components/ui/card";

type EmptyStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
};

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <Card variant="muted" className="p-6">
      <div className="max-w-xl">
        <p className="text-sm font-semibold text-foreground">{title}</p>
        <p className="secondary-text mt-2">{description}</p>
        {action ? <div className="mt-4">{action}</div> : null}
      </div>
    </Card>
  );
}
