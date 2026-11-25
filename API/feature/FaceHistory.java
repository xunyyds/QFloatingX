

Object faceHistoryActivePage = null;

void faceHistorySetActivePage(Object page) { faceHistoryActivePage = page; }

class FaceHistoryItem {
    String fileid = "";
    String headurl = "";
    int faceFlag = 0;
    long timestamp = 0;

    boolean valid() {
        return fileid != null && !fileid.isEmpty();
    }
}

class FaceHistoryParseResult {
    List items = new ArrayList();
    long nextTimestamp = 0;
    boolean isEnd = false;
    int errorCode = 0;
    String errorMessage = "";
    int listField = 0;
}

int faceHistoryCfgInt(String key, int defValue) {
    try { return getInt("face_history", key, defValue); } catch (Throwable e) { return defValue; }
}

void faceHistoryRequestList(final long timestamp, final int num, final CommonCallback callback) {
    final int cmdId = faceHistoryCfgInt("list_cmd", 0x5f1);
    final int service = faceHistoryCfgInt("list_service", 6);
    final JSONObject body = new JSONObject();
    try {
        body.put(String.valueOf(faceHistoryCfgInt("req_ts_field", 1)), timestamp);
        body.put(String.valueOf(faceHistoryCfgInt("req_num_field", 2)), num);
    } catch (Throwable e) {
        traceLog("face_history_log", "[list] 构造请求失败 " + e);
        if (callback != null) callback.onResult(-2, "构造请求失败", null, 0, true);
        return;
    }
    final long startedAt = System.currentTimeMillis();
    final boolean[] finished = new boolean[]{false};
    final Runnable timeoutTask = new Runnable() {
        public void run() {
            if (finished[0]) return;
            finished[0] = true;
            traceLog("face_history_log", "[list] 超时（12s 无回调）cmd=" + "OidbSvc.0x5f1_6");
            if (callback != null) callback.onResult(-9, "请求超时（12 秒无回调）", null, 0, true);
        }
    };
    if (uiHandler != null) uiHandler.postDelayed(timeoutTask, 12000);
    oidbSend("face_history_log", "OidbSvc.0x5f1_6", cmdId, service, body, new IReceiver() {
        public void onReceive(byte[] resp) {
            if (finished[0]) return;
            finished[0] = true;
            long cost = System.currentTimeMillis() - startedAt;
            FaceHistoryParseResult parsed = faceHistoryParseListResponse(resp);
            if (parsed == null) {
                if (callback != null) callback.onResult(-1, "无响应（发包失败或超时）", null, 0, true);
                return;
            }
            if (parsed.errorCode != 0) {
                if (callback != null) callback.onResult(parsed.errorCode, parsed.errorMessage, null, 0, true);
                return;
            }
            if (callback != null) callback.onResult(0, "", parsed.items, parsed.nextTimestamp, parsed.isEnd);
        }
    });
}

FaceHistoryParseResult faceHistoryParseListResponse(byte[] resp) {
    FaceHistoryParseResult parsed = new FaceHistoryParseResult();
    if (resp == null || resp.length == 0) {
        parsed.errorCode = -1;
        parsed.errorMessage = "无响应";
        return parsed;
    }
    OidbResult result = oidbParse(resp);
    if (result.raw == null) {
        parsed.errorCode = -1;
        parsed.errorMessage = "解析失败";
        return parsed;
    }
    if (result.resultCode != 0) {
        parsed.errorCode = result.resultCode;
        parsed.errorMessage = "OIDB 返回码 " + result.resultCode + (result.errorMsg == null || result.errorMsg.isEmpty() ? "" : " " + result.errorMsg);
        return parsed;
    }
    FunProtoData body = result.body;
    if (body == null) {
        parsed.nextTimestamp = 0;
        parsed.isEnd = true;
        return parsed;
    }

    int listField = faceHistoryCfgInt("rsp_list_field", 1);
    parsed.listField = listField;
    if (listField > 0) {
        List rawList = body.getValues(listField);
        if (rawList != null) {
            for (int i = 0; i < rawList.size(); i++) {
                Object element = rawList.get(i);
                if (element instanceof FunProtoData) {
                    FaceHistoryItem item = faceHistoryParseItem((FunProtoData) element);
                    if (item != null && item.valid()) parsed.items.add(item);
                }
            }
        }
    }

    parsed.nextTimestamp = (long) faceHistoryReadInt(body, faceHistoryCfgInt("rsp_lastts_field", 3), 0);
    parsed.isEnd = faceHistoryReadInt(body, faceHistoryCfgInt("rsp_end_field", 4), 0) == 1;
    if (parsed.nextTimestamp <= 0) parsed.isEnd = true;

    int errCode = faceHistoryReadInt(body, faceHistoryCfgInt("rsp_err_field", 2), 0);
    parsed.errorCode = errCode;
    if (errCode != 0) {
        parsed.errorMessage = "服务返回错误码 " + errCode;
        return parsed;
    }
    return parsed;
}

