package com.example.smsfirewall

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.annotation.RequiresApi
import com.example.smsfirewall.filter.FilterKeywordStore
import com.example.smsfirewall.util.normalizeSender
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class SmsFirewallCallScreeningService : CallScreeningService() {

    @Inject lateinit var filterKeywordStore: FilterKeywordStore

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart.orEmpty()

        if (number.isBlank()) {
            respondAllow(callDetails)
            return
        }

        val blockedSenders = filterKeywordStore.getBlockedSenders()
        val normalizedNumber = normalizeSender(number)
        val isBlocked = blockedSenders.any { normalizeSender(it) == normalizedNumber }

        if (isBlocked) {
            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(true)
                    .setSkipNotification(true)
                    .build()
            )
        } else {
            respondAllow(callDetails)
        }
    }

    private fun respondAllow(callDetails: Call.Details) {
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        )
    }
}
