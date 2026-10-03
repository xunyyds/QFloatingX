import me.yxp.qfun.activity.BaseComposeActivity;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;

class SettingsItemMeta {
    String name;
    String description;
    String level1;
    String level2;
    String level3;
    String category;
    String type;
    String viewType;
    String targetPage;
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
        this.viewType = type;
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
    static boolean settingsActivityRegistered;
    static TextView settingsWelcomeView;
    static boolean settingsIndexMode;
    static float settingsHostDensity;
    static List settingsCategories;
    static List settingsSubPages;
    static View settingsHighlightView;
    static Map settingsItemViews;
    static ScrollView settingsScrollView;
    static String settingsPendingHighlightKey;
    static int settingsPendingHighlightDepth = -1;
    static Activity settingsCurrentActivityRef;
    static Map settingsItemMeta;
    static String settingsCurrentLevel1;
    static String settingsCurrentLevel2;
    static String settingsCurrentLevel3;
    static String settingsCurrentCategory;
    static long settingsSearchToken;
    static String settingsHistoryRaw;
    static boolean settingsIndexReady;
    static String settingsNavLevel1;
    static String settingsNavLevel2;
    static String settingsNavLevel3;
    static String settingsNavHighlightKey;
    static String settingsNavQuery;
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
    Activity ref = SettingsState.settingsCurrentActivityRef;
    if (ref instanceof SettingsActivity && !ref.isFinishing()) {
        return ref;
    }
    Activity activity = getNowActivity();
    if (activity != null) {
        return activity;
    }
    if (ref != null && !ref.isFinishing()) {
        return ref;
    }
    return null;
}

private volatile long themeColorCacheTime = 0L;
private final java.util.Hashtable themeColorCache = new java.util.Hashtable();

String getSettingsThemeColor(Activity activity, String colorName) {
    long nowT = System.currentTimeMillis();
    if (nowT - themeColorCacheTime > 5000L) {
        themeColorCache.clear();
        themeColorCacheTime = nowT;
    }
    String cacheKey = (isThemeDark(activity) ? "d|" : "l|") + colorName;
    String cached = (String) themeColorCache.get(cacheKey);
    if (cached != null) return cached;
    String resolved = resolveSettingsThemeColor(activity, colorName);
    themeColorCache.put(cacheKey, resolved);
    return resolved;
}

String resolveSettingsThemeColor(Activity activity, String colorName) {
    boolean isDarkTheme = isThemeDark(activity);
    String customBg = getString("settings", isDarkTheme ? "ui_bg_color_dark" : "ui_bg_color_light", "");
    String customText = getString("settings", isDarkTheme ? "ui_text_color_dark" : "ui_text_color_light", "");
    boolean hasCustomBg = customBg != null && !customBg.isEmpty() && isValidHexColor(customBg);
    boolean hasCustomText = customText != null && !customText.isEmpty() && isValidHexColor(customText);
    String autoTextColor = isDarkTheme ? "#FFEFEFEF" : "#FF1A1A1A";
    if (!hasCustomText && hasCustomBg) {
        try {
            int bgColor = pc(customBg);
            double luminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255.0;
            autoTextColor = luminance > 0.5 ? "#FF1A1A1A" : "#FFEFEFEF";
        } catch (Throwable e) { traceLog("setwindow_log", "[getSettingsThemeColor] 异常: " + e); }
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
        traceLog("setwindow_log", "[getStatusBarHeightValue] 获取状态栏高度: " + exception.getMessage());
    }
    return resultHeight;
}

int getNavigationBarHeightValue(Context context) {
    if (context == null) return 0;
    int resultHeight = 0;
    try {
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            resultHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
    } catch (Throwable exception) {
        traceLog("setwindow_log", "[getNavigationBarHeightValue] 获取导航栏高度: " + exception.getMessage());
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
        traceLog("setwindow_log", "[setSettingsImmersiveStatusBar] 设置沉浸式状态栏: " + exception.getMessage());
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
        traceLog("setwindow_log", "[openSettingsExternalBrowser] 打开设置外部浏览器: " + exception.getMessage());
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
                trackBackground.setColor(isOn ? pc(getSettingsThemeColor(act, "switch_on")) : pc(getSettingsThemeColor(act, "switch_off")));
            } else {
                trackBackground.setColor(isOn ? pc("#FF34C759") : pc("#FFE5E5E5"));
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
    SettingsState.settingsPendingHighlightDepth = -1;
    SettingsState.settingsCurrentLevel1 = null;
    SettingsState.settingsCurrentLevel2 = null;
    SettingsState.settingsCurrentLevel3 = null;
}

void cleanupAllDialogs() {
    Activity ref = SettingsState.settingsCurrentActivityRef;
    if (ref instanceof SettingsActivity && !ref.isFinishing()) {
        try {
            ((SettingsActivity) ref).settingsClose();
        } catch (Throwable exception) {
            traceLog("setwindow_log", "[cleanupAllDialogs] 关闭设置页异常: " + exception);
        }
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
    highlightBg.setColor(pc(isDark ? "#338AB4F8" : "#332196F3"));
    highlightBg.setCornerRadius(dp(activity, 16));

    targetView.setBackground(highlightBg);

    uiHandler.postDelayed(new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                targetView.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(act), pc(getSettingsThemeColor(act, "ripple")), 0));
            }

            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (SettingsState.settingsHighlightView == targetView) targetView.setBackground(highlightBg);

                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (SettingsState.settingsHighlightView == targetView) {
                                Activity act2 = getSettingsCurrentActivity();
                                if (act2 != null) {
                                    targetView.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(act2), pc(getSettingsThemeColor(act2, "ripple")), 0));
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

void ensureSettingsActivityRegistered() {
    if (SettingsState.settingsActivityRegistered) return;
    try {
        registerActivity(SettingsActivity.class);
        SettingsState.settingsActivityRegistered = true;
    } catch (Throwable exception) {
        traceLog("setwindow_log", "[ensureSettingsActivityRegistered] 注册设置Activity: " + exception);
    }
}

boolean isSettingsActivityOpen() {
    Activity ref = SettingsState.settingsCurrentActivityRef;
    return ref instanceof SettingsActivity && !ref.isFinishing();
}

void launchSettingsActivity(Activity activity, int chatType, String peerUin, String peerName) {
    if (activity == null) return;
    try {
        ensureSettingsActivityRegistered();
        try {
            SettingsState.settingsHostDensity = activity.getResources().getDisplayMetrics().density;
        } catch (Throwable ignore) {}
        Intent intent = new Intent();
        intent.setClassName(activity, SettingsActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("chatType", chatType);
        intent.putExtra("peerUin", peerUin != null ? peerUin : "");
        intent.putExtra("peerName", peerName != null ? peerName : "");
        activity.startActivity(intent);
    } catch (Throwable exception) {
        traceLog("setwindow_log", "[launchSettingsActivity] 打开设置Activity: " + exception);
        Toast("打开设置失败: " + exception.getMessage());
    }
}

void showSettingsMenu(final Activity activity, final String level1Title, final String level2Title, final String level3Title) {
    if (activity == null || activity.isFinishing()) return;

    if (activity instanceof SettingsActivity) {
        final SettingsActivity settingsAct = (SettingsActivity) activity;
        SettingsState.settingsNavLevel1 = level1Title;
        SettingsState.settingsNavLevel2 = level2Title;
        SettingsState.settingsNavLevel3 = level3Title;
        SettingsState.settingsNavHighlightKey = null;
        SettingsState.settingsNavQuery = null;
        try {
            if (settingsAct.settingsPageHandler != null) {
                settingsAct.settingsPageHandler.post(new Runnable() {
                    public void run() { settingsAct.settingsRunNav(); }
                });
            } else {
                settingsAct.settingsRunNav();
            }
        } catch (Throwable e) {
            traceLog("setwindow_log", "[showSettingsMenu] 导航失败: " + e);
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

private Bitmap cachedBottomIconGithub = null;
private Bitmap cachedBottomIconQq = null;

Bitmap getSettingsIconBitmap(String path, boolean isGithub) {
    if (isGithub) {
        if (cachedBottomIconGithub != null && !cachedBottomIconGithub.isRecycled()) return cachedBottomIconGithub;
    } else {
        if (cachedBottomIconQq != null && !cachedBottomIconQq.isRecycled()) return cachedBottomIconQq;
    }
    Bitmap bm = null;
    try {
        File f = new File(path);
        if (f.exists()) bm = BitmapFactory.decodeFile(path);
    } catch (Throwable t) { traceLog("setwindow_log", "[getSettingsIconBitmap] " + t.getMessage()); }
    if (isGithub) cachedBottomIconGithub = bm; else cachedBottomIconQq = bm;
    return bm;
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
    Bitmap githubIconBm = getSettingsIconBitmap(rootPath + "GitHub.png", true);
    if (githubIconBm != null) {
        projectBtn.setImageBitmap(githubIconBm);
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
    text1.setTextColor(pc(getSettingsThemeColor(currentActivity, "on_surface_variant")));
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
    Bitmap qqIconBm = getSettingsIconBitmap(rootPath + "QQ.png", false);
    if (qqIconBm != null) {
        qqBtn.setImageBitmap(qqIconBm);
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
    text2.setTextColor(pc(getSettingsThemeColor(currentActivity, "on_surface_variant")));
    text2.setGravity(Gravity.CENTER);
    text2.setPadding(0, dp(activity, 4), 0, 0);
    icon2Container.addView(text2);

    iconRow.addView(icon2Container);

    bottomArea.addView(iconRow);

    String scriptVer = "";
    try { scriptVer = readprop(pluginPath + "/info.prop", "versionCode"); } catch (Throwable ignore) {}
    String qqVer = "";
    try { qqVer = me.yxp.qfun.utils.qq.HostInfo.INSTANCE.getVersionName(); } catch (Throwable ignore) {}
    TextView verLine = new TextView(activity);
    verLine.setText("QFloatingX v" + scriptVer + "  ·  QQ " + qqVer);
    verLine.setGravity(Gravity.CENTER);
    verLine.setTextColor(pc(isDark ? "#555555" : "#AAAAAA"));
    verLine.setTextSize(10);
    verLine.setPadding(0, dp(activity, 4), 0, 0);
    bottomArea.addView(verLine);

    TextView footer = new TextView(activity);
    footer.setText("Generated by QFloatingX");
    footer.setGravity(Gravity.CENTER);
    footer.setTextColor(pc(isDark ? "#555555" : "#AAAAAA"));
    footer.setTextSize(10);
    footer.setPadding(0, dp(activity, 2), 0, dp(activity, 4));
    bottomArea.addView(footer);

    Activity cur = getSettingsCurrentActivity();
    if (cur instanceof SettingsActivity) {
        ((SettingsActivity) cur).settingsMountFooter(bottomArea);
    } else if (SettingsState.settingsListContainer != null) {
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
    applyUiTheme(activity, alertDialog, 0);
}

void addIndexItem(String key, String name, String description, String level1, String level2, String category, String type) {
    SettingsState.settingsItemMeta.put(key, new SettingsItemMeta(name, description, level1, level2, null, category, type));
}

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

void buildSettingsMenuContent(Activity activity, String level1Title, String level2Title, String level3Title) {
    if (activity == null) return;
    if (level1Title == null) {
        buildSettingsHomeContent(activity);
        return;
    }

    if (level2Title == null) {
        buildLevel2MenuContent(activity, level1Title);
        return;
    }

    buildLevel3MenuContent(activity, level1Title, level2Title, level3Title);
}

void addSettingsCategoryEntry(String cardIcon, String cardTitle, String cardDesc, final String targetCategory) {
    if (targetCategory == null) return;
    final String itemKey = "cat_" + targetCategory;
    if (SettingsState.settingsIndexMode) {
        registerSettingsIndexEntry(itemKey, cardTitle, cardDesc, "category");
        if (SettingsState.settingsCategories == null) SettingsState.settingsCategories = new ArrayList();
        if (!SettingsState.settingsCategories.contains(targetCategory)) SettingsState.settingsCategories.add(targetCategory);
        return;
    }
    final String fullTitle = (cardIcon == null || cardIcon.length() == 0) ? cardTitle : (cardIcon + " " + cardTitle);
    addSettingsGridCard(itemKey, fullTitle, cardDesc, new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) showSettingsMenu(act, targetCategory, null, null);
    }});
    registerSettingsIndexEntry(itemKey, cardTitle, cardDesc, "category");
}

void addSettingsSubPageEntry(final String categoryName, String iconAndName, String descriptionText, String itemKey, final String subLevel2) {
    if (SettingsState.settingsIndexMode) {
        registerSettingsIndexEntry(itemKey, iconAndName, descriptionText, "page");
        ((SettingsItemMeta) SettingsState.settingsItemMeta.get(itemKey)).targetPage = subLevel2;
        if (SettingsState.settingsSubPages == null) SettingsState.settingsSubPages = new ArrayList();
        String pathKey = categoryName + "|" + subLevel2;
        if (!SettingsState.settingsSubPages.contains(pathKey)) SettingsState.settingsSubPages.add(pathKey);
        return;
    }
    addSettingsGridCard(itemKey, iconAndName, descriptionText, new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) showSettingsMenu(act, categoryName, subLevel2, null);
    }});
    registerSettingsIndexEntry(itemKey, iconAndName, descriptionText, "page");
    ((SettingsItemMeta) SettingsState.settingsItemMeta.get(itemKey)).targetPage = subLevel2;
}

void buildSettingsHomeContent(Activity activity) {
    if (activity == null) return;
    if (!SettingsState.settingsIndexMode && SettingsState.settingsListContainer == null) return;

    SettingsState.settingsCurrentCategory = "首页";

    if (!SettingsState.settingsIndexMode) {
        String welcomeText = getString("settings", "welcome_text", "欢迎使用");
        if (welcomeText == null || welcomeText.trim().isEmpty()) welcomeText = "欢迎使用";
        TextView welcomeView = new TextView(activity);
        welcomeView.setText(welcomeText);
        welcomeView.setTextSize(13);
        welcomeView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        welcomeView.setPadding(dp(activity, 28), dp(activity, 12), dp(activity, 28), dp(activity, 4));
        SettingsState.settingsListContainer.addView(welcomeView);
        SettingsState.settingsWelcomeView = welcomeView;
    }

    addSettingsCategoryEntry("", "悬浮窗与快捷", "悬浮窗开关 · 图标与大小", "悬浮窗与快捷");
    addSettingsCategoryEntry("", "消息与互动", "消息统计 · 双击消息", "消息与互动");
    addSettingsCategoryEntry("", "定位与网络", "模拟定位 · 经纬度 · 空间操作", "定位与网络");
    addSettingsCategoryEntry("", "功能扩展", "功能热插拔 · 脚本 · 模块", "功能扩展");

    addSettingsGridCard("home_html", "HTML 浏览器", "本地网页 · 文件管理", new Runnable() { public void run() {
        final Activity act = getSettingsCurrentActivity();
        if (act instanceof SettingsActivity) {
            final SettingsActivity sa = (SettingsActivity) act;
            sa.settingsPushContentViewRaw("HTML 浏览器", buildHtmlPageView(sa));
        }
    }});

    addSettingsCategoryEntry("", "界面与显示", "主题 · 背景 · 字体 · 欢迎语", "界面与显示");
    addSettingsCategoryEntry("", "提示与通知", "Toast · 加载提示 · 常驻通知", "提示与通知");
    addSettingsCategoryEntry("", "性能与调试", "线程池 · 运行状态 · 日志", "性能与调试");
    addSettingsCategoryEntry("", "输入框提示", "开关 · 提示词 · 可用变量", "输入框提示");
    addSettingsCategoryEntry("", "关于", "更新 · 版本 · 重置", "关于");
}

void addSettingsGridCard(String rowTag, String cardTitle, String cardDesc, final Runnable clickCallback) {
    if (SettingsState.settingsIndexMode) {
        registerSettingsIndexEntry(rowTag, cardTitle, cardDesc, "click");
        return;
    }
    if (SettingsState.settingsListContainer == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
    card.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), dp(activity, 16)));
    card.setClipToOutline(true);
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
    cardParams.setMargins(dp(activity, 12), dp(activity, 5), dp(activity, 12), dp(activity, 5));
    card.setLayoutParams(cardParams);

    LinearLayout textArea = new LinearLayout(activity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

    TextView titleView = new TextView(activity);
    titleView.setText(cardTitle);
    titleView.setTextSize(16);
    titleView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    textArea.addView(titleView);

    if (cardDesc != null && cardDesc.length() > 0) {
        TextView descView = new TextView(activity);
        descView.setText(cardDesc);
        descView.setTextSize(12);
        descView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        descView.setPadding(0, dp(activity, 3), 0, 0);
        textArea.addView(descView);
    }
    card.addView(textArea);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(18);
    arrowView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    card.addView(arrowView);
	
    card.setClickable(true);
	card.setOnClickListener(new View.OnClickListener() {
	    long qfxLastClick = 0L;
	    public void onClick(View view) {
	        long now = System.currentTimeMillis();
	        if (now - qfxLastClick < 1200L) return;
	        qfxLastClick = now;
	        Activity act = getSettingsCurrentActivity();
	        if (act != null) vibrate(act, 32);
	        if (clickCallback != null) clickCallback.run();
	    }
	});
	SettingsState.settingsListContainer.addView(card);
    if (SettingsState.settingsItemViews == null) SettingsState.settingsItemViews = new HashMap();
    SettingsState.settingsItemViews.put(rowTag, card);
    registerSettingsIndexEntry(rowTag, cardTitle, cardDesc, "click");
}

void buildLevel2MenuContent(Activity activity, String level1Title) {
    if (activity == null || level1Title == null) return;

    if ("悬浮窗与快捷".equals(level1Title)) {
        addSettingsCategory("开关", "");
        boolean floatState = getBoolean("settings", "开关", false);
        addSettingsItemSwitchWithKey( "悬浮窗", "settings", "开关", "悬浮球显示", floatState, new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act == null) return;
            boolean on = getBoolean("settings", "开关", false);
            try {
                if (on) 启动悬浮窗(act);
                else 停止悬浮窗(act);
                applyFloatWindowMenuText();
            } catch (Throwable e) { traceLog("setwindow_log", "[快捷开关悬浮窗] 异常: " + e); }
        }});

        addSettingsSubPageEntry("悬浮窗与快捷", "悬浮窗设置", "图标 · 大小 · 灵敏度 · 透明度", "item_float_window", "悬浮窗设置");
    }

    if ("消息与互动".equals(level1Title)) {
        addSettingsCategory("开关", "");
        boolean msgStatsState = getBoolean("settings", "消息统计开关", false);
        addSettingsItemSwitchWithKey( "消息统计", "settings", "消息统计开关", "今日消息计数", msgStatsState, new Runnable() { public void run() {
            boolean on = getBoolean("settings", "消息统计开关", false);
            try {
                if (on) startWriteThread();
                else stopWriteThread();
            } catch (Throwable e) { traceLog("setwindow_log", "[消息统计开关] 异常: " + e); }
        }});
        boolean doubleClickMsgState = getBoolean("settings", "双击消息开关", false);
        addSettingsItemSwitchWithKey( "双击消息", "settings", "双击消息开关", "预览页拦截增强", doubleClickMsgState, null);

        addSettingsGridCard("m1", "消息统计", "查看今日 / 累计数据", new Runnable() { public void run() {
            final Activity act = getSettingsCurrentActivity();
            if (act instanceof SettingsActivity) {
                LinearLayout statsView = buildStatsContentView(act);
                ((SettingsActivity) act).settingsPushContentView("消息统计", statsView);
                bindStatsViewCache(statsView);
            }
        }});
    }

    if ("定位与网络".equals(level1Title)) {
        addSettingsCategory("开关", "");
        boolean mockLocationState = getBoolean("模拟定位开关", "模拟定位开关", false);
        addSettingsItemSwitchWithKey( "模拟定位", null, null, "全局定位覆盖", mockLocationState, new Runnable() { public void run() {
            setMockLocationEnabled(!getBoolean("模拟定位开关", "模拟定位开关", false));
        }});

        addSettingsCategory("经纬度", "");
        addSettingsLocationBlock("经纬度");

        addSettingsGridCard("loc2", "空间操作", "秒赞 · 秒评 · 评论内容", new Runnable() { public void run() {
            final Activity act = getSettingsCurrentActivity();
            if (act instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) act;
                sa.settingsPushContentView("空间操作", buildQzoneContentView(sa, false, new Runnable() { public void run() {
                    sa.settingsPopPage();
                }}));
            }
        }});
    }

    if ("功能扩展".equals(level1Title)) {
        addSettingsGridCard("e1", "功能热插拔", "脚本扩展 · 独立调度", new Runnable() { public void run() {
            final Activity act = getSettingsCurrentActivity();
            if (act instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) act;
                final String gid = (sa.settingsChatType == 2) ? sa.settingsPeerUin : "";
                sa.settingsPushContentViewRaw("功能热插拔", buildHotPlugContentView(sa, gid, sa.settingsPeerName, new Runnable() { public void run() {
                    sa.settingsPopPage();
                }}, true));
            }
        }});
        addSettingsGridCard("e2", "Java脚本", "QFun 脚本列表", new Runnable() { public void run() { 跳转到页面("me.yxp.qfun.activity.PluginActivity"); }});
        addSettingsGridCard("e4", "模块设置", "QFun 模块页", new Runnable() { public void run() { 跳转到页面("me.yxp.qfun.activity.SettingActivity"); }});
        addSettingsGridCard("e5", "取消 / 重载", "卸载或重新加载脚本", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showReOrUnDialog(act);
        }});
    }

    if ("界面与显示".equals(level1Title)) {
        addSettingsCategory("界面", "");
        addSettingsSubPageEntry("界面与显示", "基础模式", "主题、弹窗大小、振动反馈", "item_basic_mode", "基础模式");
        addSettingsSubPageEntry("界面与显示", "背景样式", "背景类型、颜色、图片", "item_bg_icon", "背景样式");
        addSettingsSubPageEntry("界面与显示", "字体样式", "字体风格、大小、颜色", "item_font_style", "字体样式");

        addSettingsCategory("首页", "");
        addSettingsInputItem( "欢迎语", "首页顶部显示的文字，留空则显示「欢迎使用」", "welcome_text", "如: 欢迎使用", "欢迎使用", new Runnable() { public void run() {
            if (SettingsState.settingsWelcomeView != null) {
                String wt = getString("settings", "welcome_text", "欢迎使用");
                if (wt == null || wt.trim().isEmpty()) wt = "欢迎使用";
                SettingsState.settingsWelcomeView.setText(wt);
            }
        }});
    }

    if ("提示与通知".equals(level1Title)) {
        addSettingsCategory("开关", "");
        boolean loadTipState = getBoolean("settings", "加载提示", false);
        addSettingsItemSwitchWithKey( "加载提示", "settings", "加载提示", "脚本加载时显示提示", loadTipState, null);
        boolean loadNotifyState = getBoolean("settings", "加载通知", false);
        addSettingsItemSwitchWithKey( "加载通知", "settings", "加载通知", "脚本加载时发送通知提示", loadNotifyState, null);
        boolean keepNotifyState = getBoolean("settings", "常驻通知", false);
        addSettingsItemSwitchWithKey( "常驻通知", "settings", "常驻通知", "后台常驻「运行中」通知", keepNotifyState, null);

        addSettingsSubPageEntry("提示与通知", "toast设置", "吐司提示开关与样式", "item_toast_hint", "提示");
    }

    if ("性能与调试".equals(level1Title)) {
        addSettingsSubPageEntry("性能与调试", "线程池", "优先级、队列、满载策略", "item_thread_pool", "线程池");
        addSettingsGridCard("p2", "运行状态", "脚本与宿主信息", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act instanceof SettingsActivity) {
                ((SettingsActivity) act).settingsPushContentView("运行状态", build运行状态ContentView(act));
            }
        }});
        addSettingsSubPageEntry("性能与调试", "调试", "预览设置 · 日志清理阈值", "item_debug", "调试");
    }

    if ("输入框提示".equals(level1Title)) {
        addSettingsCategory("开关", "");
        addSettingsItemSwitchWithKey("输入框提示", "输入框", "输入框开关", "聊天输入框显示自定义提示词", getBoolean("输入框", "输入框开关", false), null);
        addSettingsCategory("提示词", "");
        addSettingsInputHintBlock(activity);
    }

    if ("关于".equals(level1Title)) {
        addSettingsGridCard("a1", "检查更新", "手动检测最新版本", new Runnable() { public void run() { manualCheckQFXUpdate(); }});
        addSettingsGridCard("a2", "查看更新日志", "查看历史更新记录", new Runnable() { public void run() {
            try {
                String logContent = 读(pluginPath + "/更新日志.txt");
                Activity act = getSettingsCurrentActivity();
                if (act != null) mkts(act, "更新日志", logContent);
            } catch (Throwable exception) {
                Toast("读取更新日志失败: " + exception.getMessage());
            }
        }});

        addSettingsCategory("其他", "");
        addSettingsItemChoiceWithKey( "更新通道", "update_channel", (("github".equals(getString("settings", "update_channel", "gitee")) ? "GitHub" : "Gitee (默认)")), new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showUpdateChannelChoiceDialog(act);
        }});
        addSettingsItemClickWithKey( "重置所有设置", "恢复默认设置", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showResetAllSettingsConfirmDialog(act);
        }});
    }
}

