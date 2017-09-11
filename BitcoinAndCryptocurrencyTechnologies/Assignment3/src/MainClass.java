
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author can
 */
public class MainClass {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Hello!!!");
        try {
        mainUnofficial(args);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
        }
        test23();
                
        try {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair rk = keyGen.genKeyPair();
        KeyPair rk2 = keyGen.genKeyPair();

        Block genBlock = new Block(null, rk.getPublic());
        genBlock.finalize();
        BlockChain bc = new BlockChain(genBlock);

        Transaction tx = new Transaction();
        tx.addInput(genBlock.getCoinbase().getHash(), 0);
        tx.addOutput(25, rk2.getPublic());
        tx.finalize();

        Block block2 = new Block(genBlock.getHash(), rk.getPublic());
        block2.addTransaction(tx);
        block2.finalize();
        bc.addBlock(block2);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
        }
    }
    
    public static int test23() { //The method was called test23 back then... this is line 840 on the original file
        System.out.println("Process a transaction, create a block, process a transaction, create a block, ...");
        
        ArrayList<KeyPair> people = new ArrayList<KeyPair>();
        try {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair rk = keyGen.genKeyPair();
        people.add(rk);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.out.println(e.getMessage());
        }

        Block genesisBlock = new Block(null, people.get(0).getPublic());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        boolean passes = true;
        Transaction spendCoinbaseTx;
        Block prevBlock = genesisBlock;

        for (int i = 0; i < 20; i++) {
            spendCoinbaseTx = new Transaction();
            spendCoinbaseTx.addInput(prevBlock.getCoinbase().getHash(), 0);
            spendCoinbaseTx.addOutput(Block.COINBASE, people.get(0).getPublic());
            spendCoinbaseTx.addSignature(new byte[i], 0);
            spendCoinbaseTx.finalize();
            blockHandler.processTx(spendCoinbaseTx);

            Block createdBlock = blockHandler.createBlock(people.get(0).getPublic());

            passes = passes && createdBlock != null;
            passes = passes && createdBlock.getPrevBlockHash().equals(prevBlock.getHash());
            passes = passes && createdBlock.getTransactions().size() == 1;
            passes = passes && createdBlock.getTransaction(0).equals(spendCoinbaseTx);
            prevBlock = createdBlock;
        }
        return 0;//UtilCOS.printPassFail(passes);
    }
    
    
    
    
    
    
    
    
   public static void mainUnofficial(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        /*
         * Generate key pairs, for Scrooge, Alice & Bob
         */
        KeyPair pk_scrooge = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_alice   = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        KeyPair pk_bob     = KeyPairGenerator.getInstance("RSA").generateKeyPair();

	/*
         * Create Genesis block: No TXs, but 1 Coinbase
         */
	Block genesis = new Block(null, pk_scrooge.getPublic());
	genesis.finalize();
	BlockChain bc = new BlockChain(genesis);
	BlockHandler bh = new BlockHandler(bc);

	/*
	 * Create block1 from alice with tx scrooge -> alice
         */
	Block block1 = new Block(genesis.getHash(), pk_alice.getPublic());
        // new TX: scrooge pays 25 coins to alice
        Tx tx1 = new Tx();

        // the genesis block has a value of 25
        tx1.addInput(genesis.getCoinbase().getHash(), 0);

        tx1.addOutput(5, pk_alice.getPublic());
        tx1.addOutput(10, pk_alice.getPublic());
        tx1.addOutput(10, pk_alice.getPublic());

        // There is only one (at position 0) Transaction.Input in tx2
        // and it contains the coin from Scrooge, therefore I have to sign with the private key from Scrooge
        tx1.signTx(pk_scrooge.getPrivate(), 0);

	block1.addTransaction(tx1);
	block1.finalize();

	System.out.println("Block1 Added ok: " + bh.processBlock(block1));

	/*
	 * Create alternative block, block2 from scrooge with tx scrooge -> scrooge
         */
	Block block2 = new Block(genesis.getHash(), pk_scrooge.getPublic());

        // new TX: scrooge pays 25 coins to scrooge
        Tx tx2 = new Tx();
        tx2.addInput(genesis.getCoinbase().getHash(), 0);  //25
        tx2.addOutput(5, pk_scrooge.getPublic());
        tx2.addOutput(10, pk_scrooge.getPublic());
        tx2.addOutput(10, pk_scrooge.getPublic());
        tx2.signTx(pk_scrooge.getPrivate(), 0);

	block2.addTransaction(tx2);
	block2.finalize();
	
	System.out.println("Block2 Added ok: " + bh.processBlock(block2));

	/*
	 * Create new block3 chained to block1 with tx alice -> bob
         */
	Block block3 = new Block(block1.getHash(), pk_scrooge.getPublic());

        // new TX: alice pays 15 coins to bob
        Tx tx3 = new Tx();
        tx3.addOutput(20, pk_bob.getPublic());
        tx3.addInput(tx1.getHash(), 1);	// 10 coins
        tx3.signTx(pk_alice.getPrivate(), 0);
        tx3.addInput(tx1.getHash(), 2);	// 10 coins
        tx3.signTx(pk_alice.getPrivate(), 1);

	block3.addTransaction(tx3);
	block3.finalize();

	System.out.println("Block3 Added ok: " + bh.processBlock(block3));

	/*
	 * Create new block4 chained to block3 with tx bob -> bob
         */
	Block block4 = new Block(block3.getHash(), pk_scrooge.getPublic());

        // new TX: bob splits 15 coins to bob
        Tx tx4 = new Tx();
        tx4.addOutput(10, pk_bob.getPublic());
        tx4.addOutput(5, pk_bob.getPublic());
        tx4.addInput(tx3.getHash(), 0);	// 15 coins
        tx4.signTx(pk_bob.getPrivate(), 0);

	block4.addTransaction(tx4);
	block4.finalize();

	System.out.println("Block4 Added ok: " + bh.processBlock(block4));

	/*
	 * Create new block5 chained to block4 with tx alice -> bob
	 */
	Block block5 = new Block(block4.getHash(), pk_alice.getPublic());
          
	// new TX: alice pays 5+25 coins to bob
        Tx tx5 = new Tx();
        tx5.addOutput(25, pk_bob.getPublic());
        tx5.addInput(tx1.getHash(), 0);	// 5 coins
        tx5.signTx(pk_alice.getPrivate(), 0);
        tx5.addInput(block1.getCoinbase().getHash(), 0); // 25 coins
        tx5.signTx(pk_alice.getPrivate(), 1);

	block5.addTransaction(tx5);
	block5.finalize();
	System.out.println("Block5 Added ok: " + bh.processBlock(block5));

    }


    public static class Tx extends Transaction { 
        public void signTx(PrivateKey sk, int input) throws SignatureException {
            Signature sig = null;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initSign(sk);
                sig.update(this.getRawDataToSign(input));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
            this.addSignature(sig.sign(),input);
            // Note that this method is incorrectly named, and should not in fact override the Java
            // object finalize garbage collection related method.
            this.finalize();
        }
    }
}
