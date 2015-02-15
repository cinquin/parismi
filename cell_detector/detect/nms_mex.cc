#include "mex.h"
#include <vector>
#include <algorithm>
#include "kdtree.h"
using std::vector;

class detection {
public:
  detection(double xa, double ya, double za, double ra, double ca)
    : x(xa), y(ya), z(za), r(ra), conf(ca) {}
  bool operator<(const detection& rhs) const {
    return conf > rhs.conf; // so we sort in descending order
  }
  double x, y, z, r, conf;
};

void mexFunction(int nlhs, mxArray* plhs[], int nrhs, const mxArray* prhs[]) {
  if (nrhs != 1)
    mexErrMsgTxt("Wrong number of inputs");
  if (mxGetClassID(prhs[0]) != mxDOUBLE_CLASS)
    mexErrMsgTxt("Input should be double");

  mwSize  nrows = mxGetM(prhs[0]);
  double* x     = mxGetPr(prhs[0]);
  double* y     = x + nrows;
  double* z     = y + nrows;
  double* rad   = z + nrows;
  double* conf  = rad + nrows;

  vector<detection> dets;   
  for (mwSize i = 0; i < nrows; ++i)
    dets.push_back(detection(x[i], y[i], z[i], rad[i], conf[i]));

  std::sort(dets.begin(), dets.end());

  vector<detection> pick;
  kdtree* tree = kd_create(3);
  // insert the first element
  kd_insert3(tree, dets[0].x, dets[0].y, dets[0].z, &(dets[0].r));
  pick.push_back(dets[0]);
  
  for (mwSize best = 1; best < nrows; ++best) {
    double x = dets[best].x;
    double y = dets[best].y;
    double z = dets[best].z;

    // find nearest neighbor
    double xnear, ynear, znear;
    kdres* res = kd_nearest3(tree, x, y, z);
    if (res == 0)
      mexErrMsgTxt("kd-tree lookup failed");
    kd_res_item3(res, &xnear, &ynear, &znear);
    double rnear = *((double*)kd_res_item_data(res));

    // add if distance is above sum of radii
    double rsum = dets[best].r + rnear;
    double dx = x-xnear, dy = y-ynear, dz = z-znear;
    if (dx*dx + dy*dy + dz*dz > rsum*rsum) {
      int status = kd_insert3(tree, x, y, z, &(dets[best].r));
      if (status != 0)
        mexErrMsgTxt("kd-tree insertion failed");
      pick.push_back(dets[best]);
    }

    kd_res_free(res); // free memory used for nearest neighbor
  }

  kd_free(tree); // free memory used for the tree

  int npick = pick.size();
  plhs[0] = mxCreateNumericMatrix(npick, 5, mxDOUBLE_CLASS, mxREAL);
  double* p = mxGetPr(plhs[0]);
  
  for (vector<detection>::size_type i = 0; i < npick; ++i) {
    *p = pick[i].x;
    *(p+npick) = pick[i].y;
    *(p+2*npick) = pick[i].z;
    *(p+3*npick) = pick[i].r;
    *(p+4*npick) = pick[i].conf;
    ++p;
  }
}
