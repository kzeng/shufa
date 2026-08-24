package com.example.shufa.model

data class CalligraphyPost(
    val id: String,
    val title: String,
    val author: String,
    val dynasty: String,
    val style: CalligraphyStyle,
    val description: String,
    val imageUrl: String,
    val characters: List<String> = emptyList()
)

enum class CalligraphyStyle(val label: String) {
    KAISHU("楷书"),
    XINGSHU("行书"),
    CAOSHU("草书"),
    LISHU("隶书"),
    ZHUANSHU("篆书")
}
