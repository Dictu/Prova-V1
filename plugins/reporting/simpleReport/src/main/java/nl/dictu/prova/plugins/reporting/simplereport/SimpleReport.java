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
package nl.dictu.prova.plugins.reporting.simplereport;


import nl.dictu.prova.Config;
import nl.dictu.prova.TestRunner;
import nl.dictu.prova.framework.TestAction;
import nl.dictu.prova.framework.TestCase;
import nl.dictu.prova.framework.TestStatus;
import nl.dictu.prova.framework.TestSuite;
import nl.dictu.prova.plugins.reporting.ReportingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;


/*
 * Hello world!
 *
 */
public class SimpleReport implements ReportingPlugin
{
  final static Logger LOGGER = LogManager.getLogger();
  private PrintWriter pwTestcase;
  private PrintWriter pwTestsuite;
  private PrintWriter pwSummary;
  private TestRunner testRunner;
  
  private String testRoot;
  private String reportRoot;
  private String currTestSuiteDir;
  private String currTestCaseFile;
  //private String fileName;
  private Long startTime;
  private Long startTimeTestsuite;
  private Boolean summaryCreated = false;
  private Long startTimeSummary;
  private Integer countPassedTestcases = 0;
  private Integer countFailedTestcases = 0;
	private String testProject;

  @Override
  public void init(TestRunner testRunner) throws Exception
  {
    LOGGER.debug("Init: reporting plugin Simple Report!");
    
    if(testRunner == null)
       throw new Exception("No testRunner supplied!");
    
    this.testRunner = testRunner;
  }

