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
package at.syntaxerror.syntaxc.generator.alloc.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import at.syntaxerror.syntaxc.SyntaxC;
import at.syntaxerror.syntaxc.generator.alloc.RegisterAllocator;
import at.syntaxerror.syntaxc.generator.arch.Alignment;
import at.syntaxerror.syntaxc.generator.asm.Instructions;
import at.syntaxerror.syntaxc.generator.asm.insn.AssemblyInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.PersistentMemoryInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.RestoreRegistersInstruction;
import at.syntaxerror.syntaxc.generator.asm.insn.StoreRegistersInstruction;
import at.syntaxerror.syntaxc.generator.asm.target.AssemblyTarget;
import at.syntaxerror.syntaxc.generator.asm.target.RegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualRegisterTarget;
import at.syntaxerror.syntaxc.generator.asm.target.VirtualStackTarget;
import at.syntaxerror.syntaxc.logger.Logger;
import at.syntaxerror.syntaxc.misc.Pair;
import at.syntaxerror.syntaxc.type.Type;

/**
 * @author Thomas Kasper
 * 
 */
public abstract class GraphColoringRegisterAllocator extends RegisterAllocator {

	private final Instructions asm;
	
	private StackAllocator allocator;
	
	private long registerId = -1;
	
	private Map<RegisterTarget, List<Long>> registers;
	private Map<Long, LiveInterval> intervals;
	private Map<AssemblyInstruction, Long> positions;
	private Map<Long, Long> coalesced;
	private Map<Long, RegisterTarget> assigned;
	
	private List<PendingSpill> spills;
	
	private int maxDegree;
	
	public abstract List<RegisterSupplier> getRegisterSuppliers();
	
	public abstract boolean isCopy(AssemblyInstruction insn);
	public abstract boolean isBlockEnd(AssemblyInstruction insn);
	
	public abstract AssemblyInstruction store(Instructions asm, VirtualStackTarget target, RegisterTarget register);
	public abstract AssemblyInstruction restore(Instructions asm, RegisterTarget register, VirtualStackTarget target);
	
	public GraphColoringRegisterAllocator(Instructions asm, Alignment alignment) {
		this.asm = asm;
		
		allocator = new StackAllocator(alignment);
		
		registers = new HashMap<>();
		intervals = new HashMap<>();
		positions = new HashMap<>();
		coalesced = new HashMap<>();
		assigned = new HashMap<>();
		
		spills = new ArrayList<>();
	}
	
	@Override
	public List<RegisterTarget> getAssignedRegisters() {
		return new ArrayList<>(assigned.values());
	}
	
	@Override
	public long getStackSize() {
		return allocator.getStackSize();
	}
	
	@Override
	public void allocate() {
		assigned.clear();
		
		getRegisterSuppliers().forEach(this::allocateRegisters);
		
		replaceVirtualRegisters();
		replaceStoreRestore();
		allocateStackMemory();
	}
	
	private void replaceStoreRestore() {
		Map<Long, Map<RegisterTarget, VirtualStackTarget>> locations = new HashMap<>();

		var assigned = getAssignedRegisters();
		
		for(AssemblyInstruction insn : asm)
			if(insn instanceof StoreRegistersInstruction store) {
				long id = store.getId();
				
				Map<RegisterTarget, VirtualStackTarget> registers = new HashMap<>();
				
				for(AssemblyTarget target : store.getSources()) {
					RegisterTarget register = (RegisterTarget) target;
					
					if(!assigned.stream().anyMatch(register::intersects))
						continue;
					
					VirtualStackTarget stack = new VirtualStackTarget(register.getType());
					
					store.insertAfter(store(asm, stack, register));
					
					registers.put(register, stack);
				}
				
				locations.put(id, registers);
				store.remove();
			}
			else if(insn instanceof RestoreRegistersInstruction restore) {
				long id = restore.getId();

				if(!locations.containsKey(id)) {
					restore.remove();
					continue;
				}

				Map<RegisterTarget, VirtualStackTarget> registers = locations.get(id);
				
				for(Map.Entry<RegisterTarget, VirtualStackTarget> entry : registers.entrySet())
					restore.insertAfter(restore(asm, entry.getKey(), entry.getValue()));
				
				restore.remove();
			}
	}
	
