/**
 * 设置界面 - 全屏设置风格
 * 统一使用showMenu方法和add方法构建菜单
 */

static Dialog menuDialog;
static LinearLayout currentListContainer;
static Map categoryContainers;
static Activity currentActivity;
static boolean isDarkMode;
static Handler uiHandler = new Handler(Looper.getMainLooper());
static boolean menuOpened = false;

String getThemeColor(String colorName) {
    boolean dark = isDarkMode;
    switch (colorName) {
        case "surface": return dark ? "#FF1A1A1A" : "#FFF5F5F5";
        case "background": return dark ? "#FF000000" : "#FFFFFFFF";
        case "on_surface": return dark ? "#FFEFEFEF" : "#FF1A1A1A";
        case "on_surface_variant": return dark ? "#99FFFFFF" : "#99000000";
        case "primary": return dark ? "#FF8AB4F8" : "#FF2196F3";
        case "primary_container": return dark ? "#1A8AB4F8" : "#1A2196F3";
        case "outline": return dark ? "#33FFFFFF" : "#1A000000";
        case "ripple": return dark ? "#268AB4F8" : "#262196F3";
        case "switch_on": return "#FF34C759";
        case "switch_off": return "#FFE5E5E5";
        case "error": return "#FFFF5555";
        default: return "#FF000000";
    }
}

int dp2px(Context ctx, int dp) {
    float density = ctx.getResources().getDisplayMetrics().density;
    return (int)(dp * density + 0.5f);
}

GradientDrawable createRoundedBg(int color, int radiusDp) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(color);
    gd.setCornerRadius(dp2px(currentActivity, radiusDp));
    return gd;
}

RippleDrawable createRippleBg(int normalColor, int rippleColor, int radiusDp) {
    GradientDrawable content = new GradientDrawable();
    content.setColor(normalColor);
    content.setCornerRadius(dp2px(currentActivity, radiusDp));
    ColorStateList rippleList = ColorStateList.valueOf(rippleColor);
    return new RippleDrawable(rippleList, content, null);
}

boolean isValidHexColor(String color) {
    if (color == null || color.isEmpty()) return false;
    return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$");
}

int getStatusBarHeight(Context ctx) {
    int result = 0;
    int resourceId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resourceId > 0) {
        result = ctx.getResources().getDimensionPixelSize(resourceId);
    }
    return result;
}

void setImmersiveStatusBar(Activity activity, Window window, boolean dark) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | 
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                (dark ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0)
            );
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    } catch (Throwable e) {
        log("settings_error.log", "setImmersiveStatusBar: " + e.getMessage());
    }
}

void openExternalBrowser(Context ctx, String url) {
    try {
        if (url.contains("mqqapi")) {
        ((IJumpApi) QRoute.api(IJumpApi.class)).doJumpAction(ctx, url);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        }
    } catch (Throwable e) {
        log("settings_error.log", "openExternalBrowser: " + e.getMessage());
        Toast("打开失败: " + e.getMessage());
    }
}

View createSwitchView(Context ctx, boolean initVal, final String configName, final String key, final String itemName, final Runnable onChangeCallback) {
    FrameLayout container = new FrameLayout(ctx);
    int swW = dp2px(ctx, 48);
    int swH = dp2px(ctx, 28);
    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(swW, swH);
    container.setLayoutParams(lp);
    
    final View track = new View(ctx);
    FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(-1, -1);
    track.setLayoutParams(trackLp);
    final GradientDrawable trackBg = new GradientDrawable();
    trackBg.setCornerRadius(dp2px(ctx, 14));
    track.setBackground(trackBg);
    container.addView(track);
    
    final View thumb = new View(ctx);
    int thumbSize = dp2px(ctx, 24);
    int margin = dp2px(ctx, 2);
    FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(thumbSize, thumbSize);
    thumbLp.gravity = Gravity.CENTER_VERTICAL | (initVal ? Gravity.RIGHT : Gravity.LEFT);
    thumbLp.setMargins(margin, 0, margin, 0);
    thumb.setLayoutParams(thumbLp);
    GradientDrawable thumbBg = new GradientDrawable();
    thumbBg.setColor(Color.WHITE);
    thumbBg.setCornerRadius(dp2px(ctx, 12));
    thumb.setBackground(thumbBg);
    container.addView(thumb);
    
    final boolean[] state = new boolean[]{initVal};
    
    final Runnable updateUI = new Runnable() {
        public void run() {
            boolean on = state[0];
            trackBg.setColor(on ? Color.parseColor(getThemeColor("switch_on")) : Color.parseColor(getThemeColor("switch_off")));
            FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp2.gravity = Gravity.CENTER_VERTICAL | (on ? Gravity.RIGHT : Gravity.LEFT);
            thumb.setLayoutParams(lp2);
        }
    };
    updateUI.run();
    
    container.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            state[0] = !state[0];
            updateUI.run();
            if (configName != null && key != null) {
                putBoolean(configName, key, state[0]);
            }
            String msg = itemName + (state[0] ? " 已开启" : " 已关闭");
            qqToast(2, msg);
            vibrate(currentActivity, 32);
            if (onChangeCallback != null) {
                onChangeCallback.run();
            }
        }
    });
    
    return container;
}

/**
 * 拼音匹配
 */
boolean pinyinMatch(String text, String query) {
    if (text == null || query == null) return false;
    text = text.toLowerCase();
    query = query.toLowerCase();
    
    // 直接包含
    if (text.contains(query)) return true;
    
    // 拼音首字母匹配
    String pinyin = getPinyinFirstLetters(text);
    return pinyin.contains(query);
}

String getPinyinFirstLetters(String text) {
    StringBuilder sb = new StringBuilder();
    for (char c : text.toCharArray()) {
        String py = getPinyinFirstLetter(c);
        if (py != null && !py.isEmpty()) {
            sb.append(py.toLowerCase());
        }
    }
    return sb.toString();
}

String getPinyinFirstLetter(char c) {
    // 常用汉字拼音首字母映射
    int code = (int) c;
    if (code >= 0x4E00 && code <= 0x9FA5) {
        // 简化的拼音首字母判断
        int index = code - 0x4E00;
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
    return String.valueOf(c);
}

/**
 * 显示菜单
 */
void showMenu(final Activity activity, final String level1, final String level2, final String level3) {
    if (activity == null || activity.isFinishing()) return;
    
    if (level1 == null && menuOpened) return;
    
    uiHandler.post(new Runnable() {
        public void run() {
            try {
                currentActivity = activity;
                isDarkMode = isThemeDark(activity);
                categoryContainers = new HashMap();
                
                if (level1 == null) {
                    menuOpened = true;
                }
                
                menuDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar);
                menuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                
                LinearLayout rootLayout = new LinearLayout(activity);
                rootLayout.setOrientation(LinearLayout.VERTICAL);
                rootLayout.setBackgroundColor(Color.parseColor(getThemeColor("background")));
                
                View statusBarPlaceholder = new View(activity);
                int statusBarHeight = getStatusBarHeight(activity);
                LinearLayout.LayoutParams placeholderLp = new LinearLayout.LayoutParams(-1, statusBarHeight);
                statusBarPlaceholder.setLayoutParams(placeholderLp);
                rootLayout.addView(statusBarPlaceholder);
                
                LinearLayout titleBar = new LinearLayout(activity);
                titleBar.setOrientation(LinearLayout.HORIZONTAL);
                titleBar.setGravity(Gravity.CENTER_VERTICAL);
                titleBar.setPadding(dp2px(activity, 8), dp2px(activity, 8), dp2px(activity, 8), dp2px(activity, 8));
                
                TextView backBtn = new TextView(activity);
                backBtn.setText("‹");
                backBtn.setTextSize(28);
                backBtn.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                backBtn.setGravity(Gravity.CENTER);
                backBtn.setPadding(dp2px(activity, 8), 0, dp2px(activity, 8), 0);
                backBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        vibrate(activity, 32);
                        menuDialog.dismiss();
                    }
                });
                titleBar.addView(backBtn);
                
                String title = "功能菜单";
                if (level1 != null) title = level1;
                if (level2 != null) title = level2;
                if (level3 != null) title = level3;
                
                TextView titleView = new TextView(activity);
                titleView.setText(title);
                titleView.setTextSize(20);
                titleView.setTypeface(null, Typeface.BOLD);
                titleView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                titleView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                titleBar.addView(titleView);
                
                if (level1 == null) {
                    TextView searchBtn = new TextView(activity);
                    searchBtn.setText("🔍");
                    searchBtn.setTextSize(20);
                    searchBtn.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                    searchBtn.setGravity(Gravity.CENTER);
                    searchBtn.setPadding(dp2px(activity, 8), 0, dp2px(activity, 8), 0);
                    searchBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            vibrate(activity, 32);
                            menuDialog.dismiss();
                            showSearchPage(activity);
                        }
                    });
                    titleBar.addView(searchBtn);
                }
                
                rootLayout.addView(titleBar);
                
                ScrollView scrollView = new ScrollView(activity);
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
                
                currentListContainer = new LinearLayout(activity);
                currentListContainer.setOrientation(LinearLayout.VERTICAL);
                currentListContainer.setPadding(0, 0, 0, dp2px(activity, 16));
                
                buildMenuContent(activity, level1, level2, level3);
                
                if (level1 == null) {
                    buildBottomArea(activity);
                }
                
                scrollView.addView(currentListContainer);
                rootLayout.addView(scrollView);
                
                menuDialog.setContentView(rootLayout);
                menuDialog.setCancelable(true);
                menuDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        if (level1 == null) {
                            menuOpened = false;
                        }
                        menuDialog = null;
                        currentListContainer = null;
                        categoryContainers = null;
                    }
                });
                
                Window window = menuDialog.getWindow();
                if (window != null) {
                    window.setLayout(-1, -1);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    setImmersiveStatusBar(activity, window, !isDarkMode);
                }
                
                menuDialog.show();
                
            } catch (Throwable e) {
                log("settings_error.log", "showMenu: " + e.getMessage());
                Toast("菜单打开失败: " + e.getMessage());
                if (level1 == null) {
                    menuOpened = false;
                }
            }
        }
    });
}

