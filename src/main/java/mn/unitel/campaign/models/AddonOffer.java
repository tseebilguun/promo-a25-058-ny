package mn.unitel.campaign.models;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class AddonOffer {
    public String id;
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate start;
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate end;
}