int[] getSettingsGradientColors(Activity activity) {
    if (activity == null) return null;
    try {
        if (!"gradient".equals(getUiBgType())) return null;
        boolean dark = isThemeDark(activity);
        String graw = getString("settings", dark ? "ui_bg_gradient_dark" : "ui_bg_gradient_light", "");
        if (graw == null || graw.trim().isEmpty()) return null;
        String[] parts = graw.split(",");
        if (parts == null || parts.length < 2) return null;
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String cs = parts[i].trim();
            if (!cs.startsWith("#")) cs = "#" + cs;
            out[i] = pc(cs);
        }
        return out;
    } catch (Throwable e) {
        traceLog("setwindow_log", "[getSettingsGradientColors] 异常: " + e);
        return null;
    }
}

void applySettingsRootBg(Activity activity, View view) {
    if (activity == null || view == null) return;
    int[] colors = getSettingsGradientColors(activity);
    if (colors != null && colors.length >= 2) {
        try {
            GradientDrawable gd = new GradientDrawable();
            gd.setColors(colors);
            gd.setOrientation(GradientDrawable.Orientation.TL_BR);
            view.setBackground(gd);
            return;
        } catch (Throwable e) {
            traceLog("setwindow_log", "[applySettingsRootBg] 渐变绘制失败: " + e);
        }
    }
    view.setBackgroundColor(pc(getSettingsThemeColor(activity, "background")));
}

void refreshSettingsActivityBg() {
    Activity act = getSettingsCurrentActivity();
    if (act instanceof SettingsActivity) {
        try {
            ((SettingsActivity) act).settingsRefreshBackgrounds();
        } catch (Throwable e) {
            traceLog("setwindow_log", "[refreshSettingsActivityBg] 异常: " + e);
        }
    }
}

int getSettingsFooterBg(Activity activity) {
    int[] colors = getSettingsGradientColors(activity);
    if (colors != null && colors.length >= 2) return colors[colors.length - 1];
    return pc(getSettingsThemeColor(activity, "background"));
}

void renderHintPreviewText(Activity activity, TextView preview, String result) {
    String text = "效果预览：" + (result == null ? "" : result);
    try {
        SpannableString sp = new SpannableString(text);
        sp.setSpan(new ForegroundColorSpan(pc(getSettingsThemeColor(activity, "on_surface_variant"))), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new ForegroundColorSpan(pc(getSettingsThemeColor(activity, "primary"))), 5, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        preview.setText(sp);
    } catch (Throwable e) {
        preview.setText(text);
    }
}

void applyHintPreview(final Activity activity, final TextView preview, final String value, final Object scope, final long[] token, final long stamp) {
    new Thread(new Runnable() { public void run() {
        String result = value;
        try { result = 替换变量占位符(value, scope); } catch (Throwable e) { result = value; }
        final String shown = (result == null) ? "" : result;
        activity.runOnUiThread(new Runnable() { public void run() {
            if (stamp != token[0]) return;
            if (preview.getParent() == null) return;
            renderHintPreviewText(activity, preview, shown);
        }});
    }}).start();
}

void addSettingsInputHintBlock(final Activity activity) {
    final String itemKey = settingsAutoItemKey("设置输入框提示词");
    if (SettingsState.settingsIndexMode) {
        registerSettingsIndexEntry(itemKey, "设置输入框提示词", "支持 #变量# 和 ##链接##", "input");
        return;
    }
    LinearLayout container = settingsCurrentCategoryContainer();
    if (container == null) return;
    LinearLayout block = new LinearLayout(activity);
    block.setOrientation(LinearLayout.VERTICAL);
    block.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
    block.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
    TextView label = new TextView(activity);
    label.setText("设置输入框提示词");
    label.setTextSize(16);
    label.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    block.addView(label);
    final EditText input = makeInput(activity, "输入内容，支持 #变量# 或 ##链接##", null);
    input.setSingleLine(false);
    input.setMinLines(2);
    input.setMaxLines(4);
    input.setText(getString("输入框", "提示词", ""));
    block.addView(input);
    final TextView preview = new TextView(activity);
    preview.setTextSize(13);
    preview.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    preview.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
    block.addView(preview);
    final Object scriptScope = this;
    final long[] token = new long[1];
    final String initialValue = input.getText().toString();
    renderHintPreviewText(activity, preview, initialValue);
    applyHintPreview(activity, preview, initialValue, scriptScope, token, 0);
    input.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            final String value = editable.toString();
            final long now = ++token[0];
            renderHintPreviewText(activity, preview, value);
            uiHandler.postDelayed(new Runnable() { public void run() {
                if (now != token[0]) return;
                putString("输入框", "提示词", value);
                applyHintPreview(activity, preview, value, scriptScope, token, now);
            }}, 500);
        }
    });
    TextView tips = new TextView(activity);
    tips.setText("#变量# 引用变量，##链接## 引用纯文本链接；留空使用默认提示。修改后自动保存。");
    tips.setTextSize(12);
    tips.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    block.addView(tips);
    TextView variables = new TextView(activity);
    variables.setText("查看可用变量  ›");
    variables.setTextSize(14);
    variables.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    variables.setPadding(0, dp(activity, 14), 0, dp(activity, 4));
    variables.setOnClickListener(new View.OnClickListener() { public void onClick(View view) {
        Activity current = getSettingsCurrentActivity();
        if (current instanceof SettingsActivity) ((SettingsActivity) current).settingsPushContentView("可用变量", buildSettingsVariablesView(current));
    }});
    block.addView(variables);
    container.addView(block);
    if (SettingsState.settingsItemViews != null) SettingsState.settingsItemViews.put(itemKey, block);
    registerSettingsIndexEntry(itemKey, "设置输入框提示词", "支持 #变量# 和 ##链接##", "input");
}

View buildSettingsVariablesView(final Activity activity) {
    LinearLayout list = new LinearLayout(activity);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(dp(activity, 20), dp(activity, 12), dp(activity, 20), dp(activity, 20));
    List entries = getSortedVariableList(this);
    for (int i = 0; i < entries.size(); i++) {
        java.util.Map.Entry entry = (java.util.Map.Entry) entries.get(i);
        String key = (String) entry.getKey();
        if (key == null || (key.startsWith("date_") && key.length() > 20)) continue;
        final String text = "#" + key + "#";
        TextView row = new TextView(activity);
        row.setText(text + "\n" + getVarDescription(key, (String) entry.getValue()));
        row.setTextSize(14);
        row.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
        row.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        row.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), dp(activity, 12)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(activity, 8);
        list.addView(row, lp);
        row.setOnClickListener(new View.OnClickListener() { public void onClick(View view) {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("变量", text));
                Toast("已复制: " + text);
            }
        }});
    }
    return list;
}

void buildLevel3MenuContent(Activity activity, String level1Title, String level2Title, String level3Title) {
    if (activity == null || level1Title == null || level2Title == null) return;
    if (level1Title != null) {
        if ("基础模式".equals(level2Title)) {
            addSettingsCategory("基础模式", null);
            addSettingsItemChoiceWithKey( "主题模式", "ui_theme_mode", (("system".equals(getString("settings", "ui_theme_mode", "default")) ? "跟随系统" : "light".equals(getString("settings", "ui_theme_mode", "default")) ? "强制浅色" : "dark".equals(getString("settings", "ui_theme_mode", "default")) ? "强制深色" : "默认（推荐）")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showThemeModeChoiceDialog(act);
                }
            }});
            addSettingsItemChoiceWithKey( "弹窗大小(比例)", "ui_dialog_scale", ((getString("settings", "ui_dialog_scale", "").isEmpty() ? "1.0x (默认)" : getString("settings", "ui_dialog_scale", "") + "x")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showScaleSliderDialog(act);
                }
            }});
            addSettingsInputItem( "弹窗宽度", "默认最大260dp", "ui_dialog_width", "如: 280", "", null);
            addSettingsInputItem( "弹窗高度", "自适应内容", "ui_dialog_height", "如: 400", "", null);
            boolean vibrationFeedbackState = getBoolean("settings", "振动反馈", false);
            addSettingsSwitchItem( "振动反馈", null, "振动反馈", vibrationFeedbackState, null);
            boolean bgBlurState = getBoolean("settings", "背景模糊", false);
            addSettingsSwitchItem( "背景模糊", "系统模糊窗体后方内容(Android 12+)", "背景模糊", bgBlurState, null);
            addSettingsItemClickWithKey( "弹窗圆角", ("当前 " + getUiCornerDp() + "dp"), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) showUiCornerPicker(act);
            }});
        }

        if ("背景样式".equals(level2Title)) {
            addSettingsCategory("背景样式", null);
            addSettingsItemChoiceWithKey( "背景类型", "ui_bg_type", (("image".equals(getString("settings", "ui_bg_type", "color")) ? "图片背景" : "gradient".equals(getString("settings", "ui_bg_type", "color")) ? "三色渐变" : "纯色背景")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showBgTypeChoiceDialog(act);
                }
            }});

            String bgType = getString("settings", "ui_bg_type", "color");
            boolean isDark = isThemeDark(activity);
            String suffix = isDark ? " (深色模式)" : " (浅色模式)";

            if ("color".equals(bgType)) {
                addSettingsItemClickWithKey( "预设颜色" + suffix, "点击选择内置配色", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) {
                        showPresetColorDialog(act);
                    }
                }});
                final String solidListKey = isDark ? "ui_bg_solid_list_dark" : "ui_bg_solid_list_light";
                final String singleColorKey = isDark ? "ui_bg_color_dark" : "ui_bg_color_light";
                addSettingsItemClickWithKey( "背景颜色列表" + suffix, "多色轮转，1色常驻", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    String seed = getString("settings", solidListKey, "");
                    if (seed == null || seed.trim().isEmpty()) {
                        String single = getString("settings", singleColorKey, "");
                        if (single != null && single.trim().length() > 0) seed = single.trim();
                        else seed = isDark ? "#FF1E1E1E" : "#FFFFFFFF";
                    }
                    showColorListEditor(act, "背景颜色列表", 0, seed, new ColorListCallback() {
                        public void onColorListSaved(String csv) {
                            putString("settings", solidListKey, csv);
                            if (csv != null && csv.indexOf(",") < 0 && csv.trim().length() > 0) {
                                putString("settings", singleColorKey, csv.trim());
                            }
                            Toast("背景颜色列表已保存");
                        }
                    });
                }});
            } else if ("gradient".equals(bgType)) {
                addSettingsItemClickWithKey( "预设渐变" + suffix, "点击选择内置渐变", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) {
                        showPresetGradientDialog(act);
                    }
                }});
                final String gradKey = isDark ? "ui_bg_gradient_dark" : "ui_bg_gradient_light";
                addSettingsItemClickWithKey( "渐变颜色列表" + suffix, "2~3色，复用调色盘列表", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    String seed = getString("settings", gradKey, "");
                    if (seed == null || seed.trim().isEmpty()) {
                        seed = isDark ? "#FF2C2C2C,#FF121212,#FF2C2C2C" : "#FFFFFFFF,#FFF5F5F5,#FFFFFFFF";
                    }
                    showColorListEditor(act, "渐变颜色列表", 3, seed, new ColorListCallback() {
                        public void onColorListSaved(String csv) {
                            putString("settings", gradKey, csv);
                            Toast("渐变颜色列表已保存");
                        }
                    });
                }});
            } else if ("image".equals(bgType)) {
                addSettingsItemClickWithKey( "选择背景图片", "点击选择本地图片", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    openFilePicker(act, 1007, "image/*", null, null, new FilePickerCallback() {
                        public void onFilePicked(Activity a, Uri uri, String fileName, String filePath) {
                            traceLog("setwindow_log", "[1007] name=" + fileName + " path=" + filePath + " uri=" + uri);
                            editAndSaveImage(a, uri, pluginPath + "/API/background.png", 1007);
                        }
                    });
                    Toast("选择后自动居中裁剪应用");
                }});
                addSettingsInputItem( "图片模糊 (0-25)", "0为不模糊", "ui_img_blur", "0-25", "0", null);
                addSettingsInputItem( "遮罩浓度 (0-255)", "越大越暗", "ui_img_alpha", "0-255", isDark ? "180" : "100", null);
            }
        }

        if ("字体样式".equals(level2Title)) {
            addSettingsCategory("字体样式", null);
            addSettingsItemChoiceWithKey( "字体风格", "ui_font_type", (("serif".equals(getString("settings", "ui_font_type", "default")) ? "衬线体" : "sans".equals(getString("settings", "ui_font_type", "default")) ? "无衬线" : "monospace".equals(getString("settings", "ui_font_type", "default")) ? "等宽" : "bold".equals(getString("settings", "ui_font_type", "default")) ? "粗体" : "默认字体")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showFontTypeChoiceDialog(act);
                }
            }});
            addSettingsItemChoiceWithKey( "字体大小", "ui_font_size", (("0.85".equals(getString("settings", "ui_font_size", "1.0")) ? "小 (0.85x)" : "1.15".equals(getString("settings", "ui_font_size", "1.0")) ? "中 (1.15x)" : "1.3".equals(getString("settings", "ui_font_size", "1.0")) ? "大 (1.3x)" : "默认 (1.0x)")), new Runnable() { public void run() {
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
            addSettingsCategory("Toast样式", null);
            addSettingsItemChoiceWithKey( "样式", "toast_style", (("theme".equals(getString("settings", "toast_style", "default")) ? "跟随主题色" : "blur".equals(getString("settings", "toast_style", "default")) ? "实时模糊(仅Toast区)" : "gradient".equals(getString("settings", "toast_style", "default")) ? "渐变背景" : "默认")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) showToastStyleChoiceDialog(act);
            }});
            String toastStyleNow = getString("settings", "toast_style", "default");
            if (toastStyleNow == null || toastStyleNow.isEmpty()) toastStyleNow = "default";
            if (!"theme".equals(toastStyleNow) && !"blur".equals(toastStyleNow)) {
                addSettingsItemClickWithKey( "文字轮换颜色", "1色常驻，多色轮换", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    showColorListEditor(act, "文字轮换颜色", 0, loadRotColorList("toast_color_list"), new ColorListCallback() {
                        public void onColorListSaved(String csv) {
                            putString("settings", "toast_color_list", csv);
                            Toast("文字颜色已保存");
                        }
                    });
                }});
            }
            if ("default".equals(toastStyleNow)) {
                addSettingsItemClickWithKey( "背景轮换颜色", "纯色背景，1色常驻", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    showColorListEditor(act, "背景轮换颜色", 0, loadRotColorList("toast_bg_solid_list"), new ColorListCallback() {
                        public void onColorListSaved(String csv) {
                            putString("settings", "toast_bg_solid_list", csv);
                            Toast("背景颜色已保存");
                        }
                    });
                }});
            }
            if ("gradient".equals(toastStyleNow)) {
                addSettingsItemClickWithKey( "渐变背景颜色", "2色起，最多3色", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    showColorListEditor(act, "渐变背景颜色", 3, loadRotColorList("toast_bg_color_list"), new ColorListCallback() {
                        public void onColorListSaved(String csv) {
                            putString("settings", "toast_bg_color_list", csv);
                            Toast("渐变颜色已保存");
                        }
                    });
                }});
            }
            addSettingsInputItem( "弹出时长(ms)", "500-10000", "toast_duration", "2000", "2000", null);

            addSettingsCategory("Toast位置", null);
            addSettingsItemChoiceWithKey( "弹出位置", "toast_pos", (("top".equals(getString("settings", "toast_pos", "bottom")) ? "顶部" : "center".equals(getString("settings", "toast_pos", "bottom")) ? "居中" : "custom".equals(getString("settings", "toast_pos", "bottom")) ? "自定义" : "底部")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) showToastPosChoiceDialog(act);
            }});
            String toastPosNow = getString("settings", "toast_pos", "bottom");
            if (toastPosNow == null || toastPosNow.isEmpty()) toastPosNow = "bottom";
            if ("custom".equals(toastPosNow)) {
                addSettingsItemClickWithKey( "自定义坐标/宽高", "拖主体移动，拖边角改大小", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act == null) return;
                    int x = 0, y = 0, w = 120, h = 48;
                    try { x = Integer.parseInt(getString("settings", "toast_custom_x", "0")); } catch (Throwable e) {}
                    try { y = Integer.parseInt(getString("settings", "toast_custom_y", "0")); } catch (Throwable e) {}
                    try { w = Integer.parseInt(getString("settings", "toast_custom_w", "120")); } catch (Throwable e) {}
                    try { h = Integer.parseInt(getString("settings", "toast_custom_h", "48")); } catch (Throwable e) {}
                    showScreenPointPicker(act, x, y, w, h, new ScreenPointCallback() {
                        public void onPointPicked(int px, int py, int pw, int ph) {
                            putString("settings", "toast_custom_x", String.valueOf(px));
                            putString("settings", "toast_custom_y", String.valueOf(py));
                            putString("settings", "toast_custom_w", String.valueOf(pw));
                            putString("settings", "toast_custom_h", String.valueOf(ph));
                            Toast("已保存坐标 x=" + px + " y=" + py + " " + pw + "x" + ph);
                        }
                    });
                }});
                boolean adaptiveOn = getBoolean("settings", "toast_adaptive", false);
                addSettingsSwitchItem( "自适应", "开：气泡贴合内容+框内位置；关：气泡撑满框+文字填充", "toast_adaptive", adaptiveOn, new Runnable() { public void run() {
                    try {
                        Activity act2 = getSettingsCurrentActivity();
                        if (act2 != null) showSettingsMenu(act2, "提示与通知", "提示", null);
                    } catch (Throwable ignore) {}
                }});
                addSettingsItemChoiceWithKey( "气泡在框内位置", "toast_box_gravity", (("left".equals(getString("settings", "toast_box_gravity", "center")) ? "靠左" : "right".equals(getString("settings", "toast_box_gravity", "center")) ? "靠右" : "top".equals(getString("settings", "toast_box_gravity", "center")) ? "靠上" : "bottom".equals(getString("settings", "toast_box_gravity", "center")) ? "靠下" : "top_left".equals(getString("settings", "toast_box_gravity", "center")) ? "左上" : "top_right".equals(getString("settings", "toast_box_gravity", "center")) ? "右上" : "bottom_left".equals(getString("settings", "toast_box_gravity", "center")) ? "左下" : "bottom_right".equals(getString("settings", "toast_box_gravity", "center")) ? "右下" : "居中")), new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) showToastBoxGravityChoiceDialog(act);
                }});
                addSettingsItemChoiceWithKey( "文字填充", "toast_text_gravity", (("left".equals(getString("settings", "toast_text_gravity", "center")) ? "靠左" : "right".equals(getString("settings", "toast_text_gravity", "center")) ? "靠右" : "top".equals(getString("settings", "toast_text_gravity", "center")) ? "靠上" : "bottom".equals(getString("settings", "toast_text_gravity", "center")) ? "靠下" : "justify".equals(getString("settings", "toast_text_gravity", "center")) ? "两端对齐" : "居中")), new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) showToastGravityChoiceDialog(act);
                }});
            }

            addSettingsCategory("Toast文字", null);
            addSettingsInputItem( "前缀", "显示在文字前", "toast_prefix", "可留空", "", null);
            addSettingsInputItem( "后缀", "显示在文字后", "toast_suffix", "可留空", "", null);
            addSettingsItemClickWithKey( "测试Toast", "预览当前配置", new Runnable() { public void run() {
                Toast("这是一条测试 Toast");
            }});
        }

        if ("线程池".equals(level2Title)) {
            addSettingsCategory("线程池", null);
            addSettingsItemChoiceWithKey( "线程优先级", "thread_pool_priority", ((getString("settings", "thread_pool_priority", "").isEmpty() ? "5 (默认)" : getString("settings", "thread_pool_priority", "") + "")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showThreadPriorityChoiceDialog(act);
                }
            }});
            addSettingsInputItem( "任务队列容量", "默认50", "thread_pool_queue_capacity", "数字", "50", null);
            addSettingsInputItem( "核心线程存活(秒)", "默认30", "thread_pool_keep_alive", "秒数", "30", null);
            addSettingsItemChoiceWithKey( "任务满载策略", "thread_pool_reject_policy", (("1".equals(getString("settings", "thread_pool_reject_policy", "0")) ? "丢弃最新任务" : "2".equals(getString("settings", "thread_pool_reject_policy", "0")) ? "抛出异常" : "3".equals(getString("settings", "thread_pool_reject_policy", "0")) ? "调用者执行" : "丢弃最旧任务 (默认)")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showRejectPolicyChoiceDialog(act);
                }
            }});
        }

        if ("悬浮窗设置".equals(level2Title)) {
            addSettingsCategory("悬浮窗设置", null);
            addSettingsItemClickWithKey( "更换图标", (("".equals(getString("settings", "iconPath", "")) || getString("settings", "iconPath", "") == null ? "未设置" : getString("settings", "iconPath", "").toLowerCase().endsWith(".gif") ? "动态图标 (GIF)" : "静态图标")), new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act == null) return;
                openFilePicker(act, 1005, "image/*", null, pluginPath + "/API/icon{ext}", new FilePickerCallback() {
                    public void onFilePicked(Activity a, Uri uri, String fileName, String filePath) {
                        traceLog("setwindow_log", "[1005] name=" + fileName + " path=" + filePath + " uri=" + uri);
                        putString("settings", "iconPath", filePath);
                        editAndSaveImage(a, uri, filePath, 1005);
                    }
                });
                Toast("选择后请重新打开设置刷新");
            }});
            addSettingsInputItem( "悬浮窗大小", "默认48", "悬浮窗大小", "dp", "48", null);
            addSettingsInputItem( "关闭图标大小", "默认24", "关闭区域图标大小", "dp", "24", null);
            addSettingsInputItem( "拖拽灵敏度", "数值越小越灵敏", "拖拽灵敏度", "数字", "12", null);
            addSettingsInputItem( "长按关闭阈值", "默认650ms", "长按关闭阈值", "毫秒", "650", null);
            addSettingsInputItem( "图标透明度", "0-255", "iconAlpha", "0-255", "255", null);

            String iconPath = getString("settings", "iconPath", "");
            boolean isAnimatedIcon = iconPath != null && iconPath.toLowerCase().endsWith(".gif");
            if (isAnimatedIcon) {
                addSettingsItemClickWithKey( "GIF播放速度", "带实时预览，越小越快", new Runnable() { public void run() {
                    Activity act = getSettingsCurrentActivity();
                    if (act != null) showGifSpeedPicker(act);
                }});
            }
        }

        if ("调试".equals(level2Title)) {
            addSettingsCategory("调试", null);
            addSettingsItemClickWithKey( "预览设置", "预览当前设置效果", new Runnable() { public void run() {
                Activity act = getSettingsCurrentActivity();
                if (act != null) {
                    showSettingsPreviewPopup(act);
                }
            }});
            String logThreshold = getString("settings", "log_delete_threshold", "1");
            addSettingsInputItem( "日志大小阈值(MB)", "日志文件夹超过此大小自动清理", "log_delete_threshold", "如: 1", logThreshold, null);
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
    title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    title.setGravity(Gravity.CENTER);
    content.addView(title);

    TextView msg = new TextView(activity);
    msg.setText("确定要重置所有设置为默认值吗？\n此操作不可撤销");
    msg.setTextSize(14);
    msg.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    msg.setGravity(Gravity.CENTER);
    msg.setPadding(0, dp(activity, 12), 0, dp(activity, 16));
    content.addView(msg);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(content);

    builder.setNegativeButton("取消", null);

    builder.setPositiveButton("确认重置", new android.content.DialogInterface.OnClickListener() {
        public void onClick(android.content.DialogInterface dialog, int which) {
            int count = resetAllSettingsToDefault();
            qqToast(0, "已重置 " + count + " 项设置");
            Activity resetAct = getSettingsCurrentActivity();
            if (resetAct != null) {
                showSettingsMenu(resetAct, null, null, null);
            }
        }
    });

    AlertDialog dialog = builder.show();
    applyUiTheme(activity, dialog, 0);
}

