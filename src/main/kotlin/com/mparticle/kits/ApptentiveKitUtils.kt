package com.mparticle.kits

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.feedback.ApptentiveActivityInfo
import com.mparticle.MParticle


object ApptentiveKitUtils {
    @JvmStatic
    fun registerApptentiveActivityContext(callback: ApptentiveActivityInfo) {
        if (MParticle.getInstance()?.isKitActive(MParticle.ServiceProviders.APPTENTIVE) == true) {
            Apptentive.registerApptentiveActivityInfoCallback(callback)
        } else {
            val filter =
                IntentFilter(MParticle.ServiceProviders.BROADCAST_ACTIVE + MParticle.ServiceProviders.APPTENTIVE);
            callback.getApptentiveActivityInfo().registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action?.startsWith(MParticle.ServiceProviders.BROADCAST_ACTIVE) == true) {
                        Apptentive.registerApptentiveActivityInfoCallback(callback)
                    }
                }
            }, filter)
        }
    }
}
