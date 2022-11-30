package io.continual.util.naming;

import org.junit.Assert;
import org.junit.Test;

public class NameTest {
	
	@Test
    public void staticInit(){
		Name name = Name.fromString("arzu");
		Assert.assertNotNull(name);
		Assert.assertEquals("arzu", name.toString());
    }
	
	@Test
    public void init(){
		Name name = new Name("arzu");
		Assert.assertNotNull(name);
		Assert.assertEquals("arzu", name.toString());
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void initException(){
		new Name("ar/zu");
    }
	
	
	@Test
    public void compare(){
		Name name1 = new Name("arzu");
		Name name2 = new Name("arzu");
		Assert.assertEquals(0, name1.compareTo(name2));
    }
	
	@Test
    public void matches(){
		Name name = new Name("arzu");
		Assert.assertTrue(name.matches("[a-z]*"));
    }

}
