package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@WebServlet("/list")
public class List extends HttpServlet {
    private static final long serialVersionUID = 1L;
	
	 @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		JsonObject output = new JsonObject();
		try {
		    String sessionId = request.getParameter("sessionId");
		    String userInput = request.getParameter("text").trim().toLowerCase();

	    	JsonObject book = AdvisorService.sessionData.has(sessionId)?
					AdvisorService.sessionData.get(sessionId).getAsJsonObject():new JsonObject();

			if(userInput == null) 
				output.addProperty("message", "No option found. You can say list books or list chapters");
			else {
				//listing the books in the bookshelf
				if(userInput.contains("book")) {
					if(book.has("bookshelf")) {
						JsonObject bookshelf = book.get("bookshelf").getAsJsonObject();
						JsonArray options = new JsonArray();
						for(Entry<String, JsonElement> entry : bookshelf.entrySet()) {
							options.add("book " + entry.getKey());
						}
						output.addProperty("message", "Available books are as below. ");
						output.add("options", options);
					}
					else
						output.addProperty("message", "No book is available. Please set the book first. You can say set book one http://someurl.com");
				}
				else if(userInput.contains("chapters")) {
					if(book.has("chapters")) {
						JsonArray options = new JsonArray();
						JsonArray chapters = book.get("chapters").getAsJsonArray();
						for(int i=0; i<chapters.size(); i++) {
							JsonObject chapter = chapters.get(i).getAsJsonObject();
							options.add("Chapter " + (i+1) + " : " + chapter.get("chapterName").getAsString());
						}
						output.addProperty("message", "You are reading book " + book.get("bookName").getAsString() + ". ");
						output.addProperty("description", "Available chapters are as below. ");
						output.add("options", options);
					}
					else
						output.addProperty("message", "No chapter found. You can first say use book one. ");
				}
				else
					output.addProperty("message", "No option found. You can say list books or list chapters");
			}
		}
		catch (Exception e) {
			output.addProperty("message", e.getMessage()); 
			e.printStackTrace();
		}
		
		out.println(output);
		out.close();
    }
}
