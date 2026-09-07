#!/usr/bin/env python3
"""Second pass: restore quotes/spaces dropped after recovered Unicode."""
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


KEEP_DELETED = set(' "\'')


def merge_line(orig: str, curr: str) -> str:
    if orig == curr:
        return orig
    matcher = SequenceMatcher(a=orig, b=curr, autojunk=False)
    out: list[str] = []
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == "equal":
            out.append(orig[i1:i2])
        elif tag == "insert":
            out.append(curr[j1:j2])
        elif tag == "replace":
            o, c = orig[i1:i2], curr[j1:j2]
            if "\ufffd" in c or (any(ord(ch) > 127 for ch in o) and not any(ord(ch) > 127 for ch in c)):
                out.append(o)
                extra = c
                for ch in o:
                    extra = extra.replace(ch, "", 1)
                if extra and extra not in o:
                    # Keep current-only ASCII wrappers such as AppLocale.text(
                    if any(ord(ch) < 128 for ch in extra):
                        out.append(extra)
            else:
                out.append(c)
        elif tag == "delete":
            o = orig[i1:i2]
            if o and all(ch in KEEP_DELETED or ord(ch) > 127 for ch in o):
                out.append(o)
    return "".join(out)


def merge_file(orig: str, current: str) -> str:
    orig_lines = orig.split("\n")
    curr_lines = current.split("\n")
    matcher = SequenceMatcher(a=orig_lines, b=curr_lines, autojunk=False)
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
                out.extend(merge_line(o, c) for o, c in zip(o_span, c_span))
            else:
                out.extend(c_span)
        elif tag == "delete":
            continue
    return "\n".join(out)


def decode(path: Path) -> str:
    return path.read_bytes().decode("utf-8").replace("\r\n", "\n").replace("\r", "\n")


def main() -> None:
    repaired = 0
    leftovers = []
    for old in git_ls():
        if "/com/runanywhere/runanywhereai/" not in old:
            continue
        new = old.replace(
            "com/runanywhere/runanywhereai",
            "xyz/normalwindow/runanywhere",
        )
        dest = ROOT / new
        if not dest.exists():
            continue
        orig = git_show(old).replace("\r\n", "\n").replace(OLD_PKG, NEW_PKG)
        current = decode(dest)
        updated = merge_file(orig, current)
        if updated != current:
            dest.write_bytes(updated.encode("utf-8"))
            repaired += 1
        if "\ufffd" in updated:
            leftovers.append(new)

    print(f"Repaired {repaired} files")
    if leftovers:
        print("LEFTOVER FFFD:")
        for item in leftovers:
            print(" ", item)


if __name__ == "__main__":
    main()
