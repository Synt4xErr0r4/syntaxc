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
package at.syntaxerror.syntaxc.parser.node.declaration;

import java.util.List;

import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.parser.node.Node;
import at.syntaxerror.syntaxc.tracking.Position;
import at.syntaxerror.syntaxc.tracking.Positioned;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author Thomas Kasper
 * 
 */
@Getter
@RequiredArgsConstructor
@ToString(exclude = "position")
public class Declarator extends Node {

	private final Position position;

	private final Pair<String, Declarator> nameOrNested;
	private final List<Pointer> pointers;
	private final List<DeclaratorPostfix> postfixes;
	
	public String getName() {
		return nameOrNested.hasFirst()
			? nameOrNested.getFirst()
			: nameOrNested.getSecond().getName();
	}
	
	public Type merge(Positioned pos, Type type) {
		for(Pointer pointer : pointers) {
			type = type.addressOf();
			
			if(pointer.isConst()) type = type.asConst();
			if(pointer.isVolatile()) type = type.asVolatile();
		}
		
		for(DeclaratorPostfix postfix : postfixes)
			type = postfix.applyTo(pos, type);
		
		return nameOrNested.hasSecond()
			? nameOrNested.getSecond().merge(pos, type)
			: type;
	}
	
}
