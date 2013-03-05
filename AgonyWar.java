import java.util.Arrays;
import java.util.Random;


/**
 * Created by Roy van Rijn, http://www.royvanrijn.com
 * Feel free to edit/copy/distribute, just be nice and give the appropriate credits.
 */
public class AgonyWar {

	private static final char[] instructions = {'$', '}', '{', '>', '<', '@', '~', '+', '-', '.', ',', '(', ')', '[', ']', '*'};

	/**
	 * Experiment:
	 * 
	 * AgonyWar is an implementation of a mixture between CoreWar and the Agony language.
	 * 
	 * The input , and output . instructions don't do anything.
	 * 
	 * Two or more programs are loaded in a core and execute instructions turn by turn.
	 * They run until a maximum amount of cycles is reached or one warrior is left.
	 *  
	 */
	public static void main(String[] args)  {
		AgonyWar agony = new AgonyWar(8000, 100, 100, 10000, 10000
				, "+[>->~]$@[]-~>{(**[(~)*,<(>(.]<-+*-[>)~@+}>--+"
				, "-[>>>[}@}@}@]+]"
				);
		int[] score = agony.battle();
		
		for(int i = 0; i<score.length;i++) {
			System.out.println("W"+(i+1)+" scores: "+score[i]);
		}
	}
	
	protected final int SIZE;
	protected final int ROUNDS;
	protected final int WARRIOR_SEPERATION;
	protected final int WARRIOR_MAX_LENGTH;
	protected final int MAX_CYCLES;
	
	protected final byte[] core;
	
	protected int cellPointer[];
	protected int executionPointer[];

	protected int cycleCount = 0;
	
	protected final String[] warriors;
	
	protected final boolean[] isDead;
	
	protected int roundOffset = 0;

	public AgonyWar(int coreSize, int warriorSeperation, int warriorMaxLength, int maxCycles, int rounds, String... warriors) {
		SIZE = coreSize;
		MAX_CYCLES = maxCycles;
		ROUNDS = rounds;
		WARRIOR_SEPERATION = warriorSeperation;
		WARRIOR_MAX_LENGTH = warriorMaxLength;
		this.warriors = warriors.clone();
		cycleCount = 0;
		core = new byte[coreSize];
		isDead = new boolean[warriors.length];
		cellPointer = new int[warriors.length];
		executionPointer = new int[warriors.length];
		score = new int[warriors.length];
	}
	
	protected int[] score;
	
	private void reset() {
		Arrays.fill(core, (byte)0);
		Arrays.fill(isDead, false);
		Arrays.fill(cellPointer, 0);
		Arrays.fill(executionPointer, 0);
		cycleCount = 0;
	}
	
	public int[] battle()  {
		for(int round = 0; round<ROUNDS; round++) {
			reset();
			runBattle();
		}
//		for(int warriorNr = 0; warriorNr<warriors.length;warriorNr++) {
//			System.out.println("Warrior "+(warriorNr+1)+" scored: "+score[warriorNr]);
//		}
		return score.clone();
	}
	
	private void runBattle()  {
		roundOffset = (roundOffset+1)%warriors.length;
		int[] startPositions = calculateStartPositions();
		
		//Parse warriors
		for(int x = 0; x<warriors.length;x++) {
			int warriorNr = (x+roundOffset)%warriors.length; 

			//Determine start location:
			cellPointer[warriorNr] = startPositions[warriorNr];
			executionPointer[warriorNr] = startPositions[warriorNr];
			
			char[] data = warriors[warriorNr].toCharArray();
			if(warriors[warriorNr].length() > WARRIOR_MAX_LENGTH) {
				throw new IllegalArgumentException("Warrior " + (warriorNr + 1) + " too long");
			}
			
			for(int i = 0; i <  warriors[warriorNr].length(); i++) {
				parseCharacter(data[i], warriorNr);
			}
			cellPointer[warriorNr] = (cellPointer[warriorNr] + 1) % SIZE;
		}
		
		//Execution
		int survivors = warriors.length;
		while(cycleCount < MAX_CYCLES) {
			int stillAlive = 0;
			for (int x = 0; x < warriors.length; x++) {
				int warriorNr = (x+roundOffset)%warriors.length; 
				if (!isDead[warriorNr]) {
					boolean isAlive = step(warriorNr);
					if(isAlive) {
						stillAlive++;
					} else {
						survivors--;
						isDead[warriorNr] = true;
					}
				}
			}
			cycleCount++;
			if(stillAlive <= 1) {
				break;
			}
		}
		for(int warriorNr = 0; warriorNr<warriors.length;warriorNr++) {
			if(!isDead[warriorNr]) {
				score[warriorNr] += (warriors.length*warriors.length-1)/survivors;
			}
		}
	}
	
	private int[] calculateStartPositions() {
		Random random = new Random();
		int[] startPositions = new int[warriors.length];
		boolean[] inUse = new boolean[SIZE];
		
		int register = 0;
		markUsed(inUse, register);
		
		for(int i = 1; i<startPositions.length; i++) {
			while(inUse[register = random.nextInt(SIZE)]);
			startPositions[i] = register;
			markUsed(inUse, register);
		}
		return startPositions;
	}

	public void markUsed(boolean[] inUse, int register) {
		for(int x = register-WARRIOR_SEPERATION-WARRIOR_MAX_LENGTH; x < register+WARRIOR_MAX_LENGTH+WARRIOR_SEPERATION; x++) {
			inUse[(x+SIZE)%SIZE] = true;;
		}
	}
	
