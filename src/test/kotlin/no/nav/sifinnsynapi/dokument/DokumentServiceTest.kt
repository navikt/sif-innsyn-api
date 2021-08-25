package no.nav.sifinnsynapi.dokument

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.size
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Journalstatus
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Variantformat
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.*
import no.nav.sifinnsynapi.util.CallIdGenerator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class DokumentServiceTest {

    @MockkBean
    lateinit var safSelvbetjeningService: SafSelvbetjeningService

    @Autowired
    lateinit var dokumentService: DokumentService

    @BeforeEach
    internal fun setUp() {
        stubDokumentOversikt()
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent 1 journalpost ved filtrering av en av dem`() {
        val forventetBrevkode = "NAVe 09-11.05"
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf(forventetBrevkode))
        assertThat(dokumentoversikt.journalposter).hasSize(1)
        assertThat(dokumentoversikt.journalposter.first())
            .transform { it.dokumenter!!.first()!!.brevkode }.isEqualTo(forventetBrevkode)
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent 0 journalpost når brevkoder ikke matcher`() {
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf("ukjent brevkode"))
        assertThat(dokumentoversikt.journalposter).isEmpty()
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent begge journalposter når brevkoder matcher`() {
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf("NAV 09-11.05", "NAVe 09-11.05"))
        assertThat(dokumentoversikt.journalposter).hasSize(2)
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent at filtrering er case-insensitive`() {
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf("nav 09-11.05 ", "NAVe 09-11.05"))
        assertThat(dokumentoversikt.journalposter).hasSize(2)
    }

    private fun stubDokumentOversikt() {
        coEvery {
            safSelvbetjeningService.hentDokumentoversikt()
        } returns Dokumentoversikt(
            journalposter = listOf(
                Journalpost(
                    journalpostId = "510536545",
                    tittel = "Søknad om pleiepenger – sykt barn - NAV 09-11.05",
                    journalstatus = Journalstatus.JOURNALFOERT,
                    sak = Sak(
                        fagsakId = "1DMELD6",
                        fagsaksystem = "K9"
                    ),
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533440578",
                            tittel = "Søknad om pleiepenger",
                            brevkode = "NAV 09-11.05",
                            dokumentvarianter = listOf(Dokumentvariant(Variantformat.ARKIV, "PDF", true))
                        )
                    )
                ),
                Journalpost(
                    journalpostId = "545635015",
                    tittel = "Søknad om pleiepenger – sykt barn - NAVe 09-11.05",
                    journalstatus = Journalstatus.MOTTATT,
                    sak = null,
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533439503",
                            tittel = "Ettersendelse pleiepenger sykt barn",
                            brevkode = "NAVe 09-11.05",
                            dokumentvarianter = listOf(Dokumentvariant(Variantformat.ARKIV, "PDF", true))
                        )
                    )
                )
            )
        )
    }
}
