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
        public int port;
        public int coin;
        public int bet;
        public int cardsValue;
        public int cardsReceived;
        public boolean standing;
        public boolean inRound;

        public Player(String ip, int port, int coin) {
            this.ip = ip;
            this.port = port;
            this.coin = coin;
            this.bet = 0;
            cardsValue = 0;
            cardsReceived = 0;
            standing = false;
            inRound = false;
        }
    }

    //fxml variables
    public Button nextRoundButton;
    public ListView listview;

    //Arrays
    public String[] initialDeck = {
            "2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "10H", "JH", "QH", "KH", "AH", //Szív
            "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "10S", "JS", "QS", "KS", "AS", //Pikk
            "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "10C", "JC", "QC", "KC", "AC", //Treff
            "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "JD", "QD", "KD", "AD"  //Káró
    };
    public LinkedList<String> mainDecks = new LinkedList<>();

    public int[] initialDeckValue = {
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11,
            2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 11
    };

    public LinkedList<Integer> mainDecksValue = new LinkedList<>();
    public LinkedList<Player> players = new LinkedList<>();

    //Other Variables
    public DatagramSocket socket = null;
    public boolean round = false;
    public int serverCardsValue = 0;
    public LinkedList<String> serverCards = new LinkedList<>();


    //When program starts
    public void initialize() {
        listview.getItems().add("Server starting...");
        //Fill the mainDecks with 6 initialDeck
        fillMainDecks();

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

        listview.getItems().add("Server is now running!");
    }

    //Fills the main deck array
    public void fillMainDecks() {
        for (int i = 0; i < 6; i++) for (int j = 0; j < initialDeck.length; j++) mainDecks.add(initialDeck[j]);
        for (int i = 0; i < 6; i++) for (int j = 0; j < initialDeckValue.length; j++) mainDecksValue.add(initialDeckValue[j]);
    }


    //Start round button
    public void onClickNextRound() {
        nextRoundButton.setDisable(true);
        round = true;
        String message = String.format("start:%d", players.size());

        while (serverCardsValue < 17) serverCards.add(randCard('s', ""));

        for (String x : serverCards) listview.getItems().add("serverCard = " + x);
        
        for (Player x : players) {
            x.inRound = true;
            send(message, x.ip, x.port);
        }
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

        //Player joins
        if (s[0].equals("join") && players.size() < 5 && tryInt(s[1]) > 0 && searchPlayer(ip) == -1 && tryInt(s[1]) < 100000) {
            listview.getItems().add(ip + " játékos csatlakozott " + s[1] + " pénzel");
            message = String.format("joined:%s", s[1]);
            send(message, ip, port);
            players.add(new Player(ip, port, Integer.parseInt(s[1])));
            nextRoundButton.setDisable(false);
        }

        //Player exists
        if (s[0].equals("exit") && searchPlayer(ip) != -1) {
            message = String.format("paid:%d", players.get(searchPlayer(ip)).coin);
            listview.getItems().add(ip + " játékos kilépett " + players.get(searchPlayer(ip)).coin + " pénz visszaadva");
            send(message, ip, port);
            players.remove(searchPlayer(ip));

            if (players.size() == 0) nextRoundButton.setDisable(true);
            if (round && players.size() == 0) {
                mainDecks.clear();
                mainDecksValue.clear();
                serverCardsValue = 0;
                nextRoundButton.setDisable(true);
                fillMainDecks();
                listview.getItems().add("NEXT ROUND AVAILABLE --------------------------------");
            }
        }

        //Inround bet stand etc
        if (round && players.get(searchPlayer(ip)).inRound) {
            //Player puts a bet
            if (s[0].equals("bet") && players.get(searchPlayer(ip)).cardsValue == 0) {
                players.get(searchPlayer(ip)).bet = Integer.parseInt(s[1]);
                players.get(searchPlayer(ip)).coin -= Integer.parseInt(s[1]);

                String randCard;
                try { Thread.sleep(500); } catch (InterruptedException e) { throw new RuntimeException(e); }
                randCard = randCard('k', ip);
                message = String.format("k:%s", randCard);
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);

                try { Thread.sleep(500); } catch (InterruptedException e) { throw new RuntimeException(e); }
                randCard = randCard('k', ip);
                message = String.format("k:%s", randCard);
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);

                try { Thread.sleep(500); } catch (InterruptedException e) { throw new RuntimeException(e); }
                message = String.format("s:%s", serverCards.get(0));
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);
            }

            //Player hits
            if (s[0].equals("hit") && players.get(searchPlayer(ip)).cardsValue <= 21 && players.get(searchPlayer(ip)).cardsValue > 0) {
                message = String.format("k:%s", randCard('k', ip));
                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                send(message, ip, port);
            }

            //Player stands
            if (s[0].equals("stand")) {
                players.get(searchPlayer(ip)).standing = true;
                if (allStand()) {
                    for (Player player : players) {
                            for (int i = 1; i < serverCards.size(); i++) {
                                try { Thread.sleep(100); } catch (InterruptedException e) { throw new RuntimeException(e); }
                                message = String.format("s:%s", serverCards.get(i));
                                send(message, player.ip, player.port);
                        }
                        send("end", player.ip, player.port);

                        listview.getItems().add(String.format("%s coin = %d\n", player.ip, player.coin));
                        listview.getItems().add(String.format("%s bet = %d\n", player.ip, player.bet));
                        listview.getItems().add(String.format("%s cardReceived = %d\n", player.ip, player.cardsReceived));
                        listview.getItems().add(String.format("%s cardsValue = %d\n", player.ip, player.cardsValue));
                        listview.getItems().add(String.format("Server cardsValue = %d\n", serverCardsValue));

                        if (player.cardsValue < 22) {

                            if (player.cardsValue < serverCardsValue && serverCardsValue < 22) {
                                message = String.format("balance:%d", player.coin);
                                listview.getItems().add(player.ip + " játékosnak elküldve: " + message);
                                send(message, player.ip, player.port);
                            }

                            else if (serverCardsValue > 21) {
                                player.coin += player.bet * 2;
                                message = String.format("balance:%d", player.coin);
                                listview.getItems().add(player.ip + " játékosnak elküldve: " + message);
                                send(message, player.ip, player.port);
                            }

                            else if (player.cardsValue == 21 && player.cardsReceived == 2) {
                                player.coin += player.bet * 2.5;
                                message = String.format("balance:%d", player.coin);
                                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                                send(message, player.ip, player.port);
                            }

                            else if (player.cardsValue > serverCardsValue) {
                                player.coin += player.bet * 2;
                                message = String.format("balance:%d", player.coin);
                                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                                send(message, player.ip, player.port);
                            }

                            else if (player.cardsValue == serverCardsValue) {
                                player.coin += player.bet;
                                message = String.format("balance:%d", player.coin);
                                listview.getItems().add(ip + " játékosnak elküldve: " + message);
                                send(message, player.ip, player.port);
                            }

                        } else {
                            message = String.format("balance:%d", player.coin);
                            listview.getItems().add(ip + " játékosnak elküldve: " + message);
                            send(message, player.ip, player.port);
                        }
                    }
                }

                try { Thread.sleep(5000); } catch (InterruptedException e) { throw new RuntimeException(e); }
                for (Player player : players) {
                    player.bet = 0;
                    player.cardsReceived = 0;
                    player.cardsValue = 0;
                    player.inRound = false;
                    player.standing = false;
                }

                mainDecks.clear();
                mainDecksValue.clear();
                serverCardsValue = 0;
                round = false;
                nextRoundButton.setDisable(false);
                fillMainDecks();
                serverCards.clear();
                listview.getItems().add("NEXT ROUND AVAILABLE --------------------------------");
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
        int randIndex = (int)(Math.random()*mainDecks.size());
        calculateCardValue(randIndex, platform, ip);
        String randomCard = mainDecks.get(randIndex);

        mainDecks.remove(randIndex);
        mainDecksValue.remove(randIndex);
        return randomCard;
    }

    //Calculate card value
    public void calculateCardValue(int randIndex, char platform, String ip) {
        if (platform == 's') {
            if (mainDecks.get(randIndex).charAt(0) == 'A') {
                if ((serverCardsValue + 11) > 21) {
                    serverCardsValue += 1;
                } else {
                    serverCardsValue += 11;
                }
            } else {
                serverCardsValue += mainDecksValue.get(randIndex);
            }
        } else {
            if (mainDecks.get(randIndex).charAt(0) == 'A') {
                if ((players.get(searchPlayer(ip)).cardsValue + 11) > 21) {
                    players.get(searchPlayer(ip)).cardsValue += 1;
                } else {
                    players.get(searchPlayer(ip)).cardsValue += 11;
                }
            } else {
                players.get(searchPlayer(ip)).cardsValue += mainDecksValue.get(randIndex);
            }
            players.get(searchPlayer(ip)).cardsReceived++;
        }
    }

    //Contains a player in players array by ip
    public boolean containsPlayer(String ip) {
        for (Player x : players) if (x.ip.equals(ip)) return true;
        return false;
    }

    //Tries to parseInt
    public int tryInt(String input) {
        int num;
        try { num = Integer.parseInt(input); } catch (Exception e) { num = 0; }
        return num;
    }

    //Everyone is standing
    public boolean allStand() {
        for (Player player : players) if (!player.standing) return false;
        return true;
    }
}
