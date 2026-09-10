import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.RenderEffect; // Android 12+
import android.graphics.Shader;       // Android 12+
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.URLUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.ArrayList;

// 引入基类
import me.yxp.qfun.activity.BaseComposeActivity;

public class HtmlPreviewActivity extends BaseComposeActivity implements View.OnClickListener, View.OnTouchListener {

    private WebView webView;
    private TextView titleView;
    private ProgressBar centerSpinner;
    private FrameLayout rootFrame;
    private LinearLayout contentWrapper;
    
    // 底栏相关
    private LinearLayout bottomSheetLayout;
    private View dragHandle;
    private LinearLayout headerContainer; 
    private LinearLayout controlsContainer;
    private SnifferWindow snifferWindow;
    
    // 动画状态变量
    private float initialY;
    private float startTranslationY;
    private int maxDragRange = 0;
    private boolean isDragging = false;
    private boolean isExpanded = true;
    private long downTime = 0;
    private static final long TAP_TIMEOUT = 180;
    private boolean isTranslationActive = false;
    
    // 按钮
    private TextView btnBack, btnForward, btnRefresh, btnSniff, btnTranslate, btnZoomIn, btnZoomOut, btnClose;
    private HashSet sniffedResources = new HashSet();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            final String filePath = getIntent().getStringExtra("filePath");
            boolean isDark = isThemeDark(this);

            // 沉浸式状态栏
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | (isDark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);

            // --- 1. 根布局 ---
            rootFrame = new FrameLayout(this);
            rootFrame.setBackgroundColor(tc(this, "background"));

            // --- 2. 内容容器 ---
            contentWrapper = new LinearLayout(this);
            contentWrapper.setOrientation(1);
            contentWrapper.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            
            // 状态栏占位
            View statusBarSpacer = new View(this);
            statusBarSpacer.setLayoutParams(new LinearLayout.LayoutParams(-1, getStatusBarHeight()));
            statusBarSpacer.setBackgroundColor(Color.TRANSPARENT);
            contentWrapper.addView(statusBarSpacer);

            // WebView
            webView = new WebView(this);
            LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(-1, -1);
            webParams.bottomMargin = dp(60);
            webView.setLayoutParams(webParams);
            initWebSettings(webView);
            registerForContextMenu(webView);
            
            contentWrapper.addView(webView);
            rootFrame.addView(contentWrapper);

            // --- 3. 中心加载圈 ---
            centerSpinner = new ProgressBar(this);
            FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(dp(50), dp(50));
            spinnerParams.gravity = Gravity.CENTER;
            centerSpinner.setLayoutParams(spinnerParams);
            centerSpinner.setVisibility(View.GONE);
            rootFrame.addView(centerSpinner);

            // --- 4. 底部抽屉 (默认收起) ---
            bottomSheetLayout = new LinearLayout(this);
            bottomSheetLayout.setOrientation(1);
            
