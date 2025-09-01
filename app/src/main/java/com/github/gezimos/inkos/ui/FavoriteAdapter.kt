package com.github.gezimos.inkos.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.AppDiffCallback

// Sealed class to represent different item types in the list
sealed class FavoriteListItem {
    data class AppItem(val app: AppListItem, val displayNumber: Int) : FavoriteListItem()
    object SeparatorItem : FavoriteListItem()
}

// Adapter to display Home Apps with page separators
class FavoriteAdapter(
    private val apps: MutableList<AppListItem>, // List of AppListItem objects
    private val onItemMoved: (fromPosition: Int, toPosition: Int) -> Unit,
    private val prefs: Prefs
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_APP = 0
        private const val VIEW_TYPE_SEPARATOR = 1
    }

    private var displayItems: List<FavoriteListItem> = emptyList()

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appTextView: TextView = itemView.findViewById(R.id.homeAppLabel)
    }

    inner class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is FavoriteListItem.AppItem -> VIEW_TYPE_APP
            is FavoriteListItem.SeparatorItem -> VIEW_TYPE_SEPARATOR
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_APP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.home_app_button, parent, false)
                AppViewHolder(view)
            }

            VIEW_TYPE_SEPARATOR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.favorite_separator, parent, false)
                SeparatorViewHolder(view)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is FavoriteListItem.AppItem -> {
                val appHolder = holder as AppViewHolder

                // Set the label text with position number (1-based indexing)
                val displayText = "${item.displayNumber}. ${item.app.label}"
                appHolder.appTextView.text = displayText

                // Set the text size and color dynamically using prefs
                appHolder.appTextView.setTextColor(prefs.appColor)
                appHolder.appTextView.textSize = prefs.appSize.toFloat()

                // Set the font from prefs.appsFont
                val font = prefs.appsFont.getFont(
                    appHolder.itemView.context,
                    prefs.getCustomFontPathForContext("apps")
                )
                if (font != null) {
                    appHolder.appTextView.typeface = font
                }

                // Set the gravity to align text to the left and ensure it's centered vertically
                appHolder.appTextView.gravity = Gravity.START or Gravity.CENTER_VERTICAL

                // Set drawable to the right side of the text
                val prefixDrawable: Drawable? =
                    ContextCompat.getDrawable(appHolder.itemView.context, R.drawable.ic_order_apps)
                appHolder.appTextView.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    prefixDrawable,
                    null
                )
            }

            is FavoriteListItem.SeparatorItem -> {
                val separatorHolder = holder as SeparatorViewHolder
                // Set the separator color to match app text color
                separatorHolder.itemView.setBackgroundColor(prefs.appColor)
            }
        }
    }

    override fun getItemCount(): Int = displayItems.size

    // Get the actual app position (excluding separators) for move operations
    private fun getActualAppPosition(displayPosition: Int): Int {
        var appCount = 0
        for (i in 0 until displayPosition) {
            if (displayItems[i] is FavoriteListItem.AppItem) {
                appCount++
            }
        }
        return appCount
    }

    // Convert actual app position to display position
    private fun getDisplayPosition(appPosition: Int): Int {
        var appCount = 0
        for (i in displayItems.indices) {
            if (displayItems[i] is FavoriteListItem.AppItem) {
                if (appCount == appPosition) {
                    return i
                }
                appCount++
            }
        }
        return -1
    }

    // Notify when an item is moved - only handle app items
    fun moveItem(fromPosition: Int, toPosition: Int) {
        // Only allow moving app items, not separators
        if (displayItems[fromPosition] !is FavoriteListItem.AppItem ||
            displayItems[toPosition] !is FavoriteListItem.AppItem
        ) {
            return
        }

        val actualFromPos = getActualAppPosition(fromPosition)
        val actualToPos = getActualAppPosition(toPosition)

        // Swap the apps in the underlying list
        val temp = apps[actualFromPos]
        apps[actualFromPos] = apps[actualToPos]
        apps[actualToPos] = temp

        // For the visual update during drag, just swap the display items
        val tempDisplayItem = displayItems[fromPosition]
        val newDisplayItems = displayItems.toMutableList()
        newDisplayItems[fromPosition] = newDisplayItems[toPosition]
        newDisplayItems[toPosition] = tempDisplayItem
        displayItems = newDisplayItems

        // Use notifyItemMoved for smooth animation during drag
        notifyItemMoved(fromPosition, toPosition)

        onItemMoved(actualFromPos, actualToPos)
    }

    // Method to rebuild display items after drag operation is complete
    fun finalizeMoveOperation() {
        buildDisplayItems()
        notifyDataSetChanged()
    }

    // Build the display items list with separators
    private fun buildDisplayItems() {
        val items = mutableListOf<FavoriteListItem>()
        val totalApps = apps.size
        val totalPages = prefs.homePagesNum

        if (totalPages <= 1) {
            // No separators needed for single page
            apps.forEachIndexed { index, app ->
                items.add(FavoriteListItem.AppItem(app, index + 1))
            }
        } else {
            // Calculate apps per page using the same logic as HomeFragment
            val appsPerPage = if (totalPages > 0) {
                (totalApps + totalPages - 1) / totalPages
            } else {
                0
            }

            apps.forEachIndexed { index, app ->
                items.add(FavoriteListItem.AppItem(app, index + 1))

                // Add separator after each complete page (except the last page)
                val isEndOfPage = (index + 1) % appsPerPage == 0
                val isNotLastApp = index < apps.size - 1
                val currentPage = index / appsPerPage
                val isNotLastPage = currentPage < totalPages - 1

                if (isEndOfPage && isNotLastApp && isNotLastPage) {
                    items.add(FavoriteListItem.SeparatorItem)
                }
            }
        }

        displayItems = items
    }

    // Update the list when the data changes
    fun updateList(newList: List<AppListItem>) {
        val diffCallback = AppDiffCallback(apps, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        apps.clear()
        apps.addAll(newList)
        buildDisplayItems() // Rebuild display items with separators
        diffResult.dispatchUpdatesTo(this)
    }

    // Initialize display items
    init {
        buildDisplayItems()
    }
}
