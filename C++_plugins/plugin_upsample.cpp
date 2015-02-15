/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_upsample.h"

static void scaleUp(int dimx_new, int dimy_new, int dimz_new,
		Image3D<float> *input, Image3D<float> *output) {
	// get dimensions of input, output images
	int dimx_old, dimy_old, dimz_old;
	input->getDimensions(dimx_old, dimy_old, dimz_old);
	output->resize(dimx_new, dimy_new, dimz_new);

	// get scaled up image
	for (int x = 0; x < dimx_new; x++) {
		for (int y = 0; y < dimy_new; y++) {
			for (int z = 0; z < dimz_new; z++) {
				float xf = (float) x;
				float yf = (float) y;
				float zf = (float) z;
				float dimx_newf = (float) dimx_new;
				float dimy_newf = (float) dimy_new;
				float dimz_newf = (float) dimz_new;
				float dimx_oldf = (float) dimx_old;
				float dimy_oldf = (float) dimy_old;
				float dimz_oldf = (float) dimz_old;
				int xOld = boost::math::iround(
						xf * (dimx_oldf - 1) / (dimx_newf - 1));
				int yOld = boost::math::iround(
						yf * (dimy_oldf - 1) / (dimy_newf - 1));
				int zOld = boost::math::iround(
						zf * (dimz_oldf - 1) / (dimz_newf - 1));
				(*output)(x, y, z) = (*input)(xOld, yOld, zOld);
			}
		}
	}
}

static void segmentationToProtobuf(Protobuf *output,
		Image3D<float> *fullSegmentation,
		Image3D<float> *perimeterSegmentation, CallbackFunctions *cb) {
	// get image dimensions
	int dimx, dimy, dimz;
	fullSegmentation->getDimensions(dimx, dimy, dimz);

	// get mapping from seed index to protobuf index
	Array1D<float> seedIdx(cb);
	output->getList("idx", &seedIdx);
	Array1D<int> protobufIdx(cb);
	protobufIdx.resize(boost::math::iround(seedIdx.max()) + 1);
	for (int i = 0; i < seedIdx.size(); i++) {
		int j = boost::math::iround(seedIdx(i));
		protobufIdx(j) = i;
	}

	// clear previous segmentaiton
	output->clearSegmentation();

	// get raw protobuf object, set new image dimensions
	ProtobufDirectory *proto = output->getProto();
	proto->set_image_dimx((float) dimx);
	proto->set_image_dimy((float) dimy);
	proto->set_image_dimz((float) dimz);

	// store sparse segmentations ordered by seed index
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				int seedIdx_full = boost::math::iround(
						(*fullSegmentation)(x, y, z));
				if (seedIdx_full != 0) {
					int protoIndex = protobufIdx(seedIdx_full);
					proto->mutable_protobuf_info(protoIndex)->add_image_fullseg_coords_x(
							x);
					proto->mutable_protobuf_info(protoIndex)->add_image_fullseg_coords_y(
							y);
					proto->mutable_protobuf_info(protoIndex)->add_image_fullseg_coords_z(
							z);
				}
				int seedIdx_perim = boost::math::iround(
						(*perimeterSegmentation)(x, y, z));
				if (seedIdx_perim != 0) {
					int protoIndex = protobufIdx(seedIdx_perim);
					proto->mutable_protobuf_info(protoIndex)->add_image_perimseg_coords_x(
							x);
					proto->mutable_protobuf_info(protoIndex)->add_image_perimseg_coords_y(
							y);
					proto->mutable_protobuf_info(protoIndex)->add_image_perimseg_coords_z(
							z);
				}
			}
		}
	}

}

static void getPerimeterSegmentation(Image3D<float> *fullSegmentation,
		Image3D<float> *perimeterSegmentation) {
	//set dimensions
	int dimx, dimy, dimz;
	fullSegmentation->getDimensions(dimx, dimy, dimz);
	perimeterSegmentation->resize(dimx, dimy, dimz);

	// calculate perimeter segmentation
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				// check if center pixel value is non-zero.  If non-zero, check if surrounding pixel values differ from center pixel value.  If so, then perimeter pixel
				int pixelValue = boost::math::iround(
						(*fullSegmentation)(x, y, z));
				if (pixelValue != 0) {
					for (int dx = -1; dx <= 1; dx++) {
						for (int dy = -1; dy <= 1; dy++) {
							for (int dz = -1; dz <= 1; dz++) {
								int xd = x + dx;
								int yd = y + dy;
								int zd = z + dz;
								if (xd < 0) {
									xd = 0;
								}
								if (yd < 0) {
									yd = 0;
								}
								if (zd < 0) {
									zd = 0;
								}
								if (xd >= dimx) {
									xd = dimx - 1;
								}
								if (yd >= dimy) {
									yd = dimy - 1;
								}
								if (zd >= dimz) {
									zd = dimz - 1;
								}
								int neighborValue = boost::math::iround(
										(*fullSegmentation)(xd, yd, zd));
								if (neighborValue != pixelValue) {
									(*perimeterSegmentation)(x, y, z)
											= (*fullSegmentation)(x, y, z);
								}
							}
						}
					}
				}
			}
		}
	}

}

/*!
 * Upsamples segmentation stored in protobuf file.
 *   Arguments:	input.proto - input protobuf file containing segmentation to be upsampled
 *   			output.proto - output protobuf file containing upsampled segmentation
 *   			DIM_X - x dimension of upsampled image
 *   			DIM_Y - y dimension of upsampled image
 *   			DIM_Z - z dimension of upsampled image
 *   Syntax:	$BIN ... input.proto ... upsample 32 ... output.proto ... << EOT
 *   			0 0 DIM_X DIM_Y DIM_Z
 *   			EOT
 *   Notes:		Segmentation upsampling is done through nearest neighbor interpolation to
 *   			avoid artifacts.  Note that x,y,z resolution is not updated in protobuf file.
 */
void upSample(TextIO* textInput, TextIO* textOutput, CallbackFunctions *cb) {
	log(cb, 4, "Up-sampling segmentation");

	// get input segmentation
	Protobuf *proto = new Protobuf(cb);
	proto->readProto(textInput);
	Image3D<float> *I = new Image3D<float> (cb);
	Array1D<float> idx(cb);
	proto->getList("idx", &idx);
	proto->drawSegmentationImage("full", I, &idx);

	// get parameters
	log(cb, 4, "Prompt: 0 0 DIM_X DIMY_Y DIMZ_Z");
	const char **work_storage = cb->getMoreWork();
	int dimx = atoi(work_storage[2]);
	int dimy = atoi(work_storage[3]);
	int dimz = atoi(work_storage[4]);
	cb->freeGetMoreWork(work_storage);

	// get upscaled full segmentation
	Image3D<float> *fullSegmentation = new Image3D<float> (cb);
	scaleUp(dimx, dimy, dimz, I, fullSegmentation);

	// get upscaled perimeter segmentation
	Image3D<float> *perimeterSegmentation = new Image3D<float> (cb);
	getPerimeterSegmentation(fullSegmentation, perimeterSegmentation);

	// store segmentation to protobuf file
	segmentationToProtobuf(proto, fullSegmentation, perimeterSegmentation, cb);

	// write protobuf file
	proto->writeProto(textOutput);

	// clean up
	delete proto;
	proto = NULL;
	delete I;
	I = NULL;
	delete fullSegmentation;
	fullSegmentation = NULL;
	delete perimeterSegmentation;
	perimeterSegmentation = NULL;

}
