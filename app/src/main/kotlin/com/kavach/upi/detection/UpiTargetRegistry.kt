package com.kavach.upi.detection

object UpiTargetRegistry {

    private val upiPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                       // Paytm
        "in.org.npci.upiapp",                    // BHIM
        "in.amazon.mShop.android.shopping",      // Amazon Pay (mShop containing pay)
        "com.dreamplug.androidapp",              // CRED
        "com.whatsapp"                           // WhatsApp (for UPI integrations)
    )

    private val bankPackagePrefixes = setOf(
        "com.icicibank.",
        "com.hdfcbank.",
        "com.sbi.",
        "com.axisbank."
    )

    /**
     * Checks if the given package name belongs to a known UPI or bank application.
     */
    fun isUpiTarget(packageName: String?): Boolean {
        if (packageName == null) return false
        val normalized = packageName.lowercase().trim()

        if (upiPackages.contains(normalized)) {
            return true
        }

        return bankPackagePrefixes.any { prefix -> normalized.startsWith(prefix) }
    }
}
