package com.example.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolExecutionEngine(private val context: Context) {

    data class ToolResult(
        val success: Boolean,
        val outputMessage: String,
        val sassyComment: String,
        val requiresPermissionPrompt: Boolean = false,
        val missingPermission: String? = null
    ) {
        fun toJsonObject(): JSONObject {
            val obj = JSONObject()
            val output = JSONObject()
            output.put("success", success)
            output.put("message", outputMessage)
            output.put("sassyComment", sassyComment)
            if (missingPermission != null) {
                output.put("missingPermission", missingPermission)
            }
            obj.put("output", output)
            return obj
        }
    }

    fun execute(toolName: String, args: Map<String, Any?>): ToolResult {
        return try {
            when (toolName) {
                "openApp" -> {
                    val appName = args["appName"]?.toString() ?: ""
                    val packageName = args["packageName"]?.toString()
                    openApp(appName, packageName)
                }
                "searchAndCallContact" -> {
                    val contactName = args["contactName"]?.toString() ?: ""
                    searchAndCallContact(contactName)
                }
                "sendWhatsAppMessage" -> {
                    val contactName = args["contactName"]?.toString() ?: ""
                    val message = args["message"]?.toString() ?: ""
                    sendWhatsAppMessage(contactName, message)
                }
                "sendGmail" -> {
                    val recipient = args["recipientEmail"]?.toString() ?: ""
                    val subject = args["subject"]?.toString() ?: ""
                    val body = args["body"]?.toString() ?: ""
                    sendGmail(recipient, subject, body)
                }
                "getDeviceInfo" -> {
                    getDeviceInfo()
                }
                else -> {
                    ToolResult(
                        success = false,
                        outputMessage = "Unknown tool: $toolName",
                        sassyComment = "Darling, I don't know that trick yet, but I bet I'd look fabulous doing it."
                    )
                }
            }
        } catch (e: Exception) {
            ToolResult(
                success = false,
                outputMessage = "Error: ${e.localizedMessage}",
                sassyComment = "Oops! My charm broke the gadget for a second: ${e.localizedMessage}"
            )
        }
    }

    private fun openApp(appName: String, packageNameHint: String?): ToolResult {
        val pm = context.packageManager

        // Known package mapping for convenience
        val appMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "camera" to "com.android.camera",
            "calculator" to "com.google.android.calculator",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "photos" to "com.google.android.apps.photos",
            "settings" to "com.android.settings",
            "clock" to "com.google.android.deskclock"
        )

        val targetPkg = packageNameHint?.takeIf { it.isNotBlank() }
            ?: appMap[appName.lowercase(Locale.ROOT).trim()]
            ?: findPackageByName(appName)

        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolResult(
                    success = true,
                    outputMessage = "Opened $appName ($targetPkg) successfully.",
                    sassyComment = "Boom! Opened $appName for you. What would you do without me?"
                )
            }
        }

        // Special fallback for camera/calculator intents
        val lower = appName.lowercase(Locale.ROOT)
        if (lower.contains("camera")) {
            val camIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (camIntent.resolveActivity(pm) != null) {
                context.startActivity(camIntent)
                return ToolResult(
                    success = true,
                    outputMessage = "Opened Camera.",
                    sassyComment = "Smile gorgeous! Camera's open."
                )
            }
        }

        return ToolResult(
            success = false,
            outputMessage = "Could not find application '$appName' installed on this device.",
            sassyComment = "Honey, you don't even have '$appName' installed! Want me to pull it up on Google Play?"
        )
    }

    private fun findPackageByName(query: String): String? {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val normalized = query.lowercase(Locale.ROOT).trim()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue
            val appLabel = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.ROOT)
            if (appLabel.contains(normalized) || pkg.packageName.lowercase(Locale.ROOT).contains(normalized)) {
                return pkg.packageName
            }
        }
        return null
    }

    private fun searchAndCallContact(contactName: String): ToolResult {
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPerm) {
            return ToolResult(
                success = false,
                outputMessage = "READ_CONTACTS permission is missing.",
                sassyComment = "Sweetheart, I'd love to call $contactName for you, but you haven't given me permission to look at your contacts yet! Enable it, please?",
                requiresPermissionPrompt = true,
                missingPermission = Manifest.permission.READ_CONTACTS
            )
        }

        val phone = findContactPhoneNumber(contactName)
        if (phone.isNullOrBlank()) {
            return ToolResult(
                success = false,
                outputMessage = "Contact '$contactName' was not found in your phone book.",
                sassyComment = "I checked your little black book, darling, and '$contactName' isn't in there! Double-check the name for me?"
            )
        }

        if (hasCallPerm) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            return ToolResult(
                success = true,
                outputMessage = "Calling $contactName at $phone.",
                sassyComment = "Dialing $contactName right now. Tell them SAQI said hello!"
            )
        } else {
            // Graceful fallback to dialer
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ToolResult(
                success = true,
                outputMessage = "Opened dialer for $contactName ($phone) because direct CALL_PHONE is not yet approved.",
                sassyComment = "I pulled up $contactName's number ($phone) on your dialer! Grant me CALL_PHONE permission next time if you want me to dial directly without you lifting a finger."
            )
        }
    }

    private fun findContactPhoneNumber(contactName: String): String? {
        val contentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$contactName%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numIdx != -1) {
                    return it.getString(numIdx)
                }
            }
        }
        return null
    }

    private fun sendWhatsAppMessage(contactName: String, message: String): ToolResult {
        // Try looking up the phone number if contactName is a person's name
        var phoneNumber: String? = null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            phoneNumber = findContactPhoneNumber(contactName)
        }

        // Clean phone number (strip spaces, dashes)
        val cleanPhone = phoneNumber?.replace("[^0-9+]".toRegex(), "")?.removePrefix("+")

        val encodedMsg = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        // WhatsApp deep link uri
        val uri = if (!cleanPhone.isNullOrBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
        } else {
            Uri.parse("whatsapp://send?text=$encodedMsg")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        return if (intent.resolveActivity(pm) != null) {
            context.startActivity(intent)
            ToolResult(
                success = true,
                outputMessage = "WhatsApp opened with pre-filled message for $contactName.",
                sassyComment = "Message prepped for $contactName on WhatsApp! You're welcome, darling."
            )
        } else {
            // Fallback generic send intent
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (genericIntent.resolveActivity(pm) != null) {
                context.startActivity(Intent.createChooser(genericIntent, "Send via").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                ToolResult(
                    success = true,
                    outputMessage = "WhatsApp not detected; opened system share chooser.",
                    sassyComment = "WhatsApp wasn't on the phone, but I brought up your share sheet with the message ready!"
                )
            } else {
                ToolResult(
                    success = false,
                    outputMessage = "WhatsApp is not installed.",
                    sassyComment = "Honey, you don't even have WhatsApp installed! Want me to grab it from the Play Store?"
                )
            }
        }
    }

    private fun sendGmail(recipientEmail: String, subject: String, body: String): ToolResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipientEmail")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        // Prefer Gmail if installed
        val gmailPackage = "com.google.android.gm"
        val isGmailInstalled = try {
            pm.getPackageInfo(gmailPackage, 0)
            true
        } catch (e: Exception) {
            false
        }

        if (isGmailInstalled) {
            intent.setPackage(gmailPackage)
        }

        return if (intent.resolveActivity(pm) != null) {
            context.startActivity(intent)
            ToolResult(
                success = true,
                outputMessage = "Email composed to $recipientEmail with subject '$subject'.",
                sassyComment = "Voila! Email to $recipientEmail drafted and ready to fly. Look at us being productive!"
            )
        } else {
            ToolResult(
                success = false,
                outputMessage = "No email application found on this device.",
                sassyComment = "Oops, I can't find an email client configured! Even my magic has limits, babe."
            )
        }
    }

    private fun getDeviceInfo(): ToolResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            bm?.isCharging == true
        } else {
            false
        }

        val timeFormat = SimpleDateFormat("h:mm a, EEEE, MMMM d", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val statusStr = "Battery: $batteryPct% (Charging: $isCharging). Current Time: $currentTime."
        val sassyComment = when {
            batteryPct in 0..15 -> "Battery's down to $batteryPct%! Plug me in, sweetie, before I pass out!"
            isCharging -> "Battery is at $batteryPct% and soaking up that sweet juice. It's $currentTime."
            else -> "It's $currentTime, and your battery is chilling at $batteryPct%. We've got plenty of stamina!"
        }

        return ToolResult(
            success = true,
            outputMessage = statusStr,
            sassyComment = sassyComment
        )
    }
}
