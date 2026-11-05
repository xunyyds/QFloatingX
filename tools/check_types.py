"""类型检查诊断：列出结构错误（解析/包缺失），行号映射回原始源文件，并与 HEAD 基线对照。

方言处理：QFun 允许顶层松散语句（宿主包进初始化），javac 检查时收进合成 __looseInit；
脚本类参数在松散方法边界按方言写作 Object ctx，检查时回写为 SettingsBuildContext ctx
以恢复成员访问的静态校验。checkAndStartOrStopThread/registerActivity 顶层调用与
Settings.java 末尾的引导 try/catch 按惯例剥离（对当前与 HEAD 同等处理）。

环境变量：QFX_ANDROID_JAR 指定 android.jar，QFX_JAVAC 指定 javac 可执行文件；
未设置时按常见安装位置自动探测。
"""
import os, re, shutil, subprocess, sys
from collections import Counter
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = Path(__file__).resolve().parent.parent


def find_first(candidates):
    for p in candidates:
        if p and Path(p).exists():
            return str(Path(p))
    return ''


def find_android_jar():
    env = os.environ.get('QFX_ANDROID_JAR')
    if env and Path(env).exists():
        return env
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    roots = [sdk] if sdk else []
    roots += [r'D:\dev\android-sdk', r'C:\Users\%s\AppData\Local\Android\Sdk' % os.environ.get('USERNAME', '')]
    for root in roots:
        if not root or not Path(root).exists():
            continue
        platforms = Path(root) / 'platforms'
        if not platforms.exists():
            continue
        versions = sorted((p for p in platforms.iterdir() if p.is_dir()),
                          key=lambda p: p.name, reverse=True)
        for v in versions:
            jar = v / 'android.jar'
            if jar.exists():
                return str(jar)
    return ''


def find_javac():
    env = os.environ.get('QFX_JAVAC')
    if env and Path(env).exists():
        return env
    java_home = os.environ.get('JAVA_HOME')
    if java_home:
        p = Path(java_home) / 'bin' / 'javac.exe'
        if p.exists():
            return str(p)
        p = Path(java_home) / 'bin' / 'javac'
        if p.exists():
            return str(p)
    found = shutil.which('javac')
    if found:
        return found
    for base in [r'D:\dev\jdk-21', r'C:\Program Files\Java']:
        b = Path(base)
        if not b.exists():
            continue
        for d in sorted(b.iterdir(), key=lambda p: p.name, reverse=True):
            for name in ('javac.exe', 'javac'):
                p = d / 'bin' / name
                if p.exists():
                    return str(p)
    return ''


ANDROID_JAR = find_android_jar()
JAVAC = find_javac()
if not ANDROID_JAR or not JAVAC:
    print('缺少依赖：请设置 QFX_ANDROID_JAR 与 QFX_JAVAC 环境变量')
    sys.exit(2)
WORK = ROOT / 'typecheck'

HOST_ROOTS = [
    'com.tencent.common.app.BaseApplicationImpl',
    'com.tencent.mobileqq.app.BaseActivity',
    'com.tencent.mobileqq.app.BusinessObserver',
    'com.tencent.mobileqq.jump.api.IJumpApi',
    'com.tencent.mobileqq.qroute.QRoute',
    'com.tencent.mobileqq.transfile.api.ITransFileController',
    'com.tencent.mobileqq.troop.api.ITroopInfoService',
    'com.tencent.qqnt.kernel.nativeinterface.MsgRecord',
    'com.tencent.qqnt.kernelpublic.nativeinterface.Contact',
    'com.tencent.qqnt.msg.api.IMsgService',
    'com.tencent.qqnt.msg.api.IMsgUtilApi',
    'com.tencent.mobileqq.activity.shortvideo.d',
    'me.yxp.qfun.utils.qq.HostInfo',
    'me.yxp.qfun.activity.BaseComposeActivity',
]

HOST_HELPERS = [
    'void traceLog(String tag, String msg) {}',
    'void qqToast(int type, String msg) {}',
    'int adjustAlpha(int color, float factor) { return color; }',
    'int tc(android.content.Context ctx, String name) { return 0; }',
    'int pc(String hex) { return 0; }',
    'int dp(android.content.Context ctx, int value) { return value; }',
    'boolean isThemeDark(android.content.Context ctx) { return false; }',
    'android.view.View createButton(android.content.Context ctx, String text, int color, int bg, float size, int p1, int p2, int p3, boolean b, int p4, int p5, Object cb) { return null; }',
    'android.widget.EditText makeInput(android.content.Context ctx, String hint, String value) { return null; }',
    'void applyUiTheme(android.content.Context ctx, android.app.Dialog dialog, int mode) {}',
    'android.content.Context safeActivity() { return null; }',
]

