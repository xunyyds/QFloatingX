/**
 * 颜色选择回调接口
 */
interface OnColorPickedListener {
    /**
     * 当用户选择颜色时回调
     * @param color 选中的颜色值
     */
    void onColorPicked(int color);
}

/**
 * 颜色变化回调接口
 */
interface OnColorChangedListener {
    /**
     * 当颜色值变化时回调
     * @param color 新的颜色值
     */
    void onColorChanged(int color);
}
/**
 * 从存储中加载收藏的颜色列表
 * @param activity 当前 Activity
 * @return 包含颜色字符串的 ArrayList
 */
ArrayList loadFavoriteColors(Activity activity) {
    ArrayList list = new ArrayList();
    try {
        String favStr = getString("settings", "color_favorites", "");
        if (favStr != null && !favStr.isEmpty()) {
            String[] colors = favStr.split(",");
            for (int i = 0; i < colors.length; i++) {
                String c = colors[i].trim();
                if (!c.isEmpty() && !list.contains(c)) {
                    list.add(c);
                }
            }
        }
    } catch (Exception e) {}
    return list;
}

/**
 * 保存收藏的颜色列表到存储
 * @param activity 当前 Activity
 * @param favorites 包含颜色字符串的 ArrayList
 */
void saveFavoriteColors(Activity activity, ArrayList favorites) {
    try {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(favorites.size(), 30);
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append(favorites.get(i).toString());
        }
        putString("settings", "color_favorites", sb.toString());
    } catch (Exception e) {}
}


/**
 * SeekBar 包装器类
 */
class SeekBar extends LinearLayout {
    private SeekBar seekBar; // 滑块控件
    private TextView valueLabel; // 数值显示标签
    private OnColorChangedListener listener; // 颜色变化监听器
    private int index; // 索引

    /**
     * 构造函数
     * @param context 上下文
     * @param label 标签文本
     * @param color 主色调
     * @param initialValue 初始值
     * @param max 最大值
     */
    public SeekBar(Context context, String label, int color, int initialValue, int max) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(0, dp(context, 8), 0, dp(context, 8));
        
        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setTextColor(color);
        labelView.setTypeface(null, Typeface.BOLD);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 32), -2));
        addView(labelView);
        
        LinearLayout seekContainer = new LinearLayout(context);
        seekContainer.setOrientation(LinearLayout.VERTICAL);
        seekContainer.setGravity(Gravity.CENTER_VERTICAL);
        seekContainer.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        seekContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        
        seekBar = new SeekBar(context);
        seekBar.setMax(max);
        seekBar.setProgress(initialValue);
        seekBar.setPadding(dp(context, 4), dp(context, 8), dp(context, 4), dp(context, 8));
        
        try {
            if (Build.VERSION.SDK_INT >= 16) {
                seekBar.getProgressDrawable().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                seekBar.getThumb().setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        } catch (Exception e) {}
        
        seekContainer.addView(seekBar);
        addView(seekContainer);
        
        valueLabel = new TextView(context);
        valueLabel.setText(String.valueOf(initialValue));
        valueLabel.setTextSize(14);
        valueLabel.setTextColor(Color.parseColor("#FF666666"));
        valueLabel.setTypeface(Typeface.MONOSPACE);
        valueLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 40), -2));
        valueLabel.setGravity(Gravity.RIGHT);
        addView(valueLabel);
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueLabel.setText(String.valueOf(progress));
                if (listener != null) {
                    listener.onColorChanged(progress);
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    /** 获取当前进度值 */
    public int getProgress() {
        return seekBar.getProgress();
    }
    
    /** 设置颜色变化监听器 */
    public void setOnColorChangedListener(OnColorChangedListener l) {
        listener = l;
    }
}


/**
 * HSV (色相, 饱和度, 亮度) 选择器视图类
 */
class HsvPickerView extends View {
    private Paint huePaint; // 色相条画笔
    private Paint svPaint; // SV 平面画笔
    private Paint cursorPaint; // 光标画笔
    private Paint borderPaint; // 边框画笔
    
    private int currentHue = 0; // 当前色相
    private float currentSat = 1.0f; // 当前饱和度
    private float currentVal = 1.0f; // 当前明度
    
    private OnColorChangedListener listener; // 颜色变化监听器
    private boolean trackingSV = false; // 是否正在跟踪 SV 操作
    private boolean trackingHue = false; // 是否正在跟踪 Hue 操作
    
    private int svLeft, svTop, svRight, svBottom; // SV 区域边界
    private int hueLeft, hueTop, hueRight, hueBottom; // Hue 区域边界
    private int hueWidth = 40; // 色相条宽度
    
    private boolean isSizeValid = false; // 尺寸是否有效
    private Shader hueShader; // 色相渐变着色器

    /**
     * 构造函数
     * @param context 上下文
     * @param initialColor 初始颜色
     */
    public HsvPickerView(Context context, int initialColor) {
        super(context);
        setWillNotDraw(false);
        
        huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        svPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(4);
        cursorPaint.setColor(Color.WHITE);
        try {
            cursorPaint.setShadowLayer(2, 0, 0, Color.BLACK);
        } catch (Exception e) {}
        
        borderPaint.setColor(Color.parseColor("#40FFFFFF"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1);
        
        setColor(initialColor);
    }
    
    /** 设置当前颜色 */
    public void setColor(int color) {
        try {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            currentHue = (int) hsv[0];
            currentSat = hsv[1];
            currentVal = hsv[2];
            invalidate();
        } catch (Exception e) {}
    }
    
    /** 设置颜色变化监听器 */
    public void setOnColorChangedListener(OnColorChangedListener l) {
        listener = l;
    }
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w, h, oldw, oldh);
	    
