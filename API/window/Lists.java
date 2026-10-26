String groupDisplayName(String qun) {
    String gName = getGroupName(qun);
    String gRemark = getGroupRemark(qun);
    if (gRemark == null || gRemark.isEmpty()) return gName;
    if (gName == null || gName.isEmpty() || gRemark.equals(gName)) return gRemark;
    return gRemark + "(" + gName + ")";
}
View buildSimpleListItem(Activity activity,
    Object[] itemData,
    int listMode,
    String groupUin,
    boolean isDark) {
    String uin = String.valueOf(itemData[0]);
    String name = String.valueOf(itemData[1]);
    LinearLayout item = new LinearLayout(activity);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setMinimumHeight(dp(activity, 58));
    item.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 10), dp(activity, 8));
    item.setClickable(true);
    item.setBackground(makeAdaptiveRippleBg(activity, Color.TRANSPARENT, 12));
    item.setClipToOutline(true);

    LinearLayout textBox = new LinearLayout(activity);
    textBox.setOrientation(LinearLayout.VERTICAL);
    textBox.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

    TextView titleView = new TextView(activity);
    titleView.setText(name);
    titleView.setTextSize(16);
    titleView.setMaxLines(1);
    titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    titleView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    textBox.addView(titleView);

    TextView uinView = new TextView(activity);
    uinView.setText(uin);
    uinView.setTextSize(11);
    uinView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    uinView.setSingleLine(true);
    uinView.setIncludeFontPadding(false);
    LinearLayout.LayoutParams uinParams = new LinearLayout.LayoutParams(-1, -2);
    uinParams.topMargin = dp(activity, 2);
    textBox.addView(uinView, uinParams);

    TextView infoView = new TextView(activity);
    infoView.setTextSize(11);
    infoView.setMaxLines(1);
    infoView.setIncludeFontPadding(false);
    infoView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    infoView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-2, -2);
    infoParams.topMargin = dp(activity, 2);
    textBox.addView(infoView, infoParams);

    item.addView(textBox);

    TextView roleView = new TextView(activity);
    roleView.setTextSize(11);
    roleView.setGravity(Gravity.CENTER);
    roleView.setIncludeFontPadding(false);
    roleView.setPadding(dp(activity, 9), dp(activity, 4), dp(activity, 9), dp(activity, 4));
    LinearLayout.LayoutParams roleParams = new LinearLayout.LayoutParams(-2, -2);
    roleParams.leftMargin = dp(activity, 10);
    item.addView(roleView, roleParams);

    item.setTag(new Object[] {titleView, uinView, infoView, roleView});
    bindSimpleListItem(activity, item, itemData, listMode, groupUin, isDark);
    return item;
}

void scheduleProgressiveListRender(final BaseAdapter adapter,
    final List display,
    final int[] visibleLimit,
    final boolean[] closed,
    final AlertDialog dialog,
    final ListView listView,
    final int batchSize,
    final Runnable[] taskRef,
    final String logName) {
    if (closed[0] || !dialog.isShowing() || display.isEmpty()) return;
    if (visibleLimit[0] >= display.size()) return;
    if (taskRef[0] != null && uiHandler != null) uiHandler.removeCallbacks(taskRef[0]);
    taskRef[0] = new Runnable() {
        public void run() {
            if (closed[0] || !dialog.isShowing()) return;
            try {
                if (visibleLimit[0] < display.size()) {
                    int first = listView == null ? 0 : listView.getFirstVisiblePosition();
                    int visible = listView == null ? 8 : Math.max(listView.getChildCount(), 8);
                    int target = Math.min(display.size(), first + visible + batchSize * 3);
                    int next = Math.min(target, visibleLimit[0] + batchSize);
                    if (next > visibleLimit[0]) {
                        visibleLimit[0] = next;
                        adapter.notifyDataSetChanged();
                    }
                    if (visibleLimit[0] < target) uiHandler.postDelayed(this, 16);
                }
            } catch (Throwable e) {
                traceLog("dialog_log", "[" + logName + "] 单批渲染异常: " + e);
            }
        }
    };
    uiHandler.post(taskRef[0]);
}

