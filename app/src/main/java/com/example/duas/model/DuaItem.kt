package com.example.duas.model

data class DuaItem(
    val id: String,
    val category: DuaCategory,
    val title: String,
    val arabicText: String,
    val sourceTakhrij: String,
    val virtue: String,
    val targetCount: Int = 1,
    val isFavorite: Boolean = false
)
