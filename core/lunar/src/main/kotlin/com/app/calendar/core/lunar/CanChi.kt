package com.app.calendar.core.lunar

/** Thiên can (10 heavenly stems). */
enum class Can(val displayName: String) {
    GIAP("Giáp"), AT("Ất"), BINH("Bính"), DINH("Đinh"), MAU("Mậu"),
    KY("Kỷ"), CANH("Canh"), TAN("Tân"), NHAM("Nhâm"), QUY("Quý"),
}

/** Địa chi (12 earthly branches). */
enum class Chi(val displayName: String, val zodiacAnimal: String) {
    TY("Tý", "Chuột"), SUU("Sửu", "Trâu"), DAN("Dần", "Hổ"), MAO("Mão", "Mèo"),
    THIN("Thìn", "Rồng"), TY_SNAKE("Tỵ", "Rắn"), NGO("Ngọ", "Ngựa"), MUI("Mùi", "Dê"),
    THAN("Thân", "Khỉ"), DAU("Dậu", "Gà"), TUAT("Tuất", "Chó"), HOI("Hợi", "Lợn");

    /** Local clock hours covered by this branch, e.g. Tý = 23h–1h. */
    val startHour: Int get() = (ordinal * 2 + 23) % 24
    val endHour: Int get() = (ordinal * 2 + 1) % 24
}

data class CanChi(val can: Can, val chi: Chi) {
    val displayName: String get() = "${can.displayName} ${chi.displayName}"
    override fun toString(): String = displayName
}

internal fun canChiOf(canIndex: Int, chiIndex: Int): CanChi =
    CanChi(Can.entries[Math.floorMod(canIndex, 10)], Chi.entries[Math.floorMod(chiIndex, 12)])
