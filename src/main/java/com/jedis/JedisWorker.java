package com.jedis;

import java.util.HashMap;
import java.util.Set;

import redis.clients.jedis.Jedis;

public class JedisWorker {

	private static Jedis jedis = null;
	
	public static Jedis getInstance()
	{
		if(jedis == null)
		{
			jedis = new Jedis("localhost", 6379);
			//Prod version
			jedis.select(10);
			//Dev version
//			jedis.select(1);
			
			//Test Version
//			jedis.select(10);
		}
		return jedis;
	}
	
	private JedisWorker()
	{
		
	}
	
	public static Long saveListRedis(String key, String[] values)
	{
		if(jedis == null)
		{
			getInstance();
		}
		Long result = 0l;
		for(String str: values)
		{
			result = jedis.lpush(key, str);
		}
		jedis.save();
		return result;
	}
	
	public static String saveHashMapRedis(String key, HashMap<String, String> hm)
	{
		if(jedis == null)
			getInstance();
		String result = jedis.hmset(key, hm);
		jedis.save();
		return result;
	}
	
	public static HashMap<String, String> getHashMapRedis(String key)
	{
		if(jedis == null)
		{
			getInstance();
			
		}
		HashMap<String, String> hm = new HashMap<String, String>();
		hm = (HashMap<String, String>) jedis.hgetAll(key);
		return hm;
	}
	
	public static Long deleteFromRedis(String key)
	{
		if(jedis == null)
		{
			getInstance();
		}
		Set<String> keys = jedis.keys(key+"*");
		String[] strArr = new String[keys.size()];
		int i = 0;
		for(String str : keys)
		{
			strArr[i] = str;
			i++;
		}
		Long result = jedis.del(strArr);
		return result;
	}
	
	public static Long getRootObjectNumber()
	{
		if(jedis == null)
		{
			getInstance();
		}
		Long result = jedis.incr("rootObjectIdentification");
		return result;
	}
	
	public static Long getChildObjectNumber()
	{
		if(jedis == null)
		{
			getInstance();
		}
		Long result = jedis.incr("childObjectIdentification");
		return result;
	}
	
	
	public static void saveIntermittentStrings(String key, String value)
	{
		if(jedis == null)
		{
			getInstance();
		}
		String result = jedis.set(key, value);
		System.out.println(result);
	}
}
