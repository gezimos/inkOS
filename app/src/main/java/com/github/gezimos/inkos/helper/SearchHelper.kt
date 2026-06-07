package com.github.gezimos.inkos.helper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.webkit.MimeTypeMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import java.text.Normalizer
import java.util.Locale

object SearchHelper {
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val PUNCTUATION_REGEX = Regex("[-_+,. ]")
    private val PUNCTUATION_NO_SPACE_REGEX = Regex("[-_+,.]")


    private const val FREQ_PREF_KEY = "search_launch_frequency"
    private const val FREQ_MAX_PREFIXES = 200
    private const val FREQ_BOOST_PER_COUNT = 3
    private const val FREQ_MAX_BOOST = 30
    private const val FREQ_MAX_COUNT = 10

    @Volatile
    private var freqMap: MutableMap<String, MutableMap<String, Int>>? = null
    @Volatile
    private var decayApplied = false

    private fun loadFreqMap(context: android.content.Context): MutableMap<String, MutableMap<String, Int>> {
        freqMap?.let { return it }
        val prefs = context.getSharedPreferences("inkos_search", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(FREQ_PREF_KEY, null)
        val map: MutableMap<String, MutableMap<String, Int>> = if (json != null) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<MutableMap<String, MutableMap<String, Int>>>() {}.type
                com.google.gson.Gson().fromJson(json, type) ?: mutableMapOf()
            } catch (_: Exception) { mutableMapOf() }
        } else mutableMapOf()

        if (!decayApplied) {
            decayApplied = true
            var changed = false
            val prefixIterator = map.entries.iterator()
            while (prefixIterator.hasNext()) {
                val (_, pkgMap) = prefixIterator.next()
                val pkgIterator = pkgMap.entries.iterator()
                while (pkgIterator.hasNext()) {
                    val entry = pkgIterator.next()
                    val halved = entry.value / 2
                    if (halved == 0) { pkgIterator.remove(); changed = true }
                    else { entry.setValue(halved); changed = true }
                }
                if (pkgMap.isEmpty()) { prefixIterator.remove() }
            }
            if (changed) {
                val decayJson = com.google.gson.Gson().toJson(map)
                prefs.edit().putString(FREQ_PREF_KEY, decayJson).apply()
            }
        }

