CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO
$$
    BEGIN
        -- Check if the mikrofrontend table is empty
        IF
            NOT EXISTS (SELECT 1 FROM mikrofrontend) THEN
            INSERT INTO mikrofrontend (id, fødselsnummer, mikrofrontend_id, status, opprettet)
            SELECT uuid_generate_v4(),
                   s.fødselsnummer,
                   'pleiepenger-innsyn',
                   'DISABLE',
                   CURRENT_TIMESTAMP
            FROM søknad s
            WHERE s.søknadstype = 'PP_SYKT_BARN'
            GROUP BY s.fødselsnummer, s.status, s.opprettet
            HAVING COUNT(DISTINCT s.fødselsnummer) = 1;
        END IF;
    END
$$;
