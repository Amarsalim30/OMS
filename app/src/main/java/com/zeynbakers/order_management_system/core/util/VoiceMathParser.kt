package com.zeynbakers.order_management_system.core.util

import java.math.BigDecimal
import java.math.RoundingMode

data class VoiceMathParseResult(
    val value: BigDecimal,
    val expression: String
)

fun parseVoiceMath(transcript: String): VoiceMathParseResult? {
    val normalized = normalizeTranscript(transcript)
    if (normalized.isBlank()) return null
    parseFromTokens(normalized)?.let { return it }
    val fallback = normalizeFallback(transcript)
    if (fallback.isBlank()) return null
    return parseFromTokens(fallback)
}

private fun normalizeTranscript(input: String): String {
    val lowered = input.lowercase()
    val cleaned =
        lowered
            .replace(",", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace(Regex("(?<!\\d)\\.|\\.(?!\\d)"), " ")
            .replace("divided by", "/")
            .replace(Regex("\\bover\\b"), "/")
            .replace("multiplied by", "*")
            .replace(Regex("\\btime\\b"), "*")
            .replace(Regex("\\btimes\\b"), "*")
            .replace(Regex("\\bmultiply\\b"), "*")
            .replace(Regex("\\bx\\b"), "*")
            .replace(Regex("(?<=\\d)x(?=\\d)"), "*")
            .replace(Regex("\\bto\\b"), "two")
            .replace(Regex("\\btoo\\b"), "two")
            .replace(Regex("\\bfor\\b"), "four")
            .replace(Regex("\\bfore\\b"), "four")
            .replace(Regex("\\bate\\b"), "eight")
            .replace(Regex("\\bplus\\b"), "+")
            .replace(Regex("\\badd\\b"), "+")
            .replace(Regex("\\bminus\\b"), " - ")
            .replace(Regex("\\bsubtract\\b"), " - ")
            .replace(Regex("([a-z]+)-([a-z]+)"), "$1 $2")
            .replace(Regex("\\bcash\\b"), " ")
            .replace(Regex("\\bkes\\b"), " ")
            .replace("ksh", " ")
            .replace("kshs", " ")
            .replace("shillings", " ")
            .replace("shilling", " ")
            .replace("bob", " ")
            .replace("=", " ")
            .replace(Regex("([+*/])"), " $1 ")
            .replace(Regex("[^a-z0-9+*/.% ]"), " ")
    return cleaned.split(Regex("\\s+")).joinToString(" ")
}

private fun normalizeFallback(input: String): String {
    val cleaned =
        input.lowercase()
            .replace(",", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace(Regex("(?<!\\d)\\.|\\.(?!\\d)"), " ")
            .replace(Regex("(?<=\\d)x(?=\\d)"), "*")
            .replace(Regex("[^0-9+*/.% ]"), " ")
            .replace(Regex("([+*/])"), " $1 ")
    return cleaned.split(Regex("\\s+")).joinToString(" ")
}

private fun parseFromTokens(normalized: String): VoiceMathParseResult? {
    val tokens = normalized.split(" ").filter { it.isNotBlank() }
    val parsed = tokenize(tokens) ?: return null
    val prepared = applyPercentShorthand(parsed)
    val result = evaluateExpression(prepared) ?: return null
    return VoiceMathParseResult(result, normalized)
}

private sealed class Token {
    data class Number(val value: BigDecimal) : Token()
    data class Percent(val value: BigDecimal) : Token()
    data class Operator(val op: Char) : Token()
}

private val IGNORE_WORDS = setOf("cash", "kes", "ksh", "kshs", "shillings", "shilling", "bob")

private fun tokenize(tokens: List<String>): List<Token>? {
    val output = mutableListOf<Token>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token.isBlank()) {
            i++
            continue
        }
        if (IGNORE_WORDS.contains(token)) {
            i++
            continue
        }
        when (token) {
            "+", "-", "*", "/" -> {
                output.add(Token.Operator(token[0]))
                i++
            }
            "and" -> {
                val hasNumber =
                    output.lastOrNull() is Token.Number || output.lastOrNull() is Token.Percent
                val nextParsed = if (i + 1 < tokens.size) parseNumberToken(tokens, i + 1) else null
                if (hasNumber && nextParsed != null) {
                    output.add(Token.Operator('+'))
                }
                i++
            }
            "percent" -> {
                // Percent without a number is invalid.
                return null
            }
            "of" -> {
                i++
            }
            else -> {
                val parsed = parseNumberToken(tokens, i) ?: return null
                val number = parsed.first
                i = parsed.second
                if (i < tokens.size && tokens[i] == "percent") {
                    i++
                    if (i < tokens.size && tokens[i] == "of") {
                        i++
                        output.add(Token.Number(number.divide(BigDecimal(100))))
                        output.add(Token.Operator('*'))
                    } else {
                        output.add(Token.Percent(number))
                    }
                } else {
                    output.add(Token.Number(number))
                }
            }
        }
    }
    return output
}

