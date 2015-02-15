function B = subarray(A, i1, i2, j1, j2, pad)
  
% B = subarray(A, i1, i2, j1, j2, pad)
% Extract subarray from array
% pad with boundary values if pad = 1
% pad with zeros if pad = 0

assert(pad == 0 || pad == 1);

dim = size(A);
is = i1:i2;
js = j1:j2;

if pad == 0
  % Record which indices are out of bounds
  I = is<1 | is>dim(1);
  J = js<1 | js>dim(2);
end

is = min(max(is,1),dim(1));
js = min(max(js,1),dim(2));
B  = A(is,js,:);

if pad == 0
  B(I,:,:) = 0;
  B(:,J,:) = 0;
end
