package no.nav.sifinnsynapi.config

import com.google.common.collect.Sets
import io.swagger.models.Scheme
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class SwaggerConfiguration : EnvironmentAware {
    private var env: Environment? = null

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .protocols(setOf(Scheme.HTTP.toValue(), Scheme.HTTPS.toValue()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("no.nav.sifinnsynapi"))
                .build()
    }

    private fun protocol(): Set<String> {
        return if (isLocal(env)) Sets.newHashSet(Scheme.HTTP.toValue()) else Sets.newHashSet(Scheme.HTTPS.toValue())
    }

    override fun setEnvironment(env: Environment) {
        this.env = env
    }
}

fun isLocal(env: Environment?): Boolean {
    return env == null || env.acceptsProfiles(Profiles.of(*arrayOf("local")))
}
