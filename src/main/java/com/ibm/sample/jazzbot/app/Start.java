package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

@WebServlet("/start")
public class Start extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	String sessionId = request.getParameter("sessionId");
    	String userInput = request.getParameter("text").trim().toLowerCase();

    	JsonObject output = startSurvey(sessionId, userInput);
    	
    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(output);
		out.close();
    }
    
    protected JsonObject startSurvey(String sessionId, String userInput) {
    	
    	JsonObject book = AdvisorService.sessionData.has(sessionId)?
				AdvisorService.sessionData.get(sessionId).getAsJsonObject():new JsonObject();
		
		JsonObject output = new JsonObject();

		if(!book.has("chapters"))
			output = AdvisorService.setJazzbotMessage("Please set the book first. You can say set book one http://someurl.com");
		else {

			String outputStr = "";
			
			JsonArray chapters = book.get("chapters").getAsJsonArray();
			
			int whichChapter = -1;
			boolean readAll = false;
			boolean random = false;
			//to read all chapters
			if(userInput.contains("all")) {
				whichChapter = 1;
				readAll = true;
				if(userInput.contains("random")) {
					random = true;
					
					List<Integer> remainingChapters = new ArrayList<>();
					for(int i=0; i<chapters.size(); i++) {
						remainingChapters.add(i+1);
					}
					
					Collections.shuffle(remainingChapters);
					whichChapter = remainingChapters.iterator().next();
					
					JsonArray remaining = new JsonParser().parse(new Gson().toJson(remainingChapters)).getAsJsonArray();
					book.add("remaining", remaining);
				}
			}
			
			//to start reading next chapter
			else if(userInput.contains("next")) {

				if(!book.has("reading"))
					outputStr = "Please start the chapter first. You can say start all chapters or to be more specific start chapter one.";
				else {
					if(book.get("random").getAsBoolean()) {
						JsonArray remaining = book.has("remaining")?book.get("remaining").getAsJsonArray():new JsonArray();
						if(remaining.size() > 0) {
							List<Integer> remainingList = new Gson().fromJson(remaining, new TypeToken<List<Integer>>(){}.getType());

							Collections.shuffle(remainingList);
							whichChapter = remainingList.iterator().next();
						}	
						else {
							outputStr = "Thanks for completing all chapters. To start again, you can say start all chapters or more specifically start chapter one. "
									+ "To read another book, you can say use book followed by book name. ";
						}
					}
					else {
						int currentChapter = book.get("reading").getAsInt();
						if(currentChapter >= chapters.size())
							outputStr = "Thanks for completing all chapters. To start again, you can say start all chapters or more specifically start chapter one. "
									+ "To read another book, you can say use book followed by book name. ";
						else
							whichChapter = currentChapter + 1;

					}
					
					readAll = book.get("readAll").getAsBoolean();
					random = book.get("random").getAsBoolean();
				}
			}

			//to read specific chapter
			else {
		    	String[] arr = userInput.toLowerCase().split("\\s+");
		    	String chapterName = "";
		    	for(int i=0; i<arr.length; i++) {
		    		if((i+1)<arr.length) {
		    			//expecting 3rd word to be chapter name 
		    			if(arr[i].equals("chapter")) {
		    				chapterName = arr[i+1];
		    				break;
		    			}
		    		}
		    	}
		    	if(chapterName.isEmpty())
		    		outputStr = "Invalid chapter name. You can say start all chapters or start chapter one.";
		    	else {
		    		
		    		//user gives the chapter sequence
		    		whichChapter = ConvertWordsToNumber.convert(chapterName);

		    		//if not valid chapter sequence, possible that user gives the chapter name
		    		if(whichChapter <= 0) {
		    			for(int i=0; i < chapters.size(); i++) {
		    				JsonObject chapter = chapters.get(i).getAsJsonObject();
		    				if(("chapter " + chapterName).equalsIgnoreCase(chapter.get("chapterName").getAsString())) {
		    					whichChapter = i + 1;
		    					break;
		    				}
		    			}
		    		}
		    		//check if the chapter number is bigger than chapters contained in book.If yes, reset the whichChapter to -1  
		    		else if(whichChapter > chapters.size()) {
		    			outputStr = "Sorry. Cannot find the chapter " + chapterName + ". "
		    					+ "There are only " + chapters.size() + " chapters in this book. Please try again. ";
		    			whichChapter = -1;
		    		}
		    	}
			}
			
			if(whichChapter <= 0)
				output = AdvisorService.setJazzbotMessage(outputStr.isEmpty()?
						"Invalid chapter specified. You can say start all chapters or to be more specific start chapter one":
						outputStr);
			else {
				JsonObject chapter = chapters.get(whichChapter-1).getAsJsonObject();
				int startPos = 1;
				output = AdvisorService.getQASet(book, chapter, startPos);
				book.addProperty("pos", startPos);
				book.addProperty("reading", whichChapter);
				book.addProperty("readAll", readAll);
				book.addProperty("random", random);
				AdvisorService.sessionData.add(sessionId, book);
			}
			
		}
		return output;
    }
}
