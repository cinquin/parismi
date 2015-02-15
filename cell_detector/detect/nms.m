function res = nms(dets,alpha,zscale)
% res = nms(dets,alpha,zscale)
%
% Performs greedy 3D NMS using the spherical overlap criterion.
% Two spheres i,j overlap if dist(i,j) < radius(i) + radius(j).
% Of course, the true cell radii are unknown, so we estimate
% them to be a scalar multiple ALPHA of the size of the given
% detection window. Thus alpha is in the range (0,1].
%
% So, LARGE alpha ==> FEWER detections, and FASTER runtime.
%
% Sam Hallman, copyright 2015

x = dets.x + (dets.w-1)/2;
y = dets.y + (dets.h-1)/2;
z = zscale*dets.zc;
dim = alpha*dets.w/2;

X = double([x y z dim dets.r]);
Y = nms_mex(X);

res.h  = Y(:,4)/alpha;
res.w  = Y(:,4)/alpha;
res.x  = Y(:,1) - (res.w-1)/2;
res.y  = Y(:,2) - (res.h-1)/2;
res.zc = Y(:,3)/zscale;
res.r  = Y(:,end);
