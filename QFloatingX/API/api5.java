/** 标记 Intent 防止递归 Hook */
private final String KEY_HANDLED = "qfun_script_handled";

/** 全局弹窗显示锁，防止多重弹窗 */
private volatile boolean isDialogShowing = false;

/** 原功能重放标记，用于回旋镖逻辑 */
private volatile boolean isReplayingClick = false;

/** 图片内存缓存 (Url -> Bitmap) */
private final HashMap picimageCache = new HashMap();

/** 原始特殊文本集合 (用于精准渲染 @ 和表情) */
private final HashSet validSpecialTexts = new HashSet();

/** 全局 Hook 注册表，用于一键卸载 */
private final List HOOK_REGISTRY = new ArrayList();

/** 滑动菜单状态池 [0:Popup, 1:Slider, 2:Centers, 4:TextViews, 5:BaseInfo, 7:UpdateRunnable, 8:Root] */
private final Object[] WHEEL_STATE = new Object[10];

/** 双击检测变量：上次点击时间 */
private volatile long lastClickTime = 0L;

/** 双击检测变量：双击时间阈值 (ms) */
private static final long DOUBLE_CLICK_THRESHOLD = 300L;

/** 双击检测变量：单击延迟执行时间 (ms) */
private static final long SINGLE_CLICK_DELAY = 350L;

/** 双击监听器引用，用于卸载 */
private AIOViewUpdateListener doubleClickListener = null;

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
                    uiHandler.post(new Runnable() { public void run() { Toast("内核服务未就绪"); isDialogShowing = false; } });
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
                                uiHandler.post(new Runnable() { public void run() { if (callback != null) callback.onLoaded(msgData); } });
                            } catch (Throwable t) {
                                traceLog("main_log","MsgData构造失败");
                                isDialogShowing = false;
                            }
                        } else {
                            uiHandler.post(new Runnable() { public void run() { Toast("消息不存在"); isDialogShowing = false; } });
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
void 复读(data){
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
                    uiHandler.post(new Runnable() { public void run() { Toast(tips); } });
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
                uiHandler.post(new Runnable() { public void run() { Toast(tips); } });
            } catch (final Throwable e) {
                uiHandler.post(new Runnable() { public void run() { Toast("失败: " + e.getMessage()); } });
            }
        }
    });
}

/**
 * 带间隔的多次发送（非阻塞，使用 postDelayed 递归链）
 * <p>
 * 通过 uiHandler.postDelayed 实现每条消息之间的等待，
 * 不阻塞任何线程。每次发送结束后在主线程调度下一次。
 * </p>
 *
 * @param data     消息数据对象
 * @param newText  新文本内容，null 表示原样复读
 * @param count    总发送次数
 * @param delayMs  每次发送之间的间隔（毫秒）
 */
void doMultiSendWithDelay(final MsgData data, final String newText, final int count, final int delayMs) {
    if (count <= 0 || data == null) return;
    final int[] sent = {0};

    final Runnable[] chain = new Runnable[1];
    chain[0] = new Runnable() {
        public void run() {
            if (sent[0] >= count) {
                final String tips = (newText != null ? "发送" : "复读")
                        + " x" + count + " 完成";
                uiHandler.post(new Runnable() { public void run() { Toast(tips); } });
                return;
            }
            /** 在线程池内执行实际发送，避免主线程网络操作 */
            final int idx = sent[0];
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        MsgRecord originalRecord = data.data;
                        if (newText == null && !isTextOnlyMsg(originalRecord.elements)) {
                            String targetUin = String.valueOf(originalRecord.peerUin);
                            forwardViaServer(originalRecord, targetUin);
                        } else {
                            final ArrayList sendElements = new ArrayList();
                            if (newText != null) {
                                parseTextToElements(newText, sendElements, originalRecord, data.type);
                            } else {
                                if (originalRecord.elements != null) sendElements.addAll(originalRecord.elements);
                            }
                            IMsgService msgService = (IMsgService) QRoute.api(IMsgService.class);
                            if (msgService != null) {
                                String targetUid = originalRecord.peerUid;
                                if (targetUid == null || targetUid.isEmpty()) {
                                    if (originalRecord.chatType == 2) targetUid = String.valueOf(originalRecord.peerUin);
                                    else targetUid = FriendTool.INSTANCE.getUidFromUin(String.valueOf(originalRecord.peerUin));
                                }
                                Contact contact = new Contact(originalRecord.chatType, targetUid, "");
                                msgService.sendMsg(contact, sendElements, null);
                            }
                        }
                    } catch (Throwable ignored) {}
                    /** 发送完成后，主线程调度下一条 */
                    uiHandler.postDelayed(new Runnable() {
                        public void run() {
                            sent[0]++;
                            chain[0].run();
                        }
                    }, delayMs);
                }
            });
        }
    };
    chain[0].run();
}


 /*
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
                    if (callback != null) uiHandler.post(callback);
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
    // traceLog("main_log","Call makeImage()");
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
 * 显示滑动选择器
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
PopupWindow showStyleWheelSelector(
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
    uiHandler.post(physicsUpdate);
    return popup;
}

/**
 * 显示操作对话框
 *
 * @param activity       Activity上下文
 * @param msgData        消息数据对象
 * @param targetView     目标视图（用于原功能回放）
 * @param originalIntent 原始Intent（用于原功能回放）
 */
