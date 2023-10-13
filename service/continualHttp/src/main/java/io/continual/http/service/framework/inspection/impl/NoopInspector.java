package io.continual.http.service.framework.inspection.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import io.continual.http.service.framework.inspection.CHttpObserver;

public class NoopInspector implements CHttpObserver
{
	@Override
	public CHttpObserver method ( String method ) { return this; }

	@Override
	public CHttpObserver onUrl ( String url ) { return this; }

	@Override
	public CHttpObserver queryString ( String qs ) { return this; }

	@Override
	public CHttpObserver contentTypeRequest ( String type ) { return this; }

	@Override
	public CHttpObserver contentLengthRequest ( int length ) { return this; }

	@Override
	public InputStream wrap ( InputStream inputStream ) { return inputStream; }

	@Override
	public CHttpObserver withHeaders ( HeaderLister hl ) { return this; }

	@Override
	public void closeTrx () {}

	@Override
	public CHttpObserver replyWith ( int status, String msg ) { return this; }

	@Override
	public CHttpObserver replyWith ( int code ) { return this; }

	@Override
	public CHttpObserver replyHeader ( String string, String mimeType )  { return this; }

	@Override
	public PrintWriter wrap ( PrintWriter writer ) { return writer; }

	@Override
	public OutputStream wrap ( OutputStream outputStream ) { return outputStream; }
}
