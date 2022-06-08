package app.dapk.st.matrix.common

enum class EventType(val value: String) {
    ROOM_MESSAGE("m.room.message"),
    ENCRYPTED("m.room.encrypted"),
    ENCRYPTION("m.room.encryption"),
    VERIFICATION_REQUEST("m.key.verification.request"),
    VERIFICATION_READY("m.key.verification.ready"),
    VERIFICATION_START("m.key.verification.start"),
    VERIFICATION_ACCEPT("m.key.verification.accept"),
    VERIFICATION_MAC("m.key.verification.mac"),
    VERIFICATION_KEY("m.key.verification.key"),
    VERIFICATION_DONE("m.key.verification.done"),
}

enum class MessageType(val value: String) {
    TEXT("m.text"),
    IMAGE("m.image"),
}