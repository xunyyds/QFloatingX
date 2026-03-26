/*
 * Copyright (C) 2025 ᗜ×ᗜ
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * [原作者信息]
 * Author: ᗜ×ᗜ
 * Source: https://github.com/xunyyds/QFloatingX
 * 以上内容必须完整注明在你的项目中
 * 在任何基于本项目或其修改版的公开版本中，请保留原项目的版权声明和开源许可证，并在显著位置注明：
 * 原项目：QFloatingX
 * 作者：ᗜ×ᗜ
 * 项目地址：https://github.com/xunyyds/QFloatingX
 * 开源许可证：Apache-2.0
*/

//有bug或者建议可以大胆向我反馈

// 图标路径
String iconBase        = pluginPath + "/API/icon";
String iconPath        = new java.io.File(iconBase + ".png").exists() ? iconBase + ".png" :
                        new java.io.File(iconBase + ".gif").exists() ? iconBase + ".gif" :
                        iconBase + ".png";
String closeIconPath   = pluginPath + "/API/closeIcon.png";
String settingiconPath = pluginPath + "/API/settingicon.png";

// 不要改！不要改！不要改！
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.SystemClock;

String rootPath        = pluginPath + "/API/";
String htmlPath        = pluginPath + "/HTML/";
String configPath      = pluginPath + "/config/";
String qq              = myUin;
final String logPath   = pluginPath + "/Log/";
long startTime         = System.currentTimeMillis();
final String QQpackage = "com.tencent.mobileqq";
ExecutorService ThreadPool = null;

// 核心api（顺序加载）  非核心api（并行加载）
final String[] coreFiles = {
    rootPath + "import.java",   // 导类
    rootPath + "api.java",      // 基础
    rootPath + "uitools.java"   // UI工具
};
final String[] parallelFiles = {
    rootPath + "ColorPicker.java",// 调色盘
    rootPath + "KeepAlive.java", // 后台保活
    rootPath + "setwindow.java",// 设置弹窗
    rootPath + "Dialog.java",    //  弹窗
    rootPath + "api2.java",     // 模拟定位
    rootPath + "api3.java",     // 消息统计
    rootPath + "api4.java",     // 悬浮窗
    rootPath + "api5.java",     // 双击消息
    rootPath + "api6.java",     // 运行状态
    rootPath + "api7.java",     // html
    rootPath + "api8.java",     // QQ空间
    rootPath + "api9.java",     // pb
    rootPath + "function.java"  // 功能(动态热插拔)
};

// API加载耗时，-1表示未初始化
volatile long apiLoadCostTime = -1;
private static ThreadLocal dateFormatHolder = new ThreadLocal();

public static String getTime() {
    try {
        SimpleDateFormat df = (SimpleDateFormat) dateFormatHolder.get();
        if (df == null) {
            df = new SimpleDateFormat("HH:mm:ss");
            dateFormatHolder.set(df);
        }
        return df.format(new Date());
    } catch (Exception e) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            return df.format(new Date());
        } catch (Exception ex) {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            return (hour < 10 ? "0" + hour : hour) + ":" +
                   (minute < 10 ? "0" + minute : minute) + ":" +
                   (second < 10 ? "0" + second : second);
        }
    }
}

/**
 * 日志记录方法
 */
void traceLog(String name, String txt) {
    String 文件名 = name;
    try {
        if (!name.endsWith(".txt")) {
            文件名 = name + ".txt";
        }
        log("/Log/" + 文件名, getTime() + "    " + txt);
    } catch (Exception e) {
        log("/Log/" + 文件名, "    " + txt);
    }
}

void traceLog(String txt) {
    try {
        log("/Log/api_log.txt", getTime() + "    " + txt);
    } catch (Exception e) {}
}

