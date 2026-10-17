private volatile boolean isDialogShowing = false;
private volatile boolean isReplayingClick = false;

private final ConcurrentHashMap picimageCache = new ConcurrentHashMap();

private final HashSet validSpecialTexts = new HashSet();

private final Object[] WHEEL_STATE = new Object[10];

private final String[] rkeyCache = new String[]{"", ""};
private final long[] rkeyCacheTime = new long[]{0L, 0L};

Object trGetOcrHandler() {
    try {
        Class handlerClass = ClassUtils.INSTANCE.getModuleClassLoader()
            .loadClass("com.tencent.mobileqq.ocr.api.impl.OCRHandler");
        try {
        } catch (Throwable ignore) {}
        if (app == null) {
            try {
                Object mobileQQ = Class.forName("mqq.app.MobileQQ").getField("sMobileQQ").get(null);
                app = mobileQQ.getClass()
                    .getMethod("waitAppRuntime", Class.forName("mqq.app.BaseActivity"))
                    .invoke(mobileQQ, new Object[]{null});
            } catch (Throwable ignore) {}
        }
        if (app == null) return null;
        java.lang.reflect.Method m = app.getClass().getMethod("getBusinessHandler", String.class);
        m.setAccessible(true);
        return m.invoke(app, "com.tencent.mobileqq.ocr.api.impl.OCRHandler");
    } catch (Throwable t) {
        traceLog("api5_log", "[翻译] 获取OCRHandler失败: " + t);
        return null;
    }
}

String getFullPicUrl(String url, int chatType) {
    if (url == null || url.isEmpty()) return "";
    if (url.startsWith("http")) return url;
    String domain = "https://multimedia.nt.qq.com.cn";
    if (!url.startsWith("/")) url = "/" + url;
    int cacheIdx = (chatType == 1) ? 0 : 1;
    String rkey = "";
    long nowR = System.currentTimeMillis();
    if (rkeyCacheTime[cacheIdx] != 0L && nowR - rkeyCacheTime[cacheIdx] < 60000L) {
        rkey = rkeyCache[cacheIdx];
    } else {
        try {
            rkey = (chatType == 1) ? OnGetRKey.INSTANCE.getFriendRkey() : OnGetRKey.INSTANCE.getGroupRkey();
        } catch (Throwable t) {
            traceLog("api5_log", "[getFullPicUrl] RKey获取失败: " + t.getMessage());
        }
        rkeyCache[cacheIdx] = rkey;
        rkeyCacheTime[cacheIdx] = nowR;
    }
    return domain + url + rkey;
}

interface MsgLoadedCallback {
    void onLoaded(MsgData msgData);
}

void fetchRealMsgRecord(final long msgId, final int chatType, final String peerUid, final MsgLoadedCallback callback) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                if (msgId == 0) return;
                IKernelMsgService kernel = QQCurrentEnv.INSTANCE.getKernelMsgService();
                if (kernel == null) {
                    uiHandler.post(new Runnable() {
                        public void run() { Toast("内核服务未就绪"); isDialogShowing = false; }
                    });
                    return;
                }
                Contact contact = new Contact(chatType, peerUid, "");
                ArrayList ids = new ArrayList();
                ids.add(msgId);
                kernel.getMsgsByMsgId(contact, ids, new IMsgOperateCallback() {
                    public void onResult(int res, String err, ArrayList list) {
                        if (list != null && !list.isEmpty()) {
                            try {
                                MsgRecord realRecord = (MsgRecord) list.get(0);
                                final MsgData msgData = new MsgData(realRecord);
                                uiHandler.post(new Runnable() {
                                    public void run() {
                                        if (callback != null) callback.onLoaded(msgData);
                                    }
                                });
                            } catch (Throwable t) {
                                traceLog("api5_log", "[fetchRealMsgRecord] MsgData构造失败");
                                isDialogShowing = false;
                            }
                        } else {
                            uiHandler.post(new Runnable() {
                                public void run() { Toast("消息不存在"); isDialogShowing = false; }
                            });
                        }
                    }
                });
            } catch (Throwable t) {
                traceLog("api5_log", "[fetchRealMsgRecord] FetchMsg异常: " + t.getMessage());
                isDialogShowing = false;
            }
        }
    });
}

void forwardViaServer(Object msgRecordObj, String targetUin) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                if (!(msgRecordObj instanceof MsgRecord)) return;
                MsgRecord msgRecord = (MsgRecord) msgRecordObj;
                IKernelMsgService kernel = QQCurrentEnv.INSTANCE.getKernelMsgService();
                if (kernel == null) return;
                String guildId = msgRecord.guildId == null ? "" : msgRecord.guildId;
                List msgIds = new ArrayList();
                msgIds.add(msgRecord.msgId);
                Contact srcContact = new Contact(msgRecord.chatType, msgRecord.peerUid, guildId);
                int targetChatType = msgRecord.chatType;
                String targetUid = msgRecord.peerUid;
                String targetGuild = guildId;
                if (targetUin == null || !targetUin.equals(String.valueOf(msgRecord.peerUin))) {
                    targetChatType = isFriend(targetUin) ? 1 : 2;
                    targetUid = targetChatType == 1 ? getUidFromUin(targetUin) : targetUin;
                    targetGuild = "";
                }
                Contact dstContact = new Contact(targetChatType, targetUid, targetGuild);
                List dstContacts = new ArrayList();
                dstContacts.add(dstContact);
                kernel.forwardMsg(msgIds, srcContact, dstContacts, msgRecord.msgAttrs, null);
            } catch (Throwable ignored) { }
        }
    });
}

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
    Matcher m = Pattern.compile("\\[pic=(.*?)\\]").matcher(text);
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

void addTextElement(ArrayList list, String content) {
    MsgElement el = new MsgElement();
    el.elementType = 1;
    TextElement te = new TextElement();
    te.content = content;
    te.atType = 0;
    el.textElement = te;
    list.add(el);
}

void 复读(Object data, String newText) {
    doMultiSend(data, newText, 1);
}

boolean sendOneInternal(final MsgData data, final String newText) {
    final MsgRecord originalRecord = data.data;
    final String targetUin = String.valueOf(originalRecord.peerUin);
    if (newText == null) {
        boolean needForward = false;
        int t = originalRecord.msgType;
        if (t == 3 || t == 7) {
            needForward = true;
        } else if (t == 2) {
            needForward = true;
            if (originalRecord.elements != null) {
                for (int i = 0; i < originalRecord.elements.size(); i++) {
                    MsgElement el = (MsgElement) originalRecord.elements.get(i);
                    if (el != null && el.textElement != null && el.textElement.atType == 2) {
                        needForward = false;
                        break;
                    }
                }
            }
        }
        if (needForward) {
            forwardViaServer(originalRecord, targetUin);
            return true;
        }
    }
    final ArrayList sendElements = new ArrayList();
    if (newText != null) {
        parseTextToElements(newText, sendElements, originalRecord, data.type);
    } else if (originalRecord.elements != null) {
        sendElements.addAll(originalRecord.elements);
    }
    if (sendElements.isEmpty()) return false;
    IKernelMsgService kernel = QQCurrentEnv.INSTANCE.getKernelMsgService();
    if (kernel == null) return false;
    String targetUid = originalRecord.peerUid;
    if (targetUid == null || targetUid.isEmpty()) {
        if (originalRecord.chatType == 2) targetUid = targetUin;
        else targetUid = FriendTool.INSTANCE.getUidFromUin(targetUin);
    }
    HashMap attrs = originalRecord.msgAttrs;
    if (attrs == null) attrs = new HashMap();
    String guildId = originalRecord.guildId == null ? "" : originalRecord.guildId;
    IOperateCallback callback = new IOperateCallback() {
        public void onResult(int code, String err) {
            if (code != 0 && newText == null) {
                traceLog("api5_log", "[复读] 内核发送失败 code=" + code + " err=" + err
                         + " msgId=" + originalRecord.msgId + " chatType=" + originalRecord.chatType + " peerUid=" + originalRecord.peerUid);
                traceLog("api5_log", "[复读] 失败元素=" + sendElements);
                forwardViaServer(originalRecord, targetUin);
            }
        }
    };
    long msgId = kernel.generateMsgUniqueId(originalRecord.chatType, System.currentTimeMillis());
    kernel.sendMsg(msgId, new Contact(originalRecord.chatType, targetUid, guildId), sendElements, attrs, callback);
    return true;
}