void bindSimpleListItem(Activity activity,
    View cv,
    Object[] itemData,
    int listMode,
    String groupUin,
    boolean isDark) {
    LinearLayout item = (LinearLayout) cv;
    Object tag = item.getTag();
    if (!(tag instanceof Object[])) return;
    Object[] holder = (Object[]) tag;
    if (holder.length < 4) return;
    if (!(holder[0] instanceof TextView) || !(holder[1] instanceof TextView) || !(holder[2] instanceof TextView) || !(holder[3] instanceof TextView)) return;

    TextView titleView = (TextView) holder[0];
    TextView uinView = (TextView) holder[1];
    TextView infoView = (TextView) holder[2];
    TextView roleView = (TextView) holder[3];
    String uin = String.valueOf(itemData[0]);
    String name = String.valueOf(itemData[1]);
    String info = itemData.length > 2 && itemData[2] != null ? String.valueOf(itemData[2]) : "";
    String role = itemData.length > 3 && itemData[3] != null ? String.valueOf(itemData[3]) : "";

    titleView.setText(name);
    uinView.setText("QQ · " + uin);
    if (listMode == 0 && info != null && !info.isEmpty()) {
        infoView.setText(info);
        infoView.setTextColor(tc(activity, "error"));
        infoView.setBackground(null);
        infoView.setPadding(0, 0, 0, 0);
        infoView.setVisibility(View.VISIBLE);
    } else {
        infoView.setText("");
        infoView.setBackground(null);
        infoView.setPadding(0, 0, 0, 0);
        infoView.setVisibility(View.GONE);
    }

    if (listMode == 1 && role != null && !role.isEmpty()) {
        applyRolePill(activity, roleView, role);
    } else {
        roleView.setText("");
        roleView.setVisibility(View.GONE);
    }

    final String finalUin = uin;
    final String finalName = name;
    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (listMode == 0) showShutUpDialog(activity, groupUin, finalUin, finalName);
            else if (listMode == 1) showMemberInfoDialog(activity, groupUin, finalUin, 2);
            else if (listMode == 2) showGroupInfoDialog(activity, finalUin, isDark);
            else showMemberInfoDialog(activity, finalUin, finalUin, 1);
        }
    });
}
void showSimpleListDialog(Activity act0,
    final String title,
    final int listMode,
    final String groupUin,
    final boolean isDark) {
    Activity dlgAct = act0;
    if (dlgAct == null || dlgAct.isFinishing()) {
        try {
            dlgAct = getNowActivity();
        } catch (Throwable ignored) {}
    }
    if (dlgAct == null || dlgAct.isFinishing()) {
        try {
            dlgAct = getSettingsCurrentActivity();
        } catch (Throwable ignored) {
            dlgAct = null;
        }
    }
    if (dlgAct == null || dlgAct.isFinishing()) {
        traceLog("dialog_log", "[showSimpleListDialog] 无可用 Activity，弹窗放弃显示");
        return;
    }
    try {
        final int textColor = pc(getSettingsThemeColor(dlgAct, "on_surface"));
        final int subTextColor = pc(getSettingsThemeColor(dlgAct, "on_surface_variant"));
        final String emptyText = listMode == 0 ? "当前群无禁言成员" : listMode == 1 ? "当前群无成员" : listMode == 2 ? "暂无群聊" : "暂无好友";
        final String countUnit = listMode == 2 ? "个" : "人";
        LinearLayout root = new LinearLayout(dlgAct);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(dlgAct, 14), dp(dlgAct, 14), dp(dlgAct, 14), dp(dlgAct, 10));
        final TextView titleView = new TextView(dlgAct);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(textColor);
        titleView.setPadding(0, 0, 0, dp(dlgAct, 4));
        root.addView(titleView);
        final EditText search = makeInput(dlgAct,
            listMode == 0 || listMode == 1 ? "搜索昵称或QQ号" : listMode == 2 ? "搜索群名或群号" : "搜索昵称或QQ号",
            null);
        search.setSingleLine(true);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(dlgAct, 42)));
        search.setVisibility(View.GONE);
        FrameLayout listFrame = new FrameLayout(dlgAct);
        LinearLayout.LayoutParams listFrameParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        listFrameParams.topMargin = dp(dlgAct, 8);
        listFrameParams.bottomMargin = dp(dlgAct, 2);
        root.addView(listFrame, listFrameParams);
        final ListView lv = new ListView(dlgAct);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        listFrame.addView(lv, new FrameLayout.LayoutParams(-1, -1));
        final LinearLayout loading = createModernLoading(dlgAct, isDark);
        listFrame.addView(loading, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        final TextView emptyView = new TextView(dlgAct);
        emptyView.setText(emptyText);
        emptyView.setTextSize(14);
        emptyView.setTextColor(subTextColor);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        listFrame.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout btnBox = new LinearLayout(dlgAct);
        btnBox.setOrientation(LinearLayout.HORIZONTAL);
        btnBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        btnBox.setMinimumHeight(dp(dlgAct, 44));
        TextView closeBtn = createButton(dlgAct, "关闭", pc(getSettingsThemeColor(dlgAct, "primary")), Color.TRANSPARENT,
            15f, 0, 16, 12, false, 0, 0, null);
        btnBox.addView(closeBtn);
        root.addView(btnBox);
        final AlertDialog dialog = new AlertDialog.Builder(dlgAct,
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setView(root)
            .create();
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
        applyUiTheme(dlgAct, dialog, 0);
        try {
            Window win = dialog.getWindow();
            if (win != null) {
                int screenW = dlgAct.getResources().getDisplayMetrics().widthPixels;
                int screenH = dlgAct.getResources().getDisplayMetrics().heightPixels;
                int width = Math.min(dp(dlgAct, 420), screenW - dp(dlgAct, 32));
                int maxHeight = Math.min(dp(dlgAct, 400), (int)(screenH * 0.52f));
                int minHeight = Math.min(dp(dlgAct, 280), Math.max(1, screenH - dp(dlgAct, 64)));
                int height = Math.max(minHeight, maxHeight);
                if (height > 0) win.setLayout(width, height);
            }
        } catch (Throwable e) {
            traceLog("dialog_log", "[showSimpleListDialog.window] 异常: " + e);
        }
        final ArrayList allData = new ArrayList();
        final ArrayList displayData = new ArrayList();
        final boolean[] closed = new boolean[] {false};
        final boolean[] loadFinished = new boolean[] {false};
        final int[] visibleLimit = new int[] {0};
        final int initialBatch = 64;
        final int batchSize = 48;
        final int dataBatchSize = 160;
        final Runnable[] renderTask = new Runnable[1];
        final int[] filterGeneration = new int[] {0};
        final Runnable updateListState = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                boolean hasData = !allData.isEmpty();
                if (hasData) {
                    if (search.getVisibility() != View.VISIBLE) {
                        search.setAlpha(0f);
                        search.setVisibility(View.VISIBLE);
                        search.animate().alpha(1f).setDuration(180).start();
                    }
                } else {
                    search.setVisibility(View.GONE);
                }
                if (displayData.isEmpty()) {
                    if (!loadFinished[0]) {
                        loading.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else if (!hasData) {
                        loading.setVisibility(View.GONE);
                        emptyView.setText(emptyText);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        loading.setVisibility(View.GONE);
                        emptyView.setText("无匹配结果");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    loading.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                }
                SpannableStringBuilder titleSpan = new SpannableStringBuilder(title + " (" + displayData.size() + countUnit + ")");
                titleSpan.setSpan(new ForegroundColorSpan(pc(getSettingsThemeColor(dlgAct, "primary"))), title.length(), titleSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                titleView.setText((CharSequence) titleSpan);
            }
        };
        final BaseAdapter adapter = new BaseAdapter() {
            public int getCount() {
                return Math.min(displayData.size(), visibleLimit[0]);
            }
            public Object getItem(int p) {
                return displayData.get(p);
            }
            public long getItemId(int p) {
                return p;
            }
            public View getView(int pos, View cv, ViewGroup parent) {
                try {
                    if (cv instanceof LinearLayout) {
                        Object t = ((LinearLayout) cv).getTag();
                        if (!(t instanceof Object[]) || ((Object[]) t).length < 4 || !(((Object[]) t)[0] instanceof TextView) || !(((Object[]) t)[1] instanceof TextView) || !(((Object[]) t)[2] instanceof TextView) || !(((Object[]) t)[3] instanceof TextView)) cv = null;
                    } else {
                        cv = null;
                    }
                    if (cv == null) {
                        cv = buildSimpleListItem(dlgAct, (Object[]) displayData.get(pos), listMode, groupUin, isDark);
                    } else {
                        bindSimpleListItem(dlgAct, cv, (Object[]) displayData.get(pos), listMode, groupUin, isDark);
                    }
                    return cv;
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showSimpleListDialog.getView] 异常: " + e);
                    return cv == null ? new TextView(dlgAct) : cv;
                }
            }
        };
        lv.setAdapter(adapter);
        final Runnable applyFilter = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                final String q = search.getText().toString().trim().toLowerCase();
                final int generation = ++filterGeneration[0];
                final ArrayList snapshot = new ArrayList(allData);
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            final ArrayList result = new ArrayList();
                            if (q.isEmpty()) {
                                result.addAll(snapshot);
                            } else {
                                for (Object obj : snapshot) {
                                    Object[] item = (Object[]) obj;
                                    String text = String.valueOf(item[0]) + " " + String.valueOf(item[1]) + " " + String.valueOf(item[2]);
                                    if (text.toLowerCase().contains(q)) result.add(item);
                                }
                            }
                            uiHandler.post(new Runnable() {
                                public void run() {
                                    if (closed[0] || !dialog.isShowing() || generation != filterGeneration[0]) return;
                                    if (snapshot.size() != allData.size()) {
                                        applyFilter.run();
                                        return;
                                    }
                                    displayData.clear();
                                    displayData.addAll(result);
                                    visibleLimit[0] = Math.min(displayData.size(), initialBatch);
                                    adapter.notifyDataSetChanged();
                                    updateListState.run();
                                    if (visibleLimit[0] < displayData.size()) scheduleProgressiveListRender(adapter, displayData, visibleLimit, closed, dialog, lv, batchSize, renderTask, "showSimpleListDialog.render");
                                }
                            });
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showSimpleListDialog.filter] 异常: " + e);
                        }
                    }
                });
            }
        };
        final Runnable searchTask = new Runnable() {
            public void run() {
                applyFilter.run();
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }
            public void afterTextChanged(Editable e) {
                search.removeCallbacks(searchTask);
                search.postDelayed(searchTask, 120);
            }
        });
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int state) {
            }
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (closed[0] || visible <= 0) return;
                if (first + visible >= visibleLimit[0] - 3 && visibleLimit[0] < displayData.size()) {
                    visibleLimit[0] = Math.min(displayData.size(), visibleLimit[0] + batchSize);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        ThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    List source = null;
                    final long listNow = System.currentTimeMillis() / 1000;
                    if (listMode == 0) source = (List) getProhibitList(groupUin);
                    else if (listMode == 1) source = (List) getGroupMemberList(groupUin);
                    else if (listMode == 2) source = (List) getGroupList();
                    else source = (List) getAllFriend();
                    if (source == null) source = new ArrayList();
                    ArrayList readyBatch = new ArrayList(dataBatchSize);
                    int batchIndex = 0;
                    for (Object o : source) {
                        if (o == null) continue;
                        try {
                            Object[] item;
                            if (listMode == 0) {
                                String gUin = String.valueOf(o.user);
                                String gName = String.valueOf(o.userName);
                                String gRemark = getUserRemark(gUin);
                                item = new Object[] {gUin, (gRemark == null || gRemark.isEmpty()) ? gName : gRemark + "(" + gName + ")", getGagStatus(String.valueOf(o.endTime), listNow), ""};
                            } else if (listMode == 1) {
                                String mUin = String.valueOf(o.uin);
                                String mName = String.valueOf(o.uinName);
                                String mRemark = getUserRemark(String.valueOf(o.uin));
                                String role = convertRole(o.role == null ? "" : String.valueOf(o.role));
                                item = new Object[] {mUin, (mRemark == null || mRemark.isEmpty()) ? mName : mRemark + "(" + mName + ")", "", role};
                            } else if (listMode == 2) {
                                String gUin = String.valueOf(o.group);
                                item = new Object[] {gUin, groupDisplayName(gUin), "", ""};
                            } else {
                                String uin = String.valueOf(o.uin);
                                String uName = String.valueOf(o.name);
                                String uRemark = getUserRemark(uin);
                                item = new Object[] {uin, (uRemark == null || uRemark.isEmpty()) ? uName : uRemark + "(" + uName + ")", "", ""};
                            }
                            readyBatch.add(item);
                            if (readyBatch.size() >= dataBatchSize) {
                                final ArrayList postBatch = new ArrayList(readyBatch);
                                readyBatch.clear();
                                final long batchDelay = batchIndex++ * 8L;
                                uiHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        if (closed[0] || !dialog.isShowing()) return;
                                        try {
                                            allData.addAll(postBatch);
                                            String q = search.getText().toString().trim().toLowerCase();
                                            if (q.isEmpty()) {
                                                displayData.addAll(postBatch);
                                            } else {
                                                for (Object obj : postBatch) {
                                                    Object[] item = (Object[]) obj;
                                                    String text = String.valueOf(item[0]) + " " + String.valueOf(item[1]) + " " + String.valueOf(item[2]);
                                                    if (text.toLowerCase().contains(q)) displayData.add(item);
                                                }
                                            }
                                            if (visibleLimit[0] < initialBatch) visibleLimit[0] = Math.min(displayData.size(), initialBatch);
                                            adapter.notifyDataSetChanged();
                                            updateListState.run();
                                            if (visibleLimit[0] < displayData.size()) scheduleProgressiveListRender(adapter, displayData, visibleLimit, closed, dialog, lv, batchSize, renderTask, "showSimpleListDialog.render");
                                        } catch (Throwable e) {
                                            traceLog("dialog_log", "[showSimpleListDialog.add] 批量异常: " + e);
                                        }
                                    }
                                }, batchDelay);
                            }
                        } catch (Throwable e) {
                            traceLog("dialog_log", "[showSimpleListDialog.data] 单条异常: " + e);
                        }
                    }
                    if (!readyBatch.isEmpty()) {
                        final ArrayList postBatch = new ArrayList(readyBatch);
                        final long lastBatchDelay = batchIndex++ * 8L;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (closed[0] || !dialog.isShowing()) return;
                                        try {
                                            allData.addAll(postBatch);
                                            String q = search.getText().toString().trim().toLowerCase();
                                            if (q.isEmpty()) {
                                                displayData.addAll(postBatch);
                                            } else {
                                                for (Object obj : postBatch) {
                                                    Object[] item = (Object[]) obj;
                                                    String text = String.valueOf(item[0]) + " " + String.valueOf(item[1]) + " " + String.valueOf(item[2]);
                                                    if (text.toLowerCase().contains(q)) displayData.add(item);
                                                }
                                            }
                                            if (visibleLimit[0] < initialBatch) visibleLimit[0] = Math.min(displayData.size(), initialBatch);
                                            adapter.notifyDataSetChanged();
                                            updateListState.run();
                                            if (visibleLimit[0] < displayData.size()) scheduleProgressiveListRender(adapter, displayData, visibleLimit, closed, dialog, lv, batchSize, renderTask, "showSimpleListDialog.render");
                                        } catch (Throwable e) {
                                            traceLog("dialog_log", "[showSimpleListDialog.add] 批量异常: " + e);
                                        }
                            }
                        }, lastBatchDelay);
                    }
                    final long finishDelay = batchIndex * 8L + 8L;
                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (closed[0] || !dialog.isShowing()) return;
                            loadFinished[0] = true;
                            updateListState.run();
                        }
                    }, finishDelay);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showSimpleListDialog.load] 异常: " + e);
                }
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                closed[0] = true;
                search.removeCallbacks(searchTask);
                if (renderTask[0] != null && uiHandler != null) uiHandler.removeCallbacks(renderTask[0]);
                allData.clear();
                displayData.clear();
            }
        });
    } catch (Throwable e) {
        traceLog("dialog_log", "[showSimpleListDialog] 异常: " + e);
    }
}
void showProhibitListDialog(Activity activity, String groupUin, boolean isDark) {
    showSimpleListDialog(activity, "禁言列表", 0, groupUin, isDark);
}

