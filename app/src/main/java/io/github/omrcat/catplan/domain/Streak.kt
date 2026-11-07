package io.github.omrcat.catplan.domain

/** 连续记录天数：任意打卡/记录的日期去重后向下连续计数（纯函数） */
object Streak {

    /**
     * @param recordedDates 所有产生过记录的 epochDay（乱序、可重复）
     * @return 连续天数：今天有记录从今天起算，今天没有则从昨天起算；都没有为 0
     */
    fun streakDays(recordedDates: List<Long>, todayEpochDay: Long): Int {
        val days = recordedDates.distinct().sortedDescending()
        if (days.isEmpty()) return 0
        val anchor = if (days.contains(todayEpochDay)) todayEpochDay else todayEpochDay - 1
        if (!days.contains(anchor)) return 0
        var streak = 0
        var cursor = anchor
        while (days.contains(cursor)) {
            streak++
            cursor--
        }
        return streak
    }
}
