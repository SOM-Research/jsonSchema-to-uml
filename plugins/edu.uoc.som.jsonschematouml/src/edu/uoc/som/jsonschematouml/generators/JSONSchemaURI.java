package edu.uoc.som.jsonschematouml.generators;

/**
 * Utility class to deal with URIs in JSON Schemas. The class includes some helper
 * methods to ease the resolution and comparison of URIs.
 * 
 * The behavior of the helpers methods of this class follows the indications described in 
 * RFC 3986 ("Uniform Resource Identitier (URI): Generic Syntax")
 * 
 */
public class JSONSchemaURI {
	/**
	 * Holds the full URI string
	 */
	private String URIString;
	/**
	 * The scheme of the URI (cf. Section 3.1 - RFC 3986) 
	 */
	private String scheme;
	/**
	 * The authority of the URI (cf. Section 3.2 - RFC 3986) 
	 */
	private String authority;
	/**
	 * The path of the URI (cf. Section 3.3 - RFC 3986) 
	 */
	private String path;
	/**
	 * The query of the URI (cf. Section 3.4 - RFC 3986) 
	 */
	private String query;
	/**
	 * The fragment of the URI (cf. Section 3.5 - RFC 3986) 
	 */
	private String fragment;
	
	
	public JSONSchemaURI(String URIString) {
		this.URIString = URIString;
		parseURIString();
	}
	
	/**
	 * Parses the URIString to obtain the main element of the URI
	 */
	private void parseURIString() {
		/* Extracting the scheme */
		int index = this.URIString.indexOf(":");
		if(index < 0) 
			throw new JSONSchemaToUMLException("The URI must contain a scheme");
		this.scheme = this.URIString.substring(0, index);
		
		/* Extracting the authority */
		int startIndex = this.URIString.indexOf("//");
		if(startIndex < 0)
			throw new JSONSchemaToUMLException("The URI must contain an authority");
		int endIndex = this.URIString.substring(startIndex+2).indexOf("/"); 
		this.authority = this.URIString.substring(startIndex+2, startIndex+2+endIndex);
		
		/* Extracting the path */
		startIndex = startIndex+2+endIndex;
		endIndex = this.URIString.lastIndexOf("/");
		endIndex = this.URIString.indexOf("?");
		if(endIndex < 0 && this.URIString.indexOf("#") < 0) {
			/* No query or fragment part */
			this.path = this.URIString.substring(startIndex, this.URIString.length());
			return;
		} else if(endIndex < 0 && this.URIString.indexOf("#") > 0) {
			/* No query part but fragment included */
			endIndex = this.URIString.indexOf("#");
		}
		this.path = this.URIString.substring(startIndex, endIndex);
			
		/* Extracting the query and fragment (if any) */
		startIndex = endIndex;
		endIndex = this.URIString.indexOf("#");
		if(endIndex < 0) {
			/* Only query part */
			this.query = this.URIString.substring(startIndex+1, this.URIString.length());
			return;
		} else if (this.URIString.indexOf("?") < 0) {
			/* Only fragment part */
			this.fragment = this.URIString.substring(endIndex+1, this.URIString.length());
		} else {
			/* Both query and fragment */
			this.query = this.URIString.substring(startIndex+1, endIndex);
			this.fragment = this.URIString.substring(endIndex+1, this.URIString.length());
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Scheme    : " + this.scheme + "\n");
		sb.append("Authority : " + this.authority + "\n");
		sb.append("Path      : " + this.path + "\n");
		sb.append("Query     : " + this.query + "\n");
		sb.append("Fragment  : " + this.fragment + "\n");
		return sb.toString();
	}
	
	/**
	 * Digest the name of an object given an URI. It uses the last element of the path
	 * of the URI (and removes the extension, if any).
	 * <br>
	 * For instance:
	 *   foo://example.com:8042/over/there.json
	 * <br>
	 * returns:
	 *   there
	 * 
	 * @return String with the name digested
	 */
	public String digestName() {
		String[] splitPath = this.path.split("/");
		String lastPathElement = splitPath[splitPath.length-1];
		int index = lastPathElement.lastIndexOf(".");
		if(index > 0) {
			return lastPathElement.substring(0, index);
		}
		return lastPathElement;
	}
	
	/**
	 * Digest the name of an schema in an URI. It uses the previous to last 
	 * element of the path of the URI.
	 * <br>
	 * For instance:
	 *   foo://example.com:8042/over/there.json
	 * <br>
	 * returns:
	 *   over
	 * 
	 * @return String with the name digested
	 */
	public String digestIdName() {
		String[] splitPath = this.path.split("/");
		if(splitPath.length > 1) {
			return splitPath[splitPath.length-2]; 
		} else {
			return splitPath[splitPath.length-1]; 
		}
	}
	
	/**
	 * Digest the name of a fragment. It iuses the last element of the
	 * fragment
	 * <br>
	 * For instance:
	 * <br>
	 *   1. foo://example.com:8042/over/there.json#/fragment/name
	 * <br>
	 *   2. foo://example.com:8042/over/there.json#/fragment
	 * <br>
	 * returns:
	 * <br>
	 *   1. name
	 * <br>
	 *   2. fragment
	 * 
	 * @return
	 */
	public String digestFragmentName() {
		String[] splitFragment = this.fragment.split("/");
		return splitFragment[splitFragment.length-1];
	}

	/* Generated */
	public String getURIString() {
		return URIString;
	}

	public String getScheme() {
		return scheme;
	}

	public String getAuthority() {
		return authority;
	}

	public String getPath() {
		return path;
	}

	public String getQuery() {
		return query;
	}

	public String getFragment() {
		return fragment;
	}
	
	
	
	
}
