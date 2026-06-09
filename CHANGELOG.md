# Changelog

## v0.6 - June 2026

### Fixes
- **Fixed crash when tapping apps on the home screen.** Apps with saved message notifications (Signal, WhatsApp, Phone, etc.) could crash the launcher when opened from the home screen. Corrupted leftover notification data is now cleaned up automatically.
- **Fixed crash opening Settings on Android 12-14.** Work-profile detection used an API that only exists on Android 15+, crashing Settings on older versions. It now falls back to the older detection method below Android 15.

## v0.5 - April 2026

### IMPORTANT: How to Access Settings
- **Long-press replaced with pinch.** Long-press was conflicting with page swipes and app opening, so it has been replaced. **Pinch on any empty area of the home screen** to open the quick menu. From there you can access inkOS Settings, Edit Mode, Edit Favorites, System Settings, Wallpaper, and About.

### Settings
- **Redesigned settings menu.** The main settings page has been redesigned with icons.
- **Setting descriptions.** Every setting across all sections now has a description explaining what it does. Edit Mode keeps shorter rows without descriptions to save space.
- **Settings page swiping optimized.** You can now drag to move across multiple settings pages at once.
- **UI scale.** 8 scale modes (Auto, Tiny, Small, Medium, Normal, Big, Large, Extra Large) so inkOS adapts to any screen size and preference.
- **New fonts.** Iosevka and Zalando Sans Expanded added to the font picker.
- **Reworked onboarding.** New users now pick a theme preset during setup, so the launcher looks good from the first launch.
- **Vibration Strength.** New slider under Vibration Feedback to adjust how strong all vibrations feel, so haptics can be tuned to taste.

### Edit Mode
- **Live editing.** Edit Mode lets you change the look and settings of inkOS while seeing the changes in real time. When you enter Edit Mode, a dashed border appears around the screen to indicate you're editing.
- **Tap to edit.** Tap directly on the clock, date row, quote, apps, or background and a bottom sheet with the relevant settings will appear. No need to hunt through settings menus.
- **Works across screens.** You can swipe to other screens like Letters, SimpleTray, Recents, or App Drawer and Edit Mode stays active. Tap on elements or the background to open their settings from any screen.
- **Exiting Edit Mode.** Press the home button or pinch again to open the quick menu and select Exit Edit Mode.

### Home Screen
- **New clock designs.** 11 styles to choose from - Default, Flip, Boxed, Round, Split, Horizontal, Box Outline, Analog, Stacked, Digital, and Matrix.
- **Configurable Notification count indicator.** 5 indicator styles to show unread counts next to app names. Choose the notification source: SimpleTray, Letters, or Hub.
- **Per-element alignment.** Clock, date, apps, and quote can each be aligned independently (left, center, or right), so you can create asymmetrical layouts - for example, clock on the left, apps centered, quote on the right.
- **Bottom widgets.** The area below your apps now supports 7 widget types: Quote, Calendar Events, Android Widget, Shortcuts, Total Usage, Page Dots, or Disabled. Each one might have configurable settings. Only One widget allowed.
- **Shortcuts widget.** Two customizable buttons (left and right) with 11 icon options and configurable tap actions. Quick access to anything without leaving the home screen.
- **Calendar events widget.** Shows upcoming events from selected calendars with a time range filter (next 24 hours, 1 week, 2 weeks, or 1 month).
- **Android widget embedding.** Place any standard Android widget directly on your home screen with adjustable height and margins.
- **Separators.** Empty Space returns from v0.2 with two new options - Em Dash and Dots. Assign separators to any home slot to visually group your apps.

### Shortcuts
- **Redesigned shortcut system.** The entire shortcut architecture has been rebuilt. Migration from v0.4 is automatic - your existing setup carries over.
- **Pinned shortcuts.** Third-party apps (like Activity Launcher) can now pin shortcuts directly into inkOS using Android's standard pin shortcut API.
- **App shortcuts.** Apps that define their own shortcuts (e.g., Signal conversations, WhatsApp contacts) can now be launched directly from the home screen or app drawer.
- **Web shortcuts.** Save any website to your home screen as a shortcut, complete with its favicon.
- **Progressive web app shortcuts.** PWA shortcuts are supported with automatic favicon detection.
- **Shortcut management.** All shortcuts are managed from Settings/Appdrawer or the App Drawer (via Edit mode).
- **System shortcuts removed from app list.** Device utilities like Default Apps, Accessibility, and Display Settings are no longer synthetic apps in the drawer. To find them, enable "Search Settings" in App Drawer settings - they'll appear in search results and launch directly to the right Android settings page.

### Icons
- **4 icon modes.** Choose how your apps look: Text (periodic-table style letter acronyms), System (default adaptive icons), inkOS Tinted (system icons with a luminance color filter based on your theme), or Icon Packs (any third-party pack from the Play Store).
- **Icon shapes.** 3 shape options for icon backgrounds: Pill, Rounded, and Square.