/**
 * 显示搜索页面（新界面）
 */
void showSearchPage(final Activity activity) {
    uiHandler.post(new Runnable() {
        public void run() {
            try {
                currentActivity = activity;
                isDarkMode = isThemeDark(activity);
                
                final Dialog searchDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar);
                searchDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                
                LinearLayout rootLayout = new LinearLayout(activity);
                rootLayout.setOrientation(LinearLayout.VERTICAL);
                rootLayout.setBackgroundColor(Color.parseColor(getThemeColor("background")));
                
                View statusBarPlaceholder = new View(activity);
                int statusBarHeight = getStatusBarHeight(activity);
                LinearLayout.LayoutParams placeholderLp = new LinearLayout.LayoutParams(-1, statusBarHeight);
                statusBarPlaceholder.setLayoutParams(placeholderLp);
                rootLayout.addView(statusBarPlaceholder);
                
                // 搜索栏
                LinearLayout searchBar = new LinearLayout(activity);
                searchBar.setOrientation(LinearLayout.HORIZONTAL);
                searchBar.setGravity(Gravity.CENTER_VERTICAL);
                searchBar.setPadding(dp2px(activity, 8), dp2px(activity, 8), dp2px(activity, 8), dp2px(activity, 8));
                
                TextView backBtn = new TextView(activity);
                backBtn.setText("‹");
                backBtn.setTextSize(28);
                backBtn.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                backBtn.setGravity(Gravity.CENTER);
                backBtn.setPadding(dp2px(activity, 8), 0, dp2px(activity, 8), 0);
                backBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        vibrate(activity, 32);
                        searchDialog.dismiss();
                        showMenu(activity, null, null, null);
                    }
                });
                searchBar.addView(backBtn);
                
                final EditText searchInput = new EditText(activity);
                searchInput.setHint("搜索设置项...");
                searchInput.setTextSize(16);
                searchInput.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                searchInput.setHintTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
                searchInput.setBackground(null);
                searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                searchInput.setSingleLine(true);
                searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                searchBar.addView(searchInput);
                
                rootLayout.addView(searchBar);
                
                // 内容区域
                ScrollView scrollView = new ScrollView(activity);
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
                
                final LinearLayout contentContainer = new LinearLayout(activity);
                contentContainer.setOrientation(LinearLayout.VERTICAL);
                contentContainer.setPadding(dp2px(activity, 12), 0, dp2px(activity, 12), dp2px(activity, 16));
                
                scrollView.addView(contentContainer);
                rootLayout.addView(scrollView);
                
                // 加载历史搜索
                final String historyStr = getSetting("settings", "search_history", "");
                final List historyList = new ArrayList();
                if (historyStr != null && !historyStr.isEmpty()) {
                    String[] items = historyStr.split("\\|");
                    for (String item : items) {
                        if (item != null && !item.trim().isEmpty()) {
                            historyList.add(item.trim());
                        }
                    }
                }
                
                // 显示历史搜索
                final LinearLayout historyContainer = new LinearLayout(activity);
                historyContainer.setOrientation(LinearLayout.VERTICAL);
                
                TextView historyTitle = new TextView(activity);
                historyTitle.setText("历史搜索");
                historyTitle.setTextSize(14);
                historyTitle.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
                historyTitle.setPadding(dp2px(activity, 4), dp2px(activity, 8), dp2px(activity, 4), dp2px(activity, 8));
                historyContainer.addView(historyTitle);
                
                for (int i = 0; i < historyList.size(); i++) {
                    final String historyItem = (String) historyList.get(i);
                    TextView historyItemView = new TextView(activity);
                    historyItemView.setText(historyItem);
                    historyItemView.setTextSize(16);
                    historyItemView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                    historyItemView.setPadding(dp2px(activity, 4), dp2px(activity, 12), dp2px(activity, 4), dp2px(activity, 12));
                    historyItemView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
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
                    emptyHint.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
                    emptyHint.setPadding(dp2px(activity, 4), dp2px(activity, 12), dp2px(activity, 4), dp2px(activity, 12));
                    historyContainer.addView(emptyHint);
                }
                
                contentContainer.addView(historyContainer);
                
                // 搜索结果容器
                final LinearLayout resultsContainer = new LinearLayout(activity);
                resultsContainer.setOrientation(LinearLayout.VERTICAL);
                resultsContainer.setVisibility(View.GONE);
                contentContainer.addView(resultsContainer);
                
                // 所有设置项
                final Map allSettings = getAllSettings();
                
                // 搜索监听
                searchInput.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(Editable s) {
                        String query = s.toString().trim();
                        
                        if (query.isEmpty()) {
                            historyContainer.setVisibility(View.VISIBLE);
                            resultsContainer.setVisibility(View.GONE);
                            return;
                        }
                        
                        historyContainer.setVisibility(View.GONE);
                        resultsContainer.setVisibility(View.VISIBLE);
                        resultsContainer.removeAllViews();
                        
                        // 保存搜索历史
                        if (query.length() >= 2) {
                            historyList.remove(query);
                            historyList.add(0, query);
                            if (historyList.size() > 10) {
                                historyList.remove(historyList.size() - 1);
                            }
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < historyList.size(); i++) {
                                if (i > 0) sb.append("|");
                                sb.append(historyList.get(i));
                            }
                            putString("settings", "search_history", sb.toString());
                        }
                        
                        // 搜索匹配
                        Iterator it = allSettings.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry entry = (Map.Entry) it.next();
                            String path = (String) entry.getKey();
                            final Runnable callback = (Runnable) entry.getValue();
                            
                            // 支持拼音匹配
                            if (pinyinMatch(path, query)) {
                                TextView resultItem = new TextView(activity);
                                resultItem.setText(path);
                                resultItem.setTextSize(16);
                                resultItem.setTextColor(Color.parseColor(getThemeColor("on_surface")));
                                resultItem.setPadding(dp2px(activity, 4), dp2px(activity, 12), dp2px(activity, 4), dp2px(activity, 12));
                                resultItem.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        vibrate(activity, 32);
                                        searchDialog.dismiss();
                                        if (callback != null) {
                                            callback.run();
                                        }
                                    }
                                });
                                resultsContainer.addView(resultItem);
                            }
                        }
                        
                        if (resultsContainer.getChildCount() == 0) {
                            TextView noResult = new TextView(activity);
                            noResult.setText("未找到相关设置");
                            noResult.setTextSize(14);
                            noResult.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
                            noResult.setGravity(Gravity.CENTER);
                            noResult.setPadding(dp2px(activity, 4), dp2px(activity, 24), dp2px(activity, 4), dp2px(activity, 24));
                            resultsContainer.addView(noResult);
                        }
                    }
                });
                
                // 软键盘搜索按钮
                searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });
                
                searchDialog.setContentView(rootLayout);
                searchDialog.setCancelable(true);
                searchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        // 返回一级菜单
                        showMenu(activity, null, null, null);
                    }
                });
                
                Window window = searchDialog.getWindow();
                if (window != null) {
                    window.setLayout(-1, -1);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    setImmersiveStatusBar(activity, window, !isDarkMode);
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                
                searchDialog.show();
                
                // 自动弹出键盘
                searchInput.requestFocus();
                
            } catch (Throwable e) {
                log("settings_error.log", "showSearchPage: " + e.getMessage());
                Toast("搜索页面打开失败: " + e.getMessage());
            }
        }
    });
}

