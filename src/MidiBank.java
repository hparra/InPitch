/* 
This file is a part of InPitch - a simple ear-training program
for note recognition. Copyright (C) 2006 Chris Gilbreth, Hector Parra.

See the file InPitch.java and COPYING.txt for further copyright and
contact information.
*/

//import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
//import javax.swing.event.*;
import javax.sound.midi.*;
import java.util.*;

//import java.io.*;
//import java.io.File;
//import java.io.IOException;

public class MidiBank implements SoundBank
{ 
	final int PROGRAM = 192;
	final int NOTEON = 144;
	final int NOTEOFF = 128;
	final int SUSTAIN = 64;
	final int REVERB = 91;
	final int ON = 0, OFF = 1;

	int lastKey = -1;
	
	int highestKey = 108;
	int lowestKey = 24;
	
	Vector currentKeys;

	Sequencer sequencer;
	Sequence sequence;
	Synthesizer synthesizer;
	Instrument instruments[];
	ChannelData channels[];
	ChannelData currentChannel;
	long startTime;
	
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
		try
		{
			if (synthesizer == null)
			{
				if ((synthesizer = MidiSystem.getSynthesizer()) == null)
				{
					System.out.println("getSynthesizer() failed!");
					return;
				}
			}
			synthesizer.open();
			sequencer = MidiSystem.getSequencer();
			sequence = new Sequence(Sequence.PPQ, 10);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return;
		}

		Soundbank soundbank = synthesizer.getDefaultSoundbank();
		if (soundbank != null)
		{
			instruments = synthesizer.getDefaultSoundbank().getInstruments();
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
				Thread.sleep(5000);
				currentChannel.channel.noteOff(key, currentChannel.velocity);
			}
			catch (InterruptedException ex)
			{
				currentChannel.channel.noteOff(key, currentChannel.velocity);
				System.err.println("PlayThread interrupted prematurely...");
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

		JMenu midiMenu, subMenu;
		JRadioButtonMenuItem menuItem;
		ButtonGroup midiGroup = group;

		midiMenu = new JMenu("MIDI");

		for (int i = 0; i < names.length; ++i)
		{
			subMenu = new JMenu(names[i]);

			try
			{

				for (int k = 0; k < 8; ++k) // only show first 8 of each type
				{
					menuItem = new JRadioButtonMenuItem(instruments[i * 8 + k].getName());

					if ((i * 8 + k) == 0)
						menuItem.setSelected(true);

					menuItem.setActionCommand((new Integer(i * 8 + k)).toString());
					menuItem.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{	
						    switchIPMToMidi();
							switchInstrument(Integer.parseInt(e.getActionCommand()));
						}
					});
					midiGroup.add(menuItem);
					subMenu.add(menuItem);
				}
			}
			catch (Exception e)
			{
				System.err.println(
					"Exception caught: " + e.getMessage() + e.getStackTrace());
			}
			midiMenu.add(subMenu);
		}

		return midiMenu;
	}

	class ChannelData
	{
		MidiChannel channel;
		boolean solo, mono, mute, sustain;
		int velocity, pressure, bend, reverb;
		int row, col, num;

		public ChannelData(MidiChannel channel, int num)
		{
			this.channel = channel;
			this.num = num;
			velocity = pressure = bend = reverb = 64;
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
