package com.amazonaws.samples.WebTier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.codec.binary.Base64;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;

public class Controller {

	public Controller() {
		// TODO Auto-generated constructor stub
	}
	
	public Integer numOfInstances = 0;
	final public Integer maxInstances = 19;
	public Integer numOfMsgInRequestQueue = 0;
	
	
	
	Ec2Service ec2Service = new Ec2Service();
	
	SQSService sqsService = new SQSService();
	
	AWSConstants constants = new AWSConstants();
	
	private ArrayList<String> flag = new ArrayList<>();
	private Dictionary response_dict = new Hashtable();
	
	public void createEc2Instance() {
		ec2Service.createInstance();
		numOfInstances = ec2Service.getNumberOfInstances();
	}
	
	public void startEc2Instance(String insId) {
		ec2Service.startInstance(insId);
		numOfInstances = ec2Service.getNumberOfInstances();
	}
	
	public void stopEc2Instance(String insId) {
		ec2Service.stopInstance(insId);
		numOfInstances = ec2Service.getNumberOfInstances();
	}
	
	public void terminateEc2Instance(String insId) {
		ec2Service.terminateInstance(insId);
		numOfInstances = ec2Service.getNumberOfInstances();
	}
	
	public void sendQueueMessage(String encoded_message,String queueName,String image_name) {
		sqsService.sendMessage(encoded_message, queueName,image_name);
		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
	}
	
	public void deleteQueueMessage(String queueName,Message msg) {
		sqsService.deleteMessage(queueName, msg);
		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
	}
	
	public Message receiveQueueMessage(String queueName) {
		return sqsService.receiveMessage(queueName);
	}
	
	public CreateQueueResult createQueue(String queueName) {
		return sqsService.createQueue(queueName);
	}

	public void autoscale() {
		class autoScaleThread extends Thread {

		    public void run(){
		    	Integer countTo10 = 0;
		    	Integer countTo12 = 0;
		    	
		    	while(true) {
					//One Ec2 Instance shouldn't be for the App instances
		    		//numOfInstances = ec2Service.getNumberOfInstances();
		    		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
		    		
		    		
					//Integer numOfAppInst = ((numOfInstances == 0) ? 0 : ec2Service.listOfIDs.size());
					Integer numOfAppInst = ec2Service.listOfIDs.size();
					Integer numOfRunningInst = ec2Service.listOfIDsRunning.size();
					Integer numOfAvailInst = maxInstances - numOfAppInst;
					//Integer msg_counter = numOfMsgInRequestQueue;
					
					//SCALING UP
					if (numOfMsgInRequestQueue > numOfRunningInst && numOfRunningInst < maxInstances) {
						
						//Restarting Stopped Instances
						while(ec2Service.listOfIDsStopped.size() > 0 
								&& numOfMsgInRequestQueue > numOfRunningInst)
						{
							try {
								String stopInstanceID = ec2Service.listOfIDsStopped.get(0);
								ec2Service.startInstance(stopInstanceID);
								numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
								numOfRunningInst = ec2Service.listOfIDsRunning.size();
							}
							catch(AmazonEC2Exception EC2Excep)
							{
								System.out.println("Attempted to Stop an EC2 Instance in Initialization");
							}
						}
						//Creating Instances. Once reach the max number, this while
						//loop is never entered
						while(numOfMsgInRequestQueue > numOfAppInst && numOfAvailInst > 0)
						{
							try {
								createEc2Instance();
								numOfAvailInst = maxInstances - ec2Service.listOfIDs.size();
								numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
								numOfAppInst = ec2Service.listOfIDs.size();
							}
							catch(AmazonEC2Exception EC2Excep)
							{
								System.out.println("Attempted to Create an EC2 Instance but Failed");
							}
						}
						
						countTo10 = 0;
						countTo12 = 0;
						
						//I am unsure of whether the Instances are deleted automatically upon ending of Image Processing
					} 
					//SCALING DOWN
					else if (numOfMsgInRequestQueue + 5 < numOfRunningInst && numOfRunningInst > 0) { //The plus 5 is to give it some leaway, there's a counter so that if it's less than that for more than ~10 seconds it will still go down
						for(int i = 0; i < numOfRunningInst - (numOfMsgInRequestQueue + 5); i++) {
							//TODO: Hypothetically if would go through and see which are no longer being used and take out the numOf difference-1 
							//of msg in queue and numOfAppInst, but if it's automatically taken out, no need?
							
							//My work around is that the instances need to be stopped when they are done executing. These are put in a list of stopped and from there they should be terminated. 
							//We then terminate the first couple determined by: numOfAppInst - (numOfMsgInRequestQueue + 5), which should be a positive integer because of the else if.
							try
							{
								if(ec2Service.listOfIDsRunning.size() > 0)
								{
									String runningInstanceID = ec2Service.listOfIDsRunning.get(0);
									ec2Service.stopInstance(runningInstanceID);
									numOfRunningInst = ec2Service.listOfIDsRunning.size();
								}
								//numOfInstances = ec2Service.getNumberOfInstances();
					    		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
							}
							catch(AmazonEC2Exception EC2Excep)
							{
								System.out.println("Attempted to Stop an EC2 Instance in Initialization");
							}
						}
						countTo10 = 0;
						countTo12 = 0;
						//control.terminateEc2Instance(String.valueOf(?));
					} else if (numOfMsgInRequestQueue + 2 < numOfRunningInst && numOfRunningInst > 0) {
						countTo10++;
						if (countTo10 == 10) {
							for(int i = 0; i < numOfRunningInst - (numOfMsgInRequestQueue + 2); i++) {
								
								try
								{
									if(ec2Service.listOfIDsRunning.size() > 0)
									{
										String runningInstanceID = ec2Service.listOfIDsRunning.get(0);
										ec2Service.stopInstance(runningInstanceID);
										numOfRunningInst = ec2Service.listOfIDsRunning.size();
									}
									//numOfInstances = ec2Service.getNumberOfInstances();
						    		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
								}
								catch(AmazonEC2Exception EC2Excep)
								{
									System.out.println("Attempted to Stop an EC2 Instance in Initialization");
								}
							}
				    		countTo10 = 0;
				    		countTo12 = 0;
						}
					} else if (numOfMsgInRequestQueue < numOfRunningInst && numOfRunningInst > 0) {
						countTo12++;
						if (countTo12 == 12) {
							for(int i = 0; i < numOfRunningInst - (numOfMsgInRequestQueue + 2); i++) {
								
								try
								{
									if(ec2Service.listOfIDsRunning.size() > 0)
									{
										String runningInstanceID = ec2Service.listOfIDsRunning.get(0);
										ec2Service.stopInstance(runningInstanceID);
										numOfRunningInst = ec2Service.listOfIDsRunning.size();
									}
									//numOfInstances = ec2Service.getNumberOfInstances();
						    		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
								}
								catch(AmazonEC2Exception EC2Excep)
								{
									System.out.println("Attempted to Stop an EC2 Instance in Initialization");
								}
							}
							countTo10 = 0;
							countTo12 = 0;
						}
					}
					//Guarantee Shutdown once all messages are processed
					else if(numOfMsgInRequestQueue == 0)
					{
						try
						{
							if(ec2Service.listOfIDsRunning.size() > 0)
							{
								int i = 0;
								int size = ec2Service.listOfIDsRunning.size();
								
								while(i < size)
								{
									String runningInstanceID = ec2Service.listOfIDsRunning.get(i);
									ec2Service.stopInstance(runningInstanceID);
									numOfRunningInst = ec2Service.listOfIDsRunning.size();
									i++;
								}
							}
							//numOfInstances = ec2Service.getNumberOfInstances();
				    		numOfMsgInRequestQueue = sqsService.getNumberOfMsgs(constants.queueName1);
						}
						catch(AmazonEC2Exception EC2Excep)
						{
							System.out.println("Attempted to Stop an EC2 Instance in Initialization");
						}
					}
					
					try {
						Thread.sleep(4000); //1 second balances the count and wait for the instances so that they aren't gotten rid of prematurely
					} catch (Exception error) {
						System.out.print("AutoScaleError");
						
					} finally {
						
					}
				}
		       
		    }
		  }
		//
		autoScaleThread ast = new autoScaleThread();
		ast.start();
		
	}
	
