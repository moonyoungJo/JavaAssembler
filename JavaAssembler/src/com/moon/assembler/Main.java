package com.moon.assembler;

import java.io.FileReader;

public class Main {
	public static void main(String[] args) throws Exception {
		Assembler assembler = new Assembler("data/table6_8.txt");
		assembler.run();
		System.out.println();
		assembler.printLabelResult();
	}
}