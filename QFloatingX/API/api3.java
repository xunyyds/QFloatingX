/**
 * 核心配置常量
 */
private static final String SP_NAME = "msg_stats_config";
private static final String SP_TIME_RANGE_KEY = "selected_time_range";
private static final String SP_CUSTOM_DATE_KEY = "custom_selected_date";
private static final String DAILY_TARGET_KEY = "daily_msg_target";
private static final int DEFAULT_DAILY_TARGET = 100;
private static final String TEMP_FILE_SUFFIX = ".tmp";
private static final long MIN_UI_UPDATE_INTERVAL = 150;
private static final int BATCH_SIZE_THRESHOLD = 10;

/**
 * UI与状态管理变量
 */
volatile boolean dialogVisible = false;
AlertDialog statsDialog = null;

/**
 * 线程与并发控制变量
 */
private final Object writeLock = new Object();
private volatile boolean writeThreadRunning = false;
private Handler msgHandle = new Handler(Looper.getMainLooper());
private volatile long lastUIUpdateTime = 0L;

/**
 * 缓存机制变量
 */
private List weekDatesCache = new ArrayList();
private List monthDatesCache = new ArrayList();
private String weekCacheKey = "";
private String monthCacheKey = "";
private List statsTextViewCache = new ArrayList();
private LinearLayout todayCoreCardCache = null;
private View progressBarCache = null;
private TextView sendTargetLabelCache = null;
private TextView achievementTextCache = null;

/**
 * 统计类型定义数组
 */
private static final String[] STAT_TYPES = {
    "Receive", "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo",
    "ReceiveEmoji", "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip",
    "ReceiveWordCount", "ReceiveUnknown",
    "Send", "SendText", "SendPic", "SendFile", "SendVideo",
    "SendEmoji", "SendAudio", "SendCard", "SendCall", "Command", "Like",
    "SendWordCount", "SendUnknown"
};

/**
 * 数据存储核心变量
 */
private static final Hashtable OP_STATS = new Hashtable();
private static final Vector CHANGED_KEYS = new Vector();
private static final Vector messageBatchQueue = new Vector();
private static final Hashtable cardExpandStatus = new Hashtable();
volatile long TOTAL_MSG_SEQ_MAX = 0L;

/**
 * 颜色配置数组
 */
private int[] COLORS = {
    Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"), Color.parseColor("#45B7D1"),
    Color.parseColor("#96CEB4"), Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
    Color.parseColor("#FFA07A"), Color.parseColor("#87CEEB"), Color.parseColor("#F0E68C"),
    Color.parseColor("#CD853F"), Color.parseColor("#98FB98")
};

/**
 * 文件路径配置
 */
String configName = pluginPath + "/config/msg_stats.json";

/**
 * 时间范围类型定义类
 */
public class TimeRange {
    public static final int TODAY = 0;
    public static final int YESTERDAY = 1;
    public static final int THIS_WEEK = 2;
    public static final int THIS_MONTH = 3;
    public static final int CUSTOM_DATE = 4;
    
    /**
     * 将时间范围常量转换为可读字符串
     * @param range 时间范围常量
     * @return 可读的中文字符串
     */
    public static String toString(int range) {
        switch (range) {
            case TODAY: return "今日";
            case YESTERDAY: return "昨日";
            case THIS_WEEK: return "本周";
            case THIS_MONTH: return "本月";
            case CUSTOM_DATE: return "自定义日期";
            default: return "今日";
        }
    }
}

private volatile int currentTimeRange = TimeRange.TODAY;
private String customDateStr = null;

/**
 * 当前日期字符串，用于跨天检测
 */
private volatile String todayDateStr = getTodayDateStr();

/**
 * 原子递增操作
 * @param key 统计键名
 * @return 递增后的新值
 */
private synchronized long atomicIncrement(String key) {
    Long current = (Long) OP_STATS.get(key);
    long newValue = (current != null ? current.longValue() + 1 : 1);
    OP_STATS.put(key, Long.valueOf(newValue));
    addChangedKey(key);
    return newValue;
}

/**
 * 原子增加指定数值操作
 * @param key 统计键名
 * @param delta 增加的数值
 * @return 增加后的新值
 */
private synchronized long atomicAdd(String key, long delta) {
    Long current = (Long) OP_STATS.get(key);
    long newValue = (current != null ? current.longValue() + delta : delta);
    OP_STATS.put(key, Long.valueOf(newValue));
    addChangedKey(key);
    return newValue;
}

/**
 * 原子获取操作
 * @param key 统计键名
 * @return 当前值，不存在返回0
 */
private synchronized long atomicGet(String key) {
    Long value = (Long) OP_STATS.get(key);
    return value != null ? value.longValue() : 0L;
}

/**
 * 添加变更键到脏数据记录表
 * @param key 变更的键名
 */
private synchronized void addChangedKey(String key) {
    if (!CHANGED_KEYS.contains(key)) {
        CHANGED_KEYS.add(key);
    }
}

/**
 * 格式化统计数值显示
 * @param value 原始数值
 * @return 格式化后的字符串
 */
private String formatStatValue(long value) {
    if (value >= 10000) {
        double w = (double) value / 10000.0;
        return String.format(Locale.CHINA, "%.1f w", w);
    }
    return String.valueOf(value);
}

/**
 * 启动后台写入线程（懒调用模式）
 */
private void startWriteThread() {
    if (writeThreadRunning) return;
    
    writeThreadRunning = true;
    
    ThreadPool.execute(new Runnable() {
        public void run() {
            traceLog("api3_log.txt", "后台写入任务启动（懒调用模式）");
            while (writeThreadRunning) {
                try {
                    synchronized(writeLock) {
                        if (messageBatchQueue.isEmpty() && CHANGED_KEYS.isEmpty()) {
                            writeLock.wait();
                        }
                        if (!messageBatchQueue.isEmpty()) {
                            processBatch();
                        }
                    }
                    writeStats();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    traceLog("api3_log.txt", "任务被中断，退出");
                    break;
                } catch (Exception e) {
                    traceLog("api3_log.txt", "运行时错误: " + e.getMessage());
                }
            }
            traceLog("api3_log.txt", "后台写入任务已停止");
        }
    });
    traceLog("api3_log.txt","写入任务提交完成");
}

/**
 * 停止后台写入线程
 */
private void stopWriteThread() {
    traceLog("api3_log.txt", "停止写入任务");
    writeThreadRunning = false;
    synchronized(writeLock) {
        writeLock.notifyAll();
    }
}

/**
 * 格式化日期字符串用于显示
 * @param dateStr yyyyMMdd格式的日期字符串
 * @return yyyy年MM月dd日格式的日期字符串
 */
private String formatDateForDisplay(String dateStr) {
    try {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        return outputFormat.format(inputFormat.parse(dateStr));
    } catch (Exception e) {
        return dateStr;
    }
}

/**
 * 获取消息类型
 * @param data 消息数据对象
 * @return 消息类型字符串
 */
private String getMsgType(Object data) {
    if (data == null || data.data == null) return "unknown";
    
    List elements = data.data.elements;
    
    boolean hasText = false;
    boolean hasPic = false;
    boolean hasVideo = false;
    boolean hasFile = false;
    boolean hasVoice = false;
    boolean hasEmoji = false;
    boolean hasCard = false;

    for (int i = 0; i < elements.size(); i++) {
        Object element = elements.get(i);
        if (element == null) continue;

        if (element.picElement != null) hasPic = true;
        else if (element.videoElement != null) hasVideo = true;
        else if (element.fileElement != null) hasFile = true;
        else if (element.pttElement != null) hasVoice = true;
        else if (element.marketFaceElement != null || element.faceElement != null) {
            hasEmoji = true; 
        }
        else if (element.arkElement != null || element.structMsgElement != null) hasCard = true;
        else if (element.textElement != null) {
            if (element.textElement.content != null && element.textElement.content.length() > 0) {
                hasText = true;
            }
        }
    }

    if (hasVideo) return "video";
    if (hasFile) return "file";
    if (hasPic) return "pic";
    if (hasVoice) return "audio";
    if (hasCard) return "card";
    if (hasText) return "text";
    if (hasEmoji) return "emoji";

    int rawType = data.data.msgType;
    if (rawType == 5) return "grayTip"; 
    if (rawType == 19) return "call";   

    return "unknown";
}

/**
 * 获取纯文本字数统计
 * @param data 消息数据对象
 * @return 文本字符总数
 */
private long getPureWordCount(Object data) {
    if (data == null || data.data == null || data.data.elements == null) return 0;
    
    long totalLen = 0;
    List elements = data.data.elements;
    
    for (int i = 0; i < elements.size(); i++) {
        Object element = elements.get(i);
        if (element != null && element.textElement != null) {
            String content = element.textElement.content;
            if (content != null) {
                totalLen += content.length();
            }
        }
    }
    return totalLen;
}

/**
 * 检查并处理跨天重置
 */
private void checkTodayReset() {
    String currentDate = getTodayDateStr();
    if (todayDateStr == null || todayDateStr.isEmpty()) {
        todayDateStr = currentDate;
        traceLog("api3_log.txt", "初始化todayDateStr=" + todayDateStr);
        return;
    }
    if (!currentDate.equals(todayDateStr)) {
        traceLog("api3_log.txt", "日期变更，旧:" + todayDateStr + " 新:" + currentDate);
        todayDateStr = currentDate;
        
        synchronized(writeLock) {
            writeStats();
        }
        
        triggerUIUpdate();
        weekDatesCache.clear();
        monthDatesCache.clear();
        weekCacheKey = "";
        monthCacheKey = "";
    }
}

/**
 * 从SharedPreferences获取每日目标值
 * @return 每日消息目标数
 */
private int getDailyTargetFromPrefs() {
    Activity activity = getNowActivity();
    if (activity != null) {
        SharedPreferences sp = activity.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
        int target = sp.getInt(DAILY_TARGET_KEY, DEFAULT_DAILY_TARGET);
        traceLog("api3_log.txt","读取目标: " + target);
        return target;
    }
    traceLog("api3_log.txt","activity为null，返回默认值: " + DEFAULT_DAILY_TARGET);
    return DEFAULT_DAILY_TARGET;
}

/**
 * 获取统计类型对应的颜色
 * @param index 类型索引
 * @return 颜色值
 */
private int getColorForStat(int index) {
    if (index >= 0 && index < COLORS.length) {
        return COLORS[index];
    }
    return Color.parseColor("#333333");
}

