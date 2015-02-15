function scales = getscales(minsc,maxsc,numsc)
% scales = getscales(minsc,maxsc,numsc)
% If minsc<1<maxsc, then we ensure that scale=1 appears in the
% output vector, i.e., one of the scales will exactly equal 1.
%
% Sam Hallman, copyright 2015

if minsc == maxsc
  scales = minsc;
else
  % when 1 is in the scale range, we want one of
  % the scales to be exactly 1
  scales = linspace(minsc,maxsc,numsc);
  if minsc < 1 && 1 < maxsc
    [~,closest] = min(abs(scales-1));
    scales(closest) = 1;
  end
end
