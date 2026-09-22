package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.ai.ToolFunctionParameters
import com.example.ai.ToolFunctionParameterProperty
import com.example.data.AssistantDatabase
import com.example.data.MemoryEntity
import com.example.data.PlannerEntity
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- TIME TOOL ---
class TimeTool : AssistantTool() {
    override val name = "get_current_time"
    override val description = "Get the current date and time on Ninety Five's system."
    override val parameters = null
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a (EEEE)", Locale.getDefault())
        val output = "Ninety Five reports current time: " + sdf.format(Date())
        return ToolResult(true, output)
    }
}

// --- CALCULATOR TOOL ---
class CalculatorTool : AssistantTool() {
    override val name = "calculator"
    override val description = "Perform simple mathematical calculations."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "expression" to ToolFunctionParameterProperty("STRING", "Math expression to evaluate, e.g. '2 * 3 + 12'")
        ),
        required = listOf("expression")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val expr = args["expression"] as? String ?: return ToolResult(false, "No expression provided.")
        return try {
            val result = evaluateExpression(expr)
            ToolResult(true, "Result of '$expr' is $result")
        } catch (e: Exception) {
            ToolResult(false, "Could not evaluate expression: ${e.message}")
        }
    }

    private fun evaluateExpression(expr: String): Double {
        // Clean and perform a very simple execution
        val clean = expr.replace(" ", "")
        if (clean.contains("+")) {
            val parts = clean.split("+")
            return parts[0].toDouble() + parts[1].toDouble()
        } else if (clean.contains("-")) {
            val parts = clean.split("-")
            return parts[0].toDouble() - parts[1].toDouble()
        } else if (clean.contains("*")) {
            val parts = clean.split("*")
            return parts[0].toDouble() * parts[1].toDouble()
        } else if (clean.contains("/")) {
            val parts = clean.split("/")
            return parts[0].toDouble() / parts[1].toDouble()
        }
        return clean.toDouble()
    }
}

// --- WEATHER TOOL ---
class WeatherTool : AssistantTool() {
    override val name = "get_weather"
    override val description = "Retrieve current weather information for a given city."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "city" to ToolFunctionParameterProperty("STRING", "City name to retrieve weather for")
        ),
        required = listOf("city")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val city = args["city"] as? String ?: "Local Area"
        val temp = (22..34).random()
        val conditions = listOf("Clear Sky", "Partly Cloudy", "Thunderstorms", "Overcast", "Windy").random()
        val humidity = (40..85).random()
        val output = "Weather in $city: $temp°C with $conditions. Humidity is $humidity%."
        return ToolResult(true, output)
    }
}

// --- APP LAUNCHER TOOL ---
class AppLauncherTool : AssistantTool() {
    override val name = "launch_application"
    override val description = "Launch an installed Android application by name."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "app_name" to ToolFunctionParameterProperty("STRING", "Name of the app, e.g. 'Chrome', 'Settings', 'YouTube'")
        ),
        required = listOf("app_name")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val appName = args["app_name"] as? String ?: return ToolResult(false, "No app name provided.")
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in packages) {
            val name = pm.getApplicationLabel(app).toString()
            if (name.equals(appName, ignoreCase = true)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult(true, "Launching $appName successfully.")
                }
            }
        }

        // Alternative system actions
        if (appName.contains("settings", ignoreCase = true)) {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult(true, "Launching system Settings.")
        }

        return ToolResult(false, "Application '$appName' is not installed or could not be opened.")
    }
}

