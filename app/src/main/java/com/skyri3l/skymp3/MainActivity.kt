package com.skyri3l.skymp3

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

        private lateinit var mediaPlayer: MediaPlayer
        private lateinit var seekBar: SeekBar
        private lateinit var endTimerTextView: TextView
        private val handler = Handler(Looper.getMainLooper())
        private var isLoopEnabled = false
        private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
        private var selectedUri: Uri? = null // Declare the variable here

        // Function to open file picker
        private fun openFilePicker() {
                launchFilePicker()
        }

        // Function to launch file picker
        private fun launchFilePicker() {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "audio/*"  // Filter to only show audio files
                filePickerLauncher.launch(intent)
        }

        // Function to play music from selected file
        private fun playMusicFromFile(uri: Uri?) {
                uri?.let { selectedUri ->
                        val filePath = getRealPathFromURI(selectedUri)
                        if (filePath != null) {
                                mediaPlayer.apply {
                                        reset() // Reset the MediaPlayer to its uninitialized state
                                        setDataSource(filePath)
                                        prepare()
                                        start()
                                }

                                // Load album art for the selected song
                                val albumArtBitmap = loadAlbumArt(filePath)
                                val albumArtImageView = findViewById<ImageView>(R.id.albumArtImageView)
                                if (albumArtBitmap != null) {
                                        albumArtImageView.setImageBitmap(albumArtBitmap)
                                } else {
                                        // Set a default image if no album art is available
                                        albumArtImageView.setImageResource(R.drawable.default_album_art)
                                }
                        } else {
                                Log.e("MainActivity", "File path is null")
                        }
                }
        }

        // Extension function to retrieve file path from URI
        private fun Uri.getPath(context: Context): String? {
                val scheme = this.scheme
                if (scheme == "content") {
                        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                        context.contentResolver.query(this, projection, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                                        val fileName = cursor.getString(columnIndex)
                                        val file = File(context.cacheDir, fileName)
                                        return file.absolutePath
                                }
                        }
                } else if (scheme == "file") {
                        return this.path
                }
                return null
        }

        private fun getRealPathFromURI(uri: Uri): String? {
                val projection = arrayOf(MediaStore.MediaColumns.DATA)
                val cursor = contentResolver.query(uri, projection, null, null, null)
                return cursor?.use {
                        val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                        it.moveToFirst()
                        it.getString(columnIndex)
                }
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

        fun onPlayButtonClick(ignoredView: View) {
                if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
                }
        }

        fun onPauseButtonClick(ignoredView: View) {
                if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                }
        }

        private fun onPauseButtonClick() {
                if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                }
        }

        private fun onPlayButtonClick() {
                if (!mediaPlayer.isPlaying) {
                        mediaPlayer.start()
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

        override fun onDestroy() {
                super.onDestroy()
                mediaPlayer.release()
                handler.removeCallbacksAndMessages(null)
        }


        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.main_menu)

                mediaPlayer = MediaPlayer()

                // Initialize the file picker launcher
                filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK) {
                                val data: Intent? = result.data
                                data?.data?.let { uri ->
                                        playMusicFromFile(uri) // Pass the URI here
                                }
                        }
                }

                // Add a button to trigger file selection
                val selectFileButton = findViewById<Button>(R.id.selectFileButton)
                selectFileButton.setOnClickListener {
                        openFilePicker()
                }

                // Find loop button
                val loopButton = findViewById<ImageButton>(R.id.loopButton)
                // Set click listener for loop button
                loopButton.setOnClickListener {
                        // Toggle loop state
                        isLoopEnabled = !isLoopEnabled
                        // Update loop button UI (change icon color or state indicator)
                        updateLoopButtonUI()
                }

                // Load album art for the selected song
                selectedUri?.let { uri ->
                        val filePath = uri.getPath(this)
                        val albumArtBitmap = filePath?.let { loadAlbumArt(it) }
                        val albumArtImageView = findViewById<ImageView>(R.id.albumArtImageView)
                        if (albumArtBitmap != null) {
                                albumArtImageView.setImageBitmap(albumArtBitmap)
                        } else {
                                // Set a default image if no album art is available
                                albumArtImageView.setImageResource(R.drawable.default_album_art)
                        }
                }

                val songNameTextView = findViewById<TextView>(R.id.songNameTextView)
                songNameTextView.text = "" // Set an initial text

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

}