        freqMap = map
        return map
    }

    private fun saveFreqMap(context: android.content.Context) {
        val map = freqMap ?: return
        val json = com.google.gson.Gson().toJson(map)
        context.getSharedPreferences("inkos_search", android.content.Context.MODE_PRIVATE)
            .edit().putString(FREQ_PREF_KEY, json).apply()
    }

    fun recordLaunch(context: android.content.Context, query: String, packageName: String) {
        if (query.isBlank() || packageName.startsWith("com.inkos.internal")) return
        val prefix = normalizeString(query).take(2)
        if (prefix.length < 2) return
        val map = loadFreqMap(context)
        val pkgMap = map.getOrPut(prefix) { mutableMapOf() }
        pkgMap[packageName] = ((pkgMap[packageName] ?: 0) + 1).coerceAtMost(FREQ_MAX_COUNT)
        if (map.size > FREQ_MAX_PREFIXES) {
            map.entries.sortedBy { it.value.values.sum() }.take(map.size - FREQ_MAX_PREFIXES)
                .forEach { map.remove(it.key) }
        }
        saveFreqMap(context)
    }

    fun getFrequencyBoost(context: android.content.Context, normalizedQuery: String, packageName: String): Int {
        if (normalizedQuery.length < 2) return 0
        val prefix = normalizedQuery.take(2)
        val map = loadFreqMap(context)
        val count = map[prefix]?.get(packageName) ?: return 0
        return (count * FREQ_BOOST_PER_COUNT).coerceAtMost(FREQ_MAX_BOOST)
    }


    fun keyToChar(key: Key): Char? = when (key) {
        Key.A -> 'a'; Key.B -> 'b'; Key.C -> 'c'; Key.D -> 'd'
        Key.E -> 'e'; Key.F -> 'f'; Key.G -> 'g'; Key.H -> 'h'
        Key.I -> 'i'; Key.J -> 'j'; Key.K -> 'k'; Key.L -> 'l'
        Key.M -> 'm'; Key.N -> 'n'; Key.O -> 'o'; Key.P -> 'p'
        Key.Q -> 'q'; Key.R -> 'r'; Key.S -> 's'; Key.T -> 't'
        Key.U -> 'u'; Key.V -> 'v'; Key.W -> 'w'; Key.X -> 'x'
        Key.Y -> 'y'; Key.Z -> 'z'
        else -> null
    }

    fun appendCharToSearch(searchQuery: TextFieldValue, ch: Char): TextFieldValue {
        val newText = (searchQuery.text + ch).lowercase()
        return TextFieldValue(text = newText, selection = TextRange(newText.length))
    }

    fun backspaceSearch(searchQuery: TextFieldValue): TextFieldValue {
        if (searchQuery.text.isEmpty()) return searchQuery
        val newText = searchQuery.text.dropLast(1)
        return TextFieldValue(text = newText, selection = TextRange(newText.length))
    }

    fun keyEventToChar(event: KeyEvent): Char? {
        return try {
            val unicode = event.unicodeChar
            if (unicode == 0) return null
            val ch = unicode.toChar()
            val lower = ch.lowercaseChar()
            if (lower in 'a'..'z') lower else null
        } catch (_: Exception) {
            null
        }
    }


    /** Strict scoring — prefix and word-start contains only, no fuzzy. For music, files, settings. */
    fun scoreStrict(normalizedLabel: String, normalizedQuery: String, topScore: Int, originalLabel: String? = null): Int {
        if (normalizedLabel.startsWith(normalizedQuery)) return topScore
        if (!normalizedLabel.contains(normalizedQuery)) return 0
        if (originalLabel == null) return (topScore * 0.9).toInt()
        val spacedLabel = normalizeKeepSpaces(originalLabel)
        val stripped = normalizeString(originalLabel)
        var searchFrom = 0
        while (true) {
            val idx = stripped.indexOf(normalizedQuery, searchFrom)
            if (idx < 0) break
            if (idx == 0) return (topScore * 0.95).toInt()
            val spacedIdx = mapStrippedToSpaced(spacedLabel, idx)
            if (spacedIdx > 0 && spacedLabel[spacedIdx - 1] == ' ') return (topScore * 0.9).toInt()
            searchFrom = idx + 1
        }
        return 0
    }

    fun scorePreNormalized(normalizedLabel: String, normalizedQuery: String, topScore: Int, originalLabel: String? = null): Int {
        if (normalizedLabel.startsWith(normalizedQuery)) return topScore
        if (normalizedLabel.contains(normalizedQuery)) {
            if (originalLabel != null) {
                val spacedLabel = normalizeKeepSpaces(originalLabel)
                val idx = normalizedLabel.indexOf(normalizedQuery)
                if (idx > 0) {
                    val spacedIdx = mapStrippedToSpaced(spacedLabel, idx)
                    if (spacedIdx > 0 && spacedLabel[spacedIdx - 1] == ' ') {
                        return (topScore * 0.95).toInt() // word-start contains > mid-word contains
                    }
                }
            }
            return (topScore * 0.9).toInt()
        }
        val spacedLabel = if (originalLabel != null) normalizeKeepSpaces(originalLabel) else null
        val fuzzyScore = calculateFuzzyScore(normalizedLabel, normalizedQuery, spacedLabel)
        if (fuzzyScore < 0.4f) return 0
        return (fuzzyScore * topScore).toInt()
    }


    internal fun normalizeString(input: String): String {
        return input
            .uppercase(Locale.getDefault())
            .let { normalizeDiacritics(it) }
            .replace(PUNCTUATION_REGEX, "")
    }

    private fun normalizeKeepSpaces(input: String): String {
        val camelSpaced = buildString {
            for (i in input.indices) {
                if (i > 0 && input[i].isUpperCase() && input[i - 1].isLowerCase()) append(' ')
                append(input[i])
            }
        }
        return Normalizer.normalize(camelSpaced.uppercase(Locale.getDefault()), Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
            .replace(PUNCTUATION_NO_SPACE_REGEX, " ")
    }

    private fun mapStrippedToSpaced(spaced: String, strippedIndex: Int): Int {
        var count = 0
        for (i in spaced.indices) {
            if (spaced[i] != ' ') {
                if (count == strippedIndex) return i
                count++
            }
        }
        return spaced.length
    }

    private fun normalizeDiacritics(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(DIACRITICS_REGEX, "")
    }

    private fun calculateFuzzyScore(stripped: String, query: String, spacedLabel: String?): Float {
        val m = stripped.length
        val n = query.length
        if (n == 0 || m == 0) return 0f

        val currentPositions = IntArray(n)
        val bestScore = floatArrayOf(-1f)

        fun recurse(queryIdx: Int, labelIdx: Int, depth: Int) {
            if (queryIdx == n) {
                val score = scorePositions(currentPositions, stripped, query, spacedLabel)
                if (score > bestScore[0]) bestScore[0] = score
                return
            }
            if (depth > 5) {
                var idx = labelIdx
                for (qi in queryIdx until n) {
                    var found = false
                    while (idx < m) {
                        if (stripped[idx] == query[qi]) { currentPositions[qi] = idx; idx++; found = true; break }
                        idx++
                    }
                    if (!found) return
                }
                val score = scorePositions(currentPositions, stripped, query, spacedLabel)
                if (score > bestScore[0]) bestScore[0] = score
                return
            }
            for (j in labelIdx until m) {
                if (stripped[j] == query[queryIdx]) {
                    currentPositions[queryIdx] = j
                    recurse(queryIdx + 1, j + 1, depth + 1)
                }
            }
        }

        recurse(0, 0, 0)
        if (bestScore[0] < 0f) return 0f
        return bestScore[0]
    }

    private fun scorePositions(
        positions: IntArray, stripped: String, query: String, spacedLabel: String?
    ): Float {
        val n = positions.size
        val m = stripped.length
        val firstIdx = positions[0]
        val lastIdx = positions[n - 1]
        val span = lastIdx - firstIdx + 1

        val maxSpan = if (n <= 2) n * 4 else n * 3
        if (span > maxSpan) return -1f

        val baseScore = n.toFloat() / maxOf(m, n)
        val tightness = n.toFloat() / span
        var score = (baseScore * tightness).coerceAtMost(0.85f)

        if (spacedLabel == null) return score

        var boundaryMatches = 0
        var bonus = 0f
        for (i in 0 until n) {
            val pos = mapStrippedToSpaced(spacedLabel, positions[i])

            if (pos == 0) {
                bonus += 0.15f
                boundaryMatches++
            } else if (pos > 0 && spacedLabel[pos - 1] == ' ') {
                bonus += 0.15f
                boundaryMatches++
            }

            if (i > 0 && positions[i] == positions[i - 1] + 1) {
                bonus += 0.08f
            }
        }

        if (boundaryMatches == n) {
            score = score.coerceAtLeast(0.55f)
        }

        return (score + bonus).coerceAtMost(0.89f)
    }


    fun createWebSearchItem(query: String): AppListItem {
        return AppListItem(
            activityLabel = "Search web for \"$query\"",
            activityPackage = Constants.INTERNAL_WEB_SEARCH,
            activityClass = query,
            user = Process.myUserHandle(),
            customLabel = "",
            shortcutId = null
        )
    }

    fun launchWebSearch(context: Context, query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            return
        }
        val fallback = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (fallback.resolveActivity(context.packageManager) != null) {
            context.startActivity(fallback)
        }
    }
}