void showActionDialog(final Activity activity, final MsgData msgData, final View targetView, final Intent originalIntent) {
    if (activity == null || activity.isFinishing()) {
        isDialogShowing = false;
        return;
    }

    final Object finaldata = msgData;

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
                    WHEEL_STATE[0] = showStyleWheelSelector(activity, actionBtn, currentModeIndex, modeNames, actionBtn, btnTv, event);

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
                                    showEncryptDecryptDialog(activity, finaldata);
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

    uiHandler.post(new Runnable() { public void run() { applySpans(editText, true); } });
    LinearLayout btm = new LinearLayout(activity);
    btm.setPadding(0, dp(activity, 4), 0, 0);
    FrameLayout menuBtn = createButton(activity, "更多", "#B3007AFF", Color.WHITE, new Runnable() {
        public void run() {
            dialog.dismiss();
            菜单(finaldata);
        }
    });
    FrameLayout origBtn = createButton(activity, "原功能", "#F2F2F7", Color.parseColor("#B3007AFF"), new Runnable() {
        public void run() {
            dialog.dismiss();
            
            /**
             * 原功能回放逻辑
             * 优先级: Intent > View.performClick()
             */
            isReplayingClick = true;
            try {
                /** 方式1: 如果有原始Intent，直接启动 */
                if (originalIntent != null) {
                    try {
                        originalIntent.putExtra(KEY_HANDLED, true);
                        activity.startActivity(originalIntent);
                        traceLog("dblclick_log", "原功能Intent启动成功");
                    } catch (Exception e) {
                        traceLog("dblclick_log", "原功能Intent启动异常: " + e.getMessage());
                    }
                }
                /** 方式2: 如果没有Intent，尝试View点击 */
                if (originalIntent == null && targetView != null) {
                    boolean clicked = false;
                    
                    /** 尝试 callOnClick */
                    try {
                        clicked = targetView.callOnClick();
                        traceLog("dblclick_log", "原功能 callOnClick: " + clicked);
                    } catch (Exception e) {}
                    
                    /** 尝试 performClick */
                    if (!clicked) {
                        try {
                            clicked = targetView.performClick();
                            traceLog("dblclick_log", "原功能 performClick: " + clicked);
                        } catch (Exception e) {}
                    }
                    
                    /** 尝试子视图点击 */
                    if (!clicked && targetView instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) targetView;
                        for (int i = 0; i < vg.getChildCount(); i++) {
                            View child = vg.getChildAt(i);
                            if (child != null && child.isClickable()) {
                                try {
                                    child.performClick();
                                    traceLog("dblclick_log", "原功能子视图点击成功");
                                    break;
                                } catch (Exception e) {}
                            }
                        }
                    }
                }
            } catch (Exception e) {
                traceLog("dblclick_log", "原功能回放异常: " + e.getMessage());
            } finally {
                uiHandler.postDelayed(new Runnable() {
                    public void run() { isReplayingClick = false; }
                }, 500);
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
 * @param delayMs   发送间隔（毫秒），0 表示连续发送
 * @param parent    父对话框（可为 null）
 */
void showBigCountConfirm(Activity activity, final MsgData data, final String text, final int count, final int delayMs, final Dialog parent) {
    boolean isDark = isThemeDark(activity);
    int textColor   = isDark ? UI_COLOR_TEXT_DARK    : UI_COLOR_TEXT_LIGHT;
    int subColor    = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
    int errorColor  = Color.parseColor("#FFE53935");

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 8));

    TextView title = new TextView(activity);
    title.setText("⚠ 高频警告");
    title.setTextSize(17);
    title.setTextColor(errorColor);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 0, 0, dp(activity, 12));
    root.addView(title);

    TextView msg = new TextView(activity);
    msg.setText("即将发送 " + count + " 条消息，间隔 " + delayMs + "ms\n"
            + "高频发送可能导致账号风控，确定要继续吗？");
    msg.setTextSize(14);
    msg.setTextColor(textColor);
    msg.setPadding(0, 0, 0, dp(activity, 4));
    root.addView(msg);

    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.RIGHT);
    btnRow.setPadding(0, dp(activity, 16), 0, dp(activity, 4));

    TextView cancelBtn = new TextView(activity);
    cancelBtn.setText("取消");
    cancelBtn.setTextSize(14);
    cancelBtn.setTextColor(subColor);
    cancelBtn.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 16), dp(activity, 10));

    TextView confirmBtn = new TextView(activity);
    confirmBtn.setText("继续发送");
    confirmBtn.setTextSize(14);
    confirmBtn.setTextColor(errorColor);
    confirmBtn.setTypeface(null, Typeface.BOLD);
    confirmBtn.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 4), dp(activity, 10));

    btnRow.addView(cancelBtn);
    btnRow.addView(confirmBtn);
    root.addView(btnRow);

    final android.app.AlertDialog[] ref = new android.app.AlertDialog[1];

    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (ref[0] != null) ref[0].dismiss();
        }
    });

    confirmBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (ref[0] != null) ref[0].dismiss();
            if (parent != null) parent.dismiss();
            if (delayMs == 0) {
                doMultiSend(data, text, count);
            } else {
                doMultiSendWithDelay(data, text, count, delayMs);
            }
        }
    });

    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
            activity,
            isDark ? android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK
                   : android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(root);
    ref[0] = builder.create();
    ref[0].show();
    applyUiTheme(activity, ref[0]);
}

