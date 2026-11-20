package com.app.calendar.core.lunar

/** Traditional Vietnamese names of the lunar months. */
object LunarNames {
    private val MONTHS = arrayOf(
        "Giêng", "Hai", "Ba", "Tư", "Năm", "Sáu",
        "Bảy", "Tám", "Chín", "Mười", "Mười Một", "Chạp",
    )

    /** "Giêng", "Hai", … "Chạp". */
    fun monthName(month: Int): String = MONTHS[month - 1]
}
