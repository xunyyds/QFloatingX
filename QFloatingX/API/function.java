

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

HashMap runningThreads = new HashMap();

ConcurrentHashMap stopFlags = new ConcurrentHashMap();

HashMap loopFlags = new HashMap();
HashMap loadFlags = new HashMap();
HashMap runFlags = new HashMap();

HashMap eventRegistry = new HashMap();

HashMap lastExecTime = new HashMap();

HashMap preProcConfig = new HashMap();
boolean inPreproc = false;

Queue sendMsgQueue = new ConcurrentLinkedQueue();

java.util.concurrent.atomic.AtomicBoolean isProcessingQueue = new java.util.concurrent.atomic.AtomicBoolean(false);

String currentPeerUin = "";

int currentChatType = 0;

boolean isAdding = false;

class FormComponents {
    EditText etName;        
    EditText etCode;        
    TextView chipFile;      
    TextView[] chips;       
    TextView chipLoop;      
    EditText etInterval;    
    EditText etCount;       
    EditText etTime;        
    LinearLayout loopSettings; 
    
    
    LinearLayout preprocContainer; 
    TextView chipPreType;   
    EditText etPreTail;     
    TextView[] preTypeChips; 
    EditText etRepeatSend;  
    EditText etRepeatConcat;
    TextView dynamicTips;   
    TextView prePreview;    
}

class SendUnit {
    String uin; 
    String msg; 
    int type;   
    
    SendUnit(String uin, String msg, int type) {
        this.uin = uin;
        this.msg = msg;
        this.type = type;
    }
}

class HotPlugClassLoader {
    ConcurrentHashMap objectCache = new ConcurrentHashMap(); 
    ConcurrentHashMap directMethodCache = new ConcurrentHashMap(); 
    ConcurrentHashMap modeCache = new ConcurrentHashMap(); 
    ConcurrentHashMap timeStampCache = new ConcurrentHashMap(); 
    ConcurrentHashMap hashCache = new ConcurrentHashMap(); 
    
    boolean interpreterInitialized = false; 
    
    
    final String[] SYNC_VARS = {
        "qun", "uin", "msg", "msgId", "msgType", "type", "data", "operator", "time", "paiType", "qq", "pluginPath", "this", "scriptLoader"
    };

    
    String getCodeHash(String code) {
        if (code == null || code.length() == 0) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(code.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(hexByte(b));
            return sb.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    
    bsh.NameSpace getObjNameSpace(bsh.This scriptObj) {
        try {
            java.lang.reflect.Method m = scriptObj.getClass().getMethod("getNameSpace", new Class[0]);
            return (bsh.NameSpace) m.invoke(scriptObj, new Object[0]);
        } catch (Throwable e) {
            traceLog("function_log", "[getObjNameSpace] 反射获取 NameSpace 失败: " + e);
            return null;
        }
    }

    
    void loadAndExecute(String funcName, bsh.Interpreter interpreter) {
        File f = new File(getDir() + "/" + funcName + ".java");
        if (!f.exists()) {
            traceLog("function_log", "[loadAndExecute] 文件不存在: " + funcName);
            return;
        }
        
        long lastMod = f.lastModified();
        Object cachedTime = timeStampCache.get(funcName);
        Object cachedHash = hashCache.get(funcName);
        
        boolean timeMatched = (cachedTime != null && modeCache.containsKey(funcName) && ((Long)cachedTime).longValue() == lastMod);
        boolean needsCompile = !timeMatched;
        String currentHash = null;
        if (needsCompile) {
            currentHash = getCodeHash(readCodeFile(funcName));
            cachedHash = hashCache.get(funcName);
            
            if (cachedHash != null && currentHash.equals(cachedHash) && modeCache.containsKey(funcName)) {
                hashCache.put(funcName, currentHash);
                timeStampCache.put(funcName, new Long(lastMod));
                needsCompile = false;
            }
        }
        
        if (needsCompile) {
            hashCache.put(funcName, currentHash);
            timeStampCache.put(funcName, new Long(lastMod));
            
            try {
                String code = readCodeFile(funcName);
                if (code == null || code.trim().equals("")) return;
                
                boolean hasRunMethod = java.util.regex.Pattern.compile("void\\s+run\\s*\\(").matcher(code).find();
                String hashStr = String.valueOf(Math.abs(funcName.hashCode()));
                
                if (hasRunMethod) {
                    String safeName = "OBJ_" + funcName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + hashStr;
                    String factoryDef = "bsh.This " + safeName + "() {\n" + code + "\n return this;\n}";
                    
                    this.interpreter.eval(factoryDef); 
                    bsh.This scriptObj = (bsh.This) this.interpreter.eval(safeName + "()");
                    
                    bsh.NameSpace ns = getObjNameSpace(scriptObj);
                    if (ns != null && ns.getMethod("run", new Class[0]) != null) {
                        objectCache.put(funcName, scriptObj);
                        modeCache.put(funcName, new Integer(1));
                    } else {
                        hasRunMethod = false;
                    }
                }
                
                if (!hasRunMethod) {
                    String methodName = "EXEC_" + funcName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + hashStr;
                    String methodDef = "void " + methodName + "() {\n" + code + "\n}";
                    this.interpreter.eval(methodDef);
                    
                    directMethodCache.put(funcName, methodName);
                    modeCache.put(funcName, new Integer(2));
                }
                
            } catch (Throwable e) {
                remove(funcName); 
                traceLog("function_log", "[编译失败] 功能 [" + funcName + "] 语法错误:\n" + e.toString());
                return;
            }
        }
        
        try {
            int mode = ((Integer)modeCache.get(funcName)).intValue();
            
            if (mode == 1) {
                bsh.This scriptObj = (bsh.This) objectCache.get(funcName);
                if (scriptObj == null) throw new Exception("脚本对象缓存为空");
                
                bsh.NameSpace ns = getObjNameSpace(scriptObj);
                if (ns != null) {
                    for(int i = 0; i < SYNC_VARS.length; i++) {
                        String key = SYNC_VARS[i];
                        Object val = this.interpreter.get(key);
                        if (val != null) ns.setVariable(key, val, false);
                    }
                    scriptObj.invokeMethod("run", new Object[0]);
                }
            } else {
                String methodName = (String) directMethodCache.get(funcName);
                this.interpreter.eval(methodName + "()");
            }
            
        } catch (Throwable e) {
            traceLog("function_log", "[运行异常] 功能 [" + funcName + "] 执行出错: " + e.getMessage());
            remove(funcName);
            if (e.toString().contains("Command not found")) remove(funcName);
        }
    }
    
    
    void remove(String funcName) {
        timeStampCache.remove(funcName);
        hashCache.remove(funcName);
        objectCache.remove(funcName);
        directMethodCache.remove(funcName);
        modeCache.remove(funcName);
        loadFlags.remove(funcName);
        runFlags.remove(funcName);
    }
}

HotPlugClassLoader scriptLoader = new HotPlugClassLoader();

String getDir() {
    String d = pluginPath + "/HotPlug/Funcs";
    try {
        File f = new File(d);
        if (!f.exists()) f.mkdirs();
    } catch (Throwable e) { traceLog("function_log", "[getDir] 异常: " + e); }
    return d;
}

String readCodeFile(String name) {
    try {
        File f = new File(getDir() + "/" + name + ".java");
        if (!f.exists()) return "";
        FileReader r = new FileReader(f);
        char[] b = new char[(int)f.length()];
        r.read(b);
        r.close();
        return new String(b);
    } catch (Throwable e) { return ""; }
}

String readExtFile(String p) {
    try {
        File f = new File(p);
        if (!f.exists()) return "";
        FileReader r = new FileReader(f);
        char[] b = new char[(int)f.length()];
        r.read(b);
        r.close();
        return new String(b);
    } catch (Throwable e) { return ""; }
}

void saveFunc(String n, String content, boolean isFile, boolean[] cb, boolean hasGrp, String timeVal, long interval, boolean isLoop, int loopCount, int preType, String preTail, int repeatSend, int repeatConcat) {
    String code;
    if (isFile) {
        code = readExtFile(content);
        if (code.equals("")) {
            toast("文件读取失败");
            return;
        }
    } else {
        code = content;
    }
    
    try {
        FileWriter w = new FileWriter(getDir() + "/" + n + ".java");
        w.write(code);
        w.close();
    } catch (Throwable e) {
        traceLog("function_log", "[saveFunc]" + e);
        return;
    }
    
    try {
        JSONObject jo = new JSONObject();
        jo.put("n", n);          
        jo.put("f", isFile);     
        JSONArray ja = new JSONArray();
        for(int i=0; i<7 && i<cb.length; i++) ja.put(cb[i] ? 1 : 0);
        jo.put("cb", ja);        
        jo.put("g", hasGrp);     
        jo.put("p", isFile ? content : ""); 
        jo.put("t", timeVal == null ? "" : timeVal); 
        jo.put("i", interval);   
        jo.put("l", isLoop);     
        jo.put("c", loopCount);  
        jo.put("pt", preType);   
        jo.put("tail", preTail == null ? "" : preTail); 
        jo.put("rs", repeatSend);    
        jo.put("rc", repeatConcat);  
        
        putString("HotPlug", "meta_" + n, jo.toString());
    } catch (Throwable e) {
        traceLog("function_log", "[saveFunc] " + e);
    }
    
    String list = getString("HotPlug", "list", "");
    if (!list.contains(n + ",")) putString("HotPlug", "list", list + n + ",");
    
    rebuildRegistry();
    
    if (cb.length > 6 && cb[6]) {
        HashMap cfg = new HashMap();
        cfg.put("type", preType);
        cfg.put("tail", preTail);
        cfg.put("rs", repeatSend);
        cfg.put("rc", repeatConcat);
        cfg.put("grp", hasGrp);
        preProcConfig.put(n, cfg);
    } else {
        preProcConfig.remove(n);
    }
    
    if (!hasCallback(cb)) {
        stopThread(n);
        
        String runRaw = getString("HotPlug", "run_" + n, "");
        if (runRaw.equals("")) {
            boolean needRun = (timeVal != null && !timeVal.trim().equals("")) || isLoop || interval > 0;
            if (needRun) setRun(n, true);
        }
        if (getLoad(n)) {
            startIndepThread(n, interval, loopCount);
        }
    }
    traceLog("function_log", "[saveFunc]" + n);
}

String[] getMeta(String n) {
    String m = getString("HotPlug", "meta_" + n, "");
    if (m.equals("")) return null;
    
    if (m.startsWith("{")) {
        try {
            JSONObject jo = new JSONObject(m);
            String[] res = new String[20];
            res[0] = jo.optString("n");
            res[1] = jo.optBoolean("f") ? "1" : "0";
            JSONArray ja = jo.optJSONArray("cb");
            for(int i=0; i<7; i++) {
                if (i < ja.length()) {
                    res[2+i] = ja.optInt(i) == 1 ? "1" : "0";
                } else {
                    res[2+i] = "0";
                }
            }
            res[9] = jo.optBoolean("g") ? "1" : "0";
            res[10] = jo.optString("p");
            res[11] = jo.optString("t");
            res[12] = String.valueOf(jo.optLong("i"));
            res[13] = jo.optBoolean("l") ? "1" : "0";
            res[14] = String.valueOf(jo.optInt("c"));
            res[15] = String.valueOf(jo.optInt("pt"));
            res[16] = jo.optString("tail");
            res[17] = String.valueOf(jo.optInt("rs"));
            res[18] = String.valueOf(jo.optInt("rc"));
            return res;
        } catch (Throwable e) {
            return null;
        }
    } else {
        String[] old = m.split("\\|", 20);
        String[] res = new String[20];
        System.arraycopy(old, 0, res, 0, Math.min(old.length, 20));
        for (int i = old.length; i < 20; i++) res[i] = "0";
        res[15] = res[15].equals("") ? "0" : res[15];
        return res;
    }
}

HashMap getPreProcConfig(String n) {
    String m = getString("HotPlug", "meta_" + n, "");
    if (m.equals("")) return null;
    try {
        JSONObject jo = new JSONObject(m);
        HashMap cfg = new HashMap();
        cfg.put("type", jo.optInt("pt"));
        cfg.put("tail", jo.optString("tail"));
        cfg.put("rs", jo.optInt("rs"));
        cfg.put("rc", jo.optInt("rc"));
        cfg.put("grp", jo.optBoolean("g"));
        return cfg;
    } catch (Throwable e) {
        return null;
    }
}

void rebuildRegistry() {
    HashMap newRegistry = new HashMap();
    HashMap newPreProc = new HashMap();
    String[] fs = getAll();
    for (int i = 0; i < fs.length; i++) {
        String f = fs[i];
        if (f.equals("")) continue;
        String[] m = getMeta(f);
        if (m == null) continue;
        
        for (int type = 1; type <= 7; type++) {
            if (m[type + 1].equals("1")) {
                Integer key = new Integer(type);
                if (!newRegistry.containsKey(key)) {
                    newRegistry.put(key, new ArrayList());
                }
                ArrayList list = (ArrayList) newRegistry.get(key);
                if (!list.contains(f)) list.add(f);
            }
        }
        
        if (m[8].equals("1")) {
            HashMap cfg = getPreProcConfig(f);
            if (cfg != null) {
                newPreProc.put(f, cfg);
            }
        }
    }
    eventRegistry = newRegistry;
    preProcConfig = newPreProc;
}

String[] getAll() {
    String l = getString("HotPlug", "list", "");
    if (l.equals("")) return new String[0];
    if (l.endsWith(",")) l = l.substring(0, l.length() - 1);
    return l.split(",");
}

void delFunc(String n) {
    stopThread(n); 
    try {
        new java.io.File(getDir() + "/" + n + ".java").delete();
    } catch (Throwable e) { traceLog("function_log", "[delFunc] 异常: " + e); }
    putString("HotPlug", "meta_" + n, ""); 
    String list = getString("HotPlug", "list", "");
    list = list.replace(n + ",", ""); 
    putString("HotPlug", "list", list);
    loopFlags.remove(n);
    scriptLoader.remove(n);
    preProcConfig.remove(n);
    rebuildRegistry();
}

boolean hasCallback(boolean[] cb) {
    for (int i = 0; i < cb.length && i < 7; i++) if (cb[i]) return true;
    return false;
}

void setLoad(String f, boolean on) { 
    putString("HotPlug", "load_" + f, on ? "1" : "0"); 
    loadFlags.put(f, new Boolean(on));
    String[] m = getMeta(f);
    if (m != null && !hasCallback(new boolean[]{m[2].equals("1"), m[3].equals("1"), m[4].equals("1"), m[5].equals("1"), m[6].equals("1"), m[7].equals("1"), m[8].equals("1")})) {
        if (on) {
            long interval = 0;
            int count = 0;
            try {
                interval = Long.parseLong(m[12]); 
                count = Integer.parseInt(m[14]); 
            } catch (Throwable e) { traceLog("function_log", "[setLoad] 异常: " + e); }
            startIndepThread(f, interval, count);
            
            String runRaw = getString("HotPlug", "run_" + f, "");
            if (runRaw.equals("")) setRun(f, true);
        } else {
            stopThread(f);
        }
    }
}

boolean getLoad(String f) {
    Boolean b = (Boolean)loadFlags.get(f);
    if (b != null) return b.booleanValue();
    boolean v = getString("HotPlug", "load_" + f, "0").equals("1");
    loadFlags.put(f, new Boolean(v));
    return v;
}

void setRun(String f, boolean on) { 
    putString("HotPlug", "run_" + f, on ? "1" : "0"); 
    runFlags.put(f, new Boolean(on));
}

boolean getRun(String f) {
    Boolean b = (Boolean)runFlags.get(f);
    if (b != null) return b.booleanValue();
    boolean v = getString("HotPlug", "run_" + f, "0").equals("1");
    runFlags.put(f, new Boolean(v));
    return v;
}

void setGrp(String f, String g, boolean on) { 
    putString("HotPlug", "grp_" + f + "_" + g, on ? "1" : "0"); 
}

boolean getGrp(String f, String g) { return getString("HotPlug", "grp_" + f + "_" + g, "0").equals("1"); }

void setLoop(String f, boolean on) { 
    putString("HotPlug", "loop_" + f, on ? "1" : "0");
    loopFlags.put(f, new Boolean(on)); 
}

boolean getLoop(String f) { 
    Boolean b = (Boolean)loopFlags.get(f);
    if (b != null) return b.booleanValue();
    return getString("HotPlug", "loop_" + f, "0").equals("1");
}

long getNextScheduleTime(String cfg, long notBefore) {
    if (cfg == null || cfg.equals("")) return 0;
    try {
        String raw = cfg.trim();
        if (raw.equals("")) return 0;
        String mode = "";
        String[] toks = null;
        if (raw.indexOf(":") > 0 && raw.indexOf(" ") < 0) {
            toks = raw.split(":");
            if (toks.length < 2) return 0;
            mode = toks[0].toLowerCase();
        } else {
            String[] parts = raw.split("\\s+");
            if (parts.length >= 4 && parts[0].toLowerCase().startsWith("w")) {
                mode = "w";
                toks = new String[]{"w", parts[0].substring(1), parts[1], parts[2], parts[3]};
            } else if (parts.length >= 4) {
                mode = "m";
                toks = new String[]{"m", parts[0], parts[1], parts[2], parts[3]};
            } else if (parts.length >= 3) {
                mode = "d";
                toks = new String[]{"d", parts[0], parts[1], parts[2]};
            } else if (!raw.contains(" ")) {
                String num = raw.replaceAll("[^0-9]", "");
                if (num.length() >= 4) {
                    String hh = num.substring(0, 2);
                    String mm = num.substring(2, 4);
                    String ss = num.length() >= 6 ? num.substring(4, 6) : "0";
                    mode = "d";
                    toks = new String[]{"d", hh, mm, ss};
                } else return 0;
            } else return 0;
        }
        int h = 0, mi = 0, s = 0;
        if (mode.equals("d")) {
            if (toks.length < 4) return 0;
            h = Integer.parseInt(toks[1]);
            mi = Integer.parseInt(toks[2]);
            s = Integer.parseInt(toks[3]);
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, h);
            target.set(Calendar.MINUTE, mi);
            target.set(Calendar.SECOND, s);
            target.set(Calendar.MILLISECOND, 0);
            while (target.getTimeInMillis() <= notBefore) {
                target.add(Calendar.DAY_OF_YEAR, 1);
            }
            return target.getTimeInMillis();
        }
        if (mode.equals("i")) {
            if (toks.length < 4) return 0;
            h = Integer.parseInt(toks[1]);
            mi = Integer.parseInt(toks[2]);
            s = Integer.parseInt(toks[3]);
            long span = h * 3600000L + mi * 60000L + s * 1000L;
            if (span <= 0) return 0;
            return notBefore + span;
        }
        if (toks.length < 5) return 0;
        int day = Integer.parseInt(toks[1]);
        h = Integer.parseInt(toks[2]);
        mi = Integer.parseInt(toks[3]);
        s = Integer.parseInt(toks[4]);
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, h);
        target.set(Calendar.MINUTE, mi);
        target.set(Calendar.SECOND, s);
        target.set(Calendar.MILLISECOND, 0);
        if (mode.equals("w")) {
            if (day < 1 || day > 7) return 0;
            target.set(Calendar.DAY_OF_WEEK, day);
            while (target.getTimeInMillis() <= notBefore) {
                target.add(Calendar.WEEK_OF_YEAR, 1);
            }
            return target.getTimeInMillis();
        }
        if (mode.equals("m")) {
            if (day < 1 || day > 31) return 0;
            target.set(Calendar.DAY_OF_MONTH, day);
            while (target.getTimeInMillis() <= notBefore) {
                target.add(Calendar.MONTH, 1);
            }
            return target.getTimeInMillis();
        }
        return 0;
    } catch (Throwable e) {
        return 0;
    }
}

