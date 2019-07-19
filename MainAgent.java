package PSI1;


import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainAgent extends Agent {
    public boolean slp = false;
    Random random = new Random(System.currentTimeMillis());
    private GUI gui;
    private AID[] playerAgents;
    private GameParametersStruct parameters = new GameParametersStruct();
    private Object[][] data;
    private int count, total1 = 0, total2 = 0;

    @Override
    protected void setup() {
        gui = new GUI(this);
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));
        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    public int updatePlayers() {
        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gui.logLine("Found " + result.length + " players");
            }
            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }
        //Provisional
        String[] playerNames = new String[playerAgents.length];
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();
        }
        gui.setPlayersUI(playerNames);
        return 0;
    }

    public void newGame() {
        addBehaviour(new GameManager());
    }

    public void generateMatrix() {
        int size = parameters.S, numi, numj;
        data = new Object[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    numi = random.nextInt(10);
                    numj = numi;
                    data[i][j] = numi + ";" + numj;
                } else {
                    numi = random.nextInt(10);
                    numj = random.nextInt(10);
                    data[i][j] = numi + ";" + numj;
                    data[j][i] = numj + ";" + numi;
                }
            }
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                gui.log(data[i][j].toString());
            }
        }
        gui.modifyMatrix(data, size);
    }

    public void setNewParams(String[] params) {
        parameters.N = Integer.parseInt(params[0]);
        parameters.S = Integer.parseInt(params[1]);
        parameters.R = Integer.parseInt(params[2]);
        parameters.I = Integer.parseInt(params[3]);
        parameters.P = Integer.parseInt(params[4]);
        gui.logLine("Configured (NSRIP): " + parameters.N + " " + parameters.S + " " + parameters.R + " " + parameters.I + " " + parameters.P);
    }

    public void setNewParams(int i) {
        parameters.R = i;
    }


    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {
            //Assign the IDs
            ArrayList<PlayerInformation> players = new ArrayList<>();
            int lastId = 0;
            for (AID a : playerAgents) {
                players.add(new PlayerInformation(a, lastId++));
            }

            //Initialize (inform ID)
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.S + "," + parameters.R + "," + parameters.I + "," + parameters.P);
                msg.addReceiver(player.aid);
                send(msg);
            }
            //Organize the matches
            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) {
                    try {
                        count=0;
                        playGame(players.get(i), players.get(j));


                    } catch (IOException ex) {
                        Logger.getLogger(MainAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            Collections.sort(players);
            int pos = 1;
            for (PlayerInformation player : players) {
                gui.logLine(pos + "º: " + player.id +": "+playerAgents[player.id].getName()+ " (" + player.points + ").");
                pos++;
            }


        }

        private void playGame(PlayerInformation player1, PlayerInformation player2) throws IOException {
            generateMatrix();
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(player1.aid);
            msg.addReceiver(player2.aid);
            msg.setContent("NewGame#" + player1.id + "," + player2.id);
            send(msg);
            int pos1, pos2, punt1, punt2;

            while (true) {
                if (count == 0) {
                    total1 = 0;
                    total2 = 0;
                }
                if (slp) {
                    while (true) {
                        try {
                            Thread.sleep(400);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!slp) break;
                    }
                }
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Position");
                msg.addReceiver(player1.aid);
                send(msg);
                gui.logLine("Main Waiting for movement");
                ACLMessage move1 = blockingReceive();
                gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                pos1 = Integer.parseInt(move1.getContent().split("#")[1]);

                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Position");
                msg.addReceiver(player2.aid);
                send(msg);

                gui.getLoggingOutputStream().write(1/*"Main Waiting for movement"*/);
                ACLMessage move2 = blockingReceive();
                gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
                gui.getLoggingOutputStream().flush();
                pos2 = Integer.parseInt(move2.getContent().split("#")[1]);

                punt1 = Integer.parseInt(data[pos1][pos2].toString().split(";")[0]);
                punt2 = Integer.parseInt(data[pos1][pos2].toString().split(";")[1]);
                total1 = total1 + punt1;
                total2 = total2 + punt2;
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(player1.aid);
                msg.addReceiver(player2.aid);
                msg.setContent("Results#" + pos1 + "," + pos2 + "#" + punt1 + "," + punt2);
                send(msg);

                count++;
                if (count == parameters.R) {
                    msg.setContent("EndGame");
                    send(msg);
                    if (total1 > total2)
                        gui.logLine("Player1 " + player1.aid.getName() + " wins! (" + total1 + "-" + total2 + ")");
                    if (total1 < total2)
                        gui.logLine("Player2 " + player2.aid.getName() + " wins! (" + total1 + "-" + total2 + ")");
                    if (total1 == total2) gui.logLine("DRAW! (" + total1 + "-" + total2 + ")");
                    player1.points = player1.points + total1;
                    player2.points = player2.points + total2;
                    return;
                }

                if (parameters.I != 0 && (count % parameters.I) == 0) {
                    int changed = (int) modify();
                    msg.setContent("Changed#" + changed);
                    send(msg);
                }
            }
        }

        public double modify() {

            ArrayList<String> a = new ArrayList<String>();
            int nModificadas = 0, nTotales = parameters.S * parameters.S, i, j, numi, numj;
            if ((float) nModificadas / (float) nTotales >= parameters.P) {
                return nModificadas / nTotales * 100;
            }
            while (true) {
                i = random.nextInt(parameters.S);
                j = random.nextInt(parameters.S);
                if (!a.contains(i + "" + j + "")) {
                    a.add(i + "" + j + "");
                    if (i == j) {
                        numi = random.nextInt(10);
                        numj = numi;
                        data[i][j] = numi + ";" + numj;
                        nModificadas++;
                    } else {
                        a.add(j + i + "");
                        numi = random.nextInt(10);
                        numj = random.nextInt(10);
                        data[i][j] = numi + ";" + numj;
                        data[j][i] = numj + ";" + numi;
                        nModificadas += 2;
                    }
                    if ((double) nModificadas / (double) nTotales >= ((double) parameters.P / 100)) {
                        gui.modifyMatrix(data, parameters.S);
                        return nModificadas / nTotales * 100;
                    }
                }
            }

        }

        @Override
        public boolean done() {
            return true;
        }


    }

    public class PlayerInformation implements Comparable {

        AID aid;
        int id;
        int points;

        public PlayerInformation(AID a, int i) {
            aid = a;
            id = i;
            points = 0;
        }


        @Override
        public boolean equals(Object o) {
            return aid.equals(o);
        }

        @Override
        public int compareTo(Object o) {
            if (((PlayerInformation) o).points < points) return -1;
            if (((PlayerInformation) o).points > points) return 1;
            else return 0;
        }
    }

    public class GameParametersStruct {

        int N;
        int S;
        int R;
        int I;
        int P;

        public GameParametersStruct() {
            N = 4;      //Number of players
            S = 4;      //Matrix dimension
            R = 1000;     //Number of rounds per game
            I = 0;      //Number of rounds until the matrix is changed
            P = 0;     //Percentage of the matrix cells that are changed
        }
    }
}
