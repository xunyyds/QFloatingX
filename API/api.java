
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
import java.lang.Thread; 

interface SettingsFeatureBuilder {
    View build(Activity activity);
}

Object app = BaseApplicationImpl.getApplication().getRuntime();

public String get(String url) {
	StringBuffer buffer = new StringBuffer();
	InputStreamReader isr = null;
	HttpURLConnection httpConn = null;
	try {
		URL urlObj = new URL(url);
		URLConnection uc = urlObj.openConnection();
		uc.setConnectTimeout(10000);
		uc.setReadTimeout(10000);
		uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36");
		uc.setRequestProperty("Accept", "application/json, text/plain, */*");
		if (uc instanceof HttpURLConnection) httpConn = (HttpURLConnection) uc;
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
		if (httpConn != null) { try { httpConn.disconnect(); } catch (Throwable ignore) {} }
	}
	if (buffer.length() == 0) return "访问网页失败";
	buffer.delete(buffer.length() - 1, buffer.length());
	return buffer.toString();
}

public String getCounter(String url) {
	StringBuffer buffer = new StringBuffer();
	InputStreamReader isr = null;
	HttpURLConnection httpConn = null;
	try {
		URL urlObj = new URL(url);
		URLConnection uc = urlObj.openConnection();
		uc.setConnectTimeout(10000);
		uc.setReadTimeout(10000);
		uc.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36");
		uc.setRequestProperty("Accept", "application/json, text/plain, */*");
		uc.setRequestProperty("Authorization", "Bearer ut_zmgCqsl1jmyyTxNNOteiU0nywTCldcVok1dUUi8S");
		if (uc instanceof HttpURLConnection) httpConn = (HttpURLConnection) uc;
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
		if (httpConn != null) { try { httpConn.disconnect(); } catch (Throwable ignore) {} }
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
String coreHttp(String tag, String url, String cookie, String method, String contentType, String userAgent, String data, int timeoutMs) {
    traceLog("api_log","[" + tag + "] " + url);
    HttpURLConnection uc = null;
    BufferedReader reader = null;
    try {
        uc = (HttpURLConnection) new URL(url).openConnection();
        uc.setDoInput(true);
        uc.setRequestMethod(method);
        if (contentType != null) uc.setRequestProperty("Content-Type", contentType);
        if (userAgent != null) uc.setRequestProperty("user-agent", userAgent);
        if (cookie != null && !cookie.isEmpty()) uc.setRequestProperty("Cookie", cookie);
        uc.setConnectTimeout(timeoutMs); uc.setReadTimeout(timeoutMs);
        if (data != null) {
            uc.setDoOutput(true);
            java.io.OutputStream os = uc.getOutputStream();
            os.write(data.getBytes("UTF-8"));
            os.flush();
            os.close();
        }
        reader = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        String result = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
        traceLog("api_log","[" + tag + "] 成功，长度: " + result.length());
        return result;
    } catch (Throwable e) {
        traceLog("api_log","[" + tag + "] 异常: " + e.getMessage());
        return "";
    } finally {
        try { if (reader != null) reader.close(); } catch (Throwable ignore) {}
        try { if (uc != null) uc.disconnect(); } catch (Throwable ignore) {}
    }
}

String httpGet(String url, String cookie) {
    return coreHttp("httpGet", url, cookie, "GET", null, "Mozilla/5.0 (Linux; Android 12; V2055A Build/SP1A.210812.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/89.0.4389.72 MQQBrowser/6.2 TBS/046209 Mobile Safari/537.36 V1_AND_SQ_8.9.5_3176_YYB_D A_8090500 QQ/8.9.5.8845 NetType/WIFI WebP/0.3.0 Pixel/1080 StatusBarHeight/85 SimpleUISwitch/0 QQTheme/1000 InMagicWin/0 StudyMode/0 CurrentMode/0 CurrentFontScale/0.87 GlobalDensityScale/0.90000004 AppId/537129734", null, 10000);
}

String httpPost(String url, String cookie, String data) {
    return coreHttp("httpPost", url, cookie, "POST", "application/x-www-form-urlencoded; charset=UTF-8", null, data, 20000);
}

String httpPostJson(String url, String cookie, String json) {
    return coreHttp("httpPostJson", url, cookie, "POST", "application/json; charset=UTF-8", null, json, 20000);
}
public String httppost1(String urlPath, String cookie, String data)
{
    StringBuffer buffer = new StringBuffer();
    InputStreamReader isr = null;
    try
    {
        URL url = new URL(urlPath);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        uc.setDoInput(true);
        uc.setDoOutput(true);
        uc.setConnectTimeout(15000);
        uc.setReadTimeout(15000);
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", "application/json");
        uc.setRequestProperty("Cookie", cookie);
        java.io.OutputStream os = uc.getOutputStream();
        os.write(data.getBytes("UTF-8"));
        os.flush();
        os.close();
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
                    traceLog("api_log", "[CheckSign] 调用打卡方法失败: " + invE);
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
	
	try {
		ITransFileController control = app.getApplication().getRuntime().getRuntimeService(ITransFileController.class);
		TransferRequest transferRequest = new TransferRequest();
		transferRequest.mIsUp = true;
		transferRequest.mLocalPath = path;
		transferRequest.mFileType = 22;
		boolean transferAsync = control.transferAsync(transferRequest);
		return transferAsync;
		
	} catch (e) {
		return false;
	}
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
	
	ITroopInfoService Info = app.getRuntimeService(ITroopInfoService.class);
	return Info.findTroopInfo("" + qun);
}












boolean validDisplayName(String value) {
    if (value == null || value.isEmpty()) return false;
    if ("null".equals(value) || value.contains("*")) return false;
    return true;
}


Object getProfileCard(String uin) {
    if (uin == null || uin.isEmpty()) return null;
    try {
        IProfileDataService profileData = app.getRuntimeService(IProfileDataService.class);
        return profileData.getProfileCard(uin, false);
    } catch (Throwable ignored) {
        return null;
    }
}


String getUserRemark(String uin) {
    Object card = getProfileCard(uin);
    if (card == null) return "";
    try {
        String remark = String.valueOf(card.strReMark);
        return validDisplayName(remark) ? remark : "";
    } catch (Throwable ignored) {
        return "";
    }
}


String getUserName(String uin) {
    if (uin == null || uin.isEmpty()) return "";
    Object card = getProfileCard(uin);
    if (card != null) {
        try {
            String nick = String.valueOf(card.strNick);
            if (validDisplayName(nick)) return nick;
        } catch (Throwable ignored) {}
    }
    return uin;
}


String getGroupName(String qun) {
    if (qun == null || qun.isEmpty()) return "";
    TroopInfo troopInfo = null;
    try {
        troopInfo = findTroopInfo(qun);
    } catch (Throwable ignored) {
        return qun;
    }
    if (troopInfo == null) return qun;
    try {
        String name = String.valueOf(troopInfo.troopname);
        if (validDisplayName(name)) return name;
    } catch (Throwable ignored) {}
    return qun;
}


String getGroupRemark(String qun) {
    if (qun == null || qun.isEmpty()) return "";
    TroopInfo troopInfo = null;
    try {
        troopInfo = findTroopInfo(qun);
    } catch (Throwable ignored) {
        return "";
    }
    if (troopInfo == null) return "";
    try {
        String remark = String.valueOf(troopInfo.troopRemark);
        return validDisplayName(remark) ? remark : "";
    } catch (Throwable ignored) {
        return "";
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

String formatRemainingTime(long seconds) {
	if (seconds <= 0) return "0秒";
	if (seconds < 60) return seconds + "秒";
	if (seconds < 3600) return (seconds / 60) + "分钟" + (seconds % 60) + "秒";
	if (seconds < 86400) return (seconds / 3600) + "小时" + ((seconds % 3600) / 60) + "分钟";
	return (seconds / 86400) + "天" + ((seconds % 86400) / 3600) + "小时";
}

String formatRemainingTimeMs(long ms) {
	if (ms <= 0) return "立即执行";
	return formatRemainingTime(ms / 1000);
}

int getAdaptiveCardBg(Activity a) {
	if (a == null) return pc("#33FFFFFF");
	boolean dark = isThemeDark(a);
	String bgType = getString("settings", "ui_bg_type", "color");
	if ("image".equals(bgType)) return Color.TRANSPARENT;
	if ("gradient".equals(bgType)) {
		int s = pc(getSettingsThemeColor(a, "surface"));
		return Color.argb(dark ? 150 : 170, Color.red(s), Color.green(s), Color.blue(s));
	}
	return mixTowardElevated(pc(getSettingsThemeColor(a, "background")), dark);
}

int getAdaptiveInputBg(Activity a) {
	if (a == null) return pc("#22FFFFFF");
	boolean dark = isThemeDark(a);
	String bgType = getString("settings", "ui_bg_type", "color");
	if ("image".equals(bgType)) {
		int s = pc(getSettingsThemeColor(a, "surface"));
		return Color.argb(dark ? 70 : 90, Color.red(s), Color.green(s), Color.blue(s));
	}
	return pc(getSettingsThemeColor(a, "surface"));
}

int getAdaptiveSheetBg(Activity a) {
	if (a == null) return pc("#FF1E1E1E");
	boolean dark = isThemeDark(a);
	String bgType = getString("settings", "ui_bg_type", "color");
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

int getAdaptiveMenuItemBg(Activity a) {
	if (a == null) return pc("#FF3A3A3A");
	boolean dark = isThemeDark(a);
	String bgType = getString("settings", "ui_bg_type", "color");
	int base;
	if ("image".equals(bgType)) {
		return dark ? Color.argb(100, 255, 255, 255) : Color.argb(82, 255, 255, 255);
	}
	if ("gradient".equals(bgType)) {
		base = pc(getSettingsThemeColor(a, "surface"));
	} else {
		base = pc(getSettingsThemeColor(a, "background"));
	}
	return mixTowardElevated(base, dark);
}

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
		return formatSize(getFolderSize(folder));
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
				return "";
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
			sb.append(str).append("\n"); 
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
		if (reader != null) {
			try {
				reader.close();
			} catch (Exception e) {
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

		if (parentDir != null && !parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				traceLog("api_log", "[写]  【写入失败】创建目录失败: " + parentDir.getAbsolutePath());
				return;
			}
		}

		if (!file.exists() && !file.createNewFile()) {
			traceLog("api_log", "[写]  【写入失败】创建文件失败: " + Path);
			return;
		}

		fos = new FileOutputStream(file);
		osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
		osw.write(WriteData);
		osw.flush();
		fos.flush();
		fos.getFD().sync(); 

		traceLog("api_log", "[写]  【写入成功】 " + Path + " (" + WriteData.length() + "字节)");

	} catch (IOException e) {
		traceLog("api_log", "[写]  【写入异常】 " + Path + " - " + e.getMessage());
	} catch (Exception e) {
		traceLog("api_log", "[写]  【写入异常】 " + Path + " - " + e.getMessage());
	} finally {
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

import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.net.URLDecoder;
import android.view.GestureDetector;
import android.view.MotionEvent;


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
		hex = hex.replace(" ", ""); 
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


void saveLocalData(String name, String content) {
	if (name == null || name.length() == 0) return;
	try {
		写(pluginPath + "/config/" + name, encryptBase64(content != null ? content : ""));
	} catch (Throwable e) {
		traceLog("api_log", "[saveLocalData] 异常: " + e);
	}
}

String loadLocalData(String name) {
	if (name == null || name.length() == 0) return "";
	try {
		String raw = 读(pluginPath + "/config/" + name);
		if (raw == null) return "";
		raw = raw.trim();
		if (raw.length() == 0) return "";
		if (raw.startsWith("读文件失败")) return "";
		String decoded = decryptBase64(raw);
		if (decoded == null || decoded.startsWith("Base64解密失败")) return "";
		return decoded;
	} catch (Throwable e) {
		traceLog("api_log", "[loadLocalData] 异常: " + e);
		return "";
	}
}


String reverseString(String text) {
	return new StringBuilder(text).reverse().toString();
}



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

int dp(Object contextObj, float dpValue) {
	return (int)(dpValue * getPluginUiDensity(contextObj) + 0.5f);
}
int dp(float dpValue) {
	return dp(context, dpValue);
}

void vibrate(Activity activity, int milliseconds) {
    if (!getBoolean("settings", "振动反馈", false)) {
        return;
    }
	if (activity == null) activity = safeActivity();
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


double 默认经度 = 116.397128;
double 默认纬度 = 39.907500;
void showApiProtectGuide(final Activity activity) {
    if (activity == null || activity.isFinishing()) return;
    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity,
                    isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                builder.setTitle("需要关闭 API 调用保护");
                builder.setMessage("未能找到 hook/反射所需的类或方法。\n请打开 LSPosed（LSP）管理器，关闭「API 调用保护」后重试。\n\n此提示可关闭，不影响已启动的功能。");
                builder.setPositiveButton("知道了", null);
                builder.setCancelable(true);
                AlertDialog dlg = builder.create();
                dlg.show();
                applyUiTheme(activity, dlg, 0);
            } catch (Throwable e) {
                traceLog("api_log", "[showApiProtectGuide] 异常: " + e);
            }
        }
    });
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
                            try { cleanupAllDialogs(); } catch (Throwable ignore) {}
                            dialog.dismiss();
                            取消加载脚本();
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
                            try { cleanupAllDialogs(); } catch (Throwable ignore) {}
                            dialog.dismiss();
                            重新加载脚本();
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

private View wrapToastInBox(Context ctx, View bubble, int boxW, int boxH) {
    if (boxW <= 0 || boxH <= 0) {
        
        return bubble;
    }
    FrameLayout box = new FrameLayout(ctx);
    box.setClickable(false);
    box.setFocusable(false);
    boolean adaptive = getBoolean("settings", "toast_adaptive", false);
    int boxG = parseToastBoxGravity();
    FrameLayout.LayoutParams blp;
    if (adaptive) {
        blp = new FrameLayout.LayoutParams(-2, -2);
        blp.gravity = boxG;
    } else {
        blp = new FrameLayout.LayoutParams(-1, -1);
        
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
        Activity a = safeActivity();
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
        Activity a = safeActivity();
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

private void xToast(String text) {
    try {
        Context ctx = context;
        if (ctx == null) {
            Activity act = safeActivity();
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
            
            int[] barr = parseColorCsv(getString("settings", "toast_bg_solid_list", ""), false);
            if (barr != null && barr.length > 0) {
                bg.setColor(pickFrom(barr));
            } else {
                bg.setColor(pc("#FFFFFFFF"));
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
            
            int[] tarr = parseColorCsv(getString("settings", "toast_color_list", ""), false);
            if (tarr != null && tarr.length > 0) textColor = pickFrom(tarr);
            else textColor = pc("#FF000000");
        }
        tv.setTextColor(textColor);
        applyToastTextAlign(tv);
        int maxW = ctx.getResources().getDisplayMetrics().widthPixels - dp(ctx, 48);
        if (boxSize[0] > 0 && boxSize[0] < maxW) maxW = boxSize[0];
        tv.setMaxWidth(maxW);
        tv.setMaxLines(8);
        boolean adaptive = getBoolean("settings", "toast_adaptive", false);
        if (!adaptive && boxSize[0] > 0 && boxSize[1] > 0) {
            root.addView(tv, new LinearLayout.LayoutParams(-1, -1));
        } else {
            root.addView(tv, new LinearLayout.LayoutParams(-2, -2));
        }
        

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

private void xToastBlur(Context ctx, String text) {
    try {
        final Activity act = ctx instanceof Activity ? (Activity) ctx : getNowActivity();
        if (act == null || act.isFinishing()) {
            
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
        int maxW = act.getResources().getDisplayMetrics().widthPixels - dp(act, 48);
        if (boxSize[0] > 0 && boxSize[0] < maxW) maxW = boxSize[0];
        
            
            
            

        final TextView tv = new TextView(act);
        tv.setText(display);
        tv.setTextSize(16);
        tv.setTextColor(isDark ? pc("#FFEFEFEF") : pc("#FF222222"));
        applyToastTextAlign(tv);
        tv.setMaxWidth(maxW);
        tv.setMaxLines(8);
        tv.setPadding(dp(20), dp(14), dp(20), dp(14));
        GradientDrawable bg = new GradientDrawable();
        
        bg.setColor(Color.argb(isDark ? 160 : 200, isDark ? 48 : 255, isDark ? 48 : 255, isDark ? 48 : 255));
        bg.setCornerRadius(dp(14));
        tv.setBackground(bg);
        tv.setClickable(false);
        tv.setFocusable(false);

        final View content = wrapToastInBox(act, tv, boxSize[0], boxSize[1]);

        if (act.getSystemService(Context.WINDOW_SERVICE) == null) {
            
            xToastBlurFallback(act, content, posMode, durMs, boxSize);
            return;
        }

        try {
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

                
                final View[] hs = new View[8];
                
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
                syncMenuOrder(items);
                return true;
            }
        }
    } catch (Throwable e) { traceLog("api_log", "[updateMenuItemText] 异常: " + e); }
    return false;
}

String[] MENU_ORDER = {"开/关悬浮窗", "开启悬浮窗", "关闭悬浮窗", "设置", "Java脚本"};

void syncMenuOrder(Map items) {
    try {
        if (items == null || items.isEmpty()) return;
        java.util.ArrayList orderedKeys = new java.util.ArrayList();
        for (int i = 0; i < MENU_ORDER.length; i++) {
            if (items.containsKey(MENU_ORDER[i])) orderedKeys.add(MENU_ORDER[i]);
        }
        Object[] rest = items.keySet().toArray();
        for (int i = 0; i < rest.length; i++) {
            if (!orderedKeys.contains(rest[i])) orderedKeys.add(rest[i]);
        }
        java.util.ArrayList vals = new java.util.ArrayList();
        for (int i = 0; i < orderedKeys.size(); i++) {
            Object k = orderedKeys.get(i);
            vals.add(items.get(k));
        }
        items.clear();
        for (int i = 0; i < orderedKeys.size(); i++) {
            items.put(orderedKeys.get(i), vals.get(i));
        }
    } catch (Throwable e) { traceLog("api_log", "[syncMenuOrder] 异常: " + e); }
}

void applyFloatWindowMenuText() {
    try {
        boolean on = getBoolean("settings", "开关", false);
        String want = on ? "关闭悬浮窗" : "开启悬浮窗";
        if (updateMenuItemText("开/关悬浮窗", want)) return;
        if (updateMenuItemText("开启悬浮窗", want)) return;
        updateMenuItemText("关闭悬浮窗", want);
    } catch (Throwable e) { traceLog("api_log", "[applyFloatWindowMenuText] 异常: " + e); }
}

boolean isPowerSaveMode() {
    try {
        Activity a = safeActivity();
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

void performUiCleanup() {
	try {
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

		Activity activity = safeActivity();
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
	Activity activity = safeActivity();

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

	try {
		执行卸载核心逻辑();
	} catch (Throwable e) { traceLog("api_log", "[卸载脚本] 异常: " + e); }
	异步关闭线程池();
}

void 执行卸载核心逻辑() {
	traceLog("api_log", "[执行卸载核心逻辑] ====== 完整卸载开始 ======");

	try {
		unhookAll();
		flushAllDedupCounters();
		onUnMsgload();
		a卸载悬浮窗();
		putBoolean("settings", "开关", false);
		
			
				unloadBackgroundCache();
				清理Handler(uiHandler);
				uiHandler = null;
				延迟启动完成 = false;
				应用前台状态 = false;
				Toast("脚本卸载完成，欢迎下次使用");
			
		
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
				Activity activity = safeActivity();
				if (activity == null || activity.isFinishing()) activity = 最后Activity;
				if (activity != null && !activity.isFinishing()) 重新加载操作(activity);
			}
		}, 300);
	} else {
		Activity activity = safeActivity();
		if (activity == null || activity.isFinishing()) activity = 最后Activity;
		if (activity != null && !activity.isFinishing()) 重新加载操作(activity);
	}
}

void 取消加载脚本() {
	if (uiHandler != null) {
		uiHandler.postDelayed(new Runnable() {
			public void run() {
				Activity activity = safeActivity();
				if (activity == null || activity.isFinishing()) activity = 最后Activity;
				if (activity != null && !activity.isFinishing()) 取消加载操作(activity);
			}
		}, 300);
	} else {
		Activity activity = safeActivity();
		if (activity == null || activity.isFinishing()) activity = 最后Activity;
		if (activity != null && !activity.isFinishing()) 取消加载操作(activity);
	}
}

void 重新加载操作(Activity activity) {
    try {
        try {
            Activity ref = SettingsState.settingsCurrentActivityRef;
            if (ref instanceof SettingsActivity && !ref.isFinishing()) {
                ref.finish();
            }
        } catch (Throwable closeIgnore) {}
        SettingsState.settingsCurrentActivityRef = null;

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
            } catch (Throwable e) { continue; }
        }
        if (target != null) pm.reloadPlugin(target);
    } catch (Throwable e) {
        traceLog("api_log", "[重新加载操作] 异常: " + e);
    }
}

void 取消加载操作(Activity activity) {
    try {
        try {
            Activity ref = SettingsState.settingsCurrentActivityRef;
            if (ref instanceof SettingsActivity && !ref.isFinishing()) {
                ref.finish();
            }
        } catch (Throwable closeIgnore) {}
        SettingsState.settingsCurrentActivityRef = null;

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
            } catch (Throwable e) { continue; }
        }
        if (target != null) pm.stopPlugin(target);
    } catch (Throwable e) {
        traceLog("api_log", "[取消加载操作] 异常: " + e);
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
	} catch (Throwable e) { traceLog("api_log", "[unLoadPlugin] 异常: " + e); }

	卸载脚本();

}

interface ProgressCallback {
    void onProgress(int progress);
    void onProgressTip(String tip);
}

interface UpdateProgressListener {
    void onPhase(String phase);
    void onFileStart(String displayName, int currentIndex, int totalFiles);
    void onFileProgress(int filePercent, long fileBytes, long fileTotal, long totalBytesAll, long elapsedMs);
    void onFileVerify(String displayName, boolean pass);
    void onAllDone(boolean success, String failReason);
}

String formatElapsedTime(long ms) {
    long totalSec = ms / 1000;
    long mm = totalSec / 60;
    long ss = totalSec % 60;
    String mmS = mm < 10 ? "0" + mm : String.valueOf(mm);
    String ssS = ss < 10 ? "0" + ss : String.valueOf(ss);
    return mmS + ":" + ssS;
}

String sha256HexOfFile(String path) {
    java.io.InputStream in = null;
    try {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        in = new java.io.FileInputStream(path);
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) != -1) md.update(buf, 0, read);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            int b = digest[i] & 0xFF;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    } catch (Throwable e) {
        traceLog("api_log", "[sha256HexOfFile] 异常: " + e);
        return null;
    } finally {
        if (in != null) { try { in.close(); } catch (Throwable ignore) {} }
    }
}

boolean verifyDownloadedFile(String tmpPath, long actualSize, String expectedSha256) {
    try {
        java.io.File f = new java.io.File(tmpPath);
        if (!f.exists()) return false;
        if (f.length() <= 0) return false;
        if (actualSize > 0 && f.length() != actualSize) return false;
        if (expectedSha256 != null && expectedSha256.trim().length() > 0) {
            String actualSha = sha256HexOfFile(tmpPath);
            if (actualSha == null) return false;
            if (!actualSha.equalsIgnoreCase(expectedSha256.trim())) return false;
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[verifyDownloadedFile] 异常: " + e);
        return false;
    }
}

boolean replaceWithTmp(String savePath, String tmpPath) {
    try {
        java.io.File target = new java.io.File(savePath);
        java.io.File tmp = new java.io.File(tmpPath);
        java.io.File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (target.exists()) {
            java.io.File bak = new java.io.File(savePath + ".bak");
            if (bak.exists()) bak.delete();
            boolean movedAside = target.renameTo(bak);
            if (!movedAside) {
                if (!target.delete()) return false;
            }
            if (!tmp.renameTo(target)) {
                if (movedAside) bak.renameTo(target);
                return false;
            }
            if (bak.exists()) bak.delete();
        } else {
            if (!tmp.renameTo(target)) return false;
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[replaceWithTmp] 异常: " + e);
        return false;
    }
}

void migrateUpdateChannelOnce() {
    try {
        if (getBoolean("settings", "update_channel_migrated", false)) return;
        if ("gitee".equals(getString("settings", "update_channel", "github"))) {
            putString("settings", "update_channel", "github");
            traceLog("api_log", "[migrateUpdateChannelOnce] 旧版下拉选 GitHub 实存 gitee，已归正为 github");
        }
        putBoolean("settings", "update_channel_migrated", true);
    } catch (Throwable e) {
        traceLog("api_log", "[migrateUpdateChannelOnce] 异常: " + e);
    }
}

String[] buildMirrorUrls(String relativePath) {
    migrateUpdateChannelOnce();
    String channel = getString("settings", "update_channel", "github");
    String githubRaw = "https://raw.githubusercontent.com/xunyyds/QFloatingX/QF/" + relativePath;
    if (!"github".equals(channel)) {
        return new String[]{"https://gitee.com/ovoxiaomo/qfloating-x/raw/QF/" + relativePath, githubRaw};
    }
    String[] mirrorHosts = {"gh.07150721.xyz", "ghpxy.hwinzniej.top", "ghf.无名氏.top", "gh.chjina.com", "js.jiangss.shop", "ghproxy.icu"};
    String[] urls = new String[2 + mirrorHosts.length];
    urls[0] = githubRaw;
    for (int k = 0; k < mirrorHosts.length; k++) {
        String h = mirrorHosts[k];
        try { h = java.net.IDN.toASCII(h); } catch (Throwable ignore) {}
        urls[k + 1] = "https://" + h + "/" + githubRaw;
    }
    urls[mirrorHosts.length + 1] = "https://gitee.com/ovoxiaomo/qfloating-x/raw/QF/" + relativePath;
    return urls;
}

boolean downloadToTmpDetailed(String[] urls, String tmpPath, final UpdateProgressListener listener,
    final long grandBytesBefore, final long startMs, final int idx, final int totalFiles,
    final String displayName, long[] resultBytes) {
    java.io.File tmpFile = new java.io.File(tmpPath);
    java.io.File saveDir = tmpFile.getParentFile();
    if (saveDir != null && !saveDir.exists()) saveDir.mkdirs();
    for (int u = 0; u < urls.length; u++) {
        String url = urls[u];
        if (u > 0 && listener != null) {
            listener.onPhase("mirror");
            traceLog("api_log", "[downloadToTmpDetailed] 尝试镜像#" + u + ": " + url);
        }
        java.io.FileOutputStream out = null;
        java.io.InputStream in = null;
        boolean ok = false;
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept-Encoding", "identity");
            int remoteSize = conn.getContentLength();
            in = conn.getInputStream();
            out = new java.io.FileOutputStream(tmpFile);
            byte[] buf = new byte[8192];
            long total = 0;
            int read;
            int tick = 0;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                total += read;
                tick++;
                if (listener != null && tick % 4 == 0) {
                    int percent = 0;
                    if (remoteSize > 0) percent = (int) ((total * 100) / remoteSize);
                    listener.onFileProgress(percent, total, remoteSize > 0 ? remoteSize : 0, grandBytesBefore + total, System.currentTimeMillis() - startMs);
                }
            }
            out.flush();
            if (resultBytes != null) { resultBytes[0] = total; resultBytes[1] = remoteSize; }
            if (remoteSize > 0) ok = (total == remoteSize);
            else ok = (total > 0);
        } catch (Throwable e) {
            traceLog("api_log", "[downloadToTmpDetailed] 下载失败: " + e.getMessage() + " url=" + url);
        } finally {
            try { if (out != null) out.close(); } catch (Throwable t) {}
            try { if (in != null) in.close(); } catch (Throwable t) {}
        }
        if (ok) return true;
        try { if (tmpFile.exists()) tmpFile.delete(); } catch (Throwable t) {}
    }
    return false;
}

boolean executeQfxUpdate(List updateFiles, JSONObject checksums, UpdateProgressListener listener) {
    if (updateFiles == null || updateFiles.isEmpty()) {
        if (listener != null) listener.onAllDone(false, "没有需要更新的文件");
        return false;
    }
    int total = updateFiles.size();
    long grandTotalBytes = 0;
    long startMs = System.currentTimeMillis();
    if (listener != null) listener.onPhase("download");
    for (int i = 0; i < total; i++) {
        String fileName = (String) updateFiles.get(i);
        String relativePath = fileName;
        if (relativePath.startsWith("QFloatingX/")) relativePath = relativePath.substring("QFloatingX/".length());
        String[] urlArr = buildMirrorUrls(fileName);
        String savePath = pluginPath + "/" + relativePath;
        String tmpPath = savePath + ".new";
        if (listener != null) listener.onFileStart(relativePath, i + 1, total);
        long[] bytesInfo = new long[]{0, 0};
        boolean dlOk = downloadToTmpDetailed(urlArr, tmpPath, listener, grandTotalBytes, startMs, i + 1, total, relativePath, bytesInfo);
        if (!dlOk) {
            cleanupTempFile(tmpPath);
            if (listener != null) listener.onAllDone(false, "下载失败: " + relativePath);
            return false;
        }
        grandTotalBytes += bytesInfo[0];
        if (listener != null) listener.onPhase("verify");
        String expectedSha = null;
        if (checksums != null) expectedSha = checksums.optString(relativePath, null);
        boolean verifyOk = verifyDownloadedFile(tmpPath, bytesInfo[0], expectedSha);
        if (listener != null) listener.onFileVerify(relativePath, verifyOk);
        if (!verifyOk) {
            cleanupTempFile(tmpPath);
            if (listener != null) listener.onAllDone(false, "完整性校验失败，已中止: " + relativePath);
            return false;
        }
        if (!replaceWithTmp(savePath, tmpPath)) {
            cleanupTempFile(tmpPath);
            if (listener != null) listener.onAllDone(false, "文件替换失败: " + relativePath);
            return false;
        }
        if (listener != null) listener.onPhase("download");
    }
    if (listener != null) listener.onAllDone(true, null);
    return true;
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
    java.io.File zipFile = new java.io.File(zipPath);
    java.io.File destDirFile = new java.io.File(destDir);
    java.util.zip.ZipFile zip = null;
    try {
        if (!zipFile.exists() || !zipFile.isFile()) return false;
        if (!destDirFile.exists() && !destDirFile.mkdirs()) return false;

        String destCanonical = destDirFile.getCanonicalPath() + java.io.File.separator;
        zip = new java.util.zip.ZipFile(zipFile);
        int totalEntry = zip.size();
        int currentEntry = 0;
        String zipRootDir = "";

        java.util.Enumeration firstPass = zip.entries();
        while (firstPass.hasMoreElements()) {
            java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) firstPass.nextElement();
            if (!entry.isDirectory()) {
                String name = entry.getName();
                int idx = name.indexOf('/');
                if (idx > 0) {
                    zipRootDir = name.substring(0, idx + 1);
                    break;
                }
            }
        }

        if (callback != null) {
            callback.onProgressTip("开始解压文件");
            callback.onProgress(0);
        }

        java.util.Enumeration entries = zip.entries();
        byte[] buf = new byte[32768];
        while (entries.hasMoreElements()) {
            currentEntry++;
            java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) entries.nextElement();
            String name = entry.getName();

            if (!zipRootDir.isEmpty() && name.startsWith(zipRootDir)) {
                name = name.substring(zipRootDir.length());
                if (name.isEmpty()) continue;
            }

            java.io.File entryFile = new java.io.File(destDirFile, name);
            String canonicalPath = entryFile.getCanonicalPath();
            if (!canonicalPath.equals(destDirFile.getCanonicalPath()) && !canonicalPath.startsWith(destCanonical)) {
                traceLog("api_log", "[unzipFile] 拒绝越界条目: " + name);
                continue;
            }

            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                java.io.File parent = entryFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                java.io.InputStream zin = null;
                java.io.OutputStream fout = null;
                try {
                    zin = new java.io.BufferedInputStream(zip.getInputStream(entry), 32768);
                    fout = new java.io.BufferedOutputStream(new java.io.FileOutputStream(entryFile), 32768);
                    int read;
                    while ((read = zin.read(buf)) != -1) fout.write(buf, 0, read);
                    fout.flush();
                } finally {
                    try { if (fout != null) fout.close(); } catch (Throwable e) {}
                    try { if (zin != null) zin.close(); } catch (Throwable e) {}
                }
            }

            if (callback != null) {
                int progress = totalEntry > 0 ? (int) ((currentEntry * 100L) / totalEntry) : 100;
                callback.onProgress(progress);
                callback.onProgressTip("解压: " + name);
            }
        }

        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("解压完成");
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[unzipFile] 异常: " + e);
        return false;
    } finally {
        try { if (zip != null) zip.close(); } catch (Throwable e) { traceLog("api_log", "[unzipFile] 关闭压缩包异常: " + e); }
    }
}

int countZipFiles(java.io.File dir) {
    if (dir == null || !dir.exists()) return 0;
    if (dir.isFile()) return 1;
    java.io.File[] files = dir.listFiles();
    if (files == null) return 0;
    int total = 0;
    for (int i = 0; i < files.length; i++) {
        java.io.File f = files[i];
        if (f == null) continue;
        total += f.isDirectory() ? countZipFiles(f) : 1;
    }
    return total;
}

boolean zipDirectoryFiles(java.io.File root, java.io.File current, String entryPrefix, java.util.zip.ZipOutputStream zos, byte[] buf, ProgressCallback callback, int[] progressState, int totalFiles) throws Exception {
    java.io.File[] files = current.listFiles();
    if (files == null) return true;
    String rootPath = root.getCanonicalPath();
    String currentPath = current.getCanonicalPath();
    if (files.length == 0 && !currentPath.equals(rootPath)) {
        String relativeDir = currentPath.substring(rootPath.length()).replace(java.io.File.separatorChar, '/');
        while (relativeDir.startsWith("/")) relativeDir = relativeDir.substring(1);
        String emptyDirName = (entryPrefix == null || entryPrefix.trim().isEmpty() ? "" : entryPrefix + "/") + relativeDir + "/";
        zos.putNextEntry(new java.util.zip.ZipEntry(emptyDirName));
        zos.closeEntry();
        return true;
    }
    for (int i = 0; i < files.length; i++) {
        java.io.File file = files[i];
        if (file == null) continue;
        String filePath = file.getCanonicalPath();
        String relative = filePath.substring(rootPath.length()).replace(java.io.File.separatorChar, '/');
        while (relative.startsWith("/")) relative = relative.substring(1);
        String entryName = (entryPrefix == null || entryPrefix.trim().isEmpty()) ? relative : entryPrefix + "/" + relative;
        if (file.isDirectory()) {
            zipDirectoryFiles(root, file, entryPrefix, zos, buf, callback, progressState, totalFiles);
            continue;
        }
        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
        java.io.InputStream in = null;
        try {
            in = new java.io.BufferedInputStream(new java.io.FileInputStream(file), 32768);
            int read;
            while ((read = in.read(buf)) != -1) zos.write(buf, 0, read);
        } finally {
            try { if (in != null) in.close(); } catch (Throwable e) {}
            try { zos.closeEntry(); } catch (Throwable e) {}
        }
        progressState[0]++;
        if (callback != null) {
            callback.onProgress(totalFiles > 0 ? (int) ((progressState[0] * 100L) / totalFiles) : 100);
            callback.onProgressTip("压缩: " + entryName);
        }
    }
    return true;
}

boolean zipDirectory(String sourceDir, String zipPath, String entryPrefix, String extraEntryName, String extraEntryContent, ProgressCallback callback) {
    java.io.File root = new java.io.File(sourceDir);
    java.io.File zipFile = new java.io.File(zipPath);
    java.util.zip.ZipOutputStream zos = null;
    try {
        if (!root.exists() || !root.isDirectory()) return false;
        java.io.File parent = zipFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        java.io.File temp = new java.io.File(zipPath + ".new");
        if (temp.exists()) temp.delete();

        int totalFiles = countZipFiles(root);
        int[] progressState = new int[]{0};
        byte[] buf = new byte[32768];
        zos = new java.util.zip.ZipOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(temp), 32768));
        if (callback != null) {
            callback.onProgressTip("开始压缩文件");
            callback.onProgress(0);
        }

        if (extraEntryName != null && !extraEntryName.trim().isEmpty()) {
            zos.putNextEntry(new java.util.zip.ZipEntry(extraEntryName));
            if (extraEntryContent != null) zos.write(extraEntryContent.getBytes("UTF-8"));
            zos.closeEntry();
        }

        zipDirectoryFiles(root, root, entryPrefix, zos, buf, callback, progressState, totalFiles);
        zos.finish();
        zos.close();
        zos = null;

        if (zipFile.exists() && !zipFile.delete()) {
            temp.delete();
            return false;
        }
        if (!temp.renameTo(zipFile)) {
            temp.delete();
            return false;
        }

        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("压缩完成");
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[zipDirectory] 异常: " + e);
        return false;
    } finally {
        try { if (zos != null) zos.close(); } catch (Throwable e) { traceLog("api_log", "[zipDirectory] 关闭压缩包异常: " + e); }
        try {
            java.io.File temp = new java.io.File(zipPath + ".new");
            if (temp.exists()) temp.delete();
        } catch (Throwable e) {}
    }
}

