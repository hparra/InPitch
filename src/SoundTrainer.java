/*
This file is a part of InPitch - a simple ear-training program
for note recognition. Copyright (C) 2006, 2016 Chris Gilbreth, Hector Parra.

See the file InPitch.java and COPYING.txt for further copyright and
contact information.
*/

/**
 * @author cngilbreth
 * SoundTrainer: This class encapsulates the logic / data resposible for
 * keeping track of which notes are being trained at the moment.
 *
 * TODO: Make this class responsible for choosing the next note so we
 * can customize the note order a bit?
 */

import java.util.*;

public class SoundTrainer
{
	// number of total guesses for each note (capped at histsize (below))
	private int totalNum[] = new int[12];

	// history of last 12 tries for each note
	private int histsize = 12;
	private int history[][] = new int[12][histsize];

	private Vector notes;

	private int level = 0;

	public String ordering1[] = {"A", "E", "C", "F# | Gb", "D", "B", "F", "G", "D# | Eb", "A# | Bb", "C# | Db", "G# | Ab"};
	public String ordering2[] = {"F", "G", "D# | Eb", "A# | Bb", "C# | Db", "G# | Ab", "E", "A", "C", "F# | Gb", "D", "B"};
	public String ordering3[] = {"C", "C# | Db", "D", "D# | Eb", "E", "F", "F# | Gb", "G", "G# | Ab", "A", "A# | Bb", "B"};

	public String ordering[] = ordering1;

	// Returns a vector of Strings representing the notes that the
	// program will test the student on.
    public Vector getNotes() throws Exception
    {
	if (canAdvance() && level < 11)
		increaseLevel();

	//System.out.println("Trainer returning notes: " + notes);
	return notes;
    }

	private void increaseLevel() throws Exception
	{
		level++;
		notes = vecFromNotes(ordering, level + 2);

	}

	public void setLevel(int newLevel) throws Exception
	{
		if (newLevel > 11 || newLevel < 0)
			throw new Exception("Invalid level");


		notes = vecFromNotes(ordering, newLevel + 1);
		level = newLevel - 1;

		//System.out.println("Trainer: setting level");

		// when switch a level we start over
		for (int i = 0; i < 12; ++i)
		{
			totalNum[i] = 0;
			for (int j = 0; j < histsize; ++j)
				history[i][j] = 0;
		}

	}

    // The program is to call reportCorrect() whenever the user guesses
    // wrong for the note represented by the string passed in. This allows
    // the SoundTrainer to keep track of how well the student is doing.
    public void reportCorrect(String note) throws Exception
    {
	int key = MidiBank.noteToKey(note);

	if (totalNum[key] < histsize)
		totalNum[key]++;
	shiftleft(history[key], histsize);
	history[key][histsize - 1] = 1;
    }

	// The program is to call reportWrong() whenever the user guesses
	// wrong for the note represented by the string passed in. This allows
	// the SoundTrainer to keep track of how well the student is doing.
    public void reportWrong(String note) throws Exception
    {
		int key = MidiBank.noteToKey(note);

		if (totalNum[key] < histsize)
			totalNum[key]++;
		shiftleft(history[key], histsize);
		history[key][histsize - 1] = 0;
    }

	// Returns some sort of identifier for the current level. The only
	// requirement is that the identifier be unique for each, so the program
	// knows when the current set of notes has changed. Also it must be
	// a positive number.
	public int levelID() throws Exception
	{
		if (canAdvance() && level < 11)
			increaseLevel();

		return level;
	}

	// TODO: this is odd, or rather, levelID() is odd
	public int getLevel()
	{
		return level;
	}

	// This tests whether the conditions have been met for advancement
	// to the next level.
	private boolean canAdvance() throws Exception
	{
		boolean can = true;

		//System.out.println("-----");
		// for each note...
		for (int i = 0; i < notes.size(); ++i)
		{
			int key = MidiBank.noteToKey((String) notes.get(i));
			int numguessed = totalNum[key];

			int index1 = 0;
			if (numguessed < histsize)
				index1 = histsize - numguessed;

			// sum from index 1 to histsize - 1, inclusive (this is the number correct)
			int sum = sumInclusive(history[key], index1, histsize - 1);

			int numwrong = histsize - numguessed;
			double ratio = 0;

			// look for 85% correctness
			if (numguessed > 0)
			{
				ratio = (double) sum / numguessed;
				//System.out.print("Ratio for " + notes.get(i) + ": " + ratio);
				//System.out.print(" Num guessed: " + numguessed + " Num right: " + sum + "\n");
				if (ratio < 0.85)
					can = false;
			}

			// minimum of 12 correct guesses or 7 correct guesses with 100% accuracy
			int numcorrect = numguessed - numwrong;
			if (numcorrect < 12 && !(numcorrect > 6 && ratio == 1.0))
				can = false;
		}

		return can;
	}


	public SoundTrainer()
	{
		for (int i = 0; i < 12; ++i)
		{
			totalNum[i] = 0;
			for (int j = 0; j < histsize; ++j)
				history[i][j] = -100; // to catch mistakes
		}

		// level is already initialized to 0 here
		notes = vecFromNotes(ordering, level + 2);
	}

	// Creates a vector of Strings from an array of Strings.
	public static Vector vecFromNotes(String[] s, int index)
	{
		Vector v = new Vector();
		for (int i = 0; i < index; ++i)
			v.add(s[i]);

		return v;
	}

	private static void shiftleft(int array[], int size)
	{
		if (size < 1)
			return;

		int zeroth = array[0];
		for (int i = 0; i < size - 1; ++i)
			array[i] = array[i + 1];

		array[size - 1] = zeroth;
	}

	private static int sumInclusive(int array[], int index1, int index2)
	{
		int sum = 0;
		for (int i = index1; i <= index2; ++i)
			sum += array[i];

		return sum;
	}

	public void switchOrdering(int newOrdering)
	{
	if (newOrdering == 1)
		ordering = ordering1;
	else if (newOrdering == 2)
		ordering = ordering2;
	else if (newOrdering == 3)
		ordering = ordering3;
	else
		return;
	}


	// Just here to test the class.
    public static void main(String[] args)
    {

	SoundTrainer t = new SoundTrainer();
	try
	{
	    //System.out.println(t.getNotes());
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }
}
