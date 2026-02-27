private Handler positionHandler = new Handler(Looper.getMainLooper());
private Location fakeLocation = new Location(LocationManager.GPS_PROVIDER);
private List positionHookList = new ArrayList();
private List activeListeners = new ArrayList();

public void 模拟定位开关() {
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    final Activity finalActivity = activity;
    
    if (finalActivity == null) {
        traceLog("api2_log.txt","模拟定位开关异常: Activity为空");
        return;
    }
    
    boolean 模拟定位开关 = !getBoolean("模拟定位开关", "模拟定位开关", false);
    putBoolean("模拟定位开关", "模拟定位开关", 模拟定位开关);
    vibrate(finalActivity, 48);
    
    if (模拟定位开关) {
        Toast("正在开启模拟定位...");
        traceLog("api2_log.txt","[开关] 开启模拟定位");
        开模拟定位();
    } else {
        Toast("正在关闭模拟定位...");
        traceLog("api2_log.txt","[开关] 关闭模拟定位");
        关模拟定位();
    }
}

private void initFakeLocation() {
    String locStr = getLocationData();
    Double[] loc = splitLocation(locStr);
    if (loc == null || loc.length < 2) {
        traceLog("api2_log.txt","  [initFakeLocation] 定位数据解析失败: " + locStr);
        throw new RuntimeException("定位数据格式错误");
    }
    
    double longitude = loc[0];
    double latitude = loc[1];
    fakeLocation.setLatitude(latitude);
    fakeLocation.setLongitude(longitude);
    fakeLocation.setAccuracy(100);
    fakeLocation.setTime(System.currentTimeMillis());
    fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    fakeLocation.setSpeed(0.0f);
    fakeLocation.setBearing(0.0f);
}

private void getAppContext() {
    if (appContext != null) return;
    
    try {
        appContext = ActivityThread.currentActivityThread().getApplication();
        if (appContext != null) {
            traceLog("api2_log.txt","  [getAppContext] Context获取成功: " + appContext.getPackageName());
        } else {
            throw new RuntimeException("Application Context为空");
        }
    } catch (Throwable e) {
        traceLog("api2_log.txt","  [getAppContext] 异常: " + e.getMessage());
        throw new RuntimeException(e);
    }
}

private void hookLocation() {
    if (appContext == null) {
        traceLog("api2_log.txt","  [hookLocation] 失败: appContext为空");
        throw new RuntimeException("appContext未初始化");
    }
    
    String currentPkg = appContext.getPackageName();
    if (!QQpackage.equals(currentPkg)) {
        traceLog("api2_log.txt","  [hookLocation] 终止: 非目标应用，包名=" + currentPkg);
        return;
    }

    try {
        Class locationManagerCls = LocationManager.class;
        
        // Hook 1: getLastKnownLocation
        Method getLastKnownLocMethod = locationManagerCls.getDeclaredMethod("getLastKnownLocation", String.class);
        getLastKnownLocMethod.setAccessible(true);
        Object unhook1 = XposedBridge.hookMethod(getLastKnownLocMethod, new XC_MethodHook() {
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                param.setResult(fakeLocation);
            }
        });
        positionHookList.add(unhook1);
        
        // Hook 2: getProvider
        Method getProviderMethod = locationManagerCls.getDeclaredMethod("getProvider", String.class);
        getProviderMethod.setAccessible(true);
        Object unhook2 = XposedBridge.hookMethod(getProviderMethod, new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                    LocationProvider mockProvider = new LocationProvider("gps", null) {
                        public boolean isEnabled() { return true; }
                        public boolean meetsCriteria(Criteria criteria) { return true; }
                        public String getName() { return LocationManager.GPS_PROVIDER; }
                        public int getAccuracy() { return ACCURACY_FINE; }
                        public int getPowerRequirement() { return POWER_LOW; }
                        public boolean hasMonetaryCost() { return false; }
                        public boolean supportsAltitude() { return true; }
                        public boolean supportsSpeed() { return true; }
                        public boolean supportsBearing() { return true; }
                    };
                    param.setResult(mockProvider);
                }
            }
        });
        positionHookList.add(unhook2);
        
        // Hook 3: getAllProviders
        Method getAllProvidersMethod = locationManagerCls.getDeclaredMethod("getAllProviders");
        getAllProvidersMethod.setAccessible(true);
        Object unhook3 = XposedBridge.hookMethod(getAllProvidersMethod, new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                List providerList = new ArrayList();
                providerList.add(LocationManager.GPS_PROVIDER);
                param.setResult(providerList);
            }
        });
        positionHookList.add(unhook3);
        
        // Hook 4: isProviderEnabled
        Method isProviderEnabledMethod = locationManagerCls.getDeclaredMethod("isProviderEnabled", String.class);
        isProviderEnabledMethod.setAccessible(true);
        Object unhook4 = XposedBridge.hookMethod(isProviderEnabledMethod, new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                    param.setResult(true);
                }
            }
        });
        positionHookList.add(unhook4);
        
        // Hook 5: requestLocationUpdates
        Method[] allMethods = locationManagerCls.getDeclaredMethods();
        for (int i = 0; i < allMethods.length; i++) {
            Method method = allMethods[i];
            if (!method.getName().equals("requestLocationUpdates")) continue;
            
            method.setAccessible(true);
            Object unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                    LocationListener listener = null;
                    for (int j = 0; j < param.args.length; j++) {
                        Object arg = param.args[j];
                        if (arg instanceof LocationListener) {
                            listener = (LocationListener) arg;
                            break;
                        }
                    }
                    
                    if (listener != null) {
                        synchronized (activeListeners) {
                            boolean exists = false;
                            for (int k = 0; k < activeListeners.size(); k++) {
                                if (activeListeners.get(k) == listener) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                activeListeners.add(listener);
                            }
                        }
                        
                        final LocationListener finalListener = listener;
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                try {
                                    finalListener.onLocationChanged(fakeLocation);
                                } catch (Exception e) {
                                    traceLog("api2_log.txt","    [Hook] 回调异常: " + e);
                                }
                            }
                        });
                    }
                    
                    param.setResult(null);
                }
            });
            positionHookList.add(unhook);
        }
        
        traceLog("api2_log.txt","  [hookLocation] Hook完成，总数=" + positionHookList.size());
        verifyHookStatus();
        
    } catch (Throwable e) {
        traceLog("api2_log.txt","  [hookLocation] 致命异常: " + e.getMessage());
        StackTraceElement[] stack = e.getStackTrace();
        for (int i = 0; i < Math.min(stack.length, 3); i++) {
            traceLog("api2_log.txt","    [堆栈] " + stack[i]);
        }
        throw new RuntimeException(e);
    }
}

