interface OnColorPickedListener {
    void onColorPicked(int color);
}

interface OnColorChangedListener {
    void onColorChanged(int color);
}

// SeekBar 统一走 uitools.applyUiSeekBar

String colorToHex(int color) {
    try {
        String a = Integer.toHexString((color >> 24) & 0xFF);
        String r = Integer.toHexString((color >> 16) & 0xFF);
        String g = Integer.toHexString((color >> 8) & 0xFF);
        String b = Integer.toHexString(color & 0xFF);
        if (a.length() < 2) a = "0" + a;
        if (r.length() < 2) r = "0" + r;
        if (g.length() < 2) g = "0" + g;
        if (b.length() < 2) b = "0" + b;
        return "#" + a.toUpperCase() + r.toUpperCase() + g.toUpperCase() + b.toUpperCase();
    } catch (Exception e) {
        return "#FF000000";
    }
}

boolean isValidHexColor(String colorCode) {
    if (colorCode == null || colorCode.trim().length() == 0) return false;
    String c = colorCode.trim();
    if (!c.startsWith("#")) return false;
    int len = c.length();
    if (len != 7 && len != 9) return false;
    for (int i = 1; i < len; i++) {
        char ch = c.charAt(i);
        boolean isHex = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
        if (!isHex) return false;
    }
    return true;
}

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
    } catch (Exception e) { traceLog("colorpicker_log", "[loadFavoriteColors] " + e.getMessage()); }
    return list;
}

void saveFavoriteColors(Activity activity, ArrayList favorites) {
    try {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(favorites.size(), 30);
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append(favorites.get(i).toString());
        }
        putString("settings", "color_favorites", sb.toString());
    } catch (Exception e) { traceLog("colorpicker_log", "[saveFavoriteColors] " + e.getMessage()); }
}

class ColorSeekBar extends LinearLayout {
    private android.widget.SeekBar seekBar; // 滑块控件
    private TextView valueLabel; // 数值显示标签
    private OnColorChangedListener listener; // 颜色变化监听器
    private int index; // 索引

    public ColorSeekBar(Context context, String label, int color, int initialValue, int max) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(0, dpxc(context, 8), 0, dpxc(context, 8));
        
        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setTextColor(color);
        labelView.setTypeface(null, Typeface.BOLD);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(dpxc(context, 32), -2));
        addView(labelView);
        
        LinearLayout seekContainer = new LinearLayout(context);
        seekContainer.setOrientation(LinearLayout.VERTICAL);
        seekContainer.setGravity(Gravity.CENTER_VERTICAL);
        seekContainer.setPadding(dpxc(context, 8), 0, dpxc(context, 8), 0);
        seekContainer.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        
        seekBar = new android.widget.SeekBar(context);
        seekBar.setMax(max);
        seekBar.setProgress(initialValue);
        try {
            applyUiSeekBar(seekBar, context, color);
        } catch (Exception e) { traceLog("colorpicker_log", "[ColorSeekBar] ColorSeekBar md3 错误: " + e.getMessage()); }
        
        seekContainer.addView(seekBar);
        addView(seekContainer);
        
        valueLabel = new TextView(context);
        valueLabel.setText(String.valueOf(initialValue));
        valueLabel.setTextSize(14);
        valueLabel.setTextColor(pc(getSettingsThemeColor(context, "on_surface_variant")));
        valueLabel.setTypeface(Typeface.MONOSPACE);
        valueLabel.setLayoutParams(new LinearLayout.LayoutParams(dpxc(context, 40), -2));
        valueLabel.setGravity(Gravity.RIGHT);
        addView(valueLabel);
        
        seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
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
    
    // 获取当前进度值   
     public int getProgress() {
        return seekBar.getProgress();
    }
    
    // 设置颜色变化监听器   
     public void setOnColorChangedListener(OnColorChangedListener l) {
        listener = l;
    }
}

class MagnifierView extends View {
    private Paint paint; // 主画笔
    private Paint borderPaint; // 边框画笔
    private Bitmap magnifiedBitmap; // 放大后的位图
    
    public MagnifierView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4);
        borderPaint.setColor(pc("#FFFFFFFF"));
    }
    
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
            crossPaint.setColor(pc("#FFFFFFFF"));
            crossPaint.setStrokeWidth(2);
            int center = size * scale;
            canvas.drawLine(center, 0, center, size * 2 * scale, crossPaint);
            canvas.drawLine(0, center, size * 2 * scale, center, crossPaint);
            
            invalidate();
        } catch (Exception e) { traceLog("colorpicker_log", "[update] " + e.getMessage()); }
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
        } catch (Exception e) { traceLog("colorpicker_log", "[onDraw] " + e.getMessage()); }
    }
}

View createRgbView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dpx(activity, 16), dpx(activity, 24), dpx(activity, 16), dpx(activity, 24));
    layout.setGravity(Gravity.CENTER_VERTICAL);
    
    final int[] rgb = {Color.red(initialColor), Color.green(initialColor), Color.blue(initialColor)};
    
    // R
    final ColorSeekBar redBar = new ColorSeekBar(activity, "R", pc("#FFFF3B30"), rgb[0], 255);
    redBar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int progress) {
            rgb[0] = progress;
            if (listener != null) listener.onColorChanged(Color.rgb(rgb[0], rgb[1], rgb[2]));
        }
    });
    layout.addView(redBar);
    
    // G
    final ColorSeekBar greenBar = new ColorSeekBar(activity, "G", pc("#FF34C759"), rgb[1], 255);
    greenBar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int progress) {
            rgb[1] = progress;
            if (listener != null) listener.onColorChanged(Color.rgb(rgb[0], rgb[1], rgb[2]));
        }
    });
    layout.addView(greenBar);
    
    // B
    final ColorSeekBar blueBar = new ColorSeekBar(activity, "B", pc("#FF007AFF"), rgb[2], 255);
    blueBar.setOnColorChangedListener(new OnColorChangedListener() {
        public void onColorChanged(int progress) {
            rgb[2] = progress;
            if (listener != null) listener.onColorChanged(Color.rgb(rgb[0], rgb[1], rgb[2]));
        }
    });
    layout.addView(blueBar);
    
    return layout;
}

