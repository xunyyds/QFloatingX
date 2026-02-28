/** 标记 Intent 防止递归 Hook */
private final String KEY_HANDLED = "qfun_script_handled";

/** 全局弹窗显示锁，防止多重弹窗 */
private volatile boolean isDialogShowing = false;

/** 原功能重放标记，用于回旋镖逻辑 */
private volatile boolean isReplayingClick = false;

/** 主线程 Handler，用于 UI 操作 */
private final Handler mainHandler = new Handler(Looper.getMainLooper());

/** 图片内存缓存 (Url -> Bitmap) */
private final HashMap picimageCache = new HashMap();

/** 原始特殊文本集合 (用于精准渲染 @ 和表情) */
private final HashSet validSpecialTexts = new HashSet();

/** 全局 Hook 注册表，用于一键卸载 */
private final List HOOK_REGISTRY = new ArrayList();

/** 滑动菜单状态池 [0:Popup, 1:Slider, 2:Centers, 4:TextViews, 5:BaseInfo, 7:UpdateRunnable, 8:Root] */
private final Object[] WHEEL_STATE = new Object[10];

/** 双击检测变量：上次点击时间 */
private volatile long lastClickTime = 0;

/** 双击检测变量：上次点击的 View Hash */
private volatile int lastClickViewHash = 0;

/** 双击检测变量：待执行的单击任务 (用于取消) */
private volatile Runnable pendingClickRunnable = null;

/**
 * 获取完整图片链接 (拼接域名和 RKey)
 *
 * @param url       图片相对路径或完整URL
 * @param chatType  聊天类型，1=私聊，2=群聊
 * @return          完整的图片URL
 */
String getFullPicUrl(String url, int chatType) {
    if (url == null || url.isEmpty()) return "";
    if (url.startsWith("http")) return url;
    String domain = "https://multimedia.nt.qq.com.cn";
    if (!url.startsWith("/")) url = "/" + url;
    String rkey = "";
    try {
        rkey = (chatType == 1) ? OnGetRKey.INSTANCE.getFriendRkey() : OnGetRKey.INSTANCE.getGroupRkey();
    } catch (Throwable t) {
        traceLog("main_log","RKey获取失败: " + t.getMessage());
    }
    return domain + url + rkey;
}


interface MsgLoadedCallback {
    void onLoaded(MsgData msgData);
}

/**
 * 通过 Kernel 异步获取完整消息记录
 *
 * @param msgId     消息ID
 * @param chatType  聊天类型
 * @param peerUid   对方UID
 * @param callback  加载完成回调
 */
void fetchRealMsgRecord(final long msgId, final int chatType, final String peerUid, final MsgLoadedCallback callback) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                if (msgId == 0) return;
                IKernelMsgService kernel = QQCurrentEnv.INSTANCE.getKernelMsgService();
                if (kernel == null) {
                    mainHandler.post(new Runnable() { public void run() { Toast("内核服务未就绪"); isDialogShowing = false; } });
                    return;
                }
                Contact contact = new Contact(chatType, peerUid, "");
                ArrayList ids = new ArrayList();
                ids.add(msgId);
                kernel.getMsgsByMsgId(contact, ids, new IMsgOperateCallback() {
                    public void onResult(int res, String err, ArrayList list) {
                        if (list != null && !list.isEmpty()) {
                            final MsgRecord realRecord = (MsgRecord) list.get(0);
                            try {
                                final MsgData msgData = new MsgData(realRecord);
                                mainHandler.post(new Runnable() { public void run() { if (callback != null) callback.onLoaded(msgData); } });
                            } catch (Throwable t) {
                                traceLog("main_log","MsgData构造失败");
                                isDialogShowing = false;
                            }
                        } else {
                            mainHandler.post(new Runnable() { public void run() { Toast("消息不存在"); isDialogShowing = false; } });
                        }
                    }
                });
            } catch (Throwable t) {
                traceLog("main_log","FetchMsg异常: " + t.getMessage());
                isDialogShowing = false;
            }
        }
    });
}

/**
 * 通过服务器转发消息到指定目标
 * <p>
 * 该方法利用QQ内核服务的转发功能，将消息原样转发到目标联系人。
 * 适用于非文本消息（图片、视频、语音、文件等）的转发场景。
 * </p>
 *
 * @param msgRecordObj 消息记录对象，必须为 MsgRecord 类型
 * @param targetUin    目标用户的QQ号或群号
 */
void forwardViaServer(Object msgRecordObj, String targetUin) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                if (!(msgRecordObj instanceof com.tencent.qqnt.kernel.nativeinterface.MsgRecord)) return;
                MsgRecord msgRecord = (MsgRecord) msgRecordObj;
                IKernelMsgService kernel = QQCurrentEnv.INSTANCE.getKernelMsgService();
                if (kernel == null) return;

                List msgIds = new ArrayList();
                msgIds.add(msgRecord.msgId);

                Contact srcContact = new Contact(msgRecord.chatType, msgRecord.peerUid, "");

                int targetChatType = isFriend(targetUin) ? 1 : 2;
                String targetUid = targetChatType == 1 ? getUidFromUin(targetUin) : targetUin;

                Contact dstContact = new Contact(targetChatType, targetUid, "");
                List dstContacts = new ArrayList();
                dstContacts.add(dstContact);

                kernel.forwardMsg(msgIds, srcContact, dstContacts, msgRecord.msgAttrs, null);
            } catch (Throwable ignored) {}
        }
    });
}

/**
 * 判断消息是否为纯文本消息
 * <p>
 * 遍历消息元素列表，检查是否仅包含文本类型元素。
 * 如果存在图片、视频、语音、文件等非文本元素，则返回 false。
 * </p>
 *
 * @param elements 消息元素列表
 * @return 如果是纯文本消息返回 true，否则返回 false
 */
boolean isTextOnlyMsg(List elements) {
    if (elements == null || elements.isEmpty()) return false;
    for (int i = 0; i < elements.size(); i++) {
        MsgElement el = (MsgElement) elements.get(i);
        if (el == null) continue;
        if (el.elementType != 1 && el.elementType != 7) return false;
    }
    return true;
}

