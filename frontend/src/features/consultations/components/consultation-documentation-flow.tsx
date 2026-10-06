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
        console.log(response)
        router.push("/dashboard")
        router.refresh()
      } catch (error) {
        setRecordingError(
            error instanceof SendNotesApiError ? error.message : "Notitele nu au putut fi trimise. Incearca din nou"
        )
        setIsSubmitting(false)
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
        router.push("/dashboard");
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
      className="relative min-w-0 overflow-x-clip rounded-xl bg-white/72 px-1 py-2 sm:px-2"
    >
      <div
        className="pointer-events-none absolute inset-x-8 top-0 h-px bg-[linear-gradient(90deg,transparent,#9fdcf1,transparent)]"
        aria-hidden="true"
      />
      <div className="relative">
        <div className="mb-6 flex min-w-0 flex-col gap-4 border-b border-border/70 pb-5 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0">
            <h2 id="documentation-flow-title" className="section-title">
              Documentare consultație
            </h2>
            <p className="secondary-text mt-1 max-w-2xl">
              Trimite audio sau notițe clinice. Procesarea și generarea SOAP
              continuă în fundal.
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
    <ol className="flex flex-wrap gap-2" aria-label="Pași documentare">
      {steps.map((step, index) => {
        const isActive = currentStep === step.key;

        return (
          <li
            key={step.key}
            className={cn(
              "inline-flex min-w-0 items-center gap-2 rounded-full px-3 py-1.5 text-sm",
              isActive
                ? "bg-info-soft text-primary"
                : "bg-transparent text-muted-foreground",
            )}
          >
            <span
              className={cn(
                "inline-flex size-5 items-center justify-center rounded-full text-xs",
                isActive ? "bg-primary text-primary-foreground" : "bg-secondary",
              )}
              aria-hidden="true"
            >
              {index + 1}
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
      <div className="grid min-w-0 gap-3 sm:grid-cols-2">
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
          description="Notițe clinice structurate, trimise direct pentru generarea draftului SOAP."
          onChange={onSourceChange}
        />
      </div>

      {source === "audio" ? (
        <div className="max-w-2xl">
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

      <div className="flex justify-end border-t border-border/70 pt-4">
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
        "group min-w-0 rounded-xl px-4 py-4 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        isSelected
          ? "bg-[linear-gradient(135deg,#ffffff_0%,#eef9ff_100%)] text-foreground"
          : "bg-transparent text-muted-foreground hover:bg-white/70 hover:text-foreground",
      )}
      aria-pressed={isSelected}
      onClick={() => onChange(value)}
    >
      <span className="flex min-w-0 items-center gap-3">
        <span
          className={cn(
            "size-2.5 rounded-full",
            isSelected ? "bg-primary" : "bg-muted-foreground",
          )}
          aria-hidden="true"
        />
        <span className="text-base font-semibold">{title}</span>
      </span>
      <span className="secondary-text mt-2 block">{description}</span>
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
        <NotesCapture
          notes={notes}
          onChange={onNoteChange}
          onMedicationsChange={onMedicationsChange}
          onPrefillTestNotes={onPrefillTestNotes}
        />
      )}

      <div className="flex min-w-0 flex-col gap-3 border-t border-border/70 pt-4 md:flex-row md:items-center md:justify-between">
        <p className="secondary-text min-w-0 max-w-xl">
          După trimitere revii în panou. Transcrierea și generarea SOAP vor
          continua în fundal când backendul va conecta acest pas.
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
            Trimite spre procesare
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
      <div className="grid w-full grid-cols-2 rounded-full bg-surface-muted p-1 sm:inline-grid sm:w-fit">
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
        <div className="grid min-w-0 gap-4 rounded-xl bg-[linear-gradient(135deg,#ffffff_0%,#f0faff_100%)] p-4 sm:p-5">
          <div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div className="min-w-0">
              <p className="text-lg font-semibold text-foreground">
                {recordingState === "recording"
                  ? "Înregistrare în curs"
                  : recordingState === "recorded"
                    ? "Audio pregătit"
                    : "Recorder audio"}
              </p>
              <p className="mt-1 text-3xl font-semibold tracking-normal text-primary">
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
    <div className="grid min-w-0 gap-4">
      {showTestNotesPrefill ? (
        <div className="flex min-w-0 flex-col gap-3 rounded-xl bg-surface-muted p-4 sm:flex-row sm:items-center sm:justify-between">
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
        className="min-h-24 border-border/70 bg-white/82 shadow-none"
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
        "min-h-9 min-w-0 rounded-full px-3 py-2 text-sm font-medium leading-tight transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        isSelected
          ? "bg-white text-primary shadow-surface"
          : "text-muted-foreground hover:text-foreground",
      )}
      aria-pressed={isSelected}
      onClick={onClick}
    >
      {label}
    </button>
  );
}

function formatDuration(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}
