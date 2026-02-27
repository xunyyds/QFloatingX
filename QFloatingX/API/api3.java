// 核心配置常量
private static final String SP_NAME = "msg_stats_config"; // SharedPreferences 存储文件名，用于保存用户配置
private static final String SP_TIME_RANGE_KEY = "selected_time_range"; // SP键名：保存用户上次选择的时间范围
private static final String SP_CUSTOM_DATE_KEY = "custom_selected_date"; // SP键名：保存用户自定义选择的具体日期
private static final String DAILY_TARGET_KEY = "daily_msg_target"; // SP键名：保存用户设置的每日发送消息目标数
private static final int DEFAULT_DAILY_TARGET = 100; // 默认的每日目标数值
private static final String TEMP_FILE_SUFFIX = ".tmp"; // 写入文件时的临时后缀
private static final long MIN_UI_UPDATE_INTERVAL = 150; // UI刷新的最小间隔，防抖动
private static final int BATCH_SIZE_THRESHOLD = 10; // 消息批处理阈值

// UI与状态管理
volatile boolean dialogVisible = false; // 标记统计对话框当前是否处于显示状态
AlertDialog statsDialog = null; // 持有当前显示的对话框实例

// 线程与并发控制
private final Object writeLock = new Object(); // 核心同步锁，保护数据读写，也用于线程等待
private volatile boolean writeThreadRunning = false; // 标记后台写入线程是否正在运行
private Handler msgHandle = new Handler(Looper.getMainLooper()); // 主线程Handler
private volatile long lastUIUpdateTime = 0L; // 记录上次UI刷新的时间戳

// 缓存机制
private List weekDatesCache = new ArrayList(); // 缓存"本周"日期列表
private List monthDatesCache = new ArrayList(); // 缓存"本月"日期列表
private String weekCacheKey = ""; // 周缓存校验Key
private String monthCacheKey = ""; // 月缓存校验Key
private List statsTextViewCache = new ArrayList(); // 缓存对话框中的 TextView 引用
private LinearLayout todayCoreCardCache = null; // 缓存"今日核心卡片"布局
private View progressBarCache = null; // 缓存进度条 View
private TextView sendTargetLabelCache = null; // 缓存目标描述文字 TextView
private TextView achievementTextCache = null; // 缓存成就文字 TextView

// 统计类型定义 - 包含 Unknown 类型
private static final String[] STAT_TYPES = {
    "Receive", "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo",
    "ReceiveEmoji", "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip",
    "ReceiveWordCount", "ReceiveUnknown", // 新增：未知接收
    "Send", "SendText", "SendPic", "SendFile", "SendVideo",
    "SendEmoji", "SendAudio", "SendCard", "SendCall", "Command", "Like",
    "SendWordCount", "SendUnknown" // 新增：未知发送
};

// 数据存储核心
private static final Hashtable OP_STATS = new Hashtable(); // 内存数据库 (Key: String, Value: Long)
private static final Vector CHANGED_KEYS = new Vector(); // 脏数据记录表
private static final Vector messageBatchQueue = new Vector(); // 消息缓冲队列
private static final Hashtable cardExpandStatus = new Hashtable(); // 卡片展开状态
volatile long TOTAL_MSG_SEQ_MAX = 0L; // 最大消息序列号

// 颜色配置
private int[] COLORS = {
    Color.parseColor("#FF6B6B"), Color.parseColor("#4ECDC4"), Color.parseColor("#45B7D1"),
    Color.parseColor("#96CEB4"), Color.parseColor("#FFEAA7"), Color.parseColor("#DDA0DD"),
    Color.parseColor("#FFA07A"), Color.parseColor("#87CEEB"), Color.parseColor("#F0E68C"),
    Color.parseColor("#CD853F"), Color.parseColor("#98FB98")
};

// 文件路径与时间范围
String configName = pluginPath + "/config/msg_stats.json";

