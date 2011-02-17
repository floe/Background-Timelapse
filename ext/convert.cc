#include "YUV420SPImage.h"
#include "ColorLUT.h"
#include <stdio.h>
#include <fstream>

int main( int argc, char* argv[] ) {

	initLUT();

	YUV420SPImage img(640,480);

	FILE* fin = fopen( argv[1], "r" );
	fread( img.getData(), 1, 460800, fin );
	fclose(fin);

	RGBImage out(640,480);

	img.getImage( out );

	std::ofstream fout( argv[2] );
	fout << out;
	fout.close();
}
