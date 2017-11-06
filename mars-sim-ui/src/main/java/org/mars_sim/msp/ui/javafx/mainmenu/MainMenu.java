/**
 * Mars Simulation Project
 * MainMenu.java
 * @version 3.1.0 2017-10-03
 * @author Manny Kung
 */

package org.mars_sim.msp.ui.javafx.mainmenu;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.layout.AnchorPane;
import javafx.animation.FadeTransition;
//import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.StackPane;
//import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.geometry.HPos;
//mport javafx.scene.control.Button;
//import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
//import javafx.scene.SceneAntialiasing;
//import javafx.scene.control.Alert;
//import javafx.scene.control.Alert.AlertType;
//import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.geometry.Insets;
//import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.stage.FileChooser;
//import javafx.stage.Modality;
import javafx.stage.Screen;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.ui.javafx.config.ScenarioConfigEditorFX;
import org.mars_sim.msp.ui.javafx.config.controller.MainMenuController;
import org.mars_sim.msp.ui.javafx.networking.MultiplayerMode;
import org.mars_sim.msp.ui.javafx.MainScene;
import org.mars_sim.msp.ui.swing.tool.StartUpLocation;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXDialog.DialogTransition;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;

/*
 * The MainMenu class creates the Main Menu and the spinning Mars Globe for MSP
 */
@SuppressWarnings({ "restriction", "deprecation"})
public class MainMenu {

	// ------------------------------ FIELDS ------------------------------

	/** default logger. */
	private static Logger logger = Logger.getLogger(MainMenu.class.getName());

	public static final String OS = System.getProperty("os.name").toLowerCase(); // e.g. 'linux', 'mac os x'

    public static final int WIDTH = 1024;
    public static final int HEIGHT = 768;
  
    //public static final int MUSIC_VOLUME = 0;
    //public static final int SOUND_EFFECT_VOLUME = 1;
    
	// Data members
    public static String screen1ID = "main";
    public static String screen1File = "/fxui/fxml/MainMenu.fxml";
    public static String screen2ID = "configuration";
    public static String screen2File = "/fxui/fxml/Configuration.fxml";
    public static String screen3ID = "credits";
    public static String screen3File = "/fxui/fxml/Credits.fxml";

	private boolean isExit = false;
	
	public int mainscene_width = 1366; //1920;//
	public int mainscene_height = 768; //1080;//
	
	private double x = 0;
    private double y= 0;
    
	public float music_v = 50f;
	public float sound_effect_v = 50f;

    //private Point2D anchorPt;
    //private Point2D previousLocation;
    
    private AnchorPane anchorPane;
    
    private StackPane stackPane;
    
    private StackPane rootPane;

	private Stage primaryStage;
	
	private MainMenu mainMenu;
	
	private MainScene mainScene;
	//private ScreensSwitcher screen;

	private transient ThreadPoolExecutor executor;
	private MultiplayerMode multiplayerMode;
	private MainMenuController mainMenuController;

	private MenuApp menuApp;
	
	private JFXDialog settingDialog;
	
	private JFXDialog exitDialog;

    public MainMenu() {
       	//logger.info("MainMenu's constructor is on " + Thread.currentThread().getName());
    	mainMenu = this;
    	
		// See DPI Scaling at
		// http://news.kynosarges.org/2015/06/29/javafx-dpi-scaling-fixed/
		// "I guess we'll have to wait until Java 9 for more flexible DPI support.
		// In the meantime I managed to get JavaFX DPI scale factor,
		// but it is a hack (uses both AWT and JavaFX methods)"
/*
		// Number of actual horizontal lines (768p)
		double trueHorizontalLines = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
		// Number of scaled horizontal lines. (384p for 200%)
		double scaledHorizontalLines = Screen.getPrimary().getBounds().getHeight();
		// DPI scale factor.
		double dpiScaleFactor = trueHorizontalLines / scaledHorizontalLines;
		
		logger.info("horizontal lines : " + trueHorizontalLines);
		logger.info("DPI Scale Factor is " + dpiScaleFactor);
*/
		
		Screen screen = Screen.getPrimary(); 
		Rectangle2D bounds = screen.getVisualBounds();
		
		mainscene_width = (int) bounds.getWidth();
		mainscene_height = (int) bounds.getHeight();
       
		logger.info("Your Current Resolution is " + mainscene_width + " x " + mainscene_height);
		
		// Test
		//logger.info("Earth's surface gravity : " + Math.round(PlanetType.EARTH.getSurfaceGravity()*100.0)/100.0 + " m/s^2");
		//logger.info("Mars's surface gravity : " + Math.round(PlanetType.MARS.getSurfaceGravity()*100.0)/100.0 + " m/s^2");
		
 	}

