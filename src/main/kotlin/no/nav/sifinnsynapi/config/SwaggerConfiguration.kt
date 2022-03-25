package no.nav.sifinnsynapi.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment


@Configuration
@Profile("local", "dev-gcp")
class SwaggerConfiguration : EnvironmentAware {
    private var env: Environment? = null

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
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

    override fun setEnvironment(env: Environment) {
        this.env = env
    }
}
