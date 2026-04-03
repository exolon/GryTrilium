package com.example.grytrilium

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

enum class AppScreen { SETUP, WORKSPACE, SETTINGS }

class UiTreeNode(
    val id: String,
    initialTitle: String,
    initialContent: String = "Loading...",
    val iconClass: String? = null,
    val children: MutableList<UiTreeNode> = mutableListOf()
) {
    var title by mutableStateOf(initialTitle)
    var content by mutableStateOf(initialContent)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("GryTriliumPrefs", Context.MODE_PRIVATE) }

            var themePref by remember { mutableStateOf(sharedPrefs.getString("themePref", "amoled") ?: "amoled") }
            var fontScale by remember { mutableFloatStateOf(sharedPrefs.getFloat("fontScale", 1.0f)) }

            // Swapped to the Inter font
            val interFont = FontFamily(Font(R.font.inter))

            val lightColors = lightColorScheme()
            val amoledColors = darkColorScheme(background = Color.Black, surface = Color(0xFF121212), primary = Color(0xFFBB86FC), onSurfaceVariant = Color.LightGray)
            val sepiaColors = lightColorScheme(background = Color(0xFFF4ECD8), surface = Color(0xFFEBE0C5), primary = Color(0xFF5D4037), onBackground = Color(0xFF3E2723), onSurface = Color(0xFF3E2723))

            val activeColors = when (themePref) {
                "light" -> lightColors
                "sepia" -> sepiaColors
                else -> amoledColors
            }

            // Typography updated for Inter
            val appTypography = Typography(
                headlineMedium = Typography().headlineMedium.copy(fontFamily = interFont, fontWeight = FontWeight.Normal, fontSize = 28.sp * fontScale),
                headlineSmall = Typography().headlineSmall.copy(fontFamily = interFont, fontWeight = FontWeight.Normal, fontSize = 24.sp * fontScale),
                titleLarge = Typography().titleLarge.copy(fontFamily = interFont, fontWeight = FontWeight.Normal),
                titleMedium = Typography().titleMedium.copy(fontFamily = interFont, fontWeight = FontWeight.Normal, fontSize = 16.sp * fontScale),
                titleSmall = Typography().titleSmall.copy(fontFamily = interFont, fontWeight = FontWeight.Normal),
                bodyLarge = Typography().bodyLarge.copy(fontFamily = interFont, fontWeight = FontWeight.Normal),
                bodyMedium = Typography().bodyMedium.copy(fontFamily = interFont, fontWeight = FontWeight.Normal, fontSize = 14.sp * fontScale),
                bodySmall = Typography().bodySmall.copy(fontFamily = interFont, fontWeight = FontWeight.Normal),
                labelLarge = Typography().labelLarge.copy(fontFamily = interFont, fontWeight = FontWeight.Normal),
                labelMedium = Typography().labelMedium.copy(fontFamily = interFont, fontWeight = FontWeight.Normal),
                labelSmall = Typography().labelSmall.copy(fontFamily = interFont, fontWeight = FontWeight.Normal)
            )

            MaterialTheme(colorScheme = activeColors, typography = appTypography) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GryTriliumApp(
                        themePref = themePref,
                        onThemeChange = { newTheme ->
                            themePref = newTheme
                            sharedPrefs.edit().putString("themePref", newTheme).apply()
                        },
                        fontScale = fontScale,
                        onFontScaleChange = { newScale ->
                            fontScale = newScale
                            sharedPrefs.edit().putFloat("fontScale", newScale).apply()
                        }
                    )
                }
            }
        }
    }
}

