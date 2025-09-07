package com.justin.ssh;
import com.jcraft.jsch.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: justin
 * @date: 2025/9/6
 */



public class ScpClient {

    /**
     * 透過 SCP 從遠端伺服器下載檔案。
     *
     * @param host       遠端主機 IP
     * @param port       SSH 埠號
     * @param user       SSH 使用者名稱
     * @param pemFilePath PEM 身份驗證檔案路徑
     * @param remoteFile 遠端檔案的完整路徑
     * @param localDir   本地端下載目錄
     * @throws Exception 如果連線或傳輸過程中出現錯誤
     */
    public static void scpFromRemote(String host, int port, String user, String pemFilePath, String remoteFile, String localDir) throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(pemFilePath);

        Session session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        System.out.println("成功連線到: " + host);

        String command = "scp -f " + remoteFile;

        // 建立 SCP 傳輸頻道
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();
        channel.connect();

        // SCP 協定握手
        // 發送空位元組以請求檔案
        out.write(0);
        out.flush();

        // 讀取伺服器發送的檔案頭訊息
        // 格式通常為 "C<權限> <檔案大小> <檔名>"
        // 例如：C0644 123456 filename.txt
        byte[] buf = new byte[1024];
        int len = in.read(buf, 0, 1024);
        String header = new String(buf, 0, len);

        // 檢查伺服器回應是否為錯誤
        if (header.charAt(0) != 'C') {
            throw new IOException("SCP 協定錯誤，無法讀取檔案頭: " + header);
        }

        // 從檔案頭解析檔案大小和名稱
        Pattern pattern = Pattern.compile("C\\d+ (\\d+) (.*)");
        Matcher matcher = pattern.matcher(header);
        if (!matcher.find()) {
            throw new IOException("無法從檔案頭解析檔案大小和名稱");
        }
        long fileSize = Long.parseLong(matcher.group(1));
        String fileName = matcher.group(2);

        System.out.println("檔案頭訊息: " + header.trim());
        System.out.println("檔案名稱: " + fileName);
        System.out.println("檔案大小: " + fileSize + " 位元組");

        File localFile = new File(localDir, fileName);
        new File(localDir).mkdirs();

        // 發送空位元組確認收到檔案頭，可以開始傳輸內容
        out.write(0);
        out.flush();

        long totalRead = 0;
        // 開始下載檔案內容
        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            while (totalRead < fileSize) {
                len = in.read(buf, 0, (int) Math.min(buf.length, fileSize - totalRead));
                if (len < 0) {
                    throw new IOException("檔案傳輸中斷，未讀取完整檔案。");
                }
                fos.write(buf, 0, len);
                totalRead += len;
            }
        }
        System.out.println("檔案內容下載完成，總計讀取 " + totalRead + " 位元組。");

        // 讀取伺服器發送的最終成功確認 (空位元組)
        in.read(buf, 0, 1);
        if (buf[0] != 0) {
            throw new IOException("SCP 協定錯誤，未收到最終成功確認。");
        }

        // 發送最終成功確認給伺服器
        out.write(0);
        out.flush();

        // 關閉資源
        channel.disconnect();
        session.disconnect();
        System.out.println("成功透過 SCP 下載: " + remoteFile + " 到 " + localFile.getAbsolutePath());
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while (true) {
            c = in.read();
            if (c == '\n') break;
            sb.append((char) c);
        }
        return sb.toString();
    }


}