void doMultiSend(final MsgData data, final String newText, final int count) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                if (data == null || data.data == null) {
                    Toast("复读失败：消息数据为空");
                    return;
                }
                boolean allOk = true;
                for (int i = 0; i < count; i++) {
                    if (!sendOneInternal(data, newText)) allOk = false;
                }
                Toast(allOk ? ((newText != null ? "发送" : "复读") + (count > 1 ? " x" + count : "") + " 完成") : "复读失败");
            } catch (final Throwable e) {
                Toast("失败: " + e.getMessage());
            }
        }
    });
}

void doMultiSendWithDelay(final MsgData data, final String newText, final int count, final int delayMs) {
    if (count <= 0 || data == null) return;
    final int[] sent = {0};
    final boolean[] allOk = {true};
    final Runnable[] chain = new Runnable[1];
    chain[0] = new Runnable() {
        public void run() {
            if (sent[0] >= count) {
                Toast(allOk[0] ? ((newText != null ? "发送" : "复读") + " x" + count + " 完成") : "复读失败");
                return;
            }
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try { if (!sendOneInternal(data, newText)) allOk[0] = false; } catch (Throwable ignored) { allOk[0] = false; }
                    uiHandler.postDelayed(new Runnable() {
                        public void run() { sent[0]++; chain[0].run(); }
                    }, delayMs);
                }
            });
        }
    };
    chain[0].run();
}

void downloadImage(final String url, final Runnable callback) {
    if (picimageCache.containsKey(url)) {
        if (callback != null) callback.run();
        return;
    }
    ThreadPool.execute(new Runnable() {
        public void run() {
            InputStream is = null;
            try {
                URL u = new URL(url);
                java.net.URLConnection conn = u.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                Bitmap bmp = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
                if (bmp != null) {
                    if (picimageCache.size() >= 60) {
                        java.util.Iterator evictIt = picimageCache.keySet().iterator();
                        int evictCount = 0;
                        while (evictIt.hasNext() && evictCount < 15) { evictIt.remove(); evictCount++; }
                    }
                    picimageCache.put(url, bmp);
                    if (callback != null) uiHandler.post(callback);
                }
            } catch (Throwable t) {
                traceLog("api5_log", "[downloadImage] 异常: " + t);
            } finally {
                if (is != null) try { is.close(); } catch (Throwable ignore) { }
            }
        }
    });
}

class LinkTagSpan extends ForegroundColorSpan {
    public String url;
    public LinkTagSpan(String url) {
        super(pc("#007AFF"));
        this.url = url;
    }
}

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

    Matcher mPic = Pattern.compile("\\[pic=(.*?)\\]").matcher(text);
    while (mPic.find()) {
        final String url = mPic.group(1);
        final int start = mPic.start();
        final int end = mPic.end();
        boolean isCursorInside = (selStart >= start && selStart <= end) || (selEnd >= start && selEnd <= end);
        if (forceImage || (picimageCache.containsKey(url) && !isCursorInside)) {
            downloadImage(url, new Runnable() {
                public void run() {
                    String cur = et.getText().toString();
                    if (cur.length() >= end && cur.substring(start, end).equals("[pic=" + url + "]")) {
                        Bitmap bmp = (Bitmap) picimageCache.get(url);
                        if (bmp != null) {
                            int h = dp(ctx, 64);
                            int w = (int) ((float) bmp.getWidth() / bmp.getHeight() * h);
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
            if (validText.startsWith("@")) color = pc("#007AFF");
            else if (validText.startsWith("/")) color = pc("#A6FFD700");
            if (color != 0) {
                s.setSpan(new ForegroundColorSpan(color), idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            idx = text.indexOf(validText, end);
        }
    }
}

void setupEditTextTouch(final EditText et) {
    final GestureDetector gestureDetector = new GestureDetector(et.getContext(), new GestureDetector.SimpleOnGestureListener() {
        public boolean onSingleTapUp(MotionEvent e) {
            boolean ret = false;
            try {
                int x = (int) e.getX() - et.getTotalPaddingLeft() + et.getScrollX();
                int y = (int) e.getY() - et.getTotalPaddingTop() + et.getScrollY();
                Layout layout = et.getLayout();
                if (layout != null) {
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
                            ret = true;
                        }
                    }
                }
            } catch (Throwable t) {
                ret = false;
            }
            return ret;
        }
    });
    et.setOnTouchListener(new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            boolean result = false;
            try {
                result = gestureDetector.onTouchEvent(event);
            } catch (Throwable t) {
                result = false;
            }
            return result;
        }
    });
}

View createReplyBox(Context ctx, MsgData msgData) {
    MsgElement replyEl = null;
    if (msgData.data.elements != null) {
        for (int i = 0; i < msgData.data.elements.size(); i++) {
            MsgElement e = (MsgElement) msgData.data.elements.get(i);
            if (e != null && e.elementType == 7) { replyEl = e; break; }
        }
    }
    if (replyEl == null || replyEl.replyElement == null) return null;
    ReplyElement re = replyEl.replyElement;
    LinearLayout replyBox = new LinearLayout(ctx);
    replyBox.setOrientation(LinearLayout.HORIZONTAL);
    replyBox.setBackgroundColor(pc("#F5F5F5"));
    replyBox.setPadding(0, 0, dp(ctx, 8), 0);
    View line = new View(ctx);
    GradientDrawable lineBg = new GradientDrawable();
    lineBg.setColor(pc("#D0D0D0"));
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
        } catch (Throwable t) { }
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
    tvBody.setTextColor(pc("#666666"));
    tvBody.setMaxLines(3);
    tvBody.setEllipsize(TextUtils.TruncateAt.END);

    SpannableStringBuilder ssb = new SpannableStringBuilder();
    boolean hasContent = false;
    if (re.sourceMsgTextElems != null && !re.sourceMsgTextElems.isEmpty()) {
        for (int i = 0; i < re.sourceMsgTextElems.size(); i++) {
            ReplyAbsElement rae = (ReplyAbsElement) re.sourceMsgTextElems.get(i);
            if (rae == null) continue;
            if (rae.textElemContent != null) { ssb.append(rae.textElemContent); hasContent = true; }
            else if (rae.faceElem != null) { ssb.append(rae.faceElem.faceText != null ? rae.faceElem.faceText : "[表情]"); hasContent = true; }
            else if (rae.picElem != null) { ssb.append("[图片]"); hasContent = true; }
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

void 作图(Activity activity, String content) {
    Toast("还是空壳\n" + content);
}

void updateSliderPhysics(float rawDx, boolean isDrag) {
    View slider = (View) WHEEL_STATE[1];
    Runnable updater = (Runnable) WHEEL_STATE[7];
    if (slider == null) return;
    float[] baseInfo = (float[]) WHEEL_STATE[5];
    float targetTrans = baseInfo[1] + rawDx;
    slider.setTranslationX(targetTrans);
    if (updater != null) updater.run();
}

PopupWindow showStyleWheelSelector(
        final Activity activity,
        View anchor,
        final int[] modeIndexRef,
        final String[] modeNames,
        final FrameLayout actionBtn,
        final TextView btnTv,
        MotionEvent initialEvent) {
    final PopupWindow popup = new PopupWindow(activity);
    popup.setBackgroundDrawable(null);
    final boolean wheelDark = isThemeDark(activity);
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
    cardBg.setColor(wheelDark ? pc("#FF2C2C2C") : pc("#F2F2F7"));
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
    sliderBg.setColor(wheelDark ? pc("#FF3C3C3C") : Color.WHITE);
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
        tv.setTextColor(wheelDark ? pc("#FFEFEFEF") : Color.BLACK);
        float centerX = PADDING + (ITEM_W / 2f) + i * (ITEM_W + GAP);
        itemCenters[i] = centerX;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ITEM_W, ITEM_H);
        lp.leftMargin = (int) (centerX - ITEM_W / 2f);
        lp.topMargin = PADDING;
        textContainer.addView(tv, lp);
        textViews[i] = tv;
    }

    int containerW = (ITEM_W * modeNames.length) + (GAP * (modeNames.length - 1)) + PADDING * 2;
    card.addView(textContainer, new FrameLayout.LayoutParams(containerW, ITEM_H + PADDING * 2, Gravity.CENTER));
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
            View s = (View) WHEEL_STATE[1];
            float[] centers = (float[]) WHEEL_STATE[2];
            TextView[] tvs = (TextView[]) WHEEL_STATE[4];
            if (s == null || centers == null) return;

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
                newW = baseW - (int) squash;
            } else if (logicTrans > maxTrans) {
                float overflow = logicTrans - maxTrans;
                float squash = overflow * 0.4f;
                if (squash > baseW * 0.4f) squash = baseW * 0.4f;
                newW = baseW - (int) squash;
                visualTrans = maxTrans + (baseW - newW);
            } else {
                for (int i = 0; i < centers.length - 1; i++) {
                    if (logicCenter >= centers[i] && logicCenter <= centers[i + 1]) {
                        float range = centers[i + 1] - centers[i];
                        float progress = (logicCenter - centers[i]) / range;
                        float stretch = (float) Math.sin(progress * Math.PI) * dp(activity, 10);
                        newW = baseW + (int) stretch;
                        break;
                    }
                }
            }

            if (s.getTranslationX() != visualTrans) s.setTranslationX(visualTrans);
            ViewGroup.LayoutParams lp = s.getLayoutParams();
            if (lp.width != newW) { lp.width = newW; s.setLayoutParams(lp); }

            for (int i = 0; i < centers.length; i++) {
                float dist = Math.abs(logicCenter - centers[i]);
                if (dist < baseW * 0.6f) tvs[i].setTextColor(wheelDark ? pc("#FF8AB4F8") : pc("#007AFF"));
                else tvs[i].setTextColor(wheelDark ? pc("#FFEFEFEF") : Color.BLACK);
            }
        }
    };
    WHEEL_STATE[7] = physicsUpdate;
    uiHandler.post(physicsUpdate);
    return popup;
}

