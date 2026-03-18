//此空间api由冷雨开发  点赞/评论由ᗜ×ᗜ改进并适配新版 使用请留名
private final HashSet doneTasks = new HashSet();
private volatile boolean isServiceRunning = false;
private Thread qzoneThread = null;
private static final String DONE_TASKS_STR_KEY = "qzone_done_tasks_str";
private final HashSet blackList = new HashSet();

private void loadDoneTasks() {
    String savedStr = getString("qzone_cfg", DONE_TASKS_STR_KEY, "");
    doneTasks.clear();
    if (!savedStr.equals("")) {
        String[] items = savedStr.split(",");
        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();
            if (!item.equals("")) doneTasks.add(item);
        }
        traceLog("qzone_log", "加载已处理任务数量: " + doneTasks.size());
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
    putString("qzone_cfg", DONE_TASKS_STR_KEY, sb.toString());
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
    } catch (Throwable e) {}
    return result;
}

private void delay(long millis) {
    if (millis <= 0 || !isServiceRunning) return;
    Object lock = new Object();
    synchronized (lock) {
        long start = System.currentTimeMillis();
        long remaining = millis;
        while (remaining > 0 && isServiceRunning) {
            try {
                lock.wait(Math.min(remaining, 200));
                remaining = millis - (System.currentTimeMillis() - start);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
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
                card.setBackground(roundRect(Color.WHITE, dp(finalAct, 16)));
                card.setPadding(dp(finalAct, 20), dp(finalAct, 20), dp(finalAct, 20), dp(finalAct, 20));
                scroll.addView(card);

                TextView title = new TextView(finalAct);
                title.setText("空间操作配置");
                title.setTextSize(18);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(Color.parseColor("#212121"));
                title.setPadding(0, 0, 0, dp(finalAct, 16));
                card.addView(title);

                final boolean initLike = getBoolean("qzone_cfg", "switch_like", false);
                final boolean initComment = getBoolean("qzone_cfg", "switch_comment", false);

                LinearLayout likeRow = new LinearLayout(finalAct);
                likeRow.setOrientation(LinearLayout.HORIZONTAL);
                likeRow.setGravity(Gravity.CENTER_VERTICAL);
                likeRow.setPadding(0, dp(finalAct, 12), 0, dp(finalAct, 12));
                TextView likeLabel = new TextView(finalAct);
                likeLabel.setText("秒赞");
                likeLabel.setTextSize(16);
                likeLabel.setTextColor(Color.parseColor("#212121"));
                likeRow.addView(likeLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));
                final boolean[] likeState = new boolean[]{initLike};
                final View likeSwitch = createSwitchView(finalAct, likeState[0]);
                setClickAnimation(likeSwitch);
                likeSwitch.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        likeState[0] = !likeState[0];
                        updateSwitchUI(likeSwitch, likeState[0]);
                    }
                });
                likeRow.addView(likeSwitch);
                card.addView(likeRow);

                LinearLayout commentRow = new LinearLayout(finalAct);
                commentRow.setOrientation(LinearLayout.HORIZONTAL);
                commentRow.setGravity(Gravity.CENTER_VERTICAL);
                commentRow.setPadding(0, dp(finalAct, 12), 0, dp(finalAct, 12));
                TextView commentLabel = new TextView(finalAct);
                commentLabel.setText("秒评");
                commentLabel.setTextSize(16);
                commentLabel.setTextColor(Color.parseColor("#212121"));
                commentRow.addView(commentLabel, new LinearLayout.LayoutParams(0, -2, 1.0f));
                final boolean[] commentState = new boolean[]{initComment};
                final View commentSwitch = createSwitchView(finalAct, commentState[0]);
                setClickAnimation(commentSwitch);
                commentSwitch.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        commentState[0] = !commentState[0];
                        updateSwitchUI(commentSwitch, commentState[0]);
                    }
                });
                commentRow.addView(commentSwitch);
                card.addView(commentRow);

                TextView commentTitle = new TextView(finalAct);
                commentTitle.setText("评论内容");
                commentTitle.setTextSize(14);
                commentTitle.setTextColor(Color.parseColor("#757575"));
                commentTitle.setPadding(0, dp(finalAct, 16), 0, dp(finalAct, 8));
                card.addView(commentTitle);

                String initText = getString("qzone_cfg", "comment", "我来暖说说啦！");
                final EditText commentInput = makeInput(finalAct, "输入评论（≤100字）", Color.parseColor("#F5F5F5"));
                commentInput.setText(initText);
                commentInput.setMaxLines(4);
                commentInput.setGravity(Gravity.TOP | Gravity.START);
                card.addView(commentInput);

                TextView delayTitle = new TextView(finalAct);
                delayTitle.setText("间隔设置");
                delayTitle.setTextSize(14);
                delayTitle.setTextColor(Color.parseColor("#757575"));
                delayTitle.setPadding(0, dp(finalAct, 16), 0, dp(finalAct, 8));
                card.addView(delayTitle);

                LinearLayout intervalRow = new LinearLayout(finalAct);
                intervalRow.setOrientation(LinearLayout.HORIZONTAL);
                intervalRow.setGravity(Gravity.CENTER_VERTICAL);
                int itemSpacing = dp(finalAct, 12);

                LinearLayout fetchDelayLayout = new LinearLayout(finalAct);
                fetchDelayLayout.setOrientation(LinearLayout.VERTICAL);
                fetchDelayLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                TextView fetchDelayLabel = new TextView(finalAct);
                fetchDelayLabel.setText("拉取列表后延迟（秒）");
                fetchDelayLabel.setTextSize(12);
                fetchDelayLabel.setTextColor(Color.parseColor("#757575"));
                fetchDelayLabel.setPadding(0, 0, 0, dp(finalAct, 4));
                fetchDelayLayout.addView(fetchDelayLabel);
                final EditText fetchDelayInput = makeSmallInput(finalAct, "3~15秒", Color.parseColor("#F5F5F5"));
                fetchDelayInput.setText(String.valueOf(getInt("qzone_cfg", "fetch_delay_ms", 5000) / 1000));
                fetchDelayLayout.addView(fetchDelayInput);
                intervalRow.addView(fetchDelayLayout);

                Space space1 = new Space(finalAct);
                space1.setLayoutParams(new LinearLayout.LayoutParams(itemSpacing, -2));
                intervalRow.addView(space1);

                LinearLayout feedIntervalLayout = new LinearLayout(finalAct);
                feedIntervalLayout.setOrientation(LinearLayout.VERTICAL);
                feedIntervalLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                TextView feedIntervalLabel = new TextView(finalAct);
                feedIntervalLabel.setText("每条说说间隔（秒）");
                feedIntervalLabel.setTextSize(12);
                feedIntervalLabel.setTextColor(Color.parseColor("#757575"));
                feedIntervalLabel.setPadding(0, 0, 0, dp(finalAct, 4));
                feedIntervalLayout.addView(feedIntervalLabel);
                final EditText feedIntervalInput = makeSmallInput(finalAct, "0.5~5秒", Color.parseColor("#F5F5F5"));
                feedIntervalInput.setText(String.valueOf(getInt("qzone_cfg", "feed_interval_ms", 1000) / 1000));
                feedIntervalLayout.addView(feedIntervalInput);
                intervalRow.addView(feedIntervalLayout);
                card.addView(intervalRow);

                TextView blackTitle = new TextView(finalAct);
                blackTitle.setText("黑名单设置");
                blackTitle.setTextSize(14);
                blackTitle.setTextColor(Color.parseColor("#757575"));
                blackTitle.setPadding(0, dp(finalAct, 16), 0, dp(finalAct, 8));
                card.addView(blackTitle);

                LinearLayout blackRow = new LinearLayout(finalAct);
                blackRow.setOrientation(LinearLayout.HORIZONTAL);
                blackRow.setGravity(Gravity.CENTER_VERTICAL);
                blackRow.setPadding(0, dp(finalAct, 8), 0, dp(finalAct, 12));

                TextView blackBtn = makeBtn(finalAct, "设置黑名单", Color.parseColor("#3B71FE"), Color.TRANSPARENT);
                blackBtn.setBackground(roundRect(Color.TRANSPARENT, dp(finalAct, 24)));
                blackBtn.setPadding(0, 0, 0, 0);
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                blackRow.addView(blackBtn, btnLp);

                // 弹性空间，将文本推到右侧
                Space spacer = new Space(finalAct);
                LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                );
                blackRow.addView(spacer, spacerLp);

                final TextView blackLabel = new TextView(finalAct);
                blackLabel.setText("已屏蔽 " + blackList.size() + " 人");
                blackLabel.setTextSize(16);
                blackLabel.setTextColor(Color.parseColor("#212121"));
                LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                blackRow.addView(blackLabel, labelLp);

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

                LinearLayout btnRow = new LinearLayout(finalAct);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END);
                btnRow.setPadding(0, dp(finalAct, 24), 0, 0);

                TextView cancel = makeBtn(finalAct, "取消", Color.parseColor("#3B71FE"), Color.TRANSPARENT);
                GradientDrawable cancelBg = new GradientDrawable();
                cancelBg.setShape(GradientDrawable.RECTANGLE);
                cancelBg.setCornerRadius(dp(finalAct, 24));
                cancelBg.setStroke(dp(finalAct, 1), Color.parseColor("#757575"));
                cancelBg.setColor(Color.TRANSPARENT);
                cancel.setBackground(cancelBg);
                cancel.setPadding(dp(finalAct, 24), 0, dp(finalAct, 24), 0);
                cancel.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(finalAct, 48));
                lp.rightMargin = dp(finalAct, 12);
                btnRow.addView(cancel, lp);

                TextView save = makeBtn(finalAct, "保存", Color.WHITE, Color.parseColor("#3B71FE"));
                save.setBackground(roundRect(Color.parseColor("#3B71FE"), dp(finalAct, 24)));
                save.setPadding(dp(finalAct, 24), 0, dp(finalAct, 24), 0);
                save.setGravity(Gravity.CENTER);
                btnRow.addView(save, new LinearLayout.LayoutParams(-2, dp(finalAct, 48)));

                card.addView(btnRow);

                save.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        putBoolean("qzone_cfg", "switch_like", likeState[0]);
                        putBoolean("qzone_cfg", "switch_comment", commentState[0]);

                        String text = commentInput.getText().toString().trim();
                        if (text.length() > 100) text = text.substring(0, 100);
                        putString("qzone_cfg", "comment", text);

                        try {
                            int sec = Integer.parseInt(fetchDelayInput.getText().toString().trim());
                            putInt("qzone_cfg", "fetch_delay_ms", Math.max(1, Math.min(60, sec)) * 1000);
                        } catch (Throwable e) { putInt("qzone_cfg", "fetch_delay_ms", 5000); }

                        try {
                            float fsec = Float.parseFloat(feedIntervalInput.getText().toString().trim());
                            putInt("qzone_cfg", "feed_interval_ms", (int)(Math.max(0.1f, Math.min(10f, fsec)) * 1000));
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
                d.getWindow().setLayout((int)(finalAct.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
                d.show();
            } catch (Throwable e) {
                Toast("弹窗创建失败: " + e.getMessage());
            }
        }
    });
}

