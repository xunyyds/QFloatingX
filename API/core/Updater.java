interface ProgressCallback {
    void onProgress(int progress);
    void onProgressTip(String tip);
}

interface UpdateProgressListener {
    void onPhase(String phase);
    void onFileStart(String displayName, int currentIndex, int totalFiles);
    void onFileProgress(int filePercent, long fileBytes, long fileTotal, long totalBytesAll, long elapsedMs);
    void onFileVerify(String displayName, boolean pass);
    void onAllDone(boolean success, String failReason);
}
String formatElapsedTime(long ms) {
    long totalSec = ms / 1000;
    long mm = totalSec / 60;
    long ss = totalSec % 60;
    String mmS = mm < 10 ? "0" + mm : String.valueOf(mm);
    String ssS = ss < 10 ? "0" + ss : String.valueOf(ss);
    return mmS + ":" + ssS;
}

String sha256HexOfFile(String path) {
    java.io.InputStream in = null;
    try {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        in = new java.io.FileInputStream(path);
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) != -1) md.update(buf, 0, read);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            int b = digest[i] & 0xFF;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    } catch (Throwable e) {
        traceLog("api_log", "[sha256HexOfFile] 异常: " + e);
        return null;
    } finally {
        if (in != null) { try { in.close(); } catch (Throwable ignore) {} }
    }
}

boolean verifyDownloadedFile(String tmpPath, long actualSize, String expectedSha256) {
    try {
        java.io.File f = new java.io.File(tmpPath);
        if (!f.exists()) return false;
        if (f.length() <= 0) return false;
        if (actualSize > 0 && f.length() != actualSize) return false;
        if (expectedSha256 != null && expectedSha256.trim().length() > 0) {
            String actualSha = sha256HexOfFile(tmpPath);
            if (actualSha == null) return false;
            if (!actualSha.equalsIgnoreCase(expectedSha256.trim())) return false;
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[verifyDownloadedFile] 异常: " + e);
        return false;
    }
}

boolean replaceWithTmp(String savePath, String tmpPath) {
    try {
        java.io.File target = new java.io.File(savePath);
        java.io.File tmp = new java.io.File(tmpPath);
        java.io.File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (target.exists()) {
            java.io.File bak = new java.io.File(savePath + ".bak");
            if (bak.exists()) bak.delete();
            boolean movedAside = target.renameTo(bak);
            if (!movedAside) {
                if (!target.delete()) return false;
            }
            if (!tmp.renameTo(target)) {
                if (movedAside) bak.renameTo(target);
                return false;
            }
            if (bak.exists()) bak.delete();
        } else {
            if (!tmp.renameTo(target)) return false;
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[replaceWithTmp] 异常: " + e);
        return false;
    }
}
void migrateUpdateChannelOnce() {
    try {
        if (getBoolean("settings", "update_channel_migrated", false)) return;
        if ("gitee".equals(getString("settings", "update_channel", "github"))) {
            putString("settings", "update_channel", "github");
        }
        putBoolean("settings", "update_channel_migrated", true);
    } catch (Throwable e) {
        traceLog("api_log", "[migrateUpdateChannelOnce] 异常: " + e);
    }
}

String[] buildMirrorUrlsOn(String branch, String relativePath, boolean useGitee) {
    String githubRaw = "https://raw.githubusercontent.com/xunyyds/QFloatingX/" + branch + "/" + relativePath;
    String[] mirrorHosts = {"gh.07150721.xyz", "ghpxy.hwinzniej.top", "ghf.无名氏.top", "gh.chjina.com", "js.jiangss.shop", "ghproxy.icu"};
    int extra = useGitee ? 1 : 0;
    String[] urls = new String[1 + mirrorHosts.length + extra];
    urls[0] = githubRaw;
    for (int k = 0; k < mirrorHosts.length; k++) {
        String h = mirrorHosts[k];
        try { h = java.net.IDN.toASCII(h); } catch (Throwable ignore) {}
        urls[k + 1] = "https://" + h + "/" + githubRaw;
    }
    if (useGitee) urls[mirrorHosts.length + 1] = "https://gitee.com/ovoxiaomo/qfloating-x/raw/QF/" + relativePath;
    return urls;
}

String[] buildMirrorUrls(String relativePath) {
    migrateUpdateChannelOnce();
    String channel = getString("settings", "update_channel", "github");
    boolean useGitee = !"github".equals(channel);
    return buildMirrorUrlsOn("main", relativePath, useGitee);
}
boolean downloadToTmpDetailed(String[] urls, String tmpPath, final UpdateProgressListener listener,
    final long grandBytesBefore, final long startMs, final int idx, final int totalFiles,
    final String displayName, long[] resultBytes) {
    java.io.File tmpFile = new java.io.File(tmpPath);
    java.io.File saveDir = tmpFile.getParentFile();
    if (saveDir != null && !saveDir.exists()) saveDir.mkdirs();
    for (int u = 0; u < urls.length; u++) {
        String url = urls[u];
        if (u > 0 && listener != null) {
            listener.onPhase("mirror");
        }
        java.io.FileOutputStream out = null;
        java.io.InputStream in = null;
        boolean ok = false;
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept-Encoding", "identity");
            int remoteSize = conn.getContentLength();
            in = conn.getInputStream();
            out = new java.io.FileOutputStream(tmpFile);
            byte[] buf = new byte[8192];
            long total = 0;
            int read;
            int tick = 0;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                total += read;
                tick++;
                if (listener != null && tick % 4 == 0) {
                    int percent = 0;
                    if (remoteSize > 0) percent = (int) ((total * 100) / remoteSize);
                    listener.onFileProgress(percent, total, remoteSize > 0 ? remoteSize : 0, grandBytesBefore + total, System.currentTimeMillis() - startMs);
                }
            }
            out.flush();
            if (resultBytes != null) { resultBytes[0] = total; resultBytes[1] = remoteSize; }
            if (remoteSize > 0) ok = (total == remoteSize);
            else ok = (total > 0);
        } catch (Throwable e) {
            traceLog("api_log", "[downloadToTmpDetailed] 下载失败: " + e.getMessage() + " url=" + url);
        } finally {
            try { if (out != null) out.close(); } catch (Throwable t) {}
            try { if (in != null) in.close(); } catch (Throwable t) {}
        }
        if (ok) return true;
        try { if (tmpFile.exists()) tmpFile.delete(); } catch (Throwable t) {}
    }
    return false;
}

