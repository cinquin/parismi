function model = model_train(varargin)
% Train a detector.
%
% USAGE
%   model = model_train(opts)
%
% INPUTS
%  opts  - parameters (struct or name/value pairs)
%   .trainset    - indices of the training stacks
%   .dim         - [48] size (in pixels) of the detection window in XY
%   .sbin        - [4] the bin size (in pixels) for the HOG features in XY
%   .C           - [0.001] the SVM regularization parameter
%   .cTol        - [0.02] tolerance for trigger of cache update in neghard
%   .maxIter     - [12] the max # of iterations of hard negative mining
%   .posov       - [0.7] latent positive sphere overlap threshold
%   .negov       - [0.3] negative mining sphere overlap threshold
%   .cache_bytes - [2^30] size of training cache in bytes
%   .minsc       - [0.7] min scale to search over during detection
%   .maxsc       - [1.5] max scale to search over during detection
%   .numsc       - [14] num scales to search over during detection
%   .xypad       - [0] if 1 then learn a detector which can search for
%                  truncated objects
%
% OUTPUTS
%  model  - structure containing the trained detector
%
% EXAMPLES
%   model = model_train('trainset',[17 23 44]);
%   model = model_train('dim',72,'sbin',6,'trainset',[1 2 5 10 37]);
%
% Sam Hallman, copyright 2015

% get default parameters
dfs = {'dim',48, 'sbin',4, 'C',0.001, 'maxIter',12, ...
       'posov',0.7, 'negov',0.3, 'cache_bytes',2^30, ...
       'minsc',0.7, 'maxsc',1.5, 'numsc',14, 'xypad',0, ...
       'cTol',0.02, 'trainset','REQ'};
opts = getPrmDflt(varargin,dfs,1);

tag = strrep(sprintf('%g_%d_%g', ...
  opts.negov, opts.maxIter, opts.posov), '.', '');
% initialize model struct
model = initmodel(datestr(now), opts.dim, opts.sbin, opts.xypad);
model.minsc = opts.minsc;
model.maxsc = opts.maxsc;
model.numsc = opts.numsc;
model.parms.C = opts.C;
model.parms.posov = opts.posov;
model.parms.negov = opts.negov;
model.parms.trainset = opts.trainset;

% learn model
windowDim = [model.xy.dim(1:2) model.xz.dim(1)];
[pos,ims] = trndata(windowDim, opts.trainset);
model = train(model, pos, ims, opts.maxIter, opts.negov, ...
              opts.posov, opts.C, opts.cache_bytes, opts.cTol, tag);
