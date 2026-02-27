//UI工具类

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.*;
import android.widget.*;
import android.util.TypedValue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
/**
 * 创建带按压反馈的圆角背景 Drawable
 * @param normalColor int: 常态颜色
 * @param pressedColor int: 按压颜色
 * @param r int: 圆角半径
 * @return StateListDrawable: 状态列表Drawable
 */
StateListDrawable makeFeedbackBg(int normalColor, int pressedColor, int r) {
    StateListDrawable sld = new StateListDrawable();
    GradientDrawable pressed = new GradientDrawable();
    pressed.setColor(pressedColor);
    pressed.setCornerRadius(r);
    GradientDrawable normal = new GradientDrawable();
    normal.setColor(normalColor);
    normal.setCornerRadius(r);
    sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
    sld.addState(new int[]{}, normal);
    return sld;
}

/**
 * 调整颜色亮度
 * @param color int: 原色值
 * @param factor float: 调整系数 (0-1变暗, >1变亮)
 * @return int: 新颜色值
 */
int adjustColor(int color, float factor) {
    int a = Color.alpha(color);
    int r = Math.round(Color.red(color) * factor);
    int g = Math.round(Color.green(color) * factor);
    int b = Math.round(Color.blue(color) * factor);
    return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
}

/**
 * 创建简单圆角背景
 * @param c int: 颜色
 * @param r int: 半径
 * @return GradientDrawable: 圆角Drawable
 */
GradientDrawable roundRect(int c, int r) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(c);
    g.setCornerRadius(r);
    return g;
}

/**
 * 工厂：创建通用输入框
 * @param a Activity: 上下文
 * @param h String: 提示文本
 * @param bg int: 背景颜色
 * @return EditText: 输入框实例
 */
EditText makeInput(Activity a, String h, int bg) {
    int pressedBg = adjustColor(bg, 0.9f);
    EditText e = new EditText(a);
    e.setHint(h);
    e.setTextSize(13);
    e.setTextColor(Color.parseColor("#222222"));
    e.setHintTextColor(Color.parseColor("#BBBBBB"));
    e.setBackground(makeFeedbackBg(bg, pressedBg, dp(a, 6)));
    e.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8));
    e.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return e;
}

/**
 * 工厂：创建小型输入框（用于数字）
 * @param a Activity: 上下文
 * @param h String: 提示文本
 * @param bg int: 背景颜色
 * @return EditText: 输入框实例
 */
EditText makeSmallInput(Activity a, String h, int bg) {
    int pressedBg = adjustColor(bg, 0.9f);
    EditText e = new EditText(a);
    e.setHint(h);
    e.setTextSize(12);
    e.setTextColor(Color.parseColor("#222222"));
    e.setHintTextColor(Color.parseColor("#BBBBBB"));
    e.setBackground(makeFeedbackBg(bg, pressedBg, dp(a, 4)));
    e.setPadding(dp(a, 8), dp(a, 6), dp(a, 8), dp(a, 6));
    e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    e.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(a, 60), -2);
    e.setLayoutParams(p);
    return e;
}

/**
 * 工厂：创建超小输入框（用于紧凑布局）
 * @param a Activity: 上下文
 * @param h String: 提示文本
 * @param bg int: 背景颜色
 * @return EditText: 输入框实例
 */
EditText makeTinyInput(Activity a, String h, int bg) {
    int pressedBg = adjustColor(bg, 0.9f);
    EditText e = new EditText(a);
    e.setHint(h);
    e.setTextSize(11);
    e.setTextColor(Color.parseColor("#222222"));
    e.setHintTextColor(Color.parseColor("#BBBBBB"));
    e.setBackground(makeFeedbackBg(bg, pressedBg, dp(a, 4)));
    e.setPadding(dp(a, 6), dp(a, 4), dp(a, 6), dp(a, 4));
    e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    e.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(a, 45), dp(a, 32));
    e.setLayoutParams(p);
    return e;
}

/**
 * 工厂：创建标签块
 * @param a Activity: 上下文
 * @param t String: 标签文本
 * @param s boolean: 是否选中
 * @param type int: 颜色样式类型
 * @return TextView: 标签视图
 */
TextView makeChip(Activity a, String t, boolean s, int type) {
    TextView v = new TextView(a);
    v.setText(t);
    v.setTextSize(11);
    v.setPadding(dp(a, 10), dp(a, 4), dp(a, 10), dp(a, 4));
    
    int normalColor, pressedColor;
    if (type == 1) { 
        normalColor = s ? Color.parseColor("#3B71FE") : Color.parseColor("#E0E0E0");
        pressedColor = s ? Color.parseColor("#2E5BC7") : Color.parseColor("#B0B0B0");
        v.setTextColor(s ? Color.WHITE : Color.parseColor("#666666"));
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(a, 12), dp(a, 4), dp(a, 12), dp(a, 4));
    } else if (type == 2) {
        normalColor = s ? Color.parseColor("#FF9800") : Color.parseColor("#F0F0F0");
        pressedColor = s ? Color.parseColor("#F57C00") : Color.parseColor("#D6D6D6");
        v.setTextColor(s ? Color.WHITE : Color.parseColor("#666666"));
    } else {
        normalColor = s ? Color.parseColor("#3B71FE") : Color.parseColor("#F0F0F0");
        pressedColor = s ? Color.parseColor("#2E5BC7") : Color.parseColor("#D6D6D6");
        v.setTextColor(s ? Color.WHITE : Color.parseColor("#666666"));
    }
    
    v.setBackground(makeFeedbackBg(normalColor, pressedColor, dp(a, 50)));
    return v;
}

/**
 * 更新标签视觉状态
 * @param v TextView: 标签视图
 * @param s boolean: 是否选中
 */
void setChip(TextView v, boolean s) {
    int normalColor = s ? Color.parseColor("#3B71FE") : Color.parseColor("#F0F0F0");
    int pressedColor = s ? Color.parseColor("#2E5BC7") : Color.parseColor("#D6D6D6");
    v.setTextColor(s ? Color.WHITE : Color.parseColor("#666666"));
    v.setBackground(makeFeedbackBg(normalColor, pressedColor, dp(v.getContext(), 50)));
}

/**
 * 更新标签视觉状态（带类型）
 * @param v TextView: 标签视图
 * @param s boolean: 是否选中
 * @param type int: 样式类型
 */
void setChipWithType(TextView v, boolean s, int type) {
    int normalColor, pressedColor;
    if (type == 2) {
        normalColor = s ? Color.parseColor("#FF9800") : Color.parseColor("#F0F0F0");
        pressedColor = s ? Color.parseColor("#F57C00") : Color.parseColor("#D6D6D6");
    } else {
        normalColor = s ? Color.parseColor("#3B71FE") : Color.parseColor("#F0F0F0");
        pressedColor = s ? Color.parseColor("#2E5BC7") : Color.parseColor("#D6D6D6");
    }
    v.setTextColor(s ? Color.WHITE : Color.parseColor("#666666"));
    v.setBackground(makeFeedbackBg(normalColor, pressedColor, dp(v.getContext(), 50)));
}

/**
 * 工厂：创建代码预设按钮
 * @param a Activity: 上下文
 * @param t String: 文本
 * @param textColor int: 字体颜色
 * @return TextView: 按钮视图
 */
TextView makePresetChip(Activity a, String t, int textColor) {
    TextView v = new TextView(a);
    v.setText(t);
    v.setTextSize(10);
    v.setPadding(dp(a, 8), dp(a, 3), dp(a, 8), dp(a, 3));
    v.setTextColor(textColor);
    int bg = Color.parseColor("#F5F5F5");
    v.setBackground(makeFeedbackBg(bg, adjustColor(bg, 0.9f), dp(a, 50)));
    v.setMinWidth(dp(a, 36));
    v.setGravity(Gravity.CENTER);
    return v;
}

/**
 * 工厂：创建开关按钮
 * @param a Activity: 上下文
 * @param o boolean: 是否开启
 * @param c int: 开启时的颜色
 * @return TextView: 按钮视图
 */
TextView makeSwitch(Activity a, boolean o, int c) {
    TextView v = new TextView(a);
    v.setText(o ? "开" : "关");
    v.setTextSize(12);
    v.setTextColor(o ? Color.WHITE : Color.parseColor("#666666"));
    v.setGravity(Gravity.CENTER);
    v.setPadding(dp(a, 12), dp(a, 4), dp(a, 12), dp(a, 4));
    int bg = o ? c : Color.parseColor("#E0E0E0");
    v.setBackground(makeFeedbackBg(bg, adjustColor(bg, 0.9f), dp(a, 20)));
    return v;
}

/**
 * 更新开关按钮状态
 * @param v TextView: 按钮视图
 * @param o boolean: 是否开启
 * @param c int: 开启时的颜色
 */
void setSwitch(TextView v, boolean o, int c) {
    v.setText(o ? "开" : "关");
    v.setTextColor(o ? Color.WHITE : Color.parseColor("#666666"));
    int bg = o ? c : Color.parseColor("#E0E0E0");
    v.setBackground(makeFeedbackBg(bg, adjustColor(bg, 0.9f), dp(v.getContext(), 20)));
}

