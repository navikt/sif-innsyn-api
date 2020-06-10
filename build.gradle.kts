import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.2.7.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
	kotlin("plugin.jpa") version "1.3.72"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

val developmentOnly by configurations.creating
configurations {
	runtimeClasspath {
		extendsFrom(developmentOnly)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}
val springfoxVersion by extra("2.9.2")
val confluentVersion by extra( "5.5.0")

val avroVersion by extra( "1.9.2")
val brukernotifikasjonVersion by extra("1.2020.03.25-11.14-c3621e6d211a")
val logstashLogbackEncoderVersion by extra("6.3")
val tokenValidationVersion by extra("1.1.5")
val springCloudVersion by extra( "Hoxton.SR5")



repositories {
	mavenCentral()

	maven {
		name = "github-package-registry-navikt"
		url = uri("https://maven.pkg.github.com/navikt/maven-releas")
	}
}

dependencies {

	// NAV
	implementation("no.nav:brukernotifikasjon-schemas:$brukernotifikasjonVersion")
	implementation("no.nav.security:token-validation-spring:$tokenValidationVersion")
	testImplementation("no.nav.security:token-validation-test-support:$tokenValidationVersion")

	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Spring Cloud
	implementation("org.springframework.cloud:spring-cloud-gcp-starter-secretmanager")

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
	testImplementation("org.hsqldb:hsqldb")
	implementation("com.github.ben-manes.caffeine:caffeine")

	// Jackson
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// Project Reactor
	testImplementation("io.projectreactor:reactor-test")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
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
		jvmTarget = "1.8"
	}
}
