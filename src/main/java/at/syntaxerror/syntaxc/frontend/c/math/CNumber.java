/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.frontend.c.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import at.syntaxerror.ieee754.Floating;
import at.syntaxerror.syntaxc.frontend.c.type.BinaryFloatType;
import at.syntaxerror.syntaxc.frontend.c.type.FloatType;
import at.syntaxerror.syntaxc.frontend.c.type.IntType;
import at.syntaxerror.syntaxc.frontend.c.type.NumberType;
import at.syntaxerror.syntaxc.frontend.c.type.Types;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class CNumber {

	public static EvalMethod evalMethod = EvalMethod.FDL;
	
	public static CNumber of(IntType type, long value) {
		return new CNumber(type, BigInteger.valueOf(value), null);
	}
	
	public static CNumber of(IntType type, BigInteger value) {
		return new CNumber(type, value, null);
	}

	public static CNumber of(BinaryFloatType<?> type, double value) {
		return new CNumber(type, null, BigDecimal.valueOf(value));
	}

	public static CNumber of(BinaryFloatType<?> type, BigDecimal value) {
		return new CNumber(type, null, value);
	}
	
	@NonNull
	private final NumberType type;
	private final BigInteger intValue;
	private final BigDecimal floatValue;
	
	public static BigDecimal round(BinaryFloatType<?> type, BigDecimal value) {
		return round(evalMethod, type, value);
	}
	
	public static BigDecimal round(EvalMethod eval, FloatType<?> type, BigDecimal value) {
		if(eval == EvalMethod.INFINITE)
			return value;
		
		FloatType<?> evalType = type;
		
		if(type.isFloat()) {
			if(eval == EvalMethod.LLL)
				evalType = Types.LDOUBLE;
			else if(eval == EvalMethod.DDL)
				evalType = Types.DOUBLE;
		}

		if(type.isDouble() && eval == EvalMethod.LLL)
			evalType = Types.LDOUBLE;

		return roundFloat(evalType, value);
	}
	
	private static <T extends Floating<T>> BigDecimal roundFloat(FloatType<T> type, BigDecimal value) {
		var codec = type.getCodec();
		var factory = type.getFactory();
		
		// TODO better way to do this?
		return codec.decode(
			codec.encode(
				factory.create(value)
			)
		).getBigDecimal();
	}
	
	@Override
	public String toString() {
		return (intValue == null ? floatValue : intValue).toString();
	}
	
}
