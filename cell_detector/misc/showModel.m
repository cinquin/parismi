function showModel(model)
% Sam Hallman, copyright 2015

subplot(2,2,1); showHOG(model.xy.w); axis off; title 'XY';
subplot(2,2,2); showHOG(model.xz.w); axis off; title 'XZ';

subplot(2,2,[3 4]);
w = model.xy.wm;
e = edges(); e = e(1:end-1);
plot(e,w,'b-','LineWidth',2); hold on;
plot(e,w,'ro','LineWidth',2); hold off;
set(gca,'YGrid','on');
