/**
 * 设置界面
 */

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
}

/**
 * 获取当前活动上下文
 * @return 当前活动，如果没有则返回null
 */
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

/**
 * 获取主题颜色值
 * @param activity 活动上下文
 * @param colorName 颜色名称
 * @return 十六进制颜色值
 */
String getSettingsThemeColor(Activity activity, String colorName) {
    boolean isDarkTheme = isThemeDark(activity);
    switch (colorName) {
        case "surface": return isDarkTheme ? "#FF1A1A1A" : "#FFF5F5F5";
        case "background": return isDarkTheme ? "#FF000000" : "#FFFFFFFF";
        case "on_surface": return isDarkTheme ? "#FFEFEFEF" : "#FF1A1A1A";
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

/**
 * 获取状态栏高度
 * @param context 上下文
 * @return 状态栏高度像素值
 */
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

/**
 * 设置沉浸式状态栏
 * @param activity 活动
 * @param window 窗口
 * @param isLightMode 是否为浅色模式
 */
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

/**
 * 打开外部浏览器
 * @param context 上下文
 * @param urlValue URL地址
 */
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

/**
 * 创建开关视图
 * @param context 上下文
 * @param initialValue 初始值
 * @param configName 配置名称
 * @param keyName 键名
 * @param itemName 项目名称
 * @param onChangeCallback 变化回调
 * @return 开关视图
 */
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
/**
 * 拼音匹配
 * @param textValue 文本值
 * @param queryValue 查询值
 * @return 是否匹配
 */
boolean pinyinMatchText(String textValue, String queryValue) {
    if (textValue == null || queryValue == null) return false;
    textValue = textValue.toLowerCase();
    queryValue = queryValue.toLowerCase();

    if (textValue.contains(queryValue)) return true;

    String pinyinLetters = getPinyinFirstLetters(textValue);
    return pinyinLetters.contains(queryValue);
}

/**
 * 获取拼音首字母
 * @param textValue 文本值
 * @return 拼音首字母字符串
 */
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

/**
 * 获取单个字符的拼音首字母
 * @param character 字符
 * @return 拼音首字母
 */
String getPinyinFirstLetter(char character) {
    int charCode = (int) character;
    if (charCode >= 0x4E00 && charCode <= 0x9FA5) {
        int index = charCode - 0x4E00;
        String[] pinyinMap = {
            "a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a",
            "b","b","b","b","b","b","b","b","b","b","b","b","b","b","b","b","b","b","b","b",
            "c","c","c","c","c","c","c","c","c","c","c","c","c","c","c","c","c","c","c","c",
            "d","d","d","d","d","d","d","d","d","d","d","d","d","d","d","d","d","d","d","d",
            "e","e","e","e","e","e","e","e","e","e","e","e","e","e","e","e","e","e","e","e",
            "f","f","f","f","f","f","f","f","f","f","f","f","f","f","f","f","f","f","f","f",
            "g","g","g","g","g","g","g","g","g","g","g","g","g","g","g","g","g","g","g","g",
            "h","h","h","h","h","h","h","h","h","h","h","h","h","h","h","h","h","h","h","h",
            "j","j","j","j","j","j","j","j","j","j","j","j","j","j","j","j","j","j","j","j",
            "k","k","k","k","k","k","k","k","k","k","k","k","k","k","k","k","k","k","k","k",
            "l","l","l","l","l","l","l","l","l","l","l","l","l","l","l","l","l","l","l","l",
            "m","m","m","m","m","m","m","m","m","m","m","m","m","m","m","m","m","m","m","m",
            "n","n","n","n","n","n","n","n","n","n","n","n","n","n","n","n","n","n","n","n",
            "o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o","o",
            "p","p","p","p","p","p","p","p","p","p","p","p","p","p","p","p","p","p","p","p",
            "q","q","q","q","q","q","q","q","q","q","q","q","q","q","q","q","q","q","q","q",
            "r","r","r","r","r","r","r","r","r","r","r","r","r","r","r","r","r","r","r","r",
            "s","s","s","s","s","s","s","s","s","s","s","s","s","s","s","s","s","s","s","s",
            "t","t","t","t","t","t","t","t","t","t","t","t","t","t","t","t","t","t","t","t",
            "w","w","w","w","w","w","w","w","w","w","w","w","w","w","w","w","w","w","w","w",
            "x","x","x","x","x","x","x","x","x","x","x","x","x","x","x","x","x","x","x","x",
            "y","y","y","y","y","y","y","y","y","y","y","y","y","y","y","y","y","y","y","y",
            "z","z","z","z","z","z","z","z","z","z","z","z","z","z","z","z","z","z","z","z"
        };
        int mapIndex = index % pinyinMap.length;
        return pinyinMap[mapIndex];
    }
    return String.valueOf(character);
}

/**
 * 清理设置界面资源
 */
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
    SettingsState.settingsListContainer = null;
    SettingsState.settingsScrollView = null;
    SettingsState.settingsHighlightView = null;
    SettingsState.settingsPendingHighlightKey = null;
}

/**
 * 清理所有对话框
 */
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

/**
 * 高亮显示目标项
 * @param targetView 目标视图
 */
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


/**
 * 滚动并高亮目标项
 * @param itemKey 项目键
 */
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

/**
 * 处理搜索结果点击
 * @param activity 活动
 * @param itemKey 项目键
 * @param level1 一级菜单
 * @param level2 二级菜单
 */
void handleSearchResultClick(final Activity activity, final String itemKey, final String level1, final String level2) {
    if (activity == null) return;
    vibrate(activity, 32);

    // 关闭搜索对话框
    if (SettingsState.settingsSearchDialog != null && SettingsState.settingsSearchDialog.isShowing()) {
        SettingsState.settingsSearchDialog.dismiss();
    }
    SettingsState.settingsSearchDialog = null;

    // 设置待高亮的key
    SettingsState.settingsPendingHighlightKey = itemKey;

    // 检查是否需要打开新界面
    boolean needOpenNewMenu = true;

    if (SettingsState.settingsDialogStack != null && !SettingsState.settingsDialogStack.isEmpty()) {
        // 检查当前界面是否已经是目标界面
        Dialog topDialog = (Dialog) SettingsState.settingsDialogStack.get(SettingsState.settingsDialogStack.size() - 1);
        if (topDialog != null && topDialog.isShowing()) {
            // 如果是一级菜单的目标项，直接定位
            if (level1 != null && level2 == null) {
                // 检查是否已经在一级菜单
                if (SettingsState.settingsDialogStack.size() == 1) {
                    needOpenNewMenu = false;
                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            scrollToAndHighlight(itemKey);
                        }
                    }, 100);
                }
            }
            // 如果是二级菜单的目标项
            else if (level1 != null && level2 != null) {
                // 检查是否已经在对应的二级菜单
                if (SettingsState.settingsDialogStack.size() == 2) {
                    needOpenNewMenu = false;
                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            scrollToAndHighlight(itemKey);
                        }
                    }, 100);
                }
            }
        }
    }

    // 需要打开新界面
    if (needOpenNewMenu) {
        uiHandler.postDelayed(new Runnable() {
            public void run() {
                showSettingsMenu(activity, level1, level2, null);
            }
        }, 150);
    }
}

