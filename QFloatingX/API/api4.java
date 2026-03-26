// 悬浮窗生命周期状态常量
private static final int STATE_DESTROYED = 0; // 视图已销毁，内存释放，不可见不可交互
private static final int STATE_CREATED = 1; // 视图已实例化但未添加到WindowManager，不可见
private static final int STATE_VISIBLE = 2; // 视图已添加到WindowManager，可见且可交互
private static final int STATE_HIDDEN = 3; // 视图已从WindowManager移除但实例保留，不可见

// 关闭区域常量（可配置）
private static final int DEFAULT_CLOSE_RANGE_SIZE_DP = 80; // 关闭区域直径(dp)
private static final int DEFAULT_ICON_SIZE_DP = 60; // 图标默认大小(dp)
private static final int DEFAULT_CLOSE_ICON_SIZE_DP = 24; // 关闭图标默认大小(dp)
private static final int VIBRATION_INTERVAL_MS = 100; // 持续振动间隔

// 拖拽手势状态标志
private boolean isDragging = false; // 是否处于拖拽模式
private boolean isInCloseRange = false; // 悬浮窗中心是否进入关闭区域
private boolean isCloseRangeAttached = false; // 防止重复添加到WindowManager
private FrameLayout closeRangeView = null; // 关闭区域容器
private ImageView closeRangeIcon = null; // 关闭区域图标
private Bitmap cachedCloseIconBitmap = null; // 关闭图标缓存
private Vibrator vibrator = null; // 系统振动服务
private Runnable continuousVibrationRunnable = null; // 持续振动任务

// 触摸与动画状态
private final Handler xfcHandler = new Handler(Looper.getMainLooper()); // 主线程Handler
private int touchOffsetX = 0; // 触摸点X轴偏移
private int touchOffsetY = 0; // 触摸点Y轴偏移
private long touchStartTime = 0; // 触摸开始时间
private boolean isLongClickScheduled = false; // 是否已调度长按任务
private int currentAlpha = 255; // 当前透明度
private Runnable longClickRunnable = null; // 长按检测任务
private Runnable fadeRunnable = null; // 透明度渐变任务

// 悬浮窗核心状态字段
private int 悬浮窗状态 = STATE_DESTROYED; // 当前悬浮窗状态
private boolean 允许触摸 = false; // 是否允许触摸交互
private WindowManager wm = null; // 系统窗口管理器服务
private WindowManager.LayoutParams params = null; // 窗口布局参数
private FrameLayout floatingView = null; // 悬浮窗根视图
private ImageView iconImageView = null; // 图标显示视图
private Bitmap cachedIconBitmap = null; // 静态图标缓存
private String cachedGifPath = ""; // 动态图标路径缓存
private boolean isGifMode = false; // 当前是否为GIF模式
private Movie cachedGifMovie = null; // GIF动画数据缓存

private static class GifImageView extends ImageView {
    private Movie movie = null; // GIF Movie 对象
    private long movieStart = 0; // 动画起始时间
    private boolean isAnimating = false; // 是否正在动画
    private boolean lastSetMovieSuccess = false; // 记录最后一次 setMovie 是否成功（用于判断缓存是否可用）

    public GifImageView(Context context) {
        super(context);
        setWillNotDraw(false); // 强制允许自定义绘制
    }
    
    public void setMovie(Movie movie) {
        this.movie = movie;
        this.movieStart = 0;
        this.isAnimating = (movie != null);
        this.lastSetMovieSuccess = (movie != null);
        setWillNotDraw(false);
        invalidate();
    }
    
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (lastSetMovieSuccess && movie != null) {
            setMovie(movie);
        }
    }
    
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
    
    protected void onDraw(Canvas canvas) {
        try {
            if (movie != null && isAnimating) {
                long now = android.os.SystemClock.uptimeMillis();
                if (movieStart == 0) {
                    movieStart = now;
                }
                int dur = movie.duration();
                if (dur == 0) {
                    dur = 100;
                }
                int time = (int) ((now - movieStart) % dur);
                movie.setTime(time);
                canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
                movie.draw(canvas, 0, 0);
                invalidate();
            } else {
                super.onDraw(canvas);
            }
        } catch (Exception e) {
            super.onDraw(canvas);
        }
    }
    
    public void stopAnimation() {
        if (isAnimating) {
            isAnimating = false;
            invalidate();
        }
    }
}

/**
 * 获取安全的Activity实例
 * @return Activity 实例，优先返回当前Activity，否则返回最后保存的Activity
 */
private Activity 获取安全Activity() {
    Activity activity = getNowActivity();
    if (activity == null && 最后Activity != null) {
        activity = 最后Activity;
    }
    return activity;
}

/**
 * 获取悬浮窗大小（统一的尺寸）
 * 从"悬浮窗大小"设置读取
 * @param activity Activity实例
 * @return 悬浮窗大小像素值
 */