String formatSchedule(String cfg) {
    if (cfg == null || cfg.equals("")) return "";
    try {
        String raw = cfg.trim();
        if (raw.equals("")) return "";
        String mode = "";
        String[] toks = null;
        if (raw.indexOf(":") > 0 && raw.indexOf(" ") < 0) {
            toks = raw.split(":");
            if (toks.length < 2) return cfg;
            mode = toks[0].toLowerCase();
        } else {
            String[] parts = raw.split("\\s+");
            if (parts.length >= 4 && parts[0].toLowerCase().startsWith("w")) {
                mode = "w";
                toks = new String[]{"w", parts[0].substring(1), parts[1], parts[2], parts[3]};
            } else if (parts.length >= 4) {
                mode = "m";
                toks = new String[]{"m", parts[0], parts[1], parts[2], parts[3]};
            } else if (parts.length >= 3) {
                mode = "d";
                toks = new String[]{"d", parts[0], parts[1], parts[2]};
            } else if (!raw.contains(" ")) {
                String num = raw.replaceAll("[^0-9]", "");
                if (num.length() >= 4) {
                    String hh = num.substring(0, 2);
                    String mm = num.substring(2, 4);
                    String ss = num.length() >= 6 ? num.substring(4, 6) : "0";
                    mode = "d";
                    toks = new String[]{"d", hh, mm, ss};
                } else return cfg;
            } else return cfg;
        }
        String time = "";
        if (mode.equals("d") && toks.length >= 4) {
            return "每日 " + toks[1] + ":" + toks[2] + ":" + toks[3];
        }
        if (mode.equals("i") && toks.length >= 4) {
            return "每隔 " + toks[1] + ":" + toks[2] + ":" + toks[3];
        }
        if (toks.length < 5) return cfg;
        time = toks[2] + ":" + toks[3] + ":" + toks[4];
        if (mode.equals("w")) {
            String[] weekDays = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            int day = Integer.parseInt(toks[1]);
            if (day >= 1 && day <= 7) return "每周" + weekDays[day] + " " + time;
            return "每周" + toks[1] + " " + time;
        }
        if (mode.equals("m")) {
            return "每月" + toks[1] + "日 " + time;
        }
    } catch (Throwable e) { traceLog("function_log", "[formatSchedule] 异常: " + e); }
    return cfg;
}

long getPrevScheduleTime(String cfg, long notAfter, long lastRecorded) {
    if (cfg == null || cfg.equals("")) return 0;
    try {
        String raw = cfg.trim();
        if (raw.equals("")) return 0;
        String mode = "";
        String[] toks = null;
        if (raw.indexOf(":") > 0 && raw.indexOf(" ") < 0) {
            toks = raw.split(":");
            if (toks.length < 2) return 0;
            mode = toks[0].toLowerCase();
        } else {
            String[] parts = raw.split("\\s+");
            if (parts.length >= 4 && parts[0].toLowerCase().startsWith("w")) {
                mode = "w";
                toks = new String[]{"w", parts[0].substring(1), parts[1], parts[2], parts[3]};
            } else if (parts.length >= 4) {
                mode = "m";
                toks = new String[]{"m", parts[0], parts[1], parts[2], parts[3]};
            } else if (parts.length >= 3) {
                mode = "d";
                toks = new String[]{"d", parts[0], parts[1], parts[2]};
            } else if (!raw.contains(" ")) {
                String num = raw.replaceAll("[^0-9]", "");
                if (num.length() >= 4) {
                    String hh = num.substring(0, 2);
                    String mm = num.substring(2, 4);
                    String ss = num.length() >= 6 ? num.substring(4, 6) : "0";
                    mode = "d";
                    toks = new String[]{"d", hh, mm, ss};
                } else return 0;
            } else return 0;
        }
        if (mode.equals("d") && toks.length >= 4) {
            int h = Integer.parseInt(toks[1]);
            int mi = Integer.parseInt(toks[2]);
            int s = Integer.parseInt(toks[3]);
            Calendar target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, h);
            target.set(Calendar.MINUTE, mi);
            target.set(Calendar.SECOND, s);
            target.set(Calendar.MILLISECOND, 0);
            return target.getTimeInMillis();
        }
        if (mode.equals("i") && toks.length >= 4) {
            int h = Integer.parseInt(toks[1]);
            int mi = Integer.parseInt(toks[2]);
            int s = Integer.parseInt(toks[3]);
            long span = h * 3600000L + mi * 60000L + s * 1000L;
            if (span <= 0) return 0;
            if (lastRecorded <= 0) return 0;
            return lastRecorded + span;
        }
        if (toks.length < 5) return 0;
        int day = Integer.parseInt(toks[1]);
        int h = Integer.parseInt(toks[2]);
        int mi = Integer.parseInt(toks[3]);
        int s = Integer.parseInt(toks[4]);
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, h);
        target.set(Calendar.MINUTE, mi);
        target.set(Calendar.SECOND, s);
        target.set(Calendar.MILLISECOND, 0);
        if (mode.equals("w")) {
            if (day < 1 || day > 7) return 0;
            target.set(Calendar.DAY_OF_WEEK, day);
            return target.getTimeInMillis();
        }
        if (mode.equals("m")) {
            if (day < 1 || day > 31) return 0;
            target.set(Calendar.DAY_OF_MONTH, day);
            return target.getTimeInMillis();
        }
        return 0;
    } catch (Throwable e) {
        return 0;
    }
}

void startIndepThread(final String func, final long interval, final int maxCount) {
    Thread existing = (Thread)runningThreads.get(func);
    if (existing != null && existing.isAlive()) {
        existing.interrupt();
        try { existing.join(1000); } catch (Throwable e) { traceLog("function_log", "[startIndepThread] 异常: " + e); }
    }
    
    loopFlags.put(func, new Boolean(getLoop(func)));
    final Object stopToken = new Object();
    stopFlags.put(func, stopToken);
    
    final Runnable task = new Runnable() {
        public void run() {
            traceLog("function_log", "[startIndepThread]" + func);
            int executedCount = 0;
            String[] meta = getMeta(func);
            
            boolean isScheduled = (meta != null && meta[11] != null && !meta[11].equals(""));
            boolean isLooping = getLoop(func);
            boolean runOnce = !isScheduled && !isLooping;
            long lastFireTime = 0L;
            String lfRaw = getString("HotPlug", "lastFireTime_" + func, "");
            if (lfRaw != null && !lfRaw.equals("")) {
                try { lastFireTime = Long.parseLong(lfRaw); } catch (Throwable e) { lastFireTime = 0L; }
            }
            if (lastFireTime <= 0) lastFireTime = System.currentTimeMillis();
            boolean needCatchUp = false;
            if (isScheduled && getBoolean("settings", "补一次执行", false)) {
                long nowTs = System.currentTimeMillis();
                long recorded = 0L;
                String recRaw = getString("HotPlug", "lastFireTime_" + func, "");
                if (recRaw != null && !recRaw.equals("")) {
                    try { recorded = Long.parseLong(recRaw); } catch (Throwable e) { recorded = 0L; }
                }
                long due = getPrevScheduleTime(meta[11], nowTs, recorded);
                
                if (due > 0 && due <= nowTs && recorded < due) {
                    needCatchUp = true;
                    lastFireTime = due;
                    traceLog("function_log", "[补执行] " + func + " 错过应执行点 " + due + "，启动补跑一次");
                }
            }

            while (!Thread.interrupted() && stopFlags.get(func) == stopToken) {
                try {
                    if (!getLoad(func)) {
                        traceLog("function_log", "[停止] " + func + " 总开关关闭");
                        break;
                    }
                    if (!getRun(func) && !runOnce && !needCatchUp) {
                        Thread.sleep(2000);
                        continue;
                    }
                    if (!isScheduled && !isLooping && !runOnce) break;

                    if (!isScheduled && maxCount > 0 && executedCount >= maxCount) {
                        traceLog("function_log", "[完成] " + func + " 次数达标");
                        break;
                    }

                    long scheduledTarget = 0;
                    if (needCatchUp) {
                        needCatchUp = false;
                        scheduledTarget = lastFireTime;
                    } else if (isScheduled) {
                        scheduledTarget = getNextScheduleTime(meta[11], lastFireTime);

                        if (scheduledTarget <= 0) {
                            traceLog("function_log", "[计划] " + func + " 定时配置无效，立即执行一次");
                            runOnce = true;
                        } else {
                            long waitTime = scheduledTarget - System.currentTimeMillis();
                            if (waitTime > 0) {
                                traceLog("function_log", "[计划] " + func + " 等待: " + (waitTime/1000) + "秒");
                                long deadline = scheduledTarget;
                                while (!Thread.interrupted()) {
                                    long remain = deadline - System.currentTimeMillis();
                                    if (remain <= 0) break;
                                    Thread.sleep(remain > 500 ? 500 : remain);
                                    if (stopFlags.get(func) != stopToken) break;
                                }
                            }
                        }

                    } else if (isLooping) {
                        long sleepTime = interval > 0 ? interval : 5000;
                        if (sleepTime < 500) sleepTime = 500; 
                        if (executedCount > 0) Thread.sleep(sleepTime); 
                        
                        if (!getLoad(func) || !getRun(func) || !getLoop(func)) continue;
                    }
                    
                    String code = readCodeFile(func);
                    if (!code.equals("")) {
                        synchronized (this.interpreter) {
                        this.interpreter.set("qq", myUin);
                        this.interpreter.set("pluginPath", pluginPath);
                        this.interpreter.set("this", this);
                        this.interpreter.set("qun", ""); 
                        this.interpreter.set("uin", "");
                        
                        scriptLoader.loadAndExecute(func, this.interpreter);
                        executedCount++;
                        lastExecTime.put(func, System.currentTimeMillis());
                        if (isScheduled) putString("HotPlug", "lastFireTime_" + func, String.valueOf(System.currentTimeMillis()));

                        traceLog("function_log", "[执行] " + func + " 第" + executedCount + "次");
                        }
                    }

                    if (isScheduled && scheduledTarget > 0) {
                        lastFireTime = scheduledTarget;
                        putString("HotPlug", "lastFireTime_" + func, String.valueOf(scheduledTarget));
                    }
                    
                    if (runOnce) break; 
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    traceLog("function_log", "[错误] " + func + ": " + e);
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
                }
            }
            traceLog("function_log", "[结束] " + func);
            if (stopFlags.get(func) == stopToken) {
                stopFlags.remove(func);
                runningThreads.remove(func);
            }
        }
    };
    
    ThreadPool.execute(task);
    Thread wrapper = new Thread(task);
    wrapper.setName("HotPlug_" + func);
    runningThreads.put(func, wrapper);
}

