/*
 * 特别关心 · 协议字段说明
 *
 * 一、设置开关（增删）
 *   serviceCmd = OidbSvc.0x5d6_7
 *   信封 OIDBSSOPkg: 1=command(0x5d6/1494), 2=serviceType(7), 3=result(0), 4=body
 *   body(ReqBody): 1=domain(0), 2=rpt_msg_update_buffer[], 3=seq(1)
 *   update_buffer(SnsUpateBuffer): 1=uint64_uin(对方QQ), 400=rpt_msg_sns_update_item[]
 *   sns_update_item(SnsUpdateItem): 1=uint32_update_sns_type, 2=bytes_value
 *     sns 类型: 13568=特别关心, 13572=空间关心, 13573=铃声
 *     bytes_value: "1"=开启, 空=关闭
 *
 * 二、名单与「关心我的」数量（只读）
 *   GET https://r.qzone.qq.com/cgi-bin/tfriend/specialcare_get.cgi
 *     ?uin=<自己QQ>&do=3&fupdate=1&rd=<随机数>&g_tk=<gtk>
 *   Cookie: uin=o<自己>; skey=<skey>; p_uin=o<自己>; p_skey=<qzone pskey>
 *   响应(data): items_special[]={uin,name} = 我关心的名单
 *               fans_count[0].fans_count = 关心我的数量
 */

class SpecialCareItem {
    String uin = "";
    String name = "";
    String remark = "";

    String display() {
        String d = (remark == null || remark.isEmpty()) ? name : remark + "(" + name + ")";
        return d == null || d.isEmpty() ? uin : d;
    }
}

JSONObject scLoadCache() {
    try {
        File f = new File(configPath + "special_care/list.json");
        if (!f.exists()) return null;
        byte[] data = new byte[(int) f.length()];
        java.io.FileInputStream fis = new java.io.FileInputStream(f);
        int read = 0;
        while (read < data.length) {
            int n = fis.read(data, read, data.length - read);
            if (n < 0) break;
            read += n;
        }
        fis.close();
        return new JSONObject(new String(data, "UTF-8"));
    } catch (Throwable e) { traceLog("special_care_log", "[cache] 读取失败 " + e); }
    return null;
}

void scSaveCache(List items, long time, String careMe) {
    try {
        File dir = new File(configPath + "special_care");
        if (!dir.exists()) dir.mkdirs();
        JSONObject root = new JSONObject();
        root.put("time", time);
        root.put("careme", careMe);
        JSONArray arr = new JSONArray();
        for (int i = 0; i < items.size(); i++) {
            SpecialCareItem it = (SpecialCareItem) items.get(i);
            JSONObject o = new JSONObject();
            o.put("uin", it.uin);
            o.put("name", it.name);
            o.put("remark", it.remark);
            arr.put(o);
        }
        root.put("items", arr);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(configPath + "special_care/list.json");
        fos.write(root.toString().getBytes("UTF-8"));
        fos.flush();
        fos.close();
    } catch (Throwable e) { traceLog("special_care_log", "[cache] 写入失败 " + e); }
}

class SpecialCarePage {
    final Activity activity;
    final boolean isDark;
    final List items = new ArrayList();
    long cacheTime = 0;
    String careMe = "";
    boolean refreshing = false;
    LinearLayout root;
    TextView summaryView;
    TextView careMeView;
    LinearLayout listHost;
    TextView refreshHint;
    Object self;

    SpecialCarePage(Activity activity, boolean isDark) {
        this.activity = activity;
        this.isDark = isDark;
        this.self = this;
    }

