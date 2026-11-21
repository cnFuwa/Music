package cn.primeessence.music

import android.net.Uri
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricRepository @Inject constructor(private val songRepo: SongRepository) {

    private val cache = mutableMapOf<String, List<LrcParser.LyricLine>>()
    private val mutex = Mutex()

    suspend fun loadLyric(song: Song, treeUri: Uri?): List<LrcParser.LyricLine> {
        val uri = treeUri ?: return emptyList()
        val key = "$uri|${song.uri}"
        cache[key]?.let { return it }
        return mutex.withLock {
            cache.getOrPut(key) {
                val raw = songRepo.findLyric(song, uri) ?: return@getOrPut emptyList()
                LrcParser.parse(raw)
            }
        }
    }
}
