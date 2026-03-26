static volatile Thread  sWatchdogThread  = null;
static volatile boolean sWatchdogEnabled = false;
static volatile Context sWatchdogCtx     = null;
static final long WATCHDOG_INTERVAL_MS   = 60L * 1000;
/**
 * 判断 MsfService 是否正在运行。
 * <p>{@code getRunningServices} 在 Android O+ 对进程自身仍然有效。</p>
 *
 * @param ctx 任意有效 {@link Context}
 * @return {@code true} 表示服务正在运行
 */
boolean isMsfServiceRunning(Context ctx) {
    try {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List services = am.getRunningServices(50);
        if (services == null) return false;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).service.getClassName().contains("MsfService")) return true;
        }
    } catch (Exception e) {}
    return false;
}


/**
 * 启动独立静态 Watchdog 守护线程。
 * <p>
 * 设计原理：运行中的 {@link Thread} 是 JVM GC Root，即使 BeanShell Interpreter
 * 实例被回收，线程本体依然存活；因此无论脚本是否处于"运行状态"，守护逻辑
 * 都能持续生效。线程每隔 {@link #WATCHDOG_INTERVAL_MS} 毫秒检查一次
 * MsfService 存活情况，若已停止则主动拉起。
 * </p>
 * <p>本方法幂等，多次调用安全。</p>
 *
 * @param ctx 任意有效 {@link Context}，内部会取 ApplicationContext 防止泄漏
 */
void startWatchdog(Context ctx) {
    if (ctx == null) return;
    sWatchdogCtx     = ctx.getApplicationContext();
    sWatchdogEnabled = true;

    if (sWatchdogThread != null && sWatchdogThread.isAlive()) {
        return;
    }

    sWatchdogThread = new Thread(new Runnable() {
        public void run() {
            traceLog("QFunWatchdog", "Watchdog 线程已启动");
            while (sWatchdogEnabled) {
                try {
                    Thread.sleep(WATCHDOG_INTERVAL_MS);
                } catch (InterruptedException e) {
                    break;
                }
                try {
                    Context c = sWatchdogCtx;
                    if (c == null) continue;
                    if (!isMsfServiceRunning(c)) {
                        traceLog("QFunWatchdog", "MsfService 未运行，正在重启");
                        traceLog("main_log", "Watchdog 检测到 MsfService 停止，正在重启");
                        tryStartMsfService(c);
                        scheduleNextBeat(c);
                    }
                } catch (Throwable t) {
                    traceLog("main_log", "Watchdog 循环异常: " + t.getMessage());
                }
            }
            traceLog("QFunWatchdog", "Watchdog 线程已退出");
        }
    });
    sWatchdogThread.setDaemon(false);
    sWatchdogThread.setName("qfun-watchdog");
    sWatchdogThread.setPriority(Thread.MIN_PRIORITY + 1);
    sWatchdogThread.start();
    traceLog("main_log", "Watchdog 线程已启动");
}

/**
 * 停止 Watchdog 线程，在 {@code unLoadPlugin} 时调用以释放资源。
 */
void stopWatchdog() {
    sWatchdogEnabled = false;
    if (sWatchdogThread != null) {
        sWatchdogThread.interrupt();
        sWatchdogThread = null;
    }
}

try {
    startWatchdog(context);
} catch (Throwable _we) {}

// 保活相关变量
private static final String COUNT_KEY = "startCount";
private static SharedPreferences prefs;

int    KEEP_ALIVE_NOTIFICATION_ID = 0x9527;
String KEEP_ALIVE_CHANNEL_ID      = "qfun_keep_alive";
String ACTION_GUARD_TRIGGERED     = "com.qfun.GUARD_TRIGGERED";
String ACTION_MSF_PING            = "com.qfun.MSF_PING";
String ACTION_KEEP_ALIVE_BEAT     = "com.qfun.KEEP_ALIVE_BEAT";
String ACTION_RESTART_MSF         = "com.qfun.RESTART_MSF";
int    BEAT_REQUEST_CODE          = 0x9530;
int    RESTART_REQUEST_CODE       = 0x9531;
long   BEAT_INTERVAL_MS           = 3L * 60 * 1000;

