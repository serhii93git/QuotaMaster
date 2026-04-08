package com.quotamaster.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {

    private val icons: Map<String, ImageVector> = mapOf(
        "School"          to Icons.Default.School,
        "FitnessCenter"   to Icons.Default.FitnessCenter,
        "Code"            to Icons.Default.Code,
        "Book"            to Icons.Default.Book,
        "MusicNote"       to Icons.Default.MusicNote,
        "Palette"         to Icons.Default.Palette,
        "Language"        to Icons.Default.Language,
        "Work"            to Icons.Default.Work,
        "DirectionsRun"   to Icons.Default.DirectionsRun,
        "DirectionsBike"  to Icons.Default.DirectionsBike,
        "SelfImprovement" to Icons.Default.SelfImprovement,
        "Headphones"      to Icons.Default.Headphones,
        "SportsEsports"   to Icons.Default.SportsEsports,
        "Star"            to Icons.Default.Star
    )

    fun get(name: String): ImageVector =
        icons[name] ?: Icons.Default.Star

    fun allNames(): List<String> = icons.keys.toList()
}