            GradientDrawable simpleBg = new GradientDrawable();
            simpleBg.setColor(tc(this, "surface"));
            simpleBg.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0});
            bottomSheetLayout.setBackground(simpleBg);
            
            FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(-1, -2);
            bottomParams.gravity = Gravity.BOTTOM;
            bottomSheetLayout.setLayoutParams(bottomParams);

            // 4.1 头部容器
            headerContainer = new LinearLayout(this);
            headerContainer.setOrientation(1);
            headerContainer.setPadding(0, dp(10), 0, 0);
            headerContainer.setOnTouchListener(this);
            
            // 小横条
            dragHandle = new View(this);
            LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(40), dp(5));
            handleParams.gravity = Gravity.CENTER_HORIZONTAL;
            handleParams.bottomMargin = dp(5);
            dragHandle.setLayoutParams(handleParams);
            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setColor(tca(this, "on_surface", 0x55));
            handleBg.setCornerRadius(dp(2.5f));
            dragHandle.setBackground(handleBg);
            headerContainer.addView(dragHandle);

            // 标题行
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(0);
            titleRow.setGravity(16);
            titleRow.setPadding(dp(20), 0, dp(12), dp(10));
            
            titleView = new TextView(this);
            titleView.setText("加载中...");
            titleView.setTextSize(16);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setTextColor(tc(this, "on_surface"));
            titleView.setSingleLine(true);
            titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams titleTextParams = new LinearLayout.LayoutParams(0, -2);
            titleTextParams.weight = 1.0f;
            titleView.setLayoutParams(titleTextParams);
            
            btnRefresh = createButton(this, "↻", tc(this, "on_surface"), Color.TRANSPARENT, 22f, 0, 12, 0, false, 0, 0, null);
            btnRefresh.setOnClickListener(this);
            titleRow.addView(titleView);
            titleRow.addView(btnRefresh);
            headerContainer.addView(titleRow);
            bottomSheetLayout.addView(headerContainer);

            // 4.2 功能按钮区
            controlsContainer = new LinearLayout(this);
            controlsContainer.setOrientation(1);
            controlsContainer.setPadding(dp(10), 0, dp(10), dp(20));
            
            // 第一行
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(0);
            int gridBg = tc(this, "outline");
            int gridTxt = tc(this, "on_surface");
            btnBack = createButton(this, "<后退", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            btnForward = createButton(this, "前进>", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            btnZoomOut = createButton(this, "缩小-", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            btnZoomIn = createButton(this, "放大+", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            applyGridBtnParams(btnBack); applyGridBtnParams(btnForward);
            applyGridBtnParams(btnZoomOut); applyGridBtnParams(btnZoomIn);
            row1.addView(btnBack); row1.addView(btnForward);
            row1.addView(btnZoomOut); row1.addView(btnZoomIn);
            
            // 第二行
            LinearLayout row2 = new LinearLayout(this);
            row2.setOrientation(0);
            row2.setPadding(0, dp(8), 0, 0);
            btnSniff = createButton(this, "嗅探", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            btnTranslate = createButton(this, "翻译", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            btnClose = createButton(this, "退出", gridTxt, gridBg, 12f, 8, 12, 12, false, 0, 0, null);
            applyGridBtnParams(btnSniff); applyGridBtnParams(btnTranslate); applyGridBtnParams(btnClose);
            btnClose.setTextColor(tc(this, "error"));
            row2.addView(btnSniff); row2.addView(btnTranslate); row2.addView(btnClose);

            // 绑定事件
            btnBack.setOnClickListener(this); btnForward.setOnClickListener(this);
            btnZoomIn.setOnClickListener(this); btnZoomOut.setOnClickListener(this);
            btnSniff.setOnClickListener(this); btnTranslate.setOnClickListener(this);
            btnClose.setOnClickListener(this);

            controlsContainer.addView(row1);
            controlsContainer.addView(row2);
            bottomSheetLayout.addView(controlsContainer);
            
            rootFrame.addView(bottomSheetLayout);

            // --- 5. 嗅探小窗 ---
            snifferWindow = new SnifferWindow(this);
            rootFrame.addView(snifferWindow.getView());

            setContentView(rootFrame);
            traceLog("api7_log", "ContentView设置完成");

            // --- 6. 延迟初始化 (默认收起底栏) ---
            bottomSheetLayout.post(new Runnable() {
                public void run() {
                    try {
                        maxDragRange = controlsContainer.getHeight();
                        if (maxDragRange <= 0) {
                            traceLog("api7_log", "获取高度失败，使用默认值");
                            maxDragRange = dp(180);
                        }
                        bottomSheetLayout.setTranslationY(maxDragRange);
                        isExpanded = false;
                        traceLog("api7_log", "底栏初始化完成，默认收起，高度=" + maxDragRange);
                    } catch (Throwable e) {
                        traceLog("api7_log", "初始化异常: " + e.getMessage());
                    }
                }
            });

            // --- 7. 加载逻辑 ---
            if (filePath != null && !filePath.equals("")) {
                if (filePath.startsWith("http")) {
                    titleView.setText("网络页面");
                    webView.loadUrl(filePath);
                    traceLog("api7_log", "加载网络: " + filePath);
                } else {
                    File file = new File(filePath);
                    setCleanTitle(file.getName());
                    if (file.getName().toLowerCase().endsWith(".zip")) {
                        traceLog("api7_log", "准备解压: " + filePath);
                        handleZipFile(file);
                    } else {
                        webView.loadUrl("file://" + filePath);
                        traceLog("api7_log", "加载文件: " + filePath);
                    }
                }
            } else {
                webView.loadData("<h3>无效地址</h3>", "text/html", "utf-8");
            }

        } catch (Throwable e) {
            traceLog("api7_log", "onCreate致命异常: " + e.getMessage());
            android.widget.Toast.makeText(this, "Init Error: " + e, 0).show();
        }
    }

    
    public int dp(float d) {
        return (int) (d * getResources().getDisplayMetrics().density);
    }
    
    public boolean isSystemDark() {
        return (getResources().getConfiguration().uiMode & 48) == 32;
    }
    
    public int getStatusBarHeight() {
        int r = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return r > 0 ? getResources().getDimensionPixelSize(r) : dp(25);
    }
    
    public void setCleanTitle(String filename) {
        int dot = filename.lastIndexOf(".");
        titleView.setText(dot > 0 ? filename.substring(0, dot) : filename);
    }

    private void initWebSettings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false); 
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        try {
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
        } catch(Exception e){
            traceLog("api7_log", "设置URL访问权限异常: " + e.getMessage());
        }

        webView.setWebViewClient(new WebViewClient() {
            public void onLoadResource(WebView view, String url) {
                try {
                    String lower = url.toLowerCase();
                    if (lower.matches(".*\\.(mp3|mp4|m3u8|avi|flv|mov|mkv).*") || 
                        lower.contains(".mp4?") || lower.contains("googlevideo")) {
                        if (!sniffedResources.contains(url)) {
                            sniffedResources.add(url);
                            traceLog("api7_log", "嗅探到资源: " + url);
                            if(snifferWindow != null && snifferWindow.isShowing()) {
                                runOnUiThread(new Runnable() { public void run() { snifferWindow.refreshList(); }});
                            }
                        }
                    }
                } catch (Throwable e) {
                    traceLog("api7_log", "嗅探异常: " + e.getMessage());
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                try {
                    if (newProgress == 100) centerSpinner.setVisibility(View.GONE);
                    else centerSpinner.setVisibility(View.VISIBLE);
                } catch (Throwable e) {
                    traceLog("api7_log", "进度更新异常: " + e.getMessage());
                }
            }
        });
    }
    
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        try {
            final WebView.HitTestResult result = webView.getHitTestResult();
            if (result == null) return;
            int type = result.getType();

            if (type == WebView.HitTestResult.IMAGE_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                menu.setHeaderTitle("图片选项");
                menu.add(0, 1, 0, "保存图片").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) { downloadFile(result.getExtra()); return true; }
                });
                menu.add(0, 2, 0, "复制链接").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) { copyToClipboard(result.getExtra()); return true; }
                });
            } else if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                final String url = result.getExtra();
                menu.setHeaderTitle("链接选项");
                menu.add(0, 3, 0, "系统播放器").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) { openInSystemPlayer(url); return true; }
                });
                menu.add(0, 4, 0, "复制链接").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) { copyToClipboard(url); return true; }
                });
            }
        } catch (Throwable e) {
            traceLog("api7_log", "上下文菜单异常: " + e.getMessage());
        }
    }

    
    public boolean onTouch(View v, MotionEvent event) {
        try {
            if (v != headerContainer) return false;
            
            final int action = event.getAction();
            
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    downTime = System.currentTimeMillis();
                    initialY = event.getRawY();
                    startTranslationY = bottomSheetLayout.getTranslationY();
                    isDragging = false;
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    if (downTime == 0) return true;
                    
                    float deltaY = event.getRawY() - initialY;
                    if (Math.abs(deltaY) > dp(5)) {
                        isDragging = true;
                        float targetY = startTranslationY + deltaY;
                        if (targetY < 0) targetY = 0;
                        if (targetY > maxDragRange) targetY = maxDragRange;
                        bottomSheetLayout.setTranslationY(targetY);
                    }
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (downTime == 0) return true;
                    
                    long duration = System.currentTimeMillis() - downTime;
                    
                    if (!isDragging && duration < TAP_TIMEOUT) {
                        toggleBottomSheet();
                    } else if (isDragging) {
                        float currentY = bottomSheetLayout.getTranslationY();
                        if (currentY > maxDragRange / 2) {
                            collapseBottomSheet();
                        } else {
                            expandBottomSheet();
                        }
                    }
                    
                    downTime = 0;
                    isDragging = false;
                    return true;
            }
            return false;
        } catch (Throwable e) {
            traceLog("api7_log", "触摸异常: " + e.getMessage());
            downTime = 0;
            isDragging = false;
            return false;
        }
    }

    private void toggleBottomSheet() {
        try {
            if (isExpanded) {
                collapseBottomSheet();
            } else {
                expandBottomSheet();
            }
        } catch (Throwable e) {
            traceLog("api7_log", "切换异常: " + e.getMessage());
        }
    }

    private void expandBottomSheet() {
        try {
            if (bottomSheetLayout == null) return;
            bottomSheetLayout.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(1.0f))
                .withEndAction(new Runnable() { public void run() { isExpanded = true; }})
                .start();
        } catch (Throwable e) {
            traceLog("api7_log", "展开动画异常: " + e.getMessage());
        }
    }

    private void collapseBottomSheet() {
        try {
            if (bottomSheetLayout == null) return;
            bottomSheetLayout.animate()
                .translationY(maxDragRange)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(new Runnable() { public void run() { isExpanded = false; }})
                .start();
        } catch (Throwable e) {
            traceLog("api7_log", "收起动画异常: " + e.getMessage());
        }
    }

    public void onClick(View v) {
        try {
            if (v == btnRefresh) { 
                webView.reload(); 
                Toast("已刷新"); 
                traceLog("api7_log", "点击刷新按钮");
            }
            else if (v == btnBack) { 
                if (webView.canGoBack()) webView.goBack(); 
                else Toast("已是第一页"); 
            }
            else if (v == btnForward) { 
                if (webView.canGoForward()) webView.goForward(); 
                else Toast("已是最后一页"); 
            }
            else if (v == btnZoomIn) webView.zoomIn();
            else if (v == btnZoomOut) webView.zoomOut();
            else if (v == btnSniff) {
                if (snifferWindow != null) {
                    if (sniffedResources.isEmpty()) {
                        Toast("未嗅探到资源");
                        traceLog("api7_log", "用户尝试打开嗅探窗口但资源为空");
                    } else {
                        snifferWindow.show();
                    }
                }
            }
            else if (v == btnTranslate) {
                toggleTranslation(); // 直接调用翻译切换
            }
            else if (v == btnClose) finish();
        } catch (Throwable e) {
            traceLog("api7_log", "点击处理异常: " + e.getMessage());
        }
    }

    private void openInSystemPlayer(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } catch(Exception ex) { Toast("调用失败"); }
        }
    }
    
    private void copyToClipboard(String text) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService("clipboard");
            cm.setText(text);
            Toast("已复制");
        } catch (Throwable e) {
            traceLog("api7_log", "复制异常: " + e.getMessage());
        }
    }

    private void downloadFile(final String url) {
        try {
            Toast("开始下载...");
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        String fileName = URLUtil.guessFileName(url, null, null);
                        File target = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName);
                        java.io.InputStream in = new java.net.URL(url).openStream();
                        FileOutputStream fos = new FileOutputStream(target);
                        byte[] buf = new byte[4096];
                        int len;
                        while((len=in.read(buf))>0) fos.write(buf,0,len);
                        in.close(); fos.close();
                        Toast("已保存");
                    } catch(Exception e) {
                        Toast("下载失败");
                    }
                }
            });
        } catch (Throwable e) {
            traceLog("api7_log", "下载启动异常: " + e.getMessage());
        }
    }

    private void handleZipFile(final File zipFile) {
        try {
            centerSpinner.setVisibility(View.VISIBLE);
            titleView.setText("解压中...");
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        String cacheDir = getCacheDir().getAbsolutePath() + "/web_unzip/" + zipFile.getName() + "_" + zipFile.lastModified();
                        File outputDir = new File(cacheDir);
                        if (!outputDir.exists()) {
                            outputDir.mkdirs();
                            unzipFile(zipFile.getAbsolutePath(), outputDir.getAbsolutePath(), new ProgressCallback() {
                                public void onProgress(int progressVal) {}
                                public void onProgressTip(String tip) {}
                            });
                        }
                        final File entry = findEntryFile(outputDir);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                centerSpinner.setVisibility(View.GONE);
                                if (entry != null) {
                                    setCleanTitle(entry.getName());
                                    webView.loadUrl("file://" + entry.getAbsolutePath());
                                } else {
                                    webView.loadData("<h3>未找到索引文件</h3>", "text/html", "utf-8");
                                }
                            }
                        });
                    } catch (final Exception e) {
                        runOnUiThread(new Runnable() { public void run() {
                            centerSpinner.setVisibility(View.GONE);
                            Toast("解压失败");
                        }});
                    }
                }
            });
        } catch (Throwable e) {
            traceLog("api7_log", "解压启动异常: " + e.getMessage());
        }
    }


    private File findEntryFile(File dir) {
        if (!dir.exists()) return null;
        File[] attempts = {new File(dir, "index.html"), new File(dir, "index.php"), new File(dir, "index.htm")};
        for (File f : attempts) if (f.exists()) return f;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) { File sub = findEntryFile(f); if (sub != null) return sub; }
                else if (f.getName().endsWith(".html") || f.getName().endsWith(".php")) return f;
            }
        }
        return null;
    }

    // 网格按钮共享布局：weight=1 等分、38dp 高、4dp 外边距（样式本体走 uitools.createButton）
    private void applyGridBtnParams(TextView btn) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38));
        params.weight = 1.0f;
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        btn.setLayoutParams(params);
    }

    class SnifferWindow {
        private LinearLayout layout, contentLayout, listContainer;
        private TextView titleTv;
        private ScrollView listScroll;
        private boolean isMinimized = true;
        private float dX, dY;
        private Activity act;

        public SnifferWindow(Activity activity) {
            this.act = activity; init();
        }

        private void init() {
            try {
                layout = new LinearLayout(act);
                layout.setOrientation(1);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(tca(act, "surface", 0xE6));
                bg.setCornerRadius(dp(12));
                bg.setStroke(1, tc(act, "outline"));
                layout.setBackground(bg);
                
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(140), -2);
                params.gravity = Gravity.TOP | Gravity.START;
                params.topMargin = dp(100); params.leftMargin = dp(20);
                layout.setLayoutParams(params);
                layout.setVisibility(View.GONE);

                LinearLayout header = new LinearLayout(act);
                header.setOrientation(0); header.setGravity(16);
                header.setPadding(dp(10), dp(8), dp(10), dp(8));
                titleTv = new TextView(act);
                titleTv.setText("资源 (0)"); titleTv.setTextSize(12);
                titleTv.setTextColor(tc(act, "on_surface"));
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
                titleTv.setLayoutParams(tp);
                
                TextView btnToggle = createButton(act, isMinimized ? "展开" : "收起", tc(act, "on_surface_variant"), Color.TRANSPARENT, 10f, 0, 0, 0, false, 0, 0, null);
                btnToggle.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(30)));
                TextView btnCls = createButton(act, "×", tc(act, "on_surface_variant"), Color.TRANSPARENT, 10f, 0, 0, 0, false, 0, 0, null);
                btnCls.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(30)));
                btnToggle.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { toggleMinimize(); }});
                btnCls.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { layout.setVisibility(View.GONE); }});
                
                header.addView(titleTv); header.addView(btnToggle); header.addView(btnCls);
                layout.addView(header);

                header.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            switch(event.getAction()) {
                                case MotionEvent.ACTION_DOWN: dX = layout.getX() - event.getRawX(); dY = layout.getY() - event.getRawY(); return true;
                                case MotionEvent.ACTION_MOVE: layout.animate().x(event.getRawX() + dX).y(event.getRawY() + dY).setDuration(0).start(); return true;
                            }
                        } catch (Throwable e) {
                            traceLog("api7_log", "拖拽异常: " + e.getMessage());
                        }
                        return false;
                    }
                });

                contentLayout = new LinearLayout(act);
                contentLayout.setOrientation(1);
                contentLayout.setVisibility(View.GONE);
                listScroll = new ScrollView(act);
                listContainer = new LinearLayout(act);
                listContainer.setOrientation(1);
                listScroll.addView(listContainer);
                listScroll.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(200)));
                contentLayout.addView(listScroll);
                layout.addView(contentLayout);
            } catch (Throwable e) {
                traceLog("api7_log", "初始化异常: " + e.getMessage());
            }
        }

        public View getView() { return layout; }

        public boolean isShowing() { return layout.getVisibility() == View.VISIBLE; }

        public void show() {
            try {
                if (sniffedResources == null || sniffedResources.isEmpty()) {
                    traceLog("api7_log", "嗅探资源为空，拒绝显示窗口");
                    Toast("未嗅探到任何资源");
                    return;
                }
                
                traceLog("api7_log", "显示嗅探窗口，资源数量: " + sniffedResources.size());
                layout.setVisibility(View.VISIBLE);
                refreshList();
                if(isMinimized) toggleMinimize();
            } catch (Throwable e) {
                traceLog("api7_log", "显示窗口异常: " + e.getMessage());
            }
        }

        public void refreshList() {
            try {
                titleTv.setText("资源 (" + sniffedResources.size() + ")");
                listContainer.removeAllViews();
                if (sniffedResources.isEmpty()) {
                    traceLog("api7_log", "刷新列表但资源为空");
                    return;
                }

                traceLog("api7_log", "刷新列表，资源数量: " + sniffedResources.size());
                for (final Object obj : sniffedResources) {
                    final String url = (String)obj;
                    LinearLayout item = new LinearLayout(act);
                    item.setOrientation(1);
                    item.setPadding(dp(8), dp(8), dp(8), dp(8));
                    
                    TextView urlTv = new TextView(act);
                    urlTv.setText(url); urlTv.setMaxLines(1);
                    urlTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    urlTv.setTextColor(tc(act, "on_surface_variant"));
                    urlTv.setTextSize(11);
                    item.addView(urlTv);
                    
                    LinearLayout acts = new LinearLayout(act);
                    TextView play = createButton(act, "播放", tc(act, "primary"), Color.TRANSPARENT, 10f, 0, 8, 8, false, 0, 0, null);
                    TextView dl = createButton(act, "下载", tc(act, "primary"), Color.TRANSPARENT, 10f, 0, 8, 8, false, 0, 0, null);
                    TextView copy = createButton(act, "复制", tc(act, "primary"), Color.TRANSPARENT, 10f, 0, 8, 8, false, 0, 0, null);
                    play.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openInSystemPlayer(url); }});
                    dl.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { downloadFile(url); }});
                    copy.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { copyToClipboard(url); }});
                    acts.addView(play); acts.addView(dl); acts.addView(copy);
                    item.addView(acts);
                    
                    View line = new View(act); line.setBackgroundColor(tc(act, "outline"));
                    line.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
                    
                    listContainer.addView(item);
                    listContainer.addView(line);
                }
            } catch (Throwable e) {
                traceLog("api7_log", "刷新列表异常: " + e.getMessage());
            }
        }
        
        private void toggleMinimize() {
            try {
                isMinimized = !isMinimized;
                contentLayout.setVisibility(isMinimized ? View.GONE : View.VISIBLE);
                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) layout.getLayoutParams();
                p.width = isMinimized ? dp(140) : dp(280);
                layout.setLayoutParams(p);
                traceLog("api7_log", "切换最小化状态: " + isMinimized);
            } catch (Throwable e) {
                traceLog("api7_log", "切换异常: " + e.getMessage());
            }
        }
    }

    private void toggleTranslation() {
        try {
            if (isTranslationActive) {
                String script = "javascript:(function(){" +
                    "var elem = document.getElementById('google_translate_element');" +
                    "var frame = document.querySelector('.goog-te-banner-frame');" +
                    "var script = document.querySelector('script[src*=\"translate.google.com\"]');" +
                    "if(elem) elem.remove();" +
                    "if(frame) frame.remove();" +
                    "if(script) script.remove();" +
                    "delete window.googleTranslateElementInit;" +
                "})();";
                webView.loadUrl(script);
                
                btnTranslate.setText("翻译");
                isTranslationActive = false;
                Toast("已退出翻译");
                traceLog("api7_log", "用户退出翻译");
            } else {
                String script = "javascript:(function(){" +
                    "var s=document.createElement('script');" +
                    "s.src='https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';" +
                    "document.body.appendChild(s);" +
                    "window.googleTranslateElementInit=function(){" +
                        "new google.translate.TranslateElement({pageLanguage:'auto', layout:0}, 'google_translate_element');" +
                        "var div=document.createElement('div');" +
                        "div.id='google_translate_element';" +
                        "div.style.cssText='position:fixed;top:0;left:0;width:100%;z-index:99999;';" +
                        "document.body.insertBefore(div, document.body.firstChild);" +
                    "};" +
                "})();";
                
                webView.loadUrl(script);
                
                btnTranslate.setText("关闭翻译");
                isTranslationActive = true;
                Toast("翻译已启用");
                traceLog("api7_log", "用户启用翻译");
            }
        } catch (Throwable e) {
            traceLog("api7_log", "翻译切换异常: " + e.getMessage());
            Toast("翻译操作失败");
        }
    }
}

