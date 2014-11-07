package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import java_cup.Main;
import java_cup.lalr_item;
import java_cup.lalr_state;
import java_cup.non_terminal;
import java_cup.production;
import java_cup.symbol;
import java_cup.symbol_part;
import java_cup.terminal;

public class UnifiedExample {

    public static boolean optimizeShortestPath = true;
    public static boolean extendedSearch = false;

    protected static final int PRODUCTION_COST = 50;
    protected static final int REDUCE_COST = 1;
    protected static final int SHIFT_COST = 1;
    protected static final int UNSHIFT_COST = 1;
    protected static final int DUPLICATE_PRODUCTION_COST = 0;
    protected static final int EXTENDED_COST = 10000;

    protected static final long ASSURANCE_LIMIT = 2 * 1000000000L;
    protected static final long TIME_LIMIT = 5 * 1000000000L;

    protected lalr_state conflict;
    protected lalr_item itm1;
    protected lalr_item itm2;
    protected terminal nextSym;
    protected boolean isShiftReduce;
    protected List<StateItem> shortestConflictPath;
    protected Set<lalr_state> scpSet;
    protected Set<lalr_state> rppSet;

    public UnifiedExample(lalr_state conflict, lalr_item itm1, lalr_item itm2,
            terminal nextSym) {
        this.conflict = conflict;
        this.nextSym = nextSym;
        if (itm1.dot_at_end()) {
            if (itm2.dot_at_end()) {
                // reduce/reduce conflict
                this.itm1 = itm1;
                this.itm2 = itm2;
                isShiftReduce = false;
            }
            else {
                // shift/reduce conflict
                this.itm1 = itm1;
                this.itm2 = itm2;
                isShiftReduce = true;
            }
        }
        else if (itm2.dot_at_end()) {
            // shift/reduce conflict
            this.itm1 = itm2;
            this.itm2 = itm1;
            isShiftReduce = true;
        }
        else throw new Error("Expected at least one reduce item.");
        shortestConflictPath = findShortestPathFromStart(optimizeShortestPath);
        scpSet = new HashSet<>(shortestConflictPath.size());
        production reduceProd = this.itm1.the_production();
        rppSet = new HashSet<>(reduceProd.rhs_length());
        boolean reduceProdReached = false;
        for (StateItem si : shortestConflictPath) {
            scpSet.add(si.state);
            reduceProdReached =
                    reduceProdReached || si.item.the_production() == reduceProd;
            if (reduceProdReached) rppSet.add(si.state);
        }
    }

    protected List<StateItem> findShortestPathFromStart(boolean optimized) {
        StateItem.init();
        long start = System.nanoTime();
        lalr_state startState = lalr_state.startState();
        lalr_item startItem = lalr_state.startItem();
        StateItem source = StateItem.lookup(startState, startItem);
        StateItem target = StateItem.lookup(conflict, itm1);
        Set<StateItem> eligible =
                optimized ? eligibleStateItemsToConflict(target) : null;
        Queue<List<StateItemWithLookahead>> queue = new LinkedList<>();
        Set<StateItemWithLookahead> visited = new HashSet<>();
        {
            List<StateItemWithLookahead> init = new LinkedList<>();
            init.add(new StateItemWithLookahead(source,
                                                StateItem.symbolSet(source.item.lookahead())));
            queue.add(init);
        }
        while (!queue.isEmpty()) {
            List<StateItemWithLookahead> path = queue.remove();
            StateItemWithLookahead last = path.get(path.size() - 1);
            if (visited.contains(last)) continue;
            visited.add(last);
            if (target.equals(last.si) && last.lookahead.contains(nextSym)) {
                // Done
//                System.err.println(path);
                if (Main.report_cex_stats)
                    System.out.println("reachable"
                            + (optimized ? " optimized" : "") + ":\n"
                            + (System.nanoTime() - start));
                System.err.println("reachable"
                        + (optimized ? " optimized" : "") + ": "
                                + (System.nanoTime() - start));
                List<StateItem> shortestConflictPath =
                        new ArrayList<>(path.size());
                for (StateItemWithLookahead sil : path)
                    shortestConflictPath.add(sil.si);
                return shortestConflictPath;
            }
            if (StateItem.trans.containsKey(last.si)) {
                for (Map.Entry<symbol, StateItem> trans : StateItem.trans.get(last.si)
                                                                         .entrySet()) {
                    StateItem nextSI = trans.getValue();
                    if (optimized && !eligible.contains(nextSI)) continue;
                    StateItemWithLookahead next =
                            new StateItemWithLookahead(nextSI, last.lookahead);
                    List<StateItemWithLookahead> nextPath =
                            new LinkedList<>(path);
                    nextPath.add(next);
                    queue.add(nextPath);
                }
            }
            if (StateItem.prods.containsKey(last.si)) {
                production prod = last.si.item.the_production();
                int len = prod.rhs_length();
                int pos = last.si.item.dot_pos() + 1;
                Set<symbol> lookahead = new HashSet<>();
                do {
                    if (pos == len) {
                        lookahead.addAll(last.lookahead);
                        break;
                    }
                    else {
                        symbol sym = rhs(prod, pos);
                        if (sym instanceof terminal) {
                            lookahead.add(sym);
                            break;
                        }
                        else {
                            non_terminal nt = (non_terminal) sym;
                            lookahead.addAll(StateItem.symbolSet(nt.first_set()));
                            if (!nt.nullable()) break;
                        }
                    }
                    pos++;
                } while (pos <= len);
                for (lalr_item itm : StateItem.prods.get(last.si)) {
                    StateItem nextSI = StateItem.lookup(last.si.state, itm);
                    if (optimized && !eligible.contains(nextSI)) continue;
                    StateItemWithLookahead next =
                            new StateItemWithLookahead(nextSI, lookahead);
                    List<StateItemWithLookahead> nextPath =
                            new LinkedList<>(path);
                    nextPath.add(next);
                    queue.add(nextPath);
                }
            }
        }
        throw new Error("Cannot find shortest path to conflict state.");
    }

