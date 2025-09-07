package com.justin.analyzer;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * @author: justin
 * @date: 2025/9/6
 */

public class LogAnalyzer {

    public static Map<String, List<String>> analyzeServiceLogs(String logDir, Set<String> filterTraceIds, String traceRegex) {
        Map<String, List<String>> traceLogMap = new HashMap<>();
        Pattern pattern = Pattern.compile(traceRegex);

        File dir = new File(logDir);
        for (File file : dir.listFiles()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String traceId = matcher.group(1);
                        if (filterTraceIds == null || filterTraceIds.contains(traceId)) {
                            traceLogMap.computeIfAbsent(traceId, k -> new ArrayList<>()).add(line);
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
        return traceLogMap;
    }

    public static List<String> mergeLogs(List<Map<String, List<String>>> logsPerService) {
        List<String> merged = new ArrayList<>();
        for (Map<String, List<String>> serviceLogs : logsPerService) {
            for (List<String> lines : serviceLogs.values()) {
                merged.addAll(lines);
            }
        }
        merged.sort(Comparator.comparing(line -> line.substring(0, 19))); // 假設 log 開頭時間
        return merged;
    }

    public static void writeFinalReport(List<String> mergedLogs, String outputFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line : mergedLogs) {
                bw.write(line);
                bw.newLine();
            }
            System.out.println("Final report generated at: " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

