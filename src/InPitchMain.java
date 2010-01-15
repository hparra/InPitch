/* 
This file is a part of InPitch - a simple ear-training program
for note recognition. Copyright (C) 2006 Chris Gilbreth, Hector Parra.

See the file InPitch.java and COPYING.txt for further copyright and
contact information.
*/

// InPitchMain - Main window

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;

public class InPitchMain extends  JFrame
{
    private Container c;

    private JPanel mainPanel, titlePanel, buttonGrid, controlPanel;
	
	private JButton playButton;
	
	private JPanel levelPanel;
	private JComboBox levelBox;
	
    private JLabel title;
    private JLabel statusBar;

    private String lastNoteName;
    
    private String levelList[] = {"Level 1", "Level 2", "Level 3", "Level 4",
			  "Level 5", "Level 6", "Level 7", "Level 8",
			  "Level 9", "Level 10", "Level 11"};
    
    private String answer = "";


	public static void showAboutBox()
	{
	    final String aboutString =
	        "InPitch 0.5, Copyright (c) 2006, Chris Gilbreth, Hector Parra.\n\n" +
            "InPitch comes with ABSOLUTELY NO WARRANTY; for details\n" +
            "see the file COPYING.txt.  This is free software, and you are\n" +
            "welcome to redistribute it under certain conditions; see\n" +
            "COPYING.txt for details.";
	
	    new Thread(new Runnable()
	    {
	        public void run()
	        {
	            JOptionPane.showMessageDialog(
	                null,
	                aboutString,
	                "About InPitch",
	                JOptionPane.INFORMATION_MESSAGE);
	        }
	    }).start();
	}

    // some state flags. Each button needs to consider all of the different possible states
    private boolean listenMode = false; // not implemented yet
    private boolean question = false;
    private boolean begun = false;
	private boolean alreadyWrong = false;

    private int wrongMessage = 0;

    private WaveBank waveBank;
    private SoundBank bank;
    private MidiBank midiBank;
    private SoundTrainer trainer;

    private int level = -1;


	// Helper function that reports a correct guess to the SoundTrainer.
	// see comments for wrong() below.
	// Also makes the play button active
    public void correct(String label)
    {
        try
        {
        	if (!alreadyWrong)
            	trainer.reportCorrect(label);
            playButton.setEnabled(true);
        }
        catch (Exception e)
        {
            //System.out.println("erk!");
            e.printStackTrace();
            System.exit(1);
        }
    }


	// helper function that reports a wrong guess to the SoundTrainer.
	// Needed to allow the inner classes for the buttons to access the
	// trainer (I don't entirely understand how this works. I could use
	// a Java book)
    public void wrong(String label)
    {
        try
        {
        	if (!alreadyWrong)
            	trainer.reportWrong(label);
        }
        catch (Exception e)
        {
            //System.out.println("erk!");
            e.printStackTrace();
            System.exit(1);
        }
    }

