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
    static int settingsActivityAliveCount = 0;
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
    static Map settingsFeatureBuilders;
    static Map settingsFeatureRaw;
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
interface SoftSliderListener {
    void onSoftSliderChanged(int value, boolean fromUser);
}
class SoftSliderView extends View {
    private Paint trackPaint;
    private Paint fillPaint;
    private Paint thumbPaint;
    private Paint ringPaint;
    private SoftSliderListener listener;
    private int minValue;
    private int maxValue;
    private int value;
    private int accentColor;
    private int trackColor;
    private float density;
    private float trackHeight;
    private float thumbRadius;
    private boolean dragging;

    public SoftSliderView(Context context) {
        super(context);
        try {
            density = context.getResources().getDisplayMetrics().density;
            if (density <= 0) density = 2f;
            trackHeight = 5f * density;
            thumbRadius = 12f * density;
            trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            thumbPaint.setColor(0xFFFFFFFF);
            thumbPaint.setShadowLayer(3f * density, 0f, 1.5f * density, 0x40000000);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(2f * density);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } catch (Throwable ignore) {}
    }

    public void setRange(int min, int max) {
        if (max < min) max = min;
        minValue = min;
        maxValue = max;
        if (value < minValue) value = minValue;
        if (value > maxValue) value = maxValue;
        invalidate();
    }

    public void setValue(int newValue) {
        if (newValue < minValue) newValue = minValue;
        if (newValue > maxValue) newValue = maxValue;
        value = newValue;
        invalidate();
    }

    public int getValue() { return value; }

    public void setAccentColor(int color) { accentColor = color; invalidate(); }

    public void setTrackColor(int color) { trackColor = color; invalidate(); }

    public void setOnSoftSliderChangedListener(SoftSliderListener l) { listener = l; }

    private float getRatio() {
        if (maxValue <= minValue) return 0f;
        return (float) (value - minValue) / (maxValue - minValue);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null || trackPaint == null) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float cy = h / 2f;
        float left = thumbRadius + 2f * density;
        float right = w - thumbRadius - 2f * density;
        if (right <= left) right = left + 1f;
        float pos = left + (right - left) * getRatio();
        float r = trackHeight / 2f;
        trackPaint.setColor(trackColor);
        canvas.drawRoundRect(new RectF(left, cy - r, right, cy + r), r, r, trackPaint);
        if (pos > left) {
            fillPaint.setColor(accentColor);
            canvas.drawRoundRect(new RectF(left, cy - r, pos, cy + r), r, r, fillPaint);
        }
        canvas.drawCircle(pos, cy, thumbRadius, thumbPaint);
        ringPaint.setColor(accentColor);
        canvas.drawCircle(pos, cy, thumbRadius - density, ringPaint);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;
        int action = event.getAction();
        float x = event.getX();
        if (action == MotionEvent.ACTION_DOWN) {
            dragging = true;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
            updateFromX(x);
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (dragging) updateFromX(x);
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (dragging) updateFromX(x);
            dragging = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            dragging = false;
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return true;
    }

