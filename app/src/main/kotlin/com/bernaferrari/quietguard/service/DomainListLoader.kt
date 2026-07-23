package com.bernaferrari.quietguard.service

import java.io.File
import java.io.IOException

/** Parses hosts-style domain lists without leaking file or line-handling details into the VPN service. */
internal object DomainListLoader {
    fun load(
        file: File,
        requireExactlyTwoColumns: Boolean,
        onInvalidLine: (String) -> Unit,
    ): Set<String>? = try {
        file.bufferedReader().useLines { lines ->
            buildSet {
                lines.forEach { rawLine ->
                    val fields = rawLine.substringBefore('#').trim().split(Regex("\\s+"))
                    if (fields.size == 1 && fields.single().isEmpty()) return@forEach
                    val valid = if (requireExactlyTwoColumns) fields.size == 2 else fields.size > 1
                    if (valid) add(fields[1]) else onInvalidLine(rawLine.trim())
                }
            }
        }
    } catch (_: IOException) {
        null
    }
}
