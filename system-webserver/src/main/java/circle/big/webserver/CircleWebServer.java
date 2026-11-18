package circle.big.webserver;

import io.fusionauth.http.HTTPMethod;
import io.fusionauth.http.server.HTTPHandler;
import io.fusionauth.http.server.HTTPListenerConfiguration;
import io.fusionauth.http.server.HTTPResponse;
import io.fusionauth.http.server.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class CircleWebServer {
    private static final int PORT = 8880; // this should probably be configurable eventually
    private static final Logger LOGGER = LoggerFactory.getLogger(CircleWebServer.class);

    private Path serverDirectory;

    public CircleWebServer(Path server_directory) {
        this.serverDirectory = server_directory.toAbsolutePath();
    }

    private void respondNotFound(HTTPResponse res, String path) throws IOException {
        res.setStatus(404);
        Path notFoundPage = serverDirectory.resolve("404.html");
        try (var reader = Files.newBufferedReader(notFoundPage)) {
            Writer writer = res.getWriter();
            reader.transferTo(writer);
        } catch (IOException failure) {
            throw new IOException("webserver: error reading " + notFoundPage, failure);
        }
        res.close();
    }

    private final HTTPHandler HANDLER = (req, res) -> {
        HTTPMethod method = req.getMethod();
        if (method.equals(HTTPMethod.GET)) {
            String dir = req.getPath().substring(1);
            String query = req.getQueryString();
            Path requestedPath = serverDirectory.resolve(dir);
            if (requestedPath.normalize().startsWith(serverDirectory)) {
                if (!Files.exists(requestedPath)) {
                    requestedPath = requestedPath.resolveSibling(requestedPath.getFileName() + ".html");
                    if (!Files.exists(requestedPath)) {
                        respondNotFound(res, dir);
                    }
                }
                if (Files.isDirectory(requestedPath)) {
                    requestedPath = requestedPath.resolve("index.html");
                    if (!Files.exists(requestedPath)) {
                        respondNotFound(res, dir);
                    }
                }
                try (var reader = Files.newBufferedReader(requestedPath)) {
                    Writer writer = res.getWriter();
                    reader.transferTo(writer);
                } catch (IOException failure) {
                    throw new IOException("webserver: error reading " + requestedPath, failure);
                }
                res.close();
            } else {
                respondNotFound(res, dir);
            }
        } else if (method.equals(HTTPMethod.HEAD)) {
            res.close();
        } else {
            res.setHeader("Connection", "close");
            res.setStatus(405);
            res.close();
        }
    };

    private final HTTPServer server = new HTTPServer().withHandler(HANDLER).withListener(new HTTPListenerConfiguration(PORT));

    public void start() {
        LOGGER.info("Web server started on port {}", PORT);
        server.start();
    }

    public void stop() {
        server.close();
    }
}