package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.util.Log
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.feedback.ApptentiveConfiguration
import apptentive.com.android.feedback.RegisterResult
import apptentive.com.android.util.InternalUseOnly
import apptentive.com.android.util.LogLevel
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.consent.ConsentState
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.UserAttributeListener
import java.util.*
import kotlin.collections.HashMap

class ApptentiveKit : KitIntegration(), KitIntegration.EventListener, IdentityListener,
    UserAttributeListener  {
    private var enableTypeDetection = true
    private var lastKnownFirstName: String? = null
    private var lastKnownLastName: String? = null

    //region KitIntegration
    override fun getName(): String = NAME
    @OptIn(InternalUseOnly::class)
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
            configuration.logLevel = LogLevel.Verbose
            configuration.shouldSanitizeLogMessages = false
            configuration.distributionVersion = getSDKVersion(context)
            configuration.distributionName = "mParticle"
            Apptentive.register(context.applicationContext as Application, configuration) { registerResult ->
                if (registerResult is RegisterResult.Success) {
                    Apptentive.setMParticleId(currentUser?.id.toString())
                }
            }
        }
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    override fun supportsAttributeLists(): Boolean = false
    override fun onConsentStateUpdated(
        oldState: ConsentState?,
        newState: ConsentState?,
        user: FilteredMParticleUser?
    ) {
        //Ignored
    }

    //endregion

    //region UserAttributeListener
    override fun onIncrementUserAttribute(
        key: String?,
        incrementedBy: Number?,
        value: String?,
        user: FilteredMParticleUser?
    ) {
       //Ignored
    }

    override fun onRemoveUserAttribute(key: String?, user: FilteredMParticleUser?) {
        key?.let {
            Apptentive.removeCustomPersonData(it)
        }
    }

    override fun onSetUserAttribute(key: String?, value: Any?, user: FilteredMParticleUser?) {
        if (key != null && value != null) {
            when (key.lowercase()){
                MParticle.UserAttributes.FIRSTNAME.lowercase() -> {
                    lastKnownFirstName = value.toString()
                }
                MParticle.UserAttributes.LASTNAME.lowercase() -> {
                    lastKnownLastName = value.toString()
                }
                else -> {
                    addCustomPersonData(key, value.toString())
                    return
                }
            }
            val fullName = listOfNotNull(lastKnownLastName, lastKnownFirstName).joinToString(separator = " ")
            if (fullName.isNotBlank()) Apptentive.setPersonName(fullName.trim())
        }
    }

    override fun onSetUserTag(key: String?, user: FilteredMParticleUser?) {
       //Ignored
    }

    override fun onSetUserAttributeList(
        attributeKey: String?,
        attributeValueList: MutableList<String>?,
        user: FilteredMParticleUser?
    ) {
        //Ignored
    }

    override fun onSetAllUserAttributes(
        userAttributes: MutableMap<String, String>?,
        userAttributeLists: MutableMap<String, MutableList<String>>?,
        user: FilteredMParticleUser?
    ) {
        userAttributes?.let { userAttribute ->
            val firstName = userAttribute[MParticle.UserAttributes.FIRSTNAME] ?: ""
            val lastName = userAttribute[MParticle.UserAttributes.LASTNAME] ?: ""
            val fullName = listOfNotNull(firstName, lastName).joinToString(separator = " ")
            if (fullName.isNotBlank()) Apptentive.setPersonName(fullName.trim())
            userAttribute.filterKeys { key ->
                key != MParticle.UserAttributes.FIRSTNAME && key != MParticle.UserAttributes.LASTNAME
            }.map {
                addCustomPersonData(it.key, it.value)
            }
        }
    }

    //endregion

    //region EventListener
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
        engage(event.eventName, event.customAttributeStrings)
        val messageList = LinkedList<ReportingMessage>()
        messageList.add(ReportingMessage.fromEvent(this, event))
        return messageList
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>
    ): List<ReportingMessage> {
        engage(screenName, eventAttributes)
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
    } //endregion

    //region IdentityListener
    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser?,
        identityApiRequest: FilteredIdentityApiRequest?
    ) {
        setUserIdentity(mParticleUser)
    }

    override fun onUserIdentified(mParticleUser: MParticleUser?) {
        Apptentive.setMParticleId(mParticleUser?.id.toString())
    }

    //endregion

    //region Helpers
    private fun setUserIdentity(user: MParticleUser?) {
        user?.userIdentities?.entries?.forEach {
            when (it.key) {
                IdentityType.CustomerId ->  {
                    if (KitUtils.isEmpty(Apptentive.getPersonName())) {
                        // Use id as customer name iff no full name is set yet.
                        Apptentive.setPersonName(it.value)
                    }
                }
                IdentityType.Email -> Apptentive.setPersonEmail(it.value)
                else -> Log.d("UserIdentity", "Other type")
            }
        }
    }

    private fun engage(event: String, customData: Map<String, String>?) {
        Apptentive.engage(event, parseCustomData(customData))
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
    }

    private fun getSDKVersion(context: Context): String {
        val packageManager = context.packageManager
        return try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode.toString()
        } catch (exception: NameNotFoundException) {
            Log.e("mParticle", "There is a issue in getting the current SDK version from the PackageManager")
            "1"
        }
    }
    //endregion

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
