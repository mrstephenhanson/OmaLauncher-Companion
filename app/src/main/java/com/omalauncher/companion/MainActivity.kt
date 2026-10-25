package com.omalauncher.companion

import android.app.*
import android.content.*
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

// Tokyo Night Theme Palette
val BgColor = Color(0xFF1a1b26)
val CardColor = Color(0xFF24283b)
val InputBg = Color(0xFF1f2335)
val TextColor = Color(0xFFc0caf5)
val AccentPurple = Color(0xFFbb9af7)
val AccentCyan = Color(0xFF7dcfff)
val AccentGreen = Color(0xFF9ece6a)
val AccentOrange = Color(0xFFff9e64)
val AccentRed = Color(0xFFf7768e)

data class AurPackage(
    val name: String,
    val version: String,
    val description: String,
    val votes: Int
)

data class CommandExecution(
    val id: String,
    val command: String,
    val status: String,
    val output: String,
    val exitCode: Int? = null
)

enum class Screen { DECK, AUR, COMMANDS, CLIPBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, BridgeServerService::class.java))
        setContent { MainRoot() }
    }
}

@Composable
fun MainRoot() {
    var currentScreen by remember { mutableStateOf(Screen.DECK) }
    Surface(modifier = Modifier.fillMaxSize(), color = BgColor) {
        when (currentScreen) {
            Screen.DECK -> ControlDeckScreen(
                onNavigateAur = { currentScreen = Screen.AUR },
                onNavigateCommands = { currentScreen = Screen.COMMANDS },
                onNavigateClipboard = { currentScreen = Screen.CLIPBOARD }
            )
            Screen.AUR -> AurBrowserScreen(onBack = { currentScreen = Screen.DECK })
            Screen.COMMANDS -> RemoteCommandScreen(onBack = { currentScreen = Screen.DECK })
            Screen.CLIPBOARD -> ClipboardSyncScreen(onBack = { currentScreen = Screen.DECK })
        }
    }
}