int faceHistoryReadInt(FunProtoData node, int field, int defValue) {
    if (node == null) return defValue;
    List values = node.getValues(field);
    if (values == null || values.isEmpty()) return defValue;
    Object value = values.get(0);
    if (value instanceof Number) return oidbNumToInt(value, defValue);
    return defValue;
}

FaceHistoryItem faceHistoryParseItem(FunProtoData node) {
    if (node == null) return null;
    FaceHistoryItem item = new FaceHistoryItem();
    int headField = faceHistoryCfgInt("item_head_field", 2);
    int fileField = faceHistoryCfgInt("item_file_field", 1);
    int flagField = faceHistoryCfgInt("item_flag_field", 4);
    int tsField = faceHistoryCfgInt("item_ts_field", 6);

    List keys = new ArrayList(node.values.keySet());
    for (int i = 0; i < keys.size(); i++) {
        int field = oidbNumToInt(keys.get(i), 0);
        List values = node.getValues(field);
        if (values == null) continue;
        for (int j = 0; j < values.size(); j++) {
            Object value = values.get(j);
            if (value instanceof String) {
                String text = (String) value;
                if (text.startsWith("hex->")) continue;
                boolean isUrl = text.startsWith("http") || text.contains("://") || text.contains("qlogo.cn");
                if (isUrl && (headField == 0 ? item.headurl.isEmpty() : field == headField)) {
                    item.headurl = text.replace("http://", "https://");
                } else if (!isUrl && (fileField == 0 ? item.fileid.isEmpty() : field == fileField)) {
                    if (text.length() >= 4 && !text.contains(" ")) item.fileid = text;
                }
            } else if (value instanceof Number) {
                long number = oidbNumToLong(value, 0L);
                if (tsField > 0 ? field == tsField : (number >= 1000000000L && item.timestamp == 0)) {
                    item.timestamp = number;
                } else if (flagField > 0 ? field == flagField : (number >= 0 && number <= 255 && item.faceFlag == 0)) {
                    item.faceFlag = oidbNumToInt(value, 0);
                }
            }
        }
    }
    return item;
}

void faceHistoryRequestSet(final String fileid, final CommonCallback callback) {
    final JSONObject body = new JSONObject();
    try {
        body.put(String.valueOf(faceHistoryCfgInt("set_file_field", 1)), fileid);
        body.put(String.valueOf(faceHistoryCfgInt("set_sync_field", 2)), 1);
    } catch (Throwable e) {
        if (callback != null) callback.onResult(-2, "构造请求失败", null, 0, true);
        return;
    }
    oidbSend("face_history_log", "OidbSvc.0x8de_4", faceHistoryCfgInt("set_cmd", 0x8de), faceHistoryCfgInt("set_service", 4), body, new IReceiver() {
        public void onReceive(byte[] resp) {
            OidbResult result = oidbParse(resp);
            int code = result.raw == null ? -1 : result.resultCode;
            String message = code == 0 ? "已提交" : ("失败 code=" + code + (result.errorMsg == null || result.errorMsg.isEmpty() ? "" : " " + result.errorMsg));
            if (callback != null) callback.onResult(code, message, null, 0, true);
        }
    });
}

void faceHistoryRequestDelete(final List fileids, final CommonCallback callback) {
    if (fileids == null || fileids.isEmpty()) {
        if (callback != null) callback.onResult(-2, "没有可删除的头像", null, 0, true);
        return;
    }
    final JSONObject body = new JSONObject();
    try {
        JSONArray array = new JSONArray();
        for (int i = 0; i < fileids.size(); i++) array.put(String.valueOf(fileids.get(i)));
        body.put(String.valueOf(faceHistoryCfgInt("del_files_field", 1)), array);
    } catch (Throwable e) {
        if (callback != null) callback.onResult(-2, "构造请求失败", null, 0, true);
        return;
    }
    oidbSend("face_history_log", "OidbSvc.0x5f2_3", faceHistoryCfgInt("del_cmd", 0x5f2), faceHistoryCfgInt("del_service", 3), body, new IReceiver() {
        public void onReceive(byte[] resp) {
            OidbResult result = oidbParse(resp);
            int code = result.raw == null ? -1 : result.resultCode;
            String message = code == 0 ? "已删除" : ("失败 code=" + code);
            if (callback != null) callback.onResult(code, message, null, 0, true);
        }
    });
}

String faceHistoryCurrentFileid() {
    FaceHistoryPage page = (FaceHistoryPage) faceHistoryActivePage;
    if (page == null || page.items.isEmpty()) return null;
    long newest = 0;
    for (int i = 0; i < page.items.size(); i++) {
        FaceHistoryItem item = (FaceHistoryItem) page.items.get(i);
        if (item.timestamp > newest) newest = item.timestamp;
    }
    for (int i = 0; i < page.items.size(); i++) {
        FaceHistoryItem item = (FaceHistoryItem) page.items.get(i);
        if (item.timestamp == newest && item.valid()) return item.fileid;
    }
    return null;
}

