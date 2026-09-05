
class SettingsItemMeta {
    String name;
    String description;
    String level1;
    String level2;
    String level3;
    String category;
    String type;
    String searchText;
    String pinyinLetters;
    SettingsItemMeta(String name, String description, String level1, String level2, String level3, String category, String type) {
        this.name = name;
        this.description = description;
        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.category = category;
        this.type = type;
        this.pinyinLetters = name != null ? getPinyinFirstLetters(name) : "";
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name).append(" ");
        if (description != null) sb.append(description).append(" ");
        if (level1 != null) sb.append(level1).append(" ");
        if (level2 != null) sb.append(level2).append(" ");
        if (category != null) sb.append(category).append(" ");
        sb.append(this.pinyinLetters);
        this.searchText = sb.toString().toLowerCase();
    }
    String getDisplayPath() {
        StringBuilder sb = new StringBuilder();
        if (level1 != null) sb.append(level1);
        if (level2 != null) { if (sb.length() > 0) sb.append(" > "); sb.append(level2); }
        if (category != null) { if (sb.length() > 0) sb.append(" > "); sb.append(category); }
        if (sb.length() > 0) sb.append(" > ");
        sb.append(name);
        return sb.toString();
    }
}
class SettingsState {
    static LinearLayout settingsListContainer;
    static Map settingsCategoryContainers;
    static Map settingsItemTextViews;
    static List settingsDialogStack;
    static View settingsHighlightView;
    static Map settingsItemViews;
    static ScrollView settingsScrollView;
    static Dialog settingsSearchDialog;
    static String settingsPendingHighlightKey;
    static Activity settingsCurrentActivityRef;
    static Map settingsItemMeta;
    static String settingsCurrentLevel1;
    static String settingsCurrentLevel2;
    static String settingsCurrentLevel3;
    static String settingsCurrentCategory;
    static boolean settingsIndexBuilt;
    static long settingsSearchToken;
    static int[] pinyinBoundaries = {
        0xB0A1, 0xB0C5, 0xB2C1, 0xB4EE, 0xB6EA, 0xB7A2, 0xB8C1, 0xB9FE,
        0xBBF7, 0xBFA6, 0xC0AC, 0xC2E8, 0xC4C3, 0xC5B6, 0xC5BE, 0xC6DA,
        0xC8BB, 0xC8F6, 0xCBFA, 0xCDDA, 0xCEF4, 0xD1B9, 0xD4D1
    };
    static String[] pinyinLetters = {
        "a", "b", "c", "d", "e", "f", "g", "h", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "w", "x", "y", "z"
    };
}

Activity getSettingsCurrentActivity() {
    Activity activity = getNowActivity();
    if (activity != null) {
        return activity;
    }
    if (SettingsState.settingsCurrentActivityRef != null && !SettingsState.settingsCurrentActivityRef.isFinishing()) {
        return SettingsState.settingsCurrentActivityRef;
    }
    return null;
}

String getSettingsThemeColor(Activity activity, String colorName) {
    boolean isDarkTheme = isThemeDark(activity);
    String customBg = getString("settings", isDarkTheme ? "ui_bg_color_dark" : "ui_bg_color_light", "");
    String customText = getString("settings", isDarkTheme ? "ui_text_color_dark" : "ui_text_color_light", "");
    boolean hasCustomBg = customBg != null && !customBg.isEmpty() && isValidHexColor(customBg);
    boolean hasCustomText = customText != null && !customText.isEmpty() && isValidHexColor(customText);
    String autoTextColor = isDarkTheme ? "#FFEFEFEF" : "#FF1A1A1A";
    if (!hasCustomText && hasCustomBg) {
        try {
            int bgColor = Color.parseColor(customBg);
            double luminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255.0;
            autoTextColor = luminance > 0.5 ? "#FF1A1A1A" : "#FFEFEFEF";
        } catch (Throwable e) {}
    }
    switch (colorName) {
        case "surface": return hasCustomBg ? customBg : (isDarkTheme ? "#FF1A1A1A" : "#FFF5F5F5");
        case "background": return hasCustomBg ? customBg : (isDarkTheme ? "#FF000000" : "#FFFFFFFF");
        case "on_surface": return hasCustomText ? customText : autoTextColor;
        case "on_surface_variant": return isDarkTheme ? "#99FFFFFF" : "#99000000";
        case "primary": return isDarkTheme ? "#FF8AB4F8" : "#FF2196F3";
        case "primary_container": return isDarkTheme ? "#1A8AB4F8" : "#1A2196F3";
        case "outline": return isDarkTheme ? "#33FFFFFF" : "#1A000000";
        case "ripple": return isDarkTheme ? "#268AB4F8" : "#262196F3";
        case "switch_on": return "#FF34C759";
        case "switch_off": return "#FFE5E5E5";
        case "error": return "#FFFF5555";
        default: return "#FF000000";
    }
}

int getStatusBarHeightValue(Context context) {
    if (context == null) return 0;
    int resultHeight = 0;
    try {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            resultHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
    } catch (Throwable exception) {
        traceLog("setwindow", "getStatusBarHeightValue: " + exception.getMessage());
    }
    return resultHeight;
}

void setSettingsImmersiveStatusBar(Activity activity, Window window, boolean isLightMode) {
    if (activity == null || window == null) return;
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                (isLightMode ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0)
            );
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    } catch (Throwable exception) {
        traceLog("setwindow", "setSettingsImmersiveStatusBar: " + exception.getMessage());
    }
}

void openSettingsExternalBrowser(Context context, String urlValue) {
    if (context == null || urlValue == null) return;
    try {
        if (urlValue.contains("mqqapi")) {
            ((IJumpApi) QRoute.api(IJumpApi.class)).doJumpAction(context, urlValue);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlValue));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    } catch (Throwable exception) {
        traceLog("setwindow", "openSettingsExternalBrowser: " + exception.getMessage());
        Toast("打开失败: " + exception.getMessage());
    }
}

View createSettingsSwitchView(Context context, boolean initialValue, final String configName, final String keyName, final String itemName, final Runnable onChangeCallback) {
    if (context == null) return null;

    Activity activity = (context instanceof Activity) ? (Activity) context : getSettingsCurrentActivity();
    if (activity == null) return null;

    FrameLayout containerLayout = new FrameLayout(context);
    int switchWidth = dp(context, 48);
    int switchHeight = dp(context, 28);
    FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(switchWidth, switchHeight);
    containerLayout.setLayoutParams(containerParams);

    final View trackView = new View(context);
    FrameLayout.LayoutParams trackParams = new FrameLayout.LayoutParams(-1, -1);
    trackView.setLayoutParams(trackParams);
    final GradientDrawable trackBackground = new GradientDrawable();
    trackBackground.setCornerRadius(dp(context, 14));
    trackView.setBackground(trackBackground);
    containerLayout.addView(trackView);

    final View thumbView = new View(context);
    int thumbSize = dp(context, 24);
    int thumbMargin = dp(context, 2);
    FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(thumbSize, thumbSize);
    thumbParams.gravity = Gravity.CENTER_VERTICAL | (initialValue ? Gravity.RIGHT : Gravity.LEFT);
    thumbParams.setMargins(thumbMargin, 0, thumbMargin, 0);
    thumbView.setLayoutParams(thumbParams);
    GradientDrawable thumbBackground = new GradientDrawable();
    thumbBackground.setColor(Color.WHITE);
    thumbBackground.setCornerRadius(dp(context, 12));
    thumbView.setBackground(thumbBackground);
    containerLayout.addView(thumbView);

    final boolean[] currentState = new boolean[]{initialValue};

    final Runnable updateSwitchUI = new Runnable() {
        public void run() {
            boolean isOn = currentState[0];
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                trackBackground.setColor(isOn ? Color.parseColor(getSettingsThemeColor(act, "switch_on")) : Color.parseColor(getSettingsThemeColor(act, "switch_off")));
            } else {
                trackBackground.setColor(isOn ? Color.parseColor("#FF34C759") : Color.parseColor("#FFE5E5E5"));
            }
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) thumbView.getLayoutParams();
            params.gravity = Gravity.CENTER_VERTICAL | (isOn ? Gravity.RIGHT : Gravity.LEFT);
            thumbView.setLayoutParams(params);
        }
    };
    updateSwitchUI.run();

    containerLayout.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            currentState[0] = !currentState[0];
            updateSwitchUI.run();
            if (configName != null && keyName != null) {
                putBoolean(configName, keyName, currentState[0]);
            }
            String message = itemName + (currentState[0] ? " 已开启" : " 已关闭");
            qqToast(2, message);
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                vibrate(act, 32);
            }
            if (onChangeCallback != null) {
                onChangeCallback.run();
            }
        }
    });

    return containerLayout;
}
boolean pinyinMatchText(String textValue, String queryValue) {
    if (textValue == null || queryValue == null) return false;
    textValue = textValue.toLowerCase();
    queryValue = queryValue.toLowerCase();

    if (textValue.contains(queryValue)) return true;

    String pinyinLetters = getPinyinFirstLetters(textValue);
    return pinyinLetters.contains(queryValue);
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

static String lastSearchQuery = "";
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
            if (calcSearchScore(meta, queryValue) > 0) {
                String displayPath = meta.getDisplayPath();
                if (addedPaths.contains(displayPath)) continue;
                addedPaths.add(displayPath);
                scoreList.add(new Integer(calcSearchScore(meta, queryValue)));
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
            addSearchResultItem(ctx, resultsContainer, meta.getDisplayPath(), queryValue, itemKey, meta.level1, meta.level2, meta.level3, isDark);
        }
    }
    if (resultsContainer.getChildCount() == 0) {
        TextView noResult = new TextView(ctx);
        noResult.setText("未找到相关设置");
        noResult.setTextSize(14);
        noResult.setTextColor(Color.parseColor(isDark ? "#99FFFFFF" : "#99000000"));
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
        traceLog("setwindow", "getPinyinFirstLetter: " + exception.getMessage());
    }
    return String.valueOf(character);
}

void cleanupSettingsResources() {
    if (SettingsState.settingsItemTextViews != null) {
        SettingsState.settingsItemTextViews.clear();
        SettingsState.settingsItemTextViews = null;
    }
    if (SettingsState.settingsCategoryContainers != null) {
        SettingsState.settingsCategoryContainers.clear();
        SettingsState.settingsCategoryContainers = null;
    }
    if (SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.clear();
        SettingsState.settingsItemViews = null;
    }
    if (SettingsState.settingsItemMeta != null) {
        SettingsState.settingsItemMeta.clear();
        SettingsState.settingsItemMeta = null;
    }
    SettingsState.settingsListContainer = null;
    SettingsState.settingsScrollView = null;
    SettingsState.settingsHighlightView = null;
    SettingsState.settingsPendingHighlightKey = null;
    SettingsState.settingsCurrentLevel1 = null;
    SettingsState.settingsCurrentLevel2 = null;
    SettingsState.settingsCurrentLevel3 = null;
    SettingsState.settingsIndexBuilt = false;
}

