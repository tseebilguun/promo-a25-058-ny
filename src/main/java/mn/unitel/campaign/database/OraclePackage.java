package mn.unitel.campaign.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OraclePackage {

    @Inject
    StoredProcedure storedProcedure;

    private String packageName;

    public OraclePackage getPackage(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public StoredProcedure getStoredProcedure(String procedureName) {
        storedProcedure.setPackageName(this.packageName);
        storedProcedure.setProcedureName(procedureName);
        return storedProcedure;
    }
}