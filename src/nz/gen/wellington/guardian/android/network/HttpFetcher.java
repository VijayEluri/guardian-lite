package nz.gen.wellington.guardian.android.network;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HttpFetcher {
	
	private static final String TAG = "HttpFetcher";
	
    public static final String DOWNLOAD_PROGRESS = "nz.gen.wellington.guardian.android.network.DOWNLOAD_PROGRESS";
    
	HttpClient client;
	Context context;
	private boolean running;
		
	public static final int DOWNLOAD_STARTED = 1;
	public static final int DOWNLOAD_UPDATE = 2;
	public static final int DOWNLOAD_COMPLETED = 3;

	
	public HttpFetcher(Context context) {
		this.context =context;
		running = true;

		client = new DefaultHttpClient();
		((AbstractHttpClient) client)
		.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(final HttpRequest request,
					final HttpContext context)
					throws HttpException, IOException {
				if (!request.containsHeader("Accept-Encoding")) {
					request.addHeader("Accept-Encoding", "gzip");
					Log.i(TAG, "Added gzip header");
				}
			}
		});
    			  
		((AbstractHttpClient) client)
		.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(final HttpResponse response,
					final HttpContext context)
					throws HttpException, IOException {
				HttpEntity entity = response.getEntity();
				Header ceheader = entity.getContentEncoding();
				if (ceheader != null) {
					HeaderElement[] codecs = ceheader.getElements();
					for (int i = 0; i < codecs.length; i++) {
						if (codecs[i].getName().equalsIgnoreCase("gzip")) {
							Log.i(TAG, "Got gzip response");
							response.setEntity(new GzipDecompressingEntity(
									response.getEntity()));
							return;
						}
					}
				}
			}
		});
	}


	public InputStream httpFetch(String uri) {
		try {
			Log.i(TAG, "Making http fetch of: " + uri);						
			HttpGet get = new HttpGet(uri);	
			
			get.addHeader(new BasicHeader("User-agent", "gzip"));
			get.addHeader(new BasicHeader("Accept-Encoding", "gzip"));
			
			HttpResponse execute = client.execute(get);			
			if (execute.getStatusLine().getStatusCode() == 200) {
				
				long contentLength = execute.getEntity().getContentLength();
				Log.d(TAG, "Content length: " + contentLength);
				
				BufferedInputStream is = new BufferedInputStream(execute.getEntity().getContent(), 1024);				
				return is;				
			}
			return null;
			
		} catch (Exception e) {
			Log.w(TAG, e.getMessage());
		}
		return null;
	}

	
	public byte[] httpFetchStream(String uri) {
		try {
			Log.i(TAG, "Making http fetch of image: " + uri);
			HttpGet get = new HttpGet(uri);		
			return EntityUtils.toByteArray(client.execute(get).getEntity());
									
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return null;
	}

	
	public void stopLoading() {
		Log.d(TAG, "Stopping loading");
		running = false;
	} 

	
	private void announceDownloadStart(String url) {
		Intent intent = new Intent(DOWNLOAD_PROGRESS);
		intent.putExtra("type", DOWNLOAD_STARTED);
		intent.putExtra("url", url);
		context.sendBroadcast(intent);
	}
	
	private void announceProgress(String url, long contentLength, int totalRead) {
		Intent intent = new Intent(DOWNLOAD_PROGRESS);
		intent.putExtra("type", DOWNLOAD_UPDATE);
		intent.putExtra("url", url);
		intent.putExtra("bytes_received", totalRead);
		intent.putExtra("bytes_expected", contentLength);
		context.sendBroadcast(intent);
	}
	
	private void announceDownloadCompleted(String url) {
		Intent intent = new Intent(DOWNLOAD_PROGRESS);
		intent.putExtra("type", DOWNLOAD_COMPLETED);
		intent.putExtra("url", url);
		context.sendBroadcast(intent);
	}

	
	static class GzipDecompressingEntity extends HttpEntityWrapper {

        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }
    
        @Override
        public InputStream getContent()
            throws IOException, IllegalStateException {

            // the wrapped entity's getContent() decides about repeatability
            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }

        @Override
        public long getContentLength() {
            return this.wrappedEntity.getContentLength();
        }
    }

}
