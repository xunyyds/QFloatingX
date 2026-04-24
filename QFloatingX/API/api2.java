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
        toast("正在开启模拟定位...");
        开模拟定位();
    } else {
        toast("正在关闭模拟定位...");
        关模拟定位();
    }
}

private void initFakeLocation() {
    String locStr = getLocationData();
    Double[] loc = splitLocation(locStr);
    if (loc == null || loc.length < 2) throw new RuntimeException("定位数据格式错误");
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

private void hookLocation() {
    try {
        Class locationManagerCls = LocationManager.class;

        Method getLastKnownLocMethod = locationManagerCls.getDeclaredMethod("getLastKnownLocation", String.class);
        getLastKnownLocMethod.setAccessible(true);
        Object unhook1 = XposedBridge.hookMethod(getLastKnownLocMethod, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(fakeLocation);
            }
        });
        hookloveList.add(unhook1);

        Method getProviderMethod = locationManagerCls.getDeclaredMethod("getProvider", String.class);
        getProviderMethod.setAccessible(true);
        Object unhook2 = XposedBridge.hookMethod(getProviderMethod, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
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

        Method getAllProvidersMethod = locationManagerCls.getDeclaredMethod("getAllProviders");
        getAllProvidersMethod.setAccessible(true);
        Object unhook3 = XposedBridge.hookMethod(getAllProvidersMethod, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                List providerList = new ArrayList();
                providerList.add(LocationManager.GPS_PROVIDER);
                param.setResult(providerList);
            }
        });
        hookloveList.add(unhook3);

        Method isProviderEnabledMethod = locationManagerCls.getDeclaredMethod("isProviderEnabled", String.class);
        isProviderEnabledMethod.setAccessible(true);
        Object unhook4 = XposedBridge.hookMethod(isProviderEnabledMethod, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                    param.setResult(true);
                }
            }
        });
        hookloveList.add(unhook4);

        Method[] allMethods = locationManagerCls.getDeclaredMethods();
        for (int i = 0; i < allMethods.length; i++) {
            Method method = allMethods[i];
            if (!method.getName().equals("requestLocationUpdates")) continue;
            method.setAccessible(true);
            Object unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
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
                                } catch (Exception e) {}
                            }
                        });
                    }
                    param.setResult(null);
                }
            });
            hookloveList.add(unhook);
        }

        verifyHookStatus();
    } catch (Throwable e) {
        throw new RuntimeException(e);
    }
}

private void verifyHookStatus() {
    try {
        LocationManager lm = (LocationManager) ActivityThread.currentActivityThread().getApplication().getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        List providers = lm.getAllProviders();
    } catch (Exception e) {}
}

void 关模拟定位() {
    getNowActivity().runOnUiThread(new Runnable() {
        public void run() {
            positionHandler.removeCallbacksAndMessages(null);
        }
    });
    putBoolean("模拟定位开关", "模拟定位开关", false);
    toast("模拟定位已关闭");
}

void 开模拟定位() {
    if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
    positionHandler.postDelayed(new Runnable() {
        public void run() {
            if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
            try {
                initFakeLocation();
                hookLocation();
                startLocationUpdates();
                toast("模拟定位已开启");
            } catch (Throwable t) {
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
                        for (int i = 0; i < activeListeners.size(); i++) {
                            LocationListener listener = (LocationListener) activeListeners.get(i);
                            try {
                                listener.onLocationChanged(fakeLocation);
                            } catch (Exception e) {}
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    break;
                }
            }
        }
    });
}