/** 初始化/重建线程池 */
void initThreadPool() {
    if (ThreadPool != null) {
        try {
            ThreadPool.submit(new Runnable() { public void run() {} }).get(100, TimeUnit.MILLISECONDS);
            traceLog("main_log", "线程池存活，直接使用");
            return;
        } catch (Exception e) {
            traceLog("main_log", "线程池重建: " + e.getMessage());
            ThreadPool = null;
        }
    }

    int threadPriority = 5;
    try {
        String sp = getString("settings", "thread_pool_priority", "");
        if (!sp.isEmpty()) threadPriority = Math.max(1, Math.min(10, Integer.parseInt(sp)));
    } catch (Exception e) {}

    int queueCapacity = 50;
    try {
        String sp = getString("settings", "thread_pool_queue_capacity", "");
        if (!sp.isEmpty()) queueCapacity = Math.max(10, Integer.parseInt(sp));
    } catch (Exception e) {}

    long keepAliveTime = 30;
    try {
        String sp = getString("settings", "thread_pool_keep_alive", "");
        if (!sp.isEmpty()) keepAliveTime = Math.max(5, Long.parseLong(sp));
    } catch (Exception e) {}

    String threadNamePrefix = getString("settings", "thread_pool_name_prefix", "ovoWorker");

    int cpuCores = Runtime.getRuntime().availableProcessors();
    int corePoolSize = Math.max(2, Math.min(cpuCores, 8));
    int maxPoolSize = Math.min(corePoolSize * 2, 16);

    traceLog("main_log", "初始化线程池: 核心=" + corePoolSize);

    ThreadFactory threadFactory = new ThreadFactory() {
        private int threadCount = 1;
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(threadNamePrefix + "-" + threadCount++);
            t.setDaemon(true);
            t.setPriority(threadPriority);
            return t;
        }
    };

    ThreadPool = new ThreadPoolExecutor(
        corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
        new LinkedBlockingQueue(queueCapacity), threadFactory,
        new ThreadPoolExecutor.DiscardOldestPolicy()
    );
}

initThreadPool();

ThreadPool.execute(new Runnable() {
    public void run() {
        try {
            for (int i = 0; i < coreFiles.length; i++) {
                loadJava(coreFiles[i]);
                traceLog("main_log", "核心api加载：" + coreFiles[i]);
            }

            final CountDownLatch latch = new CountDownLatch(parallelFiles.length);
            for (int i = 0; i < parallelFiles.length; i++) {
                final String currentFile = parallelFiles[i];
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            loadJava(currentFile);
                        } catch (Exception e) {
                            traceLog("main_log", "非核心API加载失败：" + currentFile + " " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }

            latch.await(10000, TimeUnit.MILLISECONDS);
            apiLoadCostTime = System.currentTimeMillis() - startTime;
            traceLog("main_log", "所有API加载完成，耗时：" + apiLoadCostTime + "ms");

            uiHandler.post(new Runnable() {
                public void run() {
                    原神启动();
                }
            });

        } catch (Exception e) {
            traceLog("main_log", "API加载异常：" + e.getMessage());
        }
    }
});

// 全局变量
String currentPackageName = context.getPackageName();
String applicationType = "";

// 状态变量
volatile boolean 悬浮窗显示状态 = false;
volatile boolean 应用前台状态 = false;
volatile boolean 非UI初始化完成 = false;
volatile boolean UI初始化完成 = false;
volatile boolean 允许触摸 = true;

// [精准前后台检测] Activity计数器
private static volatile int resumedActivityCount = 0;

// Handlers
Handler uiHandler = new Handler(Looper.getMainLooper());
Handler backgroundHandler = new Handler(Looper.getMainLooper());

Activity 最后Activity = null;
AtomicBoolean isRunning = new AtomicBoolean(false);
Context appContext = null;
boolean dialogVisible = false;
Activity activity = null;
volatile boolean Hook已调用 = false;

List hookloveList = new ArrayList();


void 卸载loveHook() {
    if (hookloveList.isEmpty()) return;
    for (int i = 0; i < hookloveList.size(); i++) {
        try {
            Object unhook = hookloveList.get(i);
            Method unhookMethod = unhook.getClass().getMethod("unhook");
            unhookMethod.invoke(unhook);
        } catch (Exception e) {}
    }
    hookloveList.clear();
}

/**
 * 检查并更新前台状态。
 * <p>
 * 当 {@code resumedActivityCount} 大于 0 时表示有 Activity 可见；
 * 若后台初始化已完成但 UI 尚未初始化，则在主线程补做前台初始化；
 * 并在状态从后台切换到前台时停止保活服务、重启悬浮窗。
 * </p>
 *
 * @param activity 当前可见的 {@link Activity}
 */
private void checkAndUpdateForegroundState(final Activity activity) {
    if (resumedActivityCount <= 0) return;

    if (非UI初始化完成 && !UI初始化完成) {
        uiHandler.post(new Runnable() {
            public void run() {
                前台初始化(activity);
            }
        });
    }

    if (!应用前台状态) {
        traceLog("state_log", "状态变更 → 前台");
        应用前台状态 = true;
        允许触摸 = true;
        最后Activity = activity;

        stopKeepAlive();

        if (UI初始化完成 && !悬浮窗显示状态 && getBoolean("settings", "开关", false)) {
            启动悬浮窗(activity);
        }
    }
}

/**
 * 检查并更新后台状态，带 400ms 延迟防止 Activity 切换时的误判。
 * <p>
 * 延迟结束后若 {@code resumedActivityCount} 仍为 0，则确认进入后台：
 * 停止悬浮窗并启动保活服务。
 * </p>
 */
private void checkAndUpdateBackgroundState() {
    if (resumedActivityCount > 0) return;

    backgroundHandler.postDelayed(new Runnable() {
        public void run() {
            if (resumedActivityCount > 0) return;
            if (应用前台状态) {
                traceLog("state_log", "状态变更 → 后台");
                应用前台状态 = false;
                允许触摸 = false;
                if (悬浮窗显示状态) 停止悬浮窗();
                startKeepAliveService();
            }
        }
    }, 400);
}
/**
 * QFun 专用通用 Hook 辅助函数，替代 XposedHelpers.findAndHookMethod。
 *
 * @param clazz              目标类
 * @param methodName         目标方法名
 * @param typesAndCallback   参数类型列表 + 末位 {@link XC_MethodHook} 回调
 */
void qfunHook(Class clazz, String methodName, Object[] typesAndCallback) {
    try {
        if (clazz == null) return;

        XC_MethodHook callback = (XC_MethodHook) typesAndCallback[typesAndCallback.length - 1];
        Class[] paramTypes = new Class[typesAndCallback.length - 1];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = (Class) typesAndCallback[i];
        }

        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);

        hookloveList.add(XposedBridge.hookMethod(method, callback));
    } catch (Throwable e) {
        traceLog("main_log", "Hook失败 [" + methodName + "]: " + e.toString());
    }
}

