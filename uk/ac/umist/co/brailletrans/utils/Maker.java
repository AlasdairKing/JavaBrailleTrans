package uk.ac.umist.co.brailletrans.utils;
import java.io.*;

/**
 * Provides functions and constants for the Make programs that produce formatted
 * machine-readable language files from their corresponding human-editable
 * data files.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @see uk.ac.umist.co.brailletrans.Language
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class Maker
{
  Maker()
  {
  }
  
  /**
   * Information for a wildcard definition - it must match zero or more of its type in the input text.
   */
  static final int WILDCARD_NONE = 1;

  /**
   * Information for a wildcard definition - it must match only one of its type in the input text.
   */
  static final int WILDCARD_ONE = 2;

  /**
   * Information for a wildcard definition - it must match one or more of its type in the input text.
   */
  static final int WILDCARD_SEVERAL = 3;


  /**
   * Character indicating the start of an escape character for encoding characters of values
   * 128 - 255 in ASCII language files.
   */
   public static final int ESCAPE_CHARACTER_1 = '\\';

  /**
   * Character indicating the second part of an escape character for encoding characters of values
   * 128 - 255 in ASCII language files.
   */
   public static final int ESCAPE_CHARACTER_2 = 'x';


  /**
   * Error code for successful completion of a Maker program.
   */
  public static final int SUCCESS = 0;

  /**
   * Error code for a failure to complete a program caused by a I/O fault.
   */
  public static final int DISK_ERROR = 1;

  /**
   * Error code for a failure using a Language.
   */
  public static final int LANGUAGE_ERROR = 2;

  /**
   * The ASCII character indicating the end of the translation rules.
   */
  public static final char TABLE_DELIMITER = '#';

  /**
   * Character denoting a comment line in a language rules table.  It must be
   * placed at the very beginning of the line.
   */
  static final char COMMENT = ':';

  /**
   * Integer value of linefeed ASCII character
   */
  static final int LINEFEED = 10;

  /**
   * Integer value of carriage-return ASCII character
   */
  static final int CR = 13;

  /**
   * Integer indicating TRUE boolean in legacy C-based tables
   */
   static final int TRUE = 1;

  /**
   * Integer indicating FALSE boolean in legacy C-based tables
   */
   static final int FALSE = 0;

  /**
    * Skips over comment lines in the language files.  The human-readable files
    * can have comment lines, indicated by the ':' colon character, or the COMMENT
    * constant in MakeLanguage256.  This should return first non-comment character
    * of the first non-comment line as an int.<P>To be explicit: the method halts
    * only when one of the following is met:
    * <OL><LI>A carriage return is followed by a non-comment non-linefeed character or
    * <LI>A linefeed followed by a non-comment non-carriage return character.</OL>
    * In both cases, the ASCII/Unicode characters for CR, LF are used.  This should
    * work for Apple Macintosh, Windows and Unix systems.
    *
    * @param fileIn  BufferedInputStream to the file being read from.
    * @return readBuffer[1]   An int, the first character of the next non-comment line.
  */
  public static int skipComments(BufferedInputStream fileIn)
  {
    // Skip files beginning with :
    // check for a carriage return (CR) followed by a non- COMMENT character.
    // This will match LF+CR+(not :) OR  CR+(not :)
    // - so should handle UNIX and Windows text files.
    // Must match LF+(neither : NOR CR) for Macintosh
    boolean looking = true;  // true = still looking, false = found a new line
    int[] readBuffer = new int[2]; // buffer for last two characters read
    readBuffer[1] = readBuffer[0] = 0; // initialise at 0
    while (looking)
    {
      readBuffer[0] = readBuffer[1]; // old second character becomes new first
      try
      {
        readBuffer[1] = fileIn.read();  // get new second character
      }
      catch (IOException e)
      {
		    System.err.println("Error skipping comments: " + e);
   		  System.exit(1);
      }
      if (readBuffer[0] == CR && readBuffer[1] != COMMENT)
        looking = false;
      if (readBuffer[0] == LINEFEED && readBuffer[1] != COMMENT)
        looking = false;
      if (readBuffer[0] == LINEFEED && readBuffer[1] == CR)
        looking = true;
      if (readBuffer[1] == LINEFEED && readBuffer[0] == CR)
        looking = true;

    }
    // ASSERTION: end of comment section.
    return readBuffer[1];
  }

  /**
   * Provides the result of 2<SUP>i</SUP>.
   *
   * @param number int that 2 will be raised to.
   * @return toReturn int that results from 2 to the power number.
   */
  static int twoToPower(int number)
  {
    int toReturn = 1;
    for (int i = 0; i < number; i++)
      toReturn *= 2;
    return toReturn;
  }

  /**
   * Reads a line of text from an input file.
   *
   * @param fileIn  <CODE>BufferedReader</CODE> for a file.
   * @return toReturn   <CODE>String</CODE> containing line of text.
   */
  static String getLine(BufferedReader fileIn)
  {
    String toReturn = null;
    try
    {
      toReturn = fileIn.readLine();
    }
    catch (IOException e)
    {
      System.err.println("Error reading line of text from file: " + e);
      System.exit(1);
    }
    return toReturn;
  }

  /**
   * Skips over comment lines in the language files.  The human-readable files
   * can have comment lines, indicated by the ':' colon character, or the COMMENT
   * constant in MakeLanguage256.  This should return first non-comment character
   * of the first non-comment line as an int.
   * @param fileIn <CODE>BufferedInputStream</CODE> for the file being read.
   * @return toReturn <CODE>String</CODE> containing the first non-comment line.
   */
  public static String skipComments(BufferedReader fileIn)
  {
    String toReturn = null;
    do
    {
      try
      {
         toReturn = fileIn.readLine();  // get new second character
//System.err.println("Testing: <" + toReturn + ">");
      }
      catch (IOException e)
      {
		    System.err.println("Error skipping comments: " + e);
   		  System.exit(DISK_ERROR);
      }
      catch (Exception e)
      {
        System.err.println("Error skipping comments: ensure the file being " +
          "processed is in big-endian network-order Unicode");
        System.exit(DISK_ERROR);

        
      }
    } while (toReturn.charAt(0) == COMMENT);
    // ASSERTION: end of comment section.
//System.err.println("Returning:" + toReturn);
    return toReturn;
  }

  /**
   * If a '\x' character is encountered in a language human file it might be an
   * escape sequence for a 256-character set value.  This iteratively processes
   * the values and returns as an array of integers the values of the possible
   * escape character OR escape characters AND the next non-escape character.  If
   * the potential escape character is not in fact an escape character it is
   * returned instead.
   *
   * @param inFile  The file being read from.
   * @return  The sequence of characters, escaped or not, and the next character.
   */
  public static int[] getEscapedCharacter(BufferedInputStream inFile)
  {
    int[] toReturn;
    int nextGot = 0;

    try
    {
      nextGot = inFile.read();
    }
    catch (IOException e)
    {
      System.err.println("Error reading from disk: " + e);
      System.exit(DISK_ERROR);
    }
    if (nextGot != ESCAPE_CHARACTER_2)
    {
      toReturn = new int[2];
      toReturn[0] = '\\';
      toReturn[1] = nextGot;
      return toReturn;
    }
    // ASSERTION: it is an escape character
    try
    {
      nextGot = inFile.read();
    }
    catch (IOException e)
    {
      System.err.println("Error reading from disk: " + e);
      System.exit(DISK_ERROR);
    }
    String value = "";
    while ((nextGot >= '0') && (nextGot <= '9'))
    {
      value += (char) nextGot;
      try
      {
        nextGot = inFile.read();
      }
      catch (IOException e)
      {
        System.err.println("Error reading from disk: " + e);
        System.exit(DISK_ERROR);
      }
    }
    int escapedChar = Integer.parseInt(value);
    if (nextGot == ESCAPE_CHARACTER_1)
    {
      int[] toAdd = getEscapedCharacter(inFile);
      toReturn = new int[toAdd.length + 1];
      toReturn[0] = escapedChar;
      for (int i = 0; i < toAdd.length; i++)
        toReturn[i+1] = toAdd[i];
      return toReturn;
    }
    toReturn = new int[2];
    toReturn[0] = escapedChar;
    toReturn[1] = nextGot;
    return toReturn;
    // ASSERTION: nextGot holds non-escape character - must be returned
    

  }
}
