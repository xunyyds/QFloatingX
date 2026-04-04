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
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;
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
 * View 标签系统常量 - 用于标记 View 的主题行为
 */
final String TAG_KEEP_ORIGINAL_COLOR = "keep_original_color";
final String TAG_KEEP_ORIGINAL_BG = "keep_original_bg";
final String TAG_SKIP_THEME = "skip_theme";
final String TAG_CUSTOM_TEXT_COLOR = "custom_text_color";
final String TAG_ANIMATION_DURATION = "animation_duration";
final String TAG_SKIP_ANIMATION = "skip_animation";

/**
 * 动画过渡配置常量
 */
static final long DEFAULT_ANIMATION_DURATION = 300L;
static final long COLOR_ANIMATION_DURATION = 250L;
static final long BACKGROUND_ANIMATION_DURATION = 300L;

/**
 * 主题应用动画缓存
 */
static WeakHashMap<View, Long> viewAnimationDurations = new WeakHashMap<>();

import android.content.res.Configuration;
boolean isThemeDark(Activity activity) {
    try {
        // 获取当前主题模式配置，默认 default
        String themeMode = getString("settings", "ui_theme_mode", "default");
        
        // 强制深色
        if ("dark".equals(themeMode)) {
            return true;
        }
        
        // 强制浅色
        if ("light".equals(themeMode)) {
            return false;
        }
        
        // 跟随系统 或 默认（推荐），都走系统暗黑判断
        boolean systemDarkMode = false;
        int uiMode = activity.getResources().getConfiguration().uiMode;
        int nightMode = uiMode & Configuration.UI_MODE_NIGHT_MASK;
        systemDarkMode = (nightMode == Configuration.UI_MODE_NIGHT_YES);
        
        return systemDarkMode;
    } catch (Throwable e) {
        // 异常兜底默认浅色
        return false;
    }
}

private Drawable createBg(Context ctx, int color, int radius) {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(color);
    gd.setCornerRadius(dp(ctx, radius));
    return gd;
}

private Drawable createRippleBg(Context ctx, int bgColor, int radius) {
    GradientDrawable content = new GradientDrawable();
    content.setColor(bgColor);
    content.setCornerRadius(dp(ctx, radius));
    return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#1A000000")), content, content);
}

private Drawable createButtonBg(Context ctx, int bgColor, int radius) {
    GradientDrawable content = new GradientDrawable();
    content.setColor(bgColor);
    content.setCornerRadius(dp(ctx, radius));
    return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")), content, content);
}

private StateListDrawable createInputBg(Context ctx, int surfaceVariant, int outline, int primary) {
    int r = dp(ctx, 12);
    GradientDrawable normal = new GradientDrawable();
    normal.setColor(surfaceVariant);
    normal.setCornerRadius(r);
    normal.setStroke(dp(ctx, 1), outline);
    GradientDrawable focused = new GradientDrawable();
    focused.setColor(surfaceVariant);
    focused.setCornerRadius(r);
    focused.setStroke(dp(ctx, 2), primary);
    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[]{android.R.attr.state_focused}, focused);
    sld.addState(new int[]{}, normal);
    return sld;
}

private Button makeSmallBtn(Context ctx, String text, int color) {
    Button b = new Button(ctx);
    b.setText(text);
    b.setTextColor(color);
    b.setBackground(createRippleBg(ctx, Color.TRANSPARENT, 20));
    b.setMinHeight(dp(ctx, 40));
    b.setPadding(dp(ctx, 16), dp(ctx, 8), dp(ctx, 16), dp(ctx, 8));
    
    ObjectAnimator scaleX = ObjectAnimator.ofFloat(b, "scaleX", 1f, 0.95f, 1f);
    ObjectAnimator scaleY = ObjectAnimator.ofFloat(b, "scaleY", 1f, 0.95f, 1f);
    scaleX.setDuration(150);
    scaleY.setDuration(150);
    AnimatorSet scaleDown = new AnimatorSet();
    scaleDown.playTogether(scaleX, scaleY);
    
    b.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                scaleDown.start();
            }
            return false;
        }
    });
    
    return b;
}

/**
 * 创建带按压反馈的圆角背景 Drawable
 * @param normalColor 常态颜色值
 * @param pressedColor 按压状态颜色值
 * @param r 圆角半径（像素）
 * @return StateListDrawable 状态列表Drawable对象
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
 * @param color 原始颜色值
 * @param factor 调整系数，0-1变暗，大于1变亮
 * @return 调整后的颜色值
 */
int adjustColor(int color, float factor) {
    int a = Color.alpha(color);
    int r = Math.round(Color.red(color) * factor);
    int g = Math.round(Color.green(color) * factor);
    int b = Math.round(Color.blue(color) * factor);
    return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
}

/**
 * 调整颜色透明度
 * @param color 原始颜色值
 * @param factor 透明度因子，范围0-1
 * @return 调整后的颜色值
 */
int adjustAlpha(int color, float factor) {
    return Color.argb(Math.round(Color.alpha(color) * factor), Color.red(color), Color.green(color), Color.blue(color));
}

/**
 * 创建简单圆角背景
 * @param c 背景颜色值
 * @param r 圆角半径（像素）
 * @return GradientDrawable 圆角Drawable对象
 */
