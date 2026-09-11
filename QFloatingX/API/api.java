
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
        TroopClockInHandler inHandler;
        try {
            inHandler = new TroopClockInHandler(app);
        } catch (Throwable e) {
            try {
                inHandler = new TroopClockInHandler();
            } catch (Throwable ex) {
                qqToast(1, "创建处理器失败");
                return false;
            }
        }

        Method[] methods = TroopClockInHandler.class.getDeclaredMethods();
        for (Method m : methods) {
            Class[] paramTypes = m.getParameterTypes();
            ArrayList args = new ArrayList();
            int strCount = 0;
            
            boolean match = true;
            for (Class type : paramTypes) {
                if (type == String.class || type == CharSequence.class) {
                    args.add(strCount == 0 ? qun : uin);
                    strCount++;
                } 
                else if (type == int.class || type == Integer.class) {
                    args.add(0);
                } 
                else if (type == boolean.class || type == Boolean.class) {
                    args.add(true);
                } 
                else if (type == long.class || type == Long.class) {
                    args.add(0L);
                } 
                else {
                    match = false;
                    break;
                }
            }

            if (match && strCount >= 2) {
                try {
                    m.setAccessible(true);
                    m.invoke(inHandler, args.toArray());
                    return true;
                } catch (Throwable invE) {
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
                    } catch (Throwable e) { traceLog("api_log", "[sendHighPriorityNotification] 异常: " + e); }
                }
            });
        } else {
            try {
                notificationManager.notify(notificationId, notification);
            } catch (Throwable e) { traceLog("api_log", "[sendHighPriorityNotification] 异常: " + e); }
        }
        
    } catch (Throwable e) { traceLog("api_log", "[sendHighPriorityNotification] 异常: " + e); }
}

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
		traceLog("api_log", "[getbitmap] 加载失败: " + e.getMessage());
		Toast("图片加载错误: " + e.getMessage());
		return Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888);
	} finally {
		if (stream != null) {
			try {
				stream.close();
			} catch (Throwable closeE) {
				traceLog("api_log", "[getbitmap] 流关闭失败: " + closeE.getMessage());
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
		traceLog("api_log", "[getroundbmp] 圆角处理失败: " + e.getMessage());
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

		traceLog("api_log", "[bmptofile] 图片已保存: " + path);
	} catch (Throwable e) {
		traceLog("api_log", "[bmptofile] 保存失败: " + e.getMessage());
		Toast("保存失败: " + e.getMessage());
	} finally {
		if (fs != null) {
			try {
				fs.close();
			} catch (Throwable closeE) {
				traceLog("api_log", "[bmptofile] 文件流关闭失败: " + closeE.getMessage());
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
		traceLog("api_log", "[urltofile] 下载失败: " + e.getMessage());
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
				traceLog("api_log", "[fsdx] 缩放失败: " + e.getMessage());
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
				traceLog("api_log", "[fsdx] 叠加失败: " + e.getMessage());
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
					pt.setColor(pc(color));
				}

				float x1 = ((Number) x).floatValue() * bmp.getWidth() - 0.5f* text.length() * ((Number) size).floatValue();
				float y1 = ((Number) y).floatValue() * bmp.getHeight() + 0.5f* ((Number) size).floatValue();

				cas.drawText(text, x1, y1, pt);

				String savePath = ("".equals(path) || path == null) ? path1 : path;
				bmptofile(bmp, savePath);
			} catch (Throwable e) {
				traceLog("api_log", "[fsdx] 文字写入失败: " + e.getMessage());
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
			traceLog("api_log", "[删除] 文件不存在: " + Path);
		}
	} catch (Exception e) {
		traceLog("api_log", "[删除] 删除文件时发生错误: " + e);
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
						traceLog("api_log", "[删除文件夹] 删除子文件夹失败: " + 子项.getAbsolutePath());
					}
				} else {
					if (!子项.delete()) {
						所有子项删除成功 = false;
						traceLog("api_log", "[删除文件夹] 删除文件失败: " + 子项.getAbsolutePath());
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
		traceLog("api_log", "[删除文件夹] 删除文件夹过程中异常: " + e);
		return false;
	} finally {
		if (记录异常 != null) {
			traceLog("api_log", "[删除文件夹] 未完全删除" + 记录异常);
		}
	}
}


/** UI背景类型：color / gradient / image */
String getUiBgType() {
	return getString("settings", "ui_bg_type", "color");
}

/** 统一剩余时长（秒）→ 中文描述 */
String formatRemainingTime(long seconds) {
	if (seconds <= 0) return "0秒";
	if (seconds < 60) return seconds + "秒";
	if (seconds < 3600) return (seconds / 60) + "分钟" + (seconds % 60) + "秒";
	if (seconds < 86400) return (seconds / 3600) + "小时" + ((seconds % 3600) / 60) + "分钟";
	return (seconds / 86400) + "天" + ((seconds % 86400) / 3600) + "小时";
}

/** 统一剩余时长（毫秒）→ 中文描述；≤0 返回立即执行 */
String formatRemainingTimeMs(long ms) {
	if (ms <= 0) return "立即执行";
	return formatRemainingTime(ms / 1000);
}

/** 分类卡片底色：三种背景样式都有；图片背景透明 */
int getAdaptiveCardBg(Activity a) {
	if (a == null) return pc("#33FFFFFF");
	boolean dark = isThemeDark(a);
	String bgType = getUiBgType();
	if ("image".equals(bgType)) return Color.TRANSPARENT;
	if ("gradient".equals(bgType)) {
		int s = pc(getSettingsThemeColor(a, "surface"));
		return Color.argb(dark ? 150 : 170, Color.red(s), Color.green(s), Color.blue(s));
	}
	return mixTowardElevated(pc(getSettingsThemeColor(a, "background")), dark);
}

/** 输入框底色：图片半透明少遮挡；纯色/渐变用有对比 surface */
int getAdaptiveInputBg(Activity a) {
	if (a == null) return pc("#22FFFFFF");
	boolean dark = isThemeDark(a);
	String bgType = getUiBgType();
	if ("image".equals(bgType)) {
		int s = pc(getSettingsThemeColor(a, "surface"));
		return Color.argb(dark ? 70 : 90, Color.red(s), Color.green(s), Color.blue(s));
	}
	return pc(getSettingsThemeColor(a, "surface"));
}

/** 半屏/弹窗填充底色 */
int getAdaptiveSheetBg(Activity a) {
	if (a == null) return pc("#FF1E1E1E");
	boolean dark = isThemeDark(a);
	String bgType = getUiBgType();
	if ("image".equals(bgType)) {
		int b = pc(getSettingsThemeColor(a, "background"));
		return Color.argb(dark ? 200 : 220, Color.red(b), Color.green(b), Color.blue(b));
	}
	if ("gradient".equals(bgType)) {
		int s = pc(getSettingsThemeColor(a, "surface"));
		return Color.argb(dark ? 230 : 235, Color.red(s), Color.green(s), Color.blue(s));
	}
	return pc(getSettingsThemeColor(a, "background"));
}

/** 菜单项卡片底色：始终比半屏背景更浅 */
int getAdaptiveMenuItemBg(Activity a) {
	if (a == null) return pc("#FF3A3A3A");
	boolean dark = isThemeDark(a);
	String bgType = getUiBgType();
	int base;
	if ("image".equals(bgType)) {
		return dark ? Color.argb(90, 255, 255, 255) : Color.argb(70, 255, 255, 255);
	}
	if ("gradient".equals(bgType)) {
		base = pc(getSettingsThemeColor(a, "surface"));
	} else {
		base = pc(getSettingsThemeColor(a, "background"));
	}
	int r = Color.red(base);
	int g = Color.green(base);
	int b = Color.blue(base);
	if (dark) {
		return Color.argb(255,
			Math.min(255, r + 52),
			Math.min(255, g + 52),
			Math.min(255, b + 52));
	}
	return Color.argb(255,
		r + (255 - r) * 45 / 100,
		g + (255 - g) * 45 / 100,
		b + (255 - b) * 45 / 100);
}

/** 设置页条目底色：半透明，让分类卡片底色透出来 */
int getAdaptiveSettingsItemBg(Activity a) {
	if (a == null) return Color.argb(30, 255, 255, 255);
	boolean dark = isThemeDark(a);
	return dark ? Color.argb(40, 255, 255, 255) : Color.argb(28, 255, 255, 255);
}

/** 颜色抬升为卡片色：浅色压暗、深色提亮，保证自定义纯色下可见 */
int mixTowardElevated(int color, boolean dark) {
	int r = Color.red(color);
	int g = Color.green(color);
	int b = Color.blue(color);
	if (dark) {
		return Color.argb(255,
			Math.min(255, r + 36),
			Math.min(255, g + 36),
			Math.min(255, b + 36));
	}
	return Color.argb(255,
		r * 92 / 100,
		g * 92 / 100,
		b * 92 / 100);
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
	if (digitGroups < 0) digitGroups = 0;
	if (digitGroups > units.length - 1) digitGroups = units.length - 1;
	StringBuffer result = new StringBuffer();
	result.append(new java.text.DecimalFormat("#,##0.##").format(bytes / Math.pow(1024, digitGroups)));
	result.append(units[digitGroups]);
	return result.toString();
}
long getFileSize(File file) {
	if (file == null) {
		traceLog("api_log", "[getFileSize] getFileSize参数为null");
		return 0;
	}
	try {
		return file.length();
	} catch (Exception e) {
		traceLog("api_log", "[getFileSize] 获取文件大小失败: " + file.getName() + "    " + e);
		return 0;
	}
}

// 递归文件夹大小计算 - 依赖getFileSize
// 递归文件夹大小计算
long getFolderSize(File folder) {
	if (folder == null || !folder.exists()) {
		traceLog("api_log", "[getFolderSize] getFolderSize文件夹不存在: " + folder);
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
		traceLog("api_log", "[getFolderSize] 遍历文件夹失败: " + folder.getName() + "    " + e);
	}
	return size;
}

String getFormattedSize(long sizeInBytes) {
	if (sizeInBytes <= 0) {
		return "0KB";
	}
	java.text.DecimalFormat df = new java.text.DecimalFormat("0.###");
	double size = sizeInBytes / 1024.0;
	String unit = "KB";
	if (size >= 1048576.0) {
		size = size / 1048576.0;
		unit = "GB";
	} else if (size >= 1024.0) {
		size = size / 1024.0;
		unit = "MB";
	}
	return df.format(size) + unit;
}
String getFormattedSize(File folder) {
	if (folder == null) {
		traceLog("api_log", "[getFormattedSize] getFormattedSize(File)参数为null");
		return "文件夹不存在";
	}
	if (!folder.exists()) {
		traceLog("api_log", "[getFormattedSize] 文件夹不存在: " + folder.getAbsolutePath());
		return "文件夹不存在";
	}
	if (!folder.isDirectory()) {
		traceLog("api_log", "[getFormattedSize] 路径不是文件夹: " + folder.getAbsolutePath());
		return "不是有效文件夹";
	}

	try {
		long sizeInBytes = getFolderSize(folder);
		if (sizeInBytes == 0) {
			return "0KB";
		}
		return getFormattedSize(sizeInBytes);
	} catch (Exception e) {
		traceLog("api_log", "[getFormattedSize] 格式化文件夹大小失败: " + folder.getName() + "    " + e);
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
		traceLog("api_log", "[readprop] readprop接收null参数: file=" + file + ", key=" + name2);
		return "";
	}

	String text = null;
	try {
		text = 读(file);
		if (text == null || text.trim().isEmpty()) {
			traceLog("api_log", "[readprop] properties文件内容为空: " + file);
			return "";
		}
	} catch (Exception e) {
		traceLog("api_log", "[readprop] 读取properties文件失败: " + file + "    " + e);
		return "";
	}

	Properties props = new Properties();
	StringReader reader = null;
	try {
		reader = new StringReader(text);
		props.load(reader);
		String value = props.getProperty(name2);
		if (value == null) {
			traceLog("api_log", "[readprop] properties键不存在: " + name2 + " in " + file);
			return "";
		}
		return value;
	} catch (IOException e) {
		traceLog("api_log", "[readprop] Properties加载失败: " + file + "    " + e);
		return "";
	} catch (Exception e) {
		traceLog("api_log", "[readprop] Properties解析异常: " + file + "    " + e);
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
		traceLog("api_log", "[写]  【写入失败】路径为空");
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
				traceLog("api_log", "[写]  【写入失败】创建目录失败: " + parentDir.getAbsolutePath());
				return;
			}
		}

		// 创建文件（如果不存在）
		if (!file.exists() && !file.createNewFile()) {
			traceLog("api_log", "[写]  【写入失败】创建文件失败: " + Path);
			return;
		}

		// 写入数据（使用UTF-8编码）
		fos = new FileOutputStream(file);
		osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
		osw.write(WriteData);
		osw.flush();
		fos.flush();
		fos.getFD().sync(); // 确保数据持久化到磁盘

		traceLog("api_log", "[写]  【写入成功】 " + Path + " (" + WriteData.length() + "字节)");

	} catch (IOException e) {
		traceLog("api_log", "[写]  【写入异常】 " + Path + " - " + e.getMessage());
	} catch (Exception e) {
		traceLog("api_log", "[写]  【写入异常】 " + Path + " - " + e.getMessage());
	} finally {
		// 在finally中关闭流
		try {
			if (osw != null) {
				osw.close();
			}
		} catch (Exception e) {
			traceLog("api_log", "[写]  【关闭writer失败】 " + e.getMessage());
		}

		try {
			if (fos != null) {
				fos.close();
			}
		} catch (Exception e) {
			traceLog("api_log", "[写]  【关闭stream失败】 " + e.getMessage());
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
			traceLog("api_log", "[log大小限制] 文件夹不存在: " + Path);
			return;
		}
		if (!targetFile.isDirectory()) {
			traceLog("api_log", "[log大小限制] 目标路径不是文件夹: " + Path);
			return;
		}

		long 文件夹总大小 = getFolderSize(targetFile);
		int 阈值MB = 1;
		try { 阈值MB = Integer.parseInt(getString("settings", "log_delete_threshold", "1")); } catch (Throwable e) { traceLog("api_log", "[log大小限制] 异常: " + e); }
		long MB = 阈值MB * 1024 * 1024;

		if (文件夹总大小 > MB) {
			traceLog("api_log", "[log大小限制] 文件夹总大小超过" + (MB / 1024 / 1024) + "MB，准备删除: " + targetFile.getName() +
				" (" + 文件夹总大小 + " 字节)");

			boolean 删除成功 = 删除文件夹(targetFile);
			if (删除成功) {
				traceLog("api_log", "[log大小限制] 成功删除文件夹: " + Path);
			} else {
				traceLog("api_log", "[log大小限制] 删除文件夹失败: " + Path);
			}
		}
	} catch (Exception e) {
		traceLog("api_log", "[log大小限制] 处理log文件夹时出错: " + e);
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
			textView.setTextColor(isDark ? pc("#FFEFEFEF") : pc("#FF000000"));
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
            
			applyUiTheme(ThisActivity, dialog, 0);
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
			String uc = Integer.toHexString((int) c);
			while (uc.length() < 4) uc = "0" + uc;
			result.append("\\u").append(uc);
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


// 3. Hex (16进制)
String stringToHex(String str) {
	try {
		StringBuilder sb = new StringBuilder();
		byte[] bytes = str.getBytes("UTF-8");
		for (byte b: bytes) sb.append(hexByte(b).toUpperCase());
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
		traceLog("api_log", "[vibrate] 震动异常: " + e);
	}
}

// 默认经纬度（天安门）
double 默认经度 = 116.397128;
double 默认纬度 = 39.907500;
private void showLocationDialog(Activity activity) {
    boolean isDark = isThemeDark(activity);
    int textColor = isDark ? pc("#FFEFEFEF") : pc("#FF000000");
    int subTextColor = isDark ? pc("#99EFEFEF") : pc("#99000000");

    LinearLayout layout = new LinearLayout(activity);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(dp(activity, 20), dp(activity, 15), dp(activity, 20), dp(activity, 15));

    TextView tvLongitude = new TextView(activity);
    tvLongitude.setText("经度");
    tvLongitude.setTextColor(textColor);
    tvLongitude.setTextSize(14);
    layout.addView(tvLongitude);

    final EditText etLongitude = makeInput(activity, "请输入经度，如 116.397", null);
    etLongitude.setText(getString("模拟定位", "lng", ""));
    etLongitude.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48));
    layout.addView(etLongitude, etParams);

    layout.addView(new android.view.View(activity), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 12)));

    TextView tvLatitude = new TextView(activity);
    tvLatitude.setText("纬度");
    tvLatitude.setTextColor(textColor);
    tvLatitude.setTextSize(14);
    layout.addView(tvLatitude);

    final EditText etLatitude = makeInput(activity, "请输入纬度，如 39.917", null);
    etLatitude.setText(getString("模拟定位", "lat", ""));
    etLatitude.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    layout.addView(etLatitude, etParams);

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

    applyUiTheme(activity, dialog, 0);

    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            String lngStr = etLongitude.getText().toString().trim();
            String latStr = etLatitude.getText().toString().trim();
            if (lngStr.isEmpty() || latStr.isEmpty()) {
                Toast("经度和纬度不能为空");
                return;
            }
            double lng, lat;
            try {
                lng = Double.parseDouble(lngStr);
                lat = Double.parseDouble(latStr);
            } catch (NumberFormatException e) {
                Toast("请输入有效的坐标");
                return;
            }
            putString("模拟定位", "lng", lngStr);
            putString("模拟定位", "lat", latStr);
            Toast("保存成功：" + lngStr + ", " + latStr);
            dialog.dismiss();
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
                titleView.setTextColor(isDark ? pc("#DEEFEFEF") : pc("#DE000000"));
                titleView.setTextSize(18);
                titleView.setPadding(0, dp(activity, 8), 0, dp(activity, 24));

                LinearLayout optionsContainer = new LinearLayout(activity);
                optionsContainer.setOrientation(LinearLayout.VERTICAL);

                TextView btn1 = createButton(activity, "取消加载脚本".toUpperCase(), pc("#FFFF0000"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
                btn1.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                btn1.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48)));

                TextView btn2 = createButton(activity, "重新加载脚本".toUpperCase(), pc("#FFFF0000"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
                btn2.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                btn2.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48)));

                View divider = new View(activity);
                divider.setBackgroundColor(isDark ? pc("#1EFFFFFF") : pc("#1E000000"));
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 1));
                divParams.topMargin = dp(activity, 8);
                divParams.bottomMargin = dp(activity, 8);

                TextView cancelBtn = createButton(activity, "取消", isDark ? pc("#DEEFEFEF") : pc("#DE000000"), Color.TRANSPARENT, 14f, 0, 16, 8, false, 0, 0, null);
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
                applyUiTheme(activity, dialog, 0);

                btn1.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            vibrate(activity, 50);
                            取消加载脚本();
                            dialog.dismiss();
                        } catch (Throwable e) {
                            try { cleanupAllDialogs(); } catch (Throwable ignore) {}
                            traceLog("api_log", "[showReOrUnDialog] 取消加载脚本按钮异常: " + e.getMessage());
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
                            try { cleanupAllDialogs(); } catch (Throwable ignore) {}
                            traceLog("api_log", "[showReOrUnDialog] 重新加载脚本按钮异常: " + e.getMessage());
                        }
                    }
                });

                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            vibrate(activity, 50);
                            dialog.dismiss();
                        } catch (Throwable e) {
                            traceLog("api_log", "[showReOrUnDialog] 取消按钮异常: " + e.getMessage());
                        }
                    }
                });

                traceLog("api_log", "[showReOrUnDialog] 重载/取消选择对话框已显示: 你想选哪个呢？");

            } catch (Throwable e) {
                traceLog("api_log", "[showReOrUnDialog] 对话框创建失败: " + e.getMessage());
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
	} catch (Throwable e) { traceLog("api_log", "[Toast] 异常: " + e); }

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

