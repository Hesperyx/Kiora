package com.tencent.mobileqq.mini.servlet;

import com.tencent.mobileqq.pb.MessageMicro;

/**
 * Stub for QQ's mini-program SSO helper (compileOnly).
 *
 * The callback interface is declared without nullability annotations on purpose:
 * Kotlin then treats the last parameter as a platform type and lets callers pass
 * null, which is what a fire-and-forget request does.
 */
public class MiniAppSSOCmdHelper {

    public interface MiniAppCmdCallback<RESPONSE extends MessageMicro<RESPONSE>> {
        void onReceived(boolean z, RESPONSE response);
    }

    private MiniAppSSOCmdHelper() {
    }

    public static <REQUEST extends MessageMicro<REQUEST>, RESPONSE extends MessageMicro<RESPONSE>> void sendSSOCmdRequest(String str, String str2, REQUEST request, Class<RESPONSE> cls, MiniAppCmdCallback<RESPONSE> miniAppCmdCallback) {
    }
}