boolean executeQfxUpdate(List updateFiles, JSONObject checksums, UpdateProgressListener listener) {
    if (updateFiles == null || updateFiles.isEmpty()) {
        if (listener != null) listener.onAllDone(false, "没有需要更新的文件");
        return false;
    }
    int total = updateFiles.size();
    long grandTotalBytes = 0;
    long startMs = System.currentTimeMillis();
    if (listener != null) listener.onPhase("download");
    for (int i = 0; i < total; i++) {
        String fileName = (String) updateFiles.get(i);
        String relativePath = fileName;
        if (relativePath.startsWith("QFloatingX/")) relativePath = relativePath.substring("QFloatingX/".length());
        String[] urlArr = buildMirrorUrls(fileName);
        String savePath = pluginPath + "/" + relativePath;
        String tmpPath = savePath + ".new";
        if (listener != null) listener.onFileStart(relativePath, i + 1, total);
        long[] bytesInfo = new long[]{0, 0};
        boolean dlOk = downloadToTmpDetailed(urlArr, tmpPath, listener, grandTotalBytes, startMs, i + 1, total, relativePath, bytesInfo);
        if (!dlOk) {
            cleanupTempFile(tmpPath);
            if (listener != null) listener.onAllDone(false, "下载失败: " + relativePath);
            return false;
        }
        grandTotalBytes += bytesInfo[0];
        if (listener != null) listener.onPhase("verify");
        String expectedSha = null;
        if (checksums != null) expectedSha = checksums.optString(relativePath, null);
        boolean verifyOk = verifyDownloadedFile(tmpPath, bytesInfo[0], expectedSha);
        if (listener != null) listener.onFileVerify(relativePath, verifyOk);
        if (!verifyOk) {
            cleanupTempFile(tmpPath);
            if (listener != null) listener.onAllDone(false, "完整性校验失败，已中止: " + relativePath);
            return false;
        }
        if (!replaceWithTmp(savePath, tmpPath)) {
            cleanupTempFile(tmpPath);
            if (listener != null) listener.onAllDone(false, "文件替换失败: " + relativePath);
            return false;
        }
        if (listener != null) listener.onPhase("download");
    }
    if (listener != null) listener.onAllDone(true, null);
    return true;
}
boolean downloadFile(String url, String savePath, ProgressCallback callback) {
    boolean success = false;
    java.io.FileOutputStream out = null;
    java.io.InputStream in = null;
    final int BUF_SIZE = 8192;
    final int PROGRESS_STEP_BUF = 10;
    int downloadedBufCount = 0;
    int currentProgress = 0;
    java.io.File saveFile = new java.io.File(savePath);
    java.io.File saveDir = saveFile.getParentFile();
    if (saveDir != null && !saveDir.exists()) {
        saveDir.mkdirs();
    }
    try {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept-Encoding", "identity");
        int remoteSize = conn.getContentLength();
        if (callback != null) {
            if (remoteSize == -1) {
                callback.onProgressTip("正在下载...");
                callback.onProgress(0);
            } else {
                callback.onProgressTip("总大小: " + (remoteSize / 1024) + "KB");
                callback.onProgress(0);
            }
        }

        in = conn.getInputStream();
        out = new java.io.FileOutputStream(saveFile);

        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
            total += read;
            downloadedBufCount++;

            if (callback != null) {
                if (remoteSize == -1) {
                    if (downloadedBufCount % PROGRESS_STEP_BUF == 0) {
                        currentProgress += 10;
                        if (currentProgress > 90) currentProgress = 90;
                        callback.onProgress(currentProgress);
                        callback.onProgressTip("已下载: " + (total / 1024) + "KB");
                    }
                } else {
                    int progress = (int) ((total * 100) / remoteSize);
                    callback.onProgress(progress);
                    callback.onProgressTip("已下载: " + (total / 1024) + "KB/" + (remoteSize / 1024) + "KB");
                }
            }
        }
        out.flush();

        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("下载完成");
        }

        if (remoteSize == -1) {
            success = (total > 0);
        } else {
            success = (total == remoteSize);
        }
        if (!success && saveFile.exists()) {
            saveFile.delete();
        }

    } catch (Throwable e) {
        if (saveFile.exists()) saveFile.delete();
        traceLog("api_log", "[downloadFile] 异常: " + e.getMessage());
    } finally {
        try { if (out != null) out.close(); } catch (Throwable t) { traceLog("api_log", "[downloadFile] 异常: " + t); }
        try { if (in != null) in.close(); } catch (Throwable t) { traceLog("api_log", "[downloadFile] 异常: " + t); }
    }
    return success;
}

boolean unzipFile(String zipPath, String destDir, ProgressCallback callback) {
    java.io.File zipFile = new java.io.File(zipPath);
    java.io.File destDirFile = new java.io.File(destDir);
    java.util.zip.ZipFile zip = null;
    try {
        if (!zipFile.exists() || !zipFile.isFile()) return false;
        if (!destDirFile.exists() && !destDirFile.mkdirs()) return false;

        String destCanonical = destDirFile.getCanonicalPath() + java.io.File.separator;
        zip = new java.util.zip.ZipFile(zipFile);
        int totalEntry = zip.size();
        int currentEntry = 0;
        String zipRootDir = "";

        java.util.Enumeration firstPass = zip.entries();
        while (firstPass.hasMoreElements()) {
            java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) firstPass.nextElement();
            if (!entry.isDirectory()) {
                String name = entry.getName();
                int idx = name.indexOf('/');
                if (idx > 0) {
                    zipRootDir = name.substring(0, idx + 1);
                    break;
                }
            }
        }

        if (callback != null) {
            callback.onProgressTip("开始解压文件");
            callback.onProgress(0);
        }

        java.util.Enumeration entries = zip.entries();
        byte[] buf = new byte[32768];
        while (entries.hasMoreElements()) {
            currentEntry++;
            java.util.zip.ZipEntry entry = (java.util.zip.ZipEntry) entries.nextElement();
            String name = entry.getName();

            if (!zipRootDir.isEmpty() && name.startsWith(zipRootDir)) {
                name = name.substring(zipRootDir.length());
                if (name.isEmpty()) continue;
            }

            java.io.File entryFile = new java.io.File(destDirFile, name);
            String canonicalPath = entryFile.getCanonicalPath();
            if (!canonicalPath.equals(destDirFile.getCanonicalPath()) && !canonicalPath.startsWith(destCanonical)) {
                traceLog("api_log", "[unzipFile] 拒绝越界条目: " + name);
                continue;
            }

            if (entry.isDirectory()) {
                entryFile.mkdirs();
            } else {
                java.io.File parent = entryFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                java.io.InputStream zin = null;
                java.io.OutputStream fout = null;
                try {
                    zin = new java.io.BufferedInputStream(zip.getInputStream(entry), 32768);
                    fout = new java.io.BufferedOutputStream(new java.io.FileOutputStream(entryFile), 32768);
                    int read;
                    while ((read = zin.read(buf)) != -1) fout.write(buf, 0, read);
                    fout.flush();
                } finally {
                    try { if (fout != null) fout.close(); } catch (Throwable e) {}
                    try { if (zin != null) zin.close(); } catch (Throwable e) {}
                }
            }

            if (callback != null) {
                int progress = totalEntry > 0 ? (int) ((currentEntry * 100L) / totalEntry) : 100;
                callback.onProgress(progress);
                callback.onProgressTip("解压: " + name);
            }
        }

        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("解压完成");
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[unzipFile] 异常: " + e);
        return false;
    } finally {
        try { if (zip != null) zip.close(); } catch (Throwable e) { traceLog("api_log", "[unzipFile] 关闭压缩包异常: " + e); }
    }
}

