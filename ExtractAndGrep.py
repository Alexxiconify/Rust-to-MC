import zipfile
import os

jar_path = r"C:\Users\Taylor Allred\AppData\Roaming\PandoraLauncher\instances\1.21.11-1\.minecraft\mods\minihud-fabric-1.21.11-0.38.3.jar"
out_dir = r"C:\temp\minihud_classes"
os.makedirs(out_dir, exist_ok=True)

with zipfile.ZipFile(jar_path, 'r') as z:
    for f in z.namelist():
        if f.endswith('.class'):
            z.extract(f, out_dir)

print("Extracted. Now grepping...")
found = []
for root, _, files in os.walk(out_dir):
    for f in files:
        if f.endswith('.class'):
            p = os.path.join(root, f)
            try:
                with open(p, 'rb') as fp:
                    content = fp.read()
                    if b'setNeedsUpdate' in content:
                        found.append(p)
            except Exception as e:
                pass

print("Found setNeedsUpdate in:")
for p in found:
    print(p)
