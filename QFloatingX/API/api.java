
import com.tencent.qqnt.msg.api.IMsgUtilApi;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.TextElement;
import com.tencent.qqnt.kernel.nativeinterface.PicElement;
import com.tencent.qqnt.kernel.nativeinterface.VideoElement;
import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.qqnt.msg.api.IMsgService;
import com.tencent.qqnt.kernel.nativeinterface.PttElement;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback;
import me.yxp.qfun.utils.qq.QQCurrentEnv;
import com.tencent.mobileqq.profilecard.api.IProfileDataService;
import com.tencent.mobileqq.profilecard.api.IProfileProtocolService;
import android.os.Bundle;
import com.tencent.mobileqq.data.Card;
import java.lang.Thread; // Thread.yield()
Object app = BaseApplicationImpl.getApplication().getRuntime();


// 1. 背景颜色 (使用处: 弹窗背景、根布局背景)
String UI_COLOR_BG_LIGHT = "#FFFFFF"; // 亮色模式：纯白
String UI_COLOR_BG_DARK = "#FF1E1E1E"; // 暗色模式：深灰 (Material Dark)

// 2. 主文本颜色 (使用处: 标题、正文、列表项)
int UI_COLOR_TEXT_LIGHT = Color.parseColor("#FF000000"); // 亮色模式：纯黑
int UI_COLOR_TEXT_DARK = Color.parseColor("#FFEFEFEF"); // 暗色模式：灰白

// 3. 次要文本颜色 (使用处: Hint提示、取消按钮、说明文字)
int UI_COLOR_SUBTEXT_LIGHT = Color.parseColor("#99000000"); // 亮色模式：半透黑
int UI_COLOR_SUBTEXT_DARK = Color.parseColor("#99EFEFEF"); // 暗色模式：半透白

// 4. 输入框/容器背景 (使用处: EditText背景、代码块背景)
int UI_COLOR_INPUT_BG_LIGHT = Color.parseColor("#0D000000"); // 亮色模式：极淡黑
int UI_COLOR_INPUT_BG_DARK = Color.parseColor("#1AFFFFFF"); // 暗色模式：极淡白

// 5. 描边/分割线颜色 (使用处: 按钮边框、输入框边框)
int UI_COLOR_STROKE_LIGHT = Color.parseColor("#1A000000"); // 亮色模式：淡黑线条
int UI_COLOR_STROKE_DARK = Color.parseColor("#33FFFFFF"); // 暗色模式：淡白线条

// 6. 强调色 (使用处: 确定按钮文字、图标)
// 亮色模式默认蓝，暗色模式使用更柔和的蓝
int UI_COLOR_ACCENT_LIGHT = Color.parseColor("#FF2196F3");
int UI_COLOR_ACCENT_DARK = Color.parseColor("#FF8AB4F8");

public String get(String url) {
	StringBuffer buffer = new StringBuffer();
	InputStreamReader isr = null;
	try {
		URL urlObj = new URL(url);
		URLConnection uc = urlObj.openConnection();
		uc.setConnectTimeout(10000);
		uc.setReadTimeout(10000);
		isr = new InputStreamReader(uc.getInputStream(), "utf-8");
		BufferedReader reader = new BufferedReader(isr);
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line + "\n");
		}
	} catch (Exception e) {
		return "访问网页失败，原因:" + e;
	} finally {
		try {
			if (null != isr) {
				isr.close();
			}
		} catch (IOException e) {
			return "访问网页失败，原因:" + e;
		}
	}
	if (buffer.length() == 0) return "访问网页失败";
	buffer.delete(buffer.length() - 1, buffer.length());
	return buffer.toString();
}

private String getTodayDateStr() {
	try {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
		return dateFormat.format(cal.getTime());
	} catch (Exception e) {
		log("日期格式化错误: " + e.getMessage());
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return (year < 1000 ? "2000" : String.valueOf(year)) +
			(month < 10 ? "0" + month : String.valueOf(month)) +
			(day < 10 ? "0" + day : String.valueOf(day));
	}
}

public String qzoneGet(String url, String cookie)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL urlObj = new URL(url);
        URLConnection uc = urlObj.openConnection();
        uc.setRequestProperty("Content-Type", "text/html; charset=UTF-8");
        uc.setRequestProperty("Host", "h5.qzone.qq.com");
        uc.setRequestProperty("Cookie", cookie);
        uc.setRequestProperty("user-agent", "Mozilla/5.0 (Linux; Android 12; V2055A Build/SP1A.210812.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.72 MQQBrowser/6.2 TBS/046209 Mobile Safari/537.36 V1_AND_SQ_8.9.5_3176_YYB_D A_8090500 QQ/8.9.5.8845 NetType/WIFI WebP/0.3.0 Pixel/1080 StatusBarHeight/85 SimpleUISwitch/0 QQTheme/1000 InMagicWin/0 StudyMode/0 CurrentMode/0 CurrentFontScale/0.87 GlobalDensityScale/0.90000004 AppId/537129734");
        uc.setConnectTimeout(10000);
        uc.setReadTimeout(10000);
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}
String httpGet(String url, String cookie) {
    traceLog("api_log","[httpGet] " + url);
    try {
        URLConnection uc = new URL(url).openConnection();
        uc.setRequestProperty("Host", "h5.qzone.qq.com");
        if (cookie != null && !cookie.isEmpty()) uc.setRequestProperty("Cookie", cookie);
        uc.setRequestProperty("user-agent", "Mozilla/5.0 (Linux; Android 12; V2055A Build/SP1A.210812.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.72 MQQBrowser/6.2 TBS/046209 Mobile Safari/537.36 V1_AND_SQ_8.9.5_3176_YYB_D A_8090500 QQ/8.9.5.8845 NetType/WIFI WebP/0.3.0 Pixel/1080 StatusBarHeight/85 SimpleUISwitch/0 QQTheme/1000 InMagicWin/0 StudyMode/0 CurrentMode/0 CurrentFontScale/0.87 GlobalDensityScale/0.90000004 AppId/537129734");
        uc.setConnectTimeout(10000);
        uc.setReadTimeout(10000);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
        traceLog("api_log","[httpGet] 成功，长度: " + result.length());
        return result;
    } catch (Throwable e) {
        traceLog("api_log","[httpGet] 异常: " + e.getMessage());
        return "";
    }
}

String httpPost(String url, String cookie, String data) {
    traceLog("api_log","[httpPost] " + url);
    try {
        HttpURLConnection uc = (HttpURLConnection) new URL(url).openConnection();
        uc.setDoInput(true); uc.setDoOutput(true); uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if (cookie != null && !cookie.isEmpty()) uc.setRequestProperty("Cookie", cookie);
        uc.setConnectTimeout(20000); uc.setReadTimeout(20000);
        
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush(); uc.getOutputStream().close();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
        traceLog("api_log","[httpPost] 成功，长度: " + result.length());
        return result;
    } catch (Throwable e) {
        traceLog("api_log","[httpPost] 异常: " + e.getMessage());
        return "";
    }
}

String httpPostJson(String url, String cookie, String json) {
    traceLog("api_log","[httpPostJson] " + url);
    try {
        HttpURLConnection uc = (HttpURLConnection) new URL(url).openConnection();
        uc.setDoInput(true); uc.setDoOutput(true); uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        if (cookie != null && !cookie.isEmpty()) uc.setRequestProperty("Cookie", cookie);
        uc.setConnectTimeout(20000); uc.setReadTimeout(20000);
        
        uc.getOutputStream().write(json.getBytes("UTF-8"));
        uc.getOutputStream().flush(); uc.getOutputStream().close();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
        traceLog("api_log","[httpPostJson] 成功，长度: " + result.length());
        return result;
    } catch (Throwable e) {
        traceLog("api_log","[httpPostJson] 异常: " + e.getMessage());
        return "";
    }
}
public String httppost1(String urlPath, String cookie, String data)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL url = new URL(urlPath);
        uc = (HttpURLConnection) url.openConnection();
        uc.setDoInput(true);
        uc.setDoOutput(true);
        uc.setConnectTimeout(2000000);
        uc.setReadTimeout(2000000);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", "application/json");
        uc.setRequestProperty("Cookie", cookie);
        uc.getOutputStream().write(data.getBytes("UTF-8"));
        uc.getOutputStream().flush();
        uc.getOutputStream().close();
        isr = new InputStreamReader(uc.getInputStream(), "utf-8");
        BufferedReader reader = new BufferedReader(isr);
        String line;
        while((line = reader.readLine()) != null)
        {
            buffer.append(line + "\n");
        }
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
    finally
    {
        try
        {
            if(null != isr)
            {
                isr.close();
            }
        }
        catch(IOException e)
        {
            Toast( "错误:\n" + e);
        }
    }
    if(buffer.length() == 0) return buffer.toString();
    buffer.delete(buffer.length() - 1, buffer.length());
    return buffer.toString();
}

IProfileDataService ProfileData = app.getRuntimeService(IProfileDataService.class);
IProfileProtocolService ProtocolService = app.getRuntimeService(IProfileProtocolService.class);

//ᗜ×ᗜ开发，使用请保留版权
import com.tencent.mobileqq.troop.clockin.handler.TroopClockInHandler;
import com.tencent.mobileqq.troop.api.ITroopInfoService;

public boolean CheckSign(String qun, String uin) {
    try {
        // 1. 实例化处理器
        TroopClockInHandler inHandler;
        try {
            // 尝试使用 app (QQAppInterface) 实例化
            inHandler = new TroopClockInHandler(app);
        } catch (Throwable e) {
            try {
                // 尝试空构造函数
                inHandler = new TroopClockInHandler();
            } catch (Throwable ex) {
                qqToast(1, "创建处理器失败");
                return false;
            }
        }

        // 2. 遍历并匹配方法
        Method[] methods = TroopClockInHandler.class.getDeclaredMethods();
        for (Method m : methods) {
            Class[] paramTypes = m.getParameterTypes();
            ArrayList args = new ArrayList();
            int strCount = 0; // 记录已填充的字符串参数数量
            
            boolean match = true;
            for (Class type : paramTypes) {
                // 字符串参数：依次填入 qun, uin
                if (type == String.class || type == CharSequence.class) {
                    args.add(strCount == 0 ? qun : uin);
                    strCount++;
                } 
                // 整型参数：默认填 0
                else if (type == int.class || type == Integer.class) {
                    args.add(0);
                } 
                // 布尔参数：默认填 true
                else if (type == boolean.class || type == Boolean.class) {
                    args.add(true);
                } 
                // 长整型参数：尝试解析数字，否则默认 0
                else if (type == long.class || type == Long.class) {
                    args.add(0L);
                } 
                // 其他不支持的类型：标记为不匹配，跳过该方法
                else {
                    match = false;
                    break;
                }
            }

            // 校验：必须包含至少两个字符串参数才能承载 qun 和 uin
            if (match && strCount >= 2) {
                try {
                    m.setAccessible(true);
                    m.invoke(inHandler, args.toArray());
                    return true;
                } catch (Throwable invE) {
                    // 调用失败继续尝试下一个可能的方法
                }
            }
        }

        qqToast(1, "未找到打卡方法");
        return false;

    } catch (Throwable e) {
        qqToast(1, "执行异常: " + e.getMessage());
        return false;
    }
}