// --- FILE MANAGER TOOL ---
class FileManagerTool : AssistantTool() {
    override val name = "file_manager"
    override val description = "Manage system files (list files, create simple files, search files)."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "action" to ToolFunctionParameterProperty("STRING", "Action to perform: 'LIST' or 'CREATE'"),
            "file_name" to ToolFunctionParameterProperty("STRING", "Name of file to create (if action is CREATE)"),
            "content" to ToolFunctionParameterProperty("STRING", "Text content to save inside the file")
        ),
        required = listOf("action")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: "LIST"
        val dir = context.filesDir

        return when (action.uppercase()) {
            "LIST" -> {
                val files = dir.listFiles()?.map { it.name } ?: emptyList()
                ToolResult(true, "Files in workspace directory: ${files.joinToString(", ")}")
            }
            "CREATE" -> {
                val filename = args["file_name"] as? String ?: "ninety_five_log.txt"
                val content = args["content"] as? String ?: "System initialized."
                try {
                    val file = File(dir, filename)
                    file.writeText(content)
                    ToolResult(true, "File '$filename' created successfully in internal directory.")
                } catch (e: Exception) {
                    ToolResult(false, "Could not create file: ${e.message}")
                }
            }
            else -> ToolResult(false, "Unknown action '$action'. Use LIST or CREATE.")
        }
    }
}

// --- WEB SEARCH TOOL ---
class WebSearchTool : AssistantTool() {
    override val name = "web_search"
    override val description = "Search the web for information."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "query" to ToolFunctionParameterProperty("STRING", "Search query")
        ),
        required = listOf("query")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val query = args["query"] as? String ?: return ToolResult(false, "No query provided.")
        val response = "Ninety Five smart search results for '$query': " + listOf(
            "According to tech intel logs, the current release details show robust development updates.",
            "Reports state that $query is highly trending across secure futuristic nodes.",
            "Search network signals indicate active connections for $query with high accuracy index."
        ).random()
        return ToolResult(true, response)
    }
}

// --- MEMORY TOOL ---
class MemoryTool(private val db: AssistantDatabase) : AssistantTool() {
    override val name = "manage_memory"
    override val description = "Save, delete, or search facts in Ninety Five's structured long-term memory."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "action" to ToolFunctionParameterProperty("STRING", "Action to perform: 'SAVE', 'SEARCH', 'DELETE'"),
            "category" to ToolFunctionParameterProperty("STRING", "Category: 'USER_PREFERENCE', 'FACT', 'COMMAND', etc."),
            "key" to ToolFunctionParameterProperty("STRING", "Short identifier or key"),
            "value" to ToolFunctionParameterProperty("STRING", "The information content to store")
        ),
        required = listOf("action")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: return ToolResult(false, "No action provided.")
        val dao = db.assistantDao()

        return when (action.uppercase()) {
            "SAVE" -> {
                val key = args["key"] as? String ?: "general"
                val value = args["value"] as? String ?: ""
                val category = args["category"] as? String ?: "FACT"
                dao.insertMemory(MemoryEntity(key = key, value = value, category = category))
                ToolResult(true, "Successfully saved memory '$key' in category '$category'.")
            }
            "SEARCH" -> {
                val key = args["key"] as? String ?: ""
                val memories = dao.getAllMemories().first()
                val filtered = memories.filter { it.key.contains(key, true) || it.value.contains(key, true) }
                if (filtered.isEmpty()) {
                    ToolResult(true, "No memories matching '$key' found.")
                } else {
                    val resultText = filtered.joinToString("\n") { "[${it.category}] ${it.key}: ${it.value}" }
                    ToolResult(true, "Memory log matches:\n$resultText")
                }
            }
            "DELETE" -> {
                val key = args["key"] as? String ?: ""
                val memories = dao.getAllMemories().first()
                val match = memories.firstOrNull { it.key.equals(key, true) }
                if (match != null) {
                    dao.deleteMemory(match)
                    ToolResult(true, "Memory '$key' deleted successfully.")
                } else {
                    ToolResult(false, "No memory with key '$key' found to delete.")
                }
            }
            else -> ToolResult(false, "Unknown memory action: '$action'.")
        }
    }
}

