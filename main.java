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


String iconBase        = pluginPath + "/API/icon";
String iconPath        = new java.io.File(iconBase + ".png").exists() ? iconBase + ".png" :
                        new java.io.File(iconBase + ".gif").exists() ? iconBase + ".gif" :
                        iconBase + ".png";
String closeIconPath   = pluginPath + "/API/closeIcon.png";

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
import java.lang.reflect.Field;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.text.*;
import android.text.method.*;

String rootPath        = pluginPath + "/API/";
String htmlPath        = pluginPath + "/HTML/";
String configPath      = pluginPath + "/config/";
String qq              = myUin;
final String logPath   = pluginPath + "/Log/";
long startTime         = System.currentTimeMillis();
ExecutorService ThreadPool = null;


final String[] coreFiles = {
    rootPath + "import.java",
    rootPath + "core/Core.java",
    rootPath + "core/UiTools.java"
};
final String[] parallelFiles = {
    rootPath + "widget/ColorPicker.java",
    rootPath + "core/Updater.java",
    rootPath + "core/Entry.java",
    rootPath + "window/FloatingWindow.java",
    rootPath + "window/Menu.java",
    rootPath + "window/Dialogs.java",
    rootPath + "window/Lists.java",
    rootPath + "window/Markdown.java",
    rootPath + "settings/Settings.java",
    rootPath + "settings/SettingsPages.java",
    rootPath + "settings/SettingsSearch.java",
    rootPath + "settings/Backup.java",
    rootPath + "feature/Stats.java",
    rootPath + "feature/MockLocation.java",
    rootPath + "feature/DoubleMsg.java",
    rootPath + "plugin/RuntimeStatus.java",
    rootPath + "browser/Browser.java",
    rootPath + "browser/QZone.java",
    rootPath + "feature/Proto.java",
    rootPath + "plugin/HotPlug.java",
    rootPath + "plugin/KeepAlive.java"
};


volatile long apiLoadCostTime = -1;
static ThreadLocal dateFormatHolder = new ThreadLocal();

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

void traceLog(String name, String txt) {
    String 文件名 = name;
    try {
        if (!name.endsWith(".txt")) {
            文件名 = name + ".txt";
        }
        logDedup("/Log/" + 文件名, 文件名, getTime() + "    " + txt);
    } catch (Exception e) {
        log("/Log/" + 文件名, "    " + txt);
    }
}

void traceLog(String txt) {
    try {
        logDedup("/Log/api_log.txt", "api_log.txt", getTime() + "    " + txt);
    } catch (Throwable e) {
        log("/Log/api_log.txt", "    " + txt);
    }
}

Map logDedupState = new ConcurrentHashMap();
Object logDedupLock = new Object();
long logDedupWindowMs = 3000L;

void logDedup(String logPath, String logName, String line) {
    if (line == null) return;
    String content = line;
    int markIdx = content.indexOf("    ");
    if (markIdx >= 0) content = content.substring(markIdx + 4);
    String key = logName + "\u0000" + content;
    long now = System.currentTimeMillis();
    synchronized (logDedupLock) {
        Object[] prev = (Object[]) logDedupState.get(key);
        if (prev != null) {
            String prevPath = (String) prev[0];
            int prevCount = ((Integer) prev[1]).intValue();
            long prevTime = ((Long) prev[2]).longValue();
            if (now - prevTime < logDedupWindowMs && rewriteLastLogLine(prevPath, content, prevCount + 1)) {
                logDedupState.put(key, new Object[]{prevPath, Integer.valueOf(prevCount + 1), Long.valueOf(now)});
                return;
            }
            logDedupState.remove(key);
        }
        try {
            log(logPath, line);
        } catch (Throwable e) {
            return;
        }
        logDedupState.put(key, new Object[]{logPath, Integer.valueOf(1), Long.valueOf(now)});
    }
}

void flushAllDedupCounters() {
    synchronized (logDedupLock) {
        logDedupState.clear();
    }
}

