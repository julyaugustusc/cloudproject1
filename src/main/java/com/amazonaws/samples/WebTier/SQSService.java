package com.amazonaws.samples.WebTier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SQSService {

	public SQSService() {
		// TODO Auto-generated constructor stub
	}
	
	private static final AWSCredentials credentials;

    static {
        // put your accesskey and secretkey here
        credentials = new BasicAWSCredentials(
    		AWSConstants.ACCESS_KEY, 
        	AWSConstants.PRIVATE_KEY
        );
    }
    
 // Set up the client
    AmazonSQS sqs = AmazonSQSClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.US_EAST_1)
        .build();

	
    public CreateQueueResult createQueue(String queueName) {
    	CreateQueueResult createQueueResult = sqs.createQueue(queueName);
		return createQueueResult;
	}

	
	public void sendMessage(String encoded_message, String queueName,String img_name) {
		Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		messageAttributes.put("ImageName", new MessageAttributeValue()
		  .withStringValue(img_name)
		  .withDataType("String"));
		
		String standardQueueUrl = sqs.getQueueUrl(queueName)
	            .getQueueUrl();
		SendMessageRequest sendMessageRequest = new SendMessageRequest().withQueueUrl(standardQueueUrl)
				.withMessageBody(encoded_message).withDelaySeconds(10)
				.withMessageAttributes(messageAttributes);
		sqs.sendMessage(sendMessageRequest);
	}
	
	
	public void deleteMessage(String queueName,Message message) {
		String standardQueueUrl = sqs.getQueueUrl(queueName)
	            .getQueueUrl();
		String messageReceiptHandle = message.getReceiptHandle();
		DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(standardQueueUrl, messageReceiptHandle);
		sqs.deleteMessage(deleteMessageRequest);
	}
	
	
	public Message receiveMessage(String queueName) {
		String standardQueueUrl = sqs.getQueueUrl(queueName)
	            .getQueueUrl();
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(standardQueueUrl);
		receiveMessageRequest.setMaxNumberOfMessages(1);
		receiveMessageRequest.setWaitTimeSeconds(10);
		receiveMessageRequest.setVisibilityTimeout(30);
		ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);
		List<Message> messageList = receiveMessageResult.getMessages();
		if (messageList.isEmpty()) {
			return null;
		}
		return messageList.get(0);
	}

	
	public Integer getNumberOfMsgs(String queueName) {
		String standardQueueUrl = sqs.getQueueUrl(queueName)
	            .getQueueUrl();
		List<String> attributeNames = new ArrayList<String>();
		attributeNames.add("ApproximateNumberOfMessages");
		GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest(standardQueueUrl, attributeNames);
		Map map = sqs.getQueueAttributes(getQueueAttributesRequest).getAttributes();
		String numberOfMessagesString = (String) map.get("ApproximateNumberOfMessages");
		Integer numberOfMessages = Integer.valueOf(numberOfMessagesString);
		return numberOfMessages;
	}
}
