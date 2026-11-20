package com.app.calendar.core.lunar

/** 24 tiết khí, ordered from 0° solar longitude (Xuân phân) in 15° steps. */
enum class SolarTerm(val displayName: String) {
    XUAN_PHAN("Xuân phân"), THANH_MINH("Thanh minh"), COC_VU("Cốc vũ"), LAP_HA("Lập hạ"),
    TIEU_MAN("Tiểu mãn"), MANG_CHUNG("Mang chủng"), HA_CHI("Hạ chí"), TIEU_THU("Tiểu thử"),
    DAI_THU("Đại thử"), LAP_THU("Lập thu"), XU_THU("Xử thử"), BACH_LO("Bạch lộ"),
    THU_PHAN("Thu phân"), HAN_LO("Hàn lộ"), SUONG_GIANG("Sương giáng"), LAP_DONG("Lập đông"),
    TIEU_TUYET("Tiểu tuyết"), DAI_TUYET("Đại tuyết"), DONG_CHI("Đông chí"), TIEU_HAN("Tiểu hàn"),
    DAI_HAN("Đại hàn"), LAP_XUAN("Lập xuân"), VU_THUY("Vũ thủy"), KINH_TRAP("Kinh trập"),
}
