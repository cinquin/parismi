function model = initmodel(name, dim, sbin, xypad)
  % model = initmodel(name, dim, sbin, xypad)
  % model.xy is the XY component
  % model.xz is the XZ component
  % In XY, template size is [dim   dim], bin size is [sbin   sbin].
  % In XZ, template size is [dim/2 dim], bin size is [sbin/2 sbin].
  % xypad should be true if we will be searching for truncated objects.
  %
  % Sam Hallman, copyright 2015

  assert(rem(dim,2) == 0);
  assert(rem(sbin,2) == 0);

  model.name = name;
  model.b  = 0;
  model.sb = 10;
  model.xypad = xypad;
  model.thresh = 0;

  model.xy = initmodel_xy(dim, sbin);
  model.xz = initmodel_xz(dim, dim/2, sbin, sbin/2);

  model.symlen   = model.xy.symlen   + model.xz.symlen   + 1;    % +1 for true bias
  model.unsymlen = model.xy.unsymlen + model.xz.unsymlen + 1;    % +1 for true bias

  model.w_sym = zeros(model.symlen,1);


function model = initmodel_xy(dim,sbin)

  tmp.w = features(zeros(dim), sbin);
  tmp.len = numel(tmp.w) + 1;
  [symlen,flipI] = flipModel(tmp);

  mblen = length(edges())-1;

  model.dim      = [dim dim];
  model.sbin     = [sbin sbin];
  model.w        = tmp.w;                % HOG 
  model.wm       = zeros(mblen,1);       % MB
  model.symlen   = symlen + mblen;       % len of symmeterized feats
  model.unsymlen = tmp.len + mblen;      % len of raw/unsymmeterized feats
  model.flipI    = flipI;


function model = initmodel_xz(dim_xy, dim_z, sbin_xy, sbin_z)

  dim  = [dim_z , dim_xy ];
  sbin = [sbin_z, sbin_xy];

  tmp.w = features(zeros(dim), sbin);
  tmp.len = numel(tmp.w) + 1;
  [symlen,flipI] = flipModel(tmp);

  model.dim      = dim;
  model.sbin     = sbin;
  model.w        = tmp.w;
  model.symlen   = symlen;
  model.unsymlen = tmp.len;
  model.flipI    = flipI;
