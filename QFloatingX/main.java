//作者ᗜ×ᗜ
//使用请保留版权
//有bug或者建议可以大胆向我反馈

// 图标路径
String iconBase        = pluginPath + "/API/icon";
String iconPath        = new java.io.File(iconBase + ".png").exists() ? iconBase + ".png" :
                        new java.io.File(iconBase + ".gif").exists() ? iconBase + ".gif" :
                        iconBase + ".png"; // 默认兜底
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
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Color;

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
    rootPath + "setwindow.java",// 设置弹窗
    rootPath + "Dialog.java",    //  弹窗
    rootPath + "api2.java",     // 模拟定位
    rootPath + "api3.java",     // 消息统计
    rootPath + "api4.java",     // 悬浮窗
    // rootPath + "api5.java",     // 跳转
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
 * 发送高优先级通知到系统通知栏，支持点击回调代码执行和长文本展开
 */
void sendHighPriorityNotification(String title, String content, String channelId, String clickCode) {
    try {
        Context nowContext = getNowActivity();
        if (nowContext == null) {
            nowContext = context;
        }
        if (nowContext == null) return;
        
        NotificationManager notificationManager = (NotificationManager) 
            nowContext.getSystemService(Context.NOTIFICATION_SERVICE);
        
        PendingIntent pendingIntent;
        if (clickCode == null) {
            Intent intent = nowContext.getPackageManager().getLaunchIntentForPackage(nowContext.getPackageName());
            if (intent == null) {
                intent = new Intent();
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(nowContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(nowContext, 0, intent, 0);
            }
        } else {
            Intent intent = new Intent();
            intent.setPackage(nowContext.getPackageName());
            intent.setAction("QFUN_NOTIFICATION_CLICK_" + System.currentTimeMillis());
            intent.putExtra("execute_code", clickCode);
            int requestCode = (int)(System.currentTimeMillis() % 10000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(nowContext, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent.getActivity(nowContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "QFun通知", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("高优先级通知渠道");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(false);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(nowContext, channelId);
        } else {
            builder = new Notification.Builder(nowContext);
        }
        
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
        builder.setAutoCancel(true);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentIntent(pendingIntent);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
            bigTextStyle.setBigContentTitle(title);
            bigTextStyle.bigText(content);
            builder.setStyle(bigTextStyle);
        }
        
        Notification notification = builder.build();
        int notificationId = (int) (System.currentTimeMillis() % 10000);
        
        if (nowContext instanceof Activity) {
             ((Activity)nowContext).runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        notificationManager.notify(notificationId, notification);
                    } catch (Exception e) {}
                }
            });
        } else {
            try {
                notificationManager.notify(notificationId, notification);
            } catch (Exception e) {}
        }
        
    } catch (Throwable e) {}
}

/**
 * 简版通知发送
 */
void sendNotification(String title, String content) {
    sendHighPriorityNotification(title, content, "qfun_default_channel", null);
}

void sendNotification(String title, String content, String clickCode) {
    sendHighPriorityNotification(title, content, "qfun_default_channel", clickCode);
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
volatile boolean OK = false;
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

// 保活相关变量
private static final String COUNT_KEY = "startCount";
private static SharedPreferences prefs;

int KEEP_ALIVE_NOTIFICATION_ID = 0x9527;
String KEEP_ALIVE_CHANNEL_ID = "qfun_keep_alive";
String ACTION_GUARD_TRIGGERED = "com.qfun.GUARD_TRIGGERED"; 

static volatile boolean 守护进程已启动 = false;
private static volatile int guardCount = 0; 
BroadcastReceiver guardReceiver = null;

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

// ========== 精准前后台检测核心逻辑 ==========

/**
 * 检查并更新前台状态
 */
private void checkAndUpdateForegroundState(final Activity activity) {
    if (resumedActivityCount <= 0) return;
    
    // 后台初始化已完成但UI未初始化，立即补做UI
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
 * 检查并更新后台状态（带400ms延迟防误判）
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

// === QFun 专用 Hook 类导入 ===
import me.yxp.qfun.utils.hook.xpcompat.XposedBridge;
import me.yxp.qfun.utils.hook.xpcompat.XC_MethodHook;
import me.yxp.qfun.utils.hook.xpcompat.XC_MethodHook.MethodHookParam;
/**
 * QFun 专用通用 Hook 辅助函数
 * 替代 XposedHelpers.findAndHookMethod
 */
void qfunHook(Class clazz, String methodName, Object[] typesAndCallback) {
    try {
        if (clazz == null) return;
        
        // 分离参数类型和回调
        XC_MethodHook callback = (XC_MethodHook) typesAndCallback[typesAndCallback.length - 1];
        Class[] paramTypes = new Class[typesAndCallback.length - 1];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = (Class) typesAndCallback[i];
        }
        
        // 反射查找方法
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        
        // 使用 QFun 的 Bridge 进行 Hook
        hookloveList.add(XposedBridge.hookMethod(method, callback));
        // traceLog("main_log", "Hook成功: " + clazz.getName() + "." + methodName);
        
    } catch (Throwable e) {
        traceLog("main_log", "Hook失败 [" + methodName + "]: " + e.toString());
    }
}
void Hook生命周期() {
    try {
        traceLog("main_log", "开始 Hook生命周期 (QFun原生版)");

        // Hook onResume
        qfunHook(android.app.Activity.class, "onResume", new Object[]{
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity a = (Activity) param.thisObject;
                    if (QQpackage.equals(a.getPackageName())) {
                        resumedActivityCount++;
                        checkAndUpdateForegroundState(a);
                    }
                }
            }
        });
        
        // Hook onPause
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

        // 这里的其他调用保持不变...
        if (getNowActivity() != null) {
            resumedActivityCount = 1;
            checkAndUpdateForegroundState(getNowActivity());
        }
        
        try { initStats(); } catch (Exception e) {}
        try { installQFunHooks(); } catch (Exception e) {}
        
        // 调用我们重写后的 HookQQService
        try { HookQQService(); } catch (Exception e) {}
        
        Hook已调用 = true;
        
    } catch (Throwable e) {
        traceLog("main_log", "Hook生命周期异常: " + e.getMessage());
    }
}


void updateNotification(Context ctx) {
    try {
        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(QQpackage);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= 0x04000000;
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, flags);
        
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) builder = new Notification.Builder(ctx, KEEP_ALIVE_CHANNEL_ID);
        else builder = new Notification.Builder(ctx).setPriority(Notification.PRIORITY_MAX);
        
        builder.setContentTitle("QFloatingX运行中...  ")
               .setContentText("正在守护您的QQ丨已为您守护 " + guardCount + " 次")
               .setSmallIcon(android.R.drawable.ic_menu_info_details)
               .setContentIntent(pendingIntent)
               .setOngoing(true)
               .setUsesChronometer(true)
               .setWhen(startTime)
               .setShowWhen(true);
        
        nm.notify(KEEP_ALIVE_NOTIFICATION_ID, builder.build());
    } catch (Exception e) {}
}

void createKeepAliveChannel(Context context) {
    if (Build.VERSION.SDK_INT >= 26) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(KEEP_ALIVE_CHANNEL_ID, "QFun后台保活", NotificationManager.IMPORTANCE_MIN);
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        } catch (Exception e) {}
    }
}

static volatile boolean isMsfHooked = false;
// 定义广播 Action
String ACTION_MSF_PING = "com.qfun.MSF_PING";