void stopThread(String func) {
    stopFlags.remove(func);
    Thread t = (Thread)runningThreads.get(func);
    if (t != null) { 
        t.interrupt(); 
        try { t.join(1000); } catch (Throwable e) { traceLog("function_log", "[stopThread] 异常: " + e); }
        runningThreads.remove(func); 
    }
}

void stopAllThreads() {
    List stopFuncs = new ArrayList(runningThreads.keySet());
    for (int i = 0; i < stopFuncs.size(); i++) {
        String f = (String)stopFuncs.get(i);
        stopFlags.remove(f);
        Thread t = (Thread)runningThreads.get(f);
        if (t != null) {
            t.interrupt();
            try { t.join(1000); } catch (Throwable e) { traceLog("function_log", "[stopAllThreads] 异常: " + e); }
        }
    }
    runningThreads.clear();
}

void execFunc(String func, Object data, int type) {
    try {
        String[] m = getMeta(func);
        if (m == null) return;
        
        int idx = type + 1;
        if (idx >= m.length || !m[idx].equals("1")) return;
        if (!getRun(func)) return;
        
        String currentGroupId = "";
        if (m[9].equals("1")) { 
            String gid = "";
            if (type == 1 && data != null) {
                try { gid = data.peerUin; } catch (Throwable e) { traceLog("function_log", "[execFunc] 异常: " + e); }
            } else if (type >= 2 && type <= 6) {
                try {
                    if (data instanceof String) gid = (String)data;
                    else if (data instanceof Object[]) gid = (String)((Object[])data)[0];
                    else if (data instanceof String[]) gid = ((String[])data)[0];
                } catch (Throwable e) { traceLog("function_log", "[execFunc] 异常: " + e); }
            }
            if (gid.equals("") || !getGrp(func, gid)) return;
            currentGroupId = gid;
        }
        
        File codeFile = new File(getDir() + "/" + func + ".java");
        if (!codeFile.exists() || codeFile.length() == 0) return;
        synchronized (this.interpreter) {
            this.interpreter.unset("msg");
            this.interpreter.unset("time");
            this.interpreter.unset("operator");

        if (type == 1) { 
            this.interpreter.set("data", data);
            this.interpreter.set("qun", data.peerUin);
            this.interpreter.set("uin", data.userUin);
            this.interpreter.set("msg", data.msg);
            try { this.interpreter.set("msgId", data.msgId); } catch (Throwable e) { this.interpreter.set("msgId", 0); }
            try { this.interpreter.set("msgType", data.msgType); } catch (Throwable e) { this.interpreter.set("msgType", 0); }
            try { this.interpreter.set("type", data.type); } catch (Throwable e) { this.interpreter.set("type", 2); }
            this.interpreter.set("qq", myUin);
        } else if (type == 2) { 
            String[] arr = (String[])data;
            this.interpreter.set("qun", arr[0]);
            this.interpreter.set("uin", arr[1]);
            this.interpreter.set("data", data);
            this.interpreter.set("qq", myUin);
        } else if (type == 3) { 
            String[] arr = (String[])data;
            this.interpreter.set("qun", arr[0]);
            this.interpreter.set("uin", arr[1]);
            this.interpreter.set("data", data);
            this.interpreter.set("qq", myUin);
        } else if (type == 4) { 
            Object[] arr = (Object[])data;
            this.interpreter.set("qun", arr[0]);
            this.interpreter.set("uin", arr[1]);
            this.interpreter.set("time", arr[2]);
            this.interpreter.set("operator", arr[3]);
            this.interpreter.set("data", data);
            this.interpreter.set("qq", myUin);
        } else if (type == 5) { 
            this.interpreter.set("qun", data);
            this.interpreter.set("uin", myUin);
            this.interpreter.set("data", data);
            this.interpreter.set("qq", myUin);
        } else if (type == 6) { 
            Object[] arr = (Object[])data;
            this.interpreter.set("qun", arr[0]);
            this.interpreter.set("uin", "");
            this.interpreter.set("paiType", arr[1]);
            this.interpreter.set("operator", arr[2]);
            this.interpreter.set("data", data);
            this.interpreter.set("qq", myUin);
        }
        scriptLoader.loadAndExecute(func, this.interpreter);
        }
        lastExecTime.put(func, System.currentTimeMillis());
    } catch (Throwable e) {
        traceLog("function_log", "[execFunc]" + func + ":" + e);
    }
}

void dispatchEvent(Object data, int type) {
    Integer key = new Integer(type);
    if (!eventRegistry.containsKey(key)) return;
    ArrayList list = (ArrayList) eventRegistry.get(key);
    if (list == null || list.size() == 0) return;
    for (int i = 0; i < list.size(); i++) {
        execFunc((String)list.get(i), data, type);
    }
}

String[][] getPresetsCategory1() {
    String[][] arr = new String[9][2];
            arr[0][0] = "发文本";
            arr[0][1] = "sendMsg(qun, \"[atUin=\"+uin+\"]内容\", type);";
            arr[1][0] = "发图片";
            arr[1][1] = "sendPic(qun, pluginPath+\"/test.jpg\", 2);";
            arr[2][0] = "发语音";
            arr[2][1] = "sendPtt(qun, pluginPath+\"/test.amr\", 2);";
            arr[3][0] = "发卡片";
            arr[3][1] = "sendCard(qun, \"{\\\"app\\\":\\\"miniapp\\\"}\", 2);";
            arr[4][0] = "发文件";
            arr[4][1] = "sendFile(qun, pluginPath+\"/file.txt\", 2);";
            arr[5][0] = "发视频";
            arr[5][1] = "sendVideo(qun, pluginPath+\"/video.mp4\", 2);";
            arr[6][0] = "引用回复";
            arr[6][1] = "sendReplyMsg(qun, msgId, \"回复\", type);";
            arr[7][0] = "撤回消息";
            arr[7][1] = "recallMsg(type, qun, msgId);";
            arr[8][0] = "拍一拍";
            arr[8][1] = "sendPai(uin, qun, type);";
    return arr;
}

String[][] getPresetsCategory2() {
    String[][] arr = new String[5][2];
            arr[0][0] = "获取好友";
            arr[0][1] = "List list = getAllFriend();";
            arr[1][0] = "是否好友";
            arr[1][1] = "boolean flag = isFriend(uin);";
            arr[2][0] = "点赞";
            arr[2][1] = "sendZan(uin, 10);";
            arr[3][0] = "Uin转Uid";
            arr[3][1] = "String uid = getUidFromUin(uin);";
            arr[4][0] = "Uid转Uin";
            arr[4][1] = "String uin2 = getUinFromUid(uid);";
    return arr;
}

String[][] getPresetsCategory3() {
    String[][] arr = new String[13][2];
            arr[0][0] = "群列表";
            arr[0][1] = "List groups = getGroupList();";
            arr[1][0] = "成员列表";
            arr[1][1] = "List members = getGroupMemberList(qun);";
            arr[2][0] = "禁言列表";
            arr[2][1] = "List forbids = getProhibitList(qun);";
            arr[3][0] = "群信息";
            arr[3][1] = "TroopInfo info = getGroupInfo(qun);";
            arr[4][0] = "成员信息";
            arr[4][1] = "MemberInfo minfo = getMemberInfo(qun, uin);";
            arr[5][0] = "禁言";
            arr[5][1] = "shutUp(qun, uin, 600);";
            arr[6][0] = "全员禁言";
            arr[6][1] = "shutUpAll(qun, true);";
            arr[7][0] = "踢人";
            arr[7][1] = "kickGroup(qun, uin, false);";
            arr[8][0] = "设管理";
            arr[8][1] = "setGroupAdmin(qun, uin, true);";
            arr[9][0] = "改头衔";
            arr[9][1] = "setGroupMemberTitle(qun, uin, \"头衔\");";
            arr[10][0] = "改名片";
            arr[10][1] = "changeMemberName(qun, uin, \"名片\");";
            arr[11][0] = "查是否禁言";
            arr[11][1] = "boolean shut = isShutUp(qun);";
            arr[12][0] = "群打卡";
            arr[12][1] = "clockIn(qun);";
    return arr;
}

void animateDialogIn(final android.app.Dialog d) {
    try {
        View v = d.getWindow().getDecorView();
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(v, "alpha", new float[]{0f, 1f});
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(v, "scaleX", new float[]{0.92f, 1f});
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(v, "scaleY", new float[]{0.92f, 1f});
        AnimatorSet animatorSet = new AnimatorSet();
        android.animation.Animator[] animArr = new android.animation.Animator[3];
        animArr[0] = alphaAnim;
        animArr[1] = scaleXAnim;
        animArr[2] = scaleYAnim;
        animatorSet.playTogether(animArr);
        animatorSet.setDuration(250);
        animatorSet.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animatorSet.start();
    } catch (Throwable e) { traceLog("function_log", "[animateDialogIn] 异常: " + e); }
}

void animateDialogOut(final android.app.Dialog d, final Runnable onEnd) {
    try {
        Window w = d.getWindow();
        View v = w.getDecorView();
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(v, "alpha", new float[]{1f, 0f});
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(v, "scaleX", new float[]{1f, 0.95f});
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(v, "scaleY", new float[]{1f, 0.95f});
        AnimatorSet animatorSet = new AnimatorSet();
        android.animation.Animator[] animArr = new android.animation.Animator[3];
        animArr[0] = alphaAnim;
        animArr[1] = scaleXAnim;
        animArr[2] = scaleYAnim;
        animatorSet.playTogether(animArr);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new android.view.animation.AccelerateInterpolator());
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            public void onAnimationEnd(android.animation.Animator animation) {
                d.dismiss();
                if (onEnd != null) onEnd.run();
            }
        });
        animatorSet.start();
    } catch (Throwable e) {
        d.dismiss();
        if (onEnd != null) onEnd.run();
    }
}

EditText addNameInput(Activity a, LinearLayout parent, String name) {
    EditText et = makeInput(a, "功能名", null);
    if (name != null) et.setText(name);
    parent.addView(et);
    return et;
}

EditText addCodeInput(Activity a, LinearLayout parent, String code, boolean isFile, final boolean[] isFileState) {
    LinearLayout rowFile = new LinearLayout(a);
    rowFile.setOrientation(LinearLayout.HORIZONTAL);
    rowFile.setGravity(Gravity.CENTER_VERTICAL);
    rowFile.setPadding(0, dp(a, 6), 0, dp(a, 2));
    parent.addView(rowFile);

    final TextView chipFile = makeChip(a, "文件路径", isFileState[0], 0);
    rowFile.addView(chipFile);

    TextView tipFile = new TextView(a);
    tipFile.setText("/q/f/x.java   勾选后在输入框中输入文件路径,要确保路径正确");
    tipFile.setTextSize(9);
    tipFile.setTextColor(tc(a, "on_surface_variant"));
    tipFile.setPadding(dp(a, 6), 0, 0, 0);
    rowFile.addView(tipFile);

    final EditText et = makeInput(a, isFile ? "文件绝对路径" : "代码内容", null);
    if (code != null) et.setText(code);
    if (!isFile) {
        et.setMinLines(4);
        et.setGravity(Gravity.TOP);
    }
    LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(-1, -2);
    lpEt.setMargins(0, dp(a, 6), 0, 0);
    et.setLayoutParams(lpEt);
    parent.addView(et);

    chipFile.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            isFileState[0] = !isFileState[0];
            setChip(chipFile, isFileState[0]);
            et.setHint(isFileState[0] ? "文件绝对路径" : "代码内容");
        }
    });
    
    return et;
}

