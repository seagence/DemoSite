import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;

/**
 * Usage: java VirtualUsersForMultithreadingDefect numberOfVirtualUsers totalNumberOfRequestsToSend
 * @author Srinivas
 *
 */
public class VirtualUsersForMultithreadingDefect {

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		int numberOfVirtualUsers = 8;// default
		if(args.length == 1) {
			numberOfVirtualUsers = Integer.parseInt(args[0]);
		}
		int totalNumberOfRequestsToSend = 500;// default
		if(args.length == 2) {
			totalNumberOfRequestsToSend = Integer.parseInt(args[1]);
		}
		System.out.println("Starting with " + numberOfVirtualUsers + " threads and " + totalNumberOfRequestsToSend + " total requests");
		createVirtualUsersAdnSubmit(numberOfVirtualUsers, totalNumberOfRequestsToSend);
		System.out.println("Done...");
	}

	public static void createVirtualUsersAdnSubmit(int numberOfVirtualUsers, int totalNumberOfRequestsToSend) throws IOException, InterruptedException, ExecutionException {
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfVirtualUsers);
		List<Callable<String>> callables = create500AddItemToCartRequests(totalNumberOfRequestsToSend);
		executorService.invokeAll(callables);
		executorService.shutdown();
	}

	public static List<Callable<String>> create500AddItemToCartRequests(int totalNumberOfRequestsToSend) {
		int productIds [] = {4,5,6,7,8,9,10,11,12,13};
		List<Callable<String>> callables = new ArrayList<Callable<String>>();
		for (int i = 0; i < totalNumberOfRequestsToSend; i++) {
			int productIdIndex = i%productIds.length;
			callables.add(new Callable<String>() {
				int productId = productIds[productIdIndex];
				public String call() {
					try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
						
						HttpClientContext context = HttpClientContext.create();
						ClassicHttpRequest httpPost = ClassicRequestBuilder.post("http://localhost:8083/cart/add")
								.setEntity(new UrlEncodedFormEntity(
										Arrays.asList(new BasicNameValuePair("productId", "" + productId),
												new BasicNameValuePair("quantity", "1"),
												new BasicNameValuePair("hasProductOptions", "false"))))
								.build();
						httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
						httpPost.setHeader("Accept-Language", "en-US,en;q=0.9");
						httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
						httpPost.setHeader("Origin", "http://localhost:8083");
						httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
						String resultTemp = httpclient.execute(httpPost, context, response -> {
							org.apache.hc.core5.http.Header header = response.getHeader("result");
							if (header != null && "success".equalsIgnoreCase(header.getValue())) {
								return "success";
							} else {
								return "fail";
							}
						});
					} catch (IOException exc) {
						exc.printStackTrace();
					}
					return null;
				};
			});
		}
		return callables;
	}
}
