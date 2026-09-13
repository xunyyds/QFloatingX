final int KEEP_NOTIFY_ID = 0x9527;
boolean keepNotifyChannelCreated = false;

void toggleKeepNotify(boolean show) {
    android.content.Context ctx = context != null ? context : getNowActivity();
    if (ctx == null) return;
    android.app.NotificationManager nm = (android.app.NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
    if (show && getBoolean("settings", "常驻通知", false)) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !keepNotifyChannelCreated) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel("qfun_keep", "保活", android.app.NotificationManager.IMPORTANCE_LOW);
            ch.enableLights(false); ch.setShowBadge(false); ch.setSound(null, null);
            nm.createNotificationChannel(ch);
            keepNotifyChannelCreated = true;
        }
        android.content.Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launchIntent == null) return;
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(ctx, 0, launchIntent, android.app.PendingIntent.FLAG_IMMUTABLE | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        android.app.Notification.Builder builder = (android.os.Build.VERSION.SDK_INT >= 26) ?
            new android.app.Notification.Builder(ctx, "qfun_keep") :
            new android.app.Notification.Builder(ctx).setPriority(android.app.Notification.PRIORITY_LOW);
        builder.setContentTitle("QFloatingX 运行中...  ")
               .setContentText("正在守护您的 QQ丨已为您守护 " + getInt("settings", "加载次数", 0) + " 次")
               .setSmallIcon(android.R.drawable.ic_menu_info_details)
               .setContentIntent(pi)
               .setOngoing(true)
               .setUsesChronometer(true)
               .setWhen(startTime)
               .setShowWhen(true);
        nm.notify(KEEP_NOTIFY_ID, builder.build());
    } else {
        nm.cancel(KEEP_NOTIFY_ID);
    }
}