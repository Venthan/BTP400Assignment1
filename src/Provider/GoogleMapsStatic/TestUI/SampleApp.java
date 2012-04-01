/*
 * Created by JFormDesigner on Mon Apr 21 12:50:34 EDT 2008
 * Edited by Team 2 
 ** Suventhan Krishnamoorthy (016225080)
 ** Aayush Dhamala (045079068)
 ** Nan Zhou (058459100)
 */

package Provider.GoogleMapsStatic.TestUI;


import Provider.GoogleMapsStatic.*;
import Task.*;
import Task.Manager.*;
import Task.ProgressMonitor.*;
import Task.Support.CoreSupport.*;
import Task.Support.GUISupport.*;
import com.jgoodies.forms.factories.*;
import info.clearthought.layout.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;


import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.beans.*;
import java.text.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.awt.BorderLayout;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.awt.image.BufferedImage;

/** @author nazmul idris */
public class SampleApp extends JFrame {
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
// data members
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
/** reference to task */
private SimpleTask _task;
/** this might be null. holds the image to display in a popup */
private BufferedImage _img;
/** this might be null. holds the text in case image doesn't display */
private String _respStr;
//Initizaling a string to hold the names of location
private String[] cities = {"Toronto","CN Tower","Ontario Museum","Ontario Science Centre","Seneca@York","Seneca newnham",
						   "Montreal","Vancouver","London","Ottawa","WonderLand","Niagara Falls"};
private String[] infoCitie;
private String nameL;
private JFileChooser jFileChooser1 = new JFileChooser(new File("."));


//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
// main method...
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

public static void main(String[] args) {
  Utils.createInEDT(SampleApp.class);
  
}

//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
// constructor
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

private void doInit() {
  GUIUtils.setAppIcon(this, "burn.png");
  //GUIUtils.centerOnScreen(this);
  setVisible(true);

  int W = 28, H = W;
  boolean blur = false;
  float alpha = .7f;

  try {
    btnGetMap.setIcon(ImageUtils.loadScaledBufferedIcon("ok1.png", W, H, blur, alpha));
    btnQuit.setIcon(ImageUtils.loadScaledBufferedIcon("charging.png", W, H, blur, alpha));
  }
  catch (Exception e) {
    System.out.println(e);
  }

  _setupTask();
}

/** create a test task and wire it up with a task handler that dumps output to the textarea */
@SuppressWarnings("unchecked")
private void _setupTask() {

  TaskExecutorIF<ByteBuffer> functor = new TaskExecutorAdapter<ByteBuffer>() {
    public ByteBuffer doInBackground(Future<ByteBuffer> swingWorker,
                                     SwingUIHookAdapter hook) throws Exception
    {

      _initHook(hook);

      // set the license key
      MapLookup.setLicenseKey(ttfLicense.getText());
      
     
      // get the uri for the static map
      String uri = MapLookup.getMap(Double.parseDouble(ttfLat.getText()),
                                    Double.parseDouble(ttfLon.getText()),
                                    Integer.parseInt(ttfSizeW.getText()),
                                    Integer.parseInt(ttfSizeH.getText()),
                                    Integer.parseInt(ttfZoom.getText())
      );
      sout("Google Maps URI=" + uri);

      // get the map from Google
      GetMethod get = new GetMethod(uri);
      new HttpClient().executeMethod(get);

      ByteBuffer data = HttpUtils.getMonitoredResponse(hook, get);

      try {
        _img = ImageUtils.toCompatibleImage(ImageIO.read(data.getInputStream()));
        sout("converted downloaded data to image...");
      }
      catch (Exception e) {
        _img = null;
        sout("The URI is not an image. Data is downloaded, can't display it as an image.");
        _respStr = new String(data.getBytes());
      }

      return data;
    }

    @Override public String getName() {
      return _task.getName();
    }
  };

  _task = new SimpleTask(
      new TaskManager(),
      functor,
      "HTTP GET Task",
      "Download an image from a URL",
      AutoShutdownSignals.Daemon
  );

  _task.addStatusListener(new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      sout(":: task status change - " + ProgressMonitorUtils.parseStatusMessageFrom(evt));
      lblProgressStatus.setText(ProgressMonitorUtils.parseStatusMessageFrom(evt));
    }
  });

  _task.setTaskHandler(new
      SimpleTaskHandler<ByteBuffer>() {
        @Override public void beforeStart(AbstractTask task) {
          sout(":: taskHandler - beforeStart");
        }
        @Override public void started(AbstractTask task) {
          sout(":: taskHandler - started ");
        }
        /** {@link SampleApp#_initHook} adds the task status listener, which is removed here */
        @Override public void stopped(long time, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- stopped");
          sout(":: time = " + time / 1000f + "sec");
          task.getUIHook().clearAllStatusListeners();
        }
        @Override public void interrupted(Throwable e, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- interrupted - " + e.toString());
        }
        @Override public void ok(ByteBuffer value, long time, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- ok - size=" + (value == null
              ? "null"
              : value.toString()));
          if (_img != null) {
            _displayImgInFrame();
          }
          else _displayRespStrInFrame();

        }
        @Override public void error(Throwable e, long time, AbstractTask task) {
          sout(":: taskHandler [" + task.getName() + "]- error - " + e.toString());
        }
        @Override public void cancelled(long time, AbstractTask task) {
          sout(" :: taskHandler [" + task.getName() + "]- cancelled");
        }
      }
  );
}