Bitmap createHsvBitmap(int width, int height, int hue) {
    try {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        int hueW = width / 6;
        int gap = width / 20;
        int svW = width - hueW - gap - 16;
        int svH = height - 16;
        int hueLeft = svW + gap + 8;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] hueColors = new int[7];
        hueColors[0] = pc("#FFFF0000");
        hueColors[1] = pc("#FFFFFF00");
        hueColors[2] = pc("#FF00FF00");
        hueColors[3] = pc("#FF00FFFF");
        hueColors[4] = pc("#FF0000FF");
        hueColors[5] = pc("#FFFF00FF");
        hueColors[6] = pc("#FFFF0000");
        Shader hueShader = new LinearGradient(0, 8, 0, height - 8, (int[]) hueColors, null, Shader.TileMode.CLAMP);
        p.setShader(hueShader);
        canvas.drawRect(hueLeft, 8, hueLeft + hueW, height - 8, p);
        int hueColor = hsvColor(hue, 1.0f, 1.0f);
        Shader satShader = new LinearGradient(8, 8, svW + 8, 8, pc("#FFFFFFFF"), hueColor, Shader.TileMode.CLAMP);
        p.setShader(satShader);
        canvas.drawRect(8, 8, svW + 8, svH + 8, p);
        Shader valShader = new LinearGradient(8, 8, 8, svH + 8, pc("#00000000"), pc("#FF000000"), Shader.TileMode.CLAMP);
        p.setShader(valShader);
        canvas.drawRect(8, 8, svW + 8, svH + 8, p);
        return bmp;
    } catch (Exception e) { traceLog("colorpicker_log", "[createHsvBitmap] 错误: " + e.getMessage()); return null; }
}

View createHsvView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    final float[] hsv = new float[3];
    Color.colorToHSV(initialColor, hsv);
    final int W = dpx(activity, 280);
    final int H = dpx(activity, 200);
    final int hueW = W / 6;
    final int gap = W / 20;
    final int svW = W - hueW - gap - 16;
    final int hueLeft = svW + gap + 8;
    final FrameLayout layout = new FrameLayout(activity);
    layout.setPadding(dpx(activity, 4), dpx(activity, 4), dpx(activity, 4), dpx(activity, 4));
    final ImageView img = new ImageView(activity);
    img.setScaleType(ImageView.ScaleType.FIT_XY);
    Bitmap bmp = createHsvBitmap(W, H, (int)hsv[0]);
    if (bmp != null) img.setImageBitmap(bmp);
    layout.addView(img, new FrameLayout.LayoutParams(W, H));
    final View svCursor = new View(activity);
    svCursor.setBackgroundColor(pc("#00000000"));
    final FrameLayout.LayoutParams svParams = new FrameLayout.LayoutParams(dpx(activity, 16), dpx(activity, 16));
    svCursor.setLayoutParams(svParams);
    layout.addView(svCursor);
    final View hueCursor = new View(activity);
    hueCursor.setBackgroundColor(pc("#FFFFFFFF"));
    final FrameLayout.LayoutParams hueParams = new FrameLayout.LayoutParams(dpx(activity, 40), dpx(activity, 3));
    hueCursor.setLayoutParams(hueParams);
    layout.addView(hueCursor);
    final int[] svPos = {0, 0};
    final int[] huePos = {0};
    svPos[0] = (int)(8 + svW * hsv[1] - dpx(activity, 8));
    svPos[1] = (int)(8 + (H - 16) * (1 - hsv[2]) - dpx(activity, 8));
    svParams.leftMargin = svPos[0];
    svParams.topMargin = svPos[1];
    svCursor.setLayoutParams(svParams);
    huePos[0] = (int)(8 + (H - 16) * (hsv[0] / 360) - dpx(activity, 1.5f));
    hueParams.leftMargin = hueLeft - dpx(activity, 2);
    hueParams.topMargin = huePos[0];
    hueCursor.setLayoutParams(hueParams);
    img.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            float scaleX = W / (float)v.getWidth();
            float scaleY = H / (float)v.getHeight();
            x = x * scaleX;
            y = y * scaleY;
            if (x >= hueLeft && x <= hueLeft + hueW) {
                hsv[0] = Math.max(0, Math.min(360, (y - 8) / (H - 16) * 360));
                Bitmap nb = createHsvBitmap(W, H, (int)hsv[0]);
                if (nb != null) img.setImageBitmap(nb);
                huePos[0] = (int)(8 + (H - 16) * (hsv[0] / 360) - dpx(activity, 1.5f));
                hueParams.topMargin = huePos[0];
                hueCursor.setLayoutParams(hueParams);
            } else if (x >= 8 && x <= svW + 8 && y >= 8 && y <= H - 8) {
                hsv[1] = Math.max(0, Math.min(1, (x - 8) / svW));
                hsv[2] = Math.max(0, Math.min(1, 1 - (y - 8) / (H - 16)));
                svPos[0] = (int)(8 + svW * hsv[1] - dpx(activity, 8));
                svPos[1] = (int)(8 + (H - 16) * (1 - hsv[2]) - dpx(activity, 8));
                svParams.leftMargin = svPos[0];
                svParams.topMargin = svPos[1];
                svCursor.setLayoutParams(svParams);
            } else {
                return false;
            }
            if (listener != null) listener.onColorChanged(hsvColor(hsv[0], hsv[1], hsv[2]));
            return true;
        }
    });
    return layout;
}

Bitmap createColorWheelBitmap(int size) {
    try {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int cx = size / 2;
        int cy = size / 2;
        int r = size / 2 - 4;
        int[] colors = new int[]{ pc("#FFFF0000"), pc("#FFFFFF00"), pc("#FF00FF00"), pc("#FF00FFFF"), pc("#FF0000FF"), pc("#FFFF00FF"), pc("#FFFF0000") };
        Shader sweep = new SweepGradient(cx, cy, colors, null);
        p.setShader(sweep);
        canvas.drawCircle(cx, cy, r, p);
        Shader radial = new RadialGradient(cx, cy, r, pc("#FFFFFFFF"), pc("#00FFFFFF"), Shader.TileMode.CLAMP);
        p.setShader(radial);
        canvas.drawCircle(cx, cy, r, p);
        return bmp;
    } catch (Exception e) { traceLog("colorpicker_log", "[createColorWheelBitmap] 错误: " + e.getMessage()); return null; }
}

