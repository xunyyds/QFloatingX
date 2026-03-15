// =============== 1. 腾讯 QQ/第三方 SDK 核心依赖 ===============
import com.tencent.common.app.BaseApplicationImpl; // QQ应用基础实现类
import com.tencent.mobileqq.activity.QQSettingMe; // QQ个人设置页面Activity
import com.tencent.mobileqq.activity.shortvideo.d; // 短视频相关功能
import com.tencent.mobileqq.aio.helper.EmojiReplyHelper; // 表情快速回复助手
import com.tencent.mobileqq.app.BaseActivity; // QQ基础Activity类
import com.tencent.mobileqq.app.BusinessHandlerFactory; // 业务处理器工厂，用于创建各种业务处理器
import com.tencent.mobileqq.app.CardHandler; // 名片消息处理器
import com.tencent.mobileqq.app.QQAppInterface; // QQ应用核心接口，提供各种服务访问
import com.tencent.mobileqq.data.Friends; // 好友数据模型
import com.tencent.mobileqq.filemanager.app.FileManagerApplication; // 文件管理应用上下文
import com.tencent.mobileqq.forward.ForwardSDKB77Sender; // 消息转发SDK发送器
import com.tencent.mobileqq.friend.api.IFriendDataService; // 好友数据服务接口，查询好友信息
import com.tencent.mobileqq.jump.api.IJumpApi; // 页面跳转API接口
import com.tencent.mobileqq.onlinestatus.api.IOnlineStatusService; // 在线状态服务接口
import com.tencent.mobileqq.profilecard.utils.URLSafeUtil; // URL安全处理工具类
import com.tencent.mobileqq.qroute.QRoute; // QQ模块化路由框架，用于跨模块通信
import com.tencent.mobileqq.roamsetting.api.IRoamSettingService; // 消息漫游设置服务接口
import com.tencent.mobileqq.structmsg.AbsShareMsg; // 抽象分享消息基类
import com.tencent.mobileqq.structmsg.StructMsgForAudioShare; // 音频分享结构化消息
import com.tencent.mobileqq.structmsg.StructMsgForGeneralShare; // 通用分享结构化消息（链接、小程序等）
import com.tencent.mobileqq.structmsg.StructMsgForImageShare; // 图片分享结构化消息
import com.tencent.mobileqq.transfile.api.ITransFileController; // 文件传输控制器接口
import com.tencent.mobileqq.troop.api.ITroopInfoService; // 群组信息服务接口
import com.tencent.mobileqq.utils.DialogUtil; // 对话框工具类
import com.tencent.mobileqq.vip.api.IVipColorName; // VIP彩色昵称API接口


// =============== 2. 腾讯 QQNT 内核接口 ===============
import com.tencent.qqnt.kernel.nativeinterface.FaceBubbleElement; // 表情气泡消息元素
import com.tencent.qqnt.kernel.nativeinterface.FaceElement; // 表情消息元素（QQ表情、emoji等）
import com.tencent.qqnt.kernel.nativeinterface.FileElement; // 文件消息元素
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService; // QQNT内核消息服务接口
import com.tencent.qqnt.kernel.nativeinterface.IMsgOperateCallback; // 消息操作回调接口（发送、撤回等）
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback; // 通用操作回调接口
import com.tencent.qqnt.kernel.nativeinterface.LinkInfo; // 链接信息（URL、标题、摘要等）
import com.tencent.qqnt.kernel.nativeinterface.MarkdownElement; // Markdown格式消息元素
import com.tencent.qqnt.kernel.nativeinterface.MsgElement; // 消息元素基类
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord; // 消息记录对象
import com.tencent.qqnt.kernel.nativeinterface.PicElement; // 图片消息元素
import com.tencent.qqnt.kernel.nativeinterface.PttElement; // 语音消息元素（PTT=Push To Talk）
import com.tencent.qqnt.kernel.nativeinterface.ReplyAbsElement; // 回复消息抽象元素
import com.tencent.qqnt.kernel.nativeinterface.ReplyElement; // 回复消息元素
import com.tencent.qqnt.kernel.nativeinterface.SmallYellowFaceInfo; // 小黄脸表情信息
import com.tencent.qqnt.kernel.nativeinterface.TextElement; // 文本消息元素
import com.tencent.qqnt.kernelpublic.nativeinterface.Contact; // 联系人对象（好友/群聊）
import com.tencent.qqnt.msg.api.IMsgService; // QQNT消息服务API接口
import com.tencent.qqnt.msg.api.IMsgUtilApi; // QQNT消息工具API接口


