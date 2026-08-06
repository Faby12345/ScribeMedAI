import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { LoginForm } from "@/features/auth/components/login-form";

export default function LoginPage() {
  const initialLoginValues =
    process.env.NODE_ENV === "development"
      ? {
          email: "doctor@example.com",
          password: "your-local-password",
        }
      : undefined;

  return (
    <main className="min-h-screen bg-background">
      <div className="page-container flex min-h-screen items-center py-8 sm:py-12">
        <div className="grid w-full items-center gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(390px,430px)]">
          <section className="max-w-2xl">
            <div className="mb-10 flex items-center gap-3">
              <div
                className="flex size-10 items-center justify-center rounded-[var(--radius-control)] bg-primary text-sm font-semibold text-primary-foreground"
                aria-hidden="true"
              >
                SM
              </div>
              <div>
                <p className="text-base font-semibold text-foreground">
                  ScribeMedAI
                </p>
                <p className="caption-text">Documentație medicală asistată</p>
              </div>
            </div>

            <Badge variant="info">Acces securizat pentru personal medical</Badge>
            <h1 className="mt-5 max-w-xl text-4xl font-semibold leading-tight tracking-normal text-foreground sm:text-5xl">
              Intră în spațiul clinic al cabinetului tău.
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-muted-foreground sm:text-lg sm:leading-8">
              ScribeMedAI ajută medicii să continue fluxul de documentare fără
              a pierde controlul asupra conținutului medical.
            </p>

            <dl className="mt-8 grid max-w-xl gap-5 border-y border-border py-6 sm:grid-cols-3">
              <div>
                <dt className="text-sm font-medium text-foreground">
                  Sesiuni opace
                </dt>
                <dd className="mt-1 text-sm leading-6 text-muted-foreground">
                  Acces gestionat în siguranță pentru cabinet.
                </dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-foreground">
                  Control medical
                </dt>
                <dd className="mt-1 text-sm leading-6 text-muted-foreground">
                  Drafturile necesită aprobare explicită.
                </dd>
              </div>
              <div>
                <dt className="text-sm font-medium text-foreground">
                  Date protejate
                </dt>
                <dd className="mt-1 text-sm leading-6 text-muted-foreground">
                  Fără tokenuri în stocarea locală.
                </dd>
              </div>
            </dl>
          </section>

          <section className="w-full">
            <Card variant="elevated" className="mx-auto max-w-md">
              <CardHeader className="p-6 pb-4">
                <p className="text-sm font-medium text-primary">
                  Bine ai revenit
                </p>
                <h2 className="text-2xl font-semibold tracking-normal text-foreground">
                  Autentificare
                </h2>
                <p className="secondary-text">
                  Folosește contul clinicii pentru a accesa panoul medical.
                </p>
              </CardHeader>
              <CardContent className="p-6 pt-2">
                <LoginForm initialValues={initialLoginValues} />
              </CardContent>
            </Card>

            <p className="mx-auto mt-5 max-w-md text-center text-sm leading-6 text-muted-foreground">
              Ai nevoie de acces pentru clinică?{" "}
              <a
                href="mailto:support@scribemed.ai"
                className="font-medium text-primary underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                Contactează suportul
              </a>
              .
            </p>
          </section>
        </div>
      </div>
    </main>
  );
}
