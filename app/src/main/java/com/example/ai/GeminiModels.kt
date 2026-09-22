package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class SystemInstruction(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class ToolFunctionParameterProperty(
    val type: String,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ToolFunctionParameters(
    val type: String = "OBJECT",
    val properties: Map<String, ToolFunctionParameterProperty>,
    val required: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ToolFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: ToolFunctionParameters? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    val functionDeclarations: List<ToolFunctionDeclaration>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null
)

@JsonClass(generateAdapter = true)
data class FunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class CandidatePart(
    val text: String? = null,
    val functionCall: FunctionCall? = null
)

@JsonClass(generateAdapter = true)
data class CandidateContent(
    val parts: List<CandidatePart>? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: CandidateContent? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)