    private void updateFromX(float x) {
        int w = getWidth();
        if (w <= 0) return;
        float left = thumbRadius + 2f * density;
        float right = w - thumbRadius - 2f * density;
        if (right <= left) return;
        float ratio = (x - left) / (right - left);
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;
        int newValue = minValue + Math.round(ratio * (maxValue - minValue));
        if (newValue != value) {
            value = newValue;
            invalidate();
        }
        if (listener != null) listener.onSoftSliderChanged(newValue, true);
    }
}
private volatile long themeColorCacheTime = 0L;
private final java.util.Hashtable themeColorCache = new java.util.Hashtable();
static String lastSearchQuery = "";
private Bitmap cachedBottomIconGithub = null;
private Bitmap cachedBottomIconQq = null;
Activity getSettingsCurrentActivity() {
    Activity ref = SettingsState.settingsCurrentActivityRef;
    if (ref instanceof SettingsActivity && !ref.isFinishing()) {
        return ref;
    }
    Activity activity = safeActivity();
    if (activity != null) {
        return activity;
    }
    if (ref != null && !ref.isFinishing()) {
        return ref;
    }
    return null;
}
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
        case "surface_variant": return isDarkTheme ? "#FF303030" : "#FFE8E8E8";
        case "background": return hasCustomBg ? customBg : (isDarkTheme ? "#FF000000" : "#FFFFFFFF");
        case "on_surface": return hasCustomText ? customText : autoTextColor;
        case "on_surface_strong": return isDarkTheme ? "#FFEFEFEF" : "#FF000000";
        case "on_surface_sub": return isDarkTheme ? "#99EFEFEF" : "#99000000";
        case "input_bg": return isDarkTheme ? "#1AFFFFFF" : "#0D000000";
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
int[] getSettingsGradientColors(Activity activity) {
    if (activity == null) return null;
    try {
        if (!"gradient".equals(getString("settings", "ui_bg_type", "color"))) return null;
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

    itemWrapper.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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

    itemWrapper.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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

	itemWrapper.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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
void addSettingsInputItemWithConfig(String itemName, String descriptionText, final String configName, final String keyName, String hintText, String defaultValue, boolean multiLine, boolean numeric, final Runnable onValueChanged) {
    String itemKey = settingsAutoItemKey(itemName);
    if (SettingsState.settingsIndexMode) { registerSettingsIndexEntry(itemKey, itemName, descriptionText, "input"); return; }
    if (SettingsState.settingsCategoryContainers == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    int defaultNumber = 0;
    try { defaultNumber = Integer.parseInt(defaultValue); } catch (Throwable ignore) {}
    String currentValue = numeric ? String.valueOf(getInt(configName, keyName, defaultNumber)) : getString(configName, keyName, defaultValue);

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
    if (numeric) {
        inputEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    }
    if (multiLine) {
        inputEdit.setSingleLine(false);
        inputEdit.setMaxLines(4);
        inputEdit.setGravity(Gravity.TOP | Gravity.START);
    } else {
        inputEdit.setSingleLine(true);
    }
    itemLayout.addView(inputEdit);

    final String finalConfigName = configName;
    final String finalKeyName = keyName;
    final boolean finalNumeric = numeric;
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
                    if (finalNumeric) {
                        int number = -1;
                        try { number = Integer.parseInt(value); } catch (Throwable ignore) {}
                        if (number >= 0) putInt(finalConfigName, finalKeyName, number);
                    } else {
                        putString(finalConfigName, finalKeyName, value);
                    }
                    if (onValueChanged != null) {
                        onValueChanged.run();
                    }
                }
            }, 300);
        }
    });

    itemLayout.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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
