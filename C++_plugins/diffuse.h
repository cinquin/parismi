
//******************************************************************************
// Diffuse.cpp Copyright(c) 2003 Jonathan D. Lettvin, All Rights Reserved.
//******************************************************************************
// Permission to use this code if and only if the first eight lines are first.
// This code is offered under the GPL.
// Please send improvements to the author: jdl@alum.mit.edu
// The User is responsible for errors.
//******************************************************************************

#ifndef DIFFUSE_H_
#define DIFFUSE_H_
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wheader-hygiene"
#pragma clang diagnostic ignored "-Wconversion"
#pragma clang diagnostic ignored "-Wfloat-equal"
#pragma clang diagnostic ignored "-Wsign-conversion"
#pragma clang diagnostic ignored "-Wsign-compare"

#include <iostream>  // sync_with_stdio
#include <cstdio>    // printf
#include <cstdlib>   // I believe sqrt is in here
#include <ctime>     // Used to salt the random number generator
#include <cmath>     // Various mathematical needs
#include <vector>    // Used to contain many points
#include <valarray>  // Used to implement a true XYZ vector
//******************************************************************************

using namespace std;
// Necessary to gain access to many C++ names

//******************************************************************************
typedef valarray<double> coordinates; // To simplify declarations


//******************************************************************************

/*!
 * \class XYZ
 * \brief Used to find uniformly spaced points on a sphere.
 */
class XYZ { // This class contains operations for placing and moving points
private:
	double change_magnitude;
	coordinates xyz; // This holds the coordinates of the point.
	coordinates dxyz; // This holds the summed force vectors from other points
	inline double random() {
		return (double((rand() % 1000) - 500));
	} // ordinates
	inline double square(const double& n) {
		return (n * n);
	}
	inline coordinates square(const coordinates& n) {
		return (n * n);
	}
	inline double inverse(const double& n) {
		return (1.0 / n);
	}
	XYZ& inverse_square() {
		xyz *= inverse(square(magnitude()));
		return *this;
	}
	inline double magnitude() {
		return (sqrt((xyz * xyz).sum()));
	}
	void normalize() {
		xyz /= magnitude();
	} // unit vector
public:
	XYZ() :
		xyz(3), dxyz(3) {
		xyz[0] = random();
		xyz[1] = random();
		xyz[2] = random();
		normalize();
	}
	XYZ(const double& x, const double& y, const double& z) :
		xyz(3), dxyz(3) {
		xyz[0] = x;
		xyz[1] = y;
		xyz[2] = z;
	}
	XYZ(const coordinates& p) :
		xyz(3), dxyz(3) {
		xyz = p;
	}
	coordinates& array() {
		return xyz;
	}
	void zero_force() {
		dxyz = 0.0;
	}
	double change() {
		return (change_magnitude);
	}
	double magnitude(XYZ& b) { // Return length of vector.  (not const)
		return (sqrt(square(b.array() - xyz).sum()));
	}
	void sum_force(XYZ& b) { // Cause force from each point to sum.  (not const)
		dxyz += (XYZ(b.array() - xyz).inverse_square().array()); // Calculate and add
	}
	void move_over_sphere() { // Cause point to move due to force
		coordinates before = xyz; // Save previous position
		xyz -= dxyz; // Follow movement vector
		normalize(); // Project back to sphere
		before -= xyz; // Calculate traversal
		change_magnitude = sqrt((before * before).sum()); // Record largest
	}
	void report(const double& d) {
		printf("  { %+1.3e,%+1.3e,%+1.3e,%+1.3e }", xyz[0], xyz[1], xyz[2], d);
	}
	void report(float &xc, float &yc, float &zc) {
		xc = xyz[0];
		yc = xyz[1];
		zc = xyz[2];
	}
};

class points { // This class organizes expression of relations between points
private:
	const size_t N; // Number of point charges on surface of sphere
	const double L; // Threshold of movement below which to stop
	const size_t R; // Number of rounds after which to stop
	const char *S; // Name of this vertex set
	size_t rounds; // Index of rounds processed
	vector<XYZ> V; // List of point charges
	vector<double> H; // List of minimum distances
	double maximum_change; // The distance traversed by the most moved point
	double minimum_radius; // The radius of the smallest circle
	//time_t T0; // Timing values

	void relax() { // Cause all points to sum forces from all other points
		size_t i, j;
		rounds = 0;
		do {
			maximum_change = 0.0;
			for (i = 1; i < N; i++) { // for all points other than the fixed point
				V[i].zero_force(); // Initialize force vector
				for (j = 0; j < i; j++)
					V[i].sum_force(V[j]); // Get contributing forces
				// Skip i==j
				for (j = i + 1; j < N; j++)
					V[i].sum_force(V[j]); // Get contributing forces
			}
			for (i = 1; i < N; i++) { // React to summed forces except for the fixed point
				V[i].move_over_sphere();
				if (V[i].change() > maximum_change)
					maximum_change = V[i].change();
			}
		} while (maximum_change > L && ++rounds < R); // Until small or too much movement
	}
public:
	points(const char *s, const size_t& n, const double& l, const size_t& r) :
		N(n), L(l), R(r) {
		S = s;
		//T0 = time(NULL); // Get the current time
		srand(1); // Salt the random number generator.  USE SAME SEED (1 INSTEAD OF T0) SO FUNCTIONAL TESTS WORK
		V.push_back(XYZ(1.0, 0.0, 0.0)); // Create Anchored first point V[0] (1,0,0)
		H.push_back(2.0);
		while (V.size() < N) { // For all other points, until we have enough
			V.push_back(XYZ()); // Create randomized position
			H.push_back(2.0);
			coordinates& last = V.back().array(); // Remember this position
			for (size_t i = V.size() - 1; i--;) { // And check to see if it is occupied
				coordinates& temp = V[i].array();
				if (temp[0] == last[0] && temp[1] == last[1] && temp[2]
						== last[2]) {
					V.pop_back(); // Remove the position if it is already occupied
					break;
				}
			}
		}
		relax(); // After vector construction, start the relaxation process
		size_t i, j;
		minimum_radius = 1.0; // On a unit sphere, the maximum circle radius is 1.0
		for (i = 0; i < V.size(); i++) { // Discover the minimum distance between points.
			for (j = 0; j < V.size(); j++) {
				if (j == i)
					continue;
				double rtemp = V[i].magnitude(V[j]) / 2.0;
				if (rtemp < minimum_radius)
					minimum_radius = rtemp; // Record when smaller.
				if (rtemp < H[i])
					H[i] = rtemp;
			}
		}
	}
	~points() {
	}
	coordinates& operator[](const size_t& i) { // Caller access to positions
		return (V[i].array());
	}

	void report(Array1D<float> *xc, Array1D<float> *yc, Array1D<float> *zc) { // output positions of all points in
		xc->resize(N);
		yc->resize(N);
		zc->resize(N);
		for (int i = 0; i < N; i++) {
			V[i].report((*xc)(i), (*yc)(i), (*zc)(i));
		}
	}

};

#pragma clang diagnostic pop
#endif /* TEXTINPUT_H_ */