    void ui(final Runnable r) {
        if (activity == null || activity.isFinishing()) return;
        activity.runOnUiThread(r);
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(activity);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 16)));
        c.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(activity, 8);
        c.setLayoutParams(p);
        return c;
    }

    TextView label(String text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextSize(13);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        t.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 6));
        return t;
    }

    View build() {
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 18));

        LinearLayout top = card();
        summaryView = new TextView(activity);
        summaryView.setTextSize(20);
        summaryView.setTypeface(null, Typeface.BOLD);
        summaryView.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
        top.addView(summaryView);
        careMeView = new TextView(activity);
        careMeView.setTextSize(12);
        careMeView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        careMeView.setPadding(0, dp(activity, 4), 0, 0);
        top.addView(careMeView);
        refreshHint = new TextView(activity);
        refreshHint.setTextSize(11);
        refreshHint.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        refreshHint.setPadding(0, dp(activity, 4), 0, 0);
        top.addView(refreshHint);
        root.addView(top);

        LinearLayout tools = new LinearLayout(activity);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(activity, 6);
        tools.setLayoutParams(tp);
        TextView refreshBtn = createButton(activity, "刷新", pc("#FFFFFFFF"), pc(getSettingsThemeColor(activity, "primary")), 12f, 12, 16, 8, false);
        refreshBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { refresh(); } });
        tools.addView(refreshBtn);
        TextView addBtn = createButton(activity, "添加", pc(getSettingsThemeColor(activity, "on_surface")), pc(isDark ? "#22FFFFFF" : "#11000000"), 12f, 12, 16, 8, false);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, -2);
        ap.leftMargin = dp(activity, 8);
        addBtn.setLayoutParams(ap);
        addBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showAddDialog(); } });
        tools.addView(addBtn);
        root.addView(tools);

        LinearLayout listCard = card();
        listCard.addView(label("特别关心列表"));
        listHost = new LinearLayout(activity);
        listHost.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(listHost);
        root.addView(listCard);

        JSONObject cache = scLoadCache();
        if (cache != null) {
            cacheTime = cache.optLong("time", 0);
            careMe = cache.optString("careme", "");
            JSONArray arr = cache.optJSONArray("items");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    SpecialCareItem it = new SpecialCareItem();
                    it.uin = o.optString("uin", "");
                    it.name = o.optString("name", "");
                    it.remark = o.optString("remark", "");
                    items.add(it);
                }
            }
        }
        render();
        scStartRefresh(this);
        startPolling();
        return root;
    }

    void startPolling() {
        if (uiHandler != null) {
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (root == null || !root.isAttachedToWindow()) return;
                    render();
                    startPolling();
                }
            }, 3000);
        }
    }

    void render() {
        if (root == null) return;
        summaryView.setText("我关心的：" + items.size() + "人");
        careMeView.setText("关心我的：" + (careMe == null || careMe.isEmpty() ? "获取中…" : careMe));
        if (cacheTime > 0) {
            refreshHint.setText("缓存 " + new SimpleDateFormat("HH:mm:ss").format(new Date(cacheTime))
                    + (refreshing ? " · 后台更新中…" : ""));
        } else {
            refreshHint.setText(refreshing ? "首次加载中…" : "点击刷新");
        }
        listHost.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            SpecialCareItem it = (SpecialCareItem) items.get(i);
            final int idx = i;
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
            row.setBackground(roundRect(pc(isDark ? "#11FFFFFF" : "#08000000"), dp(activity, 10)));

            TextView num = new TextView(activity);
            num.setText(String.valueOf(i + 1));
            num.setTextSize(12);
            num.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            num.setMinWidth(dp(activity, 28));
            row.addView(num);

            TextView name = new TextView(activity);
            name.setText(it.display() + "  (" + it.uin + ")");
            name.setTextSize(13);
            name.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(name);

            TextView del = createButton(activity, "移除", pc("#FFFFFF"), pc("#FFE53935"), 11f, 10, 10, 6, false);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { removeAt(idx); }
            });
            row.addView(del);
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showInfoDialog(idx); }
            });

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = i == 0 ? 0 : dp(activity, 6);
            listHost.addView(row, rp);
        }
    }

    void showInfoDialog(final int idx) {
        if (idx < 0 || idx >= items.size()) return;
        final SpecialCareItem it = (SpecialCareItem) items.get(idx);
        try {
            LinearLayout box = new LinearLayout(activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 8));

            ImageView avatar = new ImageView(activity);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setBackground(roundRect(pc(isDark ? "#33FFFFFF" : "#11000000"), dp(activity, 14)));
            int avSize = dp(activity, 64);
            LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(avSize, avSize);
            avp.gravity = Gravity.CENTER_HORIZONTAL;
            avatar.setLayoutParams(avp);
            remoteImageLoad(qqAvatarUrl(it.uin, 100), avatar, avSize, "sc_" + it.uin);
            box.addView(avatar);

            TextView title = new TextView(activity);
            title.setText(it.display());
            title.setTextSize(17);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, dp(activity, 10), 0, 0);
            box.addView(title);

            TextView sub = new TextView(activity);
            String subText = "QQ：" + it.uin;
            if (it.remark != null && !it.remark.isEmpty()) subText += "\n备注：" + it.remark;
            if (it.name != null && !it.name.isEmpty() && !it.name.equals(it.uin)) subText += "\n昵称：" + it.name;
            sub.setText(subText);
            sub.setTextSize(12);
            sub.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            sub.setGravity(Gravity.CENTER);
            sub.setPadding(0, dp(activity, 6), 0, 0);
            box.addView(sub);

            LinearLayout btns = new LinearLayout(activity);
            btns.setOrientation(LinearLayout.HORIZONTAL);
            btns.setPadding(0, dp(activity, 14), 0, 0);
            TextView profileBtn = createButton(activity, "打开主页", pc("#FFFFFFFF"), pc(getSettingsThemeColor(activity, "primary")), 13f, 12, 12, 8, false);
            profileBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            profileBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openFriendProfile(activity, it.uin); }
            });
            btns.addView(profileBtn);
            TextView removeBtn = createButton(activity, "移除关心", pc("#FFFFFF"), pc("#FFE53935"), 13f, 12, 12, 8, false);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1f);
            rp.leftMargin = dp(activity, 8);
            removeBtn.setLayoutParams(rp);
            removeBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { removeAt(idx); }
            });
            btns.addView(removeBtn);
            box.addView(btns);

            AlertDialog.Builder b = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            b.setTitle("特别关心信息");
            b.setView(box);
            b.setNegativeButton("关闭", null);
            AlertDialog dlg = b.show();
            applyUiTheme(activity, dlg, 0);
        } catch (Throwable e) { traceLog("special_care_log", "[ui] 信息卡片失败 " + e); }
    }

    void refresh() { scStartRefresh(this); render(); }

    void removeAt(final int idx) {
        if (idx < 0 || idx >= items.size()) return;
        final SpecialCareItem it = (SpecialCareItem) items.get(idx);
        confirm("取消特别关心", "确定取消对 " + it.display() + " 的特别关心吗？", new Runnable() {
            public void run() {
                scSetSpecial(it.uin, false, new Runnable() {
                    public void run() {
                        ui(new Runnable() {
                            public void run() {
                                SpecialCareItem target = null;
                                for (int i = 0; i < items.size(); i++) {
                                    if (((SpecialCareItem) items.get(i)).uin.equals(it.uin)) { target = (SpecialCareItem) items.get(i); break; }
                                }
                                if (target != null) items.remove(target);
                                scSaveCache(items, cacheTime, careMe);
                                render();
                                qqToast(2, "已取消特别关心");
                                scStartRefresh(self);
                            }
                        });
                    }
                });
            }
        });
    }

    void showAddDialog() {
        showGroupSelector(activity, 1, null, new GroupSelectCallback() {
            public void onSelected(List selected) {
                try {
                    if (selected == null || selected.isEmpty()) return;
                    String uin = String.valueOf(selected.get(0));
                    if (uin == null || uin.isEmpty() || "null".equals(uin)) return;
                    final String target = uin;
                    scSetSpecial(target, true, new Runnable() {
                        public void run() {
                            ui(new Runnable() {
                                public void run() {
                                    try {
                                        boolean exists = false;
                                        for (int k = 0; k < items.size(); k++) {
                                            if (((SpecialCareItem) items.get(k)).uin.equals(target)) { exists = true; break; }
                                        }
                                        if (!exists) {
                                            SpecialCareItem it = new SpecialCareItem();
                                            it.uin = target;
                                            it.name = friendDisplayName(target);
                                            items.add(it);
                                        }
                                        scSaveCache(items, cacheTime, careMe);
                                        render();
                                        qqToast(2, "已设置为特别关心");
                                        scStartRefresh(self);
                                    } catch (Throwable e) {
                                        traceLog("special_care_log", "[ui] 添加回调异常 " + e);
                                        qqToast(1, "设置失败，请稍后再试");
                                    }
                                }
                            });
                        }
                    });
                } catch (Throwable e) {
                    traceLog("special_care_log", "[ui] 选择回调异常 " + e);
                    qqToast(1, "添加失败，请稍后再试");
                }
            }
        }, true);
    }

    String friendDisplayName(String uin) {
        try {
            List friends = (List) getAllFriend();
            if (friends != null) {
                for (int i = 0; i < friends.size(); i++) {
                    Object f = friends.get(i);
                    if (f == null) continue;
                    if (uin.equals(String.valueOf(f.uin))) {
                        String[] keys = {"name", "uinName", "nick", "nickName"};
                        for (int k = 0; k < keys.length; k++) {
                            try {
                                java.lang.reflect.Field field = f.getClass().getField(keys[k]);
                                Object v = field.get(f);
                                String name = v == null ? "" : String.valueOf(v);
                                if (!name.isEmpty() && !"null".equals(name) && !name.equals(uin)) return name;
                            } catch (Throwable ignore) {}
                        }
                        break;
                    }
                }
            }
        } catch (Throwable ignore) {}
        String remark = getUserRemark(uin);
        if (remark != null && !remark.isEmpty() && !"null".equals(remark)) return remark;
        return uin;
    }

    void confirm(String title, String msg, final Runnable ok) {
        AlertDialog.Builder b = new AlertDialog.Builder(activity,
                isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        b.setTitle(title);
        b.setMessage(msg);
        b.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) { ok.run(); }
        });
        b.setNegativeButton("取消", null);
        AlertDialog dlg = b.show();
        applyUiTheme(activity, dlg, 0);
    }
}

