function fsym = symFeat(f,flipI)
% fsym = symFeat(f,flipI)
%
% Sam Hallman, copyright 2015

% Make f symmetric
fsym = zeros(size(f));
for i = 1:4
  fsym = fsym + (1/4)*f(flipI{i},:);
end

%
% Fold into unique parameters, weight each feature by the number of 
% times a feature appears in the symmetrized version so that
%   unsym(w)*f  = w*sym(f)
%
I =   find((flipI{1} < flipI{2})&(flipI{1} < flipI{3})&(flipI{1}<flipI{4}));
Ich = find((flipI{1} == flipI{2})&(flipI{1} < flipI{3}));
Icv = find((flipI{1} < flipI{2})&(flipI{1} == flipI{3}));
Icc = find((flipI{1} == flipI{2})&(flipI{1} == flipI{3}));
fsym = [4*fsym(I,:); 2*fsym(Ich,:); 2*fsym(Icv,:); fsym(Icc,:)];

