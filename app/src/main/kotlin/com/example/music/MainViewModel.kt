package cn.primeessence.music

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiState(
    val songs: List<Song> = emptyList(),
    val loading: Boolean = false,
    val currentIndex: Int = -1,
    val playing: Boolean = false,
    val desktopLyric: Boolean = false,
    val requestOverlay: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songRepo: SongRepository,
    private val lyricRepo: LyricRepository,
    private val player: PlayerController,
    private val lyricHost: DesktopLyricHost,
) : ViewModel() {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var treeUri: Uri? = null

    init {
        val saved = prefs.getString("tree_uri", null)?.let { Uri.parse(it) }
        if (saved != null && songRepo.hasReadPermission(saved)) {
            treeUri = saved
            loadSongs(saved)
        }
        viewModelScope.launch { player.currentIndex.collect { i -> _state.value = _state.value.copy(currentIndex = i) } }
        viewModelScope.launch { player.isPlaying.collect { p -> _state.value = _state.value.copy(playing = p) } }
    }

    fun takeTreeUri(uri: Uri?) {
        if (uri == null) return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        prefs.edit().putString("tree_uri", uri.toString()).apply()
        treeUri = uri
        loadSongs(uri)
    }

    private fun loadSongs(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val list = songRepo.loadSongs(uri)
            player.songs.clear(); player.songs.addAll(list)
            _state.value = _state.value.copy(songs = list, loading = false)
        }
    }

    fun playAt(index: Int) {
        if (index !in _state.value.songs.indices) return
        player.playList(_state.value.songs, index)
    }

    fun toggle() = player.toggle()
    fun next() = player.next()
    fun prev() = player.prev()

    fun toggleDesktopLyric() {
        if (!_state.value.desktopLyric && !Settings.canDrawOverlays(context)) {
            _state.value = _state.value.copy(requestOverlay = true)
            return
        }
        val enable = !_state.value.desktopLyric
        _state.value = _state.value.copy(desktopLyric = enable)
        if (enable) {
            lyricHost.show()
            startTicker()
        } else {
            lyricJob?.cancel(); lyricJob = null
            lyricHost.hide()
        }
    }

    private var lyricJob: kotlinx.coroutines.Job? = null

    private fun startTicker() {
        lyricJob?.cancel()
        lyricJob = viewModelScope.launch {
            var lastKey: String? = null
            var lines: List<LrcParser.LyricLine> = emptyList()
            while (true) {
                player.tick()
                val song = player.currentSong()
                val key = song?.uri?.toString()
                if (key != lastKey) {
                    lastKey = key
                    lines = if (song != null) lyricRepo.loadLyric(song, treeUri) else emptyList()
                }
                lyricHost.setText(
                    when {
                        song == null -> "暂无播放"
                        else -> LrcParser.indexAt(lines, player.position.value).let { lines.getOrNull(it)?.text } ?: song.title
                    }
                )
                kotlinx.coroutines.delay(200)
            }
        }
    }

    fun overlayIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun overlayRequested() {
        _state.value = _state.value.copy(requestOverlay = false)
    }

    override fun onCleared() {
        lyricJob?.cancel()
        lyricHost.hide()
    }
}
