package com.gui.demo;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class VoiceServer {

    private static final int PORT = 55555;
    // IMPORTANTE: Coloque aqui o SEU endereço de IP do Radmin VPN
    private static final String SERVER_IP = "26.176.74.82";

    private static final Map<String, Set<SocketAddress>> rooms = new ConcurrentHashMap<>();
    private static final Map<SocketAddress, String> clientRooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try {
            // Modificação: Força o socket a escutar apenas no IP especificado
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            DatagramSocket serverSocket = new DatagramSocket(PORT, serverAddress);

            byte[] buffer = new byte[1024];

            System.out.println("Servidor de voz rodando em " + serverAddress.getHostAddress() + ":" + PORT);


            //FAWDFAWGFAWFDAWSDAWTESTE

            System.out.println("A aguardar conexões...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet); // A execução fica parada aqui até receber um pacote
                SocketAddress sender = packet.getSocketAddress();

                System.out.println("Pacote recebido de: " + sender); // Linha de debug para ver se algo chega

                // Primeiro pacote do cliente: nome da sala
                if (!clientRooms.containsKey(sender)) {
                    String roomName = new String(packet.getData(), 0, packet.getLength()).trim();

                    // Validação para não processar pacotes de áudio como nome de sala
                    if (roomName.length() > 50) { // Nomes de sala não devem ser tão grandes
                        System.out.println("Pacote inválido (provavelmente áudio) recebido de um cliente não registado. A ignorar.");
                        continue;
                    }

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
            }
        } catch (Exception e) {
            System.err.println("Ocorreu um erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
