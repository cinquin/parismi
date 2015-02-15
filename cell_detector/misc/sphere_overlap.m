function ov = sphere_overlap(Y,X)
% ov = sphere_overlap(Y,X)
%
% ov(i,j) = intersection-over-union sphere overlap
%           between Y(i,1:4) and X(j,1:4).
%
% hence ov is m-by-n, where m=size(Y,1), n=size(X,1).
%
% Sam Hallman, copyright 2015

inter = sphere_intersect(Y,X);

vy = 4/3*pi*Y(:,4).^3;
vx = 4/3*pi*X(:,4).^3;
[Vx,Vy] = meshgrid(vx,vy);
union = Vy+Vx-inter;

ov = inter ./ union;


function M = sphere_intersect(Y,X)
% Modified from volume_intersect_sphere_analytical
% from Guillaume JACQUENOT on MATLAB File Exchange

if any(X(:,4)<=0) || any(Y(:,4)<=0)
  error('sphere radii should be positive');
end

D = sqrt(dist2(Y(:,1:3),X(:,1:3)));
[Xr,Yr] = meshgrid(X(:,4),Y(:,4));
sumR = Xr+Yr;
difR = abs(Xr-Yr);

M = zeros(size(difR));

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% C2: Case 2: spheres i & j fully overlap
% One of the spheres is inside the other one.
C2    = D<=difR;
M(C2) = 4/3*pi*min(Xr(C2),Yr(C2)).^3;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% Case 3: spheres i & j partially overlap
% Partial intersection between spheres i & j
C3 = (D>difR)&(D<sumR);
% Computation of the coordinates of one of the intersection points of the
% spheres i & j
Pi = (Xr.^2-Yr.^2+D.^2)./(2*D);

H1 = Xr-Pi;
H2 = Yr+Pi-D;

% Computation of the partial intersection volume between spheres i & j
M3 = pi/3*(H1.^2.*(3*Xr-H1)+...
           H2.^2.*(3*Yr-H2));

M(C3) = M3(C3);




%   M = zeros(size(difR));
%   [Pi,H1,H2] = deal(M);
%   
%   C2    = D<=difR;
%   M(C2) = 4/3*pi*min(Xr(C2),Yr(C2)).^3;
%   
%   C3 = (D>difR)&(D<sumR);
%   Pi(C3) = (Xr(C3).^2-Yr(C3).^2+D(C3).^2)./(2*D(C3));
%   
%   H1(C3) = Xr(C3)-Pi(C3);
%   H2(C3) = Yr(C3)+Pi(C3)-D(C3);
%   
%   % Computation of the partial intersection volume between spheres i & j
%   M(C3) = pi/3*(H1(C3).^2.*(3*Xr(C3)-H1(C3))+...
%                 H2(C3).^2.*(3*Yr(C3)-H2(C3)));
