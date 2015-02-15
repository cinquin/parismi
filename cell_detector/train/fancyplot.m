function fancyplot(im,boxes)
% Sam Hallman, copyright 2015

nrow = 2;
ncol = 10;

clim = [min(im(:)) max(im(:))];

if isempty(boxes)
  n = 0;
else
  n = length(boxes.x);
end

% Display image with all boxes centers on top row
% Color top detections green, others red
subplot(nrow,ncol,[1 ncol]);
imagesc(im); axis image; colormap gray;
if n > 0
  if n < ncol
    drawPoints(boxes,'g');
  else
    [~,I] = sort([boxes.r],'descend');
    drawPoints(sub(boxes,I(1:ncol)),'g');
    drawPoints(sub(boxes,I(ncol+1:end)),'r');
  end
end

if n == 0
  % whatever, just use some value so that we can show the "X" images
  d = 48;
else
  % Boxes should all be square and of the same size
  d = boxes.w(1);
  assert(all(d == boxes.w));
  assert(all(d == boxes.h));
end

% Big "X" image
X = zeros(d,d);
diag = sub2ind([d d],1:d,1:d);
antidiag = sub2ind([d d],1:d,d:-1:1);
X(diag) = 1;
X(antidiag) = 1;

% Display top 10 negatives on bottom row
if n > 0
  [r,I] = sort([boxes.r],'descend');
end
for i = 1:min(n,ncol)
  j = I(i);
  subplot(nrow,ncol,ncol+i);
  x1 = boxes.x(j);
  y1 = boxes.y(j);
  x2 = x1 + boxes.w(j) - 1;
  y2 = y1 + boxes.h(j) - 1;
  patch = subarray(im,y1,y2,x1,x2,0);
  imagesc(patch);
  set(gca,'clim',clim);
  axis image;
  axis off;
  colormap gray;
  if isfield(boxes,'dists')
    str3 = sprintf('dist = %g',boxes.dists(j));
  elseif isfield(boxes,'ov')
    str3 = sprintf('ov = %g',boxes.ov(j));
  else
    error('something is very wrong');
  end
  title({sprintf('conf = %g',r(i)); ...
         sprintf('mb = %g',mean(patch(:))); ...
         str3});
  hold on, plot(d/2,d/2,'g.'), hold off
end
for i = min(n,ncol)+1:ncol
  subplot(nrow,ncol,ncol+i);
  imagesc(X);
  colormap gray;
  axis image, axis off;
end


function drawPoints(boxes,color)
hold on
xcen = boxes.x + (boxes.w-1)/2;
ycen = boxes.y + (boxes.h-1)/2;
plot(xcen,ycen,[color '.']);
hold off