	    try {
	        // 安全检查：防止宽高为0
	        if (w <= 0 || h <= 0) {
	            isSizeValid = false;
	            return;
	        }
	        
	        int padding = dp(getContext(), 8);
	        int gap = dp(getContext(), 16);
	        
	        hueWidth = dp(getContext(), 36);
	        hueLeft = w - padding - hueWidth;
	        hueTop = padding;
	        hueRight = w - padding;
	        hueBottom = h - padding;
	        
	        if (hueRight <= hueLeft || hueBottom <= hueTop || hueWidth <= 0) {
	            isSizeValid = false;
	            return;
	        }
	        
	        svLeft = padding;
	        svTop = padding;
	        svRight = hueLeft - gap;
	        svBottom = h - padding;
	        
	        if (svRight <= svLeft || svBottom <= svTop) {
	            isSizeValid = false;
	            return;
	        }
	        
	        // 使用Color.parseColor(#AARRGGBB)
	        int[] hueColors = new int[7];
	        hueColors[0] = Color.parseColor("#FFFF0000"); // 红
	        hueColors[1] = Color.parseColor("#FFFFFF00"); // 黄
	        hueColors[2] = Color.parseColor("#FF00FF00"); // 绿
	        hueColors[3] = Color.parseColor("#FF00FFFF"); // 青
	        hueColors[4] = Color.parseColor("#FF0000FF"); // 蓝
	        hueColors[5] = Color.parseColor("#FFFF00FF"); // 洋红
	        hueColors[6] = Color.parseColor("#FFFF0000"); // 红（闭合）
	        
	        float[] positions = null;
	        
	        float x0 = 0.0f;
	        float y0 = (float)hueTop;
	        float x1 = 0.0f;
	        float y1 = (float)hueBottom;
	        
	        // 使用Object接收构造结果
	        Object shaderObj = new LinearGradient(
	            x0, 
	            y0, 
	            x1, 
	            y1, 
	            hueColors,      // 显式int[]类型变量
	            positions,      // 显式float[]类型变量（非null字面量）
	            Shader.TileMode.CLAMP
	        );
	        
	        hueShader = (android.graphics.Shader)shaderObj;
	        huePaint.setShader(hueShader);
	        
	        isSizeValid = true;
	        traceLog("hsv_picker", "[onSizeChanged] 渐变创建成功");
	        
	    } catch (Throwable e) {
	        traceLog("hsv_picker", "[onSizeChanged] 错误: " + e.getMessage());
	        isSizeValid = false;
	    }
	}
	
        protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!isSizeValid) return;
        
        try {
            // 绘制SV平面背景 (白色到当前色相)
            int[] svColors = new int[]{
                Color.WHITE,
                Color.HSVToColor(new float[]{currentHue, 1.0f, 1.0f})
            };
            Shader svShader = new LinearGradient(svLeft, svTop, svRight, svTop, svColors, null, Shader.TileMode.CLAMP);
            svPaint.setShader(svShader);
            canvas.drawRect(svLeft, svTop, svRight, svBottom, svPaint);
            
            // 叠加明度渐变 (从上到下：透明到黑色)
            Shader valShader = new LinearGradient(svLeft, svTop, svLeft, svBottom, 
                Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
            svPaint.setShader(valShader);
            canvas.drawRect(svLeft, svTop, svRight, svBottom, svPaint);
            
            // 绘制SV平面边框
            canvas.drawRect(svLeft, svTop, svRight, svBottom, borderPaint);
            
            // 绘制色相条 (右侧)
            canvas.drawRect(hueLeft, hueTop, hueRight, hueBottom, huePaint);
            canvas.drawRect(hueLeft, hueTop, hueRight, hueBottom, borderPaint);
            
            // 绘制SV选择器光标 (空心圆+中心点)
            float svX = svLeft + (svRight - svLeft) * currentSat;
            float svY = svTop + (svBottom - svTop) * (1 - currentVal);
            
            // 确保光标在区域内
            svX = Math.max(svLeft + 10, Math.min(svRight - 10, svX));
            svY = Math.max(svTop + 10, Math.min(svBottom - 10, svY));
            
            // 外圈 (黑色描边)
            cursorPaint.setColor(Color.BLACK);
            cursorPaint.setStrokeWidth(3);
            canvas.drawCircle(svX, svY, 10, cursorPaint);
            // 内圈 (白色)
            cursorPaint.setColor(Color.WHITE);
            cursorPaint.setStrokeWidth(2);
            canvas.drawCircle(svX, svY, 8, cursorPaint);
            
            // 绘制色相选择器 (右侧横线)
            float hueY = hueTop + (hueBottom - hueTop) * (currentHue / 360.0f);
            hueY = Math.max(hueTop + 5, Math.min(hueBottom - 5, hueY));
            
            Paint hueCursorPaint = new Paint();
            hueCursorPaint.setColor(Color.WHITE);
            hueCursorPaint.setStrokeWidth(4);
            try {
                hueCursorPaint.setShadowLayer(3, 0, 0, Color.BLACK);
            } catch (Exception e) {}
            canvas.drawLine(hueLeft - 6, hueY, hueRight + 6, hueY, hueCursorPaint);
            
        } catch (Exception e) {}
    }
        public boolean onTouchEvent(MotionEvent event) {
        if (!isSizeValid) return false;
        
        try {
            float x = event.getX();
            float y = event.getY();
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (x >= svLeft && x <= svRight && y >= svTop && y <= svBottom) {
                        trackingSV = true;
                        updateSV(x, y);
                        return true;
                    } else if (x >= hueLeft && x <= hueRight && y >= hueTop && y <= hueBottom) {
                        trackingHue = true;
                        updateHue(y);
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (trackingSV) {
                        updateSV(x, y);
                        return true;
                    } else if (trackingHue) {
                        updateHue(y);
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    trackingSV = false;
                    trackingHue = false;
                    break;
            }
        } catch (Exception e) {
            trackingSV = false;
            trackingHue = false;
        }
        return super.onTouchEvent(event);
    }
    
    /** 更新饱和度和明度 */
    private void updateSV(float x, float y) {
        try {
            currentSat = Math.max(0, Math.min(1, (x - svLeft) / (svRight - svLeft)));
            currentVal = Math.max(0, Math.min(1, 1 - (y - svTop) / (svBottom - svTop)));
            if (listener != null) {
                listener.onColorChanged(Color.HSVToColor(new float[]{currentHue, currentSat, currentVal}));
            }
            invalidate();
        } catch (Exception e) {}
    }
    
    /** 更新色相 */
    private void updateHue(float y) {
        try {
            currentHue = (int) (Math.max(0, Math.min(1, (y - hueTop) / (hueBottom - hueTop))) * 360);
            if (listener != null) {
                listener.onColorChanged(Color.HSVToColor(new float[]{currentHue, currentSat, currentVal}));
            }
            invalidate();
        } catch (Exception e) {}
    }
}


/**
 * 色轮视图类
 */
