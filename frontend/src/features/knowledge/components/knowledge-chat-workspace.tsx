"use client";

import {
  type FormEvent,
  type KeyboardEvent,
  useEffect,
  useRef,
  useState,
} from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  getAllKnowledgeDocuments,
  GetKnowledgeDocumentsApiError,
} from "@/features/knowledge/api/get-knowledge-documents";
import {
  queryKnowledge,
  QueryKnowledgeApiError,
} from "@/features/knowledge/api/query-knowledge";
import type {
  KnowledgeCitation,
  KnowledgeDocument,
} from "@/features/knowledge/types";
import { cn } from "@/lib/class-names";

const MAX_QUERY_LENGTH = 2000;
const MAX_SELECTED_DOCUMENTS = 20;

type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  citations?: KnowledgeCitation[];
};

type FailedQuestion = {
  question: string;
  documentIds: string[];
};

const suggestedQuestions = [
  "Care sunt etapele evaluării inițiale descrise în ghiduri?",
  "Ce criterii de monitorizare sunt menționate în documente?",
  "Rezumați informațiile disponibile despre evaluarea riscului.",
];

export function KnowledgeChatWorkspace() {
  const [documents, setDocuments] = useState<KnowledgeDocument[]>([]);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(true);
  const [documentsError, setDocumentsError] = useState<string | null>(null);
  const [documentsRetryKey, setDocumentsRetryKey] = useState(0);
  const [scope, setScope] = useState<"all" | "selected">("all");
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<string[]>([]);
  const [selectionError, setSelectionError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [query, setQuery] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [queryError, setQueryError] = useState<string | null>(null);
  const [failedQuestion, setFailedQuestion] = useState<FailedQuestion | null>(null);
  const conversationEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    let isActive = true;

    async function loadDocuments() {
      setIsLoadingDocuments(true);
      setDocumentsError(null);

      try {
        const loadedDocuments = await getAllKnowledgeDocuments();

        if (isActive) {
          setDocuments(loadedDocuments);
        }
      } catch (error) {
        if (!isActive) {
          return;
        }

        setDocumentsError(
          error instanceof GetKnowledgeDocumentsApiError
            ? error.message
            : "Documentele nu au putut fi încărcate.",
        );
      } finally {
        if (isActive) {
          setIsLoadingDocuments(false);
        }
      }
    }

    loadDocuments();
    return () => {
      isActive = false;
    };
  }, [documentsRetryKey]);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;

    conversationEndRef.current?.scrollIntoView({
      behavior: prefersReducedMotion ? "auto" : "smooth",
      block: "nearest",
    });
  }, [messages, isSubmitting, queryError]);

  const sourceSummary =
    scope === "all"
      ? `${documents.length} ${documents.length === 1 ? "document disponibil" : "documente disponibile"}`
      : `${selectedDocumentIds.length} ${selectedDocumentIds.length === 1 ? "document selectat" : "documente selectate"}`;

  function selectAllDocuments() {
    setScope("all");
    setSelectedDocumentIds([]);
    setSelectionError(null);
  }

  function toggleDocument(documentId: string) {
    setScope("selected");
    setSelectionError(null);
    setSelectedDocumentIds((current) => {
      if (current.includes(documentId)) {
        const nextSelection = current.filter((id) => id !== documentId);
        if (nextSelection.length === 0) {
          setSelectionError(
            "Selectează cel puțin un document sau folosește toate documentele.",
          );
        }
        return nextSelection;
      }

      if (current.length >= MAX_SELECTED_DOCUMENTS) {
        setSelectionError(
          `Poți selecta maximum ${MAX_SELECTED_DOCUMENTS} de documente.`,
        );
        return current;
      }

      return [...current, documentId];
    });
  }

  async function submitQuestion(question: string, documentIds: string[]) {
    setIsSubmitting(true);
    setQueryError(null);
    setFailedQuestion(null);

    try {
      const response = await queryKnowledge(question, documentIds);
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: "assistant",
          content: response.answer,
          citations: response.citations,
        },
      ]);
    } catch (error) {
      setQueryError(
        error instanceof QueryKnowledgeApiError
          ? error.message
          : "Răspunsul nu a putut fi generat. Încearcă din nou.",
      );
      setFailedQuestion({ question, documentIds });
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const normalizedQuery = query.trim();
    if (!normalizedQuery || isSubmitting) {
      return;
    }

    if (scope === "selected" && selectedDocumentIds.length === 0) {
      setSelectionError(
        "Selectează cel puțin un document sau folosește toate documentele.",
      );
      return;
    }

    const documentIds = scope === "all" ? [] : [...selectedDocumentIds];
    setMessages((current) => [
      ...current,
      {
        id: crypto.randomUUID(),
        role: "user",
        content: normalizedQuery,
      },
    ]);
    setQuery("");
    void submitQuestion(normalizedQuery, documentIds);
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (
      event.key === "Enter" &&
      !event.shiftKey &&
      !event.nativeEvent.isComposing
    ) {
      event.preventDefault();
      event.currentTarget.form?.requestSubmit();
    }
  }

  function useSuggestedQuestion(question: string) {
    setQuery(question);
    textareaRef.current?.focus();
  }

  return (
    <section
      className="flex h-full min-h-0 flex-col overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface"
      aria-label="Spațiu de conversație cu baza de cunoștințe"
    >
      <div className="border-b border-border bg-surface-muted px-4 py-3 lg:hidden">
        <details className="group">
          <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 rounded-[var(--radius-control)] px-1 text-sm font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            <span className="flex min-w-0 items-center gap-2.5">
              <SourcesIcon />
              <span className="truncate">Surse · {sourceSummary}</span>
            </span>
            <ChevronIcon className="transition-transform duration-200 group-open:rotate-180" />
          </summary>
          <div className="max-h-[min(42dvh,22rem)] overflow-y-auto overscroll-contain pt-3">
            <DocumentSelector
              documents={documents}
              isLoading={isLoadingDocuments}
              error={documentsError}
              scope={scope}
              selectedIds={selectedDocumentIds}
              selectionError={selectionError}
              onSelectAll={selectAllDocuments}
              onToggle={toggleDocument}
              onRetry={() => setDocumentsRetryKey((current) => current + 1)}
            />
          </div>
        </details>
      </div>

      <div className="grid min-h-0 flex-1 lg:grid-cols-[18rem_minmax(0,1fr)]">
        <aside className="hidden min-h-0 overflow-hidden border-r border-border bg-surface-muted/70 lg:flex lg:flex-col">
          <div className="border-b border-border px-5 py-5">
            <div className="flex items-center gap-2.5">
              <SourcesIcon />
              <h2 className="text-sm font-semibold text-foreground">Surse consultate</h2>
            </div>
            <p className="caption-text mt-2">
              Alege documentele folosite pentru următoarele întrebări.
            </p>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-3">
            <DocumentSelector
              documents={documents}
              isLoading={isLoadingDocuments}
              error={documentsError}
              scope={scope}
              selectedIds={selectedDocumentIds}
              selectionError={selectionError}
              onSelectAll={selectAllDocuments}
              onToggle={toggleDocument}
              onRetry={() => setDocumentsRetryKey((current) => current + 1)}
            />
          </div>
        </aside>

        <div className="flex min-h-0 min-w-0 flex-col">
          <div className="flex items-center justify-between gap-4 border-b border-border px-4 py-3 sm:px-6">
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-foreground">
                Conversație nouă
              </p>
              <p className="caption-text mt-0.5 truncate">{sourceSummary}</p>
            </div>
            <Badge variant="neutral">Nu se salvează</Badge>
          </div>

          <div
            className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-4 py-6 sm:px-6 lg:px-8"
            aria-live="polite"
          >
            <div className="mx-auto flex w-full max-w-3xl flex-col gap-6">
              {messages.length === 0 && !isSubmitting ? (
                <ConversationEmptyState onSelectQuestion={useSuggestedQuestion} />
              ) : null}

              {messages.map((message) => (
                <ChatMessageItem key={message.id} message={message} />
              ))}

              {isSubmitting ? <ThinkingMessage /> : null}

              {queryError ? (
                <Alert
                  variant="error"
                  title="Răspunsul nu a putut fi generat"
                  className="knowledge-answer-enter"
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <span>{queryError}</span>
                    {failedQuestion ? (
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          void submitQuestion(
                            failedQuestion.question,
                            failedQuestion.documentIds,
                          )
                        }
                      >
                        Reîncearcă
                      </Button>
                    ) : null}
                  </div>
                </Alert>
              ) : null}
              <div ref={conversationEndRef} />
            </div>
          </div>

          <div className="border-t border-border bg-surface px-4 py-4 sm:px-6">
            <form
              onSubmit={handleSubmit}
              className="mx-auto w-full max-w-3xl"
              aria-label="Trimite o întrebare"
            >
              <div className="rounded-[var(--radius-surface)] border border-input bg-surface shadow-[0_10px_30px_rgb(16_24_40_/_0.08)] transition-colors focus-within:border-primary focus-within:ring-2 focus-within:ring-ring/20">
                <label htmlFor="knowledge-question" className="sr-only">
                  Întrebare pentru baza de cunoștințe
                </label>
                <Textarea
                  ref={textareaRef}
                  id="knowledge-question"
                  value={query}
                  maxLength={MAX_QUERY_LENGTH}
                  rows={3}
                  disabled={isSubmitting || documents.length === 0}
                  className="min-h-[5.5rem] border-0 shadow-none hover:border-0 focus-visible:border-0 focus-visible:ring-0"
                  placeholder="Întreabă pe baza documentelor selectate…"
                  onChange={(event) => setQuery(event.target.value)}
                  onKeyDown={handleComposerKeyDown}
                />
                <div className="flex items-center justify-between gap-3 border-t border-border/80 px-3 py-2.5">
                  <p className="caption-text min-w-0 truncate">
                    Enter pentru trimitere · Shift + Enter pentru rând nou
                  </p>
                  <div className="flex shrink-0 items-center gap-2">
                    {query.length >= 1600 ? (
                      <span className="caption-text tabular-nums">
                        {query.length}/{MAX_QUERY_LENGTH}
                      </span>
                    ) : null}
                    <Button
                      type="submit"
                      size="icon"
                      disabled={
                        !query.trim() ||
                        isSubmitting ||
                        documents.length === 0 ||
                        (scope === "selected" && selectedDocumentIds.length === 0)
                      }
                      aria-label="Trimite întrebarea"
                    >
                      <SendIcon />
                    </Button>
                  </div>
                </div>
              </div>
              <p className="caption-text mt-2 text-center">
                Răspunsurile sunt informative și trebuie verificate în sursele citate.
              </p>
            </form>
          </div>
        </div>
      </div>
    </section>
  );
}

