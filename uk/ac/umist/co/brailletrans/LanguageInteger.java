package uk.ac.umist.co.brailletrans;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * <CODE>Language</CODE> class based on the C implementation BrailleTrans by Paul
 * Blenkhorn, UMIST, Manchester, UK.  This runs the fastest of all the
 * <CODE>Language</CODE> implementations developed by Alasdair King.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class LanguageInteger implements Language
{
  /**
   * The maximum ratio of input to output arrays after translation - 1:MAX_COMPRESS.  Any greater will
   * cause the language to throw an ArrayIndexOutOfBoundsException.  The input text may be padded with
   * spaces if this occurs.
   */
  static final double MAX_COMPRESS = 2;
  private static final int STR_SIZE             =32000;      /* number of characters in strings */
  private static final int TAB_SIZE             =25000;      /* size of tables */
  private static final int NUL                    ='\0';
  private static final int TRUE                    = 1;      /* logical constants */
  private static final int FALSE                    =0;
  private static final int NO_MOVE                  =0;
  private static final int NOT_DEFINED              =0;
  private static final int NUMBER_OF_CHARACTERS   =256;
  private static final int VERSION_FAULT           =16;      /* error conditions */
  private static final int CHAR_FAULT              =17;
  private static final int WILD_FAULT              =18;
  private static final int DECISION_FAULT          =19;
  private static final int SIZE_FAULT              =20;
  private static final int WILD_MATCH               =1;
  private static final int WILD_BIT_PATTERN         =2;
  private static final int WILD_DATA                =3;
  private static final int EOF = -1;
  private static final int INTERNAL_ERROR = 3;

  private int[] table = new int[TAB_SIZE];     /* area to hold exceptions table */
  private int current_state = 1;               /* the current state */
  private int[] wild_tab = new int[200];       /* wildcard table */
  private int[] decision_table = new int[200]; /* state table */
  private int no_wilds;                        /* number of wildcards */
  private int no_input_classes;                /* number of input classes */
  private int no_states;                       /* number of states */
  private ChInfo[] ch_info = new ChInfo[NUMBER_OF_CHARACTERS]; // ADT for a character rule
  private int InstallOK;
  private int defaultState;
  private int looking;                       /* pointer for search through tables */

  /**
   * Indicates language rules tables (data files) on disk, suitable for
   * loading into the <CODE>LanguageInteger</CODE> class and performing translation.
   */
  public static final String FILENAME_EXTENSION = "dat";
  /**
   * Indicates the text-only human-editable language rules tables format
   * on disk.  These must be converted into the machine format by
   * <CODE>MakeLegacy</CODE>
   */
  public static final String DATAFILE_EXTENSION = "con";

 /**
  * Loads a LanguageInteger object from disk.
  *
  * @param filename Full path and name of language rules table as file to load,
  * according to local filesystem.  Do not append the filename extension, for
  * example ".DAT", just use the full path except this, for example
  * "C:\trans\english" for the english.dat language rules table.
  */
  public LanguageInteger(String filename)
    throws IOException, FileNotFoundException, LanguageLegacyDatafileFormatException
  {
    BufferedInputStream inFile = null;
  		// Used for File input
    defaultState = current_state = 1;
      // default state unless told otherwise
    filename = filename + FILE_EXTENSION_DELIMITER + FILENAME_EXTENSION;

    // 1 Open file "filename" to read
    try
    {
      inFile = new BufferedInputStream(new FileInputStream(filename));
    }
  	catch (FileNotFoundException e)
  	{ throw new FileNotFoundException("Unable to find language file " + filename); }

    // 2 Read in the data
    read_version_number(inFile);
    read_character_data(inFile);
    read_wildcards(inFile);
    read_decision(inFile);
    read_main_tables(inFile);

    // Close the language file
    try
    {
      inFile.close();
    }
    catch (IOException e)
    {
      throw new IOException("Unable to close language file after reading " +
       filename);
    }
  }


  /* Function: convert
     Translate the text in the buffer input_dat.
     Parameters:
          convert_to is set for how may chars, or character type to convert to
          input_dat is the input data
          output_dat is the output text
     Returns:
          number of characters converted
  */
  private int convert(int[] input_dat, Output output_dat)
  {
    int up_to = 0;                /* position in input buffer */
    int step;                     /* amount to step along input buffer */
    do
    {
       /* check the table, return how far to move along input buffer
       if no match then move 1 char along the input buffer. */

      if ((step = find_match(up_to,input_dat, output_dat)) != FALSE)
        up_to += step;
      else { /* output input character and change state to default */
  	    add_to_output(input_dat[up_to], output_dat);
  	    current_state = defaultState;
//System.err.println("Failed to match at " + up_to);
  	    up_to++;
      }
    } while (up_to < input_dat.length);
    return up_to;
  }


  /* Function: initialise
     Initialise for each group of characters to convert.
     Parameters:
          none
     Returns:
          void
  */
  private void initialise()
  {
    current_state = defaultState;
  }

  /* Function: add_to_output
     output character, and check for things like capitalisation later?
     Parameters:
          chr is the character to add
          output_buffer is the buffer to add it to
     Returns:
          nothing
  */
  private void add_to_output(int chr, Output output_buffer)
  // added outUpTo to track where on the output array we've got to
  {
    if (output_buffer.upTo < output_buffer.output.length)
      output_buffer.output[output_buffer.upTo++] = chr;
  }

/********************************* find match ******************************/


/* Function: find_match
   Try to find a match in table for current position in input buffer from
   tables.
   Parameters:
        up_to is position in input buffer
        input_dat is the input data
        output_dat is the output text
   Returns:
        number of characters converted - NO_MOVE (0) if none
*/
  private int find_match(int up_to, int[] input_dat, Output output_dat)
  {
    int move_no;           /* how far to move along input buffer */
    int this_table_entry;  /* pointer to input class of current table entry */

    /* quick hash into the contraction table from first character in buffer.
        *looking == 0 if no entry found.
    */
    this_table_entry = looking = ch_info[input_dat[up_to]].hash;


      /* if hash character then check rules */
    if (table[looking] == NOT_DEFINED)
      return(NO_MOVE);
    else
    {
      do
      {  /* go through the table entries */
        while (table[looking++] != LEFT_FOCUS_DELIMITER)
          ; /* get to character after '[' */

        if (table[looking] != input_dat[up_to]) /* run out of table entries for this letter? */
          return(NO_MOVE);

        if ((move_no = words_match(up_to,input_dat)) != FALSE)
          if (check_state(table[(this_table_entry-1)]) != FALSE)
            if (right_context(up_to+move_no,input_dat) != FALSE)
              if (left_context(up_to,input_dat) != FALSE)
              {
                match_found(output_dat);
                return(move_no);
              }

      /* go to next entry in the table */
        looking = (this_table_entry += table[this_table_entry]);
      } while (true);
    } /* *looking */
  }


/* Function: check_state
   check the input class against the current state in the state table
   Parameters:
        chr is the input class
   Returns:
        whether input class is acceptable for match
*/
  private int check_state(int inp_class)
  {
    int result = 0;
    try
    {
      result = decision_table[((no_input_classes* (current_state-1) ) + inp_class - 1)];
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      System.err.println("Out of bounds error.  \nno_input_classes = " + no_input_classes
        + "\ncurrent_state = " + current_state
        + "\ninp_class = " + inp_class + " Error:" + e);
      System.exit(INTERNAL_ERROR);
    }
    return result;
  }


/* Function: left_context
   Check to see if the left context of the match string is valid.
   Parameters:
        up_to is end of match string in input data
        input_dat is the input text
   Returns:
        whether left context is satisfied
*/
  private int left_context(int up_to, int[] input_dat)
  {
      while (table[looking--] != LEFT_FOCUS_DELIMITER)
         ;
      return (wild_match(-1, --up_to, input_dat));
  }


/* Function: right_context
   Check to see if the right context of the match string is valid.
   Parameters:
        up_to is end of match string in input data
        input_dat is the input text
   Returns:
        whether right context is satisfied
*/
  private int right_context(int up_to, int[] input_dat)
  {
      looking++;
      return (wild_match(+1, up_to, input_dat));
  }


/* Function: wild_match
   Used by left_context and right_context to check contexts.
   It uses wild cards and other characters to validate context
   Parameters:
        step is +ve or -ve to tell function which way to increment
        up_to is end of match string in input data
        input_dat is the input text
   Returns:
        whether match is successful
*/
  private int wild_match(int step, int up_to, int[] input_dat)
  {
//System.err.println("up_to=" + up_to + " table[looking]=" + (char) table[looking] + " step=" + step);

    int i;
    int bits;
    while (table[looking] >= ' ')
    { /* work through the entries */
      if (table[looking] == RULE_OUTPUT_DELIMITER && step == 1)
        break;
      if ((ch_info[table[looking]].data & WILDCARD_FLAG) != FALSE)
      { // got wildcard in rule
        for (i = 0; i < no_wilds; i++)
          if (table[looking] == wild_tab[i*WILD_DATA])
          { // identified which wild card
            bits = wild_tab[i*WILD_DATA+WILD_BIT_PATTERN];
            switch (wild_tab[i*WILD_DATA+WILD_MATCH])
            {
              case WILDCARD_ONE :
                if (up_to < 0)
                // ASSERTION: checking has progressed beyond end of input - only
                // permissable is wildcard indicates a SPACE character
                  if ((SPACE_FLAG & bits) == 0)
                    return(FALSE); // not a space character, not permitted outside
                  else
                    return(TRUE); // SPACE wildcard, so okay to match against outside input
                if (up_to >= input_dat.length)
                // ASSERTION: checking has progressed beyond end of input - only
                // permissable is wildcard indicates a SPACE character
                  if ((SPACE_FLAG & bits) == 0)
                    return(FALSE); // not a space character, not permitted outside
                  else
                    return(TRUE); // SPACE wildcard, so okay to match against outside input
                if ( (ch_info[input_dat[up_to]].data & bits) == 0) //!= bits
                  return(FALSE);
                up_to += step;
                break;
              case WILDCARD_SEVERAL :
                if (up_to < 0)
                // ASSERTION: checking has progressed beyond end of input - only
                // permissable is wildcard indicates a SPACE character
                  if ((SPACE_FLAG & bits) == 0)
                    return(FALSE); // not a space character, not permitted outside
                  else
                    return(TRUE); // SPACE wildcard, so okay to match against outside input
                if (up_to >= input_dat.length)
                // ASSERTION: checking has progressed beyond end of input - only
                // permissable is wildcard indicates a SPACE character
                  if ((SPACE_FLAG & bits) == 0)
                    return(FALSE); // not a space character, not permitted outside
                  else
                    return(TRUE); // SPACE wildcard, so okay to match against outside input
                if ( (ch_info[input_dat[up_to]].data & bits) == 0) // != bits )
                  return(FALSE);
                do
                {
                  up_to += step;
                  if (up_to < 0)
                    break;
                  if (up_to >= input_dat.length)
                    break;
                } while ( (ch_info[input_dat[up_to]].data & bits) > 0);// == bits);
                break;
              case WILDCARD_NONE :
                if (up_to < 0)
                  break;
                if (up_to >= input_dat.length)
                  break;
                while ( (ch_info[input_dat[up_to]].data & bits) > 0)//== bits)
                {
                  up_to += step;
                  if (up_to < 0)
                    break;
                  if (up_to >= input_dat.length)
                    break;
                }
                break;
            } // end of switch
//            break;    Don't think this is required
          } // end of (if) found wild card
      } // end of got wildcard
      else
      { /* not wildcard */
        if (up_to < 0)
          return(FALSE);
        if (up_to >= input_dat.length)
          return (FALSE);

        if (table[looking] != input_dat[up_to])
          return(FALSE);
        up_to += step;
      }
//      up_to += step;  // can't go here!  May not happen (ie match 0+ wcard)
  	  looking += step;
    } /* end of while work through the entries */
    return (TRUE);
  }


/* Function: words_match
   compare input buffer with focus of entry in table [in brackets]
   Parameters:
        up_to is end of match string in input data
        input_txt is the input text
   Returns:
        whether words do match
*/
  private int words_match(int up_to, int[] input_txt)
  {
    int start = up_to;
    do
    {
      if (up_to == input_txt.length)
        return(FALSE);
      if (table[looking++] != input_txt[up_to++])
        return(FALSE);
//      up_to++;  // did up_to++ in input_txt[] two lines up instead, for speed
//System.out.print("\tup=" + up_to);
    } while (table[looking] != ']');
/*
System.err.print("<");
for (int i = start; i < up_to; i++)
System.err.print((char) input_txt[i]);
System.err.print(">");
System.err.println();
*/

    return(up_to-start);
  }


/* Function: match_found
   move the transcribed text (right hand side of the rule) to the output buffer.
   update the state of the system.
   Parameters:
        output_dat is the output buffer
   Returns:
        void
*/
  private void match_found(Output output_dat)
  {
  	int temp;
    while (table[looking++] != RULE_OUTPUT_DELIMITER)     /* get to rhs of rule */
      ;
    while (table[looking] != RULE_CONTENT_DELIMITER)              /* output info */
      add_to_output(table[looking++], output_dat);

    // current_state = *(looking+1);  /* update state */
    if (table[(looking+1)] > 0)  /* update state if a change is made */
  	  current_state = table[(looking+1)];
  }
/****************************** end find match ******************************/

/********************************* read tables ******************************/




/* Function: table_fault
   fault occured in reading table.  Report fault and exit.
   Parameters:
        fault_no the fault to be reported
   Returns:
        whether left context is satisfied
*//*
  private void table_fault(int fault_no)
  {
  	InstallOK = fault_no;
  } */


/* Function: read_version_number
   read and verify version number from program data file
   Parameters:
        fp handle for file
   Returns:
        void
*/
  private void read_version_number(BufferedInputStream inFile)
    throws IOException, LanguageLegacyDatafileFormatException
  {
    int version_number;

    try
    {
      if (inFile.read() != 17)
        throw new LanguageLegacyDatafileFormatException("Language file format" +
        " error, code " + VERSION_FAULT);
      if (inFile.read() != 12)
        throw new LanguageLegacyDatafileFormatException("Language file format" +
        " error, code " + VERSION_FAULT);
      if (inFile.read() != 8)
        throw new LanguageLegacyDatafileFormatException("Language file format" +
        " error, code " + VERSION_FAULT);
      version_number = inFile.read();
    }
    catch (IOException e)
    {
      throw new IOException("Error reading version number: " + e);
    }
  }


  /* Function: read_character_data
     read and verify data for 256 characters.
     Parameters:
          inFile handle for file
     Returns:
          void
  */
  private void read_character_data(BufferedInputStream inFile)
    throws IOException, LanguageLegacyDatafileFormatException
  {
    int chr;
    for (int i = 0; i < NUMBER_OF_CHARACTERS; i++)
    {
      try
      {
        ch_info[i] = new ChInfo();
        if ((chr = inFile.read()) == EOF)
          throw new LanguageLegacyDatafileFormatException("Language file format" +
          " error, code " + CHAR_FAULT);
        else
          ch_info[i].input_trans = chr;
        if ((chr = inFile.read()) == EOF)
          throw new LanguageLegacyDatafileFormatException("Language file format" +
          " error, code " + CHAR_FAULT);
        else
          ch_info[i].to_up = chr;
        if ((chr = inFile.read()) == EOF)
          throw new LanguageLegacyDatafileFormatException("Language file format" +
          " error, code " + CHAR_FAULT);
        else
          ch_info[i].data = chr;
      }
      catch (IOException e)
      {
        throw new IOException("Error reading character table: " + e);
      }
    }
}


  /* Function: read_wildcards
     read and verify number of wildcards from program data file.
     Allocate memory and read and verify wildcard data.
     Parameters:
          fp handle for file
     Returns:
          void
  */
  private void read_wildcards(BufferedInputStream inFile)
    throws IOException, LanguageLegacyDatafileFormatException
  {
    int i;
    int chr;

    try
    {
      if ( (no_wilds = inFile.read()) == EOF)
        throw new LanguageLegacyDatafileFormatException("Language file format" +
        " error, code " + WILD_FAULT);
      //   wild_tab = (char *) malloc(no_wilds*WILD_DATA);
      for (i = 0; i < no_wilds*WILD_DATA; i++)
      {
        if ( (chr = inFile.read()) == EOF)
          throw new LanguageLegacyDatafileFormatException("Language file format" +
          " error, code " + WILD_FAULT);
        wild_tab[i] = chr;
      }
    }
    catch (IOException e)
    {
      throw new IOException("Error reading wildcard table: " + e);
    }
  }

  /* Function: read_decision table
     read and verify number of states and input classes from program data file.
     Allocate memory and read and verify decision table data.
     Parameters:
          fp handle for file
     Returns:
          void
  */
  private void read_decision(BufferedInputStream inFile)
    throws IOException, LanguageLegacyDatafileFormatException
  {
    int i, j, chr;

    try
    {
    if ( (no_states = inFile.read()) == EOF)
      throw new LanguageLegacyDatafileFormatException("Language file format" +
      " error, code " + DECISION_FAULT);
    if ( (no_input_classes = inFile.read()) == EOF)
      throw new LanguageLegacyDatafileFormatException("Language file format" +
      " error, code " + DECISION_FAULT);
    //   decision_table = (char *) malloc(no_input_classes*no_states);
    for (i = 0; i < no_states; i++)
    {
      for (j = 0; j < no_input_classes; j++)
      {
        if ( (chr = inFile.read()) == EOF)
          throw new LanguageLegacyDatafileFormatException("Language file format" +
          " error, code " + DECISION_FAULT);
          decision_table[i*no_input_classes+j] = chr;
      }
    }
    }
    catch (IOException e)
    {
      throw new IOException("Error reading decision table: " + e);
    }
  }


  /* Function: read_main tables
     read into table[] and verify exceptions from program data file.
     Build hash into table[] based on first character of focus [in brackets].
     Parameters:
          fp handle for file
     Returns:
          void
  */
  private void read_main_tables(BufferedInputStream inFile)
    throws IOException, LanguageLegacyDatafileFormatException
  {
    int i;
    int chr;
    int start;

    for (i = 0; i < 256; i++)             /* initialise hash table */
          ch_info[i].hash = NOT_DEFINED;

    i = 0;
    table[i++] = 0;       /* at start of table have dummy NUL and new state */
    table[i++] = 1;       /* to ensure that algorithms work */

    try
    {
      while (table[i - 1] != EOF)
      {
        table[i++] = inFile.read();                         /* input class */
        start = i;
        table[i++] = inFile.read();                   /* length of entry */
        if (table[i - 1] == EOF)
          break;
        while ( (table[i++] = inFile.read()) != LEFT_FOCUS_DELIMITER ) /* skip over left context and '[' */
          ;
        table[i++] = chr = inFile.read();             /* first character of focus */
        if (ch_info[chr].hash == NOT_DEFINED)
        {
          ch_info[chr].hash = start;
        }

        /* read to the end of the line */
        while ((table[i++] = inFile.read()) != FALSE)
          if (table [i - 1] == EOF)
            break;
        table[i++] = inFile.read();                   /* new state */
        if (i >= TAB_SIZE)
          throw new LanguageLegacyDatafileFormatException("Language file format" +
            " error, code " + SIZE_FAULT);
      }
    }
    catch (IOException e)
    {
      throw new IOException("Error reading translation rules: " + e);
    }
  } // end of function read_main_tables


  public int[] translate(int[] input_txt)
  {
    Output output_txt = new Output((int) (input_txt.length * MAX_COMPRESS));
//    int[] output_txt = new int[(int) (input_txt.length * MAX_COMPRESS)]; // for output

    for (int i = 0; i < input_txt.length; i++)
      input_txt[i] = ch_info[input_txt[i]].input_trans;
    convert(input_txt, output_txt);
    int[] toReturn = new int[output_txt.upTo];
    System.arraycopy(output_txt.output, 0, toReturn, 0, output_txt.upTo);
    return toReturn;
  }

  public String translate(String toConvert)
  {
     int toConvertL = toConvert.length();
     int[] inputAsArray = new int[toConvertL];
     for (int i = 0; i < toConvertL; i++)
       inputAsArray[i] = (int) toConvert.charAt(i);
     int[] converted = translate(inputAsArray);
     StringBuffer output = null;
     for (int i = 0; i < converted.length; i++)
       output = output.append((char) converted[i]);
     String outputAsString = new String(output);
     return outputAsString;
  }

  public int getState()
  {
    return current_state;
  }

  public int getPermittedStates()
  {
    return no_states;
  }

  public boolean setState(int newState)
  {
    if (newState < 1)
    {
      System.err.println("Failed to set state: new state requested < 1, was in fact " + newState);
      return false;
    }
    if (newState > no_states)
    {
      System.err.println("Failed to set state: new state > no. states, " + newState + " v " + no_states);
      return false;
    }
    defaultState = newState;
    current_state = newState;
    return true;
  }
/*
  public int getHashValue(int index)
  {
    return ch_info[index].hash;
  }
*/

  // ADTs for Language256Array
  private class ChInfo
  {
    private int input_trans;
    private int to_up;
    private int data;
    private int hash;

    ChInfo()
    {
    }

    ChInfo (int input_trans, int to_up, int data, int hash)
    {
      this.input_trans = input_trans;
      this.to_up = to_up;
      this.data = data;
      this.hash = hash;
    }
  }

  private class Output
  {
    private int[] output;
    private int upTo;

    private Output(int size)
    {
      upTo = 0;
      output = new int[size];
    }
  }


}





