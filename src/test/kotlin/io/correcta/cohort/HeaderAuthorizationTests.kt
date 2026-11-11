package io.correcta.cohort

import io.correcta.cohort.security.models.IdentityHeaders
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class HeaderAuthorizationTests(
  @Autowired val mockMvc: MockMvc,
) {

  @Test
  fun `rejects requests without identity headers`() {
    mockMvc.get("/api/v1/cohorts").andExpect { status { isUnauthorized() } }
  }

  @Test
  fun `requires the FACULTY profile type to create cohorts`() {
    mockMvc.post("/api/v1/cohorts") {
      header(IdentityHeaders.PROFILE_ID, "profile-a")
      header(IdentityHeaders.PROFILE_TYPE, "STUDENT")
      contentType = MediaType.APPLICATION_JSON
      content = """{"name": "Turma"}"""
    }.andExpect { status { isForbidden() } }
  }

  @Test
  fun `hides cohorts from profiles that are not members`() {
    val response = mockMvc.post("/api/v1/cohorts") {
      header(IdentityHeaders.PROFILE_ID, "owner-a")
      header(IdentityHeaders.PROFILE_TYPE, "FACULTY")
      contentType = MediaType.APPLICATION_JSON
      content = """{"name": "Introdução à Computação"}"""
    }.andExpect {
      status { isCreated() }
      jsonPath("$.ownerProfileId") { value("owner-a") }
      jsonPath("$.slug") { value("introducao-a-computacao") }
      jsonPath("$.status") { value("DRAFT") }
    }.andReturn().response.contentAsString

    val id = Regex("\"id\":\"([^\"]+)\"").find(response)!!.groupValues[1]

    mockMvc.get("/api/v1/cohorts/$id/introducao-a-computacao") {
      header(IdentityHeaders.PROFILE_ID, "owner-a")
    }.andExpect { status { isOk() } }

    mockMvc.get("/api/v1/cohorts/$id/wrong-slug") {
      header(IdentityHeaders.PROFILE_ID, "owner-a")
    }.andExpect { status { isNotFound() } }

    mockMvc.get("/api/v1/cohorts/$id/introducao-a-computacao") {
      header(IdentityHeaders.PROFILE_ID, "profile-b")
    }.andExpect { status { isNotFound() } }
  }

  @Test
  fun `cohorts may share a slug and are told apart by their public code`() {
    val codes = (1..2).map {
      mockMvc.post("/api/v1/cohorts") {
        header(IdentityHeaders.PROFILE_ID, "owner-shared")
        header(IdentityHeaders.PROFILE_TYPE, "FACULTY")
        contentType = MediaType.APPLICATION_JSON
        content = """{"name": "Algoritmos", "slug": "algoritmos"}"""
      }.andExpect {
        status { isCreated() }
        jsonPath("$.slug") { value("algoritmos") }
      }.andReturn().response.contentAsString.let { Regex("\"publicCode\":\"([a-z0-9]{6})\"").find(it)!!.groupValues[1] }
    }

    assert(codes[0] != codes[1])

    codes.forEach { code ->
      mockMvc.get("/api/v1/cohorts/by-code/$code") {
        header(IdentityHeaders.PROFILE_ID, "owner-shared")
      }.andExpect {
        status { isOk() }
        jsonPath("$.publicCode") { value(code) }
      }
    }
  }
}