static volatile boolean 守护进程已启动 = false;
int guardCount = getInt("settings", "加载次数", 0) + 1;
BroadcastReceiver guardReceiver = null;

/**
 * 创建保活通知渠道（Android 8.0+ 必须）
 * <p>重要性设为 IMPORTANCE_MIN，避免在通知抽屉发出声音或振动。</p>
 *
 * @param context 任意有效 {@link Context}
 */
void createKeepAliveChannel(Context context) {
    if (Build.VERSION.SDK_INT >= 26) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(KEEP_ALIVE_CHANNEL_ID, "QFun 后台保活", NotificationManager.IMPORTANCE_MIN);
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        } catch (Exception e) {}
    }
}

/**
 * 发送或更新保活通知栏，显示守护次数和运行计时
 *
 * @param ctx 任意有效 {@link Context}
 */
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

        builder.setContentTitle("QFloatingX 运行中...  ")
               .setContentText("正在守护您的 QQ丨已为您守护 " + guardCount + " 次")
               .setSmallIcon(android.R.drawable.ic_menu_info_details)
               .setContentIntent(pendingIntent)
               .setOngoing(true)
               .setUsesChronometer(true)
               .setWhen(startTime)
               .setShowWhen(true);

        nm.notify(KEEP_ALIVE_NOTIFICATION_ID, builder.build());
    } catch (Exception e) {}
}

/**
 * 向系统发出启动 MsfService 的指令。
 * <p>
 * 从 {@link BroadcastReceiver#onReceive} 调用 {@code startForegroundService}
 * 属于系统明确豁免的场景，不受 Android 8+ 后台启动 Service 的限制。
 * </p>
 *
 * @param ctx 任意有效 {@link Context}
 */
void tryStartMsfService(Context ctx) {
    try {
        Intent i = new Intent();
        i.setClassName(ctx.getPackageName(), "com.tencent.mobileqq.msf.service.MsfService");
        i.setPackage(ctx.getPackageName());
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
        traceLog("main_log", "tryStartMsfService: 启动指令已发出");
    } catch (Throwable e) {
        traceLog("main_log", "tryStartMsfService 失败: " + e.getMessage());
    }
}

/**
 * 调度下一次 AlarmManager 心跳（ping-pong 精确单次模式）。
 * <p>
 * 使用 {@code setExactAndAllowWhileIdle}，在 Doze 模式下也能保证触发。
 * 每次触发后需再次调用本方法以续期，形成 ping-pong 链。
 * </p>
 *
 * @param ctx 任意有效 {@link Context}
 */
void scheduleNextBeat(Context ctx) {
    try {
        Intent intent = new Intent(ACTION_KEEP_ALIVE_BEAT);
        intent.setPackage(ctx.getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= 0x04000000;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, BEAT_REQUEST_CODE, intent, flags);
        AlarmManager am  = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        long triggerAt   = SystemClock.elapsedRealtime() + BEAT_INTERVAL_MS;
        if (Build.VERSION.SDK_INT >= 23)
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        else if (Build.VERSION.SDK_INT >= 19)
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        else
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
    } catch (Exception e) {
        traceLog("main_log", "scheduleNextBeat 失败: " + e.getMessage());
    }
}

/**
 * 调度延迟 3 秒后的 MsfService 重启广播。
 * <p>
 * 在 {@code onDestroy} / {@code onTaskRemoved} 中直接调用
 * {@code startForegroundService} 会因后台限制失败；改为注册一个
 * 3 秒后触发的精确 Alarm，由 {@link BroadcastReceiver#onReceive}
 * 负责实际启动，从而绕过该限制。
 * </p>
 *
 * @param ctx 任意有效 {@link Context}
 */
void scheduleDelayedRestart(Context ctx) {
    try {
        Intent intent = new Intent(ACTION_RESTART_MSF);
        intent.setPackage(ctx.getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= 0x04000000;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, RESTART_REQUEST_CODE, intent, flags);
        AlarmManager am  = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        long triggerAt   = SystemClock.elapsedRealtime() + 3000L;
        if (Build.VERSION.SDK_INT >= 23)
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        else if (Build.VERSION.SDK_INT >= 19)
            am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        else
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        traceLog("main_log", "延迟重启已调度 (3s 后)");
    } catch (Exception e) {
        traceLog("main_log", "scheduleDelayedRestart 失败: " + e.getMessage());
    }
}

