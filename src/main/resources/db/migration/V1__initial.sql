CREATE TABLE søknad(
    id UUID PRIMARY KEY,
    språk VARCHAR(100) NOT NULL,
    harForståttRettigheterOgPlikter boolean  NOT NULL,
    harBekreftetOpplysninger boolean  NOT NULL,
    beskrivelse VARCHAR (1000)  NOT NULL,
    søknadstype VARCHAR (100) NOT NULL
)