class ColorWheelView extends View {
    private Paint wheelPaint; // 色轮画笔
    private Paint centerPaint; // 中心画笔
    private Paint cursorPaint; // 光标画笔
    private int centerX, centerY, radius; // 圆心坐标和半径
    private float cursorX, cursorY; // 光标坐标
    private int currentHue = 0; // 当前色相
    private float currentSat = 1.0f; // 当前饱和度
    private OnColorChangedListener listener; // 颜色变化监听器
    private Bitmap cacheBitmap; // 缓存位图
    private boolean isSizeValid = false; // 尺寸是否有效
    
    /**
     * 构造函数
     * @param context 上下文
     * @param initialColor 初始颜色
     */
    public ColorWheelView(Context context, int initialColor) {
        super(context);
        setWillNotDraw(false);
        
        wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(3);
        cursorPaint.setColor(Color.WHITE);
        try {
            cursorPaint.setShadowLayer(2, 0, 0, Color.BLACK);
        } catch (Exception e) {}
        
        setColor(initialColor);
    }
    
    /** 设置当前颜色 */
    public void setColor(int color) {
        try {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            currentHue = (int) hsv[0];
            currentSat = hsv[1];
            updateCursorPosition();
            invalidate();
        } catch (Exception e) {}
    }
    
    /** 设置颜色变化监听器 */
    public void setOnColorChangedListener(OnColorChangedListener l) {
        listener = l;
    }
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // 安全检查：防止宽高为0
        if (w <= 0 || h <= 0) {
            isSizeValid = false;
            return;
        }
        
        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(w, h) / 2 - dp(getContext(), 16);
        
        // 安全检查：半径必须大于0才能创建位图
        if (radius <= 0) {
            isSizeValid = false;
            return;
        }
        
        updateCursorPosition();
        isSizeValid = true;
        
        // 预渲染色轮背景
        try {
            if (cacheBitmap != null && !cacheBitmap.isRecycled()) {
                cacheBitmap.recycle();
            }
            cacheBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas cacheCanvas = new Canvas(cacheBitmap);
            
            // 绘制色轮 (扫描渐变)
            Shader shader = new SweepGradient(centerX, centerY, 
                new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED},
                null);
            wheelPaint.setShader(shader);
            cacheCanvas.drawCircle(centerX, centerY, radius, wheelPaint);
            
