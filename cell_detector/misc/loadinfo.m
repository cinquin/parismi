function info = loadinfo(stackNum)
% Load ground truth seeds and metadata for a given stack
%
% This file is the interface through which all training
% code accesses seeds and metadata.
%
%     THE USER SHOULD MODIFY THIS FUNCTION AS
%     NECESSARY TO SUIT HIS OR HER PROJECT
%
% USAGE
%  info = loadinfo(stackNum)
%
% INPUT
%  stackNum - the index of a stack
%
% OUTPUT
%  info     - a structure with the following fields
%   .seeds    - a numSeeds-by-3 matrix of seeds [x y z]
%   .xyCal    - num microns per pixel in x,y
%   .zCal     - num microns per pixel in z
%   .ppm      - num pixels per micron in x,y (i.e. 1/xyCal)
%   .zscale   - relative scaling in z (i.e. zCal/xyCal)
%   .width    - the width of the stack
%   .height   - the height of the stack
%   .depth    - the depth (i.e. #slices) of the stack
%
% See also: loadstack

% This part should be modified to suit your project
%
% In our implementation, we assume that the seeds and metadata
% are stored in a .mat file. This function then simply loads
% the data from that file
base = fileparts(fileparts(mfilename('fullpath')));
gtpath = [base '/data/groundTruth/%d.mat'];
gtpath = sprintf(gtpath,stackNum);
info = load(gtpath,'seeds','xyCal','zCal');

tiffpath = [base '/data/stacks/%d.tif'];
tmp = imfinfo(sprintf(tiffpath,stackNum));
info.width = tmp(1).Width;
info.height = tmp(1).Height;
info.depth = length(tmp);

% add some useful fields
info.ppm = 1/info.xyCal;
info.zscale = info.zCal/info.xyCal;
