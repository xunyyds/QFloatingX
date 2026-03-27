/**
 * QFun 保活服务封装类。
 * 
 * <p>基于原生 Xposed API 实现三层有效保活防线：
 * <ol>
 *   <li>Hook Service 返回值：强制返回 START_STICKY，让系统在服务被杀后自动重建</li>
 *   <li>前台服务绑定：将 MsfService 绑定为前台服务，降低被杀优先级</li>
 *   <li>AlarmManager 心跳链：在 Doze 模式下仍能触发的精确闹钟，用于检测并拉起服务</li>
 * </ol>
 * 
 * <p>调用方式：{@code KeepAlive.start()} / {@code KeepAlive.stop()}
 * 
 */
class KeepAlive {
    
    /** AlarmManager 心跳间隔：3 分钟 */
    static final long INTERVAL_BEAT = 180000L;
    
    /** 通知栏 ID */
    static final int NOTIFY_ID = 0x9527;
    
    /** AlarmManager 请求码：心跳 */
    static final int REQ_BEAT = 0x9530;
    
    /** 通知渠道 ID */
    static final String CHAN_ID = "qfun_keep";
    
    /** 广播 Action：心跳检测 */
    static final String ACT_BEAT = "qfun.beat";
    
    /** 广播 Action：MsfService 存活确认 */
    static final String ACT_PING = "qfun.ping";
    
    /** MsfService 完整类名 */
    static final String CLS_MSF = "com.tencent.mobileqq.msf.service.MsfService";
    
    /** 运行状态：是否已初始化 */
    static volatile boolean sInited = false;
    
    /** 运行状态：是否已 Hook */
    static volatile boolean sHooked = false;
    
    /** 运行状态：广播接收器是否已注册 */
    static volatile boolean sRegistered = false;
    
    /** 广播接收器实例 */
    static BroadcastReceiver sReceiver;
    
    /** 守护计数器 */
    static int sGuardCount;
    
    /** 启动时间戳 */
    static long sStartTime = System.currentTimeMillis();
    