void HookQQService() {
    // 1. 防重复检查
    if (isMsfHooked) {
        traceLog("keepalive_log", "MsfService 逻辑已注入，跳过重复执行");
        return;
    }

    try {
        // 2. 进程判断：尝试加载 MsfService 类
        // 如果加载不到，说明当前是主进程，直接标记已处理并退出，防止浪费资源
        Class msfClass = null;
        ClassLoader loader = null;
        try { loader = classLoader; } catch(Exception e) {} 
        if (loader == null) loader = context.getClassLoader();

        try {
            // 只是为了检测是否在 MSF 进程，不直接 Hook 这个类
            loader.loadClass("com.tencent.mobileqq.msf.service.MsfService");
        } catch (ClassNotFoundException e) {
            return; // 不是 MSF 进程，直接退出
        }

        traceLog("keepalive_log", "检测到 MSF 进程 (PID: " + android.os.Process.myPid() + ")，开始注入保活...");

        // 3. 通用 Hook：直接 Hook Service 基类，稳准狠
        // 这样不用担心 MsfService 有没有重写方法，也不用担心找不到类
        
        // --- Hook onStartCommand (心跳/启动) ---
        qfunHook(android.app.Service.class, "onStartCommand", new Object[]{
            Intent.class, int.class, int.class,
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (!service.getClass().getName().contains("MsfService")) return; // 只处理 MsfService

                    try {
                        Context ctx = (Context) service;
                        // 发送广播给主进程：“我活着，给我计数+1”
                        Intent intent = new Intent(ACTION_MSF_PING);
                        intent.setPackage(ctx.getPackageName()); // 明确包名，通过限制
                        ctx.sendBroadcast(intent);
                        // traceLog("keepalive_log", "MsfService 心跳发送完毕");
                    } catch (Throwable e) {}
                }
            }
        });

        // --- Hook onDestroy (复活) ---
        qfunHook(android.app.Service.class, "onDestroy", new Object[]{
            new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (!service.getClass().getName().contains("MsfService")) return;

                    try {
                        Context ctx = (Context) service;
                        traceLog("keepalive_log", "MsfService 正在死亡，执行复活术...");
                        
                        Intent restart = new Intent();
                        restart.setClassName(ctx.getPackageName(), "com.tencent.mobileqq.msf.service.MsfService");
                        restart.setPackage(ctx.getPackageName());
                        
                        try {
                           if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(restart);
                           else ctx.startService(restart);
                        } catch (Exception e) {
                           ctx.startService(restart); 
                        }
                    } catch (Throwable e) {}
                }
            }
        });

        // --- Hook onTaskRemoved (划卡复活) ---
        qfunHook(android.app.Service.class, "onTaskRemoved", new Object[]{
            Intent.class,
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (!service.getClass().getName().contains("MsfService")) return;

                    try {
                        Context ctx = (Context) service;
                        traceLog("keepalive_log", "检测到划卡操作，立即重启 MsfService...");
                        
                        Intent restart = new Intent();
                        restart.setClassName(ctx.getPackageName(), "com.tencent.mobileqq.msf.service.MsfService");
                        restart.setPackage(ctx.getPackageName());
                        ctx.startService(restart);
                    } catch (Throwable e) {}
                }
            }
        });

        // 标记为已注入，防止再次调用
        isMsfHooked = true;
        traceLog("keepalive_log", "保活逻辑注入完成");

    } catch (Throwable e) {
        traceLog("keepalive_err", "HookQQService 异常: " + e.toString());
    }
}

// 记录接收器是否已注册，防止重复注册报错
static volatile boolean isReceiverRegistered = false;

void startKeepAliveService() {
    try {
        Context ctx = context; // 这里的 Context 是主进程的 UI Context
        
        // 读取历史计数
        guardCount = getInt("settings", "guardCount", 0);
        
        // 注册广播接收器 (只注册一次)
        if (!isReceiverRegistered) {
            guardReceiver = new BroadcastReceiver() {
                public void onReceive(Context c, Intent i) {
                    if (ACTION_MSF_PING.equals(i.getAction())) {
                        // 收到后台的“我活着”信号，主进程自己加 1
                        guardCount++;
                        // 保存到本地配置
                        putInt("settings", "guardCount", guardCount);
                        
                        // 更新通知栏（降低频率，每10次更新一次，或者每次更新都行）
                        // traceLog("keepalive_log", "收到心跳，当前计数: " + guardCount);
                        updateNotification(c);
                    }
                }
            };
            
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_MSF_PING);
            
            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(guardReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(guardReceiver, filter);
            }
            
            isReceiverRegistered = true;
            traceLog("keepalive_log", "主进程广播监听已启动");
        }
        
        createKeepAliveChannel(ctx);
        updateNotification(ctx);
        
        // 尝试注入 Hook (内部有防重复判断)
        try { HookQQService(); } catch (Exception e) {}

    } catch (Throwable e) {
        traceLog("keepalive_error", "启动失败: " + e.getMessage());
    }
}


