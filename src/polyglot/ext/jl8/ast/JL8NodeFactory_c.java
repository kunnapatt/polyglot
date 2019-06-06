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

import polyglot.ast.Block;
import polyglot.ast.Catch;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.ast.AnnotationElem;
import polyglot.ext.jl5.ast.JL5AnnotatedElementExt;
import polyglot.ext.jl5.ast.JL5Ext;
import polyglot.ext.jl7.ast.AmbDiamondTypeNode;
import polyglot.ext.jl7.ast.AmbUnionType;
import polyglot.ext.jl7.ast.JL7NodeFactory_c;
import polyglot.ext.jl7.ast.MultiCatch;
import polyglot.ext.jl7.ast.MultiCatch_c;
import polyglot.ext.jl7.ast.TryWithResources;
import polyglot.ext.jl7.ast.TryWithResources_c;
import polyglot.types.Flags;
import polyglot.util.Position;

public class JL8NodeFactory_c extends JL7NodeFactory_c
        implements JL8NodeFactory {
    public JL8NodeFactory_c() {
        this(J8Lang_c.instance);
    }

    public JL8NodeFactory_c(J8Lang lang) {
        super(lang);
    }

    public JL8NodeFactory_c(J8Lang lang, JL8ExtFactory extFactory) {
        super(lang, extFactory);
    }

    @Override
    public JL8ExtFactory extFactory() {
        return (JL8ExtFactory) super.extFactory();
    }

    @Override
    public J8Lang lang() {
        return (J8Lang) super.lang();
    }

    @Override
    public AmbDiamondTypeNode AmbDiamondTypeNode(Position pos, TypeNode base) {
        AmbDiamondTypeNode n = new AmbDiamondTypeNode(pos, base);
        n = ext(n, extFactory().extAmbDiamondTypeNode());
        return n;
    }

    @Override
    public TypeNode AmbUnionType(Position pos, List<TypeNode> alternatives) {
        AmbUnionType n = new AmbUnionType(pos, alternatives);
        n = ext(n, extFactory().extAmbUnionType());
        return n;
    }

    @Override
    public MultiCatch MultiCatch(Position pos, Formal formal,
            List<TypeNode> alternatives, Block body) {
        MultiCatch n = new MultiCatch_c(pos, formal, alternatives, body);
        n = ext(n, extFactory().extMultiCatch());
        return n;
    }

    @Override
    public LocalDecl Resource(Position pos, Flags flags,
            List<AnnotationElem> annotations, TypeNode type, Id name,
            Expr init) {
        LocalDecl n =
                super.LocalDecl(pos, flags, annotations, type, name, init);
        n = ext(n, extFactory().extResource());
        // FIXME ugh!
        JL5AnnotatedElementExt jl5ext = (JL5AnnotatedElementExt) JL5Ext.ext(n);
        n = (LocalDecl) jl5ext.annotationElems(annotations);
        return n;
    }

    @Override
    public TryWithResources TryWithResources(Position pos,
            List<LocalDecl> resources, Block tryBlock, List<Catch> catchBlocks,
            Block finallyBlock) {
        TryWithResources n = new TryWithResources_c(pos,
                                                    resources,
                                                    tryBlock,
                                                    catchBlocks,
                                                    finallyBlock);
        n = ext(n, extFactory().extTryWithResources());
        return n;
    }

    @Override
    public Lambda Lambda(Position pos, List<Formal> parameters, Term body) {
        Lambda n = new Lambda_c(pos, parameters, body);
        n = ext(n, extFactory().extLambda());
        return n;
    }
}
