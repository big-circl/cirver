package wilpam.wilp2p;

import circle.big.cirver.core.System;
import circle.big.cirver.core.SystemContext;
import wilpam.wilp2p.internal.ProjectPropertiesLookup;
import wilpam.wilp2p.internal.Wilp2pServer;

import java.io.IOException;

/** Hosts the wilp2p protocol. */
public final class Wilp2pSystem implements System {
    private ProjectPropertiesLookup projects;
    private Wilp2pServer server;

    @Override
    public void initialize(SystemContext context) throws IOException {
        var config = Wilp2pConfig.load(context.configDirectory());
        projects = new ProjectPropertiesLookup(config.projectsFile());
        server = new Wilp2pServer(config.address(), projects);
    }

    @Override
    public void start() throws Exception {
        server.start();
        server.awaitStartup();
    }

    /** Actual bound port, including when configured with port zero. */
    public int port() {
        if (server == null) {
            throw new IllegalStateException("wilp2p has not been initialized");
        }
        return server.getPort();
    }

    @Override
    public void stop() throws Exception {
        try {
            if (server != null) {
                server.stop();
            }
        } finally {
            if (projects != null) {
                projects.close();
            }
        }
    }
}