MODULE = [    ('API/core/Core.java', 'core/Core.java'),
    ('API/settings/Settings.java', 'settings/Settings.java'),
    ('API/settings/SettingsPages.java', 'settings/SettingsPages.java'),
    ('API/settings/SettingsSearch.java', 'settings/SettingsSearch.java'),
    ('API/browser/QZone.java', 'browser/QZone.java'),
    ('API/widget/ColorPicker.java', 'widget/ColorPicker.java'),
    ('API/plugin/HotPlug.java', 'plugin/HotPlug.java'),
    ('API/feature/Stats.java', 'feature/Stats.java'),
    ('API/feature/Proto.java', 'feature/Proto.java'),
]


def read(p):
    return Path(p).read_text(encoding='utf-8', errors='replace')


def git_show(rev, rel):
    r = subprocess.run(['git', '-C', str(ROOT), 'show', rev + ':' + rel], capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode('utf-8', 'replace'))
    return r.stdout.decode('utf-8', 'replace')


def strip(src):
    """移除 import/package、引导 try/catch、顶层松散注册语句；catch (e) -> catch (Throwable e)；
    Object ctx -> SettingsBuildContext ctx（方言回写）。返回 (文本, 原始行号列表)"""
    lines = src.splitlines()
    out, orig = [], []
    i, n = 0, len(lines)
    while i < n:
        l = lines[i]
        if re.match(r'^\s*(import|package)\s', l):
            i += 1
            continue
        if re.match(r'^try \{', l) and i + 1 < n and re.match(r'^\s*registerActivity\(', lines[i + 1]):
            depth = l.count('{') - l.count('}')
            i += 1
            while i < n and depth > 0:
                depth += lines[i].count('{') - lines[i].count('}')
                i += 1
            continue
        if re.match(r'^(?:checkAndStartOrStopThread|registerActivity)\s*\(', l):
            i += 1
            continue
        line = l.replace('} catch (e) {', '} catch (Throwable e) {')
        line = line.replace('Object ctx', 'SettingsBuildContext ctx')
        line = line.replace('Object feature', 'SettingsFeaturePage feature')
        line = line.replace('Object pageCtx', 'SettingsBuildContext pageCtx')
        line = line.replace('Object settingsContextForActivity', 'SettingsBuildContext settingsContextForActivity')
        line = line.replace('Object findSettingsFeaturePage', 'SettingsFeaturePage findSettingsFeaturePage')
        line = line.replace('return SettingsState.settingsFeatureRegistry.get(',
                            'return (SettingsFeaturePage) SettingsState.settingsFeatureRegistry.get(')
        # QFun 方言：?: 默认值，检查时还原为标准 Java 三元
        line = re.sub(r'\?:', '!= null ?', line)
        # QFun 方言：?. 安全调用。语句位置还原为 if 包裹的调用表达式
        m = re.search(r'^(\s*)([A-Za-z_][\w.]*(?:\[\d\])*)\?\.(\w+)\((.*)$', line)
        if m:
            indent, recv, meth, rest = m.groups()
            line = f'{indent}if ({recv} != null) {recv}.{meth}({rest}'
        # 赋值右侧的安全调用还原为三元
        line = re.sub(r'([A-Za-z_][\w.]*(?:\[\d\])*)\?\.(\w+)\((.*)\);$',
                      r'\1 != null ? \1.\2(\3) : null;', line)
        out.append(line)
        orig.append(i + 1)
        i += 1
    return '\n'.join(out), orig


def module_rows(text, orig, disp):
    return [(tl, disp, orig[k]) for k, tl in enumerate(text.splitlines())]


def build_unit(sources_rows, wrap_ids):
    import_lines = [(k + 1, l) for k, l in enumerate(read(ROOT / 'API' / 'import.java').splitlines())
                    if re.match(r'^\s*import\s', l)]
    kept = []
    for no, l in import_lines:
        if any(r.rsplit('.', 1)[0] in l for r in HOST_ROOTS) \
                and 'BaseComposeActivity' not in l:
            continue
        kept.append((no, l))
    rows = [(l, 'import.java', no) for no, l in kept]
    rows.append(('void main() {}', '(synthetic)', 0))
    loose = []
    for mrows in sources_rows:
        for row in mrows:
            if (row[1], row[2]) in wrap_ids:
                loose.append(row)
            else:
                rows.append(row)
    if loose:
        rows.append(('void __looseInit() {', '(synthetic)', 0))
        rows += loose
        rows.append(('}', '(synthetic)', 0))
    unit_text = '\n'.join(r[0] for r in rows)
    # 宿主注入的无名包 helper：不属于任何 API 文件，检查时在合并单元末尾补合成方法
    for sig in HOST_HELPERS:
        name = sig.split('(')[0].strip()
        if name + '(' not in unit_text:
            rows.append((sig, '(synthetic)', 0))
    return '\n'.join(r[0] for r in rows), rows


