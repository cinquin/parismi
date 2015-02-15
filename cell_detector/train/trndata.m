function [pos,ims] = trndata(dim,stacks)
% Load training data
% [pos,ims] = trndata(dim,stacks)
%
% dim       template size in pixels, [dimy dimx imz]
% stacks    indices of the training stacks
%
% Sam Hallman, copyright 2015

[oy1,oy2] = offsets(dim(1));
[ox1,ox2] = offsets(dim(2));
[oz1,oz2] = offsets(dim(3));

numims = 0;
numpos = 0;
for ind = stacks
  info = loadinfo(ind);
  % Add an entry for every positive
  for X = info.seeds'
    numpos = numpos + 1;
    pos(numpos).x1 = X(1) - ox1;
    pos(numpos).y1 = X(2) - oy1;
    pos(numpos).z1 = X(3) - oz1;
    pos(numpos).x2 = X(1) + ox2;
    pos(numpos).y2 = X(2) + oy2;
    pos(numpos).z2 = X(3) + oz2;
    pos(numpos).x  = X(1);
    pos(numpos).y  = X(2);
    pos(numpos).z  = X(3);
    pos(numpos).stackind = ind;
  end
  % Add an entry for every slice
  for z = 1:info.depth
    numims = numims + 1;
    ims(numims).z = z;
    ims(numims).stackind = ind;
  end
end


function [o1,o2] = offsets(dim)

if rem(dim,2) == 0
  o1 = dim/2-1;
  o2 = dim/2;
else
  o1 = (dim-1)/2;
  o2 = (dim-1)/2;
end
