package ij.gui;
import ij.*;
import ij.measure.Calibration;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

/** This class is an extended ImageWindow used to display image stacks. */
public class StackWindow extends ImageWindow implements Runnable, AdjustmentListener, ActionListener, MouseWheelListener {

	protected Scrollbar sliceSelector; // for backward compatibity with Image5D
	private ScrollbarWithLabel cSelector, zSelector, tSelector;
	protected Thread thread;
	protected volatile boolean done;
	protected volatile int slice;
	private ScrollbarWithLabel animationSelector;
	boolean hyperStack;
	int nChannels=1;
	protected int nSlices=1;
	public int getnSlices() {
		return nSlices;
	}

	public int getZ() {
		return imp.getCurrentSlice();
	}

	int nFrames=1;
	protected int c=1;
	protected int z=1;
	protected int t=1;
	

	public StackWindow(ImagePlus imp) {
		this(imp, null);
	}
    
    public StackWindow(ImagePlus imp, ImageCanvas ic) {
		super(imp, ic);
		ImageStack s = imp.getStack();
		int stackSize = s.getSize();
		nSlices = stackSize;
		hyperStack = imp.getOpenAsHyperStack();
		imp.setOpenAsHyperStack(false);
		int[] dim = imp.getDimensions();
		int nDimensions = 2+(dim[2]>1?1:0)+(dim[3]>1?1:0)+(dim[4]>1?1:0);
		if (nDimensions<=3 && dim[2]!=nSlices) hyperStack = false;
		if (hyperStack) {
			nChannels = dim[2];
			nSlices = dim[3];
			nFrames = dim[4];
		}
		//IJ.log("StackWindow: "+hyperStack+" "+nChannels+" "+nSlices+" "+nFrames);
		if (nSlices==stackSize) hyperStack = false;
		if (nChannels*nSlices*nFrames!=stackSize) hyperStack = false;
		addMouseWheelListener(this);
		ImageJ ij = IJ.getInstance();
		if (nChannels>1) {
			cSelector = new ScrollbarWithLabel(this, 1, 1, 1, nChannels+1, 'c');
			add(cSelector);
			if (ij!=null) cSelector.addKeyListener(ij);
			cSelector.addAdjustmentListener(this);
			cSelector.setFocusable(false); // prevents scroll bar from blinking on Windows
			cSelector.setUnitIncrement(1);
			cSelector.setBlockIncrement(1);
		}
		if (nSlices>1) {
			char label = nChannels>1||nFrames>1?'z':'t';
			if (nSlices==dim[2]) label = 'c';
			zSelector = new ScrollbarWithLabel(this, 1, 1, 1, nSlices+1, label);
			if (label=='t') animationSelector = zSelector;
			add(zSelector);
			if (ij!=null) zSelector.addKeyListener(ij);
			zSelector.addAdjustmentListener(this);
			zSelector.setFocusable(false);
			int blockIncrement = nSlices/10;
			if (blockIncrement<1) blockIncrement = 1;
			zSelector.setUnitIncrement(1);
			zSelector.setBlockIncrement(blockIncrement);
			sliceSelector = zSelector.bar;
		}
		if (nFrames>1) {
			animationSelector = tSelector = new ScrollbarWithLabel(this, 1, 1, 1, nFrames+1, 't');
			add(tSelector);
			if (ij!=null) tSelector.addKeyListener(ij);
			tSelector.addAdjustmentListener(this);
			tSelector.setFocusable(false);
			int blockIncrement = nFrames/10;
			if (blockIncrement<1) blockIncrement = 1;
			tSelector.setUnitIncrement(1);
			tSelector.setBlockIncrement(blockIncrement);
		}
		if (sliceSelector==null && this.getClass().getName().indexOf("Image5D")!=-1)
			sliceSelector = new Scrollbar(); // prevents Image5D from crashing
		//IJ.log(nChannels+" "+nSlices+" "+nFrames);
		pack();
		ic = imp.getCanvas();
		if (ic!=null) ic.setMaxBounds();
		show();
		int previousSlice = imp.getCurrentSlice();
		if (previousSlice>1 && previousSlice<=stackSize)
			imp.setSlice(previousSlice);
		else
			imp.setSlice(1);
		thread = new Thread(this, "zSelector");
		thread.start();
	}

