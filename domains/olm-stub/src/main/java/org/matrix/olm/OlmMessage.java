package org.matrix.olm;

public class OlmMessage {
    public static final int MESSAGE_TYPE_PRE_KEY = 0;
    public static final int MESSAGE_TYPE_MESSAGE = 1;
    public String mCipherText;
    public long mType;

    public OlmMessage() {
        throw new RuntimeException("stub");
    }
}