public class TimeRange {
    public static final int TODAY = 0;
    public static final int YESTERDAY = 1;
    public static final int THIS_WEEK = 2;
    public static final int THIS_MONTH = 3;
    public static final int CUSTOM_DATE = 4;
    
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

// 原子操作方法
private synchronized long atomicIncrement(String key) {
    Long current = (Long) OP_STATS.get(key);
    long newValue = (current != null ? current.longValue() + 1 : 1);
    
    // 【优化】使用 Long.valueOf 代替 new Long
    // 对于 -128 到 127 的数值，直接使用缓存对象，减少对象创建和 GC 压力
    OP_STATS.put(key, Long.valueOf(newValue));
    
    addChangedKey(key);
    return newValue;
}

// 新增：支持增加指定数值的原子操作（用于字数统计累加）
private synchronized long atomicAdd(String key, long delta) {
    Long current = (Long) OP_STATS.get(key);
    long newValue = (current != null ? current.longValue() + delta : delta);
    
    // 【优化】同上
    OP_STATS.put(key, Long.valueOf(newValue));
    
    addChangedKey(key);
    return newValue;
}

private synchronized long atomicGet(String key) {
    Long value = (Long) OP_STATS.get(key);
    return value != null ? value.longValue() : 0L;
}

private synchronized void addChangedKey(String key) {
    if (!CHANGED_KEYS.contains(key)) {
        CHANGED_KEYS.add(key);
    }
}

// UI数值格式化方法：处理大数字显示 (k/w)
private String formatStatValue(long value) {
    // Java Long最大值 9223372036854775807，存储永远不会溢出。
    // 如果大于 10000，显示为 "x.x w"
    if (value >= 10000) {
        double w = (double) value / 10000.0;
        return String.format(Locale.CHINA, "%.1f w", w);
    }
    return String.valueOf(value);
}

// 线程管理方法 - 修改为懒调用模式
private void startWriteThread() {
    if (writeThreadRunning) return;
    
    writeThreadRunning = true;
    
    ThreadPool.execute(new Runnable() {
        public void run() {
            traceLog("api3_log.txt", "后台写入任务启动（懒调用模式）");
            while (writeThreadRunning) {
                try {
                    synchronized(writeLock) {
                        // 【懒调用优化】如果没有待处理的队列且没有脏数据需要写入，则进入等待状态
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

private void stopWriteThread() {
    traceLog("api3_log.txt", "停止写入任务");
    writeThreadRunning = false;
    synchronized(writeLock) {
        writeLock.notifyAll();
    }
}

String todayDateStr = getTodayDateStr();

private String formatDateForDisplay(String dateStr) {
    try {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA);
        return outputFormat.format(inputFormat.parse(dateStr));
    } catch (Exception e) {
        return dateStr;
    }
}

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
        else if (element.pttElement != null) hasVoice = true; // 语音
        
        else if (element.marketFaceElement != null || element.faceElement != null) {
            hasEmoji = true; 
        }
        
        else if (element.arkElement != null || element.structMsgElement != null) hasCard = true; // 卡片
        else if (element.textElement != null) {
            if (element.textElement.content != null && element.textElement.content.length() > 0) {
                hasText = true;
            }
        }
    }

    // 媒体类型优先
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

private int getColorForStat(int index) {
    if (index >= 0 && index < COLORS.length) {
        return COLORS[index];
    }
    return Color.parseColor("#333333");
}

private GradientDrawable createRoundRectDrawable(Activity activity, int color, int radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(dp(activity, radius));
    return drawable;
}

private View createSpaceView(Activity activity, int heightDp) {
    View space = new View(activity);
    space.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, heightDp)));
    return space;
}

private View createDivider(Activity activity) {
    boolean isDark = isThemeDark(activity);
    View divider = new View(activity);
    // 暗黑模式下分割线颜色
    divider.setBackgroundColor(isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#F0F0F0"));
    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 1);
    dividerParams.setMargins(0, dp(activity, 6), 0, dp(activity, 6));
    divider.setLayoutParams(dividerParams);
    return divider;
}

private Button createColorButton(Activity activity, String text, int bgColor) {
    Button button = new Button(activity);
    button.setText(text);
    // 按钮文字保持深色，因为背景是淡色Pastel
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

// 初始化与数据加载
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

public void initStats() {
    traceLog("api3_log.txt", "api3初始化开始");
    Activity activity = getNowActivity();
    initTimeRange(activity);
    readFullStats();
    startWriteThread();
    traceLog("api3_log.txt", "api3初始化完成");
}

// 批量处理核心
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
        
        String msgDateStr = todayDateStr;
        try {
            long msgTimeSec = data.time;
            if (msgTimeSec > 0) {
                // 转换为毫秒
                long msgTimeMs = msgTimeSec * 1000; 
                long currentTime = System.currentTimeMillis();
                
                // 这里比较 msgTimeMs 和 currentTime，两者单位一致
                if (Math.abs(currentTime - msgTimeMs) > 24 * 60 * 60 * 1000L) {
                    msgDateStr = sdf.format(new Date(msgTimeMs));
                }
            }
        } catch (Exception e) {
            // 获取时间失败，回退到系统日期
        }
        
        String dateKeyPrefix = "date_" + msgDateStr + "_";
        boolean isSend = myUin.equals(senderUin);
        long wordCount = 0;
        
        if ("text".equals(msgTypeStr)) {
            wordCount = getPureWordCount(data);
        }

        if (isSend) {
            // 在分类前先 +1
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
                // 兜底：未知类型
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
                // 兜底：未知类型
                traceLog("api3_log.txt", "消息类型未知，归类为Unknown");
                atomicIncrement(dateKeyPrefix + "ReceiveUnknown");
                atomicIncrement("totalReceiveUnknown");
            }
        }
    }
    
    triggerUIUpdate();
}

public void onMsg(Object data) {
    if (data == null) {
        traceLog("api3_log.txt", "data为null，忽略");
        return;
    }
    log大小限制(logPath);
    
    try { dispatchEvent(data, 1); 
	} catch (Throwable e) { traceLog("function_log", "[onMsg]" + e); }

    synchronized(writeLock) {
        messageBatchQueue.add(data);
        writeLock.notifyAll();
    }
    
    int queueSize = messageBatchQueue.size();
    if (queueSize > 50) {
        traceLog("api3_log.txt", "队列积压: " + queueSize + " 条");
    }
}

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

private void writeFullStats() {
    traceLog("api3_log.txt", "用户触发强制全量持久化");
    synchronized(writeLock) {
        writeFullStatsInternal(new Vector());
    }
}

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

// UI更新与交互
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

