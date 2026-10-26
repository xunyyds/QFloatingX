String convertRole(String role) {
    if ("OWNER".equals(role)) return "群主";
    if ("ADMIN".equals(role)) return "管理员";
    if ("MEMBER".equals(role)) return "普通成员";
    return role != null ? role : "未知";
}

String timestampToDate(long timestamp) {
    try {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    } catch (Throwable e) {
        return "时间错误";
    }
}

String getGagStatus(String timestamp) {
    return getGagStatus(timestamp, System.currentTimeMillis() / 1000);
}

String getGagStatus(String timestamp, long currentTime) {
    try {
        long ts = Long.parseLong(timestamp);
        if (ts == 0) return "未禁言";
        if (ts > 4102358400L) return "禁言中：永久";
        if (ts > currentTime) return "禁言中：剩余" + formatRemainingTime(ts - currentTime);
        return "未禁言";
    } catch (Throwable e) {
        return "未知";
    }
}
void 菜单(Object data) {
    if (!非UI初始化完成) {
        traceLog("dialog_log", "[菜单] 菜单调用时延迟启动未完成");
        return;
    }
    Activity activity = safeActivity();
    if (activity == null) {
        traceLog("dialog_log", "[菜单] 菜单调用时无法获取Activity");
        return;
    }
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                if (!activity.isFinishing()) 长按消息菜单(activity, data);
            } catch (Throwable t) {
                traceLog("dialog_log", "[菜单] 异常: " + t);
            }
        }
    });
}
FrameLayout createMenuItem(Activity activity,
    String title,
    boolean isDark,
    int colorPrimary,
    int colorContainer,
    Dialog parentDialog,
    Runnable callback,
    boolean skipDismiss) {
    FrameLayout itemWrapper = new FrameLayout(activity);
    itemWrapper.setLayoutParams(new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f),
        GridLayout.spec(GridLayout.UNDEFINED, 1f)));
    itemWrapper.setPadding(dp(activity, 7), dp(activity, 6), dp(activity, 7), dp(activity, 6));
    itemWrapper.setClipToPadding(false);
    itemWrapper.setClipChildren(false);
    itemWrapper.setElevation(dp(activity, 3.5f));

    int backgroundColor = pc(getSettingsThemeColor(activity, "background"));
    if (Color.alpha(backgroundColor) < 255) {
        backgroundColor = getAdaptiveMenuItemBg(activity);
    }
    final int normalColor = isDark
        ? blendColor(backgroundColor, Color.WHITE, 0.065f)
        : blendColor(backgroundColor, Color.BLACK, 0.038f);
    final int pressedColor = isDark
        ? blendColor(normalColor, Color.WHITE, 0.055f)
        : blendColor(normalColor, Color.BLACK, 0.065f);
    final GradientDrawable itemBg = new GradientDrawable();
    itemBg.setShape(GradientDrawable.RECTANGLE);
    itemBg.setCornerRadius(dp(activity, getUiCornerDp()));
    itemBg.setColor(normalColor);

    LinearLayout itemContent = new LinearLayout(activity);
    itemContent.setOrientation(LinearLayout.HORIZONTAL);
    itemContent.setGravity(Gravity.CENTER);
    itemContent.setMinimumHeight(dp(activity, 54));
    itemContent.setPadding(dp(activity, 12), dp(activity, 5), dp(activity, 12), dp(activity, 5));
    itemContent.setBackground(itemBg);
    itemContent.setElevation(dp(activity, 4.5f));

    final TextView titleView = new TextView(activity);
    titleView.setText(title);
    titleView.setTextSize(14);
    titleView.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    titleView.setGravity(Gravity.CENTER);
    titleView.setMaxLines(2);
    titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
    titleView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
    itemContent.addView(titleView);

    itemWrapper.addView(itemContent);
    final boolean[] itemPressed = new boolean[] { false };
    itemWrapper.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    itemPressed[0] = true;
                    itemBg.setColor(pressedColor);
                    itemContent.setBackground(itemBg);
                    itemContent.animate().scaleX(0.992f).scaleY(0.992f).setDuration(70).start();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (itemPressed[0] && (event.getX() < 0 || event.getY() < 0 || event.getX() > itemWrapper.getWidth() || event.getY() > itemWrapper.getHeight())) {
                        itemPressed[0] = false;
                        itemBg.setColor(normalColor);
                        itemContent.setBackground(itemBg);
                        itemContent.animate().scaleX(1f).scaleY(1f).setDuration(70).start();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    boolean click = itemPressed[0];
                    itemPressed[0] = false;
                    itemBg.setColor(normalColor);
                    itemContent.setBackground(itemBg);
                    itemContent.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    if (click) v.performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    itemPressed[0] = false;
                    itemBg.setColor(normalColor);
                    itemContent.setBackground(itemBg);
                    itemContent.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
                    return true;
            }
            return false;
        }
    });
    itemWrapper.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (!skipDismiss && parentDialog != null && parentDialog.isShowing()) parentDialog.dismiss();
            if (callback != null) callback.run();
        }
    });
    return itemWrapper;
}
TextView createCategoryTitle(Activity activity, String category, int colorPrimary) {
    TextView tv = new TextView(activity);
    tv.setText(category);
    tv.setTextSize(14);
    tv.setTextColor(colorPrimary);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setPadding(dp(activity, 8), dp(activity, 12), dp(activity, 8), dp(activity, 8));
    return tv;
}
String[][]CATEGORY_COLOR_STRS = new String[6][3];
CATEGORY_COLOR_STRS[0][0] = "#26384A";
CATEGORY_COLOR_STRS[0][1] = "#D7E4F3";
CATEGORY_COLOR_STRS[0][2] = "#A7B5C7";
CATEGORY_COLOR_STRS[1][0] = "#304634";
CATEGORY_COLOR_STRS[1][1] = "#D9E9DC";
CATEGORY_COLOR_STRS[1][2] = "#A9B9AB";
CATEGORY_COLOR_STRS[2][0] = "#48365E";
CATEGORY_COLOR_STRS[2][1] = "#E5D9F2";
CATEGORY_COLOR_STRS[2][2] = "#B4A9C0";
CATEGORY_COLOR_STRS[3][0] = "#563E24";
CATEGORY_COLOR_STRS[3][1] = "#F0DFCA";
CATEGORY_COLOR_STRS[3][2] = "#C4B69E";
CATEGORY_COLOR_STRS[4][0] = "#2D5558";
CATEGORY_COLOR_STRS[4][1] = "#D8ECEE";
CATEGORY_COLOR_STRS[4][2] = "#A6B9BB";
CATEGORY_COLOR_STRS[5][0] = "#30353C";
CATEGORY_COLOR_STRS[5][1] = "#E0E3E8";
CATEGORY_COLOR_STRS[5][2] = "#A6A9AF";

