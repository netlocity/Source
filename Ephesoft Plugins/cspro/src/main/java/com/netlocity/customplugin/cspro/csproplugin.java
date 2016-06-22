package com.netlocity.customplugin.cspro;

import com.ephesoft.dcma.core.DCMAException;

import com.ephesoft.dcma.da.id.BatchInstanceID;

public interface csproplugin {
	void execute(final BatchInstanceID batchInstanceID, final String pluginWorkflow) throws DCMAException;
}
