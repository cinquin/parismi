package processing_utilities.straightening;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class LocalStraightener {
	public static ImageProcessor localStraightenLine(ImageProcessor ip, PolygonRoi roi, int width) {
		ip.setInterpolationMethod(ImageProcessor.NEAREST_NEIGHBOR);
		if (roi == null)
			return null;
		if (roi.getState() == Roi.CONSTRUCTING)
			roi.exitConstructingMode();
		boolean isSpline = roi.isSplineFit();
		int type = roi.getType();
		roi.fitSplineForStraightening();
		if (roi.getNCoordinates() < 2)
			return null;
		FloatPolygon p = roi.getFloatPolygon();
		int n = p.npoints;
		ImageProcessor ip2 = new FloatProcessor(n, width);
		double x1, y1;
		double x2 = p.xpoints[0] - (p.xpoints[1] - p.xpoints[0]);
		double y2 = p.ypoints[0] - (p.ypoints[1] - p.ypoints[0]);
		if (width == 1)
			ip2.putPixelValue(0, 0, ip.getInterpolatedValue(x2, y2));
		for (int i = 0; i < n; i++) {
			x1 = x2;
			y1 = y2;
			x2 = p.xpoints[i];
			y2 = p.ypoints[i];
			// if (distances!=null) distances.putPixelValue(i, 0, (float)Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)));
			if (width == 1) {
				ip2.putPixelValue(i, 0, ip.getInterpolatedValue(x2, y2));
				continue;
			}
			double dx = x2 - x1;
			double dy = y1 - y2;
			double length = (float) Math.sqrt(dx * dx + dy * dy);
			dx /= length;
			dy /= length;
			// IJ.log(i+"  "+x2+"  "+dy+"  "+(dy*width/2f)+"   "+y2+"  "+dx+"   "+(dx*width/2f));
			double x = x2 - dy * width / 2.0;
			double y = y2 - dx * width / 2.0;
			int j = 0;
			int n2 = width;
			do {
				ip2.putPixelValue(i, j++, ip.getInterpolatedValue(x, y));
				// ip.drawDot((int)x, (int)y);
				x += dy;
				y += dx;
			} while (--n2 > 0);
		}
		if (!isSpline) {
			if (type == Roi.FREELINE)
				roi.removeSplineFit();
		}
		return ip2;
	}

	public static ImageProcessor localStraightenLine(ImageProcessor ip, FloatPolygon p, int width) {
		ip.setInterpolationMethod(ImageProcessor.NEAREST_NEIGHBOR);
		// if (roi==null) return null;
		// if (roi.getState()==Roi.CONSTRUCTING)
		// roi.exitConstructingMode();
		// boolean isSpline = roi.isSplineFit();
		// int type = roi.getType();
		// roi.fitSplineForStraightening();
		// if (roi.getNCoordinates()<2) return null;
		// FloatPolygon p = roi.getFloatPolygon();
		int n = p.npoints;
		ImageProcessor ip2 = new FloatProcessor(n, width);
		double x1, y1;
		double x2 = p.xpoints[0] - (p.xpoints[1] - p.xpoints[0]);
		double y2 = p.ypoints[0] - (p.ypoints[1] - p.ypoints[0]);
		if (width == 1)
			ip2.putPixelValue(0, 0, ip.getInterpolatedValue(x2, y2));
		for (int i = 0; i < n; i++) {
			x1 = x2;
			y1 = y2;
			x2 = p.xpoints[i];
			y2 = p.ypoints[i];
			// if (distances!=null) distances.putPixelValue(i, 0, (float)Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)));
			if (width == 1) {
				ip2.putPixelValue(i, 0, ip.getInterpolatedValue(x2, y2));
				continue;
			}
			double dx = x2 - x1;
			double dy = y1 - y2;
			double length = (float) Math.sqrt(dx * dx + dy * dy);
			dx /= length;
			dy /= length;
			// IJ.log(i+"  "+x2+"  "+dy+"  "+(dy*width/2f)+"   "+y2+"  "+dx+"   "+(dx*width/2f));
			double x = x2 - dy * width / 2.0;
			double y = y2 - dx * width / 2.0;
			int j = 0;
			int n2 = width;
			do {
				ip2.putPixelValue(i, j++, ip.getInterpolatedValue(x, y));
				// ip.drawDot((int)x, (int)y);
				x += dy;
				y += dx;
			} while (--n2 > 0);
		}

		return ip2;
	}
}