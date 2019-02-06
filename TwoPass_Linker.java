package Lab1;
import java.util.*;
import java.io.*; 

public class TwoPass_Linker {
	
	public static void main(String[] args) throws IOException {
		File input = new File("src/input/input-5");
		Scanner scan = new Scanner(input);
		
		ArrayList<String> symbols = new ArrayList<>();
		ArrayList<Integer> symbolAddresses = new ArrayList<>();
		ArrayList<Boolean> used = new ArrayList<>();
		ArrayList<ArrayList<Integer>> memoryMap = new ArrayList<>();

		int baseAddress = 0;
		int numModules = scan.nextInt();
		
		for (int i = 0; i < numModules; i++) {
			int numDefinitionPairs = scan.nextInt();
			
			// Definition list
			for (int j = 0; j < numDefinitionPairs; j++) {
				String symbol = scan.next();
				int tempAddress = scan.nextInt();
				
				int address = baseAddress + tempAddress;
				
				if (!symbols.contains(symbol)) {
					symbols.add(symbol);
					symbolAddresses.add(address);
					used.add(false);
				}
				else {
					System.out.println("Error: This variable is multiply defined; last value used.");
					int indexToChange = symbols.indexOf(symbol);
					symbolAddresses.set(indexToChange, address);
				}
					
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
		
		// --- SECOND PASS ---
		// During the second iteration, we recreate the memory table.
		// R - Relative. Must be relocated based on base address offset.
		// E - External. Must be resolved. 
		// I - Immediate. Unchanged.
		// A - Absolute. Unchanged.
		
		baseAddress = 0;
		scan = new Scanner(input);
		// Get the number of modules and clear the buffer
		numModules = scan.nextInt();
		scan.nextLine();
		
		for (int i = 0; i < numModules; i++) {
			// Definition list: Skip second time
			int numDefs = scan.nextInt();
			for (int j = 0; j < numDefs; j++) {
				scan.next();
				scan.next();
			}
			
			// Use list: Place symbol address/symbol into a Hashtable.
			int numUsePairs = scan.nextInt();
			ArrayList<Integer> useSymbolAddresses = new ArrayList<>();
			ArrayList<String> useSymbols = new ArrayList<>();
			ArrayList<Boolean> useExists = new ArrayList<>();
			
			for (int j = 0; j < numUsePairs; j++) {
				String symbol = scan.next();
				int symbolAddress = scan.nextInt();
				
				if (symbols.contains(symbol)) {
					useSymbolAddresses.add(symbolAddress);
					useSymbols.add(symbol);
					int symbolIndex = symbols.indexOf(symbol);
					used.set(symbolIndex, true);
					useExists.add(true);
				}
				else {
					System.out.println(symbol + " is used but not defined; 111 used.");
					useSymbolAddresses.add(symbolAddress);
					useSymbols.add(symbol);
					useExists.add(false);
				}
					
			}
			
			System.out.println(useSymbolAddresses);
			System.out.println(useSymbols);
			System.out.println(useExists);
			
			// Program list: Adjust addresses according to rules. Read in the line and put it into a list.
			ArrayList<Integer> subList = new ArrayList<Integer>();
			int moduleSize = scan.nextInt();
			String[] programArray = scan.nextLine().split(" +");
			
			// Switch usageMappings to double ArrayLists.
			
			for (int j = 1; j < programArray.length; j += 2) {
				char type = programArray[j].charAt(0);
				int address = Integer.parseInt(programArray[j + 1]);
				
				switch (type) {
					case 'R':
						address += baseAddress;
						break;
					case 'E':	
						break;
					case 'I':
					case 'A':
						break;
					default:
						break;
				}
				
				subList.add(address);
			}
			
			System.out.println("Use symbols: " + useSymbols);
			System.out.println("Use symbol addresses: " + useSymbolAddresses);
			
			// Adjust External addresses last
			for (int j = 0; j < useSymbolAddresses.size(); j++) {
				int index = useSymbolAddresses.get(j);
				String symbol = useSymbols.get(j);
				// If it doesn't exist just grab 111
				if (!useExists.get(j)) {
					int tempAddress = subList.get(index);
					int offset = 111;
					int finalAddress = changeAddress(tempAddress, offset);
					subList.set(index, finalAddress);
				}
				
				else {
					int tempAddress = subList.get(index);
					int nextAddress = nextAddress(tempAddress);
					int offset = symbolAddresses.get(symbols.indexOf(symbol));
					
					int finalAddress = changeAddress(tempAddress, offset);
					subList.set(index, finalAddress);
					
					while (nextAddress != 777) {
						int nextNextAddress = subList.get(nextAddress);
						finalAddress = changeAddress(nextNextAddress, offset);
						subList.set(nextAddress, finalAddress);
						nextAddress = nextAddress(nextNextAddress);
					}
				}
				
				
				
			}
			
			
			memoryMap.add(subList);
			baseAddress += moduleSize;
		}

		// Outputs
		System.out.println("Symbols: " + symbols);
		System.out.println("Addresses: " + symbolAddresses);
		
		for (ArrayList<Integer> list : memoryMap) {
			System.out.println("---");
			for (int address : list) {
				System.out.println(address);
			}
		}
		
		for (int i = 0; i < symbols.size(); i++) {
			if (!used.get(i))
				System.out.println("Warning: Symbol " + symbols.get(i) + " is defined but never used");
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
