private Handler positionHandler = new Handler(Looper.getMainLooper());
private Location fakeLocation = new Location(LocationManager.GPS_PROVIDER);
private List activeListeners = new ArrayList();

public void 模拟定位开关() {
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
    final Activity finalActivity = activity;
    if (finalActivity == null) return;
    boolean state = !getBoolean("模拟定位开关", "模拟定位开关", false);
    putBoolean("模拟定位开关", "模拟定位开关", state);
    vibrate(finalActivity, 48);
    if (state) {
        开模拟定位();
    } else {
        关模拟定位();
    }
}

Double[] getLocation() {
    String lngStr = getString("模拟定位", "lng", "");
    String latStr = getString("模拟定位", "lat", "");
    traceLog("api2_log", "[getLocation] 已保存 lng=" + lngStr + " lat=" + latStr);
    if (lngStr?.length() > 0 && latStr?.length() > 0) {
            double lng = Double.parseDouble(lngStr);
            double lat = Double.parseDouble(latStr);
            if (lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90) {
                traceLog("api2_log", "[getLocation] 返回自定义 lng=" + lng + " lat=" + lat);
                return new Double[]{lng, lat};
            }
    }
    traceLog("api2_log", "[getLocation] 返回默认 lng=" + 默认经度 + " lat=" + 默认纬度);
    return new Double[]{默认经度, 默认纬度};
}
private void initFakeLocation() {
    Double[] loc = getLocation();
    fakeLocation.setLatitude(loc[1]);
    fakeLocation.setLongitude(loc[0]);
    fakeLocation.setAccuracy(100);
    fakeLocation.setTime(System.currentTimeMillis());
    fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    fakeLocation.setSpeed(0.0f);
    fakeLocation.setBearing(0.0f);
    traceLog("api2_log", "[initFakeLocation] lng=" + loc[0] + " lat=" + loc[1] + " accuracy=100");
}

private Method findMethodSafe(Class cls, String name, int paramCount) {
    try {
        Method[] methods = cls.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name) && methods[i].getParameterTypes().length == paramCount) {
                return methods[i];
            }
        }
    } catch (Throwable e) { traceLog("api2_log", "[findMethodSafe] 异常: " + e); }
    return null;
}

