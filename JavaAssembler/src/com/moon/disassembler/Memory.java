package com.moon.disassembler;

import java.util.Random;

/* 16bit word를 클래스로 나타내었다.
  	필드배열은 각각의 비트를 나타냅니다. 0또는 1의 값만을 가진다.
	bit[0] -> immediate  1개
	bit[1] ~ [3] -> op  3개
	bit[4] ~ bit[11] -> address  12개
*/
public class Memory {
	private int[] bit = new int[16];

	// 랜덤으로 만들기
	public Memory() {
		// 랜덤한 값 지정 0, 1
		Random r = new Random();

		for (int i = 0; i < bit.length; i++)
			bit[i] = (int) (r.nextDouble() * 2);
	}

	// Assembler에서 data 받아오기
	public Memory(int[] bit) {
		this.bit = bit;
	}

	// 2진수
	public String toBinaryString() {
		StringBuilder binary = new StringBuilder();

		for (int n : bit)
			binary.append(n);

		return binary.toString();
	}

	// 16진수로 바꾸기
	public String toHexString() {
		StringBuilder hex = new StringBuilder();

		for (int i = 0; i < 4; i++) {
			int n = 8 * bit[i * 4] + 4 * bit[1 + i * 4] + 2 * bit[2 + i * 4] + bit[3 + i * 4];
			// 정수를 16진수로바꿔서 char로 바꿈
			hex.append(Character.forDigit(n, 16));
		}

		return hex.toString();
	}

	// 간접주소이면 ture
	public boolean isIndirect() {
		if (bit[0] == 1)
			return true;
		else
			return false;
	}
}