private int 获取悬浮窗大小(Activity activity) {
    if (activity == null) {
        return 180;
    }
    String value = getString("settings", "悬浮窗大小", String.valueOf(DEFAULT_ICON_SIZE_DP));
    try {
        if (value == null || value.trim().isEmpty()) {
            value = String.valueOf(DEFAULT_ICON_SIZE_DP);
        }
        double dpValue = Double.parseDouble(value.trim());
        if (dpValue <= 0) {
            dpValue = DEFAULT_ICON_SIZE_DP;
        }
        if (dpValue > 200) {
            dpValue = 200;
        }
        int pixelSize = (int) (dpValue * activity.getResources().getDisplayMetrics().density);
        return pixelSize > 0 ? pixelSize : 180;
    } catch (Exception e) {
        return (int) (DEFAULT_ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
    }
}

/**
 * 获取悬浮窗尺寸（WindowManager参数）
 * @param activity Activity实例
 * @return 悬浮窗尺寸像素值
 */
private int 获取悬浮窗尺寸(Activity activity) {
    return 获取悬浮窗大小(activity);
}

/**
 * 获取关闭区域大小
 * @param activity Activity实例
 * @return 关闭区域大小像素值
 */
private int 获取关闭区域大小(Activity activity) {
    if (activity == null) {
        return 240;
    }
    String value = getString("settings", "关闭区域图标大小", String.valueOf(DEFAULT_CLOSE_RANGE_SIZE_DP));
    try {
        if (value == null || value.trim().isEmpty()) {
            value = String.valueOf(DEFAULT_CLOSE_RANGE_SIZE_DP);
        }
        double dpValue = Double.parseDouble(value.trim());
        if (dpValue <= 0) {
            dpValue = DEFAULT_CLOSE_RANGE_SIZE_DP;
        }
        int pixelSize = (int) (dpValue * activity.getResources().getDisplayMetrics().density);
        return pixelSize > 0 ? pixelSize : 240;
    } catch (Exception e) {
        return (int) (DEFAULT_CLOSE_RANGE_SIZE_DP * activity.getResources().getDisplayMetrics().density);
    }
}

/**
 * 获取关闭图标大小
 * @param activity Activity实例
 * @return 关闭图标大小像素值
 */
private int 获取关闭图标大小(Activity activity) {
    if (activity == null) {
        return 72;
    }
    String value = getString("settings", "关闭区域图标大小", String.valueOf(DEFAULT_CLOSE_ICON_SIZE_DP));
    try {
        if (value == null || value.trim().isEmpty()) {
            value = String.valueOf(DEFAULT_CLOSE_ICON_SIZE_DP);
        }
        double dpValue = Double.parseDouble(value.trim());
        if (dpValue <= 0) {
            dpValue = DEFAULT_CLOSE_ICON_SIZE_DP;
        }
        int pixelSize = (int) (dpValue * activity.getResources().getDisplayMetrics().density);
        return pixelSize > 0 ? pixelSize : 72;
    } catch (Exception e) {
        return (int) (DEFAULT_CLOSE_ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
    }
}

/**
 * 获取移动阈值
 * @param activity Activity实例
 * @return 移动阈值像素值
 */
private float 获取移动阈值(Activity activity) {
    if (activity == null) {
        return 36.0f;
    }
    String value = getString("settings", "移动阈值", "12");
    try {
        if (value == null || value.trim().isEmpty()) value = "12";
        double dpValue = Double.parseDouble(value.trim());
        if (dpValue <= 0) dpValue = 12;
        return (float) (dpValue * activity.getResources().getDisplayMetrics().density);
    } catch (Exception e) {
        return 36.0f;
    }
}

/**
 * 获取长按关闭阈值
 * @return 长按关闭时间阈值（毫秒）
 */
private long 获取长按关闭阈值() {
    String value = getString("settings", "长按关闭阈值", "650");
    try {
        if (value == null || value.trim().isEmpty()) value = "650";
        long msValue = Long.parseLong(value.trim());
        return msValue > 0 ? msValue : 650;
    } catch (Exception e) {
        return 650;
    }
}

/**
 * 获取拖拽灵敏度
 * @return 拖拽灵敏度值
 */
private int 获取拖拽灵敏度() {
    String value = getString("settings", "拖拽灵敏度", "12");
    try {
        if (value == null || value.trim().isEmpty()) value = "12";
        int intValue = Integer.parseInt(value.trim());
        return intValue > 0 ? intValue : 12;
    } catch (Exception e) {
        return 12;
    }
}

/**
 * 获取静态图标路径
 * @return 图标文件路径字符串
 */
private String 获取静态图标路径() {
    String customPath = getString("settings", "iconPath", "");
    if (customPath != null && !customPath.isEmpty()) {
        java.io.File f = new java.io.File(customPath);
        if (f.exists()) {
            return customPath;
        }
    }
    String defaultPath = pluginPath + "/API/icon.png";
    java.io.File defaultFile = new java.io.File(defaultPath);
    if (defaultFile.exists()) {
        return defaultPath;
    }
    return "";
}

/**
 * 获取动态图标路径
 * @return GIF文件路径字符串
 */
private String 获取动态图标路径() {
    String customPath = getString("settings", "iconPath", "");
    if (customPath != null && !customPath.isEmpty()) {
        java.io.File f = new java.io.File(customPath);
        if (f.exists()) {
            return customPath;
        }
    }
    String defaultPath = pluginPath + "/API/icon.gif";
    java.io.File defaultFile = new java.io.File(defaultPath);
    if (defaultFile.exists()) {
        return defaultPath;
    }
    return "";
}

/**
 * 判断是否使用动态图标
 * @return true使用GIF，false使用静态图
 */
private boolean 是否使用动态图标() {
    String animPath = 获取动态图标路径();
    if (!animPath.isEmpty()) {
        String staticPath = 获取静态图标路径();
        java.io.File staticFile = new java.io.File(staticPath);
        java.io.File animFile = new java.io.File(animPath);
        if (animFile.exists()) {
            if (!staticFile.exists()) {
                return true;
            }
            return animFile.lastModified() > staticFile.lastModified();
        }
    }
    return false;
}

/**
 * 获取图标缩放比例
 * @return 缩放比例 float
 */
private float 获取图标缩放() {
    String value = getString("settings", "iconScale", "1.0");
    try {
        if (value == null || value.trim().isEmpty()) return 1.0f;
        float scale = Float.parseFloat(value.trim());
        if (scale < 0.5f) scale = 0.5f;
        if (scale > 2.0f) scale = 2.0f;
        return scale;
    } catch (Exception e) {
        return 1.0f;
    }
}

/**
 * 获取动态图标缩放比例
 * @return 缩放比例 float
 */
private float 获取动态图标缩放() {
    String value = getString("settings", "animIconScale", "1.0");
    try {
        if (value == null || value.trim().isEmpty()) return 1.0f;
        float scale = Float.parseFloat(value.trim());
        if (scale < 0.5f) scale = 0.5f;
        if (scale > 2.0f) scale = 2.0f;
        return scale;
    } catch (Exception e) {
        return 1.0f;
    }
}

/**
 * 获取图标透明度
 * @return 透明度值 0-255
 */
private int 获取图标透明度() {
    String value = getString("settings", "iconAlpha", "255");
    try {
        if (value == null || value.trim().isEmpty()) return 255;
        int alpha = Integer.parseInt(value.trim());
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return alpha;
    } catch (Exception e) {
        return 255;
    }
}

/**
 * 获取动态图标透明度
 * @return 透明度值 0-255
 */
private int 获取动态图标透明度() {
    String value = getString("settings", "animIconAlpha", "255");
    try {
        if (value == null || value.trim().isEmpty()) return 255;
        int alpha = Integer.parseInt(value.trim());
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return alpha;
    } catch (Exception e) {
        return 255;
    }
}

/**
 * 创建圆形背景Drawable
 * @param colorStr 颜色字符串
 * @return ShapeDrawable对象
 */
android.graphics.drawable.Drawable createCircleDrawable(String colorStr) {
    try {
        if (colorStr == null || colorStr.isEmpty()) {
            colorStr = "#4DFFFFFF";
        }
        int color = Color.parseColor(colorStr);
        android.graphics.drawable.ShapeDrawable drawable = new android.graphics.drawable.ShapeDrawable(
            new android.graphics.drawable.shapes.OvalShape()
        );
        drawable.getPaint().setColor(color);
        drawable.getPaint().setAntiAlias(true);
        return drawable;
    } catch (Exception e) {
        return null;
    }
}

/**
 * 创建关闭区域的Fallback图标（红色圆圈带X）
 * @param activity Activity实例
 * @param size 图标大小
 * @return Bitmap对象
 */
Bitmap 创建关闭区域Fallback图标(Activity activity, int size) {
    try {
        if (activity == null) {
            return null;
        }
        if (size <= 0) {
            size = (int) (DEFAULT_CLOSE_ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#F44336"));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size * 0.5f);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float centerX = size / 2;
        float centerY = size / 2 - (fontMetrics.ascent + fontMetrics.descent) / 2;
        canvas.drawText("X", centerX, centerY, paint);
        return bitmap;
    } catch (Exception e) {
        return null;
    }
}

/**
 * 加载图片文件，支持静态图和GIF
 * @param activity Activity实例
 * @param path 图片路径
 * @param isCloseIcon 是否为关闭图标
 * @return Bitmap对象（GIF返回null）
 */
Bitmap 加载图片文件(Activity activity, String path, boolean isCloseIcon) {
    try {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (path.toLowerCase().endsWith(".gif")) {
            cachedGifMovie = null;
            cachedIconBitmap = null;
            java.io.FileInputStream fis = new java.io.FileInputStream(path);
            cachedGifMovie = Movie.decodeStream(fis);
            fis.close();
            if (cachedGifMovie != null) {
                isGifMode = true;
                cachedGifPath = path;
                return null;
            } else {
                isGifMode = false;
                cachedGifPath = path;
            }
        } else {
            isGifMode = false;
            cachedGifPath = path;
            cachedGifMovie = null;
            Bitmap originalBitmap = BitmapFactory.decodeFile(path);
            if (originalBitmap != null) {
                if (activity != null) {
                    int targetSize;
                    if (isCloseIcon) {
                        targetSize = 获取关闭图标大小(activity);
                    } else {
                        targetSize = 获取悬浮窗大小(activity);
                    }
                    if (targetSize > 0 && (originalBitmap.getWidth() != targetSize || originalBitmap.getHeight() != targetSize)) {
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true);
                        originalBitmap.recycle();
                        return scaledBitmap;
                    }
                }
                return originalBitmap;
            }
        }
        return null;
    } catch (Exception e) {
        return null;
    }
}

/**
 * 创建默认图标（绿色圆圈带三横线）
 * @param activity Activity实例
 * @return Bitmap对象
 */
Bitmap 创建默认图标(Activity activity) {
    try {
        if (activity == null) {
            return null;
        }
        int size = 获取悬浮窗大小(activity);
        if (size <= 0) {
            size = (int) (DEFAULT_ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
        }
        if (size > 800) {
            size = 800;
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#4CAF50"));
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(size * 0.4f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("≡", size / 2, size / 2 + size * 0.12f, paint);
        return bitmap;
    } catch (OutOfMemoryError e) {
        return null;
    } catch (Exception e) {
        return null;
    }
}

/**
 * 加载图标Bitmap（根据设置自动选择静态或动态）
 * @param activity Activity实例
 * @return Bitmap对象
 */
Bitmap 加载图标Bitmap(Activity activity) {
    Bitmap bitmap = null;
    boolean useAnimIcon = 是否使用动态图标();
    String iconPath;
    if (useAnimIcon) {
        iconPath = 获取动态图标路径();
        if (!iconPath.isEmpty()) {
            bitmap = 加载图片文件(activity, iconPath, false);
        }
    } else {
        iconPath = 获取静态图标路径();
        if (!iconPath.isEmpty()) {
            bitmap = 加载图片文件(activity, iconPath, false);
        }
    }
    if (bitmap == null && !isGifMode) {
        bitmap = 创建默认图标(activity);
    }
    return bitmap;
}

/**
 * 设置图标图片（应用缩放和透明度）
 * @param activity Activity实例
 * @param imageView ImageView实例
 */
void 设置图标图片(Activity activity, ImageView imageView) {
    if (imageView == null || activity == null) {
        return;
    }
    int targetAlpha;
    if (isGifMode) {
        targetAlpha = 获取动态图标透明度();
    } else {
        targetAlpha = 获取图标透明度();
    }
    currentAlpha = targetAlpha;
    if (isGifMode) {
        if (!(imageView instanceof GifImageView)) {
            return;
        }
        if (cachedGifMovie != null) {
            ((GifImageView) imageView).setMovie(cachedGifMovie);
            imageView.setImageAlpha(currentAlpha);
        }
    } else {
        if (imageView instanceof GifImageView) {
            return;
        }
        if (cachedIconBitmap == null) {
            cachedIconBitmap = 加载图标Bitmap(activity);
        }
        if (cachedIconBitmap != null) {
            imageView.setImageBitmap(cachedIconBitmap);
            imageView.setImageAlpha(currentAlpha);
        }
    }
}

/**
 * 检查悬浮窗权限
 * @param activity Activity实例
 * @return 是否有权限
 */
boolean 检查悬浮窗权限(Activity activity) {
    if (activity == null) {
        return false;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            return Settings.canDrawOverlays(activity);
        } catch (Exception e) {
            return false;
        }
    }
    return true;
}

/**
 * 保存悬浮窗位置
 * @param activity Activity实例
 */
void 保存悬浮窗位置(Activity activity) {
    try {
        if (params == null || activity == null) {
            return;
        }
        SharedPreferences sp = activity.getSharedPreferences("floating_window_pos", Context.MODE_PRIVATE);
        if (sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("x", params.x);
            editor.putInt("y", params.y);
            editor.apply();
        }
    } catch (Exception e) {
    }
}

/**
 * 加载悬浮窗X坐标
 * @param activity Activity实例
 * @return X坐标
 */
int 加载悬浮窗位置(Activity activity) {
    try {
        if (activity == null) {
            return 0;
        }
        SharedPreferences sp = activity.getSharedPreferences("floating_window_pos", Context.MODE_PRIVATE);
        if (sp != null) {
            return sp.getInt("x", 0);
        }
        return 0;
    } catch (Exception e) {
        return 0;
    }
}

/**
 * 加载悬浮窗Y坐标
 * @param activity Activity实例
 * @return Y坐标
 */
int 加载悬浮窗Y位置(Activity activity) {
    try {
        if (activity == null) {
            return 300;
        }
        SharedPreferences sp = activity.getSharedPreferences("floating_window_pos", Context.MODE_PRIVATE);
        if (sp != null) {
            return sp.getInt("y", (int)(100 * activity.getResources().getDisplayMetrics().density));
        }
        int defaultY = (int)(100 * activity.getResources().getDisplayMetrics().density);
        return defaultY;
    } catch (Exception e) {
        int defaultY = (int)(100 * activity.getResources().getDisplayMetrics().density);
        return defaultY;
    }
}

/**
 * 创建关闭区域视图
 * @param activity Activity实例
 */
void 创建关闭区域视图(final Activity activity) {
    if (activity == null || closeRangeView != null) {
        return;
    }
    final int closeRangeSize = 获取关闭区域大小(activity);
    final int closeIconSize = 获取关闭图标大小(activity);
    if (closeRangeSize <= 0 || closeIconSize <= 0) {
        return;
    }
    closeRangeView = new FrameLayout(activity);
    FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(closeRangeSize, closeRangeSize);
    closeRangeView.setLayoutParams(containerParams);
    closeRangeView.setBackground(createCircleDrawable("#4DFFFFFF"));

    closeRangeIcon = new ImageView(activity);
    FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(closeIconSize, closeIconSize);
    iconParams.gravity = Gravity.CENTER;
    closeRangeIcon.setLayoutParams(iconParams);
    closeRangeIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);

    String closePath = (closeIconPath != null) ? closeIconPath : "";
    if (!closePath.isEmpty()) {
        cachedCloseIconBitmap = 加载图片文件(activity, closePath, true);
    }
    if (cachedCloseIconBitmap == null) {
        cachedCloseIconBitmap = 创建关闭区域Fallback图标(activity, closeIconSize);
    }
    if (cachedCloseIconBitmap != null) {
        closeRangeIcon.setImageBitmap(cachedCloseIconBitmap);
    }

    closeRangeView.addView(closeRangeIcon);
    closeRangeView.setVisibility(View.GONE);
}

/**
 * 显示关闭区域
 * @param activity Activity实例
 */
void 显示关闭区域(final Activity activity) {
    if (wm == null || params == null) {
        return;
    }
    if (closeRangeView == null) {
        创建关闭区域视图(activity);
    }
    if (closeRangeView == null) {
        return;
    }
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (isCloseRangeAttached && closeRangeView != null) {
                    try {
                        if (closeRangeView.isAttachedToWindow()) {
                            wm.removeView(closeRangeView);
                        }
                    } catch (Exception e) {
                    }
                }
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(dm);
                int screenHeight = dm.heightPixels;
                int closeRangeSize = 获取关闭区域大小(activity);
                WindowManager.LayoutParams closeParams = new WindowManager.LayoutParams(
                    closeRangeSize, closeRangeSize,
                    params.type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    android.graphics.PixelFormat.TRANSLUCENT
                );
                closeParams.gravity = Gravity.CENTER;
                closeParams.y = (int)(screenHeight * 0.45f);
                wm.addView(closeRangeView, closeParams);
                closeRangeView.setVisibility(View.VISIBLE);
                isCloseRangeAttached = true;
            } catch (Exception e) {
            }
        }
    });
}

