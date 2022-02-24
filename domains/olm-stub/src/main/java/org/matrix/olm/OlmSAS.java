package org.matrix.olm;

public class OlmSAS {

    public OlmSAS() throws OlmException {
        throw new RuntimeException("stub");
    }

    public String getPublicKey() throws OlmException {
        throw new RuntimeException("stub");
    }

    public void setTheirPublicKey(String otherPkey) throws OlmException {
        throw new RuntimeException("stub");
    }

    public byte[] generateShortCode(String info, int byteNumber) throws OlmException {
        throw new RuntimeException("stub");
    }

    public String calculateMac(String message, String info) throws OlmException {
        throw new RuntimeException("stub");
    }

    public String calculateMacLongKdf(String message, String info) throws OlmException {
        throw new RuntimeException("stub");
    }

    public void releaseSas() {
        throw new RuntimeException("stub");
    }
}