void cleanupAllDialogs() {
    if (SettingsState.settingsSearchDialog != null && SettingsState.settingsSearchDialog.isShowing()) {
        try {
            SettingsState.settingsSearchDialog.dismiss();
        } catch (Throwable exception) {}
    }
    SettingsState.settingsSearchDialog = null;

    if (SettingsState.settingsDialogStack != null) {
        for (int i = SettingsState.settingsDialogStack.size() - 1; i >= 0; i--) {
            Dialog dialog = (Dialog) SettingsState.settingsDialogStack.get(i);
            if (dialog != null && dialog.isShowing()) {
                try {
                    dialog.dismiss();
                } catch (Throwable exception) {
                    traceLog("setwindow", "cleanup dialog: " + exception.getMessage());
                }
            }
        }
        SettingsState.settingsDialogStack.clear();
    }
    SettingsState.settingsCurrentActivityRef = null;
    cleanupSettingsResources();
}

void highlightSettingsItem(final View targetView) {
    if (targetView == null) return;

    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    boolean isDark = isThemeDark(activity);

    SettingsState.settingsHighlightView = targetView;
    final GradientDrawable highlightBg = new GradientDrawable();
    highlightBg.setColor(Color.parseColor(isDark ? "#338AB4F8" : "#332196F3"));
    highlightBg.setCornerRadius(dp(activity, 16));

    // 第一次高亮
    targetView.setBackground(highlightBg);

    uiHandler.postDelayed(() -> {
        // 第一次恢复
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            targetView.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(act, "surface")), Color.parseColor(getSettingsThemeColor(act, "ripple")), 0));
        }

        uiHandler.postDelayed(() -> {
            // 第二次高亮
            if (SettingsState.settingsHighlightView == targetView) targetView.setBackground(highlightBg);

            uiHandler.postDelayed(() -> {
                // 最终恢复
                if (SettingsState.settingsHighlightView == targetView) {
                    Activity act2 = getSettingsCurrentActivity();
                    if (act2 != null) {
                        targetView.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(act2, "surface")), Color.parseColor(getSettingsThemeColor(act2, "ripple")), 0));
                    }
                    SettingsState.settingsHighlightView = null;
                }
            }, 350);
        }, 150);
    }, 350);
}

void scrollToAndHighlight(String itemKey) {
    if (SettingsState.settingsItemViews == null || !SettingsState.settingsItemViews.containsKey(itemKey)) {
        traceLog("setwindow", "scrollToAndHighlight: itemKey not found: " + itemKey);
        return;
    }

    final View targetView = (View) SettingsState.settingsItemViews.get(itemKey);
    if (targetView == null) {
        traceLog("setwindow", "scrollToAndHighlight: targetView is null");
        return;
    }

    if (SettingsState.settingsScrollView == null) {
        traceLog("setwindow", "scrollToAndHighlight: scrollView is null");
        return;
    }

    Activity activity = getSettingsCurrentActivity();
    if (activity == null) {
        traceLog("setwindow", "scrollToAndHighlight: activity is null");
        return;
    }

    uiHandler.post(new Runnable() {
        public void run() {
            try {
                final int[] location = new int[2];
                targetView.getLocationOnScreen(location);
                Activity act = getSettingsCurrentActivity();
                if (act == null) return;
                final int scrollY = location[1] - dp(act, 150);
                SettingsState.settingsScrollView.smoothScrollTo(0, Math.max(0, scrollY));

                uiHandler.postDelayed(new Runnable() {
                    public void run() {
                        highlightSettingsItem(targetView);
                    }
                }, 400);
            } catch (Throwable exception) {
                traceLog("setwindow", "scrollToAndHighlight error: " + exception.getMessage());
            }
        }
    });
}

void handleSearchResultClick(final Activity activity, final String itemKey, final String level1, final String level2, final String level3, final String query) {
    if (activity == null) return;
    vibrate(activity, 32);

    if (query != null && query.length() >= 2) {
        String historyString = getString("settings", "search_history", "");
        List historyList = new ArrayList();
        if (historyString != null && !historyString.isEmpty()) {
            String[] historyItems = historyString.split("\\|");
            for (String item : historyItems) {
                if (item != null && !item.trim().isEmpty()) {
                    historyList.add(item.trim());
                }
            }
        }
        historyList.remove(query);
        historyList.add(0, query);
        if (historyList.size() > 10) {
            historyList.remove(historyList.size() - 1);
        }
        StringBuilder historyBuilder = new StringBuilder();
        for (int i = 0; i < historyList.size(); i++) {
            if (i > 0) historyBuilder.append("|");
            historyBuilder.append(historyList.get(i));
        }
        putString("settings", "search_history", historyBuilder.toString());
    }

    if (SettingsState.settingsSearchDialog != null && SettingsState.settingsSearchDialog.isShowing()) {
        SettingsState.settingsSearchDialog.dismiss();
    }
    SettingsState.settingsSearchDialog = null;
    SettingsState.settingsPendingHighlightKey = itemKey;

    int targetDepth = level2 != null ? 2 : (level1 != null ? 1 : 0);
    int currentSize = SettingsState.settingsDialogStack != null ? SettingsState.settingsDialogStack.size() : 0;

    if (currentSize == targetDepth + 1) {
        Dialog topDialog = (Dialog) SettingsState.settingsDialogStack.get(currentSize - 1);
        if (topDialog != null && topDialog.isShowing()) {
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    scrollToAndHighlight(itemKey);
                }
            }, 100);
            return;
        }
    }

    if (SettingsState.settingsDialogStack != null) {
        for (int i = SettingsState.settingsDialogStack.size() - 1; i >= 0; i--) {
            Dialog dialog = (Dialog) SettingsState.settingsDialogStack.get(i);
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
        SettingsState.settingsDialogStack.clear();
    }

    uiHandler.postDelayed(new Runnable() {
        public void run() {
            showSettingsMenu(activity, level1, level2, level3);
        }
    }, 150);
}

void showSettingsMenu(final Activity activity, final String level1Title, final String level2Title, final String level3Title) {
    if (activity == null || activity.isFinishing()) return;

    uiHandler.post(new Runnable() {
        public void run() {
            try {
                if (SettingsState.settingsCurrentActivityRef == null) {
                    SettingsState.settingsCurrentActivityRef = activity;
                }

                if (SettingsState.settingsDialogStack == null) {
                    SettingsState.settingsDialogStack = new ArrayList();
                }

                if (SettingsState.settingsCategoryContainers == null) {
                    SettingsState.settingsCategoryContainers = new HashMap();
                } else {
                    SettingsState.settingsCategoryContainers.clear();
                }

                if (SettingsState.settingsItemTextViews == null) {
                    SettingsState.settingsItemTextViews = new HashMap();
                } else {
                    SettingsState.settingsItemTextViews.clear();
                }

                if (SettingsState.settingsItemViews == null) {
                    SettingsState.settingsItemViews = new HashMap();
                } else {
                    SettingsState.settingsItemViews.clear();
                }

                if (level1Title == null && level2Title == null && level3Title == null) {
                    if (SettingsState.settingsItemMeta == null) {
                        SettingsState.settingsItemMeta = new HashMap();
                    } else {
                        SettingsState.settingsItemMeta.clear();
                    }
                }
                SettingsState.settingsCurrentLevel1 = level1Title;
                SettingsState.settingsCurrentLevel2 = level2Title;
                SettingsState.settingsCurrentLevel3 = level3Title;
                SettingsState.settingsCurrentCategory = null;

                boolean isDark = isThemeDark(activity);

                final Dialog currentDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar);
                currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                LinearLayout rootLayout = new LinearLayout(activity);
                rootLayout.setOrientation(LinearLayout.VERTICAL);
                rootLayout.setBackgroundColor(Color.parseColor(getSettingsThemeColor(activity, "background")));

                View statusBarPlaceholder = new View(activity);
                int statusBarHeight = getStatusBarHeightValue(activity);
                LinearLayout.LayoutParams placeholderParams = new LinearLayout.LayoutParams(-1, statusBarHeight);
                statusBarPlaceholder.setLayoutParams(placeholderParams);
                rootLayout.addView(statusBarPlaceholder);

                LinearLayout titleBarLayout = new LinearLayout(activity);
                titleBarLayout.setOrientation(LinearLayout.HORIZONTAL);
                titleBarLayout.setGravity(Gravity.CENTER_VERTICAL);
                titleBarLayout.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));

                FrameLayout backBtnContainer = new FrameLayout(activity);
                FrameLayout.LayoutParams backContainerParams = new FrameLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
                backBtnContainer.setLayoutParams(backContainerParams);

                TextView backBtn = createButton(activity, "‹", Color.parseColor(getSettingsThemeColor(activity, "on_surface")), Color.TRANSPARENT, 28f, 0, 0, 0, false, 0, 0, null);
                FrameLayout.LayoutParams backBtnParams = new FrameLayout.LayoutParams(-2, -2);
                backBtnParams.gravity = Gravity.CENTER;
                backBtn.setLayoutParams(backBtnParams);

                final boolean isRootMenu = level1Title == null && level2Title == null && level3Title == null;
                if (isRootMenu) {
                    backBtn.setText("‹");
                    backBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            vibrate(activity, 32);
                            cleanupAllDialogs();
                        }
                    });
                } else {
                    backBtn.setText("‹");
                    backBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            vibrate(activity, 32);
                            if (currentDialog != null && currentDialog.isShowing()) {
                                currentDialog.dismiss();
                            }
                            if (SettingsState.settingsDialogStack != null && !SettingsState.settingsDialogStack.isEmpty()) {
                                SettingsState.settingsDialogStack.remove(SettingsState.settingsDialogStack.size() - 1);
                            }
                            if (SettingsState.settingsDialogStack == null || SettingsState.settingsDialogStack.isEmpty()) {
                                if (level2Title != null) {
                                    showSettingsMenu(activity, level1Title, null, null);
                                } else if (level1Title != null) {
                                    showSettingsMenu(activity, null, null, null);
                                }
                            }
                        }
                    });
                }
                backBtnContainer.addView(backBtn);
                titleBarLayout.addView(backBtnContainer);

                String titleText = "功能菜单";
                if (level1Title != null) titleText = level1Title;
                if (level2Title != null) titleText = level2Title;
                if (level3Title != null) titleText = level3Title;

                TextView titleView = new TextView(activity);
                titleView.setText(titleText);
                titleView.setTextSize(20);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
                titleView.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(activity, 40), 1.0f);
                titleView.setLayoutParams(titleParams);
                titleBarLayout.addView(titleView);

                if (level1Title == null && level2Title == null && level3Title == null) {
                    FrameLayout searchBtnContainer = new FrameLayout(activity);
                    FrameLayout.LayoutParams searchContainerParams = new FrameLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
                    searchBtnContainer.setLayoutParams(searchContainerParams);

                    TextView searchBtn = createButton(activity, "🔍", Color.parseColor(getSettingsThemeColor(activity, "on_surface")), Color.TRANSPARENT, 20f, 0, 0, 0, false, 0, 0, null);
                    FrameLayout.LayoutParams searchBtnParams = new FrameLayout.LayoutParams(-2, -2);
                    searchBtnParams.gravity = Gravity.CENTER;
                    searchBtn.setLayoutParams(searchBtnParams);
                    searchBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            vibrate(activity, 32);
                            showSettingsSearchPage(activity);
                        }
                    });
                    searchBtnContainer.addView(searchBtn);
                    titleBarLayout.addView(searchBtnContainer);
                }

                rootLayout.addView(titleBarLayout);

                SettingsState.settingsScrollView = new ScrollView(activity);
                SettingsState.settingsScrollView.setVerticalScrollBarEnabled(false);
                SettingsState.settingsScrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));

                SettingsState.settingsListContainer = new LinearLayout(activity);
                SettingsState.settingsListContainer.setOrientation(LinearLayout.VERTICAL);
                SettingsState.settingsListContainer.setPadding(0, 0, 0, dp(activity, 16));

                if (level1Title == null && level2Title == null && level3Title == null) {
                    buildFullSettingsIndex(activity);
                }
                buildSettingsMenuContent(activity, level1Title, level2Title, level3Title);

                if (level1Title == null && level2Title == null && level3Title == null) {
                    buildSettingsBottomArea(activity);
                }

                SettingsState.settingsScrollView.addView(SettingsState.settingsListContainer);
                rootLayout.addView(SettingsState.settingsScrollView);

                currentDialog.setContentView(rootLayout);
                currentDialog.setCancelable(true);
                currentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (SettingsState.settingsDialogStack != null) {
                            SettingsState.settingsDialogStack.remove(currentDialog);
                        }
                        if (SettingsState.settingsDialogStack == null || SettingsState.settingsDialogStack.isEmpty()) {
                            SettingsState.settingsCurrentActivityRef = null;
                            cleanupSettingsResources();
                        }
                    }
                });

                Window window = currentDialog.getWindow();
                if (window != null) {
                    window.setLayout(-1, -1);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    setSettingsImmersiveStatusBar(activity, window, !isDark);
                }

                SettingsState.settingsDialogStack.add(currentDialog);
                currentDialog.show();

                // 处理待高亮的项
                if (SettingsState.settingsPendingHighlightKey != null && !SettingsState.settingsPendingHighlightKey.isEmpty()) {
                    final String keyToHighlight = SettingsState.settingsPendingHighlightKey;
                    SettingsState.settingsPendingHighlightKey = null;
                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            scrollToAndHighlight(keyToHighlight);
                        }
                    }, 300);
                }

            } catch (Throwable exception) {
                traceLog("setwindow", "showSettingsMenu: " + exception.getMessage());
                Toast("菜单打开失败: " + exception.getMessage());
                cleanupAllDialogs();
            }
        }
    });
}

