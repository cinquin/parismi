function outsize = featsize(insize, sbin)
% Sam Hallman, copyright 2015

outsize = [max(round(insize./sbin)-2,0) 23];
