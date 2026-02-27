/*
 * 热插拔系统核心脚本 (HotPlug Kernel)
 * 
 * 【系统架构说明】
 * 1. 线程层: 负责独立线程的生命周期管理(启动/停止/休眠)。
 * 2. IO层: JSON元数据存储与脚本文件读写。
 * 3. 逻辑层: Load(总闸)/Run(运行)/Grp(群限)/Loop(循环) 四级开关控制。
 * 4. 交互层: 基于Android原生View构建的动态UI，复用表单组件。
 * 5. 事件层: 基于Registry的高效事件分发机制。
 * 
 */

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

/** 全局运行线程映射表 // HashMap<String, Thread>: 存储功能名与对应的线程对象 */
HashMap runningThreads = new HashMap();

/** 循环状态标记表 // HashMap<String, Boolean>: 存储功能是否处于循环模式 */
HashMap loopFlags = new HashMap();

/** 中央事件注册表 // HashMap<Integer, ArrayList<String>>: 存储事件类型ID与功能名列表的映射 */
HashMap eventRegistry = new HashMap();

/** 功能执行时间戳缓存 // HashMap<String, Long>: 存储功能最后一次执行的时间 */
HashMap lastExecTime = new HashMap();

/** 预处理配置缓存 // HashMap<String, HashMap>: 缓存预处理功能的具体配置（样式、尾巴等） */
HashMap preProcConfig = new HashMap();

/** 逐字发送消息队列 // Queue<SendUnit>: 存储待发送的消息单元 */
Queue sendMsgQueue = new ConcurrentLinkedQueue();

/** 逐字发送是否正在处理 // boolean: 队列处理锁状态 */
boolean isProcessingQueue = false;

/** 全局线程池 // ExecutorService: 用于执行异步任务 */
// ExecutorService ThreadPool = Executors.newCachedThreadPool();

/** 当前发送目标信息 // String: 当前聊天对象的QQ号或群号 */
String currentTargetUin = "";
/** 当前目标类型 // int: 1=私聊, 2=群聊 */
int currentTargetType = 0;

/** 全局状态存储 - 当前聊天对象 // String: 记录最后一次活跃的聊天对象Uin */
String currentPeerUin = "";
/** 全局状态存储 - 当前聊天类型 // int: 记录最后一次活跃的聊天类型 */
int currentChatType = 0;

/** 待分割的完整文本 // String: 逐字发送时的源文本 */
String splitBuffer = "";
/** 当前分割位置 // int: 逐字发送的游标 */
int splitPos = 0;
/** 逐字发送锁 // boolean: 防止并发分割 */
boolean isSplitting = false;

/** UI状态 - 是否正在添加/编辑中 // boolean: 控制弹窗状态 */
boolean isAdding = false;

/** 
 * 表单组件容器
 * 用于在 create 和 edit 之间复用 UI 元素的引用
 */
class FormComponents {
    EditText etName;        // EditText: 功能名称输入框
    EditText etCode;        // EditText: 代码内容输入框
    TextView chipFile;      // TextView: 文件模式切换按钮
    TextView[] chips;       // TextView[]: 回调类型选择按钮数组
    TextView chipLoop;      // TextView: 循环开关按钮
    EditText etInterval;    // EditText: 循环间隔输入框
    EditText etCount;       // EditText: 循环次数输入框
    EditText etTime;        // EditText: 定时时间输入框
    LinearLayout loopSettings; // LinearLayout: 循环设置区域容器
    
    // 预处理相关组件
    LinearLayout preprocContainer; // LinearLayout: 预处理设置区域
    TextView chipPreType;   // TextView: 存储当前选中样式的Tag
    EditText etPreTail;     // EditText: 小尾巴输入框
    TextView[] preTypeChips; // TextView[]: 预处理样式选择按钮数组
    EditText etRepeatSend;  // EditText: 重复发送次数输入框
    EditText etRepeatConcat;// EditText: 重复拼接次数输入框
    TextView dynamicTips;   // TextView: 动态提示文本区域
    TextView prePreview;    // TextView: 预处理效果预览区域
}

/** 
 * 消息发送单元
 * 用于队列存储单条待发送消息
 */
class SendUnit {
    String uin; // String: 目标对象
    String msg; // String: 消息内容
    int type;   // int: 消息类型
    
    SendUnit(String uin, String msg, int type) {
        this.uin = uin;
        this.msg = msg;
        this.type = type;
    }
}

/**
 * 热插拔类加载器
 * 负责脚本的哈希校验、编译缓存与反射执行
 */
class HotPlugClassLoader {
    ConcurrentHashMap objectCache = new ConcurrentHashMap(); // Cache: 脚本对象实例缓存
    ConcurrentHashMap directMethodCache = new ConcurrentHashMap(); // Cache: 脚本方法名缓存
    ConcurrentHashMap modeCache = new ConcurrentHashMap(); // Cache: 执行模式缓存 (1=对象, 2=脚本)
    ConcurrentHashMap timeStampCache = new ConcurrentHashMap(); // Cache: 文件修改时间戳缓存
    ConcurrentHashMap hashCache = new ConcurrentHashMap(); // Cache: 代码内容MD5缓存
    
    boolean interpreterInitialized = false; // boolean: 解释器初始化状态
    
    // 注入白名单变量
    final String[] SYNC_VARS = {
        "qun", "uin", "msg", "msgId", "msgType", "type", "data", "operator", "time", "paiType", "qq", "pluginPath", "this", "scriptLoader"
    };