            // 叠加饱和度渐变 (从中心白色到边缘透明)
            Shader satShader = new RadialGradient(centerX, centerY, radius, 
                Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP);
            centerPaint.setShader(satShader);
            cacheCanvas.drawCircle(centerX, centerY, radius, centerPaint);
            
        } catch (Exception e) {
            // 如果创建失败，清理资源
            if (cacheBitmap != null && !cacheBitmap.isRecycled()) {
                cacheBitmap.recycle();
            }
            cacheBitmap = null;
        }
    }
    
    /** 更新光标位置 */
    private void updateCursorPosition() {
        try {
            float angle = (float) Math.toRadians(currentHue);
            float r = currentSat * radius;
            cursorX = centerX + (float) Math.cos(angle) * r;
            cursorY = centerY + (float) Math.sin(angle) * r;
        } catch (Exception e) {}
    }
        protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!isSizeValid) return;
        
        try {
            // 绘制缓存的色轮
            if (cacheBitmap != null && !cacheBitmap.isRecycled()) {
                canvas.drawBitmap(cacheBitmap, 0, 0, null);
            }
            
            // 确保光标在有效范围内
            float dx = cursorX - centerX;
            float dy = cursorY - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > radius) {
                dx = dx / dist * radius;
                dy = dy / dist * radius;
                cursorX = centerX + dx;
                cursorY = centerY + dy;
            }
            
            // 绘制光标 (双圈)
            Paint shadowPaint = new Paint();
            shadowPaint.setColor(Color.BLACK);
            shadowPaint.setStyle(Paint.Style.STROKE);
            shadowPaint.setStrokeWidth(4);
            canvas.drawCircle(cursorX, cursorY, 10, shadowPaint);
            
            cursorPaint.setColor(Color.WHITE);
            cursorPaint.setStrokeWidth(2);
            canvas.drawCircle(cursorX, cursorY, 8, cursorPaint);
            
        } catch (Exception e) {}
    }
        public boolean onTouchEvent(MotionEvent event) {
        if (!isSizeValid) return false;
        
        try {
            float x = event.getX();
            float y = event.getY();
            
            float dx = x - centerX;
            float dy = y - centerY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (dist > radius) {
                dx = dx / dist * radius;
                dy = dy / dist * radius;
                dist = radius;
            }
            
            cursorX = centerX + dx;
            cursorY = centerY + dy;
            
            currentSat = dist / radius;
            currentHue = (int) Math.toDegrees(Math.atan2(dy, dx));
            if (currentHue < 0) currentHue += 360;
            
            if (listener != null) {
                listener.onColorChanged(Color.HSVToColor(new float[]{currentHue, currentSat, 1.0f}));
            }
            
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


/**
 * 颜色条视图 (垂直，右侧，修复版)
 */
class ColorBarView extends View {
    private Paint paint; // 主画笔
    private Paint cursorPaint; // 光标画笔
    private Paint borderPaint; // 边框画笔
    private int currentColor; // 当前颜色
    private int[] colors; // 颜色数组
    private OnColorChangedListener listener; // 颜色变化监听器
    private int barLeft, barTop, barRight, barBottom; // 条形边界
    private boolean isSizeValid = false; // 尺寸是否有效
    
    /**
     * 构造函数
     * @param context 上下文
     * @param initialColor 初始颜色
     */
    public ColorBarView(Context context, int initialColor) {
        super(context);
        setWillNotDraw(false);
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setStrokeWidth(4);
        cursorPaint.setColor(Color.WHITE);
        try {
            cursorPaint.setShadowLayer(3, 0, 0, Color.BLACK);
        } catch (Exception e) {}
        
        borderPaint.setColor(Color.parseColor("#40FFFFFF"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1);
        
        colors = new int[]{
            Color.parseColor("#FFFF0000"),
            Color.parseColor("#FFFFFF00"),
            Color.parseColor("#FF00FF00"),
            Color.parseColor("#FF00FFFF"),
            Color.parseColor("#FF0000FF"),
            Color.parseColor("#FFFF00FF"),
            Color.parseColor("#FFFF0000")
        };
        
        setColor(initialColor);
    }
    
    /** 设置当前颜色 */
    public void setColor(int color) {
        currentColor = color;
        invalidate();
    }
    
    /** 设置颜色变化监听器 */
    public void setOnColorChangedListener(OnColorChangedListener l) {
        listener = l;
    }
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // 安全检查：防止宽高为0
        if (w <= 0 || h <= 0) {
            isSizeValid = false;
            return;
        }
        
        int padding = dp(getContext(), 8);
        barLeft = padding;
        barTop = padding;
        barRight = w - padding;
        barBottom = h - padding;
        
        // 安全检查：防止 RectF 坐标无效
        if (barLeft >= barRight || barTop >= barBottom) {
            isSizeValid = false;
            return;
        }
        
        isSizeValid = true;
    }
        protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!isSizeValid) return;
        
        try {
            // 创建Shader (仅在尺寸有效时)
            Shader shader = new LinearGradient(barLeft, barTop, barLeft, barBottom, colors, null, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            
            RectF rect = new RectF(barLeft, barTop, barRight, barBottom);
            canvas.drawRoundRect(rect, 8, 8, paint);
            canvas.drawRoundRect(rect, 8, 8, borderPaint); // 绘制边框
            
            // 绘制光标
            float[] hsv = new float[3];
            Color.colorToHSV(currentColor, hsv);
            float y = barTop + (barBottom - barTop) * (hsv[0] / 360.0f);
            y = Math.max(barTop + 2, Math.min(barBottom - 2, y));
            
            canvas.drawLine(barLeft - 4, y, barRight + 4, y, cursorPaint);
            
        } catch (Exception e) {}
    }
        public boolean onTouchEvent(MotionEvent event) {
        if (!isSizeValid) return false;
        
        try {
            float y = event.getY();
            
            if (y < barTop) y = barTop;
            if (y > barBottom) y = barBottom;
            
            float hue = (y - barTop) / (barBottom - barTop) * 360;
            currentColor = Color.HSVToColor(new float[]{hue, 1.0f, 1.0f});
            
            if (listener != null) {
                listener.onColorChanged(currentColor);
            }
            
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


/**
 * 放大镜视图
 */
class MagnifierView extends View {
    private Paint paint; // 主画笔
    private Paint borderPaint; // 边框画笔
    private Bitmap magnifiedBitmap; // 放大后的位图
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public MagnifierView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        borderPaint.setColor(Color.WHITE);
    }
    
    /**
     * 更新放大镜内容
     * @param source 源位图
     * @param srcX 源X坐标
     * @param srcY 源Y坐标
     * @param currentColor 当前颜色
     */
    public void update(Bitmap source, int srcX, int srcY, int currentColor) {
        try {
            int size = 30; // 放大尺寸
            int scale = 4; // 缩放倍数
            
            // 安全检查：防止 source 为 null
            if (source == null || source.isRecycled() || source.getWidth() <= 0 || source.getHeight() <= 0) {
                return;
            }
            
            if (magnifiedBitmap != null && !magnifiedBitmap.isRecycled()) {
                magnifiedBitmap.recycle();
            }
            
            // 确保尺寸有效
            if (size <= 0 || scale <= 0) return;
            
            magnifiedBitmap = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(magnifiedBitmap);
            
            Rect srcRect = new Rect(
                Math.max(0, srcX - size),
                Math.max(0, srcY - size),
                Math.min(source.getWidth(), srcX + size),
                Math.min(source.getHeight(), srcY + size)
            );
            
            // 确保源矩形有效
            if (srcRect.width() <= 0 || srcRect.height() <= 0) {
                return;
            }
            
            canvas.save();
            canvas.scale(scale, scale);
            canvas.drawBitmap(source, srcRect, new RectF(0, 0, size * 2, size * 2), paint);
            canvas.restore();
            
            Paint crossPaint = new Paint();
            crossPaint.setColor(Color.WHITE);
            crossPaint.setStrokeWidth(2);
            int center = size * scale;
            canvas.drawLine(center, 0, center, size * 2 * scale, crossPaint);
            canvas.drawLine(0, center, size * 2 * scale, center, crossPaint);
            
            invalidate();
        } catch (Exception e) {}
    }
        protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            if (magnifiedBitmap != null && !magnifiedBitmap.isRecycled()) {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;
                
                float radius = Math.min(w, h) / 2;
                
                canvas.save();
                Path path = new Path();
                path.addCircle(w/2, h/2, radius, Path.Direction.CCW);
                canvas.clipPath(path);
                
                canvas.drawBitmap(magnifiedBitmap, null, new RectF(0, 0, w, h), paint);
                canvas.restore();
                
                canvas.drawCircle(w/2, h/2, radius, borderPaint);
            }
        } catch (Exception e) {}
    }
}


/**
 * 创建 RGB 调色视图
 * @param activity 当前 Activity
 * @param initialColor 初始颜色
 * @param listener 颜色变化监听器
 * @return 创建的视图
 */
View createRgbView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dp(activity, 16), dp(activity, 24), dp(activity, 16), dp(activity, 24));
    layout.setGravity(Gravity.CENTER_VERTICAL);
    
    final int[] rgb = {Color.red(initialColor), Color.green(initialColor), Color.blue(initialColor)};
    
    // R
    final SeekBar redBar = new SeekBar(activity, "R", Color.parseColor("#FFFF3B30"), rgb[0], 255);
    redBar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int progress) {
            rgb[0] = progress;
            if (listener != null) listener.onColorChanged(Color.rgb(rgb[0], rgb[1], rgb[2]));
        }
    });
    layout.addView(redBar);
    
    // G
    final SeekBar greenBar = new SeekBar(activity, "G", Color.parseColor("#FF34C759"), rgb[1], 255);
    greenBar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int progress) {
            rgb[1] = progress;
            if (listener != null) listener.onColorChanged(Color.rgb(rgb[0], rgb[1], rgb[2]));
        }
    });
    layout.addView(greenBar);
    
    // B
    final SeekBar blueBar = new SeekBar(activity, "B", Color.parseColor("#FF007AFF"), rgb[2], 255);
    blueBar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int progress) {
            rgb[2] = progress;
            if (listener != null) listener.onColorChanged(Color.rgb(rgb[0], rgb[1], rgb[2]));
        }
    });
    layout.addView(blueBar);
    
    return layout;
}

