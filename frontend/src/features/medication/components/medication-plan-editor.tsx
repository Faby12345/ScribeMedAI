"use client";

import { useEffect, useRef, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input, Select, Textarea } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { searchMedications } from "@/features/medication/api/search-medications";
import type {
  MedicationResponse,
  PrescribedMedicationDraft,
} from "@/features/medication/types";

type MedicationPlanEditorProps = {
  value: PrescribedMedicationDraft[];
  onChange: (value: PrescribedMedicationDraft[]) => void;
};

const emptyResults: MedicationResponse[] = [];

export function MedicationPlanEditor({
  value,
  onChange,
}: MedicationPlanEditorProps) {
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<MedicationResponse[]>(emptyResults);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const hasIncompleteMedication = value.some(
    (item) =>
      !item.dose.trim() ||
      !item.administrationRoute.trim() ||
      !item.frequency.trim() ||
      !item.duration.trim(),
  );

  useEffect(() => {
    if (!isSearchOpen) {
      return;
    }

    searchInputRef.current?.focus();

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsSearchOpen(false);
      }
    };

    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [isSearchOpen]);

  useEffect(() => {
    const normalizedQuery = query.trim();
    if (!isSearchOpen || normalizedQuery.length < 2) {
      return;
    }

    const controller = new AbortController();
    const timeoutId = window.setTimeout(async () => {
      setIsSearching(true);
      setSearchError(null);

      try {
        const medications = await searchMedications(
          normalizedQuery,
          controller.signal,
        );
        setResults(medications.slice(0, 5));
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
        setResults(emptyResults);
        setSearchError(
          error instanceof Error
            ? error.message
            : "Medicamentele nu au putut fi căutate.",
        );
      } finally {
        if (!controller.signal.aborted) {
          setIsSearching(false);
        }
      }
    }, 1000);

    return () => {
      window.clearTimeout(timeoutId);
      controller.abort();
    };
  }, [isSearchOpen, query, retryKey]);

  function addMedication(medication: MedicationResponse) {
    if (value.some((item) => item.medication.cimCode === medication.cimCode)) {
      return;
    }

    onChange([
      ...value,
      {
        medication,
        dose: "",
        administrationRoute: "",
        frequency: "",
        duration: "",
        quantity: "",
        instructions: "",
        notes: "",
      },
    ]);
  }

  function updateMedication(
    cimCode: string,
    field: Exclude<keyof PrescribedMedicationDraft, "medication">,
    fieldValue: string,
  ) {
    onChange(
      value.map((item) =>
        item.medication.cimCode === cimCode
          ? { ...item, [field]: fieldValue }
          : item,
      ),
    );
  }

  return (
    <>
      <section aria-labelledby="medication-plan-title" className="mt-3 border-t border-border pt-7">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h3 id="medication-plan-title" className="text-base font-semibold text-foreground">
              Tratament medicamentos
            </h3>
            <p className="secondary-text mt-1">
              Medicamentele selectate sunt adăugate documentului clinic în ordinea de mai jos.
            </p>
          </div>
          {value.length > 0 ? (
            <span className="text-sm font-medium text-muted-foreground">
              {value.length} {value.length === 1 ? "medicament" : "medicamente"}
            </span>
          ) : null}
        </div>

        {value.length === 0 ? (
          <div className="mt-4 border-y border-dashed border-border py-5 text-sm text-muted-foreground">
            Nu ai adăugat încă niciun medicament. Folosește acțiunea rapidă din dreapta ecranului.
          </div>
        ) : (
          <ol className="mt-4 border-b border-border">
            {value.map((item, index) => (
              <MedicationPlanItem
                key={item.medication.cimCode}
                index={index}
                item={item}
                onRemove={() =>
                  onChange(
                    value.filter(
                      (candidate) =>
                        candidate.medication.cimCode !== item.medication.cimCode,
                    ),
                  )
                }
                onUpdate={(field, fieldValue) =>
                  updateMedication(item.medication.cimCode, field, fieldValue)
                }
              />
            ))}
          </ol>
        )}
        {hasIncompleteMedication ? (
          <Alert variant="warning" title="Schema de tratament este incompletă" className="mt-4">
            Completează doza, calea de administrare, frecvența și durata pentru fiecare medicament.
          </Alert>
        ) : null}
      </section>

      <div className="fixed bottom-28 right-4 z-40 flex flex-col items-end gap-3 sm:bottom-24 sm:right-7">
        {isSearchOpen ? (
          <MedicationSearchPanel
            query={query}
            results={results}
            selected={value}
            isSearching={isSearching}
            searchError={searchError}
            inputRef={searchInputRef}
            onClose={() => setIsSearchOpen(false)}
            onQueryChange={(nextQuery) => {
              setQuery(nextQuery);
              setResults(emptyResults);
              setSearchError(null);
              setIsSearching(false);
            }}
            onRetry={() => setRetryKey((current) => current + 1)}
            onAdd={addMedication}
          />
        ) : null}

        <Button
          type="button"
          size="icon"
          className="size-12 rounded-full shadow-lg sm:size-14"
          aria-label={isSearchOpen ? "Închide căutarea medicamentelor" : "Adaugă medicament"}
          aria-expanded={isSearchOpen}
          aria-controls="medication-search-panel"
          onClick={() => setIsSearchOpen((current) => !current)}
        >
          {isSearchOpen ? <CloseIcon /> : <MedicationIcon />}
          <span className="sr-only">
            {isSearchOpen ? "Închide" : "Adaugă medicament"}
          </span>
        </Button>
      </div>
    </>
  );
}