void showSettingsSearchPage(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;

    uiHandler.post(new Runnable() {
        public void run() {
            try {
                final boolean isDark = isThemeDark(activity);

                SettingsState.settingsSearchDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar);
                SettingsState.settingsSearchDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                LinearLayout rootLayout = new LinearLayout(activity);
                rootLayout.setOrientation(LinearLayout.VERTICAL);
                rootLayout.setBackgroundColor(Color.parseColor(getSettingsThemeColor(activity, "background")));

                View statusBarPlaceholder = new View(activity);
                int statusBarHeight = getStatusBarHeightValue(activity);
                LinearLayout.LayoutParams placeholderParams = new LinearLayout.LayoutParams(-1, statusBarHeight);
                statusBarPlaceholder.setLayoutParams(placeholderParams);
                rootLayout.addView(statusBarPlaceholder);

                LinearLayout searchBarLayout = new LinearLayout(activity);
                searchBarLayout.setOrientation(LinearLayout.HORIZONTAL);
                searchBarLayout.setGravity(Gravity.CENTER_VERTICAL);
                searchBarLayout.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));

                FrameLayout backBtnContainer = new FrameLayout(activity);
                FrameLayout.LayoutParams backContainerParams = new FrameLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
                backBtnContainer.setLayoutParams(backContainerParams);

                TextView backBtn = createButton(activity, "‹", Color.parseColor(isDark ? "#FFEFEFEF" : "#FF1A1A1A"), Color.TRANSPARENT, 28f, 0, 0, 0, false, 0, 0, null);
                FrameLayout.LayoutParams backBtnParams = new FrameLayout.LayoutParams(-2, -2);
                backBtnParams.gravity = Gravity.CENTER;
                backBtn.setLayoutParams(backBtnParams);
                backBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        vibrate(activity, 32);
                        if (SettingsState.settingsSearchDialog != null && SettingsState.settingsSearchDialog.isShowing()) {
                            SettingsState.settingsSearchDialog.dismiss();
                        }
                        SettingsState.settingsSearchDialog = null;
                    }
                });
                backBtnContainer.addView(backBtn);
                searchBarLayout.addView(backBtnContainer);

                final EditText searchInput = new EditText(activity);
                searchInput.setHint("搜索设置项...");
                searchInput.setTextSize(14);
                searchInput.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
                searchInput.setHintTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
                searchInput.setBackgroundColor(Color.parseColor(getSettingsThemeColor(activity, "surface")));
                searchInput.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
                searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                searchInput.setSingleLine(true);
                searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                searchBarLayout.addView(searchInput);

                rootLayout.addView(searchBarLayout);

                ScrollView scrollView = new ScrollView(activity);
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));

                final LinearLayout contentContainer = new LinearLayout(activity);
                contentContainer.setOrientation(LinearLayout.VERTICAL);
                contentContainer.setPadding(dp(activity, 12), 0, dp(activity, 12), dp(activity, 16));

                scrollView.addView(contentContainer);
                rootLayout.addView(scrollView);

                final String historyString = getString("settings", "search_history", "");
                final List historyList = new ArrayList();
                if (historyString != null && !historyString.isEmpty()) {
                    String[] historyItems = historyString.split("\\|");
                    for (String item : historyItems) {
                        if (item != null && !item.trim().isEmpty()) {
                            historyList.add(item.trim());
                        }
                    }
                }

                final LinearLayout historyContainer = new LinearLayout(activity);
                historyContainer.setOrientation(LinearLayout.VERTICAL);

                TextView historyTitle = new TextView(activity);
                historyTitle.setText("历史搜索");
                historyTitle.setTextSize(14);
                historyTitle.setTextColor(Color.parseColor(isDark ? "#99FFFFFF" : "#99000000"));
                historyTitle.setPadding(dp(activity, 4), dp(activity, 8), dp(activity, 4), dp(activity, 8));
                historyContainer.addView(historyTitle);

                for (int i = 0; i < historyList.size(); i++) {
                    final String historyItem = (String) historyList.get(i);
                    TextView historyItemView = new TextView(activity);
                    historyItemView.setText(historyItem);
                    historyItemView.setTextSize(16);
                    historyItemView.setTextColor(Color.parseColor(isDark ? "#FFEFEFEF" : "#FF1A1A1A"));
                    historyItemView.setPadding(dp(activity, 4), dp(activity, 12), dp(activity, 4), dp(activity, 12));
                    historyItemView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            searchInput.setText(historyItem);
                            searchInput.setSelection(historyItem.length());
                        }
                    });
                    historyContainer.addView(historyItemView);
                }

                if (historyList.isEmpty()) {
                    TextView emptyHint = new TextView(activity);
                    emptyHint.setText("暂无历史搜索");
                    emptyHint.setTextSize(14);
                    emptyHint.setTextColor(Color.parseColor(isDark ? "#99FFFFFF" : "#99000000"));
                    emptyHint.setPadding(dp(activity, 4), dp(activity, 12), dp(activity, 4), dp(activity, 12));
                    historyContainer.addView(emptyHint);
                }

                contentContainer.addView(historyContainer);

                final LinearLayout resultsContainer = new LinearLayout(activity);
                resultsContainer.setOrientation(LinearLayout.VERTICAL);
                resultsContainer.setVisibility(View.GONE);
                contentContainer.addView(resultsContainer);

                searchInput.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(Editable editable) {
                        final String queryValue = editable != null ? editable.toString().trim() : "";
                        final long token = System.currentTimeMillis();
                        SettingsState.settingsSearchToken = token;
                        final Activity ctx = activity;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                if (token != SettingsState.settingsSearchToken) return;
                                doSearch(queryValue, historyContainer, resultsContainer, ctx, isDark);
                            }
                        }, 200);
                    }
                });

                searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            hideSearchInputKeyboard(activity, searchInput);
                            return true;
                        }
                        return false;
                    }
                });

                SettingsState.settingsSearchDialog.setContentView(rootLayout);
                SettingsState.settingsSearchDialog.setCancelable(true);
                SettingsState.settingsSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        SettingsState.settingsSearchToken = 0;
                        SettingsState.settingsSearchDialog = null;
                    }
                });

                Window window = SettingsState.settingsSearchDialog.getWindow();
                if (window != null) {
                    window.setLayout(-1, -1);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    setSettingsImmersiveStatusBar(activity, window, !isDark);
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }

                SettingsState.settingsSearchDialog.show();
                searchInput.requestFocus();

            } catch (Throwable exception) {
                traceLog("setwindow", "showSettingsSearchPage: " + exception.getMessage());
                Toast("搜索页面打开失败: " + exception.getMessage());
            }
        }
    });
}

void addSearchResultItem(Activity activity, LinearLayout container, String itemText, String queryValue, final String itemKey, final String level1, final String level2, final String level3, boolean isDark) {
    if (activity == null || container == null || itemText == null || queryValue == null) return;
    TextView resultItem = new TextView(activity);
    SpannableString spannable = new SpannableString(itemText);
    int start = itemText.toLowerCase().indexOf(queryValue.toLowerCase());
    if (start >= 0) {
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor(isDark ? "#FF8AB4F8" : "#FF2196F3")), start, start + queryValue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    resultItem.setText(spannable);
    resultItem.setTextSize(16);
    resultItem.setTextColor(Color.parseColor(isDark ? "#FFEFEFEF" : "#FF1A1A1A"));
    resultItem.setPadding(dp(activity, 4), dp(activity, 12), dp(activity, 4), dp(activity, 12));
    resultItem.setClickable(true);
    resultItem.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            handleSearchResultClick(activity, itemKey, level1, level2, level3, queryValue);
        }
    });
    container.addView(resultItem);
}