View createSwitchView(Context ctx, boolean initVal) {
    FrameLayout swContainer = new FrameLayout(ctx);
    int swW = dp(ctx, 52);
    int swH = dp(ctx, 32);
    FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(swW, swH);
    swContainer.setLayoutParams(containerLp);
    swContainer.setClickable(true);
    swContainer.setFocusable(true);

    GradientDrawable mask = new GradientDrawable();
    mask.setCornerRadius(dp(ctx, 16));
    mask.setColor(Color.WHITE);
    swContainer.setBackground(new RippleDrawable(
        ColorStateList.valueOf(Color.parseColor("#1A000000")),
        null,
        mask
    ));

    View track = new View(ctx);
    FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(-1, -1);
    track.setLayoutParams(trackLp);
    GradientDrawable trackBg = new GradientDrawable();
    trackBg.setCornerRadius(dp(ctx, 16));
    track.setTag(trackBg);
    swContainer.addView(track);

    View thumb = new View(ctx);
    int thumbSize = dp(ctx, 24);
    int margin = dp(ctx, 4);
    FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(thumbSize, thumbSize);
    thumbLp.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
    thumbLp.setMargins(margin, 0, margin, 0);
    thumb.setLayoutParams(thumbLp);
    GradientDrawable thumbBg = new GradientDrawable();
    thumbBg.setColor(Color.WHITE);
    thumbBg.setCornerRadius(dp(ctx, 12));
    thumb.setBackground(thumbBg);
    thumb.setElevation(dp(ctx, 2));
    swContainer.addView(thumb);
    swContainer.setTag(thumb);

    updateSwitchUI(swContainer, initVal);
    return swContainer;
}