void stopKeepAlive() {
    try {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(KEEP_ALIVE_NOTIFICATION_ID);
        if (guardReceiver != null) {
            try { context.unregisterReceiver(guardReceiver); } catch (Exception e) {}
            guardReceiver = null;
        }
    } catch (Exception e) {}
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

void 前台初始化(Activity currentActivity) {
    if (UI初始化完成 || currentActivity == null) return;
    
    try {
        checkQFXUpdate();

        final String appType;
        if ("com.tencent.mobileqq".equals(currentPackageName)) {
            appType = "QQ";
        } else if ("com.tencent.tim".equals(currentPackageName)) {
            appType = "TIM";
        } else {
            return;
        }

        Toast("当前运行App为: " + appType + "\n点击悬浮窗查看菜单\n加载耗时：" + apiLoadCostTime + "ms");

        boolean 模拟定位开关a = getBoolean("模拟定位开关", "模拟定位开关", false);
        if (模拟定位开关a) {
            Toast("正在开启模拟定位...");
        }

        if (getBoolean("settings", "开关", false)) {
            悬浮窗状态 = STATE_DESTROYED;
            启动悬浮窗(currentActivity);
        }
        
        UI初始化完成 = true;
        
    } catch (Exception e) {
        traceLog("main_log", "前台初始化异常: " + e.getMessage());
    }
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
                    sendNotification("QFloatingX初始化提示", notifyContent);
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




/** 标记 Intent 防止递归 Hook */
private final String KEY_HANDLED = "qfun_script_handled";

/** 全局弹窗显示锁，防止多重弹窗 */
private volatile boolean isDialogShowing = false;

/** 原功能重放标记，用于回旋镖逻辑 */
private volatile boolean isReplayingClick = false;

/** 主线程 Handler，用于 UI 操作 */
private final Handler mainHandler = new Handler(Looper.getMainLooper());

/** 图片内存缓存 (Url -> Bitmap) */
private final HashMap picimageCache = new HashMap();

/** 原始特殊文本集合 (用于精准渲染 @ 和表情) */
private final HashSet validSpecialTexts = new HashSet();

/** 全局 Hook 注册表，用于一键卸载 */
private final List HOOK_REGISTRY = new ArrayList();

/** 滑动菜单状态池 [0:Popup, 1:Slider, 2:Centers, 4:TextViews, 5:BaseInfo, 7:UpdateRunnable, 8:Root] */
private final Object[] WHEEL_STATE = new Object[10];

/** 日志 TAG */
private final String TAG = "maints_debug";

/** 双击检测变量：上次点击时间 */
private volatile long lastClickTime = 0;

/** 双击检测变量：上次点击的 View Hash */
private volatile int lastClickViewHash = 0;

/** 双击检测变量：待执行的单击任务 (用于取消) */
private volatile Runnable pendingClickRunnable = null;

void debugLog(String msg) {
    traceLog(TAG, msg);
}


/**
 * 获取完整图片链接 (拼接域名和 RKey)
 */
String getFullPicUrl(String url, int chatType) {
    if (url == null || url.isEmpty()) return "";
    if (url.startsWith("http")) return url;
    String domain = "https://multimedia.nt.qq.com.cn";
    if (!url.startsWith("/")) url = "/" + url;
    String rkey = "";
    try {
        rkey = (chatType == 1) ? OnGetRKey.INSTANCE.getFriendRkey() : OnGetRKey.INSTANCE.getGroupRkey();
    } catch (Throwable t) {
        debugLog("RKey获取失败: " + t.getMessage());
    }
    return domain + url + rkey;
}


// ============================================================================
// 数据挖掘核心 (Data Mining) - 重构版
// ============================================================================

/**
 * 递归反射搜索 MsgRecord 对象
 * 核心修复：强制黑名单 (source/reply/quote)
 */
MsgRecord deepSearchMsgRecord(Object obj, int depth, HashSet visited) {
    if (obj == null || depth > 8) return null;

    // 防环
    int hash = System.identityHashCode(obj);
    if (visited.contains(hash)) return null;
    visited.add(hash);

    // 1. 直接匹配
    if (obj instanceof MsgRecord) {
        if (((MsgRecord) obj).msgId != 0) return (MsgRecord) obj;
    }

    String clsName = obj.getClass().getName();
    if (clsName.startsWith("java.") || clsName.startsWith("android.") || clsName.startsWith("androidx.")) return null;

    // 2. 容器遍历
    try {
        if (obj instanceof Iterable) {
            for (Object item : (Iterable) obj) {
                MsgRecord res = deepSearchMsgRecord(item, depth + 1, visited);
                if (res != null) return res;
            }
            return null;
        }
        if (obj instanceof Map) {
            for (Object value : ((Map) obj).values()) {
                MsgRecord res = deepSearchMsgRecord(value, depth + 1, visited);
                if (res != null) return res;
            }
            return null;
        }
    } catch (Throwable t) {}

    // 3. 反射遍历字段
    try {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName().toLowerCase();

            // 🔥【核心修复】跳过引用/回复相关字段    
            // 防止挖掘到被引用的 MsgRecord (历史记录)
            if (fieldName.contains("reply") ||     
                fieldName.contains("source") ||     
                fieldName.contains("quote") ||     
                fieldName.contains("origin") ||    
                fieldName.contains("ref") ||
                fieldName.contains("record")) { // 这里的 record 有时是主数据，但也可能是引用，需谨慎，但为了解决 Bug 暂时屏蔽
                
                 // 特例：如果字段叫 msgRecord 或 data，放行
                 if (!fieldName.equals("msgrecord") && !fieldName.equals("data") && !fieldName.equals("msginfo")) {
                     continue;
                 }
            }    

            Object val = field.get(obj);    
            if (val != null) {    
                if (val instanceof MsgRecord) {    
                    if (((MsgRecord) val).msgId != 0) {
                        debugLog("  -> 字段命中: " + field.getName());    
                        return (MsgRecord) val;
                    }
                }    

                // 仅对特定对象递归
                String valCls = val.getClass().getName();    
                if (valCls.startsWith("com.tencent") || val instanceof Map || val instanceof Iterable || 
                    fieldName.contains("data") || fieldName.contains("item") || fieldName.contains("info")) {    
                    MsgRecord deepRes = deepSearchMsgRecord(val, depth + 1, visited);    
                    if (deepRes != null) return deepRes;    
                }    
            }    
        }
    } catch (Throwable t) {}
    return null;
}

MsgRecord deepSearchMsgRecord(Object obj) {
    return deepSearchMsgRecord(obj, 0, new HashSet());
}

boolean isAvatarView(View view) {
    if (view == null) return false;
    String clsName = view.getClass().getName();
    String idName = "";
    try { if (view.getId() != View.NO_ID) idName = view.getResources().getResourceEntryName(view.getId()); } catch (Exception e) {}
    return clsName.contains("Avatar") || clsName.contains("Head") || idName.contains("head") || idName.contains("avatar") || idName.equals("chat_item_head_icon");
}

/**
 * 判断 View 是否处于引用/回复容器内
 */
boolean isInQuoteOrReplyContainer(View view) {
    View current = view;
    for (int i = 0; i < 10; i++) {
        if (current == null) break;
        String clsName = current.getClass().getName();
        String idName = "";
        try { if (current.getId() != View.NO_ID) idName = current.getResources().getResourceEntryName(current.getId()); } catch (Exception e) {}

        if (clsName.contains("Reply") || clsName.contains("Quote") || clsName.contains("SourceMsg") || idName.contains("reply") || idName.contains("quote")) {    
            debugLog("判定在引用容器: Parent L" + i + " = " + clsName);    
            return true;    
        }    
        if (current.getParent() instanceof View) current = (View) current.getParent(); else break;    
    }
    return false;
}

/**
 * 从 View 层级中挖掘消息记录
 * 核心修复：如果是引用组件，跳过 Tag 扫描，向上回溯
 */
MsgRecord findMsgRecordFromViewHierarchy(View startView) {
    View current = startView;
    for (int i = 0; i < 20; i++) { // 增加深度
        if (current == null) break;
        
        String clsName = current.getClass().getName();
        String idName = "";
        try { if (current.getId() != View.NO_ID) idName = current.getResources().getResourceEntryName(current.getId()); } catch(Exception e){}

        // 1. 引用区逃逸逻辑
        boolean isReference = clsName.contains("Reply") || clsName.contains("Quote") || clsName.contains("SourceMsg") || idName.contains("reply");
        if (isReference) {
            debugLog("L" + i + " 位于引用容器，跳过 Tag 扫描，向上回溯...");
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
                continue; 
            } else break;
        }

        // 2. 扫描 KeyedTags (优先)
        try {    
            Field fKeyedTags = View.class.getDeclaredField("mKeyedTags");    
            fKeyedTags.setAccessible(true);    
            Object keyedTags = fKeyedTags.get(current);    
            if (keyedTags instanceof SparseArray) {    
                SparseArray sa = (SparseArray) keyedTags;
                for(int k=0; k<sa.size(); k++) {
                    MsgRecord found = deepSearchMsgRecord(sa.valueAt(k));
                    if (found != null) {
                        debugLog("L" + i + " KeyedTag Found: " + found.msgId);
                        return found;
                    }
                }
            }    
        } catch (Throwable t) {}   

        // 3. 扫描 Tag
        Object tag = current.getTag();
        if (tag != null) {
            MsgRecord rec = deepSearchMsgRecord(tag);
            if (rec != null) {
                debugLog("L" + i + " Tag Found: " + rec.msgId);
                return rec;
            }
        }

        // 4. 低层级反射 View 字段
        if (i < 4) {
             MsgRecord viewBound = deepSearchMsgRecord(current);
             if (viewBound != null) {
                 debugLog("L" + i + " View Field Found: " + viewBound.msgId);
                 return viewBound;
             }
        }

        if (current.getParent() instanceof View) current = (View) current.getParent(); else break;
    }
    return null;
}


// ============================================================================
// 业务逻辑 (Business Logic)
// ============================================================================

interface MsgLoadedCallback {
    void onLoaded(MsgData msgData);
}

/**
 * 通过 Kernel 异步获取完整消息记录
 */
void fetchRealMsgRecord(final long msgId, final int chatType, final String peerUid, final MsgLoadedCallback callback) {
    new Thread(new Runnable() {
        public void run() {
            try {
                if (msgId == 0) return;
                IKernelMsgService kernel = QQCurrentEnv.INSTANCE.getKernelMsgService();
                if (kernel == null) {
                    mainHandler.post(new Runnable() { public void run() { Toast("内核服务未就绪"); isDialogShowing = false; } });
                    return;
                }
                Contact contact = new Contact(chatType, peerUid, "");
                ArrayList ids = new ArrayList();
                ids.add(msgId);
                kernel.getMsgsByMsgId(contact, ids, new IMsgOperateCallback() {
                    public void onResult(int res, String err, ArrayList list) {
                        if (list != null && !list.isEmpty()) {
                            final MsgRecord realRecord = (MsgRecord) list.get(0);
                            try {
                                final MsgData msgData = new MsgData(realRecord);
                                mainHandler.post(new Runnable() { public void run() { if (callback != null) callback.onLoaded(msgData); } });
                            } catch (Throwable t) {
                                debugLog("MsgData构造失败");
                                isDialogShowing = false;
                            }
                        } else {
                            mainHandler.post(new Runnable() { public void run() { Toast("消息不存在"); isDialogShowing = false; } });
                        }
                    }
                });
            } catch (Throwable t) {
                debugLog("FetchMsg异常: " + t.getMessage());
                isDialogShowing = false;
            }
        }
    }).start();
}


/**
 * 解析文本中的 [pic=url] 标签，还原为 MsgElements
 */
void parseTextToElements(String text, ArrayList sendElements, MsgRecord originalRecord, int chatType) {
    Map originalPicMap = new HashMap();
    // 建立原消息图片映射，以便复用
    if (originalRecord.elements != null) {
        for (int i = 0; i < originalRecord.elements.size(); i++) {
            MsgElement el = (MsgElement) originalRecord.elements.get(i);
            if (el.elementType == 2 && el.picElement != null) {
                String fullUrl = getFullPicUrl(el.picElement.originImageUrl, chatType);
                originalPicMap.put(fullUrl, el);
            }
        }
    }

    Pattern p = Pattern.compile("\\[pic=(.*?)\\]");
    Matcher m = p.matcher(text);
    int lastEnd = 0;
    while (m.find()) {
        String sub = text.substring(lastEnd, m.start());
        if (!sub.isEmpty()) addTextElement(sendElements, sub);

        String url = m.group(1);    
        if (originalPicMap.containsKey(url)) sendElements.add(originalPicMap.get(url));    
        else addTextElement(sendElements, "[pic=" + url + "]");    
        lastEnd = m.end();
    }
    String remain = text.substring(lastEnd);
    if (!remain.isEmpty()) addTextElement(sendElements, remain);
}


void addTextElement(ArrayList list, String content) {
    MsgElement el = new MsgElement();
    el.elementType = 1;
    TextElement te = new TextElement();
    te.content = content;
    te.atType = 0;
    el.textElement = te;
    list.add(el);
}

/** 发送/复读 入口 */
void doSendOrRepeat(final MsgData data, final String newText) {
    doMultiSend(data, newText, 1);
}

/**
 * 执行发送逻辑 (支持多次)
 */
