package id.saku.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * V1.6.8 #3 — helper izin & preferensi deteksi notifikasi pembayaran e-wallet.
 */
object NotifHelper {
    private const val PREFS = "saku_notif_prefs"
    private const val KEY_ENABLED = "notif_detect_enabled"
    const val CHANNEL_ID = "saku_quick_log"

    const val EXTRA_OPEN_SHEET = "saku_open_sheet"
    const val EXTRA_AMOUNT = "saku_prefill_amount"
    const val EXTRA_NOTE = "saku_prefill_note"
    const val EXTRA_TYPE = "saku_prefill_type"
    const val EXTRA_ACCOUNT = "saku_prefill_account"

    val supportedApps = listOf(
        "com.gojek.app" to "GoPay",
        "com.gojek.gopay" to "GoPay",
        "ovo.id" to "OVO",
        "id.dana" to "DANA",
        "com.shopee.id" to "ShopeePay",
        "com.telkom.mwallet" to "LinkAja",
        "com.bca" to "BCA mobile",
        "com.bca.mybca.omni.android" to "myBCA",
        "id.bmri.livin" to "Livin' Mandiri",
        "id.co.bri.brimo" to "BRImo",
        "src.com.bni" to "BNI Mobile",
        "id.bni.wondr" to "wondr BNI",
        "id.co.bankbkemobile.digitalbank" to "SeaBank",
        "com.jago.digitalBanking" to "Bank Jago"
    )

    private val pkgToName = supportedApps.toMap()

    fun syncEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun isEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun hasAccess(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val flat = ComponentName(ctx, PaymentNotifListener::class.java).flattenToString()
        return enabled.split(':').any { it.equals(flat, ignoreCase = true) || it.contains(ctx.packageName) }
    }

    fun openSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(intent) }
    }

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Pengingat Catat Transaksi",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Menawarkan pencatatan cepat saat terdeteksi notifikasi pembayaran e-wallet."
                    }
                )
            }
        }
    }

    fun appNameFor(pkg: String): String? = pkgToName[pkg]

    /** Ekstrak nominal rupiah dari teks notifikasi pembayaran Indonesia. */
    fun parseAmount(text: String): Long? {
        val regex = Regex("""(?:Rp|IDR)\.?\s*([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,9})(?:[.,][0-9]{2})?""", RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return null
        val digits = match.groupValues[1].filter { it.isDigit() }
        val value = digits.toLongOrNull() ?: return null
        return value.takeIf { it in 100L..1_000_000_000L }
    }

    fun looksLikePayment(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf(
            "bayar", "pembayaran", "berhasil", "transaksi", "qris",
            "debit", "transfer", "terima", "masuk", "top up", "topup",
            "kirim", "pembelian", "tagihan", "tarik"
        )
        return keywords.any { lower.contains(it) }
    }

    fun detectType(text: String): String {
        val lower = text.lowercase()
        val inWords = listOf("diterima", "menerima", "uang masuk", "masuk sebesar", "top up berhasil", "refund", "cashback")
        return if (inWords.any { lower.contains(it) }) "in" else "out"
    }
}

/**
 * V1.6.8 #3 — mendengarkan notifikasi pembayaran e-wallet/bank populer saat
 * diaktifkan pengguna, lalu menawarkan pencatatan cepat lewat notifikasi Saku.
 */
class PaymentNotifListener : NotificationListenerService() {
    @Volatile
    private var lastKey: String = ""
    @Volatile
    private var lastAt: Long = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        if (!NotifHelper.isEnabled(this)) return
        val pkg = posted.packageName ?: return
        if (pkg == packageName) return
        val sourceName = NotifHelper.appNameFor(pkg) ?: return

        val extras = posted.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        val big = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val full = listOf(title, big.ifBlank { body }).filter { it.isNotBlank() }.joinToString(" ")
        if (full.isBlank() || !NotifHelper.looksLikePayment(full)) return

        val amount = NotifHelper.parseAmount(full) ?: return
        val key = "$pkg:$amount"
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastAt < 15_000L) return
        lastKey = key
        lastAt = now

        val txType = NotifHelper.detectType(full)
        val note = if (txType == "in") "Masuk via $sourceName" else "Bayar via $sourceName"
        showQuickLogNotification(sourceName, amount, txType, note)
    }

    private fun showQuickLogNotification(sourceName: String, amount: Long, type: String, note: String) {
        NotifHelper.ensureChannel(this)
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotifHelper.EXTRA_OPEN_SHEET, true)
            putExtra(NotifHelper.EXTRA_AMOUNT, amount)
            putExtra(NotifHelper.EXTRA_NOTE, note)
            putExtra(NotifHelper.EXTRA_TYPE, type)
            putExtra(NotifHelper.EXTRA_ACCOUNT, "ewallet")
        }
        val pi = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 100_000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = if (type == "in") "Catat masuk ${Fmt.rp(amount)}?" else "Catat keluar ${Fmt.rp(amount)}?"
        val notif = NotificationCompat.Builder(this, NotifHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(label)
            .setContentText("Terdeteksi dari $sourceName \u00b7 Ketuk untuk catat cepat di Saku")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        runCatching { nm?.notify((System.currentTimeMillis() % 9000).toInt() + 1000, notif) }
    }
}
