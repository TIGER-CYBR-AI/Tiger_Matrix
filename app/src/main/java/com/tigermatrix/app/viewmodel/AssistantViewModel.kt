package com.tigermatrix.app.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tigermatrix.app.actions.AccessibilityStatus
import com.tigermatrix.app.actions.ActionLog
import com.tigermatrix.app.actions.AutoSendBridge
import com.tigermatrix.app.actions.ContactEntry
import com.tigermatrix.app.actions.ContactResolver
import com.tigermatrix.app.actions.PhoneActions
import com.tigermatrix.app.assistant.ArabicText
import com.tigermatrix.app.assistant.Command
import com.tigermatrix.app.assistant.CommandParser
import com.tigermatrix.app.assistant.Responses
import com.tigermatrix.app.assistant.TimeFormat
import com.tigermatrix.app.data.ActionMode
import com.tigermatrix.app.data.AppDatabase
import com.tigermatrix.app.data.Note
import com.tigermatrix.app.data.SettingsStore
import com.tigermatrix.app.data.TaskItem
import com.tigermatrix.app.reminders.Notifier
import com.tigermatrix.app.reminders.ReminderScheduler
import com.tigermatrix.app.system.SystemActions
import com.tigermatrix.app.tools.ExpressionEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(val text: String, val fromUser: Boolean)

/** إجراء بانتظار تأكيدك (اسألني) أو بعد عدّاد تنازلي (تلقائي). */
data class PendingUi(val title: String, val detail: String, val secondsLeft: Int?)

/** طلب إذن من النظام؛ الشاشة بتنفّذه ولما يخلص بنعيد تنفيذ نفس الأمر. */
data class PermissionRequest(val permission: String, val retryText: String, val id: Long)

private sealed class Undo {
    class TaskAdded(val id: Long) : Undo()
    class NoteAdded(val id: Long) : Undo()
    class TaskDone(val task: TaskItem) : Undo()
    class TaskDeleted(val task: TaskItem) : Undo()
    class NoteDeleted(val note: Note) : Undo()
}

private sealed class Resolved {
    class Found(val name: String, val number: String) : Resolved()
    class Problem(val text: String) : Resolved()
}

private const val AUTO_SECONDS = 10

class AssistantViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Application = app
    private val db = AppDatabase.get(app)
    private val settingsStore = SettingsStore(app)

    private val _messages = MutableStateFlow(listOf(ChatMessage(Responses.WELCOME, false)))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pending = MutableStateFlow<PendingUi?>(null)
    val pending: StateFlow<PendingUi?> = _pending.asStateFlow()

    private val _permissionRequest = MutableStateFlow<PermissionRequest?>(null)
    val permissionRequest: StateFlow<PermissionRequest?> = _permissionRequest.asStateFlow()

    private val undoStack = ArrayDeque<Undo>()
    private val deniedPermissions = mutableSetOf<String>()
    private var pendingRun: (suspend () -> String)? = null
    private var countdownJob: Job? = null
    private var requestCounter = 0L
    private var currentInput = ""

    // ---------------------------------------------------------------- إدخال

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        _messages.update { it + ChatMessage(t, true) }
        process(t)
    }

    private fun process(text: String) {
        currentInput = text
        viewModelScope.launch {
            val reply = try {
                execute(CommandParser.parse(text))
            } catch (e: Exception) {
                Responses.ERROR
            }
            if (reply.isNotBlank()) addAssistant(reply)
        }
    }

    private fun addAssistant(text: String) {
        _messages.update { it + ChatMessage(text, false) }
    }

    // ---------------------------------------------------------------- أذونات

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    /** true = نكمل. false = فتحنا طلب إذن وننتظر النتيجة (بعدها بنعيد الأمر). */
    private fun ensurePermission(permission: String): Boolean {
        if (hasPermission(permission) || permission in deniedPermissions) return true
        _permissionRequest.value = PermissionRequest(permission, currentInput, ++requestCounter)
        return false
    }

    fun onPermissionResult(granted: Boolean) {
        val req = _permissionRequest.value ?: return
        _permissionRequest.value = null
        if (!granted) deniedPermissions.add(req.permission)
        process(req.retryText)
    }

    // ---------------------------------------------------------------- إجراءات معلّقة

    private fun startPending(title: String, detail: String, auto: Boolean, run: suspend () -> String) {
        clearPending()
        pendingRun = run
        if (auto) {
            _pending.value = PendingUi(title, detail, AUTO_SECONDS)
            countdownJob = viewModelScope.launch {
                var left = AUTO_SECONDS
                while (left > 0) {
                    delay(1000)
                    left--
                    _pending.value = PendingUi(title, detail, left)
                }
                runPending()
            }
        } else {
            _pending.value = PendingUi(title, detail, null)
        }
    }

    private fun clearPending() {
        countdownJob?.cancel()
        countdownJob = null
        pendingRun = null
        _pending.value = null
    }

    private suspend fun runPending() {
        val run = pendingRun
        pendingRun = null
        countdownJob = null
        _pending.value = null
        if (run == null) return
        val reply = try {
            run()
        } catch (e: Exception) {
            Responses.ERROR
        }
        addAssistant(reply)
    }

    fun confirmPending() {
        if (pendingRun == null) return
        countdownJob?.cancel()
        viewModelScope.launch { runPending() }
    }

    fun cancelPending() {
        if (pendingRun == null) return
        clearPending()
        addAssistant(Responses.CANCELLED)
    }

    private fun startAction(
        mode: ActionMode,
        title: String,
        detail: String,
        askText: String,
        autoText: String,
        run: suspend () -> String
    ): String {
        val auto = mode == ActionMode.AUTO
        startPending(title, detail, auto, run)
        return if (auto) autoText else askText
    }

    // ---------------------------------------------------------------- تنفيذ الأوامر

    private fun pushUndo(u: Undo) {
        undoStack.addLast(u)
        while (undoStack.size > 10) undoStack.removeFirst()
    }

    private fun matchTitle(title: String, query: String): Boolean {
        val q = ArabicText.normalize(ArabicText.stripDiacritics(query)).trim()
        if (q.isEmpty()) return false
        return ArabicText.normalize(title).contains(q)
    }

    private fun bullets(items: List<String>): String =
        items.take(5).joinToString("\n") { "• $it" }

    private suspend fun execute(cmd: Command): String = when (cmd) {
        is Command.AddTask -> addTask(cmd)
        is Command.AddReminder -> addReminder(cmd)
        is Command.AddNote -> addNote(cmd)
        is Command.CompleteTask -> completeTask(cmd.query)
        is Command.DeleteTask -> deleteTask(cmd.query)
        is Command.DeleteNote -> deleteNote(cmd.query)
        is Command.Search -> search(cmd.query)
        is Command.Calc -> {
            val r = ExpressionEvaluator.evaluateToString(cmd.expr)
            if (r == null) Responses.CALC_FAILED else "${cmd.expr.trim()} = $r"
        }
        is Command.OpenApp -> SystemActions.openApp(ctx, cmd.name)
        is Command.SetAlarm -> SystemActions.setAlarm(ctx, cmd.hour, cmd.minute, cmd.label)
        is Command.Flashlight -> SystemActions.setFlashlight(ctx, cmd.on)
        is Command.Call -> handleCall(cmd.target)
        is Command.SendSms -> handleSms(cmd.rest)
        is Command.SendWhatsApp -> handleWhatsApp(cmd.rest)
        is Command.Reply -> cmd.text
        Command.ListTasks -> {
            val tasks = db.taskDao().getPending()
            if (tasks.isEmpty()) Responses.NO_TASKS
            else "مهامك المعلقة (${tasks.size}):\n" + bullets(tasks.map { it.title })
        }
        Command.ListNotes -> {
            val notes = db.noteDao().getRecent(5)
            if (notes.isEmpty()) Responses.NO_NOTES
            else "آخر ملاحظاتك:\n" + bullets(notes.map { it.title })
        }
        Command.ListReminders -> {
            val list = db.taskDao().getReminders()
            if (list.isEmpty()) Responses.NO_REMINDERS
            else "تذكيراتك:\n" + list.take(8).joinToString("\n") {
                "• ${it.title} — ${TimeFormat.describe(it.dueAt ?: 0L)}"
            }
        }
        Command.Undo -> undo()
        Command.TimeNow -> "الساعة هلق ${TimeFormat.clock(System.currentTimeMillis())}"
        Command.DateToday -> "اليوم ${TimeFormat.dateFull(System.currentTimeMillis())}"
        Command.Help -> Responses.HELP
        Command.Unknown -> Responses.UNKNOWN
    }

    private suspend fun addTask(cmd: Command.AddTask): String {
        if (cmd.title.isBlank()) return Responses.TASK_MISSING
        val id = db.taskDao().insert(TaskItem(title = cmd.title))
        pushUndo(Undo.TaskAdded(id))
        val hint = if (cmd.needTimeHint) "\n\n${Responses.TASK_NO_TIME_HINT}" else ""
        return "تمت إضافة المهمة:\n${cmd.title}$hint"
    }

    private suspend fun addReminder(cmd: Command.AddReminder): String {
        val id = db.taskDao().insert(TaskItem(title = cmd.title, dueAt = cmd.triggerAt))
        ReminderScheduler.schedule(ctx, id, cmd.triggerAt)
        pushUndo(Undo.TaskAdded(id))
        var reply = "تمام، رح أذكرك:\n${cmd.title}\n${TimeFormat.describe(cmd.triggerAt)}"
        if (!Notifier.canNotify(ctx)) reply += "\n\n${Responses.NOTIFICATIONS_OFF}"
        return reply
    }

    private suspend fun addNote(cmd: Command.AddNote): String {
        if (cmd.text.isBlank()) return Responses.NOTE_MISSING
        val id = db.noteDao().insert(Note(title = cmd.text.lines().first().take(40), content = cmd.text))
        pushUndo(Undo.NoteAdded(id))
        return "تم حفظ الملاحظة."
    }

    private suspend fun completeTask(query: String): String {
        val found = db.taskDao().getPending().filter { matchTitle(it.title, query) }
        if (found.isEmpty()) return "ما لقيت مهمة معلقة فيها «$query»."
        if (found.size > 1) {
            return "لقيت أكتر من مهمة:\n" + bullets(found.map { it.title }) + "\n\nحدد أكتر."
        }
        val task = found[0]
        db.taskDao().update(task.copy(done = true))
        ReminderScheduler.cancel(ctx, task.id)
        Notifier.cancel(ctx, task.id)
        pushUndo(Undo.TaskDone(task))
        return "خلّصت المهمة: ${task.title}"
    }

    private suspend fun deleteTask(query: String): String {
        val found = db.taskDao().getAll().filter { matchTitle(it.title, query) }
        if (found.isEmpty()) return "ما لقيت مهمة فيها «$query»."
        if (found.size > 1) {
            return "لقيت أكتر من مهمة:\n" + bullets(found.map { it.title }) + "\n\nحدد أكتر."
        }
        val task = found[0]
        db.taskDao().delete(task)
        ReminderScheduler.cancel(ctx, task.id)
        Notifier.cancel(ctx, task.id)
        pushUndo(Undo.TaskDeleted(task))
        return "حذفت المهمة: ${task.title}\n(اكتب «تراجع» إذا غلطت)"
    }

    private suspend fun deleteNote(query: String): String {
        val found = db.noteDao().getAll().filter {
            matchTitle(it.title, query) || matchTitle(it.content, query)
        }
        if (found.isEmpty()) return "ما لقيت ملاحظة فيها «$query»."
        if (found.size > 1) {
            return "لقيت أكتر من ملاحظة:\n" + bullets(found.map { it.title }) + "\n\nحدد أكتر."
        }
        val note = found[0]
        db.noteDao().delete(note)
        pushUndo(Undo.NoteDeleted(note))
        return "حذفت الملاحظة: ${note.title}\n(اكتب «تراجع» إذا غلطت)"
    }

    private suspend fun search(query: String): String {
        if (query.isBlank()) return Responses.SEARCH_MISSING
        val notes = db.noteDao().search(query)
        val tasks = db.taskDao().search(query)
        if (notes.isEmpty() && tasks.isEmpty()) return Responses.NO_RESULTS
        return buildString {
            if (notes.isNotEmpty()) {
                append("ملاحظات:\n")
                append(bullets(notes.map { it.title }))
            }
            if (tasks.isNotEmpty()) {
                if (notes.isNotEmpty()) append("\n\n")
                append("مهام:\n")
                append(tasks.take(5).joinToString("\n") { (if (it.done) "✓ " else "• ") + it.title })
            }
        }
    }

    private suspend fun undo(): String {
        val u = undoStack.removeLastOrNull() ?: return Responses.NOTHING_TO_UNDO
        return when (u) {
            is Undo.TaskAdded -> {
                db.taskDao().deleteById(u.id)
                ReminderScheduler.cancel(ctx, u.id)
                Notifier.cancel(ctx, u.id)
                "تراجعت عن إضافة المهمة."
            }
            is Undo.NoteAdded -> {
                db.noteDao().deleteById(u.id)
                "تراجعت عن إضافة الملاحظة."
            }
            is Undo.TaskDone -> {
                db.taskDao().update(u.task)
                val due = u.task.dueAt
                if (due != null && due > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(ctx, u.task.id, due)
                }
                "رجّعت المهمة «${u.task.title}» كغير منجزة."
            }
            is Undo.TaskDeleted -> {
                db.taskDao().insert(u.task)
                val due = u.task.dueAt
                if (!u.task.done && due != null && due > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(ctx, u.task.id, due)
                }
                "رجّعت المهمة «${u.task.title}»."
            }
            is Undo.NoteDeleted -> {
                db.noteDao().insert(u.note)
                "رجّعت الملاحظة «${u.note.title}»."
            }
        }
    }

    // ---------------------------------------------------------------- التواصل (اتصال / SMS / واتساب)

    private suspend fun loadContacts(): List<ContactEntry> =
        if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            withContext(Dispatchers.IO) { ContactResolver.loadAll(ctx) }
        } else {
            emptyList()
        }

    private fun resolveName(query: String, all: List<ContactEntry>): Resolved {
        if (PhoneActions.looksLikeNumber(query)) return Resolved.Found(query, query)
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            return if (ensurePermission(Manifest.permission.READ_CONTACTS)) {
                Resolved.Problem(Responses.NO_CONTACTS_ACCESS)
            } else {
                Resolved.Problem(Responses.PERMISSION_ASKING)
            }
        }
        val matches = ContactResolver.search(all, query)
        if (matches.isEmpty()) return Resolved.Problem("ما لقيت جهة اتصال باسم «$query».")
        val names = matches.map { it.name }.distinct()
        if (names.size > 1) {
            return Resolved.Problem(
                "لقيت أكتر من جهة اتصال:\n" + bullets(names) + "\n\nحدد الاسم أكتر."
            )
        }
        val best = ContactResolver.bestNumber(matches)
            ?: return Resolved.Problem("ما لقيت رقم لهالجهة.")
        return Resolved.Found(best.name, best.number)
    }

    private suspend fun handleCall(target: String): String {
        val cfg = settingsStore.actionSettings.first()
        if (cfg.callMode == ActionMode.OFF) return Responses.off("المكالمات")

        val all = loadContacts()
        val found = when (val r = resolveName(target.trim(), all)) {
            is Resolved.Problem -> return r.text
            is Resolved.Found -> r
        }
        if (!ensurePermission(Manifest.permission.CALL_PHONE)) return Responses.PERMISSION_ASKING

        val name = found.name
        val number = found.number
        return startAction(
            mode = cfg.callMode,
            title = "اتصال بـ $name",
            detail = number,
            askText = "جاهز أتصل بـ $name ($number). اضغط «تأكيد».",
            autoText = "رح أتصل بـ $name ($number) خلال $AUTO_SECONDS ثواني. اضغط «إلغاء» إذا غيّرت رأيك."
        ) {
            val ok = PhoneActions.call(ctx, number, PhoneActions.canCallDirect(ctx))
            ActionLog.append(ctx, "اتصال → $name")
            if (ok) "عم أتصل بـ $name." else Responses.ACTION_FAILED
        }
    }

    private suspend fun handleSms(rest: String): String {
        val cfg = settingsStore.actionSettings.first()
        if (cfg.smsMode == ActionMode.OFF) return Responses.off("رسائل SMS")

        val all = loadContacts()
        val (nameQuery, message) = ContactResolver.splitNameAndMessage(all, rest)
        if (nameQuery.isBlank()) return Responses.RECIPIENT_MISSING
        val found = when (val r = resolveName(nameQuery, all)) {
            is Resolved.Problem -> return r.text
            is Resolved.Found -> r
        }
        if (message.isBlank()) return Responses.MESSAGE_MISSING
        if (!ensurePermission(Manifest.permission.SEND_SMS)) return Responses.PERMISSION_ASKING

        val name = found.name
        val number = found.number
        return startAction(
            mode = cfg.smsMode,
            title = "رسالة SMS إلى $name",
            detail = message,
            askText = "جاهز أرسل SMS لـ $name:\n«$message»\nاضغط «تأكيد».",
            autoText = "رح أرسل SMS لـ $name خلال $AUTO_SECONDS ثواني:\n«$message»\nاضغط «إلغاء» إذا غيّرت رأيك."
        ) {
            val direct = PhoneActions.canSendSms(ctx)
            val ok = if (direct) {
                PhoneActions.sendSms(ctx, number, message)
            } else {
                PhoneActions.composeSms(ctx, number, message)
            }
            ActionLog.append(ctx, "SMS → $name" + if (direct) "" else " (مسودة)")
            when {
                !ok -> Responses.ACTION_FAILED
                direct -> "أرسلت الرسالة لـ $name."
                else -> "فتحت تطبيق الرسائل والنص جاهز، اضغط إرسال."
            }
        }
    }

    private fun openWhatsAppDraft(pkg: String, number: String, name: String, message: String): String {
        val ok = PhoneActions.openWhatsAppChat(ctx, pkg, number, message)
        ActionLog.append(ctx, "واتساب (مسودة) → $name")
        return if (ok) "فتحت واتساب عند $name والرسالة جاهزة. اضغط إرسال." else Responses.ACTION_FAILED
    }

    private suspend fun handleWhatsApp(rest: String): String {
        val cfg = settingsStore.actionSettings.first()
        if (cfg.whatsappMode == ActionMode.OFF) return Responses.off("واتساب")

        val pkg = PhoneActions.pickWhatsApp(ctx, cfg.whatsappApp) ?: return Responses.WA_NOT_INSTALLED
        val all = loadContacts()
        val (nameQuery, message) = ContactResolver.splitNameAndMessage(all, rest)
        if (nameQuery.isBlank()) return Responses.RECIPIENT_MISSING
        val found = when (val r = resolveName(nameQuery, all)) {
            is Resolved.Problem -> return r.text
            is Resolved.Found -> r
        }
        if (message.isBlank()) return Responses.MESSAGE_MISSING

        val name = found.name
        val number = PhoneActions.normalizeForWhatsApp(found.number, cfg.countryCode)

        if (cfg.whatsappMode == ActionMode.ASK) {
            return openWhatsAppDraft(pkg, number, name, message)
        }

        // الوضع التلقائي: بنتأكد من الشروط، وإذا ناقصة بنرجع لوضع المسودة
        val allowed = if (pkg == PhoneActions.PKG_BUSINESS) cfg.allowBusiness else cfg.allowWhatsApp
        if (!AccessibilityStatus.isServiceEnabled(ctx)) {
            return openWhatsAppDraft(pkg, number, name, message) +
                "\n\nالإرسال التلقائي بدو خدمة إمكانية الوصول وهي مو مفعّلة (الإعدادات ← مركز الصلاحيات)، فجهّزتلك الرسالة بس."
        }
        if (!allowed) {
            return openWhatsAppDraft(pkg, number, name, message) +
                "\n\nما سمحت بالإرسال التلقائي لهالتطبيق (الخطوة 2 بمركز الصلاحيات)."
        }
        if (!AutoSendBridge.canArmNow()) {
            return openWhatsAppDraft(pkg, number, name, message) +
                "\n\nبعتّ رسالة من ثواني، فتركتلك هي جاهزة (حماية من الإرسال المتكرر)."
        }

        return startAction(
            mode = ActionMode.AUTO,
            title = "واتساب إلى $name",
            detail = message,
            askText = "",
            autoText = "رح أرسل لـ $name خلال $AUTO_SECONDS ثواني:\n«$message»\nاضغط «إلغاء» إذا غيّرت رأيك."
        ) {
            AutoSendBridge.arm(pkg)
            val ok = PhoneActions.openWhatsAppChat(ctx, pkg, number, message)
            ActionLog.append(ctx, "واتساب (تلقائي) → $name")
            if (ok) {
                "عم أرسل الرسالة لـ $name عبر واتساب."
            } else {
                AutoSendBridge.disarm()
                Responses.ACTION_FAILED
            }
        }
    }
}