public String markdownToHtml(String markdown) {
    if (markdown == null || markdown.isEmpty()) return "";
    String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    StringBuilder html = new StringBuilder(markdown.length() + 128);
    StringBuilder code = new StringBuilder();
    int listType = 0;
    boolean inCode = false;
    for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        String trimmed = line.trim();
        if (trimmed.startsWith("```")) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            if (inCode) {
                String escapedCode = code.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
                html.append("<pre><code>").append(escapedCode).append("</code></pre>");
                code.setLength(0);
                inCode = false;
            } else {
                inCode = true;
            }
            continue;
        }
        if (inCode) {
            if (code.length() > 0) code.append('\n');
            code.append(line);
            continue;
        }
        if (trimmed.isEmpty()) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            continue;
        }
        if (trimmed.matches("-{2,}|\\*{3,}|_{3,}")) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            html.append("<hr/>");
            continue;
        }
        String content = trimmed;
        int lineType = 0;
        Matcher heading = Pattern.compile("^(#{1,6})\\s+(.+)$").matcher(trimmed);
        Matcher bullet = Pattern.compile("^[-+*]\\s+(.+)$").matcher(trimmed);
        Matcher numbered = Pattern.compile("^\\d+[.)]\\s+(.+)$").matcher(trimmed);
        if (heading.matches()) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            lineType = Math.min(heading.group(1).length(), 3);
            content = heading.group(2);
        } else if (bullet.matches() || numbered.matches()) {
            int nextType = bullet.matches() ? 1 : 2;
            if (listType != nextType) {
                if (listType != 0) html.append(listType == 1 ? "</ul>" : "</ol>");
                html.append(nextType == 1 ? "<ul>" : "<ol>");
                listType = nextType;
            }
            lineType = 4;
            content = bullet.matches() ? bullet.group(1) : numbered.group(1);
        } else {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            if (trimmed.startsWith(">")) {
                lineType = 5;
                content = trimmed.substring(1).trim();
            }
        }
        String value = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        ArrayList codeBlocks = new ArrayList();
        Matcher codeMatcher = Pattern.compile("`([^`]+)`").matcher(value);
        StringBuffer codeBuffer = new StringBuffer();
        int codeIndex = 0;
        while (codeMatcher.find()) {
            String token = "@@MDINLINECODE" + codeIndex++ + "@@";
            codeBlocks.add("<code>" + codeMatcher.group(1) + "</code>");
            codeMatcher.appendReplacement(codeBuffer, Matcher.quoteReplacement(token));
        }
        codeMatcher.appendTail(codeBuffer);
        value = codeBuffer.toString();
        Matcher linkMatcher = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^\\s)]+|mailto:[^\\s)]+)\\)").matcher(value);
        StringBuffer linkBuffer = new StringBuffer();
        while (linkMatcher.find()) {
            String replacement = "<a href=\"" + linkMatcher.group(2) + "\">" + linkMatcher.group(1) + "</a>";
            linkMatcher.appendReplacement(linkBuffer, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(linkBuffer);
        value = linkBuffer.toString();
        Matcher boldA = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(value);
        StringBuffer formatBuffer = new StringBuffer();
        while (boldA.find()) boldA.appendReplacement(formatBuffer, Matcher.quoteReplacement("<strong>" + boldA.group(1) + "</strong>"));
        boldA.appendTail(formatBuffer);
        value = formatBuffer.toString();
        Matcher boldB = Pattern.compile("__(.+?)__").matcher(value);
        formatBuffer = new StringBuffer();
        while (boldB.find()) boldB.appendReplacement(formatBuffer, Matcher.quoteReplacement("<strong>" + boldB.group(1) + "</strong>"));
        boldB.appendTail(formatBuffer);
        value = formatBuffer.toString();
        Matcher italicA = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)").matcher(value);
        formatBuffer = new StringBuffer();
        while (italicA.find()) italicA.appendReplacement(formatBuffer, Matcher.quoteReplacement("<em>" + italicA.group(1) + "</em>"));
        italicA.appendTail(formatBuffer);
        value = formatBuffer.toString();
        Matcher italicB = Pattern.compile("(?<!_)_([^_]+)_(?!_)").matcher(value);
        formatBuffer = new StringBuffer();
        while (italicB.find()) italicB.appendReplacement(formatBuffer, Matcher.quoteReplacement("<em>" + italicB.group(1) + "</em>"));
        italicB.appendTail(formatBuffer);
        value = formatBuffer.toString();
        for (int j = 0; j < codeBlocks.size(); j++) value = value.replace("@@MDINLINECODE" + j + "@@", (String) codeBlocks.get(j));
        if (lineType >= 1 && lineType <= 3) html.append("<h").append(lineType).append(">").append(value).append("</h").append(lineType).append(">");
        else if (lineType == 4) html.append("<li>").append(value).append("</li>");
        else if (lineType == 5) html.append("<blockquote><p>").append(value).append("</p></blockquote>");
        else html.append("<p>").append(value).append("</p>");
    }
    if (inCode) {
        String escapedCode = code.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        html.append("<pre><code>").append(escapedCode).append("</code></pre>");
    }
    if (listType != 0) html.append(listType == 1 ? "</ul>" : "</ol>");
    return html.toString();
}