	private void checkStackMemory(List<AssemblyTarget> targets, Map<Long, Long> lastUse, long pos) {
		for(int i = 0; i < targets.size(); ++i)
			if(targets.get(i) instanceof VirtualStackTarget virtual) {
				long id = virtual.getId();
				
				if(lastUse.getOrDefault(id, -1L) < pos)
					lastUse.put(id, pos);
			}

		targets.stream()
			.filter(target -> target != null)
			.map(AssemblyTarget::getNestedTargets)
			.filter(list -> list != null && !list.isEmpty())
			.forEach(list -> checkStackMemory(list, lastUse, pos));
	}

	private void allocateStackMemory(List<AssemblyTarget> targets) {
		for(int i = 0; i < targets.size(); ++i)
			if(targets.get(i) instanceof VirtualStackTarget virtual)
				targets.set(
					i,
					resolveVirtualMemory(
						allocator.allocate(
							virtual.getId(),
							virtual.getType()
						),
						virtual.getType()
					)
				);
		
		targets.stream()
			.filter(target -> target != null)
			.map(AssemblyTarget::getNestedTargets)
			.filter(list -> list != null && !list.isEmpty())
			.forEach(this::allocateStackMemory);
	}
	
	private void allocateStackMemory() {
		Map<Long, Long> lastUse = new HashMap<>();
		Map<Long, List<Long>> positions = new HashMap<>();

		long pos = 0;
		
		for(AssemblyInstruction insn : asm) {
			
			if(insn instanceof PersistentMemoryInstruction persistent) {
				lastUse.put(persistent.getTarget().getId(), Long.MAX_VALUE);
				persistent.remove();
				continue;
			}
			
			checkStackMemory(insn.getSources(), lastUse, pos);
			checkStackMemory(insn.getDestinations(), lastUse, pos);
			++pos;
		}
		
		lastUse.forEach(
			(target, position) -> positions.computeIfAbsent(
				position,
				p -> new ArrayList<>()
			).add(target)
		);
		
		pos = 0;
		
		for(AssemblyInstruction insn : asm) {
			allocateStackMemory(insn.getSources());
			
			positions.getOrDefault(pos++, List.of())
				.forEach(allocator::free);
			
			allocateStackMemory(insn.getDestinations());
		}
	}
	
	private void replaceVirtualRegisters(List<AssemblyTarget> targets) {
		for(int i = 0; i < targets.size(); ++i)
			if(targets.get(i) instanceof VirtualRegisterTarget virtual) {
				
				long id = virtual.getId();
				
				if(!assigned.containsKey(id))
					Logger.softError("Failed to allocate physical register to virtual register %d. This is a bug.", id);
				
				else targets.set(i, assigned.get(id).resized(virtual.getType()));
			}

		targets.stream()
			.filter(target -> target != null)
			.map(AssemblyTarget::getNestedTargets)
			.filter(list -> list != null && !list.isEmpty())
			.forEach(this::replaceVirtualRegisters);
		
		SyntaxC.checkTerminationState();
	}
	
	private void replaceVirtualRegisters() {
		for(AssemblyInstruction insn : asm) {
			replaceVirtualRegisters(insn.getSources());
			replaceVirtualRegisters(insn.getDestinations());
		}
	}
	
	private void allocateRegisters(RegisterSupplier supplier) {
		if(supplier == null)
			return;
		
		maxDegree = supplier.getRegisterCount();
		
		do {
			registerId = 0;
			
			registers.clear();
			intervals.clear();
			positions.clear();
			spills.clear();
			coalesced.clear();
			
			findLiveRanges(supplier);
			
			do buildGraph();
			while(coalesce());
			
			if(spills.isEmpty()) {
				color(supplier);
				break;
			}
			
			estimateSpillCost();
			spill();
			break;
		} while(true);
	}
	
	private void updateInterval(RegisterSupplier supplier, long pos, long intervalId, Type type, RegisterTarget reg) {
		if(!supplier.isSuitableFor(type))
			return;
		
		intervals.compute(
			intervalId,
			(id, live) -> {
				if(live == null)
					live = new LiveInterval(pos, pos);
				else live.setTo(pos);
				
				if(reg != null)
					live.setAssignedRegister(reg);
				
				return live;
			}
		);
	}
	