void buildSettingsBottomArea(Activity activity) {
    if (activity == null) return;
    Activity currentActivity = getSettingsCurrentActivity();
    if (currentActivity == null) currentActivity = activity;

    boolean isDark = isThemeDark(currentActivity);

    LinearLayout bottomArea = new LinearLayout(activity);
    bottomArea.setOrientation(LinearLayout.VERTICAL);
    bottomArea.setGravity(Gravity.CENTER);
    bottomArea.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 16));

    LinearLayout iconRow = new LinearLayout(activity);
    iconRow.setOrientation(LinearLayout.HORIZONTAL);
    iconRow.setGravity(Gravity.CENTER);

    LinearLayout icon1Container = new LinearLayout(activity);
    icon1Container.setOrientation(LinearLayout.VERTICAL);
    icon1Container.setGravity(Gravity.CENTER);

    ImageView projectBtn = new ImageView(activity);
    try {
        String imagePath = rootPath + "GitHub.png";
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                projectBtn.setImageBitmap(bitmap);
            }
        }
    } catch (Throwable exception) {
        traceLog("setwindow", "load project btn: " + exception.getMessage());
    }
    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
    projectBtn.setLayoutParams(btnParams);
    projectBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            vibrate(activity, 32);
            showSettingsConfirmDialog(activity, "即将打开项目主页", "https://github.com/xunyyds/QFloatingX", new Runnable() {
                public void run() {
                    openSettingsExternalBrowser(activity, "https://github.com/xunyyds/QFloatingX");
                }
            });
        }
    });
    icon1Container.addView(projectBtn);

    TextView text1 = new TextView(activity);
    text1.setText("GitHub");
    text1.setTextSize(12);
    text1.setTextColor(Color.parseColor(getSettingsThemeColor(currentActivity, "on_surface_variant")));
    text1.setGravity(Gravity.CENTER);
    text1.setPadding(0, dp(activity, 4), 0, 0);
    icon1Container.addView(text1);

    LinearLayout.LayoutParams icon1Params = new LinearLayout.LayoutParams(-2, -2);
    icon1Params.rightMargin = dp(activity, 48);
    icon1Container.setLayoutParams(icon1Params);
    iconRow.addView(icon1Container);

    LinearLayout icon2Container = new LinearLayout(activity);
    icon2Container.setOrientation(LinearLayout.VERTICAL);
    icon2Container.setGravity(Gravity.CENTER);

    ImageView qqBtn = new ImageView(activity);
    try {
        String imagePath = rootPath + "QQ.png";
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                qqBtn.setImageBitmap(bitmap);
            }
        }
    } catch (Throwable exception) {
        traceLog("setwindow", "load qq btn: " + exception.getMessage());
    }
    LinearLayout.LayoutParams qqBtnParams = new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
    qqBtn.setLayoutParams(qqBtnParams);
    qqBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            vibrate(activity, 32);
            showSettingsConfirmDialog(activity, "即将打开QQ群", "702227641", new Runnable() {
                public void run() {
                    openSettingsExternalBrowser(activity, "mqqapi://app/joinImmediately?source_id=3&version=1.0&src_type=app&pkg=com.tencent.mobileqq&cmp=com.tencent.biz.JoinGroupTransitActivity&group_code=702227641&subsource_id=10019");
                }
            });
        }
    });
    icon2Container.addView(qqBtn);

    TextView text2 = new TextView(activity);
    text2.setText("加入我们");
    text2.setTextSize(12);
    text2.setTextColor(Color.parseColor(getSettingsThemeColor(currentActivity, "on_surface_variant")));
    text2.setGravity(Gravity.CENTER);
    text2.setPadding(0, dp(activity, 4), 0, 0);
    icon2Container.addView(text2);

    iconRow.addView(icon2Container);

    bottomArea.addView(iconRow);

    TextView footer = new TextView(activity);
    footer.setText("Generated by QFloatingX");
    footer.setGravity(Gravity.CENTER);
    footer.setTextColor(Color.parseColor(isDark ? "#555555" : "#AAAAAA"));
    footer.setTextSize(10);
    footer.setPadding(0, dp(activity, 4), 0, dp(activity, 4));
    bottomArea.addView(footer);

    if (SettingsState.settingsListContainer != null) {
        SettingsState.settingsListContainer.addView(bottomArea);
    }
}

void showSettingsConfirmDialog(Activity activity, String titleText, String messageText, final Runnable confirmCallback) {
    if (activity == null) return;
    boolean isDark = isThemeDark(activity);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle(titleText);
    builder.setMessage(messageText);
    builder.setPositiveButton("打开", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (confirmCallback != null) {
                confirmCallback.run();
            }
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void addIndexItem(String key, String name, String description, String level1, String level2, String category, String type) {
    SettingsState.settingsItemMeta.put(key, new SettingsItemMeta(name, description, level1, level2, null, category, type));
}

void buildFullSettingsIndex(Activity activity) {
    if (SettingsState.settingsIndexBuilt && SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.isEmpty()) {
        return;
    }
    if (SettingsState.settingsItemMeta == null) {
        SettingsState.settingsItemMeta = new HashMap();
    }
    SettingsState.settingsItemMeta.clear();
    SettingsState.settingsIndexBuilt = true;

    boolean isDark = isThemeDark(activity);
    String suffix = isDark ? " (深色模式)" : " (浅色模式)";

    addIndexItem("item_java_script", "Java脚本", "", null, null, "设置", "click");
    addIndexItem("item_script_settings", "模块设置", "", null, null, "设置", "click");
    addIndexItem("item_settings_ui", "脚本设置", "", null, null, "设置", "click");
    addIndexItem("item_mock_location", "模拟定位", "", null, null, "开关", "switch");
    addIndexItem("item_input_hint", "输入框提示", "", null, null, "开关", "switch");
    addIndexItem("item_KeepAlive", "后台保活", "", null, null, "开关", "switch");
    addIndexItem("item_msg_stats_switch", "消息统计", "", null, null, "开关", "switch");
    addIndexItem("item_double_click_switch", "双击消息", "", null, null, "开关", "switch");
    addIndexItem("item_set_location", "设置经纬度", "", null, null, "功能", "click");
    addIndexItem("item_set_input_hint", "设置输入框提示词", "", null, null, "功能", "click");
    addIndexItem("item_msg_stats", "消息统计", "", null, null, "功能", "click");
    addIndexItem("item_qzone", "空间操作", "", null, null, "功能", "click");
    addIndexItem("item_run_status", "运行状态", "", null, null, "功能", "click");
    addIndexItem("item_html_browser", "HTML浏览器", "", null, null, "功能", "click");
    addIndexItem("item_cancel_reload", "取消/重载", "", null, null, "其他", "click");
    addIndexItem("item_check_update", "检查更新", "手动检测最新版本（使用当前更新通道）", null, null, "关于", "click");
    addIndexItem("item_update_channel", "更新通道", "", null, null, "关于", "choice");
    addIndexItem("item_changelog", "查看更新日志", "查看历史更新记录", null, null, "关于", "click");

    addIndexItem("item_basic_mode", "基础模式", "主题、弹窗大小、振动反馈", "设置", null, "界面", "click");
    addIndexItem("item_bg_icon", "背景样式", "背景类型、颜色、图片", "设置", null, "界面", "click");
    addIndexItem("item_font_style", "字体样式", "字体风格、大小、颜色", "设置", null, "界面", "click");
    addIndexItem("item_toast_hint", "开关吐司提示", "开关和配置你的相关吐司提示", "设置", null, "提示", "click");
    addIndexItem("item_thread_pool", "线程池", "优先级、队列、策略", "设置", null, "其他", "click");
    addIndexItem("item_float_window", "悬浮窗设置", "图标、大小、灵敏度", "设置", null, "其他", "click");
    addIndexItem("item_debug", "调试", "预览、重置", "设置", null, "其他", "click");

    addIndexItem("基础模式_主题模式", "主题模式", "", "设置", "基础模式", "基础模式", "choice");
    addIndexItem("基础模式_弹窗大小(比例)", "弹窗大小(比例)", "", "设置", "基础模式", "基础模式", "choice");
    addIndexItem("ui_dialog_width", "弹窗宽度", "默认最大260dp", "设置", "基础模式", "基础模式", "input");
    addIndexItem("ui_dialog_height", "弹窗高度", "自适应内容", "设置", "基础模式", "基础模式", "input");
    addIndexItem("振动反馈", "振动反馈", "", "设置", "基础模式", "基础模式", "switch");

    addIndexItem("背景样式_背景类型", "背景类型", "", "设置", "背景样式", "背景样式", "choice");
    addIndexItem("背景样式_预设颜色" + suffix, "预设颜色", "点击选择内置配色", "设置", "背景样式", "背景样式", "click");
    addIndexItem("背景样式_预设渐变" + suffix, "预设渐变", "点击选择内置渐变", "设置", "背景样式", "背景样式", "click");
    addIndexItem("背景样式_选择背景图片", "选择背景图片", "点击选择本地图片", "设置", "背景样式", "背景样式", "click");
    addIndexItem("ui_bg_color_dark", "自定义Hex", "", "设置", "背景样式", "背景样式", "color");
    addIndexItem("ui_bg_color_light", "自定义Hex", "", "设置", "背景样式", "背景样式", "color");
    addIndexItem("ui_bg_gradient_dark", "自定义渐变", "Hex1,Hex2,Hex3", "设置", "背景样式", "背景样式", "input");
    addIndexItem("ui_bg_gradient_light", "自定义渐变", "Hex1,Hex2,Hex3", "设置", "背景样式", "背景样式", "input");
    addIndexItem("ui_img_blur", "图片模糊 (0-25)", "0为不模糊", "设置", "背景样式", "背景样式", "input");
    addIndexItem("ui_img_alpha", "遮罩浓度 (0-255)", "越大越暗", "设置", "背景样式", "背景样式", "input");

    addIndexItem("字体样式_字体风格", "字体风格", "", "设置", "字体样式", "字体样式", "choice");
    addIndexItem("字体样式_字体大小", "字体大小", "", "设置", "字体样式", "字体样式", "choice");
    addIndexItem("ui_text_color_dark", "字体颜色", "", "设置", "字体样式", "字体样式", "color");
    addIndexItem("ui_text_color_light", "字体颜色", "", "设置", "字体样式", "字体样式", "color");

    addIndexItem("加载提示", "加载提示", "开启后在脚本加载时显示提示", "设置", "提示", "提示", "switch");
    addIndexItem("加载通知", "加载通知", "开启后在脚本加载时发送通知提示", "设置", "提示", "提示", "switch");

    addIndexItem("线程池_线程优先级", "线程优先级", "", "设置", "线程池", "线程池", "choice");
    addIndexItem("thread_pool_queue_capacity", "任务队列容量", "默认50", "设置", "线程池", "线程池", "input");
    addIndexItem("thread_pool_keep_alive", "核心线程存活(秒)", "默认30", "设置", "线程池", "线程池", "input");
    addIndexItem("线程池_任务满载策略", "任务满载策略", "", "设置", "线程池", "线程池", "choice");

    addIndexItem("悬浮窗设置_更换图标", "更换图标", "", "设置", "悬浮窗设置", "悬浮窗设置", "click");
    addIndexItem("悬浮窗大小", "悬浮窗大小", "默认48", "设置", "悬浮窗设置", "悬浮窗设置", "input");
    addIndexItem("关闭区域图标大小", "关闭图标大小", "默认24", "设置", "悬浮窗设置", "悬浮窗设置", "input");
    addIndexItem("拖拽灵敏度", "拖拽灵敏度", "数值越小越灵敏", "设置", "悬浮窗设置", "悬浮窗设置", "input");
    addIndexItem("长按关闭阈值", "长按关闭阈值", "默认650ms", "设置", "悬浮窗设置", "悬浮窗设置", "input");
    addIndexItem("iconAlpha", "图标透明度", "0-255", "设置", "悬浮窗设置", "悬浮窗设置", "input");
    addIndexItem("悬浮窗设置_帧率设置", "帧率设置", "", "设置", "悬浮窗设置", "悬浮窗设置", "choice");
    addIndexItem("gifDelay", "动画速度 (每帧延迟ms)", "越小越快", "设置", "悬浮窗设置", "悬浮窗设置", "input");

    addIndexItem("调试_预览设置", "预览设置", "预览当前设置效果", "设置", "调试", "调试", "click");
    addIndexItem("调试_重置设置", "重置设置", "恢复默认设置", "设置", "调试", "调试", "click");
    addIndexItem("log_delete_threshold", "日志大小阈值", "日志文件夹超过此MB数自动清理", "设置", "调试", "调试", "input");
}

void buildSettingsMenuContent(Activity activity, String level1Title, String level2Title, String level3Title) {
    if (activity == null) return;
    if (level1Title == null) {
        buildLevel1MenuContent(activity);
        return;
    }

    if (level2Title == null) {
        buildLevel2MenuContent(activity, level1Title);
        return;
    }

    buildLevel3MenuContent(activity, level1Title, level2Title, level3Title);
}

void buildLevel1MenuContent(Activity activity) {
    if (activity == null) return;
    addSettingsCategory("设置", "");
    addSettingsItemClickWithKey("设置", "Java脚本", "", "item_java_script", new Runnable() { public void run() { 跳转到页面("me.yxp.qfun.activity.PluginActivity"); }});
    addSettingsItemClickWithKey("设置", "模块设置", "", "item_script_settings", new Runnable() { public void run() { 跳转到页面("me.yxp.qfun.activity.SettingActivity"); }});
    addSettingsItemClickWithKey("设置", "脚本设置", "", "item_settings_ui", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showSettingsMenu(act, "设置", null, null);
        }
    }});

    addSettingsCategory("开关", "");
    boolean mockLocationState = getBoolean("模拟定位开关", "模拟定位开关", false);
    addSettingsItemSwitchWithKey("开关", "模拟定位", "item_mock_location", null, null, mockLocationState, new Runnable() { public void run() {
    boolean 模拟定位开关 = !getBoolean("模拟定位开关", "模拟定位开关", false);
    putBoolean("模拟定位开关", "模拟定位开关", 模拟定位开关);
    if (模拟定位开关) {
        开模拟定位();
    } else {
        关模拟定位();
    }
    }});
    boolean inputHintState = getBoolean("输入框", "输入框开关", false);
    addSettingsItemSwitchWithKey("开关", "输入框提示", "item_input_hint", "输入框", "输入框开关", inputHintState, null);
    boolean KeepAlive = getBoolean("settings", "后台保活", false);
    addSettingsItemSwitchWithKey("开关", "后台保活", "item_KeepAlive", "settings", "后台保活", KeepAlive, null);
    boolean msgStatsState = getBoolean("settings", "消息统计开关", true);
    addSettingsItemSwitchWithKey("开关", "消息统计", "item_msg_stats_switch", "settings", "消息统计开关", msgStatsState, null);
    boolean doubleClickMsgState = getBoolean("settings", "双击消息开关", true);
    addSettingsItemSwitchWithKey("开关", "双击消息", "item_double_click_switch", "settings", "双击消息开关", doubleClickMsgState, null);

    addSettingsCategory("功能", "");
    addSettingsItemClickWithKey("功能", "设置经纬度", "", "item_set_location", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showLocationDialog(act);
        }
    }});
    addSettingsItemClickWithKey("功能", "设置输入框提示词", "", "item_set_input_hint", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showInputDialog(act);
        }
    }});
    addSettingsItemClickWithKey("功能", "消息统计", "", "item_msg_stats", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showStatsDialog(act);
        }
    }});
    addSettingsItemClickWithKey("功能", "空间操作", "", "item_qzone", new Runnable() { public void run() { showQzoneConfig(); }});
    addSettingsItemClickWithKey("功能", "运行状态", "", "item_run_status", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            运行状态Dialog(act);
        }
    }});
    addSettingsItemClickWithKey("功能", "HTML浏览器", "", "item_html_browser", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showHtmlOptionDialog(act);
        }
    }});

    addSettingsCategory("其他", "");
    addSettingsItemClickWithKey("其他", "取消/重载", "", "item_cancel_reload", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showReOrUnDialog(act);
        }
    }});

    addSettingsCategory("关于", "");
    addSettingsItemClickWithKey("关于", "检查更新", "手动检测最新版本（使用当前更新通道）", "item_check_update", new Runnable() { public void run() {
        manualCheckQFXUpdate();
    }});
    addSettingsItemChoiceWithKey("关于", "更新通道", "item_update_channel", "item_update_channel", getUpdateChannelDisplayText(), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showUpdateChannelChoiceDialog(act);
        }
    }});
    addSettingsItemClickWithKey("关于", "查看更新日志", "查看历史更新记录", "item_changelog", new Runnable() { public void run() {
        try {
            String logContent = 读(pluginPath + "/更新日志.txt");
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                mkts(act, "更新日志", logContent);
            }
        } catch (Throwable exception) {
            Toast("读取更新日志失败: " + exception.getMessage());
        }
    }});
}

