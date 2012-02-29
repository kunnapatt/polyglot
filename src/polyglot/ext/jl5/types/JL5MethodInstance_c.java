package polyglot.ext.jl5.types;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.Position;

@SuppressWarnings("serial")
public class JL5MethodInstance_c extends MethodInstance_c implements JL5MethodInstance {
	private List<TypeVariable> typeParams;
//    protected PClass instantiatedFrom;

    public JL5MethodInstance_c(JL5TypeSystem_c ts, Position pos,
			ReferenceType container, Flags flags, Type returnType,
			String name, List argTypes, List excTypes, List typeParams) {
    	super(ts, pos, container, flags, returnType, name, argTypes, excTypes);
    	this.typeParams = typeParams;
	}

	public boolean isVariableArity() {
        return JL5Flags.isVarArgs(this.flags());
    }

	@Override
    public List overridesImpl() {
        List l = new LinkedList();
        ReferenceType rt = container();
        JL5TypeSystem ts = (JL5TypeSystem)this.typeSystem();
        while (rt != null) {
            // add any method with the same name and formalTypes from 
            // rt
            for (MethodInstance mj : (List<MethodInstance>)rt.methodsNamed(name)) {
                if (ts.areOverrideEquivalent(this, (JL5MethodInstance)mj)) {
                    l.add(mj);
                }
            }

            ReferenceType sup = null;
            if (rt.superType() != null && rt.superType().isReference()) {
                sup = (ReferenceType) rt.superType();    
            }
            
            rt = sup;
        };

        return l;
    }

	public boolean canOverrideImpl(MethodInstance mj, boolean quiet)
			throws SemanticException {
        JL5MethodInstance mi = this;
        JL5TypeSystem ts = (JL5TypeSystem)this.typeSystem();
        if (!(ts.areOverrideEquivalent(mi, (JL5MethodInstance)mj))) {
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; incompatible parameter types",
                                        mi.position());
        }
                
        Type miRet = mi.returnType();
        Type mjRet = mj.returnType();

