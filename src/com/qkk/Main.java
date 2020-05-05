package com.qkk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Main {

    public static final String SS_PREFIX = "ss://";

    public static void main(String[] args) {
        // 将需要转换的 ss:// 链接放到 ssUrls.txt 中
        // 转换后的可订阅 json 在 result.json 中生成
        String json = ssUrlsToJson();
        if (json == null) {
            return;
        }
        writeResultToFile(json);
    }

    private static void writeResultToFile(String json) {
        try {
            FileWriter fileWriter = new FileWriter("result.json", StandardCharsets.UTF_8);
            fileWriter.write(json);
            close(fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String ssUrlsToJson() {
        BufferedReader bufferedReader = null;
        try {
            ServerConfigs serverConfigs = new ServerConfigs();
            bufferedReader = new BufferedReader(new FileReader("ssUrls.txt"));
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                ServerConfig serverConfig = ssUrl2ServerConfig(str);
                if (serverConfig != null) {
                    serverConfigs.serverConfigs.add(serverConfig);
                }
            }
            if (serverConfigs.serverConfigs.size() > 0) {
                String jsonResult = JSON.toJSONString(serverConfigs);
                System.out.println(jsonResult);
                return jsonResult;
            }
       } catch (IOException e) {
            e.printStackTrace();
       } finally {
            close(bufferedReader);
       }
       return null;
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ServerConfig ssUrl2ServerConfig(String ss) {
        if (ss == null || !ss.startsWith(SS_PREFIX)) {
            return null;
        }
        ServerConfig serverConfig = new ServerConfig();
        int start = SS_PREFIX.length();
        boolean containsPound = ss.contains("#");
        int end;
        if (containsPound) {
            end = ss.indexOf("#");
            serverConfig.remarks = ss.substring(end + 1);
            serverConfig.remarks = URLDecoder.decode(serverConfig.remarks, StandardCharsets.UTF_8);
        } else {
            end = ss.length();
        }
        String strWithOutPound = ss.substring(start, end);
        int indexOfAt = strWithOutPound.indexOf("@");
        // 如果不包含 @ 符号
        if (indexOfAt == -1) {
            byte[] decode = Base64.getDecoder().decode(strWithOutPound);
            String config = new String(decode);
            String[] configArray = config.split("@");
            if (configArray.length < 2) {
                return null;
            }
            String methodAndPassword = configArray[0];
            if (!parseMethodAndPassword(serverConfig, methodAndPassword)) return null;
            if (!parseServerAndPort(serverConfig, configArray[1])) return null;
        } else {
            String serverAndPort = strWithOutPound.substring(indexOfAt + 1);
            if (!parseServerAndPort(serverConfig, serverAndPort)) {
                return null;
            }
            byte[] decode = Base64.getDecoder().decode(strWithOutPound.substring(0, indexOfAt));
            String methodAndPassword = new String(decode);
            if (!parseMethodAndPassword(serverConfig, methodAndPassword)) {
                return null;
            }
        }
        System.out.println(serverConfig);
        return serverConfig;
    }

    private static boolean parseMethodAndPassword(ServerConfig serverConfig, String methodAndPassword) {
        int splitIndexOfMethodAndPassword = methodAndPassword.lastIndexOf(":");
        if (splitIndexOfMethodAndPassword == -1) {
            return false;
        }
        serverConfig.method = methodAndPassword.substring(0, splitIndexOfMethodAndPassword);
        serverConfig.password = methodAndPassword.substring(splitIndexOfMethodAndPassword + 1);
        return true;
    }

    private static boolean parseServerAndPort(ServerConfig serverConfig, String serverAndPort) {
        int splitIndexOfServerAndPort = serverAndPort.lastIndexOf(":");
        if (splitIndexOfServerAndPort == -1) {
            return false;
        }
        serverConfig.server = serverAndPort.substring(0, splitIndexOfServerAndPort);
        serverConfig.serverPort = serverAndPort.substring(splitIndexOfServerAndPort + 1);
        return true;
    }

    static class ServerConfigs {
        public int version = 1;
        public String remark = "";
        @JSONField(name = "servers")
        public List<ServerConfig> serverConfigs = new ArrayList<>();
    }

    static class ServerConfig {
        public String id = "";
        public String server = "";
        @JSONField(name = "server_port")
        public String serverPort = "";
        public String password = "";
        public String method = "";
        public String remarks = "";
        public String plugin = "";
        @JSONField(name = "plugin_opts")
        public String pluginOpts = "";

        @Override
        public String toString() {
            return "Server{" +
                    "id='" + id + '\'' +
                    ", server='" + server + '\'' +
                    ", serverPort='" + serverPort + '\'' +
                    ", password='" + password + '\'' +
                    ", method='" + method + '\'' +
                    ", remarks='" + remarks + '\'' +
                    ", plugin='" + plugin + '\'' +
                    ", pluginOpts='" + pluginOpts + '\'' +
                    '}';
        }
    }
}
