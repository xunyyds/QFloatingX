private Handler positionHandler = new Handler(Looper.getMainLooper());
private Location fakeLocation = new Location(LocationManager.GPS_PROVIDER);
private List activeListeners = new ArrayList();
private final java.util.Map listenerDispatchers = new java.util.concurrent.ConcurrentHashMap();
private volatile boolean locationHooked = false;
private volatile boolean locationGetterHooked = false;
private volatile boolean locationDispatchRunning = false;

public void 模拟定位开关() {
    Activity activity = safeActivity();
    if (activity == null) return;
    boolean state = !getBoolean("模拟定位开关", "模拟定位开关", false);
    putBoolean("模拟定位开关", "模拟定位开关", state);
    vibrate(activity, 48);
    if (state) 开模拟定位(); else 关模拟定位();
}

Double[] getLocation() {
    String lngStr = getString("模拟定位", "lng", "");
    String latStr = getString("模拟定位", "lat", "");
    if (lngStr != null && lngStr.length() > 0 && latStr != null && latStr.length() > 0) {
        try {
            double lng = Double.parseDouble(lngStr);
            double lat = Double.parseDouble(latStr);
            if (lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90) {
                return new Double[]{lng, lat};
            }
        } catch (Throwable t) {}
    }
    return new Double[]{默认经度, 默认纬度};
}

private void initFakeLocation() {
    Double[] loc = getLocation();
    fakeLocation.setLatitude(loc[1]);
    fakeLocation.setLongitude(loc[0]);
    fakeLocation.setAccuracy(10f);
    fakeLocation.setAltitude(30.0);
    fakeLocation.setSpeed(0.0f);
    fakeLocation.setBearing(0.0f);
    fakeLocation.setTime(System.currentTimeMillis());
    fakeLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    fakeLocation.setProvider(LocationManager.GPS_PROVIDER);
}

private Location createFreshLocation(String provider) {
    Location loc = new Location(provider != null ? provider : LocationManager.GPS_PROVIDER);
    loc.setLatitude(fakeLocation.getLatitude());
    loc.setLongitude(fakeLocation.getLongitude());
    loc.setAccuracy(10f);
    loc.setAltitude(30.0);
    loc.setSpeed(0.0f);
    loc.setBearing(0.0f);
    loc.setTime(System.currentTimeMillis());
    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    Bundle extras = new Bundle();
    extras.putBoolean("noGPSLocation", false);
    extras.putString("Source", "GPS");
    extras.putInt("Satellites", 12);
    extras.putBoolean("isValid", true);
    loc.setExtras(extras);
    return loc;
}

private void dispatchLocationToListener(final LocationListener listener, final Location loc) {
    final String p = loc.getProvider() != null ? loc.getProvider() : LocationManager.GPS_PROVIDER;
    Runnable r = new Runnable() {
        public void run() {
            try { listener.onStatusChanged(p, LocationProvider.AVAILABLE, null); } catch (Throwable t) {}
            try { listener.onLocationChanged(loc); } catch (Throwable t) {}
        }
    };
    postToDispatcher(listenerDispatchers.get(listener), r);
}

String[] MOCK_GETTERS = {"getLatitude", "getLongitude", "getAccuracy", "getTime", "getSpeed", "getBearing", "isFromMockProvider", "isMock"};

private boolean hookLocationGetters() {
    if (locationGetterHooked) return true;
    locationGetterHooked = true;
    boolean any = false;
    for (int i = 0; i < MOCK_GETTERS.length; i++) {
        String mn = MOCK_GETTERS[i];
        Method m = findMethodByCount(Location.class, mn, 0);
        if (m == null) continue;
        final String[] nameRef = new String[]{mn};
        hook("mock_location", m, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.thisObject == fakeLocation) return;
                param.setResult(mockGetterValue(nameRef[0]));
            }
        });
        if ("getLatitude".equals(mn) || "isFromMockProvider".equals(mn) || "isMock".equals(mn)) any = true;
    }
    return any;
}

Object mockGetterValue(String mn) {
    if ("getLatitude".equals(mn)) return new Double(fakeLocation.getLatitude());
    if ("getLongitude".equals(mn)) return new Double(fakeLocation.getLongitude());
    if ("getAccuracy".equals(mn)) return new Float(10f);
    if ("getTime".equals(mn)) return new Long(System.currentTimeMillis());
    if ("getSpeed".equals(mn)) return new Float(0f);
    if ("getBearing".equals(mn)) return new Float(0f);
    if ("isFromMockProvider".equals(mn) || "isMock".equals(mn)) return Boolean.FALSE;
    return null;
}

