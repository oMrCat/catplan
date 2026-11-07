package io.github.omrcat.catplan.domain

/**
 * 健康日记异常标记：识别需要主人警惕的观察值（纯函数）。
 * 公猫泌尿系统阻塞、便便带血等是猫科急症的前兆，值得醒目提示。
 */
object HealthFlags {

    /** 异常值白名单（与 HealthNoteEntity 的选项常量对应） */
    private val ABNORMAL = setOf(
        "软便", "腹泻", "便秘", "带血", // 便便
        "尿多", "尿少", "血尿", // 尿液
        "萎靡", // 精神
        "差", // 食欲
    )

    /** 返回该条记录中的异常项列表；全部正常则返回空列表 */
    fun abnormalFlags(
        appetite: String?,
        energy: String?,
        poop: String?,
        urine: String?,
    ): List<String> = buildList {
        if (appetite in ABNORMAL) add("食欲 $appetite")
        if (energy in ABNORMAL) add("精神 $energy")
        if (poop in ABNORMAL) add("便便 $poop")
        if (urine in ABNORMAL) add("尿液 $urine")
    }

    /** 是否存在需要尽快就医级别的异常（血尿/便血） */
    fun urgent(flags: List<String>): Boolean = flags.any { it.contains("血") }
}