private SwingUIHookAdapter _initHook(SwingUIHookAdapter hook) {
  hook.enableRecieveStatusNotification(checkboxRecvStatus.isSelected());
  hook.enableSendStatusNotification(checkboxSendStatus.isSelected());

  hook.setProgressMessage(ttfProgressMsg.getText());

  PropertyChangeListener listener = new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      SwingUIHookAdapter.PropertyList type = ProgressMonitorUtils.parseTypeFrom(evt);
      int progress = ProgressMonitorUtils.parsePercentFrom(evt);
      String msg = ProgressMonitorUtils.parseMessageFrom(evt);

      progressBar.setValue(progress);
      progressBar.setString(type.toString());

      sout(msg);
    }
  };

  hook.addRecieveStatusListener(listener);
  hook.addSendStatusListener(listener);
  hook.addUnderlyingIOStreamInterruptedOrClosed(new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      sout(evt.getPropertyName() + " fired!!!");
    }
  });

  return hook;
}
//Suventhan's Code
/*
 * Removing the popup window and recreating the img to a different location
 */
private void _displayImgInFrame() {
	
	//remove everything thats in mapPanel
	mapPanel.removeAll();
	//repaint the img
	mapPanel.repaint();
	//create a label and insert the img
	JLabel imgLbl=new JLabel(new ImageIcon(_img));
	//add the label to the mapPanel
	mapPanel.add(imgLbl);
	
	
}

private void _displayRespStrInFrame() {

  final JFrame frame = new JFrame("Google Static Map - Error");
  GUIUtils.setAppIcon(frame, "69.png");
  frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

  JTextArea response = new JTextArea(_respStr, 25, 80);
  response.addMouseListener(new MouseListener() {
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) { frame.dispose();}
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
  });

  frame.setContentPane(new JScrollPane(response));
  frame.pack();

  GUIUtils.centerOnScreen(frame);
  frame.setVisible(true);
}

/** simply dump status info to the textarea */
private void sout(final String s) {
  Runnable soutRunner = new Runnable() {
    public void run() {
      if (ttaStatus.getText().equals("")) {
        ttaStatus.setText(s);
      }
      else {
        ttaStatus.setText(ttaStatus.getText() + "\n" + s);
      }
    }
  };

  if (ThreadUtils.isInEDT()) {
    soutRunner.run();
  }
  else {
    SwingUtilities.invokeLater(soutRunner);
  }
}

private void startTaskAction() {
  try {
    _task.execute();
  }
  catch (TaskException e) {
    sout(e.getMessage());
  }
}


public SampleApp() {
  initComponents();
  doInit();
}

private void quitProgram() {
  _task.shutdown();
  System.exit(0);
}

