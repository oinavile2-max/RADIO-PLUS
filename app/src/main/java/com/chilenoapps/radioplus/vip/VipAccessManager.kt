package com.chilenoapps.radioplus.vip

import com.chilenoapps.radioplus.BuildConfig

class VipAccessManager {
    val access: VipAccess
        get() = if (BuildConfig.ADMIN_MODE) {
            VipAccess.AdminTest
        } else {
            VipAccess.NotEntitled
        }
}

sealed class VipAccess {
    object AdminTest : VipAccess()
    object NotEntitled : VipAccess()
    data class Verified(val validUntilEpochSeconds: Long) : VipAccess()

    val isVip: Boolean get() = this is AdminTest || this is Verified
}