/**
 * 创建圆角矩形Drawable
 * @param activity Activity上下文
 * @param color 背景颜色
 * @param radius 圆角半径dp值
 * @return GradientDrawable对象
 */
private GradientDrawable createRoundRectDrawable(Activity activity, int color, int radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(activity, radius));
    return drawable;
}

/**
 * 创建空白间距View
 * @param activity Activity上下文
 * @param heightDp 高度dp值
 * @return View对象
 */
private View createSpaceView(Activity activity, int heightDp) {
    View space = new View(activity);
    space.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, heightDp)));
    return space;
}

/**
 * 创建分割线View
 * @param activity Activity上下文
 * @return 分割线View
 */
private View createDivider(Activity activity) {
    boolean isDark = isThemeDark(activity);
    View divider = new View(activity);
    divider.setBackgroundColor(isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#F0F0F0"));
    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 1);
    dividerParams.setMargins(0, dp(activity, 6), 0, dp(activity, 6));
    divider.setLayoutParams(dividerParams);
    return divider;
}

/**
 * 创建彩色按钮
 * @param activity Activity上下文
 * @param text 按钮文本
 * @param bgColor 背景颜色
 * @return Button对象
 */
private Button createColorButton(Activity activity, String text, int bgColor) {
    Button button = new Button(activity);
    button.setText(text);
    button.setTextColor(Color.parseColor("#333333"));
    button.setBackground(createRoundRectDrawable(activity, bgColor, 8));
    button.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        dp(activity, 120), dp(activity, 40));
    params.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
    button.setLayoutParams(params);
    button.setTextSize(13);
    button.setAllCaps(false);
    return button;
}

/**
 * 初始化时间范围设置
 * @param activity Activity上下文
 */
private void initTimeRange(Activity activity) {
    if (activity == null) {
        currentTimeRange = TimeRange.TODAY;
        traceLog("api3_log.txt","activity为null，默认TODAY");
        return;
    }
    SharedPreferences sp = activity.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
    int savedOrdinal = sp.getInt(SP_TIME_RANGE_KEY, 0);
    switch (savedOrdinal) {
        case 1: currentTimeRange = TimeRange.YESTERDAY; break;
        case 2: currentTimeRange = TimeRange.THIS_WEEK; break;
        case 3: currentTimeRange = TimeRange.THIS_MONTH; break;
        case 4: currentTimeRange = TimeRange.CUSTOM_DATE; break;
        default: currentTimeRange = TimeRange.TODAY; break;
    }
    customDateStr = sp.getString(SP_CUSTOM_DATE_KEY, null);
    if (customDateStr == null) {
        customDateStr = getTodayDateStr();
    }
    traceLog("api3_log.txt","加载范围: " + TimeRange.toString(currentTimeRange) + " 自定义日期: " + customDateStr);
}

/**
 * 读取完整统计数据文件
 */
private synchronized void readFullStats() {
    if (configName == null || configName.isEmpty()) {
        traceLog("api3_log.txt", "configName无效");
        return;
    }

    File mainFile = new File(configName);
    traceLog("api3_log.txt", "尝试读取主文件: " + mainFile.getAbsolutePath());
    if (mainFile.exists() && parseStatsFile(mainFile)) {
        traceLog("api3_log.txt", "从主文件加载数据");
        return;
    }

    File backupFile = new File(configName + ".bak");
    if (backupFile.exists() && parseStatsFile(backupFile)) {
        traceLog("api3_log.txt", "从备份文件恢复数据");
        Toast("已从备份文件恢复数据");
        return;
    }

    msgHandle.post(new Runnable() {
        public void run() {
            traceLog("api3_log.txt", "无历史数据，初始化空统计");
            initEmptyStats();
            Toast("无历史数据，初始化新统计");
        }
    });
}

/**
 * 解析统计数据文件
 * @param file 数据文件
 * @return 解析是否成功
 */
private boolean parseStatsFile(File file) {
    BufferedReader bf = null;
    try {
        bf = new BufferedReader(new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8), 8192);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) sb.append(line);
        String jsonStr = sb.length() == 0 ? "{}" : sb.toString();
        
        JSONObject statsJson = new JSONObject(jsonStr);
        Iterator jsonIterator = statsJson.keys();
        while (jsonIterator.hasNext()) {
            String key = (String) jsonIterator.next();
            long value = statsJson.optLong(key, 0L);
            OP_STATS.put(key, new Long(value));
        }
        TOTAL_MSG_SEQ_MAX = atomicGet("totalReceive");
        traceLog("api3_log.txt", "解析完成，加载键数量: " + OP_STATS.size());
        return true;
    } catch (Exception e) {
        traceLog("api3_log.txt", "文件: " + file.getName() + " - " + e.getMessage());
        return false;
    } finally {
        try { if (bf != null) bf.close(); } catch (Exception e) {}
    }
}

/**
 * 初始化空统计数据
 */
private void initEmptyStats() {
    synchronized(writeLock) {
        traceLog("api3_log.txt", "初始化空统计数据");
        OP_STATS.clear();
        CHANGED_KEYS.clear();
        for (int i = 0; i < STAT_TYPES.length; i++) {
            String type = STAT_TYPES[i];
            OP_STATS.put("total" + type, new Long(0L));
            traceLog("api3_log.txt", "初始化total" + type);
        }
        TOTAL_MSG_SEQ_MAX = 0L;
        traceLog("api3_log.txt", "初始化结束");
    }
}

/**
 * 初始化统计模块
 */
public void initStats() {
    traceLog("api3_log.txt", "api3初始化开始");
    Activity activity = getNowActivity();
    initTimeRange(activity);
    readFullStats();
    startWriteThread();
    traceLog("api3_log.txt", "api3初始化完成");
}

/**
 * 批量处理消息队列
 * 优化后的日期判断：直接使用消息时间戳确定日期
 */
private void processBatch() {
    if (messageBatchQueue.isEmpty()) return;
    
    List batch = new ArrayList();
    synchronized(writeLock) {
        while (!messageBatchQueue.isEmpty() && batch.size() < BATCH_SIZE_THRESHOLD) {
            batch.add(messageBatchQueue.remove(0));
        }
    }
    
    if (batch.isEmpty()) return;
    
    traceLog("api3_log.txt", "批量处理 " + batch.size() + " 条消息");
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    
    for (int i = 0; i < batch.size(); i++) {
        Object data = batch.get(i);
        String senderUin = data.userUin != null ? data.userUin.trim() : "";
        
        String msgTypeStr = getMsgType(data);
        
        /**
         * 优化后的日期判断：
         * 直接使用消息时间戳计算日期，解决跨天问题
         */
        String msgDateStr;
        try {
            long msgTimeSec = data.time;
            if (msgTimeSec > 0) {
                long msgTimeMs = msgTimeSec * 1000;
                msgDateStr = sdf.format(new Date(msgTimeMs));
            } else {
                msgDateStr = todayDateStr;
            }
        } catch (Exception e) {
            msgDateStr = todayDateStr;
        }
        
        String dateKeyPrefix = "date_" + msgDateStr + "_";
        boolean isSend = myUin.equals(senderUin);
        long wordCount = 0;
        
        if ("text".equals(msgTypeStr)) {
            wordCount = getPureWordCount(data);
        }

        if (isSend) {
            atomicIncrement(dateKeyPrefix + "Send");
            atomicIncrement("totalSend");
            
            if ("text".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendText");
                atomicIncrement("totalSendText");
                if (wordCount > 0) {
                    atomicAdd(dateKeyPrefix + "SendWordCount", wordCount);
                    atomicAdd("totalSendWordCount", wordCount);
                }
            } else if ("pic".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendPic");
                atomicIncrement("totalSendPic");
            } else if ("file".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendFile");
                atomicIncrement("totalSendFile");
            } else if ("video".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendVideo");
                atomicIncrement("totalSendVideo");
            } else if ("emoji".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendEmoji");
                atomicIncrement("totalSendEmoji");
            } else if ("audio".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendAudio");
                atomicIncrement("totalSendAudio");
            } else if ("card".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendCard");
                atomicIncrement("totalSendCard");
            } else if ("call".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "SendCall");
                atomicIncrement("totalSendCall");
            } else {
                traceLog("api3_log.txt", "消息类型未知，归类为Unknown");
                atomicIncrement(dateKeyPrefix + "SendUnknown");
                atomicIncrement("totalSendUnknown");
            }
        } else {
            atomicIncrement(dateKeyPrefix + "Receive");
            atomicIncrement("totalReceive");
            
            if ("text".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveText");
                atomicIncrement("totalReceiveText");
                if (wordCount > 0) {
                    atomicAdd(dateKeyPrefix + "ReceiveWordCount", wordCount);
                    atomicAdd("totalReceiveWordCount", wordCount);
                }
            } else if ("pic".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceivePic");
                atomicIncrement("totalReceivePic");
            } else if ("file".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveFile");
                atomicIncrement("totalReceiveFile");
            } else if ("video".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveVideo");
                atomicIncrement("totalReceiveVideo");
            } else if ("emoji".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveEmoji");
                atomicIncrement("totalReceiveEmoji");
            } else if ("audio".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveAudio");
                atomicIncrement("totalReceiveAudio");
            } else if ("card".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveCard");
                atomicIncrement("totalReceiveCard");
            } else if ("call".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveCall");
                atomicIncrement("totalReceiveCall");
            } else if ("grayTip".equals(msgTypeStr)) {
                atomicIncrement(dateKeyPrefix + "ReceiveGrayTip");
                atomicIncrement("totalReceiveGrayTip");
            } else {
                traceLog("api3_log.txt", "消息类型未知，归类为Unknown");
                atomicIncrement(dateKeyPrefix + "ReceiveUnknown");
                atomicIncrement("totalReceiveUnknown");
            }
        }
    }
    
    triggerUIUpdate();
}

/**
 * 消息接收回调入口
 * @param data 消息数据对象
 */
public void onMsg(Object data) {

    
    try { dispatchEvent(data, 1); 
        log大小限制(logPath);
        } catch (Throwable e) { traceLog("function_log", "[onMsg]" + e); }

    if (!getBoolean("settings", "消息统计开关", true)) return;

    synchronized(writeLock) {
        messageBatchQueue.add(data);
        writeLock.notifyAll();
    }
    
    int queueSize = messageBatchQueue.size();
    if (queueSize > 50) {
        traceLog("api3_log.txt", "队列积压: " + queueSize + " 条");
    }
}

/**
 * 写入变更的统计数据
 */
