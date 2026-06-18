package com.kavach.upi.detection

object ThreatSignatureStore {

    // A list of common package prefixes/names for screen-sharing and remote access tools
    private val threatPackages = setOf(
        "com.anydesk.anydeskandroid",
        "com.teamviewer.host.market",
        "com.teamviewer.quicksupport.market",
        "com.splashtop.personal",
        "com.splashtop.remote",
        "com.logmein.joinme",
        "org.videolan.vlc", // Example for testing, or we can use generic ones
        "com.facebook.katana", // Example, check if we want to monitor typical candidates
        "com.duapps.recorder",
        "com.hecorat.xrecorder",
        "com.rsupport.mvagent"
    )

    /**
     * Checks if a given package name corresponds to a known threat tool.
     */
    fun isThreatPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        val normalized = packageName.lowercase().trim()
        return threatPackages.any { threat -> normalized.contains(threat) }
    }

    /**
     * Exposes the raw list of target threat packages.
     */
    fun getThreatPackages(): Set<String> {
        return threatPackages
    }
}
