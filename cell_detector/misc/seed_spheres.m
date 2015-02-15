function spheres = seed_spheres(pos,zscale)
% Sam Hallman, copyright 2015

% sphere centroids
x = [pos(:).x];
y = [pos(:).y];
z = zscale*[pos(:).z];                  % !! notice zscale !!

% sphere radii
x1 = [pos.x1];
y1 = [pos.y1];
x2 = [pos.x2];
y2 = [pos.y2];
rad = (x2-x1+1)/2;
assert(all(abs(rad-(y2-y1+1)/2)<1e-8));

spheres = [x(:) y(:) z(:) rad(:)];