private void writeStats() {
    if (CHANGED_KEYS.isEmpty() || configName == null || configName.isEmpty()) {
        return;
    }

    Vector keysToWrite = null;
    synchronized(writeLock) {
        keysToWrite = new Vector(CHANGED_KEYS);
    }

    if (keysToWrite != null && !keysToWrite.isEmpty()) {
        writeFullStatsInternal(keysToWrite);
        
        synchronized(writeLock) {
            CHANGED_KEYS.removeAll(keysToWrite);
            traceLog("api3_log.txt", "成功写入" + keysToWrite.size() + "个键，剩余" + CHANGED_KEYS.size() + "个键待写入");
        }
    }
}

/**
 * 强制全量持久化
 */
private void writeFullStats() {
    traceLog("api3_log.txt", "用户触发强制全量持久化");
    synchronized(writeLock) {
        writeFullStatsInternal(new Vector());
    }
}

/**
 * 内部写入统计数据实现
 * @param triggeredKeys 触发写入的键列表
 */
private void writeFullStatsInternal(Vector triggeredKeys) {
    JSONObject statsJson = new JSONObject();
    Iterator iterator = OP_STATS.entrySet().iterator();
    while (iterator.hasNext()) {
        Map.Entry entry = (Map.Entry) iterator.next();
        Long value = (Long) entry.getValue();
        statsJson.put((String) entry.getKey(), value.longValue());
    }
    String jsonStr = statsJson.toString(2);

    File mainFile = new File(configName);
    File tempFile = new File(configName + TEMP_FILE_SUFFIX);
    
    File parentDir = mainFile.getParentFile();
    if (!parentDir.exists() && !parentDir.mkdirs()) {
        traceLog("api3_log.txt", "创建目录失败: " + parentDir.getAbsolutePath());
        return;
    }

    FileOutputStream fos = null;
    OutputStreamWriter osw = null;
    try {
        fos = new FileOutputStream(tempFile);
        osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        osw.write(jsonStr);
        osw.flush();
        fos.getFD().sync();
        osw.close();

        if (mainFile.exists() && !mainFile.delete()) {
            traceLog("api3_log.txt", "删除原文件失败: " + mainFile.getName());
            tempFile.delete();
            return;
        }
        if (!tempFile.renameTo(mainFile)) {
            traceLog("api3_log.txt", "重命名失败: " + tempFile.getName());
            tempFile.delete();
            return;
        }

        if (mainFile.exists() && mainFile.length() > 0) {
            traceLog("api3_log.txt", "文件写入完成: " + mainFile.getName() + " 大小=" + mainFile.length() + "字节");
            
            String verifyContent = 读(mainFile.getAbsolutePath());
            if (verifyContent != null && verifyContent.contains("totalSend") && verifyContent.contains("totalReceive")) {
                traceLog("api3_log.txt", "文件完整性验证通过");
            } else {
                traceLog("api3_log.txt", "文件完整性验证失败！");
            }
        } else {
            traceLog("api3_log.txt", "文件不存在或大小为0！");
        }
        
        createSingleBackup();
        
    } catch (Exception e) {
        traceLog("api3_log.txt", "写入失败: " + e.getMessage());
        if (tempFile != null) tempFile.delete();
    } finally {
        if (osw != null) try { osw.close(); } catch (Exception e) {}
        if (fos != null) try { fos.close(); } catch (Exception e) {}
    }
}

/**
 * 重新计算所有累计统计
 * 用于修复累计数据小于当天数据的情况
 */
private void recalculateTotalStats() {
    traceLog("api3_log.txt", "重新计算所有total统计");
    for (int i = 0; i < STAT_TYPES.length; i++) {
        String type = STAT_TYPES[i];
        long total = 0L;
        String keySuffix = "_" + type;
        
        List snapshot = new ArrayList(OP_STATS.entrySet());
        Iterator iterator = snapshot.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            if (key.startsWith("date_") && key.endsWith(keySuffix)) {
                total += ((Long) entry.getValue()).longValue();
            }
        }
        
        OP_STATS.put("total" + type, new Long(total));
        addChangedKey("total" + type);
        traceLog("api3_log.txt", "type=" + type + " total=" + total);
    }
    traceLog("api3_log.txt", "重新计算结束");
}

/**
 * 创建单一备份文件
 */
private void createSingleBackup() {
    File mainFile = new File(configName);
    if (!mainFile.exists() || mainFile.length() == 0) return;
    
    File backupFile = new File(configName + ".bak");
    if (backupFile.exists()) backupFile.delete();
    
    try {
        java.nio.file.Files.copy(mainFile.toPath(), backupFile.toPath());
        traceLog("api3_log.txt", "备份文件已创建");
    } catch (Exception e) {
        traceLog("api3_log.txt", "备份失败: " + e.getMessage());
    }
}

/**
 * 触发UI更新
 */
private void triggerUIUpdate() {
    if (!dialogVisible || statsDialog == null) {
        return;
    }
    
    try {
        if (!statsDialog.isShowing()) {
            return;
        }
    } catch (Exception e) {
        traceLog("api3_log.txt", "对话框状态检查失败: " + e.getMessage());
        return;
    }
    
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastUIUpdateTime < MIN_UI_UPDATE_INTERVAL) {
        msgHandle.removeCallbacksAndMessages(null);
        msgHandle.postDelayed(new Runnable() {
            public void run() {
                traceLog("api3_log.txt", "防抖延迟刷新");
                updateUIImmediately();
            }
        }, MIN_UI_UPDATE_INTERVAL);
        return;
    }
    lastUIUpdateTime = currentTime;
    
    traceLog("api3_log.txt", "触发UI刷新");
    msgHandle.post(new Runnable() {
        public void run() {
            try {
                updateUIImmediately();
                traceLog("api3_log.txt", "UI刷新成功");
            } catch (Throwable e) {
                traceLog("api3_log.txt", "UI更新失败: " + e.getMessage());
                traceLog("api3_log.txt","" + e);
            }
        }
    });
}

/**
 * 立即更新UI
 */
private void updateUIImmediately() {
    if (statsDialog == null || !statsDialog.isShowing()) {
        traceLog("api3_log.txt", "对话框无效");
        return;
    }
    
    final String rangeName;
    if (currentTimeRange == TimeRange.CUSTOM_DATE && customDateStr != null) {
        rangeName = formatDateForDisplay(customDateStr);
    } else {
        rangeName = TimeRange.toString(currentTimeRange);
    }
    
    final long todaySend = atomicGet("date_" + todayDateStr + "_Send");
    final int dailyTarget = getDailyTargetFromPrefs();
    final long totalReceive = atomicGet("totalReceive");
    final long totalSend = atomicGet("totalSend");

    updateCachedTextView("totalReceive", "累计: " + formatStatValue(totalReceive));
    updateCachedTextView("totalSend", "累计: " + formatStatValue(totalSend));
    updateCachedTextView("Receive", rangeName + ": " + formatStatValue(getStatsByTimeRange("Receive")));
    updateCachedTextView("Send", rangeName + ": " + formatStatValue(getStatsByTimeRange("Send")));

    String[] typeKeys = {
        "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo", "ReceiveEmoji",
        "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip", "ReceiveWordCount", "ReceiveUnknown",
        "SendText", "SendPic", "SendFile", "SendVideo", "SendEmoji",
        "SendAudio", "SendCard", "SendCall", "SendWordCount", "SendUnknown"
    };
    for (int i = 0; i < typeKeys.length; i++) {
        String key = typeKeys[i];
        long rangeVal = getStatsByTimeRange(key);
        long totalVal = atomicGet("total" + key);
        updateCachedTextView(key, rangeName + ": " + formatStatValue(rangeVal) + "  累计: " + formatStatValue(totalVal));
    }

    updateTodayProgress(todaySend, dailyTarget);
}

/**
 * 更新缓存的TextView
 * @param tag TextView标签
 * @param text 要设置的文本
 */
private void updateCachedTextView(String tag, String text) {
    if (tag == null || text == null) return;
    
    TextView tv = null;
    for (int i = 0; i < statsTextViewCache.size(); i++) {
        Object item = statsTextViewCache.get(i);
        if (item instanceof TextView && tag.equals(((TextView) item).getTag())) {
            tv = (TextView) item;
            break;
        }
    }
    
    if (tv == null) {
        View rootView = statsDialog.getWindow().getDecorView();
        if (rootView != null) {
            tv = (TextView) rootView.findViewWithTag(tag);
            if (tv != null) {
                statsTextViewCache.add(tv);
            } else {
                traceLog("api3_log.txt", "未找到Tag=" + tag + "的TextView");
                return;
            }
        } else {
            traceLog("api3_log.txt", "rootView为null");
            return;
        }
    }
    if (!text.equals(tv.getText().toString())) {
        tv.setText(text);
        traceLog("api3_log.txt", "更新Tag=" + tag + " 文本=" + text);
    }
}

/**
 * 更新今日进度显示
 * @param todaySend 今日发送数
 * @param dailyTarget 每日目标
 */
private void updateTodayProgress(long todaySend, int dailyTarget) {
    if (todayCoreCardCache == null) {
        traceLog("api3_log.txt", "todayCoreCardCache为null");
        return;
    }
    
    int progress = (int) Math.min(todaySend * 100 / Math.max(dailyTarget, 1), 100);
    int remaining = Math.max(dailyTarget - (int) todaySend, 0);
    traceLog("api3_log.txt", "todaySend=" + todaySend + " dailyTarget=" + dailyTarget + " progress=" + progress);

    if (sendTargetLabelCache != null) {
        sendTargetLabelCache.setText("今天已经逼逼了" + todaySend + "句，还差" + remaining + " 句，目标：" + dailyTarget + "(" + progress + "%)");
    }

    if (progressBarCache != null && todayCoreCardCache.getWidth() > 0) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) progressBarCache.getLayoutParams();
        params.width = (int) (todayCoreCardCache.getWidth() * progress / 100.0f);
        progressBarCache.setLayoutParams(params);
        traceLog("api3_log.txt", "进度条宽度更新: " + params.width);
    }

    if (achievementTextCache != null) {
        achievementTextCache.setVisibility(progress >= 100 ? View.VISIBLE : View.GONE);
        traceLog("api3_log.txt", "成就显示: " + (progress >= 100 ? "显示" : "隐藏"));
    }
}

/**
 * 根据时间范围获取统计数据
 * @param typeKey 统计类型键
 * @return 统计值
 */
