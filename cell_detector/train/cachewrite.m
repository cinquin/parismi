function cachewrite(path)
% Sam Hallman, copyright 2015

global cache;
if isempty(cache)
  error('cache not found');
end

% Shrink dat to cache.n columns before writing to save
% space and speed up the disk write
cachesize = size(cache.dat);
cache.dat = cache.dat(:,1:cache.n);

% Leave all other cache variables "as is" and write to disk
save(path, 'cache', 'cachesize');

% Expand back to original size
cache.dat(:,cache.n+1:cachesize(2)) = 0;
assert(isequal(cachesize,size(cache.dat)));
