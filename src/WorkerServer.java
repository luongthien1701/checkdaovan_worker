import model.Request;
import model.Response;
import service.PlagiarismDetector;
import util.JsonUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class WorkerServer {
    private static final int PORT = 8888;
    private static final int WORKERS = 4;
    private static final Path DATA_DIR = Paths.get("data_text");

    private final BlockingQueue<ClientTask> queue = new LinkedBlockingQueue<>();
    private final ExecutorService workerPool = Executors.newFixedThreadPool(WORKERS);
    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final PlagiarismDetector detector = new PlagiarismDetector(DATA_DIR);
    private volatile boolean running = true;

    public static void main(String[] args) {
        new WorkerServer().start();
    }

    private void start() {
        startWorkers();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Worker listening on port " + PORT);
            System.out.println("Using data directory: " + DATA_DIR.toAbsolutePath());
            while (running) {
                Socket socket = serverSocket.accept();
                clientPool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            System.err.println("Server socket error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void startWorkers() {
        for (int i = 0; i < WORKERS; i++) {
            workerPool.submit(() -> {
                while (running) {
                    try {
                        ClientTask task = queue.take();
                        process(task);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }

    private void process(ClientTask task) {
        Response response = detector.handle(task.request());
        try {
            task.send(JsonUtil.toJson(response));
        } catch (IOException e) {
            System.err.println("Failed to send response: " + e.getMessage());
        } finally {
            task.close();
        }
    }

    private void handleClient(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String payload = reader.readLine();
            if (payload == null || payload.isEmpty()) {
                throw new IOException("Empty payload");
            }
            Request request = JsonUtil.parseRequest(payload);
            if (request.getFullText() == null) {
                throw new IOException("Invalid request");
            }
            queue.put(new ClientTask(request, socket));
        } catch (Exception e) {
            sendError(socket, e.getMessage());
        }
    }

    private void sendError(Socket socket, String message) {
        try {
            Response response = new Response();
            response.setStatus("ERROR");
            response.setErrorMessage(message);
            OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(JsonUtil.toJson(response));
            writer.write("\n");
            writer.flush();
        } catch (IOException ignored) {
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void shutdown() {
        running = false;
        workerPool.shutdownNow();
        clientPool.shutdownNow();
    }

    private static class ClientTask {
        private final Request request;
        private final Socket socket;

        ClientTask(Request request, Socket socket) {
            this.request = request;
            this.socket = socket;
        }

        Request request() {
            return request;
        }

        void send(String json) throws IOException {
            OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(json);
            writer.write("\n");
            writer.flush();
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}

