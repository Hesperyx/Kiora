package com.tenpay.sdk.net.gateway;

/**
 * QQ 钱包网关 Servlet stub。
 *
 * 实际 QQ 中存在两个版本：
 *  - 新 API（versionCode >= 14498）：com.tenpay.sdk.net.gateway.QWalletGatewayServlet，
 *    sendRequest 有 5 参数与 6 参数两个重载，抢红包使用 6 参数版本：
 *    sendRequest(String service, String method, Req req, Rsp targetRsp, j config, n<Rsp> bizCallback)
 *  - 旧 API：com.tenpay.sdk.net.QWalletGatewayServlet
 *
 * 由于 config（R8 混淆为 ai2.j）与 callback（ai2.n）类型无法在编译期确定，
 * 这里仅提供 INSTANCE 静态字段占位，实际调用走运行时反射（按 name + paramCount 查找）。
 */
public final class QWalletGatewayServlet {

    public static final QWalletGatewayServlet INSTANCE = new QWalletGatewayServlet();

    private QWalletGatewayServlet() {
    }
}