int resetAllSettingsToDefault() {
    String[] settingKeys = {
            "ui_theme_mode", "ui_dialog_scale", "ui_dialog_width", "ui_dialog_height",
            "振动反馈", "ui_bg_type", "ui_bg_color_dark", "ui_bg_color_light",
            "ui_bg_gradient_dark", "ui_bg_gradient_light", "ui_img_blur", "ui_img_alpha",
            "ui_bg_solid_list_dark", "ui_bg_solid_list_light",
            "ui_font_type", "ui_font_size", "ui_text_color_dark", "ui_text_color_light",
            "thread_pool_priority", "thread_pool_queue_capacity", "thread_pool_keep_alive",
            "thread_pool_name_prefix", "thread_pool_reject_policy", "悬浮窗大小",
            "关闭区域图标大小", "关闭区域大小", "拖拽灵敏度", "长按关闭阈值", "移动阈值",
            "背景模糊", "iconAlpha", "ui_corner_dp", "log_delete_threshold", "gifDelay",
            "加载提示", "加载通知", "常驻通知", "补一次执行", "toast_style", "toast_pos", "toast_box_gravity",
            "toast_text_gravity", "toast_duration", "toast_adaptive", "toast_prefix",
            "toast_suffix", "toast_color_list", "toast_bg_solid_list", "toast_bg_color_list",
            "黑白"
    };

    int count = 0;
    for (String key : settingKeys) {
        putString("settings", key, "");
        count++;
    }

    return count;
}


String settingsAutoItemKey(String itemName) {
    String l1 = SettingsState.settingsCurrentLevel1;
    String l2 = SettingsState.settingsCurrentLevel2;
    String cat = SettingsState.settingsCurrentCategory;
    return (l1 == null ? "" : l1 + "_") + (l2 == null ? "" : l2 + "_") + (cat == null ? "" : cat + "_") + (itemName == null ? "" : itemName);
}

LinearLayout settingsCurrentCategoryContainer() {
    String cat = SettingsState.settingsCurrentCategory;
    if (cat == null || SettingsState.settingsCategoryContainers == null) return null;
    return (LinearLayout) SettingsState.settingsCategoryContainers.get(cat);
}

void addSettingsCategory(String titleText, String descriptionText) {
    SettingsState.settingsCurrentCategory = titleText;
    if (SettingsState.settingsIndexMode) return;
    if (SettingsState.settingsListContainer == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    boolean isDark = isThemeDark(activity);

    TextView categoryTitle = new TextView(activity);
    categoryTitle.setText(titleText);
    categoryTitle.setTextSize(14);
    categoryTitle.setTypeface(null, Typeface.BOLD);
    categoryTitle.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    categoryTitle.setPadding(dp(activity, 28), dp(activity, 20), dp(activity, 16), dp(activity, 8));

    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
    SettingsState.settingsListContainer.addView(categoryTitle, titleParams);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView categoryDesc = new TextView(activity);
        categoryDesc.setText(descriptionText);
        categoryDesc.setTextSize(12);
        categoryDesc.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        categoryDesc.setPadding(dp(activity, 28), 0, dp(activity, 16), dp(activity, 8));

        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        SettingsState.settingsListContainer.addView(categoryDesc, descParams);
    }

    LinearLayout categoryContainer = new LinearLayout(activity);
    categoryContainer.setOrientation(LinearLayout.VERTICAL);
    int cardBgColor = getAdaptiveCardBg(activity);
    categoryContainer.setBackground(roundRect(cardBgColor, dp(activity, 16)));
    categoryContainer.setClipToOutline(true);

    LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(-1, -2);
    containerParams.setMargins(dp(activity, 12), 0, dp(activity, 12), dp(activity, 8));
    SettingsState.settingsListContainer.addView(categoryContainer, containerParams);

    if (SettingsState.settingsCategoryContainers == null) SettingsState.settingsCategoryContainers = new HashMap();
    SettingsState.settingsCategoryContainers.put(titleText, categoryContainer);
    SettingsState.settingsCurrentCategory = titleText;
}


void addSettingsItemClickWithKey(String itemName, String descriptionText, final Runnable clickCallback) {
    String itemKey = settingsAutoItemKey(itemName);
    if (SettingsState.settingsIndexMode) { registerSettingsIndexEntry(itemKey, itemName, descriptionText, "click"); return; }
    if (SettingsState.settingsCategoryContainers == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout container = settingsCurrentCategoryContainer();
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
    nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    textArea.addView(nameView);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView descView = new TextView(activity);
        descView.setText(descriptionText);
        descView.setTextSize(12);
        descView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        descView.setPadding(0, dp(activity, 2), 0, 0);
        textArea.addView(descView);
    }

    itemLayout.addView(textArea);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(20);
    arrowView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    itemLayout.addView(arrowView);

    itemWrapper.addView(itemLayout);

    itemWrapper.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.containsKey(itemKey)) {
        SettingsState.settingsItemMeta.put(itemKey, new SettingsItemMeta(
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


void addSettingsItemChoiceWithKey(String itemName, String configKey, String valueText, final Runnable clickCallback) {
    String itemKey = settingsAutoItemKey(itemName);
    if (SettingsState.settingsIndexMode) { registerSettingsIndexEntry(itemKey, itemName, null, "choice"); return; }
    if (SettingsState.settingsCategoryContainers == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    LinearLayout container = settingsCurrentCategoryContainer();
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
    nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    titleRow.addView(nameView);

    TextView arrowView = new TextView(activity);
    arrowView.setText("›");
    arrowView.setTextSize(20);
    arrowView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    titleRow.addView(arrowView);

    itemLayout.addView(titleRow);

    final TextView valueView = new TextView(activity);
    valueView.setText(valueText);
    valueView.setTextSize(12);
    valueView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    valueView.setPadding(0, dp(activity, 2), 0, 0);
    itemLayout.addView(valueView);

    if (SettingsState.settingsItemTextViews == null) SettingsState.settingsItemTextViews = new HashMap();
    SettingsState.settingsItemTextViews.put(configKey, valueView);

    itemWrapper.addView(itemLayout);

    itemWrapper.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.containsKey(itemKey)) {
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

void addSettingsItemSwitchWithKey(String itemName, String configName, String keyName, String descriptionText, boolean currentValue, final Runnable switchCallback) {
	String itemKey = settingsAutoItemKey(itemName);
	if (SettingsState.settingsIndexMode) { registerSettingsIndexEntry(itemKey, itemName, descriptionText, "switch"); return; }
	if (SettingsState.settingsCategoryContainers == null) return;
	Activity activity = getSettingsCurrentActivity();
	if (activity == null) return;

	LinearLayout container = settingsCurrentCategoryContainer();
	if (container == null) return;

	FrameLayout itemWrapper = new FrameLayout(activity);

	LinearLayout itemLayout = new LinearLayout(activity);
	itemLayout.setOrientation(LinearLayout.HORIZONTAL);
	itemLayout.setGravity(Gravity.CENTER_VERTICAL);
	itemLayout.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));

	if (descriptionText != null && !descriptionText.isEmpty()) {
		LinearLayout textArea = new LinearLayout(activity);
		textArea.setOrientation(LinearLayout.VERTICAL);
		textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

		TextView nameView = new TextView(activity);
		nameView.setText(itemName);
		nameView.setTextSize(16);
		nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
		textArea.addView(nameView);

		TextView descView = new TextView(activity);
		descView.setText(descriptionText);
		descView.setTextSize(12);
		descView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
		descView.setPadding(0, dp(activity, 2), 0, 0);
		textArea.addView(descView);

		itemLayout.addView(textArea);
	} else {
		TextView nameView = new TextView(activity);
		nameView.setText(itemName);
		nameView.setTextSize(16);
		nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
		nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
		itemLayout.addView(nameView);
	}

	final View switchView = createSettingsSwitchView(activity, currentValue, configName, keyName, itemName, switchCallback);
	if (switchView != null) {
		itemLayout.addView(switchView);
	}

	itemWrapper.addView(itemLayout);

	itemWrapper.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
	if (descriptionText != null && !descriptionText.isEmpty()) {
		itemWrapper.setClickable(true);
		itemWrapper.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				if (switchView != null) {
					switchView.performClick();
				}
			}
		});
	} else {
		itemWrapper.setClickable(false);
	}

	if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemViews != null) {
		SettingsState.settingsItemViews.put(itemKey, itemWrapper);
	}

	if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.containsKey(itemKey)) {
		SettingsState.settingsItemMeta.put(itemKey, new SettingsItemMeta(
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

void addSettingsSwitchItem(String itemName, String descriptionText, String keyName, boolean currentValue, final Runnable onChangeCallback) {
	addSettingsItemSwitchWithKey(itemName, "settings", keyName, descriptionText, currentValue, onChangeCallback);
}

void addSettingsInputItem(String itemName, String descriptionText, String keyName, String hintText, String defaultValue, final Runnable onValueChanged) {
    String itemKey = settingsAutoItemKey(itemName);
    if (SettingsState.settingsIndexMode) { registerSettingsIndexEntry(itemKey, itemName, descriptionText, "input"); return; }
    if (SettingsState.settingsCategoryContainers == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    String currentValue = getString("settings", keyName, defaultValue);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));

    TextView nameView = new TextView(activity);
    nameView.setText(itemName);
    nameView.setTextSize(16);
    nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    itemLayout.addView(nameView);

    if (descriptionText != null && !descriptionText.isEmpty()) {
        TextView descView = new TextView(activity);
        descView.setText(descriptionText);
        descView.setTextSize(12);
        descView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        descView.setPadding(0, dp(activity, 2), 0, dp(activity, 8));
        itemLayout.addView(descView);
    }

    final EditText inputEdit = makeInput(activity, hintText != null ? hintText : "", null);
    inputEdit.setText(currentValue);
    inputEdit.setTextSize(14);
    inputEdit.setSingleLine(true);
    itemLayout.addView(inputEdit);

    final String finalKeyName = keyName;
    final long[] inputToken = new long[1];
    inputEdit.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            final String value = editable.toString().trim();
            inputToken[0] = System.currentTimeMillis();
            final long token = inputToken[0];
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (token != inputToken[0]) return;
                    putString("settings", finalKeyName, value);
                    if (onValueChanged != null) {
                        onValueChanged.run();
                    }
                }
            }, 300);
        }
    });

    itemLayout.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
    itemLayout.setClickable(true);

    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemViews != null) {
        SettingsState.settingsItemViews.put(itemKey, itemLayout);
    }
    if (itemKey != null && !itemKey.isEmpty() && SettingsState.settingsItemMeta != null && !SettingsState.settingsItemMeta.containsKey(itemKey)) {
        SettingsState.settingsItemMeta.put(itemKey, new SettingsItemMeta(
            itemName, descriptionText,
            SettingsState.settingsCurrentLevel1,
            SettingsState.settingsCurrentLevel2,
            SettingsState.settingsCurrentLevel3,
            SettingsState.settingsCurrentCategory,
            "input"
        ));
    }

    LinearLayout container = settingsCurrentCategoryContainer();
    if (container != null) {
        container.addView(itemLayout);
    }
}

void showChoiceDialogEx(Activity activity, String title, String[] names, String[] values, String configKey, String defaultVal, int checkedInit, int toastMode, String toastMsg, Runnable onDone) {
	if (activity == null || activity.isFinishing()) return;
	String cur = getString("settings", configKey, defaultVal);
	int checked = checkedInit;
	for (int i = 0; i < names.length; i++) if (values[i].equals(cur)) { checked = i; break; }
	AlertDialog.Builder b = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
	b.setTitle(title);
	b.setSingleChoiceItems(names, checked, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			putString("settings", configKey, values[which]);
			updateSettingsItemText(configKey, names[which]);
			dialog.dismiss();
			if (toastMode == 1) Toast(toastMsg + names[which]);
			else if (toastMode == 2) qqToast(2, toastMsg);
			else if (toastMode == 3) qqToast(2, toastMsg + names[which]);
			if (onDone != null) onDone.run();
		}
	});
	b.setNegativeButton("取消", null);
	AlertDialog dlg = b.show();
	applyUiTheme(activity, dlg, 0);
}

void showToastBoxGravityChoiceDialog(final Activity activity) {
    final String[] names = {"居中", "靠左", "靠右", "靠上", "靠下", "左上", "右上", "左下", "右下"};
    final String[] values = {"center", "left", "right", "top", "bottom", "top_left", "top_right", "bottom_left", "bottom_right"};
    showChoiceDialogEx(activity, "气泡在框内位置", names, values, "toast_box_gravity", "center", 0, 1, "气泡位置: ", null);
}

void showToastGravityChoiceDialog(final Activity activity) {
    final String[] names = {"居中", "靠左", "靠右", "靠上", "靠下", "两端对齐"};
    final String[] values = {"center", "left", "right", "top", "bottom", "justify"};
    showChoiceDialogEx(activity, "文字填充", names, values, "toast_text_gravity", "center", 0, 1, "填充: ", null);
}

void showToastStyleChoiceDialog(final Activity activity) {
    final String[] names = {"默认", "跟随主题色", "实时模糊(仅Toast区,可能耗性能)", "渐变背景"};
    final String[] values = {"default", "theme", "blur", "gradient"};
    showChoiceDialogEx(activity, "Toast样式", names, values, "toast_style", "default", 0, 1, "Toast样式: ", new Runnable() { public void run() {
        try { Activity act2 = getSettingsCurrentActivity(); if (act2 != null) showSettingsMenu(act2, "提示与通知", "提示", null); } catch (Throwable ignore) {}
    } });
}

