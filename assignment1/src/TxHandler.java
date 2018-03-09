package src;

import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	public UTXOPool utxoPool;
	public static int maxTxSize = 64;
	
    //public TxHandler(UTXOPool utxoPool) {
	public TxHandler(UTXOPool uPool) {
        // IMPLEMENT THIS
		if (uPool == null) {
			utxoPool = new UTXOPool();
		}else {
			utxoPool = new UTXOPool(uPool);
		}
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
        // IMPLEMENT THIS
    	
    	if (tx == null) return false;
    	
		double inputSum = 0;
		
    	for (int i = 0; i < tx.numInputs(); i++) {
        	boolean result1 = false;
    		Transaction.Input in = tx.getInput(i);
			UTXO u = new UTXO(in.prevTxHash, in.outputIndex);

			//(1) all outputs claimed by {@code tx} are in the current UTXO pool, 
    		for (UTXO tUtxo : utxoPool.getAllUTXO() ) {
    			if ( tUtxo.equals(u)) {
    				result1 = true;
    				break;
    			}
    		}
    		if (!result1) return false;
    		
    		//(2) the signatures on each input of {@code tx} are valid, 
    		PublicKey pk = utxoPool.getTxOutput(u).address;
    		if (!Crypto.verifySignature(pk, tx.getRawDataToSign(i), in.signature))
    			return false;
    		
    		//(3) no UTXO is claimed multiple times by {@code tx}
    		for (int j = i+1; j < tx.numInputs(); j++) {
    			Transaction.Input in2 = tx.getInput(j);
    			UTXO u2 = new UTXO(in2.prevTxHash, in2.outputIndex);
    			if (u.equals(u2)) return false;
    		}

			inputSum = inputSum + utxoPool.getTxOutput(u).value;
    	}

		//(4) all of {@code tx}s output values are non-negative, and
		double outputSum = 0;
		for (int i = 0; i < tx.numOutputs(); i++) {
			if (tx.getOutput(i).value < 0) return false;
			outputSum = outputSum + tx.getOutput(i).value;
		}
		
        //(5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
        //    values; and false otherwise.
		if (outputSum > inputSum) return false;
		
    	return true;
    	
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    	
    	ArrayList<Transaction> resultTx = new ArrayList<Transaction>();
    	
    	if ( (possibleTxs.length >= maxTxSize) || (possibleTxs.length==0)) 
    		return null;
    	
    	for (int i=0; i<possibleTxs.length; i++){
    		Transaction tx = possibleTxs[i];
    		if (isValidTx(tx)){
        		byte[] newUtxo = tx.getRawTx();
        		
    			//add new UTXO into Pool
        		for (int index=0; index<tx.getOutputs().size(); index++) {
        			utxoPool.addUTXO(new UTXO(newUtxo,index), tx.getOutput(index));
        		}
    			
    			//delete used TXO from Pool
        		for (int index=0; index<tx.getInputs().size(); index++) {
            		Transaction.Input in = tx.getInput(index);
        			UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
        			utxoPool.removeUTXO(u);
        		}
    		}
    		
    		resultTx.add(tx);
    	}
    	
    	Transaction[] result = new Transaction[resultTx.size()];
    	for (int i=0; i<resultTx.size(); i++)
    		result[i] = resultTx.get(i);
    	
    	return result;
    }

}
