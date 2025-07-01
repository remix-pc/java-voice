package com.gui.demo;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class VoiceServer {

    private static final int PORT = 55555;
    private static final Set<SocketAddress> clients = ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<SocketAddress>> rooms = new ConcurrentHashMap<>();
    private static final Map<SocketAddress, String> clientRooms = new ConcurrentHashMap<>();


    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(PORT);
        byte[] buffer = new byte[1024];

        System.out.println("Servidor de voz rodando na porta " + PORT);

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);
            SocketAddress sender = packet.getSocketAddress();

            // Primeiro pacote do cliente: nome da sala
            if (!clientRooms.containsKey(sender)) {
                String roomName = new String(packet.getData(), 0, packet.getLength()).trim();
                rooms.putIfAbsent(roomName, ConcurrentHashMap.newKeySet());
                rooms.get(roomName).add(sender);
                clientRooms.put(sender, roomName);

                System.out.println("Cliente " + sender + " entrou na sala '" + roomName + "'");
                continue; // não reenvia o nome da sala
            }

            String room = clientRooms.get(sender);
            if (room != null) {
                for (SocketAddress client : rooms.get(room)) {
                    if (!client.equals(packet.getSocketAddress())) {
                        DatagramPacket forward = new DatagramPacket(packet.getData(), packet.getLength(), client);
                        serverSocket.send(forward);
                    }
                }
            }


//            // Salva o cliente
//            clients.add(packet.getSocketAddress());
//
//            // Reenvia o áudio para todos os clientes (inclusive o remetente)
//            for (SocketAddress client : clients) {
//                DatagramPacket forward = new DatagramPacket(packet.getData(), packet.getLength(), client);
//                serverSocket.send(forward);
//            }
        }
    }

}
