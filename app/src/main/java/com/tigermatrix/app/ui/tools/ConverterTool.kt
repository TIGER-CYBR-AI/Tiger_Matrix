@file:OptIn(ExperimentalMaterial3Api::class)

package com.tigermatrix.app.ui.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tigermatrix.app.tools.ExpressionEvaluator
import com.tigermatrix.app.tools.UnitConverter
import com.tigermatrix.app.tools.UnitDef

@Composable
fun ConverterTool() {
    var catIndex by remember { mutableIntStateOf(0) }
    val cat = UnitConverter.categories[catIndex]
    var fromIdx by remember(catIndex) { mutableIntStateOf(0) }
    var toIdx by remember(catIndex) { mutableIntStateOf(1) }
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UnitConverter.categories.forEachIndexed { i, c ->
                FilterChip(selected = i == catIndex, onClick = { catIndex = i }, label = { Text(c.name) })
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("القيمة") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        UnitDropdown("من", cat.units, fromIdx) { fromIdx = it }
        UnitDropdown("إلى", cat.units, toIdx) { toIdx = it }

        val v = ExpressionEvaluator.normalizeDigits(input).toDoubleOrNull()
        if (v != null) {
            val from = cat.units[fromIdx]
            val to = cat.units[toIdx]
            val r = UnitConverter.convert(v, from, to)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${ExpressionEvaluator.format(v)} ${from.name} = ${ExpressionEvaluator.format(r)} ${to.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UnitDropdown(label: String, units: List<UnitDef>, selected: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(48.dp))
        Box {
            OutlinedButton(onClick = { open = true }) { Text(units[selected].name) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                units.forEachIndexed { i, u ->
                    DropdownMenuItem(
                        text = { Text(u.name) },
                        onClick = {
                            onSelect(i)
                            open = false
                        }
                    )
                }
            }
        }
    }
}
