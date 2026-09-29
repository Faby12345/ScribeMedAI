CREATE TABLE knowledge_document (
                                    id UUID PRIMARY KEY,
                                    title VARCHAR(500) NOT NULL,
                                    source_institution VARCHAR(255) NOT NULL,
                                    source_url TEXT,
                                    published_at DATE,
                                    version VARCHAR(100),
                                    status VARCHAR(30) NOT NULL,
                                    original_filename VARCHAR(500) NOT NULL,
                                    checksum VARCHAR(64) NOT NULL UNIQUE,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_chunk (
                                 id UUID PRIMARY KEY,
                                 document_id UUID NOT NULL
                                     REFERENCES knowledge_document(id)
                                         ON DELETE CASCADE,

                                 chunk_index INTEGER NOT NULL,
                                 content TEXT NOT NULL,

                                 page_from INTEGER,
                                 page_to INTEGER,
                                 section_title VARCHAR(500),

                                 embedding VECTOR(384) NOT NULL,

                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                 UNIQUE (document_id, chunk_index)
);