    /*
     * Sets up and shows the MainMenu and prepare the stage for MainScene
     */
	@SuppressWarnings("restriction")
	public void initMainMenu(Stage stage) {
		System.setProperty("sampler.mode", "true");   
	   //logger.info("MainMenu's initAndShowGUI() is on " + Thread.currentThread().getName());
		this.primaryStage = stage;

    	Platform.setImplicitExit(false);
		// Note: If this attribute is true, the JavaFX runtime will implicitly shutdown when the last window is closed;
		// the JavaFX launcher will call the Application.stop method and terminate the JavaFX application thread.
		// If this attribute is false, the application will continue to run normally even after the last window is closed,
		// until the application calls exit. The default value is true.

    	menuApp = new MenuApp(mainMenu);
    	anchorPane = menuApp.createContent();
       
    	stackPane = new StackPane(anchorPane);
    	stackPane.setPrefSize(WIDTH, HEIGHT);
    	stackPane.setLayoutX(10);
    	stackPane.setLayoutY(10);
       
    	double sceneWidth = stackPane.getPrefWidth() + 30;
    	double sceneHeight = stackPane.getPrefHeight() + 30;

    	// Create application area
    	@SuppressWarnings("restriction")
    	Rectangle applicationArea = RectangleBuilder.create()
                .width(sceneWidth - 10)
                .height(sceneHeight - 10)
                .arcWidth(20)
                .arcHeight(20)
                .fill(Color.rgb(0, 0, 0, .80))
                .x(0)
                .y(0)
                .strokeWidth(2)
                .stroke(Color.rgb(255, 255, 255, .70))
                .build();
       
    	// Create close button
    	final Group closeApp = new Group();
    	Node closeRect = RectangleBuilder.create()
               .width(25)
               .height(25)
               .arcWidth(15)
               .arcHeight(15)
               .fill(Color.rgb(0, 0, 0, .80))
               .stroke(Color.WHITE)
               .build();
       
    	Text closeXmark = new Text(9, 16.5, "X");
    	closeXmark.setStroke( Color.WHITE);
    	closeXmark.setFill(Color.WHITE);
    	closeXmark.setStrokeWidth(2);
    	closeApp.setTranslateY(5);
    	closeApp.getChildren().addAll(closeRect, closeXmark);
    	closeApp.setOnMouseClicked(new EventHandler<MouseEvent>() {
       	@Override
       	public void handle(MouseEvent event) {
       		dialogOnExit(rootPane);

       		if (!isExit) {
       			event.consume();
       		}
       		else {
       			Platform.exit();
           		System.exit(0);
       		}
       	}
       });
       

       rootPane = new StackPane();
       rootPane.setPrefSize(stackPane.getPrefWidth()-300, stackPane.getPrefHeight());//stackPane.getPrefWidth()-50, stackPane.getPrefHeight()-50);
       //rootPane.setLayoutX((sceneWidth-stackPane.getPrefWidth()));
       //rootPane.setLayoutY((sceneHeight-stackPane.getPrefHeight()));
       
       Group root = new Group();
       root.getChildren().add(applicationArea);
       root.getChildren().add(stackPane);
       root.getChildren().add(closeApp);
       root.getChildren().add(rootPane);
       
       Scene scene = new Scene(root, sceneWidth, sceneHeight, Color.rgb(0, 0, 0, 0));

       closeApp.translateXProperty().bind(scene.widthProperty().subtract(40));
 
       menuApp.getTitleStackPane().setOnMousePressed(new EventHandler<MouseEvent>() {
           @Override
           public void handle(MouseEvent event) {
               x = event.getSceneX();
               y = event.getSceneY();
           }
       });
       menuApp.getTitleStackPane().setOnMouseDragged(new EventHandler<MouseEvent>() {
           @Override
           public void handle(MouseEvent event) {
               stage.setX(event.getScreenX() - x);
               stage.setY(event.getScreenY() - y);
           }
       });
       
/*       
       // starting initial anchor point
       scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event){
                anchorPt = new Point2D(event.getScreenX(), event.getScreenY());
            }
        });
        
        // dragging the entire stage
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event){
                if (anchorPt != null && previousLocation != null) {
                    primaryStage.setX(previousLocation.getX() + event.getScreenX() - anchorPt.getX());
                    primaryStage.setY(previousLocation.getY() + event.getScreenY() - anchorPt.getY());                    
                }
            }
        });
        
        // set the current location
        scene.setOnMouseReleased(new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event){
                previousLocation = new Point2D(primaryStage.getX(), primaryStage.getY());
            }
        });

       
        previousLocation = new Point2D(primaryStage.getX(), primaryStage.getY()); 
  */
       
	   //scene = new Scene(stackPane, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED); // Color.DARKGOLDENROD, Color.TAN);//MAROON); //TRANSPARENT);//DARKGOLDENROD);
        // Add keyboard control
        menuApp.getSpinningGlobe().getGlobe().handleKeyboard(scene);
       // Add mouse control
        menuApp.getSpinningGlobe().getGlobe().handleMouse(scene);

       //scene.setFill(Color.BLACK);//DARKGOLDENROD);//Color.BLACK);

        scene.getStylesheets().add(this.getClass().getResource("/fxui/css/mainmenu.css").toExternalForm());
       //mainMenuScene.setFill(Color.BLACK); // if using Group, a black border will remain
       //mainMenuScene.setFill(Color.TRANSPARENT); // if using Group, a white border will remain
        scene.setCursor(Cursor.HAND);

       // Makes the menu option box fades in
        scene.setOnMouseEntered(new EventHandler<MouseEvent>(){
           public void handle(MouseEvent mouseEvent){
        	   menuApp.startAnimation();
               FadeTransition fadeTransition
                       = new FadeTransition(Duration.millis(1000), menuApp.getOptionMenu());
               fadeTransition.setFromValue(0.0);
               fadeTransition.setToValue(1.0);
               fadeTransition.play();
           }
        });

       // Makes the menu option box fades out
        scene.setOnMouseExited(new EventHandler<MouseEvent>(){
           public void handle(MouseEvent mouseEvent){
        	   menuApp.endAnimation();
               FadeTransition fadeTransition
                       = new FadeTransition(Duration.millis(1000), menuApp.getOptionMenu());
               fadeTransition.setFromValue(1.0);
               fadeTransition.setToValue(0.0);
               fadeTransition.play();
        	   fadeTransition.setOnFinished(e -> menuApp.clearMenuItems());
           }
        });
     
       primaryStage.setOnCloseRequest(e -> {
    		dialogOnExit(rootPane);

    		if (!isExit) {
    			e.consume();
    		}
    		else {
    			Platform.exit();
        		System.exit(0);
    		}
        });

       //primaryStage.setResizable(false);
       //primaryStage.setTitle(Simulation.title);
       primaryStage.getIcons().add(new Image(this.getClass().getResource("/icons/lander_hab64.png").toExternalForm()));
       //NOTE: OR use svg file with stage.getIcons().add(new Image(this.getClass().getResource("/icons/lander_hab.svg").toString()));
       primaryStage.setScene(scene);
       
       setMonitor(stage);
       
       primaryStage.centerOnScreen();
       primaryStage.initStyle(StageStyle.TRANSPARENT);
       primaryStage.show();


   }
	