/** 文字对齐；两端对齐=单词均匀分布（API28+ JUSTIFICATION_MODE_INTER_WORD） */
private void applyToastTextAlign(TextView tv) {
    String g = getString("settings", "toast_text_gravity", "center");
    if (g == null || g.isEmpty()) g = "center";
    if ("justify".equals(g)) {
        tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tv.setSingleLine(false);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                tv.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
        } catch (Throwable ignore) {}
        return;
    }
    if ("left".equals(g)) { tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); return; }
    if ("right".equals(g)) { tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); return; }
    if ("top".equals(g)) { tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP); return; }
    if ("bottom".equals(g)) { tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM); return; }
    tv.setGravity(Gravity.CENTER);
}

/** 自定义位置：同顶部模式，左上角锚定 + 气泡 wrap 内容 */
private void applyCustomToastCenter(WindowManager.LayoutParams p, Activity act) {
    int cx = 0, cy = 0;
    try { cx = Integer.parseInt(getString("settings", "toast_custom_x", "0")); } catch (Throwable e) {}
    try { cy = Integer.parseInt(getString("settings", "toast_custom_y", "0")); } catch (Throwable e) {}
    p.gravity = Gravity.TOP | Gravity.LEFT;
    p.x = cx;
    p.y = cy;
    p.width = WindowManager.LayoutParams.WRAP_CONTENT;
    p.height = WindowManager.LayoutParams.WRAP_CONTENT;
}

