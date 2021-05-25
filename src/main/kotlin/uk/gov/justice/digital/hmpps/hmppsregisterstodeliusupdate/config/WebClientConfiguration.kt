package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${oauth.endpoint.url}") private val oauthRootUri: String,
  @Value("\${probation.endpoint.url}") private val probationRootUri: String,
  @Value("\${court.register.endpoint.url}") private val courtRegisterRootUri: String,
  private val webClientBuilder: WebClient.Builder
) {

  @Bean
  fun probationApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("probation-api")

    return webClientBuilder
      .baseUrl(probationRootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun courtRegisterApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    return webClientBuilder
      .baseUrl(courtRegisterRootUri)
      .build()
  }

  @Bean
  fun courtRegisterHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(courtRegisterRootUri).build()
  }

  @Bean
  fun probationApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(probationRootUri).build()
  }

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(oauthRootUri).build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