    /**
     * 计算代码内容的 MD5 哈希
     * @param code String: 源代码内容
     * @return String: MD5哈希字符串
     */
    String getCodeHash(String code) {
        if (code == null || code.length() == 0) return "";
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(code.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * 通过反射获取 BeanShell This 对象的 NameSpace
     * @param scriptObj bsh.This: 脚本对象
     * @return bsh.NameSpace: 命名空间对象
     */
    bsh.NameSpace getObjNameSpace(bsh.This scriptObj) {
        try {
            java.lang.reflect.Method m = scriptObj.getClass().getMethod("getNameSpace", new Class[0]);
            return (bsh.NameSpace) m.invoke(scriptObj, new Object[0]);
        } catch (Throwable e) {
            traceLog("function_log", "[getObjNameSpace] 反射获取 NameSpace 失败: " + e);
            return null;
        }
    }

    /**
     * 加载并执行指定功能脚本
     * @param funcName String: 功能名称（文件名）
     * @param interpreter bsh.Interpreter: 解释器实例
     */
    void loadAndExecute(String funcName, bsh.Interpreter interpreter) {
        File f = new File(getDir() + "/" + funcName + ".java");
        if (!f.exists()) {
            traceLog("function_log", "[loadAndExecute] 文件不存在: " + funcName);
            return;
        }
        
        long lastMod = f.lastModified();
        String currentHash = getCodeHash(readCodeFile(funcName));
        Object cachedTime = timeStampCache.get(funcName);
        Object cachedHash = hashCache.get(funcName);
        
        boolean needsCompile = (cachedTime == null || !modeCache.containsKey(funcName) || !currentHash.equals(cachedHash) || ((Long)cachedTime).longValue() != lastMod);
        
        if (needsCompile) {
            hashCache.put(funcName, currentHash);
            timeStampCache.put(funcName, new Long(lastMod));
            
            try {
                String code = readCodeFile(funcName);
                if (code == null || code.trim().equals("")) return;
                
                boolean hasRunMethod = java.util.regex.Pattern.compile("void\\s+run\\s*\\(").matcher(code).find();
                String hashStr = Math.abs(funcName.hashCode()) + "_" + System.currentTimeMillis();
                
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
    
    /**
     * 清除指定功能的缓存
     * @param funcName String: 功能名称
     */
    void remove(String funcName) {
        timeStampCache.remove(funcName);
        hashCache.remove(funcName);
        objectCache.remove(funcName);
        directMethodCache.remove(funcName);
        modeCache.remove(funcName);
    }
}

/** 全局实例 // HotPlugClassLoader: 脚本加载器单例 */
HotPlugClassLoader scriptLoader = new HotPlugClassLoader();

/**
 * 获取脚本存储目录
 * @return String: 目录路径
 */
String getDir() {
    String d = pluginPath + "/HotPlug/Funcs";
    try {
        File f = new File(d);
        if (!f.exists()) f.mkdirs();
    } catch (Throwable e) {}
    return d;
}

/**
 * 读取本地代码文件
 * @param name String: 文件名（不含后缀）
 * @return String: 文件内容
 */
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

/**
 * 读取外部文件内容
 * @param p String: 绝对路径
 * @return String: 文件内容
 */
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

/**
 * 保存功能配置及代码
 * 对应 getMeta 的索引映射：
 * n=0, f=1, cb=2-8, g=9, p=10, t=11, i=12, l=13, c=14, pt=15, tail=16, rs=17, rc=18
 * 
 * @param n String: 功能名
 * @param content String: 代码或路径
 * @param isFile boolean: 是否为文件引用
 * @param cb boolean[]: 回调开关数组
 * @param hasGrp boolean: 是否启用群限
 * @param timeVal String: 定时配置字符串
 * @param interval long: 循环间隔
 * @param isLoop boolean: 是否循环
 * @param loopCount int: 循环次数
 * @param preType int: 预处理样式类型
 * @param preTail String: 预处理小尾巴
 * @param repeatSend int: 重复发送次数
 * @param repeatConcat int: 重复拼接次数
 */
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
        traceLog("save_err", "[saveFunc]" + e);
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
        jo.put("pt", preType);   // preType (0~49)
        jo.put("tail", preTail == null ? "" : preTail); 
        jo.put("rs", repeatSend);    
        jo.put("rc", repeatConcat);  
        
        putString("HotPlug", "meta_" + n, jo.toString());
    } catch (Throwable e) {
        traceLog("json_err", "[saveFunc] " + e);
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
        preProcConfig.put(n, cfg);
    } else {
        preProcConfig.remove(n);
    }
    
    if (!hasCallback(cb)) {
        stopThread(n);
        if (getLoad(n)) {
            startIndepThread(n, interval, loopCount); 
        }
    }
    traceLog("save", "[saveFunc]" + n);
}

/**
 * 获取元数据并转换为数组格式 (String[20])
 * 索引映射:
 * 0:Name, 1:IsFile, 2-8:CBs, 9:Grp, 10:Path, 11:Time, 12:Interval, 13:IsLoop, 14:Count, 15:Pt, 16:Tail, 17:Rs, 18:Rc
 * @param n String: 功能名
 * @return String[]: 元数据数组
 */
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

/**
 * 获取完整预处理配置
 * @param n String: 功能名
 * @return HashMap: 配置Map
 */
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
        return cfg;
    } catch (Throwable e) {
        return null;
    }
}

/**
 * 重建事件注册表
 * 遍历所有功能，根据配置的 Callback 类型注册到 eventRegistry
 */
void rebuildRegistry() {
    eventRegistry.clear();
    preProcConfig.clear();
    String[] fs = getAll();
    for (int i = 0; i < fs.length; i++) {
        String f = fs[i];
        if (f.equals("")) continue;
        String[] m = getMeta(f);
        if (m == null) continue;
        
        for (int type = 1; type <= 7; type++) {
            if (m[type + 1].equals("1")) {
                Integer key = new Integer(type);
                if (!eventRegistry.containsKey(key)) {
                    eventRegistry.put(key, new ArrayList());
                }
                ArrayList list = (ArrayList) eventRegistry.get(key);
                if (!list.contains(f)) list.add(f);
            }
        }
        
        if (m[8].equals("1")) {
            HashMap cfg = getPreProcConfig(f);
            if (cfg != null) {
                preProcConfig.put(f, cfg);
            }
        }
    }
}

/**
 * 获取所有功能列表
 * @return String[]: 功能名数组
 */
String[] getAll() {
    String l = getString("HotPlug", "list", "");
    if (l.equals("")) return new String[0];
    if (l.endsWith(",")) l = l.substring(0, l.length() - 1);
    return l.split(",");
}

/**
 * 删除功能
 * @param n String: 功能名
 */
void delFunc(String n) {
    stopThread(n); 
    try {
        new java.io.File(getDir() + "/" + n + ".java").delete();
    } catch (Throwable e) {}
    putString("HotPlug", "meta_" + n, ""); 
    String list = getString("HotPlug", "list", "");
    list = list.replace(n + ",", ""); 
    putString("HotPlug", "list", list);
    loopFlags.remove(n);
    scriptLoader.remove(n);
    preProcConfig.remove(n);
    rebuildRegistry();
}

/**
 * 检查是否有回调配置
 * @param cb boolean[]: 回调开关数组
 * @return boolean: 是否存在任一回调
 */
boolean hasCallback(boolean[] cb) {
    for (int i = 0; i < cb.length && i < 7; i++) if (cb[i]) return true;
    return false;
}

/** 设置加载开关 */
void setLoad(String f, boolean on) { 
    putString("HotPlug", "load_" + f, on ? "1" : "0"); 
    String[] m = getMeta(f);
    if (m != null && !hasCallback(new boolean[]{m[2].equals("1"), m[3].equals("1"), m[4].equals("1"), m[5].equals("1"), m[6].equals("1"), m[7].equals("1"), m[8].equals("1")})) {
        if (on) {
            long interval = 0;
            int count = 0;
            try { interval = Long.parseLong(m[12]); } catch (Throwable e) {} // res[12] is interval
            try { count = Integer.parseInt(m[14]); } catch (Throwable e) {} // res[14] is count
            startIndepThread(f, interval, count);
        } else {
            stopThread(f);
        }
    }
}

/** 获取加载状态 */
boolean getLoad(String f) { return getString("HotPlug", "load_" + f, "0").equals("1"); }

/** 设置运行许可 */
void setRun(String f, boolean on) { 
    putString("HotPlug", "run_" + f, on ? "1" : "0"); 
}

/** 获取运行状态 */
boolean getRun(String f) { return getString("HotPlug", "run_" + f, "0").equals("1"); }

/** 设置群限开关 */
void setGrp(String f, String g, boolean on) { 
    putString("HotPlug", "grp_" + f + "_" + g, on ? "1" : "0"); 
}

/** 获取群限状态 */
boolean getGrp(String f, String g) { return getString("HotPlug", "grp_" + f + "_" + g, "0").equals("1"); }

/** 设置循环开关 */
void setLoop(String f, boolean on) { 
    putString("HotPlug", "loop_" + f, on ? "1" : "0");
    loopFlags.put(f, new Boolean(on)); 
}

/** 获取循环状态 */
boolean getLoop(String f) { 
    Boolean b = (Boolean)loopFlags.get(f);
    if (b != null) return b.booleanValue();
    return getString("HotPlug", "loop_" + f, "0").equals("1");
}

/**
 * 格式化时间显示
 * @param raw String: 原始时间串
 * @return String: 格式化后的时间串
 */
String formatTimeDisplay(String raw) {
    if (raw == null || raw.equals("")) return "";
    String n = raw.replaceAll("[^0-9]", "");
    if (n.length() < 4) return raw;
    while (n.length() < 10) n = "0" + n; 
    int d = Integer.parseInt(n.substring(0, 2));
    int h = Integer.parseInt(n.substring(2, 4));
    int min = Integer.parseInt(n.substring(4, 6));
    int s = Integer.parseInt(n.substring(6, 8));
    int ms = Integer.parseInt(n.substring(8, 10));
    StringBuilder sb = new StringBuilder();
    if (d > 0) sb.append(d).append(":");
    if (h > 0 || sb.length() > 0) sb.append(h).append(":");
    if (min > 0 || sb.length() > 0) sb.append(min).append(":");
    sb.append(s).append(":").append(ms);
    return sb.toString();
}

/**
 * 获取下一次执行时间戳
 * @param cfg String: 定时配置字符串
 * @return long: 下次执行的毫秒时间戳
 */
long getNextScheduleTime(String cfg) {
    if (cfg == null || cfg.equals("")) return 0;
    try {
        String raw = cfg.trim();
        if (!raw.contains(" ")) {
            String num = raw.replaceAll("[^0-9]", "");
            if (num.length() >= 4) {
                Calendar target = Calendar.getInstance();
                target.set(Calendar.SECOND, 0);
                target.set(Calendar.MILLISECOND, 0);
                
                int h, m, s;
                if (num.length() >= 6) {
                     h = Integer.parseInt(num.substring(0, 2));
                     m = Integer.parseInt(num.substring(2, 4));
                     s = Integer.parseInt(num.substring(4, 6));
                } else {
                     h = Integer.parseInt(num.substring(0, 2));
                     m = Integer.parseInt(num.substring(2, 4));
                     s = 0;
                }
                
                target.set(Calendar.HOUR_OF_DAY, h);
                target.set(Calendar.MINUTE, m);
                target.set(Calendar.SECOND, s);
                target.set(Calendar.MILLISECOND, 0);
                
                Calendar now = Calendar.getInstance();
                if (target.before(now) || target.equals(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1);
                }
                return target.getTimeInMillis();
            }
        }

        String[] parts = raw.split("\\s+");
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        
        if (parts.length >= 3) {
            int h = Integer.parseInt(parts[parts.length-3]);
            int m = Integer.parseInt(parts[parts.length-2]);
            int s = Integer.parseInt(parts[parts.length-1]);
            target.set(Calendar.HOUR_OF_DAY, h);
            target.set(Calendar.MINUTE, m);
            target.set(Calendar.SECOND, s);
            target.set(Calendar.MILLISECOND, 0);
            
            if (parts.length == 3) {
                if (target.before(now) || target.equals(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1);
                }
            } else if (parts.length == 4) {
                String flag = parts[0].toLowerCase();
                if (flag.startsWith("w")) {
                    int dayOfWeek = Integer.parseInt(flag.substring(1));
                    target.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                    if (target.before(now) || target.equals(now)) {
                        target.add(Calendar.WEEK_OF_YEAR, 1);
                    }
                } else {
                    int dayOfMonth = Integer.parseInt(flag);
                    target.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    if (target.before(now) || target.equals(now)) {
                        target.add(Calendar.MONTH, 1);
                    }
                }
            }
            return target.getTimeInMillis();
        }
        return 0;
    } catch (Throwable e) {
        return 0;
    }
}

/**
 * 格式化倒计时
 * @param ms long: 毫秒数
 * @return String: 友好的时间描述
 */
String formatCountdown(long ms) {
    if (ms <= 0) return "立即执行";
    long totalSeconds = ms / 1000;
    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    
    StringBuilder sb = new StringBuilder();
    if (days > 0) sb.append(days).append("天");
    if (hours > 0) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(hours).append("时");
    }
    if (minutes > 0) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(minutes).append("分");
    }
    if (seconds > 0) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(seconds).append("秒");
    }
    if (sb.length() == 0) return "立即执行";
    return sb.toString();
}

/**
 * 格式化日程配置
 * @param cfg String: 配置字符串
 * @return String: 人类可读的描述
 */
String formatSchedule(String cfg) {
    if (cfg == null || cfg.equals("")) return "";
    try {
        String raw = cfg.trim();
        if (!raw.contains(" ")) {
            String num = raw.replaceAll("[^0-9]", "");
            if (num.length() >= 4) {
                 int h, m, s;
                 if (num.length() >= 6) {
                     h = Integer.parseInt(num.substring(0, 2));
                     m = Integer.parseInt(num.substring(2, 4));
                     s = Integer.parseInt(num.substring(4, 6));
                 } else {
                     h = Integer.parseInt(num.substring(0, 2));
                     m = Integer.parseInt(num.substring(2, 4));
                     s = 0;
                 }
                 return "每日 " + h + ":" + m + ":" + s;
            }
        }
        
        String[] parts = raw.split("\\s+");
        if (parts.length == 3) {
            return "每日 " + parts[0] + ":" + parts[1] + ":" + parts[2];
        } else if (parts.length == 4) {
            String flag = parts[0].toLowerCase();
            String time = parts[1] + ":" + parts[2] + ":" + parts[3];
            if (flag.startsWith("w")) {
                String[] weekDays = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                int day = Integer.parseInt(flag.substring(1));
                if (day >= 1 && day <= 7) return "每周" + weekDays[day] + " " + time;
                else return "每周" + day + " " + time;
            } else {
                return "每月" + flag + "日 " + time;
            }
        }
    } catch (Throwable e) {}
    return cfg;
}

/**
 * 启动独立执行线程
 * 负责定时任务、循环任务和单次初始化任务的调度
 * @param func String: 功能名
 * @param interval long: 循环间隔
 * @param maxCount int: 最大执行次数
 */