private long getStatsByTimeRange(String typeKey) {
    long total = 0L;
    String datePrefix = "date_";
    String keySuffix = "_" + typeKey;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);

    if (currentTimeRange == TimeRange.TODAY) {
        return atomicGet(datePrefix + todayDateStr + keySuffix);
    } else if (currentTimeRange == TimeRange.YESTERDAY) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String yesterday = dateFormat.format(cal.getTime());
        return atomicGet(datePrefix + yesterday + keySuffix);
    } else if (currentTimeRange == TimeRange.THIS_WEEK) {
        String cacheKey = todayDateStr + "_week";
        if (!cacheKey.equals(weekCacheKey) || weekDatesCache.isEmpty()) {
            weekDatesCache.clear();
            Calendar cal = Calendar.getInstance();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String today = getTodayDateStr();

            while (true) {
                String currentDate = dateFormat.format(cal.getTime());
                if (currentDate.compareTo(today) > 0) break;
                weekDatesCache.add(currentDate);
                cal.add(Calendar.DATE, 1);
            }
            weekCacheKey = cacheKey;
        }

        for (int i = 0; i < weekDatesCache.size(); i++) {
            String date = (String) weekDatesCache.get(i);
            total += atomicGet(datePrefix + date + keySuffix);
        }
    } else if (currentTimeRange == TimeRange.THIS_MONTH) {
        String cacheKey = todayDateStr.substring(0, 6) + "_month";
        if (!cacheKey.equals(monthCacheKey) || monthDatesCache.isEmpty()) {
            monthDatesCache.clear();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String today = getTodayDateStr();
            String monthPrefix = today.substring(0, 6);

            while (true) {
                String currentDate = dateFormat.format(cal.getTime());
                if (!currentDate.substring(0, 6).equals(monthPrefix)) break;
                monthDatesCache.add(currentDate);
                cal.add(Calendar.DATE, 1);
            }
            monthCacheKey = cacheKey;
        }

        for (int i = 0; i < monthDatesCache.size(); i++) {
            String date = (String) monthDatesCache.get(i);
            total += atomicGet(datePrefix + date + keySuffix);
        }
    } else if (currentTimeRange == TimeRange.CUSTOM_DATE) {
        if (customDateStr == null) {
            customDateStr = getTodayDateStr();
        }
        return atomicGet(datePrefix + customDateStr + keySuffix);
    }
    return total;
}

/**
 * 创建时间范围选择器（Spinner样式，美化版）
 * @param activity Activity上下文
 * @return 包含选择器的LinearLayout
 */
private View createRangeSpinner(Activity activity) {
    final Activity finalActivity = activity;
    final boolean isDark = isThemeDark(activity);
    
    final int textColor = isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333");
    final int accentColor = isDark ? Color.parseColor("#8AB4F8") : Color.parseColor("#2196F3");
    final int cardBgColor = isDark ? Color.parseColor("#FF2D2D2D") : Color.WHITE;
    
    final int capsuleBgNormal = isDark ? Color.parseColor("#1AFFFFFF") : Color.parseColor("#F2F2F7");
    final int capsuleBgPressed = isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#E5E5EA");
    
    final int dropdownBgColor = isDark ? Color.parseColor("#FF383838") : Color.WHITE;

    LinearLayout layout = new LinearLayout(finalActivity);
    layout.setGravity(Gravity.CENTER_VERTICAL);
    layout.setBackground(createRoundRectDrawable(finalActivity, cardBgColor, 16));
    layout.setPadding(dp(finalActivity, 16), dp(finalActivity, 14), dp(finalActivity, 16), dp(finalActivity, 14));
    
    layout.setClipChildren(false); 
    layout.setClipToPadding(false);

    TextView label = new TextView(finalActivity);
    label.setText("📅  时间范围：");
    label.setTextColor(textColor);
    label.setTextSize(15);
    label.setTypeface(null, Typeface.BOLD);
    layout.addView(label);

    View spacer = new View(finalActivity);
    layout.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

    Spinner spinner = new Spinner(finalActivity, Spinner.MODE_DROPDOWN);
    String[] rangeNames = {"今日", "昨日", "本周", "本月", "自定义日期"};

    spinner.setPadding(0, 0, 0, 0);

    StateListDrawable capsuleDrawable = new StateListDrawable();
    
    GradientDrawable pressedBg = new GradientDrawable();
    pressedBg.setColor(capsuleBgPressed);
    pressedBg.setCornerRadius(dp(finalActivity, 20)); 
    capsuleDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
    
    GradientDrawable normalBg = new GradientDrawable();
    normalBg.setColor(capsuleBgNormal);
    normalBg.setCornerRadius(dp(finalActivity, 20)); 
    capsuleDrawable.addState(new int[]{}, normalBg);

    LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(dp(finalActivity, 120), dp(finalActivity, 36));
    
    spinnerParams.setMargins(0, 0, 0, 0); 
    spinnerParams.leftMargin = -dp(finalActivity, 2); 
    
    spinner.setLayoutParams(spinnerParams);
    spinner.setBackground(capsuleDrawable);
    
    try {
        GradientDrawable popupBg = new GradientDrawable();
        popupBg.setColor(dropdownBgColor);
        popupBg.setCornerRadius(dp(finalActivity, 12));
        spinner.setPopupBackgroundDrawable(popupBg);
    } catch(Throwable e) {}

    try {
        ArrayAdapter adapter = new ArrayAdapter(finalActivity, android.R.layout.simple_spinner_item, rangeNames) {
            
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = new TextView(finalActivity);
                
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                
                tv.setGravity(Gravity.CENTER);
                
                tv.setTextSize(13);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setText(getItem(position));
                
                tv.setPadding(0, 0, 0, 0);
                tv.setIncludeFontPadding(false);

                if (position == currentTimeRange) {
                    tv.setTextColor(accentColor);
                } else {
                    tv.setTextColor(textColor);
                }
                return tv;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = new TextView(finalActivity);
                tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tv.setTextSize(14);
                tv.setText(getItem(position));
                tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                
                int padV = dp(finalActivity, 12);
                int padH = dp(finalActivity, 16);
                tv.setPadding(padH, padV, padH, padV);
                
                if (position == currentTimeRange) {
                    tv.setTextColor(accentColor);
                    tv.setTypeface(null, Typeface.BOLD);
                    tv.setText("✓  " + getItem(position));
                } else {
                    tv.setTextColor(textColor);
                    tv.setTypeface(null, Typeface.NORMAL);
                    tv.setText("    " + getItem(position));
                }
                tv.setBackgroundColor(Color.TRANSPARENT);
                return tv;
            }
        };
        spinner.setAdapter(adapter);
    } catch (Throwable e) {
        ArrayAdapter adapter = new ArrayAdapter(finalActivity, android.R.layout.simple_spinner_item, rangeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    int selectedPos = 0;
    if (currentTimeRange == TimeRange.YESTERDAY) selectedPos = 1;
    else if (currentTimeRange == TimeRange.THIS_WEEK) selectedPos = 2;
    else if (currentTimeRange == TimeRange.THIS_MONTH) selectedPos = 3;
    else if (currentTimeRange == TimeRange.CUSTOM_DATE) selectedPos = 4;
    spinner.setSelection(selectedPos);

    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView parent, View view, int position, long id) {
            int selected = TimeRange.TODAY;
            boolean needShowDatePicker = false;
            switch (position) {
                case 1: selected = TimeRange.YESTERDAY; break;
                case 2: selected = TimeRange.THIS_WEEK; break;
                case 3: selected = TimeRange.THIS_MONTH; break;
                case 4: 
                    selected = TimeRange.CUSTOM_DATE;
                    needShowDatePicker = true;
                    break;
            }

            if (currentTimeRange != selected || needShowDatePicker) {
                currentTimeRange = selected;
                SharedPreferences sp = finalActivity.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
                sp.edit().putInt(SP_TIME_RANGE_KEY, position).apply();
                
                if (selected == TimeRange.THIS_WEEK) {
                    weekDatesCache.clear();
                    weekCacheKey = "";
                } else if (selected == TimeRange.THIS_MONTH) {
                    monthDatesCache.clear();
                    monthCacheKey = "";
                }
                
                if (needShowDatePicker) {
                    showDatePickerDialog(finalActivity);
                }
                
                triggerUIUpdate();
            }
        }
        public void onNothingSelected(AdapterView parent) {}
    });

    layout.addView(spinner);
    return layout;
}

/**
 * 显示日期选择对话框（保持原有DatePicker，美化样式）
 * @param activity Activity上下文
 */
private void showDatePickerDialog(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);

    final Calendar calendar = Calendar.getInstance();
    if (customDateStr != null) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
            calendar.setTime(sdf.parse(customDateStr));
        } catch (Exception e) {}
    }

    DatePicker datePicker = new DatePicker(activity);
    datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null);

    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(dp(activity, 20), dp(activity, 10), dp(activity, 20), dp(activity, 10));
    container.addView(datePicker);

    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("选择日期")
        .setView(container)
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int year = datePicker.getYear();
                int month = datePicker.getMonth() + 1;
                int day = datePicker.getDayOfMonth();
                customDateStr = String.format(Locale.CHINA, "%04d%02d%02d", year, month, day);
                
                SharedPreferences sp = activity.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
                sp.edit().putString(SP_CUSTOM_DATE_KEY, customDateStr).apply();
                
                traceLog("api3_log.txt", "用户选择日期: " + customDateStr);
                Toast("已选择日期: " + formatDateForDisplay(customDateStr));
                triggerUIUpdate();
            }
        })
        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                currentTimeRange = TimeRange.TODAY;
                SharedPreferences sp = activity.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
                sp.edit().putInt(SP_TIME_RANGE_KEY, 0).apply();
                triggerUIUpdate();
            }
        })
        .show();
}

/**
 * 显示目标设置对话框
 * @param activity Activity上下文
 */