/**
 * Hook QQ 生命周期方法（onResume / onPause / Application.onCreate）。
 * <p>
 * onResume：Activity 可见时递增计数器并检查前台状态。<br>
 * onPause：Activity 不可见时递减计数器并检查后台状态。<br>
 * Application.onCreate：进程下次冷启动时提前拉起 Watchdog，
 * 使保活在脚本完全加载之前即已生效。
 * </p>
 */
void Hook生命周期() {
    try {
        traceLog("main_log", "开始 Hook生命周期");

        qfunHook(android.app.Activity.class, "onResume", new Object[]{
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity a = (Activity) param.thisObject;
                    traceLog(" _log.txt", "" + a.getClass().getSimpleName());
                    if (QQpackage.equals(a.getPackageName())) {
                        resumedActivityCount++;
                        checkAndUpdateForegroundState(a);
                    }
                }
            }
        });

        qfunHook(android.app.Activity.class, "onPause", new Object[]{
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity a = (Activity) param.thisObject;
                    if (QQpackage.equals(a.getPackageName())) {
                        resumedActivityCount--;
                        if (resumedActivityCount < 0) resumedActivityCount = 0;
                        checkAndUpdateBackgroundState();
                    }
                }
            }
        });

        if (getNowActivity() != null) {
            resumedActivityCount = 1;
            checkAndUpdateForegroundState(getNowActivity());
        }

        try { initStats(); } catch (Exception e) {}
        try { installQFunHooks(); } catch (Exception e) {}
		if (getBoolean("settings", "后台保活", false)) {
		    try { HookQQService(); } catch (Exception e) {}
		}


        Hook已调用 = true;

    } catch (Throwable e) {
        traceLog("main_log", "Hook生命周期异常: " + e.getMessage());
    }
}


void 后台初始化() {
    if (非UI初始化完成) return;

    try {
        ensureResourceAvailable();
        addItem("开/关悬浮窗", "悬浮窗开关");
        addItem("Java脚本", "openPlugin");
        addItem("设置页面", "openSetting");
        traceLog("main_log", "add项添加完成");
        isRunning.set(false);

        boolean 模拟定位开关a = getBoolean("模拟定位开关", "模拟定位开关", false);
        if (模拟定位开关a) {
            开模拟定位();
        }

        非UI初始化完成 = true;

    } catch (Exception e) {
        traceLog("main_log", "后台初始化异常: " + e.getMessage());
    }
}