// =============== 3. QQ 底层协议与认证 ===============
import com.tencent.qphone.base.util.BaseApplication; // QPhone基础应用类
import mqq.app.AppRuntime.Status; // MQQ应用运行时状态枚举
import mqq.app.AppService; // MQQ应用服务基类
import mqq.app.MobileQQ; // MobileQQ应用主类
import mqq.app.TicketManagerImpl; // 票据管理器实现类
import mqq.inject.SkeyInjectManager; // Skey注入管理器（用于权限验证）
import mqq.manager.TicketManager; // 票据管理器接口
import mqq.manager.TicketManager.IPskeyManager; // Pskey管理器接口（页面鉴权票据）
import oicq.wlogin_sdk.request.Ticket; // 登录票据对象
import oicq.wlogin_sdk.request.WtTicketPromise; // WebToken票据Promise
import oicq.wlogin_sdk.request.WtloginHelper; // WebToken登录助手


// =============== 4. Android 基础组件 ===============
import android.app.Activity; // 活动组件，应用的单个屏幕
import android.app.ActivityManager; // 活动管理器，管理应用任务和进程
import android.app.ActivityManager.MemoryInfo; // 内存信息对象
import android.app.ActivityThread; // Activity线程（隐藏API，应用主线程）
import android.app.AlertDialog; // 警告对话框
import android.app.Application; // 应用全局类，整个应用生命周期
import android.app.Dialog; // 对话框基类
import android.app.Instrumentation; // 应用插桩工具，用于监控和自动化测试
import android.app.ProgressDialog; // 进度对话框
import android.content.ActivityNotFoundException; // 活动未找到异常
import android.content.ClipboardManager; // 剪贴板管理器
import android.content.ClipData; // 剪贴板数据对象
import android.content.ComponentName; // 组件名称（包名+类名）
import android.content.Context; // 上下文对象，访问应用资源和系统服务
import android.content.DialogInterface; // 对话框接口，处理对话框事件
import android.content.Intent; // 意图对象，用于组件间通信和启动
import android.content.IntentFilter; // 意图过滤器，用于广播接收器
import android.content.SharedPreferences; // 共享偏好设置，轻量级键值对存储
import android.content.pm.ApplicationInfo; // 应用信息对象
import android.content.pm.PackageInfo; // 包信息对象
import android.content.pm.PackageManager; // 包管理器，查询和操作应用包
import android.content.pm.ResolveInfo; // 解析信息，查询可处理Intent的组件
import android.content.res.Resources; // 资源访问类，获取字符串、颜色、尺寸等
import android.os.BatteryManager; // 电池管理器
import android.os.Build; // 构建信息，获取系统版本等
import android.os.Bundle; // 数据包，用于组件间传递数据
import android.os.Environment; // 环境变量，访问外部存储路径
import android.os.Handler; // 消息处理器，线程间通信
import android.os.IBinder; // 跨进程通信Binder接口
import android.os.Looper; // 消息循环器，处理线程消息队列
import android.os.StatFs; // 文件系统统计，获取存储空间信息
import android.os.SystemClock; // 系统时钟，获取系统启动时间等
import android.os.VibrationEffect; // 震动效果对象（Android 8.0+）
import android.os.Vibrator; // 震动器服务
import android.os.VibratorManager; // 震动管理器（Android 12+）


// =============== 5. Android UI 组件 - 布局 ===============
import android.widget.FrameLayout; // 帧布局，层叠式布局
import android.widget.GridLayout; // 网格布局
import android.widget.LinearLayout; // 线性布局，水平或垂直排列
import android.widget.RelativeLayout; // 相对布局，相对位置排列
import android.widget.ScrollView; // 滚动视图，垂直滚动容器


// =============== 6. Android UI 组件 - 控件 ===============
import android.widget.AdapterView; // 适配器视图基类
import android.widget.ArrayAdapter; // 数组适配器
import android.widget.BaseAdapter; // 适配器基类
import android.widget.Button; // 按钮控件
import android.widget.CheckBox; // 复选框控件
import android.widget.CompoundButton; // 复合按钮基类（带选中状态）
import android.widget.DatePicker; // 日期选择器
import android.widget.EditText; // 可编辑文本框
import android.widget.GridView; // 网格视图
import android.widget.ImageView; // 图片视图
import android.widget.ListView; // 列表视图
import android.widget.PopupWindow; // 弹出窗口
import android.widget.ProgressBar; // 进度条
import android.widget.RadioButton; // 单选按钮
import android.widget.SeekBar; // 拖动条
import android.widget.Space; // 空白占位控件
import android.widget.Spinner; // 下拉选择器
import android.widget.Switch; // 开关控件
import android.widget.TabHost; // 标签页主机
import android.widget.TabWidget; // 标签页控件
import android.widget.TextView; // 文本视图
import android.widget.TimePicker; // 时间选择器
import android.widget.Toast; // 吐司提示
import android.widget.ToggleButton; // 切换按钮