void addSettingsValueSliderItem(String itemName, String descriptionText, String keyName, String hintText, int minValue, int maxValue, int fallbackValue, boolean allowEmpty, String syncKey, String syncTemplate) {
    String itemKey = settingsAutoItemKey(itemName);
    if (SettingsState.settingsIndexMode) { registerSettingsIndexEntry(itemKey, itemName, descriptionText, "input"); return; }
    if (SettingsState.settingsCategoryContainers == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    String stored = getString("settings", keyName, "");
    boolean isEmpty = (stored == null || stored.trim().isEmpty());
    int current = fallbackValue;
    if (!isEmpty) {
        try { current = (int) (Float.parseFloat(stored.trim()) + 0.5f); } catch (Throwable ignore) {}
    }
    if (current < minValue) current = minValue;
    if (current > maxValue) current = maxValue;

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
        descView.setPadding(0, dp(activity, 2), 0, dp(activity, 6));
        itemLayout.addView(descView);
    }

    LinearLayout row = new LinearLayout(activity);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    itemLayout.addView(row);

    final SoftSliderView slider = new SoftSliderView(activity);
    slider.setRange(minValue, maxValue);
    slider.setValue(current);
    slider.setAccentColor(pc(getSettingsThemeColor(activity, "primary")));
    slider.setTrackColor(pc(getSettingsThemeColor(activity, "outline")));
    row.addView(slider, new LinearLayout.LayoutParams(0, dp(activity, 36), 1.0f));

    final EditText valueInput = makeInput(activity, hintText != null ? hintText : "", null);
    if (!isEmpty) valueInput.setText(String.valueOf(current));
    valueInput.setTextSize(14);
    valueInput.setSingleLine(true);
    valueInput.setGravity(Gravity.CENTER);
    valueInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(dp(activity, 76), -2);
    inputLp.leftMargin = dp(activity, 12);
    row.addView(valueInput, inputLp);

    final String finalKeyName = keyName;
    final String finalSyncKey = syncKey;
    final String finalSyncTemplate = syncTemplate;
    final boolean[] syncing = {false};

    final Runnable save = new Runnable() { public void run() {
        String raw = valueInput.getText().toString().trim();
        if (raw.isEmpty()) {
            if (allowEmpty) putString("settings", finalKeyName, "");
            return;
        }
        int value = -1;
        try { value = (int) (Float.parseFloat(raw) + 0.5f); } catch (Throwable ignore) { return; }
        if (value < minValue) value = minValue;
        if (value > maxValue) value = maxValue;
        syncing[0] = true;
        valueInput.setText(String.valueOf(value));
        slider.setValue(value);
        syncing[0] = false;
        putString("settings", finalKeyName, String.valueOf(value));
        if (finalSyncKey != null && finalSyncTemplate != null) {
            updateSettingsItemText(finalSyncKey, finalSyncTemplate.replace("%d", String.valueOf(value)));
        }
    } };

    slider.setOnSoftSliderChangedListener(new SoftSliderListener() {
        public void onSoftSliderChanged(int value, boolean fromUser) {
            if (syncing[0]) return;
            syncing[0] = true;
            valueInput.setText(String.valueOf(value));
            syncing[0] = false;
            putString("settings", finalKeyName, String.valueOf(value));
                if (finalSyncKey != null && finalSyncTemplate != null) {
                updateSettingsItemText(finalSyncKey, finalSyncTemplate.replace("%d", String.valueOf(value)));
            }
        }
    });

    final long[] inputToken = new long[1];
    valueInput.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) {
            if (syncing[0]) return;
            inputToken[0] = System.currentTimeMillis();
            final long token = inputToken[0];
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    if (token != inputToken[0]) return;
                    save.run();
                }
            }, 300);
        }
    });

    itemLayout.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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
                else 停止悬浮窗();
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
        addSettingsFeaturePage("消息与互动", "消息统计", "查看今日 / 累计数据", "item_msg_stats", false, statsBoardFeatureBuilder());
        registerSettingsFeaturePage("消息与互动", "消息统计设置", "统计范围 · 类别 · 数据操作", "item_msg_stats_settings", false, statsSettingsFeatureBuilder());

        addSettingsSubPageEntry("消息与互动", "撤回标记", "重定向已撤回提示 · 文本 · 位置 · 颜色", "item_revoke_mark", "撤回标记");
    }

    if ("定位与网络".equals(level1Title)) {
        addSettingsCategory("开关", "");
        boolean mockLocationState = getBoolean("模拟定位开关", "模拟定位开关", false);
        addSettingsItemSwitchWithKey( "模拟定位", null, null, "全局定位覆盖", mockLocationState, new Runnable() { public void run() {
            setMockLocationEnabled(!getBoolean("模拟定位开关", "模拟定位开关", false));
        }});

        addSettingsCategory("经纬度", "");
        addSettingsLocationBlock("经纬度");

        addSettingsFeaturePage("定位与网络", "空间操作", "秒赞 · 秒评 · 评论内容", "item_qzone_op", false, new SettingsFeatureBuilder() {
            public View build(Activity act) {
                buildQzoneSettingsView((act != null) ? act : getSettingsCurrentActivity());
                return null;
            }
        });
    }

    if ("功能扩展".equals(level1Title)) {
        addSettingsFeaturePage("功能扩展", "功能热插拔", "脚本扩展 · 独立调度", "item_hotplug", true, new SettingsFeatureBuilder() {
            public View build(Activity act) {
                Activity current = (act != null) ? act : getSettingsCurrentActivity();
                if (current == null && context instanceof Activity) current = (Activity) context;
                if (current == null) return null;
                String gid = "";
                String peerName = "";
                try {
                    if (current instanceof SettingsActivity) {
                        SettingsActivity sa = (SettingsActivity) current;
                        if (sa.settingsChatType == 2) gid = sa.settingsPeerUin;
                        peerName = sa.settingsPeerName;
                    } else {
                        int chatType = current.getIntent().getIntExtra("chatType", 0);
                        String peerUin = current.getIntent().getStringExtra("peerUin");
                        String name = current.getIntent().getStringExtra("peerName");
                        if (chatType == 2 && peerUin != null) gid = peerUin;
                        if (name != null) peerName = name;
                    }
                } catch (Throwable e) { traceLog("setwindow_log", "[功能热插拔] 获取会话上下文失败: " + e); }
                return buildHotPlugContentView(current, gid, peerName, true);
            }
        });
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
        addSettingsInputItemWithConfig("欢迎语", "首页顶部显示的文字，留空则显示「欢迎使用」", "settings", "welcome_text", "如: 欢迎使用", "欢迎使用", false, false, new Runnable() { public void run() {
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
        addSettingsFeaturePage("性能与调试", "运行状态", "脚本与宿主信息", "item_runtime_status", false, new SettingsFeatureBuilder() {
            public View build(Activity act) {
                if (SettingsState.settingsIndexMode) return null;
                return build运行状态ContentView((act != null) ? act : getSettingsCurrentActivity());
            }
        });
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

        addSettingsCategory("更新", "");
        addSettingsItemChoiceWithKey( "更新通道", "update_channel", (("github".equals(getString("settings", "update_channel", "github")) ? "GitHub（推荐）" : "Gitee")), new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showUpdateChannelChoiceDialog(act);
        }});
        addSettingsItemSwitchWithKey("接收测试版更新提示", "settings", "receive_beta_update_notice", "接收测试版更新的自动提示", getBoolean("settings", "receive_beta_update_notice", false), null);

        addSettingsCategory("其他", "");
        addSettingsItemClickWithKey("配置备份与导入", "备份全部自定义配置与热插拔脚本，导入前预览", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showSettingsBackupDialog(act);
        }});
        addSettingsItemClickWithKey( "重置所有设置", "恢复默认设置", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showResetAllSettingsConfirmDialog(act);
        }});
    }
}
void buildLevel3MenuContent(Activity activity, String level1Title, String level2Title, String level3Title) {
    if (activity == null || level1Title == null || level2Title == null) return;
    if (level1Title != null) {
        String featureKey = level1Title + "|" + level2Title;
        Object featureBuilder = (SettingsState.settingsFeatureBuilders == null) ? null : SettingsState.settingsFeatureBuilders.get(featureKey);
        if (featureBuilder != null) {
            SettingsFeatureBuilder builder = (SettingsFeatureBuilder) featureBuilder;
            if (SettingsState.settingsIndexMode) { builder.build(activity); return; }
            LinearLayout listContainer = SettingsState.settingsListContainer;
            View featureContent = null;
            try {
                if ("功能热插拔".equals(level2Title)) {
                    String hotPlugGid = "";
                    String hotPlugName = "";
                    try {
                        if (activity instanceof SettingsActivity) {
                            SettingsActivity sa = (SettingsActivity) activity;
                            if (sa.settingsChatType == 2 && sa.settingsPeerUin != null) hotPlugGid = sa.settingsPeerUin;
                            hotPlugName = sa.settingsPeerName;
                        } else {
                            int chatType = activity.getIntent().getIntExtra("chatType", 0);
                            String peerUin = activity.getIntent().getStringExtra("peerUin");
                            String peerName = activity.getIntent().getStringExtra("peerName");
                            if (chatType == 2 && peerUin != null) hotPlugGid = peerUin;
                            if (peerName != null) hotPlugName = peerName;
                        }
                    } catch (Throwable contextError) {
                        traceLog("setwindow_log", "[功能热插拔] 获取会话上下文失败: " + contextError);
                    }
                    featureContent = buildHotPlugContentView(activity, hotPlugGid, hotPlugName, true);
                } else {
                    featureContent = builder.build(activity);
                }
            } catch (Throwable e) {
                java.io.StringWriter errorWriter = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(errorWriter));
                String rawError = errorWriter.toString();
                traceLog("setwindow_log", "[功能页] " + level2Title + " 构建异常: " + rawError);
                if (listContainer != null) {
                    TextView errView = new TextView(activity);
                    errView.setText("页面构建失败:\n" + rawError);
                    errView.setTextSize(13);
                    errView.setTextColor(pc(getSettingsThemeColor(activity, "error")));
                    errView.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 24));
                    listContainer.addView(errView, new LinearLayout.LayoutParams(-1, -2, 0f));
                }
                return;
            }
            boolean rawPage = SettingsState.settingsFeatureRaw != null && "1".equals(SettingsState.settingsFeatureRaw.get(featureKey));
            if (featureContent == null) traceLog("setwindow_log", "[功能页诊断] " + level2Title + " 返回 null，key=" + featureKey + ", activity=" + activity.getClass().getName() + ", builder=" + builder.getClass().getName());
            if (featureContent != null && listContainer != null) {
                if (rawPage) {
                    listContainer.addView(featureContent, new LinearLayout.LayoutParams(-1, 0, 1.0f));
                } else if (featureContent.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    listContainer.addView(featureContent);
                } else {
                    listContainer.addView(featureContent, new LinearLayout.LayoutParams(-1, -2, 0f));
                }
            } else if (listContainer != null) {
                TextView errView = new TextView(activity);
                StringBuilder errorText = new StringBuilder("功能页内容为空（诊断信息）\n页面：").append(level2Title).append("\nkey：").append(featureKey).append("\nraw：").append(rawPage).append("\nactivity：").append(activity.getClass().getName()).append("\nbuilder：").append(builder.getClass().getName()).append("\ncontent：null\n如果没有捕获异常，说明构建器直接返回了 null。");
                errView.setText(errorText.toString());
                errView.setTextSize(13);
                errView.setTextColor(pc(getSettingsThemeColor(activity, "error")));
                errView.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 24));
                listContainer.addView(errView, new LinearLayout.LayoutParams(-1, -2, 0f));
            }
            return;
        }

        if ("基础模式".equals(level2Title)) buildBasicModePage(activity);
        if ("背景样式".equals(level2Title)) buildBackgroundStylePage(activity);
        if ("字体样式".equals(level2Title)) buildFontStylePage(activity);
        if ("提示".equals(level2Title)) buildToastStylePage(activity);
        if ("线程池".equals(level2Title)) buildThreadPoolPage(activity);
        if ("悬浮窗设置".equals(level2Title)) buildFloatingWindowPage(activity);
        if ("撤回标记".equals(level2Title)) buildRevokeMarkPage(activity);
        if ("调试".equals(level2Title)) buildDebugPage(activity);

    }
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
void registerSettingsFeaturePage(String categoryName, String pageName, String descriptionText, String itemKey, boolean raw, SettingsFeatureBuilder builder) {
    if (builder == null || pageName == null) return;
    String pathKey = (categoryName == null ? "" : categoryName) + "|" + pageName;
    if (SettingsState.settingsFeatureBuilders == null) SettingsState.settingsFeatureBuilders = new HashMap();
    if (SettingsState.settingsFeatureRaw == null) SettingsState.settingsFeatureRaw = new HashMap();
    SettingsState.settingsFeatureBuilders.put(pathKey, builder);
    SettingsState.settingsFeatureRaw.put(pathKey, raw ? "1" : "0");

    if (SettingsState.settingsIndexMode) {
        registerSettingsIndexEntry(itemKey, pageName, descriptionText, "page");
        ((SettingsItemMeta) SettingsState.settingsItemMeta.get(itemKey)).targetPage = pageName;
        if (categoryName != null) {
            if (SettingsState.settingsSubPages == null) SettingsState.settingsSubPages = new ArrayList();
            if (!SettingsState.settingsSubPages.contains(pathKey)) SettingsState.settingsSubPages.add(pathKey);
        }
    }
}
void addSettingsFeaturePage(final String categoryName, final String pageName, String descriptionText, String itemKey, final boolean raw, final SettingsFeatureBuilder builder) {
    if (builder == null || pageName == null) return;
    registerSettingsFeaturePage(categoryName, pageName, descriptionText, itemKey, raw, builder);
    if (SettingsState.settingsIndexMode) return;
    addSettingsGridCard(itemKey, pageName, descriptionText, new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) showSettingsMenu(act, categoryName, pageName, null);
    }});
    registerSettingsIndexEntry(itemKey, pageName, descriptionText, "page");
    ((SettingsItemMeta) SettingsState.settingsItemMeta.get(itemKey)).targetPage = pageName;
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

    addSettingsFeaturePage(null, "HTML 浏览器", "本地网页 · 文件管理", "item_html", false, new SettingsFeatureBuilder() {
        public View build(Activity act) {
            if (SettingsState.settingsIndexMode) return null;
            return buildHtmlPageView((act != null) ? act : getSettingsCurrentActivity());
        }
    });

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

    itemLayout.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
    itemLayout.setClickable(true);
    return itemLayout;
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
    block.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
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
    variables.setText("变量池  ›");
    variables.setTextSize(14);
    variables.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    variables.setPadding(0, dp(activity, 14), 0, dp(activity, 4));
    variables.setOnClickListener(new View.OnClickListener() { public void onClick(View view) {
        showVariablePool(getSettingsCurrentActivity(), null, false, "hint");
    }});
    block.addView(variables);
    container.addView(block);
    if (SettingsState.settingsItemViews != null) SettingsState.settingsItemViews.put(itemKey, block);
    registerSettingsIndexEntry(itemKey, "设置输入框提示词", "支持 #变量# 和 ##链接##", "input");
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
        refreshCurrentSettingsPage();
    } });
}
void showToastPosChoiceDialog(final Activity activity) {
    final String[] names = {"底部", "居中", "顶部", "自定义"};
    final String[] values = {"bottom", "center", "top", "custom"};
    showChoiceDialogEx(activity, "弹出位置", names, values, "toast_pos", "bottom", 0, 1, "位置: ", new Runnable() { public void run() {
        refreshCurrentSettingsPage();
    } });
}
void showUpdateChannelChoiceDialog(final Activity activity) {
    final String[] names = {"GitHub（推荐）", "Gitee"};
    final String[] values = {"github", "gitee"};
    showChoiceDialogEx(activity, "更新通道", names, values, "update_channel", "github", 0, 3, "已切换至 ", null);
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
void refreshCurrentSettingsPage() {
    Activity act = getSettingsCurrentActivity();
    if (act == null) return;
    try {
        if (act instanceof SettingsActivity) {
            ((SettingsActivity) act).settingsRebuildTopPage();
        } else {
            showSettingsMenu(act, "界面与显示", "基础模式", null);
        }
    } catch (Throwable e) { traceLog("setwindow_log", "[refreshCurrentSettingsPage] 刷新失败: " + e); }
}
void showDpiModeChoiceDialog(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    final String[] names = {"跟随QQ", "跟随模块", "自定义"};
    final String[] values = {"follow_qq", "follow_module", "custom"};
    String current = getString("settings", "ui_dialog_dpi_mode", "follow_qq");
    int checked = 0;
    for (int i = 0; i < values.length; i++) {
        if (values[i].equals(current)) { checked = i; break; }
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
            isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("显示密度");
    builder.setSingleChoiceItems(names, checked, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_dialog_dpi_mode", values[which]);
            String label = names[which];
            if ("custom".equals(values[which])) {
                String d = getString("settings", "ui_dialog_custom_dpi", "320");
                if (d == null || d.trim().isEmpty()) d = "320";
                label = "自定义 " + d.trim() + " dpi";
            }
            updateSettingsItemText("ui_dialog_dpi_mode", label);
            dialog.dismiss();
            qqToast(2, "显示密度: " + label);
            refreshCurrentSettingsPage();
        }
    });
    builder.setNegativeButton("取消", null);
    AlertDialog alertDialog = builder.show();
    applyUiTheme(activity, alertDialog, 0);
}
LinearLayout buildPreviewInfoRow(Activity activity, String name, String value) {
    LinearLayout row = new LinearLayout(activity);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(activity, 5), 0, dp(activity, 5));

    TextView nameView = new TextView(activity);
    nameView.setText(name);
    nameView.setTextSize(13);
    nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    row.addView(nameView, new LinearLayout.LayoutParams(0, -2, 1.0f));

    TextView valueView = new TextView(activity);
    valueView.setText(value);
    valueView.setTextSize(13);
    valueView.setTypeface(null, Typeface.BOLD);
    valueView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    row.addView(valueView, new LinearLayout.LayoutParams(-2, -2));

    return row;
}
String getDialogDpiModeName() {
    String mode = getString("settings", "ui_dialog_dpi_mode", "follow_qq");
    if (mode == null || mode.isEmpty()) mode = "follow_qq";
    if ("custom".equals(mode)) {
        String d = getString("settings", "ui_dialog_custom_dpi", "320");
        if (d == null || d.trim().isEmpty()) d = "320";
        return "自定义 " + d.trim() + " dpi";
    }
    if ("follow_module".equals(mode)) return "跟随模块";
    return "跟随QQ";
}
String getDialogThemeModeName() {
    String mode = getString("settings", "ui_theme_mode", "default");
    if ("system".equals(mode)) return "跟随系统";
    if ("light".equals(mode)) return "强制浅色";
    if ("dark".equals(mode)) return "强制深色";
    if ("module".equals(mode)) return "跟随模块";
    return "默认";
}
void showDialogPreview(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;

    float hostDensity = activity.getResources().getDisplayMetrics().density;
    if (hostDensity <= 0) hostDensity = 2f;
    float targetDensity = getPluginUiDensity(activity);
    int previewWidth = getDialogWidthPx(activity, hostDensity, targetDensity);
    int previewHeight = getDialogHeightPx(activity, hostDensity, targetDensity);
    float scale = targetDensity / hostDensity;
    boolean dark = isThemeDark(activity);

    String widthRaw = getString("settings", "ui_dialog_width", "");
    String heightRaw = getString("settings", "ui_dialog_height", "");
    boolean widthSet = widthRaw != null && !widthRaw.trim().isEmpty();
    boolean heightSet = heightRaw != null && !heightRaw.trim().isEmpty();
    String widthText = (widthSet ? widthRaw.trim() + "dp" : "自适应") + " → " + previewWidth + "px";
    String heightText = heightSet ? (heightRaw.trim() + "dp → " + previewHeight + "px") : "自适应内容";

    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER_HORIZONTAL);
    content.setPadding(dp(activity, 22), dp(activity, 20), dp(activity, 22), dp(activity, 16));

    TextView previewTitle = new TextView(activity);
    previewTitle.setText("预览标题");
    previewTitle.setTextSize(18);
    previewTitle.setTypeface(null, Typeface.BOLD);
    previewTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    previewTitle.setGravity(Gravity.CENTER);
    content.addView(previewTitle);

    TextView previewText = new TextView(activity);
    previewText.setText("这是一段测试文本，用来查看当前弹窗的实际排版效果。");
    previewText.setTextSize(14);
    previewText.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    previewText.setGravity(Gravity.CENTER);
    previewText.setPadding(0, dp(activity, 10), 0, dp(activity, 16));
    content.addView(previewText);

    LinearLayout infoBox = new LinearLayout(activity);
    infoBox.setOrientation(LinearLayout.VERTICAL);
    infoBox.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
    GradientDrawable infoBg = new GradientDrawable();
    infoBg.setColor(pc(getSettingsThemeColor(activity, "outline")));
    infoBg.setCornerRadius(dp(activity, 12));
    infoBox.setBackground(infoBg);
    infoBox.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

    infoBox.addView(buildPreviewInfoRow(activity, "显示密度", getDialogDpiModeName()));
    infoBox.addView(buildPreviewInfoRow(activity, "密度换算", format2f(hostDensity) + " → " + format2f(targetDensity)));
    infoBox.addView(buildPreviewInfoRow(activity, "视觉缩放", format2f(scale) + "x"));
    infoBox.addView(buildPreviewInfoRow(activity, "弹窗宽度", widthText));
    infoBox.addView(buildPreviewInfoRow(activity, "弹窗高度", heightText));
    infoBox.addView(buildPreviewInfoRow(activity, "弹窗圆角", getUiCornerDp() + "dp"));
    infoBox.addView(buildPreviewInfoRow(activity, "主题模式", getDialogThemeModeName() + (dark ? "（深色）" : "（浅色）")));
    infoBox.addView(buildPreviewInfoRow(activity, "宿主分辨率", activity.getResources().getDisplayMetrics().widthPixels + " x " + activity.getResources().getDisplayMetrics().heightPixels));
    content.addView(infoBox);

    TextView hintTv = new TextView(activity);
    hintTv.setText("以上为本次换算结果，实际显示以当前屏幕密度与缩放为准");
    hintTv.setTextSize(11);
    hintTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    hintTv.setGravity(Gravity.CENTER);
    hintTv.setPadding(0, dp(activity, 12), 0, 0);
    content.addView(hintTv);

    final Dialog dialog = new AlertDialog.Builder(activity, dark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setView(content)
            .setPositiveButton("关闭", null)
            .create();
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
    applyUiTheme(activity, dialog, 0);
    applyDialogSize(activity, dialog.getWindow());
}
void showBgTypeChoiceDialog(final Activity activity) {
    final String[] names = {"纯色背景", "三色渐变", "图片背景"};
    final String[] values = {"color", "gradient", "image"};
    showChoiceDialogEx(activity, "背景类型", names, values, "ui_bg_type", "color", 0, 2, "背景类型已更改", new Runnable() { public void run() {
        SettingsState.settingsIndexReady = false;
        refreshSettingsActivityBg();
        refreshCurrentSettingsPage();
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
            "ui_theme_mode", "ui_dialog_dpi_mode", "ui_dialog_custom_dpi", "ui_dialog_width", "ui_dialog_height",
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
            SettingsState.settingsHostDensity = getPluginUiDensity(activity);
        } catch (Throwable ignore) {
            SettingsState.settingsHostDensity = activity.getResources().getDisplayMetrics().density;
        }
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
class SettingsActivity extends BaseComposeActivity {
    FrameLayout settingsActivityRoot;
    FrameLayout settingsPageContainer;
    List settingsPageStack;
    Handler settingsPageHandler;
    boolean settingsCleanedUp = false;
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
            float density = SettingsState.settingsHostDensity;
            if (base != null && density > 0f) {
                android.content.res.Configuration cfg = new android.content.res.Configuration(base.getResources().getConfiguration());
                cfg.densityDpi = (int)(density * 160f + 0.5f);
                cfg.fontScale = base.getResources().getConfiguration().fontScale;
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
            SettingsState.settingsActivityAliveCount = SettingsState.settingsActivityAliveCount + 1;
            try {
                settingsChatType = getIntent().getIntExtra("chatType", 0);
                String pu = getIntent().getStringExtra("peerUin");
                String pn = getIntent().getStringExtra("peerName");
                settingsPeerUin = (pu != null) ? pu : "";
                settingsPeerName = (pn != null) ? pn : "";
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
            if (SettingsState.settingsActivityAliveCount > 0) {
                SettingsState.settingsActivityAliveCount = SettingsState.settingsActivityAliveCount - 1;
            }
        } catch (Throwable ignore) {}
        try {
            settingsCleanup();
        } catch (Throwable ignore) {
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

    void settingsRebuildTopPage() {
        if (settingsPageStack == null || settingsPageStack.isEmpty() || settingsPageContainer == null) return;
        int topIndex = settingsPageStack.size() - 1;
        SettingsPage top = (SettingsPage) settingsPageStack.get(topIndex);
        if (top == null) return;
        if (top.level1 == null) {
            try { settingsShowHome(); } catch (Throwable e) { traceLog("setwindow_log", "[settingsRebuildTopPage] 重建首页失败: " + e); }
            return;
        }
        SettingsPage fresh = null;
        try {
            fresh = settingsBuildMenuPage(top.level1, top.level2, top.level3);
        } catch (Throwable e) {
            traceLog("setwindow_log", "[settingsRebuildTopPage] 重建失败: " + e);
            return;
        }
        View oldView = top.pageView;
        int index = settingsPageContainer.getChildCount();
        if (oldView != null) {
            int found = settingsPageContainer.indexOfChild(oldView);
            if (found >= 0) index = found;
        }
        settingsPageStack.set(topIndex, fresh);
        settingsPageContainer.addView(fresh.pageView, index);
        if (oldView != null) settingsPageContainer.removeView(oldView);
    }

	boolean settingsPopPage() {
	    if (settingsPageStack == null || settingsPageStack.size() <= 1) return false;
	    SettingsPage top = (SettingsPage) settingsPageStack.remove(settingsPageStack.size() - 1);
	    SettingsPage below = (SettingsPage) settingsPageStack.get(settingsPageStack.size() - 1);
	    settingsActivatePage(below);
	    settingsAnimateOut(top.pageView, below.pageView);
	    settingsRefreshWelcomeText(below);
	    try { refreshStatsBoardOnReturn(); } catch (Throwable ignore) {}
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
        if (l1 == null && l2 != null) {
            settingsOpenHomeFeaturePage(l2);
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

    void settingsOpenHomeFeaturePage(String pageName) {
        Object b = (SettingsState.settingsFeatureBuilders == null) ? null : SettingsState.settingsFeatureBuilders.get("|" + pageName);
        View content = null;
        if (b != null) content = ((SettingsFeatureBuilder) b).build(this);
        if (content == null) {
            settingsShowHome();
            return;
        }
        settingsPushContentViewRaw(pageName, content);
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

        boolean rawFeature = level2 != null && SettingsState.settingsFeatureRaw != null
                && "1".equals(SettingsState.settingsFeatureRaw.get(level1 + "|" + level2));

        page.pageView = pageLayout;
        page.categoryContainers = new HashMap();
        page.itemViews = new HashMap();
        page.itemTextViews = new HashMap();

        LinearLayout listContainer = null;
        if (rawFeature) {
            listContainer = new LinearLayout(this);
            listContainer.setOrientation(LinearLayout.VERTICAL);
            listContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
            pageLayout.addView(listContainer);
            page.scrollView = null;
        } else {
            ScrollView scrollView = new ScrollView(this);
            scrollView.setVerticalScrollBarEnabled(false);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
            listContainer = new LinearLayout(this);
            listContainer.setOrientation(LinearLayout.VERTICAL);
            listContainer.setPadding(0, 0, 0, dp(this, 16));
            scrollView.addView(listContainer);
            pageLayout.addView(scrollView);
            page.scrollView = scrollView;
        }
        page.listContainer = listContainer;

        SettingsState.settingsListContainer = listContainer;
        SettingsState.settingsScrollView = page.scrollView;
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
        if (settingsCategoryList != null && settingsHomeFooter.getParent() == null) {
            settingsCategoryList.addView(settingsHomeFooter, new LinearLayout.LayoutParams(-1, -2));
        }
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
        if (settingsCleanedUp) return;
        settingsCleanedUp = true;

        try {
            if (SettingsState.settingsCurrentActivityRef == this) {
                SettingsState.settingsCurrentActivityRef = null;
            }
        } catch (Throwable ignore) {}

        try {
            if (settingsPageHandler != null) {
                settingsPageHandler.removeCallbacksAndMessages(null);
            }
        } catch (Throwable ignore) {}

        try {
            if (settingsHistoryWriter != null) settingsHistoryWriter.shutdown();
        } catch (Throwable ignore) {}
        settingsHistoryWriter = null;

        try { resetSettingsSearchState(); } catch (Throwable ignore) {}

        try { unbindStatsViewCache(); } catch (Throwable ignore) {}

        try {
            SettingsState.settingsPendingHighlightKey = null;
            SettingsState.settingsPendingHighlightDepth = -1;
        } catch (Throwable ignore) {}

        try {
            if (settingsPageStack != null) settingsPageStack.clear();
        } catch (Throwable ignore) {}

        try {
            if (settingsPageContainer != null) settingsPageContainer.removeAllViews();
        } catch (Throwable ignore) {}

        try { SettingsState.settingsHostDensity = 0f; } catch (Throwable ignore) {}

        try { releaseSettingsIconCache(); } catch (Throwable ignore) {}

        try { cleanupSettingsResources(); } catch (Throwable ignore) {}

        settingsPageHandler = null;
        settingsPageStack = null;
        settingsPageContainer = null;
        settingsActivityRoot = null;
    }
}
try {
    registerActivity(SettingsActivity.class);
    SettingsState.settingsActivityRegistered = true;
} catch (Throwable e) {
    traceLog("setwindow_log", "[register] 注册设置Activity异常: " + e);
}
