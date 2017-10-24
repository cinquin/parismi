/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "Protobuf.h"
#include <google/protobuf/io/coded_stream.h>

using namespace google::protobuf::io;
using namespace google::protobuf;
using namespace std;

/*!
 * Class constructor.
 * \param callback A struct of useful callback functions
 */
Protobuf::Protobuf(CallbackFunctions *callback) {
	cb = callback;
	_isEmpty = 1;
}

/*!
 * destructor
 */
Protobuf::~Protobuf() {
}

/*!
 * Returns maximum seed index.  If protobuf file is empty (i.e., there are no seeds),
 * then return 0.
 */
float Protobuf::getMaxSeedIdx() {
	int sz = getNumberOfSeeds();
	if (sz == 0) {
		return 0;
	}
	float maxSeedIdx = -INF;
	for (int i = 0; i < sz; i++) {
		maxSeedIdx = std::max(maxSeedIdx, proto.protobuf_info(i).idx());
	}
	return maxSeedIdx;
}

/*!
 *
 */
void Protobuf::reorderSeedIndices(int minIdx) {
	int sz = getNumberOfSeeds();
	for (int i = 0; i < sz; i++) {
		setSeedIdx(i, (float) (minIdx + i + 1));
	}
}

float Protobuf::getSeedIdx(int protobuf_index) {
	return proto.protobuf_info(protobuf_index).idx();
}

void Protobuf::setSeedIdx(int protobuf_index, float value) {
	proto.mutable_protobuf_info(protobuf_index)->set_idx(value);
}

ProtobufDirectory * Protobuf::getProto() {
	return &proto;
}

void Protobuf::calculateVolume(Array1D<float> *volume) {
	volume->resize(getNumberOfSeeds());
	for (int i = 0; i < getNumberOfSeeds(); i++) {
		Array1D<int> x(cb), y(cb), z(cb);
		getSparseSegmentationCoordinates("full", i, &x, &y, &z);
		(*volume)(i) = (float) x.size();
	}
}

void Protobuf::appendSeedData(Protobuf *other, Array1D<int> *protobuf_idx_other) {
	for (int i = 0; i < protobuf_idx_other->size(); i++) {
		ProtobufInfo *add_entry = proto.add_protobuf_info();
		int protoIdx = (*protobuf_idx_other)(i);
		add_entry->CopyFrom(other->getProto()->protobuf_info(protoIdx));
	}
}

void Protobuf::appendXYZ(Array1D<float> *x, Array1D<float> *y,
		Array1D<float> *z) {
	for (int i = 0; i < x->size(); i++) {
		ProtobufInfo *add_entry = proto.add_protobuf_info();
		add_entry->set_seed_x((*x)(i));
		add_entry->set_seed_y((*y)(i));
		add_entry->set_seed_z((*z)(i));
	}
}

void Protobuf::deleteSeedData(Array1D<int> *protobuf_index) {
	protobuf_index->sort();
	for (int i = protobuf_index->size() - 1; i >= 0; i--) {
		int protoIdx = (*protobuf_index)(i);
		proto.GetReflection()->SwapElements(&proto,
				proto.GetDescriptor()->FindFieldByName("protobuf_info"),
				protoIdx, proto.protobuf_info_size() - 1);
		proto.GetReflection()->RemoveLast(&proto,
				proto.GetDescriptor()->FindFieldByName("protobuf_info"));
	}
}

/*!
 * Finds protobuf indices where seed indices match with given other protobuf file
 */
void Protobuf::ismember(Array1D<int> *idx, Array1D<int> *idx_other,
		Protobuf *other) {

	// get seed indices
	Array1D<float> list(cb), list_other(cb);
	getList("idx", &list);
	other->getList("idx", &list_other);

	// find which seed indices are members of other seed indices
	list.ismember(idx, idx_other, &list_other);
}

/*!
 * Reads the specified protobuf input
 * \param in An ImageAccessor object corresponding to some protobuf file
 * \return Returns 0 if read operation is successful, or 1 if the input is empty (i.e., the input does not exist)
 */
int Protobuf::readProto(TextIO *in) {

	// check if file name is '0'
	string fileName = in->getFileName();
	if (fileName.compare("0") == 0) {
		return 1;
	}

	// read text file
	string inputText = in->readText();

	// parse text as protobuf
	return parseStringToProtobuf(inputText);
}

