package PSI1;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;


//Statistical Approach
public class Intelligent0 extends Agent {

    final static double mine = 0.1; //This is a parameter of how much I value my own points
    final static double yours = 0.05; //This is a parameter of how much I value my opponent obtaining less points
    private static double beta = 0.47; // beta: Parámetro que controla la importancia de alpha.
    private final double InitialLR = 0.8; // This constant saves the initial value of the LearningRate
    Random random = new Random(System.currentTimeMillis());
    private State state;
    private AID mainAgent;
    private int myId, opponentId;
    private int N, S, R, I, P; //Game control parameters
    private ACLMessage msg;
    private double learningRate = InitialLR;
    private double minLR = 0.1; //This marks the minimum learningRate from which the LR cant go lower
    private int[][][] data; //This represents the matrix
    private double[] intel, opponent, intel_basic, opponent_basic, alpha; // Alpha: vector that makes intel vary if the result is good (or not).
                                                                          // The other vectors conform how suitable is every choice (or every opponent choice)
                                                                          // The basic ones just evaluate what we know about the matrix, and the others use alpha to help the agent to choose
    private double p_discovered;
    private int n_discovered;

    protected void setup() {
        state = State.s0NoConfig;

        //Register in the yellow pages as a player
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        sd.setName("Game");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new Play());
        System.out.println("IntelligentAgent " + getAID().getName() + " is ready.");

    }

    protected void takeDown() {
        //Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("IntelligentPlayer " + getAID().getName() + " terminating.");
    }

    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    private class Play extends CyclicBehaviour {
        @Override
        public void action() {
            System.out.println(getAID().getName() + ":" + state.name());
            msg = blockingReceive();
            if (msg != null) {
                System.out.println(getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
                        //Else ERROR
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try {
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) {
                                state = State.s1AwaitingGame;
                            }

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s1AwaitingGame:
                        //If INFORM NEWGAME#_,_ PROCESS NEWGAME --> go to state 2
                        //If INFORM Id#_#_,_,_,_ PROCESS SETUP --> stay at s1
                        //Else ERROR
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                initializeMatrix();
                                boolean gameStarted = false;
                                try {
                                    gameStarted = validateNewGame(msg.getContent());
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                if (gameStarted) state = State.s2Round;
                            }
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s2Round:
                        //If REQUEST POSITION --> INFORM POSITION --> go to state 3
                        //If INFORM CHANGED stay at state 2
                        //If INFORM ENDGAME go to state 1
                        //Else error
                        if (msg.getPerformative() == ACLMessage.REQUEST /*&& msg.getContent().startsWith("Position")*/) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);
                            msg.setContent("Position#" + myGuess()); //
                            System.out.println(getAID().getName() + " sent " + msg.getContent());
                            send(msg);
                            state = State.s3AwaitingResult;
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
                            mod(msg.getContent());
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("EndGame")) {
                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult:
                        //If INFORM RESULTS --> go to state 2
                        //Else error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            //Process results
                            update(msg.getContent());
                            state = State.s2Round;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                }
            }
        }

        /**
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            int tN, tS, tR, tI, tP, tMyId;
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;
            tMyId = Integer.parseInt(contentSplit[1]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 5) return false;
            tN = Integer.parseInt(parametersSplit[0]);
            tS = Integer.parseInt(parametersSplit[1]);
            tR = Integer.parseInt(parametersSplit[2]);
            tI = Integer.parseInt(parametersSplit[3]);
            tP = Integer.parseInt(parametersSplit[4]);

            //At this point everything should be fine, updating class variables
            mainAgent = msg.getSender();
            N = tN;
            S = tS;
            R = tR;
            I = tI;
            P = tP;
            myId = tMyId;
            return true;
        }

        /**
         * Processes the contents of the New Game message
         *
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("NewGame")) return false;
            String[] idSplit = contentSplit[1].split(",");
            if (idSplit.length != 2) return false;
            msgId0 = Integer.parseInt(idSplit[0]);
            msgId1 = Integer.parseInt(idSplit[1]);
            if (myId == msgId0) {
                opponentId = msgId1;
                return true;
            } else if (myId == msgId1) {
                opponentId = msgId0;
                return true;
            }
            return false;
        }

        //It updates the matrix and the vectors we use to decide using the result
        public void update(String result) {
            int mine = 0, his = 1;
            if (myId > opponentId) {
                mine = 1;
                his = 0;
            }
            int row = Integer.parseInt(result.split("#")[1].split(",")[mine]);
            int column = Integer.parseInt(result.split("#")[1].split(",")[his]);
            int mypoints = Integer.parseInt(result.split("#")[2].split(",")[mine]);
            int oppoints = Integer.parseInt(result.split("#")[2].split(",")[his]);
            boolean modified = false;
            if (row == column) {
                if (data[row][column][0] != mypoints) {
                    data[row][column][0] = mypoints;
                    data[row][column][1] = oppoints;
                    n_discovered++;
                    modified = true;
                }
            } else {
                if (data[row][column][0] != mypoints && data[row][column][1] != oppoints) {
                    data[row][column][0] = mypoints;
                    data[row][column][1] = oppoints;
                    data[column][row][0] = oppoints;
                    data[column][row][1] = mypoints;
                    n_discovered += 2;
                    modified = true;
                }
            }
            alpha[row] += learningRate * (mypoints * 0.1 - oppoints * 0.07); //alpha vary depending on our reward, the opponents reward, and the leraningRate
            if (modified) {
                p_discovered = ((float) n_discovered) / ((float) S * S);
                updateValues();
            } else updateValues();

            if (learningRate >= minLR) learningRate -= 0.05; //The LR decreases 0.05 every round
        }

        //This function helps me to calculate the vectors intel and opponent using what we know about the matrix

        public void updateValues() {
            double mytotal = 0, optotal = 0;
            for (int i = 0; i < S; i++) {
                mytotal += getValue(i, true);
                optotal += getValue(i, false);
            }

            for (int i = 0; i < S; i++) {
                intel_basic[i] = getValue(i, true) / mytotal;
                opponent_basic[i] = getValue(i, false) / optotal;
                intel[i] = intel_basic[i] + beta * alpha[i];
            }

            double total_intel = 0;

            for (int i = 0; i < S; i++) {
                total_intel += intel[i];
            }
            for (int i = 0; i < S; i++) {
                intel[i] = intel[i] / total_intel;
            }

            return;
        }

        //Initializes everything we need to play the game when it starts
        public void initializeMatrix() {
            data = new int[S][S][2];
            for (int i = 0; i < S; i++) {
                for (int j = 0; j < S; j++) {
                    for (int k = 0; k < 2; k++) {
                        data[i][j][k] = -1;
                    }
                }
            }
            intel_basic = new double[S];
            opponent_basic = new double[S];
            intel = new double[S];
            opponent = new double[S];
            alpha = new double[S];
            for (int i = 0; i < S; i++) alpha[i] = 0;
            double mytotal = 0;
            updateValues();
            p_discovered = 0;
            n_discovered = 0;
            //printMatrix();
        }


        //This function does easy math to help us calculate the vectors

        public double getValue(int number, boolean row) {
            double total = 0;
            if (row) {
                for (int i = 0; i < S; i++) {
                    if (data[number][i][0] != -1) total += ((mine * data[number][i][0]) - (yours) * data[number][i][1]);
                    else if (number == i)
                        total += 0.225; //For the diagonas: We do not know what we have there, but we do know that we are gonna make the same points as the opponent
                }
            } else {
                for (int i = 0; i < S; i++) {
                    if (data[number][i][0] != -1) total += ((mine * data[i][number][1]) - (yours) * data[i][number][0]);
                    else if (number == i) total += 0.225;
                }
            }

            return total;
        }

        //This function calculates our next choice
        public int myGuess() {
            double max = 0;
            int nmax = 0, contador = 0;
            if (random.nextDouble() > learningRate) { //We use the information we have

                double x = random.nextDouble();
                double threshold = 0;
                for (int i = 0; i < S; i++) { //We make our choice using the probabilities vector.

                    threshold += intel[i];

                    if (x < threshold) break;
                    contador++;
                }

                return contador;


            } else {//We try to discover
                return random.nextInt(S);
            }
        }

        //This function modify the parameters needed every time the MainAgent modify the Matrix.
        private void mod(String s) {
            double perc = Double.parseDouble(s.split("#")[1]) / 100;
            double factor = (1 - perc) - (0.2 * perc);
            if (factor < 0) factor = 0;
            for (int i = 0; i < S; i++) {
                alpha[i] = alpha[i] * factor;
            }
            learningRate += (1 - factor) * (InitialLR - minLR); //We increase the LR an amount proportional to the percentage the matrix has been modified
        }

        //This function used if we want to see how the agent is doing, and what he knows about the matrix. I don't use it now, but I am gonna leave it here because it can be very useful for debugging.
        public void printMatrix() {

            for (int i = 0; i < S; i++) {
                for (int j = 0; j < S; j++) {
                    System.out.print(data[i][j][0] + ";" + data[i][j][1] + "\t");
                }
                System.out.println();
            }

            return;
        }


    }

}
