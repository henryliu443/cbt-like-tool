package com.henryliu.cbtreframe.shared

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

sealed class AIServiceError(
    override val message: String
) : Exception(message) {

    class NoAPIKey : AIServiceError("请先在设置中填写 API Key")
    class InvalidResponse : AIServiceError("AI 返回了无效的响应，请稍后重试")
    class NetworkError(val error: Throwable) : AIServiceError("网络错误：${error.message ?: ""}")
    class RateLimited : AIServiceError("请求过于频繁，请稍后再试")
    class InvalidKey : AIServiceError("API Key 无效，请检查设置")
    class HttpStatus(val code: Int) : AIServiceError("服务返回异常（$code）")
    class ParseError(val detail: String) : AIServiceError("解析响应失败：$detail")
    class InvalidSocraticOutput : AIServiceError("模型未返回有效的引导问题")
    class InvalidStructuredOutput(val detail: String) : AIServiceError(detail)

    val userFacingMessage: String
        get() = when (this) {
            is NoAPIKey -> "请先在设置中填写 API Key"
            is InvalidResponse -> "AI 返回了无效的响应，请稍后重试"
            is NetworkError -> {
                val cause = error
                if (cause is HttpRequestTimeoutException ||
                    cause is ConnectTimeoutException ||
                    cause is SocketTimeoutException ||
                    cause.message?.contains("timeout", ignoreCase = true) == true
                ) {
                    "请求超时，请稍后重试"
                } else if (cause.message?.contains("connect", ignoreCase = true) == true ||
                           cause.message?.contains("dns", ignoreCase = true) == true ||
                           cause.message?.contains("host", ignoreCase = true) == true ||
                           cause.message?.contains("network", ignoreCase = true) == true ||
                           cause is io.ktor.utils.io.errors.IOException
                ) {
                    "网络连接中断，请检查网络后重试"
                } else {
                    "网络错误，请稍后重试"
                }
            }
            is RateLimited -> "请求过于频繁，请稍后再试"
            is InvalidKey -> "API Key 无效或无权限，请检查设置"
            is HttpStatus -> {
                when (code) {
                    400 -> "请求参数或模型配置有误，请检查后重试"
                    401, 403 -> "API Key 无效或无权限，请检查设置"
                    429 -> "请求过于频繁，请稍后再试"
                    in 500..599 -> {
                        if (code == 501) {
                            "服务返回异常（$code），请稍后重试"
                        } else {
                            "服务暂时不可用，请稍后重试"
                        }
                    }
                    else -> "服务返回异常（$code），请稍后重试"
                }
            }
            is ParseError -> detail
            is InvalidSocraticOutput -> "模型未返回有效的引导问题，请重试或换用其他服务商。"
            is InvalidStructuredOutput -> detail
        }

    val isRetriable: Boolean
        get() = when (this) {
            is RateLimited -> true
            is HttpStatus -> code == 429 || (code in 500..599 && code != 501)
            is NetworkError -> {
                val cause = error
                cause is HttpRequestTimeoutException ||
                cause is ConnectTimeoutException ||
                cause is SocketTimeoutException ||
                cause is io.ktor.utils.io.errors.IOException ||
                cause.message?.contains("timeout", ignoreCase = true) == true ||
                cause.message?.contains("connect", ignoreCase = true) == true ||
                cause.message?.contains("dns", ignoreCase = true) == true ||
                cause.message?.contains("host", ignoreCase = true) == true ||
                cause.message?.contains("network", ignoreCase = true) == true
            }
            is InvalidSocraticOutput, is InvalidStructuredOutput -> true
            is NoAPIKey, is InvalidResponse, is InvalidKey, is ParseError -> false
        }

    companion object {
        fun classify(error: Throwable): AIServiceError {
            if (error is AIServiceError) return error
            return NetworkError(error)
        }
    }
}