boolean rewriteLastLogLine(String logPath, String content, int count) {
    File file = new File(pluginPath + logPath);
    if (!file.exists()) return false;
    long size = file.length();
    if (size <= 0) return false;
    String suffix = count > 1 ? (" *" + count) : "";
    String newLine = getTime() + "    " + content + suffix;
    RandomAccessFile raf = null;
    try {
        raf = new RandomAccessFile(file, "rw");
        int tailLen = (int) Math.min(size, 4096L);
        long tailStart = size - tailLen;
        byte[] buf = new byte[tailLen];
        raf.seek(tailStart);
        raf.readFully(buf);
        int end = tailLen;
        if (end > 0 && buf[end - 1] == '\n') end--;
        if (end > 0 && buf[end - 1] == '\r') end--;
        int lineStart = 0;
        for (int i = end - 1; i >= 0; i--) {
            if (buf[i] == '\n') { lineStart = i + 1; break; }
        }
        String last = new String(buf, lineStart, end - lineStart, "UTF-8");
        int m = last.indexOf("    ");
        String lastContent = m >= 0 ? last.substring(m + 4) : last;
        int star = lastContent.lastIndexOf(" *");
        if (star > 0) lastContent = lastContent.substring(0, star);
        if (!content.equals(lastContent)) return false;
        long keep = tailStart + lineStart;
        raf.setLength(keep);
        raf.seek(keep);
        raf.write((newLine + "\n").getBytes("UTF-8"));
        return true;
    } catch (Throwable e) {
        return false;
    } finally {
        if (raf != null) try { raf.close(); } catch (Throwable closeErr) { log("/Log/main_log.txt", getTime() + "    [logDedup] 关闭文件失败: " + closeErr); }
    }
}

void initThreadPool() {
    if (ThreadPool != null) {
        try {
            ThreadPool.submit(new Runnable() { public void run() {} }).get(100, TimeUnit.MILLISECONDS);
            traceLog("main_log", "[initThreadPool] 线程池存活，直接使用");
            return;
        } catch (Exception e) {
            traceLog("main_log", "[initThreadPool] 线程池重建: " + e.getMessage());
            ThreadPool = null;
        }
    }

    int threadPriority = 5;
    try {
        String sp = getString("settings", "thread_pool_priority", "5");
        if (!sp.isEmpty()) threadPriority = Math.max(1, Math.min(10, Integer.parseInt(sp)));
    } catch (Throwable e) { traceLog("main_log", "[initThreadPool] 异常: " + e); }

    int queueCapacity = 50;
    try {
        String sp = getString("settings", "thread_pool_queue_capacity", "50");
        if (!sp.isEmpty()) queueCapacity = Math.max(10, Integer.parseInt(sp));
    } catch (Throwable e) { traceLog("main_log", "[initThreadPool] 异常: " + e); }

    long keepAliveTime = 30;
    try {
        String sp = getString("settings", "thread_pool_keep_alive", "30");
        if (!sp.isEmpty()) keepAliveTime = Math.max(5, Long.parseLong(sp));
    } catch (Throwable e) { traceLog("main_log", "[initThreadPool] 异常: " + e); }

    String threadNamePrefix = getString("settings", "thread_pool_name_prefix", "ovoWorker");

    int cpuCores = Runtime.getRuntime().availableProcessors();
    int corePoolSize = Math.max(2, Math.min(cpuCores, 8));
    int maxPoolSize = Math.min(corePoolSize * 2, 16);

    traceLog("main_log", "[initThreadPool] 初始化线程池: 核心=" + corePoolSize);

    ThreadFactory threadFactory = new ThreadFactory() {
        int threadCount = 1;
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
        int stage = 0;
        try {
            for (int i = 0; i < coreFiles.length; i++) {
                loadJava(coreFiles[i]);
            }
            traceLog("main_log", "[apiLoad] 核心文件加载完成");

            stage = 2;
            final CountDownLatch latch = new CountDownLatch(parallelFiles.length);
            for (int i = 0; i < parallelFiles.length; i++) {
                final String currentFile = parallelFiles[i];
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            loadJava(currentFile);
                        } catch (Throwable e) {
                            traceLog("main_log", "[apiLoad] 非核心API加载失败：" + currentFile + " " + e);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }

            latch.await(10000, TimeUnit.MILLISECONDS);
            apiLoadCostTime = System.currentTimeMillis() - startTime;
            traceLog("main_log", "[apiLoad] 所有API加载完成，耗时：" + apiLoadCostTime + "ms");

            uiHandler.post(new Runnable() {
                public void run() {
                    原神启动();
                }
            });

        } catch (Throwable e) {
            traceLog("main_log", "[apiLoad] API加载异常 stage=" + stage + " : " + e);
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                traceLog("main_log", "[apiLoad] 堆栈: " + sw.toString());
            } catch (Throwable ignore) {}
        }
    }
});

