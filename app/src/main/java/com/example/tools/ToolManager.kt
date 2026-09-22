package com.example.tools

import android.content.Context
import com.example.data.AssistantDatabase

class ToolManager(db: AssistantDatabase) {
    val tools: List<AssistantTool> = listOf(
        TimeTool(),
        CalculatorTool(),
        WeatherTool(),
        AppLauncherTool(),
        FileManagerTool(),
        WebSearchTool(),
        MemoryTool(db),
        PlannerTool(db),
        PCControlTool()
    )

    fun getToolDeclarations(): List<com.example.ai.Tool> {
        val decls = tools.map { it.toDeclaration() }
        return listOf(com.example.ai.Tool(functionDeclarations = decls))
    }

    suspend fun executeTool(context: Context, name: String, args: Map<String, Any?>?): ToolResult {
        val tool = tools.find { it.name == name } ?: return ToolResult(false, "Tool '$name' not found in system manifest.")
        return try {
            tool.execute(context, args ?: emptyMap())
        } catch (e: Exception) {
            ToolResult(false, "Tool '$name' crashed during execution: ${e.message}", e.localizedMessage)
        }
    }
}
