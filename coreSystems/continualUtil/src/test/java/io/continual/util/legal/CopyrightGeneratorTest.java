package io.continual.util.legal;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class CopyrightGeneratorTest {
	
	@Test
    public void generateNotice(){		
		CopyrightGenerator standartNotice = CopyrightGenerator.getStandardNotice();
		Assert.assertNotNull(standartNotice);
	}
	
	@Test
    public void addHolder(){		
		CopyrightGenerator copyright = CopyrightGenerator
				.getStandardNotice()
				.addHolder("hold", 1984);	
		Assert.assertNotNull(copyright);
	}
	
	@Test
    public void getCopyrightNotices(){		
		CopyrightGenerator copyright = CopyrightGenerator
				.getStandardNotice()
				.addHolder("hold", 1984);
		Assert.assertNotNull(copyright.getCopyrightNotices());
		Assert.assertFalse(copyright.getCopyrightNotices().isEmpty());
		Assert.assertTrue(copyright.getCopyrightNotices().size() == 2);
		
		Assert.assertNotNull(copyright.getCopyrightNotices().get(0));
		Assert.assertEquals("(c) 1984-" + thisYear() + ", hold", copyright.getCopyrightNotices().get(0));
		
		Assert.assertNotNull(copyright.getCopyrightNotices().get(1));
		Assert.assertEquals("(c) 2004-" + thisYear() + ", Continual.io Corporation", copyright.getCopyrightNotices().get(1));
	}
	
	@Test
    public void getCopyrightNoticesStatic(){		
		String copyright = CopyrightGenerator.getCopyrightNotice();
		Assert.assertNotNull(copyright);
		Assert.assertFalse(copyright.isEmpty());
		Assert.assertEquals("(c) 2004-" + thisYear() + ", Continual.io Corporation", copyright);
	}
	
	@Test
    public void getCopyrightNoticesStaticWithParam(){		
		String copyright = CopyrightGenerator.getCopyrightNotice("hold", 1984);
		Assert.assertNotNull(copyright);
		Assert.assertFalse(copyright.isEmpty());
		Assert.assertEquals("(c) 1984-" + thisYear() + ", hold", copyright);
	}
	
	@Test
    public void getYearRange(){		
		String copyright = CopyrightGenerator.getYearRange(1984, 1985);
		Assert.assertNotNull(copyright);
		Assert.assertFalse(copyright.isEmpty());
		Assert.assertEquals("1984-1985", copyright);
	}
	
	@Test
    public void getYearRangeEquals(){		
		String copyright = CopyrightGenerator.getYearRange(1984, 1984);
		Assert.assertNotNull(copyright);
		Assert.assertFalse(copyright.isEmpty());
		Assert.assertEquals("1984", copyright);
	}
	
	private static String thisYear ()
	{
		return new SimpleDateFormat ( "YYYY" ).format ( new Date () );
	}
}
