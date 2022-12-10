package app.dapk.st.impl

import app.dapk.st.engine.DeviceDisplayNameGenerator

internal class SmallTalkDeviceNameGenerator : DeviceDisplayNameGenerator {
    override fun generate(): String {
        val randomIdentifier = (('A'..'Z') + ('a'..'z') + ('0'..'9')).shuffled().take(4).joinToString("")
        return "SmallTalk Android ($randomIdentifier)"
    }
}