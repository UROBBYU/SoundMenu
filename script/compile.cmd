rmdir /q /s out\build\SoundMenu
del /f out\zips\SoundMenu.zip
"C:\Users\UROBBYU\.jdks\adopt-openjdk-14.0.2\bin\jlink" --module-path "C:\Users\UROBBYU\.jdks\adopt-openjdk-14.0.2\jmods;out\production\SoundMenu" --add-modules SoundMenu --output "out\build\SoundMenu" --compress=2
copy script\start.cmd out\build\SoundMenu
copy SoundVolumeView.exe out\build\SoundMenu
copy GetNir.exe out\build\SoundMenu
copy icon.ico out\build\SoundMenu
cd out\build\
del /f SoundMenu\bin\java.exe
"C:\Program Files\7-Zip\7z" a -tzip ..\zips\SoundMenu SoundMenu\*
exit