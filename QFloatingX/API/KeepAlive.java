void toggleKeepNotify(boolean show) {
    Context ctx = context != null ? context : getNowActivity();
    if (ctx == null) return;
    NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    if (show) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("qfun_keep", "保活", NotificationManager.IMPORTANCE_LOW);
            ch.enableLights(false); ch.setShowBadge(false); ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = (Build.VERSION.SDK_INT >= 26) ?
            new Notification.Builder(ctx, "qfun_keep") :
            new Notification.Builder(ctx).setPriority(Notification.PRIORITY_LOW);
        builder.setContentTitle("QFloatingX 运行中...  ")
               .setContentText("正在守护您的 QQ丨已为您守护 " + KeepAlive.guardCount + " 次")
               .setSmallIcon(android.R.drawable.ic_menu_info_details)
               .setContentIntent(pi)
               .setOngoing(true)
               .setUsesChronometer(true)
               .setWhen(startTime)
               .setShowWhen(true);
        nm.notify(0x9527, builder.build());
    } else {
        nm.cancel(0x9527);
    }
}

class KeepAlive {
    static volatile boolean inited;
    static android.content.BroadcastReceiver receiver;
    static int guardCount;

    static void startMsf(android.content.Context ctx) {
        android.content.Intent i = new android.content.Intent(ctx, com.tencent.mobileqq.msf.service.MsfService.class)
                .setPackage(ctx.getPackageName());
        if (android.os.Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    static void scheduleBeat(android.content.Context ctx) {
        long jitter = (long)(Math.random() * 20000 - 10000);
        long delay = 60000 + (jitter > 0 ? jitter : 0);
        android.content.Intent intent = new android.content.Intent("qfun.beat").setPackage(ctx.getPackageName());
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(ctx, 0x9530, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        android.app.AlarmManager am = (android.app.AlarmManager) ctx.getSystemService(android.content.Context.ALARM_SERVICE);
        long trigger = android.os.SystemClock.elapsedRealtime() + delay;
        if (android.os.Build.VERSION.SDK_INT >= 23)
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
        else am.setExact(android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
    }

    static void hookCore() {
            java.lang.reflect.Method startCmd = android.app.Service.class.getDeclaredMethod(
                    "onStartCommand", android.content.Intent.class, int.class, int.class);
            Object unhookStart = de.robv.android.xposed.XposedBridge.hookMethod(startCmd,
                    new de.robv.android.xposed.XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.thisObject == null ||
                                    !param.thisObject.getClass().getName().contains("MsfService")) return;
                            param.setResult(android.app.Service.START_STICKY);
                            android.content.Context ctx = (android.content.Context) param.thisObject;
                            ctx.sendBroadcast(new android.content.Intent("qfun.ping").setPackage(ctx.getPackageName()));
                            java.lang.reflect.Field initedField = param.thisObject.getClass().getField("inited");
                            if (!(boolean) initedField.get(param.thisObject)) {
                                java.lang.reflect.Method init = param.thisObject.getClass()
                                        .getDeclaredMethod("serviceInit", android.content.Context.class, boolean.class);
                                init.setAccessible(true);
                                init.invoke(param.thisObject, ctx, false);
                            }
                        }
                    });
            if (unhookStart != null) hookloveList.add(unhookStart);

            java.lang.reflect.Method destroy = android.app.Service.class.getDeclaredMethod("onDestroy");
            Object unhookDestroy = de.robv.android.xposed.XposedBridge.hookMethod(destroy,
                    new de.robv.android.xposed.XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.thisObject == null ||
                                    !param.thisObject.getClass().getName().contains("MsfService")) return;
                            android.content.Context ctx = (android.content.Context) param.thisObject;
                            guardCount++;
                            putInt("settings", "加载次数", guardCount);
                            startMsf(ctx);
                            scheduleBeat(ctx);
                        }
                    });
            if (unhookDestroy != null) hookloveList.add(unhookDestroy);

            java.lang.reflect.Method exitMethod = System.class.getDeclaredMethod("exit", int.class);
            Object unhookExit = de.robv.android.xposed.XposedBridge.hookMethod(exitMethod,
                    new de.robv.android.xposed.XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) { param.setResult(null); }
                    });
            if (unhookExit != null) hookloveList.add(unhookExit);

            java.lang.reflect.Method runtimeExit = Runtime.class.getDeclaredMethod("exit", int.class);
            Object unhookRuntime = de.robv.android.xposed.XposedBridge.hookMethod(runtimeExit,
                    new de.robv.android.xposed.XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) { param.setResult(null); }
                    });
            if (unhookRuntime != null) hookloveList.add(unhookRuntime);

            java.lang.reflect.Method killProc = Process.class.getDeclaredMethod("killProcess", int.class);
            Object unhookKill = de.robv.android.xposed.XposedBridge.hookMethod(killProc,
                    new de.robv.android.xposed.XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ((int) param.args[0] == Process.myPid()) param.setResult(null);
                        }
                    });
            if (unhookKill != null) hookloveList.add(unhookKill);
    }

    static void start() {
        if (inited) return;
        android.content.Context ctx = context != null ? context : getNowActivity();
        if (ctx == null) return;
        toggleKeepNotify(true);
        guardCount = getInt("settings", "加载次数", 0);
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction("qfun.ping");
        filter.addAction("qfun.beat");
        receiver = new android.content.BroadcastReceiver() {
            public void onReceive(android.content.Context c, android.content.Intent i) {
                if ("qfun.ping".equals(i.getAction())) {
                    guardCount++;
                    putInt("settings", "加载次数", guardCount);
                    toggleKeepNotify(true);
                } else if ("qfun.beat".equals(i.getAction())) {
                    new Thread(() -> {
                        try {
                            Object core = com.tencent.mobileqq.msf.service.MsfService.class.getField("core").get(null);
                            boolean ok = core != null &&
                                    (boolean) com.tencent.mobileqq.msf.service.MsfService.class.getField("inited").get(null);
                            if (!ok) startMsf(c);
                        } catch (Exception ignored) {}
                        scheduleBeat(c);
                    }).start();
                }
            }
        };
        ctx.registerReceiver(receiver, filter,
                android.os.Build.VERSION.SDK_INT >= 33 ? android.content.Context.RECEIVER_EXPORTED : 0);
        scheduleBeat(ctx);
        hookCore();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            android.content.Context c = context != null ? context : getNowActivity();
            if (c != null) {
                startMsf(c);
                scheduleBeat(c);
            }
        }));
        inited = true;
    }
}