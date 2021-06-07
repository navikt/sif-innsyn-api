package no.nav.sifinnsynapi.soknad

import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import assertk.assertions.size
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Topics
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

/*@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    count = 3,
    topics = [Topics.K9_ETTERSENDING, Topics.K9_DITTNAV_VARSEL_BESKJED],
    bootstrapServersProperty = "kafka.onprem.servers" // Setter bootstrap-servers for consumer og producer.
)*/
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
internal class SøknadServiceTest {

    @Autowired
    private lateinit var søknadRepository: SøknadRepository

    @Autowired
    private lateinit var søknadService: SøknadService

    @Test
    fun hentArbeidsgiverMeldingFil() {
        val søknadId = UUID.randomUUID()
        val organisasjonsnummer = "917755645"
        søknadRepository.save(
            SøknadDAO(
                id = søknadId,
                saksId = "abc123",
                søknadstype = Søknadstype.PP_SYKT_BARN,
                status = SøknadsStatus.MOTTATT,
                journalpostId = "123456789",
                opprettet = ZonedDateTime.parse("2020-08-04T10:30:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                fødselsnummer = Fødselsnummer(""),
                aktørId = AktørId(""),
                søknad =
                //language=json
                """
                            {
                                "fraOgMed": "2021-01-01",
                                "tilOgMed": "2021-01-01",
                                "søker": {
                                  "mellomnavn": "Mellomnavn",
                                  "etternavn": "Nordmann",
                                  "aktørId": "123456",
                                  "fødselsnummer": "02119970078",
                                  "fornavn": "Ola"
                                },
                                "arbeidsgivere": {
                                  "organisasjoner": [
                                    {
                                      "navn": "Stolt Hane",
                                      "organisasjonsnummer": "917755736"
                                    },
                                    {
                                      "navn": "Snill Torpedo",
                                      "organisasjonsnummer": "$organisasjonsnummer"
                                    },
                                    {
                                      "navn": "Something Fishy",
                                      "organisasjonsnummer": "917755645"
                                    }
                                  ]
                                }
                            }
                        """.trimIndent()
            )
        )
        val bytes = søknadService.hentArbeidsgiverMeldingFil(søknadId, organisasjonsnummer)
        assertNotNull(bytes)
        assertThat(bytes).isNotEmpty()
        assertThat(bytes).size().isGreaterThan(1000)
    }
}
