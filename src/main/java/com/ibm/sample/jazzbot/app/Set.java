package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

@WebServlet("/set")
public class Set extends HttpServlet {
    private static final long serialVersionUID = 1L;
	
	 @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		JsonObject output = new JsonObject();
		try {
		    String sessionId = request.getParameter("sessionId");
			String userInput = request.getParameter("book").trim().toLowerCase();
			
			String[] arr = userInput.split("\\s+");
			String bookname = "";
	    	String bookurl = "";
	    	//user input: set book <bookname> <bookurl>
	    	if(arr.length == 2) {
	    		bookname = arr[0];
	    		bookurl = arr[1];
	    	}
	    	//any other user input than the 2 above
	    	else
	    		output.addProperty("message", "Invalid bookname/bookurl. Please try again. You can say set book one http://someurl.com");
			
	    	if(!bookname.isEmpty() && !bookurl.isEmpty()) {
	    		JsonObject book = AdvisorService.sessionData.has(sessionId)?
	    				AdvisorService.sessionData.get(sessionId).getAsJsonObject():new JsonObject();
				JsonObject bookshelf = book.has("bookshelf")?book.get("bookshelf").getAsJsonObject():new JsonObject();
		    	bookshelf.addProperty(bookname, bookurl);
		    	book.add("bookshelf", bookshelf);
	    		
		    	output.addProperty("message", "Book URL stored successfully. To start reading, you can say use book " + bookname);
	    		AdvisorService.sessionData.add(sessionId, book);
	    	}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		out.println(output);
		out.close();
    }
}
