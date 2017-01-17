package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@WebServlet("/use")
public class Use extends HttpServlet {
    private static final long serialVersionUID = 1L;
	
	 @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		JsonObject output = new JsonObject();
		try {
		    String sessionId = request.getParameter("sessionId");
			String userInput = request.getParameter("text").trim().toLowerCase();
			
			String[] arr = userInput.split("\\s+");
			String bookname = "";
	    	if(arr.length == 2 && "book".equalsIgnoreCase(arr[0])) 
	    		bookname = arr[1];
	    	else
	    		output.addProperty("message", "Invalid bookname. Please try again. You can say use book one. ");
			
	    	if(!bookname.isEmpty()) {
	    		JsonObject book = AdvisorService.sessionData.has(sessionId)?
	    				AdvisorService.sessionData.get(sessionId).getAsJsonObject():new JsonObject();
	    		if(!book.has("bookshelf"))
	    			output.addProperty("message", "No book set yet. Please start by set book " + bookname + " http://someurl.com");
	    		else {
	    			JsonObject bookshelf = book.get("bookshelf").getAsJsonObject();
	    			if(!bookshelf.has(bookname))
	    				output.addProperty("message", "The book " + bookname + " cannot be found. Please set the book first. "
		    					+ "You can say set book " + bookname + " http://someurl.com");
	    			else {
				    	//Load the book content
	    				String bookurl = bookshelf.get(bookname).getAsString();
			    		JsonArray chapters = AdvisorService.init(bookname, bookurl);
						
			    		if(chapters == null)
			    			output.addProperty("message", "Book resource cannot be loaded. Please check again the URL: " + bookurl);
						else {
							book.add("chapters", chapters);
							book.addProperty("bookName", bookname);
							book.addProperty("bookUrl", bookurl);
							
							output.addProperty("message", "Book loaded successfully. To continue, you can say start all chapters or start chapter one");
							AdvisorService.sessionData.add(sessionId, book);
						}
	    			}
	    		}
	    	}
		}
		catch (Exception e) { 
			e.printStackTrace();
		}
		
		out.println(output);
		out.close();
    }
}
