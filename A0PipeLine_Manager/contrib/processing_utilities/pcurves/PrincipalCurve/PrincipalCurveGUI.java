package processing_utilities.pcurves.PrincipalCurve;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Window;
import java.io.File;
import java.util.Date;

import processing_utilities.pcurves.AWT.ColorChoice;
import processing_utilities.pcurves.AWT.ScrollbarTextFieldPanel;
import processing_utilities.pcurves.Debug.Debug;
import processing_utilities.pcurves.Debug.DebugEvent;
import processing_utilities.pcurves.Internet.DownLoadDialog;
import processing_utilities.pcurves.Internet.DownLoadFile;
import processing_utilities.pcurves.LinearAlgebra.Curve;
import processing_utilities.pcurves.LinearAlgebra.Loadable;
import processing_utilities.pcurves.LinearAlgebra.Sample;
import processing_utilities.pcurves.LinearAlgebra.SampleDD;
import processing_utilities.pcurves.LinearAlgebra.SampleDDWeighted;
import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.LinearAlgebra.VektorDDWeighted;
import processing_utilities.pcurves.Optimize.Optimizer;
import processing_utilities.pcurves.Paintable.ProjectionPlane;
import processing_utilities.pcurves.Paintable.ProjectionPlaneSettingDialog;
import processing_utilities.pcurves.Paintable.Recallable;

final public class PrincipalCurveGUI extends Frame implements Recallable {

	private static final long serialVersionUID = 1L;
	final String logDirectory = "PrincipalCurveLogs/";
	final String outputDirectory = "PrincipalCurveOutputs/";
	// Internet.UpLoadFile logFile = new Internet.UpLoadFile(logDirectory + "log");
	private processing_utilities.pcurves.Internet.UpLoadFile logFile = null;
	private final String sampleFileName = "sample.dta";
	private final String weightedSampleFileName = "wsample.dta";
	private final String pcurveFileName = "pcurve.dta";
	final String projectionsFileName = "project.dta";
	private final String gencurveFileName = "gencurve.dta";
	private final String hscurveFileName = "hscurve.dta";
	private final String brcurveFileName = "brcurve.dta";
	final String projectionsInOrderFileName = "project2.dta";
	final String projectionIndicesFileName = "indices.dta";

	// Dialogs
	private FileDialog loadDialog = null;
	private FileDialog saveDialog = null;
	DownLoadDialog downLoadDialog = null;
	DownLoadDialog emailDialog = null;
	int numOfEmails = -1;
	private InitCurveDialog initCurveDialog = null;
	private ParametersDialog parametersDialog = null;
	private ProjectionPlaneSettingDialog projectionPlaneDialog = null;
	private DiagnosisDialog diagnosisDialog = null;

	private PrincipalCurveCanvas canvas = new PrincipalCurveCanvas();

	// Principal Curve Members
	private Sample sample;
	private Curve generatorCurve;
	private PrincipalCurveClass principalCurve;
	private Curve hsCurve;
	private Curve brCurve;

	private Loadable[] loadableData = new Loadable[5]; // {sample,generatorCurve,principalCurve,hsCurve,brCurve};

	// Menus
	private MenuBar menuBar = new MenuBar();
	private MenuItem separator = new MenuItem("-");

	private Menu sampleMenu = new Menu("Sample");
	private Menu builtInSamplesMenu = new Menu("Built in samples");
	private MenuItem halfCircleMenuItem = new MenuItem("Half circle");
	private MenuItem circleMenuItem = new MenuItem("Circle");
	private MenuItem sShapeMenuItem = new MenuItem("S-shape, small noise");
	private MenuItem circleSmallNoiseMenuItem = new MenuItem("Circle, small noise");
	private MenuItem distortedHalfCircleMenuItem = new MenuItem("Distorted half circle");
	private MenuItem distortedSShapeMenuItem = new MenuItem("Distorted S-shape");
	private MenuItem spiralMenuItem = new MenuItem("Spiral");
	private MenuItem zigzagMenuItem = new MenuItem("Zigzag");
	private MenuItem loadMenuItem = new MenuItem("Load data...");
	private MenuItem downLoadMenuItem = new MenuItem("Download data...");
	private MenuItem saveMenuItem = new MenuItem("Save data...");
	MenuItem emailMenuItem = new MenuItem("Email data...");
	private MenuItem saveImageMenuItem = new MenuItem("Save image...");
	private MenuItem quitMenuItem = new MenuItem("Quit");

	private Menu principalCurveMenu = new Menu("Principal curve");
	private MenuItem parametersMenuItem = new MenuItem("Parameters...");
	private MenuItem initCurveMenuItem = new MenuItem("Initialize curve...");
	private MenuItem diagnosisMenuItem = new MenuItem("Diagnosis...");

	private Menu appearanceMenu = new Menu("Appearance");
	private MenuItem packMenuItem = new MenuItem("Pack");
	private CheckboxMenuItem paintProjectionPointsMenuItem = new CheckboxMenuItem("Connect to projections");
	private MenuItem projectionPlaneMenuItem = new MenuItem("Projection plane...");
	private MenuItem colorsAndSizesMenuItem = new MenuItem("Colors & sizes...");