/**
 * 多次复读/发送配置弹窗
 * <p>
 * 支持设置发送次数和消息间隔（毫秒），间隔通过 postDelayed 递归实现，
 * 不阻塞线程。弹窗样式自适应系统深色/浅色主题（使用 api.txt 全局颜色变量）
 * 输入框背景使用 uitools.txt 的 createInputBg() 实现焦点高亮。
 * </p>
 *
 * @param activity     Activity上下文
 * @param data         消息数据对象
 * @param currentText  当前编辑框文本，null 表示原样复读
 */
void showRepeatCountDialog(final Activity activity, final MsgData data, final String currentText) {
    if (activity == null || activity.isFinishing()) return;

    boolean isDark = isThemeDark(activity);
    int textColor    = isDark ? UI_COLOR_TEXT_DARK     : UI_COLOR_TEXT_LIGHT;
    int subColor     = isDark ? UI_COLOR_SUBTEXT_DARK  : UI_COLOR_SUBTEXT_LIGHT;
    int accentColor  = isDark ? UI_COLOR_ACCENT_DARK   : UI_COLOR_ACCENT_LIGHT;
    int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
    int strokeColor  = isDark ? UI_COLOR_STROKE_DARK   : UI_COLOR_STROKE_LIGHT;

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 8));

    /** ── 标题 ── */
    TextView title = new TextView(activity);
    title.setText(currentText != null ? "多次发送" : "多次复读");
    title.setTextSize(17);
    title.setTextColor(textColor);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 0, 0, dp(activity, 16));
    root.addView(title);

    /** ── 次数输入 ── */
    TextView countLabel = new TextView(activity);
    countLabel.setText("发送次数");
    countLabel.setTextSize(13);
    countLabel.setTextColor(subColor);
    countLabel.setPadding(dp(activity, 2), 0, 0, dp(activity, 4));
    root.addView(countLabel);

    final EditText countInput = new EditText(activity);
    countInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    countInput.setHint("例如：5");
    countInput.setHintTextColor(subColor);
    countInput.setTextColor(textColor);
    countInput.setTextSize(14);
    countInput.setText("5");
    countInput.setSingleLine(true);
    countInput.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    countInput.setBackground(createInputBg(activity, inputBgColor, strokeColor, accentColor));
    root.addView(countInput, new LinearLayout.LayoutParams(-1, -2));

    /** ── 次数提示 ── */
    TextView countTip = new TextView(activity);
    countTip.setText("次数过多可能会造成刷屏");
    countTip.setTextSize(11);
    countTip.setTextColor(adjustAlpha(textColor, 0.45f));
    countTip.setPadding(dp(activity, 2), dp(activity, 4), 0, dp(activity, 14));
    root.addView(countTip);

    /** ── 间隔输入 ── */
    TextView delayLabel = new TextView(activity);
    delayLabel.setText("发送间隔（毫秒）");
    delayLabel.setTextSize(13);
    delayLabel.setTextColor(subColor);
    delayLabel.setPadding(dp(activity, 2), 0, 0, dp(activity, 4));
    root.addView(delayLabel);

    final EditText delayInput = new EditText(activity);
    delayInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    delayInput.setHint("例如：500");
    delayInput.setHintTextColor(subColor);
    delayInput.setTextColor(textColor);
    delayInput.setTextSize(14);
    delayInput.setSingleLine(true);
    delayInput.setText("500");
    delayInput.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    delayInput.setBackground(createInputBg(activity, inputBgColor, strokeColor, accentColor));
    root.addView(delayInput, new LinearLayout.LayoutParams(-1, -2));

    /** ── 间隔提示 ── */
    TextView delayTip = new TextView(activity);
    delayTip.setText("每条消息之间的等待时间，填 0 无间隔\n"
            + "间隔过短可能触发风控，建议 ≥ 300ms");
    delayTip.setTextSize(11);
    delayTip.setTextColor(adjustAlpha(textColor, 0.45f));
    delayTip.setPadding(dp(activity, 2), dp(activity, 4), 0, dp(activity, 6));
    root.addView(delayTip);

    /** ── 按钮行 ── */
    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.RIGHT);
    btnRow.setPadding(0, dp(activity, 16), 0, dp(activity, 4));

    TextView cancelBtn = new TextView(activity);
    cancelBtn.setText("取消");
    cancelBtn.setTextSize(14);
    cancelBtn.setTextColor(subColor);
    cancelBtn.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 16), dp(activity, 10));

    final TextView confirmBtn = new TextView(activity);
    confirmBtn.setText("开始发送");
    confirmBtn.setTextSize(14);
    confirmBtn.setTextColor(accentColor);
    confirmBtn.setTypeface(null, Typeface.BOLD);
    confirmBtn.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 4), dp(activity, 10));

    btnRow.addView(cancelBtn);
    btnRow.addView(confirmBtn);
    root.addView(btnRow);

    final android.app.AlertDialog[] ref = new android.app.AlertDialog[1];

    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (ref[0] != null) ref[0].dismiss();
        }
    });

    confirmBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String countStr = countInput.getText().toString().trim();
            String delayStr = delayInput.getText().toString().trim();

            if (countStr.isEmpty()) {
                Toast("请输入发送次数");
                return;
            }

            int count;
            int delayMs;
            try {
                count = Integer.parseInt(countStr);
            } catch (Exception e) {
                Toast("次数格式错误");
                return;
            }
            try {
                delayMs = delayStr.isEmpty() ? 500 : Integer.parseInt(delayStr);
            } catch (Exception e) {
                Toast("间隔格式错误");
                return;
            }

            if (count <= 0) {
                Toast("次数必须大于 0");
                return;
            }
            if (delayMs < 0) {
                Toast("间隔不能为负数");
                return;
            }

            if (count > 10) {
                /** 高频二次确认 */
                if (ref[0] != null) ref[0].dismiss();
                showBigCountConfirm(activity, data, currentText, count, delayMs, null);
            } else {
                if (ref[0] != null) ref[0].dismiss();
                if (delayMs == 0) {
                    doMultiSend(data, currentText, count);
                } else {
                    doMultiSendWithDelay(data, currentText, count, delayMs);
                }
            }
        }
    });

    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
            activity,
            isDark ? android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK
                   : android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(root);
    ref[0] = builder.create();
    ref[0].show();
    applyUiTheme(activity, ref[0]);
}