/**
 * 解析文本中的 [pic=url] 标签，还原为 MsgElements
 *
 * @param text            包含图片标签的文本
 * @param sendElements    输出的消息元素列表
 * @param originalRecord  原始消息记录
 * @param chatType        聊天类型
 */
void parseTextToElements(String text, ArrayList sendElements, MsgRecord originalRecord, int chatType) {
    Map originalPicMap = new HashMap();
    if (originalRecord.elements != null) {
        for (int i = 0; i < originalRecord.elements.size(); i++) {
            MsgElement el = (MsgElement) originalRecord.elements.get(i);
            if (el.elementType == 2 && el.picElement != null) {
                String fullUrl = getFullPicUrl(el.picElement.originImageUrl, chatType);
                originalPicMap.put(fullUrl, el);
            }
        }
    }

    Pattern p = Pattern.compile("\\[pic=(.*?)\\]");
    Matcher m = p.matcher(text);
    int lastEnd = 0;
    while (m.find()) {
        String sub = text.substring(lastEnd, m.start());
        if (!sub.isEmpty()) addTextElement(sendElements, sub);

        String url = m.group(1);    
        if (originalPicMap.containsKey(url)) sendElements.add(originalPicMap.get(url));    
        else addTextElement(sendElements, "[pic=" + url + "]");    
        lastEnd = m.end();
    }
    String remain = text.substring(lastEnd);
    if (!remain.isEmpty()) addTextElement(sendElements, remain);
}

/**
 * 添加文本元素到消息元素列表
 *
 * @param list     消息元素列表
 * @param content  文本内容
 */
void addTextElement(ArrayList list, String content) {
    MsgElement el = new MsgElement();
    el.elementType = 1;
    TextElement te = new TextElement();
    te.content = content;
    te.atType = 0;
    el.textElement = te;
    list.add(el);
}

/**
 * 复读消息入口重载
 * @param data     消息数据对象
 */
void 复读(final Object data) {
    doMultiSend(data, null, 1);
}
/**
 * 发送或复读消息入口
 *
 * @param data     消息数据对象
 * @param newText  新文本内容，为 null 时表示原样复读
 */
void doSendOrRepeat(final MsgData data, final String newText) {
    doMultiSend(data, newText, 1);
}

/**
 * 执行发送逻辑（支持多次发送）
 * <p>
 * 核心发送方法，根据消息内容和类型选择最优发送策略：
 * <ul>
 *   <li>有新文本内容：解析文本中的图片标签，构建消息元素发送</li>
 *   <li>无新文本且为纯文本消息：直接复制元素发送</li>
 *   <li>无新文本且为非文本消息：使用 forwardViaServer 转发</li>
 * </ul>
 * </p>
 *
 * @param data     消息数据对象
 * @param newText  新文本内容，为 null 时表示原样复读
 * @param count    发送次数
 */
void doMultiSend(final MsgData data, final String newText, final int count) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                if (data == null || data.data == null) return;
                MsgRecord originalRecord = data.data;

                if (newText == null && !isTextOnlyMsg(originalRecord.elements)) {
                    String targetUin = String.valueOf(originalRecord.peerUin);
                    for (int i = 0; i < count; i++) {
                        forwardViaServer(originalRecord, targetUin);
                    }
                    final String tips = "转发" + (count > 1 ? " x" + count : "") + " 完成";
                    mainHandler.post(new Runnable() { public void run() { Toast(tips); } });
                    return;
                }

                final ArrayList sendElements = new ArrayList();

                if (newText != null) {
                    parseTextToElements(newText, sendElements, originalRecord, data.type);
                } else {
                    if (originalRecord.elements != null) sendElements.addAll(originalRecord.elements);
                }

                IMsgService msgService = (IMsgService) QRoute.api(IMsgService.class);
                if (msgService == null) return;

                String targetUid = originalRecord.peerUid;
                if (targetUid == null || targetUid.isEmpty()) {
                    if (originalRecord.chatType == 2) targetUid = String.valueOf(originalRecord.peerUin);
                    else targetUid = FriendTool.INSTANCE.getUidFromUin(String.valueOf(originalRecord.peerUin));
                }

                Contact contact = new Contact(originalRecord.chatType, targetUid, "");
                for (int i = 0; i < count; i++) {
                    msgService.sendMsg(contact, sendElements, null);
                }
                final String tips = (newText != null ? "发送" : "复读") + (count > 1 ? " x" + count : "") + " 完成";
                mainHandler.post(new Runnable() { public void run() { Toast(tips); } });
            } catch (final Throwable e) {
                mainHandler.post(new Runnable() { public void run() { Toast("失败: " + e.getMessage()); } });
            }
        }
    });
}

/**
 * 下载图片到内存缓存
 *
 * @param url       图片URL
 * @param callback  下载完成回调
 */
void downloadImage(final String url, final Runnable callback) {
    if (picimageCache.containsKey(url)) { if (callback != null) callback.run();
        return; }
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                URL u = new URL(url);
                InputStream is = u.openStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                is.close();
                Bitmap bmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                if (bmp != null) {
                    picimageCache.put(url, bmp);
                    if (callback != null) mainHandler.post(callback);
                }
            } catch (Throwable t) {}
        }
    });
}

/** 自定义链接 Span */
class LinkTagSpan extends ForegroundColorSpan {
    public String url;
    public LinkTagSpan(String url) { super(Color.parseColor("#007AFF"));
        this.url = url; }
}

/**
 * 文本渲染逻辑
 *
 * @param et          编辑框控件
 * @param forceImage  是否强制渲染图片
 */
