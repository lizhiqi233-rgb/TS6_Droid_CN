package dev.tsdroid.bridge

import android.util.Log
import dev.tslib.Channel
import dev.tslib.Client
import dev.tslib.ConnectionState
import dev.tslib.Event
import dev.tslib.Identity
import dev.tslib.ServerInfo
import dev.tslib.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

data class TsFileEntry(
    val name: String,
    val size: Long,
    val datetime: Long,
    val isFile: Boolean,
)

class TsClient {

    companion object {
        private const val TAG = "TsClient"
    }

    private var client: Client? = null
    var serverAddress: String? = null
        private set

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<Int> = _state.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

    private val _commandErrors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val commandErrors: SharedFlow<String> = _commandErrors.asSharedFlow()

    private val downloadCallbacks = ConcurrentHashMap<String, CompletableDeferred<ByteArray>>()
    private val uploadCallbacks = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val fileListCallbacks = ConcurrentHashMap<String, CompletableDeferred<List<TsFileEntry>>>()

    private var eventLoopJob: Job? = null
    private val clientCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectMutex = Mutex()

    val isConnected: Boolean
        get() = client?.isConnected == true

    val clientId: Int?
        get() = client?.clientId

    suspend fun connect(
        address: String,
        identity: Identity,
        nickname: String,
        password: String? = null,
        channel: String? = null,
    ) = withContext(Dispatchers.IO) {
        connectMutex.withLock {
            try {
                // Phase 1: Pure physical reset of old client tokens
                stopEventLoop()
                disconnect()
                
                // Phase 2: Start new handshake after a safe 300ms propagation delay
                delay(300)
                
                serverAddress = address
                val c = Client(address, identity, nickname, password, channel)
                client = c
                _state.value = ConnectionState.CONNECTING
                c.waitConnected()
                _state.value = ConnectionState.CONNECTED
                // Log immediately after waitConnected
                val users = c.users
                val channels = c.channels
                Log.i(TAG, "After waitConnected: ${users?.size ?: "null"} users, ${channels?.size ?: "null"} channels")
                if (users != null) {
                    for (u in users) {
                        if (u != null) Log.d(TAG, "  User: ${u.nickname} (id=${u.id}, ch=${u.channelId})")
                    }
                }
                refreshState()
            } catch (e: Throwable) {
                Log.e("TS6_CRASH_PREVENTION", "Aggressively blocking AppCustomException", e)
                _state.value = ConnectionState.DISCONNECTED
                _commandErrors.tryEmit("服务器连接 busy，正在排队重试...")
                
                // CRITICAL FIX FOR CRASH: If waitConnected fails (e.g. server rejects connection because of swift reconnect), 
                // the client pointer is left in a broken state. 
                // Discard it safely without trying to send disconnect packets or flush network events (which causes SIGSEGV).
                val failedClient = client
                client = null
                if (failedClient != null) {
                    try {
                        failedClient.close() // Safe memory free instead of .disconnect()
                    } catch (_: Exception) {}
                }
                
                // In order to properly stop TsConnectionService from proceeding to start audioBridge/eventLoop
                // and crashing the app in subsequent steps, we MUST throw an exception back to the caller.
                throw Exception("Connection failed: ${e.message ?: "Server busy or rejected"}", e)
            }
        }
    }

