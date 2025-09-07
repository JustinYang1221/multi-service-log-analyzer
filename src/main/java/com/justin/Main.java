package com.justin;

import com.justin.ssh.ScpClient;
import com.justin.analyzer.LogAnalyzer;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    public static void runAnalysis(String pemPath, JTextArea logArea) throws InterruptedException {
        String[] services = {"A","B","C"};
        Map<String,List<String>> servers = Map.of(
                "A", List.of("192.168.1.101"),
                "B", List.of("192.168.1.201"),
                "C", List.of("192.168.1.301")
        );

        ExecutorService executor = Executors.newFixedThreadPool(5);

        for(String service : services) {
            for(String host : servers.get(service)) {
                executor.submit(() -> {
                    try {
                        logArea.append("下載 "+service+" log 從 "+host+"\n");
                        ScpClient.scpFromRemote(host, 22, "user", pemPath, "/var/log/"+service+".log", "./logs/"+service);
                    } catch(Exception e) {
                        logArea.append("錯誤: " + e.getMessage() + "\n");
                    }
                });
            }
        }
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.MINUTES);

        logArea.append("開始鏈式分析...\n");

        Set<String> prevTraceIds = null;
        List<Map<String,List<String>>> logsPerService = new ArrayList<>();
        String traceRegex = "traceId=(\\w+)";
        for(String service: services){
            Map<String,List<String>> result = LogAnalyzer.analyzeServiceLogs("./logs/"+service, prevTraceIds, traceRegex);
            prevTraceIds = result.keySet();
            logsPerService.add(result);
        }

        List<String> mergedLogs = LogAnalyzer.mergeLogs(logsPerService);
        LogAnalyzer.writeFinalReport(mergedLogs, "./output/final_report.log");
        logArea.append("分析完成，報告生成於 ./output/final_report.log\n");
    }
}

