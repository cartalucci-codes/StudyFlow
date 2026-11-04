package com.studyflow.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class GamificationTest {

    @Test
    fun `xp is 1 per focused minute plus 10 per completed task`() {
        assertEquals(0, Gamification.totalXp(0, 0))
        assertEquals(10, Gamification.totalXp(1, 0))
        assertEquals(25, Gamification.totalXp(0, 25))
        assertEquals(65, Gamification.totalXp(4, 25))
    }

    @Test
    fun `negative session minutes never reduce xp`() {
        assertEquals(0, Gamification.xpForSession(-5))
    }

    @Test
    fun `a new level every 100 xp`() {
        assertEquals(1, Gamification.level(0))
        assertEquals(1, Gamification.level(99))
        assertEquals(2, Gamification.level(100))
        assertEquals(4, Gamification.level(350))
    }

    @Test
    fun `progress ring values stay inside the current level`() {
        assertEquals(0, Gamification.xpIntoLevel(200))
        assertEquals(35, Gamification.xpIntoLevel(235))
        assertEquals(65, Gamification.xpToNextLevel(235))
        assertEquals(100, Gamification.xpToNextLevel(200))
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        assertEquals(3, Gamification.currentStreak(setOf(10L, 9L, 8L), today = 10L))
    }

    @Test
    fun `streak survives until a whole day is missed`() {
        // studied up to yesterday, not yet today -> streak still alive
        assertEquals(2, Gamification.currentStreak(setOf(9L, 8L), today = 10L))
        // last study day was two days ago -> streak is lost
        assertEquals(0, Gamification.currentStreak(setOf(8L, 7L), today = 10L))
    }

    @Test
    fun `a gap resets the current streak`() {
        assertEquals(1, Gamification.currentStreak(setOf(10L, 7L, 6L), today = 10L))
    }

    @Test
    fun `longest streak finds the best run`() {
        assertEquals(3, Gamification.longestStreak(setOf(1L, 2L, 3L, 10L, 11L)))
        assertEquals(0, Gamification.longestStreak(emptySet()))
        assertEquals(1, Gamification.longestStreak(setOf(5L)))
    }

    @Test
    fun `badges are earned at the right thresholds`() {
        assertTrue(Gamification.earnedBadges(0, 0).isEmpty())
        assertEquals(listOf(BadgeType.FIRST_SESSION), Gamification.earnedBadges(1, 1))
        assertEquals(
            listOf(BadgeType.FIRST_SESSION, BadgeType.STREAK_3, BadgeType.STREAK_7),
            Gamification.earnedBadges(9, 7)
        )
        assertTrue(BadgeType.STREAK_30 in Gamification.earnedBadges(40, 30))
    }

    @Test
    fun `day index follows the given time zone`() {
        val utc = TimeZone.getTimeZone("UTC")
        val johannesburg = TimeZone.getTimeZone("Africa/Johannesburg") // UTC+2
        val lateEveningUtc = 23L * 3_600_000L // 1970-01-01 23:00 UTC
        assertEquals(0L, Gamification.dayIndex(lateEveningUtc, utc))
        assertEquals(1L, Gamification.dayIndex(lateEveningUtc, johannesburg)) // already tomorrow in SA
    }
}
