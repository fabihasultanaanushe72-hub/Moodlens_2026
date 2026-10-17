package com.example.moodlens

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout

class MainActivity : MoodLensActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.view.View>(R.id.btnAbout).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About MoodLens")
                .setMessage("Facial expression estimates run on this device. Scores describe model predictions, not a person's feelings.\n\nHistory stores labels, scores, and dates; it does not store photos. Live sessions save stable results periodically.")
                .setPositiveButton("Got it", null).show()
        }

        findViewById<LinearLayout>(R.id.cardLive).setOnClickListener {
            startActivity(
                Intent(this, LiveDetectionActivity::class.java).apply {
                    putExtra(
                        LiveDetectionActivity.EXTRA_MODE,
                        LiveDetectionActivity.MODE_LIVE
                    )
                }
            )
        }

        findViewById<LinearLayout>(R.id.cardPhoto).setOnClickListener {
            startActivity(
                Intent(this, PhotoAnalysisActivity::class.java)
            )
        }

        findViewById<LinearLayout>(R.id.cardTargets).setOnClickListener {
            startActivity(
                Intent(this, TargetEmotionsActivity::class.java)
            )
        }

        findViewById<LinearLayout>(R.id.cardHistory).setOnClickListener {
            startActivity(
                Intent(this, HistoryActivity::class.java)
            )
        }

        findViewById<LinearLayout>(R.id.cardAnalytics).setOnClickListener {
            startActivity(
                Intent(this, AnalyticsActivity::class.java)
            )
        }
    }
}