try {
    registerActivity(HtmlPreviewActivity.class);
} catch (Throwable e) { traceLog("api7_log", "[toggleTranslation] 异常: " + e); }

void launchHtmlActivity(Activity activity, String filePath) {
    try {
        Intent intent = new Intent();
        intent.setClassName(activity, HtmlPreviewActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("filePath", filePath);
        activity.startActivity(intent);
    } catch (Exception e) {
        Toast("启动Activity失败: " + e.getMessage());
    }
}

private void startHtmlFilePicker(Activity activity) {
    String[] mimeTypes = {"text/html", "application/zip", "application/x-zip-compressed"};
    openFilePicker(activity, 1006, "*/*", mimeTypes, null, new FilePickerCallback() {
        public void onFilePicked(Activity a, Uri uri, String fileName, String filePath) {
            ThreadPool.execute(new Runnable() {
                public void run() { processSelectedHtmlFile(a, uri, fileName); }
            });
        }
    });
    Toast("请选择 HTML 或 ZIP 文件");
}


private void processSelectedHtmlFile(final Activity activity, final Uri uri, String fileName) {
    try {
        if (fileName == null || fileName.length() == 0) fileName = "unknown_file.html";
        File tempF = new File(fileName); fileName = tempF.getName();
        
        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".html") && !lowerName.endsWith(".zip") && !lowerName.endsWith(".php")) {
            final String name = fileName;
            Toast("不支持文件: " + name);
            return;
        }

        File htmlDir = new File(htmlPath);
        if (!htmlDir.exists()) htmlDir.mkdirs();
        File targetFile = new File(htmlPath, fileName);
        
        final String finalName = fileName;
        final Uri finalUri = uri;

        if (targetFile.exists()) {
            activity.runOnUiThread(new Runnable() {
                public void run() { showFileConflictDialog(activity, finalName, finalUri); }
            });
        } else {
            saveUriToFile(activity, uri, targetFile);
        }
    } catch (Exception e) {
        final String msg = e.getMessage();
        Toast("处理错误: " + msg);
    }
}

