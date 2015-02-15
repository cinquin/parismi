function cacheload(path)
% Sam Hallman, copyright 2015

saved = load(path, 'cache', 'cachesize');

% The dat file on disk was shrunk to cache.n columns
global cache;
cache.n = saved.cache.n;
cache.dat = zeros(saved.cachesize,'single');
cache.dat(:,1:cache.n) = saved.cache.dat;

% The other cache variables were saved "as is"
cache.val  = saved.cache.val;
cache.ids  = saved.cache.ids;
cache.posi = saved.cache.posi;
cache.n    = saved.cache.n;
