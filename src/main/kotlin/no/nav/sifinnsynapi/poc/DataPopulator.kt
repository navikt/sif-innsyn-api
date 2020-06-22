package no.nav.sifinnsynapi.poc

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional

@Configuration
class DataPopulator(
        private val repo: SøknadRepository
) {
    companion object {
        val log = LoggerFactory.getLogger(CommandLineRunner::class.java)
    }

    @Bean
    //@Profile("local")
    fun commandlineRunner(): CommandLineRunner {
        return CommandLineRunner {
            listOf(
                    SøknadDAO(
                            saksId = "sak-123456789",
                            aktørId = AktørId.valueOf("123456"),
                            fødselsnummer = Fødselsnummer.valueOf("25118921464"),
                            journalpostId = "12345678",
                            søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                            status = SøknadsStatus.MOTTATT,
                            søknad =
                            //language=json
                            """
                                {
                                    "søknadId": "05ce3630-76eb-40f4-87a3-a5d55af58e40",
                                    "språk": "nb",
                                    "mottatt": "2018-01-02T03:04:05.000000006Z",
                                    "søker": {
                                      "aktørId": "123456",
                                      "fødselsnummer": "02119970078",
                                      "fødselsdato": "1999-11-02",
                                      "etternavn": "Nordmann",
                                      "mellomnavn": null,
                                      "fornavn": "Ola"
                                    },
                                    "bosteder": [
                                      {
                                        "fraOgMed": "2020-01-01",
                                        "tilOgMed": "2020-01-06",
                                        "landkode": "SWE",
                                        "landnavn": "Sverige",
                                        "erEØSLand": true
                                      },
                                      {
                                        "fraOgMed": "2020-01-11",
                                        "tilOgMed": "2020-01-11",
                                        "landkode": "NOR",
                                        "landnavn": "Norge",
                                        "erEØSLand": true
                                      }
                                    ],
                                    "opphold": [
                                      {
                                        "fraOgMed": "2020-01-16",
                                        "tilOgMed": "2020-01-21",
                                        "landkode": "Eng",
                                        "landnavn": "England",
                                        "erEØSLand": true
                                      },
                                      {
                                        "fraOgMed": "2019-12-22",
                                        "tilOgMed": "2019-12-27",
                                        "landkode": "CRO",
                                        "landnavn": "Kroatia",
                                        "erEØSLand": true
                                      }
                                    ],
                                    "spørsmål": [
                                      {
                                        "spørsmål": "Har du vært hjemme?",
                                        "svar": false
                                      },
                                      {
                                        "spørsmål": "Skal du være hjemme?",
                                        "svar": true
                                      }
                                    ],
                                    "utbetalingsperioder": [
                                      {
                                        "fraOgMed": "2020-01-01",
                                        "tilOgMed": "2020-01-11",
                                        "lengde": "PT5H30M"
                                      },
                                      {
                                        "fraOgMed": "2020-01-21",
                                        "tilOgMed": "2020-01-21",
                                        "lengde": "PT5H30M"
                                      },
                                      {
                                        "fraOgMed": "2020-01-31",
                                        "tilOgMed": "2020-02-05",
                                        "lengde": "PT5H30M"
                                      }
                                    ],
                                    "andreUtbetalinger": [
                                      "dagpenger",
                                      "sykepenger"
                                    ],
                                    "vedlegg": [
                                      "http://localhost:8080/vedlegg/1",
                                      "http://localhost:8080/vedlegg/2",
                                      "http://localhost:8080/vedlegg/3"
                                    ],
                                    "frilans": null,
                                    "selvstendigVirksomheter": null,
                                    "erArbeidstakerOgså": true,
                                    "fosterbarn": [
                                      {
                                        "fødselsnummer": "02119970078"
                                      }
                                    ],
                                    "hjemmePgaSmittevernhensyn": true,
                                    "bekreftelser": {
                                      "harBekreftetOpplysninger": true,
                                      "harForståttRettigheterOgPlikter": true
                                    }
                                  }
                            """.trimIndent()
                    ),
                    SøknadDAO(
                            saksId = "sak-987654321",
                            aktørId = AktørId.valueOf("654321"),
                            fødselsnummer = Fødselsnummer.valueOf("30058922699"),
                            journalpostId = "87654321",
                            søknadstype = Søknadstype.OMP_UTBETALING_ARBEIDSTAKER,
                            status = SøknadsStatus.MOTTATT,
                            søknad =
                            //language=json
                            """
                                {
                                  "søknadId": "cc644d91-4eff-4679-a7d3-fc57a0c6a1c2",
                                  "mottatt": "2020-06-11T10:09:12.129299+02:00",
                                  "språk": "nb",
                                  "søker": {
                                    "fødselsnummer": "02119970078",
                                    "fornavn": "Ola",
                                    "mellomnavn": null,
                                    "etternavn": "Nordmann",
                                    "fødselsdato": "1999-11-02",
                                    "aktørId": "123456"
                                  },
                                  "bosteder": [
                                    {
                                      "fraOgMed": "2019-12-12",
                                      "tilOgMed": "2019-12-22",
                                      "landkode": "GB",
                                      "landnavn": "Great Britain",
                                      "erEØSLand": true
                                    },
                                    {
                                      "fraOgMed": "2019-12-12",
                                      "tilOgMed": "2019-12-22",
                                      "landkode": "US",
                                      "landnavn": "USA",
                                      "erEØSLand": false
                                    }
                                  ],
                                  "opphold": [
                                    {
                                      "fraOgMed": "2019-12-12",
                                      "tilOgMed": "2019-12-22",
                                      "landkode": "GB",
                                      "landnavn": "Great Britain",
                                      "erEØSLand": true
                                    }
                                  ],
                                  "arbeidsgivere": [
                                    {
                                      "navn": "Arbeidsgiver 1",
                                      "organisasjonsnummer": "917755736",
                                      "harHattFraværHosArbeidsgiver": true,
                                      "arbeidsgiverHarUtbetaltLønn": false,
                                      "ansettelseslengde": {
                                        "merEnn4Uker": true,
                                        "begrunnelse": null,
                                        "ingenAvSituasjoneneForklaring": null
                                      },
                                      "perioder": [
                                        {
                                          "fraOgMed": "2020-01-01",
                                          "tilOgMed": "2020-01-11",
                                          "lengde": null
                                        }
                                      ]
                                    },
                                    {
                                      "navn": "Arbeidsgiver 2",
                                      "organisasjonsnummer": "917755736",
                                      "harHattFraværHosArbeidsgiver": true,
                                      "arbeidsgiverHarUtbetaltLønn": false,
                                      "ansettelseslengde": {
                                        "merEnn4Uker": false,
                                        "begrunnelse": "ANNET_ARBEIDSFORHOLD",
                                        "ingenAvSituasjoneneForklaring": null
                                      },
                                      "perioder": [
                                        {
                                          "fraOgMed": "2020-01-21",
                                          "tilOgMed": "2020-01-21",
                                          "lengde": "PT5H30M"
                                        }
                                      ]
                                    },
                                    {
                                      "navn": "Arbeidsgiver 3",
                                      "organisasjonsnummer": "917755736",
                                      "harHattFraværHosArbeidsgiver": true,
                                      "arbeidsgiverHarUtbetaltLønn": false,
                                      "ansettelseslengde": {
                                        "merEnn4Uker": false,
                                        "begrunnelse": "MILITÆRTJENESTE",
                                        "ingenAvSituasjoneneForklaring": null
                                      },
                                      "perioder": [
                                        {
                                          "fraOgMed": "2020-01-31",
                                          "tilOgMed": "2020-02-05",
                                          "lengde": null
                                        }
                                      ]
                                    },
                                    {
                                      "navn": "Arbeidsgiver 4",
                                      "organisasjonsnummer": "917755736",
                                      "harHattFraværHosArbeidsgiver": true,
                                      "arbeidsgiverHarUtbetaltLønn": false,
                                      "ansettelseslengde": {
                                        "merEnn4Uker": false,
                                        "begrunnelse": "INGEN_AV_SITUASJONENE",
                                        "ingenAvSituasjoneneForklaring": "Forklarer hvorfor ingen av situasjonene passer."
                                      },
                                      "perioder": [
                                        {
                                          "fraOgMed": "2020-01-31",
                                          "tilOgMed": "2020-02-05",
                                          "lengde": null
                                        }
                                      ]
                                    },
                                    {
                                      "navn": null,
                                      "organisasjonsnummer": "917755736",
                                      "harHattFraværHosArbeidsgiver": true,
                                      "arbeidsgiverHarUtbetaltLønn": false,
                                      "ansettelseslengde": {
                                        "merEnn4Uker": false,
                                        "begrunnelse": "ANDRE_YTELSER",
                                        "ingenAvSituasjoneneForklaring": null
                                      },
                                      "perioder": [
                                        {
                                          "fraOgMed": "2020-02-01",
                                          "tilOgMed": "2020-02-06",
                                          "lengde": null
                                        }
                                      ]
                                    },
                                    {
                                      "navn": "Ikke registrert arbeidsgiver",
                                      "organisasjonsnummer": null,
                                      "harHattFraværHosArbeidsgiver": true,
                                      "arbeidsgiverHarUtbetaltLønn": false,
                                      "ansettelseslengde": {
                                        "merEnn4Uker": false,
                                        "begrunnelse": "ANDRE_YTELSER",
                                        "ingenAvSituasjoneneForklaring": null
                                      },
                                      "perioder": [
                                        {
                                          "fraOgMed": "2020-02-01",
                                          "tilOgMed": "2020-02-06",
                                          "lengde": null
                                        }
                                      ]
                                    }
                                  ],
                                  "bekreftelser": {
                                    "harBekreftetOpplysninger": true,
                                    "harForståttRettigheterOgPlikter": true
                                  },
                                  "fosterbarn": [
                                    {
                                      "fødselsnummer": "02119970078"
                                    }
                                  ],
                                  "titler": [
                                    "vedlegg1"
                                  ],
                                  "vedleggUrls": [
                                    "http://localhost:8080/vedlegg/1",
                                    "http://localhost:8080/vedlegg/2",
                                    "http://localhost:8080/vedlegg/3"
                                  ],
                                  "hjemmePgaSmittevernhensyn": true
                                }
                            """.trimIndent()
                    ),
                    SøknadDAO(
                            saksId = "sak-5674839201",
                            aktørId = AktørId.valueOf("978674"),
                            fødselsnummer = Fødselsnummer.valueOf("27118921009"),
                            journalpostId = "68493021",
                            søknadstype = Søknadstype.PP_SYKT_BARN,
                            status = SøknadsStatus.MOTTATT,
                            søknad =
                            //language=json
                            """
                                {
                                    "språk": null,
                                    "søknadId": "cc644d91-4eff-4679-a7d3-fc57a0c6a1c2",
                                    "mottatt": "2018-01-02T03:04:05.000000006Z",
                                    "fraOgMed": "2018-01-01",
                                    "tilOgMed": "2018-02-02",
                                    "vedleggUrls": [
                                        "http://localhost:8080/1234", "http://localhost:8080/12345"],
                                    "søker": {
                                        "aktørId": "123456",
                                        "fødselsnummer": "1212",
                                        "fornavn": "Ola",
                                        "mellomnavn": "Mellomnavn",
                                        "etternavn": "Nordmann"
                                    },
                                    "barn": {
                                        "fødselsnummer": "2323",
                                        "navn": "Kari",
                                        "fødselsdato": null,
                                        "aktørId": null
                                    },
                                    "relasjonTilBarnet": "Mor",
                                    "arbeidsgivere": {
                                        "organisasjoner": [{
                                            "organisasjonsnummer": "1212",
                                            "navn": "Nei",
                                            "skalJobbe": "nei",
                                            "jobberNormaltTimer": 0.0,
                                            "skalJobbeProsent": 0.0,
                                            "vetIkkeEkstrainfo": null
                                        },{
                                            "organisasjonsnummer": "54321",
                                            "navn": "Navn",
                                            "skalJobbe": "redusert",
                                            "skalJobbeProsent": 22.512,
                                            "vetIkkeEkstrainfo": null,
                                            "jobberNormaltTimer": 0.0
                                        }]
                                    },
                                    "medlemskap": {
                                        "harBoddIUtlandetSiste12Mnd": true,
                                        "utenlandsoppholdNeste12Mnd": [],
                                        "skalBoIUtlandetNeste12Mnd": true,
                                        "utenlandsoppholdSiste12Mnd": []
                                    },
                                    "harMedsøker": true,
                                    "samtidigHjemme": null,
                                    "bekrefterPeriodeOver8Uker": true,
                                    "harBekreftetOpplysninger" : true,
                                    "harForståttRettigheterOgPlikter": true,
                                    "tilsynsordning": {
                                        "svar": "ja",
                                        "ja": {
                                            "mandag": "PT5H",
                                            "tirsdag": "PT4H",
                                            "onsdag": "PT3H45M",
                                            "torsdag": "PT2H",
                                            "fredag": "PT1H30M",
                                            "tilleggsinformasjon": "Litt tilleggsinformasjon."
                                        },
                                        "vetIkke": null
                                    },
                                    "beredskap": {
                                        "beredskap": true,
                                        "tilleggsinformasjon": "I Beredskap"
                                    },
                                    "nattevåk": {
                                        "harNattevåk": true,
                                        "tilleggsinformasjon": "Har Nattevåk"
                                    },
                                     "utenlandsoppholdIPerioden": {
                                        "skalOppholdeSegIUtlandetIPerioden": false,
                                        "opphold": []
                                    },
                                  "ferieuttakIPerioden": {
                                    "skalTaUtFerieIPerioden": false,
                                    "ferieuttak": [
                                    ]
                                  },
                                    "frilans": {
                                      "startdato": "2018-02-01",
                                      "jobberFortsattSomFrilans": true
                                    },
                                    "selvstendigVirksomheter" : [],
                                  "skalBekrefteOmsorg": true,
                                  "skalPassePaBarnetIHelePerioden": true,
                                  "beskrivelseOmsorgsrollen": "En kort beskrivelse"
                                }
                            """.trimIndent()
                    ),
                    SøknadDAO(
                            saksId = "sak-5674839201",
                            aktørId = AktørId.valueOf("1157852636281"),
                            fødselsnummer = Fødselsnummer.valueOf("27118921009"),
                            journalpostId = "68493021",
                            søknadstype = Søknadstype.PP_SYKT_BARN,
                            status = SøknadsStatus.MOTTATT,
                            søknad =
                            //language=json
                            """
                                {
                                    "språk": null,
                                    "søknadId": "cc644d91-4eff-4679-a7d3-fc57a0c6a1c2",
                                    "mottatt": "2018-01-02T03:04:05.000000006Z",
                                    "fraOgMed": "2018-01-01",
                                    "tilOgMed": "2018-02-02",
                                    "vedleggUrls": [
                                        "http://localhost:8080/1234", "http://localhost:8080/12345"],
                                    "søker": {
                                        "aktørId": "1157852636281",
                                        "fødselsnummer": "27118921009",
                                        "fornavn": "Robust",
                                        "mellomnavn": "",
                                        "etternavn": "STAFFELI"
                                    },
                                    "barn": {
                                        "fødselsnummer": "2323",
                                        "navn": "Kari",
                                        "fødselsdato": null,
                                        "aktørId": null
                                    },
                                    "relasjonTilBarnet": "Mor",
                                    "arbeidsgivere": {
                                        "organisasjoner": [{
                                            "organisasjonsnummer": "1212",
                                            "navn": "Nei",
                                            "skalJobbe": "nei",
                                            "jobberNormaltTimer": 0.0,
                                            "skalJobbeProsent": 0.0,
                                            "vetIkkeEkstrainfo": null
                                        },{
                                            "organisasjonsnummer": "54321",
                                            "navn": "Navn",
                                            "skalJobbe": "redusert",
                                            "skalJobbeProsent": 22.512,
                                            "vetIkkeEkstrainfo": null,
                                            "jobberNormaltTimer": 0.0
                                        }]
                                    },
                                    "medlemskap": {
                                        "harBoddIUtlandetSiste12Mnd": true,
                                        "utenlandsoppholdNeste12Mnd": [],
                                        "skalBoIUtlandetNeste12Mnd": true,
                                        "utenlandsoppholdSiste12Mnd": []
                                    },
                                    "harMedsøker": true,
                                    "samtidigHjemme": null,
                                    "bekrefterPeriodeOver8Uker": true,
                                    "harBekreftetOpplysninger" : true,
                                    "harForståttRettigheterOgPlikter": true,
                                    "tilsynsordning": {
                                        "svar": "ja",
                                        "ja": {
                                            "mandag": "PT5H",
                                            "tirsdag": "PT4H",
                                            "onsdag": "PT3H45M",
                                            "torsdag": "PT2H",
                                            "fredag": "PT1H30M",
                                            "tilleggsinformasjon": "Litt tilleggsinformasjon."
                                        },
                                        "vetIkke": null
                                    },
                                    "beredskap": {
                                        "beredskap": true,
                                        "tilleggsinformasjon": "I Beredskap"
                                    },
                                    "nattevåk": {
                                        "harNattevåk": true,
                                        "tilleggsinformasjon": "Har Nattevåk"
                                    },
                                     "utenlandsoppholdIPerioden": {
                                        "skalOppholdeSegIUtlandetIPerioden": false,
                                        "opphold": []
                                    },
                                  "ferieuttakIPerioden": {
                                    "skalTaUtFerieIPerioden": false,
                                    "ferieuttak": [
                                    ]
                                  },
                                    "frilans": {
                                      "startdato": "2018-02-01",
                                      "jobberFortsattSomFrilans": true
                                    },
                                    "selvstendigVirksomheter" : [],
                                  "skalBekrefteOmsorg": true,
                                  "skalPassePaBarnetIHelePerioden": true,
                                  "beskrivelseOmsorgsrollen": "En kort beskrivelse"
                                }
                            """.trimIndent()
                    )
            ).map {
                log.info("Lagrer søknad: {}", it)
                repo.save(it)
            }.forEach {
                log.info("Hentet Søknad: {}", it)
            }
        }
    }
}
