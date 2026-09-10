//此空间api由冷雨开发  点赞/评论由ᗜ×ᗜ改进并适配新版 使用请留名
private final HashSet doneTasks = new HashSet();
private volatile boolean isServiceRunning = false;
private final HashSet blackList = new HashSet();

private void loadDoneTasks() {
    String savedStr = getString("qzone_cfg", "qzone_done_tasks_str", "");
    doneTasks.clear();
    if (!savedStr.equals("")) {
        String[] items = savedStr.split(",");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (!item.equals("")) doneTasks.add(item);
        }
        traceLog("api8_log", "加载已处理任务数量: " + doneTasks.size());
    }
}

private void saveDoneTasks() {
    if (doneTasks.size() > 100) doneTasks.clear();
    StringBuffer sb = new StringBuffer();
    java.util.Iterator it = doneTasks.iterator();
    while (it.hasNext()) {
        if (sb.length() > 0) sb.append(",");
        sb.append((String) it.next());
    }
    putString("qzone_cfg", "qzone_done_tasks_str", sb.toString());
}

private void loadBlackList() {
    String saved = getString("qzone_cfg", "blacklist", "");
    blackList.clear();
    if (!saved.isEmpty()) {
        String[] items = saved.split(",");
        for (String s : items) {
            String trim = s.trim();
            if (!trim.isEmpty()) blackList.add(trim);
        }
    }
}

private void saveBlackList() {
    StringBuffer sb = new StringBuffer();
    java.util.Iterator it = blackList.iterator();
    while (it.hasNext()) {
        if (sb.length() > 0) sb.append(",");
        sb.append((String) it.next());
    }
    putString("qzone_cfg", "blacklist", sb.toString());
}