/**
 * 执行原功能回放（延迟执行单击操作）
 * 参考复读功能的 performRepeat 实现
 *
 * @param layout     消息布局视图
 * @param msgRecord  消息记录对象
 */
void performOriginalClick(final View layout, final MsgRecord msgRecord) {
    traceLog("dblclick_log", "执行原功能回放: msgId=" + msgRecord.msgId);
    isReplayingClick = true;
    try {
        /**
         * 尝试多种方式触发原点击事件
         * 1. callOnClick() - 直接调用 OnClickListener
         * 2. performClick() - 模拟点击事件
         */
        boolean clicked = false;
        
        /** 方式1: callOnClick 直接调用 OnClickListener */
        try {
            clicked = layout.callOnClick();
            traceLog("dblclick_log", "callOnClick 结果: " + clicked);
        } catch (Exception e) {
            traceLog("dblclick_log", "callOnClick 异常: " + e.getMessage());
        }
        
        /** 方式2: 如果 callOnClick 失败，尝试 performClick */
        if (!clicked) {
            try {
                clicked = layout.performClick();
                traceLog("dblclick_log", "performClick 结果: " + clicked);
            } catch (Exception e) {
                traceLog("dblclick_log", "performClick 异常: " + e.getMessage());
            }
        }
        
        /** 方式3: 尝试触发子视图的点击 */
        if (!clicked && layout instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) layout;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child != null && child.isClickable()) {
                    try {
                        child.performClick();
                        traceLog("dblclick_log", "子视图 performClick 成功");
                        clicked = true;
                        break;
                    } catch (Exception e) {}
                }
            }
        }
        
        traceLog("dblclick_log", "原功能回放完成: " + (clicked ? "成功" : "失败"));
    } catch (Exception e) {
        traceLog("dblclick_log", "原功能回放异常: " + e.getMessage());
    } finally {
        uiHandler.postDelayed(new Runnable() {
            public void run() { isReplayingClick = false; }
        }, 500);
    }
}

