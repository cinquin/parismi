
OS:=$(shell uname -s | tr a-z A-Z)

CFLAGS = -DUSELIBDISPATCH -std=c++11 -Werror -Wall -pedantic -Weverything -Wno-c++98-compat -Wno-old-style-cast -Wno-padded -Wno-old-style-cast -g -fmessage-length=0 -fexceptions
DEBUG_FLAGS = -O3
RELEASE_FLAGS = -ffast-math -funroll-loops -fstrict-aliasing -Wstrict-aliasing=2 -DBOOST_DISABLE_ASSERTS

CLANG_PATH = clang

ifneq (,$(findstring FREEBSD, $(OS)))
	RELEASE_FLAGS+= -Ofast
	MAKE=gmake
	CC=$(CLANG_PATH)
	CFLAGS+= -fPIC -fblocks 
        LDFLAGS= -L/usr/lib -L/usr/local/lib  -L/lib -ldispatch -lm -lprotobuf -ltiff -ldc1394 -lusb -lexecinfo -lBlocksRuntime -lc++ 
	INCLUDE= -I. -isystem /usr/include -isystem /usr/local/include -isystem /opt/local/include/google/protobuf -isystem /opt/local/include
	INCLUDE_MAKEDEPEND= -I/usr/include/c++/4.2

	DYLIB_SUFFIX=.so
endif

ifneq (,$(findstring LINUX, $(OS)))
	RELEASE_FLAGS+= -O3
	MAKE=make
	CC=$(CLANG_PATH)
	CFLAGS+= -fPIC -fblocks -DLINUX
	LDFLAGS= -L/usr/lib/x86_64-linux-gnu/ -L/usr/local/lib -L /usr/lib -L/lib -ldispatch -lm -lprotobuf -ldc1394 -lusb-1.0 -lBlocksRuntime -ltiff
	INCLUDE= -I. -I/usrinclude -I/usr/local/include
	DYLIB_SUFFIX=.so
endif

ifneq (,$(findstring DARWIN, $(OS)))
	RELEASE_FLAGS+= -O3
	MAKE=make
	CC=$(CLANG_PATH)
	LDFLAGS= -lstdc++ -lm -lpthread -framework IOKit -framework CoreServices -L/opt/local/lib/ -L/usr/local/lib/ -lprotobuf -ltiff -ldc1394 -lusb-1.0 
	INCLUDE=-I. -isystem /usr/include -isystem /opt/local/include/google/protobuf -isystem /opt/local/include -isystem /usr/local/include
	INCLUDE_MAKEDEPEND= -I/usr/include/c++/4.2.1
        RELEASE_FLAGS+= -mdynamic-no-pic 
	DYLIB_SUFFIX=.dylib
endif

EXEC_NAME=segpipeline_${OS}
DYLIB_NAME=libsegpipeline_1${DYLIB_SUFFIX}
CFLAGS+=$(INCLUDE)

all: release
debug: CFLAGS+= $(DEBUG_FLAGS)
debug: MODE=debug
debug: preprocess_debug
debug: protobuf $(EXEC_NAME) $(DYLIB_NAME)
release: CFLAGS+= $(RELEASE_FLAGS)
release: MODE=release
release: preprocess_release
release: protobuf $(EXEC_NAME) $(DYLIB_NAME)
clean: clean_protobuf
	rm -f *.o $(EXEC_NAME) $(DYLIB_NAME) gitVersion.h
clean_protobuf:
	rm -f *.pb.* 
preprocess_release:
	@echo Building $(EXEC_NAME) and $(DYLIB_NAME), release build for $(UNAMEARG) with sources files $(SRCS)...

preprocess_debug:
	@echo Building $(EXEC_NAME) and $(DYLIB_NAME), debug build for $(UNAMEARG) with sources files $(SRCS)...

FORCE:
gitVersion.h: FORCE
	echo "static const char *gitVersion = \"$(shell git rev-parse HEAD && git diff-index HEAD); compilation flags: $(CFLAGS) $(LDFLAGS) \";" > $@

protobuf: protobuf_package.pb.h

protobuf_package.pb.h: protobuf_package.proto
	protoc -I=. --cpp_out=. ./protobuf_package.proto 
	mv protobuf_package.pb.h temp
	echo "#pragma clang diagnostic push" > protobuf_package.pb.h
	echo '#pragma clang diagnostic ignored "-Wpadded"' >> protobuf_package.pb.h
	echo '#pragma clang diagnostic ignored "-Wshorten-64-to-32"' >> protobuf_package.pb.h
	echo '#pragma clang diagnostic ignored "-Wglobal-constructors"' >> protobuf_package.pb.h
#Avoid errors when running older versions of clang that do not have reserved-id-macro warning
	echo '#pragma clang diagnostic ignored "-Wunknown-pragmas"' >> protobuf_package.pb.h
	echo '#pragma clang diagnostic ignored "-Wreserved-id-macro"' >> protobuf_package.pb.h
	cat temp >>  protobuf_package.pb.h 
	echo "#pragma clang diagnostic pop" >> protobuf_package.pb.h
	mv protobuf_package.pb.cc temp
	echo "#pragma clang diagnostic push" > protobuf_package.pb.cc
	echo '#pragma clang diagnostic ignored "-Wshorten-64-to-32"' >> protobuf_package.pb.cc
	echo '#pragma clang diagnostic ignored "-Wsign-conversion"' >> protobuf_package.pb.cc
	echo '#pragma clang diagnostic ignored "-Wmissing-variable-declarations"' >> protobuf_package.pb.cc
	echo '#pragma clang diagnostic ignored "-Wglobal-constructors"' >> protobuf_package.pb.cc
	echo '#pragma clang diagnostic ignored "-Wunused-macros"' >> protobuf_package.pb.cc
	cat temp >>  protobuf_package.pb.cc
	echo "#pragma clang diagnostic pop" >> protobuf_package.pb.cc
	rm temp	

protobuf_package.pb.cc: protobuf

SRCS= $(wildcard *.cpp) protobuf_package.pb.cc
OBJS1= $(SRCS:.cpp=.o)
OBJS = $(OBJS1:.cc=.o)

.cpp.o:
	$(CC) $(CFLAGS) -c $<  -o $@

.cc.o:
	$(CC) $(CFLAGS) -c $<  -o $@

$(EXEC_NAME): $(OBJS) gitVersion.h
	$(CC) $(OBJS) -o $(EXEC_NAME) $(LDFLAGS)

$(DYLIB_NAME): $(OBJS) gitVersion.h
	$(CC) -shared $(OBJS) -o $(DYLIB_NAME) $(LDFLAGS)

.PHONY: depend clean

depend: $(SRCS)
	touch Makefile_dependencies_full
	@echo include $(INCLUDE_MAKEDEPEND) $(INCLUDE)
	makedepend -fMakefile_dependencies_full $(INCLUDE_MAKEDEPEND) $(INCLUDE) $^
	@echo $(shell grep -v /usr/ Makefile_dependencies_full > Makefile_dependencies)

-include Makefile_dependencies