View createColorWheelView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    final float[] hsv = new float[3];
    Color.colorToHSV(initialColor, hsv);
    final int SIZE = dpx(activity, 200);
    final int maxR = SIZE / 2 - 4;
    final FrameLayout layout = new FrameLayout(activity);
    layout.setPadding(dpx(activity, 8), dpx(activity, 8), dpx(activity, 8), dpx(activity, 8));
    final ImageView img = new ImageView(activity);
    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
    Bitmap bmp = createColorWheelBitmap(SIZE);
    if (bmp != null) img.setImageBitmap(bmp);
    layout.addView(img, new FrameLayout.LayoutParams(SIZE, SIZE));
    final View cursor = new View(activity);
    cursor.setBackgroundColor(pc("#00000000"));
    final FrameLayout.LayoutParams cursorParams = new FrameLayout.LayoutParams(dpx(activity, 16), dpx(activity, 16));
    cursor.setLayoutParams(cursorParams);
    layout.addView(cursor);
    float initR = hsv[1] * maxR;
    float initAngle = (float)Math.toRadians(hsv[0]);
    cursorParams.leftMargin = (int)(SIZE / 2 + Math.cos(initAngle) * initR - dpx(activity, 8));
    cursorParams.topMargin = (int)(SIZE / 2 + Math.sin(initAngle) * initR - dpx(activity, 8));
    cursor.setLayoutParams(cursorParams);
    img.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            float scale = SIZE / (float)Math.min(v.getWidth(), v.getHeight());
            float offsetX = (v.getWidth() - SIZE / scale) / 2;
            float offsetY = (v.getHeight() - SIZE / scale) / 2;
            x = (x - offsetX) * scale;
            y = (y - offsetY) * scale;
            float dx = x - SIZE / 2;
            float dy = y - SIZE / 2;
            float dist = (float)Math.sqrt(dx * dx + dy * dy);
            if (dist > maxR) { dx = dx / dist * maxR; dy = dy / dist * maxR; dist = maxR; }
            hsv[1] = Math.max(0, Math.min(1, dist / maxR));
            float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0) angle += 360;
            hsv[0] = angle;
            hsv[2] = 1.0f;
            cursorParams.leftMargin = (int)(SIZE / 2 + dx - dpx(activity, 8));
            cursorParams.topMargin = (int)(SIZE / 2 + dy - dpx(activity, 8));
            cursor.setLayoutParams(cursorParams);
            if (listener != null) listener.onColorChanged(hsvColor(hsv[0], hsv[1], hsv[2]));
            return true;
        }
    });
    return layout;
}

Bitmap createColorBarBitmap(int width, int height) {
    try {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int[] colors = new int[]{ pc("#FFFF0000"), pc("#FFFFFF00"), pc("#FF00FF00"), pc("#FF00FFFF"), pc("#FF0000FF"), pc("#FFFF00FF"), pc("#FFFF0000") };
        Shader shader = new LinearGradient(0, 0, 0, height, colors, null, Shader.TileMode.CLAMP);
        p.setShader(shader);
        canvas.drawRect(0, 0, width, height, p);
        return bmp;
    } catch (Exception e) { traceLog("colorpicker_log", "[createColorBarBitmap] 错误: " + e.getMessage()); return null; }
}