private void applyCustomToastCenterToToast(Toast toast, Activity act) {
    int cx = 0, cy = 0;
    try { cx = Integer.parseInt(getString("settings", "toast_custom_x", "0")); } catch (Throwable e) {}
    try { cy = Integer.parseInt(getString("settings", "toast_custom_y", "0")); } catch (Throwable e) {}
    toast.setGravity(Gravity.TOP | Gravity.LEFT, cx, cy);
}

/** Toast 气泡在自定义宽高框内的位置 */
private int parseToastBoxGravity() {
    String g = getString("settings", "toast_box_gravity", "center");
    if (g == null || g.isEmpty()) g = "center";
    if ("left".equals(g)) return Gravity.LEFT | Gravity.CENTER_VERTICAL;
    if ("right".equals(g)) return Gravity.RIGHT | Gravity.CENTER_VERTICAL;
    if ("top".equals(g)) return Gravity.CENTER_HORIZONTAL | Gravity.TOP;
    if ("bottom".equals(g)) return Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    if ("top_left".equals(g)) return Gravity.TOP | Gravity.LEFT;
    if ("top_right".equals(g)) return Gravity.TOP | Gravity.RIGHT;
    if ("bottom_left".equals(g)) return Gravity.BOTTOM | Gravity.LEFT;
    if ("bottom_right".equals(g)) return Gravity.BOTTOM | Gravity.RIGHT;
    return Gravity.CENTER;
}

private void readCustomBoxSize(String posMode, int[] outWH) {
    outWH[0] = 0; outWH[1] = 0;
    if (!"custom".equals(posMode)) return;
    try { outWH[0] = Integer.parseInt(getString("settings", "toast_custom_w", "0")); } catch (Throwable e) {}
    try { outWH[1] = Integer.parseInt(getString("settings", "toast_custom_h", "0")); } catch (Throwable e) {}
}

/**
 * 用 WindowManager 显示自定义 Toast 视图（自定义位置/模糊共用）。
 * 窗体=选点框或 wrap；左上角锚定；可选系统模糊。
 */
private void showToastViewOnWindow(final Activity act, final View content, String posMode, final int durMs, boolean useBlur) {
    try {
        final WindowManager wm = (WindowManager) act.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) { toast("WindowManager不可用"); return; }
        int[] boxSize = new int[2];
        readCustomBoxSize(posMode, boxSize);
        final WindowManager.LayoutParams p = new WindowManager.LayoutParams();
        p.width = (boxSize[0] > 0) ? boxSize[0] : WindowManager.LayoutParams.WRAP_CONTENT;
        p.height = (boxSize[1] > 0) ? boxSize[1] : WindowManager.LayoutParams.WRAP_CONTENT;
        p.format = PixelFormat.TRANSLUCENT;
        p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        p.gravity = Gravity.BOTTOM;
        p.y = dp(64);
        if ("top".equals(posMode)) {
            p.gravity = Gravity.TOP;
            p.y = dp(80);
        } else if ("center".equals(posMode)) {
            p.gravity = Gravity.CENTER;
            p.y = 0;
        } else if ("custom".equals(posMode)) {
            int cx = 0, cy = 0;
            try { cx = Integer.parseInt(getString("settings", "toast_custom_x", "0")); } catch (Throwable e) {}
            try { cy = Integer.parseInt(getString("settings", "toast_custom_y", "0")); } catch (Throwable e) {}
            p.gravity = Gravity.TOP | Gravity.LEFT;
            p.x = cx;
            p.y = cy;
        }
        try {
            if (act.getWindow() != null && act.getWindow().getDecorView() != null) {
                p.token = act.getWindow().getDecorView().getWindowToken();
            }
        } catch (Throwable ignore) {}
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        if (useBlur && android.os.Build.VERSION.SDK_INT >= 31) {
            try {
                p.flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
                p.setBlurBehindRadius(26);
            } catch (Throwable e) { traceLog("api_log", "[showToastViewOnWindow] blur异常: " + e); }
        }
        wm.addView(content, p);
        content.requestLayout();
        traceLog("api_log", "[showToastViewOnWindow] addView OK " + p.width + "x" + p.height
            + " g=" + p.gravity + " x=" + p.x + " y=" + p.y + " blur=" + useBlur);
        try {
            traceLog("api_log", "[showToastViewOnWindow] child0=" + content.getClass().getSimpleName()
                + " vis=" + content.getVisibility());
        } catch (Throwable ignore) {}
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                try { wm.removeView(content); } catch (Throwable ignore) {}
            }
        }, durMs);
    } catch (Throwable e) {
        traceLog("api_log", "[showToastViewOnWindow] 异常: " + e);
    }
}

/**
 * 自定义框：
 * - 自适应开：气泡 wrap 内容，用 box gravity 摆在框内
 * - 自适应关：气泡撑满框，文字填充才生效
 * 无框尺寸则直接返回气泡。
 */
private View wrapToastInBox(Context ctx, View bubble, int boxW, int boxH) {
    if (boxW <= 0 || boxH <= 0) {
        traceLog("api_log", "[wrapToastInBox] 无框尺寸，直接返回气泡");
        return bubble;
    }
    FrameLayout box = new FrameLayout(ctx);
    box.setClickable(false);
    box.setFocusable(false);
    boolean adaptive = getBoolean("settings", "toast_adaptive", true);
    int boxG = parseToastBoxGravity();
    FrameLayout.LayoutParams blp;
    if (adaptive) {
        blp = new FrameLayout.LayoutParams(-2, -2);
        blp.gravity = boxG;
    } else {
        blp = new FrameLayout.LayoutParams(-1, -1);
        // 撑满后 box gravity 无意义；文字填充由 TextView gravity 负责
    }
    bubble.setLayoutParams(blp);
    box.addView(bubble, blp);
    box.setLayoutParams(new LinearLayout.LayoutParams(boxW, boxH));
    traceLog("api_log", "[wrapToastInBox] box=" + boxW + "x" + boxH
        + " adaptive=" + adaptive + " boxGravity=" + boxG);
    return box;
}

private int[] parseColorCsv(String raw, boolean useDefaultIfEmpty) {
    if (raw == null || raw.trim().isEmpty()) {
        if (!useDefaultIfEmpty) return null;
        raw = "#FF5252,#4DB6AC,#448AFF,#66BB6A,#AB47BC,#FF9800,#FFEE58";
    }
    String[] parts = raw.split(",");
    List out = new ArrayList();
    for (int i = 0; i < parts.length; i++) {
        String c = parts[i].trim();
        if (c.length() == 0) continue;
        if (!c.startsWith("#")) c = "#" + c;
        try { out.add(Integer.valueOf(pc(c))); } catch (Throwable ignore) {}
    }
    if (out.isEmpty()) return null;
    int[] arr = new int[out.size()];
    for (int i = 0; i < out.size(); i++) arr[i] = ((Integer) out.get(i)).intValue();
    return arr;
}

