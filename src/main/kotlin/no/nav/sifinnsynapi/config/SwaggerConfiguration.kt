package no.nav.sifinnsynapi.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment


@Configuration
@Profile("local", "dev-gcp")
@SecurityScheme(
    name = "cookieAuth",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    scheme = "bearer",
    bearerFormat = "JWT"
)
class SwaggerConfiguration : EnvironmentAware {
    private var env: Environment? = null

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .security(listOf(
                SecurityRequirement().addList("")
            ))
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