    // 应用数字格式化 (超过10000显示w)
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
        // 应用数字格式化
        updateCachedTextView(key, rangeName + ": " + formatStatValue(rangeVal) + "  累计: " + formatStatValue(totalVal));
    }

    updateTodayProgress(todaySend, dailyTarget);
}

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

import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;

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
    // 适配：输入框背景和文字颜色
    editText.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#33FFFFFF") : Color.parseColor("#F5F5F5"), 8));
    editText.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.BLACK);
    editText.setHintTextColor(isDark ? Color.parseColor("#AAAAAA") : Color.GRAY);
    editText.setTextSize(14);
    dialogLayout.addView(editText);

    // 适配：目标设置弹窗主题
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

private LinearLayout createStatsCardBase(Activity activity, String title, String cardType) {
    boolean isDark = isThemeDark(activity);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    // 适配：卡片背景（暗黑模式下稍微亮一点，区别于Dialog背景）
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
    // 适配：卡片标题颜色
    cardTitle.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333"));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), android.graphics.Typeface.BOLD);
    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    cardTitle.setLayoutParams(titleParams);
    titleBar.addView(cardTitle);

    TextView arrowTv = new TextView(activity);
    arrowTv.setText("▶");
    arrowTv.setTextSize(14);
    // 适配：箭头颜色
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
    // 适配：列表项标签颜色
    labelTv.setTextColor(isDark ? Color.parseColor("#AAAAAA") : Color.parseColor("#666666"));
    labelTv.setTextSize(14);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

    TextView statsTv = new TextView(activity);
    long rangeValue = getStatsByTimeRange(typeKey);
    long totalValue = atomicGet("total" + typeKey);
    // 应用数字格式化
    statsTv.setText(TimeRange.toString(currentTimeRange) + ": " + formatStatValue(rangeValue) + "  累计: " + formatStatValue(totalValue));
    statsTv.setTextColor(getColorForStat(colorIndex));
    statsTv.setTextSize(14);
    statsTv.setTag(typeKey);

    itemLayout.addView(iconTv);
    itemLayout.addView(labelTv);
    itemLayout.addView(statsTv);
    return itemLayout;
}

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
    // 适配：列表项标签颜色
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
    // 应用数字格式化
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

