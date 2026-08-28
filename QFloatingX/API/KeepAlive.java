import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import com.tencent.mobileqq.msf.service.MsfService;

void toggleKeepNotify(boolean show) {
    android.content.Context ctx = context != null ? context : getNowActivity();
    if (ctx == null) return;
    android.app.NotificationManager nm = (android.app.NotificationManager) ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
    if (show && getBoolean("settings", "常驻通知", false)) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel ch = new android.app.NotificationChannel("qfun_keep", "保活", android.app.NotificationManager.IMPORTANCE_LOW);
            ch.enableLights(false); ch.setShowBadge(false); ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
        android.content.Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
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
        nm.notify(0x9527, builder.build());
    } else {
        nm.cancel(0x9527);
    }
}
