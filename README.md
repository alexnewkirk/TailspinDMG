#TailspinDMG

An interpreting GameBoy emulator and development tooling written in Java.

Licensed under the [MIT License](https://opensource.org/licenses/MITL).

##Components

###TEMU
Emulator core for Tailspin (_com.echodrop.gameboy.core_ and _com.echodrop.gameboy.graphics_). Contains core hardware model and logic.

###TDMG-UI
A collection of user interfaces for various components of TDMG (_com.echodrop.gameboy.ui_). Command-line interfaces are available in addition to graphical user interfaces to the debugger and emulator.

###TDBG
Emulation/ROM debugging engine (_com.echodrop.gameboy.debugger_). Supports memory/register dumping, tile/framebuffer dumping, conditional breakpoints, and live memory search/edit. Real-time disassembler has yet to be implemented.

##Roadmap

###Emulator Core

* CPU interrupts

* Sprite Rendering

* Direct Memory Access transfers

* Joypad input

* CPU timers

* State serialization


###Debugger

* Time-travel

* Real-time disassembly


###Misc / Stretch goals

* Android support

* Custom color palettes

* Tailspin Bootstrap


##Resources
Documentation used in the development of TDMG

1. http://bgb.bircd.org/pandocs.htm

2. http://imrannazar.com/GameBoy-Emulation-in-JavaScript:-The-CPU

3. http://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html

4. http://www.devrs.com/gb/files/GBCPU_Instr.html

5. http://marc.rawer.de/Gameboy/Docs/GBCPUman.pdf

6. http://www.enliten.force9.co.uk/gameboy/memory.htm

7. http://gbdev.gg8.se/

8. http://gameboy.mongenel.com/asmschool.html

9. http://www.codeslinger.co.uk/pages/projects/gameboy.html