/**
 * 发送高优先级通知到系统通知栏，支持点击回调代码执行和长文本展开
 */
void sendHighPriorityNotification(String title, String content, String channelId, String clickCode) {
    try {
        Context nowContext = getNowActivity();
        if (nowContext == null) {
            nowContext = context;
        }
        if (nowContext == null) return;
        
        NotificationManager notificationManager = (NotificationManager) 
            nowContext.getSystemService(Context.NOTIFICATION_SERVICE);
        
        PendingIntent pendingIntent;
        if (clickCode == null) {
            Intent intent = nowContext.getPackageManager().getLaunchIntentForPackage(nowContext.getPackageName());
            if (intent == null) {
                intent = new Intent();
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(nowContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(nowContext, 0, intent, 0);
            }
        } else {
            Intent intent = new Intent();
            intent.setPackage(nowContext.getPackageName());
            intent.setAction("QFUN_NOTIFICATION_CLICK_" + System.currentTimeMillis());
            intent.putExtra("execute_code", clickCode);
            int requestCode = (int)(System.currentTimeMillis() % 10000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(nowContext, requestCode, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent.getActivity(nowContext, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId, "QFun通知", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("高优先级通知渠道");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(false);
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
        
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(nowContext, channelId);
        } else {
            builder = new Notification.Builder(nowContext);
        }
        
        builder.setContentTitle(title);
        builder.setContentText(content);
        builder.setSmallIcon(android.R.drawable.ic_dialog_info);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
        builder.setAutoCancel(true);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentIntent(pendingIntent);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
            bigTextStyle.setBigContentTitle(title);
            bigTextStyle.bigText(content);
            builder.setStyle(bigTextStyle);
        }
        
        Notification notification = builder.build();
        int notificationId = (int) (System.currentTimeMillis() % 10000);
        
        if (nowContext instanceof Activity) {
             ((Activity)nowContext).runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        notificationManager.notify(notificationId, notification);
                    } catch (Exception e) {}
                }
            });
        } else {
            try {
                notificationManager.notify(notificationId, notification);
            } catch (Exception e) {}
        }
        
    } catch (Throwable e) {}
}

/**
 * 简版通知发送
 */
void sendNotification(String title, String content) {
    sendHighPriorityNotification(title, content, "qfun_default_channel", null);
}

void sendNotification(String title, String content, String clickCode) {
    sendHighPriorityNotification(title, content, "qfun_default_channel", clickCode);
}

import com.tencent.mobileqq.transfile.TransferRequest;
import com.tencent.mobileqq.transfile.api.ITransFileController;
import com.tencent.mobileqq.app.BaseApplicationImpl;
//无心 由ᗜ×ᗜ适配本脚本，你直接搬可能会遇到bug哦～
boolean 上传头像(String path) {
	// toast ("111");
	try {
		ITransFileController control = app.getApplication().getRuntime().getRuntimeService(ITransFileController.class);
		TransferRequest transferRequest = new TransferRequest();
		transferRequest.mIsUp = true;
		transferRequest.mLocalPath = path;
		transferRequest.mFileType = 22;
		boolean transferAsync = control.transferAsync(transferRequest);
		return transferAsync;
		// toast("调用了上传头像");
	} catch (e) {
		return false;
	}
}

public boolean uploadCover(String path) {
	ITransFileController control = BaseApplicationImpl.getApplication().getRuntime().getRuntimeService(ITransFileController.class);
	TransferRequest transferRequest = new TransferRequest();
	transferRequest.mIsUp = true;
	transferRequest.mLocalPath = path;
	transferRequest.mFileType = 35;
	boolean transferAsync = control.transferAsync(transferRequest);
	return transferAsync;
}
public boolean uploadTroopAvatar(String qun, String path) {
	ITransFileController control = BaseApplicationImpl.getApplication().getRuntime().getRuntimeService(ITransFileController.class);
	TransferRequest transferRequest = new TransferRequest();
	transferRequest.mIsUp = true;
	transferRequest.mLocalPath = path;
	transferRequest.mFileType = 24;
	transferRequest.mPeerUin = qun;
	boolean transferAsync = control.transferAsync(transferRequest);
	return transferAsync;
}
import com.tencent.mobileqq.app.BaseActivity;
import com.tencent.mobileqq.troop.avatar.TroopPhotoController;
public boolean UploadTroopPhoto(String qun, String filepath) {
	boolean a = false;
	BaseActivity.sTopActivity.runOnUiThread(new Runnable() {
		public void run() {
			TroopAvatarActivity = new TroopAvatarWallEditActivity();
			Bundle bundle = new Bundle();
			bundle.putString("troopUin", qun);
			bundle.putInt("type", 1);
			TroopPhotoController troopPhotoController = new TroopPhotoController(context, TroopAvatarActivity, app, bundle);
			String tt = QRoute.api(ITroopPhotoUtilsApi.class).getClipStr(0, 0, 0, 0);
			a = troopPhotoController.E(filepath, tt);
		}
	});
	return a;
}
//上传群封面
import com.tencent.mobileqq.troop.avatar.TroopAvatarController;
import com.tencent.mobileqq.troop.avatar.api.ITroopPhotoUtilsApi;
import com.tencent.mobileqq.troop.activity.TroopAvatarWallEditActivity;
TroopAvatarWallEditActivity TroopAvatarActivity;
public boolean UploadTroopAvatar(String qun, String filepath) {
	boolean a = false;
	BaseActivity.sTopActivity.runOnUiThread(new Runnable() {
		public void run() {
			TroopAvatarActivity = new TroopAvatarWallEditActivity();
			Bundle bundle = new Bundle();
			bundle.putString("troopUin", qun);
			bundle.putInt("type", 1);
			TroopAvatarController troopAvatarController = new TroopAvatarController(context, TroopAvatarActivity, app, bundle);
			String tt = QRoute.api(ITroopPhotoUtilsApi.class).getClipStr(0, 0, 0, 0);
			a = troopAvatarController.E(filepath, tt);
		}
	});
	return a;
}

//🥶🐔开发
import com.tencent.mobileqq.profilecard.api.IProfileDataService;
import com.tencent.mobileqq.profilecard.api.IProfileProtocolService;
import com.tencent.mobileqq.data.Card;
public Object GetCard(String uin)
{
    IProfileDataService ProfileData = app.getRuntimeService(IProfileDataService.class);
    IProfileProtocolService ProtocolService = app.getRuntimeService(IProfileProtocolService.class);
    ProfileData.onCreate(app);
    Object card = ProfileData.getProfileCard(uin, false);
    if(card == null || card.iQQLevel == null)
    {
        Bundle bundle = new Bundle();
        bundle.putLong("selfUin", Long.parseLong(myUin));
        bundle.putLong("targetUin", Long.parseLong(uin));
        bundle.putInt("comeFromType", 12);
        ProtocolService.requestProfileCard(bundle);
        return null;
    }
    else return card;
}
public Object GetCard(String qun,String uin)
{
    IProfileDataService ProfileData = app.getRuntimeService(IProfileDataService.class);
    IProfileProtocolService ProtocolService = app.getRuntimeService(IProfileProtocolService.class);
    ProfileData.onCreate(app);
    Object card = ProfileData.getProfileCard(uin, false);
    if(card == null || card.iQQLevel == null)
    {
        Bundle bundle = new Bundle();
        bundle.putLong("selfUin", Long.parseLong(myUin));
        bundle.putLong("targetUin", Long.parseLong(uin));
        bundle.putLong("troopUin", Long.parseLong(qun));
        bundle.putInt("comeFromType", 5);
        ProtocolService.requestProfileCard(bundle);
        return null;
    }
    else return card;
}

import com.tencent.mobileqq.troop.api.ITroopInfoService;
import com.tencent.mobileqq.data.troop.TroopInfo;

public TroopInfo findTroopInfo(String qun) {
	// Object app = BaseApplicationImpl.getApplication().getRuntime();
	ITroopInfoService Info = app.getRuntimeService(ITroopInfoService.class);
	return Info.findTroopInfo("" + qun);
}
public String getGroupNames(String qun)
{
    TroopInfo info=findTroopInfo(qun);
    return info.troopname;
}
public String getUserName(String uin)
{
    try
    {
        Object card = GetCard(uin);
        if(card == null||card.strNick==null)
        {
            return getUserNickName(uin);
        }
        else
        {
            return card.strNick;
        }
    }
    catch(e)
    {
        return getUserNickName(uin);
    }
}
public String getUserNickName(String uin)
{
    try {
        String qzone = getPskey("qzone.qq.com");
        long gtk = getGTK(qzone);
        String cookie = "p_uin=o0" + myUin + ";skey=" + skey + ";p_skey=" + qzone;
        String url = "https://r.qzone.qq.com/cgi-bin/user/cgi_personal_card?uin=" + uin + "&remark=0&g_tk=" + gtk;
        String nm = httpget(url, cookie);
        String nv = nm.replace("_Callback(", "");
        String nan = nv.replace(");", "");
        String boy = nan.replaceAll("\n", "");
        JSONObject json1 = new JSONObject(boy);
        String nickname = json1.get("nickname");
        return nickname;
    }
    catch(e) {
        return uin;
    }
}

//图片类工具，由伊志平开发，由ᗜ×ᗜ适配本脚本并规范，不可直接搬运，因为适配性等未知问题

// 从路径/URL加载Bitmap（网络/本地）
Bitmap getbitmap(String path) {
	InputStream stream = null;
	try {
		if (path.startsWith("http")) {
			URL url1 = new URL(path);
			HttpURLConnection urlc = url1.openConnection();
			stream = urlc.getInputStream();
			Bitmap bmp = BitmapFactory.decodeStream(stream).copy(Bitmap.Config.ARGB_8888, true);
			return bmp;
		} else {
			stream = new FileInputStream(path);
			Bitmap bmp = BitmapFactory.decodeStream(stream).copy(Bitmap.Config.ARGB_8888, true);
			return bmp;
		}
	} catch (Throwable e) {
		traceLog("api_log.txt", "加载失败: " + e.getMessage());
		Toast("图片加载错误: " + e.getMessage());
		return Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
	} finally {
		if (stream != null) {
			try {
				stream.close();
			} catch (Throwable closeE) {
				traceLog("api_log.txt", "流关闭失败: " + closeE.getMessage());
			}
		}
	}
}

// 生成圆角Bitmap
Bitmap getroundbmp(Bitmap bitmap, float roundPx) {
	try {
		Bitmap bmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);
		Paint paint = new Paint();
		Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		RectF rectF = new RectF(rect);
		paint.setAntiAlias(true);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return bmp;
	} catch (Throwable e) {
		traceLog("api_log.txt", "圆角处理失败: " + e.getMessage());
		return bitmap;
	}
}

