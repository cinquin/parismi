function [first,last] = rangesplit(range,splitnum)
% Sam Hallman, copyright 2015

k = splitnum;
n = length(range);
frac = n/k - floor(n/k);
a = round(k*frac);         % num ceil
b = k-a;                   % num floor
high = ceil(n/k);
low = floor(n/k);

assert(n == a*high + b*low);

i = range(1);
j = i + (b-1)*low;
k = j + low;
l = k + (a-1)*high;

first = [i:low:j, k:high:l];
last  = [first(2:end)-1, range(end)];

assert(length(first) == splitnum);
