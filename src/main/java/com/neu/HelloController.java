package com.neu;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.jedis.JedisWorker;
import com.security.EncryptionAndDecrytion;

import redis.clients.jedis.Jedis;


@RestController
@RequestMapping("/rest")
public class HelloController {

	Jedis jedis = JedisWorker.getInstance();
	HttpHeaders httpHeader = new HttpHeaders();

	@RequestMapping("/welcome")
	public String index() {
		System.out.println("welcome reached:");
		return "Greetings from My App: Akash!";
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<String> readResources(@PathVariable("id") String key, HttpServletRequest request, HttpServletResponse response) throws IOException {

		if(request.getHeader("Authentication") != null && authenticateToken(request.getHeader("Authentication").toString()))
		{
			ResponseEntity<String> rs = null;
			if(JedisWorker.getInstance().hgetAll(key).size() == 0)
			{
				return new ResponseEntity<String>("No records found!", HttpStatus.BAD_REQUEST);
			}
			if(request.getHeader("If-None-Match") != null)
			{
				String etag = request.getHeader("If-None-Match");
				String redisEtag = JedisWorker.getInstance().get(key+"_etag");
				if(etag.equals(redisEtag))
				{
					rs = new ResponseEntity<>("Resource not updated since last checkedin!", HttpStatus.NOT_MODIFIED);
					return rs;
				}
				else
				{
					httpHeader.setContentType(MediaType.APPLICATION_JSON);
					rs = new ResponseEntity<String>(ReadFromRedis.printJsonObject(key), httpHeader, HttpStatus.ACCEPTED);
					return rs;	
				}
			}
			else
			{
				rs = new ResponseEntity<String>(ReadFromRedis.printJsonObject(key), HttpStatus.ACCEPTED);
				return rs;	
			}

		}
		else
			return new ResponseEntity<String>("Authentication Fails! Try Again.", HttpStatus.UNAUTHORIZED);

	}



	@RequestMapping(value = {"", "/"}, method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> addResources(@RequestBody String jn, HttpServletRequest request, HttpServletResponse response) throws IOException, ProcessingException, NoSuchAlgorithmException {
		System.out.println("Reached add:");

		if(request.getHeader("Authentication") != null && authenticateToken(request.getHeader("Authentication").toString()))
		{
			ObjectMapper om = new ObjectMapper();
			JsonNode node = om.readValue(jn, JsonNode.class);

			//Get type of data being persisted
			String type = node.get("type").toString();
			if(type.contains("Subscription"))
				type = "subscription";
			else
				type = "plan";

			//Validate the data against schema available in redis
			JsonNode schemaNode = ReadFromRedis.getSchemaNode(type);
			JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
			com.github.fge.jsonschema.main.JsonSchema jsonSchema = factory.getJsonSchema(schemaNode);
			ProcessingReport pr = jsonSchema.validate(node);
			if(pr.isSuccess())
			{
				//if validation success
				String key = JsonPersist.saveJsonObject(node, true, null);
				System.out.println(key);

				//Hash calculation for etags
				String calculatedHash = calculateHash(jn);
				System.out.println(calculatedHash);
				JedisWorker.getInstance().set(key+"_etag", calculatedHash);

				//Put the new object on Queue for Indexing -- Search API
				if(key.length() > 0)
					goForIndexing(key);

				return new ResponseEntity<String> ("Persisted object key: "+key +" Etag: "+calculatedHash, HttpStatus.OK);
			}
			else //Mention this on console
				return new ResponseEntity<String> ("Schema validation failed: "+pr.toString(), HttpStatus.BAD_REQUEST);
		}
		else
			return new ResponseEntity<String>("Authentication Fails! Try Again.", HttpStatus.UNAUTHORIZED);



	}

	@RequestMapping(value = "/{key}", method = RequestMethod.DELETE)
	@ResponseBody
	public ResponseEntity<String> deleteResources(@PathVariable String key, HttpServletRequest request, HttpServletResponse response) throws IOException {
		System.out.println("Reached delete:");

		if(request.getHeader("Authentication") != null && authenticateToken(request.getHeader("Authentication").toString()))
		{
			boolean status = JsonPersist.deleteJsonObject(key);
			String result = "";
			if(status)
			{
				result = "Success";
				//Remove from indexing as well
				JedisWorker.getInstance().lpush("elasticSearchDeleteQueue", key);
			}

			return new ResponseEntity<String> ("Delete status: " + result, HttpStatus.OK);
		}
		else
			return new ResponseEntity<String>("Authentication Fails! Try Again.", HttpStatus.UNAUTHORIZED);

	}

	private static ObjectMapper om;
	public static ObjectMapper getObjectMapper()
	{
		if(om == null)
			om = new ObjectMapper();
		return om;
	}

	@RequestMapping(value = "/{key}", method = RequestMethod.PATCH)
	@ResponseBody
	public ResponseEntity<String> mergeResources2(@RequestBody String body, @PathVariable("key") String key, 
			HttpServletRequest request, HttpServletResponse response) throws IOException, NoSuchAlgorithmException, ProcessingException {
		System.out.println("Reached merge:");

		if(request.getHeader("Authentication") != null && authenticateToken(request.getHeader("Authentication").toString()))
		{
			boolean result = false;

			//Convert string body to json node
			JsonNode schema = JsonLoader.fromString(JedisWorker.getInstance().get("json-schema-plan"));
			JsonNode bodyNode = JsonLoader.fromString(body);

			if(JedisWorker.getInstance().hgetAll(key).size() == 0)
			{
				return new ResponseEntity<String>("No such key.", HttpStatus.BAD_REQUEST);
			}
			else if(!validateNode(schema, bodyNode))
			{
				return new ResponseEntity<String>("Schema validation fails!", HttpStatus.BAD_REQUEST);
			}
			result = merge(bodyNode, schema, key);

			//Calculate new E-tag
			String updatedEtag = calculateHash(body);
			JedisWorker.getInstance().set(key+"_etag", updatedEtag);

			//Put the key on Queue to update or refresh Indexes on ElasticSearch
			JedisWorker.getInstance().lpush("elasticSearchQueue", key);

			//return response
			return new ResponseEntity<String> ("Merge status: "+result +" Updated Etag: "+updatedEtag, HttpStatus.OK);
		}
		else
			return new ResponseEntity<String>("Authentication Fails! Try Again.", HttpStatus.UNAUTHORIZED);

	}

	@RequestMapping(value = "/saveJsonSchema/{id}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> saveJsonSchema(@RequestBody String body, @PathVariable String id, HttpServletRequest request, HttpServletResponse response) throws IOException {

		if(request.getHeader("Authentication") != null && authenticateToken(request.getHeader("Authentication").toString()))
		{

			String result = JedisWorker.getInstance().set(id, body);
			return new ResponseEntity<String> ("Record persisted as key: " + result, HttpStatus.OK);
		}
		else
			return new ResponseEntity<String>("Authentication Fails! Try Again.", HttpStatus.UNAUTHORIZED);

	}




	public static boolean authenticateToken(String token)
	{
		if(token.contains("Bearer"))
		{
			token = token.trim().replaceAll("Bearer", "").trim();

			String json_user_details = JedisWorker.getInstance().get("user_details");
			String decryptJson = "";
			try 
			{
				decryptJson = EncryptionAndDecrytion.getInstance().decrypt(token);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			return json_user_details.equals(decryptJson);
		}
		else
			return false;
	}

	public static boolean merge(JsonNode payLoad, JsonNode schema, String key) throws ProcessingException
	{
		boolean status = false;
		//Schema validation
		Iterator<String> i = payLoad.fieldNames();
		HashMap<String, String> root = (HashMap<String, String>) JedisWorker.getInstance().hgetAll(key);
		while(i.hasNext())
		{
			String currProp = i.next();
			System.out.println("Merge -> serving root property:"+currProp);
			if(root.containsKey(currProp))
			{
				JedisWorker.getInstance().hset(key, currProp, payLoad.get(currProp).asText());
				status = true;
			}
			else
			{
				System.out.println("Merge -> Going again for: "+currProp);
				status = mergeFunctionality(payLoad.get(currProp), schema.get("properties").get(currProp), key+"_"+currProp);
			}

		}

		return status;
	}




	public static boolean mergeFunctionality(JsonNode payLoad, JsonNode schema, String key) throws ProcessingException
	{
		boolean status = false;

		//Get redis saved object type
		String redisObjType = JedisWorker.getInstance().type(key);

		if(redisObjType.equalsIgnoreCase("hash"))
		{
			Iterator<String> payLoadIterator = payLoad.fieldNames();
			HashMap<String, String> root = (HashMap<String, String>) JedisWorker.getInstance().hgetAll(key);
			while(payLoadIterator.hasNext())
			{
				String currSubProperty = payLoadIterator.next();
				if(root.containsKey(currSubProperty) && (payLoad.get(currSubProperty).getNodeType().equals(JsonNodeType.STRING) || payLoad.get(currSubProperty).getNodeType().equals(JsonNodeType.NUMBER)))
				{
					System.out.println("MergeFunc -> redis hash -> updating: "+key+" prop:"+currSubProperty);
					root.put(currSubProperty, payLoad.get(currSubProperty).asText());
					status = true;
				}

				else if(payLoad.get(currSubProperty).getNodeType().equals(JsonNodeType.OBJECT))
				{
					System.out.println("MergeFunc -> redis hash ->Object Going again For:"+currSubProperty);
					status = mergeFunctionality(payLoad.get(currSubProperty), schema.get("properties").get(currSubProperty), key+"_"+currSubProperty);
				}
				else if(payLoad.get(currSubProperty).getNodeType().equals(JsonNodeType.ARRAY))
				{
					System.out.println("MergeFunc -> redis hash -> Array Going again For:"+currSubProperty);
					status = mergeFunctionality(payLoad.get(currSubProperty), schema.get("properties").get(currSubProperty), key+"_"+currSubProperty);
				}
				else
					status = false;

			}
			JedisWorker.getInstance().hmset(key, root);
		}
		else if(redisObjType.equalsIgnoreCase("list"))
		{
			Long arrayLength = JedisWorker.getInstance().llen(key);
			ArrayList<String> al = (ArrayList<String>) JedisWorker.getInstance().lrange(key, 0, arrayLength - 1);

			//check if redis array contains only one element that's mean its only a sub object really not array
			if(arrayLength == 1 && JedisWorker.getInstance().type(al.get(0)) != null)
			{
				System.out.println("MergeFunc -> redis -> List single object -> Going For: "+al.get(0));
				status = mergeFunctionality(payLoad, schema, al.get(0));
			}
			else
			{
				//check if array contains strings only
				if(schema.get("items").get("type").equals(JsonNodeType.STRING))
				{
					for(JsonNode j : payLoad)
					{
						JedisWorker.getInstance().lpush(key, j.asText());
					}

				}
				//handling objects inside array. This will update first object of array
				else if(schema.get("items").get("properties").getNodeType().equals(JsonNodeType.OBJECT))
				{
					System.out.println("MergeFunc -> redis list -> Going for obj in array");

					Long length = JedisWorker.getInstance().llen(key);
					ArrayList<String> arrayObjKeys = (ArrayList<String>) JedisWorker.getInstance().lrange(key, 0, length);
					
					for(JsonNode j : payLoad)
					{
						//Flags to see if ObjectId is there in the payload
						boolean objectIdPayload = false;
						boolean dataUpdated = false;
						String objectPayloadId = "";
						if(j.has("objectId"))
						{
							objectIdPayload = true;
							objectPayloadId = j.get("objectId").asText();
						}

						if(objectIdPayload && !dataUpdated)
						{
							System.out.println("MergeFunc -> redis list -> ObjectId is thr in payload:"+objectPayloadId);
							// If you want to edit one particular object in an array based on ObjectId
							String childKey = "";

							//ObjectId found in payload. That is just update not create
							boolean foundInRedis = false;
							HashMap<String, String> arrayChild = null;

							for(String s : arrayObjKeys)
							{
								if(!foundInRedis)
								{
									arrayChild = (HashMap<String, String>) JedisWorker.getInstance().hgetAll(s);
									if(arrayChild != null && arrayChild.containsKey("objectId") && arrayChild.get("objectId").equals(objectPayloadId))
									{
										foundInRedis = true;
										childKey = s;
										
										System.out.println("MergeFunc -> redis list -> Going again for:"+childKey);
										status = mergeFunctionality(j, schema.get("items"), childKey);
										
										//updating flags
										dataUpdated = true;
										if(!status)
											System.out.println("MergeFunc -> redis list -> updating failed for: "+childKey);
									}
									else
										System.out.println("MergeFunc -> redis list -> hashmap failed for:"+s);

								}
								else
								{
									System.out.println("MergeFunc -> redis list -> objectId matched with records in redis: "+ objectPayloadId);
									break;
								}
									

							}

						}
						
						if(!dataUpdated)
						{
							//If objectid field not present or objectId field present in payload is not available in redis

							System.out.println("MergeFunc -> redis list -> objectId not found in payload");
							long length_index = JedisWorker.getInstance().llen(key);
							//construct child key
							String childKey = JedisWorker.getInstance().lindex(key, length_index - 1);
							childKey = childKey.substring(0, childKey.length() - 1);
							childKey = childKey + (length_index + 1);
							System.out.println("MergeFunc -> redis list -> child key in array: "+childKey);
							System.out.println("payload: "+ j.toString());
							
							String resKey = JsonPersist.saveJsonObject(j, false, childKey);
							System.out.println("MergeFunc -> redis list -> new array entry created: "+resKey+" under: "+key);
							al.add(childKey);
							JedisWorker.getInstance().lpush(key, childKey);
							status = true;

						}


					}

				}



			}
		}

		return status;
	}


	public static boolean validateNode(JsonNode schema, JsonNode node) throws ProcessingException
	{
		boolean status = false;
		if(schema != null && node != null && (schema.isObject() || schema.isArray()))
		{
			schema = schema.get("properties");
			Set<String> schemaProps = new HashSet<String>();
			Set<String> nodeProps = new HashSet<String>();

			Iterator<String> schemaIter = schema.fieldNames();
			Iterator<String> nodeIter = node.fieldNames();
			while(schemaIter.hasNext())
			{
				String sp = schemaIter.next();
				//System.out.println("Schema: Adding to set: "+sp);
				schemaProps.add(sp);
			}
			while(nodeIter.hasNext())
			{
				String np = nodeIter.next();
				//System.out.println("Node: Adding to set: "+np);
				nodeProps.add(np);
			}

			for(String s : nodeProps)
			{
				//System.out.println("Serving in for: "+s);
				if(!schemaProps.contains(s))
				{
					System.out.println("validateNode -> Does not contain: "+s);
					status = false;
					break;
				}
				else
				{
					if(!schema.get(s).get("type").asText().equalsIgnoreCase(node.get(s).getNodeType().toString()))
					{
						status = false;
						System.out.println("validateNode -> d");
						System.out.println("validateNode -> Value type of "+s+" is inappropriate.");
						break;
					}
					if(schema.get(s).get("type").asText().equalsIgnoreCase("object"))
					{
						System.out.println("validateNode -> Going again for: "+s);
						status = validateNode(schema.get(s), node.get(s));
						if(!status)
						{
							System.out.println("validateNode -> c");
							break;
						}
					}
					else if(schema.get(s).get("type").asText().equalsIgnoreCase("array"))
					{
						for(JsonNode j : node.get(s))
						{
							System.out.println("validateNode -> Going again from array for: "+s);
							status = validateNode(schema.get(s).get("items"), j);
							if(!status)
							{
								System.out.println("validateNode -> a");
								break;
							}

						}
						if(!status)
						{
							System.out.println("validateNode -> b");
							break;
						}

					}
					else
						status = true;
				}
			}
		}
		else
		{
			System.out.println("Schemaa null or node null"+(schema != null ? schema.toString() : node.toString()));
		}

		return status;
	}

	public static String calculateHash(String json) throws NoSuchAlgorithmException
	{
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(StandardCharsets.UTF_8.encode(json));
		return String.format("%032x", new BigInteger(1, md5.digest()));
	}

	public static void goForIndexing(String str)
	{
		JedisWorker.getInstance().lpush("elasticSearchQueue", str);
	}
}
//	@RequestMapping(value = "/test/{key}", method = RequestMethod.PATCH)
//	@ResponseBody
//	public ResponseEntity<String> mergeResourcesTest(@RequestBody String body, @PathVariable("key") String key, 
//			HttpServletRequest request, HttpServletResponse response) throws IOException, NoSuchAlgorithmException, ProcessingException {
//		System.out.println("Reached test merge:");
//		JsonNode bodyNode = JsonLoader.fromString(body);
//		JsonNode schemaNode = JsonLoader.fromString(JedisWorker.getInstance().get("json-schema-plan"));
//		boolean s = validateNode(schemaNode, bodyNode);
//
//		ResponseEntity<String> rs = new ResponseEntity<String>(String.valueOf(s), HttpStatus.ACCEPTED);
//		return rs;
//	}
