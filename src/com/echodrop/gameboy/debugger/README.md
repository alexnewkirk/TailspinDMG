###TDBG
Emulation/ROM debugging engine

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