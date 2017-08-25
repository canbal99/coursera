import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class MaxFeeTxHandler extends TxHandler  {
    
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

    public MaxFeeTxHandler(UTXOPool utxoPool) {
        super(utxoPool);
    }
    
    @Override
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        
        ArrayList<Transaction> possibleTransactions = new ArrayList<Transaction>(Arrays.asList(possibleTxs));
        ArrayList<TransactionWithFee> possibleTransactionsWithFee = new ArrayList<TransactionWithFee>();
        Boolean foundOneMoreValidTransaction = true;
        
        while (foundOneMoreValidTransaction) {
            foundOneMoreValidTransaction = false;
            for (int i = 0; i < possibleTransactions.size(); i++) {
                Transaction tx = possibleTransactions.get(i);
                double fee = calculateFee(tx);
                if (fee >= 0) {
                    foundOneMoreValidTransaction = true;
                    possibleTransactions.remove(i);
                    possibleTransactionsWithFee.add(new TransactionWithFee(tx, fee));
                }
            }
        }
        
        possibleTransactionsWithFee.sort(new TransactionWithFeeComparator());
        
        Transaction validTxs[] = new Transaction[possibleTransactionsWithFee.size()];
        
        for (int i=0; i<possibleTransactionsWithFee.size(); i++) {
            Transaction tx = possibleTransactionsWithFee.get(i).transaction;
            
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
            
            validTxs[i] = tx;
        }
        
        return validTxs;
    }
    
}
