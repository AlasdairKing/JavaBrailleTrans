package uk.ac.umist.co.brailletrans.utils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.lang.ArrayIndexOutOfBoundsException;
import uk.ac.umist.co.brailletrans.*;

/**
 * Constructs a legacy language rules table file in machine format from a text
 * format file.  The format of the text file is very important.  It must be
 * encoded in ASCII or 256-character extended-ANSI.
 * <H3>Structure of the textfile to process</H3>
 * <TABLE BORDER="1"><TR><TH>What<TH>How many (lines)
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>version number, integer<TD>1
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>character rules<TD>256
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>number of wildcards<TD>1
 * <TR><TD>wildcard definitions<TD>as many as <EM>number of wildcards</EM>
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>number of states<TD>1
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>number of input classes<TD>1
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>decision table<TD>as many rows as states
 * <TR><TD>comments<TD>as many as wanted
 * <TR><TD>translation rules<TD>as many as needed at one per line
 * <TR><TD># character to end<TD>on separate line, indicates end of translation rules
 * </TABLE>
 * <P>There are a number of further constraints: a maximum of 9 input classes, a
 * maximum of 9 states, characters indicating true flags must be ASCII upper
 * case, comment lines must begin with the ':' character, the asterisk '*'
 * character must not be used as a wildcard.
 * <P>Escape characters are supported: '\\xdd' where dd = decimal value of
 * the character.  Use '\\\\' for backslash.
 * <P>See a text-format file for more details.
 * 
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King (alasdairking@yahoo.co.uk)
 * @version 1.0 08/31/2001
 */
public class MakeLegacy extends Maker
{
  static final int NUMBER_CHARACTER_RULES = 256;
    // Number of character translation rules
  static final int TRANSLATION_RULE_LENGTH = 256;
    // Number of permitted characters in one translation rule
  static final char TABLE_DELIMITER = '#';

  private MakeLegacy()
  {
  }

  /**
   * Command-line access to conversion process.
   *
   * @param args The array of Strings passed as command-line arguments.  The arguments are:
   * <UL><LI>input filename - full path and name of .con file to convert.
   * <LI>output filename - full path and name of .dat file produced.</UL>
   */
  public static void main(String[] args)
  {
    String inputFilename = "";
    String outputFilename = "";
      // the file to convert and what to convert it to
    if (args.length != 2)
    {
      System.out.println("MakeLegacy");
      System.out.println("USAGE  java brailletrans.MakeLegacy <from> <to>");
      System.exit(SUCCESS);
    }
    inputFilename = args[0];
    outputFilename = args[1];
    make(inputFilename, outputFilename);
  }

