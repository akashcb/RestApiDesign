package hello;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.jedis.JedisWorker;
import com.neu.HelloController;
import com.neu.ReadFromRedis;
import com.security.EncryptionAndDecrytion;
import com.utils.Utilities;

public class SavingJsonRecord {

	@Test
	public void test() throws Exception {
		
//		ReadFromRedis.printJsonObject("plan_Basic_496697");
		//System.out.println(ReadFromRedis.createObject("plan_Basic_496697").toString());
//		JsonNode jn = ReadFromRedis.getSchemaNode("plan");
//		System.out.println(jn.path("properties"));
		//BearerAuthorization.generateKey();
		//JedisWorker.saveIntermittentStrings("", value);a
		System.out.println(Utilities.getAuthCode());
	}
	
	
	//Reads json from file and save it into redis
	//@Test
//	public void test1() throws IOException
//	{
//		FileReader fr = new FileReader(new File("resources/example-token.txt"));
//		BufferedReader br = new BufferedReader(fr);
//		String data;
//		StringBuffer sb = new StringBuffer();
//		
//		while((data = br.readLine()) != null)
//		{
//			sb.append(data);
//		}
//		System.out.println(sb.toString());
//	}
	
	
	@Test
	public void test3() throws Exception
	{
//		EncryptionAndDecrytion ed = new EncryptionAndDecrytion();
//		
//		Key k = ed.generateSymmetricKey();
//		System.out.println(k.serialVersionUID);
		//Utilities.generateKey();
		//Utilities.getAuthCode();
		//qNG+Y6rrlc0ORMSWpC6OhA==:R318qQ8+pQ2nYcdQrYqop4ZYiV7vPHNVEHdrz500Go9vMVX9x3LimX0BwWya5MLzYqTfwrj6Ql5DV2+tay0eVLCG7uSYr1PSl2+jni7Xn9A=
		//HelloController.authenticateToken("qNG+Y6rrlc0ORMSWpC6OhA==:R318qQ8+pQ2nYcdQrYqop4ZYiV7vPHNVEHdrz500Go9vMVX9x3LimX0BwWya5MLzYqTfwrj6Ql5DV2+tay0eVLCG7uSYr1PSl2+jni7Xn9A=");
		//JsonNode schemaN = ReadFromRedis.getSchemaNode("plan").get("properties");
		//HelloController.getRedisKey(schemaN, "plan_Basic_496697", "_name");
		
	}
	
	
	@Test
	public void test4() throws JsonParseException, JsonMappingException, IOException, ProcessingException
	{
		//Validating schema while merge or patch
		
//		String schema = JedisWorker.getInstance().get("json-schema-plan");
		JsonNode schemaNode = ReadFromRedis.getSchemaNode("plan");
//		String json = ReadFromRedis.printJsonObject("plan__194519");
//		ObjectMapper om = new ObjectMapper();
//		JsonNode node = om.readValue(json, JsonNode.class);
		
//		JsonNode node = ReadFromRedis.getJsonObject("plan__194519");
//		JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
//		com.github.fge.jsonschema.main.JsonSchema jsonSchema = factory.getJsonSchema(schemaNode.get("properties").get("planCostShares"));
//		ProcessingReport pr = jsonSchema.validate(node.get("linkedPlanServices"));
//		if(pr.isSuccess())
//		{
//			System.out.println("success");
//		}
//		else
//		{
//			System.out.println(pr);
//			System.out.println("failed");
//		}
	}
	
	
	@Test
	public void test5() throws JsonParseException, JsonMappingException, IOException, ProcessingException
	{
//		System.out.println("abcd");
//	System.out.println(JedisWorker.getInstance().hgetAll("ascd"));	
//		ObjectMapper om = new ObjectMapper();
//		JsonSchemaGenerator jg = new JsonSchemaGenerator(om);
//		JsonSchema js = jg.generateSchema(JsonNode.class);
//		
		JsonNode schema = JsonLoader.fromString(JedisWorker.getInstance().get("json-schema-plan2"));
		JsonNode jn = JsonLoader.fromString(JedisWorker.getInstance().get("timepasexamplebad"));
		JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		JsonValidator v = factory.getValidator();
		System.out.println(schema.toString());
		System.out.println(jn.toString());
		ProcessingReport pr = v.validate(schema, jn);
		System.out.println(pr);
	}
	
	
	
	
	

}
