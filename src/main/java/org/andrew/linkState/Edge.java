package org.andrew.linkState;

import java.io.Serializable;

public class Edge implements Serializable {
    public String toUUID;
    public String toHost;
    public int toFront;
    public int toBack;
    public int weight;

    public Edge(String toUUID, String toHost, int toFront, int toBack, int weight) {
        this.toUUID = toUUID;
        this.toHost = toHost;
        this.toFront = toFront;
        this.toBack = toBack;
        this.weight = weight;
    }

    public String toString() {
        return "Edge{" +
                "toUUID='" + toUUID + '\'' +
                ", toHost='" + toHost + '\'' +
                ", toFront=" + toFront +
                ", toBack=" + toBack +
                ", weight=" + weight +
                '}';
    }
}
