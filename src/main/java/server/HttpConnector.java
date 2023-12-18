package server;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;

public class HttpConnector implements Runnable {
    int minProcessors = 3;
    int maxProcessors = 10;
    int curProcessors = 0;
    Deque<HttpProcessor> processors = new ArrayDeque<>();
    @Override
    public void run() {
        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (int i = 0; i < minProcessors; i++) {
            HttpProcessor initProcessor = new HttpProcessor(this);
            initProcessor.start();
            processors.push(initProcessor);
        }
        curProcessors = minProcessors;

        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                HttpProcessor processor = createProcessor();
                if (processor == null) {
                    socket.close();
                    continue;
                }
                processor.assign(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HttpProcessor createProcessor() {
        synchronized (processors) {
            if (processors.size() > 0) {
                return processors.pop();
            }

            if (curProcessors < maxProcessors) {
                return (newProcessor());
            } else {
                return null;
            }
            
        }
    }

    private HttpProcessor newProcessor() {
        HttpProcessor initProcessor = new HttpProcessor(this);
        initProcessor.start();
        processors.push(initProcessor);
        curProcessors ++;
        return processors.pop();
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void recycle(HttpProcessor processor) {
        processors.push(processor);
    }
}
