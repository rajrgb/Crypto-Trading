package com.crypto.trade;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;

class Trade{
    
	private String apiKey="Binance_api_key";
	private String secretKey="api_secret";
	
	private RestTemplate restTemplate;
	Trade(){
		restTemplate=new RestTemplate();
	}

	
	public String executeTrade(String symbol, String side, String type, double quantity, double price) throws URISyntaxException{
		  long timestamp=System.currentTimeMillis();
		  System.out.println("Executing trade..");
		
	      String queryString = "symbol="+symbol+"&positionSide=BOTH&side="+side+"&type="+type+"&quantity="+quantity+"&timestamp="+timestamp;
	      if(type.equals("LIMIT"))
	    	  queryString=queryString+"&timeInForce=GTC"+"&price="+price;
          
	        String signature="";
	        try {
	         signature = generateSignature(secretKey, queryString);
	        }catch(Exception e) {
	        	System.out.println(e.getMessage());
	        }
	      
	        
		queryString=queryString+"&signature="+signature;
		
		String url="https://fapi.binance.com/fapi/v1/order";
		URI uri=new URI(url);
		
		
		//headers
		MultiValueMap<String, String> headers=new LinkedMultiValueMap<>();
		headers.add(HttpHeaders.ACCEPT, "application/json");
		headers.add("X-MBX-APIKEY",apiKey);
		headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
	

		//request
		RequestEntity<String> requestEntity=new RequestEntity<>(queryString, headers, HttpMethod.POST,uri);
		
		ResponseEntity<String> responseEntity=restTemplate.exchange(requestEntity, String.class);
	
		String response=responseEntity.getBody();
		System.out.println(response);
		return response;
	}
	
	public List<List<String>> fetchCurrentOhlc(String url, String symbol, String interval, String limit, String endTime) throws JsonMappingException, JsonProcessingException{
		MultiValueMap<String, String> headers=new LinkedMultiValueMap<>();
		
		
		
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
	                .queryParam("symbol", symbol)
	                .queryParam("interval", interval)
	                .queryParam("limit", limit)
		            .queryParam("endTime",endTime);
	        String uri = builder.toUriString();
		
		
		headers.add(HttpHeaders.ACCEPT, "application/json");
		RequestEntity<Void> requestEntity=new RequestEntity<>( headers, HttpMethod.GET,URI.create(uri));
		ResponseEntity<String> responseEntity=restTemplate.exchange(requestEntity, String.class);
		String response=responseEntity.getBody();
		ObjectMapper objectMapper=new ObjectMapper();
	    List<List<String>> result=objectMapper.readValue(response, new TypeReference<List<List<String>>>() {});
	    Collections.reverse(result);
		return result;
	}
	public String convertTime(long milliseconds) {
		Instant instant = Instant.ofEpochMilli(milliseconds);
		LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String result = dateTime.format(formatter);
		return result;
	}
	public void printRecentCandle(List<List<String>> candles) {
		
			Long milliseconds=Long.parseLong(candles.get(0).get(0));
			String fetchTime = convertTime(milliseconds);
			System.out.println(fetchTime);
			System.out.println("O: "+candles.get(0).get(1));
			System.out.println("H: "+candles.get(0).get(2));
			System.out.println("L: "+candles.get(0).get(3));
			System.out.println("C: "+candles.get(0).get(4));
			System.out.println("");
		
	}
	
	public String closeAllOrders() throws URISyntaxException{
		System.out.println("Cancelling all open orders..");
		long timestamp=System.currentTimeMillis();
		String symbol="BTCUSDT";
		String url="https://fapi.binance.com/fapi/v1/allOpenOrders";
		URI uri=new URI(url);
		
		String queryString="symbol="+symbol+"&timestamp="+timestamp;
		
		 String signature="";
	        try {
	         signature = generateSignature(secretKey, queryString);
	        }catch(Exception e) {
	        	System.out.println(e.getMessage());
	        }
	        queryString=queryString+"&signature="+signature;
		//headers
		MultiValueMap<String, String> headers=new LinkedMultiValueMap<>();
		headers.add(HttpHeaders.ACCEPT, "application/json");
		headers.add("X-MBX-APIKEY",apiKey);
		headers.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
	

		//request
		RequestEntity<String> requestEntity=new RequestEntity<>(queryString, headers, HttpMethod.DELETE,uri);
		
		ResponseEntity<String> responseEntity=restTemplate.exchange(requestEntity, String.class);
	
		String response=responseEntity.getBody();
		System.out.println(response);
		return response;
	}
	
