# 📱 App Documentation

---

## 🔧 Home Features

### Layout & Positioning

**Gap Between Apps**
Controls the gap between home app items. Font family and size can affect this spacing, so it's recommended to adjust it for better visibility.

**Home Apps Y Offset**
Adjusts the vertical position of home apps on the screen (makes them off-centre).

**Top Widget Margin**
Adjusts the top margin for the clock/date widget.

**Bottom Widget Margin**
Adjusts the bottom margin for the media/quote widget.

**Home Alignment**
Sets the horizontal alignment of home apps (Left, Center, Right).

### Home Apps

**Apps on Home Screen**
Sets how many apps in total are featured on the home screen.

**Pages on Home Screen**
Determines the total number of pages. Apps will be auto-divided across pages.

**Enable Page Indicator**
Displays dots on the right side to indicate the current page.

**Home Page Reset**
When enabled, pressing the home button will reset to the 1st page (by default, it remembers your last page).

**Extend Home Apps Area**
Makes the full horizontal area of app names clickable.
E.g., `[Calendar] → [      Calendar      ]`

### Top Widgets

#### Clock

**Show Clock**
Enables the clock widget at the top of the home screen.

**Clock Format**
Choose between System, 24-hour, 12-hour, or 12-hour alt format.

**Clock Style**
Visual style of the clock display. Options: Default, Flip, Boxed, Round, Split, Horizontal, Box Outline, Analog.

**Show AM/PM**
Displays AM/PM indicator for 12-hour format.

**Dual Clocks**
Enables a second clock display with a manual timezone offset.

**Second Clock Offset**
Sets the timezone offset for the second clock (when dual clocks is enabled).

**Clock Alignment**
Horizontal alignment of the clock (Left, Center, Right).

#### Date Row

**Show Date**
Displays the date widget on the home screen.

**Date Format**
Choose how the date is displayed.

**Show Battery**
Shows battery percentage next to the date.

**Show Notification Count**
Displays the count of unread notifications in the date row.

**Notification Count Source**
Choose between SimpleTray or Letters as the notification count source (shown when notification count is enabled).

**Date Alignment**
Horizontal alignment of the date row (Left, Center, Right).

### Bottom Widgets

**Show Audio Widget**
Widget appears when audio is playing, remains even if paused, and can be dismissed by clicking stop.

**Bottom Widget**
Choose your bottom widget type. Options:
- **Disabled** — No bottom widget
- **Quote** — Custom text widget
- **Events** — Calendar events from your device
- **Android Widget** — Embed any Android widget
- **Shortcuts** — App shortcuts
- **Total Usage** — Screen time usage
- **Page Dots** — Page indicator dots

**Widget Alignment**
Horizontal alignment of the bottom widget (Left, Center, Right). Not available for Android Widget or Disabled.

#### Quote Widget

**Quote Text**
Sets the custom quote text to display.

#### Events Widget

**Choose Calendar**
Select which calendar to display events from (or all calendars). Requires calendar permission.

**Events Filter**
Filter events by time range: 24 hours, 1 week, 2 weeks, or 1 month.

#### Android Widget

**Choose Android Widget**
Select an Android widget from installed apps to embed on the home screen.

**Widget Height**
Height of the embedded Android widget (in dp).

**Left Margin / Right Margin**
Horizontal margins for the Android widget (in %).

**Remove Android Widget**
Remove the currently set widget.

---

## 🅰 Fonts

**Universal Custom Font**
Sets a single font family for all font-selectable items. When enabled, only text size can be modified.
*Tip: Set a universal font, then disable it to customize specific fonts (e.g., a display font for the clock).*

**Settings Font**
Controls font family for all settings menus (disabled when universal font is enabled).

**Settings Text Size**
Controls text size for all settings menus.

### Home Fonts

**Apps Font**
Controls font family for home menu apps, app drawer, and hidden apps (disabled when universal font is enabled).

**App Text Size**
Controls text size for home menu apps.

**App Name Mode**
Choose between Lettercase, lowercase, or UPPERCASE for app names.

**Clock Font**
Controls font family for the Clock widget (disabled when universal font is enabled).

**Clock Text Size**
Controls text size for the Clock widget.

**Date Font**
Controls font family for the Date widget (disabled when universal font is enabled).

**Date Text Size**
Controls text size for the Date widget.

**Quote Font**
Controls font family for the Quote/bottom widget (disabled when universal font is enabled).

**Quote Text Size**
Controls text size for the Quote/bottom widget.

