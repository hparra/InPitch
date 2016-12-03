/*
   This file is a part of InPitch - a simple ear-training program
   for note recognition. Copyright (C) 2006, 2016 Chris Gilbreth, Hector Parra.

   See the file InPitch.java and COPYING.txt for further copyright and
   contact information.
*/

//import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
//import javax.swing.event.*;
import javax.sound.midi.spi.*;
import javax.sound.midi.*;
import java.util.*;
import java.net.URL;
import java.net.URI;
import java.io.*;
//import java.io.File;
//import java.io.IOException;

public class MidiBank implements SoundBank
{
    // Midi Control Codes
    final int CC_VOLUME = 7;
    final int CC_REVERB = 91;
    final int CC_MODULATION = 1;
    final int CC_SUSTAIN_PEDAL = 64;
    final int CC_RELEASE_TIME = 72;
    final int CC_ATTACK = 73;

    JMenu midiMenu, subMenu;

    int lastKey = -1;

    int highestKey = 96;
    int lowestKey = 24;

    Vector currentKeys;

    Sequencer sequencer;
    Sequence sequence;
    Synthesizer synthesizer;
    Instrument instruments[];
    ChannelData channels[];
    ChannelData currentChannel;
    SoundbankReader reader;
    long startTime;
    File soundbankfile;
    Soundbank soundbank;

    InPitchMain ipm;

    Random random;

    public MidiBank(InPitchMain inpitch)
    {
	ipm = inpitch;
	random = new Random();
	currentKeys = new Vector();
    }

    public void open() throws SoundbankException
    {
	try {
	    if (synthesizer == null) {
		if ((synthesizer = MidiSystem.getSynthesizer()) == null) {
		    System.out.println("getSynthesizer() failed!");
		    return;
		}
	    }
	    synthesizer.open();
	    sequencer = MidiSystem.getSequencer();
	    sequence = new Sequence(Sequence.PPQ, 10);
	    // Load MIDI SoundFont from .jar file
	    InputStream is = this.getClass().getResourceAsStream("/Nice-Keys-B-JNv1.5.sf2");
	    InputStream buffered = new BufferedInputStream(is);
	    soundbank = MidiSystem.getSoundbank(buffered);
	}
	catch (Exception ex) {
	    ex.printStackTrace();
	    return;
	}

	//Soundbank soundbank = synthesizer.getDefaultSoundbank();
	if (soundbank != null)
	    {
		instruments = soundbank.getInstruments();
		synthesizer.loadInstrument(instruments[0]);
		//System.out.println("Number of instruments: " + instruments.length);
		// loading piano default
	    }
	else
	    {
		//System.err.println("Couldn't load soundbank!");
		//System.exit(1);

		throw new SoundbankException();
	    }
	MidiChannel midiChannels[] = synthesizer.getChannels();
	channels = new ChannelData[midiChannels.length];
	for (int i = 0; i < channels.length; i++)
	    {
		channels[i] = new ChannelData(midiChannels[i], i);
	    }
	currentChannel = channels[0];

	currentChannel.channel.controlChange(CC_RELEASE_TIME, 0);
	currentChannel.channel.controlChange(CC_ATTACK, 14);
	currentChannel.channel.controlChange(CC_VOLUME, 256);

    }

    public void close()
    {
	if (synthesizer != null)
	    synthesizer.close();

	if (sequencer != null)
	    sequencer.close();

	sequencer = null;
	synthesizer = null;
	instruments = null;
	channels = null;
    }

    // Sets the available notes to play. This function takes a vector of strings
    // and converts it to a vector of keys (integers) and saves that instead.
    public void setNotes(Vector notes) throws Exception
    {
	currentKeys.clear();
	for (int i = 0; i < notes.size(); ++i)
	    {
		// add all octaves of the given note that are in our range.
		int key = noteToKey((String) notes.get(i)); // gets lowest (smallest int val) key
		// for the note
		while (key < lowestKey)
		    key += 12;
		while (key <= highestKey)
		    {
			currentKeys.add(new Integer(key));
			key += 12;
		    }
	    }
    }

