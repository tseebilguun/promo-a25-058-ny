package mn.unitel.campaign.models;

public class AddonConfig {
    public int batch;
    public int price;
    public int days;
    public OfferDetail offer1;
    public OfferDetail offer2;
    public OfferDetail offer3;

    public OfferDetail getOffer(String id) {
        return switch (id) {
            case "offer1" -> offer1;
            case "offer2" -> offer2;
            case "offer3" -> offer3;
            default -> null;
        };
    }
}
