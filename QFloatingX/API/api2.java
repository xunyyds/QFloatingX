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
        Toast("正在开启模拟定位...");
        开模拟定位();
    } else {
        Toast("正在关闭模拟定位...");
        关模拟定位();
    }
}

void 开模拟定位() {
	if(!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
    initFakeLocation();
    hookLocation();
    startLocationUpdates();
    Toast("模拟定位已开启");
}

void 关模拟定位() {
    getNowActivity().runOnUiThread(() -> positionHandler.removeCallbacksAndMessages(null));
    Toast("模拟定位已关闭");
}

private void initFakeLocation() {
    String locStr = getLocationData();
    Double[] loc = splitLocation(locStr);
    if (loc == null || loc.length < 2) throw new RuntimeException("坐标格式错误");
    double lng = loc[0];
    double lat = loc[1];
    fakeLocation.setLatitude(lat);
    fakeLocation.setLongitude(lng);
    fakeLocation.setAccuracy(100);
    fakeLocation.setTime(System.currentTimeMillis());
    fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    fakeLocation.setSpeed(0.0f);
    fakeLocation.setBearing(0.0f);
}

private void hookLocation() {
    hook(LocationManager.class, "getLastKnownLocation", new Class[]{String.class}, new XC_MethodHook() {
        protected void beforeHookedMethod(MethodHookParam param) {
            param.setResult(fakeLocation);
        }
    });

    hook(LocationManager.class, "getProvider", new Class[]{String.class}, new XC_MethodHook() {
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

    hook(LocationManager.class, "getAllProviders", new Class[0], new XC_MethodHook() {
        protected void afterHookedMethod(MethodHookParam param) {
            List<String> list = new ArrayList<>();
            list.add(LocationManager.GPS_PROVIDER);
            param.setResult(list);
        }
    });

    hook(LocationManager.class, "isProviderEnabled", new Class[]{String.class}, new XC_MethodHook() {
        protected void afterHookedMethod(MethodHookParam param) {
            if (LocationManager.GPS_PROVIDER.equals(param.args[0])) {
                param.setResult(true);
            }
        }
    });

    for (Method method : LocationManager.class.getDeclaredMethods()) {
        if (!method.getName().equals("requestLocationUpdates")) continue;
        hook(LocationManager.class, "requestLocationUpdates", method.getParameterTypes(), new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) {
                LocationListener listener = null;
                for (Object arg : param.args) {
                    if (arg instanceof LocationListener) {
                        listener = (LocationListener) arg;
                        break;
                    }
                }
                if (listener != null) {
                    synchronized (activeListeners) {
                        if (!activeListeners.contains(listener)) {
                            activeListeners.add(listener);
                        }
                    }
                    final LocationListener fl = listener;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try { fl.onLocationChanged(fakeLocation); } catch (Exception ignored) {}
                    });
                }
                param.setResult(null);
            }
        });
    }
}

private void startLocationUpdates() {
    Runnable updateTask = new Runnable() {
        public void run() {
            if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
            fakeLocation.setTime(System.currentTimeMillis());
            fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            synchronized (activeListeners) {
                for (Object obj : activeListeners) {
                    try {
                        ((LocationListener) obj).onLocationChanged(fakeLocation);
                    } catch (Exception ignored) {}
                }
            }
            positionHandler.postDelayed(this, 3000);
        }
    };
    positionHandler.post(updateTask);
}