/**
 * Prepare the data for the app drawer, which is the list of all the installed applications.
 */

package com.github.gezimos.inkos.ui

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.github.gezimos.common.isSystemApp
import com.github.gezimos.common.showKeyboard
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.databinding.AdapterAppDrawerBinding
import com.github.gezimos.inkos.helper.dp2px
import kotlinx.coroutines.Job

class AppDrawerAdapter(
    val context: Context,
    private val flag: AppDrawerFlag,
    private val drawerGravity: Int,
    private val clickListener: (AppListItem) -> Unit,
    private val deleteListener: (AppListItem) -> Unit,
    private val renameListener: (String, String) -> Unit,
    private val showHideListener: (AppDrawerFlag, AppListItem) -> Unit,
    private val infoListener: (AppListItem) -> Unit,
    // Optional key navigation listener: (keyCode, adapterPosition) -> handled
    private val keyNavListener: ((Int, Int) -> Boolean)? = null,
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    init {
        // Enable stable ids to help RecyclerView/DiffUtil avoid unnecessary rebinds
        setHasStableIds(true)
    }

    // Per-instance job/scope so cancelBackgroundWork() only affects this adapter
    private val adapterJob = Job()

    // Cache a single Prefs instance and commonly used derived values to avoid
    // recreating Prefs and typefaces on every bind.
    private val prefs: Prefs = Prefs(context)
    private val appFilter = createAppFilter()
    // Use app-drawer specific prefs (fall back to shared app values inside Prefs)
    private val cachedTextSize: Float = prefs.appDrawerSize.toFloat()
    private val cachedPadding: Int = prefs.appDrawerGap
    private val cachedAppColor: Int = prefs.appColor
    private val cachedAllCaps: Boolean = prefs.allCapsApps
    private val cachedSmallCaps: Boolean = prefs.smallCapsApps
    private val cachedTypeface: Typeface? = prefs.getFontForContext("apps")
        .getFont(context, prefs.getCustomFontPathForContext("apps"))

    // Pool frequently used drawables to avoid repeated inflate/drawable allocation
    private val workProfileDrawable =
        androidx.core.content.ContextCompat.getDrawable(context, R.drawable.work_profile)
    var appsList: MutableList<AppListItem> = mutableListOf() // full list
    var appFilteredList: MutableList<AppListItem> = mutableListOf() // current page

    private var lastQuery: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Apply cached visual settings
        binding.appTitle.setTextColor(cachedAppColor)
        binding.appTitle.textSize = cachedTextSize
        binding.appTitle.setPadding(0, cachedPadding, 0, cachedPadding)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.isEmpty()) return
        val index =
            holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: position
        val appModel = appFilteredList.getOrNull(index) ?: return
        holder.bind(
            flag,
            drawerGravity,
            appModel,
            clickListener,
            infoListener,
            deleteListener,
            renameListener,
            showHideListener
        )
    }

    // Partial bind handler: when payload indicates only textual content changed,
    // avoid running the full bind which does allocations and listeners.
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            // If payload contains textual update marker, only update the visible text and work-profile icon
            if (payloads.contains(PAYLOAD_TEXT)) {
                val appModel = appFilteredList.getOrNull(position) ?: return
                val displayText =
                    if (appModel.customLabel.isNotEmpty()) appModel.customLabel else appModel.label
                holder.textView.text = when {
                    cachedAllCaps -> displayText.uppercase()
                    cachedSmallCaps -> displayText.lowercase()
                    else -> displayText
                }
                // update work profile icon presence quickly
                try {
                    if (appModel.user != android.os.Process.myUserHandle()) {
                        val icon = workProfileDrawable?.constantState?.newDrawable()?.mutate()
                        val px = dp2px(holder.itemView.resources, cachedTextSize.toInt())
                        icon?.setBounds(0, 0, px, px)
                        holder.textView.setCompoundDrawables(null, null, icon, null)
                        holder.textView.compoundDrawablePadding = 20
                    } else {
                        holder.textView.setCompoundDrawables(null, null, null, null)
                    }
                } catch (_: Exception) {
                    holder.textView.setCompoundDrawables(null, null, null, null)
                }
                return
            }
        }
        // Fallback to full bind
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getItemId(position: Int): Long {
        return appFilteredList.getOrNull(position)?.activityPackage?.hashCode()?.toLong()
            ?: position.toLong()
    }

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                lastQuery = charSearch?.toString() ?: ""
                val filterResults = FilterResults()
                filterResults.values = appsList  // Return all apps since we're removing search
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values is MutableList<*>) {
                    appFilteredList = results.values as MutableList<AppListItem>
                    // Use payload-based range update to only update text content on e-ink
                    notifyItemRangeChanged(0, appFilteredList.size.coerceAtLeast(0), PAYLOAD_TEXT)
                } else {
                    return
                }
            }
        }
    }

    /**
     * Update visible page using DiffUtil on a background thread for smooth transitions.
     */
    // For e-ink displays we prefer instant page swaps (no animated diffs).
    fun setPageAppsWithDiff(newPageApps: List<AppListItem>) {
        replacePage(newPageApps.toMutableList())
    }

    // Call this to cancel any running background tasks when adapter is no longer used
    fun cancelBackgroundWork() {
        adapterJob.cancel()
    }

    private fun sortAppList() {
        // Use AppListItem's natural ordering which relies on the repo-wide
        // collator configured to ignore accents and case differences.
        appsList.sort()
        appFilteredList.sort()
    }

    // Payload marker used to indicate only textual content changed
    private val PAYLOAD_TEXT = "payload_text"

    /**
     * Replace the current visible page with a new list instantly. Uses range notifications
     * with a text payload where possible to minimize rebind work on e-ink.
     */
    fun replacePage(newPage: MutableList<AppListItem>) {
        val oldSize = appFilteredList.size
        val newSize = newPage.size
        // Update backing list
        this.appFilteredList = newPage

        // Notify common prefix as changed with payload so onBind can optimize
        val common = kotlin.math.min(oldSize, newSize)
        if (common > 0) notifyItemRangeChanged(0, common, PAYLOAD_TEXT)

        // Handle inserts/removals without animations (insert/remove ranges)
        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize)
        } else if (newSize < oldSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize)
        }
    }

    inner class ViewHolder(private val binding: AdapterAppDrawerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val appHide: TextView = binding.appHide
        val appLock: TextView = binding.appLock
        val appRenameEdit: EditText = binding.appRenameEdit
        val appSaveRename: TextView = binding.appSaveRename
        val textView: TextView = binding.appTitle

        private val appHideLayout: LinearLayout = binding.appHideLayout
        private val appRenameLayout: LinearLayout = binding.appRenameLayout
        private val appRename: TextView = binding.appRename
        private val appClose: TextView = binding.appClose
        private val appInfo: TextView = binding.appInfo
        private val appDelete: TextView = binding.appDelete

        @SuppressLint("RtlHardcoded", "NewApi")
        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appListItem: AppListItem,
            appClickListener: (AppListItem) -> Unit,
            appInfoListener: (AppListItem) -> Unit,
            appDeleteListener: (AppListItem) -> Unit,
            renameListener: (String, String) -> Unit,
            showHideListener: (AppDrawerFlag, AppListItem) -> Unit
        ) {
            // Reuse adapter-level prefs and cached visuals
            val prefs = this@AppDrawerAdapter.prefs
            appHideLayout.visibility = View.GONE
            appRenameLayout.visibility = View.GONE

            fun setContextMenuOpen(open: Boolean) {
                try {
                    appHideLayout.isFocusable = open
                    appHideLayout.isFocusableInTouchMode = open
                    appRenameLayout.isFocusable = open
                    appRenameLayout.isFocusableInTouchMode = open
                    textView.isFocusable = !open
                    textView.isFocusableInTouchMode = !open
                    textView.isClickable = !open
                } catch (_: Exception) {
                }
            }

            val isAppDrawerSynthetic =
                appListItem.activityPackage == "com.inkos.internal.app_drawer"
            val isEmptySpaceSynthetic =
                appListItem.activityPackage == "com.inkos.internal.empty_space"
            val isNotificationsSynthetic =
                appListItem.activityPackage == "com.inkos.internal.notifications"
            val isSystemShortcut =
                com.github.gezimos.inkos.helper.SystemShortcutHelper.isSystemShortcut(appListItem.activityPackage)
            val isSyntheticApp =
                isAppDrawerSynthetic || isEmptySpaceSynthetic || isNotificationsSynthetic || isSystemShortcut

            // set show/hide icon
            if (flag == AppDrawerFlag.HiddenApps) {
                appHide.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.visibility, 0, 0)
                appHide.text = binding.root.context.getString(R.string.show)
            } else {
                appHide.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.visibility_off, 0, 0)
                appHide.text = binding.root.context.getString(R.string.hide)
            }

            val appName = appListItem.activityPackage
            val currentLockedApps = prefs.lockedApps

            if (isSyntheticApp) {
                appLock.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock_off, 0, 0)
                appLock.text = binding.root.context.getString(R.string.lock)
                appLock.alpha = 0.3f
            } else if (currentLockedApps.contains(appName)) {
                appLock.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock, 0, 0)
                appLock.text = binding.root.context.getString(R.string.unlock)
                appLock.alpha = 1.0f
            } else {
                appLock.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.padlock_off, 0, 0)
                appLock.text = binding.root.context.getString(R.string.lock)
                appLock.alpha = 1.0f
            }

            appRename.setOnClickListener {
                if (appListItem.activityPackage == "com.inkos.internal.empty_space") return@setOnClickListener
                if (appListItem.activityPackage.isNotEmpty()) {
                    appRenameEdit.setText(appListItem.customLabel.ifEmpty { appListItem.label })
                    appRenameLayout.visibility = View.VISIBLE
                    appHideLayout.visibility = View.GONE
                    setContextMenuOpen(true)
                    appRenameEdit.showKeyboard()
                    appRenameEdit.setSelection(appRenameEdit.text.length)
                }
            }

            // Toggle app lock state when lock button is pressed
            appLock.setOnClickListener {
                if (isSyntheticApp) return@setOnClickListener
                try {
                    val current = mutableSetOf<String>().apply { addAll(prefs.lockedApps) }
                    val pkg = appListItem.activityPackage
                    if (current.contains(pkg)) {
                        current.remove(pkg)
                    } else {
                        current.add(pkg)
                    }
                    prefs.lockedApps = current
                    // Refresh this item so drawable/text reflect new locked state
                    (bindingAdapter as AppDrawerAdapter).notifyItemChanged(absoluteAdapterPosition)
                } catch (_: Exception) {
                }
            }

            // Reuse TextWatcher stored in tag to avoid reallocating on every bind
            val watcherTag = R.id.appRenameEdit
            var watcher = appRenameEdit.getTag(watcherTag) as? TextWatcher
            if (watcher == null) {
                watcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(
                        s: CharSequence,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (appRenameEdit.text.isEmpty()) {
                            appSaveRename.text = binding.root.context.getString(R.string.reset)
                        } else if (appRenameEdit.text.toString() == appListItem.customLabel) {
                            appSaveRename.text = binding.root.context.getString(R.string.cancel)
                        } else {
                            appSaveRename.text = binding.root.context.getString(R.string.rename)
                        }
                    }
                }
                appRenameEdit.addTextChangedListener(watcher)
                appRenameEdit.setTag(watcherTag, watcher)
            }
            appRenameEdit.text = Editable.Factory.getInstance().newEditable(appListItem.label)

            appRenameEdit.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    val name = appRenameEdit.text.toString().trim()
                    appListItem.customLabel = name
                    (bindingAdapter as AppDrawerAdapter).notifyItemChanged(absoluteAdapterPosition)
                    renameListener(appListItem.activityPackage, name)
                    appRenameLayout.visibility = View.GONE
                    setContextMenuOpen(false)
                    true
                } else false
            }

            appSaveRename.setOnClickListener {
                val name = appRenameEdit.text.toString().trim()
                renameListener(appListItem.activityPackage, name)
                if (name.isEmpty()) {
                    appListItem.customLabel = ""
                    textView.text = when {
                        cachedAllCaps -> appListItem.activityLabel.uppercase()
                        cachedSmallCaps -> appListItem.activityLabel.lowercase()
                        else -> appListItem.activityLabel
                    }
                } else {
                    appListItem.customLabel = name
                    textView.text = when {
                        cachedAllCaps -> name.uppercase()
                        cachedSmallCaps -> name.lowercase()
                        else -> name
                    }
                }
                appRenameLayout.visibility = View.GONE
                setContextMenuOpen(false)
                (bindingAdapter as AppDrawerAdapter).sortAppList()
                (bindingAdapter as AppDrawerAdapter).notifyItemRangeChanged(
                    0,
                    (bindingAdapter as AppDrawerAdapter).appFilteredList.size,
                    PAYLOAD_TEXT
                )
            }

            // Main title setup
            val displayText =
                if (appListItem.customLabel.isNotEmpty()) appListItem.customLabel else appListItem.label
            textView.text = when {
                cachedAllCaps -> displayText.uppercase()
                cachedSmallCaps -> displayText.lowercase()
                else -> displayText
            }
            // Respect alignment preference passed from fragment (appLabelGravity)
            textView.gravity = appLabelGravity
            textView.textSize = cachedTextSize
            cachedTypeface?.let { textView.typeface = it }
            textView.setTextColor(cachedAppColor)

            // Work profile icon (pooled)
            try {
                if (appListItem.user != android.os.Process.myUserHandle()) {
                    val icon = workProfileDrawable?.constantState?.newDrawable()?.mutate()
                    val px = dp2px(binding.root.resources, cachedTextSize.toInt())
                    icon?.setBounds(0, 0, px, px)
                    textView.setCompoundDrawables(null, null, icon, null)
                    textView.compoundDrawablePadding = 20
                } else {
                    textView.setCompoundDrawables(null, null, null, null)
                }
            } catch (_: Exception) {
                textView.setCompoundDrawables(null, null, null, null)
            }

            val params = textView.layoutParams as FrameLayout.LayoutParams
            params.gravity = appLabelGravity
            textView.layoutParams = params
            val padding = dp2px(binding.root.resources, 24)
            // apply drawer-specific vertical gap as padding top/bottom
            textView.updatePadding(left = padding, right = padding, top = cachedPadding, bottom = cachedPadding)

            appHide.setOnClickListener {
                (bindingAdapter as AppDrawerAdapter).let { adapter ->
                    adapter.appFilteredList.removeAt(absoluteAdapterPosition)
                    adapter.notifyItemRemoved(absoluteAdapterPosition)
                    adapter.appsList.remove(appListItem)
                    showHideListener(flag, appListItem)
                    appHideLayout.visibility = View.GONE
                    adapter.filter.filter(adapter.lastQuery)
                }
            }

            textView.setOnClickListener { appClickListener(appListItem) }

            textView.setOnLongClickListener {
                val openApp = flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps
                if (openApp) {
                    try {
                        appDelete.alpha =
                            if (isSyntheticApp) 0.3f else if (binding.root.context.isSystemApp(
                                    appListItem.activityPackage
                                )
                            ) 0.3f else 1.0f
                        appInfo.alpha = if (isSyntheticApp) 0.3f else 1.0f
                        appHideLayout.visibility = View.VISIBLE
                        appRenameLayout.visibility = View.GONE
                        setContextMenuOpen(true)
                        try {
                            if (appInfo.isFocusable) appInfo.requestFocus() else appClose.requestFocus()
                        } catch (_: Exception) {
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                true
            }

            textView.setOnKeyListener { _, keyCode, event ->
                if (appHideLayout.visibility == View.VISIBLE || appRenameLayout.visibility == View.VISIBLE) return@setOnKeyListener true
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return@setOnKeyListener keyNavListener?.invoke(
                            keyCode,
                            absoluteAdapterPosition
                        ) ?: false
                    }
                }
                false
            }

            appInfo.setOnClickListener {
                if (!isSyntheticApp) appInfoListener(appListItem)
            }

            appDelete.setOnClickListener {
                if (!isSyntheticApp) appDeleteListener(appListItem)
            }

            appClose.setOnClickListener {
                appHideLayout.visibility = View.GONE
                appRenameLayout.visibility = View.GONE
                setContextMenuOpen(false)
            }
        }
    }
}