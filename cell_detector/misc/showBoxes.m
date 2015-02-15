function showBoxes(boxes,color)
% showBoxes(boxes,color)
% e.g. color = 'r'
% Sam Hallman, copyright 2015

if nargin < 2
  color = 'r';
end

x1 = boxes.x;
y1 = boxes.y;
x2 = boxes.x + boxes.w - 1;
y2 = boxes.y + boxes.h - 1;

line([x1 x1 x2 x2 x1]', [y1 y2 y2 y1 y1]', ...
  'Color', color, 'LineStyle', '-', 'LineWidth', 1);
