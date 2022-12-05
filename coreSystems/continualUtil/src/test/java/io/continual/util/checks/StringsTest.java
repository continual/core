package io.continual.util.checks;

import org.junit.Test;

public class StringsTest {
	
	@Test(expected = IllegalArgumentException.class)
    public void testThrowOnEmpty(){
            Strings.throwIfEmpty ( "", "string is empty" );
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void testThrowOnEmptyNull(){
            Strings.throwIfEmpty ( null, "string is empty" );
    }
	
	@Test(expected = org.junit.Test.None.class)
    public void testThrowOnEmptyNotNull(){
            Strings.throwIfEmpty ( "data", "string is empty" );
    }
}
