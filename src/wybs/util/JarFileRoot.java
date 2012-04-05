// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wybs.util;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import wybs.lang.Content;
import wybs.lang.Content.Type;
import wybs.lang.Path;

/**
 * Provides an implementation of <code>Path.Root</code> for representing the
 * contents of a jar file.
 * 
 * @author David J. Pearce
 * 
 */
public final class JarFileRoot extends AbstractRoot implements Path.Root {	
	private final String dir;
	private Path.Item[] contents;
	
	public JarFileRoot(String dir, Content.Registry contentTypes) throws IOException {
		super(contentTypes);
		this.dir = dir;		
	}
	
	@Override
	public <T> Path.Entry<T> create(Path.ID id, Content.Type<T> ct,Path.Entry<?>... sources) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void flush() {
		// no-op, since jar files are read-only.
	}

	@Override
	public void refresh() throws IOException {
		JarFile jf = new JarFile(dir);
		Enumeration<JarEntry> entries = jf.entries();
		contents = new Path.Entry[jf.size()];
		int i = 0;
		while (entries.hasMoreElements()) {
			JarEntry e = entries.nextElement();	
			String filename = e.getName();					
			int lastSlash = filename.lastIndexOf('/');
			int lastDot = filename.lastIndexOf('.');			
			Trie pkg = Trie.fromString(filename.substring(0, lastSlash));			
			String name = lastDot >= 0 ? filename.substring(lastSlash + 1, lastDot) : filename;
			String suffix = lastDot >= 0 ? filename.substring(lastDot + 1) : null;						
			Trie id = pkg.append(name);
			Entry pe = new Entry(id, jf, e);
			contentTypes.associate(pe);
			contents[i++] = pe;
		}		
	}
	
	@Override
	protected Folder root() {
		return new Folder(Trie.ROOT);
	}
	
	public String toString() {
		return dir;
	}
	
	
	/**
	 * Represents a directory on a physical file system.
	 * 
	 * @author David J. Pearce
	 *
	 */
	public final class Folder extends AbstractFolder {
		public Folder(Path.ID id) {
			super(id);
		}

		@Override
		protected Path.Item[] contents() throws IOException {		
			// This algorithm is straightforward. I use a two loops instead of a
			// single loop with ArrayList to avoid allocating on the heap. 
			int count = 0 ;
			for(int i=0;i!=contents.length;++i) {
				Path.Item item = contents[i];
				if(item.id() == id) {
					count++;
				}
			}
			
			Path.Item[] myContents = new Path.Item[count];
			count=0;
			for(int i=0;i!=contents.length;++i) {
				Path.Item item = contents[i];
				if(item.id() == id) {
					myContents[count++] = item;
				}
			}
			
			return myContents;
		}

		@Override
		public <T> wybs.lang.Path.Entry<T> create(Path.ID id, Type<T> ct, Path.Entry<?>... sources) {
			throw new UnsupportedOperationException();
		}
	}
	
	private static final class Entry<T> extends AbstractEntry<T> implements Path.Entry<T> {		
		private final JarFile parent;
		private final JarEntry entry;

		public Entry(Trie mid, JarFile parent, JarEntry entry) {
			super(mid);
			this.parent = parent;
			this.entry = entry;
		}

		public String location() {
			return parent.getName();
		}
		
		public long lastModified() {
			return entry.getTime();
		}
		
		public boolean isModified() {
			// cannot modify something in a Jar file.
			return false;
		}
		
		public void touch() {
			throw new UnsupportedOperationException();
		}
		
		public String suffix() {
			String suffix = "";
			String filename = entry.getName();
			int pos = filename.lastIndexOf('.');
			if (pos > 0) {
				suffix = filename.substring(pos + 1);
			}
			return suffix;
		}
		
		public InputStream inputStream() throws IOException {
			return parent.getInputStream(entry);
		}

		public OutputStream outputStream() throws IOException {
			throw new UnsupportedOperationException();
		}

		public void write(T contents) {
			throw new UnsupportedOperationException();
		}		
	}
}
