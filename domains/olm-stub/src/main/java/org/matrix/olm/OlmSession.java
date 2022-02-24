package org.matrix.olm;

import java.io.Serializable;

public class OlmSession implements Serializable {

    public OlmSession() throws OlmException {
        throw new RuntimeException("stub");
    }

    long getOlmSessionId() {
        throw new RuntimeException("stub");
    }

    public void releaseSession() {
        throw new RuntimeException("stub");
    }

    public boolean isReleased() {
        throw new RuntimeException("stub");
    }

    public void initOutboundSession(OlmAccount aAccount, String aTheirIdentityKey, String aTheirOneTimeKey) throws OlmException {
        throw new RuntimeException("stub");
    }

    public void initInboundSession(OlmAccount aAccount, String aPreKeyMsg) throws OlmException {
        throw new RuntimeException("stub");
    }

    public void initInboundSessionFrom(OlmAccount aAccount, String aTheirIdentityKey, String aPreKeyMsg) throws OlmException {
        throw new RuntimeException("stub");
    }

    public String sessionIdentifier() throws OlmException {
        throw new RuntimeException("stub");
    }

    public boolean matchesInboundSession(String aOneTimeKeyMsg) {
        throw new RuntimeException("stub");
    }

    public boolean matchesInboundSessionFrom(String aTheirIdentityKey, String aOneTimeKeyMsg) {
        throw new RuntimeException("stub");
    }

    public OlmMessage encryptMessage(String aClearMsg) throws OlmException {
        throw new RuntimeException("stub");
    }

    public String decryptMessage(OlmMessage aEncryptedMsg) throws OlmException {
        throw new RuntimeException("stub");
    }

    protected byte[] serialize(byte[] aKey, StringBuffer aErrorMsg) {
        throw new RuntimeException("stub");

    }

    protected void deserialize(byte[] aSerializedData, byte[] aKey) throws Exception {
        throw new RuntimeException("stub");
    }
}
