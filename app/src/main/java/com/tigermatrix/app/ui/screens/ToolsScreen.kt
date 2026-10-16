package com.tigermatrix.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tigermatrix.app.ui.common.ScreenHeader
import com.tigermatrix.app.ui.tools.CalculatorTool
import com.tigermatrix.app.ui.tools.ConverterTool
import com.tigermatrix.app.ui.tools.TextTool

/** لإضافة أداة جديدة: اكتب composable بمجلد ui/tools وأضف اسمها هنا. */
private val toolNames = listOf("حاسبة", "محوّل", "نصوص")

@Composable
fun ToolsScreen() {
    var selected by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الأدوات")
        TabRow(selectedTabIndex = selected) {
            toolNames.forEachIndexed { i, name ->
                Tab(selected = selected == i, onClick = { selected = i }, text = { Text(name) })
            }
        }
        when (selected) {
            0 -> CalculatorTool()
            1 -> ConverterTool()
            else -> TextTool()
        }
    }
}
