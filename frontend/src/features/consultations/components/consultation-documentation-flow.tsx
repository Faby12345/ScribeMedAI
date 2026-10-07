"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Checkbox, Input, Textarea } from "@/components/ui/input";
import { cn } from "@/lib/class-names";
import {
  confirmPatientInformed,
  ConfirmPatientInformedApiError,
} from "@/features/consultations/api/confirm-patient-informed";
import { sendAudio, SendAudioApiError } from "@/features/consultations/api/send-audio";
import { sendNotes, SendNotesApiError } from "@/features/consultations/api/send-notes";
import { MedicationPlanEditor } from "@/features/medication/components/medication-plan-editor";
import type { PrescribedMedicationDraft } from "@/features/medication/types";

type DocumentationSource = "audio" | "notes";
type FlowStep = "source" | "capture";
type AudioMode = "record" | "upload";
type ClinicalNoteTextField = "reason" | "history" | "objective" | "assessment" | "plan";

export type ClinicalNotes = {
  reason: string;
  history: string;
  objective: string;
  assessment: string;
  plan: string;
  medications: PrescribedMedicationDraft[];
};

type ConsultationDocumentationFlowProps = {
  consultationId: string
  isPatientInformed: boolean
};

const emptyNotes: ClinicalNotes = {
  reason: "",
  history: "",
  objective: "",
  assessment: "",
  plan: "",
  medications: [],
};

const testClinicalNotes: ClinicalNotes = {
  reason: "Pacientul se prezintă pentru tuse seacă, rinoree și subfebrilitate apărute de aproximativ 3 zile.",
  history:
    "Simptome debutate progresiv, fără dispnee, durere toracică sau alergii medicamentoase cunoscute. A administrat paracetamol ocazional, cu ameliorare parțială.",
  objective:
    "Stare generală bună, afebril la prezentare. Faringe discret hiperemic, murmur vezicular prezent bilateral, fără raluri. TA 122/78 mmHg, puls 82/min.",
  assessment:
    "Tablou clinic sugestiv pentru infecție acută de căi respiratorii superioare, formă ușoară, fără semne de alarmă la evaluarea curentă.",
  plan: "Tratament simptomatic, hidratare, repaus relativ și reevaluare dacă apare febră persistentă, dispnee, agravarea tusei sau stare generală alterată.",
  medications: [],
};

const showTestNotesPrefill = process.env.NODE_ENV !== "production";