/**
 * 创建 HSV 调色视图
 * @param activity 当前 Activity
 * @param initialColor 初始颜色
 * @param listener 颜色变化监听器
 * @return 创建的视图
 */
View createHsvView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    FrameLayout layout = new FrameLayout(activity);
    layout.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
    
    HsvPickerView hsvView = new HsvPickerView(activity, initialColor);
    hsvView.setOnColorChangedListener(listener);
    layout.addView(hsvView, new FrameLayout.LayoutParams(-1, -1));
    
    return layout;
}

/**
 * 创建色轮调色视图
 * @param activity 当前 Activity
 * @param initialColor 初始颜色
 * @param listener 颜色变化监听器
 * @return 创建的视图
 */
View createColorWheelView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    FrameLayout layout = new FrameLayout(activity);
    layout.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
    
    ColorWheelView wheelView = new ColorWheelView(activity, initialColor);
    wheelView.setOnColorChangedListener(listener);
    layout.addView(wheelView, new FrameLayout.LayoutParams(-1, -1));
    
    return layout;
}

/**
 * 创建颜色条调色视图
 * @param activity 当前 Activity
 * @param initialColor 初始颜色
 * @param listener 颜色变化监听器
 * @return 创建的视图
 */
View createColorBarView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.HORIZONTAL);
    layout.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
    
    // 左侧预览 (大色块)
    final View preview = new View(activity);
    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(activity, 80), -1);
    previewParams.rightMargin = dp(activity, 16);
    preview.setLayoutParams(previewParams);
    preview.setBackgroundColor(initialColor);
    
    GradientDrawable previewBg = new GradientDrawable();
    previewBg.setColor(initialColor);
    previewBg.setCornerRadius(dp(activity, 12));
    preview.setBackgroundDrawable(previewBg);
    
    layout.addView(preview);
    
    // 右侧颜色条
    ColorBarView bar = new ColorBarView(activity, initialColor);
    bar.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 48), -1));
    layout.addView(bar);
    
    bar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int color) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(color);
            bg.setCornerRadius(dp(activity, 12));
            preview.setBackgroundDrawable(bg);
            if (listener != null) listener.onColorChanged(color);
        }
    });
    
    return layout;
}


/**
 * 显示收藏夹视图
 * @param activity 当前 Activity
 * @param container 容器
 * @param favorites 收藏的颜色列表
 * @param listener 颜色选择监听器
 */
void showFavoritesView(Activity activity, FrameLayout container, ArrayList favorites, final OnColorPickedListener listener) {
    container.removeAllViews();
    
    ScrollView scroll = new ScrollView(activity);
    scroll.setVerticalScrollBarEnabled(false);
    
    LinearLayout list = new LinearLayout(activity);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
    
    if (favorites.isEmpty()) {
        TextView empty = new TextView(activity);
        empty.setText("暂无收藏颜色\n点击 + 添加当前颜色");
        empty.setTextSize(14);
        empty.setTextColor(Color.GRAY);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dp(activity, 40), 0, 0);
        list.addView(empty);
    } else {
        for (int i = 0; i < favorites.size(); i++) {
            final String colorStr = favorites.get(i).toString();
            
            // 每项容器
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
            item.setBackground(createSelectableBackground());
            
            // 颜色方块
            View colorBlock = new View(activity);
            LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
            colorBlock.setLayoutParams(blockParams);
            
            try {
                int color = Color.parseColor(colorStr);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(color);
                bg.setCornerRadius(dp(activity, 8));
                // 添加边框
                bg.setStroke(2, Color.parseColor("#20FFFFFF"));
                colorBlock.setBackgroundDrawable(bg);
            } catch (Exception e) {
                colorBlock.setBackgroundColor(Color.GRAY);
            }
            item.addView(colorBlock);
            
            // 颜色代码文本
            TextView codeText = new TextView(activity);
            codeText.setText(colorStr.toUpperCase());
            codeText.setTextSize(16);
            codeText.setTypeface(Typeface.MONOSPACE);
            codeText.setTextColor(Color.WHITE);
            codeText.setPadding(dp(activity, 16), 0, 0, 0);
            codeText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            item.addView(codeText);
            
            // 删除按钮
            TextView deleteBtn = new TextView(activity);
            deleteBtn.setText("×");
            deleteBtn.setTextSize(24);
            deleteBtn.setTextColor(Color.parseColor("#FF999999"));
            deleteBtn.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
            item.addView(deleteBtn);
            
            // 点击选择
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        int color = Color.parseColor(colorStr);
                        if (listener != null) listener.onColorPicked(color);
                    } catch (Exception e) {}
                }
            });
            
            // 点击删除
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    favorites.remove(colorStr);
                    saveFavoriteColors(activity, favorites);
                    item.setVisibility(View.GONE);
                    Toast("已删除");
                }
            });
            
            list.addView(item);
            
            // 分隔线
            if (i < favorites.size() - 1) {
                View line = new View(activity);
                line.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
                line.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
                list.addView(line);
            }
        }
    }
    
    scroll.addView(list);
    container.addView(scroll);
}


/**
 * 显示图片取色对话框
 * @param activity 当前 Activity
 * @param callback 颜色选择回调
 */