	private Menu[] menuBarElements = { sampleMenu, principalCurveMenu, appearanceMenu };

	private Menu[] menus = { sampleMenu, builtInSamplesMenu, principalCurveMenu, appearanceMenu };

	private MenuItem[][] menuItems = {
			{ builtInSamplesMenu, loadMenuItem, downLoadMenuItem, saveMenuItem,
					// emailMenuItem,
					saveImageMenuItem, separator, quitMenuItem },

			{ halfCircleMenuItem, circleMenuItem, sShapeMenuItem, circleSmallNoiseMenuItem,
					distortedHalfCircleMenuItem, distortedSShapeMenuItem, spiralMenuItem, zigzagMenuItem },

			{ parametersMenuItem, initCurveMenuItem, diagnosisMenuItem },

			{ packMenuItem, paintProjectionPointsMenuItem, projectionPlaneMenuItem, colorsAndSizesMenuItem } };

	private PrincipalCurveParameters principalCurveParameters;

	private void InitializeMenuBar() {
		paintProjectionPointsMenuItem.setState(principalCurveParameters.paintProjectionPoints);
		setMenuBar(menuBar);
		for (Menu menuBarElement : menuBarElements)
			menuBar.add(menuBarElement);
		for (int i = 0; i < menus.length; i++) {
			for (int j = 0; j < menuItems[i].length; j++) {
				if (shouldAddComponent(menuItems[i][j])) {
					menus[i].add(menuItems[i][j]);
				}
			}
		}
	}

	// Buttons
	private Button initButton = new Button("Init");
	private Button innerStepButton = new Button("Inner step");
	private Button outerStepButton = new Button("Outer step");
	private Button addOneVertexAsMidpointButton = new Button("Add vertex");
	private Button startButton = new Button("Start");
	private Button stopButton = new Button("Stop");
	private Button continueButton = new Button("Continue");

	Button[] buttons = { initButton, innerStepButton, outerStepButton, addOneVertexAsMidpointButton, startButton,
			stopButton, continueButton };

	// Text fields
	private TextField diagnosisTextField = new TextField();

	private TextField[] textFields = { diagnosisTextField };
	private boolean[] textFieldEditables = { false };

	private void InitializeTextFields() {
		for (int i = 0; i < textFields.length; i++) {
			textFields[i].setEditable(textFieldEditables[i]);
		}
	}

	// Gridlayout panels
	private Panel canvasPanel = new Panel();
	private Panel algorithmManagingPanel = new Panel();
	private Panel messagePanel = new Panel();

	private Panel[] gridPanels = { canvasPanel, algorithmManagingPanel, messagePanel };

	private Component[][] gridPanelComponents = {
			{ canvas },

			{ startButton, stopButton, continueButton, initButton, innerStepButton, outerStepButton,
					addOneVertexAsMidpointButton },

			{ diagnosisTextField } };

	private int[][] gridPanelLayoutDimensions = { { 1, 1 }, { 0, 1 }, { 1, 0 } };

	private void InitializeGridPanels() {
		for (int i = 0; i < gridPanels.length; i++)
			InitializeGridPanel(i);
	}

	private void InitializeGridPanel(int i) {
		gridPanels[i].setLayout(new GridLayout(gridPanelLayoutDimensions[i][0], gridPanelLayoutDimensions[i][1]));
		for (int j = 0; j < gridPanelComponents[i].length; j++) {
			if (shouldAddComponent(gridPanelComponents[i][j]))
				gridPanels[i].add(gridPanelComponents[i][j]);
		}
	}

	// The main panel (borderlayout)
	private Component[] mainPanels = { canvasPanel, algorithmManagingPanel, messagePanel };

	private String[] mainPanelPositions = { "Center", "East", "South" };

	private void InitializeMainPanel() {
		setLayout(new BorderLayout());
		for (int i = 0; i < mainPanels.length; i++) {
			if (shouldAddComponent(mainPanels[i])) {
				add(mainPanelPositions[i], mainPanels[i]);
			}
		}
	}

	private void InitializePanels() {
		InitializeMainPanel();
		InitializeGridPanels();
	}

	// The components that should not appear if we are in applets
	private Object[] nonAppletComponents = { loadMenuItem, saveMenuItem, saveImageMenuItem, initCurveMenuItem };

	boolean shouldAddComponent(Object component) {
		if (processing_utilities.pcurves.Utilities.Environment.inApplet)
			for (Object nonAppletComponent : nonAppletComponents)
				if (component == nonAppletComponent)
					return false;
		return true;
	}

	// Samples to download
	private final String URL_SAMPLE_DIRECTORY = processing_utilities.pcurves.Utilities.Environment.homeURL
			+ "/research/pcurves/implementations/Samples/";
	private final String[] SAMPLE_DIRECTORIES = { "HalfCircle/", "Circle/", "SmallNoise/", "CircleSmallNoise/",
			"DistortedHalfCircle/", "DistortedSShape/", "Spiral/", "Zigzag/" };
	private long timeStart;
	@SuppressWarnings("unused")
	private int xPosition;
	@SuppressWarnings("unused")
	private int yPosition;

