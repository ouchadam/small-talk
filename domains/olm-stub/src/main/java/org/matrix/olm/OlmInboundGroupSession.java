package org.matrix.olm;

import java.io.Serializable;

public class OlmInboundGroupSession implements Serializable {

    public OlmInboundGroupSession(String aSessionKey) throws OlmException {
        throw new RuntimeException("stub");
    }

    public static OlmInboundGroupSession importSession(String exported) throws OlmException {
        throw new RuntimeException("stub");
    }

    public void releaseSession() {
        throw new RuntimeException("stub");
    }

    public boolean isReleased() {
        throw new RuntimeException("stub");
    }

    public String sessionIdentifier() throws OlmException {
        throw new RuntimeException("stub");
    }

    public long getFirstKnownIndex() throws OlmException {
        throw new RuntimeException("stub");
    }

    public boolean isVerified() throws OlmException {
        throw new RuntimeException("stub");
    }

    public String export(long messageIndex) throws OlmException {
        throw new RuntimeException("stub");
    }

    public OlmInboundGroupSession.DecryptMessageResult decryptMessage(String aEncryptedMsg) throws OlmException {
        throw new RuntimeException("stub");
    }

    protected byte[] serialize(byte[] aKey, StringBuffer aErrorMsg) {
        throw new RuntimeException("stub");
    }

    protected void deserialize(byte[] aSerializedData, byte[] aKey) throws Exception {
        throw new RuntimeException("stub");
    }

    public static class DecryptMessageResult {
        public String mDecryptedMessage;
        public long mIndex;

        public DecryptMessageResult() {
            throw new RuntimeException("stub");
        }
    }
}
