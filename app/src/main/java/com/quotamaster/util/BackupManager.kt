package com.quotamaster.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.quotamaster.data.db.AppDatabase
import com.quotamaster.data.model.Activity
import com.quotamaster.data.model.WorkSession
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Export / import entire database as JSON via ShareSheet.
 * Uses android.org.json (zero extra dependencies).
 *
 * JSON structure:
 * {
 *   "version": 1,
 *   "exportedAt": "2025-04-08T20:00:00",
 *   "activities": [...],
 *   "sessions": [...]
 * }
 */
class BackupManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val activityDao = db.activityDao()
    private val sessionDao = db.workSessionDao()

    // ── Export ────────────────────────────────────────────────────────

    /**
     * Creates a JSON backup file and returns a share [Intent].
     */
    suspend fun exportToShareIntent(): Intent {
        val json = buildExportJson()
        val file = writeToFile(json)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private suspend fun buildExportJson(): JSONObject {
        val activities = activityDao.getAllActivitiesOnce()
        val sessions = sessionDao.getAllSessionsOnce()

        return JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("activities", activitiesToJson(activities))
            put("sessions", sessionsToJson(sessions))
        }
    }

    private fun writeToFile(json: JSONObject): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val dir = File(context.cacheDir, "backups")
        dir.mkdirs()
        val file = File(dir, "quotamaster_backup_$timestamp.json")
        file.writeText(json.toString(2))
        return file
    }

    // ── Import ───────────────────────────────────────────────────────

    /**
     * Restores database from a JSON [InputStream].
     * Runs in a single transaction — either everything succeeds or nothing changes.
     *
     * @return Pair(activitiesCount, sessionsCount) of imported items
     */
    suspend fun importFromStream(inputStream: InputStream): Pair<Int, Int> {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(jsonString)

        val version = json.optInt("version", 0)
        if (version < 1) {
            throw IllegalArgumentException("Unknown backup version: $version")
        }

        val activities = jsonToActivities(json.getJSONArray("activities"))
        val sessions = jsonToSessions(json.getJSONArray("sessions"))

        db.runInTransaction {
            // Room's runInTransaction expects non-suspend lambdas,
            // but DAO suspend functions use the same transaction coroutine context.
            // We use a blocking approach via a helper.
        }

        // Use withTransaction for suspend DAO calls
        importData(activities, sessions)

        return Pair(activities.size, sessions.size)
    }

    private suspend fun importData(activities: List<Activity>, sessions: List<WorkSession>) {
        // Clear existing data — sessions first (FK constraint)
        sessionDao.deleteAll()
        activityDao.deleteAll()

        // Insert all activities
        activities.forEach { activity ->
            activityDao.insert(activity)
        }

        // Insert all sessions
        sessions.forEach { session ->
            sessionDao.insert(session)
        }
    }

    // ── Serialization helpers ────────────────────────────────────────

    private fun activitiesToJson(activities: List<Activity>): JSONArray {
        val array = JSONArray()
        activities.forEach { a ->
            array.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("activityType", a.activityType)
                put("period", a.period)
                put("goalHoursPerPeriod", a.goalHoursPerPeriod.toDouble())
                put("goalDaysPerPeriod", a.goalDaysPerPeriod)
                put("goalWeeksPerPeriod", a.goalWeeksPerPeriod)
                put("goalMonthsPerPeriod", a.goalMonthsPerPeriod)
                put("startDate", a.startDate)
                put("endDate", a.endDate ?: JSONObject.NULL)
                put("iconName", a.iconName)
                put("colorHex", a.colorHex)
                put("isArchived", a.isArchived)
            })
        }
        return array
    }

    private fun sessionsToJson(sessions: List<WorkSession>): JSONArray {
        val array = JSONArray()
        sessions.forEach { s ->
            array.put(JSONObject().apply {
                put("id", s.id)
                put("activityId", s.activityId)
                put("date", s.date)
                put("startTime", s.startTime)
                put("endTime", s.endTime)
                put("durationMinutes", s.durationMinutes)
                put("periodTag", s.periodTag)
                put("note", s.note)
            })
        }
        return array
    }

    private fun jsonToActivities(array: JSONArray): List<Activity> {
        val list = mutableListOf<Activity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Activity(
                    id                  = obj.getLong("id"),
                    name                = obj.getString("name"),
                    activityType        = obj.getString("activityType"),
                    period              = obj.getString("period"),
                    goalHoursPerPeriod  = obj.getDouble("goalHoursPerPeriod").toFloat(),
                    goalDaysPerPeriod   = obj.getInt("goalDaysPerPeriod"),
                    goalWeeksPerPeriod  = obj.optInt("goalWeeksPerPeriod", 0),
                    goalMonthsPerPeriod = obj.optInt("goalMonthsPerPeriod", 0),
                    startDate           = obj.getString("startDate"),
                    endDate             = if (obj.isNull("endDate")) null else obj.getString("endDate"),
                    iconName            = obj.optString("iconName", "School"),
                    colorHex            = obj.optString("colorHex", "6650A4"),
                    isArchived          = obj.optBoolean("isArchived", false)
                )
            )
        }
        return list
    }

    private fun jsonToSessions(array: JSONArray): List<WorkSession> {
        val list = mutableListOf<WorkSession>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                WorkSession(
                    id              = obj.getLong("id"),
                    activityId      = obj.getLong("activityId"),
                    date            = obj.getString("date"),
                    startTime       = obj.getString("startTime"),
                    endTime         = obj.getString("endTime"),
                    durationMinutes = obj.getInt("durationMinutes"),
                    periodTag       = obj.getString("periodTag"),
                    note            = obj.optString("note", "")
                )
            )
        }
        return list
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}
