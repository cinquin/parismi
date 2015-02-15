ParismiPlugins v0.1
Copyright (c) 2009-2015 Michael Chiang.
All rights reserved. This program and the accompanying materials
are made available under a dual license: the two-clause BSD license or
the GNU Public License v2.

To compile:
(1) Make sure you have the following libraries installed: boost, protobuf, tiff, 
    dc1394.
(2) Use the makefile (type make or gmake) to create shared library
    libsegpipeline_1.so or libsegpipeline_1.dylib depending on your OS
    (FreeBSD and OSX, respectively).  For your convenience, these libraries 
    have been pre-compiled for you and may work out of the box without 
    need for you to compile.
(3) Copy appropriate shared library into correct sub-directory in Parismi/IJ/native_libs/.