void showImagePickerDialog(final Activity activity, final OnColorPickedListener callback) {
    try {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(Color.parseColor("#FF000000"));
        
        String imgPath = null;
        try {
            if (pluginPath != null) {
                imgPath = pluginPath + "/API/background.png";
            }
        } catch (Exception e) {}
        
        if (imgPath == null) {
            Toast("图片路径错误");
            return;
        }
        
        File imgFile = new File(imgPath);
        if (!imgFile.exists()) {
            Toast("图片不存在");
            return;
        }
        
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 2;
        final Bitmap bitmap = BitmapFactory.decodeFile(imgPath, opts);
        
        if (bitmap == null) {
            Toast("无法加载图片");
            return;
        }
        
        // 图片视图 (限制最大高度)
        final ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageBitmap(bitmap);
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(-1, dp(activity, 400));
        imgParams.gravity = Gravity.CENTER;
        root.addView(imageView, imgParams);
        
        // 放大镜
        final MagnifierView magnifier = new MagnifierView(activity);
        magnifier.setVisibility(View.GONE);
        root.addView(magnifier, new FrameLayout.LayoutParams(dp(activity, 120), dp(activity, 120)));
        
        // 底部颜色预览栏
        LinearLayout bottomBar = new LinearLayout(activity);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(Color.parseColor("#FF2D2D2D"));
        bottomBar.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, -2);
        barParams.gravity = Gravity.BOTTOM;
        bottomBar.setLayoutParams(barParams);
        
        final View colorPreview = new View(activity);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48));
        colorPreview.setLayoutParams(previewParams);
        colorPreview.setBackgroundColor(Color.WHITE);
        bottomBar.addView(colorPreview);
        
        final TextView hexText = new TextView(activity);
        hexText.setText("#FFFFFF");
        hexText.setTextSize(18);
        hexText.setTypeface(Typeface.MONOSPACE);
        hexText.setTextColor(Color.WHITE);
        hexText.setPadding(dp(activity, 16), 0, 0, 0);
        hexText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        bottomBar.addView(hexText);
        
        TextView confirmBtn = new TextView(activity);
        confirmBtn.setText("确定");
        confirmBtn.setTextSize(16);
        confirmBtn.setTextColor(Color.parseColor("#FF7A9681"));
        confirmBtn.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        bottomBar.addView(confirmBtn);
        
        root.addView(bottomBar);
        
        // 取消按钮 (左上角)
        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(24);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(-2, -2);
        closeParams.gravity = Gravity.TOP | Gravity.RIGHT;
        closeBtn.setLayoutParams(closeParams);
        root.addView(closeBtn);
        
        imageView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (bitmap == null) return false;
                
                try {
                    Matrix matrix = imageView.getImageMatrix();
                    RectF drawableRect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    matrix.mapRect(drawableRect);
                    
                    float x = event.getX();
                    float y = event.getY();
                    
                    if (!drawableRect.contains(x, y)) {
                        magnifier.setVisibility(View.GONE);
                        return true;
                    }
                    
                    int bitmapX = (int) ((x - drawableRect.left) / drawableRect.width() * bitmap.getWidth());
                    int bitmapY = (int) ((y - drawableRect.top) / drawableRect.height() * bitmap.getHeight());
                    
                    bitmapX = Math.max(0, Math.min(bitmap.getWidth() - 1, bitmapX));
                    bitmapY = Math.max(0, Math.min(bitmap.getHeight() - 1, bitmapY));
                    
                    final int color = bitmap.getPixel(bitmapX, bitmapY);
                    
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                            magnifier.setVisibility(View.VISIBLE);
                            
                            int magX = (int) x - dp(activity, 60);
                            int magY = (int) y - dp(activity, 140);
                            if (magX < 0) magX = 0;
                            if (magY < 0) magY = 0;
                            magnifier.setX(magX);
                            magnifier.setY(magY);
                            magnifier.update(bitmap, bitmapX, bitmapY, color);
                            
                            colorPreview.setBackgroundColor(color);
                            hexText.setText(colorToHex(color));
                            return true;
                            
                        case MotionEvent.ACTION_UP:
                            return true;
                    }
                } catch (Exception e) {}
                return true;
            }
        });
        
        builder.setView(root);
        final AlertDialog dialog = builder.create();
        
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    int color = Color.parseColor(hexText.getText().toString());
                    if (callback != null) callback.onColorPicked(color);
                } catch (Exception e) {}
                dialog.dismiss();
            }
        });
        
        dialog.show();
        
    } catch (Exception e) {}
}


/**
 * 显示主调色盘对话框
 * @param activity 当前 Activity
 * @param initialColor 初始颜色
 * @param callback 颜色选择回调
 */
