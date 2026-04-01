package com.ricoh.printservice

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.regex.Pattern

class ConfigActivity : AppCompatActivity() {

    private lateinit var ipAddressEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton

    companion object {
        private const val PREFS_NAME = "RicohPrintPrefs"
        private const val KEY_PRINTER_IP = "printer_ip"
        private const val DEFAULT_IP = "192.168.0.50"
        
        private val IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        saveButton = findViewById(R.id.saveButton)

        loadSavedIp()

        saveButton.setOnClickListener {
            saveIpAddress()
        }
    }

    private fun loadSavedIp() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIp = prefs.getString(KEY_PRINTER_IP, DEFAULT_IP)
        ipAddressEditText.setText(savedIp)
    }

    private fun saveIpAddress() {
        val ipAddress = ipAddressEditText.text.toString().trim()

        if (isValidIp(ipAddress)) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_PRINTER_IP, ipAddress).apply()
            
            Toast.makeText(this, getString(R.string.ip_saved_success), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            ipAddressEditText.error = getString(R.string.invalid_ip_error)
        }
    }

    private fun isValidIp(ip: String): Boolean {
        return IP_PATTERN.matcher(ip).matches()
    }
}
