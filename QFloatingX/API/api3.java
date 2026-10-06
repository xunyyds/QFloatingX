class StatsState {
    boolean pageVisible = false;
    View contentRoot = null;
    final Object writeLock = new Object();
    volatile boolean writeThreadRunning = false;
    Handler msgHandle = new Handler(Looper.getMainLooper());
    volatile long lastUIUpdateTime = 0L;
    List weekDatesCache = new ArrayList();
    List monthDatesCache = new ArrayList();
    String weekCacheKey = "";
    String monthCacheKey = "";
    LinearLayout todayCoreCardCache = null;
    View progressBarCache = null;
    TextView sendTargetLabelCache = null;
    TextView achievementTextCache = null;
    TextView whitelistDetailCache = null;
    int whitelistCurrentIndex = 0;
    List statsTextViewCache = new ArrayList();
    final Hashtable OP_STATS = new Hashtable();
    final Vector CHANGED_KEYS = new Vector();
    final Vector messageBatchQueue = new Vector();
    final Hashtable cardExpandStatus = new Hashtable();
    volatile int currentTimeRange = 0;
    String customDateStr = null;
    volatile String todayDateStr = "";
    final Hashtable linkCache = new Hashtable();
    final Hashtable linkCacheTime = new Hashtable();
    volatile long lastLogCheckTime = 0L;
    volatile int logMsgCounter = 0;
    volatile boolean logCheckRunning = false;
    volatile long lastBackupTime = 0L;
    final Pattern LINK_PATTERN = Pattern.compile("##(.*?)##");
    final Pattern VAR_PATTERN = Pattern.compile("#(.*?)#");
}

StatsState SC = new StatsState();
SC.todayDateStr = getTodayDateStr();

interface OnSelectorPickedListener {
    void onPicked(int index);
}

void showStyledSelector(final Activity act, final View anchor, final String[] items, final int selectedIndex, final OnSelectorPickedListener callback) {
    if (act == null || items == null || items.length == 0) return;

    int bgColor = pc(getSettingsThemeColor(act, "surface"));
    int textColor = pc(getSettingsThemeColor(act, "on_surface"));
    int accentColor = pc(getSettingsThemeColor(act, "primary"));
    int dividerColor = pc(getSettingsThemeColor(act, "outline"));
    int pressedColor = tca(act, "primary", 40);
    int corner = dp(act, 12);
    int outerPad = dp(act, 6);

    final PopupWindow popup = new PopupWindow(act);
    popup.setOutsideTouchable(true);
    popup.setFocusable(true);
    popup.setBackgroundDrawable(new ColorDrawable(0));

    FrameLayout wrap = new FrameLayout(act);
    wrap.setPadding(0, outerPad, 0, outerPad);

    LinearLayout container = new LinearLayout(act);
    container.setOrientation(LinearLayout.VERTICAL);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(bgColor);
    bg.setCornerRadius(corner);
    container.setBackground(bg);
    container.setClipToOutline(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        final int fc = corner;
        container.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), fc);
            }
        });
    }
    wrap.addView(container, new FrameLayout.LayoutParams(-1, -2));

    int itemHeight = dp(act, 46);
    for (int i = 0; i < items.length; i++) {
        final int idx = i;
        boolean selected = (i == selectedIndex);
        TextView tv = new TextView(act);
        tv.setText(items[i]);
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        tv.setPadding(dp(act, 20), 0, dp(act, 20), 0);
        tv.setLayoutParams(new LinearLayout.LayoutParams(-1, itemHeight));
        tv.setTextColor(selected ? accentColor : textColor);
        if (selected) tv.setTypeface(null, Typeface.BOLD);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);

        StateListDrawable sld = new StateListDrawable();
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(pressedColor);
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.TRANSPARENT);
        sld.addState(new int[0], normal);
        tv.setBackground(sld);

        tv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                popup.dismiss();
                if (callback != null) callback.onPicked(idx);
            }
        });
        container.addView(tv);

        if (i < items.length - 1) {
            View div = new View(act);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, 1);
            dlp.leftMargin = dp(act, 12);
            dlp.rightMargin = dp(act, 12);
            div.setLayoutParams(dlp);
            div.setBackgroundColor(dividerColor);
            container.addView(div);
        }
    }

    popup.setContentView(wrap);

    int screenW = act.getResources().getDisplayMetrics().widthPixels;
    int screenH = act.getResources().getDisplayMetrics().heightPixels;
    int popupW = Math.max(dp(act, 180), anchor.getWidth());

    wrap.measure(
        View.MeasureSpec.makeMeasureSpec(popupW, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(screenH, View.MeasureSpec.AT_MOST));
    int popupH = Math.min(wrap.getMeasuredHeight(), (int)(screenH * 0.6));

    popup.setWidth(popupW);
    popup.setHeight(popupH);

    int[] loc = new int[2];
    anchor.getLocationOnScreen(loc);

    int x = loc[0];
    if (x + popupW > screenW - dp(act, 8)) x = screenW - popupW - dp(act, 8);
    if (x < dp(act, 8)) x = dp(act, 8);

    int y = loc[1] + anchor.getHeight() + dp(act, 4);
    if (y + popupH > screenH - dp(act, 8)) {
        y = loc[1] - popupH - dp(act, 4);
        if (y < dp(act, 8)) y = dp(act, 8);
    }

    popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
}

private String[] api3StatTypes() {
    return new String[] {
        "Receive", "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo",
        "ReceiveEmoji", "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip",
        "ReceiveWordCount", "ReceiveUnknown",
        "Send", "SendText", "SendPic", "SendFile", "SendVideo",
        "SendEmoji", "SendAudio", "SendCard", "SendCall", "Command", "Like",
        "SendWordCount", "SendUnknown"
    };
}

private static final String[][] STAT_CATEGORIES = {
    {"Text", "📝 文本"},
    {"Pic", "🖼️ 图片"},
    {"File", "📎 文件"},
    {"Video", "🎬 视频"},
    {"Emoji", "😊 表情"},
    {"Audio", "🎙️ 语音"},
    {"Card", "🍭 卡片"},
    {"Call", "📞 通话"},
    {"WordCount", "🔢 字数"},
    {"GrayTip", "📋 灰字提示"},
    {"Unknown", "🤔 未知"}
};

private String catKeyOf(String msgType) {
    if ("text".equals(msgType)) return "Text";
    if ("pic".equals(msgType)) return "Pic";
    if ("file".equals(msgType)) return "File";
    if ("video".equals(msgType)) return "Video";
    if ("emoji".equals(msgType)) return "Emoji";
    if ("audio".equals(msgType)) return "Audio";
    if ("card".equals(msgType)) return "Card";
    if ("call".equals(msgType)) return "Call";
    if ("grayTip".equals(msgType)) return "GrayTip";
    return "Unknown";
}

private String statsConfigPath() {
    return pluginPath + "/config/msg_stats.json";
}

private String timeRangeName(int range) {
    if (range == 1) return "昨日";
    if (range == 2) return "本周";
    if (range == 3) return "本月";
    if (range == 4) return "自定义日期";
    return "今日";
}

private long atomicIncrement(String key) {
    synchronized (SC.writeLock) {
        Long current = (Long) SC.OP_STATS.get(key);
        long newValue = (current != null ? current.longValue() + 1 : 1);
        SC.OP_STATS.put(key, Long.valueOf(newValue));
        addChangedKey(key);
        return newValue;
    }
}

private long atomicAdd(String key, long delta) {
    synchronized (SC.writeLock) {
        Long current = (Long) SC.OP_STATS.get(key);
        long newValue = (current != null ? current.longValue() + delta : delta);
        SC.OP_STATS.put(key, Long.valueOf(newValue));
        addChangedKey(key);
        return newValue;
    }
}

private long atomicGet(String key) {
    synchronized (SC.writeLock) {
        Long value = (Long) SC.OP_STATS.get(key);
        return value != null ? value.longValue() : 0L;
    }
}

private void addChangedKey(String key) {
    synchronized (SC.writeLock) {
        if (!SC.CHANGED_KEYS.contains(key)) SC.CHANGED_KEYS.add(key);
    }
}

private String formatStatValue(long value) {
    if (value >= 10000) {
        double w = (double) value / 10000.0;
        return String.valueOf(Math.round(w * 10) / 10.0) + " w";
    }
    return String.valueOf(value);
}

