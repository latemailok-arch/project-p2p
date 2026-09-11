import os

directory = r"C:\Users\Tanishq\Desktop\project-p2p"
skip_dirs = {'.git', 'build', '.gradle', '.idea', '.cxx', 'captures', 'gradle'}
skip_exts = {'.png', '.jpg', '.jpeg', '.gif', '.jar', '.aar', '.so', '.dex', '.class', '.zip', '.ico', '.keystore', '.lockfile'}

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except (UnicodeDecodeError, PermissionError):
        return False

    original = content
    content = content.replace("com.pingchat", "com.pingchat")
    content = content.replace("com.pingchat", "com.pingchat")
    content = content.replace("PingChat", "PingChat")
    content = content.replace("PINGCHAT", "PINGCHAT")
    content = content.replace("pingchat", "pingchat")
    content = content.replace("Ping Chat", "Ping Chat")
    content = content.replace("ping_chat", "ping_chat")
    content = content.replace("PING_CHAT", "PING_CHAT")

    if original != content:
        with open(filepath, 'w', encoding='utf-8', newline='') as f:
            f.write(content)
        return True
    return False

def rename_directories_and_files(start_path):
    import shutil
    changes_made = True
    renamed_count = 0
    while changes_made:
        changes_made = False
        for root, dirs, files in os.walk(start_path):
            dirs[:] = [d for d in dirs if d not in skip_dirs]

            for file in files:
                if "bitchat" in file.lower() and not file.endswith('.py'):
                    old_path = os.path.join(root, file)
                    new_file = file.replace("bitchat", "pingchat").replace("BitChat", "PingChat").replace("BITCHAT", "PINGCHAT")
                    new_path = os.path.join(root, new_file)
                    if old_path != new_path:
                        print(f"Renaming file: {file} -> {new_file}")
                        if os.path.exists(new_path):
                            print(f"  Warning: {new_path} already exists, skipping")
                        else:
                            os.rename(old_path, new_path)
                            renamed_count += 1
                        changes_made = True
                        break

            if changes_made:
                break

            for d in dirs:
                if "bitchat" in d.lower():
                    old_path = os.path.join(root, d)
                    new_d = d.replace("bitchat", "pingchat").replace("BitChat", "PingChat").replace("BITCHAT", "PINGCHAT")
                    new_path = os.path.join(root, new_d)
                    if old_path != new_path:
                        print(f"Renaming directory: {d} -> {new_d}")
                        if os.path.exists(new_path):
                            print(f"  Warning: {new_path} already exists, merging contents")
                            for item in os.listdir(old_path):
                                src = os.path.join(old_path, item)
                                dst = os.path.join(new_path, item)
                                if os.path.isdir(src):
                                    if os.path.exists(dst):
                                        shutil.rmtree(src)
                                    else:
                                        shutil.move(src, dst)
                                else:
                                    shutil.move(src, dst)
                            os.rmdir(old_path)
                        else:
                            os.rename(old_path, new_path)
                        renamed_count += 1
                        changes_made = True
                        break

            if changes_made:
                break

    return renamed_count

print("Starting file content replacements...")
count = 0
for root, dirs, files in os.walk(directory):
    dirs[:] = [d for d in dirs if d not in skip_dirs]
    for file in files:
        if os.path.splitext(file)[1].lower() in skip_exts: continue
        if process_file(os.path.join(root, file)):
            count += 1

print(f"Updated {count} files with new text replacements.")

print("Starting file and folder renaming...")
rename_count = rename_directories_and_files(directory)
print(f"Renamed {rename_count} files/directories.")
