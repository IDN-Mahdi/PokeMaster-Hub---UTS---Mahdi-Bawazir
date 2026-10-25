package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PokemonAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isAudioPlaying: Boolean = false
        private set

    suspend fun playCry(pokemonId: Int, onComplete: () -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                mediaPlayer?.release()
                mediaPlayer = null

                val cryUrl = "https://raw.githubusercontent.com/PokeAPI/cries/main/cries/pokemon/latest/$pokemonId.ogg"
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    setDataSource(context, Uri.parse(cryUrl))
                    setOnCompletionListener {
                        this@PokemonAudioPlayer.isAudioPlaying = false
                        it.release()
                        if (mediaPlayer == it) mediaPlayer = null
                        onComplete()
                    }
                    setOnErrorListener { mp, _, _ ->
                        this@PokemonAudioPlayer.isAudioPlaying = false
                        mp.release()
                        if (mediaPlayer == mp) mediaPlayer = null
                        onComplete()
                        true
                    }
                    prepareAsync()
                    setOnPreparedListener {
                        this@PokemonAudioPlayer.isAudioPlaying = true
                        it.start()
                    }
                }
                mediaPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
                this@PokemonAudioPlayer.isAudioPlaying = false
                onComplete()
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isAudioPlaying = false
        } catch (_: Exception) {}
    }
}