void showUiCornerPicker(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    final int[] radius = new int[]{getUiCornerDp()};
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                Dialog d = new Dialog(activity);
                d.requestWindowFeature(1);
                try {
                    Window w = d.getWindow();
                    if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                } catch (Throwable ignore) {}

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 12));
                root.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

                TextView title = new TextView(activity);
                title.setText("弹窗圆角");
                title.setTextSize(17);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
                root.addView(title);

                TextView valueTv = new TextView(activity);
                valueTv.setText(radius[0] + " dp");
                valueTv.setTextSize(24);
                valueTv.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
                valueTv.setGravity(Gravity.CENTER);
                valueTv.setPadding(0, dp(activity, 12), 0, dp(activity, 8));
                root.addView(valueTv);

                FrameLayout previewWrap = new FrameLayout(activity);
                previewWrap.setPadding(0, dp(activity, 12), 0, dp(activity, 8));
                previewWrap.setBackgroundColor(isThemeDark(activity) ? pc("#FF000000") : pc("#FFDDDDDD"));
                final View preview = new View(activity);
                final Runnable updatePreview = new Runnable() {
                    public void run() {
                        GradientDrawable g = new GradientDrawable();
                        g.setColor(pc(getSettingsThemeColor(activity, "primary_container")));
                        float cr = dp(activity, radius[0] * 3);
                        g.setCornerRadii(new float[]{cr, cr, 0f, 0f, 0f, 0f, 0f, 0f});
                        preview.setBackground(g);
                    }
                };
                updatePreview.run();
                FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(dp(activity, 120), dp(activity, 120));
                plp.gravity = Gravity.TOP | Gravity.START;
                plp.leftMargin = dp(activity, 16);
                previewWrap.addView(preview, plp);
                root.addView(previewWrap);

                FrameLayout seekWrap = new FrameLayout(activity);
                seekWrap.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), 0);
                SeekBar seek = new SeekBar(activity);
                seek.setMax(32);
                seek.setProgress(radius[0]);
                applyUiSeekBar(seek, activity, tc(activity, "primary"));
                seekWrap.addView(seek, new FrameLayout.LayoutParams(-1, -2));
                final TextView bubble = new TextView(activity);
                bubble.setText(String.valueOf(radius[0]));
                bubble.setTextSize(12);
                bubble.setTextColor(Color.WHITE);
                bubble.setGravity(Gravity.CENTER);
                bubble.setPadding(dp(activity, 8), dp(activity, 2), dp(activity, 8), dp(activity, 2));
                GradientDrawable bb = new GradientDrawable();
                bb.setColor(tc(activity, "primary"));
                bb.setCornerRadius(dp(activity, 6));
                bubble.setBackground(bb);
                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-2, -2);
                blp.gravity = Gravity.TOP;
                seekWrap.addView(bubble, blp);
                View pointer = new View(activity);
                GradientDrawable ptrBg = new GradientDrawable();
                ptrBg.setColor(tc(activity, "primary"));
                ptrBg.setCornerRadius(dp(activity, 2));
                pointer.setBackground(ptrBg);
                pointer.setRotation(45f);
                FrameLayout.LayoutParams ptrLp = new FrameLayout.LayoutParams(dp(activity, 10), dp(activity, 10));
                ptrLp.gravity = Gravity.TOP;
                seekWrap.addView(pointer, ptrLp);
                seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                        radius[0] = progress;
                        valueTv.setText(progress + " dp");
                        bubble.setText(String.valueOf(progress));
                        root.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, progress)));
                        int max = Math.max(1, sb.getMax());
                        int trackW = sb.getWidth() - sb.getPaddingLeft() - sb.getPaddingRight();
                        if (trackW <= 0) trackW = sb.getWidth();
                        int x = sb.getPaddingLeft() + (int)((float)progress / max * trackW) - bubble.getWidth() / 2;
                        if (x < 0) x = 0;
                        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) bubble.getLayoutParams();
                        p.leftMargin = x;
                        bubble.setLayoutParams(p);
                        FrameLayout.LayoutParams pp = (FrameLayout.LayoutParams) pointer.getLayoutParams();
                        pp.leftMargin = sb.getPaddingLeft() + (int)((float)progress / max * trackW) - dp(activity, 5);
                        pp.topMargin = bubble.getBottom() > 0 ? bubble.getBottom() - dp(activity, 4) : dp(activity, 28);
                        pointer.setLayoutParams(pp);
                        updatePreview.run();
                    }
                    public void onStartTrackingTouch(SeekBar sb) {}
                    public void onStopTrackingTouch(SeekBar sb) {}
                });
                root.addView(seekWrap);

                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END);
                btnRow.setPadding(0, dp(activity, 16), 0, 0);
                TextView cancel = createButton(activity, "取消", tc(activity, "on_surface_variant"), Color.TRANSPARENT, 14f, 8, 16, 8, false, 0, 0, null);
                TextView ok = createButton(activity, "确定", Color.WHITE, tc(activity, "primary"), 14f, 8, 16, 8, false, 0, 0, null);
                btnRow.addView(cancel);
                btnRow.addView(ok);
                root.addView(btnRow);

                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { try { d.dismiss(); } catch (Throwable ignore) {} }
                });
                ok.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        putString("settings", "ui_corner_dp", String.valueOf(radius[0]));
                        Toast("圆角: " + radius[0] + "dp");
                        try { d.dismiss(); } catch (Throwable ignore) {}
                        try {
                            Activity act2 = getSettingsCurrentActivity();
                            if (act2 != null) showSettingsMenu(act2, "界面与显示", "基础模式", null);
                        } catch (Throwable ignore) {}
                    }
                });

                d.setContentView(root);
                d.show();
                applyUiTheme(activity, d, 1);
            } catch (Throwable e) { traceLog("setwindow_log", "[showUiCornerPicker] 异常: " + e); }
        }
    });
}

void showToastPosChoiceDialog(final Activity activity) {
    final String[] names = {"底部", "居中", "顶部", "自定义"};
    final String[] values = {"bottom", "center", "top", "custom"};
    showChoiceDialogEx(activity, "弹出位置", names, values, "toast_pos", "bottom", 0, 1, "位置: ", new Runnable() { public void run() {
        try { Activity actR = getSettingsCurrentActivity(); if (actR != null) showSettingsMenu(actR, "提示与通知", "提示", null); } catch (Throwable ignore) {}
    } });
}

void showUpdateChannelChoiceDialog(final Activity activity) {
    final String[] names = {"Gitee (默认)", "GitHub"};
    final String[] values = {"gitee", "github"};
    showChoiceDialogEx(activity, "更新通道", names, values, "update_channel", "gitee", 0, 3, "已切换至 ", null);
}

void showThemeModeChoiceDialog(final Activity activity) {
    final String[] names = {"默认（推荐）", "跟随系统", "跟随模块", "强制浅色", "强制深色"};
    final String[] values = {"default", "system", "module", "light", "dark"};
    showChoiceDialogEx(activity, "主题模式", names, values, "ui_theme_mode", "default", 0, 2, "主题已更改", new Runnable() { public void run() {
        String v = getString("settings", "ui_theme_mode", "default");
        if ("dark".equals(v)) putBoolean("settings", "黑白", true);
        else if ("light".equals(v)) putBoolean("settings", "黑白", false);
        refreshSettingsActivityBg();
    } });
}

void showBgTypeChoiceDialog(final Activity activity) {
    final String[] names = {"纯色背景", "三色渐变", "图片背景"};
    final String[] values = {"color", "gradient", "image"};
    showChoiceDialogEx(activity, "背景类型", names, values, "ui_bg_type", "color", 0, 2, "背景类型已更改", new Runnable() { public void run() {
        SettingsState.settingsIndexReady = false;
        refreshSettingsActivityBg();
        try { Activity act2 = getSettingsCurrentActivity(); if (act2 != null) showSettingsMenu(act2, "界面与显示", "背景样式", null); } catch (Throwable ignore) {}
    } });
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
    final String solidListKey = isThemeDark(activity) ? "ui_bg_solid_list_dark" : "ui_bg_solid_list_light";
    String currentColor = getString("settings", keyName, isThemeDark(activity) ? "#FF1E1E1E" : "#FFFFFF");
    String listNow = getString("settings", solidListKey, "");
    boolean hasCustomList = listNow != null && listNow.trim().length() > 0 && listNow.indexOf(",") >= 0;
    String[] namesAll;
    if (hasCustomList) {
        namesAll = new String[colorNames.length + 1];
        for (int i = 0; i < colorNames.length; i++) namesAll[i] = colorNames[i];
        namesAll[colorNames.length] = "自定义列表…";
    } else {
        namesAll = colorNames;
    }
    int checkedItem = 0;
    if (hasCustomList) {
        checkedItem = colorNames.length;
    } else {
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i].equals(currentColor)) {
                checkedItem = i;
                break;
            }
        }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("预设颜色");
    builder.setSingleChoiceItems(namesAll, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (hasCustomList && which == colorNames.length) {
                dialog.dismiss();
                String seed = getString("settings", solidListKey, currentColor);
                if (seed == null || seed.trim().isEmpty()) seed = currentColor;
                showColorListEditor(activity, "背景颜色列表", 0, seed, new ColorListCallback() {
                    public void onColorListSaved(String csv) {
                        putString("settings", solidListKey, csv);
                        if (csv != null && csv.indexOf(",") < 0 && csv.trim().length() > 0) {
                            putString("settings", keyName, csv.trim());
                        }
                        Toast("背景颜色列表已保存");
                    }
                });
                return;
            }
            putString("settings", keyName, colorValues[which]);
            putString("settings", solidListKey, colorValues[which]);
            qqToast(2, "已选择: " + colorNames[which]);
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog, 0);
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
    applyUiTheme(activity, alertDialog, 0);
}

void showFontTypeChoiceDialog(final Activity activity) {
    final String[] names = {"默认字体", "衬线体", "无衬线", "等宽", "粗体"};
    final String[] values = {"default", "serif", "sans", "monospace", "bold"};
    showChoiceDialogEx(activity, "字体风格", names, values, "ui_font_type", "default", 0, 2, "字体已更改", null);
}

void showFontSizeChoiceDialog(final Activity activity) {
    final String[] names = {"小 (0.85x)", "默认 (1.0x)", "中 (1.15x)", "大 (1.3x)"};
    final String[] values = {"0.85", "1.0", "1.15", "1.3"};
    showChoiceDialogEx(activity, "字体大小", names, values, "ui_font_size", "1.0", 1, 2, "字体大小已更改", null);
}

void showThreadPriorityChoiceDialog(final Activity activity) {
    final String[] names = {"1 (最低)", "2", "3", "4", "5 (默认)", "6", "7", "8", "9", "10 (最高)"};
    final String[] values = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    showChoiceDialogEx(activity, "线程优先级", names, values, "thread_pool_priority", "", 4, 2, "优先级已设置", null);
}

void showRejectPolicyChoiceDialog(final Activity activity) {
    final String[] names = {"丢弃最旧任务 (默认)", "丢弃最新任务", "抛出异常", "调用者执行"};
    final String[] values = {"0", "1", "2", "3"};
    showChoiceDialogEx(activity, "任务满载策略", names, values, "thread_pool_reject_policy", "0", 0, 2, "策略已设置", null);
}

void showGifSpeedPicker(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    final int[] delay = new int[]{100};
    try { delay[0] = Integer.parseInt(getString("settings", "gifDelay", "100")); } catch (Throwable e) { delay[0] = 100; }
    if (delay[0] < 10) delay[0] = 10;
    if (delay[0] > 500) delay[0] = 500;

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(activity);
                d.requestWindowFeature(1);
                try {
                    Window w = d.getWindow();
                    if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                } catch (Throwable ignore) {}

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 12));
                root.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

                TextView title = new TextView(activity);
                title.setText("GIF播放速度");
                title.setTextSize(17);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
                root.addView(title);

                final TextView valueTv = new TextView(activity);
                valueTv.setText(delay[0] + " ms/帧");
                valueTv.setTextSize(22);
                valueTv.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
                valueTv.setGravity(Gravity.CENTER);
                valueTv.setPadding(0, dp(activity, 10), 0, dp(activity, 8));
                root.addView(valueTv);

                FrameLayout previewWrap = new FrameLayout(activity);
                previewWrap.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
                previewWrap.setBackgroundColor(pc(getSettingsThemeColor(activity, "background")));
                final View ballA = new View(activity);
                final View ballB = new View(activity);
                GradientDrawable pa = new GradientDrawable();
                pa.setShape(GradientDrawable.OVAL);
                pa.setColor(pc(getSettingsThemeColor(activity, "primary")));
                ballA.setBackground(pa);
                GradientDrawable pb = new GradientDrawable();
                pb.setShape(GradientDrawable.OVAL);
                pb.setColor(pc(getSettingsThemeColor(activity, "primary_container")));
                ballB.setBackground(pb);
                int ballSz = dp(activity, 28);
                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(ballSz, ballSz);
                blp.gravity = Gravity.CENTER;
                previewWrap.addView(ballA, blp);
                previewWrap.addView(ballB, new FrameLayout.LayoutParams(ballSz, ballSz));
                root.addView(previewWrap);
                previewWrap.setMinimumHeight(dp(activity, 80));

                final Handler h = new Handler(Looper.getMainLooper());
                final boolean[] runAnim = new boolean[]{true};
                final boolean[] touching = new boolean[]{false};
                final long[] start = new long[]{0};
                final long[] lastTick = new long[]{0};
                previewWrap.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent e) {
                        if (e.getAction() == MotionEvent.ACTION_DOWN) {
                            touching[0] = true;
                        } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                            touching[0] = false;
                            start[0] = 0;
                            lastTick[0] = 0;
                        }
                        return true;
                    }
                });
                h.post(new Runnable() {
                    public void run() {
                        if (!runAnim[0]) return;
                        long now = android.os.SystemClock.uptimeMillis();
                        if (!touching[0]) {
                            if (lastTick[0] != 0 && now - lastTick[0] < delay[0]) {
                                h.postDelayed(this, delay[0] - (now - lastTick[0]));
                                return;
                            }
                            lastTick[0] = now;
                            if (start[0] == 0) start[0] = now;
                            float t = ((now - start[0]) % 2000f) / 2000f;
                            float ang = t * (float) Math.PI * 2f;
                            float r = dp(activity, 22);
                            ballA.setTranslationX((float) Math.cos(ang) * r);
                            ballA.setTranslationY((float) Math.sin(ang) * r);
                            ballB.setTranslationX((float) Math.cos(ang + Math.PI) * r);
                            ballB.setTranslationY((float) Math.sin(ang + Math.PI) * r);
                        }
                        h.postDelayed(this, 16);
                    }
                });

                SeekBar seek = new SeekBar(activity);
                seek.setMax(490);
                seek.setProgress(delay[0] - 10);
                applyUiSeekBar(seek, activity, pc(getSettingsThemeColor(activity, "primary")));
                root.addView(seek);
                seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                        delay[0] = progress + 10;
                        valueTv.setText(delay[0] + " ms/帧");
                        start[0] = 0;
                        lastTick[0] = 0;
                    }
                    public void onStartTrackingTouch(SeekBar sb) {}
                    public void onStopTrackingTouch(SeekBar sb) {}
                });

                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END);
                btnRow.setPadding(0, dp(activity, 16), 0, 0);
                TextView cancel = createButton(activity, "取消", tc(activity, "on_surface_variant"), Color.TRANSPARENT, 14f, 8, 16, 8, false, 0, 0, null);
                TextView ok = createButton(activity, "确定", Color.WHITE, tc(activity, "primary"), 14f, 8, 16, 8, false, 0, 0, null);
                btnRow.addView(cancel);
                btnRow.addView(ok);
                root.addView(btnRow);

                cancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { runAnim[0] = false; try { d.dismiss(); } catch (Throwable ignore) {} }
                });
                ok.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        runAnim[0] = false;
                        putString("settings", "gifDelay", String.valueOf(delay[0]));
                        Toast("GIF速度: " + delay[0] + "ms/帧");
                        try { d.dismiss(); } catch (Throwable ignore) {}
                    }
                });
                d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) { runAnim[0] = false; }
                });

                d.setContentView(root);
                d.show();
                applyUiTheme(activity, d, 1);
            } catch (Throwable e) { traceLog("setwindow_log", "[showGifSpeedPicker] 异常: " + e); }
        }
    });
}

void showScaleSliderDialog(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    final float[] pending = new float[]{1.0f};
    try { pending[0] = Float.parseFloat(getString("settings", "ui_dialog_scale", "1.0")); } catch (Throwable e) { pending[0] = 1.0f; }

    final Dialog d = new Dialog(activity);
    d.requestWindowFeature(1);
    try {
        Window w = d.getWindow();
        if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    } catch (Throwable ignore) {}

    FrameLayout host = new FrameLayout(activity);
    final LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setPadding(dp(activity, 20), dp(activity, 18), dp(activity, 20), dp(activity, 12));
    rootLayout.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));
    FrameLayout.LayoutParams hostLp = new FrameLayout.LayoutParams(-2, -2);
    hostLp.gravity = Gravity.CENTER;
    host.addView(rootLayout, hostLp);

    TextView title = new TextView(activity);
    title.setText("弹窗大小（本弹窗即预览）");
    title.setTextSize(17);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    rootLayout.addView(title);

    final TextView valueText = new TextView(activity);
    valueText.setText(format2f(pending[0]) + "x");
    valueText.setTextSize(24);
    valueText.setTypeface(null, Typeface.BOLD);
    valueText.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    valueText.setGravity(Gravity.CENTER);
    valueText.setPadding(0, dp(activity, 12), 0, dp(activity, 8));
    rootLayout.addView(valueText);

    final float MIN_SCALE = 0.5f;
    final float MAX_SCALE = 1.5f;
    final float SCALE_STEP = 0.01f;
    final int MAX_PROGRESS = (int) ((MAX_SCALE - MIN_SCALE) / SCALE_STEP);

    SeekBar seekBar = new SeekBar(activity);
    seekBar.setMax(MAX_PROGRESS);
    int startP = (int) ((pending[0] - MIN_SCALE) / SCALE_STEP);
    if (startP < 0) startP = 0;
    if (startP > MAX_PROGRESS) startP = MAX_PROGRESS;
    seekBar.setProgress(startP);
    applyUiSeekBar(seekBar, activity, pc(getSettingsThemeColor(activity, "primary")));
    rootLayout.addView(seekBar);

    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.END);
    btnRow.setPadding(0, dp(activity, 20), 0, 0);
    TextView cancelBtn = createButton(activity, "取消", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 8, 16, 8, false, 0, 0, null);
    TextView okBtn = createButton(activity, "确定", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 8, 16, 8, false, 0, 0, null);
    btnRow.addView(cancelBtn);
    btnRow.addView(okBtn);
    rootLayout.addView(btnRow);

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
            pending[0] = MIN_SCALE + progress * SCALE_STEP;
            valueText.setText(format2f(pending[0]) + "x");
            host.setScaleX(pending[0]);
            host.setScaleY(pending[0]);
        }
        public void onStartTrackingTouch(SeekBar sb) {}
        public void onStopTrackingTouch(SeekBar sb) {}
    });

    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { try { d.dismiss(); } catch (Throwable ignore) {} }
    });
    okBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            putString("settings", "ui_dialog_scale", format2f(pending[0]));
            updateSettingsItemText("ui_dialog_scale", format2f(pending[0]) + "x");
            Toast("弹窗大小: " + format2f(pending[0]) + "x");
            try { d.dismiss(); } catch (Throwable ignore) {}
        }
    });

    d.setContentView(host);
    d.show();
    applyUiTheme(activity, d, 1);
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
    previewTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    previewTitle.setGravity(Gravity.CENTER);
    previewContent.addView(previewTitle);

    TextView previewText = new TextView(activity);
    previewText.setText("这是一段测试文本，用于预览设置效果。");
    previewText.setTextSize(14);
    previewText.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    previewText.setGravity(Gravity.CENTER);
    previewText.setPadding(0, dp(activity, 12), 0, dp(activity, 16));
    previewContent.addView(previewText);

    LinearLayout buttonRow = new LinearLayout(activity);
    buttonRow.setOrientation(LinearLayout.HORIZONTAL);
    buttonRow.setGravity(Gravity.CENTER);

    TextView toastButton = createButton(activity, "测试Toast", pc(getSettingsThemeColor(activity, "primary")), pc(getSettingsThemeColor(activity, "surface")), 14f, 20, 24, 12, false, 0, 0, null);
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
    applyUiTheme(activity, previewDialog, 0);
}

void addSettingsLocationBlock(String categoryName) {
    if (SettingsState.settingsIndexMode) {
        registerSettingsIndexEntry("loc_lng", "经度", "模拟定位经度", "input");
        registerSettingsIndexEntry("loc_lat", "纬度", "模拟定位纬度", "input");
        return;
    }
    if (SettingsState.settingsCategoryContainers == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;
    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container == null) return;

    container.addView(buildSettingsLocationRow(activity, "经度", "如 116.397", "lng"));
    container.addView(buildSettingsLocationRow(activity, "纬度", "如 39.917", "lat"));
}