// Bitmap保存到文件
void bmptofile(Bitmap bmp, String path) {
	FileOutputStream fs = null;
	try {
		if (bmp == null) throw new IllegalArgumentException("Bitmap为空");

		File f = new File(path);
		if (f.exists()) f.delete();
		if (!f.getParentFile().exists()) f.getParentFile().mkdirs();

		fs = new FileOutputStream(path);
		bmp.compress(Bitmap.CompressFormat.PNG, 100, fs);
		fs.flush();

		traceLog("api_log.txt", "图片已保存: " + path);
	} catch (Throwable e) {
		traceLog("api_log.txt", "保存失败: " + e.getMessage());
		Toast("保存失败: " + e.getMessage());
	} finally {
		if (fs != null) {
			try {
				fs.close();
			} catch (Throwable closeE) {
				traceLog("api_log.txt", "文件流关闭失败: " + closeE.getMessage());
			}
		}
	}
}

// URL下载图片（异步）
void urltofile(final String url, final String path) {
	// ThreadPool.execute(new Runnable() {
	// public void run() {
	try {
		Bitmap bmp = getbitmap(url);
		bmptofile(bmp, path);
	} catch (Throwable e) {
		traceLog("api_log.txt", "下载失败: " + e.getMessage());
	}
	// }
	// });
}

// 缩放图片（异步）
void fsdx(final String path1, final String path, final Object a, final Object b) {
	ThreadPool.execute(new Runnable() {
		public void run() {
			try {
				Bitmap bm1 = getbitmap(path1);
				Matrix ma = new Matrix();
				float w = ((Number) a).floatValue();
				float h = ((Number) b).floatValue();
				ma.postScale(w, h);
				Bitmap bmp = Bitmap.createBitmap(bm1, 0, 0, bm1.getWidth(), bm1.getHeight(), ma, true);

				String savePath = ("".equals(path) || path == null) ? path1 : path;
				bmptofile(bmp, savePath);
			} catch (Throwable e) {
				traceLog("api_log.txt", "缩放失败: " + e.getMessage());
			}
		}
	});
}

// 叠加图片（异步）
void pinpic(final String path1, final String path2, final Object sw, final Object sh,
	final Object x, final Object y, final Object yd, final String path) {
	ThreadPool.execute(new Runnable() {
		public void run() {
			try {
				Bitmap bm1 = getbitmap(path1);
				Bitmap bm2 = getbitmap(path2);

				float a = ((Number) sw).floatValue() * ((float) bm1.getWidth() / (float) bm2.getWidth());
				float b = ((Number) sh).floatValue() * ((float) bm1.getHeight() / (float) bm2.getHeight());

				Matrix ma = new Matrix();
				if (((Number) sw).floatValue() > 998 || ((Number) sh).floatValue() > 998) {
					float i = Math.min(a, b);
					ma.postScale(i, i);
				} else {
					ma.postScale(a, b);
				}

				Bitmap zmp = Bitmap.createBitmap(bm2, 0, 0, bm2.getWidth(), bm2.getHeight(), ma, true);
				float x1 = ((Number) x).floatValue() * bm1.getWidth() - 0.5f* zmp.getWidth();
				float y1 = ((Number) y).floatValue() * bm1.getHeight() - 0.5f* zmp.getHeight();
				Bitmap smp = getroundbmp(zmp, ((Number) yd).floatValue());

				Canvas cas = new Canvas(bm1);
				cas.drawBitmap(smp, x1, y1, null);

				String savePath = ("".equals(path) || path == null) ? path1 : path;
				bmptofile(bm1, savePath);
			} catch (Throwable e) {
				traceLog("api_log.txt", "叠加失败: " + e.getMessage());
			}
		}
	});
}

// 写入文字（异步）
void writetopic(final String path1, final String text, final String color,
	final Object x, final Object y, final Object size, final String path) {
	ThreadPool.execute(new Runnable() {
		public void run() {
			try {
				Bitmap bmp = getbitmap(path1);
				Canvas cas = new Canvas(bmp);
				Paint pt = new Paint();
				pt.setTextSize(((Number) size).floatValue());
				pt.setTypeface(Typeface.MONOSPACE);
				if (color != null && !"".equals(color)) {
					pt.setColor(Color.parseColor(color));
				}

				float x1 = ((Number) x).floatValue() * bmp.getWidth() - 0.5f* text.length() * ((Number) size).floatValue();
				float y1 = ((Number) y).floatValue() * bmp.getHeight() + 0.5f* ((Number) size).floatValue();

				cas.drawText(text, x1, y1, pt);

				String savePath = ("".equals(path) || path == null) ? path1 : path;
				bmptofile(bmp, savePath);
			} catch (Throwable e) {
				traceLog("api_log.txt", "文字写入失败: " + e.getMessage());
			}
		}
	});
}

// 叠加QQ头像（异步）
void pinqpic(final String path1, final String qq, final Object sw, final Object sh,
	final Object x, final Object y, final Object yd, final String path) {
	ThreadPool.execute(new Runnable() {
		public void run() {
			String qqUrl = "http://q1.qlogo.cn/g?b=qq&nk=" + qq + "&s=640";
			pinpic(path1, qqUrl, sw, sh, x, y, yd, path);
		}
	});
}


//  JSON解析工具  开发者:如如 改进:荨宝（有人记得这个人吗，其实就是ᗜ×ᗜ哦，嘻嘻）
//  必须配合org.json.JSONObject使用

public String jiexi(org.json.JSONObject json, String tag) {
	String result = "";
	for (String str: json.keySet()) {
		result += "\n" + jiexi(json.get(str), str, tag);
	}
	return result;
}

public String jiexi(org.json.JSONObject json, String name, String tag) {
	String newTag = tag + "_" + name;
	if (!name.equals("h")) name = "\"" + name + "\"";
	String result = "\nJSONObject " + newTag + " = " + tag + ".getJSONObject(" + name + ");\n";
	for (String str: json.keySet()) {
		result += "\n" + jiexi(json.get(str), str, newTag);
	}
	return result;
}

public String jiexi(org.json.JSONArray json, String name, String tag) {
	String newTag = tag + "_" + name;
	if (!name.equals("h")) name = "\"" + name + "\"";
	int length = json.length();
	if (length > 0) return "\nJSONArray " + newTag + " = " + tag + ".getJSONArray(" + name + ");\nfor(int h = 0; h < " + newTag + ".length(); h++)\n{\n   " + jiexi(json.get(0), "h", newTag) + "\n}";
	else return "//" + newTag + "没有数据\n";
}

public String jiexi(Object object, String name, String tag) {
	String newTag = tag + "_" + name;
	if (!name.equals("h")) name = "\"" + name + "\"";
	if (object instanceof Integer) return "\nInteger " + newTag + " = " + tag + ".getInt(" + name + ");//→" + object;
	else if (object instanceof Long) return "\nLong " + newTag + " = " + tag + ".getLong(" + name + ");//→" + object;
	else if (object instanceof Double) return "\nDouble " + newTag + " = " + tag + ".getDouble(" + name + ");//→" + object;
	else if (object instanceof Boolean) return "\nBoolean " + newTag + " = " + tag + ".getBoolean(" + name + ");//→" + object;
	else if (object instanceof String) return "\nString " + newTag + " = " + tag + ".getString(" + name + ");//→\"" + object + "\"";
	else return "\nObject " + newTag + " = " + tag + ".get(" + name + ");//→" + object;
}

boolean 判断文件(String files) {
	File file = new File(files);
	long totalBytes = file.length();
	if (totalBytes == 0) {
		return false;
	} else {
		return true;
	}
}
public void 删除(String Path) {
	File file = null;
	try {
		file = new File(Path);
		if (file.exists()) {
			boolean deleted = file.delete();
		} else {
			traceLog("api_log.txt", "文件不存在: " + Path);
		}
	} catch (Exception e) {
		traceLog("api_log.txt", "删除文件时发生错误: " + e);
	}
}

private boolean 删除文件夹(File folder) {
	if (folder == null || !folder.isDirectory()) {
		return false;
	}

	File[] 子文件数组 = folder.listFiles();
	boolean 所有子项删除成功 = true;
	Exception 记录异常 = null;

	try {
		if (子文件数组 != null) {
			for (File 子项: 子文件数组) {
				if (子项.isDirectory()) {
					if (!删除文件夹(子项)) {
						所有子项删除成功 = false;
						traceLog("api_log.txt", "删除子文件夹失败: " + 子项.getAbsolutePath());
					}
				} else {
					if (!子项.delete()) {
						所有子项删除成功 = false;
						traceLog("api_log.txt", "删除文件失败: " + 子项.getAbsolutePath());
					}
				}
			}
		}

		if (所有子项删除成功) {
			return folder.delete();
		} else {
			return false;
		}
	} catch (Exception e) {
		记录异常 = e;
		traceLog("api_log.txt", "删除文件夹过程中异常: " + e);
		return false;
	} finally {
		if (记录异常 != null) {
			traceLog("api_log.txt", "删除文件夹: 未完全删除" + 记录异常);
		}
	}
}


String formatTime(float time) {
	String suffix = "豪秒";
	long seconds = (long)(time / 1000);
	String tr = seconds / 3600 + "时" + (seconds % 3600) / 60 + "分" + seconds % 3600 % 60 % 60 + "秒";
	tr = tr.replace("分0秒", "分");
	tr = tr.replace("时0分", "时");
	tr = tr.replace("0时", "");
	return tr;
}
String formatSize(long bytes) {
	if (bytes <= 0) return "0B";
	String[] units = new String[] {
		"B",
		"KB",
		"MB",
		"GB",
		"TB"
	};
	int digitGroups = (int)(Math.log10(bytes) / Math.log10(1024));
	StringBuffer result = new StringBuffer();
	result.append(new java.text.DecimalFormat("#,##0.##").format(bytes / Math.pow(1024, digitGroups)));
	result.append(units[digitGroups]);
	return result.toString();
}
// 基础文件大小获取 - 无外部依赖
long getFileSize(File file) {
	if (file == null) {
		traceLog("api_log.txt", "getFileSize参数为null");
		return 0;
	}
	try {
		return file.length();
	} catch (Exception e) {
		traceLog("api_log.txt", "获取文件大小失败: " + file.getName() + "    " + e);
		return 0;
	}
}

// 递归文件夹大小计算 - 依赖getFileSize
long getFolderSize(File folder) {
	if (folder == null || !folder.exists()) {
		traceLog("api_log.txt", "getFolderSize文件夹不存在: " + folder);
		return 0;
	}
	long size = 0;
	try {
		File[] files = folder.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isFile()) {
					size += getFileSize(file);
				} else if (file.isDirectory()) {
					size += getFolderSize(file);
				}
			}
		}
	} catch (Exception e) {
		traceLog("api_log.txt", "遍历文件夹失败: " + folder.getName() + "    " + e);
	}
	return size;
}