private void hookLocation() {
    Class locationManagerCls = LocationManager.class;
    int successCount = 0;

    Method getLastKnownLocMethod = findMethodSafe(locationManagerCls, "getLastKnownLocation", 1);
    if (getLastKnownLocMethod != null) {
        try {
            getLastKnownLocMethod.setAccessible(true);
            Object unhook1 = XposedBridge.hookMethod(getLastKnownLocMethod, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
                    traceLog("api2_log", "[hook] getLastKnownLocation 被调用, provider=" + param.args[0]);
                    param.setResult(fakeLocation);
                }
            });
            hookloveList.add(unhook1);
            successCount++;
            traceLog("api2_log", "[hookLocation] getLastKnownLocation 已挂钩");
        } catch (Throwable e) {
            traceLog("api2_log", "[hookLocation] getLastKnownLocation 挂钩失败: " + e);
        }
    } else {
        traceLog("api2_log", "[hookLocation] getLastKnownLocation 未找到 (Android 14+), 已跳过");
    }

    Method getProviderMethod = findMethodSafe(locationManagerCls, "getProvider", 1);
    if (getProviderMethod != null) {
        try {
            getProviderMethod.setAccessible(true);
            Object unhook2 = XposedBridge.hookMethod(getProviderMethod, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    traceLog("api2_log", "[hook] getProvider 被调用, provider=" + param.args[0]);
                    if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                        param.setResult(new LocationProvider("gps", null) {
                            public boolean isEnabled() { return true; }
                            public boolean meetsCriteria(Criteria criteria) { return true; }
                            public String getName() { return LocationManager.GPS_PROVIDER; }
                            public int getAccuracy() { return Criteria.ACCURACY_FINE; }
                            public int getPowerRequirement() { return Criteria.POWER_LOW; }
                            public boolean hasMonetaryCost() { return false; }
                            public boolean supportsAltitude() { return true; }
                            public boolean supportsSpeed() { return true; }
                            public boolean supportsBearing() { return true; }
                        });
                    }
                }
            });
            hookloveList.add(unhook2);
            successCount++;
            traceLog("api2_log", "[hookLocation] getProvider 已挂钩");
        } catch (Throwable e) {
            traceLog("api2_log", "[hookLocation] getProvider 挂钩失败: " + e);
        }
    } else {
        traceLog("api2_log", "[hookLocation] getProvider 未找到, 已跳过");
    }

    Method getAllProvidersMethod = findMethodSafe(locationManagerCls, "getAllProviders", 0);
    if (getAllProvidersMethod != null) {
        try {
            getAllProvidersMethod.setAccessible(true);
            Object unhook3 = XposedBridge.hookMethod(getAllProvidersMethod, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    List providerList = new ArrayList();
                    providerList.add(LocationManager.GPS_PROVIDER);
                    param.setResult(providerList);
                }
            });
            hookloveList.add(unhook3);
            successCount++;
            traceLog("api2_log", "[hookLocation] getAllProviders 已挂钩");
        } catch (Throwable e) {
            traceLog("api2_log", "[hookLocation] getAllProviders 挂钩失败: " + e);
        }
    } else {
        traceLog("api2_log", "[hookLocation] getAllProviders 未找到, 已跳过");
    }

    Method isProviderEnabledMethod = findMethodSafe(locationManagerCls, "isProviderEnabled", 1);
    if (isProviderEnabledMethod != null) {
        try {
            isProviderEnabledMethod.setAccessible(true);
            Object unhook4 = XposedBridge.hookMethod(isProviderEnabledMethod, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    traceLog("api2_log", "[hook] isProviderEnabled 被调用, provider=" + param.args[0]);
                    if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                        param.setResult(true);
                    }
                }
            });
            hookloveList.add(unhook4);
            successCount++;
            traceLog("api2_log", "[hookLocation] isProviderEnabled 已挂钩");
        } catch (Throwable e) {
            traceLog("api2_log", "[hookLocation] isProviderEnabled 挂钩失败: " + e);
        }
    } else {
        traceLog("api2_log", "[hookLocation] isProviderEnabled 未找到, 已跳过");
    }

    Method getCurrentLocationMethod = findMethodSafe(locationManagerCls, "getCurrentLocation", 4);
    if (getCurrentLocationMethod != null) {
        try {
            getCurrentLocationMethod.setAccessible(true);
            Object unhook5 = XposedBridge.hookMethod(getCurrentLocationMethod, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
                    traceLog("api2_log", "[hook] getCurrentLocation 被调用, provider=" + param.args[0]);
                    Object cancellationSignal = param.args[1];
                    Object executor = param.args[2];
                    final Object consumer = param.args[3];
                    if (cancellationSignal != null) {
                        try {
                            Method setCancelMethod = cancellationSignal.getClass().getMethod("setOnCancelListener", Class.forName("android.os.CancellationSignal$OnCancelListener"));
                            setCancelMethod.invoke(cancellationSignal, new Object[]{null});
                            traceLog("api2_log", "[hook] getCurrentLocation cancellationSignal 已清除");
                        } catch (Throwable e) {
                            traceLog("api2_log", "[hook] getCurrentLocation cancellationSignal clear 错误: " + e);
                        }
                    }
                    if (consumer != null) {
                        final Location loc = fakeLocation;
                        Runnable callback = new Runnable() {
                            public void run() {
                                try {
                                    Method[] consumerMethods = consumer.getClass().getMethods();
                                    Method acceptMethod = null;
                                    for (int m = 0; m < consumerMethods.length; m++) {
                                        if (consumerMethods[m].getName().equals("accept") && consumerMethods[m].getParameterTypes().length == 1) {
                                            acceptMethod = consumerMethods[m];
                                            break;
                                        }
                                    }
                                    if (acceptMethod != null) {
                                        acceptMethod.setAccessible(true);
                                        acceptMethod.invoke(consumer, new Object[]{loc});
                                        traceLog("api2_log", "[hook] getCurrentLocation consumer.accept 已调用");
                                    } else {
                                        traceLog("api2_log", "[hook] getCurrentLocation accept 方法未找到");
                                    }
                                } catch (Throwable e) {
                                    traceLog("api2_log", "[hook] getCurrentLocation consumer.accept 错误: " + e);
                                }
                            }
                        };
                        if (executor != null) {
                            try {
                                Method executeMethod = executor.getClass().getMethod("execute", Runnable.class);
                                executeMethod.invoke(executor, callback);
                                traceLog("api2_log", "[hook] getCurrentLocation 已通过 executor 分发");
                            } catch (Throwable e) {
                                traceLog("api2_log", "[hook] getCurrentLocation executor 错误: " + e);
                                new Handler(Looper.getMainLooper()).post(callback);
                            }
                        } else {
                            new Handler(Looper.getMainLooper()).post(callback);
                            traceLog("api2_log", "[hook] getCurrentLocation 已通过主 handler 分发");
                        }
                    }
                    param.setResult(null);
                }
            });
            hookloveList.add(unhook5);
            successCount++;
            traceLog("api2_log", "[hookLocation] getCurrentLocation 已挂钩");
        } catch (Throwable e) {
            traceLog("api2_log", "[hookLocation] getCurrentLocation 挂钩失败: " + e);
        }
    } else {
        traceLog("api2_log", "[hookLocation] getCurrentLocation 未找到, 已跳过");
    }

    try {
        Method[] allMethods = locationManagerCls.getDeclaredMethods();
        int rluCount = 0;
        for (int i = 0; i < allMethods.length; i++) {
            Method method = allMethods[i];
            if (!method.getName().equals("requestLocationUpdates")) continue;
            try {
                method.setAccessible(true);
                Object unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
                    protected void beforeHookedMethod(MethodHookParam param) {
                        traceLog("api2_log", "[hook] requestLocationUpdates 被调用, argsCount=" + param.args.length);
                        LocationListener listener = null;
                        for (int j = 0; j < param.args.length; j++) {
                            Object arg = param.args[j];
                            if (arg instanceof LocationListener) {
                                listener = (LocationListener) arg;
                                break;
                            }
                        }
                        traceLog("api2_log", "[hook] requestLocationUpdates listenerFound=" + (listener != null));
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
                                    traceLog("api2_log", "[hook] requestLocationUpdates 监听器已添加, total=" + activeListeners.size());
                                }
                            }
                            final LocationListener finalListener = listener;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                public void run() {
                                    try {
                                        finalListener.onLocationChanged(fakeLocation);
                                        traceLog("api2_log", "[hook] requestLocationUpdates 立即回调已发送");
                                    } catch (Exception e) {
                                        traceLog("api2_log", "[hook] requestLocationUpdates immediate callback 错误: " + e);
                                    }
                                }
                            });
                        }
                        param.setResult(null);
                    }
                });
                hookloveList.add(unhook);
                rluCount++;
            } catch (Throwable e) {
                traceLog("api2_log", "[hookLocation] requestLocationUpdates[" + i + "] hook failed: " + e);
            }
        }
        successCount += rluCount;
        traceLog("api2_log", "[hookLocation] requestLocationUpdates 已挂钩 " + rluCount + " overloads");
    } catch (Throwable e) {
        traceLog("api2_log", "[hookLocation] requestLocationUpdates 扫描失败: " + e);
    }

    traceLog("api2_log", "[hookLocation] 完成, successCount=" + successCount);
    verifyHookStatus();
}

