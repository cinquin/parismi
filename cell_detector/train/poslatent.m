function [newpos delta] = poslatent(t,iter,model,pos,minov,Jpos)
% Sam Hallman, copyright 2015

global cache;
model.numsc = 7;
stacks = [pos.stackind];
[~,I] = unique(stacks);
stacks = stacks(sort(I));
w = model.w_sym;
if t == 1
  assert(cache.n==0 && all(w==0));
  % we disable local nms and use weights w=0 so that every
  % pos will be assigned to its highest-overlapping window
  localnms = false;
  minov = 0;
  model.minsc = 1;
  model.maxsc = 1;
  % init all cache fields except the features
  I = 1:length(pos);
  cache.val(I)   = Jpos;
  cache.ids(1,I) = I;
  cache.ids(2,I) = [pos.stackind];
  cache.posi(I)  = true;
  cache.n        = I(end);
else
  feat = cache.dat(:,cache.posi);
  score0 = w'*feat/Jpos;
  localnms = true;
end

cnt = 0;
newpos = [];
must_improve = 0;
for s = stacks
  fprintf('iter %d/%d: poslatent: stack %d\n', t, iter, s);
  % Run the detect code in latent mode
  spos = pos([pos.stackind] == s);
  numpos = length(spos);
  info = loadinfo(s); stack = loadstack(s);
  [dets,feat,matched] = detect_multiscale(stack, ...
    model, inf, localnms, true, spos, minov, info.zscale);
  fprintf('matched %d/%d', sum(matched), numpos);
  if t == 1 && ~all(matched)
    display('warning: poslatent failed');
    keyboard;
  end
  % Write features to the cache
  I = cnt + find(matched);
  updated = false(1,numpos);
  if t > 1
    improve = dets.r' > score0(I) - 1e-5;
    if must_improve
      dets = sub(dets,improve);
      feat = feat(:,improve);
      I = I(improve);
    end
    fprintf(', improved %d', sum(improve));
  end
  new = sum(cache.dat(:,I) - feat*Jpos,1) ~= 0;
  cache.dat(:,I) = feat*Jpos;
  updated(I-cnt) = true;
  cnt = cnt + numpos;
  assert(all(cache.ids(2,I) == s));
  % Record bbox coordinates of the updated positives to newpos
  spos2 = convert(spos, dets, updated);
  newpos = [newpos, spos2];
  fprintf(', updated %d (%d new)\n', sum(updated), sum(new));
end
assert(cnt == length(pos));

if t == 1
  delta = inf;
else
  feat = cache.dat(:,cache.posi);
  score1 = w'*feat/Jpos;
  loss0 = sum(max(0,1-score0));
  loss1 = sum(max(0,1-score1));
  delta = abs((loss1-loss0)/loss0);
  fprintf('pos loss before: %f, after: %f, delta: %f\n', loss0, loss1, delta);
  if must_improve && loss1 > loss0
    display('warning: pos loss went up');
    keyboard;
  end
end


function pos = convert(pos, dets, updated)

cnt = 0;
for i = 1:length(updated)
  if updated(i)
    cnt = cnt + 1;
    pos(i).x1 = dets.x(cnt);
    pos(i).y1 = dets.y(cnt);
    pos(i).z1 = 'ignore';
    pos(i).x2 = dets.x(cnt) + dets.w(cnt) - 1;
    pos(i).y2 = dets.y(cnt) + dets.h(cnt) - 1;
    pos(i).z2 = 'ignore';
    pos(i).x = (pos(i).x1 + pos(i).x2)/2;
    pos(i).y = (pos(i).y1 + pos(i).y2)/2;
    pos(i).z = dets.zc(cnt);
  end
end
assert(cnt == length(dets.x));