/**
 * 隐藏关闭区域
 * @param activity Activity实例
 */
void 隐藏关闭区域(final Activity activity) {
    if (wm == null) {
        return;
    }
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (closeRangeView != null && closeRangeView.isAttachedToWindow()) {
                    wm.removeView(closeRangeView);
                }
                if (vibrator != null) {
                    vibrator.cancel();
                }
                xfcHandler.removeCallbacks(continuousVibrationRunnable);
                isInCloseRange = false;
                isCloseRangeAttached = false;
            } catch (Exception e) {
            }
        }
    });
}

/**
 * 更新关闭区域状态（检测拖拽进入）
 * @param activity Activity实例
 * @param floatX 悬浮窗X坐标
 * @param floatY 悬浮窗Y坐标
 */
void 更新关闭区域状态(Activity activity, int floatX, int floatY) {
    if (closeRangeView == null || !isDragging) {
        return;
    }
    try {
        int[] closeRangePos = new int[2];
        closeRangeView.getLocationOnScreen(closeRangePos);
        int closeCenterX = closeRangePos[0] + closeRangeView.getWidth() / 2;
        int closeCenterY = closeRangePos[1] + closeRangeView.getHeight() / 2;

        int iconSize = 获取悬浮窗大小(activity);
        int floatCenterX = floatX + iconSize / 2;
        int floatCenterY = floatY + iconSize / 2;

        float distance = (float) Math.sqrt(
            Math.pow(closeCenterX - floatCenterX, 2) + 
            Math.pow(closeCenterY - floatCenterY, 2)
        );
        
        boolean previouslyInRange = isInCloseRange;
        isInCloseRange = distance <= (closeRangeView.getWidth() / 2f);

        if (closeRangeIcon != null) {
            int brightness = isInCloseRange ? 255 : 180;
            closeRangeIcon.setImageAlpha(brightness);
        }

        if (isInCloseRange && !previouslyInRange) {
            启动持续振动(activity);
        } else if (!isInCloseRange && previouslyInRange) {
            停止持续振动();
        }
    } catch (Exception e) {
    }
}

