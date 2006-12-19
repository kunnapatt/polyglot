/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

/*
 * ForwardReferencesChecked.java
 * 
 * Author: nystrom
 * Creation date: Oct 11, 2005
 */
package polyglot.frontend.goals;

import java.util.*;

import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.types.TypeSystem;
import polyglot.visit.FwdReferenceChecker;

public class ForwardReferencesChecked extends VisitorGoal {
    public static Goal create(Scheduler scheduler, Job job, TypeSystem ts, NodeFactory nf) {
        return scheduler.internGoal(new ForwardReferencesChecked(job, ts, nf));
    }

    protected ForwardReferencesChecked(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, new FwdReferenceChecker(job, ts, nf));
    }

    public Collection prerequisiteGoals(Scheduler scheduler) {
        List l = new ArrayList();
        l.add(scheduler.ReachabilityChecked(job));
        l.addAll(super.prerequisiteGoals(scheduler));
        return l;
    }
}
