# 📝 ملاحظاتي — تطبيق ملاحظات فاخر (Android)

تطبيق ملاحظات بتصميم زجاجي فاخر، ورق مسطّر يتماشى مع الكتابة تمامًا،
حفظ تلقائي دائم، وتصدير الملاحظة كاملة كصورة PNG بجودة عالية.

---

## 📂 شكل الملفات بعد فك الضغط

```
AndroidManifest.xml
EditorActivity.kt
ExportUtils.kt
HtmlText.kt
MainActivity.kt
Note.kt
NoteStore.kt
RuledPaperView.kt
SettingsActivity.kt
Theme.kt
Ui.kt
build.gradle
settings.gradle
gradle.properties
README.md
.github/workflows/build-apk.yml   ← فولدر واحد بس (بيبني الـ APK تلقائيًا)
```

كل الملفات ورا بعض مباشرة ✅ — وفولدر واحد مخفي فقط `.github`
ودا **شرط من جيت هاب نفسها**: لو ملف البناء مش في المسار `.github/workflows/`
جيت هاب مش هتعرف تبني الـ APK خالص.

---

## 🚀 الطريقة 1 — الأسهل (سحب وإفلات)

1. **فك ضغط** `notes-app.zip`
2. افتح [github.com/new](https://github.com/new) → اسم المستودع `MyNotes` → اختر **Public** → **Create repository**
3. اضغط رابط **"uploading an existing file"**
4. ⚠️ **ادخل جوا فولدر فك الضغط** (ماتسحبش الفولدر نفسه!) وحدد **كل حاجة**: **Ctrl + A**
5. اسحبها كلها مرة واحدة لصفحة الرفع → **Commit changes**

📌 على **ويندوز**: فولدر `.github` ظاهر عادي ويتسحب مع الباقي.
📌 على **ماك**: اضغط `Cmd + Shift + .` الأول عشان يظهر، وبعدين حدد الكل واسحب.

---

## 🚀 الطريقة 2 — من غير أي فولدرات نهائيًا (مضمونة 100%)

لو سحب وإفلات الفولدر المخفي وقّع معاك، اعمل كده:

1. فك الضغط → ارفع **الملفات اللي ورا بعض فقط** بالسحب والإفلات (من غير فولدر `.github`) → **Commit changes**
2. في صفحة المستودع: **Add file ← Create new file**
3. في خانة اسم الملف اكتب **بالظبط**:
   ```
   .github/workflows/build-apk.yml
   ```
   ✨ لحظة ما تكتب `/` جيت هاب **بيعمل الفولدرات تلقائيًا** — مش هتلمسها بإيدك!
4. انسخ المحتوى ده والصقه في الملف:

```yaml
name: Build Android APK

on:
  push:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'

      - name: Build debug APK
        run: gradle assembleDebug --no-daemon

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: build/outputs/apk/debug/app-debug.apk
```

5. اضغط **Commit changes** — خلاص، كل حاجة جاهزة! ✅

---

## 📥 نزّل الـ APK

1. افتح تبويب **Actions** أعلى صفحة المستودع
2. هتلاقي بناء اسمه **"Build Android APK"** بيشتغل لوحده
3. انتظر **4–6 دقايق** لحد علامة ✓ الخضراء
4. اضغط على البناء → انزل لقسم **Artifacts** → نزّل **app-debug-apk**
5. فك ضغط الملف النازل → انقل `app-debug.apk` للموبايل → افتحه → اسمح بالتثبيت من مصادر غير معروفة
6. انتهى! 🎉 هتلاقي تطبيق **"ملاحظاتي"** على الموبايل

---

## ✨ مميزات التطبيق

- 🪟 تصميم زجاجي (Glassmorphism) مستوحى من iOS
- 📒 ورق مسطّر **يتماشى مع الكتابة حرفيًا** — السطور تحت النص تمامًا
- 💾 حفظ تلقائي دائم + مؤشر "تم الحفظ"
- 🖼️ تصدير الملاحظة كاملة (عنوان + محتوى + سطور) كصورة PNG بـ 3 جودات
- ↩️ تراجع / إعادة
- **B** / **I** / **U** / تظليل / ألوان / حجم خط / قوائم نقطية ورقمية
- 😀 لوحة رموز تعبيرية
- 📁 مجلدات + 📌 تثبيت + ⭐ مفضلة + 🗑️ مهملات
- 📄 4 أنماط ورق (سطور / أبيض / نقاط / مربعات) + ألوان ورق وسطور
- 🌙 وضع ليلي ☀️ ووضع نهاري
- 🔤 دعم كامل للعربية (RTL) والإنجليزية (LTR) تلقائيًا
- 📊 عداد كلمات + وقت قراءة + بحث فوري

---

## 🛠 ملاحظات

- المستودع لازم يكون **Public** عشان البناء المجاني غير محدود.
- لو تبويب Actions مش ظاهر بعد الرفع، يبقى ملف `.github/workflows/build-apk.yml`
  مش في أول المستودع — استخدم الطريقة 2 واعمله بالصق.
- أيقونة التطبيق دلوقتي هي الافتراضية بتاعة أندرويد — لو عايز أيقونة مخصصة
  (الورقة الصفراء ✨) قولي وهبعتلك إضافتها في خطوتين.
- بياناتك كلها على جهازك فقط — التطبيق مبيتصلش بالإنترنت.
