/*
* 设置选择回调
*/
interface OnChoiceSelected {
    void onSelect(String val);
}

void showStyleSettingsDialog(final Activity activity) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final boolean isDark = isEffectiveDarkMode(activity);
                
                int cardColor = isDark ? Color.parseColor("#FF2D2D2D") : Color.parseColor("#FFF5F5F5");
                int subTextColor = isDark ? Color.parseColor("#AAFFFFFF") : Color.parseColor("#99000000");
                int dividerColor = isDark ? Color.parseColor("#1AFFFFFF") : Color.parseColor("#1A000000");
                int textColor = isDark ? Color.parseColor("#FFEFEFEF") : Color.parseColor("#FF333333");
                
                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                
                LinearLayout header = new LinearLayout(activity);
                header.setOrientation(LinearLayout.VERTICAL);
                header.setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 16), dp(activity, 8));
                header.setBackgroundColor(Color.TRANSPARENT);
                
                LinearLayout titleRow = new LinearLayout(activity);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);
                
                TextView title = new TextView(activity);
                title.setText("设置");
                title.setTextSize(22);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(isDark ? Color.parseColor("#FFEFEFEF") : Color.parseColor("#FF333333"));
                title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                titleRow.addView(title);
                
                TextView closeBtn = new TextView(activity);
                closeBtn.setText("取消");
                closeBtn.setTextSize(16);
                closeBtn.setTextColor(isDark ? Color.parseColor("#AAFFFFFF") : Color.parseColor("#99000000"));
                closeBtn.setPadding(dp(activity, 12), 0, dp(activity, 4), 0);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Toast("已取消");
                        pendingSettingsChanges.clear();
                        dialog.dismiss();
                    }
                });
                
                TextView previewBtn = new TextView(activity);
                previewBtn.setText("预览");
                previewBtn.setTextSize(16);
                previewBtn.setTextColor(Color.parseColor("#007AFF"));
                previewBtn.setPadding(dp(activity, 12), 0, dp(activity, 8), 0);
                previewBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        savePendingSettings();
                        showPreviewPopup(activity);
                    }
                });
                
                TextView resetBtn = new TextView(activity);
                resetBtn.setText("重置");
                resetBtn.setTextSize(16);
                resetBtn.setTextColor(Color.parseColor("#FF9500"));
                resetBtn.setPadding(dp(activity, 12), 0, dp(activity, 8), 0);
                resetBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        pendingSettingsChanges.clear();
                        Toast("已重置");
                        dialog.dismiss();
                    }
                });
                
                TextView saveBtn = new TextView(activity);
                saveBtn.setText("保存");
                saveBtn.setTextSize(16);
                saveBtn.setTextColor(Color.parseColor("#34C759"));
                saveBtn.setPadding(dp(activity, 12), 0, dp(activity, 8), 0);
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int savedCount = savePendingSettings();
                        String msg = savedCount > 0 ? "已保存" + savedCount + "项设置" : "无更改";
                        Toast(msg);
                        dialog.dismiss();
                    }
                });
                
                LinearLayout buttonRow = new LinearLayout(activity);
                buttonRow.setOrientation(LinearLayout.HORIZONTAL);
                buttonRow.addView(resetBtn);
                buttonRow.addView(previewBtn);
                buttonRow.addView(saveBtn);
                buttonRow.addView(closeBtn);
                LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(-2, -2);
                buttonRowParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                titleRow.addView(buttonRow, buttonRowParams);
                header.addView(titleRow);
                
                TextView subTitle = new TextView(activity);
                subTitle.setText("深度定制你的QFX");
                subTitle.setTextSize(12);
                subTitle.setTextColor(subTextColor);
                subTitle.setPadding(0, dp(activity, 4), 0, 0);
                header.addView(subTitle);
                root.addView(header);
                
                ScrollView scroll = new ScrollView(activity);
                scroll.setVerticalScrollBarEnabled(false);
                scroll.setBackgroundColor(Color.TRANSPARENT);
                
                LinearLayout list = new LinearLayout(activity);
                list.setOrientation(LinearLayout.VERTICAL);
                list.setPadding(dp(activity, 16), 0, dp(activity, 16), dp(activity, 30));
                list.setBackgroundColor(Color.TRANSPARENT);
                
                addSectionHeader(activity, list, "基础模式", subTextColor);
                LinearLayout group1 = createCardGroup(activity, cardColor, 12);
                
                String currMode = getSetting("settings", "ui_theme_mode", "default");
                final String[] modes = {"默认（推荐）", "跟随系统", "强制浅色", "强制深色"};
                final String[] modeVals = {"default", "system", "light", "dark"};
                String modeDisplay = "默认（推荐）";
                if(currMode.equals("system")) modeDisplay = "跟随系统";
                else if(currMode.equals("light")) modeDisplay = "强制浅色";
                else if(currMode.equals("dark")) modeDisplay = "强制深色";
                
                addClickableItem(activity, group1, "主题模式", modeDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        showM3ChoiceDialog(activity, "主题模式", modes, modeVals, "ui_theme_mode", new OnChoiceSelected() {
                            public void onSelect(String val) {
                                if(val.equals("dark")) putBoolean("settings", "黑白", true);
                                else if(val.equals("light")) putBoolean("settings", "黑白", false);
                                else if(val.equals("default")) putBoolean("settings", "黑白", false);
                                dialog.dismiss();
                                showStyleSettingsDialog(activity);
                            }
                        });
                    }
                });
                
                String scaleVal = getSetting("settings", "ui_dialog_scale", "");
                String scaleDisplay = scaleVal.isEmpty() ? "1.0x (默认)" : scaleVal + "x";
                addClickableItem(activity, group1, "弹窗大小(比例)", scaleDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        showScaleSliderDialog(activity, new OnChoiceSelected() {
                            public void onSelect(String val) {
                                if (val != null) {
                                    scaleDisplay = val + "x";
                                    dialog.dismiss();
                                    showStyleSettingsDialog(activity);
                                }
                            }
                        });
                    }
                });

                String widthVal = getSetting("settings", "ui_dialog_width", "");
                addInputItem(activity, group1, "弹窗宽度", widthVal, "默认最大260dp，两边各留12dp", textColor, cardColor, "ui_dialog_width", "");
                
                String heightVal = getSetting("settings", "ui_dialog_height", "");
                addInputItem(activity, group1, "弹窗高度", heightVal, "自适应内容", textColor, cardColor, "ui_dialog_height", "");

                LinearLayout switchContainer = new LinearLayout(activity);
                switchContainer.setOrientation(LinearLayout.HORIZONTAL);
                switchContainer.setGravity(Gravity.CENTER_VERTICAL);
                switchContainer.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14));
                
                int itemColor = lightenColor(cardColor, 0.35f);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(itemColor);
                bg.setCornerRadius(dp(activity, 8));
                switchContainer.setBackgroundDrawable(bg);
                
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                containerParams.topMargin = dp(activity, 4);
                containerParams.bottomMargin = dp(activity, 4);
                switchContainer.setLayoutParams(containerParams);
                    
                TextView label = new TextView(activity);
                label.setText("振动反馈");
                label.setTextSize(16);
                label.setTextColor(textColor);
                label.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                switchContainer.addView(label);
                
                Switch switch1 = createSwitch(activity, " ",getSettingBoolean("settings", "振动反馈", true), 16, 0);
                switchContainer.addView(switch1);
                switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        pendingSettingsChanges.put("振动反馈",isChecked ? "true":"false");
                    }
                });
                group1.addView(switchContainer);

                View space = new LinearLayout(activity);
                space.setBackgroundColor(Color.TRANSPARENT);
                LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(-1, dp(activity, 8));
                group1.addView(space, spaceParams);
                
                list.addView(group1);
                
                addSectionHeader(activity, list, "背景与图标", subTextColor);
                LinearLayout group2 = createCardGroup(activity, cardColor, 12);
                
                String bgType = getSetting("settings", "ui_bg_type", "color");
                final String[] bgTypes = {"纯色背景", "三色渐变", "图片背景"};
                final String[] bgTypeVals = {"color", "gradient", "image"};
                String bgDisplay = bgType.equals("image") ? "图片背景" : (bgType.equals("gradient") ? "三色渐变" : "纯色背景");
                
                addClickableItem(activity, group2, "背景类型", bgDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        showM3ChoiceDialog(activity, "背景类型", bgTypes, bgTypeVals, "ui_bg_type", new OnChoiceSelected() {
                            public void onSelect(String val) {
                                dialog.dismiss();
                                showStyleSettingsDialog(activity);
                            }
                        });
                    }
                });
                
                String suffix = isDark ? " (深色模式)" : " (浅色模式)";
                
                if ("color".equals(bgType)) {
                    final String[] colorNames = isDark 
                        ? new String[]{"默认黑", "深空灰", "午夜蓝", "暗夜紫", "墨绿", "酒红", "深褐"} 
                        : new String[]{"默认白", "米白", "柔粉", "天蓝", "薄荷", "香芋紫", "柠檬黄"};
                    final String[] colorVals = isDark
                        ? new String[]{"#FF1E1E1E", "#FF2D2D2D", "#FF1A237E", "#FF4A148C", "#FF1B5E20", "#FF880E4F", "#FF3E2723"}
                        : new String[]{"#FFFFFF", "#FFF8F0", "#FFF0F5", "#E6F7FF", "#F0FFF0", "#E6E6FA", "#FFFFF0"};
                    final String key = isDark ? "ui_bg_color_dark" : "ui_bg_color_light";
                    
                    addClickableItem(activity, group2, "预设颜色" + suffix, "点击选择内置配色", textColor, cardColor, false, new View.OnClickListener() {
                        public void onClick(View v) {
                            showM3ChoiceDialog(activity, "预设颜色", colorNames, colorVals, key, new OnChoiceSelected() {
                                public void onSelect(String val) {
                                    dialog.dismiss();
                                    showStyleSettingsDialog(activity);
                                }
                            });
                        }
                    });
                    String val = getSetting("settings", key, isDark ? "#FF1E1E1E" : "#FFFFFF");
                    addColorInputItem(activity, group2, "自定义Hex", val, "#RRGGBB", textColor, cardColor, key);
                    
                } else if ("gradient".equals(bgType)) {
                    final String[] gradNames = {"默认渐变", "落日余晖", "深海幽蓝", "清新森林", "梦幻紫罗兰", "极光", "黑金"};
                    final String[] gradVals = isDark
                        ? new String[]{"#FF2C2C2C,#FF121212,#FF2C2C2C", "#FF4E342E,#FF3E2723,#FF4E342E", "#FF1A237E,#FF0D47A1,#FF1A237E", "#FF1B5E20,#FF33691E,#FF1B5E20", "#FF4A148C,#FF311B92,#FF4A148C", "#FF006064,#FF004D40,#FF006064", "#FF212121,#FF000000,#FF212121"}
                        : new String[]{"#FFFFFFFF,#FFF5F5F5,#FFFFFFFF", "#FFFFE0B2,#FFFFCC80,#FFFFE0B2", "#FFBBDEFB,#FF90CAF9,#FFBBDEFB", "#FFC8E6C9,#FFA5D6A7,#FFC8E6C9", "#FFE1BEE7,#FFCE93D8,#FFE1BEE7", "#FFB2EBF2,#FF80DEEA,#FFB2EBF2", "#FFF5F5F5,#FFE0E0E0,#FFF5F5F5"};
                    final String key = isDark ? "ui_bg_gradient_dark" : "ui_bg_gradient_light";
                    
                    addClickableItem(activity, group2, "预设渐变" + suffix, "点击选择内置渐变", textColor, cardColor, false, new View.OnClickListener() {
                        public void onClick(View v) {
                            showM3ChoiceDialog(activity, "预设渐变", gradNames, gradVals, key, new OnChoiceSelected() {
                                public void onSelect(String val) {
                                    dialog.dismiss();
                                    showStyleSettingsDialog(activity);
                                }
                            });
                        }
                    });
                    String gradVal = getSetting("settings", key, "");
                    addInputItem(activity, group2, "自定义渐变", gradVal, "Hex1,Hex2,Hex3", textColor, cardColor, key, null);
                    
                } else if ("image".equals(bgType)) {
                    addClickableItem(activity, group2, "选择背景图片", "点击选择本地图片", textColor, cardColor, false, new View.OnClickListener() {
                        public void onClick(View v) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("image/*");
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                activity.startActivityForResult(intent, 1007);
                                Toast("选择后自动居中裁剪应用");
                            } catch(Exception e) { 
                                Toast("失败: " + e); 
                            }
                        }
                    });
                    
                    String blur = getSetting("settings", "ui_img_blur", "0");
                    addInputItem(activity, group2, "图片模糊 (0-25)", blur, "0为不模糊", textColor, cardColor, "ui_img_blur", "0");
                    
                    String alpha = getSetting("settings", "ui_img_alpha", isDark ? "180" : "100");
                    addInputItem(activity, group2, "遮罩浓度 (0-255)", alpha, "越大越暗", textColor, cardColor, "ui_img_alpha", isDark ? "180" : "100");
                }
                
                list.addView(group2);
                
                // ==================== 字体样式 ====================
                addSectionHeader(activity, list, "字体样式", subTextColor);
                LinearLayout group3 = createCardGroup(activity, cardColor, 12);
                
                String currFont = getSetting("settings", "ui_font_type", "default");
                final String[] fonts = {"默认字体", "衬线体", "无衬线", "等宽", "粗体"};
                final String[] fontVals = {"default", "serif", "sans", "monospace", "bold"};
                String fontDisplay = "默认字体";
                for(int i=0; i<fontVals.length; i++) if(fontVals[i].equals(currFont)) fontDisplay = fonts[i];
                
                addClickableItem(activity, group3, "字体风格", fontDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        showM3ChoiceDialog(activity, "字体风格", fonts, fontVals, "ui_font_type", new OnChoiceSelected() {
                            public void onSelect(String val) {
                                dialog.dismiss();
                                showStyleSettingsDialog(activity);
                            }
                        });
                    }
                });
                
                String currSize = getSetting("settings", "ui_font_size", "1.0");
                final String[] sizes = {"小 (0.85x)", "默认 (1.0x)", "中 (1.15x)", "大 (1.3x)"};
                final String[] sizeVals = {"0.85", "1.0", "1.15", "1.3"};
                String sizeDisplay = "默认 (1.0x)";
                for(int i=0; i<sizeVals.length; i++) if(sizeVals[i].equals(currSize)) sizeDisplay = sizes[i];
                
                addClickableItem(activity, group3, "字体大小", sizeDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        showM3ChoiceDialog(activity, "字体大小", sizes, sizeVals, "ui_font_size", new OnChoiceSelected() {
                            public void onSelect(String val) {
                                dialog.dismiss();
                                showStyleSettingsDialog(activity);
                            }
                        });
                    }
                });
                
                String tKey = isDark ? "ui_text_color_dark" : "ui_text_color_light";
                String tVal = getSetting("settings", tKey, "");
                addColorInputItem(activity, group3, "字体颜色" + suffix, tVal, "留空自动配色 (推荐)", textColor, cardColor, tKey);
                
                list.addView(group3);
                
                // ==================== 线程池 ====================
                addSectionHeader(activity, list, "线程池", subTextColor);
                LinearLayout group4 = createCardGroup(activity, cardColor, 12);
                
                String priorityVal = getSetting("settings", "thread_pool_priority", "");
                String priorityDisplay;
                if (priorityVal.isEmpty()) {
                    priorityDisplay = "5 (默认)";
                } else {
                    switch (priorityVal) {
                        case "1": priorityDisplay = "1 (最低)"; break;
                        case "2": priorityDisplay = "2"; break;
                        case "3": priorityDisplay = "3"; break;
                        case "4": priorityDisplay = "4"; break;
                        case "5": priorityDisplay = "5 (默认)"; break;
                        case "6": priorityDisplay = "6"; break;
                        case "7": priorityDisplay = "7"; break;
                        case "8": priorityDisplay = "8"; break;
                        case "9": priorityDisplay = "9"; break;
                        case "10": priorityDisplay = "10 (最高)"; break;
                        default: priorityDisplay = "5 (默认)"; break;
                    }
                }
                
                addClickableItem(activity, group4, "线程优先级", priorityDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        final String[] priorities = {"1 (最低)", "2", "3", "4", "5 (默认)", "6", "7", "8", "9", "10 (最高)"};
                        final String[] priorityNums = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
                        showM3ChoiceDialog(activity, "线程优先级", priorities, priorityNums, "thread_pool_priority", new OnChoiceSelected() {
                            public void onSelect(String val) {
                                dialog.dismiss();
                                showStyleSettingsDialog(activity);
                            }
                        });
                    }
                });
                
                String queueCapacity = getSetting("settings", "thread_pool_queue_capacity", "50");
                addInputItem(activity, group4, "任务队列容量", queueCapacity, "默认50", textColor, cardColor, "thread_pool_queue_capacity", "");
                
                String keepAlive = getSetting("settings", "thread_pool_keep_alive", "30");
                addInputItem(activity, group4, "核心线程存活(秒)", keepAlive, "默认30", textColor, cardColor, "thread_pool_keep_alive", "");
                
                String rejectPolicy = getSetting("settings", "thread_pool_reject_policy", "0");
                String[] rejectPolicies = {"丢弃最旧任务 (默认)", "丢弃最新任务", "抛出异常", "调用者执行"};
                String[] rejectPolicyVals = {"0", "1", "2", "3"};
                String rejectPolicyDisplay = "丢弃最旧任务 (默认)";
                for(int i=0; i<rejectPolicyVals.length; i++) {
                    if(rejectPolicyVals[i].equals(rejectPolicy)) {
                        rejectPolicyDisplay = rejectPolicies[i];
                        break;
                    }
                }
                
                addClickableItem(activity, group4, "任务满载策略", rejectPolicyDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        showM3ChoiceDialog(activity, "任务满载策略", rejectPolicies, rejectPolicyVals, "thread_pool_reject_policy", new OnChoiceSelected() {
                            public void onSelect(String val) {
                                dialog.dismiss();
                                showStyleSettingsDialog(activity);
                            }
                        });
                    }
                });
                
                list.addView(group4);
                
                // ==================== 悬浮窗设置 ====================
                addSectionHeader(activity, list, "悬浮窗设置", subTextColor);
                LinearLayout group5 = createCardGroup(activity, cardColor, 12);
                
                String icpath = getSetting("settings", "iconPath", "");
                boolean isAnim = icpath.toLowerCase().endsWith(".gif") || icpath.toLowerCase().endsWith(".GIF");
                String iconTypeDisplay = isAnim ? "动态图标 (GIF/WebP)" : "静态图标 (PNG/JPG)";
                if (icpath.isEmpty()) iconTypeDisplay = "点击选择图标 (未设置)";
                
                addClickableItem(activity, group5, "更换图标", iconTypeDisplay, textColor, cardColor, false, new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*"); 
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            activity.startActivityForResult(intent, 1005);
                            Toast("选择后请重新打开设置刷新");
                        } catch(Exception e) { 
                            Toast("文件选择启动失败: " + e); 
                        }
                    }
                });
                
                String floatSize = getSetting("settings", "悬浮窗大小", "48");
                addInputItem(activity, group5, "悬浮窗大小", floatSize, "默认48", textColor, cardColor, "悬浮窗大小", "");
                
                String closeIconSize = getSetting("settings", "关闭区域图标大小", "24");
                addInputItem(activity, group5, "关闭图标大小", closeIconSize, "默认24", textColor, cardColor, "关闭区域图标大小", "");
                
                String dragSens = getSetting("settings", "拖拽灵敏度", "12");
                addInputItem(activity, group5, "拖拽灵敏度", dragSens, "数值越小越灵敏", textColor, cardColor, "拖拽灵敏度", "");
                
                String longPress = getSetting("settings", "长按关闭阈值", "650");
                addInputItem(activity, group5, "长按关闭阈值", longPress, "默认650", textColor, cardColor, "长按关闭阈值", "");
                
                String iconAlpha = getSetting("settings", "iconAlpha", "255");
                addInputItem(activity, group5, "图标透明度", iconAlpha, "0-255", textColor, cardColor, "iconAlpha", "");
                
                if (isAnim) {
                    float refreshRate = 60f;
                    try {
                        Display display = activity.getWindowManager().getDefaultDisplay();
                        refreshRate = display.getRefreshRate();
                    } catch (Exception e) {}
                    
                    java.util.ArrayList<String> fpsPresetList = new java.util.ArrayList<>();
                    java.util.ArrayList<String> fpsDelayList = new java.util.ArrayList<>();
                    
                    fpsPresetList.add("30 FPS (默认)");
                    fpsDelayList.add("33");
                    
                    if (refreshRate >= 59.9f) {
                        fpsPresetList.add("60 FPS");
                        fpsDelayList.add("17");
                    }
                    
                    if (refreshRate >= 89.9f) {
                        fpsPresetList.add("90 FPS");
                        fpsDelayList.add("11");
                    }
                    
                    if (refreshRate >= 119.9f) {
                        fpsPresetList.add("120 FPS");
                        fpsDelayList.add("8");
                    }
                    
                    if (refreshRate >= 143.9f) {
                        fpsPresetList.add("144 FPS");
                        fpsDelayList.add("7");
                    }
                    
                    final String[] fpsPresets = fpsPresetList.toArray(new String[0]);
                    final String[] fpsDelayValues = fpsDelayList.toArray(new String[0]);
                    
                    String currentDelay = getSetting("settings", "gifDelay", "100");
                    int currentFps = 10;
                    try {
                        int delay = Integer.parseInt(currentDelay);
                        currentFps = delay > 0 ? 1000 / delay : 10;
                    } catch(Exception e) {}
                    
                    String fpsDisplay = "点击选择帧率";
                    for (int i = 0; i < fpsPresets.length; i++) {
                        int presetDelay = Integer.parseInt(fpsDelayValues[i]);
                        int presetFps = 1000 / presetDelay;
                        if (Math.abs(currentFps - presetFps) <= 2) {
                            fpsDisplay = fpsPresets[i];
                            break;
                        }
                    }
                    if (currentDelay.equals("100") || currentDelay.equals("33")) fpsDisplay = "30 FPS (默认)";
                    
                    addClickableItem(activity, group5, "帧率设置", fpsDisplay, textColor, cardColor, false, new View.OnClickListener() {
                        public void onClick(View v) {
                            showM3ChoiceDialog(activity, "选择帧率", fpsPresets, fpsDelayValues, "gifDelay", new OnChoiceSelected() {
                                public void onSelect(String val) {
                                    dialog.dismiss();
                                    showStyleSettingsDialog(activity);
                                }
                            });
                        }
                    });
                    
                    LinearLayout animItem = new LinearLayout(activity);
                    animItem.setOrientation(LinearLayout.VERTICAL);
                    animItem.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
                    
                    GradientDrawable animBg = new GradientDrawable();
                    animBg.setColor(itemColor);
                    animBg.setCornerRadius(dp(activity, 8));
                    animItem.setBackgroundDrawable(animBg);
                    
                    TextView tAnim = new TextView(activity);
                    tAnim.setText("动画速度 (每帧延迟 ms)");
                    tAnim.setTextSize(14);
                    tAnim.setTextColor(textColor);
                    animItem.addView(tAnim);

                    final EditText inputAnim = new EditText(activity);
                    inputAnim.setText(currentDelay);
                    inputAnim.setTextSize(14);
                    inputAnim.setHintTextColor(Color.GRAY);
                    inputAnim.setBackgroundColor(Color.TRANSPARENT);
                    inputAnim.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
                    inputAnim.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

                    final TextView fpsText = new TextView(activity);
                    fpsText.setTextSize(12);
                    fpsText.setTextColor(textColor);
                    fpsText.setAlpha(0.7f);
                    
                    try {
                        int delay = Integer.parseInt(currentDelay);
                        int fps = delay > 0 ? 1000 / delay : 0;
                        fpsText.setText("当前帧率: " + fps + " FPS");
                    } catch(Exception e) { 
                        fpsText.setText("当前帧率: - FPS"); 
                    }

                    inputAnim.addTextChangedListener(new android.text.TextWatcher() {
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        public void afterTextChanged(android.text.Editable s) {
                            String val = s.toString().trim();
                            pendingSettingsChanges.put("gifDelay", val);
                            try {
                                int delay = Integer.parseInt(val);
                                if (delay > 0) {
                                    int fps = 1000 / delay;
                                    fpsText.setText("当前帧率: " + fps + " FPS");
                                } else { fpsText.setText("当前帧率: - FPS"); }
                            } catch(Exception e) { fpsText.setText("当前帧率: - FPS"); }
                        }
                    });

                    animItem.addView(inputAnim);
                    animItem.addView(fpsText);
                    group5.addView(animItem);
                    
                    View aspace = new View(activity);
                    aspace.setBackgroundColor(Color.TRANSPARENT);
                    group5.addView(aspace, new LinearLayout.LayoutParams(-1, dp(activity, 4)));
                }
                
                list.addView(group5);
                
                scroll.addView(list);
                root.addView(scroll);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(root);
                builder.setCancelable(true);
                
                dialog = builder.create();
                
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface d) {
                        isDialogShowing = false;
                    }
                });
                
                Window win = dialog.getWindow();
                if (win != null) {
                    win.requestFeature(Window.FEATURE_NO_TITLE);
                } else {
                    // ignore
                }
                
                dialog.show();
                isDialogShowing = true;
                
                applyUiTheme(activity, dialog);
                
                hookFilePicker(activity, 1005, pluginPath + "/API/icon{ext}");
                hookFilePicker(activity, 1007, pluginPath + "/API/background{ext}");
                
                if (win != null) {
                    applyDialogSize(activity, win);
                }
                
            } catch (Exception e) {
                Toast("设置打开失败: " + e.getMessage());
            }
        }
    });
}