View createColorBarView(Activity activity, int initialColor, final OnColorChangedListener listener) {
    final float[] hsv = new float[3];
    Color.colorToHSV(initialColor, hsv);
    final int BAR_H = dpx(activity, 180);
    final int BAR_W = dpx(activity, 48);
    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.HORIZONTAL);
    layout.setPadding(dpx(activity, 16), dpx(activity, 16), dpx(activity, 16), dpx(activity, 16));
    final View preview = new View(activity);
    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dpx(activity, 80), BAR_H);
    previewParams.rightMargin = dpx(activity, 16);
    preview.setLayoutParams(previewParams);
    preview.setBackgroundColor(initialColor);
    layout.addView(preview);
    final FrameLayout barContainer = new FrameLayout(activity);
    barContainer.setLayoutParams(new LinearLayout.LayoutParams(BAR_W, BAR_H));
    final ImageView barImg = new ImageView(activity);
    barImg.setScaleType(ImageView.ScaleType.FIT_XY);
    Bitmap barBmp = createColorBarBitmap(BAR_W, BAR_H);
    if (barBmp != null) barImg.setImageBitmap(barBmp);
    barContainer.addView(barImg, new FrameLayout.LayoutParams(BAR_W, BAR_H));
    final View barCursor = new View(activity);
    barCursor.setBackgroundColor(pc("#FFFFFFFF"));
    final FrameLayout.LayoutParams barCursorParams = new FrameLayout.LayoutParams(dpx(activity, 56), dpx(activity, 3));
    barCursorParams.leftMargin = -dpx(activity, 4);
    barCursorParams.topMargin = (int)(BAR_H * (hsv[0] / 360) - dpx(activity, 1.5f));
    barCursor.setLayoutParams(barCursorParams);
    barContainer.addView(barCursor);
    layout.addView(barContainer);
    barImg.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            float y = Math.max(0, Math.min(BAR_H, event.getY()));
            hsv[0] = y / BAR_H * 360;
            hsv[1] = 1.0f;
            hsv[2] = 1.0f;
            int color = hsvColor(hsv[0], hsv[1], hsv[2]);
            barCursorParams.topMargin = (int)(y - dpx(activity, 1.5f));
            barCursor.setLayoutParams(barCursorParams);
            preview.setBackgroundColor(color);
            if (listener != null) listener.onColorChanged(color);
            return true;
        }
    });
    return layout;
}
void showFavoritesView(Activity activity, FrameLayout container, ArrayList favorites, final OnColorPickedListener listener) {
    container.removeAllViews();
    
    ScrollView scroll = new ScrollView(activity);
    scroll.setVerticalScrollBarEnabled(false);
    
    LinearLayout list = new LinearLayout(activity);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(dpx(activity, 16), dpx(activity, 8), dpx(activity, 16), dpx(activity, 8));
    
    if (favorites.isEmpty()) {
        TextView empty = new TextView(activity);
        empty.setText("暂无收藏颜色\n点击 + 添加当前颜色");
        empty.setTextSize(14);
        empty.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, dpx(activity, 40), 0, 0);
        list.addView(empty);
    } else {
        for (int i = 0; i < favorites.size(); i++) {
            final String colorStr = favorites.get(i).toString();
            
            // 每项容器
            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dpx(activity, 12), dpx(activity, 12), dpx(activity, 12), dpx(activity, 12));
            item.setBackground(getSelectableBg(activity));
            
            // 颜色方块
            View colorBlock = new View(activity);
            LinearLayout.LayoutParams blockParams = new LinearLayout.LayoutParams(dpx(activity, 40), dpx(activity, 40));
            colorBlock.setLayoutParams(blockParams);
            
            try {
                int color = pc(colorStr);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(color);
                bg.setCornerRadius(dpx(activity, 8));
                // 添加边框
                bg.setStroke(2, pc("#20FFFFFF"));
                colorBlock.setBackgroundDrawable(bg);
            } catch (Exception e) {
                colorBlock.setBackgroundColor(pc("#FF888888"));
            }
            item.addView(colorBlock);
            
            // 颜色代码文本
            TextView codeText = new TextView(activity);
            codeText.setText(colorStr.toUpperCase());
            codeText.setTextSize(16);
            codeText.setTypeface(Typeface.MONOSPACE);
            codeText.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            codeText.setPadding(dpx(activity, 16), 0, 0, 0);
            codeText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            item.addView(codeText);
            
            // 删除按钮
            TextView deleteBtn = createButton(activity, "×", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 24f, 0, 8, 0, false, 0, 0, null);
            item.addView(deleteBtn);
            
            // 点击选择
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        int color = pc(colorStr);
                        if (listener != null) listener.onColorPicked(color);
                    } catch (Exception e) { traceLog("colorpicker_log", "[showFavoritesView] " + e.getMessage()); }
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
                line.setBackgroundColor(pc("#1AFFFFFF"));
                line.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
                list.addView(line);
            }
        }
    }
    
    scroll.addView(list);
    container.addView(scroll);
}

void showImagePickerDialog(final Activity activity, final OnColorPickedListener callback) {
    try {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(pc("#FF000000"));
        
        String imgPath = null;
        try {
            if (pluginPath != null) {
                imgPath = pluginPath + "/API/background.png";
            }
        } catch (Exception e) { traceLog("colorpicker_log", "[showImagePickerDialog] " + e.getMessage()); }
        
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
        
        // 图片视图 (限制最大高度；短屏取屏幕一半，保证底部确认栏不被裁掉)
        final ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageBitmap(bitmap);
        int imgHeight = (int) Math.min(dpx(activity, 400), activity.getResources().getDisplayMetrics().heightPixels * 0.5f);
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(-1, imgHeight);
        imgParams.gravity = Gravity.CENTER;
        root.addView(imageView, imgParams);
        
        // 放大镜
        final MagnifierView magnifier = new MagnifierView(activity);
        magnifier.setVisibility(View.GONE);
        root.addView(magnifier, new FrameLayout.LayoutParams(dpx(activity, 120), dpx(activity, 120)));
        
        // 底部颜色预览栏
        LinearLayout bottomBar = new LinearLayout(activity);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(pc(getSettingsThemeColor(activity, "surface")));
        bottomBar.setPadding(dpx(activity, 16), dpx(activity, 12), dpx(activity, 16), dpx(activity, 12));
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, -2);
        barParams.gravity = Gravity.BOTTOM;
        bottomBar.setLayoutParams(barParams);
        
        final View colorPreview = new View(activity);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dpx(activity, 48), dpx(activity, 48));
        colorPreview.setLayoutParams(previewParams);
        colorPreview.setBackgroundColor(pc("#FFFFFFFF"));
        bottomBar.addView(colorPreview);
        
        final TextView hexText = new TextView(activity);
        hexText.setText("#FFFFFF");
        hexText.setTextSize(18);
        hexText.setTypeface(Typeface.MONOSPACE);
        hexText.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
        hexText.setPadding(dpx(activity, 16), 0, 0, 0);
        hexText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        bottomBar.addView(hexText);
        
        TextView confirmBtn = createButton(activity, "确定", pc(getSettingsThemeColor(activity, "primary")), Color.TRANSPARENT, 16f, 0, 16, 8, false, 0, 0, null);
        bottomBar.addView(confirmBtn);
        
        root.addView(bottomBar);
        
        // 取消按钮 (左上角)
        TextView closeBtn = createButton(activity, "✕", pc("#FFFFFFFF"), Color.TRANSPARENT, 24f, 0, 16, 16, false, 0, 0, null);
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
                            
                            int magX = (int) x - dpx(activity, 60);
                            int magY = (int) y - dpx(activity, 140);
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
                } catch (Exception e) { traceLog("colorpicker_log", "[showImagePickerDialog] " + e.getMessage()); }
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
                    int color = pc(hexText.getText().toString());
                    if (callback != null) callback.onColorPicked(color);
                } catch (Exception e) { traceLog("colorpicker_log", "[showImagePickerDialog] " + e.getMessage()); }
                dialog.dismiss();
            }
        });
        
        dialog.show();

    } catch (Exception e) { traceLog("colorpicker_log", "[showImagePickerDialog] " + e.getMessage()); }
}