data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String?,
    val lookupKey: String,
    val photoUri: String? = null
)

object ContactsHelper {

    /** Sentinel key for local/phone contacts that have null account type. */
    const val LOCAL_ACCOUNT_KEY = "__local__"

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Returns distinct (accountKey → displayLabel) pairs for traditional contact sources only. */
    fun getAvailableAccounts(context: Context): List<Pair<String, String>> {
        if (!hasContactsPermission(context)) return emptyList()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Pair<String, String>>()
        try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContract.RawContacts.ACCOUNT_NAME),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null, null
            )?.use { cursor ->
                val typeCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                val nameCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                while (cursor.moveToNext()) {
                    val type = cursor.getString(typeCol)
                    if (!isTraditionalAccountType(type)) continue
                    val name = cursor.getString(nameCol)
                    val key = type ?: LOCAL_ACCOUNT_KEY
                    if (seen.add(key)) {
                        result.add(key to accountLabel(type, name))
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    /** Only allow phone, SIM, SDN, Google, and similar traditional contact sources. */
    private fun isTraditionalAccountType(accountType: String?): Boolean {
        if (accountType == null) return true // local/phone
        val lower = accountType.lowercase()
        return lower == "com.google" ||
            lower.contains("sim") ||
            lower.contains("sdn") ||
            lower.contains("phone") ||
            lower.contains("contact") ||
            lower.contains("exchange")
    }

    /** Human-readable label for an account type. */
    private fun accountLabel(accountType: String?, accountName: String?): String = when (accountType) {
        null -> "Phone"
        "com.google" -> "Google" + (if (!accountName.isNullOrBlank()) " ($accountName)" else "")
        else -> when {
            accountType.contains("sim", ignoreCase = true) -> "SIM" + (if (!accountName.isNullOrBlank() && accountName.contains("2")) " 2" else "")
            accountType.contains("sdn", ignoreCase = true) -> "SDN"
            accountType.contains("phone", ignoreCase = true) -> "Phone"
            accountType.contains("exchange", ignoreCase = true) -> "Exchange" + (if (!accountName.isNullOrBlank()) " ($accountName)" else "")
            else -> accountType.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
    private fun getAllowedContactIds(context: Context, allowedAccounts: Set<String>?): Set<String>? {
        if (allowedAccounts == null) return null
        val ids = mutableSetOf<String>()
        // Split into null-type (local) and non-null types
        val hasLocal = allowedAccounts.contains(LOCAL_ACCOUNT_KEY)
        val nonLocalTypes = allowedAccounts.filter { it != LOCAL_ACCOUNT_KEY }

        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (hasLocal) {
            clauses.add("${ContactsContract.RawContacts.ACCOUNT_TYPE} IS NULL")
        }
        if (nonLocalTypes.isNotEmpty()) {
            clauses.add("${ContactsContract.RawContacts.ACCOUNT_TYPE} IN (${nonLocalTypes.joinToString(",") { "?" }})")
            args.addAll(nonLocalTypes)
        }
        if (clauses.isEmpty()) return ids // empty set = no accounts selected

        val selection = "(${clauses.joinToString(" OR ")}) AND ${ContactsContract.RawContacts.DELETED} = 0"
        try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                selection,
                args.toTypedArray(),
                null
            )?.use { cursor ->
                val col = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getString(col)
                    if (id != null) ids.add(id)
                }
            }
        } catch (_: Exception) {}
        return ids
    }

    fun searchContacts(context: Context, query: String, listAll: Boolean = false, allowedAccounts: Set<String>? = null): List<ContactItem> {
        if (!hasContactsPermission(context)) return emptyList()
        if (query.isBlank() && !listAll) return emptyList()
        if (allowedAccounts != null && allowedAccounts.isEmpty()) return emptyList()

        val allowedIds = getAllowedContactIds(context, allowedAccounts)

        val scored = mutableListOf<Pair<ContactItem, Int>>()
        val contactsMap = mutableMapOf<String, ContactItem>()

        val contentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        )

        val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"

        try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameColumn = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val lookupKeyColumn = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                val photoUriColumn = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
                val normalizedQuery = if (!listAll) SearchHelper.normalizeString(query) else ""

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)
                    // Skip contacts not in allowed accounts
                    if (allowedIds != null && id !in allowedIds) continue
                    val name = cursor.getString(nameColumn) ?: continue
                    val score = if (listAll) 100 else SearchHelper.scorePreNormalized(SearchHelper.normalizeString(name), normalizedQuery, 100)
                    if (score == 0) continue
                    val lookupKey = cursor.getString(lookupKeyColumn) ?: id
                    val photoUri = cursor.getString(photoUriColumn)

                    if (!contactsMap.containsKey(id)) {
                        val contact = ContactItem(
                            id = id,
                            name = name,
                            phoneNumber = null,
                            lookupKey = lookupKey,
                            photoUri = photoUri
                        )
                        contactsMap[id] = contact
                        scored.add(contact to score)
                    }
                }
            }

            // Sort by score descending, take top results
            if (!listAll) {
                scored.sortByDescending { it.second }
                val top = scored.take(10)
                scored.clear()
                scored.addAll(top)
            }

            val contacts = scored.map { it.first }.toMutableList()

            if (contacts.isNotEmpty()) {
                val contactIds = contacts.map { it.id }
                val phoneProjection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN (${contactIds.joinToString(",")})"

                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    phoneProjection,
                    phoneSelection,
                    null,
                    null
                )?.use { phoneCursor ->
                    val contactIdColumn = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val numberColumn = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (phoneCursor.moveToNext()) {
                        val contactId = phoneCursor.getString(contactIdColumn)
                        val number = phoneCursor.getString(numberColumn)

                        contactsMap[contactId]?.let { contact ->
                            if (contact.phoneNumber == null) {
                                val index = contacts.indexOf(contact)
                                if (index >= 0) {
                                    contacts[index] = contact.copy(phoneNumber = number)
                                    contactsMap[contactId] = contacts[index]
                                }
                            }
                        }
                    }
                }
            }

            return contacts
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyList()
    }

    fun convertToAppListItem(contact: ContactItem): AppListItem {
        return AppListItem(
            activityLabel = contact.name,
            activityPackage = Constants.INTERNAL_CONTACT_PREFIX + contact.id,
            activityClass = contact.photoUri ?: "",
            user = Process.myUserHandle(),
            customLabel = "",
            shortcutId = null
        )
    }

    /** Query the real lookup key for a contact ID. Returns null if not found. */
    fun getLookupKey(context: Context, contactId: String): String? {
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.LOOKUP_KEY),
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getString(0)
            }
        } catch (_: Exception) {}
        return null
    }

    fun launchContactQuickActions(context: Context, contactId: String, lookupKey: String) {
        val contactUri = ContactsContract.Contacts.getLookupUri(contactId.toLongOrNull() ?: 0, lookupKey)

        val intents = mutableListOf<Intent>()

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = contactUri
        }
        intents.add(viewIntent)

        var phoneNumber: String? = null
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    phoneNumber = cursor.getString(numberColumn)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        phoneNumber?.let { number ->
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
            }
            intents.add(callIntent)

            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$number")
            }
            intents.add(smsIntent)
        }

        if (intents.isNotEmpty()) {
            val chooserIntent = Intent.createChooser(intents.removeAt(0), "Contact Action")
            if (intents.isNotEmpty()) {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            }
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }
}



