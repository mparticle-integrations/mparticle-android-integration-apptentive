package com.mparticle.kits

import android.app.Application
import android.content.Context
import com.apptentive.android.sdk.Apptentive
import com.apptentive.android.sdk.ApptentiveConfiguration
import com.apptentive.android.sdk.ApptentiveLog
import com.apptentive.android.sdk.ApptentiveNotifications
import com.apptentive.android.sdk.conversation.Conversation
import com.apptentive.android.sdk.model.CommerceExtendedData
import com.apptentive.android.sdk.model.ExtendedData
import com.apptentive.android.sdk.notifications.ApptentiveNotification
import com.apptentive.android.sdk.notifications.ApptentiveNotificationCenter
import com.apptentive.android.sdk.notifications.ApptentiveNotificationObserver
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.kits.KitIntegration.AttributeListener
import com.mparticle.kits.KitIntegration.CommerceListener
import org.json.JSONException
import java.math.BigDecimal
import java.util.*

class ApptentiveKit : KitIntegration(), KitIntegration.EventListener, CommerceListener,
    AttributeListener, ApptentiveNotificationObserver {
    private var enableTypeDetection = false
    private var lastKnownFirstName: String? = null
    private var lastKnownLastName: String? = null
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
            Apptentive.register(context.applicationContext as Application, configuration)
            ApptentiveNotificationCenter.defaultCenter()
                .addObserver(
                    ApptentiveNotifications.NOTIFICATION_CONVERSATION_STATE_DID_CHANGE,
                    this
                )
        }
        return emptyList()
    }

    override fun onKitDestroy() {
        super.onKitDestroy()
        ApptentiveNotificationCenter.defaultCenter().removeObserver(this)
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

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        if (!KitUtils.isEmpty(event.productAction)) {
            try {
                val eventActionAttributes = HashMap<String, String>()
                CommerceEventUtils.extractActionAttributes(event, eventActionAttributes)
                var apptentiveCommerceData: CommerceExtendedData? = null
                val transactionAttributes = event.transactionAttributes
                if (transactionAttributes != null) {

                    apptentiveCommerceData = setTransactionAttributes(
                        transactionAttributes,
                        eventActionAttributes,
                        event
                    )
                }
                if (apptentiveCommerceData != null) {
                    engage(
                        context, String.format("eCommerce - %s", event.productAction),
                        event.customAttributeStrings,
                        apptentiveCommerceData
                    )
                    val messages = LinkedList<ReportingMessage>()
                    messages.add(ReportingMessage.fromEvent(this, event))
                    return messages
                }
            } catch (jse: JSONException) {
            }
        }
        return emptyList()
    }

    private fun setTransactionAttributes(
        transactionAttributes: TransactionAttributes,
        eventActionAttributes: HashMap<String, String>,
        event: CommerceEvent
    ): CommerceExtendedData {
        val apptentiveCommerceData = CommerceExtendedData()
        val transactionId = transactionAttributes.id
        if (!KitUtils.isEmpty(transactionId)) {
            apptentiveCommerceData.setId(transactionId)
        }
        val transRevenue = transactionAttributes.revenue
        if (transRevenue != null) {
            apptentiveCommerceData.setRevenue(transRevenue)
        }
        val transShipping = transactionAttributes.shipping
        if (transShipping != null) {
            apptentiveCommerceData.setShipping(transShipping)
        }
        val transTax = transactionAttributes.tax
        if (transTax != null) {
            apptentiveCommerceData.setTax(transTax)
        }
        val transAffiliation = transactionAttributes.affiliation
        if (!KitUtils.isEmpty(transAffiliation)) {
            apptentiveCommerceData.setAffiliation(transAffiliation)
        }
        var transCurrency =
            eventActionAttributes[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE]
        if (KitUtils.isEmpty(transCurrency)) {
            transCurrency = CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE
        }
        apptentiveCommerceData.setCurrency(transCurrency)

        // Add each item
        val productList = event.products
        if (productList != null) {
            for (product in productList) {
                val item = CommerceExtendedData.Item()
                item.setId(product.sku)
                item.setName(product.name)
                item.setCategory(product.category)
                item.setPrice(product.unitPrice)
                item.setQuantity(product.quantity)
                item.setCurrency(transCurrency)
                apptentiveCommerceData.addItem(item)
            }
        }
        return apptentiveCommerceData
    }

    //region Notifications
    override fun onReceiveNotification(notification: ApptentiveNotification) {
        if (notification.hasName(ApptentiveNotifications.NOTIFICATION_CONVERSATION_STATE_DID_CHANGE)) {
            val conversation = notification.getRequiredUserInfo(
                ApptentiveNotifications.NOTIFICATION_KEY_CONVERSATION,
                Conversation::class.java
            )
            if (conversation != null && conversation.hasActiveState()) {
                val currentUser = currentUser
                if (currentUser == null) {
                    ApptentiveLog.w(NO_CURRENT_USER_LOG_MESSAGE)
                    return
                }
                val userId = currentUser.id.toString()
                ApptentiveLog.v("Updating mParticle id: %s", ApptentiveLog.hideIfSanitized(userId))
                conversation.person.mParticleId = userId
            }
        }
    }

    //endregion
    //region Helpers
    private fun engage(context: Context, event: String, customData: Map<String, String>?) {
        engage(context, event, customData, *arrayOf())
    }

    private fun engage(
        context: Context,
        event: String,
        customData: Map<String, String>?,
        vararg extendedData: ExtendedData
    ) {
        Apptentive.engage(context, event, parseCustomData(customData), *extendedData)
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
                    ApptentiveLog.e("Unexpected custom person data type: %s", typedValue?.javaClass)
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
                            ApptentiveLog.e(
                                "Unexpected custom data type: %s",
                                typedValue?.javaClass
                            )
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