private void showTargetSettingDialog(Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);

    SharedPreferences sp = activity.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
    int currentTarget = sp.getInt(DAILY_TARGET_KEY, DEFAULT_DAILY_TARGET);

    LinearLayout dialogLayout = new LinearLayout(activity);
    dialogLayout.setOrientation(LinearLayout.VERTICAL);
    dialogLayout.setPadding(dp(activity, 24), dp(activity, 16), dp(activity, 24), dp(activity, 16));

    EditText editText = new EditText(activity);
    editText.setHint("输入每日消息目标（默认100）");
    editText.setText(String.valueOf(currentTarget));
    editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    editText.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    editText.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#F5F5F5"), 8));
    editText.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.BLACK);
    editText.setHintTextColor(isDark ? Color.parseColor("#AAAAAA") : Color.GRAY);
    editText.setTextSize(14);
    dialogLayout.addView(editText);

    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("设置每日消息目标")
        .setView(dialogLayout)
        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String input = editText.getText().toString().trim();
                if (!input.isEmpty()) {
                    try {
                        int target = Integer.parseInt(input);
                        if (target > 0) {
                            sp.edit().putInt(DAILY_TARGET_KEY, target).apply();
                            traceLog("api3_log.txt", "用户设置目标: " + target);
                            Toast("目标设置成功：" + target + "条/天");
                            triggerUIUpdate();
                        } else {
                            Toast("目标需大于0");
                        }
                    } catch (NumberFormatException e) {
                        Toast("请输入有效数字");
                    }
                } else {
                    Toast("请输入有效数字");
                }
            }
        })
        .setNegativeButton("取消", null)
        .show();
}

/**
 * 创建统计卡片基础布局
 * @param activity Activity上下文
 * @param title 卡片标题
 * @param cardType 卡片类型标识
 * @return LinearLayout卡片
 */
private LinearLayout createStatsCardBase(Activity activity, String title, String cardType) {
    boolean isDark = isThemeDark(activity);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#FF2D2D2D") : Color.WHITE, 12));
    card.setTag(cardType);
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    LinearLayout titleBar = new LinearLayout(activity);
    titleBar.setOrientation(LinearLayout.HORIZONTAL);
    titleBar.setGravity(Gravity.CENTER_VERTICAL);
    titleBar.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            Boolean current = (Boolean) cardExpandStatus.get(cardType);
            boolean isExpanded = current != null && current;
            cardExpandStatus.put(cardType, !isExpanded);
            updateCardExpandStatus(card, !isExpanded);
        }
    });

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333"));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), android.graphics.Typeface.BOLD);
    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    cardTitle.setLayoutParams(titleParams);
    titleBar.addView(cardTitle);

    TextView arrowTv = new TextView(activity);
    arrowTv.setText("▶");
    arrowTv.setTextSize(14);
    arrowTv.setTextColor(isDark ? Color.parseColor("#AAAAAA") : Color.parseColor("#666666"));
    arrowTv.setTag(cardType + "_arrow");
    titleBar.addView(arrowTv);

    card.addView(titleBar);

    LinearLayout statsContainer = new LinearLayout(activity);
    statsContainer.setOrientation(LinearLayout.VERTICAL);
    statsContainer.setTag(cardType + "_container");
    statsContainer.setVisibility(View.GONE);
    card.addView(statsContainer);

    cardExpandStatus.put(cardType, false);
    return card;
}

/**
 * 更新卡片展开状态
 * @param card 卡片布局
 * @param isExpanded 是否展开
 */
private void updateCardExpandStatus(LinearLayout card, boolean isExpanded) {
    String cardType = (String) card.getTag();
    TextView arrowTv = (TextView) card.findViewWithTag(cardType + "_arrow");
    LinearLayout container = (LinearLayout) card.findViewWithTag(cardType + "_container");
    if (arrowTv != null && container != null) {
        arrowTv.animate().rotation(isExpanded ? 90 : 0).setDuration(300).start();
        if (isExpanded) {
            container.setVisibility(View.VISIBLE);
            Animation expandAnim = new ExpandAnimation(container, 300);
            container.startAnimation(expandAnim);
        } else {
            Animation collapseAnim = new CollapseAnimation(container, 300);
            collapseAnim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {}
                public void onAnimationEnd(Animation animation) {
                    container.setVisibility(View.GONE);
                }
                public void onAnimationRepeat(Animation animation) {}
            });
            container.startAnimation(collapseAnim);
        }
        cardExpandStatus.put(cardType, isExpanded);
        traceLog("api3_log.txt", cardType + " 展开状态: " + isExpanded);
    }
}

/**
 * 展开动画类
 */
class ExpandAnimation extends Animation {
    private final View view;
    private final int targetHeight;

    public ExpandAnimation(View view, int duration) {
        this.view = view;
        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.targetHeight = view.getMeasuredHeight();
        setDuration(duration);
    }

    protected void applyTransformation(float interpolatedTime, Transformation t) {
        view.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
        view.requestLayout();
    }

    public boolean willChangeBounds() {
        return true;
    }
}

/**
 * 折叠动画类
 */
class CollapseAnimation extends Animation {
    private final View view;
    private final int startHeight;

    public CollapseAnimation(View view, int duration) {
        this.view = view;
        this.startHeight = view.getHeight();
        setDuration(duration);
    }

    protected void applyTransformation(float interpolatedTime, Transformation t) {
        view.getLayoutParams().height = startHeight - (int) (startHeight * interpolatedTime);
        view.requestLayout();
    }

    public boolean willChangeBounds() {
        return true;
    }
}

/**
 * 创建类型统计项布局
 * @param activity Activity上下文
 * @param label 标签文字
 * @param typeKey 类型键
 * @param colorIndex 颜色索引
 * @return LinearLayout项
 */
private LinearLayout createTypeStatsItemLayout(Activity activity, String label, String typeKey, int colorIndex) {
    boolean isDark = isThemeDark(activity);
    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(0, dp(activity, 6), 0, dp(activity, 6));

    TextView iconTv = new TextView(activity);
    iconTv.setText(label.substring(0, 2));
    iconTv.setTextSize(16);
    iconTv.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 30), dp(activity, 30)));

    TextView labelTv = new TextView(activity);
    labelTv.setText(label.substring(3));
    labelTv.setTextColor(isDark ? Color.parseColor("#AAAAAA") : Color.parseColor("#666666"));
    labelTv.setTextSize(14);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

    TextView statsTv = new TextView(activity);
    long rangeValue = getStatsByTimeRange(typeKey);
    long totalValue = atomicGet("total" + typeKey);
    statsTv.setText(TimeRange.toString(currentTimeRange) + ": " + formatStatValue(rangeValue) + "  累计: " + formatStatValue(totalValue));
    statsTv.setTextColor(getColorForStat(colorIndex));
    statsTv.setTextSize(14);
    statsTv.setTag(typeKey);

    itemLayout.addView(iconTv);
    itemLayout.addView(labelTv);
    itemLayout.addView(statsTv);
    return itemLayout;
}

/**
 * 创建统计项布局
 * @param activity Activity上下文
 * @param label 标签文字
 * @param key 统计键
 * @param colorIndex 颜色索引
 * @return LinearLayout项
 */
private LinearLayout createStatsItemLayout(Activity activity, String label, String key, int colorIndex) {
    boolean isDark = isThemeDark(activity);
    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(0, dp(activity, 6), 0, dp(activity, 6));

    TextView iconTv = new TextView(activity);
    iconTv.setText(label.substring(0, 2));
    iconTv.setTextSize(16);
    iconTv.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 30), dp(activity, 30)));

    TextView labelTv = new TextView(activity);
    labelTv.setText(label.substring(3));
    labelTv.setTextColor(isDark ? Color.parseColor("#AAAAAA") : Color.parseColor("#666666"));
    labelTv.setTextSize(14);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(
        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

    long value;
    String textPrefix;
    if (key.startsWith("total")) {
        value = atomicGet(key);
        textPrefix = "累计: ";
    } else {
        value = getStatsByTimeRange(key);
        textPrefix = TimeRange.toString(currentTimeRange) + ": ";
    }

    TextView valueTv = new TextView(activity);
    valueTv.setText(textPrefix + formatStatValue(value));
    valueTv.setTextColor(getColorForStat(colorIndex));
    valueTv.setTextSize(16);
    valueTv.setTypeface(valueTv.getTypeface(), android.graphics.Typeface.BOLD);
    valueTv.setTag(key);

    itemLayout.addView(iconTv);
    itemLayout.addView(labelTv);
    itemLayout.addView(valueTv);
    return itemLayout;
}

/**
 * 创建今日核心统计卡片
 * @param activity Activity上下文
 * @param title 卡片标题
 * @return LinearLayout卡片
 */
private LinearLayout createTodayCoreStatsCard(Activity activity, String title) {
    boolean isDark = isThemeDark(activity);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#FF2D2D2D") : Color.WHITE, 12));
    card.setTag("todayCoreCard");
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTag("core_card_title");
    cardTitle.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333"));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), android.graphics.Typeface.BOLD);
    card.addView(cardTitle);

    String[][] statsItems = {
        {"📥 今日接收总数", "Receive"},
        {"📤 今日发送总数", "Send"}
    };
    for (int i = 0; i < statsItems.length; i++) {
        LinearLayout itemLayout = createStatsItemLayout(activity, statsItems[i][0], statsItems[i][1], i);
        card.addView(itemLayout);
        if (i < statsItems.length - 1) {
            card.addView(createDivider(activity));
        }
    }

    card.addView(createDivider(activity));

    LinearLayout sendTargetLayout = new LinearLayout(activity);
    sendTargetLayout.setOrientation(LinearLayout.VERTICAL);
    sendTargetLayout.setPadding(0, dp(activity, 10), 0, 0);

    TextView sendTargetLabel = new TextView(activity);
    sendTargetLabel.setTag("send_target_label");

    long todaySend = atomicGet("date_" + todayDateStr + "_Send");
    Activity act = getNowActivity();
    if (act != null) {
        SharedPreferences sp = act.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE);
        int dailyTarget = sp.getInt(DAILY_TARGET_KEY, DEFAULT_DAILY_TARGET);
        int progress = (int) Math.min(todaySend * 100 / Math.max(dailyTarget, 1), 100);
        sendTargetLabel.setText("今天已经逼逼了" + todaySend + "句，还差" + (dailyTarget - todaySend) + " 句，目标：" + dailyTarget + "(" + progress + "%)");
    } else {
        sendTargetLabel.setText("正在加载目标数据...");
    }

    sendTargetLabel.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333"));
    sendTargetLabel.setTextSize(14);
    sendTargetLayout.addView(sendTargetLabel);

    View progressBg = new View(activity);
    progressBg.setTag("send_progress_bg");
    progressBg.setBackgroundColor(isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#E0E0E0"));
    LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 8));
    bgParams.setMargins(0, dp(activity, 8), 0, 0);
    progressBg.setLayoutParams(bgParams);
    sendTargetLayout.addView(progressBg);

    View progressBar = new View(activity);
    progressBar.setTag("send_progress");
    progressBar.setBackgroundColor(Color.parseColor("#81C784"));
    LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(0, dp(activity, 8));
    progressParams.setMargins(0, -dp(activity, 8), 0, 0);
    progressBar.setLayoutParams(progressParams);
    sendTargetLayout.addView(progressBar);

    TextView achievementText = new TextView(activity);
    achievementText.setTag("achievement_text");
    achievementText.setText("🎉 今日达成成就：屁话王");
    achievementText.setTextColor(Color.parseColor("#FF6B6B"));
    achievementText.setTextSize(14);
    achievementText.setTypeface(achievementText.getTypeface(), android.graphics.Typeface.BOLD);
    LinearLayout.LayoutParams achievementParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    achievementParams.setMargins(0, dp(activity, 8), 0, 0);
    achievementText.setLayoutParams(achievementParams);
    achievementText.setVisibility(View.GONE);
    sendTargetLayout.addView(achievementText);

    card.addView(sendTargetLayout);
    return card;
}