String currentPackageName = context.getPackageName();


volatile boolean 应用前台状态 = false;
volatile boolean 非UI初始化完成 = false;
volatile boolean UI初始化完成 = false;


static volatile int resumedActivityCount = 0;

Handler uiHandler = new Handler(Looper.getMainLooper());
Handler backgroundHandler = new Handler(Looper.getMainLooper());

Activity 最后Activity = null;
boolean dialogVisible = false;
Activity activity = null;
volatile boolean Hook已调用 = false;

Activity safeActivity() {
    Activity a = getNowActivity();
    if (a == null) a = 最后Activity;
    return a;
}


void checkAndUpdateForegroundState(final Activity activity) {
    if (resumedActivityCount <= 0) return;

    if (非UI初始化完成 && !UI初始化完成) {
        uiHandler.post(new Runnable() {
            public void run() {
                前台初始化(activity);
            }
        });
    }

    if (!应用前台状态) {
        
        应用前台状态 = true;
        最后Activity = activity;
		if (getBoolean("settings", "常驻通知", false)) {
        toggleKeepNotify(false);
        }

        if (UI初始化完成 && !悬浮窗可见() && getBoolean("settings", "开关", false)) {
            启动悬浮窗(activity);
        }
    }
}


void checkAndUpdateBackgroundState() {
    if (resumedActivityCount > 0) return;

    backgroundHandler.postDelayed(new Runnable() {
        public void run() {
            if (resumedActivityCount > 0) return;
            if (应用前台状态) {
                
                应用前台状态 = false;
                if (悬浮窗可见()) 停止悬浮窗();
                if (getBoolean("settings", "常驻通知", false)) {
                toggleKeepNotify(true);
                }
            }
        }
    }, 400);
}

static final ConcurrentHashMap METHOD_CACHE = new ConcurrentHashMap();
static final ConcurrentHashMap FIELD_CACHE = new ConcurrentHashMap();
List hookloveList = new ArrayList();
Map hookTagMap = new ConcurrentHashMap();

String typesSignature(Class[] types) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < types.length; i++) {
        if (types[i] == null) return null;
        sb.append(types[i].getName());
        if (i < types.length - 1) sb.append(",");
    }
    return sb.toString();
}

Method getCachedMethod(Class clazz, String methodName, Class[] paramTypes) {
    if (clazz == null || methodName == null) return null;
    Class[] types = paramTypes != null ? paramTypes : new Class[0];
    String sig = typesSignature(types);
    if (sig == null) return null;
    String key = System.identityHashCode(clazz) + "#" + methodName + "(" + sig + ")";
    Object cached = METHOD_CACHE.get(key);
    if (cached instanceof Method) return (Method) cached;
    Method m = findCompatibleMethod(clazz, methodName, types);
    if (m != null) METHOD_CACHE.put(key, m);
    return m;
}

Method findCompatibleMethod(Class clazz, String methodName, Class[] paramTypes) {
    if (clazz == null || methodName == null) return null;
    Class[] types = paramTypes != null ? paramTypes : new Class[0];
    Class c = clazz;
    while (c != null) {
        Method m = pickBestOverload(c, methodName, types);
        if (m != null) return m;
        c = c.getSuperclass();
    }
    return null;
}