void startIndepThread(final String func, final long interval, final int maxCount) {
    Thread existing = (Thread)runningThreads.get(func);
    if (existing != null && existing.isAlive()) {
        existing.interrupt();
        try { existing.join(1000); } catch (Throwable e) {}
    }
    
    loopFlags.put(func, new Boolean(getLoop(func)));
    
    final Runnable task = new Runnable() {
        public void run() {
            traceLog("function_log", "[startIndepThread]" + func);
            int executedCount = 0;
            String[] meta = getMeta(func);
            // res[11] is time string
            boolean isScheduled = (meta != null && meta[11] != null && !meta[11].equals(""));
            boolean isLooping = getLoop(func);
            boolean runOnce = !isScheduled && !isLooping;

            while (!Thread.interrupted()) {
                try {
                    if (!getLoad(func)) {
                        traceLog("function_log", "[Stop] " + func + " 总开关关闭");
                        break;
                    }
                    if (!getRun(func) && !runOnce) {
                        Thread.sleep(2000); 
                        continue;
                    }
                    if (!isScheduled && !isLooping && !runOnce) break;
                    
                    if (!isScheduled && maxCount > 0 && executedCount >= maxCount) {
                        traceLog("function_log", "[Complete] " + func + " 次数达标");
                        break;
                    }
                    
                    if (isScheduled) {
                        long now = System.currentTimeMillis();
                        long nextTime = getNextScheduleTime(meta[11]);
                        
                        if (nextTime <= now) {
                            Thread.sleep(60000);
                            continue;
                        }
                        
                        long waitTime = nextTime - now;
                        traceLog("function_log", "[Schedule] " + func + " 等待: " + (waitTime/1000) + "秒");
                        
                        long blocks = waitTime / 2000;
                        long remain = waitTime % 2000;
                        
                        for(long i=0; i<blocks; i++) {
                            if (Thread.interrupted() || !getLoad(func)) throw new InterruptedException();
                            Thread.sleep(2000);
                        }
                        if(remain > 0) Thread.sleep(remain);
                        
                    } else if (isLooping) {
                        long sleepTime = interval > 0 ? interval : 5000;
                        if (sleepTime < 500) sleepTime = 500; 
                        if (executedCount > 0) Thread.sleep(sleepTime); 
                        
                        if (!getLoad(func) || !getRun(func) || !getLoop(func)) continue;
                    }
                    
                    String code = readCodeFile(func);
                    if (!code.equals("")) {
                        this.interpreter.set("qq", myUin);
                        this.interpreter.set("pluginPath", pluginPath);
                        this.interpreter.set("this", this);
                        this.interpreter.set("qun", ""); 
                        this.interpreter.set("uin", "");
                        
                        scriptLoader.loadAndExecute(func, this.interpreter); 
                        executedCount++; 
                        lastExecTime.put(func, System.currentTimeMillis());
                        
                        traceLog("function_log", "[Execute] " + func + " 第" + executedCount + "次");
                    }
                    
                    if (runOnce) break; 
                    
                    if (isScheduled) {
                        Thread.sleep(2000); 
                    }
                    
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    traceLog("function_log", "[Error] " + func + ": " + e);
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { break; }
                }
            }
            traceLog("function_log", "[Finish] " + func);
            runningThreads.remove(func);
        }
    };
    
    ThreadPool.execute(task);
    Thread wrapper = new Thread(task);
    wrapper.setName("HotPlug_" + func);
    runningThreads.put(func, wrapper);
}

/** 停止线程 */
void stopThread(String func) {
    Thread t = (Thread)runningThreads.get(func);
    if (t != null) { 
        t.interrupt(); 
        try { t.join(1000); } catch (Throwable e) {}
        runningThreads.remove(func); 
    }
}

/** 停止所有线程 */
void stopAllThreads() {
    Iterator it = runningThreads.keySet().iterator();
    while (it.hasNext()) {
        Thread t = (Thread)runningThreads.get((String)it.next());
        if (t != null) {
            t.interrupt();
            try { t.join(1000); } catch (Throwable e) {}
        }
    runningThreads.clear();
    }
}

/**
 * 执行功能脚本
 * @param func String: 功能名
 * @param data Object: 触发数据
 * @param type int: 触发类型 (1:Msg, 2:Join, 3:Quit, 4:Shut, 5:Chat, 6:Pai, 7:Pre)
 */
void execFunc(String func, Object data, int type) {
    try {
        String[] m = getMeta(func);
        if (m == null) return;
        
        int idx = type + 1;
        if (idx >= m.length || !m[idx].equals("1")) return;
        if (!getRun(func)) return;
        
        String currentGroupId = "";
        if (m[9].equals("1")) { // m[9] is GrpLimit
            String gid = "";
            if (type == 1 && data != null) {
                try { gid = data.peerUin; } catch (Throwable e) {}
            } else if (type >= 2 && type <= 6) {
                try {
                    if (data instanceof String) gid = (String)data;
                    else if (data instanceof Object[]) gid = (String)((Object[])data)[0];
                    else if (data instanceof String[]) gid = ((String[])data)[0];
                } catch (Throwable e) {}
            }
            if (gid.equals("") || !getGrp(func, gid)) return;
            currentGroupId = gid;
        }
        
        String code = readCodeFile(func);
        if (code.equals("")) return;
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
        lastExecTime.put(func, System.currentTimeMillis());
    } catch (Throwable e) {
        traceLog("function_log", "[execFunc]" + func + ":" + e);
    }
}

/**
 * 事件分发
 * @param data Object: 事件数据
 * @param type int: 事件类型
 */
void dispatchEvent(Object data, int type) {
    Integer key = new Integer(type);
    if (!eventRegistry.containsKey(key)) return;
    ArrayList list = (ArrayList) eventRegistry.get(key);
    if (list == null || list.size() == 0) return;
    for (int i = 0; i < list.size(); i++) {
        execFunc((String)list.get(i), data, type);
    }
}

/** 预设：消息 */
String[][] getPresetsCategory1() {
    return new String[][]{
        {"发文本", "sendMsg(qun, \"[atUin=\"+uin+\"]内容\", type);"},
        {"发图片", "sendPic(qun, pluginPath+\"/test.jpg\", 2);"},
        {"发语音", "sendPtt(qun, pluginPath+\"/test.amr\", 2);"},
        {"发卡片", "sendCard(qun, \"{\\\"app\\\":\\\"miniapp\\\"}\", 2);"},
        {"发文件", "sendFile(qun, pluginPath+\"/file.txt\", 2);"},
        {"发视频", "sendVideo(qun, pluginPath+\"/video.mp4\", 2);"},
        {"引用回复", "sendReplyMsg(qun, msgId, \"回复\", type);"},
        {"撤回消息", "recallMsg(type, qun, msgId);"},
        {"拍一拍", "sendPai(uin, qun, type);"}
    };
}

/** 预设：好友 */
String[][] getPresetsCategory2() {
    return new String[][]{
        {"获取好友", "List list = getAllFriend();"},
        {"是否好友", "boolean flag = isFriend(uin);"},
        {"点赞", "sendZan(uin, 10);"},
        {"Uin转Uid", "String uid = getUidFromUin(uin);"},
        {"Uid转Uin", "String uin2 = getUinFromUid(uid);"}
    };
}

/** 预设：群管 */
String[][] getPresetsCategory3() {
    return new String[][]{
        {"群列表", "List groups = getGroupList();"},
        {"成员列表", "List members = getGroupMemberList(qun);"},
        {"禁言列表", "List forbids = getProhibitList(qun);"},
        {"群信息", "TroopInfo info = getGroupInfo(qun);"},
        {"成员信息", "MemberInfo minfo = getMemberInfo(qun, uin);"},
        {"禁言", "shutUp(qun, uin, 600);"},
        {"全员禁言", "shutUpAll(qun, true);"},
        {"踢人", "kickGroup(qun, uin, false);"},
        {"设管理", "setGroupAdmin(qun, uin, true);"},
        {"改头衔", "setGroupMemberTitle(qun, uin, \"头衔\");"},
        {"改名片", "changeMemberName(qun, uin, \"名片\");"},
        {"查是否禁言", "boolean shut = isShutUp(qun);"},
        {"群打卡", "clockIn(qun);"}
    };
}

/**
 * 动画：弹窗进入
 */
void animateDialogIn(final android.app.Dialog d) {
    try {
        Window w = d.getWindow();
        View v = w.getDecorView();
        v.setAlpha(0f);
        v.setScaleX(0.95f);
        v.setScaleY(0.95f);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(v, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(v, "scaleY", 0.95f, 1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animatorSet.start();
    } catch (Throwable e) {}
}

/**
 * 动画：弹窗退出
 */
void animateDialogOut(final android.app.Dialog d, final Runnable onEnd) {
    try {
        Window w = d.getWindow();
        View v = w.getDecorView();
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.95f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
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

/**
 * 构建表单：名称
 */
EditText addNameInput(Activity a, LinearLayout parent, String name, int bgColor) {
    EditText et = makeInput(a, "功能名", bgColor);
    if (name != null) et.setText(name);
    parent.addView(et);
    return et;
}

/**
 * 构建表单：代码
 */
EditText addCodeInput(Activity a, LinearLayout parent, String code, boolean isFile, final boolean[] isFileState, final int bgColor) {
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
    tipFile.setTextColor(Color.parseColor("#999999"));
    tipFile.setPadding(dp(a, 6), 0, 0, 0);
    rowFile.addView(tipFile);

    final EditText et = makeInput(a, isFile ? "文件绝对路径" : "代码内容", bgColor);
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

/**
 * 构建表单：预设代码
 */
void addPresetRows(Activity a, LinearLayout parent, final EditText et) {
    TextView pt = new TextView(a);
    pt.setText("快捷填入:");
    pt.setTextSize(10);
    pt.setTextColor(Color.parseColor("#888888"));
    pt.setPadding(0, dp(a, 4), 0, 0);
    parent.addView(pt);
    
    addPresetRow(a, parent, et, getPresetsCategory1(), Color.parseColor("#3B71FE"));
    addPresetRow(a, parent, et, getPresetsCategory2(), Color.parseColor("#00C853"));
    addPresetRow(a, parent, et, getPresetsCategory3(), Color.parseColor("#FF9800"));
}

/**
 * 获取回调详细信息
 */
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

/**
 * 更新动态tips显示
 */
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

/**
 * 构建表单：回调选择
 */
TextView[] addChipRows(Activity a, LinearLayout parent, final boolean[] cks, final FormComponents fc) {
    TextView tipsHeader = new TextView(a);
    tipsHeader.setText("回调详情:");
    tipsHeader.setTextSize(11);
    tipsHeader.setTextColor(Color.parseColor("#666666"));
    tipsHeader.setPadding(0, dp(a, 10), 0, dp(a, 6));
    parent.addView(tipsHeader);
    
    fc.dynamicTips = new TextView(a);
    fc.dynamicTips.setTextSize(10);
    fc.dynamicTips.setTextColor(Color.parseColor("#3B71FE"));
    fc.dynamicTips.setPadding(dp(a, 8), dp(a, 8), dp(a, 8), dp(a, 8));
    fc.dynamicTips.setBackground(roundRect(Color.parseColor("#F0F5FF"), dp(a, 6)));
    fc.dynamicTips.setLineSpacing(dp(a, 2), 1.0f);
    parent.addView(fc.dynamicTips);
    
    updateDynamicTips(fc, cks);
    
    TextView sub = new TextView(a);
    sub.setText("挂载回调(多选):");
    sub.setTextSize(11);
    sub.setTextColor(Color.parseColor("#666666"));
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

/**
 * 更新预处理控件可见性
 */
void updatePreprocVisibility(FormComponents fc, boolean visible) {
    if (fc.preprocContainer != null) {
        fc.preprocContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            fc.preprocContainer.setAlpha(0f);
            fc.preprocContainer.animate().alpha(1f).setDuration(300).start();
        }
    }
}

/**
 * 根据选中的样式类型返回对应的 tips 说明
 * 只在选中非“无”时调用
 */
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

/**
 * 构建表单：预处理设置
 */
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
    styleTitle.setTextColor(Color.parseColor("#666666"));
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
    
    fc.prePreview = new TextView(a);
    fc.prePreview.setTextSize(10);
    fc.prePreview.setTextColor(Color.parseColor("#3B71FE"));
    fc.prePreview.setPadding(dp(a, 8), dp(a, 8), dp(a, 8), dp(a, 8));
    fc.prePreview.setBackground(roundRect(Color.parseColor("#F0F5FF"), dp(a, 6)));
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
    detailedTips.setTextColor(Color.parseColor("#888888"));
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
    tailTitle.setTextColor(Color.parseColor("#666666"));
    tailTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2.0f));
    titleRow.addView(tailTitle);
    
    TextView sendTitle = new TextView(a);
    sendTitle.setText("连发");
    sendTitle.setTextSize(11);
    sendTitle.setTextColor(Color.parseColor("#666666"));
    sendTitle.setGravity(Gravity.CENTER);
    sendTitle.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 50), -2));
    titleRow.addView(sendTitle);
    
    View spacer = new View(a);
    spacer.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 8), 0));
    titleRow.addView(spacer);
    
    TextView concatTitle = new TextView(a);
    concatTitle.setText("拼接");
    concatTitle.setTextSize(11);
    concatTitle.setTextColor(Color.parseColor("#666666"));
    concatTitle.setGravity(Gravity.CENTER);
    concatTitle.setLayoutParams(new LinearLayout.LayoutParams(dp(a, 50), -2));
    titleRow.addView(concatTitle);
    
    LinearLayout inputRow = new LinearLayout(a);
    inputRow.setOrientation(LinearLayout.HORIZONTAL);
    inputRow.setGravity(Gravity.CENTER_VERTICAL);
    inputRow.setPadding(0, dp(a, 4), 0, 0);
    container.addView(inputRow);
    
    fc.etPreTail = makeInput(a, "例如: 前缀 msg 后缀", Color.parseColor("#F7F8FA"));
    if (preTail != null) fc.etPreTail.setText(preTail);
    fc.etPreTail.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 2.0f));
    inputRow.addView(fc.etPreTail);
    
    fc.etRepeatSend = makeTinyInput(a, "次", Color.parseColor("#F7F8FA"));
    if (repeatSend > 0) fc.etRepeatSend.setText(String.valueOf(repeatSend));
    LinearLayout.LayoutParams lpRs = new LinearLayout.LayoutParams(dp(a, 50), dp(a, 32));
    lpRs.setMargins(dp(a, 8), 0, 0, 0);
    fc.etRepeatSend.setLayoutParams(lpRs);
    inputRow.addView(fc.etRepeatSend);
    
    fc.etRepeatConcat = makeTinyInput(a, "次", Color.parseColor("#F7F8FA"));
    if (repeatConcat > 0) fc.etRepeatConcat.setText(String.valueOf(repeatConcat));
    LinearLayout.LayoutParams lpRc = new LinearLayout.LayoutParams(dp(a, 50), dp(a, 32));
    lpRc.setMargins(dp(a, 8), 0, 0, 0);
    fc.etRepeatConcat.setLayoutParams(lpRc);
    inputRow.addView(fc.etRepeatConcat);
    
    TextView inputTips = new TextView(a);
    inputTips.setText("提示: msg会被替换为实际消息内容\n连发: 重复发送N次  拼接: 内容重复N次后再发");
    inputTips.setTextSize(9);
    inputTips.setTextColor(Color.parseColor("#AAAAAA"));
    inputTips.setPadding(0, dp(a, 4), 0, 0);
    inputTips.setLineSpacing(dp(a, 2), 1.0f);
    container.addView(inputTips);
}

