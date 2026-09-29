"use client";

import {
  type ChangeEvent,
  type DragEvent,
  type FormEvent,
  useRef,
  useState,
} from "react";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import {
  uploadKnowledgeDocument,
  UploadDocumentApiError,
} from "@/features/knowledge/api/upload-pdf";
import { cn } from "@/lib/class-names";

const MAX_PDF_SIZE_BYTES = 25 * 1024 * 1024;

type FormErrors = Partial<
  Record<"file" | "title" | "sourceInstitution" | "sourceUrl", string>
>;

type KnowledgeDocumentUploadProps = {
  onClose?: () => void;
  onUploaded?: () => void;
};

export function KnowledgeDocumentUpload({
  onClose,
  onUploaded,
}: KnowledgeDocumentUploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [sourceInstitution, setSourceInstitution] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [publishedAt, setPublishedAt] = useState("");
  const [version, setVersion] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [isDragging, setIsDragging] = useState(false);
  const [isPrepared, setIsPrepared] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  function markFormChanged() {
    setIsPrepared(false);
    setSubmitError(null);
  }

  function selectFile(nextFile: File | undefined) {
    markFormChanged();

    if (!nextFile) {
      return;
    }

    const fileError = validatePdf(nextFile);
    if (fileError) {
      setFile(null);
      setErrors((current) => ({ ...current, file: fileError }));
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      return;
    }

    setFile(nextFile);
    setErrors((current) => ({ ...current, file: undefined }));

    if (!title.trim()) {
      setTitle(nextFile.name.replace(/\.pdf$/i, ""));
    }
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    selectFile(event.target.files?.[0]);
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setIsDragging(false);
    selectFile(event.dataTransfer.files?.[0]);
  }

  function removeFile() {
    setFile(null);
    markFormChanged();
    setErrors((current) => ({ ...current, file: undefined }));
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  function resetForm() {
    removeFile();
    setTitle("");
    setSourceInstitution("");
    setSourceUrl("");
    setPublishedAt("");
    setVersion("");
    setErrors({});
    setSubmitError(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    const nextErrors: FormErrors = {};

    if (!file) {
      nextErrors.file = "Selectează documentul PDF pe care vrei să-l adaugi.";
    }
    if (!title.trim()) {
      nextErrors.title = "Introdu titlul documentului.";
    }
    if (!sourceInstitution.trim()) {
      nextErrors.sourceInstitution = "Introdu instituția sau organizația sursă.";
    }
    if (sourceUrl && !isValidUrl(sourceUrl)) {
      nextErrors.sourceUrl =
        "Introdu o adresă web completă, de exemplu https://exemplu.ro.";
    }

    setErrors(nextErrors);
    setIsPrepared(false);
    setSubmitError(null);

    const hasErrors = Object.keys(nextErrors).length > 0;

    if (hasErrors || !file) {
      return;
    }

    setIsSubmitting(true);

    try {
      await uploadKnowledgeDocument(file, {
        title: title.trim(),
        sourceInstitution: sourceInstitution.trim(),
        sourceUrl: sourceUrl.trim() || undefined,
        publishedAt: publishedAt || undefined,
        version: version.trim() || undefined,
      });

      setIsPrepared(true);
      onUploaded?.();
    } catch (error) {
      setSubmitError(
        error instanceof UploadDocumentApiError
          ? error.message
          : "Documentul nu a putut fi încărcat. Încearcă din nou.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_18rem] lg:items-start">
      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0 border-b border-border pb-5">
          <div>
            <h2 className="section-title">Document nou</h2>
            <p className="secondary-text mt-1 max-w-2xl">
              Selectează fișierul și descrie sursa astfel încât să poată fi
              identificată corect ulterior.
            </p>
          </div>
          {onClose ? (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={isSubmitting}
              onClick={onClose}
            >
              Închide
            </Button>
          ) : null}
        </CardHeader>

        <CardContent className="pt-5">
          <form noValidate onSubmit={handleSubmit} className="space-y-6">
            <FormField
              id="knowledge-file"
              label="Fișier PDF"
              required
              error={errors.file}
              description="Este acceptat un singur PDF, de maximum 25 MB."
            >
              <div
                className={cn(
                  "rounded-[var(--radius-surface)] border border-dashed p-5 transition-colors focus-within:outline-none focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background",
                  isDragging
                    ? "border-primary bg-primary-soft"
                    : errors.file
                      ? "border-destructive bg-destructive-soft"
                      : "border-input bg-surface-muted hover:border-primary/50",
                )}
                onDragEnter={(event) => {
                  event.preventDefault();
                  setIsDragging(true);
                }}
                onDragOver={(event) => event.preventDefault()}
                onDragLeave={(event) => {
                  if (!event.currentTarget.contains(event.relatedTarget as Node)) {
                    setIsDragging(false);
                  }
                }}
                onDrop={handleDrop}
              >
                <input
                  ref={fileInputRef}
                  id="knowledge-file"
                  type="file"
                  accept="application/pdf,.pdf"
                  className="sr-only"
                  disabled={isSubmitting}
                  aria-describedby={
                    errors.file
                      ? "knowledge-file-error"
                      : "knowledge-file-description"
                  }
                  aria-invalid={Boolean(errors.file) || undefined}
                  onChange={handleFileChange}
                />

                {file ? (
                  <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex min-w-0 items-center gap-3">
                      <DocumentIcon />
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold text-foreground">
                          {file.name}
                        </p>
                        <p className="caption-text mt-1">
                          PDF · {formatFileSize(file.size)}
                        </p>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={isSubmitting}
                        onClick={() => fileInputRef.current?.click()}
                      >
                        Înlocuiește
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        disabled={isSubmitting}
                        onClick={removeFile}
                      >
                        Elimină
                      </Button>
                    </div>
                  </div>
                ) : (
                  <label
                    htmlFor="knowledge-file"
                    className="flex min-h-36 cursor-pointer flex-col items-center justify-center rounded-[var(--radius-control)] text-center"
                  >
                    <UploadIcon />
                    <span className="mt-4 text-sm font-semibold text-foreground">
                      Alege un PDF
                    </span>
                    <span className="secondary-text mt-1">
                      sau trage fișierul aici
                    </span>
                  </label>
                )}
              </div>
            </FormField>

            <div className="grid gap-5 sm:grid-cols-2">
              <FormField
                id="knowledge-title"
                label="Titlu"
                required
                error={errors.title}
              >
                <Input
                  id="knowledge-title"
                  value={title}
                  maxLength={500}
                  hasError={Boolean(errors.title)}
                  aria-describedby={errors.title ? "knowledge-title-error" : undefined}
                  onChange={(event) => {
                    setTitle(event.target.value);
                    markFormChanged();
                  }}
                />
              </FormField>

              <FormField
                id="knowledge-institution"
                label="Instituția sursă"
                required
                error={errors.sourceInstitution}
              >
                <Input
                  id="knowledge-institution"
                  value={sourceInstitution}
                  maxLength={255}
                  hasError={Boolean(errors.sourceInstitution)}
                  aria-describedby={
                    errors.sourceInstitution
                      ? "knowledge-institution-error"
                      : undefined
                  }
                  onChange={(event) => {
                    setSourceInstitution(event.target.value);
                    markFormChanged();
                  }}
                />
              </FormField>

              <FormField
                id="knowledge-source-url"
                label="Pagina sursei"
                error={errors.sourceUrl}
                description="Opțional. Adresa de unde a fost obținut documentul."
              >
                <Input
                  id="knowledge-source-url"
                  type="url"
                  inputMode="url"
                  value={sourceUrl}
                  placeholder="https://"
                  hasError={Boolean(errors.sourceUrl)}
                  aria-describedby={
                    errors.sourceUrl
                      ? "knowledge-source-url-error"
                      : "knowledge-source-url-description"
                  }
                  onChange={(event) => {
                    setSourceUrl(event.target.value);
                    markFormChanged();
                  }}
                />
              </FormField>

              <FormField
                id="knowledge-published-at"
                label="Data publicării"
                description="Opțional."
              >
                <Input
                  id="knowledge-published-at"
                  type="date"
                  value={publishedAt}
                  max={new Date().toISOString().slice(0, 10)}
                  aria-describedby="knowledge-published-at-description"
                  onChange={(event) => {
                    setPublishedAt(event.target.value);
                    markFormChanged();
                  }}
                />
              </FormField>

              <FormField
                id="knowledge-version"
                label="Versiune"
                description="Opțional. De exemplu: ediția 2025 sau v2.1."
                className="sm:col-span-2"
              >
                <Input
                  id="knowledge-version"
                  value={version}
                  maxLength={100}
                  aria-describedby="knowledge-version-description"
                  onChange={(event) => {
                    setVersion(event.target.value);
                    markFormChanged();
                  }}
                />
              </FormField>
            </div>

            {isPrepared ? (
              <Alert variant="success" title="Document trimis">
                Documentul a fost acceptat și va fi procesat în fundal.
              </Alert>
            ) : null}

            {submitError ? (
              <Alert variant="error" title="Încărcarea a eșuat">
                {submitError}
              </Alert>
            ) : null}

            <div className="flex flex-col-reverse gap-3 border-t border-border pt-5 sm:flex-row sm:items-center sm:justify-end">
              <Button
                type="button"
                variant="ghost"
                disabled={isSubmitting}
                onClick={resetForm}
              >
                Resetează
              </Button>
              <Button
                type="submit"
                variant="primary"
                disabled={isPrepared}
                isLoading={isSubmitting}
                loadingText="Se încarcă..."
              >
                {isPrepared ? "Document trimis" : "Încarcă documentul"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <aside
        className="space-y-6 lg:sticky lg:top-24"
        aria-label="Informații despre documente"
      >
        <Alert variant="info" title="Procesare asincronă">
          După încărcare, documentul este procesat în fundal și devine
          disponibil după finalizarea verificărilor.
        </Alert>

        <section
          className="border-t border-border pt-5"
          aria-labelledby="source-guidance-title"
        >
          <h2 id="source-guidance-title" className="section-title">
            Alege surse de încredere
          </h2>
          <ul className="secondary-text mt-3 space-y-3">
            <li>Folosește ghiduri, protocoale sau publicații verificabile.</li>
            <li>Completează instituția și versiunea pentru trasabilitate.</li>
            <li>Nu încărca documente care conțin date despre pacienți.</li>
          </ul>
        </section>
      </aside>
    </div>
  );
}

function validatePdf(file: File) {
  const hasPdfExtension = file.name.toLocaleLowerCase("ro-RO").endsWith(".pdf");
  const hasPdfType = file.type === "application/pdf";

  if (!hasPdfExtension && !hasPdfType) {
    return "Fișierul selectat trebuie să fie în format PDF.";
  }
  if (file.size === 0) {
    return "Fișierul selectat este gol.";
  }
  if (file.size > MAX_PDF_SIZE_BYTES) {
    return "Fișierul depășește limita de 25 MB.";
  }

  return null;
}

function isValidUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function formatFileSize(size: number) {
  return new Intl.NumberFormat("ro-RO", {
    style: "unit",
    unit: "megabyte",
    maximumFractionDigits: 1,
  }).format(size / (1024 * 1024));
}

function UploadIcon() {
  return (
    <span
      className="flex size-11 items-center justify-center rounded-[var(--radius-control)] bg-primary-soft text-primary"
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        className="size-5"
        stroke="currentColor"
        strokeWidth="1.8"
      >
        <path
          d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M5 14.5V18a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-3.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}

function DocumentIcon() {
  return (
    <span
      className="flex size-10 shrink-0 items-center justify-center rounded-[var(--radius-control)] bg-primary-soft text-primary"
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        className="size-5"
        stroke="currentColor"
        strokeWidth="1.8"
      >
        <path
          d="M7 3.75h6.5L18 8.25v12H7a2 2 0 0 1-2-2V5.75a2 2 0 0 1 2-2Z"
          strokeLinejoin="round"
        />
        <path
          d="M13.5 3.75v4.5H18M8.5 12h6M8.5 15.5h6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}
