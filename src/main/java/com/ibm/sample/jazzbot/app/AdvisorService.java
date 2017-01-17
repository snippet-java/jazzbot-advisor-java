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
    
    /**
     * Firstly retrieve the location of book (currently from nodered), then load and transform the book content
     * @param urlInput book name input by user
     * @param sessionId user session ID
     * @return book content in json format (elements: message, id, node, options)
     * @throws ClientProtocolException
     * @throws IOException
     */
    protected static JsonArray init(String bookName, String bookUrl) throws ClientProtocolException, IOException {

    	if(bookUrl.isEmpty()) {
    		//todo - now hardcode. Later read from .properties file
    		bookUrl = "http://nodered-reflect-laksri.mybluemix.net/red/flows/";
    	}
    	
    	return transformJsonFromAllFlows(bookUrl);
    }
    

    protected static JsonArray transformJsonFromAllFlows(String bookUrl) {
		
		
		if(!bookUrl.contains("http"))
			return null;

		//------------- To get the survey material (from NodeRed)------------------------
		String result = makeHTTPCall(bookUrl);
		JsonParser parser = new JsonParser();
		JsonArray inputSurvey = parser.parse(result).getAsJsonArray();
		
		
		//-------------- Build up the survey array --------------------------
		
		
		System.out.println("Building up book with ALL chapters");
		 
		ArrayList<JsonObject> chapters = new ArrayList<>();
		ArrayList<JsonObject> switches = new ArrayList<>();
		
		for(JsonElement node : inputSurvey) {
			//Gather all chapters info (id, type, label) from "tab"
			if("tab".equalsIgnoreCase(node.getAsJsonObject().get("type").getAsString()) && 
					node.getAsJsonObject().has("label") && node.getAsJsonObject().get("label").getAsString().toLowerCase().contains("chapter"))
				chapters.add(node.getAsJsonObject());
			
			//Gather all switches (chapter content) -- to optimize the search within filtered data compared to whole flow
			else if("switch".equalsIgnoreCase(node.getAsJsonObject().get("type").getAsString()))
				switches.add(node.getAsJsonObject());
		}
		System.out.println("Total Chapters: " + chapters.size());
		
		if(chapters.size() <= 0)
			return null;
		
		JsonArray book = new JsonArray();
		for(JsonObject chapter : chapters) {
		
			JsonArray inputQASets = new JsonArray();
			for(JsonObject swi : switches) {
				if(swi.get("z").getAsString().equals(chapter.get("id").getAsString()))
					inputQASets.add(swi);
			}
			
			//map each node element to an index number
			ArrayList<String> nodeIndexes = new ArrayList<>();
			for(int i=0; i<inputQASets.size(); i++) {
				JsonObject inputQASet = inputQASets.get(i).getAsJsonObject();
				nodeIndexes.add(inputQASet.get("id").getAsString());
			}
			
			//build chapter component
			JsonObject outputChapter = new JsonObject();
			outputChapter.addProperty("chapterName", chapter.get("label").getAsString());
			
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
						//Catch those scenario where option does not connect to other node
						if(wires.get(j).getAsJsonArray().size() > 0) {
							options.addProperty(rule.get("v").getAsString(), nodeIndexes.indexOf(wires.get(j).getAsJsonArray().get(0).getAsString())+1);
							hasOptions = true;
						}
					}	
				}
				if(hasOptions)
					outputQASet.add("options", options);
				
				outputQASets.add(outputQASet);
			}
			System.out.println("Survey generated: \n" + outputQASets);
			outputChapter.add("material", outputQASets);
			book.add(outputChapter);
		}
		
		return book;
    }

	/**
	 * Get the question and options
	 * @param chapter
	 * @param position
	 * @return
	 */
    protected static JsonObject getQASet(JsonObject book, JsonObject chapter, int position) {
    	
    	JsonObject nextResponse = new JsonObject();
    	
		JsonArray materials = chapter.get("material").getAsJsonArray();
		JsonObject material = materials.get(position-1).getAsJsonObject();

		String responseMessage = "";
		String materialMsg = material.get("message").getAsString();
		if(materialMsg.startsWith("/")) {
			String bookUrl = book.get("bookUrl").getAsString();
			String controlUrl = bookUrl.substring(0, bookUrl.indexOf("/red/flows")) + materialMsg;
			System.out.println("Control executing: " + controlUrl);
			
			String result = makeHTTPCall(controlUrl);
			responseMessage += result;
		}
		else
			responseMessage += material.get("message").getAsString();
		nextResponse.addProperty("message", responseMessage);

		String finalMessage = "";
		if(material.has("description"))
			finalMessage += material.get("description").getAsString();
		
		JsonArray arr = new JsonArray();
		if(material.has("options")) {
			JsonObject options = material.get("options").getAsJsonObject();
			int counter = 1;
			for(Entry<String, JsonElement> option : options.entrySet()) {
				arr.add(counter + " : " + option.getKey());
				counter++;
			}
		}
		else {
			nextResponse.addProperty("end", true);
			finalMessage += "This is the end of Chapter. ";
		}
		nextResponse.addProperty("description", finalMessage);
		
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
    
    protected static String makeHTTPCall(String httpURL) {
    	HttpClient client = HttpClientBuilder.create().build();
		
		HttpGet get = new HttpGet(httpURL);
		HttpResponse resp;
		String result = "";
		try {
			resp = client.execute(get);
			BufferedReader rd = new BufferedReader(
			        new InputStreamReader(resp.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null) {
				result += line;
			}
		} catch (ClientProtocolException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		return result;
    }
    
  //Match param to books.properties. If found, return the matching url. 
    //Otherwise, assume param itself is a valid url
//    private static String mapBookUrl(String bookSelection) throws ClientProtocolException, IOException {
//    	
//    	HttpClient client = HttpClientBuilder.create().build();
//    	HttpGet get = new HttpGet("https://raw.githubusercontent.com/snippet-java/AlexaBooks/master/books.properties");
//    	
//		HttpResponse resp = client.execute(get);
//    	
//		Properties prop = new Properties();
//		prop.load(resp.getEntity().getContent());
//
//		if(prop.containsKey(bookSelection))
//			bookSelection = prop.getProperty(bookSelection);
//
//		return bookSelection;
//    }
    
}
