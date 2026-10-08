from pathlib import Path
import hashlib
import io
import sys
import struct
import tarfile
import zipfile

repo = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1]) if len(sys.argv) > 1 else repo / 'launcher/app/build/outputs/apk/debug/app-debug.apk'
assert apk.is_file(), 'APK not built yet'

def check_elf(data, name):
    assert data[:4] == b'\x7fELF', name
    assert data[4] == 1, f'{name}: not ELF32'
    assert struct.unpack('<H', data[18:20])[0] == 40, f'{name}: not ARM'

with zipfile.ZipFile(apk) as archive:
    names = archive.namelist()
    libs = [n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert libs
    for name in libs:
        assert name.startswith('lib/armeabi-v7a/'), name
        check_elf(archive.read(name), name)
    assert 'lib/armeabi-v7a/libc++_shared.so' in names
    assert 'lib/armeabi-v7a/libsklauncher.so' in names
    assert 'lib/armeabi-v7a/libawt_xawt.so' in names
    assert b'-Djdk.util.jar.version=17\x00' in archive.read('lib/armeabi-v7a/libsklauncher.so'), 'ARM32 must select JNI instead of the Java 25 FFM layer'
    assert not any(n.startswith('assets/jre25/') for n in names)
    assert not any('natives-arm64.zip' in n for n in names)
    with tarfile.open(fileobj=io.BytesIO(archive.read('assets/jre25-arm32/bin-arm.tar.xz'))) as runtime:
        jvm = runtime.extractfile('./lib/server/libjvm.so').read()
        check_elf(jvm, 'Java VM')
        release = runtime.extractfile('./release').read().decode()
        assert 'JAVA_VERSION="25.0.3"' in release
        assert 'OS_ARCH="arm"' in release
    # Match the installed image: the two bad FCL entries are skipped by the
    # extractor, and the rebuilt AWT stub comes from nativeLibraryDir.
    installed = {}
    for asset in ['universal.tar.xz', 'bin-arm.tar.xz']:
        with tarfile.open(fileobj=io.BytesIO(archive.read('assets/jre25-arm32/' + asset))) as runtime:
            for member in runtime:
                name = member.name.removeprefix('./')
                if member.isfile() and name not in ['lib/libawt_xawt.so', 'lib/jspawnhelper']:
                    installed[name] = runtime.extractfile(member).read()
    installed['lib/libawt_xawt.so'] = archive.read('lib/armeabi-v7a/libawt_xawt.so')
    for name, data in installed.items():
        if data.startswith(b'\x7fELF'):
            check_elf(data, 'installed JRE: ' + name)
    assert 'lib/jspawnhelper' not in installed
    with zipfile.ZipFile(io.BytesIO(archive.read('assets/lwjgl/lwjgl-3.4.1-android-natives-arm32.zip'))) as natives:
        for name in natives.namelist():
            if name.endswith('.so'):
                check_elf(natives.read(name), name)
    for name in ['assets/cacio/cacio-shared.jar', 'assets/cacio/cacio-tta.jar',
                 'assets/frenchpress/frenchpress.jar', 'assets/sk/sk-bootstrap.jar']:
        assert name in names, name

print('PASS: APK libraries, installed JRE image and LWJGL are ARM32; ARM64 runtime entries excluded.')
print('APK bytes:', apk.stat().st_size)
print('SHA256:', hashlib.sha256(apk.read_bytes()).hexdigest())