int countZipFiles(java.io.File dir) {
    if (dir == null || !dir.exists()) return 0;
    if (dir.isFile()) return 1;
    java.io.File[] files = dir.listFiles();
    if (files == null) return 0;
    int total = 0;
    for (int i = 0; i < files.length; i++) {
        java.io.File f = files[i];
        if (f == null) continue;
        total += f.isDirectory() ? countZipFiles(f) : 1;
    }
    return total;
}

boolean zipDirectoryFiles(java.io.File root, java.io.File current, String entryPrefix, java.util.zip.ZipOutputStream zos, byte[] buf, ProgressCallback callback, int[] progressState, int totalFiles) throws Exception {
    java.io.File[] files = current.listFiles();
    if (files == null) return true;
    String rootPath = root.getCanonicalPath();
    String currentPath = current.getCanonicalPath();
    if (files.length == 0 && !currentPath.equals(rootPath)) {
        String relativeDir = currentPath.substring(rootPath.length()).replace(java.io.File.separatorChar, '/');
        while (relativeDir.startsWith("/")) relativeDir = relativeDir.substring(1);
        String emptyDirName = (entryPrefix == null || entryPrefix.trim().isEmpty() ? "" : entryPrefix + "/") + relativeDir + "/";
        zos.putNextEntry(new java.util.zip.ZipEntry(emptyDirName));
        zos.closeEntry();
        return true;
    }
    for (int i = 0; i < files.length; i++) {
        java.io.File file = files[i];
        if (file == null) continue;
        String filePath = file.getCanonicalPath();
        String relative = filePath.substring(rootPath.length()).replace(java.io.File.separatorChar, '/');
        while (relative.startsWith("/")) relative = relative.substring(1);
        String entryName = (entryPrefix == null || entryPrefix.trim().isEmpty()) ? relative : entryPrefix + "/" + relative;
        if (file.isDirectory()) {
            zipDirectoryFiles(root, file, entryPrefix, zos, buf, callback, progressState, totalFiles);
            continue;
        }
        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
        java.io.InputStream in = null;
        try {
            in = new java.io.BufferedInputStream(new java.io.FileInputStream(file), 32768);
            int read;
            while ((read = in.read(buf)) != -1) zos.write(buf, 0, read);
        } finally {
            try { if (in != null) in.close(); } catch (Throwable e) {}
            try { zos.closeEntry(); } catch (Throwable e) {}
        }
        progressState[0]++;
        if (callback != null) {
            callback.onProgress(totalFiles > 0 ? (int) ((progressState[0] * 100L) / totalFiles) : 100);
            callback.onProgressTip("压缩: " + entryName);
        }
    }
    return true;
}

