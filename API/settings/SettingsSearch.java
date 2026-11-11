void registerSettingsIndexEntry(Object ctx, String itemKey, String itemName, String descriptionText, String type) {
    if (ctx == null || itemKey == null || itemKey.length() == 0) return;
    ctx.itemMeta.put(itemKey, new SettingsItemMeta(itemName, descriptionText,
        ctx.level1, ctx.level2, ctx.level3, ctx.currentCategory, type));
}
void settingsCatalogEntry(Map entries, String l1, String l2, String category,
        String key, String name, String description, String type, String targetPage) {
    SettingsItemMeta meta = new SettingsItemMeta(name, description, l1, l2, null, category, type);
    meta.targetPage = targetPage;
    if (entries.containsKey(key)) throw new IllegalStateException("重复设置索引: " + key);
    entries.put(key, meta);
}
void settingsCatalogItems(Map entries, String l1, String l2, String category, String[][] items) {
    for (int i = 0; i < items.length; i++) {
        String[] item = items[i];
        // Keep the UI's path-based keys without consulting mutable page state.
        String key = (l1 == null ? "" : l1 + "_") + (l2 == null ? "" : l2 + "_")
            + (category == null ? "" : category + "_") + item[0];
        settingsCatalogEntry(entries, l1, l2, category, key, item[0], item[1], item[2], null);
    }
}
void rebuildSettingsIndex(Activity activity) {
    if (activity == null) return;
    synchronized (SettingsState.settingsIndexLock) {
        if (SettingsState.settingsIndexReady && SettingsState.settingsItemMeta != null
                && !SettingsState.settingsItemMeta.isEmpty()) return;
        try {
            // Build the full catalog locally: no views, callbacks, factories or page registry.
            Map entries = new HashMap();
            String[][] categories = {
                {"悬浮窗与快捷", "悬浮窗开关 · 图标与大小"},
                {"消息与互动", "消息统计 · 双击消息"},
                {"定位与网络", "模拟定位 · 经纬度 · 空间操作"},
                {"功能扩展", "功能热插拔 · 脚本 · 模块"},
                {"界面与显示", "主题 · 背景 · 字体 · 欢迎语"},
                {"提示与通知", "Toast · 加载提示 · 常驻通知"},
                {"性能与调试", "线程池 · 运行状态 · 日志"},
                {"输入框提示", "开关 · 提示词 · 可用变量"},
                {"关于", "更新 · 版本 · 重置"}
            };
            for (int i = 0; i < categories.length; i++) {
                settingsCatalogEntry(entries, null, null, "首页", "cat_" + categories[i][0],
                    categories[i][0], categories[i][1], "category", null);
            }
            // Each row explicitly records the containing category and destination page.
            String[][] pages = {
                {null, "首页", "item_html", "HTML 浏览器", "本地网页 · 文件管理", "HTML 浏览器"},
                {"悬浮窗与快捷", "开关", "item_float_window", "悬浮窗设置", "图标 · 大小 · 灵敏度 · 透明度", "悬浮窗设置"},
                {"消息与互动", "开关", "item_msg_stats", "消息统计", "查看今日 / 累计数据", "消息统计"},
                {"消息与互动", "开关", "item_msg_stats_settings", "消息统计设置", "统计范围 · 类别 · 数据操作", "消息统计设置"},
                {"消息与互动", "开关", "item_revoke_mark", "撤回标记", "重定向已撤回提示 · 文本 · 位置 · 颜色", "撤回标记"},
                {"定位与网络", "经纬度", "item_qzone_op", "空间操作", "秒赞 · 秒评 · 评论内容", "空间操作"},
                {"功能扩展", null, "item_hotplug", "功能热插拔", "脚本扩展 · 独立调度", "功能热插拔"},
                {"界面与显示", "界面", "item_basic_mode", "基础模式", "主题、弹窗大小、振动反馈", "基础模式"},
                {"界面与显示", "界面", "item_bg_icon", "背景样式", "背景类型、颜色、图片", "背景样式"},
                {"界面与显示", "界面", "item_font_style", "字体样式", "字体风格、大小、颜色", "字体样式"},
                {"提示与通知", "开关", "item_toast_hint", "toast设置", "吐司提示开关与样式", "提示"},
                {"性能与调试", null, "item_thread_pool", "线程池", "优先级、队列、满载策略", "线程池"},
                {"性能与调试", null, "item_runtime_status", "运行状态", "脚本与宿主信息", "运行状态"},
                {"性能与调试", null, "item_debug", "调试", "预览设置 · 日志清理阈值", "调试"}
            };
            for (int i = 0; i < pages.length; i++) {
                String[] page = pages[i];
                settingsCatalogEntry(entries, page[0], null, page[1], page[2], page[3], page[4], "page", page[5]);
            }
            settingsCatalogItems(entries, "悬浮窗与快捷", null, "开关", new String[][] {
                {"悬浮窗", "悬浮球显示", "switch"}
            });
            settingsCatalogItems(entries, "消息与互动", null, "开关", new String[][] {
                {"消息统计", "今日消息计数", "switch"}, {"双击消息", "预览页拦截增强", "switch"}
            });
            settingsCatalogItems(entries, "定位与网络", null, "开关", new String[][] {
                {"模拟定位", "全局定位覆盖", "switch"}
            });
            settingsCatalogEntry(entries, "定位与网络", null, "经纬度", "loc_lng", "经度", "模拟定位经度", "input", null);
            settingsCatalogEntry(entries, "定位与网络", null, "经纬度", "loc_lat", "纬度", "模拟定位纬度", "input", null);
            settingsCatalogEntry(entries, "功能扩展", null, null, "e2", "Java脚本", "QFun 脚本列表", "click", null);
            settingsCatalogEntry(entries, "功能扩展", null, null, "e4", "模块设置", "QFun 模块页", "click", null);
            settingsCatalogEntry(entries, "功能扩展", null, null, "e5", "取消 / 重载", "卸载或重新加载脚本", "click", null);
            settingsCatalogItems(entries, "界面与显示", null, "首页", new String[][] {
                {"欢迎语", "首页顶部显示的文字，留空则显示「欢迎使用」", "input"}
            });
            settingsCatalogItems(entries, "提示与通知", null, "开关", new String[][] {
                {"加载提示", "脚本加载时显示提示", "switch"}, {"加载通知", "脚本加载时发送通知提示", "switch"},
                {"常驻通知", "后台常驻「运行中」通知", "switch"}
            });
            settingsCatalogItems(entries, "输入框提示", null, "开关", new String[][] {
                {"输入框提示", "聊天输入框显示自定义提示词", "switch"}
            });
            settingsCatalogItems(entries, "输入框提示", null, "提示词", new String[][] {
                {"设置输入框提示词", "支持 #变量# 和 ##链接##", "input"}
            });
            settingsCatalogEntry(entries, "关于", null, null, "a1", "检查更新", "手动检测最新版本", "click", null);
            settingsCatalogEntry(entries, "关于", null, null, "a2", "查看更新日志", "查看历史更新记录", "click", null);
            settingsCatalogItems(entries, "关于", null, "更新", new String[][] {
                {"更新通道", null, "choice"}, {"接收测试版更新提示", "接收测试版更新的自动提示", "switch"}
            });
            settingsCatalogItems(entries, "关于", null, "其他", new String[][] {
                {"配置备份与导入", "备份全部自定义配置与热插拔脚本，导入前预览", "click"},
                {"重置所有设置", "恢复默认设置", "click"}
            });
            settingsCatalogItems(entries, "界面与显示", "基础模式", "基础模式", new String[][] {
                {"主题模式", null, "choice"}, {"弹窗宽度", "单位 dp，留空为自适应，默认最大 260dp", "input"},
                {"弹窗高度", "单位 dp，留空为自适应内容", "input"}, {"显示密度", null, "choice"},
                {"自定义 DPI", "范围 160-640，越大显示越大", "input"}, {"振动反馈", null, "switch"},
                {"背景模糊", "系统模糊窗体后方内容(Android 12+)", "switch"},
                {"弹窗圆角", "设置弹窗圆角半径", "click"}, {"预览弹窗", "按当前宽度/高度/显示密度预览", "click"}
            });
            settingsCatalogItems(entries, "界面与显示", "背景样式", "背景样式", new String[][] {
                {"背景类型", null, "choice"}, {"选择背景图片", "点击选择本地图片", "click"},
                {"图片模糊 (0-25)", "0为不模糊", "input"}, {"遮罩浓度 (0-255)", "越大越暗", "input"}
            });
            // Index both theme variants and all conditional controls, not just the current UI.
            String[] suffixes = {" (浅色模式)", " (深色模式)"};
            String[] fontKeys = {"ui_text_color_light", "ui_text_color_dark"};
            for (int i = 0; i < suffixes.length; i++) {
                String suffix = suffixes[i];
                settingsCatalogItems(entries, "界面与显示", "背景样式", "背景样式", new String[][] {
                    {"预设颜色" + suffix, "点击选择内置配色", "click"},
                    {"背景颜色列表" + suffix, "多色轮转，1色常驻", "click"},
                    {"预设渐变" + suffix, "点击选择内置渐变", "click"},
                    {"渐变颜色列表" + suffix, "2~3色，复用调色盘列表", "click"}
                });
                settingsCatalogEntry(entries, "界面与显示", "字体样式", "字体样式", fontKeys[i],
                    "字体颜色" + suffix, "留空自动配色 (推荐)", "color", null);
            }
            settingsCatalogItems(entries, "界面与显示", "字体样式", "字体样式", new String[][] {
                {"字体风格", null, "choice"}, {"字体大小", null, "choice"}
            });
            settingsCatalogItems(entries, "提示与通知", "提示", "Toast样式", new String[][] {
                {"样式", null, "choice"}, {"文字轮换颜色", "1色常驻，多色轮换", "click"},
                {"背景轮换颜色", "纯色背景，1色常驻", "click"}, {"渐变背景颜色", "2色起，最多3色", "click"},
                {"弹出时长(ms)", "500-10000", "input"}
            });
            settingsCatalogItems(entries, "提示与通知", "提示", "Toast位置", new String[][] {
                {"弹出位置", null, "choice"}, {"自定义坐标/宽高", "拖主体移动，拖边角改大小", "click"},
                {"自适应", "开：气泡贴合内容+框内位置；关：气泡撑满框+文字填充", "switch"},
                {"气泡在框内位置", null, "choice"}, {"文字填充", null, "choice"}
            });
            settingsCatalogItems(entries, "提示与通知", "提示", "Toast文字", new String[][] {
                {"前缀", "显示在文字前", "input"}, {"后缀", "显示在文字后", "input"}, {"测试Toast", "预览当前配置", "click"}
            });
            settingsCatalogItems(entries, "性能与调试", "线程池", "线程池", new String[][] {
                {"线程优先级", null, "choice"}, {"任务队列容量", "默认50", "input"},
                {"核心线程存活(秒)", "默认30", "input"}, {"任务满载策略", null, "choice"}
            });
            settingsCatalogItems(entries, "悬浮窗与快捷", "悬浮窗设置", "悬浮窗设置", new String[][] {
                {"更换图标", "选择静态图标或动态图标 (GIF)", "click"}, {"悬浮窗大小", "默认48", "input"},
                {"关闭图标大小", "默认24", "input"}, {"拖拽灵敏度", "数值越小越灵敏", "input"},
                {"长按关闭阈值", "默认650ms", "input"}, {"图标透明度", "0-255", "input"},
                {"GIF播放速度", "带实时预览，越小越快", "click"}
            });
            settingsCatalogItems(entries, "消息与互动", "撤回标记", "撤回标记", new String[][] {
                {"启用消息视图监听", "关闭后不再注册消息视图监听，可避免部分宿主缺少 AIOViewUpdateListener 导致的加载失败", "switch"},
                {"已撤回标记", "把模块内置的「已撤回」提示挪到消息时间后面", "switch"},
                {"标记文本", "默认「已撤回」，追加在消息时间后面", "input"}, {"标记位置", null, "choice"},
                {"标记颜色", null, "choice"}, {"自定义颜色", "设置已撤回标记的自定义颜色", "click"}
            });
            settingsCatalogItems(entries, "消息与互动", "撤回标记", "模块开关", new String[][] {
                {"显示消息时间", "QFun 模块：气泡显示消息发送时间", "switch"},
                {"防撤回", "QFun 模块：拦截撤回指令并保留消息", "switch"}
            });
            settingsCatalogItems(entries, "性能与调试", "调试", "调试", new String[][] {
                {"预览设置", "预览当前设置效果", "click"},
                {"日志大小阈值(MB)", "日志文件夹超过此大小自动清理", "input"}
            });
            settingsCatalogItems(entries, "消息与互动", "消息统计设置", "统计范围", new String[][] {
                {"按好友/群聊单独统计", "开启后仅白名单内的群/好友单独统计，其他仍汇总", "switch"},
                {"群聊白名单", "选择单独统计的群聊，需先开启上方开关", "click"},
                {"好友白名单", "选择单独统计的好友，需先开启上方开关", "click"}
            });
            settingsCatalogItems(entries, "消息与互动", "消息统计设置", "收发总开关", new String[][] {
                {"接收统计", "关闭后不统计任何接收数据", "switch"}, {"发送统计", "关闭后不统计任何发送数据", "switch"}
            });
            for (int i = 0; i < STAT_CATEGORIES.length; i++) {
                settingsCatalogItems(entries, "消息与互动", "消息统计设置", "统计类别", new String[][] {
                    {STAT_CATEGORIES[i][1], null, "switch"}
                });
            }
            settingsCatalogItems(entries, "消息与互动", "消息统计设置", "每日目标", new String[][] {
                {"每日消息目标", "默认 100", "input"}
            });
            settingsCatalogItems(entries, "消息与互动", "消息统计设置", "数据操作", new String[][] {
                {"重置今日数据", "清空今日所有统计", "click"}, {"重置累计数据", "清空所有累计统计", "click"},
                {"修复数据", "重算所有 total 键", "click"}
            });
            settingsCatalogItems(entries, "定位与网络", "空间操作", "开关", new String[][] {
                {"秒赞", "自动给好友说说点赞", "switch"}, {"秒评", "自动评论好友说说", "switch"}
            });
            settingsCatalogItems(entries, "定位与网络", "空间操作", "评论内容", new String[][] {
                {"评论内容", "自动评论使用的文字（≤100 字）", "input"}
            });
            settingsCatalogItems(entries, "定位与网络", "空间操作", "间隔设置", new String[][] {
                {"拉取列表后延迟", "毫秒，建议 3000~15000", "input"}, {"每条说说间隔", "毫秒，建议 500~5000", "input"}
            });
            settingsCatalogItems(entries, "定位与网络", "空间操作", "黑名单", new String[][] {
                {"设置黑名单", null, "choice"}
            });
            // Publish only a complete snapshot; a failed rebuild retains the previous map.
            SettingsState.settingsItemMeta = entries;
            SettingsState.settingsIndexReady = true;
        } catch (Throwable e) {
            SettingsState.settingsIndexReady = false;
            traceLog("setwindow_log", "[rebuildSettingsIndex] 异常: " + e);
        }
    }
}
class SettingsSearchPalette {
    final int cardBg;
    final int ripple;
    final int primary;
    final int onSurface;
    final int onSurfaceVariant;
    SettingsSearchPalette(Activity activity) {
        cardBg = getAdaptiveCardBg(activity);
        ripple = pc(getSettingsThemeColor(activity, "ripple"));
        primary = pc(getSettingsThemeColor(activity, "primary"));
        onSurface = pc(getSettingsThemeColor(activity, "on_surface"));
        onSurfaceVariant = pc(getSettingsThemeColor(activity, "on_surface_variant"));
    }
}
private Object[] settingsSearchSnapshotCache = null;
List settingsSearchSnapshot() {
    Map source = SettingsState.settingsItemMeta;
    Object[] cache = settingsSearchSnapshotCache;
    if (cache != null && cache[0] == source) return (List) cache[1];
    List rows = new ArrayList(source.size());
    java.util.Iterator iterator = source.entrySet().iterator();
    while (iterator.hasNext()) {
        java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
        SettingsItemMeta meta = (SettingsItemMeta) entry.getValue();
        rows.add(new Object[] {entry.getKey(), meta,
            meta.name != null ? meta.name.toLowerCase() : "",
            meta.description != null ? meta.description.toLowerCase() : null});
    }
    settingsSearchSnapshotCache = new Object[] {source, rows};
    return rows;
}
int calcSearchScore(SettingsItemMeta meta, String nameLower, String descriptionLower, String queryLower) {
    if (meta == null || meta.name == null || queryLower == null || queryLower.length() == 0) return 0;
    if (nameLower.equals(queryLower)) return 100;
    if (nameLower.startsWith(queryLower)) return 80;
    if (nameLower.contains(queryLower)) return 60;
    if (meta.pinyinLetters != null && meta.pinyinLetters.contains(queryLower)) return 40;
    if (descriptionLower != null && descriptionLower.contains(queryLower)) return 20;
    if (meta.searchText != null && meta.searchText.contains(queryLower)) return 10;
    return 0;
}
void hideSearchInputKeyboard(Activity ctx, View inputView) {
    if (ctx == null || inputView == null) return;
    InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
        imm.hideSoftInputFromWindow(inputView.getWindowToken(), 0);
    }
}
void doSearch(String queryValue, LinearLayout historyContainer, LinearLayout resultsContainer, Activity ctx, boolean isDark) {
    if (queryValue == null || queryValue.isEmpty()) {
        lastSearchQuery = "";
        historyContainer.setVisibility(View.VISIBLE);
        resultsContainer.setVisibility(View.GONE);
        return;
    }
    if (queryValue.equals(lastSearchQuery)) return;
    lastSearchQuery = queryValue;
    rebuildSettingsIndex(ctx);
    historyContainer.setVisibility(View.GONE);
    resultsContainer.setVisibility(View.VISIBLE);
    resultsContainer.removeAllViews();
    if (SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.isEmpty()) {
        String queryLower = queryValue.toLowerCase();
        List rows = settingsSearchSnapshot();
        int total = rows.size();
        String[] keyList = new String[total];
        Object[] metaList = new Object[total];
        String[] pathList = new String[total];
        int[] scoreList = new int[total];
        java.util.Set addedPaths = new java.util.HashSet();
        int matched = 0;
        for (int i = 0; i < total; i++) {
            Object[] row = (Object[]) rows.get(i);
            SettingsItemMeta meta = (SettingsItemMeta) row[1];
            int score = calcSearchScore(meta, (String) row[2], (String) row[3], queryLower);
            if (score <= 0) continue;
            String displayPath = meta.getDisplayPath();
            if (!addedPaths.add(displayPath)) continue;
            keyList[matched] = (String) row[0];
            metaList[matched] = meta;
            pathList[matched] = displayPath;
            scoreList[matched] = score;
            matched++;
        }
        for (int i = 1; i < matched; i++) {
            String pendingKey = keyList[i];
            Object pendingMeta = metaList[i];
            String pendingPath = pathList[i];
            int pendingScore = scoreList[i];
            int j = i - 1;
            while (j >= 0 && scoreList[j] < pendingScore) {
                keyList[j + 1] = keyList[j];
                metaList[j + 1] = metaList[j];
                pathList[j + 1] = pathList[j];
                scoreList[j + 1] = scoreList[j];
                j--;
            }
            keyList[j + 1] = pendingKey;
            metaList[j + 1] = pendingMeta;
            pathList[j + 1] = pendingPath;
            scoreList[j + 1] = pendingScore;
        }
        SettingsSearchPalette palette = new SettingsSearchPalette(ctx);
        for (int i = 0; i < matched; i++) {
            SettingsItemMeta meta = (SettingsItemMeta) metaList[i];
            String displayPath = pathList[i];
            String pathOnly = displayPath;
            if (meta.name != null && pathOnly.endsWith(meta.name)) {
                pathOnly = pathOnly.substring(0, pathOnly.length() - meta.name.length());
            }
            if (pathOnly.endsWith(" > ")) {
                pathOnly = pathOnly.substring(0, pathOnly.length() - 3);
            }
            addSearchResultItem(ctx, resultsContainer, meta.name != null ? meta.name : displayPath, pathOnly, queryValue, queryLower, keyList[i], meta.level1, meta.level2, meta.level3, isDark, palette);
        }
    }
    if (resultsContainer.getChildCount() == 0) {
        TextView noResult = new TextView(ctx);
        noResult.setText("未找到相关设置");
        noResult.setTextSize(14);
        noResult.setTextColor(pc(isDark ? "#99FFFFFF" : "#99000000"));
        noResult.setGravity(Gravity.CENTER);
        noResult.setPadding(dp(ctx, 4), dp(ctx, 24), dp(ctx, 4), dp(ctx, 24));
        resultsContainer.addView(noResult);
    }
}
void highlightSettingsItem(final View targetView) {
    if (targetView == null) return;

    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    boolean isDark = isThemeDark(activity);

    SettingsState.settingsHighlightView = targetView;
    final GradientDrawable highlightBg = new GradientDrawable();
    highlightBg.setColor(pc(isDark ? "#338AB4F8" : "#332196F3"));
    highlightBg.setCornerRadius(dp(activity, 16));

    targetView.setBackground(highlightBg);

    uiHandler.postDelayed(new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                targetView.setBackground(makeFeedbackBg(getAdaptiveCardBg(act), pc(getSettingsThemeColor(act, "ripple")), 0));
            }

            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (SettingsState.settingsHighlightView == targetView) targetView.setBackground(highlightBg);

                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (SettingsState.settingsHighlightView == targetView) {
                                Activity act2 = getSettingsCurrentActivity();
                                if (act2 != null) {
                                    targetView.setBackground(makeFeedbackBg(getAdaptiveCardBg(act2), pc(getSettingsThemeColor(act2, "ripple")), 0));
                                }
                                SettingsState.settingsHighlightView = null;
                            }
                        }
                    }, 350);
                }
            }, 150);
        }
    }, 350);
}
void scrollToAndHighlight(String itemKey) {
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) {
        traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: activity 为空");
        return;
    }
    final Object pageCtx = (activity instanceof SettingsActivity) ? ((SettingsActivity) activity).settingsTopPage() : null;
    if (pageCtx == null) return;

    final String finalKey = itemKey;
    final int[] retryCount = new int[]{0};

    uiHandler.post(new Runnable() {
        public void run() {
            try {
                if (pageCtx.disposed || !pageCtx.itemViews.containsKey(finalKey)) {
                    retryCount[0]++;
                    if (retryCount[0] < 20) {
                        uiHandler.postDelayed(this, 100);
                    } else {
                        traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: itemKey 未找到: " + finalKey);
                    }
                    return;
                }

                View targetView = (View) pageCtx.itemViews.get(finalKey);
                if (targetView == null) {
                    traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: targetView 为空");
                    return;
                }

                if (pageCtx.scrollView == null) {
                    traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: scrollView 为空");
                    return;
                }

                Activity act = getSettingsCurrentActivity();
                if (act == null) {
                    traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: activity 为空");
                    return;
                }

                final int[] location = new int[2];
                targetView.getLocationOnScreen(location);
                if (targetView.getHeight() <= 0 || (location[0] == 0 && location[1] == 0)) {
                    retryCount[0]++;
                    if (retryCount[0] < 20) {
                        uiHandler.postDelayed(this, 100);
                    }
                    return;
                }

                final View fTarget = targetView;
                int[] scrollLocation = new int[2];
                pageCtx.scrollView.getLocationOnScreen(scrollLocation);
                final int scrollY = pageCtx.scrollView.getScrollY() + location[1] - scrollLocation[1] - dp(act, 28);
                pageCtx.scrollView.smoothScrollTo(0, Math.max(0, scrollY));

                uiHandler.postDelayed(new Runnable() {
                    public void run() {
                        highlightSettingsItem(fTarget);
                    }
                }, 400);
            } catch (Throwable exception) {
                traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮 错误: " + exception.getMessage());
            }
        }
    });
}
void handleSearchResultClick(final Activity activity, final String itemKey, final String level1, final String level2, final String level3, final String query) {
    if (activity == null) return;
    try { vibrate(activity, 32); } catch (Throwable ignore) {}

    SettingsItemMeta meta = SettingsState.settingsItemMeta == null ? null : (SettingsItemMeta) SettingsState.settingsItemMeta.get(itemKey);
    boolean isCategory = meta != null && "category".equals(meta.type);
    boolean isPage = meta != null && "page".equals(meta.type);
    String targetLevel1 = isCategory ? meta.name : level1;
    String targetLevel2 = isPage ? meta.targetPage : level2;
    String highlightKey = (isCategory || isPage) ? null : itemKey;

    if (activity instanceof SettingsActivity) {
        final SettingsActivity settingsAct = (SettingsActivity) activity;
        SettingsState.settingsNavLevel1 = targetLevel1;
        SettingsState.settingsNavLevel2 = targetLevel2;
        SettingsState.settingsNavLevel3 = level3;
        SettingsState.settingsNavHighlightKey = highlightKey;
        SettingsState.settingsNavQuery = query;
        try {
            if (settingsAct.settingsPageHandler != null) {
                settingsAct.settingsPageHandler.post(new Runnable() {
                    public void run() { settingsAct.settingsRunNav(); }
                });
            } else {
                settingsAct.settingsRunNav();
            }
        } catch (Throwable e) {
            traceLog("setwindow_log", "[handleSearchResultClick] 导航失败: " + e);
        }
        return;
    }

    launchSettingsActivity(activity, 0, "", "");
}
void addSearchResultItem(final Activity activity, LinearLayout container, String itemTitle, String itemPath, String queryValue, String queryLower, final String itemKey, final String level1, final String level2, final String level3, boolean isDark, SettingsSearchPalette palette) {
    if (activity == null || container == null || itemTitle == null || queryValue == null) return;

    int padH = dp(activity, 16);
    int padV = dp(activity, 14);
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(padH, padV, padH, padV);
    card.setBackground(makeFeedbackBg(palette.cardBg, palette.ripple, dp(activity, 14)));
    card.setClipToOutline(true);
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
    cardParams.setMargins(0, 0, 0, dp(activity, 8));
    card.setLayoutParams(cardParams);

    LinearLayout textArea = new LinearLayout(activity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

    SpannableString spannable = new SpannableString(itemTitle);
    int start = itemTitle.toLowerCase().indexOf(queryLower);
    if (start >= 0) {
        spannable.setSpan(new ForegroundColorSpan(palette.primary), start, start + queryValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    TextView titleView = new TextView(activity);
    titleView.setText(spannable);
    titleView.setTextSize(15);
    titleView.setTextColor(palette.onSurface);
    textArea.addView(titleView);

    if (itemPath != null && itemPath.length() > 0) {
        TextView pathView = new TextView(activity);
        pathView.setText(itemPath);
        pathView.setTextSize(12);
        pathView.setTextColor(palette.onSurfaceVariant);
        pathView.setPadding(0, dp(activity, 3), 0, 0);
        textArea.addView(pathView);
    }
    card.addView(textArea);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(18);
    arrowView.setTextColor(palette.onSurfaceVariant);
    card.addView(arrowView);

    card.setClickable(true);
    card.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            handleSearchResultClick(activity, itemKey, level1, level2, level3, queryValue);
        }
    });
    container.addView(card);
}
void resetSettingsSearchState() {
    lastSearchQuery = "";
    SettingsState.settingsSearchToken = 0;
}