int Protobuf::writeProto(TextIO *out) {
	// check if file name is '0'
	string fileName = out->getFileName();
	if (fileName.compare("0") == 0) {
		return 1;
	}

	// serialize protobuf to string
	string str;
	proto.SerializeToString(&str);
	out->writeText(str);
	return 0;
}

int Protobuf::readProto(const char *fileName, TextIO *container) {
	// check if file name is '0'
	if (strcmp(fileName, "0") == 0) {
		return 1;
	}

	// read text file
	string oldFileName = container->getFileName();
	container->setFileName(fileName);
	string inputText = container->readText();
	container->setFileName(oldFileName.c_str());

	// parse text as protobuf
	return parseStringToProtobuf(inputText);
}

int Protobuf::parseStringToProtobuf(string str) {
	if (str.empty()) {
		return 1;
	}
	// parse text as protobuf
	ArrayInputStream arrayinputstream(str.c_str(), (int) str.size(), -1);
	CodedInputStream codedinputstream(&arrayinputstream);
	codedinputstream.SetTotalBytesLimit(MAX_PROTOBUF_SZ, MAX_PROTOBUF_SZ);
	proto.ParseFromCodedStream(&codedinputstream);
	_isEmpty = 0;
	return 0;
}

void Protobuf::getDimensions(int &dimx, int &dimy, int &dimz) {
	dimx = (int) proto.image_dimx();
	dimy = (int) proto.image_dimy();
	dimz = (int) proto.image_dimz();
}

void Protobuf::setDimensions(int dimx, int dimy, int dimz) {
	proto.set_image_dimx((float) dimx);
	proto.set_image_dimy((float) dimy);
	proto.set_image_dimz((float) dimz);
}

int Protobuf::setSparseSegmentationCoordinates(int protobuf_idx,
		Array1D<int> *xFull, Array1D<int> *yFull, Array1D<int> *zFull,
		Array1D<int> *xPerim, Array1D<int> *yPerim, Array1D<int> *zPerim) {
	// get individual seed
	ProtobufInfo* seed = proto.mutable_protobuf_info(protobuf_idx);

	// clear previous segmentation
	seed->clear_image_fullseg_coords_x();
	seed->clear_image_fullseg_coords_y();
	seed->clear_image_fullseg_coords_z();
	seed->clear_image_perimseg_coords_x();
	seed->clear_image_perimseg_coords_y();
	seed->clear_image_perimseg_coords_z();

	// add full segmentation
	for (int i = 0; i < xFull->size(); i++) {
		seed->add_image_fullseg_coords_x((*xFull)(i));
		seed->add_image_fullseg_coords_y((*yFull)(i));
		seed->add_image_fullseg_coords_z((*zFull)(i));
	}

	// add perim segmentation
	for (int i = 0; i < xPerim->size(); i++) {
		seed->add_image_perimseg_coords_x((*xPerim)(i));
		seed->add_image_perimseg_coords_y((*yPerim)(i));
		seed->add_image_perimseg_coords_z((*zPerim)(i));
	}

	return 0;
}

int Protobuf::getSparseSegmentationCoordinates(const char *fullOrPerimeter,
		int protobuf_idx, Array1D<int> *x, Array1D<int> *y, Array1D<int> *z) {

	// option to get either full or perimeter segmentation
	string x_fOp, y_fOp, z_fOp;
	if (strcmp(fullOrPerimeter, "full") == 0) {
		x_fOp = "image_fullseg_coords_x";
		y_fOp = "image_fullseg_coords_y";
		z_fOp = "image_fullseg_coords_z";
	} else if (strcmp(fullOrPerimeter, "Perimeter") == 0) {
		x_fOp = "image_perimseg_coords_x";
		y_fOp = "image_perimseg_coords_y";
		z_fOp = "image_perimseg_coords_z";
	} else {
		log(cb, 1, "Error: Invalid choice of full, Perimeter");
		throw 999;
	}
	// get protobuf descriptors
	ProtobufInfo* info = proto.mutable_protobuf_info(protobuf_idx);
	const Descriptor* descriptor = info->GetDescriptor();
	const Reflection* reflection = info->GetReflection();
	const FieldDescriptor* fieldDescriptorX =
			descriptor->FindFieldByName(x_fOp);
	const FieldDescriptor* fieldDescriptorY =
			descriptor->FindFieldByName(y_fOp);
	const FieldDescriptor* fieldDescriptorZ =
			descriptor->FindFieldByName(z_fOp);

	// sanity check
	int numRepeatedElements_X = reflection->FieldSize(*info, fieldDescriptorX);
	int numRepeatedElements_Y = reflection->FieldSize(*info, fieldDescriptorY);
	int numRepeatedElements_Z = reflection->FieldSize(*info, fieldDescriptorZ);
	if (numRepeatedElements_X != numRepeatedElements_Y || numRepeatedElements_X
			!= numRepeatedElements_Z) {
		log(cb, 1,
				"Error: Sparse segmentation does not have same number of x,y,z coordinates");
		throw 999;
	}

	// store coordinates
	x->resize(numRepeatedElements_X);
	y->resize(numRepeatedElements_Y);
	z->resize(numRepeatedElements_Z);
	for (int i = 0; i < numRepeatedElements_X; i++) {
		(*x)(i) = reflection->GetRepeatedInt32(*info, fieldDescriptorX, i);
		(*y)(i) = reflection->GetRepeatedInt32(*info, fieldDescriptorY, i);
		(*z)(i) = reflection->GetRepeatedInt32(*info, fieldDescriptorZ, i);
	}
	return 0;
}

