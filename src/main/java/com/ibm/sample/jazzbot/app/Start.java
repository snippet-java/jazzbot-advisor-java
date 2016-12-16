package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

@WebServlet("/start")
public class Start extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	String sessionId = request.getParameter("sessionId");
    	
    	JsonObject book = AdvisorService.sessionData.get(sessionId)==null?
				new JsonObject():AdvisorService.sessionData.get(sessionId).getAsJsonObject();
		
		JsonObject output = new JsonObject();

		if(!book.has("material"))
			output = AdvisorService.setJazzbotMessage("Please set the book first");
		else {
			int startPos = 1;
			output = AdvisorService.getQASet(book, startPos);
			
			//remember the position
			book.addProperty("pos", startPos);
			AdvisorService.sessionData.add(sessionId, book);
		}
    	
    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(output);
		out.close();
    }
}
