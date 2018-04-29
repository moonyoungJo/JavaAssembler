package com.moon.assembler;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;

import com.moon.util.StoreData;

/* 사용법 :
 * 어셈블러입니다.
 * txt파일로 생성해서, firstPath와 secondPath를 한 뒤
 * 첨부된 코드와 합쳐서 동작합니다.
 */

public class Assembler {
	// 파일읽어와서 배열로
	private File file;
	private StringBuilder[] stringCode;

	// 주소-심볼테이블
	private HashMap<String, Integer> addressSymbolTable = new HashMap();
	// 메모리에 저장_4096개의 워드
	private short[] memory = new short[4096];

	// 메모리, 레지스터<첨부된 코드를 이용하기위해추가>
	private short PC, DR, AR, AC, IR, INPR, OUTPR, TR, SC, indirection, head, I, E, S, D7, INpR, FGI, OUTR, FGO, IEN;
	private String symbol;

	// 파일읽고 String배열로. 주석,공백제거
	public Assembler(String fileName) {
		file = new File(fileName);
		FileReader fr;
		try {
			fr = new FileReader(fileName);

			// 줄수세기
			int count = 0;
			boolean isPrevChangeLine = true; // 줄의 시작부터 주석일 경우 대비

			int read = fr.read();
			while (read != -1) {
				if (read == '\n') {
					count++;
					isPrevChangeLine = true;
				} else {
					// 줄의 시작부터 주석일때, 하나의 줄로 인정하지 않는다.
					if (read == '/' && isPrevChangeLine)
						count--;
					isPrevChangeLine = false;
				}
				read = fr.read();
			}
			count++;
			fr.close();

			// 코드들만 String[]으로 저장
			stringCode = new StringBuilder[count];
			fr = new FileReader(fileName);

			// 줄채우기
			read = fr.read();
			for (int i = 0; i < stringCode.length; i++) {
				stringCode[i] = new StringBuilder();

				// 간접수소 I판단을 위한 3개의 변수
				int lastThree = 0; // 마지막에서 3번째로 읽은 것
				int lastTwo = 0; // 마지막에서 2번째로 읽은 것
				int lastOne = 0; // 마지막에서 1번째로 읽은 것

				// 주석관리를위한 2개의 변수
				boolean isFirstLetter = true;
				boolean isSave = true; // false이면 주석이니 저장x

				// txt파일의 줄바꿈문자는 \r\n, 한줄을 읽는 코드
				while (read != '\r') {
					if (read == -1) // 문서의 끝
						break;
					else if (isFirstLetter && read == '/') // 줄시작부터 주석
					{
						i--; // 줄 수를 증가시키지 않는다.
						isFirstLetter = false;
						isSave = false;
					} else if (!isFirstLetter && read == '/') // 중간부터 주석시작
						isSave = false;
					else // 명령어문자
					{
						if (isFirstLetter)
							isFirstLetter = false;

						if (isSave && (read != ' ' && read != 9)) // 공백은 무시
							stringCode[i].append((char) read); // string에 한글자 추가
					}

					read = fr.read();
					lastThree = lastTwo;
					lastTwo = lastOne;
					lastOne = read;

					// I판단(I는 공백으로 구분한다)
					if (lastThree == ' ' && lastTwo == 'I' && lastOne == ' ') // 중간에
																				// I나오는경우
						stringCode[i].deleteCharAt(i).append(" I");
					else {
						// 줄끝에 I나오는 경우
						if (lastThree == ' ' && lastTwo == 'I') {
							stringCode[i].deleteCharAt(stringCode[i].length() - 1).append(" I");
						}
					}
				}

				// 탈출,END
				if (read == -1)
					break;

				read = fr.read(); // 공백문자는\r\n이므로 안읽힌 \n을 마저 읽는다
				read = fr.read();
			}
		} catch (Exception e) {
			System.out.println("Assembler객체의 파일받기 오류\n");
			e.printStackTrace();
		}
	}

	// 한번에 수행
	public void run() {
		firstPath();
		secondPath();
		printCycle();
	}