void showActionDialog(final Activity activity, final MsgData msgData, final View targetView, final Intent originalIntent) {
    if (msgData == null) {
        isDialogShowing = false;
        return;
    }
    final Activity a = (activity != null && !activity.isFinishing()) ? activity : getNowActivity();
    if (a == null) {
        isDialogShowing = false;
        return;
    }
    uiHandler.post(new Runnable() {
        public void run() {
            try {
                showActionDialogInner(a, msgData, targetView, originalIntent);
            } catch (Throwable e) {
                isDialogShowing = false;
                traceLog("api5_log", "[showActionDialog] 弹窗构建失败: " + e);
                Toast("弹窗打开失败: " + e);
            }
        }
    });
}

void trStartRequest(final Object handler, final String text, final String from, final String to, final TextView resultView, final TextView statusView, final Dialog dialog, final boolean[] busy) {
    if (handler == null) {
        statusView.setText("QQ 翻译服务暂不可用");
        busy[0] = false;
        return;
    }
    if (text == null || text.trim().isEmpty()) {
        statusView.setText("请输入要翻译的内容");
        busy[0] = false;
        return;
    }
    try {
        final Class handlerClass = handler.getClass();
        ClassLoader cl = ClassUtils.INSTANCE.getModuleClassLoader();
        Class bhCls = cl.loadClass("com.tencent.mobileqq.app.BusinessHandler");
        final Method nu = bhCls.getMethod("notifyUI", new Class[]{int.class, boolean.class, Object.class});
        nu.setAccessible(true);

        final Object[] unhookRef = new Object[1];

        XC_MethodHook hook = new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                try {
                    Object thisObj = param.thisObject;
                    if (thisObj == null) return;
                    if (thisObj.getClass().getName().indexOf("OCRHandler") < 0) return;

                    int type = ((Integer) param.args[0]).intValue();
                    if (type != 2) return;
                    boolean success = ((Boolean) param.args[1]).booleanValue();
                    Object data = param.args[2];

                    String translated = null;
                    String error = null;
                    if (!success) error = "翻译失败";

                    if (data instanceof Object[]) {
                        Object[] arr = (Object[]) data;
                        Object result = arr.length > 1 ? arr[1] : (arr.length > 0 ? arr[0] : null);
                        if (result != null) {
                            Class rc = result.getClass();
                            try {
                                java.lang.reflect.Field errField = rc.getDeclaredField("o");
                                errField.setAccessible(true);
                                Object errValue = errField.get(result);
                                if (errValue != null && !"0".equals(errValue.toString())) {
                                    java.lang.reflect.Field msgField = rc.getDeclaredField("p");
                                    msgField.setAccessible(true);
                                    Object msgValue = msgField.get(result);
                                    error = msgValue == null ? ("翻译失败(" + errValue + ")") : msgValue.toString();
                                }
                            } catch (Throwable ignored) {}

                            try {
                                java.lang.reflect.Field listField = rc.getDeclaredField("h");
                                listField.setAccessible(true);
                                Object listObj = listField.get(result);
                                if (listObj instanceof java.util.List) {
                                    java.util.List records = (java.util.List) listObj;
                                    StringBuffer sb = new StringBuffer();
                                    for (int i = 0; i < records.size(); i++) {
                                        Object record = records.get(i);
                                        if (record == null) continue;
                                        Class rr = record.getClass();
                                        java.lang.reflect.Field valueField = rr.getDeclaredField("e");
                                        valueField.setAccessible(true);
                                        Object value = valueField.get(record);
                                        if (value != null) {
                                            if (sb.length() > 0) sb.append("\n");
                                            sb.append(value.toString());
                                        }
                                    }
                                    if (sb.length() > 0) translated = sb.toString();
                                }
                            } catch (Throwable ignored) {}
                        }
                    }

                    if (translated == null && error == null) return;

                    final String finalTranslated = translated;
                    final String finalError = error;

                    try {
                        if (unhookRef[0] != null) ((XC_MethodHook.Unhook) unhookRef[0]).unhook();
                        hookloveList.remove(unhookRef[0]);
                        unhookRef[0] = null;
                    } catch (Throwable ignored) {}

                    uiHandler.post(new Runnable() {
                        public void run() {
                            busy[0] = false;
                            String msg;
                            if (finalTranslated != null) {
                                msg = "翻译完成";
                            } else if (finalError == null) {
                                msg = "没有返回译文";
                            } else if ("auto".equals(from)) {
                                msg = "自动检测失败，请手动选择源语言";
                            } else {
                                msg = finalError;
                            }
                            statusView.setText(msg);
                            if (finalTranslated != null) resultView.setText(finalTranslated);
                        }
                    });
                } catch (Throwable t) {
                    traceLog("api5_log", "[翻译] notifyUI 处理异常: " + t);
                }
            }
        };

        unhookRef[0] = XposedBridge.hookMethod(nu, hook);
        hookloveList.add(unhookRef[0]);

        Method batch = handlerClass.getMethod("batchTranslate", new Class[]{String.class, String.class, String.class});
        batch.invoke(handler, new Object[]{text, from, to});

        uiHandler.postDelayed(new Runnable() {
            public void run() {
                if (unhookRef[0] != null && busy[0]) {
                    busy[0] = false;
                    statusView.setText("翻译超时");
                    try {
                        ((XC_MethodHook.Unhook) unhookRef[0]).unhook();
                        hookloveList.remove(unhookRef[0]);
                        unhookRef[0] = null;
                    } catch (Throwable ignored) {}
                }
            }
        }, 15000);
    } catch (Throwable t) {
        busy[0] = false;
        statusView.setText("启动翻译失败: " + t.getMessage());
        traceLog("api5_log", "[翻译] 请求失败: " + t);
    }
}

