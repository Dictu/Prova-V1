/**
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * <p>
 * http://ec.europa.eu/idabc/eupl
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * <p>
 * Date:      18-12-2016
 * Author(s): Sjoerd Boerhout, Robert Bralts & Coos van der Galiën
 * <p>
 */
package nl.dictu.prova.framework.parameters;

import org.junit.BeforeClass;

import nl.dictu.prova.Junit;

/**
 * Contains all the common functions of the test action parameter XPath.
 * XPath extends the basic functions of Text
 * 
 * @author  Sjoerd Boerhout
 * @since   2016-04-21
 */
public class XpathTest
{
  /*
   *  One-time initialization code
   */
  @BeforeClass 
  public static void oneTimeSetUp()
  {
    Junit.configure();
  }
  
/*  *//**
   * Constructor
   *//*
  public Xpath()
  {
    super();
    
    // Update limitations
    minLength = 5;
    maxLength = Integer.MAX_VALUE;
  }

  
  *//**
   * Validate if <value> is a valid Xpath.
   * 
   * @param value
   * @return
   *//*
  public boolean isValid(String xPath)
  {
    LOGGER.trace("Validate '{}' as Xpath. Min: {}, Max: {}", 
                 () -> xPath, () -> minLength, () -> maxLength);
    
    // TODO implement a proper Xpath validation
    return validateString(xPath, minLength, maxLength);
  }  */
}