/**
 * 显示菜单
 * @param activity 活动
 * @param level1Title 一级标题
 * @param level2Title 二级标题
 * @param level3Title 三级标题
 */
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

                TextView backBtn = new TextView(activity);
                backBtn.setTextSize(28);
                backBtn.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
                backBtn.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams backBtnParams = new FrameLayout.LayoutParams(-2, -2);
                backBtnParams.gravity = Gravity.CENTER;
                backBtn.setLayoutParams(backBtnParams);

                if (SettingsState.settingsDialogStack.isEmpty()) {
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

                    TextView searchBtn = new TextView(activity);
                    searchBtn.setText("🔍");
                    searchBtn.setTextSize(20);
                    searchBtn.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
                    searchBtn.setGravity(Gravity.CENTER);
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

/**
 * 显示搜索页面
 * @param activity 活动
 */
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
                rootLayout.setBackgroundColor(Color.parseColor(isDark ? "#FF000000" : "#FFFFFFFF"));

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

                TextView backBtn = new TextView(activity);
                backBtn.setText("‹");
                backBtn.setTextSize(28);
                backBtn.setTextColor(Color.parseColor(isDark ? "#FFEFEFEF" : "#FF1A1A1A"));
                backBtn.setGravity(Gravity.CENTER);
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
                searchInput.setTextSize(16);
                searchInput.setTextColor(Color.parseColor(isDark ? "#FFEFEFEF" : "#FF1A1A1A"));
                searchInput.setHintTextColor(Color.parseColor(isDark ? "#99FFFFFF" : "#99000000"));
                searchInput.setBackground(null);
                searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, dp(activity, 40), 1.0f));
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
                        String queryValue = editable.toString().trim();

                        if (queryValue.isEmpty()) {
                            historyContainer.setVisibility(View.VISIBLE);
                            resultsContainer.setVisibility(View.GONE);
                            return;
                        }

                        historyContainer.setVisibility(View.GONE);
                        resultsContainer.setVisibility(View.VISIBLE);
                        resultsContainer.removeAllViews();

                        if (queryValue.length() >= 2) {
                            historyList.remove(queryValue);
                            historyList.add(0, queryValue);
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

                        // 一级菜单项
                        addSearchResultItem(activity, resultsContainer, "设置 > Java脚本", queryValue, "item_java_script", null, null);
                        addSearchResultItem(activity, resultsContainer, "设置 > 脚本设置", queryValue, "item_script_settings", null, null);
                        addSearchResultItem(activity, resultsContainer, "设置 > 设置界面", queryValue, "item_settings_ui", "设置", null);
                        addSearchResultItem(activity, resultsContainer, "开关 > 模拟定位", queryValue, "item_mock_location", null, null);
                        addSearchResultItem(activity, resultsContainer, "开关 > 输入框提示", queryValue, "item_input_hint", null, null);
                        addSearchResultItem(activity, resultsContainer, "功能 > 设置经纬度", queryValue, "item_set_location", null, null);
                        addSearchResultItem(activity, resultsContainer, "功能 > 设置输入框提示词", queryValue, "item_set_input_hint", null, null);
                        addSearchResultItem(activity, resultsContainer, "功能 > 消息统计", queryValue, "item_msg_stats", null, null);
                        addSearchResultItem(activity, resultsContainer, "功能 > 空间操作", queryValue, "item_qzone", null, null);
                        addSearchResultItem(activity, resultsContainer, "功能 > 运行状态", queryValue, "item_run_status", null, null);
                        addSearchResultItem(activity, resultsContainer, "功能 > HTML浏览器", queryValue, "item_html_browser", null, null);
                        addSearchResultItem(activity, resultsContainer, "其他 > 取消/重载", queryValue, "item_cancel_reload", null, null);

                        // 二级菜单项
                        addSearchResultItem(activity, resultsContainer, "设置 > 基础模式", queryValue, "item_basic_mode", "设置", "基础模式");
                        addSearchResultItem(activity, resultsContainer, "设置 > 背景样式", queryValue, "item_bg_icon", "设置", "背景样式");
                        addSearchResultItem(activity, resultsContainer, "设置 > 字体样式", queryValue, "item_font_style", "设置", "字体样式");
                        addSearchResultItem(activity, resultsContainer, "设置 > 提示", queryValue, "item_toast_hint", "设置", "提示");
                        addSearchResultItem(activity, resultsContainer, "设置 > 线程池", queryValue, "item_thread_pool", "设置", "线程池");
                        addSearchResultItem(activity, resultsContainer, "设置 > 悬浮窗设置", queryValue, "item_float_window", "设置", "悬浮窗设置");
                        addSearchResultItem(activity, resultsContainer, "设置 > 调试", queryValue, "item_debug", "设置", "调试");

                        if (resultsContainer.getChildCount() == 0) {
                            TextView noResult = new TextView(activity);
                            noResult.setText("未找到相关设置");
                            noResult.setTextSize(14);
                            noResult.setTextColor(Color.parseColor(isDark ? "#99FFFFFF" : "#99000000"));
                            noResult.setGravity(Gravity.CENTER);
                            noResult.setPadding(dp(activity, 4), dp(activity, 24), dp(activity, 4), dp(activity, 24));
                            resultsContainer.addView(noResult);
                        }
                    }
                });

                searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });

                SettingsState.settingsSearchDialog.setContentView(rootLayout);
                SettingsState.settingsSearchDialog.setCancelable(true);
                SettingsState.settingsSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
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

