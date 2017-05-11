package com.neu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.jedis.JedisWorker;

public class JsonPersist {

	//Guidelines:-
	//Subscription_Number: variable named rootObjectIdentification
	//Sub-object or sub-json node number: variable named childObjectIdentification
	private static Random r = new Random();
	
	public static String saveJsonObject(JsonNode node, boolean isRoot, String rootKey)
	{
		System.out.println("Serving json object: "+rootKey);
		Iterator<String> i = node.fieldNames();
		String result = "";
		String objectKey = "";
		
		if(isRoot)
		{
			objectKey += node.get("type").asText() + "_" + (node.has("name") ? node.get("name").asText() : "") + "_" + r.nextInt(1000000);
		}
		else
		{
			objectKey = rootKey;
		}
		
		//create hashmap for current object
		HashMap<String, String> hm = new HashMap<String, String>();
		while(i.hasNext())
		{
			String nodeProperty = i.next().toString();
			JsonNode currentNode = node.get(nodeProperty);

			if(currentNode.isObject() || currentNode.isArray())
			{
				saveJsonArray(currentNode, objectKey + "_" + nodeProperty);
			}
			else
				hm.put(nodeProperty, currentNode.asText());
			
		}
		if(hm.size() > 0)
		{
			result = JedisWorker.saveHashMapRedis(objectKey, hm);
			System.out.println("From saveObject: "+result);
		}
		return objectKey;
	}
	
	public static String saveJsonArray(JsonNode node, String arrayKey)
	{
		ArrayList<String> ls = new ArrayList<String>();
		String objectKey = arrayKey;
		int childNum = 0;
		if(node.isObject())
		{
			String childKey = objectKey + "_child_" + ++childNum;
			String savedChild = saveJsonObject(node, false, childKey);
			ls.add(savedChild);
		}
		else if(node.isArray())
		{
			for(JsonNode jn : node)
			{
				String childKey = objectKey + "_child_" + ++childNum;
				String savedChild = saveJsonObject(jn, false, childKey);
				ls.add(savedChild);
			}
		}
		if(ls.size() > 0)
		{
			String[] rs = new String[ls.size()];
			rs = ls.toArray(rs);
			Long status = JedisWorker.saveListRedis(objectKey, rs);
			System.out.println("Save array status: "+ status);
		}
		return objectKey;
	}
	
	public static boolean deleteJsonObject(String key)
	{
		boolean status = false;
		Long result = JedisWorker.deleteFromRedis(key);
		if(result > 0)
		{
			status = true;
		}
		return status;
	}
	
}