	public static String image_to_text(String pathname)
	{
		File file = new File(pathname);
		String imageDataString = null;
		try {            
            // Reading a Image file from file system
            FileInputStream imageInFile = new FileInputStream(file);
            byte imageData[] = new byte[(int) file.length()];
            imageInFile.read(imageData);
 
            // Converting Image byte array into Base64 String
            imageDataString = encodeImage(imageData);
 
            imageInFile.close();
 
            System.out.println("Image Successfully Manipulated!");
        } catch (FileNotFoundException e) {
            System.out.println("Image not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading the Image " + ioe);
        }
		return imageDataString;
	}
	
	public static String encodeImage(byte[] imageByteArray) {
        return Base64.encodeBase64URLSafeString(imageByteArray);
    }
	
	//dealing with input request and sending output response to users
	public String responseController() {
		//createQueue(queue1);
		//String img_path = "/home/ubuntu/upload_images";
		String img_path;
		File input_img = new File(AWSConstants.USR_IMG_DIR);
		String contents[] = input_img.list();
		boolean new_image = false;
		for(String next_image : contents) {
			if(flag.size()!=0 && !flag.contains(next_image))
			{
				new_image = true;
				img_path = AWSConstants.USR_IMG_DIR + "/" + next_image;
				flag.add(next_image);
				String encoded_input_img = image_to_text(img_path);
				sendQueueMessage(encoded_input_img ,AWSConstants.REQUEST_NAME,next_image);
			}
			else if(flag.size() == 0)
			{
				new_image = true;
				img_path = AWSConstants.USR_IMG_DIR + "/" + next_image;
				flag.add(next_image);
				String encoded_input_img = image_to_text(img_path);
				sendQueueMessage(encoded_input_img ,AWSConstants.REQUEST_NAME,next_image);
			}
		}
		
		String outputBody = "";
		Message outputResponse = receiveQueueMessage(AWSConstants.RESPONSE_NAME);
		if (outputResponse != null) {
			outputBody = outputResponse.getBody();
			if(outputBody.contains(","))
			{
				String image_name = outputBody.substring(1, outputBody.indexOf(",")).trim();
				response_dict.put(image_name, outputBody);
				//Write to Dictionary File
				FileWriter fw = null;
				BufferedWriter bw = null;
				try {
					fw = new FileWriter(AWSConstants.RESP_DICT_DIR, true);
					bw = new BufferedWriter(fw);
					if(response_dict.size() > 1)
						bw.newLine();
				    bw.write(image_name + " : " + outputBody);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally {
					if(bw != null)
					{
						try {
							bw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}    
			}
			deleteQueueMessage(AWSConstants.RESPONSE_NAME,outputResponse);
		}
		else
		{
			try {
				Thread.sleep(4000);
			}
			catch(Exception e)
			{}
		}
		
		return outputBody;
	}
	
}