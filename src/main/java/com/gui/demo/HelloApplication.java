package com.gui.demo;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.net.*;

public class HelloApplication extends Application {

    private static final int SERVER_PORT = 55555;
    private static final String SERVER_IP = "127.0.0.1"; // Altere para o IP do servidor

    private DatagramSocket socket;
    private TargetDataLine mic;
    private SourceDataLine speakers;
    private boolean transmitting = false;


    @Override
    public void start(Stage stage){
        TextField roomField = new TextField();
        roomField.setPromptText("Digite o nome da sala");

        Button connectBtn = new Button("Conectar");
        Button talkBtn = new Button("Falar");
        talkBtn.setDisable(true);

        Button disconnectBtn = new Button("Desconectar");
        disconnectBtn.setDisable(true);

        TextArea status = new TextArea();
        status.setEditable(false);

        VBox root = new VBox(10, roomField, connectBtn, talkBtn, disconnectBtn ,status);
        root.setStyle("-fx-padding: 20");
        stage.setScene(new Scene(root, 300, 280));
        stage.setTitle("Chat de Voz");
        stage.show();

        connectBtn.setOnAction(e -> {
            String roomName = roomField.getText().trim();
            if (roomName.isEmpty()) {
                status.appendText("Por favor, digite o nome da sala.\n");
                return;
            }

            try {
                socket = new DatagramSocket();
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                // Envia nome da sala ao servidor
                byte[] roomData = roomName.getBytes();
                DatagramPacket roomPacket = new DatagramPacket(roomData, roomData.length, serverAddr, SERVER_PORT);
                socket.send(roomPacket);

                AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                mic = AudioSystem.getTargetDataLine(format);
                mic.open(format);
                mic.start();

                speakers = AudioSystem.getSourceDataLine(format);
                speakers.open(format);
                speakers.start();

                // Thread de recepção
                new Thread(() -> {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        while (true) {
                            socket.receive(packet);
                            speakers.write(packet.getData(), 0, packet.getLength());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();

                talkBtn.setDisable(false);
                connectBtn.setDisable(true);
                roomField.setDisable(true);
                disconnectBtn.setDisable(false);
                status.appendText("Conectado à sala '" + roomName + "'\n");

            } catch (Exception ex) {
                status.appendText("Erro ao conectar: " + ex.getMessage() + "\n");
            }
        });

        talkBtn.setOnMousePressed(e -> {
            transmitting = true;
            new Thread(() -> {
                try {
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                    byte[] buffer = new byte[1024];
                    while (transmitting) {
                        int bytesRead = mic.read(buffer, 0, buffer.length);
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, serverAddr, SERVER_PORT);
                        socket.send(packet);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });


        disconnectBtn.setOnAction(e -> {
            try {
                transmitting = false;

                if (mic != null) {
                    mic.stop();
                    mic.close();
                }

                if (speakers != null) {
                    speakers.stop();
                    speakers.close();
                }

                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }

                talkBtn.setDisable(true);
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
                roomField.setDisable(false);

                status.appendText("Desconectado.\n");
            } catch (Exception ex) {
                status.appendText("Erro ao desconectar: " + ex.getMessage() + "\n");
            }
        });

        talkBtn.setOnMouseReleased(e -> transmitting = false);
    }

    public static void main(String[] args) {
        launch();
    }
}