PopupWindow showLangPicker(final Activity activity, final ArrayList pairs, final int[] currentIdxRef, final boolean dark, final int textColor, final int popupBg, final Runnable onSelected) {
    final PopupWindow popup = new PopupWindow(activity);
    popup.setBackgroundDrawable(null);
    popup.setOutsideTouchable(true);
    popup.setFocusable(true);
    popup.setTouchable(true);

    LinearLayout container = new LinearLayout(activity);
    container.setOrientation(LinearLayout.VERTICAL);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(popupBg);
    bg.setCornerRadius(dp(activity, 14));
    container.setBackground(bg);
    container.setPadding(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6));

    ScrollView scroll = new ScrollView(activity);
    LinearLayout list = new LinearLayout(activity);
    list.setOrientation(LinearLayout.VERTICAL);

    for (int i = 0; i < pairs.size(); i++) {
        final String[] pair = (String[]) pairs.get(i);
        final int idx = i;
        TextView item = new TextView(activity);
        item.setText(pair[1]);
        item.setTextSize(14);
        item.setTextColor(textColor);
        item.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        GradientDrawable itemBg = new GradientDrawable();
        itemBg.setColor(idx == currentIdxRef[0] ? (dark ? pc("#FF1E3A5F") : pc("#FFD6E6FF")) : Color.TRANSPARENT);
        itemBg.setCornerRadius(dp(activity, 8));
        item.setBackground(itemBg);
        item.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentIdxRef[0] = idx;
                onSelected.run();
                popup.dismiss();
            }
        });
        list.addView(item, new LinearLayout.LayoutParams(-1, -2));
    }
    scroll.addView(list);
    container.addView(scroll);

    int h = pairs.size() * dp(activity, 46) + dp(activity, 12);
    if (h > dp(activity, 360)) h = dp(activity, 360);
    popup.setContentView(container);
    popup.setWidth(dp(activity, 220));
    popup.setHeight(h);
    return popup;
}

