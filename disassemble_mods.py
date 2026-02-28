import zipfile
import subprocess
import os

minihud_jar = r"C:\Users\Taylor Allred\AppData\Roaming\PandoraLauncher\instances\1.21.11-1\.minecraft\mods\minihud-fabric-1.21.11-0.38.3.jar"
lighty_jar = r"C:\Users\Taylor Allred\AppData\Roaming\PandoraLauncher\instances\1.21.11-1\.minecraft\mods\lighty-fabric-3.1.3+1.21.11.jar"
out_dir = r"C:\temp\javap_out"
os.makedirs(out_dir, exist_ok=True)

def process_jar(jar_path, filter_str):
    with zipfile.ZipFile(jar_path, 'r') as z:
        for f in z.namelist():
            if filter_str in f and f.endswith(".class"):
                z.extract(f, out_dir)
                p = os.path.join(out_dir, f)
                with open(p + ".javap.txt", "w") as out_f:
                    subprocess.call(["javap", "-c", "-p", p], stdout=out_f)

process_jar(minihud_jar, "OverlayRendererSpawn")
process_jar(minihud_jar, "ShapeSpawnSphere")
process_jar(minihud_jar, "ShapeDespawnSphere")
process_jar(lighty_jar, "LightUpdateMixin")
process_jar(lighty_jar, "BlockPlacedMixin")

print("Disassembly complete!")
