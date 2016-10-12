;*****************************
; Tailspin Bootstrap ROM v0.1
; -anewkirk
;*****************************
SECTION "rom", HOME

; init stack pointer
ld SP,$FFFE

; Clear 0x8000 - 0x9FFF (VRAM)
xor A
ld DE,$1FFF
ld HL,$8000
clear:
	ldi [HL],A
	dec DE
	jr nz,clear

; Load logo tiles into VRAM

;load palette
ld A,$E4
ldh [$47],A

;turn on display
ld A,$81
ldh [$40],A

;unmap bootstrap from memory
ld A,$1
ldh [$50],A

;jump to start of ROM
jp $100