private void startWriteThread() {
    if (SC.writeThreadRunning) return;
    SC.writeThreadRunning = true;
    ThreadPool.execute(new Runnable() {
        public void run() {
            long lastWriteTime = 0L;
            while (SC.writeThreadRunning) {
                try {
                    synchronized(SC.writeLock) {
                        if (SC.messageBatchQueue.isEmpty() && SC.CHANGED_KEYS.isEmpty()) {
                            SC.writeLock.wait();
                        }
                        if (!SC.messageBatchQueue.isEmpty()) processBatch();
                    }
                    int pendingKeys = SC.CHANGED_KEYS.size();
                    long nowW = System.currentTimeMillis();
                    if (pendingKeys > 0) {
                        if (nowW - lastWriteTime >= 30000L || pendingKeys > 100) {
                            writeStats();
                            lastWriteTime = System.currentTimeMillis();
                        } else {
                            synchronized(SC.writeLock) {
                                SC.writeLock.wait(30000L - (nowW - lastWriteTime));
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    traceLog("api3_log", "[startWriteThread] 运行时错误: " + e.getMessage());
                }
            }
        }
    });
}

private void stopWriteThread() {
    SC.writeThreadRunning = false;
    synchronized(SC.writeLock) { SC.writeLock.notifyAll(); }
}

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
    boolean hasText = false, hasPic = false, hasVideo = false, hasFile = false;
    boolean hasVoice = false, hasEmoji = false, hasCard = false;
    for (int i = 0; i < elements.size(); i++) {
        Object element = elements.get(i);
        if (element == null) continue;
        if (element.picElement != null) hasPic = true;
        else if (element.videoElement != null) hasVideo = true;
        else if (element.fileElement != null) hasFile = true;
        else if (element.pttElement != null) hasVoice = true;
        else if (element.marketFaceElement != null || element.faceElement != null) hasEmoji = true;
        else if (element.arkElement != null || element.structMsgElement != null) hasCard = true;
        else if (element.textElement != null) {
            if (element.textElement.content != null && element.textElement.content.length() > 0) hasText = true;
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

private long getPureWordCount(Object data) {
    if (data == null || data.data == null || data.data.elements == null) return 0;
    long totalLen = 0;
    List elements = data.data.elements;
    for (int i = 0; i < elements.size(); i++) {
        Object element = elements.get(i);
        if (element != null && element.textElement != null) {
            String content = element.textElement.content;
            if (content != null) totalLen += content.length();
        }
    }
    return totalLen;
}

private void checkTodayReset() {
    String currentDate = getTodayDateStr();
    if (SC.todayDateStr == null || SC.todayDateStr.isEmpty()) {
        SC.todayDateStr = currentDate;
        return;
    }
    if (!currentDate.equals(SC.todayDateStr)) {
        SC.todayDateStr = currentDate;
        synchronized(SC.writeLock) { writeStats(); }
        triggerUIUpdate();
        SC.weekDatesCache.clear();
        SC.monthDatesCache.clear();
        SC.weekCacheKey = "";
        SC.monthCacheKey = "";
    }
}

private int getDailyTargetFromPrefs() {
    int target = 0;
    try {
        String s = getString("settings", "msg_stats_daily_target", "");
        if (s != null && !s.trim().isEmpty()) target = Integer.parseInt(s.trim());
    } catch (Throwable ignore) {}
    if (target > 0) return target;
    Activity activity = getNowActivity();
    if (activity != null) {
        try {
            SharedPreferences sp = activity.getSharedPreferences("msg_stats_config", Activity.MODE_PRIVATE);
            int legacy = sp.getInt("daily_msg_target", 100);
            if (legacy > 0) return legacy;
        } catch (Throwable ignore) {}
    }
    return 100;
}

private int getColorForStat(int index) {
    int[] colors = {
        pc("#FF6B6B"), pc("#4ECDC4"), pc("#45B7D1"),
        pc("#96CEB4"), pc("#FFEAA7"), pc("#DDA0DD"),
        pc("#FFA07A"), pc("#87CEEB"), pc("#F0E68C"),
        pc("#CD853F"), pc("#98FB98")
    };
    if (index >= 0 && index < colors.length) return colors[index];
    return pc("#333333");
}

private View createSpaceView(Activity activity, int heightDp) {
    View space = new View(activity);
    space.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, heightDp)));
    return space;
}

private View createDivider(Activity activity) {
    View divider = new View(activity);
    divider.setBackgroundColor(pc(getSettingsThemeColor(activity, "outline")));
    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, 1);
    dividerParams.setMargins(0, dp(activity, 6), 0, dp(activity, 6));
    divider.setLayoutParams(dividerParams);
    return divider;
}

private void initTimeRange(Activity activity) {
    if (activity == null) { SC.currentTimeRange = 0; return; }
    SharedPreferences sp = activity.getSharedPreferences("msg_stats_config", Activity.MODE_PRIVATE);
    int savedOrdinal = sp.getInt("selected_time_range", 0);
    switch (savedOrdinal) {
        case 1: SC.currentTimeRange = 1; break;
        case 2: SC.currentTimeRange = 2; break;
        case 3: SC.currentTimeRange = 3; break;
        case 4: SC.currentTimeRange = 4; break;
        default: SC.currentTimeRange = 0; break;
    }
    SC.customDateStr = sp.getString("custom_selected_date", null);
    if (SC.customDateStr == null) SC.customDateStr = getTodayDateStr();
}

private synchronized void readFullStats() {
    String configName = statsConfigPath();
    if (configName == null || configName.isEmpty()) return;
    File mainFile = new File(statsConfigPath());
    if (mainFile.exists() && parseStatsFile(mainFile)) return;
    File backupFile = new File(statsConfigPath() + ".bak");
    if (backupFile.exists() && parseStatsFile(backupFile)) {
        Toast("已从备份文件恢复数据");
        return;
    }
    SC.msgHandle.post(new Runnable() {
        public void run() {
            initEmptyStats();
            Toast("无历史数据，初始化新统计");
        }
    });
}

private boolean parseStatsFile(File file) {
    BufferedReader bf = null;
    try {
        bf = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8), 8192);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bf.readLine()) != null) sb.append(line);
        String jsonStr = sb.length() == 0 ? "{}" : sb.toString();
        JSONObject statsJson = new JSONObject(jsonStr);
        Iterator jsonIterator = statsJson.keys();
        while (jsonIterator.hasNext()) {
            String key = (String) jsonIterator.next();
            long value = statsJson.optLong(key, 0L);
            SC.OP_STATS.put(key, new Long(value));
        }
        return true;
    } catch (Exception e) {
        traceLog("api3_log", "[parseStatsFile] " + file.getName() + " - " + e.getMessage());
        return false;
    } finally {
        try { if (bf != null) bf.close(); } catch (Throwable e) {}
    }
}

private void initEmptyStats() {
    synchronized(SC.writeLock) {
        SC.OP_STATS.clear();
        SC.CHANGED_KEYS.clear();
        String[] statTypes = api3StatTypes();
        for (int i = 0; i < statTypes.length; i++) {
            SC.OP_STATS.put("total" + statTypes[i], new Long(0L));
        }
    }
}

public void initStats() {
    if (!getBoolean("settings", "消息统计开关", false)) return;
    Activity activity = getNowActivity();
    initTimeRange(activity);
    readFullStats();
    startWriteThread();
}

