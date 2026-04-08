package com.quotamaster.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.quotamaster.data.model.Activity
import com.quotamaster.data.model.WorkSession
import java.io.File
import java.time.LocalDate

object CsvExporter {

    fun export(
        context: Context,
        sessions: List<WorkSession>,
        activities: List<Activity>
    ): Uri? {
        val activityMap = activities.associateBy { it.id }
        val dir = File(context.cacheDir, "exports")
        dir.mkdirs()
        val file = File(dir, "quotamaster_${LocalDate.now()}.csv")

        file.bufferedWriter().use { w ->
            w.write("Activity,Date,Start,End,Duration (min),Note")
            w.newLine()
            sessions.forEach { s ->
                val name = activityMap[s.activityId]?.name?.replace("\"", "\"\"") ?: "Unknown"
                val note = s.note.replace("\"", "\"\"")
                w.write("\"${name}\",${s.date},${s.startTime},${s.endTime},${s.durationMinutes},\"${note}\"")
                w.newLine()
            }
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}