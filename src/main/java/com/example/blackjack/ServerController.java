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
        public int cardsValue;

        public Player(String ip, int coin) {
            this.ip = ip;
            this.coin = coin;
            this.bet = 0;
            cardsValue = 0;
        }
    }

    //fxml variables
    public Button nextRoundButton;
    public ListView listview;
    public Button resetButton;

    //Arrays
    public String[] initialDeck = {
            "2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "10H", "JH", "QH", "KH", "AH", //Szív
            "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS", "AS", //Pikk
            "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "10C", "JC", "QC", "KC", "AC", //Treff
            "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "JD", "QD", "KD", "AD"  //Káró
    };

    public int[] initialDeckValue = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11
    };

    public LinkedList<String> mainDecks = new LinkedList<>();
    public LinkedList<Integer> mainDecksValue = new LinkedList<>();
    public LinkedList<Player> players = new LinkedList<>();

    //Other Variables
    public DatagramSocket socket = null;
    public boolean round = false;
    public int deckLength = 0;
    public int standCount = 0;
    public int serverCardsValue = 0;


    //When program starts
    public void initialize() {
        //Fill the mainDecks with 6 initialDeck
        fillMainDecks();
        fillMainDecksValues();

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

    //Fills the main deck array
    public void fillMainDecks() {
        for (int i = 0; i < 6; i++) for (int j = 0; j < initialDeck.length; j++) mainDecks.add(initialDeck[j]);
        deckLength = mainDecks.size();
    }

    //Fills the main deck array
    public void fillMainDecksValues() {
        for (int i = 0; i < 6; i++) for (int j = 0; j < initialDeckValue.length; j++) mainDecksValue.add(initialDeckValue[j]);
    }

    //Start round button
    public void onClickNextRound() {
        nextRoundButton.setDisable(true);
        round = true;
        resetButton.setDisable(false);

        String message = String.format("start:%d", players.size());
        for (Player x : players) {
            send(message, x.ip, 678);
        }
    }

    //Reset game
    public void onClickReset() {
        mainDecks.clear();
        listview.getItems().clear();
        serverCardsValue = 0;
        players.clear();
        nextRoundButton.setDisable(false);
        resetButton.setDisable(true);
        fillMainDecks();
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

    //Recieve data from client
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

    //Process the recieved data according to the protocol
    private void onRecieve(String uzenet, String ip, int port) {
        String[] s = uzenet.split(":");
        String message = "";
        if (s[0].equals("join") && players.size() < 5 && Integer.parseInt(s[1]) > 0 && !containsPlayer(ip)) {
            listview.getItems().add(ip + " játékos csatlakozott " + s[1] + " pénzel");
            message = String.format("joined:%s", s[1]);
            send(message, ip, port);
            players.add(new Player(ip, Integer.parseInt(s[1])));
        }

        else if (s[0].equals("exit")) {
            message = String.format("paid:%d", players.get(searchPlayer(ip)).coin);
            listview.getItems().add(ip + " játékos kilépett " + players.get(searchPlayer(ip)).coin + " pénz visszaadva");
            send(message, ip, port);
            players.remove(searchPlayer(ip));
            deckLength = mainDecks.size();
        }

        if (round) {
            if (s[0].equals("bet")) {
                players.get(searchPlayer(ip)).bet = Integer.parseInt(s[1]);
                players.get(searchPlayer(ip)).coin -= Integer.parseInt(s[1]);
                String randCard;

                randCard = randCard('s', ip);
                message = String.format("s:%s", randCard);
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);

                randCard = randCard('k', ip);
                message = String.format("k:%s", randCard);
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);

                randCard = randCard('k', ip);
                message = String.format("k:%s", randCard);
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);
            }

            else if (s[0].equals("hit") && players.get(searchPlayer(ip)).cardsValue <= 21) { //ENNÉL VALAMI NEM JÓ
                message = String.format("k:%s", randCard('k', ip));
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);
            }

            else if (s[0].equals("stand")) {
                standCount++;
                String randCard;

                if (standCount == players.size()) {
                    do {
                        randCard = randCard('s', ip);
                        message = String.format("s:%s", randCard);
                        send(message, ip, port);
                    } while (serverCardsValue < 17);
                }
            }
        }
    }

    //Searches a player by ip
    public int searchPlayer(String ip) {
        for (int index = 0; index < players.size(); index++) {
            if (players.get(index).ip.equals(ip)) return index;
        }
        return -1;
    }

    //Random card
    public String randCard(char platform, String ip) {
        int randIndex = (int)(Math.random()*deckLength);
        calculateCardValue(randIndex, platform, ip);
        String randomCard = mainDecks.get(randIndex);
        mainDecks.remove(randIndex);
        mainDecksValue.remove(randIndex);
        return randomCard;
    }

    //Contains a player in players array by ip
    public boolean containsPlayer(String ip) {
        for (Player x : players) if (x.ip.equals(ip)) return true;
        return false;
    }

    //Calculate card value
    public void calculateCardValue(int randIndex, char platform, String ip) {
        if (platform == 's') {
            if (mainDecks.get(randIndex).equals("A") && serverCardsValue + mainDecksValue.get(randIndex) > 21) {
                serverCardsValue += 1;
            } else {
                serverCardsValue += mainDecksValue.get(randIndex);
            }
        } else {
            int playerCardsValue = players.get(searchPlayer(ip)).cardsValue;
            if (mainDecks.get(randIndex).equals("A") && playerCardsValue + mainDecksValue.get(randIndex) > 21) {
                players.get(searchPlayer(ip)).cardsValue += 1;
            } else {
                players.get(searchPlayer(ip)).cardsValue += mainDecksValue.get(randIndex);
            }
        }
    }
}