private void processBatch() {
    if (SC.messageBatchQueue.isEmpty()) return;

    List batch = new ArrayList();
    synchronized(SC.writeLock) {
        while (!SC.messageBatchQueue.isEmpty() && batch.size() < 10) {
            batch.add(SC.messageBatchQueue.remove(0));
        }
    }
    if (batch.isEmpty()) return;

    boolean wlOn = getBoolean("settings", "msg_stats_whitelist_enabled", false);
    HashSet wlG = new HashSet();
    HashSet wlF = new HashSet();
    if (wlOn) {
        String gs = getString("settings", "msg_stats_whitelist_groups", "");
        String fs = getString("settings", "msg_stats_whitelist_friends", "");
        if (gs != null) {
            String[] parts = gs.split(",");
            for (int i = 0; i < parts.length; i++) {
                String s = parts[i].trim();
                if (!s.isEmpty()) wlG.add(s);
            }
        }
        if (fs != null) {
            String[] parts = fs.split(",");
            for (int i = 0; i < parts.length; i++) {
                String s = parts[i].trim();
                if (!s.isEmpty()) wlF.add(s);
            }
        }
    }

    boolean sendOn = getBoolean("settings", "msg_stats_send_enabled", true);
    boolean recvOn = getBoolean("settings", "msg_stats_receive_enabled", true);
    boolean[] catOn = new boolean[STAT_CATEGORIES.length];
    for (int i = 0; i < STAT_CATEGORIES.length; i++) {
        catOn[i] = getBoolean("settings", "msg_stats_cat_" + STAT_CATEGORIES[i][0], true);
    }

    int wcIdx = -1;
    for (int k = 0; k < STAT_CATEGORIES.length; k++) {
        if ("WordCount".equals(STAT_CATEGORIES[k][0])) { wcIdx = k; break; }
    }
    boolean wcOn = wcIdx >= 0 && catOn[wcIdx];

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);

    for (int i = 0; i < batch.size(); i++) {
        Object data = batch.get(i);
        String senderUin = data.userUin != null ? data.userUin.trim() : "";
        String msgTypeStr = getMsgType(data);
        String msgDateStr;
        try {
            long msgTimeSec = data.time;
            msgDateStr = msgTimeSec > 0 ? sdf.format(new Date(msgTimeSec * 1000)) : SC.todayDateStr;
        } catch (Exception e) {
            msgDateStr = SC.todayDateStr;
        }

        String dateKeyPrefix = "date_" + msgDateStr + "_";
        boolean isSend = myUin.equals(senderUin);
        if (isSend && !sendOn) continue;
        if (!isSend && !recvOn) continue;

        String cat = catKeyOf(msgTypeStr);
        int catIdx = -1;
        for (int k = 0; k < STAT_CATEGORIES.length; k++) {
            if (STAT_CATEGORIES[k][0].equals(cat)) { catIdx = k; break; }
        }
        boolean catEnabled = catIdx >= 0 && catOn[catIdx];

        long wordCount = 0;
        if ("text".equals(msgTypeStr) && catEnabled && wcOn) {
            wordCount = getPureWordCount(data);
        }

        String wlPrefix = null;
        if (wlOn) {
            try {
                int chatType = data.type;
                String peerUin = String.valueOf(data.peerUin);
                if (chatType == 2 && wlG.contains(peerUin)) wlPrefix = "g" + peerUin;
                else if (chatType == 1 && wlF.contains(peerUin)) wlPrefix = "f" + peerUin;
            } catch (Throwable ignore) {}
        }

        if (isSend) {
            atomicIncrement(dateKeyPrefix + "Send");
            atomicIncrement("totalSend");
            if (catEnabled) {
                atomicIncrement(dateKeyPrefix + "Send" + cat);
                atomicIncrement("totalSend" + cat);
            }
            if (wordCount > 0) {
                atomicAdd(dateKeyPrefix + "SendWordCount", wordCount);
                atomicAdd("totalSendWordCount", wordCount);
            }
            if (wlPrefix != null) {
                atomicIncrement(dateKeyPrefix + "Send#" + wlPrefix);
                atomicIncrement("totalSend#" + wlPrefix);
                if (wordCount > 0) {
                    atomicAdd(dateKeyPrefix + "SendWordCount#" + wlPrefix, wordCount);
                    atomicAdd("totalSendWordCount#" + wlPrefix, wordCount);
                }
            }
        } else {
            atomicIncrement(dateKeyPrefix + "Receive");
            atomicIncrement("totalReceive");
            if (catEnabled) {
                atomicIncrement(dateKeyPrefix + "Receive" + cat);
                atomicIncrement("totalReceive" + cat);
            }
            if (wordCount > 0) {
                atomicAdd(dateKeyPrefix + "ReceiveWordCount", wordCount);
                atomicAdd("totalReceiveWordCount", wordCount);
            }
            if (wlPrefix != null) {
                atomicIncrement(dateKeyPrefix + "Receive#" + wlPrefix);
                atomicIncrement("totalReceive#" + wlPrefix);
                if (wordCount > 0) {
                    atomicAdd(dateKeyPrefix + "ReceiveWordCount#" + wlPrefix, wordCount);
                    atomicAdd("totalReceiveWordCount#" + wlPrefix, wordCount);
                }
            }
        }
    }
    triggerUIUpdate();
}

public void onMsg(Object data) {
    try {
        dispatchEvent(data, 1);
        checkTodayReset();
        SC.logMsgCounter++;
        long nowMs = System.currentTimeMillis();
        if (!SC.logCheckRunning && (nowMs - SC.lastLogCheckTime > 60000L || SC.logMsgCounter >= 200)) {
            SC.lastLogCheckTime = nowMs;
            SC.logMsgCounter = 0;
            SC.logCheckRunning = true;
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try { log大小限制(logPath); } catch (Throwable t) {}
                    SC.logCheckRunning = false;
                }
            });
        }
    } catch (Throwable e) { traceLog("api3_log", "[onMsg]" + e); }

    if (!getBoolean("settings", "消息统计开关", false)) return;

    synchronized(SC.writeLock) {
        SC.messageBatchQueue.add(data);
        SC.writeLock.notifyAll();
    }
}

private void writeStats() {
    String configName = statsConfigPath();
    if (SC.CHANGED_KEYS.isEmpty() || configName == null || configName.isEmpty()) return;
    Vector keysToWrite = null;
    synchronized(SC.writeLock) { keysToWrite = new Vector(SC.CHANGED_KEYS); }
    if (keysToWrite != null && !keysToWrite.isEmpty()) {
        writeFullStatsInternal();
        synchronized(SC.writeLock) { SC.CHANGED_KEYS.removeAll(keysToWrite); }
    }
}

private void writeFullStatsInternal() {
    JSONObject statsJson = new JSONObject();
    Iterator iterator = SC.OP_STATS.entrySet().iterator();
    while (iterator.hasNext()) {
        Map.Entry entry = (Map.Entry) iterator.next();
        Long value = (Long) entry.getValue();
        statsJson.put((String) entry.getKey(), value.longValue());
    }
    String jsonStr = statsJson.toString(2);

    File mainFile = new File(statsConfigPath());
    File tempFile = new File(statsConfigPath() + ".tmp");
    File parentDir = mainFile.getParentFile();
    if (!parentDir.exists() && !parentDir.mkdirs()) return;

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
            tempFile.delete();
            return;
        }
        if (!tempFile.renameTo(mainFile)) {
            tempFile.delete();
            return;
        }
        createSingleBackup();
    } catch (Exception e) {
        traceLog("api3_log", "[writeFullStatsInternal] 写入失败: " + e.getMessage());
        if (tempFile != null) tempFile.delete();
    } finally {
        if (osw != null) try { osw.close(); } catch (Throwable e) {}
        if (fos != null) try { fos.close(); } catch (Throwable e) {}
    }
}

private void recalculateTotalStats() {
    String[] statTypes = api3StatTypes();
    for (int i = 0; i < statTypes.length; i++) {
        String type = statTypes[i];
        long total = 0L;
        String keySuffix = "_" + type;
        List snapshot = new ArrayList(SC.OP_STATS.entrySet());
        Iterator iterator = snapshot.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            if (key.startsWith("date_") && key.endsWith(keySuffix)) {
                total += ((Long) entry.getValue()).longValue();
            }
        }
        SC.OP_STATS.put("total" + type, new Long(total));
        addChangedKey("total" + type);
    }
}

private void createSingleBackup() {
    if (System.currentTimeMillis() - SC.lastBackupTime < 600000L) return;
    File mainFile = new File(statsConfigPath());
    if (!mainFile.exists() || mainFile.length() == 0) return;
    File backupFile = new File(statsConfigPath() + ".bak");
    if (backupFile.exists()) backupFile.delete();
    try {
        java.nio.file.Files.copy(mainFile.toPath(), backupFile.toPath());
        SC.lastBackupTime = System.currentTimeMillis();
    } catch (Exception e) {}
}

private void triggerUIUpdate() {
    if (!SC.pageVisible || SC.contentRoot == null) return;
    long currentTime = System.currentTimeMillis();
    if (currentTime - SC.lastUIUpdateTime < 150) {
        SC.msgHandle.removeCallbacksAndMessages(null);
        SC.msgHandle.postDelayed(new Runnable() {
            public void run() { updateUIImmediately(); }
        }, 150);
        return;
    }
    SC.lastUIUpdateTime = currentTime;
    SC.msgHandle.post(new Runnable() {
        public void run() {
            try { updateUIImmediately(); } catch (Throwable e) {}
        }
    });
}