/**
 * 构建表单：循环设置
 */
FormComponents addLoopRow(Activity a, LinearLayout parent, boolean isLoop, long interval, int count, final boolean[] cks, final int bgColor) {
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
    lblInterval.setTextColor(Color.parseColor("#888888"));
    leftCol.addView(lblInterval);
    
    fc.etInterval = makeInput(a, "5000", bgColor);
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
    lblCount.setTextColor(Color.parseColor("#888888"));
    rightCol.addView(lblCount);
    
    fc.etCount = makeInput(a, "0", bgColor);
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

/**
 * 构建表单：定时设置
 */
EditText addTimeRow(Activity a, LinearLayout parent, String timeVal) {
    TextView lblTime = new TextView(a);
    lblTime.setText("定时(可选):");
    lblTime.setTextSize(11);
    lblTime.setTextColor(Color.parseColor("#666666"));
    lblTime.setPadding(0, dp(a, 10), 0, dp(a, 4));
    parent.addView(lblTime);
    
    LinearLayout timeRow = new LinearLayout(a);
    timeRow.setOrientation(LinearLayout.HORIZONTAL);
    parent.addView(timeRow);
    
    final EditText etTime = makeInput(a, "时 分 秒 或 w1 时 分 秒 或 1 时 分 秒", Color.parseColor("#F7F8FA"));
    if (timeVal != null) etTime.setText(timeVal);
    etTime.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
    timeRow.addView(etTime);
    
    TextView btnSchedule = new TextView(a);
    btnSchedule.setText("📅");
    btnSchedule.setTextSize(16);
    btnSchedule.setPadding(dp(a, 12), 0, dp(a, 8), 0);
    btnSchedule.setBackground(makeFeedbackBg(Color.parseColor("#E8F0FE"), adjustColor(Color.parseColor("#E8F0FE"), 0.9f), dp(a, 6)));
    btnSchedule.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { showSchedulePicker(a, etTime); }
    });
    timeRow.addView(btnSchedule);
    
    TextView btnTime = new TextView(a);
    btnTime.setText("⏱");
    btnTime.setTextSize(16);
    btnTime.setPadding(dp(a, 8), 0, dp(a, 12), 0);
    btnTime.setBackground(makeFeedbackBg(Color.parseColor("#E8F0FE"), adjustColor(Color.parseColor("#E8F0FE"), 0.9f), dp(a, 6)));
    btnTime.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { showTimePicker(a, etTime); }
    });
    timeRow.addView(btnTime);
    
    return etTime;
}

/**
 * 构建表单：预设按钮行
 */
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

/**
 * 显示时间选择器
 */
void showTimePicker(Activity a, final EditText target) {
    a.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final android.app.Dialog d = new android.app.Dialog(a);
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
                card.setBackground(roundRect(Color.parseColor("#FFFFFF"), dp(a, 16)));
                card.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 20));
                outer.addView(card);
                TextView title = new TextView(a);
                title.setText("设置执行时间 (日:时:分:秒)");
                title.setTextSize(17);
                card.addView(title);
                final LinearLayout container = new LinearLayout(a);
                container.setOrientation(LinearLayout.VERTICAL);
                card.addView(container);
                final String[] val = {""};
                String current = target.getText().toString().replaceAll("[^0-9]", "");
                while (current.length() < 10) current = "0" + current;
                val[0] = current;
                final int[] mode = {0};
                final String[] names = {"键盘输入", "钟表选择", "快捷换算", "单项输入"};
                final Runnable[] build = new Runnable[1];
                build[0] = new Runnable() {
                    public void run() {
                        container.removeAllViews();
                        if (mode[0] == 0) {
                            EditText e = makeInput(a, "日:时:分:秒:毫秒", Color.parseColor("#F7F8FA"));
                            e.setText(formatTimeDisplay(val[0]));
                            e.addTextChangedListener(new android.text.TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                public void afterTextChanged(android.text.Editable s) {
                                    String n = s.toString().replaceAll("[^0-9]", "");
                                    while (n.length() < 10) n = "0" + n;
                                    val[0] = n;
                                }
                            });
                            container.addView(e);
                        } else if (mode[0] == 1) { 
                            HorizontalScrollView hs = new HorizontalScrollView(a);
                            LinearLayout row = new LinearLayout(a);
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            hs.addView(row);
                            container.addView(hs);
                            String[] lbls = {"日", "时", "分", "秒", "毫秒"};
                            final int[] vs = {0,0,0,0,0};
                            try {
                                vs[0] = Integer.parseInt(val[0].substring(0,2));
                                vs[1] = Integer.parseInt(val[0].substring(2,4));
                                vs[2] = Integer.parseInt(val[0].substring(4,6));
                                vs[3] = Integer.parseInt(val[0].substring(6,8));
                                vs[4] = Integer.parseInt(val[0].substring(8,10));
                            } catch (Throwable e) {}
                            for (int i=0; i<5; i++) {
                                LinearLayout col = new LinearLayout(a);
                                col.setOrientation(LinearLayout.VERTICAL);
                                col.setPadding(dp(a,4),0,dp(a,4),0);
                                TextView l = new TextView(a);
                                l.setText(lbls[i]);
                                l.setTextSize(11);
                                l.setGravity(Gravity.CENTER);
                                col.addView(l);
                                NumberPicker p = new NumberPicker(a);
                                int max = (i==0)?30:(i==1)?23:(i==4)?999:59;
                                p.setMinValue(0);
                                p.setMaxValue(max);
                                p.setValue(vs[i]);
                                final int idx = i;
                                p.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                                    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                                        vs[idx] = newVal;
                                        val[0] = String.format("%02d%02d%02d%02d%03d", vs[0], vs[1], vs[2], vs[3], vs[4]);
                                    }
                                });
                                col.addView(p);
                                row.addView(col);
                            }
                        } else if (mode[0] == 2) { 
                            EditText e = makeInput(a, "输入秒数(如3600=1小时)", Color.parseColor("#F7F8FA"));
                            e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            e.addTextChangedListener(new android.text.TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                                public void afterTextChanged(android.text.Editable s) {
                                    try {
                                        int sec = Integer.parseInt(s.toString());
                                        int h = sec / 3600;
                                        int m = (sec % 3600) / 60;
                                        int sc = sec % 60;
                                        val[0] = String.format("00%02d%02d%02d000", h, m, sc);
                                    } catch (Throwable e) {}
                                }
                            });
                            container.addView(e);
                        } else { 
                            GridLayout grid = new GridLayout(a);
                            grid.setColumnCount(2);
                            container.addView(grid);
                            String[] lbls = {"日:", "时:", "分:", "秒:", "毫秒:"};
                            final EditText[] ets = new EditText[5];
                            for (int i=0; i<5; i++) {
                                TextView l = new TextView(a);
                                l.setText(lbls[i]);
                                grid.addView(l);
                                ets[i] = new EditText(a);
                                ets[i].setHint(i==4?"000":"00");
                                ets[i].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                                ets[i].setTextSize(13);
                                ets[i].setGravity(Gravity.CENTER);
                                ets[i].setBackground(roundRect(Color.parseColor("#F7F8FA"), dp(a,4)));
                                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                                lp.width = dp(a, 60);
                                ets[i].setLayoutParams(lp);
                                grid.addView(ets[i]);
                            }
                            try {
                                ets[0].setText(val[0].substring(0,2));
                                ets[1].setText(val[0].substring(2,4));
                                ets[2].setText(val[0].substring(4,6));
                                ets[3].setText(val[0].substring(6,8));
                                ets[4].setText(val[0].substring(8,10));
                            } catch (Throwable e) {}
                        }
                    }
                };
                build[0].run();
                LinearLayout ctrl = new LinearLayout(a);
                ctrl.setOrientation(LinearLayout.HORIZONTAL);
                ctrl.setPadding(0, dp(a, 16), 0, 0);
                card.addView(ctrl);
                TextView btnMode = new TextView(a);
                btnMode.setText(" " + names[0]);
                btnMode.setTextSize(12);
                btnMode.setTextColor(Color.parseColor("#3B71FE"));
                btnMode.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
                ctrl.addView(btnMode);
                View sp = new View(a);
                sp.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
                ctrl.addView(sp);
                TextView btnOk = new TextView(a);
                btnOk.setText("确定");
                btnOk.setTextSize(14);
                btnOk.setTextColor(Color.WHITE);
                btnOk.setBackground(roundRect(Color.parseColor("#3B71FE"), dp(a, 6)));
                btnOk.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 8));
                ctrl.addView(btnOk);
                btnMode.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mode[0] = (mode[0] + 1) % 4;
                        ((TextView)v).setText(" " + names[mode[0]]);
                        build[0].run();
                    }
                });
                btnOk.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (val[0].replaceAll("[^0-9]", "").length() < 4) {
                            toast("请至少设置时和分");
                            return;
                        }
                        target.setText(formatTimeDisplay(val[0]));
                        animateDialogOut(d, null);
                    }
                });
                d.setContentView(outer);
                d.getWindow().setLayout((int)(a.getResources().getDisplayMetrics().widthPixels * 0.9), -2);
                d.show();
                animateDialogIn(d);
            } catch (Throwable e) {
                traceLog("function_log", "[showTimePicker]" + e);
            }
        }
    });
}