// 字节数格式化 - 无外部依赖
String getFormattedSize(long sizeInBytes) {
	if (sizeInBytes < 0) {
		traceLog("api_log.txt", "getFormattedSize接收负值: " + sizeInBytes);
		return "0KB";
	}

	double sizeInKB = sizeInBytes / 1024.0;

	if (sizeInKB < 1024) {
		return String.format("%.3fKB", sizeInKB);
	} else if (sizeInKB < 1048576) { // 1024*1024
		double sizeInMB = sizeInKB / 1024.0;
		return String.format("%.3fMB", sizeInMB);
	} else {
		double sizeInGB = sizeInKB / 1048576.0;
		return String.format("%.3fGB", sizeInGB);
	}
}

// 文件夹对象格式化 - 依赖getFolderSize和getFormattedSize(long)
String getFormattedSize(File folder) {
	if (folder == null) {
		traceLog("api_log.txt", "getFormattedSize(File)参数为null");
		return "文件夹不存在";
	}
	if (!folder.exists()) {
		traceLog("api_log.txt", "文件夹不存在: " + folder.getAbsolutePath());
		return "文件夹不存在";
	}
	if (!folder.isDirectory()) {
		traceLog("api_log.txt", "路径不是文件夹: " + folder.getAbsolutePath());
		return "不是有效文件夹";
	}

	try {
		long sizeInBytes = getFolderSize(folder);
		if (sizeInBytes == 0) {
			return "0KB";
		}
		return getFormattedSize(sizeInBytes);
	} catch (Exception e) {
		traceLog("api_log.txt", "格式化文件夹大小失败: " + folder.getName() + "    " + e);
		return "计算失败";
	}
}
public String 读(String FilePath) {
	if (FilePath == null || FilePath.trim().isEmpty()) {
		return "读文件失败：文件路径为空";
	}
	InputStreamReader inputReader = null;
	BufferedReader bf = null;
	try {
		File file = new File(FilePath);
		if (!file.exists()) {
			if (file.createNewFile()) {
				return ""; // 新建空文件，返回空字符串
			} else {
				return "读文件失败：文件不存在且创建失败，路径：" + FilePath;
			}
		}
		if (file.isDirectory()) {
			return "读文件失败：路径指向目录，无法读取，路径：" + FilePath;
		}
		if (!file.canRead()) {
			return "读文件失败：文件无读取权限，路径：" + FilePath;
		}

		inputReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
		bf = new BufferedReader(inputReader);
		StringBuilder sb = new StringBuilder();
		String str;

		while ((str = bf.readLine()) != null) {
			sb.append(str).append("\n"); // 每行结尾添加换行符，还原文件原始换行
		}
		return sb.toString();

	} catch (Exception e) {
		String errorMsg = "读文件失败：路径=" + FilePath + "，异常=" + e.getMessage();
		return errorMsg;
	} finally {
		try {
			if (bf != null) {
				bf.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (inputReader != null) {
				inputReader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

String readprop(String file, String name2) {
	if (file == null || name2 == null) {
		traceLog("api_log.txt", "readprop接收null参数: file=" + file + ", key=" + name2);
		return "";
	}

	String text = null;
	try {
		text = 读(file);
		if (text == null || text.trim().isEmpty()) {
			traceLog("api_log.txt", "properties文件内容为空: " + file);
			return "";
		}
	} catch (Exception e) {
		traceLog("api_log.txt", "读取properties文件失败: " + file + "    " + e);
		return "";
	}

	Properties props = new Properties();
	StringReader reader = null;
	try {
		reader = new StringReader(text);
		props.load(reader);
		String value = props.getProperty(name2);
		if (value == null) {
			traceLog("api_log.txt", "properties键不存在: " + name2 + " in " + file);
			return "";
		}
		return value;
	} catch (IOException e) {
		traceLog("api_log.txt", "Properties加载失败: " + file + "    " + e);
		return "";
	} catch (Exception e) {
		traceLog("api_log.txt", "Properties解析异常: " + file + "    " + e);
		return "";
	} finally {
		// 资源释放保护
		if (reader != null) {
			try {
				reader.close();
			} catch (Exception e) {
				// 关闭异常不处理，避免掩盖主异常
			}
		}
	}
}


private void 写(String Path, String WriteData) {
	if (Path == null || Path.trim().isEmpty()) {
		traceLog("api_log.txt", " 【写入失败】路径为空");
		return;
	}

	FileOutputStream fos = null;
	OutputStreamWriter osw = null;

	try {
		File file = new File(Path);
		File parentDir = file.getParentFile();

		// 确保父目录存在
		if (parentDir != null && !parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				traceLog("api_log.txt", " 【写入失败】创建目录失败: " + parentDir.getAbsolutePath());
				return;
			}
		}

		// 创建文件（如果不存在）
		if (!file.exists() && !file.createNewFile()) {
			traceLog("api_log.txt", " 【写入失败】创建文件失败: " + Path);
			return;
		}

		// 写入数据（使用UTF-8编码）
		fos = new FileOutputStream(file);
		osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
		osw.write(WriteData);
		osw.flush();
		fos.flush();
		fos.getFD().sync(); // 确保数据持久化到磁盘

		traceLog("api_log.txt", " 【写入成功】 " + Path + " (" + WriteData.length() + "字节)");

	} catch (IOException e) {
		traceLog("api_log.txt", " 【写入异常】 " + Path + " - " + e.getMessage());
	} catch (Exception e) {
		traceLog("api_log.txt", " 【写入异常】 " + Path + " - " + e.getMessage());
	} finally {
		// 在finally中关闭流
		try {
			if (osw != null) {
				osw.close();
			}
		} catch (Exception e) {
			traceLog("api_log.txt", " 【关闭writer失败】 " + e.getMessage());
		}

		try {
			if (fos != null) {
				fos.close();
			}
		} catch (Exception e) {
			traceLog("api_log.txt", " 【关闭stream失败】 " + e.getMessage());
		}
	}
}


private void 新建(String Path) {
	File dir = new File(Path);
	if (!dir.exists()) {
		dir.mkdirs();
	}
}
private String readFileContent(String filePath) throws Exception {
	File file = new File(filePath);
	if (!file.exists()) {
		return "文件不存在";
	}
	InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
	BufferedReader bf = new BufferedReader(inputReader);
	StringBuilder sb = new StringBuilder();
	String str;
	while ((str = bf.readLine()) != null) {
		sb.append(str);
	}
	bf.close();
	inputReader.close();
	return sb.toString();
}

public void log大小限制(String Path) {
	File targetFile = new File(Path);
	try {
		if (!targetFile.exists()) {
			traceLog("api_log.txt", "文件夹不存在: " + Path);
			return;
		}
		if (!targetFile.isDirectory()) {
			traceLog("api_log.txt", "目标路径不是文件夹: " + Path);
			return;
		}

		long 文件夹总大小 = getFolderSize(targetFile);
		long MB = 1024 * 1024;

		if (文件夹总大小 > MB) {
			traceLog("api_log.txt", "文件夹总大小超过1MB，准备删除: " + targetFile.getName() +
				" (" + 文件夹总大小 + " 字节)");

			boolean 删除成功 = 删除文件夹(targetFile);
			if (删除成功) {
				traceLog("api_log.txt", "成功删除文件夹: " + Path);
			} else {
				traceLog("api_log.txt", "删除文件夹失败: " + Path);
			}
		}
	} catch (Exception e) {
		traceLog("api_log.txt", "处理log文件夹时出错: " + e);
	}
}


public void setTips(String title, String message) {
	Activity ThisActivity = getNowActivity();
	ThisActivity.runOnUiThread(new Runnable() {
		public void run() {
			boolean isDark = isThemeDark(ThisActivity);

			// TextView优化配置
			TextView textView = new TextView(ThisActivity);
			textView.setText(message);
			textView.setTextSize(17);
			textView.setTextColor(isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT);
			textView.setTextIsSelectable(true);
			textView.setHorizontallyScrolling(false); // 禁用水平滚动，启用自动换行

			//  ScrollView包裹解决滑动性能问题
			ScrollView scrollView = new ScrollView(ThisActivity);
			scrollView.setFillViewport(true);
			scrollView.addView(textView);

			// 布局容器：遵循宪章3.3节尺寸约束（最大360dp宽度，左右20dp边距）
			LinearLayout layout = new LinearLayout(ThisActivity);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.setPadding(dp(20), dp(20), dp(20), dp(20)); // 20dp边距

			// 设置布局参数：宽度360dp，高度自适应
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				dp转px(ThisActivity, 320), // 宽度320dp（360dp减去左右边距）
				LinearLayout.LayoutParams.WRAP_CONTENT
			);
			layout.setLayoutParams(layoutParams);
			layout.addView(scrollView);

			// 创建并显示弹窗
			AlertDialog.Builder builder = new AlertDialog.Builder(ThisActivity,
				isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

			builder.setTitle(title)
				.setView(layout)
				.setNegativeButton("关闭", null);

			AlertDialog dialog = builder.create();
			dialog.show();
            
            // 应用统一主题
			applyUiTheme(ThisActivity, dialog);
		}
	});
}


import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.net.URLDecoder;
import android.view.GestureDetector;
import android.view.MotionEvent;


// 1. Base64
String encryptBase64(String text) {
	try {
		return new String(android.util.Base64.encode(text.getBytes("UTF-8"), android.util.Base64.NO_WRAP), "UTF-8");
	} catch (Exception e) {
		return "Base64加密失败: " + e.getMessage();
	}
}

String decryptBase64(String text) {
	try {
		return new String(android.util.Base64.decode(text.getBytes("UTF-8"), android.util.Base64.NO_WRAP), "UTF-8");
	} catch (Exception e) {
		return "Base64解密失败: " + e.getMessage();
	}
}

// 2. Unicode
String encryptUnicode(String text) {
	try {
		StringBuilder result = new StringBuilder();
		for (char c: text.toCharArray()) {
			result.append("\\u").append(String.format("%04x", (int) c));
		}
		return result.toString();
	} catch (Exception e) {
		return null;
	}
}

String decryptUnicode(String text) {
	try {
		StringBuilder result = new StringBuilder();
		String[] parts = text.split("\\\\u");
		if (text.indexOf("\\u") != 0 && parts.length > 0) result.append(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			if (part.length() >= 4) {
				try {
					int codePoint = Integer.parseInt(part.substring(0, 4), 16);
					result.append((char) codePoint);
					if (part.length() > 4) result.append(part.substring(4));
				} catch (Exception e) {
					result.append("\\u").append(part);
				}
			} else {
				result.append("\\u").append(part);
			}
		}
		return result.toString();
	} catch (Exception e) {
		return "Unicode还原失败: " + e.getMessage();
	}
}

// 分块安全调用封装 (保持接口一致性)
String encryptBase64Safe(String text) {
	return encryptBase64(text);
}
String decryptBase64Safe(String text) {
	return decryptBase64(text);
}
String encryptUnicodeSafe(String text) {
	return encryptUnicode(text);
}
String decryptUnicodeSafe(String text) {
	return decryptUnicode(text);
}

// 3. Hex (16进制)
String stringToHex(String str) {
	try {
		StringBuilder sb = new StringBuilder();
		byte[] bytes = str.getBytes("UTF-8");
		for (byte b: bytes) sb.append(String.format("%02X", b));
		return sb.toString();
	} catch (Exception e) {
		return null;
	}
}

String hexToString(String hex) {
	try {
		hex = hex.replace(" ", ""); // 容错空格
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 2) {
			String str = hex.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));
		}
		// 注意：这里做的是简单字符还原，如果是复杂字节流建议用 new String(bytes)
		return output.toString();
	} catch (Exception e) {
		return "Hex解析失败: " + e.getMessage();
	}
}

// 4. 二进制
String stringToBinary(String text) {
	try {
		StringBuilder binary = new StringBuilder();
		for (char c: text.toCharArray()) {
			String bin = Integer.toBinaryString((int) c);
			while (bin.length() < 8) bin = "0" + bin;
			binary.append(bin).append(" ");
		}
		return binary.toString().trim();
	} catch (Exception e) {
		return null;
	}
}

// 5. URL 编码
String urlEncode(String text) {
	try {
		return URLEncoder.encode(text, "UTF-8");
	} catch (Exception e) {
		return null;
	}
}
String urlDecode(String text) {
	try {
		return URLDecoder.decode(text, "UTF-8");
	} catch (Exception e) {
		return null;
	}
}

// 6. 哈希摘要 (MD5/SHA)
String getHash(String text, String algorithm) {
	try {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		byte[] hash = digest.digest(text.getBytes("UTF-8"));
		StringBuilder hexString = new StringBuilder();
		for (byte b: hash) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	} catch (Exception e) {
		return "哈希计算失败: " + e.getMessage();
	}
}

// 7. 凯撒密码 (支持正负偏移)
String caesarCipher(String text, int shift) {
	StringBuilder result = new StringBuilder();
	for (char character: text.toCharArray()) {
		if (character >= 'A' && character <= 'Z') {
			int originalPos = character - 'A';
			int newPos = (originalPos + shift) % 26;
			if (newPos < 0) newPos += 26;
			result.append((char)('A' + newPos));
		} else if (character >= 'a' && character <= 'z') {
			int originalPos = character - 'a';
			int newPos = (originalPos + shift) % 26;
			if (newPos < 0) newPos += 26;
			result.append((char)('a' + newPos));
		} else {
			result.append(character);
		}
	}
	return result.toString();
}

// 8. XOR 简单加密
String xorCipher(String text) {
	char[] key = {
		'Q',
		'F',
		'L',
		'O',
		'A',
		'T'
	};
	StringBuilder output = new StringBuilder();
	for (int i = 0; i < text.length(); i++) {
		output.append((char)(text.charAt(i) ^ key[i % key.length]));
	}
	return output.toString();
}

// 9. 倒序
String reverseString(String text) {
	return new StringBuilder(text).reverse().toString();
}

// --- 高级加密辅助 (AES/DES) ---
// 密钥生成器
private byte[] getRawKey(String seed, int length) {
	try {
		byte[] keyBytes = seed.getBytes("UTF-8");
		byte[] validKey = new byte[length];
		for (int i = 0; i < length; i++) {
			if (i < keyBytes.length) validKey[i] = keyBytes[i];
			else validKey[i] = 0;
		}
		return validKey;
	} catch (Exception e) {
		return new byte[length];
	}
}

// 10. AES 加密/解密
String aesEncrypt(String text) {
	try {
		SecretKeySpec keySpec = new SecretKeySpec(getRawKey("QFloatingX_AES_Key", 16), "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
		return new String(android.util.Base64.encode(encrypted, android.util.Base64.NO_WRAP), "UTF-8");
	} catch (Exception e) {
		return "AES加密失败: " + e.getMessage();
	}
}

String aesDecrypt(String base64Text) {
	try {
		SecretKeySpec keySpec = new SecretKeySpec(getRawKey("QFloatingX_AES_Key", 16), "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		byte[] encryptedBytes = android.util.Base64.decode(base64Text, android.util.Base64.NO_WRAP);
		return new String(cipher.doFinal(encryptedBytes), "UTF-8");
	} catch (Exception e) {
		return "AES解密失败(请确认输入为Base64): " + e.getMessage();
	}
}

// 11. DES 加密/解密
String desEncrypt(String text) {
	try {
		SecretKeySpec keySpec = new SecretKeySpec(getRawKey("QFloatingX_DES", 8), "DES");
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
		return new String(android.util.Base64.encode(encrypted, android.util.Base64.NO_WRAP), "UTF-8");
	} catch (Exception e) {
		return "DES加密失败: " + e.getMessage();
	}
}

String desDecrypt(String base64Text) {
	try {
		SecretKeySpec keySpec = new SecretKeySpec(getRawKey("QFloatingX_DES", 8), "DES");
		Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		byte[] encryptedBytes = android.util.Base64.decode(base64Text, android.util.Base64.NO_WRAP);
		return new String(cipher.doFinal(encryptedBytes), "UTF-8");
	} catch (Exception e) {
		return "DES解密失败: " + e.getMessage();
	}
}

public int dp(Activity activity, int d) {
	return (int)(d * activity.getResources().getDisplayMetrics().density);
}
public int dp(int d) {
	return (int)(d * context.getResources().getDisplayMetrics().density);
}
int dp转px(Activity activity, int dp) {
	try {
		float density = activity.getResources().getDisplayMetrics().density;
		return (int)(dp * density);
	} catch (Exception e) {
		return dp * 2;
	}
}
int dp(Context context, float dpValue) {
	final float scale = context.getResources().getDisplayMetrics().density;
	return (int)(dpValue * scale + 0.5f);
}

void vibrate(Activity activity, int milliseconds) {
    if (!getBoolean("settings", "振动反馈", true)) {
        return;
    }
	if (activity == null) activity = getNowActivity();
	if (activity == null) activity = 最后Activity;
	if (activity == null) return;

	try {
		Vibrator vibrator;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			VibratorManager vibratorManager = (VibratorManager) activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
			vibrator = vibratorManager.getDefaultVibrator();
		} else {
			vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
		}

		if (vibrator == null || !vibrator.hasVibrator()) {
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			VibrationEffect effect = VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE);
			vibrator.vibrate(effect);
		} else {
			vibrator.vibrate(milliseconds);
		}
	} catch (Exception e) {
		traceLog("api_log.txt", "震动异常: " + e);
	}
}

public String getLocationData() {
	try {
		String loc = getString("经纬度", "经纬度", "");
		if (!TextUtils.isEmpty(loc) && loc.contains(",")) {
			return loc;
		} else {
			putString("经纬度", "经纬度", 默认经纬度);
			return 默认经纬度;
		}
	} catch (Exception e) {
		putString("经纬度", "经纬度", 默认经纬度);
		return 默认经纬度;
	}
}

public Double[] splitLocation(String locStr) {
	String location = TextUtils.isEmpty(locStr) ? getLocationData() : locStr;
	try {
		String[] locArray = location.split(",");
		if (locArray.length == 2) {
			double lng = Double.parseDouble(locArray[0].trim());
			double lat = Double.parseDouble(locArray[1].trim());
			if (lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90) {
				return new Double[] {
					lng,
					lat
				};
			}
		}
	} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
		toast("经纬度格式错误，使用默认值");
	}
	return new Double[] {
		默认经度,
		默认纬度
	};
}
// 默认经纬度（天安门）
private static final String 默认经纬度 = "116.397128,39.907500";
private static final double 默认经度 = 116.397128;
private static final double 默认纬度 = 39.907500;
String locStr = getLocationData();
Double[] loc = splitLocation(locStr);
double longitude = loc[0];
double latitude = loc[1];