	public void createMenuApp() {
		 menuApp = new MenuApp(mainMenu);
		 anchorPane = menuApp.createContent();
	}
	
	public Stage getStage() {
		return primaryStage;
	}

   public MainScene getMainScene() {
	   return mainScene;
   }

   public MultiplayerMode getMultiplayerMode() {
	   return multiplayerMode;
	}

   public void runNew() {
	   primaryStage.setIconified(true);
	   primaryStage.hide();
	   primaryStage.close();
	   
	   // creates a mainScene instance
	   //if (mainScene != null)
	   mainScene = new MainScene(mainscene_width, mainscene_height);
	   //mainScene.setSound(music_v, sound_effect_v);
	   
       try {
    	   // Loads Scenario Config Editor
    	   Simulation.instance().getSimExecutor().execute(new ConfigEditorTask());

       } catch (Exception e) {
    	   e.printStackTrace();
    	   exitWithError("Error : could not create a new simulation ", e);
       }


   }

	public class ConfigEditorTask implements Runnable {
		  public void run() {
			  //logger.info("MarsProjectFX's ConfigEditorTask's run() is on " + Thread.currentThread().getName() );
			  new ScenarioConfigEditorFX(mainMenu); //marsProjectFX,
		  }
	}

   /**
    * Opens the file chooser to select a saved sim to load 
    */
   public void runLoad() {

	   primaryStage.setIconified(true);
	   primaryStage.hide();
	   primaryStage.close();

	   loadSim(null);

   }

   /**
    * Loads the setting dialog
    */
   public void runSettings() {   
	   selectResolutionDialog(rootPane);
   }

