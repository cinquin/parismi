function [match,matched] = posmatch(pos,dets,ov,zscale)
%
% Sam Hallman, copyright 2015

npos = length(pos);
Xpos = seed_spheres(pos,zscale);
Xdet = det_spheres(dets,zscale);
O = sphere_overlap(Xpos,Xdet);

scores = repmat(dets.r',[npos 1]);
scores(O < ov) = -inf;
[s,match] = max(scores,[],2);
matched = s > -inf;

if 1
  for i = find(matched)'
    if sum(scores(i,:) == s(i)) > 1
      [~,j] = max(O(i,:));
      assert(scores(i,j) == s(i));
      match(i) = j;
    end
  end
end

match(~matched) = [];
