package org.andrew.linkState;

import java.io.*;
import java.net.*;
import java.util.Map;

public class LinkStart {
    public void run() throws IOException {
        ServerSocket serverSocket = null;
        String configFile = "src\\main\\resources\\node.conf";
        PeerNode mainNode = new PeerNode(configFile);
        Graph netGraph = new Graph(mainNode);
        Map<String, Integer> rankMap;
        rankMap = Graph.dijkstra(mainNode.uuid, netGraph);
        int frontEndPort = mainNode.frontPort;
        int backEndPort = mainNode.backPort;
        try {
            serverSocket = new ServerSocket(frontEndPort);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + frontEndPort);
            System.exit(1);
        }
        System.out.println("Waiting for connection.....");
        try {
            new BackEndReceiver(backEndPort, netGraph, rankMap).start();
            new BackEndSender(backEndPort, mainNode, netGraph).start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new FrontEnd(clientSocket, mainNode, netGraph, rankMap).start();
            }
        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
    }
}

