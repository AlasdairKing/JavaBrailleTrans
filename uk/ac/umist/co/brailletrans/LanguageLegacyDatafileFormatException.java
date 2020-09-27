package uk.ac.umist.co.brailletrans;
/**
 * Indicates an error in loading and parsing a legacy 256-character language
 * file for use.
 *
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class LanguageLegacyDatafileFormatException extends Exception

{
  /**
   * Creates a new default LanguageLegacyDatafileFormatException
   */
  public LanguageLegacyDatafileFormatException()
  {
    super();
  }
  /**
   * Creates a new LanguageLegacyDatafileFormatException  with the error text held
   * in <CODE>description</CODE>.
   *
   * @param description  <CODE>String</CODE> containing description of error.
   */
  public LanguageLegacyDatafileFormatException(String description)
  {
    super(description);
  }
}
