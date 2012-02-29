package polyglot.ext.jl5.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import polyglot.types.*;
import polyglot.util.StringUtil;

public class JL5Context_c extends Context_c implements JL5Context {

	protected Map<String, TypeVariable> typeVars;

	protected TypeVariable typeVariable;

	protected Type switchType;

	public static final Kind TYPE_VAR = new Kind("type-var");
	public static final Kind SWITCH = new Kind("switch");

	public JL5Context_c(TypeSystem ts) {
		super(ts);
	}

	public JL5TypeSystem typeSystem() {
		return (JL5TypeSystem) ts;
	}

	public VarInstance findVariableSilent(String name) {
		VarInstance vi = findVariableInThisScope(name);
		if (vi != null) {
			return vi;
		}

		try {
			// might be static
			if (importTable() != null) {
				JL5ImportTable jit = (JL5ImportTable) importTable();
				for (Iterator it = jit.singleStaticImports().iterator(); it.hasNext();) {
					String next = (String) it.next();
					String id = StringUtil.getShortNameComponent(next);
					if (name.equals(id)) {
						Named nt = ts.forName(StringUtil.getPackageComponent(next));
						if (nt instanceof Type) {
							Type t = (Type) nt;
							try {
								vi = ts.findField(t.toClass(), name);
							} catch (SemanticException e) {
							}
							if (vi != null) {
								return vi;
							}
						}
					}
				}
				if (vi == null) {
					for (Iterator it = jit.staticOnDemandImports().iterator(); it.hasNext();) {
						String next = (String) it.next();
						Named nt = ts.forName(next);
						if (nt instanceof Type) {
							Type t = (Type) nt;
							try {
								vi = ts.findField(t.toClass(), name);
							} catch (SemanticException e) {
							}
							if (vi != null)
								return vi;
						}
					}
				}
			}
		} catch (SemanticException e) {
		}

		if (outer != null) {
			return outer.findVariableSilent(name);
		}
		return null;
	}

	protected Context_c push() {
		JL5Context_c c = (JL5Context_c) super.push();
		c.typeVars = this.typeVars;
		return c;
	}

	public JL5Context pushTypeVariable(TypeVariable iType) {
		JL5Context_c v = (JL5Context_c) push();
		v.typeVariable = iType;
		v.kind = TYPE_VAR;
		// v.outer = this;
		return v;
	}

	public TypeVariable findTypeVariableInThisScope(String name) {
		if (typeVariable != null && typeVariable.name().equals(name))
			return typeVariable;
		if (typeVars != null && typeVars.containsKey(name)) {
			return (TypeVariable) typeVars.get(name);
		}
		if (outer != null) {
			return ((JL5Context) outer).findTypeVariableInThisScope(name);
		}
		return null;
	}

	public boolean inTypeVariable() {
		return kind == TYPE_VAR;
	}

	public String toString() {
		return super.toString() + "type var: " + typeVariable + "type vars: "
				+ typeVars;
	}

	public JL5Context addTypeVariable(TypeVariable type) {
		if (typeVars == null)
			typeVars = new HashMap<String, TypeVariable>();
		typeVars.put(type.name(), type);
		return this;
	}

	@Override
	public Context pushSwitch(Type type) {
		JL5Context_c c = (JL5Context_c) push();
		c.switchType = type;
		c.kind = SWITCH;
		return c;
	}

	@Override
	public Type switchType() {
		return switchType;
	}
	
    @Override
    public MethodInstance findMethod(String name, List argTypes) throws SemanticException {
        try {
            return super.findMethod(name, argTypes);
        }
        catch (SemanticException e) {
            // couldn't find the method.
            // try static imports.
            JL5ImportTable it = (JL5ImportTable)this.importTable();
            if (it != null && this.currentClass() != null) {
                ReferenceType rt = it.findTypeContainingMethodOrField(name);
                if (rt != null) {
                    try {
                        return ts.findMethod(rt, name, argTypes, this.currentClass());
                    }
                    catch (SemanticException f) {
                        // ignore this exception and throw the previous one.
                    }
                }
            }
            throw e;
        }
    }


}