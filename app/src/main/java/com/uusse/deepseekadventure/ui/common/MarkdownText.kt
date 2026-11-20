package com.uusse.deepseekadventure.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 轻量 Markdown 渲染：代码块、行内代码、粗体、标题。
 * 不引入第三方库以降低构建风险；覆盖文字冒险最常见格式。
 */
@Composable
fun MarkdownText(text: String) {
    val annotated = buildAnnotatedString {
        val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = MaterialTheme.colorScheme.surface)
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("```", i) -> {
                    val end = text.indexOf("```", i + 3)
                    if (end == -1) {
                        append(text.substring(i))
                        break
                    }
                    withStyle(codeStyle) { append(text.substring(i + 3, end).trimStart('\n')) }
                    append("\n")
                    i = end + 3
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end == -1) {
                        append(text[i]); i++
                    } else {
                        withStyle(codeStyle) { append(text.substring(i + 1, end)) }
                        i = end + 1
                    }
                }
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end == -1) {
                        append(text[i]); i++
                    } else {
                        withStyle(boldStyle) { append(text.substring(i + 2, end)) }
                        i = end + 2
                    }
                }
                text[i] == '#' && (i == 0 || text[i - 1] == '\n') -> {
                    var j = i
                    while (j < text.length && text[j] == '#') j++
                    if (j < text.length && text[j] == ' ') {
                        withStyle(boldStyle) { append(text.substring(j + 1).substringBefore('\n')) }
                        val nl = text.indexOf('\n', j)
                        i = if (nl == -1) text.length else nl + 1
                        append("\n")
                    } else {
                        append(text[i]); i++
                    }
                }
                else -> {
                    append(text[i]); i++
                }
            }
        }
    }
    Text(text = annotated)
}
