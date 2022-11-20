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
package at.syntaxerror.syntaxc.generator.alloc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import at.syntaxerror.syntaxc.generator.asm.target.AssemblyRegister;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.FreeIntermediate;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.Operand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.ReturnValueOperand;
import at.syntaxerror.syntaxc.intermediate.representation.Intermediate.TemporaryOperand;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.type.Type;
import lombok.Getter;

/**
 * @author Thomas Kasper
 * 
 */
public class RegisterAllocator {
	
	private static final Map<RegisterProvider, CachedSuppliers> CACHE = new HashMap<>();

	private final Map<Long, AssemblyRegister> registerAllocations;
	private final Map<Long, Long> stackAllocations;

	private final Map<AssemblyRegister, RegisterSupplier> registers;
	private final Map<Integer, List<RegisterSupplier>> suppliers;
	
	private final List<Integer> sizes;
	
	private final List<Intermediate> intermediates;
	
	private final List<Interval> intervals;
	private final List<Interval> active;
	
	@Getter
	private long stackOffset;
	
	public RegisterAllocator(RegisterProvider provider, List<Intermediate> intermediates, long stackOffset) {
		registerAllocations = new HashMap<>();
		stackAllocations = new HashMap<>();
		registers = new HashMap<>();
		
		intervals = new ArrayList<>();
		active = new ArrayList<>();
		
		this.intermediates = intermediates;
		this.stackOffset = stackOffset;

		CachedSuppliers cached = CACHE.computeIfAbsent(
			provider,
			CachedSuppliers::makeCached
		);
		
		sizes = cached.sizes();
		suppliers = cached.suppliers();
		
		suppliers
			.values()
			.forEach(
				list -> list.forEach(RegisterSupplier::reset)
			);
		
		findIntervals();
		allocate();
		
		System.out.println();
		System.out.println(registerAllocations);
		System.out.println(stackAllocations);
	}
	
	public Allocation getAllocation(long id) {
		AssemblyRegister register = registerAllocations.get(id);
		
		if(register == null)
			return Allocation.stack(stackAllocations.get(id));
		
		return Allocation.register(register);
	}
	
	public void reserveStack(long size) {
		stackOffset += size;
	}
	
	private void findIntervals() {
		Map<Long, Pair<TemporaryOperand, Long>> pending = new HashMap<>();
		Set<Long> done = new HashSet<>();
		
		long position = 0;
		
		for(Intermediate intermediate : intermediates) {
			++position;
			
			if(intermediate instanceof FreeIntermediate free) {
				if(free.isDiscarded())
					continue;
				
				long id = free.getOperand().getId();
				
				var data = pending.remove(id);
				
				intervals.add(new Interval(
					data.getLeft(),
					data.getRight(),
					position
				));
			}
				
			else for(Operand operand : intermediate.getOperands())
				if(operand instanceof TemporaryOperand temporary) {
					long id = temporary.getId();
					
					if(!done.add(id) || id == ReturnValueOperand.ID)
						continue;
					
					pending.put(id, Pair.of(temporary, position));
				}
			
		}
		
		if(!pending.isEmpty())
			Logger.warn("%d unfreed registers encountered! This is a bug.", pending.size());
		
		for(long id : pending.keySet()) {
			var data = pending.get(id);
			
			intervals.add(new Interval(
				data.getLeft(),
				data.getRight(),
				position
			));
		}
		
		sortByStart(intervals);
	}
	
	private void allocateStack(Interval interval) {
		stackAllocations.put(interval.id(), stackOffset);
		stackOffset += interval.size();
	}
	
	private int findClosestIndex(int size) {
		int low = 0;
		int high = sizes.size();
		
		int mid = -1;
		int val = -1;
		
		while(low < high) {
			mid = (low + high) / 2;
			val = sizes.get(mid);
			
			if(size == val)
				return mid;
			
			else if(size > val)
				low = mid + 1;
			
			else high = mid - 1;
		}
		
		if(mid < 0 || mid > sizes.size())
			return -1;
		
		if(mid > val)
			return mid - 1;
		
		return mid;
	}
	
