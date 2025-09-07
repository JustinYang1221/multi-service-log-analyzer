package com.justin;

import com.justin.ssh.ScpClient;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ScpClientTest {

    @Test
    void testScpFromRemote() {
        String host = "10.228.38.173";
        int port = 22; // 預設 SSH port
        String user = "centos"; // 或其他 Linux 帳號
        String keyPath = "/Users/justing/gts/gts2newkey.pem";
        String localLogDir = "/Users/justing/gts/logs";
        String remoteFile = "/log/RelatedAccount_uat/RelatedAccount_api_1.0.4_UAT.log";

        try {
            ScpClient.scpFromRemote(host, port, user, keyPath, remoteFile, localLogDir);

            File downloaded = new File(localLogDir, new File(remoteFile).getName());

            assertTrue(downloaded.exists(), "下載的檔案應該存在");
            assertTrue(downloaded.length() > 0, "下載的檔案不應該是空的");

        } catch (Exception e) {
            fail("SCP 過程失敗: " + e.getMessage());
        }
    }
}
