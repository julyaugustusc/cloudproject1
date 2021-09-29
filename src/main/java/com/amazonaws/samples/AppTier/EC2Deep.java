package com.amazonaws.samples.AppTier;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

/*
 * EC2Deep is a class designed for running
 * on EC2 Instances that perform deep learning.
 * The EC2Deep is instantiated from given SQS queue
 * URLs of the request and response queue. The credentials
 * are established from the access and secret key and the names
 * of the S3 input and output buckets are provided to
 * be written to by the EC2 instances
 */
public class EC2Deep {
	String request_url, response_url;
	AWSCredentials credentials;
	AmazonSQS sqs;
	DeepBucket input, output;
	
	/*
	 * RAN BY THE WEB TIER TO PASS IMAGE INTO
	 * SQS Request Queue
	 */
	public void sendRequestImageFile(String img_name, String encoded_img)
	{
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		messageAttributes.put("ImageName", new MessageAttributeValue()
		  .withStringValue(img_name)
		  .withDataType("String"));
		
		SendMessageRequest imageRequest = new SendMessageRequest()
				  .withQueueUrl(request_url)
				  .withMessageBody(encoded_img)
				  .withMessageAttributes(messageAttributes);
		sqs.sendMessage(imageRequest);
	}
	//CHANGE THIS PICTURES VARIABLE TO INCLUDE THE PATH YOU WANT
	//THIS WAS ONLY FOR TESTING PURPOSES
	final static String PICTURES = "C:/Users/jchan/Pictures";
	final static String LNX_IMG_SAVE_DIR = "/home/ubuntu/images/";
	final static String IMG_SAVE_DIR = "C:/Users/jchan/Pictures/Hearthstone/";
	
	public EC2Deep(String access_key, String secret_key, String req_url, String resp_url,
			String input_bucket, String output_bucket)
	{
		request_url = req_url;
		response_url = resp_url;
		credentials = new BasicAWSCredentials(
				  access_key, 
				  secret_key
				);
		sqs = AmazonSQSClientBuilder.standard()
				  .withCredentials(new AWSStaticCredentialsProvider(credentials))
				  .withRegion(Regions.US_EAST_1)
				  .build();
		input = new DeepBucket(access_key,secret_key,input_bucket);
		output = new DeepBucket(access_key,secret_key,output_bucket);
	}
	//Send request message to SQS request queue
	public void sendRequestMessage(String req_msg)
	{
		sqs.sendMessage(request_url,req_msg);
	}
	public void sendRequestMessage(String queue_name,String req_msg)
	{
		String queue_url = sqs.getQueueUrl(queue_name).getQueueUrl();
		System.out.println("QUEUE URL " + queue_url);
		sqs.sendMessage(queue_url,req_msg);
	}
	//Send response message to SQS response queue
	public void sendResponseMessage(String resp_msg)
	{
		sqs.sendMessage(response_url,resp_msg);
	}
	/*
	 * receiveRequestMessage receives the message
	 * from the SQS request queue. It then packages
	 * the information from the message such as the
	 * content and the receipt into a ReceivedMessage
	 * which is used later for deleting the message and
	 * reading the contents of the message.
	 */
	public ReceivedMessage receiveRequestMessage()
	{
		ReceivedMessage req_msg = null;
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(request_url)
				  .withWaitTimeSeconds(10)
				  .withMaxNumberOfMessages(1);
		List<Message> request_msgs = sqs.receiveMessage(receiveMessageRequest.withMessageAttributeNames("All")).getMessages();
		if (request_msgs != null && !request_msgs.isEmpty()){
			req_msg = new ReceivedMessage(request_msgs.get(0).getBody(),
					request_msgs.get(0).getReceiptHandle(),request_msgs.get(0).getMessageAttributes().get("ImageName").getStringValue());
		}
		
		return req_msg;
	}
	/*****************************************************************************
	 ***  The Below Process Image functions were created for testing           ***
	 ***  While some process image functions may seem redundant, these were    ***
	 ***  designed to initially send strings back to the SQS queue and then    ***
	 ***  adapted to process image files itself.                               ***
	 *****************************************************************************/
	