void applySpans(final EditText et, final boolean forceImage) {
    Editable s = et.getText();
    String text = s.toString();
    final Context ctx = et.getContext();
    int selStart = et.getSelectionStart();
    int selEnd = et.getSelectionEnd();
    Object[] allSpans = s.getSpans(0, s.length(), Object.class);
    for (int i = 0; i < allSpans.length; i++) {
        Object span = allSpans[i];
        if (span instanceof LinkTagSpan || span instanceof ForegroundColorSpan || span instanceof ImageSpan || span instanceof UnderlineSpan) {
            s.removeSpan(span);
        }
    }

    Pattern pPic = Pattern.compile("\\[pic=(.*?)\\]");
    Matcher mPic = pPic.matcher(text);
    while (mPic.find()) {
        final String url = mPic.group(1);
        final int start = mPic.start();
        final int end = mPic.end();
        boolean isCursorInside = (selStart >= start && selStart <= end) || (selEnd >= start && selEnd <= end);
        if ((forceImage || (picimageCache.containsKey(url) && !isCursorInside))) {
            downloadImage(url, new Runnable() {
                public void run() {
                    String cur = et.getText().toString();
                    if (cur.length() >= end && cur.substring(start, end).equals("[pic=" + url + "]")) {
                        Bitmap bmp = picimageCache.get(url);
                        if (bmp != null) {
                            int h = dp(ctx, 64);
                            int w = (int) ((float)bmp.getWidth() / bmp.getHeight() * h);
                            Bitmap scaled = Bitmap.createScaledBitmap(bmp, w, h, true);
                            Drawable d = new BitmapDrawable(ctx.getResources(), scaled);
                            d.setBounds(0, 0, w, h);
                            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                            et.getText().setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            });
        } else {
            s.setSpan(new LinkTagSpan(url), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    for (Object validObj : validSpecialTexts) {
        String validText = (String) validObj;
        if (validText == null || validText.isEmpty()) continue;

        int idx = text.indexOf(validText);    
        while (idx >= 0) {    
            int end = idx + validText.length();    
            int color = 0;    
            if (validText.startsWith("@")) color = Color.parseColor("#007AFF");    
            else if (validText.startsWith("/")) color = Color.parseColor("#A6FFD700");

            if (color != 0) {    
                s.setSpan(new ForegroundColorSpan(color), idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);    
            }    
            idx = text.indexOf(validText, end);    
        }
    }
}

/**
 * 触摸监听：处理点击图片 Span 还原为文本
 *
 * @param et  编辑框控件
 */
void setupEditTextTouch(final EditText et) {
    final GestureDetector gestureDetector = new GestureDetector(et.getContext(), new GestureDetector.SimpleOnGestureListener() {
        public boolean onSingleTapUp(MotionEvent e) {
            int x = (int) e.getX() - et.getTotalPaddingLeft() + et.getScrollX();
            int y = (int) e.getY() - et.getTotalPaddingTop() + et.getScrollY();
            Layout layout = et.getLayout();
            if (layout == null) return false;
            int line = layout.getLineForVertical(y);
            int offset = layout.getOffsetForHorizontal(line, x);
            Editable text = et.getText();
            ImageSpan[] imgs = text.getSpans(offset, offset, ImageSpan.class);
            if (imgs.length > 0) {
                ImageSpan clickedSpan = imgs[0];
                int spanStart = text.getSpanStart(clickedSpan);
                int spanEnd = text.getSpanEnd(clickedSpan);
                if (offset >= spanStart && offset <= spanEnd) {
                    text.removeSpan(clickedSpan);
                    et.setSelection((spanStart + spanEnd) / 2);
                    applySpans(et, false);
                    return true;
                }
            }
            return false;
        }
    });
    et.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) { return gestureDetector.onTouchEvent(event); }
    });
}

/**
 * 创建按钮组件
 *
 * @param ctx         上下文
 * @param text        按钮文本
 * @param bgColorStr  背景颜色字符串
 * @param textColor   文字颜色
 * @param onClick     点击回调
 * @return            按钮组件
 */
FrameLayout createButton(Context ctx, String text, String bgColorStr, int textColor, final Runnable onClick) {
    final FrameLayout btn = new FrameLayout(ctx);
    final GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(ctx, 8));
    bg.setColor(Color.parseColor(bgColorStr));
    btn.setBackground(bg);
    TextView tv = new TextView(ctx);
    tv.setText(text);
    tv.setTextColor(textColor);
    tv.setTextSize(14);
    tv.setGravity(Gravity.CENTER);
    tv.setTypeface(null, Typeface.BOLD);
    btn.addView(tv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onClick.run(); } });
    return btn;
}

/**
 * 创建预览按钮组件
 *
 * @param ctx      上下文
 * @param onClick  点击回调
 * @return         预览按钮组件
 */
FrameLayout createPreviewButton(Context ctx, final Runnable onClick) {
    final FrameLayout btn = new FrameLayout(ctx);
    final GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(ctx, 8));
    bg.setColor(Color.TRANSPARENT);
    bg.setStroke(dp(ctx, 1), Color.parseColor("#E0E0E0"));
    btn.setBackground(bg);
    TextView tv = new TextView(ctx);
    tv.setText("预览");
    tv.setTextColor(Color.parseColor("#999999"));
    tv.setTextSize(12);
    tv.setGravity(Gravity.CENTER);
    btn.addView(tv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    btn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onClick.run(); } });
    return btn;
}

/**
 * 创建消息引用展示框
 *
 * @param ctx      上下文
 * @param msgData  消息数据对象
 * @return         引用展示框视图，无引用时返回 null
 */