void showColorPickerDialog(final Activity activity, final String initialColor, final OnColorPickedListener callback) {
    try {
        final int[] currentColor = {Color.parseColor("#FF7A9681")};
        try {
            if (initialColor != null && !initialColor.isEmpty()) {
                currentColor[0] = Color.parseColor(initialColor);
            }
        } catch (Exception e) {}
        
        final ArrayList favoriteColors = loadFavoriteColors(activity);
        final boolean[] showingFavorites = {false};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        
        // 根布局
        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#FF2D2D2D"));
        root.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
        
        // 顶部标题栏
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(activity);
        title.setText("颜色");
        title.setTextSize(20);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        header.addView(title);
        
        // 收藏夹按钮
        final TextView favBtn = new TextView(activity);
        favBtn.setText("收藏夹");
        favBtn.setTextSize(14);
        favBtn.setTextColor(Color.WHITE);
        favBtn.setBackground(createRoundRectDrawable(Color.parseColor("#FF7A9681"), dp(activity, 16)));
        favBtn.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        header.addView(favBtn);
        
        // 取色按钮
        final TextView pickBtn = new TextView(activity);
        pickBtn.setText("取色");
        pickBtn.setTextSize(14);
        pickBtn.setTextColor(Color.WHITE);
        pickBtn.setBackground(createRoundRectDrawable(Color.parseColor("#FF5A5A5A"), dp(activity, 16)));
        pickBtn.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(-2, -2);
        pickParams.leftMargin = dp(activity, 12);
        pickBtn.setLayoutParams(pickParams);
        header.addView(pickBtn);
        
        root.addView(header);
        
        // 颜色预览
        LinearLayout previewRow = new LinearLayout(activity);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        previewRow.setPadding(0, dp(activity, 16), 0, dp(activity, 16));
        
        final View colorPreview = new View(activity);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(activity, 56), dp(activity, 56));
        previewParams.rightMargin = dp(activity, 16);
        colorPreview.setLayoutParams(previewParams);
        
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(currentColor[0]);
        previewBg.setCornerRadius(dp(activity, 12));
        previewBg.setStroke(2, Color.parseColor("#40FFFFFF"));
        colorPreview.setBackgroundDrawable(previewBg);
        previewRow.addView(colorPreview);
        
        final TextView hexText = new TextView(activity);
        hexText.setText(colorToHex(currentColor[0]));
        hexText.setTextSize(16);
        hexText.setTextColor(Color.WHITE);
        hexText.setTypeface(Typeface.MONOSPACE);
        hexText.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
        hexText.setBackground(createRoundRectDrawable(Color.parseColor("#FF3D3D3D"), dp(activity, 8)));
        hexText.setClickable(true);
        hexText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                copyToClipboard(activity, hexText.getText().toString());
                Toast("颜色代码已复制");
            }
        });
        previewRow.addView(hexText);
        
        final TextView addFavBtn = new TextView(activity);
        addFavBtn.setText("+");
        addFavBtn.setTextSize(28);
        addFavBtn.setTextColor(Color.parseColor("#FF7A9681"));
        addFavBtn.setPadding(dp(activity, 16), 0, dp(activity, 8), 0);
        previewRow.addView(addFavBtn);
        
        root.addView(previewRow);
        
        // 模式标签栏
        LinearLayout tabRow = new LinearLayout(activity);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setBackground(createRoundRectDrawable(Color.parseColor("#FF1E1E1E"), dp(activity, 8)));
        tabRow.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
        
        final String[] modes = {"RGB", "HSV", "色轮", "颜色条"};
        final TextView[] tabViews = new TextView[4];
        final int[] currentMode = {1};
        
        for (int i = 0; i < 4; i++) {
            final int mode = i;
            TextView tab = new TextView(activity);
            tab.setText(modes[i]);
            tab.setTextSize(14);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            
            if (i == currentMode[0]) {
                tab.setTextColor(Color.parseColor("#FF2D2D2D"));
                tab.setTypeface(null, Typeface.BOLD);
                tab.setBackground(createRoundRectDrawable(Color.parseColor("#FF7A9681"), dp(activity, 6)));
            } else {
                tab.setTextColor(Color.GRAY);
                tab.setBackground(null);
            }
            
            final int index = i;
            tab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentMode[0] = index;
                    for (int j = 0; j < 4; j++) {
                        if (j == index) {
                            tabViews[j].setTextColor(Color.parseColor("#FF2D2D2D"));
                            tabViews[j].setTypeface(null, Typeface.BOLD);
                            tabViews[j].setBackground(createRoundRectDrawable(Color.parseColor("#FF7A9681"), dp(activity, 6)));
                        } else {
                            tabViews[j].setTextColor(Color.GRAY);
                            tabViews[j].setTypeface(null, Typeface.NORMAL);
                            tabViews[j].setBackground(null);
                        }
                    }
                    showModeContent(activity, contentContainer, index, currentColor[0], new OnColorChangedListener() {
                        public void onColorChanged(int color) {
                            currentColor[0] = color;
                            updatePreview(colorPreview, hexText, color);
                        }
                    });
                }
            });
            
            tabViews[i] = tab;
            tabRow.addView(tab);
        }
        
        root.addView(tabRow);
        
        // 内容容器 - 固定高度确保显示完整
        final FrameLayout contentContainer = new FrameLayout(activity);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, dp(activity, 280));
        contentParams.topMargin = dp(activity, 16);
        contentParams.bottomMargin = dp(activity, 16);
        contentContainer.setLayoutParams(contentParams);
        contentContainer.setBackground(createRoundRectDrawable(Color.parseColor("#FF1E1E1E"), dp(activity, 12)));
        root.addView(contentContainer);
        
        // 初始化
        showModeContent(activity, contentContainer, 1, currentColor[0], new OnColorChangedListener() {
            public void onColorChanged(int color) {
                currentColor[0] = color;
                updatePreview(colorPreview, hexText, color);
            }
        });
        
        // 透明度滑块
        LinearLayout alphaRow = new LinearLayout(activity);
        alphaRow.setOrientation(LinearLayout.HORIZONTAL);
        alphaRow.setGravity(Gravity.CENTER_VERTICAL);
        alphaRow.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        
        TextView alphaLabel = new TextView(activity);
        alphaLabel.setText("透明度");
        alphaLabel.setTextSize(14);
        alphaLabel.setTextColor(Color.parseColor("#FFAAAAAA"));
        alphaLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 60), -2));
        alphaRow.addView(alphaLabel);
        
        final SeekBar alphaSeek = new SeekBar(activity);
        alphaSeek.setMax(255);
        alphaSeek.setProgress(Color.alpha(currentColor[0]));
        alphaSeek.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        alphaRow.addView(alphaSeek);
        
        final TextView alphaValue = new TextView(activity);
        alphaValue.setText(String.valueOf(Color.alpha(currentColor[0])));
        alphaValue.setTextSize(14);
        alphaValue.setTextColor(Color.WHITE);
        alphaValue.setTypeface(Typeface.MONOSPACE);
        alphaValue.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 40), -2));
        alphaValue.setGravity(Gravity.RIGHT);
        alphaRow.addView(alphaValue);
        
        root.addView(alphaRow);
        
        alphaSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentColor[0] = Color.argb(progress, 
                        Color.red(currentColor[0]), 
                        Color.green(currentColor[0]), 
                        Color.blue(currentColor[0]));
                    updatePreview(colorPreview, hexText, currentColor[0]);
                    alphaValue.setText(String.valueOf(progress));
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 底部按钮
        LinearLayout buttonRow = new LinearLayout(activity);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        buttonRow.setPadding(0, dp(activity, 8), 0, 0);
        
        TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("取消");
        cancelBtn.setTextSize(16);
        cancelBtn.setTextColor(Color.parseColor("#FF7A9681"));
        cancelBtn.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12));
        buttonRow.addView(cancelBtn);
        
        TextView confirmBtn = new TextView(activity);
        confirmBtn.setText("确定");
        confirmBtn.setTextSize(16);
        confirmBtn.setTypeface(null, Typeface.BOLD);
        confirmBtn.setTextColor(Color.parseColor("#FF7A9681"));
        confirmBtn.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12));
        buttonRow.addView(confirmBtn);
        
        root.addView(buttonRow);
        
        builder.setView(root);
        final AlertDialog dialog = builder.create();
        
        // 按钮事件
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (callback != null) {
                    callback.onColorPicked(currentColor[0]);
                }
                dialog.dismiss();
            }
        });
        
        favBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showingFavorites[0] = !showingFavorites[0];
                if (showingFavorites[0]) {
                    favBtn.setText("返回");
                    favBtn.setBackground(createRoundRectDrawable(Color.parseColor("#FF5A5A5A"), dp(activity, 16)));
                    showFavoritesView(activity, contentContainer, favoriteColors, new OnColorPickedListener() {
                        public void onColorPicked(int color) {
                            currentColor[0] = color;
                            updatePreview(colorPreview, hexText, color);
                            alphaSeek.setProgress(Color.alpha(color));
                            alphaValue.setText(String.valueOf(Color.alpha(color)));
                        }
                    });
                } else {
                    favBtn.setText("收藏夹");
                    favBtn.setBackground(createRoundRectDrawable(Color.parseColor("#FF7A9681"), dp(activity, 16)));
                    showModeContent(activity, contentContainer, currentMode[0], currentColor[0], new OnColorChangedListener() {
                        public void onColorChanged(int color) {
                            currentColor[0] = color;
                            updatePreview(colorPreview, hexText, color);
                        }
                    });
                }
            }
        });
        
        addFavBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String hex = colorToHex(currentColor[0]);
                if (!favoriteColors.contains(hex)) {
                    if (favoriteColors.size() >= 30) {
                        favoriteColors.remove(0);
                    }
                    favoriteColors.add(hex);
                    saveFavoriteColors(activity, favoriteColors);
                    Toast("已添加到收藏夹");
                } else {
                    Toast("该颜色已存在");
                }
            }
        });
        
        pickBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showImagePickerDialog(activity, new OnColorPickedListener() {
                    public void onColorPicked(int color) {
                        currentColor[0] = color;
                        updatePreview(colorPreview, hexText, color);
                        alphaSeek.setProgress(Color.alpha(color));
                        alphaValue.setText(String.valueOf(Color.alpha(color)));
                    }
                });
            }
        });
        
        dialog.show();
        
        try {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(createRoundRectDrawable(Color.parseColor("#FF2D2D2D"), dp(activity, 16)));
            }
        } catch (Exception e) {}
        
    } catch (Exception e) {}
}

