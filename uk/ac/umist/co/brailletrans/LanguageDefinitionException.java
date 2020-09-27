package uk.ac.umist.co.brailletrans;

/**
 * Indicates an error in the creation of a new LanguageUnicode object.  This
 * is thrown when one of the LanguageUnicode <CODE>set</CODE> methods is used
 * to add data (character rules, translation rules, state descriptions and so
 * on) to a new LanguageUnicode (created by <CODE>new LanguageUnicode()</CODE>
 * but the methods are used with invalid parameters or in the incorrect order.
 * 
 * <p><small>Copyright 1999, 2004 Alasdair King. This program is free software 
 * under the terms of the GNU General Public License. </small>
 *
 * @author Alasdair King, alasdairking@yahoo.co.uk
 * @version 1.0 09/01/2001
 */
public class LanguageDefinitionException extends Exception
{
  /**
   * Creates a new default LanguageDefinitionException
   */
  public LanguageDefinitionException()
  {
    super();
  }
  /**
   * Creates a new LanguageDefinitionException with the error text held
   * in <CODE>description</CODE>.
   *
   * @param description  <CODE>String</CODE> containing description of error.
   */
  public LanguageDefinitionException(String description)
  {
    super(description);
  }
}