/**
 * 创建累计统计卡片
 * @param activity Activity上下文
 * @param title 卡片标题
 * @return LinearLayout卡片
 */
private LinearLayout createTotalStatsCard(Activity activity, String title) {
    boolean isDark = isThemeDark(activity);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#FF2D2D2D") : Color.WHITE, 12));
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333"));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), android.graphics.Typeface.BOLD);
    card.addView(cardTitle);

    String[][] statsItems = {
        {"📥 累计接收总数", "totalReceive"},
        {"📤 累计发送总数", "totalSend"}
    };
    for (int i = 0; i < statsItems.length; i++) {
        LinearLayout itemLayout = createStatsItemLayout(activity, statsItems[i][0], statsItems[i][1], i);
        card.addView(itemLayout);
        if (i < statsItems.length - 1) {
            card.addView(createDivider(activity));
        }
    }
    return card;
}

/**
 * 创建接收消息统计卡片
 * @param activity Activity上下文
 * @param title 卡片标题
 * @param cardType 卡片类型标识
 * @return LinearLayout卡片
 */
private LinearLayout createReceiveStatsCard(Activity activity, String title, String cardType) {
    LinearLayout card = createStatsCardBase(activity, title, cardType);

    String[][] statsItems = {
        {"💬 文本", "ReceiveText"},
        {"🖼️ 图片", "ReceivePic"},
        {"📎 文件", "ReceiveFile"},
        {"🎬 视频", "ReceiveVideo"},
        {"😊 表情", "ReceiveEmoji"},
        {"🎙️ 语音", "ReceiveAudio"},
        {"🍭 卡片", "ReceiveCard"},
        {"📞 通话", "ReceiveCall"},
        {"📝 灰字提示", "ReceiveGrayTip"},
        {"📝 字数", "ReceiveWordCount"},
        {"🤔 未知", "ReceiveUnknown"}
    };

    LinearLayout statsContainer = (LinearLayout) card.findViewWithTag(cardType + "_container");
    for (int i = 0; i < statsItems.length; i++) {
        LinearLayout itemLayout = createTypeStatsItemLayout(activity, statsItems[i][0], statsItems[i][1], i);
        statsContainer.addView(itemLayout);
        if (i < statsItems.length - 1) {
            statsContainer.addView(createDivider(activity));
        }
    }

    return card;
}

/**
 * 创建发送消息统计卡片
 * @param activity Activity上下文
 * @param title 卡片标题
 * @param cardType 卡片类型标识
 * @return LinearLayout卡片
 */
private LinearLayout createSendStatsCard(Activity activity, String title, String cardType) {
    LinearLayout card = createStatsCardBase(activity, title, cardType);

    String[][] statsItems = {
        {"💬 文本", "SendText"},
        {"🖼️ 图片", "SendPic"},
        {"📎 文件", "SendFile"},
        {"🎬 视频", "SendVideo"},
        {"😊 表情", "SendEmoji"},
        {"🎙️ 语音", "SendAudio"},
        {"🍭 卡片", "SendCard"},
        {"📞 通话", "SendCall"},
        {"📝 字数", "SendWordCount"},
        {"🤔 未知", "SendUnknown"}
    };

    LinearLayout statsContainer = (LinearLayout) card.findViewWithTag(cardType + "_container");
    for (int i = 0; i < statsItems.length; i++) {
        LinearLayout itemLayout = createTypeStatsItemLayout(activity, statsItems[i][0], statsItems[i][1], i);
        statsContainer.addView(itemLayout);
        if (i < statsItems.length - 1) {
            statsContainer.addView(createDivider(activity));
        }
    }

    return card;
}

/**
 * 重置今日统计数据
 * @param activity Activity上下文
 */
private void resetTodayStats(final Activity activity) {
    traceLog("api3_log.txt", "用户请求重置今日数据");
    boolean isDark = isThemeDark(activity);
    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("重置确认")
        .setMessage("确定重置今日统计数据吗？")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                vibrate(activity, 48);
                String todayPrefix = "date_" + todayDateStr + "_";
                synchronized(writeLock) {
                    traceLog("api3_log.txt", "删除今日数据，前缀: " + todayPrefix);
                    List entriesToRemove = new ArrayList();
                    Iterator iterator = OP_STATS.entrySet().iterator();
                    int deletedCount = 0;
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        String key = (String) entry.getKey();
                        if (key.startsWith(todayPrefix)) {
                            entriesToRemove.add(entry);
                            deletedCount++;
                        }
                    }
                    for (int i = 0; i < entriesToRemove.size(); i++) {
                        Object entry = entriesToRemove.get(i);
                        if (entry instanceof Map.Entry) {
                            String keyStr = (String) ((Map.Entry) entry).getKey();
                            OP_STATS.remove(keyStr);
                        }
                    }
                    recalculateTotalStats();
                    traceLog("api3_log.txt", "删除键数量: " + deletedCount);
                    writeStats();
                }
                
                weekDatesCache.clear();
                monthDatesCache.clear();
                weekCacheKey = "";
                monthCacheKey = "";
                statsTextViewCache.clear();
                triggerUIUpdate();
                Toast("今日数据已重置");
            }
        })
        .setNegativeButton("取消", null)
        .show();
}

/**
 * 重置累计统计数据
 * @param activity Activity上下文
 */
private void resetTotalStats(final Activity activity) {
    traceLog("api3_log.txt", "用户请求重置累计数据");
    boolean isDark = isThemeDark(activity);
    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("重置确认")
        .setMessage("确定重置所有累计统计数据吗？")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                vibrate(activity, 48);
                synchronized(writeLock) {
                    traceLog("api3_log.txt", "清空所有数据");
                    OP_STATS.clear();
                    for (int i = 0; i < STAT_TYPES.length; i++) {
                        String type = STAT_TYPES[i];
                        OP_STATS.put("total" + type, new Long(0L));
                        addChangedKey("total" + type);
                        traceLog("api3_log.txt", "初始化total" + type);
                    }
                    TOTAL_MSG_SEQ_MAX = 0L;
                    traceLog("api3_log.txt", "数据清空并初始化");
                    writeStats();
                }
                
                weekDatesCache.clear();
                monthDatesCache.clear();
                weekCacheKey = "";
                monthCacheKey = "";
                statsTextViewCache.clear();
                triggerUIUpdate();
                Toast("累计数据已重置");
            }
        })
        .setNegativeButton("取消", null)
        .show();
}

/**
 * 修复统计数据
 * 重新计算所有累计数据，用于修复累计小于当天的情况
 * @param activity Activity上下文
 */
private void repairStatsData(final Activity activity) {
    traceLog("api3_log.txt", "用户请求修复数据");
    vibrate(activity, 48);
    
    synchronized(writeLock) {
        recalculateTotalStats();
        writeStats();
    }
    
    statsTextViewCache.clear();
    triggerUIUpdate();
    Toast("数据已修复完成");
}

/**
 * 显示统计对话框
 * @param activity Activity上下文
 */
public void showStatsDialog(Activity activity) {
    if (activity == null || activity.isFinishing()) {
        traceLog("api3_log.txt", "activity无效");
        Toast("无法显示消息统计: Activity无效");
        return;
    }

    if (statsDialog != null && statsDialog.isShowing()) {
        traceLog("api3_log.txt", "对话框已存在，执行关闭");
        statsDialog.dismiss();
        statsDialog = null;
        dialogVisible = false;
        statsTextViewCache.clear();
        todayCoreCardCache = null;
        progressBarCache = null;
        sendTargetLabelCache = null;
        achievementTextCache = null;
        return;
    }

    readFullStats();
    
    boolean isDark = isThemeDark(activity);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("📊 消息统计");

    ScrollView scrollView = new ScrollView(activity);
    scrollView.setPadding(dp(activity, 5), 0, dp(activity, 5), 0);

    LinearLayout contentLayout = new LinearLayout(activity);
    contentLayout.setOrientation(LinearLayout.VERTICAL);
    contentLayout.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

    contentLayout.addView(createRangeSpinner(activity));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createTotalStatsCard(activity, "📈 累计统计"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createTodayCoreStatsCard(activity, "📊  今日统计"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createReceiveStatsCard(activity, "📥 接收消息统计", "receive"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createSendStatsCard(activity, "📤 发送消息统计", "send"));
    contentLayout.addView(createSpaceView(activity, 18));

    LinearLayout buttonLayout1 = new LinearLayout(activity);
    buttonLayout1.setOrientation(LinearLayout.HORIZONTAL);
    buttonLayout1.setGravity(Gravity.CENTER_HORIZONTAL);
    buttonLayout1.setPadding(0, 0, 0, dp(activity, 10));

    Button resetTodayBtn = createColorButton(activity, "重置今日数据", Color.parseColor("#FFCDD2"));
    resetTodayBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            resetTodayStats(activity);
        }
    });

    Button resetTotalBtn = createColorButton(activity, "重置累计数据", Color.parseColor("#BBDEFB"));
    resetTotalBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            resetTotalStats(activity);
        }
    });

    buttonLayout1.addView(resetTodayBtn);
    buttonLayout1.addView(resetTotalBtn);

    LinearLayout buttonLayout2 = new LinearLayout(activity);
    buttonLayout2.setOrientation(LinearLayout.HORIZONTAL);
    buttonLayout2.setGravity(Gravity.CENTER_HORIZONTAL);
    buttonLayout2.setPadding(0, 0, 0, dp(activity, 10));

    Button repairBtn = createColorButton(activity, "修复数据", Color.parseColor("#C8E6C9"));
    repairBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            repairStatsData(activity);
        }
    });

    Button targetBtn = createColorButton(activity, "设置每日目标", Color.parseColor("#F8BBD0"));
    targetBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            showTargetSettingDialog(activity);
        }
    });

    buttonLayout2.addView(repairBtn);
    buttonLayout2.addView(targetBtn);

    contentLayout.addView(buttonLayout1);
    contentLayout.addView(buttonLayout2);

    scrollView.addView(contentLayout);
    builder.setView(scrollView);

    builder.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            vibrate(activity, 48);
            View decorView = ((AlertDialog) dialog).getWindow().getDecorView();
            startDialogDismissAnimation(decorView, dialog);
            dialogVisible = false;
            statsTextViewCache.clear();
            todayCoreCardCache = null;
            progressBarCache = null;
            sendTargetLabelCache = null;
            achievementTextCache = null;
        }
    });

    statsDialog = builder.create();
    statsDialog.show();

    startDialogShowAnimation(contentLayout);

    applyUiTheme(activity, statsDialog);

    Window window = statsDialog.getWindow();
    if (window != null) {
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = Math.min(dp(activity, 360), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 40));
        window.setAttributes(params);
    }

    dialogVisible = true;
    traceLog("api3_log.txt", "对话框显示完成");

    msgHandle.postDelayed(new Runnable() {
        public void run() {
            if (dialogVisible && statsDialog != null && statsDialog.isShowing()) {
                View rootView = statsDialog.getWindow().getDecorView();
                if (rootView == null) {
                    traceLog("api3_log.txt", "rootView为null");
                    return;
                }

                String[] tagsToFind = {
                    "totalReceive", "totalSend", "Receive", "Send",
                    "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo", "ReceiveEmoji",
                    "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip",
                    "ReceiveWordCount", "ReceiveUnknown",
                    "SendText", "SendPic", "SendFile", "SendVideo", "SendEmoji",
                    "SendAudio", "SendCard", "SendCall",
                    "SendWordCount", "SendUnknown"
                };
                
                for (int i = 0; i < tagsToFind.length; i++) {
                    TextView tv = (TextView) rootView.findViewWithTag(tagsToFind[i]);
                    if (tv != null) {
                        statsTextViewCache.add(tv);
                    }
                }

                todayCoreCardCache = (LinearLayout) rootView.findViewWithTag("todayCoreCard");
                if (todayCoreCardCache != null) {
                    sendTargetLabelCache = (TextView) todayCoreCardCache.findViewWithTag("send_target_label");
                    progressBarCache = todayCoreCardCache.findViewWithTag("send_progress");
                    achievementTextCache = (TextView) todayCoreCardCache.findViewWithTag("achievement_text");
                }
                traceLog("api3_log.txt", "控件缓存完成");

                triggerUIUpdate();
            }
        }
    }, 100);
}

