"use client";

import { useRouter } from "next/navigation";

import { Card } from "@/components/ui/card";
import { CreatePatientForm } from "@/features/patients/components/create-patient-form";
import type { Patient } from "@/features/patients/types";

export function CreatePatientPageForm() {
  const router = useRouter();

  function handleCreated(patient: Patient) {
    router.push(`/patients/${patient.id}`);
    router.refresh();
  }

  return (
    <Card>
      <CreatePatientForm
        onCancel={() => router.push("/patients")}
        onCreated={handleCreated}
      />
    </Card>
  );
}
