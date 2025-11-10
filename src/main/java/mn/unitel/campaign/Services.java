package mn.unitel.campaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class Services {
    private static final Logger logger = Logger.getLogger(Services.class);

    @Inject
    DSLContext dsl;

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode config = NullNode.getInstance();

    @PostConstruct
    void init() {
        try {
            Path configPath = Paths.get(System.getProperty("user.dir"), "config", "data-addon-config.json");

            if (!Files.exists(configPath)) {
                logger.error("Config file not found: " + configPath.toAbsolutePath());
                return;
            }

            config = mapper.readTree(Files.newInputStream(configPath));
            logger.info("Loaded config from " + configPath.toAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to load data-addon-config.json", e);
            config = NullNode.getInstance();
        }
    }

    public Response getNewYearAddonList(String msisdn) {
        if (config.isMissingNode() || config.isNull()) {
            return Response.ok(new CustomResponse<>("Fail", "Config not loaded", null)).build();
        }

        String segment = getSegment(msisdn);
        if (segment == null || segment.isBlank()) {
            return Response.ok(new CustomResponse<>("Fail", "No segment found", null)).build();
        }

        String activeOffer = getActiveOffer();
        if (activeOffer == null) {
            return Response.ok(new CustomResponse<>("Fail", "No active offer period", null)).build();
        }

        JsonNode segmentsNode = config.path("segments").path(segment);
        if (segmentsNode.isMissingNode() || !segmentsNode.isArray()) {
            return Response.ok(new CustomResponse<>("Fail", "No config for segment " + segment, null)).build();
        }

        List<JsonNode> resultList = new ArrayList<>();
        for (JsonNode entry : segmentsNode) {
            JsonNode offerNode = entry.path(activeOffer);
            if (!offerNode.isMissingNode()) {
                JsonNode resultItem = mapper.createObjectNode()
                        .put("order", entry.path("order").asInt())
                        .put("id", offerNode.path("provisionId").asText())
                        .put("name", offerNode.path("name").asText())
                        .put("duration", entry.path("duration").asText())
                        .put("size", offerNode.path("size").asInt())
                        .put("price", entry.path("price").asInt());
                resultList.add(resultItem);
            }
        }

        return Response.ok(new CustomResponse<>("Success", "", resultList)).build();
    }

    private String getActiveOffer() {
        if (config.isMissingNode() || config.isNull()) return null;
        LocalDate now = LocalDate.now();
        for (JsonNode offer : config.path("offers")) {
            LocalDate start = LocalDate.parse(offer.path("start").asText());
            LocalDate end = LocalDate.parse(offer.path("end").asText());
            if (!now.isBefore(start) && !now.isAfter(end)) {
                return offer.path("id").asText();
            }
        }
        return null;
    }

    public String getSegment(String msisdn) {
        // stub — replace when DB ready
        return "1_1";
    }
}
