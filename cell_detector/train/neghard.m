function model = neghard(t,iter,model,pos,ims,maxov,C,cTol,show)
% Sam Hallman, copyright 2015

global cache;
assert(~isempty(cache));
assert(sum(cache.posi) > 0);
assert(size(cache.dat,1) == model.symlen);

% Learning parameters for SVM optimization
%cTol = .03;  % Tolerance for trigger of cache update
qTol = .001; % Tolerance of inner qp solver
mTol = .001; % Tolerance of margin for support vector selection
%xTol = 30;   % Tolerance for Michael's x-thresholds

model.thresh = -1 - mTol;
ineg = 1; % Pointer to particular image
icnt = 1; % Number of images encountered with a fixed model
iall = 0; % Total number of iterations encountered
imax = length(ims);
w0   = zeros(model.unsymlen,1);
obj  = 0;
lb   = 0;
done = 0;

% After the first iteration, we need not start from w=0
if t > 1
  fprintf('neghard: t>1, calling qpopt\n');
  J = 1:cache.n;
  [w0,qp] = qpopt(cache.dat(:,J),cache.val(J),C,qTol);
  model = vec2model(w0,model);
  lb  = qp.lb;
  obj = lb;
end

slast = -1;
% Grab negative features
% Repeat until we can pass through all images w/o retraining
while ~done,
  fprintf('\nt=%d/%d fr=%d/%d (%d)',t,iter,ineg,imax,icnt);

  % Read the stack from disk if necessary
  sInd = ims(ineg).stackind;
  if sInd ~= slast
    fprintf(' [reading stack...]');
    info = loadinfo(sInd);
    stack = loadstack(sInd);
    slast = sInd;
  end

  % Look for hard negatives
  maxnum = size(cache.dat,2) - cache.n;
  [blobs,feat] = detect_singlescale(...
    stack, model, 1, ims(ineg).z*[1 1], maxnum, false, true, true);

% % Remove detections beyond Michael's threshold
% if ~isempty(blobs) && isfield(info,'xthresh'),
%   xt = info.xthresh;
%   xcen = blobs.x + (blobs.w-1)/2;
%   if xt<0, I = xcen < -xt - xTol;      % unmarked positives are to the *right*
%   else,    I = xcen >  xt + xTol; end  % unmarked positives are to the *left*
%   blobs = sub(blobs,I);
%   feat = feat(:,I);
% end

  % Remove detections too close to a positive
  if ~isempty(blobs) && ~isempty(blobs.x),
    [I,ov] = filternegs(blobs, pos, maxov, info.zscale);
    blobs = sub(blobs,I);
    feat = feat(:,I);
    if show
      blobs.ov = ov;
    end
  end

  % Add non-duplicate features to cache
  if ~isempty(blobs) && ~isempty(blobs.x),
    assert(length(blobs.x) == size(feat,2)); % check for bugs
    if show,
      set(0,'CurrentFigure',1);
      showModel(model); drawnow;
      set(0,'CurrentFigure',2);
      res = sub(blobs,blobs.r > -1);
      fancyplot(stack(:,:,ims(ineg).z),res); drawnow;
    end
    idt = [repmat(uint16(ineg),size(blobs.x)) blobs.id]';
    [~,inds] = intersect(idt',cache.ids','rows');
    I = true(size(blobs.x));
    I(inds) = 0;
    if any(I),
      r = blobs.r(I);
      obj = obj + C*sum(max(1+r,0));
      % Total error is unbounded if we didn't collect all violations
      if size(feat,2) == maxnum,
        obj = inf;
      end
      J = (cache.n+1):(cache.n+sum(I));
      cache.dat(:,J) = -feat(:,I);
      cache.val(:,J) = 1;
      cache.ids(:,J) = idt(:,I);
      cache.n = J(end);
      fprintf(' Cache+%d=%d/%d, Obj_LB=%.3f, Obj=%.3f, Gap=%.3f', ...
        length(blobs.x), cache.n, size(cache.dat,2), lb, obj, 1-lb/obj);
    end
  end
    
  % Update model if necessary
  if (1 - lb/obj > cTol || icnt == imax || cache.n == size(cache.dat,2)),
    % Call QP to update lower bound
    fprintf('..Calling qp..');
    J = 1:cache.n;
    [w0,qp] = qpopt(cache.dat(:,J),cache.val(J),C,qTol);
    if qp.lb > obj + 1e-4
      fprintf('\n\nbug: lb > ub\n\n');
      keyboard;
    end
    lb = qp.lb;
    
    % Prune cache, keeping all positive features
    I = find(qp.sv | cache.posi(J));
    cache.n = length(I);
    J = 1:cache.n;
    cache.dat(:,J) = cache.dat(:,I);
    cache.val(:,J) = cache.val(:,I);
    cache.ids(:,J) = cache.ids(:,I);

    % 1) We want to avoid updating "w" since this prevents us from calculating 
    %    an upper bound (obj = upper bound after passing through data)
    % 2) If "w" is not epsilon-optimal w.r.t. current lower bound, then we know
    %    we won't be epsilon-optimal after passing through all data so let's update "w"
    %    and reset counter
    if 1 - lb/obj > cTol,
      model = vec2model(w0,model);
      iall = iall + icnt;
      icnt = 0;
      obj  = lb;
    elseif icnt == imax,
      fprintf('\n Converged lb=%.3f,obj=%.3f,#cache=%d, niter=%d/%d\n', ...
        lb, obj, cache.n, iall+icnt, imax);
      done = true;
      model = vec2model(w0,model);
      model.w_sym = w0;
    end
  end
  icnt = icnt + 1;
  ineg = ineg + 1;
  if ineg > imax,
    ineg = 1;
  end
end

if show
  set(0,'CurrentFigure',1);
  showModel(model);
end