String faceHistoryDeleteTitle(FaceHistoryItem item) {
    String currentFileid = faceHistoryCurrentFileid();
    if (currentFileid != null && currentFileid.equals(item.fileid)) {
        return "删除的是当前使用中的头像";
    }
    return "删除这张历史头像";
}

class FaceHistoryPage {
    final Activity activity;
    final boolean isDark;
    final List items = new ArrayList();
    long nextTimestamp = 0;
    boolean endReached = false;
    boolean loading = false;
    long cacheTime = 0;
    String lastError = "";

    LinearLayout root;
    MultiSelectGrid grid;
    LinearLayout skeletonHost;
    TextView skeletonFooter;
    final List skeletonCells = new ArrayList();
    ImageView headerImage;
    TextView statusView;
    TextView moreBtn;
    TextView emptyView;
    Object self;

    FaceHistoryPage(Activity activity, boolean isDark) {
        this.activity = activity;
        this.isDark = isDark;
        this.self = this;
    }

    void runOnUi(Runnable runnable) {
        if (runnable == null) return;
        try {
            if (activity == null || activity.isFinishing()) return;
            activity.runOnUiThread(runnable);
        } catch (Throwable e) {
            traceLog("face_history_log", "[ui] 切换线程失败 " + e);
        }
    }

