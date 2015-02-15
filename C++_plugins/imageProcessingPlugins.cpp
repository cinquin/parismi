/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "imageProcessingPlugins.h"
#include "CallbackFunctions.h"
#include "Image3D.h"
#include "plugin_normalize.h"
#include "plugin_blur.h"
#include "plugin_distanceTransform.h"
#include "plugin_boxNormalize.h"
#include "plugin_principalCurvature.h"
#include "plugin_imadjust.h"
#include "plugin_activeContours.h"
#include "plugin_CDiameterQuantification.h"
#include "plugin_clearSegmentation.h"
#include "plugin_quantify.h"
#include "plugin_volume.h"
#include "plugin_topLayer.h"
#include "plugin_grabVideo.h"
#include "plugin_recenterImage.h"
#include "plugin_protoToImage.h"
#include "plugin_emptyImage.h"
#include "plugin_downsample.h"
#include "plugin_djikstraDistance.h"
#include "plugin_unaryOperation.h"
#include "plugin_upsample.h"
#include "plugin_rayTraceFiller.h"
#include "plugin_erode.h"
#include "util.h"
#include "plugin_dilate.h"
#include "plugin_rescaleZ.h"
#include "plugin_reorderSeedIndices.h"
#include "plugin_dummy.h"

int imageProcessingPlugins(vector<ImageIO*> imageInputs,
		vector<TextIO*> textInputs, char *command,
		vector<ImageIO*> imageOutputs, vector<TextIO*> textOutputs,
		CallbackFunctions *cb) {

	// display pid
	pid_t p = getpid();
	log(cb, 4, "pid= %d, command= %s", p, command);
	int rn = 0;

	// run appropriate plugin
	if (strcmp(command, "normalize") == 0) {
		normalize(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "unaryOperation") == 0) {
		unaryOperation(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "cell_row_counter") == 0) {
		djikstraDistance(textInputs[0], textOutputs[0], cb);
	} else if (strcmp(command, "upsample") == 0) {
		upSample(textInputs[0], textOutputs[0], cb);
	} else if (strcmp(command, "downsample") == 0) {
		downSample(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "blur") == 0) {
		gaussianBlur(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "distanceTransform") == 0) {
		distanceTransform(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "box_normalize") == 0) {
		boxNormalize(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "principal_curvature") == 0) {
		principalCurvature(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "imadjust") == 0) {
		imadjust(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "active_contours") == 0) {
		active_contours(imageInputs[0], textInputs[0], imageOutputs[0],
				imageOutputs[1], textOutputs[0], false, cb);
	} else if (strcmp(command, "active_contours_movie") == 0) {
		active_contours(imageInputs[0], textInputs[0], imageOutputs[0],
				imageOutputs[1], textOutputs[0], true, cb);
	} else if (strcmp(command, "CDiameterQuantification") == 0) {
		CDiameterQuantification(imageInputs[0], textInputs[0], imageOutputs[0],
				textOutputs[0], cb);
	} else if (strcmp(command, "clearSegmentation") == 0) {
		clearSegmentation(textInputs[0], textOutputs[0], cb);
	} else if (strcmp(command, "quantify") == 0) {
		quantify(cb);
	} else if (strcmp(command, "top_layer") == 0) {
		topLayer(textInputs[0], textOutputs[0], cb);
	} else if (strcmp(command, "protobuf_volume") == 0) {
		volume(textInputs[0], textOutputs[0], cb);
	} /* else if (strcmp(command, "grab_video") == 0) {
		rn = grab_video(imageInputs[0], imageInputs[1], imageOutputs[0],
				imageOutputs[1], cb);
	} */ else if (strcmp(command, "active_contours_deprecated") == 0) {
	} else if (strcmp(command, "recenterImage") == 0) {
		recenterImage(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "proto2image") == 0) {
		proto2image(textInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "emptyImage") == 0) {
		emptyImage(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "rayTraceFiller") == 0) {
		rayTraceFiller(textInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "erode") == 0) {
		imerode(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "dilate") == 0) {
		imdilate(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "rescaleZ") == 0) {
		rescaleZ(imageInputs[0], imageOutputs[0], cb);
	} else if (strcmp(command, "reorder_protobuf_idx") == 0) {
		reorderSeedIndices(textInputs[0], textOutputs[0], cb);
	} else if (strcmp(command, "dummyPlugin") == 0) {
		dummyPlugin(imageInputs[0], imageOutputs[0], cb);
	} else {
		log(cb, 0, "Invalid command: %s", command);
		return 1;
	}

	log(cb, 4, "Completed plugin operation");

	return rn;
}
