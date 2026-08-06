"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import {
  CreateConsultationApiError,
  createConsultation,
} from "@/features/consultations/api/create-consultation";

type StartPatientConsultationButtonProps = {
  patientId: string;
};

export function StartPatientConsultationButton({
  patientId,
}: StartPatientConsultationButtonProps) {
  const router = useRouter();
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleStartConsultation() {
    if (isCreating) {
      return;
    }

    setIsCreating(true);
    setError(null);

    try {
      const consultation = await createConsultation(patientId);
      router.push(`/consultations/${consultation.id}`);
      router.refresh();
    } catch (createError) {
      if (createError instanceof CreateConsultationApiError) {
        setError(createError.message);
      } else {
        setError("Consultația nu a putut fi creată. Încearcă din nou.");
      }
      setIsCreating(false);
    }
  }

  return (
    <div className="flex flex-col items-start gap-2 sm:items-end">
      <Button
        type="button"
        variant="primary"
        isLoading={isCreating}
        loadingText="Se creează"
        onClick={handleStartConsultation}
      >
        Începe consultația
      </Button>
      {error ? (
        <p className="max-w-72 text-sm text-destructive" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}
