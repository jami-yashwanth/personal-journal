package com.personal.journal.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Line icons matching the design (24-unit grid, round caps). Tint via Icon(tint = ...). */
object JournalIcons {
    private fun icon(name: String, vararg paths: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach {
            addPath(
                pathData = addPathNodes(it),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

    val Search = icon("search", "M11,4.5a6.5,6.5 0,1 1,0 13a6.5,6.5 0,1 1,0 -13z", "M20,20l-4.2,-4.2")
    val Close = icon("close", "M6,6l12,12", "M18,6L6,18")
    val Back = icon("back", "M15,5l-7,7l7,7")
    val ChevronRight = icon("chevron", "M9,5l7,7l-7,7")
    val Camera = icon("camera", "M4,8h3l2,-3h6l2,3h3v11H4z", "M12,9.5a3.5,3.5 0,1 1,0 7a3.5,3.5 0,1 1,0 -7z")
    val Gallery = icon("gallery", "M6,4h12a3,3 0,0 1,3 3v10a3,3 0,0 1,-3 3H6a3,3 0,0 1,-3 -3V7a3,3 0,0 1,3 -3z", "M9,8.4a1.6,1.6 0,1 1,0 3.2a1.6,1.6 0,1 1,0 -3.2z", "M21,16l-5,-5l-9,9")
    val Pen = icon("pen", "M4,20h4L19,9l-4,-4L4,16v4z")
    val FlashOff = icon("flash_off", "M13,3L5,14h6l-1,7l8,-11h-6l1,-7z", "M3,3l18,18")
    val FlashOn = icon("flash_on", "M13,3L5,14h6l-1,7l8,-11h-6l1,-7z")
    val Flip = icon("flip", "M20,11a8,8 0,0 0,-14.5 -4.5", "M5,3v4h4", "M4,13a8,8 0,0 0,14.5 4.5", "M19,21v-4h-4")
    val Settings = icon("settings", "M4,6h10", "M18,6h2", "M4,12h4", "M12,12h8", "M4,18h12", "M16,4a2,2 0,1 1,0 4a2,2 0,1 1,0 -4z", "M10,10a2,2 0,1 1,0 4a2,2 0,1 1,0 -4z", "M18,16a2,2 0,1 1,0 4a2,2 0,1 1,0 -4z")
    val Trash = icon("trash", "M4,7h16", "M9,7V4h6v3", "M6,7l1,13h10l1,-13")
    val Calendar = icon("calendar", "M6,5h12a3,3 0,0 1,3 3v10a3,3 0,0 1,-3 3H6a3,3 0,0 1,-3 -3V8a3,3 0,0 1,3 -3z", "M3,10h18", "M8,3v4", "M16,3v4")
    val Lock = icon("lock", "M7,11h10a2,2 0,0 1,2 2v6a2,2 0,0 1,-2 2H7a2,2 0,0 1,-2 -2v-6a2,2 0,0 1,2 -2z", "M8,11V8a4,4 0,0 1,8 0v3")
    val CameraOff = icon("camera_off", "M4,8h3l2,-3h6l2,3h3v11H4z", "M12,9.5a3.5,3.5 0,1 1,0 7a3.5,3.5 0,1 1,0 -7z", "M3,3l18,18")
}