/**
 * 前台初始化
 * @param currentActivity 当前 Activity
 */
void 前台初始化(Activity currentActivity) {
    if (UI初始化完成 || currentActivity == null) {
        return;
    }

    String appType = null;
    String pkg = currentActivity.getPackageName();
    if ("com.tencent.mobileqq".equals(pkg)) {
        appType = "QQ";
    } else if ("com.tencent.tim".equals(pkg)) {
        appType = "TIM";
    }
    if (appType == null) {
        return;
    }
    int personalCount = getInt("settings", "加载次数", 0) + 1;
    putInt("settings", "加载次数", personalCount);
    final String finalAppType = appType;
    final int finalPersonalCount = personalCount;
    final Activity finalActivity = currentActivity;

    ThreadPool.execute(new Runnable() {
        public void run() {
            String countJson = get("https://api.521567.xyz/api/jisuan/api.php?id=5201314&key=ovo5201314&type=1&number=1");
            int totalCount = 0;
            if (countJson != null && !countJson.isEmpty()) {
                try {
                    JSONObject jsonObj = new JSONObject(countJson.trim());
                    if (jsonObj.getInt("code") == 200) {
                        totalCount = jsonObj.getInt("value");
                    } else {
                        Toast("计数异常");
                    }
                } catch (Exception e) {
                    traceLog("api_error", "计数解析异常：" + e.getMessage());
                }
            }

            String qqKey = "用户数" + qq;
            if (!getBoolean("settings", qqKey, false)) {
                String writeJson = get("https://cn.apihz.cn/api/jisuan/jishuqi2.php?id=10013224&key=17e1755199ff8eebc2fd58bce20d950e&type=1&number=2");
                if (writeJson != null && !writeJson.isEmpty()) {
                    try {
                        JSONObject jsonObje = new JSONObject(writeJson.trim());
                        if (jsonObje.getInt("code") == 200) {
                            putBoolean("settings", qqKey, true);
                        }
                    } catch (Exception e) {
                        traceLog("api_error", "新用户标记异常：" + e.getMessage());
                    }
                }
            }

            final int finalTotalCount = totalCount;
            finalActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (getBoolean("settings", "加载提示", false)) {
                        Toast("当前运行 App 为：" + finalAppType + "\n点击悬浮窗查看菜单\n加载耗时：" + apiLoadCostTime + "ms\n您累计加载" + finalPersonalCount + "次\n全网累计加载" + finalTotalCount + "次");
                    }
                }
            });
        }
    });
    // chatInterface(1, "666666", "请重新进入当前聊天");
    checkQFXUpdate();

    if (getBoolean("settings", "开关", false)) {
        悬浮窗状态 = STATE_DESTROYED;
        启动悬浮窗(currentActivity);
    }

    UI初始化完成 = true;
}

void 原神启动() {
    traceLog("main_log", "原神启动 被调用");

    if (apiLoadCostTime == -1) {
        traceLog("main_log", "错误：apiLoadCostTime未初始化");
        return;
    }

    traceLog("main_log", "脚本开始初始化，耗时：" + apiLoadCostTime + "ms");

    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                后台初始化();
                traceLog("main_log", "后台初始化完成");

                Activity currentAct = getNowActivity();
                if (currentAct != null) {
                    traceLog("main_log", "有可见Activity，执行前台初始化");
                    最后Activity = currentAct;
                    uiHandler.post(new Runnable() {
                        public void run() {
                            前台初始化(currentAct);
                        }
                    });
                } else {
                    traceLog("main_log", "无可见Activity，发送通知");
                    String appType = "com.tencent.mobileqq".equals(currentPackageName) ? "QQ" : "TIM";
                    String notifyContent = "脚本已后台加载完成\n" +
                                           "加载耗时：" + apiLoadCostTime + "ms\n" +
                                           "当前应用：" + appType + "\n" +
                                           "请打开 QQ 后查看悬浮窗或设置";
                    if (getBoolean("settings", "加载通知", false)) {
                        sendNotification("QFloatingX初始化提示", notifyContent);
                    }
                }

            } catch (Exception e) {
                traceLog("main_log", "初始化异常: " + e.getMessage());
            }
        }
    });
        if (!Hook已调用) {
        Hook生命周期();
        Hook已调用 = true;
        traceLog("main_log", "Hook生命周期已调用");
    }

}

addMenuItem("菜单", "菜单");
// 给作者点个赞
sendZan("3069670151", 50);
