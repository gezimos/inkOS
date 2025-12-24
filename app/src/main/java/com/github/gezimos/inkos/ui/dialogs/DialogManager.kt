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
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.getTrueSystemFont
import com.github.gezimos.inkos.helper.loadFile
import com.github.gezimos.inkos.helper.storeFile
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.style.resolveThemeColors

class DialogManager(val context: Context, val activity: Activity) {

    private lateinit var prefs: Prefs

    var backupRestoreDialog: LockedBottomSheetDialog? = null
    var sliderDialog: AlertDialog? = null
    var singleChoiceDialog: AlertDialog? = null
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
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
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
        // Apply background color from theme so bottom sheet follows app background (and night mode if prefs changes)
        try {
            val (_, bg) = resolveThemeColors(context)
            layout.setBackgroundColor(bg)
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

                // Style buttons using resolved theme colors
                try {
                    val (textColor, backgroundColor) = resolveThemeColors(context)
                    val density = context.resources.displayMetrics.density
                    val prefs = Prefs(context)
                    val textIslandsShape = prefs.textIslandsShape
                    val radius = ShapeHelper.getCornerRadiusPx(
                        textIslandsShape = textIslandsShape,
                        density = density
                    )
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(backgroundColor)
                        setStroke(strokeWidth, textColor)
                    }
                    noBtn.background = bgDrawable
                    yesBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    noBtn.setTextColor(textColor)
                    yesBtn.setTextColor(textColor)
                } catch (_: Exception) {}