// =============== 7. Android 文本处理 ===============
import android.text.Editable; // 可编辑文本接口
import android.text.Html; // HTML文本解析
import android.text.InputType; // 输入类型常量（数字、邮箱等）
import android.text.Layout; // 文本布局
import android.text.Spannable; // 可样式化文本接口
import android.text.SpannableString; // 可样式化字符串
import android.text.SpannableStringBuilder; // 可样式化字符串构建器
import android.text.TextUtils; // 文本工具类
import android.text.TextWatcher; // 文本监听器，监听输入变化
import android.text.format.Formatter; // 格式化工具（字节转KB/MB等）
import android.text.method.ScrollingMovementMethod; // 滚动移动方法（让TextView可滚动）
import android.text.style.ForegroundColorSpan; // 前景色样式（文字颜色）
import android.text.style.ImageSpan; // 图片样式（文本中插入图片）
import android.text.style.StyleSpan; // 字体样式（粗体、斜体）
import android.text.style.UnderlineSpan; // 下划线样式


// =============== 8. Android View 与交互 ===============
import android.util.DisplayMetrics; // 显示度量，屏幕尺寸密度等
import android.view.GestureDetector; // 手势检测器（双击、滑动、长按等）
import android.view.Gravity; // 重力常量，控制对齐方式
import android.view.KeyEvent; // 按键事件
import android.view.LayoutInflater; // 布局填充器，从XML加载视图
import android.view.MotionEvent; // 触摸事件
import android.view.View; // 视图基类
import android.view.ViewGroup; // 视图组基类，容纳子视图
import android.view.ViewOutlineProvider; // 视图轮廓提供器（用于阴影和裁剪）
import android.view.Window; // 窗口对象，控制Activity顶层视图
import android.view.WindowManager; // 窗口管理器
import android.view.animation.AccelerateDecelerateInterpolator; // 加速减速插值器
import android.view.animation.AlphaAnimation; // 透明度动画
import android.view.animation.Animation; // 动画基类
import android.view.animation.AnimationSet; // 动画集合
import android.view.animation.ScaleAnimation; // 缩放动画
import android.view.animation.Transformation; // 动画变换


// =============== 9. Android 图形绘制 ===============
import android.graphics.Bitmap; // 位图对象
import android.graphics.BitmapFactory; // 位图工厂，创建和解码位图
import android.graphics.Canvas; // 画布，绘制图形的表面
import android.graphics.Color; // 颜色工具类
import android.graphics.Matrix; // 矩阵变换（旋转、缩放、平移）
import android.graphics.Movie; // GIF动画解码器
import android.graphics.Outline; // 视图轮廓（用于阴影）
import android.graphics.Paint; // 画笔，定义绘制样式
import android.graphics.PixelFormat; // 像素格式常量
import android.graphics.Typeface; // 字体
import android.graphics.drawable.BitmapDrawable; // 位图Drawable
import android.graphics.drawable.ColorDrawable; // 纯色Drawable
import android.graphics.drawable.Drawable; // 可绘制对象基类
import android.graphics.drawable.GradientDrawable; // 渐变Drawable
import android.graphics.drawable.ShapeDrawable; // 形状Drawable
import android.graphics.drawable.StateListDrawable; // 状态列表Drawable（不同状态不同样式）
import android.graphics.drawable.shapes.OvalShape; // 椭圆形状


// =============== 10. Android RenderScript（高性能图像处理）===============
import android.renderscript.Allocation; // RenderScript内存分配对象
import android.renderscript.Element; // RenderScript数据元素类型
import android.renderscript.RenderScript; // RenderScript上下文（GPU加速计算）
import android.renderscript.ScriptIntrinsicBlur; // 内置高斯模糊脚本


