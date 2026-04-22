import com.tencent.mobileqq.onlinestatus.api.IOnlineStatusService;
import me.yxp.qfun.BuildConfig;
import me.yxp.qfun.utils.qq.HostInfo;

String[] get电池状态(Activity context) {
    try {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = Math.round((level * 100.0f) / scale);
        
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String statusStr = "未知";
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) statusStr = "充电中";
        else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) statusStr = "使用中";
        else if (status == BatteryManager.BATTERY_STATUS_FULL) statusStr = "已充满";
        else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) statusStr = "未充电";
        
        float temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f;
        
        return new String[]{batteryPct + "%", statusStr, "", temp + "°C"};
    } catch (Exception e) {
        return new String[]{"未知", "未知", "", "未知"};
    }
}

String get电池健康(Context context) {
    try {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "良好";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "过热";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "损坏";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "电压过高";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "未指定故障";
            default: return "未知";
        }
    } catch (Exception e) { return "未知"; }
}

String get可用运存(Activity context) {
    try {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        return formatSize(memInfo.availMem);
    } catch (Exception e) { return "未知"; }
}

String get可用内部存储(Activity context) {
    try {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return formatSize(stat.getAvailableBlocksLong() * stat.getBlockSizeLong());
    } catch (Exception e) { return "未知"; }
}

String get总内部存储(Activity context) {
    try {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return formatSize(stat.getBlockCountLong() * stat.getBlockSizeLong());
    } catch (Exception e) { return "未知"; }
}

public String getIPAddress() {
    try {
        InetAddress localHost = InetAddress.getLocalHost();
        return localHost.getHostAddress();
    } catch (Exception e) { return e.getMessage(); }
}

public int getCPURunningNum() {
    return Runtime.getRuntime().availableProcessors();
}

public static String getCPUInfo() {
    return Build.CPU_ABI;
}

String get在线状态() {
    try {
        Object statusObj = QQCurrentEnv.INSTANCE.getQQAppInterface().getRuntimeService(IOnlineStatusService.class, "");
        Object status = statusObj.getClass().getMethod("getOnlineStatus").invoke(statusObj);
        String StatusString = status + "";
        if(StatusString.equals("null")) return "未知";
        if(StatusString.equals("away")) return "离开";
        if(StatusString.equals("offline")) return "离线";
        if(StatusString.equals("invisiable")) return "隐身";
        if(StatusString.equals("busy")) return "忙碌";
        if(StatusString.equals("qme")) return "Q我吧";
        if(StatusString.equals("dnd")) return "请勿打扰";
        if(StatusString.equals("online")) return "在线";
        if(StatusString.equals("receiveofflinemsg")) return "失联";
        return StatusString;
    } catch (Exception e) { return "获取失败"; }
}

public static List getInstalledApplication(boolean needSysAPP) {
    PackageManager packageManager = context.getPackageManager();
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    List resolveInfos = packageManager.queryIntentActivities(intent, 0);
    if (!needSysAPP) {
        return (List) resolveInfos.stream()
                .filter(info -> !isSysApp(((ResolveInfo) info).activityInfo.packageName))
                .collect(Collectors.toList());
    }
    return resolveInfos;
}

String 获取应用运行状态(Activity context) {
    try {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return (String) am.getRunningAppProcesses().stream()
                .filter(process -> ((ActivityManager.RunningAppProcessInfo) process).processName.equals(HostInfo.INSTANCE.getPackageName()))
                .findFirst()
                .map(process -> ((ActivityManager.RunningAppProcessInfo) process).importance <= 100 ? "运行中" : "后台")
                .orElse("未运行");
    } catch (Exception e) { return "未知"; }
}

private int getBatchQueueSize() {
    return messageBatchQueue != null ? messageBatchQueue.size() : 0;
}

private boolean isWriteThreadRunning() {
    return writeThreadRunning;
}

private int getPendingWriteKeysCount() {
    return CHANGED_KEYS != null ? CHANGED_KEYS.size() : 0;
}

