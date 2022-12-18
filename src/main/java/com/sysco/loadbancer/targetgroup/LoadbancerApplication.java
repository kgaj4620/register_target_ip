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
		
		
		String aws_access_key_id = "ASIAX6N6XJ56BZU27LBS";
		String aws_secret_access_key = "O2i+F5lmHTlfgtiCB9Hg7b5wartITahq50/eUWYF";
		String aws_session_token = "IQoJb3JpZ2luX2VjEAgaCXVzLWVhc3QtMSJIMEYCIQDBCNRXK4px1XP+2mzu6u9cwZF8/OnBOyk0HYsRyAD0yAIhAJdHcPdsXKncdWGfjx9ULbcQ8izkinqQiz5XS8ozpZfEKowDCEAQAxoMNTQ2Mzk3NzA0MDYwIgwofmNW3lPdjiTrnDUq6QK6tyjSj32WpdRtUUpDXZuwfWFFkAqIO+tNMv5JK1w77l4DI/vzU2BCopbNFi64AZuS19bwjFrtQZ13pmmCKVCWf2pUKxyhlNp4JHLTiAEwT9blW+J7mBbleMNltI0YBrMDwlRiCxrHrquNbSyC7F1Oeiy0R9GwUAFqO9p7vDrwDgZdpE6azFN/sgkr0wKhFHuRfgyFZVMKaGPGHnj0BDWrhisyzLZ1zRh7y9YsxXtL+7MhLiIZjJUgPFBLWky/BqtlMh0wK56/JmLsgdTsG+EWu9jsyp4F7iXw2+iXFDavmByH4j82QLzLzCvZQiLy+yEfSC+pxvpDC9N6Ce+63RhrMR+/ztMad800N8Ek9uCWK5F1ixeXPaL3EVOrHJfrXVWZngeRdAr382/8G/NyrpXlPwBDo3UAUxN8VVdmaR0378RIbpe0LziT8bgFDch2j2MIwu6IAvmDa0CXpTgnh1qUrxQhI68hpH4IMJL5+pwGOqUBGE/F1UbdCz9TwcAW1HGFpC6yB1NNETj2w1C4kgbkNsUl3sh3mznuzlHZSNvMW/NpKkT27Rvt3J+gXwMdA3J+uZ9OQ4QiQGChoRRpwEAodi5iAAPA1f86SS9afhTofTYBWvVNaS+j7sTg7F83UEoOW+v3vQLZE7iiWUzyuTA/TFGGUJ2dbggT7xCtO9mL5owdbUihTuNlOy7ugGfc/XBiTCEOt0c6";
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
					try {
						
					
					deRegisterTargetGroup(aws_access_key_id, aws_secret_access_key, aws_session_token,
							someClassObject[i].getRegion(), someClassObject[i].getTargetGroupName(), oldIp);
					} catch (Exception e) {
						
						logger.info("failed  deRegisterTargetGroup( " +e.getMessage());
					}
					try {
						registerTargetGroup(aws_access_key_id, aws_secret_access_key, aws_session_token,
								someClassObject[i].getRegion(), someClassObject[i].getTargetGroupName(), currentIp);	
					} catch (Exception e) {
						logger.info("failed  registerTargetGroup( " +e.getMessage());
					}
					
					flag=true;
				}
			}
			if (flag) {
				addDataTofile(list);
			}
		} catch (IOException e) {
			logger.info("No data presnt in file " + e.getMessage());
		}


	};

	public RegisterTargetsResponse registerTargetGroup(String aws_access_key_id, String aws_secret_access_key,
			String aws_session_token, String region, String targetGroupName, String ipAddress) {
		AwsSessionCredentials awsCreds = AwsSessionCredentials.create(aws_access_key_id, aws_secret_access_key,
				aws_session_token);
		logger.info(" Inside registerTargetGroup () with region :" + region + " targetGroupName:" + targetGroupName
				+ " and ipAddress :" + ipAddress);
		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).region(Region.of(region)).build();

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

	public DeregisterTargetsResponse deRegisterTargetGroup(String aws_access_key_id, String aws_secret_access_key,
			String aws_session_token, String region, String targetGroupName, String ipAddress) {

		logger.info(" Inside DeregisterTargetsResponse () with region :" + region + " targetGroupName:"
				+ targetGroupName + " and ipAddress :" + ipAddress);
		AwsSessionCredentials awsCreds = AwsSessionCredentials.create(aws_access_key_id, aws_secret_access_key,
				aws_session_token);

		ElasticLoadBalancingV2Client loadBalancingClient = ElasticLoadBalancingV2Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(awsCreds)).region(Region.of(region)).build();

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
