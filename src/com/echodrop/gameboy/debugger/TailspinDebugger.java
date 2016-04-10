package com.echodrop.gameboy.debugger;

import java.util.ArrayList;
import java.util.Scanner;

import com.echodrop.gameboy.core.TailspinGB;

public class TailspinDebugger {
	
	private ArrayList<Character> breakpoints;
	private Scanner sc;
	private TailspinGB system;
	private boolean running;
	
	public TailspinDebugger() {
		this.breakpoints = new ArrayList<Character>();
		this.running = true;
		this.sc = new Scanner(System.in);
		this.system = new TailspinGB();
		
		system.reset();
		
		system.getMem().loadRom("tetris.gb");
	}
	
	public static void main(String[] args) {
		
		TailspinDebugger tsd = new TailspinDebugger();
		
		while(tsd.running) {
			
			DebugCommand currentCommand = tsd.readCommand();
			
			System.out.println(currentCommand + "\n");
			
			switch(currentCommand.getCommand()) {
			
			case EXIT:
				tsd.running = false;
				break;
			case STEP:
				tsd.system.getProcessor().step();
				break;
			case SETBREAK:
				char bp = tsd.system.getProcessor().getPc();
				tsd.breakpoints.add(bp);
				System.out.println("[!] Breakpoint added at 0x" + Integer.toHexString(bp & 0xFFFF).toUpperCase());
				break;
			case RMBREAK:
				break;
			case CONTINUE:
				break;
			case REGDUMP:
				break;
			case MEMDUMP:
				break;
				
			
			}
			
			
		}
		
	}
	
	private DebugCommand readCommand() {
		
		DebugCommand c = null;
		
		while(true) {
			System.out.print("[tailspin] >");
			String input = sc.nextLine().toUpperCase();
			
			DebugCommandType commandType = null;
			Character argument = null;
			
			for(DebugCommandType dct : DebugCommandType.values()) {
				
				if(input.contains(dct.name())) {
					
					commandType = dct;
					
					String remaining = input.replace(dct.toString(), "");
					
					remaining = remaining.replace("0X", "");
					
					remaining = remaining.trim();
					
					if(!remaining.isEmpty()) {
						
						try {
							
							argument = (char) Integer.parseInt(remaining, 16);
							
						} catch (NumberFormatException e) {
							
							//invalid or no argument
							
						}
						
					}
					
					c = new DebugCommand(commandType, argument);
					return c;
					
				}
				
			}
			
			
			
		}
		
		//throw new InvalidDebugCommandException(input);
	}
	
	

}
