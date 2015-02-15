function [len,fI] = flipModel(model)
% [len,flipI] = flipModel(model)
% Computes indexes that will flip the weights of a model
% len is the length of the symmetric model 
% (typically 1/4 the length of the original model)
%
% Sam Hallman, copyright 2015

siz = size(model.w);
n   = prod(siz);
I   = reshape(1:n,siz);

fI{1} = I;
fI{2} = flipHOG_LR(I);
fI{3} = flipHOG_UD(I);
fI{4} = flipHOG_UD(flipHOG_LR(I));
for i = 1:4
  fI{i} = [fI{i}(:)' model.len];
end

uniq = (fI{1} <= fI{2})&(fI{1} <= fI{3})&(fI{1} <= fI{4});
%len = sum(uniq)+1;
len = sum(uniq);    % no need for +1 since bias already in each fI{i}


function f = flipHOG_LR(f)

% flip permutation
p = [10  9  8  7  6  5  4  3  2 ... % 1st set of contrast sensitive features
      1 18 17 16 15 14 13 12 11 ... % 2nd set of contrast sensitive features
     21 22 19 20 ...                % Gradient/texture energy features
     23];                           % Boundary truncation feature
f = f(:,end:-1:1,p);


function f = flipHOG_UD(f)

% flip permutation
p = [10  9  8  7  6  5  4  3  2 ... % 1st set of contrast sensitive features
      1 18 17 16 15 14 13 12 11 ... % 2nd set of contrast sensitive features
     20 19 22 21 ...                % Gradient/texture energy features
     23];                           % Boundary truncation feature
f = f(end:-1:1,:,p);
