package com.justin.ui;


import com.justin.config.ConfigLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainUI extends JFrame {
    private ConfigLoader.RemoteHostsInfo config;
    private JTable hostTable;
    private JTextField keyPathField;
    private JTextField localLogDirField;

    private JTextArea logArea;
    private JButton startButton;
    public MainUI() throws IOException {
        setTitle("Remote Log Fetcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 400);
        setLayout(new BorderLayout());

        // Load config
        config = ConfigLoader.load();

        // Top panel
        JPanel topPanel = new JPanel(new GridLayout(2, 2));
        topPanel.add(new JLabel("Key Path:"));
        keyPathField = new JTextField(config.getKeyPathFile());
        topPanel.add(keyPathField);

        topPanel.add(new JLabel("Local Log Dir:"));
        localLogDirField = new JTextField(config.getLocalLogDir());
        topPanel.add(localLogDirField);

        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"Select", "IP", "Port", "User", "Log Dir", "Log File Name"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class; // 第一欄為 checkbox
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column >= 3; // 允許勾選 & 編輯 logDir/logFileName
            }
        };
        // 塞入資料
        for (ConfigLoader.RemoteHost host : config.getRemoteHosts()) {
            tableModel.addRow(new Object[]{
                    false, // 預設不勾選
                    host.getIp(), host.getPort(), host.getUser(),
                    host.getLogDir(), host.getLogFileName()
            });
        }

        hostTable = new JTable(tableModel);
        add(new JScrollPane(hostTable), BorderLayout.CENTER);

        //logArea
        logArea = new JTextArea();
        logArea.setEditable(false);
        startButton = new JButton("開始分析");
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Config", new JScrollPane(hostTable));
        tabbedPane.addTab("Logs", new JScrollPane(logArea));

        // 把 tabbedPane 放到 CENTER
        add(tabbedPane, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        JButton saveBtn = new JButton("Save Config");

        saveBtn.addActionListener(e -> {
            try {
                saveConfig();
                JOptionPane.showMessageDialog(this, "Configuration saved!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + ex.getMessage());
            }
        });

        startButton.addActionListener(e -> {
            logArea.append("開始分析...\n");

            DefaultTableModel model = (DefaultTableModel) hostTable.getModel();
            java.util.List<ConfigLoader.RemoteHost> selectedHosts = new java.util.ArrayList<>();

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddHHmmss");
            String nowStr = now.format(formatter);
            for (int i = 0; i < model.getRowCount(); i++) {
                Boolean isSelected = (Boolean) model.getValueAt(i, 0);
                if (isSelected != null && isSelected) {
                    ConfigLoader.RemoteHost host = new ConfigLoader.RemoteHost();
                    host.setIp((String) model.getValueAt(i, 1));
                    host.setPort(Integer.parseInt(model.getValueAt(i, 2).toString()));
                    host.setUser((String) model.getValueAt(i, 3));
                    host.setLogDir((String) model.getValueAt(i, 4));
                    host.setLogFileName((String) model.getValueAt(i, 5)+ "_" + nowStr); // 加上時間戳
                    selectedHosts.add(host);
                }
            }

            if (selectedHosts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "請至少選擇一個 Host");
                return;
            }

            // TODO: 呼叫你的 MultiServiceLogAnalyzer, 帶入 selectedHosts
            for (ConfigLoader.RemoteHost host : selectedHosts) {
                logArea.append("分析 " + host.getIp() + ":" + host.getPort() + " ...\n");
                // 假設這裡呼叫 ScpClient 抓 log，並執行分析
                // String result = analyzer.processHostLogs(host);
                // logArea.append(result + "\n");
            }

            logArea.append("分析完成 ✅\n");
        });

        bottomPanel.add(saveBtn);
        bottomPanel.add(startButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void saveConfig() throws IOException {
        config.setKeyPathFile(keyPathField.getText());
        config.setLocalLogDir(localLogDirField.getText());

        DefaultTableModel model = (DefaultTableModel) hostTable.getModel();
        config.getRemoteHosts().clear();
        for (int i = 0; i < model.getRowCount(); i++) {
            ConfigLoader.RemoteHost host = new ConfigLoader.RemoteHost();
            host.setIp((String) model.getValueAt(i, 0));
            host.setPort(Integer.parseInt(model.getValueAt(i, 1).toString()));
            host.setUser((String) model.getValueAt(i, 2));
            host.setLogDir((String) model.getValueAt(i, 3));
            host.setLogFileName((String) model.getValueAt(i, 4));
            config.getRemoteHosts().add(host);
        }

        ConfigLoader.save(config);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainUI().setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}


