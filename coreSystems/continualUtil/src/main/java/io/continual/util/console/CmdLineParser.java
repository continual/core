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
package io.continual.util.console;

import java.util.HashMap;
import java.util.TreeSet;

import io.continual.util.console.ConsoleProgram.UsageException;
import io.continual.util.data.TypeConvertor;

/**
 * Assists in reading command line settings.
 */
public class CmdLineParser
{
	public CmdLineParser ()
	{
		fSingleToWord = new HashMap<Character,String> ();
		fWordsNeedingValues = new TreeSet<String> ();
		fOptions = new HashMap<String,Option> ();
		fMinFiles = 0;
		fMaxFiles = Integer.MAX_VALUE;
	}

	/**
	 * Register a boolean option. 
	 * @param word e.g. "force"
	 * @param singleChar e.g. "f". 
	 * @param defValue the default value for this option
	 * @return this command line parser
	 */
	public CmdLineParser registerOnOffOption ( String word, Character singleChar, boolean defValue )
	{
		if ( word == null )
		{
			throw new IllegalArgumentException ( "An option 'word' is required." );
		}

		if ( singleChar != null )
		{
			fSingleToWord.put ( singleChar, word );
		}

		fOptions.put ( word, new OnOff ( defValue ) );

		return this;
	}

	public CmdLineParser registerOptionWithValue ( String word )
	{
		return registerOptionWithValue ( word, null, null, null );
	}

	public CmdLineParser registerOptionWithValue ( String word, String singleChar )
	{
		return registerOptionWithValue ( word, singleChar, null, null );
	}

	/**
	 * register an option that takes a value
	 * @param word the full word for this option, e.g. "verbose"
	 * @param singleChar a single char representation of this option, e.g. "v". Can be null.
	 * @param defValue the default value for the option if none is provided
	 * @param allowed if not null, a limited range of values for the option
	 * @return this command line parser
	 */
	public CmdLineParser registerOptionWithValue ( String word, String singleChar, String defValue, String[] allowed )
	{
		if ( word == null )
		{
			throw new IllegalArgumentException ( "An option 'word' is required." );
		}

		if ( singleChar != null )
		{
			if ( singleChar.length () > 1 )
			{
				throw new IllegalArgumentException ( singleChar + " is not a single character." );
			}
			fSingleToWord.put ( singleChar.charAt ( 0 ), word );
		}

		fWordsNeedingValues.add ( word );
		fOptions.put ( word, new Setting ( word, defValue, allowed ) );

		return this;
	}

	/**
	 * allows no file arguments
	 * @return this command line parser
	 */
	public CmdLineParser requireNoFileArguments ()
	{
		return requireFileArguments ( 0, 0 );
	}

	/**
	 * allows exactly one file argument
	 * @return this command line parser
	 */
	public CmdLineParser requireOneFileArgument ()
	{
		return requireFileArguments ( 1, 1 );
	}

	/**
	 * sets the range for required file args from the given min to no max
	 * @param min the minimum number of file arguments
	 * @return this command line parser
	 */
	public CmdLineParser requireMinFileArguments ( int min )
	{
		return requireFileArguments ( min, Integer.MAX_VALUE );
	}

	/**
	 * require a specific number of file arguments
	 * @param exactly the exact number of file arguments
	 * @return this command line parser
	 */
	public CmdLineParser requireFileArguments ( int exactly )
	{
		return requireFileArguments ( exactly, exactly );
	}

	/**
	 * set a range for file arg count for the parser 
	 * @param min 0 or higher
	 * @param max 0 or higher, use Integer.MAX_VALUE for no max
	 * @return this command line parser
	 */
	public CmdLineParser requireFileArguments ( int min, int max )
	{
		fMinFiles = min;
		fMaxFiles = max;
		return this;
	}
	
	/**
	 * find out if an option has a value (or is just on/off)
	 * @param optionWord the option word
	 * @return the value for the option
	 */
	public boolean hasArg ( String optionWord )
	{
		return ( fOptions.get ( optionWord ) != null );
	}

	/**
	 * find out if a boolean option has been set
	 * @param optionWord the option word
	 * @return true if the value for the option is set
	 */
	public boolean isSet ( String optionWord )
	{
		final Option o = fOptions.get ( optionWord );
		return ( o != null ) ? TypeConvertor.convertToBoolean (o.getDefault()) : false;
	}

	/**
	 * get the default value for a given option
	 * @param optionWord the option word
	 * @return the default value for the option
	 */
	public String getArgFor ( String optionWord )
	{
		final Option o = fOptions.get ( optionWord );
		return ( o != null ) ? o.getDefault () : "";
	}