View createReplyBox(Context ctx, MsgData msgData) {
    MsgElement replyEl = null;
    if (msgData.data.elements != null) {
        for (int i=0; i<msgData.data.elements.size(); i++) {
            MsgElement e = (MsgElement) msgData.data.elements.get(i);
            if (e != null && e.elementType == 7) { replyEl = e; break;
            }
        }
    }
    if (replyEl == null || replyEl.replyElement == null) return null;
    ReplyElement re = replyEl.replyElement;
    LinearLayout replyBox = new LinearLayout(ctx);
    replyBox.setOrientation(LinearLayout.HORIZONTAL);
    replyBox.setBackgroundColor(Color.parseColor("#F5F5F5"));
    replyBox.setPadding(0, 0, dp(ctx, 8), 0);
    View line = new View(ctx);
    GradientDrawable lineBg = new GradientDrawable();
    lineBg.setColor(Color.parseColor("#D0D0D0"));
    lineBg.setCornerRadius(dp(ctx, 2));
    line.setBackground(lineBg);
    replyBox.addView(line, new LinearLayout.LayoutParams(dp(ctx, 4), -1));
    LinearLayout replyTextContainer = new LinearLayout(ctx);
    replyTextContainer.setOrientation(LinearLayout.VERTICAL);
    replyTextContainer.setPadding(dp(ctx, 8), dp(ctx, 6), 0, dp(ctx, 6));
    long quoteTime = re.replyMsgTime;
    String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(quoteTime * 1000L));
    String nick = "引用消息";
    if (msgData.data.records != null && !msgData.data.records.isEmpty()) {
        try {
            MsgRecord src = (MsgRecord) msgData.data.records.get(0);
            if (src.sendNickName != null && !src.sendNickName.isEmpty()) nick = src.sendNickName;
        } catch(Throwable t) {}
    } else if (re.sourceMsgText != null && re.sourceMsgText.contains(":")) {
        nick = re.sourceMsgText.substring(0, re.sourceMsgText.indexOf(":"));
    }

    TextView tvHeader = new TextView(ctx);
    tvHeader.setText(nick + " " + timeStr);
    tvHeader.setTextSize(12);
    tvHeader.setTextColor(Color.GRAY);
    replyTextContainer.addView(tvHeader);
    TextView tvBody = new TextView(ctx);
    tvBody.setTextSize(13);
    tvBody.setTextColor(Color.parseColor("#666666"));
    tvBody.setMaxLines(3);
    tvBody.setEllipsize(TextUtils.TruncateAt.END);

    SpannableStringBuilder ssb = new SpannableStringBuilder();
    boolean hasContent = false;
    if (re.sourceMsgTextElems != null && !re.sourceMsgTextElems.isEmpty()) {
        for (int i=0; i<re.sourceMsgTextElems.size(); i++) {
            ReplyAbsElement rae = (ReplyAbsElement) re.sourceMsgTextElems.get(i);
            if (rae == null) continue;
            if (rae.textElemContent != null) { ssb.append(rae.textElemContent); hasContent = true;
            }
            else if (rae.faceElem != null) { ssb.append(rae.faceElem.faceText != null ? rae.faceElem.faceText : "[表情]");
                hasContent = true; }
            else if (rae.picElem != null) { ssb.append("[图片]");
                hasContent = true; }
        }
    }
    if (!hasContent && re.sourceMsgText != null && !re.sourceMsgText.isEmpty()) {
        ssb.append(re.sourceMsgText);
        hasContent = true;
    }
    if (!hasContent) ssb.append("[图片]");

    tvBody.setText(ssb);
    replyTextContainer.addView(tvBody);
    replyBox.addView(replyTextContainer, new LinearLayout.LayoutParams(-1, -2));
    return replyBox;
}


/**
 * 调用外部作图方法
 *
 * @param activity  Activity上下文
 * @param content   待处理内容
 */
void 作图(Activity activity, String content) {
    Toast("还是空壳\n" + content);
    traceLog("main_log","Call makeImage()");
}

/**
 * 更新滑动选择器物理效果
 *
 * @param rawDx   原始水平位移
 * @param isDrag  是否为拖拽操作
 */
void updateSliderPhysics(float rawDx, boolean isDrag) {
    View slider = (View) WHEEL_STATE[1];
    Runnable updater = (Runnable) WHEEL_STATE[7];
    if (slider == null) return;
    float[] baseInfo = (float[]) WHEEL_STATE[5];
    float targetTrans = baseInfo[1] + rawDx;
    slider.setTranslationX(targetTrans);
    if (updater != null) updater.run();
}

/**
 * 显示iOS风格滑动选择器
 *
 * @param activity      Activity上下文
 * @param anchor        锚点视图
 * @param modeIndexRef  当前模式索引引用
 * @param modeNames     模式名称数组
 * @param actionBtn     操作按钮
 * @param btnTv         按钮文本视图
 * @param initialEvent  初始触摸事件
 * @return              弹出窗口实例
 */
