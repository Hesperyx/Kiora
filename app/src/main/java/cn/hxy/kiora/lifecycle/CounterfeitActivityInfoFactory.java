package cn.hxy.kiora.lifecycle;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Collections;
import java.util.List;

import cn.hxy.kiora.host.IHostAdapter;
import cn.hxy.kiora.utils.qq.HostInfo;

public class CounterfeitActivityInfoFactory {

    public static ActivityInfo makeProxyActivityInfo(String className, long flags) {
        try {
            Context ctx = HostInfo.INSTANCE.getHostContext();
            Class<?> cl = Class.forName(className);

            // 模板 Activity 由宿主适配器给出，不再写死 QQ 的两个类名 ——
            // 微信包内不存在 com.tencent.mobileqq.*，写死会让查询必定抛异常。
            IHostAdapter adapter = HostInfo.INSTANCE.getAdapter();
            List<String> candidates = adapter == null
                    ? Collections.<String>emptyList()
                    : adapter.getCounterfeitCandidates();

            PackageManager.NameNotFoundException last = null;
            for (String activityName : candidates) {
                try {

                    ActivityInfo proto = ctx.getPackageManager()
                            .getActivityInfo(new ComponentName(ctx.getPackageName(), activityName), (int) flags);

                    return initCommon(proto, className);
                } catch (PackageManager.NameNotFoundException e) {
                    last = e;
                }
            }
            throw new IllegalStateException(
                    "no counterfeit ActivityInfo template found in " + ctx.getPackageName()
                            + " (candidates=" + candidates + ")", last);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static ActivityInfo initCommon(ActivityInfo ai, String name) {
        ai.targetActivity = null;
        ai.taskAffinity = null;
        ai.descriptionRes = 0;
        ai.name = name;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ai.splitName = null;
        }
        ai.configChanges |= ActivityInfo.CONFIG_UI_MODE;
        return ai;
    }
}