type DocumentSelectorProps = {
  documents: KnowledgeDocument[];
  isLoading: boolean;
  error: string | null;
  scope: "all" | "selected";
  selectedIds: string[];
  selectionError: string | null;
  onSelectAll: () => void;
  onToggle: (documentId: string) => void;
  onRetry: () => void;
};

function DocumentSelector({
  documents,
  isLoading,
  error,
  scope,
  selectedIds,
  selectionError,
  onSelectAll,
  onToggle,
  onRetry,
}: DocumentSelectorProps) {
  if (isLoading) {
    return (
      <div className="space-y-2" aria-label="Se încarcă documentele" aria-busy="true">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-14 w-full" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="error" title="Sursele nu sunt disponibile">
        <p>{error}</p>
        <Button type="button" variant="outline" size="sm" className="mt-3" onClick={onRetry}>
          Reîncearcă
        </Button>
      </Alert>
    );
  }

  if (documents.length === 0) {
    return (
      <p className="rounded-[var(--radius-control)] border border-border bg-surface px-3 py-4 text-sm leading-6 text-muted-foreground">
        Biblioteca nu conține încă documente procesate.
      </p>
    );
  }

  return (
    <div>
      <button
        type="button"
        onClick={onSelectAll}
        className={cn(
          "flex min-h-12 w-full items-center gap-3 rounded-[var(--radius-control)] px-3 py-2.5 text-left text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
          scope === "all"
            ? "bg-primary-soft font-semibold text-primary"
            : "bg-surface text-foreground hover:bg-secondary",
        )}
        aria-pressed={scope === "all"}
      >
        <span
          className={cn(
            "flex size-5 shrink-0 items-center justify-center rounded-full border",
            scope === "all" ? "border-primary bg-primary text-primary-foreground" : "border-input",
          )}
          aria-hidden="true"
        >
          {scope === "all" ? <CheckIcon /> : null}
        </span>
        <span className="min-w-0">
          <span className="block">Toate documentele</span>
          <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
            {documents.length} disponibile
          </span>
        </span>
      </button>

      <div className="my-3 flex items-center gap-3 px-1" aria-hidden="true">
        <span className="h-px flex-1 bg-border" />
        <span className="text-[0.6875rem] font-semibold uppercase text-muted-foreground">
          Selecție
        </span>
        <span className="h-px flex-1 bg-border" />
      </div>

      <ul className="space-y-1">
        {documents.map((document) => {
          const isSelected = selectedIds.includes(document.id);
          return (
            <li key={document.id}>
              <label
                className={cn(
                  "flex min-h-12 cursor-pointer items-start gap-3 rounded-[var(--radius-control)] px-3 py-2.5 text-sm transition-colors focus-within:ring-2 focus-within:ring-ring",
                  isSelected ? "bg-secondary text-foreground" : "text-muted-foreground hover:bg-surface",
                )}
              >
                <input
                  type="checkbox"
                  checked={isSelected}
                  onChange={() => onToggle(document.id)}
                  className="mt-0.5 size-4 shrink-0 accent-primary"
                />
                <span className="min-w-0">
                  <span className="block break-words font-medium leading-5 text-foreground">
                    {document.fileName}
                  </span>
                  <span className="caption-text mt-0.5 block">
                    PDF · {formatDate(document.createdAt)}
                  </span>
                </span>
              </label>
            </li>
          );
        })}
      </ul>

      {scope === "selected" ? (
        <p className="caption-text mt-3 px-1" role="status">
          {selectedIds.length}/{MAX_SELECTED_DOCUMENTS} selectate
        </p>
      ) : null}
      {selectionError ? (
        <p className="mt-2 px-1 text-xs leading-5 text-destructive" role="alert">
          {selectionError}
        </p>
      ) : null}
    </div>
  );
}

