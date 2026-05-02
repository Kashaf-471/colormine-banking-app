import os
import glob

res_dir = r"c:\Users\HP\Downloads\ColorMineBank\app\src\main\res\layout"

for filepath in glob.glob(os.path.join(res_dir, "*.xml")):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    if 'android:tint=' in content:
        content = content.replace('android:tint=', 'app:tint=')
        if 'xmlns:app="http://schemas.android.com/apk/res-auto"' not in content:
            content = content.replace('xmlns:android="http://schemas.android.com/apk/res/android"', 'xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:app="http://schemas.android.com/apk/res-auto"')
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

manifest_path = r"c:\Users\HP\Downloads\ColorMineBank\app\src\main\AndroidManifest.xml"
with open(manifest_path, 'r', encoding='utf-8') as f:
    manifest_content = f.read()
    
if 'android.hardware.telephony' not in manifest_content:
    manifest_content = manifest_content.replace(
        '<uses-permission android:name="android.permission.SEND_SMS" />',
        '<uses-feature android:name="android.hardware.telephony" android:required="false" />\n    <uses-permission android:name="android.permission.SEND_SMS" />'
    )
    with open(manifest_path, 'w', encoding='utf-8') as f:
        f.write(manifest_content)

print("Fixed lint errors.")