void doMultiSend(final MsgData data, final String newText, final int count) {
    new Thread(new Runnable() {
        public void run() {
            try {
                if (data == null || data.data == null) return;
                MsgRecord originalRecord = data.data;
                final ArrayList sendElements = new ArrayList();

                if (newText != null) {    
                    parseTextToElements(newText, sendElements, originalRecord, data.type);    
                } else {    
                    if (originalRecord.elements != null) sendElements.addAll(originalRecord.elements);    
                }    

                IMsgService msgService = (IMsgService) QRoute.api(IMsgService.class);    
                if (msgService == null) return;    

                String targetUid = originalRecord.peerUid;    
                if (targetUid == null || targetUid.isEmpty()) {    
                    if (originalRecord.chatType == 2) targetUid = String.valueOf(originalRecord.peerUin);    
                    else targetUid = FriendTool.INSTANCE.getUidFromUin(String.valueOf(originalRecord.peerUin));    
                }    

                Contact contact = new Contact(originalRecord.chatType, targetUid, "");    
                for (int i = 0; i < count; i++) {    
                    msgService.sendMsg(contact, sendElements, null);    
                    if (count > 5) Thread.sleep(50);    
                }    
                final String tips = (newText != null ? "发送" : "复读") + (count > 1 ? " x" + count : "") + " 完成";    
                mainHandler.post(new Runnable() { public void run() { Toast(tips); } });    
            } catch (final Throwable e) {    
                mainHandler.post(new Runnable() { public void run() { Toast("失败: " + e.getMessage()); } });    
            }    
        }
    }).start();
}


// ============================================================================
// UI 渲染与交互 (UI Rendering & Interaction)
// ============================================================================

/** 异步下载图片 */
void downloadImage(final String url, final Runnable callback) {
    if (picimageCache.containsKey(url)) { if (callback != null) callback.run();
        return; }
    new Thread(new Runnable() {
        public void run() {
            try {
                URL u = new URL(url);
                InputStream is = u.openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                is.close();
                Bitmap bmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                if (bmp != null) {
                    picimageCache.put(url, bmp);
                    if (callback != null) mainHandler.post(callback);
                }
            } catch (Throwable t) {}
        }
    }).start();
}

/** 自定义链接 Span */
class LinkTagSpan extends ForegroundColorSpan {
    public String url;
    public LinkTagSpan(String url) { super(Color.parseColor("#007AFF"));
        this.url = url; }
}

/**
 * 文本渲染逻辑
 */
void applySpans(final EditText et, final boolean forceImage) {
    Editable s = et.getText();
    String text = s.toString();
    final Context ctx = et.getContext();
    int selStart = et.getSelectionStart();
    int selEnd = et.getSelectionEnd();
    // 1. 清理旧 Spans
    Object[] allSpans = s.getSpans(0, s.length(), Object.class);
    for (int i = 0; i < allSpans.length; i++) {
        Object span = allSpans[i];
        if (span instanceof LinkTagSpan || span instanceof ForegroundColorSpan || span instanceof ImageSpan || span instanceof UnderlineSpan) {
            s.removeSpan(span);
        }
    }

    // 2. 渲染图片标签
    Pattern pPic = Pattern.compile("\\[pic=(.*?)\\]");
    Matcher mPic = pPic.matcher(text);
    while (mPic.find()) {
        final String url = mPic.group(1);
        final int start = mPic.start();
        final int end = mPic.end();
        boolean isCursorInside = (selStart >= start && selStart <= end) || (selEnd >= start && selEnd <= end);
        if ((forceImage || (picimageCache.containsKey(url) && !isCursorInside))) {
            downloadImage(url, new Runnable() {
                public void run() {
                    String cur = et.getText().toString();
                    if (cur.length() >= end && cur.substring(start, end).equals("[pic=" + url + "]")) {
                        Bitmap bmp = picimageCache.get(url);
                        if (bmp != null) {
                            int h = dp(ctx, 64);
                            int w = (int) ((float)bmp.getWidth() / bmp.getHeight() * h);
                            Bitmap scaled = Bitmap.createScaledBitmap(bmp, w, h, true);
                            Drawable d = new BitmapDrawable(ctx.getResources(), scaled);
                            d.setBounds(0, 0, w, h);
                            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                            et.getText().setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            });
        } else {
            s.setSpan(new LinkTagSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // 3. 精准渲染特殊文本 (@ /表情)
    for (Object validObj : validSpecialTexts) {
        String validText = (String) validObj;
        if (validText == null || validText.isEmpty()) continue;

        int idx = text.indexOf(validText);    
        while (idx >= 0) {    
            int end = idx + validText.length();    
            int color = 0;    
            if (validText.startsWith("@")) color = Color.parseColor("#007AFF");    
            else if (validText.startsWith("/")) color = Color.parseColor("#A6FFD700");    

            if (color != 0) {    
                s.setSpan(new ForegroundColorSpan(color), idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);    
            }    
            idx = text.indexOf(validText, end);    
        }
    }
}


/**
 * 触摸监听：处理点击图片 Span 还原为文本
 */
void setupEditTextTouch(final EditText et) {
    final GestureDetector gestureDetector = new GestureDetector(et.getContext(), new GestureDetector.SimpleOnGestureListener() {
        public boolean onSingleTapUp(MotionEvent e) {
            int x = (int) e.getX() - et.getTotalPaddingLeft() + et.getScrollX();
            int y = (int) e.getY() - et.getTotalPaddingTop() + et.getScrollY();
            Layout layout = et.getLayout();
            if (layout == null) return false;
            int line = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(line, x);
            Editable text = et.getText();
            ImageSpan[] imgs = text.getSpans(offset, offset, ImageSpan.class);
            if (imgs.length > 0) {
                ImageSpan clickedSpan = imgs[0];
                int spanStart = text.getSpanStart(clickedSpan);
                int spanEnd = text.getSpanEnd(clickedSpan);
                if (offset >= spanStart && offset <= spanEnd) {
                    text.removeSpan(clickedSpan);
                    et.setSelection((spanStart + spanEnd) / 2);
                    applySpans(et, false); // 重绘
                    return true;
                }
            }
            return false;
        }
    });
    et.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) { return gestureDetector.onTouchEvent(event); }
    });
}


// ============================================================================
// UI 组件工厂 (UI Component Factory)
// ============================================================================

FrameLayout createButton(Context ctx, String text, String bgColorStr, int textColor, final Runnable onClick) {
    final FrameLayout btn = new FrameLayout(ctx);
    final GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(ctx, 8));
    bg.setColor(Color.parseColor(bgColorStr));
    btn.setBackground(bg);
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextColor(textColor);
    tv.setTextSize(14);
    tv.setGravity(Gravity.CENTER);
    tv.setTypeface(null, Typeface.BOLD);
    btn.addView(tv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onClick.run(); } });
    return btn;
}

FrameLayout createPreviewButton(Context ctx, final Runnable onClick) {
    final FrameLayout btn = new FrameLayout(ctx);
    final GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(ctx, 8));
    bg.setColor(Color.TRANSPARENT);
    bg.setStroke(dp(ctx, 1), Color.parseColor("#E0E0E0"));
    btn.setBackground(bg);
    TextView tv = new TextView(ctx);
    tv.setText("预览");
    tv.setTextColor(Color.parseColor("#999999"));
    tv.setTextSize(12);
    tv.setGravity(Gravity.CENTER);
    btn.addView(tv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onClick.run(); } });
    return btn;
}

/**
 * 创建消息引用展示框
 */
