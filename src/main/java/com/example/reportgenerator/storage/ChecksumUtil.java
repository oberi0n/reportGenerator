package com.example.reportgenerator.storage;
import java.security.MessageDigest;
public final class ChecksumUtil { private ChecksumUtil(){} public static String sha256(byte[] b){ try{ var md=MessageDigest.getInstance("SHA-256"); var h=md.digest(b); StringBuilder sb=new StringBuilder(); for(byte x:h) sb.append(String.format("%02x",x)); return sb.toString(); }catch(Exception e){throw new IllegalStateException(e);} } }