	/*
	 * Test function that takes in an arbitrary
	 * string and stores the name and length of the string.
	 * This information is stored to an S3 output bucket with
	 * the key being the name of the image
	 */
	public void process_image(String image)
	{
		String image_len = "STR NAME: " + image + "\nLENGTH: " + image.length();
		output.store_image(image, image_len);
		
		sendResponseMessage(image_len);
	}
	//deletes message from SQS request queue
	public void deleteRequestMessage(String receipt)
	{
		sqs.deleteMessage(new DeleteMessageRequest()
				  .withQueueUrl(request_url)
				  .withReceiptHandle(receipt));
	}
	/*
	 * This process image utilizes the receiveRequestMessage
	 * function to extract a message from the request SQS queue.
	 * The content and receipt are extracted and then the processed
	 * string result is sent to the response SQS queue.
	 */
	public void process_image()
	{
		ReceivedMessage image_req = receiveRequestMessage();
		String image = image_req.get_content();
		String receipt = image_req.get_receipt();
		
		String image_len = "STR NAME: " + image + "\nLENGTH: " + image.length();
		output.store_image(image, image_len);
		
		sendResponseMessage(image_len);
		deleteRequestMessage(receipt);
	}
	/*
	 * Save image takes in a given String path
	 * where the image from the request queue is
	 * decoded to its original image file and stored
	 * in the specified path.
	 * 
	 * TODO: Update this functionality to correctly
	 * send output pairs to output S3 bucket. In addition
	 * implement the processing of the image to utilize
	 * the deep learning model and then store the image output
	 * pairs
	 */
	public void save_image(String path)
	{
		ReceivedMessage image_req = receiveRequestMessage();
		String image = image_req.get_content();
		String receipt = image_req.get_receipt();
		
		System.out.println("STRING LENGTH RECEIVED: " + image.length());
		ImageProcessing.text_to_image(path, image);
		//output.store_image(image, image_len);
		
		//sendResponseMessage(image_len);
		deleteRequestMessage(receipt);
	}
	
	/********************************************************************************
	 ***  The Below Process Deep Learning Test functions were created for testing ***
	 ***  creating a process in java that executes python code. For example the   ***
	 ***  python code to be executed is a simple Hello World function but will    ***
	 ***  be extended to run the image classifier                                 ***
	 ********************************************************************************/
	
