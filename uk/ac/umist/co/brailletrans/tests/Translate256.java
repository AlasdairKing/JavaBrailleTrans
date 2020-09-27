package uk.ac.umist.co.brailletrans.tests;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import uk.ac.umist.co.brailletrans.*;
import uk.ac.umist.co.brailletrans.tests.Stopwatch;

/*
 * Translate256
 * Uses the Language256 class to perform translation.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 */

public class Translate256 extends Translator
{
  static final String RESULT_SUFFIX = ".out.txt";

  public static void main(String[] args)
  {
    String inFilename = "";
    String languageToUse = "";
    int[] toConvert = null;
    Language256 language = null;
    int state = 1;
    Stopwatch toLoad = new Stopwatch();
    Stopwatch toRun = new Stopwatch();


    if (args.length != 3)
    {
      System.out.println("Translate256");
      System.out.println("USAGE  java Translate256 <file> <language> <state>");
      System.exit(SUCCESS);
    }

    inFilename = new String(args[0]);
    languageToUse = new String(args[1]);
    state = Integer.parseInt(args[2]);
    args = null;

    try
    {
      toLoad.start();
      language = new Language256(languageToUse);
      toLoad.stop();
    }
    catch (IOException e)
    {
      System.err.println(e);
      System.exit(DISK_ERROR);
    }
    language.setState(state);

    // Get the input to translate
    try {
      toConvert = Translator.readIntArrayFromDisk(inFilename);
    } catch (Exception e) { System.err.println(e); System.exit(DISK_ERROR); }

    toRun.start();
    int[] output = language.translate(toConvert);
    toRun.stop();

    System.out.println("Result:" +Translator.turnIntoString(output));
    System.out.println("Time to load: " + toLoad.getTime());
    System.out.println("Time to run: " + toRun.getTime());
  }
}
