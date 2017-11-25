package com.cloudcoreo.plugins.jenkins;

import hudson.FilePath;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class CloudCoreoResultArchiverTest {

    private CloudCoreoResultArchiver archiver;
    private List<ContextTestResult> results;
    private PrintStream logger;

    private class CloudCoreoResultArchiverStub extends CloudCoreoResultArchiver {

        CloudCoreoResultArchiverStub(boolean blockOnHigh, boolean blockOnMedium, boolean blockOnLow) {
            super(blockOnHigh, blockOnMedium, blockOnLow);
        }

        @Override
        List<ContextTestResult> getRunResults() {
            return results;
        }

        CloudCoreoTeam getTeam() {
            return new CloudCoreoTeam(CloudCoreoTeamTest.SETUP_PARAMS);
        }
    }

    @Before
    public void setUpResultsAndArchiver() throws Exception {
        JSONArray jsonArray = ContextTestResultTest.getObjectArrayJson();
        JSONObject jsonObject = ContextTestResultTest.getObjectJson();
        JSONObject realObjectJson = ContextTestResultTest.getRealObjectJson();
        results = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            results.add(new ContextTestResult(jsonArray.getJSONObject(i)));
        }
        results.add(new ContextTestResult(jsonObject));
        results.add(new ContextTestResult(realObjectJson));
        archiver = new CloudCoreoResultArchiverStub(false, false, true);
        logger = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
        });
    }

    @Test
    public void shouldWriteResultsToHTML() throws Exception {
        File file = new File(new URI("file:///tmp"));
        archiver.writeResultsToFile(new FilePath(file), "unittest");
        Document doc = Jsoup.parse(archiver.getResultsHtml().toString());

        Assert.assertEquals(51, doc.getAllElements().size());
    }

    @Test
    public void shouldHaveBlockingFailures() {
        Assert.assertTrue(archiver.hasBlockingFailures());
    }

    @Test
    public void shouldOutputResults() {
        archiver.reportResults(logger);
        Assert.assertFalse(logger.checkError());
    }

    @Test
    public void descriptorShouldHaveNameAndIsApplicable() {
        CloudCoreoResultArchiver.DescriptorImpl descriptor = new CloudCoreoResultArchiver.DescriptorImpl();
        Assert.assertNotNull(descriptor.getDisplayName());
        Assert.assertTrue(descriptor.isApplicable(null));
    }

}