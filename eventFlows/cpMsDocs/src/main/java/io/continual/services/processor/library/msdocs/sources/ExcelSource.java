/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.services.processor.library.msdocs.sources;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.resources.ResourceLoader;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

/**
 * An excel spreadsheet stream source. This source will report EOF when all records are read.
 */
public class ExcelSource extends BasicSource
{
	public ExcelSource ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public ExcelSource ( String defPipeline, String resource, boolean firstLineHeader ) throws BuildFailure
	{
		super ( defPipeline );
		try
		{
			fResource = resource;
			fLineNumberToField = null;
			fFirstLineHeader = firstLineHeader;

			fFieldMap = new HashMap<> ();
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public ExcelSource ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( config );
		try
		{
			fResource = config.optString ( "data", "" );
			fLineNumberToField = config.optString ( "lineNumberTo", null );
			fFirstLineHeader = config.optBoolean ( "firstLineHeader", false );

			fFieldMap = new HashMap<> ();
			JsonVisitor.forEachElement ( config.optJSONObject ( "fieldMap" ), new ObjectVisitor<Object,JSONException> ()
			{
				@Override
				public boolean visit ( String srcField, Object fieldData ) throws JSONException
				{
					if ( fieldData instanceof String )
					{
						fFieldMap.put ( srcField, new FieldInfo ( (String)fieldData ) );
					}
					else if ( fieldData instanceof JSONObject )
					{
						fFieldMap.put ( srcField, new FieldInfo ( (JSONObject)fieldData ) );
					}
					return true;
				}
			} );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void close () throws IOException
	{
		if ( fWorkbook != null ) fWorkbook.close ();
		fWorkbook = null;

		fInited = true;	// not really, but we want isEof() 

		super.close ();
	}

	@Override
	public boolean isEof ()
	{
		return fInited && fWorkbook == null;
	}

	public void setResource ( InputStream data )
	{
		fStream = data;
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException
	{
		// normal processing...
		init ( spc );

		// maybe it was closed?
		if ( isEof() ) return null;

		// get the next record
		if ( fIterator.hasNext () )
		{
			fLineNumber++;
			return buildMessage ( fIterator.next () );
		}

		// out of records, close the worksheet as an EOF signal
		fWorkbook.close ();
		fWorkbook = null;
		log.info ( "ExcelSource sent " + fLineNumber + " records." );

		return null;
	}

	private final String fResource;
	private final boolean fFirstLineHeader;
	private final String fLineNumberToField;
	private final HashMap<String,FieldInfo> fFieldMap;

	private static class FieldInfo
	{
		public FieldInfo ( String srcName )
		{
			fSrcName = srcName;
			fType = null;
		}
		public FieldInfo ( JSONObject data )
		{
			fSrcName = data.optString ( "name", null );
			fType = CellType.valueOf ( data.optString ( "type", CellType._NONE.toString () ).toUpperCase () );
		}
		public String getMappedName ()
		{
			return fSrcName;
		}
		public CellType getCellType ()
		{
			return fType;
		}

		private final String fSrcName;
		private final CellType fType;
	}
	
	private boolean fInited = false;
	private ArrayList<String> fHeaderValues = null;
	private InputStream fStream = null;
	private int fLineNumber = 0;
	private int fRowWidth = -1;

	private Workbook fWorkbook;
	private Sheet fSheet;
	private Iterator<Row> fIterator = null;

	private void init ( StreamProcessingContext spc ) throws IOException
	{
		if ( !fInited )
		{
			fInited = true;

			// locate the stream
			if ( fStream == null )
			{
				final String name = spc.evalExpression ( fResource );
				fStream = ResourceLoader.load ( name );
				if ( fStream == null )
				{
					throw new IOException ( "Unable to load resource: " + name + " (" + fResource + ")" );
				}
			}

			// open the spreadsheet
			fWorkbook = WorkbookFactory.create ( fStream );
			fSheet = fWorkbook.getSheetAt ( 0 );
			fIterator = fSheet.iterator ();

			if ( fFirstLineHeader )
			{
				// read the first line as a header list
				fHeaderValues = new ArrayList<> ();
				if ( !fIterator.hasNext () )
				{
					// no first line, so no headers.
					spc.warn ( "Excel source configured to use header line, but no rows are available." );

					// FIXME: need to decide how to flag this and handle it in data read below
				}
				else
				{
					final Row firstRow = fIterator.next ();
					fRowWidth = firstRow.getLastCellNum ();
					for ( int i=0; i<fRowWidth; i++ )
					{
						final Cell c = firstRow.getCell ( i, MissingCellPolicy.RETURN_BLANK_AS_NULL );
						if ( c == null )
						{
							fHeaderValues.add ( "col_" + i );
						}
						else
						{
							fHeaderValues.add ( c.getStringCellValue () );
						}
					}
				}
			}
		}
	}

	private void cellToValue ( JSONObject target, String fieldName, Cell cell, CellType asType )
	{
		switch ( asType )
		{
			case BOOLEAN:
				target.put ( fieldName, cell.getBooleanCellValue () );
				break;

			case ERROR:
				target.put ( fieldName, "ERROR" );
				break;

			case FORMULA:
				target.put ( fieldName, cell.getCellFormula () );
				break;

			case NUMERIC:
				target.put ( fieldName, cell.getNumericCellValue () );
				break;

			case BLANK:
			case STRING:
			default:
				String val;
				if ( cell.getCellType () != asType )
				{
					final DataFormatter df = new DataFormatter ();
					val = df.formatCellValue ( cell );
				}
				else
				{
					val = cell.getStringCellValue ();
				}
				target.put ( fieldName, val );
				break;
		}
	}

	private void cellToValue ( JSONObject target, String fieldName, Cell cell, FieldInfo fi )
	{
		if ( cell == null )
		{
			target.put ( fieldName, "" );
			return;
		}

		CellType type = fi == null ? null : fi.getCellType ();
		if ( type == null )
		{
			type = cell.getCellType ();
		}

		switch ( type )
		{
			case BOOLEAN:
			case NUMERIC:
			case STRING:
			case BLANK:
			case ERROR:
				cellToValue ( target, fieldName, cell, type );
				break;

			case FORMULA:
				cellToValue ( target, fieldName, cell, cell.getCachedFormulaResultType () );
				break;

			case _NONE:
			default:
				// do nothing
				break;
		}
	}
	
	private MessageAndRouting buildMessage ( Row currentRow )
	{
		final JSONObject data = new JSONObject ();

		for ( int colNo = 0; colNo < fRowWidth; colNo++ )
		{
			// get the column name
			final String name = fHeaderValues != null ? fHeaderValues.get ( colNo ) : "" + colNo;

			String useName = name;
			final FieldInfo fi = fFieldMap.get ( name );
			if ( fi != null )
			{
				String subName = fi.getMappedName ();
				if ( subName != null )
				{
					useName = subName;
				}
			}

			final Cell cell = currentRow.getCell ( colNo, MissingCellPolicy.RETURN_BLANK_AS_NULL );
			cellToValue ( data, useName, cell, fi );
		}

		// include a line number value if requested
		if ( fLineNumberToField != null )
		{
			data.put ( fLineNumberToField, fLineNumber );
		}

		return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( data ) );
	}

	private static final Logger log = LoggerFactory.getLogger ( ExcelSource.class );
}
