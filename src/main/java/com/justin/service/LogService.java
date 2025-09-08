package com.justin.service;

import com.justin.config.ConfigLoader;
import com.justin.ssh.ScpClient;
import com.justin.service.analyzer.LogAnalyzer;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;

public class LogService {

    public static void downloadLogAndAnalysis(JTextArea logArea, List<ConfigLoader.RemoteHost> selectedHosts, String localLogDir,
                                              String keyPathFile) throws InterruptedException {


        ExecutorService executor = Executors.newFixedThreadPool(5);

        // collect service names for later analysis
        List<String> services = new ArrayList<>();

        // download logs from each selected host in parallel
        for (ConfigLoader.RemoteHost host : selectedHosts) {
            services.add(host.getLogFileName());
            executor.submit(() -> {
                String remotePath = host.getLogDir() + "/" + host.getLogFileName();
                try {
                    ScpClient.scpFromRemote(host.getIp(), host.getPort(), host.getUser(), keyPathFile, remotePath, localLogDir);
                    SwingUtilities.invokeLater(() ->
                            logArea.append("已下載 " + remotePath + " from " + host.getIp() + "\n"));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            logArea.append("下載 " + remotePath + " 失敗: " + e.getMessage() + "\n"));
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.MINUTES);

        logArea.append("開始鏈式分析...\n");

        Set<String> prevTraceIds = null;
        List<Map<String, List<String>>> logsPerService = new ArrayList<>();
        String traceRegex = "traceId=(\\w+)";
        for (String service : services) {
            Map<String, List<String>> result = LogAnalyzer.analyzeServiceLogs("./logs/" + service, prevTraceIds, traceRegex);
            prevTraceIds = result.keySet();
            logsPerService.add(result);
        }

        List<String> mergedLogs = LogAnalyzer.mergeLogs(logsPerService);
        LogAnalyzer.writeFinalReport(mergedLogs, "./output/final_report.log");
        logArea.append("分析完成，報告生成於 ./output/final_report.log\n");
    }
}

