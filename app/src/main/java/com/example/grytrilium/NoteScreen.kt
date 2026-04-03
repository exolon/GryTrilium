package com.example.grytrilium

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// --- DATA MODEL ---
data class NoteNode(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Untitled Note",
    var content: String = "",
    val children: MutableList<NoteNode> = mutableListOf()
)

// --- VIEW MODEL (State Management) ---
class NoteViewModel : ViewModel() {
    // Tree State
    private var _rootNodes = mutableStateListOf(
        NoteNode(title = "Strategy Ideas", content = "Q3 Marketing approach..."),
        NoteNode(title = "Research", children = mutableListOf(
            NoteNode(title = "Competitor Analysis", content = "Data goes here.")
        ))
    )
    val rootNodes: List<NoteNode> get() = _rootNodes

    var expandedNodes = mutableStateListOf<String>()

    // Layout State
    var isTreeVisible by mutableStateOf(true)

    // Editor State
    var activeNodeId by mutableStateOf<String?>(null)
    var editorTitle by mutableStateOf("")
    var editorContent by mutableStateOf("")

    // Sync State
    var isRefreshing by mutableStateOf(false)

    fun toggleTree() {
        isTreeVisible = !isTreeVisible
    }

    fun toggleExpand(nodeId: String) {
        if (expandedNodes.contains(nodeId)) {
            expandedNodes.remove(nodeId)
        } else {
            expandedNodes.add(nodeId)
        }
    }

    fun setActiveNode(newId: String, context: Context) {
        // Autosave previous node before switching
        if (activeNodeId != null && activeNodeId != newId) {
            saveCurrentActiveNode()
            Toast.makeText(context, "Autosaved", Toast.LENGTH_SHORT).show()
        }

        activeNodeId = newId
        val node = findNode(newId, _rootNodes)
        if (node != null) {
            editorTitle = node.title
            editorContent = node.content
        }
    }

    private fun saveCurrentActiveNode() {
        activeNodeId?.let { id ->
            val node = findNode(id, _rootNodes)
            if (node != null) {
                node.title = editorTitle
                node.content = editorContent
            }
        }
    }

    fun addChildNode(parentId: String, context: Context) {
        val parent = findNode(parentId, _rootNodes)
        if (parent != null) {
            val newNode = NoteNode()
            parent.children.add(newNode)

            // Auto-expand parent so the new child is visible
            if (!expandedNodes.contains(parentId)) {
                expandedNodes.add(parentId)
            }

            // Switch focus to the new node
            setActiveNode(newNode.id, context)
        }
    }

    suspend fun refreshData(context: Context) {
        isRefreshing = true
        // Autosave first to prevent data loss on refresh collision
        if (activeNodeId != null) {
            saveCurrentActiveNode()
            Toast.makeText(context, "Autosaved prior to refresh", Toast.LENGTH_SHORT).show()
        }

        // Simulate network/DB pull
        delay(800)

        // In a real app, you would re-assign _rootNodes from your repository here

        isRefreshing = false
        Toast.makeText(context, "Tree Data Refreshed", Toast.LENGTH_SHORT).show()
    }

    private fun findNode(targetId: String, nodes: List<NoteNode>): NoteNode? {
        for (node in nodes) {
            if (node.id == targetId) return node
            val foundInChildren = findNode(targetId, node.children)
            if (foundInChildren != null) return foundInChildren
        }
        return null
    }
}

// --- UI COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes Workspace") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.toggleTree() }) {
                        Icon(Icons.Default.Menu, contentDescription = "Toggle Tree Panel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch { viewModel.refreshData(context) }
                        },
                        enabled = !viewModel.isRefreshing
                    ) {
                        if (viewModel.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Panel: Hideable Tree
            AnimatedVisibility(
                visible = viewModel.isTreeVisible,
                enter = expandHorizontally(animationSpec = tween(300)),
                exit = shrinkHorizontally(animationSpec = tween(300))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(viewModel.rootNodes) { rootNode ->
                            TreeItem(
                                node = rootNode,
                                depth = 0,
                                viewModel = viewModel,
                                context = context
                            )
                        }
                    }
                }
            }

            // Right Panel: Note Editor
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                if (viewModel.activeNodeId != null) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TextField(
                            value = viewModel.editorTitle,
                            onValueChange = { viewModel.editorTitle = it },
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Note Title") }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        TextField(
                            value = viewModel.editorContent,
                            onValueChange = { viewModel.editorContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("Start typing...") }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a note from the tree to start editing.", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun TreeItem(node: NoteNode, depth: Int, viewModel: NoteViewModel, context: Context) {
    val isExpanded = viewModel.expandedNodes.contains(node.id)
    val isActive = viewModel.activeNodeId == node.id

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/Collapse Icon
            if (node.children.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.toggleExpand(node.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = "Toggle Node"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            // Node Title
            Text(
                text = node.title,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setActiveNode(node.id, context) }
                    .padding(horizontal = 8.dp),
                maxLines = 1
            )

            // Add Child Button
            IconButton(
                onClick = { viewModel.addChildNode(node.id, context) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Child Note", tint = Color.Gray)
            }
        }

        // Render Children Recursively
        if (isExpanded) {
            node.children.forEach { childNode ->
                TreeItem(node = childNode, depth = depth + 1, viewModel = viewModel, context = context)
            }
        }
    }
}