/**
 * 获取发送类型映射表
 * @return 类型键到中文名称的映射
 */
Map getSendTypeMapping() {
    Map map = new HashMap();
    map.put("SendText", "文本");
    map.put("SendPic", "图片");
    map.put("SendFile", "文件");
    map.put("SendVideo", "视频");
    map.put("SendEmoji", "表情");
    map.put("SendAudio", "语音");
    map.put("SendCard", "卡片");
    map.put("SendCall", "通话");
    map.put("SendWordCount", "字数");
    map.put("Send", "总数"); 
    return map;
}

/**
 * 获取接收类型映射表
 * @return 类型键到中文名称的映射
 */
Map getReceiveTypeMapping() {
    Map map = new HashMap();
    map.put("ReceiveText", "文本");
    map.put("ReceivePic", "图片");
    map.put("ReceiveFile", "文件");
    map.put("ReceiveVideo", "视频");
    map.put("ReceiveEmoji", "表情");
    map.put("ReceiveAudio", "语音");
    map.put("ReceiveCard", "卡片");
    map.put("ReceiveCall", "通话");
    map.put("ReceiveWordCount", "字数");
    map.put("Receive", "总数");
    return map;
}

/**
 * 获取所有变量映射表
 * @param scriptScope 脚本作用域
 * @return 变量名到值的映射
 */
Map getAllVariablesMap(Object scriptScope) {
    Map map = new HashMap();

    map.put("time", getTime());
    try {
        map.put("qq", String.valueOf(myUin));
    } catch (Exception e) {}

    String rawTime = getTodayDateStr();
    String todayStr = "";
    if (rawTime != null) {
        if (rawTime.contains("-")) {
            todayStr = rawTime.split(" ")[0].replace("-", "");
        } else if (rawTime.contains("/")) {
            todayStr = rawTime.split(" ")[0].replace("/", "");
        } else {
            if (rawTime.length() >= 8) {
                todayStr = rawTime.substring(0, 8);
            } else {
                todayStr = rawTime;
            }
        }
    }

    Map SEND_TYPE_MAP = getSendTypeMapping();
    Map RECEIVE_TYPE_MAP = getReceiveTypeMapping();
    
    for (Object sendValue : SEND_TYPE_MAP.values()) {
        String chineseType = (String) sendValue;
        map.put("今日发送" + chineseType, "0");
    }
    for (Object receiveValue : RECEIVE_TYPE_MAP.values()) {
        String chineseType = (String) receiveValue;
        map.put("今日接收" + chineseType, "0");
    }

    synchronized(writeLock) {
        Iterator iterator = OP_STATS.entrySet().iterator();
        String todayPrefix = "date_" + todayStr + "_";

        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            Object valObj = entry.getValue();
            String valueStr = (valObj != null) ? String.valueOf(valObj) : "0";
            
            map.put(key, valueStr);

            if (key.startsWith(todayPrefix)) {
                String suffix = key.substring(todayPrefix.length());

                if (SEND_TYPE_MAP.containsKey(suffix)) {
                    String chineseType = (String) SEND_TYPE_MAP.get(suffix);
                    map.put("今日发送" + chineseType, valueStr);
                } else if (RECEIVE_TYPE_MAP.containsKey(suffix)) {
                    String chineseType = (String) RECEIVE_TYPE_MAP.get(suffix);
                    map.put("今日接收" + chineseType, valueStr);
                }
            }
        }
    }
    return map;
}

/**
 * 获取指定变量值
 * @param scriptScope 脚本作用域
 * @param varName 变量名
 * @return 变量值字符串
 */
String getVariableValue(Object scriptScope, String varName) {
    Map allVars = getAllVariablesMap(scriptScope);
    for (Object obj: allVars.entrySet()) {
        Map.Entry entry = (Map.Entry) obj;
        if (((String) entry.getKey()).equalsIgnoreCase(varName)) {
            return (String) entry.getValue();
        }
    }
    return null;
}

/**
 * 获取变量描述
 * @param key 变量键
 * @param value 变量值
 * @return 描述字符串
 */
String getVarDescription(String key, String value) {
    String desc = "变量值";

    if (key.equalsIgnoreCase("time")) {
        desc = "当前系统时间";
    } else if (key.equalsIgnoreCase("qq")) {
        desc = "宿主QQ号";
    } else if (key.startsWith("total")) {
        String type = key.substring(5);
        if (type.equals("Send")) desc = "累计发送总数";
        else if (type.equals("Receive")) desc = "累计接收总数";
        else if (type.equals("SendWordCount")) desc = "累计发送字数";
        else if (type.equals("ReceiveWordCount")) desc = "累计接收字数";
        else desc = "累计 " + type + " 数量";
    } else if (key.startsWith("今日发送")) {
        String type = key.substring(4);
        desc = "今日发送" + type + "数量";
    } else if (key.startsWith("今日接收")) {
        String type = key.substring(4);
        desc = "今日接收" + type + "数量";
    }

    return desc + ": " + value;
}

/**
 * 获取排序后的变量列表
 * @param scriptScope 脚本作用域
 * @return 排序后的变量条目列表
 */
List getSortedVariableList(Object scriptScope) {
    Map map = getAllVariablesMap(scriptScope);
    List list = new ArrayList(map.entrySet());
    
    Collections.sort(list, new Comparator() {
        public int compare(Object o1, Object o2) {
            Map.Entry e1 = (Map.Entry) o1;
            Map.Entry e2 = (Map.Entry) o2;
            String k1 = (String) e1.getKey();
            String k2 = (String) e2.getKey();
            int len1 = k1.length();
            int len2 = k2.length();
            if (len1 != len2) return len1 - len2;
            return k1.compareToIgnoreCase(k2);
        }
    });
    return list;
}

/**
 * 替换变量占位符
 * @param template 模板字符串
 * @param scriptScope 脚本作用域
 * @return 替换后的字符串
 */
String 替换变量占位符(String template, Object scriptScope) {
    if (template == null || template.trim().isEmpty()) {
        return "";
    }

    String result = template;

    try {
        Pattern linkPattern = Pattern.compile("##(.*?)##");
        Matcher linkMatcher = linkPattern.matcher(result);
        StringBuilder linkSb = new StringBuilder();
        int lastIdx = 0;
        while (linkMatcher.find()) {
            linkSb.append(result, lastIdx, linkMatcher.start());
            String linkName = linkMatcher.group(1).trim();
            if (linkName.isEmpty()) {
                linkSb.append("##");
            } else {
                String linkValue = null;
                try {
                    Future future = ThreadPool.submit(new Callable() {
                        public Object call() throws Exception {
                            return get(linkName);
                        }
                    });
                    linkValue = (String) future.get(500, TimeUnit.MILLISECONDS);
                    if (linkValue == null || linkValue.trim().isEmpty() || "null".equals(linkValue)) {
                        linkValue = null;
                    }
                } catch (Exception e) {
                }
                linkSb.append(linkValue != null ? linkValue : "访问链接失败了哦～");
            }
            lastIdx = linkMatcher.end();
        }
        linkSb.append(result, lastIdx, result.length());
        result = linkSb.toString();
    } catch (Exception e) {
    }

    try {
        Pattern varPattern = Pattern.compile("#(.*?)#");
        Matcher varMatcher = varPattern.matcher(result);
        StringBuilder varSb = new StringBuilder();
        int lastIdx = 0;
        while (varMatcher.find()) {
            varSb.append(result, lastIdx, varMatcher.start());
            String varName = varMatcher.group(1).trim();
            if (varName.isEmpty()) {
                varSb.append("#");
            } else {
                String val = getVariableValue(scriptScope, varName);
                String replacement = (val != null && !val.trim().isEmpty() && !"null".equals(val)) ? val : ("变量" + varName + "不存在哦～");
                varSb.append(replacement);
            }
            lastIdx = varMatcher.end();
        }
        varSb.append(result, lastIdx, result.length());
        result = varSb.toString();
    } catch (Exception e) {
    }

    return result;
}