Method pickBestOverload(Class clazz, String methodName, Class[] paramTypes) {
    Method[] all = null;
    try { all = clazz.getDeclaredMethods(); } catch (Throwable e) { return null; }
    if (all == null) return null;
    Method best = null;
    int bestScore = Integer.MAX_VALUE;
    for (int i = 0; i < all.length; i++) {
        Method cand = all[i];
        if (!methodName.equals(cand.getName())) continue;
        int score = overloadScore(cand.getParameterTypes(), paramTypes);
        if (score < 0) continue;
        if (score < bestScore) { bestScore = score; best = cand; }
    }
    if (best != null) {
        try { best.setAccessible(true); } catch (Throwable e) { traceLog("main_log", "[pickBestOverload] setAccessible 失败: " + e); }
    }
    return best;
}

int overloadScore(Class[] actual, Class[] expected) {
    if (actual.length != expected.length) return -1;
    int score = 0;
    for (int i = 0; i < actual.length; i++) {
        Class a = actual[i];
        Class e = expected[i];
        if (e == null) { score += 50; continue; }
        if (a == e) continue;
        if (a.isAssignableFrom(e)) { score += 1; continue; }
        Class boxed = boxPrimitive(a);
        if (boxed != null && boxed == e) { score += 3; continue; }
        return -1;
    }
    return score;
}

Class boxPrimitive(Class p) {
    if (p == null || !p.isPrimitive()) return null;
    if (p == int.class) return Integer.class;
    if (p == long.class) return Long.class;
    if (p == boolean.class) return Boolean.class;
    if (p == double.class) return Double.class;
    if (p == float.class) return Float.class;
    if (p == short.class) return Short.class;
    if (p == byte.class) return Byte.class;
    if (p == char.class) return Character.class;
    return null;
}

Object callRuntimeService(Object appInterface, Class serviceClass) {
    if (appInterface == null || serviceClass == null) return null;
    Method two = getCachedMethod(appInterface.getClass(), "getRuntimeService", new Class[]{Class.class, String.class});
    if (two != null) {
        try { return two.invoke(appInterface, new Object[]{serviceClass, ""}); } catch (Exception e) { traceLog("main_log", "[callRuntimeService] 两参调用失败: " + e); }
    }
    Method one = getCachedMethod(appInterface.getClass(), "getRuntimeService", new Class[]{Class.class});
    if (one != null) {
        try { return one.invoke(appInterface, new Object[]{serviceClass}); } catch (Exception e) { traceLog("main_log", "[callRuntimeService] 单参调用失败: " + e); }
    }
    return null;
}

Field getCachedField(Class clazz, String fieldName) {
    if (clazz == null || fieldName == null) return null;
    String key = System.identityHashCode(clazz) + "#f:" + fieldName;
    Object cached = FIELD_CACHE.get(key);
    if (cached instanceof Field) return (Field) cached;
    Field f = qfunCachedField(clazz, fieldName);
    if (f != null) FIELD_CACHE.put(key, f);
    return f;
}