	/*
	 * TODO: Handle exceptions for file
	 * writing and reading
	 */
	public void deep_learning(String img_path, String img_name)
	{
		
		ReceivedMessage image_req = receiveRequestMessage();
		String image = image_req.get_content();
		String receipt = image_req.get_receipt();
		
		ImageProcessing.text_to_image(img_path, image);
		File input_img = new File(img_path);
		input.store_image(img_name, input_img);
		
		//Execute Deep Learning Process
		String classifcation_out = null;
		Process p;
		try {
			/*
			p = new ProcessBuilder("/bin/bash", "-c", "cd /home/ubuntu/classifier/"
					+ " && python3 image_classification.py " + img_path).start();
			*/
			//Windows only for testing
			p = new ProcessBuilder("CMD", "/C", "cd " + PICTURES + " && python hello.py").start();
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			classifcation_out = br.readLine();
			p.destroy();
		} catch (Exception e) {
		}
		if(classifcation_out != null)
		{
			output.store_image(img_name,classifcation_out);
		}
		String output_pair = "(" + img_name + ", " + classifcation_out + ")";
		sendResponseMessage(output_pair);
		deleteRequestMessage(receipt);
	}
	/*
	 * TODO: Currently the S3 commands to write to the
	 * S3 bucket are commented out due to the errors 
	 * received due to lack of certain packages. Once
	 * this error is resolved uncomment these functions
	 * so that the images and output pairs can be stored
	 */
	public void deep_learning_test(String img_path)
	{
		ReceivedMessage image_req = receiveRequestMessage();
		String image = image_req.get_content();
		String receipt = image_req.get_receipt();
		String img_name = image_req.get_image_name();
		
		ImageProcessing.text_to_image(img_path, image);
		File input_img = new File(img_path);
		
		//input.store_image(img_name, input_img);
		
		//Execute Deep Learning Process
		String classifcation_out = null;
		Process p;
		try {
			//Ubuntu version for processing
			/*
			p = new ProcessBuilder("/bin/bash", "-c", "cd /home/ubuntu/classifier/"
					+ " && python3 image_classification.py " + img_path).start();
			*/
			
			//Windows only for testing on local machine
			p = new ProcessBuilder("CMD", "/C", "cd " + PICTURES + " && python hello.py").start();
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			classifcation_out = br.readLine();
			p.destroy();
		} catch (Exception e) {
		}
		if(classifcation_out != null)
		{
			//output.store_image(img_name,classifcation_out);
		}
		String output_pair = "(" + img_name + ", " + classifcation_out + ")";
		sendResponseMessage(output_pair);
		deleteRequestMessage(receipt);
	}
	/*
	 * Image Path is path to the image locally which is then
	 * encoded to a string image and passed to the SQS Request
	 * Queue. The EC2 Instance will then receive this image and
	 * 
	 * USE THIS FUNCTION IN THE MAIN FUNCTION FOR THE APP
	 * TIER
	 */
	public void send_and_process_deep(String img_path)
	{
		/*
		 * Encode image and send it to the request queue
		 * NORMALLY THIS IS HANDLED BY THE WEB TIER
		 */
		
		
		File input_img = new File(img_path);
		String encoded_input_img = ImageProcessing.image_to_text(input_img);
		this.sendRequestImageFile(input_img.getName(), encoded_input_img);
		
		
		/*
		 * THIS IS WHERE THE APP TIER WOULD NORMALLY
		 * START IN ITS LISTENER LOOP
		 */
		ReceivedMessage image_req = receiveRequestMessage();
		String image = image_req.get_content();
		String receipt = image_req.get_receipt();
		String img_name = image_req.get_image_name();
		
		ImageProcessing.text_to_image(LNX_IMG_SAVE_DIR + img_name, image);
		File received_img = new File(LNX_IMG_SAVE_DIR + img_name);
		
		input.store_image(img_name, received_img);
		
		//Execute Deep Learning Process
		String classifcation_out = null;
		/*
		 * Add part of process to delete
		 * the image once complete
		 */
		Process p;
		try {
			//Ubuntu version for processing
			p = new ProcessBuilder("/bin/bash", "-c", "cd /home/ubuntu/classifier/"
					+ " && python3 image_classification.py " + LNX_IMG_SAVE_DIR + img_name).start();
			p.waitFor();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			classifcation_out = br.readLine();
			p.destroy();
		} catch (Exception e) {
		}
		if(classifcation_out != null)
		{
			output.store_image(img_name,classifcation_out);
		}
		String output_pair = "(" + img_name + ", " + classifcation_out + ")";
		sendResponseMessage(output_pair);
		deleteRequestMessage(receipt);
	}
	public void send_and_process_deep()
	{
		/*
		 * THIS IS WHERE THE APP TIER WOULD NORMALLY
		 * START IN ITS LISTENER LOOP
		 */
		ReceivedMessage image_req = receiveRequestMessage();
		
		if(image_req != null)
		{
			String image = image_req.get_content();
			String receipt = image_req.get_receipt();
			String img_name = image_req.get_image_name();
			
			ImageProcessing.text_to_image(LNX_IMG_SAVE_DIR + img_name, image);
			File received_img = new File(LNX_IMG_SAVE_DIR + img_name);
			
			input.store_image(img_name, received_img);
			
			//Execute Deep Learning Process
			String classifcation_out = null;
			/*
			 * Add part of process to delete
			 * the image once complete
			 */
			Process p;
			try {
				//Ubuntu version for processing
				p = new ProcessBuilder("/bin/bash", "-c", "cd /home/ubuntu/classifier/"
						+ " && python3 image_classification.py " + LNX_IMG_SAVE_DIR + img_name).start();
				p.waitFor();
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				classifcation_out = br.readLine();
				p.destroy();
			} catch (Exception e) {
			}
			if(classifcation_out != null)
			{
				output.store_image(img_name,classifcation_out);
			}
			String output_pair = "(" + img_name + ", " + classifcation_out + ")";
			sendResponseMessage(output_pair);
			deleteRequestMessage(receipt);
		}
		else
		{
			try {
				Thread.sleep(4000);
			}
			catch(Exception e)
			{}
		}
	}
}