        if (!ts.areReturnTypeSubstitutable(miRet, mjRet)) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, "return type " + miRet +
                              " != " + mjRet);
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; attempting to use incompatible " +
                                        "return type\n" +                                        
                                        "found: " + miRet + "\n" +
                                        "required: " + mjRet, 
                                        mi.position());
        } 

        if (! ts.throwsSubset(mi, mj)) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, mi.throwTypes() + " not subset of " +
                              mj.throwTypes());
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; the throw set " + mi.throwTypes() + " is not a subset of the " +
                                        "overridden method's throw set " + mj.throwTypes() + ".", 
                                        mi.position());
        }   

        if (mi.flags().moreRestrictiveThan(mj.flags())) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, mi.flags() + " more restrictive than " +
                              mj.flags());
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; attempting to assign weaker " + 
                                        "access privileges", 
                                        mi.position());
        }

        if (mi.flags().isStatic() != mj.flags().isStatic()) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, mi.signature() + " is " + 
                              (mi.flags().isStatic() ? "" : "not") + 
                              " static but " + mj.signature() + " is " +
                              (mj.flags().isStatic() ? "" : "not") + " static");
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is " + 
                                        (mj.flags().isStatic() ? "" : "not") +
                                        "static", 
                                        mi.position());
        }

        if (mi != mj && !mi.equals(mj) && mj.flags().isFinal()) {
            // mi can "override" a final method mj if mi and mj are the same method instance.
            if (Report.should_report(Report.types, 3))
                Report.report(3, mj.flags() + " final");
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is final", 
                                        mi.position());
        }

        return true;
	}
	
	@Override
    public boolean callValidImpl(List argTypes) {
        List<Type> myFormalTypes = this.formalTypes;
        if (this.container() instanceof JL5ParsedClassType) {
            // we have a stripped off class type. Replace any type variables
            // with their bounds.
            JL5Subst erasureSubst = ((JL5ParsedClassType) this.container())
                    .erasureSubst();
            if (erasureSubst != null) {
                myFormalTypes = erasureSubst.substTypeList(this.formalTypes);
            }
        }

//         System.err.println("JL5MethodInstance_c callValid Impl " + this +" called with " +argTypes);
        // now compare myFormalTypes to argTypes
        if (!this.isVariableArity() && argTypes.size() != myFormalTypes.size()) {
//            System.err.println("     1");
            return false;
        }
        if (this.isVariableArity() && argTypes.size() < myFormalTypes.size()-1) {
            // the last (variable) argument can consume 0 or more of the actual arguments. 
//            System.err.println("     2");
            return false;
        }

        // Here, argTypes has at least myFormalTypes.size()-1 elements.
        Iterator formalTypes = myFormalTypes.iterator();
        Iterator actualTypes = argTypes.iterator();
        Type formal = null;
        while (actualTypes.hasNext()) {
            Type actual = (Type) actualTypes.next();
            if (formalTypes.hasNext()) {
                formal = (Type) formalTypes.next();
            }
            if (!formalTypes.hasNext() && this.isVariableArity()) {
                // varible arity method, and this is the last arg.
                ArrayType arr = (ArrayType) myFormalTypes.get(myFormalTypes.size() - 1);
                formal = arr.base();
            }
            if (!ts.isImplicitCastValid(actual, formal)) {
                // the actual can't be cast to the formal.
                // HOWEVER: there is still hope.
                if (this.isVariableArity()
                        && myFormalTypes.size() == argTypes.size()
                        && !formalTypes.hasNext()) {
                    // This is a variable arity method (e.g., m(int x,
                    // String[])) and there
                    // are the same number of actual arguments as formal
                    // arguments.
                    // The last actual can be either the base type of the array,
                    // or the array type.
                    ArrayType arr = (ArrayType) myFormalTypes.get(myFormalTypes.size() - 1);
                    if (!ts.isImplicitCastValid(actual, arr)) {
//                         System.err.println("     3: failed " + actual + " to " +formal + " and " + actual + " to " + arr);
                        return false;
                    }
                } else {
//                     System.err.println("     4: failed " + actual + " to " +formal);
                    return false;
                }
            }
        }

        return true;
    }

	
	/**
	 * See JLS 3rd ed. 15.12.2.5.
	 */
	@Override
    public boolean moreSpecificImpl(ProcedureInstance p) {
        JL5MethodInstance p1 = this;
        JL5MethodInstance p2 = (JL5MethodInstance)p;

//        if (p1.isVariableArity() != p2.isVariableArity()) {
//            // they must both be variable arity or both be fixed arity.
//            return false;
//        }
//        // are they fixed arity or variable arity?
//        if (!p1.isVariableArity()) {
//            // p1 and p2 are fixed arity
//            // !@! XXX need to handle generic methods, by substing types? Inference?
//            
//            if (p1.formalTypes().size() != p2.formalTypes().size()) {
//                return false;
//            }
//            Iterator<Type> formal1 = p1.formalTypes().iterator();
//            Iterator<Type> formal2 = p2.formalTypes().iterator();
//            while (formal1.hasNext()) {
//                Type t1 = formal1.next();
//                Type t2 = formal2.next();
//                if (!t1.isImplicitCastValid(t2)) {
//                    return false;
//                }
//            }    
//        }
//        else {
//            // p1 and p2 are variable arity
//            Iterator<Type> formal1 = p1.formalTypes().iterator();
//            Iterator<Type> formal2 = p2.formalTypes().iterator();
//            Type t1 = null;
//            Type t2 = null;
//            while (formal1.hasNext()) {
//                if (formal1.hasNext()) {
//                    t1 = formal1.next();
//                }
//                if (formal2.hasNext()) {
//                    t2 = formal2.next();
//                }
//                if (!t1.isImplicitCastValid(t2)) {
//                    return false;
//                }
//            }            
//        }
        return ts.callValid(p2, p1.formalTypes());

    }

    @Override
    public boolean isCanonical() {
        return super.isCanonical() && listIsCanonical(typeParams);
    }

    @Override
    public boolean isRawGeneric() {
        return !this.typeParams.isEmpty();
    }

    @Override
    public boolean isInstantiatedGeneric() {
        return false;
    }

    @Override
    public void setTypeParams(List<TypeVariable> typeParams) {
        this.typeParams = typeParams;
    }

    @Override
    public List<TypeVariable> typeParams() {
        return Collections.unmodifiableList(this.typeParams);
    }

    @Override
    public JL5Subst erasureSubst() {
        JL5TypeSystem ts = (JL5TypeSystem) this.typeSystem();
        return ts.erasureSubst(this.typeParams);
    }
}