GradientDrawable roundRect(int c, int r) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(c);
    g.setCornerRadius(r);
    return g;
}

/**
 * 创建通用输入框
 * @param a Activity上下文
 * @param h 提示文本
 * @param bg 背景颜色值
 * @return EditText 输入框实例
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
 * 创建小型数字输入框
 * @param a Activity上下文
 * @param h 提示文本
 * @param bg 背景颜色值
 * @return EditText 输入框实例
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
 * 创建超小输入框用于紧凑布局
 * @param a Activity上下文
 * @param h 提示文本
 * @param bg 背景颜色值
 * @return EditText 输入框实例
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
 * 创建圆角矩形背景Drawable
 */
GradientDrawable makeRoundRect(int color, int radiusPx) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(radiusPx);
    return drawable;
}

/**
 * 创建紧凑型输入框
 */
EditText makeInputCompact(Activity ctx, String val, String hint, int colorBg) {
    EditText et = new EditText(ctx);
    et.setText(val);
    et.setHint(hint);
    et.setTextSize(13);
    et.setTextColor(Color.parseColor("#222222"));
    et.setHintTextColor(Color.parseColor("#BBBBBB"));
    et.setBackground(makeRoundRect(colorBg, dp(ctx, 6)));
    et.setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8));
    et.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return et;
}

/**
 * 创建小标题TextView
 */
TextView makeSubTitleCompact(Activity ctx, String text, int color) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(12);
    tv.setTextColor(color);
    tv.setPadding(dp(ctx, 4), dp(ctx, 16), 0, dp(ctx, 6));
    return tv;
}

/**
 * 创建操作按钮
 */
TextView makeActionBtn(Activity ctx, String text, int textColor, int bgColor) {
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextSize(14);
    tv.setTextColor(textColor);
    tv.setGravity(Gravity.CENTER);
    tv.setBackground(makeRoundRect(bgColor, dp(ctx, 8)));
    return tv;
}


/**
 * 创建标签块视图
 * @param a Activity上下文
 * @param t 标签文本
 * @param s 是否选中状态
 * @param type 颜色样式类型，1为蓝色系，2为橙色系，其他为默认蓝色系
 * @return TextView 标签视图实例
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
 * @param v 标签视图
 * @param s 是否选中状态
 */
void setChip(TextView v, boolean s) {
    int normalColor = s ? Color.parseColor("#3B71FE") : Color.parseColor("#F0F0F0");
    int pressedColor = s ? Color.parseColor("#2E5BC7") : Color.parseColor("#D6D6D6");
    v.setTextColor(s ? Color.WHITE : Color.parseColor("#666666"));
    v.setBackground(makeFeedbackBg(normalColor, pressedColor, dp(v.getContext(), 50)));
}

/**
 * 更新标签视觉状态带类型参数
 * @param v 标签视图
 * @param s 是否选中状态
 * @param type 颜色样式类型
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
 * 创建代码预设按钮
 * @param a Activity上下文
 * @param t 按钮文本
 * @param textColor 文字颜色值
 * @return TextView 按钮视图实例
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
 * 创建开关按钮
 * @param a Activity上下文
 * @param o 是否开启状态
 * @param c 开启时的颜色值
 * @return TextView 按钮视图实例
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
 * @param v 按钮视图
 * @param o 是否开启状态
 * @param c 开启时的颜色值
 */
void setSwitch(TextView v, boolean o, int c) {
    v.setText(o ? "开" : "关");
    v.setTextColor(o ? Color.WHITE : Color.parseColor("#666666"));
    int bg = o ? c : Color.parseColor("#E0E0E0");
    v.setBackground(makeFeedbackBg(bg, adjustColor(bg, 0.9f), dp(v.getContext(), 20)));
}

/**
 * 创建大按钮
 * @param a Activity上下文
 * @param t 按钮文本
 * @param tc 文字颜色值
 * @param bg 背景颜色值
 * @return TextView 按钮视图实例
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
 * 标记View保留原始颜色，主题系统不会修改其文字颜色
 * @param view 目标View
 */
void setKeepOriginalColor(View view) {
    if (view != null) {
        view.setTag(TAG_KEEP_ORIGINAL_COLOR, true);
    }
}

/**
 * 检查View是否标记为保留原始颜色
 * @param view 目标View
 * @return true表示应保留原始颜色
 */
boolean shouldKeepOriginalColor(View view) {
    if (view == null) return false;
    Object tag = view.getTag(TAG_KEEP_ORIGINAL_COLOR);
    return tag != null && Boolean.TRUE.equals(tag);
}

/**
 * 标记View保留原始背景，主题系统不会修改其背景
 * @param view 目标View
 */
void setKeepOriginalBackground(View view) {
    if (view != null) {
        view.setTag(TAG_KEEP_ORIGINAL_BG, true);
    }
}

/**
 * 检查View是否标记为保留原始背景
 * @param view 目标View
 * @return true表示应保留原始背景
 */
boolean shouldKeepOriginalBackground(View view) {
    if (view == null) return false;
    Object tag = view.getTag(TAG_KEEP_ORIGINAL_BG);
    return tag != null && Boolean.TRUE.equals(tag);
}

/**
 * 标记View完全跳过主题处理
 * @param view 目标View
 */