void showTranslateDialog(final Activity activity, final MsgData msgData) {
    final boolean dark = isThemeDark(activity);
    final int textColor = dark ? pc("#FFF0F0F0") : pc("#FF202124");
    final int subColor = dark ? pc("#FFB0B0B0") : pc("#FF666666");
    final int surface = dark ? pc("#FF292929") : Color.WHITE;
    final int inputBg = dark ? pc("#FF383838") : pc("#FFF4F5F7");
    final int chipBg = dark ? pc("#FF3A3A3A") : pc("#FFEDEFF2");
    final int popupBg = dark ? pc("#FF2D2D2D") : Color.WHITE;

    ArrayList pairs = new ArrayList();
    try {
        Class configClass = ClassUtils.INSTANCE.getModuleClassLoader()
            .loadClass("com.tencent.mobileqq.ocr.data.OcrConfig");
        java.lang.reflect.Field namesField = configClass.getDeclaredField("d");
        namesField.setAccessible(true);
        Object namesObj = namesField.get(null);
        java.util.LinkedHashSet codeSet = new java.util.LinkedHashSet();
        if (namesObj instanceof java.util.Map) {
            java.util.Map names = (java.util.Map) namesObj;
            java.util.Iterator it = names.keySet().iterator();
            while (it.hasNext()) {
                Object k = it.next();
                if (k != null) codeSet.add(k.toString());
            }
        }
        java.util.ArrayList codes = new java.util.ArrayList(codeSet);
        java.util.Collections.sort(codes);
        if (codes.remove("zh")) codes.add(0, "zh");
        java.lang.reflect.Method getName = configClass.getDeclaredMethod("getDefaultLanguageName", new Class[]{String.class});
        java.util.HashSet seenCode = new java.util.HashSet();
        java.util.HashSet seenName = new java.util.HashSet();
        seenCode.add("auto");
        seenName.add("自动检测");
        pairs.add(new String[]{"auto", "自动检测"});
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i).toString();
            if (seenCode.contains(code)) continue;
            String name = code;
            try {
                Object v = getName.invoke(null, new Object[]{code});
                if (v != null && !v.toString().trim().isEmpty()) name = v.toString();
            } catch (Throwable ignored) {}
            if (seenName.contains(name)) continue;
            seenCode.add(code);
            seenName.add(name);
            pairs.add(new String[]{code, name});
        }
    } catch (Throwable t) {
        traceLog("api5_log", "[翻译] 构建语言表失败: " + t);
        pairs.add(new String[]{"auto", "自动检测"});
        pairs.add(new String[]{"zh", "中文"});
        pairs.add(new String[]{"en", "英语"});
    }
    final ArrayList sourcePairs = pairs;
    final ArrayList[] targetPairsRef = new ArrayList[]{new ArrayList()};
    final int[] srcIdxRef = new int[]{0};
    final int[] tgtIdxRef = new int[]{0};

    final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
    LinearLayout root = new LinearLayout(activity);
    root.setGravity(Gravity.CENTER);
    root.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 20));
    root.setBackgroundColor(pc("#99000000"));
    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(activity, 18), dp(activity, 16), dp(activity, 18), dp(activity, 16));
    GradientDrawable cardBg = new GradientDrawable();
    cardBg.setColor(surface);
    cardBg.setCornerRadius(dp(activity, 18));
    card.setBackground(cardBg);
    root.addView(card, new LinearLayout.LayoutParams(-1, -2));
    root.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });
    card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {} });

    LinearLayout header = new LinearLayout(activity);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = new TextView(activity);
    title.setText("翻译");
    title.setTextSize(18);
    title.setTypeface(null, 1);
    title.setTextColor(textColor);
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
    TextView close = new TextView(activity);
    close.setText("×");
    close.setTextSize(26);
    close.setTextColor(subColor);
    close.setGravity(Gravity.CENTER);
    header.addView(close, new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 36)));
    card.addView(header);

    LinearLayout langRow = new LinearLayout(activity);
    langRow.setGravity(Gravity.CENTER_VERTICAL);
    langRow.setPadding(0, dp(activity, 12), 0, 0);

    final TextView sourceBtn = new TextView(activity);
    sourceBtn.setGravity(Gravity.CENTER);
    sourceBtn.setTextSize(14);
    sourceBtn.setTextColor(textColor);
    sourceBtn.setSingleLine(true);
    sourceBtn.setEllipsize(TextUtils.TruncateAt.END);
    GradientDrawable srcBg = new GradientDrawable();
    srcBg.setColor(chipBg);
    srcBg.setCornerRadius(dp(activity, 22));
    sourceBtn.setBackground(srcBg);
    sourceBtn.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));

    TextView swap = new TextView(activity);
    swap.setText("⇄");
    swap.setTextSize(22);
    swap.setTextColor(pc("#FF1677FF"));
    swap.setGravity(Gravity.CENTER);

    final TextView targetBtn = new TextView(activity);
    targetBtn.setGravity(Gravity.CENTER);
    targetBtn.setTextSize(14);
    targetBtn.setTextColor(textColor);
    targetBtn.setSingleLine(true);
    targetBtn.setEllipsize(TextUtils.TruncateAt.END);
    GradientDrawable tgtBg = new GradientDrawable();
    tgtBg.setColor(chipBg);
    tgtBg.setCornerRadius(dp(activity, 22));
    targetBtn.setBackground(tgtBg);
    targetBtn.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));

    langRow.addView(sourceBtn, new LinearLayout.LayoutParams(0, -2, 1f));
    langRow.addView(swap, new LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 44)));
    langRow.addView(targetBtn, new LinearLayout.LayoutParams(0, -2, 1f));
    card.addView(langRow);

    final Runnable[] refreshTargets = new Runnable[1];
    refreshTargets[0] = new Runnable() {
        public void run() {
            String srcCode = ((String[])sourcePairs.get(srcIdxRef[0]))[0];
            ArrayList targets = new ArrayList();
            try {
                Class configClass = ClassUtils.INSTANCE.getModuleClassLoader()
                    .loadClass("com.tencent.mobileqq.ocr.data.OcrConfig");
                java.lang.reflect.Method method = configClass.getDeclaredMethod("getDefaultSupportLanguages", new Class[]{String.class});
                Object result = method.invoke(null, new Object[]{("auto".equals(srcCode) ? "zh" : srcCode)});
                if (result instanceof java.util.List) {
                    java.util.List list = (java.util.List) result;
                    java.util.HashSet seen = new java.util.HashSet();
                    for (int i = 0; i < list.size(); i++) {
                        Object code = list.get(i);
                        if (code == null) continue;
                        String cs = code.toString();
                        if (cs.equals(srcCode) || seen.contains(cs)) continue;
                        seen.add(cs);
                        targets.add(cs);
                    }
                }
            } catch (Throwable ignored) {}
            if (targets.isEmpty()) {
                if (!"zh".equals(srcCode) && !"auto".equals(srcCode)) {
                    targets.add("zh");
                } else {
                    String[] fb = new String[]{"en","ja","ko","fr","es","it","de","tr","ru","pt","vi","id","ms","th"};
                    for (int i = 0; i < fb.length; i++) targets.add(fb[i]);
                }
            }
            targetPairsRef[0].clear();
            java.util.HashSet seenName2 = new java.util.HashSet();
            for (int i = 0; i < targets.size(); i++) {
                String code = targets.get(i).toString();
                String name = code;
                for (int k = 0; k < sourcePairs.size(); k++) {
                    String[] pp = (String[]) sourcePairs.get(k);
                    if (pp[0].equals(code)) { name = pp[1]; break; }
                }
                if (seenName2.contains(name)) continue;
                seenName2.add(name);
                targetPairsRef[0].add(new String[]{code, name});
            }
            if (tgtIdxRef[0] >= targetPairsRef[0].size()) tgtIdxRef[0] = 0;
            if (targetPairsRef[0].size() > 0) {
                targetBtn.setText(((String[])targetPairsRef[0].get(tgtIdxRef[0]))[1]);
            } else {
                targetBtn.setText("无可用");
            }
        }
    };

    sourceBtn.setText(((String[])sourcePairs.get(0))[1]);
    refreshTargets[0].run();

    final EditText original = new EditText(activity);
    original.setTextColor(textColor);
    original.setHintTextColor(subColor);
    original.setHint("输入或编辑原文");
    original.setMinLines(3);
    original.setMaxLines(6);
    original.setGravity(Gravity.TOP | Gravity.LEFT);
    original.setTextSize(15);
    original.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    GradientDrawable inputDrawable = new GradientDrawable();
    inputDrawable.setColor(inputBg);
    inputDrawable.setCornerRadius(dp(activity, 10));
    original.setBackground(inputDrawable);
    String initialText = "";
    try {
        if (msgData != null && msgData.data != null && msgData.data.elements != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < msgData.data.elements.size(); i++) {
                MsgElement el = (MsgElement) msgData.data.elements.get(i);
                if (el != null && el.textElement != null && el.textElement.content != null) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(el.textElement.content);
                }
            }
            initialText = sb.toString();
        }
    } catch (Throwable ignored) {}
    original.setText(initialText);
    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(-1, -2);
    editParams.topMargin = dp(activity, 12);
    card.addView(original, editParams);

    final EditText translated = new EditText(activity);
    translated.setTextColor(textColor);
    translated.setHintTextColor(subColor);
    translated.setHint("译文（可编辑）");
    translated.setMinLines(3);
    translated.setMaxLines(6);
    translated.setGravity(Gravity.TOP | Gravity.LEFT);
    translated.setTextSize(15);
    translated.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    GradientDrawable resultBg = new GradientDrawable();
    resultBg.setColor(inputBg);
    resultBg.setCornerRadius(dp(activity, 10));
    translated.setBackground(resultBg);
    LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(-1, -2);
    resultParams.topMargin = dp(activity, 10);
    card.addView(translated, resultParams);

    final TextView status = new TextView(activity);
    status.setTextColor(subColor);
    status.setTextSize(12);
    status.setText("选择语言后点击翻译");
    LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
    statusParams.topMargin = dp(activity, 8);
    card.addView(status, statusParams);

    LinearLayout copyRow = new LinearLayout(activity);
    copyRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    copyRow.setPadding(0, dp(activity, 8), 0, 0);
    TextView copyOriginal = createButton(activity, "复制原文", textColor, inputBg, 12f, 8, 14, 7, false, 0, 0, null);
    TextView copyTranslated = createButton(activity, "复制译文", textColor, inputBg, 12f, 8, 14, 7, false, 0, 0, null);
    LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(-2, dp(activity, 36));
    copyParams.leftMargin = dp(activity, 8);
    copyRow.addView(copyOriginal, copyParams);
    LinearLayout.LayoutParams copyResultParams = new LinearLayout.LayoutParams(-2, dp(activity, 36));
    copyResultParams.leftMargin = dp(activity, 8);
    copyRow.addView(copyTranslated, copyResultParams);
    card.addView(copyRow);

    final PopupWindow[] pickerRef = new PopupWindow[1];

    sourceBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (pickerRef[0] != null && pickerRef[0].isShowing()) {
                pickerRef[0].dismiss();
                pickerRef[0] = null;
                return;
            }
            pickerRef[0] = showLangPicker(activity, sourcePairs, srcIdxRef, dark, textColor, popupBg, new Runnable() {
                public void run() {
                    sourceBtn.setText(((String[])sourcePairs.get(srcIdxRef[0]))[1]);
                    tgtIdxRef[0] = 0;
                    refreshTargets[0].run();
                }
            });
            pickerRef[0].showAsDropDown(sourceBtn, 0, dp(activity, 6));
        }
    });

    targetBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (targetPairsRef[0].size() == 0) { Toast("没有可用的目标语言"); return; }
            if (pickerRef[0] != null && pickerRef[0].isShowing()) {
                pickerRef[0].dismiss();
                pickerRef[0] = null;
                return;
            }
            pickerRef[0] = showLangPicker(activity, targetPairsRef[0], tgtIdxRef, dark, textColor, popupBg, new Runnable() {
                public void run() {
                    targetBtn.setText(((String[])targetPairsRef[0].get(tgtIdxRef[0]))[1]);
                }
            });
            pickerRef[0].showAsDropDown(targetBtn, 0, dp(activity, 6));
        }
    });

    swap.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if (targetPairsRef[0].size() == 0) return;
                final String srcCode = ((String[])sourcePairs.get(srcIdxRef[0]))[0];
                final String tgtCode = ((String[])targetPairsRef[0].get(tgtIdxRef[0]))[0];
                if ("auto".equals(srcCode)) {
                    status.setText("自动检测不能作为目标语言");
                    return;
                }
                boolean canReverse = false;
                try {
                    Class configClass = ClassUtils.INSTANCE.getModuleClassLoader()
                        .loadClass("com.tencent.mobileqq.ocr.data.OcrConfig");
                    java.lang.reflect.Method method = configClass.getDeclaredMethod("getDefaultSupportLanguages", new Class[]{String.class});
                    Object result = method.invoke(null, new Object[]{tgtCode});
                    if (result instanceof java.util.List) {
                        java.util.List list = (java.util.List) result;
                        for (int i = 0; i < list.size(); i++) {
                            if (srcCode.equals(String.valueOf(list.get(i)))) { canReverse = true; break; }
                        }
                    }
                } catch (Throwable ignored) {}
                if (!canReverse) {
                    status.setText("当前语言组合不支持互换");
                    return;
                }
                final String oldOriginalText = original.getText().toString();
                final String oldTranslatedText = translated.getText().toString();
                int newSrcIdx = 0;
                for (int i = 0; i < sourcePairs.size(); i++) {
                    if (((String[])sourcePairs.get(i))[0].equals(tgtCode)) { newSrcIdx = i; break; }
                }
                srcIdxRef[0] = newSrcIdx;
                sourceBtn.setText(((String[])sourcePairs.get(newSrcIdx))[1]);
                tgtIdxRef[0] = 0;
                refreshTargets[0].run();
                if (!oldTranslatedText.trim().isEmpty()) {
                    original.setText(oldTranslatedText);
                    translated.setText(oldOriginalText);
                } else {
                    translated.setText(oldOriginalText);
                    original.setText("");
                }
                status.setText("已交换语言");
            } catch (Throwable t) {
                traceLog("api5_log", "[翻译] 交换异常: " + t);
            }
        }
    });

    LinearLayout buttons = new LinearLayout(activity);
    buttons.setGravity(Gravity.CENTER_VERTICAL);
    buttons.setPadding(0, dp(activity, 12), 0, 0);
    TextView translateBtn = createButton(activity, "翻译", Color.WHITE, pc("#FF1677FF"), 14f, 10, 16, 10, true, 0, 0, null);
    TextView sendBtn = createButton(activity, "发送译文", Color.WHITE, pc("#FF34A853"), 14f, 10, 16, 10, true, 0, 0, null);
    buttons.addView(translateBtn, new LinearLayout.LayoutParams(0, dp(activity, 44), 1f));
    View gap = new View(activity);
    buttons.addView(gap, new LinearLayout.LayoutParams(dp(activity, 10), 1));
    buttons.addView(sendBtn, new LinearLayout.LayoutParams(0, dp(activity, 44), 1f));
    card.addView(buttons);

    copyOriginal.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("原文", original.getText().toString()));
                Toast("已复制原文");
            } catch (Throwable t) { Toast("复制失败"); }
        }
    });
    copyTranslated.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("译文", translated.getText().toString()));
                Toast("已复制译文");
            } catch (Throwable t) { Toast("复制失败"); }
        }
    });

    final boolean[] busy = new boolean[]{false};
    final Object handler = trGetOcrHandler();

    translateBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if (busy[0]) return;
                if (targetPairsRef[0].size() == 0) { status.setText("没有可用的目标语言"); return; }
                final String from = ((String[])sourcePairs.get(srcIdxRef[0]))[0];
                final String to = ((String[])targetPairsRef[0].get(tgtIdxRef[0]))[0];
                busy[0] = true;
                status.setText("正在翻译…");
                trStartRequest(handler, original.getText().toString(), from, to, translated, status, dialog, busy);
            } catch (Throwable t) {
                busy[0] = false;
                status.setText("启动翻译失败: " + t.getMessage());
                traceLog("api5_log", "[翻译] 启动异常: " + t);
            }
        }
    });
    sendBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try {
                String text = translated.getText().toString();
                if (text.trim().isEmpty()) { status.setText("没有可发送的译文"); return; }
                dialog.dismiss();
                复读(msgData, text);
            } catch (Throwable t) {
                traceLog("api5_log", "[翻译] 发送译文异常: " + t);
            }
        }
    });

    close.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });

    dialog.setContentView(root);
    dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
        public void onDismiss(android.content.DialogInterface d) {
            isDialogShowing = false;
            if (pickerRef[0] != null && pickerRef[0].isShowing()) {
                try { pickerRef[0].dismiss(); } catch (Throwable ignore) {}
            }
        }
    });
    dialog.getWindow().setLayout(-1, -1);
    dialog.show();
    dialog.getWindow().setLayout(-1, -1);
    isDialogShowing = true;
}