// =============== 11. Android 系统服务 ===============
import android.location.Criteria; // 位置提供者筛选条件
import android.location.Location; // 位置对象（经纬度、海拔等）
import android.location.LocationListener; // 位置监听器
import android.location.LocationManager; // 位置管理器
import android.location.LocationProvider; // 位置提供者（GPS、网络等）
import android.media.MediaPlayer; // 媒体播放器
import android.net.ConnectivityManager; // 网络连接管理器
import android.net.NetworkInfo; // 网络信息对象
import android.net.Uri; // 统一资源标识符
import android.net.wifi.WifiInfo; // WiFi信息对象
import android.net.wifi.WifiManager; // WiFi管理器
import android.provider.Settings; // 系统设置
import android.provider.Settings.Secure; // 安全设置（设备ID等）
import android.telephony.PhoneStateListener; // 电话状态监听器
import android.telephony.TelephonyManager; // 电话管理器
import android.webkit.WebSettings; // WebView设置
import android.webkit.WebView; // 网页视图
import android.webkit.WebViewClient; // WebView客户端，处理网页事件


// =============== 12. AndroidX 库 ===============
import androidx.annotation.Keep; // 防止代码混淆注解
import androidx.dynamicanimation.animation.DynamicAnimation; // 动态动画基类（物理动画）
import androidx.dynamicanimation.animation.SpringAnimation; // 弹簧动画（真实物理效果）
import androidx.dynamicanimation.animation.SpringForce; // 弹簧力参数（刚度、阻尼）


// =============== 13. Java 基础类 ===============
import java.lang.Exception; // 异常基类
import java.lang.Math; // 数学工具类
import java.lang.Runnable; // 可运行接口
import java.lang.RuntimeException; // 运行时异常
import java.lang.StringBuffer; // 线程安全字符串缓冲
import java.lang.StringBuilder; // 非线程安全字符串构建器（性能更好）
import java.lang.System; // 系统类（标准输入输出、时间等）


// =============== 14. Java 反射 API ===============
import java.lang.reflect.Constructor; // 构造器反射
import java.lang.reflect.Field; // 字段反射
import java.lang.reflect.InvocationTargetException; // 调用目标异常
import java.lang.reflect.Method; // 方法反射


// =============== 15. Java 网络通信 ===============
import java.net.HttpURLConnection; // HTTP URL连接
import java.net.InetAddress; // 网络地址
import java.net.ServerSocket; // 服务器Socket
import java.net.Socket; // 客户端Socket
import java.net.URI; // 统一资源标识符
import java.net.URL; // 统一资源定位符
import java.net.URLConnection; // URL连接基类
import java.net.URLDecoder; // URL解码器
import java.net.URLEncoder; // URL编码器


// =============== 16. Java IO 流操作 ===============
import java.io.BufferedReader; // 缓冲字符输入流
import java.io.BufferedWriter; // 缓冲字符输出流
import java.io.ByteArrayOutputStream; // 字节数组输出流
import java.io.File; // 文件对象
import java.io.FileInputStream; // 文件输入流
import java.io.FileNotFoundException; // 文件未找到异常
import java.io.FileOutputStream; // 文件输出流
import java.io.FileReader; // 文件字符读取器
import java.io.FileWriter; // 文件字符写入器
import java.io.IOException; // IO异常
import java.io.InputStream; // 输入流基类
import java.io.InputStreamReader; // 输入流读取器（字节转字符）
import java.io.OutputStream; // 输出流基类
import java.io.OutputStreamWriter; // 输出流写入器（字符转字节）
import java.io.StringReader; // 字符串读取器
import java.io.UnsupportedEncodingException; // 不支持编码异常


// =============== 17. Java 数据压缩 ===============
import java.util.zip.Deflater; // 压缩器（DEFLATE算法）
import java.util.zip.Inflater; // 解压器（DEFLATE算法）
import java.util.zip.ZipEntry; // ZIP条目
import java.util.zip.ZipInputStream; // ZIP输入流
import java.util.zip.ZipOutputStream; // ZIP输出流


// =============== 18. Java 加密与安全 ===============
import java.security.MessageDigest; // 消息摘要（MD5、SHA等哈希算法）
import java.security.NoSuchAlgorithmException; // 算法不存在异常
import java.util.Base64; // Base64编解码（Java 8+）
import javax.crypto.Cipher; // 加密/解密器（AES、DES等）
import javax.crypto.spec.SecretKeySpec; // 密钥规范


// =============== 19. Java 集合框架 ===============
import java.util.ArrayList; // 动态数组列表
import java.util.Arrays; // 数组工具类
import java.util.Collections; // 集合工具类
import java.util.Comparator; // 比较器接口
import java.util.Enumeration; // 枚举接口（迭代器的旧版本）
import java.util.HashMap; // 哈希映射表
import java.util.HashSet; // 哈希集合（无重复元素）
import java.util.Iterator; // 迭代器接口
import java.util.List; // 列表接口
import java.util.Map; // 映射接口
import java.util.Map.Entry; // 映射条目（键值对）
import java.util.Properties; // 属性配置类
import java.util.Set; // 集合接口
import java.util.Vector; // 线程安全动态数组（已过时，建议用ArrayList）