PopupWindow showIOSStyleWheelSelector(
        final Activity activity,
        View anchor,
        final int[] modeIndexRef,
        final String[] modeNames,
        final FrameLayout actionBtn,
        final TextView btnTv,
        MotionEvent initialEvent
) {
    final PopupWindow popup = new PopupWindow(activity);
    popup.setBackgroundDrawable(null);
    popup.setOutsideTouchable(false);
    popup.setFocusable(false);
    popup.setTouchable(true);

    FrameLayout root = new FrameLayout(activity);    
    root.setBackgroundColor(Color.TRANSPARENT);    
    root.setAlpha(0f);    
    root.setScaleX(0.9f);    
    root.setScaleY(0.9f);    

    FrameLayout card = new FrameLayout(activity);    
    GradientDrawable cardBg = new GradientDrawable();    
    cardBg.setColor(Color.parseColor("#F2F2F7"));    
    cardBg.setCornerRadius(dp(activity, 16));    
    card.setBackground(cardBg);    
    card.setClipChildren(false);     
    card.setClipToPadding(false);    
        
    final int ITEM_H = dp(activity, 36) + dp(activity, 1);    
    final int ITEM_W = dp(activity, 49);     
    final int GAP = dp(activity, 4);    
    final int PADDING = dp(activity, 8);    
    final View slider = new View(activity);    
    GradientDrawable sliderBg = new GradientDrawable();    
    sliderBg.setColor(Color.WHITE);    
    sliderBg.setCornerRadius(dp(activity, 14));    
    slider.setBackground(sliderBg);    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {    
        slider.setClipToOutline(true);    
        slider.setOutlineProvider(new ViewOutlineProvider() {    
            public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(activity, 14)); }    
        });    
    }    
    FrameLayout.LayoutParams sliderLp = new FrameLayout.LayoutParams(ITEM_W, ITEM_H);    
    sliderLp.topMargin = PADDING;    
    sliderLp.leftMargin = PADDING;    
    card.addView(slider, sliderLp);    
    final FrameLayout textContainer = new FrameLayout(activity);    
    final TextView[] textViews = new TextView[modeNames.length];    
    final float[] itemCenters = new float[modeNames.length];    
    for (int i = 0; i < modeNames.length; i++) {    
        TextView tv = new TextView(activity);    
        tv.setText(modeNames[i]);    
        tv.setGravity(Gravity.CENTER);    
        tv.setTextSize(13);    
        tv.setTextColor(Color.BLACK);     
        float centerX = PADDING + (ITEM_W / 2f) + i * (ITEM_W + GAP);    
        itemCenters[i] = centerX;    
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ITEM_W, ITEM_H);    
        lp.leftMargin = (int)(centerX - ITEM_W / 2f);    
        lp.topMargin = PADDING;    
        textContainer.addView(tv, lp);    
        textViews[i] = tv;    
    }    
        
    int containerW = (ITEM_W * modeNames.length) + (GAP * (modeNames.length - 1)) + PADDING * 2;    
    card.addView(textContainer, new FrameLayout.LayoutParams(containerW, ITEM_H + PADDING*2, Gravity.CENTER));    
    root.addView(card);    
    popup.setContentView(root);    
        
    root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);    
    int popupHeight = root.getMeasuredHeight();    
    int[] location = new int[2];    
    anchor.getLocationOnScreen(location);    
    popup.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], location[1] - popupHeight - dp(activity, 4));

    root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();    

    WHEEL_STATE[0] = popup;    
    WHEEL_STATE[1] = slider;    
    WHEEL_STATE[2] = itemCenters;    
    WHEEL_STATE[4] = textViews;    
    WHEEL_STATE[8] = root;    
        
    final float startCenterX = itemCenters[modeIndexRef[0]];    
    final float minCenter = itemCenters[0];    
        
    slider.setTranslationX(startCenterX - minCenter);
    final Runnable physicsUpdate = new Runnable() {    
        public void run() {    
            View s = (View)WHEEL_STATE[1];    
            float[] centers = (float[])WHEEL_STATE[2];    
            TextView[] tvs = (TextView[])WHEEL_STATE[4];    
            if(s==null || centers==null) return;

            float logicTrans = s.getTranslationX();    
            float logicCenter = centers[0] + logicTrans;    
            int baseW = dp(activity, 49);    
            int newW = baseW;    
            float visualTrans = logicTrans;    
            float maxTrans = centers[centers.length - 1] - centers[0];

            if (logicTrans < 0) {    
                visualTrans = 0f;    
                float overflow = -logicTrans;    
                float squash = overflow * 0.4f;    
                if (squash > baseW * 0.4f) squash = baseW * 0.4f;    
                newW = baseW - (int)squash;    
            } else if (logicTrans > maxTrans) {    
                float overflow = logicTrans - maxTrans;    
                float squash = overflow * 0.4f;    
                if (squash > baseW * 0.4f) squash = baseW * 0.4f;    
                newW = baseW - (int)squash;    
                visualTrans = maxTrans + (baseW - newW);    
            } else {    
                for (int i=0; i<centers.length-1; i++) {    
                    if (logicCenter >= centers[i] && logicCenter <= centers[i+1]) {    
                        float range = centers[i+1] - centers[i];    
                        float progress = (logicCenter - centers[i]) / range;     
                        float stretch = (float) Math.sin(progress * Math.PI) * dp(activity, 10);    
                        newW = baseW + (int)stretch;    
                        break;    
                    }    
                }    
            }

            if (s.getTranslationX() != visualTrans) s.setTranslationX(visualTrans);    
            ViewGroup.LayoutParams lp = s.getLayoutParams();    
            if (lp.width != newW) { lp.width = newW; s.setLayoutParams(lp);    
            }    
                
            for(int i=0; i<centers.length; i++) {    
                float dist = Math.abs(logicCenter - centers[i]);    
                if (dist < baseW * 0.6f) tvs[i].setTextColor(Color.parseColor("#007AFF"));     
                else tvs[i].setTextColor(Color.BLACK);    
            }    
        }    
    };    
    WHEEL_STATE[7] = physicsUpdate;    
    mainHandler.post(physicsUpdate);    
    return popup;
}

/**
 * 显示操作对话框
 *
 * @param activity       Activity上下文
 * @param msgData        消息数据对象
 * @param originalIntent 原始Intent
 * @param targetView     目标视图
 */