/**
 * 启动持续振动
 * @param activity Activity实例
 */
void 启动持续振动(Activity activity) {
    if (activity == null) {
        return;
    }
    if (vibrator == null) {
        vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    }
    if (vibrator == null) {
        return;
    }
    if (continuousVibrationRunnable == null) {
        continuousVibrationRunnable = new Runnable() {
            public void run() {
                try {
                    if (isInCloseRange && isDragging) {
                        vibrate(activity, 12);
                        xfcHandler.postDelayed(this, VIBRATION_INTERVAL_MS);
                    }
                } catch (Exception e) {
                }
            }
        };
    }
    xfcHandler.post(continuousVibrationRunnable);
}

/**
 * 停止持续振动
 */
void 停止持续振动() {
    if (vibrator != null) {
        vibrator.cancel();
    }
    xfcHandler.removeCallbacks(continuousVibrationRunnable);
}

/**
 * 设置窗口参数
 * @param activity Activity实例
 */
void 设置窗口参数(Activity activity) {
    if (activity == null) {
        return;
    }
    int type;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
        type = WindowManager.LayoutParams.TYPE_PHONE;
    }
    int windowSize = 获取悬浮窗尺寸(activity);
    if (windowSize <= 0) {
        windowSize = (int) (DEFAULT_ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
    }
    params = new WindowManager.LayoutParams(
        windowSize, windowSize,
        type,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        android.graphics.PixelFormat.TRANSLUCENT
    );
    params.gravity = Gravity.TOP | Gravity.LEFT;
    params.x = 加载悬浮窗位置(activity);
    params.y = 加载悬浮窗Y位置(activity);
}