void setSkipTheme(View view) {
    if (view != null) {
        view.setTag(TAG_SKIP_THEME, true);
    }
}

/**
 * 检查View是否标记为跳过主题处理
 * @param view 目标View
 * @return true表示应跳过主题处理
 */
boolean shouldSkipTheme(View view) {
    if (view == null) return false;
    Object tag = view.getTag(TAG_SKIP_THEME);
    return tag != null && Boolean.TRUE.equals(tag);
}

/**
 * 为View设置自定义文本颜色，主题系统会使用此颜色
 * @param view 目标View
 * @param color 颜色值
 */
void setCustomTextColor(View view, int color) {
    if (view != null) {
        view.setTag(TAG_CUSTOM_TEXT_COLOR, color);
    }
}

/**
 * 获取View的自定义文本颜色
 * @param view 目标View
 * @param defaultColor 默认颜色值
 * @return 自定义颜色或默认颜色
 */
int getCustomTextColor(View view, int defaultColor) {
    if (view == null) return defaultColor;
    Object tag = view.getTag(TAG_CUSTOM_TEXT_COLOR);
    if (tag instanceof Integer) {
        return (Integer) tag;
    }
    return defaultColor;
}

/**
 * 标记View跳过动画过渡
 * @param view 目标View
 */
void setSkipAnimation(View view) {
    if (view != null) {
        view.setTag(TAG_SKIP_ANIMATION, true);
    }
}

/**
 * 检查View是否标记为跳过动画
 * @param view 目标View
 * @return true表示应跳过动画
 */
boolean shouldSkipAnimation(View view) {
    if (view == null) return false;
    Object tag = view.getTag(TAG_SKIP_ANIMATION);
    return tag != null && Boolean.TRUE.equals(tag);
}

/**
 * 为View设置自定义动画时长
 * @param view 目标View
 * @param durationMs 动画时长（毫秒）
 */
void setAnimationDuration(View view, long durationMs) {
    if (view != null) {
        view.setTag(TAG_ANIMATION_DURATION, durationMs);
        viewAnimationDurations.put(view, durationMs);
    }
}

/**
 * 获取View的动画时长
 * @param view 目标View
 * @return 动画时长（毫秒），默认为DEFAULT_ANIMATION_DURATION
 */
long getAnimationDuration(View view) {
    if (view == null) return DEFAULT_ANIMATION_DURATION;
    Object tag = view.getTag(TAG_ANIMATION_DURATION);
    if (tag instanceof Long) {
        return (Long) tag;
    }
    Long cached = viewAnimationDurations.get(view);
    return cached != null ? cached : DEFAULT_ANIMATION_DURATION;
}

/**
 * 文字颜色渐变动画
 * @param textView 目标TextView
 * @param fromColor 起始颜色值
 * @param toColor 目标颜色值
 * @param duration 动画时长（毫秒）
 */
void animateTextColor(final TextView textView, int fromColor, int toColor, long duration) {
    if (textView == null || shouldSkipAnimation(textView)) {
        if (textView != null) {
            textView.setTextColor(toColor);
        }
        return;
    }
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final int finalToColor = toColor;
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateTextColor(textView, fromColor, finalToColor, finalDuration);
            }
        });
        return;
    }
    
    ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
    colorAnim.setDuration(duration);
    colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animator) {
            try {
                textView.setTextColor((Integer) animator.getAnimatedValue());
            } catch (Exception e) {}
        }
    });
    colorAnim.start();
}

/**
 * 文字颜色渐变动画使用默认时长
 * @param textView 目标TextView
 * @param fromColor 起始颜色值
 * @param toColor 目标颜色值
 */
void animateTextColor(TextView textView, int fromColor, int toColor) {
    animateTextColor(textView, fromColor, toColor, COLOR_ANIMATION_DURATION);
}

/**
 * 背景颜色渐变动画
 * @param view 目标View
 * @param fromColor 起始颜色值
 * @param toColor 目标颜色值
 * @param duration 动画时长（毫秒）
 */
void animateBackgroundColor(final View view, int fromColor, int toColor, long duration) {
    if (view == null || shouldSkipAnimation(view)) {
        if (view != null) {
            view.setBackgroundColor(toColor);
        }
        return;
    }
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final int finalToColor = toColor;
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateBackgroundColor(view, fromColor, finalToColor, finalDuration);
            }
        });
        return;
    }
    
    ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
    colorAnim.setDuration(duration);
    colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animator) {
            try {
                view.setBackgroundColor((Integer) animator.getAnimatedValue());
            } catch (Exception e) {}
        }
    });
    colorAnim.start();
}

/**
 * 背景颜色渐变动画使用默认时长
 * @param view 目标View
 * @param fromColor 起始颜色值
 * @param toColor 目标颜色值
 */
void animateBackgroundColor(View view, int fromColor, int toColor) {
    animateBackgroundColor(view, fromColor, toColor, BACKGROUND_ANIMATION_DURATION);
}

/**
 * 背景Drawable过渡动画
 * @param view 目标View
 * @param newDrawable 新背景Drawable
 * @param duration 过渡时长（毫秒）
 */