/**
 * 工厂：创建大按钮
 * @param a Activity: 上下文
 * @param t String: 文本
 * @param tc int: 文本颜色
 * @param bg int: 背景颜色
 * @return TextView: 按钮视图
 */
TextView makeBtn(Activity a, String t, int tc, int bg) {
    TextView v = new TextView(a);
    v.setText(t);
    v.setTextSize(14);
    v.setTextColor(tc);
    v.setGravity(Gravity.CENTER);
    v.setBackground(makeFeedbackBg(bg, adjustColor(bg, 0.9f), dp(a, 8)));
    v.setPadding(0, dp(a, 10), 0, dp(a, 10));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(a, 6), 0, 0);
    v.setLayoutParams(p);
    return v;
}



/**
 * 根据 MIME 类型获取文件扩展名
 * @param mimeType MIME 类型字符串
 * @return 文件扩展名，默认返回 .png
 */
String getExtensionFromMimeType(String mimeType) {
    if (mimeType == null) return ".png";
    if (mimeType.equals("image/gif") || mimeType.equals("webp/gif")) return ".gif";
    return ".png";
}

/**
 * 从 Uri 加载 Bitmap
 * @param activity 当前 Activity
 * @param uri 图片 URI
 * @return 解码后的 Bitmap，失败返回 null
 */
Bitmap loadBitmapFromUri(Activity activity, Uri uri) {
    try {
        return BitmapFactory.decodeStream(activity.getContentResolver().openInputStream(uri));
    } catch (Throwable e) {
        return null;
    }
}

/**
 * 获取压缩预览图
 * @param src 源图片
 * @param quality 压缩质量 (0-100)
 * @return 压缩后的 Bitmap
 */
Bitmap getCompressedPreview(Bitmap src, int quality) {
    if (quality >= 100 || src == null) return src;
    try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] bytes = baos.toByteArray();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    } catch (Throwable e) { return src; }
}

/**
 * 创建棋盘格背景 Drawable（用于透明图片展示）
 * @param context 上下文
 * @return 棋盘格 BitmapDrawable
 */
BitmapDrawable getCheckerboardDrawable(Context context) {
    int size = dp(context, 20); // 格子大小
    Bitmap b = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    Paint p = new Paint();
    p.setColor(Color.parseColor("#FFCCCCCC"));
    c.drawRect(0, 0, size, size, p);
    c.drawRect(size, size, size * 2, size * 2, p);
    p.setColor(Color.parseColor("#FFEEEEEE"));
    c.drawRect(size, 0, size * 2, size, p);
    c.drawRect(0, size, size, size * 2, p);
    BitmapDrawable drawable = new BitmapDrawable(context.getResources(), b);
    drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    return drawable;
}


/**
 * 支持手势缩放和点击切换背景的 ImageView
 */
class ZoomImageView extends ImageView {
    public Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private float startDistance = 0f;
    private float midX = 0f, midY = 0f;
    private float lastX = 0f, lastY = 0f;
    private int mode = 0; // 模式：1=拖拽，2=缩放
    private View rootLayout; // 根布局
    private int bgIndex = 0; // 背景索引

    public ZoomImageView(Context context, View root) {
        super(context);
        this.rootLayout = root;
        setScaleType(ImageView.ScaleType.MATRIX);
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                lastX = x; lastY = y;
                mode = 1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                startDistance = calculateDistance(event);
                if (startDistance > 10f) {
                    savedMatrix.set(matrix);
                    midX = (event.getX(0) + event.getX(1)) / 2;
                    midY = (event.getY(0) + event.getY(1)) / 2;
                    mode = 2;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == 1) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(x - lastX, y - lastY);
                } else if (mode == 2) {
                    float newDist = calculateDistance(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / startDistance;
                        matrix.postScale(scale, scale, midX, midY);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (mode == 1 && Math.abs(x - lastX) < 10 && Math.abs(y - lastY) < 10) {
                    if (!isInside(x, y)) {
                        bgIndex = (bgIndex + 1) % 3;
                        if (bgIndex == 0) rootLayout.setBackgroundColor(Color.parseColor("#FF000000"));
                        else if (bgIndex == 1) rootLayout.setBackgroundColor(Color.parseColor("#FFFFFFFF"));
                        else rootLayout.setBackground(getCheckerboardDrawable(getContext()));
                        vibrate(getContext(), 30);
                    }
                }
                mode = 0;
                break;
        }
        setImageMatrix(matrix);
        return true;
    }

    /** 计算两点距离 */
    private float calculateDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** 判断点击点是否在图片内部 */
    private boolean isInside(float x, float y) {
        if (getDrawable() == null) return false;
        RectF r = new RectF(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        matrix.mapRect(r);
        return r.contains(x, y);
    }
}


/**
 * 裁剪覆盖视图
 */
class CropOverlayView extends View {
    public Rect cropRect = new Rect();
    private Paint pBorder, pCorner, pMask;
    private int tMode = 0; // 触摸模式
    private float lastX, lastY;

    public CropOverlayView(Context context) {
        super(context);
        pBorder = new Paint(1); pBorder.setColor(Color.parseColor("#FF00FF00")); pBorder.setStyle(Paint.Style.STROKE); pBorder.setStrokeWidth(4);
        pCorner = new Paint(1); pCorner.setColor(Color.parseColor("#FF00FF00")); pCorner.setStrokeWidth(16);
        pMask = new Paint(); pMask.setColor(Color.parseColor("#99000000"));
    }

    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        int side = Math.min(w, h) * 4 / 5;
        cropRect.set((w - side) / 2, (h - side) / 2, (w + side) / 2, (h + side) / 2);
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), cropRect.top, pMask);
        canvas.drawRect(0, cropRect.bottom, getWidth(), getHeight(), pMask);
        canvas.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, pMask);
        canvas.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, pMask);
        canvas.drawRect(cropRect, pBorder);
        int l = 50;
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + l, cropRect.top, pCorner);
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + l, pCorner);
        canvas.drawLine(cropRect.right - l, cropRect.top, cropRect.right, cropRect.top, pCorner);
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + l, pCorner);
        canvas.drawLine(cropRect.left, cropRect.bottom - l, cropRect.left, cropRect.bottom, pCorner);
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + l, cropRect.bottom, pCorner);
        canvas.drawLine(cropRect.right - l, cropRect.bottom, cropRect.right, cropRect.bottom, pCorner);
        canvas.drawLine(cropRect.right, cropRect.bottom - l, cropRect.right, cropRect.bottom, pCorner);
    }

    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            lastX = x; lastY = y;
            if (Math.abs(x - cropRect.left) < 80 && Math.abs(y - cropRect.top) < 80) tMode = 2;
            else if (Math.abs(x - cropRect.right) < 80 && Math.abs(y - cropRect.top) < 80) tMode = 3;
            else if (Math.abs(x - cropRect.left) < 80 && Math.abs(y - cropRect.bottom) < 80) tMode = 4;
            else if (Math.abs(x - cropRect.right) < 80 && Math.abs(y - cropRect.bottom) < 80) tMode = 5;
            else if (cropRect.contains((int)x, (int)y)) tMode = 1;
            else tMode = 0;
            return tMode != 0;
        } else if (e.getAction() == MotionEvent.ACTION_MOVE && tMode != 0) {
            float dx = x - lastX, dy = y - lastY;
            if (tMode == 1) cropRect.offset((int)dx, (int)dy);
            else {
                if (tMode == 2 || tMode == 4) cropRect.left = (int)Math.max(0, Math.min(x, cropRect.right - 100));
                if (tMode == 3 || tMode == 5) cropRect.right = (int)Math.min(getWidth(), Math.max(x, cropRect.left + 100));
                if (tMode == 2 || tMode == 3) cropRect.top = (int)Math.max(0, Math.min(y, cropRect.bottom - 100));
                if (tMode == 4 || tMode == 5) cropRect.bottom = (int)Math.min(getHeight(), Math.max(y, cropRect.top + 100));
            }
            lastX = x; lastY = y; invalidate(); return true;
        }
        return false;
    }

    /** 获取实际裁剪矩形 */
    public Rect getRealRect(Bitmap b, Matrix m) {
        float[] v = new float[9]; m.getValues(v);
        float s = v[0], tx = v[2], ty = v[5];
        return new Rect(
            (int)((cropRect.left - tx) / s), (int)((cropRect.top - ty) / s),
            (int)((cropRect.right - tx) / s), (int)((cropRect.bottom - ty) / s)
        );
    }
}


/**
 * 显示图片编辑对话框
 * @param activity 当前 Activity
 * @param origin 原始图片
 * @param savePath 保存路径
 * @param requestCode 请求码
 */