void addMenuItem(List list, String category, String title, Runnable callback) {
    list.add(new Object[] { category, title, callback });
}
int[]getCategoryColorInts(String category, boolean isDark) {
    int index = 5;
    if ("消息操作".equals(category)) index = 0;
    else if ("群管理".equals(category) || "好友信息".equals(category)) index = 1;
    else if ("互动功能".equals(category)) index = 2;
    else if ("工具".equals(category)) index = 3;
    else if ("其他".equals(category)) index = 4;
    else if ("设置".equals(category)) index = 5;
    String[]colorStrs = CATEGORY_COLOR_STRS[index];
    int titleColor = pc(isDark ? colorStrs[1] : colorStrs[0]);
    return new int[] {
        titleColor, titleColor
    };
}
class ArrowDrawable extends Drawable {
    private Paint paint;
    private Path path;
    private boolean isUp;
    ArrowDrawable(int color, boolean isUp) {
        this.isUp = isUp;
        this.paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
        this.path = new Path();
    }
    void setColor(int color) {
        paint.setColor(color);
        invalidateSelf();
    }
    private void rebuildPath() {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        float sw = Math.min(w, h) / 6f;
        paint.setStrokeWidth(sw);
        float cx = w / 2f;
        path.reset();
        if (isUp) {
            path.moveTo(sw, h * 0.65f);
            path.lineTo(cx, h * 0.35f);
            path.lineTo(w - sw, h * 0.65f);
        } else {
            path.moveTo(sw, h * 0.35f);
            path.lineTo(cx, h * 0.65f);
            path.lineTo(w - sw, h * 0.35f);
        }
    }
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        rebuildPath();
    }
    public void draw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