private void verifyHookStatus() {
    try {
        LocationManager lm = (LocationManager) ActivityThread.currentActivityThread().getApplication().getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        List providers = lm.getAllProviders();
    } catch (Throwable e) { traceLog("api2_log", "[verifyHookStatus] 异常: " + e); }
}

void 关模拟定位() {
    traceLog("api2_log", "[关模拟定位] called");
    getNowActivity().runOnUiThread(new Runnable() {
        public void run() {
            positionHandler.removeCallbacksAndMessages(null);
        }
    });
    putBoolean("模拟定位开关", "模拟定位开关", false);
}

void 开模拟定位() {
    traceLog("api2_log", "[开模拟定位] called, will start in 2s");
    if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
    positionHandler.postDelayed(new Runnable() {
        public void run() {
            if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
            try {
                traceLog("api2_log", "[开模拟定位] starting init+hook+updates");
                initFakeLocation();
                hookLocation();
                startLocationUpdates();
                traceLog("api2_log", "[开模拟定位] started successfully");
            } catch (Throwable t) {
                traceLog("api2_log", "[开模拟定位] 错误: " + t);
                toast("模拟定位启动失败: " + t.getMessage());
            }
        }
    }, 2000);
}

private void startLocationUpdates() {
    ThreadPool.execute(new Runnable() {
        public void run() {
            while (getBoolean("模拟定位开关", "模拟定位开关", false)) {
                try {
                    Thread.sleep(3000);
                    fakeLocation.setTime(System.currentTimeMillis());
                    fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    synchronized (activeListeners) {
                        traceLog("api2_log", "[startLocationUpdates] 轮询, listeners=" + activeListeners.size() + " lng=" + fakeLocation.getLongitude() + " lat=" + fakeLocation.getLatitude());
                        for (int i = 0; i < activeListeners.size(); i++) {
                            LocationListener listener = (LocationListener) activeListeners.get(i);
                            try {
                                listener.onLocationChanged(fakeLocation);
                            } catch (Exception e) {
                                traceLog("api2_log", "[startLocationUpdates] listener[" + i + "] 错误: " + e);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    traceLog("api2_log", "[startLocationUpdates] 已中断");
                    break;
                } catch (Exception e) {
                    traceLog("api2_log", "[startLocationUpdates] 错误: " + e);
                    break;
                }
            }
            traceLog("api2_log", "[startLocationUpdates] 循环结束");
        }
    });
}