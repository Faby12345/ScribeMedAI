"use client";

import { useEffect, useRef, useState } from "react";

import { Button, ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { KnowledgeDocumentList } from "@/features/knowledge/components/knowledge-document-list";
import { KnowledgeDocumentUpload } from "@/features/knowledge/components/knowledge-document-upload";

export function KnowledgeLibrary() {
  const [isUploadVisible, setIsUploadVisible] = useState(false);
  const [listKey, setListKey] = useState(0);
  const uploadSectionRef = useRef<HTMLElement>(null);

  useEffect(() => {
    if (!isUploadVisible || !uploadSectionRef.current) {
      return;
    }

    const section = uploadSectionRef.current;
    const prefersReducedMotion = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    ).matches;

    section.focus({ preventScroll: true });
    section.scrollIntoView({
      behavior: prefersReducedMotion ? "auto" : "smooth",
      block: "start",
    });
  }, [isUploadVisible]);

  return (
    <>
      <PageHeader
        title="Bibliotecă de cunoștințe"
        description="Gestionează sursele PDF folosite de asistent pentru răspunsuri bazate pe documente."
        actions={
          <>
            <ButtonLink href="/knowledge/chat" variant="outline">
              Deschide asistentul
            </ButtonLink>
            {!isUploadVisible ? (
              <Button
                type="button"
                aria-expanded={false}
                aria-controls="knowledge-upload-section"
                onClick={() => setIsUploadVisible(true)}
              >
                Adaugă document
              </Button>
            ) : null}
          </>
        }
      />

      {isUploadVisible ? (
        <section
          ref={uploadSectionRef}
          id="knowledge-upload-section"
          className="mb-8 scroll-mt-6 outline-none"
          aria-label="Adaugă document"
          tabIndex={-1}
        >
          <KnowledgeDocumentUpload
            onClose={() => setIsUploadVisible(false)}
            onUploaded={() => setListKey((current) => current + 1)}
          />
        </section>
      ) : null}

      <KnowledgeDocumentList key={listKey} />
    </>
  );
}