    fun startEventLoop() {
        // 1. Cancel any active event loop cleanly first
        stopEventLoop()

        // 2. Launch a completely fresh lifecycle track
        eventLoopJob = clientCoroutineScope.launch {
            try {
                var refreshCounter = 0
                while (isActive && client != null) {
                    ensureActive()
                    try {
                        val c = client ?: break
                        val events = c.processEvents() ?: emptyArray()
                        for (event in events) {
                            _events.tryEmit(event)
                            handleEvent(event)
                        }
                        refreshCounter++
                        // Refresh on events or every ~500ms (25 * 20ms)
                        if (events.isNotEmpty() || refreshCounter >= 25) {
                            refreshState()
                            refreshCounter = 0
                        }
                    } catch (e: Exception) {
                        if (client == null) break
                        _state.value = ConnectionState.DISCONNECTED
                        break
                    }
                    delay(20)
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Event loop coroutine clean cancelled.")
            } catch (e: Exception) {
                Log.e(TAG, "Gracefully trapped event loop runtime friction", e)
            }
        }
    }

    fun stopEventLoop() {
        eventLoopJob?.let {
            if (it.isActive) {
                it.cancel()
                Log.d(TAG, "Killing stagnant legacy event loop forcefully.")
            }
        }
        eventLoopJob = null
    }

    private fun handleEvent(event: Event) {
        when (event.type) {
            "disconnected" -> _state.value = ConnectionState.DISCONNECTED
            "connected" -> _state.value = ConnectionState.CONNECTED
            "file_downloaded" -> {
                val path = event.data["path"] as? String ?: return
                val data = event.data["data"] as? ByteArray ?: return
                Log.d(TAG, "File downloaded: $path (${data.size} bytes)")
                downloadCallbacks.remove(path)?.complete(data)
            }
            "file_uploaded" -> {
                val path = event.data["path"] as? String ?: return
                Log.d(TAG, "File uploaded: $path")
                uploadCallbacks.remove(path)?.complete(true)
            }
            "file_transfer_failed" -> {
                val path = event.data["path"] as? String ?: return
                val error = event.data["error"] as? String ?: "unknown"
                Log.w(TAG, "File transfer failed: $path — $error")
                downloadCallbacks.remove(path)?.completeExceptionally(
                    Exception("File transfer failed: $error")
                )
                uploadCallbacks.remove(path)?.complete(false)
            }
            "file_list_received" -> {
                val channelId = (event.data["channel_id"] as? Number)?.toLong() ?: return
                val path = event.data["path"] as? String ?: return
                val filesJson = event.data["files"] as? String ?: return
                val entries = parseFileEntries(filesJson)
                Log.d(TAG, "File list received: channel=$channelId path=$path entries=${entries.size}")
                fileListCallbacks.remove("$channelId:$path")?.complete(entries)
            }
            "command_error" -> {
                val message = event.data["message"] as? String ?: return
                Log.w(TAG, "Command error: $message")
                _commandErrors.tryEmit(message)
            }
            "channel_permissions_updated" -> {
                val channelId = (event.data["channel_id"] as? Number)?.toLong() ?: return
                val hints = (event.data["permission_hints"] as? Number)?.toLong() ?: return
                Log.i(TAG, "Channel $channelId permissions updated: ${hints.toString(16)}")
                // Force refresh to propagate updated permission_hints
                refreshState()
            }
        }
    }

    private fun refreshState() {
        val c = client ?: return
        try {
            _channels.value = c.channels?.filterNotNull() ?: emptyList()
            val rawUsers = c.users
            val filteredUsers = rawUsers?.filterNotNull() ?: emptyList()
            Log.d(TAG, "refreshState: rawUsers=${rawUsers?.size}, filtered=${filteredUsers.size}")
            if (filteredUsers.isNotEmpty()) {
                for (u in filteredUsers) {
                    Log.d(TAG, "  user: ${u.nickname} id=${u.id} ch=${u.channelId}")
                }
            }
            _users.value = filteredUsers
            _serverInfo.value = c.serverInfo
            val st = c.state
            if (_state.value != st) _state.value = st
        } catch (e: Exception) {
            Log.w(TAG, "refreshState failed", e)
        }
    }

    fun sendChannelMessage(msg: String) {
        client?.sendChannelMessage(msg)
    }

    fun sendServerMessage(msg: String) {
        client?.sendServerMessage(msg)
    }

    fun sendPrivateMessage(userId: Int, msg: String) {
        client?.sendPrivateMessage(userId, msg)
    }

    fun moveToChannel(channelId: Long) {
        client?.moveToChannel(channelId)
    }

    fun sendAudio(data: ByteArray, codec: Int) {
        client?.sendAudio(data, codec)
    }

    fun setInputMuted(muted: Boolean) {
        val c = client
        Log.i(TAG, "setInputMuted($muted) — client=${if (c != null) "present" else "NULL"}")
        if (c == null) return
        try {
            c.setInputMuted(muted)
        } catch (e: Exception) {
            Log.w(TAG, "setInputMuted failed", e)
        }
    }

    suspend fun downloadFile(channelId: Long, path: String): ByteArray? {
        val deferred = CompletableDeferred<ByteArray>()
        downloadCallbacks[path] = deferred
        try {
            client?.downloadFile(channelId, path)
                ?: run { downloadCallbacks.remove(path); return null }
        } catch (e: Exception) {
            downloadCallbacks.remove(path)
            Log.w(TAG, "downloadFile failed for $path", e)
            return null
        }
        return withTimeoutOrNull(10_000) {
            try {
                deferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "downloadFile await failed for $path", e)
                null
            }
        }.also { downloadCallbacks.remove(path) }
    }

