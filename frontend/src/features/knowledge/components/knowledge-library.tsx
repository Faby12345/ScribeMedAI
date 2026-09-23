"use client";

import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { KnowledgeDocumentList } from "@/features/knowledge/components/knowledge-document-list";
import { KnowledgeDocumentUpload } from "@/features/knowledge/components/knowledge-document-upload";

export function KnowledgeLibrary() {
  const [isUploadVisible, setIsUploadVisible] = useState(false);
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
      <div className="mb-6 flex justify-end">
        <Button
          type="button"
          variant={isUploadVisible ? "outline" : "primary"}
          aria-expanded={isUploadVisible}
          aria-controls="knowledge-upload-section"
          onClick={() => setIsUploadVisible((isVisible) => !isVisible)}
        >
          {isUploadVisible ? "Închide formularul" : "Adaugă document"}
        </Button>
      </div>

      <KnowledgeDocumentList />

      {isUploadVisible ? (
        <section
          ref={uploadSectionRef}
          id="knowledge-upload-section"
          className="mt-10 scroll-mt-6 border-t border-border pt-8 outline-none"
          aria-label="Adaugă document"
          tabIndex={-1}
        >
          <KnowledgeDocumentUpload />
        </section>
      ) : null}
    </>
  );
}
