package com.linkcare.linkcarewear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.linkcare.linkcarewear.R
import com.linkcare.linkcarewear.data.DataLayerManager

class MainActivity : AppCompatActivity() {
    private lateinit var summaryView: TextView
    private lateinit var themeTestButton: Button
    private val summaryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val summaryText = intent?.getStringExtra("summary")
            summaryView.text = "운동 리포트\n$summaryText"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        summaryView = findViewById(R.id.summaryTextView)
        themeTestButton = findViewById(R.id.themeTestButton)
        // ✅ 버튼 클릭 → 워치로 캐릭터 ID 전송
        themeTestButton.setOnClickListener {
            val testCharacterId = 1002L // 예시 ID
            val testThemeId = 2002L
            DataLayerManager.sendThemeToWatch(
                context = this,
                characterId = testCharacterId,
                backgroundId = testThemeId,
            )
            summaryView.text = "워치로 캐릭터 ID 전송됨: $testCharacterId"
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(
            summaryReceiver,
            IntentFilter("WORKOUT_SUMMARY_RECEIVED"),
            Context.RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(summaryReceiver)
    }
}
