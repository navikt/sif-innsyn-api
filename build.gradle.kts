import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.expediagroup.graphql") version "8.8.1"
    id("org.sonarqube") version "6.2.0.5505"
    jacoco
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val confluentVersion = "8.0.0"
val springCloudVersion = "4.3.0"
val logstashLogbackEncoderVersion = "8.1"
val tokenSupportVersion = "5.0.30"
val retryVersion = "2.0.12"
val zalandoVersion = "0.27.0"
val openhtmltopdfVersion = "1.0.10"
val handlebarsVersion = "4.4.0"
val postgresqlVersion = "42.7.7"
val awailitilityKotlinVersion = "4.3.0"
val assertkJvmVersion = "0.28.1"
val springMockkVersion = "4.0.2"
val mockkVersion = "1.14.4"
val guavaVersion = "33.4.8-jre"
val orgJsonVersion = "20250517"
val graphQLKotlinVersion = "8.8.1"
val k9FormatVersion = "9.5.0"
val teamDokumenthåndteringAvroSchemaVersion = "357738b9"
val testContainersVersion = "1.21.2"
val springdocVersion = "2.8.9"

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
            username = "sif-innsyn-api"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
dependencies {

    implementation("org.yaml:snakeyaml:2.4") {
        because("https://github.com/navikt/sif-innsyn-api/security/dependabot/2")
    }

    // NAV
    implementation("no.nav.k9:soknad:$k9FormatVersion")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")

    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$teamDokumenthåndteringAvroSchemaVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web") {
       // exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    //implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.retry:spring-retry:$retryVersion")
    implementation("org.springframework:spring-aspects")
    runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }

    // Spring Cloud
    // https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-contract-stub-runner
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner:$springCloudVersion")

    // Swagger (openapi 3)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
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
    constraints {
        implementation("org.scala-lang:scala-library") {
            because("org.apache.kafka:kafka_2.13:3.3.2 -> https://www.cve.org/CVERecord?id=CVE-2022-36944")
            version {
                require("2.13.9")
            }
        }
    }
    implementation("io.confluent:kafka-connect-avro-converter:$confluentVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:1.12.0")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // PDF
    implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    implementation("com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")
    implementation("com.github.jknack:handlebars:$handlebarsVersion")

    // Diverse
    implementation("org.json:json:$orgJsonVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.google.guava:guava:$guavaVersion")
    testImplementation("org.awaitility:awaitility-kotlin:$awailitilityKotlinVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkJvmVersion")
    testImplementation("com.ninja-squad:springmockk:$springMockkVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_sif-innsyn-api")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.sourceEncoding", "UTF-8")
    }
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