void addPresetRows(Activity a, LinearLayout parent, final EditText et) {
    TextView pt = new TextView(a);
    pt.setText("快捷填入:");
    pt.setTextSize(10);
    pt.setTextColor(tc(a, "on_surface_variant"));
    pt.setPadding(0, dp(a, 4), 0, 0);
    parent.addView(pt);
    
    addPresetRow(a, parent, et, getPresetsCategory1(), pc("#3B71FE"));
    addPresetRow(a, parent, et, getPresetsCategory2(), pc("#00C853"));
    addPresetRow(a, parent, et, getPresetsCategory3(), pc("#FF9800"));
}

String getCallbackTip(int index) {
    switch(index) {
        case 0: return "【消息】收到消息时触发\n参数: qun(群号), uin(发送者), msg(消息内容), msgId(消息ID), msgType(消息类型), type(聊天类型), data(Object对象)";
        case 1: return "【入群】有人加入群聊时触发\n参数: qun(群号), uin(入群者QQ)";
        case 2: return "【退群】有人退出群聊时触发\n参数: qun(群号), uin(退群者QQ)";
        case 3: return "【禁言】群成员被禁言时触发\n参数: qun(群号), uin(被禁言者), time(禁言秒数), operator(操作者QQ)";
        case 4: return "【切换聊天】切换聊天窗口时触发\n参数: qun/uin(当前聊天对象Uin)";
        case 5: return "【拍一拍】收到拍一拍时触发\n参数: qun(群号/私聊对象), paiType(拍一拍类型), operator(发送者QQ)";
        case 6: return "【预处理】发送消息前处理消息内容\n参数: qun(目标), type(聊天类型), msg(消息内容)\n可用: 逐字/样式/小尾巴/重复发送";
        case 7: return "【群开关】为每个群单独控制功能开关\n需配合其他回调使用，开启后可在列表项中控制";
        default: return "";
    }
}

void updateDynamicTips(FormComponents fc, boolean[] cks) {
    if (fc.dynamicTips == null) return;
    
    StringBuilder sb = new StringBuilder();
    boolean hasAny = false;
    
    for (int i = 0; i < 8; i++) {
        if (cks[i]) {
            if (hasAny) sb.append("\n\n");
            sb.append(getCallbackTip(i));
            hasAny = true;
        }
    }
    
    if (!hasAny) {
        sb.append("当前不在任何回调中，代码将在独立线程中运行\n若未设置循环或定时，保存并开启后将【立即执行一次】（适合单次功能）\n可用参数: qq(当前登录QQ), pluginPath(插件路径), this(当前对象) 以及Java中全部变量和方法");
    }
    
    fc.dynamicTips.setText(sb.toString());
}

TextView[] addChipRows(Activity a, LinearLayout parent, final boolean[] cks, final FormComponents fc) {
    TextView tipsHeader = new TextView(a);
    tipsHeader.setText("回调详情:");
    tipsHeader.setTextSize(11);
    tipsHeader.setTextColor(tc(a, "on_surface_variant"));
    tipsHeader.setPadding(0, dp(a, 10), 0, dp(a, 6));
    parent.addView(tipsHeader);
    
    fc.dynamicTips = new TextView(a);
    fc.dynamicTips.setTextSize(10);
    fc.dynamicTips.setTextColor(pc("#3B71FE"));
    fc.dynamicTips.setPadding(dp(a, 8), dp(a, 8), dp(a, 8), dp(a, 8));
    fc.dynamicTips.setBackground(roundRect(pc("#F0F5FF"), dp(a, 6)));
    fc.dynamicTips.setLineSpacing(dp(a, 2), 1.0f);
    parent.addView(fc.dynamicTips);
    
    updateDynamicTips(fc, cks);
    
    TextView sub = new TextView(a);
    sub.setText("挂载回调(多选):");
    sub.setTextSize(11);
    sub.setTextColor(tc(a, "on_surface_variant"));
    sub.setPadding(0, dp(a, 10), 0, dp(a, 6));
    parent.addView(sub);
    
    String[] lbls = {"消息", "入群", "退群", "禁言", "切换聊天", "拍一拍", "预处理", "群开关"};
    final TextView[] chips = new TextView[8];
    
    LinearLayout r1 = new LinearLayout(a);
    r1.setOrientation(LinearLayout.HORIZONTAL);
    parent.addView(r1);
    
    for (int i = 0; i < 4; i++) {
        chips[i] = makeChip(a, lbls[i], cks[i], 0);
        final int x = i;
        chips[i].setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cks[x] = !cks[x];
                setChip(chips[x], cks[x]);
                if (x == 6 && fc != null) {
                    updatePreprocVisibility(fc, cks[6]);
                }
                updateDynamicTips(fc, cks);
            }
        });
        r1.addView(chips[i]);
        if (i < 3) {
            LinearLayout.LayoutParams pm = (LinearLayout.LayoutParams)chips[i].getLayoutParams();
            pm.setMargins(0, 0, dp(a, 4), 0);
            chips[i].setLayoutParams(pm);
        }
    }
    
    LinearLayout r2 = new LinearLayout(a);
    r2.setOrientation(LinearLayout.HORIZONTAL);
    r2.setPadding(0, dp(a, 6), 0, 0);
    parent.addView(r2);
    
    for (int i = 4; i < 8; i++) {
        chips[i] = makeChip(a, lbls[i], cks[i], i == 7 ? 0 : 0);
        final int x = i;
        chips[i].setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cks[x] = !cks[x];
                setChip(chips[x], cks[x]);
                if (x == 6 && fc != null) {
                    updatePreprocVisibility(fc, cks[6]);
                }
                updateDynamicTips(fc, cks);
            }
        });
        r2.addView(chips[i]);
        if (i < 7) {
            LinearLayout.LayoutParams pm = (LinearLayout.LayoutParams)chips[i].getLayoutParams();
            pm.setMargins(0, 0, dp(a, 4), 0);
            chips[i].setLayoutParams(pm);
        }
    }
    return chips;
}

void updatePreprocVisibility(FormComponents fc, boolean visible) {
    if (fc.preprocContainer != null) {
        fc.preprocContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            fc.preprocContainer.setAlpha(0f);
            fc.preprocContainer.animate().alpha(1f).setDuration(300).start();
        }
    }
}

String getStyleTip(int type) {
    switch (type) {
        case 0: return "";
        case 1: return "逐字发送：消息将被逐个字符/表情拆开发送，产生打字机效果";
        case 2: return "下划线：每个字后加下沉下划线，末尾补齐，经典风格";
        case 3: return "删除线：贯穿文字的删除线，末尾补齐";
        case 4: return "斜体：文字带斜向下划装饰，末尾补齐";
        case 5: return "粗体：文字加粗风格下划，末尾补齐";
        case 6: return "上标：文字上浮装饰，适合花样字效果";
        case 7: return "下标：文字下沉装饰，适合花样字效果";
        case 8: return "反转：完整反转文字顺序，不使用组合字符";
        case 9: return "双字：每个字符/表情重复两次，不使用组合字符";
        case 10: return "下划꯭：使用特殊字符꯭，视觉冲击力强，末尾补齐";
        case 11: return "细下划线：经典细线贯穿文字，末尾补齐";
        case 12: return "双下划线：两条平行下划线，粗壮明显";
        case 13: return "波浪下划：波浪形下划线，活泼感";
        case 14: return "长下划线：加长下划，覆盖更完整";
        case 15: return "下点下划：下划线带点装饰";
        case 16: return "底圈：文字底部带圈装饰";
        case 17: return "粗删除线：加粗删除线，更醒目";
        case 18: return "斜删除线：倾斜删除线，动感";
        case 19: return "双斜删除：两条斜删除线，强烈效果";
        case 20: return "圆圈包围：文字被圆圈包围";
        case 21: return "大圆圈：更大一圈的圆形包围";
        case 22: return "小方框：方框包围文字";
        case 23: return "三角包围：三角形包围文字";
        case 24: return "禁止符号：带禁止圈的装饰，常用于警告风格";
        case 25: return "左箭头：文字左侧带箭头装饰";
        case 26: return "右箭头：文字右侧带箭头装饰";
        case 27: return "星号包围：文字被星号包围";
        case 28: return "三横包围：三条横线包围";
        case 29: return "波浪等号：波浪形等号装饰";
        case 30: return "包围上升箭：上升箭头包围";
        case 31: return "包围下降箭：下降箭头包围";
        case 32: return "包围四点：四点装饰包围";
        case 33: return "包围五点：五点装饰包围";
        case 34: return "短斜杠：短斜杠贯穿";
        case 35: return "长斜杠：长斜杠贯穿";
        case 36: return "左斜杠：向左倾斜斜杠";
        case 37: return "右斜杠：向右倾斜斜杠";
        case 38: return "组合箭头：箭头组合装饰";
        case 39: return "包围双箭：双箭头包围";
        case 40: return "包围波浪2：另一种波浪包围";
        case 41: return "包围倒V：倒V形包围";
        case 42: return "包围长下划：长下划包围";
        case 43: return "包围三点：三点装饰包围";
        case 44: return "包围等号：等号包围";
        case 45: return "包围双斜杠：双斜杠包围";
        case 46: return "包围短删除：短删除线包围";
        case 47: return "包围粗下划：粗下划包围";
        case 48: return "包围星号2：另一种星号包围";
        case 49: return "包围心形：心形包围（浪漫风格）";
        default: return "选中样式后自动应用装饰效果，末尾补齐增强连贯性";
    }
}

void addPreprocRow(Activity a, LinearLayout parent, FormComponents fc, int preType, String preTail, int repeatSend, int repeatConcat) {
    final LinearLayout container = new LinearLayout(a);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(0, dp(a, 8), 0, 0);
    container.setVisibility(preType > 0 ? View.VISIBLE : View.GONE);
    parent.addView(container);
    fc.preprocContainer = container;
    
    TextView styleTitle = new TextView(a);
    styleTitle.setText("选择样式效果:");
    styleTitle.setTextSize(11);
    styleTitle.setTextColor(tc(a, "on_surface_variant"));
    styleTitle.setPadding(0, 0, 0, dp(a, 6));
    container.addView(styleTitle);
    
    HorizontalScrollView hs = new HorizontalScrollView(a);
    hs.setHorizontalScrollBarEnabled(false);
    hs.setPadding(0, 0, 0, dp(a, 8));
    container.addView(hs);
    
    LinearLayout typeRow = new LinearLayout(a);
    typeRow.setOrientation(LinearLayout.HORIZONTAL);
    hs.addView(typeRow);
    
    String[] types = {
        "无", "逐字", "下划线", "删除线", "斜体", "粗体", "上标(花样字)", "下标(花样字)",
        "反转", "双字", "压抑下划꯭", "细下划线", "双下划线", "波浪下划", "长下划线", "下点下划", "底圈",
        "粗删除线", "斜删除线", "双斜删除", "圆圈包围", "大圆圈", "小方框", "三角包围", "禁止符号",
        "左箭头", "右箭头", "星号包围", "三横包围", "波浪等号", "包围上升箭", "包围下降箭", "包围四点",
        "包围五点", "短斜杠", "长斜杠", "左斜杠", "右斜杠", "组合箭头", "包围双箭", "包围波浪2",
        "包围倒V", "包围长下划", "包围三点", "包围等号", "包围双斜杠", "包围短删除", "包围粗下划", "包围星号2",
        "包围心形"
    };
    
    fc.preTypeChips = new TextView[types.length];
    final int[] currentType = {preType >= 0 && preType < types.length ? preType : 0};
    
    for (int i = 0; i < types.length; i++) {
        int typeVal = i;
        boolean isSelected = (typeVal == currentType[0]);
        fc.preTypeChips[i] = makeChip(a, types[i], isSelected, 2);
        final int idx = i;
        fc.preTypeChips[i].setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentType[0] = idx;
                for (int j = 0; j < types.length; j++) {
                    setChipWithType(fc.preTypeChips[j], j == idx, 2);
                }
                fc.chipPreType.setTag(currentType[0]);
                
                if (idx == 0) {
                    fc.prePreview.setVisibility(View.GONE);
                } else {
                    fc.prePreview.setVisibility(View.VISIBLE);
                    String previewText = applyPreprocess("预览", idx);
                    String tip = "预览：" + previewText + "\n" + getStyleTip(idx);
                    fc.prePreview.setText(tip);
                }
            }
        });
        typeRow.addView(fc.preTypeChips[i]);
        if (i < types.length - 1) {
            LinearLayout.LayoutParams pm = (LinearLayout.LayoutParams)fc.preTypeChips[i].getLayoutParams();
            pm.setMargins(0, 0, dp(a, 4), 0);
            fc.preTypeChips[i].setLayoutParams(pm);
        }
    }
    
    fc.chipPreType = new TextView(a);
    fc.chipPreType.setTag(currentType[0]);
    fc.chipPreType.setTextColor(tc(a, "on_surface"));
    
    fc.prePreview = new TextView(a);
    fc.prePreview.setTextSize(10);
    fc.prePreview.setTextColor(pc("#3B71FE"));
    fc.prePreview.setPadding(dp(a, 8), dp(a, 8), dp(a, 8), dp(a, 8));
    fc.prePreview.setBackground(roundRect(pc("#F0F5FF"), dp(a, 6)));
    fc.prePreview.setLineSpacing(dp(a, 2), 1.0f);
    fc.prePreview.setVisibility(currentType[0] == 0 ? View.GONE : View.VISIBLE);
    if (currentType[0] > 0) {
        String previewText = applyPreprocess("预览", currentType[0]);
        String tip = "预览：" + previewText + "\n" + getStyleTip(currentType[0]);
        fc.prePreview.setText(tip);
    }
    container.addView(fc.prePreview);
    
    TextView detailedTips = new TextView(a);
    detailedTips.setText("提示：选中样式后，每个字符后会自动添加对应装饰");
    detailedTips.setTextSize(9);
    detailedTips.setTextColor(tc(a, "on_surface_variant"));
    detailedTips.setPadding(dp(a, 4), dp(a, 4), dp(a, 4), dp(a, 8));
    detailedTips.setLineSpacing(dp(a, 2), 1.0f);
    container.addView(detailedTips);
    
    LinearLayout titleRow = new LinearLayout(a);
    titleRow.setOrientation(LinearLayout.HORIZONTAL);
    titleRow.setGravity(Gravity.CENTER_VERTICAL);
    container.addView(titleRow);
    
    TextView tailTitle = new TextView(a);
    tailTitle.setText("小尾巴(前缀 msg 后缀)");
    tailTitle.setTextSize(11);
    tailTitle.setTextColor(tc(a, "on_surface_variant"));
    tailTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2.0f));
    titleRow.addView(tailTitle);
    
    TextView sendTitle = new TextView(a);
    sendTitle.setText("连发");
    sendTitle.setTextSize(11);
    sendTitle.setTextColor(tc(a, "on_surface_variant"));
    sendTitle.setGravity(Gravity.CENTER);
    sendTitle.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 50), -2));
    titleRow.addView(sendTitle);
    
    View spacer = new View(a);
    spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 8), 0));
    titleRow.addView(spacer);
    
    TextView concatTitle = new TextView(a);
    concatTitle.setText("拼接");
    concatTitle.setTextSize(11);
    concatTitle.setTextColor(tc(a, "on_surface_variant"));
    concatTitle.setGravity(Gravity.CENTER);
    concatTitle.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 50), -2));
    titleRow.addView(concatTitle);
    
    LinearLayout inputRow = new LinearLayout(a);
    inputRow.setOrientation(LinearLayout.HORIZONTAL);
    inputRow.setGravity(Gravity.CENTER_VERTICAL);
    inputRow.setPadding(0, dp(a, 4), 0, 0);
    container.addView(inputRow);
    
    fc.etPreTail = makeInput(a, "例如: 前缀 msg 后缀", null);
    if (preTail != null) fc.etPreTail.setText(preTail);
    fc.etPreTail.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2.0f));
    inputRow.addView(fc.etPreTail);
    
    fc.etRepeatSend = makeInput(a, "次", null);
    fc.etRepeatSend.setTextSize(11);
    fc.etRepeatSend.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    fc.etRepeatSend.setGravity(Gravity.CENTER);
    fc.etRepeatSend.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 45), dp(a, 32)));
    if (repeatSend > 0) fc.etRepeatSend.setText(String.valueOf(repeatSend));
    LinearLayout.LayoutParams lpRs = new LinearLayout.LayoutParams(dp(a, 50), dp(a, 32));
    lpRs.setMargins(dp(a, 8), 0, 0, 0);
    fc.etRepeatSend.setLayoutParams(lpRs);
    inputRow.addView(fc.etRepeatSend);
    
    fc.etRepeatConcat = makeInput(a, "次", null);
    fc.etRepeatConcat.setTextSize(11);
    fc.etRepeatConcat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    fc.etRepeatConcat.setGravity(Gravity.CENTER);
    fc.etRepeatConcat.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 45), dp(a, 32)));
    if (repeatConcat > 0) fc.etRepeatConcat.setText(String.valueOf(repeatConcat));
    LinearLayout.LayoutParams lpRc = new LinearLayout.LayoutParams(dp(a, 50), dp(a, 32));
    lpRc.setMargins(dp(a, 8), 0, 0, 0);
    fc.etRepeatConcat.setLayoutParams(lpRc);
    inputRow.addView(fc.etRepeatConcat);
    
    TextView inputTips = new TextView(a);
    inputTips.setText("提示: msg会被替换为实际消息内容\n连发: 重复发送N次  拼接: 内容重复N次后再发");
    inputTips.setTextSize(9);
    inputTips.setTextColor(pc("#AAAAAA"));
    inputTips.setPadding(0, dp(a, 4), 0, 0);
    inputTips.setLineSpacing(dp(a, 2), 1.0f);
    container.addView(inputTips);
}

