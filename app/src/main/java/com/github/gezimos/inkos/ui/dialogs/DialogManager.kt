package com.github.gezimos.inkos.ui.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getTrueSystemFont
import com.github.gezimos.inkos.helper.loadFile
import com.github.gezimos.inkos.helper.storeFile
import com.github.gezimos.inkos.helper.utils.AppReloader

class DialogManager(val context: Context, val activity: Activity) {

    private lateinit var prefs: Prefs

    var backupRestoreDialog: LockedBottomSheetDialog? = null
    var sliderDialog: AlertDialog? = null
    var singleChoiceDialog: AlertDialog? = null
    var multiChoiceDialog: AlertDialog? = null
    var colorPickerDialog: AlertDialog? = null

    fun showBackupRestoreDialog() {
        backupRestoreDialog?.dismiss()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        fun makeItem(text: String, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                this.text = text
                val padding = (12 * context.resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                setOnClickListener {
                    onClick()
                    backupRestoreDialog?.dismiss()
                }
            }
        }

        layout.addView(makeItem(context.getString(R.string.advanced_settings_backup_restore_backup)) {
            storeFile(activity, Constants.BackupType.FullSystem)
        })
        layout.addView(makeItem(context.getString(R.string.advanced_settings_backup_restore_restore)) {
            loadFile(activity, Constants.BackupType.FullSystem)
        })
        layout.addView(makeItem(context.getString(R.string.advanced_settings_backup_restore_clear)) {
            confirmClearData()
        })

        // Create and show LockedBottomSheetDialog
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(layout)
        // Apply background color from prefs so bottom sheet follows app background (and night mode if prefs changes)
        val p = Prefs(context)
        try {
            layout.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {
            // best-effort; ignore failures
        }
        dialog.setLocked(true)
        dialog.show()
        backupRestoreDialog = dialog

    // Apply fonts and other styling to the bottom sheet content view
    setDialogFontForView(layout, context, "settings")
    }

    // Function to handle the Clear Data action, with a confirmation dialog
    private fun confirmClearData() {
        // Use a bottom-sheet confirm dialog to match the app's dialog style
    // allow button listeners to dismiss the dialog by capturing this var
    var bottomSheet: LockedBottomSheetDialog? = null

    val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            val titleView = TextView(context).apply {
                text = context.getString(R.string.advanced_settings_backup_restore_clear_title).uppercase()
                gravity = Gravity.CENTER
                textSize = 14f
            }
            addView(titleView)

            val messageView = TextView(context).apply {
                text = context.getString(R.string.advanced_settings_backup_restore_clear_description)
                gravity = Gravity.CENTER
                textSize = 14f
                val mPad = (8 * context.resources.displayMetrics.density).toInt()
                setPadding(mPad, mPad, mPad, mPad)
            }
            addView(messageView)

            val btnRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (12 * context.resources.displayMetrics.density).toInt()
                }
                layoutParams = params

                val btnPadding = (8 * context.resources.displayMetrics.density).toInt()

                val noBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.cancel)
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                }

                val yesBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.okay)
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                }

                // Style buttons
                try {
                    val p = Prefs(context)
                    val density = context.resources.displayMetrics.density
                    val radius = (6 * density)
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(p.backgroundColor)
                        setStroke(strokeWidth, p.appColor)
                    }
                    noBtn.background = bgDrawable
                    yesBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    noBtn.setTextColor(p.appColor)
                    yesBtn.setTextColor(p.appColor)
                } catch (_: Exception) {}

                val spacing = (8 * context.resources.displayMetrics.density).toInt()
                noBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = spacing }
                yesBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = spacing }

                addView(noBtn)
                addView(yesBtn)

                noBtn.setOnClickListener { bottomSheet?.dismiss() }
                yesBtn.setOnClickListener {
                    clearData()
                    bottomSheet?.dismiss()
                }
            }

            addView(btnRow)
        }

        // show bottom sheet
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            val p = Prefs(context)
            content.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        setDialogFontForView(content, context, "settings")
        dialog.setLocked(true)
        dialog.show()
        // ensure bottom padding for nav bar
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}
    }

    private fun clearData() {
        prefs = Prefs(context)
        prefs.clear()

        AppReloader.restartApp(context)
    }

    // legacy AlertDialog references removed; use bottom-sheet instances below
    var sliderBottomSheetDialog: LockedBottomSheetDialog? = null
    var inputBottomSheetDialog: LockedBottomSheetDialog? = null

    fun showSliderDialog(
        context: Context,
        title: String,
        minValue: Int,
        maxValue: Int,
        currentValue: Int,
        onValueSelected: (Int) -> Unit // Callback for when the user selects a value
    ) {
    // Dismiss any existing dialog to prevent multiple dialogs from being open simultaneously
    sliderBottomSheetDialog?.dismiss()

        var seekBar: SeekBar
        lateinit var valueText: TextView

        // Create a layout to hold the SeekBar, value display and buttons
        val seekBarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val pad = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            // TextView to display the current value
            valueText = TextView(context).apply {
                text = "$currentValue"
                textSize = 16f
                gravity = Gravity.CENTER
            }

            // Declare the seekBar outside the layout block so we can access it later
            seekBar = SeekBar(context).apply {
                min = minValue // Minimum value
                max = maxValue // Maximum value
                progress = currentValue // Default value
                isFocusable = true
                isFocusableInTouchMode = true
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        valueText.text = "$progress"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            }

            // Add TextView and SeekBar to the layout
            addView(valueText)
            addView(seekBar)

            // Buttons row
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // add spacing between SeekBar and buttons (12dp)
                    topMargin = (16 * context.resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                val btnPadding = (8 * context.resources.displayMetrics.density).toInt()
                // smaller button padding and text size for compact appearance
                val smallBtnPadding = (8 * context.resources.displayMetrics.density).toInt()
                val smallTextSizeSp = 16f

                val cancelBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.cancel)
                    // make button visually smaller
                    setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                    textSize = smallTextSizeSp
                    // remove enforced minimums so button can shrink
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener { sliderBottomSheetDialog?.dismiss() }
                }
                // We'll make both buttons share available width equally. Add a small gap between them.
                val btnSpacing = (8 * context.resources.displayMetrics.density).toInt()

                val okBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.okay)
                    // make button visually smaller
                    setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                    textSize = smallTextSizeSp
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener {
                        val finalValue = seekBar.progress
                        onValueSelected(finalValue)
                        sliderBottomSheetDialog?.dismiss()
                    }
                }

                // Style buttons: fill = backgroundColor, outline = appColor
                try {
                    val p = Prefs(context)
                    val density = context.resources.displayMetrics.density
                    val radius = (6 * density)
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(p.backgroundColor)
                        setStroke(strokeWidth, p.appColor)
                    }
                    cancelBtn.background = bgDrawable
                    okBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    // Ensure button text color uses appColor for visibility
                    cancelBtn.setTextColor(p.appColor)
                    okBtn.setTextColor(p.appColor)
                } catch (_: Exception) { }
                // Give both buttons equal weight so they have the same width, with a small margin between them
                val leftParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = btnSpacing
                }
                val rightParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = btnSpacing
                }
                cancelBtn.layoutParams = leftParams
                okBtn.layoutParams = rightParams

                addView(cancelBtn)
                addView(okBtn)
            }

            addView(buttonsRow)
        }

        // Apply fonts and color styling to the content view
        setDialogFontForView(seekBarLayout, context, "settings")

        // Create bottom sheet dialog and wire key handling
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(seekBarLayout)
        // apply background color
        try {
            val p = Prefs(context)
            seekBarLayout.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}

        // Key handling: D-Pad for fine tuning when SeekBar has focus, volume keys if enabled
        dialog.keyEventListener = { event ->
            if (event.action != android.view.KeyEvent.ACTION_DOWN) {
                false
            } else {
                when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (seekBar.hasFocus()) {
                            if (seekBar.progress > seekBar.min) seekBar.progress = seekBar.progress - 1
                            true
                        } else {
                            false
                        }
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (seekBar.hasFocus()) {
                            if (seekBar.progress < seekBar.max) seekBar.progress = seekBar.progress + 1
                            true
                        } else {
                            false
                        }
                    }
                    android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                        val prefs = Prefs(context)
                        if (prefs.useVolumeKeysForPages) {
                            if (seekBar.progress < seekBar.max) seekBar.progress = seekBar.progress + 1
                            true
                        } else {
                            false
                        }
                    }
                    android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        val prefs = Prefs(context)
                        if (prefs.useVolumeKeysForPages) {
                            if (seekBar.progress > seekBar.min) seekBar.progress = seekBar.progress - 1
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            }
        }

        dialog.setLocked(true)
        dialog.show()
        sliderBottomSheetDialog = dialog
    }

    // singleChoiceDialog (AlertDialog) removed
    var singleChoiceBottomSheetDialog: LockedBottomSheetDialog? = null

    fun <T> showSingleChoiceDialog(
        context: Context,
        options: Array<T>,
        titleResId: Int,
        fonts: List<Typeface>? = null, // Optional fonts
        fontSize: Float = 18f, // Default font size
        selectedIndex: Int? = null, // Index of selected font
        isCustomFont: ((T) -> Boolean)? = null, // Function to check if font is custom
        nonSelectable: ((T) -> Boolean)? = null, // items for which we should NOT show a radio (e.g. Add Custom)
        onItemSelected: (T) -> Unit,
        onItemDeleted: ((T) -> Unit)? = null, // kept for compatibility but not used here
        showButtons: Boolean = true
    ) {
    singleChoiceBottomSheetDialog?.dismiss()


        // Prepare mutable option and display lists so we can update UI when items change
        val optionList: MutableList<T> = options.toMutableList()
        val displayList: MutableList<String> = optionList.map { option ->
            val raw = when (option) {
                is Enum<*> -> option.name
                else -> option.toString()
            }
            var name = if (raw.length > 18) raw.substring(0, 18) + "..." else raw
            // Prefix custom fonts with 'x ' when provided a checker (purely informative)
            try {
                if (isCustomFont != null && isCustomFont(option)) name = "x $name"
            } catch (_: Exception) {}
            name
        }.toMutableList()

        var checkedItem = selectedIndex ?: -1

        val prefsForAdapter = Prefs(context)
        val defaultTypeface = prefsForAdapter.getFontForContext("settings").getFont(context, prefsForAdapter.getCustomFontPathForContext("settings"))
            ?: getTrueSystemFont()
        // Custom adapter: render position 0 (e.g. "Add Custom Font...") without a radio button
        val stringAdapter: android.widget.ArrayAdapter<String> = object : android.widget.ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_1,
            displayList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = android.view.LayoutInflater.from(context)
                val optionObj = optionList.getOrNull(position)
                val layoutRes = if (nonSelectable != null && optionObj != null && nonSelectable(optionObj)) android.R.layout.simple_list_item_1 else android.R.layout.simple_list_item_single_choice
                val view = inflater.inflate(layoutRes, parent, false)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                // Ensure the label text is set (we inflate different layouts so super.getView isn't called)
                val label = try { displayList.getOrNull(position) ?: optionObj?.toString() ?: "" } catch (_: Exception) { optionObj?.toString() ?: "" }
                textView.text = label
                // Prefer per-item font if provided, otherwise use app font
                if (fonts != null) {
                    fonts.getOrNull(position)?.let { font ->
                        textView.typeface = font
                        textView.textSize = fontSize
                    } ?: run {
                        textView.typeface = defaultTypeface
                        textView.textSize = prefsForAdapter.settingsSize.toFloat()
                    }
                } else {
                    textView.typeface = defaultTypeface
                    textView.textSize = prefsForAdapter.settingsSize.toFloat()
                }
                val paddingPx = (24 * context.resources.displayMetrics.density).toInt()
                textView.setPadding(paddingPx, 0, paddingPx, 0)
                try { textView.setTextColor(prefsForAdapter.appColor) } catch (_: Exception) {}

                return view
            }
        }

    // Build bottom-sheet content
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (6 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            // Title
            val titleView = TextView(context).apply {
                text = context.getString(titleResId).uppercase()
                gravity = Gravity.CENTER
                textSize = 14f
            }
            addView(titleView)

            // ListView
            val listView = ListView(context).apply {
                choiceMode = ListView.CHOICE_MODE_SINGLE
                divider = null
                adapter = stringAdapter
            }
            // Handle long-press at the ListView level so item clicks remain responsive.
            listView.setOnItemLongClickListener { _, view, position, _ ->
                try {
                    val opt = optionList.getOrNull(position)
                    if (opt != null && isCustomFont != null && isCustomFont(opt)) {
                        android.app.AlertDialog.Builder(context)
                            .setTitle(context.getString(android.R.string.dialog_alert_title))
                            .setMessage(context.getString(R.string.confirm_delete_font))
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                try { onItemDeleted?.invoke(opt) } catch (_: Exception) {}
                                val removedPos = position.coerceIn(0, optionList.size - 1)
                                optionList.removeAt(removedPos)
                                displayList.removeAt(removedPos)
                                try { stringAdapter.notifyDataSetChanged() } catch (_: Exception) {}
                                listView.clearChoices()
                            }
                            .show()
                        return@setOnItemLongClickListener true
                    }
                } catch (_: Exception) {}
                false
            }
            // Pre-check selection
            if (checkedItem >= 0) {
                listView.post { listView.setItemChecked(checkedItem, true) }
            }

            val maxListHeight = (context.resources.displayMetrics.heightPixels * 0.30).toInt()
            val listParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxListHeight)
            addView(listView, listParams)

            // Buttons row (when showButtons=true we offer Select/Cancel, but no Delete)
            val buttonsRow = if (showButtons) {
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        topMargin = (12 * context.resources.displayMetrics.density).toInt()
                    }
                    layoutParams = params

                    val cancelBtn = android.widget.Button(context).apply {
                        text = context.getString(android.R.string.cancel)
                    }
                    val selectBtn = android.widget.Button(context).apply {
                        text = context.getString(android.R.string.ok)
                    }

                    // Style buttons
                    try {
                        selectBtn.setTextColor(prefsForAdapter.appColor)
                        cancelBtn.setTextColor(prefsForAdapter.appColor)
                    } catch (_: Exception) {}

                    val spacing = (8 * context.resources.displayMetrics.density).toInt()
                    cancelBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = spacing }
                    selectBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = spacing }

                    addView(cancelBtn)
                    addView(selectBtn)

                    cancelBtn.setOnClickListener { singleChoiceBottomSheetDialog?.dismiss() }
                    selectBtn.setOnClickListener {
                        val checkedPos = listView.checkedItemPosition
                        if (checkedPos >= 0 && checkedPos < optionList.size) {
                            try { onItemSelected(optionList[checkedPos]) } catch (_: Exception) {}
                        }
                        singleChoiceBottomSheetDialog?.dismiss()
                    }
                }
            } else null

            buttonsRow?.let { addView(it) }

            // Immediate select on item click when buttons are hidden (fonts flow)
            if (!showButtons) {
                listView.setOnItemClickListener { _, _, position, _ ->
                    try {
                        onItemSelected(optionList[position])
                    } catch (_: Exception) {}
                    singleChoiceBottomSheetDialog?.dismiss()
                }
            } else {
                // When buttons are shown, clicking list just checks the item
                listView.setOnItemClickListener { _, _, position, _ ->
                    listView.setItemChecked(position, true)
                }
            }
        }

        // Apply fonts/colors
        setDialogFontForView(content, context, "settings")

        // Create and show bottom sheet
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            val p = Prefs(context)
            content.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        dialog.setLocked(true)
        dialog.show()
        // Use WindowInsets to dynamically pad the bottom of the content so buttons are never clipped
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}

        singleChoiceBottomSheetDialog = dialog
    }

    private fun applyFontsToListView(listView: ListView, fonts: List<Typeface>?, fontSize: Float) {
        // This method is no longer needed, keeping for compatibility
    }

    // multiChoiceDialog (AlertDialog) removed
    var multiChoiceBottomSheetDialog: LockedBottomSheetDialog? = null
    fun showMultiChoiceDialog(
        context: Context,
        title: String,
        items: Array<String>,
        initialChecked: BooleanArray,
        onConfirm: (selectedIndices: List<Int>) -> Unit
    ) {
    // Dismiss any existing dialogs
    multiChoiceBottomSheetDialog?.dismiss()

        val checked = initialChecked.copyOf()

        // Build content layout for bottom sheet
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (8 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            // Title
            val titleView = TextView(context).apply {
                text = title.uppercase()
                gravity = Gravity.CENTER
                textSize = 16f
            }
            addView(titleView)

            // ListView with multiple choice
            val adapter = object : android.widget.ArrayAdapter<String>(
                context,
                android.R.layout.simple_list_item_multiple_choice,
                items
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    try {
                        val tv = v.findViewById<TextView>(android.R.id.text1)
                        tv.setTextColor(Prefs(context).appColor)
                        tv.typeface = Prefs(context).getFontForContext("settings").getFont(context, Prefs(context).getCustomFontPathForContext("settings")) ?: getTrueSystemFont()
                    } catch (_: Exception) {}
                    return v
                }
            }

            val listView = ListView(context).apply {
                choiceMode = ListView.CHOICE_MODE_MULTIPLE
                divider = null
                this.adapter = adapter
            }

            // Pre-check items
            listView.post {
                for (i in checked.indices) {
                    if (checked[i]) listView.setItemChecked(i, true)
                }
            }

            // Limit height to keep dialog compact
            val maxListHeight = (context.resources.displayMetrics.heightPixels * 0.35).toInt()
            val listParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxListHeight)
            addView(listView, listParams)

            // Handle immediate toggles: update checked array and call onConfirm
            listView.setOnItemClickListener { _, _, position, _ ->
                try {
                    val currently = listView.isItemChecked(position)
                    checked[position] = currently
                    val selected = mutableListOf<Int>()
                    for (idx in checked.indices) if (checked[idx]) selected.add(idx)
                    onConfirm(selected)
                } catch (_: Exception) {}
            }
        }

        // Apply fonts/colors and show as bottom sheet
        setDialogFontForView(content, context, "settings")
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            val p = Prefs(context)
            content.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        dialog.setLocked(true)
        dialog.show()

        // WindowInsets padding
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}

        multiChoiceBottomSheetDialog = dialog
    }

    // colorPickerDialog (AlertDialog) removed
    var colorPickerBottomSheetDialog: LockedBottomSheetDialog? = null
    var errorBottomSheetDialog: LockedBottomSheetDialog? = null

    fun showColorPickerDialog(
        context: Context,
        titleResId: Int,
        color: Int,
        onItemSelected: (Int) -> Unit // Callback to handle the selected color
    ) {
    // Dismiss any existing color picker
    colorPickerBottomSheetDialog?.dismiss()

        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        var isUpdatingText = false

        // Create SeekBars for Red, Green, and Blue
        val redSeekBar = createColorSeekBar(context, red)
        val greenSeekBar = createColorSeekBar(context, green)
        val blueSeekBar = createColorSeekBar(context, blue)

        // Create color preview box and RGB Hex input field
        val colorPreviewBox = createColorPreviewBox(context, color)
        val rgbText = createRgbTextField(context, red, green, blue)

        // Layout with SeekBars, Color Preview, and RGB Hex Text Input
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)

            // Title
            val titleView = TextView(context).apply {
                text = context.getString(titleResId).uppercase()
                gravity = Gravity.CENTER
                textSize = 16f
            }
            addView(titleView)

            // Create a horizontal layout for the text box and color preview
            val horizontalLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL // Vertically center the views

                // RGB Text field
                val rgbParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 32
                }
                rgbText.layoutParams = rgbParams
                addView(rgbText)

                // Color preview box
                val colorParams = LinearLayout.LayoutParams(150, 50).apply {
                    marginEnd = 32
                }
                colorPreviewBox.layoutParams = colorParams
                addView(colorPreviewBox)
            }

            addView(redSeekBar)
            addView(greenSeekBar)
            addView(blueSeekBar)
            addView(horizontalLayout)
        }

        // Update color preview and immediately apply when SeekBars are adjusted
        val updateColorPreview = {
            val updatedColor = Color.rgb(
                redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress
            )
            colorPreviewBox.setBackgroundColor(updatedColor)
            // update text field without re-entrancy
            if (!isUpdatingText) {
                isUpdatingText = true
                try {
                    rgbText.setText(String.format("#%02X%02X%02X", redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress))
                } catch (_: Exception) {}
                isUpdatingText = false
            }
            // Apply immediately
            try { onItemSelected(updatedColor) } catch (_: Exception) {}
        }

        // Listeners to update color preview and apply changes
        redSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))
        greenSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))
        blueSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))

        // Listen for text input and update sliders and preview (apply on valid hex)
        rgbText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.trim()?.let { colorString ->
                    if (colorString.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                        val hexColor = colorString.toColorInt()
                        redSeekBar.progress = Color.red(hexColor)
                        greenSeekBar.progress = Color.green(hexColor)
                        blueSeekBar.progress = Color.blue(hexColor)
                        updateColorPreview()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Apply fonts/colors to the content
        setDialogFontForView(layout, context, "settings")

        // Create and show bottom sheet
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(layout)
        try {
            val p = Prefs(context)
            layout.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}

        dialog.setLocked(true)
        dialog.show()
        colorPickerBottomSheetDialog = dialog

        // Use WindowInsets to dynamically pad the bottom of the content so it's not clipped
        try {
            val baseBottom = layout.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        layout.setPadding(layout.paddingLeft, layout.paddingTop, layout.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}
    }

    fun showInputDialog(
        context: Context,
        title: String,
        initialValue: String,
        onValueEntered: (String) -> Unit
    ) {
        // Dismiss any existing input bottom sheet to avoid duplicates
        inputBottomSheetDialog?.dismiss()

        val input = EditText(context).apply {
            setText(initialValue)
            isSingleLine = false
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
        }

        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)
            ) {
                onValueEntered(input.text.toString())
                inputBottomSheetDialog?.dismiss()
                true
            } else {
                false
            }
        }

        // Build content layout for bottom sheet
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)

            // Title
            val titleView = TextView(context).apply {
                text = title.uppercase()
                gravity = Gravity.CENTER
                textSize = 16f
            }
            addView(titleView)

            // Input field container (match previous padding)
            val frame = android.widget.FrameLayout(context).apply {
                val innerPad = (8 * context.resources.displayMetrics.density).toInt()
                setPadding(0, innerPad, 0, innerPad)
                (input.parent as? ViewGroup)?.removeView(input)
                addView(input)
            }
            addView(frame)

            // Buttons row
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (12 * context.resources.displayMetrics.density).toInt()
                }
                layoutParams = params

                val btnPadding = (8 * context.resources.displayMetrics.density).toInt()
                val smallTextSizeSp = 16f

                val cancelBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.cancel)
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    textSize = smallTextSizeSp
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener { inputBottomSheetDialog?.dismiss() }
                }

                val okBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.okay)
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    textSize = smallTextSizeSp
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener {
                        onValueEntered(input.text.toString())
                        inputBottomSheetDialog?.dismiss()
                    }
                }

                // Style buttons: fill = backgroundColor, outline = appColor (thicker)
                try {
                    val p = Prefs(context)
                    val density = context.resources.displayMetrics.density
                    val radius = (6 * density)
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(p.backgroundColor)
                        setStroke(strokeWidth, p.appColor)
                    }
                    cancelBtn.background = bgDrawable
                    okBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    cancelBtn.setTextColor(p.appColor)
                    okBtn.setTextColor(p.appColor)
                } catch (_: Exception) {}

                // Equal width params with spacing
                val btnSpacing = (8 * context.resources.displayMetrics.density).toInt()
                val leftParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = btnSpacing
                }
                val rightParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = btnSpacing
                }
                cancelBtn.layoutParams = leftParams
                okBtn.layoutParams = rightParams

                addView(cancelBtn)
                addView(okBtn)
            }

            addView(buttonsRow)
        }

        // Apply fonts/colors to content
        try { setDialogFontForView(content, context, "settings") } catch (_: Exception) {}

        // Create and show LockedBottomSheetDialog
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            val p = Prefs(context)
            content.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}

        dialog.setLocked(true)
        dialog.show()
        inputBottomSheetDialog = dialog
    }

    fun showErrorDialog(context: Context, title: String, message: String) {
        // Dismiss any existing error dialogs
        errorBottomSheetDialog?.dismiss()

    // Grab prefs early so we can apply appColor to title/message
    val p = Prefs(context)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            val titleView = TextView(context).apply {
                text = title.uppercase()
                // Center title for crash dialog
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
                try { setTextColor(p.appColor) } catch (_: Exception) {}
            }
            addView(titleView)

            val messageView = TextView(context).apply {
                text = message
                // Left-align message text
                gravity = Gravity.START
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                textSize = 14f
                val mPad = (8 * context.resources.displayMetrics.density).toInt()
                setPadding(mPad, mPad, mPad, mPad)
                try { setTextColor(p.appColor) } catch (_: Exception) {}
            }
            addView(messageView)

            // OK button
            val btnPadding = (10 * context.resources.displayMetrics.density).toInt()
            val okBtn = android.widget.Button(context).apply {
                text = context.getString(android.R.string.ok)
                setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setOnClickListener { errorBottomSheetDialog?.dismiss() }
            }
            // style button
            try {
                val p = Prefs(context)
                val density = context.resources.displayMetrics.density
                val radius = (6 * density)
                val strokeWidth = (3f * density).toInt()
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(p.backgroundColor)
                    setStroke(strokeWidth, p.appColor)
                }
                okBtn.background = bgDrawable
                okBtn.setTextColor(p.appColor)
            } catch (_: Exception) {}

            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * context.resources.displayMetrics.density).toInt()
            }
            okBtn.layoutParams = params
            addView(okBtn)
        }

        // Apply fonts/colors
        setDialogFontForView(content, context, "settings")

        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            val p = Prefs(context)
            content.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}

        dialog.setLocked(true)
        dialog.show()
        errorBottomSheetDialog = dialog

        // WindowInsets padding
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}
    }

    private fun createColorSeekBar(context: Context, initialValue: Int): SeekBar {
        return SeekBar(context).apply {
            max = 255
            progress = initialValue
        }
    }

    private fun createColorPreviewBox(context: Context, color: Int): View {
        return View(context).apply {
            setBackgroundColor(color)
        }
    }

    private fun createRgbTextField(context: Context, red: Int, green: Int, blue: Int): EditText {
        return EditText(context).apply {
            setText(String.format("#%02X%02X%02X", red, green, blue))
            inputType = InputType.TYPE_CLASS_TEXT

            // Remove the bottom line (underline) from the EditText
            background = null
        }
    }

    private fun createSeekBarChangeListener(updateColorPreview: () -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateColorPreview()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    fun setDialogFontForAllButtonsAndText(
        dialog: AlertDialog?,
        context: Context,
        contextKey: String = "settings"
    ) {
        val prefs = Prefs(context)
        val fontFamily = prefs.getFontForContext(contextKey)
        val customFontPath = prefs.getCustomFontPathForContext(contextKey)
        val typeface = fontFamily.getFont(context, customFontPath) ?: getTrueSystemFont()
        val textSize = prefs.settingsSize.toFloat()
        val appColor = prefs.appColor
        // Set for all buttons
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.let {
            it.typeface = typeface
            it.textSize = textSize
            it.setTextColor(appColor)
        }
        dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.let {
            it.typeface = typeface
            it.textSize = textSize
            it.setTextColor(appColor)
        }
        dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.let {
            it.typeface = typeface
            it.textSize = textSize
            it.setTextColor(appColor)
        }
        // Set for all TextViews in the dialog view
        (dialog?.window?.decorView as? ViewGroup)?.let { root ->
            fun applyToAllTextViews(view: View) {
                if (view is TextView) {
                    view.typeface = typeface
                    view.textSize = textSize
                    view.setTextColor(appColor)
                    // If it's an EditText, also update hint color for visibility
                    if (view is EditText) {
                        view.setHintTextColor(appColor)
                    }
                } else if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        applyToAllTextViews(view.getChildAt(i))
                    }
                }
            }
            applyToAllTextViews(root)
        }
    }

    /**
     * Apply the app font and text size to any view hierarchy rooted at [view].
     * This is useful for BottomSheetDialog content views which are not AlertDialogs.
     */
    private fun setDialogFontForView(view: View?, context: Context, contextKey: String = "settings") {
        if (view == null) return

        val prefs = Prefs(context)
        val fontFamily = prefs.getFontForContext(contextKey)
        val customFontPath = prefs.getCustomFontPathForContext(contextKey)
        val typeface = fontFamily.getFont(context, customFontPath) ?: getTrueSystemFont()
        val textSize = prefs.settingsSize.toFloat()
        val appColor = prefs.appColor

        fun apply(view: View) {
            if (view is TextView) {
                view.typeface = typeface
                view.textSize = textSize
                view.setTextColor(appColor)
                if (view is EditText) {
                    view.setHintTextColor(appColor)
                }
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    apply(view.getChildAt(i))
                }
            }
        }

        apply(view)
    }
}

