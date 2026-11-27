//Music ViewModel
package cn.primeessence.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
// 说明：isActive 是 CoroutineScope 的扩展属性，不是变量，无需初始化；
// launch {} 内可直接访问，下面写成 this.isActive 只是更直观，二者等价
import kotlinx.coroutines.isActive
// 修复：新增导入，用于提交结果前确认协程未被取消
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class UiState(
    val songs: List<Song> = emptyList(),
    val loading: Boolean = false,
    val currentIndex: Int = -1,
    val playing: Boolean = false,
    val lyric: Boolean = false,
    val overlay: Boolean = false,
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepo: SongRepo,
    private val lyricRepo: LyricRepo,
    private val player: AppPlayer,
    private val lyricHost: LyricHost,
) : ViewModel() {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var treeUri: Uri? = null
    private var pendingLyricEnable = false
    private var lyricJob: Job? = null
    private var loadJob: Job? = null

    init {
        val saved = prefs.getString("tree_uri", null)?.let { Uri.parse(it) }
        if (saved != null && songRepo.hasReadPermission(saved)) {
            treeUri = saved
            loadSongs(saved)
        } else {
            prefs.edit().remove("tree_uri").apply()
        }
        viewModelScope.launch {
            player.currentIndex.collect { i -> _state.update { it.copy(currentIndex = i) } }
        }
        viewModelScope.launch {
            player.isPlaying.collect { p -> _state.update { it.copy(playing = p) } }
        }
    }

    fun takeTreeUri(uri: Uri?) {
        if (uri == null) return
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        if (!granted) return
        prefs.edit().putString("tree_uri", uri.toString()).apply()
        treeUri = uri
        loadSongs(uri)
    }

    private fun loadSongs(uri: Uri) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val list = withContext(Dispatchers.IO) { songRepo.loadSongs(uri) }
                ensureActive()
                player.songs.clear()
                player.songs.addAll(list)
                _state.update { it.copy(songs = list, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun playAt(index: Int) {
        if (index !in _state.value.songs.indices) return
        player.playList(_state.value.songs, index)
    }

    fun toggle() = player.toggle()
    fun next() = player.next()
    fun prev() = player.prev()

    fun lyric() {
        val wantEnable = !_state.value.lyric
        if (wantEnable && !Settings.canDrawOverlays(context)) {
            pendingLyricEnable = true
            _state.update { it.copy(overlay = true) }
            return
        }
        setLyricEnabled(wantEnable)
    }

    private fun setLyricEnabled(enable: Boolean) {
        _state.update { it.copy(lyric = enable) }
        if (enable) {
            pendingLyricEnable = false
            lyricHost.show()
            startTicker()
        } else {
            lyricJob?.cancel()
            lyricJob = null
            lyricHost.hide()
        }
    }

    private fun startTicker() {
        lyricJob?.cancel()
        lyricJob = viewModelScope.launch {
            var lastKey: String? = null
            var lines: List<LyricLine> = emptyList()
            var lastText: String? = null
            while (this.isActive) {
                player.tick()
                val song = player.currentSong()
                val key = song?.uri?.toString()
                if (key != lastKey) {
                    lastKey = key
                    lines = if (song != null) {
                        try {
                            withContext(Dispatchers.IO) { lyricRepo.loadLyric(song, treeUri) }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                    lastText = null
                }
                val text = when {
                    song == null -> ""
                    else -> LrcParser.indexAt(lines, player.position.value)
                        .let { lines.getOrNull(it)?.text } ?: song.title
                }
                if (text != lastText) {
                    lastText = text
                    lyricHost.setText(text)
                }
                delay(200)
            }
        }
    }

    fun overlayIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlayRequested() {
        _state.update { it.copy(overlay = false) }
    }

    fun onOverlayPermissionReturned() {
        val canDraw = Settings.canDrawOverlays(context)
        if (pendingLyricEnable && canDraw) {
            setLyricEnabled(true)
        } else if (!canDraw) {
            pendingLyricEnable = false
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        lyricJob?.cancel()
        lyricHost.hide()
        super.onCleared()
    }
}