    protected Set<StateItem> eligibleStateItemsToConflict(StateItem target) {
        Set<StateItem> result = new HashSet<>();
        Queue<StateItem> queue = new LinkedList<>();
        queue.add(target);
        while (!queue.isEmpty()) {
            StateItem si = queue.remove();
            if (result.contains(si)) continue;
            result.add(si);
            // Consider reverse transitions and reverse productions.
            if (StateItem.revTrans.containsKey(si))
                for (Set<StateItem> prev : StateItem.revTrans.get(si).values())
                    queue.addAll(prev);
            if (si.item.dot_pos() == 0) {
                production prod = si.item.the_production();
                symbol lhs = prod.lhs().the_symbol();
                if (StateItem.revProds.containsKey(si.state)) {
                    Map<non_terminal, Set<lalr_item>> revProd =
                            StateItem.revProds.get(si.state);
                    if (revProd.containsKey(lhs))
                        for (lalr_item prev : revProd.get(lhs))
                            queue.add(StateItem.lookup(si.state, prev));
                }
            }
        }
        return result;
    }

    protected class StateItemWithLookahead {
        protected StateItem si;
        protected Set<symbol> lookahead;

        protected StateItemWithLookahead(StateItem si, Set<symbol> lookahead) {
            this.si = si;
            this.lookahead = lookahead;
        }

        @Override
        public String toString() {
            return si.toString() + lookahead.toString();
        }

        @Override
        public int hashCode() {
            return si.hashCode() * 31 + lookahead.hashCode();
        }