private void showFileConflictDialog(final Activity activity, final String fileName, final Uri uri) {
    boolean isDark = isThemeDark(activity);
    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity, isDark ? 4 : 3);
    builder.setTitle("文件冲突");
    builder.setMessage("文件 " + fileName + " 已存在");
    builder.setPositiveButton("覆盖", new android.content.DialogInterface.OnClickListener() {
        public void onClick(android.content.DialogInterface d, int w) {
            ThreadPool.execute(new Runnable() { public void run() { saveUriToFile(activity, uri, new File(htmlPath, fileName)); }});
        }
    });
    builder.setNeutralButton("导入并保留两个文件", new android.content.DialogInterface.OnClickListener() {
        public void onClick(android.content.DialogInterface d, int w) {
            ThreadPool.execute(new Runnable() { public void run() { 
                File newFile = getUniqueFile(new File(htmlPath, fileName));
                saveUriToFile(activity, uri, newFile); 
            }});
        }
    });
    builder.setNegativeButton("取消", null);
    android.app.AlertDialog conflictDialog = builder.create();
    applyUiTheme(activity, conflictDialog, 0);
    conflictDialog.show();
}

private File getUniqueFile(File file) {
    if (!file.exists()) return file;
    String name = file.getName();
    String base = name;
    String ext = "";
    int dot = name.lastIndexOf(".");
    if (dot > 0) { base = name.substring(0, dot); ext = name.substring(dot); }
    int i = 1;
    File newFile;
    do { newFile = new File(file.getParentFile(), base + "(" + i++ + ")" + ext); } while (newFile.exists());
    return newFile;
}