private void updateUIImmediately() {
    if (SC.contentRoot == null) return;

    final String rangeName;
    if (SC.currentTimeRange == 4 && SC.customDateStr != null) rangeName = formatDateForDisplay(SC.customDateStr);
    else rangeName = timeRangeName(SC.currentTimeRange);

    final long todaySend = atomicGet("date_" + SC.todayDateStr + "_Send");
    final int dailyTarget = getDailyTargetFromPrefs();
    final long totalReceive = atomicGet("totalReceive");
    final long totalSend = atomicGet("totalSend");

    updateCachedTextView("totalReceive", "累计: " + formatStatValue(totalReceive));
    updateCachedTextView("totalSend", "累计: " + formatStatValue(totalSend));
    updateCachedTextView("Receive", rangeName + ": " + formatStatValue(getStatsByTimeRange("Receive")));
    updateCachedTextView("Send", rangeName + ": " + formatStatValue(getStatsByTimeRange("Send")));

    for (int i = 0; i < STAT_CATEGORIES.length; i++) {
        String cat = STAT_CATEGORIES[i][0];
        if ("GrayTip".equals(cat)) {
            long rv = getStatsByTimeRange("ReceiveGrayTip");
            long tv = atomicGet("totalReceiveGrayTip");
            updateCachedTextView("ReceiveGrayTip", rangeName + ": " + formatStatValue(rv) + "  累计: " + formatStatValue(tv));
        } else if ("WordCount".equals(cat)) {
            long rv = getStatsByTimeRange("SendWordCount") + getStatsByTimeRange("ReceiveWordCount");
            long tv = atomicGet("totalSendWordCount") + atomicGet("totalReceiveWordCount");
            updateCachedTextView("SendWordCount", rangeName + ": " + formatStatValue(rv) + "  累计: " + formatStatValue(tv));
        } else {
            long rvS = getStatsByTimeRange("Send" + cat);
            long tvS = atomicGet("totalSend" + cat);
            updateCachedTextView("Send" + cat, rangeName + ": " + formatStatValue(rvS) + "  累计: " + formatStatValue(tvS));
            long rvR = getStatsByTimeRange("Receive" + cat);
            long tvR = atomicGet("totalReceive" + cat);
            updateCachedTextView("Receive" + cat, rangeName + ": " + formatStatValue(rvR) + "  累计: " + formatStatValue(tvR));
        }
    }

    updateTodayProgress(todaySend, dailyTarget);

    if (SC.whitelistDetailCache != null && SC.contentRoot != null) {
        View wlCard = SC.contentRoot.findViewWithTag("whitelistCard");
        if (wlCard != null) updateWhitelistDetail(wlCard, SC.whitelistCurrentIndex);
    }
}

private void updateWhitelistDetail(View wlCard, int idx) {
    if (wlCard == null || SC.whitelistDetailCache == null) return;
    Object tagObj = wlCard.getTag();
    if (!(tagObj instanceof Object[])) return;
    Object[] meta = (Object[]) tagObj;
    List prefixes = (List) meta[0];
    Map nameCache = (Map) meta[1];
    if (prefixes == null || prefixes.isEmpty()) return;
    if (idx < 0 || idx >= prefixes.size()) return;
    String prefix = (String) prefixes.get(idx);
    String name = (nameCache != null && nameCache.containsKey(prefix))
        ? (String) nameCache.get(prefix) : prefix.substring(1);
    String uin = prefix.substring(1);
    String tag2 = prefix.startsWith("g") ? "群" : "好友";

    long rSend = getStatsByTimeRange("Send#" + prefix);
    long rRecv = getStatsByTimeRange("Receive#" + prefix);
    long rWords = getStatsByTimeRange("SendWordCount#" + prefix) + getStatsByTimeRange("ReceiveWordCount#" + prefix);
    long tSend = atomicGet("totalSend#" + prefix);
    long tRecv = atomicGet("totalReceive#" + prefix);
    long tWords = atomicGet("totalSendWordCount#" + prefix) + atomicGet("totalReceiveWordCount#" + prefix);

    String rangeName = (SC.currentTimeRange == 4 && SC.customDateStr != null)
        ? formatDateForDisplay(SC.customDateStr) : timeRangeName(SC.currentTimeRange);

    StringBuilder sb = new StringBuilder();
    sb.append(tag2).append(": ").append(name).append("(").append(uin).append(")\n\n");
    sb.append(rangeName).append(": 收 ").append(rRecv).append(" 条 · 发 ").append(rSend)
      .append(" 条 · 字数 ").append(rWords).append("\n");
    sb.append("累计: 收 ").append(tRecv).append(" 条 · 发 ").append(tSend)
      .append(" 条 · 字数 ").append(tWords);
    SC.whitelistDetailCache.setText(sb.toString());
}

private void updateCachedTextView(String tag, String text) {
    if (tag == null || text == null) return;
    TextView tv = null;
    for (int i = 0; i < SC.statsTextViewCache.size(); i++) {
        Object item = SC.statsTextViewCache.get(i);
        if (item instanceof TextView && tag.equals(((TextView) item).getTag())) {
            tv = (TextView) item;
            break;
        }
    }
    if (tv == null) {
        if (SC.contentRoot != null) {
            tv = (TextView) SC.contentRoot.findViewWithTag(tag);
            if (tv != null) SC.statsTextViewCache.add(tv);
            else return;
        } else return;
    }
    if (!text.equals(tv.getText().toString())) tv.setText(text);
}

private void updateTodayProgress(long todaySend, int dailyTarget) {
    if (SC.todayCoreCardCache == null) return;
    int progress = (int) Math.min(todaySend * 100 / Math.max(dailyTarget, 1), 100);
    int remaining = Math.max(dailyTarget - (int) todaySend, 0);
    if (SC.sendTargetLabelCache != null) {
        SC.sendTargetLabelCache.setText("今天已经逼逼了" + todaySend + "句，还差" + remaining + " 句，目标：" + dailyTarget + "(" + progress + "%)");
    }
    if (SC.progressBarCache != null && SC.todayCoreCardCache.getWidth() > 0) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) SC.progressBarCache.getLayoutParams();
        params.width = (int) (SC.todayCoreCardCache.getWidth() * progress / 100.0f);
        SC.progressBarCache.setLayoutParams(params);
    }
    if (SC.achievementTextCache != null) {
        SC.achievementTextCache.setVisibility(progress >= 100 ? View.VISIBLE : View.GONE);
    }
}

private long getStatsByTimeRange(String typeKey) {
    long total = 0L;
    String datePrefix = "date_";
    String keySuffix = "_" + typeKey;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);

    if (SC.currentTimeRange == 0) {
        return atomicGet(datePrefix + SC.todayDateStr + keySuffix);
    } else if (SC.currentTimeRange == 1) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String yesterday = dateFormat.format(cal.getTime());
        return atomicGet(datePrefix + yesterday + keySuffix);
    } else if (SC.currentTimeRange == 2) {
        String cacheKey = SC.todayDateStr + "_week";
        if (!cacheKey.equals(SC.weekCacheKey) || SC.weekDatesCache.isEmpty()) {
            SC.weekDatesCache.clear();
            Calendar cal = Calendar.getInstance();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String today = getTodayDateStr();
            while (true) {
                String currentDate = dateFormat.format(cal.getTime());
                if (currentDate.compareTo(today) > 0) break;
                SC.weekDatesCache.add(currentDate);
                cal.add(Calendar.DATE, 1);
            }
            SC.weekCacheKey = cacheKey;
        }
        for (int i = 0; i < SC.weekDatesCache.size(); i++) {
            total += atomicGet(datePrefix + (String) SC.weekDatesCache.get(i) + keySuffix);
        }
    } else if (SC.currentTimeRange == 3) {
        String cacheKey = SC.todayDateStr.substring(0, 6) + "_month";
        if (!cacheKey.equals(SC.monthCacheKey) || SC.monthDatesCache.isEmpty()) {
            SC.monthDatesCache.clear();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String today = getTodayDateStr();
            String monthPrefix = today.substring(0, 6);
            while (true) {
                String currentDate = dateFormat.format(cal.getTime());
                if (!currentDate.substring(0, 6).equals(monthPrefix)) break;
                SC.monthDatesCache.add(currentDate);
                cal.add(Calendar.DATE, 1);
            }
            SC.monthCacheKey = cacheKey;
        }
        for (int i = 0; i < SC.monthDatesCache.size(); i++) {
            total += atomicGet(datePrefix + (String) SC.monthDatesCache.get(i) + keySuffix);
        }
    } else if (SC.currentTimeRange == 4) {
        if (SC.customDateStr == null) SC.customDateStr = getTodayDateStr();
        return atomicGet(datePrefix + SC.customDateStr + keySuffix);
    }
    return total;
}