void showEditDialog(final Activity activity, final Bitmap origin, final String savePath, final int requestCode) {
    try {
        final List history = new ArrayList(); history.add(origin);
        final int[] idx = {0}, quality = {100};

        final Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(Color.parseColor("#FF000000"));

        final ZoomImageView preview = new ZoomImageView(activity, root);
        preview.setImageBitmap(origin);
        
        // 自动居中
        preview.post(new Runnable() {
            public void run() {
                float vw = preview.getWidth(), vh = preview.getHeight();
                float iw = origin.getWidth(), ih = origin.getHeight();
                float avH = vh - dp(activity, 120);
                float s = Math.min(vw / iw, avH / ih) * 0.9f;
                preview.matrix.setScale(s, s);
                preview.matrix.postTranslate((vw - iw * s) / 2f, (avH - ih * s) / 2f);
                preview.setImageMatrix(preview.matrix);
            }
        });

        final FrameLayout container = new FrameLayout(activity);
        FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(-1, -1);
        cLp.bottomMargin = dp(activity, 120);
        container.addView(preview, new FrameLayout.LayoutParams(-1, -1));

        final CropOverlayView crop = new CropOverlayView(activity);
        crop.setVisibility(View.GONE);
        container.addView(crop, new FrameLayout.LayoutParams(-1, -1));
        root.addView(container, cLp);

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout toolBar = new LinearLayout(activity);
        toolBar.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));

        String[] toolNames = {"取消", "撤销", "重做", "旋转", "裁剪", "质量: 原画", "保存"};
        for (int i = 0; i < toolNames.length; i++) {
            final Button btn = new Button(activity);
            btn.setText(toolNames[i]); btn.setTextColor(Color.parseColor("#FFFFFFFF")); btn.setAllCaps(false);
            GradientDrawable gd = new GradientDrawable(); gd.setColor(Color.parseColor("#FF333333")); gd.setCornerRadius(dp(activity, 10));
            btn.setBackground(gd);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(activity, 90), dp(activity, 48));
            bLp.setMargins(dp(activity, 6), 0, dp(activity, 6), 0);

            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    vibrate(activity, 40);
                    String action = btn.getText().toString();
                    if (action.equals("取消")) {
                        dialog.dismiss();
                    } else if (action.equals("撤销")) {
                        if (idx[0] > 0) {
                            idx[0]--; preview.setImageBitmap(getCompressedPreview((Bitmap)history.get(idx[0]), quality[0]));
                        }
                    } else if (action.equals("重做")) {
                        if (idx[0] < history.size() - 1) {
                            idx[0]++; preview.setImageBitmap(getCompressedPreview((Bitmap)history.get(idx[0]), quality[0]));
                        }
                    } else if (action.equals("旋转")) {
                        Matrix m = new Matrix(); m.postRotate(90);
                        Bitmap cur = (Bitmap)history.get(idx[0]);
                        Bitmap next = Bitmap.createBitmap(cur, 0, 0, cur.getWidth(), cur.getHeight(), m, true);
                        while (history.size() > idx[0] + 1) history.remove(history.size() - 1);
                        history.add(next); idx[0]++; preview.setImageBitmap(getCompressedPreview(next, quality[0]));
                    } else if (action.equals("裁剪")) {
                        crop.setVisibility(crop.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                    } else if (action.startsWith("质量")) {
                        if (quality[0] == 100) { quality[0] = 85; btn.setText("质量: 高"); }
                        else if (quality[0] == 85) { quality[0] = 60; btn.setText("质量: 中"); }
                        else { quality[0] = 100; btn.setText("质量: 原画"); }
                        preview.setImageBitmap(getCompressedPreview((Bitmap)history.get(idx[0]), quality[0]));
                    } else if (action.equals("保存")) {
                        Bitmap res = (Bitmap)history.get(idx[0]);
                        if (crop.getVisibility() == View.VISIBLE) {
                            Rect r = crop.getRealRect(res, preview.matrix);
                            int nx = Math.max(0, r.left), ny = Math.max(0, r.top);
                            int nw = Math.min(res.getWidth() - nx, r.width());
                            int nh = Math.min(res.getHeight() - ny, r.height());
                            if (nw > 10 && nh > 10) res = Bitmap.createBitmap(res, nx, ny, nw, nh);
                        }
                        performSave(activity, res, savePath, quality[0]);
                        dialog.dismiss();
                    }
                }
            });
            toolBar.addView(btn, bLp);
        }
        scroll.addView(toolBar);
        FrameLayout.LayoutParams sLp = new FrameLayout.LayoutParams(-1, -2);
        sLp.gravity = Gravity.BOTTOM; sLp.bottomMargin = dp(activity, 30);
        root.addView(scroll, sLp);

        // 内存回收监听
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                for (int i = 0; i < history.size(); i++) {
                    Bitmap b = (Bitmap) history.get(i);
                    if (b != null && !b.isRecycled()) b.recycle();
                }
                history.clear();
            }
        });

        dialog.setContentView(root);
        dialog.show();
    } catch (Throwable e) {
        Toast("启动失败: " + e.getMessage());
    }
}

/** 执行保存操作 */
void performSave(final Activity activity, final Bitmap b, final String p, final int q) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                File f = new File(p); if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
                FileOutputStream o = new FileOutputStream(f);
                b.compress(Bitmap.CompressFormat.JPEG, q, o);
                o.flush(); o.close();
                activity.runOnUiThread(new Runnable() { public void run() { Toast("保存成功 (" + q + "%)"); 刷新悬浮窗(); } });
            } catch (Throwable e) {}
        }
    });
}

/** 编辑并保存图片入口 */
void editAndSaveImage(final Activity activity, final Uri uri, final String p, final int c) {
    // GIF判定
    if (p.toLowerCase().endsWith(".gif")) {
        ThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    InputStream is = activity.getContentResolver().openInputStream(uri);
                    File f = new File(p); if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
                    FileOutputStream os = new FileOutputStream(f);
                    byte[] buf = new byte[8192]; int len;
                    while ((len = is.read(buf)) != -1) os.write(buf, 0, len);
                    os.flush(); os.close(); is.close();
                    activity.runOnUiThread(new Runnable() { public void run() { Toast("GIF动图已保存"); 刷新悬浮窗(); } });
                } catch (Exception e) {}
            }
        });
        return;
    }

    ThreadPool.execute(new Runnable() {
        public void run() {
            final Bitmap b = loadBitmapFromUri(activity, uri);
            if (b == null) return;
            activity.runOnUiThread(new Runnable() { public void run() { showEditDialog(activity, b, p, c); } });
        }
    });
}

static boolean isFilePickerHooked = false;
/** Hook 文件选择器结果 */
void hookFilePicker(final Activity activity, final int requestCode, final String savePath) {
    if (isFilePickerHooked) return;
    try {
        isFilePickerHooked = true;
        XposedBridge.hookMethod(Activity.class.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class), new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Activity act = (Activity) param.thisObject;
                            if (act.equals(activity) && (Integer)param.args[1] == Activity.RESULT_OK && (Integer)param.args[0] == requestCode) {
                                Intent d = (Intent)param.args[2]; if (d == null) return;
                                Uri u = d.getData(); if (u == null) return;
                                String type = act.getContentResolver().getType(u);
                                String ext = getExtensionFromMimeType(type);
                                String path = savePath.contains("{ext}") ? savePath.replace("{ext}", ext) : (savePath.lastIndexOf('.') > 0 ? savePath.substring(0, savePath.lastIndexOf('.')) + ext : savePath + ext);
                                if (requestCode == 1005) {
                                putString("settings", "iconPath", path);
                                }
                                act.runOnUiThread(new Runnable() { public void run() { editAndSaveImage(act, u, path, requestCode); } });
                            }
                        } catch (Throwable e) {}
                    }
                });
            }
        });
    } catch (Throwable e) {}
}

/** 判断颜色是否为深色 */
public boolean isColorDark(int color) {
    double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
    return darkness >= 0.5; 
}


/**
 * RenderScript模糊 (支持原图修改和输出Bitmap两种模式)
 * @param activity 上下文
 * @param bitmap 源 Bitmap
 * @param outBitmap 输出 Bitmap (可为空，则修改原图)
 * @param radius 模糊半径
 * @return 模糊后的 Bitmap
 */
public Bitmap blurBitmap(Activity activity, Bitmap bitmap, Bitmap outBitmap, float radius) {
    if (radius <= 0 || bitmap == null || bitmap.isRecycled()) return bitmap;
    if (radius > 25) radius = 25;
    
    if (outBitmap == null) {
        outBitmap = bitmap;
    }
    
    RenderScript rs = null;
    try {
        rs = RenderScript.create(activity);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        
        blurScript.setRadius(radius);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outBitmap);
        
        return outBitmap;
    } catch (Throwable e) {
        try {
            return fastblur(bitmap, (int) Math.min(7, radius));
        } catch (Exception ex) {
            return bitmap;
        }
    } finally {
        if (rs != null) {
            try { rs.destroy(); } catch (Exception e) {}
        }
    }
}

/**
 * 快速模糊 (StackBlur算法的简化版)
 * @param bmp 源图片
 * @param radius 模糊半径
 * @return 模糊后的图片
 */
