plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.0"
  kotlin("plugin.spring") version "1.6.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.3")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

  implementation("com.google.code.gson:gson:2.8.9")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.1")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.1")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.1")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.testcontainers:localstack:1.16.2")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
