/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_CDiameterQuantification.h"
using namespace std;
using namespace boost;

/*!
 * Returns normalized vectors [x1,y1,z1] and [x2,y2,z2] that
 * are orthogonal to input vector [x,y,z].
 */
static void getOrthogonalVectors(float &x1, float &y1, float &z1, float &x2,
		float &y2, float &z2, float x, float y, float z, CallbackFunctions *cb) {
	// Sanity check: input vector should be non-zero
	if (isCloseTo(x, 0) & isCloseTo(y, 0) & isCloseTo(z, 0)) {
		log(cb, 1, "Error: Non-zero vector required");
		throw 999;
	}

	// find orthogonal vectors to use as basis for plane
	if (isCloseTo(x, 0) & isCloseTo(y, 0)) {
		x1 = 1;
		y1 = 0;
		z1 = 0;
		x2 = 0;
		y2 = 1;
		z2 = 0;
	} else {
		float xTemp = 0, yTemp = 0, zTemp = 1;
		// cross product to obtain first orthogonal vector 1
		x1 = yTemp * z - zTemp * y;
		y1 = zTemp * x - xTemp * z;
		z1 = xTemp * y - yTemp * x;
		// cross product to obtain second orthogonal vector 2
		x2 = y * z1 - z * y1;
		y2 = z * x1 - x * z1;
		z2 = x * y1 - y * x1;
	}

	// normalize orthogonal vectors
	float length1 = sqrt(x1 * x1 + y1 * y1 + z1 * z1);
	x1 = x1 / length1;
	y1 = y1 / length1;
	z1 = z1 / length1;
	float length2 = sqrt(x2 * x2 + y2 * y2 + z2 * z2);
	x2 = x2 / length2;
	y2 = y2 / length2;
	z2 = z2 / length2;
}

static float getRayTracingDiameter(Image3D<float> *debuggingOutput, int xC,
		int yC, int zC, float xVec, float yVec, float zVec, int numRays,
		Image3D<float> *segmentation, CallbackFunctions *cb) {

	// get vectors [xV1,yV1,zV1] and [xV2,yV2,zV2] orthogonal to [xVec,yVec,zVec]
	float xV1, yV1, zV1, xV2, yV2, zV2 = 0;
	getOrthogonalVectors(xV1, yV1, zV1, xV2, yV2, zV2, xVec, yVec, zVec, cb);

	// get x,y,z coordinates of uniformly distributed rays emanating from [x,y,z] orthogonal to [xVec,yVec,zVec] and of unit length
	Array1D<float> *xRay = new Array1D<float> (cb);
	Array1D<float> *yRay = new Array1D<float> (cb);
	Array1D<float> *zRay = new Array1D<float> (cb);
	xRay->resize(numRays);
	yRay->resize(numRays);
	zRay->resize(numRays);
	for (int i = 0; i < numRays; i++) {
		float theta = (float) (i) * 2 * PI / (float) (numRays);
		float y3 = yV1 * sin(theta) + yV2 * cos(theta);
		float x3 = xV1 * sin(theta) + xV2 * cos(theta);
		float z3 = zV1 * sin(theta) + zV2 * cos(theta);
		(*xRay)(i) = x3;
		(*yRay)(i) = y3;
		(*zRay)(i) = z3;
	}

	// allocate memory for rayLength, vector containing length of ray from (x,y,z) until it hits a segmentation boundary
	Array1D<float> *rayLength = new Array1D<float> (cb);

	// allocate memory for debugging output, image containing rays extending until they hit boundary
	int dimx, dimy, dimz;
	segmentation->getDimensions(dimx, dimy, dimz);
	debuggingOutput->resize(dimx, dimy, dimz);

	// Iterate through rays emanating from (x,y,z) and calculate how long they are when they hit a segmentation boundary
	__block volatile bool keepGoing = 1;
	for (int i = 0; i < numRays; i++) { // Future work: one possible improvement is to parallelize this code
		if (keepGoing == 1) {
			if (cb->shouldInterrupt()) {
				log(cb, 4, "Interrupting plugin");
				keepGoing = 0;
			} else {
				// get coordinates of a single, discrete ray using Bresenham algorithm
				int xP = (int) (MAX_RAY_LENGTH * (*xRay)(i));
				int yP = (int) (MAX_RAY_LENGTH * (*yRay)(i));
				int zP = (int) (MAX_RAY_LENGTH * (*zRay)(i));
				Array1D<int> xLine(cb), yLine(cb), zLine(cb);
				bresenhamLine(&xLine, &yLine, &zLine, xP, yP, zP);
				(*rayLength)(i) = -1;
				for (int r = 0; r < yLine.size(); r++) { // iterate over length of ray
					int x = xC + xLine(r);
					int y = yC + yLine(r);
					int z = zC + zLine(r);
					// check if ray in image boundary
					if (x >= 0 && x < dimx && y >= 0 && y < dimy && z >= 0 && z
							< dimz) {
						// if collision detected
						if (isCloseTo((*segmentation)(x, y, z), 0)) {
							// store length of ray when collided with segmentation
							(*rayLength)(i) = r - 1;
							break; // break out of iteration over length of ray
						}
						(*debuggingOutput)(x, y, z) = 1;
						// Otherwise, ray has reached image boundary
					} else {
						(*rayLength)(i) = r - 1;
						log(cb, 0,
								"Warning: ray hit image boundary before hitting segmentation boundary");
						break; // break out of iteration over length of the ray
					}
				}
				// Check  to make sure ray either hit segmentation boundary or image bondouary
				if (isCloseTo((*rayLength)(i), -1)) {
					log(
							cb,
							0,
							"Warning: Ray reached end of ray without collision with segmentation boundary or image boundary. You should increase ray size");
					(*rayLength)(8) = rayLength->size();
				}
			}
		}
	}

	// get mean diameter using crude estimation technique
	float sum = 0;
	for (int i = 0; i < numRays; i++) {
		sum += (*rayLength)(i) * (*rayLength)(i);
	}
	float meanDiameter = 2.0f * sqrtf(sum / (float) numRays);

	// clean up
	delete xRay;
	xRay = NULL;
	delete yRay;
	yRay = NULL;
	delete zRay;
	zRay = NULL;
	delete rayLength;
	rayLength = NULL;

	// return
	return meanDiameter;
}