   /*
    * Loads the simulation file via the terminal or FileChooser.
    * @param selectedFile the saved sim
    */
   public void loadSim(File selectedFile) {

	   String dir = Simulation.DEFAULT_DIR;
	   String title = null;

	   try {

		   if (selectedFile == null) {

			   FileChooser chooser = new FileChooser();
				// chooser.setInitialFileName(dir);
				// Set to user directory or go to default if cannot access
				// String userDirectoryString = System.getProperty("user.home");
			   File userDirectory = new File(dir);
			   chooser.setInitialDirectory(userDirectory);
			   chooser.setTitle(title); // $NON-NLS-1$

				// Set extension filter
			   FileChooser.ExtensionFilter simFilter = new FileChooser.ExtensionFilter(
						"Simulation files (*.sim)", "*.sim");
			   FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(
						"all files (*.*)", "*.*");

			   chooser.getExtensionFilters().addAll(simFilter, allFilter);

			   // Show open file dialog
			   selectedFile = chooser.showOpenDialog(primaryStage);
		   }

		   else {
	   			// if user wants to load the default saved sim

		   }

	   } catch (NullPointerException e) {
	       System.err.println("NullPointerException in loading sim. " + e.getMessage());
	       Platform.exit();
	       System.exit(1);

	   } catch (Exception e) {
	       System.err.println("Exception in loading sim. " + e.getMessage());
	       Platform.exit();
	       System.exit(1);
	   }

	   if (selectedFile != null) {

			final File fileLocn = selectedFile;

			Platform.runLater(() -> {
				mainScene = new MainScene(mainscene_width, mainscene_height);
				
				mainScene.createLoadingIndicator();
				mainScene.showWaitStage(MainScene.LOADING);
			});

			CompletableFuture.supplyAsync(() -> submitTask(fileLocn, false));

		}

		else {
			logger.info("No file was selected. Loading is cancelled");
	        Platform.exit();
	        System.exit(1);
		}

   }

	public int submitTask(File fileLocn, boolean autosaveDefaultName) {
		Simulation.instance().getSimExecutor().execute(new LoadSimulationTask(fileLocn, autosaveDefaultName));
		return 1;
	}

   /*
    * Loads settlement data from a saved sim
    * @param fileLocn user's sim filename
    */
	public class LoadSimulationTask implements Runnable {
		File fileLocn;
		boolean autosaveDefaultName;

		LoadSimulationTask(File fileLocn, boolean autosaveDefaultName){
			this.fileLocn = fileLocn;
			this.autosaveDefaultName = autosaveDefaultName;
		}

		public void run() {
			//logger.info("LoadSimulationTask is on " + Thread.currentThread().getName() + " Thread");

			Simulation sim = Simulation.instance();
   			// Initialize the simulation.
			Simulation.createNewSimulation();

			try {
				// Loading settlement data from the default saved simulation
				sim.loadSimulation(fileLocn); // null means loading "default.sim"

	        } catch (Exception e) {
	            //e.printStackTrace();
	            exitWithError("Error : could not create a new simulation ", e);
	        }

			sim.start(autosaveDefaultName);

			while (sim.getMasterClock() == null)
				try {
					TimeUnit.MILLISECONDS.sleep(200L);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			mainScene.finalizeMainScene();
		}
	}

   public void runMultiplayer() {
	   //logger.info("MainMenu's runThree() is on " + Thread.currentThread().getName() + " Thread");
	   Simulation.instance().getSimExecutor().submit(new MultiplayerTask());
	   primaryStage.setIconified(true);//hide();
/*
	   try {
			multiplayerMode = new MultiplayerMode(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // newCachedThreadPool();
		executor.execute(multiplayerMode.getModeTask());
*/
	    //pool.shutdown();
	     //mainSceneStage.toFront();
   		//modtoolScene = new SettlementScene().createSettlementScene();
	    //stage.setScene(modtoolScene);
	    //stage.show();
   }
 
	public class MultiplayerTask implements Runnable {
		public void run() {
			try {
				multiplayerMode = new MultiplayerMode(mainMenu);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1); // newCachedThreadPool();
			executor.execute(multiplayerMode.getModeTask());
		}
	}

   //public void changeScene(int toscene) {
	   //switch (toscene) {
		   	//case 1:
		   		//scene = new MenuScene().createScene();
		   	//	break;
	   		//case 2:
	   			//Scene scene = mainScene.createMainScene();
	   			//break;
	   		//case 3:
	   			//scene = modtoolScene.createScene(stage);
	   		//	break;
	   		//stage.setScene(scene);
	   //}
   //}