public Bitmap fastblur(Bitmap bmp, int radius) {
    if (bmp == null || radius <= 0) return bmp;
    
    int w = bmp.getWidth();
    int h = bmp.getHeight();
    
    if (radius > w / 2) radius = w / 2;
    if (radius > h / 2) radius = h / 2;
    
    int smallW = Math.max(1, w / (radius + 1));
    int smallH = Math.max(1, h / (radius + 1));
    Bitmap small = Bitmap.createScaledBitmap(bmp, smallW, smallH, true);
    
    int[] pixels = new int[smallW * smallH];
    small.getPixels(pixels, 0, smallW, 0, 0, smallW, smallH);
    
    for (int i = 0; i < 3; i++) {
        for (int y = 0; y < smallH; y++) {
            for (int x = 0; x < smallW; x++) {
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        if (ny >= 0 && ny < smallH && nx >= 0 && nx < smallW) {
                            int pixel = pixels[ny * smallW + nx];
                            r += Color.red(pixel);
                            g += Color.green(pixel);
                            b += Color.blue(pixel);
                            a += Color.alpha(pixel);
                            count++;
                        }
                    }
                }
                pixels[y * smallW + x] = Color.argb(a / count, r / count, g / count, b / count);
            }
        }
    }
    
    small.setPixels(pixels, 0, smallW, 0, 0, smallW, smallH);
    
    Bitmap result = Bitmap.createScaledBitmap(small, w, h, true);
    if (small != result && !small.isRecycled()) {
        small.recycle();
    }
    
    return result;
}

/**
 * 调整Bitmap大小，最大边不超过maxSize，保持比例
 * @param original 原图
 * @param maxSize 最大边长
 * @return 调整后的图
 */
public Bitmap resizeBitmap(Bitmap original, int maxSize) {
    if (original == null || original.isRecycled()) return null;
    
    int width = original.getWidth();
    int height = original.getHeight();
    
    if (width <= maxSize && height <= maxSize) {
        return original;
    }
    
    float scale = Math.min((float) maxSize / width, (float) maxSize / height);
    int newWidth = Math.round(width * scale);
    int newHeight = Math.round(height * scale);
    
    return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
}

/**
 * 居中裁剪Bitmap到目标尺寸 (CenterCrop)
 * @param source 源图
 * @param targetWidth 目标宽
 * @param targetHeight 目标高
 * @return 裁剪后的图
 */
public Bitmap centerCropBitmap(Bitmap source, int targetWidth, int targetHeight) {
    if (source == null || source.isRecycled()) return null;
    
    int sourceW = source.getWidth();
    int sourceH = source.getHeight();
    
    if (sourceW == targetWidth && sourceH == targetHeight) {
        return source;
    }
    
    float scale = Math.max((float) targetWidth / sourceW, (float) targetHeight / sourceH);
    float scaledW = sourceW * scale;
    float scaledH = sourceH * scale;
    
    float left = (targetWidth - scaledW) / 2;
    float top = (targetHeight - scaledH) / 2;
    
    Bitmap dest = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(dest);
    
    RectF sourceRect = new RectF(0, 0, sourceW, sourceH);
    RectF targetRect = new RectF(left, top, left + scaledW, top + scaledH);
    
    Matrix matrix = new Matrix();
    matrix.setRectToRect(sourceRect, targetRect, Matrix.ScaleToFit.FILL);
    
    Paint paint = new Paint();
    paint.setAntiAlias(true);
    paint.setFilterBitmap(true);
    canvas.drawBitmap(source, matrix, paint);
    
    return dest;
}

/**
 * 优化智能缩放算法（CenterCrop确保填满目标区域，不拉伸）
 * @param source 源图
 * @param targetW 目标宽
 * @param targetH 目标高
 * @return 缩放裁剪后的图
 */
Bitmap smartScaleBitmap(Bitmap source, int targetW, int targetH) {
    if (source == null || source.isRecycled()) return null;
    if (targetW <= 0 || targetH <= 0) return null;
    
    try {
        int sourceW = source.getWidth();
        int sourceH = source.getHeight();
        if (sourceW == 0 || sourceH == 0) return null;
        
        float sourceRatio = (float) sourceW / sourceH;
        float targetRatio = (float) targetW / targetH;
        
        Bitmap cropped;
        if (sourceRatio > targetRatio) {
            int cropW = (int) (sourceH * targetRatio);
            int cropX = (sourceW - cropW) / 2;
            cropped = Bitmap.createBitmap(source, cropX, 0, cropW, sourceH);
        } else {
            int cropH = (int) (sourceW / targetRatio);
            int cropY = (sourceH - cropH) / 2;
            cropped = Bitmap.createBitmap(source, 0, cropY, sourceW, cropH);
        }
        
        if (cropped.getWidth() != targetW || cropped.getHeight() != targetH) {
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true);
            if (cropped != source) {
                cropped.recycle();
            }
            return scaled;
        }
        
        return cropped;
        
    } catch (OutOfMemoryError e) {
        return null;
    } catch (Exception e) {
        return null;
    }
}

Bitmap scaleToFitBitmap(Bitmap source, int targetW, int targetH) {
    if (source == null || source.isRecycled()) return null;
    
    try {
        int sourceW = source.getWidth();
        int sourceH = source.getHeight();
        
        float scale = Math.min((float) targetW / sourceW, (float) targetH / sourceH);
        int scaledW = Math.round(sourceW * scale);
        int scaledH = Math.round(sourceH * scale);
        
        Bitmap result = Bitmap.createScaledBitmap(source, scaledW, scaledH, true);
        return result;
        
    } catch (Exception e) {
        return source;
    }
}

Bitmap smartCenterCropBitmap(Bitmap source, int targetW, int targetH) {
    return smartScaleBitmap(source, targetW, targetH);
}


/** 生成缓存键 */
String generateCacheKey(String imgPath, int blurRadius, int overlayAlpha, boolean isDark, int targetW, int targetH) {
    File f = new File(imgPath);
    long lastMod = f.exists() ? f.lastModified() : 0;
    long size = f.exists() ? f.length() : 0;
    int widthGroup = (targetW / 20) * 20;
    int heightGroup = (targetH / 20) * 20;
    return imgPath.hashCode() + "_" + lastMod + "_" + size + "_" + blurRadius + "_" + 
           overlayAlpha + "_" + (isDark ? "1" : "0") + "_" + widthGroup + "_" + heightGroup;
}

String generateBaseCacheKey(String imgPath, int blurRadius, int overlayAlpha, boolean isDark) {
    File f = new File(imgPath);
    long lastMod = f.exists() ? f.lastModified() : 0;
    return imgPath.hashCode() + "_" + lastMod + "_" + blurRadius + "_" + overlayAlpha + "_" + (isDark ? "1" : "0");
}

Drawable findSimilarCache(String baseKey, int targetW, int targetH) {
    Iterator iterator = cacheOrder.keySet().iterator();
    while (iterator.hasNext()) {
        String key = (String) iterator.next();
        if (key.startsWith(baseKey + "_")) {
            try {
                String[] parts = key.split("_");
                int cachedW = Integer.parseInt(parts[parts.length - 2]);
                int cachedH = Integer.parseInt(parts[parts.length - 1]);
                
                float widthDiff = Math.abs(cachedW - targetW) / (float) targetW;
                float heightDiff = Math.abs(cachedH - targetH) / (float) targetH;
                
                if (widthDiff <= 0.2f && heightDiff <= 0.2f) {
                    CacheEntry entry = (CacheEntry) imageCache.get(key);
                    if (entry != null && entry.drawable != null) {
                        return entry.drawable;
                    }
                }
            } catch (Exception e) {}
        }
    }
    return null;
}

boolean checkAndCleanupMemoryIfNeeded() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    
    float usedRatio = (float) usedMemory / maxMemory;
    
    if (usedRatio > 0.85f) {
        if (!cacheOrder.isEmpty()) {
            String oldestKey = (String) cacheOrder.keySet().iterator().next();
            removeFromCache(oldestKey);
            return true;
        }
    }
    
    return false;
}

/** 缓存条目类 */
static class CacheEntry {
    Drawable drawable;
    long createdTime;
    long lastAccessTime;
    int width;
    int height;
    String cacheKey;
    int accessCount;
    
    CacheEntry(Drawable d, String key, int w, int h) {
        this.drawable = d;
        this.cacheKey = key;
        this.width = w;
        this.height = h;
        this.createdTime = System.currentTimeMillis();
        this.lastAccessTime = this.createdTime;
        this.accessCount = 0;
    }
    
    void updateAccess() {
        this.lastAccessTime = System.currentTimeMillis();
        this.accessCount++;
    }
}

static ConcurrentHashMap imageCache = new ConcurrentHashMap();
static LinkedHashMap cacheOrder = new LinkedHashMap(10, 0.75f, true);
static Drawable globalCachedDrawable = null;
static String globalCachedParams = "";
private static final Object BG_LOCK = new Object();
private static final Object CACHE_LOCK = new Object();
private static volatile boolean isBgLoading = false;
private static volatile boolean isDialogShowing = false;
static WeakHashMap scaledViews = new WeakHashMap();
static final int MAX_CACHE_SIZE = 10;