private boolean hookLocation() {
    if (locationHooked) return true;
    locationHooked = true;
    Class cls = LocationManager.class;
    boolean any = false;

    hookByName("mock_location", cls, "getLastLocation", 0, new XC_MethodHook() {
        protected void beforeHookedMethod(MethodHookParam param) {
            param.setResult(createFreshLocation(LocationManager.GPS_PROVIDER));
        }
    });
    any = true;

    hookByName("mock_location", cls, "getLastKnownLocation", 1, new XC_MethodHook() {
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args == null || param.args.length < 1) return;
            param.setResult(createFreshLocation(String.valueOf(param.args[0])));
        }
    });
    any = true;

    hookByName("mock_location", cls, "getProvider", 1, new XC_MethodHook() {
        protected void afterHookedMethod(MethodHookParam param) {
            if (param.args == null || param.args.length < 1) return;
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

    hookByName("mock_location", cls, "getAllProviders", 0, new XC_MethodHook() {
        protected void afterHookedMethod(MethodHookParam param) {
            List l = new ArrayList();
            l.add(LocationManager.GPS_PROVIDER);
            l.add(LocationManager.NETWORK_PROVIDER);
            param.setResult(l);
        }
    });

    XC_MethodHook providerEnabledHook = new XC_MethodHook() {
        protected void afterHookedMethod(MethodHookParam param) {
            if (param.args == null || param.args.length < 1) return;
            String p = String.valueOf(param.args[0]);
            if (LocationManager.GPS_PROVIDER.equals(p) || LocationManager.NETWORK_PROVIDER.equals(p)) {
                param.setResult(Boolean.TRUE);
            }
        }
    };

    hookByName("mock_location", cls, "isProviderEnabled", 1, providerEnabledHook);
    hookByName("mock_location", cls, "hasProvider", 1, providerEnabledHook);

    hookAll("mock_location", cls, "getCurrentLocation", new XC_MethodHook() {
        protected void beforeHookedMethod(MethodHookParam param) {
            Object executor = null;
            Object consumer = null;
            if (param.args == null) { param.setResult(null); return; }
            for (int j = 0; j < param.args.length; j++) {
                Object a = param.args[j];
                if (a == null) continue;
                String cn = a.getClass().getName();
                if (cn.contains("Executor")) executor = a;
                else if (cn.contains("Consumer") || cn.contains("OutcomeReceiver") || cn.contains("Function")) consumer = a;
            }
            if (consumer != null) {
                final Object fc = consumer;
                final Location loc = createFreshLocation(LocationManager.GPS_PROVIDER);
                Runnable cb = new Runnable() {
                    public void run() {
                        invokeReceiver(fc, loc);
                    }
                };
                postToDispatcher(executor, cb);
            }
            param.setResult(null);
        }
    });

    hookAll("mock_location", cls, "requestLocationUpdates", new XC_MethodHook() {
        protected void beforeHookedMethod(MethodHookParam param) {
            LocationListener listener = null;
            Looper looper = null;
            Object dispatcher = null;
            String provider = null;
            if (param.args == null) { param.setResult(null); return; }
            for (int j = 0; j < param.args.length; j++) {
                Object arg = param.args[j];
                if (arg == null) continue;
                if (arg instanceof LocationListener) listener = (LocationListener) arg;
                else if (arg instanceof Looper) looper = (Looper) arg;
                else if (arg instanceof Handler) dispatcher = arg;
                else if (arg instanceof java.util.concurrent.Executor) dispatcher = arg;
                else if (arg instanceof String && provider == null) provider = (String) arg;
            }
            if (dispatcher == null) {
                if (looper != null) dispatcher = new Handler(looper);
                else {
                    Looper cl = Looper.myLooper();
                    dispatcher = new Handler(cl != null ? cl : Looper.getMainLooper());
                }
            }
            if (listener != null) {
                synchronized (activeListeners) {
                    if (!activeListeners.contains(listener)) activeListeners.add(listener);
                }
                listenerDispatchers.put(listener, dispatcher);
                final LocationListener fl = listener;
                final String fp = provider != null ? provider : LocationManager.GPS_PROVIDER;
                Runnable firstCb = new Runnable() {
                    public void run() {
                        try { fl.onProviderEnabled(fp); } catch (Throwable t) {}
                        try { fl.onStatusChanged(fp, LocationProvider.AVAILABLE, null); } catch (Throwable t) {}
                        try { fl.onLocationChanged(createFreshLocation(fp)); } catch (Throwable t) {}
                    }
                };
                postToDispatcher(dispatcher, firstCb);
            }
            param.setResult(null);
        }
    });

    hookAll("mock_location", cls, "removeUpdates", new XC_MethodHook() {
        protected void beforeHookedMethod(MethodHookParam param) {
            if (param.args == null) return;
            for (int j = 0; j < param.args.length; j++) {
                Object arg = param.args[j];
                if (arg instanceof LocationListener) removeActiveListener((LocationListener) arg);
            }
        }
    });

    return any;
}

void invokeReceiver(Object receiver, Location loc) {
    try {
        Method[] cm = receiver.getClass().getMethods();
        for (int k = 0; k < cm.length; k++) {
            String nm = cm[k].getName();
            if ((nm.equals("accept") || nm.equals("invoke") || nm.equals("onResult"))
                && cm[k].getParameterTypes().length == 1) {
                cm[k].setAccessible(true);
                cm[k].invoke(receiver, new Object[]{loc});
                return;
            }
        }
    } catch (Throwable t) {
        traceLog("api2_log", "[invokeReceiver] 回调失败: " + t);
    }
}

void postToDispatcher(Object dispatcher, Runnable cb) {
    if (dispatcher == null) {
        new Handler(Looper.getMainLooper()).post(cb);
        return;
    }
    if (dispatcher instanceof Handler) { ((Handler) dispatcher).post(cb); return; }
    if (dispatcher instanceof java.util.concurrent.Executor) { ((java.util.concurrent.Executor) dispatcher).execute(cb); return; }
    new Handler(Looper.getMainLooper()).post(cb);
}

void setMockLocationEnabled(boolean on) {
    putBoolean("模拟定位开关", "模拟定位开关", on);
    if (on) 开模拟定位();
    else 关模拟定位();
}

void 关模拟定位() {
    stopLocationUpdates();
    Activity a = safeActivity();
    if (a != null) {
        final Activity fa = a;
        fa.runOnUiThread(new Runnable() {
            public void run() { positionHandler.removeCallbacksAndMessages(null); }
        });
    } else {
        positionHandler.removeCallbacksAndMessages(null);
    }
    putBoolean("模拟定位开关", "模拟定位开关", false);
    try { unhook("mock_location"); } catch (Throwable e) { traceLog("api2_log", "[关模拟定位] 卸载hook异常: " + e); }
    locationHooked = false;
    locationGetterHooked = false;
}

void 开模拟定位() {
    if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
    positionHandler.postDelayed(new Runnable() {
        public void run() {
            if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
            try {
                initFakeLocation();
                boolean okGetter = hookLocationGetters();
                boolean okLoc = hookLocation();
                if (!okGetter || !okLoc) {
                    showApiProtectGuide(safeActivity());
                }
                startLocationUpdates();
            } catch (Throwable t) {
                showApiProtectGuide(safeActivity());
            }
        }
    }, 2000);
}

private void startLocationUpdates() {
    if (locationDispatchRunning) return;
    locationDispatchRunning = true;
    positionHandler.postDelayed(new Runnable() {
        public void run() { dispatchTick(); }
    }, 1000);
}

private void dispatchTick() {
    if (!locationDispatchRunning) return;
    try {
        synchronized (activeListeners) {
            for (int i = 0; i < activeListeners.size(); i++) {
                LocationListener listener = (LocationListener) activeListeners.get(i);
                dispatchLocationToListener(listener, createFreshLocation(LocationManager.GPS_PROVIDER));
            }
        }
    } catch (Exception e) {}
    positionHandler.postDelayed(new Runnable() {
        public void run() { dispatchTick(); }
    }, 1000);
}

private void stopLocationUpdates() {
    locationDispatchRunning = false;
    positionHandler.removeCallbacksAndMessages(null);
}

private void removeActiveListener(LocationListener listener) {
    if (listener == null) return;
    synchronized (activeListeners) {
        for (int i = activeListeners.size() - 1; i >= 0; i--) {
            if (activeListeners.get(i) == listener) activeListeners.remove(i);
        }
    }
    listenerDispatchers.remove(listener);
}
