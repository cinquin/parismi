function [pick,ov,dist] = ovfilternegs(dets,pos,thresh,zscale)
% [pick,ov,dist] = ovfilternegs(dets,pos,maxov,zscale)
%
% Filter out "negatives" which overlap too highly with a
% positive. The indicator vector "pick" has pick(i)=1 when
% max_j overlap(detection i, positive j) < maxov.
%
% Sam Hallman, copyright 2015

assert(~isempty(dets));
assert(~isempty(dets.x));

% For each detection, compute overlap with each positive
Xseed = seed_spheres(pos,zscale);
Xdet = det_spheres(dets,zscale);
O = sphere_overlap(Xdet,Xseed);

% Return those with low overlap with any positive
[maxov,match] = max(O,[],2);
pick = maxov < thresh;
ov = maxov(pick);

% compute distances just for fun
Xdet = Xdet(pick,1:3);
Xseed = Xseed(match(pick),1:3);
dist = sqrt(sum((Xdet-Xseed).^2,2));   % (units=pixels)