void showActionDialogInner(final Activity activity, final MsgData msgData, final View targetView, final Intent originalIntent) {
    final Object finaldata = msgData;
    final boolean dark = isThemeDark(activity);

    final String[] modeNames = new String[]{"复读", "多次复读", "作图", "加解密", "翻译"};
    final boolean[] hasImage = new boolean[]{false};
    final int[] currentModeIndex = new int[]{0};
    final SpannableStringBuilder initSb = new SpannableStringBuilder();

    validSpecialTexts.clear();

    if (msgData.data.elements != null) {
        for (int i = 0; i < msgData.data.elements.size(); i++) {
            MsgElement el = (MsgElement) msgData.data.elements.get(i);
            if (el == null) continue;
            if (el.textElement != null) {
                String c = el.textElement.content;
                if (c != null && !c.isEmpty()) {
                    initSb.append(c);
                    if (el.textElement.atType != 0) validSpecialTexts.add(c);
                }
            } else if (el.picElement != null) {
                hasImage[0] = true;
                initSb.append("[pic=" + getFullPicUrl(el.picElement.originImageUrl, msgData.type) + "]");
            } else if (el.faceElement != null) {
                String ft = el.faceElement.faceText;
                if (ft != null && !ft.isEmpty()) {
                    initSb.append(ft);
                    validSpecialTexts.add(ft);
                }
            } else if (el.marketFaceElement != null) {
                initSb.append("[动画表情]");
            } else if (el.videoElement != null) {
                hasImage[0] = true;
                initSb.append("[视频]");
            } else if (el.pttElement != null) {
                initSb.append("[语音]");
            } else if (el.fileElement != null) {
                initSb.append("[文件]");
            } else if (el.arkElement != null || el.structMsgElement != null) {
                initSb.append("[卡片]");
            }
        }
    }
    final String originalTextString = initSb.toString();
    final Dialog dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
    dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
        public void onDismiss(android.content.DialogInterface d) {
            isDialogShowing = false;
            for (int i = 0; i < WHEEL_STATE.length; i++) WHEEL_STATE[i] = null;
        }
    });
    FrameLayout root = new FrameLayout(activity);
    root.setBackgroundColor(pc("#99000000"));
    root.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { dialog.dismiss(); } });
    FrameLayout card = new FrameLayout(activity);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(dark ? pc("#FF2D2D2D") : Color.WHITE);
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
    editText.setTextColor(dark ? pc("#FFEFEFEF") : Color.BLACK);

    boolean isSelf = false;
    try {
        Object qObj = null;
        try { qObj = qq; } catch (Throwable t) { }
        if (qObj != null && Long.parseLong(qObj.toString()) == msgData.data.senderUin) isSelf = true;
    } catch (Throwable t) { }

    GradientDrawable etBg = new GradientDrawable();
    etBg.setCornerRadius(dp(activity, 8));
    if (isSelf) {
        etBg.setColor(dark ? pc("#FF1E3A5F") : pc("#E7F0FF"));
        etBg.setStroke(dp(activity, 1), dark ? pc("#FF3A5F8A") : pc("#C8D4E5"));
    } else {
        etBg.setColor(dark ? pc("#FF333333") : pc("#F5F5F5"));
        etBg.setStroke(dp(activity, 1), dark ? pc("#FF555555") : pc("#E0E0E0"));
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
    btnBg.setColor(pc("#FF007AFF"));
    actionBtn.setBackground(btnBg);

    final TextView btnTv = new TextView(activity);
    btnTv.setText(modeNames[0]);
    btnTv.setTextColor(Color.WHITE);
    btnTv.setGravity(Gravity.CENTER);
    actionBtn.addView(btnTv, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    rightBtnContainer.addView(actionBtn, new LinearLayout.LayoutParams(dp(activity, 68), dp(activity, 40)));

    View spacer = new View(activity);
    rightBtnContainer.addView(spacer, new LinearLayout.LayoutParams(-1, dp(activity, 4)));
    TextView previewBtn = createButton(activity, "预览", pc("#999999"), Color.TRANSPARENT, 12f, 8, 10, 12, false, 1, pc("#E0E0E0"), new Runnable() {
        public void run() { applySpans(editText, true); }
    });
    previewBtn.setSingleLine(true);
    previewBtn.setIncludeFontPadding(false);
    previewBtn.setGravity(Gravity.CENTER);
    if (hasImage[0]) {
        rightBtnContainer.addView(previewBtn, new LinearLayout.LayoutParams(dp(activity, 68), -2));
    }

    interactionRow.addView(rightBtnContainer);

    final boolean[] isActionTriggered = {false};
    actionBtn.setOnTouchListener(new View.OnTouchListener() {
        private float downX = 0;
        private boolean hasMoved = false;

        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_DOWN) {
                try {
                    downX = event.getRawX();
                    hasMoved = false;
                    isActionTriggered[0] = false;
                    WHEEL_STATE[0] = showStyleWheelSelector(activity, actionBtn, currentModeIndex, modeNames, actionBtn, btnTv, event);
                    View s = (View) WHEEL_STATE[1];
                    WHEEL_STATE[5] = new float[]{downX, s.getTranslationX()};
                } catch (Throwable t) {
                }
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                try {
                    float rawDx = event.getRawX() - downX;
                    if (Math.abs(rawDx) > dp(activity, 15)) hasMoved = true;
                    if (WHEEL_STATE[0] != null && hasMoved) updateSliderPhysics(rawDx, true);
                } catch (Throwable t) {
                }
                return true;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                try {
                    if (WHEEL_STATE[0] != null) {
                        final PopupWindow popup = (PopupWindow) WHEEL_STATE[0];
                        if (!hasMoved) {
                            if (!isActionTriggered[0]) {
                                isActionTriggered[0] = true;
                                String currentText = editText.getText().toString();
                                final boolean isModified = !currentText.equals(originalTextString);
                                final int mode = currentModeIndex[0];
                                if (mode == 0) {
                                    dialog.dismiss();
                                    复读(msgData, isModified ? currentText : null);
                                } else if (mode == 1) {
                                    dialog.dismiss();
                                    showRepeatCountDialog(activity, msgData, isModified ? currentText : null);
                                } else if (mode == 2) {
                                    dialog.dismiss();
                                    作图(activity, currentText);
                                } else if (mode == 3) {
                                    dialog.dismiss();
                                    showEncryptDecryptDialog(activity, finaldata);
                                } else if (mode == 4) {
                                    dialog.dismiss();
                                    uiHandler.postDelayed(new Runnable() {
                                        public void run() {
                                            try {
                                                showTranslateDialog(activity, msgData);
                                            } catch (Throwable t) {
                                                traceLog("api5_log", "[翻译] 弹窗打开失败: " + t);
                                            }
                                        }
                                    }, 100);
                                }
                            }
                        } else {
                            View slider = (View) WHEEL_STATE[1];
                            View menuRoot = (View) WHEEL_STATE[8];
                            float[] centers = (float[]) WHEEL_STATE[2];
                            if (slider != null) {
                                float logicTrans = slider.getTranslationX();
                                float logicCenter = centers[0] + logicTrans;
                                int bestIdx = 0;
                                float minDist = Float.MAX_VALUE;
                                for (int i = 0; i < centers.length; i++) {
                                    float d = Math.abs(logicCenter - centers[i]);
                                    if (d < minDist) { minDist = d; bestIdx = i; }
                                }
                                final float targetTrans = centers[bestIdx] - centers[0];
                                SpringAnimation animX = new SpringAnimation(slider, DynamicAnimation.TRANSLATION_X, targetTrans);
                                animX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
                                animX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
                                final int finalIdx = bestIdx;
                                currentModeIndex[0] = finalIdx;
                                btnTv.setText(modeNames[finalIdx]);
                                GradientDrawable bg2 = (GradientDrawable) actionBtn.getBackground();
                                String[] colors = {"#007AFF", "#FF9500", "#AF52DE", "#34C759", "#5856D6"};
                                bg2.setColor(pc(colors[finalIdx % colors.length]));
                                String now = editText.getText().toString();
                                boolean mod = !now.equals(originalTextString);
                                if (finalIdx == 0) btnTv.setText(mod ? "发送" : "复读");
                                else if (finalIdx == 1) btnTv.setText(mod ? "多次发送" : "多次复读");
                                else btnTv.setText(modeNames[finalIdx]);
                                animX.start();
                                if (menuRoot != null) {
                                    menuRoot.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(150)
                                        .withEndAction(new Runnable() {
                                            public void run() { if (popup.isShowing()) popup.dismiss(); }
                                        })
                                        .start();
                                } else {
                                    popup.dismiss();
                                }
                            } else {
                                popup.dismiss();
                            }
                        }
                    }
                } catch (Throwable t) {
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
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
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
    TextView menuBtn = createButton(activity, "更多", Color.WHITE, pc("#B3007AFF"), 14f, 8, 16, 10, true, 0, 0, new Runnable() {
        public void run() {
            dialog.dismiss();
            菜单(finaldata);
        }
    });
    TextView origBtn = createButton(activity, "原功能", pc("#B3007AFF"), pc("#F2F2F7"), 14f, 8, 16, 10, true, 0, 0, new Runnable() {
        public void run() {
            dialog.dismiss();
            isReplayingClick = true;
            try {
                if (originalIntent != null) {
                    try {
                        originalIntent.putExtra("qfun_script_handled", true);
                        activity.startActivity(originalIntent);
                        traceLog("api5_log", "原功能Intent启动成功");
                    } catch (Exception e) {
                        traceLog("api5_log", "原功能Intent启动异常: " + e.getMessage());
                    }
                }
                if (originalIntent == null && targetView != null) {
                    boolean clicked = false;
                    try { clicked = targetView.callOnClick(); } catch (Throwable e) { }
                    if (!clicked) {
                        try { clicked = targetView.performClick(); } catch (Throwable e) { }
                    }
                    if (!clicked && targetView instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) targetView;
                        for (int i = 0; i < vg.getChildCount(); i++) {
                            View child = vg.getChildAt(i);
                            if (child != null && child.isClickable()) {
                                try { child.performClick(); break; } catch (Throwable e) { }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                traceLog("api5_log", "原功能回放异常: " + e.getMessage());
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

void showBigCountConfirm(Activity activity, final MsgData data, final String text, final int count, final int delayMs, final Dialog parent) {
    boolean isDark = isThemeDark(activity);
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    int errorColor = pc("#FFE53935");

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
    msg.setText("即将发送 " + count + " 条消息，间隔 " + delayMs + "ms\n高频发送可能导致账号风控，确定要继续吗？");
    msg.setTextSize(14);
    msg.setTextColor(textColor);
    msg.setPadding(0, 0, 0, dp(activity, 4));
    root.addView(msg);

    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.RIGHT);
    btnRow.setPadding(0, dp(activity, 16), 0, dp(activity, 4));

    TextView cancelBtn = createButton(activity, "取消", subColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);
    TextView confirmBtn = createButton(activity, "继续发送", errorColor, Color.TRANSPARENT, 14f, 0, 16, 10, true, 0, 0, null);
    btnRow.addView(cancelBtn);
    btnRow.addView(confirmBtn);
    root.addView(btnRow);

    final AlertDialog[] ref = new AlertDialog[1];

    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { if (ref[0] != null) ref[0].dismiss(); }
    });
    confirmBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            if (ref[0] != null) ref[0].dismiss();
            if (parent != null) parent.dismiss();
            if (delayMs == 0) doMultiSend(data, text, count);
            else doMultiSendWithDelay(data, text, count, delayMs);
        }
    });

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(root);
    ref[0] = builder.create();
    ref[0].show();
    applyUiTheme(activity, ref[0], 0);
}

void showRepeatCountDialog(final Activity activity, final MsgData data, final String currentText) {
    if (activity == null || activity.isFinishing()) return;

    boolean isDark = isThemeDark(activity);
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subColor = isDark ? pc("#99EFEFEF") : pc("#99000000");
    int accentColor = isDark ? pc("#FF8AB4F8") : pc("#FF2196F3");

    LinearLayout root = new LinearLayout(activity);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(activity, 20), dp(activity, 20), dp(activity, 20), dp(activity, 8));

    TextView title = new TextView(activity);
    title.setText(currentText != null ? "多次发送" : "多次复读");
    title.setTextSize(17);
    title.setTextColor(textColor);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 0, 0, dp(activity, 16));
    root.addView(title);

    TextView countLabel = new TextView(activity);
    countLabel.setText("发送次数");
    countLabel.setTextSize(13);
    countLabel.setTextColor(subColor);
    countLabel.setPadding(dp(activity, 2), 0, 0, dp(activity, 4));
    root.addView(countLabel);

    final EditText countInput = makeInput(activity, "例如：5", null);
    countInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    countInput.setText("5");
    countInput.setSingleLine(true);
    countInput.setTextSize(14);
    root.addView(countInput, new LinearLayout.LayoutParams(-1, -2));

    TextView countTip = new TextView(activity);
    countTip.setText("次数过多可能会造成刷屏");
    countTip.setTextSize(11);
    countTip.setTextColor(adjustAlpha(textColor, 0.45f));
    countTip.setPadding(dp(activity, 2), dp(activity, 4), 0, dp(activity, 14));
    root.addView(countTip);

    TextView delayLabel = new TextView(activity);
    delayLabel.setText("发送间隔（毫秒）");
    delayLabel.setTextSize(13);
    delayLabel.setTextColor(subColor);
    delayLabel.setPadding(dp(activity, 2), 0, 0, dp(activity, 4));
    root.addView(delayLabel);

    final EditText delayInput = makeInput(activity, "例如：500", null);
    delayInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    delayInput.setSingleLine(true);
    delayInput.setText("500");
    delayInput.setTextSize(14);
    root.addView(delayInput, new LinearLayout.LayoutParams(-1, -2));

    TextView delayTip = new TextView(activity);
    delayTip.setText("每条消息之间的等待时间，填 0 无间隔\n间隔过短可能触发风控，建议 ≥ 300ms");
    delayTip.setTextSize(11);
    delayTip.setTextColor(adjustAlpha(textColor, 0.45f));
    delayTip.setPadding(dp(activity, 2), dp(activity, 4), 0, dp(activity, 6));
    root.addView(delayTip);

    LinearLayout btnRow = new LinearLayout(activity);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.RIGHT);
    btnRow.setPadding(0, dp(activity, 16), 0, dp(activity, 4));

    TextView cancelBtn = createButton(activity, "取消", subColor, Color.TRANSPARENT, 14f, 0, 16, 10, false, 0, 0, null);
    final TextView confirmBtn = createButton(activity, "开始发送", accentColor, Color.TRANSPARENT, 14f, 0, 16, 10, true, 0, 0, null);
    btnRow.addView(cancelBtn);
    btnRow.addView(confirmBtn);
    root.addView(btnRow);

    final AlertDialog[] ref = new AlertDialog[1];

    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { if (ref[0] != null) ref[0].dismiss(); }
    });

    confirmBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String countStr = countInput.getText().toString().trim();
            String delayStr = delayInput.getText().toString().trim();
            if (countStr.isEmpty()) { Toast("请输入发送次数"); return; }
            int count;
            int delayMs;
            try { count = Integer.parseInt(countStr); } catch (Exception e) { Toast("次数格式错误"); return; }
            try { delayMs = delayStr.isEmpty() ? 500 : Integer.parseInt(delayStr); } catch (Exception e) { Toast("间隔格式错误"); return; }
            if (count <= 0) { Toast("次数必须大于 0"); return; }
            if (delayMs < 0) { Toast("间隔不能为负数"); return; }

            if (count > 10) {
                if (ref[0] != null) ref[0].dismiss();
                showBigCountConfirm(activity, data, currentText, count, delayMs, null);
            } else {
                if (ref[0] != null) ref[0].dismiss();
                if (delayMs == 0) doMultiSend(data, currentText, count);
                else doMultiSendWithDelay(data, currentText, count, delayMs);
            }
        }
    });

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setView(root);
    ref[0] = builder.create();
    ref[0].show();
    applyUiTheme(activity, ref[0], 0);
}