void showColorPickerDialog(final Activity activity, final String initialColor, final OnColorPickedListener callback) {
    // 兼容壳:解析 hex 字符串为 int,失败回退到中灰,再走 int 主入口
    int parsedColor = pc("#FF808080");
    if (initialColor != null && !initialColor.isEmpty()) {
        try {
            parsedColor = pc(initialColor);
        } catch (Throwable e) {
            parsedColor = isThemeDark(activity) ? pc("#FFEFEFEF") : pc("#FF1A1A1A");
        }
    } else {
        parsedColor = isThemeDark(activity) ? pc("#FFEFEFEF") : pc("#FF1A1A1A");
    }
    showColorPickerDialog(activity, parsedColor, callback);
}

void showColorPickerDialog(final Activity activity, final int initialColor, final OnColorPickedListener callback) {
    try {
        // 主入口:直接接收已解析的 int 颜色,杜绝 String 解析失败导致颜色丢失
        final int[] currentColor = {initialColor};
        
        final ArrayList favoriteColors = loadFavoriteColors(activity);
        final boolean[] showingFavorites = {false};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        
        // 根布局
        final LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(pc(getSettingsThemeColor(activity, "surface")));
        root.setPadding(dpx(activity, 16), dpx(activity, 16), dpx(activity, 16), dpx(activity, 16));
        
        // 顶部标题栏
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView title = new TextView(activity);
        title.setText("颜色");
        title.setTextSize(20);
        title.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        header.addView(title);
        
        // 收藏夹按钮
        final TextView favBtn = createButton(activity, "收藏夹", pc("#FFFFFFFF"), pc(getSettingsThemeColor(activity, "primary")), 14f, 16, 16, 8, false, 0, 0, null);
        header.addView(favBtn);

        // 取色按钮
        final TextView pickBtn = createButton(activity, "取色", pc("#FFFFFFFF"), pc("#FF5A5A5A"), 14f, 16, 16, 8, false, 0, 0, null);
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(-2, -2);
        pickParams.leftMargin = dpx(activity, 12);
        pickBtn.setLayoutParams(pickParams);
        header.addView(pickBtn);
        
        root.addView(header);
        
        // 颜色预览
        LinearLayout previewRow = new LinearLayout(activity);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        previewRow.setPadding(0, dpx(activity, 16), 0, dpx(activity, 16));
        
        final View colorPreview = new View(activity);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dpx(activity, 56), dpx(activity, 56));
        previewParams.rightMargin = dpx(activity, 16);
        colorPreview.setLayoutParams(previewParams);
        
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(currentColor[0]);
        previewBg.setCornerRadius(dpx(activity, 12));
        previewBg.setStroke(2, pc("#40FFFFFF"));
        colorPreview.setBackgroundDrawable(previewBg);
        previewRow.addView(colorPreview);
        
        final EditText hexInput = new EditText(activity);
        hexInput.setText(colorToHex(currentColor[0]));
        hexInput.setTextSize(16);
        hexInput.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
        hexInput.setTypeface(Typeface.MONOSPACE);
        hexInput.setPadding(dpx(activity, 12), dpx(activity, 8), dpx(activity, 12), dpx(activity, 8));
        hexInput.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dpx(activity, 8)));
        hexInput.setSingleLine(true);
        hexInput.setSelectAllOnFocus(true);
        final boolean[] isUpdatingFromInput = {false};
        hexInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                if (isUpdatingFromInput[0]) return;
                String input = s.toString().trim();
                traceLog("colorpicker_log", "[showColorPickerDialog] 输入框变化 input=" + input);
                try {
                    if (input.startsWith("#") && (input.length() == 7 || input.length() == 9)) {
                        int newColor = pc(input);
                        currentColor[0] = newColor;
                        isUpdatingFromInput[0] = true;
                        updatePreview(colorPreview, hexInput, newColor);
                        alphaSeek.setProgress(Color.alpha(newColor));
                        alphaValue.setText(String.valueOf(Color.alpha(newColor)));
                        isUpdatingFromInput[0] = false;
                        showModeContent(activity, contentContainer, currentMode[0], newColor, new OnColorChangedListener() {
                            public void onColorChanged(int color) {
                                currentColor[0] = color;
                                isUpdatingFromInput[0] = true;
                                updatePreview(colorPreview, hexInput, color);
                                isUpdatingFromInput[0] = false;
                            }
                        });
                    }
                } catch (Throwable e) { traceLog("colorpicker_log", "[showColorPickerDialog] afterTextChanged 错误: " + e.getMessage()); }
            }
        });
        previewRow.addView(hexInput);
        
        final TextView addFavBtn = createButton(activity, "+", pc(getSettingsThemeColor(activity, "primary")), Color.TRANSPARENT, 28f, 0, 16, 0, false, 0, 0, null);
        previewRow.addView(addFavBtn);
        
        root.addView(previewRow);
        
        // 模式标签栏
        LinearLayout tabRow = new LinearLayout(activity);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setBackground(roundRect(pc(getSettingsThemeColor(activity, "background")), dpx(activity, 8)));
        tabRow.setPadding(dpx(activity, 4), dpx(activity, 4), dpx(activity, 4), dpx(activity, 4));
        
        final String[] modes = {"RGB", "HSV", "色轮", "颜色条"};
        final TextView[] tabViews = new TextView[4];
        final int[] currentMode = {1};
        
        for (int i = 0; i < 4; i++) {
            final int mode = i;
            TextView tab = new TextView(activity);
            tab.setText(modes[i]);
            tab.setTextSize(14);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, dpx(activity, 8), 0, dpx(activity, 8));
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            
            if (i == currentMode[0]) {
                tab.setTextColor(pc(getSettingsThemeColor(activity, "surface")));
                tab.setTypeface(null, Typeface.BOLD);
                tab.setBackground(roundRect(pc(getSettingsThemeColor(activity, "primary")), dpx(activity, 6)));
            } else {
                tab.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
                tab.setBackground(null);
            }
            
            final int index = i;
            tab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentMode[0] = index;
                    for (int j = 0; j < 4; j++) {
                        if (j == index) {
                            tabViews[j].setTextColor(pc(getSettingsThemeColor(activity, "surface")));
                            tabViews[j].setTypeface(null, Typeface.BOLD);
                            tabViews[j].setBackground(roundRect(pc(getSettingsThemeColor(activity, "primary")), dpx(activity, 6)));
                        } else {
                            tabViews[j].setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
                            tabViews[j].setTypeface(null, Typeface.NORMAL);
                            tabViews[j].setBackground(null);
                        }
                    }
                    showModeContent(activity, contentContainer, index, currentColor[0], new OnColorChangedListener() {
                        public void onColorChanged(int color) {
                            currentColor[0] = color;
                            updatePreview(colorPreview, hexInput, color);
                        }
                    });
                }
            });
            
            tabViews[i] = tab;
            tabRow.addView(tab);
        }
        
        root.addView(tabRow);
        
        // 内容容器 - 固定高度确保显示完整（降低高度以缓解短屏滚动）
        final FrameLayout contentContainer = new FrameLayout(activity);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, dpx(activity, 240));
        contentParams.topMargin = dpx(activity, 12);
        contentParams.bottomMargin = dpx(activity, 12);
        contentContainer.setLayoutParams(contentParams);
        contentContainer.setBackground(roundRect(pc(getSettingsThemeColor(activity, "background")), dpx(activity, 12)));
        root.addView(contentContainer);
        
        // 初始化
        showModeContent(activity, contentContainer, 1, currentColor[0], new OnColorChangedListener() {
            public void onColorChanged(int color) {
                currentColor[0] = color;
                updatePreview(colorPreview, hexInput, color);
            }
        });
        
        // 透明度滑块
        LinearLayout alphaRow = new LinearLayout(activity);
        alphaRow.setOrientation(LinearLayout.HORIZONTAL);
        alphaRow.setGravity(Gravity.CENTER_VERTICAL);
        alphaRow.setPadding(0, dpx(activity, 8), 0, dpx(activity, 8));
        
        TextView alphaLabel = new TextView(activity);
        alphaLabel.setText("透明度");
        alphaLabel.setTextSize(14);
        alphaLabel.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        alphaLabel.setLayoutParams(new LinearLayout.LayoutParams(dpx(activity, 60), -2));
        alphaRow.addView(alphaLabel);
        
        final android.widget.SeekBar alphaSeek = new android.widget.SeekBar(activity);
        alphaSeek.setMax(255);
        alphaSeek.setProgress(Color.alpha(currentColor[0]));
        alphaSeek.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        try {
            applyUiSeekBar(alphaSeek, activity, pc(getSettingsThemeColor(activity, "primary")));
        } catch (Exception e) { traceLog("colorpicker_log", "[showColorPickerDialog] alphaSeek md3 错误: " + e.getMessage()); }
        alphaRow.addView(alphaSeek);
        
        final TextView alphaValue = new TextView(activity);
        alphaValue.setText(String.valueOf(Color.alpha(currentColor[0])));
        alphaValue.setTextSize(14);
        alphaValue.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
        alphaValue.setTypeface(Typeface.MONOSPACE);
        alphaValue.setLayoutParams(new LinearLayout.LayoutParams(dpx(activity, 40), -2));
        alphaValue.setGravity(Gravity.RIGHT);
        alphaRow.addView(alphaValue);
        
        root.addView(alphaRow);
        
        alphaSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    traceLog("colorpicker_log", "[onProgressChanged] alphaSeek 进度=" + progress);
                    currentColor[0] = Color.argb(progress, 
                        Color.red(currentColor[0]), 
                        Color.green(currentColor[0]), 
                        Color.blue(currentColor[0]));
                    isUpdatingFromInput[0] = true;
                    updatePreview(colorPreview, hexInput, currentColor[0]);
                    alphaValue.setText(String.valueOf(progress));
                    isUpdatingFromInput[0] = false;
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 底部按钮：固定在最外层底部，不随内容滚动，短屏上也不会被裁掉
        LinearLayout buttonRow = new LinearLayout(activity);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.END);
        buttonRow.setPadding(dpx(activity, 16), dpx(activity, 8), dpx(activity, 16), dpx(activity, 12));

        TextView cancelBtn = createButton(activity, "取消", pc(getSettingsThemeColor(activity, "primary")), -1, 16f, 20, 24, 12, false);
        buttonRow.addView(cancelBtn);

        TextView confirmBtn = createButton(activity, "确定", pc("#FFFFFFFF"), pc(getSettingsThemeColor(activity, "primary")), 16f, 20, 24, 12, true);
        buttonRow.addView(confirmBtn);

        // 内容包进 ScrollView，按钮行钉在最外层底部：内容超高时可上下滚动，取消/确定始终可见
        ScrollView contentScroll = new ScrollView(activity);
        contentScroll.addView(root);
        LinearLayout dialogRoot = new LinearLayout(activity);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setBackgroundColor(pc(getSettingsThemeColor(activity, "surface")));
        dialogRoot.addView(contentScroll, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        dialogRoot.addView(buttonRow, new LinearLayout.LayoutParams(-1, -2));

        builder.setView(dialogRoot);
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
                    favBtn.setBackground(createRippleBg(activity, pc("#FF5A5A5A"), dpx(activity, 16)));
                    showFavoritesView(activity, contentContainer, favoriteColors, new OnColorPickedListener() {
                        public void onColorPicked(int color) {
                            currentColor[0] = color;
                            updatePreview(colorPreview, hexInput, color);
                            alphaSeek.setProgress(Color.alpha(color));
                            alphaValue.setText(String.valueOf(Color.alpha(color)));
                        }
                    });
                } else {
                    favBtn.setText("收藏夹");
                    favBtn.setBackground(createRippleBg(activity, pc(getSettingsThemeColor(activity, "primary")), dpx(activity, 16)));
                    showModeContent(activity, contentContainer, currentMode[0], currentColor[0], new OnColorChangedListener() {
                        public void onColorChanged(int color) {
                            currentColor[0] = color;
                            updatePreview(colorPreview, hexInput, color);
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
                        updatePreview(colorPreview, hexInput, color);
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
                boolean cpDark = isThemeDark(activity);
                int elevated = mixTowardElevated(pc(getSettingsThemeColor(activity, "background")), cpDark);
                window.setBackgroundDrawable(roundRect(elevated, dpx(activity, 16)));
                applyWindowRadius(activity, window);
            }
        } catch (Exception e) { traceLog("colorpicker_log", "[showColorPickerDialog] " + e.getMessage()); }
        
    } catch (Exception e) { traceLog("colorpicker_log", "[showColorPickerDialog] " + e.getMessage()); }
}