void buildLevel2MenuContent(Activity activity, String level1Title) {
    if (activity == null || level1Title == null) return;
    if ("设置".equals(level1Title)) {
        addSettingsCategory("界面", "");
        addSettingsItemClickWithKey("界面", "基础模式", "主题、弹窗大小、振动反馈", "item_basic_mode", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "基础模式", null);
            }
        }});
        addSettingsItemClickWithKey("界面", "背景样式", "背景类型、颜色、图片", "item_bg_icon", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "背景样式", null);
            }
        }});
        addSettingsItemClickWithKey("界面", "字体样式", "字体风格、大小、颜色", "item_font_style", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "字体样式", null);
            }
        }});

        addSettingsCategory("提示", "");
        addSettingsItemClickWithKey("提示", "开关吐司提示", "开关和配置你的相关吐司提示", "item_toast_hint", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "提示", null);
            }
        }});

        addSettingsCategory("其他", "");
        addSettingsItemClickWithKey("其他", "线程池", "优先级、队列、策略", "item_thread_pool", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "线程池", null);
            }
        }});
        addSettingsItemClickWithKey("其他", "悬浮窗设置", "图标、大小、灵敏度", "item_float_window", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "悬浮窗设置", null);
            }
        }});
        addSettingsItemClickWithKey("其他", "调试", "预览、重置、log删除阈值", "item_debug", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "调试", null);
            }
        }});
    }
}

void buildLevel3MenuContent(Activity activity, String level1Title, String level2Title, String level3Title) {
    if (activity == null || level1Title == null || level2Title == null) return;
    if ("设置".equals(level1Title)) {
        if ("基础模式".equals(level2Title)) {
            addSettingsCategory("基础模式");
            addSettingsItemChoice("基础模式", "主题模式", "ui_theme_mode", getThemeModeDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showThemeModeChoiceDialog(act);
                }
            }});
            addSettingsItemChoice("基础模式", "弹窗大小(比例)", "ui_dialog_scale", getScaleDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showScaleSliderDialog(act);
                }
            }});
            addSettingsInputItem("基础模式", "弹窗宽度", "默认最大260dp", "ui_dialog_width", "如: 280", "", null);
            addSettingsInputItem("基础模式", "弹窗高度", "自适应内容", "ui_dialog_height", "如: 400", "", null);
            boolean vibrationFeedbackState = getBoolean("settings", "振动反馈", true);
            addSettingsSwitchItem("基础模式", "振动反馈", null, "振动反馈", vibrationFeedbackState, null);
        }

        if ("背景样式".equals(level2Title)) {
            addSettingsCategory("背景样式");
            addSettingsItemChoice("背景样式", "背景类型", "ui_bg_type", getBgTypeDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showBgTypeChoiceDialog(act);
                }
            }});

            String bgType = getString("settings", "ui_bg_type", "color");
            boolean isDark = isThemeDark(activity);
            String suffix = isDark ? " (深色模式)" : " (浅色模式)";

            if ("color".equals(bgType)) {
                addSettingsItemClick("背景样式", "预设颜色" + suffix, "点击选择内置配色", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) {
                        showPresetColorDialog(act);
                    }
                }});
                String colorKey = isDark ? "ui_bg_color_dark" : "ui_bg_color_light";
                String colorValue = getString("settings", colorKey, isDark ? "#FF1E1E1E" : "#FFFFFF");
                addSettingsColorItem("背景样式", "自定义Hex", null, colorKey, colorValue, null, false);
            } else if ("gradient".equals(bgType)) {
                addSettingsItemClick("背景样式", "预设渐变" + suffix, "点击选择内置渐变", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) {
                        showPresetGradientDialog(act);
                    }
                }});
                String gradKey = isDark ? "ui_bg_gradient_dark" : "ui_bg_gradient_light";
                addSettingsInputItem("背景样式", "自定义渐变", "Hex1,Hex2,Hex3", gradKey, "#RRGGBB,#RRGGBB,#RRGGBB", "", null);
            } else if ("image".equals(bgType)) {
                addSettingsItemClick("背景样式", "选择背景图片", "点击选择本地图片", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        act.startActivityForResult(intent, 1007);
                        Toast("选择后自动居中裁剪应用");
                    } catch(Throwable exception) {
                        Toast("失败: " + exception);
                    }
                }});
                addSettingsInputItem("背景样式", "图片模糊 (0-25)", "0为不模糊", "ui_img_blur", "0-25", "0", null);
                addSettingsInputItem("背景样式", "遮罩浓度 (0-255)", "越大越暗", "ui_img_alpha", "0-255", isDark ? "180" : "100", null);
            }
        }

        if ("字体样式".equals(level2Title)) {
            addSettingsCategory("字体样式");
            addSettingsItemChoice("字体样式", "字体风格", "ui_font_type", getFontTypeDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showFontTypeChoiceDialog(act);
                }
            }});
            addSettingsItemChoice("字体样式", "字体大小", "ui_font_size", getFontSizeDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showFontSizeChoiceDialog(act);
                }
            }});
            boolean isDark = isThemeDark(activity);
            String textColorKey = isDark ? "ui_text_color_dark" : "ui_text_color_light";
            String textColorValue = getString("settings", textColorKey, "");
            String suffix = isDark ? " (深色模式)" : " (浅色模式)";
            addSettingsColorItem("字体样式", "字体颜色" + suffix, "留空自动配色 (推荐)", textColorKey, textColorValue, null, true);
        }

        if ("提示".equals(level2Title)) {
            addSettingsCategory("提示");
            boolean toastSwitchState = getBoolean("settings", "加载提示", false);
            addSettingsSwitchItem("提示", "加载提示", "开启后在脚本加载时显示提示", "加载提示", toastSwitchState, null);
            boolean NoticeSwitchState = getBoolean("settings", "加载通知", false);
            addSettingsSwitchItem("提示", "加载通知", "开启后在脚本加载时发送通知提示", "加载通知", NoticeSwitchState, null);
        }

        if ("线程池".equals(level2Title)) {
            addSettingsCategory("线程池");
            addSettingsItemChoice("线程池", "线程优先级", "thread_pool_priority", getThreadPriorityDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showThreadPriorityChoiceDialog(act);
                }
            }});
            addSettingsInputItem("线程池", "任务队列容量", "默认50", "thread_pool_queue_capacity", "数字", "50", null);
            addSettingsInputItem("线程池", "核心线程存活(秒)", "默认30", "thread_pool_keep_alive", "秒数", "30", null);
            addSettingsItemChoice("线程池", "任务满载策略", "thread_pool_reject_policy", getRejectPolicyDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showRejectPolicyChoiceDialog(act);
                }
            }});
        }

        if ("悬浮窗设置".equals(level2Title)) {
            addSettingsCategory("悬浮窗设置");
            addSettingsItemClick("悬浮窗设置", "更换图标", getIconTypeDisplayText(), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act == null) return;
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    act.startActivityForResult(intent, 1005);
                    Toast("选择后请重新打开设置刷新");
                } catch(Throwable exception) {
                    Toast("文件选择启动失败: " + exception);
                }
            }});
            addSettingsInputItem("悬浮窗设置", "悬浮窗大小", "默认48", "悬浮窗大小", "dp", "48", null);
            addSettingsInputItem("悬浮窗设置", "关闭图标大小", "默认24", "关闭区域图标大小", "dp", "24", null);
            addSettingsInputItem("悬浮窗设置", "拖拽灵敏度", "数值越小越灵敏", "拖拽灵敏度", "数字", "12", null);
            addSettingsInputItem("悬浮窗设置", "长按关闭阈值", "默认650ms", "长按关闭阈值", "毫秒", "650", null);
            addSettingsInputItem("悬浮窗设置", "图标透明度", "0-255", "iconAlpha", "0-255", "255", null);

            String iconPath = getString("settings", "iconPath", "");
            boolean isAnimatedIcon = iconPath != null && iconPath.toLowerCase().endsWith(".gif");
            if (isAnimatedIcon) {
                addSettingsItemChoice("悬浮窗设置", "帧率设置", "gifDelay", getFpsDisplayText(), new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) {
                        showFpsChoiceDialog(act);
                    }
                }});
                String currentDelay = getString("settings", "gifDelay", "100");
                addSettingsInputItem("悬浮窗设置", "动画速度 (每帧延迟ms)", "越小越快", "gifDelay", "毫秒", currentDelay, null);
            }
        }

        if ("调试".equals(level2Title)) {
            addSettingsCategory("调试", null);
            addSettingsItemClick("调试", "预览设置", "预览当前设置效果", new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showSettingsPreviewPopup(act);
                }
            }});
            addSettingsItemClick("调试", "重置设置", "恢复默认设置", new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showResetAllSettingsConfirmDialog(act);
                }
            }});
            String logThreshold = getString("settings", "log_delete_threshold", "1");
            addSettingsInputItem("调试", "日志大小阈值(MB)", "日志文件夹超过此大小自动清理", "log_delete_threshold", "如: 1", logThreshold, null);
        }
    }
}
void showResetAllSettingsConfirmDialog(Activity activity) {
    if (activity == null) return;
    boolean isDark = isThemeDark(activity);

    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER);
    content.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 20));

    TextView title = new TextView(activity);
    title.setText("重置所有设置");
    title.setTextSize(18);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    title.setGravity(Gravity.CENTER);
    content.addView(title);

    TextView msg = new TextView(activity);
    msg.setText("确定要重置所有设置为默认值吗？\n此操作不可撤销");
    msg.setTextSize(14);
    msg.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    msg.setGravity(Gravity.CENTER);
    msg.setPadding(0, dp(activity, 12), 0, dp(activity, 16));
    content.addView(msg);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(content);

    builder.setNegativeButton("取消", null);

    builder.setPositiveButton("确认重置", (dialog, which) -> {
        int count = resetAllSettingsToDefault();
        qqToast(0, "已重置 " + count + " 项设置");
        Activity resetAct = getSettingsCurrentActivity();
        if (resetAct != null) {
            showSettingsMenu(resetAct, null, null, null);
        }
    });

    AlertDialog dialog = builder.show();
    applyUiTheme(activity, dialog);
}