export function ConsultationDocumentationFlow({
    consultationId,
                                                isPatientInformed,
}: ConsultationDocumentationFlowProps) {
  const router = useRouter();
  const [step, setStep] = useState<FlowStep>("source");
  const [source, setSource] = useState<DocumentationSource>("audio");
  const [audioMode, setAudioMode] = useState<AudioMode>("record");
  const [patientInformed, setPatientInformed] = useState(isPatientInformed);
  const [notes, setNotes] = useState<ClinicalNotes>(emptyNotes);
  const [selectedAudioFile, setSelectedAudioFile] = useState<File | null>(null);
  const [recordedAudioBlob, setRecordedAudioBlob] = useState<Blob | null>(null);
  const [recordingState, setRecordingState] = useState<
    "idle" | "recording" | "recorded"
  >("idle");
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [recordingUrl, setRecordingUrl] = useState<string | null>(null);
  const [recordingError, setRecordingError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  const hasNotes = useMemo(
    () =>
      [notes.reason, notes.history, notes.objective, notes.assessment, notes.plan].some(
        (value) => value.trim().length > 0,
      ) || notes.medications.length > 0,
    [notes],
  );

  const hasCompleteMedicationPlan = useMemo(
    () =>
      notes.medications.every(
        (item) =>
          item.dose.trim().length > 0 &&
          item.administrationRoute.trim().length > 0 &&
          item.frequency.trim().length > 0 &&
          item.duration.trim().length > 0,
      ),
    [notes.medications],
  );

  const canContinue = source === "notes" || patientInformed;
  const canSubmit =
    source === "audio"
      ? audioMode === "record"
        ? recordingState === "recorded" && Boolean(recordedAudioBlob)
        : Boolean(selectedAudioFile)
      : hasNotes && hasCompleteMedicationPlan;

  useEffect(() => {
    if (recordingState !== "recording") {
      return;
    }

    const intervalId = window.setInterval(() => {
      setRecordingSeconds((current) => current + 1);
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [recordingState]);

  useEffect(() => {
    return () => {
      if (recordingUrl) {
        window.URL.revokeObjectURL(recordingUrl);
      }
    };
  }, [recordingUrl]);

  async function startRecording() {
    setRecordingError(null);

    if (!navigator.mediaDevices?.getUserMedia) {
      setRecordingError("Înregistrarea audio nu este disponibilă în browser.");
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      audioChunksRef.current = [];

      recorder.addEventListener("dataavailable", (event) => {
        if (event.data.size > 0) {
          audioChunksRef.current.push(event.data);
        }
      });

      recorder.addEventListener("stop", () => {
        const audioBlob = new Blob(audioChunksRef.current, {
          type: recorder.mimeType || "audio/webm",
        });
        const nextUrl = window.URL.createObjectURL(audioBlob);

        if (recordingUrl) {
          window.URL.revokeObjectURL(recordingUrl);
        }

        setRecordedAudioBlob(audioBlob);
        setRecordingUrl(nextUrl);
        setRecordingState("recorded");
        stream.getTracks().forEach((track) => track.stop());
      });

      mediaRecorderRef.current = recorder;
      setSelectedAudioFile(null);
      setRecordedAudioBlob(null);
      setRecordingUrl(null);
      setRecordingSeconds(0);
      setRecordingState("recording");
      recorder.start();
    } catch {
      setRecordingError(
        "Microfonul nu a putut fi pornit. Verifică permisiunea browserului.",
      );
    }
  }

  function stopRecording() {
    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.stop();
    }
  }

  function resetAudio() {
    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.stop();
    }

    if (recordingUrl) {
      window.URL.revokeObjectURL(recordingUrl);
    }

    setRecordingUrl(null);
    setSelectedAudioFile(null);
    setRecordedAudioBlob(null);
    setRecordingSeconds(0);
    setRecordingState("idle");
    setRecordingError(null);
  }

  function updateNote(field: ClinicalNoteTextField, value: string) {
    setNotes((current) => ({
      ...current,
      [field]: value,
    }));
  }

  function prefillTestNotes() {
    setNotes(testClinicalNotes);
  }



  async function submitForProcessing() {
    if (!canSubmit) {
      return;
    }

    setIsSubmitting(true);

    if(source === "notes"){

      try {
        const response = await sendNotes(consultationId, notes);
        router.push(`/consultations/${response.consultationId}/review`);
        router.refresh();
      } catch (error) {
        setRecordingError(
          error instanceof SendNotesApiError
            ? error.message
            : "Notițele nu au putut fi salvate. Încearcă din nou.",
        );
        setIsSubmitting(false);
      }


      return;

    } else {
      const audioFile =
        audioMode === "upload"
          ? selectedAudioFile
          : recordedAudioBlob
            ? new File([recordedAudioBlob], "consultatie.webm", {
                type: recordedAudioBlob.type || "audio/webm",
              })
            : null;

      if (!audioFile) {
        setRecordingError("Alege sau înregistrează un fișier audio înainte de trimitere.");
        setIsSubmitting(false);
        return;
      }

      try {
        if (!isPatientInformed) {
          await confirmPatientInformed(consultationId);
        }

        await sendAudio(consultationId, audioFile);
        router.push(`/consultations/${consultationId}`);
        router.refresh();
      } catch (error) {
        setRecordingError(
          error instanceof SendAudioApiError ||
            error instanceof ConfirmPatientInformedApiError
            ? error.message
            : "Audio-ul nu a putut fi trimis. Încearcă din nou.",
        );
        setIsSubmitting(false);
      }
    }
  }

  return (
    <section
      aria-labelledby="documentation-flow-title"
      className="min-w-0"
    >
      <div>
        <div className="mb-7 flex min-w-0 flex-col gap-5 border-b border-border pb-5 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0">
            <h2 id="documentation-flow-title" className="text-xl font-semibold text-foreground">
              Documentare consultație
            </h2>
            <p className="secondary-text mt-1 max-w-2xl">
              Trimite audio pentru procesare sau creează direct un document din
              notițele clinice.
            </p>
          </div>
          <StepIndicator currentStep={step} />
        </div>

        {step === "source" ? (
          <SourceStep
            isPatientAlreadyInformed={isPatientInformed}
            patientInformed={patientInformed}
            source={source}
            onPatientInformedChange={setPatientInformed}
            onSourceChange={setSource}
            onContinue={() => {
              if (canContinue) {
                setStep("capture");
              }
            }}
          />
        ) : (
          <CaptureStep
            audioMode={audioMode}
            canSubmit={canSubmit}
            isSubmitting={isSubmitting}
            notes={notes}
            recordingError={recordingError}
            recordingSeconds={recordingSeconds}
            recordingState={recordingState}
            recordingUrl={recordingUrl}
            selectedFileName={selectedAudioFile?.name ?? null}
            source={source}
            onAudioModeChange={setAudioMode}
            onBack={() => setStep("source")}
            onFileSelected={(file) => {
              setSelectedAudioFile(file);
              setRecordedAudioBlob(null);
              setRecordingUrl(null);
              setRecordingState("idle");
            }}
            onNoteChange={updateNote}
            onMedicationsChange={(medications) =>
              setNotes((current) => ({ ...current, medications }))
            }
            onResetAudio={resetAudio}
            onStartRecording={startRecording}
            onStopRecording={stopRecording}
            onSubmit={submitForProcessing}
            onPrefillTestNotes={prefillTestNotes}
          />
        )}
      </div>
    </section>
  );
}

function StepIndicator({ currentStep }: { currentStep: FlowStep }) {
  const steps: Array<{ key: FlowStep; label: string }> = [
    { key: "source", label: "Sursă" },
    { key: "capture", label: "Conținut" },
  ];

  return (
    <ol className="flex items-center" aria-label="Pași documentare">
      {steps.map((step, index) => {
        const isActive = currentStep === step.key;
        const isComplete = currentStep === "capture" && step.key === "source";

        return (
          <li
            key={step.key}
            className={cn(
              "relative inline-flex min-w-0 items-center gap-2 text-sm",
              index > 0 ? "ml-8 before:absolute before:right-full before:top-1/2 before:mr-2 before:h-px before:w-4 before:bg-border" : undefined,
              isActive ? "font-semibold text-foreground" : "text-muted-foreground",
            )}
          >
            <span
              className={cn(
                "inline-flex size-6 items-center justify-center rounded-full border text-xs",
                isActive
                  ? "border-primary bg-primary text-primary-foreground"
                  : isComplete
                    ? "border-primary bg-primary-soft text-primary"
                    : "border-border bg-surface text-muted-foreground",
              )}
              aria-hidden="true"
            >
              {isComplete ? <CheckIcon /> : index + 1}
            </span>
            {step.label}
          </li>
        );
      })}
    </ol>
  );
}

function SourceStep({
  isPatientAlreadyInformed,
  patientInformed,
  source,
  onPatientInformedChange,
  onSourceChange,
  onContinue,
}: {
  isPatientAlreadyInformed: boolean;
  patientInformed: boolean;
  source: DocumentationSource;
  onPatientInformedChange: (value: boolean) => void;
  onSourceChange: (value: DocumentationSource) => void;
  onContinue: () => void;
}) {
  const canContinue = source === "notes" || patientInformed;

  return (
    <div className="grid min-w-0 gap-6">
      <div>
        <h3 className="text-base font-semibold text-foreground">
          Alege sursa documentării
        </h3>
        <p className="secondary-text mt-1">
          Poți înregistra consultația sau poți completa documentul manual.
        </p>
      </div>

      <div className="divide-y divide-border border-y border-border" role="group" aria-label="Sursa documentării">
        <SourceChoice
          value="audio"
          currentValue={source}
          title="Audio"
          description="Înregistrare directă sau fișier audio, apoi transcriere și SOAP în fundal."
          onChange={onSourceChange}
        />
        <SourceChoice
          value="notes"
          currentValue={source}
          title="Notițe"
          description="Notițe clinice structurate, salvate direct ca draft fără procesare AI."
          onChange={onSourceChange}
        />
      </div>

      {source === "audio" ? (
        <div className="max-w-2xl rounded-[var(--radius-control)] bg-surface-muted px-4 py-3">
          <Checkbox
            checked={patientInformed}
            disabled={isPatientAlreadyInformed}
            label="Pacientul a fost informat pentru înregistrarea audio"
            description="Continuă cu audio doar după confirmarea informării."
            onChange={(event) =>
              onPatientInformedChange(event.currentTarget.checked)
            }
          />
        </div>
      ) : null}

      {!canContinue ? (
        <Alert variant="warning" title="Confirmare necesară">
          Pentru documentarea audio trebuie confirmată informarea pacientului.
        </Alert>
      ) : null}

      <div className="flex justify-end pt-1">
        <Button
          type="button"
          className="w-full sm:w-auto"
          disabled={!canContinue}
          onClick={onContinue}
        >
          Continuă
        </Button>
      </div>
    </div>
  );
}

function SourceChoice({
  value,
  currentValue,
  title,
  description,
  onChange,
}: {
  value: DocumentationSource;
  currentValue: DocumentationSource;
  title: string;
  description: string;
  onChange: (value: DocumentationSource) => void;
}) {
  const isSelected = currentValue === value;

  return (
    <button
      type="button"
      className={cn(
        "group flex w-full min-w-0 items-start gap-4 px-1 py-5 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset",
        isSelected
          ? "text-foreground"
          : "text-muted-foreground hover:bg-surface-muted/70 hover:text-foreground",
      )}
      role="radio"
      aria-checked={isSelected}
      onClick={() => onChange(value)}
    >
      <span className={cn(
        "mt-0.5 flex size-10 shrink-0 items-center justify-center rounded-[var(--radius-control)]",
        isSelected ? "bg-primary-soft text-primary" : "bg-surface-muted text-muted-foreground",
      )} aria-hidden="true">
        {value === "audio" ? <AudioIcon /> : <NotesIcon />}
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex min-w-0 items-center gap-3">
          <span className="text-base font-semibold">{title}</span>
          {isSelected ? <span className="text-xs font-medium text-primary">Selectat</span> : null}
        </span>
        <span className="secondary-text mt-1 block max-w-xl">{description}</span>
      </span>
      <span className={cn(
        "mt-2 size-4 shrink-0 rounded-full border-2",
        isSelected ? "border-[5px] border-primary" : "border-input",
      )} aria-hidden="true" />
    </button>
  );
}

function CaptureStep({
  audioMode,
  canSubmit,
  isSubmitting,
  notes,
  recordingError,
  recordingSeconds,
  recordingState,
  recordingUrl,
  selectedFileName,
  source,
  onAudioModeChange,
  onBack,
  onFileSelected,
  onNoteChange,
  onMedicationsChange,
  onResetAudio,
  onStartRecording,
  onStopRecording,
  onSubmit,
  onPrefillTestNotes,
}: {
  audioMode: AudioMode;
  canSubmit: boolean;
  isSubmitting: boolean;
  notes: ClinicalNotes;
  recordingError: string | null;
  recordingSeconds: number;
  recordingState: "idle" | "recording" | "recorded";
  recordingUrl: string | null;
  selectedFileName: string | null;
  source: DocumentationSource;
  onAudioModeChange: (mode: AudioMode) => void;
  onBack: () => void;
  onFileSelected: (file: File) => void;
  onNoteChange: (field: ClinicalNoteTextField, value: string) => void;
  onMedicationsChange: (medications: PrescribedMedicationDraft[]) => void;
  onResetAudio: () => void;
  onStartRecording: () => void;
  onStopRecording: () => void;
  onSubmit: () => void;
  onPrefillTestNotes: () => void;
}) {
  return (
    <div className="grid min-w-0 gap-6">
      <div>
        <h3 className="text-base font-semibold text-foreground">
          {source === "audio" ? "Pregătește înregistrarea" : "Completează notițele clinice"}
        </h3>
        <p className="secondary-text mt-1">
          {source === "audio"
            ? "Înregistrează direct sau selectează un fișier audio existent."
            : "Completează numai informațiile relevante; câmpurile pot fi editate și în etapa de revizuire."}
        </p>
      </div>
      {source === "audio" ? (
        <AudioCapture
          audioMode={audioMode}
          recordingError={recordingError}
          recordingSeconds={recordingSeconds}
          recordingState={recordingState}
          recordingUrl={recordingUrl}
          selectedFileName={selectedFileName}
          onAudioModeChange={onAudioModeChange}
          onFileSelected={onFileSelected}
          onResetAudio={onResetAudio}
          onStartRecording={onStartRecording}
          onStopRecording={onStopRecording}
        />
      ) : (
        <>
          {recordingError ? (
            <Alert variant="error" title="Documentul nu a putut fi salvat">
              {recordingError}
            </Alert>
          ) : null}
          <NotesCapture
            notes={notes}
            onChange={onNoteChange}
            onMedicationsChange={onMedicationsChange}
            onPrefillTestNotes={onPrefillTestNotes}
          />
        </>
      )}

      <div className="sticky bottom-0 z-20 -mx-5 flex min-w-0 flex-col gap-3 border-t border-border bg-surface px-5 py-4 sm:-mx-6 sm:px-6 md:flex-row md:items-center md:justify-between">
        <p className="secondary-text min-w-0 max-w-xl">
          {source === "audio"
            ? "După trimitere revii în panou. Transcrierea și generarea SOAP continuă în fundal."
            : "Notițele sunt salvate direct ca draft clinic și nu sunt trimise unui furnizor AI."}
        </p>
        <div className="flex w-full flex-col-reverse gap-2 sm:flex-row sm:justify-end md:w-auto md:shrink-0">
          <Button
            type="button"
            variant="outline"
            className="w-full sm:w-auto"
            disabled={isSubmitting}
            onClick={onBack}
          >
            Înapoi
          </Button>
          <Button
            type="button"
            disabled={!canSubmit}
            isLoading={isSubmitting}
            loadingText="Se trimite"
            className="w-full sm:w-auto"
            onClick={onSubmit}
          >
            {source === "audio" ? "Trimite spre procesare" : "Salvează documentul"}
          </Button>
        </div>
      </div>
    </div>
  );
}

function AudioCapture({
  audioMode,
  recordingError,
  recordingSeconds,
  recordingState,
  recordingUrl,
  selectedFileName,
  onAudioModeChange,
  onFileSelected,
  onResetAudio,
  onStartRecording,
  onStopRecording,
}: {
  audioMode: AudioMode;
  recordingError: string | null;
  recordingSeconds: number;
  recordingState: "idle" | "recording" | "recorded";
  recordingUrl: string | null;
  selectedFileName: string | null;
  onAudioModeChange: (mode: AudioMode) => void;
  onFileSelected: (file: File) => void;
  onResetAudio: () => void;
  onStartRecording: () => void;
  onStopRecording: () => void;
}) {
  return (
    <div className="grid min-w-0 gap-5">
      <div className="flex w-full border-b border-border sm:w-fit">
        <ModeButton
          isSelected={audioMode === "record"}
          label="Înregistrează"
          onClick={() => onAudioModeChange("record")}
        />
        <ModeButton
          isSelected={audioMode === "upload"}
          label="Încarcă fișier"
          onClick={() => onAudioModeChange("upload")}
        />
      </div>

      {recordingError ? (
        <Alert variant="error" title="Înregistrarea nu poate porni">
          {recordingError}
        </Alert>
      ) : null}

      {audioMode === "record" ? (
        <div className="grid min-w-0 gap-5 border-y border-border py-6">
          <div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="min-w-0">
              <p className="text-lg font-semibold text-foreground">
                {recordingState === "recording"
                  ? "Înregistrare în curs"
                  : recordingState === "recorded"
                    ? "Audio pregătit"
                    : "Recorder audio"}
              </p>
              <p className="mt-1 text-3xl font-semibold tracking-normal text-primary" aria-live="polite">
                {formatDuration(recordingSeconds)}
              </p>
            </div>
            <div className="flex w-full min-w-0 flex-col gap-2 sm:flex-row md:w-auto">
              {recordingState === "recording" ? (
                <Button
                  type="button"
                  variant="destructive"
                  className="w-full sm:w-auto"
                  onClick={onStopRecording}
                >
                  Oprește
                </Button>
              ) : (
                <Button
                  type="button"
                  className="w-full sm:w-auto"
                  onClick={onStartRecording}
                >
                  Pornește înregistrarea
                </Button>
              )}
              <Button
                type="button"
                variant="outline"
                className="w-full sm:w-auto"
                onClick={onResetAudio}
              >
                Resetează
              </Button>
            </div>
          </div>

          {recordingUrl ? (
            <audio className="w-full min-w-0" controls src={recordingUrl}>
              Browserul nu poate reda această înregistrare.
            </audio>
          ) : null}
        </div>
      ) : (
        <label className="grid min-w-0 gap-2">
          <span className="text-sm font-medium text-foreground">
            Fișier audio
          </span>
          <Input
            type="file"
            accept="audio/*"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) {
                onFileSelected(file);
              }
            }}
          />
          <span className="secondary-text">
            {selectedFileName
              ? `Fișier selectat: ${selectedFileName}`
              : "Alege fișierul audio al consultației."}
          </span>
        </label>
      )}
    </div>
  );
}