private void showLocationDialog(Activity activity) {
    boolean isDark = isThemeDark(activity);
    int cornerRadius = dp(activity, 8);

    int textColor = isDark ? UI_COLOR_TEXT_DARK : UI_COLOR_TEXT_LIGHT;
    int subTextColor = isDark ? UI_COLOR_SUBTEXT_DARK : UI_COLOR_SUBTEXT_LIGHT;
    int inputBgColor = isDark ? UI_COLOR_INPUT_BG_DARK : UI_COLOR_INPUT_BG_LIGHT;
    int borderColor = adjustAlpha(textColor, 0.3f);

    GradientDrawable inputBg = new GradientDrawable();
    inputBg.setCornerRadius(cornerRadius);
    inputBg.setColor(inputBgColor);
    inputBg.setStroke(dp(activity, 1), borderColor);

    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dp(activity, 20), dp(activity, 15), dp(activity, 20), dp(activity, 15));
    layout.setGravity(Gravity.CENTER);

    final EditText etLocation = new EditText(activity);
    etLocation.setHint("请输入格式：经度,纬度（例如：116.397,39.917）");
    etLocation.setText(getLocationData());
    etLocation.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
    etLocation.setTextColor(textColor);
    etLocation.setHintTextColor(subTextColor);
    etLocation.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
    etLocation.setBackground(inputBg);
    LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48));
    layout.addView(etLocation, etParams);

    AlertDialog.Builder builder = new AlertDialog.Builder(activity,
            isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("设置经纬度")
            .setView(layout)
            .setPositiveButton("保存", null)
            .setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
    final AlertDialog dialog = builder.create();
    dialog.show();

    applyUiTheme(activity, dialog);

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String input = etLocation.getText().toString().trim();
            if (input.contains(",")) {
                String[] parts = input.split(",", -1);
                if (parts.length == 2 && parts[0].length() > 0 && parts[1].length() > 0) {
                    putString("经纬度", "经纬度", input);
                    Toast("保存成功：" + input);
                    dialog.dismiss();
                } else {
                    Toast("格式错误！逗号前后不能为空，例如：116.397,39.917");
                }
            } else {
                Toast("格式错误！必须包含逗号，例如：116.397,39.917");
            }
        }
    });
}

//控件打开动画
private void startDialogShowAnimation(View view) {
	ScaleAnimation scaleAnim = new ScaleAnimation(
		0.7f, 1.0f, // X轴：起始0.7倍 → 目标1倍
		0.7f, 1.0f, // Y轴：起始0.7倍 → 目标1倍
		Animation.RELATIVE_TO_SELF, 0.5f, // 缩放中心：视图中心X
		Animation.RELATIVE_TO_SELF, 0.5f// 缩放中心：视图中心Y
	);
	scaleAnim.setDuration(333); // 动画时长，单位ms
	scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator()); // 先加速后减速，更顺滑

	// 透明度动画：从完全透明到不透明
	AlphaAnimation alphaAnim = new AlphaAnimation(0.0f, 1.0f);
	alphaAnim.setDuration(400);
	alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());

	AnimationSet set = new AnimationSet(true);
	set.addAnimation(scaleAnim);
	set.addAnimation(alphaAnim);
	set.setFillAfter(true); // 动画结束后保持最终状态

	view.startAnimation(set);
}
/**
 * 控件关闭动画：缩放缩小 + 透明度淡出
 * @param view 弹窗的根视图（如 alertDialog.getWindow().getDecorView()）
 * @param dialog 要关闭的弹窗，动画结束后自动dismiss
 */