/**
 * 更新预览区域
 * @param preview 预览视图
 * @param hexText Hex文本视图
 * @param color 新颜色
 */
void updatePreview(View preview, TextView hexText, int color) {
    try {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(preview.getContext(), 12));
        bg.setStroke(2, Color.parseColor("#40FFFFFF"));
        preview.setBackgroundDrawable(bg);
        hexText.setText(colorToHex(color));
    } catch (Exception e) {}
}

/**
 * 显示模式内容
 * @param activity 当前 Activity
 * @param container 容器
 * @param mode 模式
 * @param initialColor 初始颜色
 * @param listener 颜色变化监听器
 */
void showModeContent(Activity activity, FrameLayout container, int mode, int initialColor, OnColorChangedListener listener) {
    container.removeAllViews();
    
    View view = null;
    switch (mode) {
        case 0:
            view = createRgbView(activity, initialColor, listener);
            break;
        case 1:
            view = createHsvView(activity, initialColor, listener);
            break;
        case 2:
            view = createColorWheelView(activity, initialColor, listener);
            break;
        case 3:
            view = createColorBarView(activity, initialColor, listener);
            break;
    }
    
    if (view != null) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1);
        params.gravity = Gravity.CENTER;
        container.addView(view, params);
    }
}


/**
 * 添加带调色盘按钮的颜色输入项
 * @param activity 当前 Activity
 * @param parent 父容器
 * @param title 标题文本
 * @param value 初始颜色值
 * @param hint 提示文本
 * @param titleColor 标题颜色
 * @param cardColor 卡片背景颜色
 * @param saveKey 存储键
 */
void addColorInputItem(final Activity activity, LinearLayout parent, String title, String value, String hint, int titleColor, int cardColor, final String saveKey) {
    try {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        
        // 内层浅背景 (比卡片浅35%)
        int itemColor = lightenColor(cardColor, 0.35f);
        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(itemColor);
        itemBg.setCornerRadius(dp(activity, 8));
        item.setBackgroundDrawable(itemBg);
        
        TextView t1 = new TextView(activity); 
        t1.setText(title); 
        t1.setTextSize(14); 
        t1.setTextColor(titleColor);
        item.addView(t1);

        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setPadding(0, dp(activity, 8), 0, 0);
        
        final int[] currentColor = {Color.parseColor("#FF7A9681")}; // 默认颜色
        boolean hasValidColor = false;
        String displayValue = "";
        
        try {
            if (value != null && !value.trim().isEmpty()) {
                currentColor[0] = Color.parseColor(value.trim());
                hasValidColor = true;
                displayValue = value.trim();
            }
        } catch (Exception e) {
            displayValue = "";
        }
        
        final EditText input = new EditText(activity);
        input.setText(displayValue);
        input.setHint(hint);
        input.setTextSize(14);
        input.setTypeface(Typeface.MONOSPACE);
        input.setBackgroundColor(Color.parseColor("#15FFFFFF"));
        input.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        
        if (hasValidColor) {
            input.setTextColor(currentColor[0]);
        } else {
            input.setTextColor(Color.parseColor("#FF333333"));
        }
        input.setHintTextColor(Color.GRAY);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        input.setLayoutParams(inputParams);
        
        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                // 保存到挂起映射
                pendingSettingsChanges.put(saveKey, val);
                try {
                    if (val.startsWith("#") && (val.length() == 7 || val.length() == 9)) {
                        int color = Color.parseColor(val);
                        input.setTextColor(color);
                    }
                } catch (Exception e) {
                    input.setTextColor(Color.parseColor("#FF333333"));
                }
            }
        });
        
        inputRow.addView(input);

        final TextView colorBtn = new TextView(activity);
        colorBtn.setText("🎨");
        colorBtn.setTextSize(22);
        colorBtn.setGravity(Gravity.CENTER);
        colorBtn.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8));
        colorBtn.setBackground(createSelectableBackground());
        inputRow.addView(colorBtn);
        
        item.addView(inputRow);
        parent.addView(item);
        
        // 添加底部间距
        View space = new View(activity);
        space.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(-1, dp(activity, 4));
        spaceParams.leftMargin = dp(activity, 6);
        spaceParams.rightMargin = dp(activity, 6);
        parent.addView(space, spaceParams);
        
        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String initialColorStr = input.getText().toString().trim();
                            if (initialColorStr.isEmpty() || initialColorStr.equals(hint)) {
                                initialColorStr = "#FF7A9681";
                            }
                            
                            showColorPickerDialog(activity, initialColorStr, new OnColorPickedListener() {
                                public void onColorPicked(final int color) {
                                    activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            try {
                                                String hex = colorToHex(color);
                                                input.setText(hex);
                                                input.setTextColor(color);
                                                // 保存到挂起映射
                                                pendingSettingsChanges.put(saveKey, hex);
                                            } catch (Exception e) {}
                                        }
                                    });
                                }
                            });
                        } catch (Exception e) {}
                    }
                });
            }
        };
        
        colorBtn.setOnClickListener(clickListener);
        
    } catch (Exception e) {}
}
