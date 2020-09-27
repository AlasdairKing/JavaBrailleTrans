package uk.ac.umist.co.brailletrans.tests;
import java.io.*;

/**
 * Superclass of translation tools that use the Language programs.  Contains
 * various commonly-used utilities for reading from disk, converting arrays
 * and so forth.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class Translator
{

  /**
   * Some of the Translator methods test for file size: this is added to any
   * result in case of discrepancies in reality/JVM perception.
   */
  public static final int FILE_SIZE_ERROR_LEEWAY = 1000;

  /**
   * The int returned by input streams in Java that indicates that the stream
   * has not been completely read.
   */
  public static final int END_OF_FILE = -1;

  /**
   * Error code for errors with multiple possible causes.
   */
   public static final int UNKNOWN_ERROR = 666;

  /**
   * Error code for disk error.
   */
   public static final int DISK_ERROR = 1;

   /**
    * Report code indicating successful operation.
    */
   public static final int SUCCESS = 0;

  /**
   * Writes a <CODE>String</CODE> to a local file using <CODE>BufferedWriter</CODE>
   *
   * @param filename String containing full path and name of file to write to.
   * @param toWrite  String to be written to file in filename.
   */
  static public void writeStringToDisk(String filename, String toWrite)
    throws IOException
  {
    BufferedWriter outFile = null;
    try
    {
      outFile = new BufferedWriter(new FileWriter(filename));
      outFile.write(toWrite);
      outFile.close();
    }
    catch (IOException e)
    {
      throw new IOException("Failed to write output file: " + e);
    }
  }

  /**
   * Reads text as a <CODE>String</CODE> from a local file using <CODE>java.io.BufferedReader</CODE>.
   *
   * @param filename  <CODE>String</CODE> containing the full path and filename of the local file to read text from.
   * @return toReturn The <CODE>String</CODE> read from the local file.
   */
  static public String readStringFromDisk(String filename) throws FileNotFoundException, IOException
  {
    BufferedReader inFile = null;
    String toReturn = null;
    // Open the input file
	  try
    {
      inFile = new BufferedReader(new FileReader(filename));
    }
	  catch (FileNotFoundException e)
	  {
      throw new FileNotFoundException("Unable to find file " + filename +
       " \nReported error: " + e);
    }

    // Get the file into toReturn
	  try
    {
      while (inFile.ready())
        toReturn = toReturn + inFile.readLine() + " ";
    }
	  catch (IOException e)
	  {
      throw new IOException("Error when reading from file " + filename +
       " \nReported error: " + e);
    }

    // Close the input file
	  try
    {
      inFile.close();
    }
	  catch (IOException e)
	  {
      throw new IOException("Unable to close file " + filename +
       " \nReported error:" + e);
    }
    // since the wretched input stream puts a NULL at the beginning of each
    // stream, hack it off the beginning of the string.
    if (toReturn.substring(0,4).equalsIgnoreCase("null"))
      toReturn = toReturn.substring(4, toReturn.length());
    return toReturn;
  }

  /**
   * Reads an array of ints from a local file using <CODE>BufferedInputStream</CODE>.
   *
   * @param filename  <CODE>String</CODE> containing the full path and filename of the local file to read from.
   * @return toReturn An int[] containing ints read from file.
   */
  static public int[] readIntArrayFromDisk(String filename)
    throws FileNotFoundException, IOException
  {
    BufferedInputStream inFile = null;
    int[] toReturn;
    int[] gotFile = null;

        // Open the input file
	  try
    {
      inFile = new BufferedInputStream(new FileInputStream(filename));
    }
	  catch (FileNotFoundException e)
	  {
      throw new FileNotFoundException("Unable to find file " + filename +
       " \nReported error: " + e);
    }

    // Get the file into toReturn
    int counter = 0;
    // ASSERTION: counter=0, no bytes have been read
	  try
    {
      gotFile = new int[inFile.available() + FILE_SIZE_ERROR_LEEWAY];

      int got = inFile.read();
      while (got != END_OF_FILE)
      {
        gotFile[counter] = got;
        counter++;
        // ASSERTION: one and only one byte has been written to gotFile, one and only
        // one increment has been made to counter => counter==number of bytes
        got = inFile.read();
      }
    }
	  catch (IOException e)
	  {
      throw new IOException("Error when reading from file " + filename +
       " \nReported error: " + e);
    }
    // Close the input file
	  try
    {
      inFile.close();
    }
	  catch (IOException e)
	  {
      throw new IOException("Unable to close file " + filename +
       " \nReported error:" + e);
    }
    // can now declare correct toReturn size
    toReturn = new int[counter];
    // Copy array got to correct-size toReturn array
    System.arraycopy(gotFile, 0, toReturn, 0, counter);
    return toReturn;
  }

  /**
   * Converts an arrays of ints into an array of bytes
   *
   * @param toConvert int[] to be converted into byte[]
   * @return toReturn byte[] result of conversion from int[]
   */
  public static byte[] turnIntoByteArray(int[] toConvert)
  {
    byte[] toReturn = new byte[toConvert.length];
    for (int i = 0; i < toConvert.length; i++)
      toReturn[i] = (byte) toConvert[i];
    return toReturn;
  }

  /**
   * Converts an array of ints into a String
   *
   * @param toConvert int[] to be converted into String
   * @return toReturn String from toConvert
   */
  public static String turnIntoString(int[] toConvert)
  {
    String toReturn = "";
    for (int i = 0; i < toConvert.length; i++)
      toReturn += (char) toConvert[i];
    return toReturn;
  }

  /**
   * Converts an array of bytes into a String
   *
   * @param toConvert byte[] to be converted into String
   * @return toReturn String from toConvert
   */
  public static String turnIntoString(byte[] toConvert)
  {
    String toReturn = "";
    for (int i = 0; i < toConvert.length; i++)
      toReturn += (char) toConvert[i];
    return toReturn;
  }


  /**
   * Saves an array of integers to disk using FileOutputStream
   *
   * @param toWrite    Array of ints to write to disk.
   * @param filename   String holding the full path and filename to write to.
   */
   public static void writeIntArrayToDisk(int[] toWrite, String filename)
     throws IOException
   {
     try
     {
       BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
       for (int i = 0; i < toWrite.length; i++)
         out.write(toWrite[i]);
       out.close();
     }
     catch (IOException e) { throw new IOException("Unable to write file: " +
       filename); }

  }


}