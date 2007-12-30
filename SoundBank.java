/* 
This file is a part of InPitch - a simple ear-training program
for note recognition. Copyright (C) 2006 Chris Gilbreth, Hector Parra.

See the file InPitch.java and COPYING.txt for further copyright and
contact information.
*/

import java.util.*;

/**
 * @author cngilbreth
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface SoundBank
{
    public void close();
    public void open() throws SoundbankException;
    public void repeat();
    public String playRandomNote();
    public void setNotes(Vector notes) throws Exception;
    public void switchInstrument(int i);
}