View buildSettingsLocationRow(Activity activity, String labelText, String hintText, final String keyName) {
    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));

    TextView nameView = new TextView(activity);
    nameView.setText(labelText);
    nameView.setTextSize(16);
    nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    itemLayout.addView(nameView);

    final EditText inputEdit = makeInput(activity, hintText, null);
    inputEdit.setText(getString("模拟定位", keyName, ""));
    inputEdit.setTextSize(14);
    inputEdit.setSingleLine(true);
    inputEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    itemLayout.addView(inputEdit);

    final long[] inputToken = new long[1];
    inputEdit.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            final String value = editable != null ? editable.toString().trim() : "";
            inputToken[0] = System.currentTimeMillis();
            final long token = inputToken[0];
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (token != inputToken[0]) return;
                    if (value.length() == 0) return;
                    try { Double.parseDouble(value); } catch (Throwable e) { return; }
                    putString("模拟定位", keyName, value);
                }
            }, 300);
        }
    });

    itemLayout.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
    itemLayout.setClickable(true);
    return itemLayout;
}


public LinearLayout build运行状态ContentView(Activity activity) {
    if (activity == null) return null;
    boolean isDark = isThemeDark(activity);

    LinearLayout contentLayout = new LinearLayout(activity);
    contentLayout.setOrientation(LinearLayout.VERTICAL);
    contentLayout.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));

    addQQ状态卡片(activity, contentLayout, isDark);
    add开关状态卡片(activity, contentLayout, isDark);
    add监控卡片(activity, contentLayout, isDark);
    add电池信息卡片(activity, contentLayout, isDark);
    add系统资源卡片(activity, contentLayout, isDark);
    add模块信息卡片(activity, contentLayout, isDark);
    add脚本信息卡片(activity, contentLayout, isDark);
    addJVM内存信息卡片(activity, contentLayout, isDark);
    add设备信息卡片(activity, contentLayout, isDark);

    TextView footer = new TextView(activity);
    footer.setText("Powered by QFun Engine");
    footer.setGravity(Gravity.CENTER);
    footer.setTextColor(pc(isDark ? "#555555" : "#AAAAAA"));
    footer.setTextSize(10);
    footer.setPadding(0, dp(activity, 4), 0, dp(activity, 4));
    contentLayout.addView(footer);
    return contentLayout;
}

public LinearLayout buildStatsContentView(Activity activity) {
    if (activity == null) return null;
    readFullStats();

    LinearLayout contentLayout = new LinearLayout(activity);
    contentLayout.setOrientation(LinearLayout.VERTICAL);
    contentLayout.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

    contentLayout.addView(createRangeSpinner(activity));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createTotalStatsCard(activity, "📈 累计统计"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createTodayCoreStatsCard(activity, "📊  今日统计"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createReceiveStatsCard(activity, "📥 接收消息统计", "receive"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createSendStatsCard(activity, "📤 发送消息统计", "send"));
    contentLayout.addView(createSpaceView(activity, 18));

    LinearLayout buttonLayout1 = new LinearLayout(activity);
    buttonLayout1.setOrientation(LinearLayout.HORIZONTAL);
    buttonLayout1.setGravity(Gravity.CENTER_HORIZONTAL);
    buttonLayout1.setPadding(0, 0, 0, dp(activity, 10));

    TextView resetTodayBtn = createButton(activity, "重置今日数据", pc("#333333"), pc("#FFCDD2"), 13f, 8, 16, 8, false, 0, 0, null);
    LinearLayout.LayoutParams resetTodayParams = new LinearLayout.LayoutParams(dp(activity, 120), dp(activity, 40));
    resetTodayParams.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
    resetTodayBtn.setLayoutParams(resetTodayParams);
    resetTodayBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            resetTodayStats(activity);
        }
    });

    TextView resetTotalBtn = createButton(activity, "重置累计数据", pc("#333333"), pc("#BBDEFB"), 13f, 8, 16, 8, false, 0, 0, null);
    LinearLayout.LayoutParams resetTotalParams = new LinearLayout.LayoutParams(dp(activity, 120), dp(activity, 40));
    resetTotalParams.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
    resetTotalBtn.setLayoutParams(resetTotalParams);
    resetTotalBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            resetTotalStats(activity);
        }
    });

    buttonLayout1.addView(resetTodayBtn);
    buttonLayout1.addView(resetTotalBtn);

    LinearLayout buttonLayout2 = new LinearLayout(activity);
    buttonLayout2.setOrientation(LinearLayout.HORIZONTAL);
    buttonLayout2.setGravity(Gravity.CENTER_HORIZONTAL);
    buttonLayout2.setPadding(0, 0, 0, dp(activity, 10));

    TextView repairBtn = createButton(activity, "修复数据", pc("#333333"), pc("#C8E6C9"), 13f, 8, 16, 8, false, 0, 0, null);
    LinearLayout.LayoutParams repairParams = new LinearLayout.LayoutParams(dp(activity, 120), dp(activity, 40));
    repairParams.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
    repairBtn.setLayoutParams(repairParams);
    repairBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            repairStatsData(activity);
        }
    });

    TextView targetBtn = createButton(activity, "设置每日目标", pc("#333333"), pc("#F8BBD0"), 13f, 8, 16, 8, false, 0, 0, null);
    LinearLayout.LayoutParams targetParams = new LinearLayout.LayoutParams(dp(activity, 120), dp(activity, 40));
    targetParams.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);
    targetBtn.setLayoutParams(targetParams);
    targetBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 48);
            showTargetSettingDialog(activity);
        }
    });

    buttonLayout2.addView(repairBtn);
    buttonLayout2.addView(targetBtn);

    contentLayout.addView(buttonLayout1);
    contentLayout.addView(buttonLayout2);
    return contentLayout;
}

public void bindStatsViewCache(final View rootView) {
    if (rootView == null) return;
    statsContentRoot = rootView;
    statsPageVisible = true;
    msgHandle.postDelayed(new Runnable() {
        public void run() {
            try {
                if (statsTextViewCache == null) statsTextViewCache = new ArrayList();
                statsTextViewCache.clear();
                String[] tagsToFind = {
                    "totalReceive", "totalSend", "Receive", "Send",
                    "ReceiveText", "ReceivePic", "ReceiveFile", "ReceiveVideo", "ReceiveEmoji",
                    "ReceiveAudio", "ReceiveCard", "ReceiveCall", "ReceiveGrayTip",
                    "ReceiveWordCount", "ReceiveUnknown",
                    "SendText", "SendPic", "SendFile", "SendVideo", "SendEmoji",
                    "SendAudio", "SendCard", "SendCall",
                    "SendWordCount", "SendUnknown"
                };
                for (int i = 0; i < tagsToFind.length; i++) {
                    TextView tv = (TextView) rootView.findViewWithTag(tagsToFind[i]);
                    if (tv != null) statsTextViewCache.add(tv);
                }
                todayCoreCardCache = (LinearLayout) rootView.findViewWithTag("todayCoreCard");
                if (todayCoreCardCache != null) {
                    sendTargetLabelCache = (TextView) todayCoreCardCache.findViewWithTag("send_target_label");
                    progressBarCache = todayCoreCardCache.findViewWithTag("send_progress");
                    achievementTextCache = (TextView) todayCoreCardCache.findViewWithTag("achievement_text");
                }
                triggerUIUpdate();
            } catch (Throwable e) {
                traceLog("api3_log", "[bindStatsViewCache] 异常: " + e);
            }
        }
    }, 150);
}

public void unbindStatsViewCache() {
    statsPageVisible = false;
    statsContentRoot = null;
    try { if (statsTextViewCache != null) statsTextViewCache.clear(); } catch (Throwable ignore) {}
    todayCoreCardCache = null;
    progressBarCache = null;
    sendTargetLabelCache = null;
    achievementTextCache = null;
}

public LinearLayout buildQzoneContentView(final Activity act, boolean withTitle, final Runnable onClose) {
    if (act == null) return null;
    final Activity finalAct = act;

    LinearLayout card = new LinearLayout(finalAct);
    card.setOrientation(LinearLayout.VERTICAL);
    int cardBg = getAdaptiveCardBg(finalAct);
    card.setBackground(roundRect(cardBg, dp(finalAct, 16)));
    card.setClipToOutline(true);
    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
    cardLp.setMargins(dp(finalAct, 12), 0, dp(finalAct, 12), dp(finalAct, 8));

    if (withTitle) {
        TextView title = new TextView(finalAct);
        title.setText("空间操作配置");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(pc(getSettingsThemeColor(finalAct, "on_surface")));
        title.setPadding(dp(finalAct, 28), dp(finalAct, 20), dp(finalAct, 16), dp(finalAct, 8));
        card.addView(title);
    }

    FrameLayout swWrap1 = new FrameLayout(finalAct);
    LinearLayout swRow1 = new LinearLayout(finalAct);
    swRow1.setOrientation(LinearLayout.HORIZONTAL);
    swRow1.setGravity(Gravity.CENTER_VERTICAL);
    swRow1.setPadding(dp(finalAct, 16), dp(finalAct, 14), dp(finalAct, 16), dp(finalAct, 14));
    TextView swLbl1 = new TextView(finalAct);
    swLbl1.setText("秒赞");
    swLbl1.setTextSize(16);
    swLbl1.setTextColor(pc(getSettingsThemeColor(finalAct, "on_surface")));
    swRow1.addView(swLbl1, new LinearLayout.LayoutParams(0, -2, 1.0f));
    final View sw1 = createSettingsSwitchView(finalAct, getBoolean("qzone_cfg", "switch_like", false), "qzone_cfg", "switch_like", "秒赞", new Runnable() {
        public void run() { checkAndStartOrStopThread(); }
    });
    swRow1.addView(sw1);
    swWrap1.addView(swRow1);
    swWrap1.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(finalAct), pc(getSettingsThemeColor(finalAct, "ripple")), 0));
    swWrap1.setClickable(true);
    swWrap1.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { if (sw1 != null) sw1.performClick(); }
    });
    card.addView(swWrap1);

    FrameLayout swWrap2 = new FrameLayout(finalAct);
    LinearLayout swRow2 = new LinearLayout(finalAct);
    swRow2.setOrientation(LinearLayout.HORIZONTAL);
    swRow2.setGravity(Gravity.CENTER_VERTICAL);
    swRow2.setPadding(dp(finalAct, 16), dp(finalAct, 14), dp(finalAct, 16), dp(finalAct, 14));
    TextView swLbl2 = new TextView(finalAct);
    swLbl2.setText("秒评");
    swLbl2.setTextSize(16);
    swLbl2.setTextColor(pc(getSettingsThemeColor(finalAct, "on_surface")));
    swRow2.addView(swLbl2, new LinearLayout.LayoutParams(0, -2, 1.0f));
    final View sw2 = createSettingsSwitchView(finalAct, getBoolean("qzone_cfg", "switch_comment", false), "qzone_cfg", "switch_comment", "秒评", new Runnable() {
        public void run() { checkAndStartOrStopThread(); }
    });
    swRow2.addView(sw2);
    swWrap2.addView(swRow2);
    swWrap2.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(finalAct), pc(getSettingsThemeColor(finalAct, "ripple")), 0));
    swWrap2.setClickable(true);
    swWrap2.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { if (sw2 != null) sw2.performClick(); }
    });
    card.addView(swWrap2);

    card.addView(makeSubTitleCompact(finalAct, "评论内容", pc(getSettingsThemeColor(finalAct, "on_surface_variant"))));
    String initText = getString("qzone_cfg", "comment", "我来暖说说啦！");
    LinearLayout commentWrap = new LinearLayout(finalAct);
    commentWrap.setOrientation(LinearLayout.VERTICAL);
    commentWrap.setPadding(dp(finalAct, 16), 0, dp(finalAct, 16), dp(finalAct, 8));
    EditText commentInput = makeInput(finalAct, "输入评论（≤100字）", null);
    commentInput.setText(initText);
    commentInput.setMaxLines(4);
    commentInput.setGravity(Gravity.TOP | Gravity.START);
    commentWrap.addView(commentInput);
    card.addView(commentWrap);

    card.addView(makeSubTitleCompact(finalAct, "间隔设置", pc(getSettingsThemeColor(finalAct, "on_surface_variant"))));
    LinearLayout intervalRow = new LinearLayout(finalAct);
    intervalRow.setOrientation(LinearLayout.HORIZONTAL);
    intervalRow.setGravity(Gravity.CENTER_VERTICAL);
    intervalRow.setPadding(dp(finalAct, 16), 0, dp(finalAct, 16), dp(finalAct, 8));
    int itemSpacing = dp(finalAct, 12);

    LinearLayout fetchDelayLayout = new LinearLayout(finalAct);
    fetchDelayLayout.setOrientation(LinearLayout.VERTICAL);
    fetchDelayLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    TextView qzLbl1 = new TextView(finalAct);
    qzLbl1.setText("拉取列表后延迟（秒）");
    qzLbl1.setTextSize(12);
    qzLbl1.setTextColor(pc(getSettingsThemeColor(finalAct, "on_surface_variant")));
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
    qzLbl2.setTextColor(pc(getSettingsThemeColor(finalAct, "on_surface_variant")));
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

    card.addView(makeSubTitleCompact(finalAct, "黑名单设置", pc(getSettingsThemeColor(finalAct, "on_surface_variant"))));
    FrameLayout blackWrap = new FrameLayout(finalAct);
    LinearLayout blackRow = new LinearLayout(finalAct);
    blackRow.setOrientation(LinearLayout.HORIZONTAL);
    blackRow.setGravity(Gravity.CENTER_VERTICAL);
    blackRow.setPadding(dp(finalAct, 16), dp(finalAct, 14), dp(finalAct, 16), dp(finalAct, 14));

    TextView blackBtn = createButton(finalAct, "设置黑名单", pc(getSettingsThemeColor(finalAct, "primary")), Color.TRANSPARENT, 14f, 24, 0, 0, false, 0, 0, null);
    blackRow.addView(blackBtn, new LinearLayout.LayoutParams(-2, -2));

    Space spacer = new Space(finalAct);
    blackRow.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1.0f));

    final TextView blackLabel = new TextView(finalAct);
    blackLabel.setText("已屏蔽 " + blackList.size() + " 人");
    blackLabel.setTextSize(16);
    blackLabel.setTextColor(pc(getSettingsThemeColor(finalAct, "on_surface")));
    blackRow.addView(blackLabel, new LinearLayout.LayoutParams(-2, -2));
    blackWrap.addView(blackRow);
    blackWrap.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(finalAct), pc(getSettingsThemeColor(finalAct, "ripple")), 0));
    blackWrap.setClickable(true);
    card.addView(blackWrap);

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

    attachQzoneInputSaver(finalAct, commentInput, "comment", 100, 0);
    attachQzoneInputSaver(finalAct, fetchDelayInput, "fetch_delay_ms", 0, 5000);
    attachQzoneInputSaver(finalAct, feedIntervalInput, "feed_interval_ms", 0, 1000);

    card.setLayoutParams(cardLp);
    return card;
}

void attachQzoneInputSaver(final Activity act, final EditText input, final String key, final int maxLen, final int fallbackMs) {
    if (input == null || key == null) return;
    final long[] token = new long[1];
    input.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            final String value = editable != null ? editable.toString().trim() : "";
            token[0] = System.currentTimeMillis();
            final long t = token[0];
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (t != token[0]) return;
                    try {
                        if (maxLen > 0) {
                            String text = value;
                            if (text.length() > maxLen) text = text.substring(0, maxLen);
                            putString("qzone_cfg", key, text);
                        } else {
                            int fbSec = (fallbackMs > 0) ? (fallbackMs / 1000) : 1;
                            int sec = fbSec;
                            try {
                                int parsed = Integer.parseInt(value);
                                if (parsed > 0) sec = parsed;
                            } catch (Throwable ignore) {}
                            putInt("qzone_cfg", key, sec * 1000);
                        }
                        checkAndStartOrStopThread();
                    } catch (Throwable ignore) {}
                }
            }, 300);
        }
    });
}