static volatile boolean isMsfHooked = false;

/**
 * Hook MsfService 的 onStartCommand / onDestroy / onTaskRemoved，
 * 构建三层服务保活防线：
 * <ol>
 *   <li>强制返回 {@code START_STICKY}，让系统在服务被杀后自动重建；</li>
 *   <li>onDestroy 触发时申请 WakeLock 并通过 AlarmManager 延迟重启，
 *       绕过 Android 8+ 后台启动限制；</li>
 *   <li>onTaskRemoved 触发时同样走 AlarmManager + 直接重启双保险。</li>
 * </ol>
 */
void HookQQService() {
    if (isMsfHooked) return;

    try {
        ClassLoader loader = null;
        try { loader = classLoader; } catch (Exception e) {}
        if (loader == null) loader = context.getClassLoader();

        try {
            loader.loadClass("com.tencent.mobileqq.msf.service.MsfService");
        } catch (ClassNotFoundException e) {
            traceLog("main_log", "MsfService 类未找到，跳过 Hook");
            return;
        }

        qfunHook(android.app.Service.class, "onStartCommand", new Object[]{
            Intent.class, int.class, int.class,
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (!service.getClass().getName().contains("MsfService")) return;

                    param.setResult(android.app.Service.START_STICKY);

                    try {
                        Context ctx = (Context) service;
                        Intent intent = new Intent(ACTION_MSF_PING);
                        intent.setPackage(ctx.getPackageName());
                        ctx.sendBroadcast(intent);

                        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                        Notification.Builder builder;
                        if (Build.VERSION.SDK_INT >= 26) builder = new Notification.Builder(ctx, KEEP_ALIVE_CHANNEL_ID);
                        else builder = new Notification.Builder(ctx).setPriority(Notification.PRIORITY_MAX);

                        builder.setContentTitle("QFloatingX 守护中")
                               .setContentText("累计守护 " + guardCount + " 次")
                               .setSmallIcon(android.R.drawable.ic_menu_info_details)
                               .setOngoing(true);

                        ((android.app.Service) service).startForeground(KEEP_ALIVE_NOTIFICATION_ID, builder.build());
                    } catch (Throwable e) {}
                }
            }
        });

        qfunHook(android.app.Service.class, "onDestroy", new Object[]{
            new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (!service.getClass().getName().contains("MsfService")) return;

                    Context ctx = (Context) service;
                    traceLog("main_log", "MsfService.onDestroy 触发");

                    PowerManager.WakeLock wl = null;
                    try {
                        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qfun:msf_restart");
                        wl.acquire(10000L);
                    } catch (Throwable t) {}
                    final PowerManager.WakeLock finalWl = wl;

                    scheduleDelayedRestart(ctx);

                    try {
                        Intent restart = new Intent();
                        restart.setClassName(ctx.getPackageName(), "com.tencent.mobileqq.msf.service.MsfService");
                        restart.setPackage(ctx.getPackageName());
                        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(restart);
                        else ctx.startService(restart);
                    } catch (Throwable e) {}

                    new Thread(new Runnable() {
                        public void run() {
                            try { Thread.sleep(5000); } catch (Exception e) {}
                            try { if (finalWl != null && finalWl.isHeld()) finalWl.release(); }
                            catch (Exception e) {}
                        }
                    }).start();
                }
            }
        });

        qfunHook(android.app.Service.class, "onTaskRemoved", new Object[]{
            Intent.class,
            new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (!service.getClass().getName().contains("MsfService")) return;

                    Context ctx = (Context) service;
                    traceLog("main_log", "MsfService.onTaskRemoved 触发");

                    scheduleDelayedRestart(ctx);

                    try {
                        Intent restart = new Intent();
                        restart.setClassName(ctx.getPackageName(), "com.tencent.mobileqq.msf.service.MsfService");
                        restart.setPackage(ctx.getPackageName());
                        ctx.startService(restart);
                    } catch (Throwable e) {}
                }
            }
        });

        isMsfHooked = true;
        traceLog("main_log", "HookQQService 全部注册完成");

    } catch (Throwable e) {
        traceLog("main_log", "HookQQService 异常: " + e.getMessage());
    }
}

