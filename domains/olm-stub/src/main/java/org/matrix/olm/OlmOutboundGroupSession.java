package org.matrix.olm;

import java.io.Serializable;

public class OlmOutboundGroupSession implements Serializable {

    public OlmOutboundGroupSession() throws OlmException {
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

    public int messageIndex() {
        throw new RuntimeException("stub");
    }

    public String sessionKey() throws OlmException {
        throw new RuntimeException("stub");
    }

    public String encryptMessage(String aClearMsg) throws OlmException {
        throw new RuntimeException("stub");
    }

    protected byte[] serialize(byte[] aKey, StringBuffer aErrorMsg) {
        throw new RuntimeException("stub");
    }

    protected void deserialize(byte[] aSerializedData, byte[] aKey) throws Exception {
        throw new RuntimeException("stub");
    }

}