void showPreviewPopup(final Activity activity) {
    final boolean isDark = isEffectiveDarkMode(activity);
    
    String bgType = getSetting("settings", "ui_bg_type", "color");
    String bgColor = getSetting("settings", isDark ? "ui_bg_color_dark" : "ui_bg_color_light", 
        isDark ? "#FF1E1E1E" : "#FFFFFFFF");
    String textColorUser = getSetting("settings", isDark ? "ui_text_color_dark" : "ui_text_color_light", "");
    
    int overlayAlpha = 100;
    try { overlayAlpha = Integer.parseInt(getSetting("settings", "ui_img_alpha", isDark ? "180" : "100")); } catch(Exception e){}
    
    boolean isBgDark = isDark;
    if ("color".equals(bgType) && isValidHexColor(bgColor)) {
        isBgDark = isColorDark(Color.parseColor(bgColor.trim()));
    } else if ("image".equals(bgType)) {
        isBgDark = currentIsDark;
    }
    
    int previewTextColor;
    if (isValidHexColor(textColorUser)) {
        previewTextColor = Color.parseColor(textColorUser);
    } else {
        previewTextColor = isBgDark ? Color.parseColor("#FFEFEFEF") : Color.parseColor("#FF333333");
    }
    
    final String fontType = getSetting("settings", "ui_font_type", "default");
    float fSize = 1.0f;
    try { fSize = Float.parseFloat(getSetting("settings", "ui_font_size", "1.0")); } catch(Exception e){}
    final float fontSizeScale = fSize;
    final Typeface tf = getCustomTypeface(fontType);
    
    LinearLayout previewContent = new LinearLayout(activity);
    previewContent.setLayoutParams(new ViewGroup.LayoutParams(250, 250));
    previewContent.setOrientation(LinearLayout.VERTICAL);
    previewContent.setGravity(Gravity.CENTER);
    previewContent.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));
    
    TextView previewTitle = new TextView(activity);
    previewTitle.setText("预览标题");
    previewTitle.setTextSize(18);
    previewTitle.setTypeface(null, Typeface.BOLD);
    previewTitle.setTextColor(previewTextColor);
    previewTitle.setGravity(Gravity.CENTER);
    previewContent.addView(previewTitle);
    
    TextView previewText = new TextView(activity);
    previewText.setText("这是一段测试文本，用于预览弹窗的显示效果。");
    previewText.setTextSize(12);
    previewText.setTextColor(previewTextColor);
    previewText.setGravity(Gravity.CENTER);
    previewText.setPadding(0, dp(activity, 8), 0, dp(activity, 16));
    previewContent.addView(previewText);
    
    Button toastButton = new Button(activity);
    toastButton.setText("Toast");
    toastButton.setTextColor(previewTextColor);
    toastButton.setBackground(null);
    toastButton.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            Toast("这是一个测试toast");
        }
    });
    previewContent.addView(toastButton);
    
    AlertDialog.Builder previewBuilder = new AlertDialog.Builder(activity, 
        isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    previewBuilder.setView(previewContent);
    previewBuilder.setPositiveButton("关闭", null);
    
    final AlertDialog previewDialog = previewBuilder.create();
    previewDialog.show();
    
    Window previewWindow = previewDialog.getWindow();
    if (previewWindow != null) {
        applyDialogSize(activity, previewWindow);
    }
    
    applyUiTheme(activity, previewDialog);
}

