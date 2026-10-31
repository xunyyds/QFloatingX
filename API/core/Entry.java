public void openPlugin(int functionType, String groupId, String userName) {
    跳转到页面("me.yxp.qfun.activity.PluginActivity");
}

public void openSetting(int functionType, String groupId, String userName) {
    if (!apiPrefetchDone) Toast("设置页面加载中，请稍候");
    ensureAllApi();
    Activity act = safeActivity();
    if (act != null) {
        final Activity fa = act;
        final int fChatType = functionType;
        final String fPeerUin = (groupId != null) ? groupId : "";
        final String fPeerName = (userName != null) ? userName : "";
        fa.runOnUiThread(new Runnable() {
            public void run() { launchSettingsActivity(fa, fChatType, fPeerUin, fPeerName); }
        });
    }
}
void 跳转到页面(String className) {
    Activity activity = safeActivity();
    if (activity == null) return;
    
    Intent intent = new Intent();
    intent.setAction(Intent.ACTION_MAIN);
    intent.setComponent(new ComponentName(currentPackageName, className));
    try {
        activity.startActivity(intent);
    } catch (Exception e) {
        traceLog("api_log","[跳转到页面] 跳转页面失败: " + e);
    }
}
boolean isFilePickerHooked = false;
java.util.concurrent.ConcurrentHashMap filePickerTasks = new java.util.concurrent.ConcurrentHashMap();
interface FilePickerCallback {
    void onFilePicked(Activity activity, Uri uri, String fileName, String filePath);
}

class FilePickerTask {
    String savePath;
    FilePickerCallback callback;
}
void openFilePicker(Activity activity, int requestCode, String mimeType, String[] extraMimeTypes, String savePath, FilePickerCallback callback) {
    try {
        if (callback != null) {
            FilePickerTask task = new FilePickerTask();
            task.savePath = savePath;
            task.callback = callback;
            filePickerTasks.put(Integer.valueOf(requestCode), task);
        }
        ensureFilePickerHook();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        if (extraMimeTypes != null && extraMimeTypes.length > 0) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
        } else if (mimeType != null) {
            intent.setType(mimeType);
        } else {
            intent.setType("*/*");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, requestCode);
    } catch (Throwable e) {
        traceLog("api_log", "[openFilePicker] 失败: " + e);
        Toast("文件选择启动失败: " + e.getMessage());
    }
}
void ensureFilePickerHook() {
    if (isFilePickerHooked) return;
    synchronized (this) {
    if (isFilePickerHooked) return;
    try {
        Class activityClass = Class.forName("android.app.Activity");
        int hookedCount = hookAll("file_picker", activityClass, "dispatchActivityResult", new XC_MethodHook() {
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                    ThreadPool.execute(new Runnable() {
                        public void run() {
                            try {
                                if (param.args == null || param.args.length < 4) return;
                                if (!(param.args[1] instanceof Integer) || !(param.args[2] instanceof Integer)) return;
                                Activity act = (Activity) param.thisObject;
                                int rc = ((Integer) param.args[1]).intValue();
                                int resultCode = ((Integer) param.args[2]).intValue();
                                Intent data = (Intent) param.args[3];
                                if (resultCode != Activity.RESULT_OK || data == null) return;
                                Uri uri = data.getData();
                                if (uri == null) return;
                                FilePickerTask task = (FilePickerTask) filePickerTasks.remove(Integer.valueOf(rc));
                                if (task == null) return;
                                String fileName = null;
                                try {
                                    if ("content".equals(uri.getScheme())) {
                                        android.database.Cursor cursor = act.getContentResolver().query(uri, null, null, null, null);
                                        if (cursor != null) {
                                            try {
                                                if (cursor.moveToFirst()) {
                                                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                                                    if (index >= 0) fileName = cursor.getString(index);
                                                }
                                            } finally {
                                                cursor.close();
                                            }
                                        }
                                    }
                                } catch (Throwable e) { traceLog("api_log", "[afterHookedMethod] 异常: " + e); }
                                if (fileName == null || fileName.length() == 0) {
                                    try {
                                        String path = uri.getPath();
                                        if (path != null) {
                                            int cut = path.lastIndexOf('/');
                                            if (cut != -1) fileName = path.substring(cut + 1);
                                        }
                                    } catch (Throwable e) { traceLog("api_log", "[afterHookedMethod] 异常: " + e); }
                                }
                                String filePath = null;
                                if (task.savePath != null) {
                                    String type = act.getContentResolver().getType(uri);
                                    String ext = getExtensionFromMimeType(type);
                                    if (task.savePath.contains("{ext}")) {
                                        filePath = task.savePath.replace("{ext}", ext);
                                    } else if (task.savePath.lastIndexOf('.') > 0) {
                                        filePath = task.savePath.substring(0, task.savePath.lastIndexOf('.')) + ext;
                                    } else {
                                        filePath = task.savePath + ext;
                                    }
                                }
                                task.callback.onFilePicked(act, uri, fileName, filePath);
                            } catch (Throwable e) {
                                traceLog("api_log", "[dispatch] 异常: " + e);
                            }
                        }
                    });
                }
            });
        if (hookedCount == 0) {
            showApiProtectGuide(safeActivity());
            return;
        }
        isFilePickerHooked = true;
    } catch (Throwable e) {
        traceLog("api_log", "[hook] 失败: " + e);
        isFilePickerHooked = false;
        showApiProtectGuide(safeActivity());
    }
    }
}