/**
 * 创建悬浮窗视图
 * @param activity Activity实例
 */
void 创建悬浮窗视图(final Activity activity) {
    try {
        if (activity == null) {
            return;
        }
        if (floatingView != null) {
            悬浮窗状态 = STATE_CREATED;
            return;
        }
        touchOffsetX = 0;
        touchOffsetY = 0;
        currentAlpha = 255;

        加载图标Bitmap(activity);

        int size = 获取悬浮窗大小(activity);
        if (size <= 0) {
            return;
        }

        floatingView = new FrameLayout(activity);
        FrameLayout.LayoutParams viewParams = new FrameLayout.LayoutParams(size, size);
        floatingView.setLayoutParams(viewParams);

        if (isGifMode && cachedGifMovie != null) {
            iconImageView = new GifImageView(activity);
        } else {
            iconImageView = new ImageView(activity);
        }

        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(size, size);
        iconImageView.setLayoutParams(iconParams);

        设置图标图片(activity, iconImageView);
        iconImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconImageView.setAdjustViewBounds(true);
        iconImageView.setImageAlpha(255);
        floatingView.addView(iconImageView);

        设置触摸事件(activity);

        悬浮窗状态 = STATE_CREATED;
    } catch (Exception e) {
        销毁悬浮窗资源();
    }
}

