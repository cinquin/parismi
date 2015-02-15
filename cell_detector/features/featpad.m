function featIm = featpad(featIm,pady,padx,direction)
% featIm = featpad(featIm,pady,padx)
% Pad the features so we can detect truncated objects.
%
% Sam Hallman, copyright 2015

if nargin < 4
  direction = 'both';
end

featIm = padarray(featIm,[pady padx],0,direction);

% Write truncation feature
if isequal(direction,'pre') || isequal(direction,'both')
  featIm(1:pady, :, end) = 1;
  featIm(:, 1:padx, end) = 1;
end
if isequal(direction,'post') || isequal(direction,'both')
  featIm(end-pady+1:end, :, end) = 1;
  featIm(:, end-padx+1:end, end) = 1;
end
