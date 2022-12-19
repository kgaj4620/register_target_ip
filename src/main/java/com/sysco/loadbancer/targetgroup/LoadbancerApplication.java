package com.sysco.loadbancer.targetgroup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;

@SpringBootApplication
public class LoadbancerApplication {
	static Logger logger = Logger.getLogger(LoadbancerApplication.class.getName());
	

	public static void main(String[] args) throws IOException {
		SpringApplication.run(LoadbancerApplication.class, args);
		LoadbancerApplication lba = new LoadbancerApplication();
		lba.callTargetRegister();

	}
	
	public void callTargetRegister() {
		
		
//		String aws_access_key_id = "ASIAX6N6XJ56I6H7KCFW";
//		String aws_secret_access_key = "HHS20IGkRe8jQ0yq776xXiwT1EgOpogt5JzKrzAg";
//		String aws_session_token = "IQoJb3JpZ2luX2VjEAsaCXVzLWVhc3QtMSJIMEYCIQDLHihE0m09eBMmq5Lrgri6CXAnFNmdMinojS+qSl5CWAIhAKuIeltjMilERxFlq8g3HqKkcLCz/ogIcYUwauY6xHWTKowDCEQQAxoMNTQ2Mzk3NzA0MDYwIgyg1KFFQcPfJoFBDSUq6QJgDctM2EXj6o4XfIHamSO8qi3M6ZsVlmSgEfxxHU9AMtMuj0a270X8zu8f/4Hrhdtk0YRHBWr/v/7vSBpdYpK+RS8EM88NoQWC3kKEVFdfyH3wZm7OiW7dBhkTc3Lbn+zRiqbL9ZSUdGGGJ6vq4AZMSbqITfkL9ZytxfO/Q96UFdFX1t3VzFd1wmaK4uoERQiVDt1r0zGjt9AtE+IMvXexPpkpMLLh1Qf3CpO6Mn06HnxrrQOSzkTOywJXlLaqKvCw/UwrpSL4ERgsvmHjrOlELE39J5VzVmb01Ybnty2IIWabcOqcSDVN/CaaWy9M6yBGvrkoizipV0fmVffTDbsOdy0oFXlf3wp0yypzUh6ilHay+SI/3RoFRVA7cVEQLsu19Sk7WaSV2OMqGhmsQ99AZhNkXt8GzHhGCtL+m+uS/pPlkvXu+aOUeEFZdXD0qGy5XEYkdrBq1AtGkSC72SMJ0vUHQIYNXlxHMM7m+5wGOqUB5Elio3kgkTBH4SSIwHoMOtsUzr5qZyvX443O3QUBjTrGFYvTS/iWo+tW0sx/sP5EhpFMO7BzUbA1Elepgu0EMm+r4xXJ+RxA3ooj+WXfpUbwUPNdF0KFWY93ufsV4CTpoqf5FspDX767xEcO3Utyv5TlMtR5oIPl9nU9pfR+Tkres3FTOin9WTpFE1mbn+wLFM4geDup5ooPRoCohCEWeBTx2OHN";
		logger.info("##### Starting........ :-"+new Date());
		// create Object Mapper
		ObjectMapper mapper = new ObjectMapper();

		// read JSON file and map/convert to java POJO
		try {
			logger.info("##### Old  Data presnt in file are below :-");
			com.sysco.loadbancer.targetgroup.TargetGroup[] someClassObject = mapper
					.readValue(new File("src\\main\\resources\\Data.json"), com.sysco.loadbancer.targetgroup.TargetGroup[].class);
			List<com.sysco.loadbancer.targetgroup.TargetGroup> list=new ArrayList<com.sysco.loadbancer.targetgroup.TargetGroup>();
			boolean flag=false;
			for (int i = 0; i < someClassObject.length; i++) {
				logger.info(":- " + someClassObject[i]);
				String oldIp = someClassObject[i].getIpAddress();
				logger.info(" Old ip address for " + someClassObject[i].getUrl() + " is " + oldIp);
				String currentIp = getIpByAddressName(someClassObject[i].getUrl());
				logger.info(" Current ip address for " + someClassObject[i].getUrl() + " is " + currentIp);				
				com.sysco.loadbancer.targetgroup.TargetGroup tg = new com.sysco.loadbancer.targetgroup.TargetGroup();
				tg.setRegion(someClassObject[i].getRegion());
				tg.setTargetGroupName(someClassObject[i].getTargetGroupName());
				tg.setIpAddress(currentIp);
				tg.setUrl(someClassObject[i].getUrl());
				list.add(tg);
				
				if (oldIp.equals(currentIp)) {
					logger.info("Old ip is same as Current ip for url " + someClassObject[i].getUrl());
				} else {
					logger.info("Old ip is diffrent as Current ip for url " + someClassObject[i].getUrl());
					flag=true;
					try {
						
					
					deRegisterTargetGroup(someClassObject[i].getRegion(), someClassObject[i].getTargetGroupName(), oldIp);
					} catch (Exception e) {
						
						logger.info("failed  deRegisterTargetGroup( " +e.getMessage());
						
					}
					try {
						registerTargetGroup(someClassObject[i].getRegion(), someClassObject[i].getTargetGroupName(), currentIp);	
					} catch (Exception e) {
						logger.info("failed  registerTargetGroup( " +e.getMessage());
						flag=false;
					}
					
					
				}
			}
			if (flag) {
				addDataTofile(list);
			}
		} catch (IOException e) {
			logger.info("No data presnt in file " + e.getMessage());
		}


	};

