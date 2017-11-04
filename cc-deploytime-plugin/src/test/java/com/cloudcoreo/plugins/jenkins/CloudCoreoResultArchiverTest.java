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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class CloudCoreoResultArchiverTest {

    private CloudCoreoResultArchiver archiver;
    private List<ContextTestResult> results;

    @Before
    public void setUp() throws Exception {
        JSONArray jsonArray = ContextTestResultTest.getObjectArrayJson();
        JSONObject jsonObject = ContextTestResultTest.getObjectJson();
        JSONObject realObjectJson = ContextTestResultTest.getRealObjectJson();
        results = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            results.add(new ContextTestResult(jsonArray.getJSONObject(i)));
        }
        results.add(new ContextTestResult(jsonObject));
        results.add(new ContextTestResult(realObjectJson));
        archiver = new CloudCoreoResultArchiver(true, true, true);
    }

    @Test
    public void shouldWriteResultsToHTML() throws Exception {
        File file = new File(new URI("file:///tmp"));
        archiver.writeResultsHtml(new FilePath(file), "unittest", results);
        Document doc = Jsoup.parse(archiver.getResultsHtml());

        Assert.assertEquals(51, doc.getAllElements().size());
    }

}