	/*
	* Sets the main menu controller at the start of the Simulation.instance().
	*/
	// 2015-09-27 Added setController()
	public void setController(ControlledScreen myScreenController) {
	   if (mainMenuController == null)
		   if (myScreenController instanceof MainMenuController ) {
			   mainMenuController = (MainMenuController) myScreenController;
			   mainMenuController.setBuild();
		   }
	}

	public void chooseScreen(int num) {

		ObservableList<Screen> screens = Screen.getScreens();//.getScreensForRectangle(xPos, yPos, 1, 1);
    	Screen currentScreen = screens.get(num);
		Rectangle2D rect = currentScreen.getVisualBounds();

		Screen primaryScreen = Screen.getPrimary();
	}

	public void setMonitor(Stage stage) {
		// Issue: how do we tweak mars-sim to run on the "active" monitor as chosen by user ?
		// "active monitor is defined by whichever computer screen the mouse pointer is or where the command console that starts mars-sim.
		// by default MSP runs on the primary monitor (aka monitor 0 as reported by windows os) only.
		// see http://stackoverflow.com/questions/25714573/open-javafx-application-on-active-screen-or-monitor-in-multi-screen-setup/25714762#25714762

/*
		AnchorPane anchor = null;
		if (root == null) {
	       anchor = new AnchorPane();
	       anchor.setPrefWidth(WIDTH);
	       anchor.setPrefHeight(HEIGHT);
		}
*/

		StartUpLocation startUpLoc = new StartUpLocation(WIDTH, HEIGHT);
        double xPos = startUpLoc.getXPos();
        double yPos = startUpLoc.getYPos();
        // Set Only if X and Y are not zero and were computed correctly
     	//ObservableList<Screen> screens = Screen.getScreensForRectangle(xPos, yPos, 1, 1);
     	//ObservableList<Screen> screens = Screen.getScreens();
    	//System.out.println("# of monitors : " + screens.size());

        if (xPos != 0 && yPos != 0) {
            stage.setX(xPos);
            stage.setY(yPos);
            //System.out.println("Monitor 2:    x : " + xPos + "   y : " + yPos);
        } else {
            //System.out.println("calling centerOnScreen()");
            //System.out.println("Monitor 1:    x : " + xPos + "   y : " + yPos);
        }
        stage.centerOnScreen();
	}

	//public ScreensSwitcher getScreensSwitcher() {
	//	return screen;
	//}

	public MainMenuController getMainMenuController() {
		return mainMenuController;
	}

	public SpinningGlobe getSpinningGlobe() {
		return menuApp.getSpinningGlobe();
	}

    /**
     * Exit the simulation with an error message.
     * @param message the error message.
     * @param e the thrown exception or null if none.
     */
    private void exitWithError(String message, Exception e) {
        showError(message, e);
        Platform.exit();
        System.exit(1);
    }

    /**
     * Show a modal error message dialog.
     * @param message the error message.
     * @param e the thrown exception or null if none.
     */
    private void showError(String message, Exception e) {
        if (e != null) {
            logger.log(Level.SEVERE, message, e);
        }
        else {
            logger.log(Level.SEVERE, message);
        }
    }