Map getAllSettings() {
    Map settings = new HashMap();
    settings.put("功能 > Java脚本", new Runnable() { public void run() { 跳转到页面("me.yxp.qfun.activity.PluginActivity"); }});
    settings.put("功能 > 设置界面", new Runnable() { public void run() { showMenu(currentActivity, "设置", null, null); }});
    settings.put("开关 > 模拟定位", new Runnable() { public void run() { 模拟定位开关(); }});
    settings.put("开关 > 输入框提示", new Runnable() { public void run() { 输入框提示开关(); }});
    settings.put("工具 > 设置经纬度", new Runnable() { public void run() { showLocationDialog(currentActivity); }});
    settings.put("工具 > 设置输入框提示词", new Runnable() { public void run() { showInputDialog(currentActivity); }});
    settings.put("工具 > 消息统计", new Runnable() { public void run() { showStatsDialog(currentActivity); }});
    settings.put("工具 > 空间操作", new Runnable() { public void run() { showQzoneConfig(); }});
    settings.put("工具 > 运行状态", new Runnable() { public void run() { 运行状态Dialog(currentActivity); }});
    settings.put("工具 > HTML浏览器", new Runnable() { public void run() { showHtmlOptionDialog(currentActivity); }});
    settings.put("其他 > 取消/重载", new Runnable() { public void run() { showSelectionDialog(currentActivity, "你想选哪个呢？", "取消加载脚本", "重新加载脚本"); }});
    settings.put("设置 > 基础模式", new Runnable() { public void run() { showMenu(currentActivity, "设置", "基础模式", null); }});
    settings.put("设置 > 背景与图标", new Runnable() { public void run() { showMenu(currentActivity, "设置", "背景与图标", null); }});
    settings.put("设置 > 字体样式", new Runnable() { public void run() { showMenu(currentActivity, "设置", "字体样式", null); }});
    settings.put("设置 > 线程池", new Runnable() { public void run() { showMenu(currentActivity, "设置", "线程池", null); }});
    settings.put("设置 > 悬浮窗设置", new Runnable() { public void run() { showMenu(currentActivity, "设置", "悬浮窗设置", null); }});
    settings.put("设置 > 调试", new Runnable() { public void run() { showMenu(currentActivity, "设置", "调试", null); }});
    settings.put("基础模式 > 主题模式", new Runnable() { public void run() { showMenu(currentActivity, "设置", "基础模式", null); }});
    settings.put("基础模式 > 弹窗大小", new Runnable() { public void run() { showMenu(currentActivity, "设置", "基础模式", null); }});
    settings.put("基础模式 > 振动反馈", new Runnable() { public void run() { showMenu(currentActivity, "设置", "基础模式", null); }});
    return settings;
}

void buildBottomArea(Activity activity) {
    LinearLayout bottomArea = new LinearLayout(activity);
    bottomArea.setOrientation(LinearLayout.VERTICAL);
    bottomArea.setGravity(Gravity.CENTER);
    bottomArea.setPadding(dp2px(activity, 16), dp2px(activity, 8), dp2px(activity, 16), dp2px(activity, 16));
    
    LinearLayout iconRow = new LinearLayout(activity);
    iconRow.setOrientation(LinearLayout.HORIZONTAL);
    iconRow.setGravity(Gravity.CENTER);
    
    ImageView projectBtn = new ImageView(activity);
    try {
        String imgName = isDarkMode ? "黑.png" : "白.png";
        String imgPath = rootPath + imgName;
        File imgFile = new File(imgPath);
        if (imgFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(imgPath);
            if (bmp != null) {
                projectBtn.setImageBitmap(bmp);
            }
        }
    } catch (Throwable e) {
        log("settings_error.log", "load project btn: " + e.getMessage());
    }
    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(dp2px(activity, 40), dp2px(activity, 40));
    btnLp.rightMargin = dp2px(activity, 24);
    projectBtn.setLayoutParams(btnLp);
    projectBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 32);
            showConfirmDialog(activity, "即将打开项目主页", "https://gitee.com/ovoxiaomo/qfloating-x", new Runnable() {
                public void run() {
                    openExternalBrowser(activity, "https://gitee.com/ovoxiaomo/qfloating-x");
                }
            });
        }
    });
    iconRow.addView(projectBtn);
    
    ImageView qqBtn = new ImageView(activity);
    try {
        String imgName = isDarkMode ? "黑.png" : "白.png";
        String imgPath = rootPath + imgName;
        File imgFile = new File(imgPath);
        if (imgFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(imgPath);
            if (bmp != null) {
                qqBtn.setImageBitmap(bmp);
            }
        }
    } catch (Throwable e) {
        log("settings_error.log", "load qq btn: " + e.getMessage());
    }
    LinearLayout.LayoutParams qqBtnLp = new LinearLayout.LayoutParams(dp2px(activity, 40), dp2px(activity, 40));
    qqBtn.setLayoutParams(qqBtnLp);
    qqBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(activity, 32);
            showConfirmDialog(activity, "即将加入QQ群", "702227641", new Runnable() {
                public void run() {
                openExternalBrowser(activity, "mqqapi://app/joinImmediately?source_id=3&version=1.0&src_type=app&pkg=com.tencent.mobileqq&cmp=com.tencent.biz.JoinGroupTransitActivity&group_code=702227641&subsource_id=10019");
                }
            });
        }
    });
    iconRow.addView(qqBtn);
    
    bottomArea.addView(iconRow);
    
    TextView footer = new TextView(activity);
    footer.setText("Generated by QFloatingX");
    footer.setGravity(Gravity.CENTER);
    footer.setTextColor(Color.parseColor(isDarkMode ? "#555555" : "#AAAAAA"));
    footer.setTextSize(10);
    footer.setPadding(0, dp2px(activity, 4), 0, dp2px(activity, 4));
    bottomArea.addView(footer);
    
    currentListContainer.addView(bottomArea);
}