boolean zipDirectory(String sourceDir, String zipPath, String entryPrefix, String extraEntryName, String extraEntryContent, ProgressCallback callback) {
    java.io.File root = new java.io.File(sourceDir);
    java.io.File zipFile = new java.io.File(zipPath);
    java.util.zip.ZipOutputStream zos = null;
    try {
        if (!root.exists() || !root.isDirectory()) return false;
        java.io.File parent = zipFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        java.io.File temp = new java.io.File(zipPath + ".new");
        if (temp.exists()) temp.delete();

        int totalFiles = countZipFiles(root);
        int[] progressState = new int[]{0};
        byte[] buf = new byte[32768];
        zos = new java.util.zip.ZipOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(temp), 32768));
        if (callback != null) {
            callback.onProgressTip("开始压缩文件");
            callback.onProgress(0);
        }

        if (extraEntryName != null && !extraEntryName.trim().isEmpty()) {
            zos.putNextEntry(new java.util.zip.ZipEntry(extraEntryName));
            if (extraEntryContent != null) zos.write(extraEntryContent.getBytes("UTF-8"));
            zos.closeEntry();
        }

        zipDirectoryFiles(root, root, entryPrefix, zos, buf, callback, progressState, totalFiles);
        zos.finish();
        zos.close();
        zos = null;

        if (zipFile.exists() && !zipFile.delete()) {
            temp.delete();
            return false;
        }
        if (!temp.renameTo(zipFile)) {
            temp.delete();
            return false;
        }

        if (callback != null) {
            callback.onProgress(100);
            callback.onProgressTip("压缩完成");
        }
        return true;
    } catch (Throwable e) {
        traceLog("api_log", "[zipDirectory] 异常: " + e);
        return false;
    } finally {
        try { if (zos != null) zos.close(); } catch (Throwable e) { traceLog("api_log", "[zipDirectory] 关闭压缩包异常: " + e); }
        try {
            java.io.File temp = new java.io.File(zipPath + ".new");
            if (temp.exists()) temp.delete();
        } catch (Throwable e) {}
    }
}
public String markdownToHtml(String markdown) {
    if (markdown == null || markdown.isEmpty()) return "";
    String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    StringBuilder html = new StringBuilder(markdown.length() + 128);
    StringBuilder code = new StringBuilder();
    int listType = 0;
    boolean inCode = false;
    for (int i = 0; i < lines.length; i++) {
        String line = lines[i];
        String trimmed = line.trim();
        if (trimmed.startsWith("```")) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            if (inCode) {
                String escapedCode = code.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
                html.append("<pre><code>").append(escapedCode).append("</code></pre>");
                code.setLength(0);
                inCode = false;
            } else {
                inCode = true;
            }
            continue;
        }
        if (inCode) {
            if (code.length() > 0) code.append('\n');
            code.append(line);
            continue;
        }
        if (trimmed.isEmpty()) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            continue;
        }
        if (trimmed.matches("-{2,}|\\*{3,}|_{3,}")) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            html.append("<hr/>");
            continue;
        }
        String content = trimmed;
        int lineType = 0;
        Matcher heading = Pattern.compile("^(#{1,6})\\s+(.+)$").matcher(trimmed);
        Matcher bullet = Pattern.compile("^[-+*]\\s+(.+)$").matcher(trimmed);
        Matcher numbered = Pattern.compile("^\\d+[.)]\\s+(.+)$").matcher(trimmed);
        if (heading.matches()) {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            lineType = Math.min(heading.group(1).length(), 3);
            content = heading.group(2);
        } else if (bullet.matches() || numbered.matches()) {
            int nextType = bullet.matches() ? 1 : 2;
            if (listType != nextType) {
                if (listType != 0) html.append(listType == 1 ? "</ul>" : "</ol>");
                html.append(nextType == 1 ? "<ul>" : "<ol>");
                listType = nextType;
            }
            lineType = 4;
            content = bullet.matches() ? bullet.group(1) : numbered.group(1);
        } else {
            if (listType != 0) {
                html.append(listType == 1 ? "</ul>" : "</ol>");
                listType = 0;
            }
            if (trimmed.startsWith(">")) {
                lineType = 5;
                content = trimmed.substring(1).trim();
            }
        }
        String value = content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        ArrayList codeBlocks = new ArrayList();
        Matcher codeMatcher = Pattern.compile("`([^`]+)`").matcher(value);
        StringBuffer codeBuffer = new StringBuffer();
        int codeIndex = 0;
        while (codeMatcher.find()) {
            String token = "@@MDINLINECODE" + codeIndex++ + "@@";
            codeBlocks.add("<code>" + codeMatcher.group(1) + "</code>");
            codeMatcher.appendReplacement(codeBuffer, Matcher.quoteReplacement(token));
        }
        codeMatcher.appendTail(codeBuffer);
        value = codeBuffer.toString();
        Matcher linkMatcher = Pattern.compile("\\[([^\\]]+)\\]\\((https?://[^\\s)]+|mailto:[^\\s)]+)\\)").matcher(value);
        StringBuffer linkBuffer = new StringBuffer();
        while (linkMatcher.find()) {
            String replacement = "<a href=\"" + linkMatcher.group(2) + "\">" + linkMatcher.group(1) + "</a>";
            linkMatcher.appendReplacement(linkBuffer, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(linkBuffer);
        value = linkBuffer.toString();
        Matcher boldA = Pattern.compile("\\*\\*(.+?)\\*\\*").matcher(value);
        StringBuffer formatBuffer = new StringBuffer();
        while (boldA.find()) boldA.appendReplacement(formatBuffer, Matcher.quoteReplacement("<strong>" + boldA.group(1) + "</strong>"));
        boldA.appendTail(formatBuffer);
        value = formatBuffer.toString();
        Matcher boldB = Pattern.compile("__(.+?)__").matcher(value);
        formatBuffer = new StringBuffer();
        while (boldB.find()) boldB.appendReplacement(formatBuffer, Matcher.quoteReplacement("<strong>" + boldB.group(1) + "</strong>"));
        boldB.appendTail(formatBuffer);
        value = formatBuffer.toString();
        Matcher italicA = Pattern.compile("(?<!\\*)\\*([^*]+)\\*(?!\\*)").matcher(value);
        formatBuffer = new StringBuffer();
        while (italicA.find()) italicA.appendReplacement(formatBuffer, Matcher.quoteReplacement("<em>" + italicA.group(1) + "</em>"));
        italicA.appendTail(formatBuffer);
        value = formatBuffer.toString();
        Matcher italicB = Pattern.compile("(?<!_)_([^_]+)_(?!_)").matcher(value);
        formatBuffer = new StringBuffer();
        while (italicB.find()) italicB.appendReplacement(formatBuffer, Matcher.quoteReplacement("<em>" + italicB.group(1) + "</em>"));
        italicB.appendTail(formatBuffer);
        value = formatBuffer.toString();
        for (int j = 0; j < codeBlocks.size(); j++) value = value.replace("@@MDINLINECODE" + j + "@@", (String) codeBlocks.get(j));
        if (lineType >= 1 && lineType <= 3) html.append("<h").append(lineType).append(">").append(value).append("</h").append(lineType).append(">");
        else if (lineType == 4) html.append("<li>").append(value).append("</li>");
        else if (lineType == 5) html.append("<blockquote><p>").append(value).append("</p></blockquote>");
        else html.append("<p>").append(value).append("</p>");
    }
    if (inCode) {
        String escapedCode = code.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
        html.append("<pre><code>").append(escapedCode).append("</code></pre>");
    }
    if (listType != 0) html.append(listType == 1 ? "</ul>" : "</ol>");
    return html.toString();
}
void showUpdateDialog(final String version, final String versionType, final String updateType, final String changelog, final List updateFiles, final JSONObject checksums) {

    final Activity activity = safeActivity();
    if (activity == null) return;

    final String prevVersion = readprop(pluginPath + "/info.prop", "versionCode");

    LinearLayout updRoot = new LinearLayout(activity);
    updRoot.setOrientation(LinearLayout.VERTICAL);
    updRoot.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 12));
    updRoot.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

    LinearLayout headRow = new LinearLayout(activity);
    headRow.setOrientation(LinearLayout.HORIZONTAL);
    headRow.setGravity(Gravity.CENTER_VERTICAL);
    final ImageView icon = new ImageView(activity);
    icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
    headRow.addView(icon, new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40)));

    LinearLayout headText = new LinearLayout(activity);
    headText.setOrientation(LinearLayout.VERTICAL);
    headText.setPadding(dp(activity, 12), 0, 0, 0);

    TextView appName = new TextView(activity);
    appName.setText("QFloatingX");
    appName.setTextSize(16);
    appName.setTypeface(null, Typeface.BOLD);
    appName.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));

    TextView verLine = new TextView(activity);
    verLine.setText(versionType + " " + version);
    verLine.setTextSize(11);
    verLine.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));

    TextView versionCompare = new TextView(activity);
    String localDisplay = prevVersion == null || prevVersion.trim().isEmpty() ? "未知" : prevVersion.trim();
    versionCompare.setText("当前版本 " + localDisplay + "  →  新版本 " + version);
    versionCompare.setTextSize(11);
    versionCompare.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    versionCompare.setPadding(0, dp(activity, 3), 0, 0);

    headText.addView(appName);
    headText.addView(verLine);
    headText.addView(versionCompare);
    headRow.addView(headText, new LinearLayout.LayoutParams(0, -2, 1.0f));

    TextView badge = new TextView(activity);
    badge.setText(updateType);
    badge.setTextSize(11);
    badge.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
    badge.setPadding(dp(activity, 8), dp(activity, 3), dp(activity, 8), dp(activity, 3));
    GradientDrawable bd = new GradientDrawable();
    bd.setCornerRadius(dp(activity, 8));
    bd.setColor(pc(getSettingsThemeColor(activity, "primary_container")));
    badge.setBackground(bd);
    headRow.addView(badge);
    updRoot.addView(headRow);

    final TextView updateCount = new TextView(activity);
    updateCount.setText("全网累计更新用户 加载中…");
    updateCount.setTextSize(12);
    updateCount.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    updateCount.setPadding(0, dp(activity, 14), 0, dp(activity, 4));
    updRoot.addView(updateCount);

    TextView secTitle = new TextView(activity);
    secTitle.setText("更新内容");
    secTitle.setTextSize(13);
    secTitle.setTypeface(null, Typeface.BOLD);
    secTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    secTitle.setPadding(0, dp(activity, 8), 0, dp(activity, 6));
    updRoot.addView(secTitle);

    ScrollView updScroll = new ScrollView(activity);
    TextView updBody = new TextView(activity);
    String bodyText = changelog == null || changelog.trim().isEmpty() ? "暂无更新说明" : changelog.trim();
    try {
        updBody.setText(Html.fromHtml(markdownToHtml(bodyText)));
    } catch (Throwable e) {
        updBody.setText(bodyText);
        traceLog("api_log", "[showUpdateDialog] Markdown格式化失败，使用纯文本: " + e.getMessage());
    }
    updBody.setTextSize(13);
    updBody.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
    updBody.setLineSpacing(0, 1.15f);
    updScroll.addView(updBody);
    updRoot.addView(updScroll, new LinearLayout.LayoutParams(-1, dp(activity, 150)));

    TextView updateHint = new TextView(activity);
    updateHint.setText("检测到新版本，确认后即可开始更新");
    updateHint.setTextSize(13);
    updateHint.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
    updateHint.setPadding(0, dp(activity, 18), 0, dp(activity, 8));
    updRoot.addView(updateHint);

    LinearLayout updBtnRow = new LinearLayout(activity);
    updBtnRow.setOrientation(LinearLayout.HORIZONTAL);
    updBtnRow.setGravity(Gravity.END);
    updBtnRow.setPadding(0, dp(activity, 8), 0, 0);
    TextView updLater = createButton(activity, "此版本不再提示", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 20, 14, 12, false, 0, 0, null);
    TextView updSilent = createButton(activity, "静默更新", pc(getSettingsThemeColor(activity, "primary")), Color.TRANSPARENT, 14f, 20, 14, 12, false, 0, 0, null);
    TextView updOk = createButton(activity, "立即更新", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 20, 16, 12, false, 0, 0, null);
    updBtnRow.addView(updLater);
    updBtnRow.addView(updSilent);
    updBtnRow.addView(updOk);
    updRoot.addView(updBtnRow);

    final Dialog updDialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
    updDialog.requestWindowFeature(1);
    try {
        Window uw = updDialog.getWindow();
        if (uw != null) uw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    } catch (Throwable e) {
        traceLog("api_log", "[showUpdateDialog] 设置窗口背景异常: " + e);
    }
    updDialog.setContentView(updRoot);
    updDialog.setCancelable(false);

    updLater.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            putString("更新检测", "已忽略版本", version);
            try { updDialog.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭更新弹窗异常: " + e); }
            Toast("已忽略 v" + version + "，可在设置中手动检查更新");
        }
    });

    final Runnable afterUpdateOk = new Runnable() {
        public void run() {
            LinearLayout rst = new LinearLayout(activity);
            rst.setOrientation(LinearLayout.VERTICAL);
            rst.setPadding(dp(activity, 24), dp(activity, 20), dp(activity, 24), dp(activity, 12));
            rst.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));
            TextView rt = new TextView(activity);
            rt.setText("更新完成");
            rt.setTextSize(18);
            rt.setTypeface(null, Typeface.BOLD);
            rt.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            rst.addView(rt);
            TextView rm = new TextView(activity);
            rm.setText("文件已就绪，是否现在重启脚本？");
            rm.setTextSize(13);
            rm.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            rm.setPadding(0, dp(activity, 8), 0, dp(activity, 12));
            rst.addView(rm);
            LinearLayout rb = new LinearLayout(activity);
            rb.setOrientation(LinearLayout.HORIZONTAL);
            rb.setGravity(Gravity.END);
            TextView laterB = createButton(activity, "稍后重启", pc(getSettingsThemeColor(activity, "on_surface_variant")), Color.TRANSPARENT, 14f, 20, 16, 12, false, 0, 0, null);
            TextView nowB = createButton(activity, "现在重启", Color.WHITE, pc(getSettingsThemeColor(activity, "primary")), 14f, 20, 16, 12, false, 0, 0, null);
            rb.addView(laterB);
            rb.addView(nowB);
            rst.addView(rb);
            final Dialog rd = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
            rd.requestWindowFeature(1);
            try {
                Window rw = rd.getWindow();
                if (rw != null) rw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 重启窗口背景异常: " + e);
            }
            rd.setContentView(rst);
            laterB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { try { rd.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭重启弹窗异常: " + e); } }
            });
            nowB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try { rd.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭重启确认弹窗异常: " + e); }
                    重新加载脚本();
                }
            });
            rd.show();
            try { applyUiTheme(activity, rd, 1); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 应用重启弹窗主题异常: " + e); }
        }
    };

    updSilent.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try { updDialog.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭更新弹窗异常: " + e); }
            Toast("开始静默更新…");
            ThreadPool.execute(new Runnable() {
                public void run() {
                    UpdateProgressListener silentListener = new UpdateProgressListener() {
                        public void onPhase(String phase) {}
                        public void onFileStart(String displayName, int currentIndex, int totalFiles) {}
                        public void onFileProgress(int filePercent, long fileBytes, long fileTotal, long totalBytesAll, long elapsedMs) {}
                        public void onFileVerify(String displayName, boolean pass) {}
                        public void onAllDone(final boolean success, final String failReason) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (success) {
                                        Toast("更新完成");
                                        afterUpdateOk.run();
                                    } else {
                                        Toast("静默更新失败: " + failReason);
                                    }
                                }
                            });
                        }
                    };
                    executeQfxUpdate(updateFiles, checksums, silentListener);
                }
            });
        }
    });

    updOk.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            try { updDialog.dismiss(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭更新弹窗异常: " + e); }
            if (updateFiles == null || updateFiles.isEmpty()) {
                Toast("没有需要更新的文件");
                return;
            }
            showStyledUpdateProgress(activity, updateFiles, checksums, afterUpdateOk);
        }
    });

    final android.graphics.Bitmap[] avatarBmp = new android.graphics.Bitmap[]{null};
    updDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            try { icon.setImageDrawable(null); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 清理头像视图异常: " + e); }
            try {
                if (avatarBmp[0] != null && !avatarBmp[0].isRecycled()) avatarBmp[0].recycle();
            } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 回收头像异常: " + e); }
            avatarBmp[0] = null;
        }
    });

    updDialog.show();
    try { applyUiTheme(activity, updDialog, 1); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 应用更新弹窗主题异常: " + e); }

    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String countJson = get("https://cn.apihz.cn/api/jisuan/jishuqi2.php?id=10013224&key=17e1755199ff8eebc2fd58bce20d950e&type=2&number=2");
                final String count = countJson == null || countJson.trim().isEmpty() ? "0" : new JSONObject(countJson).optString("number2", "0");
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            if (updDialog.isShowing()) updateCount.setText("全网累计更新用户 " + count + "w+");
                        } catch (Throwable e) {
                            traceLog("api_log", "[showUpdateDialog] 更新用户数异常: " + e);
                        }
                    }
                });
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 用户数加载失败: " + e);
            }
        }
    });

    ThreadPool.execute(new Runnable() {
        public void run() {
            java.net.HttpURLConnection ac = null;
            java.io.InputStream ain = null;
            try {
                ac = (java.net.HttpURLConnection) new java.net.URL("http://q.qlogo.cn/headimg_dl?dst_uin=3069670151&spec=640").openConnection();
                ac.setConnectTimeout(2500);
                ac.setReadTimeout(5000);
                ac.setUseCaches(true);
                int code = ac.getResponseCode();
                if (code >= 200 && code < 300) {
                    ain = ac.getInputStream();
                    final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(ain);
                    if (bmp != null) {
                        avatarBmp[0] = bmp;
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                try {
                                    if (updDialog.isShowing()) icon.setImageBitmap(bmp);
                                    else if (!bmp.isRecycled()) bmp.recycle();
                                } catch (Throwable e) {
                                    try { if (!bmp.isRecycled()) bmp.recycle(); } catch (Throwable ignore) {}
                                    traceLog("api_log", "[showUpdateDialog] 设置头像异常: " + e);
                                }
                            }
                        });
                    }
                }
            } catch (Throwable e) {
                traceLog("api_log", "[showUpdateDialog] 头像加载失败: " + e);
            } finally {
                try { if (ain != null) ain.close(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭头像流异常: " + e); }
                try { if (ac != null) ac.disconnect(); } catch (Throwable e) { traceLog("api_log", "[showUpdateDialog] 关闭头像连接异常: " + e); }
            }
        }
    });
}