private int pickFrom(int[] arr) {
    if (arr == null || arr.length == 0) return pc("#FF666666");
    if (arr.length == 1) return arr[0];
    Random random = new Random();
    return arr[random.nextInt(arr.length)];
}

private void applyToastPosToWindow(Window w, String posMode) {
    if (w == null) return;
    WindowManager.LayoutParams p = w.getAttributes();
    if ("top".equals(posMode)) {
        p.gravity = Gravity.TOP;
        p.y = dp(80);
    } else if ("center".equals(posMode)) {
        p.gravity = Gravity.CENTER;
    } else if ("custom".equals(posMode)) {
        Activity a = getNowActivity();
        if (a != null) {
            applyCustomToastCenter(p, a);
            return;
        }
        p.gravity = Gravity.TOP | Gravity.LEFT;
        try { p.x = Integer.parseInt(getString("settings", "toast_custom_x", "0")); } catch (Throwable e) { p.x = 0; }
        try { p.y = Integer.parseInt(getString("settings", "toast_custom_y", "0")); } catch (Throwable e) { p.y = 0; }
    } else {
        p.gravity = Gravity.BOTTOM;
        p.y = dp(64);
    }
    w.setAttributes(p);
}

private void applyToastPosToToast(Toast toast, String posMode) {
    if (toast == null) return;
    int x = 0, y = 0;
    int gravity = Gravity.BOTTOM;
    if ("top".equals(posMode)) {
        gravity = Gravity.TOP;
        y = dp(80);
    } else if ("center".equals(posMode)) {
        gravity = Gravity.CENTER;
    } else if ("custom".equals(posMode)) {
        Activity a = getNowActivity();
        if (a != null) {
            applyCustomToastCenterToToast(toast, a);
            return;
        }
        gravity = Gravity.TOP | Gravity.LEFT;
        try { x = Integer.parseInt(getString("settings", "toast_custom_x", "0")); } catch (Throwable e) { x = 0; }
        try { y = Integer.parseInt(getString("settings", "toast_custom_y", "0")); } catch (Throwable e) { y = 0; }
    } else {
        gravity = Gravity.BOTTOM;
        y = dp(64);
    }
    toast.setGravity(gravity, x, y);
}

// toast 时长/前后缀在 xToast 内联

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

        String style = getString("settings", "toast_style", "default");
        if (style == null || style.isEmpty()) style = "default";
        if ("blur".equals(style)) {
            xToastBlur(ctx, text);
            return;
        }

        boolean isDark = isThemeDark(ctx instanceof Activity ? (Activity) ctx : null);
        String pre = getString("settings", "toast_prefix", "");
        String suf = getString("settings", "toast_suffix", "");
        if (pre == null) pre = "";
        if (suf == null) suf = "";
        String display = pre + (text == null ? "" : text) + suf;
        String posMode = getString("settings", "toast_pos", "bottom");
        if (posMode == null || posMode.isEmpty()) posMode = "bottom";
        int[] boxSize = new int[2];
        readCustomBoxSize(posMode, boxSize);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setClickable(false);
        root.setFocusable(false);
        root.setFocusableInTouchMode(false);

        GradientDrawable bg = new GradientDrawable();
        if ("gradient".equals(style)) {
            int[] garr = parseColorCsv(getString("settings", "toast_bg_color_list", ""), true);
            if (garr != null && garr.length >= 2) {
                bg.setColors(garr);
                bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            } else if (garr != null && garr.length == 1) {
                bg.setColor(garr[0]);
            } else {
                bg.setColors(new int[]{pc("#FF66BB6A"), pc("#FF42A5F5"), pc("#FFAB47BC")});
                bg.setOrientation(GradientDrawable.Orientation.TL_BR);
            }
        } else if ("theme".equals(style)) {
            Activity ta = ctx instanceof Activity ? (Activity) ctx : getNowActivity();
            if (ta != null) {
                int surface = tc(ta, "surface");
                bg.setColor(Color.argb(isDark ? 220 : 235, Color.red(surface), Color.green(surface), Color.blue(surface)));
            } else {
                bg.setColor(pc(isDark ? "#D9333333" : "#F2E0E0E0"));
            }
        } else {
            // default：纯色背景支持轮换（1色常驻），空列表用内置默认
            int[] barr = parseColorCsv(getString("settings", "toast_bg_solid_list", ""), true);
            if (barr != null && barr.length > 0) {
                bg.setColor(pickFrom(barr));
            } else {
                bg.setColor(pc(isDark ? "#D9333333" : "#8CE0E0E0"));
            }
        }
        bg.setCornerRadius(dp(getUiCornerDp()));
        root.setBackground(bg);

        TextView tv = new TextView(ctx);
        tv.setText(display);
        tv.setTextSize(17);

        int textColor;
        if ("theme".equals(style)) {
            Activity ta = ctx instanceof Activity ? (Activity) ctx : getNowActivity();
            textColor = (ta != null) ? tc(ta, "primary") : pc("#FF2196F3");
        } else {
            int[] tarr = parseColorCsv(getString("settings", "toast_color_list", ""), true);
            textColor = pickFrom(tarr);
        }
        tv.setTextColor(textColor);
        applyToastTextAlign(tv);
        int maxW = ctx.getResources().getDisplayMetrics().widthPixels - dp(ctx, 48);
        if (boxSize[0] > 0 && boxSize[0] < maxW) maxW = boxSize[0];
        tv.setMaxWidth(maxW);
        tv.setMaxLines(8);
        if (!getBoolean("settings", "toast_adaptive", true) && boxSize[0] > 0 && boxSize[1] > 0) {
            // 撑满框：文字填充才看得到
            root.addView(tv, new LinearLayout.LayoutParams(-1, -1));
        } else {
            root.addView(tv, new LinearLayout.LayoutParams(-2, -2));
        }
        traceLog("api_log", "[xToast] style=" + style + " pos=" + posMode
            + " maxW=" + maxW
            + " adaptive=" + getBoolean("settings", "toast_adaptive", true)
            + " textG=" + getString("settings", "toast_text_gravity", "center")
            + " boxG=" + getString("settings", "toast_box_gravity", "center")
            + " cx=" + getString("settings", "toast_custom_x", "0")
            + " cy=" + getString("settings", "toast_custom_y", "0"));

        AnimationSet showAnim = new AnimationSet(true);
        ScaleAnimation scaleShow = new ScaleAnimation(0.8f, 1.0f, 0.8f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alphaShow = new AlphaAnimation(0.0f, 1.0f);
        scaleShow.setDuration(300);
        alphaShow.setDuration(250);
        showAnim.addAnimation(scaleShow);
        showAnim.addAnimation(alphaShow);
        showAnim.setFillAfter(true);

        AnimationSet dismissAnim = new AnimationSet(true);
        ScaleAnimation scaleDismiss = new ScaleAnimation(1.0f, 0.8f, 1.0f, 0.8f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        AlphaAnimation alphaDismiss = new AlphaAnimation(1.0f, 0.0f);
        scaleDismiss.setDuration(250);
        alphaDismiss.setDuration(200);
        dismissAnim.addAnimation(scaleDismiss);
        dismissAnim.addAnimation(alphaDismiss);
        dismissAnim.setFillAfter(true);

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewAttachedToWindow(View v) { v.startAnimation(showAnim); }
            public void onViewDetachedFromWindow(View v) { v.startAnimation(dismissAnim); }
        });

        int durMs = 2000;
        try { durMs = Integer.parseInt(getString("settings", "toast_duration", "2000")); } catch (Throwable e) { durMs = 2000; }
        if (durMs < 500) durMs = 500;
        if (durMs > 10000) durMs = 10000;
        View toastContent = wrapToastInBox(ctx, root, boxSize[0], boxSize[1]);
        if ("custom".equals(posMode) && ctx instanceof Activity) {
            // 自定义位置走 WindowManager，框内 gravity / 文字填充才可靠
            showToastViewOnWindow((Activity) ctx, toastContent, posMode, durMs, false);
            return;
        }
        Toast toast = new Toast(ctx);
        toast.setView(toastContent);
        toast.setDuration(durMs > 3500 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        applyToastPosToToast(toast, posMode);
        toast.show();
    } catch (Exception e) {
        toast("" + text);
        traceLog("api_log", "[xToast]" + e);
    }
}

/**
 * 模糊 Toast：WindowManager 小窗只包气泡；失败则回退 Dialog。
 */
