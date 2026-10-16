package com.tigermatrix.app.tools

data class UnitDef(val name: String, val toBase: Double)
data class UnitCategory(val name: String, val units: List<UnitDef>)

/** لإضافة وحدة أو فئة جديدة: عدّل القائمة بالأسفل فقط (toBase = كم وحدة أساس بالوحدة الواحدة). */
object UnitConverter {
    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            "الطول",
            listOf(
                UnitDef("متر", 1.0),
                UnitDef("كيلومتر", 1000.0),
                UnitDef("سنتيمتر", 0.01),
                UnitDef("ميليمتر", 0.001),
                UnitDef("ميل", 1609.344),
                UnitDef("قدم", 0.3048),
                UnitDef("إنش", 0.0254)
            )
        ),
        UnitCategory(
            "الوزن",
            listOf(
                UnitDef("كيلوغرام", 1.0),
                UnitDef("غرام", 0.001),
                UnitDef("طن", 1000.0),
                UnitDef("رطل", 0.45359237),
                UnitDef("أونصة", 0.028349523125)
            )
        ),
        UnitCategory(
            "الحجم",
            listOf(
                UnitDef("لتر", 1.0),
                UnitDef("مليلتر", 0.001),
                UnitDef("غالون", 3.785411784),
                UnitDef("كوب", 0.2365882365)
            )
        ),
        UnitCategory(
            "البيانات",
            listOf(
                UnitDef("بايت", 1.0),
                UnitDef("كيلوبايت", 1024.0),
                UnitDef("ميغابايت", 1048576.0),
                UnitDef("غيغابايت", 1073741824.0)
            )
        )
    )

    fun convert(value: Double, from: UnitDef, to: UnitDef): Double =
        value * from.toBase / to.toBase
}