	private void initLiveRanges(RegisterSupplier supplier, long pos, boolean copy, List<AssemblyTarget> targets) {
		List<RegisterTarget> registers = supplier.getRegisters();
		
		for(AssemblyTarget target : targets)
			if(target instanceof VirtualRegisterTarget reg)
				updateInterval(supplier, pos, reg.getId(), reg.getType(), null);
		
			else if(target instanceof RegisterTarget reg) {
				
				if(!registers.stream().anyMatch(reg::intersects))
					continue;
				
				List<Long> ids = this.registers.computeIfAbsent(reg, r -> new ArrayList<>());
				
				long id;
				
				if(copy || ids.isEmpty()) {
					id = --registerId;
					ids.add(id);
				}
				else id = ids.get(ids.size() - 1);
				
				updateInterval(supplier, pos, id, reg.getType(), reg);
			}
		
		targets.stream()
			.filter(target -> target != null)
			.map(AssemblyTarget::getNestedTargets)
			.filter(list -> list != null && !list.isEmpty())
			.forEach(list -> initLiveRanges(supplier, pos, copy, list));
	}
	
	private void findLiveRanges(RegisterSupplier supplier) {
		long pos = 0;
		
		for(AssemblyInstruction insn : asm) {
			if(insn instanceof RestoreRegistersInstruction ||
				insn instanceof StoreRegistersInstruction)
				continue;
			
			positions.put(insn, pos);
			
			initLiveRanges(supplier, pos, false, insn.getSources());
			initLiveRanges(supplier, pos, isCopy(insn), insn.getDestinations());
			
			++pos;
		}
	}
	
	private void buildGraph() {
		List<Long> keys = new ArrayList<>(intervals.keySet());
		
		intervals.values().forEach(interval -> interval.getInterference().clear());
		
		int count = keys.size();
		
		spills.clear();
		
		for(int i = 0; i < count; ++i) {
			long key = keys.get(i);
			
			LiveInterval live = intervals.get(key);
			
			for(int j = i + 1; j < count; ++j) {
				long otherKey = keys.get(j);
				
				LiveInterval other = intervals.get(otherKey);
				
				if(live.interferesWith(other)) {
					live.getInterference().add(otherKey);
					other.getInterference().add(key);
				}
			}

			if(key < 0)
				continue;
			
			Set<Long> intervals = new HashSet<>(live.getInterference());
			intervals.add(key);
			
			Set<LiveInterval> liveIntervals = intervals.stream()
				.map(this.intervals::get)
				.collect(Collectors.toSet());
			
			long size = liveIntervals.stream()
				.filter(LiveInterval::isUnassigned)
				.mapToLong(
					interval -> liveIntervals.stream()
						.filter(interval::interferesWith)
						.count()
				)
				.max()
				.orElse(0);
			
			long spillCount = size - maxDegree;
			
			if(spillCount > 0)
				spills.add(new PendingSpill(
					intervals.stream()
						.filter(id -> this.intervals.get(id).isUnassigned())
						.collect(Collectors.toSet()),
					spillCount
				));
		}
	}
	
	private boolean tryCoalesce(long id, LiveInterval interval) {
		RegisterTarget intervalRegister = interval.getAssignedRegister();
		
		List<Pair<Long, RegisterTarget>> interferingRegisters = interval.getInterference()
			.stream()
			.filter(intervals::containsKey)
			.map(key -> Pair.of(key, intervals.get(key).getAssignedRegister()))
			.filter(pair -> pair.getRight() != null)
			.toList();
		
		for(long key : interval.getInterference()) {
			if(coalesced.containsKey(key))
				continue;
			
			LiveInterval live = intervals.get(key);
			
			if(interval.getTo() != live.getFrom() || interval.getFrom() == live.getFrom())
				continue;
			
			RegisterTarget liveRegister = live.getAssignedRegister();
			
			if(intervalRegister != null && liveRegister != null && !intervalRegister.intersects(liveRegister))
				continue;

			if(liveRegister == null)
				liveRegister = intervalRegister;
			
			if(liveRegister != null) {
				
				RegisterTarget register = liveRegister;
				
				if(interferingRegisters.stream().anyMatch(
					pair -> pair.getLeft() != key
						&& pair.getRight().intersects(register))
				) continue;
			}
			
			coalesced.put(key, id);
			intervals.remove(key);

			tryCoalesce(key, live);
			
			interval.setTo(live.getTo());
			
			interval.setAssignedRegister(liveRegister);
			
			return true;
		}
		
		return false;
	}
	