private void saveUriToFile(final Activity activity, Uri uri, File target) {
    try {
        java.io.InputStream is = activity.getContentResolver().openInputStream(uri);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(target);
        byte[] b = new byte[8192];
        int l;
        while ((l = is.read(b)) != -1) fos.write(b, 0, l);
        fos.flush(); is.close(); fos.close();
        
        final String path = target.getAbsolutePath();
        activity.runOnUiThread(new Runnable() { 
            public void run() { 
                Toast("✅ 导入成功");
                launchHtmlActivity(activity, path);
            }
        });
    } catch (Exception e) {
        final String err = e.getMessage();
        Toast("保存失败: " + err);
    }
}

private void showHtmlOptionDialog(final Activity activity) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                
                LinearLayout mainLayout = new LinearLayout(activity);
                mainLayout.setOrientation(1);
                mainLayout.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 12));
                
                TextView titleView = new TextView(activity);
                titleView.setText("HTML 浏览器");
                titleView.setTextColor(tc(activity, "on_surface"));
                titleView.setTextSize(18);
                titleView.setPadding(0, dp(activity, 8), 0, dp(activity, 24));
                mainLayout.addView(titleView);
                
                // URL 输入框
                final EditText urlInput = new EditText(activity);
                urlInput.setHint("输入网址或 HTML 代码 (回车打开)");
                urlInput.setSingleLine(true);
                urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
                urlInput.setBackground(null);
                urlInput.setPadding(0, dp(activity, 12), 0, dp(activity, 12));
                urlInput.setTextColor(tc(activity, "on_surface"));
                urlInput.setHintTextColor(tc(activity, "on_surface_variant"));
                
                View line = new View(activity);
                line.setBackgroundColor(tc(activity, "primary"));
                line.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(activity, 2)));
                mainLayout.addView(urlInput);
                mainLayout.addView(line);
                
                LinearLayout btnLayout = new LinearLayout(activity);
                btnLayout.setOrientation(1);
                btnLayout.setPadding(0, dp(activity, 20), 0, 0);
                
                // 按钮1：加载本地文件
                TextView btn1 = createButton(activity, "加载本地文件", tc(activity, "primary"), Color.TRANSPARENT, 14f, 0, 12, 16, false, 0, 0, null);
                btn1.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams btn1Params = new LinearLayout.LayoutParams(-1, -2);
                btn1Params.bottomMargin = dp(activity, 4);
                btnLayout.addView(btn1, btn1Params);

                // 按钮2：导入新文件
                TextView btn2 = createButton(activity, "导入新文件", tc(activity, "primary"), Color.TRANSPARENT, 14f, 0, 12, 16, false, 0, 0, null);
                btn2.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams btn2Params = new LinearLayout.LayoutParams(-1, -2);
                btn2Params.bottomMargin = dp(activity, 4);
                btnLayout.addView(btn2, btn2Params);

                // 取消按钮
                TextView btnCancel = createButton(activity, "取消", tc(activity, "on_surface"), Color.TRANSPARENT, 14f, 0, 12, 16, false, 0, 0, null);
                btnCancel.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, -2);
                cancelParams.topMargin = dp(activity, 8);
                btnLayout.addView(btnCancel, cancelParams);
                
                mainLayout.addView(btnLayout);
                
                final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
                dialog.requestWindowFeature(1);
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                dialog.setContentView(mainLayout);
                dialog.getWindow().setLayout(Math.min(dp(activity, 400), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 32)), -2);
                
                dialog.show();
                applyUiTheme(activity, dialog, 1);
                
                urlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                            String url = urlInput.getText().toString();
                            if(!url.startsWith("http") && !url.startsWith("file")) url = "http://" + url;
                            launchHtmlActivity(activity, url);
                            dialog.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
                
                btn1.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { dialog.dismiss(); showHtmlFileBrowser(activity); }
                });
                btn2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { dialog.dismiss(); startHtmlFilePicker(activity); }
                });
                btnCancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { dialog.dismiss(); }
                });
                
            } catch(Exception e) { 
                traceLog("api7_log", e.toString());
            }
        }
    });
}