private View createRangeSpinner(Activity activity) {
    final Activity finalActivity = activity;
    final int textColor = pc(getSettingsThemeColor(activity, "on_surface"));
    final int accentColor = pc(getSettingsThemeColor(activity, "primary"));
    final int cardBgColor = getAdaptiveCardBg(activity);
    final int capsuleBgNormal = tca(activity, "on_surface", 20);
    final int capsuleBgPressed = tca(activity, "on_surface", 40);

    LinearLayout layout = new LinearLayout(finalActivity);
    layout.setGravity(Gravity.CENTER_VERTICAL);
    layout.setBackground(roundRect(cardBgColor, dp(finalActivity, 16)));
    layout.setPadding(dp(finalActivity, 16), dp(finalActivity, 14), dp(finalActivity, 16), dp(finalActivity, 14));

    TextView label = new TextView(finalActivity);
    label.setText("📅  时间范围：");
    label.setTextColor(textColor);
    label.setTextSize(15);
    label.setTypeface(null, Typeface.BOLD);
    layout.addView(label);

    View spacer = new View(finalActivity);
    layout.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

    final String[] rangeNames = {"今日", "昨日", "本周", "本月", "自定义日期"};
    final TextView trigger = new TextView(finalActivity);
    trigger.setText(rangeNames[SC.currentTimeRange]);
    trigger.setTextSize(13);
    trigger.setTypeface(null, Typeface.BOLD);
    trigger.setTextColor(accentColor);
    trigger.setGravity(Gravity.CENTER);
    trigger.setPadding(dp(finalActivity, 16), 0, dp(finalActivity, 16), 0);
    trigger.setSingleLine(true);

    StateListDrawable capsuleDrawable = new StateListDrawable();
    GradientDrawable pressedBg = new GradientDrawable();
    pressedBg.setColor(capsuleBgPressed);
    pressedBg.setCornerRadius(dp(finalActivity, 20));
    capsuleDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
    GradientDrawable normalBg = new GradientDrawable();
    normalBg.setColor(capsuleBgNormal);
    normalBg.setCornerRadius(dp(finalActivity, 20));
    capsuleDrawable.addState(new int[0], normalBg);
    trigger.setBackground(capsuleDrawable);

    trigger.setLayoutParams(new LinearLayout.LayoutParams(dp(finalActivity, 130), dp(finalActivity, 36)));

    trigger.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            showStyledSelector(finalActivity, trigger, rangeNames, SC.currentTimeRange, new OnSelectorPickedListener() {
                public void onPicked(int position) {
                    int selected = 0;
                    boolean needShowDatePicker = false;
                    switch (position) {
                        case 1: selected = 1; break;
                        case 2: selected = 2; break;
                        case 3: selected = 3; break;
                        case 4: selected = 4; needShowDatePicker = true; break;
                    }
                    SC.currentTimeRange = selected;
                    SharedPreferences sp = finalActivity.getSharedPreferences("msg_stats_config", Activity.MODE_PRIVATE);
                    sp.edit().putInt("selected_time_range", position).apply();
                    if (selected == 2) { SC.weekDatesCache.clear(); SC.weekCacheKey = ""; }
                    else if (selected == 3) { SC.monthDatesCache.clear(); SC.monthCacheKey = ""; }
                    trigger.setText(rangeNames[position]);
                    if (needShowDatePicker) showDatePickerDialog(finalActivity);
                    updateUIImmediately();
                }
            });
        }
    });

    layout.addView(trigger);
    return layout;
}

private void showDatePickerDialog(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);
    final Calendar calendar = Calendar.getInstance();
    if (SC.customDateStr != null) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
            calendar.setTime(sdf.parse(SC.customDateStr));
        } catch (Throwable e) {}
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
                String sy = "0000" + year;
                String sm = "00" + month;
                String sd = "00" + day;
                SC.customDateStr = sy.substring(sy.length() - 4) + sm.substring(sm.length() - 2) + sd.substring(sd.length() - 2);
                SharedPreferences sp = activity.getSharedPreferences("msg_stats_config", Activity.MODE_PRIVATE);
                sp.edit().putString("custom_selected_date", SC.customDateStr).apply();
                Toast("已选择日期: " + formatDateForDisplay(SC.customDateStr));
                triggerUIUpdate();
            }
        })
        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SC.currentTimeRange = 0;
                SharedPreferences sp = activity.getSharedPreferences("msg_stats_config", Activity.MODE_PRIVATE);
                sp.edit().putInt("selected_time_range", 0).apply();
                triggerUIUpdate();
            }
        })
        .show();
}

private LinearLayout createStatsCardBase(Activity activity, String title, String cardType) {
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 12)));
    card.setTag(cardType);
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    LinearLayout titleBar = new LinearLayout(activity);
    titleBar.setOrientation(LinearLayout.HORIZONTAL);
    titleBar.setGravity(Gravity.CENTER_VERTICAL);
    final LinearLayout fCard = card;
    final String fCardType = cardType;
    titleBar.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            Boolean current = (Boolean) SC.cardExpandStatus.get(fCardType);
            boolean isExpanded = current != null && current;
            SC.cardExpandStatus.put(fCardType, !isExpanded);
            updateCardExpandStatus(fCard, !isExpanded);
        }
    });

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), Typeface.BOLD);
    titleBar.addView(cardTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

    TextView arrowTv = new TextView(activity);
    arrowTv.setText("▶");
    arrowTv.setTextSize(14);
    arrowTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    arrowTv.setTag(cardType + "_arrow");
    titleBar.addView(arrowTv);

    card.addView(titleBar);

    LinearLayout statsContainer = new LinearLayout(activity);
    statsContainer.setOrientation(LinearLayout.VERTICAL);
    statsContainer.setTag(cardType + "_container");
    statsContainer.setVisibility(View.GONE);
    card.addView(statsContainer);

    SC.cardExpandStatus.put(cardType, false);
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
            container.startAnimation(new ExpandAnimation(container, 300));
        } else {
            Animation collapseAnim = new CollapseAnimation(container, 300);
            collapseAnim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {}
                public void onAnimationEnd(Animation animation) { container.setVisibility(View.GONE); }
                public void onAnimationRepeat(Animation animation) {}
            });
            container.startAnimation(collapseAnim);
        }
        SC.cardExpandStatus.put(cardType, isExpanded);
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
    public boolean willChangeBounds() { return true; }
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
    public boolean willChangeBounds() { return true; }
}

private LinearLayout createStatsItemLayout(Activity activity, String label, String typeKey, int colorIndex, boolean withTotal) {
    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(0, dp(activity, 6), 0, dp(activity, 6));

    TextView iconTv = new TextView(activity);
    iconTv.setText(label.substring(0, 2));
    iconTv.setTextSize(16);
    iconTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    iconTv.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 30), dp(activity, 30)));

    TextView labelTv = new TextView(activity);
    labelTv.setText(label.substring(3));
    labelTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    labelTv.setTextSize(14);
    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

    TextView valueTv = new TextView(activity);
    if (withTotal) {
        long rangeValue = getStatsByTimeRange(typeKey);
        long totalValue = atomicGet("total" + typeKey);
        valueTv.setText(timeRangeName(SC.currentTimeRange) + ": " + formatStatValue(rangeValue) + "  累计: " + formatStatValue(totalValue));
    } else {
        long value;
        String textPrefix;
        if (typeKey.startsWith("total")) {
            value = atomicGet(typeKey);
            textPrefix = "累计: ";
        } else {
            value = getStatsByTimeRange(typeKey);
            textPrefix = timeRangeName(SC.currentTimeRange) + ": ";
        }
        valueTv.setText(textPrefix + formatStatValue(value));
    }
    valueTv.setTag(typeKey);
    valueTv.setTextColor(getColorForStat(colorIndex));
    valueTv.setTextSize(withTotal ? 14 : 16);
    if (!withTotal) valueTv.setTypeface(valueTv.getTypeface(), Typeface.BOLD);

    itemLayout.addView(iconTv);
    itemLayout.addView(labelTv);
    itemLayout.addView(valueTv);
    return itemLayout;
}