/**
 * 添加视图到WindowManager
 * @param activity Activity实例
 */
void 添加到窗口管理器(Activity activity) {
    try {
        if (floatingView == null || wm == null) {
            return;
        }
        if (floatingView.isAttachedToWindow()) {
            wm.removeView(floatingView);
        }
        设置窗口参数(activity);
        if (params == null) {
            return;
        }
        wm.addView(floatingView, params);
        if (iconImageView != null) {
            currentAlpha = 255;
            iconImageView.setImageAlpha(currentAlpha);
        }
        悬浮窗状态 = STATE_VISIBLE;
        悬浮窗显示状态 = true;
        允许触摸 = true;
    } catch (Exception e) {
        悬浮窗状态 = STATE_CREATED;
    }
}

/**
 * 销毁悬浮窗资源
 */
void 销毁悬浮窗资源() {
    try {
        xfcHandler.removeCallbacksAndMessages(null);
    } catch (Throwable e) {
    }
    try {
        if (floatingView != null && floatingView.isAttachedToWindow() && wm != null) {
            wm.removeView(floatingView);
        }
    } catch (Throwable e) {
    }
    try {
        if (cachedIconBitmap != null) {
            cachedIconBitmap.recycle();
            cachedIconBitmap = null;
        }
    } catch (Throwable e) {
    }
    try {
        cachedGifMovie = null;
        cachedGifPath = "";
        isGifMode = false;
    } catch (Throwable e) {
    }
    try {
        floatingView = null;
        iconImageView = null;
        悬浮窗状态 = STATE_DESTROYED;
        悬浮窗显示状态 = false;
        允许触摸 = false;
        isLongClickScheduled = false;
        isDragging = false;
        isInCloseRange = false;
    } catch (Throwable e) {
    }
}

/**
 * 设置触摸事件监听
 * @param activity Activity实例
 */
void 设置触摸事件(final Activity activity) {
    if (iconImageView == null) {
        return;
    }
    final float moveThreshold = 获取移动阈值(activity);
    final long longClickThreshold = 获取长按关闭阈值();

    if (fadeRunnable == null) {
        fadeRunnable = new Runnable() {
            public void run() {
                try {
                    if (isLongClickScheduled && currentAlpha > 100 && 悬浮窗显示状态 && 允许触摸 && iconImageView != null) {
                        currentAlpha -= 10;
                        iconImageView.setImageAlpha(currentAlpha);
                        if (isLongClickScheduled) {
                            xfcHandler.postDelayed(this, 30);
                        }
                    }
                } catch (Exception e) {
                }
            }
        };
    }

    if (longClickRunnable == null) {
        longClickRunnable = new Runnable() {
            public void run() {
                try {
                    isLongClickScheduled = false;
                    if (悬浮窗显示状态 && 允许触摸) {
                        long pressDuration = System.currentTimeMillis() - touchStartTime;
                        if (pressDuration >= longClickThreshold) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (悬浮窗显示状态) {
                                        Toast("长按关闭悬浮窗");
                                        停止悬浮窗(activity);
                                        putBoolean("settings", "开关", false);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                }
            }
        };
    }

    iconImageView.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (!允许触摸 || floatingView == null || !悬浮窗显示状态 || params == null || wm == null) {
                return false;
            }
            boolean handled = false;
            try {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDragging = false;
                        touchStartTime = System.currentTimeMillis();
                        touchOffsetX = (int) (event.getRawX() - params.x);
                        touchOffsetY = (int) (event.getRawY() - params.y);
                        currentAlpha = 255;
                        xfcHandler.removeCallbacks(fadeRunnable);
                        xfcHandler.postDelayed(fadeRunnable, 30);
                        if (!isLongClickScheduled) {
                            isLongClickScheduled = true;
                            xfcHandler.postDelayed(longClickRunnable, longClickThreshold);
                        }
                        handled = true;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (isLongClickScheduled) {
                            xfcHandler.removeCallbacks(longClickRunnable);
                            isLongClickScheduled = false;
                        }
                        xfcHandler.removeCallbacks(fadeRunnable);

                        int deltaX = Math.abs((int)(event.getRawX() - (params.x + touchOffsetX)));
                        int deltaY = Math.abs((int)(event.getRawY() - (params.y + touchOffsetY)));

                        if (!isDragging && (deltaX > moveThreshold || deltaY > moveThreshold)) {
                            isDragging = true;
                            显示关闭区域(activity);
                        }

                        if (isDragging) {
                            int sensitivity = 获取拖拽灵敏度();
                            int baseMoveX = (int) (event.getRawX() - touchOffsetX - params.x);
                            int baseMoveY = (int) (event.getRawY() - touchOffsetY - params.y);

                            int newX = params.x + (int)(baseMoveX * sensitivity / 12.0f);
                            int newY = params.y + (int)(baseMoveY * sensitivity / 12.0f);

                            int screenWidth = wm.getDefaultDisplay().getWidth();
                            int screenHeight = wm.getDefaultDisplay().getHeight();
                            int size = 获取悬浮窗大小(activity);

                            newX = Math.max(0, Math.min(newX, screenWidth - size));
                            newY = Math.max(0, Math.min(newY, screenHeight - size));

                            params.x = newX;
                            params.y = newY;
                            wm.updateViewLayout(floatingView, params);
                            保存悬浮窗位置(activity);

                            更新关闭区域状态(activity, newX, newY);
                        }

                        if (iconImageView != null) {
                            iconImageView.setImageAlpha(255);
                        }
                        handled = true;
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        xfcHandler.removeCallbacks(longClickRunnable);
                        xfcHandler.removeCallbacks(fadeRunnable);
                        isLongClickScheduled = false;

                        if (iconImageView != null) {
                            iconImageView.setImageAlpha(255);
                        }

                        if (isDragging) {
                            isDragging = false;

                            if (isInCloseRange) {
                                Toast("已关闭悬浮窗");
                                停止悬浮窗(activity);
                                putBoolean("settings", "开关", false);
                            }

                            隐藏关闭区域(activity);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            long pressDuration = System.currentTimeMillis() - touchStartTime;
                            int deltaXUp = Math.abs((int) event.getRawX() - (params.x + touchOffsetX));
                            int deltaYUp = Math.abs((int) event.getRawY() - (params.y + touchOffsetY));

                            if (pressDuration < longClickThreshold && 
                                deltaXUp < moveThreshold && deltaYUp < moveThreshold) {
                                处理图标点击();
                            }
                        }

                        handled = true;
                        break;
                }
            } catch (Exception e) {
                xfcHandler.removeCallbacks(longClickRunnable);
                xfcHandler.removeCallbacks(fadeRunnable);
                停止持续振动();
                isLongClickScheduled = false;
                isDragging = false;
                isInCloseRange = false;
                if (iconImageView != null) {
                    iconImageView.setImageAlpha(255);
                }
                隐藏关闭区域(activity);
            }
            return handled;
        }
    });
}