int resetAllSettingsToDefault() {
    String[] settingKeys = {
            "ui_theme_mode", "ui_dialog_scale", "ui_dialog_width", "ui_dialog_height",
            "振动反馈", "ui_bg_type", "ui_bg_color_dark", "ui_bg_color_light",
            "ui_bg_gradient_dark", "ui_bg_gradient_light", "ui_img_blur", "ui_img_alpha",
            "ui_font_type", "ui_font_size", "ui_text_color_dark", "ui_text_color_light",
            "thread_pool_priority", "thread_pool_queue_capacity", "thread_pool_keep_alive",
            "thread_pool_name_prefix", "thread_pool_reject_policy", "悬浮窗大小",
            "关闭区域图标大小", "拖拽灵敏度", "长按关闭阈值", "移动阈值"
    };

    int count = 0;
    for (String key : settingKeys) {
        putString("settings", key, "");
        count++;
    }

    if (pendingSettingsChanges != null) {
        pendingSettingsChanges.clear();
    }

    return count;
}

void addSettingsCategory(String titleText) {
    addSettingsCategory(titleText, null);
}

void addSettingsCategory(String titleText, String descriptionText) {
    if (SettingsState.settingsListContainer == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    boolean isDark = isThemeDark(activity);

    TextView categoryTitle = new TextView(activity);
    categoryTitle.setText(titleText);
    categoryTitle.setTextSize(14);
    categoryTitle.setTypeface(null, Typeface.BOLD);
    categoryTitle.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "primary")));
    categoryTitle.setPadding(dp(activity, 28), dp(activity, 20), dp(activity, 16), dp(activity, 8));

    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
    SettingsState.settingsListContainer.addView(categoryTitle, titleParams);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView categoryDesc = new TextView(activity);
        categoryDesc.setText(descriptionText);
        categoryDesc.setTextSize(12);
        categoryDesc.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
        categoryDesc.setPadding(dp(activity, 28), 0, dp(activity, 16), dp(activity, 8));

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        SettingsState.settingsListContainer.addView(categoryDesc, descParams);
    }

    LinearLayout categoryContainer = new LinearLayout(activity);
    categoryContainer.setOrientation(LinearLayout.VERTICAL);
    categoryContainer.setBackground(roundRect(Color.parseColor(getSettingsThemeColor(activity, "surface")), dp(activity, 16)));
    categoryContainer.setClipToOutline(true);

    LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(-1, -2);
    containerParams.setMargins(dp(activity, 12), 0, dp(activity, 12), dp(activity, 8));
    SettingsState.settingsListContainer.addView(categoryContainer, containerParams);

    if (SettingsState.settingsCategoryContainers == null) SettingsState.settingsCategoryContainers = new HashMap();
    SettingsState.settingsCategoryContainers.put(titleText, categoryContainer);
    SettingsState.settingsCurrentCategory = titleText;
}

void addSettingsItemClick(String categoryName, String itemName, Runnable clickCallback) {
    addSettingsItemClick(categoryName, itemName, null, clickCallback);
}

void addSettingsItemClick(String categoryName, String itemName, String descriptionText, final Runnable clickCallback) {
    addSettingsItemClickWithKey(categoryName, itemName, descriptionText, categoryName + "_" + itemName, clickCallback);
}

void addSettingsItemClickWithKey(String categoryName, String itemName, String descriptionText, String itemKey, final Runnable clickCallback) {
    if (SettingsState.settingsCategoryContainers == null || categoryName == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container == null) return;

    boolean isDark = isThemeDark(activity);

    FrameLayout itemWrapper = new FrameLayout(activity);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));

    LinearLayout textArea = new LinearLayout(activity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

    TextView nameView = new TextView(activity);
    nameView.setText(itemName);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    textArea.addView(nameView);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView descView = new TextView(activity);
        descView.setText(descriptionText);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
        descView.setPadding(0, dp(activity, 2), 0, 0);
        textArea.addView(descView);
    }

    itemLayout.addView(textArea);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(20);
    arrowView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    itemLayout.addView(arrowView);

    itemWrapper.addView(itemLayout);

    itemWrapper.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(activity, "surface")), Color.parseColor(getSettingsThemeColor(activity, "ripple")), 0));
    itemWrapper.setClickable(true);
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                vibrate(act, 32);
            }
            if (clickCallback != null) {
                clickCallback.run();
            }
        }
    });

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.put(itemKey, itemWrapper);
    }

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null) {
        SettingsState.settingsItemMeta.put(itemKey,new SettingsItemMeta(
        itemName, 
        descriptionText, 
        SettingsState.settingsCurrentLevel1, 
        SettingsState.settingsCurrentLevel2, 
        SettingsState.settingsCurrentLevel3, 
        SettingsState.settingsCurrentCategory,  
        "click"
        ));
     }
    container.addView(itemWrapper);
}

void addSettingsItemChoice(String categoryName, String itemName, String updateKey, String valueText, final Runnable clickCallback) {
    addSettingsItemChoiceWithKey(categoryName, itemName, categoryName + "_" + itemName, updateKey, valueText, clickCallback);
}

void addSettingsItemChoiceWithKey(String categoryName, String itemName, String itemKey, String updateKey, String valueText, final Runnable clickCallback) {
    if (SettingsState.settingsCategoryContainers == null || categoryName == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container == null) return;

    FrameLayout itemWrapper = new FrameLayout(activity);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));

    LinearLayout titleRow = new LinearLayout(activity);
    titleRow.setOrientation(LinearLayout.HORIZONTAL);
    titleRow.setGravity(Gravity.CENTER_VERTICAL);
    titleRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

    TextView nameView = new TextView(activity);
    nameView.setText(itemName);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    titleRow.addView(nameView);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(20);
    arrowView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    titleRow.addView(arrowView);

    itemLayout.addView(titleRow);

    final TextView valueView = new TextView(activity);
    valueView.setText(valueText);
    valueView.setTextSize(12);
    valueView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    valueView.setPadding(0, dp(activity, 2), 0, 0);
    itemLayout.addView(valueView);

    if (SettingsState.settingsItemTextViews == null) SettingsState.settingsItemTextViews = new HashMap();
    SettingsState.settingsItemTextViews.put(updateKey, valueView);

    itemWrapper.addView(itemLayout);

    itemWrapper.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(activity, "surface")), Color.parseColor(getSettingsThemeColor(activity, "ripple")), 0));
    itemWrapper.setClickable(true);
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                vibrate(act, 32);
            }
            if (clickCallback != null) {
                clickCallback.run();
            }
        }
    });

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.put(itemKey, itemWrapper);
    }

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null) {
        SettingsState.settingsItemMeta.put(itemKey, new SettingsItemMeta(
            itemName, null,
            SettingsState.settingsCurrentLevel1,
            SettingsState.settingsCurrentLevel2,
            SettingsState.settingsCurrentLevel3,
            SettingsState.settingsCurrentCategory,
            "choice"
        ));
    }

    container.addView(itemWrapper);
}

void updateSettingsItemText(String updateKey, String newText) {
    if (SettingsState.settingsItemTextViews != null && SettingsState.settingsItemTextViews.containsKey(updateKey)) {
        TextView textView = (TextView) SettingsState.settingsItemTextViews.get(updateKey);
        if (textView != null) {
            textView.setText(newText);
        }
    }
}

void addSettingsItemSwitch(String categoryName, String itemName, String configName, String keyName, boolean currentValue, final Runnable switchCallback) {
    String itemKey = keyName != null && !keyName.isEmpty() ? keyName : categoryName + "_" + itemName;
    addSettingsItemSwitchWithKey(categoryName, itemName, itemKey, configName, keyName, currentValue, switchCallback);
}