void showConfirmDialog(Activity activity, String title, String url, final Runnable onConfirm) {
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle(title);
    builder.setMessage(url);
    builder.setPositiveButton("打开", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (onConfirm != null) {
                onConfirm.run();
            }
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void buildMenuContent(Activity activity, String level1, String level2, String level3) {
    if (level1 == null) {
        buildLevel1Content(activity);
        return;
    }
    
    if (level2 == null) {
        buildLevel2Content(activity, level1);
        return;
    }
    
    buildLevel3Content(activity, level1, level2, level3);
}

void buildLevel1Content(Activity activity) {
    addCategory("功能");
    addItemClick("功能", "Java脚本", new Runnable() { public void run() { 跳转到页面("me.yxp.qfun.activity.PluginActivity"); }});
    addItemClick("功能", "设置界面", new Runnable() { public void run() { showMenu(activity, "设置", null, null); }});
    
    addCategory("开关");
    // 模拟定位开关：配置名=模拟定位开关，键名=模拟定位开关
    addItemSwitch("开关", "模拟定位", "模拟定位开关", "模拟定位开关", false, new Runnable() { public void run() { 模拟定位开关(); }});
    // 输入框提示开关：配置名=输入框，键名=输入框开关
    addItemSwitch("开关", "输入框提示", "输入框", "输入框开关", false, new Runnable() { public void run() { 输入框提示开关(); }});
    
    addCategory("工具");
    addItemClick("工具", "设置经纬度", new Runnable() { public void run() { showLocationDialog(activity); }});
    addItemClick("工具", "设置输入框提示词", new Runnable() { public void run() { showInputDialog(activity); }});
    addItemClick("工具", "消息统计", new Runnable() { public void run() { showStatsDialog(activity); }});
    addItemClick("工具", "空间操作", new Runnable() { public void run() { showQzoneConfig(); }});
    addItemClick("工具", "运行状态", new Runnable() { public void run() { 运行状态Dialog(activity); }});
    addItemClick("工具", "HTML浏览器", new Runnable() { public void run() { showHtmlOptionDialog(activity); }});
    
    addCategory("其他");
    addItemClick("其他", "取消/重载", new Runnable() { public void run() { 
        vibrate(activity, 48);
        showSelectionDialog(activity, "你想选哪个呢？", "取消加载脚本", "重新加载脚本"); 
    }});
}

void buildLevel2Content(Activity activity, String level1) {
    if ("设置".equals(level1)) {
        addCategory("设置");
        addItemClick("设置", "基础模式", "主题、弹窗大小、振动反馈", new Runnable() { public void run() { showMenu(activity, "设置", "基础模式", null); }});
        addItemClick("设置", "背景与图标", "背景类型、颜色、图片", new Runnable() { public void run() { showMenu(activity, "设置", "背景与图标", null); }});
        addItemClick("设置", "字体样式", "字体风格、大小、颜色", new Runnable() { public void run() { showMenu(activity, "设置", "字体样式", null); }});
        addItemClick("设置", "线程池", "优先级、队列、策略", new Runnable() { public void run() { showMenu(activity, "设置", "线程池", null); }});
        addItemClick("设置", "悬浮窗设置", "图标、大小、灵敏度", new Runnable() { public void run() { showMenu(activity, "设置", "悬浮窗设置", null); }});
        addItemClick("设置", "调试", "预览、重置、更新日志", new Runnable() { public void run() { showMenu(activity, "设置", "调试", null); }});
    }
}

void buildLevel3Content(Activity activity, String level1, String level2, String level3) {
    if ("设置".equals(level1)) {
        if ("基础模式".equals(level2)) {
            addCategory("基础模式");
            addItemClick("基础模式", "主题模式", getThemeModeDisplayText(), new Runnable() { public void run() { showThemeModeChoiceDialog(activity); }});
            addItemClick("基础模式", "弹窗大小(比例)", getScaleDisplayText(), new Runnable() { public void run() { showScaleSliderDialog(activity); }});
            addInputItem("基础模式", "弹窗宽度", "默认最大260dp", "ui_dialog_width", "如: 280", "", null);
            addInputItem("基础模式", "弹窗高度", "自适应内容", "ui_dialog_height", "如: 400", "", null);
            addSwitchItem("基础模式", "振动反馈", null, "振动反馈", true, null);
        }
        
        if ("背景与图标".equals(level2)) {
            addCategory("背景与图标");
            addItemClick("背景与图标", "背景类型", getBgTypeDisplayText(), new Runnable() { public void run() { showBgTypeChoiceDialog(activity); }});
            
            String bgType = getSetting("settings", "ui_bg_type", "color");
            String suffix = isDarkMode ? " (深色模式)" : " (浅色模式)";
            
            if ("color".equals(bgType)) {
                addItemClick("背景与图标", "预设颜色" + suffix, "点击选择内置配色", new Runnable() { public void run() { showPresetColorDialog(activity); }});
                String colorKey = isDarkMode ? "ui_bg_color_dark" : "ui_bg_color_light";
                String colorVal = getSetting("settings", colorKey, isDarkMode ? "#FF1E1E1E" : "#FFFFFF");
                addColorItem("背景与图标", "自定义Hex", null, colorKey, colorVal, null);
            } else if ("gradient".equals(bgType)) {
                addItemClick("背景与图标", "预设渐变" + suffix, "点击选择内置渐变", new Runnable() { public void run() { showPresetGradientDialog(activity); }});
                String gradKey = isDarkMode ? "ui_bg_gradient_dark" : "ui_bg_gradient_light";
                addInputItem("背景与图标", "自定义渐变", "Hex1,Hex2,Hex3", gradKey, "#RRGGBB,#RRGGBB,#RRGGBB", "", null);
            } else if ("image".equals(bgType)) {
                addItemClick("背景与图标", "选择背景图片", "点击选择本地图片", new Runnable() { public void run() {
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        activity.startActivityForResult(intent, 1007);
                        Toast("选择后自动居中裁剪应用");
                    } catch(Throwable e) {
                        Toast("失败: " + e);
                    }
                }});
                addInputItem("背景与图标", "图片模糊 (0-25)", "0为不模糊", "ui_img_blur", "0-25", "0", null);
                addInputItem("背景与图标", "遮罩浓度 (0-255)", "越大越暗", "ui_img_alpha", "0-255", isDarkMode ? "180" : "100", null);
            }
        }
        
        if ("字体样式".equals(level2)) {
            addCategory("字体样式");
            addItemClick("字体样式", "字体风格", getFontTypeDisplayText(), new Runnable() { public void run() { showFontTypeChoiceDialog(activity); }});
            addItemClick("字体样式", "字体大小", getFontSizeDisplayText(), new Runnable() { public void run() { showFontSizeChoiceDialog(activity); }});
            String tKey = isDarkMode ? "ui_text_color_dark" : "ui_text_color_light";
            String tVal = getSetting("settings", tKey, "");
            String suffix = isDarkMode ? " (深色模式)" : " (浅色模式)";
            addColorItem("字体样式", "字体颜色" + suffix, "留空自动配色 (推荐)", tKey, tVal, null);
        }
        
        if ("线程池".equals(level2)) {
            addCategory("线程池");
            addItemClick("线程池", "线程优先级", getThreadPriorityDisplayText(), new Runnable() { public void run() { showThreadPriorityChoiceDialog(activity); }});
            addInputItem("线程池", "任务队列容量", "默认50", "thread_pool_queue_capacity", "数字", "50", null);
            addInputItem("线程池", "核心线程存活(秒)", "默认30", "thread_pool_keep_alive", "秒数", "30", null);
            addItemClick("线程池", "任务满载策略", getRejectPolicyDisplayText(), new Runnable() { public void run() { showRejectPolicyChoiceDialog(activity); }});
        }
        
        if ("悬浮窗设置".equals(level2)) {
            addCategory("悬浮窗设置");
            addItemClick("悬浮窗设置", "更换图标", getIconTypeDisplayText(), new Runnable() { public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    activity.startActivityForResult(intent, 1005);
                    Toast("选择后请重新打开设置刷新");
                } catch(Throwable e) {
                    Toast("文件选择启动失败: " + e);
                }
            }});
            addInputItem("悬浮窗设置", "悬浮窗大小", "默认48", "悬浮窗大小", "dp", "48", null);
            addInputItem("悬浮窗设置", "关闭图标大小", "默认24", "关闭区域图标大小", "dp", "24", null);
            addInputItem("悬浮窗设置", "拖拽灵敏度", "数值越小越灵敏", "拖拽灵敏度", "数字", "12", null);
            addInputItem("悬浮窗设置", "长按关闭阈值", "默认650ms", "长按关闭阈值", "毫秒", "650", null);
            addInputItem("悬浮窗设置", "图标透明度", "0-255", "iconAlpha", "0-255", "255", null);
            
            String icpath = getSetting("settings", "iconPath", "");
            boolean isAnim = icpath.toLowerCase().endsWith(".gif");
            if (isAnim) {
                addItemClick("悬浮窗设置", "帧率设置", getFpsDisplayText(), new Runnable() { public void run() { showFpsChoiceDialog(activity); }});
                String currentDelay = getSetting("settings", "gifDelay", "100");
                addInputItem("悬浮窗设置", "动画速度 (每帧延迟ms)", "越小越快", "gifDelay", "毫秒", currentDelay, null);
            }
        }
        
        if ("调试".equals(level2)) {
            addCategory("调试", "开发者选项");
            addItemClick("调试", "预览设置", "预览当前设置效果", new Runnable() { public void run() { showPreviewPopup(activity); }});
            addItemClick("调试", "重置设置", "恢复默认设置", new Runnable() { public void run() {
                pendingSettingsChanges.clear();
                qqToast(2, "设置已重置");
            }});
            addItemClick("调试", "更新日志", "查看版本更新记录", new Runnable() { public void run() {
                try {
                    String logContent = 读(pluginPath + "/更新日志.txt");
                    mkts(activity, "更新日志", logContent);
                } catch (Throwable e) {
                    Toast("读取更新日志失败: " + e.getMessage());
                }
            }});
        }
    }
}

/**
 * 添加分类
 */
void addCategory(String title) {
    addCategory(title, null);
}

void addCategory(String title, String description) {
    if (currentListContainer == null) return;
    
    TextView categoryTitle = new TextView(currentActivity);
    categoryTitle.setText(title);
    categoryTitle.setTextSize(14);
    categoryTitle.setTypeface(null, Typeface.BOLD);
    categoryTitle.setTextColor(Color.parseColor(getThemeColor("primary")));
    categoryTitle.setPadding(dp2px(currentActivity, 16), dp2px(currentActivity, 20), dp2px(currentActivity, 16), dp2px(currentActivity, 8));
    
    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
    currentListContainer.addView(categoryTitle, titleLp);
    
    if (description != null && !description.isEmpty()) {
        TextView categoryDesc = new TextView(currentActivity);
        categoryDesc.setText(description);
        categoryDesc.setTextSize(12);
        categoryDesc.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
        categoryDesc.setPadding(dp2px(currentActivity, 16), 0, dp2px(currentActivity, 16), dp2px(currentActivity, 8));
        
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(-1, -2);
        currentListContainer.addView(categoryDesc, descLp);
    }
    
    LinearLayout categoryContainer = new LinearLayout(currentActivity);
    categoryContainer.setOrientation(LinearLayout.VERTICAL);
    categoryContainer.setBackground(createRoundedBg(Color.parseColor(getThemeColor("surface")), 16));
    categoryContainer.setClipToOutline(true);
    
    LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(-1, -2);
    containerLp.setMargins(dp2px(currentActivity, 12), 0, dp2px(currentActivity, 12), dp2px(currentActivity, 8));
    currentListContainer.addView(categoryContainer, containerLp);
    
    if (categoryContainers == null) categoryContainers = new HashMap();
    categoryContainers.put(title, categoryContainer);
}

