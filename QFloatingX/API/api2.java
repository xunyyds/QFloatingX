private Handler positionHandler = new Handler(Looper.getMainLooper());
private Location fakeLocation = new Location(LocationManager.GPS_PROVIDER);
private List activeListeners = new ArrayList();
private Activity 最后Activity;

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
    traceLog("location_log", "[getLocation] saved lng=" + lngStr + " lat=" + latStr);
    if (lngStr?.length() > 0 && latStr?.length() > 0) {
            double lng = Double.parseDouble(lngStr);
            double lat = Double.parseDouble(latStr);
            if (lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90) {
                traceLog("location_log", "[getLocation] return custom lng=" + lng + " lat=" + lat);
                return new Double[]{lng, lat};
            }
    }
    traceLog("location_log", "[getLocation] return default lng=" + 默认经度 + " lat=" + 默认纬度);
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
    traceLog("location_log", "[initFakeLocation] lng=" + loc[0] + " lat=" + loc[1] + " accuracy=100");
}

private Method findMethodSafe(Class cls, String name, int paramCount) {
    try {
        Method[] methods = cls.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name) && methods[i].getParameterTypes().length == paramCount) {
                return methods[i];
            }
        }
    } catch (Throwable e) {}
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
                    traceLog("location_log", "[hook] getLastKnownLocation called, provider=" + param.args[0]);
                    param.setResult(fakeLocation);
                }
            });
            hookloveList.add(unhook1);
            successCount++;
            traceLog("location_log", "[hookLocation] getLastKnownLocation hooked");
        } catch (Throwable e) {
            traceLog("location_log", "[hookLocation] getLastKnownLocation hook failed: " + e);
        }
    } else {
        traceLog("location_log", "[hookLocation] getLastKnownLocation not found (Android 14+), skipped");
    }

    Method getProviderMethod = findMethodSafe(locationManagerCls, "getProvider", 1);
    if (getProviderMethod != null) {
        try {
            getProviderMethod.setAccessible(true);
            Object unhook2 = XposedBridge.hookMethod(getProviderMethod, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    traceLog("location_log", "[hook] getProvider called, provider=" + param.args[0]);
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
            traceLog("location_log", "[hookLocation] getProvider hooked");
        } catch (Throwable e) {
            traceLog("location_log", "[hookLocation] getProvider hook failed: " + e);
        }
    } else {
        traceLog("location_log", "[hookLocation] getProvider not found, skipped");
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
            traceLog("location_log", "[hookLocation] getAllProviders hooked");
        } catch (Throwable e) {
            traceLog("location_log", "[hookLocation] getAllProviders hook failed: " + e);
        }
    } else {
        traceLog("location_log", "[hookLocation] getAllProviders not found, skipped");
    }

    Method isProviderEnabledMethod = findMethodSafe(locationManagerCls, "isProviderEnabled", 1);
    if (isProviderEnabledMethod != null) {
        try {
            isProviderEnabledMethod.setAccessible(true);
            Object unhook4 = XposedBridge.hookMethod(isProviderEnabledMethod, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    traceLog("location_log", "[hook] isProviderEnabled called, provider=" + param.args[0]);
                    if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                        param.setResult(true);
                    }
                }
            });
            hookloveList.add(unhook4);
            successCount++;
            traceLog("location_log", "[hookLocation] isProviderEnabled hooked");
        } catch (Throwable e) {
            traceLog("location_log", "[hookLocation] isProviderEnabled hook failed: " + e);
        }
    } else {
        traceLog("location_log", "[hookLocation] isProviderEnabled not found, skipped");
    }

    Method getCurrentLocationMethod = findMethodSafe(locationManagerCls, "getCurrentLocation", 4);
    if (getCurrentLocationMethod != null) {
        try {
            getCurrentLocationMethod.setAccessible(true);
            Object unhook5 = XposedBridge.hookMethod(getCurrentLocationMethod, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
                    traceLog("location_log", "[hook] getCurrentLocation called, provider=" + param.args[0]);
                    Object cancellationSignal = param.args[1];
                    Object executor = param.args[2];
                    final Object consumer = param.args[3];
                    if (cancellationSignal != null) {
                        try {
                            Method setCancelMethod = cancellationSignal.getClass().getMethod("setOnCancelListener", Class.forName("android.os.CancellationSignal$OnCancelListener"));
                            setCancelMethod.invoke(cancellationSignal, new Object[]{null});
                            traceLog("location_log", "[hook] getCurrentLocation cancellationSignal cleared");
                        } catch (Throwable e) {
                            traceLog("location_log", "[hook] getCurrentLocation cancellationSignal clear error: " + e);
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
                                        traceLog("location_log", "[hook] getCurrentLocation consumer.accept invoked");
                                    } else {
                                        traceLog("location_log", "[hook] getCurrentLocation accept method not found");
                                    }
                                } catch (Throwable e) {
                                    traceLog("location_log", "[hook] getCurrentLocation consumer.accept error: " + e);
                                }
                            }
                        };
                        if (executor != null) {
                            try {
                                Method executeMethod = executor.getClass().getMethod("execute", Runnable.class);
                                executeMethod.invoke(executor, callback);
                                traceLog("location_log", "[hook] getCurrentLocation dispatched via executor");
                            } catch (Throwable e) {
                                traceLog("location_log", "[hook] getCurrentLocation executor error: " + e);
                                new Handler(Looper.getMainLooper()).post(callback);
                            }
                        } else {
                            new Handler(Looper.getMainLooper()).post(callback);
                            traceLog("location_log", "[hook] getCurrentLocation dispatched via main handler");
                        }
                    }
                    param.setResult(null);
                }
            });
            hookloveList.add(unhook5);
            successCount++;
            traceLog("location_log", "[hookLocation] getCurrentLocation hooked");
        } catch (Throwable e) {
            traceLog("location_log", "[hookLocation] getCurrentLocation hook failed: " + e);
        }
    } else {
        traceLog("location_log", "[hookLocation] getCurrentLocation not found, skipped");
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
                        traceLog("location_log", "[hook] requestLocationUpdates called, argsCount=" + param.args.length);
                        LocationListener listener = null;
                        for (int j = 0; j < param.args.length; j++) {
                            Object arg = param.args[j];
                            if (arg instanceof LocationListener) {
                                listener = (LocationListener) arg;
                                break;
                            }
                        }
                        traceLog("location_log", "[hook] requestLocationUpdates listenerFound=" + (listener != null));
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
                                    traceLog("location_log", "[hook] requestLocationUpdates listener added, total=" + activeListeners.size());
                                }
                            }
                            final LocationListener finalListener = listener;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                public void run() {
                                    try {
                                        finalListener.onLocationChanged(fakeLocation);
                                        traceLog("location_log", "[hook] requestLocationUpdates immediate callback sent");
                                    } catch (Exception e) {
                                        traceLog("location_log", "[hook] requestLocationUpdates immediate callback error: " + e);
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
                traceLog("location_log", "[hookLocation] requestLocationUpdates[" + i + "] hook failed: " + e);
            }
        }
        successCount += rluCount;
        traceLog("location_log", "[hookLocation] requestLocationUpdates hooked " + rluCount + " overloads");
    } catch (Throwable e) {
        traceLog("location_log", "[hookLocation] requestLocationUpdates scan failed: " + e);
    }

    traceLog("location_log", "[hookLocation] done, successCount=" + successCount);
    verifyHookStatus();
}

