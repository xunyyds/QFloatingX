
import com.tencent.common.app.BaseApplicationImpl;
import com.tencent.mobileqq.activity.shortvideo.d;
import com.tencent.mobileqq.app.BaseActivity;
import com.tencent.mobileqq.app.BusinessObserver;
import com.tencent.mobileqq.jump.api.IJumpApi;
import com.tencent.mobileqq.onlinestatus.api.IOnlineStatusService;
import com.tencent.mobileqq.qroute.QRoute;
import com.tencent.mobileqq.transfile.api.ITransFileController;
import com.tencent.mobileqq.troop.api.ITroopInfoService;


import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.IMsgOperateCallback;
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord;
import com.tencent.qqnt.kernel.nativeinterface.PicElement;
import com.tencent.qqnt.kernel.nativeinterface.PttElement;
import com.tencent.qqnt.kernel.nativeinterface.ReplyAbsElement;
import com.tencent.qqnt.kernel.nativeinterface.ReplyElement;
import com.tencent.qqnt.kernel.nativeinterface.TextElement;
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact;
import com.tencent.qqnt.msg.api.IMsgService;
import com.tencent.qqnt.msg.api.IMsgUtilApi;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;


import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;


import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SearchView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;


import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Movie;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;


import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;


import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;


import java.lang.Exception;
import java.lang.Math;
import java.lang.Runnable;
import java.lang.StringBuffer;
import java.lang.StringBuilder;
import java.lang.System;
import android.os.PowerManager;


import java.lang.reflect.Field;
import java.lang.reflect.Method;


import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;


import java.util.zip.ZipEntry;


import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.json.JSONArray;
import org.json.JSONObject;


import me.yxp.qfun.hook.api.OnGetRKey;
import me.yxp.qfun.hook.api.OnAIOViewUpdate;
import me.yxp.qfun.hook.api.AIOViewUpdateListener;
import me.yxp.qfun.hook.chat.ShowMsgTime;
import me.yxp.qfun.hook.msg.AntiRevoke;
import me.yxp.qfun.plugin.bean.MsgData;
import me.yxp.qfun.utils.qq.FriendTool;
import me.yxp.qfun.utils.qq.QQCurrentEnv;
import me.yxp.qfun.loader.hookapi.HookEngineManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import me.yxp.qfun.utils.reflect.ClassUtils;
import com.tencent.mobileqq.app.BusinessObserver;


import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;


import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;


import java.lang.Thread;
import java.lang.ThreadLocal;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executor;
import java.lang.CharSequence;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;


import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;


import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;


import android.view.GestureDetector.SimpleOnGestureListener;


import android.view.ViewTreeObserver;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;


import android.database.Cursor;


import android.content.BroadcastReceiver;


import android.app.Service;


import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
