package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.CourtDifferences.UpdateType.ERROR
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.CourtDifferences.UpdateType.INSERT
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.CourtDifferences.UpdateType.NONE
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.CourtDifferences.UpdateType.UPDATE
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService.CourtDataToSync
import java.lang.reflect.Type

@Service
class CourtRegisterUpdateService(
  private val courtRegisterService: CourtRegisterService,
  private val probationService: ProbationService,
  private val telemetryClient: TelemetryClient,
  private val gson: Gson
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateCourtDetails(court: CourtUpdate): SyncStatistics {
    val stats = SyncStatistics()
    log.info("About to update court $court")
    courtRegisterService.getCourtInfoFromRegister(court.courtId)?.run {
      val courtFromProbation = probationService.getCourtInformation(courtId)

      // TODO("Evaluate the court Register ProbationAreaCode from the postcode or an API")
      // The probationAreaCode should be calculated from the Court Register address or through an API before the sync process
      // Until this is determined, copy over the ProbationAreaCode from the corresponding Probation Court Data (if it exists)
      val registerPayload = courtFromProbation?.let { courtWithProbationArea(CourtDataToSync(this), courtFromProbation) } ?: CourtDataToSync(this)
      val probationPayload = courtFromProbation?.let { CourtDataToSync(courtFromProbation) }

      syncCourt(probationPayload, registerPayload, stats)
    } ?: log.error("Attempt to get court failed {}", court)
    log.debug("Sync Stats: $stats")
    return stats
  }

  fun courtWithProbationArea(courtDataToSync: CourtDataToSync, courtFromProbation: ProbationService.CourtFromProbationSystem): CourtDataToSync =
    courtDataToSync.copy(probationAreaCode = courtFromProbation.probationArea.code)

  fun syncCourt(currentCourtData: CourtDataToSync?, newCourtData: CourtDataToSync, stats: SyncStatistics) {

    val diff = checkForDifferences(currentCourtData, newCourtData)
    if (!diff.areEqual()) {
      stats.courts[newCourtData.code] = CourtDifferences(newCourtData.code, diff.toString())
      try {
        storeInProbation(currentCourtData, newCourtData, stats)
        if (stats.courts[newCourtData.code]?.updateType != NONE) {
          val trackingAttributes = mapOf(
            "courtId" to newCourtData.code,
            "differences" to stats.courts[newCourtData.code]?.differences,
          )
          telemetryClient.trackEvent("HR2DU-Court-Change", trackingAttributes, null)
        }
      } catch (e: Exception) {
        stats.courts[newCourtData.code] = stats.courts[newCourtData.code]!!.copy(updateType = ERROR)

        log.error("Failed to update {} - message = {}", newCourtData.code, e.message)
        telemetryClient.trackEvent("HR2DU-Court-Change-Failure", mapOf("courtId" to newCourtData.code), null)
      }
    }
  }

  private fun storeInProbation(
    currentCourtData: CourtDataToSync?,
    newCourtData: CourtDataToSync,
    stats: SyncStatistics
  ) {
    currentCourtData?.let {
      if (newCourtData != currentCourtData) { // don't update if equal
        probationService.updateCourt(newCourtData)
        stats.courts[newCourtData.code] = stats.courts[newCourtData.code]!!.copy(updateType = UPDATE)
      }
    } ?: run {
      probationService.insertCourt(newCourtData)
      stats.courts[newCourtData.code] = stats.courts[newCourtData.code]!!.copy(updateType = INSERT)
    }

    if (stats.courts[newCourtData.code]?.updateType == NONE) {
      stats.courts.remove(newCourtData.code)
    }
  }

  private fun checkForDifferences(
    existingRecord: CourtDataToSync?,
    newRecord: CourtDataToSync
  ): MapDifference<String, Any> {
    val type: Type = object : TypeToken<Map<String, Any>>() {}.type
    val leftMap: Map<String, Any> =
      if (existingRecord != null) gson.fromJson(gson.toJson(existingRecord), type) else mapOf()
    val rightMap: Map<String, Any> = gson.fromJson(gson.toJson(newRecord), type)
    return Maps.difference(leftMap, rightMap)
  }
}
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sync Statistics")
data class SyncStatistics(
  @Schema(description = "Map of all courts have have been inserted, updated or errored")
  val courts: MutableMap<String, CourtDifferences> = mutableMapOf()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Court Changes")
data class CourtDifferences(
  @Schema(description = "The ID of the Court", example = "SHFFCC") val courtId: String,
  @Schema(description = "Differences listed", example = "SHFFCC") val differences: String,
  @Schema(description = "Type of update", example = "INSERT") val updateType: UpdateType = NONE
) {

  enum class UpdateType {
    NONE, INSERT, UPDATE, ERROR
  }
}