void applySavedSortOrder(List catOrder, Map catItems, String savedOrder) {
    if (savedOrder == null || savedOrder.isEmpty()) return;
    try {
        List newCatOrder = new ArrayList();
        Map newCatItems = new LinkedHashMap();
        String[]cats = savedOrder.split("\\|");
        for (int i = 0; i < cats.length; i++) {
            String cStr = cats[i];
            int eqIdx = cStr.indexOf("=");
            String catName = eqIdx >= 0 ? cStr.substring(0, eqIdx) : cStr;
            if (!catOrder.contains(catName)) continue;
            newCatOrder.add(catName);
            List origItems = (List) catItems.get(catName);
            if (origItems == null) continue;
            if (eqIdx >= 0 && cStr.length() > eqIdx + 1) {
                String itemsPart = cStr.substring(eqIdx + 1);
                String[]itemNames = itemsPart.split(",");
                List sortedItems = new ArrayList();
                for (int j = 0; j < itemNames.length; j++) {
                    String targetName = itemNames[j];
                    for (int k = 0; k < origItems.size(); k++) {
                        Object[]item = (Object[]) origItems.get(k);
                        if (targetName.equals(item[1])) {
                            sortedItems.add(item);
                            break;
                        }
                    }
                }
                for (int k = 0; k < origItems.size(); k++) {
                    Object[]item = (Object[]) origItems.get(k);
                    boolean found = false;
                    for (int m = 0; m < sortedItems.size(); m++) {
                        Object[]si = (Object[]) sortedItems.get(m);
                        if (si[1].equals(item[1])) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) sortedItems.add(item);
                }
                newCatItems.put(catName, sortedItems);
            } else {
                newCatItems.put(catName, new ArrayList(origItems));
            }
        }
        for (int i = 0; i < catOrder.size(); i++) {
            String catName = (String) catOrder.get(i);
            if (!newCatOrder.contains(catName)) {
                newCatOrder.add(catName);
                newCatItems.put(catName, catItems.get(catName));
            }
        }
        catOrder.clear();
        catOrder.addAll(newCatOrder);
        catItems.clear();
        catItems.putAll(newCatItems);
    } catch (Throwable t) {
        traceLog("dialog_log", "[applySavedSortOrder]排序应用失败: " + t.getMessage());
    }
}

