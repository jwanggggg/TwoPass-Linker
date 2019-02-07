package Lab1;
import java.util.*;
import java.io.*; 

public class TwoPass_Linker {
	
	public static void main(String[] args) throws IOException {
		File input = new File("src/input/input-3");
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
			ArrayList<Boolean> useUsed = new ArrayList<>(); // Tracks if programList elements have been used
			
			for (int j = 0; j < numUsePairs; j++) {
				String symbol = scan.next();
				int symbolAddress = scan.nextInt();
				
				useSymbolAddresses.add(symbolAddress);
				useSymbols.add(symbol);
				
				if (symbols.contains(symbol)) {
					int symbolIndex = symbols.indexOf(symbol);
					used.set(symbolIndex, true);
					useExists.add(true);
				}
				else {
					System.out.println(symbol + " is used but not defined; 111 used.");
					useExists.add(false);
				}
				
			}
			
			// Program list: Adjust addresses according to rules. Read in the line and put it into a list.
			ArrayList<Integer> programList = new ArrayList<Integer>();
			int moduleSize = scan.nextInt();
			String[] programArray = scan.nextLine().split(" +");
			
			// Don't change as you go. Save the programlist and typelist and modify afterwards.
			// That way you can modify and print at the same time
			
			
			for (int j = 1; j < programArray.length; j += 2) {
				char type = programArray[j].charAt(0);
				int address = Integer.parseInt(programArray[j + 1]);
				int nextAddress = nextAddress(address);
				switch (type) {
					case 'R': // Use baseAddress if nextAddress exceeds module size
						if (nextAddress >= moduleSize) {
							System.out.println("Error: Type R address exceeds module size; 0 (relative) used");
							address = changeAddress(address, baseAddress);
						}
						else
							address += baseAddress;
						break;
					case 'E': // Special case
						break; 
					case 'I': // Nothing needs to be done
						break;
					case 'A': // Check if exceeds machine size
						if (nextAddress >= 300) {
							address = changeAddress(address, 299);
							System.out.println("A type address exceeds machine size; max legal value used");
						}
						break;
				}
				
				programList.add(address);
				useUsed.add(false); // All addresses are marked unused at first
			}
			
			// Adjust External addresses last
			for (int j = 0; j < useSymbolAddresses.size(); j++) {
				int index = useSymbolAddresses.get(j);
				String symbol = useSymbols.get(j);
				
				// If offset doesn't exist use 111
				int tempAddress = programList.get(index);
				int nextAddress = nextAddress(tempAddress);
				int offset = useExists.get(j) ? symbolAddresses.get(symbols.indexOf(symbol)) : 111;
				
				int finalAddress = changeAddress(tempAddress, offset);
				programList.set(index, finalAddress);
				
				// Check if used, then mark as used
				if (useUsed.get(index) == true) {
					System.out.println("Error: Multiple symbols used here; last one used.");
					nextAddress = nextAddress(finalAddress);
					programList.set(nextAddress, finalAddress);
				}
				
				else {
					while (nextAddress != 777) {
						int nextNextAddress = programList.get(nextAddress);
						finalAddress = changeAddress(nextNextAddress, offset);
						programList.set(nextAddress, finalAddress);
						nextAddress = nextAddress(nextNextAddress);
					}
				}
				
				useUsed.set(index, true);
				
			}
			
			memoryMap.add(programList);
			baseAddress += moduleSize;
		}

		// Outputs
		for (ArrayList<Integer> list : memoryMap) {
			System.out.println("---");
			for (int address : list) {
				System.out.println(address);
			}
		}
		System.out.println("---");
		
		
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