    LinearLayout card() {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, 16)));
        layout.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(activity, 8);
        layout.setLayoutParams(params);
        return layout;
    }

    TextView title(String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(13);
        view.setTypeface(null, Typeface.BOLD);
        view.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        view.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 6));
        return view;
    }

    View build() {
        faceHistorySetActivePage(this);
        root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 18));

        root.addView(buildHeaderCard());
        root.addView(buildToolRow());
        root.addView(buildGridCard());

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) { watchScrollEnd(root, new Runnable() { public void run() { autoLoadMore(); } }, dp(activity, 420)); }
            public void onViewDetachedFromWindow(View v) { stopShimmer(); }
        });

        loadCache();
        refresh();
        return root;
    }

    View buildHeaderCard() {
        LinearLayout layout = card();
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        headerImage = new ImageView(activity);
        headerImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        headerImage.setBackground(roundRect(pc(isDark ? "#33FFFFFF" : "#11000000"), dp(activity, 14)));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(activity, 64), dp(activity, 64));
        headerImage.setLayoutParams(imageParams);
        row.addView(headerImage);

        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1f);
        infoParams.leftMargin = dp(activity, 12);
        info.setLayoutParams(infoParams);

        TextView nameView = new TextView(activity);
        nameView.setText("历史头像");
        nameView.setTextSize(16);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
        info.addView(nameView);

        statusView = new TextView(activity);
        statusView.setText("准备中…");
        statusView.setTextSize(12);
        statusView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        statusView.setPadding(0, dp(activity, 4), 0, 0);
        info.addView(statusView);

        row.addView(info);
        layout.addView(row);

        remoteImageLoad(qqAvatarUrl(myUin, 640), headerImage, dp(activity, 64), "fh_self");
        return layout;
    }

    View buildToolRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.topMargin = dp(activity, 6);
        row.setLayoutParams(rowParams);

        TextView refreshBtn = createButton(activity, "刷新", pc("#FFFFFFFF"), pc(getSettingsThemeColor(activity, "primary")), 12f, 12, 16, 8, false);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(activity, 24);
                refresh();
            }
        });
        row.addView(refreshBtn);

        TextView allBtn = createButton(activity, "全选", pc(getSettingsThemeColor(activity, "on_surface")), pc(isDark ? "#22FFFFFF" : "#11000000"), 12f, 12, 16, 8, false);
        LinearLayout.LayoutParams allParams = new LinearLayout.LayoutParams(-2, -2);
        allParams.leftMargin = dp(activity, 8);
        allBtn.setLayoutParams(allParams);
        allBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (items.isEmpty()) {
                    qqToast(1, "还没有加载到历史头像");
                    return;
                }
                grid.selectAll();
                qqToast(2, "已全选 " + grid.getSelectedCount() + " 张，点「取消」退出多选");
            }
        });
        row.addView(allBtn);

        TextView invertBtn = createButton(activity, "反选", pc(getSettingsThemeColor(activity, "on_surface")), pc(isDark ? "#22FFFFFF" : "#11000000"), 12f, 12, 16, 8, false);
        LinearLayout.LayoutParams invertParams = new LinearLayout.LayoutParams(-2, -2);
        invertParams.leftMargin = dp(activity, 8);
        invertBtn.setLayoutParams(invertParams);
        invertBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { grid.invertSelection(); }
        });
        row.addView(invertBtn);

        TextView officialBtn = createButton(activity, "官方页", pc(getSettingsThemeColor(activity, "on_surface")), pc(isDark ? "#22FFFFFF" : "#11000000"), 12f, 12, 16, 8, false);
        LinearLayout.LayoutParams officialParams = new LinearLayout.LayoutParams(-2, -2);
        officialParams.leftMargin = dp(activity, 8);
        officialBtn.setLayoutParams(officialParams);
        officialBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openOfficialPage(); }
        });
        row.addView(officialBtn);

        TextView clearBtn = createButton(activity, "清缓存", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 12f, 12, 10, 8, false);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-2, -2);
        clearParams.leftMargin = dp(activity, 8);
        clearBtn.setLayoutParams(clearParams);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearCache();
                clearRemoteImageCache();
                qqToast(2, "已清理缓存");
            }
        });
        row.addView(clearBtn);
        return row;
    }

    View buildGridCard() {
        LinearLayout layout = card();
        layout.addView(title("头像列表"));

        emptyView = new TextView(activity);
        emptyView.setText("还没有数据，点「刷新」从 QQ 拉取历史头像。");
        emptyView.setTextSize(12);
        emptyView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        emptyView.setPadding(dp(activity, 4), dp(activity, 10), dp(activity, 4), dp(activity, 10));
        emptyView.setVisibility(View.GONE);
        layout.addView(emptyView);

        skeletonHost = new LinearLayout(activity);
        skeletonHost.setOrientation(LinearLayout.VERTICAL);
        skeletonHost.setVisibility(View.GONE);
        layout.addView(skeletonHost);

        grid = new MultiSelectGrid(activity, isDark);
        grid.setBinder(new MultiGridBinder() {
            public void bindImage(int index, ImageView image) {
                if (index < 0 || index >= items.size()) return;
                FaceHistoryItem item = (FaceHistoryItem) items.get(index);
                image.setImageDrawable(null);
                remoteImageLoad(appendImageSize(item.headurl, imageSizeFromFlag(item.faceFlag, 140)), image, dp(activity, 96), "fh_" + item.fileid + "_thumb");
            }
        });
        grid.setListener(new MultiGridListener() {
            public void onItemClick(int index, boolean selectionMode) {
                if (selectionMode) return;
                preview(index);
            }
            public void onSelectionChanged(int selectedCount, boolean selectionMode) {
                updateStatus();
            }
        });
        grid.setBatchAction(new Runnable() {
            public void run() { batchDelete(); }
        });
        grid.setSecondaryAction(new Runnable() {
            public void run() { batchSave(); }
        });
        layout.addView(grid.getView());

        moreBtn = createButton(activity, "加载更多", pc(getSettingsThemeColor(activity, "primary")), pc(isDark ? "#1FFFFFFF" : "#11000000"), 13f, 12, 16, 10, false);
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(-1, -2);
        moreParams.topMargin = dp(activity, 8);
        moreBtn.setLayoutParams(moreParams);
        moreBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadMore(); }
        });
        moreBtn.setVisibility(View.GONE);
        layout.addView(moreBtn);
        return layout;
    }

    void setNextTimestamp(long value) { this.nextTimestamp = value; }

    void showSkeleton(boolean show) {
        if (show) {
            if (skeletonHost == null) buildSkeleton();
            if (skeletonHost == null) return;
            skeletonHost.setVisibility(View.VISIBLE);
            if (grid != null) grid.getView().setVisibility(View.GONE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
            if (moreBtn != null) moreBtn.setVisibility(View.GONE);
            if (headerImage != null) headerImage.setAlpha(0.35f);
            startShimmer(skeletonCells, makeShimmerAccents());
        } else {
            stopShimmer();
            if (skeletonHost != null) skeletonHost.setVisibility(View.GONE);
            if (grid != null) grid.getView().setVisibility(View.VISIBLE);
            if (headerImage != null) headerImage.setAlpha(1f);
        }
    }

    void buildSkeleton() {
        try {
            skeletonHost = new LinearLayout(activity);
            skeletonHost.setOrientation(LinearLayout.VERTICAL);
            int size = grid.getCellSize();
            for (int row = 0; row < 2; row++) {
                LinearLayout rowLayout = new LinearLayout(activity);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setGravity(Gravity.CENTER);
                for (int col = 0; col < 3; col++) {
                    View cell = new View(activity);
                    cell.setBackground(roundRect(pc(isDark ? "#1FFFFFFF" : "#14000000"), dp(activity, 12)));
                    LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(size, size);
                    if (col > 0) cellParams.leftMargin = dp(activity, 8);
                    rowLayout.addView(cell, cellParams);
                    skeletonCells.add(cell);
                }
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.topMargin = dp(activity, 8);
                skeletonHost.addView(rowLayout, rowParams);
            }
            skeletonFooter = new TextView(activity);
            skeletonFooter.setText("正在加载历史头像…");
            skeletonFooter.setTextSize(12);
            skeletonFooter.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            skeletonFooter.setGravity(Gravity.CENTER);
            skeletonFooter.setPadding(0, dp(activity, 14), 0, dp(activity, 6));
            skeletonHost.addView(skeletonFooter);
            skeletonHost.setVisibility(View.GONE);
        } catch (Throwable e) {
            traceLog("face_history_log", "[skeleton] 构建失败 " + e);
            skeletonHost = null;
        }
    }

    List makeShimmerAccents() {
        List accents = new ArrayList();
        if (headerImage != null) accents.add(headerImage);
        if (skeletonFooter != null) accents.add(skeletonFooter);
        return accents;
    }

    void autoLoadMore() {
        if (loading || endReached || items.isEmpty()) return;
        loadMore();
    }

    void updateStatus() {
        if (statusView == null) return;
        String text;
        if (loading) {
            text = items.isEmpty() ? "正在加载历史头像…" : "正在加载更多…";
        } else if (!lastError.isEmpty()) {
            text = lastError;
        } else if (items.isEmpty()) {
            text = endReached ? "没有历史头像" : "点「刷新」加载";
        } else {
            text = "已加载 " + items.size() + " 张" + (endReached ? "（已全部加载）" : "（还有更多）");
            if (cacheTime > 0) {
                text += " · 缓存 " + new SimpleDateFormat("HH:mm").format(new Date(cacheTime));
            }
        }
        if (grid != null && grid.isSelectionMode() && grid.getSelectedCount() > 0) {
            text += " · 已选 " + grid.getSelectedCount();
        }
        statusView.setText(text);
        if (emptyView != null) emptyView.setVisibility(items.isEmpty() && !loading ? View.VISIBLE : View.GONE);
        if (moreBtn != null) {
            if (loading && !items.isEmpty()) {
                moreBtn.setVisibility(View.VISIBLE);
                moreBtn.setText("正在加载…");
            } else if (!endReached && !items.isEmpty()) {
                moreBtn.setVisibility(View.VISIBLE);
                moreBtn.setText("加载更多");
            } else {
                moreBtn.setVisibility(View.GONE);
            }
        }
    }

    void refresh() {
        if (loading) return;
        loading = true;
        lastError = "";
        applyGrid(false);
        if (items.isEmpty()) showSkeleton(true);
        updateStatus();
        faceHistoryRequestList((System.currentTimeMillis() / 1000L), 24, new CommonCallback() {
            public void onResult(final int code, final String message, final Object data, final Object extra, final boolean flag) {
                final List newItems = (List) data;
                final long nextTimestamp = oidbNumToLong(extra, 0L);
                final boolean isEnd = flag;
                runOnUi(new Runnable() {
                    public void run() {
                        loading = false;
                        showSkeleton(false);
                        if (code == 0) {
                            items.clear();
                            if (newItems != null) {
                                for (int i = 0; i < newItems.size(); i++) addItem((FaceHistoryItem) newItems.get(i));
                            }
                            setNextTimestamp(nextTimestamp);
                            endReached = isEnd;
                            cacheTime = System.currentTimeMillis();
                            lastError = "";
                            saveCache();
                        } else {
                            lastError = "加载失败: " + message;
                            traceLog("face_history_log", "[refresh] 失败 code=" + code + " " + message);
                        }
                        applyGrid(false);
                        updateStatus();
                                            }
                });
            }
        });
    }

    void loadMore() {
        if (loading) return;
        if (endReached) {
            qqToast(1, "已经到底了");
            return;
        }
        if (nextTimestamp <= 0) {
            qqToast(1, "缺少翻页游标，请点刷新");
            return;
        }
        loading = true;
        updateStatus();
        final long oldNext = nextTimestamp;
        faceHistoryRequestList(nextTimestamp, 30, new CommonCallback() {
            public void onResult(final int code, final String message, final Object data, final Object extra, final boolean flag) {
                final List newItems = (List) data;
                final long nextTimestamp = oidbNumToLong(extra, 0L);
                final boolean isEnd = flag;
                runOnUi(new Runnable() {
                    public void run() {
                        loading = false;
                        if (code == 0) {
                            int before = items.size();
                            if (newItems != null) {
                                for (int i = 0; i < newItems.size(); i++) addItem((FaceHistoryItem) newItems.get(i));
                            }
                            setNextTimestamp(nextTimestamp);
                            endReached = isEnd;
                            if (items.size() == before && nextTimestamp == oldNext) endReached = true;
                            lastError = "";
                            saveCache();
                        } else {
                            lastError = "加载更多失败: " + message;
                        }
                        applyGrid(true);
                        updateStatus();
                                            }
                });
            }
        });
    }

    void addItem(FaceHistoryItem item) {
        if (item == null || !item.valid()) return;
        for (int i = 0; i < items.size(); i++) {
            FaceHistoryItem exist = (FaceHistoryItem) items.get(i);
            if (exist.fileid.equals(item.fileid)) {
                items.set(i, item);
                return;
            }
        }
        items.add(item);
    }

    void applyGrid(boolean append) {
        if (grid == null) return;
        int total = items.size();
        if (append && total > grid.getCount()) grid.appendItems(total);
        else grid.setItems(total);
        if (grid.isSelectionMode() && grid.getSelectedCount() == 0) grid.setSelectionMode(false);
    }

    void preview(final int index) {
        if (index < 0 || index >= items.size()) return;
        final FaceHistoryItem item = (FaceHistoryItem) items.get(index);
        if (activity == null || activity.isFinishing()) return;

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 4));

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(roundRect(pc(isDark ? "#33FFFFFF" : "#11000000"), dp(activity, 14)));
        int size = Math.min(activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 72), dp(activity, 320));
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(size, size);
        imageParams.gravity = Gravity.CENTER_HORIZONTAL;
        image.setLayoutParams(imageParams);
        content.addView(image);
        remoteImageLoad(appendImageSize(item.headurl, imageSizeFromFlag(item.faceFlag, 640)), image, size, "fh_" + item.fileid + "_preview");

        TextView info = new TextView(activity);
        StringBuilder sb = new StringBuilder();
        String shortId = item.fileid;
        if (shortId != null && shortId.length() > 24) shortId = shortId.substring(0, 24) + "…";
        sb.append("fileid: ").append(shortId).append("\n");
        sb.append("规格: ").append(imageSizeFromFlag(item.faceFlag, 640)).append(" · flag=").append(item.faceFlag);
        if (item.timestamp > 0) sb.append("\n时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(item.timestamp * 1000L)));
        info.setText(sb.toString());
        info.setTextSize(11);
        info.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        info.setPadding(0, dp(activity, 8), 0, dp(activity, 4));
        content.addView(info);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setPadding(0, dp(activity, 6), 0, 0);
        LinearLayout rowA = new LinearLayout(activity);
        rowA.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout rowB = new LinearLayout(activity);
        rowB.setOrientation(LinearLayout.HORIZONTAL);

        final AlertDialog[] dialogRef = new AlertDialog[1];
        TextView useBtn = createButton(activity, "设为头像", pc("#FFFFFFFF"), pc(getSettingsThemeColor(activity, "primary")), 13f, 12, 12, 8, false);
        useBtn.setSingleLine(true);
        useBtn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        useBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        useBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                confirm("设为当前头像", "将把这张头像重新设为当前 QQ 头像：\n" + item.fileid, new Runnable() {
                    public void run() { setAsAvatar(item); }
                });
            }
        });
        TextView saveBtn = createButton(activity, "保存到相册", pc(getSettingsThemeColor(activity, "on_surface")), pc(isDark ? "#22FFFFFF" : "#11000000"), 13f, 12, 12, 8, false);
        saveBtn.setSingleLine(true);
        saveBtn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, -2, 1f);
        saveParams.leftMargin = dp(activity, 6);
        saveBtn.setLayoutParams(saveParams);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                faceHistorySaveItem(self, item, true);
            }
        });
        rowA.addView(useBtn);
        rowA.addView(saveBtn);

        TextView copyBtn = createButton(activity, "复制链接", pc(getSettingsThemeColor(activity, "on_surface")), pc(isDark ? "#22FFFFFF" : "#11000000"), 13f, 12, 12, 8, false);
        copyBtn.setSingleLine(true);
        copyBtn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1f);
        copyParams.leftMargin = dp(activity, 6);
        copyBtn.setLayoutParams(copyParams);
        copyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                copyText(appendImageSize(item.headurl, imageSizeFromFlag(item.faceFlag, 640)));
                qqToast(2, "链接已复制");
            }
        });
        TextView delBtn = createButton(activity, "删除", pc("#FFFFFFFF"), pc("#FFE53935"), 13f, 12, 12, 8, false);
        delBtn.setSingleLine(true);
        delBtn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(0, -2, 1f);
        delParams.leftMargin = dp(activity, 6);
        delBtn.setLayoutParams(delParams);
        delBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                confirm(faceHistoryDeleteTitle(item), "删除后无法恢复：\n" + item.fileid, new Runnable() {
                    public void run() {
                        List one = new ArrayList();
                        one.add(item.fileid);
                        deleteByFileids(one);
                    }
                });
            }
        });
        rowB.addView(copyBtn);
        rowB.addView(delBtn);
        buttons.addView(rowA);
        LinearLayout.LayoutParams rowBParams = new LinearLayout.LayoutParams(-1, -2);
        rowBParams.topMargin = dp(activity, 8);
        buttons.addView(rowB, rowBParams);

        content.addView(buttons);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setView(content);
        builder.setNegativeButton("关闭", null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
        applyUiTheme(activity, dialogRef[0], 0);
    }

    void setAsAvatar(final FaceHistoryItem item) {
        qqToast(2, "正在设置…");
        faceHistoryRequestSet(item.fileid, new CommonCallback() {
            public void onResult(final int code, final String message, final Object data, final Object extra, final boolean flag) {
                final List list = (List) data;
                final long next = oidbNumToLong(extra, 0L);
                final boolean end = flag;
                runOnUi(new Runnable() {
                    public void run() {
                        if (code == 0) {
                            qqToast(2, "已提交，请回 QQ 查看头像是否更新");
                            faceHistorySetActivePage(self);
                            remoteImageLoad(qqAvatarUrl(myUin, 640), headerImage, dp(activity, 64), "fh_self");
                            refresh();
                        } else {
                            qqToast(1, "设置失败: " + message);
                        }
                                            }
                });
            }
        });
    }

    void batchDelete() {
        final List selected = grid == null ? new ArrayList() : grid.getSelected();
        if (selected.isEmpty()) {
            qqToast(1, "请先选择头像");
            return;
        }
        final List fileids = new ArrayList();
        for (int i = 0; i < selected.size(); i++) {
            int index = oidbNumToInt(selected.get(i), -1);
            if (index >= 0 && index < items.size()) fileids.add(((FaceHistoryItem) items.get(index)).fileid);
        }
        if (fileids.isEmpty()) return;
        String currentFileid = faceHistoryCurrentFileid();
        if (currentFileid != null && fileids.contains(currentFileid)) {
            confirm("删除的头像包含当前头像", "要删除的头像中包含你正在使用的头像，确定继续吗？\n\n将删除 " + fileids.size() + " 张历史头像，删除后无法恢复。", new Runnable() {
                public void run() { deleteByFileids(fileids); }
            });
            return;
        }
        confirm("批量删除历史头像", "将删除 " + fileids.size() + " 张历史头像，删除后无法恢复。", new Runnable() {
            public void run() { deleteByFileids(fileids); }
        });
    }

    void deleteByFileids(final List fileids) {
        qqToast(2, "正在删除 " + fileids.size() + " 张…");
        faceHistoryRequestDelete(fileids, new CommonCallback() {
            public void onResult(final int code, final String message, final Object data, final Object extra, final boolean flag) {
                final List list = (List) data;
                final long next = oidbNumToLong(extra, 0L);
                final boolean end = flag;
                runOnUi(new Runnable() {
                    public void run() {
                        if (code == 0) {
                            removeByFileids(fileids);
                            qqToast(2, "已删除 " + fileids.size() + " 张");
                            saveCache();
                        } else {
                            qqToast(1, "删除失败: " + message);
                        }
                        if (grid != null) grid.setSelectionMode(false);
                        applyGrid(false);
                        updateStatus();
                                            }
                });
            }
        });
    }

    void removeByFileids(List fileids) {
        if (fileids == null) return;
        for (int i = items.size() - 1; i >= 0; i--) {
            FaceHistoryItem item = (FaceHistoryItem) items.get(i);
            if (fileids.contains(item.fileid)) items.remove(i);
        }
    }

    void batchSave() {
        final List selected = grid == null ? new ArrayList() : grid.getSelected();
        if (selected.isEmpty()) {
            qqToast(1, "请先选择头像");
            return;
        }
        final List targets = new ArrayList();
        for (int i = 0; i < selected.size(); i++) {
            int index = oidbNumToInt(selected.get(i), -1);
            if (index >= 0 && index < items.size()) targets.add(items.get(index));
        }
        if (targets.isEmpty()) return;
        qqToast(2, "开始保存 " + targets.size() + " 张…");
        ThreadPool.execute(new Runnable() {
            public void run() {
                int success = 0;
                for (int i = 0; i < targets.size(); i++) {
                    FaceHistoryItem item = (FaceHistoryItem) targets.get(i);
                    byte[] data = httpBytes(appendImageSize(item.headurl, imageSizeFromFlag(item.faceFlag, 640)), 20000);
                    if (data != null && saveImageBytesToAlbum(activity, data, "face_" + item.fileid + ".png")) success++;
                }
                final int done = success;
                final int total = targets.size();
                runOnUi(new Runnable() {
                    public void run() {
                        qqToast(2, "已保存 " + done + "/" + total + " 张到相册（DCIM/QFloatingX）");
                        if (grid != null) grid.setSelectionMode(false);
                    }
                });
            }
        });
    }

    void copyText(String text) {
        try {
            android.content.ClipboardManager manager = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            manager.setPrimaryClip(android.content.ClipData.newPlainText("history_avatar", text));
        } catch (Throwable e) {
            traceLog("face_history_log", "[copy] 失败 " + e);
        }
    }

    void confirm(String titleText, String messageText, final Runnable onOk) {
        if (activity == null || activity.isFinishing()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle(titleText);
        builder.setMessage(messageText);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (onOk != null) onOk.run();
            }
        });
        builder.setNegativeButton("取消", null);
        AlertDialog dialog = builder.show();
        applyUiTheme(activity, dialog, 0);
    }

    void openOfficialPage() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(currentPackageName, "com.tencent.mobileqq.activity.QQBrowserActivity"));
            intent.putExtra("url", "https://ti.qq.com/avatarlist/public/index.html");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            qqToast(2, "已在 QQ 内打开官方历史头像页");
        } catch (Throwable e) {
            traceLog("face_history_log", "[official] QQ 内置浏览器失败，改用系统浏览器 " + e);
            try { openSettingsExternalBrowser(activity, "https://ti.qq.com/avatarlist/public/index.html"); } catch (Throwable e2) {
                qqToast(1, "打开失败: " + e2);
            }
        }
    }

    String cacheFilePath() {
        return configPath + "face_history" + "/list.json";
    }

    void ensureCacheDir() {
        try {
            File dir = new File(configPath + "face_history");
            if (!dir.exists()) dir.mkdirs();
        } catch (Throwable e) {
            traceLog("face_history_log", "[cache] 创建目录失败 " + e);
        }
    }

    void saveCache() {
        try {
            ensureCacheDir();
            JSONObject root = new JSONObject();
            root.put("time", cacheTime);
            root.put("next", nextTimestamp);
            root.put("end", endReached);
            JSONArray array = new JSONArray();
            for (int i = 0; i < items.size(); i++) {
                FaceHistoryItem item = (FaceHistoryItem) items.get(i);
                JSONObject obj = new JSONObject();
                obj.put("fileid", item.fileid);
                obj.put("headurl", item.headurl);
                obj.put("flag", item.faceFlag);
                obj.put("ts", item.timestamp);
                array.put(obj);
            }
            root.put("items", array);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFilePath());
            fos.write(root.toString().getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (Throwable e) {
            traceLog("face_history_log", "[cache] 写入失败 " + e);
        }
    }

    void loadCache() {
        try {
            File file = new File(cacheFilePath());
            if (!file.exists()) return;
            byte[] data = new byte[(int) file.length()];
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            int read = 0;
            while (read < data.length) {
                int n = fis.read(data, read, data.length - read);
                if (n < 0) break;
                read += n;
            }
            fis.close();
            JSONObject root = new JSONObject(new String(data, "UTF-8"));
            cacheTime = root.optLong("time", 0);
            nextTimestamp = root.optLong("next", 0);
            endReached = root.optBoolean("end", false);
            items.clear();
            JSONArray array = root.optJSONArray("items");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.optJSONObject(i);
                    if (obj == null) continue;
                    FaceHistoryItem item = new FaceHistoryItem();
                    item.fileid = obj.optString("fileid", "");
                    item.headurl = obj.optString("headurl", "");
                    item.faceFlag = obj.optInt("flag", 0);
                    item.timestamp = obj.optLong("ts", 0);
                    items.add(item);
                }
            }
            applyGrid(false);
            updateStatus();
        } catch (Throwable e) {
            traceLog("face_history_log", "[cache] 读取失败 " + e);
        }
    }

    void clearCache() {
        try {
            File file = new File(cacheFilePath());
            if (file.exists()) file.delete();
            items.clear();
            nextTimestamp = 0;
            endReached = false;
            cacheTime = 0;
            applyGrid(false);
            updateStatus();
        } catch (Throwable e) {
            traceLog("face_history_log", "[cache] 清理失败 " + e);
        }
    }

}

void faceHistorySaveItem(final Object pageObject, final FaceHistoryItem item, final boolean toastResult) {
    final FaceHistoryPage page = (FaceHistoryPage) pageObject;
    if (page == null || item == null) return;
    ThreadPool.execute(new Runnable() {
        public void run() {
            byte[] data = httpBytes(appendImageSize(item.headurl, imageSizeFromFlag(item.faceFlag, 640)), 20000);
            boolean ok = data != null && saveImageBytesToAlbum(page.activity, data, "face_" + item.fileid + ".png");
            traceLog("face_history_log", "[save] " + (ok ? "成功" : "失败") + " " + item.fileid);
            if (toastResult) {
                final boolean success = ok;
                page.runOnUi(new Runnable() {
                    public void run() { qqToast(success ? 2 : 1, success ? "已保存到相册（DCIM/QFloatingX）" : "保存失败"); }
                });
            }
        }
    });
}

View build历史头像Page(Object ctx) {
    Activity activity = ctx == null ? null : ctx.activity;
    if (activity == null) {
        activity = safeActivity();
    }
    if (activity == null) throw new IllegalStateException("历史头像页缺少 Activity");
    FaceHistoryPage page = new FaceHistoryPage(activity, isThemeDark(activity));
    return page.build();
}