static int currentTextColor = Color.WHITE;
static boolean currentIsDark = true;

String md5(String input) {
    if (input == null || input.isEmpty()) return "";
    try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    } catch (Exception e) {
        return input;
    }
}

Drawable getCachedDrawable(String cacheKey) {
    if (cacheKey == null || cacheKey.isEmpty()) return null;
    
    CacheEntry entry = (CacheEntry) imageCache.get(cacheKey);
    if (entry != null && entry.drawable != null) {
        cacheOrder.remove(cacheKey);
        cacheOrder.put(cacheKey, entry);
        entry.updateAccess();
        return entry.drawable;
    }
    return null;
}

void putToCache(Drawable drawable, String cacheKey, int width, int height) {
    if (drawable == null || cacheKey == null || cacheKey.isEmpty()) return;
    
    CacheEntry existing = (CacheEntry) imageCache.get(cacheKey);
    if (existing != null) {
        existing.updateAccess();
        cacheOrder.remove(cacheKey);
        cacheOrder.put(cacheKey, existing);
        return;
    }
    
    while (imageCache.size() >= MAX_CACHE_SIZE && !cacheOrder.isEmpty()) {
        String oldestKey = (String) cacheOrder.keySet().iterator().next();
        removeFromCache(oldestKey);
    }
    
    CacheEntry entry = new CacheEntry(drawable, cacheKey, width, height);
    imageCache.put(cacheKey, entry);
    cacheOrder.put(cacheKey, entry);
}

void removeFromCache(String cacheKey) {
    if (cacheKey == null) return;
    
    cacheOrder.remove(cacheKey);
    
    CacheEntry entry = (CacheEntry) imageCache.remove(cacheKey);
    if (entry != null && entry.drawable != null) {
        try {
            if (entry.drawable instanceof BitmapDrawable) {
                BitmapDrawable bd = (BitmapDrawable) entry.drawable;
                Bitmap bitmap = bd.getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } catch (Exception e) {}
    }
}

void unloadBackgroundCache() {
    imageCache.clear();
    cacheOrder.clear();
    scaledViews.clear();
    globalCachedDrawable = null;
    globalCachedParams = "";
}

void forceUnloadAllCache() {
    unloadBackgroundCache();
}


String getSetting(String table, String key, String defaultValue) {
    if (pendingSettingsChanges != null && pendingSettingsChanges.containsKey(key)) {
        Object val = pendingSettingsChanges.get(key);
        return val != null ? val.toString() : defaultValue;
    }
    return getString(table, key, defaultValue);
}

boolean getSettingBoolean(String table, String key, boolean defaultValue) {
    if (pendingSettingsChanges != null && pendingSettingsChanges.containsKey(key)) {
        Object val = pendingSettingsChanges.get(key);
        if (val != null) {
            String strVal = val.toString();
            if ("true".equalsIgnoreCase(strVal)) return true;
            if ("false".equalsIgnoreCase(strVal)) return false;
        }
    }
    return getBoolean(table, key, defaultValue);
}

/**
 * 创建手绘风格的开关控件（48dp × 28dp）
 * @param ctx       Context（通常传入 Activity）
 * @param initVal   初始状态（true = 开，false = 关）
 * @return Object[] { View 开关控件, boolean[] 当前状态数组（长度1，可修改） }
 */
public Object[] createSwitchViewWithState(Context ctx, boolean initVal) {
    FrameLayout swContainer = new FrameLayout(ctx);
    int swW = dp(ctx, 48);
    int swH = dp(ctx, 28);
    FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(swW, swH);
    swContainer.setLayoutParams(containerLp);

    // 简单按下涟漪效果
    ColorStateList rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000"));
    RippleDrawable ripple = new RippleDrawable(rippleColor, null, null);
    swContainer.setBackground(ripple);

    swContainer.setClickable(true);
    swContainer.setFocusable(true);

    // 轨道
    View track = new View(ctx);
    FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(-1, -1);
    track.setLayoutParams(trackLp);
    GradientDrawable trackBg = new GradientDrawable();
    trackBg.setCornerRadius(dp(ctx, 14));
    track.setBackground(trackBg);
    swContainer.addView(track);

    // 滑块
    View thumb = new View(ctx);
    int thumbSize = dp(ctx, 24);
    int margin = dp(ctx, 2);
    FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(thumbSize, thumbSize);
    thumbLp.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
    thumbLp.setMargins(margin, 0, margin, 0);
    thumb.setLayoutParams(thumbLp);

    GradientDrawable thumbBg = new GradientDrawable();
    thumbBg.setColor(Color.WHITE);
    thumbBg.setCornerRadius(dp(ctx, 12));
    thumb.setBackground(thumbBg);
    swContainer.addView(thumb);

    final boolean[] state = new boolean[]{initVal};
    final View finalThumb = thumb;
    final GradientDrawable finalTrackBg = trackBg;

    final Runnable updateUI = new Runnable() {
        public void run() {
            boolean isOn = state[0];
            finalTrackBg.setColor(isOn ? Color.parseColor("#34C759") : Color.parseColor("#E5E5E5"));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) finalThumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | (isOn ? Gravity.RIGHT : Gravity.LEFT);
            finalThumb.setLayoutParams(lp);
            finalThumb.invalidate();
            track.invalidate();
        }
    };

    updateUI.run();

    swContainer.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            state[0] = !state[0];
            updateUI.run();
            traceLog("qzone_log", "自定义开关点击，新状态: " + state[0]);
        }
    });

    return new Object[]{swContainer, state};
}

boolean isEffectiveDarkMode(Activity activity) {
    String mode = getSetting("settings", "ui_theme_mode", "default");
    if ("default".equals(mode)) {
        return getSettingBoolean("settings", "黑白", false);
    }
    if ("dark".equals(mode)) return true;
    if ("light".equals(mode)) return false;
    boolean manualDark = getSettingBoolean("settings", "黑白", false);
    if (manualDark) return true;
    try {
        int uiMode = activity.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    } catch (Exception e) { return false; }
}

boolean isValidHexColor(String colorCode) {
    if (colorCode == null || colorCode.trim().isEmpty()) return false;
    return Pattern.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$", colorCode.trim());
}

boolean isValidGradientString(String gradientStr) {
    if (gradientStr == null || gradientStr.trim().isEmpty()) {
        return false;
    }
    
    String cleanStr = gradientStr.trim();
    String[] colors = cleanStr.split(",");
    
    if (colors.length < 2) {
        return false;
    }
    
    for (String color : colors) {
        String trimmedColor = color.trim();
        if (trimmedColor.isEmpty()) {
            return false;
        }
        
        boolean isValid = false;
        
        if (trimmedColor.startsWith("#")) {
            if (trimmedColor.length() == 7 || trimmedColor.length() == 9) {
                isValid = Pattern.matches("^#[A-Fa-f0-9]{6}$", trimmedColor) || 
                          Pattern.matches("^#[A-Fa-f0-9]{8}$", trimmedColor);
            } else if (trimmedColor.length() == 4) {
                isValid = Pattern.matches("^#[A-Fa-f0-9]{3}$", trimmedColor);
            }
        }
        
        if (!isValid) {
            return false;
        }
    }
    
    return true;
}

Typeface getCustomTypeface(String typeName) {
    if ("serif".equals(typeName)) return Typeface.SERIF;
    if ("sans".equals(typeName)) return Typeface.SANS_SERIF;
    if ("monospace".equals(typeName)) return Typeface.MONOSPACE;
    if ("bold".equals(typeName)) return Typeface.DEFAULT_BOLD;
    return Typeface.DEFAULT;
}

void applyUiTheme(final Activity activity, final AlertDialog dialog) {
    if (dialog == null) return;
    
    final Window window = dialog.getWindow();
    if (window == null) return;
    
    applyDialogSize(activity, window);
    
    final View decorView = window.getDecorView();
    int dialogW = decorView.getWidth();
    int dialogH = decorView.getHeight();
    
    if (dialogW <= 0 || dialogH <= 0) {
        float density = activity.getResources().getDisplayMetrics().density;
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        dialogW = Math.min((int) (300 * density), screenWidth - (int) (48 * density));
        dialogH = (int) (800 * density);
    }
    
    executeApplyTheme(activity, dialog, dialogW, dialogH);
}