public View buildHotPlugContentView(final Activity a, String gid, String uname, final Runnable onClose, boolean fillParent) {
    if (a == null) return null;
    final String g = (gid != null) ? gid : "";
    final String gn = uname;
    isAdding = false;

        FrameLayout root = new FrameLayout(a);
        root.setPadding(dp(a, 20), dp(a, 16), dp(a, 20), dp(a, 20));

        final SwipeRefreshLayout swipe = new SwipeRefreshLayout(a);
        root.addView(swipe, new FrameLayout.LayoutParams(-1, fillParent ? -1 : -2));
        swipe.setPadding(0, 0, 0, 0);

        final ScrollView sc = new ScrollView(a);
        swipe.addView(sc, new FrameLayout.LayoutParams(-1, fillParent ? -1 : -2));

        final LinearLayout cd = new LinearLayout(a);
        cd.setOrientation(LinearLayout.VERTICAL);
        cd.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
        sc.addView(cd);

        TextView t = new TextView(a);
        t.setText("功能管理");
        t.setTextSize(16);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(tc(a, "on_surface"));
        cd.addView(t);

        TextView s = new TextView(a);
        String gn2 = (gn == null || gn.trim().length() == 0) ? "未知" : gn;
        s.setText(gn2 + (g.equals("") ? "" : " (" + g + ")"));
        s.setTextSize(12);
        s.setTextColor(tc(a, "primary"));
        s.setPadding(0, 0, 0, dp(a, 6));
        cd.addView(s);

        if (g.length() > 0 && (gn == null || gn.length() == 0)) {
            ThreadPool.execute(new Runnable() {
                public void run() {
                    String name = "";
                    try {
                        Object ti = findTroopInfo(g);
                        if (ti != null) {
                            String tn = String.valueOf(ti.troopname);
                            if (tn != null && tn.length() > 0 && !"null".equals(tn)) name = tn;
                        }
                    } catch (Throwable ignore) {}
                    if (name.length() > 0) {
                        final String fn = name;
                        a.runOnUiThread(new Runnable() {
                            public void run() {
                                try { s.setText(fn + " (" + g + ")"); } catch (Throwable ignore) {}
                            }
                        });
                    }
                }
            });
        }

        TextView tips = new TextView(a);
        tips.setText("下拉可刷新状态");
        tips.setTextSize(9);
        tips.setTextColor(tc(a, "on_surface_variant"));
        tips.setPadding(0, 0, 0, dp(a, 8));
        cd.addView(tips);

        final LinearLayout editorContainer = new LinearLayout(a);
        editorContainer.setOrientation(LinearLayout.VERTICAL);
        editorContainer.setBackground(roundRect(tc(a, "primary_container"), dp(a, 8)));
        editorContainer.setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12));
        editorContainer.setVisibility(View.GONE);
        editorContainer.setAlpha(0f);
        editorContainer.setScaleY(0.8f);
        cd.addView(editorContainer);

        TextView editTitle = new TextView(a);
        editTitle.setText("✏️ 新建功能");
        editTitle.setTextSize(15);
        editTitle.setTextColor(tc(a, "primary"));
        editTitle.setPadding(0, 0, 0, dp(a, 8));
        editorContainer.addView(editTitle);

        final boolean[] isFileState = {false};
        final boolean[] cks = new boolean[8];

        final EditText etN = addNameInput(a, editorContainer, "");
        final EditText etC = addCodeInput(a, editorContainer, "", false, isFileState);
        addPresetRows(a, editorContainer, etC);

        TextView varTips = new TextView(a);
        varTips.setText("变量:qun群号 uinQQ msg消息 type类型(1私2群) operator操作者 time秒");
        varTips.setTextSize(9);
        varTips.setTextColor(tc(a, "on_surface_variant"));
        varTips.setPadding(0, dp(a, 4), 0, 0);
        editorContainer.addView(varTips);

        final FormComponents fc = new FormComponents();
        TextView[] chips = addChipRows(a, editorContainer, cks, fc);

        addPreprocRow(a, editorContainer, fc, 0, "", 0, 0);
        updatePreprocVisibility(fc, false);

        FormComponents loopFc = addLoopRow(a, editorContainer, false, 5000, 0, cks);
        fc.etInterval = loopFc.etInterval;
        fc.etCount = loopFc.etCount;
        fc.chipLoop = loopFc.chipLoop;
        fc.loopSettings = loopFc.loopSettings;

        final EditText etTime = addTimeRow(a, editorContainer, "");

        LinearLayout editorBtnRow = new LinearLayout(a);
        editorBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        editorBtnRow.setPadding(0, dp(a, 12), 0, 0);
        editorContainer.addView(editorBtnRow);

        TextView btnSave = createButton(a, "保存", Color.WHITE, tc(a, "primary"), 14f, 8, 16, 10, false, 0, 0, null);
        btnSave.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        editorBtnRow.addView(btnSave);

        TextView btnTest = createButton(a, "测试", tc(a, "on_surface_variant"), tc(a, "surface"), 14f, 8, 16, 10, false, 0, 0, null);
        LinearLayout.LayoutParams lpTest = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lpTest.setMargins(dp(a, 6), 0, dp(a, 6), 0);
        btnTest.setLayoutParams(lpTest);
        editorBtnRow.addView(btnTest);

        TextView btnCancel = createButton(a, "取消", tc(a, "on_surface_variant"), tc(a, "surface"), 14f, 8, 16, 10, false, 0, 0, null);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        editorBtnRow.addView(btnCancel);

        final LinearLayout lst = new LinearLayout(a);
        lst.setOrientation(LinearLayout.VERTICAL);
        lst.setPadding(0, dp(a, 6), 0, 0);
        cd.addView(lst);

        TextView btnAdd = createButton(a, "+ 新建功能", Color.WHITE, tc(a, "primary"), 14f, 8, 16, 10, false, 0, 0, null);
        LinearLayout.LayoutParams btnAddLp = new LinearLayout.LayoutParams(-1, -2);
        btnAddLp.setMargins(0, dp(a, 6), 0, 0);
        cd.addView(btnAdd, cd.indexOfChild(lst), btnAddLp);

        View ln = new View(a);
        ln.setBackgroundColor(tc(a, "outline"));
        ln.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(a, 1)));
        ((LinearLayout.LayoutParams) ln.getLayoutParams()).setMargins(0, dp(a, 10), 0, dp(a, 10));
        cd.addView(ln, cd.indexOfChild(lst));

        TextView lt = new TextView(a);
        lt.setText("功能列表(长按编辑, 左滑删除):");
        lt.setTextSize(12);
        lt.setTextColor(tc(a, "on_surface_variant"));
        cd.addView(lt, cd.indexOfChild(lst));

        final Runnable refreshCallback = new Runnable() {
            public void run() {
                a.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            swipe.setRefreshing(true);
                            lst.removeAllViews();
                            String[] fs = getAll();
                            if (fs.length == 0 || (fs.length == 1 && fs[0].equals(""))) {
                                TextView e = new TextView(a);
                                e.setText("暂无功能");
                                e.setTextColor(tc(a, "on_surface_variant"));
                                lst.addView(e);
                            } else {
                                for (String f : fs) {
                                    if (!f.equals("")) {
                                        createItem(a, lst, f, g, gn, refreshCallback);
                                    }
                                }
                            }
                            swipe.postDelayed(new Runnable() {
                                public void run() {
                                    swipe.setRefreshing(false);
                                }
                            }, 300);
                        } catch (Throwable e) {
                            traceLog("function_log", "[refreshCallback] 异常: " + e);
                            swipe.setRefreshing(false);
                        }
                    }
                });
            }
        };

        refreshCallback.run();

        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                swipe.postDelayed(new Runnable() {
                    public void run() {
                        refreshCallback.run();
                    }
                }, 50);
            }
        });
        swipe.setColorSchemeColors(tc(a, "primary"));

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String n = etN.getText().toString().trim();
                String c = etC.getText().toString();
                if (n.equals("") || (c.equals("") && !cks[6])) {
                    toast("名称和内容不能为空(选择预处理时可不填代码)");
                    return;
                }
                boolean hasCb = false;
                for (int i = 0; i < 7; i++) if (cks[i]) hasCb = true;

                long intervalVal = 0;
                int countVal = 0;
                int preTypeVal = 0;
                String preTailVal = "";
                int repeatSendVal = 0;
                int repeatConcatVal = 0;

                if (!hasCb) {
                    try {
                        intervalVal = Long.parseLong(fc.etInterval.getText().toString());
                    } catch (Throwable e) {
                        toast("间隔格式错误");
                        return;
                    }
                    try {
                        countVal = Integer.parseInt(fc.etCount.getText().toString());
                    } catch (Throwable e) {
                        countVal = 0;
                    }
                }

                if (cks[6] && fc.preTypeChips != null) {
                    for (int i = 0; i < 50; i++) {
                        if (i < fc.preTypeChips.length && Boolean.TRUE.equals(fc.preTypeChips[i].getTag())) {
                            preTypeVal = i;
                            break;
                        }
                    }
                    preTailVal = fc.etPreTail.getText().toString();
                    try {
                        repeatSendVal = Integer.parseInt(fc.etRepeatSend.getText().toString());
                    } catch (Throwable e) { traceLog("function_log", "[onRefresh] 异常: " + e); }
                    try {
                        repeatConcatVal = Integer.parseInt(fc.etRepeatConcat.getText().toString());
                    } catch (Throwable e) { traceLog("function_log", "[onRefresh] 异常: " + e); }
                }

                String rawTime = etTime.getText().toString().trim();

                try {
                    saveFunc(n, c, isFileState[0], new boolean[]{cks[0], cks[1], cks[2], cks[3], cks[4], cks[5], cks[6]}, cks[7], rawTime, intervalVal, Boolean.TRUE.equals(fc.chipLoop.getTag()), countVal, preTypeVal, preTailVal, repeatSendVal, repeatConcatVal);
                    toast("已添加:" + n);
                    editorContainer.animate().scaleY(0.8f).alpha(0f).setDuration(300).withEndAction(new Runnable() {
                        public void run() {
                            editorContainer.setVisibility(View.GONE);
                            isAdding = false;
                            refreshCallback.run();
                        }
                    }).start();
                } catch (Throwable e) {
                    traceLog("function_log", "[btnSave] 保存异常: " + e);
                    toast("保存失败");
                }
            }
        });

        btnTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String c = etC.getText().toString();
                if (c.equals("")) {
                    toast("代码为空");
                    return;
                }
                testCode(etN.getText().toString().trim(), c, "新建测试");
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editorContainer.animate().scaleY(0.8f).alpha(0f).setDuration(300).withEndAction(new Runnable() {
                    public void run() {
                        editorContainer.setVisibility(View.GONE);
                        isAdding = false;
                    }
                }).start();
            }
        });

        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isAdding && editorContainer.getVisibility() == View.VISIBLE) {
                    toast("已有未保存的编辑");
                    return;
                }

                try {
                    isAdding = true;
                    etN.setText("");
                    etC.setText("");
                    etTime.setText("");
                    isFileState[0] = false;
                    for (int i = 0; i < 8; i++) {
                        cks[i] = false;
                        if (i < 7) setChip(chips[i], false);
                    }
                    if (Boolean.TRUE.equals(fc.chipLoop.getTag())) {
                        fc.chipLoop.performClick();
                    }
                    fc.etInterval.setText("5000");
                    fc.etCount.setText("0");

                    if (fc.preTypeChips != null) {
                        for (int j = 0; j < 50; j++) {
                            if(j < fc.preTypeChips.length) setChipWithType(fc.preTypeChips[j], j == 0, 2);
                        }
                        fc.etPreTail.setText("");
                        fc.etRepeatSend.setText("");
                        fc.etRepeatConcat.setText("");
                    }
                    updatePreprocVisibility(fc, false);

                    editorContainer.setVisibility(View.VISIBLE);
                    editorContainer.setScaleY(0.8f);
                    editorContainer.setAlpha(0f);
                    editorContainer.setTranslationY(-dp(a, 20));

                    AnimatorSet set = new AnimatorSet();
                    ObjectAnimator scale = ObjectAnimator.ofFloat(editorContainer, "scaleY", new float[]{0.8f, 1f});
                    ObjectAnimator alpha = ObjectAnimator.ofFloat(editorContainer, "alpha", new float[]{0f, 1f});
                    ObjectAnimator trans = ObjectAnimator.ofFloat(editorContainer, "translationY", new float[]{-dp(a, 20), 0f});
                    android.animation.Animator[] animArr = new android.animation.Animator[3];
                    animArr[0] = scale;
                    animArr[1] = alpha;
                    animArr[2] = trans;
                    set.playTogether(animArr);
                    set.setDuration(400);
                    set.setInterpolator(new OvershootInterpolator(1.2f));
                    set.start();

                } catch (Throwable e) {
                    traceLog("function_log", "[btnAdd] 异常: " + e);
                    isAdding = false;
                }
            }
        });

    return root;
}

View buildHtmlPageView(final Activity activity) {
    if (activity == null) return null;

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));

    LinearLayout urlRow = new LinearLayout(activity);
    urlRow.setOrientation(LinearLayout.HORIZONTAL);
    urlRow.setGravity(Gravity.CENTER_VERTICAL);

    final EditText urlInput = makeInput(activity, "输入网址或 HTML 代码", null);
    urlInput.setTextSize(13);
    urlInput.setSingleLine(true);
    urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
    urlInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    urlRow.addView(urlInput);

    TextView openBtn = createButton(activity, "打开", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 13f, 8, 14, 8, false, 0, 0, null);
    LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(-2, -2);
    openLp.leftMargin = dp(activity, 8);
    openBtn.setLayoutParams(openLp);
    urlRow.addView(openBtn);
    root.addView(urlRow);

    final Runnable openUrl = new Runnable() {
        public void run() {
            try {
                String url = urlInput.getText().toString().trim();
                if (url.length() == 0) { Toast("请输入网址或 HTML 代码"); return; }
                if (!url.startsWith("http") && !url.startsWith("file")) url = "http://" + url;
                launchHtmlActivity(activity, url);
            } catch (Throwable e) { Toast("打开失败: " + e.getMessage()); }
        }
    };
    openBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { openUrl.run(); }
    });
    urlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView tv, int actionId, KeyEvent keyEvent) {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                openUrl.run();
                return true;
            }
            return false;
        }
    });

    View divider = new View(activity);
    divider.setBackgroundColor(pc(getSettingsThemeColor(activity, "outline")));
    LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(activity, 1));
    dividerLp.topMargin = dp(activity, 12);
    divider.setLayoutParams(dividerLp);
    root.addView(divider);

    LinearLayout listHeader = new LinearLayout(activity);
    listHeader.setOrientation(LinearLayout.HORIZONTAL);
    listHeader.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(-1, -2);
    headerLp.topMargin = dp(activity, 12);
    listHeader.setLayoutParams(headerLp);

    TextView listTitle = new TextView(activity);
    listTitle.setText("本地文件");
    listTitle.setTextSize(14);
    listTitle.setTypeface(null, Typeface.BOLD);
    listTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    listTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    listHeader.addView(listTitle);

    TextView importBtn = new TextView(activity);
    importBtn.setText("导入新文件");
    importBtn.setTextSize(13);
    importBtn.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    importBtn.setPadding(dp(activity, 8), dp(activity, 4), 0, dp(activity, 4));
    importBtn.setClickable(true);
    importBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 32);
            startHtmlFilePicker(activity);
        }
    });
    listHeader.addView(importBtn);
    root.addView(listHeader);

    ScrollView sv = new ScrollView(activity);
    sv.setVerticalScrollBarEnabled(false);
    sv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
    LinearLayout list = new LinearLayout(activity);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
    sv.addView(list);
    root.addView(sv);

    refreshHtmlFileList(activity, list);
    return root;
}

void refreshHtmlFileList(Activity activity, LinearLayout list) {
    if (activity == null || list == null) return;
    list.removeAllViews();
    try {
        File dir = new File(htmlPath);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            TextView empty = new TextView(activity);
            empty.setText("暂无文件，点右上角「导入新文件」添加");
            empty.setTextSize(13);
            empty.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            empty.setPadding(0, dp(activity, 20), 0, 0);
            list.addView(empty);
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f == null || !f.isFile()) continue;
            String lower = f.getName().toLowerCase();
            if (!(lower.endsWith(".html") || lower.endsWith(".zip") || lower.endsWith(".php"))) continue;
            list.addView(buildHtmlFileRow(activity, f, list));
        }
    } catch (Throwable e) {
        traceLog("setwindow_log", "[refreshHtmlFileList] 异常: " + e);
    }
}

View buildHtmlFileRow(final Activity activity, final File f, final LinearLayout list) {
    final String path = f.getAbsolutePath();

    LinearLayout row = new LinearLayout(activity);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 8), dp(activity, 12));
    row.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), dp(activity, 14)));
    row.setClipToOutline(true);
    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
    rowLp.setMargins(0, dp(activity, 5), 0, dp(activity, 5));
    row.setLayoutParams(rowLp);

    LinearLayout textArea = new LinearLayout(activity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

    TextView nameTv = new TextView(activity);
    nameTv.setText(f.getName());
    nameTv.setTextSize(15);
    nameTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    textArea.addView(nameTv);

    TextView sizeTv = new TextView(activity);
    sizeTv.setText(formatSize(f.length()));
    sizeTv.setTextSize(12);
    sizeTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    sizeTv.setPadding(0, dp(activity, 3), 0, 0);
    textArea.addView(sizeTv);
    row.addView(textArea);

    TextView delBtn = new TextView(activity);
    delBtn.setText("删除");
    delBtn.setTextSize(13);
    delBtn.setTextColor(pc(getSettingsThemeColor(activity, "error")));
    delBtn.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
    delBtn.setClickable(true);
    delBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 32);
            showSettingsConfirmDialog(activity, "确认删除", "确定要删除「" + f.getName() + "」吗？", new Runnable() {
                public void run() {
                    try {
                        删除(path);
                        Toast("已删除");
                    } catch (Throwable e) { Toast("删除失败: " + e.getMessage()); }
                    refreshHtmlFileList(activity, list);
                }
            });
        }
    });
    row.addView(delBtn);

    row.setClickable(true);
    row.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { launchHtmlActivity(activity, path); }
    });
    return row;
}


class SettingsHistoryEntry {
    String key;
    int count;
    long time;
}

class SettingsPage {
    String level1;
    String level2;
    String level3;
    View pageView;
    LinearLayout listContainer;
    ScrollView scrollView;
    Map categoryContainers;
    Map itemViews;
    Map itemTextViews;
}

class SettingsActivity extends BaseComposeActivity {
    FrameLayout settingsActivityRoot;
    FrameLayout settingsPageContainer;
    List settingsPageStack;
    Handler settingsPageHandler;
    String settingsHistoryJson;
    java.util.concurrent.ExecutorService settingsHistoryWriter;

    TextView settingsHomeTitle;
    LinearLayout settingsSearchRow;
    LinearLayout settingsSearchBar;
    EditText settingsSearchInput;
    TextView settingsSearchCancel;
    ScrollView settingsCategoryScroll;
    LinearLayout settingsCategoryList;
    ScrollView settingsResultScroll;
    LinearLayout settingsResultWrapper;
    LinearLayout settingsHistoryContainer;
    LinearLayout settingsResultList;
    boolean settingsSearchMode = false;
    boolean settingsAnimating = false;

    int settingsChatType;
    String settingsPeerUin = "";
    String settingsPeerName = "";

    LinearLayout settingsHomeFooter;
    boolean settingsHistoryDeleteMode = false;
    boolean settingsSuggestionVisible = true;

    int settingsHeaderTop;
    int settingsSearchTopMargin;
    int settingsSearchBarHeight;
    int settingsTitleMaxHeight;
    int settingsTitleMinHeight;
    int settingsCurrentTitleHeight;
    int settingsTitleCollapseRange;

    public void attachBaseContext(Context base) {
        Context wrapped = base;
        try {
            float hostDensity = SettingsState.settingsHostDensity;
            if (base != null && hostDensity > 0f) {
                android.content.res.Configuration cfg = new android.content.res.Configuration(base.getResources().getConfiguration());
                cfg.densityDpi = (int)(hostDensity * 160f + 0.5f);
                wrapped = base.createConfigurationContext(cfg);
            }
        } catch (Throwable exception) {
            wrapped = base;
            traceLog("setwindow_log", "[SettingsActivity.attachBaseContext] 异常: " + exception);
        }
        super.attachBaseContext(wrapped);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            boolean isDark = isThemeDark(this);
            setSettingsImmersiveStatusBar(this, getWindow(), !isDark);
            settingsPageHandler = new Handler(Looper.getMainLooper());
            SettingsState.settingsCurrentActivityRef = this;
            try {
                settingsChatType = getIntent().getIntExtra("chatType", 0);
                String pu = getIntent().getStringExtra("peerUin");
                String pn = getIntent().getStringExtra("peerName");
                settingsPeerUin = (pu != null) ? pu : "";
                settingsPeerName = (pn != null) ? pn : "";
            } catch (Throwable ignore) {}
            try {
                traceLog("setwindow_log", "[SettingsActivity] density=" + getResources().getDisplayMetrics().density + " hostDensity=" + SettingsState.settingsHostDensity);
            } catch (Throwable ignore) {}

            settingsActivityRoot = new FrameLayout(this);
            applySettingsRootBg(this, settingsActivityRoot);
            settingsActivityRoot.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            settingsActivityRoot.setPadding(0, getStatusBarHeightValue(this), 0, getNavigationBarHeightValue(this));

            settingsPageContainer = new FrameLayout(this);
            settingsPageContainer.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            settingsActivityRoot.addView(settingsPageContainer);

            setContentView(settingsActivityRoot);

            settingsPageStack = new ArrayList();
            settingsShowHome();

            final String pendingKey = SettingsState.settingsPendingHighlightKey;
            if (pendingKey != null && !pendingKey.isEmpty()) {
                SettingsState.settingsPendingHighlightKey = null;
                SettingsState.settingsPendingHighlightDepth = -1;
                settingsPageHandler.postDelayed(new Runnable() {
                    public void run() { scrollToAndHighlight(pendingKey); }
                }, 400);
            }
        } catch (Throwable exception) {
            traceLog("setwindow_log", "[SettingsActivity.onCreate] 异常: " + exception);
            Toast("设置页初始化失败: " + exception.getMessage());
        }
    }

    public void onBackPressed() {
        try {
            if (settingsSearchMode) {
                settingsExitSearchMode();
                return;
            }
            if (settingsPageStack != null && settingsPageStack.size() > 1) {
                settingsPopPage();
                return;
            }
        } catch (Throwable exception) {
            traceLog("setwindow_log", "[SettingsActivity.onBackPressed] 异常: " + exception);
        }
        super.onBackPressed();
    }

    protected void onDestroy() {
        try {
            settingsCleanup();
        } catch (Throwable exception) {
            traceLog("setwindow_log", "[SettingsActivity.onDestroy] 异常: " + exception);
        }
        super.onDestroy();
    }

    void settingsClose() {
        try { finish(); } catch (Throwable ignore) {}
    }


    void settingsShowHome() {
        if (settingsPageContainer == null) return;
        View home = settingsBuildHomePage();
        SettingsPage page = new SettingsPage();
        page.level1 = null;
        page.level2 = null;
        page.level3 = null;
        page.pageView = home;
        page.listContainer = settingsCategoryList;
        page.scrollView = settingsCategoryScroll;
        page.categoryContainers = SettingsState.settingsCategoryContainers;
        page.itemViews = SettingsState.settingsItemViews;
        page.itemTextViews = SettingsState.settingsItemTextViews;
        settingsPageStack.clear();
        settingsPageStack.add(page);
        settingsPageContainer.removeAllViews();
        settingsPageContainer.addView(home);
    }

    void settingsPushPage(String level1, String level2, String level3) {
        if (settingsPageContainer == null) return;
        if (level1 == null) {
            settingsShowHome();
            return;
        }
        SettingsPage page = settingsBuildMenuPage(level1, level2, level3);
        View oldTop = settingsTopPageView();
        settingsPageStack.add(page);
        settingsPageContainer.addView(page.pageView);
        settingsAnimateIn(page.pageView, oldTop);
    }

    void settingsPushPageNoAnim(String level1, String level2, String level3) {
        if (settingsPageContainer == null) return;
        if (level1 == null) {
            settingsShowHome();
            return;
        }
        SettingsPage page = settingsBuildMenuPage(level1, level2, level3);
        settingsPageStack.add(page);
        settingsPageContainer.addView(page.pageView);
    }

    boolean settingsPopPage() {
        if (settingsPageStack == null || settingsPageStack.size() <= 1) return false;
        SettingsPage top = (SettingsPage) settingsPageStack.remove(settingsPageStack.size() - 1);
        SettingsPage below = (SettingsPage) settingsPageStack.get(settingsPageStack.size() - 1);
        settingsActivatePage(below);
        settingsAnimateOut(top.pageView, below.pageView);
        settingsRefreshWelcomeText(below);
        try { unbindStatsViewCache(); } catch (Throwable ignore) {}
        return true;
    }

    void settingsRefreshWelcomeText(SettingsPage page) {
        if (page == null || page.level1 != null) return;
        if (SettingsState.settingsWelcomeView == null) return;
        try {
            String wt = getString("settings", "welcome_text", "欢迎使用");
            if (wt == null || wt.trim().isEmpty()) wt = "欢迎使用";
            SettingsState.settingsWelcomeView.setText(wt);
        } catch (Throwable ignore) {}
    }

    View settingsTopPageView() {
        if (settingsPageStack == null || settingsPageStack.isEmpty()) return null;
        return ((SettingsPage) settingsPageStack.get(settingsPageStack.size() - 1)).pageView;
    }

    void settingsActivatePage(SettingsPage page) {
        if (page == null) return;
        SettingsState.settingsListContainer = page.listContainer;
        SettingsState.settingsScrollView = page.scrollView;
        SettingsState.settingsCategoryContainers = page.categoryContainers;
        SettingsState.settingsItemViews = page.itemViews;
        SettingsState.settingsItemTextViews = page.itemTextViews;
        SettingsState.settingsCurrentLevel1 = page.level1;
        SettingsState.settingsCurrentLevel2 = page.level2;
        SettingsState.settingsCurrentLevel3 = page.level3;
    }

    void settingsResetToPath(String level1, String level2, String level3) {
        settingsShowHome();
        if (level1 != null) {
            settingsPushPageNoAnim(level1, null, null);
            if (level2 != null) {
                settingsPushPageNoAnim(level1, level2, null);
                if (level3 != null) {
                    settingsPushPageNoAnim(level1, level2, level3);
                }
            }
        }
        final String key = SettingsState.settingsPendingHighlightKey;
        if (key != null && !key.isEmpty()) {
            SettingsState.settingsPendingHighlightKey = null;
            SettingsState.settingsPendingHighlightDepth = -1;
            if (settingsPageHandler != null) {
                settingsPageHandler.postDelayed(new Runnable() {
                    public void run() { scrollToAndHighlight(key); }
                }, 350);
            }
        }
    }

    void settingsRefreshBackgrounds() {
        try {
            applySettingsRootBg(this, settingsActivityRoot);
            if (settingsHomeFooter != null) settingsHomeFooter.setBackgroundColor(getSettingsFooterBg(this));
            if (settingsPageStack != null) {
                for (int i = 0; i < settingsPageStack.size(); i++) {
                    SettingsPage p = (SettingsPage) settingsPageStack.get(i);
                    if (p != null && p.pageView != null) applySettingsRootBg(this, p.pageView);
                }
            }
        } catch (Throwable e) {
            traceLog("setwindow_log", "[settingsRefreshBackgrounds] 异常: " + e);
        }
    }

