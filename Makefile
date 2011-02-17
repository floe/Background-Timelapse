SDK=$(shell cat local.properties | grep sdk-location | cut -d= -f2)

debug:
	ant debug
	$(SDK)/platform-tools/adb install -r bin/Timelapse-debug.apk

release:
	ant release
	jarsigner -verbose -keystore /home/echtler/media/docs/android-release.keystore bin/Timelapse-unsigned.apk android-release-key
	$(SDK)/tools/zipalign -f -v 4 bin/Timelapse-unsigned.apk bin/Timelapse-final.apk
	$(SDK)/platform-tools/adb install -r bin/Timelapse-final.apk

clean:
	-rm -r bin gen