FormComponents addLoopRow(Activity a, LinearLayout parent, boolean isLoop, long interval, int count, final boolean[] cks) {
    final FormComponents fc = new FormComponents();
    
    final LinearLayout loopContainer = new LinearLayout(a);
    loopContainer.setOrientation(LinearLayout.VERTICAL);
    loopContainer.setPadding(0, dp(a, 8), 0, 0);
    parent.addView(loopContainer);
    
    final LinearLayout loopHeader = new LinearLayout(a);
    loopHeader.setOrientation(LinearLayout.HORIZONTAL);
    loopHeader.setGravity(Gravity.CENTER_VERTICAL);
    loopContainer.addView(loopHeader);
    
    final boolean[] loopCheck = {isLoop};
    fc.chipLoop = makeChip(a, "循环执行", isLoop, 0);
    loopHeader.addView(fc.chipLoop);
    
    fc.loopSettings = new LinearLayout(a);
    fc.loopSettings.setOrientation(LinearLayout.HORIZONTAL);
    fc.loopSettings.setPadding(0, dp(a, 8), 0, 0);
    fc.loopSettings.setVisibility(isLoop ? View.VISIBLE : View.GONE);
    loopContainer.addView(fc.loopSettings);
    
    LinearLayout leftCol = new LinearLayout(a);
    leftCol.setOrientation(LinearLayout.VERTICAL);
    leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    fc.loopSettings.addView(leftCol);
    
    TextView lblInterval = new TextView(a);
    lblInterval.setText("间隔(毫秒)");
    lblInterval.setTextSize(10);
    lblInterval.setTextColor(tc(a, "on_surface_variant"));
    leftCol.addView(lblInterval);
    
    fc.etInterval = makeInput(a, "5000", null);
    if (interval > 0) fc.etInterval.setText(String.valueOf(interval));
    leftCol.addView(fc.etInterval);
    
    View spacer = new View(a);
    spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 8), 0));
    fc.loopSettings.addView(spacer);
    
    LinearLayout rightCol = new LinearLayout(a);
    rightCol.setOrientation(LinearLayout.VERTICAL);
    rightCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    fc.loopSettings.addView(rightCol);
    
    TextView lblCount = new TextView(a);
    lblCount.setText("次数(0=无限)");
    lblCount.setTextSize(10);
    lblCount.setTextColor(tc(a, "on_surface_variant"));
    rightCol.addView(lblCount);
    
    fc.etCount = makeInput(a, "0", null);
    fc.etCount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    if (count > 0) fc.etCount.setText(String.valueOf(count));
    rightCol.addView(fc.etCount);
    
    fc.chipLoop.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            loopCheck[0] = !loopCheck[0];
            setChip(fc.chipLoop, loopCheck[0]);
            for (int i = 1; i < loopContainer.getChildCount(); i++) {
                loopContainer.getChildAt(i).setVisibility(loopCheck[0] ? View.VISIBLE : View.GONE);
            }
        }
    });
    
    return fc;
}

EditText addTimeRow(Activity a, LinearLayout parent, String timeVal) {
    TextView lblTime = new TextView(a);
    lblTime.setText("定时(可选):");
    lblTime.setTextSize(11);
    lblTime.setTextColor(tc(a, "on_surface_variant"));
    lblTime.setPadding(0, dp(a, 10), 0, dp(a, 4));
    parent.addView(lblTime);
    
    LinearLayout timeRow = new LinearLayout(a);
    timeRow.setOrientation(LinearLayout.HORIZONTAL);
    parent.addView(timeRow);
    
    final EditText etTime = makeInput(a, "时 分 秒 或 w1 时 分 秒 或 1 时 分 秒", null);
    if (timeVal != null) etTime.setText(timeVal);
    etTime.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    timeRow.addView(etTime);
    
    TextView btnSchedule = createButton(a, "📅", pc("#333333"), pc("#E8F0FE"), 16f, 6, 12, 0, false, 0, 0, null);
    btnSchedule.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { showSchedulePicker(a, etTime); }
    });
    timeRow.addView(btnSchedule);
    
    TextView btnTime = createButton(a, "⏱", pc("#333333"), pc("#E8F0FE"), 16f, 6, 12, 0, false, 0, 0, null);
    btnTime.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { showTimePicker(a, etTime, null); }
    });
    timeRow.addView(btnTime);

    LinearLayout catchRow = new LinearLayout(a);
    catchRow.setOrientation(LinearLayout.HORIZONTAL);
    catchRow.setGravity(Gravity.CENTER_VERTICAL);
    catchRow.setPadding(0, dp(a, 6), 0, 0);
    parent.addView(catchRow);
    TextView catchLbl = new TextView(a);
    catchLbl.setText("过点补执行");
    catchLbl.setTextSize(11);
    catchLbl.setTextColor(tc(a, "on_surface_variant"));
    catchLbl.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    catchRow.addView(catchLbl);
    final boolean[] catchOn = {getBoolean("settings", "补一次执行", false)};
    final TextView catchSw = makeSwitch(a, catchOn[0], tc(a, "primary"));
    catchSw.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            catchOn[0] = !catchOn[0];
            putBoolean("settings", "补一次执行", catchOn[0]);
            setSwitch(catchSw, catchOn[0], tc(a, "primary"));
            toast(catchOn[0] ? "过点补执行已开" : "过点补执行已关");
        }
    });
    catchRow.addView(catchSw);

    return etTime;
}

void addPresetRow(Activity a, LinearLayout parent, final EditText et, String[][] presets, int color) {
    HorizontalScrollView hs = new HorizontalScrollView(a);
    hs.setHorizontalScrollBarEnabled(false);
    hs.setPadding(0, dp(a, 4), 0, dp(a, 4));
    LinearLayout row = new LinearLayout(a);
    row.setOrientation(LinearLayout.HORIZONTAL);
    hs.addView(row);
    parent.addView(hs);
    
    for (int i = 0; i < presets.length; i++) {
        TextView b = makePresetChip(a, presets[i][0], color);
        final String code = presets[i][1];
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int start = et.getSelectionStart();
                if (start < 0) start = 0;
                et.getText().insert(start, code);
            }
        });
        row.addView(b);
        if (i < presets.length - 1) {
            LinearLayout.LayoutParams pm = (LinearLayout.LayoutParams)b.getLayoutParams();
            pm.setMargins(0, 0, dp(a, 6), 0);
            b.setLayoutParams(pm);
        }
    }
}