function NotesCapture({
  notes,
  onChange,
  onMedicationsChange,
  onPrefillTestNotes,
}: {
  notes: ClinicalNotes;
  onChange: (field: ClinicalNoteTextField, value: string) => void;
  onMedicationsChange: (medications: PrescribedMedicationDraft[]) => void;
  onPrefillTestNotes: () => void;
}) {
  return (
    <div className="grid min-w-0 gap-5">
      {showTestNotesPrefill ? (
        <div className="flex min-w-0 flex-col gap-3 border-y border-border bg-surface-muted px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <p className="text-sm font-semibold text-foreground">
              Date fictive pentru testare
            </p>
            <p className="secondary-text mt-1">
              Completează rapid câmpurile de notițe ca să verifici fluxul.
            </p>
          </div>
          <Button
            type="button"
            variant="secondary"
            className="w-full sm:w-auto"
            onClick={onPrefillTestNotes}
          >
            Completează
          </Button>
        </div>
      ) : null}
      <NoteField
        id="reason"
        label="Motivul prezentării"
        value={notes.reason}
        onChange={(value) => onChange("reason", value)}
      />
      <NoteField
        id="history"
        label="Anamneză și simptome"
        value={notes.history}
        onChange={(value) => onChange("history", value)}
      />
      <NoteField
        id="objective"
        label="Examen obiectiv"
        value={notes.objective}
        onChange={(value) => onChange("objective", value)}
      />
      <NoteField
        id="assessment"
        label="Evaluare clinică"
        value={notes.assessment}
        onChange={(value) => onChange("assessment", value)}
      />
      <NoteField
        id="plan"
        label="Plan"
        value={notes.plan}
        onChange={(value) => onChange("plan", value)}
      />
      <MedicationPlanEditor
        value={notes.medications}
        onChange={onMedicationsChange}
      />
    </div>
  );
}

function NoteField({
  id,
  label,
  value,
  onChange,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="grid min-w-0 gap-2">
      <span className="text-sm font-medium text-foreground">{label}</span>
      <Textarea
        id={id}
        value={value}
        className="min-h-28 border-input bg-surface shadow-none"
        onChange={(event) => onChange(event.target.value)}
      />
    </label>
  );
}

function ModeButton({
  isSelected,
  label,
  onClick,
}: {
  isSelected: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      className={cn(
        "relative min-h-10 min-w-0 px-4 py-2 text-sm font-medium leading-tight transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        isSelected
          ? "text-primary after:absolute after:inset-x-2 after:bottom-[-1px] after:h-0.5 after:bg-primary"
          : "text-muted-foreground hover:text-foreground",
      )}
      aria-pressed={isSelected}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

function AudioIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><rect x="9" y="3" width="6" height="11" rx="3" /><path d="M5 11a7 7 0 0 0 14 0M12 18v3M9 21h6" /></svg>;
}

function NotesIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M6 3h9l3 3v15H6z" /><path d="M14 3v4h4M9 11h6M9 15h6" /></svg>;
}

function CheckIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-3.5" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 4 4L19 6" /></svg>;
}

function formatDuration(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}
