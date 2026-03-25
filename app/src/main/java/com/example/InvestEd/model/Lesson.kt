// model/Lesson.kt
package com.example.InvestEd.model

data class Lesson(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val icon: String = "📚",
    val duration: String = "5 min read",
    val points: Int = 10,
    val order: Int = 0,
    val color: String = "mod-navy",
    val enabled: Boolean = true,
    val completions: Int = 0
)