/*
 * ResolveSuperTypesPass.java
 * 
 * Author: nystrom
 * Creation date: Jan 21, 2005
 */
package polyglot.frontend.passes;

import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.SupertypesResolved;
import polyglot.types.ParsedClassType;


public class ResolveSuperTypesPass extends ClassFilePass {
    Scheduler scheduler;
    SupertypesResolved goal;
    
    public ResolveSuperTypesPass(Scheduler scheduler, SupertypesResolved goal) {
        super(goal);
        this.scheduler = scheduler;
        this.goal = goal;
    }
    
    public boolean run() {
        ParsedClassType ct = goal.type();
        ct.superType();
        ct.interfaces();
        ct.setSupertypesResolved(true);
        return true;
    }
}