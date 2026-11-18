package circle.big.webserver;

import circle.big.cirver.core.System;
import circle.big.cirver.core.SystemContext;

public final class CircleWebServerSystem implements System {
    private CircleWebServer server;

    public void initialize(SystemContext context) throws Exception {
        server = new CircleWebServer(context.dataDirectory());
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
