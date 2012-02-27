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

package wyil.lang;

import java.util.*;

import wyc.lang.Content;
import wyc.lang.Path;
import wyil.util.*;
import wyjc.io.ClassFileLoader;

public final class WyilFile {

	// =========================================================================
	// Content Type
	// =========================================================================
	
	public static final Content.Type<WyilFile> ContentType = new Content.Type<WyilFile>() {
		public Path.Entry<WyilFile> accept(Path.Entry<?> e) {			
			if (e.contentType() == this) {
				return (Path.Entry<WyilFile>) e;
			} 			
			return null;
		}

		public WyilFile read(Path.Entry entry) throws Exception {		
			
			// FIXME: this shouldn't be using a class file loader :)
			
			ClassFileLoader loader = new ClassFileLoader();
			WyilFile mi = loader.read(entry.id(), entry.inputStream());
			return mi;				
		}
		
		public void write(Path.Entry entry, WyilFile contents) {
			// for now
			throw new UnsupportedOperationException();
		}
		
		public String toString() {
			return "Content-Type: wyil";
		}
	};	

	// =========================================================================
	// State
	// =========================================================================
		
	private final Path.ID mid;
	private final String filename;
	private HashMap<Pair<String,Type.FunctionOrMethodOrMessage>,Method> methods;
	private HashMap<String,TypeDef> types;
	private HashMap<String,ConstDef> constants;
	
	// =========================================================================
	// Constructors
	// =========================================================================

	public WyilFile(Path.ID mid,
			String filename,
			Collection<Method> methods,
			Collection<TypeDef> types,
			Collection<ConstDef> constants) {		
		this.mid = mid;
		this.filename = filename;
		
		// first, init the caches
		this.methods = new HashMap<Pair<String,Type.FunctionOrMethodOrMessage>, Method>();
		this.types = new HashMap<String, TypeDef>();
		this.constants = new HashMap<String, ConstDef>();
		
		// second, build the caches
		for(Method m : methods) {
			Pair<String,Type.FunctionOrMethodOrMessage> p = new Pair<String,Type.FunctionOrMethodOrMessage>(m.name(),m.type());
			Method tmp = this.methods.get(p);
			if (tmp != null) {
				throw new IllegalArgumentException(
						"Multiple function or method definitions with the same name and type not permitted");
			}
			this.methods.put(p,m);
		}
		
		for (TypeDef t : types) {
			TypeDef tmp = this.types.get(t.name());
			if (tmp != null) {
				throw new IllegalArgumentException(
						"Multiple type definitions with the same name not permitted");
			}
			this.types.put(t.name(), t);
		}

		for (ConstDef c : constants) {
			ConstDef tmp = this.constants.get(c.name());
			if (tmp != null) {
				throw new IllegalArgumentException(
						"Multiple constant definitions with the same name not permitted");
			}
			this.constants.put(c.name(), c);
		}
	}
	
	// =========================================================================
	// Accessors
	// =========================================================================
	
	public Path.ID id() {
		return mid;
	}
	
	public String filename() {
		return filename;
	}
	
	public TypeDef type(String name) {
		return types.get(name);
	}
	
	public Collection<WyilFile.TypeDef> types() {
		return types.values();
	}
	
	public ConstDef constant(String name) {
		return constants.get(name);
	}
	
	public Collection<WyilFile.ConstDef> constants() {
		return constants.values();
	}
	
	public List<Method> method(String name) {
		ArrayList<Method> r = new ArrayList<Method>();
		for(Pair<String,Type.FunctionOrMethodOrMessage> p : methods.keySet()) {
			if(p.first().equals(name)) {
				r.add(methods.get(p));
			}
		}
		return r;
	}
	
	public Method method(String name, Type.FunctionOrMethod ft) {
		return methods.get(new Pair<String, Type.FunctionOrMethod>(name, ft));
	}
	
	public Collection<WyilFile.Method> methods() {
		return methods.values();
	}
	
	// =========================================================================
	// Mutators
	// =========================================================================
	
	public void add(WyilFile.Method m) {
		Pair<String,Type.FunctionOrMethodOrMessage> p = new Pair<String,Type.FunctionOrMethodOrMessage>(m.name(),m.type());
		this.methods.put(p,m);
	}
	
	public void add(WyilFile.TypeDef t) {
		this.types.put(t.name(), t);
	}
	
	public void add(WyilFile.ConstDef c) {
		this.constants.put(c.name(), c);
	}
	
