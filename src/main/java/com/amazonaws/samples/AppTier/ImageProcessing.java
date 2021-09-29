package com.amazonaws.samples.AppTier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;

/*
 * This Image processing is used for processing
 * images that need to be converted to Strings
 * to be passed to the Amazon SQS Queue.
 */
public class ImageProcessing {
	/*
	 * Converts encoded String of an image
	 * back to its original image format. Used
	 * for when retrieving images from the SQS
	 * queue and restoring them and writing them
	 * on the EC2 instances
	 * 
	 * @param pathname path where the restored image is written
	 * @param encoded_img String encoded image to be restored
	 */
	public static void text_to_image(String pathname,String encoded_img)
	{
		try {
			 // Converting a Base64 String into Image byte array
	        byte[] imageByteArray = decodeImage(encoded_img);
	
	        // Write a image byte array into file system
	        FileOutputStream imageOutFile = new FileOutputStream(pathname);
	
	        imageOutFile.write(imageByteArray);
	        imageOutFile.close();     
		}
		catch (FileNotFoundException e) {
            System.out.println("Image not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while reading the Image " + ioe);
        }
	}
	/*
	 * Given an image file, it encodes
	 * this file into a string so that it
	 * can be passed into the SQS queue
	 * 
	 * @param pathname location of the image file
	 */
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
	public static String image_to_text(File img_file)
	{
		File file = img_file;
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
 
    /**
     * Encodes the byte array into base64 string
     *
     * @param imageByteArray - byte array
     * @return String a {@link java.lang.String}
     */
    public static String encodeImage(byte[] imageByteArray) {
        return Base64.encodeBase64URLSafeString(imageByteArray);
    }
 
    /**
     * Decodes the base64 string into byte array
     *
     * @param imageDataString - a {@link java.lang.String}
     * @return byte array
     */
    public static byte[] decodeImage(String imageDataString) {
        return Base64.decodeBase64(imageDataString);
    }
}