function model = vec2model(w0,model)
% model = vec2model(vec,model)
%
% Sam Hallman, copyright 2015

[xyhog,xymb,xzhog,bias] = modelvec2blocks(w0,model);
unsym_xyhog = unsymWeight(xyhog, model.xy.flipI);
unsym_xzhog = unsymWeight(xzhog, model.xz.flipI);

model.xy.w  = reshape(unsym_xyhog(1:end-1), size(model.xy.w));
model.xy.wm = xymb;
model.xz.w  = reshape(unsym_xzhog(1:end-1), size(model.xz.w));
model.b     = bias;

model.w_sym = w0;