    public StackPane getPane() {
    	return rootPane;
    }
    
    
	/**
	 * Open the exit dialog box
	 * @param pane
	 */
	public void dialogOnExit(StackPane pane) {
		
		if (exitDialog == null && (settingDialog == null || (settingDialog != null && !settingDialog.isVisible()))) {

			Label l = new Label("Do you really want to exit ?");//mainScene.createBlendLabel(Msg.getString("MainScene.exit.header"));
			l.setPadding(new Insets(10, 10, 10, 10));
			l.setFont(Font.font(null, FontWeight.BOLD, 14));
			HBox hb = new HBox();
			JFXButton b1 = new JFXButton("Exit");
			b1.setStyle("-fx-background-color: white;");
			JFXButton b2 = new JFXButton("Back");
			b2.setStyle("-fx-background-color: white;");
			hb.getChildren().addAll(b1, b2);
			hb.setAlignment(Pos.CENTER);
			HBox.setMargin(b1, new Insets(3,3,3,3));
			HBox.setMargin(b2, new Insets(3,3,3,3));
			VBox vb = new VBox();
			vb.setAlignment(Pos.CENTER);
			vb.setPadding(new Insets(5, 5, 5, 5));
			vb.getChildren().addAll(l, hb);
			StackPane sp = new StackPane(vb);
			sp.setStyle("-fx-background-color:rgba(0,0,0,0.1);");
			StackPane.setMargin(vb, new Insets(10,10,10,10));
			exitDialog = new JFXDialog();
			//dialog.setDialogContainer(pane);
			exitDialog.setTransitionType(DialogTransition.BOTTOM);
			exitDialog.setContent(sp);
			exitDialog.show(pane);
			
			b1.setOnAction(e -> {
				isExit = true;
				exitDialog.close();
				Platform.exit();
				System.exit(0);
			});
			
			b2.setOnAction(e -> {
				exitDialog.close();
				e.consume();
			});

		}
		else if (exitDialog != null 
				&& !exitDialog.isVisible()
			 	&& (settingDialog == null || (settingDialog != null && !settingDialog.isVisible()))) {
			exitDialog.show(pane);
		}
	}	

	  
	/**
	 * Selects the game screen resolution in this dialog box
	 * @param pane
	 */
	public void selectResolutionDialog(StackPane pane) {
		if (settingDialog == null && (exitDialog == null || (exitDialog != null && !exitDialog.isVisible()))) {
			
			Label titleLabel = new Label("      Select Screen Resolution");
			titleLabel.setAlignment(Pos.TOP_LEFT);
			titleLabel.setPadding(new Insets(10, 10, 10, 10));
			titleLabel.setStyle("-fx-text-fill: white;");
			titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
				
			JFXButton return_btn = new JFXButton("Done");
			return_btn.setStyle("-fx-background-color: white;");
			
			HBox return_hb = new HBox();
			return_hb.getChildren().addAll(return_btn);
			return_hb.setAlignment(Pos.CENTER);
			
			HBox.setMargin(return_btn, new Insets(10, 10, 10, 10));
	
			final ToggleGroup group = new ToggleGroup();
	
			JFXRadioButton r7 = new JFXRadioButton("2560 x 1600");
			r7.setStyle("-fx-text-fill: white;");
			    r7.setToggleGroup(group);
	  
				JFXRadioButton r6 = new JFXRadioButton("2560 x 1440");
			r6.setStyle("-fx-text-fill: white;");
			    r6.setToggleGroup(group);
	  
				JFXRadioButton r5 = new JFXRadioButton("1920 x 1080");
			r5.setStyle("-fx-text-fill: white;");
		    r5.setToggleGroup(group);
	
			JFXRadioButton r4 = new JFXRadioButton("1600 x 900");
			r4.setStyle("-fx-text-fill: white;");
		    r4.setToggleGroup(group);
	
			JFXRadioButton r3 = new JFXRadioButton("1366 x 768");
			r3.setStyle("-fx-text-fill: white;");
		    r3.setToggleGroup(group);
	
			JFXRadioButton r2 = new JFXRadioButton("1280 x 800");
			r2.setStyle("-fx-text-fill: white;");
		    r2.setToggleGroup(group);
	
			JFXRadioButton r1 = new JFXRadioButton("1280 × 720");
			r1.setStyle("-fx-text-fill: white;");
		    r1.setToggleGroup(group);
	
			JFXRadioButton r0 = new JFXRadioButton("1024 x 768");
			r0.setStyle("-fx-text-fill: white;");
		    r0.setToggleGroup(group);
		
		    
		    if (mainscene_width == 2560) {
		    	if (mainscene_height == 1600) {
				    r7.setSelected(true);
		    	}
		    	else if (mainscene_height == 1440) {
				    r6.setSelected(true);
		    	}
		    }
		    else if (mainscene_width == 1920)
			    r5.setSelected(true);
		    else if (mainscene_width == 1600)
			    r4.setSelected(true);	 
		    else if (mainscene_width == 1366)
			    r3.setSelected(true);	    
		    else if (mainscene_width == 1280) {	 
		    	if (mainscene_height == 800) {
				    r2.setSelected(true);
		    	}
		    	else if (mainscene_height == 720) {
				    r1.setSelected(true);
		    	}
		    }
		    else if (mainscene_width == 1024)
			    r0.setSelected(true);
		    else 
		    	// by default, set to 1024 x 768
		    	r0.setSelected(true);
		    
		    
		    group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
		        @Override
		        public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1) {
		        	if (!t.equals(t1)) {
			        	 // Cast object to radio button
			        	JFXRadioButton selected = (JFXRadioButton) group.getSelectedToggle();
	
		        		if (selected.equals(r7))
		        			setScreenSize(2560, 1600);	
		        		else if (selected.equals(r6))
		        			setScreenSize(2560, 1440);	
		        		else if (selected.equals(r5))
		        			setScreenSize(1920, 1080);	
		        		else if (selected.equals(r4))
		        			setScreenSize(1600, 900);	
		        		else if (selected.equals(r3))
		        			setScreenSize(1366, 768);	
		        		else if (selected.equals(r2))
		        			setScreenSize(1280, 800);
		        		else if (selected.equals(r1))
		        			setScreenSize(1280, 720);	
		        		else if (selected.equals(r0))
		        			setScreenSize(1024, 768);	
	
		        	}
		        }
	
		    });
	
		    
		    GridPane gridpane = new GridPane();
		    gridpane.setPadding(new Insets(15));
		    gridpane.setHgap(15);
		    gridpane.setVgap(15);
		    ColumnConstraints column0 = new ColumnConstraints(120);
		    ColumnConstraints column1 = new ColumnConstraints(120);
		    ColumnConstraints column2 = new ColumnConstraints(120);
		    ColumnConstraints column3 = new ColumnConstraints(120);
		    column2.setHgrow(Priority.ALWAYS);
		    gridpane.getColumnConstraints().addAll(column0, column1, column2, column3);
	
