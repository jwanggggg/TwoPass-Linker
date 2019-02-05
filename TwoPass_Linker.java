import java.util.*;
import java.io.*; 

public class TwoPass_Linker {
	
	public static void main(String[] args) throws IOException {
		File input = new File("src/input/input-5");
		Scanner scan = new Scanner(input);
		
		Hashtable<String, Integer> symbolTable = new Hashtable<String, Integer>();
		boolean[] used; // Track which vars have been used
		ArrayList<ArrayList<Integer>> memoryMap = new ArrayList<ArrayList<Integer>>();
		
		int baseAddress = 0;
		int numModules = scan.nextInt();
		int max = 0; // Use this to index used array
		
		for (int i = 0; i < numModules; i++) {
			int numDefinitionPairs = scan.nextInt();
			
			// Definition list
			for (int j = 0; j < numDefinitionPairs; j++) {
				String symbol = scan.next();
				int tempAddress = scan.nextInt();
				
				int address = baseAddress + tempAddress;
				
				if (!symbolTable.containsKey(symbol)) {
					symbolTable.put(symbol, address);
					max = Math.max(max, address);
				}
				else
					System.out.println("Error: symbol already defined");
			}
			
			// Use list: clear buffer + skip first pass
			scan.nextLine();
			scan.nextLine();
			
			// Program list: increment base address, skip the rest
			int moduleSize = scan.nextInt();
			baseAddress += moduleSize;
			
			scan.nextLine();
		}
		
		scan.close();
		scan = new Scanner(input);
		
		// During the second iteration, we recreate the memory table.
		// R - Relative. Must be relocated based on base address offset.
		// E - External. Must be resolved. (Via the Linked List?) 
		// I - Immediate. Unchanged.
		// A - Absolute. Unchanged.
		
		baseAddress = 0;
		used = new boolean[max + 1];
		
		// Get the number of modules and clear the buffer
		numModules = scan.nextInt();
		scan.nextLine();
		
		for (int i = 0; i < numModules; i++) {
			// Definition list: Skip second time
			scan.nextLine();
			
			// Use list: Place symbol address/symbol into a Hashtable.
			int numUsePairs = scan.nextInt();
			Hashtable<Integer, String> useMappings = new Hashtable<Integer, String>();
			
			for (int j = 0; j < numUsePairs; j++) {
				String symbol = scan.next();
				int symbolAddress = scan.nextInt();
				if (symbolTable.containsKey(symbol)) {
					useMappings.put(symbolAddress, symbol);
					used[symbolTable.get(symbol)] = true;
				}
				else {
					System.out.println("Symbol is used but not defined. Using value 111");
					useMappings.put(111, symbol);
				}
					
			}
			
			// Program list: Adjust addresses according to rules. Read in the line and put it into a list.
			ArrayList<Integer> subList = new ArrayList<Integer>();
			int moduleSize = scan.nextInt();
			String[] programArray = scan.nextLine().split(" +");
			
			for (int j = 1; j < programArray.length; j += 2) {
				char type = programArray[j].charAt(0);
				int address = Integer.parseInt(programArray[j + 1]);
				
				switch (type) {
					case 'R':
						address += baseAddress;
						break;
					case 'E':
					case 'I':
					case 'A':
						break;
					default:
						break;
				}
				
				subList.add(address);
			}
			
			// Adjust External addresses last
			for (int index : useMappings.keySet()) {
				int element = subList.get(index);
				int nextAddress = nextAddress(element);
				int changedAddress = symbolTable.get(useMappings.get(index));
				int finalAddress = changeAddress(subList.get(index), changedAddress);
				subList.set(index, finalAddress);
				while (nextAddress != 777) {
					int nextElement = subList.get(nextAddress);
					finalAddress = changeAddress(subList.get(nextAddress), changedAddress);
					subList.set(nextAddress, finalAddress);
					nextAddress = nextAddress(nextElement);
				}
			}
			
			memoryMap.add(subList);
			baseAddress += moduleSize;
		}
		
		// Outputs
		
		System.out.println("Symbol table: \n" + symbolTable);
		for (ArrayList<Integer> list : memoryMap) {
			System.out.println("---");
			for (int address : list) {
				System.out.println(address);
			}
		}
		
		System.out.println(Arrays.toString(used));
		for (String symbol : symbolTable.keySet()) {
			if (!used[symbolTable.get(symbol)])
				System.out.println("Warning: " + symbol + " was defined but not used");
		}
		
		scan.close();
	} // End main

	public static int changeAddress(int address, int addressOffset) {
		while (address / 10 != 0) {
			address /= 10;
		}
		
		address *= 1000;
		address += addressOffset;
		return address;
	}
	
	public static int nextAddress(int address) {
		int multiplier = 1;
		int nextAddress = 0;
		while (address / 10 != 0) {
			int remainder = address % 10;
			nextAddress += (remainder * multiplier);
			multiplier *= 10;
			address /= 10;
		}
		
		return nextAddress;
	}
	
}