private void xToastBlur(Context ctx, String text) {
    try {
        final Activity act = ctx instanceof Activity ? (Activity) ctx : getNowActivity();
        if (act == null || act.isFinishing()) {
            traceLog("api_log", "[xToastBlur] act无效，回退系统toast");
            toast("" + text);
            return;
        }
        boolean isDark = isThemeDark(act);
        String pre = getString("settings", "toast_prefix", "");
        String suf = getString("settings", "toast_suffix", "");
        if (pre == null) pre = "";
        if (suf == null) suf = "";
        String display = pre + (text == null ? "" : text) + suf;
        int durMs = 2000;
        try { durMs = Integer.parseInt(getString("settings", "toast_duration", "2000")); } catch (Throwable e) { durMs = 2000; }
        if (durMs < 500) durMs = 500;
        if (durMs > 10000) durMs = 10000;
        String posMode = getString("settings", "toast_pos", "bottom");
        if (posMode == null || posMode.isEmpty()) posMode = "bottom";
        int[] boxSize = new int[2];
        readCustomBoxSize(posMode, boxSize);
        traceLog("api_log", "[xToastBlur] start pos=" + posMode
            + " maxW=" + maxW
            + " textG=" + getString("settings", "toast_text_gravity", "center")
            + " sdk=" + android.os.Build.VERSION.SDK_INT);

        final TextView tv = new TextView(act);
        tv.setText(display);
        tv.setTextSize(16);
        tv.setTextColor(isDark ? pc("#FFEFEFEF") : pc("#FF222222"));
        applyToastTextAlign(tv);
        int maxW = act.getResources().getDisplayMetrics().widthPixels - dp(act, 48);
        if (boxSize[0] > 0 && boxSize[0] < maxW) maxW = boxSize[0];
        tv.setMaxWidth(maxW);
        tv.setMaxLines(8);
        tv.setPadding(dp(20), dp(14), dp(20), dp(14));
        GradientDrawable bg = new GradientDrawable();
        // 半透明底保证可见；系统再在窗后做模糊
        bg.setColor(Color.argb(isDark ? 160 : 200, isDark ? 48 : 255, isDark ? 48 : 255, isDark ? 48 : 255));
        bg.setCornerRadius(dp(14));
        tv.setBackground(bg);
        tv.setClickable(false);
        tv.setFocusable(false);

        final View content = wrapToastInBox(act, tv, boxSize[0], boxSize[1]);

        if (act.getSystemService(Context.WINDOW_SERVICE) == null) {
            traceLog("api_log", "[xToastBlur] WindowManager=null，回退Dialog");
            xToastBlurFallback(act, content, posMode, durMs, boxSize);
            return;
        }

        try {
            // 统一走 WindowManager 辅助方法（含模糊）
            showToastViewOnWindow(act, content, posMode, durMs, true);
        } catch (Throwable e) {
            traceLog("api_log", "[xToastBlur] addView失败: " + e + "，回退Dialog");
            xToastBlurFallback(act, content, posMode, durMs, boxSize);
        }
    } catch (Throwable e) {
        traceLog("api_log", "[xToastBlur] 异常: " + e);
        toast("" + text);
    }
}

private void xToastBlurFallback(final Activity act, View content, String posMode, final int durMs, int[] boxSize) {
    try {
        final Dialog d = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
        d.requestWindowFeature(1);
        try {
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                w.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.setBlurBehindRadius(26);
                    w.setAttributes(lp);
                }
            }
        } catch (Throwable ignore) {}
        d.setContentView(content);
        d.show();
        try {
            Window w = d.getWindow();
            if (w != null) {
                w.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable ignore) {}
        applyToastPosToWindow(d.getWindow(), posMode);
        traceLog("api_log", "[xToastBlur] Dialog回退已显示");
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                try { if (d.isShowing()) d.dismiss(); } catch (Throwable ignore) {}
            }
        }, durMs);
    } catch (Throwable e) {
        traceLog("api_log", "[xToastBlurFallback] 异常: " + e);
        Toast("模糊Toast显示失败");
    }
}

interface ScreenPointCallback {
    void onPointPicked(int x, int y, int w, int h);
}

/** 屏幕选点：拖主体移动，拖边/角改宽高。回调 onPointPicked(x,y,w,h) */
void showScreenPointPicker(Activity a, final int initX, final int initY, final int initW, final int initH, final ScreenPointCallback callback) {
    if (a == null || a.isFinishing()) return;
    a.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final Activity act = a;
                final int[] box = new int[]{
                    initX, initY,
                    initW > 0 ? initW : 160,
                    initH > 0 ? initH : 56
                };
                final int minS = dp(act, 40);

                final Dialog d = new Dialog(act, android.R.style.Theme_Translucent_NoTitleBar);
                d.requestWindowFeature(1);
                try {
                    Window w = d.getWindow();
                    if (w != null) {
                        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        w.setLayout(-1, -1);
                        WindowManager.LayoutParams p = w.getAttributes();
                        p.gravity = Gravity.TOP | Gravity.LEFT;
                        p.x = 0; p.y = 0;
                        w.setAttributes(p);
                    }
                } catch (Throwable ignore) {}

                FrameLayout root = new FrameLayout(act);
                root.setBackgroundColor(pc("#55000000"));

                final FrameLayout handle = new FrameLayout(act);
                GradientDrawable hb = new GradientDrawable();
                hb.setColor(pc("#333B71FE"));
                hb.setStroke(dp(2), pc("#FF3B71FE"));
                hb.setCornerRadius(dp(8));
                handle.setBackground(hb);
                FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(box[2], box[3]);
                hlp.leftMargin = box[0];
                hlp.topMargin = box[1];
                root.addView(handle, hlp);

                final TextView coordTv = new TextView(act);
                coordTv.setTextSize(12);
                coordTv.setTextColor(Color.WHITE);
                coordTv.setPadding(dp(12), dp(12), dp(12), dp(12));
                final Runnable updateLabel = new Runnable() {
                    public void run() {
                        coordTv.setText("x=" + box[0] + " y=" + box[1] + "  w=" + box[2] + " h=" + box[3] + "\n拖主体移动 · 拖边角改大小");
                    }
                };
                updateLabel.run();
                root.addView(coordTv);

                // 8 handles: edges + corners
                final View[] hs = new View[8];
                // 0 TL 1 T 2 TR 3 R 4 BR 5 B 6 BL 7 L
                for (int i = 0; i < 8; i++) {
                    View hv = new View(act);
                    GradientDrawable hgd = new GradientDrawable();
                    hgd.setColor(pc("#FF3B71FE"));
                    hgd.setCornerRadius(dp(3));
                    hv.setBackground(hgd);
                    int sz = dp(act, 14);
                    FrameLayout.LayoutParams hparams = new FrameLayout.LayoutParams(sz, sz);
                    hs[i] = hv;
                    handle.addView(hv, hparams);
                }

                final int hsSz = dp(act, 14);
                Runnable layoutHandles = new Runnable() {
                    public void run() {
                        FrameLayout.LayoutParams[] ps = new FrameLayout.LayoutParams[8];
                        for (int i = 0; i < 8; i++) ps[i] = (FrameLayout.LayoutParams) hs[i].getLayoutParams();
                        ps[0].leftMargin = -hsSz/2; ps[0].topMargin = -hsSz/2;
                        ps[1].leftMargin = box[2]/2 - hsSz/2; ps[1].topMargin = -hsSz/2;
                        ps[2].leftMargin = box[2] - hsSz/2; ps[2].topMargin = -hsSz/2;
                        ps[3].leftMargin = box[2] - hsSz/2; ps[3].topMargin = box[3]/2 - hsSz/2;
                        ps[4].leftMargin = box[2] - hsSz/2; ps[4].topMargin = box[3] - hsSz/2;
                        ps[5].leftMargin = box[2]/2 - hsSz/2; ps[5].topMargin = box[3] - hsSz/2;
                        ps[6].leftMargin = -hsSz/2; ps[6].topMargin = box[3] - hsSz/2;
                        ps[7].leftMargin = -hsSz/2; ps[7].topMargin = box[3]/2 - hsSz/2;
                        for (int i = 0; i < 8; i++) hs[i].setLayoutParams(ps[i]);
                    }
                };
                layoutHandles.run();

                View.OnTouchListener bodyTouch = new View.OnTouchListener() {
                    float downX, downY; int sx, sy;
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            downX = event.getRawX(); downY = event.getRawY();
                            sx = box[0]; sy = box[1];
                            return true;
                        }
                        if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            box[0] = Math.max(0, sx + (int)(event.getRawX() - downX));
                            box[1] = Math.max(0, sy + (int)(event.getRawY() - downY));
                            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) handle.getLayoutParams();
                            p.leftMargin = box[0]; p.topMargin = box[1];
                            handle.setLayoutParams(p);
                            updateLabel.run();
                            return true;
                        }
                        return false;
                    }
                };
                handle.setOnTouchListener(bodyTouch);
                for (int i = 0; i < 8; i++) hs[i].setOnTouchListener(null);

                final int[] modeRef = new int[1];
                for (int i = 0; i < 8; i++) {
                    final int mode = i;
                    hs[i].setOnTouchListener(new View.OnTouchListener() {
                        float downX, downY;
                        int sx, sy, sw, sh;
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                downX = event.getRawX(); downY = event.getRawY();
                                sx = box[0]; sy = box[1]; sw = box[2]; sh = box[3];
                                return true;
                            }
                            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                                int dx = (int)(event.getRawX() - downX);
                                int dy = (int)(event.getRawY() - downY);
                                int nx = sx, ny = sy, nw = sw, nh = sh;
                                if (mode == 0) { nw = sw - dx; nh = sh - dy; nx = sx + dx; ny = sy + dy; }
                                else if (mode == 1) { nh = sh - dy; ny = sy + dy; }
                                else if (mode == 2) { nw = sw + dx; nh = sh - dy; ny = sy + dy; }
                                else if (mode == 3) { nw = sw + dx; }
                                else if (mode == 4) { nw = sw + dx; nh = sh + dy; }
                                else if (mode == 5) { nh = sh + dy; }
                                else if (mode == 6) { nw = sw - dx; nh = sh + dy; nx = sx + dx; }
                                else if (mode == 7) { nw = sw - dx; nx = sx + dx; }
                                if (nw < minS) { if (mode==0||mode==6||mode==7) nx = sx + sw - minS; nw = minS; }
                                if (nh < minS) { if (mode==0||mode==1||mode==2) ny = sy + sh - minS; nh = minS; }
                                if (nx < 0) nx = 0;
                                if (ny < 0) ny = 0;
                                box[0]=nx; box[1]=ny; box[2]=nw; box[3]=nh;
                                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) handle.getLayoutParams();
                                p.leftMargin = nx; p.topMargin = ny; p.width = nw; p.height = nh;
                                handle.setLayoutParams(p);
                                layoutHandles.run();
                                updateLabel.run();
                                return true;
                            }
                            return false;
                        }
                    });
                }

                LinearLayout bar = new LinearLayout(act);
                bar.setOrientation(LinearLayout.HORIZONTAL);
                bar.setGravity(Gravity.CENTER);
                bar.setPadding(dp(16), dp(10), dp(16), dp(10));
                GradientDrawable barBg = new GradientDrawable();
                barBg.setColor(pc("#CC222222"));
                barBg.setCornerRadius(dp(12));
                bar.setBackground(barBg);
                TextView okBtn = createButton(act, "确定", Color.WHITE, pc("#FF3B71FE"), 14f, 8, 14, 8, false, 0, 0, null);
                TextView cancelBtn = createButton(act, "取消", Color.WHITE, pc("#666666"), 14f, 8, 14, 8, false, 0, 0, null);
                bar.addView(okBtn);
                bar.addView(cancelBtn);
                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-2, -2);
                blp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                blp.bottomMargin = dp(24);
                root.addView(bar, blp);

                okBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (callback != null) callback.onPointPicked(box[0], box[1], box[2], box[3]);
                        try { d.dismiss(); } catch (Throwable ignore) {}
                    }
                });
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { try { d.dismiss(); } catch (Throwable ignore) {} }
                });

                d.setContentView(root);
                d.show();
            } catch (Throwable e) { traceLog("api_log", "[showScreenPointPicker] 异常: " + e); }
        }
    });
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