// =============== 20. Java 并发工具 ===============
import java.util.concurrent.ConcurrentHashMap; // 线程安全哈希映射表
import java.util.concurrent.CountDownLatch; // 倒计时门栓（等待多个线程完成）
import java.util.concurrent.Executors; // 执行器工厂类
import java.util.concurrent.ScheduledExecutorService; // 定时任务执行器服务
import java.util.concurrent.ScheduledFuture; // 定时任务Future
import java.util.concurrent.TimeUnit; // 时间单位枚举
import java.util.concurrent.atomic.AtomicBoolean; // 原子布尔值（线程安全）
import java.util.concurrent.atomic.AtomicLong; // 原子长整型（线程安全）


// =============== 21. Java 日期时间 ===============
import java.nio.charset.StandardCharsets; // 标准字符集（UTF-8、ISO-8859-1等）
import java.text.DateFormat; // 日期格式化抽象类
import java.text.DecimalFormat; // 十进制数格式化
import java.text.ParseException; // 解析异常
import java.text.SimpleDateFormat; // 简单日期格式化（yyyy-MM-dd等）
import java.util.Calendar; // 日历类（操作日期）
import java.util.Date; // 日期对象
import java.util.Locale; // 地区设置（语言、国家）
import java.util.TimeZone; // 时区


// =============== 22. Java 正则表达式 ===============
import java.util.Random; // 随机数生成器
import java.util.regex.Matcher; // 正则匹配器
import java.util.regex.Pattern; // 正则模式


// =============== 23. JSON 处理 ===============
import org.json.JSONArray; // JSON数组
import org.json.JSONException; // JSON异常
import org.json.JSONObject; // JSON对象


// =============== 24. QFun Hook 框架 ===============
import me.yxp.qfun.hook.api.OnGetRKey; // 获取RKey钩子接口
import me.yxp.qfun.hook.api.OnReceiveMsg; // 接收消息钩子注解
import me.yxp.qfun.hook.api.ReceiveMsgListener; // 接收消息监听器接口
import me.yxp.qfun.plugin.bean.MsgData; // 消息数据Bean对象
import me.yxp.qfun.utils.ToastUtils; // Toast工具类
import me.yxp.qfun.utils.hook.xpcompat.XC_MethodHook; // Xposed方法钩子兼容层
import me.yxp.qfun.utils.hook.xpcompat.XposedBridge; // Xposed桥接兼容层
import me.yxp.qfun.utils.hook.xpcompat.XposedHelpers;
import me.yxp.qfun.utils.hook.xpcompat.XC_MethodHook.MethodHookParam;
import me.yxp.qfun.hook.api.OnAIOViewUpdate;
import me.yxp.qfun.hook.api.AIOViewUpdateListener;
import me.yxp.qfun.utils.qq.FriendTool; // 好友工具类
import me.yxp.qfun.utils.qq.QQCurrentEnv; // QQ当前环境信息


// =============== 25. 图像处理进阶 ===============
import android.graphics.Bitmap.CompressFormat; // 位图压缩格式（JPEG、PNG、WEBP）
import android.graphics.Bitmap.Config; // 位图配置（ARGB_8888、RGB_565等）
import android.graphics.BitmapShader; // 位图着色器（用于填充图案）
import android.graphics.BlurMaskFilter; // 模糊遮罩滤镜
import android.graphics.ColorFilter; // 颜色滤镜基类
import android.graphics.ColorMatrix; // 颜色矩阵（调整饱和度、色调等）
import android.graphics.ColorMatrixColorFilter; // 颜色矩阵滤镜
import android.graphics.ComposeShader; // 组合着色器
import android.graphics.DashPathEffect; // 虚线路径效果
import android.graphics.ImageDecoder; // 图像解码器（Android 9.0+，替代BitmapFactory）
import android.graphics.LightingColorFilter; // 光照颜色滤镜
import android.graphics.LinearGradient; // 线性渐变
import android.graphics.MaskFilter; // 遮罩滤镜基类
import android.graphics.Path; // 路径对象（绘制复杂图形）
import android.graphics.PathEffect; // 路径效果基类
import android.graphics.PorterDuff; // Porter-Duff混合模式
import android.graphics.PorterDuffXfermode; // Porter-Duff传输模式（图层混合）
import android.graphics.RadialGradient; // 径向渐变
import android.graphics.Rect; // 矩形对象
import android.graphics.RectF; // 浮点矩形对象
import android.graphics.Region; // 区域对象
import android.graphics.Shader; // 着色器基类
import android.graphics.SweepGradient; // 扫描渐变（圆锥渐变）
import android.graphics.Xfermode; // 传输模式基类（图层混合）
import android.graphics.YuvImage; // YUV图像（相机预览格式）


