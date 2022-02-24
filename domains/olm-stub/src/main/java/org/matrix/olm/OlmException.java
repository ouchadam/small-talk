package org.matrix.olm;

import java.io.IOException;

public class OlmException extends IOException {
    public static final int EXCEPTION_CODE_INIT_ACCOUNT_CREATION = 10;
    public static final int EXCEPTION_CODE_ACCOUNT_SERIALIZATION = 100;
    public static final int EXCEPTION_CODE_ACCOUNT_DESERIALIZATION = 101;
    public static final int EXCEPTION_CODE_ACCOUNT_IDENTITY_KEYS = 102;
    public static final int EXCEPTION_CODE_ACCOUNT_GENERATE_ONE_TIME_KEYS = 103;
    public static final int EXCEPTION_CODE_ACCOUNT_ONE_TIME_KEYS = 104;
    public static final int EXCEPTION_CODE_ACCOUNT_REMOVE_ONE_TIME_KEYS = 105;
    public static final int EXCEPTION_CODE_ACCOUNT_MARK_ONE_KEYS_AS_PUBLISHED = 106;
    public static final int EXCEPTION_CODE_ACCOUNT_SIGN_MESSAGE = 107;
    public static final int EXCEPTION_CODE_ACCOUNT_GENERATE_FALLBACK_KEY = 108;
    public static final int EXCEPTION_CODE_ACCOUNT_FALLBACK_KEY = 109;
    public static final int EXCEPTION_CODE_ACCOUNT_FORGET_FALLBACK_KEY = 110;
    public static final int EXCEPTION_CODE_CREATE_INBOUND_GROUP_SESSION = 200;
    public static final int EXCEPTION_CODE_INIT_INBOUND_GROUP_SESSION = 201;
    public static final int EXCEPTION_CODE_INBOUND_GROUP_SESSION_IDENTIFIER = 202;
    public static final int EXCEPTION_CODE_INBOUND_GROUP_SESSION_DECRYPT_SESSION = 203;
    public static final int EXCEPTION_CODE_INBOUND_GROUP_SESSION_FIRST_KNOWN_INDEX = 204;
    public static final int EXCEPTION_CODE_INBOUND_GROUP_SESSION_IS_VERIFIED = 205;
    public static final int EXCEPTION_CODE_INBOUND_GROUP_SESSION_EXPORT = 206;
    public static final int EXCEPTION_CODE_CREATE_OUTBOUND_GROUP_SESSION = 300;
    public static final int EXCEPTION_CODE_INIT_OUTBOUND_GROUP_SESSION = 301;
    public static final int EXCEPTION_CODE_OUTBOUND_GROUP_SESSION_IDENTIFIER = 302;
    public static final int EXCEPTION_CODE_OUTBOUND_GROUP_SESSION_KEY = 303;
    public static final int EXCEPTION_CODE_OUTBOUND_GROUP_ENCRYPT_MESSAGE = 304;
    public static final int EXCEPTION_CODE_INIT_SESSION_CREATION = 400;
    public static final int EXCEPTION_CODE_SESSION_INIT_OUTBOUND_SESSION = 401;
    public static final int EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION = 402;
    public static final int EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION_FROM = 403;
    public static final int EXCEPTION_CODE_SESSION_ENCRYPT_MESSAGE = 404;
    public static final int EXCEPTION_CODE_SESSION_DECRYPT_MESSAGE = 405;
    public static final int EXCEPTION_CODE_SESSION_SESSION_IDENTIFIER = 406;
    public static final int EXCEPTION_CODE_UTILITY_CREATION = 500;
    public static final int EXCEPTION_CODE_UTILITY_VERIFY_SIGNATURE = 501;
    public static final int EXCEPTION_CODE_PK_ENCRYPTION_CREATION = 600;
    public static final int EXCEPTION_CODE_PK_ENCRYPTION_SET_RECIPIENT_KEY = 601;
    public static final int EXCEPTION_CODE_PK_ENCRYPTION_ENCRYPT = 602;
    public static final int EXCEPTION_CODE_PK_DECRYPTION_CREATION = 700;
    public static final int EXCEPTION_CODE_PK_DECRYPTION_GENERATE_KEY = 701;
    public static final int EXCEPTION_CODE_PK_DECRYPTION_DECRYPT = 702;
    public static final int EXCEPTION_CODE_PK_DECRYPTION_SET_PRIVATE_KEY = 703;
    public static final int EXCEPTION_CODE_PK_DECRYPTION_PRIVATE_KEY = 704;
    public static final int EXCEPTION_CODE_PK_SIGNING_CREATION = 800;
    public static final int EXCEPTION_CODE_PK_SIGNING_GENERATE_SEED = 801;
    public static final int EXCEPTION_CODE_PK_SIGNING_INIT_WITH_SEED = 802;
    public static final int EXCEPTION_CODE_PK_SIGNING_SIGN = 803;
    public static final int EXCEPTION_CODE_SAS_CREATION = 900;
    public static final int EXCEPTION_CODE_SAS_ERROR = 901;
    public static final int EXCEPTION_CODE_SAS_MISSING_THEIR_PKEY = 902;
    public static final int EXCEPTION_CODE_SAS_GENERATE_SHORT_CODE = 903;
    public static final String EXCEPTION_MSG_INVALID_PARAMS_DESERIALIZATION = "invalid de-serialized parameters";
    private final int mCode;
    private final String mMessage;

    public OlmException(int aExceptionCode, String aExceptionMessage) {
        throw new RuntimeException("stub");
    }

    public int getExceptionCode() {
        throw new RuntimeException("stub");
    }

    public String getMessage() {
        throw new RuntimeException("stub");
    }
}