private void startDialogDismissAnimation(View view, DialogInterface dialog) {
	if (view == null || dialog == null) return;
	//  缩放动画：从1倍缩小到0.7倍，中心缩放
	ScaleAnimation scaleAnim = new ScaleAnimation(
		1.0f, 0.7f, // X轴：1倍 → 0.7倍
		1.0f, 0.7f, // Y轴：1倍 → 0.7倍
		Animation.RELATIVE_TO_SELF, 0.5f, // 缩放中心X：视图中心
		Animation.RELATIVE_TO_SELF, 0.5f// 缩放中心Y：视图中心
	);
	scaleAnim.setDuration(250); // 关闭动画时长
	scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
	AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
	alphaAnim.setDuration(300);
	alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
	AnimationSet set = new AnimationSet(true);
	set.addAnimation(scaleAnim);
	set.addAnimation(alphaAnim);
	set.setFillAfter(true); // 保持动画结束状态
	set.setAnimationListener(new Animation.AnimationListener() {
		public void onAnimationStart(Animation animation) {}
		public void onAnimationEnd(Animation animation) {
			dialog.dismiss(); // 动画播放完再关闭，效果更自然
		}
		public void onAnimationRepeat(Animation animation) {}
	});

	view.startAnimation(set);
}

private void showReOrUnDialog(final Activity activity) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                boolean isDark = isThemeDark(activity);

                LinearLayout mainLayout = new LinearLayout(activity);
                mainLayout.setOrientation(LinearLayout.VERTICAL);
                mainLayout.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 8));

                TextView titleView = new TextView(activity);
                titleView.setText("你想选哪个呢？");
                titleView.setTextColor(isDark ? Color.parseColor("#DEEFEFEF") : Color.parseColor("#DE000000"));
                titleView.setTextSize(18);
                titleView.setPadding(0, dp(activity, 8), 0, dp(activity, 24));

                LinearLayout optionsContainer = new LinearLayout(activity);
                optionsContainer.setOrientation(LinearLayout.VERTICAL);

                Button btn1 = new Button(activity);
                btn1.setText("取消加载脚本".toUpperCase());
                btn1.setTextColor(Color.parseColor("#FFFF0000"));
                btn1.setTextSize(14);
                btn1.setBackgroundColor(Color.parseColor("#00FFFFFF"));
                btn1.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                btn1.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48)));

                Button btn2 = new Button(activity);
                btn2.setText("重新加载脚本".toUpperCase());
                btn2.setTextColor(Color.parseColor("#FFFF0000"));
                btn2.setTextSize(14);
                btn2.setBackgroundColor(Color.parseColor("#00FFFFFF"));
                btn2.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                btn2.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48)));

                View divider = new View(activity);
                divider.setBackgroundColor(isDark ? Color.parseColor("#1EFFFFFF") : Color.parseColor("#1E000000"));
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 1));
                divParams.topMargin = dp(activity, 8);
                divParams.bottomMargin = dp(activity, 8);

                Button cancelBtn = new Button(activity);
                cancelBtn.setText("取消");
                cancelBtn.setTextColor(isDark ? Color.parseColor("#DEEFEFEF") : Color.parseColor("#DE000000"));
                cancelBtn.setTextSize(14);
                cancelBtn.setBackgroundColor(Color.parseColor("#00FFFFFF"));
                cancelBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                cancelBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48)));

                mainLayout.addView(titleView);
                optionsContainer.addView(btn1);
                optionsContainer.addView(btn2);
                mainLayout.addView(optionsContainer);
                mainLayout.addView(divider, divParams);
                mainLayout.addView(cancelBtn);

                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isDark ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setView(mainLayout);

                final AlertDialog dialog = builder.create();
                dialog.show();
                applyUiTheme(activity, dialog);

                btn1.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            vibrate(activity, 50);
                            取消加载脚本();
                            dialog.dismiss();
                        } catch (Throwable e) {
                            traceLog("api_log.txt", "取消加载脚本按钮异常: " + e.getMessage());
                        }
                    }
                });

                btn2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            vibrate(activity, 50);
                            重新加载脚本();
                            dialog.dismiss();
                        } catch (Throwable e) {
                            traceLog("api_log.txt", "重新加载脚本按钮异常: " + e.getMessage());
                        }
                    }
                });

                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            vibrate(activity, 50);
                            dialog.dismiss();
                        } catch (Throwable e) {
                            traceLog("api_log.txt", "取消按钮异常: " + e.getMessage());
                        }
                    }
                });

                traceLog("api_log.txt", "重载/取消选择对话框已显示: 你想选哪个呢？");

            } catch (Throwable e) {
                traceLog("api_log.txt", "对话框创建失败: " + e.getMessage());
            }
        }
    });
}

public void Toast(String text) {
	if (Looper.myLooper() == Looper.getMainLooper()) {
		xToast(text);
		return;
	}

	try {
		if (uiHandler != null && uiHandler.getLooper() != null) {
			uiHandler.post(new Runnable() {
				public void run() {
					xToast(text);
				}
			});
			return;
		}
	} catch (Exception e) {}

	try {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				xToast(text);
			}
		});
	} catch (Exception e) {
		try {
			xToast(text);
		} catch (Exception fatal) {
			toast(text);
		}
	}
}

private void xToast(String text) {
    try {
        Context ctx = context;
        if (ctx == null) {
            Activity act = getNowActivity();
            if (act == null) act = 最后Activity;
            ctx = act;
        }
        if (ctx == null) {
            toast("" + text);
            return;
        }

        boolean isDark = isThemeDark(ctx instanceof Activity ? (Activity) ctx : null);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setGravity(Gravity.CENTER);
        root.setClickable(false);
        root.setFocusable(false);
        root.setFocusableInTouchMode(false);
        root.setOnTouchListener((v, event) -> false);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(isDark ? "#D9333333" : "#8CE0E0E0"));
        bg.setCornerRadius(dp(16));
        root.setBackground(bg);

        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(17);

        int[] TOAST_TEXT_COLORS;
        if (isDark) {
            TOAST_TEXT_COLORS = new int[]{
                Color.parseColor("#FF5252"),
                Color.parseColor("#4DB6AC"),
                Color.parseColor("#448AFF"),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#AB47BC"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#FFEE58")
            };
        } else {
            TOAST_TEXT_COLORS = new int[]{
                Color.parseColor("#C62828"),
                Color.parseColor("#00695C"),
                Color.parseColor("#1565C0"),
                Color.parseColor("#2E7D32"),
                Color.parseColor("#6A1B9A"),
                Color.parseColor("#E65100"),
                Color.parseColor("#F57F17")
            };
        }

        Random random = new Random();
        int randomColorIndex = random.nextInt(TOAST_TEXT_COLORS.length);
        tv.setTextColor(TOAST_TEXT_COLORS[randomColorIndex]);
        root.addView(tv);

        AnimationSet showAnim = new AnimationSet(true);
        ScaleAnimation scaleShow = new ScaleAnimation(
            0.8f, 1.0f,
            0.8f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        AlphaAnimation alphaShow = new AlphaAnimation(0.0f, 1.0f);
        scaleShow.setDuration(300);
        alphaShow.setDuration(250);
        scaleShow.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaShow.setInterpolator(new AccelerateDecelerateInterpolator());
        showAnim.addAnimation(scaleShow);
        showAnim.addAnimation(alphaShow);
        showAnim.setFillAfter(true);

        AnimationSet dismissAnim = new AnimationSet(true);
        ScaleAnimation scaleDismiss = new ScaleAnimation(
            1.0f, 0.8f,
            1.0f, 0.8f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        AlphaAnimation alphaDismiss = new AlphaAnimation(1.0f, 0.0f);
        scaleDismiss.setDuration(250);
        alphaDismiss.setDuration(200);
        scaleDismiss.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaDismiss.setInterpolator(new AccelerateDecelerateInterpolator());
        dismissAnim.addAnimation(scaleDismiss);
        dismissAnim.addAnimation(alphaDismiss);
        dismissAnim.setFillAfter(true);

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) {
                v.startAnimation(showAnim);
            }

            public void onViewDetachedFromWindow(View v) {
                v.startAnimation(dismissAnim);
            }
        });

        Toast toast = new Toast(ctx);
        toast.setView(root);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, dp(64));
        toast.show();
    } catch (Exception e) {
        toast("" + text);
        traceLog("api_log.txt", "" + e);
    }
}

boolean 应用状态() {
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    if (activity == null) return false;

    ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
    if (activityManager == null) return false;

    List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
    if (processes == null) return false;

    for (ActivityManager.RunningAppProcessInfo process : processes) {
        if (process.processName.equals(currentPackageName)) {
            return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        }
    }
    return false;
}

import me.yxp.qfun.plugin.loader.PluginManager;
import me.yxp.qfun.plugin.bean.PluginInfo;

volatile boolean isUnloading = false;
volatile Thread 卸载线程引用 = null;
volatile CountDownLatch 卸载完成信号 = null;

void 清理Handler(Handler handler) {
	if (handler == null) return;
	try {
		handler.removeCallbacksAndMessages(null);
	} catch (Throwable e) {}
}

void 关闭线程池() {
	if (ThreadPool == null) return;
	try {
		ThreadPool.shutdownNow();
	} catch (Throwable e) {}
	ThreadPool = null;
}

void performDataCleanup() {
	if (writeLock == null) return;
	synchronized(writeLock) {
		try {
			if (OP_STATS != null) OP_STATS.clear();
			if (messageBatchQueue != null) messageBatchQueue.clear();
			if (CHANGED_KEYS != null) CHANGED_KEYS.clear();
			if (cardExpandStatus != null) cardExpandStatus.clear();
			if (statsTextViewCache != null) statsTextViewCache.clear();
			if (weekDatesCache != null) weekDatesCache.clear();
			if (monthDatesCache != null) monthDatesCache.clear();
		} catch (Throwable e) {}
	}
	todayDateStr = null;
}

void performUiCleanup() {
	final Dialog dialog = statsDialog;
	final Handler handle = msgHandle;
	try {
		if (dialog != null) {
			try {
				if (dialog.isShowing()) dialog.dismiss();
			} catch (Throwable e) {}
			statsDialog = null;
		}
		if (handle != null) {
			handle.removeCallbacksAndMessages(null);
			msgHandle = null;
		}
		dialogVisible = false;
	} catch (Throwable e) {}
}