void showUpdateDialog(final String version, final String versionType, final String updateType, final String changelog, final List updateFiles, final JSONObject checksums) {

    final Activity activity = safeActivity();
    if (activity == null) return;

    final String prevVersion = readprop(pluginPath + "/info.prop", "versionCode");

    LinearLayout updRoot = new LinearLayout(activity);
    updRoot.setOrientation(LinearLayout.VERTICAL);
    updRoot.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 12));
    updRoot.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

    LinearLayout headRow = new LinearLayout(activity);
    headRow.setOrientation(LinearLayout.HORIZONTAL);
    headRow.setGravity(Gravity.CENTER_VERTICAL);
    final ImageView icon = new ImageView(activity);
    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
    headRow.addView(icon, new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40)));

    LinearLayout headText = new LinearLayout(activity);
    headText.setOrientation(LinearLayout.VERTICAL);
    headText.setPadding(dp(activity, 12), 0, 0, 0);

    TextView appName = new TextView(activity);
    appName.setText("QFloatingX");
    appName.setTextSize(16);
    appName.setTypeface(null, Typeface.BOLD);
    appName.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));

    TextView verLine = new TextView(activity);
    verLine.setText(versionType + " " + version);
    verLine.setTextSize(11);
    verLine.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));

    TextView versionCompare = new TextView(activity);
    String localDisplay = prevVersion == null || prevVersion.trim().isEmpty() ? "未知" : prevVersion.trim();
    versionCompare.setText("当前版本 " + localDisplay + "  →  新版本 " + version);
    versionCompare.setTextSize(11);
    versionCompare.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    versionCompare.setPadding(0, dp(activity, 3), 0, 0);

    headText.addView(appName);
    headText.addView(verLine);
    headText.addView(versionCompare);
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

    final TextView updateCount = new TextView(activity);
    updateCount.setText("全网累计更新用户 加载中…");
    updateCount.setTextSize(12);
    updateCount.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    updateCount.setPadding(0, dp(activity, 14), 0, dp(activity, 4));
    updRoot.addView(updateCount);

    TextView secTitle = new TextView(activity);
    secTitle.setText("更新内容");
    secTitle.setTextSize(13);
    secTitle.setTypeface(null, Typeface.BOLD);
    secTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    secTitle.setPadding(0, dp(activity, 8), 0, dp(activity, 6));
    updRoot.addView(secTitle);

    ScrollView updScroll = new ScrollView(activity);
    TextView updBody = new TextView(activity);
    String bodyText = changelog == null || changelog.trim().isEmpty() ? "暂无更新说明" : changelog.trim();
    try {
        updBody.setText(Html.fromHtml(markdownToHtml(bodyText)));
    } catch (Throwable e) {
        updBody.setText(bodyText);
        traceLog("api_log", "[showUpdateDialog] Markdown格式化失败，使用纯文本: " + e.getMessage());
    }
    updBody.setTextSize(13);
    updBody.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    updBody.setLineSpacing(0, 1.15f);
    updScroll.addView(updBody);
    updRoot.addView(updScroll, new LinearLayout.LayoutParams(-1, dp(activity, 150)));

    TextView updateHint = new TextView(activity);
    updateHint.setText("检测到新版本，确认后即可开始更新");
    updateHint.setTextSize(13);
    updateHint.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    updateHint.setPadding(0, dp(activity, 18), 0, dp(activity, 8));
    updRoot.addView(updateHint);

    LinearLayout updBtnRow = new LinearLayout(activity);
    updBtnRow.setOrientation(LinearLayout.HORIZONTAL);
    updBtnRow.setGravity(Gravity.END);
    updBtnRow.setPadding(0, dp(activity, 8), 0, 0);
    TextView updLater = createButton(activity, "此版本不再提示", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 20, 14, 12, false, 0, 0, null);
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
    } catch (Throwable e) {
        traceLog("api_log", "[showUpdateDialog] 设置窗口背景异常: " + e);
    }
    updDialog.setContentView(updRoot);
    updDialog.setCancelable(false);

    updLater.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            putString("更新检测", "已忽略版本", version);
            try { updDialog.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭更新弹窗异常: " + e); }
            Toast("已忽略 v" + version + "，可在设置中手动检查更新");
        }
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
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 重启窗口背景异常: " + e);
            }
            rd.setContentView(rst);
            laterB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { try { rd.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭重启弹窗异常: " + e); } }
            });
            nowB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try { rd.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭重启确认弹窗异常: " + e); }
                    重新加载脚本();
                }
            });
            rd.show();
            try { applyUiTheme(activity, rd, 1); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 应用重启弹窗主题异常: " + e); }
        }
    };

    updSilent.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try { updDialog.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭更新弹窗异常: " + e); }
            Toast("开始静默更新…");
            ThreadPool.execute(new Runnable() {
                public void run() {
                    UpdateProgressListener silentListener = new UpdateProgressListener() {
                        public void onPhase(String phase) {}
                        public void onFileStart(String displayName, int currentIndex, int totalFiles) {}
                        public void onFileProgress(int filePercent, long fileBytes, long fileTotal, long totalBytesAll, long elapsedMs) {}
                        public void onFileVerify(String displayName, boolean pass) {}
                        public void onAllDone(final boolean success, final String failReason) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (success) {
                                        Toast("更新完成");
                                        afterUpdateOk.run();
                                    } else {
                                        Toast("静默更新失败: " + failReason);
                                    }
                                }
                            });
                        }
                    };
                    executeQfxUpdate(updateFiles, checksums, silentListener);
                }
            });
        }
    });

    updOk.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try { updDialog.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭更新弹窗异常: " + e); }
            if (updateFiles == null || updateFiles.isEmpty()) {
                Toast("没有需要更新的文件");
                return;
            }
            showStyledUpdateProgress(activity, updateFiles, checksums, afterUpdateOk);
        }
    });

    final android.graphics.Bitmap[] avatarBmp = new android.graphics.Bitmap[]{null};
    updDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            try { icon.setImageDrawable(null); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 清理头像视图异常: " + e); }
            try {
                if (avatarBmp[0] != null && !avatarBmp[0].isRecycled()) avatarBmp[0].recycle();
            } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 回收头像异常: " + e); }
            avatarBmp[0] = null;
        }
    });

    updDialog.show();
    try { applyUiTheme(activity, updDialog, 1); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 应用更新弹窗主题异常: " + e); }

    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String countJson = get("https://cn.apihz.cn/api/jisuan/jishuqi2.php?id=10013224&key=17e1755199ff8eebc2fd58bce20d950e&type=2&number=2");
                final String count = countJson == null || countJson.trim().isEmpty() ? "0" : new JSONObject(countJson).optString("number2", "0");
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            if (updDialog.isShowing()) updateCount.setText("全网累计更新用户 " + count + "w+");
                        } catch (Throwable e) {
                            traceLog("api_log", "[showUpdateDialog] 更新用户数异常: " + e);
                        }
                    }
                });
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 用户数加载失败: " + e);
            }
        }
    });

    ThreadPool.execute(new Runnable() {
        public void run() {
            java.net.HttpURLConnection ac = null;
            java.io.InputStream ain = null;
            try {
                ac = (java.net.HttpURLConnection) new java.net.URL("http://q.qlogo.cn/headimg_dl?dst_uin=3069670151&spec=640").openConnection();
                ac.setConnectTimeout(2500);
                ac.setReadTimeout(5000);
                ac.setUseCaches(true);
                int code = ac.getResponseCode();
                if (code >= 200 && code < 300) {
                    ain = ac.getInputStream();
                    final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(ain);
                    if (bmp != null) {
                        avatarBmp[0] = bmp;
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    if (updDialog.isShowing()) icon.setImageBitmap(bmp);
                                    else if (!bmp.isRecycled()) bmp.recycle();
                                } catch (Throwable e) {
                                    try { if (!bmp.isRecycled()) bmp.recycle(); } catch (Throwable ignore) {}
                                    traceLog("api_log", "[showUpdateDialog] 设置头像异常: " + e);
                                }
                            }
                        });
                    }
                }
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 头像加载失败: " + e);
            } finally {
                try { if (ain != null) ain.close(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭头像流异常: " + e); }
                try { if (ac != null) ac.disconnect(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭头像连接异常: " + e); }
            }
        }
    });
}

