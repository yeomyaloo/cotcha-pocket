# -*- coding: utf-8 -*-
"""
OWASP Juice Shop 블라인드 사본 만들기.

Juice Shop 소스에는 취약한 줄을 알려 주는 `vuln-code-snippet` 주석이 있다. 분석하는 쪽이 그 힌트를 보지 못하도록
주석만 지우고(줄 번호는 그대로) 문제 목록·풀이·해설·테스트·AI 도구 설정을 뺀 사본을 만든다.
정답(juice-shop.expected.json)은 같은 주석에서 뽑았고, 사본 밖에 둔다.

    git clone --depth 1 https://github.com/juice-shop/juice-shop.git <원본>
    python benchmark/juice-shop/prepare.py <원본> <사본>

정답은 커밋 c9f9922 기준이다. 다른 커밋이면 줄 번호가 다를 수 있으니 prepare.py 가 출력하는 표시 위치와 맞춰 본다.
"""
import io
import os
import re
import shutil
import sys

# 힌트·풀이가 들어 있는 것
REMOVE = ['.git', 'test', 'cypress', 'rsn', '.github', 'data/static/codefixes', 'data/static/challenges.yml', 'data/static/i18n',
          'lib/codingChallenges.ts', 'frontend/src/hacking-instructor', 'SOLUTIONS.md', 'REFERENCES.md', 'HALL_OF_FAME.md',
          'AGENTS.md', 'CLAUDE.md', '.ai', '.agents', '.claude', '.codeium', '.continue', '.cursor', '.junie']
EXTENSIONS = ('.ts', '.js', '.html', '.yml', '.yaml', '.json', '.sol', '.tf', '.md')
MARKER_LINE = re.compile(r'\s*(//|#)\s*vuln-code-snippet.*$')
MARKER_BLOCK = re.compile(r'/\*\s*vuln-code-snippet.*?\*/|<!--\s*vuln-code-snippet.*?-->')


def long_path(path):
    """Windows 는 260자가 넘는 경로를 그냥 다루지 못한다 (프런트엔드 깊은 폴더). \\\\?\\ 를 붙여 긴 경로로 다룬다."""
    path = os.path.abspath(path)
    return '\\\\?\\' + path if os.name == 'nt' and not path.startswith('\\\\?\\') else path


def main(source, target):
    source, target = long_path(source), long_path(target)
    if os.path.exists(target):
        shutil.rmtree(target)
    shutil.copytree(source, target, ignore=shutil.ignore_patterns('.git', 'node_modules'))
    for rel in REMOVE:
        path = os.path.join(target, os.path.normpath(rel))
        if os.path.isdir(path):
            shutil.rmtree(path)
        elif os.path.exists(path):
            os.remove(path)
    markers = []
    for dirpath, _, filenames in os.walk(target):
        for name in filenames:
            if not name.endswith(EXTENSIONS):
                continue
            path = os.path.join(dirpath, name)
            try:
                text = io.open(path, encoding='utf-8').read()
            except UnicodeDecodeError:
                continue
            if 'vuln-code-snippet' not in text:
                continue
            rel = os.path.relpath(path, target).replace('\\', '/')
            lines = text.split('\n')
            for number, line in enumerate(lines, 1):
                found = re.search(r'vuln-code-snippet vuln-line ([\w ]+)', line)
                if found:
                    markers.append((rel, number, found.group(1).strip()))
                lines[number - 1] = MARKER_LINE.sub('', MARKER_BLOCK.sub('', line))
            io.open(path, 'w', encoding='utf-8', newline='').write('\n'.join(lines))
    for rel, number, challenges in markers:
        print(f'{rel}:{number} {challenges}')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit('usage: prepare.py <cloned juice-shop> <blind copy>')
    main(sys.argv[1], sys.argv[2])