View createReplyBox(Context ctx, MsgData msgData) {
    MsgElement replyEl = null;
    if (msgData.data.elements != null) {
        for (int i=0; i<msgData.data.elements.size(); i++) {
            MsgElement e = (MsgElement) msgData.data.elements.get(i);
            if (e != null && e.elementType == 7) { replyEl = e; break;
            }
        }
    }
    if (replyEl == null || replyEl.replyElement == null) return null;
    ReplyElement re = replyEl.replyElement;
    LinearLayout replyBox = new LinearLayout(ctx);
    replyBox.setOrientation(LinearLayout.HORIZONTAL);
    replyBox.setBackgroundColor(Color.parseColor("#F5F5F5"));
    replyBox.setPadding(0, 0, dp(ctx, 8), 0);
    View line = new View(ctx);
    GradientDrawable lineBg = new GradientDrawable();
    lineBg.setColor(Color.parseColor("#D0D0D0"));
    lineBg.setCornerRadius(dp(ctx, 2));
    line.setBackground(lineBg);
    replyBox.addView(line, new LinearLayout.LayoutParams(dp(ctx, 4), -1));
    LinearLayout replyTextContainer = new LinearLayout(ctx);
    replyTextContainer.setOrientation(LinearLayout.VERTICAL);
    replyTextContainer.setPadding(dp(ctx, 8), dp(ctx, 6), 0, dp(ctx, 6));
    // 解析时间
    long quoteTime = re.replyMsgTime;
    String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(quoteTime * 1000L));
    String nick = "引用消息";
    if (msgData.data.records != null && !msgData.data.records.isEmpty()) {
        try {
            MsgRecord src = (MsgRecord) msgData.data.records.get(0);
            if (src.sendNickName != null && !src.sendNickName.isEmpty()) nick = src.sendNickName;
        } catch(Throwable t) {}
    } else if (re.sourceMsgText != null && re.sourceMsgText.contains(":")) {
        nick = re.sourceMsgText.substring(0, re.sourceMsgText.indexOf(":"));
    }

    TextView tvHeader = new TextView(ctx);
    tvHeader.setText(nick + " " + timeStr);
    tvHeader.setTextSize(12);
    tvHeader.setTextColor(Color.GRAY);
    replyTextContainer.addView(tvHeader);
    TextView tvBody = new TextView(ctx);
    tvBody.setTextSize(13);
    tvBody.setTextColor(Color.parseColor("#666666"));
    tvBody.setMaxLines(3);
    tvBody.setEllipsize(TextUtils.TruncateAt.END);

    SpannableStringBuilder ssb = new SpannableStringBuilder();
    boolean hasContent = false;
    // 解析内容 (TextElems -> SourceMsgText -> Default)
    if (re.sourceMsgTextElems != null && !re.sourceMsgTextElems.isEmpty()) {
        for (int i=0; i<re.sourceMsgTextElems.size(); i++) {
            ReplyAbsElement rae = (ReplyAbsElement) re.sourceMsgTextElems.get(i);
            if (rae == null) continue;
            if (rae.textElemContent != null) { ssb.append(rae.textElemContent); hasContent = true;
            }
            else if (rae.faceElem != null) { ssb.append(rae.faceElem.faceText != null ? rae.faceElem.faceText : "[表情]");
                hasContent = true; }
            else if (rae.picElem != null) { ssb.append("[图片]");
                hasContent = true; }
        }
    }
    if (!hasContent && re.sourceMsgText != null && !re.sourceMsgText.isEmpty()) {
        ssb.append(re.sourceMsgText);
        hasContent = true;
    }
    if (!hasContent) ssb.append("[图片]"); // 回落

    tvBody.setText(ssb);
    replyTextContainer.addView(tvBody);
    replyBox.addView(replyTextContainer, new LinearLayout.LayoutParams(-1, -2));
    return replyBox;
}


// ============================================================================
// 外部调用方法 (External Methods)
// ============================================================================

void 加解密(Activity activity, String content) {
    Toast("调用外部方法: 加解密\n" + content);
    debugLog("Call crypto()");
}

void 作图(Activity activity, String content) {
    Toast("调用外部方法: 作图\n" + content);
    debugLog("Call makeImage()");
}

// ============================================================================
// 滑动选择器组件 (Slider Widget)
// ============================================================================

void updateSliderPhysics(float rawDx, boolean isDrag) {
    View slider = (View) WHEEL_STATE[1];
    Runnable updater = (Runnable) WHEEL_STATE[7];
    if (slider == null) return;
    float[] baseInfo = (float[]) WHEEL_STATE[5];
    float targetTrans = baseInfo[1] + rawDx;
    slider.setTranslationX(targetTrans);
    if (updater != null) updater.run();
}

PopupWindow showIOSStyleWheelSelector(
        final Activity activity,
        View anchor,
        final int[] modeIndexRef,
        final String[] modeNames,
        final FrameLayout actionBtn,
        final TextView btnTv,
        MotionEvent initialEvent
) {
    final PopupWindow popup = new PopupWindow(activity);
    popup.setBackgroundDrawable(null);
    popup.setOutsideTouchable(false);
    popup.setFocusable(false);
    popup.setTouchable(true);

    FrameLayout root = new FrameLayout(activity);    
    root.setBackgroundColor(Color.TRANSPARENT);    
    root.setAlpha(0f);    
    root.setScaleX(0.9f);    
    root.setScaleY(0.9f);    

    FrameLayout card = new FrameLayout(activity);    
    GradientDrawable cardBg = new GradientDrawable();    
    cardBg.setColor(Color.parseColor("#F2F2F7"));    
    cardBg.setCornerRadius(dp(activity, 16));    
    card.setBackground(cardBg);    
    card.setClipChildren(false);     
    card.setClipToPadding(false);    
        
    final int ITEM_H = dp(activity, 36) + dp(activity, 1);    
    final int ITEM_W = dp(activity, 49);     
    final int GAP = dp(activity, 4);    
    final int PADDING = dp(activity, 8);    
    final View slider = new View(activity);    
    GradientDrawable sliderBg = new GradientDrawable();    
    sliderBg.setColor(Color.WHITE);    
    sliderBg.setCornerRadius(dp(activity, 14));    
    slider.setBackground(sliderBg);    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {    
        slider.setClipToOutline(true);    
        slider.setOutlineProvider(new ViewOutlineProvider() {    
            public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(activity, 14)); }    
        });    
    }    
    FrameLayout.LayoutParams sliderLp = new FrameLayout.LayoutParams(ITEM_W, ITEM_H);    
    sliderLp.topMargin = PADDING;    
    sliderLp.leftMargin = PADDING;    
    card.addView(slider, sliderLp);    
    final FrameLayout textContainer = new FrameLayout(activity);    
    final TextView[] textViews = new TextView[modeNames.length];    
    final float[] itemCenters = new float[modeNames.length];    
    for (int i = 0; i < modeNames.length; i++) {    
        TextView tv = new TextView(activity);    
        tv.setText(modeNames[i]);    
        tv.setGravity(Gravity.CENTER);    
        tv.setTextSize(13);    
        tv.setTextColor(Color.BLACK);     
        float centerX = PADDING + (ITEM_W / 2f) + i * (ITEM_W + GAP);    
        itemCenters[i] = centerX;    
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ITEM_W, ITEM_H);    
        lp.leftMargin = (int)(centerX - ITEM_W / 2f);    
        lp.topMargin = PADDING;    
        textContainer.addView(tv, lp);    
        textViews[i] = tv;    
    }    
        
    int containerW = (ITEM_W * modeNames.length) + (GAP * (modeNames.length - 1)) + PADDING * 2;    
    card.addView(textContainer, new FrameLayout.LayoutParams(containerW, ITEM_H + PADDING*2, Gravity.CENTER));    
    root.addView(card);    
    popup.setContentView(root);    
        
    root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);    
    int popupHeight = root.getMeasuredHeight();    
    int[] location = new int[2];    
    anchor.getLocationOnScreen(location);    
    popup.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1] - popupHeight - dp(activity, 4));    

    root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();    

    WHEEL_STATE[0] = popup;    
    WHEEL_STATE[1] = slider;    
    WHEEL_STATE[2] = itemCenters;    
    WHEEL_STATE[4] = textViews;    
    WHEEL_STATE[8] = root;    
        
    final float startCenterX = itemCenters[modeIndexRef[0]];    
    final float minCenter = itemCenters[0];    
        
    slider.setTranslationX(startCenterX - minCenter);    
    // 物理引擎：阻尼回弹 + 挤压变形    
    final Runnable physicsUpdate = new Runnable() {    
        public void run() {    
            View s = (View)WHEEL_STATE[1];    
            float[] centers = (float[])WHEEL_STATE[2];    
            TextView[] tvs = (TextView[])WHEEL_STATE[4];    
            if(s==null || centers==null) return;    

            float logicTrans = s.getTranslationX();    
            float logicCenter = centers[0] + logicTrans;    
            int baseW = dp(activity, 49);    
            int newW = baseW;    
            float visualTrans = logicTrans;    
            float maxTrans = centers[centers.length - 1] - centers[0];    

            if (logicTrans < 0) {    
                visualTrans = 0f;    
                float overflow = -logicTrans;    
                float squash = overflow * 0.4f;    
                if (squash > baseW * 0.4f) squash = baseW * 0.4f;    
                newW = baseW - (int)squash;    
            } else if (logicTrans > maxTrans) {    
                float overflow = logicTrans - maxTrans;    
                float squash = overflow * 0.4f;    
                if (squash > baseW * 0.4f) squash = baseW * 0.4f;    
                newW = baseW - (int)squash;    
                visualTrans = maxTrans + (baseW - newW);    
            } else {    
                for (int i=0; i<centers.length-1; i++) {    
                    if (logicCenter >= centers[i] && logicCenter <= centers[i+1]) {    
                        float range = centers[i+1] - centers[i];    
                        float progress = (logicCenter - centers[i]) / range;     
                        float stretch = (float) Math.sin(progress * Math.PI) * dp(activity, 10);    
                        newW = baseW + (int)stretch;    
                        break;    
                    }    
                }    
            }    

            if (s.getTranslationX() != visualTrans) s.setTranslationX(visualTrans);    
            ViewGroup.LayoutParams lp = s.getLayoutParams();    
            if (lp.width != newW) { lp.width = newW; s.setLayoutParams(lp);    
            }    
                
            for(int i=0; i<centers.length; i++) {    
                float dist = Math.abs(logicCenter - centers[i]);    
                if (dist < baseW * 0.6f) tvs[i].setTextColor(Color.parseColor("#007AFF"));     
                else tvs[i].setTextColor(Color.BLACK);    
            }    
        }    
    };    
    WHEEL_STATE[7] = physicsUpdate;    
    mainHandler.post(physicsUpdate);    
    return popup;
}

