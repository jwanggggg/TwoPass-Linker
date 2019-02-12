import java.util.ArrayList;
import java.util.Scanner;

public class TwoPass_Linker {
	
	public static void main(String[] args) {
		// Trimmed String array; holds all Strings split in the file.
		String[] fileArray = generateFileArray();
		
		// Lists for symbol table
		ArrayList<String> symbols = new ArrayList<>();
		ArrayList<Integer> symbolAddresses = new ArrayList<>();
		
		// Lists to track errors
		ArrayList<Boolean> multiplyDefined = new ArrayList<>(); // Check if multiple definitions in DL
		ArrayList<Boolean> used = new ArrayList<>(); // Check if all defined symbols are used
		ArrayList<Integer> moduleDefined = new ArrayList<>(); // Track which module each symbol is defined in
		
		// Indices
		int baseAddress = 0; // Track base address for each module
		int fileIndex = 0; // Index will be used to iterate through the array
		
		// --- FIRST PASS ---
		// Create the symbol table using the definition lists.
		
		int numModules = Integer.parseInt(fileArray[fileIndex++]);
		
		for (int i = 0; i < numModules; i++) {
			// Definition list
			int numDefinitions = Integer.parseInt(fileArray[fileIndex++]);
			
			for (int j = 0; j < numDefinitions; j++) {
				// Increment tempAddress by baseAddress offset
				String symbol = fileArray[fileIndex++];
				int tempAddress = Integer.parseInt(fileArray[fileIndex++]);
				int address = baseAddress + tempAddress; 
				
				// Check if symbol table contains symbol and set error lists accordingly
				if (!symbols.contains(symbol)) {
					symbols.add(symbol);
					symbolAddresses.add(address);
					used.add(false);
					multiplyDefined.add(false);
					moduleDefined.add(i);
				}
				else {
					int indexToChange = symbols.indexOf(symbol);
					symbolAddresses.set(indexToChange, address);
					multiplyDefined.set(indexToChange, true);
					moduleDefined.set(indexToChange, i);
				}
					
			}
			
			// Use list: Skip on first pass.
			int numUses = Integer.parseInt(fileArray[fileIndex++]);
			for (int j = 0; j < numUses; j++)
				fileIndex += 2;
			
			// Program list: increment base address, skip the rest on first pass.
			int moduleSize = Integer.parseInt(fileArray[fileIndex++]);
			baseAddress += moduleSize;
			
			for (int j = 0; j < moduleSize; j++)
				fileIndex += 2;
		}
		
		// Print the Symbol Table
		System.out.println("Symbol Table\n-----");
		for (int i = 0; i < symbols.size(); i++) {
			String symbol = symbols.get(i);
			int symbolAddress = symbolAddresses.get(i);
			boolean wasMultiplyDefined = multiplyDefined.get(i); 
			System.out.print(symbol + "=" + symbolAddress);
			if (wasMultiplyDefined) {
				System.out.print(" Error: This variable is multiply defined; last value used.");
			}
			System.out.print("\n");
		}
		
		// --- SECOND PASS ---
		// During the second iteration, we recreate the memory table.
		// R - Relative. Must be relocated based on base address offset.
		// E - External. Must be resolved. 
		// I - Immediate. Unchanged.
		// A - Absolute. Unchanged.
		
		// Reset indices and begin second pass
		baseAddress = 0;
		fileIndex = 0;
		numModules = Integer.parseInt(fileArray[fileIndex++]);
		
		System.out.println("\nMemory Map\n-----");
		
		for (int i = 0; i < numModules; i++) {
			// Definition list: Skip second pass
			int numDefinitions = Integer.parseInt(fileArray[fileIndex++]);
			
			for (int j = 0; j < numDefinitions; j++) {
				fileIndex += 2;
			}
			
			
			// Use List: Place symbol address/symbol into two ArrayLists
			int numUses = Integer.parseInt(fileArray[fileIndex++]);
			
			// Lists used to track the symbols/addresses in the use list 
			ArrayList<Integer> useSymbolAddresses = new ArrayList<>();
			ArrayList<String> useSymbols = new ArrayList<>();
			
			// Lists used to track errors in the use list
			ArrayList<Boolean> useExists = new ArrayList<>(); // Tracks if use list elements are defined
			ArrayList<Boolean> useUsed = new ArrayList<>(); // Tracks if use list elements have been used
			ArrayList<Boolean> multiplyUsed = new ArrayList<>(); // Tracks if multiple use list elements are used in one index
			
			// Construct use symbol/address mapping
			for (int j = 0; j < numUses; j++) {
				String symbol = fileArray[fileIndex++];
				int symbolAddress = Integer.parseInt(fileArray[fileIndex++]);
				
				useSymbolAddresses.add(symbolAddress);
				useSymbols.add(symbol);
				
				// Handle error lists if symbol is used > once
				if (symbols.contains(symbol)) {
					int symbolIndex = symbols.indexOf(symbol);
					used.set(symbolIndex, true);
					useExists.add(true);
				}
				else {
					useExists.add(false);
				}
				
			}
						
			// PROGRAM LIST: Adjust addresses according to rules.
			
			// Lists to track type/number 
			ArrayList<Character> typeList = new ArrayList<>(); // Tracks type (R/I/E/A)
			ArrayList<Integer> programList = new ArrayList<>(); // Tracks number
			
			// Lists to track errors
			ArrayList<Boolean> hasError = new ArrayList<>(); // Determines if each one has an error
			ArrayList<Boolean> isDefined = new ArrayList<>(); // Tracks which elements in programList weren't defined
			ArrayList<String> finalListSymbolAllocations = new ArrayList<>(); // Tracks which symbols are used at which E address
			
			// Final list
			ArrayList<Integer> finalList = new ArrayList<>();
			int moduleSize = Integer.parseInt(fileArray[fileIndex++]);
			
			// Track the current SYMBOL and 4-DIGIT NUMBER in two ArrayLists
			
			for (int j = 0; j < moduleSize * 2; j++) {
				String curr = fileArray[fileIndex++];
				// Evens are type, odds are addresses.
				if (j % 2 == 0) {
					typeList.add(curr.charAt(0));
				}
				
				else {
					programList.add(Integer.parseInt(curr));
				}
			}
			
			// Track errors for R and A size
			for (int j = 0; j < typeList.size(); j++) {
				char type = typeList.get(j);
				int address = programList.get(j);
				int nextAddress = nextAddress(address);
				
				switch (type) {
					case 'R': // Use baseAddress if nextAddress exceeds module size
						if (nextAddress >= moduleSize) {
							address = changeAddress(address, baseAddress);
							hasError.add(true);
						}
						else {
							address += baseAddress;
							hasError.add(false);
						}
						break;
					case 'E': 
					case 'I': // Nothing needs to be done
						hasError.add(false);
						break;
					case 'A': // Check if exceeds machine size
						if (nextAddress >= 300) {
							address = changeAddress(address, 299);
							hasError.add(true);
						}
						else
							hasError.add(false);
						break;
				}
				finalList.add(address);
				isDefined.add(false);
				finalListSymbolAllocations.add("");
				useUsed.add(false);
				multiplyUsed.add(false);
			}
			
			// Adjust External addresses last
			for (int j = 0; j < useSymbolAddresses.size(); j++) {
				int index = useSymbolAddresses.get(j);
				String symbol = useSymbols.get(j);
				boolean defined = useExists.get(j);
				
				// Follow the Linked List using the last 3 digits of each value
				int tempAddress = finalList.get(index);
				int nextAddress = nextAddress(tempAddress);
				
				// If offset doesn't exist use 111
				int offset;
				if (defined) {
					offset = symbolAddresses.get(symbols.indexOf(symbol));
					isDefined.set(index, true);
				} else {
					offset = 111;
					isDefined.set(index, false);
					finalListSymbolAllocations.set(index, symbol);
				}
				
				int finalAddress = changeAddress(tempAddress, offset);
				finalList.set(index, finalAddress);
				
				// Check if used, then mark as used
				if (useUsed.get(index) == true) {
					nextAddress = nextAddress(finalAddress);
					finalList.set(nextAddress, finalAddress);
					if (offset == 111)
						isDefined.set(nextAddress, false);
					else
						isDefined.set(nextAddress, true);
					
					finalListSymbolAllocations.set(nextAddress, symbol);
					multiplyUsed.set(nextAddress, true);
				}
				
				else {
					while (nextAddress != 777) {
						int nextNextAddress = programList.get(nextAddress);
						finalAddress = changeAddress(nextNextAddress, offset);
						finalList.set(nextAddress, finalAddress);
						if (offset == 111)
							isDefined.set(nextAddress, false);
						else
							isDefined.set(nextAddress, true);
						
						finalListSymbolAllocations.set(nextAddress, symbol); // Set the symbol used at the index of program list
						nextAddress = nextAddress(nextNextAddress);
					}
				}
				
				useUsed.set(index, true);
			}
			
			// PRINT MEMORY MAP
			
			for (int j = 0; j < typeList.size(); j++) {
				char type = typeList.get(j);
				int address = finalList.get(j);
				boolean defined = isDefined.get(j);
				boolean multipleUsed = multiplyUsed.get(j);
				
				System.out.print(address + " ");
				switch (type) {
					case 'R':
						if (hasError.get(j) == true)
							System.out.print("Error: Type R address exceeds module size; 0 (relative) used");
						break;
					case 'A':
						if (hasError.get(j) == true)
							System.out.print("Error: A type address exceeds machine size; max legal value used");
						break;
					case 'E':
						if (!defined) {
							String symbol = finalListSymbolAllocations.get(j);
							System.out.print(symbol + " is used but not defined; 111 used.");
						}
						
						if (multipleUsed)
							System.out.print(" Error: Multiple symbols used here; last one used");
						
						break;
				}
				
				System.out.print("\n");
			}
			
			baseAddress += moduleSize;
		}

		// Lastly, check if any symbols haven't been used
		for (int i = 0; i < symbols.size(); i++) {
			if (!used.get(i))
				System.out.println("Warning: " + symbols.get(i) + " is defined in"
						+ " module " + moduleDefined.get(i) + " but never used");
		}
		
	} // End main
	
	// Generates the file array used in the program
	public static String[] generateFileArray() {
		Scanner fileInput = new Scanner(System.in);
		StringBuilder sb = new StringBuilder();
		System.out.println("Paste the entire file input in. Enter \"s\" to end the input.");
		
		while (true) {
			String line = fileInput.nextLine();
			if (line.toLowerCase().equals("s"))
				break;
			
			sb.append(line + " ");
		}
		
		fileInput.close();
		
		String[] fileArray = sb.toString().trim().split("\\s+");
		return fileArray;
	}
	
	// Modifies the last three digits of a given address.
	public static int changeAddress(int address, int addressOffset) {
		while (address / 10 != 0) {
			address /= 10;
		}
		
		address *= 1000;
		address += addressOffset;
		return address;
	}
	
	// Scrapes the last three digits of a given address.
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
