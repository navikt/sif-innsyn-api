import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.20"
    kotlin("plugin.jpa") version "1.8.10"
    id("org.springframework.boot") version "3.0.5"
    id("io.spring.dependency-management") version "1.1.0"
    id("com.expediagroup.graphql") version "6.4.0"
    id("org.sonarqube") version "4.0.0.2929"
    jacoco
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val confluentVersion by extra("7.3.0")
val logstashLogbackEncoderVersion by extra("7.2")
val tokenSupportVersion by extra("3.0.8")
val springCloudVersion by extra("2022.0.0-RC2")
val retryVersion by extra("2.0.0")
val zalandoVersion by extra("0.27.0")
val openhtmltopdfVersion by extra("1.0.10")
val handlebarsVersion by extra("4.3.1")
val postgresqlVersion by extra("42.5.1")
val awailitilityKotlinVersion by extra("4.2.0")
val assertkJvmVersion by extra("0.25")
val springMockkVersion by extra("3.1.2")
val mockkVersion by extra("1.13.2")
val guavaVersion by extra("31.1-jre")
val orgJsonVersion by extra("20230227")
val graphQLKotlinVersion by extra("6.3.0")
val k9FormatVersion by extra("8.0.7")
val teamDokumenthåndteringAvroSchemaVersion by extra("357738b9")
val testContainersVersion by extra("1.17.6")

val springdocVersion by extra("2.0.0")
ext["testcontainersVersion"] = testContainersVersion

repositories {
    mavenCentral()

    maven { url = uri("https://repo.spring.io/milestone") }

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

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.2")

    // Spring Cloud
    // https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-contract-stub-runner
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
    testImplementation("org.springframework.cloud:spring-cloud-starter")

    // Swagger (openapi 3)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
    implementation("org.flywaydb:flyway-core")
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
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
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