void updatePreview(View preview, TextView hexText, int color) {
    try {
        traceLog("colorpicker_log", "[updatePreview] 更新预览 color=" + colorToHex(color));
        preview.setBackgroundColor(color);
        String hex = colorToHex(color);
        String current = hexText.getText().toString();
        if (!current.equalsIgnoreCase(hex)) {
            traceLog("colorpicker_log", "[updatePreview] 更新预览 setText=" + hex);
            hexText.setText(hex);
        }
    } catch (Exception e) { traceLog("colorpicker_log", "[updatePreview] 错误: " + e.getMessage()); }
}
void showModeContent(Activity activity, FrameLayout container, int mode, int initialColor, OnColorChangedListener listener) {
    container.removeAllViews();
    traceLog("colorpicker_log", "[showModeContent] 显示模式内容 mode=" + mode + " initialColor=" + colorToHex(initialColor));
    
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


interface ColorListCallback {
    void onColorListSaved(String csv);
}

/** 内置默认轮换色（文字/纯色背景共用起点，互不影响） */
String getDefaultRotColorList() {
    return "#FF5252,#4DB6AC,#448AFF,#66BB6A,#AB47BC,#FF9800,#FFEE58";
}

/** 读颜色列表，空则回退默认 */
String loadRotColorList(String key) {
    String s = getString("settings", key, "");
    if (s == null || s.trim().isEmpty()) return getDefaultRotColorList();
    return s;
}

/**
 * 颜色列表编辑器：快捷增删，可限制数量
 * @param maxColors 0=不限制
 */
void showColorListEditor(final Activity activity, String title, final int maxColors, String currentCsv, final ColorListCallback callback) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final List colors = new ArrayList();
                if (currentCsv != null && !currentCsv.trim().isEmpty()) {
                    String[] parts = currentCsv.split(",");
                    for (int i = 0; i < parts.length; i++) {
                        String c = parts[i].trim();
                        if (c.length() == 0) continue;
                        if (!c.startsWith("#")) c = "#" + c;
                        if (isValidHexColor(c)) colors.add(c);
                    }
                }

                final Dialog dlg = new Dialog(activity);
                dlg.requestWindowFeature(1);
                try {
                    Window w = dlg.getWindow();
                    if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                } catch (Throwable ignore) {}

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dpx(activity, 20), dpx(activity, 18), dpx(activity, 20), dpx(activity, 12));
                root.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dpx(activity, 16)));

                TextView titleTv = new TextView(activity);
                titleTv.setText(title != null ? title : "颜色列表");
                titleTv.setTextSize(17);
                titleTv.setTypeface(null, Typeface.BOLD);
                titleTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
                titleTv.setPadding(0, 0, 0, dpx(activity, 8));
                root.addView(titleTv);

                TextView tip = new TextView(activity);
                tip.setText(maxColors > 0 ? ("最多 " + maxColors + " 个，点击色块编辑，长按删除") : "点击色块编辑，长按删除");
                tip.setTextSize(12);
                tip.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
                tip.setPadding(0, 0, 0, dpx(activity, 10));
                root.addView(tip);

                final LinearLayout listWrap = new LinearLayout(activity);
                listWrap.setOrientation(LinearLayout.VERTICAL);
                ScrollView sc = new ScrollView(activity);
                sc.addView(listWrap);
                sc.setLayoutParams(new LinearLayout.LayoutParams(-1, dpx(activity, 220)));
                root.addView(sc);

                final Runnable[] refresh = new Runnable[1];
                refresh[0] = new Runnable() {
                    public void run() {
                        listWrap.removeAllViews();
                        for (int i = 0; i < colors.size(); i++) {
                            final int idx = i;
                            final String hex = (String) colors.get(i);
                            LinearLayout row = new LinearLayout(activity);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setGravity(Gravity.CENTER_VERTICAL);
                            row.setPadding(dpx(activity, 8), dpx(activity, 8), dpx(activity, 8), dpx(activity, 8));

                            View sw = new View(activity);
                            GradientDrawable sgd = new GradientDrawable();
                            sgd.setColor(pc(hex));
                            sgd.setCornerRadius(dpx(activity, 8));
                            sw.setBackground(sgd);
                            LinearLayout.LayoutParams swp = new LinearLayout.LayoutParams(dpx(activity, 36), dpx(activity, 36));
                            swp.rightMargin = dpx(activity, 12);
                            row.addView(sw, swp);

                            TextView hexTv = new TextView(activity);
                            hexTv.setText(hex);
                            hexTv.setTextSize(14);
                            hexTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
                            row.addView(hexTv, new LinearLayout.LayoutParams(0, -2, 1.0f));

                            TextView del = createButton(activity, "删除", Color.WHITE, pc(getSettingsThemeColor(activity, "error")), 13f, 6, 12, 6, false, 0, 0, null);
                            row.addView(del);

                            row.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    showColorPickerDialog(activity, hex, new OnColorPickedListener() {
                                        public void onColorPicked(int color) {
                                            colors.set(idx, colorToHex(color));
                                            refresh[0].run();
                                        }
                                    });
                                }
                            });
                            del.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    colors.remove(idx);
                                    refresh[0].run();
                                }
                            });
                            listWrap.addView(row);
                        }
                    }
                };
                refresh[0].run();

                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.CENTER_VERTICAL);
                btnRow.setPadding(0, dpx(activity, 12), 0, 0);

                TextView addBtn = createButton(activity, "+ 添加颜色", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 8, 16, 8, false, 0, 0, null);
                TextView okBtn = createButton(activity, "确定", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 8, 16, 8, false, 0, 0, null);
                TextView cancelBtn = createButton(activity, "取消", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 8, 16, 8, false, 0, 0, null);
                btnRow.addView(addBtn);
                btnRow.addView(new Space(activity), new LinearLayout.LayoutParams(0, 1, 1.0f));
                btnRow.addView(cancelBtn);
                btnRow.addView(okBtn);
                root.addView(btnRow);

                addBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (maxColors > 0 && colors.size() >= maxColors) {
                            Toast("最多 " + maxColors + " 个颜色");
                            return;
                        }
                        showColorPickerDialog(activity, isThemeDark(activity) ? "#FF448AFF" : "#FF2196F3", new OnColorPickedListener() {
                            public void onColorPicked(int color) {
                                colors.add(colorToHex(color));
                                refresh[0].run();
                            }
                        });
                    }
                });
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { try { dlg.dismiss(); } catch (Throwable ignore) {} }
                });
                okBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < colors.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append((String) colors.get(i));
                        }
                        if (callback != null) callback.onColorListSaved(sb.toString());
                        try { dlg.dismiss(); } catch (Throwable ignore) {}
                    }
                });

                dlg.setContentView(root);
                dlg.show();
                applyUiTheme(activity, dlg, 1);
            } catch (Throwable e) { traceLog("colorpicker_log", "[showColorListEditor] 异常: " + e); }
        }
    });
}

