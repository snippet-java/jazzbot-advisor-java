package com.ibm.sample.jazzbot.app;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;

@WebServlet("/help")
public class Help extends HttpServlet {
    private static final long serialVersionUID = 1L;
	
	 @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
		JsonArray output = new JsonArray();
		output.add("set book &lt;bookname&gt; &lt;bookURL&gt; - Set the book name and its URL");
		output.add("use book &lt;bookname&gt; - Load the book resource with the given name. ");
		output.add("list books - List of books available");
		output.add("list chapters - List of chapters available in the book");
		output.add("start all chapters [in random] - start reading all chapters of book. "
				+ "If \"in random\" is specified, chapters will be started in random order");
		output.add("start chapter [chapter_no]  - start reading the specified chapter number");
		output.add("start next chapter  - start reading the next chapter");
		output.add("reply [number] &lt;option&gt; - return next question and option(s)");
    	
    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		out.println(output);
		
		out.close();
    }
}
