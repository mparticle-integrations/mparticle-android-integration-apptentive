package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.util.Log
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.feedback.ApptentiveActivityInfo
import apptentive.com.android.feedback.ApptentiveConfiguration
import apptentive.com.android.feedback.RegisterResult
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.kits.KitIntegration.AttributeListener
import java.util.*

class ApptentiveKit : KitIntegration(), KitIntegration.EventListener,
    AttributeListener  {
    private var enableTypeDetection = false
    private var lastKnownFirstName: String? = null
    private var lastKnownLastName: String? = null

    lateinit var apptentiveActivityInfo: ApptentiveActivityInfo


    override fun getName(): String = NAME
    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context
    ): List<ReportingMessage> {
        val apptentiveAppKey = settings[APPTENTIVE_APP_KEY]
        val apptentiveAppSignature = settings[APPTENTIVE_APP_SIGNATURE]
        require(!KitUtils.isEmpty(apptentiveAppKey)) { KEY_REQUIRED }
        require(!KitUtils.isEmpty(apptentiveAppSignature)) { SIGNATURE_REQUIRED }
        enableTypeDetection = StringUtils.tryParseSettingFlag(settings, ENABLE_TYPE_DETECTION, true)

        if (apptentiveAppKey != null && apptentiveAppSignature != null) {
            val configuration = ApptentiveConfiguration(apptentiveAppKey, apptentiveAppSignature)
            Apptentive.register(context.applicationContext as Application, configuration) { registerResult ->
                if (registerResult is RegisterResult.Success) {
                    Apptentive.setMParticleId(currentUser?.id.toString())
                }
            }

        }
        Apptentive.registerApptentiveActivityInfoCallback(apptentiveActivityInfo)
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun setUserIdentity(identityType: IdentityType, id: String) {
        if (identityType == IdentityType.Email) {
            Apptentive.setPersonEmail(id)
        } else if (identityType == IdentityType.CustomerId) {
            if (KitUtils.isEmpty(Apptentive.getPersonName())) {
                // Use id as customer name iff no full name is set yet.
                Apptentive.setPersonName(id)
            }
        }
    }

    override fun setUserAttribute(attributeKey: String, attributeValue: String) {
        if (attributeKey.equals(MParticle.UserAttributes.FIRSTNAME, true)) {
            lastKnownFirstName = attributeValue
        } else if (attributeKey.equals(MParticle.UserAttributes.LASTNAME, true)) {
            lastKnownLastName = attributeValue
        } else {
            addCustomPersonData(attributeKey, attributeValue)
            return
        }
        var fullName = ""
        if (!KitUtils.isEmpty(lastKnownFirstName)) {
            fullName += lastKnownFirstName
        }
        if (!KitUtils.isEmpty(lastKnownLastName)) {
            if (fullName.isNotEmpty()) {
                fullName += ""
            }
            fullName += lastKnownLastName
        }
        Apptentive.setPersonName(fullName.trim { it <= ' ' })
    }

    override fun setUserAttributeList(key: String, list: List<String>) {}
    override fun supportsAttributeLists(): Boolean = false

    override fun setAllUserAttributes(
        attributes: Map<String, String>,
        attributeLists: Map<String, List<String>>
    ) {
        var firstName = ""
        var lastName = ""
        for ((key, value) in attributes) {
            if (key.equals(MParticle.UserAttributes.FIRSTNAME, true)) {
                firstName = value
            } else if (key.equals(MParticle.UserAttributes.LASTNAME, true)) {
                lastName = value
            } else {
                addCustomPersonData(key, value)
            }
        }
        val fullName = if (!KitUtils.isEmpty(firstName) && !KitUtils.isEmpty(lastName)) {
            "$firstName $lastName"
        } else {
            firstName + lastName
        }
        Apptentive.setPersonName(fullName.trim { it <= ' ' })
    }

    override fun removeUserAttribute(key: String) {
        Apptentive.removeCustomPersonData(key)
    }

    override fun removeUserIdentity(identityType: IdentityType) {}
    override fun logout(): List<ReportingMessage> = emptyList()
    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()

    override fun logError(
        message: String,
        errorAttributes: Map<String, String>
    ): List<ReportingMessage> = emptyList()


    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String>,
        message: String
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        engage(context, event.eventName, event.customAttributeStrings)
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(ReportingMessage.fromEvent(this, event))
        return messageList
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>
    ): List<ReportingMessage> {
        engage(context, screenName, eventAttributes)
        val messages = LinkedList<ReportingMessage>()
        messages.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.SCREEN_VIEW,
                System.currentTimeMillis(),
                eventAttributes
            )
        )
        return messages
    }

    //region Helpers
    private fun engage(context: Context, event: String, customData: Map<String, String>?) {
        engage(event, customData)
    }

    private fun engage(
        event: String,
        customData: Map<String, String>?,
    ) {
        Apptentive.engage( event, parseCustomData(customData))
    }

    /* Apptentive SDK does not provide a function which accepts Object as custom data so we need to cast */
    private fun addCustomPersonData(key: String, value: String) {
        // original key
        Apptentive.addCustomPersonData(key, value)

        // typed key
        if (enableTypeDetection) {
            when (val typedValue = CustomDataParser.parseValue(value)) {
                is String -> {
                    // the value is already set
                }
                is Boolean -> {
                    Apptentive.addCustomPersonData(key + SUFFIX_KEY_FLAG, typedValue)
                }
                is Number -> {
                    Apptentive.addCustomPersonData(key + SUFFIX_KEY_NUMBER, typedValue)
                }
                else -> {
                    Log.e("mParticle-CustomData","Unexpected custom person data type:${typedValue?.javaClass}")
                }
            }
        }
    }

    private fun parseCustomData(map: Map<String, String>?): Map<String, Any>? {
        if (map != null) {
            val res: MutableMap<String, Any> = HashMap()
            for ((key, value) in map) {

                // original key
                res[key] = value

                // typed key
                if (enableTypeDetection) {
                    when (val typedValue = CustomDataParser.parseValue(value)) {
                        is String -> {
                            // the value is already set
                        }
                        is Boolean -> {
                            res[key + SUFFIX_KEY_FLAG] = typedValue
                        }
                        is Number -> {
                            res[key + SUFFIX_KEY_NUMBER] = typedValue
                        }
                        else -> {
                            Log.e("mParticle-CustomData","Unexpected custom data type:${typedValue?.javaClass}")
                        }
                    }
                }
            }
            return res
        }
        return null
    } //endregion

    companion object {
        private const val APPTENTIVE_APP_KEY = "apptentiveAppKey"
        private const val APPTENTIVE_APP_SIGNATURE = "apptentiveAppSignature"
        private const val ENABLE_TYPE_DETECTION = "enableTypeDetection"
        private const val SUFFIX_KEY_FLAG = "_flag"
        private const val SUFFIX_KEY_NUMBER = "_number"
        private const val NO_CURRENT_USER_LOG_MESSAGE =
            "Unable to update mParticle id: no current user"
        private const val KEY_REQUIRED =
            "Apptentive App Key is required. If you are migrating from a previous version, you may need to enter the new Apptentive App Key and Signature on the mParticle website."
        private const val SIGNATURE_REQUIRED =
            "Apptentive App Signature is required. If you are migrating from a previous version, you may need to enter the new Apptentive App Key and Signature on the mParticle website."
        const val NAME = "Apptentive"
    }
}