/**
 * 显示日程选择器
 */
void showSchedulePicker(Activity a, final EditText target) {
    a.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final android.app.Dialog d = new android.app.Dialog(a);
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
                card.setBackground(roundRect(Color.parseColor("#FFFFFF"), dp(a, 16)));
                card.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 20));
                outer.addView(card);
                
                TextView title = new TextView(a);
                title.setText("日程配置");
                title.setTextSize(18);
                title.setTypeface(null, Typeface.BOLD);
                title.setTextColor(Color.BLACK);
                card.addView(title);
                
                TextView subtitle = new TextView(a);
                subtitle.setText("配置定时任务的执行时间");
                subtitle.setTextSize(12);
                subtitle.setTextColor(Color.parseColor("#666666"));
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
                
                final TextView modeBtn = new TextView(a);
                modeBtn.setText(" 切换模式: " + modeNames[currentMode[0]]);
                modeBtn.setTextSize(12);
                modeBtn.setTextColor(Color.parseColor("#3B71FE"));
                modeBtn.setPadding(dp(a, 12), dp(a, 8), dp(a, 12), dp(a, 8));
                modeBtn.setBackground(roundRect(Color.parseColor("#F0F5FF"), dp(a, 6)));
                modeRow.addView(modeBtn);
                
                String currentValue = target.getText().toString().trim();
                final String[] currentParts = currentValue.split("\\s+");
                
                final Runnable rebuildUI = new Runnable() {
                    public void run() {
                        container.removeAllViews();
                        
                        if (currentMode[0] == 0) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置每日执行的时间 (时:分:秒)");
                            hint.setTextSize(12);
                            hint.setTextColor(Color.parseColor("#666666"));
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout timeRow = new LinearLayout(a);
                            timeRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(timeRow);
                            
                            final EditText hourInput = makeInput(a, "时", Color.parseColor("#F7F8FA"));
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", Color.parseColor("#F7F8FA"));
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", Color.parseColor("#F7F8FA"));
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(secondInput);
                            
                            if (currentParts.length >= 3) {
                                try {
                                    hourInput.setText(currentParts[0]);
                                    minuteInput.setText(currentParts[1]);
                                    secondInput.setText(currentParts[2]);
                                } catch (Exception e) {}
                            } else {
                                hourInput.setText("12");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                            
                        } else if (currentMode[0] == 1) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置每周执行 (星期 时:分:秒)");
                            hint.setTextSize(12);
                            hint.setTextColor(Color.parseColor("#666666"));
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
                            
                            final EditText hourInput = makeInput(a, "时", Color.parseColor("#F7F8FA"));
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", Color.parseColor("#F7F8FA"));
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", Color.parseColor("#F7F8FA"));
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(secondInput);
                            
                            if (currentParts.length >= 4 && currentParts[0].startsWith("w")) {
                                try {
                                    int day = Integer.parseInt(currentParts[0].substring(1));
                                    if (day >= 1 && day <= 7) {
                                        daySpinner.setSelection(day - 1);
                                    }
                                    hourInput.setText(currentParts[1]);
                                    minuteInput.setText(currentParts[2]);
                                    secondInput.setText(currentParts[3]);
                                } catch (Exception e) {}
                            } else {
                                daySpinner.setSelection(0);
                                hourInput.setText("12");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                            
                        } else if (currentMode[0] == 2) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置每月执行 (日期 时:分:秒)");
                            hint.setTextSize(12);
                            hint.setTextColor(Color.parseColor("#666666"));
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout monthRow = new LinearLayout(a);
                            monthRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(monthRow);
                            
                            final EditText dayInput = makeInput(a, "日期(1-31)", Color.parseColor("#F7F8FA"));
                            dayInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            dayInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            monthRow.addView(dayInput);
                            
                            LinearLayout timeRow = new LinearLayout(a);
                            timeRow.setOrientation(LinearLayout.HORIZONTAL);
                            timeRow.setPadding(dp(a, 8), 0, 0, 0);
                            monthRow.addView(timeRow);
                            
                            final EditText hourInput = makeInput(a, "时", Color.parseColor("#F7F8FA"));
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", Color.parseColor("#F7F8FA"));
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            timeRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", Color.parseColor("#F7F8FA"));
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            timeRow.addView(secondInput);
                            
                            if (currentParts.length >= 4) {
                                try {
                                    dayInput.setText(currentParts[0]);
                                    hourInput.setText(currentParts[1]);
                                    minuteInput.setText(currentParts[2]);
                                    secondInput.setText(currentParts[3]);
                                } catch (Exception e) {}
                            } else {
                                dayInput.setText("1");
                                hourInput.setText("00");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                            
                        } else if (currentMode[0] == 3) { 
                            TextView hint = new TextView(a);
                            hint.setText("设置间隔执行 (时:分:秒)");
                            hint.setTextSize(12);
                            hint.setTextColor(Color.parseColor("#666666"));
                            hint.setPadding(0, 0, 0, dp(a, 8));
                            container.addView(hint);
                            
                            LinearLayout intervalRow = new LinearLayout(a);
                            intervalRow.setOrientation(LinearLayout.HORIZONTAL);
                            container.addView(intervalRow);
                            
                            final EditText hourInput = makeInput(a, "时", Color.parseColor("#F7F8FA"));
                            hourInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            hourInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            intervalRow.addView(hourInput);
                            
                            TextView colon1 = new TextView(a);
                            colon1.setText(":");
                            colon1.setTextSize(14);
                            colon1.setGravity(Gravity.CENTER);
                            colon1.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            intervalRow.addView(colon1);
                            
                            final EditText minuteInput = makeInput(a, "分", Color.parseColor("#F7F8FA"));
                            minuteInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            minuteInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            intervalRow.addView(minuteInput);
                            
                            TextView colon2 = new TextView(a);
                            colon2.setText(":");
                            colon2.setTextSize(14);
                            colon2.setGravity(Gravity.CENTER);
                            colon2.setPadding(dp(a, 4), 0, dp(a, 4), 0);
                            intervalRow.addView(colon2);
                            
                            final EditText secondInput = makeInput(a, "秒", Color.parseColor("#F7F8FA"));
                            secondInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                            secondInput.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                            intervalRow.addView(secondInput);
                            
                            if (currentParts.length >= 3) {
                                try {
                                    hourInput.setText(currentParts[0]);
                                    minuteInput.setText(currentParts[1]);
                                    secondInput.setText(currentParts[2]);
                                } catch (Exception e) {}
                            } else {
                                hourInput.setText("01");
                                minuteInput.setText("00");
                                secondInput.setText("00");
                            }
                        }
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
                
                TextView cancelBtn = new TextView(a);
                cancelBtn.setText("取消");
                cancelBtn.setTextSize(14);
                cancelBtn.setTextColor(Color.parseColor("#666666"));
                cancelBtn.setBackground(roundRect(Color.parseColor("#F5F5F5"), dp(a, 6)));
                cancelBtn.setPadding(dp(a, 24), dp(a, 12), dp(a, 24), dp(a, 12));
                cancelBtn.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        animateDialogOut(d, null);
                    }
                });
                btnRow.addView(cancelBtn);
                
                TextView okBtn = new TextView(a);
                okBtn.setText("确定");
                okBtn.setTextColor(Color.WHITE);
                okBtn.setTextSize(14);
                okBtn.setGravity(Gravity.CENTER);
                okBtn.setBackground(roundRect(Color.parseColor("#3B71FE"), dp(a, 6)));
                okBtn.setPadding(dp(a, 24), dp(a, 12), dp(a, 24), dp(a, 12));
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
                            
                            result.append(hour).append(" ").append(minute).append(" ").append(second);
                            
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
                            
                            result.append("w").append(dayIndex).append(" ")
                                  .append(hour).append(" ").append(minute).append(" ").append(second);
                            
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
                            
                            result.append(day).append(" ")
                                  .append(hour).append(" ").append(minute).append(" ").append(second);
                            
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
                            
                            result.append(hour).append(" ").append(minute).append(" ").append(second);
                        }
                        
                        target.setText(result.toString());
                        animateDialogOut(d, null);
                    }
                });
                
                d.setContentView(outer);
                d.getWindow().setLayout((int)(a.getResources().getDisplayMetrics().widthPixels * 0.9), -2);
                d.show();
                animateDialogIn(d);
            } catch (Throwable e) {
                traceLog("function_log", "[showSchedulePicker]" + e);
            }
        }
    });
}

/**
 * 临时测试执行
 */
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

