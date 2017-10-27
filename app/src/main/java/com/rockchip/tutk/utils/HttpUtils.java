package com.rockchip.tutk.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.Context;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

public class HttpUtils {
	static Map<String, String> mHttpMapper = new HashMap<String, String>();
	static HttpResponse httpResponse = null;
	static HttpClient httpClient ;
	static HttpPost httpPost ;
	private static Context mContext;
	private static int temp_cmd;
	private static HttpEntity temp_entity;
	private static int block_val = -1;

	public static class ClientError
	{
		public final static int E_CLIENT_NET_EXCEPTION = 700; 		//网络异常 700
		public final static int E_CLIENT_NON_DEVICE = 701; 			//没有绑定设备701
		public final static int E_Local_SpaceShortage = 702;		//本地磁盘空间不足
		public final static int E_Cloud_SpaceShortage = 703;		//云端磁盘空间不足
		public final static int E_Splite_Upload_Failed = 704;
		public final static int E_CLIENT_NET_REDIRECT = 705;			//客户端网络重定向，网络不可用
		public final static int E_CLIENT_DEVICE_OFFLINE = 706;
		public final static int E_SERVER_ERROR = 707;
		public final static int E_CGI_FORMAT_INCORRECT = 697; 		// CGI返回格式错误
		public final static int E_SERVER_FORMAT_INCORRECT = 698; 	// 服务器端返回格式错误
		public final static int E_P2P_FORMAT_INCORRECT = 699; 		// P2P返回格式错误错误
		public final static int E_Server_Error = 750;				//服务端错误
		public final static int E_OTHER = 799; 						// 客户端内部其他未知错误
		public final static int Success = 100;
		public final static int NatTraversalServiceVerCode = 206;	//NAT服务版本号代码，每次更新NAT版本后必须修改该代码
	}

	public static enum StatusCode {

		E_P2P_INITSOCKET, // 初始化錯誤 601
		E_P2P_GATEWAY, // Gateway連線錯誤602
		E_P2P_GATEWAY_ROUTE, // Gateway 取得 Route info錯誤603
		E_P2P_GATEWAY_INSPECT, // Gateway 取得 Inspect info錯誤604
		E_P2P_GATEWAY_KEEPALIVE, // Gateway 取得 keepalive info錯誤605
		E_P2P_INSPECT, // Inspect 錯誤606
		E_P2P_ROUTE_UDPALIVETHREAD, // Route 錯誤 607
		E_P2P_ROUTE, // Route錯誤 608
		E_P2P_ROUTE_REGISTER, // Route 註冊錯誤 609
		E_P2P_ROUTE_CONNECT, // Route 連線錯誤 610
		E_P2P_ROUTE_QUERY, // Route 查詢錯誤 611
		E_P2P_ROUTE_UPDATE, // Route 更新錯誤 612
		E_P2P_ROUTE_UNREGISTER, // Route 反註冊錯誤 613
		E_P2P_SELECTCHANNEL, // Select channel 錯誤 614
		E_P2P_DORELAY, // Relay 錯誤 615
		E_P2P_DORELAY_ROUTECLIENT, // Route導致 Relay 錯誤 616
		E_HTTP_UNKNOW, // Http 未知錯誤 617
		E_MUXP2P_ADAPTER_OPEN, // Mux 開啟錯誤 618
		E_MUXP2P_ADAPTER_OPEN_WAIT_TIMEOUT, // Mux等待連線交握過期 619
		E_MUXP2P_ADAPTER_OPEN_SEND_CONTROL, // Mux傳送連線交握失敗 620
		E_MUXP2P_ADAPTER_OPEN_ENCRYPTION, // Mux加密方法不支援 621
		E_MUXP2P_ADAPTER_PLUGMUX_NO_FREE_SN, // Mux 無可用序號 622
		E_MUXP2P_ADAPTER_PLUGMUX_SEND_CONTROL, // Mux傳送socket交握失敗 623
		E_MUXP2P_ADAPTER_PLUGMUX_WAIT_TIMEOUT, // Mux等待socket交握過期 624
		E_MUXP2P_ADAPTER_PLUGIN_MUXSOCKET, // Mux Adapter錯誤 625
		E_MUXP2P_SERVICE_PLUGIN_MUXSOCKET, // Mux Service錯誤 626
		E_FAIL, // P2P内部其他未知錯誤627
		E_CLIENT_NET_EXCEPTION, // 网络异常 628
		E_CLIENT_NON_DEVICE, // 没有绑定设备629
		E_OTHER, // 客户端内部其他未知错误630
		E_Local_SpaceShortage,	//本地磁盘空间不足 631
		E_Cloud_SpaceShortage,	//云端磁盘空间不足632
		Success,
	}
	public static class HttpCMD{
		public static final int CLIENT_BROWSE = 0xF01;
		public static final int CLIENT_PLAY = 0xF02;
		public static final int CLIENT_DELETE = 0xF03;
		public static final int CLIENT_RENAME = 0xF04;
		public static final int CLIENT_CREATE = 0xF05;
		public static final int CLIENT_CATEGORY = 0xF06;
		public static final int CLIENT_SEARCH = 0xF07;
		public static final int CLIENT_UPLOAD = 0xF08;
		public static final int CLIENT_SETTING = 0xF09;
		public static final int CLIENT_TOKEN = 0xF0A;
		public static final int CLIENT_SIZE = 0xF0B;
		public static final int CLIENT_QRCODE = 0xF0C;
		public static final int CLIENT_FORMAT = 0xF0D;
		public static final int CLIENT_GETSAMBA = 0xF0E;
		public static final int CLIENT_SETSAMBA = 0xF0F;
		public static final int CLIENT_APPMANAGE = 0xE01;
		public static final int WEB_BROWSE = 0xD01;
		public static final int REMOTE_PLAY = 0xC01;
		public static final int REMOTE_SET_CONTROL = 0xC02;
		public static final int REMOTE_GET_STATUS = 0xC03;
	}