/**
 * 添加搜索结果项
 * @param activity 活动
 * @param container 容器
 * @param itemText 项目文本
 * @param queryValue 查询值
 * @param itemKey 项目键
 * @param level1 一级菜单
 * @param level2 二级菜单
 */
void addSearchResultItem(Activity activity, LinearLayout container, String itemText, String queryValue, final String itemKey, final String level1, final String level2) {
    if (activity == null || container == null || itemText == null || queryValue == null) return;
    if (pinyinMatchText(itemText, queryValue)) {
        boolean isDark = isThemeDark(activity);
        TextView resultItem = new TextView(activity);
        resultItem.setText(itemText);
        resultItem.setTextSize(16);
        resultItem.setTextColor(Color.parseColor(isDark ? "#FFEFEFEF" : "#FF1A1A1A"));
        resultItem.setPadding(dp(activity, 4), dp(activity, 12), dp(activity, 4), dp(activity, 12));
        resultItem.setClickable(true);
        resultItem.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                handleSearchResultClick(activity, itemKey, level1, level2);
            }
        });
        container.addView(resultItem);
    }
}

/**
 * 构建底部区域
 * @param activity 活动
 */
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

/**
 * 显示确认对话框
 * @param activity 活动
 * @param titleText 标题文本
 * @param messageText 消息文本
 * @param confirmCallback 确认回调
 */
