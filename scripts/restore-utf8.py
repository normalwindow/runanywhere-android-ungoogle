#!/usr/bin/env python3
"""Restore UTF-8 in files rewritten by PowerShell's default encoding."""
from __future__ import annotations

import subprocess
from difflib import SequenceMatcher
from pathlib import Path

OLD_PKG = "com.runanywhere.runanywhereai"
NEW_PKG = "xyz.normalwindow.runanywhere"
ROOT = Path(__file__).resolve().parents[1]


def git_show(path: str) -> str:
    return subprocess.check_output(
        ["git", "show", f"HEAD:{path}"],
        cwd=ROOT,
    ).decode("utf-8")


def git_ls() -> list[str]:
    out = subprocess.check_output(
        ["git", "ls-tree", "-r", "--name-only", "HEAD"],
        cwd=ROOT,
        text=True,
    )
    return [line.replace("\\", "/") for line in out.splitlines()]


def to_lossy(text: str) -> str:
    return "".join("\ufffd?" if ord(ch) > 127 else ch for ch in text)


def restore_line(orig: str, current: str) -> str:
    orig_lossy = to_lossy(orig)
    if orig_lossy == current:
        return orig
    matcher = SequenceMatcher(a=orig_lossy, b=current, autojunk=False)
    out: list[str] = []
    oi = 0
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            chunk = []
            pos = 0
            while pos < (i2 - i1):
                ch = orig[oi]
                if ord(ch) > 127:
                    chunk.append(ch)
                    oi += 1
                    pos += 2
                else:
                    chunk.append(ch)
                    oi += 1
                    pos += 1
            out.append("".join(chunk))
        elif tag == "insert":
            out.append(current[j1:j2])
        elif tag == "replace":
            out.append(current[j1:j2])
            pos = 0
            while pos < (i2 - i1):
                ch = orig[oi]
                if ord(ch) > 127:
                    oi += 1
                    pos += 2
                else:
                    oi += 1
                    pos += 1
        elif tag == "delete":
            pos = 0
            while pos < (i2 - i1):
                ch = orig[oi]
                if ord(ch) > 127:
                    oi += 1
                    pos += 2
                else:
                    oi += 1
                    pos += 1
    return "".join(out)


def restore(orig: str, current: str) -> str:
    orig_lines = orig.split("\n")
    curr_lines = current.split("\n")
    orig_lossy_lines = [to_lossy(line) for line in orig_lines]
    matcher = SequenceMatcher(a=orig_lossy_lines, b=curr_lines, autojunk=False)
    out: list[str] = []
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            out.extend(orig_lines[i1:i2])
        elif tag == "insert":
            out.extend(curr_lines[j1:j2])
        elif tag == "replace":
            o_span = orig_lines[i1:i2]
            c_span = curr_lines[j1:j2]
            if len(o_span) == len(c_span):
                out.extend(restore_line(o, c) for o, c in zip(o_span, c_span))
            else:
                o_text = "\n".join(o_span)
                c_text = "\n".join(c_span)
                restored = restore_line(o_text, c_text)
                out.extend(restored.split("\n"))
        elif tag == "delete":
            continue
    return "\n".join(out)


def decode_current(path: Path) -> str:
    data = path.read_bytes()
    if data.startswith(b"\xef\xbb\xbf"):
        data = data[3:]
    return data.decode("utf-8", errors="replace").replace("\r\n", "\n").replace("\r", "\n")


def write_utf8(path: Path, text: str) -> None:
    path.write_bytes(text.encode("utf-8"))


def main() -> None:
    old_files = [
        path for path in git_ls() if "/com/runanywhere/runanywhereai/" in path
    ]
    restored = 0
    leftover = []
    for old in old_files:
        new = old.replace(
            "com/runanywhere/runanywhereai",
            "xyz/normalwindow/runanywhere",
        )
        dest = ROOT / new
        if not dest.exists():
            leftover.append(("missing", new, 0))
            continue
        orig = git_show(old).replace("\r\n", "\n").replace(OLD_PKG, NEW_PKG)
        current = decode_current(dest)
        updated = restore(orig, current)
        write_utf8(dest, updated)
        restored += 1
        n = updated.count("\ufffd")
        if n:
            leftover.append(("fffd", new, n))

    print(f"Restored {restored} git-backed files")
    if leftover:
        print("LEFTOVER:")
        for item in leftover:
            print(" ", item)

    still = []
    for path in (ROOT / "app/src").rglob("*"):
        if not path.is_file() or "build" in path.parts:
            continue
        if path.suffix.lower() not in {".kt", ".kts", ".xml", ".java"}:
            continue
        data = path.read_bytes()
        try:
            text = data.decode("utf-8")
            utf_ok = True
        except UnicodeDecodeError:
            text = data.decode("utf-8", errors="replace")
            utf_ok = False
        if (not utf_ok) or ("\ufffd" in text):
            still.append((str(path.relative_to(ROOT)), utf_ok, text.count("\ufffd")))
    print(f"Remaining corrupted files: {len(still)}")
    for item in still:
        print(" ", item)


if __name__ == "__main__":
    main()
