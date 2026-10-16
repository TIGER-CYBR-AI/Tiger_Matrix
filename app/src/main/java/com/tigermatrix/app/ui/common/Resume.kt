package com.tigermatrix.app.ui.common

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** عدّاد بيزيد كل ما المستخدم يرجع للتطبيق (مثلاً من إعدادات النظام) لتحديث حالات الأذونات. */
@Composable
fun rememberResumeCounter(): Int {
    val context = LocalContext.current
    var counter by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) counter++
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    return counter
}
