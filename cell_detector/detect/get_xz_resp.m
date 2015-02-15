function [resp,featIms] = get_xz_resp(...
  im, model, write, imrows, shiftMask, rowMask, pady, extra)
% Sam Hallman, copyright 2015

[imy imx] = size(im);
[ny nx nf] = size(model.xz.w);
padx = (model.xypad==true)*(nx-1);

featIms = [];
shifts = unique(shiftMask);
for shift = shifts(:)'
  i1 = imrows(1,shift+1) + shift;
  i2 = imrows(2,shift+1) + shift;
  shifted = subarray(im,i1,i2,1,imx,0);
  featIm = features(shifted,model.xz.sbin);
  extraTop = extra(1,shift+1);
  extraBot = extra(2,shift+1);
  featIm = featIm(1+extraTop:end-extraBot,:,:);
  featIm = featpad(featIm,pady(1,shift+1),padx,'pre');
  featIm = featpad(featIm,pady(2,shift+1),padx,'post');
  resps{shift+1} = sparse_resp(featIm,model);
  width = size(resps{shift+1},2);
  if write
    featIms{shift+1} = featIm;
  end
end

height = length(shiftMask);
resp = nan(height,width,'single');
for shift = shifts(:)'
  rows = shiftMask == shift;
  respRows = rowMask(rows,shift+1);
  resp(rows,:) = resps{shift+1}(respRows,:);
end
assert(all(~isnan(resp(:))));


function resp = sparse_resp(featIm, model)

% Ready the xz weights, not xy
w = model.xz.w;
[ny nx nf] = size(w);
for i = 1:nf
  w(:,:,i) = rot90(w(:,:,i),2);
end

% Score each location
fsiz = size(featIm(:,:,1));
resp = zeros(fsiz - [ny nx] + 1);
for i = 1:nf,
  resp = resp + conv2(featIm(:,:,i),w(:,:,i),'valid');
end
