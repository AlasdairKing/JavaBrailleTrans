package uk.ac.umist.co.brailletrans.tests;
import uk.ac.umist.co.brailletrans.*;
import uk.ac.umist.co.brailletrans.tests.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/*
 * TranslateUni
 * Uses the LanguageUnicode class to perform translation.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 */

public class TranslateUni extends Translator
{

  public static void main(String[] args)
  {
    String inFilename = "";
    String languageToUse = "";
    String toConvert = null;
    LanguageUnicode language = null;
    int state = 1;
    Stopwatch toLoad = new Stopwatch();
    Stopwatch toRun = new Stopwatch();


    if (args.length != 3)
    {
      System.out.println("TranslateUni");
      System.out.println("USAGE  java TranslateUni <file> <language> <state>");
      System.exit(SUCCESS);
    }

    inFilename = new String(args[0]);
    languageToUse = new String(args[1]);
    state = Integer.parseInt(args[2]);
    args = null;

    try
    {
      toLoad.start();
      language = new LanguageUnicode(languageToUse);
      toLoad.stop();
    }
    catch (Exception e)
    {
      System.err.println(e);
      System.exit(DISK_ERROR);
    }
    language.setState(state);

    // Get the input to translate
    try {
      toConvert = Translator.readStringFromDisk(inFilename);
    } catch (Exception e) { System.err.println(e); System.exit(DISK_ERROR); }

    toRun.start();
    String output = language.translate(toConvert);
    toRun.stop();

    System.out.println("Result: " + output);
    JOptionPane.showMessageDialog(null, output, "Translation Output",
      JOptionPane.NO_OPTION);

    System.out.println("Time to load: " + toLoad.getTime());
    System.out.println("Time to run: " + toRun.getTime());
    System.exit(SUCCESS);
  }
}
