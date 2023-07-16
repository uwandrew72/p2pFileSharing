package org.andrew.linkState;

import java.util.*;
import java.io.*;

public class Graph implements Serializable {
    public String selfUUID;
    public int size;
    public HashMap<String, LinkedList<Edge>> adjList;
    public LinkedList<String> killedNodes;

    public Graph(PeerNode n) {
        this.selfUUID = n.uuid;
        this.size = 1;
        this.adjList = new HashMap<>();
        this.killedNodes = new LinkedList<>();
        this.adjList.put(n.uuid, new LinkedList<>());
        for (Edge e : n.neighbors) {
            this.adjList.get(n.uuid).add(e);
            if (!this.adjList.containsKey(e.toUUID)) {
                this.adjList.put(e.toUUID, new LinkedList<>());
                this.size++;
            }
        }
    }

    public void addNode(String uuid) {
        if (!this.adjList.containsKey(uuid)) {
            this.adjList.put(uuid, new LinkedList<>());
            this.size++;
        }
    }


    public void addEdge(String uuid, Edge e) {
        if (this.adjList.containsKey(uuid)) {
            this.adjList.get(uuid).add(e);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String uuid : this.adjList.keySet()) {
            sb.append(uuid).append(" : ");
            for (Edge e : this.adjList.get(uuid)) {
                sb.append(e.toUUID).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Dijkstra: find the shortest path from mainNode to all other nodes
    public static Map<String, Integer> dijkstra(String mainNode, Graph netGraph) {
        // create a priority queue to store nodes with their distances
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>((a, b) -> a.distance - b.distance);

        // create a map to store visited nodes with their distances
        Map<String, Integer> visited = new HashMap<>();

        // add mainNode to the queue with distance zero
        pq.add(new NodeDistance(mainNode, 0));

        while (!pq.isEmpty()) {
            // poll the node with minimum distance from the queue
            NodeDistance node = pq.poll();

            // if node is already visited, skip it
            if (visited.containsKey(node.name)) continue;

            // mark node as visited and store its distance
            visited.put(node.name, node.distance);

            // print node and its distance
            System.out.println(node.name + " : " + node.distance);

            // for each neighbor of node, update its distance and add it to the queue
            for (Edge edge : netGraph.adjList.get(node.name)) {
                int newDistance = node.distance + edge.weight;
                pq.add(new NodeDistance(edge.toUUID, newDistance));
            }
        }

        return visited;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        oos.close();
        bos.close();
        return bos.toByteArray();
    }

    public static Graph deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Graph g = (Graph) ois.readObject();
        ois.close();
        bis.close();
        return g;
    }

    public void MergeGraph(Graph g) {
        for (String uuid : g.adjList.keySet()) {
            if (!this.adjList.containsKey(uuid)) {
                this.adjList.put(uuid, new LinkedList<>());
                this.size++;
            }
            for (Edge e : g.adjList.get(uuid)) {
                if (this.adjList.get(uuid).stream().noneMatch(edge -> edge.toUUID.equals(e.toUUID))) {
                    this.adjList.get(uuid).add(e);
                }
            }
        }
        for (String uuid : g.killedNodes) {
            if (!this.killedNodes.contains(uuid)) {
                this.killedNodes.add(uuid);
            }
        }
    }

    public void KillNode() {
        if (!this.killedNodes.isEmpty()) {
            for (String uuid : this.killedNodes) {
                this.removeNode(uuid);
            }
        }
    }

    public void removeNode(String uuid) {
        for (String key : this.adjList.keySet()) {
            this.adjList.get(key).removeIf(edge -> edge.toUUID.equals(uuid));
        }
        if (this.adjList.containsKey(uuid)) {
            this.adjList.remove(uuid);
            this.size--;
        }
    }
}

class NodeDistance {
    String name;
    int distance;

    public NodeDistance(String name, int distance) {
        this.name = name;
        this.distance = distance;
    }
}

