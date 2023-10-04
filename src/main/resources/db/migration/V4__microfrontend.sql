CREATE TABLE mikrofrontend
(
    id               UUID PRIMARY KEY NOT NULL,
    fødselsnummer    VARCHAR(11)      NOT NULL,
    mikrofrontend_id VARCHAR(100)     NOT NULL,
    status           VARCHAR(50)      NOT NULL,
    opprettet        timestamp        NOT NULL,
    endret           timestamp,
    behandlingsdato  timestamp
)