	public RegisterTargetsResponse registerTargetGroup( String region, String targetGroupName, String ipAddress) {
//		AwsSessionCredentials awsCreds = AwsSessionCredentials.create(aws_access_key_id, aws_secret_access_key,
//				aws_session_token);
		logger.info(" Inside registerTargetGroup () with region :" + region + " targetGroupName:" + targetGroupName
				+ " and ipAddress :" + ipAddress);
//		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder()
//				.credentialsProvider(ProfileCredentialsProvider.create()).region(Region.of(region)).build();

		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder()
				.region(Region.of(region)).build();
		DescribeTargetGroupsResponse target = loadBalancingClient.describeTargetGroups();
		List<TargetGroup> targetGroupList = target.targetGroups();
		RegisterTargetsResponse response = null;
		TargetGroup targetGroup = targetGroupList.stream().filter(u -> u.targetGroupName().equals(targetGroupName))
				.findAny().get();
		logger.info(" Target group details for targetGroupName:" + targetGroupName + " is :" + targetGroup);
		try {

			TargetDescription td = TargetDescription.builder().id(ipAddress).port(targetGroup.port()).build();
			RegisterTargetsRequest r = RegisterTargetsRequest.builder().targetGroupArn(targetGroup.targetGroupArn())
					.targets(td).build();
			response = loadBalancingClient.registerTargets(r);
			logger.info(" Successfully registered Trarget group for targetGroupName :" + targetGroupName + " port:"
					+ targetGroup.port() + " ipAddress:" + ipAddress);
		} catch (Exception e) {

			logger.info(" Failed to registered Trarget group for targetGroupName :" + targetGroupName + " port:"
					+ targetGroup.port() + " ipAddress:" + ipAddress + " with exception" + e.getMessage());
		}

		return response;
	}

	public DeregisterTargetsResponse deRegisterTargetGroup( String region, String targetGroupName, String ipAddress) {

		logger.info(" Inside DeregisterTargetsResponse () with region :" + region + " targetGroupName:"
				+ targetGroupName + " and ipAddress :" + ipAddress);
//		AwsSessionCredentials awsCreds = AwsSessionCredentials.create(aws_access_key_id, aws_secret_access_key,
//				aws_session_token);
		//StaticCredentialsProvider.create(awsCreds)
//		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder()
//				.credentialsProvider(ProfileCredentialsProvider.create()).region(Region.of(region)).build();
		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder()
				.region(Region.of(region)).build();

		DescribeTargetGroupsResponse target = loadBalancingClient.describeTargetGroups();
		List<TargetGroup> targetGroupList = target.targetGroups();
		DeregisterTargetsResponse response = null;
		TargetGroup targetGroup = targetGroupList.stream().filter(u -> u.targetGroupName().equals(targetGroupName))
				.findAny().get();

		logger.info(" Target group details for targetGroupName:" + targetGroupName + " is :" + targetGroup);
		try {

			TargetDescription td = TargetDescription.builder().id(ipAddress).port(targetGroup.port()).build();
			DeregisterTargetsRequest r = DeregisterTargetsRequest.builder().targetGroupArn(targetGroup.targetGroupArn())
					.targets(td).build();
			response = loadBalancingClient.deregisterTargets(r);
			logger.info(" Successfully deregistered Trarget group for targetGroupName :" + targetGroupName + " port:"
					+ targetGroup.port() + " ipAddress:" + ipAddress);
		} catch (Exception e) {
			logger.info(" Failed to deregistered Trarget group for targetGroupName :" + targetGroupName + " port:"
					+ targetGroup.port() + " ipAddress:" + ipAddress + " with exception" + e.getMessage());
		}
		return response;
	}

	private static void writeUsingFileWriter(String data) {
		File file = new File("src\\main\\resources\\Data.json");
		FileWriter fr = null;
		try {
			fr = new FileWriter(file);
			fr.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// close resources
			try {
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void addJsonDatatoFile(List<com.sysco.loadbancer.targetgroup.TargetGroup> list) {
		try {
			ObjectMapper Obj = new ObjectMapper();
			// Getting organisation object as a json string
			String jsonStr = Obj.writeValueAsString(list);

			// Displaying JSON String on console

			logger.info("##### Added json data to file:-" + jsonStr);
			writeUsingFileWriter(jsonStr);
		}

		// Catch block to handle exceptions
		catch (IOException e) {

			// Display exception along with line number
			// using printStackTrace() method
			e.printStackTrace();
		}
	}

	private static void addDataTofile(List<com.sysco.loadbancer.targetgroup.TargetGroup> list1) {
		addJsonDatatoFile(null);
		List<com.sysco.loadbancer.targetgroup.TargetGroup> list = new ArrayList<>();

		for (com.sysco.loadbancer.targetgroup.TargetGroup tgrp : list1) {
			com.sysco.loadbancer.targetgroup.TargetGroup tg = new com.sysco.loadbancer.targetgroup.TargetGroup();
			tg.setRegion(tgrp.getRegion());
			tg.setTargetGroupName(tgrp.getTargetGroupName());
			tg.setIpAddress(tgrp.getIpAddress());
			tg.setUrl(tgrp.getUrl());
			list.add(tg);
		}

		addJsonDatatoFile(list);
	}

	private static String getIpByAddressName(String url) {
		logger.info("##### inside getIpByAddressName(String url):-" + url);
		String ipAddrss = null;
		try {
			InetAddress host;
			host = InetAddress.getByName(url);

			ipAddrss = host.getHostAddress();

		} catch (UnknownHostException e) {
			logger.info("##### excption getIpByAddressName(String url):-" + e.getMessage());
		}
		return ipAddrss;
	}
}
