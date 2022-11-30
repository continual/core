package io.continual.util.naming;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class PathTest2
{
	
	@Test(expected = IllegalArgumentException.class)
    public void initRelativePath(){
		Path.fromString("dev");
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void initNull(){
		Path.fromString(null);
    }
	
	@Test
    public void initRootFolder(){
		Path path = Path.fromString("/");
		Assert.assertEquals("/", path.toString());
    }
	
	@Test
    public void initWithDoubleSlash(){
		Path path = Path.fromString("//dev");
		Assert.assertEquals("/dev", path.toString());
    }
	
	@Test
    public void initWithDot(){
		Path path = Path.fromString("/./dev");
		Assert.assertEquals("/dev", path.toString());
    }

	@Test
    public void initWithEndingDot(){
		Path path = Path.fromString("/dev/.");
		Assert.assertEquals("/dev", path.toString());
    }
	
	@Test
    public void initWithEndingSlash(){
		Path path = Path.fromString("/dev/");
		Assert.assertEquals("/dev/", path.toString());
    }
	
	@Test
    public void getId(){
		Path path = Path.fromString("/dev");
		Assert.assertEquals("/dev", path.getId());
    }
	
	@Test
    public void compareTo(){
		Path path1 = Path.fromString("/dev");
		Path path2 = Path.fromString("/dev");
		Assert.assertEquals(0, path1.compareTo(path2));
    }

	@Test
    public void isRootPath(){
		Path path = Path.fromString("/");
		Assert.assertTrue(path.isRootPath());
    }
	
	@Test
    public void isRootPathNegative(){
		Path path = Path.fromString("/dev");
		Assert.assertFalse(path.isRootPath());
    }
	
	@Test
    public void getParentPath(){
		Path path = Path.fromString("/dev");
		Assert.assertEquals("/", path.getParentPath().getId());
    }

	@Test
    public void getItemName(){
		Path path = Path.fromString("/dev");
		Assert.assertEquals("dev", path.getItemName().toString());
    }
	
	@Test
    public void startsWith(){
		Path path = Path.fromString("/dev");
		Assert.assertTrue(path.startsWith("/dev"));
    }
	
	@Test
    public void startsWithPath(){
		Path path = Path.fromString("/dev");
		Assert.assertTrue(path.startsWith(Path.fromString("/dev")));
    }
	
	@Test
    public void startsWithPath1(){
		Path path = Path.fromString("/dev");
		Assert.assertTrue(path.startsWith(Path.fromString("/")));
    }
	
	@Test
    public void startsWithPath2(){
		Path path = Path.fromString("/");
		Assert.assertFalse(path.startsWith(Path.fromString("/dev")));
    }
	
	@Test
    public void makeChildItem(){
		Path path = Path.fromString("/dev");
		Name name = Name.fromString("test");
		Path generated = path.makeChildItem(name);
		Assert.assertNotNull(generated);
		Assert.assertEquals(path, generated.getParentPath());
		Assert.assertEquals(name, generated.getItemName());
    }
	
	@Test
    public void makeChildItemFromPath(){
		Path path1 = Path.fromString("/dev");
		Path path2 = Path.fromString("/test");
		Path generated = path1.makeChildPath(path2);
		Assert.assertNotNull(generated);
		Assert.assertEquals("/dev/test", generated.toString());
    }
	
	@Test
    public void makePathWithinParent(){
		Path rootPath = Path.fromString("/dev");
		Assert.assertEquals(rootPath, rootPath.makePathWithinParent(Path.fromString("/")));
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void makePathWithinParent2(){
		Path rootPath = Path.fromString("/dev");
		rootPath.makePathWithinParent(Path.fromString("/test"));
    }
	
	@Test
    public void makePathWithinParent3(){
		Path rootPath = Path.fromString("/test/aaa");
		Path generated = rootPath.makePathWithinParent(Path.fromString("/test"));
		Assert.assertEquals("/aaa", generated.toString());
    }
	
	@Test
    public void makePathWithinParent4(){
		Path rootPath = Path.fromString("/");
		Path generated = rootPath.makePathWithinParent(Path.fromString("/"));
		Assert.assertEquals("/", generated.toString());
    }
	
	@Test
    public void makePathWithinParent5(){
		Path rootPath = Path.fromString("/dev");
		Assert.assertEquals("/", rootPath.makePathWithinParent(Path.fromString("/dev")).toString());
    }
	
	@Test
    public void getSegmentList(){
		Path path = Path.fromString("/");
		List<Name> segments = path.getSegmentList();
		Assert.assertNotNull(segments);
		Assert.assertTrue(segments.isEmpty());
		Assert.assertEquals(0, segments.size());
    }
	
	@Test
    public void getSegmentList2(){
		Path path = Path.fromString("/dev");
		List<Name> segments = path.getSegmentList();
		Assert.assertNotNull(segments);
		Assert.assertFalse(segments.isEmpty());
		Assert.assertEquals(1, segments.size());
		Assert.assertNotNull(segments.get(0));
		Assert.assertEquals("dev", segments.get(0).toString());
    }
	
	@Test
    public void getSegments(){
		Path path = Path.fromString("/dev");
		Name[] segments = path.getSegments();
		Assert.assertNotNull(segments);
		Assert.assertEquals(1, segments.length);
		Assert.assertEquals("dev", segments[0].toString());
    }
	
	@Test
    public void depth(){
		Path path = Path.fromString("/");
		int depth = path.depth();
		Assert.assertEquals(1, depth);
    }
	
	@Test
    public void depth2(){
		Path path = Path.fromString("/dev/test");
		int depth = path.depth();
		Assert.assertEquals(3, depth);
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void buildName(){
		Path.fromString("/..");
    }
	
	@Test
    public void hashCodeTest(){
		Path path = Path.fromString("/");
		Assert.assertEquals(47, path.hashCode());
    }
	
	@Test
    public void equals(){
		Path path = Path.fromString("/dev");
		Assert.assertEquals(false, path.equals(null));
    }
	
	@Test
    public void equals2(){
		Path path = Path.fromString("/dev");
		Assert.assertEquals(false, path.equals(new String("")));
    }
	
}
