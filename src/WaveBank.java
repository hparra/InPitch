/*
This file is a part of InPitch - a simple ear-training program
for note recognition. Copyright (C) 2006, 2016 Chris Gilbreth, Hector Parra.

See the file InPitch.java and COPYING.txt for further copyright and
contact information.
*/

// SoundBank - sound bank for InPitch

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.*;
import java.util.*;


public class WaveBank implements SoundBank
{
    private String filename;

    private TreeMap set;

    private int lastNoteIndex;
    private int lastOctaveIndex;

    private Random random = new Random();

    public static Vector filesvec = new Vector();
    private Vector notes;

    public static String[][] files = {
	    {"C1.wav", "C2.wav", "C3.wav"},
	    {"CSharp1.wav", "CSharp2.wav", "CSharp3.wav"},
	    {"D1.wav", "D2.wav", "D3.wav"},
	    {"DSharp1.wav", "DSharp2.wav", "DSharp3.wav"},
	    {"E1.wav", "E2.wav", "E3.wav"},
	    {"F1.wav", "C2.wav", "C3.wav"},
	    {"FSharp1.wav", "FSharp2.wav", "FSharp3.wav"},
	    {"G1.wav", "G2.wav", "G3.wav"},
	    {"GSharp1.wav", "GSharp2.wav", "GSharp3.wav"},
	    {"A1.wav", "A2.wav", "A3.wav"},
	    {"ASharp1.wav", "ASharp2.wav", "ASharp3.wav"},
	    {"B1.wav", "B2.wav", "B3.wav"}};

	class PlayWaveThread extends Thread
	{
	    String filename;

		public PlayWaveThread(String file)
		{
			this.filename = file;
		}

		public void run()
		{
			try
			{
		File file = new File(filename);
		AudioInputStream ais = AudioSystem.getAudioInputStream(file);
		AudioFormat af = ais.getFormat();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);

		if (!AudioSystem.isLineSupported(info))
		{
		    //System.out.println("unsupported line");
		    System.exit(0);
		}

		int frameRate = (int) af.getFrameRate();
		//System.out.println("Frame Rate: " + frameRate);
		int frameSize = af.getFrameSize();
		//System.out.println("Frame Size: " + frameSize);
		int bufSize = frameRate * frameSize / 10;
		//System.out.println("Buffer Size: " + bufSize);

		SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
		line.open(af, bufSize);
		line.start();

		byte[] data = new byte[bufSize];
		int bytesRead;

		while ((bytesRead = ais.read(data, 0, data.length)) != -1)
		    line.write(data, 0, bytesRead);

		line.drain();
		line.stop();
		line.close();

		ais.close();
			}
			catch (Exception e)
			{
			    e.printStackTrace();
			}
		}
	}

    public WaveBank()
    {
	for (int i = 0; i < files.length; ++i)
	{
	    for (int j = 0; j < files[i].length; ++j)
		filesvec.add(files[i][j]);
	}
    }

    public String playRandomNote()
    {
	String notestr = (String) notes.get(random.nextInt(notes.size()));
	int note = 0;

	for (int i = 0; i < 12; ++i)
	{
	    if (InPitch.notes[i].equals(notestr))
	    {
		note = i;
		break;
	    }
	}

	int oct = random.nextInt(files[note].length);

	playFile(files[note][oct]);

	lastNoteIndex = note;
	lastOctaveIndex = oct;

	return InPitch.notes[note];
    }

    public void repeat()
    {
	playFile(files[lastNoteIndex][lastOctaveIndex]);
    }

    public void playFile(String filename)
    {
	//System.out.println("playing file: " + filename);

		PlayWaveThread thread = new PlayWaveThread(filename);
		thread.start();
    }

    public void load(String filename)
    {

    }

    private static String getNote(FileReader reader)
    {
	return new String("testnote.wav");
    }

    private static String getNextWaveFile(FileReader reader)
    {
	return new String("testnote.wav");
    }

    public static void main(String args[])
    {
	WaveBank bank = new WaveBank();
	bank.playRandomNote();
	bank.playRandomNote();
	bank.playRandomNote();
	bank.playRandomNote();
    }

    public void close()
    {
    }

    public void open()
    {
    }

    public void setNotes(Vector newnotes) throws Exception
    {
	notes = newnotes;
    }


    public void switchInstrument(int i)
    {
    }
}