/**
 * 处理图标点击事件
 */
void 处理图标点击() {
    if (settingsMenuDialog != null && settingsMenuDialog.isShowing()) {
        return;
    }

    try {
        Activity activity = getNowActivity();
        if (activity == null && 最后Activity != null) {
            activity = 最后Activity;
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        final Activity finalActivity = activity;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (settingsMenuDialog != null && settingsMenuDialog.isShowing()) {
                        return;
                    }
                    vibrate(finalActivity, 48);
                    showSettingsMenu(finalActivity, null, null, null);
                } catch (Exception e) {
                    traceLog("icon_click_error", "菜单弹窗异常: " + e.getMessage());
                }
            }
        });
    } catch (Exception e) {
        traceLog("icon_click_error", "图标点击异常: " + e.getMessage());
    }
}

/**
 * 悬浮窗开关 - 切换悬浮窗显示状态
 * @param chatType 聊天类型
 * @param peerUin 对方Uin
 * @param name 名称
 */
public void 悬浮窗开关(int chatType, String peerUin, String name) {
    Activity activity = 获取安全Activity();
    if (activity == null) {
        return;
    }
    最后Activity = activity;
    final Activity finalActivity = activity;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean 开关状态 = !getBoolean("settings", "开关", false);
                putBoolean("settings", "开关", 开关状态);
                vibrate(finalActivity, 48);
                if (开关状态) {
                    启动悬浮窗(finalActivity);
                } else {
                    停止悬浮窗(finalActivity);
                }
            } catch (Exception e) {
            }
        }
    });
}

/**
 * 启动悬浮窗（有参版本）
 * @param activity Activity实例
 */
public void 启动悬浮窗(final Activity activity) {
    try {
        if (activity == null) {
            启动悬浮窗();
            return;
        }
        if (activity.isFinishing()) {
            return;
        }
        最后Activity = activity;

        if (wm == null) {
            wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        }
        if (wm == null) {
            return;
        }
        if (!检查悬浮窗权限(activity)) {
            Toast("请先授予悬浮窗权限");
            return;
        }

        switch (悬浮窗状态) {
            case STATE_DESTROYED:
                创建悬浮窗视图(activity);
                if (悬浮窗状态 == STATE_CREATED) {
                    添加到窗口管理器(activity);
                }
                break;
            case STATE_HIDDEN:
                currentAlpha = 255;
                if (iconImageView != null) {
                    iconImageView.setImageAlpha(currentAlpha);
                }
                添加到窗口管理器(activity);
                break;
            case STATE_CREATED:
                currentAlpha = 255;
                if (iconImageView != null) {
                    iconImageView.setImageAlpha(currentAlpha);
                }
                添加到窗口管理器(activity);
                break;
            case STATE_VISIBLE:
                if (iconImageView != null) {
                    iconImageView.setImageAlpha(255);
                }
                break;
            default:
                创建悬浮窗视图(activity);
                if (悬浮窗状态 == STATE_CREATED) {
                    添加到窗口管理器(activity);
                }
                break;
        }

        if (悬浮窗状态 == STATE_VISIBLE) {
            允许触摸 = true;
        }
    } catch (Exception e) {
    }
}

/**
 * 启动悬浮窗（无参版本）
 */
public void 启动悬浮窗() {
    Activity activity = getNowActivity();
    if (activity == null) {
        activity = 最后Activity;
    }
    if (activity != null && !activity.isFinishing()) {
        启动悬浮窗(activity);
    }
}

/**
 * 停止悬浮窗（有参版本）
 * @param activity Activity实例
 */
public void 停止悬浮窗(final Activity activity) {
    if (activity == null || activity.isFinishing()) {
        停止悬浮窗();
        return;
    }
    最后Activity = activity;

    xfcHandler.removeCallbacksAndMessages(null);
    isLongClickScheduled = false;
    isDragging = false;
    isInCloseRange = false;
    允许触摸 = false;

    停止持续振动();
    隐藏关闭区域(activity);
    保存悬浮窗位置(activity);

    if (悬浮窗状态 == STATE_VISIBLE || 悬浮窗状态 == STATE_CREATED) {
        if (floatingView != null && wm != null) {
            try {
                if (floatingView.isAttachedToWindow()) {
                    if (iconImageView instanceof GifImageView) {
                        ((GifImageView) iconImageView).stopAnimation();
                    }
                    wm.removeView(floatingView);
                }
            } catch (Exception e) {
            }
        }
        悬浮窗状态 = STATE_HIDDEN;
        悬浮窗显示状态 = false;
    }
}

