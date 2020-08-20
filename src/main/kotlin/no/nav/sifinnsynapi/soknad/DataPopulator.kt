package no.nav.sifinnsynapi.soknad

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.dokument.DokumentDAO
import no.nav.sifinnsynapi.dokument.DokumentRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import java.time.LocalDateTime

@Configuration
@Profile("!test")
class DataPopulator(
        private val repo: SøknadRepository,
        private val dokRepo: DokumentRepository
) {
    companion object {
        val logger = LoggerFactory.getLogger(CommandLineRunner::class.java)
    }

    @Bean
    fun commandlineRunner(): CommandLineRunner {
        return CommandLineRunner {
            listOf(
                    SøknadDAO(
                            saksId = "sak-5674839201",
                            aktørId = AktørId.valueOf("1157852636281"),
                            fødselsnummer = Fødselsnummer.valueOf("27118921009"),
                            journalpostId = "68493021",
                            søknadstype = Søknadstype.PP_SYKT_BARN,
                            status = SøknadsStatus.MOTTATT,
                            opprettet = LocalDateTime.parse("2018-01-02T03:04:05"),
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
                logger.info("Lagrer søknad: {}", it)
                val søknadDAO = repo.save(it)
                dokRepo.save(DokumentDAO(
                        innhold = ClassPathResource("/static/eksempel-søknad.pdf").inputStream.readAllBytes(),
                        søknadId = søknadDAO.id
                ))
                søknadDAO
            }.forEach {
                logger.info("Hentet Søknad: {}", it)
            }
        }
    }
}
