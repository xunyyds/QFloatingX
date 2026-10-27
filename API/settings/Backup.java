int qfxSettingsBackupExportRequestCode = 10581;
int qfxSettingsBackupImportRequestCode = 10582;
boolean qfxSettingsBackupExportHooked = false;
byte[] qfxSettingsBackupPendingArchive = null;
Object qfxSettingsBackupLock = new Object();

void showSettingsBackupDialog(final Activity activity) {
    if (activity == null) return;
    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("配置备份与导入");
    builder.setMessage("备份范围包括全部配置文件、热插拔脚本源码与脚本配置、HTML 自定义文件，以及 API 目录中的图片资源。导入时会先展示新增、覆盖和移除内容，确认后才执行。");
    builder.setNeutralButton("创建备份", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            startSettingsBackupExport(activity);
        }
    });
    builder.setPositiveButton("导入备份", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            startSettingsBackupImport(activity);
        }
    });
    builder.setNegativeButton("关闭", null);
    AlertDialog dialog = builder.show();
    applyUiTheme(activity, dialog, 0);
}
void startSettingsBackupExport(final Activity activity) {
    if (activity == null) return;
    Toast("正在整理配置与脚本...");
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                final byte[] archive = buildSettingsBackupArchive();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            ensureSettingsBackupExportHook();
                            if (!qfxSettingsBackupExportHooked) {
                                Toast("无法初始化备份文件选择器");
                                return;
                            }
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/zip");
                            String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                            intent.putExtra(Intent.EXTRA_TITLE, "QFloatingX-配置备份-" + stamp + ".zip");
                            synchronized (qfxSettingsBackupLock) {
                                qfxSettingsBackupPendingArchive = archive;
                            }
                            activity.startActivityForResult(intent, qfxSettingsBackupExportRequestCode);
                        } catch (Throwable e) {
                            synchronized (qfxSettingsBackupLock) { qfxSettingsBackupPendingArchive = null; }
                            traceLog("setwindow_log", "[配置备份] 打开保存窗口失败: " + e);
                            Toast("打开保存窗口失败: " + e.getMessage());
                        }
                    }
                });
            } catch (Throwable e) {
                traceLog("setwindow_log", "[配置备份] 生成备份失败: " + e);
                activity.runOnUiThread(new Runnable() {
                    public void run() { Toast("生成备份失败，请查看日志"); }
                });
            }
        }
    });
}
void ensureSettingsBackupExportHook() {
    if (qfxSettingsBackupExportHooked) return;
    try {
        int hooked = hookAll("settings_backup", Activity.class, "dispatchActivityResult", new XC_MethodHook() {
                    protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                        try {
                            if (param.args == null || param.args.length < 4 || !(param.args[1] instanceof Integer) || !(param.args[2] instanceof Integer)) return;
                            if (((Integer) param.args[1]).intValue() != qfxSettingsBackupExportRequestCode) return;
                            final Activity resultActivity = (Activity) param.thisObject;
                            int resultCode = ((Integer) param.args[2]).intValue();
                            final Intent resultIntent = (Intent) param.args[3];
                            final byte[] archive;
                            synchronized (qfxSettingsBackupLock) {
                                archive = qfxSettingsBackupPendingArchive;
                                qfxSettingsBackupPendingArchive = null;
                            }
                            if (resultCode != Activity.RESULT_OK || resultIntent == null || resultIntent.getData() == null || archive == null) return;
                            final Uri uri = resultIntent.getData();
                            ThreadPool.execute(new Runnable() {
                                public void run() {
                                    OutputStream output = null;
                                    try {
                                        output = resultActivity.getContentResolver().openOutputStream(uri);
                                        if (output == null) throw new IOException("无法打开目标文件");
                                        output.write(archive);
                                        output.flush();
                                        final int size = archive.length;
                                        resultActivity.runOnUiThread(new Runnable() {
                                            public void run() { Toast("备份已保存，大小 " + (size / 1024) + " KB"); }
                                        });
                                    } catch (final Throwable e) {
                                        traceLog("setwindow_log", "[配置备份] 写入备份失败: " + e);
                                        resultActivity.runOnUiThread(new Runnable() {
                                            public void run() { Toast("保存备份失败: " + e.getMessage()); }
                                        });
                                    } finally {
                                        if (output != null) try { output.close(); } catch (Throwable ignore) {}
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            traceLog("setwindow_log", "[配置备份] 处理保存结果失败: " + e);
                        }
                    }
                });
        qfxSettingsBackupExportHooked = hooked > 0;
    } catch (Throwable e) {
        traceLog("setwindow_log", "[配置备份] 初始化结果回调失败: " + e);
        qfxSettingsBackupExportHooked = false;
    }
}
byte[] buildSettingsBackupArchive() throws Exception {
    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes);
    int[] counts = new int[]{0, 0, 0, 0};
    try {
        qfxAddSettingsBackupDirectory(zip, new File(pluginPath + "/config"), "config", 0, counts);
        qfxAddSettingsBackupDirectory(zip, new File(pluginPath + "/HotPlug/Funcs"), "HotPlug/Funcs", 1, counts);
        qfxAddSettingsBackupDirectory(zip, new File(pluginPath + "/API"), "API", 2, counts);
        qfxAddSettingsBackupDirectory(zip, new File(pluginPath + "/HTML"), "HTML", 3, counts);
        JSONObject manifest = new JSONObject();
        manifest.put("format", "QFloatingXBackup");
        manifest.put("version", 1);
        manifest.put("createdAt", System.currentTimeMillis());
        manifest.put("configFiles", counts[0]);
        manifest.put("scriptFiles", counts[1]);
        manifest.put("assetFiles", counts[2]);
        manifest.put("htmlFiles", counts[3]);
        zip.putNextEntry(new java.util.zip.ZipEntry("manifest.json"));
        zip.write(manifest.toString().getBytes("UTF-8"));
        zip.closeEntry();
        zip.finish();
    } finally {
        try { zip.close(); } catch (Throwable ignore) {}
    }
    return bytes.toByteArray();
}

void qfxAddSettingsBackupDirectory(java.util.zip.ZipOutputStream zip, File root, String prefix, int kind, int[] counts) throws Exception {
    if (root == null || !root.exists() || !root.isDirectory()) return;
    File[] children = root.listFiles();
    if (children == null) return;
    for (int i = 0; i < children.length; i++) {
        File child = children[i];
        if (child == null) continue;
        String entryName = prefix + "/" + child.getName();
        if (child.isDirectory()) {
            qfxAddSettingsBackupDirectory(zip, child, entryName, kind, counts);
            continue;
        }
        if (!child.isFile()) continue;
        if (kind == 2 && !qfxIsSettingsBackupImage(child.getName())) continue;
        java.io.ByteArrayOutputStream fileBytes = new java.io.ByteArrayOutputStream();
        FileInputStream fileInput = null;
        try {
            fileInput = new FileInputStream(child);
            byte[] fileBuffer = new byte[8192];
            int fileLength;
            while ((fileLength = fileInput.read(fileBuffer)) != -1) fileBytes.write(fileBuffer, 0, fileLength);
        } finally {
            if (fileInput != null) try { fileInput.close(); } catch (Throwable ignore) {}
        }
        byte[] data = fileBytes.toByteArray();
        zip.putNextEntry(new java.util.zip.ZipEntry(entryName));
        zip.write(data);
        zip.closeEntry();
        counts[kind]++;
    }
}

boolean qfxIsSettingsBackupImage(String name) {
    if (name == null) return false;
    String lower = name.toLowerCase(java.util.Locale.ROOT);
    return lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".bmp");
}

void startSettingsBackupImport(final Activity activity) {
    if (activity == null) return;
    openFilePicker(activity, qfxSettingsBackupImportRequestCode, "*/*", null, null, new FilePickerCallback() {
        public void onFilePicked(final Activity pickedActivity, Uri uri, String fileName, String filePath) {
            try {
                byte[] raw = qfxReadSettingsBackupUri(pickedActivity, uri);
                final HashMap backupFiles = qfxParseSettingsBackupArchive(raw);
                final String displayName = fileName == null ? "所选文件" : fileName;
                pickedActivity.runOnUiThread(new Runnable() {
                    public void run() { showSettingsBackupPreview(pickedActivity, displayName, backupFiles); }
                });
            } catch (final Throwable e) {
                traceLog("setwindow_log", "[配置导入] 解析备份失败: " + e);
                pickedActivity.runOnUiThread(new Runnable() {
                    public void run() { Toast("备份文件无效或无法读取: " + e.getMessage()); }
                });
            }
        }
    });
}

byte[] qfxReadSettingsBackupUri(Activity activity, Uri uri) throws Exception {
    InputStream input = null;
    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    try {
        input = activity.getContentResolver().openInputStream(uri);
        if (input == null) throw new IOException("无法读取所选文件");
        byte[] buffer = new byte[8192];
        int length;
        long total = 0;
        while ((length = input.read(buffer)) != -1) {
            total += length;
            if (total > 50L * 1024L * 1024L) throw new IOException("备份文件超过 50 MB");
            bytes.write(buffer, 0, length);
        }
    } finally {
        if (input != null) try { input.close(); } catch (Throwable ignore) {}
    }
    return bytes.toByteArray();
}

HashMap qfxParseSettingsBackupArchive(byte[] raw) throws Exception {
    if (raw == null || raw.length == 0) throw new IOException("文件为空");
    HashMap files = new HashMap();
    java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(raw));
    long total = 0;
    int count = 0;
    try {
        java.util.zip.ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();
            if (name == null) throw new IOException("备份中存在无效路径");
            name = name.replace('\\', '/');
            if (name.startsWith("/") || name.indexOf(':') >= 0) throw new IOException("备份包含不安全的路径");
            String[] parts = name.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].length() == 0 || parts[i].equals("..") || parts[i].equals(".")) throw new IOException("备份包含不安全的路径");
            }
            if (entry.isDirectory()) {
                zip.closeEntry();
                continue;
            }
            if (!qfxIsSettingsBackupPathAllowed(name)) throw new IOException("备份包含不支持的文件: " + name);
            java.io.ByteArrayOutputStream item = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            long itemSize = 0;
            while ((length = zip.read(buffer)) != -1) {
                itemSize += length;
                total += length;
                if (itemSize > 25L * 1024L * 1024L || total > 100L * 1024L * 1024L) throw new IOException("备份解压后体积过大");
                item.write(buffer, 0, length);
            }
            if (files.containsKey(name)) throw new IOException("备份包含重复文件: " + name);
            files.put(name, item.toByteArray());
            count++;
            if (count > 1000) throw new IOException("备份文件数量过多");
            zip.closeEntry();
        }
    } finally {
        try { zip.close(); } catch (Throwable ignore) {}
    }
    Object manifestRaw = files.get("manifest.json");
    if (!(manifestRaw instanceof byte[])) throw new IOException("缺少备份清单");
    JSONObject manifest = new JSONObject(new String((byte[]) manifestRaw, "UTF-8"));
    if (!"QFloatingXBackup".equals(manifest.optString("format", "")) || manifest.optInt("version", 0) != 1) throw new IOException("备份格式或版本不受支持");
    if (files.size() <= 1) throw new IOException("备份中没有可导入的内容");
    return files;
}
boolean qfxIsSettingsBackupPathAllowed(String path) {
    if (path == null || path.length() == 0) return false;
    if (path.equals("manifest.json")) return true;
    if (path.startsWith("config/") || path.startsWith("HotPlug/Funcs/") || path.startsWith("HTML/")) return true;
    if (path.startsWith("API/")) return qfxIsSettingsBackupImage(path.substring(path.lastIndexOf('/') + 1));
    return false;
}
void showSettingsBackupPreview(final Activity activity, String fileName, final HashMap backupFiles) {
    if (activity == null || backupFiles == null) return;
    int configNew = 0;
    int configOverwrite = 0;
    int scriptNew = 0;
    int scriptOverwrite = 0;
    int assetNew = 0;
    int assetOverwrite = 0;
    int htmlNew = 0;
    int htmlOverwrite = 0;
    HashSet incoming = new HashSet();
    StringBuilder detail = new StringBuilder();
    java.util.Iterator iterator = backupFiles.keySet().iterator();
    int listed = 0;
    while (iterator.hasNext()) {
        String path = String.valueOf(iterator.next());
        if (path.equals("manifest.json")) continue;
        incoming.add(path);
        File target = new File(pluginPath + "/" + path);
        boolean exists = target.exists();
        if (path.startsWith("config/")) {
            if (exists) configOverwrite++; else configNew++;
        } else if (path.startsWith("HotPlug/Funcs/")) {
            if (exists) scriptOverwrite++; else scriptNew++;
        } else if (path.startsWith("API/")) {
            if (exists) assetOverwrite++; else assetNew++;
        } else if (path.startsWith("HTML/")) {
            if (exists) htmlOverwrite++; else htmlNew++;
        }
        if (listed < 80) {
            detail.append(exists ? "覆盖  " : "新增  ").append(path).append("\n");
            listed++;
        }
    }
    HashSet stale = new HashSet();
    qfxCollectSettingsBackupStaleFiles(new File(pluginPath + "/config"), "config", incoming, stale);
    qfxCollectSettingsBackupStaleFiles(new File(pluginPath + "/HotPlug/Funcs"), "HotPlug/Funcs", incoming, stale);
    qfxCollectSettingsBackupStaleFiles(new File(pluginPath + "/HTML"), "HTML", incoming, stale);
    int staleConfig = 0;
    int staleScripts = 0;
    int staleHtml = 0;
    java.util.Iterator staleIterator = stale.iterator();
    while (staleIterator.hasNext()) {
        String path = String.valueOf(staleIterator.next());
        if (path.startsWith("config/")) staleConfig++;
        else if (path.startsWith("HotPlug/Funcs/")) staleScripts++;
        else if (path.startsWith("HTML/")) staleHtml++;
    }
    if (listed < incoming.size()) detail.append("其余 ").append(incoming.size() - listed).append(" 个文件省略\n");
    if (!stale.isEmpty()) {
        detail.append("\n将移除本地但备份中没有的文件：\n");
        staleIterator = stale.iterator();
        int removedListed = 0;
        while (staleIterator.hasNext() && removedListed < 40) {
            detail.append("移除  ").append(String.valueOf(staleIterator.next())).append("\n");
            removedListed++;
        }
        if (stale.size() > removedListed) detail.append("其余 ").append(stale.size() - removedListed).append(" 个文件省略\n");
    }
    StringBuilder message = new StringBuilder();
    message.append("文件：").append(fileName == null ? "所选备份" : fileName).append("\n\n");
    message.append("配置文件：新增 ").append(configNew).append("，覆盖 ").append(configOverwrite).append("，移除 ").append(staleConfig).append("\n");
    message.append("热插拔脚本：新增 ").append(scriptNew).append("，覆盖 ").append(scriptOverwrite).append("，移除 ").append(staleScripts).append("\n");
    message.append("图片资源：新增 ").append(assetNew).append("，覆盖 ").append(assetOverwrite).append("\n");
    message.append("HTML 自定义文件：新增 ").append(htmlNew).append("，覆盖 ").append(htmlOverwrite).append("，移除 ").append(staleHtml).append("\n\n");
    message.append("确认后会恢复备份中的配置、脚本和 HTML 文件；备份未包含的本地配置文件、脚本及 HTML 文件会被移除，未包含的图片资源会保留。\n\n");
    message.append(detail.toString());
    AlertDialog.Builder builder = new AlertDialog.Builder(activity, isThemeDark(activity) ? AlertDialog.THEME_DEVICE_DEFAULT_DARK : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
    builder.setTitle("导入预览");
    builder.setMessage(message.toString());
    builder.setNegativeButton("取消", null);
    builder.setPositiveButton("确认导入", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            applySettingsBackup(activity, backupFiles);
        }
    });
    AlertDialog dialog = builder.show();
    applyUiTheme(activity, dialog, 0);
}

