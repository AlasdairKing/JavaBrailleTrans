package uk.ac.umist.co.brailletrans;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * An implementation of <CODE>Language</CODE> that works with Unicode strings and
 * Unicode language tables.  
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class LanguageUnicode implements Language, Serializable
{
  // ***** VARIABLES ***************************************************
  // public Class variables

  /**
   * Filename extension for serialized <CODE>LanguageUnicode</CODE> objects
   * that can be loaded into memory by calling the constructor with their path
   * and name and used for translation.
   */
  public final static String FILENAME_EXTENSION = "ulf";
  /**
   * The filename extension for the unprocessed Language rules tables for this
   * implementation of language.
   */
  public final static String DATAFILE_EXTENSION = "ucn";

  /**
   * The first character forming part of the escape sequence for unicode characters
   * to be depicted in ASCII.
   */
  public final char ESCAPE_CHAR_1 = '\\';
  /**
   * The second character forming part of the escape sequence for unicode characters
   * to be depicted in ASCII.
   */
  public final char ESCAPE_CHAR_2 = 'u';

  // private Class variables
  private final static double HASHTABLE_SIZE_OPTIMIZER_PRODUCT = 1.5;
  private final static int HASHTABLE_SIZE_OPTIMIZER_OFFSET = 1;
  private final static int UNDEFINED = -1;
  private final static char SPACE=' ';
  // private instance variables (there are no public instance variables)
  private int state;
    // current state of finite state machine
  private int defaultState;
    // state of machine when first instantiated, or 1 if not specified.

  private int version;
  //The version number of the table: indicates it is appropriate for use
  private String name;
  // Name of the language
  private String description;
  // Any additional information on the language

  private boolean[][] stateTable;
  //The matrix cross-referencing machine state with input class to see whether
  //confirm that a rule can be used.  stateTable[state][inputclass].
  private int numberInputClasses;
  private int numberStates;
  // Size of the stateTable
  private String[] stateDescriptions;
  private String[] inputClassDescriptions;

  private Hashtable characterMapper;
  // maps characters in input to translation characters
  private Hashtable charFlagGetter;
  // gets flags for a given character
  private Hashtable tRuleGetter;
  // gets foci groups for a given character
  private int numberCharacters;

  private TranslationRuleUnicode currentRule;

  private WildcardUnicode[] wildcards;
  // Represents the wildcards for the language
  private Hashtable wildcardGetter;
  // gets a wildcard for a character, if any
  private int numberWildcards;
  // number of wildcards for language

  //***** CONSTRUCTORS ***************************************************
  /**
   * Constructs a LanguageUnicode object without data ready to be filled with
   * information for the language being created in the correct order.
   */
  public LanguageUnicode()
  {
    super();
    numberCharacters = UNDEFINED; // indicates that the number of characters is not
      // yet defined and the hashtable has not been fixed yet.
    numberWildcards = UNDEFINED;
    numberInputClasses = UNDEFINED;
    numberStates = UNDEFINED;
  }

  /**
   * Loads a LanguageUnicode object with all the language rules table information
   * required for translation from disk.  It should have previously been constructed
   * with the parameter-less constructor, then populated with language data by
   * the various <CODE>set</CODE> methods, then serialized to disk with <CODE>
   * writeLanguageUnicodeToDisk</CODE>, before it can be loaded in by this
   * constructor.  If a supplied LanguageUnicode object is provided, this can be
   * interrogated for its translation abilities with the <CODE>get</CODE> methods.
   *
   * @param filename Full path and name of serialized Java object as file to load,
   * excluding the filename extension.  For example, language file "english.dat" in
   * "\languages" should be instantiated with <CODE>LanguageUnicode myLanguage =
   * new LanguageUnicode("\languages\english");</CODE>
   */
  public LanguageUnicode(String filename) throws IOException, ClassNotFoundException, FileNotFoundException
  {
    ObjectInputStream inObject = null;
    LanguageUnicode fromDisk = null;
    String objectFilename = filename + FILE_EXTENSION_DELIMITER + FILENAME_EXTENSION;
    try
    {
      inObject = new ObjectInputStream(new BufferedInputStream(new FileInputStream(objectFilename)));
      fromDisk = (LanguageUnicode) inObject.readObject();
      inObject.close();
    }
    catch (FileNotFoundException e)
    {
      throw new FileNotFoundException("Could not find file " + objectFilename);
    }
    catch (IOException e)
    {
      throw new IOException("Could not read language file from disk.  Error: " + e);
    }
    catch (ClassNotFoundException e)
    {
      throw new ClassNotFoundException("Could not get language data from file.  Confirm that " +
        "file is a java language file: " + e);
    }
    this.version = fromDisk.version;
    this.name = fromDisk.name;
    this.description = fromDisk.description;
    this.stateTable = fromDisk.stateTable;
    this.numberInputClasses = fromDisk.numberInputClasses;
    this.numberStates = fromDisk.numberStates;
    this.inputClassDescriptions = fromDisk.inputClassDescriptions;
    this.stateDescriptions = fromDisk.stateDescriptions;
    this.characterMapper = fromDisk.characterMapper;
    this.charFlagGetter = fromDisk.charFlagGetter;
    this.tRuleGetter = fromDisk.tRuleGetter;
    this.numberCharacters = fromDisk.numberCharacters;
    this.wildcards = fromDisk.wildcards;
    this.wildcardGetter = fromDisk.wildcardGetter;
    this.numberWildcards = fromDisk.numberWildcards;
    defaultState = state = 1;
    currentRule = null;
    return;

  }

  //***** PUBLIC METHODS - UTILITY *********************************
  /**
   * Loads a LanguageUnicode object from disk
   *
   * @param filename Full path and name of serialized Java object as file to load.
   */
  public LanguageUnicode getLanguageUnicodeFromDisk(String filename)
    throws IOException, FileNotFoundException, StreamCorruptedException,
      ClassNotFoundException
  {
    ObjectInputStream inFile = null;
		  // Used for object input
    filename = filename + FILE_EXTENSION_DELIMITER + FILENAME_EXTENSION;
    LanguageUnicode fromDisk = null;
      // used for file reading

 	  // Try to open local file "filename"
	  try
    {
      inFile = new ObjectInputStream(new FileInputStream(filename));
      fromDisk = (LanguageUnicode) inFile.readObject();
    }
	  catch (FileNotFoundException e)
	  {
      throw new FileNotFoundException("File not found: " + filename + ".  " +
        "Remember not to add the filename extension, " + FILENAME_EXTENSION);
    }
    catch (StreamCorruptedException e)
    {
      throw new StreamCorruptedException("Stream corrupted loading language file.  Error: " + e);
    }
    catch (IOException e)
    {
      throw new IOException("Problems reading language from file.  Error: " + e);
    }
    catch (ClassNotFoundException e)
    {
      throw new ClassNotFoundException("Problems reading object from file, class not found.  Error: " + e);
    }
    return fromDisk;
  } // end of GetLanguageUnicodeFromDisk

  //***** PUBLIC METHODS - TRANSLATION ********************************
  public boolean setState(int newState)
  {
    if ((newState > 0) && (newState <= numberStates))
    {
      defaultState = state = newState;
      return true;
    }
    else
      return false;
  }

  public int getPermittedStates()
  {
    return numberStates;
  }

  public int getState()
  {
    return state;
  }

  public String translate(String toConvert)
  {
//System.out.println("toConvert=" + toConvert);
    int finishIndex = toConvert.length();
      // counter of where we try to go to
      // INV1: 0 <= start < finish
    StringBuffer converted = new StringBuffer("");
      // to return
    int startIndex = 0;
      // counter of where we start looking in the word string
    int convertedIndex = 0;
      // where we've output to so far
    state = defaultState;
      // reset to default

    // Convert the input string
    StringBuffer mappedToConvert = new StringBuffer("");
    for (int i = 0; i < finishIndex; i++)
      mappedToConvert.append(mapCharacter(toConvert.charAt(i)));
    toConvert = mappedToConvert.toString();

//System.err.println("Hashtable tRuleGetter size = " + tRuleGetter.size());

    while (startIndex < finishIndex)
    // assertion: start does not yet indicate the end of the toConvert array
    // assertion: not all of word is converted
    {
//stem.err.println("startIndex=" + startIndex + " finishIndex=" + finishIndex);
      boolean matchFound = false;  // indicates whether we've got a match
      char first = toConvert.charAt(startIndex);
//System.err.println(first);
      currentRule = (TranslationRuleUnicode) tRuleGetter.get(new Character(first));
        // set language to first rule with matching focus
      if (currentRule == null)
      {
        converted.append(toConvert.charAt(startIndex));
          // if no character found to matched, leave character untranslated
        startIndex++;  // try matching next character
        state = defaultState;  // set state to default
        continue;
      }

      if (matchFound = compareFocus(toConvert, startIndex))
      {
//if (startIndex < 2)
//System.err.println("Matched focus");
        if (matchFound = compareState())
          if (matchFound = compareLeftContext(toConvert, startIndex))
            matchFound = compareRightContext(toConvert, startIndex);
      }

      while (!matchFound &&  (!currentRule.lastInCategory))
      // assertion: no match has yet been found
      // assertion: there are still rules with the same focus initial that might match
      {
        currentRule = currentRule.nextRule;
//if (currentRule.focus.equals("?"))
//  System.err.println("index=" + startIndex + " output=" + currentRule.output);

//if (startIndex < 2)
//System.err.println("index=" + startIndex + " rule=" + translationRules.PrintCurrentRule());
        if (matchFound = compareFocus(toConvert, startIndex))
        {
//if (startIndex < 2)
//System.err.println("Matched focus");
          if (matchFound = compareState())
          {
//if (startIndex < 2)
//System.err.println("Matched state");
            if (matchFound = compareLeftContext(toConvert, startIndex))
            {
//if (startIndex < 2)
//System.err.println("Matched left");

              matchFound = compareRightContext(toConvert, startIndex);
//if (startIndex < 2 && matchFound)
//System.err.println("Matched right");

            }
          }
        }
      }
      // assertion: matchFound OR no more rules

      if (matchFound)
      {
//System.err.println("Matched " + first + " with " + currentRule.output);
        converted.append(currentRule.output);
        state = getNewState();
        startIndex += currentRule.focusLength;
      }
      else
      // assertion: !matchFound, no match found for focus
      {
        converted.append(toConvert.charAt(startIndex));
          // if no character found to matched, leave character untranslated
        startIndex++;  // try matching next character
        state = defaultState;  // set state to default
      }
    }
//System.out.println("result=" + converted.toString());
    return converted.toString();
   }

  public int[] translate(int[] toConvert)
  {
    StringBuffer toConvertS = null;
    for (int i = 0; i < toConvert.length; i++)
      toConvertS.append((char) toConvert[i]);
    String toReturnS = new String(translate(toConvertS.toString()));
    int toReturnSL = toReturnS.length();
    int[] toReturn = new int[toReturnSL];
    for (int i = 0; i < toReturnSL; i++)
      toReturn[i] = toReturnS.charAt(i);
    return toReturn;
  }

  //***** PRIVATE METHODS - TRANSLATION *********************************
  private char mapCharacter(char toMap)
  // returns the character to which an input char should be transformed,
  // the input char back if no transformation is found.
  {
    Character mapped = (Character) characterMapper.get(new Character (toMap));
    if (mapped == null)
      return SPACE;
    else
      return mapped.charValue();
  } // end of MapCharacter(char)

  //***** PUBLIC METHODS - LANGUAGE CONSTRUCTION *************************

  /**
   * Defines the number of wildcards for this language.  Can only be called once
   * for any one new language.
   *
   * @param numberWildcards    int number of wildcards.
   */
  public void setNumberWildcards(int numberWildcards) throws LanguageDefinitionException
  {
    if (this.numberWildcards != UNDEFINED)
      throw new LanguageDefinitionException("Number of wildcards already set, can only be set once.");
    this.numberWildcards = numberWildcards;
    wildcardGetter = new Hashtable(numberWildcards);
  }

  /**
   * Returns the number of wildcards defined in the language
   *
   * @return numberWildcards   Number of wildcards in language.
   */
  public int getNumberWildcards()
  {
    return numberWildcards;
  }

  /**
   * Returns the version of the language
   *
   * @return version   Version of the language.
   */
  public int getVersionNumber()
  {
    return version;
  }

  /**
   * Adds the information for one Unicode character to the language.
   *
   * @param from    Character object wrapping the character that will be mapped
   *                from in normalising the input text.
   * @param to      Character object wrapping the character that is produced
   *                when mapping the input text.
   * @param flagValue   The distinctive flags for the character in to.
   */
  public void addCharacterInformation(Character from,  Character to, Integer flagValue) throws LanguageDefinitionException
  {
    if (numberCharacters == UNDEFINED)
      throw new LanguageDefinitionException("Number of characters must be set before"
        + " character information can be added.  Use setNumberCharacters");
    if (characterMapper.size() == numberCharacters)
      throw new LanguageDefinitionException("The number of characters for this"
        + " language has been set to " + numberCharacters + ".  This limit in"
        + " character rules has been met.  No more character rules may be added");
    characterMapper.put(from, to);
    charFlagGetter.put(to, flagValue);
  }

  /**
   * Adds one wildcard's information to the language
   *
   * @param wildcardNumber  The value indicating whether the wildcard matches
   *                        zero or more characters (WILDCARD_NONE), one character
   *                        (WILDCARD_ONE), or one or more characters
   *                        (WILDCARD_SEVERAL).
   * @param wildcardFlags   The distinctive flags that the wildcard matches.
   * @param wildcardChar    Character object wrapping the character used for the
   *                        wildcard.
   */
  public void addWildcardInformation(int wildcardNumber, int wildcardFlags, Character wildcardChar) throws LanguageDefinitionException
  {
    if (numberCharacters == UNDEFINED)
      throw new LanguageDefinitionException("Number of wildcards must be set before"
        + "wildcard information can be added.  Use setNumberWildcards.");
    WildcardUnicode newWildcard = new WildcardUnicode(wildcardNumber, wildcardFlags);
    wildcardGetter.put(wildcardChar, newWildcard);
  }

  /**
   * Adds a new translation rule to the language.  The <CODE>String</CODE> argument
   * must be in the format "<CODE>inputclass [TAB] leftContext LEFT_FOCUS_DELIMITER
   * focus RIGHT_FOCUS_DELIMITER rightContext RULE_OUTPUT_DELIMITER output
   * RULE_CONTENT_DELIMITER [TAB] newState</CODE>".  The CONSTANTS are inherited
   * from <CODE>Language</CODE>.
   *
   * @param toProcess   <CODE>String</CODE> containing whole translation rule content, input
   *                    class to new state.
   */
  public void addTranslationRule(String toProcess) throws LanguageDefinitionException
  {
    TranslationRuleUnicode newRule = parseTranslationRule(toProcess);
    char focusCategory = newRule.firstCharOfFocus;
    if (tRuleGetter.containsKey(new Character(focusCategory)))
      addToExistingFocusCategory(focusCategory, newRule);
    else
      addToNewFocusCategory(focusCategory, newRule);
  }

  private void addToExistingFocusCategory(char focusCategory, TranslationRuleUnicode newRule)
  {
    TranslationRuleUnicode ruleToAddTo = (TranslationRuleUnicode) tRuleGetter.get(new Character(focusCategory));
    while (!ruleToAddTo.lastInCategory)
      ruleToAddTo = ruleToAddTo.nextRule;
    // ASSERTION: ruleToAddTo is now last rule in category.
    ruleToAddTo.lastInCategory = false;
    newRule.lastInCategory = true;
    ruleToAddTo.nextRule = newRule;
  }

  private void addToNewFocusCategory(char focusCategory, TranslationRuleUnicode newRule)
  {
    newRule.lastInCategory = true;
    tRuleGetter.put(new Character(focusCategory), newRule);
  }

  private boolean compareState()
  {
    return (stateTable[state-1][currentRule.inputClass-1]);
  }

  private boolean compareFocus(String toCompare, int index)
  {
    // check that there is enough input text left to match this focus
    if ((index + currentRule.focusLength) > toCompare.length())
      return false;
    else
  // it does, so test to see if focus matches input text
      return currentRule.focus.equals(
        toCompare.substring(index, index + currentRule.focusLength));
  }

  private boolean compareLeftContext(String toConvert, int startIndex)
  {
//System.err.println("LeftContext=<" + leftContext + "> where startIndex=" + startIndex + ", <" + toConvert.charAt(startIndex) + ">");
    int leftContextLength = currentRule.leftContext.length();
    if (leftContextLength == 0)
      return true; // always match an empty left context
    int inputIndex = startIndex - 1; // where to start looking for left con
    for (int contextIndex = leftContextLength -1 ; contextIndex >= 0; contextIndex--)
      // ie until we get to the end of the left context
    {
      char contextChar = currentRule.leftContext.charAt(contextIndex);
      // first, check right context character isn't a wildcard
      WildcardUnicode wildcard = (WildcardUnicode) wildcardGetter.get(new Character(contextChar));
      if (wildcard != null)
      // assertion: wildcard found.
      {
//System.err.println("Wildcard found=<" + contextChar + "> char=<" + toConvert.charAt(inputIndex) + ">");
        boolean wildMatched = false;
        switch (wildcard.number)
        {
          case (Language.WILDCARD_NONE):
            wildMatched = true;  // always matched none!
            if (inputIndex < 0)
              break; // ie matched 0, check the rest of the context
            while (wildcardMatches(wildcard, toConvert.charAt(inputIndex)))
            {
              inputIndex--;
              if (inputIndex < 0)
                break; // ie matched at least one, check the rest of the context
            }
            break; // ie matched some, check rest of context
          case (Language.WILDCARD_ONE):
//System.err.print("Checking WILDCARD_ONE");
            if (inputIndex < 0)
            // ASSERTION: at the very left of the input text.  This can match only
            // if the wildcard indicates a space
              if (wildcardMatches(wildcard, SPACE))
                return true; // matched a SPACE wildcard against the void.
              else
                return false;  // there should be at least one
            if (!wildcardMatches(wildcard, toConvert.charAt(inputIndex)))
            {
//System.err.println("Failed to match wildcards");
              return false;  // ie match one character
            }
//System.err.println("Did match=<" + wildcard.flags + "> and <" + toConvert.charAt(inputIndex) + ">");
            // ASSERTION: one character matched
            wildMatched = true;
            inputIndex--;
            break;
          case (Language.WILDCARD_SEVERAL):
            if (inputIndex < 0)
            // ASSERTION: at the very left of the input text.  This can match only
            // if the wildcard indicates a space
              if (wildcardMatches(wildcard, SPACE))
                return true; // matched a SPACE wildcard against the void
              else
                return false; // there should be at least one
            while (wildcardMatches(wildcard, toConvert.charAt(inputIndex)))
            // keep going until run out of matching
            {
              wildMatched = true;
              inputIndex--;
              if (inputIndex < 0)
                break; // keep going until run out of input
            }
            break;
        }
        if (wildMatched)
        {
        // right, the wildcards have matched, but now we should return if there
        // is still context to go and we've run out of characters
          if ((inputIndex < 0) && (contextIndex > 0 ))
            return false;
        }
        else
        // ASSERTION: wildcard wasn't matched.
        {
          return false;
        }
      } // end Wildcard found
      else // assertion: not wildcard
      {
        if (inputIndex < 0)
          return false;
//System.err.println("context=<" + leftContext.charAt(contextIndex) + "> input=<" + toConvert.charAt(inputIndex) + ">");
        if (currentRule.leftContext.charAt(contextIndex) == toConvert.charAt(inputIndex))
        {
          // assertion: match of input char and context char
          inputIndex--;
        }
        else // assertion: failed to match input char and context char
        {
          return false;
        }
      }
    }  // end of while

    return true;  // if got this far, has matched
  }

  private boolean compareRightContext(String toConvert, int startIndex)
  {
    int contextLength = currentRule.rightContext.length();
    if (contextLength == 0)
      return true; // always match an empty right context
    int inputIndex = startIndex + currentRule.focusLength;
    int inputLength = toConvert.length();
    for (int contextIndex = 0; contextIndex < contextLength; contextIndex++)
    {
      char contextChar = currentRule.rightContext.charAt(contextIndex);
      // first, check right context character isn't a wildcard
      WildcardUnicode wildcard = (WildcardUnicode) wildcardGetter.get(new Character(contextChar));
      if (wildcard != null)
      // assertion: wildcard found.
      {
        boolean wildcardMatched = false;
        switch (wildcard.number)
        {
          case (Language.WILDCARD_NONE):
            wildcardMatched = true;
            if (inputIndex >= inputLength)
              break; // ie matched 0, check the rest of the context
            while (wildcardMatches(wildcard, toConvert.charAt(inputIndex)))
            {
              inputIndex++;
              if (inputIndex >= inputLength)
                break; // ie matched at least one, check the rest of the context
            }
            break; // ie matched some, check rest of context
          case (Language.WILDCARD_ONE):
            if (inputIndex >= inputLength)
            // ASSERTION: checking has progressed beyond end of input - only
            // permissable is wildcard indicates a SPACE character
              if (wildcardMatches(wildcard, SPACE))
                return true; // SPACE wildcard, so okay to match against outside input
              else
                return false; // there should be at least one
            if (!wildcardMatches(wildcard, toConvert.charAt(inputIndex)))
              return false;  // ie match one character
            wildcardMatched = true;
            inputIndex++;
            break;
          case (Language.WILDCARD_SEVERAL):
            if (inputIndex >= inputLength)
            // ASSERTION: checking has progressed beyond end of input - only
            // permissable is wildcard indicates a SPACE character
              if (wildcardMatches(wildcard, SPACE))
                return true; // SPACE wildcard, so okay to match against outside input
              else
                return false; // there should be at least one
            while (wildcardMatches(wildcard, toConvert.charAt(inputIndex)))
            // keep going until run out of matching
            {
              wildcardMatched = true;
              inputIndex++;
              if (inputIndex >= inputLength)
                break; // keep going until run out of input
            }
            break;
        }
        if (wildcardMatched)
        {
        // right, the wildcards have matched, but now we should return if there
        // is still context to go and we've run out of characters
          if ((inputIndex >= inputLength) && (contextIndex < (contextLength - 1)))
            return false;
        }
        else
        {
          return false; // no wildcard matched
        }
      } // end Wildcard found
      else // assertion: not wildcard
      {
        if (inputIndex >= inputLength)
          return false;
        if (currentRule.rightContext.charAt(contextIndex) == toConvert.charAt(inputIndex))
        {
          // assertion: match of input char and context char
          inputIndex++;
        }
        else // assertion: failed to match input char and context char
        {
          return false;
        }
      }

    }  // end of for
    return true;  // if got this far, has matched
  } // end of compareRightContext()

  /**
   * Allows the name of the language to be set.
   *
   * @param name   String that will become the name of the language.
   */
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * Returns the name of the language.
   *
   * @return name   The name of the language.
   */
  public String getName()
  {
    return name;
  }

  private TranslationRuleUnicode getNextRule()
  {
    return currentRule.nextRule;
  }

  /**
   * Allows the free text description of the language to be set.
   *
   * @param description   String containing the description for the language.
   */
  public void setDescription(String description)
  {
    this.description = description;
  }

  /**
   * Returns the free text description of the language.
   *
   * @return description   String containing the description for the language.
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Sets the number of Unicode characters supported by this new language.  Can
   * only be set once.
   *
   * @param numberCharacters   <CODE>int</CODE> number of characters for language.
   */
  public void setNumberCharacters(int numberCharacters) throws LanguageDefinitionException
  {
    if (this.numberCharacters != UNDEFINED)
      throw new LanguageDefinitionException("Number of characters can only be set once for " +
        "any language.");
    this.numberCharacters = numberCharacters;
    int hashtableOptimisingSize = (int) (numberCharacters * HASHTABLE_SIZE_OPTIMIZER_PRODUCT)
      + HASHTABLE_SIZE_OPTIMIZER_OFFSET;
    characterMapper = new Hashtable(hashtableOptimisingSize);
    charFlagGetter = new Hashtable(hashtableOptimisingSize);
    tRuleGetter = new Hashtable(hashtableOptimisingSize);
  }

  /**
   * Returns the number of Unicode characters supported by this language.
   *
   * @return numberCharacters   The number of Unicode characters supported.
   */
  public int getNumberCharacters()
  {
    return numberCharacters;
  }

  /**
   * Sets the number of states possible for the finite state machine of the
   * language.  Can only be set once.
   *
   * @param numberStates   The number of possible states.
   */
  public void setNumberStates(int numberStates) throws LanguageDefinitionException
  {
    if (this.numberStates != UNDEFINED)
      throw new LanguageDefinitionException("Number of states can only be set once for "+
        "any language");
    this.numberStates = numberStates;
    stateDescriptions = new String[numberStates];
    for (int i = 0; i < numberStates; i++)
      stateDescriptions[i] = "No state description provided";
  }

  /**
   * Applies a description to one of the finite state machine states.
   *
   * @param state   The state to have a description applied to it.
   * @param description   A String containing the description to be applied.
   */
  public void setStateDescription(int state, String description) throws LanguageDefinitionException
  {
    if (numberStates == UNDEFINED)
      throw new LanguageDefinitionException("Number of states has not been set.  Cannot apply descriptions until"
        + "number of states is defined.  Use setNumberStates first.");
    if ((state < 1) ||  (state > numberStates))
      throw new ArrayIndexOutOfBoundsException("Description provided for invalid state, outside the range of possible states " +
        " (1 to " + numberStates + ")");
    state--;
    stateDescriptions[state] = description;
  }

  private int getNewState()
  {
    if (currentRule.newState == 0)
      return state;
    else
      return currentRule.newState;
  }

  /**
   * Returns the language description for a state.
   *
   * @param state   The state for which the description is required.
   * @return stateDescription[state]   The description for the state.
   */
  public String getStateDescription(int state) throws LanguageDefinitionException, ArrayIndexOutOfBoundsException
  {
    if (numberStates == UNDEFINED)
      throw new LanguageDefinitionException("Number of states has not been set.  Cannot access descriptions until"
        + "number of states is defined and descriptions provided.  Use setNumberStates and setStateDescription first.");
    if ((state < 1) || (state > numberStates))
      throw new ArrayIndexOutOfBoundsException("Attempted to access state description " +
        "for state that does not exist in this language.  State requested:" + state +
        " Possible range of states: 1 to " + numberStates);
    state--;
    return stateDescriptions[state];
  }

  /**
   * Returns the language description for an input class.
   *
   * @param inputClass   The input class for which the description is required.
   * @return inputClassDescriptions[inputClass]   The description for the input
   *                                              class.
   */
  public String getInputClassDescription(int inputClass) throws LanguageDefinitionException, ArrayIndexOutOfBoundsException
  {
    if (numberInputClasses == UNDEFINED)
      throw new LanguageDefinitionException("Number of input classes has not been set.  Cannot access descriptions until"
        + "number of input classes is defined and descriptions provided.  Use setNumberInputClasses and setInputClassDescription first.");
    if ((inputClass < 1) || (inputClass > numberInputClasses))
      throw new ArrayIndexOutOfBoundsException("Attempted to access input class description " +
        "for input class that does not exist in this language.  Input class requested:" + inputClass +
        " Possible range of input classes: 1 to " + numberInputClasses);
    inputClass--;
    return inputClassDescriptions[inputClass];
  }

  /**
   * Applies a description to one of the input classes of the language.
   *
   * @param inputClass  The input class for which a description is being provided.
   * @param description The description as a String for the input class.
   */
  public void setInputClassDescription(int inputClass, String description) throws LanguageDefinitionException
  {
    if (numberInputClasses == UNDEFINED)
      throw new LanguageDefinitionException("Number of input classes has not been set.  Cannot apply descriptions until"
        + "number of input classes is defined.  Use setNumberInputClasses first.");
    if ((inputClass < 1) ||  (inputClass > numberInputClasses))
      throw new LanguageDefinitionException("Description provided for invalid input class, outside the range of possible input classes " +
        " (1 to " + numberInputClasses + ")");
    inputClass--;
    inputClassDescriptions[inputClass] = description;
  }

  /**
   * Sets the number of input classes for the language.  Can only be set once.
   *
   * @param numberInputClasses   Number of input classes for the language.
   */
  public void setNumberInputClasses(int numberInputClasses) throws LanguageDefinitionException
  {
    if (this.numberInputClasses != UNDEFINED)
      throw new LanguageDefinitionException("Number of input classes can only be set once for "+
        "any language");
    if (this.numberStates == UNDEFINED)
      throw new LanguageDefinitionException("Number of states must be defined before the "+
        "number of input classes.  Use setNumberStates.");
    this.numberInputClasses = numberInputClasses;
    inputClassDescriptions = new String[numberInputClasses];
    for (int i = 0; i < numberInputClasses; i++)
      inputClassDescriptions[i] = "No input class description provided";
    stateTable = new boolean[numberStates][numberInputClasses];
  }

  /**
   * Sets a state table entry for the language.  Both the number of states and
   * and the number of input classes must be defined already.  The state table
   * is a (rules * input classes) table of true/false values.
   *
   * @param state   State of table.
   * @param inputClass   Input class of table.
   * @param value   Results to be entered.
   */
  public void setDecisionTableEntry(int state, int inputClass, boolean value) throws LanguageDefinitionException, ArrayIndexOutOfBoundsException
  {
    if ((numberStates == UNDEFINED) || (numberInputClasses == UNDEFINED))
      throw new LanguageDefinitionException("Cannot set decision table entries until"
        + " number of states and number of input classes is defined.  Use "
        + "setNumberStates and setNumberInputClasses first.");
    if ((inputClass < 1) ||  (inputClass > numberInputClasses))
      throw new ArrayIndexOutOfBoundsException("Invalid input class, outside " +
        " the range of possible input classes " +
        " (1 to " + numberInputClasses + ")");
    if ((state < 1) || (state > numberStates))
      throw new ArrayIndexOutOfBoundsException("Invalid state, outside possible " +
        "range of states: 1 to " + numberStates);
    state--;
    inputClass--;
    stateTable[state][inputClass] = value;
  }

  /**
   * Returns the number of input classes available in the language.  A translation
   * rule may belong to any one of the input classes.
   *
   * @return numberInputClasses   The number of input classes in the language.
   */
  public int getNumberInputClasses()
  {
    return numberInputClasses;
  }

  /**
   * Returns the number of states available in this language.  Any state may be
   * selected by <CODE>setState</CODE>.  States affect the translation performed.
   *
   * @return The number of states in the language.
   */
  public int getNumberStates()
  {
    return numberStates;
  }

  /**
   * Sets the version number of a <CODE>LanguageUnicode</CODE> object.  Version
   * is not used anywhere yet, so this can be used to differentiate languages but
   * should not be relied upon to do this.
   */
  public void setVersionNumber(int versionNumber)
  {
    this.version = versionNumber;
  }

  private boolean wildcardMatches(WildcardUnicode wildcard, char toMatch)
  {
    Integer charFlagsGot = (Integer) charFlagGetter.get(new Character(toMatch));
    int charFlags = charFlagsGot.intValue();
//System.err.println("Current rule=" + this.translationRules.PrintCurrentRule());
//System.err.println("char=" + toMatch+ " charFlags=" + charFlags + " wildcard=" + wildcard.number + " wildcardFlags=" + wildcard.flags);
    if ((charFlags & wildcard.flags) != 0)
//    {
//System.err.println("Matched");
      return true;
//    }
    else
//    {
//System.err.println("No match");
      return false;
//    }
  }

  /**
   * Downloads a language file from the web.  This can be used as an alternative
   * to using the constructor and passing it the name and path to a local serialized
   * instance of <CODE>LanguageUnicode<CODE>.  Instead, the language file name
   * can be passed to this method, and the language file will be obtained by HTTP.
   * As with other <CODE>Language</CODE> implementations, the filename extension
   * should not be added.  To get language "english", simply call
   * <CODE>getLanguageUnicodeFromDisk("english");</CODE>
   *
   * @param languageName  <CODE>String</CODE> name of language file to get.
   * @return The <CODE>LanguageUnicode</CODE> received.
   */
  public static LanguageUnicode getLanguageUnicodeFromWebsite(String languageName)
    throws Exception
  {
    languageName = languageName + FILE_EXTENSION_DELIMITER + FILENAME_EXTENSION;
    URL fileURL = null;
    ObjectInputStream inObject;
    LanguageUnicode fromWeb = null;
    try
    {
      fileURL = new URL("http://www.cus.org.uk/~alasdair/braille/" + languageName );
    }
    catch (MalformedURLException e)
    {
      throw new MalformedURLException("Invalid URL reported: " + e);
    }
    try
    {
      inObject = new ObjectInputStream(fileURL.openStream());
      fromWeb = (LanguageUnicode) inObject.readObject();
      inObject.close();
    }
    catch (StreamCorruptedException e)
    {
      throw new StreamCorruptedException("Error trying to get object from network: " + e);
    }
    catch (ClassNotFoundException e)
    {
      throw new ClassNotFoundException("Error parsing input stream into class: " + e);
    }
    catch (IOException e)
    {
      throw new IOException("Error loading langauge from network: " + e);
    }
    return fromWeb;
  } // end of getLanguageUnicodeFromWebsite

  private TranslationRuleUnicode parseTranslationRule(String toProcess)
    throws LanguageDefinitionException
  {
    try
    {
  		// Get InputClass
  		int inputClass = Integer.parseInt(toProcess.substring(0,1));

      // Get the main chunk
  		String mainChunk = new String(toProcess.substring(2, toProcess.length() - 2));
      // Get NewState
  		int newState;
  		if (toProcess.charAt(toProcess.length() - 1) == '-')
  		  newState = 0;
  		else
  		  newState = Integer.parseInt(toProcess.substring((toProcess.length() - 1),
            toProcess.length()));

  		// Now process the main chunk
      int mainChunkIndex = 0;  // where we're up to in the line we're processing
      // 1 Get the left context (if any)
  		StringBuffer leftPart = new StringBuffer("");
      while (mainChunk.charAt(mainChunkIndex) != LEFT_FOCUS_DELIMITER)
      {
        if (mainChunk.charAt(mainChunkIndex) == ESCAPE_CHAR_1)
        {
          if (mainChunk.charAt(mainChunkIndex + 1) == ESCAPE_CHAR_1)
          // ASSERTION: not escape character, just \\ character
          {
            mainChunkIndex += 2; // skip over the \\ char
            leftPart.append('\\');
          }
          else
          // ASSERTION: should be a escape character
          {
            if (mainChunk.charAt(mainChunkIndex + 1) != ESCAPE_CHAR_2)
            // ASSERTION: neither backslash-backslash or backslash-u, so problem
              throw new LanguageDefinitionException("Found backslash, indicating" +
               " a possible escape character, but followed neither by u for" +
               " an escape character nor another backslash for a simple " +
               "backslash character.  Rule being processed = " + mainChunk);
            // ASSERTION: got an escape character
            leftPart.append((char) Integer.parseInt(mainChunk.substring(mainChunkIndex + 2, mainChunkIndex + 6), 16));
            mainChunkIndex += 6;
          }
        }
        else
        // ASSERTION: not an escape character
        {
          leftPart.append(mainChunk.charAt(mainChunkIndex));
          mainChunkIndex++;
        }
      }
      // assertion: mainChunkIndexindicates delimiter
      mainChunkIndex++;
      // assertion: mainChunkIndexindicates character past delimiter
      // 2 Get the focus
  		StringBuffer middlePart = new StringBuffer("");
      // NOTE: Focus always contains at least one character, even '['.
      middlePart.append(mainChunk.charAt(mainChunkIndex));
      mainChunkIndex++;
      while (mainChunk.charAt(mainChunkIndex) != RIGHT_FOCUS_DELIMITER)
      {
        if (mainChunk.charAt(mainChunkIndex) == ESCAPE_CHAR_1)
        {
          if (mainChunk.charAt(mainChunkIndex + 1) == ESCAPE_CHAR_1)
          // ASSERTION: not escape character, just \\ character
          {
            mainChunkIndex += 2; // skip over the \\ char
            middlePart.append('\\');
          }
          else
          // ASSERTION: should be a escape character
          {
            if (mainChunk.charAt(mainChunkIndex + 1) != ESCAPE_CHAR_2)
            // ASSERTION: neither backslash-backslash or backslash-u, so problem
              throw new LanguageDefinitionException("Found backslash, indicating" +
               " a possible escape character, but followed neither by u for" +
               " an escape character nor another backslash for a simple " +
               "backslash character.  Rule being processed = " + mainChunk);
            // ASSERTION: got an escape character
            middlePart.append((char) Integer.parseInt(mainChunk.substring(mainChunkIndex + 2, mainChunkIndex + 6), 16));
            mainChunkIndex += 6;
          }
        }
        else
        // ASSERTION: not an escape character
        {
          middlePart.append(mainChunk.charAt(mainChunkIndex));
          mainChunkIndex++;
        }
      }
      // assertion: mainChunkIndexindicates delimiter
      mainChunkIndex++;
      // assertion: mainChunkIndexindicates character past delimiter
      // 3 Get the right context
  		StringBuffer rightPart = new StringBuffer("");
      while (mainChunk.charAt(mainChunkIndex) != RULE_OUTPUT_DELIMITER)
      {
        if (mainChunk.charAt(mainChunkIndex) == ESCAPE_CHAR_1)
        {
          if (mainChunk.charAt(mainChunkIndex + 1) == ESCAPE_CHAR_1)
          // ASSERTION: not escape character, just \\ character
          {
            mainChunkIndex += 2; // skip over the \\ char
            rightPart.append('\\');
          }
          else
          // ASSERTION: should be a escape character
          {
            if (mainChunk.charAt(mainChunkIndex + 1) != ESCAPE_CHAR_2)
            // ASSERTION: neither backslash-backslash or backslash-u, so problem
              throw new LanguageDefinitionException("Found backslash, indicating" +
               " a possible escape character, but followed neither by u for" +
               " an escape character nor another backslash for a simple " +
               "backslash character.  Rule being processed = " + mainChunk);
            // ASSERTION: got an escape character
            rightPart.append((char) Integer.parseInt(mainChunk.substring(mainChunkIndex + 2, mainChunkIndex + 6), 16));
            mainChunkIndex += 6;
          }
        }
        else
        // ASSERTION: not an escape character
        {
          rightPart.append(mainChunk.charAt(mainChunkIndex));
          mainChunkIndex++;
        }
      }
      // assertion: counter indicates delimiter
      mainChunkIndex++;
      // assertion: counter indicates character past delimiter
      // 4 Get the output
  		StringBuffer lastPart = new StringBuffer("");
  		lastPart.append(mainChunk.substring(mainChunkIndex, mainChunk.length()));

  		// Build a rule from the results
  	  TranslationRuleUnicode newRule = new TranslationRuleUnicode(leftPart,
          rightPart, middlePart, inputClass, newState, lastPart, null);
      return newRule;
    }
    catch (Exception e)
    {
      throw new LanguageDefinitionException("Error processing translation" +
        " rule - check correct format of rule.  Error reported:" + e);
    }
  }

  /**
   * Writes the current language to disk as a serialized object.  The filename
   * extension should not be provided - the correct extension will be appended for
   * this implementation of <CODE>Language</CODE>
   *
   * @param   filename  <CODE>String</CODE> showing full path and filename for object as file.
   */
  public void writeLanguageUnicodeToDisk(String filename) throws IOException
  {
    filename += FILE_EXTENSION_DELIMITER;
    filename += FILENAME_EXTENSION;
    ObjectOutputStream outFile;

    // Try to open write file and save object
    try
    {
      outFile = new ObjectOutputStream(new FileOutputStream(filename));
      outFile.writeObject(this);
      outFile.flush();
      outFile.close();
    }
    catch (IOException e)
    {
      throw new IOException("Unable to create new LanguageUnicode language " +
        "file: " + filename);
    }
//System.err.println("Yep, LanguageUnicode wrote " + filename + " to disk");
  }




  /*
    TranslationRule class - object holding one rule of the translation table
  */

  private class TranslationRuleUnicode implements Serializable
  {
    private String leftContext;
    private String rightContext;
    private String focus;
    private int focusLength;
    private int inputClass;
    private int newState;
    private String output;
    private TranslationRuleUnicode nextRule;
    private boolean lastInCategory;
    private char firstCharOfFocus;

    private TranslationRuleUnicode(String newLeftContext,
                String newRightContext,
                String newFocus,
                int newInputClass,
                int newNewState,
                String newOutput,
                TranslationRuleUnicode newNextRule)
    // Constructor for a full rule
    {
  	  leftContext = newLeftContext;
  	  rightContext = newRightContext;
  	  focus = newFocus;
      focusLength = focus.length();
  	  inputClass = newInputClass;
  	  newState = newNewState;
  	  output = newOutput;
      nextRule = newNextRule;
      lastInCategory = false;
      firstCharOfFocus = focus.charAt(0);
    }

    private TranslationRuleUnicode(TranslationRuleUnicode oldRule)
    {
  	  leftContext = oldRule.leftContext;
  	  rightContext = oldRule.rightContext;
  	  focus = oldRule.focus;
      focusLength = oldRule.focusLength;
  	  inputClass = oldRule.inputClass;
  	  newState = oldRule.newState;
  	  output = oldRule.output;
      nextRule = oldRule.nextRule;
      lastInCategory = oldRule.lastInCategory;
      firstCharOfFocus = focus.charAt(0);
    }

    private TranslationRuleUnicode(
            StringBuffer left,
            StringBuffer right,
            StringBuffer focus,
            int inputClass,
            int newState,
            StringBuffer output,
            TranslationRuleUnicode nextRule
            )
    {
  	  leftContext = left.toString();
  	  rightContext = right.toString();
  	  this.focus = focus.toString();
      focusLength = this.focus.length();
  	  this.inputClass = inputClass;
  	  this.newState = newState;
  	  this.output = output.toString();
      this.nextRule = nextRule;
      this.lastInCategory = false;
      this.firstCharOfFocus = focus.charAt(0);
    }
} // end of TranslationRule class

  /*
    Wildcard

    Implements a single wildcard

    char wildcard -  the character in the translation rules for this wildcard
    int number - whether the wildcard matches 1, 1+ or 0+ characters
    int flags - the character properties this wildcard should match

  */

  private class WildcardUnicode implements Serializable
  {
    int number;
    int flags;

    public WildcardUnicode(int number, int flags)
    {
      this.number = number;
      this.flags = flags;
    }
  } // end of Wildcard class

} // end of LanguageUnicode class



