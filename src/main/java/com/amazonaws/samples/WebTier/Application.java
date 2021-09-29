package com.amazonaws.samples.WebTier;

public class Application {

	public Application() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Controller control = new Controller();
	  
		
	    while(true) {
		    String output = control.responseController();
		    System.out.println(output);
	    }
	}

}
