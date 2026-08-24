import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { ButtonLink } from "@/components/ui/button";
import {
  GetDocumentReviewApiError,
  getDocumentReview,
} from "@/features/documents/api/get-document-review";
import { DocumentReviewPage } from "@/features/documents/components/document-review-page";
import type { DocumentReviewDocument } from "@/features/documents/types";
import { getCurrentUser } from "@/features/auth/api/current-user";

type ConsultationReviewPageProps = {
  params: Promise<{
    consultationId: string;
  }>;
};

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export default async function ConsultationReviewPage({
  params,
}: ConsultationReviewPageProps) {
  const { consultationId } = await params;

  if (!uuidPattern.test(consultationId)) {
    notFound();
  }

  const requestCookies = await cookies();
  const user = await getCurrentUser(requestCookies);

  if (!user) {
    redirect("/login");
  }

  let reviewDocument: DocumentReviewDocument;

  try {
    reviewDocument = await getDocumentReview(consultationId, requestCookies);
  } catch (error) {
    if (
      error instanceof GetDocumentReviewApiError &&
      (error.status === 401 || error.status === 403)
    ) {
      redirect("/login");
    }

    if (error instanceof GetDocumentReviewApiError && error.status === 404) {
      return (
        <div className="page-container py-7 sm:py-8">
          <div className="max-w-2xl">
            <Alert variant="warning" title="Draft indisponibil">
              Documentul clinic nu este încă pregătit pentru revizuire.
            </Alert>
            <div className="mt-4">
              <ButtonLink
                href={`/consultations/${consultationId}`}
                variant="outline"
              >
                Înapoi la consultație
              </ButtonLink>
            </div>
          </div>
        </div>
      );
    }

    throw error;
  }

  return (
    <DocumentReviewPage
      consultationId={consultationId}
      reviewDocument={reviewDocument}
    />
  );
}