void initdoublemsg() {
    Class[] sig9 = new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class, int.class, int.class};
    Class[] sig8 = new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class, int.class};
    Class[] sig7 = new Class[]{Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class};
    Method execStartActivity = getCachedMethod(Instrumentation.class, "execStartActivity", sig9);
    if (execStartActivity == null) execStartActivity = getCachedMethod(Instrumentation.class, "execStartActivity", sig8);
    if (execStartActivity == null) execStartActivity = getCachedMethod(Instrumentation.class, "execStartActivity", sig7);
    if (execStartActivity == null) {
        traceLog("api5_log", "[initdoublemsg] 安装失败: execStartActivity方法未找到");
        Activity failAct = getNowActivity();
        if (failAct == null) failAct = 最后Activity;
        showApiProtectGuide(failAct);
        return;
    }
    try {
        hookloveList.add(XposedBridge.hookMethod(execStartActivity, new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                if (!getBoolean("settings", "双击消息开关", false)) return;
                if (isReplayingClick) return;
                Object[] args = param.args;
                if (args == null) return;

                Intent intent = null;
                for (int j = 0; j < args.length; j++) {
                    if (args[j] instanceof Intent) { intent = (Intent) args[j]; break; }
                }
                if (intent == null || intent.getBooleanExtra("qfun_script_handled", false)) return;

                String comp = "";
                if (intent.getComponent() != null) comp = intent.getComponent().getClassName();
                else if (intent.getAction() != null) comp = intent.getAction();
                if (!comp.contains("TextPreviewActivity")
                        && !comp.contains("QQGalleryActivity")
                        && !comp.contains("PhotoPreviewActivity")
                        && !comp.contains("AIOGalleryActivity")
                        && !comp.contains("FileBrowserActivity")) return;

                if (isDialogShowing) { param.setResult(null); return; }

                Bundle extras = intent.getExtras();
                if (extras == null) return;
                long msgId = extras.getLong("realMsgId", 0);
                if (msgId == 0) msgId = extras.getLong("msgId", 0);
                if (msgId == 0) msgId = extras.getLong("uniseq", 0);
                int chatType = extras.getInt("nt_chat_type", 0);
                if (chatType == 0) chatType = extras.getInt("uintype", 0);
                String peerUid = extras.getString("key_bundle_nt_peeruid");
                if (peerUid == null) peerUid = extras.getString("peerUid", "");
                if (peerUid == null) peerUid = extras.getString("uin", "");
                if (msgId == 0 || peerUid == null || peerUid.isEmpty()) {
                    intent.putExtra("qfun_script_handled", true);
                    return;
                }

                traceLog("api5_log", "[initdoublemsg] Intent拦截: MsgId=" + msgId + " chatType=" + chatType + " peerUid=" + peerUid);
                param.setResult(null);

                Activity act = null;
                for (int j = 0; j < args.length; j++) {
                    if (args[j] instanceof Activity) { act = (Activity) args[j]; break; }
                }
                if (act == null) act = QQCurrentEnv.INSTANCE.getActivity();

                final Activity[] actRef = new Activity[]{act};
                final Intent finalIntent = intent;

                isDialogShowing = true;
                uiHandler.postDelayed(new Runnable() {
                    public void run() { isDialogShowing = false; }
                }, 5000L);

                fetchRealMsgRecord(msgId, chatType, peerUid, new MsgLoadedCallback() {
                    public void onLoaded(MsgData msgData) {
                        Activity target = actRef[0];
                        if (target == null || target.isFinishing()) target = getNowActivity();
                        showActionDialog(target, msgData, null, finalIntent);
                    }
                });
            }
        }));
    } catch (Throwable t) {
        traceLog("api5_log", "[initdoublemsg] 安装失败: " + t.getMessage());
        Activity failAct = getNowActivity();
        if (failAct == null) failAct = 最后Activity;
        showApiProtectGuide(failAct);
    }
}

