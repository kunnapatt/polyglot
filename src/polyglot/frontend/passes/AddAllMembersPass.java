/*
 * AddAllMembersPass.java
 * 
 * Author: nystrom
 * Creation date: Jan 21, 2005
 */
package polyglot.frontend.passes;

import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.AllMembersAdded;
import polyglot.types.ParsedClassType;


public class AddAllMembersPass extends ClassFilePass {
    Scheduler scheduler;
    AllMembersAdded goal;
    
    public AddAllMembersPass(Scheduler scheduler, AllMembersAdded goal) {
        super(goal);
        this.scheduler = scheduler;
        this.goal = goal;
    }
    
    public boolean run() {
        ParsedClassType ct = goal.type();
        ct.superType();
        ct.interfaces();
        ct.fields();
        ct.methods();
        ct.constructors();
        ct.memberClasses();
        return true;
    }
}