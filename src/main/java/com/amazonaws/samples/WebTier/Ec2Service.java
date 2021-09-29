package com.amazonaws.samples.WebTier;

import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.samples.AppTier.ImageProcessing;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.MonitorInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.UnmonitorInstancesRequest;

public class Ec2Service  {

	public Ec2Service() {
		// TODO Auto-generated constructor stub
	}
	
	private static final AWSCredentials credentials;
	
	public AWSConstants constants = new AWSConstants();

    static {
        // put your accesskey and secretkey here
        credentials = new BasicAWSCredentials(
        	AWSConstants.ACCESS_KEY, 
        	AWSConstants.PRIVATE_KEY
        );
    }
	// Set up the client
    AmazonEC2 ec2Client = AmazonEC2ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.US_EAST_1)
        .build();
    
    //this is to just have quick access to all the names for iteration purposes. 
    //THIS LIST SHOULD NOTTTTT INCLUDE THE WEB TIER INSTANCE BC We Don't want this code stopping itself on accident.
    ArrayList <String> listOfIDs = new ArrayList<String>();
    Integer countOfAppInst = 0;
    
    ArrayList <String> listOfIDsStopped = new ArrayList<String>();
    ArrayList <String> listOfIDsRunning = new ArrayList<String>();
    
    public String createInstance()
    {
    	String security_group = AWSConstants.SECURITY_GROUP;
		String key_name = AWSConstants.KEY_PAIR;
		String ami_id = AWSConstants.amiId; //App Tier AMI
		String shell_script = ImageProcessing.image_to_text(AWSConstants.EC2_SHELL_SCRIPT);
		
		Integer tag_count =  countOfAppInst +1;
		Tag t = new Tag();
		t.setKey("Name");
		t.setValue("app-instance-"+tag_count.toString());
		Collection<Tag> tags = new ArrayList<Tag>();
		tags.add(t);
		TagSpecification tagSpecification = new TagSpecification();
		tagSpecification.setResourceType("instance");
		tagSpecification.setTags(tags);
		Collection<TagSpecification> tagSpecifications = new ArrayList<TagSpecification>();
		tagSpecifications.add(tagSpecification);
		
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest().withImageId(ami_id)
              .withInstanceType("t2.micro")
              .withMinCount(1)
              .withMaxCount(1)
              .withKeyName(key_name)
              .withSecurityGroups(security_group)
              .withUserData(shell_script)
              .withTagSpecifications(tagSpecifications);
		
		RunInstancesResult runInstancesResult = ec2Client.runInstances(runInstancesRequest);
		Instance instance = runInstancesResult.getReservation().getInstances().get(0);
		String instanceId = instance.getInstanceId();
		
		CreateTagsRequest createTagsRequest = new CreateTagsRequest()
				.withResources(instance.getInstanceId())
				     .withTags(new Tag("TestName", "TagName"));
				ec2Client.createTags(createTagsRequest);
		
		StartInstancesRequest startInstancesRequest = new StartInstancesRequest().withInstanceIds(instanceId);
		ec2Client.startInstances(startInstancesRequest);

		listOfIDs.add(instanceId);
        countOfAppInst++;
        listOfIDsRunning.add(instanceId);//It's running right, it doesn't have to be started?
		
        System.out.println("INSTANCE CREATED WITH ID: " + instanceId);
		return instanceId;
    }
	
	public void startInstance(String insId) {
		StartInstancesRequest startInstancesRequest = new StartInstancesRequest()
	            .withInstanceIds(insId);

	        ec2Client.startInstances(startInstancesRequest);
	        listOfIDsRunning.add(insId);
	        listOfIDsStopped.remove(insId);
	        System.out.println("Instance with ID: " + insId + " Started");
	}
	
	public void monitorInstance(String insId) {
		MonitorInstancesRequest monitorInstancesRequest = new MonitorInstancesRequest()
	            .withInstanceIds(insId);

	        ec2Client.monitorInstances(monitorInstancesRequest);
	}
	public void unmonitorInstance(String insId) {

	        UnmonitorInstancesRequest unmonitorInstancesRequest = new UnmonitorInstancesRequest()
	            .withInstanceIds(insId);

	        ec2Client.unmonitorInstances(unmonitorInstancesRequest);

	}
	
	public void stopInstance(String insId) {
		StopInstancesRequest stopInstancesRequest = new StopInstancesRequest()
	            .withInstanceIds(insId);

	        ec2Client.stopInstances(stopInstancesRequest)
	            .getStoppingInstances()
	            .get(0)
	            .getPreviousState()
	            .getName();
	        
	        listOfIDsStopped.add(insId);
	        listOfIDsRunning.remove(insId);
	        System.out.println("Instance with ID: " + insId + " Stopped");
	}
	
	public void terminateInstance(String insId) {
		TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(insId);
		ec2Client.terminateInstances(request);
		
		listOfIDs.remove(insId);
		try {
			listOfIDsStopped.remove(insId); //Shouldn't be in the listOfIDRunning.
		} catch (Exception e) {
			System.out.print("Terminated a non-stopped instance");
		}
	}
	
	public int getNumberOfInstances() {
		DescribeInstanceStatusRequest describeRequest = new DescribeInstanceStatusRequest();
		describeRequest.setIncludeAllInstances(true);
		DescribeInstanceStatusResult describeInstances = ec2Client.describeInstanceStatus(describeRequest);
		List<InstanceStatus> instanceList = describeInstances.getInstanceStatuses();
		int count = 0;
		for (InstanceStatus instance : instanceList) {
			if (instance.getInstanceState().getName().equals(InstanceStateName.Running.toString())) {
				count++;
			}
		}
		
		/*
		if(listOfIDs.size() != count) {
			System.out.println("Error in instance count somewhere");
		}
		*/
		return count;
	}
}