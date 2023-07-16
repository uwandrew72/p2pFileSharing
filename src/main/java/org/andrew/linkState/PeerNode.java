package org.andrew.linkState;

import java.io.*;
import java.util.*;

public class PeerNode {
    String uuid;
    String name;
    int frontPort;
    int backPort;
    String cdir;
    List<Edge> neighbors;

    public PeerNode(String nodeConf) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(nodeConf));
        this.neighbors = new LinkedList<>();
        String configLine = br.readLine();
        while (configLine != null) {
            String[] line = configLine.split("=");
            switch (line[0].trim()) {
                case "uuid":
                    uuid = line[1].trim();
                    break;
                case "name":
                    name = line[1].trim();
                    break;
                case "frontend_port":
                    frontPort = Integer.parseInt(line[1].trim());
                    break;
                case "backend_port":
                    backPort = Integer.parseInt(line[1].trim());
                    break;
                case "content_dir":
                    cdir = line[1].trim();
                    break;
                case "peer_count":
                    int peerCount = Integer.parseInt(line[1].trim());
                    for (int i = 0; i < peerCount; i++) {
                        String[] peer = br.readLine().split("=");
                        String[] peerInfo = peer[1].trim().split(",");
                        neighbors.add(new Edge(peerInfo[0], peerInfo[1], Integer.parseInt(peerInfo[2]), Integer.parseInt(peerInfo[3]), Integer.parseInt(peerInfo[4])));
                    }
                    break;
                default:
                    break;
            }
            configLine = br.readLine();
        }
        if (uuid == null) {
            this.uuid = UUID.randomUUID().toString();

        }

    }

}