/**
 * 处理消息项双击事件（弹出功能弹窗）
 * 参考复读功能的 performRepeat 实现
 *
 * @param layout     消息布局视图
 * @param msgRecord  消息记录对象
 */
void performDoubleClick(final View layout, final MsgRecord msgRecord) {
    traceLog("dblclick_log", "双击触发: msgId=" + msgRecord.msgId);
    if (isDialogShowing) {
        traceLog("dblclick_log", "弹窗已显示，跳过");
        return;
    }
    isDialogShowing = true;
    Activity act = getNowActivity();
    if (act != null) {
        try {
            final MsgData msgData = new MsgData(msgRecord);
            showActionDialog(act, msgData, layout, null);
        } catch (Throwable e) {
            isDialogShowing = false;
            traceLog("dblclick_log", "MsgData构造失败: " + e.getMessage());
        }
    } else {
        isDialogShowing = false;
        qqToast(1, "未找到当前Activity");
    }
}

/**
 * 初始化双击监听器（注册到OnAIOViewUpdate）
 * 参考复读功能的实现方式，完全拦截消息视图的点击事件
 */
void initDoubleClickListener() {
    if (doubleClickListener != null) {
        traceLog("dblclick_log", "双击监听器已存在，跳过初始化");
        return;
    }
    doubleClickListener = new AIOViewUpdateListener() {
        public void onUpdate(View layout, MsgRecord msgRecord) {
            try {
                if (layout == null || msgRecord == null) return;
                
                /**
                 * 向上遍历视图层级，找到覆盖完整消息气泡的可点击根视图
                 * onUpdate 回调的 layout 仅为消息内容区域，气泡原始点击监听器
                 * 挂载在某个父 ViewGroup 上，触摸监听必须设置在那里才能完整拦截
                 */
                View bubbleView = layout;
                ViewParent cursor = layout.getParent();
                while (cursor instanceof ViewGroup) {
                    ViewGroup parentVg = (ViewGroup) cursor;
                    String parentName = parentVg.getClass().getSimpleName();
                    /** 到达列表容器即停止，不再向上 */
                    if (parentName.contains("RecyclerView") ||
                        parentName.contains("ListView") ||
                        parentName.contains("ScrollView")) {
                        break;
                    }
                    /**
                     * 找到气泡容器：
                     * 根据布局分析，QQ 消息气泡容器（@+id/ona）为
                     * longClickable=true / clickable=false，
                     * 因此必须同时检测 isLongClickable()
                     */
                    if (parentVg.isClickable() || parentVg.isLongClickable() || parentVg.hasOnClickListeners()) {
                        bubbleView = parentVg;
                        break;
                    }
                    cursor = parentVg.getParent();
                }
                
                /** 使用字面量.equals() 规避父视图 tag 非 String 时的 ClassCastException */
                if ("dblclick_hooked".equals(bubbleView.getTag())) {
                    return;
                }
                bubbleView.setTag("dblclick_hooked");
                traceLog("dblclick_log", "绑定双击监听: msgId=" + msgRecord.msgId);
                
                final View finalBubble = bubbleView;
                
                /**
                 * 完全拦截消息气泡的触摸事件
                 * 所有事件都返回 true，阻止事件继续向下传递
                 */
                finalBubble.setOnTouchListener(new View.OnTouchListener() {
                    private long downTime = 0L;
                    private float downX = 0f;
                    private float downY = 0f;
                    private boolean isMoved = false;
                    
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = event.getActionMasked();
                        
                        switch (action) {
                            case MotionEvent.ACTION_DOWN:
                                downTime = System.currentTimeMillis();
                                downX = event.getRawX();
                                downY = event.getRawY();
                                isMoved = false;
                                traceLog("dblclick_log", "ACTION_DOWN: msgId=" + msgRecord.msgId);
                                return true;
                                
                            case MotionEvent.ACTION_MOVE:
                                float dx = Math.abs(event.getRawX() - downX);
                                float dy = Math.abs(event.getRawY() - downY);
                                if (dx > 20 || dy > 20) {
                                    isMoved = true;
                                    traceLog("dblclick_log", "检测到滑动，取消点击检测");
                                }
                                return true;
                                
                            case MotionEvent.ACTION_UP:
                                if (isMoved) {
                                    traceLog("dblclick_log", "滑动事件，忽略");
                                    return true;
                                }
                                
                                long now = System.currentTimeMillis();
                                long clickDuration = now - downTime;
                                
                                /**
                                 * 过滤长按和误触
                                 */
                                if (clickDuration > 500 || clickDuration < 50) {
                                    traceLog("dblclick_log", "长按或误触，忽略: duration=" + clickDuration);
                                    return true;
                                }
                                
                                /**
                                 * 双击检测逻辑
                                 */
                                long timeDiff = now - lastClickTime;
                                traceLog("dblclick_log", "ACTION_UP: timeDiff=" + timeDiff + "ms");
                                
                                if (timeDiff < DOUBLE_CLICK_THRESHOLD) {
                                    /**
                                     * 双击检测成功 -> 弹出自定义弹窗
                                     */
                                    lastClickTime = 0L;
                                    performDoubleClick(finalBubble, msgRecord);
                                } else {
                                    /**
                                     * 单击 -> 延迟后执行原功能回放
                                     * 如果在延迟期间发生双击，则取消原功能回放
                                     */
                                    lastClickTime = now;
                                    uiHandler.postDelayed(new Runnable() {
                                        public void run() {
                                            /**
                                             * 检查是否在延迟期间发生了双击
                                             * 如果 lastClickTime 被重置为 0，说明发生了双击，不执行原功能
                                             */
                                            if (lastClickTime != 0L) {
                                                performOriginalClick(finalBubble, msgRecord);
                                            }
                                        }
                                    }, SINGLE_CLICK_DELAY);
                                }
                                return true;
                                
                            case MotionEvent.ACTION_CANCEL:
                                traceLog("dblclick_log", "ACTION_CANCEL");
                                return true;
                                
                            default:
                                return true;
                        }
                    }
                });
            } catch (Throwable e) {
                traceLog("dblclick_log", "双击监听异常: " + e.getMessage());
            }
        }
    };
    OnAIOViewUpdate.INSTANCE.addListener(doubleClickListener);
    traceLog("dblclick_log", "双击监听器已注册");
}

/**
 * 卸载所有已注册的Hook
 */
void uninstallQFunHooks() {
    if (HOOK_REGISTRY.isEmpty() && doubleClickListener == null) return;
    for (int i = 0; i < HOOK_REGISTRY.size(); i++) {
        try {
            ((XC_MethodHook.Unhook) HOOK_REGISTRY.get(i)).unhook();
        } catch (Exception e) {}
    }
    if (doubleClickListener != null) {
        OnAIOViewUpdate.INSTANCE.removeListener(doubleClickListener);
        doubleClickListener = null;
        traceLog("dblclick_log", "双击监听器已移除");
    }
    HOOK_REGISTRY.clear();
    traceLog("main_log","Hook 已卸载");
}

/**
 * 安装QFun Hook拦截器
 */
void installQFunHooks() {
    uninstallQFunHooks();
    initDoubleClickListener();
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
                                    final Intent finalIntent = intent;
                                    isDialogShowing = true;
                                    fetchRealMsgRecord(msgId, chatType, peerUid, new MsgLoadedCallback() {
                                        public void onLoaded(MsgData msgData) {
                                            showActionDialog(finalAct, msgData, null, finalIntent);
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