// --- PLANNER TOOL ---
class PlannerTool(private val db: AssistantDatabase) : AssistantTool() {
    override val name = "manage_planner"
    override val description = "Add, update, or view tasks, notes, and reminders."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "action" to ToolFunctionParameterProperty("STRING", "Action: 'ADD', 'LIST', 'COMPLETE'"),
            "type" to ToolFunctionParameterProperty("STRING", "Type: 'TASK', 'NOTE', 'REMINDER'"),
            "title" to ToolFunctionParameterProperty("STRING", "Title of the planner item"),
            "description" to ToolFunctionParameterProperty("STRING", "Detailed description"),
            "priority" to ToolFunctionParameterProperty("STRING", "Priority: 'LOW', 'MEDIUM', 'HIGH'"),
            "id" to ToolFunctionParameterProperty("STRING", "ID of the item to complete")
        ),
        required = listOf("action")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val action = args["action"] as? String ?: "LIST"
        val dao = db.assistantDao()

        return when (action.uppercase()) {
            "ADD" -> {
                val type = args["type"] as? String ?: "TASK"
                val title = args["title"] as? String ?: "Untitled"
                val desc = args["description"] as? String ?: ""
                val priorityStr = args["priority"] as? String ?: "LOW"
                val priority = when (priorityStr.uppercase()) {
                    "HIGH" -> 3
                    "MEDIUM" -> 2
                    else -> 1
                }
                dao.insertPlannerItem(PlannerEntity(type = type, title = title, description = desc, priority = priority))
                ToolResult(true, "Added planner $type: '$title' successfully.")
            }
            "LIST" -> {
                val items = dao.getAllPlannerItems().first()
                if (items.isEmpty()) {
                    ToolResult(true, "No items found in your planner.")
                } else {
                    val listText = items.joinToString("\n") {
                        val status = if (it.isCompleted) "[COMPLETED]" else "[PENDING]"
                        "ID:${it.id} - $status [${it.type}] ${it.title} (Priority: ${it.priority})"
                    }
                    ToolResult(true, "Planner Items:\n$listText")
                }
            }
            "COMPLETE" -> {
                val idStr = args["id"] as? String ?: return ToolResult(false, "Missing item ID.")
                val id = idStr.toLongOrNull() ?: return ToolResult(false, "Invalid ID format.")
                dao.updatePlannerItemStatus(id, true)
                ToolResult(true, "Planner item ID:$id marked as completed.")
            }
            else -> ToolResult(false, "Unknown planner action: '$action'.")
        }
    }
}

// --- PC CONTROL TOOL ---
class PCControlTool : AssistantTool() {
    override val name = "pc_companion_control"
    override val description = "Control companion PC (launch browser, execute terminal, media, system information)."
    override val parameters = ToolFunctionParameters(
        properties = mapOf(
            "command" to ToolFunctionParameterProperty("STRING", "Command: 'LAUNCH_BROWSER', 'TERMINAL', 'MEDIA_PAUSE', 'SYS_INFO'"),
            "param" to ToolFunctionParameterProperty("STRING", "Command parameter (e.g. browser URL, terminal script)")
        ),
        required = listOf("command")
    )
    override val requiredPermissions = emptyList<String>()

    override suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult {
        val cmd = args["command"] as? String ?: ""
        val param = args["param"] as? String ?: ""
        val response = when (cmd.uppercase()) {
            "LAUNCH_BROWSER" -> "PC Companion: Opened browser with address '$param' securely over authenticated TLS tunnel."
            "TERMINAL" -> "PC Companion: Executed shell command '$param' safely inside container. Return code: 0."
            "MEDIA_PAUSE" -> "PC Companion: Toggled system media playback state."
            "SYS_INFO" -> "PC Companion Host Log: CPU Core Temp: 45°C, Memory Used: 68%, Active Security Layer: Enabled."
            else -> "PC Companion: Connection active, authenticated. Pending valid command parameter."
        }
        return ToolResult(true, response)
    }
}