private fun applyPercentShorthand(tokens: List<Token>): List<Token> {
    if (tokens.size < 3) return tokens
    val output = mutableListOf<Token>()
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token is Token.Number &&
            i + 2 < tokens.size &&
            tokens[i + 1] is Token.Operator &&
            tokens[i + 2] is Token.Percent
        ) {
            val op = tokens[i + 1] as Token.Operator
            val percent = (tokens[i + 2] as Token.Percent).value
            val applied = token.value.multiply(percent).divide(BigDecimal(100))
            output.add(token)
            output.add(op)
            output.add(Token.Number(applied))
            i += 3
        } else if (token is Token.Percent) {
            output.add(
                Token.Number(token.value.divide(BigDecimal(100)))
            )
            i++
        } else {
            output.add(token)
            i++
        }
    }
    return output
}

private fun evaluateExpression(tokens: List<Token>): BigDecimal? {
    val values = ArrayDeque<BigDecimal>()
    val ops = ArrayDeque<Char>()

    fun applyOp() {
        if (values.size < 2 || ops.isEmpty()) return
        val right = values.removeLast()
        val left = values.removeLast()
        val op = ops.removeLast()
        val result =
            when (op) {
                '+' -> left + right
                '-' -> left - right
                '*' -> left * right
                '/' -> if (right.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else left.divide(right, 6, RoundingMode.HALF_UP)
                else -> left
            }
        values.addLast(result)
    }

    fun precedence(op: Char): Int = if (op == '*' || op == '/') 2 else 1

    tokens.forEach { token ->
        when (token) {
            is Token.Number -> values.addLast(token.value)
            is Token.Operator -> {
                while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(token.op)) {
                    applyOp()
                }
                ops.addLast(token.op)
            }
            is Token.Percent -> {
                values.addLast(token.value.divide(BigDecimal(100)))
            }
        }
    }
    while (ops.isNotEmpty()) {
        applyOp()
    }
    return values.lastOrNull()
}

private fun parseNumberToken(tokens: List<String>, start: Int): Pair<BigDecimal, Int>? {
    val token = tokens[start]
    val numeric = token.toBigDecimalOrNull()
    if (numeric != null) {
        return numeric to (start + 1)
    }
    return parseNumberWords(tokens, start)
}

private fun parseNumberWords(tokens: List<String>, start: Int): Pair<BigDecimal, Int>? {
    var total = 0L
    var current = 0L
    var i = start
    var sawNumber = false
    var decimalPart = ""
    var inDecimal = false

    fun flushScale(scale: Long) {
        if (current == 0L) current = 1L
        total += current * scale
        current = 0L
    }

    while (i < tokens.size) {
        val token = tokens[i]
        val value = WORD_NUMBERS[token]
        when {
            token == "and" -> {
                i++
            }
            token == "point" -> {
                inDecimal = true
                sawNumber = true
                i++
            }
            inDecimal && value != null -> {
                decimalPart += value.toString()
                i++
            }
            value != null -> {
                sawNumber = true
                current += value
                i++
            }
            token == "hundred" -> {
                sawNumber = true
                if (current == 0L) current = 1L
                current *= 100
                i++
            }
            token in SCALES -> {
                sawNumber = true
                flushScale(SCALES.getValue(token))
                i++
            }
            else -> break
        }
    }

    if (!sawNumber) return null
    val combined = total + current
    val base = BigDecimal(combined)
    val result =
        if (decimalPart.isNotEmpty()) {
            base + BigDecimal("0.$decimalPart")
        } else {
            base
        }
    return result to i
}

private val WORD_NUMBERS = mapOf(
    "zero" to 0L,
    "one" to 1L,
    "two" to 2L,
    "three" to 3L,
    "four" to 4L,
    "five" to 5L,
    "six" to 6L,
    "seven" to 7L,
    "eight" to 8L,
    "nine" to 9L,
    "ten" to 10L,
    "eleven" to 11L,
    "twelve" to 12L,
    "thirteen" to 13L,
    "fourteen" to 14L,
    "fifteen" to 15L,
    "sixteen" to 16L,
    "seventeen" to 17L,
    "eighteen" to 18L,
    "nineteen" to 19L,
    "twenty" to 20L,
    "thirty" to 30L,
    "forty" to 40L,
    "fifty" to 50L,
    "sixty" to 60L,
    "seventy" to 70L,
    "eighty" to 80L,
    "ninety" to 90L
)

private val SCALES = mapOf(
    "thousand" to 1_000L,
    "million" to 1_000_000L
)