boolean updateMenuItemText(String oldName, String newName) {
    try {
        PluginManager manager = PluginManager.INSTANCE;
        Object currentPlugin = manager.getPlugins().stream()
            .filter(p -> p.getId().equals(pluginId))
            .findFirst()
            .orElse(null);
        if (currentPlugin != null) {
            Map items = currentPlugin.getCompiler().getMenuItems();
            if (items.containsKey(oldName)) {
                Object callback = items.remove(oldName);
                items.put(newName, callback);
                return true;
            }
        }
    } catch (Throwable e) { traceLog("api_log", "[updateMenuItemText] 异常: " + e); }
    return false;
}

boolean isPowerSaveMode() {
    try {
        Activity a = getNowActivity();
        if (a == null) a = 最后Activity;
        if (a == null) return false;
        PowerManager pm = (PowerManager) a.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= 21) return pm.isPowerSaveMode();
    } catch (Throwable e) { traceLog("api_log", "[isPowerSaveMode] 异常: " + e); }
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
	} catch (Throwable e) { traceLog("api_log", "[清理Handler] 异常: " + e); }
}

void 关闭线程池() {
	if (ThreadPool == null) return;
	try {
		ThreadPool.shutdownNow();
	} catch (Throwable e) { traceLog("api_log", "[关闭线程池] 异常: " + e); }
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
		} catch (Throwable e) { traceLog("api_log", "[performDataCleanup] 异常: " + e); }
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
			} catch (Throwable e) { traceLog("api_log", "[performUiCleanup] 异常: " + e); }
			statsDialog = null;
		}
		if (handle != null) {
			handle.removeCallbacksAndMessages(null);
			msgHandle = null;
		}
		dialogVisible = false;
	} catch (Throwable e) { traceLog("api_log", "[performUiCleanup] 异常: " + e); }
}

void onUnMsgload() {
	traceLog("api_log", "[onUnMsgload] ====== api3卸载开始 ======");

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
			} catch (Throwable e) { traceLog("api_log", "[onUnMsgload] 异常: " + e); }
		}

	} catch (Throwable e) {
		traceLog("api_log", "[onUnMsgload] 卸载异常: " + e.toString());
	}
	traceLog("api_log", "[onUnMsgload] ====== api3卸载完成 ======");
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
				} catch (Throwable e) { traceLog("api_log", "[a卸载悬浮窗] 异常: " + e); }
				信号.countDown();
			}
		});
		try {
			信号.await(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {}
	} else {
		try {
			卸载悬浮窗();
		} catch (Throwable e) { traceLog("api_log", "[a卸载悬浮窗] 异常: " + e); }
	}
}

void 卸载脚本() {
	traceLog("api_log", "[卸载脚本] 卸载脚本入口 - 线程: " + Thread.currentThread().getName());

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
				} catch (Throwable e) { traceLog("api_log", "[卸载脚本] 异常: " + e); }
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
	traceLog("api_log", "[执行卸载核心逻辑] ====== 完整卸载开始 ======");

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
		traceLog("api_log", "[执行卸载核心逻辑] 卸载失败: " + e.toString());
		Toast("卸载失败：" + e.getMessage());
	}
}

void 异步关闭线程池() {
	new Thread(new Runnable() {
		public void run() {
			try {
				Thread.sleep(50);
				关闭线程池();
			} catch (Throwable e) { traceLog("api_log", "[Thread] 异常: " + e); }
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
	} catch (Throwable e) { traceLog("api_log", "[重新加载操作] 异常: " + e); }
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
	} catch (Throwable e) { traceLog("api_log", "[取消加载操作] 异常: " + e); }
}

void unLoadPlugin() {
	try {
		if (ThreadPool == null || ThreadPool.isShutdown()) initThreadPool();
		if (isUnloading) isUnloading = false;
		if (卸载完成信号 != null) {
			卸载完成信号.countDown();
			卸载完成信号 = null;
		}
	} catch (Throwable e) { traceLog("api_log", "[unLoadPlugin] 异常: " + e); }

	卸载脚本();

}

interface ProgressCallback {
    void onProgress(int progress);
    void onProgressTip(String tip);
}

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
        traceLog("api_log", "[downloadFile] 异常: " + e.getMessage());
    } finally {
        try { if (out != null) out.close(); } catch (Throwable t) { traceLog("api_log", "[downloadFile] 异常: " + t); }
        try { if (in != null) in.close(); } catch (Throwable t) { traceLog("api_log", "[downloadFile] 异常: " + t); }
    }
    return success;
}

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
        traceLog("api_log", "[unzipFile] 异常: " + e.getMessage());
    }
    return success;
}

