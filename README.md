# Tiger Matrix

تطبيق أندرويد أوفلاين (Kotlin + Jetpack Compose + Room + DataStore): لوحة تحكم، ملاحظات، مهام، تذكيرات بإشعارات،
مساعد محلي بالعربي (كتابة وصوت)، اتصال ورسائل بالاسم، أدوات، نسخ احتياطي، وويدجت.

## هيكلة الملفات (وين أعدّل شو؟)

| بدك تعدّل | الملف |
|---|---|
| كلمات أوامر المساعد | `assistant/CommandParser.kt` |
| فهم الوقت ("بكرا الساعة 5") | `assistant/TimeParser.kt` |
| ردود المساعد | `assistant/Responses.kt` |
| تنفيذ الأوامر والتأكيد/العدّاد | `viewmodel/AssistantViewModel.kt` |
| جهات الاتصال + الاتصال + SMS + واتساب | `actions/ContactResolver.kt`, `actions/PhoneActions.kt` |
| الإرسال التلقائي لواتساب (إمكانية الوصول) | `actions/WhatsAppAutoSendService.kt`, `actions/AutoSendBridge.kt` |
| فتح تطبيقات، منبه، فلاش | `system/SystemActions.kt` |
| التذكيرات والإشعارات | `reminders/` |
| الصوت (سمع ونطق) | `voice/` |
| النسخ الاحتياطي | `backup/BackupManager.kt` |
| الويدجت | `widget/QuickAddWidget.kt` + `res/layout/widget_quick_add.xml` |
| مستويات الصلاحية (اسألني/تلقائي/مطفي) | `data/ActionMode.kt`, `data/SettingsStore.kt`, `ui/screens/PermissionsScreen.kt` |
| الألوان والثيم | `ui/theme/Theme.kt` |
| شاشات التنقل السفلي | `ui/Destinations.kt` + `ui/App.kt` |
| الوحدات بالمحوّل | `tools/UnitConverter.kt` |
| جداول قاعدة البيانات | `data/Note.kt`, `data/TaskItem.kt`, `data/AppDatabase.kt` |
| الأذونات | `app/src/main/AndroidManifest.xml` |

## البناء
أي push على الفرع الرئيسي يشغّل GitHub Actions، والـ APK يطلع من تبويب Actions ← آخر Run ← Artifacts.

## ملاحظات
- تغيير جداول Room بعد ما يتثبت التطبيق يحتاج رفع `version` بـ `AppDatabase` وكتابة Migration، أو حذف التطبيق وتثبيته من جديد أثناء التطوير.
- ما في إذن INTERNET عمداً: أي ميزة أونلاين تنضاف بالتحديثات القادمة مع إذنها.
