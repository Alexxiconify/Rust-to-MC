import zipfile
import subprocess
import os

minihud_jar = r"C:\Users\Taylor Allred\AppData\Roaming\PandoraLauncher\instances\1.21.11-1\.minecraft\mods\minihud-fabric-1.21.11-0.38.3.jar"
out_dir = r"C:\temp\javap_out"
os.makedirs(out_dir, exist_ok=True)

with zipfile.ZipFile(minihud_jar, 'r') as z:
    for f in z.namelist():
        if ("OverlayRendererSpawn" in f or "ShapeSpawn" in f or "ShapeDespawn" in f or "fi/dy/masa/minihud/renderer/OverlayRendererLightLevel" in f) and f.endswith(".class"):
            z.extract(f, out_dir)
            p = os.path.join(out_dir, f.replace('/', os.sep))
            out_file = os.path.join(out_dir, os.path.basename(f) + ".javap.txt")
            with open(out_file, "w") as out_f:
                subprocess.call(["javap", "-c", "-p", p], stdout=out_f)

print("Disassembly complete!")