	public boolean hasName(String name) {		
		return types.get(name) != null || constants.get(name) != null
				|| method(name).size() > 0;
	}
	
	// =========================================================================
	// Types
	// =========================================================================		
	
	public static final class TypeDef extends SyntacticElement.Impl {
		private List<Modifier> modifiers;
		private String name;
		private Type type;		
		private Block constraint;

		public TypeDef(Collection<Modifier> modifiers, String name, Type type,
				Block constraint, Attribute... attributes) {
			super(attributes);
			this.modifiers = new ArrayList<Modifier>(modifiers);
			this.name = name;
			this.type = type;
			this.constraint = constraint;
		}

		public TypeDef(Collection<Modifier> modifiers, String name, Type type,
				Block constraint, Collection<Attribute> attributes) {
			super(attributes);
			this.modifiers = new ArrayList<Modifier>(modifiers);
			this.name = name;
			this.type = type;
			this.constraint = constraint;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}				
		
		public String name() {
			return name;
		}

		public Type type() {
			return type;
		}
		
		public Block constraint() {
			return constraint;
		}
	}
	
	public static final class ConstDef extends SyntacticElement.Impl {
		private List<Modifier> modifiers;
		private String name;		
		private Value constant;
		
		public ConstDef(Collection<Modifier> modifiers, String name, Value constant,  Attribute... attributes) {
			super(attributes);
			this.modifiers = new ArrayList<Modifier>(modifiers);
			this.name = name;
			this.constant = constant;
		}
		
		public ConstDef(Collection<Modifier> modifiers, String name, Value constant,  Collection<Attribute> attributes) {
			super(attributes);
			this.modifiers = new ArrayList<Modifier>(modifiers);
			this.name = name;
			this.constant = constant;
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}
		
		public Value constant() {
			return constant;
		}
	}
		
	public static final class Method extends SyntacticElement.Impl {
		private List<Modifier> modifiers;
		private String name;		
		private Type.FunctionOrMethodOrMessage type;		
		private List<Case> cases;		
				
		public Method(Collection<Modifier> modifiers, String name,
				Type.FunctionOrMethodOrMessage type, Collection<Case> cases,
				Attribute... attributes) {
			super(attributes);
			this.modifiers = new ArrayList<Modifier>(modifiers);
			this.name = name;
			this.type = type;
			this.cases = Collections
					.unmodifiableList(new ArrayList<Case>(cases));
		}
		
		public Method(Collection<Modifier> modifiers, String name,
				Type.FunctionOrMethodOrMessage type, Collection<Case> cases,
				Collection<Attribute> attributes) {
			super(attributes);
			this.modifiers = new ArrayList<Modifier>(modifiers);
			this.name = name;
			this.type = type;
			this.cases = Collections
					.unmodifiableList(new ArrayList<Case>(cases));
		}
		
		public List<Modifier> modifiers() {
			return modifiers;
		}
		
		public String name() {
			return name;
		}
		
		public Type.FunctionOrMethodOrMessage type() {
			return type;
		}

		public List<Case> cases() {
			return cases;
		}

		public boolean isFunction() {
			return type instanceof Type.Function;
		}
		
		public boolean isMethod() {
			return type instanceof Type.Method;
		}
		
		public boolean isMessage() {
			return type instanceof Type.Message;
		}
		
		public boolean isPublic() {
				// TODO: fixme!
			return true;
		}
		
		public boolean isNative() {
			return modifiers.contains(Modifier.NATIVE);
		}
		
		public boolean isExport() {
			return modifiers.contains(Modifier.EXPORT);
		}
	}	
	
	public static final class Case extends SyntacticElement.Impl {				
		private final Block precondition;
		private final Block postcondition;
		private final Block body;
		private final ArrayList<String> locals;		
		
		public Case(Block body, Block precondition, Block postcondition,
				Collection<String> locals, Attribute... attributes) {
			super(attributes);			
			this.body = body;
			this.precondition = precondition;
			this.postcondition = postcondition;
			this.locals = new ArrayList<String>(locals);
		}

		public Case(Block body, Block precondition, Block postcondition,
				Collection<String> locals, Collection<Attribute> attributes) {
			super(attributes);			
			this.body = body;
			this.precondition = precondition;
			this.postcondition = postcondition;
			this.locals = new ArrayList<String>(locals);			
		}
		
		public Block body() {
			return body;
		}
		
		public Block precondition() {
			return precondition;
		}
		
		public Block postcondition() {
			return postcondition;
		}
		
		public List<String> locals() {
			return Collections.unmodifiableList(locals);
		}
	}
}