function MedicationSearchPanel({
  query,
  results,
  selected,
  isSearching,
  searchError,
  inputRef,
  onClose,
  onQueryChange,
  onRetry,
  onAdd,
}: {
  query: string;
  results: MedicationResponse[];
  selected: PrescribedMedicationDraft[];
  isSearching: boolean;
  searchError: string | null;
  inputRef: React.RefObject<HTMLInputElement | null>;
  onClose: () => void;
  onQueryChange: (value: string) => void;
  onRetry: () => void;
  onAdd: (medication: MedicationResponse) => void;
}) {
  const normalizedQuery = query.trim();

  return (
    <aside
      id="medication-search-panel"
      aria-labelledby="medication-search-title"
      className="w-[calc(100vw-2rem)] max-w-md overflow-hidden rounded-xl border border-border bg-surface shadow-xl"
    >
      <div className="flex items-start justify-between gap-4 border-b border-border/70 px-4 py-4">
        <div>
          <h3 id="medication-search-title" className="font-semibold text-foreground">
            Adaugă medicament
          </h3>
          <p className="mt-1 text-sm text-muted-foreground">
            Caută după denumire, substanță activă, cod CIM sau ATC.
          </p>
        </div>
        <Button type="button" variant="ghost" size="icon" className="-mr-2 -mt-2" aria-label="Închide" onClick={onClose}>
          <CloseIcon />
        </Button>
      </div>

      <div className="p-4">
        <label htmlFor="medication-search" className="text-sm font-medium text-foreground">
          Caută în nomenclator
        </label>
        <Input
          id="medication-search"
          ref={inputRef}
          type="search"
          value={query}
          className="mt-2"
          placeholder="Ex.: Nurofen, ibuprofen, CIM..."
          autoComplete="off"
          leadingIcon={<SearchIcon />}
          trailingIcon={isSearching ? <Spinner /> : undefined}
          onChange={(event) => onQueryChange(event.target.value)}
        />

        <div className="mt-4 max-h-[min(24rem,50vh)] overflow-y-auto" aria-live="polite">
          {normalizedQuery.length < 2 ? (
            <p className="py-5 text-center text-sm text-muted-foreground">
              Introdu cel puțin 2 caractere pentru căutare.
            </p>
          ) : isSearching ? (
            <p className="flex items-center justify-center gap-2 py-5 text-sm text-muted-foreground">
              <Spinner /> Se caută medicamentele…
            </p>
          ) : searchError ? (
            <Alert variant="error" title="Căutarea a eșuat">
              <span>{searchError}</span>
              <Button type="button" variant="outline" size="sm" className="mt-3" onClick={onRetry}>
                Încearcă din nou
              </Button>
            </Alert>
          ) : results.length === 0 ? (
            <p className="py-5 text-center text-sm text-muted-foreground">
              Nu am găsit medicamente pentru această căutare.
            </p>
          ) : (
            <ul className="divide-y divide-border/70">
              {results.map((medication) => {
                const isAdded = selected.some(
                  (item) => item.medication.cimCode === medication.cimCode,
                );

                return (
                  <li key={medication.cimCode} className="flex min-w-0 items-start gap-3 py-3 first:pt-0 last:pb-0">
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-foreground">
                        {medication.commercialName ?? "Denumire indisponibilă"}
                      </p>
                      <p className="mt-0.5 text-sm text-muted-foreground">
                        {formatMedicationDetails(medication)}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        CIM {medication.cimCode}
                        {medication.atcCode ? ` · ATC ${medication.atcCode}` : ""}
                      </p>
                    </div>
                    <Button
                      type="button"
                      variant={isAdded ? "secondary" : "outline"}
                      size="sm"
                      disabled={isAdded}
                      onClick={() => onAdd(medication)}
                    >
                      {isAdded ? "Adăugat" : "Adaugă"}
                    </Button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </aside>
  );
}

function MedicationPlanItem({
  item,
  index,
  onRemove,
  onUpdate,
}: {
  item: PrescribedMedicationDraft;
  index: number;
  onRemove: () => void;
  onUpdate: (
    field: Exclude<keyof PrescribedMedicationDraft, "medication">,
    value: string,
  ) => void;
}) {
  const prefix = `medication-${item.medication.cimCode}`;

  return (
    <li className="border-t border-border py-5">
      <div className="flex min-w-0 flex-col gap-3 border-b border-border/70 pb-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <h4 className="font-semibold text-foreground">
            {index + 1}. {item.medication.commercialName ?? "Denumire indisponibilă"}
          </h4>
          <p className="mt-1 text-sm text-muted-foreground">
            {formatMedicationDetails(item.medication)} · CIM {item.medication.cimCode}
          </p>
        </div>
        <Button type="button" variant="destructive" size="sm" onClick={onRemove}>
          Elimină
        </Button>
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <FormField id={`${prefix}-dose`} label="Doză" required>
          <Input
            id={`${prefix}-dose`}
            value={item.dose}
            maxLength={255}
            placeholder="Ex.: 500 mg"
            onChange={(event) => onUpdate("dose", event.target.value)}
          />
        </FormField>
        <FormField id={`${prefix}-route`} label="Cale de administrare" required>
          <Select
            id={`${prefix}-route`}
            value={item.administrationRoute}
            onChange={(event) => onUpdate("administrationRoute", event.target.value)}
          >
            <option value="">Selectează calea</option>
            <option value="Orală">Orală</option>
            <option value="Inhalatorie">Inhalatorie</option>
            <option value="Topică">Topică</option>
            <option value="Injectabilă">Injectabilă</option>
            <option value="Oculară">Oculară</option>
            <option value="Nazală">Nazală</option>
            <option value="Rectală">Rectală</option>
            <option value="Alta">Alta</option>
          </Select>
        </FormField>
        <FormField id={`${prefix}-frequency`} label="Frecvență" required>
          <Input
            id={`${prefix}-frequency`}
            value={item.frequency}
            maxLength={255}
            placeholder="Ex.: de 2 ori pe zi"
            onChange={(event) => onUpdate("frequency", event.target.value)}
          />
        </FormField>
        <FormField id={`${prefix}-duration`} label="Durată" required>
          <Input
            id={`${prefix}-duration`}
            value={item.duration}
            maxLength={255}
            placeholder="Ex.: 7 zile"
            onChange={(event) => onUpdate("duration", event.target.value)}
          />
        </FormField>
        <FormField id={`${prefix}-quantity`} label="Cantitate">
          <Input
            id={`${prefix}-quantity`}
            value={item.quantity}
            maxLength={255}
            placeholder="Ex.: 14 comprimate"
            onChange={(event) => onUpdate("quantity", event.target.value)}
          />
        </FormField>
        <FormField id={`${prefix}-instructions`} label="Instrucțiuni">
          <Input
            id={`${prefix}-instructions`}
            value={item.instructions}
            maxLength={4000}
            placeholder="Ex.: după masă"
            onChange={(event) => onUpdate("instructions", event.target.value)}
          />
        </FormField>
      </div>
      <FormField id={`${prefix}-notes`} label="Observații" className="mt-4">
        <Textarea
          id={`${prefix}-notes`}
          value={item.notes}
          maxLength={4000}
          className="min-h-20"
          placeholder="Observații opționale pentru această recomandare"
          onChange={(event) => onUpdate("notes", event.target.value)}
        />
      </FormField>
    </li>
  );
}

function formatMedicationDetails(medication: MedicationResponse) {
  return [
    medication.activeSubstance,
    medication.pharmaceuticalForm,
    medication.concentration,
  ]
    .filter(Boolean)
    .join(" · ") || "Detalii indisponibile";
}

function MedicationIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="m8.5 15.5 7-7a3.54 3.54 0 0 1 5 5l-7 7a3.54 3.54 0 0 1-5-5Z" />
      <path d="m12 12 5 5" />
      <path d="M5 3v6M2 6h6" />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4-4" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-5" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <path d="m6 6 12 12M18 6 6 18" />
    </svg>
  );
}
