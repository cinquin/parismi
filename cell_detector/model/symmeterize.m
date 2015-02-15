function f = symmeterize(f,model)
% f = symmeterize(f,model)
% Convert full feature vector f to symmetric form.
% The vector f should be of length model.unsymlen.
%
% Sam Hallman, copyright 2015

[fxy fmb fxz fb] = modelvec2blocks(f,model);

fxy = symFeat(fxy, model.xy.flipI);
fxz = symFeat(fxz, model.xz.flipI);

f = [fxy; fmb; fxz; fb];
