function [dets,feat,matched] = detect_multiscale_chunk(...
  stack, model, nchunks, maxn, donms, sym, pos, overlap, zscale)
% [dets,feat] = detect_multiscale_chunk(...
%   stack, model, nchunks, maxn, donms, sym, pos, overlap, zscale)
%   1      2      3        4     5      6    7    8        9
% Args 1,2,3 are required
% Args 4,5,6 default to inf, false, true, respectively
% Args 7,8,9 are used in latent mode only
%
% Sam Hallman, copyright 2015

write = nargout > 1;
if nargin < 3, maxn  = inf;   end
if nargin < 4, donms = false; end
if nargin < 5, sym   = true;  end
if nargin < 6 || isempty(pos)
  latent = false;
  [pos overlap zscale] = deal([]);
else
  latent = true;
end

scales = getscales(model.minsc, model.maxsc, model.numsc);
ndigit = ceil(log(length(scales))/log(10));
dets = {}; % use cells so that parfor works
feat = {};
verbose = true;
for s = 1:length(scales)
  % Skip this scale if cannot achieve sufficient overlap
  if latent && max_overlap(model,pos,zscale,scales(s)) < overlap
    if verbose, fprintf('skipping scale %d\n',s); end
    continue;
  end
  % resize and detect
  scaled = stackresize(stack,1/scales(s));
  [first,last] = rangesplit(1:size(scaled,3),nchunks);
  for c = 1:nchunks
    zrange = [first(c) last(c)];
    [cdets{c},cfeat{c}] = detect_singlescale(scaled, model, ...
      scales(s), zrange, maxn, donms, write, sym, pos, overlap, zscale);
  end
  [dets{s},feat{s}] = merge(cdets,cfeat);
  % print some info about this iter
  if verbose
    if isempty(dets{s}), numdets = 0;
    else numdets = length(dets{s}.x); end
    fprintf('scale %*d/%d (%.4f) (nd=%d)\n', ...
      ndigit, s, length(scales), scales(s), numdets);
  end
end
[dets,feat] = merge(dets,feat);

matched = [];
if latent
  [match,matched] = posmatch(pos,dets,overlap,zscale);
  dets = sub(dets,match);
  if write
    feat = feat(:,match);
  end
end


function [dets,feat] = merge(detcell,featcell)

dets = [];
for s = 1:length(detcell)
  dets = add(dets,detcell{s});
end
feat = cat(2,featcell{:});


function ov = max_overlap(model,pos,zscale,scale)
% Compute the maximum possible overlap at this scale with any positive

ov = 0;
Xpos = seed_spheres(pos,zscale);
det_size = scale*model.xy.dim(1); % size of detections at this scale
for i = 1:length(pos)
  X = Xpos(i,:); X(4) = det_size/2;
  ov = max(ov, sphere_overlap(Xpos(i,:),X));
end
