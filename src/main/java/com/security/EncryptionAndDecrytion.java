package com.security;

import java.io.File;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import com.jedis.JedisWorker;
import com.utils.Utilities;

public class EncryptionAndDecrytion {

	/**
	 * @param args the command line arguments
	 */
//	public static void main(String[] args) throws NoSuchAlgorithmException, Exception {
//		// TODO code application logic here
//
//		//String test="Test";
//		String t = JedisWorker.getInstance().get("user_details");
//		EncryptionAndDecrytion EncodeAndDecode=new EncryptionAndDecrytion();
//		String enrcyptedvalue=EncodeAndDecode.encrypt(t);
//		System.out.println( enrcyptedvalue );
//
//		String decryptedvalue=EncodeAndDecode.decrypt(enrcyptedvalue);
//		System.out.println( decryptedvalue );
//
//	}


	private Key key;

	public Key getKey() {
		return key;
	}

	public void setKey( Key key ) {
		this.key = key;
	}
	public EncryptionAndDecrytion( Key key ) {
		this.key = key;
	}

	public EncryptionAndDecrytion() throws Exception {
		this( generateSymmetricKey() );
	}

	public static Key generateSymmetricKey() throws Exception {
//		KeyGenerator generator = KeyGenerator.getInstance( "AES" );
//		SecretKey key = generator.generateKey();
//		return key;
		Utilities.generateKey();
		SecretKey sk = new SecretKeySpec(FileUtils.readFileToByteArray(new File("resources/secretKey.txt")), "AES");
		return sk;
	}

	private static EncryptionAndDecrytion ed; 
	
	public static EncryptionAndDecrytion getInstance() throws Exception
	{
		if(ed == null)
		{
			ed = new EncryptionAndDecrytion();
		}
		return ed;
	}

	public static byte [] generateRandomBytes() {
		SecureRandom random = new SecureRandom();
		byte [] iv = new byte [16];
		random.nextBytes( iv );
		return iv;
	}

	public String encrypt( String plaintext ) throws Exception {
		return encrypted( generateRandomBytes(), plaintext );
	}

	public  String encrypted(byte [] iv,String test) throws Exception
	{
		byte [] decrypted = test.getBytes();
		byte [] encrypted = encrypt( iv, decrypted );

		StringBuilder ciphertext = new StringBuilder();
		ciphertext.append( Base64.encodeBase64String( iv ) );
		ciphertext.append( ":" );
		ciphertext.append( Base64.encodeBase64String( encrypted ) );

		return ciphertext.toString();
	}

	public byte [] encrypt( byte [] iv, byte [] plaintext ) throws Exception {
		Cipher cipher = Cipher.getInstance( key.getAlgorithm() + "/CBC/PKCS5Padding" );
		cipher.init( Cipher.ENCRYPT_MODE, key, new IvParameterSpec( iv ) );
		return cipher.doFinal( plaintext );
	}

	public String decrypt( String ciphertext ) throws Exception {
		String [] parts = ciphertext.split( ":" );
		byte [] iv = Base64.decodeBase64( parts[0] );
		byte [] encrypted = Base64.decodeBase64( parts[1] );
		byte [] decrypted = decrypt( iv, encrypted );
		return new String( decrypted );
	}

	public byte [] decrypt( byte [] iv, byte [] ciphertext ) throws Exception {
		Cipher cipher = Cipher.getInstance( key.getAlgorithm() + "/CBC/PKCS5Padding" );
		cipher.init( Cipher.DECRYPT_MODE, key, new IvParameterSpec( iv ) );
		return cipher.doFinal( ciphertext );
	}



}