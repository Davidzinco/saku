package id.saku.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf

data class LaunchTxRequest(
    val id: Long = System.nanoTime(),
    val prefill: TxPrefill
)

class MainActivity : ComponentActivity() {
    private val pendingLaunch = mutableStateOf<LaunchTxRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            SakuApp(
                launchRequest = pendingLaunch.value,
                onConsumeLaunch = { pendingLaunch.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotifHelper.EXTRA_OPEN_SHEET, false) == true) {
            pendingLaunch.value = LaunchTxRequest(
                prefill = TxPrefill(
                    type = intent.getStringExtra(NotifHelper.EXTRA_TYPE) ?: "out",
                    amount = intent.getLongExtra(NotifHelper.EXTRA_AMOUNT, 0L),
                    note = intent.getStringExtra(NotifHelper.EXTRA_NOTE) ?: "",
                    accountId = intent.getStringExtra(NotifHelper.EXTRA_ACCOUNT) ?: ""
                )
            )
            intent.removeExtra(NotifHelper.EXTRA_OPEN_SHEET)
        }
    }
}