private String jsonGet(String json, String key) {
    try {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\":\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    } catch (Throwable e) { return ""; }
}

private int jsonGetInt(String json, String key) {
    try {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\":(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    } catch (Throwable e) { return -1; }
}

private ArrayList parseActiveFeeds(String jsonResp) {
    ArrayList result = new ArrayList();
    try {
        JSONObject root = new JSONObject(jsonResp);
        if (root.optInt("code", -1) != 0) return result;
        JSONObject data = root.getJSONObject("data");
        JSONArray vFeeds = data.getJSONArray("vFeeds");
        for (int i = 0; i < vFeeds.length(); i++) {
            JSONObject feed = vFeeds.getJSONObject(i);
            JSONObject comm = feed.getJSONObject("comm");
            String orglikekey = comm.optString("orglikekey", "");
            String curlikekey = comm.optString("curlikekey", "");
            String ugckey = comm.optString("ugckey", "");
            JSONObject userinfo = feed.getJSONObject("userinfo");
            JSONObject user = userinfo.getJSONObject("user");
            String uin = user.optString("uin", "");
            if (!orglikekey.isEmpty() && !curlikekey.isEmpty() && !uin.isEmpty()) {
                String[] arr = new String[4];
                arr[0] = orglikekey;
                arr[1] = curlikekey;
                arr[2] = uin;
                arr[3] = ugckey.isEmpty() ? curlikekey : ugckey;
                result.add(arr);
            }
        }
    } catch (Throwable e) { traceLog("api8_log", "[parseActiveFeeds] 异常: " + e); }
    return result;
}


public void showQzoneConfig() {
    Activity act = getNowActivity();
    if (act == null) act = 最后Activity;
    if (act == null || act.isFinishing()) {
        Toast("无法打开设置：没有可用 Activity");
        return;
    }
    final Activity finalAct = act;
    finalAct.runOnUiThread(new Runnable() {
        public void run() {
            try {
                Dialog d = new Dialog(finalAct);
                d.requestWindowFeature(1);
                d.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                FrameLayout outer = new FrameLayout(finalAct);
                int margin = dp(finalAct, 24);
                outer.setPadding(margin, margin, margin, margin);
                ScrollView scroll = new ScrollView(finalAct);
                outer.addView(scroll);
                LinearLayout card = new LinearLayout(finalAct);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(roundRect(tc(finalAct, "surface"), dp(finalAct, 16)));
                card.setPadding(dp(finalAct, 20), dp(finalAct, 20), dp(finalAct, 20), dp(finalAct, 20));
                scroll.addView(card);

                TextView title = new TextView(finalAct);
                title.setText("空间操作配置");
                title.setTextSize(18);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(tc(finalAct, "on_surface"));
                title.setPadding(0, 0, 0, dp(finalAct, 16));
                card.addView(title);

                // 秒赞 / 秒评 开关（复用设置页开关组件，即时保存并启停）
                card.addView(buildQzoneSwitchRow(finalAct, "秒赞", "switch_like"));
                card.addView(buildQzoneSwitchRow(finalAct, "秒评", "switch_comment"));

                // 评论内容
                card.addView(makeSubTitleCompact(finalAct, "评论内容", tc(finalAct, "on_surface")));
                String initText = getString("qzone_cfg", "comment", "我来暖说说啦！");
                EditText commentInput = makeInput(finalAct, "输入评论（≤100字）", null);
                commentInput.setText(initText);
                commentInput.setMaxLines(4);
                commentInput.setGravity(Gravity.TOP | Gravity.START);
                card.addView(commentInput);

                // 间隔设置
                card.addView(makeSubTitleCompact(finalAct, "间隔设置", tc(finalAct, "on_surface")));
                LinearLayout intervalRow = new LinearLayout(finalAct);
                intervalRow.setOrientation(LinearLayout.HORIZONTAL);
                intervalRow.setGravity(Gravity.CENTER_VERTICAL);
                int itemSpacing = dp(finalAct, 12);

                LinearLayout fetchDelayLayout = new LinearLayout(finalAct);
                fetchDelayLayout.setOrientation(LinearLayout.VERTICAL);
                fetchDelayLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                TextView qzLbl1 = new TextView(finalAct);
                qzLbl1.setText("拉取列表后延迟（秒）");
                qzLbl1.setTextSize(12);
                qzLbl1.setTextColor(tc(finalAct, "on_surface"));
                qzLbl1.setPadding(0, 0, 0, dp(finalAct, 4));
                fetchDelayLayout.addView(qzLbl1);
                EditText fetchDelayInput = makeInput(finalAct, "3~15秒", null);
                fetchDelayInput.setTextSize(12);
                fetchDelayInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                fetchDelayInput.setGravity(Gravity.CENTER);
                fetchDelayInput.setLayoutParams(new LinearLayout.LayoutParams(dp(finalAct, 60), -2));
                fetchDelayInput.setText(String.valueOf(getInt("qzone_cfg", "fetch_delay_ms", 5000) / 1000));
                fetchDelayLayout.addView(fetchDelayInput);
                intervalRow.addView(fetchDelayLayout);

                Space space1 = new Space(finalAct);
                space1.setLayoutParams(new LinearLayout.LayoutParams(itemSpacing, -2));
                intervalRow.addView(space1);

                LinearLayout feedIntervalLayout = new LinearLayout(finalAct);
                feedIntervalLayout.setOrientation(LinearLayout.VERTICAL);
                feedIntervalLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                TextView qzLbl2 = new TextView(finalAct);
                qzLbl2.setText("每条说说间隔（秒）");
                qzLbl2.setTextSize(12);
                qzLbl2.setTextColor(tc(finalAct, "on_surface"));
                qzLbl2.setPadding(0, 0, 0, dp(finalAct, 4));
                feedIntervalLayout.addView(qzLbl2);
                EditText feedIntervalInput = makeInput(finalAct, "0.5~5秒", null);
                feedIntervalInput.setTextSize(12);
                feedIntervalInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                feedIntervalInput.setGravity(Gravity.CENTER);
                feedIntervalInput.setLayoutParams(new LinearLayout.LayoutParams(dp(finalAct, 60), -2));
                feedIntervalInput.setText(String.valueOf(getInt("qzone_cfg", "feed_interval_ms", 1000) / 1000));
                feedIntervalLayout.addView(feedIntervalInput);
                intervalRow.addView(feedIntervalLayout);
                card.addView(intervalRow);

                // 黑名单设置
                card.addView(makeSubTitleCompact(finalAct, "黑名单设置", tc(finalAct, "on_surface")));
                LinearLayout blackRow = new LinearLayout(finalAct);
                blackRow.setOrientation(LinearLayout.HORIZONTAL);
                blackRow.setGravity(Gravity.CENTER_VERTICAL);
                blackRow.setPadding(0, dp(finalAct, 8), 0, dp(finalAct, 12));

                TextView blackBtn = createButton(finalAct, "设置黑名单", tc(finalAct, "primary"), Color.TRANSPARENT, 14f, 24, 0, 0, false, 0, 0, null);
                blackRow.addView(blackBtn, new LinearLayout.LayoutParams(-2, -2));

                Space spacer = new Space(finalAct);
                blackRow.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1.0f));

                final TextView blackLabel = new TextView(finalAct);
                blackLabel.setText("已屏蔽 " + blackList.size() + " 人");
                blackLabel.setTextSize(16);
                blackLabel.setTextColor(tc(finalAct, "on_surface"));
                blackRow.addView(blackLabel, new LinearLayout.LayoutParams(-2, -2));
                card.addView(blackRow);

                blackBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        final ArrayList initSel = new ArrayList(blackList);
                        showGroupSelector(finalAct, 1, initSel, new GroupSelectCallback() {
                            public void onSelected(ArrayList selected) {
                                blackList.clear();
                                if (selected != null) blackList.addAll(selected);
                                saveBlackList();
                                blackLabel.setText("已屏蔽 " + blackList.size() + " 人");
                            }
                        });
                    }
                });

                // 底部按钮
                LinearLayout btnRow = new LinearLayout(finalAct);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END);
                btnRow.setPadding(0, dp(finalAct, 24), 0, 0);

                TextView cancel = createButton(finalAct, "取消", tc(finalAct, "on_surface_variant"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
                LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(-2, dp(finalAct, 48));
                cancelLp.rightMargin = dp(finalAct, 12);
                btnRow.addView(cancel, cancelLp);

                TextView save = createButton(finalAct, "保存", tc(finalAct, "on_surface_variant"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
                btnRow.addView(save, new LinearLayout.LayoutParams(-2, dp(finalAct, 48)));
                card.addView(btnRow);

                save.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String text = commentInput.getText().toString().trim();
                        if (text.length() > 100) text = text.substring(0, 100);
                        putString("qzone_cfg", "comment", text);

                        try {
                            int sec = Integer.parseInt(fetchDelayInput.getText().toString().trim());
                            putInt("qzone_cfg", "fetch_delay_ms", sec*1000);
                        } catch (Throwable e) { putInt("qzone_cfg", "fetch_delay_ms", 5000); }

                        try {
                            int fsec = Integer.parseInt(feedIntervalInput.getText().toString().trim());
                            putInt("qzone_cfg", "feed_interval_ms", fsec*1000);
                        } catch (Throwable e) { putInt("qzone_cfg", "feed_interval_ms", 1000); }

                        Toast("已保存配置");
                        vibrate(finalAct, 40);
                        d.dismiss();
                        checkAndStartOrStopThread();
                    }
                });

                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });

                d.setContentView(outer);
                d.getWindow().setLayout(Math.min(dp(finalAct, 400), finalAct.getResources().getDisplayMetrics().widthPixels - dp(finalAct, 32)), -2);
                d.show();
                applyUiTheme(finalAct, d, 1);
            } catch (Throwable e) {
                Toast("弹窗创建失败: " + e.getMessage());
            }
        }
    });
}

