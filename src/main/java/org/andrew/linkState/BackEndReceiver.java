package org.andrew.linkState;

import java.io.*;
import java.net.*;
import java.util.*;

public class BackEndReceiver extends Thread {
    int backPort;
    Graph netGraph;
    Map<String, Long> lastUpdate;
    Map<String, Integer> rankMap;
    BackEndReceiver(int backPort, Graph netGraph, Map<String, Integer> rankMap) {
        this.backPort = backPort;
        this.netGraph = netGraph;
        this.lastUpdate = new HashMap<>();
        this.rankMap = rankMap;
    }
    public void run() {
        System.out.println("BackEnd thread started");
        try {


            byte[] receiveData = new byte[1024];
            while (true) {
                long currentTime = System.currentTimeMillis();
                for (String key : lastUpdate.keySet()) {
                    if (currentTime - lastUpdate.get(key) > 30000) {
                        netGraph.killedNodes.add(key);
                    }
                }
                DatagramSocket serverSocket = new DatagramSocket(backPort);
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                serverSocket.close();
                Object receivedObject = deserialize(receivePacket.getData());
                if (receivedObject instanceof Graph) {
                    Graph receivedGraph = (Graph) receivedObject;
                    lastUpdate.put(receivedGraph.selfUUID, currentTime);
                    netGraph.MergeGraph(receivedGraph);
                    netGraph.KillNode();
                    rankMap.clear();
                    rankMap.putAll(Graph.dijkstra(netGraph.selfUUID, netGraph));

                    System.out.println(rankMap);
                }
                else {
                    // to be implemented
                    System.out.println("Received object is not a Graph");
                }
            }

        } catch (Exception e) {
            System.err.println("Exception caught in Receiver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object o = ois.readObject();
        ois.close();
        bis.close();
        return o;
    }

}
