package org.andrew.mainServer;

import java.io.*;
import java.net.*;

public class Searcher extends Thread {
    @Override
    public void run() {
        try {
            // Create a socket to listen on the port.
            DatagramSocket dsocket = new DatagramSocket(Global.searchPort);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            while (true) {
                dsocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.startsWith("Master")) {
                    String masterHost = packet.getAddress().getHostAddress();
                    int masterPort = packet.getPort();
                    String searchMsg = ("Search:" + msg.split("Master")[1]);
                    for (String s: Global.peerMap.keySet()) {
                        dsocket.send(new DatagramPacket(searchMsg.getBytes(), searchMsg.length(), InetAddress.getByName(s), Global.peerMap.get(s)));
                        // set timeout
                      //  dsocket.setSoTimeout(5000);
                        try {
                            dsocket.receive(packet);
                            String result = new String(packet.getData(), 0, packet.getLength());
                            String[] resultArray = result.split("&");
                            String[] pathArray = resultArray[1].split(",");
                            Global.pathMap.put(resultArray[0], pathArray);
                            dsocket.send(new DatagramPacket(result.getBytes(), result.length(), InetAddress.getByName("localhost"), 20000));
                        } catch (SocketTimeoutException e) {
                            continue;
                        }
                    }

                }
                else if (msg.startsWith("Search")) {
                    if (Global.contentList.contains(msg.split("Search:")[1])) {
                        String result = (msg.split("Search:")[1] + "&" + Global.hostName + "," + Global.backPort + "," + 5000);
                        dsocket.send(new DatagramPacket(result.getBytes(), result.length(), packet.getAddress(), packet.getPort()));
                    }
                    else {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Searcher: " + e);
        }
    }
}
