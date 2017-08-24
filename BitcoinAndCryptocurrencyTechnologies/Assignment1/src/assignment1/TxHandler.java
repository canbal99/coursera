package assignment1;

import assignment1.Transaction;
import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {
    
    private UTXOPool utxoPool;

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
        
        for (int i = 0; i < tx.numInputs(); i++) {
            
            Transaction.Input input = tx.getInput(i);
            byte[] prevTransactionHash = input.prevTxHash;
            int prevOutputIndex = input.outputIndex;
            UTXO utxo = new UTXO(prevTransactionHash, prevOutputIndex);
            if (!this.utxoPool.contains(utxo)) { // case (1)
                return false;
            }
            
            Transaction.Output prevOutput = this.utxoPool.getTxOutput(utxo);
            PublicKey pubKey = prevOutput.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubKey, message, signature)) { // case (2)
                return false;
            }
            
            for (int j = i+1; j < tx.numInputs(); j++) {
                Transaction.Input anotherInputInList = tx.getInput(j);
                UTXO anotherUtxo = new UTXO(anotherInputInList.prevTxHash, anotherInputInList.outputIndex);
                if (utxo.equals(anotherUtxo)) { // case (3)
                    return false;
                }
            }
            
            sumOfInputValues += prevOutput.value;
        }
        
        for (int i = 0; i < tx.numOutputs(); i++) {
            
            Transaction.Output output = tx.getOutput(i);
            
            if (output.value<0) { // case (4)
                return false;
            }
            
            sumOfOutputValues += output.value;
        }
        
        if (sumOfInputValues < sumOfOutputValues) { // case (5)
            return false;
        }
        
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
        ArrayList<Transaction> validTransactions = new ArrayList<Transaction>();
        
        for (int i=0; i<possibleTxs.length; i++) {
            Transaction tx = possibleTxs[i];
            if (isValidTx(tx)) {
                validTransactions.add(tx);
                
                for (int j=0; j<tx.numInputs(); j++) {
                    Transaction.Input input = tx.getInput(j);
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    this.utxoPool.removeUTXO(utxo);
                }
                
                for (int j=0; j<tx.numOutputs(); j++) {
                    Transaction.Output output = tx.getOutput(j);
                    UTXO utxo = new UTXO(tx.getRawTx(), j);
                    this.utxoPool.addUTXO(utxo, output);
                }
            }
        }
        
        return (Transaction[]) validTransactions.toArray();
    }

}
