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
    <main className="flex min-h-screen bg-background px-5 py-8 sm:px-8 lg:px-12">
      <div className="mx-auto grid w-full max-w-6xl items-center gap-10 lg:grid-cols-[minmax(0,0.92fr)_minmax(380px,440px)]">
        <section className="hidden lg:block">
          <div className="max-w-xl">
            <div className="mb-10 inline-flex items-center gap-3">


            </div>

            <p className="mb-4 text-sm font-medium uppercase tracking-[0.08em] text-primary">
              Spațiu pentru documentație clinică
            </p>
            <h1 className="max-w-lg text-4xl font-semibold leading-tight tracking-normal text-foreground">
              Autentifică-te pentru a continua documentarea consultațiilor.
            </h1>
            <p className="mt-5 max-w-lg text-lg leading-8 text-muted-foreground">
              Un spațiu de lucru concentrat pentru înregistrarea consultațiilor,
              revizuirea drafturilor și aprobarea documentelor medicale de către
              medic.
            </p>

            <div className="mt-10 grid max-w-lg gap-4 sm:grid-cols-2">
              <div className="rounded-lg border border-border bg-surface/70 p-4">
                <p className="text-sm font-medium text-foreground">
                  Acces pe bază de sesiune
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Autentificarea este gestionată de backend prin cookie-uri
                  securizate.
                </p>
              </div>
              <div className="rounded-lg border border-border bg-surface/70 p-4">
                <p className="text-sm font-medium text-foreground">
                  Drafturile rămân drafturi
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                  Conținutul generat de AI necesită revizuirea explicită a
                  medicului.
                </p>
              </div>
            </div>
          </div>
        </section>

        <section className="mx-auto w-full max-w-md lg:mx-0">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div
              className="flex size-10 items-center justify-center rounded-lg border border-primary/20 bg-primary-soft text-sm font-semibold text-primary"
              aria-hidden="true"
            >
              SM
            </div>
            <span className="text-lg font-semibold text-foreground">
              ScribeMedAI
            </span>
          </div>

          <Card>
            <CardHeader>
              <p className="text-sm font-medium text-primary">Bine ai revenit</p>
              <h2 className="text-2xl font-semibold tracking-normal text-foreground">
                Autentificare
              </h2>
              <p className="text-sm leading-6 text-muted-foreground">
                Folosește contul clinicii pentru a accesa spațiul de
                documentație medicală.
              </p>
            </CardHeader>
            <CardContent>
              <LoginForm initialValues={initialLoginValues} />
            </CardContent>
          </Card>

          <p className="mt-6 text-center text-sm leading-6 text-muted-foreground">
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
    </main>
  );
}
