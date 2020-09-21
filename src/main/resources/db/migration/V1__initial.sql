CREATE TABLE søknad
(
    id              UUID PRIMARY KEY NOT NULL,
    saks_id         VARCHAR(30),
    journalpost_id  VARCHAR(20),
    aktør_id        VARCHAR(10)      NOT NULL,
    fødselsnummer   VARCHAR(11)      NOT NULL,
    søknadstype     VARCHAR(50)      NOT NULL,
    status          VARCHAR(50)      NOT NULL,
    opprettet       timestamp        NOT NULL,
    endret          timestamp        NOT NULL,
    behandlingsdato timestamp,
    søknad          jsonb
)
