package id.saku.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * V1.6.8 / M4 — Widget 4×2 di layar utama dengan tata letak 3-band responsif,
 * paritas tema dinamis [Tok], tombol sembunyikan saldo (Eye toggle), 2 transaksi
 * terbaru, dan aksi cepat "− Keluar" / "+ Masuk".
 */
class SakuWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_HIDE) {
            val pendingResult = goAsync()
            val appCtx = context.applicationContext
            widgetScope.launch {
                try {
                    toggleMutex.withLock {
                        val store = Store(appCtx)
                        val cur = runCatching { store.load() }.getOrElse { Saku() }
                        val next = cur.copy(hideBalance = !cur.hideBalance)
                        store.save(next)
                        withContext(Dispatchers.Main) {
                            runCatching { updateAllWidgets(appCtx, next) }
                        }
                        val verifyStore = Store(appCtx)
                        repeat(12) {
                            val persisted = runCatching { verifyStore.load().hideBalance }.getOrNull()
                            if (persisted == next.hideBalance) return@repeat
                            delay(25L)
                        }
                    }
                } finally {
                    pendingResult?.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        val idsCopy = appWidgetIds.copyOf()
        widgetScope.launch {
            try {
                val saku = runCatching { Store(appCtx).load() }.getOrElse { Saku() }
                withContext(Dispatchers.Main) {
                    runCatching {
                        val mgr = AppWidgetManager.getInstance(appCtx)
                        idsCopy.forEach { id ->
                            val options = mgr.getAppWidgetOptions(id)
                            mgr.updateAppWidget(id, buildViews(appCtx, saku, options))
                        }
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        widgetScope.launch {
            try {
                val saku = runCatching { Store(appCtx).load() }.getOrElse { Saku() }
                withContext(Dispatchers.Main) {
                    runCatching {
                        appWidgetManager.updateAppWidget(appWidgetId, buildViews(appCtx, saku, newOptions))
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_OPEN = "id.saku.app.WIDGET_OPEN"
        const val ACTION_OUT = "id.saku.app.WIDGET_OUT"
        const val ACTION_IN = "id.saku.app.WIDGET_IN"
        const val ACTION_TOGGLE_HIDE = "id.saku.app.WIDGET_TOGGLE_HIDE"

        private const val REQ_OPEN = 101
        private const val REQ_OUT = 102
        private const val REQ_IN = 103
        private const val REQ_TOGGLE_HIDE = 104

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val toggleMutex = Mutex()

        fun requestUpdate(context: Context, saku: Saku? = null) {
            runCatching {
                val appCtx = context.applicationContext
                val mgr = AppWidgetManager.getInstance(appCtx)
                val ids = mgr.getAppWidgetIds(ComponentName(appCtx, SakuWidgetProvider::class.java))
                if (ids.isEmpty()) return
                val data = saku ?: Store(appCtx).load()
                ids.forEach { id ->
                    val options = mgr.getAppWidgetOptions(id)
                    mgr.updateAppWidget(id, buildViews(appCtx, data, options))
                }
            }
        }

        private fun updateAllWidgets(appCtx: Context, saku: Saku) {
            val mgr = AppWidgetManager.getInstance(appCtx)
            val ids = mgr.getAppWidgetIds(ComponentName(appCtx, SakuWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id ->
                val options = mgr.getAppWidgetOptions(id)
                mgr.updateAppWidget(id, buildViews(appCtx, saku, options))
            }
        }

        private fun buildViews(context: Context, saku: Saku, options: Bundle?): RemoteViews {
            val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val layoutRes = selectWidgetLayout(minHeightDp)
            val isTall = layoutRes == R.layout.widget_saku_tall

            val model = buildWidgetModel(saku, Calendar.getInstance())
            val colors = resolveWidgetColors(saku.tokens())

            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(
                context,
                REQ_OPEN,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val outIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OUT
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotifHelper.EXTRA_OPEN_SHEET, true)
                putExtra(NotifHelper.EXTRA_TYPE, "out")
            }
            val outPi = PendingIntent.getActivity(
                context,
                REQ_OUT,
                outIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val inIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_IN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotifHelper.EXTRA_OPEN_SHEET, true)
                putExtra(NotifHelper.EXTRA_TYPE, "in")
            }
            val inPi = PendingIntent.getActivity(
                context,
                REQ_IN,
                inIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val toggleIntent = Intent(context, SakuWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_HIDE
            }
            val togglePi = PendingIntent.getBroadcast(
                context,
                REQ_TOGGLE_HIDE,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return RemoteViews(context.packageName, layoutRes).apply {
                // Dynamic Saku theme surfaces & hairlines (API 26–37 safe via opaque setColorFilter)
                setInt(R.id.widget_bg_fill, "setColorFilter", colors.cardBg)
                setInt(R.id.widget_bg_stroke, "setColorFilter", colors.cardBorder)
                setInt(R.id.widget_divider, "setBackgroundColor", colors.divider)

                // Band 1: Header + Eye toggle
                setTextColor(R.id.widget_brand, colors.ink)
                setTextViewText(R.id.widget_month, model.monthLabel)
                setTextColor(R.id.widget_month, colors.muted2)
                setTextViewText(R.id.widget_diff, model.diffText)
                setTextColor(
                    R.id.widget_diff,
                    when {
                        model.hideBalance -> colors.muted2
                        model.diffKind > 0 -> colors.pos
                        model.diffKind < 0 -> colors.error
                        else -> colors.muted2
                    }
                )
                setInt(R.id.widget_eye_bg, "setColorFilter", colors.eyeBg)
                setImageViewResource(
                    R.id.widget_eye_icon,
                    if (model.hideBalance) R.drawable.widget_ic_eye_off else R.drawable.widget_ic_eye
                )
                setInt(R.id.widget_eye_icon, "setColorFilter", colors.muted)
                setContentDescription(
                    R.id.widget_eye_btn,
                    context.getString(
                        if (model.hideBalance) R.string.widget_cd_eye_show else R.string.widget_cd_eye_hide
                    )
                )

                // Band 2: Hero balance + Masuk/Keluar
                setTextViewText(R.id.widget_balance, model.balanceText)
                setTextColor(R.id.widget_balance, colors.ink)
                setTextViewText(R.id.widget_income, model.incomeText)
                setTextColor(
                    R.id.widget_income,
                    if (model.hideBalance) colors.muted2 else colors.pos
                )
                setTextViewText(R.id.widget_outcome, model.outcomeText)
                setTextColor(
                    R.id.widget_outcome,
                    if (model.hideBalance) colors.muted2 else colors.error
                )

                // Band 3a: Recent transactions (up to 2)
                if (model.recentRows.isEmpty()) {
                    setViewVisibility(R.id.widget_tx_empty, View.VISIBLE)
                    setTextColor(R.id.widget_tx_empty, colors.muted2)
                    setViewVisibility(R.id.widget_tx_1, View.GONE)
                    setViewVisibility(R.id.widget_tx_2, View.GONE)
                } else {
                    setViewVisibility(R.id.widget_tx_empty, View.GONE)
                    bindTxRow(
                        views = this,
                        rowId = R.id.widget_tx_1,
                        noteId = R.id.widget_tx_1_note,
                        metaId = R.id.widget_tx_1_meta,
                        amtId = R.id.widget_tx_1_amt,
                        row = model.recentRows[0],
                        isTall = isTall,
                        hideBalance = model.hideBalance,
                        colors = colors
                    )
                    if (model.recentRows.size > 1) {
                        bindTxRow(
                            views = this,
                            rowId = R.id.widget_tx_2,
                            noteId = R.id.widget_tx_2_note,
                            metaId = R.id.widget_tx_2_meta,
                            amtId = R.id.widget_tx_2_amt,
                            row = model.recentRows[1],
                            isTall = isTall,
                            hideBalance = model.hideBalance,
                            colors = colors
                        )
                    } else {
                        setViewVisibility(R.id.widget_tx_2, View.GONE)
                    }
                }

                // Band 3b: Dual quick actions
                setInt(R.id.widget_out_bg, "setColorFilter", colors.outBtnBg)
                setInt(R.id.widget_out_stroke, "setColorFilter", colors.outBtnBorder)
                setTextColor(R.id.widget_out_text, colors.error)
                setInt(R.id.widget_in_bg, "setColorFilter", colors.inBtnBg)
                setTextColor(R.id.widget_in_text, colors.inBtnFg)

                // Click targets
                setOnClickPendingIntent(R.id.widget_root, openPi)
                setOnClickPendingIntent(R.id.widget_eye_btn, togglePi)
                setOnClickPendingIntent(R.id.widget_out_btn, outPi)
                setOnClickPendingIntent(R.id.widget_in_btn, inPi)
            }
        }

        private fun bindTxRow(
            views: RemoteViews,
            rowId: Int,
            noteId: Int,
            metaId: Int,
            amtId: Int,
            row: WidgetTxRow,
            isTall: Boolean,
            hideBalance: Boolean,
            colors: WidgetColors
        ) {
            views.setViewVisibility(rowId, View.VISIBLE)
            views.setTextViewText(noteId, row.note)
            views.setTextColor(noteId, colors.ink)
            if (isTall) {
                if (row.meta.isBlank()) {
                    views.setViewVisibility(metaId, View.GONE)
                } else {
                    views.setViewVisibility(metaId, View.VISIBLE)
                    views.setTextViewText(metaId, row.meta)
                    views.setTextColor(metaId, colors.muted2)
                }
            }
            views.setTextViewText(amtId, row.amountText)
            views.setTextColor(
                amtId,
                when {
                    hideBalance -> colors.muted2
                    row.amountKind > 0 -> colors.pos
                    row.amountKind < 0 -> colors.error
                    else -> colors.muted
                }
            )
        }
    }
}

internal data class WidgetColors(
    val cardBg: Int,
    val cardBorder: Int,
    val divider: Int,
    val ink: Int,
    val muted: Int,
    val muted2: Int,
    val pos: Int,
    val error: Int,
    val eyeBg: Int,
    val outBtnBg: Int,
    val outBtnBorder: Int,
    val inBtnBg: Int,
    val inBtnFg: Int
)

internal fun compositeOver(fg: Color, bg: Color): Int {
    val a = fg.alpha.coerceIn(0f, 1f)
    val inv = 1f - a
    return Color(
        red = fg.red * a + bg.red * inv,
        green = fg.green * a + bg.green * inv,
        blue = fg.blue * a + bg.blue * inv,
        alpha = 1f
    ).toArgb()
}

internal fun resolveWidgetColors(t: Tok): WidgetColors = WidgetColors(
    cardBg = t.sheet.toArgb(),
    cardBorder = compositeOver(t.hair, t.sheet),
    divider = compositeOver(t.hairSoft, t.sheet),
    ink = t.ink.toArgb(),
    muted = t.muted.toArgb(),
    muted2 = if (t.dark) t.muted.toArgb() else t.muted2.toArgb(),
    pos = t.pos.toArgb(),
    error = t.error.toArgb(),
    eyeBg = compositeOver(t.hairSoft, t.sheet),
    outBtnBg = compositeOver(t.error.copy(alpha = if (t.dark) 0.12f else 0.06f), t.sheet),
    outBtnBorder = compositeOver(t.hair, t.sheet),
    inBtnBg = t.accent.toArgb(),
    inBtnFg = if (t.dark) t.bg.toArgb() else android.graphics.Color.WHITE
)

internal data class WidgetTxRow(
    val note: String,
    val meta: String,
    val amountText: String,
    val amountKind: Int
)

internal data class WidgetModel(
    val monthLabel: String,
    val diffText: String,
    val diffKind: Int,
    val balanceText: String,
    val incomeText: String,
    val outcomeText: String,
    val recentRows: List<WidgetTxRow>,
    val hideBalance: Boolean
)

internal fun buildWidgetModel(saku: Saku, now: Calendar, maxRows: Int = 2): WidgetModel {
    val snap = saku.monthSnapshot(now)
    val bal = saku.balance()
    val hide = saku.hideBalance

    val monthLabel = Fmt.monthShort(now) + " " + now.get(Calendar.YEAR)
    val diffText = "Selisih " + if (hide) "\u2022\u2022\u2022\u2022" else Fmt.sign(snap.diff)
    val diffKind = when {
        snap.diff > 0L -> 1
        snap.diff < 0L -> -1
        else -> 0
    }
    val balanceText = Fmt.rpMasked(bal, hide)
    val incomeText = "Masuk " + if (hide) "\u2022\u2022\u2022\u2022" else Fmt.sign(snap.income)
    val outcomeText = "Keluar " + if (hide) "\u2022\u2022\u2022\u2022" else Fmt.out(snap.outcome)

    val recentSource = snap.rows.ifEmpty {
        saku.tx.sortedWith(compareByDescending<Tx> { it.date }.thenByDescending { it.time })
    }.take(maxRows)
    val recentRows = recentSource.map { tx ->
        val catName = saku.catOf(tx.cat)?.name
        val label = tx.note.ifBlank {
            catName ?: if (tx.type == "transfer") "Pindah saldo" else "Transaksi"
        }
        val dateStr = if (tx.date.length >= 10) Fmt.tgl(tx.date) else tx.date
        val metaParts = buildList {
            if (dateStr.isNotBlank()) add(dateStr)
            if (!catName.isNullOrBlank() && catName != label) add(catName)
        }
        val amountText = when {
            hide -> "\u2022\u2022\u2022\u2022"
            tx.type == "in" -> Fmt.sign(tx.amount)
            tx.type == "out" -> Fmt.out(tx.amount)
            else -> Fmt.num(tx.amount)
        }
        val amountKind = when (tx.type) {
            "in" -> 1
            "out" -> -1
            else -> 0
        }
        WidgetTxRow(
            note = label,
            meta = metaParts.joinToString(" \u00B7 "),
            amountText = amountText,
            amountKind = amountKind
        )
    }

    return WidgetModel(
        monthLabel = monthLabel,
        diffText = diffText,
        diffKind = diffKind,
        balanceText = balanceText,
        incomeText = incomeText,
        outcomeText = outcomeText,
        recentRows = recentRows,
        hideBalance = hide
    )
}

internal fun selectWidgetLayout(minHeightDp: Int): Int =
    if (minHeightDp >= 180) R.layout.widget_saku_tall else R.layout.widget_saku