void showSchedulePicker(Activity a, final EditText target) {
    a.runOnUiThread(new Runnable() {
        public void run() {
            try {

                final android.app.Dialog d = new android.app.Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
                d.requestWindowFeature(1);
                d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                Window window = d.getWindow();
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
                
                FrameLayout outer = new FrameLayout(a);
                outer.setPadding(dp(a, 24), dp(a, 40), dp(a, 24), dp(a, 24));
                LinearLayout card = new LinearLayout(a);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(roundRect(tc(a, "surface"), dp(a, 16)));
                card.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 20));
                outer.addView(card);
                
                TextView title = new TextView(a);
                title.setText("日程配置");
                title.setTextSize(18);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(tc(a, "on_surface"));
                card.addView(title);
                
                TextView subtitle = new TextView(a);
                subtitle.setText("配置定时任务的执行时间");
                subtitle.setTextSize(12);
                subtitle.setTextColor(tc(a, "on_surface_variant"));
                subtitle.setPadding(0, dp(a, 4), 0, dp(a, 12));
                card.addView(subtitle);
                
                final LinearLayout container = new LinearLayout(a);
                container.setOrientation(LinearLayout.VERTICAL);
                card.addView(container);
                
                final String[] scheduleMode = {"daily", "weekly", "monthly", "interval"};
                final String[] modeNames = {"每日", "每周", "每月", "间隔"};
                final int[] currentMode = {0};
                
                final LinearLayout modeRow = new LinearLayout(a);
                modeRow.setOrientation(LinearLayout.HORIZONTAL);
                modeRow.setGravity(Gravity.LEFT | Gravity.BOTTOM);
                modeRow.setPadding(0, dp(a, 16), 0, 0);
                card.addView(modeRow);
                
                final TextView modeBtn = createButton(a, " 切换模式: " + modeNames[currentMode[0]], tc(a, "primary"), tc(a, "primary_container"), 12f, 6, 12, 8, false, 0, 0, null);
                modeRow.addView(modeBtn);
                
                String currentValue = target.getText().toString().trim();
                String modeTmp = "";
                String[] toksTmp = new String[0];
                if (currentValue.indexOf(":") > 0 && currentValue.indexOf(" ") < 0) {
                    toksTmp = currentValue.split(":");
                    if (toksTmp.length >= 2) modeTmp = toksTmp[0].toLowerCase();
                } else if (currentValue.trim().length() > 0) {
                    String[] ptmp = currentValue.split("\\s+");
                    if (ptmp.length >= 4 && ptmp[0].toLowerCase().startsWith("w")) {
                        modeTmp = "w";
                        toksTmp = new String[]{"w", ptmp[0].substring(1), ptmp[1], ptmp[2], ptmp[3]};
                    } else if (ptmp.length >= 4) {
                        modeTmp = "m";
                        toksTmp = new String[]{"m", ptmp[0], ptmp[1], ptmp[2], ptmp[3]};
                    } else if (ptmp.length >= 3) {
                        modeTmp = "d";
                        toksTmp = new String[]{"d", ptmp[0], ptmp[1], ptmp[2]};
                    }
                }
                if ("w".equals(modeTmp)) currentMode[0] = 1;
                else if ("m".equals(modeTmp)) currentMode[0] = 2;
                else if ("i".equals(modeTmp)) currentMode[0] = 3;
                else currentMode[0] = 0;
                modeBtn.setText(" 切换模式: " + modeNames[currentMode[0]]);
                final String parseMode = modeTmp;
                final String[] parseToks = toksTmp;
                
                final Runnable rebuildUI = new Runnable() {
                    public void run() {
                        container.removeAllViews();
                        
                        if (currentMode[0] == 0) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置每日执行的时间 (时:分:秒)");
                            hint.setTextColor(tc(a, "on_surface_variant"));
                            hint.setTextSize(12);
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout timeRow = new LinearLayout(a);
                            timeRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(timeRow);
                            
                            final EditText hourInput = makeInput(a, "时", null);
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setTextColor(tc(a, "on_surface_variant"));
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", null);
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setTextColor(tc(a, "on_surface_variant"));
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", null);
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(secondInput);
                            
                            if ("d".equals(parseMode) && parseToks.length >= 4) {
                                try {
                                    hourInput.setText(parseToks[1]);
                                    minuteInput.setText(parseToks[2]);
                                    secondInput.setText(parseToks[3]);
                                } catch (Throwable e) { traceLog("function_log", "[showSchedulePicker] 异常: " + e); }
                            } else {
                                hourInput.setText("12");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                            
                        } else if (currentMode[0] == 1) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置每周执行 (星期 时:分:秒)");
                            hint.setTextColor(tc(a, "on_surface_variant"));
                            hint.setTextSize(12);
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout weekRow = new LinearLayout(a);
                            weekRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(weekRow);
                            
                            final Spinner daySpinner = new Spinner(a);
                            String[] days = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                            ArrayAdapter adapter = new ArrayAdapter(a, android.R.layout.simple_spinner_item, days);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            daySpinner.setAdapter(adapter);
                            daySpinner.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            weekRow.addView(daySpinner);
                            
                            LinearLayout timeRow = new LinearLayout(a);
                            timeRow.setOrientation(LinearLayout.HORIZONTAL);
                            timeRow.setPadding(dp(a, 8), 0, 0, 0);
                            weekRow.addView(timeRow);
                            
                            final EditText hourInput = makeInput(a, "时", null);
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setTextColor(tc(a, "on_surface_variant"));
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", null);
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setTextColor(tc(a, "on_surface_variant"));
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", null);
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(secondInput);
                            
                            if ("w".equals(parseMode) && parseToks.length >= 5) {
                                try {
                                    int day = Integer.parseInt(parseToks[1]);
                                    if (day >= 1 && day <= 7) {
                                        daySpinner.setSelection(day - 1);
                                    }
                                    hourInput.setText(parseToks[2]);
                                    minuteInput.setText(parseToks[3]);
                                    secondInput.setText(parseToks[4]);
                                } catch (Throwable e) { traceLog("function_log", "[showSchedulePicker] 异常: " + e); }
                            } else {
                                daySpinner.setSelection(0);
                                hourInput.setText("12");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                            
                        } else if (currentMode[0] == 2) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置每月执行 (日期 时:分:秒)");
                            hint.setTextColor(tc(a, "on_surface_variant"));
                            hint.setTextSize(12);
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout monthRow = new LinearLayout(a);
                            monthRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(monthRow);
                            
                            final EditText dayInput = makeInput(a, "日期(1-31)", null);
                            dayInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            dayInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            monthRow.addView(dayInput);
                            
                            LinearLayout timeRow = new LinearLayout(a);
                            timeRow.setOrientation(LinearLayout.HORIZONTAL);
                            timeRow.setPadding(dp(a, 8), 0, 0, 0);
                            monthRow.addView(timeRow);
                            
                            final EditText hourInput = makeInput(a, "时", null);
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setTextColor(tc(a, "on_surface_variant"));
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", null);
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setTextColor(tc(a, "on_surface_variant"));
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", null);
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(secondInput);
                            
                            if ("m".equals(parseMode) && parseToks.length >= 5) {
                                try {
                                    dayInput.setText(parseToks[1]);
                                    hourInput.setText(parseToks[2]);
                                    minuteInput.setText(parseToks[3]);
                                    secondInput.setText(parseToks[4]);
                                } catch (Throwable e) { traceLog("function_log", "[showSchedulePicker] 异常: " + e); }
                            } else {
                                dayInput.setText("1");
                                hourInput.setText("00");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                            
                        } else if (currentMode[0] == 3) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置间隔执行 (时:分:秒)");
                            hint.setTextColor(tc(a, "on_surface_variant"));
                            hint.setTextSize(12);
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout intervalRow = new LinearLayout(a);
                            intervalRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(intervalRow);
                            
                            final EditText hourInput = makeInput(a, "时", null);
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            intervalRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setTextColor(tc(a, "on_surface_variant"));
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            intervalRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", null);
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            intervalRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setTextColor(tc(a, "on_surface_variant"));
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            intervalRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", null);
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            intervalRow.addView(secondInput);
                            
                            if ("i".equals(parseMode) && parseToks.length >= 4) {
                                try {
                                    hourInput.setText(parseToks[1]);
                                    minuteInput.setText(parseToks[2]);
                                    secondInput.setText(parseToks[3]);
                                } catch (Throwable e) { traceLog("function_log", "[showSchedulePicker] 异常: " + e); }
                            } else {
                                hourInput.setText("01");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                        }
                        applyUiTheme(a, d, 1);
                    }
                };
                
                rebuildUI.run();
                
                modeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        currentMode[0] = (currentMode[0] + 1) % 4;
                        modeBtn.setText(" 切换模式: " + modeNames[currentMode[0]]);
                        rebuildUI.run();
                    }
                });
                
                LinearLayout btnRow = new LinearLayout(a);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setPadding(0, dp(a, 20), 0, 0);
                card.addView(btnRow);
                
                TextView cancelBtn = createButton(a, "取消", tc(a, "on_surface_variant"), tc(a, "surface"), 14f, 6, 24, 12, false, 0, 0, null);
                cancelBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        animateDialogOut(d, null);
                    }
                });
                btnRow.addView(cancelBtn);
                
                TextView okBtn = createButton(a, "确定", Color.WHITE, tc(a, "primary"), 14f, 6, 24, 12, false, 0, 0, null);
                LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                okParams.setMargins(dp(a, 12), 0, 0, 0);
                okBtn.setLayoutParams(okParams);
                btnRow.addView(okBtn);
                
                okBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        StringBuilder result = new StringBuilder();
                        
                        if (currentMode[0] == 0) {
                            LinearLayout timeRow = (LinearLayout)container.getChildAt(1);
                            EditText hourInput = (EditText)timeRow.getChildAt(0);
                            EditText minuteInput = (EditText)timeRow.getChildAt(2);
                            EditText secondInput = (EditText)timeRow.getChildAt(4);

                            String hour = hourInput.getText().toString().trim();
                            String minute = minuteInput.getText().toString().trim();
                            String second = secondInput.getText().toString().trim();

                            if (hour.isEmpty()) hour = "12";
                            if (minute.isEmpty()) minute = "00";
                            if (second.isEmpty()) second = "00";

                            result.append("d:").append(hour).append(":").append(minute).append(":").append(second);

                        } else if (currentMode[0] == 1) {
                            LinearLayout weekRow = (LinearLayout)container.getChildAt(1);
                            Spinner daySpinner = (Spinner)weekRow.getChildAt(0);
                            LinearLayout timeRow = (LinearLayout)weekRow.getChildAt(1);
                            EditText hourInput = (EditText)timeRow.getChildAt(0);
                            EditText minuteInput = (EditText)timeRow.getChildAt(2);
                            EditText secondInput = (EditText)timeRow.getChildAt(4);

                            int dayIndex = daySpinner.getSelectedItemPosition() + 1;
                            String hour = hourInput.getText().toString().trim();
                            String minute = minuteInput.getText().toString().trim();
                            String second = secondInput.getText().toString().trim();

                            if (hour.isEmpty()) hour = "12";
                            if (minute.isEmpty()) minute = "00";
                            if (second.isEmpty()) second = "00";

                            result.append("w:").append(dayIndex).append(":")
                                  .append(hour).append(":").append(minute).append(":").append(second);

                        } else if (currentMode[0] == 2) {
                            LinearLayout monthRow = (LinearLayout)container.getChildAt(1);
                            EditText dayInput = (EditText)monthRow.getChildAt(0);
                            LinearLayout timeRow = (LinearLayout)monthRow.getChildAt(1);
                            EditText hourInput = (EditText)timeRow.getChildAt(0);
                            EditText minuteInput = (EditText)timeRow.getChildAt(2);
                            EditText secondInput = (EditText)timeRow.getChildAt(4);

                            String day = dayInput.getText().toString().trim();
                            String hour = hourInput.getText().toString().trim();
                            String minute = minuteInput.getText().toString().trim();
                            String second = secondInput.getText().toString().trim();

                            if (day.isEmpty()) day = "1";
                            if (hour.isEmpty()) hour = "00";
                            if (minute.isEmpty()) minute = "00";
                            if (second.isEmpty()) second = "00";

                            result.append("m:").append(day).append(":")
                                  .append(hour).append(":").append(minute).append(":").append(second);

                        } else if (currentMode[0] == 3) {
                            LinearLayout intervalRow = (LinearLayout)container.getChildAt(1);
                            EditText hourInput = (EditText)intervalRow.getChildAt(0);
                            EditText minuteInput = (EditText)intervalRow.getChildAt(2);
                            EditText secondInput = (EditText)intervalRow.getChildAt(4);

                            String hour = hourInput.getText().toString().trim();
                            String minute = minuteInput.getText().toString().trim();
                            String second = secondInput.getText().toString().trim();

                            if (hour.isEmpty()) hour = "01";
                            if (minute.isEmpty()) minute = "00";
                            if (second.isEmpty()) second = "00";

                            result.append("i:").append(hour).append(":").append(minute).append(":").append(second);
                        }
                        
                        target.setText(result.toString());
                        animateDialogOut(d, null);
                    }
                });
                
                d.setContentView(outer);
                d.getWindow().setLayout(Math.min(dp(a, 400), a.getResources().getDisplayMetrics().widthPixels - dp(a, 32)), -2);
                d.show();
                applyUiTheme(a, d, 1);
                animateDialogIn(d);
            } catch (Throwable e) {
                traceLog("function_log", "[showSchedulePicker]" + e);
            }
        }
    });
}

void testCode(String funcName, String code, String originalFunc) {
    ThreadPool.execute(new Runnable() {
    public void run() {
    if (code == null || code.equals("")) code = readCodeFile(funcName);
    if (code.equals("")) { toast("代码为空"); return; }
    try {
        this.interpreter.set("this", this);
        this.interpreter.set("qq", myUin);
        this.interpreter.set("pluginPath", pluginPath);
        this.interpreter.set("qun", "123456789");
        this.interpreter.set("uin", "987654321");
        this.interpreter.set("msg", "测试内容");
        this.interpreter.set("msgId", 123456789);
        this.interpreter.set("msgType", 0);
        this.interpreter.set("type", 2);
        scriptLoader.loadAndExecute(funcName, this.interpreter);
        toast("✓ 测试完成");
    } catch (Throwable e) {
        traceLog("function_log", "[testCode]" + e);
        toast("✗ 失败");
  		   }
 	   }
	});
}

