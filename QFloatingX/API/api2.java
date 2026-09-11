private Handler positionHandler = new Handler(Looper.getMainLooper());
private Location fakeLocation = new Location(LocationManager.GPS_PROVIDER);
private List activeListeners = new ArrayList();
private final java.util.Map listenerDispatchers = new java.util.concurrent.ConcurrentHashMap();
private volatile boolean locationHooked = false;
private volatile boolean locationGetterHooked = false;

public void 模拟定位开关() {
    Activity activity = getNowActivity();
    if (activity == null) activity = 最后Activity;
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
    Object dispatcher = listenerDispatchers.get(listener);
    final String p = loc.getProvider() != null ? loc.getProvider() : LocationManager.GPS_PROVIDER;
    Runnable r = new Runnable() {
        public void run() {
            try { listener.onStatusChanged(p, LocationProvider.AVAILABLE, null); } catch (Throwable t) {}
            try { listener.onLocationChanged(loc); } catch (Throwable t) {}
        }
    };
    if (dispatcher instanceof Handler) ((Handler) dispatcher).post(r);
    else if (dispatcher instanceof java.util.concurrent.Executor) ((java.util.concurrent.Executor) dispatcher).execute(r);
    else new Handler(Looper.getMainLooper()).post(r);
}

private Method findMethodByCount(Class cls, String name, int paramCount) {
    try {
        Method[] methods = cls.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name) && methods[i].getParameterTypes().length == paramCount) return methods[i];
        }
    } catch (Throwable e) {}
    try {
        Method[] methods = cls.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name) && methods[i].getParameterTypes().length == paramCount) return methods[i];
        }
    } catch (Throwable e) {}
    return null;
}

private void hookLocationGetters() {
    if (locationGetterHooked) return;
    locationGetterHooked = true;
    Method[] methods;
    try { methods = Location.class.getDeclaredMethods(); } catch (Throwable e) { return; }
    for (int i = 0; i < methods.length; i++) {
        final Method m = methods[i];
        final String mn = m.getName();
        if (m.getParameterTypes().length != 0) continue;
        try {
            m.setAccessible(true);
            if ("getLatitude".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject == fakeLocation) return;
                        param.setResult(fakeLocation.getLatitude());
                    }
                });
            } else if ("getLongitude".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject == fakeLocation) return;
                        param.setResult(fakeLocation.getLongitude());
                    }
                });
            } else if ("getAccuracy".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject == fakeLocation) return;
                        param.setResult(10f);
                    }
                });
            } else if ("getTime".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject == fakeLocation) return;
                        param.setResult(System.currentTimeMillis());
                    }
                });
            } else if ("getSpeed".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject == fakeLocation) return;
                        param.setResult(0.0f);
                    }
                });
            } else if ("getBearing".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject == fakeLocation) return;
                        param.setResult(0.0f);
                    }
                });
            } else if ("isFromMockProvider".equals(mn) || "isMock".equals(mn)) {
                hook("mock_location", m, new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });
            }
        } catch (Throwable e) {}
    }
}