	//starts trading:
	public void startTrade() throws InterruptedException, URISyntaxException, JsonProcessingException{
		int multiplier=3;
		int period=22;
		
		ChandelierExit chandelierExit=new ChandelierExit();
		chandelierExit.setPeriod(period);
		chandelierExit.setMultiplier(multiplier);
		int size=period+2;
		String signal="No buy or sell signal.";
		double entryPrice=-1.0;
		double targetPrice=-1.0;
		double quantity=-1.0;
		boolean openPosition=false;
		double totalProfit=0.0;
		double inputPrice=45.0;
		double targetPoint=100;
		int leverage=20;
		//Loop:
		while(true) {
		
		long currentTimeMillis = System.currentTimeMillis();

		String url= "https://fapi.binance.com";
		String symbol="BTCUSDT";
		String interval="15m";
		String limit="25";
		List<List<String>> candles=null;
		try {
		candles=fetchCurrentOhlc(url+"/fapi/v1/klines", symbol, interval, "2", ""+currentTimeMillis);
		}catch(Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		
		long entryTime=Long.parseLong(candles.get(0).get(0))+900*1000;
		long delay=entryTime-currentTimeMillis;
        
		if(delay<0)
		{
			entryTime=entryTime+900*1000;
			delay=entryTime-currentTimeMillis;
		}
        System.out.println("Next execution at: "+convertTime(entryTime)+"\n");
		
		
		Thread.sleep(delay);
		
		System.out.println("Current server time: "+convertTime(System.currentTimeMillis()));
		
		
		long firstCandleOpenTime=entryTime-60*1000;
		
		candles=fetchCurrentOhlc(url+"/fapi/v1/klines", symbol, interval, limit, ""+firstCandleOpenTime);
		
		System.out.println("Candles data fetched successfully.");
		printRecentCandle(candles);
		System.out.println("");
		double openingPrices[]=new double[size];
		double highPrices[]=new double[size];
		double lowPrices[]=new double[size];
		double closingPrices[]=new double[size];
		
	
		
		System.out.println("");
		int i=0;
		for(List<String> candle: candles) {
			if(i<24) {
			openingPrices[i]=Double.parseDouble(candle.get(1));
			highPrices[i]=Double.parseDouble(candle.get(2));
			lowPrices[i]=Double.parseDouble(candle.get(3));
			closingPrices[i]=Double.parseDouble(candle.get(4));
			}
			i++;
		}
		chandelierExit.setHighPrices(highPrices);
		chandelierExit.setLowPrices(lowPrices);
		chandelierExit.setClosingPrices(closingPrices);
	
		double chandelierLong=chandelierExit.calChandelierExitLong();
		double chandelierShort=chandelierExit.calChandelierExitShort();
		
		System.out.println("Chandelier Long= "+chandelierLong);
		System.out.println("Chandelier Short= "+chandelierShort);
		
		if(openPosition && signal.equals("BUY") && targetPrice <= highPrices[0]) 
			{
				openPosition=false;
				double profit=targetPoint*quantity;
				   System.out.println("Long position closed.");
				   System.out.println("Entry price= $"+entryPrice);
				   double closed=entryPrice+targetPoint;
				   System.out.println("Exit price= $"+closed);
				   System.out.println("Profit= $"+ profit);
				   totalProfit=totalProfit+profit;
				   System.out.println("Total profit= $"+totalProfit);
			}
		if(openPosition && signal.equals("SELL") && targetPrice >= lowPrices[0])
			{
				openPosition=false;
				double profit=targetPoint*quantity;
				   System.out.println("Short position closed.");
				   System.out.println("Entry price= $"+entryPrice);
				   double closed=entryPrice-targetPoint;
				   System.out.println("Exit price= $"+closed);
				   System.out.println("Profit= $"+ profit);
				   totalProfit=totalProfit+profit;
				   System.out.println("Total profit= $"+totalProfit);
			}
		
		
		
		
		if(signal.equals("BUY")==false && closingPrices[0]-chandelierShort >= chandelierShort-openingPrices[0] && chandelierLong-openingPrices[0] < closingPrices[0]-chandelierLong)
			{ 
			   System.out.println("Long trade signal generated.");
			   signal="BUY";
			   if(openPosition) {
				   double profit=(entryPrice-closingPrices[0])*quantity;
				   System.out.println("Short position closed.");
				   System.out.println("Entry price= $"+entryPrice);
				   System.out.println("Exit price= $"+closingPrices[0]);
				   System.out.println("Profit= $"+ profit);
				   totalProfit=totalProfit+profit;
				   System.out.println("Total profit= $"+totalProfit);
				   closeAllOrders();
				   Thread.sleep(1000);
				   executeTrade(symbol,signal,"MARKET",quantity, -1.0);
			   }
			   openPosition=true;
			   quantity=(inputPrice*leverage)/closingPrices[0];
			   DecimalFormat decimalFormat = new DecimalFormat("#.###");
			   String formattedValue = decimalFormat.format(quantity);
			   quantity=Double.parseDouble(formattedValue);
			   targetPrice=closingPrices[0]+targetPoint;
			   entryPrice=closingPrices[0];
			   executeTrade(symbol,signal,"MARKET",quantity,-1.0); 
			   Thread.sleep(1000);
			   executeTrade(symbol,"SELL","LIMIT",quantity,targetPrice);
			}
		if(signal.equals("SELL")==false && openingPrices[0]-chandelierLong<= chandelierLong-closingPrices[0] && openingPrices[0]-chandelierShort < chandelierShort - closingPrices[0])
			{
			
			   System.out.println("Short trade signal generated.");
			   signal="SELL";
			   if(openPosition) {
				   double profit=(closingPrices[0]-entryPrice)*quantity;
				   System.out.println("Long position closed.");
				   System.out.println("Entry price= $"+entryPrice);
				   System.out.println("Exit price= $"+closingPrices[0]);
				   System.out.println("Profit= $"+ profit);
				   totalProfit=totalProfit+profit;
				   System.out.println("Total profit= $"+totalProfit);
				   closeAllOrders();
				   Thread.sleep(1000);
				   executeTrade(symbol,signal,"MARKET",quantity, -1.0);
			   }
			   openPosition=true;
			   quantity=(inputPrice*leverage)/closingPrices[0];
			   DecimalFormat decimalFormat = new DecimalFormat("#.###");
			   String formattedValue = decimalFormat.format(quantity);
			   quantity=Double.parseDouble(formattedValue);
			   targetPrice=closingPrices[0]-targetPoint;
			   entryPrice=closingPrices[0];
			   executeTrade(symbol,signal,"MARKET",quantity,-1.0); 
			   Thread.sleep(1000);
			   executeTrade(symbol,"BUY","LIMIT",quantity,targetPrice);
			}
	
	System.out.println("Last signal generated: "+signal+"\n");
	
	Thread.sleep(10000);


	}
		

	}
	
	 private static String generateSignature(String secretKey, String queryString) throws NoSuchAlgorithmException, InvalidKeyException {
		
	        byte[] key = secretKey.getBytes();

	        String gHmac = Hashing.hmacSha256(key)
	                .newHasher()    
	                .putString(queryString, StandardCharsets.UTF_8)
	                .hash()
	                .toString();
	        
	        return gHmac;
	    }

}



@SpringBootApplication
public class BinanceAlgoTradingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BinanceAlgoTradingApplication.class, args);
		long currentTimeMillis = System.currentTimeMillis();
		
		Trade t=new Trade();
		System.out.println("");
		System.out.println("Application started at: "+t.convertTime(currentTimeMillis)+"\n");
	
		try {
		
			t.startTrade();
//			String symbol="BTCUSDT";
//			String signal="SELL";
//			double quantity=0.004;
//			long entryTime=currentTimeMillis;
//			double targetPrice=25900.00;
//			 t.executeTrade(symbol,signal,"MARKET",quantity,entryTime,-1.0); 
//			 t.executeTrade(symbol,"BUY","LIMIT",quantity,entryTime,targetPrice);
		}catch(Exception e) {
			System.out.println(e.getMessage());
			System.out.println("Application encountered a problem.");
		}
	}
   
}