        protected boolean equals(StateItemWithLookahead sil) {
            return si.equals(sil.si) && lookahead.equals(sil.lookahead);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StateItemWithLookahead)) return false;
            return equals((StateItemWithLookahead) o);
        }
    }

    public Counterexample find() {
        StateItem.init();
        return findExample();
    }

    protected Counterexample findExample() {
        SearchState initial =
                new SearchState(StateItem.lookup(conflict, itm1),
                                StateItem.lookup(conflict, itm2));
        long start;
        Map<Integer, FixedComplexitySearchState> fcssMap = new HashMap<>();
        PriorityQueue<FixedComplexitySearchState> pq = new PriorityQueue<>();
        Map<List<StateItem>, Set<List<StateItem>>> visited = new HashMap<>();
        add(pq, fcssMap, visited, initial);
        start = System.nanoTime();
        boolean assurancePrinted = false;
        SearchState stage3result = null;
        while (!pq.isEmpty()) {
            FixedComplexitySearchState fcss = pq.remove();
            for (SearchState ss : fcss.sss) {
                int stage = 2;
                StateItem si1src = ss.states1.get(0);
                StateItem si2src = ss.states2.get(0);
                visited(visited, ss);
                if (ss.reduceDepth < 0 && ss.shiftDepth < 0) {
                    // Stage 3
                    stage = 3;
                    if (si1src.item.the_production().lhs().the_symbol() == si2src.item.the_production()
                            .lhs()
                            .the_symbol()
                            && hasCommonPrefix(si1src.item, si2src.item)) {
                        if (ss.derivs1.size() == 1
                                && ss.derivs2.size() == 1
                                && ss.derivs1.get(0).sym == ss.derivs2.get(0).sym) {
                            System.err.println(ss.complexity);
                            return new Counterexample(ss.derivs1.get(0),
                                                      ss.derivs2.get(0),
                                                      true);
                        }
                        if (stage3result == null) stage3result = ss;
                        stage = 4;
                    }
                }
                if (!assurancePrinted
                        && System.nanoTime() - start > ASSURANCE_LIMIT
                        && stage3result != null) {
                    System.err.println("Productions leading up to the conflict stage found.  Still finding a possible unified example...");
                    assurancePrinted = true;
                }
                if (System.nanoTime() - start > TIME_LIMIT
                        && stage3result != null) {
                    System.err.println("time limit exceeded: "
                            + (System.nanoTime() - start));
                    System.err.println(stage3result.complexity);
                    if (Main.report_cex_stats)
                        System.out.println("time limit exceeded");
                    return completeDivergingExamples(stage3result);
                }
                StateItem si1 = ss.states1.get(ss.states1.size() - 1);
                StateItem si2 = ss.states2.get(ss.states2.size() - 1);
                boolean si1reduce = si1.item.dot_at_end();
                boolean si2reduce = si2.item.dot_at_end();
                symbol si1sym = si1reduce ? null : si1.item.symbol_after_dot();
                symbol si2sym = si2reduce ? null : si2.item.symbol_after_dot();
                if (!si1reduce && !si2reduce) {
                    // Both paths are not reduce items.
                    // Two actions are possible:
                    // - Make a transition on the next symbol of the items,
                    //   if they are the same.
                    // - Take a production step, avoiding duplicates as necessary.
                    if (si1sym == si2sym) {
                        StateItem si1last =
                                StateItem.trans.get(si1).get(si1sym);
                        StateItem si2last =
                                StateItem.trans.get(si2).get(si2sym);
                        List<Derivation> derivs1 = new LinkedList<>();
                        List<Derivation> derivs2 = new LinkedList<>();
                        List<StateItem> states1 = new LinkedList<>();
                        List<StateItem> states2 = new LinkedList<>();
                        derivs1.add(new Derivation(si1sym));
                        states1.add(si1last);
                        derivs2.add(new Derivation(si2sym));
                        states2.add(si2last);
                        // Check for subsequent nullable nonterminals
                        production prod1 = si1.item.the_production();
                        for (int pos = si1.item.dot_pos() + 1, len =
                                prod1.rhs_length(); pos < len; pos++) {
                            symbol sp = rhs(prod1, pos);
                            if (!sp.is_non_term()) break;
                            non_terminal nt = (non_terminal) sp;
                            if (!nt.nullable()) break;
                            si1last = StateItem.trans.get(si1last).get(nt);
                            derivs1.add(new Derivation(nt,
                                                       Collections.<Derivation> emptyList()));
                            states1.add(si1last);
                        }
                        production prod2 = si2.item.the_production();
                        for (int pos = si2.item.dot_pos() + 1, len =
                                prod2.rhs_length(); pos < len; pos++) {
                            symbol sp = rhs(prod2, pos);
                            if (!sp.is_non_term()) break;
                            non_terminal nt = (non_terminal) sp;
                            if (!nt.nullable()) break;
                            si2last = StateItem.trans.get(si2last).get(nt);
                            derivs2.add(new Derivation(nt,
                                                       Collections.<Derivation> emptyList()));
                            states2.add(si2last);
                        }
                        for (int i = 1, size1 = derivs1.size(); i <= size1; i++) {
                            List<Derivation> subderivs1 =
                                    new ArrayList<>(derivs1.subList(0, i));
                            List<StateItem> substates1 =
                                    new ArrayList<>(states1.subList(0, i));
                            for (int j = 1, size2 = derivs2.size(); j <= size2; j++) {
                                List<Derivation> subderivs2 =
                                        new ArrayList<>(derivs2.subList(0, j));
                                List<StateItem> substates2 =
                                        new ArrayList<>(states2.subList(0, j));
                                SearchState copy = ss.copy();
                                copy.derivs1.addAll(subderivs1);
                                copy.states1.addAll(substates1);
                                copy.derivs2.addAll(subderivs2);
                                copy.states2.addAll(substates2);
                                copy.complexity += 2 * SHIFT_COST;
                                add(pq, fcssMap, visited, copy);
                            }
                        }
                    }
                    if (si1sym instanceof non_terminal
                            && StateItem.prods.containsKey(si1))
                        for (lalr_item itm1 : StateItem.prods.get(si1)) {
                            // Take production step only if lhs is not nullable and
                            // if first rhs symbol is compatible with the other path
                            boolean applicable =
                                    !itm1.dot_at_end()
                                            && compatible(itm1.symbol_after_dot(),
                                                          si2sym);
                            if (!applicable) continue;
                            production prod = si1.item.the_production();
                            production nextProd = itm1.the_production();
                            if (!StateItem.productionAllowed(prod, nextProd))
                                continue;
                            SearchState copy = ss.copy();
                            StateItem next = StateItem.lookup(si1.state, itm1);
                            // TODO
                            if (copy.states1.contains(next))
                                copy.complexity += DUPLICATE_PRODUCTION_COST;
                            copy.states1.add(next);
                            copy.complexity += PRODUCTION_COST;
                            add(pq, fcssMap, visited, copy);
                        }
                    if (si2sym instanceof non_terminal
                            && StateItem.prods.containsKey(si2))
                        for (lalr_item itm2 : StateItem.prods.get(si2)) {
                            // Take production step only if lhs is not nullable and
                            // if first rhs symbol is compatible with the other path
                            boolean applicable =
                                    !itm2.dot_at_end()
                                            && compatible(itm2.symbol_after_dot(),
                                                          si1sym);
                            if (!applicable) continue;
                            production prod = si2.item.the_production();
                            production nextProd = itm2.the_production();
                            if (!StateItem.productionAllowed(prod, nextProd))
                                continue;
                            SearchState copy = ss.copy();
                            StateItem next = StateItem.lookup(si2.state, itm2);
                            // TODO
                            if (copy.states2.contains(next))
                                copy.complexity += DUPLICATE_PRODUCTION_COST;
                            copy.states2.add(next);
                            if (ss.shiftDepth >= 0) copy.shiftDepth++;
                            copy.complexity += PRODUCTION_COST;
                            add(pq, fcssMap, visited, copy);
                        }
                }
                else {
                    // One of the paths requires a reduction.
                    production prod1 = si1.item.the_production();
                    int len1 = prod1.rhs_length();
                    production prod2 = si2.item.the_production();
                    int len2 = prod2.rhs_length();
                    int size1 = ss.states1.size();
                    int size2 = ss.states2.size();
                    boolean ready1 = si1reduce && size1 > len1;
                    boolean ready2 = si2reduce && size2 > len2;
                    // If there is a path ready for reduction
                    // without being prepended further, reduce.
                    // TODO transition on nullable symbols
                    if (ready1) {
                        List<SearchState> reduced1 = ss.reduce1(si2sym);
                        if (ready2) {
                            reduced1.add(ss);
                            for (SearchState red1 : reduced1) {
                                for (SearchState candidate : red1.reduce2(si1sym))
                                    add(pq, fcssMap, visited, candidate);
                                if (si1reduce && red1 != ss)
                                    add(pq, fcssMap, visited, red1);
                            }
                        }
                        else {
                            for (SearchState candidate : reduced1)
                                add(pq, fcssMap, visited, candidate);
                        }
                    }
                    else if (ready2) {
                        List<SearchState> reduced2 = ss.reduce2(si1sym);
                        for (SearchState candidate : reduced2)
                            add(pq, fcssMap, visited, candidate);
                    }
                    // Otherwise, prepend both paths and continue.
                    else {
                        symbol sym;
                        if (si1reduce && !ready1)
                            sym = rhs(prod1, len1 - size1);
                        else sym = rhs(prod2, len2 - size2);
                        for (SearchState prepended : ss.prepend(sym,
                                                                null,
                                                                null,
                                                                ss.reduceDepth >= 0
                                                                        ? rppSet
                                                                        : scpSet)) {
                            add(pq, fcssMap, visited, prepended);
                        }
                    }
                }
            }
            fcssMap.remove(fcss.complexity);
        }
        // No unifying examples.  Construct examples from common shortest path
        // to conflict state.
        return exampleFromShortestPath();
    }

    protected void add(PriorityQueue<FixedComplexitySearchState> pq,
            Map<Integer, FixedComplexitySearchState> fcssMap,
            Map<List<StateItem>, Set<List<StateItem>>> visited, SearchState ss) {
        Set<List<StateItem>> visited1 = visited.get(ss.states1);
        if (visited1 != null && visited1.contains(ss.states2)) return;
        FixedComplexitySearchState fcss = fcssMap.get(ss.complexity);
        if (fcss == null) {
            fcss = new FixedComplexitySearchState(ss.complexity);
            fcssMap.put(ss.complexity, fcss);
            pq.add(fcss);
        }
        fcss.add(ss);
    }

    protected void visited(Map<List<StateItem>, Set<List<StateItem>>> visited,
            SearchState ss) {
        Set<List<StateItem>> visited1 = visited.get(ss.states1);
        if (visited1 == null) {
            visited1 = new HashSet<>();
            visited.put(ss.states1, visited1);
        }
        visited1.add(ss.states2);
    }

    protected Counterexample exampleFromShortestPath() {
        StateItem si = StateItem.lookup(conflict, itm2);
        List<StateItem> result = new LinkedList<>();
        result.add(si);
        ListIterator<StateItem> itr =
                shortestConflictPath.listIterator(shortestConflictPath.size());
        // refsi is the last StateItem in this state of the shortest path.
        StateItem refsi = itr.previous();
        for (; refsi != null;) {
            // Construct a list of items in the same state as refsi.
            // prevrefsi is the last StateItem in the previous state.
            List<StateItem> refsis = new LinkedList<>();
            refsis.add(refsi);
            StateItem prevrefsi = itr.hasPrevious() ? itr.previous() : null;
            if (prevrefsi != null) {
                for (int curPos = refsi.item.dot_pos(), prevPos =
                        prevrefsi.item.dot_pos(); prevrefsi != null
                        && prevPos + 1 != curPos;) {
                    refsis.add(0, prevrefsi);
                    curPos = prevPos;
                    if (itr.hasPrevious()) {
                        prevrefsi = itr.previous();
                        prevPos = prevrefsi.item.dot_pos();
                    }
                    else prevrefsi = null;
                }
            }
            if (si == refsi || si.item == lalr_state.startItem()) {
                // Reached common item; prepend to the beginning.
                refsis.remove(refsis.size() - 1);
                result.addAll(0, refsis);
                if (prevrefsi != null) result.add(0, prevrefsi);
                while (itr.hasPrevious())
                    result.add(0, itr.previous());
                Derivation deriv1 =
                        completeDivergingExample(shortestConflictPath);
                Derivation deriv2 = completeDivergingExample(result);
                return new Counterexample(deriv1, deriv2, false);
            }

            int pos = si.item.dot_pos();
            if (pos == 0) {
                // For a production item, find a sequence of items within the
                // same state that leads to this production.
                List<StateItem> init = new LinkedList<>();
                init.add(si);
                Queue<List<StateItem>> queue = new LinkedList<>();
                queue.add(init);
                while (!queue.isEmpty()) {
                    List<StateItem> sis = queue.remove();
                    StateItem sisrc = sis.get(0);
                    if (sisrc.item == lalr_state.startItem()) {
                        sis.remove(sis.size() - 1);
                        result.addAll(0, sis);
                        si = sisrc;
                        break;
                    }
                    int srcpos = sisrc.item.dot_pos();
                    if (srcpos > 0) {
                        // Determine if reverse transition is possible.
                        production prod = sisrc.item.the_production();
                        symbol sym = rhs(prod, srcpos - 1);
                        for (StateItem prevsi : StateItem.revTrans.get(sisrc)
                                .get(sym)) {
                            // Only look for state compatible with the shortest path.
                            if (prevsi.state != prevrefsi.state) continue;
                            sis.remove(sis.size() - 1);
                            result.addAll(0, sis);
                            result.add(0, prevsi);
                            si = prevsi;
                            refsi = prevrefsi;
                            queue.clear();
                            break;
                        }
                    }
                    else {
                        production prod = sisrc.item.the_production();
                        symbol lhs = prod.lhs().the_symbol();
                        for (lalr_item prev : StateItem.revProds.get(sisrc.state)
                                                                .get(lhs)) {
                            StateItem prevsi =
                                    StateItem.lookup(sisrc.state, prev);
                            if (sis.contains(prevsi)) continue;
                            List<StateItem> prevsis = new LinkedList<>(sis);
                            prevsis.add(0, prevsi);
                            queue.add(prevsis);
                        }
                    }
                }
            }
            else {
                // If not a production item, make a reverse transition.
                production prod = si.item.the_production();
                symbol sym = rhs(prod, pos - 1);
                for (StateItem prevsi : StateItem.revTrans.get(si).get(sym)) {
                    // Only look for state compatible with the shortest path.
                    if (prevsi.state != prevrefsi.state) continue;
                    result.add(0, prevsi);
                    si = prevsi;
                    refsi = prevrefsi;
                    break;
                }
            }
        }
        throw new Error("Cannot find derivation to conflict state.");
    }

    protected Counterexample completeDivergingExamples(SearchState ss) {
        Derivation deriv1 = completeDivergingExample(ss.states1, ss.derivs1);
        Derivation deriv2 = completeDivergingExample(ss.states2, ss.derivs2);
        return new Counterexample(deriv1, deriv2, false);
    }

    protected Derivation completeDivergingExample(List<StateItem> states) {
        return completeDivergingExample(states,
                                        Collections.<Derivation> emptyList());
    }

    protected Derivation completeDivergingExample(List<StateItem> states,
            List<Derivation> derivs) {
        List<Derivation> result = new LinkedList<>();
        ListIterator<Derivation> dItr = derivs.listIterator(derivs.size());
        for (ListIterator<StateItem> sItr = states.listIterator(states.size()); sItr.hasPrevious();) {
            StateItem si = sItr.previous();
            int pos = si.item.dot_pos();
            production prod = si.item.the_production();
            int len = prod.rhs_length();
            // symbols after dot
            if (result.isEmpty()) {
                if (derivs.isEmpty()) result.add(Derivation.dot);
                if (!si.item.dot_at_end())
                    result.add(new Derivation(rhs(prod, pos)));
            }
            for (int i = pos + 1; i < len; i++) {
                result.add(new Derivation(rhs(prod, i)));
            }
            // symbols before dot
            for (int i = pos - 1; i >= 0; i--) {
                if (sItr.hasPrevious()) sItr.previous();
                result.add(0, dItr.hasPrevious()
                        ? dItr.previous() : new Derivation(rhs(prod, i)));
            }
            // completing the derivation
            symbol lhs = prod.lhs().the_symbol();
            Derivation deriv = new Derivation(lhs, result);
            result = new LinkedList<>();
            result.add(deriv);
        }
        return result.get(0);
    }

    protected static class FixedComplexitySearchState implements
            Comparable<FixedComplexitySearchState> {
        protected int complexity;
        protected Set<SearchState> sss;

        protected FixedComplexitySearchState(int complexity) {
            this.complexity = complexity;
            sss = new HashSet<>();
        }

        protected void add(SearchState ss) {
            sss.add(ss);
        }

        @Override
        public int compareTo(FixedComplexitySearchState o) {
            return complexity - o.complexity;
        }
    }

    /**
     * A search state consists of
     * - a pair of the following items:
     *   - a list of derivations, simulating the parser's symbol stack
     *   - a list of StateItems, simulating the parser's state stack but
     *     including explicit production steps
     *   Each item in the pair corresponds to a possible parse that involves
     *   each of the conflict LALR items.
     * - a complexity of the partial parse trees, determined by the number of
     *   states and production steps the parse has to encounter
     * - a shift depth, indicating the number of unreduced production steps
     *   that has been made from the original shift item.  This helps keep
     *   track of when the shift conflict item is reduced.
     * @author Chinawat
     *
     */
    protected class SearchState implements Comparable<SearchState> {

        protected List<Derivation> derivs1, derivs2;
        protected List<StateItem> states1, states2;
        protected int complexity, reduceDepth, shiftDepth;

        protected SearchState(StateItem si1, StateItem si2) {
            derivs1 = new LinkedList<>();
            derivs2 = new LinkedList<>();
            states1 = new LinkedList<>();
            states2 = new LinkedList<>();
            states1.add(si1);
            states2.add(si2);
            complexity = 0;
            reduceDepth = 0;
            shiftDepth = 0;
        }

        private SearchState(List<Derivation> derivs1, List<Derivation> derivs2,
                List<StateItem> states1, List<StateItem> states2,
                int complexity, int reduceDepth, int shiftDepth) {
            this.derivs1 = new LinkedList<>(derivs1);
            this.derivs2 = new LinkedList<>(derivs2);
            this.states1 = new LinkedList<>(states1);
            this.states2 = new LinkedList<>(states2);
            this.complexity = complexity;
            this.reduceDepth = reduceDepth;
            this.shiftDepth = shiftDepth;
        }

        protected SearchState copy() {
            return new SearchState(derivs1,
                                   derivs2,
                                   states1,
                                   states2,
                                   complexity,
                                   reduceDepth,
                                   shiftDepth);
        }

        protected List<SearchState> prepend(symbol sym, symbol nextSym1,
                                            symbol nextSym2) {
            return prepend(sym, nextSym1, nextSym2, null);
        }

        protected List<SearchState> prepend(symbol sym, symbol nextSym1,
                symbol nextSym2, Set<lalr_state> guide) {
            List<SearchState> result = new LinkedList<>();
            SearchState ss = this;
            StateItem si1src = ss.states1.get(0);
            StateItem si2src = ss.states2.get(0);
            Set<symbol> si1lookahead =
                    nextSym1 == null
                    ? StateItem.symbolSet(si1src.item.lookahead())
                            : symbolSet(nextSym1);
                    Set<symbol> si2lookahead =
                            nextSym2 == null
                            ? StateItem.symbolSet(si2src.item.lookahead())
                                    : symbolSet(nextSym2);
            Set<StateItem> prev1 =
                    extendedSearch
                                    ? new HashSet<>(si1src.reverseTransition(sym,
                                                                             si1lookahead,
                                                                             guide))
                                                                             : null;
            Set<StateItem> prev2 =
                    extendedSearch
                                            ? new HashSet<>(si2src.reverseTransition(sym,
                                                                                     si2lookahead,
                                                                                     guide))
                                                                                     : null;
                                            List<StateItem> prev1ext =
                                                    si1src.reverseTransition(sym, si1lookahead, extendedSearch
                                                                             ? null : guide);
                                            List<StateItem> prev2ext =
                                                    si2src.reverseTransition(sym, si2lookahead, extendedSearch
                                                                             ? null : guide);
                                            for (StateItem psis1 : prev1ext) {
                                                boolean guided1 = extendedSearch ? prev1.contains(psis1) : true;
                                                StateItem psi1 = psis1 == null ? si1src : psis1;
                                                for (StateItem psis2 : prev2ext) {
                                                    boolean guided2 =
                                                            extendedSearch ? prev2.contains(psis2) : true;
                                                            StateItem psi2 = psis2 == null ? si2src : psis2;
                                                            if (psi1 == si1src && psi2 == si2src) continue;
                                                            if (psi1.state != psi2.state) continue;
                                                            SearchState copy = ss.copy();
                                                            if (psis1 != null) copy.states1.add(0, psis1);
                                                            if (psis2 != null) copy.states2.add(0, psis2);
                                                            if (psis1 != null
                                                                    && copy.states1.get(0).item.dot_pos() + 1 == copy.states1.get(1).item.dot_pos()) {
                                                                if (psis2 != null
                                                                        && copy.states2.get(0).item.dot_pos() + 1 == copy.states2.get(1).item.dot_pos()) {
                                                                    Derivation deriv = new Derivation(sym);
                                                                    copy.derivs1.add(0, deriv);
                                                                    copy.derivs2.add(0, deriv);
                                                                }
                                                                else continue;
                                                            }
                                                            else if (psis2 != null
                                                                    && copy.states2.get(0).item.dot_pos() + 1 == copy.states2.get(1).item.dot_pos()) {
                                                                continue;
                                                            }
                                                            int prependSize =
                                                                    (psis1 == null ? 0 : 1) + (psis2 == null ? 0 : 1);
                                                            int productionSteps =
                                                                    (psis1 == null
                                                                    ? 0
                                                                            : productionSteps(Collections.singletonList(psis1),
                                                                                              si1src))
                                                                                              + (psis2 == null
                                                                                              ? 0
                                                                                                      : productionSteps(Collections.singletonList(psis2),
                                                                                                                        si2src));
                                                            copy.complexity +=
                                                                    UNSHIFT_COST * (prependSize - productionSteps)
                                                                    + PRODUCTION_COST * productionSteps;
                                                            if (!guided1 || !guided2) copy.complexity += EXTENDED_COST;
                                                            result.add(copy);
                                                }
                                            }
            return result;
        }

        protected List<SearchState> reduce1(symbol nextSym) {
            List<StateItem> states = states1;
            List<Derivation> derivs = derivs1;
            int sSize = states.size();
            lalr_item item = states.get(sSize - 1).item;
            if (!item.dot_at_end())
                throw new Error("Cannot reduce item without dot at end.");
            List<SearchState> result = new LinkedList<>();
            Set<symbol> symbolSet =
                    nextSym == null
                    ? StateItem.symbolSet(item.lookahead())
                            : symbolSet(nextSym);
                    if (!StateItem.intersect(item.lookahead(), symbolSet))
                        return result;
                    production prod = item.the_production();
                    symbol lhs = prod.lhs().the_symbol();
                    int len = prod.rhs_length();
                    int dSize = derivs.size();
                    Derivation deriv =
                            new Derivation(lhs, new LinkedList<>(derivs.subList(dSize
                            - len, dSize)));
                    if (reduceDepth == 0) {
                        deriv.deriv.add(itm1.dot_pos(), Derivation.dot);
                        reduceDepth--;
                    }
                    derivs = new LinkedList<>(derivs.subList(0, dSize - len));
                    derivs.add(deriv);
                    if (sSize == len + 1) {
                        // The head StateItem is a production item, so we need to prepend
                        // with possible source StateItems.
                        List<List<StateItem>> prev =
                                states.get(0).reverseProduction(symbolSet);
                        for (List<StateItem> psis : prev) {
                            SearchState copy = copy();
                            copy.derivs1 = derivs;
                            copy.states1 =
                                    new LinkedList<>(states1.subList(0, sSize - len - 1));
                            copy.states1.addAll(0, psis);
                            copy.states1.add(StateItem.trans.get(copy.states1.get(copy.states1.size() - 1))
                                                    .get(lhs));
                            int statesSize = copy.states1.size();
                            int productionSteps =
                                    productionSteps(copy.states1, states.get(0));
                            copy.complexity +=
                                    UNSHIFT_COST * (statesSize - productionSteps)
                                    + PRODUCTION_COST * productionSteps;
                            result.add(copy);
                        }
                    }
                    else {
                        SearchState copy = copy();
                        copy.derivs1 = derivs;
                        copy.states1 =
                                new LinkedList<>(states1.subList(0, sSize - len - 1));
                        copy.states1.add(StateItem.trans.get(copy.states1.get(copy.states1.size() - 1))
                                                .get(lhs));
                        copy.complexity += REDUCE_COST;
                        result.add(copy);
                    }
                    return result;
        }

        protected List<SearchState> reduce2(symbol nextSym) {
            List<StateItem> states = states2;
            List<Derivation> derivs = derivs2;
            int sSize = states.size();
            lalr_item item = states.get(sSize - 1).item;
            if (!item.dot_at_end())
                throw new Error("Cannot reduce item without dot at end.");
            List<SearchState> result = new LinkedList<>();
            Set<symbol> symbolSet =
                    nextSym == null
                    ? StateItem.symbolSet(item.lookahead())
                            : symbolSet(nextSym);
                    if (!StateItem.intersect(item.lookahead(), symbolSet))
                        return result;
                    production prod = item.the_production();
                    symbol lhs = prod.lhs().the_symbol();
                    int len = prod.rhs_length();
                    int dSize = derivs.size();
                    Derivation deriv =
                            new Derivation(lhs, new LinkedList<>(derivs.subList(dSize
                            - len, dSize)));
                    if (shiftDepth == 0)
                        deriv.deriv.add(itm2.dot_pos(), Derivation.dot);
                    derivs = new LinkedList<>(derivs.subList(0, dSize - len));
                    derivs.add(deriv);
                    if (sSize == len + 1) {
                        // The head StateItem is a production item, so we need to prepend
                        // with possible source StateItems.
                        List<List<StateItem>> prev =
                                states.get(0).reverseProduction(symbolSet);
                        for (List<StateItem> psis : prev) {
                            SearchState copy = copy();
                            copy.derivs2 = derivs;
                            copy.states2 =
                                    new LinkedList<>(states.subList(0, sSize - len - 1));
                            copy.states2.addAll(0, psis);
                            copy.states2.add(StateItem.trans.get(copy.states2.get(copy.states2.size() - 1))
                                                    .get(lhs));
                            int statesSize = copy.states2.size();
                            int productionSteps =
                                    productionSteps(copy.states2, states.get(0));
                            copy.complexity +=
                                    SHIFT_COST * (statesSize - productionSteps)
                                    + PRODUCTION_COST * productionSteps;
                            if (copy.shiftDepth >= 0) copy.shiftDepth--;
                            result.add(copy);
                        }
                    }
                    else {
                        SearchState copy = copy();
                        copy.derivs2 = derivs;
                        copy.states2 =
                                new LinkedList<>(states.subList(0, sSize - len - 1));
                        copy.states2.add(StateItem.trans.get(copy.states2.get(copy.states2.size() - 1))
                                                .get(lhs));
                        copy.complexity += REDUCE_COST;
                        if (copy.shiftDepth >= 0) copy.shiftDepth--;
                        result.add(copy);
                    }
                    return result;
        }

        @Override
        public int compareTo(SearchState o) {
            int diff = complexity - o.complexity;
            if (diff != 0) return diff;
            return reductionStreak(states1) + reductionStreak(states2)
                    - (reductionStreak(o.states1) + reductionStreak(o.states2));
        }

        @Override
        public int hashCode() {
            return states1.hashCode() * 31 + states2.hashCode();
        }

        protected boolean equals(SearchState ss) {
            return states1.equals(ss.states1) && states2.equals(ss.states2);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof SearchState) return equals((SearchState) o);
            return false;
        }
    }

    protected static boolean samePrefix(lalr_item itm1, lalr_item itm2) {
        if (itm1.dot_pos() != itm2.dot_pos()) return false;
        production prod1 = itm1.the_production();
        production prod2 = itm2.the_production();
        for (int i = 0, len = itm1.dot_pos(); i < len; i++)
            if (rhs(prod1, i) != rhs(prod2, i)) return false;
        return true;
    }

    protected static int productionSteps(List<StateItem> sis, StateItem last) {
        int count = 0;
        lalr_state lastState = last.state;
        for (ListIterator<StateItem> itr = sis.listIterator(sis.size()); itr.hasPrevious();) {
            StateItem si = itr.previous();
            lalr_state state = si.state;
            if (state == lastState) count++;
            lastState = state;
        }
        return count;
    }

    protected static boolean compatible(symbol sym1, symbol sym2) {
        if (sym1 instanceof terminal) {
            if (sym2 instanceof terminal) return sym1 == sym2;
            non_terminal nt2 = (non_terminal) sym2;
            return nt2.first_set().contains((terminal) sym1);
        }
        else {
            non_terminal nt1 = (non_terminal) sym1;
            if (sym2 instanceof terminal)
                return nt1.first_set().contains((terminal) sym2);
            non_terminal nt2 = (non_terminal) sym2;
            return nt1 == nt2 || nt1.first_set().intersects(nt2.first_set());
        }
    }

    protected static int reductionStreak(List<StateItem> sis) {
        int count = 0;
        StateItem last = null;
        for (ListIterator<StateItem> itr = sis.listIterator(sis.size()); itr.hasPrevious();) {
            StateItem si = itr.previous();
            if (last == null) {
                last = si;
                continue;
            }
            if (si.state != last.state) break;
            count++;
        }
        return count;
    }

    protected static boolean hasCommonPrefix(lalr_item itm1, lalr_item itm2) {
        if (itm1.dot_pos() != itm2.dot_pos()) return false;
        int dotPos = itm1.dot_pos();
        production prod1 = itm1.the_production();
        production prod2 = itm2.the_production();
        for (int i = 0; i < dotPos; i++)
            if (rhs(prod1, i) != rhs(prod2, i)) return false;
        return true;
    }

    protected static Set<symbol> symbolSet(symbol sym) {
        Set<symbol> result = new HashSet<>();
        result.add(sym);
        return result;
    }

    protected static symbol rhs(production prod, int pos) {
        symbol_part sp = (symbol_part) prod.rhs(pos);
        return sp.the_symbol();
    }
}