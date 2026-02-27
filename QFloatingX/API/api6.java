String Module = null;
String ModuleVersionName = null;
int ModuleAPICode = 0;
int QQ_version = 0;
String ModulePackageName = "";
String LocalPath = Environment.getExternalStorageDirectory().getPath() + "/";
String CurrentApp = "QQ"; 
if ("com.tencent.tim".equals(context.getPackageName())) {
    CurrentApp = "TIM";
}

// 辅助方法：获取QQ版本
public static String getQQVersion() {
    try {
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName + 
               "(" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode + ")";
    } catch(Throwable e) {
        return "未知";
    }
}

// 辅助方法：模块信息加载
void ensureModuleInfoLoaded(Activity context) {
    if (Module != null) return;
    try {
        Module = judge(context);
        mapModuleToPackageName();
        if (!ModulePackageName.equals("")) {
            ModuleAPICode = getModuleAPICode(context);
            ModuleVersionName = getModuleVersion(context);
        } else {
            ModuleVersionName = "内置";
        }
    } catch (Exception e) {
        Module = "未知模块";
        ModuleVersionName = "获取失败";
    }
}

public String judge(Activity context) {
    try {
        ClassLoader MClassLoader = this.getClass().getClassLoader();
        String path = MClassLoader.toString();
        if(path.contains("lzlnb.cnm.hook")) return "模了个块";
        else if(path.contains("com.demo.serendipity")) return "Serendipity";
        else if(path.contains("lin.xposed")) return "QStory";
        else if(path.contains("me.yxp.qfun")) return "QFun";
        else return "未知模块";
    } catch (Exception e) {
        return "未知模块";
    }
}

void mapModuleToPackageName() {
    if (Module == null) return;
    if (Module.equals("Serendipity")) ModulePackageName = "com.demo.serendipity";
    else if (Module.equals("QStory")) ModulePackageName = "lin.xposed";
    else if (Module.equals("模了个块")) ModulePackageName = "lzlnb.cnm.hook";
    else if (Module.equals("QFun")) ModulePackageName = "me.yxp.qfun";
    else ModulePackageName = "";
}

int getModuleAPICode(Activity context) {
    if (ModulePackageName.equals("")) return 0;
    try {
        PackageManager pmm = context.getPackageManager();
        ApplicationInfo ai = pmm.getApplicationInfo(ModulePackageName, PackageManager.GET_META_DATA);
        if (ai.metaData != null) {
            return ai.metaData.getInt("xposedminversion", 0);
        }
    } catch (Exception e) {}
    return 0;
}

String getModuleVersion(Activity context) {
    if (ModulePackageName.equals("")) return "内置";
    try {
        return context.getPackageManager().getPackageInfo(ModulePackageName, 0).versionName;
    } catch (Exception e) {
        return "内置";
    }
}

// 硬件与系统信息获取
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
        Status status = app.getRuntimeService(IOnlineStatusService.class).getOnlineStatus();
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
    if(!needSysAPP) {
        List resolveInfosWithoutSystem = new ArrayList();
        for(int i = 0; i < resolveInfos.size(); i++) {
            ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(i);
            try {
                if(!isSysApp(resolveInfo.activityInfo.packageName)) {
                    resolveInfosWithoutSystem.add(resolveInfo);
                }
            } catch(Exception e) {}
        }
        return resolveInfosWithoutSystem;
    }
    return resolveInfos;
}

String 获取应用运行状态(Activity context) {
    try {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List processes = am.getRunningAppProcesses();
        for (int i = 0; i < processes.size(); i++) {
            ActivityManager.RunningAppProcessInfo process = (ActivityManager.RunningAppProcessInfo) processes.get(i);
            if (process.processName.equals("com.tencent.mobileqq")) {
                return process.importance <= 100 ? "运行中" : "后台";
            }
        }
        return "未运行";
    } catch (Exception e) { return "未知"; }
}

String getXP框架名() {
    try {
        Object cl = this.getClass().getClassLoader();
        Class clazz = cl.loadClass("de.robv.android.xposed.XposedBridge");
        Field f = clazz.getField("TAG");
        f.setAccessible(true);
        return (String) f.get(null);
    } catch(Exception e) {
        return "LSPosed";
    }
}

// 消息统计相关接口
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
        sb.append("队列容量: #").append(50).append("#\n"); // 固定值
        sb.append("总任务:    #").append(executor.getTaskCount()).append("#  ");
        sb.append("累计已完成: #").append(executor.getCompletedTaskCount()).append("#");
        return sb.toString();
    } catch (Exception e) {
        return "获取失败: " + e.getMessage();
    }
}