private void hookLocation() {
    if (locationHooked) return;
    locationHooked = true;
    Class cls = LocationManager.class;

    Method m;
    m = findMethodByCount(cls, "getLastLocation", 0);
    if (m != null) try {
        m.setAccessible(true);
        hook("mock_location", m, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(createFreshLocation(LocationManager.GPS_PROVIDER));
            }
        });
    } catch (Throwable e) {}

    m = findMethodByCount(cls, "getLastKnownLocation", 1);
    if (m != null) try {
        m.setAccessible(true);
        hook("mock_location", m, new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(createFreshLocation(String.valueOf(param.args[0])));
            }
        });
    } catch (Throwable e) {}

    m = findMethodByCount(cls, "getProvider", 1);
    if (m != null) try {
        m.setAccessible(true);
        hook("mock_location", m, new XC_MethodHook() {
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
    } catch (Throwable e) {}

    m = findMethodByCount(cls, "getAllProviders", 0);
    if (m != null) try {
        m.setAccessible(true);
        hook("mock_location", m, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                List l = new ArrayList();
                l.add(LocationManager.GPS_PROVIDER);
                l.add(LocationManager.NETWORK_PROVIDER);
                param.setResult(l);
            }
        });
    } catch (Throwable e) {}

    m = findMethodByCount(cls, "isProviderEnabled", 1);
    if (m != null) try {
        m.setAccessible(true);
        hook("mock_location", m, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                String p = String.valueOf(param.args[0]);
                if (LocationManager.GPS_PROVIDER.equals(p) || LocationManager.NETWORK_PROVIDER.equals(p)) {
                    param.setResult(true);
                }
            }
        });
    } catch (Throwable e) {}

    m = findMethodByCount(cls, "hasProvider", 1);
    if (m != null) try {
        m.setAccessible(true);
        hook("mock_location", m, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                String p = String.valueOf(param.args[0]);
                if (LocationManager.GPS_PROVIDER.equals(p) || LocationManager.NETWORK_PROVIDER.equals(p)) {
                    param.setResult(true);
                }
            }
        });
    } catch (Throwable e) {}

    try {
        Method[] allMethods = cls.getDeclaredMethods();
        for (int i = 0; i < allMethods.length; i++) {
            final Method method = allMethods[i];
            String name = method.getName();
            try {
                method.setAccessible(true);
                if ("getCurrentLocation".equals(name)) {
                    hook("mock_location", method, new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object executor = null;
                            Object consumer = null;
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
                                        try {
                                            Method[] cm = fc.getClass().getMethods();
                                            for (int k = 0; k < cm.length; k++) {
                                                String nm = cm[k].getName();
                                                if ((nm.equals("accept") || nm.equals("invoke") || nm.equals("onResult"))
                                                    && cm[k].getParameterTypes().length == 1) {
                                                    cm[k].setAccessible(true);
                                                    cm[k].invoke(fc, new Object[]{loc});
                                                    break;
                                                }
                                            }
                                        } catch (Throwable t) {}
                                    }
                                };
                                if (executor != null) {
                                    try {
                                        Method em = executor.getClass().getMethod("execute", Runnable.class);
                                        em.invoke(executor, cb);
                                    } catch (Throwable t) {
                                        new Handler(Looper.getMainLooper()).post(cb);
                                    }
                                } else {
                                    new Handler(Looper.getMainLooper()).post(cb);
                                }
                            }
                            param.setResult(null);
                        }
                    });
                } else if ("requestLocationUpdates".equals(name)) {
                    hook("mock_location", method, new XC_MethodHook() {
                        protected void beforeHookedMethod(MethodHookParam param) {
                            LocationListener listener = null;
                            Looper looper = null;
                            Object dispatcher = null;
                            String provider = null;
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
                                    boolean exists = false;
                                    for (int k = 0; k < activeListeners.size(); k++) {
                                        if (activeListeners.get(k) == listener) { exists = true; break; }
                                    }
                                    if (!exists) activeListeners.add(listener);
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
                                if (dispatcher instanceof Handler) ((Handler) dispatcher).post(firstCb);
                                else if (dispatcher instanceof java.util.concurrent.Executor) ((java.util.concurrent.Executor) dispatcher).execute(firstCb);
                                else new Handler(Looper.getMainLooper()).post(firstCb);
                            }
                            param.setResult(null);
                        }
                    });
                }
            } catch (Throwable e) {}
        }
    } catch (Throwable e) {}
}

void setMockLocationEnabled(boolean on) {
    putBoolean("模拟定位开关", "模拟定位开关", on);
    if (on) 开模拟定位();
    else 关模拟定位();
}

void 关模拟定位() {
    Activity a = getNowActivity();
    if (a == null) a = 最后Activity;
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
}

void 开模拟定位() {
    if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
    positionHandler.postDelayed(new Runnable() {
        public void run() {
            if (!getBoolean("模拟定位开关", "模拟定位开关", false)) return;
            try {
                initFakeLocation();
                hookLocationGetters();
                hookLocation();
                startLocationUpdates();
            } catch (Throwable t) {}
        }
    }, 2000);
}

private void startLocationUpdates() {
    ThreadPool.execute(new Runnable() {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                    synchronized (activeListeners) {
                        for (int i = 0; i < activeListeners.size(); i++) {
                            LocationListener listener = (LocationListener) activeListeners.get(i);
                            dispatchLocationToListener(listener, createFreshLocation(LocationManager.GPS_PROVIDER));
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {}
            }
        }
    });
}