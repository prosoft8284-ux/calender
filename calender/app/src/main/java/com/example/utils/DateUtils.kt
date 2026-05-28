package com.example.utils

import java.text.SimpleDateFormat
import java.util.*

class JalaliDate(val year: Int, val month: Int, val day: Int) {
    override fun toString(): String {
        return "$year/$month/$day"
    }
}

object DateUtils {

    private val jalaliMonths = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(text: String): String {
        var result = text
        for (i in 0..9) {
            result = result.replace(i.toString(), persianDigits[i].toString())
        }
        return result
    }

    fun toPersianDigits(value: Int): String {
        return toPersianDigits(value.toString())
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        var gYear = gy - 1600
        var gMonth = gm - 1
        var gDay = gd - 1

        var gDayNo = 365 * gYear + gYear / 4 - gYear / 100 + gYear / 400
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        for (i in 0 until gMonth) {
            gDayNo += gDaysInMonth[i]
        }
        if (gMonth > 1 && ((gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gDay

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jYear = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jYear += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jMonth = 0
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        while (jMonth < 12 && jDayNo >= jDaysInMonth[jMonth]) {
            jDayNo -= jDaysInMonth[jMonth]
            jMonth++
        }
        val jDay = jDayNo + 1
        return JalaliDate(jYear, jMonth + 1, jDay)
    }

    fun formatToJalaliString(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)

        val jalali = gregorianToJalali(gy, gm, gd)
        return toPersianDigits("${jalali.day} ${jalaliMonths[jalali.month - 1]} ${jalali.year}")
    }

    fun formatToJalaliMonthDay(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)

        val jalali = gregorianToJalali(gy, gm, gd)
        return toPersianDigits("${jalali.day} ${jalaliMonths[jalali.month - 1]}")
    }

    fun formatToJalaliMonthDayFromString(dateString: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = sdf.parse(dateString) ?: Date()
            formatToJalaliMonthDay(date)
        } catch (e: Exception) {
            toPersianDigits(dateString)
        }
    }

    fun getDateString(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(date)
    }

    fun getTodayString(): String {
        return getDateString(Date())
    }

    fun getDatesBetween(startDateMs: Long, endDateMs: Long): List<Date> {
        val dates = mutableListOf<Date>()
        val cal = Calendar.getInstance()

        // Normalize Start Date
        cal.timeInMillis = startDateMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMidnight = cal.timeInMillis

        // Normalize End Date
        cal.timeInMillis = endDateMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val endMidnight = cal.timeInMillis

        cal.timeInMillis = startMidnight
        while (cal.timeInMillis <= endMidnight) {
            dates.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            // Limit to max 366 days for memory/UI protection
            if (dates.size > 366) break
        }
        return dates
    }

    /**
     * Comparing string date key to today:
     * Returns:
     * -1 if dateString is in the past
     * 0 if dateString is today
     * 1 if dateString is in the future
     */
    fun compareToToday(dateString: String): Int {
        val todayStr = getTodayString()
        if (dateString == todayStr) return 0
        return dateString.compareTo(todayStr)
    }

    fun getDayOfWeekPersianName(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }
}