void showStyledUpdateProgress(final Activity activity, final List updateFiles, final JSONObject checksums, final Runnable afterUpdateOk) {
    activity.runOnUiThread(new Runnable() {
        public void run() {
            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(activity, 22), dp(activity, 20), dp(activity, 22), dp(activity, 16));
            root.setBackground(roundRect(pc(getSettingsThemeColor(activity, "surface")), dp(activity, getUiCornerDp())));

            final TextView phaseTitle = new TextView(activity);
            phaseTitle.setTextSize(17);
            phaseTitle.setTypeface(null, Typeface.BOLD);
            phaseTitle.setTextColor(pc(getSettingsThemeColor(activity, "on_surface")));
            root.addView(phaseTitle);

            final TextView fileTv = new TextView(activity);
            fileTv.setTextSize(12);
            fileTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            fileTv.setPadding(0, dp(activity, 10), 0, dp(activity, 6));
            root.addView(fileTv);

            final ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(0);

            GradientDrawable trackD = new GradientDrawable();
            trackD.setShape(GradientDrawable.RECTANGLE);
            trackD.setCornerRadius(dp(activity, 7));
            trackD.setColor(pc(getSettingsThemeColor(activity, "ripple")));
            GradientDrawable progressD = new GradientDrawable();
            progressD.setShape(GradientDrawable.RECTANGLE);
            progressD.setCornerRadius(dp(activity, 7));
            progressD.setColor(pc(getSettingsThemeColor(activity, "primary")));
            ClipDrawable clipD = new ClipDrawable(progressD, Gravity.LEFT, ClipDrawable.HORIZONTAL);
            LayerDrawable layerD = new LayerDrawable(new Drawable[]{trackD, clipD});
            layerD.setId(0, android.R.id.background);
            layerD.setId(1, android.R.id.progress);
            bar.setProgressDrawable(layerD);
            root.addView(bar, new LinearLayout.LayoutParams(-1, dp(activity, 14)));

            final TextView percentTv = new TextView(activity);
            percentTv.setText("0%");
            percentTv.setTextSize(22);
            percentTv.setTypeface(null, Typeface.BOLD);
            percentTv.setTextColor(pc(getSettingsThemeColor(activity, "primary")));
            percentTv.setGravity(Gravity.CENTER);
            percentTv.setPadding(0, dp(activity, 12), 0, dp(activity, 8));
            root.addView(percentTv);

            LinearLayout infoRow = new LinearLayout(activity);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            final TextView elapsedTv = new TextView(activity);
            elapsedTv.setText("已用时 00:00");
            elapsedTv.setTextSize(12);
            elapsedTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            infoRow.addView(elapsedTv);
            Space infoSpace = new Space(activity);
            infoRow.addView(infoSpace, new LinearLayout.LayoutParams(0, -2, 1.0f));
            final TextView downloadedTv = new TextView(activity);
            downloadedTv.setText("已下载 0 B");
            downloadedTv.setTextSize(12);
            downloadedTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            infoRow.addView(downloadedTv);
            root.addView(infoRow);

            final TextView remainTv = new TextView(activity);
            remainTv.setText("");
            remainTv.setTextSize(12);
            remainTv.setTextColor(pc(getSettingsThemeColor(activity, "on_surface_variant")));
            remainTv.setGravity(Gravity.CENTER);
            remainTv.setPadding(0, dp(activity, 8), 0, 0);
            root.addView(remainTv);

            final Dialog progDialog = new Dialog(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
            progDialog.requestWindowFeature(1);
            try {
                Window pw = progDialog.getWindow();
                if (pw != null) pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } catch (Throwable ignore) {}
            progDialog.setContentView(root);
            progDialog.setCancelable(false);
            progDialog.show();
            try { applyUiTheme(activity, progDialog, 1); } catch (Throwable ignore) {}

            final String[] phaseBase = new String[]{"正在下载更新"};
            final Handler animHandler = new Handler(Looper.getMainLooper());
            final int[] dotIdx = new int[]{0};
            animHandler.post(new Runnable() {
                public void run() {
                    dotIdx[0] = (dotIdx[0] + 1) % 4;
                    String dots = "";
                    for (int d = 0; d < dotIdx[0]; d++) dots += ".";
                    phaseTitle.setText(phaseBase[0] + dots);
                    animHandler.postDelayed(this, 500);
                }
            });

            final long timerStartMs = System.currentTimeMillis();
            animHandler.post(new Runnable() {
                public void run() {
                    elapsedTv.setText("已用时 " + formatElapsedTime(System.currentTimeMillis() - timerStartMs));
                    animHandler.postDelayed(this, 1000);
                }
            });

            ThreadPool.execute(new Runnable() {
                public void run() {
                    final long[] speedRef = new long[]{0L, 0L};
                    UpdateProgressListener listener = new UpdateProgressListener() {
                        public void onPhase(final String phase) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if ("verify".equals(phase)) {
                                        phaseBase[0] = "正在验证完整性";
                                        bar.setProgress(100);
                                    } else if ("mirror".equals(phase)) {
                                        phaseBase[0] = "当前正在使用镜像站进行加速下载";
                                    } else {
                                        phaseBase[0] = "正在下载更新";
                                    }
                                }
                            });
                        }
                        public void onFileStart(final String displayName, final int currentIndex, final int totalFiles) {
                            speedRef[0] = 0;
                            speedRef[1] = 0;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    fileTv.setText(displayName);
                                    remainTv.setText("第 " + currentIndex + " / " + totalFiles + " 个文件，剩余 " + (totalFiles - currentIndex) + " 个");
                                    bar.setProgress(0);
                                    percentTv.setText("0%");
                                }
                            });
                        }
                        public void onFileProgress(final int filePercent, final long fileBytes, final long fileTotal, final long totalBytesAll, final long elapsedMs) {
                            long now = System.currentTimeMillis();
                            String speedStr = null;
                            if (speedRef[0] > 0 && now - speedRef[0] >= 400) {
                                long db = totalBytesAll - speedRef[1];
                                long dt = now - speedRef[0];
                                long speed = dt > 0 ? db * 1000 / dt : 0;
                                speedStr = formatSize(speed) + "/s";
                                speedRef[0] = now;
                                speedRef[1] = totalBytesAll;
                            } else if (speedRef[0] == 0) {
                                speedRef[0] = now;
                                speedRef[1] = totalBytesAll;
                            }
                            final String fspeed = speedStr;
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    bar.setProgress(filePercent);
                                    percentTv.setText(filePercent + "%");
                                    if (fspeed != null) {
                                        downloadedTv.setText("已下载 " + formatSize(totalBytesAll) + "  ·  " + fspeed);
                                    } else {
                                        downloadedTv.setText("已下载 " + formatSize(totalBytesAll));
                                    }
                                }
                            });
                        }
                        public void onFileVerify(final String displayName, final boolean pass) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    fileTv.setText((pass ? "✓ " : "✗ ") + displayName + (pass ? " 校验通过" : " 校验失败"));
                                }
                            });
                        }
                        public void onAllDone(final boolean success, final String failReason) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    animHandler.removeCallbacksAndMessages(null);
                                    try { progDialog.dismiss(); } catch (Throwable ignore) {}
                                    if (success) afterUpdateOk.run();
                                    else Toast("更新失败，未替换任何文件: " + failReason);
                                }
                            });
                        }
                    };
                    executeQfxUpdate(updateFiles, checksums, listener);
                }
            });
        }
    });
}
private volatile long qfxUpdateLastCheckTime = 0;
private static final long QFX_UPDATE_CHECK_INTERVAL = 30 * 60 * 1000L;
int compareVersionCode(String left, String right) {
    if (left == null) left = "";
    if (right == null) right = "";
    left = left.trim();
    right = right.trim();
    if (left.startsWith("v") || left.startsWith("V")) left = left.substring(1);
    if (right.startsWith("v") || right.startsWith("V")) right = right.substring(1);
    String[] la = left.split("\\.");
    String[] ra = right.split("\\.");
    int max = Math.max(la.length, ra.length);
    for (int i = 0; i < max; i++) {
        long a = 0L;
        long b = 0L;
        try {
            if (i < la.length) {
                String s = la[i].replaceAll("[^0-9].*", "");
                if (!s.isEmpty()) a = Long.parseLong(s);
            }
        } catch (Throwable e) {}
        try {
            if (i < ra.length) {
                String s = ra[i].replaceAll("[^0-9].*", "");
                if (!s.isEmpty()) b = Long.parseLong(s);
            }
        } catch (Throwable e) {}
        if (a > b) return 1;
        if (a < b) return -1;
    }
    return 0;
}