String buildSortOrderString(List catOrder, Map catItems) {
    try {
        return((List) catOrder).stream().map(new java.util.function.Function() {
            public Object apply(Object cat) {
                List items = (List) catItems.get(cat);
                String itemNames = ((List) items).stream().map(new java.util.function.Function() {
                    public Object apply(Object item) {
                        return(String)((Object[]) item)[1];
                    }
                }).collect(Collectors.joining(","));
                return(String) cat + "=" + itemNames;
            }
        }).collect(Collectors.joining("|"));
    } catch (Throwable t) {
        return "";
    }
}
public void 长按消息菜单(Activity activity, Object data) {
    if (activity == null || activity.isFinishing()) return;
    boolean isDark = isThemeDark(activity);
    String quntext = "";
    String peerUin = "";
    String userUin = "";
    int msgtype = 0;
    long msgid = 0;
    int chatType = 0;
    Object msgRecord = null;
    Object atList = null;
    String nickName = "";
    try {
        quntext = String.valueOf(data.msg);
        peerUin = String.valueOf(data.peerUin);
        userUin = String.valueOf(data.userUin);
        msgtype = (int) data.msgType;
        msgid = (long) data.msgId;
        chatType = (int) data.type;
        msgRecord = data.data;
        atList = data.atList;
        nickName = String.valueOf(msgRecord.sendNickName);
    } catch (Throwable t) {
        traceLog("dialog_log", "[长按消息菜单] 异常: " + t);
    }
    if (nickName == null) {
        nickName = userUin;
    }
    int colorText = tc(activity, "on_surface");
    int colorSubtext = tc(activity, "on_surface_variant");
    int sheetBg = getAdaptiveSheetBg(activity);
    final Dialog bottomSheet = new Dialog(activity);
    bottomSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout rootLayout = new LinearLayout(activity);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    rootLayout.setBackgroundColor(sheetBg);
    FrameLayout headerCard = createMemberInfoCard(activity,
        userUin,
        peerUin,
        chatType,
        isDark,
        colorText,
        colorSubtext,
        quntext,
        quntext,
        msgRecord,
        nickName);
    rootLayout.addView(headerCard);
    ScrollView scrollView = new ScrollView(activity);
    scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 380)));
    scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    scrollView.setVerticalScrollBarEnabled(false);
    scrollView.setClipChildren(true);
    scrollView.setClipToPadding(true);
    final LinearLayout menuContainer = new LinearLayout(activity);
    menuContainer.setOrientation(LinearLayout.VERTICAL);
    menuContainer.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 20));
    menuContainer.setClipChildren(false);
    List menuItems = new ArrayList();
    final String finalQuntext = quntext;
    final int finalChatType = chatType;
    final String finalPeerUin = peerUin;
    final long finalMsgid = msgid;
    final Object finalMsgRecord = msgRecord;
    final String finalUserUin = userUin;
    final Object finalAtList = atList;
    final int finalMsgtype = msgtype;
    final String finalNickName = nickName;
    String roleTmp = "MEMBER";
    try {
        Object qqmember = getMemberInfo(finalPeerUin, qq);
        if (qqmember != null && qqmember.role != null) roleTmp = qqmember.role;
    } catch (Throwable t) {
        traceLog("dialog_log", "[长按菜单] 获取成员角色失败: " + t);
    }
    final String role = roleTmp;
    addMenuItem(menuItems, "消息操作", "复制内容", new Runnable() {
        public void run() {
            showCopyConfirmDialog(activity, "消息内容", finalQuntext, isDark);
        }
    });
    addMenuItem(menuItems, "消息操作", "复读加一", new Runnable() { public void run() { 复读(data, null); } });
    addMenuItem(menuItems, "消息操作", "撤回消息", new Runnable() {
        public void run() {
            recallMsg(finalChatType, finalPeerUin, finalMsgid);
            qqToast(2, "撤回操作已执行");
        }
    });
    addMenuItem(menuItems, "消息操作", "原始消息", new Runnable() { public void run() { showRawMessageDialog(finalMsgRecord); } });
    addMenuItem(menuItems, "消息操作", "解析消息", new Runnable() {
        public void run() {
            showMsgDataPaeseDialog(activity, data);
        }
    });
    addMenuItem(menuItems, "消息操作", "提取音频", new Runnable() {
        public void run() {
            showExtractAudioDialog(activity, data);
        }
    });
    if (chatType == 2 && ("OWNER".equals(role) || "ADMIN".equals(role))) {
        addMenuItem(menuItems, "消息操作", "设为精华", new Runnable() { public void run() { setMsgEssence(data, true); } });
        addMenuItem(menuItems, "消息操作", "取消精华", new Runnable() { public void run() { setMsgEssence(data, false); } });
    }
    if (chatType == 2) {
        addMenuItem(menuItems, "群管理", "成员信息", new Runnable() {
            public void run() {
                showMemberInfoDialog(activity, finalPeerUin, finalUserUin, finalChatType);
            }
        });
        addMenuItem(menuItems, "群管理", "群详细信息", new Runnable() {
            public void run() {
                showGroupInfoDialog(activity, finalPeerUin, isDark);
            }
        });
        addMenuItem(menuItems, "群管理", "全体禁言", new Runnable() {
            public void run() {
                showMuteAllDialog(activity, finalPeerUin);
            }
        });
        addMenuItem(menuItems, "群管理", "禁言此人", new Runnable() {
            public void run() {
                showShutUpDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "踢出此人", new Runnable() {
            public void run() {
                showKickConfirmDialog(activity, finalPeerUin, finalUserUin, finalNickName, isDark);
            }
        });
        addMenuItem(menuItems, "群管理", "设置头衔", new Runnable() {
            public void run() {
                showTitleDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "设置管理员", new Runnable() {
            public void run() {
                showSetAdminDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "修改群名片", new Runnable() {
            public void run() {
                showChangeCardDialog(activity, finalPeerUin, finalUserUin, finalNickName);
            }
        });
        addMenuItem(menuItems, "群管理", "禁言列表", new Runnable() {
            public void run() {
                showProhibitListDialog(activity, finalPeerUin, isDark);
            }
        });
        addMenuItem(menuItems, "群管理", "群成员列表", new Runnable() {
            public void run() {
                showGroupMemberListDialog(activity, finalPeerUin, isDark);
            }
        });
    } else {
        addMenuItem(menuItems, "好友信息", "查看资料", new Runnable() {
            public void run() {
                showMemberInfoDialog(activity, finalPeerUin, finalUserUin, finalChatType);
            }
        });
    }
    addMenuItem(menuItems, "互动功能", "为Ta点赞", new Runnable() {
        public void run() {
            showZanDialog(activity, finalUserUin);
        }
    });
    if (chatType == 2) {
        addMenuItem(menuItems, "互动功能", "回应表情", new Runnable() { public void run() { showFaceReplyConfigDialog(data); } });
        addMenuItem(menuItems, "互动功能", "AI声聊", new Runnable() { public void run() { showVoiceSendDialog(data); } });
        addMenuItem(menuItems, "互动功能", "艾特全体", new Runnable() {
            public void run() {
                sendMsg(finalPeerUin, "[atUin=0]", finalChatType);
            }
        });
        addMenuItem(menuItems, "互动功能", "艾特列表", new Runnable() { public void run() { showAtListDialog(finalAtList); } });
    }
    addMenuItem(menuItems, "互动功能", "拍一拍", new Runnable() {
        public void run() {
            showPaiDialog(activity, finalUserUin, finalPeerUin, finalChatType);
        }
    });
    addMenuItem(menuItems, "工具", "加解密工具", new Runnable() {
        public void run() {
            showEncryptDecryptDialog(activity, data);
        }
    });
    addMenuItem(menuItems, "工具", "执行代码", new Runnable() { public void run() { showCodeConsoleDialog(activity, data); } });
    addMenuItem(menuItems, "工具", "发送pb", new Runnable() { public void run() { showPBSenderDialog(); } });
    addMenuItem(menuItems, "工具", "获取Cookie", new Runnable() {
        public void run() {
            showGetCookieDialog(activity, isDark);
        }
    });
    addMenuItem(menuItems, "工具", "群列表", new Runnable() { public void run() { showGroupListDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "好友列表", new Runnable() { public void run() { showFriendListDialog(activity, isDark); } });
    addMenuItem(menuItems, "工具", "发送日志", new Runnable() {
        public void run() {
            Toast("正在打包日志...");
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        File logDir = new File(pluginPath + "/Log");
                        String zipPath = null;
                        if (logDir.exists() && logDir.isDirectory()) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss");
                            zipPath = pluginPath + "/" + sdf.format(new java.util.Date()) + "-Log.zip";
                            String envInfo = "";
                            try {
                                envInfo = String.valueOf(me.yxp.qfun.utils.log.LogUtils.INSTANCE.getEnvironmentInfo());
                            } catch (Throwable e2) {
                                envInfo = "获取环境信息异常: " + e2;
                            }
                            if (!zipDirectory(logDir.getAbsolutePath(), zipPath, "log", "info.txt", envInfo, null)) {
                                zipPath = null;
                            }
                        }
                        if (zipPath == null) {
                            Toast("日志打包失败");
                            return;
                        }
                        Toast("日志已打包，正在发送...");
                        sendFile(finalPeerUin, zipPath, finalChatType);
                        Toast("日志已发送");
                        final String delPath = zipPath;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    new File(delPath).delete();
                                } catch (Throwable e3) {
                                    traceLog("dialog_log", "[长按消息菜单] 删除日志zip异常: " + e3);
                                }
                            }
                        }, 180000);
                    } catch (Throwable e) {
                        traceLog("dialog_log", "[长按消息菜单] 发送日志异常: " + e);
                        Toast("发送日志异常: " + e.getMessage());
                    }
                }
            });
        }
    });
    addMenuItem(menuItems, "其他", "克隆头像", new Runnable() { public void run() { handleCloneAvatar(finalUserUin); } });
    addMenuItem(menuItems, "其他", "上传头像", new Runnable() { public void run() { handleUploadAvatar(finalQuntext); } });
    if (chatType == 2) {
        addMenuItem(menuItems, "其他", "群打卡", new Runnable() {
            public void run() {
                boolean ok = CheckSign(finalPeerUin, myUin);
                qqToast(ok ? 2 : 1, ok ? "打卡成功" : "打卡失败");
            }
        });
        addMenuItem(menuItems, "其他", "群字符", new Runnable() { public void run() { drawLuckyChar(finalPeerUin); } });
    }
    addMenuItem(menuItems, "其他", "设置", new Runnable() {
        public void run() {
            launchSettingsActivity(activity, finalChatType, finalPeerUin, (finalQuntext != null) ? finalQuntext : "");
        }
    });
    addMenuItem(menuItems, "其他", "双击消息菜单", new Runnable() {
        public void run() {
            fetchRealMsgRecord(finalMsgid, finalChatType, finalPeerUin, new MsgLoadedCallback() {
                public void onLoaded(MsgData msgData) {
                    Activity target = activity;
                    if (target == null || target.isFinishing()) target = getNowActivity();
                    showActionDialog(target, msgData, null, null);
                }
            });
        }
    });
    final boolean[]isEditMode = new boolean[] {
        false
    };
    String savedOrder = getString("setting", "menuSort", "");
    final List catOrder = new ArrayList();
    final Map catItems = new LinkedHashMap();
    for (int i = 0; i < menuItems.size(); i++) {
        Object[]item = (Object[]) menuItems.get(i);
        String cat = (String) item[0];
        if (!catOrder.contains(cat)) catOrder.add(cat);
        if (!catItems.containsKey(cat)) catItems.put(cat, new ArrayList());
        ((List) catItems.get(cat)).add(item);
    }
    applySavedSortOrder(catOrder, catItems, savedOrder);
    Runnable saveOrder = new Runnable() {
        public void run() {
            try {
                String orderStr = buildSortOrderString(catOrder, catItems);
                if (!orderStr.isEmpty()) {
                    putString("setting", "menuSort", orderStr);
                }
            } catch (Throwable t) {
                traceLog("dialog_log", "[onLoaded] 异常: " + t);
            }
        }
    };
    final Runnable[]renderMenuWrapper = new Runnable[1];
    renderMenuWrapper[0] = new Runnable() {
        public void run() {
            menuContainer.removeAllViews();
            View.OnDragListener dragListener = new View.OnDragListener() {
                public boolean onDrag(View v, DragEvent event) {
                    if (event.getAction() == DragEvent.ACTION_DROP) {
                        View source = (View) event.getLocalState();
                        if (source == v) return true;
                        Object[]srcTag = (Object[]) source.getTag();
                        Object[]tgtTag = (Object[]) v.getTag();
                        if (srcTag == null || tgtTag == null) return false;
                        String srcType = (String) srcTag[0];
                        String tgtType = (String) tgtTag[0];
                        if (srcType.equals("cat") && tgtType.equals("cat")) {
                            String srcCat = (String) srcTag[1];
                            String tgtCat = (String) tgtTag[1];
                            int sIdx = catOrder.indexOf(srcCat);
                            int tIdx = catOrder.indexOf(tgtCat);
                            if (sIdx >= 0 && tIdx >= 0) {
                                catOrder.remove(sIdx);
                                catOrder.add(tIdx, srcCat);
                                if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                            }
                        } else if (srcType.equals("item") && tgtType.equals("item")) {
                            String srcCat = (String) srcTag[1];
                            String srcItem = (String) srcTag[2];
                            String tgtCat = (String) tgtTag[1];
                            String tgtItem = (String) tgtTag[2];
                            if (srcCat.equals(tgtCat)) {
                                List iList = (List) catItems.get(srcCat);
                                int sIdx = - 1, tIdx = - 1;
                                for (int i = 0; i < iList.size(); i++) {
                                    Object[]itm = (Object[]) iList.get(i);
                                    if (itm[1].equals(srcItem)) sIdx = i;
                                    if (itm[1].equals(tgtItem)) tIdx = i;
                                }
                                if (sIdx >= 0 && tIdx >= 0) {
                                    Object obj = iList.remove(sIdx);
                                    iList.add(tIdx, obj);
                                    if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                                }
                            } else {
                                List srcList = (List) catItems.get(srcCat);
                                List tgtList = (List) catItems.get(tgtCat);
                                if (srcList != null && tgtList != null) {
                                    int sIdx = - 1;
                                    Object[]movedItem = null;
                                    for (int i = 0; i < srcList.size(); i++) {
                                        Object[]itm = (Object[]) srcList.get(i);
                                        if (itm[1].equals(srcItem)) {
                                            sIdx = i;
                                            movedItem = itm;
                                            break;
                                        }
                                    }
                                    if (movedItem != null) {
                                        movedItem[0] = tgtCat;
                                        srcList.remove(sIdx);
                                        int tIdx = - 1;
                                        for (int i = 0; i < tgtList.size(); i++) {
                                            Object[]itm = (Object[]) tgtList.get(i);
                                            if (itm[1].equals(tgtItem)) {
                                                tIdx = i;
                                                break;
                                            }
                                        }
                                        if (tIdx >= 0) {
                                            tgtList.add(tIdx, movedItem);
                                        } else {
                                            tgtList.add(movedItem);
                                        }
                                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            };
            for (int i = 0; i < catOrder.size(); i++) {
                String cat = (String) catOrder.get(i);
                int[]colorInts = getCategoryColorInts(cat, isDark);
                LinearLayout catLayout = new LinearLayout(activity);
                catLayout.setOrientation(LinearLayout.VERTICAL);
                catLayout.setClipChildren(false);
                catLayout.setClipToPadding(false);
                catLayout.setTag(new Object[] { "cat", cat });
                LinearLayout titleRow = new LinearLayout(activity);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);
                TextView titleView = createCategoryTitle(activity, cat, colorInts[0]);
                titleView.setTag(new Object[] { "cat", cat });
                if (isEditMode[0]) {
                    titleView.setOnDragListener(dragListener);
                    titleView.setOnTouchListener(new View.OnTouchListener() {
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                                v.startDragAndDrop(null, shadow, v, 0);
                                return true;
                            }
                            return false;
                        }
                    });
                    titleView.setText(cat + " (可拖动排序)");
                }
                titleRow.addView(titleView);
                catLayout.addView(titleRow);
                GridLayout gridLayout = new GridLayout(activity);
                gridLayout.setColumnCount(2);
                gridLayout.setClipChildren(false);
                gridLayout.setClipToPadding(false);
                gridLayout.setPadding(0, 0, 0, dp(activity, 8));
                List items = (List) catItems.get(cat);
                for (int j = 0; j < items.size(); j++) {
                    Object[]itemObj = (Object[]) items.get(j);
                    String title = (String) itemObj[1];
                    Runnable callback = (Runnable) itemObj[2];
                    FrameLayout menuItem = createMenuItem(activity, title, isDark, colorInts[1], isDark ? colorInts[1] : colorInts[0], bottomSheet, callback, false);
                    menuItem.setTag(new Object[] { "item", cat, title });
                    if (isEditMode[0]) {
                        menuItem.setOnDragListener(dragListener);
                        menuItem.setOnTouchListener(new View.OnTouchListener() {
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                                    v.startDragAndDrop(null, shadow, v, 0);
                                    return true;
                                }
                                return false;
                            }
                        });
                    }
                    if (!isEditMode[0]) {
                        menuItem.setAlpha(0f);
                        menuItem.setScaleX(0.9f);
                        menuItem.setScaleY(0.9f);
                        int delay = (i * 10 + j) * 25;
                        final FrameLayout finalItem = menuItem;
                        uiHandler.postDelayed(new Runnable() {
                            public void run() {
                                finalItem
                                    .animate()
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(180)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start();
                            }
                        }, delay);
                    }
                    gridLayout.addView(menuItem);
                }
                catLayout.addView(gridLayout);
                menuContainer.addView(catLayout);
            }
            int[]settingColorInts = getCategoryColorInts("设置", isDark);
            LinearLayout settingLayout = new LinearLayout(activity);
            settingLayout.setOrientation(LinearLayout.VERTICAL);
            settingLayout.setClipChildren(false);
            settingLayout.setClipToPadding(false);
            settingLayout.setTag(new Object[] { "cat", "设置" });
            LinearLayout settingTitleRow = new LinearLayout(activity);
            settingTitleRow.setOrientation(LinearLayout.HORIZONTAL);
            settingTitleRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView settingTitle = createCategoryTitle(activity, "设置", settingColorInts[0]);
            settingTitle.setTag(new Object[] { "cat", "设置" });
            if (isEditMode[0]) {
                settingTitle.setOnDragListener(dragListener);
                settingTitle.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                            v.startDragAndDrop(null, shadow, v, 0);
                            return true;
                        }
                        return false;
                    }
                });
                settingTitle.setText("设置 (可拖动排序)");
            }
            settingTitleRow.addView(settingTitle);
            settingLayout.addView(settingTitleRow);
            GridLayout settingGrid = new GridLayout(activity);
            settingGrid.setColumnCount(2);
            settingGrid.setClipChildren(false);
            settingGrid.setClipToPadding(false);
            settingGrid.setPadding(0, 0, 0, dp(activity, 8));
            FrameLayout sortMenuItem = createMenuItem(activity, "自定义排序", isDark, settingColorInts[1], isDark ? settingColorInts[1] : settingColorInts[0], bottomSheet, new Runnable() {
                public void run() {
                    isEditMode[0] = true;
                    if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                }
            }, true);
            sortMenuItem.setTag(new Object[] { "item", "设置", "自定义排序" });
            if (isEditMode[0]) {
                sortMenuItem.setOnDragListener(dragListener);
                sortMenuItem.setOnTouchListener(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                            v.startDragAndDrop(null, shadow, v, 0);
                            return true;
                        }
                        return false;
                    }
                });
            }
            settingGrid.addView(sortMenuItem);
            settingLayout.addView(settingGrid);
            if (isEditMode[0]) {
                LinearLayout actionBtnRow = new LinearLayout(activity);
                actionBtnRow.setOrientation(LinearLayout.HORIZONTAL);
                actionBtnRow.setGravity(Gravity.CENTER);
                actionBtnRow.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
                TextView cancelBtn = createButton(activity, "取消", Color.WHITE, isDark ? pc("#FF5252") : pc("#F44336"),
                    14f, 20, 16, 10, false, 0, 0, null);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        String saved = getString("setting", "menuSort", "");
                        applySavedSortOrder(catOrder, catItems, saved);
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                    }
                });
                TextView saveBtn = createButton(activity, "保存", Color.WHITE, isDark ? pc("#81C784") : pc("#4CAF50"),
                    14f, 20, 16, 10, false, 0, 0, null);
                saveBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isEditMode[0] = false;
                        saveOrder.run();
                        if (renderMenuWrapper[0] != null) renderMenuWrapper[0].run();
                        Toast("排序已保存");
                    }
                });
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                btnLp.setMargins(dp(activity, 16), 0, dp(activity, 16), 0);
                cancelBtn.setLayoutParams(btnLp);
                saveBtn.setLayoutParams(btnLp);
                actionBtnRow.addView(cancelBtn);
                actionBtnRow.addView(saveBtn);
                settingLayout.addView(actionBtnRow);
            }
            menuContainer.addView(settingLayout);
        }
    };
    renderMenuWrapper[0].run();
    scrollView.addView(menuContainer);
    rootLayout.addView(scrollView);
    bottomSheet.setContentView(rootLayout);
    bottomSheet.setCancelable(true);
    bottomSheet.setCanceledOnTouchOutside(true);
    bottomSheet.show();
    Window window = bottomSheet.getWindow();
    if (window != null) {
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.BOTTOM);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(sheetBg);
        float cr = dp(activity, getUiCornerDp());
        bgDrawable.setCornerRadii(new float[] { cr, cr, cr, cr, 0, 0, 0, 0 });
        rootLayout.setBackground(bgDrawable);
        rootLayout.setTranslationY(dp(activity, 120));
        rootLayout.setAlpha(0f);
        rootLayout
            .animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        if (!"color".equals(getString("settings", "ui_bg_type", "color"))) {
            applyUiTheme(activity, bottomSheet, 5);
        }
        applyWindowBlurBehind(window);
    }
}