/**
 * 显示编辑弹窗
 * 复用 FormComponents 和 saveFunc，并正确处理 getMeta 的 String[20] 索引
 */
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
        try { interval = Long.parseLong(m[12]); } catch (Throwable e) {}
        try { loopCount = Integer.parseInt(m[14]); } catch (Throwable e) {}
        try { repeatSend = Integer.parseInt(m[17]); } catch (Throwable e) {}
        try { repeatConcat = Integer.parseInt(m[18]); } catch (Throwable e) {}
        
        try {
            JSONObject jo = new JSONObject(getString("HotPlug", "meta_" + func, ""));
            preType = jo.optInt("pt");
            preTail = jo.optString("tail");
        } catch (Throwable e) {}
        
        final boolean[] cks = {
            m[2].equals("1"), m[3].equals("1"), m[4].equals("1"), 
            m[5].equals("1"), m[6].equals("1"), m[7].equals("1"), 
            m[8].equals("1"), hasGrp
        };
        
        final android.app.Dialog d = new android.app.Dialog(a);
        d.requestWindowFeature(1);
        d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        
        Window window = d.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.CENTER;
        int screenWidth = a.getResources().getDisplayMetrics().widthPixels;
        params.width = (int)(screenWidth * 0.92);
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
        card.setBackground(roundRect(Color.parseColor("#FFFFFF"), dp(a, 12)));
        card.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
        sc.addView(card);
        
        TextView t = new TextView(a);
        t.setText("编辑:" + func);
        t.setTextSize(16);
        t.setTextColor(Color.BLACK);
        card.addView(t);
        
        final EditText etName = addNameInput(a, card, func, Color.parseColor("#F7F8FA"));
        final EditText etCode = addCodeInput(a, card, isFile ? m[10] : readCodeFile(func), isFile, isFileState, Color.parseColor("#F7F8FA"));
        addPresetRows(a, card, etCode);
        
        TextView tips = new TextView(a);
        tips.setText("变量:qun群号 uinQQ号 msg消息内容 msgId消息ID type类型(1私聊2群聊) operator操作者 time禁言秒数");
        tips.setTextSize(9);
        tips.setTextColor(Color.parseColor("#AAAAAA"));
        tips.setPadding(0, dp(a, 4), 0, 0);
        card.addView(tips);
        
        final FormComponents fc = new FormComponents();
        final TextView[] chips = addChipRows(a, card, cks, fc);
        
        addPreprocRow(a, card, fc, preType, preTail, repeatSend, repeatConcat);
        updatePreprocVisibility(fc, cks[6]);
        
        FormComponents loopFc = addLoopRow(a, card, isLoop, interval, loopCount, cks, Color.parseColor("#F7F8FA"));
        fc.etInterval = loopFc.etInterval;
        fc.etCount = loopFc.etCount;
        fc.chipLoop = loopFc.chipLoop;
        fc.loopSettings = loopFc.loopSettings;
        
        final EditText etTime = addTimeRow(a, card, timeVal);
        
        LinearLayout btnRow = new LinearLayout(a);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(a, 12), 0, 0);
        card.addView(btnRow);
        
        TextView sv = makeBtn(a, "保存", Color.WHITE, Color.parseColor("#3B71FE"));
        sv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        btnRow.addView(sv);
        
        TextView tst = makeBtn(a, "测试", Color.parseColor("#666666"), Color.parseColor("#F5F5F5"));
        LinearLayout.LayoutParams lpTst = new LinearLayout.LayoutParams(0, -2, 1.0f);
        lpTst.setMargins(dp(a, 6), 0, dp(a, 6), 0);
        tst.setLayoutParams(lpTst);
        btnRow.addView(tst);
        
        TextView cn = makeBtn(a, "取消", Color.parseColor("#666666"), Color.parseColor("#F5F5F5"));
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
                    cks[i] = (chips[i].getCurrentTextColor() == Color.WHITE);
                    if (cks[i]) hasCb = true;
                }
                cks[7] = (chips[7].getCurrentTextColor() == Color.WHITE);
                
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
                        if (i < fc.preTypeChips.length && fc.preTypeChips[i].getCurrentTextColor() == Color.WHITE) {
                            preTypeVal = i;
                            break;
                        }
                    }
                    preTailVal = fc.etPreTail.getText().toString();
                    try { repeatSendVal = Integer.parseInt(fc.etRepeatSend.getText().toString()); } catch (Throwable e) {}
                    try { repeatConcatVal = Integer.parseInt(fc.etRepeatConcat.getText().toString()); } catch (Throwable e) {}
                }
                
                String rawTime = etTime.getText().toString().trim();
                
                if (!newName.equals(func)) delFunc(func);
                
                saveFunc(newName, ct, isFileState[0], 
                    new boolean[]{cks[0], cks[1], cks[2], cks[3], cks[4], cks[5], cks[6]}, 
                    cks[7], rawTime, intervalVal, 
                    fc.chipLoop.getCurrentTextColor() == Color.WHITE, countVal, preTypeVal, preTailVal, repeatSendVal, repeatConcatVal);
                
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
        d.getWindow().setLayout((int)(screenWidth * 0.92), -2);
        WindowManager.LayoutParams finalParams = d.getWindow().getAttributes();
        finalParams.gravity = Gravity.CENTER;
        finalParams.verticalMargin = 0.0f;
        d.getWindow().setAttributes(finalParams);
        d.show();
        animateDialogIn(d);
        
    } catch (Throwable e) {
        isAdding = false;
        toast("打开编辑失败: " + e.getMessage());
    }
}

/**
 * 显示热插拔功能管理主界面
 * <p>包含功能列表展示、状态刷新、新建功能入口以及侧滑删除等交互逻辑。</p>
 *
 * @param ft    功能类型标识
 * @param gid   当前群组ID (Group ID)
 * @param uname 当前用户名称
 */
public void showHotPlugMain(int ft, String gid, String uname) {
    traceLog("function_log", "[showHotPlugMain] 开始执行");
    final Activity a = getNowActivity();
    if (a == null) return;

    final String g = gid != null ? gid : "";
    final String gn = uname;
    isAdding = false;

    a.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final Dialog d = new Dialog(a);
                d.requestWindowFeature(1);
                d.getWindow().setBackgroundDrawable(new ColorDrawable(0));

                Window window = d.getWindow();
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.CENTER;
                int screenWidth = a.getResources().getDisplayMetrics().widthPixels;
                params.width = (int) (screenWidth * 0.92);
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                params.verticalMargin = 0.0f;
                window.setAttributes(params);

                d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        isAdding = false;
                    }
                });

                FrameLayout root = new FrameLayout(a);
                root.setPadding(dp(a, 20), dp(a, 60), dp(a, 20), dp(a, 20));

                final SwipeRefreshLayout swipe = new SwipeRefreshLayout(a);
                root.addView(swipe);
                swipe.setPadding(0, 0, 0, 0);

                final ScrollView sc = new ScrollView(a);
                swipe.addView(sc);

                final LinearLayout cd = new LinearLayout(a);
                cd.setOrientation(LinearLayout.VERTICAL);
                cd.setBackground(roundRect(Color.parseColor("#FFFFFF"), dp(a, 12)));
                cd.setPadding(dp(a, 16), dp(a, 16), dp(a, 16), dp(a, 16));
                sc.addView(cd);

                TextView t = new TextView(a);
                t.setText("功能管理");
                t.setTextSize(16);
                t.setTypeface(null, Typeface.BOLD);
                t.setTextColor(Color.BLACK);
                cd.addView(t);

                TextView s = new TextView(a);
                s.setText(gn + (g.equals("") ? "" : " (" + g + ")"));
                s.setTextSize(12);
                s.setTextColor(Color.parseColor("#3B71FE"));
                s.setPadding(0, 0, 0, dp(a, 6));
                cd.addView(s);

                TextView tips = new TextView(a);
                tips.setText("下拉可刷新状态");
                tips.setTextSize(9);
                tips.setTextColor(Color.parseColor("#999999"));
                tips.setPadding(0, 0, 0, dp(a, 8));
                cd.addView(tips);

                final LinearLayout editorContainer = new LinearLayout(a);
                editorContainer.setOrientation(LinearLayout.VERTICAL);
                editorContainer.setBackground(roundRect(Color.parseColor("#F0F7FF"), dp(a, 8)));
                editorContainer.setPadding(dp(a, 12), dp(a, 12), dp(a, 12), dp(a, 12));
                editorContainer.setVisibility(View.GONE);
                editorContainer.setAlpha(0f);
                editorContainer.setScaleY(0.8f);
                cd.addView(editorContainer);

                TextView editTitle = new TextView(a);
                editTitle.setText("✏️ 新建功能");
                editTitle.setTextSize(15);
                editTitle.setTextColor(Color.parseColor("#3B71FE"));
                editTitle.setPadding(0, 0, 0, dp(a, 8));
                editorContainer.addView(editTitle);

                final boolean[] isFileState = {false};
                final boolean[] cks = new boolean[8];

                final EditText etN = addNameInput(a, editorContainer, "", Color.parseColor("#FFFFFF"));
                final EditText etC = addCodeInput(a, editorContainer, "", false, isFileState, Color.parseColor("#FFFFFF"));
                addPresetRows(a, editorContainer, etC);

                TextView varTips = new TextView(a);
                varTips.setText("变量:qun群号 uinQQ msg消息 type类型(1私2群) operator操作者 time秒");
                varTips.setTextSize(9);
                varTips.setTextColor(Color.parseColor("#666666"));
                varTips.setPadding(0, dp(a, 4), 0, 0);
                editorContainer.addView(varTips);

                final FormComponents fc = new FormComponents();
                TextView[] chips = addChipRows(a, editorContainer, cks, fc);

                addPreprocRow(a, editorContainer, fc, 0, "", 0, 0);
                updatePreprocVisibility(fc, false);

                FormComponents loopFc = addLoopRow(a, editorContainer, false, 5000, 0, cks, Color.parseColor("#FFFFFF"));
                fc.etInterval = loopFc.etInterval;
                fc.etCount = loopFc.etCount;
                fc.chipLoop = loopFc.chipLoop;
                fc.loopSettings = loopFc.loopSettings;

                final EditText etTime = addTimeRow(a, editorContainer, "");

                LinearLayout editorBtnRow = new LinearLayout(a);
                editorBtnRow.setOrientation(LinearLayout.HORIZONTAL);
                editorBtnRow.setPadding(0, dp(a, 12), 0, 0);
                editorContainer.addView(editorBtnRow);

                TextView btnSave = makeBtn(a, "保存", Color.WHITE, Color.parseColor("#3B71FE"));
                btnSave.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                editorBtnRow.addView(btnSave);

                TextView btnTest = makeBtn(a, "测试", Color.parseColor("#666666"), Color.parseColor("#F5F5F5"));
                LinearLayout.LayoutParams lpTest = new LinearLayout.LayoutParams(0, -2, 1.0f);
                lpTest.setMargins(dp(a, 6), 0, dp(a, 6), 0);
                btnTest.setLayoutParams(lpTest);
                editorBtnRow.addView(btnTest);

                TextView btnCancel = makeBtn(a, "取消", Color.parseColor("#666666"), Color.parseColor("#F5F5F5"));
                btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                editorBtnRow.addView(btnCancel);

                final LinearLayout lst = new LinearLayout(a);
                lst.setOrientation(LinearLayout.VERTICAL);
                lst.setPadding(0, dp(a, 6), 0, 0);
                cd.addView(lst);

                TextView btnAdd = makeBtn(a, "+ 新建功能", Color.WHITE, Color.parseColor("#3B71FE"));
                cd.addView(btnAdd, cd.indexOfChild(lst));

                View ln = new View(a);
                ln.setBackgroundColor(Color.parseColor("#EEEEEE"));
                ln.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(a, 1)));
                ((LinearLayout.LayoutParams) ln.getLayoutParams()).setMargins(0, dp(a, 10), 0, dp(a, 10));
                cd.addView(ln, cd.indexOfChild(lst));

                TextView lt = new TextView(a);
                lt.setText("功能列表(长按编辑, 左滑删除):");
                lt.setTextSize(12);
                lt.setTextColor(Color.parseColor("#666666"));
                cd.addView(lt, cd.indexOfChild(lst));

                final Runnable refreshCallback = new Runnable() {
                    public void run() {
                        a.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    swipe.setRefreshing(true);
                                    lst.removeAllViews();
                                    String[] fs = getAll();
                                    if (fs.length == 0 || (fs.length == 1 && fs[0].equals(""))) {
                                        TextView e = new TextView(a);
                                        e.setText("暂无功能");
                                        e.setTextColor(Color.parseColor("#BBBBBB"));
                                        lst.addView(e);
                                    } else {
                                        for (String f : fs) {
                                            if (!f.equals("")) {
                                                createItem(a, lst, f, g, gn, refreshCallback);
                                            }
                                        }
                                    }
                                    swipe.postDelayed(new Runnable() {
                                        public void run() {
                                            swipe.setRefreshing(false);
                                        }
                                    }, 300);
                                } catch (Throwable e) {
                                    traceLog("function_log", "[refreshCallback] 异常: " + e);
                                    swipe.setRefreshing(false);
                                }
                            }
                        });
                    }
                };

                refreshCallback.run();

                swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    public void onRefresh() {
                        swipe.postDelayed(new Runnable() {
                            public void run() {
                                refreshCallback.run();
                            }
                        }, 50);
                    }
                });
                swipe.setColorSchemeColors(Color.parseColor("#3B71FE"));

                btnSave.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String n = etN.getText().toString().trim();
                        String c = etC.getText().toString();
                        if (n.equals("") || (c.equals("") && !cks[6])) {
                            toast("名称和内容不能为空(选择预处理时可不填代码)");
                            return;
                        }
                        boolean hasCb = false;
                        for (int i = 0; i < 7; i++) if (cks[i]) hasCb = true;

                        long intervalVal = 0;
                        int countVal = 0;
                        int preTypeVal = 0;
                        String preTailVal = "";
                        int repeatSendVal = 0;
                        int repeatConcatVal = 0;

                        if (!hasCb) {
                            try {
                                intervalVal = Long.parseLong(fc.etInterval.getText().toString());
                            } catch (Throwable e) {
                                toast("间隔格式错误");
                                return;
                            }
                            try {
                                countVal = Integer.parseInt(fc.etCount.getText().toString());
                            } catch (Throwable e) {
                                countVal = 0;
                            }
                        }

                        if (cks[6] && fc.preTypeChips != null) {
                            for (int i = 0; i < 50; i++) {
                                if (i < fc.preTypeChips.length && fc.preTypeChips[i].getCurrentTextColor() == Color.WHITE) {
                                    preTypeVal = i;
                                    break;
                                }
                            }
                            preTailVal = fc.etPreTail.getText().toString();
                            try {
                                repeatSendVal = Integer.parseInt(fc.etRepeatSend.getText().toString());
                            } catch (Throwable e) {}
                            try {
                                repeatConcatVal = Integer.parseInt(fc.etRepeatConcat.getText().toString());
                            } catch (Throwable e) {}
                        }

                        String rawTime = etTime.getText().toString().trim();

                        try {
                            saveFunc(n, c, isFileState[0], new boolean[]{cks[0], cks[1], cks[2], cks[3], cks[4], cks[5], cks[6]}, cks[7], rawTime, intervalVal, fc.chipLoop.getCurrentTextColor() == Color.WHITE, countVal, preTypeVal, preTailVal, repeatSendVal, repeatConcatVal);
                            toast("已添加:" + n);
                            editorContainer.animate().scaleY(0.8f).alpha(0f).setDuration(300).withEndAction(new Runnable() {
                                public void run() {
                                    editorContainer.setVisibility(View.GONE);
                                    isAdding = false;
                                    refreshCallback.run();
                                }
                            }).start();
                        } catch (Throwable e) {
                            traceLog("function_log", "[btnSave] 保存异常: " + e);
                            toast("保存失败");
                        }
                    }
                });

                btnTest.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String c = etC.getText().toString();
                        if (c.equals("")) {
                            toast("代码为空");
                            return;
                        }
                        testCode(etN.getText().toString().trim(), c, "新建测试");
                    }
                });

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        editorContainer.animate().scaleY(0.8f).alpha(0f).setDuration(300).withEndAction(new Runnable() {
                            public void run() {
                                editorContainer.setVisibility(View.GONE);
                                isAdding = false;
                            }
                        }).start();
                    }
                });

                btnAdd.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (isAdding && editorContainer.getVisibility() == View.VISIBLE) {
                            toast("已有未保存的编辑");
                            return;
                        }

                        try {
                            isAdding = true;
                            etN.setText("");
                            etC.setText("");
                            etTime.setText("");
                            isFileState[0] = false;
                            for (int i = 0; i < 8; i++) {
                                cks[i] = false;
                                if (i < 7) setChip(chips[i], false);
                            }
                            if (fc.chipLoop.getCurrentTextColor() == Color.WHITE) {
                                fc.chipLoop.performClick();
                            }
                            fc.etInterval.setText("5000");
                            fc.etCount.setText("0");

                            if (fc.preTypeChips != null) {
                                for (int j = 0; j < 50; j++) {
                                    if(j < fc.preTypeChips.length) setChipWithType(fc.preTypeChips[j], j == 0, 2);
                                }
                                fc.etPreTail.setText("");
                                fc.etRepeatSend.setText("");
                                fc.etRepeatConcat.setText("");
                            }
                            updatePreprocVisibility(fc, false);

                            editorContainer.setVisibility(View.VISIBLE);
                            editorContainer.setScaleY(0.8f);
                            editorContainer.setAlpha(0f);
                            editorContainer.setTranslationY(-dp(a, 20));

                            AnimatorSet set = new AnimatorSet();
                            ObjectAnimator scale = ObjectAnimator.ofFloat(editorContainer, "scaleY", 0.8f, 1f);
                            ObjectAnimator alpha = ObjectAnimator.ofFloat(editorContainer, "alpha", 0f, 1f);
                            ObjectAnimator trans = ObjectAnimator.ofFloat(editorContainer, "translationY", -dp(a, 20), 0f);
                            set.playTogether(scale, alpha, trans);
                            set.setDuration(400);
                            set.setInterpolator(new OvershootInterpolator(1.2f));
                            set.start();

                        } catch (Throwable e) {
                            traceLog("function_log", "[btnAdd] 异常: " + e);
                            isAdding = false;
                        }
                    }
                });

                TextView cls = makeBtn(a, "关闭", Color.parseColor("#666666"), Color.parseColor("#F5F5F5"));
                cls.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        isAdding = false;
                        animateDialogOut(d, null);
                    }
                });
                cd.addView(cls);

                d.setContentView(root);
                d.getWindow().setLayout((int) (screenWidth * 0.92), WindowManager.LayoutParams.WRAP_CONTENT);
                d.show();
                animateDialogIn(d);

            } catch (Throwable e) {
                traceLog("function_log", "[showHotPlugMain] 外层异常: " + e);
                isAdding = false;
            }
        }
    });
}