// =============== 26. 视频处理 ===============
import android.media.MediaCodec; // 媒体编解码器（硬件加速编解码）
import android.media.MediaCodecInfo; // 媒体编解码器信息
import android.media.MediaCodecList; // 媒体编解码器列表
import android.media.MediaExtractor; // 媒体提取器（从容器中提取音视频轨道）
import android.media.MediaFormat; // 媒体格式（分辨率、帧率、比特率等）
import android.media.MediaMetadataRetriever; // 媒体元数据检索器（获取视频信息）
import android.media.MediaMuxer; // 媒体混合器（将音视频轨道合成容器）
import android.media.MediaPlayer.OnCompletionListener; // 播放完成监听器
import android.media.MediaPlayer.OnErrorListener; // 播放错误监听器
import android.media.MediaPlayer.OnPreparedListener; // 播放准备监听器
import android.media.MediaRecorder; // 媒体录制器（录制音视频）
import android.media.ThumbnailUtils; // 缩略图工具（视频帧提取）
import android.view.SurfaceView; // 表面视图（用于视频播放渲染）
import android.view.TextureView; // 纹理视图（用于视频播放渲染，支持动画）


// =============== 27. 音频处理 ===============
import android.media.AudioAttributes; // 音频属性（音频类型、用途）
import android.media.AudioFormat; // 音频格式（采样率、位深、声道）
import android.media.AudioManager; // 音频管理器（音量、音频焦点等）
import android.media.AudioRecord; // 音频录制器（原始PCM录音）
import android.media.AudioTrack; // 音频轨道（播放原始PCM音频）
import android.media.SoundPool; // 音效池（适合短音效，低延迟）
import android.media.audiofx.AudioEffect; // 音频效果基类
import android.media.audiofx.BassBoost; // 低音增强效果
import android.media.audiofx.Equalizer; // 均衡器效果
import android.media.audiofx.EnvironmentalReverb; // 环境混响效果
import android.media.audiofx.LoudnessEnhancer; // 响度增强器（Android 4.4+）
import android.media.audiofx.Virtualizer; // 虚拟环绕声效果


// =============== 28. 线程与异步 - 基础 ===============
import java.lang.Thread; // 线程类
import java.lang.ThreadLocal; // 线程局部变量
import java.util.concurrent.Callable; // 可调用接口（有返回值的任务）
import java.util.concurrent.ExecutorService; // 执行器服务接口
import java.util.concurrent.Future; // 异步任务结果Future
import java.util.concurrent.FutureTask; // Future任务实现
import java.util.concurrent.ThreadFactory; // 线程工厂接口
import java.util.concurrent.ThreadPoolExecutor; // 线程池执行器
import java.util.concurrent.TimeoutException; // 超时异常
// 核心 Future 接口及基本实现
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

// 异步增强：CompletableFuture 及相关接口
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;

// 任务提交与执行器
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorCompletionService;

// Fork/Join 框架中的 Future 实现
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.RecursiveAction;

// 时间与异常类
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;

// 其他辅助类
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.lang.CharSequence;
// =============== 29. 线程与异步 - 线程池 ===============
import java.util.concurrent.ArrayBlockingQueue; // 有界阻塞队列（数组实现）
import java.util.concurrent.BlockingQueue; // 阻塞队列接口
import java.util.concurrent.LinkedBlockingQueue; // 有界阻塞队列（链表实现）
import java.util.concurrent.PriorityBlockingQueue; // 优先级阻塞队列
import java.util.concurrent.RejectedExecutionHandler; // 拒绝策略处理器
import java.util.concurrent.SynchronousQueue; // 同步队列（无容量，直接交接）
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy; // 中止策略（默认，抛异常）
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy; // 调用者运行策略
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy; // 丢弃最老任务策略
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy; // 丢弃策略


