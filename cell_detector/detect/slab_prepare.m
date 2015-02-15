function [imrows,shiftMask,rowMask,padys,extras] = ...
  slab_prepare(imy,ny,sbin,y1want,y2want)
%
% Sam Hallman, copyright 2015

maxpady = ny-1;

dim = (ny+2)*sbin;
pady = maxpady;
fsize = featsize([imy sbin*3],sbin);
fsize = pady + fsize(1) + pady;
rsize = fsize - ny + 1;
y1 = ([1:rsize]-pady-1)*sbin + 1;
ymid = y1+(dim-1)/2;
ymid = floor(ymid);

rows = ymid(1):ymid(end)+sbin-1;
shiftMask = nan(length(rows),1);
rowMask = nan(length(rows),sbin);
for shift = 0:sbin-1
  shiftMask(shift+1:sbin:end) = shift;
  count = sum(shiftMask==shift);
  rowMask(shift+1:sbin:end,shift+1) = 1:count;
end

imrows = nan(2,sbin);
extras = nan(2,sbin);
padys = nan(2,sbin);

found = false(1,sbin);
for y = y1want:y2want
  i = find(rows == y);
  s = shiftMask(i);
  r = rowMask(i,s+1);
  featRows = r:(r+ny-1);

  % how many of the top/bot feat rows are padding?
  firstBotPadRow = fsize - pady + 1;
  numpadtop = max(0,pady-featRows(1)+1);
  numpadbot = max(0,featRows(end)-firstBotPadRow+1);

  % the first row in featRows that isn't padding
  r1 = max(pady+1,featRows(1));
  % the image rows which yield those features
  [i1,~,extra,~] = featrow2imrows(r1-pady,imy,sbin);
  
  if ~found(s+1)
    imrows(1,s+1) = i1;
    padys(1,s+1) = numpadtop;
    extras(1,s+1) = extra;
    found(s+1) = true;
  end
  
  % the image rows which yield the last row of features
  r2 = max(pady+1,featRows(end));
  [~,i2,~,extra] = featrow2imrows(r2-pady,imy,sbin);

  if found(s+1)
    imrows(2,s+1) = i2;
    padys(2,s+1) = numpadbot;
    extras(2,s+1) = extra;
  end
end

i = find(rows == y1want);
j = find(rows == y2want);
shiftMask = shiftMask(i:j);
rowMask = nan(length(shiftMask),sbin);
for shift = 0:sbin-1
  I = shiftMask == shift;
  rowMask(I,shift+1) = 1:sum(I);
end


function [i1,i2,toppad,botpad] = featrow2imrows(i,imy,sbin)

h = round(imy/sbin)*sbin;

% pixel coordinates of hog cell i
i1 = i*sbin+1;
i2 = (i+1)*sbin;

% pad on top/bot by one hog cell
i1 = max(i1-sbin,1);
i2 = min(i2+sbin,h);

% pad on top/bot by another hog cell
i1 = i1-sbin; if i1<1, i1=1; toppad=0; else toppad=1; end
i2 = i2+sbin; if i2>h, i2=h; botpad=0; else botpad=1; end

i2 = min(i2,imy);
