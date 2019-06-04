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
package polyglot.ext.jl8;

import java.io.Reader;

import polyglot.ast.NodeFactory;
import polyglot.ext.jl5.ast.JL5ExtFactory_c;
import polyglot.ext.jl7.JL7ExtensionInfo;
import polyglot.ext.jl7.ast.JL7ExtFactory_c;
import polyglot.ext.jl7.ast.JL7NodeFactory_c;
import polyglot.ext.jl8.ast.J8Lang_c;
import polyglot.ext.jl8.ast.JL8ExtFactory_c;
import polyglot.ext.jl8.parse.Grm;
import polyglot.ext.jl8.parse.Lexer_c;
import polyglot.frontend.CupParser;
import polyglot.frontend.Parser;
import polyglot.frontend.Scheduler;
import polyglot.frontend.Source;
import polyglot.main.Version;
import polyglot.util.ErrorQueue;

/**
 * Extension information for jl8 extension.
 */
public class JL8ExtensionInfo extends JL7ExtensionInfo {

    protected polyglot.frontend.ExtensionInfo outputExtensionInfo;

    @Override
    public String defaultFileExtension() {
        return "jl8";
    }

    @Override
    public String compilerName() {
        return "jl8c";
    }

    @Override
    protected NodeFactory createNodeFactory() {
        // TODO jl8
        return new JL7NodeFactory_c(J8Lang_c.instance,
                                    new JL8ExtFactory_c(new JL7ExtFactory_c(new JL5ExtFactory_c())));
        /*
        JL5Options opt = (JL5Options) getOptions();
        if (!opt.removeJava5isms) {
            return new JL7NodeFactory_c(J7Lang_c.instance,
                                        new JL7ExtFactory_c(new JL5ExtFactory_c()));
        }
        else {
            return new JL7NodeFactory_c(J7Lang_c.instance,
                                        new JL7ExtFactory_c(new JL7ToJLExtFactory_c()));
        }
         */
    }

//    @Override
//    protected TypeSystem createTypeSystem() {
//        // TODO jl8
//        return new JL7TypeSystem_c();
//    }

    @Override
    public Scheduler createScheduler() {
        return new JL8Scheduler(this);
    }

    /**
     * Return a parser for {@code source} using the given
     * {@code reader}.
     */
    @Override
    public Parser parser(Reader reader, Source source, ErrorQueue eq) {
        reader = new polyglot.lex.EscapedUnicodeReader(reader);

        polyglot.lex.Lexer lexer = new Lexer_c(reader, source, eq);
        polyglot.parse.BaseParser parser = new Grm(lexer, ts, nf, eq);

        return new CupParser(parser, source, eq);
    }

//    @Override
//    public Set<String> keywords() {
//        // TODO jl8 lexer
//        return new Lexer_c(null).keywords();
//    }
//
    @Override
    public Version version() {
        return new JL8Version();
    }

}