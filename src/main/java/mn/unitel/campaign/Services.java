package mn.unitel.campaign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import mn.unitel.campaign.jooq.tables.records.MainSegmentRecord;
import org.jboss.logging.Logger;
import org.jooq.DSLContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static mn.unitel.campaign.jooq.Tables.MAIN_SEGMENT;

@ApplicationScoped
public class Services {
    @Inject
    DSLContext dsl;

    @Inject
    AddonConfigService addonConfigService;
    @Inject
    Logger logger;

    public Response getNewYearAddonList(String msisdn) {
        JsonNode config = addonConfigService.getConfig();
        if (config.isMissingNode() || config.isNull()) {
            return Response.ok(new CustomResponse<>("Fail", "Config not loaded", null)).build();
        }

        String segment = getSegment(msisdn);
        if (segment == null || segment.isBlank() || segment.equals("NOT_FOUND")) {
            return Response.ok(new CustomResponse<>("Fail", "No segment found", null)).build();
        }

        String activeOffer = getActiveOffer(config);
        if (activeOffer == null) {
            return Response.ok(new CustomResponse<>("Fail", "No active offer period", null)).build();
        }

        JsonNode segmentsNode = config.path("segments").path(segment);
        if (segmentsNode.isMissingNode() || !segmentsNode.isArray()) {
            return Response.ok(new CustomResponse<>("Fail", "No config for segment " + segment, null)).build();
        }

        ObjectMapper mapper = addonConfigService.getMapper();
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

    private String getActiveOffer(JsonNode config) {
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
        MainSegmentRecord record = dsl.selectFrom(MAIN_SEGMENT)
                .where(MAIN_SEGMENT.PHONE_NO.eq(msisdn))
                .fetchOne();
        if (record != null) {
            logger.infof("Found segment %s for msisdn %s", record.getSegment(), msisdn);
            return record.getSegment();
        } else {
            logger.infof("No segment found for msisdn %s from table. Returning default (2_6) segment", msisdn);
            return "2_6";
        }
    }
}
