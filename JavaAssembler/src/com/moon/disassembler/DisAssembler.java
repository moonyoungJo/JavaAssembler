package com.moon.disassembler;

import java.util.HashMap;

import com.moon.util.StoreData;

//16진수를 받아와 심볼로 바꾸는 디셈블러입니다.
public class DisAssembler {
	private Memory data;

	public DisAssembler(Memory d) {
		data = d;
	}

	// 16진수를 심볼로 변환
	public String toSymbol() {
		String hex = data.toHexString();
		char first = hex.charAt(0);
		StringBuilder sym = new StringBuilder();
		HashMap dictionary; // sym을 찾을 사전

		// 명령어 3가지로 분류해서 변환
		if (first != '7' && first != 'f') // memory
		{
			dictionary = StoreData.loadMemoryOpWithHex();

			// 탐색, 심볼 가져오기. memory op는 첫번째자리 hex로 찾는다.
			if (dictionary.containsKey(first))
				sym.append(dictionary.get(first));
			else
				sym.append("nop");

			// 주소 가져오기
			sym.append(" " + hex.substring(1, 4));

			// 간접, 직접 표시
			if (data.isIndirect()) {
				sym.append(" I");
			}
		} else if (first == '7') // register
		{
			dictionary = StoreData.loadRegisterOpWithHex();

			// 탐색. 전체hex로 찾는다.
			if (dictionary.containsKey(hex))
				sym.append(dictionary.get(hex));
		} else if (first == 'f') // io
		{
			dictionary = StoreData.loadIOOpWithHex();

			// 탐색. 전체hex로 찾는다.
			if (dictionary.containsKey(hex))
				sym.append(dictionary.get(hex));
		}

		// 메모리명령어도 아니고 사전에도 없는 경우
		if (sym.length() == 0)
			sym.append("nop");

		return sym.toString();
	}

}