Field qfunCachedField(Class clazz, String fieldName) {
    Class c = clazz;
    while (c != null) {
        try {
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch (Throwable e) {
            c = c.getSuperclass();
        }
    }
    return null;
}

Object fieldGet(Object target, String fieldName) {
    if (target == null || fieldName == null) return null;
    boolean isStatic = target instanceof Class;
    Class owner = isStatic ? (Class) target : target.getClass();
    try {
        Field field = getCachedField(owner, fieldName);
        if (field == null) return null;
        return field.get(isStatic ? null : target);
    } catch (IllegalArgumentException e) {
        try {
            String key = System.identityHashCode(owner) + "#f:" + fieldName;
            FIELD_CACHE.remove(key);
            Field fresh = qfunCachedField(owner, fieldName);
            if (fresh == null) return null;
            FIELD_CACHE.put(key, fresh);
            return fresh.get(isStatic ? null : target);
        } catch (Throwable t) {
            traceLog("main_log", "[fieldGet] " + fieldName + " 重查失败: " + t);
            return null;
        }
    } catch (Exception e) {
        traceLog("main_log", "[fieldGet] " + fieldName + " 失败: " + e);
        return null;
    }
}

Object invokeCached(Object target, String methodName, Class[] paramTypes, Object[] params) {
    if (target == null || methodName == null) return null;
    Class owner = (target instanceof Class) ? (Class) target : target.getClass();
    Method method = getCachedMethod(owner, methodName, paramTypes);
    if (method == null) return null;
    try {
        return method.invoke((target instanceof Class) ? null : target, params);
    } catch (Exception e) {
        traceLog("main_log", "[invokeCached] " + methodName + " 失败: " + e);
        return null;
    }
}

Method findMethodByCount(Class clazz, String methodName, int paramCount) {
    if (clazz == null || methodName == null) return null;
    Class c = clazz;
    while (c != null) {
        Method best = null;
        try {
            Method[] all = c.getDeclaredMethods();
            for (int i = 0; i < all.length; i++) {
                if (!methodName.equals(all[i].getName())) continue;
                if (all[i].getParameterTypes().length != paramCount) continue;
                try { all[i].setAccessible(true); } catch (Throwable ignore) {}
                if (best == null) best = all[i];
            }
        } catch (Throwable e) {
            traceLog("main_log", "[findMethodByCount] " + c.getName() + " 失败: " + e);
        }
        if (best != null) return best;
        c = c.getSuperclass();
    }
    return null;
}

Method[] findMethodsByName(Class clazz, String methodName) {
    if (clazz == null || methodName == null) return new Method[0];
    List hit = new ArrayList();
    Class c = clazz;
    while (c != null) {
        try {
            Method[] all = c.getDeclaredMethods();
            for (int i = 0; i < all.length; i++) {
                if (methodName.equals(all[i].getName())) hit.add(all[i]);
            }
        } catch (Throwable e) {
            traceLog("main_log", "[findMethodsByName] " + c.getName() + " 失败: " + e);
        }
        c = c.getSuperclass();
    }
    return (Method[]) hit.toArray(new Method[0]);
}

int hookAll(String tag, Class clazz, String methodName, XC_MethodHook callback) {
    if (callback == null || clazz == null) return 0;
    Method[] methods = findMethodsByName(clazz, methodName);
    for (int i = 0; i < methods.length; i++) {
        hook(tag, methods[i], callback);
    }
    return methods.length;
}

void hook(Class clazz, String methodName, Class[] paramTypes, XC_MethodHook callback) {
    Method method = getCachedMethod(clazz, methodName, paramTypes);
    if (method == null) return;
    hook(null, method, callback);
}

void hookByName(String tag, Class clazz, String methodName, int paramCount, XC_MethodHook callback) {
    Method method = findMethodByCount(clazz, methodName, paramCount);
    if (method == null) return;
    hook(tag, method, callback);
}

void hook(String tag, Method method, XC_MethodHook callback) {
    if (method == null || callback == null) return;
    try {
        if (!method.isAccessible()) method.setAccessible(true);
    } catch (Exception e) {
        traceLog("main_log", "[hook] " + method.getName() + " setAccessible 失败: " + e);
    }
    try {
        Object unhook = XposedBridge.hookMethod(method, callback);
        hookloveList.add(unhook);
        if (tag != null) {
            List list = (List) hookTagMap.get(tag);
            if (list == null) {
                list = new ArrayList();
                hookTagMap.put(tag, list);
            }
            list.add(unhook);
        }
    } catch (Exception e) {
        traceLog("main_log", "[hook] Hook注册失败: " + e);
    }
}

void unhook(String tag) {
    if (tag == null) {
        unhookAll();
        return;
    }
    List list = (List) hookTagMap.get(tag);
    if (list == null || list.isEmpty()) return;
    for (int i = 0; i < list.size(); i++) {
        try {
            Object u = list.get(i);
            if (u != null) u.getClass().getMethod("unhook").invoke(u);
            hookloveList.remove(u);
        } catch (Exception e) {
            traceLog("main_log", "[unhook] " + tag + " 失败: " + e);
        }
    }
    list.clear();
    hookTagMap.remove(tag);
}

void unhookAll() {
    for (Object unhook : hookloveList) {
        try {
            if (unhook != null) unhook.getClass().getMethod("unhook").invoke(unhook);
        } catch (Exception e) {
            traceLog("main_log", "[unhookAll] 卸载失败: " + e);
        }
    }
    hookloveList.clear();
    hookTagMap.clear();
}

void Hook生命周期() {
    try {
        String hostPkg = HostInfo.INSTANCE.getPackageName();
        Method resumeM = getCachedMethod(Activity.class, "onResume", new Class[0]);
        Method pauseM = getCachedMethod(Activity.class, "onPause", new Class[0]);
        if (resumeM == null || pauseM == null) {
            showApiProtectGuide(safeActivity());
            return;
        }

        hook(Activity.class, "onResume", new Class[0], new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                Activity a = (Activity) param.thisObject;
                if (hostPkg.equals(a.getPackageName())) {
                    resumedActivityCount++;
                    checkAndUpdateForegroundState(a);
                }
            }
        });

        hook(Activity.class, "onPause", new Class[0], new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                Activity a = (Activity) param.thisObject;
                if (hostPkg.equals(a.getPackageName())) {
                    resumedActivityCount--;
                    if (resumedActivityCount < 0) resumedActivityCount = 0;
                    checkAndUpdateBackgroundState();
                }
            }
        });

        if (getNowActivity() != null) {
            resumedActivityCount = 1;
            checkAndUpdateForegroundState(getNowActivity());
        }

        initStats();
        Hook已调用 = true;

    } catch (Exception e) {
        traceLog("main_log", "[Hook生命周期] Hook生命周期异常: " + e);
        showApiProtectGuide(safeActivity());
    }
}