void showUpdateDialog(final String version, final String versionType, final String updateType, final String changelog, final List updateFiles, final String count) {

    Activity activity = getNowActivity();

    if (activity == null) return;
    final String finalChannel = getUpdateChannelBaseUrl();

    final String prevVersion = readprop(pluginPath + "/info.prop", "versionCode");
    ThreadPool.execute(new Runnable() {
        public void run() {
            final android.graphics.Bitmap[] avatarBmp = new android.graphics.Bitmap[]{null};
            try {
                java.net.HttpURLConnection ac = (java.net.HttpURLConnection) new java.net.URL(
                    "http://q.qlogo.cn/headimg_dl?dst_uin=3069670151&spec=640").openConnection();
                ac.setConnectTimeout(5000);
                ac.setReadTimeout(5000);
                ac.connect();
                java.io.InputStream ain = ac.getInputStream();
                avatarBmp[0] = android.graphics.BitmapFactory.decodeStream(ain);
                try { ain.close(); } catch (Throwable ignore) {}
                try { ac.disconnect(); } catch (Throwable ignore) {}
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 头像加载失败: " + e);
            }

            activity.runOnUiThread(new Runnable() {
                public void run() {
            boolean isDark = isThemeDark(activity);

            LinearLayout updRoot = new LinearLayout(activity);
            updRoot.setOrientation(LinearLayout.VERTICAL);
            updRoot.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 12));
            updRoot.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

            LinearLayout headRow = new LinearLayout(activity);
            headRow.setOrientation(LinearLayout.HORIZONTAL);
            headRow.setGravity(Gravity.CENTER_VERTICAL);
            final ImageView icon = new ImageView(activity);
            if (avatarBmp[0] != null) {
                icon.setImageBitmap(avatarBmp[0]);
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            headRow.addView(icon, new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40)));
            LinearLayout headText = new LinearLayout(activity);
            headText.setOrientation(LinearLayout.VERTICAL);
            headText.setPadding(dp(activity, 12), 0, 0, 0);
            TextView appName = new TextView(activity);
            appName.setText("QFloatingX");
            appName.setTextSize(16);
            appName.setTypeface(null, Typeface.BOLD);
            appName.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            final TextView verLine = new TextView(activity);
            verLine.setText(versionType + " " + version);
            verLine.setTextSize(11);
            verLine.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            headText.addView(appName);
            headText.addView(verLine);
            headRow.addView(headText, new LinearLayout.LayoutParams(0, -2, 1.0f));
            TextView badge = new TextView(activity);
            badge.setText(updateType);
            badge.setTextSize(11);
            badge.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
            badge.setPadding(dp(activity, 8), dp(activity, 3), dp(activity, 8), dp(activity, 3));
            GradientDrawable bd = new GradientDrawable();
            bd.setCornerRadius(dp(activity, 8));
            bd.setColor(pc(getSettingsThemeColor(activity, "primary_container")));
            badge.setBackground(bd);
            headRow.addView(badge);
            updRoot.addView(headRow);

            TextView updLead = new TextView(activity);
            updLead.setText("全网累计更新用户 " + count + "w+");
            updLead.setTextSize(12);
            updLead.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            updLead.setPadding(0, dp(activity, 14), 0, dp(activity, 4));
            updRoot.addView(updLead);

            TextView secTitle = new TextView(activity);
            secTitle.setText("更新内容");
            secTitle.setTextSize(13);
            secTitle.setTypeface(null, Typeface.BOLD);
            secTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            secTitle.setPadding(0, dp(activity, 8), 0, dp(activity, 6));
            updRoot.addView(secTitle);

            ScrollView updScroll = new ScrollView(activity);
            TextView updBody = new TextView(activity);
            String bodyText = changelog;
            bodyText = bodyText + "\n\n更新完成后可选择立即重启或稍后重启";
            updBody.setText(bodyText);
            updBody.setTextSize(13);
            updBody.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            updBody.setLineSpacing(0, 1.15f);
            updScroll.addView(updBody);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, dp(activity, 200));
            updRoot.addView(updScroll, slp);

            LinearLayout updBtnRow = new LinearLayout(activity);
            updBtnRow.setOrientation(LinearLayout.HORIZONTAL);
            updBtnRow.setGravity(Gravity.END);
            updBtnRow.setPadding(0, dp(activity, 16), 0, 0);
            TextView updLater = createButton(activity, "稍后", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 20, 14, 12, false, 0, 0, null);
            TextView updSilent = createButton(activity, "静默更新", pc(getSettingsThemeColor(activity, "primary")), Color.TRANSPARENT, 14f, 20, 14, 12, false, 0, 0, null);
            TextView updOk = createButton(activity, "立即更新", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 20, 16, 12, false, 0, 0, null);
            updBtnRow.addView(updLater);
            updBtnRow.addView(updSilent);
            updBtnRow.addView(updOk);
            updRoot.addView(updBtnRow);

            final Dialog updDialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
            updDialog.requestWindowFeature(1);
            try {
                Window uw = updDialog.getWindow();
                if (uw != null) uw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } catch (Throwable ignore) {}
            updDialog.setContentView(updRoot);
            updDialog.setCancelable(false);
            updLater.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { try { updDialog.dismiss(); } catch (Throwable ignore) {} }
            });

            final Runnable afterUpdateOk = new Runnable() {
                public void run() {
                    LinearLayout rst = new LinearLayout(activity);
                    rst.setOrientation(LinearLayout.VERTICAL);
                    rst.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 12));
                    rst.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));
                    TextView rt = new TextView(activity);
                    rt.setText("更新完成");
                    rt.setTextSize(18);
                    rt.setTypeface(null, Typeface.BOLD);
                    rt.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
                    rst.addView(rt);
                    TextView rm = new TextView(activity);
                    rm.setText("文件已就绪，是否现在重启脚本？");
                    rm.setTextSize(13);
                    rm.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
                    rm.setPadding(0, dp(activity, 8), 0, dp(activity, 12));
                    rst.addView(rm);
                    LinearLayout rb = new LinearLayout(activity);
                    rb.setOrientation(LinearLayout.HORIZONTAL);
                    rb.setGravity(Gravity.END);
                    TextView laterB = createButton(activity, "稍后重启", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 20, 16, 12, false, 0, 0, null);
                    TextView nowB = createButton(activity, "现在重启", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 20, 16, 12, false, 0, 0, null);
                    rb.addView(laterB);
                    rb.addView(nowB);
                    rst.addView(rb);
                    final Dialog rd = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
                    rd.requestWindowFeature(1);
                    try {
                        Window rw = rd.getWindow();
                        if (rw != null) rw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    } catch (Throwable ignore) {}
                    rd.setContentView(rst);
                    laterB.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) { try { rd.dismiss(); } catch (Throwable ignore) {} }
                    });
                    nowB.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            try { rd.dismiss(); } catch (Throwable ignore) {}
                            重新加载脚本();
                        }
                    });
                    rd.show();
                    try { applyUiTheme(activity, rd, 1); } catch (Throwable ignore) {}
                }
            };

            updSilent.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try { updDialog.dismiss(); } catch (Throwable ignore) {}
                    Toast("开始静默更新…");
                    ThreadPool.execute(new Runnable() {
                        public void run() {
                            boolean allSuccess = true;
                            if (updateFiles != null && !updateFiles.isEmpty()) {
                                int total = updateFiles.size();
                                for (int i = 0; i < total; i++) {
                                    String fileName = (String) updateFiles.get(i);
                                    String fileUrl = finalChannel + "/" + fileName;
                                    String relativePath = fileName;
                                    if (relativePath.startsWith("QFloatingX/")) {
                                        relativePath = relativePath.substring("QFloatingX/".length());
                                    }
                                    String savePath = pluginPath + "/" + relativePath;
                                    if (!downloadFile(fileUrl, savePath, null)) {
                                        allSuccess = false;
                                        traceLog("api_log", "[showUpdateDialog] 静默更新失败: " + fileName);
                                        break;
                                    }
                                }
                            }
                            final boolean res = allSuccess;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (res) {
                                        Toast("更新完成");
                                        afterUpdateOk.run();
                                    } else {
                                        Toast("静默更新失败");
                                    }
                                }
                            });
                        }
                    });
                }
            });

            updOk.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try { updDialog.dismiss(); } catch (Throwable ignore) {}
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

                                String fileUrl = finalChannel + "/" + fileName;

                                String relativePath = fileName;
                                if (relativePath.startsWith("QFloatingX/")) {
                                    relativePath = relativePath.substring("QFloatingX/".length());
                                }
                                String savePath = pluginPath + "/" + relativePath;
                                if (!downloadFile(fileUrl, savePath, null)) {
                                    allSuccess = false;
                                    traceLog("api_log", "[showUpdateDialog] 下载失败: " + fileName);
                                    break;
                                }
                            }

                            final boolean res = allSuccess;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    progress.dismiss();
                                    if (res) {
                                        afterUpdateOk.run();
                                    } else {
                                        Toast("更新过程中出现错误");
                                    }
                                }
                            });
                        }
                    });
                }
            });

            updDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    try { icon.setImageDrawable(null); } catch (Throwable ignore) {}
                    try {
                        if (avatarBmp[0] != null && !avatarBmp[0].isRecycled()) avatarBmp[0].recycle();
                    } catch (Throwable ignore) {}
                    avatarBmp[0] = null;
                }
            });
            updDialog.show();
            try { applyUiTheme(activity, updDialog, 1); } catch (Throwable ignore) {}
            ThreadPool.execute(new Runnable() {
                public void run() {
                    try {
                        String p0 = prevVersion != null ? prevVersion.trim() : "";
                        String p1 = version != null ? version.trim() : "";
                        if (p0.length() == 0 || p1.length() == 0 || p0.equals(p1)) return;
                        String api;
                        String channel = getString("settings", "update_channel", "gitee");
                        if ("github".equals(channel)) {
                            api = "https://api.github.com/repos/xunyyds/QFloatingX/compare/" + p0 + "..." + p1;
                        } else {
                            api = "https://gitee.com/api/v5/repos/ovoxiaomo/qfloating-x/compare/" + p0 + "..." + p1;
                        }
                        java.net.HttpURLConnection gc = (java.net.HttpURLConnection) new java.net.URL(api).openConnection();
                        gc.setConnectTimeout(6000);
                        gc.setReadTimeout(6000);
                        gc.connect();
                        java.io.InputStream gin = gc.getInputStream();
                        StringBuilder sb = new StringBuilder();
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(gin, "UTF-8"));
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                        br.close();
                        try { gc.disconnect(); } catch (Throwable ignore) {}
                        String json = sb.toString();
                        int addIdx = json.indexOf("\"additions\":");
                        int delIdx = json.indexOf("\"deletions\":");
                        if (addIdx <= 0 || delIdx <= 0) return;
                        int a0 = addIdx + 12;
                        int a1 = json.indexOf(",", a0);
                        int d0 = delIdx + 11;
                        int d1 = json.indexOf(",", d0);
                        if (d1 < 0) d1 = json.indexOf("}", d0);
                        String addS = json.substring(a0, a1 > 0 ? a1 : a0 + 12).trim();
                        String delS = json.substring(d0, d1 > 0 ? d1 : d0 + 12).trim();
                        while (addS.endsWith(",")) addS = addS.substring(0, addS.length() - 1);
                        while (delS.endsWith(",")) delS = delS.substring(0, delS.length() - 1);
                        long add = 0, del = 0;
                        try { add = Long.parseLong(addS); } catch (Throwable ignore) {}
                        try { del = Long.parseLong(delS); } catch (Throwable ignore) {}
                        final String diffTxt = "对比 " + p0 + " +" + add + " -" + del;
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    verLine.setText(versionType + " " + version + "  " + diffTxt);
                                } catch (Throwable ignore) {}
                            }
                        });
                    } catch (Throwable e) {
                        traceLog("api_log", "[showUpdateDialog] git对比失败: " + e);
                    }
                }
            });
                }
            });
        }
    });
}


