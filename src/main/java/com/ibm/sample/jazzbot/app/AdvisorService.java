package com.ibm.sample.jazzbot.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AdvisorService {
	
    protected static JsonObject sessionData = new JsonObject();
    
    protected static JsonArray transformJson(String bookUrl) throws ClientProtocolException, IOException {
		
		//------------- To get the survey material (from NodeRed)------------------------
		HttpClient client = HttpClientBuilder.create().build();
		String url = mapBookUrl(bookUrl);
		
		if(!url.contains("http"))
			return new JsonArray();
			
		HttpGet get = new HttpGet(url);


		System.out.println("Collecting json from remote nodered");
		HttpResponse resp = client.execute(get);
		
		BufferedReader rd = new BufferedReader(
		        new InputStreamReader(resp.getEntity().getContent()));

		String result = new String();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result += line;
		}
		JsonParser parser = new JsonParser();
		JsonObject inputSurvey = parser.parse(result).getAsJsonObject();
		
		
		//-------------- Build up the survey array --------------------------
		
		//map each node element to an index number
		System.out.println("Building up survey array");
		JsonArray inputQASets = inputSurvey.get("nodes").getAsJsonArray();
		ArrayList<String> nodeIndexes = new ArrayList<>();
		for(int i=0; i<inputQASets.size(); i++) {
			JsonObject inputQASet = inputQASets.get(i).getAsJsonObject();
			nodeIndexes.add(inputQASet.get("id").getAsString());
		}
		
		//build survey with options sequences
		JsonArray outputQASets = new JsonArray();
		for(int i=0; i<inputQASets.size(); i++) {
			
			JsonObject outputQASet = new JsonObject();
			JsonObject inputQASet = inputQASets.get(i).getAsJsonObject();
			
			String inputName = inputQASet.get("name").getAsString();
			if(inputName.contains("|")) {
				String[] names = inputName.split("\\|");
				outputQASet.addProperty("message", names[0]);
				outputQASet.addProperty("description", names[1]);
			}
			else {
				outputQASet.addProperty("message", inputName);
			}
			outputQASet.addProperty("node", nodeIndexes.indexOf(inputQASet.get("id").getAsString())+1); //+1 to make it more human readable
			outputQASet.addProperty("id", inputQASet.get("id").getAsString());
			
			JsonArray rules = inputQASet.get("rules").getAsJsonArray();
			JsonArray wires = inputQASet.get("wires").getAsJsonArray();
			JsonObject options = new JsonObject();
			boolean hasOptions = false;
			for(int j=0; j<rules.size(); j++) {
				JsonObject rule = rules.get(j).getAsJsonObject();
				if(rule.get("v")!=null) {
					options.addProperty(rule.get("v").getAsString(), nodeIndexes.indexOf(wires.get(j).getAsJsonArray().get(0).getAsString())+1);
					hasOptions = true;
				}	
			}
			if(hasOptions)
				outputQASet.add("options", options);
			
			outputQASets.add(outputQASet);
		}
		System.out.println("Survey generated: \n" + outputQASets);
		
		return outputQASets;
    }
    
    protected static boolean init(String bookUrl, String sessionId) throws ClientProtocolException, IOException {
    	JsonArray qaSets = transformJson(bookUrl);
    	
    	//notify that no book found
    	if(qaSets.size() <= 0)
    		return false;
		
    	JsonObject book = sessionData.get(sessionId)==null?
				new JsonObject():AdvisorService.sessionData.get(sessionId).getAsJsonObject();
		book.add("material", qaSets);
		book.addProperty("bookUrl", bookUrl);
		sessionData.add(sessionId, book);
		
		return true;
    }

	//get the question and options
    protected static JsonObject getQASet(JsonObject book, int position) {
    	JsonObject nextResponse = new JsonObject();
    	
		JsonArray materials = book.get("material").getAsJsonArray();
		JsonObject material = materials.get(position-1).getAsJsonObject();

		nextResponse.addProperty("message", material.get("message").getAsString());

		if(material.has("description"))
			nextResponse.addProperty("description", material.get("description").getAsString());
		
		JsonArray arr = new JsonArray();
		if(material.has("options")) {
			JsonObject options = material.get("options").getAsJsonObject();
			for(Entry<String, JsonElement> option : options.entrySet()) {
				arr.add(option.getKey());
			}
		}
		nextResponse.add("options", arr);
		
		return nextResponse;
    }
    
    //ONLY usage for jazzbot - to send a message to jazzbot
    protected static JsonObject setJazzbotMessage(String message) {
    	JsonObject messageOutput = new JsonObject();
    	
    	messageOutput.addProperty("message", message);
    	messageOutput.add("options", new JsonArray());
    	
    	return messageOutput;
    }
    
  //Match param to books.properties. If found, return the matching url. 
    //Otherwise, assume param itself is a valid url
    private static String mapBookUrl(String bookSelection) throws ClientProtocolException, IOException {
    	
    	HttpClient client = HttpClientBuilder.create().build();
    	HttpGet get = new HttpGet("https://raw.githubusercontent.com/snippet-java/AlexaBooks/master/books.properties");
    	
		HttpResponse resp = client.execute(get);
    	
		Properties prop = new Properties();
		prop.load(resp.getEntity().getContent());

		if(prop.containsKey(bookSelection))
			bookSelection = prop.getProperty(bookSelection);

		return bookSelection;
    }
    
}