View buildQzoneSwitchRow(Activity act, String labelText, final String keyName) {
    LinearLayout row = new LinearLayout(act);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(act, 12), 0, dp(act, 12));

    TextView label = new TextView(act);
    label.setText(labelText);
    label.setTextSize(16);
    label.setTextColor(tc(act, "on_surface"));
    row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));

    boolean initVal = getBoolean("qzone_cfg", keyName, false);
    View sw = createSettingsSwitchView(act, initVal, "qzone_cfg", keyName, labelText, new Runnable() {
        public void run() {
            checkAndStartOrStopThread();
        }
    });
    row.addView(sw);
    return row;
}

private void checkAndStartOrStopThread() {
    loadDoneTasks();
    loadBlackList();

    boolean likeOn = getBoolean("qzone_cfg", "switch_like", false);
    boolean commentOn = getBoolean("qzone_cfg", "switch_comment", false);

    if (likeOn || commentOn) {
        if (!isServiceRunning) startQzoneAutoThread();
    } else {
        stopQzoneAutoThread();
    }
}

private void startQzoneAutoThread() {
    if (isServiceRunning) return;
    isServiceRunning = true;
    Toast("空间操作已启动");
    qzoneFetch();
}

private void scheduleQzone(long delayMs, final Runnable task) {
    if (!isServiceRunning) return;
    uiHandler.postDelayed(new Runnable() {
        public void run() {
            if (isServiceRunning) task.run();
        }
    }, delayMs);
}

private void qzoneFetch() {
    if (!isServiceRunning) return;
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String pskey = getPskey("qzone.qq.com");
                String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
                String gtk = getGTK("qzone.qq.com");

                String url = "https://h5.qzone.qq.com/webapp/json/mqzone_feeds/getActiveFeeds?g_tk=" + gtk
                        + "&res_type=0&refresh_type=1&format=json";

                String resp = qzoneGet(url, cookie);

                if (jsonGetInt(resp, "code") != 0) {
                    scheduleQzone(20000, new Runnable() { public void run() { qzoneFetch(); } });
                    return;
                }

                final ArrayList feeds = parseActiveFeeds(resp);
                long fetchDelay = getInt("qzone_cfg", "fetch_delay_ms", 5000);
                scheduleQzone(fetchDelay, new Runnable() { public void run() { qzoneProcess(feeds, 0); } });
            } catch (Throwable e) {
                if (e instanceof InterruptedException) { isServiceRunning = false; return; }
                scheduleQzone(30000, new Runnable() { public void run() { qzoneFetch(); } });
            }
        }
    });
}

