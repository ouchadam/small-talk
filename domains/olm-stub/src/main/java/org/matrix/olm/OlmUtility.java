package org.matrix.olm;

import org.json.JSONObject;

import java.util.Map;

public class OlmUtility {
    public static final int RANDOM_KEY_SIZE = 32;

    public OlmUtility() throws OlmException {
        throw new RuntimeException("stub");
    }

    public void releaseUtility() {
        throw new RuntimeException("stub");
    }

    public void verifyEd25519Signature(String aSignature, String aFingerprintKey, String aMessage) throws OlmException {
        throw new RuntimeException("stub");
    }

    public String sha256(String aMessageToHash) {
        throw new RuntimeException("stub");
    }

    public static byte[] getRandomKey() {
        throw new RuntimeException("stub");
    }

    public boolean isReleased() {
        throw new RuntimeException("stub");
    }

    public static Map<String, String> toStringMap(JSONObject jsonObject) {
        throw new RuntimeException("stub");
    }

    public static Map<String, Map<String, String>> toStringMapMap(JSONObject jsonObject) {
        throw new RuntimeException("stub");
    }
}
