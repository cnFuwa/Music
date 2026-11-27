package cn.primeessence.music

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionToken: SessionToken,
) {
    val songs = mutableListOf<Song>()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position

    @Volatile private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlay: Pair<List<Song>, Int>? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            controller?.let {
                _currentIndex.value = it.currentMediaItemIndex
                _position.value = it.currentPosition
            }
        }
    }

    init {
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val c = runCatching { controllerFuture?.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            _isPlaying.value = c.isPlaying
            _currentIndex.value = c.currentMediaItemIndex
            _position.value = c.currentPosition
            pendingPlay?.let { (list, idx) ->
                applyPlay(c, list, idx)
                pendingPlay = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyPlay(c: MediaController, list: List<Song>, index: Int) {
        if (list.isEmpty()) return
        val items = list.mapIndexed { i, s ->
            s.toMediaItem().buildUpon().setMediaId(i.toString()).build()
        }
        c.setMediaItems(items, index.coerceIn(0, items.lastIndex), 0L)
        c.repeatMode = Player.REPEAT_MODE_ALL
        c.shuffleModeEnabled = false
        c.prepare()
        c.play()
    }

    fun playList(list: List<Song>, index: Int) {
        this.songs.clear()
        this.songs.addAll(list)
        val c = controller
        if (c == null) {
            pendingPlay = list to index
            return
        }
        applyPlay(c, list, index)
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.let {
            if (it.hasNextMediaItem()) it.seekToNext() else it.seekTo(0, 0L)
        }
    }

    fun prev() {
        controller?.let {
            when {
                it.currentPosition > 3000 -> it.seekTo(0)
                it.hasPreviousMediaItem() -> it.seekToPrevious()
                it.mediaItemCount > 0 -> it.seekTo(it.mediaItemCount - 1, 0L)
            }
        }
    }

    fun currentSong(): Song? =
        songs.getOrNull(controller?.currentMediaItemIndex ?: -1)

    fun tick() {
        _position.value = controller?.currentPosition ?: 0L
    }

}
