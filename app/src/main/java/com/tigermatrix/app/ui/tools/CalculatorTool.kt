package com.tigermatrix.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tigermatrix.app.tools.ExpressionEvaluator

private val keyRows = listOf(
    listOf("C", "(", ")", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "-"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "⌫", "=")
)
private val operatorKeys = setOf("÷", "×", "-", "+", "=")

@Composable
fun CalculatorTool() {
    var expr by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    // الأرقام والأزرار تبقى من اليسار لليمين حتى ما ينعكس ترتيب الأرقام
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        expr.ifEmpty { "0" },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2
                    )
                    Text(
                        result,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            keyRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> {
                                        expr = ""
                                        result = ""
                                    }
                                    "⌫" -> expr = expr.dropLast(1)
                                    "=" -> result = ExpressionEvaluator.evaluateToString(expr) ?: "خطأ"
                                    else -> expr += key
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = if (key in operatorKeys) ButtonDefaults.buttonColors()
                            else ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(key, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
