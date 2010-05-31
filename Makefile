debug:
	ant debug
	~/bin/adb install -r bin/Timelapse-debug.apk 

release:
	ant release
	jarsigner -verbose -keystore /home/echtler/media/docs/android-release.keystore bin/Timelapse-unsigned.apk android-release-key
	zipalign -f -v 4 bin/Timelapse-unsigned.apk bin/Timelapse-final.apk
	~/bin/adb install -r bin/Timelapse-final.apk

clean:
	-rm -r bin gen