AIOViewUpdateListener revokeMarkListener = null;

String revokeMarkText() {
    String t = getString("settings", "revoke_mark_text", "");
    if (t == null || t.length() == 0) t = "已撤回";
    return t;
}

int revokeMarkColor() {
    try {
        if ("custom".equals(getString("settings", "revoke_mark_color_mode", "module"))) {
            return pc(getString("settings", "revoke_mark_color", "#FFFF0000"));
        }
        return ShowMsgTime.INSTANCE.getConfig().color;
    } catch (Throwable e) {
        traceLog("api5_log", "[已撤回标记] 取色失败: " + e);
        return pc("#FFFF0000");
    }
}

void installRevokeMarkListener() {
    if (revokeMarkListener != null) return;
    revokeMarkListener = new AIOViewUpdateListener() {
        public void onUpdate(FrameLayout layout, MsgRecord msg) {
            try {
                if (layout == null) return;
                if (!getBoolean("settings", "revoke_mark", false)
                        || !ShowMsgTime.INSTANCE.isEnable()
                        || !AntiRevoke.INSTANCE.isEnable()) {
                    return;
                }
                TextView builtIn = (TextView) layout.findViewWithTag("RevokeTagView");
                if (builtIn == null || builtIn.getVisibility() != View.VISIBLE) return;
                TextView timeView = (TextView) layout.findViewWithTag("MsgTime");
                if (timeView == null) return;
                builtIn.setVisibility(View.GONE);
                String mark = revokeMarkText();
                String cur = timeView.getText().toString();
                if (cur.indexOf(mark) < 0) timeView.setText(cur + mark);
                timeView.setTextColor(revokeMarkColor());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) timeView.getLayoutParams();
                if (lp != null) {
                    if ("top".equals(getString("settings", "revoke_mark_pos", "bottom"))) {
                        lp.gravity = Gravity.TOP | Gravity.END;
                    } else {
                        lp.gravity = Gravity.BOTTOM | Gravity.END;
                    }
                    timeView.setLayoutParams(lp);
                }
            } catch (Throwable e) {
                traceLog("api5_log", "[已撤回标记] 异常: " + e);
            }
        }
    };
    OnAIOViewUpdate.INSTANCE.addListener(revokeMarkListener);
}

installRevokeMarkListener();

initdoublemsg();