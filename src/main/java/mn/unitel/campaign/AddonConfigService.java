package mn.unitel.campaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class AddonConfigService {
    private static final Logger logger = Logger.getLogger(AddonConfigService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> configRef = new AtomicReference<>(NullNode.getInstance());
    private final Path configPath = Paths.get(System.getProperty("user.dir"), "config", "data-addon-config.json");

    private WatchService watchService;
    private ExecutorService watcherExecutor;

    @PostConstruct
    void init() {
        loadConfig();
        startWatcher();
    }

    @PreDestroy
    void shutdown() {
        if (watcherExecutor != null) {
            watcherExecutor.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.debug("Failed to close watch service", e);
            }
        }
    }

    public JsonNode getConfig() {
        return configRef.get();
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    private void loadConfig() {
        if (!Files.exists(configPath)) {
            logger.warnf("Config file not found: %s", configPath.toAbsolutePath());
            configRef.set(NullNode.getInstance());
            return;
        }

        try (InputStream inputStream = Files.newInputStream(configPath)) {
            JsonNode config = mapper.readTree(inputStream);
            if (config == null) {
                config = NullNode.getInstance();
            }
            configRef.set(config);
            logger.infof("Loaded config from %s", configPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to load data-addon-config.json", e);
            configRef.set(NullNode.getInstance());
        }
    }

    private void startWatcher() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path directory = configPath.getParent();
            if (directory == null) {
                logger.warn("Config path does not have a parent directory to watch");
                return;
            }

            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            watcherExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "addon-config-watcher");
                thread.setDaemon(true);
                return thread;
            });
            watcherExecutor.submit(this::processEvents);
        } catch (IOException e) {
            logger.error("Failed to start config watch service", e);
        }
    }

    @SuppressWarnings("BusyWait")
    private void processEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                Path changed = (Path) event.context();
                if (Objects.equals(changed, configPath.getFileName())) {
                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        logger.warnf("Config file %s was deleted", configPath.toAbsolutePath());
                        configRef.set(NullNode.getInstance());
                    } else {
                        loadConfig();
                    }
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }
}
