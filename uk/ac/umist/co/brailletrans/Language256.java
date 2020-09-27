package uk.ac.umist.co.brailletrans;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A Java implementation of the UMIST, Manchester text to Braille and Braille
 * to text system, designed to work with 256-character extended-ASCII ANSI sets.
 * This functions identically to <CODE>LanguageInteger</CODE>, but not as fast.
 * Its sole advantage is that its code is easier to understand.
 * 
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author  Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class Language256 implements Language, Serializable
{
  /**
   * The maximum ratio of input to output arrays after translation - 1:MAX_COMPRESS.  Any greater will
   * cause the language to throw an ArrayIndexOutOfBoundsException.  The input text may be padded with
   * spaces if this occurs.
   */
  public static final double MAX_COMPRESS = 2;

  private transient int state;
    // current state of finite state machine
  private transient int defaultState;
    // state of machine when first instantiated, or 1 if not specified.

  private TranslationRule256[] translationRule;
  //The array of translation rules for the language.
  private transient int transRuleIndex;
  //Index to the translationRules array to show the current rule being examined.
  //The alternative, using a Rule currentRule object, will degrade performance.
  private int numberTranslationRules;
  //Total number of translation rules.

  private int version;
  //The version number of the table: indicates it is appropriate for use

  private boolean[][] stateTable;
  //The matrix cross-referencing machine state with input class to see whether
  //confirm that a rule can be used.  stateTable[state][inputclass].
  private int numberInputClasses;
  private int numberStates;
  // Size of the stateTable

  private CharacterRule256[] characterRule;
  //The array of character mapping rules.
  private final int NUMBER_CHARACTER_RULES = 256;
  // 256-character rule tables supported
  private final int MAX_NUMBER_TRANSLATION_RULES = 2000;
  // maximum number of translation rules for the language

  private Wildcard256[] wildcards;
  // Represents the wildcards for the language
  private int numberWildcards;
  // Like, the number of wildcards

  /**
   * Indicates language rules tables (data files) on disk, suitable for
   * loading into the <CODE>Language256</CODE> class and performing translation.
   */
  public static final String FILENAME_EXTENSION = "dat";
  /**
   * Indicates the text-only human-editable language rules tables format
   * on disk.  These must be converted into the machine format by
   * <CODE>MakeLegacy</CODE>
   */
  public static final String DATAFILE_EXTENSION = "con";
  private static final int SPACE = ' ';

 /**
  * Loads a Language256 object from disk by loading and parsing a legacy data file.
  *
  * @param filename Full path and name of language rules table to load according
  * to local filesystem.
  */
  public Language256(String filename)
    throws IOException, FileNotFoundException
  {
    state = defaultState = 1;
      // Always set state to 1 on instantiation
    BufferedInputStream inFile = null;
		  // Used for File input
    transRuleIndex = 0;
      // Tracks the current rule under examination
    filename = filename + FILE_EXTENSION_DELIMITER + FILENAME_EXTENSION;
      // try to find the right language file type

    // open file for reading
    try
    {
      inFile = new BufferedInputStream(new FileInputStream(filename));
    }
    catch (FileNotFoundException e)
    {
      throw new FileNotFoundException("File not found: " + filename + " Reported"
       + " error: " + e);
    }
    catch (IOException e)
    {
      throw new IOException("Could not read from local file: " + filename
       + " Error reported: " + e);
    }

    // Read data from file and parse it into the language structures
    // ASSERTION: Next inFile.read() will get first byte of inFile file

    // LANGUAGE VERSION
    try
    {
      inFile.skip(3);  // first 3x undocumented header code - 17, 12, 8
      version = inFile.read();  // this byte is language version
    }
    catch (IOException e)
	  {
      throw new IOException("Unable to process: " + filename + " while " +
        "attempting to skip header and read version.  Error: " + e);
    }
    // ASSERTION: Next inFile.read() will get first byte of character rules

    // CHARACTER RULES
    // Instantiate the character rule table
    characterRule = new CharacterRule256[NUMBER_CHARACTER_RULES];
    // this is for the legacy 256-character sets, so NUMBER... should be 256

    // Import the rules
    for (int charRuleIndex = 0; charRuleIndex < NUMBER_CHARACTER_RULES; charRuleIndex++)
    {
      try
      {
         // ASSERTION: inFile.read() will get first byte of three-byte character rule
         int mapped = inFile.read();
           // first byte is "from" character
         int upper = inFile.read();
           // next is "to" character
         int flags = inFile.read();
           // third and final is flags for character
         // ASSERTION: three bytes have been read, character rules are three bytes
         //  -> next byte will be first byte of next three-byte character rule
         characterRule[charRuleIndex] = new CharacterRule256(mapped, upper, flags);
      }
      catch (IOException e)
  	  {
        throw new IOException("Unable to process: " + filename + " while " +
          "attempting to read character rule.  Error: " + e);
      }
    }
    // ASSERTION: all NUMBER_CHARACTER_RULES three-byte rules have been read.
    //  -> Next inFile.read() will get number of wildcards.

    // WILDCARDS
    numberWildcards = 0;  // required by compiler
    try
    {
      numberWildcards = inFile.read();
    }
    catch (IOException e)
    {
        throw new IOException("Unable to process: " + filename + " while " +
          "attempting to read wildcards.  Error: " + e);
    }
    // ASSERTION: next inFile.read() will get first byte of wildcard table

    // Declare the wildcard array
    wildcards = new Wildcard256[numberWildcards];

    for (int wildcard = 0; wildcard < numberWildcards; wildcard++)
    {
      try
      {
        // ASSERTION: inFile.read() will get first byte of three-byte wildcard
        int wildcardChar = inFile.read();
          // first byte is wildcard character character
        int number = inFile.read();
          // next is number of wildcard match, 1, 1+ or 0+
        int flags = inFile.read();
          // third and final is flags for wildcard
        // ASSERTION: three bytes have been read -> next inFile.read() will get
        //  first byte of next three-byte wildcard
//System.err.println("Wildcard=" + (char) wildcardChar + " flags=" + flags);
        wildcards[wildcard] = new Wildcard256(wildcardChar, number, flags);
      }
      catch (IOException e)
  	  {
        throw new IOException("Unable to process: " + filename + " while " +
          "attempting to read wildcards.  Error: " + e);
      }
    }
    // ASSERTION: next inFile.read() will get number of states in decision table

    // DECISION TABLE
    // First, get dimensions of table, inputClasses * numberStates
    numberStates = 0;  // required by compiler
    numberInputClasses = 0;  // required by compiler
    try
    {
      numberStates = inFile.read();
      numberInputClasses = inFile.read();
    }
    catch (IOException e)
    {
        throw new IOException("Unable to process: " + filename + " while " +
          "attempting to read state and input class numbers.  Error: " + e);
    }
    // ASSERTION: next inFile.read() will get first byte of decision table

    // declare table of correct dimensions
    stateTable = new boolean[numberStates][numberInputClasses];

    // populate table
    for (int state = 0; state < numberStates; state++)
    {
      for (int inputclass = 0; inputclass < numberInputClasses; inputclass++)
      {
        try
        {
          int tableValue = inFile.read();
          if (tableValue == 0)
            stateTable[state][inputclass] = false; // zero value = false
          else
            stateTable[state][inputclass] = true; // non-zero value == true
        }
        catch (IOException e)
        {
          throw new IOException("Unable to process: " + filename + " while " +
            "attempting to read state table information.  Error: " + e);
        }
      }
    }

    // TRANSLATION RULES + HASHTABLE

    // Declare translationRule with maximum size
    translationRule = new TranslationRule256[MAX_NUMBER_TRANSLATION_RULES];

    // Now import the translation rules and build the hashtable
    // ASSERTION: pointing at inputClass
    int lastgot = 0; // use to track last rule focus category for hashtable
    int got = 0;
    try
    {
      got = inFile.read();
    }
    catch (IOException e)
    {
          throw new IOException("Unable to process: " + filename + " while " +
            "attempting to read translation rules.  Error: " + e);
    }
    // ASSERTION: got = inputClass, pointing at size of rule
    int ruleCount = 0;
    while (got != TABLE_DELIMITER)
    // ASSERTION: inputClass != '#' => there is another rule
    {
      try
      {
        int inputClass = got;  // got is inputClass
        // ASSERTION: now pointing at size-of-rule
        got = inFile.read(); // got size-of-rule
        // ASSERTION: now pointing at beginning of rule content = left context
        int[] rule = new int[RULE_BUFFER];
        int ruleIndex = 0;
        got = inFile.read(); // got first character of rule content
        while (got != LEFT_FOCUS_DELIMITER)
        {
          rule[ruleIndex++] = got;  // one character added to left, increment size by one.
          got = inFile.read();
        }
        int leftLength = ruleIndex; // end of left is here
        // ASSERTION: now pointing at beginning of focus and got == '['
        got = inFile.read(); // got first character of focus
        // Add entry to hashtable if new character encountered
        if (lastgot != got)
        {

          characterRule[lastgot].lastTranslationRuleIndex = ruleCount - 1;
          lastgot = got;
          characterRule[lastgot].translationRuleIndex = ruleCount;

        }
        rule[ruleIndex++] = got; // the focus must have at least one char
        got = inFile.read();              // in it, even ']'.  First char to focus.
        while (got != RIGHT_FOCUS_DELIMITER)
        {
          rule[ruleIndex++] = got;  // added one character to focus, so increment size by one
          got = inFile.read();
        }
        int focusLength = ruleIndex - leftLength;
        // ASSERTION: now pointing at beginning of right context and got == ']'
        got = inFile.read(); // got first character of right context
        while (got != RULE_OUTPUT_DELIMITER)
        {
          rule[ruleIndex++] = got;  // added one character to right, so increment size by one.
          got = inFile.read();
        }
        int rightLength = ruleIndex - leftLength - focusLength;
        // ASSERTION: now pointing at beginning of output and got == '='
        got = inFile.read(); // got first character of output
        while (got != RULE_CONTENT_DELIMITER)
        {
          rule[ruleIndex++] = got; // added one character to output, so increment size by one
          got = inFile.read();
        }
        int outputLength = ruleIndex - leftLength - focusLength - rightLength;

        // ASSERTION: now pointing at output state and got == 0
        int newState = inFile.read(); // got output state
        // ASSERTION: now pointing at first character of next rule = input OR '#'

        // Trim the excess int[]s
        int[] left = new int[leftLength];
        int[] focus = new int[focusLength];
        int[] right = new int[rightLength];
        int[] output = new int[outputLength];
        System.arraycopy(rule, 0, left, 0, leftLength);
        System.arraycopy(rule, leftLength, focus, 0, focusLength);
        System.arraycopy(rule, leftLength + focusLength, right, 0, rightLength);
        System.arraycopy(rule, leftLength + focusLength + rightLength, output, 0, outputLength);

        // Build the rule
        translationRule[ruleCount++] = new TranslationRule256(left,
          right, focus, inputClass,
          newState, output);

        got = inFile.read();
        // get either the first character of next rule OR '#' to indicate end
      }
      catch (IOException e)
      {
        throw new IOException("Unable to process: " + filename + " while " +
          "attempting to read translation rules.  Error: " + e);
      }
    }
    // Mark the end of the last focus category
    characterRule[lastgot].lastTranslationRuleIndex = ruleCount - 1;

    // Close the language file after use
    try
    {
      inFile.close();
    }
    catch (IOException e)
    {
        throw new IOException("Unable to process: " + filename + " while " +
          "attempting to close language file after reading.  Error: " + e);
    }
  } // end of Language256 constructor


  // METHODS *******************************************************************

  private boolean compareLeftContext(int[] input, int position)
  //Returns true if the sequence of characters to the left of position in input
  //matches the left context of the current rule, false otherwise.
  {
    int inputIndex = position - 1;
    int[] leftContext = translationRule[transRuleIndex].leftContext;
    int contextIndex = leftContext.length - 1;
    while (contextIndex >= 0)
    {
      int leftContextChar = leftContext[contextIndex];
      int leftContextFlags = characterRule[leftContextChar].flags;
//System.err.println("leftContChar=" + (char) leftContextChar + " flags=" + leftContextFlags);

      if ((WILDCARD_FLAG & leftContextFlags) == WILDCARD_FLAG)
      // ASSERTION: Wildcard found
      {
        // Search through wildcard array for matching wildcard
        for (int thisWildcard = 0; thisWildcard < numberWildcards; thisWildcard++)
        {
          if (wildcards[thisWildcard].character == leftContextChar)
          // ASSERTION: this wildcard is the matching one
          {
            // WILDCARD_NONE
            if (wildcards[thisWildcard].number == WILDCARD_NONE)
            {
              contextIndex--;
              while ((inputIndex >= 0) && (flagsEqual(wildcards[thisWildcard].flags, input[inputIndex])))
                inputIndex--;
            } // end of WILDCARD_NONE

            // WILDCARD_ONE
            if (wildcards[thisWildcard].number == WILDCARD_ONE)
            {
              if (inputIndex < 0)
              // ASSERTION: checking has progressed beyond end of input - only
              // permissable is wildcard indicates a SPACE character
              {
                if (flagsEqual(wildcards[thisWildcard].flags, SPACE))
                  return true; // SPACE wildcard, so okay to match against outside input
                else
                  return false; // there should be at least one
              }
              if (!flagsEqual(wildcards[thisWildcard].flags, input[inputIndex]))
                return false;
              inputIndex--;
              contextIndex--;
            } // end of WILDCARD_ONE

            // WILDCARD_SEVERAL
            if (wildcards[thisWildcard].number == WILDCARD_SEVERAL)
            {
              if (inputIndex < 0)
              // ASSERTION: checking has progressed beyond end of input - only
              // permissable is wildcard indicates a SPACE character
              {
                if (flagsEqual(wildcards[thisWildcard].flags, SPACE))
                  return true; // SPACE wildcard, so okay to match against outside input
                else
                  return false; // there should be at least one
              }
              if (!flagsEqual(wildcards[thisWildcard].flags, input[inputIndex]))
                return false;
              inputIndex--;
              while ((inputIndex >= 0) && (flagsEqual(wildcards[thisWildcard].flags, input[inputIndex])))
                inputIndex--;
              contextIndex--;
            } // end of WILDCARD_SEVERAL
          } // end of search for wildcard
        } // end Wildcard found


      } // end of wildcard found
      else
      // ASSERTION: not a wildcard
      {
        if (inputIndex < 0)
          return false;
        if (input[inputIndex] != leftContextChar)
          return false;
        inputIndex--;
        contextIndex--;
      }
    }  // end of while contextIndex > 0
    return true;
  }

  private boolean compareRightContext(int[] input, int position)
  //Returns true if the sequence of characters to the right of (position + the
  //focusLength of the current rule) in input matches the right context of the
  //current rule, false otherwise.
  {
    int inputIndex = position + translationRule[transRuleIndex].focus.length;
    int[] rightContext = translationRule[transRuleIndex].rightContext;
    int contextIndex = 0;
    while (contextIndex < rightContext.length)
    {
      int rightContextChar = rightContext[contextIndex];
      int rightContextFlags = characterRule[rightContextChar].flags;

      if ((WILDCARD_FLAG & rightContextFlags) == WILDCARD_FLAG)
      // ASSERTION: Wildcard found
      {
        // Search through wildcard array for matching wildcard
        for (int thisWildcard = 0; thisWildcard < numberWildcards; thisWildcard++)
        {
          if (wildcards[thisWildcard].character == rightContextChar)
          // ASSERTION: this wildcard is the matching one
          {
            // WILDCARD_NONE
            if (wildcards[thisWildcard].number == WILDCARD_NONE)
            {
              contextIndex++;
              while ((inputIndex < input.length) && (flagsEqual(wildcards[thisWildcard].flags, input[inputIndex])))
                inputIndex++;
            } // end of WILDCARD_NONE

            // WILDCARD_ONE
            if (wildcards[thisWildcard].number == WILDCARD_ONE)
            {
              if (inputIndex >= input.length)
              // ASSERTION: checking has progressed beyond end of input - only
              // permissable is wildcard indicates a SPACE character
              {
                if (flagsEqual(wildcards[thisWildcard].flags, SPACE))
                  return true; // SPACE wildcard, so okay to match against outside input
                else
                  return false; // there should be at least one
              }
              if (!flagsEqual(wildcards[thisWildcard].flags, input[inputIndex]))
                return false;
              inputIndex++;
              contextIndex++;
            } // end of WILDCARD_ONE

            // WILDCARD_SEVERAL
            if (wildcards[thisWildcard].number == WILDCARD_SEVERAL)
            {
              if (inputIndex >= input.length)
              // ASSERTION: checking has progressed beyond end of input - only
              // permissable is wildcard indicates a SPACE character
              {
                if (flagsEqual(wildcards[thisWildcard].flags, SPACE))
                  return true; // SPACE wildcard, so okay to match against outside input
                else
                  return false; // there should be at least one
              }
              if (!flagsEqual(wildcards[thisWildcard].flags, input[inputIndex]))
                return false;
              inputIndex++;
              while ((inputIndex < input.length) && (flagsEqual(wildcards[thisWildcard].flags, input[inputIndex])))
                inputIndex++;
              contextIndex++;
            } // end of WILDCARD_SEVERAL
          } // end of search for wildcard
        } // end Wildcard found


      } // end of wildcard found
      else
      // ASSERTION: not a wildcard
      {
        if (inputIndex >= input.length)
          return false;
        if (input[inputIndex] != rightContextChar)
          return false;
        inputIndex++;
        contextIndex++;
      }
    }  // end of while contextIndex < input.length
    return true;
  }

  private boolean compareFocus(int[] input, int position)
  //Returns true if the sequence of characters from position in input matches
  //the focus of the current rule, false otherwise.
  /*
    Preconditions
      position + ruleFocusLength <= input.length
  */
  {
    if (position + translationRule[transRuleIndex].focus.length > input.length)
      return false;

    int focusLength = translationRule[transRuleIndex].focus.length;
    for (int i = 0; i < focusLength; i++)
    {
      if (input[position + i] != translationRule[transRuleIndex].focus[i])
        return false;
    }
//System.err.print("Matched: ");
//for (int i = position; i < position + focusL; i++)
//  System.err.print((char) input[i]);
//System.err.println();
    return true;
  }

  private boolean checkState()
  //Returns the value of the stateTable for the state of the machine and the
  //inputClass of the current rule. This is true if the rule can be applied, and
  //false otherwise. It is equal to stateTable[inputClass][state].
  /*
    Input and initial conditions
      state
        state' = state--
        0 <= state' < numberStates
      transRuleIndex
        0 <= transRuleIndex <= numberTranslationRules
      inputClass
        inputClass' = inputClass--
        0 <= inputClass' <= numberInputClasses
    Output and final conditions
      boolean
        true iff stateTable[state][current rule input class] == true, else false
  */
  {

    int indexState = this.state - 1;
//System.err.print("indexState=" + indexState);
    int ruleInputClassIndex = translationRule[transRuleIndex].inputClass - 1;
//System.err.print(" inputClassIndex=" + ruleInputClassIndex);
    // try to return result
    return stateTable[indexState][ruleInputClassIndex];
//System.err.println(" result=" + result);
  }

  private int[] getOutput()
  //Returns the output value of the current rule.
  {
    return translationRule[transRuleIndex].output;
  }

  public int getState()
  // returns the current state of the state machine
  {
    return state;
  }

  private void getNewState()
  //Returns the newState value of the current rule, or zero if there is no new value.
  // Input and initial conditions
  //  0 <= newMachineState <= numberStates
  {
    int newMachineState = translationRule[transRuleIndex].newState;
    if (newMachineState != 0)
//    {
//System.err.println("Changed state from " + state + " to " + newMachineState);
        state = newMachineState;
//    }
  }

  private int mapCharacter(int characterToMap)
  /*
    This takes the integer value of an input character and uses this as an
    index into the characterRule array to find the mapped attribute of the
    character rule.
  */
  {
    if (characterToMap < 0)
    {
      System.err.println("Failed to map character in Language256.mapCharacter - " +
        "value to match < 0 : " + characterToMap);
      return SPACE;
    }
    if (characterToMap > NUMBER_CHARACTER_RULES)
    {
      System.err.println("Failed to map character in Language256.MapCharacter - " +
        "value to match > " + NUMBER_CHARACTER_RULES + " : " + characterToMap);
      return SPACE;
    }
    return characterRule[characterToMap].mapped;
  } // end of MapCharacter

  private boolean flagsEqual(int flags, int inputChar)
  /*
    Used to check the flags of two characters.
    Input:  array of input characters and position of character to check
            character flags of rule character to match to this
    Output: true if flags match, false otherwise
  */
  {
    return ((flags & characterRule[inputChar].flags) != 0);
  } // end of FlagsEqual

  public int getPermittedStates()
  // Returns the number of permitted states of the finite state machine.
  {
    return numberStates;
  } // end of GetPermittedStates



  public int[] translate(int[] toConvert)
  // Takes an array of characters as integers and translates them according
  // to the language and state defined in the constructor and SetState
  {
//for (int i = 0; i < toConvert.length; i++)
//System.err.println("i=" + i + " toConvert=" + toConvert[i]);
    int finish = toConvert.length;
      // counter of where we try to go to
      // INV1: 0 <= start < finish
    int[] converted = new int[(int) (finish * MAX_COMPRESS)];
      // to return
    int start = 0;
      // counter of where we start looking in the word string
    int outputUpTo = 0;
      // where we've output to so far
    state = defaultState;

    // Convert the input string by mapping it using MapCharacter
    int[] mappedToConvert = new int[finish];
    for (int inputChar = 0; inputChar < finish; inputChar++)
      mappedToConvert[inputChar] = mapCharacter(toConvert[inputChar]);
    toConvert = mappedToConvert;

    while (start < finish)
    // assertion: start does not yet indicate the end of the toConvert array
    // assertion: not all of word is converted
    {
      boolean matchFound = false;
        // indicates whether we've got a match

      // set language to first rule with matching focus
      int focusChar = toConvert[start];
//System.err.print(" focus=" + (char) focusChar);
      transRuleIndex = characterRule[focusChar].translationRuleIndex;
//System.err.print(" start tRule=" + transRuleIndex);
//System.err.print(" output = " + (char) translationRule[transRuleIndex].output[0]);
//System.err.print(" previous= " + (char) translationRule[transRuleIndex - 1].output[0]);
      int lastFocusIndex = characterRule[focusChar].lastTranslationRuleIndex;
      if (matchFound = compareFocus(toConvert, start))
        if (matchFound = checkState())
          if (matchFound = compareLeftContext(toConvert, start))
            matchFound = compareRightContext(toConvert, start);
      while (!matchFound &&  (transRuleIndex < lastFocusIndex))
      // assertion: no match has yet been found
      // assertion: there are still rules with the same focus initial that might match
      {
        transRuleIndex++;
//System.err.print(" rule=" + transRuleIndex);

        if (matchFound = compareFocus(toConvert, start))
          if (matchFound = checkState())
            if (matchFound = compareLeftContext(toConvert, start))
              matchFound = compareRightContext(toConvert, start);
      }
      // assertion: matchFound OR no more rules

      if (matchFound)
      {
        int[] outputGot = getOutput();  // result of rule
        int numberCharsInOutput = outputGot.length;  // size of result of rule
        int i = 0;
        try
        {
//System.err.println("numberCharsInOutput=" + numberCharsInOutput);
          for (i = 0; i < numberCharsInOutput; i++)
          {
            converted[outputUpTo] = outputGot[i];
            outputUpTo++; // one character from focus transferred to output
          }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
//System.err.println("Error thrown");
          converted = resizeConverted(i, outputGot, converted, finish - start);
          outputUpTo += numberCharsInOutput - i;
//System.err.println("new converted=" + converted.length);
        }
        getNewState();  // get new state
        start = start + translationRule[transRuleIndex].focus.length;
//System.err.println(translationRule[transRuleIndex].focusLength + " " + start);
          // move along input by size of focus
      }
      else
      // assertion: !matchFound, no match found for focus
      {
        converted[outputUpTo] = toConvert[start];
//System.err.println("failed at " + start + " which is character " +
//  (new Character((char) toConvert[start]).toString()) + ", value " + toConvert[start]);
          // if nothing matched, leave character untranslated
        start++;  // try matching next character
        outputUpTo++;  // output to next position
        state = defaultState;  // set state to default
      }
    }


    // chop off redundant spaces at end of output array
    int[] toReturn = new int[outputUpTo];
    System.arraycopy(converted, 0, toReturn, 0, outputUpTo);

    return toReturn;
  } // end of int[] translate

  public String translate(String toConvert)
  // Simply converts input string into array of integers and calls
  // Translate(int[])
  {
    int length = toConvert.length();
    int[] toConvertArray = new int[length];
    for (int i = 0; i < length; i++)
      toConvertArray[i] = (int) toConvert.charAt(i);

    int[] resultArray = translate(toConvertArray);

    length = resultArray.length;
    StringBuffer result = new StringBuffer("");
    for (int i = 0; i < length; i++)
      result.append((char) resultArray[i]);
    return result.toString();
  } // end of Translate(String)

  private int[] resizeConverted
    (int outputIndex, int[] outputContent, int[] converted, int remainderToTranslate)
  {
//System.err.println("remainder=" + remainderToTranslate);
    int[] resizedConverted = new int[(int) (converted.length + remainderToTranslate * MAX_COMPRESS)];
//System.err.println("new size=" + resizedConverted.length);
    if (resizedConverted.length < (converted.length + outputContent.length -
      outputIndex))
    {
      resizedConverted = resizeConverted(outputIndex, outputContent,
        resizedConverted, remainderToTranslate * 2);
    }
    System.arraycopy(converted, 0, resizedConverted, 0, converted.length);
    System.arraycopy(outputContent, outputIndex, resizedConverted,
      converted.length, (outputContent.length - outputIndex));
    return resizedConverted;
  }

  public boolean setState(int newState)
  // Allows the user to request a new default state for the virtual machine
  {
    if (newState > numberStates)
    {
//      System.err.println("Attempted to set new state: " + newState + " greater than "
//        + "permitted number of states: " + numberStates);
      return false;
    }
    if (newState < 1)
    {
//      System.err.println("Attempted to set new state: " + newState + " less than 1");
      return false;
    }
    defaultState = newState;
    return true;
  } // end of SetState

/*
  public String viewTranslationRule(int ruleNumber)
  // Allows a translation rule to be viewed
  {
    if ((ruleNumber < 0) || (ruleNumber > numberTranslationRules))
      return "";
    return translationRule[ruleNumber].toString();
  } // end of viewTranslationRule
*/

  private class TranslationRule256 implements Serializable
  // holds one rule of the translation table
  {
    private int[] leftContext;
    private int[] rightContext;
    private int[] focus;
    private int inputClass;
    private int newState;
    private int[] output;

    private TranslationRule256(int[] newLeftContext, int[] newRightContext, int[] newFocus,
      int newInputClass, int newNewState, int[] newOutput)
    // Constructor for a full rule
    {
  	  leftContext = newLeftContext;
  	  rightContext = newRightContext;
  	  focus = newFocus;
  	  inputClass = newInputClass;
  	  newState = newNewState;
  	  output = newOutput;
    }

    private TranslationRule256(TranslationRule256 oldRule)
    {
  	  leftContext = oldRule.leftContext;
  	  rightContext = oldRule.rightContext;
  	  focus = oldRule.focus;
  	  inputClass = oldRule.inputClass;
  	  newState = oldRule.newState;
  	  output = oldRule.output;
    }
  } // end of TranslationRule256 class

  private class Wildcard256 implements Serializable
  // ADT that implements a single wildcard
  {
    private int character; // the character in the translation rules for this wildcard
    private int number;  // whether the wildcard matches 1, 1+ or 0+ characters
    private int flags; // the character properties this wildcard should match

    private Wildcard256(int character, int number, int flags)
    {
      this.character = character;
      this.number = number;
      this.flags = flags;
    }
  } // end of Wildcard256 class

  private class CharacterRule256 implements Serializable
  // A character rule
  {
    private int mapped; // the character in the text
    private int upper; // the character to which it maps
    private int flags; // used for legacy systems
    private int translationRuleIndex; // used to hash into the translation rules
    private int lastTranslationRuleIndex;

    private CharacterRule256(int mapped, int upper, int flags)
    {
      this.mapped = mapped;
  	  this.upper = upper;
  	  this.flags = flags;
    }
  } // end of CharacterRule256 class


} // END OF Language256 class




