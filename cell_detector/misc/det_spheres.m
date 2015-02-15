function spheres = det_spheres(dets,zscale)
% Sam Hallman, copyright 2015

% sphere centroids
x = dets.x + (dets.w-1)/2;
y = dets.y + (dets.h-1)/2;
z = dets.zc;
z = zscale*z;                      % !! notice zscale !!

% sphere radii
rad = dets.w/2;
assert(isequal(rad,dets.h/2));

spheres = [x(:) y(:) z(:) rad(:)];