	private PrincipalCurveAlgorithmThread algorithmThread;

	private boolean onlyInitialize;
	private boolean onlyInnerStep;
	private boolean onlyAddOneVertexAsMidpoint;
	private boolean onlyOuterStep;

	private void InitializeCanvas() {
		canvas.SetObjectToPaint(canvas.sample, sample);
		canvas.SetObjectToPaint(canvas.generatorCurve, generatorCurve);
		canvas.SetObjectToPaint(canvas.principalCurve, principalCurve);
		canvas.SetObjectToPaint(canvas.principalCurvePoints, principalCurve);
		canvas.SetObjectToPaint(canvas.hsCurve, hsCurve);
		canvas.SetObjectToPaint(canvas.brCurve, brCurve);
	}

	private PrincipalCurveGUI(PrincipalCurveParameters principalCurveParameters) {
		this.principalCurveParameters = principalCurveParameters;
		Initialize();
	}

	private long zeroTime = (new Date()).getTime();

	void Initialize() {
		setTitle("Principal Curves");
		if (logFile != null && processing_utilities.pcurves.Utilities.Environment.inApplet) {
			try {
				logFile.SetAction(processing_utilities.pcurves.Internet.UpLoadFile.ACTION_OPEN);
				logFile.AppendString((new Date()).toString() + "\n");
			} catch (RuntimeException e) {
				logFile = null;
			}
		}
		xPosition = 0;
		yPosition = 0;

		InitializeTextFields();
		InitializePanels();
		InitializeCanvas();
		InitializeMenuBar();

		Debug.diagnosisTextComponent = diagnosisTextField;
		principalCurveParameters.diagnosisTextArea = new TextArea(20, 80);

		onlyInitialize = false;
		onlyInnerStep = false;
		onlyAddOneVertexAsMidpoint = false;
		onlyOuterStep = false;

		DownLoad(URL_SAMPLE_DIRECTORY + SAMPLE_DIRECTORIES[0]);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean action(Event event, Object arg) {
		if (logFile != null && processing_utilities.pcurves.Utilities.Environment.inApplet) {
			logFile.AppendString((((new Date()).getTime() - zeroTime) / 1000.0) + ":\t" + arg.toString() + "\n");
		}
		Object target = event.target;

		// Load files
		if (target == loadMenuItem) {
			if (loadDialog == null) {
				loadDialog = new FileDialog(this, "Dialog box");
				if (saveDialog == null)
					loadDialog.setDirectory(processing_utilities.pcurves.Utilities.Environment.homeDirectory
							+ "/Research/PrincipalCurves/Experiments/");
				else
					loadDialog.setDirectory(saveDialog.getDirectory());
			}
			loadDialog.show();
			if (loadDialog.getFile() != null) {
				// Load(loadDialog.getDirectory());
				String directory = loadDialog.getDirectory();

				loadableData[0] = new SampleDD();
				File f = new File(directory + sampleFileName);
				System.out.println(directory + sampleFileName);

				try {
					loadableData[0].Load(f);
				} catch (Throwable exception) {
				} // THIS LOADS FILE FROM TEXT

				/*
				 * StringTokenizer t = new StringTokenizer("1 2 3");
				 * StringTokenizer t1 = new StringTokenizer("2 3 4");
				 * ((SampleDD)loadableData[0]).AddPoint(t);
				 * ((SampleDD)loadableData[0]).AddPoint(t1);
				 */

				// We make sure that there is always a valid sample loaded
				if (((SampleDD) loadableData[0]).getSize() >= 2) {
					sample = (SampleDD) loadableData[0];
					canvas.projectionPlane = new ProjectionPlane(sample);
					canvas.SetObjectToPaint(canvas.sample, sample);
					if (sample.getSize() == 0)
						canvas.ResetBorders();
					else
						canvas.ResetBorders(sample);

					loadableData[1] = new SampleDD();

					principalCurve = new PrincipalCurveClass(sample, new PrincipalCurveParameters());
					loadableData[2] = new SetOfCurves();

					NewPrincipalCurve();

					loadableData[3] = new SampleDD();

					loadableData[4] = new SampleDD();

					NewSample();

					algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_START);

					Optimizer optimizer = new Optimizer(principalCurve);
					double criterionBefore = principalCurve.GetCriterion();
					double criterionAfter =
							optimizer.Optimize(principalCurveParameters.relativeChangeInCriterionThreshold, 1);
					while (true) {
						if (!(Math.abs((criterionBefore - criterionAfter) / criterionBefore) >= principalCurveParameters.relativeChangeInCriterionThreshold)) {
							break;
						}
					}

					SetButtonsStartedState();

					// have to wait for thread to finish
					long t0, t1;
					t0 = System.currentTimeMillis();
					do {
						t1 = System.currentTimeMillis();
					} while (t1 - t0 < 3 * 1000);

					// save
					sample.Save("/Users/segmenter/Desktop/mydata2/results/sample.dta");
					generatorCurve.Save("/Users/segmenter/Desktop/mydata2/results/gencurve.dta");

					SetOfCurves savePrincipalCurve = principalCurve.ConvertToCurves();
					if (savePrincipalCurve != null) {
						System.out.println("3");

						savePrincipalCurve.Save("/Users/segmenter/Desktop/mydata2/results/pcurve.dta");
						principalCurve.GetProjections().Save("/Users/segmenter/Desktop/mydata2/results/project.dta");
						try {
							principalCurve.GetProjectionsInOrder().Save(
									"/Users/segmenter/Desktop/mydata2/results/project2.dta");
							principalCurve.GetProjectionIndices().Save(
									"/Users/segmenter/Desktop/mydata2/results/indices.dta");
						} catch (RuntimeException e) {
						}
					}

				}

			}

		}

		// Save files
		if (target == saveMenuItem) {

			sample.Save("/Users/segmenter/Desktop/mydata2/results/sample.dta");
			generatorCurve.Save("/Users/segmenter/Desktop/mydata2/results/gencurve.dta");

			SetOfCurves savePrincipalCurve = principalCurve.ConvertToCurves();
			if (savePrincipalCurve != null) {
				System.out.println("3");

				savePrincipalCurve.Save("/Users/segmenter/Desktop/mydata2/results/pcurve.dta");
				principalCurve.GetProjections().Save("/Users/segmenter/Desktop/mydata2/results/project.dta");
				try {
					principalCurve.GetProjectionsInOrder()
							.Save("/Users/segmenter/Desktop/mydata2/results/project2.dta");
					principalCurve.GetProjectionIndices().Save("/Users/segmenter/Desktop/mydata2/results/indices.dta");
				} catch (RuntimeException e) {
				}
			}

			/*
			 * if (saveDialog == null) {
			 * saveDialog = new FileDialog(this,"Choose a data file",FileDialog.SAVE);
			 * if (loadDialog == null)
			 * saveDialog.setDirectory("/tmp/");
			 * else
			 * saveDialog.setDirectory(loadDialog.getDirectory());
			 * }
			 * saveDialog.show();
			 * if (saveDialog.getFile() != null) {
			 * if (sample instanceof SampleDDWeighted){
			 * System.out.println("1");
			 * sample.Save(saveDialog.getDirectory() + weightedSampleFileName);
			 * }else{
			 * System.out.println("2");
			 * sample.Save(saveDialog.getDirectory() + sampleFileName);
			 * }
			 * if (generatorCurve.GetSize() > 0)
			 * generatorCurve.Save(saveDialog.getDirectory() + gencurveFileName);
			 * if (principalCurve.GetSize() > 0) {
			 * SetOfCurves savePrincipalCurve = principalCurve.ConvertToCurves();
			 * if (savePrincipalCurve != null) {
			 * savePrincipalCurve.Save(saveDialog.getDirectory() + pcurveFileName);
			 * principalCurve.GetProjections().Save(saveDialog.getDirectory() + projectionsFileName);
			 * try {
			 * principalCurve.GetProjectionsInOrder().Save(saveDialog.getDirectory() + projectionsInOrderFileName);
			 * principalCurve.GetProjectionIndices().Save(saveDialog.getDirectory() + projectionIndicesFileName);
			 * }
			 * catch(RuntimeException e) {}
			 * }
			 * }
			 * }
			 */
		}

		// Save image
		else if (target == saveImageMenuItem) {
			canvas.SaveImage(this);
		}
		// Set algorithm parameters
		else if (target == parametersMenuItem) {
			if (parametersDialog == null) {
				parametersDialog = new ParametersDialog(this);
			}
			parametersDialog.show();
		}
		// Initialize curve
		else if (target == initCurveMenuItem) {
			initCurveDialog = new InitCurveDialog(this, canvas);
			initCurveDialog.show();
		}
		// Diagnosis window
		else if (target == diagnosisMenuItem) {
			if (diagnosisDialog == null) {
				diagnosisDialog = new DiagnosisDialog(this);
				diagnosisDialog.show();
			}
		}
		// Start algorithm
		else if (target == startButton) {
			algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_START);
			SetButtonsStartedState();
		}
		// Continue algorithm
		else if (target == continueButton) {
			algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_CONTINUE);
			SetButtonsStartedState();
		}
		// Stop algorithm
		else if (target == stopButton) {
			stopButton.disable();
			algorithmThread.Stop();
		}
		// Initizalize principal curve
		else if (target == initButton) {
			onlyInitialize = true;
			SetButtonsStartedState();
			algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_INIT);
		}
		// Do one inner loop (Repartition Voronoi-regions and optimize vertices)
		else if (target == innerStepButton) {
			onlyInnerStep = true;
			SetButtonsStartedState();
			algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_INNER_STEP);
		}
		// Do one outer loop (do inner loops until the realtive change in MSE is small)
		else if (target == outerStepButton) {
			onlyOuterStep = true;
			SetButtonsStartedState();
			algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_OUTER_STEP);
		}
		// Add a vertex as the midpoint of the line segment that has the most projection points/longest line segment
		else if (target == addOneVertexAsMidpointButton) {
			onlyAddOneVertexAsMidpoint = true;
			SetButtonsStartedState();
			algorithmThread.SetAction(PrincipalCurveAlgorithmThread.ACTION_ADD_VERTEX_AS_ONE_MIDPOINT);
		}
		// Setting data colors, sizes, and types
		else if (target == colorsAndSizesMenuItem) {
			Dialog dialog = canvas.GetCustomizingDialog(this);
			dialog.show();
		}
		// Pack center canvas
		else if (target == packMenuItem) {
			canvas.Pack();
			pack();
		}
		// Connecting points to their projections
		else if (target == paintProjectionPointsMenuItem) {
			principalCurveParameters.paintProjectionPoints = paintProjectionPointsMenuItem.getState();
			canvas.Repaint();
		}
		// Setting projection plane
		else if (target == projectionPlaneMenuItem) {
			if (projectionPlaneDialog == null)
				projectionPlaneDialog = new ProjectionPlaneSettingDialog(this, canvas.projectionPlane, this);
			projectionPlaneDialog.show();
		}
		// Quit
		else if (target == quitMenuItem) {
			postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
		}
		// Select built in sample
		else {
			for (int i = 0; i < SAMPLE_DIRECTORIES.length; i++) {
				if (target == builtInSamplesMenu.getItem(i)) {
					DownLoad(URL_SAMPLE_DIRECTORY + SAMPLE_DIRECTORIES[i]);
				}
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean handleEvent(Event event) {
		// System.out.println(event);
		// Quit
		if (event.id == Event.WINDOW_DESTROY) {
			if (processing_utilities.pcurves.Utilities.Environment.inApplet) {
				dispose();
			} else {
				System.exit(0);
			}
		}
		// Debug event
		else if (event.target == diagnosisTextField && event.arg != null) {
			algorithmThread.ReleaseStep();
			if (event.id == DebugEvent.FINISHED_DEBUG_THREAD) {
				if (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_INITIALIZE)) {
					canvas.repaint();
					if (onlyInitialize) {
						SetButtons();
						onlyInitialize = false;
					}
				} else if (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_OPTIMIZE_VERTICES)) {
					canvas.repaint();
					if (onlyInnerStep) {
						SetButtons();
						onlyInnerStep = false;
					}
				} else if (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_ADD_VERTEX_AS_ONE_MIDPOINT)) {
					canvas.repaint();
					if (onlyAddOneVertexAsMidpoint) {
						SetButtons();
						onlyAddOneVertexAsMidpoint = false;
					}
					addOneVertexAsMidpointButton.disable();
				} else if (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_INNER_ITERATION)) {
					if (onlyOuterStep) {
						SetButtons();
						onlyOuterStep = false;
					}
				} else if (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_OUTER_ITERATION)) {
					SetButtons();
					System.out.println("running time = " + (double) (System.currentTimeMillis() - timeStart) / 1000
							+ " sec");
				}
			} else if (event.id == DebugEvent.STARTED_DEBUG_THREAD
					&& (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_INNER_ITERATION) || event.arg
							.equals(PrincipalCurveAlgorithm.DEBUG_OUTER_ITERATION))) {
				stopButton.enable();
			} else if (event.id == DebugEvent.ITERATE_DEBUG_THREAD) {
				if (event.arg.equals(PrincipalCurveAlgorithm.DEBUG_OPTIMIZE_VERTICES)) {
					canvas.repaint();
				}
			}
		}

		return super.handleEvent(event);
	}

	private boolean DownLoadData(int index, String debugString, String fileName) {
		diagnosisTextField.setText(debugString);
		try {
			DownLoadFile downLoadFile = new DownLoadFile(fileName);
			loadableData[index].Load(downLoadFile.GetDataInputStream());
		} catch (Throwable exception) {
			diagnosisTextField.setText("");
			return false;
		}
		diagnosisTextField.setText("");
		return true;
	}

	private void DownLoad(String directory) {
		directory += "/";
		loadableData[0] = new SampleDD();
		if (!DownLoadData(0, "Downloading sample...", directory + sampleFileName)) {
			loadableData[0] = new SampleDDWeighted();
			DownLoadData(0, "Downloading weighted sample...", directory + weightedSampleFileName);
		}
		// We make sure that there is always a valid sample loaded
		if (((SampleDD) loadableData[0]).getSize() >= 2) {
			sample = (SampleDD) loadableData[0];
			canvas.projectionPlane = new ProjectionPlane(sample);
			canvas.SetObjectToPaint(canvas.sample, sample);
			if (sample.getSize() == 0)
				canvas.ResetBorders();
			else
				canvas.ResetBorders(sample);

			loadableData[1] = new SampleDD();
			DownLoadData(1, "Downloading generating curve...", directory + gencurveFileName);
			generatorCurve = new Curve((SampleDD) loadableData[1]);
			canvas.SetObjectToPaint(canvas.generatorCurve, generatorCurve);

			principalCurve = new PrincipalCurveClass(sample, new PrincipalCurveParameters());
			loadableData[2] = new SetOfCurves();
			if (DownLoadData(2, "Downloading principal curve...", directory + pcurveFileName)
					&& ((SetOfCurves) loadableData[2]).Valid()) {
				principalCurve.InitializeToCurves((SetOfCurves) loadableData[2], 0);
			}
			NewPrincipalCurve();

			loadableData[3] = new SampleDD();
			DownLoadData(3, "Downloading Hastie-Stuetzle curve...", directory + hscurveFileName);
			hsCurve = new Curve((SampleDD) loadableData[3]);
			canvas.SetObjectToPaint(canvas.hsCurve, hsCurve);

			loadableData[4] = new SampleDD();
			DownLoadData(4, "Downloading Banfield-Raftery curve...", directory + brcurveFileName);
			brCurve = new Curve((SampleDD) loadableData[4]);
			canvas.SetObjectToPaint(canvas.brCurve, brCurve);
			NewSample();
		}
	}

	private void NewSample() {
		SetButtons();
		SetTerminatingConditionMaxLength();
		SetProjectionPlaneDialog();
	}

	void NewPrincipalCurve() {
		SetButtons();
		canvas.SetObjectToPaint(canvas.principalCurve, principalCurve);
		canvas.SetObjectToPaint(canvas.principalCurvePoints, principalCurve);
		algorithmThread = new PrincipalCurveAlgorithmThread(principalCurve, principalCurveParameters);
	}

	private void SetTerminatingConditionMaxLength() {
		principalCurveParameters.terminatingConditionMaxLength = canvas.GetCanvasSize() / 10;
		/*
		 * if (parametersDialog != null)
		 * parametersDialog.SetTerminatingConditionMaxLength();
		 */
	}

	private void SetProjectionPlaneDialog() {
		/*
		 * if (projectionPlaneDialog != null) {
		 * projectionPlaneDialog.dispose();
		 * projectionPlaneDialog = null;
		 * }
		 */
	}

	@SuppressWarnings("deprecation")
	private void SetButtons() {
		sampleMenu.enable();
		initCurveMenuItem.enable();
		if (principalCurve.getSize() >= 2) {
			// "Paused" state
			startButton.enable();
			stopButton.disable();
			continueButton.enable();
			initButton.enable();
			innerStepButton.enable();
			outerStepButton.enable();
			addOneVertexAsMidpointButton.enable();
		} else {
			// "Loaded" state
			startButton.enable();
			stopButton.disable();
			continueButton.disable();
			initButton.enable();
			innerStepButton.disable();
			outerStepButton.disable();
			addOneVertexAsMidpointButton.disable();
		}
	}

	@SuppressWarnings("deprecation")
	private void SetButtonsStartedState() {
		timeStart = System.currentTimeMillis();
		startButton.disable();
		continueButton.disable();
		stopButton.disable();
		initButton.disable();
		innerStepButton.disable();
		outerStepButton.disable();
		addOneVertexAsMidpointButton.disable();
		sampleMenu.disable();
		initCurveMenuItem.disable();
	}

	@SuppressWarnings("deprecation")
	void SetPosition() {
		move(0, 50);
		pack();
	}

	@Override
	public void Recall() {
		canvas.ResetBorders(sample);
		canvas.Repaint();
	}

	@SuppressWarnings("deprecation")
	public static void main(String args[]) {
		processing_utilities.pcurves.Utilities.Environment.inApplet = false;
		// Utilities.Environment.cRoutines = true; // comment it out if you can't compile the c functions
		VektorDDWeighted.maxWeight = 1; // for shading the color

		PrincipalCurveGUI window = new PrincipalCurveGUI(new PrincipalCurveParameters());
		window.SetPosition();
		window.show();
	}

	class ParametersDialog extends Dialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Panel panel;
		ScrollbarTextFieldPanel penaltyCoefficientPanel;
		ScrollbarTextFieldPanel relativeLengthPenaltyCoefficientPanel;
		ScrollbarTextFieldPanel terminatingConditionCoefficientPanel;
		ScrollbarTextFieldPanel terminatingConditionMaxLengthPanel;
		ScrollbarTextFieldPanel relativeChangeInCriterionThresholdPanel;
		Choice addVertexModeChoice;
		Button doneButton;

		final int PENALTY_COEFFICIENT_FACTOR = 1000;
		final int RELATIVE_LENGTH_PENALTY_COEFFICIENT_FACTOR = 100;
		final int TERMINATING_CONDITION_COEFFICIENT_FACTOR = 1000;
		final int TERMINATING_CONDITION_MAX_LENGTH_FACTOR = 1000;
		final int RELATIVE_CHANGE_FACTOR = 100000;

		@SuppressWarnings("deprecation")
		public ParametersDialog(Frame frame) {
			super(frame, "Parameters", false);
			setLayout(new BorderLayout());
			doneButton = new Button("Done");
			add("East", doneButton);
			panel = new Panel();
			panel.setLayout(new GridLayout(0, 1));
			penaltyCoefficientPanel =
					new ScrollbarTextFieldPanel("penalty coefficient: ",
							(int) (principalCurveParameters.penaltyCoefficient * PENALTY_COEFFICIENT_FACTOR), 0, 1000,
							true, PENALTY_COEFFICIENT_FACTOR);
			panel.add(penaltyCoefficientPanel);
			relativeLengthPenaltyCoefficientPanel =
					new ScrollbarTextFieldPanel(
							"length penalty coefficient at end segments: ",
							(int) (principalCurveParameters.relativeLengthPenaltyCoefficient * RELATIVE_LENGTH_PENALTY_COEFFICIENT_FACTOR),
							0, 1000, true, RELATIVE_LENGTH_PENALTY_COEFFICIENT_FACTOR);
			panel.add(relativeLengthPenaltyCoefficientPanel);
			terminatingConditionCoefficientPanel =
					new ScrollbarTextFieldPanel(
							"terminating condition coefficient: ",
							(int) (principalCurveParameters.terminatingConditionCoefficient * TERMINATING_CONDITION_COEFFICIENT_FACTOR),
							0, 100000, true, TERMINATING_CONDITION_COEFFICIENT_FACTOR);
			panel.add(terminatingConditionCoefficientPanel);
			terminatingConditionMaxLengthPanel =
					new ScrollbarTextFieldPanel(
							"maximum segment length: ",
							(int) (principalCurveParameters.terminatingConditionMaxLength * TERMINATING_CONDITION_MAX_LENGTH_FACTOR),
							0, 10000, true, TERMINATING_CONDITION_MAX_LENGTH_FACTOR);
			panel.add(terminatingConditionMaxLengthPanel);
			relativeChangeInCriterionThresholdPanel =
					new ScrollbarTextFieldPanel(
							"optimization threshold: ",
							(int) (principalCurveParameters.relativeChangeInCriterionThreshold * RELATIVE_CHANGE_FACTOR),
							0, 10000, true, RELATIVE_CHANGE_FACTOR);
			panel.add(relativeChangeInCriterionThresholdPanel);
			addVertexModeChoice = new Choice();
			addVertexModeChoice.addItem("Add one vertex");
			addVertexModeChoice.addItem("Add midpoints to all segments");
			addVertexModeChoice.addItem("Add midpoint to longest segment");
			addVertexModeChoice.select(0);
			terminatingConditionMaxLengthPanel.disable();
			add("West", panel);
			panel.add(addVertexModeChoice);
			validate();
			pack();
		}

		final public void SetTerminatingConditionMaxLength() {
			terminatingConditionMaxLengthPanel
					.setValue((int) (TERMINATING_CONDITION_MAX_LENGTH_FACTOR * principalCurveParameters.terminatingConditionMaxLength));
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean action(Event event, Object arg) {
			// Done
			if (event.target == doneButton) {
				postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
			}
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean handleEvent(Event event) {
			// Done
			if (event.id == Event.WINDOW_DESTROY) {
				dispose();
			} else if (event.target == penaltyCoefficientPanel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
				principalCurveParameters.penaltyCoefficient = (Integer) event.arg / (double) PENALTY_COEFFICIENT_FACTOR;
			} else if (event.target == relativeLengthPenaltyCoefficientPanel
					&& ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
				principalCurveParameters.relativeLengthPenaltyCoefficient =
						(Integer) event.arg / (double) RELATIVE_LENGTH_PENALTY_COEFFICIENT_FACTOR;
			} else if (event.target == terminatingConditionCoefficientPanel
					&& ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
				principalCurveParameters.terminatingConditionCoefficient =
						(Integer) event.arg / (double) TERMINATING_CONDITION_COEFFICIENT_FACTOR;
			} else if (event.target == terminatingConditionMaxLengthPanel
					&& ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
				principalCurveParameters.terminatingConditionMaxLength =
						(Integer) event.arg / (double) TERMINATING_CONDITION_MAX_LENGTH_FACTOR;
			} else if (event.target == relativeChangeInCriterionThresholdPanel
					&& ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
				principalCurveParameters.relativeChangeInCriterionThreshold =
						(Integer) event.arg / (double) RELATIVE_CHANGE_FACTOR;
			} else if (event.target == addVertexModeChoice) {
				principalCurveParameters.addVertexMode = addVertexModeChoice.getSelectedIndex();
				if (principalCurveParameters.addVertexMode == PrincipalCurveParameters.ADD_ONE_VERTEX
						|| principalCurveParameters.addVertexMode == PrincipalCurveParameters.ADD_VERTICES) {
					terminatingConditionMaxLengthPanel.disable();
					terminatingConditionCoefficientPanel.enable();
				} else if (principalCurveParameters.addVertexMode == PrincipalCurveParameters.ADD_ONE_VERTEX_TO_LONGEST) {
					terminatingConditionCoefficientPanel.disable();
					terminatingConditionMaxLengthPanel.enable();
				}
			}
			return super.handleEvent(event);
		}
	}

	class InitCurveDialog extends Dialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		PrincipalCurveGUI parent;
		Panel southPanel;
		Button doneButton;
		PrincipalCurveCanvas canvas1; // ! FIXME : This was renamed from canvas to canvas1 in order to fix error message
										// "hiding type from PrincipalCurveGUI". This might do something crazy, but we
										// don't use PrinicpalCurveGUI for germline straightening,
		Window balloon;
		TextField balloonTextField;
		Checkbox balloonCheckbox;

		SetOfCurves initialCurves;
		boolean startedDrawingInitialCurves;

		public InitCurveDialog(PrincipalCurveGUI frame, PrincipalCurveCanvas in_canvas) {
			super(frame, "Initialize principal curve", true);
			parent = frame;
			setLayout(new BorderLayout());
			southPanel = new Panel();
			southPanel.setLayout(new GridLayout(1, 0));
			doneButton = new Button("Done");
			southPanel.add(doneButton);
			balloonCheckbox = new Checkbox("Coordinate balloon");
			balloonCheckbox.setState(false);
			southPanel.add(balloonCheckbox);
			add("South", southPanel);
			canvas = in_canvas;
			add("Center", canvas);

			balloonTextField = new TextField(canvas.ReConvert(0, 0).toString());
			balloonTextField.setEditable(false);
			balloon = new Window(parent);
			balloon.add(balloonTextField);
			balloon.setBackground(ColorChoice.GetColor("Yellow"));
			balloonTextField.setBackground(ColorChoice.GetColor("Yellow"));
			balloon.validate();
			balloon.pack();

			initialCurves = parent.principalCurve.ConvertToCurves();
			if (initialCurves == null)
				initialCurves = new SetOfCurves();
			startedDrawingInitialCurves = true;
			initialCurves.StartNewCurve();
			initialCurves.AddPoint(canvas.ReConvert(0, 0));

			parent.principalCurve.Reset();
			canvas.SetObjectToPaint(canvas.principalCurve, initialCurves);
			canvas.SetObjectToPaint(canvas.principalCurvePoints, initialCurves);

			validate();
			pack();
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean action(Event event, Object arg) {
			// Done
			if (event.target == doneButton) {
				parent.canvasPanel.add(canvas);
				canvas.Pack();
				parent.pack();
				if (initialCurves.Valid()) {
					(parent.principalCurve).InitializeToCurves(initialCurves, 2.001 * canvas.GetCanvasSize()
							/ canvas.getSize().width); // two pixels
				}
				parent.NewPrincipalCurve();
				postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
			} else if (event.target == balloonCheckbox) {
				if (balloonCheckbox.getState())
					balloon.show();
				else
					balloon.hide();
			}
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean handleEvent(Event event) {
			// Done
			if (event.id == Event.WINDOW_DESTROY) {
				balloon.dispose();
				dispose();
			}
			return super.handleEvent(event);
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean mouseDown(Event event, int x, int y) {
			Point p = canvas.location();
			x -= p.x;
			y -= p.y;
			if (canvas.inside(x, y)) {
				if (parent.logFile != null && processing_utilities.pcurves.Utilities.Environment.inApplet) {
					parent.logFile.AppendString((((new Date()).getTime() - parent.zeroTime) / 1000.0) + ":\t"
							+ processing_utilities.pcurves.Utilities.Misc.MouseButton(event.modifiers) + " MouseDown "
							+ x + " " + y + "\n");
				}
				// left button
				if (event.modifiers == 0) {
					if (!startedDrawingInitialCurves) {
						initialCurves.Reset();
						startedDrawingInitialCurves = true;
						initialCurves.AddPoint(canvas.ReConvert(x, y));
					}
					doneButton.disable();
					initialCurves.AddPoint(canvas.ReConvert(x, y));
				}
				// right button
				else if (event.modifiers == 4) {
					startedDrawingInitialCurves = false;
					if (initialCurves.Valid())
						doneButton.enable();
				}
				// middle button
				else if (event.modifiers == 8) {
					initialCurves.StartNewCurve();
					initialCurves.AddPoint(canvas.ReConvert(x, y));
				}
				canvas.SetObjectToPaint(canvas.principalCurve, initialCurves);
				canvas.SetObjectToPaint(canvas.principalCurvePoints, initialCurves);
			}
			return true;
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean mouseMove(Event event, int x, int y) {
			balloon.setLocation(location().x + x + 5, location().y + y + 5);
			Point p = canvas.location();
			x -= p.x;
			y -= p.y;
			if (canvas.inside(x, y)) {
				Vektor vektor = canvas.ReConvert(x, y);
				balloonTextField.setText(vektor.toString());
				if (startedDrawingInitialCurves) {
					initialCurves.UpdateLastPoint(vektor);
					canvas.SetObjectToPaint(canvas.principalCurve, initialCurves);
					canvas.SetObjectToPaint(canvas.principalCurvePoints, initialCurves);
				}
			}
			return true;
		}
	}

	class DiagnosisDialog extends Dialog {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public DiagnosisDialog(Frame frame) {
			super(frame, "Principal curve diagnosis", false);
			setLayout(new BorderLayout());
			add("Center", principalCurveParameters.diagnosisTextArea);
			Label label = new Label("Objective           MSE                 Penalty");
			add("North", label);
			validate();
			pack();
		}

		@SuppressWarnings("deprecation")
		@Override
		final public boolean handleEvent(Event event) {
			// Done
			if (event.id == Event.WINDOW_DESTROY) {
				dispose();
			}
			return super.handleEvent(event);
		}
	}
}
