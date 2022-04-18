package org.jboss.resteasy.client.jaxrs.engines.dapr;

import io.dapr.client.DaprHttp;
import io.dapr.client.domain.HttpExtension;
import io.dapr.client.domain.InvokeMethodRequest;
import io.dapr.utils.TypeRef;
import io.quarkiverse.dapr.core.SyncDaprClient;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.jboss.resteasy.client.jaxrs.engines.HttpContextProvider;
import org.jboss.resteasy.client.jaxrs.engines.SelfExpandingBufferredInputStream;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.client.jaxrs.internal.FinalizedClientResponse;
import org.jboss.resteasy.spi.config.ConfigurationFactory;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import javax.enterprise.inject.spi.CDI;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * DaprClientHttpEngineBuilder
 *
 * An Dapr HTTP engine for use with the new Builder Config style.
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class ManualClosingDaprClientEngine implements DaprClientEngine {

    static final String FILE_UPLOAD_IN_MEMORY_THRESHOLD_PROPERTY = "org.jboss.resteasy.client.jaxrs.engines.fileUploadInMemoryThreshold";

    /**
     * Used to build temp file prefix.
     */
    private static final String processId;

    static {
        try {
            processId = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    return ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^0-9a-zA-Z]", "");
                }

            });
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae);
        }

    }

    protected final SyncDaprClient daprClient;

    protected boolean closed;

    protected final boolean allowClosingHttpClient;

    protected HttpContextProvider httpContextProvider;

    protected SSLContext sslContext;

    protected HostnameVerifier hostnameVerifier;

    protected int responseBufferSize = 8192;

    protected boolean chunked = false;

    protected boolean followRedirects = false;

    /**
     * For uploading File's over JAX-RS framework, this property, together with {@link #fileUploadMemoryUnit},
     * defines the maximum File size allowed in memory. If fileSize exceeds this size, it will be stored to
     * {@link #fileUploadTempFileDir}. <br>
     * <br>
     * Defaults to 1 MB
     */
    protected int fileUploadInMemoryThresholdLimit = 1;

    /**
     * The unit for {@link #fileUploadInMemoryThresholdLimit}. <br>
     * <br>
     * Defaults to MB.
     *
     * @see MemoryUnit
     */
    protected MemoryUnit fileUploadMemoryUnit = MemoryUnit.MB;

    /**
     * Temp directory to write output request stream to. Any file to be uploaded has to be written out to the
     * output request stream to be sent to the service and when the File is too huge the output request stream is
     * written out to the disk rather than to memory. <br>
     * <br>
     * Defaults to JVM temp directory.
     */
    protected File fileUploadTempFileDir = getTempDir();

    public ManualClosingDaprClientEngine() {
        this(null, null, true);
    }

    public ManualClosingDaprClientEngine(final SyncDaprClient httpClient) {
        this(httpClient, null, true);
    }

    public ManualClosingDaprClientEngine(final SyncDaprClient httpClient, final boolean closeHttpClient) {
        this(httpClient, null, closeHttpClient);
    }

    public ManualClosingDaprClientEngine(final SyncDaprClient httpClient,
            final HttpContextProvider httpContextProvider) {
        this(httpClient, httpContextProvider, true);
    }

    private ManualClosingDaprClientEngine(final SyncDaprClient httpClient,
            final HttpContextProvider httpContextProvider, final boolean closeHttpClient) {
        this.daprClient = httpClient != null ? httpClient : CDI.current().select(SyncDaprClient.class).get();
        if (closeHttpClient && !(this.daprClient instanceof AutoCloseable)) {
            throw new IllegalArgumentException(
                    "httpClient must be a CloseableHttpClient instance in order for allowing engine to close it!");
        }
        this.httpContextProvider = httpContextProvider;
        this.allowClosingHttpClient = closeHttpClient;
        try {
            int threshold = Integer.parseInt(ConfigurationFactory.getInstance().getConfiguration()
                    .getOptionalValue(FILE_UPLOAD_IN_MEMORY_THRESHOLD_PROPERTY, String.class)
                    .orElse("1"));
            if (threshold > -1) {
                this.fileUploadInMemoryThresholdLimit = threshold;
            }
            LogMessages.LOGGER.debugf("Negative threshold, %s, specified. Using default value", threshold);
        } catch (Exception e) {
            LogMessages.LOGGER.debug("Exception caught parsing memory threshold. Using default value.", e);
        }
    }

    /**
     * Response stream is wrapped in a BufferedInputStream. Default is 8192. Value of 0 will not wrap it.
     * Value of -1 will use a SelfExpandingBufferedInputStream
     *
     * @return response buffer size
     */
    public int getResponseBufferSize() {
        return responseBufferSize;
    }

    /**
     * Response stream is wrapped in a BufferedInputStream. Default is 8192. Value of 0 will not wrap it.
     * Value of -1 will use a SelfExpandingBufferedInputStream
     *
     * @param responseBufferSize response buffer size
     */
    public void setResponseBufferSize(int responseBufferSize) {
        this.responseBufferSize = responseBufferSize;
    }

    /**
     * Based on memory unit
     *
     * @return threshold limit
     */
    public int getFileUploadInMemoryThresholdLimit() {
        return fileUploadInMemoryThresholdLimit;
    }

    public void setFileUploadInMemoryThresholdLimit(int fileUploadInMemoryThresholdLimit) {
        this.fileUploadInMemoryThresholdLimit = fileUploadInMemoryThresholdLimit;
    }

    public MemoryUnit getFileUploadMemoryUnit() {
        return fileUploadMemoryUnit;
    }

    public void setFileUploadMemoryUnit(MemoryUnit fileUploadMemoryUnit) {
        this.fileUploadMemoryUnit = fileUploadMemoryUnit;
    }

    public File getFileUploadTempFileDir() {
        return fileUploadTempFileDir;
    }

    public void setFileUploadTempFileDir(File fileUploadTempFileDir) {
        this.fileUploadTempFileDir = fileUploadTempFileDir;
    }

    public SyncDaprClient getHttpClient() {
        return daprClient;
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public static CaseInsensitiveMap<String> extractHeaders(DaprHttp.Response response) {
        final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<String>();
        response.getHeaders().entrySet().forEach(e->headers.add(e.getKey(),e.getValue()));
        return headers;
    }

    protected InputStream createBufferedStream(InputStream is) {
        if (responseBufferSize == 0) {
            return is;
        }
        if (responseBufferSize < 0) {
            return new SelfExpandingBufferredInputStream(is);
        }
        return new BufferedInputStream(is, responseBufferSize);
    }

    @Override
    public Response invoke(Invocation inv) {
        ClientInvocation request = (ClientInvocation) inv;
        URI uri = request.getUri();
        final InvokeMethodRequest daprRequest = new InvokeMethodRequest(uri.getHost(), uri.getPath());
        //      final InvokeMethodRequest httpMethod = createHttpMethod(uri, request.getMethod());
        final DaprHttp.Response res;
        try {
            loadHttpMethod(request, daprRequest);
            res = daprClient.invokeMethod(daprRequest,TypeRef.get(DaprHttp.Response.class));
        } catch (Exception e) {
            LogMessages.LOGGER.clientSendProcessingFailure(e);
            throw new ProcessingException(Messages.MESSAGES.unableToInvokeRequest(e.toString()), e);
        }

        ClientResponse response = new FinalizedClientResponse(request.getClientConfiguration(), request.getTracingLogger()) {
            InputStream stream;

            @Override
            protected void setInputStream(InputStream is) {
                stream = is;
                resetEntity();
            }

            @Override
            public InputStream getInputStream() {
                if (stream == null) {
                    stream = new ByteArrayInputStream(res.getBody());
                }
                return stream;
            }

            @Override
            public void releaseConnection() throws IOException {
                releaseConnection(true);
            }

            @Override
            public void releaseConnection(boolean consumeInputStream) throws IOException {
                if (consumeInputStream) {
                    if (stream != null) {
                        stream.close();
                    } else {
                        InputStream is = getInputStream();
                        if (is != null) {
                            is.close();
                        }
                    }

                }
            }

        };
        response.setProperties(request.getMutableProperties());
        response.setStatus(res.getStatusCode());
        //        response.setReasonPhrase(res.getStatusLine().getReasonPhrase());
        response.setHeaders(extractHeaders(res));
        response.setClientConfiguration(request.getClientConfiguration());
        return response;
    }

    protected void loadHttpMethod(final ClientInvocation request, InvokeMethodRequest httpRequest) throws Exception {

        MediaType mediaType = request.getHeaders().getMediaType();
        if (Objects.nonNull(mediaType)) {
            httpRequest.setContentType(mediaType.toString());
        }

        DaprHttp.HttpMethods httpMethod = DaprHttp.HttpMethods.valueOf(request.getMethod());
        Object requestEntity = request.getEntity();
        if (requestEntity != null) {
            if (httpMethod == DaprHttp.HttpMethods.GET) {
                throw new ProcessingException(Messages.MESSAGES.getRequestCannotHaveBody());
            }

            httpRequest.setBody(requestEntity);

            commitQueryAndHeaders(request, httpRequest);
        } else // no body
        {
            commitQueryAndHeaders(request, httpRequest);
        }
    }

    protected void commitQueryAndHeaders(ClientInvocation request, InvokeMethodRequest httpRequest) {

        MultivaluedMap<String, String> headers = request.getHeaders().asMap();

        MultivaluedMap<String, String> query = buildQuery(request);

        Map<String, String> daprHeaders=new HashMap<>();
        headers.forEach((k,v)->daprHeaders.put(k,v.get(0)));

        HttpExtension httpExtension = new HttpExtension(
                DaprHttp.HttpMethods.valueOf(request.getMethod()),
                query,
                daprHeaders);
        httpRequest.setHttpExtension(httpExtension);
    }

    private MultivaluedMap<String, String> buildQuery(ClientInvocation request) {
        MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        String rawQuery = request.getUri().getRawQuery();
        if (Objects.nonNull(rawQuery)) {
            for (String q : rawQuery.split("&")) {
                String[] split = q.split("=");
                if (split.length > 1) {
                    query.add(split[0], split[1]);
                }
            }

        }
        return query;
    }

    public boolean isChunked() {
        return chunked;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    @Override
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    @Override
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Creates the request OutputStream, to be sent to the end Service invoked, as a
     * <a href="http://commons.apache.org/io/api-release/org/apache/commons/io/output/DeferredFileOutputStream.html"
     * >DeferredFileOutputStream</a>.
     *
     * @param request -
     * @return - DeferredFileOutputStream with the ClientRequest written out per HTTP specification.
     * @throws IOException -
     */
    private DeferredFileOutputStream writeRequestBodyToOutputStream(final ClientInvocation request) throws IOException {
        DeferredFileOutputStream memoryManagedOutStream = new DeferredFileOutputStream(
                this.fileUploadInMemoryThresholdLimit * getMemoryUnitMultiplier(), getTempfilePrefix(), ".tmp",
                this.fileUploadTempFileDir);
        request.getDelegatingOutputStream().setDelegate(memoryManagedOutStream);
        request.writeRequestBody(request.getEntityStream());
        memoryManagedOutStream.close();
        return memoryManagedOutStream;
    }

    /**
     * Use context information, which will include node name, to avoid conflicts in case of multiple VMS using same
     * temp directory location.
     *
     * @return -
     */
    protected String getTempfilePrefix() {
        return processId;
    }

    /**
     * @return - the constant to multiply {@link #fileUploadInMemoryThresholdLimit} with based on
     *         {@link #fileUploadMemoryUnit} enumeration value.
     */
    private int getMemoryUnitMultiplier() {
        switch (this.fileUploadMemoryUnit) {
            case BY:
                return 1;
            case KB:
                return 1024;
            case MB:
                return 1024 * 1024;
            case GB:
                return 1024 * 1024 * 1024;
        }
        return 1;
    }

    /**
     * Log that the file did not get deleted but prevent the request from failing by eating the exception.
     * Register the file to be deleted on exit, so it will get deleted eventually.
     *
     * @param tempRequestFile -
     * @param ex - a null may be passed in which case this param gets ignored.
     */
    private void handleFileNotDeletedError(File tempRequestFile, Exception ex) {
        LogMessages.LOGGER.warn(Messages.MESSAGES.couldNotDeleteFile(tempRequestFile.getAbsolutePath()), ex);
        tempRequestFile.deleteOnExit();
    }

    @Override
    public void close() {
        //        if (closed)
        //            return;
        //
        //        if (allowClosingHttpClient && httpClient != null) {
        //            try {
        //                ((CloseableHttpClient) httpClient).close();
        //            } catch (Exception e) {
        //                throw new RuntimeException(e);
        //            }
        //        }
        closed = true;
    }

    private static File getTempDir() {
        if (System.getSecurityManager() == null) {
            final Optional<String> value = ConfigurationFactory.getInstance().getConfiguration()
                    .getOptionalValue("java.io.tmpdir", String.class);
            return value.map(File::new).orElseGet(() -> new File(System.getProperty("java.io.tmpdir")));
        }
        return AccessController.doPrivileged((PrivilegedAction<File>) () -> {
            final Optional<String> value = ConfigurationFactory.getInstance().getConfiguration()
                    .getOptionalValue("java.io.tmpdir", String.class);
            return value.map(File::new).orElseGet(() -> new File(System.getProperty("java.io.tmpdir")));
        });
    }

}
