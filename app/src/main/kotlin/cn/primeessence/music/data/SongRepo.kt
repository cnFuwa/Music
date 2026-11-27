//Song Repo
package cn.primeessence.music

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepo(private val context: Context) {

    companion object {
        private val AUDIO_EXTS = setOf("mp3", "flac", "m4a", "aac", "ogg", "wma", "wav", "opus")
        private val LYRIC_EXTS = setOf("lrc", "txt")
    }

    suspend fun loadSongs(treeUri: Uri): List<Song> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val result = mutableListOf<Song>()
        walkFiles(root) { node, parentNames ->
            if (isAudio(node)) {
                val name = node.name ?: return@walkFiles
                result += Song(
                    uri = node.uri,
                    name = name,
                    baseName = name.substringBeforeLast('.', name),
                    parentNames = parentNames.toList(),
                )
            }
        }
        result.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        result
    }

    suspend fun findLyric(song: Song, treeUri: Uri): String? = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
        val candidates = mutableListOf<Pair<Uri, String>>() // uri -> 扩展名
        walkFiles(root) { node, parentNames ->
            val name = node.name ?: return@walkFiles
            if (name.substringBeforeLast('.', name) == song.baseName &&
                name.substringAfterLast('.', "").lowercase() in LYRIC_EXTS
            ) {
                val sameDir = song.parentNames.isNotEmpty() &&
                    song.parentNames.all { it in parentNames }
                val ext = name.substringAfterLast('.').lowercase()
                // sameDir 为 true 时排序键为 0x/1x，始终小于非同目录的 2x/3x
                val rank = (if (sameDir) 0 else 2) + if (ext == "lrc") 0 else 1
                candidates += node.uri to rank.toString()
            }
        }
        // 先同目录、再 .lrc：双关键字排序
        val ordered = candidates.sortedBy { it.second }
        for ((uri, _) in ordered) readText(uri)?.let { return@withContext it }
        null
    }

    private inline fun walkFiles(
        root: DocumentFile,
        crossinline onFile: (DocumentFile, List<String>) -> Unit,
    ) {
        val stack = ArrayDeque<Pair<DocumentFile, List<String>>>()
        stack += root to emptyList()
        while (stack.isNotEmpty()) {
            val (dir, parents) = stack.removeLast()
            val children = try {
                dir.listFiles()
            } catch (e: Exception) {
                null
            } ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    val name = child.name ?: continue
                    stack += child to (parents + name)
                } else if (child.isFile) {
                    onFile(child, parents)
                }
            }
        }
    }

    private fun isAudio(file: DocumentFile): Boolean {
        val mime = file.type
        if (mime != null && mime.startsWith("audio/")) return true
        val name = file.name ?: return false
        return name.substringAfterLast('.', "").lowercase() in AUDIO_EXTS
    }

    private fun readText(uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (e: Exception) {
        null
    }


    fun hasReadPermission(treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == treeUri && it.isReadPermission }
}