private LinearLayout createTodayCoreStatsCard(Activity activity, String title) {
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 12)));
    card.setTag("todayCoreCard");
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTag("core_card_title");
    cardTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), Typeface.BOLD);
    card.addView(cardTitle);

    String[][] items = {{"📥 今日接收总数", "Receive"}, {"📤 今日发送总数", "Send"}};
    for (int i = 0; i < items.length; i++) {
        card.addView(createStatsItemLayout(activity, items[i][0], items[i][1], i, false));
        if (i < items.length - 1) card.addView(createDivider(activity));
    }
    card.addView(createDivider(activity));

    LinearLayout sendTargetLayout = new LinearLayout(activity);
    sendTargetLayout.setOrientation(LinearLayout.VERTICAL);
    sendTargetLayout.setPadding(0, dp(activity, 10), 0, 0);

    TextView sendTargetLabel = new TextView(activity);
    sendTargetLabel.setTag("send_target_label");
    long todaySend = atomicGet("date_" + SC.todayDateStr + "_Send");
    int dailyTarget = getDailyTargetFromPrefs();
    int progress = (int) Math.min(todaySend * 100 / Math.max(dailyTarget, 1), 100);
    sendTargetLabel.setText("今天已经逼逼了" + todaySend + "句，还差" + (dailyTarget - todaySend) + " 句，目标：" + dailyTarget + "(" + progress + "%)");
    sendTargetLabel.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    sendTargetLabel.setTextSize(14);
    sendTargetLayout.addView(sendTargetLabel);

    View progressBg = new View(activity);
    progressBg.setTag("send_progress_bg");
    progressBg.setBackgroundColor(pc(getSettingsThemeColor(activity, "outline")));
    LinearLayout.LayoutParams bgParams = new LinearLayout.LayoutParams(-1, dp(activity, 8));
    bgParams.setMargins(0, dp(activity, 8), 0, 0);
    progressBg.setLayoutParams(bgParams);
    sendTargetLayout.addView(progressBg);

    View progressBar = new View(activity);
    progressBar.setTag("send_progress");
    progressBar.setBackgroundColor(pc("#81C784"));
    LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(0, dp(activity, 8));
    progressParams.setMargins(0, -dp(activity, 8), 0, 0);
    progressBar.setLayoutParams(progressParams);
    sendTargetLayout.addView(progressBar);

    TextView achievementText = new TextView(activity);
    achievementText.setTag("achievement_text");
    achievementText.setText("🎉 今日达成成就：屁话王");
    achievementText.setTextColor(pc("#FF6B6B"));
    achievementText.setTextSize(14);
    achievementText.setTypeface(achievementText.getTypeface(), Typeface.BOLD);
    LinearLayout.LayoutParams achievementParams = new LinearLayout.LayoutParams(-2, -2);
    achievementParams.setMargins(0, dp(activity, 8), 0, 0);
    achievementText.setLayoutParams(achievementParams);
    achievementText.setVisibility(View.GONE);
    sendTargetLayout.addView(achievementText);

    card.addView(sendTargetLayout);
    return card;
}

private LinearLayout createTotalStatsCard(Activity activity, String title) {
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 12)));
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView cardTitle = new TextView(activity);
    cardTitle.setText(title);
    cardTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    cardTitle.setTextSize(17);
    cardTitle.setTypeface(cardTitle.getTypeface(), Typeface.BOLD);
    card.addView(cardTitle);

    String[][] items = {{"📥 累计接收总数", "totalReceive"}, {"📤 累计发送总数", "totalSend"}};
    for (int i = 0; i < items.length; i++) {
        card.addView(createStatsItemLayout(activity, items[i][0], items[i][1], i, false));
        if (i < items.length - 1) card.addView(createDivider(activity));
    }
    return card;
}

private LinearLayout createCategoryStatsCard(Activity activity, String title, String cardType, boolean isReceive) {
    LinearLayout card = createStatsCardBase(activity, title, cardType);
    LinearLayout statsContainer = (LinearLayout) card.findViewWithTag(cardType + "_container");

    int idx = 0;
    for (int i = 0; i < STAT_CATEGORIES.length; i++) {
        String cat = STAT_CATEGORIES[i][0];
        if ("GrayTip".equals(cat) && !isReceive) continue;
        if ("WordCount".equals(cat)) continue;
        String label = STAT_CATEGORIES[i][1];
        String key = (isReceive ? "Receive" : "Send") + cat;
        statsContainer.addView(createStatsItemLayout(activity, label, key, idx, true));
        idx++;
        if (i < STAT_CATEGORIES.length - 1) statsContainer.addView(createDivider(activity));
    }
    statsContainer.addView(createStatsItemLayout(activity, "🔢 字数", (isReceive ? "Receive" : "Send") + "WordCount", idx, true));
    return card;
}

private LinearLayout createWhitelistCard(Activity activity) {
    boolean wlOn = getBoolean("settings", "msg_stats_whitelist_enabled", false);
    if (!wlOn) return null;
    String gs = getString("settings", "msg_stats_whitelist_groups", "");
    String fs = getString("settings", "msg_stats_whitelist_friends", "");
    final List prefixes = new ArrayList();
    if (gs != null && !gs.isEmpty()) {
        String[] parts = gs.split(",");
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (!s.isEmpty()) prefixes.add("g" + s);
        }
    }
    if (fs != null && !fs.isEmpty()) {
        String[] parts = fs.split(",");
        for (int i = 0; i < parts.length; i++) {
            String s = parts[i].trim();
            if (!s.isEmpty()) prefixes.add("f" + s);
        }
    }
    if (prefixes.isEmpty()) return null;

    final Map nameCache = new HashMap();
    final List displayNames = new ArrayList();
    for (int i = 0; i < prefixes.size(); i++) {
        String prefix = (String) prefixes.get(i);
        String uin = prefix.substring(1);
        String name = uin;
        if (prefix.startsWith("g")) {
            try {
                Object info = findTroopInfo(uin);
                if (info != null) {
                    String tn = String.valueOf(info.troopname);
                    if (tn != null && !tn.isEmpty() && !"null".equals(tn)) name = tn;
                }
            } catch (Throwable ignore) {}
        } else {
            try {
                Object card = GetCard(uin);
                if (card != null) {
                    String cn = String.valueOf(card.strNick);
                    if (cn != null && !cn.isEmpty() && !"null".equals(cn)) name = cn;
                }
            } catch (Throwable ignore) {}
        }
        nameCache.put(prefix, name);
        displayNames.add(name + "(" + uin + ")");
    }

    final LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 12)));
    card.setTag(new Object[]{prefixes, nameCache});
    int padding = dp(activity, 16);
    card.setPadding(padding, padding, padding, padding);

    TextView title = new TextView(activity);
    title.setText("🎯 指定数据");
    title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    title.setTextSize(17);
    title.setTypeface(title.getTypeface(), Typeface.BOLD);
    card.addView(title);

    final Activity fActivity = activity;
    final int accentColor = pc(getSettingsThemeColor(activity, "primary"));
    final int chipBgNormal = tca(activity, "on_surface", 20);
    final int chipBgPressed = tca(activity, "on_surface", 40);

    final TextView trigger = new TextView(activity);
    trigger.setText((String) displayNames.get(0));
    trigger.setTextSize(14);
    trigger.setTypeface(null, Typeface.BOLD);
    trigger.setTextColor(accentColor);
    trigger.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
    trigger.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
    trigger.setSingleLine(true);
    trigger.setEllipsize(android.text.TextUtils.TruncateAt.END);

    StateListDrawable chipBg = new StateListDrawable();
    GradientDrawable chipPressed = new GradientDrawable();
    chipPressed.setColor(chipBgPressed);
    chipPressed.setCornerRadius(dp(activity, 10));
    chipBg.addState(new int[]{android.R.attr.state_pressed}, chipPressed);
    GradientDrawable chipNormal = new GradientDrawable();
    chipNormal.setColor(chipBgNormal);
    chipNormal.setCornerRadius(dp(activity, 10));
    chipBg.addState(new int[0], chipNormal);
    trigger.setBackground(chipBg);

    LinearLayout.LayoutParams triggerLp = new LinearLayout.LayoutParams(-1, dp(activity, 44));
    triggerLp.topMargin = dp(activity, 10);
    trigger.setLayoutParams(triggerLp);

    final TextView detail = new TextView(activity);
    detail.setTag("whitelistDetail");
    detail.setTextSize(13);
    detail.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    detail.setPadding(0, dp(activity, 12), 0, 0);
    detail.setLineSpacing(0, 1.15f);

    card.addView(trigger);
    card.addView(detail);

    SC.whitelistDetailCache = detail;
    SC.whitelistCurrentIndex = 0;

    trigger.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String[] items = (String[]) displayNames.toArray(new String[0]);
            showStyledSelector(fActivity, trigger, items, SC.whitelistCurrentIndex, new OnSelectorPickedListener() {
                public void onPicked(int position) {
                    SC.whitelistCurrentIndex = position;
                    trigger.setText((String) displayNames.get(position));
                    updateWhitelistDetail(card, position);
                }
            });
        }
    });

    updateWhitelistDetail(card, 0);
    return card;
}