	@Override
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		if (!running2) {
			if (e.getSource()==cSelector) {
				c = cSelector.getValue();
				if (c==imp.getChannel()&&e.getAdjustmentType()==AdjustmentEvent.TRACK) return;
			} else if (e.getSource()==zSelector) {
				z = zSelector.getValue();
				if (z==imp.getSlice()&&e.getAdjustmentType()==AdjustmentEvent.TRACK) return;
			} else if (e.getSource()==tSelector) {
				t = tSelector.getValue();
				if (t==imp.getFrame()&&e.getAdjustmentType()==AdjustmentEvent.TRACK) return;
			}
			updatePosition();
			notify();
		}
	}
	
	void updatePosition() {
		slice = (t-1)*nChannels*nSlices + (z-1)*nChannels + c;
		imp.updatePosition(c, z, t);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}
	
	public static int min (int a, int b){
		if (a<b) return a; else return b;
	}
	
	public static int max (int a, int b){
		if (a>b) return a; else return b;
	}

	static boolean seenSmallRotation;
	
	@Override
	float findMax(float [] a){
		float max=0;
		for (int i=0;i<a.length;i++){
			if (a[i]>max) max=a[i];
		}
		return max;
	}

	@Override
	float findMax(short [] a){
		float max=0;
		for (int i=0;i<a.length;i++){
			if (((int)  (a[i]&0xffff) )>max) max=(float) (a[i]&0xffff);
		}
		return max;
	}
	
	@Override
	float findMax(byte [] a){
		float max=0;
		for (int i=0;i<a.length;i++){
			if (((int)  (a[i]&0xff) )>max) max=(float) (a[i]&0xff);
		}
		return max;
	}
	
	public void findMaxInStack(){
		float max=0;
		for (int i=1;i<=nSlices;i++){
			Object pixels=imp.getStack().getPixels((i-1)*nChannels+c);
			float sliceMax=0;
			if (pixels instanceof float [])
				sliceMax=findMax((float []) pixels);
			else if (pixels instanceof byte [])
				sliceMax=findMax((byte []) pixels);
			else if (pixels instanceof short [])
				sliceMax=findMax((short []) pixels);
			if (sliceMax>max) max=sliceMax;
		}
		double min=imp.getDisplayRangeMin();
		imp.setDisplayRange(min,max);
		imp.updateChannelAndDraw();
	}
	
	private long lastTimeScroll;
	private boolean lastScrollHorizontal;
	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		synchronized(this) {
			int rotation = event.getWheelRotation();
			if (!seenSmallRotation){
				if (Math.abs(rotation)<6) 
					seenSmallRotation=true;
				else 
					rotation=rotation/10;
			}
			
			int modifiers=event.getModifiers();
			//IJ.log("modifiers "+modifiers); 
			//java.awt.event.MouseEvent.SHIFT_DOWN_MASK
			
			boolean horizontalScroll=((event.getModifiers()&ActionEvent.SHIFT_MASK)==ActionEvent.SHIFT_MASK)
					|| ((event.getModifiers()&ActionEvent.META_MASK)==ActionEvent.META_MASK);
			//Shift key does not work over VNC, command key does
			
			if (System.currentTimeMillis()-lastTimeScroll<1000)
				if (lastScrollHorizontal ^ horizontalScroll)
					return;
			lastTimeScroll=System.currentTimeMillis();
			lastScrollHorizontal=horizontalScroll;
			
			if (horizontalScroll){
				double max=imp.getDisplayRangeMax();
				double min=imp.getDisplayRangeMin();
				double step;
				if (max!=0) 
					step=(Math.abs(max)/12.0d)*(rotation);
				else 
					step=1.0;
				int iteration=0;
				while (max>=0 && (max+step<0) && iteration++<1000){
					step/=2;
				}
				if ((max+step>0.001)&&Math.abs(max+step)<1.0e7){
					imp.setDisplayRange(min,max+step);
					if (Math.abs(imp.getDisplayRangeMax()-(max+step))>0.0001){
						if (Math.abs(step)<1){ // values are converted to byte or short by imp
							//if we don't take a sufficiently large step it won't have any effect
							if (step<0)
								step=-1;
							else step=1;
						}
						imp.setDisplayRange(min,max+step);
					}
				}
				imp.updateChannelAndDraw();
				IJ.showStatus("Set max to "+(max+step));
            } else if (hyperStack&&((event.getModifiers()&8) > 0 ||
                                    (event.getModifiers()&ActionEvent.ALT_MASK) > 0)) {
            	//8 seems to be the option key
                setPosition(min(max(1,c+rotation),nChannels),z,t);
                findMaxInStack();
			} else {
				if (hyperStack){
					setPosition(c,min(nSlices,max(z+rotation,1)),t);
					return;
				}
				int sliceIncrement=1;
				imp.setSlice(min(nSlices,max(1,imp.getCurrentSlice()+rotation*sliceIncrement)));
			}
		}
	}

	@Override
	public boolean close() {
		if (!super.close())
			return false;
		synchronized(this) {
			done = true;
			notify();
		}
        return true;
	}

	/** Displays the specified slice and updates the stack scrollbar. */
	public void showSlice(int index) {
		if (index>=1 && index<=imp.getStackSize())
			imp.setSlice(index);
	}
	
	/** Updates the stack scrollbar. */
	public void updateSliceSelector() {
		if (hyperStack) return;
		int stackSize = imp.getStackSize();
		int max = zSelector.getMaximum();
		if (max!=(stackSize+1))
			zSelector.setMaximum(stackSize+1);
		zSelector.setValue(imp.getCurrentSlice());
	}
	
	@Override
	public void run() {
		while (!done) {
			synchronized(this) {
				try {wait();}
				catch(InterruptedException e) {}
			}
			if (done) return;
			if (slice>0) {
				int s = slice;
				slice = 0;
				if (s!=imp.getCurrentSlice())
					imp.setSlice(s);
			}
		}
	}
	
	@Override
	public String createSubtitle() {
		String subtitle = super.createSubtitle();
		if (!hyperStack) return subtitle;
    	String s="";
    	int[] dim = imp.getDimensions();
    	int channels=dim[2], slices=dim[3], frames=dim[4];
		if (channels>1) {
			s += "c:"+imp.getChannel()+"/"+channels;
			if (slices>1||frames>1) s += " ";
		}
		if (slices>1) {
			s += "z:"+imp.getSlice()+"/"+slices;
			if (frames>1) s += " ";
		}
		if (frames>1)
			s += "t:"+imp.getFrame()+"/"+frames;
		if (running2) return s;
		int index = subtitle.indexOf(";");
		if (index!=-1) {
			int index2 = subtitle.indexOf("(");
			if (index2>=0 && index2<index && subtitle.length()>index2+4 && !subtitle.substring(index2+1, index2+4).equals("ch:")) {
				index = index2;
				s = s + " ";
			}
			subtitle = subtitle.substring(index, subtitle.length());
		} else
			subtitle = "";
    	return s + subtitle;
    }
    
    public boolean isHyperStack() {
    	return hyperStack;
    }
    
    public void setPosition(int channel, int slice, int frame) {
    	if (cSelector!=null && channel!=c) {
    		c = channel;
			cSelector.setValue(channel);
		}
    	if (zSelector!=null && slice!=z) {
    		z = slice;
			zSelector.setValue(slice);
		}
    	if (tSelector!=null && frame!=t) {
    		t = frame;
			tSelector.setValue(frame);
		}
    	updatePosition();
		if (this.slice>0) {
			int s = this.slice;
			this.slice = 0;
			if (s!=imp.getCurrentSlice())
				imp.setSlice(s);
		}
    }
    
    public boolean validDimensions() {
    	int c = imp.getNChannels();
    	int z = imp.getNSlices();
    	int t = imp.getNFrames();
    	if (c!=nChannels||z!=nSlices||t!=nFrames||c*z*t!=imp.getStackSize())
    		return false;
    	else
    		return true;
    }
    
    public void setAnimate(boolean b) {
    	if (running2!=b && animationSelector!=null)
    		animationSelector.updatePlayPauseIcon();
		running2 = b;
    }
    
    public boolean getAnimate() {
    	return running2;
    }

	public void incrementZ(int i) {
		if (isHyperStack()){
			setPosition(c,min(nSlices,max(z+i,1)),t);
			return;
		}
		int sliceIncrement=1;
		imp.setSlice(min(nSlices,max(1,imp.getCurrentSlice()+i*sliceIncrement)));
	}
	
}
