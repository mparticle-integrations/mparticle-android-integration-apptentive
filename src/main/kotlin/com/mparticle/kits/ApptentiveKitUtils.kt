package com.mparticle.kits

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.feedback.ApptentiveActivityInfo
import com.mparticle.MParticle


object ApptentiveKitUtils {
    @JvmStatic
    fun registerApptentiveActivityContext(callback: ApptentiveActivityInfo) {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action?.startsWith(MParticle.ServiceProviders.BROADCAST_ACTIVE) == true) {
                    Apptentive.registerApptentiveActivityInfoCallback(callback)
                }
            }
        }
        if (MParticle.getInstance()?.isKitActive(MParticle.ServiceProviders.APPTENTIVE) == true) {
            Apptentive.registerApptentiveActivityInfoCallback(callback)
            Log.d("ApptentiveKitUtils", "registerApptentiveActivityContext: kit is active")
        } else {
            val filter = IntentFilter(MParticle.ServiceProviders.BROADCAST_ACTIVE + MParticle.ServiceProviders.APPTENTIVE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                callback.getApptentiveActivityInfo()
                    ?.registerReceiver(broadcastReceiver, filter, RECEIVER_EXPORTED)
                Log.d("ApptentiveKitUtils", "registerApptentiveActivityContext: kit is active SDK 33+")
            } else {
                callback.getApptentiveActivityInfo()
                    ?.registerReceiver(broadcastReceiver, filter)
                Log.d("ApptentiveKitUtils", "registerApptentiveActivityContext: kit is active SDK < 33")
            }
        }
    }
}
