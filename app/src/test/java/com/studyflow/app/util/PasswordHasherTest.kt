package com.studyflow.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `hash never returns the plain text password`() {
        val hash = PasswordHasher.hash("MySecret123!")
        assertNotEquals("MySecret123!", hash)
    }

    @Test
    fun `verify returns true for the correct password`() {
        val hash = PasswordHasher.hash("CorrectHorse42")
        assertTrue(PasswordHasher.verify("CorrectHorse42", hash))
    }

    @Test
    fun `verify returns false for an incorrect password`() {
        val hash = PasswordHasher.hash("CorrectHorse42")
        assertFalse(PasswordHasher.verify("WrongPassword", hash))
    }

    @Test
    fun `hashing the same password twice produces different hashes (salted)`() {
        val hashOne = PasswordHasher.hash("SamePassword")
        val hashTwo = PasswordHasher.hash("SamePassword")
        assertNotEquals(hashOne, hashTwo)
    }
}
