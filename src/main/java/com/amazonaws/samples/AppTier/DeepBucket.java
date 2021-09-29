package com.amazonaws.samples.AppTier;

import java.io.File;
import java.io.IOException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/*
 * DeepBucket represents a class for
 * an S3 bucket used for the Deep Learning
 * EC2 Instance
 * 
 * After taking in credentials and the given
 * bucket the name, an s3 client is instantiated
 * so that the bucket can be accessed
 */
public class DeepBucket {
	AWSCredentials credentials;
	String bucket_name;
	AmazonS3 s3client;
	
	public DeepBucket(String access_key, String secret_key, String bucket_name)
	{
		credentials = new BasicAWSCredentials(
				  access_key, 
				  secret_key
				);
		s3client = AmazonS3ClientBuilder
				  .standard()
				  .withCredentials(new AWSStaticCredentialsProvider(credentials))
				  .withRegion(Regions.US_EAST_1)
				  .build();
		this.bucket_name = bucket_name;
	}
	/*
	 * Store image stores the name of an image
	 * and the output result of the deep learning
	 * model
	 * 
	 * TODO: Implement store function for the input
	 * bucket that stores the image name alongside the
	 * image file
	 */
	public void store_image(String image,String result)
	{
		s3client.putObject(
				  bucket_name, 
				  image, 
				  result
				);
		System.out.println("AFTER S3 PUT");
	}
	public void store_image(String image_name,File image)
	{
		s3client.putObject(bucket_name, image_name, image);
	}
}
