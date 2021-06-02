package com.github.megatronking.netbare.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class TransferEncoding {
	private static String encoding = "UTF-8";
	
	public static void main(String[] args) {
        try {  
            Socket socket = new Socket(InetAddress.getByName("www.iteye.com"), 80);  
            socket.setSoTimeout(10000); // 10秒钟超时。 
            OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());  
            StringBuffer sb = new StringBuffer();  
            sb.append("GET / HTTP/1.1\r\n");  
            sb.append("Accept: image/png, image/svg+xml, image/*;q=0.8, */*;q=0.5\r\n");  
            sb.append("X-HttpWatch-RID: 18613-10702\r\n");  
            sb.append("Referer: http://www.cnblogs.com/haitao-fan/archive/2013/01/18/2866994.html\r\n");  
            sb.append("Accept-Language: zh-CN\r\n");  
            sb.append("User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko\r\n");  
            sb.append("Accept-Encoding: gzip, deflate\r\n");  
            sb.append("Host: www.iteye.com\r\n");  
            sb.append("Connection: Keep-Alive\r\n");
            sb.append("DNT: 1\r\n");
            //注，这是关键的关键，忘了这里让我搞了半个小时。这里一定要一个回车换行，表示消息头完，不然服务器会等待  
            sb.append("\r\n");  
            osw.write(sb.toString());  
            osw.flush();  

            //--输出服务器传回的消息的头信息  
            InputStream is = socket.getInputStream();
            boolean isGzip = false;
            // 读取所有服务器发送过来的请求参数头部信息  
            String header = readHeader(is);
            System.out.println(header);
            // 返回的数据类型，是否是gzip压缩的格式
            String acceptEncoding = getHeaderValue(header, "Content-Encoding");
            if (acceptEncoding.startsWith("gzip")) {
        		isGzip = true;
        	}
            String sContLength = getHeaderValue(header, "Content-Length");
            int coentLength = isBlank(sContLength) ? 0 : Integer.parseInt(sContLength);
        	
            String body;
            if (isGzip) {
            	body = readGzipBodyToString(is);
            } else {
            	body = readBody(is, coentLength);
            }
            
            //--输消息的体  
            System.out.print(body);  
  
            //关闭流  
            is.close();
            socket.close();
            
        } catch (UnknownHostException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    } 
	
	/**
	 * 读取压缩的消息体，并且转换为字符串
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String readGzipBodyToString(InputStream is) throws IOException {
		// 读取压缩主体
        List<Byte> bodyByteList = readGzipBody(is);
        // 把读取出来的Byte List转换为数组
		byte[] tmpByteArr = new byte[bodyByteList.size()];  
        for (int i = 0; i < bodyByteList.size(); i++) {  
            tmpByteArr[i] = ((Byte) bodyByteList.get(i)).byteValue();  
        }  
        bodyByteList.clear();  // 释放内存
        // 构建GZIPInputStream对象，准备从这里读取数据
        ByteArrayInputStream bais = new ByteArrayInputStream(tmpByteArr);
		GZIPInputStream gzis = new GZIPInputStream(bais);

        StringBuffer body = new StringBuffer();
    	InputStreamReader reader = new InputStreamReader(gzis, encoding);
    	BufferedReader bin = new BufferedReader(reader);
        String str = null;
        // 开始读取
        while((str = bin.readLine()) != null) {
        	body.append(str).append("\r\n");
        }
        // 读取完成，关闭流
        bin.close();
		return body.toString();
	}

	/**
	 * 获取没有压缩的消息体
	 * 
	 * @param is
	 * @param contentLe
	 * @return
	 */
	public static String readBody(InputStream is, int contentLe) {
		List<Byte> lineByteList = new ArrayList<Byte>();  
        byte readByte;  
        int total = 0; 
        try {
			do {  
				readByte = (byte) is.read();
	            lineByteList.add(Byte.valueOf(readByte));  
	            total++;  
	        } while (total < contentLe);
		} catch (IOException e) {
			e.printStackTrace();
		}  
        
        byte[] tmpByteArr = new byte[lineByteList.size()];  
        for (int i = 0; i < lineByteList.size(); i++) {  
            tmpByteArr[i] = ((Byte) lineByteList.get(i)).byteValue();  
        }  
        lineByteList.clear();  
  
        String line = "";
		try {
			line = new String(tmpByteArr, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return line;
	}

	/**
	 * 读取一行数据
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String readLine(InputStream is) throws IOException {
        List<Byte> lineByteList = new ArrayList<Byte>();  
        byte readByte;
        
        do { 
            readByte = (byte) is.read();  
            lineByteList.add(Byte.valueOf(readByte));  
            
        } while (readByte != 10);// 读取到最后一个"\n"换行的字符
        
        byte[] tmpByteArr = new byte[lineByteList.size()];  
        for (int i = 0; i < lineByteList.size(); i++) {  
            tmpByteArr[i] = ((Byte) lineByteList.get(i)).byteValue();  
        }  
        lineByteList.clear();  
        String line = new String(tmpByteArr, encoding);
        return line;
    } 
	
	/**
	 * 读取头部信息
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String readHeader(InputStream is) throws IOException {
		List<Byte> lineByteList = new ArrayList<Byte>();
        byte readByte;
        int n = 0;
        int doMax = 1000000;   // do-while循环的最大次数
        int count = 0;
        
        do { 
            readByte = (byte) is.read();  
            lineByteList.add(Byte.valueOf(readByte));  
            if (readByte == 13 || readByte == 10) {
            	// 遇到了"\r"或者"\n"
            	n++;
            } else if (n > 0) {
            	// 没有遇到联系的"\r\n"，所以退回原来的状态。
            	n = 0;
            }
            count ++;
            if (count > doMax) {  
            	// 加上这个后是让程序必须在doMax次循环后停止循环，是防止从服务器上读取的数据不正确，无法找到"\r\n\r\n"这个位置而导致内存溢出的情况发生。
            	// doMax是一个html协议头部不可能达到的数值。
            	break;
            }
        } while (n != 4);// 连续遇到了\r\n，说明有空行，头部数据到此位置。
        
        byte[] tmpByteArr = new byte[lineByteList.size()];  
        for (int i = 0; i < lineByteList.size(); i++) {  
            tmpByteArr[i] = ((Byte) lineByteList.get(i)).byteValue();  
        }  
        lineByteList.clear();  // 释放内存
        String header = new String(tmpByteArr, encoding);
        return header;
	}
	
	/**
	 * 获取http头部某一项的值
	 * 
	 * @param content
	 * @param key
	 * @return
	 */
	public static String getHeaderValue(String content, String key) {
		Map<String, String> headerMap = new HashMap<String, String>();
		if (isNotBlank(content)) {
			String[] array = content.split("\r\n");
			for (String item : array) {
				int i = item.indexOf(":");
				if (i < 0) {
					continue;
				}
				String k = item.substring(0, i);
				String v = item.substring(i + 1);
				headerMap.put(k, v);
			}
		}
		String s = headerMap.get(key);
		
		return s != null ? s.trim() : "";
	}
	
	public static boolean isNotBlank(String content) {
		if (content != null && content.trim().length() > 0)
			return true;
		return false;
	}

	/**
	 * 读取gzip压缩的消息体
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
    private static List<Byte> readGzipBody(InputStream is) throws IOException {
    	// 压缩块的大小，由于chunked编码块的前面是一个标识压缩块大小的16进制字符串，在开始读取前，需要获取这个大小
    	int chunk = getChunkSize(is);
    	List<Byte> bodyByteList = new ArrayList<Byte>();
        byte readByte = 0;
        int count = 0;
        
        while (count < chunk) {  // 读取消息体，最多读取chunk个byte
            readByte = (byte) is.read();  
            bodyByteList.add(Byte.valueOf(readByte));
            count ++;
        }
        if (chunk > 0) { // chunk为读取到最后，如果没有读取到最后，那么接着往下读取。
        	List<Byte> tmpList = readGzipBody(is);
        	bodyByteList.addAll(tmpList);
        }
        return bodyByteList;
    }
	
    /**
     * 获取压缩包块的大小
     * 
     * @param is
     * @return
     * @throws IOException
     */
	public static int getChunkSize(InputStream is) throws IOException {
		String sLength = readLine(is).trim();
		if (isBlank(sLength)) {  // 字符串前面有可能会是一个回车换行。
			// 读了一个空行，继续往下读取一行。
			sLength = readLine(is).trim();
		}
        if (sLength.length() < 4) {
        	sLength = 0 + sLength;
        }
        // 把16进制字符串转化为Int类型
        int length = Integer.valueOf(sLength, 16);
        return length;
	}

	public static boolean isBlank(String sLength) {
		if (sLength == null)
			return true;
		if (sLength.trim().length() == 0)
			return true;
		return false;
	}
}