private LinearLayout createTodayCoreStatsCard(Activity activity, String title) {
    boolean isDark = isThemeDark(activity);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    // 适配：卡片背景
    card.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#FF2D2D2D") : Color.WHITE, 12));
    card.setTag("todayCoreCard");
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTag("core_card_title");
    // 适配：标题颜色
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

    // 适配：目标说明文字颜色
    sendTargetLabel.setTextColor(isDark ? Color.parseColor("#EFEFEF") : Color.parseColor("#333333"));
    sendTargetLabel.setTextSize(14);
    sendTargetLayout.addView(sendTargetLabel);

    View progressBg = new View(activity);
    progressBg.setTag("send_progress_bg");
    // 适配：进度条底色
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

private LinearLayout createTotalStatsCard(Activity activity, String title) {
    boolean isDark = isThemeDark(activity);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    // 适配：卡片背景
    card.setBackground(createRoundRectDrawable(activity, isDark ? Color.parseColor("#FF2D2D2D") : Color.WHITE, 12));
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    // 适配：标题颜色
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
        {"🤔 未知", "ReceiveUnknown"} // 新增：未知类型UI项
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
        {"🤔 未知", "SendUnknown"} // 新增：未知类型UI项
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

private void resetTodayStats(final Activity activity) {
    traceLog("api3_log.txt", "用户请求重置今日数据");
    boolean isDark = isThemeDark(activity);
    // 适配：重置确认弹窗
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
                    // 【修复】使用 entry.getKey() 进行移除
                    for (int i = 0; i < entriesToRemove.size(); i++) {
                        Object entry = entriesToRemove.get(i);
                        if (entry instanceof Map.Entry) {
                            String keyStr = (String) ((Map.Entry) entry).getKey();
                            OP_STATS.remove(keyStr);
                        }
                    }
                    recalculateTotalStats();
                    traceLog("api3_log.txt", "删除键数量: " + deletedCount);
                    
                    // 【修复】立即触发写入
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

private void resetTotalStats(final Activity activity) {
    traceLog("api3_log.txt", "用户请求重置累计数据");
    boolean isDark = isThemeDark(activity);
    // 适配：重置累计弹窗
    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("重置确认")
        .setMessage("确定重置所有累计统计数据吗？")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                vibrate(activity, 48);
                synchronized(writeLock) {
                    traceLog("api3_log.txt", "清空所有数据");
                    OP_STATS.clear();
                    // 移除了 CHANGED_KEYS.clear()
                    for (int i = 0; i < STAT_TYPES.length; i++) {
                        String type = STAT_TYPES[i];
                        OP_STATS.put("total" + type, new Long(0L));
                        addChangedKey("total" + type);
                        traceLog("api3_log.txt", "初始化total" + type);
                    }
                    TOTAL_MSG_SEQ_MAX = 0L;
                    traceLog("api3_log.txt", "数据清空并初始化");
                    
                    // 【修复】立即触发写入
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

    vibrate(activity, 48);
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

    Button refreshBtn = createColorButton(activity, "刷新数据", Color.parseColor("#C8E6C9"));
    refreshBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            traceLog("api3_log.txt", "用户刷新数据");
            weekDatesCache.clear();
            monthDatesCache.clear();
            weekCacheKey = "";
            monthCacheKey = "";
            statsTextViewCache.clear();
            readFullStats();
            triggerUIUpdate();
            checkTodayReset();
            Toast("数据已刷新");
        }
    });

    Button targetBtn = createColorButton(activity, "设置每日目标", Color.parseColor("#F8BBD0"));
    targetBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            showTargetSettingDialog(activity);
        }
    });

    buttonLayout2.addView(refreshBtn);
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

    // 移除手动背景设置，应用统一主题
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

                // 查找并缓存所有TextView
                String[] tagsToFind = {
                    "totalReceive", "totalSend", "Receive", "Send",
                    "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo", "ReceiveEmoji",
                    "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip",
                    "ReceiveWordCount", "ReceiveUnknown", // 新增缓存Tag
                    "SendText", "SendPic", "SendFile", "SendVideo", "SendEmoji",
                    "SendAudio", "SendCard", "SendCall",
                    "SendWordCount", "SendUnknown"     // 新增缓存Tag
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
