package com.example.blackjack;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.net.*;
import java.util.LinkedList;

public class ServerController {

    //Player Class
    private class Player {
        public String ip;
        public int coin;
        public int bet;
        public ListView<String> cards = new ListView<>();

        public Player(String ip, int coin) {
            this.ip = ip;
            this.coin = coin;
            this.bet = 0;
        }
    }

    //fxml variables
    public Button button;
    public ListView listview;

    //Arrays
    public String[] initialDeck = {
            "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9", "HJ", "HQ", "HK", "HA", //Szív
            "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "SJ", "SQ", "SK", "SA", //Pikk
            "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "CJ", "CQ", "CK", "CA", //Treff
            "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "DJ", "DQ", "DK", "DA"  //Káró
    };
    public LinkedList<String> mainDecks = new LinkedList<>();
    public LinkedList<Player> players = new LinkedList<>();

    //Other Variables
    DatagramSocket socket = null;
    public boolean round = false;
    public int deckLength = 0;

    public ListView<String> serverCards = new ListView<>();

    //When program starts
    public void initialize() {
        //Fill the mainDecks with 6 initialDeck
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < initialDeck.length; j++) {
                mainDecks.add(initialDeck[j]);
            }
        }
        deckLength = mainDecks.size();

        //Initiate the socket
        try { socket = new DatagramSocket(678); } catch (SocketException e) { e.printStackTrace(); }

        //Initiate the thread for the fogad function
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                recieve();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    //Start round button
    public void onClickButton() {
    }

    //Send function to send data to client
     private void send(String uzenet, String ip, int port) {
        try {
            byte[] adat = uzenet.getBytes("utf-8");
            InetAddress ipv4 = Inet4Address.getByName(ip);
            DatagramPacket packet = new DatagramPacket(adat, adat.length, ipv4, port);
            socket.send(packet);
        } catch (Exception e) { e.printStackTrace(); }
    }

    //Recieve function to recieve data from client
    private void recieve() { // Külön szálon!
        byte[] adat = new byte[256];
        DatagramPacket packet = new DatagramPacket(adat, adat.length);
        while (true) {
            try {
                socket.receive(packet);
                String uzenet = new String(adat, 0, packet.getLength(), "utf-8");
                String ip = packet.getAddress().getHostAddress();
                int port = packet.getPort();
                Platform.runLater(() -> onRecieve(uzenet, ip, port));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    //Process the recieved data according to the protocoll
    private void onRecieve(String uzenet, String ip, int port) {
        String[] s = uzenet.split(":");

        if (s[0].equals("join")) {
            players.add(new Player(ip, Integer.parseInt(s[1])));
        } else if (s[0].equals("exit")) {
            String message = String.format("paid:%d", players.get(searchPlayer(ip)).coin);
            send(message, ip, port);
            players.remove(searchPlayer(ip));
        } else if (s[0].equals("bet")) {
            players.get(searchPlayer(ip)).bet = Integer.parseInt(s[1]);
            int randIndex = (int)(Math.random()*deckLength) //ITT FEJEZTEM BE!!!! (feladat, bejezni a get-et)
            String randomCard = mainDecks.get(randIndex);
            mainDecks.remove(randIndex);
            String message = String.format("s:%d");
        }
    }

    //Searches a player by ip
    public int searchPlayer(String ip) {
        for (int index = 0; index < players.size(); index++) {
            if (players.get(index).ip.equals(ip)) return index;
        }
        return -1;
    }

}