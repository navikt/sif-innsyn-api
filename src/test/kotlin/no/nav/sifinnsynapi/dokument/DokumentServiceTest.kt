package no.nav.sifinnsynapi.dokument

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Datotype
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Journalstatus
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Variantformat
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.DokumentInfo
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentvariant
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Journalpost
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.RelevantDato
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Sak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
        assertThat(dokumentoversikt).hasSize(1)
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent 0 journalpost når brevkoder ikke matcher`() {
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf("ukjent brevkode"))
        assertThat(dokumentoversikt).isEmpty()
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent begge journalposter når brevkoder matcher`() {
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf("NAV 09-11.05", "NAVe 09-11.05"))
        assertThat(dokumentoversikt).hasSize(2)
    }

    @Test
    fun `gitt dokumentoversikt med 2 ulike journalposter, forvent at filtrering er case-insensitive`() {
        val dokumentoversikt = dokumentService.hentDokumentOversikt(listOf("nav 09-11.05 ", "NAVe 09-11.05"))
        assertThat(dokumentoversikt).hasSize(2)
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
                    relevanteDatoer = listOf(
                        RelevantDato(
                            dato = "",
                            datotype = Datotype.DATO_JOURNALFOERT
                        )
                    ),
                    sak = Sak(
                        fagsakId = "1DMELD6",
                        fagsaksystem = "K9"
                    ),
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533440578",
                            tittel = "Søknad om pleiepenger",
                            brevkode = "NAV 09-11.05",
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", true, listOf())
                            )
                        )
                    )
                ),
                Journalpost(
                    journalpostId = "545635015",
                    tittel = "Søknad om pleiepenger – sykt barn - NAVe 09-11.05",
                    journalstatus = Journalstatus.MOTTATT,
                    relevanteDatoer = listOf(
                        RelevantDato(
                            dato = "",
                            datotype = Datotype.DATO_OPPRETTET
                        )
                    ),
                    sak = null,
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533439503",
                            tittel = "Ettersendelse pleiepenger sykt barn",
                            brevkode = "NAVe 09-11.05",
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", true, listOf())
                            )
                        )
                    )
                ),
                Journalpost(
                    journalpostId = "545635017",
                    tittel = "skannet dokument",
                    journalstatus = Journalstatus.MOTTATT,
                    relevanteDatoer = listOf(
                        RelevantDato(
                            dato = "",
                            datotype = Datotype.DATO_OPPRETTET
                        )
                    ),
                    sak = null,
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533439507",
                            tittel = "skannet dokument",
                            brevkode = "NAVe 09-11.05",
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", false, listOf())
                            )
                        )
                    )
                ),
                Journalpost(
                    journalpostId = "545635088",
                    tittel = "uten brevkode",
                    journalstatus = Journalstatus.MOTTATT,
                    relevanteDatoer = listOf(
                        RelevantDato(
                            dato = "",
                            datotype = Datotype.DATO_OPPRETTET
                        )
                    ),
                    sak = null,
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533439507",
                            tittel = "uten brevkode",
                            brevkode = null,
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", false, listOf())
                            )
                        )
                    )
                )
            )
        )
    }
}