// =============== 30. 线程与异步 - 并发工具进阶 ===============
import java.util.concurrent.ConcurrentLinkedQueue; // 线程安全无界队列
import java.util.concurrent.CopyOnWriteArrayList; // 写时复制ArrayList（适合读多写少）
import java.util.concurrent.CopyOnWriteArraySet; // 写时复制Set
import java.util.concurrent.CyclicBarrier; // 循环栅栏（等待所有线程到达屏障点）
import java.util.concurrent.Exchanger; // 交换器（两个线程交换数据）
import java.util.concurrent.Phaser; // 相位器（多阶段同步）
import java.util.concurrent.Semaphore; // 信号量（控制并发访问数量）
import java.util.concurrent.locks.Condition; // 条件变量
import java.util.concurrent.locks.Lock; // 锁接口
import java.util.concurrent.locks.ReentrantLock; // 可重入锁
import java.util.concurrent.locks.ReentrantReadWriteLock; // 可重入读写锁
import java.util.concurrent.locks.ReadWriteLock; // 读写锁接口


// =============== 31. Android 异步处理 ===============
import android.os.AsyncTask; // 异步任务（已废弃，建议用Executor）
import android.os.HandlerThread; // Handler线程（带Looper的工作线程）
import android.os.Message; // 消息对象
import android.os.MessageQueue; // 消息队列


// =============== 32. UI 组件进阶 - RecyclerView ===============
import androidx.recyclerview.widget.RecyclerView; // 回收视图（高性能列表）
import androidx.recyclerview.widget.RecyclerView.Adapter; // RecyclerView适配器
import androidx.recyclerview.widget.RecyclerView.ViewHolder; // RecyclerView视图持有者
import androidx.recyclerview.widget.LinearLayoutManager; // 线性布局管理器
import androidx.recyclerview.widget.GridLayoutManager; // 网格布局管理器
import androidx.recyclerview.widget.StaggeredGridLayoutManager; // 瀑布流布局管理器
import androidx.recyclerview.widget.DividerItemDecoration; // 分割线装饰
import androidx.recyclerview.widget.ItemTouchHelper; // 拖拽/滑动删除助手
import androidx.recyclerview.widget.RecyclerView.ItemDecoration; // 列表项装饰基类
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;


// =============== 33. UI 组件进阶 - ViewPager ===============
import androidx.viewpager.widget.ViewPager; // 视图页面滑动器
import androidx.viewpager.widget.PagerAdapter; // 页面适配器基类
import androidx.viewpager2.widget.ViewPager2; // ViewPager2（新版本，支持竖向滑动）
import com.google.android.material.tabs.TabLayout; // Material Design标签布局
import com.google.android.material.tabs.TabLayoutMediator; // TabLayout与ViewPager2绑定


// =============== 34. UI 组件进阶 - ConstraintLayout ===============
import androidx.constraintlayout.widget.ConstraintLayout; // 约束布局（强大的相对布局）
import androidx.constraintlayout.widget.ConstraintSet; // 约束集合（动态修改约束）
import androidx.constraintlayout.widget.Guideline; // 辅助线


// =============== 35. UI 组件进阶 - CoordinatorLayout ===============
import androidx.coordinatorlayout.widget.CoordinatorLayout; // 协调布局（协调子视图行为）
import com.google.android.material.appbar.AppBarLayout; // 应用栏布局
import com.google.android.material.appbar.CollapsingToolbarLayout; // 可折叠工具栏布局
import com.google.android.material.floatingactionbutton.FloatingActionButton; // 浮动操作按钮


// =============== 36. UI 组件进阶 - Material Design ===============
import com.google.android.material.button.MaterialButton; // Material按钮
import com.google.android.material.card.MaterialCardView; // Material卡片视图
import com.google.android.material.chip.Chip; // 碎片控件（标签）
import com.google.android.material.chip.ChipGroup; // 碎片组
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Material对话框构建器
import com.google.android.material.snackbar.Snackbar; // Snackbar提示（底部弹出）
import com.google.android.material.textfield.TextInputLayout; // 文本输入布局（带浮动标签）
import com.google.android.material.textfield.TextInputEditText; // 文本输入编辑框


// =============== 37. UI 动画进阶 ===============
import android.animation.Animator; // 动画基类
import android.animation.AnimatorListenerAdapter; // 动画监听器适配器
import android.animation.AnimatorSet; // 动画集合（组合多个动画）
import android.animation.ObjectAnimator; // 对象动画器（属性动画）
import android.animation.PropertyValuesHolder; // 属性值持有者（多属性动画）
import android.animation.TimeInterpolator; // 时间插值器接口
import android.animation.ValueAnimator; // 值动画器（数值变化动画）
import android.transition.Transition; // 过渡动画基类（Activity/Fragment切换）
import android.transition.TransitionManager; // 过渡管理器
import android.view.animation.AnimationUtils; // 动画工具类
import android.view.animation.BounceInterpolator; // 弹跳插值器
import android.view.animation.DecelerateInterpolator; // 减速插值器
import android.view.animation.LinearInterpolator; // 线性插值器
import android.view.animation.OvershootInterpolator; // 超越插值器
import android.view.animation.RotateAnimation; // 旋转动画
import android.view.animation.TranslateAnimation; // 平移动画