void animateBackgroundDrawable(final View view, final Drawable newDrawable, long duration) {
    if (view == null) return;
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateBackgroundDrawable(view, newDrawable, finalDuration);
            }
        });
        return;
    }
    
    if (shouldSkipAnimation(view)) {
        view.setBackground(newDrawable);
        return;
    }
    
    Drawable currentBg = view.getBackground();
    
    if (currentBg != null && newDrawable != null) {
        TransitionDrawable transition = new TransitionDrawable(new Drawable[]{
            currentBg,
            newDrawable
        });
        view.setBackground(transition);
        transition.startTransition((int) duration);
    } else {
        view.setBackground(newDrawable);
    }
}

/**
 * 背景Drawable过渡动画使用默认时长
 * @param view 目标View
 * @param newDrawable 新背景Drawable
 */
void animateBackgroundDrawable(View view, Drawable newDrawable) {
    animateBackgroundDrawable(view, newDrawable, BACKGROUND_ANIMATION_DURATION);
}

/**
 * View淡入动画
 * @param view 目标View
 * @param duration 动画时长（毫秒）
 */
void animateFadeIn(final View view, long duration) {
    if (view == null) return;
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateFadeIn(view, finalDuration);
            }
        });
        return;
    }
    
    if (shouldSkipAnimation(view)) {
        view.setAlpha(1f);
        return;
    }
    
    view.setAlpha(0f);
    view.animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
        .start();
}

/**
 * View淡出动画
 * @param view 目标View
 * @param duration 动画时长（毫秒）
 */
void animateFadeOut(final View view, long duration) {
    if (view == null) return;
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateFadeOut(view, finalDuration);
            }
        });
        return;
    }
    
    if (shouldSkipAnimation(view)) {
        view.setAlpha(0f);
        return;
    }
    
    view.animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(null)
        .start();
}

/**
 * View缩放弹入动画
 * @param view 目标View
 * @param duration 动画时长（毫秒）
 */
void animateScaleIn(final View view, long duration) {
    if (view == null) return;
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateScaleIn(view, finalDuration);
            }
        });
        return;
    }
    
    if (shouldSkipAnimation(view)) {
        view.setScaleX(1f);
        view.setScaleY(1f);
        return;
    }
    
    view.setScaleX(0.8f);
    view.setScaleY(0.8f);
    view.animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(duration)
        .setListener(null)
        .start();
}

/**
 * 组合动画淡入加缩放
 * @param view 目标View
 * @param duration 动画时长（毫秒）
 */
void animateFadeScaleIn(final View view, long duration) {
    if (view == null) return;
    
    if (Looper.myLooper() != Looper.getMainLooper()) {
        final long finalDuration = duration;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                animateFadeScaleIn(view, finalDuration);
            }
        });
        return;
    }
    
    if (shouldSkipAnimation(view)) {
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        return;
    }
    
    view.setAlpha(0f);
    view.setScaleX(0.9f);
    view.setScaleY(0.9f);
    view.animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(duration)
        .setListener(null)
        .start();
}

/**
 * 批量动画对多个View执行淡入动画带延迟
 * @param views View列表
 * @param duration 单个动画时长（毫秒）
 * @param delayBetween 每个动画之间的延迟（毫秒）
 */
void animateFadeInSequence(final List views, long duration, long delayBetween) {
    if (views == null || views.isEmpty()) return;
    
    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            for (int i = 0; i < views.size(); i++) {
                final View view = (View) views.get(i);
                final int index = i;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    public void run() {
                        animateFadeIn(view, duration);
                    }
                }, index * delayBetween);
            }
        }
    });
}

/**
 * 根据MIME类型获取文件扩展名
 * @param mimeType MIME类型字符串
 * @return 文件扩展名，默认返回.png
 */
String getExtensionFromMimeType(String mimeType) {
    if (mimeType == null) return ".png";
    if (mimeType.equals("image/gif") || mimeType.equals("webp/gif")) return ".gif";
    return ".png";
}

/**
 * 从Uri加载Bitmap
 * @param activity 当前Activity
 * @param uri 图片URI
 * @return 解码后的Bitmap，失败返回null
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
 * @param quality 压缩质量0-100
 * @return 压缩后的Bitmap
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
 * 创建棋盘格背景Drawable用于透明图片展示
 * @param context 上下文
 * @return 棋盘格BitmapDrawable
 */