void Protobuf::getSeedCenter(int protobuf_idx, float &x, float &y, float &z) {
	x = proto.mutable_protobuf_info(protobuf_idx)->seed_x();
	y = proto.mutable_protobuf_info(protobuf_idx)->seed_y();
	z = proto.mutable_protobuf_info(protobuf_idx)->seed_z();
}

void Protobuf::drawIndividualSegmentation(const char *fullOrPerimeter,
		int protobuf_index, float color, Image3D<float> *I) {
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	Array1D<int32> x(cb), y(cb), z(cb);
	getSparseSegmentationCoordinates(fullOrPerimeter, protobuf_index, &x, &y,
			&z);

	for (int p = 0; p < x.size(); p++) {
		if (x(p) >= 0 && x(p) < dimx && y(p) >= 0 && y(p) < dimy && z(p) >= 0
				&& z(p) < dimz) {
			(*I)(x(p), y(p), z(p)) = color;
		}
	}
}

void Protobuf::drawSegmentationImage(const char *fullOrPerimeter,
		Image3D<float> *I, Array1D<float> *color) {
	// get image dimensions
	int dimy = (int) proto.image_dimy();
	int dimx = (int) proto.image_dimx();
	int dimz = (int) proto.image_dimz();
	I->resize(dimx, dimy, dimz);

	// Draw segmentation
	int numSeeds = getNumberOfSeeds();
	for (int i = 0; i < numSeeds; i++) { // iterate over all seeds
		drawIndividualSegmentation(fullOrPerimeter, i, (*color)(i), I);
	}
}

int Protobuf::getNumberOfSeeds() {
	return proto.protobuf_info_size();
}

void Protobuf::displayUserFields() {
	// Sanity check to see if protobuf file is empty
	int sz = proto.protobuf_info_size();
	if (sz == 0) {
		log(cb, 1, "Error: Trying to list fields in empty protobuf file");
		throw 999;
	}

	cout << "**************" << endl;
	cout << "quantifiedpropertynames" << endl;
	int user_field_sz = proto.quantifiedpropertynames_size();
	for (int i = 0; i < user_field_sz; i++) {
		string field_name_temp1 = proto.quantifiedpropertynames(i);
		std::cout << field_name_temp1 << std::endl;
	}
	cout << "**************" << endl;
	cout << "usercelldescriptions" << endl;
	int user_cell_sz = proto.usercelldescriptions_size();
	for (int i = 0; i < user_cell_sz; i++) {
		string field_name_temp1 = proto.usercelldescriptions(i);
		std::cout << field_name_temp1 << std::endl;
	}
}