/**
 * 停止悬浮窗（无参版本）
 */
public void 停止悬浮窗() {
    Activity activity = getNowActivity();
    if (activity == null) {
        activity = 最后Activity;
    }
    if (activity != null && !activity.isFinishing()) {
        停止悬浮窗(activity);
        return;
    }
    xfcHandler.removeCallbacksAndMessages(null);
    停止持续振动();
    isLongClickScheduled = false;
    isDragging = false;
    isInCloseRange = false;
    允许触摸 = false;
    if (悬浮窗状态 == STATE_VISIBLE || 悬浮窗状态 == STATE_CREATED) {
        try {
            if (floatingView != null && wm != null && floatingView.isAttachedToWindow()) {
                if (iconImageView instanceof GifImageView) {
                    ((GifImageView) iconImageView).stopAnimation();
                }
                wm.removeView(floatingView);
            }
        } catch (Exception e) {
        }
        悬浮窗状态 = STATE_HIDDEN;
    }
    悬浮窗显示状态 = false;
}

/**
 * 卸载悬浮窗 - 彻底清理所有资源
 */
public void 卸载悬浮窗() {
    停止悬浮窗();
    try {
        if (wm != null) {
            if (floatingView != null && floatingView.isAttachedToWindow()) {
                wm.removeView(floatingView);
            }
            if (closeRangeView != null && closeRangeView.isAttachedToWindow()) {
                wm.removeView(closeRangeView);
            }
        }
    } catch (Exception e) {
    }
    try {
        if (cachedIconBitmap != null) {
            cachedIconBitmap.recycle();
            cachedIconBitmap = null;
        }
        if (cachedCloseIconBitmap != null) {
            cachedCloseIconBitmap.recycle();
            cachedCloseIconBitmap = null;
        }
    } catch (Exception e) {
    }
    cachedGifMovie = null;
    cachedGifPath = "";
    isGifMode = false;
    floatingView = null;
    iconImageView = null;
    closeRangeView = null;
    closeRangeIcon = null;
    wm = null;
    params = null;
    悬浮窗状态 = STATE_DESTROYED;
    悬浮窗显示状态 = false;
    允许触摸 = false;
    isLongClickScheduled = false;
    isDragging = false;
    isInCloseRange = false;
    isCloseRangeAttached = false;
    currentAlpha = 255;
    touchOffsetX = 0;
    touchOffsetY = 0;
    touchStartTime = 0;
    xfcHandler.removeCallbacksAndMessages(null);
    longClickRunnable = null;
    fadeRunnable = null;
    continuousVibrationRunnable = null;
    if (vibrator != null) {
        try {
            vibrator.cancel();
        } catch (Exception e) {
        }
        vibrator = null;
    }
}

/**
 * 刷新悬浮窗图标
 */
public void 刷新悬浮窗() {
    final Activity activity = 获取安全Activity();
    if (activity == null || activity.isFinishing()) {
        return;
    }
    activity.runOnUiThread(new Runnable() {
         public void run() {
            try {
                if (wm == null) wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                if (wm == null) return;
                if (floatingView == null || params == null || 悬浮窗状态 != STATE_VISIBLE) {
                    启动悬浮窗(activity);
                    return;
                }
                try {
                    if (cachedIconBitmap != null) {
                        cachedIconBitmap.recycle();
                        cachedIconBitmap = null;
                    }
                } catch (Throwable ignore) {}
                cachedGifMovie = null;
                cachedGifPath = "";
                isGifMode = false;
                加载图标Bitmap(activity);

                boolean needRebuild =
                        (isGifMode && !(iconImageView instanceof GifImageView)) ||
                        (!isGifMode && (iconImageView instanceof GifImageView));

                if (needRebuild) {
                    重建悬浮窗图标视图(activity);
                    return;
                }
                设置图标图片(activity, iconImageView);
                currentAlpha = 255;
                iconImageView.setImageAlpha(currentAlpha);
            } catch (Throwable e) {
            }
        }
    });
}

/**
 * 重建悬浮窗图标视图（用于模式切换）
 * @param activity Activity实例
 */
private void 重建悬浮窗图标视图(Activity activity) {
    try {
        if (activity == null || floatingView == null) return;
        int size = 获取悬浮窗大小(activity);
        if (size <= 0) size = (int)(DEFAULT_ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
        try {
            if (iconImageView instanceof GifImageView) {
                ((GifImageView) iconImageView).stopAnimation();
            }
        } catch (Throwable ignore) {}
        try {
            if (iconImageView != null) {
                floatingView.removeView(iconImageView);
            }
        } catch (Throwable ignore) {}

        if (isGifMode && cachedGifMovie != null) {
            iconImageView = new GifImageView(activity);
        } else {
            iconImageView = new ImageView(activity);
        }

        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(size, size);
        iconImageView.setLayoutParams(iconParams);
        iconImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconImageView.setAdjustViewBounds(true);

        设置图标图片(activity, iconImageView);
        iconImageView.setImageAlpha(255);

        floatingView.addView(iconImageView);
        设置触摸事件(activity);
    } catch (Throwable e) {
    }
}
