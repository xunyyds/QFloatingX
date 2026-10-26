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
	try {
		File file = new File(Path);
		if (!file.exists()) {
			traceLog("api_log", "[删除] 文件不存在: " + Path);
			return;
		}
		if (!me.yxp.qfun.utils.io.FileUtils.INSTANCE.delete(file)) {
			traceLog("api_log", "[删除] 删除失败: " + Path);
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

PluginInfo 查找运行中的本脚本() {
	try {
		PluginManager pm = PluginManager.INSTANCE;
		if (pm == null) return null;
		List list = pm.getPlugins();
		if (list == null) return null;
		int n = list.size();
		for (int i = 0; i < n; i++) {
			try {
				Object obj = list.get(i);
				if (obj == null || !"me.yxp.qfun.plugin.bean.PluginInfo".equals(obj.getClass().getName())) continue;
				PluginInfo p = (PluginInfo) obj;
				if ("QFloatingX".equals(p.getId()) && p.isRunning()) return p;
			} catch (Throwable e) { continue; }
		}
	} catch (Throwable e) { traceLog("api_log", "[查找运行中的本脚本] 异常: " + e); }
	return null;
}

void 关闭设置页并执行(final Runnable action) {
	if (action == null) return;
	boolean needWait = false;
	try {
		Activity ref = SettingsState.settingsCurrentActivityRef;
		if (ref instanceof SettingsActivity && !ref.isDestroyed()) {
			needWait = true;
			if (!ref.isFinishing()) ref.finish();
		}
		if (SettingsState.settingsActivityAliveCount > 0) needWait = true;
	} catch (Throwable closeIgnore) {}
	if (!needWait) {
		try { action.run(); } catch (Throwable e) { traceLog("api_log", "[关闭设置页并执行] 异常: " + e); }
		return;
	}
	等待设置页销毁(0, action);
}

void 等待设置页销毁(final int elapsedMs, final Runnable action) {
	if (action == null) return;
	boolean destroyed = true;
	try { destroyed = SettingsState.settingsActivityAliveCount <= 0; } catch (Throwable ignore) {}
	Handler h = uiHandler;
	if (destroyed || elapsedMs >= 1500 || h == null) {
		try { action.run(); } catch (Throwable e) { traceLog("api_log", "[等待设置页销毁] 异常: " + e); }
		return;
	}
	try {
		h.postDelayed(new Runnable() {
			public void run() {
				try {
					等待设置页销毁(elapsedMs + 30, action);
				} catch (Throwable ignore) {
					try { action.run(); } catch (Throwable ignore2) {}
				}
			}
		}, 30);
	} catch (Throwable e) {
		traceLog("api_log", "[等待设置页销毁] 轮询异常，直接继续: " + e);
		try { action.run(); } catch (Throwable e2) { traceLog("api_log", "[等待设置页销毁] 异常: " + e2); }
	}
}

void 重新加载操作(Activity activity) {
	关闭设置页并执行(new Runnable() {
		public void run() {
			try {
				PluginInfo target = 查找运行中的本脚本();
				if (target == null) return;
				PluginManager pm = PluginManager.INSTANCE;
				if (pm == null) return;
				pm.reloadPlugin(target);
			} catch (Throwable e) {
				traceLog("api_log", "[重新加载操作] 异常: " + e);
			}
		}
	});
}

void 取消加载操作(Activity activity) {
	关闭设置页并执行(new Runnable() {
		public void run() {
			try {
				PluginInfo target = 查找运行中的本脚本();
				if (target == null) return;
				PluginManager pm = PluginManager.INSTANCE;
				if (pm == null) return;
				pm.stopPlugin(target);
			} catch (Throwable e) {
				traceLog("api_log", "[取消加载操作] 异常: " + e);
			}
		}
	});
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