/**
 * 添加纯点击项
 */
void addItemClick(String category, String name, final Runnable onClickCallback) {
    addItemClick(category, name, null, onClickCallback);
}

void addItemClick(String category, String name, String description, final Runnable onClickCallback) {
    LinearLayout container = (LinearLayout) categoryContainers.get(category);
    if (container == null) return;
    
    FrameLayout itemWrapper = new FrameLayout(currentActivity);
    
    LinearLayout itemLayout = new LinearLayout(currentActivity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(dp2px(currentActivity, 16), dp2px(currentActivity, 14), dp2px(currentActivity, 16), dp2px(currentActivity, 14));
    
    LinearLayout textArea = new LinearLayout(currentActivity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    
    TextView nameView = new TextView(currentActivity);
    nameView.setText(name);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    textArea.addView(nameView);
    
    if (description != null && !description.isEmpty()) {
        TextView descView = new TextView(currentActivity);
        descView.setText(description);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
        descView.setPadding(0, dp2px(currentActivity, 2), 0, 0);
        textArea.addView(descView);
    }
    
    itemLayout.addView(textArea);
    
    TextView arrowView = new TextView(currentActivity);
    arrowView.setText("›");
    arrowView.setTextSize(20);
    arrowView.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    itemLayout.addView(arrowView);
    
    itemWrapper.addView(itemLayout);
    
    itemWrapper.setBackground(createRippleBg(Color.parseColor(getThemeColor("surface")), Color.parseColor(getThemeColor("ripple")), 0));
    itemWrapper.setClickable(true);
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            vibrate(currentActivity, 32);
            if (onClickCallback != null) {
                onClickCallback.run();
            }
        }
    });
    
    container.addView(itemWrapper);
}

/**
 * 添加开关项（只能点击开关触发）
 */
void addItemSwitch(String category, String name, String configName, String switchKey, boolean switchDefault, final Runnable switchCallback) {
    LinearLayout container = (LinearLayout) categoryContainers.get(category);
    if (container == null) return;
    
    boolean currentValue = getBoolean(configName, switchKey, switchDefault);
    
    FrameLayout itemWrapper = new FrameLayout(currentActivity);
    
    LinearLayout itemLayout = new LinearLayout(currentActivity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(dp2px(currentActivity, 16), dp2px(currentActivity, 14), dp2px(currentActivity, 16), dp2px(currentActivity, 14));
    
    TextView nameView = new TextView(currentActivity);
    nameView.setText(name);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    itemLayout.addView(nameView);
    
    final View switchView = createSwitchView(currentActivity, currentValue, configName, switchKey, name, switchCallback);
    itemLayout.addView(switchView);
    
    itemWrapper.addView(itemLayout);
    
    // 不设置点击事件，只能点击开关触发
    itemWrapper.setBackground(createRippleBg(Color.parseColor(getThemeColor("surface")), Color.parseColor(getThemeColor("ripple")), 0));
    itemWrapper.setClickable(false);
    
    container.addView(itemWrapper);
}

/**
 * 添加开关项（带描述，点击整个项触发开关）
 */
void addSwitchItem(String category, String name, String description, String key, boolean defaultValue, final Runnable onChangeCallback) {
    LinearLayout container = (LinearLayout) categoryContainers.get(category);
    if (container == null) return;
    
    boolean currentValue = getBoolean("settings", key, defaultValue);
    
    FrameLayout itemWrapper = new FrameLayout(currentActivity);
    
    LinearLayout itemLayout = new LinearLayout(currentActivity);
    itemLayout.setOrientation(LinearLayout.HORIZONTAL);
    itemLayout.setGravity(Gravity.CENTER_VERTICAL);
    itemLayout.setPadding(dp2px(currentActivity, 16), dp2px(currentActivity, 14), dp2px(currentActivity, 16), dp2px(currentActivity, 14));
    
    LinearLayout textArea = new LinearLayout(currentActivity);
    textArea.setOrientation(LinearLayout.VERTICAL);
    textArea.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    
    TextView nameView = new TextView(currentActivity);
    nameView.setText(name);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    textArea.addView(nameView);
    
    if (description != null && !description.isEmpty()) {
        TextView descView = new TextView(currentActivity);
        descView.setText(description);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
        descView.setPadding(0, dp2px(currentActivity, 2), 0, 0);
        textArea.addView(descView);
    }
    
    itemLayout.addView(textArea);
    
    final View switchView = createSwitchView(currentActivity, currentValue, "settings", key, name, onChangeCallback);
    itemLayout.addView(switchView);
    
    itemWrapper.addView(itemLayout);
    
    itemWrapper.setBackground(createRippleBg(Color.parseColor(getThemeColor("surface")), Color.parseColor(getThemeColor("ripple")), 0));
    itemWrapper.setClickable(true);
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            switchView.performClick();
        }
    });
    
    container.addView(itemWrapper);
}

/**
 * 添加输入框项
 */
void addInputItem(String category, String name, String description, String key, String hint, String defaultValue, final Runnable onValueChanged) {
    LinearLayout container = (LinearLayout) categoryContainers.get(category);
    if (container == null) return;
    
    String currentValue = getSetting("settings", key, defaultValue);
    
    LinearLayout itemLayout = new LinearLayout(currentActivity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dp2px(currentActivity, 16), dp2px(currentActivity, 12), dp2px(currentActivity, 16), dp2px(currentActivity, 12));
    
    TextView nameView = new TextView(currentActivity);
    nameView.setText(name);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    itemLayout.addView(nameView);
    
    if (description != null && !description.isEmpty()) {
        TextView descView = new TextView(currentActivity);
        descView.setText(description);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
        descView.setPadding(0, dp2px(currentActivity, 2), 0, dp2px(currentActivity, 8));
        itemLayout.addView(descView);
    }
    
    final EditText inputEdit = new EditText(currentActivity);
    inputEdit.setText(currentValue);
    inputEdit.setHint(hint != null ? hint : "");
    inputEdit.setTextSize(14);
    inputEdit.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    inputEdit.setHintTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    inputEdit.setBackground(createRoundedBg(Color.parseColor(getThemeColor("surface")), 8));
    inputEdit.setPadding(dp2px(currentActivity, 12), dp2px(currentActivity, 10), dp2px(currentActivity, 12), dp2px(currentActivity, 10));
    inputEdit.setSingleLine(true);
    itemLayout.addView(inputEdit);
    
    final String finalKey = key;
    inputEdit.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable s) {
            String value = s.toString().trim();
            putString("settings", finalKey, value);
            if (onValueChanged != null) {
                onValueChanged.run();
            }
        }
    });
    
    itemLayout.setBackground(createRippleBg(Color.parseColor(getThemeColor("surface")), Color.parseColor(getThemeColor("ripple")), 0));
    itemLayout.setClickable(true);
    
    container.addView(itemLayout);
}

/**
 * 添加调色盘项
 */
void addColorItem(String category, String name, String description, String key, String defaultValue, final Runnable onColorChanged) {
    LinearLayout container = (LinearLayout) categoryContainers.get(category);
    if (container == null) return;
    
    String currentValue = getSetting("settings", key, defaultValue);
    
    LinearLayout itemLayout = new LinearLayout(currentActivity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dp2px(currentActivity, 16), dp2px(currentActivity, 12), dp2px(currentActivity, 16), dp2px(currentActivity, 12));
    
    TextView nameView = new TextView(currentActivity);
    nameView.setText(name);
    nameView.setTextSize(16);
    nameView.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    itemLayout.addView(nameView);
    
    if (description != null && !description.isEmpty()) {
        TextView descView = new TextView(currentActivity);
        descView.setText(description);
        descView.setTextSize(12);
        descView.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
        descView.setPadding(0, dp2px(currentActivity, 2), 0, dp2px(currentActivity, 8));
        itemLayout.addView(descView);
    }
    
    LinearLayout colorRow = new LinearLayout(currentActivity);
    colorRow.setOrientation(LinearLayout.HORIZONTAL);
    colorRow.setGravity(Gravity.CENTER_VERTICAL);
    
    final View colorPreview = new View(currentActivity);
    int previewSize = dp2px(currentActivity, 36);
    LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(previewSize, previewSize);
    previewLp.rightMargin = dp2px(currentActivity, 12);
    colorPreview.setLayoutParams(previewLp);
    GradientDrawable previewBg = new GradientDrawable();
    previewBg.setCornerRadius(dp2px(currentActivity, 6));
    if (isValidHexColor(currentValue)) {
        previewBg.setColor(Color.parseColor(currentValue));
    } else {
        previewBg.setColor(Color.GRAY);
    }
    colorPreview.setBackground(previewBg);
    colorRow.addView(colorPreview);
    
    TextView pickerBtn = new TextView(currentActivity);
    pickerBtn.setText("🎨 点击选择颜色");
    pickerBtn.setTextSize(14);
    pickerBtn.setTextColor(Color.parseColor(getThemeColor("primary")));
    colorRow.addView(pickerBtn);
    
    itemLayout.addView(colorRow);
    
    itemLayout.setBackground(createRippleBg(Color.parseColor(getThemeColor("surface")), Color.parseColor(getThemeColor("ripple")), 0));
    itemLayout.setClickable(true);
    
    final String finalKey = key;
    final String finalDefaultValue = defaultValue;
    itemLayout.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String currentColor = getSetting("settings", finalKey, finalDefaultValue);
            showSimpleColorPickerDialog(currentActivity, finalKey, currentColor, colorPreview, onColorChanged);
        }
    });
    
    container.addView(itemLayout);
}

