package io.correcta.boilerplate.service

import feign.FeignException
import io.correcta.boilerplate.clients.IdentityClient
import io.correcta.boilerplate.exceptions.ServiceUnavailableException
import io.correcta.boilerplate.models.request.clients.ProfileBatchLookupRequest
import io.correcta.boilerplate.models.response.clients.InstitutionalProfileResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class IdentityProfileService(
    val identityClient: IdentityClient
) {

    private val log: Logger = LoggerFactory.getLogger(IdentityProfileService::class.java)

    fun findProfile(profileId: String): InstitutionalProfileResponse? {
        return try {
            identityClient.getProfile(profileId)
        } catch (exception: FeignException.NotFound) {
            null
        } catch (exception: FeignException) {
            throw unavailable(exception)
        }
    }

    fun findProfiles(profileIds: Collection<String>): Map<String, InstitutionalProfileResponse> {
        if (profileIds.isEmpty()) return emptyMap()

        return try {
            identityClient.getProfiles(ProfileBatchLookupRequest(profileIds.distinct()))
                .profiles
                .associateBy { it.profileId }
        } catch (exception: FeignException) {
            throw unavailable(exception)
        }
    }

    fun searchProfiles(search: String, type: String?, size: Int): List<InstitutionalProfileResponse> {
        return try {
            identityClient.searchProfiles(search, type, size)
        } catch (exception: FeignException) {
            throw unavailable(exception)
        }
    }

    private fun unavailable(exception: FeignException): ServiceUnavailableException {
        log.error("Identity-service request failed with status {}", exception.status(), exception)

        return ServiceUnavailableException("O serviço de identidade está indisponível no momento. Tente novamente mais tarde.")
    }
}