object MusicSearchHelper {

    fun hasAudioPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun search(context: Context, query: String, limit: Int = 10): List<AppListItem> {
        if (!hasAudioPermission(context)) return emptyList()
        if (query.isBlank()) return emptyList()

        val scored = mutableListOf<Pair<AppListItem, Int>>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                val normalizedQuery = SearchHelper.normalizeString(query)
                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleCol) ?: continue
                    val normalizedTitle = SearchHelper.normalizeString(title)
                    val artist = cursor.getString(artistCol)
                    val titleScore = SearchHelper.scoreStrict(normalizedTitle, normalizedQuery, 100, title)
                    val artistScore = if (!artist.isNullOrBlank()) {
                        SearchHelper.scoreStrict(SearchHelper.normalizeString(artist), normalizedQuery, 100, artist)
                    } else 0
                    val bestScore = maxOf(titleScore, artistScore)
                    if (bestScore == 0) continue
                    val id = cursor.getLong(idCol)
                    val contentUri = Uri.withAppendedPath(uri, id.toString()).toString()
                    val label = if (!artist.isNullOrBlank() && artist != "<unknown>") {
                        "$title - $artist"
                    } else {
                        title
                    }
                    scored.add(
                        AppListItem(
                            activityLabel = label,
                            activityPackage = Constants.INTERNAL_MUSIC,
                            activityClass = contentUri,
                            user = Process.myUserHandle(),
                            customLabel = "",
                            shortcutId = null
                        ) to bestScore
                    )
                }
            }
        } catch (_: Exception) { }

        scored.sortByDescending { it.second }
        return scored.take(limit).map { it.first }
    }

    fun launch(context: Context, contentUriString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(contentUriString), "audio/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "No music player found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}



