#!/usr/bin/env python3
"""Complete BitChat → PingChat rename with directory merge support"""
import os
import re
import shutil
from pathlib import Path

PROJECT_ROOT = Path(r"C:\Users\Tanishq\Desktop\project-p2p")

REPLACEMENTS = [
    ("com.bitchat", "com.pingchat"),
    ("BitChat", "PingChat"),
    ("BITCHAT", "PINGCHAT"),
    ("bitchat", "pingchat"),
    ("Bit Chat", "Ping Chat"),
    ("bit_chat", "ping_chat"),
]

SKIP_DIRS = {'.git', '.gradle', 'build', '.idea', 'node_modules', '.claude-omniroute'}
TEXT_EXTENSIONS = {'.kt', '.java', '.xml', '.json', '.gradle', '.kts', '.properties', '.txt', '.md', '.pro'}

def is_text_file(path):
    return path.suffix.lower() in TEXT_EXTENSIONS

def replace_in_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        original = content
        for old, new in REPLACEMENTS:
            content = content.replace(old, new)

        if content != original:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
    return False

def merge_directories(src, dst):
    """Recursively merge src into dst"""
    if not src.exists():
        return

    if not dst.exists():
        shutil.move(str(src), str(dst))
        return

    for item in src.iterdir():
        dst_item = dst / item.name
        if item.is_dir():
            merge_directories(item, dst_item)
        else:
            if dst_item.exists():
                dst_item.unlink()
            shutil.move(str(item), str(dst_item))

    if src.exists() and not any(src.iterdir()):
        src.rmdir()

def rename_path(old_path):
    """Generate new path with replacements"""
    parts = list(old_path.parts)
    new_parts = []
    for part in parts:
        new_part = part
        for old, new in REPLACEMENTS:
            new_part = new_part.replace(old, new)
        new_parts.append(new_part)
    return Path(*new_parts)

# Step 1: Replace content in all text files
print("Replacing content in files...")
files_changed = 0
for root, dirs, files in os.walk(PROJECT_ROOT):
    dirs[:] = [d for d in dirs if d not in SKIP_DIRS]

    for file in files:
        file_path = Path(root) / file
        if is_text_file(file_path):
            if replace_in_file(file_path):
                files_changed += 1

print(f"Updated {files_changed} files")

# Step 2: Rename directories (deepest first) with merge support
print("\nRenaming directories...")
dirs_to_rename = []
for root, dirs, _ in os.walk(PROJECT_ROOT, topdown=False):
    dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
    for dir_name in dirs:
        old_dir = Path(root) / dir_name
        if any(old in dir_name for old, _ in REPLACEMENTS):
            dirs_to_rename.append(old_dir)

for old_dir in dirs_to_rename:
    new_dir = rename_path(old_dir)
    if old_dir != new_dir:
        print(f"  {old_dir.relative_to(PROJECT_ROOT)} -> {new_dir.relative_to(PROJECT_ROOT)}")
        merge_directories(old_dir, new_dir)

# Step 3: Rename files
print("\nRenaming files...")
files_to_rename = []
for root, dirs, files in os.walk(PROJECT_ROOT):
    dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
    for file in files:
        old_file = Path(root) / file
        if any(old in file for old, _ in REPLACEMENTS):
            files_to_rename.append(old_file)

for old_file in files_to_rename:
    new_file = rename_path(old_file)
    if old_file != new_file and old_file.exists():
        print(f"  {old_file.relative_to(PROJECT_ROOT)} -> {new_file.relative_to(PROJECT_ROOT)}")
        new_file.parent.mkdir(parents=True, exist_ok=True)
        if new_file.exists():
            new_file.unlink()
        shutil.move(str(old_file), str(new_file))

print("\nRename complete!")
