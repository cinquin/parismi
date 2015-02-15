function stack = loadstack(stackNum)
% Load a TIFF stack
%
% This file is the interface through which all training
% code accesses TIFF stacks.
%
%     THE USER SHOULD MODIFY THIS FUNCTION AS
%     NECESSARY TO SUIT HIS OR HER PROJECT
%
% USAGE
%  stack = loadstack(stackNum)
%
% INPUT
%  stackNum - the index of a stack
%
% OUTPUT
%  stack    - a 3D uint8 array holding the pixel data
%             of the given stack. e.g. the 10th XY slice
%             of the stack is stack(:,:,10)
%
% See also: loadinfo

% Set up path to the TIFF stack
% This part should be modified to suit your project
base = fileparts(fileparts(mfilename('fullpath')));
tiffpath = [base '/data/stacks/%d.tif'];
tiffpath = sprintf(tiffpath,stackNum);

% -----------------------------------------------------
% no need to modify any code below this line

stack = tiffread(tiffpath);
stack = cat(3,stack.data);

% set range to [0,255] and cast to uint8
stack = single(stack);
smin = min(min(min(stack)));
smax = max(max(max(stack)));
stack = (stack-smin)/(smax-smin);   % normalize to [0,1]
stack = uint8(255*stack);           % scale to [0,255]