void updateSwitchUI(View switchView, boolean isChecked) {
    if (switchView == null || switchView.getChildCount() < 2) return;
    View track = switchView.getChildAt(0);
    View thumb = (View) switchView.getTag();
    GradientDrawable trackBg = (GradientDrawable) track.getTag();
    if (trackBg == null || thumb == null) return;

    int trackColor = isChecked ? Color.parseColor("#3B71FE") : Color.parseColor("#E4E4E4");
    trackBg.setColor(trackColor);
    track.setBackground(trackBg);

    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
    lp.gravity = Gravity.CENTER_VERTICAL | (isChecked ? Gravity.RIGHT : Gravity.LEFT);
    thumb.setLayoutParams(lp);
}

void setClickAnimation(View view) {
    if (view == null) return;
    view.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).setInterpolator(new OvershootInterpolator(2.0f)).start();
                    break;
            }
            return false;
        }
    });
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

    qzoneThread = new Thread(new Runnable() {
        public void run() {
            while (isServiceRunning) {
                try {
                    String pskey = getPskey("qzone.qq.com");
                    String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin + ";p_skey=" + pskey;
                    String gtk = getGTK("qzone.qq.com");

                    String url = "https://h5.qzone.qq.com/webapp/json/mqzone_feeds/getActiveFeeds?g_tk=" + gtk
                            + "&res_type=0&refresh_type=1&format=json";

                    String resp = qzoneGet(url, cookie);

                    if (jsonGetInt(resp, "code") != 0) {
                        delay(20000);
                        continue;
                    }

                    ArrayList feeds = parseActiveFeeds(resp);
                    delay(getInt("qzone_cfg", "fetch_delay_ms", 5000));

                    boolean likeEnabled = getBoolean("qzone_cfg", "switch_like", false);
                    boolean commentEnabled = getBoolean("qzone_cfg", "switch_comment", false);

                    for (int i = 0; i < feeds.size(); i++) {
                        if (!isServiceRunning) break;

                        String[] item = (String[]) feeds.get(i);
                        String orgKey = item[0];
                        String curKey = item[1];
                        String userUin = item[2];
                        String ugcKey = item[3];

                        if (userUin.equals(myUin)) continue;
                        if (blackList.contains(userUin)) continue;
                        if (doneTasks.contains(curKey)) continue;

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

                        int feedIntervalMs = getInt("qzone_cfg", "feed_interval_ms", 1000);
                        long sleepMs = (long) (feedIntervalMs * (0.8f + Math.random() * 0.4f));
                        delay(sleepMs);
                    }

                    delay(10000 + (long)(Math.random() * 5000));

                } catch (Throwable e) {
                    if (e instanceof InterruptedException) break;
                    delay(30000);
                }
            }
            isServiceRunning = false;
        }
    });

    ThreadPool.execute(qzoneThread);
    Toast("空间操作已启动");
}

private void stopQzoneAutoThread() {
    if (!isServiceRunning) return;
    isServiceRunning = false;
    if (qzoneThread != null && qzoneThread.isAlive()) qzoneThread.interrupt();
    qzoneThread = null;
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
            traceLog("qzone_log", "点赞成功: " + curlikekey);
        } else {
            traceLog("qzone_log", "点赞失败: " + curlikekey);
        }
    } catch (Throwable e) {}
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

    traceLog("qzone_log", "准备评论 → 用户:" + userUin + " srcId:" + srcId + " 内容:" + content);

    try {
        String resp = httppost1(url, cookie, body);
        int ret = jsonGetInt(resp, "ret");
        if (ret == 0) {
            traceLog("qzone_log", "评论成功: " + userUin);
        } else {
            traceLog("qzone_log", "评论失败: " + userUin + " ret=" + ret + " resp=" + resp);
        }
    } catch (Throwable e) {
        traceLog("qzone_log", "评论异常: " + userUin + " " + e.getMessage());
    }
}
checkAndStartOrStopThread();