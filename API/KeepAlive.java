public class KeepAlive {

    private static PowerManager.WakeLock sWakeLock = null;
    private static int sGuardCount = getInt("settings", "加载次数", 0) + 1;
    private static long sStartTime = System.currentTimeMillis();

    public static void start() {
        if (context == null) return;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (sWakeLock == null) {
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qfun:keep");
            sWakeLock.setReferenceCounted(false);
        }
        if (!sWakeLock.isHeld()) {
            sWakeLock.acquire();
        }

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("QFun 后台保活", "QFun", NotificationManager.IMPORTANCE_MIN);
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        }

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pi = null;
        if (intent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            pi = PendingIntent.getActivity(context, 0, intent, flags);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(context, "QFun 后台保活");
        } else {
            builder = new Notification.Builder(context).setPriority(Notification.PRIORITY_MIN);
        }
        builder.setContentTitle("QFloatingX 运行中...  ")
               .setContentText("正在守护您的 QQ丨已为您守护 " + sGuardCount + " 次")
               .setSmallIcon(android.R.drawable.ic_menu_info_details)
               .setContentIntent(pi)
               .setOngoing(true)
               .setUsesChronometer(true)
               .setWhen(sStartTime)
               .setShowWhen(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0x9527, builder.build());
    }

    public static void stop() {
        if (context == null) return;

        if (sWakeLock != null && sWakeLock.isHeld()) {
            sWakeLock.release();
        }
        sWakeLock = null;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(0x9527);
    }
}