void settingsRunNav() {
    String l1 = SettingsState.settingsNavLevel1;
    String l2 = SettingsState.settingsNavLevel2;
    String l3 = SettingsState.settingsNavLevel3;
    String hl = SettingsState.settingsNavHighlightKey;
    String q = SettingsState.settingsNavQuery;
    SettingsState.settingsNavLevel1 = null;
    SettingsState.settingsNavLevel2 = null;
    SettingsState.settingsNavLevel3 = null;
    SettingsState.settingsNavHighlightKey = null;
    SettingsState.settingsNavQuery = null;
    boolean fromSearch = settingsSearchMode || (q != null && q.trim().length() >= 2);
    if (q != null && q.trim().length() >= 2) {
        try {
            addSearchHistory(q);
        } catch (Throwable e) {
            traceLog("setwindow_log", "[settingsRunNav] 记录历史失败: " + e);
        }
    }
    if (settingsSearchMode) {
        try {
            settingsExitSearchModeInstant();
        } catch (Throwable e) {
            traceLog("setwindow_log", "[settingsRunNav] 退出搜索失败: " + e);
        }
    }
    SettingsState.settingsPendingHighlightKey = hl;
    try {
        if (l1 == null && l2 == null && l3 == null) {
            settingsShowHome();
            return;
        }
        if (fromSearch) {
            settingsResetToPath(l1, l2, l3);
            return;
        }
        if (settingsPageStack != null && !settingsPageStack.isEmpty()) {
            SettingsPage top = (SettingsPage) settingsPageStack.get(settingsPageStack.size() - 1);
            if (top != null) {
                boolean sameLevel = (top.level1 == null ? l1 == null : top.level1.equals(l1))
                                 && (top.level2 == null ? l2 == null : top.level2.equals(l2))
                                 && (top.level3 == null ? l3 == null : top.level3.equals(l3));
                if (sameLevel) {
                    traceLog("setwindow_log", "[settingsRunNav] 目标页已在栈顶，忽略重复跳转");
                    return;
                }
            }
        }
        settingsPushPage(l1, l2, l3);
    } catch (Throwable e) {
        traceLog("setwindow_log", "[settingsRunNav] 跳转失败: " + e);
        try {
            if (l1 != null) settingsPushPageNoAnim(l1, l2, l3);
        } catch (Throwable ignore2) {
            traceLog("setwindow_log", "[settingsRunNav] 兜底跳转失败: " + ignore2);
        }
    }
}


    SettingsPage settingsBuildMenuPage(String level1, String level2, String level3) {
        SettingsPage page = new SettingsPage();
        page.level1 = level1;
        page.level2 = level2;
        page.level3 = level3;

        LinearLayout pageLayout = new LinearLayout(this);
        pageLayout.setOrientation(LinearLayout.VERTICAL);
        pageLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        applySettingsRootBg(this, pageLayout);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(this, 8), dp(this, 10), dp(this, 16), dp(this, 10));

        FrameLayout backWrap = new FrameLayout(this);
        backWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 43), dp(this, 43)));
        TextView backBtn = createButton(this, "‹", pc(getSettingsThemeColor(this, "on_surface")), Color.TRANSPARENT, 28f, 0, 0, 0, false, 0, 0, null);
        FrameLayout.LayoutParams backBtnParams = new FrameLayout.LayoutParams(-2, -2);
        backBtnParams.gravity = Gravity.CENTER;
        backBtn.setLayoutParams(backBtnParams);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                vibrate(SettingsActivity.this, 32);
                settingsPopPage();
            }
        });
        backWrap.addView(backBtn);
        header.addView(backWrap);

        String titleText = level3 != null ? level3 : (level2 != null ? level2 : level1);
        TextView titleView = new TextView(this);
        titleView.setText(titleText);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, dp(this, 40), 1.0f));
        header.addView(titleView);
        pageLayout.addView(header);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));

        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, 0, 0, dp(this, 16));
        scrollView.addView(listContainer);
        pageLayout.addView(scrollView);

        page.pageView = pageLayout;
        page.scrollView = scrollView;
        page.listContainer = listContainer;
        page.categoryContainers = new HashMap();
        page.itemViews = new HashMap();
        page.itemTextViews = new HashMap();

        SettingsState.settingsListContainer = listContainer;
        SettingsState.settingsScrollView = scrollView;
        SettingsState.settingsCategoryContainers = page.categoryContainers;
        SettingsState.settingsItemViews = page.itemViews;
        SettingsState.settingsItemTextViews = page.itemTextViews;
        SettingsState.settingsCurrentActivityRef = this;
        SettingsState.settingsCurrentLevel1 = level1;
        SettingsState.settingsCurrentLevel2 = level2;
        SettingsState.settingsCurrentLevel3 = level3;
        SettingsState.settingsCurrentCategory = null;

        if (level1 == null) {
            rebuildSettingsIndex(this);
        }
        buildSettingsMenuContent(this, level1, level2, level3);
        return page;
    }

    View settingsBuildHomePage() {
        FrameLayout pageLayout = new FrameLayout(this);
        pageLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        applySettingsRootBg(this, pageLayout);

        settingsHeaderTop = dp(this, 14);
        settingsSearchTopMargin = dp(this, 12);
        settingsSearchBarHeight = dp(this, 46);
        settingsTitleMaxHeight = dp(this, 40);
        settingsTitleMinHeight = dp(this, 26);
        settingsCurrentTitleHeight = settingsTitleMaxHeight;
        settingsTitleCollapseRange = dp(this, 60);
        int headerTop = settingsHeaderTop;
        int titleHeight = settingsCurrentTitleHeight;
        int searchTopMargin = settingsSearchTopMargin;
        int searchBarHeight = settingsSearchBarHeight;
        int sideMargin = dp(this, 20);

        settingsCategoryScroll = new ScrollView(this);
        settingsCategoryScroll.setVerticalScrollBarEnabled(false);
        FrameLayout.LayoutParams categoryParams = new FrameLayout.LayoutParams(-1, -1);
        categoryParams.topMargin = headerTop + titleHeight + searchTopMargin + searchBarHeight;
        settingsCategoryScroll.setLayoutParams(categoryParams);
        settingsCategoryList = new LinearLayout(this);
        settingsCategoryList.setOrientation(LinearLayout.VERTICAL);
        settingsCategoryList.setPadding(0, 0, 0, dp(this, 16));
        settingsCategoryScroll.addView(settingsCategoryList);
        pageLayout.addView(settingsCategoryScroll);

        settingsResultScroll = new ScrollView(this);
        settingsResultScroll.setVerticalScrollBarEnabled(false);
        FrameLayout.LayoutParams resultParams = new FrameLayout.LayoutParams(-1, -1);
        resultParams.topMargin = headerTop + searchBarHeight;
        settingsResultScroll.setLayoutParams(resultParams);
        settingsResultScroll.setVisibility(View.GONE);
        settingsResultWrapper = new LinearLayout(this);
        settingsResultWrapper.setOrientation(LinearLayout.VERTICAL);
        settingsResultWrapper.setPadding(dp(this, 12), dp(this, 6), dp(this, 12), dp(this, 16));
        settingsHistoryContainer = new LinearLayout(this);
        settingsHistoryContainer.setOrientation(LinearLayout.VERTICAL);
        settingsResultWrapper.addView(settingsHistoryContainer);
        settingsResultList = new LinearLayout(this);
        settingsResultList.setOrientation(LinearLayout.VERTICAL);
        settingsResultList.setVisibility(View.GONE);
        settingsResultWrapper.addView(settingsResultList);
        settingsResultScroll.addView(settingsResultWrapper);
        pageLayout.addView(settingsResultScroll);

        settingsHomeTitle = new TextView(this);
        settingsHomeTitle.setText("设置");
        settingsHomeTitle.setTextSize(28);
        settingsHomeTitle.setTypeface(null, Typeface.BOLD);
        settingsHomeTitle.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
        settingsHomeTitle.setIncludeFontPadding(false);
        settingsHomeTitle.setGravity(Gravity.BOTTOM | Gravity.START);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-1, titleHeight);
        titleParams.topMargin = headerTop;
        titleParams.leftMargin = sideMargin;
        titleParams.rightMargin = sideMargin;
        settingsHomeTitle.setLayoutParams(titleParams);
        pageLayout.addView(settingsHomeTitle);

        settingsSearchRow = new LinearLayout(this);
        settingsSearchRow.setOrientation(LinearLayout.HORIZONTAL);
        settingsSearchRow.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams searchRowParams = new FrameLayout.LayoutParams(-1, searchBarHeight);
        searchRowParams.topMargin = headerTop + titleHeight + searchTopMargin;
        searchRowParams.leftMargin = sideMargin;
        searchRowParams.rightMargin = sideMargin;
        settingsSearchRow.setLayoutParams(searchRowParams);

        settingsSearchBar = new LinearLayout(this);
        settingsSearchBar.setOrientation(LinearLayout.HORIZONTAL);
        settingsSearchBar.setGravity(Gravity.CENTER_VERTICAL);
        settingsSearchBar.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
        settingsSearchBar.setBackground(roundRect(getAdaptiveInputBg(this), dp(this, 22)));
        settingsSearchBar.setPadding(dp(this, 14), 0, dp(this, 6), 0);

        settingsSearchInput = makeInput(this, "搜索设置项...", null);
        settingsSearchInput.setTextSize(14);
        settingsSearchInput.setSingleLine(true);
        settingsSearchInput.setBackground(null);
        settingsSearchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        settingsSearchInput.setLayoutParams(new LinearLayout.LayoutParams(0, dp(this, 44), 1.0f));
        settingsSearchInput.setFocusable(false);
        settingsSearchInput.setFocusableInTouchMode(false);
        settingsSearchBar.addView(settingsSearchInput);
        settingsSearchRow.addView(settingsSearchBar);

        settingsSearchCancel = new TextView(this);
        settingsSearchCancel.setText("取消");
        settingsSearchCancel.setTextSize(14);
        settingsSearchCancel.setTextColor(pc(getSettingsThemeColor(this, "primary")));
        settingsSearchCancel.setPadding(dp(this, 12), dp(this, 10), 0, dp(this, 10));
        settingsSearchCancel.setVisibility(View.GONE);
        settingsSearchRow.addView(settingsSearchCancel);
        pageLayout.addView(settingsSearchRow);

        settingsHomeFooter = new LinearLayout(this);
        settingsHomeFooter.setOrientation(LinearLayout.VERTICAL);
        settingsHomeFooter.setVisibility(View.GONE);
        settingsHomeFooter.setBackgroundColor(getSettingsFooterBg(this));
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(-1, -2);
        footerParams.gravity = Gravity.BOTTOM;
        settingsHomeFooter.setLayoutParams(footerParams);
        pageLayout.addView(settingsHomeFooter);

        SettingsState.settingsListContainer = settingsCategoryList;
        SettingsState.settingsScrollView = settingsCategoryScroll;
        SettingsState.settingsCategoryContainers = new HashMap();
        SettingsState.settingsItemViews = new HashMap();
        SettingsState.settingsItemTextViews = new HashMap();
        SettingsState.settingsCurrentActivityRef = this;
        SettingsState.settingsCurrentLevel1 = null;
        SettingsState.settingsCurrentLevel2 = null;
        SettingsState.settingsCurrentLevel3 = null;
        SettingsState.settingsCurrentCategory = null;

        rebuildSettingsIndex(this);
        buildSettingsHomeContent(this);
        buildSettingsBottomArea(this);

        settingsBindSearchInput();
        settingsBindHomeHeaderCollapse();
        return pageLayout;
    }

    void settingsMountFooter(final View bottomArea) {
        if (bottomArea == null) return;
        if (settingsHomeFooter == null) {
            if (SettingsState.settingsListContainer != null) SettingsState.settingsListContainer.addView(bottomArea);
            return;
        }
        settingsHomeFooter.removeAllViews();
        settingsHomeFooter.setVisibility(settingsSearchMode ? View.GONE : View.VISIBLE);
        settingsHomeFooter.addView(bottomArea);
        bottomArea.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                try {
                    int h = bottomArea.getHeight();
                    if (h <= 0 || settingsCategoryList == null) return;
                    if (settingsCategoryScroll != null) {
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) settingsCategoryScroll.getLayoutParams();
                        if (params != null && params.bottomMargin != h) {
                            params.bottomMargin = h;
                            settingsCategoryScroll.setLayoutParams(params);
                        }
                    }
                    int need = h + dp(SettingsActivity.this, 16);
                    if (settingsCategoryList.getPaddingBottom() != need) {
                        settingsCategoryList.setPadding(0, 0, 0, need);
                    }
                } catch (Throwable ignore) {}
            }
        });
    }


    void settingsBindHomeHeaderCollapse() {
        if (settingsCategoryScroll == null || settingsHomeTitle == null) return;
        settingsCategoryScroll.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            public void onScrollChanged() {
                if (settingsSearchMode) return;
                settingsApplyHomeHeaderProgress(settingsHomeCollapseProgress());
            }
        });
    }

    float settingsHomeCollapseProgress() {
        if (settingsCategoryScroll == null || settingsTitleCollapseRange <= 0) return 0f;
        int scrollY = settingsCategoryScroll.getScrollY();
        float progress = (float) scrollY / (float) settingsTitleCollapseRange;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;
        return progress;
    }

    void settingsApplyHomeHeaderProgress(float progress) {
        if (settingsHomeTitle == null || settingsSearchRow == null || settingsCategoryScroll == null) return;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;

        settingsHomeTitle.setTextSize(28f - (28f - 18f) * progress);

        int titleH = settingsTitleMaxHeight - (int)((settingsTitleMaxHeight - settingsTitleMinHeight) * progress);
        if (titleH == settingsCurrentTitleHeight) return;
        settingsCurrentTitleHeight = titleH;

        try {
            FrameLayout.LayoutParams titleParams = (FrameLayout.LayoutParams) settingsHomeTitle.getLayoutParams();
            if (titleParams != null) {
                titleParams.height = titleH;
                settingsHomeTitle.setLayoutParams(titleParams);
            }
            FrameLayout.LayoutParams searchParams = (FrameLayout.LayoutParams) settingsSearchRow.getLayoutParams();
            if (searchParams != null) {
                searchParams.topMargin = settingsHeaderTop + titleH + settingsSearchTopMargin;
                settingsSearchRow.setLayoutParams(searchParams);
            }
            FrameLayout.LayoutParams categoryParams = (FrameLayout.LayoutParams) settingsCategoryScroll.getLayoutParams();
            if (categoryParams != null) {
                categoryParams.topMargin = settingsHeaderTop + titleH + settingsSearchTopMargin + settingsSearchBarHeight;
                settingsCategoryScroll.setLayoutParams(categoryParams);
            }
        } catch (Throwable exception) {
            traceLog("setwindow_log", "[settingsApplyHomeHeaderProgress] 异常: " + exception);
        }
    }


    void settingsBindSearchInput() {
        if (settingsSearchInput == null) return;

        View.OnClickListener enterListener = new View.OnClickListener() {
            public void onClick(View view) {
                settingsEnterSearchMode();
            }
        };
        settingsSearchRow.setOnClickListener(enterListener);
        settingsSearchBar.setOnClickListener(enterListener);
        settingsSearchInput.setOnClickListener(enterListener);

        settingsSearchCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                vibrate(SettingsActivity.this, 32);
                settingsExitSearchMode();
            }
        });

        settingsSearchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable editable) {
                final String queryValue = editable != null ? editable.toString().trim() : "";
                final long token = System.currentTimeMillis();
                SettingsState.settingsSearchToken = token;
                settingsPageHandler.postDelayed(new Runnable() {
                    public void run() {
                        if (token != SettingsState.settingsSearchToken) return;
                        doSearch(queryValue, settingsHistoryContainer, settingsResultList, SettingsActivity.this, isThemeDark(SettingsActivity.this));
                    }
                }, 200);
            }
        });

        settingsSearchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String text = settingsSearchInput.getText().toString().trim();
                    if (text.length() >= 2) SettingsActivity.this.addSearchHistory(text);
                    hideSearchInputKeyboard(SettingsActivity.this, settingsSearchInput);
                    return true;
                }
                return false;
            }
        });
    }

    List loadSearchHistory() {
        List list = new ArrayList();
        try {
            String raw = settingsHistoryJson;
            if (raw == null) {
                raw = SettingsState.settingsHistoryRaw;
                if (raw == null) {
                    raw = loadLocalData("search_history");
                    if (raw == null || raw.trim().length() == 0) raw = migrateSearchHistory();
                    if (raw == null) raw = "";
                    SettingsState.settingsHistoryRaw = raw;
                }
                settingsHistoryJson = raw;
            }
            if (raw.trim().length() == 0) return list;
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String k = o.optString("k", "");
                if (k == null || k.length() == 0) continue;
                SettingsHistoryEntry e = new SettingsHistoryEntry();
                e.key = k;
                e.count = o.optInt("c", 1);
                e.time = o.optLong("t", System.currentTimeMillis());
                list.add(e);
            }
        } catch (Throwable e) {
            traceLog("setwindow_log", "[loadSearchHistory] 异常: " + e);
        }
        return list;
    }

    String migrateSearchHistory() {
        try {
            String old = getString("settings", "search_history", "");
            if (old == null || old.trim().length() == 0) return "";
            JSONArray arr = new JSONArray();
            String[] parts = old.split("\\|");
            long now = System.currentTimeMillis();
            for (int i = 0; i < parts.length; i++) {
                String k = parts[i] != null ? parts[i].trim() : "";
                if (k.length() == 0) continue;
                JSONObject o = new JSONObject();
                o.put("k", k);
                o.put("c", 1);
                o.put("t", now - i * 1000L);
                arr.put(o);
            }
            String out = arr.toString();
            saveSearchHistoryJson(out);
            putString("settings", "search_history", "");
            return out;
        } catch (Throwable e) {
            traceLog("setwindow_log", "[migrateSearchHistory] 异常: " + e);
            return "";
        }
    }

    void saveSearchHistoryList(List list) {
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                SettingsHistoryEntry e = (SettingsHistoryEntry) list.get(i);
                if (e == null || e.key == null || e.key.length() == 0) continue;
                JSONObject o = new JSONObject();
                o.put("k", e.key);
                o.put("c", e.count);
                o.put("t", e.time);
                arr.put(o);
            }
            saveSearchHistoryJson(arr.toString());
        } catch (Throwable e) {
            traceLog("setwindow_log", "[saveSearchHistoryList] 异常: " + e);
        }
    }

    void saveSearchHistoryJson(final String json) {
        settingsHistoryJson = json;
        SettingsState.settingsHistoryRaw = json;
        try {
            if (settingsHistoryWriter == null) {
                settingsHistoryWriter = java.util.concurrent.Executors.newSingleThreadExecutor();
            }
            settingsHistoryWriter.execute(new Runnable() {
                public void run() {
                    try {
                        saveLocalData("search_history", json);
                        String check = loadLocalData("search_history");
                        if (check == null || !check.equals(json)) {
                            traceLog("setwindow_log", "[saveSearchHistoryJson] 校验失败，同步补写");
                            saveLocalData("search_history", json);
                        }
                    } catch (Throwable e) {
                        traceLog("setwindow_log", "[saveSearchHistoryJson] 写线程异常: " + e);
                        saveLocalData("search_history", json);
                    }
                }
            });
        } catch (Throwable e) {
            traceLog("setwindow_log", "[saveSearchHistoryJson] 提交失败，同步写入: " + e);
            saveLocalData("search_history", json);
        }
    }

    void addSearchHistory(String query) {
        try {
            if (query == null) return;
            String q = query.trim();
            if (q.length() < 2) return;
            List list = loadSearchHistory();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            SettingsHistoryEntry e = (SettingsHistoryEntry) list.get(i);
            if (q.equals(e.key)) {
                e.count = Math.min(e.count + 1, 99);
                e.time = System.currentTimeMillis();
                list.remove(i);
                list.add(0, e);
                found = true;
                break;
            }
        }
        if (!found) {
            SettingsHistoryEntry e = new SettingsHistoryEntry();
            e.key = q;
            e.count = 1;
            e.time = System.currentTimeMillis();
            list.add(0, e);
        }
        while (list.size() > 10) list.remove(list.size() - 1);
        saveSearchHistoryList(list);
        } catch (Throwable e) {
            traceLog("setwindow_log", "[addSearchHistory] 异常: " + e);
        }
    }

    void removeSearchHistory(String value) {
        if (value == null) return;
        List list = loadSearchHistory();
        for (int i = list.size() - 1; i >= 0; i--) {
            SettingsHistoryEntry e = (SettingsHistoryEntry) list.get(i);
            if (value.equals(e.key)) list.remove(i);
        }
        saveSearchHistoryList(list);
        settingsBuildHistory();
    }

    void clearAllSearchHistory() {
        saveSearchHistoryList(new ArrayList());
        settingsHistoryDeleteMode = false;
        settingsBuildHistory();
    }

    List buildSearchSuggestions() {
        List result = new ArrayList();
        try {
            List history = loadSearchHistory();
            int n = history.size();
            double[] weights = new double[n];
            long now = System.currentTimeMillis();
            for (int i = 0; i < n; i++) {
                SettingsHistoryEntry e = (SettingsHistoryEntry) history.get(i);
                double ageDays = (now - e.time) / 86400000.0;
                if (ageDays < 0) ageDays = 0;
                double decay = 1.0 / (1.0 + ageDays / 7.0);
                weights[i] = (e.count + 1.0) * (0.4 + 0.6 * decay);
            }
            int topN = 6;
            for (int pick = 0; pick < topN && pick < n; pick++) {
                int best = -1;
                for (int i = 0; i < n; i++) {
                    if (weights[i] < 0) continue;
                    if (best < 0 || weights[i] > weights[best]) best = i;
                }
                if (best < 0) break;
                weights[best] = -1;
                result.add(((SettingsHistoryEntry) history.get(best)).key);
            }

            List pool = new ArrayList();
            if (SettingsState.settingsItemMeta != null) {
                java.util.Iterator it = SettingsState.settingsItemMeta.values().iterator();
                while (it.hasNext()) {
                    SettingsItemMeta m = (SettingsItemMeta) it.next();
                    if (m == null || m.name == null) continue;
                    String nm = m.name.trim();
                    if (nm.length() < 2) continue;
                    if (result.contains(nm) || pool.contains(nm)) continue;
                    pool.add(nm);
                }
            }
            for (int i = 0; i < 3 && pool.size() > 0; i++) {
                int idx = (int) (Math.random() * pool.size());
                result.add(pool.remove(idx));
            }
        } catch (Throwable e) {
            traceLog("setwindow_log", "[buildSearchSuggestions] 异常: " + e);
        }
        if (result.size() < 9) {
            String[] fallback = {"悬浮窗", "模拟定位", "消息统计", "背景样式", "字体样式", "toast设置", "线程池", "输入框提示", "更新通道"};
            for (int i = 0; i < fallback.length && result.size() < 9; i++) {
                if (!result.contains(fallback[i])) result.add(fallback[i]);
            }
        }
        return result;
    }

    void settingsBuildHistory() {
        if (settingsHistoryContainer == null) return;
        settingsHistoryContainer.removeAllViews();

        List history = loadSearchHistory();

        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setOrientation(LinearLayout.HORIZONTAL);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        historyHeader.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        TextView historyTitle = new TextView(this);
        historyTitle.setText("搜索历史");
        historyTitle.setTextSize(14);
        historyTitle.setTypeface(null, Typeface.BOLD);
        historyTitle.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
        historyTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        historyHeader.addView(historyTitle);

        if (history.size() > 0) {
            TextView clearHistory = new TextView(this);
            clearHistory.setText(settingsHistoryDeleteMode ? "全部删除" : "删除");
            clearHistory.setTextSize(13);
            clearHistory.setTextColor(pc(getSettingsThemeColor(this, "primary")));
            clearHistory.setPadding(dp(this, 8), dp(this, 4), 0, dp(this, 4));
            clearHistory.setClickable(true);
            clearHistory.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    vibrate(SettingsActivity.this, 32);
                    if (!settingsHistoryDeleteMode) {
                        settingsHistoryDeleteMode = true;
                        settingsBuildHistory();
                        return;
                    }
                    showSettingsConfirmDialog(SettingsActivity.this, "确认删除", "确定要清空全部搜索历史吗？", new Runnable() {
                        public void run() { clearAllSearchHistory(); }
                    });
                }
            });
            historyHeader.addView(clearHistory);
        }
        settingsHistoryContainer.addView(historyHeader);

        settingsHistoryContainer.setClickable(true);
        settingsHistoryContainer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (settingsHistoryDeleteMode) {
                    settingsHistoryDeleteMode = false;
                    settingsBuildHistory();
                }
            }
        });

        if (history.size() == 0) {
            settingsHistoryDeleteMode = false;
            TextView emptyHint = new TextView(this);
            emptyHint.setText("暂无历史搜索");
            emptyHint.setTextSize(13);
            emptyHint.setTextColor(pc(getSettingsThemeColor(this, "on_surface_variant")));
            emptyHint.setPadding(0, dp(this, 10), 0, dp(this, 10));
            settingsHistoryContainer.addView(emptyHint);
        } else {
            List keys = new ArrayList();
            for (int i = 0; i < history.size(); i++) keys.add(((SettingsHistoryEntry) history.get(i)).key);
            settingsAddChipRows(settingsHistoryContainer, keys, true);
        }

        LinearLayout suggestHeader = new LinearLayout(this);
        suggestHeader.setOrientation(LinearLayout.HORIZONTAL);
        suggestHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams suggestHeaderLp = new LinearLayout.LayoutParams(-1, -2);
        suggestHeaderLp.topMargin = dp(this, 20);
        suggestHeader.setLayoutParams(suggestHeaderLp);

        TextView suggestTitle = new TextView(this);
        suggestTitle.setText("搜索建议");
        suggestTitle.setTextSize(14);
        suggestTitle.setTypeface(null, Typeface.BOLD);
        suggestTitle.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
        suggestTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        suggestHeader.addView(suggestTitle);

        TextView suggestToggle = new TextView(this);
        suggestToggle.setText(settingsSuggestionVisible ? "隐藏" : "显示");
        suggestToggle.setTextSize(13);
        suggestToggle.setTextColor(pc(getSettingsThemeColor(this, "primary")));
        suggestToggle.setPadding(dp(this, 8), dp(this, 4), 0, dp(this, 4));
        suggestToggle.setClickable(true);
        suggestToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                vibrate(SettingsActivity.this, 32);
                settingsSuggestionVisible = !settingsSuggestionVisible;
                settingsBuildHistory();
            }
        });
        suggestHeader.addView(suggestToggle);
        settingsHistoryContainer.addView(suggestHeader);

        if (settingsSuggestionVisible) {
            LinearLayout suggestContainer = new LinearLayout(this);
            suggestContainer.setOrientation(LinearLayout.VERTICAL);
            settingsAddChipRows(suggestContainer, buildSearchSuggestions(), false);
            settingsHistoryContainer.addView(suggestContainer);
        }
    }

    void settingsAddChipRows(LinearLayout container, List items, final boolean deletable) {
        if (container == null || items == null || items.isEmpty()) return;
        int perRow = 3;
        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.setMargins(0, dp(this, 10), 0, 0);
                row.setLayoutParams(rowParams);
                container.addView(row);
            }
            final String chipText = (String) items.get(i);

            FrameLayout wrap = new FrameLayout(this);
            LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(-2, -2);
            wrapParams.setMargins(0, 0, dp(this, 8), 0);
            wrap.setLayoutParams(wrapParams);

            final TextView chip = new TextView(this);
            chip.setText(chipText);
            chip.setTextSize(13);
            chip.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
            chip.setPadding(dp(this, 14), dp(this, 8), deletable ? dp(this, 24) : dp(this, 14), dp(this, 8));
            chip.setBackground(makeFeedbackBg(getAdaptiveCardBg(this), pc(getSettingsThemeColor(this, "ripple")), dp(this, 16)));
            chip.setClipToOutline(true);
            chip.setLayoutParams(new FrameLayout.LayoutParams(-2, -2));
            wrap.addView(chip);

            if (deletable) {
                final TextView del = new TextView(this);
                del.setText("×");
                del.setTextSize(13);
                del.setTextColor(pc(getSettingsThemeColor(this, "error")));
                del.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams delLp = new FrameLayout.LayoutParams(dp(this, 20), dp(this, 20));
                delLp.gravity = Gravity.TOP | Gravity.END;
                del.setLayoutParams(delLp);
                del.setVisibility(settingsHistoryDeleteMode ? View.VISIBLE : View.GONE);
                del.setClickable(true);
                del.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        vibrate(SettingsActivity.this, 32);
                        removeSearchHistory(chipText);
                    }
                });
                wrap.addView(del);
            }

            chip.setClickable(true);
            if (deletable) {
                final GestureDetector chipGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                    public boolean onDown(MotionEvent event) { return true; }
                    public boolean onSingleTapUp(MotionEvent event) {
                        if (!settingsHistoryDeleteMode) runSearchChip(chipText);
                        return true;
                    }
                    public void onLongPress(MotionEvent event) {
                        if (settingsHistoryDeleteMode) return;
                        vibrate(SettingsActivity.this, 32);
                        settingsHistoryDeleteMode = true;
                        if (settingsPageHandler != null) {
                            settingsPageHandler.post(new Runnable() {
                                public void run() { settingsBuildHistory(); }
                            });
                        } else {
                            settingsBuildHistory();
                        }
                    }
                });
                chip.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View view, MotionEvent event) {
                        chipGesture.onTouchEvent(event);
                        return true;
                    }
                });
            } else {
                chip.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        vibrate(SettingsActivity.this, 32);
                        if (settingsSearchInput != null) {
                            settingsSearchInput.setText(chipText);
                            settingsSearchInput.setSelection(chipText.length());
                        }
                    }
                });
            }

            if (row != null) row.addView(wrap);
        }
    }

    void runSearchChip(String chipText) {
        if (chipText == null) return;
        vibrate(this, 32);
        if (settingsSearchInput != null) {
            settingsSearchInput.setText(chipText);
            settingsSearchInput.setSelection(chipText.length());
        }
        doSearch(chipText, settingsHistoryContainer, settingsResultList, this, isThemeDark(this));
    }

    void settingsEnterSearchMode() {
        if (settingsSearchMode || settingsAnimating) return;
        if (settingsSearchInput == null) return;
        settingsSearchMode = true;
        settingsAnimating = true;
        settingsHistoryDeleteMode = false;

        settingsBuildHistory();
        settingsResultList.setVisibility(View.GONE);
        settingsHistoryContainer.setVisibility(View.VISIBLE);
        settingsResultScroll.setVisibility(View.VISIBLE);
        settingsResultScroll.setAlpha(0f);
        settingsSearchCancel.setVisibility(View.VISIBLE);
        if (settingsHomeFooter != null) settingsHomeFooter.setVisibility(View.GONE);

        settingsSearchInput.setFocusable(true);
        settingsSearchInput.setFocusableInTouchMode(true);
        settingsSearchInput.setCursorVisible(true);

        final int shift = settingsCurrentTitleHeight + settingsSearchTopMargin;

        settingsHomeTitle.animate().alpha(0f).setDuration(180).start();
        settingsSearchRow.animate().translationY(-shift).setDuration(220).start();
        settingsCategoryScroll.animate().alpha(0f).setDuration(150).withEndAction(new Runnable() {
            public void run() {
                settingsCategoryScroll.setVisibility(View.GONE);
                settingsResultScroll.animate().alpha(1f).setDuration(150).withEndAction(new Runnable() {
                    public void run() {
                        settingsAnimating = false;
                        if (settingsSearchInput != null) {
                            settingsSearchInput.requestFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.showSoftInput(settingsSearchInput, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                }).start();
            }
        }).start();
    }

    void settingsExitSearchMode() {
        if (!settingsSearchMode) return;
        if (settingsAnimating) return;
        settingsAnimating = true;
        settingsSearchMode = false;

        if (settingsSearchInput != null) {
            settingsSearchInput.setText("");
            hideSearchInputKeyboard(this, settingsSearchInput);
        }
        settingsSearchCancel.setVisibility(View.GONE);
        settingsSearchInput.setFocusable(false);
        settingsSearchInput.setFocusableInTouchMode(false);
        settingsSearchInput.setCursorVisible(false);

        settingsHomeTitle.animate().alpha(1f).setDuration(180).start();
        settingsSearchRow.animate().translationY(0f).setDuration(220).start();
        settingsResultScroll.animate().alpha(0f).setDuration(150).withEndAction(new Runnable() {
            public void run() {
                settingsResultScroll.setVisibility(View.GONE);
                settingsCategoryScroll.setVisibility(View.VISIBLE);
                if (settingsHomeFooter != null) settingsHomeFooter.setVisibility(View.VISIBLE);
                settingsApplyHomeHeaderProgress(settingsHomeCollapseProgress());
                settingsCategoryScroll.animate().alpha(1f).setDuration(150).withEndAction(new Runnable() {
                    public void run() {
                        settingsAnimating = false;
                    }
                }).start();
            }
        }).start();
    }

    void settingsExitSearchModeInstant() {
        settingsSearchMode = false;
        settingsAnimating = false;
        if (settingsSearchInput != null) {
            settingsSearchInput.setText("");
            hideSearchInputKeyboard(this, settingsSearchInput);
            settingsSearchInput.setFocusable(false);
            settingsSearchInput.setFocusableInTouchMode(false);
            settingsSearchInput.setCursorVisible(false);
        }
        if (settingsSearchCancel != null) settingsSearchCancel.setVisibility(View.GONE);
        if (settingsHomeTitle != null) settingsHomeTitle.setAlpha(1f);
        if (settingsSearchRow != null) settingsSearchRow.setTranslationY(0f);
        if (settingsResultScroll != null) {
            settingsResultScroll.setAlpha(0f);
            settingsResultScroll.setVisibility(View.GONE);
        }
        if (settingsCategoryScroll != null) {
            settingsCategoryScroll.setAlpha(1f);
            settingsCategoryScroll.setVisibility(View.VISIBLE);
            if (settingsHomeFooter != null) settingsHomeFooter.setVisibility(View.VISIBLE);
            settingsApplyHomeHeaderProgress(settingsHomeCollapseProgress());
        }
    }


    void settingsPushContentView(String title, View content) {
        if (content == null || settingsPageContainer == null) return;

        LinearLayout pageLayout = new LinearLayout(this);
        pageLayout.setOrientation(LinearLayout.VERTICAL);
        pageLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        applySettingsRootBg(this, pageLayout);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(this, 8), dp(this, 10), dp(this, 16), dp(this, 10));

        FrameLayout backWrap = new FrameLayout(this);
        backWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 43), dp(this, 43)));
        TextView backBtn = createButton(this, "‹", pc(getSettingsThemeColor(this, "on_surface")), Color.TRANSPARENT, 28f, 0, 0, 0, false, 0, 0, null);
        FrameLayout.LayoutParams backBtnParams = new FrameLayout.LayoutParams(-2, -2);
        backBtnParams.gravity = Gravity.CENTER;
        backBtn.setLayoutParams(backBtnParams);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                vibrate(SettingsActivity.this, 32);
                settingsPopPage();
            }
        });
        backWrap.addView(backBtn);
        header.addView(backWrap);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, dp(this, 40), 1.0f));
        header.addView(titleView);
        pageLayout.addView(header);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        scrollView.addView(content);
        pageLayout.addView(scrollView);

        SettingsPage page = new SettingsPage();
        page.level1 = title;
        page.level2 = null;
        page.level3 = null;
        page.pageView = pageLayout;
        page.listContainer = null;
        page.scrollView = scrollView;
        page.categoryContainers = SettingsState.settingsCategoryContainers;
        page.itemViews = SettingsState.settingsItemViews;
        page.itemTextViews = SettingsState.settingsItemTextViews;

        View oldTop = settingsTopPageView();
        settingsPageStack.add(page);
        settingsPageContainer.addView(pageLayout);
        settingsAnimateIn(pageLayout, oldTop);    }

    void settingsPushContentViewRaw(String title, View content) {
        if (content == null || settingsPageContainer == null) return;

        LinearLayout pageLayout = new LinearLayout(this);
        pageLayout.setOrientation(LinearLayout.VERTICAL);
        pageLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        applySettingsRootBg(this, pageLayout);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(this, 8), dp(this, 10), dp(this, 16), dp(this, 10));

        FrameLayout backWrap = new FrameLayout(this);
        backWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(this, 43), dp(this, 43)));
        TextView backBtn = createButton(this, "‹", pc(getSettingsThemeColor(this, "on_surface")), Color.TRANSPARENT, 28f, 0, 0, 0, false, 0, 0, null);
        FrameLayout.LayoutParams backBtnParams = new FrameLayout.LayoutParams(-2, -2);
        backBtnParams.gravity = Gravity.CENTER;
        backBtn.setLayoutParams(backBtnParams);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                vibrate(SettingsActivity.this, 32);
                settingsPopPage();
            }
        });
        backWrap.addView(backBtn);
        header.addView(backWrap);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(pc(getSettingsThemeColor(this, "on_surface")));
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(0, dp(this, 40), 1.0f));
        header.addView(titleView);
        pageLayout.addView(header);

        content.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        pageLayout.addView(content);

        SettingsPage page = new SettingsPage();
        page.level1 = title;
        page.level2 = null;
        page.level3 = null;
        page.pageView = pageLayout;
        page.listContainer = null;
        page.scrollView = null;
        page.categoryContainers = SettingsState.settingsCategoryContainers;
        page.itemViews = SettingsState.settingsItemViews;
        page.itemTextViews = SettingsState.settingsItemTextViews;

        View oldTop = settingsTopPageView();
        settingsPageStack.add(page);
        settingsPageContainer.addView(pageLayout);
        settingsAnimateIn(pageLayout, oldTop);
    }


    int settingsContainerWidth() {
        int width = settingsPageContainer != null ? settingsPageContainer.getWidth() : 0;
        if (width <= 0) width = getResources().getDisplayMetrics().widthPixels;
        return width;
    }

    void settingsAnimateIn(final View newPage, final View oldPage) {
        if (newPage == null) return;
        final int width = settingsContainerWidth();
        newPage.setVisibility(View.VISIBLE);
        newPage.setTranslationX(width);
        if (oldPage != null && oldPage != newPage) {
            oldPage.animate().translationX(-width * 0.28f).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        }
        newPage.post(new Runnable() {
            public void run() {
                newPage.setTranslationX(width);
                newPage.animate().translationX(0f).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
            }
        });
    }

    void settingsAnimateOut(final View topPage, final View belowPage) {
        if (topPage == null) return;
        final int width = settingsContainerWidth();
        if (belowPage != null) {
            belowPage.setVisibility(View.VISIBLE);
            belowPage.setTranslationX(-width * 0.28f);
            belowPage.animate().translationX(0f).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        }
        topPage.animate().translationX(width).setDuration(260).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
            public void run() {
                if (settingsPageContainer != null) settingsPageContainer.removeView(topPage);
            }
        }).start();
    }


    void settingsCleanup() {
        try {
            if (settingsPageHandler != null) {
                settingsPageHandler.removeCallbacksAndMessages(null);
            }
            if (settingsHistoryWriter != null) settingsHistoryWriter.shutdown();
            resetSettingsSearchState();
            try { unbindStatsViewCache(); } catch (Throwable ignore) {}
            SettingsState.settingsPendingHighlightKey = null;
            SettingsState.settingsPendingHighlightDepth = -1;
            if (SettingsState.settingsCurrentActivityRef == this) {
                SettingsState.settingsCurrentActivityRef = null;
            }
            if (settingsPageStack != null) settingsPageStack.clear();
            if (settingsPageContainer != null) settingsPageContainer.removeAllViews();
            SettingsState.settingsHostDensity = 0f;
            releaseSettingsIconCache();
            cleanupSettingsResources();
        } catch (Throwable exception) {
            traceLog("setwindow_log", "[SettingsActivity.settingsCleanup] 异常: " + exception);
        }
    }
}

void resetSettingsSearchState() {
    lastSearchQuery = "";
    SettingsState.settingsSearchToken = 0;
}

void releaseSettingsIconCache() {
    try {
        if (cachedBottomIconGithub != null && !cachedBottomIconGithub.isRecycled()) cachedBottomIconGithub.recycle();
    } catch (Throwable ignore) {}
    cachedBottomIconGithub = null;
    try {
        if (cachedBottomIconQq != null && !cachedBottomIconQq.isRecycled()) cachedBottomIconQq.recycle();
    } catch (Throwable ignore) {}
    cachedBottomIconQq = null;
}

try {
    registerActivity(SettingsActivity.class);
    SettingsState.settingsActivityRegistered = true;
} catch (Throwable e) {
    traceLog("setwindow_log", "[register] 注册设置Activity异常: " + e);
}