int Protobuf::setList(const char *fieldName, Array1D<float> *list) {

	// Sanity check to see if protobuf file is empty or if the list does not match protobuf file size
	int sz = proto.protobuf_info_size();
	if (sz == 0) {
		log(cb, 1, "Error: Trying to set fields in empty protobuf file");
		throw 999;
	}
	if (sz != list->size()) {
		log(cb, 1, "Error: Trying to set list but sizes do not match");
		throw 999;
	}

	// check field found in hardcoded fields. If found, set values.
	const Descriptor* descriptor =
			proto.mutable_protobuf_info(0)->GetDescriptor();
	const FieldDescriptor* fieldDescriptor = descriptor->FindFieldByName(
			fieldName);
	if (fieldDescriptor != NULL) {
		for (int i = 0; i < sz; i++) {
			const Descriptor* d =
					proto.mutable_protobuf_info(i)->GetDescriptor();
			const FieldDescriptor* val_field = d->FindFieldByName(fieldName);
			const Reflection* reflection =
					proto.mutable_protobuf_info(i)->GetReflection();
			reflection->SetFloat(proto.mutable_protobuf_info(i), val_field,
					(*list)(i));
		}
		return 0;
	}

	// check if field found in user defined field.  If found, set values
	int user_field_sz = proto.quantifiedpropertynames_size();
	for (int i = 0; i < user_field_sz; i++) {
		string field_name_temp1 = proto.quantifiedpropertynames(i);
		// check if field names match
		if (field_name_temp1.compare(fieldName) == 0) {
			for (int j = 0; j < sz; j++) {
				ProtobufInfo *temp_protoInfo = proto.mutable_protobuf_info(j);
				// sanity check
				if (user_field_sz
						!= temp_protoInfo->quantifiedproperties_size()) {
					log(cb, 1,
							"Error: size of quantifiedProperties different from quantifiedPropertyNames");
					throw 999;
				}
				temp_protoInfo->set_quantifiedproperties(i, (*list)(j));
			}
			return 0;
		}
	}

	// check if field found in user cell.  If found, set values
	int user_cell_sz = proto.usercelldescriptions_size();
	for (int i = 0; i < user_cell_sz; i++) { // iterate through all user cells
		string field_name_temp1 = proto.usercelldescriptions(i);
		if (field_name_temp1.compare(fieldName) == 0) { // check if we match user cell field
			for (int j = 0; j < sz; j++) {
				ProtobufInfo *temp_protoInfo = proto.mutable_protobuf_info(j);
				// sanity check
				if (user_cell_sz != temp_protoInfo->usercellvalue_size()) {
					log(cb, 1,
							"Error: size of userCellValue different from quantifiedPropertyNames");
					throw 999;
				}
				temp_protoInfo->set_usercellvalue(i, (*list)(j));
			}
			return 0;
		}
	}

	// fieldName not in hard-coded field, quantified properties, or user cell.  Create new quantified properties field
	proto.add_quantifiedpropertynames(fieldName);
	for (int i = 0; i < sz; i++) {
		proto.mutable_protobuf_info(i)->add_quantifiedproperties((*list)(i));
	}
	return 0;
}

