package com.stratio.decision.service;

import com.stratio.decision.configuration.StreamingSiddhiConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.SiddhiManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by aitor on 9/22/15.
 */
public class StreamMetadataServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamMetadataServiceTest.class);

    private SiddhiManager siddhiManager;

    private StreamMetadataService metadataService;

    @Before
    public void setUp() throws Exception {
        LOGGER.debug("Initializing required classes");
        siddhiManager= new StreamingSiddhiConfiguration().siddhiManager();

        siddhiManager.defineStream(StreamsHelper.STREAM_DEFINITION);
        metadataService= new StreamMetadataService(siddhiManager);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testGetAttributePosition() throws Exception {
        assertEquals("Expected value not found", 0,
                metadataService.getAttributePosition(StreamsHelper.STREAM_NAME, "name"));
        assertEquals("Expected value not found", 1,
                metadataService.getAttributePosition(StreamsHelper.STREAM_NAME, "timestamp"));
        assertEquals("Expected value not found", 2,
                metadataService.getAttributePosition(StreamsHelper.STREAM_NAME, "value"));
    }

    @Test
    public void testGetAttribute() throws Exception {
        assertEquals("Expected value not found", "name",
                metadataService.getAttribute(StreamsHelper.STREAM_NAME, 0).getName());
        assertEquals("Expected value not found", "timestamp",
                metadataService.getAttribute(StreamsHelper.STREAM_NAME, 1).getName());
        assertEquals("Expected value not found", "value",
                metadataService.getAttribute(StreamsHelper.STREAM_NAME, 2).getName());

    }

    @Test
    public void testGetSnapshot() throws Exception {
        byte[] snapshot= metadataService.getSnapshot();
        assertTrue("Expected true value not found", snapshot.length > 0);
    }

}