String fetchFirstValidUpdateJson(final String[] urls) {
    if (urls == null || urls.length == 0) return null;
    java.util.concurrent.ExecutorService mirrorPool = null;
    java.util.ArrayList futures = new java.util.ArrayList();
    final java.util.concurrent.atomic.AtomicBoolean stopRequests = new java.util.concurrent.atomic.AtomicBoolean(false);
    try {
        int taskCount = Math.max(1, urls.length);
        mirrorPool = java.util.concurrent.Executors.newFixedThreadPool(taskCount);
        java.util.concurrent.CompletionService completion = new java.util.concurrent.ExecutorCompletionService(mirrorPool);

        for (int i = 0; i < urls.length; i++) {
            final int index = i;
            final String url = urls[i];
            if (url == null || url.trim().isEmpty()) continue;
            futures.add(completion.submit(new java.util.concurrent.Callable() {
                public Object call() {
                    java.net.HttpURLConnection conn = null;
                    java.io.InputStream in = null;
                    try {
                        if (stopRequests.get() || Thread.currentThread().isInterrupted()) return null;
                        conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(3500);
                        conn.setReadTimeout(5000);
                        conn.setUseCaches(false);
                        conn.setRequestProperty("Connection", "close");
                        conn.setRequestProperty("User-Agent", "QFloatingX-Updater");
                        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
                        int code = conn.getResponseCode();
                        if (code < 200 || code >= 300) {
                            return null;
                        }
                        in = conn.getInputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        char[] buf = new char[8192];
                        int read;
                        while ((read = reader.read(buf)) != -1) {
                            if (stopRequests.get() || Thread.currentThread().isInterrupted()) return null;
                            sb.append(buf, 0, read);
                        }
                        String body = sb.toString().trim();
                        if (body.isEmpty()) return null;
                        try {
                            new JSONObject(body);
                        } catch (Throwable e) {
                            if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] up.json JSON无效 #" + index + ": " + url);
                            return null;
                        }
                        if (stopRequests.compareAndSet(false, true)) return new String[]{body, url};
                        return null;
                    } catch (java.io.InterruptedIOException e) {
                        if (!stopRequests.get() && !Thread.currentThread().isInterrupted()) {
                            if (e instanceof java.net.SocketTimeoutException) {
                                traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 请求超时 #" + index + ": " + e);
                            } else {
                                traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 请求中断 #" + index + ": " + e);
                            }
                        }
                        return null;
                    } catch (Throwable e) {
                        if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] up.json 请求失败 #" + index + ": " + e);
                        return null;
                    } finally {
                        try { if (in != null) in.close(); } catch (Throwable e) {}
                        try { if (conn != null) conn.disconnect(); } catch (Throwable e) {}
                    }
                }
            }));
        }

        for (int i = 0; i < futures.size(); i++) {
            java.util.concurrent.Future future = null;
            try {
                future = completion.poll(10000, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopRequests.set(true);
                break;
            } catch (Throwable e) {
                if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] 等待镜像结果异常: " + e);
            }
            if (future == null) break;
            try {
                Object result = future.get();
                if (result != null) {
                    String[] hit = (String[]) result;
                    return hit[0];
                }
            } catch (java.util.concurrent.CancellationException e) {
            } catch (Throwable e) {
                if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] 解析线程结果异常: " + e);
            }
        }
    } catch (Throwable e) {
        if (!stopRequests.get()) traceLog("api_log", "[fetchFirstValidUpdateJson] 并行请求异常: " + e);
    } finally {
        stopRequests.set(true);
        try {
            for (int i = 0; i < futures.size(); i++) {
                try { ((java.util.concurrent.Future) futures.get(i)).cancel(false); } catch (Throwable e) {}
            }
        } catch (Throwable e) {}
        try { if (mirrorPool != null) mirrorPool.shutdown(); } catch (Throwable e) {}
    }
    return null;
}
void checkQFXUpdate() {
    final long now = System.currentTimeMillis();
    if (now - qfxUpdateLastCheckTime < QFX_UPDATE_CHECK_INTERVAL) {
        return;
    }
    qfxUpdateLastCheckTime = now;
    runQFXUpdateCheck(false);
}

