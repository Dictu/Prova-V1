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
package nl.dictu.prova.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import nl.dictu.prova.Junit;

import java.util.Map.Entry;

/*
 * Issue ID:    PROVA-32
 * Requirement: Test suites and test cases structure
 * 
 * Create a tree structure of test suites and test cases where the suites
 * are the tree and the cases are the leafs.
 */
public class TestSuiteTest
{
  private static TestSuite testRoot;
  private static TestSuite childTestSuite;
  private static TestSuite subTestSuite;
  
  /*
   *  One-time initialization code
   */
  @BeforeClass 
  public static void oneTimeSetUp()
  {
    Junit.configure();
    
    /*
     * Create virtual structure of test suites and test cases:
     * Root test suite:testRoot
     * 0:TC: Shl-00
     * 0:TC: Web-00
     * 
     * 0:TS: TestSuite-00
     * 1: TC: Shl-00-01
     * 1: TC: Web-00-01
     * 
     * 1: TS: TestSuite-01-01
     * 2:  TC: Shl-01-01-01
     * 2:  TC: Web-01-01-01
     * 2:  TC: Shl-01-01-02
     * 2:  TC: Web-01-01-02
     * 
     * 2:  TS: TestSuite-01-01-01
     * 3:   TC: Shl-01-01-01-01
     * 3:   TC: Web-01-01-01-01
     * 
     * 2:  TS: TestSuite-01-01-02
     * 3:   TC: Shl-01-01-02-01
     * 3:   TC: Web-01-01-02-01
     * 
     * 1: TS: TestSuite-01-02
     * 2:  TC: Shl-01-02-01
     * 2:  TC: Web-01-02-01
     * 2:  TC: Shl-01-02-02
     * 2:  TC: Web-01-02-02
     * 
     * 2:  TS: TestSuite-01-02-01
     * 3:   TC: Shl-01-02-01-01
     * 3:   TC: Web-01-02-01-01
     * 
     * 2:  TS: TestSuite-01-02-02
     * 3:   TC: Shl-01-02-02-01
     * 3:   TC: Web-01-02-02-01
     */
    
    try
    {
      TestSuite tmpTestSuiteI;
      TestSuite tmpTestSuiteJ;
      TestSuite tmpTestSuiteK;
      
      testRoot = new TestSuite("testRoot");
      
      testRoot.addTestCase(new TestCase("Shl-00"));
      testRoot.addTestCase(new TestCase("Web-00"));
      
      tmpTestSuiteI = new TestSuite("TestSuite-00");
      testRoot.addTestSuite(tmpTestSuiteI);
      childTestSuite = tmpTestSuiteI;
      
      for(int i=1; i<2; i++)
      { 
        tmpTestSuiteI.addTestCase(new TestCase("Shl-00-0" + i));
        tmpTestSuiteI.addTestCase(new TestCase("Web-00-0" + i));
              
        for(int j=1; j<3; j++)
        {
          tmpTestSuiteJ = new TestSuite("TestSuite-0" + i + "-0" + j); 
          tmpTestSuiteI.addTestSuite(tmpTestSuiteJ);
          
          for(int k=1; k<3; k++)
          {
            tmpTestSuiteJ.addTestCase(new TestCase("Shl-0" + i + "-0" + j + "-0" + k));
            tmpTestSuiteJ.addTestCase(new TestCase("Web-0" + i + "-0" + j + "-0" + k));
            
            tmpTestSuiteK = new TestSuite("TestSuite-0" + i + "-0" + j + "-0" + k); 
            tmpTestSuiteJ.addTestSuite(tmpTestSuiteK);
            // For use in one of the unit tests
            subTestSuite = tmpTestSuiteK;
            
            for(int l=1; l<2; l++)
            {
              tmpTestSuiteK.addTestCase(new TestCase("Shl-0" + i + "-0" + j + "-0" + k + "-0" + l));
              tmpTestSuiteK.addTestCase(new TestCase("Web-0" + i + "-0" + j + "-0" + k + "-0" + l));
            }
          }
        }
      }
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }

  /**
   * Debug function used for displaying the test tree of test suites
   * and test cases used in this class.
   * Prints a line with <level>x space, <name> and line end.
   * 
   * @param level
   * @param name
   */
  private static void printLine(int level, String name)
  {
    System.out.print(level + ":");
    
    for( int i = 0; i < level; i++)
    {
      System.out.print(" ");
    }
    System.out.println(name);
  }

  /**
   * Debug function used for displaying the test tree of test suites
   * and test cases used in this class.
   * Loops recursively through the tree of test suites and test cases.
   * 
   * @param rootTestSuite
   * @param level
   */
  private static void printTestSuite(TestSuite rootTestSuite, int level)
  {
    for(Entry<String, TestCase> entry : rootTestSuite.getTestCases().entrySet())
    {
      printLine(level, "TC: " + entry.getValue().getId());
    }
    System.out.println("");
    
    for(Entry<String, TestSuite> entry : rootTestSuite.getTestSuites().entrySet())
    {
      printLine(level, "TS: " + entry.getValue().getId());
      printTestSuite(entry.getValue(), level+1);
    }
  }
  
  /*
   * Temp test to print the created structure.
   */
  @Test
  @Ignore
  public void printTestStructure() 
  {
    System.out.println("Root test suite:" + testRoot.getId());
    printTestSuite(testRoot, 0);
  } 
  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Unique identifier per test suite which is not empty
   * 
   * Create a test suite with a valid identifier.
   */
  @Test
  public void createTestSuiteWithValidIdentifier() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("qwerty");
      assertTrue(testSuite.getId().contentEquals("qwerty"));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Unique identifier per test suite which is not empty
   * 
   * Validate that ' ' is not a valid identifier
   */
  @Test
  public void createTestSuiteWithEmptyIdentifier() 
  {
    try
    {
      @SuppressWarnings("unused")
      TestSuite tmpTestSuite1 = new TestSuite(" "); 
                
      fail("Empty identifier is not allowed!");
    }
    catch(Exception eX){}
  } 
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Unique identifier per test suite which is not empty
   * 
   * Validate that 'null' is not a valid identifier
   */
  @Test
  public void createTestSuiteWithNoIdentifier() 
  {
    try
    {
      @SuppressWarnings("unused")
      TestSuite tmpTestSuite1 = new TestSuite(null); 
                
      fail("Empty identifier is not allowed!");
    }
    catch(Exception eX){}
  } 
  
  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Unique identifier per test suite which is not empty
   * 
   * Create a test suite with a parent and a valid identifier.
   */
  @Test
  public void createTestSuiteWithParentAndValidIdentifier() 
  {
    try
    {
      TestSuite tsParent = new TestSuite("parent");
      
      TestSuite testSuite = new TestSuite("qwerty", tsParent);
      assertTrue(testSuite.getId().contentEquals("qwerty"));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Unique identifier per test suite which is not empty
   * 
   * Validate that ' ' is not a valid identifier
   */
  @Test
  public void createTestSuiteWithParentAndEmptyIdentifier() 
  {
    TestSuite tmpTestSuite1 = null;
    
    try
    {
      TestSuite tsParent = new TestSuite("parent");
      
      tmpTestSuite1 = new TestSuite(" ", tsParent); 
                
      fail("Empty identifier is not allowed!");
    }
    catch(Exception eX)
    {
      assertTrue(tmpTestSuite1 == null);
    }
  } 
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Unique identifier per test suite which is not empty
   * 
   * Validate that 'null' is not a valid identifier
   */
  @Test
  public void createTestSuiteWithParentAndNoIdentifier() 
  {
    TestSuite tmpTestSuite1 = null;
    
    try
    {
      tmpTestSuite1 = new TestSuite(null); 
                
      fail("Empty identifier is not allowed!");
    }
    catch(Exception eX)
    {
      assertTrue(tmpTestSuite1 == null);
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Each child test suite must be unique to prevent a loop
   * 
   * Validate that an added test suite doesn't already exists in the tree
   */
  @Test
  public void createTestSuiteWithDirectParentLoop() 
  {
    TestSuite testSuiteRoot = null;
    
    try
    { 
      testSuiteRoot = new TestSuite("root");
      TestSuite testSuite1 = new TestSuite("azerty", testSuiteRoot);
      
      testSuiteRoot.addTestSuite(testSuite1);
      
      fail("Child test suites must be unique!");
    }
    catch(Exception eX)
    {
      assertEquals(0, testSuiteRoot.getTestSuites().size());
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Each child test suite must be unique to prevent a loop
   * 
   * Validate that an added test suite doesn't already exists in the tree
   */
  @Test
  public void createTestSuiteWithIndirectParentLoop() 
  {
    TestSuite testSuiteRoot = null;
    
    try
    { 
      testSuiteRoot = new TestSuite("root");
      TestSuite testSuite1 = new TestSuite("azerty", testSuiteRoot);
      TestSuite testSuite2 = new TestSuite("azerty", testSuite1);
      
      testSuiteRoot.addTestSuite(testSuite2);
      
      fail("Child test suites must be unique!");
    }
    catch(Exception eX)
    {
      assertEquals(0, testSuiteRoot.getTestSuites().size());
    }
  } 

  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each child test suite must be unique to prevent a loop
   * 
   * Validate that an added test suite doesn't already exists in the tree
   */
  @Test
  public void createTestSuiteWithItselfAsParent() 
  {
    TestSuite testSuiteRoot = null;
    
    try
    { 
      testSuiteRoot = new TestSuite("root");
      
      testSuiteRoot.addTestSuite(testSuiteRoot);
      testSuiteRoot.setParent(testSuiteRoot);
      
      fail("A test suite can't be it's own parent!");
    }
    catch(Exception eX)
    {
      assertEquals(0, testSuiteRoot.getTestSuites().size());
    }
  } 
  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have no or 1 parent.
   * 
   * Create a test suite with no parent
   */
  @Test
  public void createTestSuiteWithNoParent() 
  {
    try
    {
      TestSuite tmpTestSuite01 = new TestSuite("qwerty");
      
      assertNull(tmpTestSuite01.getParent());
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  } 
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have no or 1 parent.
   * 
   * Create a test suite with a parent and check it
   */
  @Test
  public void createTestSuiteWithParent() 
  {
    try
    {
      TestSuite tmpTestSuite01 = new TestSuite("qwerty");
      TestSuite tmpTestSuite02 = new TestSuite("azerty", tmpTestSuite01);  
      
      assertTrue(tmpTestSuite02.getParent().equals(tmpTestSuite01));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have no or 1 parent.
   * 
   * Set and check the parent of a test suite
   */
  @Test
  public void checkIfTestSuiteHasParentViaConstructor() 
  {
    try
    {
      TestSuite tmpTestSuite01 = new TestSuite("qwerty");
      TestSuite tmpTestSuite02 = new TestSuite("azerty", tmpTestSuite01);
    
      assertTrue(!tmpTestSuite01.hasParent());
      assertTrue(tmpTestSuite02.hasParent());
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have no or 1 parent.
   * 
   * Set and check the parent of a test suite
   */
  @Test
  public void checkIfTestSuiteHasParentViaSetter() 
  {
    try
    {
      TestSuite tmpTestSuite01 = new TestSuite("qwerty");
      TestSuite tmpTestSuite02 = new TestSuite("azerty");
    
      assertTrue(!tmpTestSuite02.hasParent());
      
      tmpTestSuite02.setParent(tmpTestSuite01);
      
      assertTrue(tmpTestSuite02.hasParent());
      assertTrue(tmpTestSuite02.getParent().equals(tmpTestSuite01));
      
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  } 

  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have no or 1 parent.
   * 
   * Check if the correct root parent is found
   */
  @Test
  public void testTestSuiteGetRootParent() 
  {
    try
    {
      TestSuite tmpTestSuite01 = subTestSuite.getRootParent();
    
      assertTrue(tmpTestSuite01.equals(testRoot));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }

  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check that test suites can be added to the set
   */
  @Test
  public void addTestSuite() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("root");
      TestSuite child1 = new TestSuite("child1");
      TestSuite child2 = new TestSuite("child2");
      
      assertEquals(0,testSuite.getTestSuites().size());
      
      testSuite.addTestSuite(child1);
      assertEquals(1,testSuite.getTestSuites().size());
      
      testSuite.addTestSuite(child2);
      assertEquals(2,testSuite.getTestSuites().size());
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check that test suites can be retrieved after adding to the set
   */
  @Test
  public void getTestSuite() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("root");
      TestSuite child1 = new TestSuite("child1");
      TestSuite child2 = new TestSuite("child2");
      
      testSuite.addTestSuite(child1);
      
      assertTrue(testSuite.getTestSuite("child1").equals(child1));
      assertTrue(testSuite.getTestSuites().get("child1").equals(child1));
      
      testSuite.addTestSuite(child2);
      
      assertTrue(testSuite.getTestSuite("child2").equals(child2));
      assertTrue(testSuite.getTestSuites().get("child2").equals(child2));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Prevent a loop in the test suite tree
   * 
   * Look in provided structure for a direct child test suite
   */
  @Test
  public void hasChildTestSuite() 
  {   
    assertTrue(testRoot.hasTestSuite(childTestSuite));
    assertTrue(testRoot.hasTestSuite(childTestSuite, false));
    assertTrue(testRoot.hasTestSuite(childTestSuite, true));
  }
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Prevent a loop in the test suite tree
   * 
   * Check recursively all number of child test suites
   * if a specific test suite doesn't already exists.
   */
  @Test
  public void hasChildTestSuiteOnASubLevel() 
  {   
    assertFalse(testRoot.hasTestSuite(subTestSuite));
    assertFalse(testRoot.hasTestSuite(subTestSuite, false));
    
    assertTrue(testRoot.hasTestSuite(subTestSuite, true));    
  }
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check for correct number of child test suites
   */
  @Test
  public void CountChildTestSuites() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("root");
      
      assertEquals(0,testSuite.numberOfTestSuites());
      
      testSuite.addTestSuite(new TestSuite("child1"));
      assertEquals(1,testSuite.numberOfTestSuites());
      
      testSuite.addTestSuite(new TestSuite("child2"));
      assertEquals(2,testSuite.numberOfTestSuites());
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check for correct number of child test suites
   */
  @Test
  public void CountAllLevelsChildTestSuites() 
  {
    assertEquals(1,testRoot.numberOfTestSuites());
    assertEquals(1,testRoot.numberOfTestSuites(false));
    assertEquals(7,testRoot.numberOfTestSuites(true));
  }  
  
  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check that test cases can be added to the suite
   */
  @Test
  public void addTestCase() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("root");
      TestCase tc1 = new TestCase("tc1");
      TestCase tc2 = new TestCase("tc2");
      
      assertEquals(0,testSuite.getTestCases().size());
      assertEquals(0,testSuite.numberOfTestCases());
      
      testSuite.addTestCase(tc1);
      assertEquals(1,testSuite.getTestCases().size());
      
      testSuite.addTestCase(tc2);
      assertEquals(2,testSuite.getTestCases().size());
    }
    catch(Exception eX)
    {
      eX.printStackTrace();
      fail(eX.getMessage());
    }
  }
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check that test suites can be retrieved after adding to the set
   */
  @Test
  public void getTestCase() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("root");
      TestCase tc1 = new TestCase("tc1");
      TestCase tc2 = new TestCase("tc2");
      
      testSuite.addTestCase(tc1);
      
      assertTrue(testSuite.getTestCase("tc1").equals(tc1));
      assertTrue(testSuite.getTestCases().get("tc1").equals(tc1));
      
      testSuite.addTestCase(tc2);
      
      assertTrue(testSuite.getTestCase("tc2").equals(tc2));
      assertTrue(testSuite.getTestCases().get("tc2").equals(tc2));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check for correct number of child test suites
   */
  @Test
  public void CountChildTestCases() 
  {
    try
    {
      TestSuite testSuite = new TestSuite("root");
      
      assertEquals(0,testSuite.numberOfTestCases());
      assertEquals(0,testSuite.numberOfTestCases(false));
      assertEquals(0,testSuite.numberOfTestCases(true));
      
      testSuite.addTestCase(new TestCase("child1"));
      assertEquals(1,testSuite.numberOfTestCases());
      assertEquals(1,testSuite.numberOfTestCases(false));
      assertEquals(1,testSuite.numberOfTestCases(true));
      
      testSuite.addTestCase(new TestCase("child2"));
      assertEquals(2,testSuite.numberOfTestCases(false));
      assertEquals(2,testSuite.numberOfTestCases(true));
    }
    catch(Exception eX)
    {
      fail(eX.getMessage());
    }
  }  
  
  /*
   * Issue ID:    PROVA-32
   * Requirement: Each test suite can have 0...* child test suites.
   * 
   * Check for correct number of child test suites
   */
  @Test
  public void CountAllLevelsChildTestCases() 
  {
    assertEquals(2,testRoot.numberOfTestCases());
    assertEquals(2,testRoot.numberOfTestCases(false));
    assertEquals(20,testRoot.numberOfTestCases(true));
  }  
}