### Label Notifications

**Label Notifications Font**
Controls font family for notification previews under app names (disabled when universal font is enabled).

**Label Notifications Size**
Controls text size for notification label previews.

### Letters Window

**Window Title**
Sets the title string for the letters notification window.

**Title Font**
Controls font family for the letters window title (disabled when universal font is enabled).

**Title Size**
Controls text size for the letters window title.

**Body Font**
Controls font family for the body text in the letters window (disabled when universal font is enabled).

**Body Text Size**
Controls text size for the body text in the letters window.

---

## 🎨 Look & Feel

### Visibility & Display

**Themes**
Choose a preset color theme from the theme editor.

**Theme Mode**
Switch between Light, Dark, or System themes.

**Element Colors**
Customize background and text colors in the color editor.

**Set Wallpaper**
Opens the wallpaper selection screen to set a custom background image.

**Background Color Opacity**
Adjusts the opacity of the background color (0-255). Set to 0 to make the wallpaper fully visible.

### Icons & Buttons

**Show Icons**
Display app icons next to app names.

**Icon Style**
Choose how icons are rendered. Options:
- **Letters** — Periodic-table style text acronyms (no bitmap)
- **System** — Full adaptive icons from the system
- **System Tinted** — System icons with a luminance color filter
- **Icon Packs** — Any installed third-party icon pack

**Icon Shape**
Shape of icon masks: Pill, Rounded, or Square.

**Button-Island Corners**
Corner style for UI buttons: Pill, Rounded, or Square.

### Text Islands

**Enable Text Islands**
Enables rounded/pill-shaped backgrounds behind text elements.

**Invert Islands**
Inverts the colors of text islands (white text on black background becomes black text on white background).

### System

**Vibration Feedback**
Enables vibration feedback for gestures, apps, widgets, and interactions.

**Show Status Bar**
Displays the top status bar (carrier, clock, battery, Wi-Fi, Bluetooth).

**Show Navigation Bar**
Option to show/hide the navigation bar for fullscreen look.

---

## ✋ Gestures

### Tap & Click Actions

**Double Tap (2)**
Customize double tap to open an app or perform an action.

**Click on Clock (6)**
Customize clock tap to open an app or perform an action.

**Click on Date (7)**
Customize date tap to open an app or perform an action.

**Click on Quote (8)**
Customize quote widget tap to open an app or perform an action.

### Swipe Gestures

**Swipe Left (>)**
Customize left swipe to open an app or perform an action.

**Swipe Right (<)**
Customize right swipe to open an app or perform an action.

**Swipe Up (^)**
Customize up swipe to open an app or perform an action.

**Swipe Down (v)**
Customize down swipe to open an app or perform an action.

**Gesture Options**
- Open App (not available for double tap)
- Open App Drawer
- Open Notifications Screen
- Open Recents Screen
- Open Simple Tray
- Manual E-Ink Refresh
- Brightness on/off
- Lock Screen
- Show Recents
- Open Quick Settings
- Open Power Dialog
- Restart Launcher
- Exit inkOS (switch between launchers)
- Toggle Private Space (if supported)
- Toggle Work Profile (if available)
- Disabled

### Swipe Threshold Ratios

**Short Swipe Ratio**
Adjusts the sensitivity threshold for short swipes (0.01 to 1.0). Lower values make swipes more sensitive.

**Long Swipe Ratio**
Adjusts the sensitivity threshold for long swipes (1.1 to 5.0). Higher values require longer swipes to trigger.

**Edge Swipe Back**
Enables edge swipe back gesture functionality.

---

## 🔔 Notifications

**Push Notifications**
Enable or disable notifications system-wide.
*System permission dialog will guide you to enable notification listener access.*

### Home Notifications

**Asterisk Notification**
Adds a `*` next to app names with pending notifications.

**Label Notifications**
Shows actual notification content below app names.
*Great for chat/media apps.*

**Media Playing Indicator**
Displays a `*` beside apps currently playing media.

**Media Playing Name**
Displays the name of currently playing media (e.g., song, podcast, audio file).

**Home Notifications Allowlist**
Choose which apps can show label notifications on the home screen.
*Highly recommended for focus and clarity.*

### Chat Notifications

**Show Sender Name**
Displays the sender's name in notification previews.

**Show Conversation/Group Name**
Displays the conversation or group name in notification previews.

**Show Message**
Displays the message preview in notification previews.

**Badge Character Limit**
Sets the maximum number of characters to display in notification previews (5-50).
*Important for layout stability—depends on font and size.*

