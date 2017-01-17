package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

@WebServlet("/reply")
public class Reply extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	String sessionId = request.getParameter("sessionId");
    	String userOption = request.getParameter("text").trim().toLowerCase();
    	
		JsonObject output = new JsonObject();
    	
    	JsonObject book = AdvisorService.sessionData.has(sessionId)?
				AdvisorService.sessionData.get(sessionId).getAsJsonObject():null;
    	if(book == null)
			output = AdvisorService.setJazzbotMessage("Please set the book first. You can say set book one http://someurl.com");
		else {
			//Validate if chapter has already been started reading
			if(!book.has("reading"))
				output = AdvisorService.setJazzbotMessage("Please start the chapter first. "
						+ "You can say start all chapters or start chapter one.");
			else {
				
				JsonArray chapters = book.get("chapters").getAsJsonArray();
				
				int whichChapter = book.get("reading").getAsInt();
				JsonObject chapter = chapters.get(whichChapter-1).getAsJsonObject();
				
				boolean startNew = false;
				
				int currentPos = book.get("pos").getAsInt();
				int nextPos = currentPos;
				
				//Get the next position
				JsonArray materials = chapter.get("material").getAsJsonArray();
				JsonObject material = materials.get(currentPos-1).getAsJsonObject();
				
				//Validate if end of node reached or in other words, end of chapter reached
				if(!material.has("options")) {
					
					String message = "";
					
					if(book.get("readAll").getAsBoolean()) {
						
						boolean isBookCompleted = true;
						if(book.get("random").getAsBoolean()) {
							if(book.get("remaining").getAsJsonArray().size() > 0)
								isBookCompleted = false;
						}
						else if(whichChapter < chapters.size())
							isBookCompleted = false;
						
						//if user response is "yes", 
						if(userOption.contains("yes")) {
							if(!isBookCompleted)
								output = new Start().startSurvey(sessionId, "start next chapter");
							else
								output = new Start().startSurvey(sessionId, ("start all chapters" + (book.get("random").getAsBoolean()?" in random":"")));
							
							startNew = true;
						}
						else if(userOption.contains("no")) {
							message += "Thanks for completing " + (isBookCompleted?"all chapters":"the chapter") + ". ";
						}
						else {
							message += "Sorry. I could not understand you. ";
							if(!isBookCompleted)
								message += "Do you want to continue to next chapter? ";
							else
								message += "Thanks for completing all chapters. Do you want to restart again? ";
						}
					}
					else 
						message = "This is the end of chapter. ";
					
					if(!message.isEmpty())
						output = AdvisorService.setJazzbotMessage(message);
				}
				else {
					JsonObject options = material.get("options").getAsJsonObject();
					//if user reply with one, two.. match it with the option answer
					int userOptionPos = ConvertWordsToNumber.convert(userOption);
					if(userOptionPos > 0 && userOptionPos <= options.entrySet().size()) {
						List<Entry<String, JsonElement>> optionsList = new ArrayList<Entry<String, JsonElement>>(options.entrySet());
						userOption = optionsList.get(userOptionPos-1).getKey();
					}
					//If the user input does not match with any available options provided
					if(!options.has(userOption)) {
						
						//if userOption starts with "watson" keyword, call /watson
						if(userOption.startsWith("watson")) {
							String bookUrl = book.get("bookUrl").getAsString();
							String conversationUrl = bookUrl.substring(0, bookUrl.indexOf("/red/flows")) + 
									"/watson?sessionId=" + sessionId + "&text=hi";
							String message = AdvisorService.makeHTTPCall(conversationUrl);
							output = AdvisorService.setJazzbotMessage(message);
						}
						else {
							//repeat again the question if invalid option given
							JsonObject invalidOptionMsg = AdvisorService.setJazzbotMessage("Invalid option selected. Please try again. ");
							output = AdvisorService.getQASet(book, chapter, currentPos);
							output.addProperty("message", invalidOptionMsg.get("message").getAsString() + output.get("message").getAsString());
						}
					}
					else {
						nextPos = options.get(userOption).getAsInt();
						output = AdvisorService.getQASet(book, chapter, nextPos);
						//If end of node/chapter reached
						if(output.has("end")) {
							//for RANDOM mode, remove the read chapter from all available chapter list 
							if(book.get("random").getAsBoolean()) {
								JsonArray remaining = book.has("remaining")?book.get("remaining").getAsJsonArray():new JsonArray();
								List<Integer> remainingList = new Gson().fromJson(remaining, new TypeToken<List<Integer>>(){}.getType());
								
								remainingList.remove(new Integer(whichChapter));
								
								remaining = new JsonParser().parse(new Gson().toJson(remainingList)).getAsJsonArray();
								
								book.add("remaining", remaining);
							}
							
							
							//for ALL mode, check if there is remaining chapter.If yes prompt user whether to continue next chapter.
							//If no, prompt user whether to restart again the book
							if(book.get("readAll").getAsBoolean()) {
								boolean hasNextChapter = false;
								if(book.get("random").getAsBoolean()) {
									JsonArray remaining = book.has("remaining")?book.get("remaining").getAsJsonArray():new JsonArray();
									if(remaining.size() > 0) 
										hasNextChapter = true;
								}
								else {
									int currentChapter = book.get("reading").getAsInt();
									if(currentChapter < chapters.size())
										hasNextChapter = true;
								}
								
								String str = "";
								if(hasNextChapter) 
									str = output.get("description").getAsString() + "Do you want to continue to next chapter? ";
								else 
									str = output.get("description").getAsString() + "Thanks for completing all chapters. Do you want to restart again? ";
								output.addProperty("description", str);
							}
						}
					}
				}
				
				
				//remember the position
				if(!startNew) {
					book.addProperty("pos", nextPos);
					AdvisorService.sessionData.add(sessionId, book);
				}
			}
		}
    	
    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(output);
		out.close();
    }
}