// ============================================================================
// 主界面弹窗 (Main Dialog)
// ============================================================================

void showActionDialog(final Activity activity, final MsgData msgData, final Intent originalIntent, final View targetView) {
    if (activity == null || activity.isFinishing()) {
        isDialogShowing = false;
        return;
    }

    final String[] modeNames = new String[]{"复读", "多次复读", "作图", "加解密"};    
    final int[] currentModeIndex = new int[]{0};    
    final SpannableStringBuilder initSb = new SpannableStringBuilder();    
        
    validSpecialTexts.clear();    
        
    // 初始化文本内容并收集特殊格式    
    if (msgData.data.elements != null) {    
        for (int i=0; i<msgData.data.elements.size(); i++) {    
             MsgElement el = (MsgElement)msgData.data.elements.get(i);    
             if (el.elementType == 1 && el.textElement != null) {    
                 initSb.append(el.textElement.content);    
                 if (el.textElement.atType != 0) validSpecialTexts.add(el.textElement.content);    
             } else if (el.elementType == 2 && el.picElement != null) {    
                 initSb.append("[pic=" + getFullPicUrl(el.picElement.originImageUrl, msgData.type) + "]");    
             } else if (el.elementType == 6 && el.faceElement != null) {    
                 initSb.append(el.faceElement.faceText);    
                 validSpecialTexts.add(el.faceElement.faceText);    
             }    
        }    
    }    
    final String originalTextString = initSb.toString();    
    final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);    
    dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {    
        public void onDismiss(android.content.DialogInterface d) {     
            isDialogShowing = false;    
            for(int i=0; i<WHEEL_STATE.length; i++) WHEEL_STATE[i] = null;    
        }    
    });    
    FrameLayout root = new FrameLayout(activity);    
    root.setBackgroundColor(Color.parseColor("#99000000"));    
    root.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });    
    FrameLayout card = new FrameLayout(activity);    
    GradientDrawable bg = new GradientDrawable();    
    bg.setColor(Color.WHITE);    
    bg.setCornerRadius(dp(activity, 16));    
    card.setBackground(bg);    

    LinearLayout content = new LinearLayout(activity);    
    content.setOrientation(LinearLayout.VERTICAL);    
    content.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 12));    
        
    ScrollView scroll = new ScrollView(activity);    
    scroll.addView(content);    
    card.addView(scroll);    

    View replyView = createReplyBox(activity, msgData);    
    if (replyView != null) {    
        content.addView(replyView);    
        content.addView(new View(activity), new LinearLayout.LayoutParams(-1, dp(activity, 12)));    
    }    

    LinearLayout interactionRow = new LinearLayout(activity);    
    interactionRow.setOrientation(LinearLayout.HORIZONTAL);    
    interactionRow.setGravity(Gravity.CENTER_VERTICAL);    
    interactionRow.setPadding(0, 0, 0, dp(activity, 12));    
    final EditText editText = new EditText(activity);    
    editText.setText(initSb);    
    editText.setTextColor(Color.BLACK);    
        
    boolean isSelf = false;    
    try {    
        Object qObj = null;    
        try { qObj = qq;    
        } catch(Throwable t) {}     
        if (qObj != null && Long.parseLong(qObj.toString()) == msgData.data.senderUin) isSelf = true;    
    } catch(Throwable t) {}    
        
    GradientDrawable etBg = new GradientDrawable();    
    etBg.setCornerRadius(dp(activity, 8));    
    if (isSelf) {    
        etBg.setColor(Color.parseColor("#E7F0FF"));     
        etBg.setStroke(dp(activity, 1), Color.parseColor("#C8D4E5"));    
    } else {    
        etBg.setColor(Color.parseColor("#F5F5F5"));     
        etBg.setStroke(dp(activity, 1), Color.parseColor("#E0E0E0"));    
    }    
    editText.setBackground(etBg);    
    editText.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));    
        
    setupEditTextTouch(editText);    
    LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, -2, 1.0f);    
    etParams.rightMargin = dp(activity, 8);    
    interactionRow.addView(editText, etParams);    

    LinearLayout rightBtnContainer = new LinearLayout(activity);    
    rightBtnContainer.setOrientation(LinearLayout.VERTICAL);    
    rightBtnContainer.setGravity(Gravity.CENTER_HORIZONTAL);    
        
    final FrameLayout actionBtn = new FrameLayout(activity);    
    GradientDrawable btnBg = new GradientDrawable();    
    btnBg.setCornerRadius(dp(activity, 8));    
    btnBg.setColor(Color.parseColor("#FF007AFF"));    
    actionBtn.setBackground(btnBg);    

    final TextView btnTv = new TextView(activity);    
    btnTv.setText(modeNames[0]);    
    btnTv.setTextColor(Color.WHITE);    
    btnTv.setGravity(Gravity.CENTER);    
    actionBtn.addView(btnTv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));    
    rightBtnContainer.addView(actionBtn, new LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 40)));    
        
    View spacer = new View(activity);    
    rightBtnContainer.addView(spacer, new LinearLayout.LayoutParams(-1, dp(activity, 4)));    
    FrameLayout previewBtn = createPreviewButton(activity, new Runnable() {    
        public void run() { applySpans(editText, true); }    
    });    
    rightBtnContainer.addView(previewBtn, new LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 28)));    

    interactionRow.addView(rightBtnContainer);    

    final boolean[] isActionTriggered = {false};    
    actionBtn.setOnTouchListener(new View.OnTouchListener() {    
        private float downX = 0;    
        private boolean hasMoved = false;    

        public boolean onTouch(View v, MotionEvent event) {    
            int action = event.getAction();    
            switch (action) {    
                case MotionEvent.ACTION_DOWN:    
                    downX = event.getRawX();    
                    hasMoved = false;    
                    isActionTriggered[0] = false;    
                    WHEEL_STATE[0] = showIOSStyleWheelSelector(activity, actionBtn, currentModeIndex, modeNames, actionBtn, btnTv, event);    
                        
                    View s = (View) WHEEL_STATE[1];    
                    WHEEL_STATE[5] = new float[]{downX, s.getTranslationX()};    
                    return true;    

                case MotionEvent.ACTION_MOVE:    
                    float rawDx = event.getRawX() - downX;    
                    if (Math.abs(rawDx) > dp(activity, 15)) hasMoved = true;    
                    if (WHEEL_STATE[0] != null && hasMoved) {    
                        updateSliderPhysics(rawDx, true);    
                    }    
                    return true;    
                case MotionEvent.ACTION_UP:    
                case MotionEvent.ACTION_CANCEL:    
                    if (WHEEL_STATE[0] != null) {    
                        final PopupWindow popup = (PopupWindow) WHEEL_STATE[0];    
                        if (!hasMoved) {    
                            if (!isActionTriggered[0]) {    
                                isActionTriggered[0] = true;    
                                String currentText = editText.getText().toString();    
                                boolean isModified = !currentText.equals(originalTextString);    
                                int mode = currentModeIndex[0];    
                                if (mode == 0) { // 复读    
                                    dialog.dismiss();    
                                    doSendOrRepeat(msgData, isModified ? currentText : null);    
                                } else if (mode == 1) { // 多次    
                                    dialog.dismiss();    
                                    showRepeatCountDialog(activity, msgData, isModified ? currentText : null);    
                                } else if (mode == 2) { // 作图    
                                    dialog.dismiss();    
                                    作图(activity, currentText);    
                                } else if (mode == 3) { // 加解密    
                                    dialog.dismiss();    
                                    加解密(activity, currentText);    
                                }    
                            }    
                        } else {    
                            // 释放回弹    
                            View slider = (View)WHEEL_STATE[1];    
                            View menuRoot = (View)WHEEL_STATE[8];    
                            float[] centers = (float[])WHEEL_STATE[2];    
                                
                            if (slider != null) {    
                                float logicTrans = slider.getTranslationX();    
                                float logicCenter = centers[0] + logicTrans;    
                                int bestIdx = 0;    
                                float minDist = Float.MAX_VALUE;    
                                for(int i=0; i<centers.length; i++) {    
                                    float d = Math.abs(logicCenter - centers[i]);    
                                    if(d < minDist) { minDist = d; bestIdx = i;    
                                    }    
                                }    
                                    
                                final float targetTrans = centers[bestIdx] - centers[0];    
                                SpringAnimation animX = new SpringAnimation(slider, DynamicAnimation.TRANSLATION_X, targetTrans);    
                                animX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);    
                                animX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);    
                                    
                                final int finalIdx = bestIdx;    
                                currentModeIndex[0] = finalIdx;    
                                btnTv.setText(modeNames[finalIdx]);    
                                GradientDrawable bg = (GradientDrawable) actionBtn.getBackground();    
                                String[] colors = {"#007AFF", "#FF9500", "#AF52DE", "#34C759"};    
                                bg.setColor(Color.parseColor(colors[finalIdx % colors.length]));    
                                    
                                String now = editText.getText().toString();    
                                boolean mod = !now.equals(originalTextString);    
                                if (finalIdx == 0) btnTv.setText(mod ? "发送" : "复读");    
                                else if (finalIdx == 1) btnTv.setText(mod ? "多次发送" : "多次复读");    
                                else btnTv.setText(modeNames[finalIdx]);    
                                    
                                animX.start();    
                                if (menuRoot != null) {    
                                    menuRoot.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(150)    
                                        .withEndAction(new Runnable() { public void run() { if(popup.isShowing()) popup.dismiss(); } })    
                                        .start();    
                                } else {    
                                    popup.dismiss();    
                                }    
                            } else {    
                                popup.dismiss();    
                            }    
                        }    
                    }    
                    WHEEL_STATE[0] = null;    
                    hasMoved = false;    
                    return true;    
            }    
            return false;    
        }    
    });    
        
    content.addView(interactionRow);    

    editText.addTextChangedListener(new TextWatcher() {    
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}    
        public void onTextChanged(CharSequence s, int start, int before, int count) {}    
        public void afterTextChanged(Editable s) {    
            String nowText = s.toString();    
            boolean isModified = !nowText.equals(originalTextString);    
            int idx = currentModeIndex[0];    
                
            if (idx == 0) {    
                btnTv.setText(isModified ? "发送" : "复读");    
                actionBtn.setAlpha(TextUtils.isEmpty(nowText) ? 0.5f : 1.0f);    
                actionBtn.setEnabled(!TextUtils.isEmpty(nowText));    
            } else if (idx == 1) {    
                btnTv.setText(isModified ? "多次发送" : "多次复读");    
                actionBtn.setAlpha(1.0f);    
                actionBtn.setEnabled(true);    
            } else {    
                btnTv.setText(modeNames[idx]);    
                actionBtn.setAlpha(1.0f);    
            }    
            applySpans(editText, false);    
        }    
    });    
        
    mainHandler.post(new Runnable() { public void run() { applySpans(editText, true); } });    
    LinearLayout btm = new LinearLayout(activity);    
    btm.setPadding(0, dp(activity, 4), 0, 0);    
    FrameLayout menuBtn = createButton(activity, "更多", "#B3007AFF", Color.WHITE, new Runnable() {    
        public void run() {     
            dialog.dismiss();     
            菜单(msgData);     
        }    
    });    
    FrameLayout origBtn = createButton(activity, "原功能", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {    
        public void run() {    
            dialog.dismiss();     
            if (originalIntent != null) {    
                try {    
                    originalIntent.putExtra(KEY_HANDLED, true);    
                    activity.startActivity(originalIntent);    
                } catch (Exception e) {}    
            } else if (targetView != null) {    
                isReplayingClick = true;    
                try {    
                    targetView.performClick();    
                } catch (Exception e) {    
                } finally {    
                    // 防止标志位锁死，短时间后复位    
                    new Handler().postDelayed(new Runnable() {    
                        public void run() { isReplayingClick = false; }    
                    }, 500);    
                }    
            }    
        }    
    });    
    btm.addView(menuBtn, new LinearLayout.LayoutParams(0, dp(activity, 42), 1));    
    btm.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 12), -1));    
    btm.addView(origBtn, new LinearLayout.LayoutParams(0, dp(activity, 42), 1));    
    content.addView(btm);    
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(activity, 320), -2);    
    cp.gravity = Gravity.CENTER;    
    root.addView(card, cp);    
    dialog.setContentView(root);    
    dialog.show();
}

