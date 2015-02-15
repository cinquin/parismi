function [resp,featIm,meanIm] = get_xy_resp(im, model, write)
% [resp,featIm,meanIm] = get_xy_resp(im, model, write)
%
% Sam Hallman, copyright 2015

  [respHOG,featIm] = get_xy_hog_resp(im, model);
  [respMB, meanIm] = get_xy_mb_resp(im, model);
  resp = respHOG + respMB;

  if ~write
    featIm = [];
    meanIm = [];
  end


function [resp,featIm] = get_xy_hog_resp(im, model)

  w = model.xy.w;
  [ny nx nf] = size(w);
  for i = 1:nf
    w(:,:,i) = rot90(w(:,:,i),2);
  end

  featIm = features(im, model.xy.sbin);
  if model.xypad
    featIm = featpad(featIm, ny-1, nx-1);
  end

  % Score each location
  fsiz = size(featIm(:,:,1));
  resp = zeros(fsiz - [ny nx] + 1);
  for i = 1:nf,
    resp = resp + conv2(featIm(:,:,i),w(:,:,i),'valid');
  end


function [resp,meanIm] = get_xy_mb_resp(im, model)

  [ny nx nf] = size(model.xy.w);

  % Find which pixels of im get used by the HOG code
  sbin = model.xy.sbin;
  fsize = featsize(size(im), sbin);
  fsize = fsize(1:2);
  pixels = (1+fsize+1).*sbin;

  % Use that to either grow or shrink the image as necessary, in order
  % to ensure that the MB response image is the same size as the HOG one
  im = subarray(im, 1,pixels(1), 1,pixels(2), 0);

  % Also compute the size of the HOG response image so that later
  % on we can assert that the MB response image is that size
  if model.xypad
    hogpad = [ny nx]-1;          % the argument passed to padarray
    fsize = fsize + 2*hogpad;
  end
  rsize = fsize - [ny nx] + 1;   % size of the HOG response image

  % Compute the mean brightness in each detection window
  if model.xypad
    im = padarray(im, sbin.*hogpad, 'symmetric');
  end
  dim = model.xy.dim;
  h1 = ones(1,dim(1),'single')/dim(1);
  h2 = ones(1,dim(2),'single')/dim(2);
  meanIm = conv2(h1, h2, im, 'valid');

  % Subsample like the HOG code does so that the MB and HOG reponses match
  meanIm = meanIm(1:sbin(1):end, 1:sbin(2):end);
  if ~isequal(rsize,size(meanIm))
    fprintf('ERROR: size(meanIm) is incorrect\n');
    keyboard;
  end

  % Get the bin number for each element of meanIm
  [~,binIm] = histc(meanIm, edges());
  assert(isequal(size(binIm),size(meanIm)));

  % Construct response image resp(i,j) = wm(binIm(i,j))
  resp = zeros(size(binIm),'single');
  resp(:) = model.xy.wm(binIm);
