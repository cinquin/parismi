#include "ImageIO.h"
#include "CallbackFunctions.h"
#include "Image3D.h"
#include "Image3D_util.h"
#include "definitions.h"
#include <boost/math/special_functions/round.hpp>
#include "util.h"

void imerode(ImageIO* inputImage, ImageIO* outputImage, CallbackFunctions *cb);