		    GridPane.setHalignment(r0, HPos.LEFT);
		    gridpane.add(r0, 0, 0);
	
		    GridPane.setHalignment(r1, HPos.LEFT);
		    gridpane.add(r1, 1, 0);
	
		    GridPane.setHalignment(r2, HPos.LEFT);
		    gridpane.add(r2, 2, 0);
	
		    GridPane.setHalignment(r3, HPos.LEFT);
		    gridpane.add(r3, 3, 0);
	
		    GridPane.setHalignment(r4, HPos.LEFT);
		    gridpane.add(r4, 0, 1);
	
		    GridPane.setHalignment(r5, HPos.LEFT);
		    gridpane.add(r5, 1, 1);
	
		    GridPane.setHalignment(r6, HPos.LEFT);
		    gridpane.add(r6, 2, 1);
	
		    GridPane.setHalignment(r7, HPos.LEFT);
		    gridpane.add(r7, 3, 1);
		    
		    
			Label soundlabel = new Label("      Set Sound Volume");
			soundlabel.setAlignment(Pos.TOP_LEFT);
			soundlabel.setPadding(new Insets(10, 10, 10, 10));
			soundlabel.setStyle("-fx-text-fill: white;");
			soundlabel.setFont(Font.font(null, FontWeight.BOLD, 14));
				
			// Set up the slider for background music and sound effect
			Tile soundTile0 = musicSetting();
			HBox sound0 = new HBox();
			sound0.setPadding(new Insets(5, 5, 5, 5));
			sound0.getChildren().addAll(soundTile0);
			sound0.setAlignment(Pos.CENTER);
	
			Tile soundTile1 = soundEffectSetting();
			HBox sound1 = new HBox();
			sound1.setPadding(new Insets(5, 5, 5, 5));
			sound1.getChildren().addAll(soundTile1);
			sound1.setAlignment(Pos.CENTER);
			
			//Horizontal separator
			//Separator separator = new Separator();
			//separator.setMaxWidth(160);
			//separator.setHalignment(HPos.LEFT);
			
			VBox emptyVB = new VBox();
			//vb.setAlignment(Pos.CENTER);
			emptyVB.setPadding(new Insets(25, 25, 25, 25));
			
			VBox vb = new VBox();
			//vb.setAlignment(Pos.CENTER);
			vb.setPadding(new Insets(15, 15, 15, 15));
			vb.getChildren().addAll(titleLabel, gridpane, emptyVB, soundlabel, sound0, sound1, return_hb); 
					
			StackPane sp = new StackPane(vb);
			sp.setStyle("-fx-background-color:black;");//rgb(105,105,105);");//darkgrey;");//rgba(0,0,0,0.1);");
			StackPane.setMargin(vb, new Insets(10,10,10,10));
			settingDialog = new JFXDialog();
			//dialog.setDialogContainer(pane);
			settingDialog.setContent(sp);
			settingDialog.setTransitionType(DialogTransition.BOTTOM);
			settingDialog.show(pane);
			
