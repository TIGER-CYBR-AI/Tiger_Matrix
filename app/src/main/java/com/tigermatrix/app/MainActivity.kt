package com.tigermatrix.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tigermatrix.app.ui.TigerMatrixApp

class MainActivity : ComponentActivity() {

    /** إجراء جاي من الويدجت (إضافة مهمة/ملاحظة أو فتح المساعد) */
    private var launchAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchAction = intent?.getStringExtra(EXTRA_ACTION)
        setContent {
            TigerMatrixApp(
                launchAction = launchAction,
                onLaunchActionHandled = { launchAction = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchAction = intent.getStringExtra(EXTRA_ACTION)
    }

    companion object {
        const val EXTRA_ACTION = "tm_action"
        const val ACTION_ADD_TASK = "add_task"
        const val ACTION_ADD_NOTE = "add_note"
        const val ACTION_ASSISTANT = "assistant"
    }
}