void showSettingsConfirmDialog(Activity activity, String titleText, String messageText, final Runnable confirmCallback) {
    if (activity == null) return;
    boolean isDark = isThemeDark(activity);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 构建菜单内容
 * @param activity 活动
 * @param level1Title 一级标题
 * @param level2Title 二级标题
 * @param level3Title 三级标题
 */
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

/**
 * 构建一级菜单内容
 * @param activity 活动
 */
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
}

/**
 * 构建二级菜单内容
 * @param activity 活动
 * @param level1Title 一级标题
 */
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
        addSettingsItemClickWithKey("其他", "调试", "预览、重置、更新日志", "item_debug", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSettingsMenu(act, "设置", "调试", null);
            }
        }});
    }
}

/**
 * 构建三级菜单内容
 * @param activity 活动
 * @param level1Title 一级标题
 * @param level2Title 二级标题
 * @param level3Title 三级标题
 */
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
                addSettingsColorItem("背景样式", "自定义Hex", null, colorKey, colorValue, null);
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
            addSettingsColorItem("字体样式", "字体颜色" + suffix, "留空自动配色 (推荐)", textColorKey, textColorValue, null);
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
            addSettingsItemClick("调试", "更新日志", "查看版本更新记录", new Runnable() { public void run() {
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
    }
}
/**
 * 显示重置所有设置的二次确认弹窗
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(content);

    builder.setNegativeButton("取消", null);

    builder.setPositiveButton("确认重置", (dialog, which) -> {
        int count = resetAllSettingsToDefault();
        qqToast(0, "已重置 " + count + " 项设置");
    });

    AlertDialog dialog = builder.show();
    applyUiTheme(activity, dialog);
}

/**
 * 重置所有设置为默认值
 * @return 重置的设置数量
 */
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

/**
 * 添加分类
 * @param titleText 标题文本
 */
void addSettingsCategory(String titleText) {
    addSettingsCategory(titleText, null);
}

/**
 * 添加分类
 * @param titleText 标题文本
 * @param descriptionText 描述文本
 */
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
}

