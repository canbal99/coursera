// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockChain {

    private static final boolean DEBUG = false;
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public class BlockUTXOPool {

        private final Block block;
        private UTXOPool utxoPool;

        public BlockUTXOPool(Block b, UTXOPool p) {
            block = b;
            utxoPool = p==null ? new UTXOPool() : new UTXOPool(p);
        }

        public Block getBlock() {
            return block;
        }

        public UTXOPool getUtxoPool() {
            return utxoPool;
        }
        
        public void releaseCoinbaseOutput() {
            for (int i=0;i<block.getCoinbase().numOutputs();i++) {
                UTXO utxo = new UTXO(block.getCoinbase().getHash(),i);
                utxoPool.addUTXO(utxo, block.getCoinbase().getOutput(i));
            }
        }
    }

    public class Node<T> {

        private T data;
        private Node<T> parent;
        private List<Node<T>> children;
        private int height;

        public Node(T data) {
            this.data = data;
            this.children = new ArrayList<Node<T>>();
            this.height = 1;
        }

        public void addNode(Node<T> node) {
            node.height = this.height+1;
            this.children.add(node);
        }

        public T data() {
            return data;
        }
        
        public int height() {
            return height;
        }
    }

    public static final int CUT_OFF_AGE = 10;
    private TransactionPool transactionPool;
    private Node<BlockUTXOPool> maxHeightBlock;
    private HashMap<ByteArrayWrapper,Node<BlockUTXOPool>> hashMapBlockChain;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        if (DEBUG) {System.err.println(" ");}
        if (DEBUG) {System.err.println("BlockChain constructor called");}
        this.transactionPool = new TransactionPool();
        this.hashMapBlockChain = new HashMap<ByteArrayWrapper,Node<BlockUTXOPool>>();
        
        BlockUTXOPool bp = new BlockUTXOPool(genesisBlock, new UTXOPool());
        bp.releaseCoinbaseOutput();
        this.maxHeightBlock = new Node<BlockUTXOPool>(bp);
        ByteArrayWrapper genesisItemHash = new ByteArrayWrapper(this.maxHeightBlock.data.block.getHash());
        this.hashMapBlockChain.put(genesisItemHash, this.maxHeightBlock);
        
        for (Transaction tx : genesisBlock.getTransactions()) { // case 4
            this.transactionPool.removeTransaction(tx.getHash());
        }
        if (DEBUG) {System.err.println("BlockChain constructed with genesis " + bytesToHex(genesisBlock.getHash()));}
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        if (DEBUG) {System.err.println("getMaxHeightBlock called");}
        return this.maxHeightBlock.data().getBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        if (DEBUG) {System.err.println("getMaxHeightUTXOPool called");}
        return this.maxHeightBlock.data().getUtxoPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        if (DEBUG) {System.err.println("getTransactionPool called");}
        return this.transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (DEBUG) {System.err.println("addBlock called " + bytesToHex(block.getHash())
                + " txCount:" + block.getTransactions().size()
                + " coinbase:" + block.getCoinbase().getOutputs().size()
                + " new:" + (block.getCoinbase().getOutputs().size()>0?block.getCoinbase().getOutput(0).value:0)
                + " parent:" + (block.getPrevBlockHash()==null ? "null" : bytesToHex(block.getPrevBlockHash()))
                + "");}
        
        if (block.getPrevBlockHash()==null || block.getPrevBlockHash().length==0) {
            if (DEBUG) {System.err.println("case1 failed");}
            return false; // case 1
        }
        
        ByteArrayWrapper newItemHash = new ByteArrayWrapper(block.getHash());
        if (this.hashMapBlockChain.containsKey(newItemHash )) {
            if (DEBUG) {System.err.println("block already exists");}
            return false;
        }
        
        ByteArrayWrapper parentItemHash = new ByteArrayWrapper(block.getPrevBlockHash());
        Node<BlockUTXOPool> parent = this.hashMapBlockChain.get(parentItemHash);
        if (parent==null) {
            if (DEBUG) {System.err.println("No parent found");}
            return false;
        } else if (parent.height()>CUT_OFF_AGE) {
            if (DEBUG) {System.err.println("CUT_OFF_AGE failed");}
            return false;
        }
        
        // case 3: nothing TODO
        // case 5: nothing TODO
        
        TxHandler txHandler = new TxHandler(parent.data().getUtxoPool());
        Transaction[] validTxArray = txHandler.handleTxs(block.getTransactions().toArray(new Transaction[block.getTransactions().size()]));
        if (validTxArray.length != block.getTransactions().size()) {
            if (DEBUG) {System.err.println("case6 failed");}
            return false; // case 6
        }
        
        for (Transaction tx : block.getTransactions()) { // case 4
            this.transactionPool.removeTransaction(tx.getHash());
        }
        if (DEBUG) {System.err.println("case4 completed");}
        
        BlockUTXOPool bp = new BlockUTXOPool(block, txHandler.getUTXOPool());
        bp.releaseCoinbaseOutput();
        Node<BlockUTXOPool> newTreeItem = new Node<BlockUTXOPool>(bp);
        parent.addNode(newTreeItem);
        this.hashMapBlockChain.put(newItemHash, newTreeItem);
        if (newTreeItem.height() > this.maxHeightBlock.height())
            this.maxHeightBlock = newTreeItem;

        if (DEBUG) {System.err.println("block successfully added");}
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        if (DEBUG) {System.err.println("addTransaction called");}
        this.transactionPool.addTransaction(tx);
    }
}