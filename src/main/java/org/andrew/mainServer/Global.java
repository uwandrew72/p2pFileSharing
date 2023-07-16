package org.andrew.mainServer;

import java.util.*;

public class Global {
    public static Map<String, Integer> peerMap = new HashMap<>();
    public static int searchPort;
    public static Map<String, String[]> pathMap = new HashMap<>();
    public static String contentList;
    public static int frontPort;
    public static int backPort;
    public static String hostName = "localhost";
}
