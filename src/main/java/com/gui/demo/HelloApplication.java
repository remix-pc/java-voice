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
    // Removido o IP fixo daqui

    private DatagramSocket socket;
    private TargetDataLine mic;
    private SourceDataLine speakers;
    private boolean transmitting = false;

    @Override
    public void start(Stage stage) {
        // NOVO: Campo para o IP do servidor
        TextField ipField = new TextField("26.176.74.82"); // Coloque seu IP como padrão para facilitar
        ipField.setPromptText("Digite o IP do servidor");

        TextField roomField = new TextField();
        roomField.setPromptText("Digite o nome da sala");

        Button connectBtn = new Button("Conectar");
        Button talkBtn = new Button("Falar");
        talkBtn.setDisable(true);

        Button disconnectBtn = new Button("Desconectar");
        disconnectBtn.setDisable(true);

        TextArea status = new TextArea();
        status.setEditable(false);

        // Adicionado o ipField ao layout
        VBox root = new VBox(10, ipField, roomField, connectBtn, talkBtn, disconnectBtn, status);
        root.setStyle("-fx-padding: 20");
        stage.setScene(new Scene(root, 300, 320)); // Aumentei um pouco a altura
        stage.setTitle("Chat de Voz");
        stage.show();

        connectBtn.setOnAction(e -> {
            // Pega o IP do campo de texto
            String serverIp = ipField.getText().trim();
            if (serverIp.isEmpty()) {
                status.appendText("Por favor, digite o IP do servidor.\n");
                return;
            }

            String roomName = roomField.getText().trim();
            if (roomName.isEmpty()) {
                status.appendText("Por favor, digite o nome da sala.\n");
                return;
            }

            try {
                socket = new DatagramSocket();
                // Usa o IP digitado pelo usuário
                InetAddress serverAddr = InetAddress.getByName(serverIp);

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
                        while (socket != null && !socket.isClosed()) {
                            socket.receive(packet);
                            if (speakers != null) {
                                speakers.write(packet.getData(), 0, packet.getLength());
                            }
                        }
                    } catch (Exception ex) {
                        // Não imprime erro se o socket foi fechado de propósito
                        if (!"Socket closed".equals(ex.getMessage())) {
                            ex.printStackTrace();
                        }
                    }
                }).start();

                talkBtn.setDisable(false);
                connectBtn.setDisable(true);
                roomField.setDisable(true);
                ipField.setDisable(true); // Desabilita o campo de IP após conectar
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
                    // Pega o IP do campo de texto novamente
                    InetAddress serverAddr = InetAddress.getByName(ipField.getText().trim());
                    byte[] buffer = new byte[1024];
                    while (transmitting) {
                        int bytesRead = mic.read(buffer, 0, buffer.length);
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, serverAddr, SERVER_PORT);
                        if (socket != null && !socket.isClosed()) {
                            socket.send(packet);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        talkBtn.setOnMouseReleased(e -> transmitting = false);

        disconnectBtn.setOnAction(e -> {
            try {
                transmitting = false;

                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                if (mic != null) {
                    mic.stop();
                    mic.close();
                }
                if (speakers != null) {
                    speakers.stop();
                    speakers.close();
                }

                talkBtn.setDisable(true);
                connectBtn.setDisable(false);
                disconnectBtn.setDisable(true);
                roomField.setDisable(false);
                ipField.setDisable(false); // Habilita o campo de IP novamente

                status.appendText("Desconectado.\n");
            } catch (Exception ex) {
                status.appendText("Erro ao desconectar: " + ex.getMessage() + "\n");
            }
        });

        // Garante que tudo será fechado se o usuário fechar a janela
        stage.setOnCloseRequest(event -> {
            disconnectBtn.fire(); // Simula o clique no botão de desconectar
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
