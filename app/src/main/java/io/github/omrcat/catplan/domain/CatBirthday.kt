package io.github.omrcat.catplan.domain

import java.time.LocalDate

/** 生日提醒：下一个生日的计算（纯函数） */
object CatBirthday {

    /** 下一个生日；闰 2/29 在非闰年顺延为 2/28 */
    fun nextBirthday(birthday: LocalDate, today: LocalDate): LocalDate {
        val month = birthday.monthValue
        val day = birthday.dayOfMonth
        val candidate = buildCandidate(today.year, month, day)
        return if (!candidate.isBefore(today)) candidate else buildCandidate(today.year + 1, month, day)
    }

    fun daysUntil(birthday: LocalDate, today: LocalDate): Int =
        java.time.temporal.ChronoUnit.DAYS.between(today, nextBirthday(birthday, today)).toInt()

    private fun buildCandidate(year: Int, month: Int, day: Int): LocalDate = runCatching {
        LocalDate.of(year, month, day)
    }.getOrElse {
        // 2月29日在非闰年 → 顺延到 2月28日
        LocalDate.of(year, month, day - 1)
    }
}
