/*
InPitch - a simple ear-training program for note recognition.
Copyright (C) 2006 Chris Gilbreth, Hector Parra

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

Send bugs, comments, etc. to: cngilbreth@gmail.com and hgparra@gmail.com
*/



import java.awt.*;

//import java.awt.event.*;
import javax.swing.*;

// InPitch

public class InPitch
{
	public static String[] notes =
		{
			"C",
			"C# | Db",
			"D",
			"D# | Eb",
			"E",
			"F",
			"F# | Gb",
			"G",
			"G# | Ab",
			"A",
			"A# | Bb",
			"B" };

	//private SoundSet set;

	public static void main(String[] args)
	{
		WaveBank set = new WaveBank();
		SoundTrainer trainer = new SoundTrainer();
		//        set.load("test.set"); 
		//
		InPitchMain inPitchMain = new InPitchMain(trainer);

		int width = 300;
		int height = 270;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		JFrame.setDefaultLookAndFeelDecorated(true);
		inPitchMain.setLocation(
			screenSize.width / 2 - width / 2,
			screenSize.height / 2 - height / 2);
		inPitchMain.setSize(width, height);
		inPitchMain.setTitle("InPitch");
		inPitchMain.setVisible(true);

		// how i thought the apple menu would work
		//inPitchMain.setJMenuBar(inPitchMain.createMenuBar());

		inPitchMain.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