// ==================== 辅助显示文本方法 ====================

String getThemeModeDisplayText() {
    String mode = getSetting("settings", "ui_theme_mode", "default");
    if ("system".equals(mode)) return "跟随系统";
    if ("light".equals(mode)) return "强制浅色";
    if ("dark".equals(mode)) return "强制深色";
    return "默认（推荐）";
}

String getScaleDisplayText() {
    String scale = getSetting("settings", "ui_dialog_scale", "");
    return scale.isEmpty() ? "1.0x (默认)" : scale + "x";
}

String getBgTypeDisplayText() {
    String bgType = getSetting("settings", "ui_bg_type", "color");
    if ("image".equals(bgType)) return "图片背景";
    if ("gradient".equals(bgType)) return "三色渐变";
    return "纯色背景";
}

String getFontTypeDisplayText() {
    String font = getSetting("settings", "ui_font_type", "default");
    if ("serif".equals(font)) return "衬线体";
    if ("sans".equals(font)) return "无衬线";
    if ("monospace".equals(font)) return "等宽";
    if ("bold".equals(font)) return "粗体";
    return "默认字体";
}

String getFontSizeDisplayText() {
    String size = getSetting("settings", "ui_font_size", "1.0");
    if ("0.85".equals(size)) return "小 (0.85x)";
    if ("1.15".equals(size)) return "中 (1.15x)";
    if ("1.3".equals(size)) return "大 (1.3x)";
    return "默认 (1.0x)";
}

String getThreadPriorityDisplayText() {
    String priority = getSetting("settings", "thread_pool_priority", "");
    if (priority.isEmpty()) return "5 (默认)";
    try {
        int p = Integer.parseInt(priority);
        if (p == 1) return "1 (最低)";
        if (p == 10) return "10 (最高)";
        return String.valueOf(p);
    } catch (Throwable e) {
        return "5 (默认)";
    }
}

String getRejectPolicyDisplayText() {
    String policy = getSetting("settings", "thread_pool_reject_policy", "0");
    if ("1".equals(policy)) return "丢弃最新任务";
    if ("2".equals(policy)) return "抛出异常";
    if ("3".equals(policy)) return "调用者执行";
    return "丢弃最旧任务 (默认)";
}

String getIconTypeDisplayText() {
    String icpath = getSetting("settings", "iconPath", "");
    if (icpath.isEmpty()) return "未设置";
    if (icpath.toLowerCase().endsWith(".gif")) return "动态图标 (GIF)";
    return "静态图标";
}

String getFpsDisplayText() {
    String delay = getSetting("settings", "gifDelay", "100");
    try {
        int d = Integer.parseInt(delay);
        int fps = d > 0 ? 1000 / d : 0;
        return fps + " FPS";
    } catch (Throwable e) {
        return "点击选择";
    }
}

// ==================== 对话框方法 ====================