void executeApplyTheme(final Activity activity, final AlertDialog dialog, final int dialogW, final int dialogH) {
    if (dialog == null || dialog.getWindow() == null) {
        return;
    }
    
    final Window window = dialog.getWindow();
    final boolean isDark = isEffectiveDarkMode(activity);
    currentIsDark = isDark;
    
    try {
        final String bgType = getSetting("settings", "ui_bg_type", "color");
        final String bgColor = getSetting("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
            isDark ? "#FF1E1E1E" : "#FFFFFFFF");
        final String bgGradient = getSetting("settings", isDark ? "ui_bg_gradient_dark" : "ui_bg_gradient_light", 
            isDark ? "#FF2C2C2C,#FF121212,#FF2C2C2C" : "#FFFFFFFF,#FFF5F5F5,#FFFFFFFF");
        final String textColorUser = getSetting("settings", isDark ? "ui_text_color_dark" : "ui_text_color_light", "");
        final String fontType = getSetting("settings", "ui_font_type", "default");
        final String imgPath = pluginPath + "/API/background.png";
        
        float fSize = 1.0f;
        try { fSize = Float.parseFloat(getSetting("settings", "ui_font_size", "1.0")); } catch(Exception e){}
        final float fontSizeScale = fSize;
        final Typeface tf = getCustomTypeface(fontType);
        
        int blurR = 0, alpha = 100;
        try { blurR = Integer.parseInt(getSetting("settings", "ui_img_blur", "0")); } catch(Exception e){}
        try { alpha = Integer.parseInt(getSetting("settings", "ui_img_alpha", isDark ? "180" : "100")); } catch(Exception e){}
        final int blurRadius = Math.max(0, Math.min(25, blurR));
        final int overlayAlpha = Math.max(0, Math.min(255, alpha));
        
        String rawBgColor = getSetting("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
            isDark ? "#FF1E1E1E" : "#FFFFFFFF");
        String validFallbackColor = isValidHexColor(rawBgColor) ? rawBgColor : (isDark ? "#FF1E1E1E" : "#FFFFFFFF");
        
        if ("gradient".equals(bgType) && isValidGradientString(bgGradient)) {
            applyGradientBackground(activity, window, bgGradient, validFallbackColor, isDark);
        } else {
            applyFallbackBg(activity, window, validFallbackColor, isDark);
        }
        
        if ("image".equals(bgType)) {
            applyFallbackBg(activity, window, isDark ? "#FF1E1E1E" : "#FFFFFFFF", isDark);
            
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                public void run() {
                    ThreadPool.execute(new Runnable() {
                        public void run() {
                            try {
                                final View decorView = window.getDecorView();
                                int actualW = decorView.getWidth();
                                int actualH = decorView.getHeight();
                                if (actualW <= 0 || actualH <= 0) {
                                    actualW = dialogW;
                                    actualH = dialogH;
                                }
                                
                                String baseKey = generateBaseCacheKey(imgPath, blurRadius, overlayAlpha, isDark);
                                Drawable cached = findSimilarCache(baseKey, actualW, actualH);
                                
                                if (cached != null) {
                                    applyDrawableWithTransition(activity, window, cached);
                                    return;
                                }
                                
                                File f = new File(imgPath);
                                if (!f.exists()) return;
                                
                                BitmapFactory.Options opts = new BitmapFactory.Options();
                                opts.inSampleSize = 2;
                                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                
                                Bitmap origin = BitmapFactory.decodeFile(imgPath, opts);
                                if (origin == null) throw new Exception("解码失败");
                                
                                Bitmap scaled = centerCropBitmap(origin, actualW, actualH);
                                if (scaled == null) scaled = smartScaleBitmap(origin, actualW, actualH);
                                if (scaled == null) scaled = Bitmap.createScaledBitmap(origin, actualW, actualH, true);
                                
                                Bitmap finalBitmap = scaled;
                                if (blurRadius > 0 && scaled.getWidth() > 200) {
                                    Bitmap outBmp = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
                                    finalBitmap = blurBitmap(activity, scaled, outBmp, blurRadius);
                                }
                                
                                int maskColor = isDark ? Color.BLACK : Color.WHITE;
                                ColorDrawable mask = new ColorDrawable(Color.argb(overlayAlpha, 
                                    Color.red(maskColor), Color.green(maskColor), Color.blue(maskColor)));
                                BitmapDrawable bd = new BitmapDrawable(activity.getResources(), finalBitmap);
                                final LayerDrawable ld = new LayerDrawable(new Drawable[]{bd, mask});
                                
                                String cacheKey = generateCacheKey(imgPath, blurRadius, overlayAlpha, isDark, actualW, actualH);
                                putToCache(ld, cacheKey, actualW, actualH);
                                
                                applyDrawableWithTransition(activity, window, ld);
                                
                                if (origin != scaled && !origin.isRecycled()) {
                                    origin.recycle();
                                }
                                
                            } catch (Throwable e) {}
                        }
                    });
                }
            }, 100);
        }
        
        String rawBgColor2 = getSetting("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
            isDark ? "#FF1E1E1E" : "#FFFFFFFF");
        String validBgColor = isValidHexColor(rawBgColor2) ? rawBgColor2 : (isDark ? "#FF1E1E1E" : "#FFFFFFFF");
        if ("gradient".equals(bgType)) {
            boolean isValidGrad = isValidGradientString(bgGradient);
            if (isValidGrad) {
                applyGradientBackground(activity, window, bgGradient, validBgColor, isDark);
            } else {
                applyFallbackBg(activity, window, validBgColor, isDark);
            }
        } else {
            applyFallbackBg(activity, window, validBgColor, isDark);
        }
        
        final int calculatedTextColor;
        boolean isBgDark = isDark;
        if ("color".equals(bgType) && isValidHexColor(bgColor)) {
            isBgDark = isColorDark(Color.parseColor(bgColor.trim()));
        }
        
        if (isValidHexColor(textColorUser)) {
            calculatedTextColor = Color.parseColor(textColorUser);
        } else {
            calculatedTextColor = isBgDark ? Color.parseColor("#FFEFEFEF") : Color.parseColor("#FF333333");
        }
        currentTextColor = calculatedTextColor;
        
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                updateViewStylesRecursively(window.getDecorView(), calculatedTextColor, tf, fontSizeScale);
            }
        });
        
    } catch (Throwable e) {
        try { e.printStackTrace(); } catch (Exception ex) {}
    }
}

void applyDrawableWithTransition(final Activity activity, final Window window, final Drawable newDrawable) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                View decorView = window.getDecorView();
                Drawable currentBg = decorView.getBackground();
                
                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{
                    currentBg != null ? currentBg : new ColorDrawable(Color.TRANSPARENT),
                    newDrawable
                });
                
                window.setBackgroundDrawable(transitionDrawable);
                transitionDrawable.startTransition(300);
                
                clearInnerBackgrounds(decorView);
                applyWindowRadius(activity, window);
                
            } catch (Throwable e) {
                window.setBackgroundDrawable(newDrawable);
            }
        }
    });
}

void applyImageBackgroundFast(final Activity activity, final Window window, 
                             final String imgPath, final int blurRadius, 
                             final int overlayAlpha, final boolean isDark,
                             final int dialogW, final int dialogH) {
    try {
        File f = new File(imgPath);
        boolean exists = f.exists();
        
        if (!exists) {
            applyFallbackBg(activity, window, isDark ? "#FF1E1E1E" : "#FFFFFFFF", isDark);
            return;
        }
        
        applyFallbackBg(activity, window, isDark ? "#FF1E1E1E" : "#FFFFFFFF", isDark);
        
        final int finalW = dialogW > 0 ? dialogW : 814;
        final int finalH = dialogH > 0 ? dialogH : 806;
        
        ThreadPool.execute(new Runnable() {
            public void run() {
                loadImageOptimized(activity, window, imgPath, blurRadius, overlayAlpha, isDark, finalW, finalH);
            }
        });
        
    } catch (Throwable e) {
        applyFallbackBg(activity, window, isDark ? "#FF1E1E1E" : "#FFFFFFFF", isDark);
    }
}

void loadImageOptimized(final Activity activity, final Window window, 
                       final String imgPath, final int blurRadius, 
                       final int overlayAlpha, final boolean isDark,
                       final int preWidth, final int preHeight) {
    
    int targetW = preWidth;
    int targetH = preHeight;
    
    try {
        String baseKey = generateBaseCacheKey(imgPath, blurRadius, overlayAlpha, isDark);
        Drawable cached = findSimilarCache(baseKey, targetW, targetH);
        if (cached != null) {
            applyDrawableWithoutTextRecalc(activity, window, cached);
            synchronized (BG_LOCK) { isBgLoading = false; }
            return;
        }
        
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 2;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inJustDecodeBounds = false;
        
        Bitmap origin = BitmapFactory.decodeFile(imgPath, opts);
        if (origin == null) {
            throw new Exception("图片解码失败");
        }
        
        int sourceW = origin.getWidth();
        int sourceH = origin.getHeight();
        
        if (origin.getConfig() != Bitmap.Config.ARGB_8888) {
            Bitmap argb8888 = origin.copy(Bitmap.Config.ARGB_8888, false);
            if (origin != argb8888) {
                origin.recycle();
            }
            origin = argb8888;
        }
        
        Bitmap scaled = centerCropBitmap(origin, targetW, targetH);
        
        if (scaled == null) {
            scaled = smartScaleBitmap(origin, targetW, targetH);
        }
        if (scaled == null) {
            scaled = Bitmap.createScaledBitmap(origin, targetW, targetH, true);
        }
        
        if (scaled != null) {
            if (scaled.getConfig() != Bitmap.Config.ARGB_8888) {
                Bitmap argb8888 = scaled.copy(Bitmap.Config.ARGB_8888, false);
                if (scaled != origin) {
                    scaled.recycle();
                }
                scaled = argb8888;
            }
        }
        
        Bitmap finalBitmap = scaled;
        if (blurRadius > 0 && scaled.getWidth() > 200) {
            Bitmap outBmp = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
            finalBitmap = blurBitmap(activity, scaled, outBmp, blurRadius);
            if (outBmp != finalBitmap && scaled != outBmp && !scaled.isRecycled()) {
                scaled.recycle();
            }
        }
        
        int maskColor = isDark ? Color.BLACK : Color.WHITE;
        ColorDrawable mask = new ColorDrawable(Color.argb(overlayAlpha, 
            Color.red(maskColor), Color.green(maskColor), Color.blue(maskColor)));
        BitmapDrawable bd = new BitmapDrawable(activity.getResources(), finalBitmap);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{bd, mask});
        
        String cacheKey = generateCacheKey(imgPath, blurRadius, overlayAlpha, isDark, targetW, targetH);
        putToCache(ld, cacheKey, targetW, targetH);
        
        applyDrawableWithoutTextRecalc(activity, window, ld);
        
        if (origin != scaled && !origin.isRecycled()) {
            origin.recycle();
        }
        
    } catch (Throwable e) {}
    finally {
        synchronized (BG_LOCK) {
            isBgLoading = false;
        }
    }
}

