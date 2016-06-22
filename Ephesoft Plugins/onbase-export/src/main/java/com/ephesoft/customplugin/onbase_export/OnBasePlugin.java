package com.ephesoft.customplugin.onbase_export;
import com.ephesoft.dcma.core.DCMAException;
import com.ephesoft.dcma.da.id.BatchInstanceID;

public interface OnBasePlugin {
	void execute(final BatchInstanceID batchInstanceID, final String pluginWorkflow) throws DCMAException;
}
