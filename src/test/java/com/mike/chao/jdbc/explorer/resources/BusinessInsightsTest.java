package com.mike.chao.jdbc.explorer.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BusinessInsightsTest {

    private BusinessInsights businessInsights;

    @BeforeEach
    void setUp() {
        businessInsights = new BusinessInsights();
    }

    @Test
    void testGetInsightsWhenEmpty() {
        assertEquals("No insights yet", businessInsights.getInsights());
    }

    @Test
    void testAddNullInsight() {
        businessInsights.addInsight(null);
        assertEquals("No insights yet", businessInsights.getInsights());
    }

    @Test
    void testAddBlankInsight() {
        businessInsights.addInsight("   ");
        assertEquals("No insights yet", businessInsights.getInsights());
    }

    @Test
    void testAddValidInsight() {
        businessInsights.addInsight("Insight 1");
        String result = businessInsights.getInsights();
        assertTrue(result.contains("1. Insight 1"));
        assertTrue(result.startsWith("📊 Business Intelligence Memo 📊"));
    }

    @Test
    void testAddMultipleInsights() {
        businessInsights.addInsight("First");
        businessInsights.addInsight("Second");
        businessInsights.addInsight("Third");
        String result = businessInsights.getInsights();
        assertTrue(result.contains("1. First"));
        assertTrue(result.contains("2. Second"));
        assertTrue(result.contains("3. Third"));
        assertTrue(result.startsWith("📊 Business Intelligence Memo 📊"));
    }
}