/**
 * 高频复读警告弹窗
 */
void showBigCountConfirm(Activity activity, final MsgData data, final String text, final int count, final Dialog parent) {
    final Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
    FrameLayout root = new FrameLayout(activity);
    root.setBackgroundColor(Color.parseColor("#99000000"));

    FrameLayout card = new FrameLayout(activity);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.WHITE);
    bg.setCornerRadius(dp(activity, 16));
    card.setBackground(bg);
    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
    card.addView(content);

    TextView title = new TextView(activity);
    title.setText("⚠️ 高频警告");
    title.setTextSize(18);
    title.setTextColor(Color.RED);
    title.setGravity(Gravity.CENTER);
    content.addView(title);

    TextView msg = new TextView(activity);
    msg.setText("复读 " + count + " 次可能导致账号风控\n确定要继续吗？");
    msg.setTextSize(15);
    msg.setTextColor(Color.BLACK);
    msg.setGravity(Gravity.CENTER);
    msg.setPadding(0, dp(activity, 10), 0, 0);
    content.addView(msg);

    LinearLayout btns = new LinearLayout(activity);
    btns.setPadding(0, dp(activity, 16), 0, 0);
    FrameLayout cancel = createButton(activity, "取消", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {
        public void run() { d.dismiss(); }
    });
    FrameLayout confirm = createButton(activity, "继续发送", "#FF3B30", Color.WHITE, new Runnable() {
        public void run() {
            d.dismiss();
            if(parent != null) parent.dismiss();
            doMultiSend(data, text, count);
        }
    });
    btns.addView(cancel, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    btns.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 12), -1));
    btns.addView(confirm, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    content.addView(btns);
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(activity, 280), -2);
    cp.gravity = Gravity.CENTER;
    root.addView(card, cp);
    d.setContentView(root);
    d.show();
}


/**
 * 次数输入弹窗
 */
void showRepeatCountDialog(final Activity activity, final MsgData data, final String currentText) {
    final Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
    FrameLayout root = new FrameLayout(activity);
    root.setBackgroundColor(Color.parseColor("#99000000"));
    FrameLayout card = new FrameLayout(activity);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.WHITE);
    bg.setCornerRadius(dp(activity, 16));
    card.setBackground(bg);
    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
    card.addView(content);
    final EditText et = new EditText(activity);
    et.setInputType(InputType.TYPE_CLASS_NUMBER);
    et.setHint("请输入次数");
    content.addView(et);
    LinearLayout btns = new LinearLayout(activity);
    btns.setPadding(0, dp(activity, 16), 0, 0);
    FrameLayout cancel = createButton(activity, "取消", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {
        public void run() { d.dismiss(); }
    });
    FrameLayout confirm = createButton(activity, "确定", "#FF007AFF", Color.WHITE, new Runnable() {
        public void run() {
            try {
                int count = Integer.parseInt(et.getText().toString());
                if (count > 0) {
                    if (count > 10) {
                        showBigCountConfirm(activity, data, currentText, count, d);

                    } else {    
                         doMultiSend(data, currentText, count);    
                         d.dismiss();    
                     }    
                 } else {    
                     Toast("次数必须大于0");    
                 }    
             } catch(Exception e) { Toast("请输入有效数字"); }    
         }
    });
    btns.addView(cancel, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    btns.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 12), -1));
    btns.addView(confirm, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    content.addView(btns);
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(activity, 280), -2);
    cp.gravity = Gravity.CENTER;
    root.addView(card, cp);
    d.setContentView(root);
    d.show();
}


// ============================================================================
// HOOK 入口 (Hook Logic)
// ============================================================================

