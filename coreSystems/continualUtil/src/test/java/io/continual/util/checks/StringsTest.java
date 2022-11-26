package io.continual.util.checks;

import junit.framework.TestCase;

import org.junit.Test;

public class StringsTest extends TestCase
{
    @Test
    public void testThrowOnEmpty ()
    {
        try
        {
            Strings.throwIfEmpty ( "", "string is empty" );
            fail ( "Empty string, should have thrown." );
        }
        catch ( IllegalArgumentException x )
        {
            // expected
        }
    }
}