/**
 * 添加纯点击项
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param clickCallback 点击回调
 */
void addSettingsItemClick(String categoryName, String itemName, Runnable clickCallback) {
    addSettingsItemClick(categoryName, itemName, null, clickCallback);
}

/**
 * 添加纯点击项
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param descriptionText 描述文本
 * @param clickCallback 点击回调
 */
void addSettingsItemClick(String categoryName, String itemName, String descriptionText, final Runnable clickCallback) {
    addSettingsItemClickWithKey(categoryName, itemName, descriptionText, null, clickCallback);
}

/**
 * 添加纯点击项（带键）
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param descriptionText 描述文本
 * @param itemKey 项目键
 * @param clickCallback 点击回调
 */
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

    container.addView(itemWrapper);
}

/**
 * 添加选择项（带实时更新）
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param updateKey 更新键
 * @param valueText 值文本
 * @param clickCallback 点击回调
 */
void addSettingsItemChoice(String categoryName, String itemName, String updateKey, String valueText, final Runnable clickCallback) {
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

    container.addView(itemWrapper);
}

/**
 * 更新设置项显示文本
 * @param updateKey 更新键
 * @param newText 新文本
 */
void updateSettingsItemText(String updateKey, String newText) {
    if (SettingsState.settingsItemTextViews != null && SettingsState.settingsItemTextViews.containsKey(updateKey)) {
        TextView textView = (TextView) SettingsState.settingsItemTextViews.get(updateKey);
        if (textView != null) {
            textView.setText(newText);
        }
    }
}

/**
 * 添加开关项
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param configName 配置名称
 * @param keyName 键名
 * @param currentValue 当前值
 * @param switchCallback 开关回调
 */
void addSettingsItemSwitch(String categoryName, String itemName, String configName, String keyName, boolean currentValue, final Runnable switchCallback) {
    addSettingsItemSwitchWithKey(categoryName, itemName, null, configName, keyName, currentValue, switchCallback);
}

/**
 * 添加开关项（带键）
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param itemKey 项目键
 * @param configName 配置名称
 * @param keyName 键名
 * @param currentValue 当前值
 * @param switchCallback 开关回调
 */
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

    container.addView(itemWrapper);
}

/**
 * 添加开关项（带描述）
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param descriptionText 描述文本
 * @param keyName 键名
 * @param currentValue 当前值
 * @param onChangeCallback 变化回调
 */
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

    container.addView(itemWrapper);
}

/**
 * 添加输入框项
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param descriptionText 描述文本
 * @param keyName 键名
 * @param hintText 提示文本
 * @param defaultValue 默认值
 * @param onValueChanged 值变化回调
 */
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
    inputEdit.setBackground(roundRect(Color.parseColor(getSettingsThemeColor(activity, "surface")), dp(activity, 8)));
    inputEdit.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
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

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container != null) {
        container.addView(itemLayout);
    }
}

/**
 * 添加调色盘项
 * @param categoryName 分类名称
 * @param itemName 项目名称
 * @param descriptionText 描述文本
 * @param keyName 键名
 * @param defaultValue 默认值
 * @param onColorChanged 颜色变化回调
 */
