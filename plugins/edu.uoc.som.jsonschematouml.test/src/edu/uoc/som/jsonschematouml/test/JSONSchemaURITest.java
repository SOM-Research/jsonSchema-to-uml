package edu.uoc.som.jsonschematouml.test;

import org.junit.Test;

import edu.uoc.som.jsonschematouml.generators.JSONSchemaURI;
import junit.framework.TestCase;

public class JSONSchemaURITest extends TestCase {
	
	@Test
    public void testA() {
		String URI = "foo://example.com:8042/over/there?name=ferret#nose";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("foo", jsu.getScheme());
		assertEquals("example.com:8042", jsu.getAuthority());
		assertEquals("/over/there", jsu.getPath());
		assertEquals("name=ferret", jsu.getQuery());
		assertEquals("nose", jsu.getFragment());
    }
	
	@Test
    public void testB() {
		String URI = "foo://example.com:8042/over/there?name=ferret";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("foo", jsu.getScheme());
		assertEquals("example.com:8042", jsu.getAuthority());
		assertEquals("/over/there", jsu.getPath());
		assertEquals("name=ferret", jsu.getQuery());
		assertNull(jsu.getFragment());
    }
	
	@Test
    public void testC() {
		String URI = "foo://example.com:8042/over/there#nose";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("foo", jsu.getScheme());
		assertEquals("example.com:8042", jsu.getAuthority());
		assertEquals("/over/there", jsu.getPath());
		assertNull(jsu.getQuery());
		assertEquals("nose", jsu.getFragment());
    }

	@Test
    public void testD() {
		String URI = "foo://example.com:8042/over";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("foo", jsu.getScheme());
		assertEquals("example.com:8042", jsu.getAuthority());
		assertEquals("/over", jsu.getPath());
		assertNull(jsu.getQuery());
		assertNull(jsu.getFragment());
    }

	@Test
    public void testE() {
		String URI = "foo://example.com:8042/over/there";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("foo", jsu.getScheme());
		assertEquals("example.com:8042", jsu.getAuthority());
		assertEquals("/over/there", jsu.getPath());
		assertNull(jsu.getQuery());
		assertNull(jsu.getFragment());
    }

	@Test
	public void testDigestName() {
		String URI = "foo://example.com:8042/over/there.json";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("there", jsu.digestName());
		
		URI = "foo://example.com:8042/there.json";
		jsu = new JSONSchemaURI(URI);
		assertEquals("there", jsu.digestName());
	}

	@Test
	public void testDigestIdName() {
		String URI = "foo://example.com:8042/over/there.json";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("over", jsu.digestIdName());		
	}

	@Test
	public void testDigestFragment() {
		String URI = "foo://example.com:8042/over/there.json#/fragment/name";
		JSONSchemaURI jsu = new JSONSchemaURI(URI);
		assertEquals("name", jsu.digestFragmentName());
		
		URI = "foo://example.com:8042/over/there.json#/fragment";
		jsu = new JSONSchemaURI(URI);
		assertEquals("fragment", jsu.digestFragmentName());
	}
}
