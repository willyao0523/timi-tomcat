package server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor {
    public HttpProcessor() {
    }

    public void process(Socket socket) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = socket.getInputStream();
            output = socket.getOutputStream();
            Request request = new Request(input);
            request.parse();
            Response response = new Response(output);
            response.setRequest(request);

            if (request.getUri().startsWith("/servlet/")) {
                ServletProcessor processor = new ServletProcessor();
                processor.process(request, response);
            } else {
                StaticResourceProcessor processor = new StaticResourceProcessor();
                processor.process(request, response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