void showSimpleColorPickerDialog(final Activity activity, final String key, final String currentValue, final View previewView, final Runnable onColorChanged) {
    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp2px(activity, 20), dp2px(activity, 16), dp2px(activity, 20), dp2px(activity, 16));
    
    final View colorPreview = new View(activity);
    int previewSize = dp2px(activity, 80);
    LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(previewSize, previewSize);
    previewLp.gravity = Gravity.CENTER;
    previewLp.bottomMargin = dp2px(activity, 16);
    colorPreview.setLayoutParams(previewLp);
    GradientDrawable previewBg = new GradientDrawable();
    previewBg.setCornerRadius(dp2px(activity, 12));
    if (isValidHexColor(currentValue)) {
        previewBg.setColor(Color.parseColor(currentValue));
    } else {
        previewBg.setColor(Color.GRAY);
    }
    colorPreview.setBackground(previewBg);
    root.addView(colorPreview);
    
    TextView presetLabel = new TextView(activity);
    presetLabel.setText("预设颜色");
    presetLabel.setTextSize(14);
    presetLabel.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    presetLabel.setPadding(0, 0, 0, dp2px(activity, 8));
    root.addView(presetLabel);
    
    final String[][] presetColors = isDarkMode ? new String[][]{
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
    colorGrid.setPadding(0, 0, 0, dp2px(activity, 16));
    
    for (int i = 0; i < presetColors.length; i++) {
        final String colorHex = presetColors[i][0];
        final String colorName = presetColors[i][1];
        
        FrameLayout colorItem = new FrameLayout(activity);
        int itemSize = dp2px(activity, 48);
        GridLayout.LayoutParams itemLp = new GridLayout.LayoutParams();
        itemLp.width = itemSize;
        itemLp.height = itemSize;
        itemLp.setMargins(dp2px(activity, 4), dp2px(activity, 4), dp2px(activity, 4), dp2px(activity, 4));
        colorItem.setLayoutParams(itemLp);
        
        View colorCircle = new View(activity);
        FrameLayout.LayoutParams circleLp = new FrameLayout.LayoutParams(-1, -1);
        colorCircle.setLayoutParams(circleLp);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setColor(Color.parseColor(colorHex));
        circleBg.setCornerRadius(dp2px(activity, 24));
        circleBg.setStroke(dp2px(activity, 2), Color.parseColor(getThemeColor("outline")));
        colorCircle.setBackground(circleBg);
        colorItem.addView(colorCircle);
        
        colorItem.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                putString("settings", key, colorHex);
                previewBg.setColor(Color.parseColor(colorHex));
                if (previewView != null) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor(colorHex));
                    bg.setCornerRadius(dp2px(activity, 6));
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
    root.addView(colorGrid);
    
    TextView customLabel = new TextView(activity);
    customLabel.setText("自定义颜色 (Hex)");
    customLabel.setTextSize(14);
    customLabel.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    customLabel.setPadding(0, 0, 0, dp2px(activity, 8));
    root.addView(customLabel);
    
    final EditText hexInput = new EditText(activity);
    hexInput.setText(currentValue);
    hexInput.setHint("#RRGGBB");
    hexInput.setTextSize(14);
    hexInput.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    hexInput.setHintTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    hexInput.setBackground(createRoundedBg(Color.parseColor(getThemeColor("surface")), 8));
    hexInput.setPadding(dp2px(activity, 12), dp2px(activity, 10), dp2px(activity, 12), dp2px(activity, 10));
    hexInput.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable s) {
            String hex = s.toString().trim();
            if (isValidHexColor(hex)) {
                previewBg.setColor(Color.parseColor(hex));
            }
        }
    });
    root.addView(hexInput);
    
    TextView advancedBtn = new TextView(activity);
    advancedBtn.setText("🎨 高级调色盘");
    advancedBtn.setTextSize(14);
    advancedBtn.setTextColor(Color.parseColor(getThemeColor("primary")));
    advancedBtn.setGravity(Gravity.CENTER);
    advancedBtn.setPadding(0, dp2px(activity, 16), 0, 0);
    advancedBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String currentColor = hexInput.getText().toString().trim();
            showColorPickerDialog(activity, currentColor, new OnColorPickedListener() {
                public void onColorPicked(String colorHex) {
                    hexInput.setText(colorHex);
                    putString("settings", key, colorHex);
                    if (previewView != null) {
                        GradientDrawable bg = new GradientDrawable();
                        bg.setColor(Color.parseColor(colorHex));
                        bg.setCornerRadius(dp2px(activity, 6));
                        previewView.setBackground(bg);
                    }
                    if (onColorChanged != null) {
                        onColorChanged.run();
                    }
                }
            });
        }
    });
    root.addView(advancedBtn);
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("选择颜色");
    builder.setView(root);
    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            String hex = hexInput.getText().toString().trim();
            if (isValidHexColor(hex)) {
                putString("settings", key, hex);
                if (previewView != null) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor(hex));
                    bg.setCornerRadius(dp2px(activity, 6));
                    previewView.setBackground(bg);
                }
                qqToast(2, "颜色已保存");
                if (onColorChanged != null) {
                    onColorChanged.run();
                }
            } else {
                qqToast(1, "无效的颜色格式");
            }
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showColorPickerDialog(final Activity activity, final String initialColor, final OnColorPickedListener callback) {
    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp2px(activity, 20), dp2px(activity, 16), dp2px(activity, 20), dp2px(activity, 16));
    
    final View colorPreview = new View(activity);
    int previewSize = dp2px(activity, 100);
    LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(previewSize, previewSize);
    previewLp.gravity = Gravity.CENTER;
    previewLp.bottomMargin = dp2px(activity, 16);
    colorPreview.setLayoutParams(previewLp);
    GradientDrawable previewBg = new GradientDrawable();
    previewBg.setCornerRadius(dp2px(activity, 16));
    int initColor = isValidHexColor(initialColor) ? Color.parseColor(initialColor) : Color.GRAY;
    previewBg.setColor(initColor);
    colorPreview.setBackground(previewBg);
    root.addView(colorPreview);
    
    final TextView hexDisplay = new TextView(activity);
    hexDisplay.setText(initialColor != null ? initialColor : "#000000");
    hexDisplay.setTextSize(16);
    hexDisplay.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    hexDisplay.setGravity(Gravity.CENTER);
    hexDisplay.setPadding(0, 0, 0, dp2px(activity, 16));
    root.addView(hexDisplay);
    
    TextView hueLabel = new TextView(activity);
    hueLabel.setText("色相 (Hue)");
    hueLabel.setTextSize(12);
    hueLabel.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    root.addView(hueLabel);
    
    final SeekBar hueSeekBar = new SeekBar(activity);
    hueSeekBar.setMax(360);
    hueSeekBar.setProgress(0);
    LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(-1, -2);
    seekLp.bottomMargin = dp2px(activity, 12);
    hueSeekBar.setLayoutParams(seekLp);
    root.addView(hueSeekBar);
    
    TextView satLabel = new TextView(activity);
    satLabel.setText("饱和度 (Saturation)");
    satLabel.setTextSize(12);
    satLabel.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    root.addView(satLabel);
    
    final SeekBar satSeekBar = new SeekBar(activity);
    satSeekBar.setMax(100);
    satSeekBar.setProgress(100);
    LinearLayout.LayoutParams satLp = new LinearLayout.LayoutParams(-1, -2);
    satLp.bottomMargin = dp2px(activity, 12);
    satSeekBar.setLayoutParams(satLp);
    root.addView(satSeekBar);
    
    TextView lightLabel = new TextView(activity);
    lightLabel.setText("亮度 (Lightness)");
    lightLabel.setTextSize(12);
    lightLabel.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    root.addView(lightLabel);
    
    final SeekBar lightSeekBar = new SeekBar(activity);
    lightSeekBar.setMax(100);
    lightSeekBar.setProgress(50);
    LinearLayout.LayoutParams lightLp = new LinearLayout.LayoutParams(-1, -2);
    lightSeekBar.setLayoutParams(lightLp);
    root.addView(lightSeekBar);
    
    final float[] hsv = new float[3];
    Color.colorToHSV(initColor, hsv);
    hueSeekBar.setProgress((int) hsv[0]);
    satSeekBar.setProgress((int) (hsv[1] * 100));
    lightSeekBar.setProgress((int) (hsv[2] * 100));
    
    final Runnable updateColor = new Runnable() {
        public void run() {
            hsv[0] = hueSeekBar.getProgress();
            hsv[1] = satSeekBar.getProgress() / 100f;
            hsv[2] = lightSeekBar.getProgress() / 100f;
            int color = Color.HSVToColor(hsv);
            previewBg.setColor(color);
            String hex = String.format("#%06X", (0xFFFFFF & color));
            hexDisplay.setText(hex);
        }
    };
    
    hueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateColor.run(); }
        public void onStartTrackingTouch(SeekBar seekBar) {}
        public void onStopTrackingTouch(SeekBar seekBar) {}
    });
    
    satSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateColor.run(); }
        public void onStartTrackingTouch(SeekBar seekBar) {}
        public void onStopTrackingTouch(SeekBar seekBar) {}
    });
    
    lightSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateColor.run(); }
        public void onStartTrackingTouch(SeekBar seekBar) {}
        public void onStopTrackingTouch(SeekBar seekBar) {}
    });
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("高级调色盘");
    builder.setView(root);
    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            String hex = hexDisplay.getText().toString();
            if (callback != null) {
                callback.onColorPicked(hex);
            }
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showThemeModeChoiceDialog(final Activity activity) {
    final String[] modes = {"默认（推荐）", "跟随系统", "强制浅色", "强制深色"};
    final String[] modeVals = {"default", "system", "light", "dark"};
    
    String currentMode = getSetting("settings", "ui_theme_mode", "default");
    int checkedItem = 0;
    for (int i = 0; i < modeVals.length; i++) {
        if (modeVals[i].equals(currentMode)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("主题模式");
    builder.setSingleChoiceItems(modes, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            String newVal = modeVals[which];
            putString("settings", "ui_theme_mode", newVal);
            if ("dark".equals(newVal)) {
                putBoolean("settings", "黑白", true);
            } else if ("light".equals(newVal)) {
                putBoolean("settings", "黑白", false);
            }
            qqToast(2, "主题已更改");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showBgTypeChoiceDialog(final Activity activity) {
    final String[] types = {"纯色背景", "三色渐变", "图片背景"};
    final String[] typeVals = {"color", "gradient", "image"};
    
    String currentType = getSetting("settings", "ui_bg_type", "color");
    int checkedItem = 0;
    for (int i = 0; i < typeVals.length; i++) {
        if (typeVals[i].equals(currentType)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("背景类型");
    builder.setSingleChoiceItems(types, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_bg_type", typeVals[which]);
            qqToast(2, "背景类型已更改");
            dialog.dismiss();
            menuDialog.dismiss();
            uiHandler.postDelayed(new Runnable() {
                public void run() {
                    showMenu(activity, "设置", "背景与图标", null);
                }
            }, 200);
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showPresetColorDialog(final Activity activity) {
    final String[] colorNames = isDarkMode
        ? new String[]{"默认黑", "深空灰", "午夜蓝", "暗夜紫", "墨绿", "酒红", "深褐", "深青灰"}
        : new String[]{"默认白", "米白", "柔粉", "天蓝", "薄荷", "香芋紫", "柠檬黄", "浅灰"};
    final String[] colorVals = isDarkMode
        ? new String[]{"#FF1E1E1E", "#FF2D2D2D", "#FF1A237E", "#FF4A148C", "#FF1B5E20", "#FF880E4F", "#FF3E2723", "#FF263238"}
        : new String[]{"#FFFFFF", "#FFF8F0", "#FFF0F5", "#E6F7FF", "#F0FFF0", "#E6E6FA", "#FFFFF0", "#F5F5F5"};
    
    final String key = isDarkMode ? "ui_bg_color_dark" : "ui_bg_color_light";
    String currentColor = getSetting("settings", key, isDarkMode ? "#FF1E1E1E" : "#FFFFFF");
    int checkedItem = 0;
    for (int i = 0; i < colorVals.length; i++) {
        if (colorVals[i].equals(currentColor)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("预设颜色");
    builder.setSingleChoiceItems(colorNames, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", key, colorVals[which]);
            qqToast(2, "已选择: " + colorNames[which]);
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showPresetGradientDialog(final Activity activity) {
    final String[] gradNames = {"默认渐变", "落日余晖", "深海幽蓝", "清新森林", "梦幻紫罗兰", "极光", "黑金", "银灰"};
    final String[] gradVals = isDarkMode
        ? new String[]{"#FF2C2C2C,#FF121212,#FF2C2C2C", "#FF4E342E,#FF3E2723,#FF4E342E", "#FF1A237E,#FF0D47A1,#FF1A237E", "#FF1B5E20,#FF33691E,#FF1B5E20", "#FF4A148C,#FF311B92,#FF4A148C", "#FF006064,#FF004D40,#FF006064", "#FF212121,#FF000000,#FF212121", "#FF2D2D2D,#FF1A1A1A,#FF2D2D2D"}
        : new String[]{"#FFFFFFFF,#FFF5F5F5,#FFFFFFFF", "#FFFFE0B2,#FFFFCC80,#FFFFE0B2", "#FFBBDEFB,#FF90CAF9,#FFBBDEFB", "#FFC8E6C9,#FFA5D6A7,#FFC8E6C9", "#FFE1BEE7,#FFCE93D8,#FFE1BEE7", "#FFB2EBF2,#FF80DEEA,#FFB2EBF2", "#FFF5F5F5,#FFE0E0E0,#FFF5F5F5", "#FFF8F8F8,#FFECECEC,#FFF8F8F8"};
    
    final String key = isDarkMode ? "ui_bg_gradient_dark" : "ui_bg_gradient_light";
    String currentGrad = getSetting("settings", key, "");
    int checkedItem = -1;
    for (int i = 0; i < gradVals.length; i++) {
        if (gradVals[i].equals(currentGrad)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("预设渐变");
    builder.setSingleChoiceItems(gradNames, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", key, gradVals[which]);
            qqToast(2, "已选择: " + gradNames[which]);
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showFontTypeChoiceDialog(final Activity activity) {
    final String[] fonts = {"默认字体", "衬线体", "无衬线", "等宽", "粗体"};
    final String[] fontVals = {"default", "serif", "sans", "monospace", "bold"};
    
    String currentFont = getSetting("settings", "ui_font_type", "default");
    int checkedItem = 0;
    for (int i = 0; i < fontVals.length; i++) {
        if (fontVals[i].equals(currentFont)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("字体风格");
    builder.setSingleChoiceItems(fonts, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_font_type", fontVals[which]);
            qqToast(2, "字体已更改");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showFontSizeChoiceDialog(final Activity activity) {
    final String[] sizes = {"小 (0.85x)", "默认 (1.0x)", "中 (1.15x)", "大 (1.3x)"};
    final String[] sizeVals = {"0.85", "1.0", "1.15", "1.3"};
    
    String currentSize = getSetting("settings", "ui_font_size", "1.0");
    int checkedItem = 1;
    for (int i = 0; i < sizeVals.length; i++) {
        if (sizeVals[i].equals(currentSize)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("字体大小");
    builder.setSingleChoiceItems(sizes, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "ui_font_size", sizeVals[which]);
            qqToast(2, "字体大小已更改");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showThreadPriorityChoiceDialog(final Activity activity) {
    final String[] priorities = {"1 (最低)", "2", "3", "4", "5 (默认)", "6", "7", "8", "9", "10 (最高)"};
    final String[] priorityNums = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    
    String currentPriority = getSetting("settings", "thread_pool_priority", "");
    int checkedItem = 4;
    for (int i = 0; i < priorityNums.length; i++) {
        if (priorityNums[i].equals(currentPriority)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("线程优先级");
    builder.setSingleChoiceItems(priorities, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "thread_pool_priority", priorityNums[which]);
            qqToast(2, "优先级已设置");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showRejectPolicyChoiceDialog(final Activity activity) {
    final String[] policies = {"丢弃最旧任务 (默认)", "丢弃最新任务", "抛出异常", "调用者执行"};
    final String[] policyVals = {"0", "1", "2", "3"};
    
    String currentPolicy = getSetting("settings", "thread_pool_reject_policy", "0");
    int checkedItem = 0;
    for (int i = 0; i < policyVals.length; i++) {
        if (policyVals[i].equals(currentPolicy)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("任务满载策略");
    builder.setSingleChoiceItems(policies, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "thread_pool_reject_policy", policyVals[which]);
            qqToast(2, "策略已设置");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showFpsChoiceDialog(final Activity activity) {
    float refreshRate = 60f;
    try {
        Display display = activity.getWindowManager().getDefaultDisplay();
        refreshRate = display.getRefreshRate();
    } catch (Throwable e) {
        log("settings_error.log", "get refresh rate: " + e.getMessage());
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
    
    final String[] fpsArr = (String[]) fpsList.toArray(new String[0]);
    final String[] delayArr = (String[]) delayList.toArray(new String[0]);
    
    String currentDelay = getSetting("settings", "gifDelay", "100");
    int checkedItem = -1;
    for (int i = 0; i < delayArr.length; i++) {
        if (delayArr[i].equals(currentDelay)) {
            checkedItem = i;
            break;
        }
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("帧率设置");
    builder.setSingleChoiceItems(fpsArr, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            putString("settings", "gifDelay", delayArr[which]);
            qqToast(2, "帧率已设置");
            dialog.dismiss();
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showScaleSliderDialog(final Activity activity) {
    String currentScaleStr = getString("settings", "ui_dialog_scale", "1.0");
    float currentScale = 1.0f;
    try {
        currentScale = Float.parseFloat(currentScaleStr);
    } catch (Throwable e) {
        log("settings_error.log", "parse scale: " + e.getMessage());
    }
    
    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp2px(activity, 24), dp2px(activity, 20), dp2px(activity, 24), dp2px(activity, 20));
    
    final TextView valueText = new TextView(activity);
    valueText.setText(String.format("%.2f", currentScale) + "x");
    valueText.setTextSize(28);
    valueText.setTypeface(null, Typeface.BOLD);
    valueText.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    valueText.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
    valueParams.bottomMargin = dp2px(activity, 20);
    root.addView(valueText, valueParams);
    
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
    btnMinus.setTextColor(Color.parseColor(getThemeColor("primary")));
    btnMinus.setGravity(Gravity.CENTER);
    btnMinus.setPadding(dp2px(activity, 12), 0, dp2px(activity, 12), 0);
    sliderRow.addView(btnMinus);
    
    final SeekBar seekBar = new SeekBar(activity);
    seekBar.setMax(MAX_PROGRESS);
    seekBar.setProgress((int) ((currentScale - MIN_SCALE) / SCALE_STEP));
    seekBar.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    sliderRow.addView(seekBar);
    
    TextView btnPlus = new TextView(activity);
    btnPlus.setText("+");
    btnPlus.setTextSize(24);
    btnPlus.setTextColor(Color.parseColor(getThemeColor("primary")));
    btnPlus.setGravity(Gravity.CENTER);
    btnPlus.setPadding(dp2px(activity, 12), 0, dp2px(activity, 12), 0);
    sliderRow.addView(btnPlus);
    
    root.addView(sliderRow);
    
    final Runnable updateValue = new Runnable() {
        public void run() {
            float scale = MIN_SCALE + seekBar.getProgress() * SCALE_STEP;
            valueText.setText(String.format("%.2f", scale) + "x");
        }
    };
    
    btnMinus.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            seekBar.setProgress(Math.max(0, seekBar.getProgress() - 10));
            updateValue.run();
        }
    });
    
    btnPlus.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            seekBar.setProgress(Math.min(MAX_PROGRESS, seekBar.getProgress() + 10));
            updateValue.run();
        }
    });
    
    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
            updateValue.run();
        }
        public void onStartTrackingTouch(SeekBar sb) {}
        public void onStopTrackingTouch(SeekBar sb) {}
    });
    
    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("弹窗大小");
    builder.setView(root);
    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            float scale = MIN_SCALE + seekBar.getProgress() * SCALE_STEP;
            putString("settings", "ui_dialog_scale", String.format("%.2f", scale));
            qqToast(2, "弹窗大小已设置");
        }
    });
    builder.setNegativeButton("取消", null);
    builder.show();
}

void showPreviewPopup(Activity activity) {
    LinearLayout previewContent = new LinearLayout(activity);
    previewContent.setOrientation(LinearLayout.VERTICAL);
    previewContent.setGravity(Gravity.CENTER);
    previewContent.setPadding(dp2px(activity, 24), dp2px(activity, 24), dp2px(activity, 24), dp2px(activity, 24));
    
    TextView previewTitle = new TextView(activity);
    previewTitle.setText("预览标题");
    previewTitle.setTextSize(18);
    previewTitle.setTypeface(null, Typeface.BOLD);
    previewTitle.setTextColor(Color.parseColor(getThemeColor("on_surface")));
    previewTitle.setGravity(Gravity.CENTER);
    previewContent.addView(previewTitle);
    
    TextView previewText = new TextView(activity);
    previewText.setText("这是一段测试文本，用于预览设置效果。");
    previewText.setTextSize(14);
    previewText.setTextColor(Color.parseColor(getThemeColor("on_surface_variant")));
    previewText.setGravity(Gravity.CENTER);
    previewText.setPadding(0, dp2px(activity, 12), 0, dp2px(activity, 16));
    previewContent.addView(previewText);
    
    Button toastButton = new Button(activity);
    toastButton.setText("测试Toast");
    toastButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            qqToast(2, "这是一个测试Toast");
        }
    });
    previewContent.addView(toastButton);
    
    AlertDialog.Builder previewBuilder = new AlertDialog.Builder(activity,
        isDarkMode ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    previewBuilder.setTitle("设置预览");
    previewBuilder.setView(previewContent);
    previewBuilder.setPositiveButton("关闭", null);
    
    AlertDialog previewDialog = previewBuilder.show();
    applyUiTheme(activity, previewDialog);
}

// ==================== 入口方法 ====================

void 显示菜单(final Activity activity) {
    showMenu(activity, null, null, null);
}

void showStyleSettingsDialog(final Activity activity) {
    showMenu(activity, "设置", null, null);
}