void showGroupMemberListDialog(Activity activity, String groupUin, boolean isDark) {
    showSimpleListDialog(activity, "群成员列表", 1, groupUin, isDark);
}

void showGroupListDialog(Activity activity, boolean isDark) {
    showSimpleListDialog(activity, "我的群列表", 2, null, isDark);
}

void showFriendListDialog(Activity activity, boolean isDark) {
    showSimpleListDialog(activity, "我的好友列表", 3, null, isDark);
}
interface GroupSelectCallback {
    void onSelected(List selected);
}

void showGroupSelector(final Activity act, final int mode, final List initSelected, final GroupSelectCallback callback) {
    if (act == null || act.isFinishing()) return;
    try {
        final boolean isDark = isThemeDark(act);
        final int colorAccent = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");
        final int colorOnSurface = tc(activity, "on_surface_strong");
        final int colorOnSurfaceVar = tc(activity, "on_surface_sub");
        LinearLayout root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(act, 24), dp(act, 24), dp(act, 24), dp(act, 24));
        TextView title = new TextView(act);
        title.setText(mode == 1 ? "选择好友" : mode == 2 ? "选择群聊" : "选择群聊与好友");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(colorOnSurface);
        root.addView(title);
        final TextView summary = new TextView(act);
        summary.setText("正在加载…");
        summary.setTextSize(14);
        summary.setTextColor(colorOnSurfaceVar);
        summary.setPadding(0, dp(act, 4), 0, dp(act, 16));
        root.addView(summary);
        final EditText search = makeInput(act, mode == 1 ? "搜索昵称或QQ号" : mode == 2 ? "搜索群名或群号" : "搜索昵称或群名或QQ号", null);
        root.addView(search);
        search.setVisibility(View.GONE);
        FrameLayout listFrame = new FrameLayout(act);
        root.addView(listFrame, new LinearLayout.LayoutParams( - 1, 0, 1f));
        final ListView lv = new ListView(act);
        lv.setDivider(null);
        lv.setDividerHeight(0);
        lv.setClipToPadding(false);
        lv.setPadding(0, dp(act, 8), 0, dp(act, 8));
        listFrame.addView(lv, new FrameLayout.LayoutParams( - 1, - 1));
        final LinearLayout loading = createModernLoading(act, isDark);
        listFrame.addView(loading, new FrameLayout.LayoutParams( - 2, - 2, Gravity.CENTER));
        final TextView emptyView = new TextView(act);
        emptyView.setTextSize(14);
        emptyView.setTextColor(colorOnSurfaceVar);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setVisibility(View.GONE);
        listFrame.addView(emptyView, new FrameLayout.LayoutParams( - 1, - 1));
        LinearLayout row1 = new LinearLayout(act);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.END);
        row1.setPadding(0, dp(act, 12), 0, dp(act, 16));
        TextView bCancelAll = makeChip(act, "取消全选", false, 1);
        TextView bReverse = makeChip(act, "反选", false, 1);
        TextView bSelectAll = makeChip(act, "全选", false, 1);
        row1.addView(bCancelAll);
        LinearLayout.LayoutParams lpRev = new LinearLayout.LayoutParams( - 2, - 2);
        lpRev.leftMargin = dp(act, 8);
        row1.addView(bReverse, lpRev);
        LinearLayout.LayoutParams lpAll = new LinearLayout.LayoutParams( - 2, - 2);
        lpAll.leftMargin = dp(act, 8);
        row1.addView(bSelectAll, lpAll);
        root.addView(row1);
        LinearLayout row2 = new LinearLayout(act);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.END);
        row2.setPadding(0, dp(act, 8), 0, 0);
        TextView btnCancel = createButton(act, "取消", colorAccent, Color.TRANSPARENT, 14f, 0, 16, 12, false, 0, 0, null);
        TextView btnConfirm = createButton(act, "确定", colorAccent, Color.TRANSPARENT, 14f, 0, 16, 12, true, 0, 0, null);
        row2.addView(btnCancel);
        row2.addView(btnConfirm);
        root.addView(row2);
        final AlertDialog dialog = new AlertDialog.Builder(act,
            isDark ? AlertDialog
                .THEME_DEVICE_DEFAULT_DARK : AlertDialog
                .THEME_DEVICE_DEFAULT_LIGHT)
                .setView(root)
                .create();
        dialog.show();
        applyUiTheme(act, dialog, 0);
        final ArrayList allItems = new ArrayList();
        final ArrayList display = new ArrayList();
        final Set selected = new HashSet(initSelected != null ? initSelected : new ArrayList());
        final boolean[]closed = new boolean[] {
            false
        };
        final Runnable[]renderTask = new Runnable[1];
        final int[]visibleLimit = new int[] {
            64
        };
        final int[]fullDisplayCount = new int[] {
            0
        };
        final boolean[] loadFinished = new boolean[] {false};
        final int[] pending = new int[] {(mode == 1 || mode == 2) ? 1 : 2};
        final int listBatchSize = 48;
        BaseAdapter adapter = new BaseAdapter() {
            public int getCount() {
                return Math.min(display.size(), visibleLimit[0]);
            }
            public Object getItem(int p) {
                return display.get(p);
            }
            public long getItemId(int p) {
                return p;
            }
            public View getView(final int pos, View cv, ViewGroup parent) {
                try {
                    LinearLayout item;
                    LinearLayout box;
                    CheckBox cb;
                    TextView tv;
                    TextView sub;
                    if (!(cv instanceof LinearLayout)) cv = null;
                    if (cv == null) {
                        item = new LinearLayout(act);
                        item.setOrientation(LinearLayout.HORIZONTAL);
                        item.setGravity(Gravity.CENTER_VERTICAL);
                        item.setPadding(dp(act, 8), dp(act, 12), dp(act, 8), dp(act, 12));
                        item.setBackground(createRippleBg(act, Color.TRANSPARENT, 0));
                        cb = new CheckBox(context);
                        cb.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 32), dp(act, 32)));
                        item.addView(cb);
                        box = new LinearLayout(act);
                        box.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(0, -2, 1);
                        boxLp.leftMargin = dp(act, 16);
                        tv = new TextView(act);
                        tv.setTextSize(16);
                        tv.setTextColor(colorOnSurface);
                        box.addView(tv);
                        sub = new TextView(act);
                        sub.setTextSize(12);
                        sub.setTextColor(colorOnSurfaceVar);
                        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
                        subLp.topMargin = dp(act, 2);
                        box.addView(sub, subLp);
                        item.addView(box, boxLp);
                        item.setTag(new Object[] { cb, tv, sub });
                    } else {
                        item = (LinearLayout) cv;
                        View checkChild = item.getChildAt(0);
                        View boxChild = item.getChildAt(1);
                        if (!(checkChild instanceof CheckBox) || !(boxChild instanceof LinearLayout) || ((LinearLayout) boxChild).getChildCount() < 2) {
                            item = new LinearLayout(act);
                            item.setOrientation(LinearLayout.HORIZONTAL);
                            item.setGravity(Gravity.CENTER_VERTICAL);
                            item.setPadding(dp(act, 8), dp(act, 12), dp(act, 8), dp(act, 12));
                            item.setBackground(createRippleBg(act, Color.TRANSPARENT, 0));
                            cb = new CheckBox(context);
                            cb.setLayoutParams(new LinearLayout.LayoutParams(dp(act, 32), dp(act, 32)));
                            item.addView(cb);
                            box = new LinearLayout(act);
                            box.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(0, -2, 1);
                            boxLp.leftMargin = dp(act, 16);
                            tv = new TextView(act);
                            tv.setTextSize(16);
                            tv.setTextColor(colorOnSurface);
                            box.addView(tv);
                            sub = new TextView(act);
                            sub.setTextSize(12);
                            sub.setTextColor(colorOnSurfaceVar);
                            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2);
                            subLp.topMargin = dp(act, 2);
                            box.addView(sub, subLp);
                            item.addView(box, boxLp);
                            item.setTag(new Object[] { cb, tv, sub });
                        } else {
                            cb = (CheckBox) checkChild;
                            box = (LinearLayout) boxChild;
                            tv = (TextView) box.getChildAt(0);
                            sub = (TextView) box.getChildAt(1);
                        }
                    }
                    final Map m = (Map) display.get(pos);
                    final String uin = String.valueOf(m.get("uin"));
                    tv.setText(String.valueOf(m.get("show")));
                    sub.setText(uin);
                    cb.setOnCheckedChangeListener(null);
                    cb.setChecked(selected.contains(uin));
                    cb.setTag(uin);
                    cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        public void onCheckedChanged(CompoundButton b, boolean checked) {
                            String u = String.valueOf(b.getTag());
                            if (checked) selected.add(u);
                            else selected.remove(u);
                            summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                        }
                    });
                    item.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            Object[]h = (Object[]) v.getTag();
                            CheckBox c = (CheckBox) h[0];
                            c.setChecked(!c.isChecked());
                        }
                    });
                    return item;
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.getView] 异常: " + e);
                    return cv == null ? new TextView(act) : cv;
                }
            }
        };
        lv.setAdapter(adapter);
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int state) {
            }
            public void onScroll(AbsListView view, int first, int visible, int total) {
                if (total > 0 && first + visible >= visibleLimit[0] - 2 && visibleLimit[0] < display.size()) {
                    visibleLimit[0] = Math.min(display.size(), visibleLimit[0] + listBatchSize);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        final Runnable updateState = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                boolean hasData = !allItems.isEmpty();
                if (hasData) {
                    if (search.getVisibility() != View.VISIBLE) {
                        search.setAlpha(0f);
                        search.setVisibility(View.VISIBLE);
                        search.animate().alpha(1f).setDuration(180).start();
                    }
                } else {
                    search.setVisibility(View.GONE);
                }
                summary.setText("已选 " + selected.size() + " / 总 " + display.size());
                if (display.isEmpty()) {
                    if (!loadFinished[0]) {
                        loading.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else if (!hasData) {
                        loading.setVisibility(View.GONE);
                        emptyView.setText("暂无数据");
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        loading.setVisibility(View.GONE);
                        emptyView.setText("无匹配结果");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    loading.setVisibility(View.GONE);
                    emptyView.setVisibility(View.GONE);
                }
            }
        };
        final Runnable markDone = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                pending[0]--;
                if (pending[0] <= 0) {
                    loadFinished[0] = true;
                    updateState.run();
                }
            }
        };
        final Runnable refreshDisplay = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                try {
                    String q = search.getText().toString().trim().toLowerCase();
                    if (q.isEmpty() && selected.isEmpty() && fullDisplayCount[0] == display.size()) {
                        for (int i = display.size(); i < allItems.size(); i++) display.add(allItems.get(i));
                        fullDisplayCount[0] = allItems.size();
                    } else {
                        display.clear();
                        if (q.isEmpty()) {
                            if (selected.isEmpty()) {
                                display.addAll(allItems);
                                fullDisplayCount[0] = allItems.size();
                            } else {
                                ArrayList selectedFirst = new ArrayList();
                                ArrayList normal = new ArrayList();
                                for (Object o : allItems) {
                                    Map m = (Map) o;
                                    if (selected.contains(m.get("uin"))) selectedFirst.add(o);
                                    else normal.add(o);
                                }
                                display.addAll(selectedFirst);
                                display.addAll(normal);
                                fullDisplayCount[0] = -1;
                            }
                        } else {
                        for (Object o : allItems) {
                            Map m = (Map) o;
                            String n = String.valueOf(m.get("name"));
                            String u = String.valueOf(m.get("uin"));
                            String r = String.valueOf(m.get("remark"));
                            if (n.toLowerCase().contains(q) || u.contains(q) || r.toLowerCase().contains(q)) display.add(m);
                        }
                        ArrayList selectedFirst = new ArrayList();
                        ArrayList normal = new ArrayList();
                        for (Object o : display) {
                            Map m = (Map) o;
                            if (selected.contains(m.get("uin"))) selectedFirst.add(o);
                            else normal.add(o);
                        }
                        display.clear();
                        display.addAll(selectedFirst);
                        display.addAll(normal);
                        fullDisplayCount[0] = -1;
                        }
                    }
                    visibleLimit[0] = Math.min(64, display.size());
                    adapter.notifyDataSetChanged();
                    updateState.run();
                    if (!display.isEmpty() && visibleLimit[0] < display.size()) scheduleProgressiveListRender(adapter,
                        display,
                        visibleLimit,
                        closed,
                        dialog,
                        lv,
                        listBatchSize,
                        renderTask,
                        "showGroupSelector.render");
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.refresh] 异常: " + e);
                }
            }
        };
        final Runnable addReady = new Runnable() {
            public void run() {
                if (closed[0] || !dialog.isShowing()) return;
                if (!allItems.isEmpty()) refreshDisplay.run();
            }
        };
        final Runnable addFriends = new Runnable() {
            public void run() {
                try {
                    List friends = (List) getAllFriend();
                    if (friends == null) friends = new ArrayList();
                    final int dataBatchSize = 160;
                    ArrayList batch = new ArrayList(dataBatchSize);
                    int batchIndex = 0;
                    for (Object obj : friends) {
                        if (obj == null) continue;
                        Map m = new HashMap();
                        String fUin = String.valueOf(obj.uin);
                        String fName = getUserName(fUin);
                        String fRemark = getUserRemark(fUin);
                        m.put("uin", fUin);
                        m.put("name", fName);
                        m.put("remark", fRemark);
                        m.put("show", (fRemark == null || fRemark.isEmpty()) ? fName : fRemark + "(" + fName + ")");
                        batch.add(m);
                        if (batch.size() >= dataBatchSize) {
                            final ArrayList postBatch = new ArrayList(batch);
                            batch.clear();
                            final long batchDelay = batchIndex++ * 8L;
                            uiHandler.postDelayed(new Runnable() {
                                public void run() {
                                    if (closed[0] || !dialog.isShowing()) return;
                                    try {
                                        allItems.addAll(postBatch);
                                        addReady.run();
                                    } catch (Throwable e) {
                                        traceLog("dialog_log", "[showGroupSelector.friends.add] 批量异常: " + e);
                                    }
                                }
                            }, batchDelay);
                        }
                    }
                    if (!batch.isEmpty()) {
                        final ArrayList postBatch = new ArrayList(batch);
                        final long lastBatchDelay = batchIndex++ * 8L;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (closed[0] || !dialog.isShowing()) return;
                                try {
                                    allItems.addAll(postBatch);
                                    addReady.run();
                                } catch (Throwable e) {
                                    traceLog("dialog_log", "[showGroupSelector.friends.add] 末批异常: " + e);
                                }
                            }
                        }, lastBatchDelay);
                    }
                    uiHandler.postDelayed(markDone, batchIndex * 8L + 16L);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.friends] 异常: " + e);
                }
            }
        };
        final Runnable addGroups = new Runnable() {
            public void run() {
                try {
                    List groups = (List) getGroupList();
                    if (groups == null) groups = new ArrayList();
                    final int dataBatchSize = 160;
                    ArrayList batch = new ArrayList(dataBatchSize);
                    int batchIndex = 0;
                    for (Object obj : groups) {
                        if (obj == null) continue;
                        Map m = new HashMap();
                        String gUin = String.valueOf(obj.group);
                        String gShow = groupDisplayName(gUin);
                        m.put("uin", gUin);
                        m.put("name", gShow);
                        m.put("remark", getGroupRemark(gUin));
                        m.put("show", gShow);
                        batch.add(m);
                        if (batch.size() >= dataBatchSize) {
                            final ArrayList postBatch = new ArrayList(batch);
                            batch.clear();
                            final long batchDelay = batchIndex++ * 8L;
                            uiHandler.postDelayed(new Runnable() {
                                public void run() {
                                    if (closed[0] || !dialog.isShowing()) return;
                                    try {
                                        allItems.addAll(postBatch);
                                        addReady.run();
                                    } catch (Throwable e) {
                                        traceLog("dialog_log", "[showGroupSelector.groups.add] 批量异常: " + e);
                                    }
                                }
                            }, batchDelay);
                        }
                    }
                    if (!batch.isEmpty()) {
                        final ArrayList postBatch = new ArrayList(batch);
                        final long lastBatchDelay = batchIndex++ * 8L;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (closed[0] || !dialog.isShowing()) return;
                                try {
                                    allItems.addAll(postBatch);
                                    addReady.run();
                                } catch (Throwable e) {
                                    traceLog("dialog_log", "[showGroupSelector.groups.add] 末批异常: " + e);
                                }
                            }
                        }, lastBatchDelay);
                    }
                    uiHandler.postDelayed(markDone, batchIndex * 8L + 16L);
                } catch (Throwable e) {
                    traceLog("dialog_log", "[showGroupSelector.groups] 异常: " + e);
                }
            }
        };
        if (mode == 1 || mode == 3) ThreadPool.execute(addFriends);
        if (mode == 2 || mode == 3) ThreadPool.execute(addGroups);
        final Runnable searchRunnable = new Runnable() {
            public void run() {
                refreshDisplay.run();
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }
            public void afterTextChanged(Editable e) {
                search.removeCallbacks(searchRunnable);
                search.postDelayed(searchRunnable, 120);
            }
        });
        bCancelAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selected.clear();
                adapter.notifyDataSetChanged();
                summary.setText("已选 0 / 总 " + display.size());
            }
        });
        bReverse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Set old = new HashSet(selected);
                for (Object o: display) {
                    Map m = (Map) o;
                    String u = String.valueOf(m.get("uin"));
                    if (old.contains(u)) selected.remove(u);
                    else selected.add(u);
                }
                adapter.notifyDataSetChanged();
                summary.setText("已选 " + selected.size() + " / 总 " + display.size());
            }
        });
        bSelectAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Object o: display) selected.add(String.valueOf(((Map) o).get("uin")));
                adapter.notifyDataSetChanged();
                summary.setText("已选 " + selected.size() + " / 总 " + display.size());
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (callback != null) callback.onSelected(new ArrayList(selected));
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                closed[0] = true;
                if (searchRunnable != null) search.removeCallbacks(searchRunnable);
                if (renderTask[0] != null && uiHandler != null) uiHandler.removeCallbacks(renderTask[0]);
                allItems.clear();
                display.clear();
            }
        });
    } catch (Throwable e) {
        traceLog("dialog_log", "[showGroupSelector] 异常: " + e);
    }
}
