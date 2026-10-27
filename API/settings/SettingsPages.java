void showUiCornerPicker(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    final int[] radius = new int[]{getUiCornerDp()};
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final int textColor = pc(getSettingsThemeColor(activity, "on_surface"));
                final int subColor = pc(getSettingsThemeColor(activity, "on_surface_variant"));
                final int accentColor = pc(getSettingsThemeColor(activity, "primary"));
                final int softColor = pc(getSettingsThemeColor(activity, "outline"));
                final int fieldColor = getAdaptiveInputBg(activity);

                Dialog d = new Dialog(activity);
                d.requestWindowFeature(1);
                try {
                    Window w = d.getWindow();
                    if (w != null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                } catch (Throwable ignore) {}

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(dp(activity, 18), dp(activity, 14), dp(activity, 18), dp(activity, 8));
                root.setBackground(roundRect(getAdaptiveSheetBg(activity), dp(activity, radius[0])));

                LinearLayout body = new LinearLayout(activity);
                body.setOrientation(LinearLayout.VERTICAL);
                body.setClipChildren(false);
                body.setClipToPadding(false);

                TextView title = new TextView(activity);
                title.setText("弹窗圆角");
                title.setTextSize(17);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(textColor);
                title.setGravity(Gravity.CENTER);
                title.setPadding(0, dp(activity, 2), 0, dp(activity, 8));
                body.addView(title);

                LinearLayout valueRow = new LinearLayout(activity);
                valueRow.setOrientation(LinearLayout.HORIZONTAL);
                valueRow.setGravity(Gravity.CENTER | Gravity.BOTTOM);
                final TextView valueTv = new TextView(activity);
                valueTv.setText(String.valueOf(radius[0]));
                valueTv.setTextSize(30);
                valueTv.setTypeface(null, Typeface.BOLD);
                valueTv.setTextColor(accentColor);
                valueRow.addView(valueTv);
                TextView unitTv = new TextView(activity);
                unitTv.setText("dp");
                unitTv.setTextSize(14);
                unitTv.setTextColor(subColor);
                unitTv.setPadding(dp(activity, 5), 0, 0, dp(activity, 5));
                valueRow.addView(unitTv);
                body.addView(valueRow);

                final LinearLayout previewCard = new LinearLayout(activity);
                previewCard.setOrientation(LinearLayout.VERTICAL);
                previewCard.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
                previewCard.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(activity, 122)));
                previewCard.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, radius[0])));

                View barTitle = new View(activity);
                GradientDrawable barTitleBg = new GradientDrawable();
                barTitleBg.setColor(subColor);
                barTitleBg.setCornerRadius(dp(activity, 3));
                barTitle.setBackground(barTitleBg);
                previewCard.addView(barTitle, new LinearLayout.LayoutParams(dp(activity, 84), dp(activity, 9)));

                for (int i = 0; i < 2; i++) {
                    LinearLayout rowLine = new LinearLayout(activity);
                    rowLine.setOrientation(LinearLayout.HORIZONTAL);
                    rowLine.setGravity(Gravity.CENTER_VERTICAL);
                    View dot = new View(activity);
                    GradientDrawable dotBg = new GradientDrawable();
                    dotBg.setShape(GradientDrawable.OVAL);
                    dotBg.setColor(i == 0 ? accentColor : softColor);
                    dot.setBackground(dotBg);
                    LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(activity, 12), dp(activity, 12));
                    dotLp.rightMargin = dp(activity, 8);
                    rowLine.addView(dot, dotLp);
                    View line = new View(activity);
                    GradientDrawable lineBg = new GradientDrawable();
                    lineBg.setColor(softColor);
                    lineBg.setCornerRadius(dp(activity, 3));
                    line.setBackground(lineBg);
                    rowLine.addView(line, new LinearLayout.LayoutParams(0, dp(activity, 8), 1.0f));
                    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                    rowLp.topMargin = dp(activity, 12);
                    previewCard.addView(rowLine, rowLp);
                }

                View fakeBtn = new View(activity);
                GradientDrawable fakeBtnBg = new GradientDrawable();
                fakeBtnBg.setColor(accentColor);
                fakeBtnBg.setCornerRadius(dp(activity, 9));
                fakeBtn.setBackground(fakeBtnBg);
                LinearLayout.LayoutParams fakeBtnLp = new LinearLayout.LayoutParams(dp(activity, 76), dp(activity, 24));
                fakeBtnLp.topMargin = dp(activity, 14);
                previewCard.addView(fakeBtn, fakeBtnLp);

                body.addView(previewCard);

                SoftSliderView slider = new SoftSliderView(activity);
                slider.setRange(0, 32);
                slider.setValue(radius[0]);
                slider.setAccentColor(accentColor);
                slider.setTrackColor(softColor);
                LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(-1, dp(activity, 40));
                sliderLp.topMargin = dp(activity, 8);
                body.addView(slider, sliderLp);

                final int[] presetValues = new int[]{0, 8, 12, 16, 20, 28};
                final TextView[] presetViews = new TextView[presetValues.length];
                final Runnable updatePresets = new Runnable() {
                    public void run() {
                        for (int i = 0; i < presetValues.length; i++) {
                            boolean on = presetValues[i] == radius[0];
                            GradientDrawable chipBg = new GradientDrawable();
                            chipBg.setColor(on ? accentColor : fieldColor);
                            chipBg.setCornerRadius(dp(activity, 9));
                            presetViews[i].setBackground(chipBg);
                            presetViews[i].setTextColor(on ? (isColorDark(accentColor) ? Color.WHITE : Color.BLACK) : subColor);
                        }
                    }
                };

                final Runnable updateAll = new Runnable() {
                    public void run() {
                        valueTv.setText(String.valueOf(radius[0]));
                        root.setBackground(roundRect(getAdaptiveSheetBg(activity), dp(activity, radius[0])));
                        previewCard.setBackground(roundRect(getAdaptiveCardBg(activity), dp(activity, radius[0])));
                        updatePresets.run();
                    }
                };

                LinearLayout presetRow = new LinearLayout(activity);
                presetRow.setOrientation(LinearLayout.HORIZONTAL);
                presetRow.setGravity(Gravity.CENTER);
                presetRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                for (int i = 0; i < presetValues.length; i++) {
                    final int preset = presetValues[i];
                    TextView chip = new TextView(activity);
                    chip.setText(String.valueOf(preset));
                    chip.setTextSize(13);
                    chip.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, dp(activity, 32), 1.0f);
                    chipLp.leftMargin = dp(activity, 2);
                    chipLp.rightMargin = dp(activity, 2);
                    chip.setLayoutParams(chipLp);
                    chip.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            radius[0] = preset;
                            slider.setValue(preset);
                            updateAll.run();
                            try { vibrate(activity, 20); } catch (Throwable ignore) {}
                        }
                    });
                    presetViews[i] = chip;
                    presetRow.addView(chip);
                }
                body.addView(presetRow);

                slider.setOnSoftSliderChangedListener(new SoftSliderListener() {
                    public void onSoftSliderChanged(int value, boolean fromUser) {
                        radius[0] = value;
                        updateAll.run();
                    }
                });

                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                btnRow.setClipChildren(false);
                btnRow.setClipToPadding(false);
                btnRow.setPadding(0, dp(activity, 4), 0, 0);
                btnRow.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

                TextView cancelBtn = createButton(activity, "取消", subColor, Color.TRANSPARENT, 14f, 8, 16, 8, false, 0, 0, new Runnable() {
                    public void run() { try { d.dismiss(); } catch (Throwable ignore) {} }
                });
                cancelBtn.setIncludeFontPadding(false);
                btnRow.addView(cancelBtn, new LinearLayout.LayoutParams(-2, -2));

                TextView okBtn = createButton(activity, "应用", Color.WHITE, accentColor, 14f, 8, 16, 8, false, 0, 0, new Runnable() {
                    public void run() {
                        putString("settings", "ui_corner_dp", String.valueOf(radius[0]));
                        Toast("圆角: " + radius[0] + "dp");
                        try { d.dismiss(); } catch (Throwable ignore) {}
                        refreshCurrentSettingsPage();
                    }
                });
                okBtn.setIncludeFontPadding(false);
                LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(-2, -2);
                okLp.leftMargin = dp(activity, 4);
                btnRow.addView(okBtn, okLp);

                ScrollView scrollWrap = new ScrollView(activity);
                scrollWrap.setVerticalScrollBarEnabled(false);
                scrollWrap.setFillViewport(false);
                scrollWrap.setClipChildren(false);
                scrollWrap.setClipToPadding(false);
                root.setClipChildren(false);
                root.setClipToPadding(false);
                scrollWrap.addView(body);
                root.addView(scrollWrap, new LinearLayout.LayoutParams(-1, -2));
                root.addView(btnRow);
                d.setContentView(root);
                d.show();
                applyUiTheme(activity, d, 0);
                try {
                    Window sizeWin = d.getWindow();
                    if (sizeWin != null) {
                        int wantW = Math.min(dp(activity, 300), activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 48));
                        if (wantW > 0) sizeWin.setLayout(wantW, -2);
                    }
                } catch (Throwable ignore) {}
                updateAll.run();
            } catch (Throwable e) { traceLog("setwindow_log", "[showUiCornerPicker] 异常: " + e); }
        }
    });
}
void buildBasicModePage(Activity activity) {
    addSettingsCategory("基础模式", null);
    addSettingsItemChoiceWithKey( "主题模式", "ui_theme_mode", (("system".equals(getString("settings", "ui_theme_mode", "default")) ? "跟随系统" : "light".equals(getString("settings", "ui_theme_mode", "default")) ? "强制浅色" : "dark".equals(getString("settings", "ui_theme_mode", "default")) ? "强制深色" : "默认（推荐）")), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showThemeModeChoiceDialog(act);
        }
    }});
    addSettingsValueSliderItem( "弹窗宽度", "单位 dp，留空为自适应，默认最大 260dp", "ui_dialog_width", "自适应", 180, 400, 260, true, null, null);
    addSettingsValueSliderItem( "弹窗高度", "单位 dp，留空为自适应内容", "ui_dialog_height", "自适应", 200, 900, 400, true, null, null);
    String dpiMode = getString("settings", "ui_dialog_dpi_mode", "follow_qq");
    if (dpiMode == null || dpiMode.isEmpty()) dpiMode = "follow_qq";
    String dpiLabel = "custom".equals(dpiMode)
        ? ("自定义 " + getString("settings", "ui_dialog_custom_dpi", "320") + " dpi")
        : ("follow_module".equals(dpiMode) ? "跟随模块" : "跟随QQ");
    addSettingsItemChoiceWithKey( "显示密度", "ui_dialog_dpi_mode", dpiLabel, new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showDpiModeChoiceDialog(act);
        }
    }});
    if ("custom".equals(dpiMode)) {
        addSettingsValueSliderItem( "自定义 DPI", "范围 160-640，越大显示越大", "ui_dialog_custom_dpi", "如 320", 160, 640, 320, false, "ui_dialog_dpi_mode", "自定义 %d dpi");
    }
    boolean vibrationFeedbackState = getBoolean("settings", "振动反馈", false);
    addSettingsItemSwitchWithKey("振动反馈", "settings", "振动反馈", null, vibrationFeedbackState, null);
    boolean bgBlurState = getBoolean("settings", "背景模糊", false);
    addSettingsItemSwitchWithKey("背景模糊", "settings", "背景模糊", "系统模糊窗体后方内容(Android 12+)", bgBlurState, null);
    addSettingsItemClickWithKey( "弹窗圆角", ("当前 " + getUiCornerDp() + "dp"), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) showUiCornerPicker(act);
    }});
    addSettingsItemClickWithKey( "预览弹窗", "按当前宽度/高度/显示密度预览", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showDialogPreview(act);
        }
    }});
}
void buildBackgroundStylePage(Activity activity) {
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
                    editAndSaveImage(a, uri, pluginPath + "/API/background.png", 1007);
                }
            });
            Toast("选择后自动居中裁剪应用");
        }});
        addSettingsInputItemWithConfig("图片模糊 (0-25)", "0为不模糊", "settings", "ui_img_blur", "0-25", "0", false, false, null);
        addSettingsInputItemWithConfig("遮罩浓度 (0-255)", "越大越暗", "settings", "ui_img_alpha", "0-255", isDark ? "180" : "100", false, false, null);
    }
}
void buildFontStylePage(Activity activity) {
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
void buildToastStylePage(Activity activity) {
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
    addSettingsInputItemWithConfig("弹出时长(ms)", "500-10000", "settings", "toast_duration", "2000", "2000", false, false, null);

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
        addSettingsItemSwitchWithKey("自适应", "settings", "toast_adaptive", "开：气泡贴合内容+框内位置；关：气泡撑满框+文字填充", adaptiveOn, new Runnable() { public void run() {
refreshCurrentSettingsPage();
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
    addSettingsInputItemWithConfig("前缀", "显示在文字前", "settings", "toast_prefix", "可留空", "", false, false, null);
    addSettingsInputItemWithConfig("后缀", "显示在文字后", "settings", "toast_suffix", "可留空", "", false, false, null);
    addSettingsItemClickWithKey( "测试Toast", "预览当前配置", new Runnable() { public void run() {
        Toast("这是一条测试 Toast");
    }});
}
void buildThreadPoolPage(Activity activity) {
    addSettingsCategory("线程池", null);
    addSettingsItemChoiceWithKey( "线程优先级", "thread_pool_priority", ((getString("settings", "thread_pool_priority", "").isEmpty() ? "5 (默认)" : getString("settings", "thread_pool_priority", "") + "")), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showThreadPriorityChoiceDialog(act);
        }
    }});
    addSettingsInputItemWithConfig("任务队列容量", "默认50", "settings", "thread_pool_queue_capacity", "数字", "50", false, false, null);
    addSettingsInputItemWithConfig("核心线程存活(秒)", "默认30", "settings", "thread_pool_keep_alive", "秒数", "30", false, false, null);
    addSettingsItemChoiceWithKey( "任务满载策略", "thread_pool_reject_policy", (("1".equals(getString("settings", "thread_pool_reject_policy", "0")) ? "丢弃最新任务" : "2".equals(getString("settings", "thread_pool_reject_policy", "0")) ? "抛出异常" : "3".equals(getString("settings", "thread_pool_reject_policy", "0")) ? "调用者执行" : "丢弃最旧任务 (默认)")), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showRejectPolicyChoiceDialog(act);
        }
    }});
}
void buildFloatingWindowPage(Activity activity) {
    addSettingsCategory("悬浮窗设置", null);
    addSettingsItemClickWithKey( "更换图标", (("".equals(getString("settings", "iconPath", "")) || getString("settings", "iconPath", "") == null ? "未设置" : getString("settings", "iconPath", "").toLowerCase().endsWith(".gif") ? "动态图标 (GIF)" : "静态图标")), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act == null) return;
        openFilePicker(act, 1005, "image/*", null, pluginPath + "/API/icon{ext}", new FilePickerCallback() {
            public void onFilePicked(Activity a, Uri uri, String fileName, String filePath) {
                putString("settings", "iconPath", filePath);
                editAndSaveImage(a, uri, filePath, 1005);
            }
        });
        Toast("选择后将自动保存并刷新");
    }});
    addSettingsInputItemWithConfig("悬浮窗大小", "默认48", "settings", "悬浮窗大小", "dp", "48", false, false, null);
    addSettingsInputItemWithConfig("关闭图标大小", "默认24", "settings", "关闭区域图标大小", "dp", "24", false, false, null);
    addSettingsInputItemWithConfig("拖拽灵敏度", "数值越小越灵敏", "settings", "拖拽灵敏度", "数字", "12", false, false, null);
    addSettingsInputItemWithConfig("长按关闭阈值", "默认650ms", "settings", "长按关闭阈值", "毫秒", "650", false, false, null);
    addSettingsInputItemWithConfig("图标透明度", "0-255", "settings", "iconAlpha", "0-255", "255", false, false, null);

    String iconPath = getString("settings", "iconPath", "");
    boolean isAnimatedIcon = iconPath != null && iconPath.toLowerCase().endsWith(".gif");
    if (isAnimatedIcon) {
        addSettingsItemClickWithKey( "GIF播放速度", "带实时预览，越小越快", new Runnable() { public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showGifSpeedPicker(act);
        }});
    }
}
void buildRevokeMarkPage(Activity activity) {
    addSettingsCategory("撤回标记", null);
    boolean timeOn = ShowMsgTime.INSTANCE.isEnable();
    boolean revokeOn = AntiRevoke.INSTANCE.isEnable();
    boolean listenerOn = getBoolean("settings", "revoke_mark_listener", true);
    boolean hostSupported = false;
    try { hostSupported = revokeMarkHostSupported(); } catch (Throwable ignore) { hostSupported = false; }
    String listenerDesc = "关闭后不再注册消息视图监听，可避免部分宿主缺少 AIOViewUpdateListener 导致的加载失败";
    if (!hostSupported) listenerDesc = "当前宿主未提供 AIOViewUpdateListener，已自动跳过，此项无需开启";
    addSettingsItemSwitchWithKey("启用消息视图监听", "settings", "revoke_mark_listener", listenerDesc, listenerOn, new Runnable() { public void run() {
        try {
            if (getBoolean("settings", "revoke_mark_listener", true)) {
                installRevokeMarkListener();
            } else {
                removeRevokeMarkListener();
            }
        } catch (Throwable e) {
            traceLog("setwindow_log", "[已撤回标记] 切换监听开关失败: " + e);
        }
        refreshCurrentSettingsPage();
    }});
    String markDesc = "把模块内置的「已撤回」提示挪到消息时间后面";
    if (!timeOn && !revokeOn) markDesc = "当前状态不可用，请先打开「显示消息时间」和「防撤回」开关";
    else if (!timeOn) markDesc = "当前状态不可用，请先打开「显示消息时间」开关";
    else if (!revokeOn) markDesc = "当前状态不可用，请先打开「防撤回」开关";
    else if (!hostSupported) markDesc = "当前宿主不支持消息视图监听，此项无法生效";
    addSettingsItemSwitchWithKey("已撤回标记", "settings", "revoke_mark", markDesc, getBoolean("settings", "revoke_mark", false), null);
    addSettingsInputItemWithConfig("标记文本", "默认「已撤回」，追加在消息时间后面", "settings", "revoke_mark_text", "如: 已撤回", "已撤回", false, false, null);
    addSettingsItemChoiceWithKey( "标记位置", "revoke_mark_pos", ("top".equals(getString("settings", "revoke_mark_pos", "bottom")) ? "气泡上方" : "气泡下方"), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act == null) return;
        String[] names = {"气泡上方", "气泡下方"};
        String[] values = {"top", "bottom"};
        int checked = "top".equals(getString("settings", "revoke_mark_pos", "bottom")) ? 0 : 1;
        showChoiceDialogEx(act, "标记位置", names, values, "revoke_mark_pos", "bottom", checked, 1, "标记位置: ", new Runnable() { public void run() { refreshCurrentSettingsPage(); } });
    }});
    addSettingsItemChoiceWithKey( "标记颜色", "revoke_mark_color_mode", ("custom".equals(getString("settings", "revoke_mark_color_mode", "module")) ? "自定义" : "跟随模块"), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act == null) return;
        String[] names = {"跟随模块", "自定义"};
        String[] values = {"module", "custom"};
        int checked = "custom".equals(getString("settings", "revoke_mark_color_mode", "module")) ? 1 : 0;
        showChoiceDialogEx(act, "标记颜色", names, values, "revoke_mark_color_mode", "module", checked, 1, "标记颜色: ", new Runnable() { public void run() { refreshCurrentSettingsPage(); } });
    }});
    addSettingsItemClickWithKey( "自定义颜色", getString("settings", "revoke_mark_color", "#FFFF0000"), new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act == null) return;
        showColorPickerDialog(act, getString("settings", "revoke_mark_color", "#FFFF0000"), new OnColorPickedListener() {
            public void onColorPicked(int color) {
                putString("settings", "revoke_mark_color", colorToHex(color));
                refreshCurrentSettingsPage();
            }
        });
    }});

    LinearLayout pvBox = settingsCurrentCategoryContainer();
    if (pvBox != null) {
        TextView pvLabel = new TextView(activity);
        pvLabel.setText("预览");
        pvLabel.setTextSize(12);
        pvLabel.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
        pvLabel.setPadding(dp(activity, 16), dp(activity, 4), dp(activity, 16), 0);
        pvBox.addView(pvLabel);

        FrameLayout pv = new FrameLayout(activity);
        pv.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(activity, 96)));
        pv.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));

        View pvBubble = new View(activity);
        FrameLayout.LayoutParams pvBubbleLp = new FrameLayout.LayoutParams(dp(activity, 120), dp(activity, 36));
        pvBubbleLp.gravity = Gravity.CENTER;
        pvBubble.setLayoutParams(pvBubbleLp);
        GradientDrawable pvBubbleBg = new GradientDrawable();
        pvBubbleBg.setColor(pc(getSettingsThemeColor(activity, "primary_container")));
        pvBubbleBg.setCornerRadius(dp(activity, 10));
        pvBubble.setBackground(pvBubbleBg);
        pv.addView(pvBubble);

        TextView pvMark = new TextView(activity);
        pvMark.setText("12:34:56" + revokeMarkText());
        pvMark.setTextSize(10.0f);
        pvMark.setTextColor(revokeMarkColor());
        FrameLayout.LayoutParams pvMarkLp = new FrameLayout.LayoutParams(-2, -2);
        if ("top".equals(getString("settings", "revoke_mark_pos", "bottom"))) {
            pvMarkLp.gravity = Gravity.TOP | Gravity.END;
        } else {
            pvMarkLp.gravity = Gravity.BOTTOM | Gravity.END;
        }
        pvMark.setLayoutParams(pvMarkLp);
        pv.addView(pvMark);

        pvBox.addView(pv);
    }

    addSettingsCategory("模块开关", "");
    addSettingsItemSwitchWithKey("显示消息时间", null, null, "QFun 模块：气泡显示消息发送时间", timeOn, new Runnable() { public void run() {
        ShowMsgTime.INSTANCE.setEnable(!ShowMsgTime.INSTANCE.isEnable());
        refreshCurrentSettingsPage();
    }});
    addSettingsItemSwitchWithKey("防撤回", null, null, "QFun 模块：拦截撤回指令并保留消息", revokeOn, new Runnable() { public void run() {
        AntiRevoke.INSTANCE.setEnable(!AntiRevoke.INSTANCE.isEnable());
        refreshCurrentSettingsPage();
    }});
}
void buildDebugPage(Activity activity) {
    addSettingsCategory("调试", null);
    addSettingsItemClickWithKey( "预览设置", "预览当前设置效果", new Runnable() { public void run() {
        Activity act = getSettingsCurrentActivity();
        if (act != null) {
            showSettingsPreviewPopup(act);
        }
    }});
    String logThreshold = getString("settings", "log_delete_threshold", "1");
    addSettingsInputItemWithConfig("日志大小阈值(MB)", "日志文件夹超过此大小自动清理", "settings", "log_delete_threshold", "如: 1", logThreshold, false, false, null);
}
public LinearLayout buildStatsContentView(Activity activity) {
    if (activity == null) return null;
    readFullStats();

    LinearLayout contentLayout = new LinearLayout(activity);
    contentLayout.setOrientation(LinearLayout.VERTICAL);
    contentLayout.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

    contentLayout.addView(createRangeSpinner(activity));
    contentLayout.addView(createSpaceView(activity, 12));

    LinearLayout wlCard = createWhitelistCard(activity);
    if (wlCard != null) {
        contentLayout.addView(wlCard);
        contentLayout.addView(createSpaceView(activity, 12));
    }

    contentLayout.addView(createTotalStatsCard(activity, "📈 累计统计"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createTodayCoreStatsCard(activity, "📊  今日统计"));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createCategoryStatsCard(activity, "📥 接收消息统计", "receive", true));
    contentLayout.addView(createSpaceView(activity, 12));
    contentLayout.addView(createCategoryStatsCard(activity, "📤 发送消息统计", "send", false));
    contentLayout.addView(createSpaceView(activity, 18));

    LinearLayout entry = new LinearLayout(activity);
    entry.setOrientation(LinearLayout.HORIZONTAL);
    entry.setGravity(Gravity.CENTER_VERTICAL);
    entry.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
    entry.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), dp(activity, 16)));
    entry.setClipToOutline(true);
    entry.setClickable(true);

    TextView entryLabel = new TextView(activity);
    entryLabel.setText("消息统计设置");
    entryLabel.setTextSize(16);
    entryLabel.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    entryLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    entry.addView(entryLabel);

    TextView entryArrow = new TextView(activity);
    entryArrow.setText("›");
    entryArrow.setTextSize(20);
    entryArrow.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    entry.addView(entryArrow);

    entry.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            Activity act = getSettingsCurrentActivity();
            if (act != null) showSettingsMenu(act, "消息与互动", "消息统计设置", null);
        }
    });
    contentLayout.addView(entry);
    return contentLayout;
}
void buildStatsSettingsView(Activity activity) {
    if (activity == null) return;

    boolean wlOn = getBoolean("settings", "msg_stats_whitelist_enabled", false);

    addSettingsCategory("统计范围", "");
    addSettingsItemSwitchWithKey("按好友/群聊单独统计", "settings", "msg_stats_whitelist_enabled",
        "开启后仅白名单内的群/好友单独统计，其他仍汇总", wlOn, new Runnable() {
            public void run() { rebuildStatsSettingsPage(); }
        });

    String gs = getString("settings", "msg_stats_whitelist_groups", "");
    String fs = getString("settings", "msg_stats_whitelist_friends", "");
    int gCount = 0, fCount = 0;
    if (gs != null && !gs.isEmpty()) {
        String[] p = gs.split(",");
        for (int i = 0; i < p.length; i++) if (!p[i].trim().isEmpty()) gCount++;
    }
    if (fs != null && !fs.isEmpty()) {
        String[] p = fs.split(",");
        for (int i = 0; i < p.length; i++) if (!p[i].trim().isEmpty()) fCount++;
    }

    addSettingsItemClickWithKey("群聊白名单", wlOn ? ("已选 " + gCount + " 个群") : "需先开启上方开关", new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act == null) return;
            String cur = getString("settings", "msg_stats_whitelist_groups", "");
            ArrayList init = new ArrayList();
            if (cur != null && !cur.isEmpty()) {
                String[] p = cur.split(",");
                for (int i = 0; i < p.length; i++) if (!p[i].trim().isEmpty()) init.add(p[i].trim());
            }
            showGroupSelector(act, 2, init, new GroupSelectCallback() {
                public void onSelected(List selected) {
                    StringBuilder sb = new StringBuilder();
                    if (selected != null) {
                        for (int i = 0; i < selected.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(selected.get(i));
                        }
                    }
                    putString("settings", "msg_stats_whitelist_groups", sb.toString());
                    Toast("已保存 " + (selected == null ? 0 : selected.size()) + " 个群");
                    rebuildStatsSettingsPage();
                }
            });
        }
    });
    String groupKey = settingsAutoItemKey("群聊白名单");
    View groupView = (SettingsState.settingsItemViews == null) ? null : (View) SettingsState.settingsItemViews.get(groupKey);
    if (groupView != null && !wlOn) {
        groupView.setAlpha(0.4f);
        groupView.setOnClickListener(null);
        groupView.setClickable(false);
    }

    addSettingsItemClickWithKey("好友白名单", wlOn ? ("已选 " + fCount + " 个好友") : "需先开启上方开关", new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act == null) return;
            String cur = getString("settings", "msg_stats_whitelist_friends", "");
            ArrayList init = new ArrayList();
            if (cur != null && !cur.isEmpty()) {
                String[] p = cur.split(",");
                for (int i = 0; i < p.length; i++) if (!p[i].trim().isEmpty()) init.add(p[i].trim());
            }
            showGroupSelector(act, 1, init, new GroupSelectCallback() {
                public void onSelected(List selected) {
                    StringBuilder sb = new StringBuilder();
                    if (selected != null) {
                        for (int i = 0; i < selected.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(selected.get(i));
                        }
                    }
                    putString("settings", "msg_stats_whitelist_friends", sb.toString());
                    Toast("已保存 " + (selected == null ? 0 : selected.size()) + " 个好友");
                    rebuildStatsSettingsPage();
                }
            });
        }
    });
    String friendKey = settingsAutoItemKey("好友白名单");
    View friendView = (SettingsState.settingsItemViews == null) ? null : (View) SettingsState.settingsItemViews.get(friendKey);
    if (friendView != null && !wlOn) {
        friendView.setAlpha(0.4f);
        friendView.setOnClickListener(null);
        friendView.setClickable(false);
    }

    addSettingsCategory("收发总开关", "");
    addSettingsItemSwitchWithKey("接收统计", "settings", "msg_stats_receive_enabled",
        "关闭后不统计任何接收数据", getBoolean("settings", "msg_stats_receive_enabled", true), null);
    addSettingsItemSwitchWithKey("发送统计", "settings", "msg_stats_send_enabled",
        "关闭后不统计任何发送数据", getBoolean("settings", "msg_stats_send_enabled", true), null);

    addSettingsCategory("统计类别", "");
    for (int i = 0; i < STAT_CATEGORIES.length; i++) {
        String catKey = STAT_CATEGORIES[i][0];
        String catLabel = STAT_CATEGORIES[i][1];
        addSettingsItemSwitchWithKey(catLabel, "settings", "msg_stats_cat_" + catKey, null,
            getBoolean("settings", "msg_stats_cat_" + catKey, true), null);
    }

    addSettingsCategory("每日目标", "");
    addSettingsInputItemWithConfig("每日消息目标", "默认 100", "settings", "msg_stats_daily_target", "如: 100", "100", false, false, null);

    addSettingsCategory("数据操作", "");
    addSettingsItemClickWithKey("重置今日数据", "清空今日所有统计", new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) resetTodayStats(act);
        }
    });
    addSettingsItemClickWithKey("重置累计数据", "清空所有累计统计", new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) resetTotalStats(act);
        }
    });
    addSettingsItemClickWithKey("修复数据", "重算所有 total 键", new Runnable() {
        public void run() {
            Activity act = getSettingsCurrentActivity();
            if (act != null) repairStatsData(act);
        }
    });
}
SettingsFeatureBuilder statsBoardFeatureBuilder() {
    return new SettingsFeatureBuilder() {
        public View build(Activity activity) {
            if (SettingsState.settingsIndexMode) return null;
            LinearLayout board = buildStatsContentView((activity != null) ? activity : getSettingsCurrentActivity());
            if (board != null) bindStatsViewCache(board);
            return board;
        }
    };
}

