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
package at.syntaxerror.syntaxc.intermediate.representation;

import java.util.List;
import java.util.function.Supplier;

import at.syntaxerror.syntaxc.intermediate.operand.Operand;
import at.syntaxerror.syntaxc.tracking.Position;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Intermediate representation of function calls ('fun(a, b, c)')
 * 
 * @author Thomas Kasper
 * 
 */
@AllArgsConstructor
@Getter
public class CallIntermediate extends Intermediate {

	private Position position;

	private Operand target;
	private Operand function;
	private List<Operand> arguments;

	@Override
	public void withResult(Operand operand) {
		target = operand;
	}
	
	@Override
	public String toStringInternal() {
		return toTargetString(target)
			+ "%s(%s);".formatted(
				function,
				arguments.stream()
					.map(Operand::toString)
					.reduce((a, b) -> a + ", " + b)
					.orElse("")
			);
	}
	
	@RequiredArgsConstructor
	public static class CallStartIntermediate extends Intermediate {
		
		private final Supplier<CallIntermediate> callSupplier;
		
		@Getter
		private CallIntermediate call;
		
		public void finish() {
			call = callSupplier.get();
		}
		
		@Override
		public Position getPosition() {
			return call.getPosition();
		}

		@Override
		public void withResult(Operand operand) { }
		
		@Override
		protected String toStringInternal() {
			return "/*synthetic*/ /*begin function call*/";
		}
		
	}
	
	@RequiredArgsConstructor
	@Getter
	public static class CallParameterIntermediate extends Intermediate {

		@Getter(AccessLevel.NONE)
		private final Supplier<CallIntermediate> callSupplier;
		
		private final int index;
		private final Operand operand;
		
		private CallIntermediate call;
		
		public void finish() {
			call = callSupplier.get();
		}

		@Override
		public Position getPosition() {
			return call.getPosition();
		}

		@Override
		public void withResult(Operand operand) { }
		
		@Override
		protected String toStringInternal() {
			return "/*synthetic*/ /*function call operand %d*/".formatted(index);
		}
		
	}
	
}