	/**
	 * reads command line arguments
	 * @param args the command line arguments
	 * @return this command line parser
	 * @throws UsageException if the arguments don't meet the requirements
	 */
	public CmdLinePrefs processArgs ( String[] args ) throws UsageException
	{
		final CmdLinePrefs prefs = new CmdLinePrefs ( this );

		boolean seenDashDash = false;
		int i=0;
		for ( ; i<args.length && !seenDashDash; i++ )
		{
			final String item = args[i];
			if ( item.equals ( "--" ) )
			{
				seenDashDash = true;
			}
			else if ( item.startsWith ( "--" ) )
			{
				// a word, which could take an argument
				final String word = item.substring ( 2 );
				if ( reqsValue ( word ) )
				{
					if ( i+1 == args.length )
					{
						throw new UsageException ( "Option " + item + " requires an argument." );
					}
					else
					{
						handleOption ( prefs, word, args[i+1] );
						i++;	// forward one
					}
				}
				else
				{
					handleOption ( prefs, word );
				}
			}
			else if ( item.startsWith ( "-" ) )
			{
				final int len = item.length ();
				if ( len == 1 )
				{
					throw new UsageException ( "Can't process '-' alone." );
				}
				else if ( item.length () == 2 )
				{
					// an option, which could take an argument
					char c = item.charAt ( 1 );
					final String word = fSingleToWord.get ( c );
					if ( word == null )
					{
						throw new UsageException ( "Option '" + c + "' is invalid." );
					}
					if ( reqsValue ( word ) )
					{
						if ( i==args.length )
						{
							throw new UsageException ( "Option " + item + " requires an argument." );
						}
						else
						{
							handleOption ( prefs, word, args[i+1] );
							i++;	// forward one
						}
					}
					else
					{
						handleOption ( prefs, word );
					}
				}
				else
				{
					// an option set...
					for ( char c : item.substring ( 1 ).toCharArray () )
					{
						final String optWord = fSingleToWord.get ( c );
						if ( optWord == null )
						{
							throw new UsageException ( "Option '" + c + "' is invalid." );
						}
						handleOption ( prefs, optWord );
					}
				}
			}
			else
			{
				// a plain word (not an option)
				seenDashDash = true;
				break;
			}
		}

		int leftoverCount = 0;
		while ( i<args.length )
		{
			prefs.addLeftover ( args[i] );
			i++;
			leftoverCount++;
		}

		if ( leftoverCount < fMinFiles || leftoverCount > fMaxFiles )
		{
			throw new UsageException ( getErrorMsgForWrongCount ( leftoverCount ) );
		}
		
		return prefs;
	}

	private static String plural ( String word, int count )
	{
		return ( count == 1 ? word : word + "s" );
	}

	private String getErrorMsgForWrongCount ( int count )
	{
		if ( fMinFiles == fMaxFiles )
		{
			if ( fMinFiles == 0 )
			{
				return "You may not provide any arguments.";
			}
			else
			{
				return "You must provide " + fMinFiles + " " + plural("argument",fMinFiles) + ".";
			}
		}
		else
		{
			final String minPart = ( fMinFiles <= 0 ? null : "at least " + fMinFiles + " " + plural("argument",fMinFiles) );
			final String maxPart = ( fMaxFiles == Integer.MAX_VALUE ? null : "at most " + fMaxFiles + " " + plural("argument",fMaxFiles) );

			return "You must provide " +
				( minPart != null ? minPart : "" ) +
				( minPart != null && maxPart != null ? " and " : "" ) +
				( maxPart != null ? maxPart : "" ) +
				".";
		}
	}

	private boolean reqsValue ( String optWord )
	{
		return fWordsNeedingValues.contains ( optWord );
	}

	private void handleOption ( CmdLinePrefs prefs, String optWord ) throws UsageException
	{
		handleOption ( prefs, optWord, Boolean.TRUE.toString () );
	}

	private void handleOption ( CmdLinePrefs prefs, String optWord, String value ) throws UsageException
	{
		final Option o = fOptions.get ( optWord );
		if ( o == null )
		{
			throw new UsageException ( "Unrecognized option \"" + optWord + "\"" );
		}

		final String valToUse = o.checkValue ( value );
		prefs.set ( optWord, valToUse );
	}

	private interface Option
	{
		String checkValue ( String val ) throws UsageException;
		String getDefault ();
	}

	private class OnOff implements Option
	{
		public OnOff ( boolean defVal )
		{
			fValue = defVal;
		}

		@Override
		public String checkValue ( String val )
		{
			return "" + TypeConvertor.convertToBooleanBroad ( val );
		}

		@Override
		public String getDefault ()
		{
			return Boolean.toString ( fValue );
		}

		private final boolean fValue;
	}

	private class Setting implements Option
	{
		public Setting ( String name, String defVal, String[] allowed )
		{
			fSetting = name;
			fValue = defVal;
			fAllowed = allowed;
		}
		
		@Override
		public String checkValue ( String val ) throws UsageException
		{
			boolean canSet = true;
			if ( fAllowed != null )
			{
				canSet = false;
				for ( String a : fAllowed )
				{
					if ( a.equals ( val ) )
					{
						canSet = true;
						break;
					}
				}
				if ( !canSet )
				{
					throw new UsageException ( "Value " + val + " is not allowed for setting " + fSetting );
				}
			}
			return val;
		}

		@Override
		public String getDefault ()
		{
			return fValue;
		}

		private final String fSetting;
		private String fValue;
		private String[] fAllowed;
	}

	private final HashMap<Character, String> fSingleToWord;
	private final TreeSet<String> fWordsNeedingValues;
	private final HashMap<String, Option> fOptions;
	private int fMinFiles;
	private int fMaxFiles;
}
