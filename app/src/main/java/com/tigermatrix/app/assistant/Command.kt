package com.tigermatrix.app.assistant

/** الأوامر اللي يفهمها المساعد المحلي. لإضافة أمر: هنا ثم CommandParser ثم AssistantViewModel. */
sealed class Command {
    data class AddTask(val title: String, val needTimeHint: Boolean = false) : Command()
    data class AddReminder(val title: String, val triggerAt: Long) : Command()
    data class AddNote(val text: String) : Command()
    data class CompleteTask(val query: String) : Command()
    data class DeleteTask(val query: String) : Command()
    data class DeleteNote(val query: String) : Command()
    data class Search(val query: String) : Command()
    data class Calc(val expr: String) : Command()
    data class OpenApp(val name: String) : Command()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : Command()
    data class Flashlight(val on: Boolean) : Command()
    data class Call(val target: String) : Command()
    data class SendSms(val rest: String) : Command()
    data class SendWhatsApp(val rest: String) : Command()
    data class Reply(val text: String) : Command()
    object ListTasks : Command()
    object ListNotes : Command()
    object ListReminders : Command()
    object Undo : Command()
    object TimeNow : Command()
    object DateToday : Command()
    object Help : Command()
    object Unknown : Command()
}
