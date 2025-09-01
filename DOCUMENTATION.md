# 📱 App Documentation

---

## 🔧 Features

**Padding Size**  
Controls the gap between home app items. Font family and size can affect this spacing, so it’s recommended to adjust it for better visibility.

**Re-order Apps**  
Easily drag and drop apps to rearrange their order in the home menu.

**Extend Clickable Area**  
Makes the full horizontal area of app names clickable.  
E.g., `[Calendar] → [      Calendar      ]`

**Number of Home Apps**  
Sets how many apps in total are featured on the home screen.

**Number of Pages**  
Determines the total number of pages. Apps will be auto-divided across pages.

**Enable Page Indicator**  
Displays dots on the right side to indicate the current page.

**Show Clock**  
Enables the clock widget at the top of the home screen.

**Show Battery**  
Enables the battery widget at the bottom of the home screen.

**Home Page Reset**  
When enabled, pressing the home button will reset to the 1st page (by default, it remembers your last page).

**Small Caps Apps**  
This feature changes all app names in home and app drawer to be small caps (e.g., Camera → camera).

**Empty Spaces App**  
Add empty spaces as an app to create uneven layouts or reposition apps higher/lower on the page.

---

## 🅰 Fonts

**Universal Font**  
Sets a single font family for all font-selectable items. When enabled, only text size can be modified.  
*Tip: Set a universal font, then disable it to customize specific fonts (e.g., a display font for the clock).*

**Settings Font/Text Size**  
Controls font family and text size for all settings menus.

**App Font/Text Size**  
Controls font and size for home menu apps, app drawer, and hidden apps.

**Clock Font/Text Size**  
Controls font and size for the Clock widget.

**Battery Font/Text Size**  
Controls font and size for the Battery widget.

**Label Notifications**  
Controls font and size for notification previews under app names.  
E.g.,  
`Whatsapp`  
`John: Message goes here`  
*Character limit adjustable in* **Settings > Notifications**.

**Window Title/Font/Size**  
Sets the title string and its font/size for the notification window (opened by swiping left).

**Body Font**  
Controls the body text in the notification window.

---

## 🎨 Look & Feel

**Theme Mode**  
Switch between light and dark themes.

**E-Ink Auto Refresh**  
Optimized for e-ink devices (e.g., Mudita Kompakt). Flashes the screen after exiting apps to clean ghosting artifacts.  
*Note: Doesn't apply to overlays like quick settings; press the home button to exit.*

**Manual E-Ink Refresh (Double Tap)**  
Double tap gesture can be set to manually refresh the screen and clear ghosting, without enabling auto-refresh.

**Show Status Bar**  
Displays the top status bar (carrier, clock, battery, Wi-Fi, Bluetooth).

**Element Colors**  
Allows setting custom UI colors.  
*Recommended only for AMOLED displays. Not suitable for e-ink screens.*

**Background Image / Opacity**  
Set a custom background image and adjust its opacity for the home screen (not tied to Android wallpaper system).

**Show Gesture/Navbar**  
Option to hide/show gesture bar and navbar for fullscreen look.

**Top Margin for Clock/Date**  
Adjust the top margin for the clock/date widget.

**Bottom Margin for Battery/Quote**  
Adjust the bottom margin for the battery/quote widget.

**Simple Date Widget**  
Add a simple date widget to the home screen.

**Vibration Feedback**  
Vibration feedback is now available for gestures, apps, widgets, and not only for scrolling up/down pages.

---

## ✋ Gestures

**Volume Keys Swipe**  
Navigate between pages using the volume keys.

**Double Tap**  
Customize double tap to:
- Restart launcher
- Open notifications window
- Manual E-Ink refresh (flash and clear ghosting)
- Disable the gesture

**Click on Clock**  
Customize clock tap to:
- Open clock (alarm app)
- Restart launcher
- Open notifications window

**Click on Date**  
Customize date tap as a gesture action.

**Swipe Left / Swipe Right / Clock**  
Choose which app the gesture opens, or assign to:
- Open Drawer
- Open Notifications
- Open Apps
- Lockscreen
- Power dialog
- Recents
- Quick settings
- Exit inkOS (switch between launchers)

---

## 🔔 Notifications

**Push Notifications**  
Enable or disable notifications system-wide.  
*System permission dialog will guide you.*

**Asterisk**  
Adds a `*` next to app names with pending notifications.

**Label Notifications**  
Shows actual notification content below app names.  
*Great for chat/media apps.*

**Media Playing Indicator**  
Displays a `*` beside apps currently playing media (previously a music note `♪`).

**Media Playing Name**  
Displays the name of currently playing media (e.g., song, podcast, audio file).

**Home Notifications Allowlist**  
Choose which apps can show label notifications on the home screen.  
*Highly recommended for focus and clarity.*

**Chat Notifications**  
Controls for:
- Sender/group name
- Message preview
- Character limits  
*Important for layout stability—depends on font and size.*

**Notification Window**  
Enables a dedicated window to read full messages.
- Has a separate allowlist
- Some limitations apply based on app permissions
- Keypad shortcuts: 1 to dismiss notifications, 3 to open notifications (for keypad phones)

**Audio Widget**  
Widget appears when audio is playing, remains even if paused, and can be dismissed by clicking stop.

---

## ⚙️ Advanced

**Hidden Apps**  
Hide apps from the App Drawer (e.g., system apps).

**Lock Home Apps**  
Prevents app changes on the home screen via long press.

**Long Press for App Info**  
Opens the system dialog to uninstall or force stop apps.  
*Especially useful for phones with no recents menu.*

**Lock Settings**  
Lock the Settings menu with fingerprint or PIN to avoid accidental changes.

**Backup / Restore**
- **Backup:** Save current settings for future restoration (e.g., factory reset or new device).
- **Restore:** Load saved settings.  
*Note: Custom fonts are not backed up.*
- **Clear All Data:** Reset everything to default.

**Paged Scrolling**  
Improved paged scrolling for fewer mishaps of going up/down during swipes on touch devices.

**Dynamic & Legacy Icons**  
Improved icon support for dynamic and legacy icons.

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
App Drawer can now be added as an app in the app list.

