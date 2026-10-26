
package com.smartautoclicker.app

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private lateinit var wordInput: EditText
    private lateinit var status: TextView
    private lateinit var intervalInput: EditText

    private val prefs by lazy {
        getSharedPreferences("settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(13, 17, 23)
        window.navigationBarColor = Color.rgb(13, 17, 23)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(Color.rgb(13, 17, 23))
        }

        fun addLabel(text: String, size: Float) {
            root.addView(TextView(this).apply {
                this.text = text
                textSize = size
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 12)
            })
        }

        fun addButton(text: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                this.text = text
                isAllCaps = false
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(-1, -2))
        }

        addLabel("النقر التلقائي", 26f)
        addLabel("اكتب الكلمة المطلوب البحث عنها", 16f)

        wordInput = EditText(this).apply {
            hint = "مثال: تخطي"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 18f
            setSingleLine(true)
            setText(prefs.getString("word", ""))
        }

        root.addView(wordInput)

        addLabel("الفاصل بين الضغطات (مللي ثانية، حتى 86400000)", 15f)
        intervalInput = EditText(this).apply {
            hint = "1000"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setSingleLine(true)
            setText(prefs.getLong("interval_ms", 1000L).toString())
        }
        root.addView(intervalInput)

        status = TextView(this).apply {
            text = "الحالة: متوقف"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 18, 0, 18)
        }

        addButton("حفظ الإعدادات") {
            if (saveWord()) {
                val raw = intervalInput.text.toString().trim()
                val interval = raw.toLongOrNull()
                if (interval == null || interval !in 0L..86_400_000L) {
                    intervalInput.error = "أدخل رقمًا من 0 إلى 86400000"
                } else {
                    prefs.edit().putLong("interval_ms", interval).apply()
                    status.text = "تم حفظ الكلمة والفاصل: ${interval} مللي ثانية"
                }
            }
        }

        addButton("إظهار نقطة الضغط العائمة") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                status.text = "اسمح بالظهور فوق التطبيقات، ثم ارجع واضغط الزر مرة أخرى"
            } else if (isServiceEnabled()) {
                sendBroadcast(Intent("${packageName}.SHOW_CLICK_MARKER"))
                status.text = "اسحب النقطة لمكان الضغط، واضغط عليها للتشغيل/الإيقاف"
            } else {
                status.text = "فعّل خدمة إمكانية الوصول أولًا"
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        addButton("إعدادات إمكانية الوصول") {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
        }

        addButton("تشغيل البحث") {
            if (!saveWord()) return@addButton

            if (isServiceEnabled()) {
                status.text = "الخدمة مفعّلة — البحث عن الكلمة جارٍ"
            } else {
                status.text =
                    "فعّل خدمة النقر التلقائي من إعدادات إمكانية الوصول"
                startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            }
        }

        addButton("إيقاف") {
            status.text =
                "لإيقاف النقر، عطّل خدمة النقر التلقائي من إعدادات إمكانية الوصول"
        }

        root.addView(status)

        setContentView(ScrollView(this).apply {
            addView(root)
        })
    }

    private fun saveWord(): Boolean {
        val word = wordInput.text.toString().trim()

        if (word.isEmpty()) {
            wordInput.error = "اكتب الكلمة أولًا"
            return false
        }

        prefs.edit().putString("word", word).apply()
        status.text = "تم حفظ الكلمة: $word"
        return true
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE)
                as android.view.accessibility.AccessibilityManager

        return manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
            it.resolveInfo.serviceInfo.name.endsWith("AutoClickService")
        }
    }
}