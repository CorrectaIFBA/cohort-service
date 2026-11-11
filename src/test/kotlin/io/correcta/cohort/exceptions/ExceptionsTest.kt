package io.correcta.cohort.exceptions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExceptionsTest {

  @Test
  fun `uses a default message`() {
    assertEquals("A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.", BadRequestException().message)
    assertEquals("A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.", ConflictException().message)
    assertEquals("Você não tem permissão para acessar este recurso.", ForbiddenException().message)
    assertEquals("Ocorreu um erro inesperado no servidor.", InternalServerErrorException().message)
    assertEquals("O recurso solicitado não foi encontrado.", NotFoundException().message)
    assertEquals("Um serviço necessário está indisponível no momento. Tente novamente mais tarde.", ServiceUnavailableException().message)
    assertEquals("É necessário estar autenticado para acessar este recurso.", UnauthorizedException().message)
  }

  @Test
  fun `keeps a custom message`() {
    assertEquals("custom", BadRequestException("custom").message)
  }
}