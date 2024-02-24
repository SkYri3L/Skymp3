package com.skyri3l.skymp3

import android.os.Looper
import android.widget.ImageButton
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.os.Handler
import android.widget.SeekBar
import android.widget.TextView
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory
import android.content.Context
import android.widget.ImageView
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import android.graphics.Color





class MainActivity : ComponentActivity() {
        private lateinit var mediaPlayer: MediaPlayer
        private lateinit var seekBar: SeekBar
        private lateinit var endTimerTextView: TextView
        private val handler = Handler(Looper.getMainLooper())
        private var isLoopEnabled = false

        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.main_menu)

                // Find loop button
                val loopButton = findViewById<ImageButton>(R.id.loopButton)
                // Set click listener for loop button
                loopButton.setOnClickListener {
                        // Toggle loop state
                        isLoopEnabled = !isLoopEnabled
                        // Update loop button UI (change icon color or state indicator)
                        updateLoopButtonUI()
                }

                mediaPlayer = MediaPlayer.create(this, R.raw.all_the_small_things)

                val tempMp3File = getTempFile(this, R.raw.all_the_small_things)
                val albumArtImageView = findViewById<ImageView>(R.id.albumArtImageView)
                val albumArtBitmap = loadAlbumArt(tempMp3File.absolutePath)
                if (albumArtBitmap != null) {
                        albumArtImageView.setImageBitmap(albumArtBitmap)
                } else {
                        // Set a default image if no album art is available
                        albumArtImageView.setImageResource(R.drawable.default_album_art)
                }

                val songNameTextView = findViewById<TextView>(R.id.songNameTextView)
                val songName = "All the Small Things" // Get the actual song name dynamically
                songNameTextView.text = songName


                val playButton = findViewById<ImageButton>(R.id.playButton)
                playButton.setOnClickListener {
                        onPlayButtonClick()
                }

                val pauseButton = findViewById<ImageButton>(R.id.pauseButton)
                pauseButton.setOnClickListener {
                        onPauseButtonClick()
                }

                seekBar = findViewById(R.id.seekBar)
                seekBar.max = mediaPlayer.duration

                endTimerTextView = findViewById(R.id.endTimerTextView)
                val endTime = formatTime(mediaPlayer.duration.toLong())
                endTimerTextView.text = endTime

                updateSeekBar()

                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                if (fromUser) {
                                        mediaPlayer.seekTo(progress)
                                }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
        }

        private fun updateLoopButtonUI() {
                val loopButton = findViewById<ImageButton>(R.id.loopButton)
                if (isLoopEnabled) {
                        // Set loop button to indicate loop is enabled
                        loopButton.setColorFilter(Color.GREEN) // Example: Change icon color to green
                } else {
                        // Set loop button to indicate loop is disabled
                        loopButton.setColorFilter(Color.GRAY) // Example: Change icon color to gray
                }
        }


        private fun updateSeekBar() {
                handler.postDelayed(object : Runnable {
                        override fun run() {
                                seekBar.progress = mediaPlayer.currentPosition

                                val elapsedTime = mediaPlayer.currentPosition
                                val elapsedTimeFormatted = formatTime(elapsedTime.toLong())
                                endTimerTextView.text = elapsedTimeFormatted

                                handler.postDelayed(this, 1000) // Update every second
                        }
                }, 0)
        }

        fun onPlayButtonClick() {
                if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                }
        }

        fun onPauseButtonClick() {
                if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                }
        }

        private fun formatTime(milliseconds: Long): String {
                return String.format(
                        "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(milliseconds),
                        TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(milliseconds)
                        )
                )
        }

        private fun loadAlbumArt(mp3FilePath: String): Bitmap? {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(mp3FilePath)
                val albumArtBytes = retriever.embeddedPicture
                retriever.release()
                return if (albumArtBytes != null) {
                        BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.size)
                } else {
                        null
                }
        }

        private fun getTempFile(context: Context, resId: Int): File {
                val inputStream = context.resources.openRawResource(resId)
                val tempFile = File(context.cacheDir, "temp.mp3")
                tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                }
                return tempFile
        }

        override fun onDestroy() {
                super.onDestroy()
                mediaPlayer.release()
                handler.removeCallbacksAndMessages(null)
        }
}
