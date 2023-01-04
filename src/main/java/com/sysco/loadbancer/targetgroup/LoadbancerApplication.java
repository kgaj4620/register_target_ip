package com.sysco.loadbancer.targetgroup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeregisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTargetGroupsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetGroup;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@SpringBootApplication
public class LoadbancerApplication {
	
	static Logger logger = Logger.getLogger(LoadbancerApplication.class.getName());
	static String bucketName = "targetgroupdata";
	static String keyName = "Data.json"; // For lx048-poc
	

	
	public static void main(String[] args) throws IOException {
	   // SpringApplication.run(LoadbancerApplication.class, args);
		

	}

	@Bean
	public void callTargetRegister() {

		logger.info("##### Starting........ :-" + new Date());

		logger.info("##### Old  Data presnt in file are below :-");

		S3Client s3Client = S3Client.builder().build();
		// .region(Region.of("us-east-1")).build();
		
		com.sysco.loadbancer.targetgroup.TargetGroup[] someClassObject = getObjectBytes(s3Client, bucketName, keyName);

		List<com.sysco.loadbancer.targetgroup.TargetGroup> list = new ArrayList<com.sysco.loadbancer.targetgroup.TargetGroup>();
		boolean flag = false;
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
				logger.info("Old ip is same as Current ip for endpoint " + someClassObject[i].getUrl());
			} else {
				logger.info("Old ip is diffrent as Current ip for endpoint " + someClassObject[i].getUrl());
				flag = true;
				try {

					deRegisterTargetGroup(someClassObject[i].getRegion(), someClassObject[i].getTargetGroupName(),
							oldIp);
				} catch (Exception e) {

					logger.info("failed  deRegisterTargetGroup( " + e.getMessage());

				}
				try {
					registerTargetGroup(someClassObject[i].getRegion(), someClassObject[i].getTargetGroupName(),
							currentIp);
				} catch (Exception e) {
					logger.info("failed  registerTargetGroup( " + e.getMessage());
					flag = false;
				}

			}
		}
		if (flag) {
			addDataTofile(list);
		}

	};

	public RegisterTargetsResponse registerTargetGroup(String region, String targetGroupName, String ipAddress) {

		logger.info(" Inside registerTargetGroup () with region :" + region + " targetGroupName:" + targetGroupName
				+ " and ipAddress :" + ipAddress);

		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder().build();
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

	public DeregisterTargetsResponse deRegisterTargetGroup(String region, String targetGroupName, String ipAddress) {

		logger.info(" Inside DeregisterTargetsResponse () with region :" + region + " targetGroupName:"
				+ targetGroupName + " and ipAddress :" + ipAddress);
		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder().build();

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

	

		S3Client client = S3Client.builder().build();

		PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(keyName).build();

		client.putObject(request, RequestBody.fromString(data));
		logger.info("##### Sucessfully added data into s3 bucket:-" + data);

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

			logger.info("##### \r\n" + "	private:-" + e.getMessage());
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

	public static com.sysco.loadbancer.targetgroup.TargetGroup[] getObjectBytes(S3Client s3, String bucketName,
			String keyName) {
		com.sysco.loadbancer.targetgroup.TargetGroup[] someClassObject = null;
		try {
			GetObjectRequest objectRequest = GetObjectRequest.builder().key(keyName).bucket(bucketName).build();

			ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
			byte[] data = objectBytes.asByteArray();
			String string = new String(data, StandardCharsets.UTF_8);

			ObjectMapper mapper = new ObjectMapper();

			try {
				return someClassObject = mapper.readValue(string, com.sysco.loadbancer.targetgroup.TargetGroup[].class);

			} catch (JsonMappingException e) {
				logger.info("##### Exception while reading file from s3:-" + e.getMessage());
			} catch (JsonProcessingException e) {

				logger.info("##### Exception while reading file from s3:-" + e.getMessage());
			}

		} catch (S3Exception e) {

			logger.info("##### Exception while reading file from s3:-" + e.awsErrorDetails().errorMessage());
		}

		return someClassObject;

	}

}
