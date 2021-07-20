package io.continual.http.service.framework.inspection;

import io.continual.http.service.framework.context.CHttpRequestContext;

public interface CHttpObserverMgr
{
	void consider ( CHttpRequestContext ctx );
}
