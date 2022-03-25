package no.nav.sifinnsynapi.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local", "dev-gcp")
class SwaggerConfiguration {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .addServersItem(
                Server().url("https://sif-innsyn-api.dev.nav.no/").description("Swagger Server")
            )
            .info(
                Info()
                    .title("Sif Innsyn Api")
                    .description("API spesifikasjon for sif-innsyn-api")
                    .version("v1.0.0")
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("sif-innsyn-api github repository")
                    .url("https://github.com/navikt/sif-innsyn-api")
            )
    }
}