	public static class MyConstants {
		public static String SERVER_IP = "http://113.116.215.38:28888/";
	}
	public static void initHttpMapper(Context context)
	{
		L.i(2,"initHttpMapper Constants.SERVER_IP is "+MyConstants.SERVER_IP);
		mContext = context;
		mHttpMapper.put(HttpCMD.CLIENT_BROWSE+"", MyConstants.SERVER_IP+"doarch");
		mHttpMapper.put(HttpCMD.CLIENT_PLAY+"", MyConstants.SERVER_IP+"doplay");
		mHttpMapper.put(HttpCMD.CLIENT_DELETE+"", MyConstants.SERVER_IP+"dodel");
		mHttpMapper.put(HttpCMD.CLIENT_CREATE+"", MyConstants.SERVER_IP+"docreate");
		mHttpMapper.put(HttpCMD.CLIENT_CATEGORY+"", MyConstants.SERVER_IP+"category");
		mHttpMapper.put(HttpCMD.CLIENT_SEARCH+"", MyConstants.SERVER_IP+"dosearch");
		mHttpMapper.put(HttpCMD.CLIENT_UPLOAD+"", MyConstants.SERVER_IP+"UploadFile");
		mHttpMapper.put(HttpCMD.CLIENT_SETTING+"", MyConstants.SERVER_IP+"Settings");
		mHttpMapper.put(HttpCMD.CLIENT_TOKEN+"", MyConstants.SERVER_IP+"checktoken");
		mHttpMapper.put(HttpCMD.CLIENT_SIZE+"", MyConstants.SERVER_IP+"downsize");
		mHttpMapper.put(HttpCMD.CLIENT_RENAME+"", MyConstants.SERVER_IP+"dorename");
		mHttpMapper.put(HttpCMD.CLIENT_QRCODE+"", MyConstants.SERVER_IP+"qrcode");
		mHttpMapper.put(HttpCMD.CLIENT_APPMANAGE+"", MyConstants.SERVER_IP+"InstallTV");
		mHttpMapper.put(HttpCMD.REMOTE_PLAY+"",        MyConstants.SERVER_IP +"RemotePlay");
		mHttpMapper.put(HttpCMD.REMOTE_SET_CONTROL+"", MyConstants.SERVER_IP +"RemoteSetControl");
		mHttpMapper.put(HttpCMD.REMOTE_GET_STATUS+"",  MyConstants.SERVER_IP +"RemoteGetStatus");
	}
	public static String Connect(int key)
	{
		L.i(2,"mapper add is "+mHttpMapper.get(key+""));
		return mHttpMapper.get(key+"");
	}
	
	public static String getServerIP(String url,String device)
	{
		try
		{
			String jsonstr = HttpUtils.loadJson(url+device);
			L.i(2,"getServerIP jsonstr is "+jsonstr);
			Map<String, Object> jsonmap = HttpUtils.getJsonMap(jsonstr);
			String jstr = jsonmap.get("devices").toString();
			Map<String, Object> jsondevice = HttpUtils.getJsonMap(jstr);
			Map<String, Object> jsondevicename = HttpUtils.getJsonMap(jsondevice.get(device).toString());
			Set<String> ketset = jsondevicename.keySet();
			Iterator<String> iter = ketset.iterator();
			String key = null;
			while(iter.hasNext())
			{
				key=iter.next();
				if(key.equals("HTTP"))
				{
					Map<String, Object> jsonservice = HttpUtils.getJsonMap(jsondevicename.get(key).toString());
					return jsonservice.get("ip")+":"+jsonservice.get("port");
				}
			}
			return "error";
		}catch(Exception e)
		{
			return null;
		}
	}
	
