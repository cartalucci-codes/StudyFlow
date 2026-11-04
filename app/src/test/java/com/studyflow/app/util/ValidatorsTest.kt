package com.studyflow.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `accepts normal email addresses`() {
        assertTrue(Validators.isValidEmail("cj@gmail.com"))
        assertTrue(Validators.isValidEmail("  first.last+tag@school.ac.za  "))
    }

    @Test
    fun `rejects malformed email addresses`() {
        assertFalse(Validators.isValidEmail(""))
        assertFalse(Validators.isValidEmail("not-an-email"))
        assertFalse(Validators.isValidEmail("missing@tld"))
        assertFalse(Validators.isValidEmail("@no-name.com"))
        assertFalse(Validators.isValidEmail("spaces in@mail.com"))
    }

    @Test
    fun `password needs 8 characters with a letter and a number`() {
        assertTrue(Validators.isStrongPassword("Study2026"))
        assertFalse(Validators.isStrongPassword("short1"))
        assertFalse(Validators.isStrongPassword("onlyletters"))
        assertFalse(Validators.isStrongPassword("12345678"))
    }

    @Test
    fun `task title must be present and not too long`() {
        assertTrue(Validators.isValidTitle("Maths assignment"))
        assertFalse(Validators.isValidTitle("   "))
        assertFalse(Validators.isValidTitle("x".repeat(151)))
        assertTrue(Validators.isValidTitle("x".repeat(150)))
    }

    @Test
    fun `name must be present`() {
        assertTrue(Validators.isValidName("Cameron"))
        assertFalse(Validators.isValidName(""))
    }
}
