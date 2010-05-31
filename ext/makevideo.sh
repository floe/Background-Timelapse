#!/bin/bash
for file in *.yuv ; do
	echo $file
	[ -e ${file/yuv/ppm} ] || ./convert $file ${file/yuv/ppm}
	#mogrify -rotate 90 ${file/yuv/ppm}
done

ffmpeg -r 25 -i img%06d.png -vcodec libx264 -b 1000000 output.mp4

