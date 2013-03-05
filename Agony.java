import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Roy van Rijn, http://www.royvanrijn.com
 * Feel free to edit/copy/distribute, just be nice and give the appropriate credits.
 */
public class Agony {

	/** 
	 * See http://esolangs.org/wiki/Agony 
	 * 
	 * Instruction set:
	 * 0000 $ Halt the program here
	 * 0001 } Move pointer right
	 * 0010 { Move pointer left
	 * 0011 > Move pointer right 2 cells 
	 * 0100 < Move pointer left 2 cells
	 * 0101 @ Increment cell
	 * 0110 ~ Decrement cell
	 * 0111 + Increment character
	 * 1000 - Decrement character
	 * 1001 . Output the character (2 cells) at pointer and pointer-0
	 * 1010 , Input a character (2 cells) to pointer and pointer-0
	 * 1011 ( Jump past the matching ) if the cell under the pointer is zero
	 * 1100 ) Jump back to the matching ( if the cell under the pointer is nonzero
	 * 1101 [ Jump past the matching ] if the character under the pointer is zero
	 * 1110 ] Jump back to the matching [ if the character under the pointer is nonzero
	 * 1111 * Swap character (2 cells) at pointer with the buffer (2 cells, buffer has 0000 0000 initially)
	 */
	
	private static final char[] instructions = {'$', '}', '{', '>', '<', '@', '~', '+', '-', '.', ',', '(', ')', '[', ']', '*'};
	
	public static void main(String[] args) throws IOException {
		if(args.length != 1) {
			System.out.println("Arguments expected: filename");
		}

		//Read file:
		BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
		StringBuilder builder = new StringBuilder();
		while(reader.ready()) {
			builder.append(reader.readLine());
		}
		reader.close();
		
		//Run Agony:
		Agony agony = new Agony();
		agony.interpret(builder.toString());
	}
	
	private static final boolean DEBUG = false;
	private static final int MAX_CYCLES = 100000000;

	private final int SIZE = 1500;
	private final byte[] core = new byte[SIZE];
	private byte buffer = 0;
	
	private int cellPointer = 0;
	private int executionPointer = 0;

	private int cycleCount = 0;
	
	private void interpret(String program) throws IOException {
		//Parse input
		char[] data = program.toCharArray();
		for(int i = 0; i< program.length(); i++) {
			parseCharacter(data[i]);
		}
		cellPointer = (cellPointer + 1) % SIZE;

		//Execution
		while(step() && cycleCount < MAX_CYCLES);
	}
	
