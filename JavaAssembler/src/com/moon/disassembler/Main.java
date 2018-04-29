package com.moon.disassembler;

public class Main {
	public static void main(String args[]) {
		// 20개 워드 생성
		Memory[] data = new Memory[20];

		System.out.printf("%-13s %-15s %10s %25s\n\n", "Location", "instruction", "Hex", "binary");

		for (int i = 0; i < 20; i++) {
			data[i] = new Memory();
			DisAssembler ds = new DisAssembler(data[i]);

			System.out.printf("%-13d %-15s %10s %25s\n", i, ds.toSymbol(), data[i].toHexString(),
					data[i].toBinaryString());
		}

	}
}
