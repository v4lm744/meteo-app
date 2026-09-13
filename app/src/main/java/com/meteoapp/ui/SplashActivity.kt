package com.meteoapp.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Écran de lancement : joue l'animation météo puis démarre MainActivity et se
 * termine avec un fondu pour une transition fluide.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var splashView: SplashAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashView = SplashAnimationView(this)
        setContentView(splashView)
        splashView.listener = object : SplashAnimationView.AnimationListener {
            override fun onAnimationEnd() {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
                finish()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isFinishing) {
            splashView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        splashView.cancel()
    }
}