private void qzoneProcess(final ArrayList feeds, final int index) {
    if (!isServiceRunning) return;
    if (index >= feeds.size()) {
        scheduleQzone(10000 + (long)(Math.random() * 5000), new Runnable() { public void run() { qzoneFetch(); } });
        return;
    }
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String[] item = (String[]) feeds.get(index);
                String orgKey = item[0];
                String curKey = item[1];
                String userUin = item[2];
                String ugcKey = item[3];

                if (!userUin.equals(myUin) && !blackList.contains(userUin) && !doneTasks.contains(curKey)) {
                    boolean likeEnabled = getBoolean("qzone_cfg", "switch_like", false);
                    boolean commentEnabled = getBoolean("qzone_cfg", "switch_comment", false);
                    boolean didSomething = false;

                    if (likeEnabled) {
                        sendQzoneZan(orgKey, curKey);
                        didSomething = true;
                    }
                    if (commentEnabled) {
                        sendQzoneComment(orgKey, curKey, userUin, ugcKey);
                        didSomething = true;
                    }

                    if (didSomething) {
                        doneTasks.add(curKey);
                        saveDoneTasks();
                    }
                }

                int feedIntervalMs = getInt("qzone_cfg", "feed_interval_ms", 1000);
                long sleepMs = (long) (feedIntervalMs * (0.8f + Math.random() * 0.4f));
                scheduleQzone(sleepMs, new Runnable() { public void run() { qzoneProcess(feeds, index + 1); } });
            } catch (Throwable e) {
                if (e instanceof InterruptedException) { isServiceRunning = false; return; }
                scheduleQzone(30000, new Runnable() { public void run() { qzoneFetch(); } });
            }
        }
    });
}

private void stopQzoneAutoThread() {
    if (!isServiceRunning) return;
    isServiceRunning = false;
    Toast("空间操作已停止");
}

private void sendQzoneZan(String orglikekey, String curlikekey) {
    if (!getBoolean("qzone_cfg", "switch_like", false)) return;

    String pskey = getPskey("qzone.qq.com");
    String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
    String gtk = getGTK("qzone.qq.com");

    String url = "https://h5.qzone.qq.com/proxy/domain/w.qzone.qq.com/cgi-bin/likes/internal_dolike_app?g_tk=" + gtk;
    String data = "opuin=" + myUin + "&unikey=" + orglikekey + "&curkey=" + curlikekey + "&appid=311&opr_type=like&format=purejson";

    try {
        String resp = httppost1(url, cookie, data);
        if (jsonGetInt(resp, "ret") == 0) {
            traceLog("api8_log", "点赞成功: " + curlikekey);
        } else {
            traceLog("api8_log", "点赞失败: " + curlikekey);
        }
    } catch (Throwable e) { traceLog("api8_log", "[sendQzoneZan] 异常: " + e); }
}

private void sendQzoneComment(String orglikekey, String curlikekey, String userUin, String ugckey) {
    if (!getBoolean("qzone_cfg", "switch_comment", false)) return;

    String content = getString("qzone_cfg", "comment", "我来暖说说啦！");
    if (content.length() > 100) content = content.substring(0, 100);

    String srcId = curlikekey;
    int lastSlash = srcId.lastIndexOf('/');
    if (lastSlash != -1 && lastSlash < srcId.length() - 1) {
        srcId = srcId.substring(lastSlash + 1);
    }

    String pskey = getPskey("qzone.qq.com");
    String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
    String gtk = getGTK("qzone.qq.com");

    String url = "https://h5.qzone.qq.com/webapp/json/qzoneOperation/addComment?g_tk=" + gtk;

    String body = "{\"appid\":311,\"uin\":" + myUin
            + ",\"ownuin\":\"" + userUin
            + "\",\"srcId\":\"" + srcId
            + "\",\"content\":\"" + content.replace("\"", "\\\"")
            + "\",\"isPrivateComment\":0,\"busi_param\":{},\"bypass_param\":{}}";

    traceLog("api8_log", "准备评论 → 用户:" + userUin + " srcId:" + srcId + " 内容:" + content);

    try {
        String resp = httppost1(url, cookie, body);
        int ret = jsonGetInt(resp, "ret");
        if (ret == 0) {
            traceLog("api8_log", "评论成功: " + userUin);
        } else {
            traceLog("api8_log", "评论失败: " + userUin + " ret=" + ret + " resp=" + resp);
        }
    } catch (Throwable e) {
        traceLog("api8_log", "评论异常: " + userUin + " " + e.getMessage());
    }
}
checkAndStartOrStopThread();