/**
 * 创建功能模块列表项
 * @param a Activity: 上下文
 * @param c LinearLayout: 父容器
 * @param f String: 功能名
 * @param gid String: 群号
 * @param gn String: 群名
 * @param refresh Runnable: 刷新回调
 */
void createItem(final Activity a, LinearLayout c, final String f, final String gid, final String gn, final Runnable refresh) {
    try {
        String[] m = getMeta(f);
        if (m == null) return;
        boolean hasGrp = m[9].equals("1"); // Grp is index 9
        boolean hasAnyCallback = false;
        for (int i = 2; i <= 8; i++) {
            if (m[i].equals("1")) hasAnyCallback = true;
        }
        long interval = 0;
        int loopCount = 0;
        try {
            interval = Long.parseLong(m[12]); // Interval is 12
        } catch (Throwable e) {}
        try {
            loopCount = Integer.parseInt(m[14]); // Count is 14
        } catch (Throwable e) {}
        boolean isLoop = m[13].equals("1"); // Loop is 13
        String timeCfg = m[11]; // Time is 11
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

        int visibleContentWidth = (int)(screenWidth * 0.92f) - dp(a, 72);

        final LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackground(makeFeedbackBg(Color.parseColor("#F8F9FA"), adjustColor(Color.parseColor("#F8F9FA"), 0.85f), dp(a, 8)));
        
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(visibleContentWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        contentParams.setMargins(0, 0, dp(a, 1), 0); 
        content.setLayoutParams(contentParams);
        
        content.setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10));
        content.setClickable(true);
        content.setFocusable(true);
        itemContainer.addView(content);

        final TextView deleteBtn = new TextView(a);
        deleteBtn.setText("删除");
        deleteBtn.setTextSize(14);
        deleteBtn.setTextColor(Color.WHITE);
        deleteBtn.setBackground(roundRect(Color.parseColor("#FF4444"), dp(a, 8)));
        deleteBtn.setGravity(Gravity.CENTER);
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
        tv.setText("📦 " + f);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#222222"));
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
            loopInfo.setTextColor(Color.parseColor("#FF9800"));
            loopInfo.setPadding(0, 0, dp(a, 4), 0);
            loopInfo.setMaxWidth(dp(a, 70));
            loopInfo.setSingleLine(true);
            loopInfo.setEllipsize(TextUtils.TruncateAt.END);
            switchContainer.addView(loopInfo, 0);
        }

        final boolean[] mainOn = {hasAnyCallback ? getRun(f) : getLoad(f)};
        final TextView btnMain = makeSwitch(a, mainOn[0], hasAnyCallback ? Color.parseColor("#00C853") : Color.parseColor("#3B71FE"));
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
                long nextTime = getNextScheduleTime(timeCfg);
                long now = System.currentTimeMillis();
                if (nextTime > 0) {
                    long countdown = nextTime - now;
                    if (countdown < 0) infoText = "⏰ " + formatSchedule(timeCfg) + " (计算中)";
                    else infoText = "⏰ " + formatSchedule(timeCfg) + " (剩" + formatCountdown(countdown) + ")";
                } else infoText = "⏰ " + formatSchedule(timeCfg) + " (配置无效)";
            } else if (isLoop) {
                infoText = "🔁 " + interval + "ms ×" + (loopCount == 0 ? "∞" : loopCount);
            }
            if (!infoText.equals("")) {
                TextView tvInfo = new TextView(a);
                tvInfo.setText(infoText);
                tvInfo.setTextSize(10);
                tvInfo.setTextColor(Color.parseColor("#666666"));
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
        dlv.setBackgroundColor(Color.parseColor("#E0E0E0"));
        dlv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(a, 1)));
        exp.addView(dlv);

        TextView btnTestExp = new TextView(a);
        btnTestExp.setText("▶ 测试执行");
        btnTestExp.setTextSize(12);
        btnTestExp.setTextColor(Color.parseColor("#3B71FE"));
        btnTestExp.setBackground(makeFeedbackBg(Color.parseColor("#E8EEFF"), adjustColor(Color.parseColor("#E8EEFF"), 0.9f), dp(a, 6)));
        btnTestExp.setPadding(0, dp(a, 8), 0, dp(a, 8));
        btnTestExp.setGravity(Gravity.CENTER);
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
            TextView l = new TextView(a); l.setText("运行开关"); l.setTextSize(13); l.setTextColor(Color.parseColor("#666666")); l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rRun.addView(l);
            TextView state = new TextView(a); state.setText("主开关统管"); state.setTextSize(11); state.setTextColor(Color.parseColor("#999999")); rRun.addView(state);
        } else {
             LinearLayout rRun = new LinearLayout(a); rRun.setOrientation(LinearLayout.HORIZONTAL); rRun.setGravity(Gravity.CENTER_VERTICAL); rRun.setPadding(0, dp(a, 8), 0, 0); exp.addView(rRun);
             TextView l = new TextView(a); l.setText("允许运行"); l.setTextSize(13); l.setTextColor(Color.parseColor("#666666")); l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rRun.addView(l);
             final boolean[] runOn = {getRun(f)}; final TextView btnRun = makeSwitch(a, runOn[0], Color.parseColor("#00C853")); rRun.addView(btnRun);
             btnRun.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { runOn[0] = !runOn[0]; setRun(f, runOn[0]); setSwitch(btnRun, runOn[0], Color.parseColor("#00C853")); }});
             
             if (isLoop) {
                LinearLayout rLoop = new LinearLayout(a); rLoop.setOrientation(LinearLayout.HORIZONTAL); rLoop.setGravity(Gravity.CENTER_VERTICAL); rLoop.setPadding(0, dp(a, 8), 0, 0); exp.addView(rLoop);
                TextView l2 = new TextView(a); String countText = loopCount > 0 ? (" (剩" + loopCount + "次)") : ""; l2.setText("循环执行" + countText); l2.setTextSize(13); l2.setTextColor(Color.parseColor("#666666")); l2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rLoop.addView(l2);
                final boolean[] loopOn = {getLoop(f)}; final TextView btnLoop = makeSwitch(a, loopOn[0], Color.parseColor("#FF9800")); rLoop.addView(btnLoop);
                btnLoop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { loopOn[0] = !loopOn[0]; setLoop(f, loopOn[0]); setSwitch(btnLoop, loopOn[0], Color.parseColor("#FF9800")); toast(loopOn[0] ? "循环已开" : "循环已关"); }});
             }
        }
        
        if (hasGrp && !gid.equals("")) {
             LinearLayout rGrp = new LinearLayout(a); rGrp.setOrientation(LinearLayout.HORIZONTAL); rGrp.setGravity(Gravity.CENTER_VERTICAL); rGrp.setPadding(0, dp(a, 8), 0, 0); exp.addView(rGrp);
             TextView l = new TextView(a); l.setText("本群运行"); l.setTextSize(13); l.setTextColor(Color.parseColor("#666666")); l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); rGrp.addView(l);
             final boolean[] grpOn = {getGrp(f, gid)}; final TextView btnGrp = makeSwitch(a, grpOn[0], Color.parseColor("#FF9800")); rGrp.addView(btnGrp);
             btnGrp.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { grpOn[0] = !grpOn[0]; setGrp(f, gid, grpOn[0]); setSwitch(btnGrp, grpOn[0], Color.parseColor("#FF9800")); toast(grpOn[0] ? "本群已开启" : "本群已关闭"); }});
        }

        final LinearLayout itemWrapper = new LinearLayout(a);
        itemWrapper.setOrientation(LinearLayout.VERTICAL);
        itemWrapper.addView(slideView);
        View itemDivider = new View(a);
        itemDivider.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, 1);
        dividerParams.bottomMargin = dp(a, 4);
        itemWrapper.addView(itemDivider, dividerParams);

        final boolean[] isExpanded = {false};

        final GestureDetector gestureDetector = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 80; 
            private static final int SWIPE_VELOCITY_THRESHOLD = 80;

            public boolean onDown(MotionEvent e) {
                content.animate().scaleX(0.98f).scaleY(0.98f).setDuration(50).start();
                return true; 
            }

            public boolean onSingleTapConfirmed(MotionEvent e) {
                content.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                if (isExpanded[0]) {
                    isExpanded[0] = false;
                    exp.setVisibility(View.GONE);
                    tv.setText("📦 " + f);
                } else {
                    isExpanded[0] = true;
                    exp.setVisibility(View.VISIBLE);
                    exp.setAlpha(0f);
                    exp.animate().alpha(1f).setDuration(250).start();
                    tv.setText("📂 " + f);
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
                        Math.abs(diffX) > SWIPE_THRESHOLD && 
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        
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
                    setSwitch(btnMain, mainOn[0], Color.parseColor("#00C853"));
                    toast(mainOn[0] ? "运行已开" : "运行已关");
                } else {
                    setLoad(f, mainOn[0]);
                    setSwitch(btnMain, mainOn[0], Color.parseColor("#3B71FE"));
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

/**
 * 显示删除确认弹窗
 * @param a Activity: 上下文
 * @param f String: 功能名
 * @param itemView View: 列表项视图
 * @param parent LinearLayout: 父容器
 * @param refresh Runnable: 刷新回调
 */
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
    layout.setBackground(roundRect(Color.WHITE, dp(a, 16)));
    layout.setPadding(dp(a, 24), dp(a, 24), dp(a, 24), dp(a, 24));
    
    TextView title = new TextView(a);
    title.setText("确认删除");
    title.setTextSize(18);
    title.setTypeface(null, Typeface.BOLD);
    title.setTextColor(Color.BLACK);
    layout.addView(title);
    
    TextView message = new TextView(a);
    message.setText("确定要删除功能 \"" + f + "\" 吗？此操作无法撤销");
    message.setTextSize(14);
    message.setTextColor(Color.parseColor("#666666"));
    message.setPadding(0, dp(a, 12), 0, dp(a, 24));
    layout.addView(message);
    
    LinearLayout buttons = new LinearLayout(a);
    buttons.setOrientation(LinearLayout.HORIZONTAL);
    layout.addView(buttons);
    
    TextView cancel = new TextView(a);
    cancel.setText("取消");
    cancel.setTextColor(Color.parseColor("#666666"));
    cancel.setTextSize(15);
    cancel.setGravity(Gravity.CENTER);
    cancel.setBackground(roundRect(Color.parseColor("#F5F5F5"), dp(a, 8)));
    cancel.setPadding(dp(a, 24), dp(a, 12), dp(a, 24), dp(a, 12));
    cancel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
    cancel.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            confirmDialog.dismiss();
        }
    });
    buttons.addView(cancel);
    
    TextView confirm = new TextView(a);
    confirm.setText("删除");
    confirm.setTextColor(Color.WHITE);
    confirm.setTextSize(15);
    confirm.setGravity(Gravity.CENTER);
    confirm.setBackground(roundRect(Color.parseColor("#FF4444"), dp(a, 8)));
    confirm.setPadding(dp(a, 24), dp(a, 12), dp(a, 24), dp(a, 12));
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
    confirmDialog.getWindow().setLayout((int)(a.getResources().getDisplayMetrics().widthPixels * 0.85), -2);
    confirmDialog.show();
    animateDialogIn(confirmDialog);
}