private void verifyHookStatus() {
    try {
        LocationManager lm = (LocationManager) ActivityThread.currentActivityThread().getApplication().getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        List providers = lm.getAllProviders();
    } catch (Exception e) {}
}

void 关模拟定位() {
    traceLog("location_log", "[关模拟定位] called");
    getNowActivity().runOnUiThread(new Runnable() {
        public void run() {
            positionHandler.removeCallbacksAndMessages(null);
        }
    });
    putBoolean("模拟定位开关", "模拟定位开关", false);
}

void 开模拟定位() {
    traceLog("location_log", "[开模拟定位] called, will start in 2s");
    if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
    positionHandler.postDelayed(new Runnable() {
        public void run() {
            if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
            try {
                traceLog("location_log", "[开模拟定位] starting init+hook+updates");
                initFakeLocation();
                hookLocation();
                startLocationUpdates();
                traceLog("location_log", "[开模拟定位] started successfully");
            } catch (Throwable t) {
                traceLog("location_log", "[开模拟定位] error: " + t);
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
                        traceLog("location_log", "[startLocationUpdates] tick, listeners=" + activeListeners.size() + " lng=" + fakeLocation.getLongitude() + " lat=" + fakeLocation.getLatitude());
                        for (int i = 0; i < activeListeners.size(); i++) {
                            LocationListener listener = (LocationListener) activeListeners.get(i);
                            try {
                                listener.onLocationChanged(fakeLocation);
                            } catch (Exception e) {
                                traceLog("location_log", "[startLocationUpdates] listener[" + i + "] error: " + e);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    traceLog("location_log", "[startLocationUpdates] interrupted");
                    break;
                } catch (Exception e) {
                    traceLog("location_log", "[startLocationUpdates] error: " + e);
                    break;
                }
            }
            traceLog("location_log", "[startLocationUpdates] loop ended");
        }
    });
}