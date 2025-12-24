# Changelog inkOS v0.4 - 24 December 2025

## Home
- Home alignment: left/center/right options (left default).
- Dual clocks option and 24/12hr toggle; second clock uses a simple timezone offset.
- Added charging indicator on home.
- Removed re-order apps.

## AppDrawer Settings
- AZ filter is default; supports diacritics.
- Page indicator dots removed.
- "Hide home apps" added to avoid duplicates.
- Search function added with optional auto-show keyboard.
- Auto-launch result opens the closest match automatically.

## Look & Feel: Colors
- Rule to disallow identical background and text colors to avoid unreadable UIs.
- Merged text color preferences for simpler management; recommended black/white for e-ink.
- Removed System theme mode (was unreliable with scheduled theme changes).
- Wallpaper system switched to Android Wallpaper System; includes editor and presets.
- Background opacity ranges 0–255; 0 is fully transparent.

## Fonts
- Added more font choices; default changed to PublicSans.

## Gestures
- Added swipe up/down gestures with configurable Swipe Threshold to avoid page-swipe conflicts.

## Notifications
- SimpleTray settings: notifications per page and bottom-button toggles.
- Notification icons in SimpleTray/Letters link to app notification settings.

## Extras
- Removed mKompakt Bluetooth fragment from Extra Settings.
- Added default apps shortcut in System Shortcuts.

## New Fragments
- Simple Tray: combines Quick Settings and notifications (long swipe down). Requires extra permissions.
- Recents: replacement Recents; shows recent and most-used apps (usage access required).

Notes:
- Backup data at Advanced / Backup&Restore before updating.

# Changelog inkOS v0.3 - 03 October 20225

## Home & Appdrawer
- Performance Improvmenets: Better caching for faster loading of pages.

## Brightness Gesture/Action
- Removed minimum brightness threshold (was 25/255); gestures can now set front light to true minimum.

## Clock/Date
- Fixed bug where old preferences were saved when only alarm/calendar actions were available for clock widget.

## Volume Navigation
- Fixed: Volume navigation buttons stopped working after adding a new app, swiping, or keypad up/down presses on keypad phones.
- Added: Volume navigation is disabled when AudioWidget detects audio playing, allowing volume changes without opening the app. If audio is paused/stopped, page navigation with volume keys works as usual (if enabled in Settings / Extras).

## Home Screen & Widgets
- Changed home screen app navigation dpad logic: can now move to previous page using dpad app.
- Widgets are no longer focusable (keypad shortcuts 2,6,7,8 access widgets).
- Added simple vibration feedback for long-press to go to settings and for Set Home App function.

## Notification Window & Settings
- Long-pressing Dismiss button now dismisses all notifications.
- Changed solid separators to dashed separators for less visual distraction.
- Limited notification title to one line to prevent layout breakage.
- Notification allowlists: Hidden apps can now be set; fixed bug where Home Notification Allowlist package list did not update.

## App Drawer
- Swiping left or right in App Drawer now goes back (only from edges to avoid diagonal swipe conflict).
- Fixed page miscalculation after renaming, hiding, showing, or uninstalling an app; pages now update properly without reopening App Drawer.

## Notifications Screen
- Swiping left or right goes back (only from edges).

## Extras Screen
- Added "Privacy" shortcut in Extras / System shortcuts for quick camera access (for Mudita Kompakt).
- Renamed "E-ink Quality Mode / E-ink mode" to "E-ink Auto Mode".

## Settings
- Replaced full-line separators with dashed separators for better focus on text.
- Added donation link in Settings.

# Changelog inkOS v0.2 - 01 September 20225

## Gesture Settings
- Added option to choose which app the **Open App** gesture launches (Swipe Left, Swipe Right, Clock).
- Added option to set gestures to **Open Drawer**.
- Added **Click on Date** gesture.
- Added **E-ink refresh** for **Double Tap** (manual refresh to clear ghosting without enabling auto-refresh).
- Added option to **Exit inkOS** via gestures (quickly switch between launchers without KeyMapper).
- Added gesture options for: **Lockscreen, Screenshot, Power Dialog, Recents, Quick Settings**.
- Removed **Next Page / Previous Page** from Swipe Left & Right (to avoid confusion).

## Feature Settings
- Added **Home Page Reset** option (Home button returns to page 1).
- Added **Small Caps Apps** option (e.g., “Camera” → “camera”).

## Look & Feel
- Added option for **Background Image & Opacity** on home.
- Added **Show Gesture/Navbar** toggle for fullscreen look.
- Improved **Vibration Feedback** (now works for gestures, apps, widgets, not just page scrolling).

## App Drawer
- Added **Work Profile (briefcase) icon**.
- Added App Drawer as an “app” (can now be added to the app list).

## Home
- Added **Home Page Reset** (returns to page 1 when leaving another app).
- Added **Simple Date Widget**.
- Added **Top Margin for Clock/Date**.
- Added **Bottom Margin for Battery/Quote**.
- Added **Simple Background Image with Opacity Slider** (not tied to Android wallpaper system).
- Added option to add **Empty Spaces** (acts as an app, allows uneven layouts and repositioning).
- Fixed **page-indicator dots bug** (shifting left with each added page).
- Added **Audio Widget** (appears when audio is playing, persists when paused, dismissible via Stop).

## Notifications
- Added **Keypad 1** → dismiss notifications.
- Added **Keypad 3** → open notifications.
- Changed **music note icon** to `*` (fixed padding issue from Unicode shape).

## Advanced
- Fixed **Hidden Apps** not importing correctly.

## Settings
- Improved **Paged Scrolling** (fewer accidental vertical swipes on touch devices).

## Others
- Updated **App Icon**.
- Fixed **Dynamic & Legacy Icons**.