void 后台初始化() {
    if (非UI初始化完成) return;

    try {
        ensureResourceAvailable();
        addItem("开/关悬浮窗", "悬浮窗开关");
        addItem("设置", "openSetting");
        addItem("Java脚本", "openPlugin");
        applyFloatWindowMenuText();
        traceLog("main_log", "[后台初始化] add项添加完成");
        开模拟定位();
        checkMuteAllExpiry();
        
        
        
        非UI初始化完成 = true;

    } catch (Exception e) {
        traceLog("main_log", "[后台初始化] 后台初始化异常: " + e.getMessage());
    }
}

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
	        String countJson = getCounter("https://api.counterapi.dev/v2/s-team-62-5773/qfx/up");
	        int totalCount = 0;
	        if (countJson != null && !countJson.isEmpty() && !countJson.startsWith("访问网页失败")) {
	            try {
	                JSONObject jsonObj = new JSONObject(countJson.trim());
	                JSONObject dataObj = jsonObj.optJSONObject("data");
	                if (dataObj != null) {
	                    totalCount = dataObj.optInt("up_count", 0);
	                }
	            } catch (Exception e) {
	                traceLog("main_log", "[前台初始化] 计数解析异常：" + e.getMessage());
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
	                    traceLog("main_log", "[前台初始化] 新用户标记异常：" + e.getMessage());
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

    checkQFXUpdate();

    if (getBoolean("settings", "开关", false)) {
        悬浮窗状态 = STATE_DESTROYED;
        启动悬浮窗(currentActivity);
    }

    UI初始化完成 = true;
}

void 原神启动() {
    traceLog("main_log", "[原神启动] 被调用");

    if (apiLoadCostTime == -1) {
        traceLog("main_log", "[原神启动] 错误：apiLoadCostTime未初始化");
        return;
    }

    traceLog("main_log", "[原神启动] 脚本开始初始化，耗时：" + apiLoadCostTime + "ms");

    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                后台初始化();
                traceLog("main_log", "[原神启动] 后台初始化完成");

                Activity currentAct = getNowActivity();
                if (currentAct != null) {
                    traceLog("main_log", "[原神启动] 有可见Activity，执行前台初始化");
                    最后Activity = currentAct;
                    uiHandler.post(new Runnable() {
                        public void run() {
                            前台初始化(currentAct);
                        }
                    });
                } else {
                    traceLog("main_log", "[原神启动] 无可见Activity，发送通知");
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
                traceLog("main_log", "[原神启动] 初始化异常: " + e.getMessage());
            }
        }
    });
        if (!Hook已调用) {
        Hook生命周期();
        Hook已调用 = true;
        traceLog("main_log", "[原神启动] Hook生命周期已调用");
    }

}

addMenuItem("菜单", "菜单");