int Protobuf::getList(const char *fieldName, Array1D<float> *list) {

	// initialize output
	int sz = proto.protobuf_info_size();
	if (sz == 0) {
		return 1;
	}
	list->resize(sz);

	// check if we should return protobuf indices
	if (strcmp(fieldName, "protobuf_index") == 0) {
		list->resize(getNumberOfSeeds());
		for (int i = 0; i < getNumberOfSeeds(); i++) {
			(*list)(i) = (float) i;
		}
		return 0;
	}

	// check if field found in hardcoded fields
	const Descriptor* descriptor =
			proto.mutable_protobuf_info(0)->GetDescriptor();
	const FieldDescriptor* field_descriptor = descriptor->FindFieldByName(
			fieldName);
	if (field_descriptor != NULL) {
		for (int i = 0; i < sz; i++) {
			const Descriptor* descriptorTemp =
					proto.mutable_protobuf_info(i)->GetDescriptor();
			const FieldDescriptor* field_descriptorTemp =
					descriptorTemp->FindFieldByName(fieldName);
			const Reflection* reflection =
					proto.mutable_protobuf_info(i)->GetReflection();
			if (reflection->HasField(*(proto.mutable_protobuf_info(i)),
					field_descriptorTemp)) {
				(*list)(i) = reflection->GetFloat(
						*(proto.mutable_protobuf_info(i)), field_descriptor);
			} else {
				(*list)(i) = NAN;
			}
		}
		return 0;
	}

	// check if field found in user defined field
	int user_field_sz = proto.quantifiedpropertynames_size();
	for (int i = 0; i < user_field_sz; i++) { // iterate through all user fields
		string field_name_temp1 = proto.quantifiedpropertynames(i);
		if (field_name_temp1.compare(fieldName) == 0) { // check if we match user defined field
			for (int j = 0; j < sz; j++) { // iterate through every seed
				ProtobufInfo *temp_protoInfo = proto.mutable_protobuf_info(j);
				if (user_field_sz
						!= temp_protoInfo->quantifiedproperties_size()) { // sanity check
					log(cb, 1,
							"Error: size of quantifiedProperties different from quantifiedPropertyNames");
					throw 999;
				}
				const Descriptor* descriptorTemp =
						temp_protoInfo->GetDescriptor();
				const FieldDescriptor* field_descriptorTemp =
						descriptorTemp->FindFieldByName("quantifiedProperties");
				const Reflection* reflection = temp_protoInfo->GetReflection();
				(*list)(j) = reflection->GetRepeatedFloat(*temp_protoInfo,
						field_descriptorTemp, i);
			}
			return 0;
		}
	}

	// check if field found in user cell
	int user_cell_sz = proto.usercelldescriptions_size();
	for (int i = 0; i < user_cell_sz; i++) { // iterate through all user cells
		string field_name_temp1 = proto.usercelldescriptions(i);
		if (field_name_temp1.compare(fieldName) == 0) { // check if we match user cell field
			for (int j = 0; j < sz; j++) {
				ProtobufInfo *temp_protoInfo = proto.mutable_protobuf_info(j);
				if (user_cell_sz != temp_protoInfo->usercellvalue_size()) { // check if userCellValue has same size as usercelldescriptions
					log(cb, 1,
							"Error: size of userCellValue different from quantifiedPropertyNames");
					throw 999;
				}
				const Descriptor* descriptorTemp =
						temp_protoInfo->GetDescriptor();
				const FieldDescriptor* field_descriptorTemp =
						descriptorTemp->FindFieldByName("userCellValue");
				const Reflection* reflection = temp_protoInfo->GetReflection();
				(*list)(j) = reflection->GetRepeatedFloat(*temp_protoInfo,
						field_descriptorTemp, i);
			}
			return 0;
		}
	}

	// field not found in hard-coded field, quantifiedProperities, or userCellValue
	log(cb, 4, "Warning: Field not found");
	return 1;
}

void Protobuf::applySegmentationMask(int protobuf_index, Image3D<float> *I,
		Image3D<float> *output) {
	Array1D<int> x(cb), y(cb), z(cb);
	getSparseSegmentationCoordinates("full", protobuf_index, &x, &y, &z);
	int x1 = x.min(), x2 = x.max(), y1 = y.min(), y2 = y.max(), z1 = z.min(),
			z2 = z.max();
	int dimx_out = x2 - x1 + 1, dimy_out = y2 - y1 + 1, dimz_out = z2 - z1 + 1;
	output->resize(dimx_out, dimy_out, dimz_out);
	for (int p = 0; p < x.size(); p++) {
		(*output)(x(p) - x1, y(p) - y1, z(p) - z1) = (*I)(x(p), y(p), z(p));

	}

}

void Protobuf::thresholdSeedsByPosition(int dimx, int dimy, int dimz) {
	// get protobuf indices to remove
	Array1D<float> xList(cb), yList(cb), zList(cb);
	getList("seed_x", &xList);
	getList("seed_y", &yList);
	getList("seed_z", &zList);
	Array1D<int> protoIdxToRemove(cb);
	for (int i = 0; i < xList.size(); i++) {
		int x = boost::math::iround(xList(i));
		int y = boost::math::iround(yList(i));
		int z = boost::math::iround(zList(i));
		if (x < 0 || x >= dimx || y < 0 || y >= dimy || z < 0 || z >= dimz) {
			protoIdxToRemove.push_back(i);
		}
	}

	// remove seeds
	deleteSeedData(&protoIdxToRemove);
}

void Protobuf::clearSegmentation() {
	for (int i = 0; i < getNumberOfSeeds(); i++) {
		proto.mutable_protobuf_info(i)->clear_image_fullseg_coords_x();
		proto.mutable_protobuf_info(i)->clear_image_fullseg_coords_y();
		proto.mutable_protobuf_info(i)->clear_image_fullseg_coords_z();
		proto.mutable_protobuf_info(i)->clear_image_perimseg_coords_x();
		proto.mutable_protobuf_info(i)->clear_image_perimseg_coords_y();
		proto.mutable_protobuf_info(i)->clear_image_perimseg_coords_z();
	}
}