### Letters Window

**Enable Letters**
Enables the dedicated letters notification window to read full messages.

**Clear When App Opens**
Automatically clears notifications when you open the app.

**Letters Allowlist**
Choose which apps can show notifications in the letters window.
*Has a separate allowlist from home notifications.*

**Keyboard/DPAD Shortcuts:**
- **Dismiss:** Del, C, or #1 key
- **Dismiss All:** Long press dismiss
- **Open:** Enter, Dpad Center, or #3 key
- **Navigate:** Volume Up/Down, Dpad Up/Down, or Page Up/Down
- **Dpad Left:** Dismiss current notification
- **Dpad Right:** Open current notification

### Simple Tray

**Notifications Per Page**
Sets how many notifications to display per page in Simple Tray (1-5).

**Enable Bottom Navigation**
Shows navigation controls at the bottom of Simple Tray.

**Simple Tray Allowlist**
Choose which apps can show notifications in Simple Tray.

---

## ⚙️ Advanced

**Lock Home Apps**
Prevents app changes on the home screen via long press.

**Long Press for App Info**
Opens the system dialog to uninstall or force stop apps when long-pressing apps (only available when home apps are locked).
*Especially useful for phones with no recents menu.*

**Lock Settings**
Lock the Settings menu with fingerprint or PIN to avoid accidental changes.

**Backup / Restore**
- **Backup All Data:** Save all current settings for future restoration.
- **Backup Theme:** Export just the theme/color settings.
- **Restore:** Load saved settings.
- **Clear All Data:** Clears all settings and data for inkOS, so you can start from scratch.
*Note: Custom fonts are not backed up.*

**Change Default Launcher**
Opens system settings to set inkOS as the default launcher.

**Restart Launcher**
Restarts the inkOS launcher.

**Exit inkOS**
Opens the launcher chooser to switch to another launcher.

---

## 🗂️ App Drawer

Displays a scrollable list of all installed apps.

**Long Press Options:**
1. **Delete:** Uninstalls the app
2. **Rename:** Change app name/alias (affects home too)
3. **Hide:** Moves the app to the hidden apps list
4. **Lock:** Requires fingerprint/PIN to open
5. **Info:** Opens the system info dialog

**App Drawer as an App**
App Drawer can be added as an app in the app list.

### Customizations

**App Size**
Controls the size of app items in the app drawer.

**Gap Between Apps**
Controls the gap between app items in the app drawer.

**App Drawer Alignment**
Sets the horizontal alignment of apps in the drawer (Left, Center, Right).

**Enable Icons**
Show app icons in the drawer.

### Filtering

**AZ Filter**
Enables alphabetical filtering sidebar for quick navigation.

**Sort Order**
Order of apps in the drawer: A-Z, Most Used, or Last Used.

**Hide Home Apps**
Hides apps that are already on the home screen from the app drawer.

### Search

**Enable Search**
Enables the search functionality in the app drawer.

**Auto Show Keyboard**
Automatically shows the keyboard when opening the app drawer search (requires search to be enabled).

**Auto-launch Result**
Automatically launches the first search result when typing in the app drawer search.

**Search Hidden Apps**
Include hidden apps in search results.

**Search Contacts**
Include contacts in search results. Requires contacts permission. When multiple contact accounts exist, you can choose which accounts to include.

**Search Web**
Include web search in results.

**Search Settings**
Include device settings in results.

**Search Music**
Include music in search results. Requires audio/storage permission.

**Search Files**
Search files in selected folders. When enabled, you can manage which folders to search.

---

## 🔧 Extras

### Private Space & Work Profile

**Private Space** (Android 15+)
Toggle private space on/off. Requires inkOS to be the default launcher.

**Work Profile**
Pause or resume your work profile apps.

### E-Ink Features

**E-Ink Auto Mode** (Mudita Kompakt only)
E-ink display mode selector: Clear > Contrast > Reading.

**Auto E-Ink Refresh**
Optimized for e-ink devices. Flashes the screen after exiting apps to clean ghosting artifacts.
*Note: Doesn't apply to overlays like quick settings; press the home button to exit.*

**Auto Refresh Only in Home**
When enabled, auto refresh only occurs when returning to the home screen (requires Auto E-Ink Refresh to be enabled).

**E-Ink Refresh Delay**
Sets the delay in milliseconds before the e-ink refresh occurs (adjustable in 25ms increments).

**Use Volume Keys for Pages**
Navigate between pages using the volume keys.

---
