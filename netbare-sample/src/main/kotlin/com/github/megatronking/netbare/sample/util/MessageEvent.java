package com.github.megatronking.netbare.sample.util;

public class MessageEvent {
    public static int UPDATEAPP_INSTALL = 1;
    public static int UPDATEAPP_REMOVE = 2;
    public static int UPLOAD_SUCCESS = 3;
    public static int UPLOAD_FAILED = 4;
    private String message;
    private int type;
    public MessageEvent(String message,int type) {
        this.message = message;
        this.type = type;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}

