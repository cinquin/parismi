function Y = localmax(X)
%
% Sam Hallman, copyright 2015

[m n p] = size(X); assert(m>2 && n>2);
Y = false(m,n,p);
I = 2:m-1;
J = 2:n-1;
Y(I,J,:) = ( X(I,2:n-1,:) > X(I,3:n,:)   ) & ...     % L>R?
           ( X(I,2:n-1,:) > X(I,1:n-2,:) ) & ...     % R>L?
           ( X(2:m-1,J,:) > X(3:m,J,:)   ) & ...     % U>D?
           ( X(2:m-1,J,:) > X(1:m-2,J,:) );          % D>U?
