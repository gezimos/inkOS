package com.github.gezimos.inkos.helper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object IconPackUtility {

    private const val TAG = "IconPackUtility"

    fun getIconFromPack(
        context: Context,
        iconPackPackage: String,
        appPackage: String,
        activityClass: String
    ): Drawable? {
        if (iconPackPackage.isBlank() || appPackage.isBlank()) return null

        return try {
            val packResources = context.packageManager
                .getResourcesForApplication(iconPackPackage)
            val drawableName = findIconPackDrawableName(
                context, iconPackPackage, appPackage, activityClass
            ) ?: return null

            val resId = packResources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId != 0) {
                ResourcesCompat.getDrawable(packResources, resId, null)
            } else {
                Log.w(TAG, "Icon pack drawable '$drawableName' not found in $iconPackPackage")
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Icon pack not found: $iconPackPackage", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error loading icon from pack $iconPackPackage for $appPackage", e)
            null
        }
    }

    private fun findIconPackDrawableName(
        context: Context,
        iconPackPackage: String,
        appPackage: String,
        activityClass: String
    ): String? {
        try {
            val packResources = context.packageManager
                .getResourcesForApplication(iconPackPackage)
            val resId = packResources.getIdentifier("appfilter", "xml", iconPackPackage)

            if (resId != 0) {
                val parser = packResources.getXml(resId)
                return parseAppFilter(parser, appPackage, activityClass)
            }

            val assets = context.packageManager
                .getResourcesForApplication(iconPackPackage).assets
            assets.open("appfilter.xml").use { inputStream ->
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(inputStream, "UTF-8")
                return parseAppFilter(parser, appPackage, activityClass)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse appfilter.xml from $iconPackPackage", e)
            return null
        }
    }

    private fun parseAppFilter(
        parser: XmlPullParser,
        appPackage: String,
        activityClass: String
    ): String? {
        val componentFull = "ComponentInfo{$appPackage/$activityClass}"
        val componentShort = "ComponentInfo{$appPackage/"

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component") ?: ""
                val drawable = parser.getAttributeValue(null, "drawable") ?: ""

                if (drawable.isNotEmpty()) {
                    if (component == componentFull) {
                        return drawable
                    }
                    if (component.startsWith(componentShort)) {
                        return drawable
                    }
                }
            }
            eventType = parser.next()
        }
        return null
    }

    fun getInstalledIconPacks(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val packs = mutableMapOf<String, String>()

        val themeIntents = listOf(
            Intent("org.adw.launcher.THEMES"),
            Intent("com.gau.go.launcherex.theme"),
            Intent("com.novalauncher.THEME"),
            Intent("com.teslacoilsw.launcher.THEME"),
            Intent("com.fede.launcher.THEME_ICONPACK"),
            Intent("com.anddoes.launcher.THEME"),
            Intent("com.dlto.atom.launcher.THEME")
        )

        for (intent in themeIntents) {
            try {
                val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                for (resolveInfo in activities) {
                    val pkg = resolveInfo.activityInfo.packageName
                    if (pkg !in packs) {
                        val label = resolveInfo.loadLabel(pm).toString()
                        packs[pkg] = label
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying icon packs for ${intent.action}", e)
            }
        }

        return packs.map { (pkg, label) -> Pair(pkg, label) }
            .sortedBy { it.second.lowercase() }
    }
}
