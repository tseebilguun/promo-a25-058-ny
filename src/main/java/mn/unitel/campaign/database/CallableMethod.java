package mn.unitel.campaign.database;

import java.util.Map;

/**
 * @author  guygmunkh.l
 * @version 1.0
 * @since   2021-06-22
 */
public interface CallableMethod {
    boolean onRecord(Map<String, Object> row, Integer idx);
}