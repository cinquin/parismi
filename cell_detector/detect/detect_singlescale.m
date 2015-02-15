function [dets,feat] = detect_singlescale(...
    stack, model, scale, zrange, maxn, donms, write, sym, pos, overlap, zscale)
  % 1      2      3      4       5     6      7      8    9    10       11
  %
  % Sam Hallman, copyright 2015
  
  if nargin > 8 && ~isempty(pos)
    latent = true;
    model.thresh = -inf;
  else
    latent = false;
  end
  if nargin < 4, zrange = [1 size(stack,3)]; end
  if nargin < 5, maxn   = inf;               end
  if nargin < 6, donms  = false;             end
  if nargin < 7, write  = nargout == 2;      end
  if nargin < 8, sym    = true;              end

  % Compute the responses in XY and in XZ
  [xyresp xyFeatIms meanIms] = xy_responses(stack, model, zrange, write);
  rows = response_rows(xyresp, model);
  [xzresp xzFeatIms shiftMask rowMask padz] = ...
    xz_responses(stack, model, rows, zrange, write);

  % Overall response image
  beta = model.b * model.sb;
  resp = xyresp + xzresp + beta;
  clear xyresp xzresp

  % Avoid response points not greater than their 4 neighbors
  if donms
    maxima = localmax(resp);
    resp(~maxima) = -inf;
  end

  % In latent mode, keep at most one detection per positive
  if latent
    I = find(resp > -inf);
    dets = ind2dets(model, resp, I, scale, zrange, inf);
    match = posmatch(pos, dets, overlap, zscale);
    I(match) = [];
    resp(I) = -inf;
  end

  % Compute bbox coordinates and features for all above-thresh responses
  I = find(resp > model.thresh);
  dets = [];
  feat = [];
  if ~isempty(I)
    dets = ind2dets(model, resp, I, scale, zrange, maxn);
    if write
      [y x z] = ind2sub(size(resp),I);
      feat = extract(model, x, y, z, ...
        xyFeatIms, meanIms, xzFeatIms, shiftMask, rowMask, maxn, sym);
      % check for bugs
      if sym, w = model.w_sym;
      else,   w = weights(model); end
      assert(all(abs(w'*feat - dets.r') < 1e-5));
    end
  end


function ymid = response_rows(xyresp,model)

  dim = model.xy.dim(1);
  sbin = model.xy.sbin(1);
  h = size(model.xy.w,1);
  pady = model.xypad*(h-1);

  y = 1:size(xyresp,1);
  y1 = (y-pady-1)*sbin + 1;
  y2 = y1 + dim - 1;
  ymid = floor((y1+y2)/2);  % pixel coordinates


function [resp, featIms, meanIms] = xy_responses(stack,model,zrange,write)

  [resp(:,:,1), featIms{1}, meanIms{1}] = ...
    get_xy_resp(stack(:,:,zrange(1)), model, write);
  nslices = zrange(2)-zrange(1)+1;
  resp(:,:,2:nslices) = 0;
  parfor z = 2:nslices
    [resp(:,:,z), featIms{z}, meanIms{z}] = ...
      get_xy_resp(stack(:,:,zrange(1)+z-1), model, write);
  end


function [resp, featIms, shiftMask, rowMask, pady] = ...
    xz_responses(stack, model, y, zrange, write)

  [imy, ~, imz] = size(stack);

  valid = (y >= 1)&(y <= imy);
  first = find(valid,1,'first');
  last  = find(valid,1,'last');
  if ~model.xypad
    assert(all(valid));
  end

  % get range info necessary to limit XZ responses to a given Z-interval
  [imrows, shiftMask, rowMask, pady, extra] = slab_prepare(...
    imz, size(model.xz.w,1), model.xz.sbin(1), zrange(1), zrange(2));

  % get xz responses at valid rows
  im = permute(stack(y(first),:,:),[3 2 1]);
  [resp(:,:,first), featIms{first}] = get_xz_resp(...
    im, model, write, imrows, shiftMask, rowMask, pady, extra);
  resp(:,:,first+1:length(y)) = 0;
  parfor i = first+1:last
    im = permute(stack(y(i),:,:),[3 2 1]);
    [resp(:,:,i), featIms{i}] = get_xz_resp(...
      im, model, write, imrows, shiftMask, rowMask, pady, extra);
  end

  % get xz responses at invalid rows
  f = zeros(size(model.xz.w));
  f(:,:,end) = 1;
  r = model.xz.w(:)'*f(:);
  resp(:,:,~valid) = r;
  % and features
  for i = find(~valid)
    for j = 1:length(featIms{first})
      if ~isempty(featIms{first}{j})
        f = zeros(size(featIms{first}{j}));
        f(:,:,end) = 1;
        featIms{i}{j} = f;
      end
    end
  end

  % after permuting, xzresp can be added to xyresp
  resp = permute(resp,[3 2 1]);


function dets = ind2dets(model, resp, I, scale, zrange, maxn)

  I = I(:);
  [y x z] = ind2sub(size(resp),I);
  [ny nx] = size(model.xy.w(:,:,1));
  [my mx] = size(model.xz.w(:,:,1)); assert(nx == mx);
  padx = (model.xypad==true)*(nx-1);
  pady = (model.xypad==true)*(ny-1);

  ysc = model.xy.sbin(1)*scale;
  xsc = model.xy.sbin(2)*scale;
  zimg = zrange(1) + z - 1;

  dets.y  = (y-pady-1)*ysc + 1;
  dets.x  = (x-padx-1)*xsc + 1;
  dets.h  = repmat((1+ny+1)*ysc, size(dets.x));
  dets.w  = repmat((1+nx+1)*xsc, size(dets.x));
  dets.zc = (zimg-1)*scale + 1;
  dets.r  = resp(I);
  dets.id = uint16([repmat(scale,size(dets.r)) x y]);

  dets = sub(dets,1:min(maxn,length(I)));


function feat = extract(model, x, y, z, ...
    xyFeatIms, meanIms, xzFeatIms, shiftMask, rowMask, maxn, sym)

  [ny nx ~] = size(model.xy.w);
  [my mx ~] = size(model.xz.w);

  maxn = min(maxn,length(x));
  feat = zeros(model.unsymlen, maxn, 'single');
  for i = 1:maxn,
    % XY features (HOG + MB)
    xyHOG = xyFeatIms{z(i)}(y(i):y(i)+ny-1, x(i):x(i)+nx-1, :);
    xyMB = histc(meanIms{z(i)}(y(i),x(i)), edges());
    xyMB = xyMB(1:end-1);
    % XZ features (HOG)
    shiftInd = shiftMask(z(i))+1;
    featIm = xzFeatIms{y(i)}{shiftInd};
    yresp = rowMask(z(i),shiftInd);
    xzHOG = featIm(yresp:yresp+my-1, x(i):x(i)+mx-1, :);
    % full feature vector
    feat(:,i) = [xyHOG(:); 0; xyMB(:); xzHOG(:); 0; model.sb];
  end
  if sym
    feat = symmeterize(feat,model);
  end
