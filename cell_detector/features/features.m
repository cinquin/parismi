function feat = features(x,sbin)
% Sam Hallman, copyright 2015

if length(sbin) == 1
  sbiny = sbin;
  sbinx = sbin;
else
  sbiny = sbin(1);
  sbinx = sbin(2);
end

feat = features_mex(double(x),sbiny,sbinx);