void addSettingsColorItem(String categoryName, String itemName, String descriptionText, String keyName, String defaultValue, final Runnable onColorChanged) {
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

    LinearLayout colorRow = new LinearLayout(activity);
    colorRow.setOrientation(LinearLayout.HORIZONTAL);
    colorRow.setGravity(Gravity.CENTER_VERTICAL);

    final View colorPreview = new View(activity);
    int previewSize = dp(activity, 36);
    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(previewSize, previewSize);
    previewParams.rightMargin = dp(activity, 12);
    colorPreview.setLayoutParams(previewParams);
    GradientDrawable previewBackground = new GradientDrawable();
    previewBackground.setCornerRadius(dp(activity, 6));
    if (isValidHexColor(currentValue)) {
        previewBackground.setColor(Color.parseColor(currentValue));
    } else {
        previewBackground.setColor(Color.GRAY);
    }
    colorPreview.setBackground(previewBackground);
    colorRow.addView(colorPreview);

    TextView pickerBtn = new TextView(activity);
    pickerBtn.setText("🎨 点击选择颜色");
    pickerBtn.setTextSize(14);
    pickerBtn.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "primary")));
    colorRow.addView(pickerBtn);

    itemLayout.addView(colorRow);

    itemLayout.setBackground(makeFeedbackBg(Color.parseColor(getSettingsThemeColor(activity, "surface")), Color.parseColor(getSettingsThemeColor(activity, "ripple")), 0));
    itemLayout.setClickable(true);

    final String finalKeyName = keyName;
    final String finalDefaultValue = defaultValue;
    itemLayout.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            String currentColor = getString("settings", finalKeyName, finalDefaultValue);
            Activity act = getSettingsCurrentActivity();
            if (act != null) {
                showSimpleColorPickerDialog(act, finalKeyName, currentColor, colorPreview, onColorChanged);
            }
        }
    });

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container != null) {
        container.addView(itemLayout);
    }
}

/**
 * 获取主题模式显示文本
 * @return 显示文本
 */
String getThemeModeDisplayText() {
    String mode = getString("settings", "ui_theme_mode", "default");
    if ("system".equals(mode)) return "跟随系统";
    if ("light".equals(mode)) return "强制浅色";
    if ("dark".equals(mode)) return "强制深色";
    return "默认（推荐）";
}

/**
 * 获取缩放显示文本
 * @return 显示文本
 */
String getScaleDisplayText() {
    String scale = getString("settings", "ui_dialog_scale", "");
    return scale.isEmpty() ? "1.0x (默认)" : scale + "x";
}

/**
 * 获取背景类型显示文本
 * @return 显示文本
 */
String getBgTypeDisplayText() {
    String bgType = getString("settings", "ui_bg_type", "color");
    if ("image".equals(bgType)) return "图片背景";
    if ("gradient".equals(bgType)) return "三色渐变";
    return "纯色背景";
}

/**
 * 获取字体类型显示文本
 * @return 显示文本
 */
String getFontTypeDisplayText() {
    String font = getString("settings", "ui_font_type", "default");
    if ("serif".equals(font)) return "衬线体";
    if ("sans".equals(font)) return "无衬线";
    if ("monospace".equals(font)) return "等宽";
    if ("bold".equals(font)) return "粗体";
    return "默认字体";
}

/**
 * 获取字体大小显示文本
 * @return 显示文本
 */
String getFontSizeDisplayText() {
    String size = getString("settings", "ui_font_size", "1.0");
    if ("0.85".equals(size)) return "小 (0.85x)";
    if ("1.15".equals(size)) return "中 (1.15x)";
    if ("1.3".equals(size)) return "大 (1.3x)";
    return "默认 (1.0x)";
}

/**
 * 获取线程优先级显示文本
 * @return 显示文本
 */
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

/**
 * 获取拒绝策略显示文本
 * @return 显示文本
 */
String getRejectPolicyDisplayText() {
    String policy = getString("settings", "thread_pool_reject_policy", "0");
    if ("1".equals(policy)) return "丢弃最新任务";
    if ("2".equals(policy)) return "抛出异常";
    if ("3".equals(policy)) return "调用者执行";
    return "丢弃最旧任务 (默认)";
}

/**
 * 获取图标类型显示文本
 * @return 显示文本
 */