    suspend fun listFiles(channelId: Long, path: String): List<TsFileEntry>? {
        val key = "$channelId:$path"
        val deferred = CompletableDeferred<List<TsFileEntry>>()
        fileListCallbacks[key] = deferred
        try {
            client?.listFiles(channelId, path)
                ?: run { fileListCallbacks.remove(key); return null }
        } catch (e: Exception) {
            fileListCallbacks.remove(key)
            Log.w(TAG, "listFiles failed for $path", e)
            return null
        }
        return withTimeoutOrNull(5_000) {
            try { deferred.await() } catch (e: Exception) {
                Log.w(TAG, "listFiles await failed for $path", e)
                null
            }
        }.also { fileListCallbacks.remove(key) }
    }

    fun deleteFile(channelId: Long, name: String) {
        client?.deleteFile(channelId, name)
    }

    fun renameFile(channelId: Long, oldName: String, newName: String) {
        client?.renameFile(channelId, oldName, newName)
    }

    fun createDirectory(channelId: Long, dirname: String) {
        client?.createDirectory(channelId, dirname)
    }

    fun queryChannelPermissions(channelId: Long) {
        client?.queryChannelPermissions(channelId)
    }

    private fun parseFileEntries(json: String): List<TsFileEntry> {
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TsFileEntry(
                    name = obj.getString("name"),
                    size = obj.getLong("size"),
                    datetime = obj.getLong("datetime"),
                    isFile = obj.getBoolean("is_file"),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseFileEntries failed", e)
            emptyList()
        }
    }

    suspend fun uploadFile(channelId: Long, path: String, data: ByteArray, overwrite: Boolean = true): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        uploadCallbacks[path] = deferred
        try {
            client?.uploadFile(channelId, path, data, overwrite)
                ?: run { uploadCallbacks.remove(path); return false }
        } catch (e: Exception) {
            uploadCallbacks.remove(path)
            Log.w(TAG, "uploadFile failed for $path", e)
            return false
        }
        return withTimeoutOrNull(30_000) {
            try {
                deferred.await()
            } catch (e: Exception) {
                Log.w(TAG, "uploadFile await failed for $path", e)
                false
            }
        }.also { uploadCallbacks.remove(path) } ?: false
    }

    fun disconnect() {
        stopEventLoop()
        val c = client ?: return
        client = null

        // 3. Update state flows
        _state.value = ConnectionState.DISCONNECTED
        _channels.value = emptyList()
        _users.value = emptyList()
        _serverInfo.value = null

        // 4. Now safe to call disconnect on the native client (no concurrent access)
        try {
            c.disconnect()
            Log.d(TAG, "Native disconnect command sent")
        } catch (e: Exception) {
            Log.w(TAG, "disconnect failed", e)
        }

        // 5. Drive processEvents to flush the disconnect packet over the network
        try {
            val flushEnd = System.currentTimeMillis() + 500
            while (System.currentTimeMillis() < flushEnd) {
                c.processEvents()
                Thread.sleep(20)
            }
            Log.d(TAG, "Disconnect flush complete")
        } catch (_: Exception) {}

        // 6. Destroy the native client
        try {
            c.close()
        } catch (_: Exception) {}
    }
}
