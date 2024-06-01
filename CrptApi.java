package soft_master.test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.FutureRequestExecutionService;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpRequestFutureTask;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
	private int requestLimit;
	private TimeUnit timeUnit;
	
	CrptApi(TimeUnit timeUnit, int requestLimit) {
		this.timeUnit = timeUnit;
		this.requestLimit = requestLimit;
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

	private class Invoker implements Runnable {
		ExecutorService executorService = Executors.newCachedThreadPool();
		
		@Override
		public void run() {
			sendOrder("https://ismp.crpt.ru/api/v3/lk/documents/create", new Order());
		}

		private void sendOrder(String url, Order order) {
			try (FutureRequestExecutionService executionService = new FutureRequestExecutionService(
					HttpClientBuilder.create().setMaxConnPerRoute(requestLimit).setMaxConnTotal(requestLimit).build(),
					executorService)) {
				
				ResponseHandler<Boolean> handler = new ResponseHandler<Boolean>() {
					@Override
					public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
						return response.getStatusLine().getStatusCode() == 200;
					}
				};
				
				HttpPost httpPost = new HttpPost(url);
				String jsonOrder = new ObjectMapper().writer().writeValueAsString(CrptApi.this.prepareOrder(order));

				HttpEntity stringEntity = new StringEntity(jsonOrder, ContentType.APPLICATION_JSON);
				httpPost.setEntity(stringEntity);
				httpPost.setHeader("Content-type", "application/json");

				FutureCallback<Boolean> callback = new FutureCallback<Boolean>() {
					@Override
					public void completed(Boolean result) {
						System.out.println("Cоздан документ: " + result);
					}

					@Override
					public void failed(Exception ex) {
						System.out.println("Произошла ошибка: " + ex.getMessage());
					}

					@Override
					public void cancelled() {
						System.out.println("Отмена операции");
					}
				};

				HttpRequestFutureTask<Boolean> futureTask = executionService.execute(httpPost,
						HttpClientContext.create(), handler, callback);
				Boolean requisitionResult = futureTask.get(1, timeUnit);
				System.out.println("Результат заявки: " + (requisitionResult ? "успешно" : "завершено с ошибкой"));
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
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

	public static void main(String[] args) throws StreamWriteException, DatabindException, IOException, ParseException, InterruptedException {
		CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
		ThreadPoolExecutor es = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
		
		for (int i = 0; i < 10; i++) {
			es.execute(api.new Invoker());
			Thread.sleep(300);
			System.out.println("Invoke: " +i);
		}
		es.shutdown();
	}

}