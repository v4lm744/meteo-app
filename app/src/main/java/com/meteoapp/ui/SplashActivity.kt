package com.meteoapp.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/**
 * Écran de lancement : le splash système (androidx.core:core-splashscreen)
 * couvre le démarrage du process, puis l'activité joue l'animation météo
 * canvas et démarre MainActivity avec un fondu pour une transition fluide.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var splashView: SplashAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        splashView = SplashAnimationView(this)
        setContentView(splashView)
        splashView.listener = object : SplashAnimationView.AnimationListener {
            override fun onAnimationEnd() {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    @Suppress("DEPRECATION")
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