void manualCheckQFXUpdate() {
    qfxUpdateLastCheckTime = System.currentTimeMillis();
    runQFXUpdateCheck(true);
}

void runQFXUpdateCheck(final boolean manual) {
    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                String ignored = getString("更新检测", "已忽略版本", "");
                String[] urls = buildMirrorUrls("up.json");
                String jsonStr = fetchFirstValidUpdateJson(urls);
                if (jsonStr == null || jsonStr.isEmpty()) {
                    traceLog("api_log", "[runQFXUpdateCheck] 所有up.json镜像均不可用");
                    if (manual) {
                        final Activity activity = safeActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() { Toast("无法访问更新服务器，请检查网络或切换更新通道"); }
                            });
                        }
                    }
                    return;
                }

                final JSONObject json;
                try {
                    json = new JSONObject(jsonStr);
                } catch (Throwable parseErr) {
                    traceLog("api_log", "[runQFXUpdateCheck] up.json解析失败: " + parseErr);
                    if (manual) {
                        final Activity activity = safeActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() { Toast("更新信息解析失败，请稍后重试或切换通道"); }
                            });
                        }
                    }
                    return;
                }

                String remoteVersion = json.optString("version", "0.0.0").trim();
                String versionType = json.optString("versionType", "正式版");
                String updateType = json.optString("updateType", "全量");
                String changelog = json.optString("changelog", "");
                boolean isTestVersion = versionType != null && (
                    versionType.contains("测试") ||
                    versionType.toLowerCase().contains("beta") ||
                    versionType.toLowerCase().contains("pre-release") ||
                    versionType.toLowerCase().contains("preview")
                );
                if (!manual && isTestVersion && !getBoolean("settings", "receive_beta_update_notice", false)) return;

                java.util.List files = new java.util.ArrayList();
                org.json.JSONArray filesArray = json.optJSONArray("files");
                if (filesArray != null) {
                    for (int i = 0; i < filesArray.length(); i++) {
                        files.add(filesArray.getString(i));
                    }
                }
                final JSONObject checksums = json.optJSONObject("checksums");

                String localVersion = readprop(pluginPath + "/info.prop", "versionCode");
                if (localVersion == null || localVersion.trim().isEmpty()) localVersion = "0.0.0";
                localVersion = localVersion.trim();

                int versionCompare = compareVersionCode(remoteVersion, localVersion);
                if (versionCompare <= 0) {
                    if (manual) {
                        final String currentVer = localVersion;
                        final String remoteVer = remoteVersion;
                        final Activity activity = safeActivity();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    if (compareVersionCode(remoteVer, currentVer) == 0) {
                                        Toast("已是最新版本 " + currentVer);
                                    } else {
                                        Toast("当前版本 " + currentVer + " 高于更新源 " + remoteVer);
                                    }
                                }
                            });
                        }
                    }
                    return;
                }

                if (!manual && remoteVersion.equals(ignored)) return;

                final String finalRemoteVersion = remoteVersion;
                final String finalVersionType = versionType;
                final String finalUpdateType = updateType;
                final String finalChangelog = changelog;
                final List finalFiles = files;
                final JSONObject finalChecksums = checksums;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        try {
                            showUpdateDialog(finalRemoteVersion, finalVersionType, finalUpdateType, finalChangelog, finalFiles, finalChecksums);
                        } catch (Throwable e) {
                            traceLog("api_log", "[runQFXUpdateCheck] 更新弹窗异常: " + e);
                        }
                    }
                });
            } catch (Throwable t) {
                traceLog("api_log", "[runQFXUpdateCheck] 检查更新异常: " + t);
                if (manual) {
                    final Activity activity = safeActivity();
                    if (activity != null) {
                        final String errMsg = "检查更新失败: " + t.getMessage();
                        activity.runOnUiThread(new Runnable() {
                            public void run() { Toast(errMsg); }
                        });
                    }
                }
            }
        }
    });
}
boolean iconFileExists(String fullPath) {
    if (fullPath == null || fullPath.trim().isEmpty()) return false;
    int lastDot = fullPath.lastIndexOf('.');
    int lastSlash = fullPath.lastIndexOf('/');
    String base = (lastDot > lastSlash && lastDot > 0) ? fullPath.substring(0, lastDot) : fullPath;
    try {
        if (new java.io.File(base + ".png").exists()) return true;
        if (new java.io.File(base + ".gif").exists()) return true;
    } catch (Throwable e) { traceLog("api_log", "[iconFileExists] 异常: " + e); }
    return false;
}

