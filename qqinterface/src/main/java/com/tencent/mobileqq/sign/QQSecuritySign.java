package com.tencent.mobileqq.sign;

import com.tencent.mobileqq.fe.EventCallback;

/** 编译期 stub，运行时使用宿主真实类。 */
public class QQSecuritySign {

    public static class SignResult {
        public byte[] extra;
        public byte[] sign;
        public byte[] token;
    }

    public void dispatchEvent(String str, String str2, EventCallback eventCallback) {
        throw new RuntimeException("Stub!");
    }

    public void dispatchEventPB(String str, String str2, byte[] bArr, EventCallback eventCallback) {
        throw new RuntimeException("Stub!");
    }
}
