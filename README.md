[![Build Status](https://travis-ci.org/cinquin/parismi.svg?branch=master)](https://travis-ci.org/cinquin/parismi)
<!---
[![Coverity Status](https://scan.coverity.com/projects/4509/badge.svg)](https://scan.coverity.com/projects/4509)
Put this back once Coverity have fixed their internal problems with the scanner
-->

**General notes**

We are releasing the code of our 3D image segmentation and cell cycle
analysis pipeline “Parismi” so that the results reported in our
publications can easily be reproduced, and so that the tool can be
reused and modified by other groups.

**Running Parismi**

Parismi can be run in interactive mode, as an ImageJ plugin. To give it
a quick try, [download the release
archive](http://cinquin.org.uk/static/Parismi.tgz), open
`Parismi/IJ/ij.jar` and select `A0PipelineManager` from the plugin menu.
Pipelines available in [released
datasets](http://cinquin.org.uk/static/Parismi_datasets.tgz) can be
loaded by drag and drop of corresponding `.xml` files onto the Parismi
control panel. See [Parismi operation overview](https://github.com/cinquin/parismi/blob/master/Parismi_operation_overview.pdf)
for more detail.

Parismi can also be run in batch mode, using the same pipelines that can
be edited and run from the GUI. Our datasets come with a system of
Makefiles that automate this process (see [released
datasets](http://cinquin.org.uk/static/Parismi_datasets.tgz)).

Much of Parismi’s functionality is implemented as integrated Java
plugins, which should work out of the box on a number of platforms. Some
important plugins — such as the active contour plugin — are implemented
in C++ and thus require a system-specific binary (x86-64 binaries are
provided for Mac OS X and FreeBSD, and will be made available for Debian
GNU/Linux shortly; these libraries require dependencies to have been
installed; see below). The cell detector plugin requires a Matlab
runtime.

**Functional tests**

A sample test set is provided, which covers a number of Parismi plugins.
Go to `Parismi/functional_tests/test_datasets/0/` and run GNU `make`
(you may need to compile the C++ plugins first; see below). If you want
to test the cell detector, you need to install a Matlab runtime of the
right version (currently R2014b for OS X and R2014a for Linux) or
compile the detector yourself from Matlab; using the Linux binary
provided at `cell_detector/bin/detect_executable_linux64_MCR_R2014a`
requires adjusting the script `IJ/matlab_interface/matlab_wrapper`.

**Future developments**

Note that there are features of Parismi that are not yet reported in
companion papers; many of these features are still work in progress. An
area of future development will be overhauling of the GUI pipeline
editor to make it graph-based.

**Requirements**

-   Operating system: Debian GNU/Linux (or most probably any other Linux
distribution as long as libdispatch is installed), Mac OS X, or FreeBSD
(or probably any other UNIX). These operating systems are listed by
increasing order of testing Parismi has received. No Microsoft Windows
version is available, although Parismi could probably be made largely
functional under Windows with fairly minor modifications.

-   Java \>= 8 (package `openjdk-8-jdk` under Debian).

-   A Matlab runtime to run the automatic cell detection plugin (free
binaries available from MathWorks for Linux and Mac OS X).

-   The following packages should be installed under Debian:
`libusb-1.0.0, libdc1394-22, libdc1394-22-dev, libtiff5-dev`. For
compilation purposes, the following packages should also be installed:
`git, ant, libdispatch-dev, libboost-dev, libprotobuf-dev,
protobuf-c-compiler, protobuf-compiler`. Mac OS X equivalents that can
be installed with MacPorts are `libdc1394, libusb, tiff `and` git-core,
apache-ant, boost, protobuf-cpp`.

-   Lots of RAM to deal with large images (Parismi was not optimized to
minimize RAM usage; our machines commonly have 16GB of RAM or more).

-   As many CPUs and CPU cores as possible (Parismi tries to make use of
as many cores as reported available by the hardware).

-   A C++ compiler, with block support if you wish to get
parallelization support (clang is probably the best choice), and some
open-source libraries listed below to compile the C++ plugins.

**Compiling**

A disk image of a Debian installation of Parismi will be provided at a
later time. This image will provide an installation of Parismi that will
work out of the box on any machine that can run Sun/Oracle’s free
VirtualBox.

If a JNA library is on the Java classpath it needs to be version 4 or
above. The presence of an older version of the native code part of the
library can prevent communication between the Java pipeline and the C++
plugins.

To compile the Java pipeline manager, run `ant` in the
`A0PipeLine_Manager` directory. This produces the file
`A0PipeLine_Manager.jar`, in which all dependencies are packaged and
that can be run in standalone fashion or act as an ImageJ plugin when
placed in the `IJ/plugins` directory.

To compile the C++ plugins, edit the library and include paths in the
Makefile in the `C++_plugins` directory if need be, and run GNU `make`.
This produces a `libsegpipeline_1.X` file that should be copied into the
relevant architecture-specific subdirectory in `IJ/native_libs`, and a
standalone executable that serves debugging purposes.

To compile the Matlab cell detection plugin, see the `cell_detector`
directory. Please adjust `IJ/matlab_interface/matlab_wrapper` to point
at the resulting executable.

**Credits**

We have made every effort not to reinvent the wheel and thus reused code
from a number of open source projects, either as libraries packaged with
Parismi or as source code modified for our purposes (kept for the most
part under “contrib” in our source trees). This includes in particular:

-   [ImageJ](http://rsb.info.nih.gov/ij/) by Wayne Rasband /
[Fiji](http://fiji.sc/Fiji): Parismi can be run as an ImageJ plugin,
from a customized version of ImageJ v1.43 that is distributed along with
Parismi (ImageJ is used to display images, and all ImageJ functionality
is available). The ImageJ code was tweaked to allow integration with our
annotation GUI, allow clickable orthogonal views of composite images
(modifying some code by Dimiter Prodanov), add some GUI shortcuts, and
resolve some concurrency issues. Some ImageJ and Fiji plugins were
slightly modified to improve performance with multithreading and to
allow integration with Parismi. Parismi’s TIFF image reading and writing
was adapted from ImageJ, with performance improvements, the use of the
BigTIFF file format to allow for larger file sizes, and native reading
of Zeiss LSM files.

-   Some ImageJ/Fiji plugins were adapted for use with Parismi, and
sometimes multi-threaded in the process; this includes skeletonization,
blob-finding, and hole-filling plugins by Ignacio Arganda-Carreras, Mark
Longair, and Stephan Preibisch. The Z projector plugin (by Patrick
Kelly) was more substantially modified.

-   [Principal curves by Balázs
Kégl](http://www.iro.umontreal.ca/~kegl/research/pcurves/): reused with
very minor modifications to define a path along the distal-proximal axis
of the *C. elegans* gonad.

-   [Implementation of kd trees by Simon
Levy](http://home.wlu.edu/~levys/software/kd/): used to efficiently find
neighbors of segmentation seeds.

-   [Ellipse fitter by Maurizio
Pilu](http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/PILU1/
ElliFit.java): used to fit ellipses on images of *C. elegans* embryos.
Slightly modified to decouple algorithm code from GUI and to report
ellipse characteristics.

-   Apache Commons Primitives library: used to provide number lists
backed by arrays of primitives rather than arrays of Number objects,
minimizing the performance and memory impact of large lists of numbers.

-   JCommander by Cédric Beust: used to parse command-line arguments.

-   JFreeChart by by David Gilbert: used to produce histograms and
pairwise plots to allow for interactive exploration of quantification
results.

-   Google’s Protobuf: used to to store segmentations to disk in a
space-efficient fashion, and to pass segmentations back and forth
between Java and C++ code.

-   [Protobuf patch by Ryan
Fogarty](https://code.google.com/p/protobuf/issues/detail?id=464): we
changed from custom modifications to the Java code generated by the
protobuf compiler to this patch, which modifies the protobuf compiler so
that lists of numbers are backed by arrays of primitives.

-   Google’s Guava: used to transparently cache data read from image
files that are too large to fit in RAM.

-   [Expr4J by Peter
Smith](http://sourceforge.net/projects/expr4j/files/): used to provide
spreadsheet-like functionality to explore quantification results, which
is still in a highly-experimental and unfinished state.

-   [libdc1394 by Damien
Douxchamps](http://sourceforge.net/projects/libdc1394/files/): used to
read images directly from FireWire cameras with an IEEE1394 interface.

-   Our distribution of ImageJ comes with a few plugins pre-installed
that we find convenient in our daily usage; notably an image stitching
plugin (Preibisch, S., Saalfeld, S., and Tomancak, P. (2009). Globally
optimal stitching of tiled 3D microscopic image acquisitions.
Bioinformatics 25, 1463–1465.) and dependencies including
[ImgLib](http://fiji.sc/ImgLib2), and the [LOCI Bio-Formats
Importer](http://loci.wisc.edu/software/bio-formats).

-   [3D Bresenham's line generation
code](http://www.mathworks.com/matlabcentral/fileexchange/21057-3d-
bresenham-s-line-generation) by Jimmy Shen

-   [Code for level set re-initialization by the Sussman
method](http://www.mathworks.com/matlabcentral/fileexchange/30284-active
-contours-implementation---test-platform-gui/content/Activeontours/
localized_seg.m) by Nikolay S.

-   [Eigenvalue and eigenvector computation
code](http://math.nist.gov/javanumerics/jama/) by NIST, Mathworks

-   [Code to generate randomly-distributed vectors on a
sphere](http://lettvin.com/Jonathan/diffuse.cpp) by Jonathan D. Lettvin

-   [Code for fast distance transform](http://cs.brown.edu/~pff/dt/) by
Pedro Felzenszwalb

-   Other libraries part of our distribution include XStream, JNA,
SwingX, and Eclipse annotations.

**License**

Parismi uses code from projects listed above, which come under various
licenses (GPL, BSD, Apache, etc.) or are in the public domain. The GPL
is the lowest common denominator. We are dual-licensing Parismi-specific
code and modifications to pre-existing code on a file-by-file basis,
under either the GPL or the BSD two-clause license. In other words,
anyone can freely modify Parismi’s code and redistribute it under the
GPL. Anyone who removes GPL-licensed code from Parismi should be able to
distribute the modified project under a BSD-like license.

Parismi uses YourKit for performance analysis and debugging of memory
leaks. YourKit supports open source projects with its full-featured Java
Profiler. YourKit, LLC is the creator of
[YourKit JavaProfiler](https://www.yourkit.com/java/profiler/index.jsp)
and
[YourKit .NET Profiler](www.yourkit.com/.net/profiler/index.jsp).
innovative and intelligent tools for profiling Java and .NET applications.
[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com)
