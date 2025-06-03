import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// 보드 날짜 아이템 변환
fun getDisplayDate(updatedAt: String): String {
    return try {
        // 'T' → ' ' 변환 후, 마이크로초 제거
        val cleanedDate = updatedAt.substringBefore(".").replace("T", " ")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val updatedDate = sdf.parse(cleanedDate) ?: Date()

        val now = Date()
        val diffMillis = now.time - updatedDate.time
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

        when {
            days < 1 -> "오늘"
            days < 15 -> "${days}일 전"
            else -> "${days / 30}개월 전"
        }
    } catch (e: Exception) {
        "N/A"
    }
}