	private void allocate() {
		
		outer:
		for(Interval interval : intervals) {
			expireOld(interval.start());
			
			int size = interval.size();
			Type type = interval.type();
			
			int index = findClosestIndex(size);
			int max = sizes.size();

			while(index > -1 && index < max) {
				List<RegisterSupplier> suppliers = this.suppliers.get(index);
				
				supply:
				for(RegisterSupplier supplier : suppliers)
					if(supplier.isSuitableFor(type)) {
						AssemblyRegister register = null;
						
						alloc:
						while(true) {
							if(!supplier.hasNext())
								continue supply;
							
							register = supplier.next();
							
							for(AssemblyRegister allocated : registers.keySet())
								if(register.intersects(allocated))
									continue alloc;
							
							break;
						}

						registerAllocations.put(interval.id(), register);
						registers.put(register, supplier);
						active.add(interval);
						continue outer;
					}
				
				++index;
			}
			
			spill(interval);
		}
		
	}
	
	private void expireOld(long pos) {
		sortByEnd(active);
		
		Iterator<Interval> iterator = active.iterator();
		
		while(iterator.hasNext()) {
			Interval interval = iterator.next();
			
			if(interval.end() >= pos)
				return;
			
			iterator.remove();
			
			AssemblyRegister register = registerAllocations.get(interval.id());
			
			active.remove(interval);
			registers.remove(register)
				.expire(register);
		}
		
	}
	
	private void spill(Interval interval) {
		if(active.isEmpty()) {
			allocateStack(interval);
			return;
		}

		int lastIdx = active.size() - 1;
		
		Interval last = active.get(lastIdx);
		
		Interval toStack = interval;
		
		if(last.end() > interval.end()) {
			long id = last.id();
			
			AssemblyRegister register = registerAllocations.get(id);
			
			if(registers.get(register).isSuitableFor(interval.type())) {
				toStack = last;
				
				registerAllocations.remove(id);
				registerAllocations.put(
					interval.id(),
					register
				);
				
				active.remove(lastIdx);
				active.add(interval);
			}
		}

		allocateStack(toStack);
	}
	
	private static void sortByStart(List<Interval> intervals) {
		Collections.sort(intervals, (a, b) -> Long.compare(a.start, b.start));
	}

	private static void sortByEnd(List<Interval> intervals) {
		Collections.sort(intervals, (a, b) -> Long.compare(a.end, b.end));
	}
	
	private static record Interval(TemporaryOperand operand, long start, long end) {
		
		public long id() {
			return operand.getId();
		}

		public int size() {
			return operand.getSize();
		}
		
		public Type type() {
			return operand.getType();
		}
		
	}
	
	private static record CachedSuppliers(List<Integer> sizes, Map<Integer, List<RegisterSupplier>> suppliers) {

		private static CachedSuppliers makeCached(RegisterProvider provider) {
			List<Integer> sizes = new ArrayList<>();
			
			var sizeToSupplier = provider
				.getSuppliers()
				.stream()
				.filter(s -> s != null)
				.<Pair<Integer, RegisterSupplier>>mapMulti(
					(reg, sup) -> reg.getSizes().forEach(
						sz -> sup.accept(
							Pair.of(sz, reg)
						)
					)
				).toList();
			
			
			sizeToSupplier.forEach(elem -> {
				int size = elem.getLeft();
				
				if(!sizes.contains(size)) {
					sizes.add(size);
					;
				}
			});
			
			Collections.sort(sizes);
			
			var suppliers = sizeToSupplier.stream()
				.collect(
					Collectors.groupingBy(
						elem -> sizes.indexOf(elem.getLeft()),
						Collectors.mapping(
							Pair::getRight,
							Collectors.toList()
						)
					)
				);
			
			return new CachedSuppliers(
				Collections.unmodifiableList(sizes),
				Collections.unmodifiableMap(suppliers)
			);
		}
		
	}
	
}