    public static int noteToKey(String s) throws Exception
    {
	for (int i = 0; i < 12; ++i)
	    if (s.equals(InPitch.notes[i]))
		return i;

	throw new Exception("Invalid note passed to noteToKey: " + s);
    }

    public String playRandomNote()
    {
	// random key. All octaves are represented in currentKeys
	Integer randomKey = (Integer) currentKeys.get(random.nextInt(currentKeys.size()));
	playNote(randomKey.intValue());
	return noteName(randomKey.intValue());
    }

    // not used in the program
    public int playNextNote()
    {
	++lastKey;
	playNote(lastKey);
	return lastKey;
    }

    class PlayThread extends Thread
    {
	int key;

	public PlayThread(int key)
	{
	    this.key = key;
	}

	public void run()
	{
	    try
		{
		    currentChannel.channel.noteOn(key, currentChannel.velocity);
		    Thread.sleep(6000);
		    currentChannel.channel.noteOff(key, currentChannel.velocity);
		}
	    catch (InterruptedException ex)
		{
		    currentChannel.channel.noteOff(key, currentChannel.velocity);
		    System.err.println("PlayThread interrupted prematurely...");
		    System.out.println("PlayThread interrupted prematurely...");
		}
	}
    }

    private void playNote(int key)
    {
	currentChannel.channel.allNotesOff();
	lastKey = key;
	PlayThread thread = new PlayThread(key);
	thread.start();
    }

    public JMenu createMidiMenu(ButtonGroup group)
    {
	String names[] =
	    {
		"Piano",
		"Chromatic Perc.",
		"Organ",
		"Guitar",
		"Bass",
		"Strings",
		"Ensemble",
		"Brass",
		"Reed",
		"Pipe",
		"Synth Lead",
		"Synth Pad",
		"Synth Effects",
		"Ethnic",
		"Percussive",
		"Sound Effects" };


	JRadioButtonMenuItem menuItem;
	ButtonGroup midiGroup = group;

	midiMenu = new JMenu("MIDI");

	//for (int i = 0; i < names.length; ++i) {
	//for (int k = 0; k < instruments.length; ++k) {
	for (int k = 0; k < 32; ++k) {
	    //subMenu = new JMenu(names[i]);
	    try {

		//for (int k = 0; k < 8; ++k) { // only show first 8 of each type
		//menuItem = new JRadioButtonMenuItem(instruments[i * 8 + k].getName());
		menuItem = new JRadioButtonMenuItem(instruments[k].getName());

		if (k == 0) menuItem.setSelected(true);

		menuItem.setActionCommand((new Integer(k)).toString());
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
			    switchIPMToMidi();
			    int thisInstrument = Integer.parseInt(e.getActionCommand());
			    switchInstrument(thisInstrument);

			    for (int item=0; item < midiMenu.getItemCount(); ++item) {
				if (thisInstrument != item) {
				    midiMenu.getItem(item).setSelected(false);
				}
			    }
			}
		    });
		//midiGroup.add(menuItem);
		//midiMenu.add(subMenu);
		//subMenu.add(menuItem);
		midiMenu.add(menuItem);
	    }
	    catch (Exception e) {
		System.err.println(
				   "Exception caught: " + e.getMessage() + e.getStackTrace());
	    }
	    //midiMenu.add(subMenu);
	}

	return midiMenu;
    }

    class ChannelData
    {
	MidiChannel channel;
	boolean solo, mono, mute, sustain;
	int row, col, num;
	int velocity;

	public ChannelData(MidiChannel channel, int num)
	{
	    this.channel = channel;
	    this.num = num;
	    sustain = false;
	    velocity = 32;
	}

    } // End class ChannelData

    public void switchInstrument(int i)
    {
	//synthesizer.loadInstrument(instruments[Integer.parseInt(e.getActionCommand())]);
	synthesizer.loadInstrument(instruments[i]);
	currentChannel.channel.programChange(i);
	//System.out.println(
	//	"switchInstrument: index: " + i + " string: " + instruments[i]);
    }

    public String noteName(int key)
    {
	return InPitch.notes[key % 12];
    }

    public void repeat()
    {
	if (lastKey > 0)
	    playNote(lastKey);
    }

    public void switchIPMToMidi()
    {
	ipm.switchToMidi();
    }

}