void showActionDialog(final Activity activity, final MsgData msgData, final Intent originalIntent, final View targetView) {
    if (activity == null || activity.isFinishing()) {
        isDialogShowing = false;
        return;
    }

    final String[] modeNames = new String[]{"复读", "多次复读", "作图", "加解密"};    
    final int[] currentModeIndex = new int[]{0};    
    final SpannableStringBuilder initSb = new SpannableStringBuilder();    
        
    validSpecialTexts.clear();    
        
    if (msgData.data.elements != null) {    
        for (int i=0; i<msgData.data.elements.size(); i++) {    
             MsgElement el = (MsgElement)msgData.data.elements.get(i);    
             if (el.elementType == 1 && el.textElement != null) {    
                 initSb.append(el.textElement.content);    
                 if (el.textElement.atType != 0) validSpecialTexts.add(el.textElement.content);    
             } else if (el.elementType == 2 && el.picElement != null) {    
                 initSb.append("[pic=" + getFullPicUrl(el.picElement.originImageUrl, msgData.type) + "]");    
             } else if (el.elementType == 6 && el.faceElement != null) {    
                 initSb.append(el.faceElement.faceText);    
                 validSpecialTexts.add(el.faceElement.faceText);    
             }    
        }    
    }    
    final String originalTextString = initSb.toString();    
    final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);    
    dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
        public void onDismiss(android.content.DialogInterface d) {     
            isDialogShowing = false;    
            for(int i=0; i<WHEEL_STATE.length; i++) WHEEL_STATE[i] = null;    
        }    
    });    
    FrameLayout root = new FrameLayout(activity);    
    root.setBackgroundColor(Color.parseColor("#99000000"));    
    root.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });    
    FrameLayout card = new FrameLayout(activity);    
    GradientDrawable bg = new GradientDrawable();    
    bg.setColor(Color.WHITE);    
    bg.setCornerRadius(dp(activity, 16));    
    card.setBackground(bg);    

    LinearLayout content = new LinearLayout(activity);    
    content.setOrientation(LinearLayout.VERTICAL);    
    content.setPadding(dp(activity, 16), dp(activity, 20), dp(activity, 16), dp(activity, 12));    
        
    ScrollView scroll = new ScrollView(activity);    
    scroll.addView(content);    
    card.addView(scroll);    

    View replyView = createReplyBox(activity, msgData);    
    if (replyView != null) {    
        content.addView(replyView);    
        content.addView(new View(activity), new LinearLayout.LayoutParams(-1, dp(activity, 12)));    
    }    

    LinearLayout interactionRow = new LinearLayout(activity);    
    interactionRow.setOrientation(LinearLayout.HORIZONTAL);    
    interactionRow.setGravity(Gravity.CENTER_VERTICAL);    
    interactionRow.setPadding(0, 0, 0, dp(activity, 12));    
    final EditText editText = new EditText(activity);    
    editText.setText(initSb);    
    editText.setTextColor(Color.BLACK);    
        
    boolean isSelf = false;    
    try {    
        Object qObj = null;    
        try { qObj = qq;    
        } catch(Throwable t) {}     
        if (qObj != null && Long.parseLong(qObj.toString()) == msgData.data.senderUin) isSelf = true;    
    } catch(Throwable t) {}    
        
    GradientDrawable etBg = new GradientDrawable();    
    etBg.setCornerRadius(dp(activity, 8));    
    if (isSelf) {    
        etBg.setColor(Color.parseColor("#E7F0FF"));     
        etBg.setStroke(dp(activity, 1), Color.parseColor("#C8D4E5"));    
    } else {    
        etBg.setColor(Color.parseColor("#F5F5F5"));     
        etBg.setStroke(dp(activity, 1), Color.parseColor("#E0E0E0"));    
    }    
    editText.setBackground(etBg);    
    editText.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));    
        
    setupEditTextTouch(editText);    
    LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(0, -2, 1.0f);    
    etParams.rightMargin = dp(activity, 8);    
    interactionRow.addView(editText, etParams);    

    LinearLayout rightBtnContainer = new LinearLayout(activity);    
    rightBtnContainer.setOrientation(LinearLayout.VERTICAL);    
    rightBtnContainer.setGravity(Gravity.CENTER_HORIZONTAL);    
        
    final FrameLayout actionBtn = new FrameLayout(activity);    
    GradientDrawable btnBg = new GradientDrawable();    
    btnBg.setCornerRadius(dp(activity, 8));    
    btnBg.setColor(Color.parseColor("#FF007AFF"));    
    actionBtn.setBackground(btnBg);    

    final TextView btnTv = new TextView(activity);    
    btnTv.setText(modeNames[0]);    
    btnTv.setTextColor(Color.WHITE);    
    btnTv.setGravity(Gravity.CENTER);    
    actionBtn.addView(btnTv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));    
    rightBtnContainer.addView(actionBtn, new LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 40)));    
        
    View spacer = new View(activity);    
    rightBtnContainer.addView(spacer, new LinearLayout.LayoutParams(-1, dp(activity, 4)));    
    FrameLayout previewBtn = createPreviewButton(activity, new Runnable() {    
        public void run() { applySpans(editText, true); }    
    });    
    rightBtnContainer.addView(previewBtn, new LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 28)));    

    interactionRow.addView(rightBtnContainer);    

    final boolean[] isActionTriggered = {false};    
    actionBtn.setOnTouchListener(new View.OnTouchListener() {    
        private float downX = 0;    
        private boolean hasMoved = false;    

        public boolean onTouch(View v, MotionEvent event) {    
            int action = event.getAction();    
            switch (action) {    
                case MotionEvent.ACTION_DOWN:    
                    downX = event.getRawX();    
                    hasMoved = false;    
                    isActionTriggered[0] = false;    
                    WHEEL_STATE[0] = showIOSStyleWheelSelector(activity, actionBtn, currentModeIndex, modeNames, actionBtn, btnTv, event);    
                        
                    View s = (View) WHEEL_STATE[1];    
                    WHEEL_STATE[5] = new float[]{downX, s.getTranslationX()};    
                    return true;    

                case MotionEvent.ACTION_MOVE:    
                    float rawDx = event.getRawX() - downX;    
                    if (Math.abs(rawDx) > dp(activity, 15)) hasMoved = true;    
                    if (WHEEL_STATE[0] != null && hasMoved) {    
                        updateSliderPhysics(rawDx, true);    
                    }    
                    return true;    
                case MotionEvent.ACTION_UP:    
                case MotionEvent.ACTION_CANCEL:    
                    if (WHEEL_STATE[0] != null) {    
                        final PopupWindow popup = (PopupWindow) WHEEL_STATE[0];    
                        if (!hasMoved) {    
                            if (!isActionTriggered[0]) {    
                                isActionTriggered[0] = true;    
                                String currentText = editText.getText().toString();    
                                boolean isModified = !currentText.equals(originalTextString);    
                                int mode = currentModeIndex[0];    
                                if (mode == 0) {
                                    dialog.dismiss();    
                                    doSendOrRepeat(msgData, isModified ? currentText : null);    
                                } else if (mode == 1) {
                                    dialog.dismiss();    
                                    showRepeatCountDialog(activity, msgData, isModified ? currentText : null);    
                                } else if (mode == 2) {
                                    dialog.dismiss();    
                                    作图(activity, currentText);    
                                } else if (mode == 3) {
                                    dialog.dismiss();    
                                    showEncryptDecryptDialog(activity, currentText);    
                                }    
                            }    
                        } else {
                            View slider = (View)WHEEL_STATE[1];    
                            View menuRoot = (View)WHEEL_STATE[8];    
                            float[] centers = (float[])WHEEL_STATE[2];    
                                
                            if (slider != null) {    
                                float logicTrans = slider.getTranslationX();    
                                float logicCenter = centers[0] + logicTrans;    
                                int bestIdx = 0;    
                                float minDist = Float.MAX_VALUE;    
                                for(int i=0; i<centers.length; i++) {    
                                    float d = Math.abs(logicCenter - centers[i]);    
                                    if(d < minDist) { minDist = d; bestIdx = i;    
                                    }    
                                }    
                                    
                                final float targetTrans = centers[bestIdx] - centers[0];    
                                SpringAnimation animX = new SpringAnimation(slider, DynamicAnimation.TRANSLATION_X, targetTrans);    
                                animX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);    
                                animX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);    
                                    
                                final int finalIdx = bestIdx;    
                                currentModeIndex[0] = finalIdx;    
                                btnTv.setText(modeNames[finalIdx]);    
                                GradientDrawable bg = (GradientDrawable) actionBtn.getBackground();    
                                String[] colors = {"#007AFF", "#FF9500", "#AF52DE", "#34C759"};    
                                bg.setColor(Color.parseColor(colors[finalIdx % colors.length]));    
                                    
                                String now = editText.getText().toString();    
                                boolean mod = !now.equals(originalTextString);    
                                if (finalIdx == 0) btnTv.setText(mod ? "发送" : "复读");    
                                else if (finalIdx == 1) btnTv.setText(mod ? "多次发送" : "多次复读");    
                                else btnTv.setText(modeNames[finalIdx]);    
                                    
                                animX.start();    
                                if (menuRoot != null) {    
                                    menuRoot.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(150)    
                                        .withEndAction(new Runnable() { public void run() { if(popup.isShowing()) popup.dismiss(); } })    
                                        .start();    
                                } else {    
                                    popup.dismiss();    
                                }    
                            } else {    
                                popup.dismiss();    
                            }    
                        }    
                    }    
                    WHEEL_STATE[0] = null;    
                    hasMoved = false;    
                    return true;    
            }    
            return false;    
        }    
    });    
        
    content.addView(interactionRow);    

    editText.addTextChangedListener(new TextWatcher() {    
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}    
        public void onTextChanged(CharSequence s, int start, int before, int count) {}    
        public void afterTextChanged(Editable s) {    
            String nowText = s.toString();    
            boolean isModified = !nowText.equals(originalTextString);    
            int idx = currentModeIndex[0];    
                
            if (idx == 0) {    
                btnTv.setText(isModified ? "发送" : "复读");    
                actionBtn.setAlpha(TextUtils.isEmpty(nowText) ? 0.5f : 1.0f);    
                actionBtn.setEnabled(!TextUtils.isEmpty(nowText));    
            } else if (idx == 1) {    
                btnTv.setText(isModified ? "多次发送" : "多次复读");    
                actionBtn.setAlpha(1.0f);    
                actionBtn.setEnabled(true);    
            } else {    
                btnTv.setText(modeNames[idx]);    
                actionBtn.setAlpha(1.0f);    
            }    
            applySpans(editText, false);    
        }    
    });    
        
    mainHandler.post(new Runnable() { public void run() { applySpans(editText, true); } });    
    LinearLayout btm = new LinearLayout(activity);    
    btm.setPadding(0, dp(activity, 4), 0, 0);    
    FrameLayout menuBtn = createButton(activity, "更多", "#B3007AFF", Color.WHITE, new Runnable() {    
        public void run() {     
            dialog.dismiss();     
            菜单(msgData);     
        }    
    });    
    FrameLayout origBtn = createButton(activity, "原功能", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {    
        public void run() {    
            dialog.dismiss();     
            if (originalIntent != null) {    
                try {    
                    originalIntent.putExtra(KEY_HANDLED, true);    
                    activity.startActivity(originalIntent);    
                } catch (Exception e) {}    
            } else if (targetView != null) {    
                isReplayingClick = true;    
                try {    
                    targetView.performClick();    
                } catch (Exception e) {    
                } finally {
                    new Handler().postDelayed(new Runnable() {    
                        public void run() { isReplayingClick = false; }    
                    }, 500);    
                }    
            }    
        }    
    });    
    btm.addView(menuBtn, new LinearLayout.LayoutParams(0, dp(activity, 42), 1));    
    btm.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 12), -1));    
    btm.addView(origBtn, new LinearLayout.LayoutParams(0, dp(activity, 42), 1));    
    content.addView(btm);    
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(activity, 320), -2);    
    cp.gravity = Gravity.CENTER;    
    root.addView(card, cp);    
    dialog.setContentView(root);    
    dialog.show();
}

