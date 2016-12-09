package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/set")
public class Set extends HttpServlet {
    private static final long serialVersionUID = 1L;
	
	 @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    	response.setContentType("html/text");
		PrintWriter out = response.getWriter();
		 
		String output = "";
		try {
		    String sessionId = request.getParameter("sessionId");
			String book = request.getParameter("book");
			AdvisorService.init(book, sessionId);
	    	
			output += "Success";
		}
		catch (Exception e) {
			output += "Exception caught: " + e.getMessage(); 
		}
		
		out.println(output);
		out.close();
    }
}
