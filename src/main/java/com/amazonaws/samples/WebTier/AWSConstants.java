package com.amazonaws.samples.WebTier;
public class AWSConstants {

	public AWSConstants() {

	}
	final static String ACCESS_KEY = "*******";
	final static String PRIVATE_KEY = "*******";
	final static String REQUEST_URL = "https://sqs.us-east-1.amazonaws.com/548955337833/Request";
	final static String RESPONSE_URL = "https://sqs.us-east-1.amazonaws.com/548955337833/Response";
	final static String REQUEST_NAME = "Request";
	final static String RESPONSE_NAME = "Response";
	final static String INPUT_BUCKET = "input-38u4348323434";
	final static String OUTPUT_BUCKET = "output-1238293892232";
	final static String USR_IMG_DIR = "/home/ubuntu/upload_images";
	final static String EC2_SHELL_SCRIPT = "/home/ubuntu/AppTierShell.txt";
	public static final String queueName1 = "Request";
	public static final String queueName2 = "Response";
	public static final String RESP_DICT_DIR = "/home/ubuntu/Dictionary.txt";
	final static String SECURITY_GROUP = "ProjectSecurityGroup";
	final static String KEY_PAIR = "app-tier";
	public static final String amiId = "ami-05bd6839663ffa871"; //Custom AMI for App Tier

}