void showStyledUpdateProgress(final Activity activity, final List updateFiles, final JSONObject checksums, final Runnable afterUpdateOk) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(activity, 22), dp(activity, 20), dp(activity, 22), dp(activity, 16));
            root.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

            final TextView phaseTitle = new TextView(activity);
            phaseTitle.setTextSize(17);
            phaseTitle.setTypeface(null, Typeface.BOLD);
            phaseTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            root.addView(phaseTitle);

            final TextView fileTv = new TextView(activity);
            fileTv.setTextSize(12);
            fileTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            fileTv.setPadding(0, dp(activity, 10), 0, dp(activity, 6));
            root.addView(fileTv);

            final ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(0);

            GradientDrawable trackD = new GradientDrawable();
            trackD.setShape(GradientDrawable.RECTANGLE);
            trackD.setCornerRadius(dp(activity, 7));
            trackD.setColor(pc(getSettingsThemeColor(activity, "ripple")));
            GradientDrawable progressD = new GradientDrawable();
            progressD.setShape(GradientDrawable.RECTANGLE);
            progressD.setCornerRadius(dp(activity, 7));
            progressD.setColor(pc(getSettingsThemeColor(activity, "primary")));
            ClipDrawable clipD = new ClipDrawable(progressD, Gravity.LEFT, ClipDrawable.HORIZONTAL);
            LayerDrawable layerD = new LayerDrawable(new Drawable[]{trackD, clipD});
            layerD.setId(0, android.R.id.background);
            layerD.setId(1, android.R.id.progress);
            bar.setProgressDrawable(layerD);
            root.addView(bar, new LinearLayout.LayoutParams(-1, dp(activity, 14)));

            final TextView percentTv = new TextView(activity);
            percentTv.setText("0%");
            percentTv.setTextSize(22);
            percentTv.setTypeface(null, Typeface.BOLD);
            percentTv.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
            percentTv.setGravity(Gravity.CENTER);
            percentTv.setPadding(0, dp(activity, 12), 0, dp(activity, 8));
            root.addView(percentTv);

            LinearLayout infoRow = new LinearLayout(activity);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            final TextView elapsedTv = new TextView(activity);
            elapsedTv.setText("已用时 00:00");
            elapsedTv.setTextSize(12);
            elapsedTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            infoRow.addView(elapsedTv);
            Space infoSpace = new Space(activity);
            infoRow.addView(infoSpace, new LinearLayout.LayoutParams(0, -2, 1.0f));
            final TextView downloadedTv = new TextView(activity);
            downloadedTv.setText("已下载 0 B");
            downloadedTv.setTextSize(12);
            downloadedTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            infoRow.addView(downloadedTv);
            root.addView(infoRow);

            final TextView remainTv = new TextView(activity);
            remainTv.setText("");
            remainTv.setTextSize(12);
            remainTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            remainTv.setGravity(Gravity.CENTER);
            remainTv.setPadding(0, dp(activity, 8), 0, 0);
            root.addView(remainTv);

            final Dialog progDialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
            progDialog.requestWindowFeature(1);
            try {
                Window pw = progDialog.getWindow();
                if (pw != null) pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } catch (Throwable ignore) {}
            progDialog.setContentView(root);
            progDialog.setCancelable(false);
            progDialog.show();
            try { applyUiTheme(activity, progDialog, 1); } catch (Throwable ignore) {}

            final String[] phaseBase = new String[]{"正在下载更新"};
            final Handler animHandler = new Handler(Looper.getMainLooper());
            final int[] dotIdx = new int[]{0};
            animHandler.post(new Runnable() {
                public void run() {
                    dotIdx[0] = (dotIdx[0] + 1) % 4;
                    String dots = "";
                    for (int d = 0; d < dotIdx[0]; d++) dots += ".";
                    phaseTitle.setText(phaseBase[0] + dots);
                    animHandler.postDelayed(this, 500);
                }
            });

            final long timerStartMs = System.currentTimeMillis();
            animHandler.post(new Runnable() {
                public void run() {
                    elapsedTv.setText("已用时 " + formatElapsedTime(System.currentTimeMillis() - timerStartMs));
                    animHandler.postDelayed(this, 1000);
                }
            });

            ThreadPool.execute(new Runnable() {
                public void run() {
                    final long[] speedRef = new long[]{0L, 0L};
                    UpdateProgressListener listener = new UpdateProgressListener() {
                        public void onPhase(final String phase) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if ("verify".equals(phase)) {
                                        phaseBase[0] = "正在验证完整性";
                                        bar.setProgress(100);
                                    } else if ("mirror".equals(phase)) {
                                        phaseBase[0] = "当前正在使用镜像站进行加速下载";
                                    } else {
                                        phaseBase[0] = "正在下载更新";
                                    }
                                }
                            });
                        }
                        public void onFileStart(final String displayName, final int currentIndex, final int totalFiles) {
                            speedRef[0] = 0;
                            speedRef[1] = 0;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    fileTv.setText(displayName);
                                    remainTv.setText("第 " + currentIndex + " / " + totalFiles + " 个文件，剩余 " + (totalFiles - currentIndex) + " 个");
                                    bar.setProgress(0);
                                    percentTv.setText("0%");
                                }
                            });
                        }
                        public void onFileProgress(final int filePercent, final long fileBytes, final long fileTotal, final long totalBytesAll, final long elapsedMs) {
                            long now = System.currentTimeMillis();
                            String speedStr = null;
                            if (speedRef[0] > 0 && now - speedRef[0] >= 400) {
                                long db = totalBytesAll - speedRef[1];
                                long dt = now - speedRef[0];
                                long speed = dt > 0 ? db * 1000 / dt : 0;
                                speedStr = formatSize(speed) + "/s";
                                speedRef[0] = now;
                                speedRef[1] = totalBytesAll;
                            } else if (speedRef[0] == 0) {
                                speedRef[0] = now;
                                speedRef[1] = totalBytesAll;
                            }
                            final String fspeed = speedStr;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    bar.setProgress(filePercent);
                                    percentTv.setText(filePercent + "%");
                                    if (fspeed != null) {
                                        downloadedTv.setText("已下载 " + formatSize(totalBytesAll) + "  ·  " + fspeed);
                                    } else {
                                        downloadedTv.setText("已下载 " + formatSize(totalBytesAll));
                                    }
                                }
                            });
                        }
                        public void onFileVerify(final String displayName, final boolean pass) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    fileTv.setText((pass ? "✓ " : "✗ ") + displayName + (pass ? " 校验通过" : " 校验失败"));
                                }
                            });
                        }
                        public void onAllDone(final boolean success, final String failReason) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    animHandler.removeCallbacksAndMessages(null);
                                    try { progDialog.dismiss(); } catch (Throwable ignore) {}
                                    if (success) afterUpdateOk.run();
                                    else Toast("更新失败，未替换任何文件: " + failReason);
                                }
                            });
                        }
                    };
                    executeQfxUpdate(updateFiles, checksums, listener);
                }
            });
        }
    });
}