void qfxCollectSettingsBackupStaleFiles(File root, String prefix, HashSet incoming, HashSet stale) {
    if (root == null || !root.exists() || !root.isDirectory()) return;
    File[] children = root.listFiles();
    if (children == null) return;
    for (int i = 0; i < children.length; i++) {
        File child = children[i];
        if (child == null) continue;
        if (child.isDirectory()) {
            qfxCollectSettingsBackupStaleFiles(child, prefix + "/" + child.getName(), incoming, stale);
        } else if (child.isFile()) {
            String path = prefix + "/" + child.getName();
            if (!incoming.contains(path)) stale.add(path);
        }
    }
}
void applySettingsBackup(final Activity activity, final HashMap backupFiles) {
    if (activity == null || backupFiles == null) return;
    Toast("正在导入配置与脚本...");
    ThreadPool.execute(new Runnable() {
        public void run() {
            HashMap original = new HashMap();
            HashSet incoming = new HashSet(backupFiles.keySet());
            incoming.remove("manifest.json");
            HashSet stale = new HashSet();
            qfxCollectSettingsBackupStaleFiles(new File(pluginPath + "/config"), "config", incoming, stale);
            qfxCollectSettingsBackupStaleFiles(new File(pluginPath + "/HotPlug/Funcs"), "HotPlug/Funcs", incoming, stale);
            qfxCollectSettingsBackupStaleFiles(new File(pluginPath + "/HTML"), "HTML", incoming, stale);
            String error = null;
            try {
                String[] oldFuncs = getAll();
                try { stopAllThreads(); } catch (Throwable e) { traceLog("setwindow_log", "[配置导入] 停止脚本失败: " + e); }
                for (int i = 0; i < oldFuncs.length; i++) {
                    try { if (scriptLoader != null) scriptLoader.remove(oldFuncs[i]); } catch (Throwable ignore) {}
                }
                java.util.Iterator iterator = incoming.iterator();
                while (iterator.hasNext()) {
                    String path = String.valueOf(iterator.next());
                    File current = new File(pluginPath + "/" + path);
                    if (current.exists()) {
                        java.io.ByteArrayOutputStream fileBytes = new java.io.ByteArrayOutputStream();
                        FileInputStream fileInput = null;
                        try {
                            fileInput = new FileInputStream(current);
                            byte[] fileBuffer = new byte[8192];
                            int fileLength;
                            while ((fileLength = fileInput.read(fileBuffer)) != -1) fileBytes.write(fileBuffer, 0, fileLength);
                        } finally {
                            if (fileInput != null) try { fileInput.close(); } catch (Throwable ignore) {}
                        }
                        original.put(path, fileBytes.toByteArray());
                    } else original.put(path, null);
                }
                iterator = stale.iterator();
                while (iterator.hasNext()) {
                    String path = String.valueOf(iterator.next());
                    if (original.containsKey(path)) continue;
                    File current = new File(pluginPath + "/" + path);
                    if (current.exists()) {
                        java.io.ByteArrayOutputStream fileBytes = new java.io.ByteArrayOutputStream();
                        FileInputStream fileInput = null;
                        try {
                            fileInput = new FileInputStream(current);
                            byte[] fileBuffer = new byte[8192];
                            int fileLength;
                            while ((fileLength = fileInput.read(fileBuffer)) != -1) fileBytes.write(fileBuffer, 0, fileLength);
                        } finally {
                            if (fileInput != null) try { fileInput.close(); } catch (Throwable ignore) {}
                        }
                        original.put(path, fileBytes.toByteArray());
                    } else original.put(path, null);
                }
                iterator = incoming.iterator();
                while (iterator.hasNext()) {
                    String path = String.valueOf(iterator.next());
                    Object value = backupFiles.get(path);
                    if (!(value instanceof byte[])) throw new IOException("备份文件内容无效: " + path);
                    File target = new File(pluginPath + "/" + path);
                    File parent = target.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) throw new IOException("无法创建目录: " + parent.getAbsolutePath());
                    FileOutputStream fileOutput = null;
                    try {
                        fileOutput = new FileOutputStream(target, false);
                        fileOutput.write((byte[]) value);
                        fileOutput.flush();
                    } finally {
                        if (fileOutput != null) try { fileOutput.close(); } catch (Throwable ignore) {}
                    }
                }
                iterator = stale.iterator();
                while (iterator.hasNext()) {
                    String path = String.valueOf(iterator.next());
                    File current = new File(pluginPath + "/" + path);
                    if (current.exists() && !current.delete()) throw new IOException("无法移除旧文件: " + path);
                }
            } catch (Throwable e) {
                error = e.getMessage() == null ? String.valueOf(e) : e.getMessage();
                traceLog("setwindow_log", "[配置导入] 写入失败，尝试回滚: " + e);
                java.util.Iterator rollback = original.keySet().iterator();
                while (rollback.hasNext()) {
                    String path = String.valueOf(rollback.next());
                    try {
                        Object old = original.get(path);
                        File target = new File(pluginPath + "/" + path);
                        if (old instanceof byte[]) {
                            File parent = target.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();
                            FileOutputStream fileOutput = null;
                            try {
                                fileOutput = new FileOutputStream(target, false);
                                fileOutput.write((byte[]) old);
                                fileOutput.flush();
                            } finally {
                                if (fileOutput != null) try { fileOutput.close(); } catch (Throwable ignore) {}
                            }
                        }
                        else if (target.exists()) target.delete();
                    } catch (Throwable rollbackError) {
                        traceLog("setwindow_log", "[配置导入] 回滚失败 " + path + ": " + rollbackError);
                    }
                }
            }
            try { qfxReloadSettingsBackupHotplug(); } catch (Throwable e) { traceLog("setwindow_log", "[配置导入] 刷新脚本状态失败: " + e); }
            final String resultError = error;
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (resultError == null) {
                        Toast("配置导入完成");
                        try { showSettingsMenu(activity, null, null, null); } catch (Throwable e) { traceLog("setwindow_log", "[配置导入] 刷新设置页失败: " + e); }
                    } else {
                        Toast("导入失败，已尝试恢复原文件：" + resultError);
                    }
                }
            });
        }
    });
}

void qfxReloadSettingsBackupHotplug() {
    try { if (loadFlags != null) loadFlags.clear(); } catch (Throwable ignore) {}
    try { if (runFlags != null) runFlags.clear(); } catch (Throwable ignore) {}
    try { if (loopFlags != null) loopFlags.clear(); } catch (Throwable ignore) {}
    rebuildRegistry();
    String[] funcs = getAll();
    for (int i = 0; i < funcs.length; i++) {
        String name = funcs[i];
        try { if (scriptLoader != null) scriptLoader.remove(name); } catch (Throwable ignore) {}
    }
    try { if (loadFlags != null) loadFlags.clear(); } catch (Throwable ignore) {}
    try { if (runFlags != null) runFlags.clear(); } catch (Throwable ignore) {}
    try { if (loopFlags != null) loopFlags.clear(); } catch (Throwable ignore) {}
    rebuildRegistry();
    funcs = getAll();
    for (int i = 0; i < funcs.length; i++) {
        try {
            String name = funcs[i];
            if (getLoad(name)) setLoad(name, true);
        } catch (Throwable e) {
            traceLog("setwindow_log", "[配置导入] 恢复脚本状态失败: " + funcs[i] + ": " + e);
        }
    }
}

