/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ext.param.types;

import polyglot.types.*;
import polyglot.util.Position;  
import java.util.List;  

/**
 * Parametric class.  This class is a wrapper around
 * a ClassType that associates formal parameters with the class.
 * formals can be any type object.
 */
public interface PClass extends Importable {
    /**
     * The formal type parameters associated with <code>this</code>.
     * XXX What is this a list OF?
     */
    List formals();
    
    /**
     * The class associated with <code>this</code>.  Note that
     * <code>this</code> should never be used as a first-class type.
     */
    ClassType clazz();
    
    /**
     * Instantiate <code>this</code>.
     * @param pos The position of the instantiation
     * @param actuals The actual type parameters for the instantiation
     */
    ClassType instantiate(Position pos, List actuals) throws SemanticException;
}
