CREATE TABLE mikrofrontend
(
    id              UUID PRIMARY KEY NOT NULL,
    fødselsnummer   VARCHAR(10)      NOT NULL,
    mikrofrntend_id VARCHAR(100)     NOT NULL,
    status          VARCHAR(50)      NOT NULL,
    opprettet       timestamp        NOT NULL,
    endret          timestamp,
    behandlingsdato timestamp
)