// --- SCREEN 1: CONTROL DECK ---
@Composable
fun ControlDeckScreen(
    onNavigateAur: () -> Unit,
    onNavigateCommands: () -> Unit,
    onNavigateClipboard: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OMA // COMPANION",
                color = TextColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Surface(color = CardColor, shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "127.0.0.1:8765",
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bridge status banner
        Surface(
            color = InputBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, CardColor, RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ARCH BRIDGE SERVER", color = AccentCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("ONLINE · Port 8765 · adb reverse ready", color = TextColor.copy(0.7f), fontSize = 12.sp)
                }
                Text("READY", color = AccentGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        DeckButton(
            title = "BROWSE AUR & REPOS",
            description = "Search and install packages into Arch Linux",
            accentColor = AccentPurple,
            onClick = onNavigateAur
        )

        DeckButton(
            title = "REMOTE ARCH COMMAND RUNNER",
            description = "Run terminal commands on Arch Linux & view streamed stdout",
            accentColor = AccentCyan,
            onClick = onNavigateCommands
        )

        DeckButton(
            title = "CLIPBOARD SYNCHRONIZATION",
            description = "Two-way clipboard bridge between Android and Arch (wl-copy / xclip)",
            accentColor = AccentOrange,
            onClick = onNavigateClipboard
        )

        DeckButton(
            title = "TEST HARDWARE BRIDGE",
            description = "Enqueue neofetch install job & test notification",
            accentColor = AccentGreen,
            onClick = {
                BridgeServerService.enqueuePackage("neofetch")
                Toast.makeText(context, "Queued 'neofetch' test package for Arch", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun DeckButton(title: String, description: String, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = accentColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = TextColor.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

// --- SCREEN 2: REMOTE ARCH COMMAND RUNNER ---
@Composable
fun RemoteCommandScreen(onBack: () -> Unit) {
    var commandInput by remember { mutableStateOf("") }
    val executions = BridgeServerService.commandHistory
    val quickPresets = listOf("uname -a", "neofetch", "df -h", "free -h", "uptime", "pacman -Qe")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextColor)
            }
            Text("ARCH COMMAND RUNNER", color = TextColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick presets
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quickPresets.take(3).forEach { cmd ->
                AssistChip(
                    onClick = { commandInput = cmd },
                    label = { Text(cmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AccentCyan) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = InputBg)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. uname -a, df -h...", color = TextColor.copy(0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedTextColor = TextColor,
                    unfocusedTextColor = TextColor,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = CardColor
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commandInput.isNotBlank()) {
                        BridgeServerService.enqueueCommand(commandInput.trim())
                        commandInput = ""
                    }
                })
            )

            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        BridgeServerService.enqueueCommand(commandInput.trim())
                        commandInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RUN", color = BgColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("EXECUTION LOGS // STDOUT", color = TextColor.copy(0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(executions.reversed()) { exec ->
                Card(colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$ ${exec.command}", color = AccentCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(exec.status.uppercase(), color = if (exec.status == "completed") AccentGreen else AccentOrange, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        if (exec.output.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = InputBg, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = exec.output,
                                    color = TextColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 3: CLIPBOARD SYNC SCREEN ---
@Composable
fun ClipboardSyncScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var inputToSync by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextColor)
            }
            Text("CLIPBOARD BRIDGE", color = TextColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("CURRENT ANDROID CLIPBOARD", color = AccentOrange, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(color = InputBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (BridgeServerService.currentClipboard.isNotEmpty()) BridgeServerService.currentClipboard else "(Clipboard is empty)",
                        color = TextColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        BridgeServerService.currentClipboard = clip
                        Toast.makeText(context, "Refreshed from system clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CardColor),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("REFRESH", color = TextColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardColor), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("PUSH TO ANDROID & ARCH", color = AccentGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputToSync,
                    onValueChange = { inputToSync = it },
                    placeholder = { Text("Type text to sync to clipboard...", color = TextColor.copy(0.4f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedTextColor = TextColor,
                        unfocusedTextColor = TextColor
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (inputToSync.isNotBlank()) {
                            BridgeServerService.setSystemClipboard(context, inputToSync)
                            Toast.makeText(context, "Copied to Android system clipboard!", Toast.LENGTH_SHORT).show()
                            inputToSync = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("SET SYSTEM CLIPBOARD", color = BgColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// --- SCREEN 4: AUR BROWSER ---
@Composable
fun AurBrowserScreen(onBack: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AurPackage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var installStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextColor)
            }
            Text("AUR PACKAGE REPOSITORY", color = TextColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search packages (e.g. htop, yay)...", color = TextColor.copy(0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedTextColor = TextColor,
                    unfocusedTextColor = TextColor,
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = CardColor
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            results = searchAur(searchQuery.trim())
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = BgColor)
            }
        }

        installStatus?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = AccentGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPurple)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results) { pkg ->
                    AurPackageCard(pkg = pkg, onInstall = {
                        BridgeServerService.enqueuePackage(pkg.name)
                        installStatus = "Queued: ${pkg.name}. Executing in Arch..."
                    })
                }
            }
        }
    }
}

@Composable
fun AurPackageCard(pkg: AurPackage, onInstall: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = pkg.name, color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                Text(text = "▲ ${pkg.votes}", color = AccentGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Text(text = "v${pkg.version}", color = TextColor.copy(alpha = 0.5f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = pkg.description, color = TextColor.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 3)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("INSTALL", color = BgColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

fun searchAur(term: String): List<AurPackage> {
    return try {
        val encoded = URLEncoder.encode(term, "UTF-8")
        val url = URL("https://aur.archlinux.org/rpc/v5/search/$encoded")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        val stream = BufferedReader(InputStreamReader(conn.inputStream))
        val body = stream.readText()
        stream.close()
        val json = JSONObject(body)
        val resultsArray = json.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<AurPackage>()
        for (i in 0 until resultsArray.length()) {
            val item = resultsArray.getJSONObject(i)
            list.add(
                AurPackage(
                    name = item.optString("Name", ""),
                    version = item.optString("Version", ""),
                    description = item.optString("Description", "No description available."),
                    votes = item.optInt("NumVotes", 0)
                )
            )
        }
        list
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// --- BACKGROUND BRIDGE SERVER (PORT 8765) ---
class BridgeServerService : Service() {
    companion object {
        var currentClipboard: String by mutableStateOf("https://archlinux.org")
        private val installQueue = ConcurrentLinkedQueue<String>()
        private val commandQueue = ConcurrentLinkedQueue<CommandExecution>()
        val commandHistory = mutableStateListOf<CommandExecution>()

        fun enqueuePackage(pkg: String) {
            installQueue.offer(pkg)
        }

        fun enqueueCommand(cmd: String): String {
            val id = UUID.randomUUID().toString().take(8)
            val exec = CommandExecution(id = id, command = cmd, status = "pending", output = "")
            commandQueue.offer(exec)
            commandHistory.add(exec)
            return id
        }

        fun setSystemClipboard(context: Context, text: String) {
            currentClipboard = text
            Handler(Looper.getMainLooper()).post {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("OMA_CLIP", text)
                clipboardManager.setPrimaryClip(clipData)
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createServiceNotification())
        if (!isRunning) startServer()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createServiceNotification(): Notification {
        val channelId = "oma_service"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, "OmaBridge Background Service", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("OmaBridge Active")
            .setContentText("Listening on 127.0.0.1:8765 for Arch Linux commands")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    private fun startServer() {
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(8765, 50, InetAddress.getByName("127.0.0.1"))
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val writer = PrintWriter(s.getOutputStream(), true)
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            var body = ""
            if (contentLength > 0) {
                val buf = CharArray(contentLength)
                reader.read(buf, 0, contentLength)
                body = String(buf)
            }

            when {
                path == "/queue/poll" -> {
                    val nextPkg = installQueue.poll()
                    val response = if (nextPkg != null) "{\"pkg\":\"$nextPkg\"}" else "{}"
                    sendHttpResponse(writer, 200, response)
                }

                path == "/battery" -> {
                    val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val pct = if (level != -1 && scale != -1) (level * 100 / scale) else 85
                    sendHttpResponse(writer, 200, "{\"percent\": $pct}")
                }

                path == "/notify" && method == "POST" -> {
                    val json = JSONObject(body.ifEmpty { "{}" })
                    showNotification(json.optString("title", "Omarchy"), json.optString("body", ""))
                    sendHttpResponse(writer, 200, "{\"status\": \"sent\"}")
                }

                path == "/clipboard" && method == "GET" -> {
                    val escaped = JSONObject.quote(currentClipboard)
                    sendHttpResponse(writer, 200, "{\"text\": $escaped}")
                }

                path == "/clipboard" && method == "POST" -> {
                    var textToSet = body
                    try {
                        val json = JSONObject(body)
                        if (json.has("text")) textToSet = json.getString("text")
                    } catch (_: Exception) {}

                    setSystemClipboard(applicationContext, textToSet)
                    showNotification("Clipboard Synced", "Copied from Arch: ${textToSet.take(40)}")
                    sendHttpResponse(writer, 200, "{\"status\": \"copied\", \"length\": ${textToSet.length}}")
                }

                path == "/exec/poll" && method == "GET" -> {
                    val nextCmd = commandQueue.poll()
                    val response = if (nextCmd != null) {
                        JSONObject().apply {
                            put("id", nextCmd.id)
                            put("command", nextCmd.command)
                        }.toString()
                    } else "{}"
                    sendHttpResponse(writer, 200, response)
                }

                path == "/exec/result" && method == "POST" -> {
                    try {
                        val json = JSONObject(body)
                        val id = json.optString("id", "")
                        val output = json.optString("output", "")
                        val exitCode = json.optInt("exitCode", 0)

                        val index = commandHistory.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val existing = commandHistory[index]
                            commandHistory[index] = existing.copy(
                                status = if (exitCode == 0) "completed" else "failed",
                                output = output,
                                exitCode = exitCode
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    sendHttpResponse(writer, 200, "{\"status\": \"recorded\"}")
                }

                path == "/context" -> {
                    val json = JSONObject().apply {
                        put("status", "active")
                        put("timestamp", System.currentTimeMillis())
                        put("port", 8765)
                        put("clipboard", currentClipboard)
                    }
                    sendHttpResponse(writer, 200, json.toString())
                }

                else -> sendHttpResponse(writer, 404, "{\"error\": \"Not Found\"}")
            }
        }
    }

    private fun sendHttpResponse(writer: PrintWriter, status: Int, json: String) {
        val bytes = json.toByteArray()
        writer.print("HTTP/1.1 $status OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n$json")
        writer.flush()
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "oma_notifications"
        val channel = NotificationChannel(channelId, "OmaBridge Alerts", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