### Themes
- **15 one-tap theme presets.** Each preset configures your entire look in one tap - colors, font, icon mode, icon shape, clock style, layout alignment, app count, and more. Designed so you can get a polished setup without touching individual settings.
- **Custom themes.** Start from any preset and tweak it, or build your own from scratch. Every visual setting is exposed.
- **Theme export/import.** Share your setup or back it up via Settings > Advanced. Exports include layout, fonts, alignments, sizes - no private data.
- **Independent light and dark mode colors.** Light mode and dark mode now have completely separate color settings (no longer just inverted). The color editor has dedicated tabs for each.
- **System theme mode.** Automatically switches between light and dark based on your device's system setting.
- **Smart wallpaper opacity.** Background opacity automatically resets to 0 (fully transparent) when you set a wallpaper, so it's visible immediately.

### Notifications
- **Hub.** A new full notification center. Notifications are grouped into category tabs (Messages, Email, Events, Other) with per-notification actions and a Clear All button.
- **Letters.** Renamed from Notifications.
- **SimpleTray.**  Fixed brightness slider conflicting with the system brightness slider. Fixed an issue where quick setting toggles (Wi-Fi, Bluetooth, etc.) would not open their settings panels on some devices.
- **Bulk allowlist toggle.** Notification allowlist screens now have Select All and Deselect All buttons so you can flip every app at once instead of tapping each row.

### App Drawer
- **Sort order.** Sort your apps by A-Z (default), Most Used, or Last Used.
- **Drawer icons.** Optionally show icons next to app names in the drawer.
- **A-Z sidebar.** Now shows the currently active letter instead of the old `>_` cursor, so you always know where you are.
- **Updated context menu.** Long-press any app in the drawer for a redesigned context menu with options like rename, hide, uninstall, and app info.
- **Search hidden apps.** Toggle to include hidden apps in drawer search results.

### Search
- **Search sources.** Each source can be individually toggled in App Drawer settings. All are off by default except app search.
- **Contacts.** Search your contact list directly from the drawer. Filter by account type (Google, SIM, etc.) if you have multiple accounts. Requires contact permission.
- **Device Settings.** Search Android settings like Wi-Fi, Bluetooth, Display, Battery, Accessibility, and more. Results launch directly to the right settings page.
- **Web.** Type a search query or enter a URL directly. If nothing matches on your device, a "Search web" option appears that opens your default browser.
- **Music.** Search by song title or artist name. Results open in your default music player. Requires media permission.
- **Files.** Search files inside folders you choose via a folder picker. Searches up to 3 levels deep.
- **Hidden apps.** Optionally include hidden apps in search results.
- **Contact photos.** Search results for contacts show their profile photos.
- **Search as a gesture.** Assign search to any gesture - opens the drawer with the keyboard ready.
- **Auto-launch.** When search narrows to a single result, it opens automatically.
- **Smarter ranking.** Results are ranked by frequency with time decay, so recent apps surface higher.

### Gestures
- **Multi-page home.** When using more than one home page, short swipes (up/down) navigate between pages, and long swipes trigger gesture actions like opening an app, search, or settings.
- **Single-page home.** When using only one home page, short swipes trigger gesture actions directly since there are no pages to navigate.
- **Configurable thresholds.** Short and long swipe threshold ratios are now adjustable, so you can fine-tune when a swipe becomes a gesture.
- **New gesture actions.** Search, Hub, Settings, Toggle Work Profile, and Toggle Private Space are now available as gesture targets.

### Recents
- **Two modes.** Toggle between a Recent Apps list and a Usage Statistics breakdown.
- **Usage filters.** Filter stats by Today, This Week, This Month, or All Time.
- **Usage units.** Display usage as Time, Money (with configurable currency), or Coffee equivalent.

### Private Space & Work Profile
- **Private Space.** Full support for Android 15+ Private Space. Requires inkOS as the default launcher. Private apps are completely separated from your main app list.
- **Toggle Private Space gesture.** Assign Toggle Private Space to any gesture to quickly activate or lock your private space. Once activated, your private apps and shortcuts become accessible.
- **Accessing private apps.** In the App Drawer, swipe down while on the first page or tap the shield icon in the A-Z sidebar to view your private apps.
- **Profile indicators.** Private and work profile apps are marked with `^` on the home screen and in the drawer, so you always know which profile an app belongs to.
- **Work Profile controls.** Pause and resume your Work Profile directly from inkOS.

