package com.tns.mes.common.exception;

public class BizException extends RuntimeException {
    private final int code;
    private final String messageKey;
    private final Object[] messageArgs;

    public BizException(int code, String messageKey, Object... messageArgs) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    public int getCode() { return code; }
    public String getMessageKey() { return messageKey; }
    public Object[] getMessageArgs() { return messageArgs; }
}