void scStartRefresh(final SpecialCarePage page) {
    if (page.refreshing) return;
    page.refreshing = true;
    ThreadPool.execute(new Runnable() {
        public void run() {
            scRefreshInternal(page);
            page.refreshing = false;
        }
    });
}

void scRefreshInternal(final SpecialCarePage page) {
    try {
        String gtk = getGTK("qzone.qq.com");
        String url = "https://r.qzone.qq.com/cgi-bin/tfriend/specialcare_get.cgi?uin=" + myUin
                + "&do=3&fupdate=1&rd=" + Math.random() + "&g_tk=" + gtk;
        String cookie = "uin=o" + myUin + ";skey=" + getSkey() + ";p_uin=o" + myUin
                + ";p_skey=" + getPskey("qzone.qq.com");
        String raw = httpGet(url, cookie);
        String jsonText = raw.trim();
        int lp = jsonText.indexOf("(");
        int rp = jsonText.lastIndexOf(")");
        if (lp >= 0 && rp > lp) jsonText = jsonText.substring(lp + 1, rp);
        JSONObject resp = new JSONObject(jsonText);
        JSONObject data = resp.optJSONObject("data");

        List found = new ArrayList();
        JSONArray items = data.optJSONArray("items_special");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject o = items.optJSONObject(i);
                if (o == null) continue;
                SpecialCareItem it = new SpecialCareItem();
                it.uin = String.valueOf(o.opt("uin"));
                it.name = o.optString("name", "");
                if (it.name == null || it.name.isEmpty() || "null".equals(it.name)) it.name = friendDisplayName(it.uin);
                found.add(it);
            }
        }

        String newCareMe = "";
        JSONArray fans = data.optJSONArray("fans_count");
        if (fans != null && fans.length() > 0) {
            JSONObject f0 = fans.optJSONObject(0);
            if (f0 != null) newCareMe = String.valueOf(f0.opt("fans_count"));
        }
        if (newCareMe.isEmpty()) newCareMe = "暂不可获取";

        page.items.clear();
        page.items.addAll(found);
        page.careMe = newCareMe;
        page.cacheTime = System.currentTimeMillis();
        scSaveCache(page.items, page.cacheTime, page.careMe);
    } catch (Throwable e) {
        traceLog("special_care_log", "[refresh] 失败 " + e);
    }
}

