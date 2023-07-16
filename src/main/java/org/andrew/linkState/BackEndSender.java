package org.andrew.linkState;

import java.net.*;


public class BackEndSender extends Thread {
    int backPort;
    PeerNode mainNode;
    Graph netGraph;
    BackEndSender(int backPort, PeerNode mainNode, Graph netGraph) {
        this.backPort = backPort;
        this.mainNode = mainNode;
        this.netGraph = netGraph;
    }

    public void run() {
        try {
            while (true) {
                DatagramSocket senderSocket = new DatagramSocket();
                byte [] sendData = netGraph.serialize();
                for (Edge e : mainNode.neighbors) {
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(e.toHost), e.toBack);
                    senderSocket.send(sendPacket);
                }
                senderSocket.close();
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            System.err.println("Exception caught in Sender: " + e.getMessage());
            System.err.println("Exception location: " + e.getStackTrace()[0].getLineNumber());
        }
    }

}
