package com.studyflow.app.util

import java.util.TimeZone

/** The badges a student can earn by studying consistently. */
enum class BadgeType { FIRST_SESSION, STREAK_3, STREAK_7, STREAK_30 }

/**
 * Pure (Android-free) gamification rules, so they can be unit tested on the JVM and are shared
 * by every screen. These mirror the rules in the API (api/services/gamification.js):
 *  - 1 XP per focused minute in a COMPLETED study session, 10 XP per completed task
 *  - a new level every 100 XP
 *  - a streak is the number of consecutive days with at least one completed session
 */
object Gamification {
    const val XP_PER_TASK = 10
    const val XP_PER_LEVEL = 100
    private const val DAY_MILLIS = 86_400_000L

    fun xpForSession(durationMins: Int): Int = durationMins.coerceAtLeast(0)

    fun totalXp(doneTasks: Int, completedSessionMinutes: Int): Int =
        doneTasks * XP_PER_TASK + completedSessionMinutes

    fun level(xp: Int): Int = xp / XP_PER_LEVEL + 1

    /** XP earned inside the current level (0..99) - drives the progress ring. */
    fun xpIntoLevel(xp: Int): Int = xp % XP_PER_LEVEL

    fun xpToNextLevel(xp: Int): Int = XP_PER_LEVEL - xpIntoLevel(xp)

    /** Whole days since the epoch for [millis] in the given time zone (so "today" matches the phone). */
    fun dayIndex(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Math.floorDiv(millis + timeZone.getOffset(millis), DAY_MILLIS)

    /** Streak ending today - or yesterday, so a streak is only lost after a whole missed day. */
    fun currentStreak(studyDays: Set<Long>, today: Long): Int {
        var cursor = if (today in studyDays) today else today - 1
        var streak = 0
        while (cursor in studyDays) {
            streak++
            cursor--
        }
        return streak
    }

    fun longestStreak(studyDays: Set<Long>): Int {
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in studyDays.sorted()) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            previous = day
            if (run > best) best = run
        }
        return best
    }

    fun earnedBadges(completedSessions: Int, longestStreak: Int): List<BadgeType> {
        val badges = mutableListOf<BadgeType>()
        if (completedSessions >= 1) badges += BadgeType.FIRST_SESSION
        if (longestStreak >= 3) badges += BadgeType.STREAK_3
        if (longestStreak >= 7) badges += BadgeType.STREAK_7
        if (longestStreak >= 30) badges += BadgeType.STREAK_30
        return badges
    }
}
