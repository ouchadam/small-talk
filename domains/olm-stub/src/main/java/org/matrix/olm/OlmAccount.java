package org.matrix.olm;

import java.io.Serializable;
import java.util.Map;

public class OlmAccount implements Serializable {
    public static final String JSON_KEY_ONE_TIME_KEY = "curve25519";
    public static final String JSON_KEY_IDENTITY_KEY = "curve25519";
    public static final String JSON_KEY_FINGER_PRINT_KEY = "ed25519";

    public OlmAccount() throws OlmException {
        throw new RuntimeException("stub");
    }

    long getOlmAccountId() {
        throw new RuntimeException("stub");
    }

    public void releaseAccount() {
        throw new RuntimeException("stub");
    }

    public boolean isReleased() {
        throw new RuntimeException("stub");
    }

    public Map<String, String> identityKeys() throws OlmException {
        throw new RuntimeException("stub");
    }

    public long maxOneTimeKeys() {
        throw new RuntimeException("stub");

    }

    public void generateOneTimeKeys(int aNumberOfKeys) throws OlmException {
        throw new RuntimeException("stub");
    }

    public Map<String, Map<String, String>> oneTimeKeys() throws OlmException {
        throw new RuntimeException("stub");
    }

    public void removeOneTimeKeys(OlmSession aSession) throws OlmException {
        throw new RuntimeException("stub");
    }

    public void markOneTimeKeysAsPublished() throws OlmException {
        throw new RuntimeException("stub");
    }

    public String signMessage(String aMessage) throws OlmException {
        throw new RuntimeException("stub");
    }

    protected byte[] serialize(byte[] aKey, StringBuffer aErrorMsg) {
        throw new RuntimeException("stub");
    }

    protected void deserialize(byte[] aSerializedData, byte[] aKey) throws Exception {
        throw new RuntimeException("stub");
    }

    public byte[] pickle(byte[] aKey, StringBuffer aErrorMsg) {
        throw new RuntimeException("stub");

    }

    public void unpickle(byte[] aSerializedData, byte[] aKey) throws Exception {
        throw new RuntimeException("stub");
    }

    public void generateFallbackKey() throws OlmException {
        throw new RuntimeException("stub");
    }

    public Map<String, Map<String, String>> fallbackKey() throws OlmException {
        throw new RuntimeException("stub");
    }

    public void forgetFallbackKey() throws OlmException {
        throw new RuntimeException("stub");
    }

}