BitmapDrawable getCheckerboardDrawable(Context context) {
    int size = dp(context, 20);
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
 * 支持手势缩放和点击切换背景的ImageView
 */
class ZoomImageView extends ImageView {
    public Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private float startDistance = 0f;
    private float midX = 0f, midY = 0f;
    private float lastX = 0f, lastY = 0f;
    private int mode = 0;
    private View rootLayout;
    private int bgIndex = 0;

    /**
     * 构造函数
     * @param context 上下文
     * @param root 根布局View
     */
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

    /**
     * 计算两点距离
     * @param event 触摸事件
     * @return 两点间距离
     */
    private float calculateDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 判断点击点是否在图片内部
     * @param x 点击x坐标
     * @param y 点击y坐标
     * @return true表示在图片内部
     */
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
    private int tMode = 0;
    private float lastX, lastY;

    /**
     * 构造函数
     * @param context 上下文
     */
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

    /**
     * 获取实际裁剪矩形
     * @param b 源Bitmap
     * @param m 变换矩阵
     * @return 实际裁剪区域Rect
     */
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
 * @param activity 当前Activity
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

/**
 * 执行保存操作
 * @param activity 当前Activity
 * @param b 要保存的Bitmap
 * @param p 保存路径
 * @param q 压缩质量
 */
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

/**
 * 编辑并保存图片入口
 * @param activity 当前Activity
 * @param uri 图片URI
 * @param p 保存路径
 * @param c 请求码
 */
void editAndSaveImage(final Activity activity, final Uri uri, final String p, final int c) {
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

/**
 * Hook文件选择器结果
 * @param activity 当前Activity
 * @param requestCode 请求码
 * @param savePath 保存路径
 */
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

/**
 * 判断颜色是否为深色
 * @param color 颜色值
 * @return true表示是深色
 */
public boolean isColorDark(int color) {
    double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
    return darkness >= 0.5; 
}

/**
 * RenderScript模糊支持原图修改和输出Bitmap两种模式
 * @param activity 上下文
 * @param bitmap 源Bitmap
 * @param outBitmap 输出Bitmap可为空则修改原图
 * @param radius 模糊半径
 * @return 模糊后的Bitmap
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
 * 快速模糊StackBlur算法的简化版
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
 * 调整Bitmap大小最大边不超过maxSize保持比例
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
 * 居中裁剪Bitmap到目标尺寸CenterCrop
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
 * 优化智能缩放算法CenterCrop确保填满目标区域不拉伸
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

/**
 * 缩放Bitmap以适应目标尺寸
 * @param source 源图
 * @param targetW 目标宽
 * @param targetH 目标高
 * @return 缩放后的图
 */
Bitmap scaleToFitBitmap(Bitmap source, int targetW, int targetH) {
    if (source == null || source.isRecycled()) return null;
    
    try {
        int sourceW = source.getWidth();
        int sourceH = source.getHeight();
        
        float scale = Math.min((float) targetW / sourceW, (float) targetH / sourceH);
        int scaledW = Math.round(sourceW * scale);
        int scaledH = Math.round(sourceH * scale);
        
        return Bitmap.createScaledBitmap(source, scaledW, scaledH, true);
        
    } catch (Exception e) {
        return source;
    }
}

/**
 * 智能居中裁剪Bitmap
 * @param source 源图
 * @param targetW 目标宽
 * @param targetH 目标高
 * @return 裁剪后的图
 */
Bitmap smartCenterCropBitmap(Bitmap source, int targetW, int targetH) {
    return smartScaleBitmap(source, targetW, targetH);
}

/**
 * 生成缓存键
 * @param imgPath 图片路径
 * @param blurRadius 模糊半径
 * @param overlayAlpha 遮罩透明度
 * @param isDark 是否深色模式
 * @param targetW 目标宽度
 * @param targetH 目标高度
 * @return 缓存键字符串
 */
String generateCacheKey(String imgPath, int blurRadius, int overlayAlpha, boolean isDark, int targetW, int targetH) {
    File f = new File(imgPath);
    long lastMod = f.exists() ? f.lastModified() : 0;
    long size = f.exists() ? f.length() : 0;
    int widthGroup = (targetW / 20) * 20;
    int heightGroup = (targetH / 20) * 20;
    return imgPath.hashCode() + "_" + lastMod + "_" + size + "_" + blurRadius + "_" + 
           overlayAlpha + "_" + (isDark ? "1" : "0") + "_" + widthGroup + "_" + heightGroup;
}

/**
 * 生成基础缓存键
 * @param imgPath 图片路径
 * @param blurRadius 模糊半径
 * @param overlayAlpha 遮罩透明度
 * @param isDark 是否深色模式
 * @return 基础缓存键字符串
 */
String generateBaseCacheKey(String imgPath, int blurRadius, int overlayAlpha, boolean isDark) {
    File f = new File(imgPath);
    long lastMod = f.exists() ? f.lastModified() : 0;
    return imgPath.hashCode() + "_" + lastMod + "_" + blurRadius + "_" + overlayAlpha + "_" + (isDark ? "1" : "0");
}

/**
 * 查找相似缓存
 * @param baseKey 基础缓存键
 * @param targetW 目标宽度
 * @param targetH 目标高度
 * @return 缓存的Drawable或null
 */
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

/**
 * 检查并在需要时清理内存
 * @return true表示执行了清理
 */
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

/**
 * 缓存条目类
 */
static class CacheEntry {
    Drawable drawable;
    long createdTime;
    long lastAccessTime;
    int width;
    int height;
    String cacheKey;
    int accessCount;
    
    /**
     * 构造函数
     * @param d Drawable对象
     * @param key 缓存键
     * @param w 宽度
     * @param h 高度
     */
    CacheEntry(Drawable d, String key, int w, int h) {
        this.drawable = d;
        this.cacheKey = key;
        this.width = w;
        this.height = h;
        this.createdTime = System.currentTimeMillis();
        this.lastAccessTime = this.createdTime;
        this.accessCount = 0;
    }
    
    /**
     * 更新访问信息
     */
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

/**
 * 计算MD5哈希值
 * @param input 输入字符串
 * @return MD5哈希字符串
 */
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

/**
 * 获取缓存的Drawable
 * @param cacheKey 缓存键
 * @return Drawable对象或null
 */
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

/**
 * 将Drawable添加到缓存
 * @param drawable Drawable对象
 * @param cacheKey 缓存键
 * @param width 宽度
 * @param height 高度
 */
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

/**
 * 从缓存中移除指定条目
 * @param cacheKey 缓存键
 */
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

/**
 * 卸载背景缓存
 */
void unloadBackgroundCache() {
    imageCache.clear();
    cacheOrder.clear();
    scaledViews.clear();
    globalCachedDrawable = null;
    globalCachedParams = "";
}

/**
 * 强制卸载所有缓存
 */
void forceUnloadAllCache() {
    unloadBackgroundCache();
}

/**
 * 创建手绘风格开关控件
 * @param ctx Context上下文
 * @param initVal 初始状态
 * @return Object数组包含View和状态数组
 */
public Object[] createSwitchViewWithState(Context ctx, boolean initVal) {
    FrameLayout swContainer = new FrameLayout(ctx);
    int swW = dp(ctx, 48);
    int swH = dp(ctx, 28);
    FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(swW, swH);
    swContainer.setLayoutParams(containerLp);

    ColorStateList rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000"));
    RippleDrawable ripple = new RippleDrawable(rippleColor, null, null);
    swContainer.setBackground(ripple);

    swContainer.setClickable(true);
    swContainer.setFocusable(true);

    View track = new View(ctx);
    FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(-1, -1);
    track.setLayoutParams(trackLp);
    GradientDrawable trackBg = new GradientDrawable();
    trackBg.setCornerRadius(dp(ctx, 14));
    track.setBackground(trackBg);
    swContainer.addView(track);

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

/**
 * 验证是否为有效的十六进制颜色
 * @param colorCode 颜色代码字符串
 * @return true表示有效
 */
boolean isValidHexColor(String colorCode) {
    if (colorCode == null || colorCode.trim().isEmpty()) return false;
    return Pattern.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$", colorCode.trim());
}

/**
 * 验证是否为有效的渐变字符串
 * @param gradientStr 渐变字符串
 * @return true表示有效
 */
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

/**
 * 获取自定义字体
 * @param typeName 字体类型名称
 * @return Typeface对象
 */
Typeface getCustomTypeface(String typeName) {
    if ("serif".equals(typeName)) return Typeface.SERIF;
    if ("sans".equals(typeName)) return Typeface.SANS_SERIF;
    if ("monospace".equals(typeName)) return Typeface.MONOSPACE;
    if ("bold".equals(typeName)) return Typeface.DEFAULT_BOLD;
    return Typeface.DEFAULT;
}

/**
 * 应用UI主题到对话框
 * @param activity Activity上下文
 * @param dialog AlertDialog对话框
 */
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

/**
 * 执行应用主题
 * @param activity Activity上下文
 * @param dialog AlertDialog对话框
 * @param dialogW 对话框宽度
 * @param dialogH 对话框高度
 */
void executeApplyTheme(final Activity activity, final AlertDialog dialog, final int dialogW, final int dialogH) {
    if (dialog == null || dialog.getWindow() == null) {
        return;
    }
    
    final Window window = dialog.getWindow();
    final boolean isDark = isThemeDark(activity);
    currentIsDark = isDark;
    
    try {
        final String bgType = getString("settings", "ui_bg_type", "color");
        final String bgColor = getString("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
            isDark ? "#FF1E1E1E" : "#FFFFFFFF");
        final String bgGradient = getString("settings", isDark ? "ui_bg_gradient_dark" : "ui_bg_gradient_light", 
            isDark ? "#FF2C2C2C,#FF121212,#FF2C2C2C" : "#FFFFFFFF,#FFF5F5F5,#FFFFFFFF");
        final String textColorUser = getString("settings", isDark ? "ui_text_color_dark" : "ui_text_color_light", "");
        final String fontType = getString("settings", "ui_font_type", "default");
        final String imgPath = pluginPath + "/API/background.png";
        
        float fSize = 1.0f;
        try { fSize = Float.parseFloat(getString("settings", "ui_font_size", "1.0")); } catch(Exception e){}
        final float fontSizeScale = fSize;
        final Typeface tf = getCustomTypeface(fontType);
        
        int blurR = 0, alpha = 100;
        try { blurR = Integer.parseInt(getString("settings", "ui_img_blur", "0")); } catch(Exception e){}
        try { alpha = Integer.parseInt(getString("settings", "ui_img_alpha", isDark ? "180" : "100")); } catch(Exception e){}
        final int blurRadius = Math.max(0, Math.min(25, blurR));
        final int overlayAlpha = Math.max(0, Math.min(255, alpha));
        
        String rawBgColor = getString("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
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
        
        String rawBgColor2 = getString("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
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

/**
 * 应用Drawable带过渡动画
 * @param activity Activity上下文
 * @param window Window对象
 * @param newDrawable 新Drawable
 */
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

/**
 * 快速应用图片背景
 * @param activity Activity上下文
 * @param window Window对象
 * @param imgPath 图片路径
 * @param blurRadius 模糊半径
 * @param overlayAlpha 遮罩透明度
 * @param isDark 是否深色模式
 * @param dialogW 对话框宽度
 * @param dialogH 对话框高度
 */
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

/**
 * 优化加载图片
 * @param activity Activity上下文
 * @param window Window对象
 * @param imgPath 图片路径
 * @param blurRadius 模糊半径
 * @param overlayAlpha 遮罩透明度
 * @param isDark 是否深色模式
 * @param preWidth 预设宽度
 * @param preHeight 预设高度
 */
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

/**
 * 应用Drawable不重新计算文本
 * @param activity Activity上下文
 * @param window Window对象
 * @param drawable Drawable对象
 */
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

/**
 * 完成文本样式设置
 * @param activity Activity上下文
 * @param decorView DecorView
 * @param isDark 是否深色模式
 * @param forceDark 是否强制深色
 */
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

/**
 * 清除内部背景
 * @param view View对象
 */
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

/**
 * 应用对话框尺寸
 * @param activity Activity上下文
 * @param window Window对象
 */
void applyDialogSize(final Activity activity, final Window window) {
    try {
        float scale = 1.0f;
        try {
            String scaleStr = getString("settings", "ui_dialog_scale", "1.0");
            scale = Float.parseFloat(scaleStr);
        } catch (Exception e) {}
        
        float density = activity.getResources().getDisplayMetrics().density;
        
        int customWidth = -1;
        try {
            String widthStr = getString("settings", "ui_dialog_width", "");
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

/**
 * 判断Drawable是否透明
 * @param drawable Drawable对象
 * @return true表示透明
 */
boolean isDrawableTransparent(Drawable drawable) {
    if (drawable == null) return true;
    if (drawable instanceof ColorDrawable) {
        return ((ColorDrawable) drawable).getAlpha() < 10;
    }
    return false;
}

/**
 * 应用窗口圆角
 * @param activity Activity上下文
 * @param window Window对象
 */
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

/**
 * 应用渐变背景
 * @param activity Activity上下文
 * @param window Window对象
 * @param gradientStr 渐变字符串
 * @param fallbackColor 回退颜色
 * @param isDark 是否深色模式
 */
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

/**
 * 应用回退背景
 * @param activity Activity上下文
 * @param window Window对象
 * @param colorStr 颜色字符串
 * @param isDark 是否深色模式
 */
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

/**
 * 应用Drawable到Window
 * @param window Window对象
 * @param drawable Drawable对象
 * @param activity Activity上下文
 * @param isDark 是否深色模式
 */
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

/**
 * 判断颜色是否为默认文本颜色
 * @param color 颜色值
 * @return true表示是默认颜色
 */
boolean isDefaultTextColor(int color) {
    int r = Color.red(color);
    int g = Color.green(color);
    int b = Color.blue(color);
    
    int[][] darkDefaultColors = {
        {0, 0, 0},
        {34, 34, 34},
        {51, 51, 51},
        {66, 66, 66},
        {102, 102, 102},
        {128, 128, 128}
    };
    
    int[][] lightDefaultColors = {
        {255, 255, 255},
        {239, 239, 239},
        {245, 245, 245},
        {238, 238, 238},
        {221, 221, 221},
        {204, 204, 204}
    };
    
    for (int[] defColor : darkDefaultColors) {
        if (Math.abs(r - defColor[0]) <= 15 && 
            Math.abs(g - defColor[1]) <= 15 && 
            Math.abs(b - defColor[2]) <= 15) {
            return true;
        }
    }
    
    for (int[] defColor : lightDefaultColors) {
        if (Math.abs(r - defColor[0]) <= 15 && 
            Math.abs(g - defColor[1]) <= 15 && 
            Math.abs(b - defColor[2]) <= 15) {
            return true;
        }
    }
    
    if (r == g && g == b) {
        if (r <= 140 || r >= 200) {
            return true;
        }
    }
    
    return false;
}

/**
 * 判断颜色是否为用户自定义颜色
 * @param color 颜色值
 * @return true表示是用户自定义颜色
 */
boolean isUserCustomColor(int color) {
    return !isDefaultTextColor(color);
}

/**
 * 判断两个颜色是否相近
 * @param color1 颜色1
 * @param color2 颜色2
 * @param tolerance 容差值0-255
 * @return true表示颜色相近
 */
boolean isColorSimilar(int color1, int color2, int tolerance) {
    return Math.abs(Color.red(color1) - Color.red(color2)) <= tolerance &&
           Math.abs(Color.green(color1) - Color.green(color2)) <= tolerance &&
           Math.abs(Color.blue(color1) - Color.blue(color2)) <= tolerance;
}

/**
 * 递归更新视图样式支持标签系统和动画过渡
 * @param view 待处理的视图
 * @param textColor 主题文本颜色
 * @param tf 字体
 * @param fontSizeScale 字体大小缩放比例
 */
void updateViewStylesRecursively(View view, int textColor, Typeface tf, float fontSizeScale) {
    if (view == null) return;
    
    try {
        if (shouldSkipTheme(view)) {
            return;
        }
        
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
            
            int customColor = getCustomTextColor(tv, -1);
            if (customColor != -1) {
                int currentColor = tv.getCurrentTextColor();
                if (currentColor != customColor) {
                    animateTextColor(tv, currentColor, customColor, getAnimationDuration(tv));
                }
            } else if (!shouldKeepOriginalColor(tv)) {
                int currentColor = tv.getCurrentTextColor();
                if (isDefaultTextColor(currentColor)) {
                    animateTextColor(tv, currentColor, textColor, getAnimationDuration(tv));
                }
            }
            
            if (fontSizeScale != 1.0f) {
                float originalSize = tv.getTextSize();
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize * fontSizeScale);
            }
            
            if (tf != null) {
                int style = tv.getTypeface() != null ? tv.getTypeface().getStyle() : Typeface.NORMAL;
                tv.setTypeface(tf, style);
            }
            
            if (!shouldKeepOriginalColor(tv)) {
                int currentColor = tv.getCurrentTextColor();
                if (isDefaultTextColor(currentColor)) {
                    if (textColor == Color.parseColor("#FFEFEFEF")) {
                        tv.setHintTextColor(Color.argb(100, 239, 239, 239));
                    } else if (textColor == Color.parseColor("#FF333333")) {
                        tv.setHintTextColor(Color.argb(100, 51, 51, 51));
                    }
                }
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
 * @param activity 当前Activity
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
 * @return GradientDrawable对象
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
 * @return StateListDrawable对象
 */
StateListDrawable createSelectableBackground() {
    StateListDrawable drawable = new StateListDrawable();
    drawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.parseColor("#1A000000")));
    drawable.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
    return drawable;
}

/**
 * 添加分区标题
 * @param activity Activity上下文
 * @param parent 父布局
 * @param text 标题文本
 * @param color 文字颜色
 */
void addSectionHeader(Activity activity, LinearLayout parent, String text, int color) {
    TextView tv = new TextView(activity);
    tv.setText(text); tv.setTextSize(13); tv.setTextColor(color);
    tv.setPadding(dp(activity, 4), dp(activity, 10), 0, dp(activity, 6));
    parent.addView(tv);
}

/**
 * 创建卡片组
 * @param activity Activity上下文
 * @param color 背景颜色
 * @param radius 圆角半径
 * @return LinearLayout卡片组
 */
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

/**
 * 添加可点击项
 * @param activity Activity上下文
 * @param parent 父布局
 * @param title 标题
 * @param sub 副标题
 * @param titleColor 标题颜色
 * @param cardColor 卡片颜色
 * @param isLast 是否最后一项
 * @param onClick 点击监听器
 */
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

/**
 * 添加输入项
 * @param activity Activity上下文
 * @param parent 父布局
 * @param title 标题
 * @param value 当前值
 * @param hint 提示文本
 * @param titleColor 标题颜色
 * @param cardColor 卡片颜色
 * @param saveKey 保存键
 * @param defaultValue 默认值
 */
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

/**
 * 使颜色变亮
 * @param color 原始颜色
 * @param factor 变亮因子0-1
 * @return 变亮后的颜色
 */
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

/**
 * 使颜色变暗
 * @param color 原始颜色
 * @param factor 变暗因子0-1
 * @return 变暗后的颜色
 */
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

/**
 * 添加输入项简化版
 * @param activity Activity上下文
 * @param parent 父布局
 * @param title 标题
 * @param value 当前值
 * @param hint 提示文本
 * @param titleColor 标题颜色
 * @param saveKey 保存键
 * @param defaultValue 默认值
 */
void addInputItem(Activity activity, LinearLayout parent, String title, String value, String hint, int titleColor, String saveKey, String defaultValue) {
    int cardColor = Color.parseColor("#FFF5F5F5");
    addInputItem(activity, parent, title, value, hint, titleColor, cardColor, saveKey, defaultValue);
}

/**
 * 添加输入项最简版
 * @param activity Activity上下文
 * @param parent 父布局
 * @param title 标题
 * @param value 当前值
 * @param hint 提示文本
 * @param titleColor 标题颜色
 * @param saveKey 保存键
 */
void addInputItem(Activity activity, LinearLayout parent, String title, String value, String hint, int titleColor, String saveKey) {
    addInputItem(activity, parent, title, value, hint, titleColor, saveKey, null);
}


/**
 * 创建Switch开关控件
 * @param activity Context上下文
 * @param str 开关文本
 * @param state 初始状态
 * @param size 文字大小
 * @param weight 布局权重
 * @return Switch开关实例
 */
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

/**
 * 创建CheckBox复选框
 * @param activity Activity上下文
 * @param text 文本内容
 * @param checked 初始选中状态
 * @param textSizeDp 文字大小dp
 * @param textColor 文字颜色
 * @return CheckBox实例
 */
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

/**
 * 添加分割线
 * @param activity Activity上下文
 * @param parent 父布局
 * @param color 分割线颜色
 */
void addDivider(Activity activity, LinearLayout parent, int color) {
    View v = new View(activity); v.setBackgroundColor(color);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1); lp.leftMargin = dp(activity, 16);
    parent.addView(v, lp);
}

/**
 * 获取可选中背景
 * @param activity Activity上下文
 * @return StateListDrawable可选中背景
 */
StateListDrawable getSelectableBg(Activity activity) {
    android.graphics.drawable.StateListDrawable res = new android.graphics.drawable.StateListDrawable();
    res.setExitFadeDuration(300);
    res.addState(new int[]{android.R.attr.state_pressed}, new android.graphics.drawable.ColorDrawable(Color.parseColor("#1A000000")));
    res.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    return res;
}

/**
 * 获取最大刷新率
 * @param context 上下文
 * @return 最大刷新率
 */
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
