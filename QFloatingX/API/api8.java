//此空间api由冷雨开发  点赞/评论由ᗜ×ᗜ改进并适配新版 使用请留名
private final java.util.concurrent.CopyOnWriteArraySet doneTasks = new java.util.concurrent.CopyOnWriteArraySet();
private volatile boolean isServiceRunning = false;
private final java.util.concurrent.CopyOnWriteArraySet blackList = new java.util.concurrent.CopyOnWriteArraySet();

private void loadDoneTasks() {
    String savedStr = getString("qzone_cfg", "qzone_done_tasks_str", "");
    doneTasks.clear();
    if (!savedStr.equals("")) {
        String[] items = savedStr.split(",");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (!item.equals("")) doneTasks.add(item);
        }
        traceLog("api8_log", "[loadDoneTasks] 加载已处理任务数量: " + doneTasks.size());
    }
}

private void saveDoneTasks() {
    if (doneTasks.size() > 100) {
        java.util.List oldest = new ArrayList();
        java.util.Iterator trimIt = doneTasks.iterator();
        int trimCount = 0;
        while (trimIt.hasNext() && trimCount < 20) {
            oldest.add(trimIt.next());
            trimCount++;
        }
        doneTasks.removeAll(oldest);
    }
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
            traceLog("api8_log", "[sendQzoneZan] 点赞成功: " + curlikekey);
        } else {
            traceLog("api8_log", "[sendQzoneZan] 点赞失败: " + curlikekey);
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

    traceLog("api8_log", "[sendQzoneComment] 准备评论 → 用户:" + userUin + " srcId:" + srcId + " 内容:" + content);

    try {
        String resp = httppost1(url, cookie, body);
        int ret = jsonGetInt(resp, "ret");
        if (ret == 0) {
            traceLog("api8_log", "[sendQzoneComment] 评论成功: " + userUin);
        } else {
            traceLog("api8_log", "[sendQzoneComment] 评论失败: " + userUin + " ret=" + ret + " resp=" + resp);
        }
    } catch (Throwable e) {
        traceLog("api8_log", "[sendQzoneComment] 评论异常: " + userUin + " " + e.getMessage());
    }
}
checkAndStartOrStopThread();