static volatile boolean isReceiverRegistered = false;

/**
 * 启动后台保活服务：注册广播接收器、发送通知、启动 AlarmManager 心跳链。
 * <p>
 * 广播接收器负责处理四种 Action：<br>
 * {@code ACTION_MSF_PING}：MsfService 每次 onStartCommand 后发出，用于更新通知栏；<br>
 * {@code ACTION_KEEP_ALIVE_BEAT}：AlarmManager 定时触发，检查并拉起 MsfService；<br>
 * {@code ACTION_RESTART_MSF}：onDestroy/onTaskRemoved 后的延迟重启入口；<br>
 * {@code ACTION_SCREEN_ON}：亮屏时检查，利用最宽松的前台豁免窗口。
 * </p>
 */
void startKeepAliveService() {
    try {
        Context ctx = context;

        guardCount = getInt("settings", "加载次数", 0);

        if (!isReceiverRegistered) {
            guardReceiver = new BroadcastReceiver() {
                public void onReceive(Context c, Intent i) {
                    String action = i.getAction();
                    if (action == null) return;

                    if (ACTION_MSF_PING.equals(action)) {
                        guardCount++;
                        putInt("settings", "加载次数", guardCount);
                        updateNotification(c);

                    } else if (ACTION_KEEP_ALIVE_BEAT.equals(action)) {
                        traceLog("main_log", "AlarmManager 心跳触发");
                        if (!isMsfServiceRunning(c)) {
                            traceLog("main_log", "心跳：MsfService 不在，正在重启");
                            tryStartMsfService(c);
                        }
                        scheduleNextBeat(c);

                    } else if (ACTION_RESTART_MSF.equals(action)) {
                        traceLog("main_log", "延迟重启广播触发");
                        tryStartMsfService(c);
                        scheduleNextBeat(c);

                    } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                        if (!isMsfServiceRunning(c)) {
                            traceLog("main_log", "亮屏：MsfService 不在，立即重启");
                            tryStartMsfService(c);
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_MSF_PING);
            filter.addAction(ACTION_KEEP_ALIVE_BEAT);
            filter.addAction(ACTION_RESTART_MSF);
            filter.addAction(Intent.ACTION_SCREEN_ON);

            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(guardReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(guardReceiver, filter);
            }

            isReceiverRegistered = true;
            traceLog("main_log", "guardReceiver 注册成功");
        }

        createKeepAliveChannel(ctx);
        updateNotification(ctx);

        scheduleNextBeat(ctx);
        traceLog("main_log", "AlarmManager 心跳已启动");

        startWatchdog(ctx);

        try { HookQQService(); } catch (Exception e) {}

    } catch (Throwable e) {
        traceLog("main_log", "startKeepAliveService 异常: " + e.getMessage());
    }
}

/**
 * 停止保活服务：取消 AlarmManager 心跳、撤销通知、注销广播接收器。
 * <p>
 * Watchdog 线程不受此方法影响，前台期间同样保持运行。
 * </p>
 */
void stopKeepAlive() {
    try {
        try {
            Intent beatIntent = new Intent(ACTION_KEEP_ALIVE_BEAT);
            beatIntent.setPackage(context.getPackageName());
            int flags = 0x20000000;
            if (Build.VERSION.SDK_INT >= 23) flags |= 0x04000000;
            PendingIntent pi = PendingIntent.getBroadcast(context, BEAT_REQUEST_CODE, beatIntent, flags);
            if (pi != null) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.cancel(pi);
                pi.cancel();
                traceLog("main_log", "AlarmManager 心跳已取消");
            }
        } catch (Exception e) {}

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(KEEP_ALIVE_NOTIFICATION_ID);

        if (guardReceiver != null) {
            try { context.unregisterReceiver(guardReceiver); } catch (Exception e) {}
            guardReceiver = null;
            isReceiverRegistered = false;
            traceLog("main_log", "guardReceiver 已注销");
        }
    } catch (Exception e) {}
}