	public static Map<String,Object> getJsonMap(String jsonString)
	{
//		L.i(2,"getJsonMap jsonString is "+jsonString);
	  JSONObject jsonObject;
	  try
	  {
	   jsonObject = new JSONObject(jsonString);   
	   @SuppressWarnings("unchecked")
	   Iterator keyIter = jsonObject.keys();
	   String key;
	   Object value;
	   Map<String,Object> valueMap = new HashMap<String,Object>();
	   while (keyIter.hasNext())
	   {
	    key = (String) keyIter.next();
	    value = jsonObject.get(key);
	    valueMap.put(key,value);
	   }
	   return valueMap;
	  }
	  catch (JSONException e)
	  {
	   e.printStackTrace();
	   return null; 
	  }
	}
	
	public static String loadJson (String url) {  
        StringBuilder json = new StringBuilder();  
        try {  
            URL urlObject = new URL(url);  
            URLConnection uc = urlObject.openConnection();  
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));  
            String inputLine = null;  
            while ( (inputLine = in.readLine()) != null) {  
                json.append(inputLine);  
            }  
            in.close();  
        } catch (MalformedURLException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        return json.toString();  
    } 
	
	public static String getJsonContent(String urlStr)
    {
        try
        {// 获取HttpURLConnection连接对象
            URL url = new URL(urlStr);
            HttpURLConnection httpConn = (HttpURLConnection) url
                    .openConnection();
            // 设置连接属性
            httpConn.setConnectTimeout(3000);
            httpConn.setDoInput(true);
            httpConn.setRequestMethod("GET");
            // 获取相应码
            int respCode = httpConn.getResponseCode();
            if (respCode == 200)
            {
                return ConvertStream2Json(httpConn.getInputStream());
            }
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }
	
	private static String ConvertStream2Json(InputStream inputStream)
    {
        String jsonStr = "";
        // ByteArrayOutputStream相当于内存输出流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        // 将输入流转移到内存输出流中
        try
        {
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1)
            {
                out.write(buffer, 0, len);
            }
            // 将内存流转换为字符串
            jsonStr = new String(out.toByteArray());
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jsonStr;
    }
//	public void httpRequestHandler(int cmd,Object content,Object arg1,Object arg2)
//	{
//		try
//		{
//	    	String url = mHttpMapper.get(cmd+"");
//	    	httpPost = new HttpPost(url);
//			// 2.通过DefaultClient的excute方法执行返回一个HttpResponse对象
//			postData = new JSONObject();  
//			postData.put("command",cmd);  
//			postData.put("content", content);
//			postData.put("arg1", arg1);
//			postData.put("arg2", arg2);
//			httpPost.setEntity(new StringEntity(postData.toString(), HTTP.UTF_8));
//			httpClient = new DefaultHttpClient();
//			httpResponse = httpClient.execute(httpPost);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	public static boolean  httpRequestHandler(int cmd,HttpEntity entity)
	{
		try
		{
			BasicHttpParams httpParams = new BasicHttpParams();
	    	String url = mHttpMapper.get(cmd+"");
	    	httpPost = new HttpPost(url);
			httpPost.setEntity(entity);
			HttpConnectionParams.setConnectionTimeout(httpParams, 8*1000);
		    HttpConnectionParams.setSoTimeout(httpParams, 8*1000);  
			httpClient = new DefaultHttpClient(httpParams);
			httpResponse = httpClient.execute(httpPost);
			return true;
		}
		catch (Exception e) {
			L.e(2, e.toString());
			e.printStackTrace();  
			return false;
		}
	}
	
	public static String httpGetRequestHandler(int cmd,String arg)
	{
		String line = null;
		String reponse = "";
		Log.d("wz", "=====httpGetRequestHandler cmd = " + cmd + "===");
		try {
			HttpURLConnection urlConn = null;
			URL url = new URL(mHttpMapper.get(cmd+"") + "?" + arg);
			Log.d("mtv-MediaPlayer", "URL=" + url.toString());
			urlConn = (HttpURLConnection)url.openConnection();
			Log.d("wz", "---URL= " + url.toString() + "--");
			urlConn.setRequestMethod("GET");
			urlConn.connect();
			BufferedReader bf =  new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "utf-8"));
			while (null != (line = bf.readLine())) {
				reponse += line;
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		Log.d("wz", "----reponse = " + reponse);
		return reponse;
	}
	
	public static String httpGetJSON() throws ParseException, IOException
	{
		HttpEntity httpEntity = httpResponse.getEntity();
		// 得到一些数据  
		// 通过EntityUtils并指定编码方式取到返回的数据 
		String data = EntityUtils.toString(httpEntity, "utf-8");
		return data;
	}
	
	public static int httpResponseHandler()
	{
		int result = 0;
		try
		{
			HttpEntity httpEntity = httpResponse.getEntity();
			String data = EntityUtils.toString(httpEntity, "utf-8");
			result = Integer.parseInt(data);
			return result;
		}
		catch (Exception e) {
			L.e(1,"httpResponseHandler error is "+e);
		}
		return result;
	}
	
	public static int httpGetStatusCode()
	{
		StatusLine statusLine = httpResponse.getStatusLine();  
		statusLine.getProtocolVersion(); 
		int statusCode = statusLine.getStatusCode();
		return statusCode;
	}
	
	// 转换为%E4%BD%A0形式
		public static String ToUtf8String(String s) {

			if (s == null) {
				return null;
			}
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c >= 0 && c <= 255) {
					sb.append(c);
				} else {
					byte[] b;
					try {
						b = String.valueOf(c).getBytes("utf-8");
					} catch (Exception ex) {
						L.i(2,"ToUtf8String error");
						b = new byte[0];
					}
					for (int j = 0; j < b.length; j++) {
						int k = b[j];
						if (k < 0)
							k += 256;
						sb.append("%" + Integer.toHexString(k).toUpperCase());
					}
				}
			}

			return sb.toString();
		}
	
	public static String HttpGetRequest(String actionUrl, String reqContent) {
		StringBuffer b = new StringBuffer();
		try {
			URL url = new URL(actionUrl+"?"+ToUtf8String(reqContent));
			L.i(2,"HttpGetRequest url is "+url);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(15 * 1000);
			/* 允许Input、Output，不使用Cache */
			con.setDoInput(true);
			//con.setDoOutput(true);
			con.setUseCaches(false);
			/* 设定传送的method=POST */
			con.setRequestMethod("GET");
			/* setRequestProperty */ 
			//con.setRequestProperty("Connection", "Keep-Alive");
			
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Charset", "UTF-8");
//			con.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			con.connect();
			/* 取得Response内容 */
			int code = con.getResponseCode();
			InputStream is = null;
			BufferedReader br = null;
			if(code == 600)
			{
				is = con.getErrorStream();
				br = new BufferedReader(new InputStreamReader(is,"utf-8"));
				//con.getContentLength();
				String line = null;
				while ((line = br.readLine()) != null) {
					b.append(line);
				}
				Map<String, Object> map = getJsonMap(b.toString());
				if(map.get("description") != null)
				{
					String detail = map.get("description").toString();
					String errorCode = GetStatusCodeInfo(detail);
					return "{'result':'failed','detail':'"+detail+"',code:'"+errorCode+"'}";
				}else{
					return "{'result':'failed','detail':'E_FORMAT_INCORRECT',code:'"+ClientError.E_P2P_FORMAT_INCORRECT+"'}";
				}
			}else if(code >= 400 && code < 600)
			{
				is = con.getErrorStream();
				br = new BufferedReader(new InputStreamReader(is,"utf-8"));
				//con.getContentLength();
				String line = null;
				while ((line = br.readLine()) != null) {
					b.append(line);
				}
				return "{'result':'failed','detail':'E_Server_Error',code:'"+ClientError.E_Server_Error+"'}";
			}else if(code >= 300 && code < 400)
			{
				return "{'result':'failed','detail':'E_CLIENT_NET_REDIRECT',code:'"+ClientError.E_CLIENT_NET_REDIRECT+"'}";
			}else
			{
				is = con.getInputStream();
				br = new BufferedReader(new InputStreamReader(is,"utf-8"));
				con.getContentLength();
				String line = null;
				while ((line = br.readLine()) != null) {
					b.append(line);
				}
			}
		} catch (Exception e) {
			L.i(2,"http get request exception ");
			return "{'result':'failed','detail':'E_CLIENT_NET_EXCEPTION',code:'"+ClientError.E_CLIENT_NET_EXCEPTION+"'}";
		}
		return b.toString();
	}
	/**
	 * 根据状态码得到错误原因
	 *
	 * @param statusCode
	 * @return
	 */
	public static String GetStatusCodeInfo(String statusCodeInfo) {
		StatusCode statusCode = Enum.valueOf(StatusCode.class, statusCodeInfo);
		switch (statusCode) {
			case E_P2P_INITSOCKET:// 初始化錯誤 601
				statusCodeInfo = "601";
				break;
			case E_P2P_GATEWAY: // Gateway連線錯誤602
				statusCodeInfo = "602";
				break;
			case E_P2P_GATEWAY_ROUTE: // Gateway 取得 Route info錯誤603
				statusCodeInfo = "603";
				break;
			case E_P2P_GATEWAY_INSPECT: // Gateway 取得 Inspect info錯誤604
				statusCodeInfo = "604";
				break;
			case E_P2P_GATEWAY_KEEPALIVE: // Gateway 取得 keepalive info錯誤605
				statusCodeInfo ="605";
				break;
			case E_P2P_INSPECT: // Inspect 錯誤606
				statusCodeInfo = "606";
				break;
			case E_P2P_ROUTE_UDPALIVETHREAD: // Route 錯誤 607
				statusCodeInfo = "607";
				break;
			case E_P2P_ROUTE: // Route錯誤 608
				statusCodeInfo = "608";
				break;
			case E_P2P_ROUTE_REGISTER: // Route 註冊錯誤 609
				statusCodeInfo = "609";
				break;
			case E_P2P_ROUTE_CONNECT: // Route 連線錯誤 610
				statusCodeInfo = "610";
				break;
			case E_P2P_ROUTE_QUERY: // Route 查詢錯誤 611
				statusCodeInfo = "611";
				break;
			case E_P2P_ROUTE_UPDATE: // Route 更新錯誤 612
				statusCodeInfo = "612";
				break;
			case E_P2P_ROUTE_UNREGISTER: // Route 反註冊錯誤 613
				statusCodeInfo = "613";
				break;
			case E_P2P_SELECTCHANNEL: // Select channel 錯誤 614
				statusCodeInfo = "614";
				break;
			case E_P2P_DORELAY: // Relay 錯誤 615
				statusCodeInfo = "615";
				break;
			case E_P2P_DORELAY_ROUTECLIENT: // Route導致 Relay 錯誤 616
				statusCodeInfo = "616";
				break;
			case E_HTTP_UNKNOW: // Http 未知錯誤 617
				statusCodeInfo = "617";
				break;
			case E_MUXP2P_ADAPTER_OPEN: // Mux 開啟錯誤 618
				statusCodeInfo = "618";
				break;
			case E_MUXP2P_ADAPTER_OPEN_WAIT_TIMEOUT: // Mux等待連線交握過期 619
				statusCodeInfo = "619";
				break;
			case E_MUXP2P_ADAPTER_OPEN_SEND_CONTROL: // Mux傳送連線交握失敗 620
				statusCodeInfo = "620";
				break;
			case E_MUXP2P_ADAPTER_OPEN_ENCRYPTION: // Mux加密方法不支援 621
				statusCodeInfo = "621";
				break;
			case E_MUXP2P_ADAPTER_PLUGMUX_NO_FREE_SN: // Mux 無可用序號 622
				statusCodeInfo = "622";
				break;
			case E_MUXP2P_ADAPTER_PLUGMUX_SEND_CONTROL: // Mux傳送socket交握失敗 623
				statusCodeInfo = "623";
				break;
			case E_MUXP2P_ADAPTER_PLUGMUX_WAIT_TIMEOUT: // Mux等待socket交握過期 624
				statusCodeInfo = "624";
				break;
			case E_MUXP2P_ADAPTER_PLUGIN_MUXSOCKET: // Mux Adapter錯誤 625
				statusCodeInfo = "625";
				break;
			case E_MUXP2P_SERVICE_PLUGIN_MUXSOCKET: // Mux Service錯誤 626
				statusCodeInfo = "626";
				break;
			case E_FAIL: // P2P内部其他未知錯誤627
				statusCodeInfo = "627";
				break;
			case E_CLIENT_NET_EXCEPTION:
				statusCodeInfo = "628";
				break;
			case E_CLIENT_NON_DEVICE:
				statusCodeInfo = "629";
				break;
			case E_OTHER: // 客户端内部其他未知错误628
				statusCodeInfo = "630";
				break;
			case E_Local_SpaceShortage:
				statusCodeInfo = "631";
				break;
			case E_Cloud_SpaceShortage:
				statusCodeInfo = "632";
				break;
			case Success:
				statusCodeInfo = "OK";
				break;
		}
		return statusCodeInfo;
	}
}
