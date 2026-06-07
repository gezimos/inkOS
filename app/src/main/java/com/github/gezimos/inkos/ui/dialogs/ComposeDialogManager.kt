package com.github.gezimos.inkos.ui.dialogs

import android.app.Activity
import android.content.Context
import androidx.compose.ui.graphics.Color
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.MainActivity
import com.github.gezimos.inkos.helper.loadFile
import com.github.gezimos.inkos.helper.storeFile
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.ui.compose.AppContextMenuSheet
import com.github.gezimos.inkos.ui.compose.QuickMenuSheet
class ComposeDialogManager(val context: Context, val activity: Activity) {

    private val prefs by lazy { Prefs(context) }
    private val sheetHost = ComposeBottomSheetHost(activity)

    /** Called after any sub-dialog sheet is dismissed (e.g. to re-open the parent settings sheet). */
    var onAfterDismiss: (() -> Unit)? = null
        set(value) {
            field = value
            sheetHost.onDismissed = value
        }

    fun showQuickMenu(
        onInkOSSettings: () -> Unit,
        onEditMode: () -> Unit,
        onEditFavorites: () -> Unit,
        onLookFeel: () -> Unit,
        onAbout: () -> Unit
    ) {
        sheetHost.show {
            QuickMenuSheet(
                onInkOSSettings = onInkOSSettings,
                onEditMode = onEditMode,
                onEditFavorites = onEditFavorites,
                onLookFeel = onLookFeel,
                onAbout = onAbout,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showIconStylePicker(
        currentMode: Int,
        currentIconPack: String,
        iconPacks: List<Pair<String, String>>,
        showIcons: Boolean = true,
        currentIconShape: Int = 0,
        onSelect: (mode: Int, packPackage: String?) -> Unit,
        onShapeSelect: ((Int) -> Unit)? = null
    ) {
        sheetHost.show {
            IconStylePickerSheet(
                currentMode = currentMode,
                currentIconPack = currentIconPack,
                iconPacks = iconPacks,
                showIcons = showIcons,
                currentIconShape = currentIconShape,
                onSelect = onSelect,
                onShapeSelect = onShapeSelect,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showShortcutIconPicker(
        currentIcon: Constants.ShortcutIcon,
        onSelect: (Constants.ShortcutIcon) -> Unit
    ) {
        sheetHost.show {
            ShortcutIconPickerSheet(
                currentIcon = currentIcon,
                onSelect = onSelect
            )
        }
    }

    fun showBadgeIndicatorPicker(
        currentIndicator: Constants.NotificationIndicator,
        onSelect: (Constants.NotificationIndicator) -> Unit
    ) {
        sheetHost.show {
            BadgeIndicatorPickerSheet(
                currentIndicator = currentIndicator,
                onSelect = onSelect
            )
        }
    }

    fun showSliderDialog(
        context: Context,
        title: String,
        minValue: Int,
        maxValue: Int,
        currentValue: Int,
        liveUpdate: Boolean = false,
        step: Int = 1,
        valueFormatter: (Int) -> String = { it.toString() },
        onValueSelected: (Int) -> Unit
    ) {
        sheetHost.show {
            SliderSheet(
                title = title,
                minValue = minValue,
                maxValue = maxValue,
                currentValue = currentValue,
                liveUpdate = liveUpdate,
                step = step,
                valueFormatter = valueFormatter,
                onValueSelected = onValueSelected,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun <T> showSingleChoiceDialog(
        context: Context,
        options: Array<T>,
        titleResId: Int,
        fonts: List<android.graphics.Typeface>? = null,
        fontSize: Float = 18f,
        selectedIndex: Int? = null,
        isCustomFont: ((T) -> Boolean)? = null,
        isBuiltInFont: ((T) -> Boolean)? = null,
        onInfoClick: (() -> Unit)? = null,
        nonSelectable: ((T) -> Boolean)? = null,
        onItemSelected: (T) -> Unit,
        onItemDeleted: ((T, () -> Unit) -> Unit)? = null,
        maxHeightRatio: Float = 0.30f
    ) {
        sheetHost.show {
            SingleChoiceSheet(
                title = context.getString(titleResId),
                options = options.toList(),
                selectedIndex = selectedIndex,
                optionLabel = { it.toString() },
                fonts = fonts,
                fontSize = fontSize,
                nonSelectable = nonSelectable ?: { false },
                onItemDeleted = onItemDeleted,
                isCustomFont = isCustomFont ?: { false },
                isBuiltInFont = isBuiltInFont ?: { false },
                onInfoClick = onInfoClick,
                onSelect = { option, _ -> onItemSelected(option) },
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showTabbedFontPicker(
        context: Context,
        titleResId: Int,
        builtInOptions: List<String>,
        builtInFonts: List<android.graphics.Typeface>,
        customOptions: List<String>,
        customFonts: List<android.graphics.Typeface>,
        selectedIndex: Int? = null,
        isBuiltInFont: (String) -> Boolean = { false },
        onInfoClick: (() -> Unit)? = null,
        addCustomFontLabel: String? = null,
        onAddCustomFont: (() -> Unit)? = null,
        onItemSelected: (String, Boolean) -> Unit,
        onItemDeleted: ((String, () -> Unit) -> Unit)? = null
    ) {
        sheetHost.show {
            TabbedFontPickerSheet(
                title = context.getString(titleResId),
                builtInOptions = builtInOptions,
                builtInFonts = builtInFonts,
                customOptions = customOptions,
                customFonts = customFonts,
                selectedIndex = selectedIndex,
                isBuiltInFont = isBuiltInFont,
                onInfoClick = onInfoClick,
                addCustomFontLabel = addCustomFontLabel,
                onAddCustomFont = onAddCustomFont,
                onSelect = onItemSelected,
                onDelete = onItemDeleted,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showFontLicenseSheet() {
        sheetHost.show {
            FontLicenseSheet(onDismiss = sheetHost::dismiss)
        }
    }

    fun showGestureVsPageConflictSheet() {
        sheetHost.show {
            GestureVsPageConflictSheet(onDismiss = sheetHost::dismiss)
        }
    }

    fun showPrivacyPolicySheet() {
        sheetHost.show {
            PrivacyPolicySheet(onDismiss = sheetHost::dismiss)
        }
    }

    fun showGestureActionPicker(
        context: Context,
        titleResId: Int,
        flag: AppDrawerFlag,
        allowedActions: List<Action>,
        currentAction: Action,
        currentAppLabel: String,
        viewModel: MainViewModel,
        onSelect: (Action, AppListItem?) -> Unit
    ) {
        sheetHost.show {
            GestureActionPickerSheet(
                title = context.getString(titleResId),
                flag = flag,
                allowedActions = allowedActions,
                currentAction = currentAction,
                currentAppLabel = currentAppLabel,
                viewModel = viewModel,
                onSelect = onSelect,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showMultiChoiceDialog(
        context: Context,
        title: String,
        items: Array<String>,
        initialChecked: BooleanArray,
        onConfirm: (selectedIndices: List<Int>) -> Unit,
        maxHeightRatio: Float = 0.60f,
        showSelectAll: Boolean = false
    ) {
        sheetHost.show {
            MultiChoiceSheet(
                title = title,
                items = items.toList(),
                initialChecked = initialChecked,
                onConfirm = onConfirm,
                onDismiss = sheetHost::dismiss,
                showSelectAll = showSelectAll
            )
        }
    }
    
    fun showGroupedShortcutsDialog(
        context: Context,
        title: String,
        pinnedGroup: ShortcutGroup?,
        appGroups: List<ShortcutGroup>,
        selectedKeys: Set<String>,
        onSelectionChanged: (Set<String>) -> Unit
    ) {
        sheetHost.show {
            GroupedMultiChoiceSheet(
                title = title,
                pinnedGroup = pinnedGroup,
                appGroups = appGroups,
                selectedKeys = selectedKeys,
                onSelectionChanged = onSelectionChanged,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showTabbedShortcutsDialog(
        title: String,
        appGroups: List<ShortcutGroup>,
        inkosItems: List<ShortcutItem>,
        pinnedItems: List<ShortcutItem>,
        selectedKeys: Set<String>,
        onSelectionChanged: (Set<String>) -> Unit
    ) {
        sheetHost.show {
            TabbedShortcutsSheet(
                title = title,
                appGroups = appGroups,
                inkosItems = inkosItems,
                pinnedItems = pinnedItems,
                selectedKeys = selectedKeys,
                onSelectionChanged = onSelectionChanged,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showInputDialog(
        context: Context,
        title: String,
        initialValue: String,
        onValueEntered: (String) -> Unit
    ) {
        sheetHost.show {
            InputSheet(
                title = title,
                initialValue = initialValue,
                onConfirm = onValueEntered,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showColorPickerDialog(
        context: Context,
        titleResId: Int,
        color: Int,
        onItemSelected: (Int) -> Unit
    ) {
        val composeColor = Color(color)
        sheetHost.show {
            ColorPickerSheet(
                title = context.getString(titleResId),
                initialColor = composeColor,
                onColorSelected = { selectedColor ->
                    val androidColor = android.graphics.Color.argb(
                        (selectedColor.alpha * 255).toInt().coerceIn(0, 255),
                        (selectedColor.red * 255).toInt().coerceIn(0, 255),
                        (selectedColor.green * 255).toInt().coerceIn(0, 255),
                        (selectedColor.blue * 255).toInt().coerceIn(0, 255)
                    )
                    onItemSelected(androidColor)
                },
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showErrorDialog(context: Context, title: String, message: String) {
        sheetHost.show {
            ErrorSheet(
                title = title,
                message = message,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showBackupRestoreDialog() {
        val mainActivity = activity as MainActivity
        sheetHost.show {
            BackupRestoreSheet(
                onBackupAllData = { storeFile(mainActivity.backupWriteLauncher, Constants.BackupType.FullSystem) },
                onRestoreData = { loadFile(activity, mainActivity.backupReadLauncher) },
                onClearAllData = {
                    activity.window.decorView.post { confirmClearData() }
                },
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showThemeDialog() {
        val mainActivity = activity as MainActivity
        sheetHost.show {
            ThemeSheet(
                onExportTheme = { storeFile(mainActivity.themeBackupWriteLauncher, Constants.BackupType.Theme) },
                onImportTheme = { loadFile(activity, mainActivity.themeBackupReadLauncher) },
                onDismiss = sheetHost::dismiss
            )
        }
    }

    private fun confirmClearData() {
        sheetHost.show {
            ConfirmSheet(
                title = context.getString(R.string.clear_all_data_confirm_title),
                message = context.getString(R.string.clear_all_data_confirm_description),
                confirmText = context.getString(R.string.okay),
                cancelText = context.getString(R.string.cancel),
                onConfirm = {
                    sheetHost.dismiss()
                    prefs.clear()
                    AppReloader.restartApp(context)
                },
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showAppContextMenu(
        app: AppListItem,
        flag: AppDrawerFlag,
        isLocked: Boolean,
        onDelete: (AppListItem) -> Unit,
        onRename: (String, String) -> Unit,
        onHideShow: (AppDrawerFlag, AppListItem) -> Unit,
        onLock: (AppListItem) -> Unit,
        onInfo: (AppListItem) -> Unit
    ) {
        sheetHost.show {
            AppContextMenuSheet(
                app = app,
                flag = flag,
                isLocked = isLocked,
                onDelete = onDelete,
                onRename = onRename,
                onHideShow = onHideShow,
                onLock = onLock,
                onInfo = onInfo,
                onDismiss = sheetHost::dismiss
            )
        }
    }

    fun showSheet(content: @androidx.compose.runtime.Composable () -> Unit) {
        sheetHost.show(content = content)
    }

    fun dismissAll() {
        sheetHost.dismiss()
    }
}