	// get the notes the student is working on from the SoundTrainer and 
	// set them to be our current set of notes. Also grey out the buttons
	// for the notes we're not using, and enable the ones we are.
	// Returns true if the set of notes changed (the student advanced).
    public void setNotesFromTrainer()
    {
        try
        {
//			if (level == trainer.levelID())
//						return false;
        	
            level = trainer.levelID();
			
            Vector notes = trainer.getNotes();
            bank.setNotes(trainer.getNotes());
            
            Component buttons[] = buttonGrid.getComponents();

            for (int i = 0; i < buttons.length; ++i)
            {
                JButton button = (JButton) buttons[i];
                if (!notes.contains(button.getText()))
                    button.setEnabled(false);
                else
                    button.setEnabled(true);
            }
            
            levelBox.setSelectedIndex(level);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void setLevel(int newLevel)
    {
    	try {
			trainer.setLevel(newLevel);
			setNotesFromTrainer();
			restartTestingGui();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public InPitchMain(SoundTrainer trainer)
    {
        midiBank = new MidiBank(this);
        waveBank = new WaveBank();
        bank = midiBank;
        
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });

        try {
            bank.open();
        }
        catch (SoundbankException e)
        {
            // bring up an error window and alert the user to
            // install the soundbank, etc.
            JOptionPane.showMessageDialog(this.getContentPane(),
                    "Error loading the MIDI soundbank. If you have not already done so,\n" +
                    "You will need to download the file soundbank-deluxe.gm from Sun's website at\n" +
                    "\n" +
                    "http://java.sun.com/products/java-media/sound/soundbank-deluxe.gm.zip\n" +
                    "\n" +
                    "Unzip the file and place soundbank-deluxe.gm in the InPitch directory\n" +
                    "(where InPitch.exe is located).\n" +
                    "\n" +
                    "See README.txt for more info. If you still have problems visit our forums at\n" +
                    "\n" +
                    "http://sf.net/projects/inpitch/\n", "MIDI Soundbank Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        this.trainer = trainer;
        //this.set = set;

        mainPanel = new JPanel();

        // menu
        this.setJMenuBar(createMenuBar());

        // title panel
        titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout());
        title = new JLabel("InPitch Pitch Training Engine");
        titlePanel.add(title);

        // status bar
        // needs to be before levelBox, as the levelBox selections
        // call setLevel() which calls restartTestingGui() which sets
        // the status bar...
        statusBar = new JLabel("Press \"Play\" to begin.");
        
        // button grid (notes)
        buttonGrid = new JPanel();
        buttonGrid.setLayout(new GridLayout(4, 3));

        // note buttons inside the grid
        JButton button;
        for (int i = 0; i < InPitch.notes.length; ++i)
        {
            button = new JButton(InPitch.notes[i]);
            button.setFocusPainted(false);

            // TODO: This inner class is too hard to read.
            // Anyway here is the button logic.
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    String label = ((JButton) e.getSource()).getText();

                    if (!begun)
                        return;
                    if (question)
                    {
//                        if (label.equals(InPitch.notes[lastKeyPlayed % 12]))
                        if (label.equals(lastNoteName))
                        {
                            // right answer
                            statusBar.setText("Correct!! :D  The note is " + label);
                            correct(label);
                            alreadyWrong = false;
                            advanceIfPossible();
//                            if (setNotesFromTrainer()) // we advanced
//                            	statusBar.setText("Your mind feels more agile.");
                            
                            question = false;
                        }
                        else
                        {
                            // wrong answer
                            wrong(lastNoteName);
                            alreadyWrong = true;
                            if (wrongMessage == 0)
                            {
                                statusBar.setText("Wrong!! x_x");
                                wrongMessage = 1;
                            }
                            else if (wrongMessage == 1)
                            {
                                statusBar.setText("Not that one :(");
                                wrongMessage = 0;
                            }
                        }
                    }
                    else
                    {
						// Do nothing
                    }
                }
            });

            buttonGrid.add(button);
        }

        // control panel
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        // repeat button
        button = new JButton("Repeat");
        button.setFocusPainted(false);
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!begun)
                    return;
                // if (question) // repeat button doesn't care
                //midiBank.playNote(lastKeyPlayed);
                bank.repeat();
            }
        });
        controlPanel.add(button);

        // play button
        button = new JButton("Play ->>");
        button.setFocusPainted(false);
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!begun)
                    begun = true;
                if (!question)
                {
                    question = true;
                    lastNoteName = bank.playRandomNote();
                    
                    statusBar.setText("What is this note?");
                    //System.out.println("Note: " + lastKeyPlayed);
                    playButton.setEnabled(false);
                }
                else
                {
                	// not reached unless the user clicked the play button again
                	// before it was disabled (a VERY quick double click?)
                	// So do nothing.
                }
            }
        });
        controlPanel.add(button);
        button.setSelected(true);
		playButton = button;

		// level chooser
        levelPanel = new JPanel();
        levelPanel.setLayout(new FlowLayout());
		

        
		levelBox = new JComboBox(levelList);
		levelBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JComboBox box = (JComboBox) e.getSource();
				String label = (String) box.getSelectedItem();
				//System.out.println(label);
				for (int i = 0; i < 11; ++i)
				{
					if (label.equals(levelList[i]))
					{
						setLevel(i + 1);
					}
				}
			}
		});
		
        levelPanel.add(levelBox);
		
		// this disables the buttons we're not using, as well as 
		// tells the midibank what notes to use.
		setNotesFromTrainer();

        // this panel
        mainPanel.add(titlePanel);
        mainPanel.add(buttonGrid);
        mainPanel.add(controlPanel);
        mainPanel.add(levelPanel);

        this.getContentPane().add(statusBar, BorderLayout.SOUTH);

        
        
        this.getContentPane().add(mainPanel);
    }


    private JMenuBar createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu, soundMenu, levelMenu, helpMenu, orderingMenu;

        JMenuItem item;

        // File menu
        fileMenu = new JMenu("File");
        fileMenu.add("Load Custom Trainer");
        fileMenu.add("Quit InPitch");
        //menuBar.add(fileMenu);

        // Sound menu
        soundMenu = new JMenu("Sound");
        ButtonGroup group = new ButtonGroup();
        soundMenu.add((JMenuItem) midiBank.createMidiMenu(group));
        
        JRadioButtonMenuItem waveItem = new JRadioButtonMenuItem("Wave");
        waveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                switchToWave();
            }
        });
        
        // Take out the option for recorded sound for the moment
        //soundMenu.add(waveItem);
        group.add(waveItem);
        //soundMenu.add("Custom...");
        menuBar.add(soundMenu);

        // Ordering menu
        orderingMenu = new JMenu("Note Order");
        
        JMenuItem ordering1 = new JMenuItem("A, E, C, F#, D ...");
        ordering1.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		switchOrdering(1);
        	}
        });
        
        JMenuItem ordering2 = new JMenuItem("F, G, D#, A#, C#, ...");
        ordering2.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		switchOrdering(2);
        	}
        });

        JMenuItem ordering3 = new JMenuItem("C, C#, D, D#, E, ...");
        ordering3.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		switchOrdering(3);
        	}
        });
        orderingMenu.add(ordering1);
        orderingMenu.add(ordering2);
        orderingMenu.add(ordering3);
        menuBar.add(orderingMenu);
        
        // Level menu
        levelMenu = new JMenu("Level");
		ButtonGroup levelGroup = new ButtonGroup();
		
        for (int i = 1; i <= 11; ++i)
        {
        	JRadioButtonMenuItem litem = new JRadioButtonMenuItem("Level " + Integer.toString(i));
        	litem.addActionListener(new ActionListener()
        	{
    			public void actionPerformed(ActionEvent e)
    			{
    				JMenuItem item = (JMenuItem) e.getSource();
    				String label = item.getLabel();
    				//System.out.println(label);
    				for (int i = 0; i < 10; ++i)
    				{
    					if (label.equals(levelList[i]))
    					{
    						setLevel(i + 1);
    					}
    				}
    			}
        	});
        	
        	if (i == 1)
        		litem.setSelected(true);
        	
        	levelGroup.add(litem);
        	levelMenu.add(litem);
        }
        
        //menuBar.add(levelMenu);

        // Help menu
        menuBar.add(Box.createHorizontalGlue());
        helpMenu = new JMenu("Help");
        //helpMenu.add("How do i use this?");
        //helpMenu.addSeparator();
        item = (JMenuItem) helpMenu.add(new JMenuItem("About InPitch"));
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                showAboutBox();
            }
        });
        menuBar.add(helpMenu);

        return menuBar;
    }

    public void switchOrdering(int ordering)
    {
    	
    	trainer.switchOrdering(ordering);
    	setLevel(1);
    	
    	// now, cancel the recent question
    	restartTestingGui();
    }
    
    // Take care of gui stuff so we ecan start testing again
    // for use after switching levels, orderings, etc.
    public void restartTestingGui()
    {
    	question = false;
    	begun = false;
    	statusBar.setText("Press \"Play\" to begin.");
        playButton.setEnabled(true);
    }
    
    public void advanceIfPossible()
    {
    	try {
			if (level == trainer.levelID())
				return;

			setNotesFromTrainer(); // we advanced
			statusBar.setText("Your mind feels more agile.");

		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void switchToMidi()
    {
        bank = midiBank;
        try
        {
            bank.setNotes(trainer.getNotes());
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        restartTestingGui();
    }
    
    public void switchToWave()
    {
        bank = waveBank;
        try
        {
            bank.setNotes(trainer.getNotes());
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        restartTestingGui();
    }

}
