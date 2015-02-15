function cacheinit(numbytes,model)
% Sam Hallman, copyright 2015

global cache;
assert(isempty(cache));

featbytes = model.symlen*4;
nmax = floor(numbytes/featbytes);

cache.dat  = zeros(model.symlen,nmax,'single');
cache.val  = zeros(1,nmax,'single');
cache.ids  = zeros(4,nmax,'uint16');
cache.posi = false(1,nmax);
cache.n    = 0;