String getIconTypeDisplayText() {
    String iconPath = getString("settings", "iconPath", "");
    if (iconPath == null || iconPath.isEmpty()) return "未设置";
    if (iconPath.toLowerCase().endsWith(".gif")) return "动态图标 (GIF)";
    return "静态图标";
}

/**
 * 获取FPS显示文本
 * @return 显示文本
 */
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

/**
 * 显示简单颜色选择对话框
 * @param activity 活动
 * @param keyName 键名
 * @param currentValue 当前值
 * @param previewView 预览视图
 * @param onColorChanged 颜色变化回调
 */
void showSimpleColorPickerDialog(final Activity activity, final String keyName, final String currentValue, final View previewView, final Runnable onColorChanged) {
    if (activity == null || keyName == null) return;
    boolean isDark = isThemeDark(activity);

    LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 20), dp(activity, 16));

    final View colorPreview = new View(activity);
    int previewSize = dp(activity, 80);
    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(previewSize, previewSize);
    previewParams.gravity = Gravity.CENTER;
    previewParams.bottomMargin = dp(activity, 16);
    colorPreview.setLayoutParams(previewParams);
    GradientDrawable previewBackground = new GradientDrawable();
    previewBackground.setCornerRadius(dp(activity, 12));
    if (isValidHexColor(currentValue)) {
        previewBackground.setColor(Color.parseColor(currentValue));
    } else {
        previewBackground.setColor(Color.GRAY);
    }
    colorPreview.setBackground(previewBackground);
    rootLayout.addView(colorPreview);

    TextView presetLabel = new TextView(activity);
    presetLabel.setText("预设颜色");
    presetLabel.setTextSize(14);
    presetLabel.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    presetLabel.setPadding(0, 0, 0, dp(activity, 8));
    rootLayout.addView(presetLabel);

    final String[][] presetColors = isDark ? new String[][]{
        {"#FF1E1E1E", "默认黑"}, {"#FF2D2D2D", "深空灰"}, {"#FF1A237E", "午夜蓝"},
        {"#FF4A148C", "暗夜紫"}, {"#FF1B5E20", "墨绿"}, {"#FF880E4F", "酒红"},
        {"#FF3E2723", "深褐"}, {"#FF263238", "深青灰"}
    } : new String[][]{
        {"#FFFFFF", "默认白"}, {"#FFF8F0", "米白"}, {"#FFF0F5", "柔粉"},
        {"#E6F7FF", "天蓝"}, {"#F0FFF0", "薄荷"}, {"#E6E6FA", "香芋紫"},
        {"#FFFFF0", "柠檬黄"}, {"#F5F5F5", "浅灰"}
    };

    GridLayout colorGrid = new GridLayout(activity);
    colorGrid.setColumnCount(4);
    colorGrid.setRowCount(2);
    colorGrid.setPadding(0, 0, 0, dp(activity, 16));

    for (int i = 0; i < presetColors.length; i++) {
        final String colorHex = presetColors[i][0];
        final String colorName = presetColors[i][1];

        FrameLayout colorItem = new FrameLayout(activity);
        int itemSize = dp(activity, 48);
        GridLayout.LayoutParams itemParams = new GridLayout.LayoutParams();
        itemParams.width = itemSize;
        itemParams.height = itemSize;
        itemParams.setMargins(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        colorItem.setLayoutParams(itemParams);

        View colorCircle = new View(activity);
        FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(-1, -1);
        colorCircle.setLayoutParams(circleParams);
        GradientDrawable circleBackground = new GradientDrawable();
        circleBackground.setColor(Color.parseColor(colorHex));
        circleBackground.setCornerRadius(dp(activity, 24));
        circleBackground.setStroke(dp(activity, 2), Color.parseColor(getSettingsThemeColor(activity, "outline")));
        colorCircle.setBackground(circleBackground);
        colorItem.addView(colorCircle);

        colorItem.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                putString("settings", keyName, colorHex);
                previewBackground.setColor(Color.parseColor(colorHex));
                if (previewView != null) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor(colorHex));
                    bg.setCornerRadius(dp(activity, 6));
                    previewView.setBackground(bg);
                }
                qqToast(2, "已选择: " + colorName);
                if (onColorChanged != null) {
                    onColorChanged.run();
                }
            }
        });

        colorGrid.addView(colorItem);
    }
    rootLayout.addView(colorGrid);

    TextView customLabel = new TextView(activity);
    customLabel.setText("自定义颜色 (Hex)");
    customLabel.setTextSize(14);
    customLabel.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    customLabel.setPadding(0, 0, 0, dp(activity, 8));
    rootLayout.addView(customLabel);

    final EditText hexInput = new EditText(activity);
    hexInput.setText(currentValue);
    hexInput.setHint("#RRGGBB");
    hexInput.setTextSize(14);
    hexInput.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface")));
    hexInput.setHintTextColor(Color.parseColor(getSettingsThemeColor(activity, "on_surface_variant")));
    hexInput.setBackground(roundRect(Color.parseColor(getSettingsThemeColor(activity, "surface")), dp(activity, 8)));
    hexInput.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    hexInput.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            String hex = editable.toString().trim();
            if (isValidHexColor(hex)) {
                previewBackground.setColor(Color.parseColor(hex));
                putString("settings", keyName, hex);
                if (previewView != null) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor(hex));
                    bg.setCornerRadius(dp(activity, 6));
                    previewView.setBackground(bg);
                }
            }
        }
    });
    rootLayout.addView(hexInput);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("选择颜色");
    builder.setView(rootLayout);
    builder.setPositiveButton("确定", null);
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

