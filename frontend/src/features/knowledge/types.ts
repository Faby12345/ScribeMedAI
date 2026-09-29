export type KnowledgeDocument = {
  id: string;
  fileName: string;
  createdAt: string;
};

export type KnowledgeCitation = {
  documentId: string;
  documentTitle: string;
  pageFrom: number | null;
  pageTo: number | null;
  sectionTitle: string | null;
  excerpt: string;
};

export type KnowledgeQueryResponse = {
  answer: string;
  citations: KnowledgeCitation[];
};
