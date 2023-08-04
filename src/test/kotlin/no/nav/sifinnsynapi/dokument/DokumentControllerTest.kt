package no.nav.sifinnsynapi.dokument

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.Routes
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.safselvbetjening.ArkivertDokument
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Datotype
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.*
import no.nav.sifinnsynapi.util.CallIdGenerator
import no.nav.sifinnsynapi.utils.hentToken
import org.junit.Assert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.net.URI
import java.net.URL
import jakarta.servlet.http.Cookie

@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@Import(CallIdGenerator::class, SecurityConfiguration::class)
@WebMvcTest(controllers = [DokumentController::class])
@ActiveProfiles("test")
internal class DokumentControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean(relaxed = true)
    lateinit var dokumentService: DokumentService

    @BeforeAll
    internal fun setUp() {
        Assert.assertNotNull(mockOAuth2Server)
    }

    @Test
    fun `Gitt 200 respons, forvent korrekt format på dokumentoversikt`() {
        every {
            dokumentService.hentDokumentOversikt(any())
        } returns
                listOf(
                    DokumentDTO(
                        journalpostId = "510536545",
                        dokumentInfoId = "533440578",
                        sakId = "1DMELD6",
                        tittel = "Søknad om pleiepenger",
                        filtype = "pdf",
                        harTilgang = true,
                        url = URL("http://localhost:8080/dokument/510536545/533440578/ARKIV"),
                        relevanteDatoer = listOf(
                            RelevantDato(
                                dato = "2021-08-25T09:58:55",
                                datotype = Datotype.DATO_JOURNALFOERT
                            )
                        )
                    )
                )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI("${Routes.DOKUMENT}/oversikt"))
                .queryParam("brevkoder", "NAV 09-11.05")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${mockOAuth2Server.hentToken().serialize()}")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    //language=json
                    """
                    [
                      {
                        "journalpostId": "510536545",
                        "sakId": "1DMELD6",
                        "dokumentInfoId": "533440578",
                        "tittel": "Søknad om pleiepenger",
                        "filtype": "pdf",
                        "url": "http://localhost:8080/dokument/510536545/533440578/ARKIV",
                        "harTilgang": true,
                        "relevanteDatoer": [
                          {
                            "dato": "2021-08-25T09:58:55",
                            "datotype": "DATO_JOURNALFOERT"
                          }
                        ]
                      }
                    ]
                    """.trimIndent(), true
                )
            )
    }

    @Test
    fun `hent dokument`() {
        val forventetFilnavn = "Screenshot 2021-04-23 at 12.59.57.pdf"
        every {
            dokumentService.hentDokument(any(), any(), any())
        } returns ArkivertDokument(
            body = "some byteArray".toByteArray(),
            contentType = "application/pdf",
            contentDisposition = ContentDisposition.parse("inline; filename=533439502_ARKIV.pdf")
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("${Routes.DOKUMENT}/{journalpostId}/{dokumentinfoId}/{variant}", "123456789", "987654321", "ARKIV")
                .queryParam("dokumentTittel", "Screenshot 2021-04-23 at 12.59.57.png")
                .accept(MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${mockOAuth2Server.hentToken().serialize()}")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.header().exists(HttpHeaders.CONTENT_DISPOSITION))
            .andExpect(
                MockMvcResultMatchers.header()
                    .string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=$forventetFilnavn")
            )
    }

    @Test
    fun `hent dokument med ugyldige url parametere`() {
        every {
            dokumentService.hentDokument(any(), any(), any())
        } returns ArkivertDokument(
            body = "some byteArray".toByteArray(),
            contentType = "application/pdf",
            contentDisposition = ContentDisposition.parse("inline; filename=533439502_ARKIV.pdf")
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("${Routes.DOKUMENT}/{journalpostId}/{dokumentinfoId}/{variant}", "123", "321", "ANNET")
                .queryParam("dokumentTittel", "Screenshot 2021-04-23 at 12.59.57.png")
                .accept(MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${mockOAuth2Server.hentToken().serialize()}")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(
                content().json(
                    //language=json
                    """
                    {
                      "type": "/problem-details/ugyldig-forespørsel",
                      "instance": "http://localhost/dokument/123/321/ANNET",
                      "title": "Ugyldig forespørsel",
                      "status": 400,
                      "detail": "Forespørselen inneholder valideringsfeil",
                      "violations": [
                        {
                          "property": "hentDokument.dokumentInfoId",
                          "message":  "[321] matcher ikke tillatt pattern [\\d{9}]",
                          "invalidValue": "321"
                        },
                        {
                          "property": "hentDokument.journalpostId",
                          "message": "[123] matcher ikke tillatt pattern [\\d{9}]",
                          "invalidValue": "123"
                        },
                        {
                          "property": "hentDokument.variantFormat",
                          "message":  "[ANNET] matcher ikke tillatt pattern [ARKIV]",
                          "invalidValue": "ANNET"
                        }
                      ]
                    }
                """.trimIndent(), true
                )
            )
    }

    @Test
    fun `gitt request uten token, forevnt 401`() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI("${Routes.DOKUMENT}/oversikt"))
                .queryParam("brevkoder", "NAV 09-11.05")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(
                content().json(
                    //language=json
                    """
                    {
                      "type": "/problem-details/uautentisert-forespørsel",
                      "instance": "http://localhost/dokument/oversikt",
                      "title": "Ikke autentisert",
                      "status": 401
                    }
                """.trimIndent(), true
                )
            )
    }
}
