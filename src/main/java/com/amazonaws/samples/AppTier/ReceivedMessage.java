package com.amazonaws.samples.AppTier;

/*
 * ReceivedMessage structures the Message
 * that is received from the Request SQS queue
 * to store the content and receipt.
 */
public class ReceivedMessage {
	private String content;
	private String receipt;
	private String image_name;
	
	public ReceivedMessage(String msg, String msg_receipt, String img_name)
	{
		content = msg;
		receipt  = msg_receipt;
		image_name = img_name;
	}
	public String get_content()
	{
		return this.content;
	}
	public String get_receipt()
	{
		return this.receipt;
	}
	public String get_image_name()
	{
		return this.image_name;
	}
}
