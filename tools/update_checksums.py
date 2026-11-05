import hashlib
import json
import os
import sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UP_JSON = os.path.join(ROOT, 'up.json')
ROOT_FILES = ['main.java', 'info.prop', '更新日志.txt']


def sha256_of(path):
    with open(path, 'rb') as f:
        return hashlib.sha256(f.read()).hexdigest()


def main():
    with open(UP_JSON, 'rb') as f:
        data = json.loads(f.read().decode('utf-8'))

    files = data.get('files') or []
    if not files:
        print('up.json 里没有 files 列表')
        return 1

    missing = [rel for rel in files if not os.path.isfile(os.path.join(ROOT, rel.replace('/', os.sep)))]
    if missing:
        print('以下文件在仓库里找不到，已中止：')
        for rel in missing:
            print('  ' + rel)
        return 1

    old = data.get('checksums') or {}
    new = {}
    changed = []
    for rel in files:
        digest = sha256_of(os.path.join(ROOT, rel.replace('/', os.sep)))
        new[rel] = digest
        if old.get(rel) != digest:
            changed.append(rel)

    data['checksums'] = new
    text = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    with open(UP_JSON, 'wb') as f:
        f.write(text.encode('utf-8'))

    print('已写入 %d 个文件的校验和' % len(new))
    if changed:
        print('本次变化：')
        for rel in changed:
            print('  ' + rel)
    else:
        print('全部文件与上次一致，无需改动')

    listed = set(files)
    stray = []
    for dirpath, _, names in os.walk(os.path.join(ROOT, 'API')):
        for name in names:
            if not name.endswith('.java'):
                continue
            rel = os.path.relpath(os.path.join(dirpath, name), ROOT).replace(os.sep, '/')
            if rel not in listed:
                stray.append(rel)
    for name in ROOT_FILES:
        if name not in listed:
            stray.append(name)
    if stray:
        print('以下文件没有列进 up.json 的 files，客户端不会下发：')
        for rel in sorted(stray):
            print('  ' + rel)
    return 0


if __name__ == '__main__':
    sys.exit(main())
