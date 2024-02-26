package br.com.windel.pos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import kotlin.system.exitProcess

class SettingsActivity : AppCompatActivity() {

    private lateinit var buttonBack: Button
    private lateinit var buttonManualMode: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        supportActionBar?.hide()
        val sharedPreferences = getSharedPreferences("windelConfig", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        buttonBack = findViewById(R.id.buttonExit);
        buttonManualMode = findViewById(R.id.buttonManualMode)

        buttonBack.setOnClickListener {
            startActivity(
                Intent(applicationContext, MainActivity::class.java)
            )
            exitProcess(0)
        }

        buttonManualMode.setOnCheckedChangeListener { buttonView, isChecked ->
            try {
                editor.putBoolean("manualMode", isChecked)
                editor.apply()

            } catch (e: IOException) {
                Log.e(this.javaClass.name, e.message.toString())
            }
        }

        runOnUiThread {
            buttonManualMode.isChecked = sharedPreferences.getBoolean("manualMode", false)
        }
    }
}