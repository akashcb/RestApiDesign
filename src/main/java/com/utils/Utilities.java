package com.utils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.io.FileUtils;

import com.jedis.JedisWorker;
import com.security.EncryptionAndDecrytion;

public class Utilities {

	
	public static void generateKey() throws NoSuchAlgorithmException, IOException
	{
		File f = new File("resources/secretKey.txt");
		if(f.length() < 1)
		{
			KeyGenerator generator = KeyGenerator.getInstance( "AES" );
			SecretKey key = generator.generateKey();
			byte[] encoded = key.getEncoded();
			FileUtils.writeByteArrayToFile(new File("resources/secretKey.txt"), encoded);
		}
			
		
	}
	
	public static String getAuthCode() throws Exception
	{
		String json_ud = JedisWorker.getInstance().get("user_details");
		String encrypted = EncryptionAndDecrytion.getInstance().encrypt(json_ud);
		System.out.println(encrypted);
		return encrypted;
	}
	

}
