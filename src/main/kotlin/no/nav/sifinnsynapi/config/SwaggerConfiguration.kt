package no.nav.sifinnsynapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.*
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class SwaggerConfiguration(
    @Value("\${springdoc.oAuthFlow.authorizationUrl}") val authorizationUrl: String,
    @Value("\${springdoc.oAuthFlow.tokenUrl}") val tokenUrl: String,
    @Value("\${springdoc.oAuthFlow.apiScope}") val apiScope: String
) : EnvironmentAware {

    private var env: Environment? = null


    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .addServersItem(
                Server().url("https://sif-innsyn-api.intern.dev.nav.no/").description("Swagger Server")
            )
            .info(
                Info()
                    .title("Sif Innsyn Api")
                    .description("API spesifikasjon for sif-innsyn-api")
                    .version("v1.0.0")
            )
            .externalDocs(
                ExternalDocumentation()
                    .description("Sif Innsyn Api GitHub repository")
                    .url("https://github.com/navikt/sif-innsyn-api")
            ).components(
                Components()
                    .addSecuritySchemes("oauth2", azureLogin())
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList("oauth2", listOf("read", "write"))
                    .addList("Authorization")
            )
    }


    private fun azureLogin(): SecurityScheme {
        return SecurityScheme()
            .name("oauth2")
            .type(SecurityScheme.Type.OAUTH2)
            .scheme("oauth2")
            .`in`(SecurityScheme.In.HEADER)
            .flows(
                OAuthFlows()
                    .authorizationCode(
                        OAuthFlow().authorizationUrl(authorizationUrl)
                            .tokenUrl(tokenUrl)
                            .scopes(Scopes().addString(apiScope, "read,write"))
                    )
            )
    }

    override fun setEnvironment(environment: Environment) {
        this.env = environment;
    }


}