  @Override
  public void setUp(String projectName) throws Exception
  {
	  try
	  {
		  LOGGER.debug("SimpleReport: setUp for project '" + projectName + "'");
		  
		  // Save the test root to strip that part from the test suite/case names.
		  this.testRoot = testRunner.getPropertyValue(Config.PROVA_TESTS_ROOT);
		  
			// save project name, needed for making html links relative
			this.testProject = testRunner.getPropertyValue(Config.PROVA_PROJECT);
		  /*
		   * - Check if property 'prova.plugins.reporting.dir' is an existing dir.
		   * - if not: Try to create it as a sub-dir in the Prova root dir.
		   */
		  String dirName = testRunner.getPropertyValue(Config.PROVA_PLUGINS_REPORTING_DIR);
		  File dir = new File(dirName);
		  
		  // Check if configured path is an absolute and existing path
		  if(checkValidReportDir(dir))
		  {
			  LOGGER.debug("Set up output directory for reporting to '" + dir.getAbsolutePath() + "'");
		  }
		  else
		  {
			  if (Boolean.valueOf(testRunner.getPropertyValue(Config.PROVA_PLUGINS_REPORTING_CREATE_FOLDERS))) {
				  dirName = testRunner.getPropertyValue(Config.PROVA_DIR) + File.separator +
						  testRunner.getPropertyValue(Config.PROVA_PLUGINS_REPORTING_DIR);
			  }
			  else
			  {
				  dirName = testRunner.getPropertyValue(Config.PROVA_PLUGINS_REPORTING_DIR);
			  }
			  dir = new File(dirName);
			  
			  // Try if the configured path is a sub-directoy of the Prova root path
			  if(checkValidReportDir(dir))
			  {
				  LOGGER.debug("Set up output directory for reporting to '" + dir.getAbsolutePath() + "'");
			  }
			  else
			  {
				  // Dir doesn't exists. Create it!
				  dir.mkdirs();
				  LOGGER.debug("Created output directory '" + dir.getAbsolutePath() + "' for reporting to.");
			  }
		  }
          reportRoot = dir.getAbsolutePath() + File.separator;
		  // Create a directory for this testrun
		  if (Boolean.valueOf(testRunner.getPropertyValue(Config.PROVA_PLUGINS_REPORTING_CREATE_FOLDERS)))
		  {
			  reportRoot = reportRoot +
					  (projectName.length() > 0 ? (projectName + File.separator) : "") +
					  LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")).toString() +
					  File.separator +
					  LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-MMM")).toString() +
					  File.separator +
					  LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd")).toString() +
					  File.separator +
					  LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss")).toString();

			  new File(reportRoot).mkdirs();
		  }
		  else
		  {
			  LOGGER.debug("Not creating folders as requested. Reports will be placed in: "+reportRoot);
              deleteFolder(dir);
		  }
		  // Create the start of the summary file
		  pwSummary = createPW(reportRoot + File.separator + "Testrun_Summary.html");
    	  pwSummary.println("<!DOCTYPE html>");
    	  pwSummary.println("<html>");
    	  pwSummary.println("<head>");
    	  pwSummary.println("<style> table, td { 	border: 1px solid black;	border-collapse: collapse;	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "th {	text-align: left;	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "tr:nth-child(odd) {	background: #CBCDCD;}"
		  							+ "p {	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "br {	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "h1 {	font-family: Verdana, Helvetica, sans-serif;	font-size: 30px;}</style>");
    	  pwSummary.println("<title>Prova Testreport</title>");
    	  pwSummary.println("</head>");
    	  pwSummary.println("<body>");	 
    	  pwSummary.println("<h1>Testrun Summary</h1>");
		  startTimeSummary = System.currentTimeMillis();
		  pwSummary.println("<br><b>Starttime: </b>" + LocalDateTime.now() +"</br>");
		  pwSummary.println("<table>			<tr>				<th>Testcase</th><th>Error</th><th>Details</th></tr>");
		  summaryCreated = true;
	  }
	  catch(Exception eX)
	  {
		  LOGGER.debug("Setup SimpleReport has failed: " + eX);
		  throw eX;
	  }
  }
  
  private boolean checkValidReportDir(File testDir)
  {
	  try
	  {
		  LOGGER.trace("Check if '" + testDir.getAbsolutePath() + "' is a valid Reporting dir");
		  
		  if(!testDir.exists()) 
		  {
			  LOGGER.warn("Reporting dir '" + testDir.getAbsolutePath() + "' doesn't exists.");
			  return false;
		  }
		  
		  if(!testDir.isDirectory()) 
		  {
			  LOGGER.warn("Reporting dir '" + testDir.getAbsolutePath() + "' is not a directory.");
			  return false;
		  }
		  
		  if(!testDir.canWrite())
		  {
			  LOGGER.warn("Reporting dir '" + testDir.getAbsolutePath() + "' is not writable.");
			  return false;
		  }  
		
		  LOGGER.trace("'" + testDir.getAbsolutePath() + "' is a valid Reporting dir!");
		  return true;
	  }
	  catch(Exception eX)
	  {
		  return false;
	  }
  }

  @Override
  public void logStartTest(TestCase testCase) throws Exception
  {
	  try
	  {
	    // /Users/sjoerd/Workspaces/Prova-1.0/_Prova/projects/demo/NS/NS.xlsm/WEB_NS-001//NS_WEB_NS-001.xlsx/WEB_AVI_001/TEST 01
	    String testCaseFilename = "";
      String testCaseSheetname = "";
      String testCaseDataSetFileName = "";
      String testCaseDataSetSheetName = "";
      String testCaseDataSetColumnName = "";
      String tmp;
      int iIndex;
      boolean hasDataFile = false;
      
		  LOGGER.debug("SimpleReport: Start new Test Case: '{}'", testCase.getId());
		  
		  //LOGGER.fatal("TestRoot: {}", testRoot);
      //LOGGER.fatal("currTestSuiteDir: {}", currTestSuiteDir);
      
      //currTestCaseFile = currTestSuiteDir;
      iIndex = testCase.getId().lastIndexOf(File.separator + File.separator);
      if( iIndex > 0)  
      {
        tmp = testCase.getId().substring(0, testCase.getId().lastIndexOf(File.separator + File.separator));
        hasDataFile = true;
      }
      else
      {
        tmp = testCase.getId();
      }
      
      iIndex = tmp.lastIndexOf(File.separator);
      
      testCaseSheetname = tmp.substring(iIndex + File.separator.length());
      
      tmp = tmp.substring(0, iIndex);
      // TODO: Change 5 with length of file extension test scripts!
      testCaseFilename = tmp.substring(tmp.lastIndexOf(File.separator) + File.separator.length(), tmp.length() - 5);
      
      // Find dataset if available
      if(hasDataFile)  
      {
        tmp = testCase.getId().substring(testCase.getId().lastIndexOf(File.separator + File.separator) + (File.separator.length() *2));
        testCaseDataSetFileName = tmp.substring(0, tmp.indexOf(".xlsx"));
        
        tmp = tmp.substring(tmp.indexOf(File.separator) + File.separator.length());
        
        testCaseDataSetSheetName = tmp.substring(0, tmp.indexOf(File.separator));
        testCaseDataSetColumnName = tmp.substring(tmp.indexOf(File.separator) + File.separator.length());
      }
      
      LOGGER.trace("testCaseFilename: {}", testCaseFilename);
      LOGGER.trace("testCaseSheetname: {}", testCaseSheetname);
      LOGGER.trace("testCaseDataSetFileName: {}", testCaseDataSetFileName);
      LOGGER.trace("testCaseDataSetSheetName: {}", testCaseDataSetSheetName);
      LOGGER.trace("testCaseDataSetColumnName: {}", testCaseDataSetColumnName);
      
      currTestCaseFile = currTestSuiteDir + 
                         File.separator +
                         testCaseFilename +                        
                         File.separator +
                         testCaseSheetname +
                         (testCaseDataSetFileName.length() > 0 ? File.separator : "") +
                         testCaseDataSetFileName + 
                         (testCaseDataSetSheetName.length() > 0 ? File.separator : "") +
                         testCaseDataSetSheetName +
                         (testCaseDataSetColumnName.length() > 0 ? File.separator : "") +
                         testCaseDataSetColumnName +
                         ".html";
          
      LOGGER.trace("Log file name for test case: '{}'", currTestCaseFile);
      
      tmp = currTestCaseFile.substring(0, currTestCaseFile.lastIndexOf(File.separator));
      LOGGER.trace("Create directory for test case: '{}'", tmp);
      new File(tmp).mkdirs();
		  
		  LOGGER.debug("Write begin testcase (r)");
		  File file = new File(currTestCaseFile);
    	  if(!file.exists())
    	  {
    		  LOGGER.trace("Creating file: '" + file + "'");
    		  file.createNewFile();
    	  }
    	  FileWriter fw = new FileWriter(file,true);
    	  BufferedWriter bw = new BufferedWriter(fw);
    	  //pwTestcase = new PrintWriter(bw);*/
    	  pwTestcase = createPW(currTestCaseFile);
    	  pwTestcase.println("<!DOCTYPE html>");
    	  pwTestcase.println("<html>");
    	  pwTestcase.println("<head>");
    	  pwTestcase.println("<style> table, td { 	border: 1px solid black;	border-collapse: collapse;	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "th {	text-align: left;	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "tr:nth-child(odd) {	background: #CBCDCD;}"
		  							+ "p {	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "br {	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "h1 {	font-family: Verdana, Helvetica, sans-serif;	font-size: 30px;}</style>");
    	  pwTestcase.println("<title>Prova Testreport</title>");
    	  pwTestcase.println("</head>");
    	  pwTestcase.println("<body>");	 
		  pwTestcase.println("<h1>"+testCase.getId().substring(testCase.getId().lastIndexOf("\\")+1)+"</h1>");
		  startTime = System.currentTimeMillis();
		  pwTestcase.println("<br><b>Starttime: </b>" + LocalDateTime.now() +"</br>");
		  pwTestcase.println("<table>			<tr>				<th>Result</th><th>Action</th><th>Info</th><th>Time</th></tr>");
		  
		  //this.shutDown();
	  }
	  catch(IOException eX)
	  {
		  LOGGER.debug("Write file has failed; " + eX);
		  throw eX;
	  }
  }

	private String getScreenShotFile(String status, TestAction action) throws Exception {
		String result = null;
		if (this.testRunner.hasPropertyValue("SCREENSHOT_PATH")
				&& this.testRunner.getPropertyValue("SCREENSHOT_PATH").length() > 1) {
			LOGGER.trace("SCREENSHOT_PATH found");
			try {
				String errorPrefix = "";
				if ("NOK".equals(status)) {
					errorPrefix = "error_";
				}

				File source = new File(testRunner.getPropertyValue("SCREENSHOT_PATH"));
				String destFileName = makeFilenameValid(errorPrefix + action.getId() + "_" + source.getName() );
				String destinationPath = currTestCaseFile.substring(0, currTestCaseFile.lastIndexOf(File.separator))
						+ File.separator + destFileName;
				LOGGER.debug("destinationPath: " + destinationPath);
				File destination = new File(destinationPath);

				if (!destination.exists()) {
					copyFileUsingChannel(source, destination);
				}
				result = "." + File.separator + destFileName;
				this.testRunner.setPropertyValue("SCREENSHOT_PATH", "");

			} catch (Exception eX) {
				LOGGER.error("Exception in logging testAction! ({})", eX.getMessage());
			}
		}
		return result;
	}

	
	@Override
	public void logAction(TestAction action, String status, long executionTime) throws Exception {
		String color = "red";
		String screenshotFile = getScreenShotFile(status, action);
		if (status.equalsIgnoreCase("ok")) {
			color = "lightgreen";
		}

		String statusHtml = status;
		if (screenshotFile != null) {
			statusHtml = "<a href=\"" + screenshotFile + "\"" + "target=\"_blank\" >" + status + "</a>";
		}
		String sAction = action.toString();
		if (sAction.toLowerCase().contains("setdbproperties")||sAction.toLowerCase().contains("setsoapproperties"))
		{
            String aAction[] = sAction.split(":");
		    sAction = aAction[0] + ": ***************************************************************************************";
		}
		if (sAction.toLowerCase().contains("setsoapmessage")||sAction.toLowerCase().contains("setquery"))
		{
			sAction = "<textarea style=\"width:1100px\">" + sAction + "</textarea>";
		}
		if (sAction.toLowerCase().contains("executedbtest"))
		{
			sAction = "<textarea style=\"width:1100px\">" + sAction + "</textarea>";
		}
		try {
			pwTestcase.println("<tr><td style=\"width:200px\" bgcolor=\"" + color + "\">" + statusHtml
					+ "</td><td style=\"width:1200px\">" + sAction + "</td><td style=\"width:200px\">"
					+ (action.getId()) + "</td><td style=\"width:200px\">" + executionTime + "ms</td></tr>");
		} catch (Exception eX) {
			LOGGER.error("Exception in logging testAction! ({})", eX.getMessage());
			pwTestcase
					.println("<tr><td style=\"width:200px\" bgcolor=\""
							+ color
							+ "\">N/A</td><td style=\"width:1200px\">UNKNOWN ACTION</td><td style=\"width:200px\">Unknown action id</td><td style=\"width:200px\">"
							+ executionTime + "ms</td></tr>");
		}
		pwTestcase.flush();
	}

  @Override
  public void logEndTest(TestCase testCase) throws Exception
  {
	  // Write end testcase report and close stream
	  Long elapsedTime = System.currentTimeMillis() - startTime;
	  pwTestcase.println("<br><b>Endtime: </b>" + LocalDateTime.now()+"</br>");
	  pwTestcase.println("<br><b>Runtime in seconds: </b>" + elapsedTime/1000 + "</br>");
		String color = "green";
		if (TestStatus.FAILED.equals(testCase.getStatus())||TestStatus.COMPLETED.equals(testCase.getStatus())) {
			color = "red";
		}


		pwTestcase.println("<br><b>Status testcase: <font color=\"" + color + "\">" + testCase.getStatus()
				+ "</b></font></br>");
		
		if (testCase.getStatus().toString().equalsIgnoreCase("passed")) {
		  countPassedTestcases = countPassedTestcases + 1;
		
			pwTestsuite.println("<tr><td style=\"width:200px\" bgcolor=\"lightgreen\">" + testCase.getStatus()
					+ "</td><td style=\"width:1200px\">"
					+ testCase.getId().substring(testCase.getId().lastIndexOf("\\") + 1)
					+ "</td><td style=\"width:200px\">" + "<a href=\"" + makeHtmlLinkRelative(currTestCaseFile)
				  				+ "\">Result testcase</a></td></tr>");
			pwSummary.println("<tr><td style=\"width:200px\" bgcolor=\"lightgreen\">"
					+ testCase.getId().substring(testCase.getId().lastIndexOf("\\") + 1)
					+ "</td><td style=\"width:1200px\">" + testCase.getSummary() + "</td><td style=\"width:200px\">"
					+ "<a href=\"" + makeHtmlLinkRelative(currTestCaseFile) + "\">Result testcase</a></td></tr>");

		}
		else if (testCase.getStatus().toString().equalsIgnoreCase("completedwitherrors")){
		  countFailedTestcases = countFailedTestcases + 1;
			pwTestsuite.println("<tr><td style=\"width:200px\" bgcolor=\"orange\">" + testCase.getStatus()
				    +"</td><td style=\"width:1200px\">"
					+ testCase.getId().substring(testCase.getId().lastIndexOf("\\") + 1)
					+ "</td><td style=\"width:200px\">" + "<a href=\"" + makeHtmlLinkRelative(currTestCaseFile)
	  				+ "\">Result testcase</a></td></tr>");
			pwSummary.println("<tr><td style=\"width:200px\" bgcolor=\"orange\">"
					+ testCase.getId().substring(testCase.getId().lastIndexOf("\\") + 1)
					+ "</td><td style=\"width:1200px\">" + testCase.getSummary() + "</td><td style=\"width:200px\">"
					+ "<a href=\"" + makeHtmlLinkRelative(currTestCaseFile) + "\">Result testcase</a></td></tr>");
	  }
	 	 else {
	  countFailedTestcases = countFailedTestcases + 1;
	  pwTestsuite.println("<tr><td style=\"width:200px\" bgcolor=\"red\">" + testCase.getStatus()
			  +"</td><td style=\"width:1200px\">"
			  + testCase.getId().substring(testCase.getId().lastIndexOf("\\") + 1)
			  + "</td><td style=\"width:200px\">" + "<a href=\"" + makeHtmlLinkRelative(currTestCaseFile)
			  + "\">Result testcase</a></td></tr>");
	  pwSummary.println("<tr><td style=\"width:200px\" bgcolor=\"red\">"
			  + testCase.getId().substring(testCase.getId().lastIndexOf("\\") + 1)
			  + "</td><td style=\"width:1200px\">" + testCase.getSummary() + "</td><td style=\"width:200px\">"
			  + "<a href=\"" + makeHtmlLinkRelative(currTestCaseFile) + "\">Result testcase</a></td></tr>");
  }
	  pwTestcase.println("<br><b>Summary: </b>" + testCase.getSummary()+"</br>");
	  pwTestcase.println("</table>");
	  pwTestcase.println("</body>");
	  pwTestcase.println("</html>");
	  pwTestcase.close();
    
  }

  @Override
  public void logMessage(String message) throws Exception
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void logMessage(String[] messages) throws Exception
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void shutDown()
  {
    //Close summary report file
	try
    {
    	LOGGER.debug("shutdown");
    	Long elapsedTime = System.currentTimeMillis() - startTimeSummary;
    	pwSummary.println("<br><b>Endtime: </b>" + LocalDateTime.now()+"</br>");
    	pwSummary.println("<br><b>Runtime in seconds: </b>" + elapsedTime/1000 + "</br>");
    	pwSummary.println("<br><b>Testcases Processed: </b>" + (countPassedTestcases + countFailedTestcases) + "</br>");
    	pwSummary.println("<br><b>Testcases Passed: </b>" + countPassedTestcases + "</br>");
    	pwSummary.println("<br><b>Testcases Failed: </b>" + countFailedTestcases + "</br>");
    	pwSummary.println("</table>");
    	pwSummary.println("</body>");
    	pwSummary.println("</html>");
    	pwSummary.close();
    }
    catch(Exception eX)
    {
    	LOGGER.debug("Writer already has been closed");
    }
    
  }

  @Override
  public void logStartTestSuite(TestSuite testSuite) throws Exception
  {
	// Create report testsuite
	LOGGER.debug("logStartTestSuite - TestRoot: " + testRoot);
	LOGGER.debug("TestsuiteID = " + testSuite.getId());
	String testSuiteSubDir = testSuite.getId().substring(testSuite.getId().lastIndexOf(File.separator)+1);
	
	//LOGGER.debug("Testsuite sub dir = " + testSuiteSubDir);  
  //LOGGER.debug("testroot = " + testRoot);
  try
  {
    if(testSuiteSubDir.substring(testSuiteSubDir.length() - 1) == File.separator)
      testSuiteSubDir = testSuiteSubDir.replaceAll(testRoot + File.separator, "");
    else
      testSuiteSubDir = testSuiteSubDir.replaceAll(testRoot, "");
  }
  catch(Exception ex) {}
	
	LOGGER.debug("logStartTestSuite - Test Suite sub dir: " + testSuiteSubDir);
	
	if(testSuiteSubDir.length() > 0)
	{
		currTestSuiteDir = reportRoot + File.separator + testSuiteSubDir;
	  LOGGER.debug("Write begin testsuite ({})", currTestSuiteDir);
	  new File(currTestSuiteDir).mkdirs();
	  
	  if (testSuite.numberOfTestCases(false) > 0)
		{
		  new File(reportRoot + File.separator + testSuiteSubDir).mkdirs();
			
		  pwTestsuite = createPW(	reportRoot +
				  				 	File.separator +
				  				 	testSuiteSubDir +
				  				 	File.separator +
				  				 	testSuiteSubDir +
				  					"_overall_result.html");
		  pwTestsuite.println("<!DOCTYPE html>");
		  pwTestsuite.println("<html>");
		  pwTestsuite.println("<head>");
		  pwTestsuite.println("<style> table, td { 	border: 1px solid black;	border-collapse: collapse;	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "th {	text-align: left;	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "tr:nth-child(odd) {	background: #CBCDCD;}"
		  							+ "p {	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "br {	font-family: Verdana, Helvetica, sans-serif;	font-size: 15px;}"
		  							+ "h1 {	font-family: Verdana, Helvetica, sans-serif;	font-size: 30px;}</style>");
		  pwTestsuite.println("<title>Prova Testreport</title>");
		  pwTestsuite.println("</head>");
		  pwTestsuite.println("<body>");	 
		  pwTestsuite.println("<h1>"+testSuite.getId().substring(testSuite.getId().lastIndexOf("\\")+1)+"</h1>");
		  startTimeTestsuite = System.currentTimeMillis();
		  pwTestsuite.println("<br><b>Starttime: </b>" + LocalDateTime.now() +"</br>");
		  pwTestsuite.println("<table>			<tr>				<th>Result</th><th>Testcase</th><th>Details</th></tr>");
		}	  
	}
  }

  @Override
  public void logEndTestSuite(TestSuite testSuite) throws Exception
  {
	// Create report testsuite
	  if (testSuite.numberOfTestCases(false) > 0)
		{
		  Long elapsedTime = System.currentTimeMillis() - startTimeTestsuite;
		  pwTestsuite.println("<br><b>Endtime: </b>" + LocalDateTime.now()+"</br>");
		  pwTestsuite.println("<br><b>Runtime in seconds: </b>" + elapsedTime/1000 + "</br>");
		  pwTestsuite.println("</table>");
		  pwTestsuite.println("</body>");
		  pwTestsuite.println("</html>");
		  pwTestsuite.close();
		}
    
  }
  private PrintWriter createPW(String name) throws Exception
  {
	  File file =new File(name);
	  if(!file.exists())
	  {
		  LOGGER.trace("Creating file: '" + file + "'");
		  file.createNewFile();
	  }
	  FileWriter fw = new FileWriter(file,true);
	  BufferedWriter bw = new BufferedWriter(fw);
	  return new PrintWriter(bw);
	  
  }
  private static void copyFileUsingChannel(File source, File dest) throws IOException {
	    FileChannel sourceChannel = null;
	    FileChannel destChannel = null;
	    try {
	        sourceChannel = new FileInputStream(source).getChannel();
	        destChannel = new FileOutputStream(dest).getChannel();
	        destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
	       }finally{
	           sourceChannel.close();
	           destChannel.close();
	       }
	}

  @Override
  public void storeToTxt(String text, String name) throws Exception
  {
    try
    {
      File folder = new File(reportRoot + File.separator + "txt" + File.separator);
      folder.mkdir();
      
      String file = reportRoot + File.separator + "txt" + File.separator + name + ".txt";
      PrintWriter printWriter = createPW(file);
      printWriter.println(text);
      printWriter.flush();
      printWriter.close();
      LOGGER.info("Stored message/query with name " + name + " to text file at '" + file + "'");
    }
    catch(Exception ex){
      LOGGER.error("Exception while writing message/query to txt file! : " + ex.getMessage());
    }
  }
	/**
	 * Convert a given filename as linktarget to a relative link
	 * 
	 * 
	 * @param linkTarget
	 * @return
	 */
	private String makeHtmlLinkRelative(String linkTarget) {
		if (linkTarget != null && !"".equals(linkTarget)) {
			try
			{
                LOGGER.trace("Poging 1: trying to replace " + currTestSuiteDir + " with " + this.testProject);
				linkTarget = linkTarget.replaceAll("/", Matcher.quoteReplacement(File.separator));
                //LOGGER.debug("Linktarget:  " + currTestSuiteDir );
			    return linkTarget.replaceFirst(currTestSuiteDir, this.testProject);
			}
			catch(Exception eX)
			{
				try
				{

				    LOGGER.trace("Poging 2: trying to replace " + currTestSuiteDir + " with " + this.testProject);
				    //linkTarget = linkTarget.replaceAll(":", "");
                    //LOGGER.debug("Linktarget:  " + currTestSuiteDir );
					return linkTarget.replace(currTestSuiteDir.replace("/", Matcher.quoteReplacement(File.separator)), this.testProject);
				}
				catch(Exception eX2)
				{
					LOGGER.warn("Creating relative link failed, using normal link instead: " + eX2);
					return linkTarget;
				}
			}
		}
		return linkTarget;
	}

	/**
	 * Replace all illegal characters in filename.
	 * 
	 * @param fileName
	 * @return
	 */
	public String makeFilenameValid(String fileName) {
		return fileName.replaceAll("[^a-zA-Z0-9.-]", "_").replaceAll("_+", "_");
	}
	public static void deleteFolder(File folder){
        LOGGER.trace("Deleting files in: " + folder.getAbsolutePath());
	    File[] files = folder.listFiles();
        if (files!=null)
        {
            for(File f: files)
            {
                if (f.isDirectory())
                {
                    deleteFolder(f);
                }
                else{
                    f.delete();
                }

            }
        }
    }
	

}
