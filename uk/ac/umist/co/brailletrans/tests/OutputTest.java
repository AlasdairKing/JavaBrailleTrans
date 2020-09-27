package uk.ac.umist.co.brailletrans.tests;
import uk.ac.umist.co.brailletrans.*;
import uk.ac.umist.co.brailletrans.utils.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/*
 * OutputTest
 * Shows the output of the BrailleTrans classes in a Java window.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 */

public class OutputTest extends Translator
{
  static final String RESULT_SUFFIX = ".out.txt";

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
      System.out.println("OutputTest");
      System.out.println("USAGE  java OutputTest <file> <language> <state>");
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

    String toDisplay = new String
    ("Translation Results" + "\n" + "Language256"  + "\n" + result256
      + "\n\n" + "LanguageInteger" + "\n" + resultInteger  + "\n\n" +
      "LanguageUnicode" + "\n" + resultUnicode + "\n\n");

    JTextArea display = new JTextArea(toDisplay, 20, 50);
    JOptionPane.showMessageDialog(null, new JScrollPane(display), "Translation Output",
      JOptionPane.NO_OPTION);

    System.exit(SUCCESS);
  }
}