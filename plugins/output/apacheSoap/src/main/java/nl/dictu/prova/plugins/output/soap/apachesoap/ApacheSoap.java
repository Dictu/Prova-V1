/**
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
package nl.dictu.prova.plugins.output.soap.apachesoap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;

import nl.dictu.prova.Config;
import nl.dictu.prova.TestRunner;
import nl.dictu.prova.framework.TestCase;
import nl.dictu.prova.plugins.output.SoapOutputPlugin;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.dictu.prova.framework.TestStatus;
import nl.dictu.prova.plugins.reporting.ReportingPlugin;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

public class ApacheSoap implements SoapOutputPlugin {

    final static Logger LOGGER = LogManager.getLogger();

    private TestRunner testRunner = null;
    private Integer maxRetries = 1;
    private long maxTimeOut = 1000; // milliseconds

    private HttpClient httpClient = null;
    private HttpResponse httpResponse = null;
    private StringEntity requestMessageEntity = null;
    private HttpPost post = null;
    private Integer status = null;

    private String currentAuthorization = null;
    private String currentMessage = null;
    private URI currentUrl = null;
    private String currentPrefix = null;
    private Map<String, String> headerMap = new LinkedHashMap<>();
    private TestCase testCase = null;
    private Boolean exceptionOnTest;

    @Override
    public String getName() {
        return "Soap messaging implementation";
    }

    @Override
    public void init(TestRunner testRunner) throws Exception {
        LOGGER.debug("Init: output plugin Soap messaging!");

        if (testRunner == null) {
            throw new Exception("No testRunner supplied!");
        }

        this.testRunner = testRunner;

//        maxTimeOut = Integer.valueOf(testRunner.getPropertyValue(Config.PROVA_TIMEOUT));
        maxTimeOut = Integer.parseInt(testRunner.getPropertyValue(Config.PROVA_TIMEOUT));
        if (maxTimeOut < 1000) {
            maxTimeOut = 1000;
        }

        maxRetries = Integer.valueOf(testRunner.getPropertyValue(Config.PROVA_PLUGINS_OUT_MAX_RETRIES));
        if (maxRetries < 0) {
            maxRetries = 0;
        }

        LOGGER.debug("Soap messaging initialized with timeout: {} ms, max retries: {}", maxTimeOut, maxRetries);
    }

    @Override
    public void shutDown() {
        try {
            //httpClient.close();
            //httpResponse.close();
        } catch (NullPointerException eX) {
            // Ignore. HttpClient already closed
        } catch (Exception eX) {
            LOGGER.error("Exception during shutDown: '{}'", eX.getMessage());
        }
    }

    @Override
    public void setUp(TestCase testCase) throws Exception {
      /**
       *  Get proxyserver settings from Config
       */

      String proxyHostname = null;
      String proxyPort = null;

      try {
        proxyHostname = testRunner.getPropertyValue(Config.PROVA_PLUGINS_OUTPUT_SOAP_PROXYHOST);
        proxyPort = testRunner.getPropertyValue(Config.PROVA_PLUGINS_OUTPUT_SOAP_PROXYPORT);
      } catch(Exception eX){
        proxyHostname = "";
        proxyPort = "";
        LOGGER.warn(eX);
      }

      /**
       * Create httpclient with proxy config if possible
       */

      try {
        if (proxyHostname == null || proxyHostname.equals("")) {
          httpClient = HttpClientBuilder.create().build();
        } else if (proxyPort == null || proxyPort.equals("")) {
          httpClient = HttpClientBuilder.create().build();
        } else {
          httpClient = HttpClients.custom().setProxy(new HttpHost(proxyHostname, Integer.parseInt(proxyPort))).build();
        }
      } catch (Exception eX){
        LOGGER.trace(eX);
      }

      this.testCase = testCase;
    }

    @Override
    public Properties doProcessResponse() throws Exception {
        post = new HttpPost();
        //Set headers
        post.addHeader("SOAPAction", "\"\"");
        post.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 6.0");
        post.addHeader("Host", this.currentUrl.toString());
        if (currentAuthorization != null) {
            post.addHeader("Authorization", "Basic " + currentAuthorization);
            LOGGER.trace("Encoded authorization is '" + currentAuthorization + "'.");
        }
        if (!headerMap.isEmpty()) {
            for (Entry e : headerMap.entrySet()) {
                LOGGER.debug("Adding header: " + e.getKey().toString() + " with value " + e.getValue().toString() + " to the request");
                post.addHeader(e.getKey().toString(), e.getValue().toString());
            }
        }

        //Set soap message
        while (containsKeywords(currentMessage)) {
            LOGGER.trace("Found keyword in SOAP message, replacing it with corresponding value.");
            String editedString = replaceKeywords(currentMessage);
            if (editedString == null) {
                break;
            } else {
                currentMessage = editedString;
            }
        }
      try {
        requestMessageEntity = new StringEntity(currentMessage);
        requestMessageEntity.setContentType("text/xml;charset=UTF-8");
        post.setEntity(requestMessageEntity);

        //Set target
        post.setURI(currentUrl);

        //Send it
        httpResponse = httpClient.execute(post);
        status = httpResponse.getStatusLine().getStatusCode();
      } catch (Exception ex) {
        LOGGER.trace(ex);
      }
        if (status != 200) {
            LOGGER.error("\n********************\n*\n*   Error: Message with prefix " + currentPrefix + " has been sent unsuccesfully.\n*\n********************");

            //Convert response to string
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }

            //Log the result
            for (ReportingPlugin plugin : this.testRunner.getReportingPlugins()) {
                plugin.storeToTxt(currentMessage, currentPrefix + "_request");
                plugin.storeToTxt("" + result, currentPrefix + "_response");
            }
            LOGGER.debug("Response body   : \n" + result);

            testCase.setStatus(TestStatus.FAILED);
            cleanUp();
            return new Properties();
        } else {
            LOGGER.info("\n********************\n*\n*   Message with prefix " + currentPrefix + " has been sent succesfully.\n*\n********************");

            //Convert response to string
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }

            String log = "";
            for (Header header : post.getAllHeaders()) {
                log += header + ", \n";
            }

            //Log the result
            for (ReportingPlugin plugin : this.testRunner.getReportingPlugins()) {
                log += currentMessage;
                plugin.storeToTxt(log, currentPrefix + "_request");
                plugin.storeToTxt("" + result, currentPrefix + "_response");
            }
            LOGGER.debug("Response body   : \n" + result);

            cleanUp();
            return splitSoapMessage(result.toString());
        }
    }

    @Override
    public void doSetProperties(Map<String, String> propMap, String prefix) throws Exception {
        try {
            for (Entry e : propMap.entrySet()) {
                String key = e.getKey().toString();
                LOGGER.debug("SetProperties key: " + key);
                if (key.equalsIgnoreCase("user") | key.equalsIgnoreCase("pass")) {
                    LOGGER.trace("Encoding the authorization");
                    String authorization = propMap.get("user") + ":" + propMap.get("pass");
                    byte[] encodedAuthorizationBytes = Base64.encode(authorization.getBytes());
                    currentAuthorization = new String(encodedAuthorizationBytes);
                } else if (key.equalsIgnoreCase("url")) {
                    this.currentUrl = new URI(e.getValue().toString());
                } else {
                    LOGGER.trace("Putting header: " + key + " with value: " + propMap.get(key) + " in headerMap");
                    this.headerMap.put(key, propMap.get(key));
                    LOGGER.trace("Header: " + key + " added to map");
                }
            }
        } catch (Exception ex) {
            throw new Exception("doSetProperties error: " + ex);
        }

        //Prefix
        if (prefix != null) {
            LOGGER.debug("Setting currentPrefix: " + prefix);
            this.currentPrefix = prefix;
        } else {
            throw new Exception("Supplied prefix is null!");
        }
    }

    @Override
    public void doSetMessage(String message) throws Exception {
        //Message
        if (message != null) {
            currentMessage = message;
        }
    }

    @Override
    public boolean doTest(String property, String test) throws Exception {
        LOGGER.trace("Executing test for property '" + property + "' with validation '" + test + "'");

        try {
            if (exceptionOnTest == null) {
                if (testRunner.hasPropertyValue(Config.PROVA_FLOW_FAILON_TESTFAIL)) {
                    exceptionOnTest = Config.PROVA_FLOW_FAILON_TESTFAIL.equalsIgnoreCase("true");
                }
            }

            if (test.equalsIgnoreCase("{null}")) {
                if (testRunner.hasPropertyValue(property)) {
                    if (testRunner.getPropertyValue(property).trim().length() > 0) {
                        if (exceptionOnTest) {
                            throw new Exception("Test unsuccessful! Property is not null.");
                        } else {
                            LOGGER.info("Test unsuccessful! Property is not null.");
                            return false;
                        }
                    } else {
                        LOGGER.info("Test successful!");
                        return true;
                    }
                }
            }

            if (testRunner.hasPropertyValue(property) & testRunner.getPropertyValue(property) != null & testRunner.getPropertyValue(property).trim().length() > 0) {
                String propertyValue = testRunner.getPropertyValue(property).trim();
                if (propertyValue.equalsIgnoreCase(test.trim())) {
                    LOGGER.info("Test successful!");
                    return true;
                }
                if (exceptionOnTest) {
                    throw new Exception("Test unsuccessful!  Value is '" + propertyValue + "' instead of '" + test.trim() + "'");
                } else {
                    LOGGER.error("Test unsuccessful!  Value is '{}' instead of '{}'", propertyValue, test.trim());
                    return false;
                }
            } else {
                if (exceptionOnTest) {
                    throw new Exception("Test unsuccessful! Property doesn't exist.");
                } else {
                    LOGGER.info("Test unsuccessful! Property doesn't exist.");
                    return false;
                }
            }
        } catch (Exception eX) {
            if (exceptionOnTest == null) {
                if (testRunner.hasPropertyValue(Config.PROVA_FLOW_FAILON_TESTFAIL)) {
                    if (testRunner.getPropertyValue(Config.PROVA_FLOW_FAILON_TESTFAIL).trim().equalsIgnoreCase("true")) {
                        throw new Exception("Failed to run test! Exception occured." + eX.getMessage());
                    }
                }
            }

            LOGGER.info("Test unsuccesful! Exception occured." + eX.getMessage());
            return false;
        }
    }

    @Override
    public String doGetCurrentPrefix() throws Exception {
        return currentPrefix;
    }

    public void cleanUp() {
        LOGGER.debug("Cleaning Soap variables.");
        currentAuthorization = null;
        currentMessage = null;
        currentUrl = null;
        currentPrefix = null;
        headerMap = null;
    }

    private Boolean containsKeywords(String entry) throws Exception {
        Pattern pattern = Pattern.compile("\\{[A-Za-z0-9._]+\\}");
        Matcher matcher = pattern.matcher(entry);

        while (matcher.find()) {
            return true;
        }
        return false;
    }

    private String replaceKeywords(String entry) throws Exception {
        Pattern pattern = Pattern.compile("\\{[A-Za-z0-9._]+\\}");
        Matcher matcher = pattern.matcher(entry);
        StringBuffer entryBuffer = new StringBuffer("");

        while (matcher.find()) {
            String keyword = matcher.group(0).substring(1, matcher.group(0).length() - 1);

            LOGGER.trace("Found keyword " + matcher.group(0) + " in supplied string.");

            Boolean failOnNoTestdataKeywords = false;

            try {
                matcher.appendReplacement(entryBuffer, testRunner.getPropertyValue(keyword));

                try {
                    failOnNoTestdataKeywords = Boolean.parseBoolean(this.testRunner.getPropertyValue(Config.PROVA_FLOW_FAILON_NOTESTDATAKEYWORD));
                } catch (Exception ex) {
                    LOGGER.error("Error parsing property '{}', please check your property file.", Config.PROVA_FLOW_FAILON_NOTESTDATAKEYWORD);
                }
            } catch (Exception ex) {
                if (failOnNoTestdataKeywords) {
                    throw new Exception("Keyword '" + keyword + "' in '" + currentPrefix + "' not defined with a value.");
                } else {
                    matcher.appendReplacement(entryBuffer, keyword);
                    LOGGER.error("Keyword '" + keyword + "' in '" + currentPrefix + "' not defined with a value.");
                }
            }
        }
        matcher.appendTail(entryBuffer);

        return entryBuffer.toString();
    }

    public Properties splitSoapMessage(String message) {
        Properties result = new Properties();

        //Pattern standalonePattern = Pattern.compile("\\<[A-Za-z0-9:]+\\/\\>");
        Pattern variablePattern = Pattern.compile("\\<[A-Za-z0-9 :\"=/_.]+\\>[^<>]+\\<\\/[A-Za-z0-9:]+\\>");
        //Pattern variableXmlnsPattern = Pattern.compile("\\<[A-Za-z0-9:]+\\>[^<>]+\\<\\/[A-Za-z0-9:]+\\>");
        Pattern variableName = Pattern.compile("\\:(.+?)\\>");
        Pattern variable = Pattern.compile("\\>(.+?)\\<");
        ArrayList<String> matches = new ArrayList<String>();

        Matcher variablePatternMatcher = variablePattern.matcher(message);

        while (variablePatternMatcher.find()) {
            String match = variablePatternMatcher.group(0);
            matches.add(match);
        }
        for (String string : matches) {
            Matcher variableMatcher = variable.matcher(string);

            String key = null;
            String value = null;

            while (variableMatcher.find()) {
                value = variableMatcher.group(1);
            }
            string = string.replaceFirst(value, "");

            Matcher variableNameMatcher = variableName.matcher(string);
            while (variableNameMatcher.find()) {
                key = variableNameMatcher.group(1);
            }
            //System.out.println("Key : " + key + ", value : " + value);
            if (key != null) {
                result.put(key, value);
            }
        }
        //TODO: standalone elementen toevoegen
        //result.put("authorization", currentAuthorization);

        return result;
    }

    public void runTests(Properties splitResponse, Properties testCases) {
        Properties nullChecks = new Properties();

        //Check if element has expected value
        for (Entry entry : testCases.entrySet()) {
            if (entry.getValue().equals("{null}")) {
                nullChecks.put(entry.getKey(), "");
                testCases.remove(entry.getKey());
                continue;
            }
            if (splitResponse.containsKey(entry.getKey())) {
                String value = (String) entry.getValue();
                if (splitResponse.getProperty((String) entry.getKey()).trim().toLowerCase().equals(value.trim().toLowerCase())) {
                    LOGGER.info("Test for element " + entry.getKey() + " succeeded, element has expected value.");
                    if (testCase.getStatus() != TestStatus.FAILED) {
                        testCase.setStatus(TestStatus.PASSED);
                    }
                }
            } else {
                LOGGER.info("Test for element " + entry.getKey() + " failed, could not find specified element in response.");
                testCase.setStatus(TestStatus.FAILED);
            }
        }

        //Check if element is null
        if (!nullChecks.entrySet().isEmpty()) {
            for (Entry entry : nullChecks.entrySet()) {
                if (splitResponse.containsKey(entry.getKey())) {
                    if (splitResponse.getProperty((String) entry.getKey()) == null) {
                        LOGGER.info("Test for element " + entry.getKey() + " succeeded, element is null.");
                        if (testCase.getStatus() != TestStatus.FAILED) {
                            testCase.setStatus(TestStatus.PASSED);
                        }
                    }
                    //TODO: Indien niet voorkomt moet ook goed zijn.
                } else {
                    LOGGER.info("Test for element " + entry.getKey() + " failed, could not find specified element in response.");
                    testCase.setStatus(TestStatus.FAILED);
                }
            }
        }
    }
}
