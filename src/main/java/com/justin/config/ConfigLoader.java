package com.justin.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConfigLoader {

    public static class RemoteHost {
        private String ip;
        private int port;
        private String user;
        private String logDir;
        private String logFileName;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getLogDir() {
            return logDir;
        }

        public void setLogDir(String logDir) {
            this.logDir = logDir;
        }

        public String getLogFileName() {
            return logFileName;
        }

        public void setLogFileName(String logFileName) {
            this.logFileName = logFileName;
        }

        // getters & setters ...
    }

    public static class RemoteHostsInfo {
        private String keyPathFile;
        private String localLogDir;
        private java.util.List<RemoteHost> remoteHosts;

        public String getKeyPathFile() {
            return keyPathFile;
        }

        public void setKeyPathFile(String keyPathFile) {
            this.keyPathFile = keyPathFile;
        }

        public String getLocalLogDir() {
            return localLogDir;
        }

        public void setLocalLogDir(String localLogDir) {
            this.localLogDir = localLogDir;
        }

        public List<RemoteHost> getRemoteHosts() {
            return remoteHosts;
        }

        public void setRemoteHosts(List<RemoteHost> remoteHosts) {
            this.remoteHosts = remoteHosts;
        }

        // getters & setters ...
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Path userConfigPath =
            Paths.get(System.getProperty("user.home"), ".logfetcher", "remoteHostsInfo.json");

    /**
     * 讀取設定：優先讀 user config，否則 fallback 到 resources
     */
    public static RemoteHostsInfo load() throws IOException {
        if (Files.exists(userConfigPath)) {
            return mapper.readValue(userConfigPath.toFile(), RemoteHostsInfo.class);
        } else {
            try (InputStream is = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("remoteHostsInfo.json")) {
                if (is == null) {
                    throw new IOException("remoteHostsInfo.json not found in resources");
                }
                return mapper.readValue(is, RemoteHostsInfo.class);
            }
        }
    }

    /**
     * 儲存設定到 user home
     */
    public static void save(RemoteHostsInfo config) throws IOException {
        Files.createDirectories(userConfigPath.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(userConfigPath.toFile(), config);
    }
}

