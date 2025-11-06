package mn.unitel.campaign.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Setter;
import oracle.jdbc.internal.OracleTypes;
import org.jboss.logging.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@ApplicationScoped
public class StoredProcedure {
    @Inject
    DBPromoObject dbPromoObject;

    @Setter
    String packageName;

    @Setter
    private String procedureName;
    private final ArrayList<Param> params = new ArrayList<>();
    private String paramPlaceholders = "";
    private String paramsString = "";

    Logger logger = Logger.getLogger(StoredProcedure.class);

    private enum ParamDirection {
        IN, OUT, INOUT
    }

    private static class Param {
        public int type;
        public ParamDirection inOut;
        public Object value;
        public String outKey;

        public Param(int type, Object value, ParamDirection inOut, String outKey) {
            this.type = type;
            this.value = value;
            this.inOut = inOut;
            this.outKey = outKey;
        }
    }

    public StoredProcedure() {

    }

    private void addParam(Param param) {
        this.params.add(param);

        if (this.params.size() > 1) {
            this.paramPlaceholders += ",";
            this.paramsString += ",";
        }
        this.paramPlaceholders += "?";
        this.paramsString += param.value;
    }

    public StoredProcedure addParamIn(int type, Object value) {
        Param param = new Param(type, value, ParamDirection.IN, null);

        this.addParam(param);

        return this;
    }

    public StoredProcedure addParamOut(int type, String key) {
        Param param = new Param(type, null, ParamDirection.OUT, key);

        this.addParam(param);

        return this;
    }

    public StoredProcedure addParamInOut(int type, Object value, String key) {
        Param param = new Param(type, value, ParamDirection.INOUT, key);

        this.addParam(param);

        return this;
    }

    private void clearParams() {
        this.params.clear();
        this.paramPlaceholders = "";
        this.paramsString = "";
    }

    public Map<String, Object> call() {
        return this.call((row, idx) -> false);
    }

    public Map<String, Object> call(CallableMethod callableMethod) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            if (this.procedureName == null) {
                throw new NullPointerException("Procedure name is not provided");
            }
            logger.info("call: " + this.packageName + "." + this.procedureName + "(" + paramsString + ")");

            Connection conn = dbPromoObject.getConnection();
            String sql = "{ CALL " + this.packageName + "." + this.procedureName + "(" + this.paramPlaceholders + ") }";
            CallableStatement stmt = conn.prepareCall(sql);

            try {
                int i = 0;
                for (Param param : this.params) {
                    i++;
                    switch (param.inOut) {
                        case IN:
                            stmt.setObject(i, param.value, param.type);
                            break;
                        case OUT:
                            stmt.registerOutParameter(i, param.type);
                            break;
                        case INOUT:
                            stmt.setObject(i, param.value, param.type);
                            stmt.registerOutParameter(i, param.type);
                            break;
                    }
                }

                stmt.execute();

                i = 0;
                for (Param param : this.params) {
                    i++;
                    if (param.inOut == ParamDirection.OUT || param.inOut == ParamDirection.INOUT) {
                        if (param.type != OracleTypes.CURSOR) {
                            resultMap.put(param.outKey, stmt.getObject(i));
                        } else {
                            ResultSet rs = (ResultSet) stmt.getObject(i);
                            ResultSetMetaData rsMeta = rs.getMetaData();
                            Integer idx = 0;
                            while (rs.next()) {
                                Map<String, Object> row = new HashMap<>();
                                for (int j = 1; j <= rsMeta.getColumnCount(); j++) {
                                    row.put(rsMeta.getColumnLabel(j), rs.getObject(j));
                                }
                                if (!callableMethod.onRecord(row, idx))
                                    break;
                                idx++;
                            }
                            rs.close();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("", e);
            } finally {
                stmt.close();
                conn.close();
            }
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }

        this.clearParams();
        logger.info("result of " + this.packageName + this.procedureName + "(" + paramsString + ")" + ": " + String.join(", ", resultMap.values().stream().map(Object::toString).toArray(String[]::new)));
        return resultMap;
    }
}
