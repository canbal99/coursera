import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

public class TxHandler {
    
    protected UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
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
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
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

}