void scSetSpecial(final String uin, final boolean on, final Runnable done) {
    try {
        JSONObject item = new JSONObject();
        item.put("1", Long.parseLong(uin));
        JSONArray sns = new JSONArray();
        JSONObject one = new JSONObject();
        one.put("1", 13568);
        if (on) one.put("2", "1");
        sns.put(one);
        JSONObject two = new JSONObject();
        two.put("1", 13572);
        if (on) two.put("2", "1");
        sns.put(two);
        JSONObject three = new JSONObject();
        three.put("1", 13573);
        if (on) three.put("2", "1");
        sns.put(three);
        item.put("400", sns);
        JSONArray buffers = new JSONArray();
        buffers.put(item);
        JSONObject body = new JSONObject();
        body.put("1", 0);
        body.put("2", buffers);
        body.put("3", 1);
        oidbSend("special_care_log", "OidbSvc.0x5d6_7", 0x5d6, 7, body, new IReceiver() {
            public void onReceive(byte[] resp) {
                OidbResult result = oidbParse(resp);
                if (done != null) done.run();
            }
        });
    } catch (Throwable e) {
        traceLog("special_care_log", "[set] 异常 " + e);
        if (done != null) done.run();
    }
}

void openFriendProfile(Activity activity, String uin) {
    try {
        String url = "mqqapi://card/show_pslcard?uin=" + uin + "&source_type=1&src_type=web&version=1";
        openSettingsExternalBrowser(activity, url);
    } catch (Throwable e) {
        traceLog("special_care_log", "[ui] 跳转主页失败 " + e);
        qqToast(1, "打开主页失败，请稍后再试");
    }
}

View build特别关心Page(Object ctx) {
    Activity activity = ctx == null ? null : ctx.activity;
    if (activity == null) activity = safeActivity();
    if (activity == null) throw new IllegalStateException("特别关心页缺少 Activity");
    return new SpecialCarePage(activity, isThemeDark(activity)).build();
}