/**
 * 显示输入框设置对话框
 * @param activity Activity上下文
 */
void showInputDialog(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    
    final Object scriptScope = this;

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
                int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
                int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 16));

                TextView titleView = new TextView(activity);
                titleView.setText("设置输入框提示词");
                titleView.setTextSize(18);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(textColor);
                titleView.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(titleView);

                final EditText input = new EditText(activity);
                String contentStr = getString("输入框", "提示词", "");
                if (contentStr == null) contentStr = "";
                input.setText(contentStr);
                input.setHint("输入内容，支持 #变量# 或 ##链接##");
                input.setHintTextColor(subTextColor);
                input.setTextColor(textColor);
                input.setTextSize(15);
                input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));

                GradientDrawable inputBg = new GradientDrawable();
                inputBg.setCornerRadius(dp(activity, 8));
                inputBg.setColor(isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT);
                inputBg.setStroke(dp(activity, 1), isDark ? UI_COLOR_STROKE_DARK : UI_COLOR_STROKE_LIGHT);
                input.setBackground(inputBg);
                root.addView(input);

                TextView previewLabel = new TextView(activity);
                previewLabel.setText("效果预览：");
                previewLabel.setTextSize(12);
                previewLabel.setTextColor(subTextColor);
                previewLabel.setPadding(dp(activity, 4), dp(activity, 8), 0, 0);
                root.addView(previewLabel);

                final TextView previewText = new TextView(activity);
                previewText.setText(替换变量占位符(contentStr, scriptScope));
                previewText.setTextSize(13);
                previewText.setTextColor(isDark ? Color.parseColor("#81C784") : Color.parseColor("#4CAF50"));
                previewText.setPadding(dp(activity, 4), dp(activity, 2), dp(activity, 4), dp(activity, 8));
                root.addView(previewText);

                final Handler debounceHandler = new Handler();
                final Runnable[] pendingRunnable = new Runnable[1];

                input.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(Editable s) {
                        if (pendingRunnable[0] != null) {
                            debounceHandler.removeCallbacks(pendingRunnable[0]);
                        }

                        pendingRunnable[0] = new Runnable() {
                            public void run() {
                                String raw = s.toString();
                                String preview = 替换变量占位符(raw, scriptScope);
                                previewText.setText(preview);
                            }
                        };

                        debounceHandler.postDelayed(pendingRunnable[0], 500);
                    }
                });
                
                input.setText(input.getText());
                input.setSelection(input.getText().length());

                TextView tipsView = new TextView(activity);
                tipsView.setText("💡 使用 #变量# 或 ##链接## 引用\n如: #time# 或 ##api##\napi暂时只支持返回纯文本格式\n\n在api.java文件1550行可自定义变量\n\n\n\n保存既生效,切换界面可以更新输入框内容哦～");
                tipsView.setTextSize(12);
                tipsView.setTextColor(subTextColor);
                tipsView.setPadding(dp(activity, 4), dp(activity, 8), dp(activity, 4), 0);
                root.addView(tipsView);

                TextView viewAllBtn = new TextView(activity);
                viewAllBtn.setText("🔍 查看所有可用变量");
                viewAllBtn.setTextSize(13);
                viewAllBtn.setTextColor(accentColor);
                viewAllBtn.setTypeface(null, Typeface.BOLD);
                viewAllBtn.setPadding(dp(activity, 4), dp(activity, 12), 0, 0);
                viewAllBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        vibrate(activity, 48);
                        showAllVariablesDialog(activity, scriptScope);
                    }
                });
                root.addView(viewAllBtn);

                LinearLayout btnBox = new LinearLayout(activity);
                btnBox.setOrientation(LinearLayout.HORIZONTAL);
                btnBox.setPadding(0, dp(activity, 20), 0, 0);
                btnBox.setGravity(Gravity.RIGHT);

                TextView cancel = new TextView(activity);
                cancel.setText("取消");
                cancel.setTextSize(14);
                cancel.setTextColor(subTextColor);
                cancel.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
                
                final AlertDialog[] dialogRef = new AlertDialog[1];

                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (dialogRef[0] != null) dialogRef[0].dismiss();
                    }
                });

                TextView confirm = new TextView(activity);
                confirm.setText("确定");
                confirm.setTextSize(14);
                confirm.setTextColor(accentColor);
                confirm.setTypeface(null, Typeface.BOLD);
                confirm.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
                confirm.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String inputStr = input.getText().toString().trim();
                        if (!inputStr.equals("")) {
                            putString("输入框", "提示词", inputStr);
                            Toast("提示词已保存并立即生效");
                            try {
                                chatInterface(0, "", "");
                            } catch (Exception e) {}
                        }
                        if (dialogRef[0] != null) dialogRef[0].dismiss();
                    }
                });

                btnBox.addView(cancel);
                btnBox.addView(confirm);
                root.addView(btnBox);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                AlertDialog dialog = builder.create();
                dialogRef[0] = dialog;

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (pendingRunnable[0] != null) {
                            debounceHandler.removeCallbacks(pendingRunnable[0]);
                        }
                    }
                });

                dialog.show();
                
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.width = Math.min(dp(activity, 360), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 40));
                    window.setAttributes(params);
                }
                
                applyUiTheme(activity, dialog);

            } catch (Exception e) {}
        }
    });
}

/**
 * 显示所有变量对话框
 * @param activity Activity上下文
 * @param scriptScope 脚本作用域
 */
void showAllVariablesDialog(final Activity activity, final Object scriptScope) {
    if (activity == null || activity.isFinishing()) return;
    
    boolean isDark = isThemeDark(activity);
    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
    int itemBgColor = isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#F5F5F5");
    int accentColor = isDark ? UI_COLOR_ACCENT_DARK : UI_COLOR_ACCENT_LIGHT;

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);

    TextView title = new TextView(activity);
    title.setText("所有可用变量 (点击复制)");
    title.setTextSize(17);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(textColor);
    title.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 10));
    root.addView(title);

    ScrollView scrollView = new ScrollView(activity);
    LinearLayout listLayout = new LinearLayout(activity);
    listLayout.setOrientation(LinearLayout.VERTICAL);
    listLayout.setPadding(dp(activity, 16), 0, dp(activity, 16), dp(activity, 16));

    List sortedVars = getSortedVariableList(scriptScope);

    for (int i = 0; i < sortedVars.size(); i++) {
        Map.Entry entry = (Map.Entry) sortedVars.get(i);
        String key = (String) entry.getKey();
        String val = (String) entry.getValue();

        if (key.startsWith("date_") && key.length() > 20) {
            continue; 
        }

        String desc = getVarDescription(key, val);

        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        item.setClickable(true);
        item.setFocusable(true);

        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(itemBgColor);
        itemBg.setCornerRadius(dp(activity, 8));
        item.setBackground(itemBg);

        final GradientDrawable finalItemBg = itemBg;
        item.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setBackgroundColor(Color.parseColor(isDark ? "#44FFFFFF" : "#E0E0E0"));
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setBackground(finalItemBg);
                }
                return false;
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(activity, 8));
        item.setLayoutParams(params);

        TextView keyTv = new TextView(activity);
        keyTv.setText("#" + key + "#");
        keyTv.setTextSize(14);
        keyTv.setTypeface(null, Typeface.BOLD);
        keyTv.setTextColor(accentColor);
        item.addView(keyTv);

        TextView descTv = new TextView(activity);
        descTv.setText(desc);
        descTv.setTextSize(12);
        descTv.setTextColor(subTextColor);
        descTv.setPadding(0, dp(activity, 4), 0, 0);
        item.addView(descTv);

        final String copyText = "#" + key + "#";

        item.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                    ClipboardManager cm = (ClipboardManager) activity.getApplicationContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        ClipData clip = ClipData.newPlainText("Var", copyText);
                        cm.setPrimaryClip(clip);
                        Toast("已复制: " + copyText);
                        
                        keyTv.setTextColor(Color.GREEN);
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                keyTv.setTextColor(accentColor);
                            }
                        }, 300);
                    } else {
                        Toast("系统剪贴板不可用");
                    }
                } catch (Exception e) {
                    Toast("复制失败");
                }
            }
        });

        listLayout.addView(item);
    }

    if (sortedVars.isEmpty()) {
        TextView empty = new TextView(activity);
        empty.setText("暂无变量");
        empty.setTextColor(subTextColor);
        empty.setPadding(20, 20, 20, 20);
        listLayout.addView(empty);
    }

    scrollView.addView(listLayout);
    int maxHeight = activity.getResources().getDisplayMetrics().heightPixels / 2;
    root.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxHeight));

    TextView closeBtn = new TextView(activity);
    closeBtn.setText("关闭");
    closeBtn.setGravity(Gravity.CENTER);
    closeBtn.setPadding(0, dp(activity, 16), 0, dp(activity, 16));
    closeBtn.setTextColor(textColor);
    
    final AlertDialog[] varDialogRef = new AlertDialog[1];
    closeBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            if (varDialogRef[0] != null) varDialogRef[0].dismiss();
        }
    });
    
    root.addView(closeBtn);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(root);
    AlertDialog varDialog = builder.create();
    varDialogRef[0] = varDialog;

    varDialog.show();
    
    Window window = varDialog.getWindow();
    if (window != null) {
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = Math.min(dp(activity, 360), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 40));
        window.setAttributes(params);
    }
    
    applyUiTheme(activity, varDialog);
}

import me.yxp.qfun.utils.qq.HostInfo;

void chatInterface(int chatType, String peerUin, String peerName) {
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    final Activity finalActivity = activity;
    if (finalActivity == null) return;

    final Object scriptScope = this;
    
    try {
        currentPeerUin = peerUin;
        currentChatType = chatType;
        dispatchEvent(peerUin, 5); 
    } catch (Throwable e) {}

    boolean 输入框开关 = getBoolean("输入框", "输入框开关", false);
    
    String 提示词模板 = getString("输入框", "提示词", "");
    if (提示词模板 == null || 提示词模板.trim().isEmpty()) {
        提示词模板 = "我是一个输入框提示～";
    }

    final String final提示词 = 替换变量占位符(提示词模板, scriptScope);

    finalActivity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (输入框开关) {
                    int inputId = finalActivity.getResources().getIdentifier("input", "id", HostInfo.INSTANCE.getPackageName());
                    View inputView = finalActivity.findViewById(inputId);
                    if (inputView != null && inputView instanceof TextView) {
                        ((TextView) inputView).setHint(final提示词);
                    }
                }

            } catch (Throwable e) {
            }
        }
    });
}