			return_btn.setOnAction(e -> {
				//pane.setPrefWidth(stackPane.getPrefWidth()-300);
				settingDialog.close();
				e.consume();
			});

		}
		else if (settingDialog != null 
			&& !settingDialog.isVisible()
			&& (exitDialog == null 
				|| (exitDialog != null && !exitDialog.isVisible()))) {
				settingDialog.show(pane);
			}
	}	

	
    public Tile musicSetting() {
        Tile switchSliderTile = TileBuilder.create()
                .skinType(SkinType.SWITCH_SLIDER)
                .prefSize(450, 100)
                .backgroundColor(Color.BLACK)//.rgb(105,105,105))//.DARKGRAY)//.rgb(255, 255, 255, 0.1))
                //.title(title)
                .description("Background Music")
                .descriptionAlignment(Pos.TOP_CENTER)
                .textVisible(true)
                .decimals(0)
                .minValue(0)
                .maxValue(100)
                .value(music_v)
                .build();
        
        switchSliderTile.setActive(true);

        switchSliderTile.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {

				if (old_val != new_val) {
					float vol = new_val.floatValue();
					
					if (vol <= 5) { 
						music_v = 0;
			            //System.out.println("Background Music is mute.");    
			            switchSliderTile.setValue(0);
			            switchSliderTile.setActive(false);
					} else {
						music_v = ((int)(vol*5f))/5;
						switchSliderTile.setActive(true);
					}
					
					if (!switchSliderTile.isActive()) {
						switchSliderTile.setValue(0);
						//switchSliderTile.setActive(false);
					}
				}

			}
		});

		switchSliderTile.setOnSwitchPressed(e -> {
			if (switchSliderTile.getCurrentValue() < 5 || !switchSliderTile.isActive()) {
				switchSliderTile.setValue(0);
				switchSliderTile.setActive(false);
			}
			else {
				switchSliderTile.setActive(true);
				if (switchSliderTile.getCurrentValue() < 5)
					switchSliderTile.setValue(80);
			}
				
		});
		
		switchSliderTile.setOnSwitchReleased(e -> {
			if (switchSliderTile.getCurrentValue() < 5 || !switchSliderTile.isActive()) {
				switchSliderTile.setValue(0);
				switchSliderTile.setActive(false);
			}
			else {
				switchSliderTile.setActive(true);
				if (switchSliderTile.getCurrentValue() < 5)
					switchSliderTile.setValue(80);
			}
		});

		return switchSliderTile;
    }	
 
    
    public Tile soundEffectSetting() {

        Tile switchSliderTile = TileBuilder.create()
                .skinType(SkinType.SWITCH_SLIDER)
                .prefSize(450, 100)
                .backgroundColor(Color.BLACK)//.rgb(105,105,105))//.DARKGRAY)//.rgb(255, 255, 255, 0.1))
                //.title(title)
                .description("Sound Effect")
                .descriptionAlignment(Pos.TOP_CENTER)
                .textVisible(true)
                .decimals(0)
                .minValue(0)
                .maxValue(100)
                .value(sound_effect_v)
                .build();

        switchSliderTile.setActive(true);
        
        switchSliderTile.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {

				if (old_val != new_val) {
					float vol = new_val.floatValue();

					if (vol <= 5) { 
						sound_effect_v = 0f;
						switchSliderTile.setValue(0);
			            //System.out.println("Sound Effect is mute.");    
			            switchSliderTile.setActive(false);
					} else {
						sound_effect_v = ((int)(vol*5f))/5;
						switchSliderTile.setActive(true);

					}
					
					if (!switchSliderTile.isActive()) {
						switchSliderTile.setValue(0);
						//switchSliderTile.setActive(false);
					}
						
				}
			}
		});

		switchSliderTile.setOnSwitchPressed(e -> {
			
			if (switchSliderTile.getCurrentValue() < 5 || !switchSliderTile.isActive()) {
				switchSliderTile.setValue(0);
				switchSliderTile.setActive(false);
			}
			else {
				switchSliderTile.setActive(true);
				if (switchSliderTile.getCurrentValue() < 5)
					switchSliderTile.setValue(80);
			}
				
		});
		
		switchSliderTile.setOnSwitchReleased(e -> {
			if (switchSliderTile.getCurrentValue() < 5 || !switchSliderTile.isActive()) {
				switchSliderTile.setValue(0);
				switchSliderTile.setActive(false);
			}
			else {
				switchSliderTile.setActive(true);
				if (switchSliderTile.getCurrentValue() < 5)
					switchSliderTile.setValue(80);
			}
		});

		return switchSliderTile;
    }	
    
	public void setScreenSize(int w, int h) {
		mainscene_width = w;
		mainscene_height = h;
	}
	
	public void destroy() {
		anchorPane = null;
		primaryStage = null;
		mainMenu = null;
		mainScene = null;
		executor = null;
		multiplayerMode = null;
		mainMenuController = null;
		menuApp = null;
		
	}

}
