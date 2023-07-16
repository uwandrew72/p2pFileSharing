package org.andrew.mainServer;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import org.andrew.linkState.LinkStart;

public class mainServer {

    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("src\\main\\resources\\node.conf"));
        String configLine = br.readLine();
        while (configLine != null) {
            String[] config = configLine.split("=");
            if (config[0].equals("searchPort")) {
                Global.searchPort = Integer.parseInt(config[1]);
            }
            else if (config[0].equals("contentList")) {
                Global.contentList = config[1];

            }
            else if (config[0].equals("frontPort")) {
                Global.frontPort = Integer.parseInt(config[1]);
            }
            else if (config[0].equals("backPort")) {
                Global.backPort = Integer.parseInt(config[1]);
            }
            else if (config[0].equals("hostName")) {
                Global.hostName = config[1];
            }
            else if (config[0].equals("peerCount")) {
                for (int i = 0; i < Integer.parseInt(config[1]); i++) {
                    configLine = br.readLine();
                    String[] peer = configLine.split("=")[1].split(",");
                    Global.peerMap.put(peer[0], Integer.parseInt(peer[1]));
                }
            }
            configLine = br.readLine();
        }
        Global.pathMap.put(Global.contentList, new String[]{Global.hostName, String.valueOf(Global.backPort), String.valueOf(5000)});
        Map<String, String[]> path = Global.pathMap;
        ServerSocket serverSocket = null;
        int portNumber;
        int udpPort;
        int[] rate = {5000};
        if (args.length > 0) {
            portNumber = Integer.parseInt(args[0]);
            udpPort = Integer.parseInt(args[1]);
        } else {
            portNumber = Global.frontPort;
            udpPort = Global.backPort;
        }

        try {
            serverSocket = new ServerSocket(portNumber);
            // to do: config how link state parts are initialized
            // start link state
            // new LinkStart().run();
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + portNumber);
            System.exit(1);
        }
        System.out.println("Waiting for connection.....");
        try {
            new UdpServer(udpPort).start();
            new Searcher().start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new TCPServer(clientSocket, path, rate).start();

            }
        } catch (IOException e) {
            System.err.println("Accept failed.");
            System.exit(1);
        }
    }
}

class TCPServer extends Thread {
    Socket clientSocket;
    Map<String, String[]> path;

    int[] currRate;
    private final String CRLF = "\r\n";

    TCPServer(Socket clientSocket, Map path, int[] rate) {
        this.clientSocket = clientSocket;
        this.path = path;
        this.currRate = rate;
    }