/*!
 * This plugin calculates diameter of a segmentation along its backbone.  The backbone
 * should be parameterized by:
 *   seed_x, seed_y, seed_z: x,y,z coordinate of a backbone control point
 *   vec_x, vec_y, vec_z   : x,y,z coordinates of vector pointing to next backbone
 *                           control point
 *
 * The segmentation should be a binary image with values 1 in the segmentation and 0
 * outside the segmentation.  For example, if you were quantify rachis diameters, then
 * the rachis segmentation should be 1.
 */
void CDiameterQuantification(ImageIO* inputImage, TextIO* inputText,
		ImageIO* outputImage, TextIO* outputText, CallbackFunctions *cb) {
	log(cb, 4, "Calculating rachis diameter along backbone");

	// get parameters
	log(cb, 4, "Prompt: 0 0 NUMRAYS MAXSIZERAY ZSCALE");
	const char **work_storage = cb->getMoreWork();
	int numRays = atoi(work_storage[2]);
	float scaleZ = lexical_cast<float> (work_storage[4]);
	cb->freeGetMoreWork(work_storage);

	// read in input image
	Image3D<float> I(cb);
	I.read(inputImage);

	// read input protobuf file containing backbone
	Protobuf proto(cb);
	proto.readProto(inputText);

	// read backbone
	Array1D<float> xC(cb), yC(cb), zC(cb), xVec(cb), yVec(cb), zVec(cb);
	proto.getList("seed_x", &xC);
	proto.getList("seed_y", &xC);
	proto.getList("seed_z", &xC);
	proto.getList("vec_x", &xVec);
	proto.getList("vec_y", &yVec);
	proto.getList("vec_z", &zVec);

	// rescale z-vector
	int numBackbonePoints = xC.size();
	for (int i = 0; i < numBackbonePoints; i++) {
		zVec(i) = zVec(i) * scaleZ;
	}

	// initializing output image with orthogonal rays
	Image3D<float> debuggingOutput(cb);
	int dimx, dimy, dimz;
	I.getDimensions(dimx, dimy, dimz);
	debuggingOutput.resize(dimx, dimy, dimz);

	// iterate through backbone can calculate radius at backbone
	Array1D<float> diameterStorage(cb);
	diameterStorage.resize(numBackbonePoints);
	for (int i = 0; i < numBackbonePoints; i++) {
		float diameter = getRayTracingDiameter(&debuggingOutput, (int) xC(i),
				(int) yC(i), (int) zC(i), xVec(i), yVec(i), zVec(i), numRays,
				&I, cb);
		diameterStorage(i) = diameter;
	}

	// write out output image, output protobuf file
	debuggingOutput.write(outputImage);
	proto.setList("rachisDiameter", &diameterStorage);
	proto.writeProto(outputText);
}

