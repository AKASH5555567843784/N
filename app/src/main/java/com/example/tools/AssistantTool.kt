package com.example.tools

import android.content.Context
import com.example.ai.ToolFunctionDeclaration
import com.example.ai.ToolFunctionParameters
import com.example.ai.ToolFunctionParameterProperty

abstract class AssistantTool {
    abstract val name: String
    abstract val description: String
    abstract val parameters: ToolFunctionParameters?
    abstract val requiredPermissions: List<String>

    abstract suspend fun execute(context: Context, args: Map<String, Any?>): ToolResult

    fun toDeclaration(): ToolFunctionDeclaration {
        return ToolFunctionDeclaration(
            name = name,
            description = description,
            parameters = parameters
        )
    }
}

data class ToolResult(
    val isSuccess: Boolean,
    val output: String,
    val error: String? = null
)
