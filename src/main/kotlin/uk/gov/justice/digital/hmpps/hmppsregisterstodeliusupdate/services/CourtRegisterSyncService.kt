package uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService.CourtDataToSync
import uk.gov.justice.digital.hmpps.hmppsregisterstodeliusupdate.services.ProbationService.CourtFromProbationSystem

@Service
class CourtRegisterSyncService(
  private val courtRegisterUpdateService: CourtRegisterUpdateService,
  private val courtRegisterService: CourtRegisterService,
  private val probationService: ProbationService
) {

  fun sync(): SyncStatistics {
    return syncAllCourts(probationService.getAllCourts(), courtRegisterService.getAllActiveCourts())
  }

  private fun syncAllCourts(
    probationCourts: List<CourtFromProbationSystem>,
    courtRegisterCourts: List<CourtDto>
  ): SyncStatistics {

    val probationCourtsMap = probationCourts.map { CourtDataToSync(it) }.associateBy { it.code }
    val courtRegisterMap = courtRegisterCourts.map { CourtDataToSync(it) }.associateBy { it.code }

    val stats = SyncStatistics()
    courtRegisterMap.filterMatching(probationCourtsMap)
      .forEach {
        // TODO("Evaluate the court Register ProbationAreaCode from the postcode or an API")
        // The probationAreaCode should be calculated from the Court Register address or through an API before the sync process
        // Until this is determined, copy over the ProbationAreaCode from the corresponding Probation Court Data
        val courtFromRegisterWithProbationArea = it.value.copy(probationAreaCode = probationCourtsMap[it.key]!!.probationAreaCode)
        courtRegisterUpdateService.syncCourt(probationCourtsMap[it.key], courtFromRegisterWithProbationArea, stats)
      }

    courtRegisterMap.filterNew(probationCourtsMap)
      .forEach { courtRegisterUpdateService.syncCourt(null, it.value, stats) }

    probationCourtsMap.filterInactiveOrMissing(courtRegisterMap)
      .forEach {
        courtRegisterUpdateService.syncCourt(
          probationCourtsMap[it.key],
          it.value.copy(active = false),
          stats
        )
      }

    return stats
  }

  private fun <K, V> Map<out K, V>.filterNew(courtsMap: Map<K, V>): Map<K, V> = filter { c -> courtsMap[c.key] == null }
  private fun <K, V> Map<out K, V>.filterMatching(courtsMap: Map<K, V>): Map<K, V> = filter { c -> courtsMap[c.key] != null }
  private fun <K, V> Map<out K, CourtDataToSync>.filterInactiveOrMissing(courtsMap: Map<K, V>): Map<K, CourtDataToSync> {
    return filter { c -> c.value.active && courtsMap[c.key] == null }
  }
}