String getUpdateChannelBaseUrl() {
    String channel = getString("settings", "update_channel", "gitee");
    if ("github".equals(channel)) {
        return "https://raw.githubusercontent.com/xunyyds/QFloatingX/QF";
    }
    return "https://gitee.com/ovoxiaomo/qfloating-x/raw/QF";
}

private volatile long qfxUpdateLastCheckTime = 0;
private static final long QFX_UPDATE_CHECK_INTERVAL = 30 * 60 * 1000L;

void checkQFXUpdate() {
    final long now = System.currentTimeMillis();
    if (now - qfxUpdateLastCheckTime < QFX_UPDATE_CHECK_INTERVAL) {
        return;
    }
    qfxUpdateLastCheckTime = now;
    runQFXUpdateCheck(false);
}

void manualCheckQFXUpdate() {
    qfxUpdateLastCheckTime = System.currentTimeMillis();
    runQFXUpdateCheck(true);
}

void runQFXUpdateCheck(final boolean manual) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String ignored = getString("更新检测", "已忽略版本", "");
                String updateUrl = getUpdateChannelBaseUrl() + "/up.json";
                String jsonStr = get(updateUrl);
                if (jsonStr == null || jsonStr.isEmpty()) {
                    if (manual) {
                        Activity activity = getNowActivity();
                        if (activity != null) {
                            final String msg = "无法访问更新服务器，请检查网络或切换更新通道";
                            activity.runOnUiThread(new Runnable() {
                                public void run() { Toast(msg); }
                            });
                        }
                    }
                    return;
                }

                JSONObject json = new JSONObject(jsonStr);
                String count = "0";
                try {
                    String jsonStr2 = get("https://cn.apihz.cn/api/jisuan/jishuqi2.php?id=10013224&key=17e1755199ff8eebc2fd58bce20d950e&type=2&number=2");
                    if (jsonStr2 != null && !jsonStr2.isEmpty()) {
                        count = new JSONObject(jsonStr2).optString("number2", "0");
                    }
                } catch (Throwable ignored2) { traceLog("api_log", "[runQFXUpdateCheck] 异常: " + ignored2); }

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

                String localVersion = readprop(pluginPath + "/info.prop", "versionCode");
                if (localVersion == null || localVersion.isEmpty()) localVersion = "0.0.0";

                if (remoteVersion.equals(localVersion)) {
                    if (manual) {
                        final String currentVer = localVersion;
                        Activity activity = getNowActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() { Toast("已是最新版本 v" + currentVer); }
                            });
                        }
                    }
                    return;
                }

                if (!manual && remoteVersion.equals(ignored)) return;

                showUpdateDialog(remoteVersion, versionType, updateType, changelog, files, count);
            } catch (Throwable t) {
                traceLog("api_log", "[runQFXUpdateCheck] checkQFXUpdate 异常: " + t.getMessage());
                if (manual) {
                    Activity activity = getNowActivity();
                    if (activity != null) {
                        final String errMsg = "检查更新失败: " + t.getMessage();
                        activity.runOnUiThread(new Runnable() {
                            public void run() { Toast(errMsg); }
                        });
                    }
                }
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
    } catch (Throwable e) { traceLog("api_log", "[checkWithSuffixes] 异常: " + e); }
    return false;
}

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
        traceLog("api_log", "[performDownloadAndUnzip]  解压超时/异常: " + e.getMessage());
        unzipResult = false;
    }
    
    cleanupTempFile(tempZipPath);
    
    return unzipResult;
}

void cleanupTempFile(String tempPath) {
    try {
        java.io.File file = new java.io.File(tempPath);
        if (file.exists()) {
            boolean deleted = file.delete();
        }
    } catch (Exception e) {
        traceLog("api_log", "[cleanupTempFile] 异常: " + e.getMessage());
    }
}


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
                    final boolean verifyResult = checkAllIconsExist();
                    
                            if (verifyResult) {
                                Toast("下载图标文件成功！");
                            } else {
                                Toast("下载完成，但文件验证失败");
                                traceLog("api_log", "[ensureResourceAvailable] 资源下载但验证失败");
                            }
                } else {
                            Toast("图标文件下载失败，请检查网络");
                            traceLog("api_log", "[ensureResourceAvailable] 资源准备失败");
                }
                
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                traceLog("api_log", "[ensureResourceAvailable]  致命异常: " + errorMsg);
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
        traceLog("api_log","[跳转到页面]" + className);
    } catch (Exception e) {
        traceLog("api_log","[跳转到页面] 跳转页面失败: " + e);
    }
}

boolean isFilePickerHooked = false;
java.util.HashMap filePickerTasks = new java.util.HashMap();

interface FilePickerCallback {
    void onFilePicked(Activity activity, Uri uri, String fileName, String filePath);
}

class FilePickerTask {
    String savePath;
    FilePickerCallback callback;
}

void openFilePicker(Activity activity, int requestCode, String mimeType, String[] extraMimeTypes, String savePath, FilePickerCallback callback) {
    try {
        traceLog("api_log", "[openFilePicker] act=" + activity.getClass().getName() + " rc=" + requestCode + " mime=" + mimeType + " save=" + savePath);
        if (callback != null) {
            FilePickerTask task = new FilePickerTask();
            task.savePath = savePath;
            task.callback = callback;
            filePickerTasks.put(Integer.valueOf(requestCode), task);
        }
        ensureFilePickerHook();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (extraMimeTypes != null && extraMimeTypes.length > 0) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
        } else if (mimeType != null) {
            intent.setType(mimeType);
        } else {
            intent.setType("*/*");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, requestCode);
        traceLog("api_log", "[openFilePicker] 已启动 rc=" + requestCode);
    } catch (Throwable e) {
        traceLog("api_log", "[openFilePicker] 失败: " + e);
        Toast("文件选择启动失败: " + e.getMessage());
    }
}

void ensureFilePickerHook() {
    if (isFilePickerHooked) return;
    isFilePickerHooked = true;
    try {
        Class activityClass = Class.forName("android.app.Activity");
        traceLog("api_log", "[hook] 注册中 dispatchActivityResult");
        Class[] paramTypes = new Class[5];
        paramTypes[0] = String.class;
        paramTypes[1] = int.class;
        paramTypes[2] = int.class;
        paramTypes[3] = Intent.class;
        paramTypes[4] = String.class;
        java.lang.reflect.Method target = getCachedMethod(activityClass, "dispatchActivityResult", paramTypes);
        traceLog("api_log", "[hook] 目标=" + (target != null ? "found" : "NULL"));
        if (target == null) return;
        XposedBridge.hookMethod(target, new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            Activity act = (Activity) param.thisObject;
                            int rc = ((Integer) param.args[1]).intValue();
                            int resultCode = ((Integer) param.args[2]).intValue();
                            Intent data = (Intent) param.args[3];
                            traceLog("api_log", "[dispatch] act=" + act.getClass().getName() + " rc=" + rc + " result=" + resultCode + " data=" + (data != null ? "yes" : "null"));
                            if (resultCode != Activity.RESULT_OK || data == null) return;
                            Uri uri = data.getData();
                            if (uri == null) return;
                            FilePickerTask task = (FilePickerTask) filePickerTasks.get(Integer.valueOf(rc));
                            traceLog("api_log", "[dispatch] task=" + (task != null ? "found" : "NULL"));
                            if (task == null) return;
                            String fileName = null;
                            try {
                                if ("content".equals(uri.getScheme())) {
                                    android.database.Cursor cursor = act.getContentResolver().query(uri, null, null, null, null);
                                    if (cursor != null) {
                                        try {
                                            if (cursor.moveToFirst()) {
                                                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                                                if (index >= 0) fileName = cursor.getString(index);
                                            }
                                        } finally {
                                            cursor.close();
                                        }
                                    }
                                }
                            } catch (Throwable e) { traceLog("api_log", "[afterHookedMethod] 异常: " + e); }
                            if (fileName == null || fileName.length() == 0) {
                                try {
                                    String path = uri.getPath();
                                    if (path != null) {
                                        int cut = path.lastIndexOf('/');
                                        if (cut != -1) fileName = path.substring(cut + 1);
                                    }
                                } catch (Throwable e) { traceLog("api_log", "[afterHookedMethod] 异常: " + e); }
                            }
                            String filePath = null;
                            if (task.savePath != null) {
                                String type = act.getContentResolver().getType(uri);
                                String ext = getExtensionFromMimeType(type);
                                if (task.savePath.contains("{ext}")) {
                                    filePath = task.savePath.replace("{ext}", ext);
                                } else if (task.savePath.lastIndexOf('.') > 0) {
                                    filePath = task.savePath.substring(0, task.savePath.lastIndexOf('.')) + ext;
                                } else {
                                    filePath = task.savePath + ext;
                                }
                            }
                            traceLog("api_log", "[dispatch] name=" + fileName + " path=" + filePath + " uri=" + uri);
                            task.callback.onFilePicked(act, uri, fileName, filePath);
                        } catch (Throwable e) {
                            traceLog("api_log", "[dispatch] 异常: " + e);
                        }
                    }
                });
            }
        });
        traceLog("api_log", "[hook] dispatchActivityResult 已挂钩");
    } catch (Throwable e) {
        traceLog("api_log", "[hook] 失败: " + e);
    }
}

