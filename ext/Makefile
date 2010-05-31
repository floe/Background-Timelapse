ROOT=$(subst :,,$(LD_LIBRARY_PATH))../..

convert:
	g++ -Wall -DTISCH_SHARED="" -ggdb -o convert convert.cc -I$(ROOT)/libs/simplecv -I$(ROOT)/libs/tools -L$(ROOT)/build/lib -lsimplecv
