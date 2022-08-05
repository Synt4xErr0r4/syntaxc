/* MIT License
 * 
 * Copyright (c) 2022 Thomas Kasper
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package at.syntaxerror.syntaxc.parser.node.statement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import at.syntaxerror.syntaxc.parser.node.Node;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class StatementNode extends Node {

	protected static final BinaryOperator<Set<String>> COMBINE = (a, b) -> { a.addAll(b); return a; };

	protected static Set<String> combine(Function<StatementNode, Set<String>> getter, StatementNode...nodes) {
		return Stream.of(nodes)
			.filter(stmt -> stmt != null)
			.map(getter)
			.reduce(new HashSet<>(), COMBINE);
	}
	
	private boolean analyzed;
	
	private Set<String> gotos;
	private Set<String> labels;

	private boolean interrupt;
	
	private void analyze() {
		if(analyzed)
			return;
		
		analyzed = true;
		
		gotos = checkGotos();
		labels = checkLabels();
		
		interrupt = checkInterrupt();
	}
	
	public final boolean doesInterrupt() {
		analyze();
		return interrupt;
	}
	
	public final boolean hasGoto() {
		analyze();
		return !gotos.isEmpty();
	}

	public final boolean hasLabels() {
		analyze();
		return !labels.isEmpty();
	}

	public final Set<String> getGotos() {
		analyze();
		return Collections.unmodifiableSet(gotos);
	}

	public final Set<String> getLabels() {
		analyze();
		return Collections.unmodifiableSet(labels);
	}
	
	protected Set<String> checkGotos() {
		return Set.of();
	}

	protected Set<String> checkLabels() {
		return Set.of();
	}

	protected boolean checkInterrupt() {
		return false;
	}
	
	@Override
	public String getLeafName() {
		return super.getLeafName().replaceFirst("Statement$", "");
	}

}
