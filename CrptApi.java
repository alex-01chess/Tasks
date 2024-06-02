package soft_master.test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
	private int requestLimit;
	private TimeUnit timeUnit;
	private Scheduler scheduler; 
	
	public Scheduler getScheduler() {
		return scheduler;
	}

	CrptApi(TimeUnit timeUnit, int requestLimit) {
		this.timeUnit = timeUnit;
		this.requestLimit = requestLimit;
		this.scheduler = new Scheduler(this.requestLimit);
	}

	private Order prepareOrder(Order order) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		soft_master.test.CrptApi.Order.IdNumber id_number = order.new IdNumber();
		id_number.participantInn = "12345";
		order.description = id_number;
		order.docId = "10";
		order.docStatus = "current";
		order.docType = "LP_INTRODUCE_GOODS";
		order.importRequest = true;
		order.ownerInn = "owner";
		order.participantInn = "01234";
		order.producerInn = "02345";
		order.productionDate = df.parse("2020-01-23");
		order.productionType = "type";
		soft_master.test.CrptApi.Order.Product product = order.new Product();
		product.certificateDocument = "document";
		product.certificateDocumentDate = df.parse("2020-01-23");
		product.certificateDocumentNumber = "number";
		product.ownerInn = "owner";
		product.producerInn = "producer";
		product.productionDate = df.parse("2020-01-23");
		product.tnvedCode = "code";
		product.uitCode = "uit";
		product.uituCode = "uitu";
		order.products.add(product);
		order.regDate = df.parse("2020-01-23");
		order.regNumber = "1";

		return order;
	}

	public void createOrder(Order order) throws ParseException {
		order = prepareOrder(order);
		scheduler.sendOrderBySchedule("https://ismp.crpt.ru/api/v3/lk/documents/create", order);
	}
	
	private class Scheduler {
		private ScheduledExecutorService schedulerService;
		private Semaphore semaphore;
		
		private Scheduler(int poolSize) {
			schedulerService = Executors.newScheduledThreadPool(poolSize);
			semaphore = new Semaphore(poolSize);
		}
		
		private void sendOrderBySchedule(String url, Order order) {
			schedulerService.schedule(new Issue(url, order, semaphore), 1, timeUnit);
		}
		
		private void sendOrderBySchedule(String url, Order order, long delay) {
			schedulerService.schedule(new Issue(url, order, semaphore), delay, timeUnit);
		}
		
		private void stop() {
			schedulerService.shutdown();
		}
		
	}
	
	private class Issue implements Runnable {
		private String url;
		private Order order;
		private Semaphore semaphore;
		
		private Issue(String url, Order order, Semaphore semaphore) {
			this.url = url;
			this.order = order;
			this.semaphore = semaphore;
		}
		
		@Override
		public void run() {
			sendOrder(url, order);
		}

		private void sendOrder(String url, Order order) {
			try (CloseableHttpClient httpClient = HttpClientBuilder.create().setMaxConnPerRoute(requestLimit).setMaxConnTotal(requestLimit * 2).build()) {
				HttpPost httpPost = new HttpPost(url);
				String jsonOrder = new ObjectMapper().writer().writeValueAsString(order);

				StringEntity stringEntity = new StringEntity(jsonOrder, ContentType.APPLICATION_JSON);
				httpPost.setEntity(stringEntity);
				httpPost.setHeader("Content-type", "application/json");
				
				semaphore.acquire();
				try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
					//Thread.sleep(1000);
					System.out.println(response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
					
					HttpEntity enttity = response.getEntity();
					String result = EntityUtils.toString(enttity);
					System.out.println(result);
					
					EntityUtils.consume(enttity);
				}
				semaphore.release();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

	@JsonAutoDetect
	@JsonPropertyOrder(alphabetic = true)
	private class Order {
		@JsonProperty
		private IdNumber description;
		@JsonProperty("doc_id")
		private String docId;
		@JsonProperty("doc_status")
		private String docStatus;
		@JsonProperty("doc_type")
		private String docType;
		@JsonProperty
		private Boolean importRequest;
		@JsonProperty("owner_inn")
		private String ownerInn;
		@JsonProperty("participant_inn")
		private String participantInn;
		@JsonProperty("producer_inn")
		private String producerInn;
		@JsonProperty("production_date")
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		private Date productionDate;
		@JsonProperty("production_type")
		private String productionType;
		@JsonProperty("products")
		private List<Product> products = new ArrayList<Product>();
		@JsonProperty("reg_date")
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
		private Date regDate;
		@JsonProperty("reg_number")
		private String regNumber;

		@JsonAutoDetect
		private class IdNumber {
			@JsonProperty
			private String participantInn;
		}

		@JsonAutoDetect
		private class Product {
			@JsonProperty("certificate_document")
			private String certificateDocument;
			@JsonProperty("certificate_document_date")
			@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
			private Date certificateDocumentDate;
			@JsonProperty("certificate_document_number")
			private String certificateDocumentNumber;
			@JsonProperty("owner_inn")
			private String ownerInn;
			@JsonProperty("producer_inn")
			private String producerInn;
			@JsonProperty("production_date")
			@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
			private Date productionDate;
			@JsonProperty("tnved_code")
			private String tnvedCode;
			@JsonProperty("uit_code")
			private String uitCode;
			@JsonProperty("uitu_code")
			private String uituCode;
		}

	}

	public static void main(String[] args) throws ParseException, InterruptedException {
		CrptApi api = new CrptApi(TimeUnit.SECONDS, 3);
		
		for (int i = 0; i < 10; i++) {
			api.createOrder(api.new Order());
			// Thread.sleep(100);
		}
		
		// api.getScheduler().stop();
	}

}
