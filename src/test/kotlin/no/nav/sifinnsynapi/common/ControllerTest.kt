package no.nav.sifinnsynapi.common

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.poc.PocController
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@AutoConfigureWireMock(port = 8000) // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
@WebMvcTest(PocController::class)
class ControllerTest {

/*    @InjectMocks
    lateinit var helloController: PocController

    @Mock
    lateinit var helloService: SøknadService

    @Test
    fun `Controller test`() {
        val list = listOf(
                SøknadDTO(
                        saksId = "Sak-123",
                        journalpostId = "12345",
                        søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                        status = SøknadsStatus.MOTTATT,
                        opprettet = LocalDateTime.now(),
                        søknad = mapOf(
                                "data" to "noe"
                        )
                )
        )

        doReturn(list).`when`(helloService).hentSøknad()
        val result = helloController.hentSøknad()
        assertNotNull(result)
        assertEquals(list, result)
    }*/

/*
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var service: SøknadService

    @Test
    @Throws(Exception::class)
    fun `Test controller`() {
        val list = listOf(
                SøknadDTO(
                        saksId = "Sak-123",
                        journalpostId = "12345",
                        søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                        status = SøknadsStatus.MOTTATT,
                        opprettet = LocalDateTime.now(),
                        søknad = mapOf(
                                "data" to "noe"
                        )
                )
        )

        `when`(service.hentSøknad()).thenReturn(list)
        mockMvc.perform(get("/soknad")).andDo(print()).andExpect(status().isOk)
                .andExpect(content().string(containsString("Hello, Mock")))
    }*/
    /*
    @TestConfiguration
    class ControllerTestConfig {
        @Bean
        fun service() = mockk<SøknadService>()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var service: SøknadService

    @Test
    fun `Hello returns correct message`() {
        val list = listOf(
                SøknadDTO(
                        saksId = "Sak-123",
                        journalpostId = "12345",
                        søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                        status = SøknadsStatus.MOTTATT,
                        opprettet = LocalDateTime.now(),
                        søknad = mapOf(
                                "data" to "noe"
                        )
                )
        )

        every { service.hentSøknad() } returns list

        val result = mockMvc.perform(get("/soknad"))
                .andExpect(status().isOk)
                .andDo(print())
                .andReturn()

        assertEquals(list, result.response.contentAsString)
        verify { service.hentSøknad() }
    }*/

}