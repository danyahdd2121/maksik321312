package com.example.openassistant

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var bubble: TextView
    private lateinit var mascot: TextView
    private lateinit var wakeBtn: Button
    private lateinit var cookie: TextView
    private lateinit var zzz: TextView
    private var asleep = false
    private var tts: TextToSpeech? = null
    private val idle = mutableListOf<Animator>()
    private var pendingBt: Boolean? = null

    private data class App(val label: String, val pkg: String)

    private val appAliases = mapOf(
        "ютуб" to "youtube", "телеграм" to "telegram", "телега" to "telegram",
        "ватсап" to "whatsapp", "вацап" to "whatsapp", "вк" to "vk",
        "контакт" to "vk", "хром" to "chrome", "браузер" to "chrome",
        "камера" to "camera", "настройки" to "settings", "почта" to "gmail",
        "карты" to "maps", "гугл" to "google", "инстаграм" to "instagram",
        "тикток" to "tiktok", "спотифай" to "spotify", "нетфликс" to "netflix",
        "калькулятор" to "calculator", "часы" to "clock", "галерея" to "gallery",
        "телефон" to "phone", "сообщения" to "messages", "плей маркет" to "play store"
    )

    private val siteAliases = mapOf(
        "ютуб" to "youtube.com", "гугл" to "google.com", "вк" to "vk.com",
        "яндекс" to "yandex.ru", "википедия" to "wikipedia.org",
        "гитхаб" to "github.com", "телеграм" to "web.telegram.org",
        "почта" to "mail.google.com", "карты" to "maps.google.com"
    )

    private val fillers = setOf(
        "пожалуйста", "открой", "открыть", "запусти", "запустить", "включи",
        "приложение", "программу", "open", "launch", "start", "app", "зайди", "на"
    )

    private val wakeWords = setOf("максик", "привет", "хей", "hey", "хэй", "эй", "хай")
    private val searchWords = setOf("найди", "загугли", "поищи", "гугли", "погугли")
    private val sleepWords = setOf("спать", "спи", "засыпай", "спокойной", "баиньки")

    private val weatherStop = setOf(
        "в", "во", "на", "сегодня", "сейчас", "какая", "какой", "скажи", "мне", "там",
        "у", "нас", "городе", "покажи", "расскажи", "подскажи", "узнай", "что", "пожалуйста"
    )

    private val numWords = mapOf(
        "ноль" to 0, "десять" to 10, "двадцать" to 20, "тридцать" to 30, "сорок" to 40,
        "пятьдесят" to 50, "шестьдесят" to 60, "семьдесят" to 70, "восемьдесят" to 80,
        "девяносто" to 90, "сто" to 100, "половину" to 50
    )

    private val happy = listOf(
        "Гав! Уже бегу!", "Щас всё будет, хозяин!", "Хвостом виляю, дело делаю!",
        "Принёс! Хвалить обязательно!", "Мой нос чует, что надо открыть!"
    )

    private val jokes = listOf(
        "— Доктор, я всё забываю! — И давно это у вас? — Что «это»?",
        "Почему программисты путают Хэллоуин и Рождество? Потому что 31 октября равно 25 декабря.",
        "Жена программисту: «Сходи в магазин, купи батон. Если будут яйца — возьми десяток». Он вернулся с десятью батонами: «Яйца были».",
        "Купил мужик шляпу, а она ему как раз.",
        "Учитель: — Вовочка, назови три местоимения. — Кто? Я? Мы? — Правильно!",
        "Заходит улитка в бар. Бармен: — Мы закрыты! Улитка: — Да я ненадолго, только спросить.",
        "— Официант, в моём супе муха! — Не волнуйтесь, она уже заплатила.",
        "Как собака узнаёт, что хозяин дома? По запаху тапочек, которые она уже догрызла.",
        "Два программиста пьют кофе. Один: «Мой код работает, но я не знаю почему». Второй: «А мой не работает, и я тоже не знаю почему».",
        "— Папа, а правда, что кот всегда приземляется на лапы? — Правда. Но бутерброд падает маслом вниз. Давай привяжем бутерброд коту на спину!",
        "Муж жене: «Дорогая, я купил тебе подарок, который стоит дорого, но выглядит дёшево». Жена: «Покажи!» — «Это счёт из ресторана».",
        "— Ты почему опоздал? — Автобус уехал без меня! — А ты бежал? — Бежал, но он оказался быстрее."
    )

    // ================= UI =================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("ru", "RU")
        }

        val d = resources.displayMetrics.density
        val pad = (24 * d).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        val title = TextView(this).apply {
            text = "Максик"; textSize = 32f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#E65100"))
        }
        bubble = TextView(this).apply {
            text = "Гав! Я Максик. Открываю приложения и сайты, говорю погоду, шучу и управляю телефоном!"
            textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#3E2723"))
            setPadding(pad, pad / 2, pad, pad / 2)
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = 28 * d
                setStroke((2 * d).toInt(), Color.parseColor("#FFB74D"))
            }
        }
        mascot = TextView(this).apply {
            text = "🐶"; textSize = 80f; gravity = Gravity.CENTER
            setPadding(0, 0, 0, pad / 2)
        }
        cookie = TextView(this).apply {
            text = "🍪"; textSize = 44f; visibility = View.INVISIBLE
        }
        zzz = TextView(this).apply {
            text = "💤"; textSize = 32f; visibility = View.INVISIBLE
            translationX = 70 * d
        }
        val wrap = FrameLayout.LayoutParams.WRAP_CONTENT
        val stage = FrameLayout(this).apply {
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (230 * d).toInt())
            addView(mascot, FrameLayout.LayoutParams(wrap, wrap, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
            addView(zzz, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
            addView(cookie, FrameLayout.LayoutParams(wrap, wrap, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        }
        input = EditText(this).apply {
            hint = "Например: громкость 50"
            imeOptions = EditorInfo.IME_ACTION_GO
            setSingleLine()
            setOnEditorActionListener { _, _, _ -> handle(text.toString()); true }
        }
        val mic = Button(this).apply { text = "🎤 Говорить"; setOnClickListener { listen() } }
        val go = Button(this).apply { text = "Выполнить"; setOnClickListener { handle(input.text.toString()) } }
        wakeBtn = Button(this).apply { setOnClickListener { toggleWake() } }

        listOf(title, bubble, stage, input, mic, go, wakeBtn).forEach { root.addView(it) }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#FFF8E1"))
            addView(root)
        })

        updateWakeBtn()
        startIdle()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        updateWakeBtn()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    private fun handleIntent(i: Intent?) {
        if (i == null) return
        if (i.hasExtra("command")) {
            val c = i.getStringExtra("command") ?: ""
            i.removeExtra("command")
            if (c.isBlank()) listen() else { input.setText(c); handle(c) }
        } else if (i.action == Intent.ACTION_ASSIST) listen()
    }

    // ================= Анимации =================
    private fun forever(a: ObjectAnimator, ms: Long, mode: Int = ValueAnimator.REVERSE) = a.apply {
        duration = ms
        repeatCount = ValueAnimator.INFINITE
        repeatMode = mode
        interpolator = if (mode == ValueAnimator.RESTART) LinearInterpolator() else AccelerateDecelerateInterpolator()
        idle.add(this); start()
    }

    private fun stopIdle() {
        idle.forEach { it.cancel() }
        idle.clear()
        mascot.animate().cancel()
        mascot.translationX = 0f; mascot.translationY = 0f
        mascot.rotation = 0f; mascot.scaleX = 1f; mascot.scaleY = 1f
        mascot.pivotX = mascot.width / 2f; mascot.pivotY = mascot.height / 2f
        zzz.visibility = View.INVISIBLE
    }

    private fun startIdle() {
        stopIdle()
        if (asleep) {
            // Максик спит: дышит, голова склонена, вверх плывёт «Zzz»
            mascot.text = "😴"
            mascot.pivotY = mascot.height.toFloat()
            forever(ObjectAnimator.ofFloat(mascot, "scaleY", 1f, 1.05f), 1800)
            forever(ObjectAnimator.ofFloat(mascot, "rotation", -14f, -6f), 2200)
            val top = mascot.top.toFloat()
            zzz.visibility = View.VISIBLE
            forever(ObjectAnimator.ofFloat(zzz, "translationY", top, top - 120f), 2200, ValueAnimator.RESTART)
            forever(ObjectAnimator.ofFloat(zzz, "alpha", 1f, 0f), 2200, ValueAnimator.RESTART)
        } else {
            mascot.text = "🐶"
            forever(ObjectAnimator.ofFloat(mascot, "translationY", 0f, -30f), 700)
            forever(ObjectAnimator.ofFloat(mascot, "rotation", -7f, 7f), 900)
        }
    }

    private fun pulse(emoji: String) {
        stopIdle()
        mascot.text = emoji
        forever(ObjectAnimator.ofFloat(mascot, "scaleX", 1f, 1.25f), 350)
        forever(ObjectAnimator.ofFloat(mascot, "scaleY", 1f, 1.25f), 350)
    }

    private fun backToIdle(set: AnimatorSet) {
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { startIdle() }
        })
        set.start()
    }

    private fun jump() {
        stopIdle()
        mascot.text = "🥳"
        val up = ObjectAnimator.ofFloat(mascot, "translationY", 0f, -260f).apply {
            duration = 350; interpolator = DecelerateInterpolator()
        }
        val flip = ObjectAnimator.ofFloat(mascot, "rotation", 0f, 360f).apply { duration = 700 }
        val down = ObjectAnimator.ofFloat(mascot, "translationY", -260f, 0f).apply {
            duration = 450; interpolator = BounceInterpolator()
        }
        val set = AnimatorSet()
        set.play(up).with(flip)
        set.play(down).after(up)
        backToIdle(set)
    }

    private fun shake() {
        stopIdle()
        mascot.text = "😵‍💫"
        val set = AnimatorSet()
        set.play(
            ObjectAnimator.ofFloat(mascot, "translationX", 0f, -45f, 45f, -35f, 35f, -20f, 20f, 0f)
                .apply { duration = 700 }
        )
        backToIdle(set)
    }

    private fun after(set: AnimatorSet, block: () -> Unit) {
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { block() }
        })
        set.start()
    }

    /** Печенька падает на голову, Максик благодарит и съедает её */
    private fun treat() {
        stopIdle()
        mascot.text = "🐶"
        mascot.pivotX = mascot.width / 2f; mascot.pivotY = mascot.height.toFloat()
        val startY = -cookie.height * 1.5f
        val land = mascot.top - cookie.height * 0.3f
        cookie.visibility = View.VISIBLE
        cookie.alpha = 1f; cookie.scaleX = 1f; cookie.scaleY = 1f
        cookie.rotation = 0f; cookie.translationY = startY

        val fall = ObjectAnimator.ofFloat(cookie, "translationY", startY, land).apply {
            duration = 800; interpolator = BounceInterpolator()
        }
        val spin = ObjectAnimator.ofFloat(cookie, "rotation", 0f, 720f).apply { duration = 800 }
        val squash = ObjectAnimator.ofFloat(mascot, "scaleY", 1f, 0.78f, 1f).apply {
            duration = 350; startDelay = 280
        }
        after(AnimatorSet().apply { playTogether(fall, spin, squash) }) {
            mascot.text = "😋"
            say("Спасибо, хозяин!")
            eat()
        }
    }

    private fun eat() {
        val sizes = listOf(1f, 0.7f, 0.45f, 0.2f, 0f)
        val bites = (0 until sizes.size - 1).map { i ->
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(cookie, "scaleX", sizes[i], sizes[i + 1]),
                    ObjectAnimator.ofFloat(cookie, "scaleY", sizes[i], sizes[i + 1]),
                    ObjectAnimator.ofFloat(mascot, "scaleX", 1f, 1.12f, 1f)
                )
                duration = 450
            }
        }
        after(AnimatorSet().apply { playSequentially(bites) }) {
            cookie.visibility = View.INVISIBLE
            jump()
        }
    }

    private fun praise() { say("Гав! Стараюсь, хозяин!"); jump() }

    private fun sleep() {
        if (asleep) { say("Хр-р... Я уже сплю, хозяин", false); return }
        asleep = true
        say("Спокойной ночи, хозяин")
        stopIdle()
        mascot.text = "🥱"
        mascot.pivotY = mascot.height.toFloat()
        val yawn = ObjectAnimator.ofFloat(mascot, "scaleY", 1f, 1.2f, 1f).apply { duration = 1400 }
        val droop = ObjectAnimator.ofFloat(mascot, "rotation", 0f, -14f).apply { duration = 1400 }
        backToIdle(AnimatorSet().apply { playTogether(yawn, droop) })
    }

    private fun wakeUp() {
        if (!asleep) { say("Я и так не сплю, хозяин! Гав!"); jump(); return }
        asleep = false
        say("Доброе утро, хозяин! Как же я хорошо поспал!")
        stopIdle()
        mascot.text = "🥱"
        mascot.pivotY = mascot.height.toFloat()
        val stretch = ObjectAnimator.ofFloat(mascot, "scaleY", 1f, 1.3f, 1f).apply { duration = 1000 }
        val squeeze = ObjectAnimator.ofFloat(mascot, "scaleX", 1f, 0.85f, 1f).apply { duration = 1000 }
        after(AnimatorSet().apply { playTogether(stretch, squeeze) }) { jump() }
    }

    // ================= Речь =================
    private fun say(text: String, speak: Boolean = true) {
        bubble.text = text
        if (speak) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maksik")
    }

    private fun listen() {
        pulse("🧐")
        say("Слушаю! Гав!", false)
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Что сделать Максику?")
        }
        try {
            startActivityForResult(i, 1)
        } catch (e: Exception) {
            say("Голосовой ввод недоступен, напиши текстом", false)
            shake()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1) return
        val text = if (resultCode == RESULT_OK)
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() else null
        if (text != null) { input.setText(text); handle(text) } else startIdle()
    }

    // ================= Разбор команды =================
    private fun has(tokens: List<String>, vararg p: String) =
        tokens.any { t -> p.any { t.startsWith(it) } }

    private fun numberOf(tokens: List<String>): Int? =
        tokens.firstNotNullOfOrNull { it.trim('%').toIntOrNull() ?: numWords[it] }

    private fun handle(raw: String) {
        val cleaned = raw.lowercase(Locale.getDefault())
            .replace(" точка ", ".")
            .replace(Regex("[,!?;:]"), " ")
            .trim().trimEnd('.')
        val tokens = cleaned.split(Regex("\\s+")).filter { it.isNotEmpty() && it !in wakeWords }
        val q = tokens.joinToString(" ")
        if (tokens.isEmpty()) { say("Гав! Я здесь. Что сделать?"); jump(); return }
        val off = has(tokens, "выключ", "отключ", "выкл")
        val music = has(tokens, "трек", "песн", "музык", "мелоди")

        when {
            has(tokens, "погод") ->
                weather(tokens.filter { !it.startsWith("погод") && it !in weatherStop }.joinToString(" "))
            tokens[0] in searchWords -> search(tokens.drop(1).joinToString(" "))
            has(tokens, "вкусняш", "печень", "угощен", "лакомств") -> treat()
            has(tokens, "молодец", "умница", "умничка", "хороший") -> praise()
            (has(tokens, "доброе", "добрым") && has(tokens, "утр")) ||
                has(tokens, "встава", "просыпай", "проснись", "подъем", "подъём") -> wakeUp()
            tokens.any { it in sleepWords } || has(tokens, "ложит", "ложис") -> sleep()
            has(tokens, "анекдот", "шутк", "рассмеши", "пошути") -> joke()
            isRinger(q, tokens) -> ringerCommand(q, tokens, off)
            has(tokens, "яркост", "ярче", "ярч", "темн", "затемн") -> brightness(tokens)
            has(tokens, "громк", "громче", "тише", "убавь", "прибавь") -> volume(tokens)
            has(tokens, "предыдущ", "прошл") || (music && has(tokens, "назад")) ->
                mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Предыдущий трек")
            has(tokens, "следующ", "дальше", "пропуст") || (music && has(tokens, "переключ")) ->
                mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Следующий трек")
            has(tokens, "пауз", "приостанов") || (music && has(tokens, "стоп", "останов", "выключ", "отключ")) ->
                mediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE, "Пауза")
            has(tokens, "играй", "продолж", "воспроизв") || (music && has(tokens, "включ")) ->
                mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY, "Играю")
            q.contains("wi fi") || q.contains("wi-fi") || q.contains("вай фай") ||
                has(tokens, "wifi", "вайфай", "вай-фай") -> wifi(!off)
            has(tokens, "блют", "bluetooth", "блю-") -> bluetooth(!off)
            else -> {
                val domain = Regex("([a-z0-9-]+\\.)+(com|ru|org|net|io|ua|by|kz|me|tv|app|dev|info|su|co)(/\\S*)?")
                    .find(q)?.value
                when {
                    domain != null -> openUrl("https://$domain", domain)
                    "сайт" in tokens -> openSite(tokens)
                    else -> openApp(tokens.filter { it !in fillers }.joinToString(" "))
                }
            }
        }
    }

    // ================= Анекдоты =================
    private fun joke() { say("Гав! " + jokes.random()); jump() }

    // ================= Управление телефоном =================
    private fun audio() = getSystemService(AUDIO_SERVICE) as AudioManager

    private fun openSettings(action: String, msg: String) {
        try { startActivity(Intent(action)); say(msg) } catch (e: Exception) { say("Не смог открыть настройки"); shake(); return }
        startIdle()
    }

    private fun mediaKey(code: Int, label: String) {
        val am = audio()
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        say(label); jump()
    }

    private fun volume(tokens: List<String>) {
        val am = audio()
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val step = maxOf(1, max / 7)
        val num = numberOf(tokens)
        val target = when {
            num != null -> max * num.coerceIn(0, 100) / 100
            has(tokens, "громче", "прибав", "увелич", "повыс") -> cur + step
            has(tokens, "тише", "убав", "уменьш", "пониз") -> cur - step
            has(tokens, "максим", "полную") -> max
            has(tokens, "минимум", "минимал") -> 0
            else -> { say("Громкость сейчас ${cur * 100 / max}%"); return }
        }.coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        say("Громкость ${target * 100 / max}%"); jump()
    }

    private fun brightness(tokens: List<String>) {
        if (!Settings.System.canWrite(this)) {
            say("Разреши мне менять настройки: включи переключатель и вернись ко мне")
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            shake(); return
        }
        val cr = contentResolver
        val cur = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128)
        val num = numberOf(tokens)
        val target = when {
            num != null -> 255 * num.coerceIn(0, 100) / 100
            has(tokens, "ярче", "ярч", "увелич", "прибав", "повыс") -> cur + 64
            has(tokens, "темн", "затемн", "убав", "уменьш", "пониз") -> cur - 64
            has(tokens, "максим") -> 255
            has(tokens, "минимум", "минимал") -> 10
            else -> { say("Яркость сейчас ${cur * 100 / 255}%"); return }
        }.coerceIn(1, 255)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, target)
        say("Яркость ${target * 100 / 255}%"); jump()
    }

    private fun isRinger(q: String, t: List<String>) =
        has(t, "вибро", "вибрац", "беззвуч") || q.contains("без звука") || q.contains("обычный режим") ||
            (q.contains("звук") && has(t, "включ", "выключ", "отключ", "режим", "верни"))

    private fun ringerCommand(q: String, t: List<String>, off: Boolean) {
        when {
            has(t, "вибро", "вибрац") && !off -> ringer(AudioManager.RINGER_MODE_VIBRATE, "вибрация")
            has(t, "вибро", "вибрац") && off -> ringer(AudioManager.RINGER_MODE_NORMAL, "со звуком")
            q.contains("без звука") || has(t, "беззвуч") || (off && q.contains("звук")) ->
                ringer(AudioManager.RINGER_MODE_SILENT, "без звука")
            else -> ringer(AudioManager.RINGER_MODE_NORMAL, "со звуком")
        }
    }

    private fun ringer(mode: Int, label: String) {
        try {
            audio().ringerMode = mode
            say("Режим: $label"); jump()
            if (mode == AudioManager.RINGER_MODE_VIBRATE) buzz()
        } catch (e: SecurityException) {
            say("Для этого режима нужен доступ к «Не беспокоить». Разреши и повтори")
            try { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) } catch (_: Exception) {}
            shake()
        }
    }

    @Suppress("DEPRECATION")
    private fun buzz() {
        val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        else v.vibrate(300)
    }

    @Suppress("DEPRECATION")
    private fun wifi(on: Boolean) {
        if (Build.VERSION.SDK_INT < 29) {
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            wm.isWifiEnabled = on
            say("Wi-Fi ${if (on) "включён" else "выключен"}"); jump(); return
        }
        // Android 10+ запрещает приложениям переключать Wi-Fi напрямую
        val action = if (Build.VERSION.SDK_INT >= 30) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
        openSettings(action, "Android не даёт мне самому переключать Wi-Fi. Открыл панель, нажми переключатель")
    }

    private fun bluetooth(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            pendingBt = on
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 2)
            return
        }
        doBluetooth(on)
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun doBluetooth(on: Boolean) {
        val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (adapter == null) { say("На этом телефоне нет Bluetooth"); shake(); return }
        if (Build.VERSION.SDK_INT < 33) {
            val ok = try { if (on) adapter.enable() else adapter.disable() } catch (e: SecurityException) { false }
            if (ok || adapter.isEnabled == on) {
                say("Bluetooth ${if (on) "включается" else "выключается"}"); jump(); return
            }
        }
        openSettings(Settings.ACTION_BLUETOOTH_SETTINGS,
            "На Android 13+ приложениям нельзя переключать Bluetooth. Открыл настройки, нажми переключатель")
    }

    // ================= «Привет, Максик» =================
    private fun wakeOn() = getSharedPreferences("maksik", MODE_PRIVATE).getBoolean("wake", false)

    private fun updateWakeBtn() {
        wakeBtn.text = if (wakeOn()) "👂 «Привет, Максик»: ВКЛ" else "👂 «Привет, Максик»: выкл"
    }

    private fun toggleWake() {
        val prefs = getSharedPreferences("maksik", MODE_PRIVATE)
        if (wakeOn()) {
            stopService(Intent(this, WakeService::class.java))
            prefs.edit().putBoolean("wake", false).apply()
            updateWakeBtn(); say("Больше не подслушиваю", false); return
        }
        val need = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 3) else continueWake()
    }

    private fun continueWake() {
        if (!Settings.canDrawOverlays(this)) {
            say("Разреши «Поверх других окон» (так я смогу открывать приложения по голосу) и нажми кнопку ещё раз")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val i = Intent(this, WakeService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        getSharedPreferences("maksik", MODE_PRIVATE).edit().putBoolean("wake", true).apply()
        updateWakeBtn()
        say("Теперь я слушаю! Скажи: «Привет, Максик, открой ютуб»")
        jump()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            2 -> {
                val on = pendingBt ?: return
                pendingBt = null
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) doBluetooth(on)
                else openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Без разрешения открою настройки Bluetooth")
            }
            3 -> {
                val i = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
                if (i == -1 || grantResults[i] == PackageManager.PERMISSION_GRANTED) continueWake()
                else { say("Без микрофона я тебя не услышу"); shake() }
            }
        }
    }

    // ================= Сайты и поиск =================
    private fun openUrl(url: String, name: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            say("${happy.random()} Открываю $name"); jump()
        } catch (e: Exception) {
            say("Не смог открыть $name"); shake()
        }
    }

    private fun openSite(tokens: List<String>) {
        val name = tokens.filter { it != "сайт" && it !in fillers }.joinToString(" ")
        val site = siteAliases[name]
        if (site != null) openUrl("https://$site", site) else search(name)
    }

    private fun search(query: String) {
        if (query.isBlank()) { say("Что искать?"); shake(); return }
        openUrl("https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8"), "поиск «$query»")
    }

    // ================= Приложения =================
    private fun openApp(nameRaw: String) {
        if (nameRaw.isEmpty()) { say("Какое приложение открыть?"); shake(); return }
        val name = appAliases[nameRaw] ?: nameRaw
        val app = findApp(name)
        val launch = app?.let { packageManager.getLaunchIntentForPackage(it.pkg) }
        if (app == null || launch == null) {
            say("Не нашёл «$nameRaw». Может, сказать «найди $nameRaw»?"); shake(); return
        }
        say("${happy.random()} Открываю ${app.label}"); jump()
        startActivity(launch)
    }

    private fun installedApps(): List<App> {
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(main, PackageManager.MATCH_ALL)
            .map { App(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
    }

    private fun findApp(q: String): App? {
        val apps = installedApps()
        apps.firstOrNull { it.label.lowercase() == q }?.let { return it }
        apps.firstOrNull { it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }?.let { return it }
        apps.firstOrNull { q.contains(it.label.lowercase()) && it.label.length > 2 }?.let { return it }
        return apps.map { it to distance(it.label.lowercase(), q) }
            .filter { it.second <= 2 }.minByOrNull { it.second }?.first
    }

    private fun distance(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length)
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1,
                d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        return d[a.length][b.length]
    }

    // ================= Погода (Open-Meteo) =================
    private fun weather(cityRaw: String) {
        val prefs = getSharedPreferences("maksik", MODE_PRIVATE)
        val name = cityRaw.ifBlank { prefs.getString("city", "") ?: "" }
        if (name.isBlank()) { say("В каком городе? Скажи: погода в Москве"); shake(); return }

        pulse("🤔")
        say("Секунду, выгляну в окно...", false)
        thread {
            try {
                val g = geocode(name)
                if (g == null) {
                    runOnUiThread { say("Не нашёл город «$name»"); shake() }
                    return@thread
                }
                val cur = JSONObject(get(
                    "https://api.open-meteo.com/v1/forecast?latitude=${g.second}&longitude=${g.third}" +
                    "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
                    "&wind_speed_unit=ms&timezone=auto"
                )).getJSONObject("current")
                val t = cur.getDouble("temperature_2m").roundToInt()
                val f = cur.getDouble("apparent_temperature").roundToInt()
                val w = cur.getDouble("wind_speed_10m").roundToInt()
                val desc = wmo(cur.getInt("weather_code"))
                val text = "${g.first}: сейчас ${sign(t)}°, ощущается как ${sign(f)}°, $desc, ветер $w м/с"
                prefs.edit().putString("city", g.first).apply()
                runOnUiThread { say(text); jump() }
            } catch (e: Exception) {
                runOnUiThread { say("Нет интернета, или сервис погоды спит"); shake() }
            }
        }
    }

    private fun sign(n: Int) = if (n > 0) "+$n" else "$n"

    private fun get(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        return try { c.inputStream.bufferedReader().readText() } finally { c.disconnect() }
    }

    private fun geocode(name: String): Triple<String, Double, Double>? {
        val cands = listOf(name, name.dropLast(1), name.dropLast(2)).filter { it.length >= 3 }.distinct()
        for (n in cands) {
            val j = JSONObject(get(
                "https://geocoding-api.open-meteo.com/v1/search?name=" +
                URLEncoder.encode(n, "UTF-8") + "&count=1&language=ru&format=json"
            ))
            val r = j.optJSONArray("results")
            if (r != null && r.length() > 0) {
                val o = r.getJSONObject(0)
                return Triple(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude"))
            }
        }
        return null
    }

    private fun wmo(c: Int) = when (c) {
        0 -> "ясно"; 1 -> "преимущественно ясно"; 2 -> "переменная облачность"; 3 -> "пасмурно"
        45, 48 -> "туман"; 51, 53, 55 -> "морось"; 56, 57 -> "ледяная морось"
        61 -> "небольшой дождь"; 63 -> "дождь"; 65 -> "сильный дождь"; 66, 67 -> "ледяной дождь"
        71 -> "небольшой снег"; 73 -> "снег"; 75 -> "сильный снег"; 77 -> "снежная крупа"
        80, 81, 82 -> "ливень"; 85, 86 -> "снегопад"; 95 -> "гроза"; 96, 99 -> "гроза с градом"
        else -> "погода загадочная"
    }
}