                val spacing = (8 * context.resources.displayMetrics.density).toInt()
                noBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = spacing }
                yesBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = spacing }

                addView(noBtn)
                addView(yesBtn)

                noBtn.setOnClickListener {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    bottomSheet?.dismiss()
                }
                yesBtn.setOnClickListener {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    clearData()
                    bottomSheet?.dismiss()
                }
            }

            addView(btnRow)
        }

        // show bottom sheet
        val dialog = LockedBottomSheetDialog(context)
        bottomSheet = dialog // Assign to bottomSheet so cancel button can dismiss it
        dialog.setContentView(content)
        try {
            val (_, bg) = resolveThemeColors(context)
            content.setBackgroundColor(bg)
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
        @Suppress("UNUSED_PARAMETER") title: String,
        minValue: Int,
        maxValue: Int,
        currentValue: Int,
        onValueSelected: (Int) -> Unit // Callback for when the user selects a value
    ) {
    // Dismiss any existing dialog to prevent multiple dialogs from being open simultaneously
    sliderBottomSheetDialog?.dismiss()

        var seekBar: SeekBar
        lateinit var valueEdit: EditText

        // Create a layout to hold the SeekBar, value display and buttons
        val seekBarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val pad = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            // EditText to display and edit the current value (tap to show IME for number entry)
            valueEdit = EditText(context).apply {
                id = View.generateViewId()
                setText("$currentValue")
                textSize = 16f
                gravity = Gravity.CENTER
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_NUMBER
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                // Make it visually similar to a TextView (no underline/background)
                background = null
                // Allow text selection / caret for editing
                isCursorVisible = true
                // Add a bit of padding on the number to make it easier to tap directly
                val valuePad = (6 * context.resources.displayMetrics.density).toInt()
                setPadding(valuePad, valuePad, valuePad, valuePad)
                // Make focusable for D-pad navigation
                isFocusable = true
                isFocusableInTouchMode = true
            }

            // Declare the seekBar outside the layout block so we can access it later
            val density = context.resources.displayMetrics.density

            seekBar = SeekBar(context).apply {
                id = View.generateViewId()
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
                        // update numeric field when SeekBar changes
                        try { valueEdit.setText("$progress") } catch (_: Exception) {}
                        try { valueEdit.setSelection(valueEdit.text.length) } catch (_: Exception) {}
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar) {}
                })
            }
            
            // Set up focus chain: valueEdit -> seekBar -> cancelBtn -> okBtn
            // We'll set button IDs after they're created, but set up the initial chain here
            seekBar.setNextFocusUpId(valueEdit.id)  // From seekbar, UP goes to number box
            valueEdit.setNextFocusDownId(seekBar.id)  // From number box, DOWN goes to seekbar

            // Apply theme tints to seekbar and value text
            try {
                val (textColor, _) = resolveThemeColors(context)
                valueEdit.setTextColor(textColor)
                // Use tint lists if available so thumb and progress follow theme
                try {
                    val csl = android.content.res.ColorStateList.valueOf(textColor)
                    seekBar.progressTintList = csl
                    seekBar.thumbTintList = csl
                    try {
                        val thumb = createCircularThumb(context, textColor)
                        seekBar.thumb = thumb
                        seekBar.thumbOffset = (thumb.intrinsicWidth / 2)
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
            } catch (_: Exception) {}

            // Add TextView and SeekBar to the layout. Add extra top margin
            // on the SeekBar so there's more space between the numeric value
            // and the slider itself (improves touch clarity).
            addView(valueEdit)
            val seekLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (12 * density).toInt()
            }
            addView(seekBar, seekLp)

            // After the SeekBar exists, wire the EditText input to update the SeekBar
            try {
                valueEdit.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        try {
                            val v = s?.toString()?.toIntOrNull()
                            if (v != null) {
                                val clamped = v.coerceIn(minValue, maxValue)
                                if (seekBar.progress != clamped) seekBar.progress = clamped
                            }
                        } catch (_: Exception) {}
                    }
                })

                valueEdit.setOnEditorActionListener { _, actionId, event ->
                    // Handle both IME_ACTION_DONE and Enter key press
                    val isEnterKey = event != null && 
                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && 
                        event.action == android.view.KeyEvent.ACTION_DOWN
                    
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || isEnterKey) {
                        try {
                            val entered = valueEdit.text.toString().toIntOrNull()
                            val final = entered?.coerceIn(minValue, maxValue) ?: seekBar.progress
                            seekBar.progress = final
                            // Update the EditText to show the clamped value
                            valueEdit.setText("$final")
                            valueEdit.setSelection(valueEdit.text.length)
                        } catch (_: Exception) {}
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                            imm.hideSoftInputFromWindow(valueEdit.windowToken, 0)
                        } catch (_: Exception) {}
                        // Clear focus and move to next focusable element (seekbar)
                        valueEdit.clearFocus()
                        seekBar.requestFocus()
                        true
                    } else false
                }

                valueEdit.setOnClickListener {
                    try {
                        valueEdit.requestFocus()
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.showSoftInput(valueEdit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            // Buttons row
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // add spacing between SeekBar and buttons (12dp)
                    topMargin = (24 * context.resources.displayMetrics.density).toInt()
                }
                layoutParams = params
                (8 * context.resources.displayMetrics.density).toInt()
                // slightly larger button padding and text size for compact appearance (bigger tap target)
                val smallBtnPadding = (12 * context.resources.displayMetrics.density).toInt()
                val smallTextSizeSp = 16f

                val cancelBtn = android.widget.Button(context).apply {
                    id = View.generateViewId()
                    text = context.getString(R.string.cancel)
                    // make button visually smaller
                    setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                    textSize = smallTextSizeSp
                    // remove enforced minimums so button can shrink
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    // Make focusable for keyboard navigation, but NOT in touch mode to avoid double-tap issue
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        sliderBottomSheetDialog?.dismiss()
                    }
                }
                // We'll make both buttons share available width equally. Add a small gap between them.
                val btnSpacing = (8 * context.resources.displayMetrics.density).toInt()

                val okBtn = android.widget.Button(context).apply {
                    id = View.generateViewId()
                    text = context.getString(R.string.okay)
                    // make button visually smaller
                    setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                    textSize = smallTextSizeSp
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    // Make focusable for keyboard navigation, but NOT in touch mode to avoid double-tap issue
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        val finalValue = seekBar.progress
                        onValueSelected(finalValue)
                        sliderBottomSheetDialog?.dismiss()
                    }
                }
                
                // Set up focus chain: seekBar -> cancelBtn -> okBtn -> valueEdit (circular)
                seekBar.setNextFocusDownId(cancelBtn.id)
                cancelBtn.setNextFocusUpId(seekBar.id)
                cancelBtn.setNextFocusRightId(okBtn.id)
                okBtn.setNextFocusLeftId(cancelBtn.id)
                okBtn.setNextFocusDownId(valueEdit.id)
                valueEdit.setNextFocusUpId(okBtn.id)

                // Style buttons: use resolved theme colors (background/text)
                try {
                    val (textColor, backgroundColor) = resolveThemeColors(context)
                    val density = context.resources.displayMetrics.density
                    val prefs = Prefs(context)
                    val textIslandsShape = prefs.textIslandsShape
                    val radius = ShapeHelper.getCornerRadiusPx(
                        textIslandsShape = textIslandsShape,
                        density = density
                    )
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(backgroundColor)
                        setStroke(strokeWidth, textColor)
                    }
                    cancelBtn.background = bgDrawable
                    okBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    // Ensure button text color uses textColor for visibility
                    cancelBtn.setTextColor(textColor)
                    okBtn.setTextColor(textColor)
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
        // apply background color using resolved theme
        try {
            val (_, bg) = resolveThemeColors(context)
            seekBarLayout.setBackgroundColor(bg)
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
        
        // Request focus on seekBar when dialog is shown (primary focus)
        seekBar.post {
            seekBar.requestFocus()
        }
        
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
        showButtons: Boolean = true,
        maxHeightRatio: Float = 0.30f // Maximum height as ratio of screen height (default 30%)
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

        val checkedItem = selectedIndex ?: -1

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
                // Use convertView if available, otherwise inflate new view
                // Note: We can't always reuse convertView because different items may use different layouts
                val view = convertView ?: run {
                    @Suppress("UnconditionalLayoutInflation")
                    inflater.inflate(layoutRes, parent, false)
                }
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
                try {
                    val (textColor, _) = resolveThemeColors(context)
                    textView.setTextColor(textColor)
                    try {
                        (textView as? android.widget.CheckedTextView)?.checkMarkTintList = android.content.res.ColorStateList.valueOf(textColor)
                    } catch (_: Exception) {}
                } catch (_: Exception) {}

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
            listView.setOnItemLongClickListener { _, _, position, _ ->
                try {
                    val opt = optionList.getOrNull(position)
                    if (opt != null && isCustomFont != null && isCustomFont(opt)) {
                        AlertDialog.Builder(context)
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

            val maxListHeight = (context.resources.displayMetrics.heightPixels * maxHeightRatio).toInt()
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
                        val (textColor, _) = resolveThemeColors(context)
                        selectBtn.setTextColor(textColor)
                        cancelBtn.setTextColor(textColor)
                    } catch (_: Exception) {}

                    val spacing = (8 * context.resources.displayMetrics.density).toInt()
                    cancelBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = spacing }
                    selectBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = spacing }

                    addView(cancelBtn)
                    addView(selectBtn)

                    cancelBtn.setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        singleChoiceBottomSheetDialog?.dismiss()
                    }
                    selectBtn.setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
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
            val (_, bg) = resolveThemeColors(context)
            content.setBackgroundColor(bg)
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


    // multiChoiceDialog (AlertDialog) removed
    var multiChoiceBottomSheetDialog: LockedBottomSheetDialog? = null
    fun showMultiChoiceDialog(
        context: Context,
        title: String,
        items: Array<String>,
        initialChecked: BooleanArray,
        onConfirm: (selectedIndices: List<Int>) -> Unit,
        maxHeightRatio: Float = 0.60f // Maximum height as ratio of screen height (default 60% for allowlists)
    ) {
    // Dismiss any existing dialogs
    multiChoiceBottomSheetDialog?.dismiss()

        val checked = initialChecked.copyOf()

        // Build content layout for bottom sheet
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * context.resources.displayMetrics.density).toInt()
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
                        try {
                            val (textColor, _) = resolveThemeColors(context)
                            tv.setTextColor(textColor)
                            // Tint the checkmark if this is a CheckedTextView so checkbox matches theme
                            try {
                                (tv as? android.widget.CheckedTextView)?.checkMarkTintList = android.content.res.ColorStateList.valueOf(textColor)
                            } catch (_: Exception) {}
                            tv.typeface = Prefs(context).getFontForContext("settings").getFont(context, Prefs(context).getCustomFontPathForContext("settings")) ?: getTrueSystemFont()
                        } catch (_: Exception) {}
                    } catch (_: Exception) {}
                    return v
                }
            }

            val listView = ListView(context).apply {
                choiceMode = ListView.CHOICE_MODE_MULTIPLE
                divider = null
                this.adapter = adapter
                // Add bottom padding to ListView so last item isn't hidden
                val bottomPad = (32 * context.resources.displayMetrics.density).toInt()
                setPadding(paddingLeft, paddingTop, paddingRight, bottomPad)
                clipToPadding = false
            }

            // Pre-check items
            listView.post {
                for (i in checked.indices) {
                    if (checked[i]) listView.setItemChecked(i, true)
                }
            }

            // Handle item clicks with auto-save: toggle checkbox and immediately save
            listView.setOnItemClickListener { _, _, position, _ ->
                try {
                    val currently = listView.isItemChecked(position)
                    checked[position] = currently
                    
                    // Auto-save: immediately call onConfirm with current selection
                    val selected = mutableListOf<Int>()
                    for (idx in checked.indices) if (checked[idx]) selected.add(idx)
                    try { onConfirm(selected) } catch (_: Exception) {}
                } catch (_: Exception) {}
            }

            // Calculate ListView height (no buttons needed now)
            val density = context.resources.displayMetrics.density
            val estimatedTitleHeight = (50 * density).toInt()
            val estimatedPadding = pad * 2
            val estimatedUsedSpace = estimatedTitleHeight + estimatedPadding
            
            val screenHeight = context.resources.displayMetrics.heightPixels
            val maxDialogHeight = (screenHeight * maxHeightRatio).toInt()
            val listHeight = (maxDialogHeight - estimatedUsedSpace).coerceAtLeast(300)
            
            // Add ListView with calculated height
            val listParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, listHeight)
            addView(listView, listParams)
        }

        // Apply fonts/colors and show as bottom sheet
        setDialogFontForView(content, context, "settings")
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            val (_, bg) = resolveThemeColors(context)
            content.setBackgroundColor(bg)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        dialog.setLocked(true)
        dialog.show()
        
        // Use WindowInsets to dynamically pad the bottom of the content so items are never clipped
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (16 * context.resources.displayMetrics.density).toInt()
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
    var wallpaperTargetBottomSheetDialog: LockedBottomSheetDialog? = null

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

        // Holds the color currently reflected in the preview but not yet confirmed by the user.
        var pendingColor = color

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

            // Add seekbars with vertical spacing between them
            val seekBarLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val spacing = (10 * context.resources.displayMetrics.density).toInt()
                topMargin = spacing
            }

            // First seekbar: keep a little less top margin so title doesn't feel too far
            val firstLp = LinearLayout.LayoutParams(seekBarLp).apply {
                topMargin = (6 * context.resources.displayMetrics.density).toInt()
            }

            addView(redSeekBar, firstLp)
            addView(greenSeekBar, seekBarLp)
            addView(blueSeekBar, seekBarLp)
            addView(horizontalLayout)

            // Buttons row (Cancel / OK) — mirror behavior/style from showSliderDialog
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (16 * context.resources.displayMetrics.density).toInt()
                }
                layoutParams = params

                val smallBtnPadding = (8 * context.resources.displayMetrics.density).toInt()
                val smallTextSizeSp = 16f

                val cancelBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.cancel)
                    setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                    textSize = smallTextSizeSp
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        // Revert to original color
                        try { onItemSelected(color) } catch (_: Exception) {}
                        colorPickerBottomSheetDialog?.dismiss()
                    }
                }

                val btnSpacing = (8 * context.resources.displayMetrics.density).toInt()

                val okBtn = android.widget.Button(context).apply {
                    text = context.getString(R.string.okay)
                    setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                    textSize = smallTextSizeSp
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        // Apply the pending color (user-confirmed)
                        try { onItemSelected(pendingColor) } catch (_: Exception) {}
                        colorPickerBottomSheetDialog?.dismiss()
                    }
                }

                // Style buttons using theme colors
                try {
                    val (textColor, backgroundColor) = resolveThemeColors(context)
                    val density = context.resources.displayMetrics.density
                    val prefs = Prefs(context)
                    val textIslandsShape = prefs.textIslandsShape
                    val radius = ShapeHelper.getCornerRadiusPx(
                        textIslandsShape = textIslandsShape,
                        density = density
                    )
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(backgroundColor)
                        setStroke(strokeWidth, textColor)
                    }
                    cancelBtn.background = bgDrawable
                    okBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    cancelBtn.setTextColor(textColor)
                    okBtn.setTextColor(textColor)
                } catch (_: Exception) { }

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

    // Flag to prevent infinite loop when programmatically updating text.
    // Without this flag, we get an infinite loop:
    // 1. User types hex code -> TextWatcher.afterTextChanged() fires
    // 2. afterTextChanged() calls updateColorPreview() which sets rgbText.setText()
    // 3. setText() triggers afterTextChanged() again -> back to step 2 (infinite loop)
    // By setting this flag before programmatic setText() calls, the TextWatcher can skip processing
    // and break the loop.
    var isUpdatingTextProgrammatically = false

    // Update color preview and immediately apply when SeekBars are adjusted
    val updateColorPreview = lambda@{
            val updatedColor = Color.rgb(
                redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress
            )

            // If this dialog is for background color, validate against text color prefs
            try {
                val p = Prefs(context)
                val isBackgroundDialog = try { context.getString(titleResId) == context.getString(R.string.background_color) } catch (_: Exception) { false }
                if (isBackgroundDialog) {
                    val (textColor, _) = resolveThemeColors(context)
                    val forbidden = listOf(textColor)
                    if (forbidden.contains(updatedColor)) {
                        // Disallow: show toast and reset to original default background (resource 'bg')
                        try {
                            // Determine the original default background color from app theme resources
                            val defaultBgRes = when (p.appTheme) {
                                Constants.Theme.Dark -> R.color.black
                                Constants.Theme.Light -> R.color.white
                            }
                            val defaultBg = androidx.core.content.ContextCompat.getColor(context, defaultBgRes)
                            android.widget.Toast.makeText(context, "Same color not allowed", android.widget.Toast.LENGTH_SHORT).show()

                            // Persist original default explicitly (ensure we don't keep the last saved color)
                                // Update pending color to default and show reset visually
                                pendingColor = defaultBg
                                colorPreviewBox.setBackgroundColor(defaultBg)
                            try {
                                // Set flag to prevent infinite loop
                                isUpdatingTextProgrammatically = true
                                try {
                                    rgbText.setText(String.format("#%02X%02X%02X", Color.red(defaultBg), Color.green(defaultBg), Color.blue(defaultBg)))
                                } finally {
                                    isUpdatingTextProgrammatically = false
                                }
                                redSeekBar.progress = Color.red(defaultBg)
                                greenSeekBar.progress = Color.green(defaultBg)
                                blueSeekBar.progress = Color.blue(defaultBg)
                            } catch (_: Exception) {}

                            // Do not apply automatically; user must press OK. Return after updating preview.
                            return@lambda
                        } catch (_: Exception) {
                            // fallthrough to normal behavior on error
                        }
                    }
                }
            } catch (_: Exception) {}

            colorPreviewBox.setBackgroundColor(updatedColor)
            // update text field - use flag to prevent infinite loop
            try {
                isUpdatingTextProgrammatically = true
                try {
                    rgbText.setText(String.format("#%02X%02X%02X", redSeekBar.progress, greenSeekBar.progress, blueSeekBar.progress))
                } finally {
                    isUpdatingTextProgrammatically = false
                }
            } catch (_: Exception) {}
            // Update pending color — do not apply until user confirms with OK
            pendingColor = updatedColor
        }

        // Listeners to update color preview and apply changes
        redSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))
        greenSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))
        blueSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener(updateColorPreview))

        // Listen for text input and update sliders and preview (apply on valid hex)
        rgbText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Skip if we're programmatically updating the text
                if (isUpdatingTextProgrammatically) return
                
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
            val (_, bg) = resolveThemeColors(context)
            layout.setBackgroundColor(bg)
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
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        inputBottomSheetDialog?.dismiss()
                    }
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
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onValueEntered(input.text.toString())
                        inputBottomSheetDialog?.dismiss()
                    }
                }

                // Style buttons: fill = backgroundColor, outline = textColor (thicker)
                try {
                    val (textColor, backgroundColor) = resolveThemeColors(context)
                    val density = context.resources.displayMetrics.density
                    val prefs = Prefs(context)
                    val textIslandsShape = prefs.textIslandsShape
                    val radius = ShapeHelper.getCornerRadiusPx(
                        textIslandsShape = textIslandsShape,
                        density = density
                    )
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(backgroundColor)
                        setStroke(strokeWidth, textColor)
                    }
                    cancelBtn.background = bgDrawable
                    okBtn.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    cancelBtn.setTextColor(textColor)
                    okBtn.setTextColor(textColor)
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
            val (_, bg) = resolveThemeColors(context)
            content.setBackgroundColor(bg)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}

        dialog.setLocked(true)
        dialog.show()
        inputBottomSheetDialog = dialog
    }

    fun showErrorDialog(context: Context, title: String, message: String) {
        // Dismiss any existing error dialogs
        errorBottomSheetDialog?.dismiss()

    // Grab prefs early so we can apply fonts; resolve theme colors for colors
        Prefs(context)
    val (dlgTextColor, dlgBackground) = resolveThemeColors(context)

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
                try { setTextColor(dlgTextColor) } catch (_: Exception) {}
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
                try { setTextColor(dlgTextColor) } catch (_: Exception) {}
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
                setOnClickListener {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    errorBottomSheetDialog?.dismiss()
                }
            }
            // style button using resolved theme colors
            try {
                val density = context.resources.displayMetrics.density
                val prefs = Prefs(context)
                val textIslandsShape = prefs.textIslandsShape
                val radius = ShapeHelper.getCornerRadiusPx(
                    textIslandsShape = textIslandsShape,
                    density = density
                )
                val strokeWidth = (3f * density).toInt()
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(dlgBackground)
                    setStroke(strokeWidth, dlgTextColor)
                }
                okBtn.background = bgDrawable
                okBtn.setTextColor(dlgTextColor)
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
            content.setBackgroundColor(dlgBackground)
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
            try {
                val (textColor, _) = resolveThemeColors(context)
                val csl = android.content.res.ColorStateList.valueOf(textColor)
                this.progressTintList = csl
                this.thumbTintList = csl
                try {
                    val thumb = createCircularThumb(context, textColor)
                    this.thumb = thumb
                    this.thumbOffset = (thumb.intrinsicWidth / 2)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    // Create a circular thumb drawable sized in dp for SeekBars so the thumb
    // is easier to touch/drag. Returns a drawable with intrinsic size set.
    private fun createCircularThumb(context: Context, color: Int): android.graphics.drawable.Drawable {
        val diameterDp = 22
        val diameterPx = (diameterDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val gd = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            // Add a subtle stroke so thumb is visible against any background
            setStroke((2 * context.resources.displayMetrics.density).toInt(), Color.argb(120, 0, 0, 0))
            setSize(diameterPx, diameterPx)
        }
        // Ensure intrinsic size is available by setting bounds
        try {
            gd.setBounds(0, 0, diameterPx, diameterPx)
        } catch (_: Exception) {}
        return gd
    }

    private fun createColorPreviewBox(context: Context, color: Int): View {
        return View(context).apply {
            try {
                val (textColor, _) = resolveThemeColors(context)
                val radius = (4 * context.resources.displayMetrics.density)
                val gd = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(color)
                    setStroke((2 * context.resources.displayMetrics.density).toInt(), textColor)
                }
                background = gd
            } catch (_: Exception) {
                setBackgroundColor(color)
            }
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

    @Suppress("UNUSED")
    fun setDialogFontForAllButtonsAndText(
        dialog: AlertDialog?,
        context: Context,
        contextKey: String = "settings"
    ) {
        val prefs = Prefs(context)
        val (textColor, _) = resolveThemeColors(context)
        val fontFamily = prefs.getFontForContext(contextKey)
        val customFontPath = prefs.getCustomFontPathForContext(contextKey)
        val typeface = fontFamily.getFont(context, customFontPath) ?: getTrueSystemFont()
        val textSize = prefs.settingsSize.toFloat()
        // Set for all buttons
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.let {
            it.typeface = typeface
            it.textSize = textSize
            it.setTextColor(textColor)
        }
        dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.let {
            it.typeface = typeface
            it.textSize = textSize
            it.setTextColor(textColor)
        }
        dialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.let {
            it.typeface = typeface
            it.textSize = textSize
            it.setTextColor(textColor)
        }
        // Set for all TextViews in the dialog view
        (dialog?.window?.decorView as? ViewGroup)?.let { root ->
            fun applyToAllTextViews(view: View) {
                if (view is TextView) {
                    view.typeface = typeface
                    view.textSize = textSize
                    view.setTextColor(textColor)
                    // If it's an EditText, also update hint color for visibility
                    if (view is EditText) {
                        view.setHintTextColor(textColor)
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
    private fun setDialogFontForView(view: View?, context: Context, @Suppress("UNUSED_PARAMETER") contextKey: String = "settings") {
        if (view == null) return

        val prefs = Prefs(context)
        val (textColor, _) = resolveThemeColors(context)
        val fontFamily = prefs.getFontForContext(contextKey)
        val customFontPath = prefs.getCustomFontPathForContext(contextKey)
        val typeface = fontFamily.getFont(context, customFontPath) ?: getTrueSystemFont()
        val textSize = prefs.settingsSize.toFloat()

        fun apply(view: View) {
            if (view is TextView) {
                view.typeface = typeface
                view.textSize = textSize
                view.setTextColor(textColor)
                if (view is EditText) {
                    view.setHintTextColor(textColor)
                }
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    apply(view.getChildAt(i))
                }
            }
        }

        apply(view)
    }

    @Suppress("UNUSED")
    fun showWallpaperTargetDialog(
        context: Context,
        onSelect: (Int) -> Unit,
        onCancel: () -> Unit
    ) {
        wallpaperTargetBottomSheetDialog?.dismiss()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        // Title
        val titleView = TextView(context).apply {
            @Suppress("HardcodedText")
            text = "Set wallpaper for"
            gravity = Gravity.CENTER
            textSize = 18f
        }
        layout.addView(titleView)

        @Suppress("UnnecessaryCheck")
        val supportsLockScreen = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N

        fun makeOption(text: String, flags: Int): TextView {
            return TextView(context).apply {
                this.text = text
                val padding = (16 * context.resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
                gravity = Gravity.CENTER
                textSize = 16f
                setOnClickListener {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    onSelect(flags)
                    wallpaperTargetBottomSheetDialog?.dismiss()
                }
            }
        }

        val spacing = (12 * context.resources.displayMetrics.density).toInt()
        
        // Home screen option
        layout.addView(makeOption("Home Screen", android.app.WallpaperManager.FLAG_SYSTEM))
        
        if (supportsLockScreen) {
            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    spacing
                )
            }
            layout.addView(spacer)
            
            // Lock screen option
            layout.addView(makeOption("Lock Screen", android.app.WallpaperManager.FLAG_LOCK))
            
            val spacer2 = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    spacing
                )
            }
            layout.addView(spacer2)
            
            // Both option
            layout.addView(makeOption("Both", android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK))
        }

        // Buttons row
        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (24 * context.resources.displayMetrics.density).toInt()
            }
            layoutParams = params
            val smallBtnPadding = (12 * context.resources.displayMetrics.density).toInt()
            val smallTextSizeSp = 16f

            val cancelBtn = android.widget.Button(context).apply {
                text = context.getString(R.string.cancel)
                setPadding(smallBtnPadding, smallBtnPadding, smallBtnPadding, smallBtnPadding)
                textSize = smallTextSizeSp
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setOnClickListener {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    onCancel()
                    wallpaperTargetBottomSheetDialog?.dismiss()
                }
            }

            // Style buttons
            try {
                val (textColor, backgroundColor) = resolveThemeColors(context)
                val density = context.resources.displayMetrics.density
                val prefs = Prefs(context)
                val textIslandsShape = prefs.textIslandsShape
                val radius = ShapeHelper.getCornerRadiusPx(
                    textIslandsShape = textIslandsShape,
                    density = density
                )
                val strokeWidth = (3f * density).toInt()
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(backgroundColor)
                    setStroke(strokeWidth, textColor)
                }
                cancelBtn.background = bgDrawable
                cancelBtn.setTextColor(textColor)
            } catch (_: Exception) { }

            val cancelParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            cancelBtn.layoutParams = cancelParams
            addView(cancelBtn)
        }

        layout.addView(buttonsRow)

        // Apply fonts and color styling
        setDialogFontForView(layout, context, "settings")

        // Create bottom sheet dialog
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(layout)
        try {
            val (_, bg) = resolveThemeColors(context)
            layout.setBackgroundColor(bg)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        dialog.setLocked(true)
        dialog.show()
        wallpaperTargetBottomSheetDialog = dialog
    }
}