public String getThreadPoolInfo() {
    if (ThreadPool == null) return "线程池未初始化";
    try {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) ThreadPool;
        StringBuilder sb = new StringBuilder();
        sb.append("核心线程: #").append(executor.getCorePoolSize()).append("#  ");
        sb.append("最大线程: #").append(executor.getMaximumPoolSize()).append("#\n");
        sb.append("当前线程: #").append(executor.getPoolSize()).append("#  ");
        sb.append("活跃线程: #").append(executor.getActiveCount()).append("#\n");
        sb.append("队列任务: #").append(executor.getQueue().size()).append("#  ");
        String sp = getString("settings", "thread_pool_queue_capacity", "");
        sb.append("队列容量: #").append(sp).append("#\n"); 
        sb.append("总任务:    #").append(executor.getTaskCount()).append("#  ");
        sb.append("累计已完成: #").append(executor.getCompletedTaskCount()).append("#");
        return sb.toString();
    } catch (Exception e) {
        return "获取失败: " + e.getMessage();
    }
}

TextView createTSStyleTextView(Activity context, String text, boolean isDark) {
    TextView textView = new TextView(context);
    textView.setTextSize(14); 
    textView.setTextIsSelectable(true);
    textView.setSingleLine(false);
    textView.setLineSpacing(dp(context, 3), 1.0f);
    textView.setMaxLines(Integer.MAX_VALUE);
    textView.setEllipsize(null);
    
    int normalColor = isDark ? Color.parseColor("#CCCCCC") : Color.parseColor("#555555");
    
    try {
        if (text != null && !text.equals("")) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            String[] lines = text.split("\n");
            int highlightIndex = 0;
            
            final int[] HIGHLIGHT_COLORS = isDark ? new int[]{
                Color.parseColor("#FF8A80"), Color.parseColor("#80DEEA"),
                Color.parseColor("#CE93D8"), Color.parseColor("#FFCC80")
            } : new int[]{
                Color.parseColor("#D32F2F"), Color.parseColor("#0097A7"),
                Color.parseColor("#7B1FA2"), Color.parseColor("#F57C00")
            };
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int start = 0;
                
                while (true) {
                    int openIdx = line.indexOf("#", start);
                    
                    if (openIdx == -1) {
                        if (start < line.length()) {
                            String textSegment = line.substring(start);
                            SpannableString sp = new SpannableString(textSegment);
                            sp.setSpan(new ForegroundColorSpan(normalColor), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            if (start == 0) {
                                sp.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            ssb.append(sp);
                        }
                        break;
                    }
                    
                    if (openIdx > start) {
                        String textSegment = line.substring(start, openIdx);
                        SpannableString sp = new SpannableString(textSegment);
                        sp.setSpan(new ForegroundColorSpan(normalColor), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (start == 0) {
                            sp.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        ssb.append(sp);
                    }
                    
                    int closeIdx = line.indexOf("#", openIdx + 1);
                    if (closeIdx == -1) {
                        String textSegment = line.substring(openIdx);
                        SpannableString sp = new SpannableString(textSegment);
                        sp.setSpan(new ForegroundColorSpan(normalColor), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.append(sp);
                        break;
                    }
                    
                    String highlightText = line.substring(openIdx + 1, closeIdx);
                    if (highlightText.length() > 0) {
                        SpannableString sp = new SpannableString(highlightText);
                        sp.setSpan(new ForegroundColorSpan(HIGHLIGHT_COLORS[highlightIndex++ % HIGHLIGHT_COLORS.length]), 0, highlightText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.append(sp);
                    }
                    
                    start = closeIdx + 1;
                }
                
                if (i != lines.length - 1) ssb.append("\n");
            }
            textView.setText(ssb);
        } else {
            textView.setText("");
        }
    } catch (Exception e) {
        textView.setText(text != null ? text : "");
        textView.setTextColor(normalColor);
    }
    
    return textView;
}

TextView createTitleView(Activity context, String title, boolean isDark) {
    TextView titleTv = new TextView(context);
    titleTv.setText(title);
    titleTv.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#212121"));
    titleTv.setTextSize(18); 
    titleTv.setTypeface(null, Typeface.BOLD);
    titleTv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    titleTv.setPadding(dp(context, 4), dp(context, 4), 0, dp(context, 10));
    return titleTv;
}

LinearLayout createTSCard(Activity context, String title, String content, boolean isDark) {
    LinearLayout card = new LinearLayout(context);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
    
    GradientDrawable bg = new GradientDrawable();
    if (isDark) {
        bg.setColor(Color.parseColor("#FF2D2D2D"));
        bg.setStroke(dp(context, 1), Color.parseColor("#1AFFFFFF")); 
    } else {
        bg.setColor(Color.parseColor("#FFFFFF"));
    }
    bg.setCornerRadius(dp(context, 10));
    card.setBackground(bg);
    
    if (!isDark && Build.VERSION.SDK_INT >= 21) {
        card.setElevation(dp(context, 1));
    }
    
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    params.bottomMargin = dp(context, 10); 
    card.setLayoutParams(params);
    
    TextView titleTv = new TextView(context);
    titleTv.setText(title);
    titleTv.setTextColor(isDark ? Color.parseColor("#8AB4F8") : Color.parseColor("#FF6B6B"));
    titleTv.setTextSize(16);
    titleTv.setTypeface(titleTv.getTypeface(), Typeface.BOLD);
    titleTv.setPadding(0, 0, 0, dp(context, 6));
    
    TextView contentTv = createTSStyleTextView(context, content, isDark);
    
    card.addView(titleTv);
    card.addView(contentTv);
    return card;
}

void addQQ状态卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        StringBuilder content = new StringBuilder();
        content.append("账号: #").append(qq).append("#\n");
        content.append("状态: #").append(get在线状态()).append("#\n");
        content.append("版本: #").append(HostInfo.INSTANCE.getVersionName()).append("(").append(HostInfo.INSTANCE.getVersionCode()).append(")#");
        
        parent.addView(createTSCard(context, " QQ运行状态", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " QQ运行状态", "错误: " + e.getMessage(), isDark));
    }
}

void add开关状态卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        boolean floatWindowState = getBoolean("settings", "开关", false);
        boolean mockLocationState = getBoolean("模拟定位开关", "模拟定位开关", false);
        boolean 输入框t开关 = getBoolean("输入框", "输入框开关", false);
        
        StringBuilder content = new StringBuilder();
        content.append("悬浮窗: #").append(floatWindowState ? "开启" : "关闭").append("#\n");
        content.append("模拟定位: #").append(mockLocationState ? "开启" : "关闭").append("#\n");
        content.append("输入框提示: #").append(输入框t开关 ? "开启" : "关闭").append("#");
        
        parent.addView(createTSCard(context, " 开关状态", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 开关状态", "错误: " + e.getMessage(), isDark));
    }
}

void add监控卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        int queueSize = getBatchQueueSize();
        boolean threadRunning = isWriteThreadRunning();
        int pendingKeys = getPendingWriteKeysCount();
        String 线程info = getThreadPoolInfo();
        
        StringBuilder content = new StringBuilder();
        content.append("消息统计队列: #").append(queueSize).append("# 条\n");
        content.append(线程info).append("\n");
        content.append("写入线程: #").append(threadRunning ? "✅运行中" : "❌已休眠").append("#\n");
        content.append("待写入键: #").append(pendingKeys).append("# 个");
        
        parent.addView(createTSCard(context, " 线程监控", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 线程监控", "错误: " + e.getMessage(), isDark));
    }
}

void add电池信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        String[] battery = get电池状态(context);
        StringBuilder content = new StringBuilder();
        content.append("电量: #").append(battery[0]).append("# (").append(battery[1]).append(")\n");
        content.append("健康: #").append(get电池健康(context)).append("#\n");
        content.append("温度: #").append(battery[3]).append("#");
        parent.addView(createTSCard(context, " 电池信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 电池信息", "错误: " + e.getMessage(), isDark));
    }
}