private volatile long qfxUpdateLastCheckTime = 0;
private static final long QFX_UPDATE_CHECK_INTERVAL = 30 * 60 * 1000L;

int compareVersionCode(String left, String right) {
    if (left == null) left = "";
    if (right == null) right = "";
    left = left.trim();
    right = right.trim();
    if (left.startsWith("v") || left.startsWith("V")) left = left.substring(1);
    if (right.startsWith("v") || right.startsWith("V")) right = right.substring(1);
    String[] la = left.split("\\.");
    String[] ra = right.split("\\.");
    int max = Math.max(la.length, ra.length);
    for (int i = 0; i < max; i++) {
        long a = 0L;
        long b = 0L;
        try {
            if (i < la.length) {
                String s = la[i].replaceAll("[^0-9].*", "");
                if (!s.isEmpty()) a = Long.parseLong(s);
            }
        } catch (Throwable e) {}
        try {
            if (i < ra.length) {
                String s = ra[i].replaceAll("[^0-9].*", "");
                if (!s.isEmpty()) b = Long.parseLong(s);
            }
        } catch (Throwable e) {}
        if (a > b) return 1;
        if (a < b) return -1;
    }
    return 0;
}

String fetchFirstValidUpdateJson(final String[] urls) {
    if (urls == null || urls.length == 0) return null;
    java.util.concurrent.ExecutorService mirrorPool = null;
    java.util.ArrayList futures = new java.util.ArrayList();
    final java.util.concurrent.atomic.AtomicBoolean stopRequests = new java.util.concurrent.atomic.AtomicBoolean(false);
    try {
        int taskCount = Math.max(1, urls.length);
        mirrorPool = java.util.concurrent.Executors.newFixedThreadPool(taskCount);
        java.util.concurrent.CompletionService completion = new java.util.concurrent.ExecutorCompletionService(mirrorPool);

        for (int i = 0; i < urls.length; i++) {
            final int index = i;
            final String url = urls[i];
            if (url == null || url.trim().isEmpty()) continue;
            futures.add(completion.submit(new java.util.concurrent.Callable() {
                public Object call() {
                    java.net.HttpURLConnection conn = null;
                    java.io.InputStream in = null;
                    try {
                        if (stopRequests.get() || Thread.currentThread().isInterrupted()) return null;
                        conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(3500);
                        conn.setReadTimeout(5000);
                        conn.setUseCaches(false);
                        conn.setRequestProperty("Connection", "close");
                        conn.setRequestProperty("User-Agent", "QFloatingX-Updater");
                        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                        int code = conn.getResponseCode();
                        if (code < 200 || code >= 300) {
                            if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] up.json HTTP " + code + " #" + index + ": " + url);
                            return null;
                        }
                        in = conn.getInputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        char[] buf = new char[8192];
                        int read;
                        while ((read = reader.read(buf)) != -1) {
                            if (stopRequests.get() || Thread.currentThread().isInterrupted()) return null;
                            sb.append(buf, 0, read);
                        }
                        String body = sb.toString().trim();
                        if (body.isEmpty()) return null;
                        try {
                            new JSONObject(body);
                        } catch (Throwable e) {
                            if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] up.json JSON无效 #" + index + ": " + url);
                            return null;
                        }
                        if (stopRequests.compareAndSet(false, true)) return new String[]{body, url};
                        return null;
                    } catch (java.io.InterruptedIOException e) {
                        if (!stopRequests.get() && !Thread.currentThread().isInterrupted()) {
                            if (e instanceof java.net.SocketTimeoutException) {
                                traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 请求超时 #" + index + ": " + e);
                            } else {
                                traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 请求中断 #" + index + ": " + e);
                            }
                        }
                        return null;
                    } catch (Throwable e) {
                        if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 请求失败 #" + index + ": " + e);
                        return null;
                    } finally {
                        try { if (in != null) in.close(); } catch (Throwable e) {}
                        try { if (conn != null) conn.disconnect(); } catch (Throwable e) {}
                    }
                }
            }));
        }

        for (int i = 0; i < futures.size(); i++) {
            java.util.concurrent.Future future = null;
            try {
                future = completion.poll(10000, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopRequests.set(true);
                break;
            } catch (Throwable e) {
                if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] 等待镜像结果异常: " + e);
            }
            if (future == null) break;
            try {
                Object result = future.get();
                if (result != null) {
                    String[] hit = (String[]) result;
                    traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 命中: " + hit[1]);
                    return hit[0];
                }
            } catch (java.util.concurrent.CancellationException e) {
            } catch (Throwable e) {
                if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] 解析线程结果异常: " + e);
            }
        }
    } catch (Throwable e) {
        if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] 并行请求异常: " + e);
    } finally {
        stopRequests.set(true);
        try {
            for (int i = 0; i < futures.size(); i++) {
                try { ((java.util.concurrent.Future) futures.get(i)).cancel(false); } catch (Throwable e) {}
            }
        } catch (Throwable e) {}
        try { if (mirrorPool != null) mirrorPool.shutdown(); } catch (Throwable e) {}
    }
    return null;
}

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
                String[] urls = buildMirrorUrls("up.json");
                String jsonStr = fetchFirstValidUpdateJson(urls);
                if (jsonStr == null || jsonStr.isEmpty()) {
                    traceLog("api_log", "[runQFXUpdateCheck] 所有up.json镜像均不可用");
                    if (manual) {
                        final Activity activity = safeActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() { Toast("无法访问更新服务器，请检查网络或切换更新通道"); }
                            });
                        }
                    }
                    return;
                }

                final JSONObject json;
                try {
                    json = new JSONObject(jsonStr);
                } catch (Throwable parseErr) {
                    traceLog("api_log", "[runQFXUpdateCheck] up.json解析失败: " + parseErr);
                    if (manual) {
                        final Activity activity = safeActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() { Toast("更新信息解析失败，请稍后重试或切换通道"); }
                            });
                        }
                    }
                    return;
                }

                String remoteVersion = json.optString("version", "0.0.0").trim();
                String versionType = json.optString("versionType", "正式版");
                String updateType = json.optString("updateType", "全量");
                String changelog = json.optString("changelog", "");
                boolean isTestVersion = versionType != null && (
                    versionType.contains("测试") ||
                    versionType.toLowerCase().contains("beta") ||
                    versionType.toLowerCase().contains("pre-release") ||
                    versionType.toLowerCase().contains("preview")
                );
                if (!manual && isTestVersion && !getBoolean("settings", "receive_beta_update_notice", false)) return;

                java.util.List files = new java.util.ArrayList();
                org.json.JSONArray filesArray = json.optJSONArray("files");
                if (filesArray != null) {
                    for (int i = 0; i < filesArray.length(); i++) {
                        files.add(filesArray.getString(i));
                    }
                }
                final JSONObject checksums = json.optJSONObject("checksums");

                String localVersion = readprop(pluginPath + "/info.prop", "versionCode");
                if (localVersion == null || localVersion.trim().isEmpty()) localVersion = "0.0.0";
                localVersion = localVersion.trim();

                int versionCompare = compareVersionCode(remoteVersion, localVersion);
                if (versionCompare <= 0) {
                    if (manual) {
                        final String currentVer = localVersion;
                        final String remoteVer = remoteVersion;
                        final Activity activity = safeActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (compareVersionCode(remoteVer, currentVer) == 0) {
                                        Toast("已是最新版本 " + currentVer);
                                    } else {
                                        Toast("当前版本 " + currentVer + " 高于更新源 " + remoteVer);
                                    }
                                }
                            });
                        }
                    }
                    return;
                }

                if (!manual && remoteVersion.equals(ignored)) return;

                final String finalRemoteVersion = remoteVersion;
                final String finalVersionType = versionType;
                final String finalUpdateType = updateType;
                final String finalChangelog = changelog;
                final List finalFiles = files;
                final JSONObject finalChecksums = checksums;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        try {
                            showUpdateDialog(finalRemoteVersion, finalVersionType, finalUpdateType, finalChangelog, finalFiles, finalChecksums);
                        } catch (Throwable e) {
                            traceLog("api_log", "[runQFXUpdateCheck] 更新弹窗异常: " + e);
                        }
                    }
                });
            } catch (Throwable t) {
                traceLog("api_log", "[runQFXUpdateCheck] 检查更新异常: " + t);
                if (manual) {
                    final Activity activity = safeActivity();
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

boolean iconFileExists(String fullPath) {
    if (fullPath == null || fullPath.trim().isEmpty()) return false;
    int lastDot = fullPath.lastIndexOf('.');
    int lastSlash = fullPath.lastIndexOf('/');
    String base = (lastDot > lastSlash && lastDot > 0) ? fullPath.substring(0, lastDot) : fullPath;
    try {
        if (new java.io.File(base + ".png").exists()) return true;
        if (new java.io.File(base + ".gif").exists()) return true;
    } catch (Throwable e) { traceLog("api_log", "[iconFileExists] 异常: " + e); }
    return false;
}

boolean checkAllIconsExist() {
    if (!iconFileExists(iconPath)) return false;
    if (!iconFileExists(closeIconPath)) return false;
    if (!new java.io.File(pluginPath + "/API/QQ.png").exists()) return false;
    if (!new java.io.File(pluginPath + "/API/GitHub.png").exists()) return false;
    return true;
}

boolean performDownloadAndUnzip() {
    final String tempZipPath = pluginPath + "/API/icon.zip";
    final String destDir = pluginPath + "/API/";
    try {
        String[] urls = buildMirrorUrls("icon.zip");
        boolean downloadResult = false;
        for (int u = 0; u < urls.length && !downloadResult; u++) {
            downloadResult = downloadFile(urls[u], tempZipPath, new ProgressCallback() {
                public void onProgress(int progressVal) {}
                public void onProgressTip(String tip) {}
            });
            if (!downloadResult) traceLog("api_log", "[performDownloadAndUnzip] 下载失败，尝试下一个源: " + urls[u]);
        }
        if (!downloadResult) {
            cleanupTempFile(tempZipPath);
            return false;
        }
        boolean unzipResult = unzipFile(tempZipPath, destDir, new ProgressCallback() {
            public void onProgress(int progressVal) {}
            public void onProgressTip(String tip) {}
        });
        cleanupTempFile(tempZipPath);
        return unzipResult;
    } catch (Throwable e) {
        cleanupTempFile(tempZipPath);
        traceLog("api_log", "[performDownloadAndUnzip] 异常: " + e);
        return false;
    }
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
    Activity act = safeActivity();
    if (act != null) {
        final Activity fa = act;
        final int fChatType = functionType;
        final String fPeerUin = (groupId != null) ? groupId : "";
        final String fPeerName = (userName != null) ? userName : "";
        fa.runOnUiThread(new Runnable() {
            public void run() { launchSettingsActivity(fa, fChatType, fPeerUin, fPeerName); }
        });
    }
}

void 跳转到页面(String className) {
    Activity activity = safeActivity();
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
java.util.concurrent.ConcurrentHashMap filePickerTasks = new java.util.concurrent.ConcurrentHashMap();

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
    synchronized (this) {
    if (isFilePickerHooked) return;
    try {
        Class activityClass = Class.forName("android.app.Activity");
        traceLog("api_log", "[hook] 注册中 dispatchActivityResult");
        // Activity 上有多个同名重载（5 参 / 6 参含 ComponentCaller，Android 14+ 框架调 6 参），必须全部挂钩
        int hookedCount = hookAll("file_picker", activityClass, "dispatchActivityResult", new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    ThreadPool.execute(new Runnable() {
                        public void run() {
                            try {
                                if (param.args == null || param.args.length < 4) return;
                                if (!(param.args[1] instanceof Integer) || !(param.args[2] instanceof Integer)) return;
                                Activity act = (Activity) param.thisObject;
                                int rc = ((Integer) param.args[1]).intValue();
                                int resultCode = ((Integer) param.args[2]).intValue();
                                Intent data = (Intent) param.args[3];
                                traceLog("api_log", "[dispatch] act=" + act.getClass().getName() + " rc=" + rc + " result=" + resultCode + " data=" + (data != null ? "yes" : "null"));
                                if (resultCode != Activity.RESULT_OK || data == null) return;
                                Uri uri = data.getData();
                                if (uri == null) return;
                                // 取出即移除：两个重载同时命中时只有一次能拿到任务，避免重复处理
                                FilePickerTask task = (FilePickerTask) filePickerTasks.remove(Integer.valueOf(rc));
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
        traceLog("api_log", "[hook] dispatchActivityResult 挂钩数=" + hookedCount);
        if (hookedCount == 0) {
            showApiProtectGuide(safeActivity());
            return;
        }
        isFilePickerHooked = true;
    } catch (Throwable e) {
        traceLog("api_log", "[hook] 失败: " + e);
        isFilePickerHooked = false;
        showApiProtectGuide(safeActivity());
    }
    }
}