void onUnMsgload() {
	traceLog("api_log.txt", "====== api3卸载开始 ======");

	synchronized(this) {
		if (isUnloading) return;
		isUnloading = true;
		卸载线程引用 = Thread.currentThread();
		卸载完成信号 = new CountDownLatch(1);
	}

	try {
		stopWriteThread();
		performDataCleanup();

		Activity activity = getNowActivity();
		if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
			final CountDownLatch 信号 = new CountDownLatch(1);
			activity.runOnUiThread(new Runnable() {
				public void run() {
					performUiCleanup();
					信号.countDown();
				}
			});
			try {
				信号.await(3, TimeUnit.SECONDS);
			} catch (Throwable e) {}
		}

	} catch (Throwable e) {
		traceLog("api_log.txt", "卸载异常: " + e.toString());
	}
	traceLog("api_log.txt", "====== api3卸载完成 ======");
}

void a卸载悬浮窗() {
	Activity activity = getNowActivity();
	if (activity == null) activity = 最后Activity;

	if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
		final CountDownLatch 信号 = new CountDownLatch(1);
		activity.runOnUiThread(new Runnable() {
			public void run() {
				try {
					卸载悬浮窗();
				} catch (Throwable e) {}
				信号.countDown();
			}
		});
		try {
			信号.await(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
	} else {
		try {
			卸载悬浮窗();
		} catch (Throwable e) {}
	}
}

void 卸载脚本() {
	traceLog("api_log.txt", "卸载脚本入口 - 线程: " + Thread.currentThread().getName());

	initThreadPool();

	// ThreadPool.execute(new Runnable() {
	// public void run() {
	boolean isUIThread = "main".equals(Thread.currentThread().getName());

	if (!isUIThread && getNowActivity() != null) {
		final CountDownLatch 信号 = new CountDownLatch(1);
		// new Handler(Looper.getMainLooper()).post(new Runnable() {
			// public void run() {
				// toast("测试");
				try {
					执行卸载核心逻辑();
				} catch (Throwable e) {}
				信号.countDown();
			// }
		// });
		try {
			信号.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
	} else {
		执行卸载核心逻辑();
	}
	// }
	// });
	异步关闭线程池();
}

void 执行卸载核心逻辑() {
	traceLog("api_log.txt", "====== 完整卸载开始 ======");

	try {
		unhookAll();
		onUnMsgload();
		a卸载悬浮窗();
		putBoolean("settings", "开关", false);
		// new Handler(Looper.getMainLooper()).post(new Runnable() {
			// public void run() {
				unloadBackgroundCache();
				清理Handler(uiHandler);
				uiHandler = null;
				延迟启动完成 = false;
				悬浮窗显示状态 = false;
				应用前台状态 = false;
				Toast("脚本卸载完成，欢迎下次使用");
			// }
		// });
	} catch (Throwable e) {
		traceLog("api_log.txt", "卸载失败: " + e.toString());
		Toast("卸载失败：" + e.getMessage());
	}
}

void 异步关闭线程池() {
	new Thread(new Runnable() {
		public void run() {
			try {
				Thread.sleep(50);
				关闭线程池();
			} catch (Throwable e) {}
		}
	}).start();
}

void 重新加载脚本() {
	if (uiHandler != null) {
		uiHandler.postDelayed(new Runnable() {
			public void run() {
				Activity activity = getNowActivity();
				if (activity == null || activity.isFinishing()) activity = 最后Activity;
				if (activity != null && !activity.isFinishing()) 重新加载操作(activity);
			}
		}, 300);
	} else {
		Activity activity = getNowActivity();
		if (activity == null || activity.isFinishing()) activity = 最后Activity;
		if (activity != null && !activity.isFinishing()) 重新加载操作(activity);
	}
}

void 取消加载脚本() {
	if (uiHandler != null) {
		uiHandler.postDelayed(new Runnable() {
			public void run() {
				Activity activity = getNowActivity();
				if (activity == null || activity.isFinishing()) activity = 最后Activity;
				if (activity != null && !activity.isFinishing()) 取消加载操作(activity);
			}
		}, 300);
	} else {
		Activity activity = getNowActivity();
		if (activity == null || activity.isFinishing()) activity = 最后Activity;
		if (activity != null && !activity.isFinishing()) 取消加载操作(activity);
	}
}

void 重新加载操作(Activity activity) {
	try {
		PluginManager pm = PluginManager.INSTANCE;
		if (pm == null) return;

		List list = pm.getPlugins();
		if (list == null) return;

		PluginInfo target = null;
		int n = list.size();

		for (int i = 0; i < n; i++) {
			try {
				Object obj = list.get(i);
				if (obj == null || !"me.yxp.qfun.plugin.bean.PluginInfo".equals(obj.getClass().getName())) continue;

				PluginInfo p = (PluginInfo) obj;
				if ("QFloatingX".equals(p.getId()) && p.isRunning()) {
					target = p;
					break;
				}
			} catch (Throwable e) {
				continue;
			}
		}

		if (target != null) pm.reloadPlugin(target);
	} catch (Throwable e) {
	}
}

void 取消加载操作(Activity activity) {
	try {
		PluginManager pm = PluginManager.INSTANCE;
		if (pm == null) return;

		List list = pm.getPlugins();
		if (list == null) return;

		PluginInfo target = null;
		int n = list.size();

		for (int i = 0; i < n; i++) {
			try {
				Object obj = list.get(i);
				if (obj == null || !"me.yxp.qfun.plugin.bean.PluginInfo".equals(obj.getClass().getName())) continue;

				PluginInfo p = (PluginInfo) obj;
				if ("QFloatingX".equals(p.getId()) && p.isRunning()) {
					target = p;
					break;
				}
			} catch (Throwable e) {
				continue;
			}
		}

		if (target != null) pm.stopPlugin(target);
	} catch (Throwable e) {
	}
}

void unLoadPlugin() {
	try {
		if (ThreadPool == null || ThreadPool.isShutdown()) initThreadPool();
		if (isUnloading) isUnloading = false;
		if (卸载完成信号 != null) {
			卸载完成信号.countDown();
			卸载完成信号 = null;
		}
	} catch (Throwable e) {}

	卸载脚本();

}

/**
 * 更新进度回调接口
 */
interface ProgressCallback {
    void onProgress(int progress);
    void onProgressTip(String tip);
}

/**
 * 从指定URL下载文件到本地路径
 * @param url 下载链接
 * @param savePath 本地保存路径
 * @param callback 进度回调
 * @return 下载是否成功
 */
boolean downloadFile(String url, String savePath, ProgressCallback callback) {
    boolean success = false;
    java.io.FileOutputStream out = null;
    java.io.InputStream in = null;
    final int BUF_SIZE = 8192;
    final int PROGRESS_STEP_BUF = 10;
    int downloadedBufCount = 0;
    int currentProgress = 0;
    java.io.File saveFile = new java.io.File(savePath);
    java.io.File saveDir = saveFile.getParentFile();
    if (saveDir != null && !saveDir.exists()) {
        saveDir.mkdirs();
    }
    try {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept-Encoding", "identity");
        int remoteSize = conn.getContentLength();
        if (callback != null) {
            if (remoteSize == -1) {
                callback.onProgressTip("正在下载...");
                callback.onProgress(0);
            } else {
                callback.onProgressTip("总大小: " + (remoteSize / 1024) + "KB");
                callback.onProgress(0);
            }
        }

        in = conn.getInputStream();
        out = new java.io.FileOutputStream(saveFile);

        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
            total += read;
            downloadedBufCount++;

            if (callback != null) {
                if (remoteSize == -1) {
                    if (downloadedBufCount % PROGRESS_STEP_BUF == 0) {
                        currentProgress += 10;
                        if (currentProgress > 90) currentProgress = 90;
                        callback.onProgress(currentProgress);
                        callback.onProgressTip("已下载: " + (total / 1024) + "KB");
                    }
                } else {
                    int progress = (int) ((total * 100) / remoteSize);
                    callback.onProgress(progress);
                    callback.onProgressTip("已下载: " + (total / 1024) + "KB/" + (remoteSize / 1024) + "KB");
                }
            }
        }
        out.flush();

        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("下载完成");
        }

        if (remoteSize == -1) {
            success = (total > 0);
        } else {
            success = (total == remoteSize);
        }
        if (!success && saveFile.exists()) {
            saveFile.delete();
        }

    } catch (Throwable e) {
        if (saveFile.exists()) saveFile.delete();
        traceLog("main_log", "downloadFile 异常: " + e.getMessage());
    } finally {
        try { if (out != null) out.close(); } catch (Throwable t) {}
        try { if (in != null) in.close(); } catch (Throwable t) {}
    }
    return success;
}

/**
 * 解压ZIP文件到目标目录
 * @param zipPath ZIP文件路径
 * @param destDir 目标目录
 * @param callback 进度回调
 * @return 解压是否成功
 */
boolean unzipFile(String zipPath, String destDir, ProgressCallback callback) {
    boolean success = false;
    java.io.File zipFile = new java.io.File(zipPath);
    java.io.File destDirFile = new java.io.File(destDir);
    if (!destDirFile.exists()) {
        destDirFile.mkdirs();
    }
    try {
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(zipFile);
        java.util.Enumeration entries = zip.entries();
        int totalEntry = 0;
        int currentEntry = 0;
        String zipRootDir = "";

        while (entries.hasMoreElements()) {
            totalEntry++;
            java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) entries.nextElement();
            if (zipRootDir.isEmpty() && !entry.isDirectory()) {
                String name = entry.getName();
                int idx = name.indexOf("/");
                if (idx > 0) zipRootDir = name.substring(0, idx + 1);
            }
        }

        entries = zip.entries();
        if (callback != null) {
            callback.onProgressTip("开始解压文件");
            callback.onProgress(0);
        }

        while (entries.hasMoreElements()) {
            currentEntry++;
            java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) entries.nextElement();
            String name = entry.getName();

            if (!zipRootDir.isEmpty() && name.startsWith(zipRootDir)) {
                name = name.substring(zipRootDir.length());
                if (name.isEmpty()) continue;
            }

            java.io.File entryFile = new java.io.File(destDirFile, name);
            if (entry.isDirectory()) {
                entryFile.mkdirs();
                continue;
            }

            entryFile.getParentFile().mkdirs();
            java.io.InputStream zin = zip.getInputStream(entry);
            java.io.FileOutputStream fout = new java.io.FileOutputStream(entryFile);
            byte[] buf = new byte[4096];
            int r;
            while ((r = zin.read(buf)) != -1) fout.write(buf, 0, r);
            fout.close();
            zin.close();

            if (callback != null) {
                callback.onProgress((int) ((currentEntry * 100) / totalEntry));
                callback.onProgressTip("解压: " + name);
            }
        }
        zip.close();
        success = true;
        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("解压完成");
        }

    } catch (Throwable e) {
        success = false;
        traceLog("main_log", "unzipFile 异常: " + e.getMessage());
    }
    return success;
}