    public void run() {
        System.out.println("Thread TCP start");

        try {
            System.out.println("Connection successful");
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = in.readLine();
            String[] requestElements = requestLine.split(" ");
            String method = requestElements[0];
            String request = requestElements[1];


            if (method.equals("GET")) {
                if (request.startsWith("/peer/add")) {
                    String[] requestDetail = request.split("=");
                    String content = requestDetail[1].split("&")[0];
                    String host = requestDetail[2].split("&")[0];
                    String port = requestDetail[3].split("&")[0];
                    String rate = String.valueOf(currRate[0]);

                    if (requestDetail.length > 4) {
                        rate = requestDetail[4].split("&")[0];
                    }
                    path.put(content, new String[]{host, port, rate});

                    // 200 OK
                    out.println("HTTP/1.1 200 OK" + CRLF + CRLF);
                    clientSocket.getOutputStream().write("Successfully added path.".getBytes());

                }
                else if (request.startsWith("/peer/view")) {

                    String targetContent = request.split("view/")[1];
                    int windows = Integer.parseInt(path.get(targetContent)[2]);
                    if (path.containsKey(targetContent)) {
                        DatagramSocket dsock = new DatagramSocket();
                        byte[] finalData = new byte[windows];
                        try {

                            InetAddress serverAddress = InetAddress.getByName(path.get(targetContent)[0]);
                            // send request to udp on give port
                            byte arr[] = (targetContent + "&" + path.get(targetContent)[2]).getBytes();
                            DatagramPacket dpack = new DatagramPacket(arr, arr.length, serverAddress, Integer.parseInt(path.get(targetContent)[1]));
                            dsock.send(dpack);
                            System.out.println("Tcp package sent");
                            // receive file from udp
                            byte[] receiveData = new byte[windows];
                            DatagramPacket dreceive = new DatagramPacket(receiveData, receiveData.length, serverAddress, Integer.parseInt(path.get(targetContent)[1]));
                            dsock.receive(dreceive);

                            byte[] ack = "ACK".getBytes();
                            DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, serverAddress, Integer.parseInt(path.get(targetContent)[1]));
                            dsock.send(ackPacket);

                            String headMessage = new String(dreceive.getData(), 0, dreceive.getLength());
                            String[] headSplit = headMessage.split("/");
                            int length = Integer.parseInt(headSplit[0]);
                            int count = Integer.parseInt(headSplit[1]);
                            String fileType = headSplit[2];

                            // send header and file to browser
                            out.println("HTTP/1.1 200 OK" + CRLF + "Date: " + getServerTime() + CRLF
                                    + "Content-Type: " + fileType + CRLF
                                    + "Connection: Keep-Alive");
                            out.println("");
                            out.flush();

                            int totalBytesSent = 0;
                            while (totalBytesSent < length) {
                                int bytesToSend = Math.min(length - totalBytesSent, windows);
                                dsock.receive(dreceive);
                                dsock.send(ackPacket);

                                byte[] currentData = dreceive.getData();
                                System.arraycopy(currentData, 0, finalData, 0, bytesToSend);
                                clientSocket.getOutputStream().write(finalData, 0, bytesToSend);
                                totalBytesSent += bytesToSend;
                            }

                        } catch (Exception e) {
                            System.out.println(e);
                        }


                        dsock.close();
                    }
                    else {
// Processing 404 Not Found
                        out.println("HTTP/1.1 404 Not Found" + CRLF + CRLF);
                        out.println("Couldn't find the path of this content.");
                    }
                }
                else if (request.startsWith("/peer/search")) {
                    String targetContent = request.split("search/")[1];
                    byte[] searchRequest = ("Master" + targetContent).getBytes();
                    DatagramSocket dsock = new DatagramSocket(20000);
                    DatagramPacket dpack = new DatagramPacket(searchRequest, searchRequest.length, InetAddress.getByName("localhost"), Global.searchPort);
                    dsock.send(dpack);
                    System.out.println("Search request sent");
                    byte[] receiveData = new byte[1024];
                    DatagramPacket dreceive = new DatagramPacket(receiveData, receiveData.length);
                    // set timeout
                    dsock.setSoTimeout(10000);
                    try {
                        dsock.receive(dreceive);
                        String result = new String(dreceive.getData(), 0, dreceive.getLength());

                        out.println("HTTP/1.1 200 OK" + CRLF + "Date: " + getServerTime() + CRLF
                                + "Content-Type: " + "text/plain" + CRLF
                                + "Connection: Keep-Alive" + CRLF);
                        out.println("");
                        out.flush();
                        String host = result.split("&")[1].split(",")[0];
                        String port = result.split("&")[1].split(",")[1];
                        clientSocket.getOutputStream().write(("Found the path on host: " + host + " and port: " + port + "." + CRLF).getBytes());

                    } catch (SocketTimeoutException e) {
                        out.println("HTTP/1.1 404 Not Found" + CRLF + CRLF);
                        out.println("Couldn't find the path of this content.");
                    }


                }
            } else {
// Processing 405 Method Not Allowed
                out.println("HTTP/1.1 405 Method Not Allowed" + CRLF + CRLF);
            }
            out.close();
            in.close();
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
        }

    }
    private String getServerTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT"));
        DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        return formatter.format(now);
    }
}
class UdpServer extends Thread  {
    int udpPort;
    UdpServer (int udpPort) {
        this.udpPort = udpPort;
    }
    public void run() {
        System.out.println("Thread Udp start");
        try {
            DatagramSocket serverSocket = new DatagramSocket(udpPort);
            byte[] receiveData = new byte[1024];
            while (true) {

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                System.out.println("package received");
                String nameAndRate = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String[] receiveSplit = nameAndRate.split("&");
                String fileName = receiveSplit[0];
                Queue<DatagramPacket> dataChunkQueue = NameToQueue(fileName, receivePacket, Integer.parseInt(receiveSplit[1]));
                while (!dataChunkQueue.isEmpty()) {
                    DatagramPacket sendPacket = dataChunkQueue.remove();
                    serverSocket.send(sendPacket);
                    byte[] ackData = new byte[4];
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);

                    // Receive ACK or resend if not received within a certain time limit
                    int retry = 0;
                    while (retry < 3) {
                        try {
                            serverSocket.setSoTimeout(5000); // 5 second timeout
                            serverSocket.receive(ackPacket);
                            String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                            if (ack.equals("ACK")) {
                                break;
                            }
                        } catch (SocketTimeoutException e) {
                            // Resend if ACK not received
                            serverSocket.send(sendPacket);
                            retry++;
                        }
                    }

                    // Check if all retries failed
                    if (retry == 3) {
                        System.err.println("Error: ACK not received after 3 retries, giving up");
                        break;
                    }
                }
            }

        }
        catch (Exception e) {
            System.err.println("Exception caught: " + e.getMessage());
        }
    }
    private Queue NameToQueue(String fileName, DatagramPacket receivePacket, int sendRate) throws IOException {
        System.out.println(sendRate);
        Queue<DatagramPacket> chunkQueue = new ArrayDeque<>();
        try {
            File targetFile = new File("./content/" + fileName);
            byte[] fileContent = Files.readAllBytes(targetFile.toPath());
            String fileType = Files.probeContentType(targetFile.toPath());
            String dataLength = fileContent.length + "/" + (fileContent.length / sendRate) + "/" + fileType;

            // data chunking

            byte[] dataLengthB = dataLength.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(dataLengthB, dataLengthB.length, receivePacket.getAddress(), receivePacket.getPort());
            chunkQueue.add(sendPacket);
            for (int i = 0; i < fileContent.length / sendRate; i++) {
                byte[] chunkData = Arrays.copyOfRange(fileContent, i * sendRate, (i + 1) * sendRate);
                sendPacket = new DatagramPacket(chunkData, chunkData.length, receivePacket.getAddress(), receivePacket.getPort());
                chunkQueue.add(sendPacket);
            }
            byte[] chunkData = Arrays.copyOfRange(fileContent, fileContent.length - fileContent.length % sendRate, fileContent.length);
            sendPacket = new DatagramPacket(chunkData, chunkData.length, receivePacket.getAddress(), receivePacket.getPort());
            chunkQueue.add(sendPacket);
        } catch (IOException e) {
            System.out.println(e);
        }
        return chunkQueue;
    }
}