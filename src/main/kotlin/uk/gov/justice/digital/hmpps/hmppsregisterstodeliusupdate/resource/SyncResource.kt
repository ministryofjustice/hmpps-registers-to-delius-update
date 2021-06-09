package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.CourtRegisterSyncService
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.SyncStatistics

@RestController
@Validated
@RequestMapping(name = "Sync To Delius", path = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class SyncResource(
  private val courtRegisterSyncService: CourtRegisterSyncService
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Sync all court details from HMPPS Court Register",
    description = "Updates court information, role required is ROLE_MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_REF_DATA", scopes = ["write"])],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court Information Synchronised",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SyncStatistics::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court sync",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  @PutMapping("")
  fun syncCourts(): SyncStatistics {
    return courtRegisterSyncService.sync()
  }
}
