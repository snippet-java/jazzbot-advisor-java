package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/reply")
public class Reply extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	String sessionId = request.getParameter("sessionId");
    	String userOption = request.getParameter("text").trim();
    	
		JsonObject output = new JsonObject();
    	
    	JsonObject book = AdvisorService.sessionData.get(sessionId)==null?
				new JsonObject():AdvisorService.sessionData.get(sessionId).getAsJsonObject();
		if(!book.has("material"))
			output = AdvisorService.setJazzbotMessage("Please set the book first");
		else {
			if(!book.has("pos")) 
				output = AdvisorService.setJazzbotMessage("Please start the book first");
			else {
				int currentPos = book.get("pos").getAsInt();
				int nextPos = currentPos;
				
				//Get the next position
				JsonArray materials = book.get("material").getAsJsonArray();
				JsonObject material = materials.get(currentPos-1).getAsJsonObject();
				
				if(!material.has("options")) 
					output = AdvisorService.setJazzbotMessage("End of book reached. Please start again the book");
				else {
					JsonObject options = material.get("options").getAsJsonObject();
					System.out.println("options: " + options + "\n" + "selected option: " + userOption + "--");
					if(!options.has(userOption))
						output = AdvisorService.setJazzbotMessage("Invalid option selected. Please try again");
					else {
						nextPos = options.get(userOption).getAsInt();
						output = AdvisorService.getQASet(book, nextPos);
					}
				}
				
				
				//remember the position
				book.addProperty("pos", nextPos);
				AdvisorService.sessionData.add(sessionId, book);
			}
		}
    	
    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(output);
		out.close();
    }
}
