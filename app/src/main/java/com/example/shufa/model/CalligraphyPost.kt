package com.example.shufa.model

data class CalligraphyPost(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val style: CalligraphyStyle,
    val description: String,
    val imageUrls: List<String> = emptyList(),
    val characters: List<String> = emptyList()
)

enum class CalligraphyStyle(val label: String) {
    KAISHU("楷"),
    XINGSHU("行"),
    CAOSHU("草"),
    LISHU("隶"),
    ZHUANSHU("篆")
}