private void showHtmlFileBrowser(final Activity activity) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);
                int dialogTheme = isDark ? 4 : 3;
                
                int textColor = tc(activity, "on_surface");
                
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(1);
                root.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 10));
                
                TextView title = new TextView(activity);
                title.setText("选择文件 (左滑删除)");
                title.setTextSize(18);
                title.setTextColor(textColor);
                title.setTypeface(null, Typeface.BOLD);
                title.setPadding(0, 0, 0, dp(activity, 16));
                root.addView(title);
                
                ScrollView scrollView = new ScrollView(activity);
                final LinearLayout list = new LinearLayout(activity);
                list.setOrientation(1);
                scrollView.addView(list);
                
                File dir = new File(htmlPath);
                if (!dir.exists()) dir.mkdirs();
                
                final Runnable refreshList = new Runnable() {
                    public void run() {
                        list.removeAllViews();
                        File[] files = new File(htmlPath).listFiles();
                        if (files != null) {
                            for (final File f : files) {
                                String name = f.getName().toLowerCase();
                                if (f.isFile() && (name.endsWith(".html") || name.endsWith(".zip") || name.endsWith(".php"))) {
                                    
                                    HorizontalScrollView slideView = new HorizontalScrollView(activity);
                                    slideView.setHorizontalScrollBarEnabled(false);
                                    slideView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                                    
                                    LinearLayout itemContainer = new LinearLayout(activity);
                                    itemContainer.setOrientation(0);
                                    
                                    LinearLayout content = new LinearLayout(activity);
                                    content.setOrientation(0);
                                    content.setGravity(16);
                                    content.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
                                    int contentWidth = activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 72);
                                    content.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, -2));
                                    
                                    content.setBackgroundColor(tc(activity, "surface"));
                                    
                                    TextView icon = new TextView(activity);
                                    icon.setText(name.endsWith(".zip") ? "📦" : "🌐");
                                    icon.setTextSize(20);
                                    icon.setPadding(0, 0, dp(activity, 12), 0);
                                    content.addView(icon);
                                    
                                    LinearLayout textLayout = new LinearLayout(activity);
                                    textLayout.setOrientation(1);
                                    TextView nameTv = new TextView(activity);
                                    nameTv.setText(f.getName());
                                    nameTv.setTextColor(textColor);
                                    nameTv.setTextSize(15);
                                    TextView sizeTv = new TextView(activity);
                                    sizeTv.setText(formatSize(f.length()));
                                    sizeTv.setTextColor(tc(activity, "on_surface_variant"));
                                    sizeTv.setTextSize(12);
                                    textLayout.addView(nameTv); textLayout.addView(sizeTv);
                                    content.addView(textLayout);
                                    
                                    TextView deleteBtn = createButton(activity, "删除", Color.WHITE, tc(activity, "error"), 14f, 0, 0, 0, false, 0, 0, null);
                                    deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 80), -1));
                                    
                                    itemContainer.addView(content);
                                    itemContainer.addView(deleteBtn);
                                    slideView.addView(itemContainer);
                                    
                                    LinearLayout itemWrapper = new LinearLayout(activity);
                                    itemWrapper.setOrientation(1);
                                    itemWrapper.addView(slideView);
                                    
                                    View itemDivider = new View(activity);
                                    itemDivider.setBackgroundColor(tc(activity, "outline"));
                                    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, 1);
                                    dividerParams.bottomMargin = dp(activity, 4); // dp间距分隔
                                    itemWrapper.addView(itemDivider, dividerParams);
                                    
                                    list.addView(itemWrapper);
                                    
                                    final String path = f.getAbsolutePath();
                                    final HorizontalScrollView finalSlide = slideView;
                                    
                                    content.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) { launchHtmlActivity(activity, path); }
                                    });
                                    
                                    deleteBtn.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            android.app.AlertDialog delDialog = new android.app.AlertDialog.Builder(activity, dialogTheme)
                                                .setTitle("确认删除")
                                                .setMessage("确定要删除吗？\n删除后就找不到咯～")
                                                .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                                                    public void onClick(android.content.DialogInterface d, int w) {
                                                        删除(path); 
                                                        finalSlide.setVisibility(View.GONE);
                                                        Toast("已删除");
                                                    }
                                                })
                                                .setNegativeButton("取消", new android.content.DialogInterface.OnClickListener() {
                                                    public void onClick(android.content.DialogInterface d, int w) {
                                                        finalSlide.fullScroll(View.FOCUS_LEFT);
                                                    }
                                                })
                                                .create();
                                            applyUiTheme(activity, delDialog, 0);
                                            delDialog.show();
                                        }
                                    });
                                }
                            }
                        }
                    }
                };
                refreshList.run();
                
                root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1.0f));
                
                final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
                dialog.requestWindowFeature(1);
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                dialog.setContentView(root);
                dialog.getWindow().setLayout(Math.min(dp(activity, 320), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 32)), dp(activity, 450));
                
                dialog.show();
                applyUiTheme(activity, dialog, 1);
                
            } catch(Exception e) { 
                Toast("Error: " + e); 
                traceLog("api7_log", e.toString());
            }
        }
    });
}