def build_stubs():
    stub = WORK / 'stub'
    if stub.exists():
        shutil.rmtree(stub)
    stub.mkdir(parents=True)
    for fq in HOST_ROOTS:
        pkg, _, cls = fq.rpartition('.')
        d = stub / Path(*pkg.split('.'))
        d.mkdir(parents=True, exist_ok=True)
        if fq == 'me.yxp.qfun.utils.qq.HostInfo':
            (d / (cls + '.java')).write_text(
                f'package {pkg};\npublic class {cls} {{\n'
                f'    public static {cls} INSTANCE = new {cls}();\n'
                f'    public String getVersionName() {{ return ""; }}\n'
                f'}}\n', encoding='utf-8')
            continue
        extends = ' extends android.app.Activity' if fq == 'me.yxp.qfun.activity.BaseComposeActivity' else ''
        body = ' }' if extends else (
            '    public static class INSTANCE { public String getVersionName() { return ""; } }\n'
            '    public Object __any;\n}')
        (d / (cls + '.java')).write_text(
            f'package {pkg};\npublic class {cls}{extends} {{\n'
            f'{body}\n', encoding='utf-8')


def compile_unit(unit, tag):
    d = WORK / tag
    d.mkdir(parents=True)
    (d / 'out').mkdir()
    f = d / 'Unit.java'
    f.write_text(unit, encoding='utf-8')
    r = subprocess.run(
        [JAVAC, '-J-Duser.language=en', '-proc:none', '-Xmaxerrs', '10000',
         '--enable-preview', '--release', '21',
         '-cp', str(ANDROID_JAR) + ';' + str(WORK / 'stub'),
         '-d', str(d / 'out'), str(f)],
        capture_output=True)
    out = (r.stdout + r.stderr).decode('utf-8', 'replace')
    (d / 'raw.txt').write_text(out, encoding='utf-8')

    lines = out.splitlines()
    errors = []
    for i, l in enumerate(lines):
        m = re.search(r'Unit\.java:(\d+): error: (.*)', l)
        if m:
            sym = ''
            for j in range(i + 1, min(i + 8, len(lines))):
                sm = re.search(r'symbol:\s*(.*)', lines[j])
                if sm:
                    sym = sm.group(1).strip()
                    break
            errors.append((int(m.group(1)), m.group(2), sym))
    return errors


def check(sources_rows, tag):
    wrap = {}
    errs, rows = [], []
    for round_no in range(4):
        unit, rows = build_unit(sources_rows, set(wrap))
        errs = compile_unit(unit, f'{tag}_r{round_no}')
        targets = {}
        for n, msg, sym in errs:
            if msg != 'class, interface, enum, or record expected':
                continue
            text, disp, orig = rows[n - 1]
            if disp in ('import.java', '(synthetic)') or (disp, orig) in wrap:
                continue
            if text.rstrip().endswith(';'):
                targets[(disp, orig)] = text
        if not targets:
            break
        wrap.update(targets)
    return errs, rows, wrap


if WORK.exists():
    shutil.rmtree(WORK)
WORK.mkdir()
build_stubs()

cur_rows_src = []
for rel, disp in MODULE:
    text, orig = strip(read(ROOT / rel))
    cur_rows_src.append(module_rows(text, orig, disp))
cur_errs, cur_rows, cur_wrap = check(cur_rows_src, 'cur')

head_rows_src = []
for rel, disp in MODULE:
    text, orig = strip(git_show('HEAD', rel))
    head_rows_src.append(module_rows(text, orig, disp))
head_errs, _, head_wrap = check(head_rows_src, 'head')


def classify(errs):
    sym = [e for e in errs if e[1] == 'cannot find symbol']
    st = [e for e in errs if e[1] != 'cannot find symbol']
    return st, sym


cur_st, cur_sym = classify(cur_errs)
head_st, head_sym = classify(head_errs)

print(f'当前工作区: 总错误 {len(cur_errs)}  结构错误 {len(cur_st)}  cannot find symbol {len(cur_sym)}')
print(f'HEAD 基线 : 总错误 {len(head_errs)}  结构错误 {len(head_st)}  cannot find symbol {len(head_sym)}')
print()

cs = Counter((m, s) for _, m, s in cur_st)
hs = Counter((m, s) for _, m, s in head_st)
added, removed = cs - hs, hs - cs
print('===== 与 HEAD 相比新增的结构错误 =====')
if added:
    for (m, s), c in added.items():
        for n, msg, sym in cur_st:
            if (msg, sym) == (m, s):
                disp, orig = cur_rows[n - 1][1], cur_rows[n - 1][2]
                print(f'  + x{c}  {disp}:{orig}  {m}' + (f'  [symbol: {s}]' if s else ''))
                break
else:
    print('  （无）')
print()
print('===== 与 HEAD 相比减少的结构错误 =====')
print('\n'.join(f'  - {m} {s} x{c}' for (m, s), c in removed.items()) if removed else '  （无）')
print()

cur_syms = Counter(s for _, m, s in cur_sym if s)
head_syms = Counter(s for _, m, s in head_sym if s)
print('===== cannot find symbol 符号对照 =====')
print('新增: ' + (', '.join(sorted(cur_syms - head_syms)) or '（无）'))
print('减少: ' + (', '.join(sorted(head_syms - cur_syms)) or '（无）'))
