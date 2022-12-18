package com.sysco.loadbancer.targetgroup;

public class TargetGroup {
private String region;
private String targetGroupName;
private String ipAddress;
private String url;
public String getRegion() {
	return region;
}
public void setRegion(String region) {
	this.region = region;
}
public String getTargetGroupName() {
	return targetGroupName;
}
public void setTargetGroupName(String targetGroupName) {
	this.targetGroupName = targetGroupName;
}
public String getIpAddress() {
	return ipAddress;
}
public void setIpAddress(String ipAddress) {
	this.ipAddress = ipAddress;
}
public  String getUrl() {
	return url;
}
public  void setUrl(String url) {
	this.url = url;
}
@Override
public String toString() {
	return "TargetGroup [region=" + region + ", targetGroupName=" + targetGroupName + ", ipAddress=" + ipAddress
			+ ", url=" + url + "]";
}



}
