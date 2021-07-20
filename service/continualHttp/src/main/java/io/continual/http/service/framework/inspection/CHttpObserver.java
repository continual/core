package io.continual.http.service.framework.inspection;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

public interface CHttpObserver
{
	CHttpObserver method ( String method );

	CHttpObserver onUrl ( String url );

	CHttpObserver queryString ( String qs );

	CHttpObserver contentTypeRequest ( String type );

	CHttpObserver contentLengthRequest ( int length );

	InputStream wrap ( ServletInputStream inputStream );

	interface HeaderLister
	{
		Map<String,List<String>> getHeaders ();
	}
	CHttpObserver withHeaders ( HeaderLister hl );
	
	void closeTrx ();

	CHttpObserver replyWith ( int status, String msg );

	CHttpObserver replyWith ( int code );

	CHttpObserver replyHeader ( String string, String mimeType );

	PrintWriter wrap ( PrintWriter writer );

	OutputStream wrap ( ServletOutputStream outputStream );
}