function ConversationEmptyState({
  onSelectQuestion,
}: {
  onSelectQuestion: (question: string) => void;
}) {
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center py-8 text-center sm:py-14">
      <span className="flex size-14 items-center justify-center rounded-[var(--radius-surface)] bg-primary-soft text-primary">
        <KnowledgeMarkIcon />
      </span>
      <h2 className="mt-5 text-xl font-semibold text-foreground">
        Ce vrei să afli din documente?
      </h2>
      <p className="secondary-text mt-2 max-w-lg">
        Răspunsul va fi formulat numai din fragmentele relevante ale surselor disponibile.
      </p>
      <div className="mt-6 grid w-full gap-2 sm:grid-cols-3">
        {suggestedQuestions.map((question) => (
          <button
            key={question}
            type="button"
            onClick={() => onSelectQuestion(question)}
            className="min-h-20 rounded-[var(--radius-control)] border border-border bg-surface px-3 py-3 text-left text-sm leading-5 text-foreground transition-colors hover:border-primary/45 hover:bg-primary-soft/45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            {question}
          </button>
        ))}
      </div>
    </div>
  );
}

function ChatMessageItem({ message }: { message: ChatMessage }) {
  if (message.role === "user") {
    return (
      <article className="ml-auto max-w-[min(85%,42rem)] rounded-[var(--radius-surface)] bg-primary px-4 py-3 text-sm leading-6 text-primary-foreground">
        <p className="whitespace-pre-wrap">{message.content}</p>
      </article>
    );
  }

  return (
    <article className="knowledge-answer-enter max-w-[46rem]" aria-label="Răspunsul asistentului">
      <div className="flex items-start gap-3">
        <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-[var(--radius-control)] bg-primary-soft text-primary">
          <KnowledgeMarkIcon className="size-4" />
        </span>
        <div className="min-w-0 flex-1">
          <p className="whitespace-pre-wrap text-[0.9375rem] leading-7 text-foreground">
            {message.content}
          </p>
          {message.citations?.length ? (
            <CitationList citations={message.citations} />
          ) : null}
        </div>
      </div>
    </article>
  );
}