/**
 * 高频复读警告弹窗
 *
 * @param activity  Activity上下文
 * @param data      消息数据对象
 * @param text      文本内容
 * @param count     发送次数
 * @param parent    父对话框
 */
void showBigCountConfirm(Activity activity, final MsgData data, final String text, final int count, final Dialog parent) {
    final Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
    FrameLayout root = new FrameLayout(activity);
    root.setBackgroundColor(Color.parseColor("#99000000"));

    FrameLayout card = new FrameLayout(activity);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.WHITE);
    bg.setCornerRadius(dp(activity, 16));
    card.setBackground(bg);
    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
    card.addView(content);

    TextView title = new TextView(activity);
    title.setText("⚠️ 高频警告");
    title.setTextSize(18);
    title.setTextColor(Color.RED);
    title.setGravity(Gravity.CENTER);
    content.addView(title);

    TextView msg = new TextView(activity);
    msg.setText("复读 " + count + " 次可能导致账号风控\n确定要继续吗？");
    msg.setTextSize(15);
    msg.setTextColor(Color.BLACK);
    msg.setGravity(Gravity.CENTER);
    msg.setPadding(0, dp(activity, 10), 0, 0);
    content.addView(msg);

    LinearLayout btns = new LinearLayout(activity);
    btns.setPadding(0, dp(activity, 16), 0, 0);
    FrameLayout cancel = createButton(activity, "取消", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {
        public void run() { d.dismiss(); }
    });
    FrameLayout confirm = createButton(activity, "继续发送", "#FF3B30", Color.WHITE, new Runnable() {
        public void run() {
            d.dismiss();
            if(parent != null) parent.dismiss();
            doMultiSend(data, text, count);
        }
    });
    btns.addView(cancel, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    btns.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 12), -1));
    btns.addView(confirm, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    content.addView(btns);
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(activity, 280), -2);
    cp.gravity = Gravity.CENTER;
    root.addView(card, cp);
    d.setContentView(root);
    d.show();
}

