ALTER TABLE mikrofrontend
    ALTER COLUMN fødselsnummer TYPE VARCHAR(11);

ALTER TABLE mikrofrontend
    RENAME COLUMN mikrofrntend_id TO mikrofrontend_id;
