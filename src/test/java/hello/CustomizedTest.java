package hello;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.jedis.JedisWorker;
import com.neu.ReadFromRedis;

public class CustomizedTest {

	@Test
	public void test() throws JsonParseException, JsonMappingException, IOException, ProcessingException {
//		String carJson =
//		"{ \"brand\" : \"Mercedes\", \"doors\" : 5," +
//				"  \"owners\" : [\"John\", \"Jack\", \"Jill\"]," +
//				"  \"nestedObject\" : { \"field\" : \"value\" } }";
//		System.out.println(carJson);
//		ObjectMapper om = new ObjectMapper();
//		JsonNode jn = om.readValue(carJson, JsonNode.class);
//		JsonPersist.saveJsonObject(jn, "cars");
//		ObjectMapper om = new ObjectMapper();
//		Jedis j = JedisWorker.getInstance();
//		if(j != null)
//		{
//			if(j.type("cars").equals("hash"))
//			{
//				ObjectNode root = om.createObjectNode();
//			}
//			Set<String> keys = j.keys("cars*");
//			String[] keysArr = new String[keys.size()];
//			if(keys.size() > 0)
//			{
//				int i = 0;
//				for(String str : keys)
//				{
//					keysArr[i] = str;
//					
//				}
//			}
//		}
		//System.out.println(Utils.printJsonObject("planAdvanced1486781191753"));
		JsonNode schema = ReadFromRedis.getSchemaNode("plan");
		String json = ReadFromRedis.printJsonObject("planAdvanced1486781191753");
		System.out.println(json);
		ObjectMapper om = new ObjectMapper();
		JsonNode jso = om.readValue(json, JsonNode.class);
//		JsonNode schemaNode = Utils.getSchemaNode(SCHEMA);
		JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		com.github.fge.jsonschema.main.JsonSchema jsonSchema = factory.getJsonSchema(schema);
		
		ProcessingReport pr = jsonSchema.validate(jso);
		System.out.println(pr.toString());
		
	}

}