	/**
	 * Returns false if instruction was halted
	 * @param warriorNr 
	 * @return
	 */
	protected boolean step(int warriorNr) {
//		printCore(warriorNr);

		byte instruction = core[executionPointer[warriorNr]];
		switch(instruction) {
		case 0: // 0000 $ Halt the program here
			return false;
		case 1: // 0001 } Move pointer right
			cellPointer[warriorNr] = ((cellPointer[warriorNr] + 1) % SIZE);
			break;
		case 2: // 0010 { Move pointer left
			cellPointer[warriorNr] = ((cellPointer[warriorNr] + SIZE - 1) % SIZE);
			break;
		case 3: // 0011 > Move pointer right 2 cells
			cellPointer[warriorNr] = ((cellPointer[warriorNr] + 2) % SIZE);
			break;
		case 4: // 0100 < Move pointer left 2 cells
			cellPointer[warriorNr] = ((cellPointer[warriorNr] + SIZE - 2) % SIZE);
			break;
		case 5: // 0101 @ Increment cell
			core[cellPointer[warriorNr]] = (byte) ((core[cellPointer[warriorNr]] + 1) % 16);
			break;
		case 6: // 0110 ~ Decrement cell
			core[cellPointer[warriorNr]] = (byte) ((core[cellPointer[warriorNr]] + 16 - 1) % 16);
			break;
		case 7: // 0111 + Increment character
			assignCharacter((byte)(getCharacterData(warriorNr)+1), warriorNr);
			break;
		case 8: // 1000 - Decrement character
			assignCharacter((byte)(getCharacterData(warriorNr)-1), warriorNr);
			break;
		case 9: // 1001 . Output the character (2 cells) at pointer and pointer-0
//			System.out.print((char)getCharacterData());
			break;
		case 10: // 1010 , Input a character (2 cells) to pointer and pointer-0
//			byte read = (byte)System.in.read();
//			if(read == -1) { //Translate EOF into a halt: 0
//				read = 0;
//			}
//			assignCharacter(read);
			break;
		case 11: // 1011 ( Jump past the matching ) if the cell under the pointer is zero
			if(core[cellPointer[warriorNr]] == 0) {
				if(!jumpForward(11, 12, warriorNr)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 12: // 1100 ) Jump back to the matching ( if the cell under the pointer is nonzero
			if(core[cellPointer[warriorNr]] != 0) {
				if(!jumpBackwards(11, 12, warriorNr)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 13: // 1101 [ Jump past the matching ] if the character under the pointer is zero
			if(getCharacterData(warriorNr) == 0) {
				if(!jumpForward(13, 14, warriorNr)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 14: // 1110 ] Jump back to the matching [ if the character under the pointer is nonzero
			if(getCharacterData(warriorNr) != 0) {
				if(!jumpBackwards(13, 14, warriorNr)) {
					//No matching bracket found, halt
					return false;
				}
			}
			break;
		case 15: // 1111 * Do nothing (free!)
			break;
		}
		//Increase executionPointer:
		executionPointer[warriorNr] = (executionPointer[warriorNr] + 1) % SIZE;
		return true;
	}


	private void printCore(int warriorNr) {
		System.out.println("Cycle: " + cycleCount+" for warrior "+warriorNr);
		for(int i = 0; i<core.length; i++) {
			System.out.print(instructions[core[i]]);
		}
		System.out.println();
		for(int i = 0; i<core.length; i++) {
			if(executionPointer[warriorNr] == i && cellPointer[warriorNr] == i) {
				System.out.print("#");
			} else if(executionPointer[warriorNr] == i) {
				System.out.print("^");
			} else if(cellPointer[warriorNr] == i) {
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
	private boolean jumpForward(int openBracket, int closeBracket, int warriorNr) {
		int startAt = executionPointer[warriorNr];
		int depth = 1;

		do {
			executionPointer[warriorNr] = (executionPointer[warriorNr]+1) % SIZE;
			if(core[executionPointer[warriorNr]] == openBracket) {
				depth++;
			} else if(core[executionPointer[warriorNr]] == closeBracket) {
				depth--;
			}
		} while(executionPointer[warriorNr] != startAt && depth != 0); 
		
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
	private boolean jumpBackwards(int openBracket, int closeBracket, int warriorNr) {
		int startAt = executionPointer[warriorNr];
		int depth = 1;

		do {
			executionPointer[warriorNr] = (executionPointer[warriorNr] + SIZE - 1) % SIZE;
			if(core[executionPointer[warriorNr]] == closeBracket) {
				depth++;
			} else if(core[executionPointer[warriorNr]] == openBracket) {
				depth--;
			}
		} while(executionPointer[warriorNr] != startAt && depth != 0); 
		
		if(depth == 0) { //Have we found the matching bracket?
			return true;
		}
		return false;
	}

	public void assignCharacter(byte read, int warriorNr) {
		core[(cellPointer[warriorNr] + SIZE - 1) % SIZE] = (byte) ((read >>> 4) & 15);
		core[cellPointer[warriorNr]] = (byte) (read & 15);
	}

	public byte getCharacterData(int warriorNr) {
		return (byte) ((core[cellPointer[warriorNr]]) | (core[(cellPointer[warriorNr] + SIZE - 1) % SIZE] << 4));
	}
	
	protected void parseCharacter(char instruction, int warriorNr) {
		for (int i = 0; i < instructions.length; i++) {
			if(instructions[i] == instruction) {
				core[cellPointer[warriorNr]] = (byte) i;
				cellPointer[warriorNr] = (cellPointer[warriorNr] + 1) % SIZE;
				return;
			}
		}
	}

}
