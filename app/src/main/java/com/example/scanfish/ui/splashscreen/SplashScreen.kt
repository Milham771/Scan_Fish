package com.example.scanfish.ui.splashscreen

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import com.example.scanfish.databinding.ActivityMainBinding
import com.example.scanfish.ui.beranda.Beranda

@Suppress("DEPRECATION")
@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    private var _binding : ActivityMainBinding? = null

    private val binding get() = _binding

    private var TIMER:Long = 2500 //2,5 detik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        Handler().postDelayed({
            val intent = Intent(this, Beranda::class.java)
            startActivity(intent)
            finish()
        },TIMER)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}