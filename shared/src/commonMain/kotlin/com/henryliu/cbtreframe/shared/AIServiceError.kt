package com.henryliu.cbtreframe.shared

sealed class AIServiceError(
    override val message: String,
    val isRetriable: Boolean = false,
) : Exception(message) {

    class NoAPIKey : AIServiceError("请先在设置中填写 API Key")
    class InvalidResponse : AIServiceError("AI 返回了无效的响应，请稍后重试")
    class RateLimited : AIServiceError("请求过于频繁，请稍后再试", isRetriable = true)
    class InvalidKey : AIServiceError("API Key 无效或无权限，请检查设置")
    class HttpStatus(val code: Int) : AIServiceError(
        "服务返回异常（$code）",
        isRetriable = code == 429 || code in 500..504
    )
    class ParseError(val detail: String) : AIServiceError(detail)
    class InvalidSocraticOutput : AIServiceError(
        "模型未返回有效的引导问题，请重试或换用其他服务商。",
        isRetriable = true
    )
    class InvalidStructuredOutput(detail: String) : AIServiceError(detail, isRetriable = true)
}
