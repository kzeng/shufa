package com.example.shufa.data

import kotlin.random.Random

object TidUtils {

    private const val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LENGTH = 4

    fun generate(existing: Set<String>): String {
        var tid: String
        do {
            tid = "#" + (1..LENGTH).map { CHARS[Random.nextInt(CHARS.length)] }.joinToString("")
        } while (tid in existing)
        return tid
    }
}