void add系统资源卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        List apps = getInstalledApplication(true);
        StringBuilder content = new StringBuilder();
        content.append("运存: #").append(get可用运存(context)).append("#\n");
        content.append("存储: #").append(get可用内部存储(context)).append(" / ").append(get总内部存储(context)).append("#\n");
        content.append("应用数量: #").append(apps.size() + "个").append("#\n");
        content.append("CPU架构: #").append(getCPUInfo()).append("# (运行:").append(getCPURunningNum()).append("个)");
        parent.addView(createTSCard(context, " 系统资源", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 系统资源", "错误: " + e.getMessage(), isDark));
    }
}
void add模块信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        StringBuilder content = new StringBuilder();
        content.append("模块: #QFun_").append(BuildConfig.VERSION_NAME).append("#\n");
        content.append("框架: #").append(HookEngineManager.INSTANCE.getEngine().getFrameworkName()).append("#\n");
        content.append("API Level: #").append(HookEngineManager.INSTANCE.getEngine().getApiLevel()).append("#");
        parent.addView(createTSCard(context, " 模块信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 模块信息", "错误: " + e.getMessage(), isDark));
    }
}


void add脚本信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        long time = System.currentTimeMillis();
        String versionCode = readprop(pluginPath+"/info.prop","versionCode");
        String pluginName = readprop(pluginPath + "/info.prop", "pluginName");
        String scriptId = readprop(pluginPath + "/info.prop", "id");
        String scriptauthor = readprop(pluginPath + "/info.prop", "author");
        File folder = new File(pluginPath);
        String formattedSize = getFormattedSize(folder);
        StringBuilder content = new StringBuilder();
        content.append("运行脚本: #").append(pluginName).append("(").append(versionCode).append(")#\n");
        content.append("脚本ID: #").append(scriptId).append("#\n");
        content.append("脚本作者: #").append(scriptauthor).append("#\n");
        content.append("脚本大小: #").append(formattedSize).append("#\n");
        content.append("脚本运行时间: #").append(formatTime((float)(time - startTime))).append("#");
        parent.addView(createTSCard(context, " 脚本信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 脚本信息", "错误: " + e.getMessage(), isDark));
    }
}