void showM3ChoiceDialog(final Activity activity, String title, final String[] names, final String[] values, final String saveKey, final OnChoiceSelected callback) {
    String currentVal = getSetting("settings", saveKey, values[0]);
    int checkedItem = 0;
    for (int i = 0; i < values.length; i++) {
        if (values[i].toString().equals(currentVal)) { checkedItem = i; break; }
    }
    
    AlertDialog.Builder b = new AlertDialog.Builder(activity, 
        isEffectiveDarkMode(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    b.setTitle(title);
    b.setSingleChoiceItems(names, checkedItem, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface d, int which) {
            try {
                String newVal = values[which].toString();
                pendingSettingsChanges.put(saveKey, newVal);
                try { putString("settings", saveKey, newVal); } catch (Exception e) { }
                if (callback != null) callback.onSelect(newVal);
            } catch (Throwable ex) {}
            d.dismiss();
        }
    });
    b.setNegativeButton("取消", null);
    
    AlertDialog d = b.create();
    d.show();
    applyUiTheme(activity, d);
}

void showScaleSliderDialog(final Activity activity, final OnChoiceSelected callback) {
    try {
        final boolean isDark = false; 
        
        String currentScaleStr = getString("settings", "ui_dialog_scale", "1.0");
        float currentScale = 1.0f;
        try {
            currentScale = Float.parseFloat(currentScaleStr);
        } catch (Exception e) {}
        
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 20));
        
        final TextView valueText = new TextView(activity);
        valueText.setText(String.format("%.2f", currentScale) + "x");
        valueText.setTextSize(28);
        valueText.setTypeface(null, Typeface.BOLD);
        valueText.setTextColor(Color.parseColor("#FF333333"));
        valueText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(-1, -2);
        valueParams.bottomMargin = dp(activity, 20);
        root.addView(valueText, valueParams);
        
        LinearLayout sliderRow = new LinearLayout(activity);
        sliderRow.setOrientation(LinearLayout.HORIZONTAL);
        sliderRow.setGravity(Gravity.CENTER_VERTICAL);
        
        final float MIN_SCALE = 0.25f;
        final float MAX_SCALE = 2.0f;
        final float SCALE_STEP = 0.01f;
        final int MAX_PROGRESS = (int) ((MAX_SCALE - MIN_SCALE) / SCALE_STEP);
        
        final TextView btnMinus = new TextView(activity);
        btnMinus.setText("−");
        btnMinus.setTextSize(28);
        btnMinus.setTypeface(null, Typeface.BOLD);
        btnMinus.setTextColor(Color.parseColor("#007AFF"));
        btnMinus.setPadding(dp(activity, 12), 0, dp(activity, 8), 0);
        sliderRow.addView(btnMinus);
        
        final SeekBar seekBar = new SeekBar(activity);
        int currentProgress = Math.round((currentScale - MIN_SCALE) / SCALE_STEP);
        currentProgress = Math.max(0, Math.min(MAX_PROGRESS, currentProgress));
        seekBar.setMax(MAX_PROGRESS);
        seekBar.setProgress(currentProgress);
        seekBar.setPadding(0, 0, 0, 0);
        
        int progressColor = Color.parseColor("#007AFF");
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(progressColor));
        
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        sliderRow.addView(seekBar, seekParams);
        
        final TextView btnPlus = new TextView(activity);
        btnPlus.setText("+");
        btnPlus.setTextSize(28);
        btnPlus.setTypeface(null, Typeface.BOLD);
        btnPlus.setTextColor(Color.parseColor("#007AFF"));
        btnPlus.setPadding(dp(activity, 8), 0, dp(activity, 12), 0);
        sliderRow.addView(btnPlus);
        
        LinearLayout.LayoutParams sliderRowParams = new LinearLayout.LayoutParams(-1, -2);
        sliderRowParams.bottomMargin = dp(activity, 12);
        root.addView(sliderRow, sliderRowParams);
        
        LinearLayout presetRow = new LinearLayout(activity);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setGravity(Gravity.CENTER);
        
        final String[] presets = {"0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0"};
        final List<TextView> presetButtons = new ArrayList<>();
        
        for (int i = 0; i < presets.length; i++) {
            final String value = presets[i];
            TextView btn = new TextView(activity);
            btn.setText(value + "x");
            btn.setTextSize(12);
            btn.setTextColor(Color.parseColor("#007AFF"));
            btn.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
            
            presetButtons.add(btn);
            
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    float val = Float.parseFloat(value);
                    int progress = Math.round((val - MIN_SCALE) / SCALE_STEP);
                    progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
                    seekBar.setProgress(progress);
                    valueText.setText(value + "x");
                    vibrate(activity, 32);
                    updatePresetHighlight(currentScale);
                }
            });
            
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-2, -2);
            if (i < presets.length - 1) {
                btnParams.rightMargin = dp(activity, 6);
            }
            presetRow.addView(btn, btnParams);
        }
        
        LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(-1, -2);
        presetParams.bottomMargin = dp(activity, 16);
        root.addView(presetRow, presetParams);
        
        LinearLayout inputRow = new LinearLayout(activity);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView inputLabel = new TextView(activity);
        inputLabel.setText("自定义:");
        inputLabel.setTextSize(14);
        inputLabel.setTextColor(Color.parseColor("#99000000"));
        inputRow.addView(inputLabel);
        
        final EditText input = new EditText(activity);
        input.setText(String.format("%.2f", currentScale));
        input.setTextSize(14);
        input.setTextColor(Color.parseColor("#FF333333"));
        input.setHintTextColor(Color.parseColor("#66000000"));
        input.setHint("0.25~2.0");
        input.setBackground(null);
        input.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
        inputRow.addView(input, inputParams);
        
        TextView unitLabel = new TextView(activity);
        unitLabel.setText("x");
        unitLabel.setTextSize(14);
        unitLabel.setTextColor(Color.parseColor("#99000000"));
        inputRow.addView(unitLabel);
        
        LinearLayout.LayoutParams inputRowParams = new LinearLayout.LayoutParams(-1, -2);
        inputRowParams.bottomMargin = dp(activity, 16);
        root.addView(inputRow, inputRowParams);
        
        final Runnable updateButtonStates = new Runnable() {
            public void run() {
                int progress = seekBar.getProgress();
                btnMinus.setAlpha(progress > 0 ? 1.0f : 0.3f);
                btnPlus.setAlpha(progress < MAX_PROGRESS ? 1.0f : 0.3f);
            }
        };
        
        final Runnable updatePresetHighlight = new Runnable() {
            public void run() {
                float currentScale = MIN_SCALE + (seekBar.getProgress() * SCALE_STEP);
                for (int i = 0; i < presetButtons.size(); i++) {
                    TextView btn = presetButtons.get(i);
                    float presetValue = Float.parseFloat(presets[i]);
                    if (Math.abs(currentScale - presetValue) < 0.01f) {
                        btn.setTextColor(Color.parseColor("#0047AB"));
                        btn.setTypeface(null, Typeface.BOLD);
                    } else {
                        btn.setTextColor(Color.parseColor("#007AFF"));
                        btn.setTypeface(null, Typeface.NORMAL);
                    }
                }
            }
        };
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
                
                float scale = MIN_SCALE + (progress * SCALE_STEP);
                String scaleStr = String.format("%.2f", scale);
                valueText.setText(scaleStr + "x");
                
                if (!input.hasFocus()) {
                    input.setText(scaleStr);
                }
                
                updateButtonStates.run();
                updatePresetHighlight.run();
                
                if (fromUser) {
                    vibrate(activity, 48);
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                String val = s.toString().trim();
                if (!val.isEmpty()) {
                    try {
                        float f = Float.parseFloat(val);
                        if (f >= MIN_SCALE && f <= MAX_SCALE) {
                            int progress = Math.round((f - MIN_SCALE) / SCALE_STEP);
                            progress = Math.max(0, Math.min(MAX_PROGRESS, progress));
                            seekBar.setProgress(progress);
                            valueText.setText(String.format("%.2f", f) + "x");
                            updateButtonStates.run();
                            updatePresetHighlight.run();
                        }
                    } catch (Exception e) {}
                }
            }
        });
        
        btnMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int progress = seekBar.getProgress();
                if (progress > 0) {
                    seekBar.setProgress(progress - 1);
                    vibrate(activity, 48);
                    updateButtonStates.run();
                    updatePresetHighlight.run();
                }
            }
        });
        
        btnPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int progress = seekBar.getProgress();
                if (progress < MAX_PROGRESS) {
                    seekBar.setProgress(progress + 1);
                    vibrate(activity, 48);
                    updateButtonStates.run();
                    updatePresetHighlight.run();
                }
            }
        });
        
        updateButtonStates.run();
        updatePresetHighlight.run();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setTitle("弹窗缩放");
        builder.setView(root);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                String finalValue = input.getText().toString().trim();
                
                if (!finalValue.isEmpty()) {
                    try {
                        float f = Float.parseFloat(finalValue);
                        if (f >= MIN_SCALE && f <= MAX_SCALE) {
                            pendingSettingsChanges.put("ui_dialog_scale", String.format("%.2f", f));
                        }
                    } catch (Exception e) {
                    }
                }
                
                if (callback != null) {
                    final String valueToPass = finalValue.isEmpty() ? "1.00" : finalValue;
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                callback.onSelect(valueToPass);
                            } catch (Throwable t) {
                            }
                        }
                    });
                }
                
                d.dismiss();
            }
        });
        
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int which) {
                pendingSettingsChanges.remove("ui_dialog_scale");
                
                if (callback != null) {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                callback.onSelect(null);
                            } catch (Throwable t) {
                            }
                        }
                    });
                }
                
                d.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
        
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dialogWidth;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        
        updateButtonStates.run();
        updatePresetHighlight.run();
        
    } catch (Exception e) {}
}
