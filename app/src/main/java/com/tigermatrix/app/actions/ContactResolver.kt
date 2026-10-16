package com.tigermatrix.app.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tigermatrix.app.assistant.ArabicText

data class ContactEntry(val name: String, val number: String, val type: Int)

object ContactResolver {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun loadAll(context: Context): List<ContactEntry> {
        if (!hasPermission(context)) return emptyList()
        val result = ArrayList<ContactEntry>()
        try {
            val phone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            )
            context.contentResolver.query(phone, projection, null, null, null)?.use { c ->
                val iName = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val iNum = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val iType = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                if (iName < 0 || iNum < 0) return@use
                while (c.moveToNext()) {
                    val name = c.getString(iName) ?: continue
                    val num = c.getString(iNum) ?: continue
                    val type = if (iType >= 0) c.getInt(iType) else 0
                    result.add(ContactEntry(name, num, type))
                }
            }
        } catch (e: Exception) {
            // تجاهل
        }
        return result
    }

    private fun norm(s: String): String = ArabicText.normalize(ArabicText.stripDiacritics(s)).trim()

    /** تطابق تام بالاسم أولاً، وإذا ما في بنبحث بالاحتواء. */
    fun search(all: List<ContactEntry>, query: String): List<ContactEntry> {
        val q = norm(query)
        if (q.isEmpty()) return emptyList()
        val exact = all.filter { norm(it.name) == q }
        if (exact.isNotEmpty()) return exact
        return all.filter { norm(it.name).contains(q) }
    }

    /** يختار أفضل رقم لنفس الجهة (الموبايل أولاً). */
    fun bestNumber(entries: List<ContactEntry>): ContactEntry? =
        entries.firstOrNull { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE } ?: entries.firstOrNull()

    /**
     * يفصل "احمد تعال بكرا" إلى (احمد، تعال بكرا).
     * الأفضلية: فاصل صريح (: أو ،) ثم أطول اسم يطابق جهة اتصال (حتى 3 كلمات) ثم أول كلمة.
     */
    fun splitNameAndMessage(all: List<ContactEntry>, rest: String): Pair<String, String> {
        val sep = rest.indexOfAny(charArrayOf(':', '،'))
        if (sep > 0) {
            return Pair(rest.substring(0, sep).trim(), rest.substring(sep + 1).trim())
        }
        val tokens = rest.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        if (tokens.isEmpty()) return Pair("", "")
        if (tokens.size >= 2 && (tokens[0] == "ل" || tokens[0] == "الى" || tokens[0] == "إلى")) {
            tokens.removeAt(0)
        }
        for (n in minOf(3, tokens.size) downTo 1) {
            val cand = tokens.take(n).joinToString(" ")
            val variants = ArrayList<String>()
            variants.add(cand)
            if (cand.length > 2 && cand.startsWith("ل")) variants.add(cand.substring(1))
            for (v in variants) {
                if (search(all, v).isNotEmpty()) {
                    return Pair(v, tokens.drop(n).joinToString(" "))
                }
            }
        }
        var first = tokens[0]
        if (first.length > 2 && first.startsWith("ل") && search(all, first).isEmpty()) {
            first = first.substring(1)
        }
        return Pair(first, tokens.drop(1).joinToString(" "))
    }
}