object FileSearchHelper {

    fun hasFolderAccess(context: Context): Boolean {
        return context.contentResolver.persistedUriPermissions.any { it.isReadPermission }
    }

    fun getFolderDisplayName(context: Context, treeUri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        try {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0) ?: docId
                }
            }
        } catch (_: Exception) { }
        return docId.substringAfterLast(':').substringAfterLast('/')
    }

    fun getPersistedFolders(context: Context): List<Pair<Uri, String>> {
        return context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { perm ->
                val uri = perm.uri
                uri to getFolderDisplayName(context, uri)
            }
    }

    fun search(context: Context, query: String, limit: Int = 10): List<AppListItem> {
        if (query.isBlank()) return emptyList()

        val scored = mutableListOf<Pair<AppListItem, Int>>()
        val normalizedQuery = SearchHelper.normalizeString(query)

        for (perm in context.contentResolver.persistedUriPermissions) {
            if (!perm.isReadPermission) continue
            val treeUri = perm.uri
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            try {
                searchTree(context, treeUri, rootDocId, normalizedQuery, scored, maxDepth = 3)
            } catch (_: Exception) { }
        }

        scored.sortByDescending { it.second }
        return scored.take(limit).map { it.first }
    }

    private fun searchTree(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        normalizedQuery: String,
        scored: MutableList<Pair<AppListItem, Int>>,
        maxDepth: Int
    ) {
        if (maxDepth <= 0) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        try {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null, null, null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: continue
                    val mime = cursor.getString(mimeCol) ?: continue

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        searchTree(context, treeUri, docId, normalizedQuery, scored, maxDepth - 1)
                    } else {
                        val score = SearchHelper.scoreStrict(SearchHelper.normalizeString(name), normalizedQuery, 100, name)
                        if (score > 0) {
                            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            scored.add(
                                AppListItem(
                                    activityLabel = name,
                                    activityPackage = Constants.INTERNAL_FILES,
                                    activityClass = documentUri.toString(),
                                    user = Process.myUserHandle(),
                                    customLabel = "",
                                    shortcutId = null
                                ) to score
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    fun launch(context: Context, documentUriString: String) {
        try {
            val uri = Uri.parse(documentUriString)
            val mimeType = getMimeType(context, uri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "No app found to open this file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(context: Context, uri: Uri): String {
        context.contentResolver.getType(uri)?.let { return it }
        val name = uri.lastPathSegment ?: return "*/*"
        val ext = name.substringAfterLast('.', "")
        if (ext.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())?.let { return it }
        }
        return "*/*"
    }

    fun removeFolderAccess(context: Context, treeUri: Uri) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { }
    }
}



object SettingsSearchHelper {

    private data class SettingsEntry(
        val keywords: List<String>,
        val normalizedKeywords: List<String>,
        val label: String,
        val normalizedLabel: String,
        val action: String
    )

    /** Build a SettingsEntry with pre-normalized keywords and label (computed once at init). */
    private fun entry(keywords: List<String>, label: String, action: String) = SettingsEntry(
        keywords = keywords,
        normalizedKeywords = keywords.map { SearchHelper.normalizeString(it) },
        label = label,
        normalizedLabel = SearchHelper.normalizeString(label),
        action = action
    )

    private val entries = listOf(
        entry(listOf("wifi", "wlan", "wireless", "network", "internet"), "Wi-Fi", Settings.ACTION_WIFI_SETTINGS),
        entry(listOf("bluetooth", "bt", "pair"), "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS),
        entry(listOf("airplane", "flight", "aeroplane"), "Airplane Mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        entry(listOf("data", "mobile", "cellular", "sim", "roaming"), "Mobile Data", Settings.ACTION_DATA_ROAMING_SETTINGS),
        entry(listOf("vpn"), "VPN", Settings.ACTION_VPN_SETTINGS),
        entry(listOf("nfc", "tap", "contactless"), "NFC", Settings.ACTION_NFC_SETTINGS),
        entry(listOf("hotspot", "tethering", "portable"), "Hotspot & Tethering", "android.settings.TETHERING_SETTINGS"),
        entry(listOf("cast", "miracast", "wireless display", "screen cast"), "Cast", Settings.ACTION_CAST_SETTINGS),
        entry(listOf("display", "screen", "brightness"), "Display", Settings.ACTION_DISPLAY_SETTINGS),
        entry(listOf("dpi", "density", "smallest width", "resolution", "scale"), "Display Size & DPI", Settings.ACTION_DISPLAY_SETTINGS),
        entry(listOf("font", "text size", "font size"), "Font Size", Settings.ACTION_DISPLAY_SETTINGS),
        entry(listOf("wallpaper", "background", "theme"), "Wallpaper", Intent.ACTION_SET_WALLPAPER),
        entry(listOf("dark mode", "dark theme", "night mode", "night light"), "Dark Mode", Settings.ACTION_DISPLAY_SETTINGS),
        entry(listOf("rotation", "auto rotate", "orientation"), "Auto-Rotate", Settings.ACTION_DISPLAY_SETTINGS),
        entry(listOf("sound", "volume", "ring", "audio", "vibrat"), "Sound", Settings.ACTION_SOUND_SETTINGS),
        entry(listOf("do not disturb", "dnd", "silent", "quiet"), "Do Not Disturb", Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS),
        entry(listOf("battery", "power", "charge", "saver"), "Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS),
        entry(listOf("storage", "disk", "space", "free"), "Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        entry(listOf("location", "gps"), "Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        entry(listOf("security", "lock", "password", "pin", "fingerprint", "biometric"), "Security", Settings.ACTION_SECURITY_SETTINGS),
        entry(listOf("privacy", "permission"), "Privacy", Settings.ACTION_PRIVACY_SETTINGS),
        entry(listOf("accessibility", "a11y", "talkback", "magnif"), "Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS),
        entry(listOf("language", "locale", "keyboard", "input", "ime"), "Language & Input", Settings.ACTION_INPUT_METHOD_SETTINGS),
        entry(listOf("date", "time", "clock", "timezone"), "Date & Time", Settings.ACTION_DATE_SETTINGS),
        entry(listOf("developer", "dev options", "usb debug", "debug", "adb", "oem unlock", "bootloader"), "Developer Options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        entry(listOf("usb", "otg", "connected devices"), "Connected Devices", Settings.ACTION_BLUETOOTH_SETTINGS),
        entry(listOf("apps", "application", "installed", "manage apps"), "Apps", Settings.ACTION_APPLICATION_SETTINGS),
        entry(listOf("notification", "alerts", "app notifications"), "App Notifications", "com.android.settings/com.android.settings.Settings\$NotificationAppListActivity"),
        entry(listOf("default app", "default browser", "default launcher"), "Default Apps", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        entry(listOf("account", "sync", "google", "email"), "Accounts", Settings.ACTION_SYNC_SETTINGS),
        entry(listOf("backup", "restore", "reset"), "Backup & Reset", Settings.ACTION_PRIVACY_SETTINGS),
        entry(listOf("about", "device", "info", "model", "build", "version", "android version"), "About Phone", Settings.ACTION_DEVICE_INFO_SETTINGS),
        entry(listOf("update", "system update", "software"), "System Update", "android.settings.SYSTEM_UPDATE_SETTINGS"),
        entry(listOf("memory", "ram", "memory usage"), "Memory Usage", "com.android.settings/com.android.settings.Settings\$AppMemoryUsageActivity"),
        entry(listOf("notification log", "notification history"), "Notification Log", "com.android.settings/com.android.settings.Settings\$NotificationStationActivity"),
        entry(listOf("sim lock", "sim pin", "sim card lock"), "SIM Lock", "com.android.settings/com.android.settings.Settings\$IccLockSettingsActivity"),
        entry(listOf("battery optimization", "battery exempt", "doze", "whitelist"), "Battery Optimization", "com.android.settings/com.android.settings.Settings\$HighPowerApplicationsActivity"),
        entry(listOf("data usage", "data consumption", "bandwidth"), "Data Usage", "com.android.settings/com.android.settings.Settings\$DataUsageSummaryActivity"),
        entry(listOf("mobile network", "network operator", "carrier", "apn"), "Mobile Network", "com.android.settings/com.android.settings.network.telephony.MobileNetworkActivity"),
        entry(listOf("storage manager", "free up space", "cleanup", "deletion helper"), "Storage Manager", "com.android.storagemanager/com.android.storagemanager.deletionhelper.DeletionHelperActivity"),
        entry(listOf("brightness dialog", "brightness slider", "brightness control"), "Brightness Dialog", "com.android.systemui/com.android.systemui.settings.brightness.BrightnessDialog"),
        entry(listOf("vision", "vision settings", "low vision", "screen reader"), "Vision Settings", "com.android.settings/com.android.settings.accessibility.AccessibilitySettingsForSetupWizardActivity"),
        entry(listOf("settings search", "search settings", "find setting"), "Settings Search", "android.settings.APP_SEARCH_SETTINGS"),
        entry(listOf("settings"), "Settings", Settings.ACTION_SETTINGS)
    )

    fun search(query: String, limit: Int = 3): List<AppListItem> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = SearchHelper.normalizeString(query)
        return entries
            .mapNotNull { entry ->
                val keywordScore = entry.normalizedKeywords.maxOf { normalizedKw ->
                    maxOf(
                        SearchHelper.scoreStrict(normalizedKw, normalizedQuery, 100),
                        SearchHelper.scoreStrict(normalizedQuery, normalizedKw, 100)
                    )
                }
                val labelScore = SearchHelper.scoreStrict(
                    entry.normalizedLabel, normalizedQuery, 100
                )
                val best = maxOf(keywordScore, labelScore)
                if (best > 0) entry to best else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (entry, _) -> createSettingsItem(entry.label, entry.action) }
    }

    private fun createSettingsItem(label: String, action: String): AppListItem {
        return AppListItem(
            activityLabel = label,
            activityPackage = Constants.INTERNAL_SETTINGS,
            activityClass = action,
            user = Process.myUserHandle(),
            customLabel = "",
            shortcutId = null
        )
    }

    fun launch(context: Context, action: String) {
        try {
            val intent = if (action.contains("/")) {
                // ComponentName format: "package/activity"
                val parts = action.split("/", limit = 2)
                Intent().apply {
                    component = android.content.ComponentName(parts[0], parts[1])
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(action).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) { }
        }
    }
}