void uninstallQFunHooks() {
    if (HOOK_REGISTRY.isEmpty()) return;
    for (int i = 0; i < HOOK_REGISTRY.size(); i++) {
        try {
            ((XC_MethodHook.Unhook) HOOK_REGISTRY.get(i)).unhook();
        } catch (Exception e) {}
    }
    HOOK_REGISTRY.clear();
    debugLog("Hook 已卸载");
}

void installQFunHooks() {
    uninstallQFunHooks();

    try {    
        // Hook 1: Instrumentation.execStartActivity (全屏预览拦截 - Intent层面)    
        XC_MethodHook.Unhook h1 = XposedBridge.hookMethod(    
            Instrumentation.class.getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class),    
            new XC_MethodHook() {    
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {    
                    try {    
                        // 如果是脚本触发的回放，直接放行    
                        if (isReplayingClick) return;    

                        final Intent intent = (Intent) param.args[4];    
                        if (intent == null || intent.getBooleanExtra(KEY_HANDLED, false)) return;    
                            
                        String target = "";    
                        if (intent.getComponent() != null) target = intent.getComponent().getClassName();    
                        else if (intent.getAction() != null) target = intent.getAction();    
                            
                        boolean isTarget = target.contains("TextPreviewActivity") ||     
                                           target.contains("QQGalleryActivity") ||    
                                           target.contains("PhotoPreviewActivity") ||    
                                           target.contains("AIOGalleryActivity") ||    
                                           target.contains("FileBrowserActivity");    

                        if (isTarget) {    
                            if (isDialogShowing) {    
                                param.setResult(null);    
                                return;    
                            }    

                            Bundle extras = intent.getExtras();    
                            if (extras != null) {    
                                long msgId = extras.getLong("realMsgId", 0);    
                                if (msgId == 0) msgId = extras.getLong("msgId", 0);    
                                if (msgId == 0) msgId = extras.getLong("uniseq", 0);    
                                int chatType = extras.getInt("nt_chat_type", 0);    
                                if (chatType == 0) chatType = extras.getInt("uintype", 0);    
                                    
                                String peerUid = extras.getString("key_bundle_nt_peeruid");    
                                if (peerUid == null) peerUid = extras.getString("peerUid", "");    
                                if (peerUid == null) peerUid = extras.getString("uin", "");    
                                if (msgId != 0 && peerUid != null && !peerUid.isEmpty()) {    
                                    debugLog("Intent拦截: MsgId=" + msgId);    
                                    param.setResult(null);     
                                    Activity act = (Activity) param.args[3];    
                                    if (act == null) act = QQCurrentEnv.INSTANCE.getActivity();    
                                    final Activity finalAct = act;    
                                    isDialogShowing = true;    
                                    fetchRealMsgRecord(msgId, chatType, peerUid, new MsgLoadedCallback() {    
                                        public void onLoaded(MsgData msgData) {     
                                            showActionDialog(finalAct, msgData, intent, null);     
                                        }    
                                    });    
                                } else {    
                                    intent.putExtra(KEY_HANDLED, true);    
                                }    
                            }    
                        }    
                    } catch (Throwable t) {    
                        debugLog("Intent Hook异常: " + t.getMessage());    
                    }    
                }    
            }    
        );    
        HOOK_REGISTRY.add(h1);    

        // Hook 2: View.performClick (白名单 + 双击弹窗策略)    
        XC_MethodHook.Unhook h2 = XposedBridge.hookMethod(    
            View.class.getDeclaredMethod("performClick"),    
            new XC_MethodHook() {    
                protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) {    
                    try {    
                        // 如果是脚本触发的回放，直接放行    
                        if (isReplayingClick) return;    

                        final View view = (View) param.thisObject;    
                        String clsName = view.getClass().getName();    
                            
                        // 1. 头像检测 (放行，点击头像通常是进主页)    
                        if (isAvatarView(view)) return;    

                        // 2. 白名单检测 (需要拦截的类型：引用/气泡/图片/文件/卡片/语音/灰条)    
                        boolean isTarget = false;    
                            
                        // 优先检查引用区域    
                        if (isInQuoteOrReplyContainer(view)) isTarget = true;    
                        else if (clsName.contains("RoundBubbleImageView")) isTarget = true; // 图片    
                        else if (clsName.contains("Ark")) isTarget = true; // 卡片    
                        else if (clsName.contains("Bubble")) isTarget = true; // 通用气泡    
                        else if (clsName.contains("Ptt")) isTarget = true; // 语音    
                        else if (clsName.contains("Audio")) isTarget = true; // 语音    
                        else if (clsName.contains("Struct")) isTarget = true; // 结构化消息  
                        else if (clsName.contains("Market")) isTarget = true; // 表情市场    
                        else if (clsName.contains("File")) isTarget = true; // 文件    
                        else if (clsName.contains("GrayTips")) isTarget = true; // 灰条    

                        if (!isTarget) return; // 非目标，放行    

                        // === 双击检测逻辑 ===    
                        long now = System.currentTimeMillis();    
                        int viewHash = view.hashCode();    
                        boolean isDouble = (now - lastClickTime < 350 && lastClickViewHash == viewHash);    

                        if (isDouble) {    
                            // >>> 双击触发自定义弹窗 <<<    
                            debugLog(">>> 双击命中 -> 尝试拦截并弹窗");    
                                
                            // 1. 取消待执行的单击任务    
                            if (pendingClickRunnable != null) {    
                                mainHandler.removeCallbacks(pendingClickRunnable);    
                                pendingClickRunnable = null;    
                            }    
                                
                            lastClickTime = 0; // 重置时间防止三击    
                                
                            // 2. 执行数据挖掘和弹窗逻辑    
                            final MsgRecord record = findMsgRecordFromViewHierarchy(view); 
                            
                            // 🟢【重要修复】确保 MsgId 有效，防止打开空弹窗
                            // 也防止点击引用区域时因为挖不到主Msg而卡住
                            if (record != null && record.msgId != 0) {    
                                isDialogShowing = true;    
                                Context ctx = view.getContext();
                                // 修复：View 的 Context 可能是 ContextWrapper，需获取 Activity
                                Activity act = null;
                                if (ctx instanceof Activity) act = (Activity) ctx;
                                else act = QQCurrentEnv.INSTANCE.getActivity();

                                final Activity finalAct = act;    
                                mainHandler.post(new Runnable() {    
                                    public void run() {    
                                        try {    
                                            MsgData msgData = new MsgData(record);    
                                            showActionDialog(finalAct, msgData, null, view);    
                                        } catch (Throwable t) {    
                                            debugLog("弹窗异常: " + t.getMessage());    
                                            isDialogShowing = false;    
                                        }    
                                    }    
                                });    
                            } else {
                                // 🟢【重要修复】双击了，但没挖到有效Msg（常见于点引用区没跳出来）
                                // 降级处理：执行原功能
                                debugLog(">>> 双击但未找到有效记录，回放原功能");
                                isReplayingClick = true;
                                try {
                                    view.performClick();
                                } catch (Exception e) {}
                                new Handler().postDelayed(new Runnable() { 
                                     public void run() { isReplayingClick = false; } 
                                }, 100);
                            }    
                        } else {    
                            // >>> 单击触发 (延时回放原功能) <<<    
                            debugLog(">>> 单击检测 -> 延时等待");    
                            lastClickTime = now;    
                            lastClickViewHash = viewHash;    
                                
                            // 创建延时任务：如果超时没有第二次点击，则执行原逻辑    
                            pendingClickRunnable = new Runnable() {    
                                public void run() {    
                                    debugLog(">>> 单击超时 -> 执行原功能");    
                                    isReplayingClick = true; // 标记为回放，防止递归    
                                    try {    
                                        view.performClick();    
                                    } catch (Exception e) {    
                                    } finally {    
                                        // 稍微延迟复位标记，确保执行完成    
                                        new Handler().postDelayed(new Runnable() {     
                                            public void run() { isReplayingClick = false; }     
                                        }, 100);    
                                    }    
                                    pendingClickRunnable = null;    
                                }    
                            };    
                            // 延时 350ms    
                            mainHandler.postDelayed(pendingClickRunnable, 350);    
                        }    

                        // 拦截当前的直接调用，交由 Timer 或 双击逻辑 处理    
                        param.setResult(true);     

                    } catch (Throwable t) {    
                        debugLog("Click Hook异常: " + t.getMessage());    
                    }    
                }    
            }    
        );    
        HOOK_REGISTRY.add(h2);
        
    } catch (Throwable t) {     
        debugLog("安装失败: " + t.getMessage());    
        Toast("Hook加载失败: " + t.getMessage());    
    }
}