void applyDrawableWithoutTextRecalc(final Activity activity, final Window window, final Drawable drawable) {
    final View decorView = window != null ? window.getDecorView() : null;
    
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (window == null || drawable == null) {
                    return;
                }
                
                int w = decorView != null ? decorView.getWidth() : 0;
                int h = decorView != null ? decorView.getHeight() : 0;
                
                if (w > 0 && h > 0) {
                    drawable.setBounds(0, 0, w, h);
                }
                
                window.setBackgroundDrawable(drawable);
                
                clearInnerBackgrounds(decorView);
                
                applyWindowRadius(activity, window);
                
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    });
}

void finalizeTextStyle(final Activity activity, final View decorView, final boolean isDark, final boolean forceDark) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                int textColor = forceDark ? Color.parseColor("#FFEFEFEF") : Color.parseColor("#FF333333");
                currentTextColor = textColor;
                currentIsDark = forceDark;
                
                Typeface tf = getCustomTypeface(getString("settings", "ui_font_type", "default"));
                float fSize = 1.0f;
                try { fSize = Float.parseFloat(getString("settings", "ui_font_size", "1.0")); } catch(Exception e){}
                final float fontSizeScale = fSize;
                updateViewStylesRecursively(decorView, textColor, tf, fontSizeScale);
            } catch (Exception e) {}
        }
    });
}

void clearInnerBackgrounds(View view) {
    if (view == null) return;
    
    try {
        if (view instanceof ViewGroup) {
            String clsName = view.getClass().getSimpleName();
            if (clsName.contains("Decor") || clsName.contains("Content") || clsName.contains("Dialog")) {
                Drawable bg = view.getBackground();
                if (bg == null || isDrawableTransparent(bg)) {
                    view.setBackground(null);
                }
            }
            
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearInnerBackgrounds(vg.getChildAt(i));
            }
        }
    } catch (Exception e) {}
}

void applyDialogSize(final Activity activity, final Window window) {
    try {
        float scale = 1.0f;
        try {
            String scaleStr = getSetting("settings", "ui_dialog_scale", "1.0");
            scale = Float.parseFloat(scaleStr);
        } catch (Exception e) {}
        
        float density = activity.getResources().getDisplayMetrics().density;
        
        int customWidth = -1;
        try {
            String widthStr = getSetting("settings", "ui_dialog_width", "");
            if (!widthStr.isEmpty()) {
                customWidth = (int) (Float.parseFloat(widthStr) * density);
            }
        } catch (Exception e) {}
        
        int dialogWidth;
        if (customWidth > 0) {
            dialogWidth = customWidth;
        } else {
            int maxWidth = (int) (260 * density);
            int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
            dialogWidth = (int) (Math.min(maxWidth, screenWidth - (int) (48 * density)) * scale);
        }
        
        int dialogHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
        
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = dialogWidth;
        params.height = dialogHeight;
        window.setAttributes(params);
        
    } catch (Exception e) {}
}

boolean isDrawableTransparent(Drawable drawable) {
    if (drawable == null) return true;
    if (drawable instanceof ColorDrawable) {
        return ((ColorDrawable) drawable).getAlpha() < 10;
    }
    return false;
}

void applyWindowRadius(final Activity activity, final Window window) {
    try {
        final View decor = window != null ? window.getDecorView() : null;
        final float radius = 16 * activity.getResources().getDisplayMetrics().density;
        if (decor != null) {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                decor.setClipToOutline(true);
                decor.setOutlineProvider(new ViewOutlineProvider() {
                    public void getOutline(View view, Outline outline) {
                        try {
                            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                        } catch (Exception e) {}
                    }
                });
            }
        }
    } catch (Exception e) {}
}

void applyGradientBackground(Activity activity, Window window, String gradientStr, String fallbackColor, boolean isDark) {
    try {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(activity, 16));
        
        if (gradientStr != null && !gradientStr.trim().isEmpty()) {
            String[] colors = gradientStr.split(",");
            if (colors.length >= 2) {
                int[] colorsInt = new int[colors.length];
                boolean valid = true;
                for (int i = 0; i < colors.length; i++) {
                    try {
                        String colorStr = colors[i].trim();
                        if (!colorStr.startsWith("#")) {
                            colorStr = "#" + colorStr;
                        }
                        colorsInt[i] = Color.parseColor(colorStr);
                    } catch (Exception e) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    bg.setColors(colorsInt);
                    bg.setOrientation(GradientDrawable.Orientation.TL_BR);
                    applyDrawableToWindow(window, bg, activity, isDark);
                    return;
                }
            }
        }
        
        applyFallbackBg(activity, window, fallbackColor, isDark);
        
    } catch (Throwable e) {
        applyFallbackBg(activity, window, fallbackColor, isDark);
    }
}

void applyFallbackBg(Activity activity, Window window, String colorStr, boolean isDark) {
    try {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(activity, 16));
        bg.setColor(Color.parseColor(colorStr));
        applyDrawableToWindow(window, bg, activity, isDark);
    } catch(Exception e) { 
        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(activity, 16));
            bg.setColor(isDark ? Color.parseColor("#FF1E1E1E") : Color.WHITE);
            applyDrawableToWindow(window, bg, activity, isDark);
        } catch (Exception ex) {}
    }
}

void applyDrawableToWindow(Window window, Drawable drawable, Activity activity, boolean isDark) {
    try {
        window.setBackgroundDrawable(drawable);
        applyWindowRadius(activity, window);
    } catch(Throwable e) {
        try {
            window.setBackgroundColor(isDark ? Color.parseColor("#FF1E1E1E") : Color.WHITE);
        } catch (Exception ex) {
            window.setBackgroundColor(Color.WHITE);
        }
    }
}

void updateViewStylesRecursively(View view, int textColor, Typeface tf, float fontSizeScale) {
    if (view == null) return;
    
    try {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            
            String className = tv.getClass().getName();
            if (className.contains("Search") || className.contains("EditText") || 
                className.contains("AutoComplete") || className.contains("MultiAutoComplete")) {
                return;
            }
            
            if (tv.getId() == android.R.id.title || tv.getId() == android.R.id.alertTitle ||
                tv.getId() == android.R.id.text1 || tv.getId() == android.R.id.text2) {
                return;
            }
            
            if (tv.getParent() instanceof CheckBox) {
                return;
            }
            
            tv.setTextColor(textColor);
            
            if (fontSizeScale != 1.0f) {
                float originalSize = tv.getTextSize();
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize * fontSizeScale);
            }
            
            if (tf != null) {
                int style = tv.getTypeface() != null ? tv.getTypeface().getStyle() : Typeface.NORMAL;
                tv.setTypeface(tf, style);
            }
            
            if (textColor == Color.parseColor("#FFEFEFEF")) {
                tv.setHintTextColor(Color.argb(100, 239, 239, 239));
            } else if (textColor == Color.parseColor("#FF333333")) {
                tv.setHintTextColor(Color.argb(100, 51, 51, 51));
            }
        }
        
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            String className = vg.getClass().getName();
            if (!className.contains("Search") && !className.contains("AutoComplete")) {
                for (int i = 0; i < vg.getChildCount(); i++) {
                    updateViewStylesRecursively(vg.getChildAt(i), textColor, tf, fontSizeScale);
                }
            }
        }
        
    } catch (Exception e) {}
}

/**
 * 将颜色值转换为十六进制字符串
 * @param color 颜色值
 * @return 十六进制字符串
 */
String colorToHex(int color) {
    try {
        return String.format("#%08X", color);
    } catch (Exception e) {
        return "#FF000000";
    }
}

/**
 * 将文本复制到剪贴板
 * @param activity 当前 Activity
 * @param text 要复制的文本
 */
