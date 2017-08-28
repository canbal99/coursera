import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MaxFeeTxHandler {
    
    protected UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     * @param utxoPool
     */
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double sumOfInputValues = 0;
        double sumOfOutputValues = 0;
        UTXOPool utxoPoolSpentOnThisTransaction = new UTXOPool();
        
        for (int i = 0; i < tx.numInputs(); i++) {
            
            Transaction.Input input = tx.getInput(i);
            byte[] prevTransactionHash = input.prevTxHash;
            int prevOutputIndex = input.outputIndex;
            UTXO utxo = new UTXO(prevTransactionHash, prevOutputIndex);
            if (!this.utxoPool.contains(utxo)) {
                return false; // case (1)
            }
            
            Transaction.Output prevOutput = this.utxoPool.getTxOutput(utxo);
            PublicKey pubKey = prevOutput.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) {
                return false; // case (2)
            }
            
            if (utxoPoolSpentOnThisTransaction.contains(utxo)) {
                return false; // case (3)
            } else {
                utxoPoolSpentOnThisTransaction.addUTXO(utxo, null);
            }
            
            sumOfInputValues += prevOutput.value;
        }
        
        for (int i = 0; i < tx.numOutputs(); i++) {
            
            Transaction.Output output = tx.getOutput(i);
            
            if (output.value<0) {
                return false; // case (4)
            }
            
            sumOfOutputValues += output.value;
        }
        
        if (sumOfInputValues < sumOfOutputValues) {
            return false; // case (5)
        }
        
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxsEx(Transaction[] possibleTxs) {
        
        ArrayList<Transaction> possibleTransactions = new ArrayList<Transaction>(Arrays.asList(possibleTxs));
        ArrayList<Transaction> validTransactions = new ArrayList<Transaction>();
        Boolean foundOneMoreValidTransaction = true;
        
        while (foundOneMoreValidTransaction) {
            foundOneMoreValidTransaction = false;
            for (int i=0; i<possibleTransactions.size(); i++) {
                Transaction tx = possibleTransactions.get(i);
                // can also use outputs of current transaction, so add them before starting to process
                for (int j=0; j<tx.numOutputs(); j++) {
                    Transaction.Output output = tx.getOutput(j);
                    UTXO utxo = new UTXO(tx.getHash(), j);
                    this.utxoPool.addUTXO(utxo, output);
                }
                if (isValidTx(tx)) {
                    foundOneMoreValidTransaction = true;
                    validTransactions.add(tx);
                    possibleTransactions.remove(i);
                    i = i - 1;
                    // remove unspent outputs that are used in this valid transaction
                    for (int j=0; j<tx.numInputs(); j++) {
                        Transaction.Input input = tx.getInput(j);
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        this.utxoPool.removeUTXO(utxo);
                    }
                } else {
                    // removed previously added outputs of this invalid transaction)
                    for (int j = 0; j < tx.numOutputs(); j++) {
                        Transaction.Output output = tx.getOutput(j);
                        UTXO utxo = new UTXO(tx.getHash(), j);
                        this.utxoPool.removeUTXO(utxo);
                    }
                }
            }
        }
        
        return validTransactions.toArray(new Transaction[validTransactions.size()]);
    }
    
    class TransactionWithFee {
        public Transaction transaction;
        public double fee;
        TransactionWithFee(Transaction tx, double f)  {
            transaction = tx;
            fee = f;
        }
    }
    
    class TransactionWithFeeComparator implements Comparator<TransactionWithFee> {

        @Override
        public int compare(TransactionWithFee txLeft, TransactionWithFee txRight) {
            if ((txLeft.fee - txRight.fee) < 0) {
                return -1;
            } else if ((txLeft.fee - txRight.fee) > 0) {
                return 1;
            }
            return 0;
        }
    }
    
    public ArrayList< ArrayList<TransactionWithFee> > combination(ArrayList<TransactionWithFee> values, int size) {

    if (0 == size || values.isEmpty()) {
        return new ArrayList< ArrayList<TransactionWithFee> >();
    }

    ArrayList<ArrayList<TransactionWithFee>> combination = new ArrayList<ArrayList<TransactionWithFee>>();

    TransactionWithFee actual = values.iterator().next();

    ArrayList<TransactionWithFee> subSet = new ArrayList<TransactionWithFee>(values);
    subSet.remove(actual);

    ArrayList<ArrayList<TransactionWithFee>> subSetCombination = combination(subSet, size - 1);

    for (ArrayList<TransactionWithFee> set : subSetCombination) {
        ArrayList<TransactionWithFee> newSet = new ArrayList<TransactionWithFee>(set);
        newSet.add(0, actual);
        combination.add(newSet);
    }

    combination.addAll(combination(subSet, size));

    return combination;
}
    
    public double calculateFee(Transaction tx) {
        double fee = 0;
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            byte[] prevTransactionHash = input.prevTxHash;
            int prevOutputIndex = input.outputIndex;
            UTXO utxo = new UTXO(prevTransactionHash, prevOutputIndex);
            if (this.utxoPool.contains(utxo)) {
                Transaction.Output output = this.utxoPool.getTxOutput(utxo);
                fee += output.value;
            }
        }
        for (int i = 0; i < tx.numOutputs(); i++) {
            fee -= tx.getOutput(i).value;
        }
        return fee;
    }
    
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
        ArrayList<Transaction> possibleTransactions = new ArrayList<Transaction>(Arrays.asList(possibleTxs));
        ArrayList<TransactionWithFee> possibleTransactionsWithFee = new ArrayList<TransactionWithFee>();
        
        for (int i = 0; i < possibleTransactions.size(); i++) {
            Transaction tx = possibleTransactions.get(i);
            for (int j = 0; j < tx.numOutputs(); j++) {
                Transaction.Output output = tx.getOutput(j);
                UTXO utxo = new UTXO(tx.getHash(), j);
                this.utxoPool.addUTXO(utxo, output);
            }
        }
        
        for (int i = 0; i < possibleTransactions.size(); i++) {
            Transaction tx = possibleTransactions.get(i);
            double fee = calculateFee(tx);
            if (fee >= 0) {
                possibleTransactionsWithFee.add(new TransactionWithFee(tx, fee));
            }
        }
        
        for (int i = 0; i < possibleTransactions.size(); i++) {
            Transaction tx = possibleTransactions.get(i);
            for (int j = 0; j < tx.numOutputs(); j++) {
                Transaction.Output output = tx.getOutput(j);
                UTXO utxo = new UTXO(tx.getHash(), j);
                this.utxoPool.removeUTXO(utxo);
            }
        }
        
        int maxFeeIndex = -1;
        double maxFee = -1;
        ArrayList<ArrayList<TransactionWithFee>> allCombinations = combination(possibleTransactionsWithFee, possibleTransactionsWithFee.size());
        for (int i=0;i<allCombinations.size();i++) {
            ArrayList<TransactionWithFee> list = allCombinations.get(i);
            for (int j=0;j<list.size();i++) {
                
            }
        }
        
        
        
        possibleTransactionsWithFee.sort(new TransactionWithFeeComparator());
        
        Transaction sortedPossibleTxs[] = new Transaction[possibleTransactionsWithFee.size()];
        
        for (int i=0; i<possibleTransactionsWithFee.size(); i++) {
            sortedPossibleTxs[i] = possibleTransactionsWithFee.get(i).transaction;
        }
        
        return handleTxsEx(sortedPossibleTxs);
    }
}