/**
 * 显示更新对话框并执行文件列表下载逻辑
 * @param version 远程版本号
 * @param versionType 远程版本类型（正式版/测试版）
 * @param updateType 更新类型（全量/补丁）
 * @param changelog 更新日志
 * @param updateFiles 需要下载的文件列表
 */
void showUpdateDialog(final String version, final String versionType, final String updateType, final String changelog, final List updateFiles, final String count) {

    Activity activity = getNowActivity();
    
    activity.runOnUiThread(new Runnable() {
        public void run() {
        
            boolean isDark = isThemeDark(activity);

            StringBuilder message = new StringBuilder();
            message.append("本次为").append(updateType).append("更新！全网用户已累计"+count+"w+\n\n");
            message.append("新版本为").append(versionType).append(" ").append(version).append(" 确定要更新嘛～\n");
            message.append("点击确定更新后将自动更新并重启脚本\n\n");
            message.append(changelog.replace("\n", "\n")).append("\n\n");
            
            if (updateFiles != null && !updateFiles.isEmpty()) {
                message.append("(需下载 ").append(updateFiles.size()).append(" 个文件)");
            }

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity,
                isDark ? android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK : android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            builder.setTitle("发现更新啦～").setMessage(message.toString()).setCancelable(false);

            builder.setPositiveButton("立即更新", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    final android.app.ProgressDialog progress = new android.app.ProgressDialog(activity,
                        isDark ? android.app.ProgressDialog.THEME_DEVICE_DEFAULT_DARK : android.app.ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
                    progress.setCancelable(false);
                    progress.show();
                    ThreadPool.execute(new Runnable() {
                        public void run() {
                            boolean allSuccess = true;
                            if (updateFiles == null || updateFiles.isEmpty()) {
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        progress.dismiss();
                                        Toast("没有需要更新的文件");
                                    }
                                });
                                return;
                            }

                            int total = updateFiles.size();
                            
                            for (int i = 0; i < total; i++) {
                                final String fileName = (String) updateFiles.get(i);
                                final int currentIndex = i + 1;
                                final int remaining = total - currentIndex;
                                
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        String displayName = fileName;
                                        if (displayName.startsWith("QFloatingX/")) {
                                            displayName = displayName.substring("QFloatingX/".length());
                                        }
                                        progress.setMessage("当前正在下载更新 " + displayName + "\n剩余 " + remaining + " 个文件");
                                        progress.setProgress((int)((currentIndex - 1) * 100.0 / total));
                                    }
                                });

                                String fileUrl = "https://gitee.com/ovoxiaomo/qfloating-x/raw/QF/" + fileName;
                                
                                String relativePath = fileName;
                                if (relativePath.startsWith("QFloatingX/")) {
                                    relativePath = relativePath.substring("QFloatingX/".length());
                                }
                                String savePath = pluginPath + "/" + relativePath;
                                if (!downloadFile(fileUrl, savePath, null)) {
                                    allSuccess = false;
                                    traceLog("main_log", "下载失败: " + fileName);
                                    break;
                                }
                            }

                            final boolean res = allSuccess;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    progress.dismiss();
                                    if (res) {
                                        Toast("更新成功，正在重启");
                                        重新加载脚本();
                                    } else {
                                        Toast("更新过程中出现错误");
                                    }
                                }
                            });
                        }
                    });
                }
            });

            builder.setNegativeButton("取消", null);
            builder.setNeutralButton("忽略此版本", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    putString("更新检测", "已忽略版本", version);
                }
            });

            android.app.AlertDialog dialogObj = builder.create();
            dialogObj.show();
            applyUiTheme(activity, dialogObj);
        }
    });
}

/**
 * 执行更新检查流程，解析 up.json 并比对版本号
 */
void checkQFXUpdate() {
    ThreadPool.execute(new Runnable() {
        public void run() {
            String ignored = getString("更新检测", "已忽略版本", "");
            String jsonStr = get("https://gitee.com/ovoxiaomo/qfloating-x/raw/QF/up.json");
            String jsonStr2  = get("https://cn.apihz.cn/api/jisuan/jishuqi2.php?id=10013224&key=17e1755199ff8eebc2fd58bce20d950e&type=2&number=2");
            if (jsonStr == null || jsonStr.isEmpty()) return;

            try {
                JSONObject json  = new JSONObject(jsonStr);
                JSONObject json2 = new JSONObject(jsonStr2);
                String count = json2.optString("number2", "0");
                String remoteVersion = json.optString("version", "0.0.0");
                String versionType = json.optString("versionType", "正式版");
                String updateType = json.optString("updateType", "全量");
                String changelog = json.optString("changelog", "");

                java.util.List files = new java.util.ArrayList();
                org.json.JSONArray filesArray = json.optJSONArray("files");
                if (filesArray != null) {
                    for (int i = 0; i < filesArray.length(); i++) {
                        files.add(filesArray.getString(i));
                    }
                }

                if (remoteVersion.equals(ignored)) return;

                String localVersion = readprop(pluginPath + "/info.prop", "versionCode");
                if (localVersion == null) localVersion = "0.0.0";

                if (!remoteVersion.equals(localVersion)) {
                    showUpdateDialog(remoteVersion, versionType, updateType, changelog, files, count);
                }
            } catch (Throwable t) {
                traceLog("main_log", "checkQFXUpdate 异常: " + t.getMessage());
            }
        }
    });
}

boolean checkAllIconsExist() {

    String iconBase = extractBasePath(iconPath);
    String closeBase = extractBasePath(closeIconPath);
    
    // 检查icon
    if (!checkWithSuffixes(iconBase, new String[]{".png", ".gif"})) {
        return false;
    }
    
    // 检查closeIcon
    if (!checkWithSuffixes(closeBase, new String[]{".png", ".gif"})) {
        return false;
    }
    
    // 检查固定png文件
    if (!new java.io.File(pluginPath + "/API/QQ.png").exists()) {
        return false;
    }
    
    if (!new java.io.File(pluginPath + "/API/GitHub.png").exists()) {
        return false;
    }
    
    return true;
}

// 提取基础路径（去掉已有后缀）
String extractBasePath(String fullPath) {
    if (fullPath == null || fullPath.trim().isEmpty()) return "";
    int lastDot = fullPath.lastIndexOf('.');
    int lastSlash = fullPath.lastIndexOf('/');
    if (lastDot > lastSlash && lastDot > 0) {
        return fullPath.substring(0, lastDot);
    }
    return fullPath;
}

// 检查后缀路径
boolean checkWithSuffixes(String basePath, String[] suffixes) {
    if (basePath == null || basePath.trim().isEmpty()) return false;
    try {
        for (String suffix : suffixes) {
            java.io.File f = new java.io.File(basePath + suffix);
            if (f.exists() && f.length() > 0) return true;
        }
    } catch (Exception e) {}
    return false;
}

/**
 * 执行下载并解压（FutureTask风格）
 * @param activity Activity上下文
 * @return true=成功，false=失败
 */
boolean performDownloadAndUnzip() {
    final String downloadUrl = "https://gitee.com/ovoxiaomo/qfloating-x/raw/QF/icon.zip";
    final String tempZipPath = pluginPath + "/API/icon.zip";
    final String destDir = pluginPath + "/API/";
    
    java.util.concurrent.FutureTask<Boolean> downloadTask = 
        new java.util.concurrent.FutureTask<Boolean>(
            new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return downloadFile(downloadUrl, tempZipPath, new ProgressCallback() {
                        public void onProgress(int progressVal) {
                        }
                        public void onProgressTip(String tip) {
                        }
                    });
                }
            }
        );
    
    ThreadPool.execute(downloadTask);
    boolean downloadResult = false;
    
    try {
        downloadResult = downloadTask.get(15000, java.util.concurrent.TimeUnit.MILLISECONDS).booleanValue();
    } catch (Throwable e) {
        downloadResult = false;
    }
    
    if (!downloadResult) {
        cleanupTempFile(tempZipPath);
        return false;
    }
    
    java.util.concurrent.FutureTask<Boolean> unzipTask = 
        new java.util.concurrent.FutureTask<Boolean>(
            new java.util.concurrent.Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return unzipFile(tempZipPath, destDir, new ProgressCallback() {
                        public void onProgress(int progressVal) {
                        }
                        
                        public void onProgressTip(String tip) {
                        }
                    });
                }
            }
        );
    
    ThreadPool.execute(unzipTask);
    boolean unzipResult = false;
    
    try {
        unzipResult = unzipTask.get(30000, java.util.concurrent.TimeUnit.MILLISECONDS).booleanValue();
    } catch (Throwable e) {
        traceLog("api_log.txt", " 解压超时/异常: " + e.getMessage());
        unzipResult = false;
    }
    
    cleanupTempFile(tempZipPath);
    
    return unzipResult;
}

/**
 * 清理临时文件
 */
void cleanupTempFile(String tempPath) {
    try {
        java.io.File file = new java.io.File(tempPath);
        if (file.exists()) {
            boolean deleted = file.delete();
        }
    } catch (Exception e) {
        traceLog("api_log.txt", "异常: " + e.getMessage());
    }
}

/**
 * 下载解压完成后二次验证
 */
boolean verifyAfterDownload() {
    return checkAllIconsExist();
}


/**
 * 主入口：确保图标资源可用
 * 只有检测到缺失时才提示"正在后台下载..."
 */
void ensureResourceAvailable() {

    boolean allExist = checkAllIconsExist();
    
    if (allExist) {
        return;
    }
    
    Toast("检测到图标文件缺失，正在为您后台下载中...");
    
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                boolean success = performDownloadAndUnzip();
                
                if (success) {
                    final boolean verifyResult = verifyAfterDownload();
                    
                            if (verifyResult) {
                                Toast("下载图标文件成功！");
                            } else {
                                Toast("下载完成，但文件验证失败");
                                traceLog("api_log.txt", "资源下载但验证失败");
                            }
                } else {
                            Toast("图标文件下载失败，请检查网络");
                            traceLog("api_log.txt", "资源准备失败");
                }
                
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                traceLog("api_log.txt", " 致命异常: " + errorMsg);
                Toast("图标文件准备失败: " + errorMsg);
            }
        }
    });
}

public void openPlugin(int functionType, String groupId, String userName) {
    跳转到页面("me.yxp.qfun.activity.PluginActivity");
}

public void openSetting(int functionType, String groupId, String userName) {
    跳转到页面("me.yxp.qfun.activity.SettingActivity");
}

void 跳转到页面(String className) {
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    if (activity == null) return;
    
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_MAIN);
    intent.setComponent(new ComponentName(currentPackageName, className));
    try {
        activity.startActivity(intent);
        traceLog("api_log.txt","跳转到页面: " + className);
    } catch (Exception e) {
        traceLog("api_log.txt","跳转页面失败: " + e);
    }
}
