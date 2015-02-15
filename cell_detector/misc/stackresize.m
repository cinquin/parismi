function stack = stackresize(stack,scale)
% out = stackresize(stack,scale)
%
% Resize a 3D stack
% size(out) is roughly scale*size(stack)
%
% For example, if stack is 100x200x80 and scale=0.5,
% then out will be 50x100x40.
%
% Sam Hallman, copyright 2015

if scale == 1, return, end

cls = class(stack);
if isequal(cls,'uint8') || isequal(cls,'uint16')
  stack = single(stack);
end

F = griddedInterpolant(stack);
ii = 1:(1/scale):size(stack,1);
jj = 1:(1/scale):size(stack,2);
kk = 1:(1/scale):size(stack,3);
stack = F({ii,jj,kk});

if isequal(cls,'uint8')
  stack = uint8(stack);
elseif isequal(cls,'uint16')
  stack = uint16(stack);
end
