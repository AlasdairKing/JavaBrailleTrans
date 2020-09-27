package uk.ac.umist.co.brailletrans.utils;
import java.io.*;
import java.lang.ArrayIndexOutOfBoundsException;
import uk.ac.umist.co.brailletrans.*;

/**
 * Creates Unicode version Language files as LanguageUnicode objects from
 * the simple text editable version.  See an existing language file for examples
 * of layout.  Some constraints: comment lines begin with ':' characters, and
 * the language file must begin with two comment lines.  The language file to
 * be converted must be encoded in big-endian network-order Unicode.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class MakeLanguageUnicode extends Maker
{
  private MakeLanguageUnicode()
  {
  }

  /**
   * Command-line interface to translation function.  Takes two arguments, both
   * <CODE>Strings</CODE> the file to convert from and the file to convert to.
   * In fact, it calls the <CODE>make</CODE> methods of <CODE>MakeLegacy</CODE>
   * and <CODE>MakeLanguageUnicode</CODE>
   *
   * @param args args[0] is the input filename, args[1] is the output filename
   */
  public static void main(String[] args)
  {
    String inputFilename = "";
    String outputFilename = "";
      // the file to convert and what to convert it to

    if (args.length != 2)
    {
      System.out.println("MakeLanguageUnicode");
      System.out.println("USAGE: java brailletrans.MakeLanguageUnicode <from> <to>");
      System.exit(SUCCESS);
    }

    // CHECK FOR ARGUMENTS
    inputFilename = args[0];
    outputFilename = args[1];
    make(inputFilename, outputFilename);
  }

  /**
   * Turns the text-format human-editable <CODE>inputFilename</CODE> file
   * into the <CODE>LanguageUnicode</CODE> serialized object <CODE>outputFilename
   * </CODE>.
   *
   * @param inputFilename    The path and filename for the language to convert
   * except for the filename extension, which will be appended by the program.
   * @param outputFilename  The path and filename for the language to be
   * produced, except for the filename extension, which will be appended by the
   * program.
   */
  static public void make(String inputFilename, String outputFilename)
  {
    LanguageUnicode language = new LanguageUnicode();
      // Unicode language file being constructed
    BufferedReader inFile = null;
		  // Used for file input
    final int NUMBER_CHARACTER_FLAGS = 8;
      // numbers of flags for each character (eg, lower, wildcard etc)
    String justGot = "";
      // contains the last line got from the input file

    inputFilename = inputFilename + Language.FILE_EXTENSION_DELIMITER +
      LanguageUnicode.DATAFILE_EXTENSION;
//    outputFilename = outputFilename + LanguageUnicode.FILENAME_EXTENSION;
    String enc = "UTF-16BE";


    // OPEN FILE TO READ
    System.out.println("Converting table for Unicode language rules table file.");


	  // Try to open read file filename
	  try
    {
      inFile = new BufferedReader(new InputStreamReader(new
        FileInputStream(inputFilename), enc));
      inFile.readLine();
    }
	  catch (FileNotFoundException e)
	  {
      System.err.println("Could not find file: " + inputFilename);
		  System.err.println("Error when opening file to read: " + e);
		  System.exit(DISK_ERROR);
    }
    catch (UnsupportedEncodingException e)
    {
      System.err.println("UTF-16BE is not supported : " + e);
      System.exit(DISK_ERROR);
    }
    catch (IOException e)
    {
      System.err.println("Error reading first (must be comment) line of" +
        " input file: " + e);
      System.exit(DISK_ERROR);
    }


    //Skip files beginning with :
	  language.setVersionNumber(Integer.parseInt(skipComments(inFile))); // Version number of table
    System.out.println("Version number: " + language.getVersionNumber());
    language.setName(skipComments(inFile));
    System.out.println("Language name: " + language.getName());
    language.setDescription(skipComments(inFile));
    System.out.println("Description: " + language.getDescription());

    // CHARACTER TRANSLATION RULES
	  // Next bunch of non-comment lines are character translation codes
    try
    {
      language.setNumberCharacters(Integer.parseInt(skipComments(inFile)));
    }
    catch (LanguageDefinitionException e)
    {
      System.err.println("Characters already set for this language: " + e);
      System.exit(LANGUAGE_ERROR);
    }
    System.out.println("Number of characters: " + language.getNumberCharacters());

    for (int i = 0; i < language.getNumberCharacters(); i++)
    {
      String nextLine = getLine(inFile);
      int gotIndex = 0;
      char from = nextLine.charAt(gotIndex++);
      // ASSERTION: gotIndex points at the next char after from OR the u of an
      // escape character
      if (from == '\\')
        if (nextLine.charAt(gotIndex) == 'u')
        // ASSERTION: gotIndex points to the u of an escape character
        {
          gotIndex++;
          // ASSERTION: gotIndex points to the first char of the hex escape sequence
          from = (char) Integer.parseInt(nextLine.substring(gotIndex, gotIndex + 4), 16);
          gotIndex += 4;
          // ASSERTION: gotIndex points at the next char
        }
      // ASSERTION: gotIndex points at the next char
      char to = nextLine.charAt(gotIndex++);
      // ASSERTION: gotIndex points at the next char after from OR the u of an
      // escape character
      if (to == '\\')
        if (nextLine.charAt(gotIndex) == 'u')
        // ASSERTION: gotIndex points to the u of an escape character
        {
          gotIndex++;
          // ASSERTION: gotIndex points to the first char of the hex escape sequence
          from = (char) Integer.parseInt(nextLine.substring(gotIndex, gotIndex + 4), 16);
          gotIndex += 4;
          // ASSERTION: gotIndex points at the next char
        }
      // ASSERTION: gotIndex points at the next char
      gotIndex++;
      // ASSERTION: gotIndex points at the first flag
      int flagValue = 0;
      for (int flagCount = 0; flagCount < NUMBER_CHARACTER_FLAGS; flagCount++)
      {
        if (Character.isUpperCase(nextLine.charAt(3 + flagCount)))
          flagValue += twoToPower(flagCount);
      }

      try
      {
        language.addCharacterInformation(new Character(from), new Character(to), new Integer(flagValue));
      }
      catch (LanguageDefinitionException e)
      {
        System.err.println(e);
        System.exit(LANGUAGE_ERROR);
      }
    }

    // WILDCARDS
    try
    {
      language.setNumberWildcards(Integer.parseInt(skipComments(inFile)));
    }
    catch (LanguageDefinitionException e)
    {
      System.err.println(e);
      System.exit(LANGUAGE_ERROR);
    }
    System.out.println("Number of wildcards:" + language.getNumberWildcards());

    // Read wildcard information and populate hashtable
    for (int i=0; i < language.getNumberWildcards(); i++)
    {
      String nextLine = getLine(inFile);
      char wildcardChar = nextLine.charAt(1);
      String wildcardType = nextLine.substring(3, 5);
      int wildcardNumber = 0;
      if (wildcardType.equals("0+"))
        wildcardNumber = Language.WILDCARD_NONE;
      if (wildcardType.equals("1."))
        wildcardNumber = Language.WILDCARD_ONE;
      if (wildcardType.equals("1+"))
        wildcardNumber = Language.WILDCARD_SEVERAL;

      int wildcardFlags = Integer.parseInt(nextLine.substring(6, nextLine.length()));
//System.err.println("nextLine=" + nextLine + " flags=" + wildcardFlags);

      // instantiate wildcard
      try
      {
        language.addWildcardInformation(wildcardNumber, wildcardFlags, new Character(wildcardChar));
      }
      catch (LanguageDefinitionException e)
      {
        System.err.println(e);
        System.exit(LANGUAGE_ERROR);
      }
    } // for (int i=0; i < numberWildcards; i++)

    // STATE TABLE
    try
    {
      language.setNumberStates(Integer.parseInt(skipComments(inFile)));
    }
    catch (LanguageDefinitionException e)
    {
      System.err.println(e);
      System.exit(LANGUAGE_ERROR);
    }
    System.out.println("Number of states:" + language.getNumberStates());
    String[] stateDescriptions = new String[language.getNumberStates()];
    for (int i = 1; i <= language.getNumberStates(); i++)
    {
      try
      {
        language.setStateDescription(i, getLine(inFile));
      }
      catch (LanguageDefinitionException e)
      {
        System.err.println("Error setting state description: " + e);
        System.exit(LANGUAGE_ERROR);
      }
    }

    // Read number of input classes
    try
    {
      language.setNumberInputClasses(Integer.parseInt(skipComments(inFile)));
    }
    catch (LanguageDefinitionException e)
    {
      System.err.println(e);
      System.exit(2);
    }

    System.out.println("Number of input classes:" + language.getNumberInputClasses());
    String[] inputClassDescriptions = new String[language.getNumberInputClasses()];
    for (int i = 1; i <= language.getNumberInputClasses(); i++)
    {
      try
      {
        language.setInputClassDescription(i, getLine(inFile));
      }
      catch (LanguageDefinitionException e)
      {
        System.err.println(e);
        System.exit(2);
      }
    }

    try
    {
      for (int i = 1; i <= language.getNumberStates(); i++)
        System.out.println(language.getStateDescription(i));
      for (int i = 1; i <= language.getNumberInputClasses(); i++)
        System.out.println(language.getInputClassDescription(i));
    }
    catch (LanguageDefinitionException e)
    {
      System.err.println(e);
      System.exit(LANGUAGE_ERROR);
    }

    // Read decision table
    justGot = skipComments(inFile);

    for (int i=1; i <= language.getNumberStates(); i++)
    {

      for (int j=1; j <= language.getNumberInputClasses(); j++)
      {
        if (justGot.charAt(j - 1) == '0')
        {
          try
          {
            language.setDecisionTableEntry(i, j, false);
          }
          catch (LanguageDefinitionException e)
          {
            System.err.println(e);
            System.exit(2);
          }
        }
        else
        {
          try
          {
            language.setDecisionTableEntry(i, j, true);
          }
          catch (LanguageDefinitionException e)
          {
            System.err.println(e);
            System.exit(2);
          }
        }
      }  // end for int j
      justGot = getLine(inFile);
    } // end for int i

    // TRANSLATION RULES

    System.out.println("Doing translation rules...");
    // Get first rule
    justGot = skipComments(inFile);
    while (justGot.charAt(0) != TABLE_DELIMITER)
    {
//      System.out.println(justGot);
      try
      {
        language.addTranslationRule(justGot);
      }
      catch (LanguageDefinitionException e)
      {
        System.err.println("Error adding translation rule: " + e);
        System.exit(DISK_ERROR);
      }
      justGot = getLine(inFile);
    }

    try
    {
      language.writeLanguageUnicodeToDisk(outputFilename);
    }
    catch (IOException e)
    {
      System.err.println("Error writing language to disk: " + e);
      System.exit(DISK_ERROR);
    }

    System.out.println("Finished - wrote " + outputFilename + " to disk");
    System.exit(SUCCESS);

  }
}
