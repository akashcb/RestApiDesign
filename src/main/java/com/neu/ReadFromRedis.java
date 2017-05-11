package com.neu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jedis.JedisWorker;

public class ReadFromRedis {

	private static ObjectMapper om;
	public static ObjectMapper getObjectMapper()
	{
		if(om == null)
			om = new ObjectMapper();
		return om;
	}
	public static String printJsonObject(String key) throws JsonParseException, JsonMappingException, IOException
	{
		JsonNode schemaNode = constructSchemaNode(getSchemaNode("plan"));
		ObjectNode rootNode = createObject(key, schemaNode);
		return rootNode.toString();
	}
	public static JsonNode getJsonObject(String key) throws JsonParseException, JsonMappingException, IOException
	{
		JsonNode schemaNode = constructSchemaNode(getSchemaNode("plan"));
		ObjectNode rootNode = createObject(key, schemaNode);
		return rootNode;
	}

	public static ObjectNode createObject(String key, JsonNode schemaNode) throws JsonParseException, JsonMappingException, IOException
	{
		ObjectNode node = getObjectMapper().createObjectNode();
		System.out.println("RFR -> createObject for: "+key);
		HashMap<String, String> hm = (HashMap<String, String>) JedisWorker.getInstance().hgetAll(key);
		//Get properties from schemaNode

		Iterator<String> i = schemaNode.fieldNames();
		while(i.hasNext())
		{
			String prop = i.next().toString();
			//System.out.println("RFR serving property: "+prop);
			JsonNode jn = schemaNode.get(prop);
			if(jn.get("type").asText().equalsIgnoreCase("object") || jn.get("type").asText().equalsIgnoreCase("array"))
			{
				//get Redis data structure
				String objectType = JedisWorker.getInstance().type(key+"_"+prop);
				
				if(objectType.equalsIgnoreCase("hash"))
				{
					System.out.println("RFR -> createObject -> going again for: "+prop +" object type: "+objectType +" with child key: "+(key+"_"+prop));
					ObjectNode subNode = createObject(key+"_"+prop, schemaNode.get(prop));
					node.put(prop, subNode);
				}
				else if(objectType.equalsIgnoreCase("list"))
				{
					Long length = JedisWorker.getInstance().llen(key+"_"+prop);
					if(length == 1)
					{
						String subObjectKey = JedisWorker.getInstance().lindex(key+"_"+prop, 0);
						System.out.println("RFR -> createObject -> going again for single object: "+prop +" object type: "+objectType +" with child key: "+subObjectKey);
						ObjectNode subNode = createObject(subObjectKey, schemaNode.get(prop).get("properties"));
						node.put(prop, subNode);
					}
					else
					{
						System.out.println("RFR -> createObject -> going again for multiple objects: "+prop +" object type: "+objectType +" with child key: "+key+"_"+prop);
						ArrayNode arraySubNode = createList(key+"_"+prop, schemaNode.get(prop));
						node.put(prop, arraySubNode);
					}


				}
			}
			else
			{
				node.put(prop, hm.get(prop));
			}

		}		

		//System.out.println(_type);
		//		for(String str: hm.keySet())
		//		{
		//			
		//			String objectType = JedisWorker.getInstance().type(hm.get(str));
		//
		//			if(objectType.equalsIgnoreCase("hash"))
		//			{
		//				ObjectNode subNode = createObject(node, hm.get(str));
		//				node.put(str, subNode);
		//			}
		//			else if(objectType.equalsIgnoreCase("list"))
		//			{
		//				ArrayNode subNode = createList(node, hm.get(str));
		//				node.put(str, subNode);
		//			}
		//			else
		//				node.put(str, hm.get(str));
		//		}
		return node;
	}

	public static ArrayNode createList(String key, JsonNode schemaNode) throws JsonParseException, JsonMappingException, IOException
	{
		//System.out.println("Serving "+key);
		Long length = JedisWorker.getInstance().llen(key);
		List<String> al = JedisWorker.getInstance().lrange(key, 0, length - 1);

		ArrayNode an = getObjectMapper().createArrayNode();
		for(String str : al)
		{
			String objectType = JedisWorker.getInstance().type(str);
			System.out.println("Serving subArrayObj: "+str+" : "+ objectType);
			if(objectType.equalsIgnoreCase("hash"))
			{
				ObjectNode subNode = createObject(str, schemaNode.get("items").get("properties"));
				an.add(subNode);
			}
			else if(objectType.equalsIgnoreCase("list"))
			{
				ArrayNode subArrNode = createList(str, schemaNode.get("items").get("properties"));
				an.add(subArrNode);
			}
			else
				an.add(str);
		}
		return an;
	}

	public static JsonNode getSchemaNode(String key) throws JsonParseException, JsonMappingException, IOException
	{
		ObjectMapper om = getObjectMapper();
		String json = "";
		if(key.equalsIgnoreCase("plan")){
			json = JedisWorker.getInstance().get("json-schema-plan");
		}
		else if(key.equalsIgnoreCase("subscription"))
			json = JedisWorker.getInstance().get("json-schema-subscription");
		return om.readValue(json, JsonNode.class);
	}

	public static JsonNode constructSchemaNode(JsonNode n)
	{
		JsonNode properties = n.get("properties");
		return properties;
	}

	public static JsonNode getSchemaObject(JsonNode n)
	{
		return n.get("properties");
	}

	public static JsonNode getSchemaArray(JsonNode n)
	{
		return n.get("items");
	}

}
