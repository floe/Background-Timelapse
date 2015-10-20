// compile with: g++ -Wall -o convert_cv convert_cv.cc -lopencv_core -lopencv_imgproc -lopencv_highgui

#include <stdio.h>
#include <iostream>
#include "opencv2/core/core.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/highgui/highgui.hpp"

#include <vector>

using namespace std;
using namespace cv;

uint8_t buffer[1024*1024];
uint8_t outbuf[1024*1024];

int main(int argc, char* argv[]) {

  int h = 320;
  int w = 480;

 	FILE* fin = fopen( argv[1], "r" );
	fread( buffer, 1, 230400, fin );
	fclose(fin);

	Mat mYuv(h + h/2, w, CV_8UC1, buffer);
  Mat mRgba(h,w,CV_8UC4,outbuf);
  cvtColor( mYuv, mRgba, CV_YUV2BGR_NV21 );
  Mat result(h,w,CV_8UC4);
  cv::flip(mRgba.t(),result,1);

  imwrite(argv[2],result);

  return 0;
}