void copyToClipboard(Activity activity, String text) {
    try {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("color", text);
        clipboard.setPrimaryClip(clip);
    } catch (Exception e) {}
}

/**
 * 创建圆角矩形背景
 * @param color 背景颜色
 * @param radius 圆角半径
 * @return GradientDrawable 对象
 */
GradientDrawable createRoundRectDrawable(int color, float radius) {
    try {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    } catch (Exception e) {
        return new GradientDrawable();
    }
}

/**
 * 创建可点击背景
 * @return StateListDrawable 对象
 */
android.graphics.drawable.StateListDrawable createSelectableBackground() {
    android.graphics.drawable.StateListDrawable drawable = new android.graphics.drawable.StateListDrawable();
    drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.parseColor("#1A000000")));
    drawable.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
    return drawable;
}

void addSectionHeader(Activity activity, LinearLayout parent, String text, int color) {
    TextView tv = new TextView(activity);
    tv.setText(text); tv.setTextSize(13); tv.setTextColor(color);
    tv.setPadding(dp(activity, 4), dp(activity, 10), 0, dp(activity, 6));
    parent.addView(tv);
}

LinearLayout createCardGroup(Activity activity, int color, int radius) {
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    int padding = dp(activity, 6);
    card.setPadding(padding, padding, padding, padding);
    
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(darkenColor(color, 0.02f));
    bg.setCornerRadius(dp(activity, radius));
    card.setBackgroundDrawable(bg);
    card.setClipToOutline(true);
    return card;
}

void addClickableItem(Activity activity, LinearLayout parent, String title, String sub, int titleColor, int cardColor, boolean isLast, View.OnClickListener onClick) {
    LinearLayout item = new LinearLayout(activity);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
    
    int itemColor = lightenColor(cardColor, 0.35f);
    GradientDrawable itemBg = new GradientDrawable();
    itemBg.setColor(itemColor);
    itemBg.setCornerRadius(dp(activity, 8));
    item.setBackgroundDrawable(itemBg);
    item.setOnClickListener(onClick);
    
    LinearLayout textLayout = new LinearLayout(activity);
    textLayout.setOrientation(LinearLayout.VERTICAL);
    TextView t1 = new TextView(activity); t1.setText(title); t1.setTextSize(16); t1.setTextColor(titleColor);
    textLayout.addView(t1);
    TextView t2 = new TextView(activity); t2.setText(sub); t2.setTextSize(12); t2.setTextColor(titleColor); t2.setAlpha(0.6f);
    textLayout.addView(t2);
    
    item.addView(textLayout, new LinearLayout.LayoutParams(0, -2, 1.0f));
    TextView arrow = new TextView(activity); arrow.setText(">"); arrow.setTextSize(20); arrow.setTextColor(titleColor); arrow.setAlpha(0.4f);
    item.addView(arrow);
    
    parent.addView(item);
    
    if (!isLast) {
        View space = new View(activity);
        space.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(-1, dp(activity, 4));
        spaceParams.leftMargin = dp(activity, 6);
        spaceParams.rightMargin = dp(activity, 6);
        parent.addView(space, spaceParams);
    }
}

void addInputItem(Activity activity, LinearLayout parent, String title, String value, String hint, int titleColor, int cardColor, String saveKey, String defaultValue) {
    LinearLayout item = new LinearLayout(activity);
    item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
    
    int itemColor = lightenColor(cardColor, 0.35f);
    GradientDrawable itemBg = new GradientDrawable();
    itemBg.setColor(itemColor);
    itemBg.setCornerRadius(dp(activity, 8));
    item.setBackgroundDrawable(itemBg);
    
    TextView t1 = new TextView(activity); t1.setText(title); t1.setTextSize(14); t1.setTextColor(titleColor);
    item.addView(t1);

    final EditText input = new EditText(activity);
    String displayValue = value;
    if (value == null || value.trim().isEmpty()) {
        if (defaultValue != null) {
            displayValue = defaultValue;
            input.setHint(hint);
            input.setTextColor(Color.GRAY);
        } else {
            input.setHint(hint);
        }
    }
    input.setText(displayValue);
    input.setTextSize(14);
    input.setHintTextColor(Color.GRAY);
    input.setBackgroundColor(Color.TRANSPARENT);
    input.setPadding(0, dp(activity, 8), 0, dp(activity, 8));

    input.addTextChangedListener(new android.text.TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(android.text.Editable s) {
            String val = s.toString().trim();
            pendingSettingsChanges.put(saveKey, val);
        }
    });

    item.addView(input);
    parent.addView(item);
    
    View space = new View(activity);
    space.setBackgroundColor(Color.TRANSPARENT);
    LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(-1, dp(activity, 4));
    spaceParams.leftMargin = dp(activity, 6);
    spaceParams.rightMargin = dp(activity, 6);
    parent.addView(space, spaceParams);
}

int lightenColor(int color, float factor) {
    int r = Color.red(color);
    int g = Color.green(color);
    int b = Color.blue(color);
    int a = Color.alpha(color);
    
    r = (int) (r + (255 - r) * factor);
    g = (int) (g + (255 - g) * factor);
    b = (int) (b + (255 - b) * factor);
    
    return Color.argb(a, Math.min(255, r), Math.min(255, g), Math.min(255, b));
}

int darkenColor(int color, float factor) {
    int r = Color.red(color);
    int g = Color.green(color);
    int b = Color.blue(color);
    int a = Color.alpha(color);
    
    r = (int) (r * (1 - factor));
    g = (int) (g * (1 - factor));
    b = (int) (b * (1 - factor));
    
    return Color.argb(a, Math.max(0, r), Math.max(0, g), Math.max(0, b));
}

void addInputItem(Activity activity, LinearLayout parent, String title, String value, String hint, int titleColor, String saveKey, String defaultValue) {
    int cardColor = Color.parseColor("#FFF5F5F5");
    addInputItem(activity, parent, title, value, hint, titleColor, cardColor, saveKey, defaultValue);
}

void addInputItem(Activity activity, LinearLayout parent, String title, String value, String hint, int titleColor, String saveKey) {
    addInputItem(activity, parent, title, value, hint, titleColor, saveKey, null);
}

static java.util.HashMap pendingSettingsChanges = new java.util.HashMap();

int savePendingSettings() {
    if (pendingSettingsChanges == null || pendingSettingsChanges.isEmpty()) {
        return 0;
    }
    
    int count = 0;
    Object[] keys = pendingSettingsChanges.keySet().toArray();
    for (Object keyObj : keys) {
        String key = (String) keyObj;
        Object value = pendingSettingsChanges.get(key);
        if (value != null) {
            putString("settings", key, value.toString());
            count++;
        }
    }
    
    pendingSettingsChanges.clear();
    return count;
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

public Switch createSwitch(Context activity, String str, boolean state, int size, float weight) {
    Switch switch1 = new Switch(activity);
    switch1.setText(str);
    switch1.setTextColor(Color.parseColor("#4CA1AF"));
    switch1.setChecked(state);
    
    switch1.setScaleX(3.2f);
    switch1.setScaleY(3.2f);
    
    if(size > 0) switch1.setTextSize(size);
    
    if(weight > 0) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        params.leftMargin = dp(activity, 8);
        params.rightMargin = dp(activity, 8);
        switch1.setLayoutParams(params);
    }
    
    return switch1;
}

CheckBox createCheckBox(Activity activity, String text, boolean checked, int textSizeDp, int textColor) {
    CheckBox checkBox = new CheckBox(activity);
    checkBox.setText(text);
    checkBox.setChecked(checked);
    if (textSizeDp > 0) checkBox.setTextSize(textSizeDp);
    
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    params.topMargin = dp(activity, 4);
    params.bottomMargin = dp(activity, 4);
    checkBox.setLayoutParams(params);
    
    int padding = dp(activity, 16);
    checkBox.setPadding(padding, padding, padding, padding);
    
    checkBox.setTextColor(textColor);
    
    checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(textColor));
    
    return checkBox;
}

void addDivider(Activity activity, LinearLayout parent, int color) {
    View v = new View(activity); v.setBackgroundColor(color);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1); lp.leftMargin = dp(activity, 16);
    parent.addView(v, lp);
}

android.graphics.drawable.StateListDrawable getSelectableBg(Activity activity) {
    android.graphics.drawable.StateListDrawable res = new android.graphics.drawable.StateListDrawable();
    res.setExitFadeDuration(300);
    res.addState(new int[]{android.R.attr.state_pressed}, new android.graphics.drawable.ColorDrawable(Color.parseColor("#1A000000")));
    res.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    return res;
}

private float getMaxRefreshRate(Context context) {
    try {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60f;
        Display display = wm.getDefaultDisplay();
        if (display == null) return 60f;

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            Display.Mode[] modes = display.getSupportedModes();
            float max = 0f;
            for (int i = 0; i < modes.length; i++) {
                float r = modes[i].getRefreshRate();
                if (r > max) {
                    max = r;
                }
            }
            return max > 0f ? max : display.getRefreshRate();
        } else {
            return display.getRefreshRate();
        }
    } catch (Throwable t) {
        return 60f;
    }
}
