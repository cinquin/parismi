function model = train(model,pos,ims,iter,negov,posov,C,bytelimit,cTol,tag)
% Sam Hallman, copyright 2015

show = true;
if show
  figure(2);
  figure(1);
end
Jpos = 1;
model.parms.Jpos = Jpos;
base = fileparts(fileparts(mfilename('fullpath')));

% Allocate empty cache
global cache;
cacheinit(bytelimit,model);

for t = 1:iter
  %
  % Step 1: Optimize wrt latent labels (z)
  %
  [newpos, model.delta] = poslatent(t,iter,model,pos,posov,Jpos);
  if t == 1
    pos = newpos;
  end
  if model.delta < 0.001
    fprintf('Converged (delta=%f)\n', model.delta);
    break;
  end

  %
  % Step 2: Optimize wrt weights (w)
  %
  model = neghard(t,iter,model,newpos,ims,negov,C,cTol,show);
  fprintf('iter %d/%d: #cache=%d (#pos=%d #neg=%d)\n', ...
    t, iter, cache.n, sum(cache.posi), cache.n-sum(cache.posi));

  % set threshold for high recall
  rpos = sort(model.w_sym'*cache.dat(:,cache.posi)/Jpos);
  model.thresh = rpos(ceil(length(rpos)*0.03));
  fprintf('set model.thresh = %f\n',model.thresh);

  % save model
  name = ['model_' tag '_' num2str(t)];
  save([base '/data/' name '.mat'], 'model');
end

% save final model
name = ['model_' tag '_final'];
save([base '/data/' name '.mat'], 'model');