/**
 * 显示主题模式选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示背景类型选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示预设颜色对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示预设渐变对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示字体类型选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示字体大小选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示线程优先级选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示拒绝策略选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示FPS选择对话框
 * @param activity 活动
 */
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

/**
 * 显示缩放滑块对话框
 * @param activity 活动
 */
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

    TextView btnMinus = new TextView(activity);
    btnMinus.setText("−");
    btnMinus.setTextSize(24);
    btnMinus.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "primary")));
    btnMinus.setGravity(Gravity.CENTER);
    btnMinus.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);
    sliderRow.addView(btnMinus);

    final SeekBar seekBar = new SeekBar(activity);
    seekBar.setMax(MAX_PROGRESS);
    seekBar.setProgress((int) ((currentScale - MIN_SCALE) / SCALE_STEP));
    seekBar.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    sliderRow.addView(seekBar);

    TextView btnPlus = new TextView(activity);
    btnPlus.setText("+");
    btnPlus.setTextSize(24);
    btnPlus.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "primary")));
    btnPlus.setGravity(Gravity.CENTER);
    btnPlus.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);
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

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("弹窗大小");
    builder.setView(rootLayout);
    builder.setPositiveButton("确定", null);
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog);
}

/**
 * 显示预览弹窗
 * @param activity 活动
 */
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

    TextView toastButton = new TextView(activity);
    toastButton.setText("测试Toast");
    toastButton.setTextSize(14);
    toastButton.setTextColor(Color.parseColor(getSettingsThemeColor(activity, "primary")));
    toastButton.setGravity(Gravity.CENTER);
    toastButton.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12));
    toastButton.setBackground(roundRect(Color.parseColor(getSettingsThemeColor(activity, "surface")), dp(activity, 20)));
    toastButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            qqToast(2, "这是一个测试Toast");
        }
    });
    buttonRow.addView(toastButton);

    previewContent.addView(buttonRow);

    AlertDialog.Builder previewBuilder = new AlertDialog.Builder(activity,
        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    previewBuilder.setTitle("设置预览");
    previewBuilder.setView(previewContent);
    previewBuilder.setPositiveButton("关闭", null);

    AlertDialog previewDialog = previewBuilder.show();
    applyUiTheme(activity, previewDialog);
}