void addSettingsColorItem(String categoryName, String itemName, String descriptionText, String keyName, String defaultValue, final Runnable onColorChanged, final boolean checkContrast) {
    if (SettingsState.settingsCategoryContainers == null || categoryName == null) return;
    Activity activity = getSettingsCurrentActivity();
    if (activity == null) return;

    String currentValue = getString("settings", keyName, defaultValue);

    LinearLayout itemLayout = new LinearLayout(activity);
    itemLayout.setOrientation(LinearLayout.VERTICAL);
    itemLayout.setPadding(dpx(activity, 16), dpx(activity, 12), dpx(activity, 16), dpx(activity, 12));

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
        descView.setPadding(0, dpx(activity, 2), 0, dpx(activity, 8));
        itemLayout.addView(descView);
    }

    LinearLayout colorRow = new LinearLayout(activity);
    colorRow.setOrientation(LinearLayout.HORIZONTAL);
    colorRow.setGravity(Gravity.CENTER_VERTICAL);

    final View colorPreview = new View(activity);
    int previewSize = dpx(activity, 36);
    LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(previewSize, previewSize);
    previewParams.rightMargin = dpx(activity, 12);
    colorPreview.setLayoutParams(previewParams);
    GradientDrawable previewBackground = new GradientDrawable();
    previewBackground.setCornerRadius(dpx(activity, 6));
    if (isValidHexColor(currentValue)) {
        previewBackground.setColor(pc(currentValue));
    } else {
        previewBackground.setColor(pc("#FF888888"));
    }
    colorPreview.setBackground(previewBackground);
    colorRow.addView(colorPreview);

    TextView pickerBtn = new TextView(activity);
    pickerBtn.setText("🎨 点击选择颜色");
    pickerBtn.setTextSize(14);
    pickerBtn.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    colorRow.addView(pickerBtn);

    itemLayout.addView(colorRow);

    itemLayout.setBackground(makeFeedbackBg(getAdaptiveSettingsItemBg(activity), pc(getSettingsThemeColor(activity, "ripple")), 0));
    itemLayout.setClickable(true);

    final String finalKeyName = keyName;
    final String finalDefaultValue = defaultValue;
    itemLayout.setOnClickListener(new View.OnClickListener() {
        public void onClick(View view) {
            Activity act = getSettingsCurrentActivity();
            if (act == null) return;
            String savedColor = getString("settings", finalKeyName, "");
            String currentColor = (savedColor != null && !savedColor.isEmpty() && isValidHexColor(savedColor)) ? savedColor : finalDefaultValue;
            if (currentColor == null || currentColor.isEmpty() || !isValidHexColor(currentColor)) {
                boolean isDark = isThemeDark(act);
                currentColor = isDark ? "#f7efffef" : "#FF1A1A1A";
            }
            // 在调用方把 hex 字符串解析成 int,避免 ColorPicker 内部 String 解析失败导致颜色丢失
            int parsedColor = pc("#FF808080");
            try {
                if (currentColor != null && !currentColor.isEmpty()) {
                    parsedColor = pc(currentColor);
                }
            } catch (Throwable e) {
                parsedColor = isThemeDark(act) ? pc("#FFEFEFEF") : pc("#FF1A1A1A");
            }
            showColorPickerDialog(act, parsedColor, new OnColorPickedListener() {
                    public void onColorPicked(int color) {
                        String hex = colorToHex(color);
                        putString("settings", finalKeyName, hex);
                        if (colorPreview != null) {
                            GradientDrawable bg = new GradientDrawable();
                            bg.setColor(color);
                            bg.setCornerRadius(dp(act, 6));
                            colorPreview.setBackground(bg);
                        }
                        if (onColorChanged != null) {
                            onColorChanged.run();
                        }
                        if (checkContrast) {
                            Activity checkAct = getSettingsCurrentActivity();
                            if (checkAct != null) {
                                String bgColorStr = getString("settings", isThemeDark(checkAct) ? "ui_bg_color_dark" : "ui_bg_color_light", "");
                                if (bgColorStr != null && !bgColorStr.isEmpty() && isValidHexColor(bgColorStr)) {
                                    try {
                                        int bgColor = pc(bgColorStr);
                                        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
                                        double bgLuminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255.0;
                                        double contrast = Math.abs(luminance - bgLuminance);
                                        if (contrast < 0.2) {
                                            Toast("提示:当前颜色与背景对比度较低,可能影响可读性");
                                        }
                                    } catch (Throwable e) { traceLog("colorpicker_log", "[onColorPicked] 异常: " + e); }
                                }
                            }
                        }
                    }
                });
        }
    });

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
            "color"
        ));
    }

    LinearLayout container = (LinearLayout) SettingsState.settingsCategoryContainers.get(categoryName);
    if (container != null) {
        container.addView(itemLayout);
    }
}

