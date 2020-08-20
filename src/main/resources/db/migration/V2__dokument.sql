CREATE TABLE dokument
(
    id        UUID PRIMARY KEY NOT NULL,
    søknad_id UUID             NOT NULL,
    innhold   OID              NOT NULL
)