void showEdit(Activity a, final String func, final String gid, final String gn) {
    traceLog("function_log", "[showEdit] 开始编辑: " + func);
    try {
        final String[] m = getMeta(func);
        if (m == null) return;
        
        final boolean isFile = m[1].equals("1");
        final boolean[] isFileState = {isFile};
        final boolean hasGrp = m[9].equals("1");
        final String timeVal = m[11];
        long interval = 0;
        int loopCount = 0;
        boolean isLoop = m[13].equals("1");
        int preType = 0;
        String preTail = "";
        int repeatSend = 0;
        int repeatConcat = 0;
        try {
            interval = Long.parseLong(m[12]);
            loopCount = Integer.parseInt(m[14]);
            repeatSend = Integer.parseInt(m[17]);
            repeatConcat = Integer.parseInt(m[18]);
        } catch (Throwable e) { traceLog("function_log", "[showEdit] 异常: " + e); }
        
        try {
            JSONObject jo = new JSONObject(getString("HotPlug", "meta_" + func, ""));
            preType = jo.optInt("pt");
            preTail = jo.optString("tail");
        } catch (Throwable e) { traceLog("function_log", "[showEdit] 异常: " + e); }
        
        final boolean[] cks = {
            m[2].equals("1"), m[3].equals("1"), m[4].equals("1"), 
            m[5].equals("1"), m[6].equals("1"), m[7].equals("1"), 
            m[8].equals("1"), hasGrp
        };
        
        final android.app.Dialog d = new android.app.Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
        d.requestWindowFeature(1);
        d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        
        Window window = d.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        int screenWidth = a.getResources().getDisplayMetrics().widthPixels;
        params.width = Math.min(dp(a, 400), screenWidth - dp(a, 32));
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.verticalMargin = 0.0f;
        window.setAttributes(params);
        
        d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            public void onDismiss(android.content.DialogInterface dialog) { isAdding = false; }
        });
        
        FrameLayout outer = new FrameLayout(a);
        outer.setPadding(dp(a, 20), dp(a, 32), dp(a, 20), dp(a, 20));
        ScrollView sc = new ScrollView(a);
        outer.addView(sc);
        
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundRect(tc(a, "surface"), dp(a, 12)));
        card.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
        sc.addView(card);
        
        TextView t = new TextView(a);
        t.setText("编辑:" + func);
        t.setTextSize(16);
        t.setTextColor(tc(a, "on_surface"));
        card.addView(t);
        
        final EditText etName = addNameInput(a, card, func);
        final EditText etCode = addCodeInput(a, card, isFile ? m[10] : readCodeFile(func), isFile, isFileState);
        addPresetRows(a, card, etCode);
        
        TextView tips = new TextView(a);
        tips.setText("变量:qun群号 uinQQ号 msg消息内容 msgId消息ID type类型(1私聊2群聊) operator操作者 time禁言秒数");
        tips.setTextSize(9);
        tips.setTextColor(tc(a, "on_surface_variant"));
        tips.setPadding(0, dp(a, 4), 0, 0);
        card.addView(tips);
        
        final FormComponents fc = new FormComponents();
        final TextView[] chips = addChipRows(a, card, cks, fc);
        
        addPreprocRow(a, card, fc, preType, preTail, repeatSend, repeatConcat);
        updatePreprocVisibility(fc, cks[6]);
        
        FormComponents loopFc = addLoopRow(a, card, isLoop, interval, loopCount, cks);
        fc.etInterval = loopFc.etInterval;
        fc.etCount = loopFc.etCount;
        fc.chipLoop = loopFc.chipLoop;
        fc.loopSettings = loopFc.loopSettings;
        
        final EditText etTime = addTimeRow(a, card, timeVal);
        
        LinearLayout btnRow = new LinearLayout(a);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(a, 12), 0, 0);
        card.addView(btnRow);
        
        TextView sv = createButton(a, "保存", Color.WHITE, tc(a, "primary"), 14f, 8, 16, 10, false, 0, 0, null);
        sv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        btnRow.addView(sv);

        TextView tst = createButton(a, "测试", tc(a, "on_surface_variant"), tc(a, "surface"), 14f, 8, 16, 10, false, 0, 0, null);
        LinearLayout.LayoutParams lpTst = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lpTst.setMargins(dp(a, 6), 0, dp(a, 6), 0);
        tst.setLayoutParams(lpTst);
        btnRow.addView(tst);

        TextView cn = createButton(a, "取消", tc(a, "on_surface_variant"), tc(a, "surface"), 14f, 8, 16, 10, false, 0, 0, null);
        cn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        btnRow.addView(cn);
        
        sv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newName = etName.getText().toString().trim();
                String ct = etCode.getText().toString();
                if (newName.equals("") || (ct.equals("") && !cks[6])) {
                    toast("名称和内容不能为空(选择预处理时可不填代码)");
                    return;
                }
                boolean hasCb = false;
                for (int i = 0; i < 7; i++) {
                    cks[i] = Boolean.TRUE.equals(chips[i].getTag());
                    if (cks[i]) hasCb = true;
                }
                cks[7] = Boolean.TRUE.equals(chips[7].getTag());
                
                long intervalVal = 0;
                int countVal = 0;
                int preTypeVal = 0;
                String preTailVal = "";
                int repeatSendVal = 0;
                int repeatConcatVal = 0;
                
                if (!hasCb) {
                    try { intervalVal = Long.parseLong(fc.etInterval.getText().toString()); } 
                    catch (Throwable e) { toast("间隔格式错误"); return; }
                    try { countVal = Integer.parseInt(fc.etCount.getText().toString()); } 
                    catch (Throwable e) { countVal = 0; }
                }
                
                if (cks[6] && fc.preTypeChips != null) {
                    for (int i = 0; i < 50; i++) {
                        if (i < fc.preTypeChips.length && Boolean.TRUE.equals(fc.preTypeChips[i].getTag())) {
                            preTypeVal = i;
                            break;
                        }
                    }
                    preTailVal = fc.etPreTail.getText().toString();
                    try {
                        repeatSendVal = Integer.parseInt(fc.etRepeatSend.getText().toString());
                        repeatConcatVal = Integer.parseInt(fc.etRepeatConcat.getText().toString());
                    } catch (Throwable e) { traceLog("function_log", "[showEdit] 异常: " + e); }
                }
                
                String rawTime = etTime.getText().toString().trim();
                
                if (!newName.equals(func)) delFunc(func);
                
                saveFunc(newName, ct, isFileState[0], 
                    new boolean[]{cks[0], cks[1], cks[2], cks[3], cks[4], cks[5], cks[6]}, 
                    cks[7], rawTime, intervalVal, 
                    Boolean.TRUE.equals(fc.chipLoop.getTag()), countVal, preTypeVal, preTailVal, repeatSendVal, repeatConcatVal);
                
                toast("已保存");
                isAdding = false;
                animateDialogOut(d, null);
            }
        });
        
        tst.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String ct = etCode.getText().toString();
                if (ct.equals("")) { toast("代码为空"); return; }
                testCode(func, ct, func);
            }
        });
        
        cn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { 
                isAdding = false;
                animateDialogOut(d, null);
            }
        });
        
        d.setContentView(outer);
        d.getWindow().setLayout(Math.min(dp(a, 400), screenWidth - dp(a, 32)), -2);
        WindowManager.LayoutParams finalParams = d.getWindow().getAttributes();
        finalParams.gravity = Gravity.CENTER;
        finalParams.verticalMargin = 0.0f;
        d.getWindow().setAttributes(finalParams);
        d.show();
        applyUiTheme(a, d, 1);
        animateDialogIn(d);
        
    } catch (Throwable e) {
        isAdding = false;
        toast("打开编辑失败: " + e.getMessage());
    }
}

void createItem(final Activity a, LinearLayout c, final String f, final String gid, final String gn, final Runnable refresh) {
    try {
        String[] m = getMeta(f);
        if (m == null) return;
        boolean hasGrp = m[9].equals("1"); 
        boolean hasAnyCallback = false;
        for (int i = 2; i <= 8; i++) {
            if (m[i].equals("1")) hasAnyCallback = true;
        }
        long interval = 0;
        int loopCount = 0;
        try {
            interval = Long.parseLong(m[12]); 
        } catch (Throwable e) { traceLog("function_log", "[createItem] 异常: " + e); }
        try {
            loopCount = Integer.parseInt(m[14]); 
        } catch (Throwable e) { traceLog("function_log", "[createItem] 异常: " + e); }
        boolean isLoop = m[13].equals("1"); 
        String timeCfg = m[11]; 
        boolean isScheduled = (timeCfg != null && !timeCfg.equals(""));

        final HorizontalScrollView slideView = new HorizontalScrollView(a);
        slideView.setHorizontalScrollBarEnabled(false);
        slideView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams slideParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slideParams.setMargins(0, dp(a, 6), 0, 0);
        slideView.setLayoutParams(slideParams);

        final LinearLayout itemContainer = new LinearLayout(a);
        itemContainer.setOrientation(LinearLayout.HORIZONTAL);
        slideView.addView(itemContainer);

        int screenWidth = a.getResources().getDisplayMetrics().widthPixels;
        final int deleteBtnWidth = dp(a, 80);

        int visibleContentWidth = screenWidth - dp(a, 72);

        final LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackground(makeFeedbackBg(tc(a, "surface"), adjustColor(tc(a, "surface"), 0.85f), dp(a, 8)));
        
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(visibleContentWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        contentParams.setMargins(0, 0, dp(a, 1), 0); 
        content.setLayoutParams(contentParams);
        
        content.setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10));
        content.setClickable(true);
        content.setFocusable(true);
        itemContainer.addView(content);

        final TextView deleteBtn = createButton(a, "删除", Color.WHITE, tc(a, "error"), 14f, 8, 16, 10, false, 0, 0, null);
        deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(deleteBtnWidth, LinearLayout.LayoutParams.MATCH_PARENT));
        itemContainer.addView(deleteBtn);

        LinearLayout r1 = new LinearLayout(a);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(r1);

        HorizontalScrollView titleScroll = new HorizontalScrollView(a);
        titleScroll.setHorizontalScrollBarEnabled(false);
        titleScroll.setClickable(false);
        titleScroll.setFocusable(false);
        LinearLayout.LayoutParams titleScrollParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        titleScrollParams.rightMargin = dp(a, 8);
        titleScroll.setLayoutParams(titleScrollParams);
        r1.addView(titleScroll);

        final TextView tv = new TextView(a);
        tv.setText(f);
        tv.setTextSize(14);
        tv.setTextColor(tc(a, "on_surface"));
        tv.setSingleLine(true);
        tv.setClickable(false);
        titleScroll.addView(tv);

        LinearLayout switchContainer = new LinearLayout(a);
        switchContainer.setOrientation(LinearLayout.HORIZONTAL);
        switchContainer.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams switchContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        switchContainer.setLayoutParams(switchContainerParams);
        r1.addView(switchContainer);

        if (!hasAnyCallback && isLoop && loopCount > 0) {
            TextView loopInfo = new TextView(a);
            loopInfo.setText(interval + "ms×" + loopCount);
            loopInfo.setTextSize(10);
            loopInfo.setTextColor(pc("#FF9800"));
            loopInfo.setPadding(0, 0, dp(a, 4), 0);
            loopInfo.setMaxWidth(dp(a, 70));
            loopInfo.setSingleLine(true);
            loopInfo.setEllipsize(TextUtils.TruncateAt.END);
            switchContainer.addView(loopInfo, 0);
        }

        final boolean[] mainOn = {hasAnyCallback ? getRun(f) : getLoad(f)};
        final TextView btnMain = makeSwitch(a, mainOn[0], hasAnyCallback ? pc("#00C853") : tc(a, "primary"));
        btnMain.setMinWidth(dp(a, 50));
        btnMain.setMinimumWidth(dp(a, 50));
        LinearLayout.LayoutParams btnMainParams = new LinearLayout.LayoutParams(dp(a, 50), LinearLayout.LayoutParams.WRAP_CONTENT);
        btnMain.setLayoutParams(btnMainParams);
        switchContainer.addView(btnMain);

        LinearLayout infoRow = new LinearLayout(a);
        infoRow.setOrientation(LinearLayout.VERTICAL);
        infoRow.setPadding(0, dp(a, 6), 0, 0);
        content.addView(infoRow);

        if (!hasAnyCallback && mainOn[0]) {
            String infoText = "";
            if (isScheduled) {
                long now = System.currentTimeMillis();
                long nextTime = getNextScheduleTime(timeCfg, now);
                if (nextTime > 0) {
                    long countdown = nextTime - now;
                    if (countdown < 0) infoText = "⏰ " + formatSchedule(timeCfg) + " (计算中)";
                    else infoText = "⏰ " + formatSchedule(timeCfg) + " (剩" + formatRemainingTimeMs(countdown) + ")";
                } else infoText = "⏰ " + formatSchedule(timeCfg) + " (配置无效)";
            } else if (isLoop) {
                infoText = "🔁 " + interval + "ms ×" + (loopCount == 0 ? "∞" : loopCount);
            }
            if (!infoText.equals("")) {
                TextView tvInfo = new TextView(a);
                tvInfo.setText(infoText);
                tvInfo.setTextSize(10);
                tvInfo.setTextColor(tc(a, "on_surface_variant"));
                tvInfo.setPadding(0, dp(a, 4), 0, 0);
                infoRow.addView(tvInfo);
            }
        }

        final LinearLayout exp = new LinearLayout(a);
        exp.setOrientation(LinearLayout.VERTICAL);
        exp.setVisibility(View.GONE);
        exp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        exp.setPadding(dp(a, 8), dp(a, 8), dp(a, 8), 0);
        exp.setAlpha(0f);
        content.addView(exp);

        View dlv = new View(a);
        dlv.setBackgroundColor(tc(a, "outline"));
        dlv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(a, 1)));
        exp.addView(dlv);

        TextView btnTestExp = createButton(a, "▶ 测试执行", tc(a, "primary"), tc(a, "primary_container"), 12f, 6, 0, 8, false, 0, 0, null);
        btnTestExp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                testCode(f, null, f);
            }
        });
        LinearLayout.LayoutParams lpTestExp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTestExp.setMargins(0, dp(a, 8), 0, 0);
        btnTestExp.setLayoutParams(lpTestExp);
        exp.addView(btnTestExp);

        if (hasAnyCallback) {
            LinearLayout rRun = new LinearLayout(a); rRun.setOrientation(LinearLayout.HORIZONTAL); rRun.setGravity(Gravity.CENTER_VERTICAL); rRun.setPadding(0, dp(a, 8), 0, 0); exp.addView(rRun);
            TextView l = new TextView(a); l.setText("运行开关"); l.setTextSize(13); l.setTextColor(tc(a, "on_surface_variant")); l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rRun.addView(l);
            TextView state = new TextView(a); state.setText("主开关统管"); state.setTextSize(11); state.setTextColor(tc(a, "on_surface_variant")); rRun.addView(state);
        } else {
             LinearLayout rRun = new LinearLayout(a); rRun.setOrientation(LinearLayout.HORIZONTAL); rRun.setGravity(Gravity.CENTER_VERTICAL); rRun.setPadding(0, dp(a, 8), 0, 0); exp.addView(rRun);
             TextView l = new TextView(a); l.setText("允许运行"); l.setTextSize(13); l.setTextColor(tc(a, "on_surface_variant")); l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rRun.addView(l);
             final boolean[] runOn = {getRun(f)}; final TextView btnRun = makeSwitch(a, runOn[0], pc("#00C853")); rRun.addView(btnRun);
             btnRun.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { runOn[0] = !runOn[0]; setRun(f, runOn[0]); setSwitch(btnRun, runOn[0], pc("#00C853")); }});
             
             if (isLoop) {
                LinearLayout rLoop = new LinearLayout(a); rLoop.setOrientation(LinearLayout.HORIZONTAL); rLoop.setGravity(Gravity.CENTER_VERTICAL); rLoop.setPadding(0, dp(a, 8), 0, 0); exp.addView(rLoop);
                TextView l2 = new TextView(a); String countText = loopCount > 0 ? (" (剩" + loopCount + "次)") : ""; l2.setText("循环执行" + countText); l2.setTextSize(13); l2.setTextColor(tc(a, "on_surface_variant")); l2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rLoop.addView(l2);
                final boolean[] loopOn = {getLoop(f)}; final TextView btnLoop = makeSwitch(a, loopOn[0], pc("#FF9800")); rLoop.addView(btnLoop);
                btnLoop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { loopOn[0] = !loopOn[0]; setLoop(f, loopOn[0]); setSwitch(btnLoop, loopOn[0], pc("#FF9800")); toast(loopOn[0] ? "循环已开" : "循环已关"); }});
             }
        }
        
        if (hasGrp && !gid.equals("")) {
             LinearLayout rGrp = new LinearLayout(a); rGrp.setOrientation(LinearLayout.HORIZONTAL); rGrp.setGravity(Gravity.CENTER_VERTICAL); rGrp.setPadding(0, dp(a, 8), 0, 0); exp.addView(rGrp);
             TextView l = new TextView(a); l.setText("本群运行"); l.setTextSize(13); l.setTextColor(tc(a, "on_surface_variant")); l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rGrp.addView(l);
             final boolean[] grpOn = {getGrp(f, gid)}; final TextView btnGrp = makeSwitch(a, grpOn[0], pc("#FF9800")); rGrp.addView(btnGrp);
             btnGrp.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { grpOn[0] = !grpOn[0]; setGrp(f, gid, grpOn[0]); setSwitch(btnGrp, grpOn[0], pc("#FF9800")); toast(grpOn[0] ? "本群已开启" : "本群已关闭"); }});
        }

        final LinearLayout itemWrapper = new LinearLayout(a);
        itemWrapper.setOrientation(LinearLayout.VERTICAL);
        itemWrapper.addView(slideView);
        View itemDivider = new View(a);
        itemDivider.setBackgroundColor(tc(a, "outline"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(a, 1));
        dividerParams.bottomMargin = dp(a, 4);
        itemWrapper.addView(itemDivider, dividerParams);

        final boolean[] isExpanded = {false};

        final int swipeThreshold = dp(a, 24);
        final int swipeVelocityThreshold = dp(a, 300);

        final GestureDetector gestureDetector = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            public boolean onDown(MotionEvent e) {
                content.animate().scaleX(0.98f).scaleY(0.98f).setDuration(50).start();
                return true; 
            }

            public boolean onSingleTapConfirmed(MotionEvent e) {
                content.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                if (isExpanded[0]) {
                    isExpanded[0] = false;
                    exp.setVisibility(View.GONE);
                    tv.setText(f);
                } else {
                    isExpanded[0] = true;
                    exp.setVisibility(View.VISIBLE);
                    exp.setAlpha(0f);
                    exp.animate().alpha(1f).setDuration(250).start();
                    tv.setText(f);
                }
                return true;
            }

            public void onLongPress(MotionEvent e) {
                content.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                content.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                a.runOnUiThread(new Runnable() {
                    public void run() {
                        showEdit(a, f, gid, gn);
                    }
                });
            }

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY()) && 
                        Math.abs(diffX) > swipeThreshold && 
                        Math.abs(velocityX) > swipeVelocityThreshold) {
                        
                        if (diffX < 0) {
                            slideView.smoothScrollTo(deleteBtnWidth, 0);
                        } else {
                            slideView.smoothScrollTo(0, 0);
                        }
                        result = true;
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                if(result) {
                    content.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                }
                return result;
            }
            
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (Math.abs(distanceX) > Math.abs(distanceY)) {
                    slideView.requestDisallowInterceptTouchEvent(true);
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }
        });

        View.OnTouchListener unifiedTouchListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                boolean handled = gestureDetector.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    content.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    int scrollX = slideView.getScrollX();
                    if (!handled) {
                        if (scrollX >= deleteBtnWidth / 2) {
                            slideView.smoothScrollTo(deleteBtnWidth, 0);
                        } else {
                            slideView.smoothScrollTo(0, 0);
                        }
                    }
                }
                return true; 
            }
        };

        content.setOnTouchListener(unifiedTouchListener);
        titleScroll.setOnTouchListener(unifiedTouchListener);
        
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDeleteConfirm(a, f, itemWrapper, c, refresh);
            }
        });

        btnMain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean oldState = mainOn[0];
                mainOn[0] = !mainOn[0];
                if (hasAnyCallback) {
                    setRun(f, mainOn[0]);
                    setSwitch(btnMain, mainOn[0], pc("#00C853"));
                    toast(mainOn[0] ? "运行已开" : "运行已关");
                } else {
                    setLoad(f, mainOn[0]);
                    setSwitch(btnMain, mainOn[0], tc(a, "primary"));
                    toast(mainOn[0] ? "加载已开" : "加载已关");
                }
                if (refresh != null) refresh.run();
            }
        });

        c.addView(itemWrapper);

    } catch (Throwable e) {
        traceLog("function_log", "[createItem] 异常: " + f + " - " + e);
    }
}

