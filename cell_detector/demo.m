% (1) Run compile.m
% (2) Set these to true or false:
do_train_demo  = false;
do_detect_demo = true;

%
% Simple demo of how to train a detector
%
if do_train_demo
  fprintf('\n\nRunning training demo\n\n');

  % Train on the stack ./data/stacks/1.tif
  trainset = [1];

  % Give the training code some information about cell sizes
  dim   = 48;   % big enough to contain the average-sized cell, plus margin
  minsc = 0.7;  % the min scale to search over (find small cells)
  maxsc = 1.5;  % the max scale to search over (find big cells)

  % Train a detector
  % note that all models (intermediate and final) are saved in ./data/
  model = model_train('trainset',trainset, ...
    'dim',dim, 'minsc',minsc, 'maxsc',maxsc);
end

%
% Simple demo of how to use a detector to find cells
%
if do_detect_demo
  fprintf('\n\nRunning detection demo\n\n');

  % Load a trained detector
  worm = load('data/wormModel.mat');
  worm.model.numsc = 9;  % search over 9 scales
  
  % Run it on ./data/stacks/1.tif (should take ~30 sec)
  stackNum = 1;
  stack = loadstack(stackNum);
  tic; dets = detect_multiscale(stack, worm.model, inf, true); toc

  % Threshold the detections
  thresh = 0;  % ...play around with this
  dets = sub(dets, dets.r>thresh);

  % Do NMS
  info = loadinfo(stackNum);
  alpha = 0.4;
  dets = nms(dets, alpha, info.zscale);

  % Collect the (x,y,z,confidence) results
  x = dets.x + (dets.w-1)/2;
  y = dets.y + (dets.h-1)/2;
  z = dets.zc;
  conf = dets.r;

  figure(1); clf;
  fprintf('The top 9 highest-scoring detections are:\n');
  [~,I] = sort(conf,'descend');
  for i = 1:9
    j = I(i);
    fprintf('  x=%g y=%g z=%g conf=%g\n', x(j), y(j), z(j), conf(j));

    subplot(3,3,i);
    imagesc(stack(:,:,round(z(j)))); axis image; colormap gray
    hold on;
    h = rectangle('Position',[x(j)-dets.w(j)/2 y(j)-dets.h(j)/2 dets.w(j) dets.h(j)],'Curvature',[1 1],'EdgeColor','r');
    plot(x(j),y(j),'ro','LineWidth',3); 
    hold off;
  end
end
