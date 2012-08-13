package info.huggard.charlie.ews;

import info.huggard.charlie.ews.Configuration.Section;
import info.huggard.charlie.ews.util.EWSUtil;

/**
 * A cleanup method indicates one cleanup operation that we wish to perform. .
 * @author Charlie Huggard
 */
public interface CleanupMethod {

    void setConfig(Section config);

    void execute(EWSUtil util) throws Exception;
}