### Other
- **E-ink modes.** Reworked from a simple toggle to 4 distinct modes: Disabled, Contrast, Clear, and Reading (Mudita Kompakt only). Thanks to [UndefinedProgrammer](https://github.com/UndefinedProgrammer/InkMaster) for the E-ink helper fix.
- **Wallpaper.** inkOS's own wallpaper system is back, running alongside Android's native wallpaper.
- **Audio widget.** Improved detection. Spotify's persistent notification is now ignored after pressing stop, so the widget properly hides.
- **Android widgets.** Two new widgets for your lockscreen or home: Notification (shows top 5 notification sources with counts) and Quote (displays your quote text).

---

## v0.4 - 24 December 2025

### Home
- **Home alignment.** Left, center, or right alignment for the entire home screen (left by default).
- **Dual clocks.** Optional second clock with 24/12hr toggle. The second clock uses a simple timezone offset.
- **Charging indicator.** A subtle indicator on the home screen when charging.
- Removed app reordering.

### App Drawer
- **A-Z filter** enabled by default with full diacritic support.
- Page indicator dots removed.
- **Hide home apps** option to avoid seeing duplicates in the drawer.
- **Search** with optional auto-show keyboard and auto-launch when only one result matches.

### Colors & Wallpaper
- Added a rule preventing identical background and text colors (avoids unreadable UIs).
- Merged text color preferences for simpler management.
- Removed System theme mode (was unreliable with scheduled theme changes).
- Wallpaper switched to Android's native wallpaper system with an editor and presets.
- Background opacity slider from 0 (fully transparent) to 255 (fully opaque).

### Fonts
- More font choices added. Default changed to PublicSans.

### Gestures
- Swipe up/down gestures with a configurable swipe threshold.

### Notifications
- **SimpleTray** now has configurable notifications per page and toggleable bottom buttons.
- Notification icons now link directly to the app's notification settings.

### Extras
- Removed Bluetooth fragment from Extra Settings.
- Added Default Apps shortcut in System Shortcuts.

### New Screens
- **SimpleTray.** Combines quick settings and notifications in one panel (long swipe down). Requires extra permissions.
- **Recents.** Shows recent and most-used apps. Requires usage access permission.

---

## v0.3 - 3 October 2025

### Home & App Drawer
- Performance improvements with better caching for faster page loading.

### Brightness
- Removed the minimum brightness threshold (was 25/255). Gestures can now set the front light to its true minimum.

### Clock/Date
- Fixed a bug where old preferences were saved when only alarm/calendar actions were available.

### Volume Navigation
- Fixed volume buttons stopping after adding a new app, swiping, or using keypad navigation.
- Volume navigation is now disabled during audio playback, so volume keys control media instead of navigating pages.

### Home Screen
- D-pad navigation can now move to the previous page.
- Widgets are no longer focusable (keypad shortcuts 2, 6, 7, 8 access widgets directly).
- Added vibration feedback for long-press and Set Home App actions.

### Notifications
- Long-pressing the Dismiss button now clears all notifications at once.
- Switched to dashed separators for less visual distraction.
- Notification titles are now limited to one line to prevent layout breakage.
- Hidden apps can now be added to notification allowlists.

### App Drawer
- Swiping left or right from the edges now goes back (avoids diagonal swipe conflicts).
- Page count now updates correctly after renaming, hiding, showing, or uninstalling an app.

### Settings
- Replaced full-line separators with dashed separators for cleaner focus on text.
- Added a donation link.

### Extras
- Added "Privacy" shortcut for quick camera access (Mudita Kompakt).
- Renamed "E-ink Quality Mode" to "E-ink Auto Mode".

---

## v0.2 - 1 September 2025

### Gestures
- You can now choose which app to launch for Swipe Left, Swipe Right, and Clock gestures.
- New gesture targets: Open Drawer, Click on Date, E-ink Refresh (double tap), Exit inkOS, Lockscreen, Screenshot, Power Dialog, Recents, Quick Settings.
- Removed Next/Previous Page from Swipe Left and Right gestures.

### Features
- **Home Page Reset.** Pressing the home button now returns to page 1.
- **Small Caps Apps.** Option to display app names in lowercase (e.g., "Camera" becomes "camera").

### Look & Feel
- Background image support with an opacity slider.
- Toggle to show or hide the gesture/navigation bar for a fullscreen look.
- Improved vibration feedback - now works for gestures, apps, and widgets, not just page scrolling.

### App Drawer
- Work Profile apps now show a briefcase icon.
- App Drawer itself can be added as an "app" to your home screen.

### Home
- **Simple Date Widget.** A minimal date display for the home screen.
- Configurable top margin for clock/date and bottom margin for battery/quote.
- **Empty Spaces.** Invisible home slots that let you create uneven layouts and custom spacing between apps.
- Fixed the page-indicator dot shift bug (dots shifted left with each added page).
- **Audio Widget.** Appears automatically when audio is playing, stays visible when paused, and can be dismissed by pressing Stop.

### Notifications
- Keypad 1 now dismisses notifications, keypad 3 opens them.
- Replaced the music note icon with `*` (fixed a padding issue from the Unicode character shape).

### Advanced
- Fixed Hidden Apps not importing correctly.

### Settings
- Improved paged scrolling with fewer accidental vertical swipes on touch devices.

### Other
- Updated the app icon.
- Fixed dynamic and legacy icon rendering.
