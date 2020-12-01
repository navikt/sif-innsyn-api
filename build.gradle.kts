import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm") version "1.4.20"
    kotlin("plugin.spring") version "1.4.20"
    kotlin("plugin.jpa") version "1.4.20"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
val springfoxVersion by extra("2.9.2")
val confluentVersion by extra("5.5.0")

val avroVersion by extra("1.9.2")
val logstashLogbackEncoderVersion by extra("6.3")
val tokenValidationVersion by extra("1.1.5")
val springCloudVersion by extra("Hoxton.SR6")
val retryVersion by extra("1.3.0")
val zalandoVersion by extra("0.26.2")


repositories {
    mavenCentral()

    maven {
        name = "github-package-registry-navikt"
        url = uri("https://maven.pkg.github.com/navikt/maven-releas")
    }

    maven {
        name = "confluent"
        url = uri("http://packages.confluent.io/maven/")
    }
}

dependencies {

    // NAV
    implementation("no.nav.security:token-validation-spring:$tokenValidationVersion")
    testImplementation("no.nav.security:token-validation-test-support:$tokenValidationVersion")

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
    //developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Spring Cloud
    // https://mvnrepository.com/artifact/org.springframework.cloud/spring-cloud-starter-contract-stub-runner
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-stub-runner")
    testImplementation("org.springframework.cloud:spring-cloud-starter")

    // SpringFox
    implementation("io.springfox:springfox-swagger2:$springfoxVersion")
    implementation("io.springfox:springfox-swagger-ui:$springfoxVersion")

    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.hibernate:hibernate-jpamodelgen")
    implementation("com.vladmihalcea:hibernate-types-52:2.3.4")
    testImplementation("org.testcontainers:postgresql:1.15.0")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    //Kafka
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-connect-avro-converter:$confluentVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.springframework.kafka:spring-kafka")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // https://github.com/spring-projects/spring-kafka/issues/1604
    testImplementation("org.scala-lang:scala-library:2.12.11")

    // Diverse
    implementation("org.json:json:20200518")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.zalando:problem-spring-web-starter:$zalandoVersion")

    testImplementation("org.awaitility:awaitility-kotlin:4.0.3")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.21")
    testImplementation("com.ninja-squad:springmockk:2.0.3")
    testImplementation("io.mockk:mockk:1.10.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}
