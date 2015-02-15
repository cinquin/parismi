function w = unsymWeight(f,flipI)
% w = unsymWeight(f,flipI)
% Applies the inverse of symFeat.m

len = length(flipI{1});
w = zeros(len,size(f,2));

% unpack indices used in symFeat
I =   find((flipI{1} < flipI{2})&(flipI{1} < flipI{3})&(flipI{1}<flipI{4}));
Ich = find((flipI{1} == flipI{2})&(flipI{1} < flipI{3}));
Icv = find((flipI{1} < flipI{2})&(flipI{1} == flipI{3}));
Icc = find((flipI{1} == flipI{2})&(flipI{1} == flipI{3}));

% fill in the 4 quadrants of w with entries of f
for i = 1:4
  w(flipI{i}([I Ich Icv Icc]),:) = f;
end

