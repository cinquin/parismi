function [xy mb xz b] = modelvec2blocks(x,model)
% [xyhog xymb xzhog bias] = modelvec2blocks(x,model)
% Decompose input features into its blocks
%
% Sam Hallman, copyright 2015

% Get lengths of each block
switch size(x,1)
  case model.symlen, name = 'symlen';
  case model.unsymlen, name = 'unsymlen';
  otherwise, error('unrecognized format');
end
nmb = numel(model.xy.wm);
nxy = model.xy.(name) - nmb;
nxz = model.xz.(name);

% Extract each block
lens = [nxy nmb nxz 1];
last = cumsum(lens);
first = last - lens + 1;
xy = x(first(1):last(1),:);
mb = x(first(2):last(2),:);
xz = x(first(3):last(3),:);
b  = x(first(4):last(4),:);  assert(size(b,1)==1);