  /**
   * Performs actual conversion of CON files to DAT files.
   *
   * @param inputFilename  Full path and name of CON file to process.
   * @param outputFilename  Full path and name of DAT file to produce.
   */
  public static void make(String inputFilename, String outputFilename)
  {
    int numberOfRules;
   	  // Number of translation rules in file
    int numberWildcards = 0;
      // Number of wildcards
    int numberStates = 0;
    int numberInputClasses;
      // number of states and input classes for the decision table
    int tableVersion;
      // version of the table
    BufferedInputStream inFile = null;
		  // Used for file input
    BufferedOutputStream outFile = null;
      // For file output
    int[] readBuffer = {0, 0, 0};
      // the last three 8-bit bytes got from the input file, all ints

    inputFilename = inputFilename + Language.FILE_EXTENSION_DELIMITER
      + LanguageInteger.DATAFILE_EXTENSION;
    outputFilename = outputFilename + Language.FILE_EXTENSION_DELIMITER
      + LanguageInteger.FILENAME_EXTENSION;

    // OPEN FILES TO READ AND SAVE
    System.out.println("MakeLegacy: Converting table for 256 character set language");

	  // Try to open read file filename
	  try
    {
      inFile = new BufferedInputStream(new FileInputStream(inputFilename));
    }
	  catch (FileNotFoundException e)
	  {
      System.err.println("Could not find file: " + inputFilename);
		  System.err.println("Error when opening file to read: " + e);
		  System.exit(DISK_ERROR);
    }

    // Try to open write file filename
	  try
    {
      outFile = new BufferedOutputStream(new FileOutputStream(outputFilename));
    }
	  catch (IOException e)
	  {
      System.err.println("Error writing file: " + outputFilename);
		  System.err.println("Error when opening file to write: " + e);
		  System.exit(DISK_ERROR);
    }


	  tableVersion = skipComments(inFile) - '0'; // Version number of table
    System.out.println("Version number: " + tableVersion);

    // Write version number to file
    try
    {
      outFile.write(17);  // Legacy version numbers, not documented.
      outFile.write(12);
      outFile.write(8);
      outFile.write(tableVersion);
    }
    catch (IOException e)
    {
      System.out.println("Error writing file: " + outputFilename);
		  System.err.println("Error: " + e);
		  System.exit(DISK_ERROR);
    }
    skipComments(inFile);

    // CHARACTER TRANSLATION RULES
	  // Next NUMBERCHARACTERRULES non-comment lines are character translation codes
	  System.out.println("Translating character codes");
	  for (int i = 0; i < NUMBER_CHARACTER_RULES; i++)
	  {
      // write input character and translation character
      try
      {
        // INPUT AND MAPPING CHARACTER
        // inFile.read() has just returned the first character of the rule line
        inFile.read(); // second
        inFile.read(); // third
        inFile.read(); // fourth = :
        int input = inFile.read();
        outFile.write(input); // fifth is input character
        int output = inFile.read();
        outFile.write(output); // sixth is output character
        inFile.read(); // seventh is : again, now onto the flags

        // FLAGS
        int flagToWrite = 0;
        for (int j=1; j < 256; j*=2)
        {
          int flagValue = inFile.read();
          if ((flagValue >= 'A') && (flagValue <= 'Z'))
            flagToWrite = flagToWrite | j;
        }
        outFile.write(flagToWrite);

        // NEXT LINE
        readBuffer[2] = inFile.read();
        while (readBuffer[2] < '0')
          readBuffer[2] = inFile.read();
      }
      catch (IOException e)
      {
        System.out.println("Error processing character information: " + outputFilename);
  		  System.err.println("Error: " + e);
  		  System.exit(DISK_ERROR);
      }
    }  // for (int i=0; i < NUMBERCHARACTERRULES; i++)

    readBuffer[2] = skipComments(inFile);
    // ASSERTION: readBuffer[2] now holds the one-byte number of wildcards

    // *************************************************************************
    // Read number of wildcards
    // *************************************************************************
    try
    {
      numberWildcards = readBuffer[2] - '0';
      outFile.write(numberWildcards);
      System.out.println("Number of wildcards:" + numberWildcards);
    }
    catch (IOException e)
    {
      System.err.println("Error processing number of wildcards: " + e);
      System.exit(DISK_ERROR);
    }

    // *************************************************************************
    // Read wildcard information
    // *************************************************************************

    for (int i=0; i < numberWildcards; i++)
    {
      try
      {
        // Skip to wildcard character
        readBuffer[2] = inFile.read();
        while (readBuffer[2] < ' ' || readBuffer[2] == '*')
          readBuffer[2] = inFile.read();
        // ASSERTION: rB[2] contains wildcard character

        // read and write wildcard character
        outFile.write(readBuffer[2]);

        readBuffer[0] = inFile.read(); // get '='
        readBuffer[1] = inFile.read(); // get first char of wildcard value
        readBuffer[2] = inFile.read(); // get second char of wildcard value
        if (readBuffer[1] == '0') // match none
          outFile.write(WILDCARD_NONE);
        else if (readBuffer[1] == '1')
        {
          if (readBuffer[2] == '+')
            outFile.write(WILDCARD_SEVERAL); // match several
          else
            outFile.write(WILDCARD_ONE); // match one
        }

        readBuffer[2] = inFile.read();
        // ASSERTION: inFile now at start of flag information on line
        readBuffer[0] = inFile.read();
        readBuffer[1] = inFile.read();
        if (readBuffer[1] > '0')
          readBuffer[2] = inFile.read();
        else
          readBuffer[2] = 'a';
        String fromBuffer = "" + (readBuffer[0]-'0');
        if (readBuffer[1] >= '0' && readBuffer[1] <= '9')
          fromBuffer = fromBuffer + (readBuffer[1]-'0');
        if (readBuffer[2] >= '0' && readBuffer[2] <= '9')
          fromBuffer = fromBuffer + (readBuffer[2]-'0');
        outFile.write(Integer.parseInt(fromBuffer));
      }
      catch (IOException e)
      {
        System.err.println("Error reading wildcard information: " + e);
        System.exit(DISK_ERROR);
      }
    } // for (int i=0; i < numberWildcards; i++)

    //*****************************************************************
    // State table
    //*****************************************************************

    readBuffer[2] = skipComments(inFile);
    // ASSERTION: readBuffer[2] now holds the number of states

    try
    {
      numberStates = readBuffer[2] - '0';
      System.out.println("Number of states:" + numberStates);
      outFile.write(numberStates);
    }
    catch (IOException e)
    {
      System.err.println("Error processing number of states: " + e);
    }

    readBuffer[2] = skipComments(inFile);
    // ASSERTION: readBuffer[2] now holds the number of input classes, or the first digit thereof

    // *************************************************************************
    // Read number of input classes
    // *************************************************************************
    numberInputClasses = readBuffer[2] - '0';
    System.out.println("Number of input classes:" + numberInputClasses);
    try
    {
      outFile.write(numberInputClasses);
    }
    catch (IOException e)
    {
      System.err.println("Error writing number of input classes: " + e);
      System.exit(DISK_ERROR);
    }

    readBuffer[2] = skipComments(inFile);
    // ASSERTION: readBuffer[2] now holds the first value of the decision table

    // *************************************************************************
    // Read decision table
    // *************************************************************************


    for (int i=0; i < numberStates; i++)
    {
      for (int j=0; j < numberInputClasses; j++)
      {
        try
        {
          if (readBuffer[2] == '0')
            outFile.write(FALSE);
          else
            outFile.write(TRUE);
          readBuffer[2] = inFile.read();
        }
        catch (IOException e)
        {
  		    System.err.println("Error processing state table: " + e);
          System.exit(DISK_ERROR);
        }
      }
      try
      {
        while (readBuffer[2] < '0')
          readBuffer[2] = inFile.read();
      }
      catch (IOException e)
      {
		    System.err.println("Error processing state table: " + e);
        System.exit(DISK_ERROR);
      }
    }

    System.out.println("Doing translation rules...");
    readBuffer[2] = skipComments(inFile);
    // ASSERTION: readBuffer[2] contains first variable of translation rules

    // *************************************************************************
    // Read translation rules
    // *************************************************************************

    while (readBuffer[2] != '#')
    // ASSERTION: first character of new line is not #, still rules to go.
    {
      try
      {
        int count = 0;
        int[] readRule = new int[TRANSLATION_RULE_LENGTH];
        int[] writeRule = new int[TRANSLATION_RULE_LENGTH];
        writeRule[0] = readBuffer[2] - '0'; // input class
        while ((readBuffer[2] != LINEFEED) && (readBuffer[2] != CR ))
         // check char isn't CR
        {
          if (count == TRANSLATION_RULE_LENGTH)
          {
            System.err.println("Error: rule length exceeds buffer size");
            System.exit(DISK_ERROR);
          }
          if (readBuffer[2] == ESCAPE_CHARACTER_1)
          {
            int[] toAdd = getEscapedCharacter(inFile);
            for (int i = 0; i < toAdd.length; i++)
              readRule[count + i] = toAdd[i];
            count += toAdd.length;
          }
          else
          {
            readRule[count] = readBuffer[2]; // populate read
            count++;                         // point at next space in read
          }
          readBuffer[2] = inFile.read();   // get next char
        }
        // ASSERTION: readrule[] now contains whole rule up to CR exclusive

        int readCount = 2; // start of left focus (0 is input, 1 is size of rule)
        int writeCount = 2; // as readCount
        while (readRule[readCount] >= ' ') // while isChar
        {
          // ASSERTION: end of body not reached, still char: copy character to write
          writeRule[writeCount] = readRule[readCount];
          writeCount++;
          readCount++;
          // ASSERTION: readCount points to next character, writeCount to next empty
        }
        // ASSERTION: readRule[readCount] < ' ' => is not a char => finished body

        while (readRule[readCount] < ' ')  // move through TABs to new state
          readCount++;
        // ASSERTION: readCount points to a output state

        writeRule[writeCount] = 0;
        writeCount++;
        if (readRule[readCount] == '-')
          writeRule[writeCount] = 0;
        else
          writeRule[writeCount] = readRule[readCount] - '0'; // transfer state
        writeCount++;
        // ASSERTION: writeCount now indicates the total length of the rule
        writeRule[1] = writeCount;

        // transfer to output file
        for (int j=0; j < writeCount; j++)
          outFile.write(writeRule[j]);

        // skip to next rule
        while (readBuffer[2] < ' ')
          readBuffer[2] = inFile.read();
      }
      catch (IOException e)
      {
        System.out.println("Error processing translation rules: " + e);
        System.exit(DISK_ERROR);
      }
    }

    // Write '#' to finish
    try
    {
      outFile.write(TABLE_DELIMITER);
    }
    catch (IOException e)
    {
      System.out.println("Error writing # to finish: " + e);
      System.exit(DISK_ERROR);
    }

    System.out.println("Finished, written new compiled file to disk.");

    try
    {
      outFile.close();
      inFile.close();
    }
    catch (IOException e)
    {
		  System.err.println("Error closing file: " + e);
      System.exit(DISK_ERROR);
    }
  }



}