void addSettingsItemSwitchWithKey(String categoryName, String itemName, String itemKey, String configName, String keyName, boolean currentValue, final Runnable switchCallback) {
    if (SettingsState.settingsCategoryContainers == null || categoryName == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container == null) return;

    FrameLayout itemWrapper = new FrameLayout(activity);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));

    TextView nameView = new TextView(activity);
    nameView.setText(itemName);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    itemLayout.addView(nameView);

    final View switchView = createSettingsSwitchView(activity, currentValue, configName, keyName, itemName, switchCallback);
    if (switchView != null) {
        itemLayout.addView(switchView);
    }

    itemWrapper.addView(itemLayout);

    itemWrapper.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(activity, "surface")), Color.parseColor(getSettingsThemeColor(activity, "ripple")), 0));
    itemWrapper.setClickable(false);

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.put(itemKey, itemWrapper);
    }

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null) {
        SettingsState.settingsItemMeta.put(itemKey, new SettingsItemMeta(
            itemName, null,
            SettingsState.settingsCurrentLevel1,
            SettingsState.settingsCurrentLevel2,
            SettingsState.settingsCurrentLevel3,
            SettingsState.settingsCurrentCategory,
            "switch"
        ));
    }

    container.addView(itemWrapper);
}

void addSettingsSwitchItem(String categoryName, String itemName, String descriptionText, String keyName, boolean currentValue, final Runnable onChangeCallback) {
    if (SettingsState.settingsCategoryContainers == null || categoryName == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container == null) return;

    FrameLayout itemWrapper = new FrameLayout(activity);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));

    LinearLayout textArea = new LinearLayout(activity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

    TextView nameView = new TextView(activity);
    nameView.setText(itemName);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    textArea.addView(nameView);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView descView = new TextView(activity);
        descView.setText(descriptionText);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
        descView.setPadding(0, dp(activity, 2), 0, 0);
        textArea.addView(descView);
    }

    itemLayout.addView(textArea);

    final View switchView = createSettingsSwitchView(activity, currentValue, "settings", keyName, itemName, onChangeCallback);
    if (switchView != null) {
        itemLayout.addView(switchView);
    }

    itemWrapper.addView(itemLayout);

    itemWrapper.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(activity, "surface")), Color.parseColor(getSettingsThemeColor(activity, "ripple")), 0));
    itemWrapper.setClickable(true);
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            if (switchView != null) {
                switchView.performClick();
            }
        }
    });

    if (keyName != null && !keyName.isEmpty() && SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.put(keyName, itemWrapper);
    }
    if (keyName != null && !keyName.isEmpty() && SettingsState.settingsItemMeta != null) {
        SettingsState.settingsItemMeta.put(keyName, new SettingsItemMeta(
            itemName, descriptionText,
            SettingsState.settingsCurrentLevel1,
            SettingsState.settingsCurrentLevel2,
            SettingsState.settingsCurrentLevel3,
            SettingsState.settingsCurrentCategory,
            "switch"
        ));
    }

    container.addView(itemWrapper);
}

void addSettingsInputItem(String categoryName, String itemName, String descriptionText, String keyName, String hintText, String defaultValue, final Runnable onValueChanged) {
    if (SettingsState.settingsCategoryContainers == null || categoryName == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    String currentValue = getString("settings", keyName, defaultValue);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));

    TextView nameView = new TextView(activity);
    nameView.setText(itemName);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    itemLayout.addView(nameView);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView descView = new TextView(activity);
        descView.setText(descriptionText);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
        descView.setPadding(0, dp(activity, 2), 0, dp(activity, 8));
        itemLayout.addView(descView);
    }

    final EditText inputEdit = new EditText(activity);
    inputEdit.setText(currentValue);
    inputEdit.setHint(hintText != null ? hintText : "");
    inputEdit.setTextSize(14);
    inputEdit.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    inputEdit.setHintTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    inputEdit.setBackgroundColor(Color.parseColor(getSettingsThemeColor(activity, "surface")));
    inputEdit.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
    inputEdit.setSingleLine(true);
    itemLayout.addView(inputEdit);

    final String finalKeyName = keyName;
    inputEdit.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            String value = editable.toString().trim();
            putString("settings", finalKeyName, value);
            if (onValueChanged != null) {
                onValueChanged.run();
            }
        }
    });

    itemLayout.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(activity, "surface")), Color.parseColor(getSettingsThemeColor(activity, "ripple")), 0));
    itemLayout.setClickable(true);

    if (keyName != null && !keyName.isEmpty() && SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.put(keyName, itemLayout);
    }
    if (keyName != null && !keyName.isEmpty() && SettingsState.settingsItemMeta != null) {
        SettingsState.settingsItemMeta.put(keyName, new SettingsItemMeta(
            itemName, descriptionText,
            SettingsState.settingsCurrentLevel1,
            SettingsState.settingsCurrentLevel2,
            SettingsState.settingsCurrentLevel3,
            SettingsState.settingsCurrentCategory,
            "input"
        ));
    }

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container != null) {
        container.addView(itemLayout);
    }
}

String getThemeModeDisplayText() {
    String mode = getString("settings", "ui_theme_mode", "default");
    if ("system".equals(mode)) return "跟随系统";
    if ("light".equals(mode)) return "强制浅色";
    if ("dark".equals(mode)) return "强制深色";
    return "默认（推荐）";
}

String getScaleDisplayText() {
    String scale = getString("settings", "ui_dialog_scale", "");
    return scale.isEmpty() ? "1.0x (默认)" : scale + "x";
}

String getBgTypeDisplayText() {
    String bgType = getString("settings", "ui_bg_type", "color");
    if ("image".equals(bgType)) return "图片背景";
    if ("gradient".equals(bgType)) return "三色渐变";
    return "纯色背景";
}

String getFontTypeDisplayText() {
    String font = getString("settings", "ui_font_type", "default");
    if ("serif".equals(font)) return "衬线体";
    if ("sans".equals(font)) return "无衬线";
    if ("monospace".equals(font)) return "等宽";
    if ("bold".equals(font)) return "粗体";
    return "默认字体";
}

String getFontSizeDisplayText() {
    String size = getString("settings", "ui_font_size", "1.0");
    if ("0.85".equals(size)) return "小 (0.85x)";
    if ("1.15".equals(size)) return "中 (1.15x)";
    if ("1.3".equals(size)) return "大 (1.3x)";
    return "默认 (1.0x)";
}

String getThreadPriorityDisplayText() {
    String priority = getString("settings", "thread_pool_priority", "");
    if (priority.isEmpty()) return "5 (默认)";
    try {
        int p = Integer.parseInt(priority);
        if (p == 1) return "1 (最低)";
        if (p == 10) return "10 (最高)";
        return String.valueOf(p);
    } catch (Throwable exception) {
        return "5 (默认)";
    }
}

String getRejectPolicyDisplayText() {
    String policy = getString("settings", "thread_pool_reject_policy", "0");
    if ("1".equals(policy)) return "丢弃最新任务";
    if ("2".equals(policy)) return "抛出异常";
    if ("3".equals(policy)) return "调用者执行";
    return "丢弃最旧任务 (默认)";
}

String getUpdateChannelDisplayText() {
    String channel = getString("settings", "update_channel", "gitee");
    if ("github".equals(channel)) return "GitHub";
    return "Gitee (默认)";
}

void showUpdateChannelChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] channels = {"Gitee (默认)", "GitHub"};
    final String[] channelValues = {"gitee", "github"};

    String currentChannel = getString("settings", "update_channel", "gitee");
    int checkedItem = 0;
    for (int i = 0; i < channelValues.length; i++) {
        if (channelValues[i].equals(currentChannel)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("更新通道");
    builder.setSingleChoiceItems(channels, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "update_channel", channelValues[which]);
            updateSettingsItemText("item_update_channel", channels[which]);
            qqToast(2, "已切换至 " + channels[which]);
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

String getIconTypeDisplayText() {
    String iconPath = getString("settings", "iconPath", "");
    if (iconPath == null || iconPath.isEmpty()) return "未设置";
    if (iconPath.toLowerCase().endsWith(".gif")) return "动态图标 (GIF)";
    return "静态图标";
}

String getFpsDisplayText() {
    String delay = getString("settings", "gifDelay", "100");
    try {
        int d = Integer.parseInt(delay);
        int fps = d > 0 ? 1000 / d : 0;
        return fps + " FPS";
    } catch (Throwable exception) {
        return "点击选择";
    }
}

void showThemeModeChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] modes = {"默认（推荐）", "跟随系统", "强制浅色", "强制深色"};
    final String[] modeValues = {"default", "system", "light", "dark"};

    String currentMode = getString("settings", "ui_theme_mode", "default");
    int checkedItem = 0;
    for (int i = 0; i < modeValues.length; i++) {
        if (modeValues[i].equals(currentMode)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("主题模式");
    builder.setSingleChoiceItems(modes, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            String newValue = modeValues[which];
            putString("settings", "ui_theme_mode", newValue);
            if ("dark".equals(newValue)) {
                putBoolean("settings", "黑白", true);
            } else if ("light".equals(newValue)) {
                putBoolean("settings", "黑白", false);
            }
            updateSettingsItemText("ui_theme_mode", modes[which]);
            qqToast(2, "主题已更改");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showBgTypeChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] types = {"纯色背景", "三色渐变", "图片背景"};
    final String[] typeValues = {"color", "gradient", "image"};

    String currentType = getString("settings", "ui_bg_type", "color");
    int checkedItem = 0;
    for (int i = 0; i < typeValues.length; i++) {
        if (typeValues[i].equals(currentType)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("背景类型");
    builder.setSingleChoiceItems(types, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_bg_type", typeValues[which]);
            updateSettingsItemText("ui_bg_type", types[which]);
            qqToast(2, "背景类型已更改");
            dialog.dismiss();
            cleanupAllDialogs();
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    showSettingsMenu(activity, "设置", "背景样式", null);
                }
            }, 200);
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showPresetColorDialog(final Activity activity) {
    if (activity == null) return;
    final String[] colorNames = isThemeDark(activity)
        ? new String[]{"默认黑", "深空灰", "午夜蓝", "暗夜紫", "墨绿", "酒红", "深褐", "深青灰"}
        : new String[]{"默认白", "米白", "柔粉", "天蓝", "薄荷", "香芋紫", "柠檬黄", "浅灰"};
    final String[] colorValues = isThemeDark(activity)
        ? new String[]{"#FF1E1E1E", "#FF2D2D2D", "#FF1A237E", "#FF4A148C", "#FF1B5E20", "#FF880E4F", "#FF3E2723", "#FF263238"}
        : new String[]{"#FFFFFF", "#FFF8F0", "#FFF0F5", "#E6F7FF", "#F0FFF0", "#E6E6FA", "#FFFFF0", "#F5F5F5"};

    final String keyName = isThemeDark(activity) ? "ui_bg_color_dark" : "ui_bg_color_light";
    String currentColor = getString("settings", keyName, isThemeDark(activity) ? "#FF1E1E1E" : "#FFFFFF");
    int checkedItem = 0;
    for (int i = 0; i < colorValues.length; i++) {
        if (colorValues[i].equals(currentColor)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("预设颜色");
    builder.setSingleChoiceItems(colorNames, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", keyName, colorValues[which]);
            qqToast(2, "已选择: " + colorNames[which]);
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showPresetGradientDialog(final Activity activity) {
    if (activity == null) return;
    final String[] gradNames = {"默认渐变", "落日余晖", "深海幽蓝", "清新森林", "梦幻紫罗兰", "极光", "黑金", "银灰"};
    final String[] gradValues = isThemeDark(activity)
        ? new String[]{"#FF2C2C2C,#FF121212,#FF2C2C2C", "#FF4E342E,#FF3E2723,#FF4E342E", "#FF1A237E,#FF0D47A1,#FF1A237E", "#FF1B5E20,#FF33691E,#FF1B5E20", "#FF4A148C,#FF311B92,#FF4A148C", "#FF006064,#FF004D40,#FF006064", "#FF212121,#FF000000,#FF212121", "#FF2D2D2D,#FF1A1A1A,#FF2D2D2D"}
        : new String[]{"#FFFFFFFF,#FFF5F5F5,#FFFFFFFF", "#FFFFE0B2,#FFFFCC80,#FFFFE0B2", "#FFBBDEFB,#FF90CAF9,#FFBBDEFB", "#FFC8E6C9,#FFA5D6A7,#FFC8E6C9", "#FFE1BEE7,#FFCE93D8,#FFE1BEE7", "#FFB2EBF2,#FF80DEEA,#FFB2EBF2", "#FFF5F5F5,#FFE0E0E0,#FFF5F5F5", "#FFF8F8F8,#FFECECEC,#FFF8F8F8"};

    final String keyName = isThemeDark(activity) ? "ui_bg_gradient_dark" : "ui_bg_gradient_light";
    String currentGrad = getString("settings", keyName, "");
    int checkedItem = -1;
    for (int i = 0; i < gradValues.length; i++) {
        if (gradValues[i].equals(currentGrad)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("预设渐变");
    builder.setSingleChoiceItems(gradNames, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", keyName, gradValues[which]);
            qqToast(2, "已选择: " + gradNames[which]);
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showFontTypeChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] fonts = {"默认字体", "衬线体", "无衬线", "等宽", "粗体"};
    final String[] fontValues = {"default", "serif", "sans", "monospace", "bold"};

    String currentFont = getString("settings", "ui_font_type", "default");
    int checkedItem = 0;
    for (int i = 0; i < fontValues.length; i++) {
        if (fontValues[i].equals(currentFont)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("字体风格");
    builder.setSingleChoiceItems(fonts, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_font_type", fontValues[which]);
            updateSettingsItemText("ui_font_type", fonts[which]);
            qqToast(2, "字体已更改");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showFontSizeChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] sizes = {"小 (0.85x)", "默认 (1.0x)", "中 (1.15x)", "大 (1.3x)"};
    final String[] sizeValues = {"0.85", "1.0", "1.15", "1.3"};

    String currentSize = getString("settings", "ui_font_size", "1.0");
    int checkedItem = 1;
    for (int i = 0; i < sizeValues.length; i++) {
        if (sizeValues[i].equals(currentSize)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("字体大小");
    builder.setSingleChoiceItems(sizes, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_font_size", sizeValues[which]);
            updateSettingsItemText("ui_font_size", sizes[which]);
            qqToast(2, "字体大小已更改");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showThreadPriorityChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] priorities = {"1 (最低)", "2", "3", "4", "5 (默认)", "6", "7", "8", "9", "10 (最高)"};
    final String[] priorityNumbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

    String currentPriority = getString("settings", "thread_pool_priority", "");
    int checkedItem = 4;
    for (int i = 0; i < priorityNumbers.length; i++) {
        if (priorityNumbers[i].equals(currentPriority)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("线程优先级");
    builder.setSingleChoiceItems(priorities, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "thread_pool_priority", priorityNumbers[which]);
            updateSettingsItemText("thread_pool_priority", priorities[which]);
            qqToast(2, "优先级已设置");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showRejectPolicyChoiceDialog(final Activity activity) {
    if (activity == null) return;
    final String[] policies = {"丢弃最旧任务 (默认)", "丢弃最新任务", "抛出异常", "调用者执行"};
    final String[] policyValues = {"0", "1", "2", "3"};

    String currentPolicy = getString("settings", "thread_pool_reject_policy", "0");
    int checkedItem = 0;
    for (int i = 0; i < policyValues.length; i++) {
        if (policyValues[i].equals(currentPolicy)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("任务满载策略");
    builder.setSingleChoiceItems(policies, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "thread_pool_reject_policy", policyValues[which]);
            updateSettingsItemText("thread_pool_reject_policy", policies[which]);
            qqToast(2, "策略已设置");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showFpsChoiceDialog(final Activity activity) {
    if (activity == null) return;
    float refreshRate = 60f;
    try {
        Display display = activity.getWindowManager().getDefaultDisplay();
        refreshRate = display.getRefreshRate();
    } catch (Throwable exception) {
        traceLog("setwindow", "get refresh rate: " + exception.getMessage());
    }

    final List fpsList = new ArrayList();
    final List delayList = new ArrayList();

    fpsList.add("30 FPS (默认)");
    delayList.add("33");

    if (refreshRate >= 59.9f) {
        fpsList.add("60 FPS");
        delayList.add("17");
    }
    if (refreshRate >= 89.9f) {
        fpsList.add("90 FPS");
        delayList.add("11");
    }
    if (refreshRate >= 119.9f) {
        fpsList.add("120 FPS");
        delayList.add("8");
    }
    if (refreshRate >= 143.9f) {
        fpsList.add("144 FPS");
        delayList.add("7");
    }

    final String[] fpsArray = (String[]) fpsList.toArray(new String[0]);
    final String[] delayArray = (String[]) delayList.toArray(new String[0]);

    String currentDelay = getString("settings", "gifDelay", "100");
    int checkedItem = -1;
    for (int i = 0; i < delayArray.length; i++) {
        if (delayArray[i].equals(currentDelay)) {
            checkedItem = i;
            break;
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("帧率设置");
    builder.setSingleChoiceItems(fpsArray, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "gifDelay", delayArray[which]);
            updateSettingsItemText("gifDelay", fpsArray[which]);
            qqToast(2, "帧率已设置");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showScaleSliderDialog(final Activity activity) {
    if (activity == null) return;
    String currentScaleString = getString("settings", "ui_dialog_scale", "1.0");
    float currentScale = 1.0f;
    try {
        currentScale = Float.parseFloat(currentScaleString);
    } catch (Throwable exception) {
        traceLog("setwindow", "parse scale: " + exception.getMessage());
    }

    LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 20));

    final TextView valueText = new TextView(activity);
    valueText.setText(String.format("%.2f", currentScale) + "x");
    valueText.setTextSize(28);
    valueText.setTypeface(null, Typeface.BOLD);
    valueText.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    valueText.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
    valueParams.bottomMargin = dp(activity, 20);
    rootLayout.addView(valueText, valueParams);

    LinearLayout sliderRow = new LinearLayout(activity);
    sliderRow.setOrientation(LinearLayout.HORIZONTAL);
    sliderRow.setGravity(Gravity.CENTER_VERTICAL);

    final float MIN_SCALE = 0.25f;
    final float MAX_SCALE = 2.0f;
    final float SCALE_STEP = 0.01f;
    final int MAX_PROGRESS = (int) ((MAX_SCALE - MIN_SCALE) / SCALE_STEP);

    TextView btnMinus = createButton(activity, "−", Color.parseColor(getSettingsThemeColor(activity, "primary")), Color.TRANSPARENT, 24f, 0, 12, 0, false, 0, 0, null);
    sliderRow.addView(btnMinus);

    final SeekBar seekBar = new SeekBar(activity);
    seekBar.setMax(MAX_PROGRESS);
    seekBar.setProgress((int) ((currentScale - MIN_SCALE) / SCALE_STEP));
    seekBar.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    sliderRow.addView(seekBar);

    TextView btnPlus = createButton(activity, "+", Color.parseColor(getSettingsThemeColor(activity, "primary")), Color.TRANSPARENT, 24f, 0, 12, 0, false, 0, 0, null);
    sliderRow.addView(btnPlus);

    rootLayout.addView(sliderRow);

    final Runnable updateValue = new Runnable() {
        public void run() {
            float scale = MIN_SCALE + seekBar.getProgress() * SCALE_STEP;
            String scaleText = String.format("%.2f", scale) + "x";
            valueText.setText(scaleText);
            putString("settings", "ui_dialog_scale", String.format("%.2f", scale));
            updateSettingsItemText("ui_dialog_scale", scaleText);
        }
    };

    btnMinus.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            seekBar.setProgress(Math.max(0, seekBar.getProgress() - 10));
            updateValue.run();
        }
    });

    btnPlus.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            seekBar.setProgress(Math.min(MAX_PROGRESS, seekBar.getProgress() + 10));
            updateValue.run();
        }
    });

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateValue.run();
        }
        public void onStartTrackingTouch(SeekBar seekBar) {}
        public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert);
    builder.setTitle("弹窗大小");
    builder.setView(rootLayout);
    builder.setPositiveButton("确定", null);
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

void showSettingsPreviewPopup(Activity activity) {
    if (activity == null) return;
    boolean isDark = isThemeDark(activity);

    LinearLayout previewContent = new LinearLayout(activity);
    previewContent.setOrientation(LinearLayout.VERTICAL);
    previewContent.setGravity(Gravity.CENTER);
    previewContent.setPadding(dp(activity, 24), dp(activity, 24), dp(activity, 24), dp(activity, 24));

    TextView previewTitle = new TextView(activity);
    previewTitle.setText("预览标题");
    previewTitle.setTextSize(18);
    previewTitle.setTypeface(null, Typeface.BOLD);
    previewTitle.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    previewTitle.setGravity(Gravity.CENTER);
    previewContent.addView(previewTitle);

    TextView previewText = new TextView(activity);
    previewText.setText("这是一段测试文本，用于预览设置效果。");
    previewText.setTextSize(14);
    previewText.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    previewText.setGravity(Gravity.CENTER);
    previewText.setPadding(0, dp(activity, 12), 0, dp(activity, 16));
    previewContent.addView(previewText);

    LinearLayout buttonRow = new LinearLayout(activity);
    buttonRow.setOrientation(LinearLayout.HORIZONTAL);
    buttonRow.setGravity(Gravity.CENTER);

    TextView toastButton = createButton(activity, "测试Toast", Color.parseColor(getSettingsThemeColor(activity, "primary")), Color.parseColor(getSettingsThemeColor(activity, "surface")), 14f, 20, 24, 12, false, 0, 0, null);
    toastButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            qqToast(2, "这是一个测试Toast");
        }
    });
    buttonRow.addView(toastButton);

    previewContent.addView(buttonRow);

    AlertDialog.Builder previewBuilder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    previewBuilder.setTitle("设置预览");
    previewBuilder.setView(previewContent);
    previewBuilder.setPositiveButton("关闭", null);

    AlertDialog previewDialog = previewBuilder.show();
    applyUiTheme(activity, previewDialog);
}
