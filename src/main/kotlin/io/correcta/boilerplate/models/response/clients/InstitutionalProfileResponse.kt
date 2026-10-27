package io.correcta.boilerplate.models.response.clients

data class InstitutionalProfileResponse(
    val profileId: String,
    val type: String,
    val state: String,
    val profileActive: Boolean,
    val credentialActive: Boolean,
    val displayName: String,
    val email: String,
    val login: String?
) {

    fun isEligible(): Boolean {
        return state == "ACTIVE" && profileActive && credentialActive && type in COHORT_PROFILE_TYPES
    }

    companion object {
        val COHORT_PROFILE_TYPES = setOf("STUDENT", "FACULTY")
    }
}
