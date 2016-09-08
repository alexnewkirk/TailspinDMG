#TailspinDMG

Interpreting GameBoy emulator and development tooling written in Java.

Licensed under the [MIT License](https://opensource.org/licenses/MITL).

##Components

###TEMU
Emulator core for Tailspin (_com.echodrop.gameboy.core_ and _com.echodrop.gameboy.graphics_). Contains core hardware model and logic.

###TDSM
GameBoy ROM disassembler (_com.echodrop.gameboy.disasm_)

###TDMG-UI
A collection of simple user interfaces for various components of TDMG

###TDBG
Emulation/ROM debugging engine (_com.echodrop.gameboy.debugger_)

####Usage (via _com.echodrop.gameboy.ui.DebuggerCLI_):
| Command                  |                                                              |
|--------------------------|--------------------------------------------------------------|
| help                     | Show command list                                            |
| step                     | Advance emulator by one instruction                          |
| setbrk  [memory address] | set a new breakpoint at specified address                    |
| setbrk                   | set a new breakpoint at current address                      |
| continue                 | run emulator until next breakpoint is reached                |
| exit                     | quit tdbg                                                    |
| logall                   | set logging mode to Level.ALL                                |
| loginfo                  | set logging mode to Level.INFO                               |
| nolog                    | set logging mode to Level.OFF                                |
| reset                    | initialize emulator state                                    |
| loadrom                  | load a new GameBoy ROM                                       |
| loadbios                 | load BIOS from file                                          |
| lsbrk                    | list all breakpoints                                         |
| regdmp                   | display value of all register states                         |
| memdmp                   | display memory dump of emulator's current state              |
| framedmp                 | display text representation of current framebuffer           |
| condbrk                  | add a new conditional breakpoint                             |
| clrbrk                   | clear all breakpoints                                        |
| tiledmp                  | display text representation of currently loaded tileset data |
| vtiledmp                 | render current tileset data to framebuffer                   |
| video                    | enable video mode                                            |
| render                   | draw framebuffer to screen                                   |

###Resources
Documentation used in the development of TDMG

1. http://bgb.bircd.org/pandocs.htm

2. http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-The-CPU

3. http://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html

4. http://www.devrs.com/gb/files/GBCPU_Instr.html

5. http://marc.rawer.de/Gameboy/Docs/GBCPUman.pdf

6. http://www.enliten.force9.co.uk/gameboy/memory.htm