    /**
     * 判断 MsfService 是否正在运行。
     *
     * @param ctx 任意有效 Context
     * @return true 表示服务正在运行
     */
    static boolean isMsfRunning(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List services = am.getRunningServices(50);
            if (services == null) return false;
            
            for (int i = 0; i < services.size(); i++) {
                Object rsi = services.get(i);
                if (rsi == null) continue;
                
                Object service = rsi.getClass().getField("service").get(rsi);
                if (service == null) continue;
                
                String className = (String) service.getClass().getMethod("getClassName").invoke(service);
                if (className != null && className.contains("MsfService")) {
                    return true;
                }
            }
        } catch (Exception e) {}
        return false;
    }
    
    /**
     * 启动 MsfService。
     *
     * @param ctx 任意有效 Context
     */
    static void startMsf(Context ctx) {
        try {
            Intent i = new Intent();
            i.setClassName(ctx.getPackageName(), CLS_MSF);
            i.setPackage(ctx.getPackageName());
            
            if (Build.VERSION.SDK_INT >= 26) {
                ctx.startForegroundService(i);
            } else {
                ctx.startService(i);
            }
        } catch (Throwable e) {
            traceLog("KeepAlive", "[startMsf] 失败: " + e.getMessage());
        }
    }
    
    /**
     * 调度 AlarmManager 精确闹钟。
     *
     * @param ctx 任意有效 Context
     * @param action 广播 Action
     * @param reqCode 请求码
     * @param delayMs 延迟毫秒数
     */
    static void scheduleAlarm(Context ctx, String action, int reqCode, long delayMs) {
        try {
            Intent intent = new Intent(action);
            intent.setPackage(ctx.getPackageName());
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent, flags);
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            
            long triggerAt = SystemClock.elapsedRealtime() + delayMs;
            
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else if (Build.VERSION.SDK_INT >= 19) {
                am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            }
        } catch (Exception e) {
            traceLog("KeepAlive", "[scheduleAlarm] 失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消 AlarmManager 闹钟。
     *
     * @param ctx 任意有效 Context
     * @param action 广播 Action
     * @param reqCode 请求码
     */
    static void cancelAlarm(Context ctx, String action, int reqCode) {
        try {
            Intent intent = new Intent(action);
            intent.setPackage(ctx.getPackageName());
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent, flags);
            if (pi != null) {
                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                am.cancel(pi);
                pi.cancel();
            }
        } catch (Exception e) {}
    }
    
    /**
     * 创建通知渠道（Android O+ 必需）。
     *
     * @param ctx 任意有效 Context
     */
    static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHAN_ID, "QFun 后台保活", NotificationManager.IMPORTANCE_MIN);
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        } catch (Exception e) {
            traceLog("KeepAlive", "[createChannel] 失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新或显示保活通知栏。
     *
     * @param ctx 任意有效 Context
     */
    static void updateNotify(Context ctx) {
        try {
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(QQpackage);
            if (intent == null) return;
            
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, flags);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new Notification.Builder(ctx, CHAN_ID);
            } else {
                builder = new Notification.Builder(ctx).setPriority(Notification.PRIORITY_MIN);
            }
            
            builder.setContentTitle("QFloatingX 运行中...  ")
                   .setContentText("正在守护您的 QQ丨已为您守护 " + sGuardCount + " 次")
                   .setSmallIcon(android.R.drawable.ic_menu_info_details)
                   .setContentIntent(pi)
                   .setOngoing(true)
                   .setUsesChronometer(true)
                   .setWhen(sStartTime)
                   .setShowWhen(true);
            
            nm.notify(NOTIFY_ID, builder.build());
        } catch (Exception e) {
            traceLog("KeepAlive", "[updateNotify] 失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用原生 Xposed API Hook MsfService。
     * 
     * <p>Hook 对象添加到 {@code hookloveList}，可通过 {@code 卸载loveHook()} 卸载。
     *
     * @param loader 类加载器
     */
    static void hookMsf(ClassLoader loader) {
        if (sHooked) return;
        
        try {
            Class<?> msfClass = Class.forName(CLS_MSF, true, loader);
            
            // Hook onStartCommand
            Method startCmdMethod = Service.class.getDeclaredMethod("onStartCommand", Intent.class, int.class, int.class);
            Object unhookStart = XposedBridge.hookMethod(startCmdMethod, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (service == null) return;
                    
                    String clsName = service.getClass().getName();
                    if (!clsName.contains("MsfService")) return;
                    
                    param.setResult(Service.START_STICKY);
                    
                    try {
                        Context ctx = (Context) service;
                        ctx.sendBroadcast(new Intent(ACT_PING).setPackage(ctx.getPackageName()));
                        
                        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                        Notification.Builder builder;
                        
                        if (Build.VERSION.SDK_INT >= 26) {
                            builder = new Notification.Builder(ctx, CHAN_ID);
                        } else {
                            builder = new Notification.Builder(ctx).setPriority(Notification.PRIORITY_MIN);
                        }
                        
                        builder.setContentTitle("QFun 守护中")
                               .setContentText("累计守护 " + sGuardCount + " 次")
                               .setSmallIcon(android.R.drawable.ic_menu_info_details)
                               .setOngoing(true);
                        
                        ((Service) service).startForeground(NOTIFY_ID, builder.build());
                        
                    } catch (Throwable e) {}
                }
            });
            
            if (unhookStart != null) {
                hookloveList.add(unhookStart);
            }
            
            // Hook onDestroy
            Method destroyMethod = Service.class.getDeclaredMethod("onDestroy");
            Object unhookDestroy = XposedBridge.hookMethod(destroyMethod, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object service = param.thisObject;
                    if (service == null) return;
                    
                    String clsName = service.getClass().getName();
                    if (!clsName.contains("MsfService")) return;
                    
                    Context ctx = (Context) service;
                    startMsf(ctx);
                    scheduleAlarm(ctx, ACT_BEAT, REQ_BEAT, 5000L);
                }
            });
            
            if (unhookDestroy != null) {
                hookloveList.add(unhookDestroy);
            }
            
            sHooked = true;
            
        } catch (Throwable e) {
            traceLog("KeepAlive", "[hookMsf] 异常: " + e.getMessage());
        }
    }
    
    /**
     * 注册广播接收器。
     *
     * @param ctx 任意有效 Context
     */
    static void registerReceiver(Context ctx) {
        if (sRegistered) return;
        
        sGuardCount = getInt("settings", "加载次数", 0);
        
        sReceiver = new BroadcastReceiver() {
            public void onReceive(Context c, Intent i) {
                String action = i.getAction();
                if (action == null) return;
                
                if (ACT_PING.equals(action)) {
                    sGuardCount++;
                    putInt("settings", "加载次数", sGuardCount);
                    updateNotify(c);
                    
                } else if (ACT_BEAT.equals(action)) {
                    ThreadPool.execute(new Runnable() {
                        public void run() {
                            if (!isMsfRunning(c)) {
                                startMsf(c);
                            }
                            scheduleAlarm(c, ACT_BEAT, REQ_BEAT, INTERVAL_BEAT);
                        }
                    });
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACT_PING);
        filter.addAction(ACT_BEAT);
        
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                ctx.registerReceiver(sReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                ctx.registerReceiver(sReceiver, filter);
            }
            
            sRegistered = true;
            
        } catch (Exception e) {
            traceLog("KeepAlive", "[registerReceiver] 失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动保活服务（入口方法）。
     */
    static void start() {
        if (sInited) return;
        
        // 优先使用 context，其次 getNowActivity()
        Context ctx = null;
        try {
            if (context != null) {
                ctx = context;
            }
        } catch (Exception e) {}
        
        if (ctx == null) {
            Activity act = getNowActivity();
            if (act != null) {
                ctx = act;
            }
        }
        
        if (ctx == null) {
            traceLog("KeepAlive", "[start] 失败: 无法获取 Context");
            return;
        }
        
        registerReceiver(ctx);
        createChannel(ctx);
        updateNotify(ctx);
        scheduleAlarm(ctx, ACT_BEAT, REQ_BEAT, INTERVAL_BEAT);
        
        ClassLoader loader = null;
        try {
            loader = classLoader;
        } catch (Exception e) {}
        if (loader == null) {
            loader = ctx.getClassLoader();
        }
        
        hookMsf(loader);
        
        sInited = true;
    }
    
    /**
     * 停止保活服务。
     */
    static void stop() {
        ThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    Context ctx = null;
                    try {
                        if (context != null) {
                            ctx = context;
                        }
                    } catch (Exception e) {}
                    
                    if (ctx == null) {
                        Activity act = getNowActivity();
                        if (act != null) {
                            ctx = act;
                        }
                    }
                    
                    if (ctx != null) {
                        cancelAlarm(ctx, ACT_BEAT, REQ_BEAT);
                        
                        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                        nm.cancel(NOTIFY_ID);
                    }
                    
                    if (sReceiver != null) {
                        try {
                            if (ctx != null) {
                                ctx.unregisterReceiver(sReceiver);
                            }
                        } catch (Exception e) {}
                        sReceiver = null;
                        sRegistered = false;
                    }
                    
                    sInited = false;
                    sHooked = false;
                    
                } catch (Exception e) {
                    traceLog("KeepAlive", "[stop] 异常: " + e.getMessage());
                }
            }
        });
    }
}
