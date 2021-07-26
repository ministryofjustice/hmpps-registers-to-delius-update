plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.5-beta"
  kotlin("plugin.spring") version "1.5.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework:spring-jms")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

  implementation("com.google.code.gson:gson:2.8.7")
  implementation(platform("com.amazonaws:aws-java-sdk-bom:1.12.2"))
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:1.0.8")

  implementation("org.springdoc:springdoc-openapi-ui:1.5.9")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.9")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.5.9")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.0")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