/**
 * 次数输入弹窗
 *
 * @param activity     Activity上下文
 * @param data         消息数据对象
 * @param currentText  当前文本内容
 */
void showRepeatCountDialog(final Activity activity, final MsgData data, final String currentText) {
    final Dialog d = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
    FrameLayout root = new FrameLayout(activity);
    root.setBackgroundColor(Color.parseColor("#99000000"));
    FrameLayout card = new FrameLayout(activity);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.WHITE);
    bg.setCornerRadius(dp(activity, 16));
    card.setBackground(bg);
    LinearLayout content = new LinearLayout(activity);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
    card.addView(content);
    final EditText et = new EditText(activity);
    et.setInputType(InputType.TYPE_CLASS_NUMBER);
    et.setHint("请输入次数");
    content.addView(et);
    LinearLayout btns = new LinearLayout(activity);
    btns.setPadding(0, dp(activity, 16), 0, 0);
    FrameLayout cancel = createButton(activity, "取消", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {
        public void run() { d.dismiss(); }
    });
    FrameLayout confirm = createButton(activity, "确定", "#FF007AFF", Color.WHITE, new Runnable() {
        public void run() {
            try {
                int count = Integer.parseInt(et.getText().toString());
                if (count > 0) {
                    if (count > 10) {
                        showBigCountConfirm(activity, data, currentText, count, d);

                    } else {    
                         doMultiSend(data, currentText, count);    
                         d.dismiss();    
                     }    
                 } else {    
                     Toast("次数必须大于0");    
                 }    
             } catch(Exception e) { Toast("请输入有效数字"); }    
         }
    });
    btns.addView(cancel, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    btns.addView(new View(activity), new LinearLayout.LayoutParams(dp(activity, 12), -1));
    btns.addView(confirm, new LinearLayout.LayoutParams(0, dp(activity, 40), 1));
    content.addView(btns);
    FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(activity, 280), -2);
    cp.gravity = Gravity.CENTER;
    root.addView(card, cp);
    d.setContentView(root);
    d.show();
}

/**
 * 卸载所有已注册的Hook
 */
void uninstallQFunHooks() {
    if (HOOK_REGISTRY.isEmpty()) return;
    for (int i = 0; i < HOOK_REGISTRY.size(); i++) {
        try {
            ((XC_MethodHook.Unhook) HOOK_REGISTRY.get(i)).unhook();
        } catch (Exception e) {}
    }
    HOOK_REGISTRY.clear();
    traceLog("main_log","Hook 已卸载");
}

/**
 * 安装QFun Hook拦截器
 */
void installQFunHooks() {
    uninstallQFunHooks();

    try {    
        XC_MethodHook.Unhook h1 = XposedBridge.hookMethod(    
            Instrumentation.class.getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class),    
            new XC_MethodHook() {    
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {    
                    try {
                        if (isReplayingClick) return;    

                        final Intent intent = (Intent) param.args[4];    
                        if (intent == null || intent.getBooleanExtra(KEY_HANDLED, false)) return;    
                            
                        String target = "";    
                        if (intent.getComponent() != null) target = intent.getComponent().getClassName();    
                        else if (intent.getAction() != null) target = intent.getAction();    
                            
                        boolean isTarget = target.contains("TextPreviewActivity") ||     
                                           target.contains("QQGalleryActivity") ||    
                                           target.contains("PhotoPreviewActivity") ||    
                                           target.contains("AIOGalleryActivity") ||    
                                           target.contains("FileBrowserActivity");

                        if (isTarget) {    
                            if (isDialogShowing) {    
                                param.setResult(null);    
                                return;    
                            }    

                            Bundle extras = intent.getExtras();    
                            if (extras != null) {    
                                long msgId = extras.getLong("realMsgId", 0);    
                                if (msgId == 0) msgId = extras.getLong("msgId", 0);    
                                if (msgId == 0) msgId = extras.getLong("uniseq", 0);    
                                int chatType = extras.getInt("nt_chat_type", 0);    
                                if (chatType == 0) chatType = extras.getInt("uintype", 0);    
                                    
                                String peerUid = extras.getString("key_bundle_nt_peeruid");    
                                if (peerUid == null) peerUid = extras.getString("peerUid", "");    
                                if (peerUid == null) peerUid = extras.getString("uin", "");    
                                if (msgId != 0 && peerUid != null && !peerUid.isEmpty()) {    
                                    traceLog("main_log","Intent拦截: MsgId=" + msgId);    
                                    param.setResult(null);     
                                    Activity act = (Activity) param.args[3];    
                                    if (act == null) act = QQCurrentEnv.INSTANCE.getActivity();    
                                    final Activity finalAct = act;    
                                    isDialogShowing = true;    
                                    fetchRealMsgRecord(msgId, chatType, peerUid, new MsgLoadedCallback() {    
                                        public void onLoaded(MsgData msgData) {     
                                            showActionDialog(finalAct, msgData, intent, null);     
                                        }    
                                    });    
                                } else {    
                                    intent.putExtra(KEY_HANDLED, true);    
                                }    
                            }    
                        }    
                    } catch (Throwable t) {    
                        traceLog("main_log","Intent Hook异常: " + t.getMessage());    
                    }    
                }    
            }    
        );    
        HOOK_REGISTRY.add(h1);    

    } catch (Throwable t) {     
        traceLog("main_log","安装失败: " + t.getMessage());    
        Toast("Hook加载失败: " + t.getMessage());    
    }
}