private void verifyHookStatus() {
    try {
        LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        List providers = lm.getAllProviders();
        traceLog("api2_log.txt","  [验证] GPS状态=" + isEnabled + " Providers=" + providers);
    } catch (Exception e) {
        traceLog("api2_log.txt","  [验证] 状态检查失败: " + e);
    }
}

private void unhookAll() {
    if (positionHookList.isEmpty() && activeListeners.isEmpty()) {
        return;
    }
    
    traceLog("api2_log.txt","  [unhookAll] 清理Hook数=" + positionHookList.size() + ", 监听器数=" + activeListeners.size());
    
    for (int i = 0; i < positionHookList.size(); i++) {
        Object unhook = positionHookList.get(i);
        try {
            Method unhookMethod = unhook.getClass().getMethod("unhook");
            unhookMethod.invoke(unhook);
        } catch (Exception e) {
            traceLog("api2_log.txt","    [unhookAll] 取消失败: " + e.getMessage());
        }
    }
    
    synchronized (activeListeners) {
        activeListeners.clear();
    }
    positionHookList.clear();
    isRunning.set(false);
    appContext = null;
    
    traceLog("api2_log.txt","  [unhookAll] 清理完成，恢复正常定位");
}

void 关模拟定位() {
    traceLog("api2_log.txt","[关模拟定位] 执行开始");
    
    isRunning.set(false);
    
    getNowActivity().runOnUiThread(new Runnable() {
        public void run() {
            positionHandler.removeCallbacksAndMessages(null);
        }
    });
    
    unhookAll();
    putBoolean("模拟定位开关", "模拟定位开关", false);
    
    Toast("模拟定位已完全关闭");
}

void 开模拟定位() {
    traceLog("api2_log.txt","[开模拟定位] 执行开始，isRunning=" + isRunning.get());
    
    if (isRunning.get()) {
        traceLog("api2_log.txt","  [开模拟定位] 已在运行，直接返回");
        return;
    }
    
    isRunning.set(true);
    
    positionHandler.postDelayed(new Runnable() {
        public void run() {
            traceLog("api2_log.txt","[延迟任务] 执行开始，isRunning=" + isRunning.get());
            
            if (!isRunning.get()) {
                traceLog("api2_log.txt","  [延迟任务] 已停止，取消执行");
                return;
            }
            
            try {
                initFakeLocation();
                getAppContext();
                hookLocation();
                startLocationStreamThread();
                
                traceLog("api2_log.txt","  [延迟任务] 所有步骤完成，模拟定位运行中");
                Toast("模拟定位已开启，支持实时位置共享");
                
            } catch (Throwable t) {
                traceLog("api2_log.txt","  [延迟任务] 致命错误: " + t.getMessage());
                isRunning.set(false);
                Toast("模拟定位启动失败: " + t.getMessage());
            }
        }
    }, 2000);
    
    traceLog("api2_log.txt","[开模拟定位] 延迟任务已提交");
}

private void startLocationStreamThread() {
    ThreadPool.execute(new Runnable() {
        public void run() {
            int loopCount = 0;
            
            while (isRunning.get()) {
                try {
                    Thread.sleep(3000);
                    loopCount++;
                    
                    fakeLocation.setTime(System.currentTimeMillis());
                    fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    
                    synchronized (activeListeners) {
                        if (!activeListeners.isEmpty()) {
                            for (int i = 0; i < activeListeners.size(); i++) {
                                LocationListener listener = (LocationListener) activeListeners.get(i);
                                try {
                                    listener.onLocationChanged(fakeLocation);
                                    // 心跳日志：每15秒输出一次
                                    if (loopCount % 5 == 0) {
                                        traceLog("api2_log.txt","  [心跳] 位置流运行中，循环次数=" + loopCount + ", 监听器数=" + activeListeners.size());
                                    }
                                } catch (Exception e) {
                                    traceLog("api2_log.txt","  [心跳] 回调异常: " + e.getMessage());
                                }
                            }
                        }
                    }
                    
                } catch (InterruptedException e) {
                    traceLog("api2_log.txt","    [位置流] 线程被中断，退出");
                    break;
                } catch (Exception e) {
                    traceLog("api2_log.txt","    [位置流] 异常: " + e);
                    break;
                }
            }
            
            traceLog("api2_log.txt","    [位置流] 线程退出，isRunning=" + isRunning.get());
        }
    });
}