void performDataCleanup() {
    if (SC == null || SC.writeLock == null) return;
    synchronized(SC.writeLock) {
        try {
            if (SC.OP_STATS != null) SC.OP_STATS.clear();
            if (SC.messageBatchQueue != null) SC.messageBatchQueue.clear();
            if (SC.CHANGED_KEYS != null) SC.CHANGED_KEYS.clear();
            if (SC.cardExpandStatus != null) SC.cardExpandStatus.clear();
            if (SC.statsTextViewCache != null) SC.statsTextViewCache.clear();
            if (SC.weekDatesCache != null) SC.weekDatesCache.clear();
            if (SC.monthDatesCache != null) SC.monthDatesCache.clear();
        } catch (Throwable e) { traceLog("api3_log", "[performDataCleanup] 异常: " + e); }
    }
    SC.todayDateStr = null;
}

private void resetTodayStats(final Activity activity) {
    boolean isDark = isThemeDark(activity);
    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("重置确认")
        .setMessage("确定重置今日统计数据吗？")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                vibrate(activity, 48);
                String todayPrefix = "date_" + SC.todayDateStr + "_";
                synchronized(SC.writeLock) {
                    List toRemove = new ArrayList();
                    Iterator iterator = SC.OP_STATS.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        String key = (String) entry.getKey();
                        if (key.startsWith(todayPrefix)) toRemove.add(key);
                    }
                    for (int i = 0; i < toRemove.size(); i++) SC.OP_STATS.remove(toRemove.get(i));
                    recalculateTotalStats();
                    writeStats();
                }
                SC.weekDatesCache.clear();
                SC.monthDatesCache.clear();
                SC.weekCacheKey = "";
                SC.monthCacheKey = "";
                SC.statsTextViewCache.clear();
                triggerUIUpdate();
                Toast("今日数据已重置");
            }
        })
        .setNegativeButton("取消", null)
        .show();
}

private void resetTotalStats(final Activity activity) {
    boolean isDark = isThemeDark(activity);
    new AlertDialog.Builder(activity, isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("重置确认")
        .setMessage("确定重置所有累计统计数据吗？")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                vibrate(activity, 48);
                synchronized(SC.writeLock) {
                    SC.OP_STATS.clear();
                    String[] statTypes = api3StatTypes();
                    for (int i = 0; i < statTypes.length; i++) {
                        SC.OP_STATS.put("total" + statTypes[i], new Long(0L));
                        addChangedKey("total" + statTypes[i]);
                    }
                    writeStats();
                }
                SC.weekDatesCache.clear();
                SC.monthDatesCache.clear();
                SC.weekCacheKey = "";
                SC.monthCacheKey = "";
                SC.statsTextViewCache.clear();
                triggerUIUpdate();
                Toast("累计数据已重置");
            }
        })
        .setNegativeButton("取消", null)
        .show();
}

private void repairStatsData(final Activity activity) {
    vibrate(activity, 48);
    synchronized(SC.writeLock) {
        recalculateTotalStats();
        writeStats();
    }
    SC.statsTextViewCache.clear();
    triggerUIUpdate();
    Toast("数据已修复完成");
}

Map getSendTypeMapping() {
    Map map = new HashMap();
    map.put("SendText", "文本"); map.put("SendPic", "图片");
    map.put("SendFile", "文件"); map.put("SendVideo", "视频");
    map.put("SendEmoji", "表情"); map.put("SendAudio", "语音");
    map.put("SendCard", "卡片"); map.put("SendCall", "通话");
    map.put("SendWordCount", "字数"); map.put("Send", "总数");
    return map;
}

Map getReceiveTypeMapping() {
    Map map = new HashMap();
    map.put("ReceiveText", "文本"); map.put("ReceivePic", "图片");
    map.put("ReceiveFile", "文件"); map.put("ReceiveVideo", "视频");
    map.put("ReceiveEmoji", "表情"); map.put("ReceiveAudio", "语音");
    map.put("ReceiveCard", "卡片"); map.put("ReceiveCall", "通话");
    map.put("ReceiveWordCount", "字数"); map.put("Receive", "总数");
    return map;
}

Map getAllVariablesMap(Object scriptScope) {
    Map map = new HashMap();
    map.put("time", getTime());
    try { map.put("qq", String.valueOf(myUin)); } catch (Throwable e) {}

    String rawTime = getTodayDateStr();
    String todayStr = "";
    if (rawTime != null) {
        if (rawTime.contains("-")) todayStr = rawTime.split(" ")[0].replace("-", "");
        else if (rawTime.contains("/")) todayStr = rawTime.split(" ")[0].replace("/", "");
        else todayStr = rawTime.length() >= 8 ? rawTime.substring(0, 8) : rawTime;
    }

    Map SEND_TYPE_MAP = getSendTypeMapping();
    Map RECEIVE_TYPE_MAP = getReceiveTypeMapping();

    for (Object sendValue : SEND_TYPE_MAP.values()) {
        map.put("今日发送" + (String) sendValue, "0");
    }
    for (Object receiveValue : RECEIVE_TYPE_MAP.values()) {
        map.put("今日接收" + (String) receiveValue, "0");
    }

    synchronized(SC.writeLock) {
        Iterator iterator = SC.OP_STATS.entrySet().iterator();
        String todayPrefix = "date_" + todayStr + "_";
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            Object valObj = entry.getValue();
            String valueStr = (valObj != null) ? String.valueOf(valObj) : "0";
            if (!key.startsWith("date_") || key.startsWith(todayPrefix)) {
                map.put(key, valueStr);
            }
            if (key.startsWith(todayPrefix)) {
                String suffix = key.substring(todayPrefix.length());
                if (SEND_TYPE_MAP.containsKey(suffix)) {
                    map.put("今日发送" + (String) SEND_TYPE_MAP.get(suffix), valueStr);
                } else if (RECEIVE_TYPE_MAP.containsKey(suffix)) {
                    map.put("今日接收" + (String) RECEIVE_TYPE_MAP.get(suffix), valueStr);
                }
            }
        }
    }
    return map;
}

String getVariableValue(Object scriptScope, String varName) {
    Map allVars = getAllVariablesMap(scriptScope);
    for (Object obj : allVars.entrySet()) {
        Map.Entry entry = (Map.Entry) obj;
        if (((String) entry.getKey()).equalsIgnoreCase(varName)) return (String) entry.getValue();
    }
    return null;
}

String getVarDescription(String key) {
    String desc = "变量值";
    if (key.equalsIgnoreCase("time")) desc = "当前系统时间";
    else if (key.equalsIgnoreCase("qq")) desc = "宿主QQ号";
    else if (key.startsWith("total")) {
        String type = key.substring(5);
        if (type.equals("Send")) desc = "累计发送总数";
        else if (type.equals("Receive")) desc = "累计接收总数";
        else if (type.equals("SendWordCount")) desc = "累计发送字数";
        else if (type.equals("ReceiveWordCount")) desc = "累计接收字数";
        else desc = "累计 " + type + " 数量";
    } else if (key.startsWith("今日发送")) desc = "今日发送" + key.substring(4) + "数量";
    else if (key.startsWith("今日接收")) desc = "今日接收" + key.substring(4) + "数量";
    return desc;
}

final String[] VAR_POOL_GROUPS = { "推荐", "全部", "消息统计", "事件变量", "全局变量" };

final String[] VAR_POOL_FALLBACK_EVENT = {
    "qun", "uin", "msg", "msgId", "msgType", "type", "data", "operator", "time", "paiType", "qq", "pluginPath", "this", "scriptLoader"
};

String varPoolAnyDesc(String key) {
    String d = varPoolDesc(key);
    if (d != null && d.length() > 0) return d;
    try { return getVarDescription(key); } catch (Throwable e) { return "变量"; }
}

final String[][] VAR_DESC_DICT = {
    {"time", "当前系统时间（热插拔里为禁言秒数）"},
    {"qq", "当前登录QQ号"},
    {"qun", "群号（私聊时为空）"},
    {"uin", "触发者QQ号"},
    {"msg", "消息内容"},
    {"msgId", "消息ID"},
    {"msgType", "消息类型"},
    {"type", "聊天类型（1私聊 2群聊）"},
    {"data", "原始消息对象 MsgData"},
    {"operator", "操作者QQ号"},
    {"paiType", "拍一拍类型"},
    {"pluginPath", "插件目录绝对路径"},
    {"this", "当前脚本对象"},
    {"scriptLoader", "热插拔脚本加载器"},
    {"myUin", "当前登录QQ号"},
    {"context", "宿主 Context"},
    {"ThreadPool", "全局线程池"}
};