/** 接口实现：入群事件 */
void joinGroup(String g, String m) { 
    try { dispatchEvent(new String[]{g, m}, 2); } catch (Throwable e) {} 
}

/** 接口实现：退群事件 */
void quitGroup(String g, String m) { 
    try { dispatchEvent(new String[]{g, m}, 3); } catch (Throwable e) {} 
}

/** 接口实现：禁言事件 */
void shutUpGroup(String g, String m, long t, String o) { 
    try { dispatchEvent(new Object[]{g, m, t, o}, 4); } catch (Throwable e) {} 
}

/** 接口实现：拍一拍事件 */
void onPaiYiPai(String p, int t, String o) {
    try { dispatchEvent(new Object[]{p, t, o}, 6); } catch (Throwable e) {} 
}

/**
 * 预处理消息文本，根据 type 应用不同样式效果
 * 所有样式（除反转、双字外）都采用：每个字符后加组合字符 + 末尾补一个
 * @param text String: 原始文本
 * @param type int: 样式类型（0=无，1=逐字发送，2~49=各种装饰样式）
 * @return String: 处理后的文本
 */
String applyPreprocess(String text, int type) {
    if (text == null || text.isEmpty()) return text;

    if (type <= 0 || type > 49) return text;

    try {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        int len = text.length();

        String[] combs = {
            null, null,                  // 0 无, 1 逐字
            "̲", "̶", "̳", "̸", "͒", "͙", null, null,  // 2~7
            "꯭", "̲", "̿", "̬", "͟", "̤", "̥", "̸", "⃥", "⃫",  // 10~19
            "⃘", "⃝", "⃞", "⃟", "⃠", "⃖", "⃗", "⃰", "⃢", "⃲",  // 20~29
            "⃤", "⃦", "⃴", "⃵", "⃒", "⃓", "⃔", "⃕", "⃡", "⃪",  // 30~39
            "⃬", "⃭", "⃮", "⃯", "⃱", "⃫", "⃷", "̿", "⃰", "⃠"   // 40~49
        };

        String comb = combs[type];
        if (comb == null) {
            if (type == 8) { // 反转
                return new StringBuilder(text).reverse().toString();
            } else if (type == 9) { // 双字
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

/**
 * 添加到发送队列 - 优化的队列系统
 * @param uin String: 目标UIN
 * @param msg String: 消息内容
 * @param type int: 聊天类型
 */
void addToSendQueue(String uin, String msg, int type) {
    SendUnit unit = new SendUnit(uin, msg, type);
    sendMsgQueue.offer(unit);
    processSendQueue();
}

/**
 * 处理发送队列 - 优化线程安全与异常兜底，修复队列卡死问题
 */
void processSendQueue() {
    if (isProcessingQueue) return;
    isProcessingQueue = true;
    
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
                isProcessingQueue = false;
            }
        }
    });
}

/**
 * 逐字发送处理
 * @param m String: 原始消息
 * @return String: 消息首片段
 */
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
                            traceLog("function_log", "消息切片入队失败: " + e);
                        }
                    }
                });
            }
            return firstSeg;
        }
        return m;
    } catch(Throwable e) {
        traceLog("function_log", "逐字模块异常: " + e);
        return m;
    }
}

/**
 * 获取（处理）即将发送的消息
 * 触发预处理逻辑
 * @param m String: 原始消息
 * @return String: 处理后的消息
 */
String getMsg(String m){
    if(m == null || m.isEmpty()) return m;
    
    try{
        Integer key = new Integer(7);
        if (eventRegistry.containsKey(key)) {
            ArrayList list = (ArrayList) eventRegistry.get(key);
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    String func = (String)list.get(i);
                    if (!getRun(func)) continue;
                    
                    String[] meta = getMeta(func);
                    if (meta != null && meta[9].equals("1")) { // Grp is index 9
                        if (currentPeerUin == null || currentPeerUin.isEmpty()) continue;
                        if (!getGrp(func, currentPeerUin)) continue;
                    }
                    
                    HashMap cfg = (HashMap)preProcConfig.get(func);
                    if (cfg != null) {
                        int type = (Integer)cfg.get("type");
                        String tail = (String)cfg.get("tail");
                        int repeatSend = 0;
                        int repeatConcat = 0;
                        try { repeatSend = (Integer)cfg.get("rs"); } catch(Throwable e) {}
                        try { repeatConcat = (Integer)cfg.get("rc"); } catch(Throwable e) {}
                        
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
                        
                        if (type == 1) { // Split is 1
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
        }
        
        return m;
        
    }catch(Throwable e){
        traceLog("function_log", "[getMsg]" + e);
        return m;
    }
}

/**
 * 提取下一个发送单元
 * @return String: 下一个字符或表情单元
 */
String extractNextSegment(){
    String seg = extractSegmentAt(splitBuffer, splitPos);
    splitPos += seg.length();
    return seg;
}

/**
 * 从指定位置提取单元（表情/Emoji/单字）
 * @param text String: 源文本
 * @param pos int: 起始位置
 * @return String: 提取的单元
 */
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

addItem("功能热插拔", "showHotPlugMain");
rebuildRegistry();