	private boolean coalesce() {
		List<Long> keys = new ArrayList<>(intervals.keySet());
		
		boolean didCoalesce = false;
		
		int count = keys.size();
		
		for(int i = 0; i < count; ++i) {
			long id = keys.get(i);
			
			if(coalesced.containsKey(id))
				continue;
			
			didCoalesce |= tryCoalesce(id, intervals.get(id));
		}

		return didCoalesce;
	}
	
	private long resolveCoalescedId(long id) {
		while(coalesced.containsKey(id))
			id = coalesced.get(id);
		
		return id;
	}
	
	private void color(RegisterSupplier supplier) {
		List<RegisterTarget> available;
		RegisterTarget register;
		
		for(Map.Entry<Long, LiveInterval> entry : intervals.entrySet()) {
			LiveInterval interval = entry.getValue();
			
			if(interval.isUnassigned()) {
				available = new ArrayList<>(supplier.getRegisters());
				
				for(long interfering : interval.getInterference()) {
					LiveInterval live = intervals.get(interfering);
					
					if(live.isAssigned())
						available.removeIf(live.getAssignedRegister()::intersects);
				}
				
				if(available.isEmpty())
					continue;
				
				register = available.get(0);
				
				interval.setAssignedRegister(register);
			}
			else register = interval.getAssignedRegister();
			
			assigned.put(entry.getKey(), register);
		}
		
		for(Map.Entry<Long, Long> entry : coalesced.entrySet())
			assigned.put(
				entry.getKey(),
				intervals.get(resolveCoalescedId(entry.getValue()))
					.getAssignedRegister()
			);
	}
	
	private boolean areSpillsSatisfied(List<Long> toBeSpilled) {
		return spills.stream()
			.allMatch(pending ->
				pending.intervals()
					.stream()
					.map(this::resolveCoalescedId)
					.filter(toBeSpilled::contains)
					.count() >= pending.count()
			);
	}
	
	private void estimateSpillCost() {
		Map<Long, Long> spillCounts = spills.stream()
			.map(PendingSpill::intervals)
			.flatMap(Set::stream)
			.collect(Collectors.toMap(
				this::resolveCoalescedId,
				v -> 1L,
				Long::sum
			));
		
		List<Long> toBeSpilled = new ArrayList<>();
		
		Stack<Long> pending = spillCounts
			.entrySet()
			.stream()
			.map(e -> Pair.of(e, intervals.get(e.getKey())))
			.sorted((a, b) -> b.getRight().compareTo(a.getRight()))
			.mapToLong(p -> p.getLeft().getKey())
			.collect(
				Stack::new,
				Stack::add,
				Stack::addAll
			);
		
		boolean skip;
		
		do {
			if(pending.empty())
				Logger.error("Spill estimation failed");
			
			long id = pending.pop();
			
			toBeSpilled.add(id);
			
			skip = false;
		} while(skip || !areSpillsSatisfied(toBeSpilled));
		
		// TODO
		
		System.out.println(spills);
		System.out.println(toBeSpilled);
		System.out.println(
			toBeSpilled.stream()
				.map(intervals::get)
				.map(LiveInterval::getInterference)
				.toList()
		);
		
		Logger.error("Failed to allocate registers without spilling");
	}
	
	private void spill() {
		// TODO
	}
	
	private static record PendingSpill(Set<Long> intervals, long count) {
		
	}
	
	@SuppressWarnings("unused")
	private static record EstimatedSpill(LiveInterval interval, List<LiveInterval> spillIntervals,
			long useCount, long definitionCount, long spillCount) {
		
	}
	
}
