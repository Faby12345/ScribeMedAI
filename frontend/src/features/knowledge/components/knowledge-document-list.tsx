"use client";

import { useEffect, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import {
  getKnowledgeDocuments,
  GetKnowledgeDocumentsApiError,
} from "@/features/knowledge/api/get-knowledge-documents";
import type { PaginatedResponse } from "@/features/consultations/types";
import type { KnowledgeDocument } from "@/features/knowledge/types";

const PAGE_SIZE = 8;

export function KnowledgeDocumentList() {
  const [documentsPage, setDocumentsPage] =
    useState<PaginatedResponse<KnowledgeDocument> | null>(null);
  const [page, setPage] = useState(0);
  const [retryKey, setRetryKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function loadDocuments() {
      setIsLoading(true);
      setError(null);

      try {
        const nextPage = await getKnowledgeDocuments({ page, size: PAGE_SIZE });

        if (isActive) {
          setDocumentsPage(nextPage);
        }
      } catch (loadError) {
        if (!isActive) {
          return;
        }

        setError(
          loadError instanceof GetKnowledgeDocumentsApiError
            ? loadError.message
            : "Lista documentelor nu a putut fi încărcată.",
        );
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadDocuments();

    return () => {
      isActive = false;
    };
  }, [page, retryKey]);

  if (isLoading) {
    return <KnowledgeDocumentListSkeleton />;
  }

  if (error) {
    return (
      <section aria-labelledby="knowledge-documents-title">
        <SectionHeading totalDocuments={documentsPage?.totalElements} />
        <Alert variant="error" title="Nu am putut încărca documentele">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span>{error}</span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setRetryKey((current) => current + 1)}
            >
              Reîncearcă
            </Button>
          </div>
        </Alert>
      </section>
    );
  }

  if (!documentsPage || documentsPage.totalElements === 0) {
    return (
      <section aria-labelledby="knowledge-documents-title">
        <SectionHeading totalDocuments={0} />
        <EmptyState
          title="Biblioteca nu conține încă documente disponibile."
          description="Folosește acțiunea «Adaugă document» pentru a încărca primul PDF. Acesta va apărea aici după finalizarea procesării."
        />
      </section>
    );
  }

  const totalPages = Math.max(documentsPage.totalPages, 1);
  const rangeStart = documentsPage.number * documentsPage.size + 1;
  const rangeEnd = rangeStart + documentsPage.content.length - 1;

  return (
    <section aria-labelledby="knowledge-documents-title">
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <SectionHeading totalDocuments={documentsPage.totalElements} />
        <Button
          type="button"
          variant="outline"
          size="sm"
          isLoading={isLoading}
          loadingText="Se actualizează"
          onClick={() => setRetryKey((current) => current + 1)}
        >
          Actualizează lista
        </Button>
      </div>

      <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
        <div className="hidden grid-cols-[minmax(0,1fr)_12rem_8rem] gap-4 border-b border-border bg-surface-muted px-5 py-3 text-xs font-semibold uppercase text-muted-foreground sm:grid">
          <span>Document</span>
          <span>Adăugat</span>
          <span>Status</span>
        </div>
        <ul className="divide-y divide-border">
          {documentsPage.content.map((document) => (
            <KnowledgeDocumentListItem key={document.id} document={document} />
          ))}
        </ul>
      </div>

      <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="secondary-text" role="status" aria-live="polite">
          {rangeStart}–{rangeEnd} din {documentsPage.totalElements} documente · Pagina {documentsPage.number + 1} din {totalPages}
        </p>
        <div className="grid grid-cols-2 gap-2 sm:flex">
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-full sm:w-auto"
            disabled={documentsPage.first}
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
          >
            Anterioară
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-full sm:w-auto"
            disabled={documentsPage.last}
            onClick={() => setPage((current) => current + 1)}
          >
            Următoare
          </Button>
        </div>
      </div>
    </section>
  );
}

function SectionHeading({ totalDocuments }: { totalDocuments?: number }) {
  return (
    <div>
      <div className="flex flex-wrap items-center gap-2">
        <h2 id="knowledge-documents-title" className="section-title">
          Documente disponibile
        </h2>
        {totalDocuments !== undefined ? (
          <Badge variant="neutral">{totalDocuments}</Badge>
        ) : null}
      </div>
      <p className="secondary-text mt-1 max-w-2xl">
        Sursele procesate și pregătite pentru căutarea semantică.
      </p>
    </div>
  );
}

function KnowledgeDocumentListItem({ document }: { document: KnowledgeDocument }) {
  return (
    <li className="grid gap-3 px-5 py-4 sm:grid-cols-[minmax(0,1fr)_12rem_8rem] sm:items-center">
      <div className="flex min-w-0 items-center gap-3">
        <PdfDocumentIcon />
        <div className="min-w-0">
          <p
            className="truncate text-sm font-semibold text-foreground"
            title={document.fileName}
          >
            {document.fileName}
          </p>
          <p className="caption-text mt-1">Document PDF</p>
        </div>
      </div>

      <DocumentField label="Adăugat" value={formatDateTime(document.createdAt)} />

      <div>
        <span className="caption-text mb-1 block sm:hidden">Status</span>
        <Badge variant="success">Disponibil</Badge>
      </div>
    </li>
  );
}

function DocumentField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="caption-text sm:hidden">{label}</p>
      <p className="mt-1 text-sm text-foreground sm:mt-0">{value}</p>
    </div>
  );
}

function PdfDocumentIcon() {
  return (
    <span
      className="relative flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] bg-primary-soft text-primary"
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="size-6"
      >
        <path d="M7 3.75h6.5L18 8.25v12H7a2 2 0 0 1-2-2V5.75a2 2 0 0 1 2-2Z" />
        <path d="M13.5 3.75v4.5H18" />
        <path d="M8.5 12.25h6M8.5 15.5h4.5" />
      </svg>
      <span className="absolute -bottom-1 -right-1 rounded bg-primary px-1 py-0.5 text-[0.5625rem] font-bold leading-none text-primary-foreground">
        PDF
      </span>
    </span>
  );
}

function formatDateTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Dată indisponibilă";
  }

  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function KnowledgeDocumentListSkeleton() {
  return (
    <section aria-labelledby="knowledge-documents-title" aria-busy="true">
      <SectionHeading />
      <div className="mt-4 overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
        <div className="divide-y divide-border">
          {Array.from({ length: 4 }, (_, index) => (
            <div
              key={index}
              className="grid gap-3 px-5 py-4 sm:grid-cols-[minmax(0,1fr)_12rem_8rem] sm:items-center"
            >
              <div className="flex items-center gap-3">
                <Skeleton className="size-11 shrink-0" />
                <div className="w-full max-w-sm">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="mt-2 h-3 w-24" />
                </div>
              </div>
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-6 w-20" />
            </div>
          ))}
        </div>
      </div>
      <p className="secondary-text mt-3">Se încarcă documentele...</p>
    </section>
  );
}
