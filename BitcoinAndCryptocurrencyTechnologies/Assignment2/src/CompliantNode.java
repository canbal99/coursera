import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    
    private HashSet<Transaction> pendingTxs;
    private HashSet<Transaction> incomingTxs;
    private HashMap< Integer, HashSet<Transaction> > incomingTxsFromAllNodes;
    private boolean[] nodeFollowees;
    private int numOfRounds = 0;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
        pendingTxs = new HashSet<Transaction>();
        incomingTxs = new HashSet<Transaction>();
        incomingTxsFromAllNodes = new HashMap< Integer, HashSet<Transaction> >();
        numOfRounds = numRounds+1;
    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
        nodeFollowees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        incomingTxs.clear();
        incomingTxs.addAll(pendingTransactions);
        incomingTxs.removeAll(pendingTxs);
        pendingTxs.addAll(incomingTxs);
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
        numOfRounds--;
        return numOfRounds>0 ? incomingTxs : pendingTxs;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
        incomingTxs.clear();
        for (Candidate c : candidates) {
            if (!nodeFollowees[c.sender]) {
                continue;
            }
            
            if (!incomingTxsFromAllNodes.containsKey(c.sender)) {
                incomingTxsFromAllNodes.put(c.sender, new HashSet<Transaction>());
            }
            HashSet<Transaction> transactionsFromNode = incomingTxsFromAllNodes.get(c.sender);
            if (transactionsFromNode.contains(c.tx)) {
                incomingTxsFromAllNodes.remove(c.sender);
                nodeFollowees[c.sender] = false;
                continue;
            } else {
                transactionsFromNode.add(c.tx);
            }
            
            if (!pendingTxs.contains(c.tx)) {
                incomingTxs.add(c.tx);
                pendingTxs.add(c.tx);
            }
        }
    }
}
