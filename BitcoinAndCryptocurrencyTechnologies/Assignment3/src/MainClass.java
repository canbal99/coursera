
import java.security.KeyPair;
import java.security.KeyPairGenerator;

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
}