SettingsFeatureBuilder statsSettingsFeatureBuilder() {
    return new SettingsFeatureBuilder() {
        public View build(Activity activity) {
            buildStatsSettingsView((activity != null) ? activity : getSettingsCurrentActivity());
            return null;
        }
    };
}

void rebuildStatsSettingsPage() {
    Activity act = getSettingsCurrentActivity();
    if (act == null) return;
    LinearLayout listContainer = SettingsState.settingsListContainer;
    if (listContainer == null) return;
    try {
        listContainer.removeAllViews();
        if (SettingsState.settingsCategoryContainers != null) SettingsState.settingsCategoryContainers.clear();
        if (SettingsState.settingsItemViews != null) SettingsState.settingsItemViews.clear();
        if (SettingsState.settingsItemTextViews != null) SettingsState.settingsItemTextViews.clear();
        SettingsState.settingsCurrentCategory = null;
        buildStatsSettingsView(act);
    } catch (Throwable e) {
        traceLog("setwindow_log", "[rebuildStatsSettingsPage] 异常: " + e);
    }
}

public void refreshStatsBoardOnReturn() {
    Activity act = getSettingsCurrentActivity();
    if (!(act instanceof SettingsActivity)) {
        unbindStatsViewCache();
        return;
    }
    SettingsActivity sa = (SettingsActivity) act;
    if (sa.settingsPageStack == null || sa.settingsPageStack.isEmpty()) {
        unbindStatsViewCache();
        return;
    }
    Object topObj = sa.settingsPageStack.get(sa.settingsPageStack.size() - 1);
    if (topObj == null) {
        unbindStatsViewCache();
        return;
    }
    try {
        Object l2 = fieldGet(topObj, "level2");
        if (!"消息统计".equals(l2)) {
            unbindStatsViewCache();
            return;
        }
        Object pvObj = fieldGet(topObj, "pageView");
        if (!(pvObj instanceof LinearLayout)) {
            unbindStatsViewCache();
            return;
        }
        LinearLayout pageLayout = (LinearLayout) pvObj;
        if (pageLayout.getChildCount() < 2) {
            unbindStatsViewCache();
            return;
        }
        View scrollChild = pageLayout.getChildAt(1);
        if (!(scrollChild instanceof ScrollView)) {
            unbindStatsViewCache();
            return;
        }
        unbindStatsViewCache();
        LinearLayout newContent = buildStatsContentView(act);
        if (newContent == null) return;
        ScrollView sv = (ScrollView) scrollChild;
        sv.removeAllViews();
        sv.addView(newContent);
        bindStatsViewCache(newContent);
    } catch (Throwable e) {
        traceLog("setwindow_log", "[refreshStatsBoardOnReturn] 异常: " + e);
        unbindStatsViewCache();
    }
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
void showVariablePool(Activity activity, final EditText insertTarget, boolean forceDialog, String context) {
    if (activity == null) return;
    boolean inSettings = !forceDialog && (activity instanceof SettingsActivity);
    View content = buildVariablePoolView(activity, insertTarget, context);
    if (inSettings) {
        ((SettingsActivity) activity).settingsPushContentViewRaw("变量池", content);
        return;
    }
    int dialogH = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.65);
    FrameLayout wrap = new FrameLayout(activity);
    content.setLayoutParams(new FrameLayout.LayoutParams(-1, dialogH));
    wrap.addView(content);
    AlertDialog dialog = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle("变量池")
        .setView(wrap)
        .setPositiveButton("关闭", null)
        .show();
    applyUiTheme(activity, dialog, 0);
}
View buildVariablePoolView(final Activity activity, final EditText insertTarget, final String context) {
    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), 0);

    final EditText search = makeInput(activity, "搜索变量名 / 描述 / 值", null);
    search.setTextSize(14);
    search.setSingleLine(true);
    root.addView(search);

    final LinearLayout chipsRow = new LinearLayout(activity);
    chipsRow.setOrientation(LinearLayout.HORIZONTAL);
    chipsRow.setPadding(0, dp(activity, 10), 0, dp(activity, 6));
    android.widget.HorizontalScrollView chipsScroll = new android.widget.HorizontalScrollView(activity);
    chipsScroll.setHorizontalScrollBarEnabled(false);
    chipsScroll.addView(chipsRow);
    root.addView(chipsScroll);

    final LinearLayout results = new LinearLayout(activity);
    results.setOrientation(LinearLayout.VERTICAL);
    results.setPadding(0, 0, 0, dp(activity, 16));

    final String[] selectedGroup = { "推荐" };
    final Runnable[] refresh = new Runnable[1];
    refresh[0] = new Runnable() {
        public void run() {
            results.removeAllViews();
            List items = collectVariablePool(selectedGroup[0], search.getText().toString(), context);
            if (items.isEmpty()) {
                TextView empty = new TextView(activity);
                empty.setText("「" + selectedGroup[0] + "」没有匹配的变量");
                empty.setTextSize(13);
                empty.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, dp(activity, 24), 0, dp(activity, 24));
                results.addView(empty);
                return;
            }
            for (int i = 0; i < items.size(); i++) {
                final String[] it = (String[]) items.get(i);
                LinearLayout card = new LinearLayout(activity);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
                card.setBackground(makeFeedbackBg(getAdaptiveCardBg(activity), pc(getSettingsThemeColor(activity, "ripple")), dp(activity, 12)));
                card.setClipToOutline(true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(activity, 6);
                card.setLayoutParams(lp);

                TextView nameView = new TextView(activity);
                nameView.setText(it[0]);
                nameView.setTextSize(14);
                nameView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
                card.addView(nameView);

                TextView descView = new TextView(activity);
                descView.setText(it[1] + "  ·  " + it[3]);
                descView.setTextSize(12);
                descView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
                card.addView(descView);

                TextView valueView = new TextView(activity);
                valueView.setText("= " + it[2]);
                valueView.setTextSize(11);
                valueView.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
                card.addView(valueView);

                card.setClickable(true);
                card.setOnClickListener(new View.OnClickListener() { public void onClick(View view) {
                    onVariablePoolPick(activity, it[0], it[1], insertTarget);
                }});
                results.addView(card);
            }
        }
    };

    for (int i = 0; i < VAR_POOL_GROUPS.length; i++) {
        final String group = VAR_POOL_GROUPS[i];
        final TextView chip = makeChip(activity, group, group.equals(selectedGroup[0]), 1);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
        clp.rightMargin = dp(activity, 8);
        chip.setLayoutParams(clp);
        chip.setOnClickListener(new View.OnClickListener() { public void onClick(View view) {
            selectedGroup[0] = group;
            for (int k = 0; k < chipsRow.getChildCount(); k++) {
                View cv = chipsRow.getChildAt(k);
                if (cv instanceof TextView) {
                    String ct = ((TextView) cv).getText().toString();
                    setChipWithType((TextView) cv, group.equals(ct), 1);
                }
            }
            refresh[0].run();
        }});
        chipsRow.addView(chip);
    }

    search.addTextChangedListener(new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        public void afterTextChanged(Editable editable) { refresh[0].run(); }
    });

    ScrollView sv = new ScrollView(activity);
    sv.setVerticalScrollBarEnabled(false);
    sv.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
    sv.addView(results);
    root.addView(sv);

    refresh[0].run();
    return root;
}
void onVariablePoolPick(final Activity activity, final String varName, String varDesc, final EditText insertTarget) {
    if (activity == null || varName == null) return;
    boolean forInsert = (insertTarget != null);
    String shown = forInsert ? varName : ("#" + varName + "#");
    String message = shown + (varDesc != null && varDesc.length() > 0 ? ("\n\n" + varDesc) : "");
    new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
        .setTitle(forInsert ? "插入变量" : "复制变量")
        .setMessage(message)
        .setPositiveButton(forInsert ? "插入" : "复制", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try {
                    if (insertTarget != null) {
                        int start = insertTarget.getSelectionStart();
                        if (start < 0) start = insertTarget.getText().length();
                        insertTarget.getText().insert(start, varName);
                        Toast("已插入: " + varName);
                    } else {
                        android.content.ClipboardManager cm = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("变量", "#" + varName + "#"));
                            Toast("已复制: #" + varName + "#");
                        }
                    }
                } catch (Throwable e) { Toast("操作失败: " + e.getMessage()); }
            }
        })
        .setNegativeButton("取消", null)
        .show();
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
