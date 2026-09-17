package ro.noadsx15.app

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.net.InetAddress
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var rules: BlocklistStore
    private lateinit var badge: TextView
    private lateinit var info: TextView
    private lateinit var updateButton: Button

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            render(intent?.getStringExtra("message"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rules = BlocklistStore(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContentView(buildUi())
        render(null)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(DnsFilterService.STATUS)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }

    override fun onStop() {
        try { unregisterReceiver(receiver) } catch (_: Throwable) {}
        super.onStop()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(38), dp(22), dp(28))
            setBackgroundColor(Color.rgb(248, 250, 252))
        }

        root.addView(TextView(this).apply {
            text = "NoAds X15"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(15, 23, 42))
        }, layout(-1, 60))

        root.addView(TextView(this).apply {
            text = "Standalone 3.0.1 • filtrare globală DNS"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(71, 85, 105))
        }, layout(-1, 42))

        badge = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        info = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
            setTextColor(Color.rgb(71, 85, 105))
        }
        root.addView(badge, layout(-1, 52))
        root.addView(info, layout(-1, 76))

        root.addView(makeButton("Activează protecția", Color.rgb(37, 99, 235)) {
            startProtection()
        }, layout(-1, 54))

        root.addView(makeButton("Oprește protecția", Color.rgb(220, 38, 38)) {
            stopProtection()
        }, layout(-1, 54))

        updateButton = makeButton("Actualizează listele", Color.rgb(15, 118, 110)) {
            updateLists()
        }
        root.addView(updateButton, layout(-1, 54))

        root.addView(makeButton("Testează DNS", Color.rgb(124, 58, 237)) {
            testDns()
        }, layout(-1, 54))

        root.addView(makeButton("Setări VPN Android", Color.rgb(71, 85, 105)) {
            startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
        }, layout(-1, 54))

        val customDomain = EditText(this).apply {
            hint = "domeniu pentru blocare/permitere"
            setSingleLine()
            background = rounded(Color.WHITE, 12)
            setPadding(dp(14), 0, dp(14), 0)
        }
        root.addView(customDomain, layout(-1, 54))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(makeButton("Blochează", Color.rgb(185, 28, 28)) {
            runCatching { rules.addBlocked(customDomain.text.toString()) }
                .onSuccess { render("Regulă de blocare adăugată") }
                .onFailure { toast("Domeniu invalid") }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        row.addView(makeButton("Permite", Color.rgb(22, 163, 74)) {
            runCatching { rules.addAllowed(customDomain.text.toString()) }
                .onSuccess { render("Excepție adăugată") }
                .onFailure { toast("Domeniu invalid") }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(row)

        root.addView(TextView(this).apply {
            text = "Blochează reclame, trackere și multe domenii de popup în browsere și aplicații. " +
                "Nu poate elimina elementele vizuale same-origin sau reclamele YouTube integrate în video."
            textSize = 13f
            setTextColor(Color.rgb(100, 116, 139))
            setPadding(0, dp(18), 0, 0)
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun startProtection() {
        val consent = VpnService.prepare(this)
        if (consent != null) startActivityForResult(consent, 10) else startNow()
    }

    private fun startNow() {
        val intent = Intent(this, DnsFilterService::class.java).setAction(DnsFilterService.START)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
        render("Pornire…")
    }

    private fun stopProtection() {
        startService(Intent(this, DnsFilterService::class.java).setAction(DnsFilterService.STOP))
        stopService(Intent(this, DnsFilterService::class.java))
        render("Oprit")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == RESULT_OK) startNow()
    }

    private fun updateLists() {
        if (DnsFilterService.running) {
            toast("Oprește protecția înainte de actualizare")
            return
        }
        updateButton.isEnabled = false
        info.text = "Pregătesc actualizarea…"
        Thread {
            val result = runCatching { rules.update { message -> runOnUiThread { info.text = message } } }
            runOnUiThread {
                updateButton.isEnabled = true
                result.onSuccess { render("Liste actualizate: $it domenii") }
                    .onFailure { render("Actualizarea a eșuat: ${it.message}") }
            }
        }.start()
    }

    private fun testDns() {
        info.text = "Testez…"
        Thread {
            val message = try {
                "DNS OK: ${InetAddress.getByName("example.com").hostAddress}"
            } catch (error: Throwable) {
                "DNS eșuat: ${error.javaClass.simpleName}"
            }
            runOnUiThread { info.text = message }
        }.start()
    }

    private fun render(message: String?) {
        val enabled = DnsFilterService.running
        badge.text = if (enabled) "ACTIV" else "OPRIT"
        badge.background = rounded(
            if (enabled) Color.rgb(22, 163, 74) else Color.rgb(100, 116, 139),
            22
        )

        val lastUpdate = getSharedPreferences("state", MODE_PRIVATE).getLong("last_update", 0)
        val date = if (lastUpdate == 0L) {
            "niciodată"
        } else {
            DateFormat.getDateTimeInstance().format(Date(lastUpdate))
        }

        info.text = message ?: (
            "${rules.count()} domenii • update: $date\n" +
                "Blocate ${DnsFilterService.blocked.get()} • " +
                "Permise ${DnsFilterService.allowed.get()}"
            )
    }

    private fun makeButton(label: String, color: Int, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(Color.WHITE)
            background = rounded(color, 13)
            setOnClickListener { action() }
        }
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun layout(width: Int, heightDp: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(width, dp(heightDp)).apply {
            setMargins(0, dp(5), 0, dp(5))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
