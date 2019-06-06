/*******************************************************************************
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2012 Polyglot project group, Cornell University
 * Copyright (c) 2006-2012 IBM Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This program and the accompanying materials are made available under
 * the terms of the Lesser GNU Public License v2.0 which accompanies this
 * distribution.
 *
 * The development of the Polyglot project has been supported by a
 * number of funding sources, including DARPA Contract F30602-99-1-0533,
 * monitored by USAF Rome Laboratory, ONR Grants N00014-01-1-0968 and
 * N00014-09-1-0652, NSF Grants CNS-0208642, CNS-0430161, CCF-0133302,
 * and CCF-1054172, AFRL Contract FA8650-10-C-7022, an Alfred P. Sloan
 * Research Fellowship, and an Intel Research Ph.D. Fellowship.
 *
 * See README for contributors.
 ******************************************************************************/

package polyglot.ext.jl8.ast;

import java.util.List;

import polyglot.ast.Expr_c;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Context;
import polyglot.util.CollectionUtil;
import polyglot.util.Copy;
import polyglot.util.InternalCompilerError;
import polyglot.util.ListUtil;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

/**
 * An immutable representation of a lambda expression.
 */
public class Lambda_c extends Expr_c implements Lambda {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected List<Formal> parameters;
    protected Term body;

    public Lambda_c(Position pos, List<Formal> parameters, Term body) {
        super(pos, null);
        assert_(pos, parameters, body);
        this.parameters = ListUtil.copy(parameters, true);
        this.body = body;
    }

    protected void assert_(Position pos, List<?> parameters, Term body) {
        assert parameters != null;
        assert body != null;
    }

    @Override
    public List<Formal> parameters() {
        return parameters;
    }

    @Override
    public Lambda parameters(List<Formal> parameters) {
        return parameters(this, parameters);
    }

    protected <N extends Lambda_c> N parameters(N n, List<Formal> parameters) {
        if (CollectionUtil.equals(n.parameters, parameters)) return n;
        if (n == this) n = Copy.Util.copy(n);
        n.parameters = ListUtil.copy(parameters, true);
        return n;
    }

    @Override
    public Term body() {
        return body;
    }

    @Override
    public Lambda body(Term body) {
        return body(this, body);
    }

    protected <N extends Lambda_c> N body(N n, Term body) {
        if (n.body == body) return n;
        if (n == this) n = Copy.Util.copy(n);
        n.body = body;
        return n;
    }

    /** Reconstruct the statement. */
    protected <N extends Lambda_c> N reconstruct(N n, List<Formal> parameters,
            Term body) {
        n = parameters(n, parameters);
        n = body(n, body);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<Formal> parameters = visitList(this.parameters, v);
        Term body = visitChild(this.body, v);
        return reconstruct(this, parameters, body);
    }

    @Override
    public Context enterScope(Context c) {
        // Parameters are in scope only within the lambda body.
        return c.pushBlock();
    }

    /*
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("try (");
        int count = 0;
        for (LocalDecl l : resources) {
            if (count++ > 2) {
                sb.append("...");
                break;
            }

            sb.append(l);
            sb.append(" ");
        }
        sb.append(") ");
        sb.append(tryBlock.toString());

        count = 0;
        for (Catch cb : catchBlocks) {
            if (count++ > 2) {
                sb.append("...");
                break;
            }

            sb.append(" ");
            sb.append(cb.toString());
        }

        if (finallyBlock != null) {
            sb.append(" finally ");
            sb.append(finallyBlock.toString());
        }

        return sb.toString();
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("try (");
        w.begin(0);

        int count = 0;
        for (LocalDecl l : resources) {
            if (count++ > 0) w.newline(0);
            print(l, w, tr);
        }
        w.end();
        w.write(")");
        printSubStmt(tryBlock, w, tr);

        for (Catch cb : catchBlocks) {
            w.newline(0);
            printBlock(cb, w, tr);
        }

        if (finallyBlock != null) {
            w.newline(0);
            w.write("finally");
            printSubStmt(finallyBlock, w, tr);
        }
    }
    */

    @Override
    public Term firstChild() {
        return listChild(parameters, null);
    }

    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        throw new InternalCompilerError("unimplemented");
//        v.push(this).visitCFGList(resources, tryBlock, ENTRY);
//        return super.acceptCFG(v, succs);
    }
}
