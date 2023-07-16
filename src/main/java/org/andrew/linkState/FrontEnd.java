package org.andrew.linkState;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class FrontEnd extends Thread {
    Socket clientSocket;
    PeerNode myNode;
    Graph netGraph;
    Map<String, Integer> rankMap;
    private final String CRLF = "\r\n";

    public FrontEnd(Socket s, PeerNode n, Graph g, Map<String, Integer> r) {
        this.clientSocket = s;
        this.myNode = n;
        this.netGraph = g;
        this.rankMap = r;
    }

    public void run() {
        System.out.println("FrontEnd thread started");
        try {
            System.out.println("Connection successful");
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // read request
            String requestLine = in.readLine();
            String[] requestElements = requestLine.split(" ");
            String method = requestElements[0];
            String request = requestElements[1];

            if (method.equals("GET")) {
                if (request.startsWith("/peer/add?")) {
                    // process add peer request
                }
                else if (request.startsWith("/peer/view")) {
                    // process view peer request
                }
                else if (request.startsWith("/peer/config")) {
                    // process config peer request
                }
                else if (request.startsWith("/peer/uuid")) {
                    out.println("HTTP/1.1 200 OK" + CRLF
                            + "Connection: Keep-Alive" + CRLF + CRLF);
                    out.flush();
                    clientSocket.getOutputStream().write((myNode.uuid).getBytes());
                }
                else if (request.startsWith("/peer/neighbors")) {
                    // process neighbors peer request
                    out.println("HTTP/1.1 200 OK" + CRLF
                            + "Connection: Keep-Alive" + CRLF + CRLF);
                    out.flush();
                    clientSocket.getOutputStream().write(("UUID" + "    " + "HOST" + "    " + "FRONTEND" + "    " + "BACKEND" + "    " + "METRIC").getBytes());
                    clientSocket.getOutputStream().write(CRLF.getBytes());
                    for (int i = 0; i < myNode.neighbors.size(); i++) {
                        String neighborEdge = myNode.neighbors.get(i).toString();
                        clientSocket.getOutputStream().write((neighborEdge).getBytes());
                        clientSocket.getOutputStream().write(CRLF.getBytes());
                    }
                }
                else if (request.startsWith("/peer/addneighbor?")) {
                    String requestParams = request.substring(request.indexOf("?") + 1);
                    String[] params = requestParams.split("&");
                    String uuid = params[0].split("=")[1];
                    String host = params[1].split("=")[1];
                    int frontPort = Integer.parseInt(params[2].split("=")[1]);
                    int backPort = Integer.parseInt(params[3].split("=")[1]);
                    int metric = Integer.parseInt(params[4].split("=")[1]);
                    Edge newEdge = new Edge(uuid, host, frontPort, backPort, metric);
                    myNode.neighbors.add(newEdge);
                    netGraph.addEdge(myNode.uuid, newEdge);
                    netGraph.addNode(uuid);
                    out.println("HTTP/1.1 200 OK" + CRLF
                            + "Connection: Keep-Alive" + CRLF + CRLF);
                    out.flush();
                    clientSocket.getOutputStream().write("Successfully added neighbor".getBytes());
                }
                else if (request.startsWith("/peer/rank")) {
                    out.println("HTTP/1.1 200 OK" + CRLF
                            + "Connection: Keep-Alive" + CRLF + CRLF);
                    out.flush();
                    for (Map.Entry<String, Integer> entry : rankMap.entrySet()) {
                        String key = entry.getKey();
                        Integer value = entry.getValue();
                        clientSocket.getOutputStream().write((key + "    " + value).getBytes());
                        clientSocket.getOutputStream().write(CRLF.getBytes());
                    }
                }
                else if (request.startsWith("/peer/map")) {
                    out.println("HTTP/1.1 200 OK" + CRLF
                            + "Connection: Keep-Alive" + CRLF + CRLF);
                    out.flush();
                    clientSocket.getOutputStream().write(netGraph.toString().getBytes());
                }
                else if (request.startsWith("/peer/kill")) {
                    System.exit(0);
                }
            }
            else {
                // 405 Method Not Allowed
                out.println("HTTP/1.1 405 Method Not Allowed" + CRLF + CRLF);
            }
            out.close();
            in.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Exception caught in FrontEnd: " + e.getMessage());
            System.err.println("Exception location: " + e.getStackTrace()[0].getLineNumber());
        }
    }
    private String getServerTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT"));
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        return formatter.format(now);
    }

}
