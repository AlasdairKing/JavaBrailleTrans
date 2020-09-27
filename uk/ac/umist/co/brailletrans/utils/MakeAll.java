package uk.ac.umist.co.brailletrans.utils;
import java.io.*;
import uk.ac.umist.co.brailletrans.*;

/**
 * Creates machine-format language files from data files for all types, by
 * calling in turn the MakeLanguageUnicode and MakeLegacy classes.  Assumes,
 * therefore, the existence of both a CON and UCN file for the provided language
 * filename.
 * 
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class MakeAll extends Maker
{
  private MakeAll()
  {
  }

  /**
   * Provides command-line functionality for language process
   *
   * @param args  Takes the <CODE>String args[0]</CODE> as the filename (full
   * path and filename) of the language files to convert.
   */
  public static void main(String[] args)
  {
    String inputFilename = "";
      // the file to convert and what to convert it to
    File oldFile = null;
    // CHECK FOR ARGUMENTS
    if (args.length != 1)
    {
      System.out.println("Make USAGE: java brailleTrans.Make <language .con file>");
      System.exit(SUCCESS);
    }
    inputFilename = args[0];

    // Delete any existing files
    // get the .dat
    oldFile = new File(inputFilename + Language.FILE_EXTENSION_DELIMITER +
      Language.FILE_EXTENSION_DELIMITER + Language.FILENAME_EXTENSION);
    if (oldFile.exists())
      oldFile.delete();
    // get the .ulf
    oldFile = new File(inputFilename + Language.FILE_EXTENSION_DELIMITER +
      LanguageUnicode.FILENAME_EXTENSION);
    if (oldFile.exists())
      oldFile.delete();

    MakeLegacy.make(inputFilename, inputFilename);
    MakeLanguageUnicode.make(inputFilename, inputFilename);

    System.exit(SUCCESS);

  }
}