function CitationList({ citations }: { citations: KnowledgeCitation[] }) {
  return (
    <details className="group mt-5 rounded-[var(--radius-control)] border border-border bg-surface-muted">
      <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 px-3.5 py-2 text-sm font-medium text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
        <span>Fragmente consultate ({citations.length})</span>
        <ChevronIcon className="transition-transform duration-200 group-open:rotate-180" />
      </summary>
      <ol className="divide-y divide-border border-t border-border">
        {citations.map((citation, index) => (
          <li key={`${citation.documentId}-${index}`} className="px-3.5 py-3">
            <div className="flex items-start gap-2.5">
              <span className="mt-0.5 flex min-w-7 items-center justify-center rounded bg-primary-soft px-1.5 py-1 text-xs font-semibold text-primary">
                [{index + 1}]
              </span>
              <div className="min-w-0">
                <p className="text-sm font-semibold text-foreground">
                  {citation.documentTitle}
                </p>
                <p className="caption-text mt-1">
                  {formatPages(citation.pageFrom, citation.pageTo)}
                  {citation.sectionTitle ? ` · ${citation.sectionTitle}` : ""}
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  {citation.excerpt}
                </p>
              </div>
            </div>
          </li>
        ))}
      </ol>
    </details>
  );
}

function ThinkingMessage() {
  return (
    <div className="knowledge-thinking flex max-w-lg items-start gap-3" role="status">
      <span className="flex size-8 shrink-0 items-center justify-center rounded-[var(--radius-control)] bg-primary-soft text-primary">
        <KnowledgeMarkIcon className="size-4" />
      </span>
      <div className="min-w-0 flex-1 rounded-[var(--radius-surface)] border border-border bg-surface-muted px-4 py-3.5">
        <p className="text-sm font-medium text-foreground">Consult sursele selectate…</p>
        <div className="mt-3 space-y-1.5" aria-hidden="true">
          {[1, 2, 3].map((sourceNumber) => (
            <span key={sourceNumber} className="knowledge-reading-source flex items-center gap-2 rounded px-2 py-1.5">
              <span className="text-[0.625rem] font-semibold text-primary">[{sourceNumber}]</span>
              <span className="knowledge-reading-line h-1.5 rounded-full bg-border" />
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "dată indisponibilă";
  }
  return new Intl.DateTimeFormat("ro-RO", { dateStyle: "medium" }).format(date);
}

function formatPages(pageFrom: number | null, pageTo: number | null) {
  if (pageFrom === null) {
    return "Pagină indisponibilă";
  }
  if (pageTo === null || pageFrom === pageTo) {
    return `Pagina ${pageFrom}`;
  }
  return `Paginile ${pageFrom}–${pageTo}`;
}

function SourcesIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-5 text-primary" aria-hidden="true">
      <path d="M6.5 4.5h9a2 2 0 0 1 2 2v13h-9a2 2 0 0 1-2-2v-13Z" />
      <path d="M6.5 7.5h-1a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h10M10 9h4M10 12h4" strokeLinecap="round" />
    </svg>
  );
}

function KnowledgeMarkIcon({ className = "size-6" }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className={className} aria-hidden="true">
      <path d="M5 5.5A2.5 2.5 0 0 1 7.5 3H11v15H7.5A2.5 2.5 0 0 0 5 20.5v-15Z" />
      <path d="M19 5.5A2.5 2.5 0 0 0 16.5 3H13v15h3.5a2.5 2.5 0 0 1 2.5 2.5v-15Z" />
      <path d="M8 7h1M15 7h1M8 10h1M15 10h1" strokeLinecap="round" />
    </svg>
  );
}

function ChevronIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className={cn("size-4 text-muted-foreground", className)} aria-hidden="true">
      <path d="m6 8 4 4 4-4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" className="size-3" aria-hidden="true">
      <path d="m3.5 8 3 3 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function SendIcon() {
  return (
    <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.8" className="size-5" aria-hidden="true">
      <path d="m4 10 11-6-3.5 12-2-4.5L4 10Z" strokeLinecap="round" strokeLinejoin="round" />
      <path d="m9.5 11.5 5.5-7.5" strokeLinecap="round" />
    </svg>
  );
}