fun mapBoxiconToMaterial(boxiconClass: String?): ImageVector {
    if (boxiconClass == null) return Icons.Default.Description
    val lowerClass = boxiconClass.lowercase()
    return when {
        lowerClass.contains("bx-plus-medical") -> Icons.Default.Add
        lowerClass.contains("bx-book") -> Icons.Default.MenuBook
        lowerClass.contains("bx-folder") -> Icons.Default.Folder
        lowerClass.contains("bx-home") -> Icons.Default.Home
        lowerClass.contains("bx-calendar") -> Icons.Default.DateRange
        lowerClass.contains("bx-star") -> Icons.Default.Star
        lowerClass.contains("bx-heart") -> Icons.Default.Favorite
        lowerClass.contains("bx-check") -> Icons.Default.CheckCircle
        lowerClass.contains("bx-x") -> Icons.Default.Close
        lowerClass.contains("bx-cog") -> Icons.Default.Settings
        lowerClass.contains("bx-user") -> Icons.Default.Person
        lowerClass.contains("bx-edit") -> Icons.Default.Edit
        lowerClass.contains("bx-trash") -> Icons.Default.Delete
        lowerClass.contains("bx-time") -> Icons.Default.Schedule
        lowerClass.contains("bx-bulb") -> Icons.Default.Lightbulb
        lowerClass.contains("bx-note") || lowerClass.contains("bx-file") -> Icons.Default.InsertDriveFile
        else -> Icons.Default.Description
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GryTriliumApp(themePref: String, onThemeChange: (String) -> Unit, fontScale: Float, onFontScaleChange: (Float) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("GryTriliumPrefs", Context.MODE_PRIVATE) }

    var currentScreen by remember { mutableStateOf(AppScreen.SETUP) }

    var serverUrl by remember { mutableStateOf(sharedPrefs.getString("serverUrl", "http://10.0.0.100:9999") ?: "http://10.0.0.100:9999") }
    var token by remember { mutableStateOf(sharedPrefs.getString("token", "") ?: "") }
    var searchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var uiNodes by remember { mutableStateOf<List<UiTreeNode>>(emptyList()) }
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }
    var isRefreshing by remember { mutableStateOf(false) }

    var activeNodeId by remember { mutableStateOf<String?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }

    var initialEditTitle by remember { mutableStateOf("") }
    var initialEditContent by remember { mutableStateOf("") }

    val richTextState = rememberRichTextState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    fun getApi(): TriliumApi {
        return Retrofit.Builder()
            .baseUrl(if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TriliumApi::class.java)
    }

    fun findNode(id: String, nodes: List<UiTreeNode>): UiTreeNode? {
        for (node in nodes) {
            if (node.id == id) return node
            findNode(id, node.children)?.let { return it }
        }
        return null
    }

    fun autosaveCurrentNode() {
        activeNodeId?.let { id ->
            findNode(id, uiNodes)?.let { node ->
                val currentHtml = richTextState.toHtml()
                val hasTitleChanged = editTitle != initialEditTitle
                val hasContentChanged = currentHtml != initialEditContent

                if (!hasTitleChanged && !hasContentChanged) return@let

                node.title = editTitle
                node.content = currentHtml

                if (id.startsWith("local_")) return@let

                coroutineScope.launch {
                    try {
                        val api = getApi()

                        if (hasTitleChanged) {
                            api.updateNoteMetadata(token.trim(), id, UpdateNoteRequest(node.title))
                            initialEditTitle = node.title
                        }

                        if (hasContentChanged) {
                            val mediaType = okhttp3.MediaType.parse("text/html")
                            val body = okhttp3.RequestBody.create(mediaType, node.content)
                            api.updateNoteContent(token.trim(), id, body)
                            initialEditContent = node.content
                        }

                        Toast.makeText(context, "Saved to Server", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Auto-save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun fetchNotes() {
        isRefreshing = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val api = getApi()
                val q = searchQuery.trim()

                val response = if (q.isEmpty()) {
                    api.searchNotes(token.trim(), "note.title %= '.*'", "root", "eq1")
                } else {
                    api.searchNotes(token.trim(), q)
                }

                val nodeMap = response.results.associate { apiNote ->
                    val iconAttr = apiNote.attributes.find { it.name == "iconClass" }
                    apiNote.noteId to UiTreeNode(id = apiNote.noteId, initialTitle = apiNote.title, iconClass = iconAttr?.value)
                }

                val roots = mutableListOf<UiTreeNode>()

                for (apiNote in response.results) {
                    val uiNode = nodeMap[apiNote.noteId]!!
                    var isRoot = true
                    for (parentId in apiNote.parentNoteIds) {
                        if (parentId != "root" && parentId != "none" && nodeMap.containsKey(parentId)) {
                            nodeMap[parentId]!!.children.add(uiNode)
                            isRoot = false
                        }
                    }
                    if (isRoot || q.isNotEmpty()) {
                        roots.add(uiNode)
                    }
                }

                uiNodes = roots.distinctBy { it.id }

                uiNodes.forEach { rootNode ->
                    if (rootNode.children.isEmpty() && rootNode.content == "Loading...") {
                        coroutineScope.launch {
                            try {
                                val contentResp = api.getNoteContent(token.trim(), rootNode.id)
                                rootNode.content = contentResp.string()
                            } catch (e: Exception) {
                                rootNode.content = "Failed to load snippet."
                            }
                        }
                    }
                }

                if (uiNodes.isEmpty()) {
                    errorMessage = "Connected, but no notes found."
                } else {
                    sharedPrefs.edit().putString("serverUrl", serverUrl.trim()).putString("token", token.trim()).apply()
                    currentScreen = AppScreen.WORKSPACE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    var autoConnectAttempted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (token.isNotEmpty() && !autoConnectAttempted) {
            autoConnectAttempted = true
            fetchNotes()
        }
    }

    fun selectNode(node: UiTreeNode) {
        if (activeNodeId != node.id) {
            autosaveCurrentNode()
            activeNodeId = node.id
            editTitle = node.title
            initialEditTitle = node.title
            isEditMode = false
            richTextState.setHtml("<p>Loading...</p>")
            coroutineScope.launch { drawerState.close() }

            coroutineScope.launch {
                try {
                    val rawResponse = getApi().getNoteContent(token.trim(), node.id)
                    val fetchedStr = rawResponse.string()
                    richTextState.setHtml(fetchedStr)
                    node.content = fetchedStr
                    initialEditContent = richTextState.toHtml()
                } catch (e: Exception) {
                    richTextState.setHtml("<p>Failed to load: ${e.message}</p>")
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == AppScreen.WORKSPACE,
        drawerContent = {
            if (currentScreen == AppScreen.WORKSPACE) {
                ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("Notes Index", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        Divider()
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                            fun renderNode(node: UiTreeNode, depth: Int) {
                                item {
                                    val isExpanded = expandedNodes.contains(node.id)
                                    val isActive = activeNodeId == node.id

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                            if (node.children.isNotEmpty()) {
                                                IconButton(onClick = { expandedNodes = if (isExpanded) expandedNodes - node.id else expandedNodes + node.id }) {
                                                    Icon(if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight, null)
                                                }
                                            }
                                        }

                                        Icon(
                                            imageVector = mapBoxiconToMaterial(node.iconClass),
                                            contentDescription = null,
                                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                        )

                                        Text(
                                            text = node.title,
                                            maxLines = 1,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                            modifier = Modifier.weight(1f).clickable { selectNode(node) }.padding(vertical = 8.dp, horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                autosaveCurrentNode()
                                                val newChild = UiTreeNode(id = "local_${UUID.randomUUID()}", initialTitle = "New Note", initialContent = "")
                                                node.children.add(newChild)
                                                expandedNodes = expandedNodes + node.id
                                                activeNodeId = newChild.id
                                                editTitle = newChild.title
                                                initialEditTitle = editTitle
                                                richTextState.setHtml("")
                                                initialEditContent = richTextState.toHtml()
                                                isEditMode = true
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Child", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                if (expandedNodes.contains(node.id)) {
                                    node.children.forEach { child -> renderNode(child, depth + 1) }
                                }
                            }
                            uiNodes.forEach { rootNode -> renderNode(rootNode, 0) }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentScreen) {
                AppScreen.SETUP -> {
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize()) {
                        Text("GryTrilium v1.0", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = { Text("unRAID Server URL") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("ETAPI Token") }, modifier = Modifier.fillMaxWidth())

                        Row(modifier = Modifier.padding(top = 16.dp)) {
                            Button(onClick = { fetchNotes() }) { Text("Connect") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { currentScreen = AppScreen.SETTINGS }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                Text("Settings")
                            }
                        }

                        if (isRefreshing) CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                        if (errorMessage != null) Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                AppScreen.SETTINGS -> {
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentScreen = if (token.isNotEmpty() && uiNodes.isNotEmpty()) AppScreen.WORKSPACE else AppScreen.SETUP }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
                        }
                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        Text("Theme Preference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FilterChip(selected = themePref == "light", onClick = { onThemeChange("light") }, label = { Text("Light") })
                            FilterChip(selected = themePref == "sepia", onClick = { onThemeChange("sepia") }, label = { Text("Sepia") })
                            FilterChip(selected = themePref == "amoled", onClick = { onThemeChange("amoled") }, label = { Text("AMOLED") })
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Text Size (x${"%.1f".format(fontScale)})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = fontScale,
                            onValueChange = { onFontScaleChange(it) },
                            valueRange = 0.8f..1.4f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("System Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(onClick = { Toast.makeText(context, "Auto-update coming in v1.1", Toast.LENGTH_SHORT).show() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Check for App Updates")
                        }
                    }
                }

                AppScreen.WORKSPACE -> {
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Tree")
                                }

                                // Directly referencing the new drawable PNG
                                Image(
                                    painter = painterResource(id = R.drawable.trilium_icon),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                )

                                val topBarTitle = if (activeNodeId != null) editTitle else "Workspace Overview"
                                Text(text = topBarTitle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)

                                IconButton(onClick = { currentScreen = AppScreen.SETTINGS }) { Icon(Icons.Default.Settings, "Settings") }
                                IconButton(
                                    onClick = { autosaveCurrentNode(); fetchNotes() },
                                    enabled = !isRefreshing
                                ) {
                                    if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    else Icon(Icons.Default.Refresh, "Refresh")
                                }
                            }
                        }

                        if (activeNodeId != null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (isEditMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }) { Text("B", fontWeight = FontWeight.Bold) }
                                        TextButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }) { Text("I", fontStyle = FontStyle.Italic) }
                                        TextButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }) { Text("U", textDecoration = TextDecoration.Underline) }
                                        TextButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)) }) { Text("H1") }
                                        TextButton(onClick = { richTextState.toggleUnorderedList() }) { Text("• List") }
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = { isEditMode = false; autosaveCurrentNode() }) { Icon(Icons.Default.Check, "Done", tint = MaterialTheme.colorScheme.primary) }
                                    }
                                    Divider()
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.End) {
                                        IconButton(onClick = { isEditMode = true }) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                                    if (isEditMode) {
                                        RichTextEditor(
                                            state = richTextState, modifier = Modifier.fillMaxSize(),
                                            colors = RichTextEditorDefaults.richTextEditorColors(containerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                                        )
                                    } else {
                                        AndroidView(
                                            factory = { ctx ->
                                                TextView(ctx).apply {
                                                    textSize = 16f * fontScale
                                                    setTextColor(android.graphics.Color.DKGRAY)
                                                    movementMethod = LinkMovementMethod.getInstance()
                                                }
                                            },
                                            update = { textView ->
                                                textView.text = HtmlCompat.fromHtml(richTextState.toHtml(), HtmlCompat.FROM_HTML_MODE_COMPACT)
                                                if (themePref == "amoled") textView.setTextColor(android.graphics.Color.LTGRAY)
                                                else if (themePref == "sepia") textView.setTextColor(android.graphics.Color.parseColor("#3E2723"))
                                                else textView.setTextColor(android.graphics.Color.DKGRAY)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("Root Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 250.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(uiNodes) { rootNode ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 160.dp).clickable { selectNode(rootNode) },
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(mapBoxiconToMaterial(rootNode.iconClass), "Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(end = 6.dp))
                                                    Text(rootNode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))

                                                if (rootNode.children.isNotEmpty()) {
                                                    val childTitles = rootNode.children.take(4).joinToString("\n") { "• ${it.title}" }
                                                    Text(text = childTitles + if (rootNode.children.size > 4) "\n..." else "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                } else {
                                                    val strippedText = HtmlCompat.fromHtml(rootNode.content, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                                                    Text(text = if (strippedText.isEmpty() || rootNode.content == "Loading...") "Tap to view note..." else strippedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}