	/**
	 * Returns false if instruction was halted
	 * @return
	 * @throws IOException 
	 */
	protected boolean step() throws IOException {
		cycleCount++;
		if(DEBUG) {
			printCore();
		}

		byte instruction = core[executionPointer];
		switch(instruction) {
		case 0: // 0000 $ Halt the program here
			return false;
		case 1: // 0001 } Move pointer right
			cellPointer = ((cellPointer + 1) % SIZE);
			break;
		case 2: // 0010 { Move pointer left
			cellPointer = ((cellPointer + SIZE - 1) % SIZE);
			break;
		case 3: // 0011 > Move pointer right 2 cells
			cellPointer = ((cellPointer + 2) % SIZE);
			break;
		case 4: // 0100 < Move pointer left 2 cells
			cellPointer = ((cellPointer + SIZE - 2) % SIZE);
			break;
		case 5: // 0101 @ Increment cell
			core[cellPointer] = (byte) ((core[cellPointer] + 1) % 16);
			break;
		case 6: // 0110 ~ Decrement cell
			core[cellPointer] = (byte) ((core[cellPointer] + 16 - 1) % 16);
			break;
		case 7: // 0111 + Increment character
			assignCharacter((byte)(getCharacterData()+1));
			break;
		case 8: // 1000 - Decrement character
			assignCharacter((byte)(getCharacterData()-1));
			break;
		case 9: // 1001 . Output the character (2 cells) at pointer and pointer-0
			char value = (char)getCharacterData();
			output(value);
			break;
		case 10: // 1010 , Input a character (2 cells) to pointer and pointer-0
			byte read = input();
			if(read == -1) { //Translate EOF into a halt: 0
				read = 0;
			}
			assignCharacter(read);
			break;
		case 11: // 1011 ( Jump past the matching ) if the cell under the pointer is zero
			if(core[cellPointer] == 0) {
				if(!jumpForward(11, 12)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 12: // 1100 ) Jump back to the matching ( if the cell under the pointer is nonzero
			if(core[cellPointer] != 0) {
				if(!jumpBackwards(11, 12)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 13: // 1101 [ Jump past the matching ] if the character under the pointer is zero
			if(getCharacterData() == 0) {
				if(!jumpForward(13, 14)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 14: // 1110 ] Jump back to the matching [ if the character under the pointer is nonzero
			if(getCharacterData() != 0) {
				if(!jumpBackwards(13, 14)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 15: // 1111 * Swap value in buffer with cell at pointer location (buffer has 0000 initially)
			byte temp = getCharacterData();
			assignCharacter(buffer);
			buffer = temp;
			break;
		}
		//Increase executionPointer:
		executionPointer = (executionPointer + 1) % SIZE;
		return true;
	}

	public byte input() throws IOException {
		return (byte)System.in.read();
	}

	public void output(char value) {
		System.out.print(value);
	}


	private void printCore() {
		System.out.println("Step: " + cycleCount);
		for(int i = 0; i<core.length; i++) {
			System.out.print(instructions[core[i]]);
		}
		System.out.println();
		for(int i = 0; i<core.length; i++) {
			if(executionPointer == i && cellPointer == i) {
				System.out.print("#");
			} else if(executionPointer == i) {
				System.out.print("^");
			} else if(cellPointer == i) {
				System.out.print("!");
			} else {
				System.out.print(" ");
			}
		}
		System.out.println();
	}

	/**
	 * Look forward until: 
	 *  1) We've made a complete loop (returns false, halt execution)
	 *  2) We have a matching closing bracket at same level 
	 */
	private boolean jumpForward(int openBracket, int closeBracket) {
		int startAt = executionPointer;
		int depth = 1;

		do {
			executionPointer = (executionPointer+1) % SIZE;
			if(core[executionPointer] == openBracket) {
				depth++;
			} else if(core[executionPointer] == closeBracket) {
				depth--;
			}
		} while(executionPointer != startAt && depth != 0); 
		
		if(depth == 0) { //Have we found the matching bracket?
			return true;
		}
		return false;
	}
	
	/**
	 * Look backwards until: 
	 *  1) We've made a complete loop (returns false, halt execution)
	 *  2) We have a matching opening bracket at same level 
	 */
	private boolean jumpBackwards(int openBracket, int closeBracket) {
		int startAt = executionPointer;
		int depth = 1;

		do {
			executionPointer = (executionPointer + SIZE - 1) % SIZE;
			if(core[executionPointer] == closeBracket) {
				depth++;
			} else if(core[executionPointer] == openBracket) {
				depth--;
			}
		} while(executionPointer != startAt && depth != 0); 
		
		if(depth == 0) { //Have we found the matching bracket?
			return true;
		}
		return false;
	}

	public void assignCharacter(byte read) {
		core[(cellPointer + SIZE - 1) % SIZE] = (byte) ((read >>> 4) & 15);
		core[cellPointer] = (byte) (read & 15);
	}

	public byte getCharacterData() {
		return (byte) ((core[cellPointer]) | (core[(cellPointer + SIZE - 1) % SIZE] << 4));
	}
	
	protected void parseCharacter(char instruction) {
		for (int i = 0; i < instructions.length; i++) {
			if(instructions[i] == instruction) {
				core[cellPointer] = (byte) i;
				cellPointer = (cellPointer + 1) % SIZE;
				return;
			}
		}
	}
}
