/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_rayTraceFiller.h"
using namespace std;
using namespace boost::math;

/*!
 * Name		  : rayTraceFiller
 * Description: Used to generate rachis segmentation
 * Arguments  : PROTO_INPUT  : protobuf input containing segmented germline
 * 				IMAGE_OUTPUT : float image containing intermediate rachis segmentation.  This
 * 				               image needs to be post-processed (thresholding, throw out
 * 				               unconnected components).
 * Syntax	  : ./segpipeline_FREEBSD 0 0 0 0 0 PROTO_INPUT 0 0 rayTraceFiller 32 IMAGE_OUTPUT 0 0 0 0 0 0 0 0
 * Notes	  : This function has a random component, so you won't get the exact same result every time.
 */
void rayTraceFiller(TextIO* inputText, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "Calculating rachis segmentation through ray tracing");

	// generate full segmentation
	Protobuf proto(cb);
	proto.readProto(inputText);
	Image3D<float> *I = new Image3D<float> (cb);
	Array1D<float> color(cb);
	proto.getList("protobuf_index", &color);
	color + 1;
	proto.drawSegmentationImage("full", I, &color);

	// generate points spaced uniformly on a sphere
	double Jiggle = 0.03 / sqrt(double(NUM_UNIFORM_POINTS_ON_SPHERE));
	unsigned int Rounds = 5000;
	points P("default", NUM_UNIFORM_POINTS_ON_SPHERE, Jiggle, Rounds);
	Array1D<float> *xUniformP = new Array1D<float> (cb);
	Array1D<float> *yUniformP = new Array1D<float> (cb);
	Array1D<float> *zUniformP = new Array1D<float> (cb);
	P.report(xUniformP, yUniformP, zUniformP);

	// allocate rayScore.   0 if not yet checked, 1 if collision, 2 if no collision
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	boost::multi_array<unsigned char, 4> *rayScore = new boost::multi_array<
			unsigned char, 4>(
			boost::extents[dimx][dimy][dimz][NUM_UNIFORM_POINTS_ON_SPHERE]);

	// allocate array for progress bar
	Array1D<float> *progress = new Array1D<float> (cb);
	progress->resize(NUM_UNIFORM_POINTS_ON_SPHERE);

	// do ray tracing
	log(cb, 4, "Starting ray tracing");
#if defined USELIBDISPATCH
	dispatch_apply(NUM_UNIFORM_POINTS_ON_SPHERE, dispatch_get_global_queue(0,0), ^(size_t ii) {
#else
	for (int ii = 0; ii < NUM_UNIFORM_POINTS_ON_SPHERE; ii++) {
#endif
		int i = (int) ii;

		// get a discrete line
		int xP = iround(LENGTH_BRESENHAM_LINE * (*xUniformP)(i));
		int yP = iround(LENGTH_BRESENHAM_LINE * (*yUniformP)(i));
		int zP = iround(LENGTH_BRESENHAM_LINE * (*zUniformP)(i));
		Array1D<int> xLine(cb), yLine(cb), zLine(cb);
		bresenhamLine(&xLine, &yLine, &zLine, xP, yP, zP);

		// check if ray hits image boundary for every pixel in the image
		for (int xc = 0; xc < dimx; xc++) {
			for (int yc = 0; yc < dimy; yc++) {
				for (int zc = 0; zc < dimz; zc++) {

					// only do ray tracing if ray has not passed through point previously.
					if ((*rayScore)[xc][yc][zc][i] == 0) {
						// iterate over length of ray
						for (int r = 0; r < yLine.size(); r++) {
							int x = xc + xLine(r), y = yc + yLine(r), z = zc
									+ zLine(r);
							if (x >= 0 & x < dimx & y >= 0 & y < dimy & z >= 0
									& z < dimz) { // check if ray in image boundary
								if ((*I)(x, y, z) > BWTHRESH) { // if collision detected, update collision information for all points on ray
									for (int rr = 0; rr <= r; rr++) {
										int xx = xc + xLine(rr), yy = yc
												+ yLine(rr), zz = zc
												+ zLine(rr);
										(*rayScore)[xx][yy][zz][i] = 1;
									}
									break;
								}
								// else ray has reach image boundary
							} else {
								// update non-collision information on all points on the ray
								for (int rr = 0; rr < r; rr++) {
									int xx = xc + xLine(rr), yy = yc
											+ yLine(rr), zz = zc + zLine(rr);
									(*rayScore)[xx][yy][zz][i] = 2;
								}
								break; // break out of iteration over length of the ray
							}
						} // end iterate over length of ray
					}
				}
			}
		}

		// update progress
		(*progress)(i) = 1;
		int progressOutOf100 = (int) (progress->sum()
				/ NUM_UNIFORM_POINTS_ON_SPHERE * 100);
		cb->progressReport(progressOutOf100);
#if defined USELIBDISPATCH
	});
#else
	}
#endif
	log(cb, 4, "Done with ray tracing");

	// generate output image
	log(cb, 4, "Generating output image");
	Image3D<float> *output = new Image3D<float> (cb);
	output->resize(dimx, dimy, dimz);
#if defined USELIBDISPATCH
	dispatch_apply((unsigned int)dimz, dispatch_get_global_queue(0,0), ^(size_t zz) {
#else
	for (int zz = 0; zz < dimz; zz++) {
#endif
		int z = (int) zz;
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				// skip over pixels in segmentaiton mask
				if ((*I)(x, y, z) > BWTHRESH) {
					continue;
				}
				// for each pixel in the image, calculate the percentage of rays that hit segmentation mask
				float numRaysHitMask = 0;
				for (int i = 0; i < NUM_UNIFORM_POINTS_ON_SPHERE; i++) { // iterate over all rays
					if ((*rayScore)[x][y][z][i] == 1) {
						numRaysHitMask++;
					}
				}
				float percentageRaysHitMask = numRaysHitMask
						/ NUM_UNIFORM_POINTS_ON_SPHERE;
				(*output)(x, y, z) = percentageRaysHitMask;
			}
		}
#if defined USELIBDISPATCH
	});
#else
	}
#endif
	log(cb, 4, "Done generating output image");

	// write output image
	output->write(outputImage);

	// clean up
	delete I;
	I = NULL;
	delete rayScore;
	rayScore = NULL;
	delete xUniformP;
	xUniformP = NULL;
	delete yUniformP;
	yUniformP = NULL;
	delete zUniformP;
	zUniformP = NULL;
	delete output;
	output = NULL;
	delete progress;
	progress = NULL;
}