void showDeleteConfirm(Activity a, final String f, final View itemView, final LinearLayout parent, final Runnable refresh) {
    final android.app.Dialog confirmDialog = new android.app.Dialog(a);
    confirmDialog.requestWindowFeature(1);
    confirmDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
    Window window = confirmDialog.getWindow();
    WindowManager.LayoutParams params = window.getAttributes();
    params.gravity = Gravity.CENTER;
    params.verticalMargin = 0.0f;
    window.setAttributes(params);
    
    LinearLayout layout = new LinearLayout(a);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackground(roundRect(tc(a, "surface"), dp(a, 16)));
    layout.setPadding(dp(a, 24), dp(a, 24), dp(a, 24), dp(a, 24));
    
    TextView title = new TextView(a);
    title.setText("确认删除");
    title.setTextSize(18);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(tc(a, "on_surface"));
    layout.addView(title);
    
    TextView message = new TextView(a);
    message.setText("确定要删除功能 \"" + f + "\" 吗？此操作无法撤销");
    message.setTextSize(14);
    message.setTextColor(tc(a, "on_surface_variant"));
    message.setPadding(0, dp(a, 12), 0, dp(a, 24));
    layout.addView(message);
    
    LinearLayout buttons = new LinearLayout(a);
    buttons.setOrientation(LinearLayout.HORIZONTAL);
    layout.addView(buttons);
    
    TextView cancel = createButton(a, "取消", tc(a, "on_surface_variant"), tc(a, "surface"), 15f, 8, 24, 12, false, 0, 0, null);
    cancel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
    cancel.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            confirmDialog.dismiss();
        }
    });
    buttons.addView(cancel);
    
    TextView confirm = createButton(a, "删除", Color.WHITE, tc(a, "error"), 15f, 8, 24, 12, false, 0, 0, null);
    LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, -2, 1f);
    confirmParams.setMargins(dp(a, 12), 0, 0, 0);
    confirm.setLayoutParams(confirmParams);
    confirm.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            delFunc(f);
            parent.removeView(itemView);
            toast("已删除");
            if (refresh != null) refresh.run();
            confirmDialog.dismiss();
        }
    });
    buttons.addView(confirm);
    
    confirmDialog.setContentView(layout);
    confirmDialog.getWindow().setLayout(Math.min(dp(a, 400), a.getResources().getDisplayMetrics().widthPixels - dp(a, 32)), -2);
    confirmDialog.show();
    applyUiTheme(a, confirmDialog, 1);
    animateDialogIn(confirmDialog);
}

void joinGroup(String g, String m) { 
    try { dispatchEvent(new String[]{g, m}, 2); } catch (Throwable e) { traceLog("function_log", "[joinGroup] 异常: " + e); } 
}

void quitGroup(String g, String m) { 
    try { dispatchEvent(new String[]{g, m}, 3); } catch (Throwable e) { traceLog("function_log", "[quitGroup] 异常: " + e); } 
}

void shutUpGroup(String g, String m, long t, String o) { 
    try { dispatchEvent(new Object[]{g, m, t, o}, 4); } catch (Throwable e) { traceLog("function_log", "[shutUpGroup] 异常: " + e); } 
}

void onPaiYiPai(String p, int t, String o) {
    try { dispatchEvent(new Object[]{p, t, o}, 6); } catch (Throwable e) { traceLog("function_log", "[onPaiYiPai] 异常: " + e); } 
}

String applyPreprocess(String text, int type) {
    if (text == null || text.isEmpty()) return text;

    if (type <= 0 || type > 49) return text;

    try {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = text.length();

        String[] combs = {
            null, null,                  
            "̲", "̶", "̳", "̸", "͒", "͙", null, null,  
            "꯭", "̲", "̿", "̬", "͟", "̤", "̥", "̸", "⃥", "⃫",  
            "⃘", "⃝", "⃞", "⃟", "⃠", "⃖", "⃗", "⃰", "⃢", "⃲",  
            "⃤", "⃦", "⃴", "⃵", "⃒", "⃓", "⃔", "⃕", "⃡", "⃪",  
            "⃬", "⃭", "⃮", "⃯", "⃱", "⃫", "⃷", "̿", "⃰", "⃠"   
        };

        String comb = combs[type];
        if (comb == null) {
            if (type == 8) { 
                return new StringBuilder(text).reverse().toString();
            } else if (type == 9) { 
                while (i < len) {
                    String seg = extractSegmentAt(text, i);
                    if (seg.isEmpty()) break;
                    sb.append(seg).append(seg);
                    i += seg.length();
                }
                return sb.toString();
            } else {
                return text; 
            }
        }

        while (i < len) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);
            String ch = text.substring(i, i + charCount);
            sb.append(ch).append(comb);
            i += charCount;
        }
        sb.append(comb);

        return sb.toString();

    } catch (Exception e) {
        return text;
    }
}

void addToSendQueue(String uin, String msg, int type) {
    SendUnit unit = new SendUnit(uin, msg, type);
    sendMsgQueue.offer(unit);
    processSendQueue();
}

void processSendQueue() {
    if (!isProcessingQueue.compareAndSet(false, true)) return;
    
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                while (!sendMsgQueue.isEmpty()) {
                    SendUnit unit = (SendUnit)sendMsgQueue.poll();
                    if (unit != null) {
                        try {
                            sendMsg(unit.uin, unit.msg, unit.type);
                            Thread.sleep(200);
                        } catch (Throwable e) {
                            traceLog("function_log", "[processSendQueue] 发送失败: " + e);
                        }
                    }
                }
            } finally {
                isProcessingQueue.set(false);
            }
        }
    });
}

String getMsgSplit(String m) {
    if(m == null || m.isEmpty()) return m;
    
    try {
        if(currentPeerUin != null && !currentPeerUin.isEmpty() && currentChatType > 0) {
            
            final String fullMsg = m;
            final String targetUin = currentPeerUin;
            final int targetType = currentChatType;
            
            String firstSeg = extractSegmentAt(fullMsg, 0);
            if (firstSeg.isEmpty()) {
                return m;
            }
            int currentPos = firstSeg.length();
            
            if (currentPos < fullMsg.length()) {
                ThreadPool.execute(new Runnable() {
                    public void run() {
                        try {
                            int pos = currentPos;
                            int len = fullMsg.length();
                            while (pos < len) {
                                String seg = extractSegmentAt(fullMsg, pos);
                                if (seg.isEmpty()) break;
                                addToSendQueue(targetUin, seg, targetType);
                                pos += seg.length();
                            }
                        } catch (Throwable e) {
                            traceLog("function_log", "[getMsgSplit] 消息切片入队失败: " + e);
                        }
                    }
                });
            }
            return firstSeg;
        }
        return m;
    } catch(Throwable e) {
        traceLog("function_log", "[getMsgSplit] 逐字模块异常: " + e);
        return m;
    }
}

String getMsg(String m){
    if(m == null || m.isEmpty()) return m;
    if (inPreproc) return m;
    inPreproc = true;
    
    try{
        Integer key = new Integer(7);
        if (eventRegistry.containsKey(key)) {
            ArrayList list = (ArrayList) eventRegistry.get(key);
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    String func = (String)list.get(i);
                    HashMap cfg = (HashMap)preProcConfig.get(func);
                    if (cfg == null) continue;
                    if (!getRun(func)) continue;
                    
                    if (Boolean.TRUE.equals(cfg.get("grp"))) {
                        if (currentPeerUin == null || currentPeerUin.isEmpty()) continue;
                        if (!getGrp(func, currentPeerUin)) continue;
                    }
                    
                    int type = (Integer)cfg.get("type");
                    String tail = (String)cfg.get("tail");
                    int repeatSend = 0;
                    int repeatConcat = 0;
                    try {
                        repeatSend = (Integer)cfg.get("rs");
                        repeatConcat = (Integer)cfg.get("rc");
                    } catch (Throwable e) { traceLog("function_log", "[getMsg] 异常: " + e); }
                    
                    String result = m;
                    
                    try {
                        Object[] eventData = new Object[3];
                        eventData[0] = currentPeerUin; 
                        eventData[1] = currentChatType; 
                        eventData[2] = m; 
                        
                        dispatchEvent(eventData, 7);
                    } catch (Throwable e) {
                        traceLog("function_log", "[getMsg] dispatchEvent异常: " + e);
                    }
                    
                    if (type == 1) { 
                        return getMsgSplit(m);
                    } else if (type > 1) {
                        result = applyPreprocess(m, type);
                    }
                    
                    if (repeatConcat > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int k = 0; k < repeatConcat; k++) {
                            sb.append(result);
                        }
                        result = sb.toString();
                    }
                    
                    if (tail != null && !tail.trim().isEmpty()) {
                        if (tail.contains("msg")) {
                            String[] parts = tail.split("msg", 2);
                            if (parts.length == 2) {
                                result = parts[0].trim() + result + parts[1].trim();
                            } else if (parts.length == 1) {
                                if (tail.trim().startsWith("msg")) {
                                    result = result + parts[0].trim();
                                } else {
                                    result = parts[0].trim() + result;
                                }
                            }
                        } else {
                            result = result + tail;
                        }
                    }
                    
                    if (repeatSend > 1 && currentPeerUin != null && !currentPeerUin.isEmpty() && currentChatType > 0) {
                        for (int k = 1; k < repeatSend; k++) {
                            addToSendQueue(currentPeerUin, result, currentChatType);
                        }
                    }
                    
                    return result;
                }
            }
        }
        
        return m;
        
    }catch(Throwable e){
        traceLog("function_log", "[getMsg]" + e);
        return m;
    } finally {
        inPreproc = false;
    }
}

String extractSegmentAt(String text, int pos){
    if(pos >= text.length()) return "";
    
    char c = text.charAt(pos);
    
    if(c == '['){
        int end = text.indexOf(']', pos);
        if(end != -1 && end - pos < 25){
            return text.substring(pos, end + 1);
        }
    }
    
    if(c >= 0xD800 && c <= 0xDBFF && pos + 1 < text.length()){
        char low = text.charAt(pos + 1);
        if(low >= 0xDC00 && low <= 0xDFFF){
            return text.substring(pos, pos + 2);
        }
    }
    
    return String.valueOf(c);
}

rebuildRegistry();

