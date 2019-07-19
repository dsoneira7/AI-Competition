Daniel Soneira Rama
47434906M
PSI1

**I don't remember my account from the lab

 - To compile:

        javac -classpath jade.jar *.javac

 - To execute:

        java -classpath jade.jar:. jade.Boot -gui -agents "agent_name:PSI1.class_name;..."

        for example:

                  java -classpath jade.jar:. jade.Boot -gui -agents "Main:PSI1.MainAgent;Random1:PSI1.RandomAgent;fixed1:PSI1.FixedAgent"

        This launches the main agent and two players: a fixed one and a random one.


-------------------------------------------------------------

Summary:

I'm implementing two Intelligencies: Intel0 and Intel1

    Intel0: It implements a algorithm based in Q-Learning. We assume that we have as much options as rows (or columns) the matrix has.
    We have two vectors. One evaluates how valuable is every choice looking at what we know about the matrix, and the other is the one
    that we use to reinforce our learning. It varies looking at the reward we get from every round. How much this reward matters every
    round depends also on the Learning Rate, a parameter that decreases every round. Then we mix those two vectors and we obtain the
    final vector, which we use to make our choice probabilistically.
    The learning rate is also used to evaluate if we should discover or not.
    When the matrix is modified we also modify the learning rate, between other things.

    We adjusted the parameters of the algorithm by trial and mistake:

      double mine = 0.1;        //How much our points are valuable for our choices
      double yours = 0.05;      //How much our opponnent getting points is valuable for us(or not)
      double beta = 0.47;       //Parameter that controls how much important is the "rewards vector" for our choice vector
      double initialLR = 0.8;   //The initial value of the learning Rate
      double minLR= 0.1;        //The minimal value the learning rate can achieve


----------------------------

    Intel1: It implements a statistical algorithm which plays around the opponents choice. We assume that we have as much operations
    as rows (or columns) the matrix has. We have four vectors: one evaluates how valuable is a choice for us looking at what we know
    about the matrix, other evaluates using the same algorithm as the first one how valuable is a choice for our opponent, the third
    makes a registry about how often the opponent makes a choice, and the last ones uses this last two to evaluate which is the most
    probable choice of the opponent.
    New round, we look at what we know about the matrix, if we have a very valuable choice (we know that if the value of this choice
    in our vector is higher than a threshold, defined by a parameter and the size of the matrix) we choose that one. If not we evaluate
    the opponents choices, and if he has a very probable choice (evaluated by another threshold, defined similarly) we play around that,
    choosing the best option if he chooses that. ¿And how do we know if the choice of the opponent is probable or not? We evaluate that,
    by looking at what we know of the matrix, and looking at what he has done before. We assume that the opponent knows hows to play,
    and that a choice in turn 10 is most valuable that in turn 1, because he now knows more about the matrix and how we play. Then,
    we mix those two vectors and get a opponents choice vector.
    If we cant figure out a valuable option for us or the preferent choice of the opponent, we make our choice probabilistically
    looking at our vector.
    20% of every choice is used to discover the matrix. When the matrix is modified the choices of the opponent become valuable again,
    depending on the percentage the matrix has changed.

    We adjusted the parameters of the algorithm by trial and mistake:

      double mine = 0.1;        //How much our points are valuable for our choices
      double yours = 0.05;      //How much our opponnent getting points is valuable for us(or not)
      double beta = 0.18;       //Parameter that controls how much important is the opponent statistical choice vector for the final one
      double gamma;             //This parameters evaluates how important is choice depending on the time of the game
      double initialGamma = 0.1;   //The initial value of gamma
      double maxGamma = 0.4;        //The max value gamma can achieve
      double gammaIncrease = 0.04;  //It specifies how much gamma increases every turn
      double myThreshold=k1/S;      //Specifies the threshold from which we consider a choice is preferible for us. It depends
                                      on the size of the matrix (S) and k1=2;
      double opThreshold=k2/S;      //Specifies the threshold from which we consider a choice would be more frequent on the opponnent.
                                      It depends on the size of the matrix (S) and k2=2;

-----------------------------------------------------

Comments: I used your skeleton to develop the exercise regarding that I did not have much time when
I started in the 1st period.
Both algorithms can be severely improved and perfectioned, and I also realized that maybe it could be useful to fuse some of
their futures together. I also was a bit lost when I confronted the statistical approach algorithm, and I dont know if this what you had
in mind.
Sometimes a "ArrayOutOfBounds..." exception occurs regarding the GUI, but it does not really affect the functioning of the main program
