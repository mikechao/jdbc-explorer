package com.mike.chao.jdbc.explorer.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
public class BusinessInsights {

    private final List<String> insights;

    public BusinessInsights() {
        this.insights = Collections.synchronizedList(new ArrayList<>());
    }

    public void addInsight(String insight) {
        insights.add(insight);
    }

    public String getInsights() {
        if (insights.isEmpty()) {
            return "No insights yet";
        }
        var header = """
            📊 Business Intelligence Memo 📊
            Key insights discovered during data analysis:

            """;

        var text = IntStream.range(0, insights.size())
            .mapToObj(i -> (i + 1) + ". " + insights.get(i))
            .collect(Collectors.joining("\n"));
        return header  + text;
    }
}