private void initComponents() {
  // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
  // Generated using JFormDesigner non-commercial license
  dialogPane = new JPanel();
  contentPanel = new JPanel();
  contentPane2 = new JPanel();
  contentPane3 = new JTextArea();
  contentPaneM = new JPanel();
  newD = new JPanel();
  panel1 = new JPanel();
  panelsuv = new JPanel();
  panelsouth = new JTextArea();
  panelMap = new JPanel();
  centerPanelC = new JPanel();
  label2 = new JLabel();
  ttfSizeW = new JTextField();
  label4 = new JLabel();
  ttfLat = new JTextField();
  btnGetMap = new JButton();
  btnGetTorMap = new JButton();
  btnGetSenecaYMap = new JButton();
  btnGetSenecaNMap = new JButton();
  btnGetEfTowerMap = new JButton();
  btnGetGizaPMap = new JButton();
  btnGetIndiaMap = new JButton();
  btnGetStauteMap = new JButton();
  btnGetCnTowerMap = new JButton();
  okButton = new JButton();
  saveButton = new JButton();
  centerPanel = new JButton();
  centerPaneld = new JButton();
  centerPanell = new JButton();
  centerPanelr = new JButton();
  pickComboBox = new JComboBox();
  label3 = new JLabel();
  ttfSizeH = new JTextField();
  label5 = new JLabel();
  ttfLon = new JTextField();
  btnQuit = new JButton();
  label1 = new JLabel();
  ttfLicense = new JTextField();
  label6 = new JLabel();
  ttfZoom = new JTextField();
  scrollPane1 = new JScrollPane();
  ttaStatus = new JTextArea();
  panel2 = new JPanel();
  panel3 = new JPanel();
  checkboxRecvStatus = new JCheckBox();
  checkboxSendStatus = new JCheckBox();
  ttfProgressMsg = new JTextField();
  progressBar = new JProgressBar();
  lblProgressStatus = new JLabel();
  jlblStatus = new JLabel();
  saveinput = new JFileChooser();

  //======== this ========
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
  setTitle("Google Static Maps-Edited by Team 2(Suventhan,Aayush & Nan)");
  setIconImage(null);
  Container contentPane = getContentPane();
  contentPane.setLayout(new BorderLayout());

  //======== dialogPane ========
  {
  	dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
  	dialogPane.setOpaque(false);
  	dialogPane.setLayout(new BorderLayout());

  	//======== contentPanel ========
  	{
  		contentPanel.setOpaque(false);
  		contentPanel.setLayout(new TableLayout(new double[][] {
  			{550,TableLayout.FILL},
  			{TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED}}));
  		((TableLayout)contentPanel.getLayout()).setHGap(5);
  		((TableLayout)contentPanel.getLayout()).setVGap(5);
  		
  		contentPane2.setOpaque(false);
  		contentPane2.setLayout(new TableLayout(new double[][] {
  			{TableLayout.FILL},
  			{TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED}}));
  		((TableLayout)contentPane2.getLayout()).setHGap(5);
  		((TableLayout)contentPane2.getLayout()).setVGap(5);
  		
  		//======== panel1 ========
  		{
  			panel1.setOpaque(false);
  			panel1.setBorder(new CompoundBorder(
  				new TitledBorder("Configure the inputs to Google Static Maps"),
  				Borders.DLU2_BORDER));
  			panel1.setLayout(new TableLayout(new double[][] {
  				{0.17, 0.17, 0.17, 0.17, 0.05, TableLayout.FILL},
  				{TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED}}));
  			((TableLayout)panel1.getLayout()).setHGap(5);
  			((TableLayout)panel1.getLayout()).setVGap(5);

  			//---- label2 ----
  			label2.setText("Size Width");
  			label2.setHorizontalAlignment(SwingConstants.RIGHT);
  			panel1.add(label2, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfSizeW ----
  			ttfSizeW.setText("512");
  			panel1.add(ttfSizeW, new TableLayoutConstraints(1, 0, 1, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- label4 ----
  			label4.setText("Latitude");
  			label4.setHorizontalAlignment(SwingConstants.RIGHT);
  			panel1.add(label4, new TableLayoutConstraints(2, 0, 2, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfLat ----
  			//ttfLat.setText("38.931099");
  			panel1.add(ttfLat, new TableLayoutConstraints(3, 0, 3, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- btnGetMap ----
  			btnGetMap.setText("Get Map");
  			btnGetMap.setHorizontalAlignment(SwingConstants.LEFT);
  			btnGetMap.setMnemonic('G');
  			btnGetMap.addActionListener(new ActionListener() {
  				public void actionPerformed(ActionEvent e) {
  					startTaskAction();
  				}
  			});
  			panel1.add(btnGetMap, new TableLayoutConstraints(5, 0, 5, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- label3 ----
  			label3.setText("Size Height");
  			label3.setHorizontalAlignment(SwingConstants.RIGHT);
  			panel1.add(label3, new TableLayoutConstraints(0, 1, 0, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfSizeH ----
  			ttfSizeH.setText("512");
  			panel1.add(ttfSizeH, new TableLayoutConstraints(1, 1, 1, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- label5 ----
  			label5.setText("Longitude");
  			label5.setHorizontalAlignment(SwingConstants.RIGHT);
  			panel1.add(label5, new TableLayoutConstraints(2, 1, 2, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfLon ----
  			//ttfLon.setText("-77.3489");
  			panel1.add(ttfLon, new TableLayoutConstraints(3, 1, 3, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- btnQuit ----
  			btnQuit.setText("Quit");
  			btnQuit.setMnemonic('Q');
  			btnQuit.setHorizontalAlignment(SwingConstants.LEFT);
  			btnQuit.setHorizontalTextPosition(SwingConstants.RIGHT);
  			btnQuit.addActionListener(new ActionListener() {
  				public void actionPerformed(ActionEvent e) {
  					quitProgram();
  				}
  			});
  			panel1.add(btnQuit, new TableLayoutConstraints(5, 1, 5, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- label1 ----
  			label1.setText("License Key");
  			label1.setHorizontalAlignment(SwingConstants.RIGHT);
  			panel1.add(label1, new TableLayoutConstraints(0, 2, 0, 2, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfLicense ----
  			ttfLicense.setToolTipText("Enter your own URI for a file to download in the background");
  			panel1.add(ttfLicense, new TableLayoutConstraints(1, 2, 1, 2, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- label6 ----
  			label6.setText("Zoom");
  			label6.setHorizontalAlignment(SwingConstants.RIGHT);
  			panel1.add(label6, new TableLayoutConstraints(2, 2, 2, 2, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfZoom ----
  			ttfZoom.setText("14");
  			panel1.add(ttfZoom, new TableLayoutConstraints(3, 2, 3, 2, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  		}
  		contentPanel.add(panel1, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  		//Suventhan's Code
  		/*
  		 * Creating the north panel to hold checkbox, checkbox and labels
  		 */
  		{
  			
  			panelsuv.setOpaque(false);
  			panelsuv.setBorder(new CompoundBorder(
  				new TitledBorder("Option 1: Select a defualt map location"),
  				Borders.DLU2_BORDER));
  			panelsuv.setLayout(new TableLayout(new double[][] {
  				{0.15, 0.08, 0.10, 0.10, 0.10,0.08,0.15,0.15,0.08, TableLayout.FILL},
  				{TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED}}));
  			((TableLayout)panelsuv.getLayout()).setHGap(5);
  			((TableLayout)panelsuv.getLayout()).setVGap(5);
  		
  			//==========ComboBox=======
  				// Setting up the ComboBox 
  			pickComboBox = new JComboBox(cities);
  			pickComboBox.setEditable( false );
  			pickComboBox.setMaximumRowCount( 5 );
  			pickComboBox.firePopupMenuWillBecomeVisible();
  			pickComboBox.insertItemAt( "SELECT A LOCATION", 0 );  
  			pickComboBox.setSelectedIndex( 0 );
  			  			
  			//adding the combobox to the north panel
  			panelsuv.add(pickComboBox, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  			
  			//=========Label======
  					//Aayush's Code
  						//a Jlabel indicating when the save button is
  			jlblStatus= new JLabel("Click to save the map");  
  			jlblStatus.setForeground(Color.red);
  			jlblStatus.setHorizontalAlignment(SwingConstants.CENTER);
  			
  			
  			panelsuv.add(jlblStatus, new TableLayoutConstraints(7, 0, 7, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  					//Aayush's code ended
  			//Suventhan's code
  			//========OK==========
  				//Creating a OK button to connect a actionlistener to it
  			okButton.setText("OK");
  			okButton.setHorizontalAlignment(SwingConstants.CENTER);
  		
  			//actionlistener for the OK button
  			okButton.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e) {
					
			  			 
						// Getting the string from the combobox
				nameL = (String)pickComboBox.getSelectedItem();
				//checking which option the user has selected
				if(nameL.equals("Toronto")==true){
				
					//writing the location manually to each destination and check for errors if it happens   
				try {
					
						 ttfLat.setText("43.670906");
					      ttfLon.setText("-79.393331");
					    _task.execute();
					  }
					  catch (TaskException e1) {
					    sout(e1.getMessage());
					  }
					}
					//next
				else if(nameL.equals("CN Tower")==true){
					
				
				try {
					 ttfLat.setText("43.643865");
				      ttfLon.setText("-79.3885451");
				    _task.execute();
				  }
				  catch (TaskException e1) {
				    sout(e1.getMessage());
				  }
				
				}
				else if(nameL.equals("Ontario Museum")==true){
					
					
					try {
						 ttfLat.setText("43.667313");
					      ttfLon.setText("-79.393636");
					    _task.execute();
					  }
					  catch (TaskException e1) {
					    sout(e1.getMessage());
					  }
					
					}
					else if(nameL.equals("Ontario Science Centre")==true){
					
					
					try {
						 ttfLat.setText("43.721802");
					      ttfLon.setText("-79.342717");
					    _task.execute();
					  }
					  catch (TaskException e1) {
					    sout(e1.getMessage());
					  }
					
					}
					else if(nameL.equals("Seneca@York")==true){
						
						
						try {
							 ttfLat.setText("43.770390");
						      ttfLon.setText("-79.500750");
						    _task.execute();
						  }
						  catch (TaskException e1) {
						    sout(e1.getMessage());
						  }
						
						}
					else if(nameL.equals("Seneca newnham")==true){
						
						
						try {
							ttfLat.setText("43.794544");
						      ttfLon.setText("-79.346623");
						    _task.execute();
						  }
						  catch (TaskException e1) {
						    sout(e1.getMessage());
						  }
						
						}
						else if(nameL.equals("Montreal")==true){
						
						
						try {
							ttfLat.setText("45.536482");
						      ttfLon.setText("-73.592702");
						    _task.execute();
						  }
						  catch (TaskException e1) {
						    sout(e1.getMessage());
						  }
						
						}
						else if(nameL.equals("Vancouver")==true){
							
							
							try {
								ttfLat.setText("49.253976");
							      ttfLon.setText("-123.108091");
							    _task.execute();
							  }
							  catch (TaskException e1) {
							    sout(e1.getMessage());
							  }
							
							}
							else if(nameL.equals("London")==true){
							
							
							try {
								ttfLat.setText("42.980791");
							      ttfLon.setText("-81.246983");
							    _task.execute();
							  }
							  catch (TaskException e1) {
							    sout(e1.getMessage());
							  }
							
							}
							else if(nameL.equals("Ottawa")==true){
								
								
								try {
									ttfLat.setText("45.393348");
								      ttfLon.setText("-75.695610");
								    _task.execute();
								  }
								  catch (TaskException e1) {
								    sout(e1.getMessage());
								  }
								
								}
								else if(nameL.equals("WonderLand")==true){
								
								
								try {
									ttfLat.setText("43.839232");
								      ttfLon.setText("-79.534436");
								    _task.execute();
								  }
								  catch (TaskException e1) {
								    sout(e1.getMessage());
								  }
								
								}
								else if(nameL.equals("Niagara Falls")==true){
									
									
									try {
										ttfLat.setText("43.097288");
									      ttfLon.setText("-79.095321");
									    _task.execute();
									  }
									  catch (TaskException e1) {
									    sout(e1.getMessage());
									  }
									
									}
				
					//
					}
  			});
  			panelsuv.add(okButton, new TableLayoutConstraints(1, 0, 1, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  			//Suventhan's code ended
  			
  				//Aayush's Code
  			//========SAVE==========
  				//Creating a save button that allows user to save the image
  			saveButton.setText("SAVE");
  			saveButton.setHorizontalAlignment(SwingConstants.CENTER);
  				//acionlistener for the save button
  			saveButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					//Create an image
					BufferedImage bi= _img;
					//setting the file name as location.png
					 File outputfile=new File(nameL+ ".png");
					 //connecting the File to a JFileChooser and get the selected file
				      saveinput.setSelectedFile(outputfile);
				      //Return value if approve is chosen
					if(saveinput.showSaveDialog(dialogPane) == JFileChooser.APPROVE_OPTION){
						//showing the location.png in the save name, in case the user doesn't want to type themself
						outputfile=saveinput.getSelectedFile();
					try {
						//writing the image as location.png
						ImageIO.write(bi, "png", outputfile);
					}
					//catch any error that may happen
					catch(IOException ex){
						
					}

					       // jlblStatus.setText("Error");
					     
					      
					}
				}
					
				
  			});
				
  			panelsuv.add(saveButton, new TableLayoutConstraints(8, 0, 8, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  				
  			
  		}
  		contentPane2.add(panelsuv, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  		//======SOUTH PANEL=====
  		/*
  		 * Creating a Instruction JTextArea to allow user to understand the GUI and what it does better
  		 */
  		{
  			contentPane3.setBorder(new CompoundBorder(new TitledBorder("Instruction"), Borders.DLU7_BORDER));
  			contentPane3.setEditable(false);
  			contentPane3.setForeground(Color.BLUE);
  			contentPane3.append("WELCOME TO THE TEAM 2 EDITED GOOGLE STATIC MAP"+"\n"+"\n");
  			contentPane3.append("Option 1: Use the ComboBox and select a location"+"\n");
  			contentPane3.append("         - If you wish to save the map, click the save button on the top right corner"+"\n");
  			contentPane3.append("         - Click the move up or move down button to move around the map"+"\n");
  			contentPane3.append("         - Note: The image file's extension will be saved as .png "+"\n");
  			contentPane3.append("Option 2: Configure the inputs to Google Static Maps"+"\n");
  			contentPane3.append("         - Enter the latitude and longitude of the location you desire"+"\n");

  			
  		}
  		//Aayush's Code's ended
  		
  		//Suventhan's code
  		//-----side map panel-----
  		/*
  		 * Creating a Jpanel to display the map on the same screen
  		 */
  		{
  	  		mapPanel=new JPanel();
  	  		mapPanel.setOpaque(false);
  	  		mapPanel.setBorder(new CompoundBorder(new TitledBorder("MAP"), Borders.DLU7_BORDER));
  	  		mapPanel.setLayout(new BorderLayout());
  		}
  	  		contentPaneM.add(mapPanel, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  	  		//Suventhan's code ended
  	  		//Nan's code
  	  		/*
  	  		 * Creating UP,DOWN,LEFT & RIGHT button that will move the map according to the user selections
  	  		 */
  	  		{
  	  		newD.setOpaque(false);
  	    	newD.setLayout(new BorderLayout());
  	    	newD.setSize(80, 100);
  	    	/*
  	  		newD.setLayout(new TableLayout(new double[][] {
  	  				{0.15, 0.15, TableLayout.FILL},
  	  				{TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED}}));
  	  				*/
  	  			centerPanel.setText("Move UP");
  	  			centerPanel.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					/*
					 * get the value of ttflat and ttflon. convert them into double and reenter the value to get a new coordination
					 * so that it can recreate a map which is little up from the orginal map
					 */
					//String declaration
					double newlat;
					double newlon;
					//converting the string to double
					newlat=Double.parseDouble(ttfLat.getText());
					newlon=Double.parseDouble(ttfLon.getText());
					//add values according to the directions
					newlat = newlat+0.005;
					newlon = newlon-0.005;
					
					try {
						//convering double to string and passing it to ttflat
					ttfLat.setText(Double.toString(newlat));
				      ttfLon.setText(Double.toString(newlon));
				    _task.execute();
				}
				  catch (TaskException e1) {
				    sout(e1.getMessage());
				  }

					}
				
  			});
  	  		
  	  	centerPaneld.setText("Move DOWN");

  	  	//Actionlistener for the move down button
  	  centerPaneld.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				
				double newlat;
				double newlon;
				newlat=Double.parseDouble(ttfLat.getText());
				newlon=Double.parseDouble(ttfLon.getText());
				newlat = newlat-0.005;
				newlon = newlon-0.005;
				
				try {
				ttfLat.setText(Double.toString(newlat));
			      ttfLon.setText(Double.toString(newlon));
			    _task.execute();
			}
			  catch (TaskException e1) {
			    sout(e1.getMessage());
			  }

				}
			
		});
  	centerPanell.setText("LEFT");
		centerPanell.addActionListener(new ActionListener(){
	public void actionPerformed(ActionEvent e) {
		/*
		 * get the value of ttflat and ttflon. convert them into double and reenter the value to get a new coordination
		 * so that it can recreate a map which is little up from the orginal map
		 */
		//String declaration
		double newlat;
		double newlon;
		//converting the string to double
		newlat=Double.parseDouble(ttfLat.getText());
		newlon=Double.parseDouble(ttfLon.getText());
		//add values according to the directions
		newlat = newlat;
		newlon = newlon-0.010;
		
		try {
			//convering double to string and passing it to ttflat
		ttfLat.setText(Double.toString(newlat));
	      ttfLon.setText(Double.toString(newlon));
	    _task.execute();
	}
	  catch (TaskException e1) {
	    sout(e1.getMessage());
	  }

		}
	
	});
		centerPanelr.setText("RIGHT");
			centerPanelr.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			/*
			 * get the value of ttflat and ttflon. convert them into double and reenter the value to get a new coordination
			 * so that it can recreate a map which is little up from the orginal map
			 */
			//String declaration
			double newlat;
			double newlon;
			//converting the string to double
			newlat=Double.parseDouble(ttfLat.getText());
			newlon=Double.parseDouble(ttfLon.getText());
			//add values according to the directions
			newlat = newlat;
			newlon = newlon+0.010;
			
			try {
				//convering double to string and passing it to ttflat
			ttfLat.setText(Double.toString(newlat));
		      ttfLon.setText(Double.toString(newlon));
		    _task.execute();
		}
		  catch (TaskException e1) {
		    sout(e1.getMessage());
		  }

			}
		
		});
    	


  	  		}
  	  		//postion the button in a borderlayout
  	  	newD.add(centerPanel, BorderLayout.NORTH);
    	newD.add(centerPaneld, BorderLayout.SOUTH);
    	newD.add(centerPanell, BorderLayout.WEST);
    	newD.add(centerPanelr, BorderLayout.EAST);


  	  		
  	  		//Nan's code ended
  	  		//Suventhan's code 
  		
  		
  		//======== scrollPane1 ========
  		{
  			scrollPane1.setBorder(new TitledBorder("System.out - displays all status and progress messages, etc."));
  			scrollPane1.setOpaque(false);

  			//---- ttaStatus ----
  			ttaStatus.setBorder(Borders.createEmptyBorder("1dlu, 1dlu, 1dlu, 1dlu"));
  			ttaStatus.setToolTipText("<html>Task progress updates (messages) are displayed here,<br>along with any other output generated by the Task.<html>");
  			scrollPane1.setViewportView(ttaStatus);
  		}
  		contentPanel.add(scrollPane1, new TableLayoutConstraints(0, 1, 0, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  		//======== panel2 ========
  		{
  			panel2.setOpaque(false);
  			panel2.setBorder(new CompoundBorder(
  				new TitledBorder("Status - control progress reporting"),
  				Borders.DLU2_BORDER));
  			panel2.setLayout(new TableLayout(new double[][] {
  				{0.45, TableLayout.FILL, 0.45},
  				{TableLayout.PREFERRED, TableLayout.PREFERRED}}));
  			((TableLayout)panel2.getLayout()).setHGap(5);
  			((TableLayout)panel2.getLayout()).setVGap(5);

  			//======== panel3 ========
  			{
  				panel3.setOpaque(false);
  				panel3.setLayout(new GridLayout(1, 2));

  				//---- checkboxRecvStatus ----
  				checkboxRecvStatus.setText("Enable \"Recieve\"");
  				checkboxRecvStatus.setOpaque(false);
  				checkboxRecvStatus.setToolTipText("Task will fire \"send\" status updates");
  				checkboxRecvStatus.setSelected(true);
  				panel3.add(checkboxRecvStatus);

  				//---- checkboxSendStatus ----
  				checkboxSendStatus.setText("Enable \"Send\"");
  				checkboxSendStatus.setOpaque(false);
  				checkboxSendStatus.setToolTipText("Task will fire \"recieve\" status updates");
  				panel3.add(checkboxSendStatus);
  			}
  			panel2.add(panel3, new TableLayoutConstraints(0, 0, 0, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- ttfProgressMsg ----
  			ttfProgressMsg.setText("Loading map from Google Static Maps");
  			ttfProgressMsg.setToolTipText("Set the task progress message here");
  			panel2.add(ttfProgressMsg, new TableLayoutConstraints(2, 0, 2, 0, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- progressBar ----
  			progressBar.setStringPainted(true);
  			progressBar.setString("progress %");
  			progressBar.setToolTipText("% progress is displayed here");
  			panel2.add(progressBar, new TableLayoutConstraints(0, 1, 0, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));

  			//---- lblProgressStatus ----
  			lblProgressStatus.setText("task status listener");
  			lblProgressStatus.setHorizontalTextPosition(SwingConstants.LEFT);
  			lblProgressStatus.setHorizontalAlignment(SwingConstants.LEFT);
  			lblProgressStatus.setToolTipText("Task status messages are displayed here when the task runs");
  			panel2.add(lblProgressStatus, new TableLayoutConstraints(2, 1, 2, 1, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  		}
  		contentPanel.add(panel2, new TableLayoutConstraints(0, 2, 0, 2, TableLayoutConstraints.FULL, TableLayoutConstraints.FULL));
  	
  	}
  	dialogPane.add(contentPaneM, BorderLayout.WEST);
  	dialogPane.add(contentPanel, BorderLayout.EAST);
  	dialogPane.add(contentPane2, BorderLayout.NORTH);
  	dialogPane.add(contentPane3, BorderLayout.SOUTH);
  	dialogPane.add(newD, BorderLayout.CENTER);
  }
  contentPane.add(dialogPane, BorderLayout.CENTER);
  setSize(1275, 685);
  setLocationRelativeTo(null);
  // JFormDesigner - End of component initialization  //GEN-END:initComponents
}

// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
// Generated using JFormDesigner non-commercial license
private JPanel dialogPane;
private JPanel mapPanel;
private JPanel contentPanel;
private JPanel contentPane2;
private JPanel contentPaneM;
private JPanel newD;
private JTextArea contentPane3;
private JPanel centerPanelC;
private JPanel panel1;
private JPanel panelsuv;
private JTextArea panelsouth;
private JPanel panelMap;
private JLabel label2;
private JTextField ttfSizeW;
private JLabel label4;
private JTextField ttfLat;
private JButton btnGetMap;
private JButton btnGetTorMap;
private JButton btnGetSenecaYMap;
private JButton btnGetSenecaNMap;
private JButton btnGetEfTowerMap;
private JButton btnGetIndiaMap;
private JButton btnGetStauteMap;
private JButton btnGetGizaPMap;
private JButton btnGetCnTowerMap;
private JButton okButton;
private JButton saveButton;
private JButton centerPanel;
private JButton centerPaneld;
private JButton centerPanell;
private JButton centerPanelr;
private JComboBox pickComboBox;
private JLabel label3;
private JTextField ttfSizeH;
private JLabel label5;
private JTextField ttfLon;
private JButton btnQuit;
private JLabel label1;
private JTextField ttfLicense;
private JLabel label6;
private JTextField ttfZoom;
private JScrollPane scrollPane1;
private JTextArea ttaStatus;
private JPanel panel2;
private JPanel panel3;
private JCheckBox checkboxRecvStatus;
private JCheckBox checkboxSendStatus;
private JTextField ttfProgressMsg;
private JProgressBar progressBar;
private JLabel lblProgressStatus;
private JLabel jlblStatus;
private JFileChooser saveinput;
// JFormDesigner - End of variables declaration  //GEN-END:variables

	
 }




