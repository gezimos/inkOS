package com.github.gezimos.inkos.helper

import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
object CalendarEventsHelper {

    data class CalendarEvent(
        val eventId: Long,
        val title: String,
        val location: String?,
        val beginTime: Long,
        val endTime: Long
    )

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    fun loadCalendars(context: Context): List<Pair<Long, String>> {
        if (!hasCalendarPermission(context)) return emptyList()
        val cr = context.contentResolver
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1"
        val uri = CalendarContract.Calendars.CONTENT_URI
        return try {
            cr.query(uri, projection, selection, null, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC")
                ?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                    val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                    if (idIdx < 0 || nameIdx < 0) return emptyList()
                    val list = mutableListOf<Pair<Long, String>>()
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val name = cursor.getString(nameIdx) ?: "Calendar $id"
                        list.add(id to name)
                    }
                    list
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Sentinel for "All calendars" - load events from all visible calendars. */
    const val ALL_CALENDARS_ID: Long = -2
    fun loadUpcomingEvents(context: Context, calendarId: Long, filterIndex: Int): List<CalendarEvent> {
        if (!hasCalendarPermission(context)) return emptyList()
        if (calendarId == -1L) return emptyList()
        val days = when (filterIndex) {
            0 -> 1
            1 -> 7
            2 -> 14
            3 -> 30
            else -> 7
        }
        val now = System.currentTimeMillis()
        val endMillis = now + (days * 24L * 60 * 60 * 1000)
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, now)
        android.content.ContentUris.appendId(builder, endMillis)
        val uri = builder.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )
        val (selection, selectionArgs) = if (calendarId == ALL_CALENDARS_ID) {
            null to null
        } else {
            "${CalendarContract.Instances.CALENDAR_ID} = ?" to arrayOf(calendarId.toString())
        }
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        return try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val eventIdIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                if (eventIdIdx < 0 || titleIdx < 0 || beginIdx < 0 || endIdx < 0) return emptyList()
                val list = mutableListOf<CalendarEvent>()
                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(eventIdIdx)
                    val title = cursor.getString(titleIdx) ?: ""
                    val location = if (locIdx >= 0) cursor.getString(locIdx)?.takeIf { it.isNotBlank() } else null
                    val begin = cursor.getLong(beginIdx)
                    val end = cursor.getLong(endIdx)
                    list.add(CalendarEvent(eventId = eventId, title = title, location = location, beginTime = begin, endTime = end))
                }
                list
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
