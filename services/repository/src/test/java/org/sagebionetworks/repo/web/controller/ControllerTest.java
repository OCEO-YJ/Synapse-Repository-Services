/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlPrefixes;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Suite for tests not specific to any particular model.<p>  
 * 
 * Most of the conditions they test occur either before we enter the controller code or 
 * after we leave it.  Therefore they do not need to be implemented for each 
 * specific controller because the tests don't enter code paths that differ for each.
 * 
 * @author deflaux
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:repository-context.xml","classpath:repository-servlet.xml"})
public class ControllerTest {

    private static final Logger log = Logger.getLogger(DatasetControllerTest.class.getName());
    private Helpers helper = new Helpers();
    private DispatcherServlet servlet;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        servlet = helper.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }
    

    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity(Object, HttpServletRequest)}.
     * @throws Exception 
     */
    @Test
    public void testInvalidJsonCreateEntity() throws Exception {
        
        Collection<String> urls = UrlPrefixes.getAllUrlPrefixes();
        for(String url : urls) {
            
            // Notice the missing quotes around the key
            JSONObject results = helper.testCreateJsonEntityShouldFail("/dataset",
                    "{name:\"bad json from a unit test\"}",
                    HttpStatus.BAD_REQUEST);

            // The response should be something like:  {"reason":"Could not read JSON: Unexpected character
            // ('t' (code 116)): was expecting double-quote to start field name\n at [Source:
            // org.springframework.mock.web.DelegatingServletInputStream@11e3c2c6; line: 1, column: 3];
            // nested exception is org.codehaus.jackson.JsonParseException: Unexpected character
            // ('n' (code 116)): was expecting double-quote to start field name\n at [Source:
            // org.springframework.mock.web.DelegatingServletInputStream@11e3c2c6; line: 1, column: 3]"}
            assertTrue("Testing " + url,
                    results.getString("reason").startsWith("Could not read JSON: Unexpected character"));
        }
    }
    
    
    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity(Object, HttpServletRequest)}.
     * @throws Exception
     */
    @Test
    public void testMissingBodyCreateEntity() throws Exception {
        
        Collection<String> urls = UrlPrefixes.getAllUrlPrefixes();
        for(String url : urls) {
        
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setMethod("POST");
            request.addHeader("Accept", "application/json");
            request.setRequestURI(url);
            request.addHeader("Content-Type", "application/json; charset=UTF-8");
            // No call to request.setContent()
            servlet.service(request, response);
            log.info("Results: " + response.getContentAsString());
            assertEquals("Testing " + url,
                    HttpStatus.BAD_REQUEST.value(), response.getStatus());
            JSONObject results = new JSONObject(response.getContentAsString());
            // The response should be something like: {"reason":"No content to map to Object due to end of input"}
            assertEquals("Testing " + url,
                    "No content to map to Object due to end of input", results.getString("reason"));
        }
    }
    
    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity(Object, HttpServletRequest)}.
     * @throws Exception
     */
    @Test
    public void testMissingBodyUpdateEntity() throws Exception {
        
        Collection<String> urls = UrlPrefixes.getAllUrlPrefixes();
        for(String url : urls) {
        
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setMethod("PUT");
            request.addHeader("Accept", "application/json");
            request.setRequestURI(url + "/1");
            request.addHeader("Content-Type", "application/json; charset=UTF-8");
            request.addHeader(ServiceConstants.ETAG_HEADER, "123");
            // No call to request.setContent()
            servlet.service(request, response);
            log.info("Results: " + response.getContentAsString());
            assertEquals("Testing " + url,
                    HttpStatus.BAD_REQUEST.value(), response.getStatus());
            JSONObject results = new JSONObject(response.getContentAsString());
            // The response should be something like: {"reason":"No content to map to Object due to end of input"}
            assertEquals("Testing " + url,
                    "No content to map to Object due to end of input", results.getString("reason"));
        }
    }
    /**
     * Test method for {@link org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity(String, Integer, Object, javax.servlet.http.HttpServletRequest)}.
     * @throws Exception
     */
    @Test
    public void testUpdateEntityMissingEtag() throws Exception {
        Collection<String> urls = UrlPrefixes.getAllUrlPrefixes();
        for(String url : urls) {

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setMethod("PUT");
            request.addHeader("Accept", "application/json");
            request.setRequestURI(url + "/1");
            request.addHeader("Content-Type", "application/json; charset=UTF-8");
            request.setContent("{\"id\": 1, \"text\":\"updated dataset from a unit test\"}".getBytes("UTF-8"));
            servlet.service(request, response);
            log.info("Results: " + response.getContentAsString());
            assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
            JSONObject results = new JSONObject(response.getContentAsString());
            // The response should be something like:  {"reason":"Failed to invoke handler method [public
            // org.sagebionetworks.repo.model.Dataset org.sagebionetworks.repo.web.controller.DatasetController.updateDataset
            // (java.lang.Long,java.lang.String,org.sagebionetworks.repo.model.Dataset)
            // throws org.sagebionetworks.repo.web.NotFoundException]; nested exception is java.lang.IllegalStateException:
            // Missing header 'Etag' of type [java.lang.String]"}
            assertNotNull(results.getString("reason"));

            assertTrue(results.getString("reason").matches("(?s).*Missing header 'ETag'.*"));
        }
    }
}
