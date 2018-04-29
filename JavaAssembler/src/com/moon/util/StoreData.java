package com.moon.util;

import java.util.HashMap;

/* 명령어 사전
 * 용도에 따라 필요한 리스트를 보낸다.
 * 
 */
public class StoreData {
	// 심볼 간접주소 메모리명령어를 hex로
	public static HashMap loadMemoryIndOpWithSym() {
		HashMap sd = new HashMap();

		sd.put("AND", '8');
		sd.put("ADD", '9');
		sd.put("LDA", 'a');
		sd.put("STA", 'b');
		sd.put("BUN", 'c');
		sd.put("BSA", 'd');
		sd.put("ISZ", 'e');

		return sd;
	}

	// 심볼 직접주소 메모리명령어를 hex로
	public static HashMap loadMemoryDOpWithSym() {
		HashMap sd = new HashMap();

		sd.put("AND", 0);
		sd.put("ADD", 1);
		sd.put("LDA", 2);
		sd.put("STA", 3);
		sd.put("BUN", 4);
		sd.put("BSA", 5);
		sd.put("ISZ", 6);

		return sd;
	}

	// 심볼 레지스터명령어를 hex로
	public static HashMap loadRegisterOpWithSym() {
		HashMap sd = new HashMap();

		sd.put("CLA", "7800");
		sd.put("CLE", "7400");
		sd.put("CMA", "7200");
		sd.put("CME", "7100");
		sd.put("CIR", "7080");
		sd.put("CIL", "7040");
		sd.put("INC", "7020");
		sd.put("SPA", "7010");
		sd.put("SNA", "7008");
		sd.put("SZA", "7004");
		sd.put("SZE", "7002");
		sd.put("HLT", "7001");

		return sd;
	}

	public static HashMap loadIOOpWithSym() {
		HashMap sd = new HashMap();

		sd.put("INP", "F800");
		sd.put("OUT", "F400");
		sd.put("SKI", "F200");
		sd.put("SKO", "F100");
		sd.put("ION", "F080");
		sd.put("IOF", "F040");

		return sd;
	}

	// 16진수로 메모리참조명령어 찾을때 <맨앞 자리로만 찾을 것>
	public static HashMap loadMemoryOpWithHex() {
		HashMap sd = new HashMap();

		sd.put('0', "AND");
		sd.put('8', "AND");
		sd.put('1', "ADD");
		sd.put('9', "ADD");
		sd.put('2', "LDA");
		sd.put('a', "LDA");
		sd.put('3', "STA");
		sd.put('b', "STA");
		sd.put('4', "BUN");
		sd.put('c', "BUN");
		sd.put('5', "BSA");
		sd.put('d', "BSA");
		sd.put('6', "ISZ");
		sd.put('e', "ISZ");

		return sd;
	}

	// 16진수로 레지스터명령어찾을때
	public static HashMap loadRegisterOpWithHex() {
		HashMap sd = new HashMap();

		sd.put("7800", "CLA");
		sd.put("7400", "CLE");
		sd.put("7200", "CMA");
		sd.put("7100", "CME");
		sd.put("7080", "CIR");
		sd.put("7040", "CIL");
		sd.put("7020", "INC");
		sd.put("7010", "SPA");
		sd.put("7008", "SNA");
		sd.put("7004", "SZA");
		sd.put("7002", "SZE");
		sd.put("7001", "HLT");

		return sd;
	}

	// 16진수로 입출력명령어찾을때
	public static HashMap loadIOOpWithHex() {
		HashMap sd = new HashMap();

		sd.put("f800", "INP");
		sd.put("f400", "OUT");
		sd.put("f200", "SKI");
		sd.put("f100", "SKO");
		sd.put("f080", "ION");
		sd.put("f040", "IOF");

		return sd;
	}

}