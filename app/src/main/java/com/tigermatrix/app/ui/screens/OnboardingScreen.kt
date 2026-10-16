package com.tigermatrix.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tigermatrix.app.ui.common.ReminderSetup

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "أهلاً بك في Tiger Matrix",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("مساعدك المحلي بالعربي: ملاحظات، مهام، تذكيرات، وأدوات. كل بياناتك بتضل على جهازك.")

            FeatureLine("🎤", "احكي أوامرك أو اكتبها: «ذكرني بكرا الساعة 5 أشتري خبز»")
            FeatureLine("⏰", "تذكيرات بإشعارات حتى لو التطبيق مسكّر")
            FeatureLine("📞", "اتصال وإرسال رسائل بالاسم، وأنت اللي بتتحكم بالصلاحيات")
            FeatureLine("💾", "نسخ احتياطي واستعادة بملف")

            Text(
                "عشان التذكيرات توصلك:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            ReminderSetup()

            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("ابدأ") }
        }
    }
}

@Composable
private fun FeatureLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(icon, fontSize = 24.sp)
        Text(text, modifier = Modifier.weight(1f))
    }
}