// =============== 38. 触摸手势进阶 ===============
import android.view.GestureDetector.OnDoubleTapListener; // 双击监听器
import android.view.GestureDetector.OnGestureListener; // 手势监听器
import android.view.GestureDetector.SimpleOnGestureListener; // 简单手势监听器
import android.view.ScaleGestureDetector; // 缩放手势检测器
import android.view.VelocityTracker; // 速度追踪器（计算滑动速度）
import android.view.ViewConfiguration; // 视图配置（触摸阈值等）


// =============== 39. 自定义 View ===============
import android.content.res.TypedArray; // 类型数组（读取自定义属性）
import android.util.AttributeSet; // 属性集合
import android.view.ViewTreeObserver; // 视图树观察者（监听布局变化）
import android.view.ViewTreeObserver.OnGlobalLayoutListener; // 全局布局监听器
import android.view.ViewTreeObserver.OnPreDrawListener; // 预绘制监听器


// =============== 40. Fragment 支持 ===============
import androidx.fragment.app.Fragment; // Fragment基类
import androidx.fragment.app.FragmentActivity; // Fragment Activity
import androidx.fragment.app.FragmentManager; // Fragment管理器
import androidx.fragment.app.FragmentTransaction; // Fragment事务


// =============== 41. 权限处理 ===============
import androidx.core.app.ActivityCompat; // Activity兼容类（请求权限）
import androidx.core.content.ContextCompat; // Context兼容类（检查权限）
import android.content.pm.PackageManager.NameNotFoundException; // 包未找到异常
import android.Manifest; // 权限清单常量


// =============== 42. 通知处理 ===============
import android.app.Notification; // 通知对象
import android.app.NotificationChannel; // 通知渠道（Android 8.0+）
import android.app.NotificationManager; // 通知管理器
import android.app.PendingIntent; // 待处理意图
import androidx.core.app.NotificationCompat; // 通知兼容类


// =============== 43. 数据库支持 ===============
import android.database.Cursor; // 数据库游标
import android.database.sqlite.SQLiteDatabase; // SQLite数据库
import android.database.sqlite.SQLiteOpenHelper; // SQLite打开助手


// =============== 44. ContentProvider 支持 ===============
import android.content.ContentProvider; // 内容提供者基类
import android.content.ContentResolver; // 内容解析器
import android.content.ContentValues; // 内容值（插入/更新数据）


// =============== 45. 广播接收器 ===============
import android.content.BroadcastReceiver; // 广播接收器基类
import android.content.LocalBroadcastManager; // 本地广播管理器（应用内广播）


// =============== 46. 服务组件 ===============
import android.app.Service; // 服务基类
import android.app.IntentService; // 意图服务（在工作线程处理）
import android.content.ServiceConnection; // 服务连接接口


// =============== 47. 相机相关 ===============
import android.hardware.Camera; // 相机类（已废弃，建议用Camera2）
import android.hardware.camera2.CameraAccessException; // 相机访问异常
import android.hardware.camera2.CameraCharacteristics; // 相机特性
import android.hardware.camera2.CameraDevice; // 相机设备
import android.hardware.camera2.CameraManager; // 相机管理器
import android.hardware.camera2.CaptureRequest; // 捕获请求


// =============== 48. 传感器相关 ===============
import android.hardware.Sensor; // 传感器对象
import android.hardware.SensorEvent; // 传感器事件
import android.hardware.SensorEventListener; // 传感器事件监听器
import android.hardware.SensorManager; // 传感器管理器


// =============== 49. 蓝牙相关 ===============
import android.bluetooth.BluetoothAdapter; // 蓝牙适配器
import android.bluetooth.BluetoothDevice; // 蓝牙设备
import android.bluetooth.BluetoothGatt; // 蓝牙GATT（低功耗蓝牙）
import android.bluetooth.BluetoothSocket; // 蓝牙Socket


// =============== 50. NFC 相关 ===============
import android.nfc.NfcAdapter; // NFC适配器
import android.nfc.NfcManager; // NFC管理器
import android.nfc.Tag; // NFC标签
