package io.github.omrcat.catplan.domain

import java.time.LocalDate

/** 根据生日计算人类可读的猫龄描述 */
object CatAge {

    fun describe(birthday: LocalDate, today: LocalDate = LocalDate.now()): String {
        if (!birthday.isBefore(today)) return "刚到家"

        var years = today.year - birthday.year
        var months = today.monthValue - birthday.monthValue
        if (today.dayOfMonth < birthday.dayOfMonth) months -= 1
        if (months < 0) {
            years -= 1
            months += 12
        }
        months = months.coerceAtLeast(0)

        return when {
            years < 0 -> "刚到家"
            years == 0 && months == 0 -> "未满月"
            years == 0 -> "$months 个月"
            months == 0 -> "$years 岁"
            else -> "$years 岁 $months 个月"
        }
    }
}
