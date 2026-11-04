package com.studyflow.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMappingTest {

    @Test
    fun `401 means wrong credentials`() {
        assertEquals(AuthError.INVALID_CREDENTIALS, AuthRepository.errorForCode(401))
    }

    @Test
    fun `409 means the email is already registered`() {
        assertEquals(AuthError.EMAIL_TAKEN, AuthRepository.errorForCode(409))
    }

    @Test
    fun `other 4xx codes are treated as invalid input`() {
        assertEquals(AuthError.INVALID_INPUT, AuthRepository.errorForCode(400))
    }

    @Test
    fun `5xx codes are server problems`() {
        assertEquals(AuthError.SERVER, AuthRepository.errorForCode(500))
        assertEquals(AuthError.SERVER, AuthRepository.errorForCode(503))
    }

    @Test
    fun `google sign-in rejections get their own message`() {
        assertEquals(AuthError.GOOGLE, AuthRepository.errorForCode(401, google = true))
    }
}
