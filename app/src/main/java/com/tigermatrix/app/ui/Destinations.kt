package com.tigermatrix.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

/** شاشات شريط التنقل السفلي. لإضافة شاشة: أضف عنصر هنا ثم composable في App.kt */
enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "الرئيسية", Icons.Default.Home),
    Notes("notes", "ملاحظات", Icons.Default.Edit),
    Tasks("tasks", "مهام", Icons.Default.CheckCircle),
    Assistant("assistant", "المساعد", Icons.Default.Face),
    Tools("tools", "أدوات", Icons.Default.Build);

    companion object {
        const val SETTINGS_ROUTE = "settings"
        const val PERMISSIONS_ROUTE = "permissions"
    }
}
