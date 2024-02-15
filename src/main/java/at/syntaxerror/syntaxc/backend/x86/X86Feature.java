/**
 * Copyright (c) 2024 Thomas Kasper
 * Licensed under the MIT License
 */
package at.syntaxerror.syntaxc.backend.x86;

import static at.syntaxerror.syntaxc.backend.x86.X86FeatureMasks.*;

import java.util.Set;

import lombok.RequiredArgsConstructor;

/**
 * 
 *
 * @author Thomas Kasper
 */
@RequiredArgsConstructor
public enum X86Feature {

	FPU(0, EDX, 0),
	CX8(0, EDX, 8),
	CMOV(0, EDX, 15),
	CLFLUSH(0, EDX, 19),
	MMX(0, EDX, 23),
	FXSR(0, EDX, 24),
	SSE(0, EDX, 25),
	SSE2(0, EDX, 26),
	
	SSE3(0, ECX, 0),
	PCLMULQDQ(0, ECX, 1),
	SSSE3(0, ECX, 9),
	FMA(0, ECX, 12),
	CX16(0, ECX, 13),
	SSE4_1(0, ECX, 19),
	SSE4_2(0, ECX, 20),
	MOVBE(0, ECX, 22),
	POPCNT(0, ECX, 23),
	AES(0, ECX, 25),
	XSAVE(0, ECX, 26),
	AVX(0, ECX, 28),
	F16C(0, ECX, 29),
	RDRND(0, ECX, 30),
	
	FSGSBASE(1, EBX, 0),
	SGX(1, EBX, 2),
	BMI(1, EBX, 3),
	HLE(1, EBX, 4),
	AVX2(1, EBX, 5),
	BMI2(1, EBX, 8),
	ERMS(1, EBX, 9),
	INVPCID(1, EBX, 10),
	RTM(1, EBX, 11),
	AVX512F(1, EBX, 16),
	AVX512DQ(1, EBX, 17),
	RDSEED(1, EBX, 18),
	ADX(1, EBX, 19),
	AVX512IFMA(1, EBX, 21),
	CLFLUSHOPT(1, EBX, 23),
	CLWB(1, EBX, 24),
	PT(1, EBX, 25),
	AVX512PF(1, EBX, 26),
	AVX512ER(1, EBX, 27),
	AVX512CD(1, EBX, 28),
	SHA(1, EBX, 29),
	AVX512BW(1, EBX, 30),
	AVX512VL(1, EBX, 31),
	
	PREFETCHWT1(1, ECX, 0),
	AVX512VBMI(1, ECX, 1),
	PKU(1, ECX, 3),
	WAITPKG(1, ECX, 5),
	AVX512VBMI2(1, ECX, 6),
	GFNI(1, ECX, 8),
	VAES(1, ECX, 9),
	VPCLMULQDQ(1, ECX, 10),
	AVX512VNNI(1, ECX, 11),
	AVX512BITALG(1, ECX, 12),
	AVX512VPOPCNTDQ(1, ECX, 14),
	RDPID(1, ECX, 22),
	KL(1, ECX, 23),
	CLDEMOTE(1, ECX, 25),
	MOVDIRI(1, ECX, 27),
	MOVDIR64B(1, ECX, 28),
	ENQCMD(1, ECX, 29),
	
	AVX5124VNNIW(1, EDX, 2),
	AVX5124FMAPS(1, EDX, 3),
	FSRM(1, EDX, 4),
	UINTR(1, EDX, 5),
	AVX512VP2INTERSECT(1, EDX, 8),
	SERIALIZE(1, EDX, 14),
	TSXLDTRK(1, EDX, 16),
	PCONFIG(1, EDX, 18),
	AMX_BF16(1, EDX, 22),
	AVX512FP16(1, EDX, 23),
	AMX_TILE(1, EDX, 24),
	AMX_INT8(1, EDX, 25),
	
	SHA512(2, EAX, 0),
	SM3(2, EAX, 1),
	SM4(2, EAX, 2),
	RAOINT(2, EAX, 3),
	AVXVNNI(2, EAX, 4),
	AVX512BF16(2, EAX, 5),
	CMPCCXADD(2, EAX, 7),
	FZRM(2, EAX, 10),
	FSRS(2, EAX, 11),
	RSRCS(2, EAX, 12),
	WRMSRNS(2, EAX, 19),
	AMX_FP16(2, EAX, 21),
	HRESET(2, EAX, 22),
	AVXIFMA(2, EAX, 23),
	MSRLIST(2, EAX, 27),
	
	PBNDKB(2, EBX, 1),
	
	X86S(2, ECX, 2),
	
	AVXVNNIINT8(2, EDX, 4),
	AVXNECONVERT(2, EDX, 5),
	AMX_COMPLEX(2, EDX, 8),
	AVXVNNIINT16(2, EDX, 10),
	PREFETCHI(2, EDX, 14),
	USERMSR(2, EDX, 15),
	AVX10(2, EDX, 19),
	APXF(2, EDX, 21),
	
	SYSCALL(3, EDX, 11),
	MMXEXT(3, EDX, 22),
	_3DNOWEXT(3, EDX, 30),
	_3DNOW(3, EDX, 21),
	
	ABM(3, ECX, 5),
	SSE4A(3, ECX, 6),
	_3DNOWPREFETCH(3, ECX, 8),
	XOP(3, ECX, 11),
	LWP(3, ECX, 15),
	FMA4(3, ECX, 16),
	TBM(3, ECX, 21),
	MONITORX(3, ECX, 29),
	
	XSAVEOPT(4, EAX, 0),
	XSAVEC(4, EAX, 1),
	XSAVES(4, EAX, 3),
	
	PTWRITE(5, EBX, 4),

	WIDEKL(6, EBX, 2),
	
	AVX10256(7, EBX, 17),
	AVX10512(7, EBX, 18),
	
	CLZERO(8, EAX, 0),
	WBNOINVD(8, EAX, 9),
	
	;
	
	static {
		// defaults
		
		X86Feature supported[] = {
			FPU, MMX
		};
		
		for(var feature : supported)
			feature.supported = true;
	}
	
	protected static final void listFeatures(Set<X86Feature> features, int page, int eax, int ebx, int ecx, int edx) {
		int[] regs = { eax, ebx, ecx, edx };
		
		for(X86Feature feature : values()) {
			if(feature.page != page)
				continue;
			
			int reg = regs[feature.register];
			
			feature.supported = (reg & (1 << feature.bit)) != 0;
			
			if(feature.supported)
				features.add(feature);
		}
	}

	private final int page;
	private final int register;
	private final int bit;
	
	private boolean supported;
	
	public boolean isSupported() {
		return supported;
	}
	
}
