cd detect/kdtree-0.5.6
unix('./configure');
unix('make');
mex -outdir .. -I. -L. -lkdtree ../nms_mex.cc
cd ../..
mex -outdir train train/qp.cc
mex -outdir train train/addcols.cc
mex -outdir features features/features_mex.cc
