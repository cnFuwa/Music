package cn.primeessence.music

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(private val player: ExoPlayer) {

    val songs = mutableListOf<Song>()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentIndex.value = player.currentMediaItemIndex
            _position.value = player.currentPosition
        }
    }

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.shuffleModeEnabled = false
        player.addListener(listener)
    }

    fun playList(songs: List<Song>, index: Int) {
        this.songs.clear(); this.songs.addAll(songs)
        val items = songs.mapIndexed { i, s ->
            s.toMediaItem().buildUpon().setMediaId(i.toString()).build()
        }
        player.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0L)
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.shuffleModeEnabled =false
        player.prepare()
        player.play()
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNext() else player.seekTo(0, 0L)
    }

    fun prev() {
        when {
            player.currentPosition > 3000 -> player.seekTo(0)
            player.hasPreviousMediaItem() -> player.seekToPrevious()
            else -> player.seekTo(player.mediaItemCount - 1, 0L)
        }
    }

    fun currentSong(): Song? = songs.getOrNull(player.currentMediaItemIndex)

    fun tick() {
        _position.value = player.currentPosition
    }
}