// 创建支持 #高亮# 语法的文本视图 (支持多重闭合高亮)
TextView createTSStyleTextView(Activity context, String text, boolean isDark) {
    TextView textView = new TextView(context);
    textView.setTextSize(14); 
    textView.setTextIsSelectable(true);
    textView.setSingleLine(false);
    textView.setLineSpacing(dp(context, 3), 1.0f);
    textView.setMaxLines(Integer.MAX_VALUE);
    textView.setEllipsize(null);
    
    // 基础文字颜色
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
                
                // 循环处理单行内的所有 #内容# 对
                while (true) {
                    int openIdx = line.indexOf("#", start);
                    
                    if (openIdx == -1) {
                        // 剩余部分为普通文字
                        if (start < line.length()) {
                            String textSegment = line.substring(start);
                            SpannableString sp = new SpannableString(textSegment);
                            sp.setSpan(new ForegroundColorSpan(normalColor), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            // 如果是行首，加粗（模拟标签样式）
                            if (start == 0) {
                                sp.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            ssb.append(sp);
                        }
                        break;
                    }
                    
                    // 添加 # 之前的普通文字
                    if (openIdx > start) {
                        String textSegment = line.substring(start, openIdx);
                        SpannableString sp = new SpannableString(textSegment);
                        sp.setSpan(new ForegroundColorSpan(normalColor), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (start == 0) {
                            sp.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        ssb.append(sp);
                    }
                    
                    // 寻找闭合的 #
                    int closeIdx = line.indexOf("#", openIdx + 1);
                    if (closeIdx == -1) {
                        // 没有闭合，剩余部分按普通文字处理（包含起始#）
                        String textSegment = line.substring(openIdx);
                        SpannableString sp = new SpannableString(textSegment);
                        sp.setSpan(new ForegroundColorSpan(normalColor), 0, textSegment.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.append(sp);
                        break;
                    }
                    
                    // 添加高亮内容
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

// 创建大标题 (左上角对齐)
TextView createTitleView(Activity context, String title, boolean isDark) {
    TextView titleTv = new TextView(context);
    titleTv.setText(title);
    titleTv.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#212121"));
    titleTv.setTextSize(18); // Mini标题
    titleTv.setTypeface(null, Typeface.BOLD);
    titleTv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
    titleTv.setPadding(dp(context, 4), dp(context, 4), 0, dp(context, 10));
    return titleTv;
}

// 创建通用卡片
LinearLayout createTSCard(Activity context, String title, String content, boolean isDark) {
    LinearLayout card = new LinearLayout(context);
    card.setOrientation(LinearLayout.VERTICAL);
    // 卡片内边距 12
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
    params.bottomMargin = dp(context, 10); // 卡片间距 10
    card.setLayoutParams(params);
    
    TextView titleTv = new TextView(context);
    titleTv.setText(title);
    titleTv.setTextColor(isDark ? Color.parseColor("#8AB4F8") : Color.parseColor("#FF6B6B"));
    titleTv.setTextSize(16); // 标题 16
    titleTv.setTypeface(titleTv.getTypeface(), Typeface.BOLD);
    titleTv.setPadding(0, 0, 0, dp(context, 6));
    
    TextView contentTv = createTSStyleTextView(context, content, isDark);
    
    card.addView(titleTv);
    card.addView(contentTv);
    return card;
}

void addQQ状态卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        String Path = LocalPath;
        if (Path != null && Path.length() > 18) Path = Path.substring(18);
        String doubleOpen = "";
        if (Path.startsWith("999")) doubleOpen = "分身";
        else if (Path.startsWith("10")) doubleOpen = "非机主";
        
        StringBuilder content = new StringBuilder();
        content.append("账号: #").append(myUin != null ? myUin : "未知").append("#\n");
        content.append("状态: #").append(get在线状态()).append("#\n");
        content.append("版本: #").append(getQQVersion()).append("#");
        content.append("(").append(doubleOpen.equals("") ? "主QQ" : doubleOpen).append(")");
        
        parent.addView(createTSCard(context, "📱 QQ运行状态", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "📱 QQ运行状态", "错误: " + e.getMessage(), isDark));
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
        
        parent.addView(createTSCard(context, "⚙️ 开关状态", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "⚙️ 开关状态", "错误: " + e.getMessage(), isDark));
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
        
        parent.addView(createTSCard(context, "📊 线程监控", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "📊 线程监控", "错误: " + e.getMessage(), isDark));
    }
}

void add电池信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        String[] battery = get电池状态(context);
        StringBuilder content = new StringBuilder();
        content.append("电量: #").append(battery[0]).append("# (").append(battery[1]).append(")\n");
        content.append("健康: #").append(get电池健康(context)).append("#\n");
        content.append("温度: #").append(battery[3]).append("#");
        parent.addView(createTSCard(context, "🔋 电池信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "🔋 电池信息", "错误: " + e.getMessage(), isDark));
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
        parent.addView(createTSCard(context, "🧠 系统资源", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "🧠 系统资源", "错误: " + e.getMessage(), isDark));
    }
}

void add模块信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        ensureModuleInfoLoaded(context);
        StringBuilder content = new StringBuilder();
        content.append("模块: #").append(Module != null ? Module : "未知").append("_").append(ModuleVersionName).append("#\n");
        content.append("API: #").append(ModuleAPICode).append("#\n");
        content.append("框架: #").append(getXP框架名()).append("#");
        parent.addView(createTSCard(context, "📦 模块信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "📦 模块信息", "错误: " + e.getMessage(), isDark));
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
        parent.addView(createTSCard(context, "🍭 脚本信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "🔧 脚本信息", "错误: " + e.getMessage(), isDark));
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
        parent.addView(createTSCard(context, "💾 JVM内存信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "💾 JVM内存", "错误: " + e.getMessage(), isDark));
    }
}

void add设备信息卡片(Activity context, LinearLayout parent, boolean isDark) {
    try {
        StringBuilder content = new StringBuilder();
        content.append("型号: #").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("#\n");
        content.append("Android: #").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")#\n");
        content.append("应用: #").append(CurrentApp).append("# (").append(获取应用运行状态(context)).append(")");
        parent.addView(createTSCard(context, "🔧 设备信息", content.toString(), isDark));
    } catch (Exception e) {
        parent.addView(createTSCard(context, "🔧 设备信息", "错误: " + e.getMessage(), isDark));
    }
}

void display状态对话框(Activity activity) {
    String errorStage = "初始化";
    try {
        boolean isDark = isThemeDark(activity);
        
        // 主内容容器
        LinearLayout contentLayout = new LinearLayout(activity);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        // 容器内边距
        contentLayout.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
        
        errorStage = "添加组件";
        contentLayout.addView(createTitleView(activity, "运行状态监控", isDark));
        addQQ状态卡片(activity, contentLayout, isDark);
        add开关状态卡片(activity, contentLayout, isDark);
        add监控卡片(activity, contentLayout, isDark);
        add电池信息卡片(activity, contentLayout, isDark);
        add系统资源卡片(activity, contentLayout, isDark);
        add模块信息卡片(activity, contentLayout, isDark);
        add脚本信息卡片(activity, contentLayout, isDark);
        addJVM内存信息卡片(activity, contentLayout, isDark);
        add设备信息卡片(activity, contentLayout, isDark);
        
        // 底部Footer
        TextView footer = new TextView(activity);
        footer.setText("Generated by QFloatingX");
        footer.setGravity(Gravity.CENTER);
        footer.setTextColor(Color.parseColor(isDark ? "#555555" : "#AAAAAA"));
        footer.setTextSize(10);
        footer.setPadding(0, dp(activity, 4), 0, dp(activity, 4));
        contentLayout.addView(footer);

        // 滚动容器
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
        
        //  设置高度限制 (屏幕高度的 55%)，宽度保持 300dp
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = dp(activity, 300); // 强制固定宽度
        params.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.55); // 高度55%
        dialog.getWindow().setAttributes(params);
        
        traceLog("api6_log.txt","状态对话框显示成功");
        
    } catch (Exception e) {
        traceLog("api6_log.txt","构建对话框失败 [" + errorStage + "]: " + e);
        Toast("展示失败: " + e.getMessage());
    }
}

// 入口方法
public void 运行状态Dialog(Activity activity) {
    if (activity == null || activity.isFinishing()) {
        Toast("Activity无效");
        return;
    }
    
    try {
        vibrate(activity, 48);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    display状态对话框(activity);
                } catch (Exception e) {
                    traceLog("api6_log.txt","弹窗失败: " + e.getMessage());
                    Toast("显示失败: " + e.getMessage());
                }
            }
        });
    } catch (Exception e) {
        traceLog("api6_log.txt","UI线程失败: " + e.getMessage());
    }
}