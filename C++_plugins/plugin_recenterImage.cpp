/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_recenterImage.h"
using namespace std;
using namespace boost;

static void getCenterOfMass(int &x_COM, int &y_COM, int &z_COM,
		Image3D<float> *I, float threshold) {

	// get image dimensions
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);

	// calculate un-normalized center of mass
	float M_total = 0, xCOM = 0, yCOM = 0, zCOM = 0;
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				float val = (*I)(x, y, z);
				if (val > threshold) {
					M_total += val;
					xCOM += val * x;
					yCOM += val * y;
					zCOM += val * z;
				}
			}
		}
	}

	// normalize center of mass
	x_COM = boost::math::iround(xCOM / M_total);
	y_COM = boost::math::iround(yCOM / M_total);
	z_COM = boost::math::iround(zCOM / M_total);
}

/*
 * Don't need this anymore
 */
/*
 static void padImage(int pad_x1, int pad_x2, int pad_y1, int pad_y2, int pad_z1, int pad_z2, Image3D<float> *I, CallbackFunctions *cb) {
 Image3D<float> padI(cb);
 int dimx,dimy,dimz;
 I->getDimensions(dimx,dimy,dimz);
 padI.resize(dimx+pad_x1+pad_x2,dimy+pad_y1+pad_y2,dimz+pad_z1+pad_z2);
 for (int x=0;x<dimx;x++) {
 for (int y=0;y<dimy;y++) {
 for (int z=0;z<dimz;z++) {
 padI(x+pad_x1,y+pad_y1,z+pad_z1)=(*I)(x,y,z);
 }
 }
 }
 (*I)=padI;
 }
 */

static void shiftImage(int shift_x, int shift_y, int shift_z,
		Image3D<float> *I, CallbackFunctions *cb) {
	Image3D<float> shiftI(cb);
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	shiftI.resize(dimx, dimy, dimz);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				// calculate shifted coordinates
				int xx = x + shift_x, yy = y + shift_y, zz = z + shift_z;
				// shift image
				if (xx >= 0 && xx < dimx && yy >= 0 && yy < dimy && zz >= 0
						&& zz < dimz) {
					shiftI(xx, yy, zz) = (*I)(x, y, z);
				}
			}
		}
	}
	(*I) = shiftI;
}

/*
 * Don't need this anymore
 */
/*
 static void getPadSz(int com, int dim, int &pad1, int &pad2) {
 int t1 = com;
 int t2 = dim-com-1;
 if (t1==t2) {
 pad1=0;
 pad2=0;
 } else if (t1<t2) {
 pad1 = abs(t1-t2);
 pad2 = 0;
 } else if (t1>t2) {
 pad1 = 0;
 pad2 = abs(t1-t2);
 }
 }
 */

static int getCenterCoordinate(int dim) {
	int center;
	if (dim % 2 == 0) {
		center = dim / 2 - 1;
	} else {
		center = (dim - 1) / 2 + 1;
	}
	return center;
}

/*!
 * Name		  : recenterImage
 * Description: Recenters image so that center of mass is in the center of the image
 * Arguments  : INPUT_IMAGE  : Input image
 * 				OUTPUT_IMAGE : Recentered output image
 * 				THRESHOLD	 : Threshold use when calculating center of mass
 * Syntax	  : ./segpipeline_FREEBSD INPUT_IMAGE 0 0 0 0 0 0 0 recenterImage 32 OUTPUT_IMAGE 0 0 0 0 0 0 0 0 << EOT
 * 				0 0 THRESHOLD
 * Notes	  : Only pixel values above THRESHOLD are used when calculating the center of mass.  OUTPUT_IMAGE has same
 *              dimensions as INPUT_IMAGE
 */
void recenterImage(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "Recentering image");

	// read in input image
	Image3D<float> I(cb);
	I.read(inputImage);

	// get recentering parameter
	log(cb, 4, "Prompt: 0 0 THRESHOLD");
	const char **work_storage = cb->getMoreWork();
	float prc = lexical_cast<float> (work_storage[2]);
	cb->freeGetMoreWork(work_storage);

	// get threshold
	float threshold = I.perctile(prc);

	// get center of mass
	int xCOM, yCOM, zCOM;
	getCenterOfMass(xCOM, yCOM, zCOM, &I, threshold);

	/*
	 // calculate how much to pad image in order to recenter it
	 int dimx,dimy,dimz;
	 I.getDimensions(dimx,dimy,dimz);
	 int pad1_x,pad2_x,pad1_y,pad2_y,pad1_z,pad2_z;
	 getPadSz(xCOM,dimx,pad1_x,pad2_x);
	 getPadSz(yCOM,dimy,pad1_y,pad2_y);
	 getPadSz(zCOM,dimz,pad1_z,pad2_z);

	 // pad image
	 padImage(pad1_x,pad2_x,pad1_y,pad2_y,pad1_z,pad2_z,&I,cb);
	 */

	// calculate how much to shift image in order to recenter it
	int dimx, dimy, dimz;
	I.getDimensions(dimx, dimy, dimz);
	int xCenter = getCenterCoordinate(dimx), yCenter =
			getCenterCoordinate(dimy), zCenter = getCenterCoordinate(dimz);
	int shift_x = xCenter - xCOM, shift_y = yCenter - yCOM, shift_z = zCenter
			- zCOM;

	// shift image
	shiftImage(shift_x, shift_y, shift_z, &I, cb);

	// write output image
	I.write(outputImage);
}