void addJVM内存信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        Runtime runtime = Runtime.getRuntime();
        StringBuilder content = new StringBuilder();
        content.append("剩余内存: #").append(formatSize(runtime.totalMemory())).append("#\n");
        content.append("已用内存: #").append(formatSize(runtime.totalMemory() - runtime.freeMemory())).append("#\n");
        content.append("空闲内存: #").append(formatSize(runtime.freeMemory())).append("#\n");
        content.append("最大内存: #").append(formatSize(runtime.maxMemory())).append("#\n");
        content.append("内存使用率: #").append(String.format("%.2f%%",
                (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100)).append("#");
        parent.addView(createTSCard(context, " JVM内存信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " JVM内存", "错误: " + e.getMessage(), isDark));
    }
}

void add设备信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        StringBuilder content = new StringBuilder();
        content.append("型号: #").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("#\n");
        content.append("Android: #").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")#\n");
        content.append("宿主: #").append(HostInfo.INSTANCE.isTIM() ? "TIM" : "QQ").append("# (").append(获取应用运行状态(context)).append(")");
        parent.addView(createTSCard(context, " 设备信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, " 设备信息", "错误: " + e.getMessage(), isDark));
    }
}

void display状态对话框(final Activity activity) {
    String errorStage = "初始化";
    try {
        boolean isDark = isThemeDark(activity);
        
        LinearLayout contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
        
        errorStage = "添加组件";
        contentLayout.addView(createTitleView(activity, "运行状态", isDark));
        addQQ状态卡片(activity, contentLayout, isDark);
        add开关状态卡片(activity, contentLayout, isDark);
        add监控卡片(activity, contentLayout, isDark);
        add电池信息卡片(activity, contentLayout, isDark);
        add系统资源卡片(activity, contentLayout, isDark);
        add模块信息卡片(activity, contentLayout, isDark);
        add脚本信息卡片(activity, contentLayout, isDark);
        addJVM内存信息卡片(activity, contentLayout, isDark);
        add设备信息卡片(activity, contentLayout, isDark);
        
        TextView footer = new TextView(activity);
        footer.setText("Powered by QFun Engine");
        footer.setGravity(Gravity.CENTER);
        footer.setTextColor(Color.parseColor(isDark ? "#555555" : "#AAAAAA"));
        footer.setTextSize(10);
        footer.setPadding(0, dp(activity, 4), 0, dp(activity, 4));
        contentLayout.addView(footer);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.addView(contentLayout);
        
        errorStage = "构建弹窗";
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, 
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setView(scrollView);
        builder.setPositiveButton("关闭", null);
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        applyUiTheme(activity, dialog);
        
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = dp(activity, 300); 
        params.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.55); 
        dialog.getWindow().setAttributes(params);
        
    } catch (Exception e) {
        traceLog("api6_log.txt","构建对话框失败 [" + errorStage + "]: " + e);
        Toast("展示失败: " + e.getMessage());
    }
}

public void 运行状态Dialog(final Activity activity) {
    activity.runOnUiThread(() -> {
        try {
            display状态对话框(getNowActivity());
        } catch (Exception e) {
            Toast("显示失败: " + e.getMessage());
        }
    });
}