boolean checkAllIconsExist() {
    if (!iconFileExists(iconPath)) return false;
    if (!iconFileExists(closeIconPath)) return false;
    if (!new java.io.File(pluginPath + "/API/QQ.png").exists()) return false;
    if (!new java.io.File(pluginPath + "/API/GitHub.png").exists()) return false;
    return true;
}

boolean performDownloadAndUnzip() {
    final String tempZipPath = pluginPath + "/API/icon.zip";
    final String destDir = pluginPath + "/API/";
    try {
        String[] urls = buildMirrorUrlsOn("QF", "icon.zip", true);
        boolean downloadResult = false;
        for (int u = 0; u < urls.length && !downloadResult; u++) {
            downloadResult = downloadFile(urls[u], tempZipPath, new ProgressCallback() {
                public void onProgress(int progressVal) {}
                public void onProgressTip(String tip) {}
            });
            if (!downloadResult) traceLog("api_log", "[performDownloadAndUnzip] 下载失败，尝试下一个源: " + urls[u]);
        }
        if (!downloadResult) {
            cleanupTempFile(tempZipPath);
            return false;
        }
        boolean unzipResult = unzipFile(tempZipPath, destDir, new ProgressCallback() {
            public void onProgress(int progressVal) {}
            public void onProgressTip(String tip) {}
        });
        cleanupTempFile(tempZipPath);
        return unzipResult;
    } catch (Throwable e) {
        cleanupTempFile(tempZipPath);
        traceLog("api_log", "[performDownloadAndUnzip] 异常: " + e);
        return false;
    }
}

void cleanupTempFile(String tempPath) {
    try {
        java.io.File file = new java.io.File(tempPath);
        if (file.exists()) {
            boolean deleted = file.delete();
        }
    } catch (Exception e) {
        traceLog("api_log", "[cleanupTempFile] 异常: " + e.getMessage());
    }
}


void ensureResourceAvailable() {

    boolean allExist = checkAllIconsExist();

    if (allExist) {
        return;
    }

    Toast("检测到图标文件缺失，正在为您后台下载中...");

    ThreadPool.execute(new Runnable() {
        public void run() {
            try {
                boolean success = performDownloadAndUnzip();

                if (success) {
                    final boolean verifyResult = checkAllIconsExist();

                            if (verifyResult) {
                                Toast("下载图标文件成功！");
                            } else {
                                Toast("下载完成，但文件验证失败");
                                traceLog("api_log", "[ensureResourceAvailable] 资源下载但验证失败");
                            }
                } else {
                            Toast("图标文件下载失败，请检查网络");
                            traceLog("api_log", "[ensureResourceAvailable] 资源准备失败");
                }

            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                traceLog("api_log", "[ensureResourceAvailable]  致命异常: " + errorMsg);
                Toast("图标文件准备失败: " + errorMsg);
            }
        }
    });
}
