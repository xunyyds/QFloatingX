void registerSettingsIndexEntry(String itemKey, String itemName, String descriptionText, String type) {
    if (itemKey == null || itemKey.length() == 0) return;
    if (SettingsState.settingsItemMeta == null) SettingsState.settingsItemMeta = new HashMap();
    SettingsState.settingsItemMeta.put(itemKey, new SettingsItemMeta(
        itemName, descriptionText,
        SettingsState.settingsCurrentLevel1,
        SettingsState.settingsCurrentLevel2,
        SettingsState.settingsCurrentLevel3,
        SettingsState.settingsCurrentCategory,
        type));
}
void rebuildSettingsIndex(Activity activity) {
    if (activity == null) return;
    if (SettingsState.settingsIndexReady && SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.isEmpty()) return;
    SettingsState.settingsIndexMode = true;
    try {
        if (SettingsState.settingsItemMeta == null) SettingsState.settingsItemMeta = new HashMap();
        SettingsState.settingsItemMeta.clear();
        SettingsState.settingsCategories = new ArrayList();
        SettingsState.settingsSubPages = new ArrayList();

        SettingsState.settingsCurrentLevel1 = null;
        SettingsState.settingsCurrentLevel2 = null;
        SettingsState.settingsCurrentLevel3 = null;
        SettingsState.settingsCurrentCategory = "首页";
        buildSettingsHomeContent(activity);

        int catCount = SettingsState.settingsCategories.size();
        for (int i = 0; i < catCount; i++) {
            String cat = (String) SettingsState.settingsCategories.get(i);
            SettingsState.settingsCurrentLevel1 = cat;
            SettingsState.settingsCurrentLevel2 = null;
            SettingsState.settingsCurrentLevel3 = null;
            SettingsState.settingsCurrentCategory = null;
            buildLevel2MenuContent(activity, cat);
        }

        int subCount = SettingsState.settingsSubPages.size();
        for (int i = 0; i < subCount; i++) {
            String pathKey = (String) SettingsState.settingsSubPages.get(i);
            String[] parts = pathKey.split("\\|");
            if (parts == null || parts.length < 2) continue;
            SettingsState.settingsCurrentLevel1 = parts[0];
            SettingsState.settingsCurrentLevel2 = parts[1];
            SettingsState.settingsCurrentLevel3 = null;
            SettingsState.settingsCurrentCategory = null;
            buildLevel3MenuContent(activity, parts[0], parts[1], null);
        }
    } catch (Throwable e) {
        traceLog("setwindow_log", "[rebuildSettingsIndex] 异常: " + e);
    }
    SettingsState.settingsIndexMode = false;
    SettingsState.settingsCurrentLevel1 = null;
    SettingsState.settingsCurrentLevel2 = null;
    SettingsState.settingsCurrentLevel3 = null;
    SettingsState.settingsCurrentCategory = null;
    SettingsState.settingsIndexReady = true;
    traceLog("setwindow_log", "[rebuildSettingsIndex] 动态索引条数: " + (SettingsState.settingsItemMeta == null ? 0 : SettingsState.settingsItemMeta.size()));
}
int calcSearchScore(SettingsItemMeta meta, String query) {
    if (meta == null || meta.name == null || query == null || query.length() == 0) return 0;
    if (meta.name.toLowerCase().equals(query.toLowerCase())) return 100;
    if (meta.name.toLowerCase().startsWith(query.toLowerCase())) return 80;
    if (meta.name.toLowerCase().contains(query.toLowerCase())) return 60;
    if (meta.pinyinLetters != null && meta.pinyinLetters.contains(query.toLowerCase())) return 40;
    if (meta.description != null && meta.description.toLowerCase().contains(query.toLowerCase())) return 20;
    if (meta.searchText != null && meta.searchText.contains(query.toLowerCase())) return 10;
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
    historyContainer.setVisibility(View.GONE);
    resultsContainer.setVisibility(View.VISIBLE);
    resultsContainer.removeAllViews();
    if (SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.isEmpty()) {
        List scoreList = new ArrayList();
        List keyList = new ArrayList();
        List metaList = new ArrayList();
        java.util.List entries = new java.util.ArrayList(SettingsState.settingsItemMeta.entrySet());
        java.util.Set addedPaths = new java.util.HashSet();
        for (int i = 0; i < entries.size(); i++) {
            java.util.Map.Entry entry = (java.util.Map.Entry) entries.get(i);
            String itemKey = (String) entry.getKey();
            SettingsItemMeta meta = (SettingsItemMeta) entry.getValue();
            int score = calcSearchScore(meta, queryValue);
            if (score > 0) {
                String displayPath = meta.getDisplayPath();
                if (addedPaths.contains(displayPath)) continue;
                addedPaths.add(displayPath);
                scoreList.add(new Integer(score));
                keyList.add(itemKey);
                metaList.add(meta);
            }
        }
        for (int i = 0; i < scoreList.size(); i++) {
            int maxIdx = i;
            for (int j = i + 1; j < scoreList.size(); j++) {
                if (((Integer)scoreList.get(j)) > ((Integer)scoreList.get(maxIdx))) {
                    maxIdx = j;
                }
            }
            if (maxIdx != i) {
                Object tmpScore = scoreList.get(i);
                scoreList.set(i, scoreList.get(maxIdx));
                scoreList.set(maxIdx, tmpScore);
                Object tmpKey = keyList.get(i);
                keyList.set(i, keyList.get(maxIdx));
                keyList.set(maxIdx, tmpKey);
                Object tmpMeta = metaList.get(i);
                metaList.set(i, metaList.get(maxIdx));
                metaList.set(maxIdx, tmpMeta);
            }
        }
        for (int i = 0; i < metaList.size(); i++) {
            String itemKey = (String) keyList.get(i);
            SettingsItemMeta meta = (SettingsItemMeta) metaList.get(i);
            String displayPath = meta.getDisplayPath();
            String pathOnly = displayPath;
            if (meta.name != null && pathOnly.endsWith(meta.name)) {
                pathOnly = pathOnly.substring(0, pathOnly.length() - meta.name.length());
            }
            if (pathOnly.endsWith(" > ")) {
                pathOnly = pathOnly.substring(0, pathOnly.length() - 3);
            }
            addSearchResultItem(ctx, resultsContainer, meta.name != null ? meta.name : displayPath, pathOnly, queryValue, itemKey, meta.level1, meta.level2, meta.level3, isDark);
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
String getPinyinFirstLetters(String textValue) {
    if (textValue == null) return "";
    StringBuilder builder = new StringBuilder();
    for (char character : textValue.toCharArray()) {
        String letter = getPinyinFirstLetter(character);
        if (letter != null && !letter.isEmpty()) {
            builder.append(letter.toLowerCase());
        }
    }
    return builder.toString();
}
String getPinyinFirstLetter(char character) {
    if (character < 0x4E00 || character > 0x9FA5) {
        return String.valueOf(character);
    }
    try {
        byte[] bytes = String.valueOf(character).getBytes("GB2312");
        if (bytes.length < 2) {
            return String.valueOf(character);
        }
        int code = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
        int[] boundaries = SettingsState.pinyinBoundaries;
        String[] letters = SettingsState.pinyinLetters;
        for (int i = boundaries.length - 1; i >= 0; i--) {
            if (code >= boundaries[i]) {
                return letters[i];
            }
        }
    } catch (Throwable exception) {
        traceLog("setwindow_log", "[getPinyinFirstLetter] 获取拼音首字母: " + exception.getMessage());
    }
    return String.valueOf(character);
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

    final String finalKey = itemKey;
    final int[] retryCount = new int[]{0};

    uiHandler.post(new Runnable() {
        public void run() {
            try {
                if (SettingsState.settingsItemViews == null || !SettingsState.settingsItemViews.containsKey(finalKey)) {
                    retryCount[0]++;
                    if (retryCount[0] < 20) {
                        uiHandler.postDelayed(this, 100);
                    } else {
                        traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: itemKey 未找到: " + finalKey);
                    }
                    return;
                }

                View targetView = (View) SettingsState.settingsItemViews.get(finalKey);
                if (targetView == null) {
                    traceLog("setwindow_log", "[scrollToAndHighlight] 滚动高亮: targetView 为空");
                    return;
                }

                if (SettingsState.settingsScrollView == null) {
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
                SettingsState.settingsScrollView.getLocationOnScreen(scrollLocation);
                final int scrollY = SettingsState.settingsScrollView.getScrollY() + location[1] - scrollLocation[1] - dp(act, 28);
                SettingsState.settingsScrollView.smoothScrollTo(0, Math.max(0, scrollY));

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
    traceLog("setwindow_log", "[handleSearchResultClick] key=" + itemKey + " l1=" + targetLevel1 + " l2=" + targetLevel2 + " l3=" + level3);

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
void addSearchResultItem(final Activity activity, LinearLayout container, String itemTitle, String itemPath, String queryValue, final String itemKey, final String level1, final String level2, final String level3, boolean isDark) {
    if (activity == null || container == null || itemTitle == null || queryValue == null) return;

    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
    card.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), dp(activity, 14)));
    card.setClipToOutline(true);
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
    cardParams.setMargins(0, 0, 0, dp(activity, 8));
    card.setLayoutParams(cardParams);

    LinearLayout textArea = new LinearLayout(activity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

    SpannableString spannable = new SpannableString(itemTitle);
    int start = itemTitle.toLowerCase().indexOf(queryValue.toLowerCase());
    if (start >= 0) {
        spannable.setSpan(new ForegroundColorSpan(pc(getSettingsThemeColor(activity, "primary"))), start, start + queryValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    TextView titleView = new TextView(activity);
    titleView.setText(spannable);
    titleView.setTextSize(15);
    titleView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    textArea.addView(titleView);

    if (itemPath != null && itemPath.length() > 0) {
        TextView pathView = new TextView(activity);
        pathView.setText(itemPath);
        pathView.setTextSize(12);
        pathView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        pathView.setPadding(0, dp(activity, 3), 0, 0);
        textArea.addView(pathView);
    }
    card.addView(textArea);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(18);
    arrowView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
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