String varPoolDesc(String name) {
    if (name == null) return "";
    for (int i = 0; i < VAR_DESC_DICT.length; i++) {
        if (VAR_DESC_DICT[i][0].equalsIgnoreCase(name)) return VAR_DESC_DICT[i][1];
    }
    return "";
}

String formatVarValue(Object val) {
    if (val == null) return "null";
    try {
        if (val instanceof bsh.This) return "<脚本对象>";
        String s = String.valueOf(val);
        if (s == null) return "";
        if (s.length() > 120) s = s.substring(0, 120) + "…";
        return s;
    } catch (Throwable e) {
        return "<对象>";
    }
}

List collectVariablePool(String group, String keyword, String context) {
    List out = new ArrayList();
    String g = (group == null || group.length() == 0) ? "推荐" : group;
    boolean all = "全部".equals(g);
    boolean hotplug = "hotplug".equals(context);

    Map statMap = null;
    try { statMap = getAllVariablesMap(this); } catch (Throwable e) { traceLog("api3_log", "[collectVariablePool] 统计表: " + e); }

    if (("推荐".equals(g) && !hotplug) || all || "消息统计".equals(g)) {
        if (statMap != null) {
            Iterator iter = statMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry e = (Map.Entry) iter.next();
                String k = (String) e.getKey();
                if (k == null || k.length() == 0) continue;
                if (k.startsWith("date_") && k.length() > 20) continue;
                boolean isStat = k.equalsIgnoreCase("time") || k.equalsIgnoreCase("qq")
                    || k.startsWith("total") || k.startsWith("今日发送") || k.startsWith("今日接收");
                if ("推荐".equals(g) && !isStat) continue;
                out.add(new String[]{ k, varPoolAnyDesc(k), String.valueOf(e.getValue()), "消息统计" });
            }
        }
    }

    if (("推荐".equals(g) && hotplug) || all || "事件变量".equals(g)) {
        String[] names = null;
        try { names = SYNC_VARS; } catch (Throwable e) { traceLog("api3_log", "[collectVariablePool] SYNC_VARS 不可见: " + e); }
        if (names == null || names.length == 0) names = VAR_POOL_FALLBACK_EVENT;
        for (int i = 0; i < names.length; i++) {
            String k = names[i];
            if (k == null || k.length() == 0) continue;
            if ("推荐".equals(g) && (k.equals("this") || k.equals("scriptLoader"))) continue;
            Object v = null;
            try { v = this.interpreter.get(k); } catch (Throwable ignore) {}
            String d = varPoolDesc(k);
            out.add(new String[]{ k, d.length() > 0 ? d : "热插拔脚本可用变量", formatVarValue(v), "事件变量" });
        }
    }

    if (all || "全局变量".equals(g)) {
        try {
            String[] names = this.interpreter.getNameSpace().getVariableNames();
            for (int i = 0; i < names.length; i++) {
                String k = names[i];
                if (k == null || k.length() == 0) continue;
                if (k.startsWith("bsh") || k.equals("unhooks")) continue;
                Object v = null;
                try { v = this.interpreter.get(k); } catch (Throwable ignore) {}
                String d = varPoolDesc(k);
                out.add(new String[]{ k, d.length() > 0 ? d : "全局变量", formatVarValue(v), "全局变量" });
            }
        } catch (Throwable e) { traceLog("api3_log", "[collectVariablePool] 全局: " + e); }
    }

    try {
        Collections.sort(out, new Comparator() {
            public int compare(Object o1, Object o2) {
                String a = ((String[]) o1)[0];
                String b = ((String[]) o2)[0];
                int la = a.length();
                int lb = b.length();
                if (la != lb) return la - lb;
                return a.compareToIgnoreCase(b);
            }
        });
    } catch (Throwable e) { traceLog("api3_log", "[collectVariablePool] 排序: " + e); }

    String kw = (keyword == null) ? "" : keyword.trim().toLowerCase();
    if (kw.length() == 0) return out;
    List filtered = new ArrayList();
    for (int i = 0; i < out.size(); i++) {
        String[] it = (String[]) out.get(i);
        if (it[0].toLowerCase().contains(kw) || it[1].toLowerCase().contains(kw) || it[2].toLowerCase().contains(kw)) {
            filtered.add(it);
        }
    }
    traceLog("api3_log", "[collectVariablePool] group=" + g + " ctx=" + context + " 总=" + out.size() + " 命中=" + filtered.size());
    return filtered;
}

String 替换变量占位符(String template, Object scriptScope) {
    if (template == null || template.trim().isEmpty()) return "";
    String result = template;

    try {
        Matcher linkMatcher = SC.LINK_PATTERN.matcher(result);
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
                    Long cacheTs = (Long) SC.linkCacheTime.get(linkName);
                    if (cacheTs != null && System.currentTimeMillis() - cacheTs.longValue() < 60000L) {
                        linkValue = (String) SC.linkCache.get(linkName);
                    } else {
                        Future future = ThreadPool.submit(new Callable() {
                            public Object call() throws Exception { return get(linkName); }
                        });
                        linkValue = (String) future.get(500, TimeUnit.MILLISECONDS);
                        if (linkValue == null || linkValue.trim().isEmpty() || "null".equals(linkValue)) linkValue = null;
                        SC.linkCache.put(linkName, linkValue != null ? linkValue : "");
                        SC.linkCacheTime.put(linkName, Long.valueOf(System.currentTimeMillis()));
                    }
                } catch (Throwable e) {}
                if (linkValue == null || linkValue.isEmpty()) linkValue = null;
                linkSb.append(linkValue != null ? linkValue : "访问链接失败了哦～");
            }
            lastIdx = linkMatcher.end();
        }
        linkSb.append(result, lastIdx, result.length());
        result = linkSb.toString();
    } catch (Throwable e) {}

    try {
        Matcher varMatcher = SC.VAR_PATTERN.matcher(result);
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
    } catch (Throwable e) {}

    return result;
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
    if (提示词模板 == null || 提示词模板.trim().isEmpty()) 提示词模板 = "我是一个输入框提示～";

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
            } catch (Throwable e) {}
        }
    });
}

public void bindStatsViewCache(final View rootView) {
    if (rootView == null) return;
    SC.contentRoot = rootView;
    SC.pageVisible = true;
    SC.msgHandle.postDelayed(new Runnable() {
        public void run() {
            try {
                if (SC.statsTextViewCache == null) SC.statsTextViewCache = new ArrayList();
                SC.statsTextViewCache.clear();
                collectTextViewsWithTag(rootView, SC.statsTextViewCache);
                SC.todayCoreCardCache = (LinearLayout) rootView.findViewWithTag("todayCoreCard");
                if (SC.todayCoreCardCache != null) {
                    SC.sendTargetLabelCache = (TextView) SC.todayCoreCardCache.findViewWithTag("send_target_label");
                    SC.progressBarCache = SC.todayCoreCardCache.findViewWithTag("send_progress");
                    SC.achievementTextCache = (TextView) SC.todayCoreCardCache.findViewWithTag("achievement_text");
                }
                View wlCard = rootView.findViewWithTag("whitelistCard");
                SC.whitelistDetailCache = wlCard != null ? (TextView) wlCard.findViewWithTag("whitelistDetail") : null;
                SC.whitelistCurrentIndex = 0;
                if (wlCard != null) updateWhitelistDetail(wlCard, 0);
                triggerUIUpdate();
            } catch (Throwable e) {
                traceLog("api3_log", "[bindStatsViewCache] 异常: " + e);
            }
        }
    }, 150);
}

private void collectTextViewsWithTag(View root, List out) {
    if (root == null || out == null) return;
    if (root instanceof TextView) {
        Object tag = ((TextView) root).getTag();
        if (tag instanceof String) {
            String s = (String) tag;
            if ("totalReceive".equals(s) || "totalSend".equals(s) || "Receive".equals(s) || "Send".equals(s)
                || s.startsWith("Receive") || s.startsWith("Send")) {
                out.add(root);
            }
        }
    }
    if (root instanceof ViewGroup) {
        ViewGroup vg = (ViewGroup) root;
        for (int i = 0; i < vg.getChildCount(); i++) {
            collectTextViewsWithTag(vg.getChildAt(i), out);
        }
    }
}

public void unbindStatsViewCache() {
    SC.pageVisible = false;
    SC.contentRoot = null;
    try { if (SC.statsTextViewCache != null) SC.statsTextViewCache.clear(); } catch (Throwable ignore) {}
    SC.todayCoreCardCache = null;
    SC.progressBarCache = null;
    SC.sendTargetLabelCache = null;
    SC.achievementTextCache = null;
    SC.whitelistDetailCache = null;
    SC.whitelistCurrentIndex = 0;
}