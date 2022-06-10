import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.expediagroup.graphql") version "5.5.0"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val confluentVersion by extra("5.5.0")
val logstashLogbackEncoderVersion by extra("7.0.1")
val tokenSupportVersion by extra("2.0.14")
val springCloudVersion by extra("2021.0.1")
val retryVersion by extra("1.3.0")
val zalandoVersion by extra("0.27.0")
val openhtmltopdfVersion = "1.0.10"
val handlebarsVersion = "4.3.0"
val hibernateTypes52Version by extra("2.15.1")
val awailitilityKotlinVersion by extra("4.1.1")
val assertkJvmVersion by extra("0.25")
val springMockkVersion by extra("3.1.1")
val mockkVersion by extra("1.12.3")
val guavaVersion by extra("31.1-jre")
val okHttp3Version by extra("4.9.1")
val orgJsonVersion by extra("20220320")
val graphQLKotlinVersion by extra("5.3.2")
val k9FormatVersion by extra("5.5.20")
val teamDokumenthåndteringAvroSchemaVersion by extra("bbea40a3")
val testContainersVersion by extra( "1.16.3")

val springdocVersion by extra("1.6.7")
ext["okhttp3.version"] = okHttp3Version
ext["testcontainersVersion"] = testContainersVersion

repositories {
    mavenCentral()

    maven {
        name = "confluent"
        url = uri("https://packages.confluent.io/maven/")
    }

    maven {
        name = "k9FormatPakker"
        url = uri("https://maven.pkg.github.com/navikt/k9-format")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
dependencies {

    // NAV
    implementation("no.nav.k9:soknad:$k9FormatVersion")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")

    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$teamDokumenthåndteringAvroSchemaVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("com.squareup.okhttp3:okhttp:$okHttp3Version")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.retry:spring-retry:$retryVersion")
    implementation("org.springframework:spring-aspects")
    runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "junit")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")

    // Spring Cloud
    // https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-contract-stub-runner
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
    testImplementation("org.springframework.cloud:spring-cloud-starter")

    // Swagger (openapi 3)
    implementation("org.springdoc:springdoc-openapi-kotlin:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-ui:$springdocVersion")


    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.hibernate:hibernate-jpamodelgen")
    implementation("com.vladmihalcea:hibernate-types-52:$hibernateTypes52Version")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    //graphql
    implementation("com.expediagroup:graphql-kotlin-spring-client:$graphQLKotlinVersion")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    //Kafka
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-connect-avro-converter:$confluentVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // PDF
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")
    implementation("com.github.jknack:handlebars:$handlebarsVersion")

    // Diverse
    implementation("org.json:json:$orgJsonVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.zalando:problem-spring-web-starter:$zalandoVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    testImplementation("org.awaitility:awaitility-kotlin:$awailitilityKotlinVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkJvmVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

/**
 * Generering av flere graphql klienter:
 *
 * GraphQL Kotlin Gradle Plugin registerer "tasks" for generering av en klients queries mot en singel endepunkt.
 * For å generere flere, må først den default "task"en hentes og konfigureres, og deretter opprette en ny "task" for en ny endepunkt.
 *
 * For mer info, se lenke under
 * https://opensource.expediagroup.com/graphql-kotlin/docs/plugins/gradle-plugin-usage#generating-multiple-clients
 */
val graphqlGenerateClient by tasks.getting(GraphQLGenerateClientTask::class) {
    queryFileDirectory.set(file("${project.projectDir}/src/main/resources/saf"))
    schemaFile.set(file("${project.projectDir}/src/main/resources/saf/saf-api-sdl.graphqls"))
    packageName.set("no.nav.sifinnsynapi.saf.generated")
}

val graphqlGenerateOtherClient by tasks.creating(GraphQLGenerateClientTask::class) {
    queryFileDirectory.set(file("${project.projectDir}/src/main/resources/safselvbetjening"))
    schemaFile.set(file("${project.projectDir}/src/main/resources/safselvbetjening/saf-selvbetjening-sdl.graphqls"))
    packageName.set("no.nav.sifinnsynapi.safselvbetjening.generated")
}