	// 공백과 주석을 제거한 문자열출력
	public void printStringCode() {
		System.out.println("공백과 주석을 삭제한 string코드");

		for (int i = 0; i < stringCode.length; i++)
			System.out.println(stringCode[i]);
	}

	// 주소-심볼테이블 제작
	public void firstPath() {
		int LC = 0; // 주소, 16진수
		int lineNum = 0; // 읽을 줄

		while (true) {
			int commaIndex = stringCode[lineNum].indexOf(",");

			// 라벨일때, 주소-심볼테이블제작
			if (commaIndex != -1) {
				String symbol = stringCode[lineNum].substring(0, commaIndex);
				int address = LC;
				addressSymbolTable.put(symbol, address);

			}
			// 슈도명령어일때
			else {
				String pseudo = stringCode[lineNum].substring(0, 3);

				if (pseudo.equals("ORG")) {
					String sNum = stringCode[lineNum].substring(3);
					LC = stringHexToDeci(sNum);
					lineNum++;
					continue;
				} else if (pseudo.equals("END"))
					break;
			}
			lineNum++;
			LC++;
		}
	}

	// 주소-심볼테이블출력
	public void printAddressSymbolTable() {
		System.out.println("AddressSymbolTable(Adress는 16진수입니다)");
		System.out.println("===========================================");

		// 반복자를 이용해서 출력
		Iterator<String> iterator = addressSymbolTable.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next(); // 키 얻기
			System.out.println(key + "  |   " + Integer.toHexString(addressSymbolTable.get(key))); // 출력
		}
	}

	// 메모리에 저장
	public void secondPath() {
		int LC = 0; // 주소
		int lineNum = 0; // 읽을 줄
		boolean isFirst = true; // 시작할 pc값 저장하기 위해

		// 명령어 바꾸기위한 백과사전
		// 간접주소메모리참조, 직접주소메모리참조, 레지스터, 입출력
		HashMap<String, Character> IMRI = StoreData.loadMemoryIndOpWithSym();
		HashMap<String, Character> MRI = StoreData.loadMemoryDOpWithSym();
		HashMap<String, String> RGI = StoreData.loadRegisterOpWithSym();
		HashMap<String, String> OOI = StoreData.loadIOOpWithSym();

		while (true) {
			String code = stringCode[lineNum].toString();

			// 심볼일때
			if (code.indexOf("HEX") != -1) {
				int numIndex = code.indexOf("HEX") + 3;
				String sNum = code.substring(numIndex);

				int tmp = Integer.parseInt(sNum, 16);
				memory[LC] = (short) (tmp & 0xffff);
			} else if (code.indexOf("DEC") != -1) {
				int numIndex = code.indexOf("DEC") + 3;
				String sNum = code.substring(numIndex);
				memory[LC] = Short.parseShort(sNum);

			} else if (code.indexOf("ORG") != -1) {
				// LC값 변경
				LC = stringHexToDeci(code.substring(3));

				// 첫번째 나오는 ORG일 경우 PC값으로 설정
				if (isFirst) {
					PC = (short) LC;
					isFirst = false;
				}
				LC--; // 반복문끝에서 1더하므로
			} else if (code.indexOf("END") != -1)
				break;

			// 명령어 일 때
			else {
				// 명령어 string찾기
				String op;
				int labelIndex; // 피연산자 라벨인덱스

				// 명령어에 라벨 붙어있을 때
				// ex) LOP, ADD PTR I
				if (code.indexOf(",") != -1) {
					int comIndex = code.indexOf(",");
					op = code.substring(comIndex + 1, comIndex + 4);
					labelIndex = comIndex + 4;
				} else {
					op = code.substring(0, 3);
					labelIndex = 3;
				}

				// 메모리참조 명령어일때
				if (MRI.containsKey(op)) {
					String label = code.substring(labelIndex);
					int value;
					// 간접
					if (code.indexOf(" I") != -1) {
						int blankIndex = code.indexOf(" ");

						label = code.substring(labelIndex, blankIndex); // 순수한
																		// 라벨 얻기
						value = Integer.parseInt((IMRI.get(op) + "000"), 16); // op값
																				// 넣기(맨앞자리)
						value += addressSymbolTable.get(label); // 피연산자주소값 찾아서
																// 넣기(뒤 3자리)
					}
					// 직접
					else {
						label = code.substring(3); // 라벨얻기
						value = Integer.parseInt((MRI.get(op) + "000"), 16); // op값
																				// 넣기
						value += addressSymbolTable.get(label); // 피연산자 주소 넣기
					}
					memory[LC] = (short) (value & 0xffff);
				}
				// 입출력 명령어 또는 레지스터 명령어일 경우
				else {
					int value;

					// 레지스터명령어
					if (RGI.containsKey(op))
						value = Integer.parseInt(RGI.get(op), 16);
					// 입출력명령어
					else
						value = Integer.parseInt(OOI.get(op), 16);

					memory[LC] = (short) (value & 0xffff);
				}
			}

			// System.out.println("memory[" + Integer.toHexString(LC) + "] : " +
			// Integer.toHexString(memory[LC]));
			LC++;
			lineNum++;
		}
	}

	// 라벨의 값을 프린트
	public void printLabelResult() {
		System.out.println("라벨의 결과");
		System.out.printf("%4s %4s %12s %12s\n", "주소(16진수)", "라벨", "결과(10진수)", "결과(16진수)");
		System.out.println("========================================");

		Iterator<String> iterator = addressSymbolTable.keySet().iterator();
		// 반복자를 이용해서 출력
		while (iterator.hasNext()) {
			String key = (String) iterator.next(); // 키 얻기
			int address = addressSymbolTable.get(key);
			String hex = Integer.toHexString(memory[address]);

			if (hex.length() >= 4)
				hex = hex.substring(hex.length() - 4, hex.length());

			System.out.printf("%5s %12s %10d %12s\n", Integer.toHexString(address), key, memory[address], hex); // 출력
		}
		System.out.println("========================================");
	}

	// 16진수 문자열을 10진수 int로
	private int stringHexToDeci(String s) {
		return Integer.parseInt(s, 16);
	}

	// 첨부된 코드, 몇가지 수정
	private String symbolCheck(short a) { // instruction 값인지 체크해 심볼을 문자열로 반환하는
											// 메소드
		// instruction 값인지 체크해 심볼을 문자열로 반환하는 메소드

		// 코드 수정head = (short) ((short) a / 0x1000) -> 맨앞수가 8이상일때 문제가 생긴다
		head = (short) (((short) a & 0xf000) / 0x1000);

		// 16진수의 맨 앞을 얻음 ex) 0x1234 이면 head = 1
		D7 = 0;

		indirection = (short) (head / 8);
		// indirect bit 를 얻음 ex) 0~7로 시작하면 0, 8~f로 시작하면 1

		symbol = "nop";
		// symbol을 nop 으로 설정한 후 조건에 따라 바꾼다.

		// String address = Integer.toHexString(a + 0x10000).substring(2) ->
		// 우리과제는 주소가 2자리 이상이므로 수정
		String address = Integer.toHexString(a & 0x0fff);
		// 주소값을 미리 넣는다.

		if (head == 7) { // 7xxx
			address = "   "; // 주소를 없앤다
			D7 = 1;

			switch (a & 0x0FFF) { // 뒷자리를 비교한다.
			case 0x800:
				symbol = "CLA";
				break;
			case 0x400:
				symbol = "CLE";
				break;
			case 0x200:
				symbol = "CMA";
				break;
			case 0x100:
				symbol = "CME";
				break;
			case 0x80:
				symbol = "CIR";
				break;
			case 0x40:
				symbol = "CIL";
				break;
			case 0x20:
				symbol = "INC";
				break;
			case 0x10:
				symbol = "SPA";
				break;
			case 0x8:
				symbol = "SNA";
				break;
			case 0x4:
				symbol = "SZA";
				break;
			case 0x2:
				symbol = "SZE";
				break;
			case 0x1:
				symbol = "HLT";
				break;
			}
		} else if (head == 0xf) { // fxxx
			address = "   "; // 주소를 없앤다
			D7 = 1;

			switch (a & 0x0FFF) { // 뒷자리를 비교한다.
			case 0x800:
				symbol = "INP";
				break;
			case 0x400:
				symbol = "OUT";
				break;
			case 0x200:
				symbol = "SKI";
				break;
			case 0x100:
				symbol = "SKO";
				break;
			case 0x80:
				symbol = "ION";
				break;
			case 0x40:
				symbol = "IOF";
				break;
			}
		} else {

			switch (head % 8) { //
			case 0: // (a=0xxx, a=8xxx)
				symbol = "AND";
				break;
			case 1: // (1xxx, 9xxx)
				symbol = "ADD";
				break;
			case 2: // (2xxx, Axxx)
				symbol = "LDA";
				break;
			case 3: // (3xxx, Bxxx)
				symbol = "STA";
				break;
			case 4:
				symbol = "BUN";
				break;
			case 5:
				symbol = "BSA";
				break;
			case 6:
				symbol = "ISZ";
				break;
			}
			// if (indirection == 1)
			// symbol = "I " + symbol;
		}

		// 수정, 직접간접주소는 여기서 판정해야 밑에서 오류가 생기지 않는다.
		if (indirection == 1)
			return "I " + symbol + address;
		else
			return symbol + " " + address;

	}

	private void start() { // 시작 메소드
		SC = 0;
	}

	private void T0() { // T0 일 때
		SC++;
		AR = (short) PC;
		System.out.println("T0 : ");
		System.out.println("AR <- PC");
		System.out.println("AR = " + Integer.toHexString(AR + 0x10000).substring(2));
		System.out.println();
	}

	private void T1() { // T1 일 때
		SC++;
		IR = memory[AR];
		PC = (short) (PC + 1);
		System.out.println("T1 : ");
		System.out.println("IR <- memory[AR], PC <- PC + 1");
		// 수정
		System.out.println("IR = " + Integer.toHexString(memory[AR] + 0xffff0000).substring(4) + ", PC = "
				+ Integer.toHexString(PC + 0x10000).substring(2));
		System.out.println();
	}

	private void T2() { // T2 일 때
		SC++;
		symbol = symbolCheck(memory[AR]);
		AR = (short) (IR & 0x0fff);
		I = indirection;

		System.out.println("T2 : ");
		System.out.println("Decode operation code in IR(12-14)");
		System.out.println("AR <- IR(0-11), I <- IR(15)");
		System.out.println("AR = " + Integer.toHexString(AR + 0x10000).substring(2) + ", I = " + I);
		System.out.println("D7 = " + D7);
		System.out.println();
	}

	private void instructionCheck() throws HaltException { // 인스트럭션 체크하고 명령어에 따라
															// T3, T4, T5 ... 할일
															// 결정
		System.out.println("T3 : ");
		System.out.println("instruction : " + symbolCheck(IR));
		if (head == 7) { // D7 = 1 이고, I = 0 인경우
			System.out.println("Excute register-reference instruction");
			switch (symbol) {
			case "CLA":
				AC = 0;
				System.out.println("AC <- 0");
				System.out.println("AC = " + AC);
				System.out.println("AC = " + Integer.toHexString(AC + 0x10000).substring(1));
				break;
			case "CLE":
				E = 0;
				System.out.println("E <- 0");
				System.out.println("E = " + E);
				System.out.println();
				break;
			case "CMA":
				AC = (short) ~(short) AC;
				System.out.println("AC <- ~AC");
				System.out.println("AC = " + Integer.toHexString(AC + 0x10000).substring(1));
				break;
			case "CME":
				if (E == 0) {
					E = 1;
				} else {
					E = 0;
				}
				System.out.println("E <- ~E");
				System.out.println("E = " + E);
				break;
			case "CIR":
				E = (short) (AC & 0x0001);
				AC = (short) ((short) AC >> 1);
				System.out.println("AC <- shr AC, AC(15) <- E, E <- AC(0)");
				System.out.println("AC : " + Integer.toHexString(AC + 0x10000).substring(1) + ", E : " + E);
				break;
			case "CIL":
				E = I;
				AC = (short) ((short) AC << 1);
				System.out.println("AC <- shl AC, AC(0) <- E, E <- AC(15)");
				System.out.println("AC : " + Integer.toHexString(AC + 0x10000).substring(1) + ", E : " + E);
				break;
			case "INC":
				AC = (short) ((short) AC + (short) 1);
				System.out.println("AC <- AC + 1");
				System.out.println("AC : " + Integer.toHexString(AC + 0x10000).substring(1));
				break;
			case "SPA":
				if (I == 0)
					PC = (short) ((short) PC + (short) 1);
				System.out.println("If(AC(15) = 0) then (PC <- PC + 1)");
				System.out.println("PC : " + Integer.toHexString(PC + 0x10000).substring(1));
				break;
			case "SNA":
				if (I == 1)
					PC = (short) (PC + 1);
				System.out.println("If(AC(15) = 1) then (PC <- PC + 1)");
				System.out.println("PC : " + Integer.toHexString(PC + 0x10000).substring(1));
				break;
			case "SZA":
				if (AC == 0)
					PC = (short) (PC + 1);
				System.out.println("If(AC = 0) then (PC <- PC + 1)");
				System.out.println("PC : " + Integer.toHexString(PC + 0x10000).substring(1));
				break;
			case "SZE":
				if (E == 0)
					PC = (short) (PC + 1);
				System.out.println("If(E = 0) then (PC <- PC + 1)");
				System.out.println("E : " + E + ", PC : " + Integer.toHexString(PC + 0x10000).substring(1));
				break;
			case "HLT":
				System.out.println("Halt Computer");
				S = 0;
				throw new HaltException();
			}
			SC = 0;
			System.out.println("SC <- " + SC);
			System.out.println("SC = " + SC);
		} else if (head == 0xf) { // D7 = 1 이고, I = 1 인경우
			System.out.println("Excute input-output instruction");
			switch (symbol) {
			case "INP":
				System.out.println("AC(0-7) <- INPR, FGI <- 0");
				System.out.println("AC(0-7) = " + Integer.toHexString(0x00ff & AC).substring(1));
				AC = INPR;
				FGI = 0;
				break;
			case "OUT":
				System.out.println("OUTR <- AC(0-7), FGO <- 0");
				OUTR = (short) (0x00ff & AC);
				FGO = 0;
				break;
			case "SKI":
				System.out.println("If(FGI = 1) then (PC <- PC + 1)");
				if (FGI == 1) {
					PC += 1;
				}
				break;
			case "SKO":
				System.out.println("If(FGO = 1) then (PC <- PC + 1)");
				if (FGO == 1) {
					PC += 1;
				}
				break;
			case "ION":
				System.out.println("IEN <- 1");
				IEN = 1;
				break;
			case "IOF":
				System.out.println("IEN <- 0");
				IEN = 0;
				break;
			}

		} else {
			if (I == 1) {
				System.out.println("AR <- memory[AR]");
				AR = memory[AR];
				System.out.println("AR = " + Integer.toHexString(AR + 0x10000).substring(1));
			} else
				System.out.println("Nothing");

			System.out.println();

			switch (symbol) {
			case "AND":
				System.out.println("T4 : ");
				System.out.println("DR <- memory[AR]");
				DR = memory[AR];
				System.out.println("DR = " + Integer.toHexString(DR + 0x10000).substring(1));
				System.out.println();

				System.out.println("T5 : ");
				System.out.println("AC <- AC ^ DR");
				System.out.println("SC <- 0");
				AC = (short) (AC & DR);
				SC = 0;
				System.out.println("AC = " + Integer.toHexString(DR + 0x10000).substring(1));
				System.out.println("SC = " + SC);
				break;
			case "ADD":
				int Cout = 0;
				System.out.println("T4 : ");
				System.out.println("DR <- memory[AR]");
				DR = memory[AR];
				System.out.println("DR = " + Integer.toHexString(DR + 0x10000).substring(1));
				System.out.println();

				System.out.println("T5 : ");
				System.out.println("AC <- AC + DR");
				System.out.println("E <- Cout");
				System.out.println("SC <- 0");
				if (AC < 0 && AC + DR < 0 && DR > 0 || AC > 0 && AC + DR > 0 && DR < 0) { // 오버플로우가
																							// 일어났을때
					Cout = 1;
				}
				AC = (short) (AC + DR);
				E = (short) Cout;
				SC = 0;
				System.out.println("AC = " + Integer.toHexString(AC + 0x10000).substring(1));
				System.out.println("E = " + E);
				System.out.println("SC = " + SC);
				break;
			case "LDA":
				System.out.println("T4 : ");
				System.out.println("DR <- memory[AR]");
				DR = memory[AR];
				System.out.println("DR = " + Integer.toHexString(DR + 0x100000).substring(1));
				System.out.println();

				System.out.println("T5 : ");
				System.out.println("AC <- DR");
				System.out.println("SC <- 0");
				AC = DR;
				SC = 0;
				System.out.println("AC = " + Integer.toHexString(AC + 0x100000).substring(1));
				System.out.println("SC = " + SC);
				break;
			case "STA":
				System.out.println("T4 : ");
				System.out.println("memory[AR] <- AC");
				System.out.println("SC <- 0");
				memory[AR] = AC;
				SC = 0;
				System.out.println("memory[AR] = " + Integer.toHexString(memory[AR] + 0x10000).substring(1));
				System.out.println("SC = " + SC);
				break;
			case "BUN":
				System.out.println("T4 : ");
				System.out.println("PC <- AR");
				System.out.println("SC <- 0");
				PC = AR;
				SC = 0;
				System.out.println("PC = " + Integer.toHexString(PC + 0x10000).substring(1));
				System.out.println("SC = " + 0);
				break;
			case "BSA":
				System.out.println("T4 : ");
				System.out.println("memory[AR] <- PC");
				System.out.println("AR <- AR + 1");
				memory[AR] = PC;
				AR = (short) (AR + 1);
				System.out.println("memory[AR] = " + Integer.toHexString(memory[AR] + 0x10000).substring(1));
				System.out.println("AR = " + Integer.toHexString(AR + 0x10000).substring(2));
				System.out.println();

				System.out.println("T5 : ");
				System.out.println("PC <- AR");
				System.out.println("SC <- 0");
				PC = AR;
				SC = 0;
				System.out.println("PC = " + Integer.toHexString(PC + 0x10000).substring(1));
				System.out.println("SC = " + SC);
				break;
			case "ISZ":
				System.out.println("T4 : ");
				System.out.println("DR <- memory[AR]");
				DR = memory[AR];
				System.out.println("DR = " + Integer.toHexString(DR + 0x10000).substring(1));
				System.out.println();

				System.out.println("T5 : ");
				System.out.println("DR = DR + 1");
				DR = (short) (DR + 1);
				System.out.println("DR = " + Integer.toHexString(DR + 0x10000).substring(1));
				System.out.println();

				System.out.println("T6 : ");
				System.out.println("memory[AR] <- DR");
				System.out.println("If(DR = 0) then (PC <- PC + 1)");
				System.out.println("SC <- 0");
				memory[AR] = DR;
				if (DR == 0) {
					PC = (short) (PC + 1);
				}
				SC = 0;
				System.out.println("memory[AR] = " + Integer.toHexString(memory[AR] + 0x10000).substring(1));
				System.out.println("PC = " + Integer.toHexString(PC + 0x10000).substring(1));
				System.out.println("SC = " + SC);
			}
		}
	}

	public void printCycle() { // 명령어 사이클을 눈에 보이게 프린트 해준다.
		try {
			while (true) {
				System.out.println("--------------- Location : " + Integer.toHexString(PC) + "------------------");
				start();
				T0();
				T1();
				T2();
				instructionCheck();
				System.out.println("-------------------------------------------------------");
				System.out.println();
			}
		} catch (HaltException e) { // Halt 명령어 만나면 종료.
			System.out.println("종료 됩니다.");
		}
	}
}

class HaltException extends Exception { // Halt 명령수행하기위해 만든 예외 클래스
	HaltException() {
		super();
	}
}