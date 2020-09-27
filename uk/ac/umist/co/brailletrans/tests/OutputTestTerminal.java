package uk.ac.umist.co.brailletrans.tests;
import uk.ac.umist.co.brailletrans.*;
import uk.ac.umist.co.brailletrans.utils.*;
import java.io.*;

/*
 * OutputTestTerminal
 * Shows the output of the BrailleTrans classes in the terminal.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 */

public class OutputTestTerminal extends Translator
{

  public static void main(String[] args) throws Exception
  {
    String inFilename = "";
    String languageToUse = "";
    Language256 language256 = null;
    LanguageUnicode languageUnicode = null;
    LanguageInteger languageInteger = null;
    String result256 = null;
    String resultInteger = null;
    String resultUnicode = null;

    int state = 1;

    if (args.length != 3)
    {
      System.out.println("OutputTestOld");
      System.out.println("USAGE  java OutputTestTerminal <file> <language> <state>");
      System.exit(SUCCESS);
    }

    inFilename = new String(args[0]);
    languageToUse = new String(args[1]);
    state = Integer.parseInt(args[2]);
    args = null;

    language256 = new Language256(languageToUse);
    language256.setState(state);
    languageUnicode = new LanguageUnicode(languageToUse);
    languageUnicode.setState(state);
    languageInteger = new LanguageInteger(languageToUse);
    languageInteger.setState(state);
    // Get the input to translate
    result256 = turnIntoString(language256.translate(readIntArrayFromDisk(inFilename)));
    resultInteger = turnIntoString(languageInteger.translate(readIntArrayFromDisk(inFilename)));
    resultUnicode = languageUnicode.translate(readStringFromDisk(inFilename));

    System.out.println("Translation Results" + "\n" + "Language256"  + "\n"
      + result256
      + "\n\n" + "LanguageInteger" + "\n" + resultInteger  + "\n\n" +
      "LanguageUnicode" + "\n" + resultUnicode + "\n\n");
    System.exit(SUCCESS);
  }
}
