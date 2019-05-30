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

import polyglot.ast.MethodDecl;
import polyglot.ext.jl5.ast.JL5ProcedureDeclOps;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.Flags;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.SerialVersionUID;
import polyglot.visit.PrettyPrinter;

public class JL8MethodDeclExt extends JL8Ext implements JL5ProcedureDeclOps {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public MethodDecl buildTypesFinish(JL5TypeSystem ts, ParsedClassType ct,
            Flags flags, List<? extends Type> formalTypes,
            List<? extends Type> throwTypes, List<TypeVariable> typeParams) {
        MethodDecl md = (MethodDecl) node();
        if (ct.flags().isInterface()) {
            flags = flags.Public();
            if (md.body() == null) flags = flags.Abstract();
        }

        MethodInstance mi = ts.methodInstance(md.position(),
                                              ct,
                                              flags,
                                              ts.unknownType(md.position()),
                                              md.name(),
                                              formalTypes,
                                              throwTypes,
                                              typeParams);
        ct.addMethod(mi);
        return md.methodInstance(mi);
    }

    @Override
    public void prettyPrintHeader(Flags flags, CodeWriter w, PrettyPrinter tr) {
        MethodDecl md = (MethodDecl) node();
        superLang().prettyPrintHeader(md, flags, w, tr);
    }
}
